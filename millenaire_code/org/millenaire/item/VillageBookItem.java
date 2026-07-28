/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.mojang.logging.LogUtils
 *  javax.annotation.Nullable
 *  net.minecraft.core.component.DataComponents
 *  net.minecraft.nbt.CompoundTag
 *  net.minecraft.network.chat.Component
 *  net.minecraft.network.protocol.common.custom.CustomPacketPayload
 *  net.minecraft.server.level.ServerLevel
 *  net.minecraft.server.level.ServerPlayer
 *  net.minecraft.world.InteractionHand
 *  net.minecraft.world.InteractionResultHolder
 *  net.minecraft.world.entity.player.Player
 *  net.minecraft.world.item.Item
 *  net.minecraft.world.item.Item$Properties
 *  net.minecraft.world.item.ItemStack
 *  net.minecraft.world.item.component.CustomData
 *  net.minecraft.world.level.Level
 *  net.neoforged.neoforge.network.PacketDistributor
 *  org.slf4j.Logger
 */
package org.millenaire.item;

import com.mojang.logging.LogUtils;
import java.util.List;
import java.util.UUID;
import javax.annotation.Nullable;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.level.Level;
import net.neoforged.neoforge.network.PacketDistributor;
import org.millenaire.network.MapData;
import org.millenaire.network.PanelContentPayload;
import org.millenaire.network.VillageBookPayload;
import org.millenaire.village.Village;
import org.millenaire.village.VillageBookService;
import org.millenaire.village.VillageId;
import org.millenaire.village.panel.PanelContent;
import org.millenaire.village.panel.PanelContentGenerator;
import org.slf4j.Logger;

public class VillageBookItem
extends Item {
    private static final Logger LOGGER = LogUtils.getLogger();
    private static final String TAG_VILLAGE_ID = "village_id";
    private static final String TAG_VILLAGE_NAME = "village_name";

    public VillageBookItem(Item.Properties properties) {
        super(properties);
    }

    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (level.isClientSide()) {
            return InteractionResultHolder.success((Object)stack);
        }
        if (!(player instanceof ServerPlayer)) {
            return InteractionResultHolder.fail((Object)stack);
        }
        ServerPlayer serverPlayer = (ServerPlayer)player;
        UUID villageUuid = VillageBookItem.getVillageId(stack);
        if (villageUuid == null) {
            serverPlayer.sendSystemMessage((Component)Component.translatable((String)"millenaire.scroll.error.village_not_found"));
            return InteractionResultHolder.fail((Object)stack);
        }
        ServerLevel serverLevel = (ServerLevel)level;
        Village village = Village.resolve(serverLevel, new VillageId(villageUuid));
        if (village == null) {
            serverPlayer.sendSystemMessage((Component)Component.translatable((String)"millenaire.scroll.error.village_not_found"));
            return InteractionResultHolder.fail((Object)stack);
        }
        try {
            boolean degraded = VillageBookService.isDegraded(village, serverLevel);
            ServerLevel effectiveLevel = degraded ? null : serverLevel;
            List<PanelContent> sections = VillageBookService.generateBookContent(village, effectiveLevel, serverPlayer);
            List<MapData.MapBuilding> mapBuildings = List.of();
            List<MapData.MapVillager> mapVillagers = List.of();
            int mapPlayerX = 0;
            int mapPlayerZ = 0;
            int mapCenterX = 0;
            int mapCenterZ = 0;
            MapData.MapTerrain mapTerrain = MapData.MapTerrain.EMPTY;
            List<MapData.MapPath> mapPaths = List.of();
            boolean hasMapData = false;
            if (!degraded) {
                PanelContent dummyContent = sections.get(0);
                PanelContentPayload mapPayload = PanelContentGenerator.createMapPayload(dummyContent, village, serverLevel, serverPlayer);
                mapBuildings = mapPayload.mapBuildings();
                mapVillagers = mapPayload.mapVillagers();
                mapPlayerX = mapPayload.mapPlayerX();
                mapPlayerZ = mapPayload.mapPlayerZ();
                mapCenterX = mapPayload.mapCenterX();
                mapCenterZ = mapPayload.mapCenterZ();
                mapTerrain = mapPayload.mapTerrain();
                mapPaths = mapPayload.mapPaths();
                hasMapData = mapPayload.hasMapData();
            }
            VillageBookPayload payload = new VillageBookPayload(village.getVillageName(), village.getCultureId().toString(), sections, mapBuildings, mapVillagers, mapPlayerX, mapPlayerZ, mapCenterX, mapCenterZ, mapTerrain, mapPaths, hasMapData, degraded);
            PacketDistributor.sendToPlayer((ServerPlayer)serverPlayer, (CustomPacketPayload)payload, (CustomPacketPayload[])new CustomPacketPayload[0]);
            LOGGER.debug("Scroll used by {} for village {}", (Object)player.getName().getString(), (Object)village.getVillageName());
        }
        catch (Exception e) {
            LOGGER.error("Error generating scroll for village {}", (Object)village.getVillageName(), (Object)e);
            serverPlayer.sendSystemMessage((Component)Component.translatable((String)"millenaire.scroll.error.generation_failed"));
        }
        return InteractionResultHolder.success((Object)stack);
    }

    public Component getName(ItemStack stack) {
        String villageName = VillageBookItem.getVillageName(stack);
        if (villageName != null) {
            return Component.translatable((String)"item.millenaire.village_scroll.named").append(villageName);
        }
        return super.getName(stack);
    }

    @Nullable
    public static UUID getVillageId(ItemStack stack) {
        CustomData customData = (CustomData)stack.get(DataComponents.CUSTOM_DATA);
        if (customData == null) {
            return null;
        }
        CompoundTag tag = customData.copyTag();
        if (!tag.contains(TAG_VILLAGE_ID)) {
            return null;
        }
        try {
            return UUID.fromString(tag.getString(TAG_VILLAGE_ID));
        }
        catch (IllegalArgumentException e) {
            return null;
        }
    }

    @Nullable
    public static String getVillageName(ItemStack stack) {
        CustomData customData = (CustomData)stack.get(DataComponents.CUSTOM_DATA);
        if (customData == null) {
            return null;
        }
        CompoundTag tag = customData.copyTag();
        if (!tag.contains(TAG_VILLAGE_NAME)) {
            return null;
        }
        String name = tag.getString(TAG_VILLAGE_NAME);
        return name.isEmpty() ? null : name;
    }
}

