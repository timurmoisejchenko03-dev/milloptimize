/*
 * Decompiled with CFR 0.150.
 * 
 * Could not load the following classes:
 *  com.mojang.logging.LogUtils
 *  net.minecraft.core.BlockPos
 *  net.minecraft.network.chat.Component
 *  net.minecraft.network.protocol.common.custom.CustomPacketPayload
 *  net.minecraft.server.level.ServerLevel
 *  net.minecraft.server.level.ServerPlayer
 *  net.minecraft.world.InteractionResult
 *  net.minecraft.world.entity.Entity
 *  net.minecraft.world.entity.player.Player
 *  net.minecraft.world.item.Item
 *  net.minecraft.world.item.Item$Properties
 *  net.minecraft.world.item.context.UseOnContext
 *  net.minecraft.world.level.Level
 *  net.minecraft.world.level.block.entity.BlockEntity
 *  net.neoforged.neoforge.network.PacketDistributor
 *  org.slf4j.Logger
 */
package org.millenaire.item;

import com.mojang.logging.LogUtils;
import java.util.Set;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.neoforged.neoforge.network.PacketDistributor;
import org.millenaire.advancement.MillAdvancements;
import org.millenaire.block.LockedChestBlockEntity;
import org.millenaire.building.BuildingInstance;
import org.millenaire.culture.ModCultures;
import org.millenaire.culture.VillageType;
import org.millenaire.entity.MillVillager;
import org.millenaire.network.NegationWandPayload;
import org.millenaire.village.Village;
import org.millenaire.village.VillageChunkLoader;
import org.millenaire.village.VillageSavedData;
import org.slf4j.Logger;

public class NegationWandItem
extends Item {
    private static final Logger LOGGER = LogUtils.getLogger();
    private static final double SEARCH_RADIUS = 30.0;

    public NegationWandItem(Item.Properties properties) {
        super(properties);
    }

    public InteractionResult useOn(UseOnContext context) {
        Level level = context.getLevel();
        if (level.isClientSide()) {
            return InteractionResult.SUCCESS;
        }
        ServerLevel serverLevel = (ServerLevel)level;
        Player player = context.getPlayer();
        if (!(player instanceof ServerPlayer)) {
            return InteractionResult.FAIL;
        }
        ServerPlayer player2 = (ServerPlayer)player;
        BlockPos clickedPos = context.getClickedPos();
        VillageSavedData savedData = VillageSavedData.get(serverLevel);
        Village village = savedData.getVillageManager().findNearestVillage(clickedPos, 30.0);
        if (village == null) {
            player2.sendSystemMessage((Component)Component.translatable((String)"negationwand.novillage"));
            return InteractionResult.FAIL;
        }
        if (village.areChestsLocked()) {
            String name = village.getVillageName() != null ? village.getVillageName() : village.getVillageTypeId().getPath();
            player2.sendSystemMessage((Component)Component.translatable((String)"negationwand.villagelocked", (Object[])new Object[]{name}));
            return InteractionResult.SUCCESS;
        }
        String villageName = village.getVillageName() != null ? village.getVillageName() : "";
        PacketDistributor.sendToPlayer((ServerPlayer)player2, (CustomPacketPayload)new NegationWandPayload(village.getId().uuid().toString(), village.getVillageTypeId().getPath(), villageName), (CustomPacketPayload[])new CustomPacketPayload[0]);
        return InteractionResult.SUCCESS;
    }

    public static void performDeletion(ServerLevel level, VillageSavedData savedData, Village village, ServerPlayer player) {
        for (BuildingInstance buildingInstance : village.getBuildings()) {
            for (BlockPos chestPos : buildingInstance.getChestPositions()) {
                BlockEntity blockEntity = level.getBlockEntity(chestPos);
                if (!(blockEntity instanceof LockedChestBlockEntity)) continue;
                LockedChestBlockEntity chest = (LockedChestBlockEntity)blockEntity;
                chest.setBuildingId(null);
            }
        }
        int killed = 0;
        for (UUID uuid : village.getVillagerUuids()) {
            Entity entity = level.getEntity(uuid);
            if (!(entity instanceof MillVillager)) continue;
            MillVillager villager = (MillVillager)entity;
            villager.discard();
            ++killed;
        }
        if (!village.getLoadedChunks().isEmpty()) {
            VillageChunkLoader.releaseVillageChunks(level, village.getCenter(), village.getLoadedChunks());
            village.setLoadedChunks(Set.of());
            village.setChunksForceLoaded(false);
        }
        savedData.getVillageManager().removeVillage(village.getId());
        for (Village other : savedData.getVillageManager().getAllVillages()) {
            other.removeRelation(village.getId());
            if (!village.getId().equals(other.getParentVillageId())) continue;
            other.setParentVillageId(null);
        }
        savedData.removeLoneBuilding(village.getCenter());
        savedData.setDirty();
        LOGGER.info("[Mill\u00e9naire] Village {} deleted by negation wand ({} villagers removed)", (Object)village.getId().uuid().toString().substring(0, 8), (Object)killed);
        VillageType villageType = ModCultures.getVillageType(village.getVillageTypeId());
        if (villageType != null && !villageType.loneBuilding()) {
            MillAdvancements.grant(player, MillAdvancements.SCIPIO);
        }
        String name = village.getVillageName() != null ? village.getVillageName() : village.getVillageTypeId().getPath();
        player.sendSystemMessage((Component)Component.translatable((String)"negationwand.destroyed", (Object[])new Object[]{name}));
    }
}

