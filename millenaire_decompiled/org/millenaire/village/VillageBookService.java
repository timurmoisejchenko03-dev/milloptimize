/*
 * Decompiled with CFR 0.150.
 * 
 * Could not load the following classes:
 *  com.mojang.logging.LogUtils
 *  javax.annotation.Nullable
 *  net.minecraft.core.Vec3i
 *  net.minecraft.core.component.DataComponents
 *  net.minecraft.nbt.CompoundTag
 *  net.minecraft.network.chat.Component
 *  net.minecraft.server.level.ServerLevel
 *  net.minecraft.server.level.ServerPlayer
 *  net.minecraft.world.entity.player.Player
 *  net.minecraft.world.item.ItemStack
 *  net.minecraft.world.item.component.CustomData
 *  net.minecraft.world.level.ItemLike
 *  net.neoforged.neoforge.network.handling.IPayloadContext
 *  org.slf4j.Logger
 */
package org.millenaire.village;

import com.mojang.logging.LogUtils;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import javax.annotation.Nullable;
import net.minecraft.core.Vec3i;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.level.ItemLike;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import org.millenaire.building.BuildingInstance;
import org.millenaire.entity.VillagerInteraction;
import org.millenaire.item.ModItems;
import org.millenaire.item.MoneyHelper;
import org.millenaire.network.VillageScrollPurchasePayload;
import org.millenaire.village.Village;
import org.millenaire.village.VillageId;
import org.millenaire.village.VillageSavedData;
import org.millenaire.village.panel.ConstructionPanelGenerator;
import org.millenaire.village.panel.MilitaryPanelGenerator;
import org.millenaire.village.panel.PanelContent;
import org.millenaire.village.panel.VillageOverviewPanelGenerator;
import org.slf4j.Logger;

public final class VillageBookService {
    private static final Logger LOGGER = LogUtils.getLogger();
    public static final int SCROLL_PRICE = 128;
    public static final int SCROLL_REPUTATION = 8192;
    public static final int REGEN_INTERVAL = 1000;
    private static final String TAG_VILLAGE_ID = "village_id";
    private static final String TAG_VILLAGE_NAME = "village_name";
    private static final double MAX_PURCHASE_DISTANCE_SQ = 4096.0;

    private VillageBookService() {
    }

    public static ItemStack createScrollForVillage(Village village) {
        ItemStack stack = new ItemStack((ItemLike)ModItems.VILLAGE_SCROLL.get());
        CompoundTag tag = new CompoundTag();
        tag.putString(TAG_VILLAGE_ID, village.getId().uuid().toString());
        tag.putString(TAG_VILLAGE_NAME, village.getVillageName());
        stack.set(DataComponents.CUSTOM_DATA, (Object)CustomData.of((CompoundTag)tag));
        return stack;
    }

    public static List<PanelContent> generateBookContent(Village village, @Nullable ServerLevel level, @Nullable ServerPlayer player) {
        ServerLevel effectiveLevel = level != null && VillageBookService.isDegraded(village, level) ? null : level;
        BuildingInstance townhall = village.getTownhall();
        ArrayList<PanelContent> sections = new ArrayList<PanelContent>(7);
        sections.add(VillageOverviewPanelGenerator.generateSummary(village, townhall, effectiveLevel));
        sections.add(VillageOverviewPanelGenerator.generatePopulation(village, effectiveLevel));
        sections.add(ConstructionPanelGenerator.generateConstructions(village, player));
        sections.add(ConstructionPanelGenerator.generateProjects(village, player));
        sections.add(ConstructionPanelGenerator.generateResources(village, effectiveLevel));
        sections.add(MilitaryPanelGenerator.generateMilitary(village, effectiveLevel));
        sections.add(VillageOverviewPanelGenerator.generateChronicle(village));
        return sections;
    }

    public static boolean isDegraded(Village village, ServerLevel level) {
        BuildingInstance townhall = village.getTownhall();
        if (townhall == null) {
            return true;
        }
        return !level.isLoaded(townhall.getOrigin());
    }

    public static void handleScrollPurchasePacket(VillageScrollPurchasePayload payload, IPayloadContext context) {
        context.enqueueWork(() -> {
            Player patt0$temp = context.player();
            if (patt0$temp instanceof ServerPlayer) {
                UUID villageUuid;
                ServerPlayer serverPlayer = (ServerPlayer)patt0$temp;
                try {
                    villageUuid = UUID.fromString(payload.villageId());
                }
                catch (IllegalArgumentException e) {
                    LOGGER.warn("Scroll purchase payload: invalid UUID '{}'", (Object)payload.villageId());
                    return;
                }
                VillageBookService.handleScrollPurchase(serverPlayer, villageUuid);
                VillagerInteraction.sendChiefRefresh(serverPlayer, serverPlayer.serverLevel(), payload.villageId());
            }
        });
    }

    public static void handleScrollPurchase(ServerPlayer player, UUID villageUuid) {
        ServerLevel level = player.serverLevel();
        VillageSavedData savedData = VillageSavedData.get(level);
        Village village = savedData.getVillageManager().getVillage(new VillageId(villageUuid));
        if (village == null) {
            player.sendSystemMessage((Component)Component.translatable((String)"millenaire.scroll.error.village_not_found"));
            LOGGER.warn("Scroll purchase failed: village {} not found", (Object)villageUuid);
            return;
        }
        double distSq = player.blockPosition().distSqr((Vec3i)village.getCenter());
        if (distSq > 4096.0) {
            LOGGER.warn("Scroll purchase denied: player {} too far from village {}", (Object)player.getName().getString(), (Object)villageUuid);
            return;
        }
        int rep = village.getCombinedReputation(level, player.getUUID());
        if (rep < 8192) {
            player.sendSystemMessage((Component)Component.translatable((String)"millenaire.scroll.error.reputation"));
            return;
        }
        if (!MoneyHelper.removeDeniers(player.getInventory(), 128)) {
            player.sendSystemMessage((Component)Component.translatable((String)"millenaire.scroll.error.money"));
            return;
        }
        ItemStack scroll = VillageBookService.createScrollForVillage(village);
        if (!player.getInventory().add(scroll)) {
            player.drop(scroll, false);
        }
        player.sendSystemMessage((Component)Component.translatable((String)"millenaire.scroll.purchased", (Object[])new Object[]{village.getVillageName()}));
        LOGGER.debug("Scroll purchased by {} for village {}", (Object)player.getName().getString(), (Object)village.getVillageName());
    }
}

