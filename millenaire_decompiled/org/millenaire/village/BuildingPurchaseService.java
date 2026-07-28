/*
 * Decompiled with CFR 0.150.
 * 
 * Could not load the following classes:
 *  com.mojang.logging.LogUtils
 *  javax.annotation.Nullable
 *  net.minecraft.core.Vec3i
 *  net.minecraft.network.chat.Component
 *  net.minecraft.resources.ResourceLocation
 *  net.minecraft.server.level.ServerLevel
 *  net.minecraft.server.level.ServerPlayer
 *  net.minecraft.world.entity.player.Player
 *  net.minecraft.world.level.Level
 *  net.neoforged.neoforge.network.handling.IPayloadContext
 *  org.slf4j.Logger
 */
package org.millenaire.village;

import com.mojang.logging.LogUtils;
import java.util.UUID;
import javax.annotation.Nullable;
import net.minecraft.core.Vec3i;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import org.millenaire.building.BuildingPlanSet;
import org.millenaire.culture.ModCultures;
import org.millenaire.culture.VillageType;
import org.millenaire.entity.VillagerInteraction;
import org.millenaire.item.MoneyHelper;
import org.millenaire.net.DonorStatusData;
import org.millenaire.network.BuildingPurchasePayload;
import org.millenaire.village.Village;
import org.millenaire.village.VillageGrowthManager;
import org.millenaire.village.VillageId;
import org.slf4j.Logger;

public final class BuildingPurchaseService {
    private static final Logger LOGGER = LogUtils.getLogger();
    private static final double MAX_PURCHASE_DISTANCE_SQ = 4096.0;

    private BuildingPurchaseService() {
    }

    public static void handlePurchasePacket(BuildingPurchasePayload payload, IPayloadContext context) {
        context.enqueueWork(() -> {
            Player patt0$temp = context.player();
            if (patt0$temp instanceof ServerPlayer) {
                ResourceLocation planSetId;
                UUID villageUuid;
                ServerPlayer serverPlayer = (ServerPlayer)patt0$temp;
                try {
                    villageUuid = UUID.fromString(payload.villageId());
                }
                catch (IllegalArgumentException e) {
                    LOGGER.warn("Building purchase payload: invalid UUID '{}'", (Object)payload.villageId());
                    return;
                }
                try {
                    planSetId = ResourceLocation.parse((String)payload.planSetId());
                }
                catch (Exception e) {
                    LOGGER.warn("Building purchase payload: invalid planSetId '{}'", (Object)payload.planSetId());
                    return;
                }
                BuildingPurchaseService.handlePurchase(serverPlayer, villageUuid, planSetId);
                VillagerInteraction.sendChiefRefresh(serverPlayer, serverPlayer.serverLevel(), payload.villageId());
            }
        });
    }

    private static void handlePurchase(ServerPlayer player, UUID villageUuid, ResourceLocation planSetId) {
        boolean isDonor;
        int playerMoney;
        Level level = player.level();
        if (!(level instanceof ServerLevel)) {
            return;
        }
        ServerLevel serverLevel = (ServerLevel)level;
        VillageId vId = new VillageId(villageUuid);
        Village village = Village.resolve(serverLevel, vId);
        if (village == null) {
            LOGGER.warn("Building purchase: village not found {}", (Object)villageUuid);
            return;
        }
        if (player.blockPosition().distSqr((Vec3i)village.getCenter()) > 4096.0) {
            LOGGER.warn("Building purchase: player {} too far from village", (Object)player.getName().getString());
            return;
        }
        int playerReputation = village.getCombinedReputation(serverLevel, player.getUUID());
        PurchaseResult result = BuildingPurchaseService.validatePurchase(village, planSetId, playerReputation, playerMoney = MoneyHelper.getTotalDeniers(player.getInventory()), isDonor = DonorStatusData.get(serverLevel).isDonor(player.getUUID()));
        if (!result.isOk()) {
            LOGGER.warn("Building purchase refused for {}: {}", (Object)player.getName().getString(), (Object)result.errorKey());
            player.sendSystemMessage((Component)Component.translatable((String)result.errorKey(), (Object[])result.args()));
            return;
        }
        BuildingPlanSet planSet = ModCultures.getBuildingPlanSet(planSetId);
        if (planSet == null) {
            return;
        }
        if (!planSet.isGift() && !MoneyHelper.removeDeniers(player.getInventory(), planSet.price())) {
            LOGGER.error("Failed to deduct {} deniers from {} despite pre-validation \u2014 aborting purchase of {}", new Object[]{planSet.price(), player.getName().getString(), planSetId});
            player.sendSystemMessage((Component)Component.translatable((String)"message.millenaire.purchase_failed"));
            return;
        }
        village.addBoughtBuilding(planSetId);
        village.setPendingProject(null);
        VillageGrowthManager.onBuildingCompleted(village);
        player.sendSystemMessage((Component)Component.translatable((String)"message.millenaire.building_purchased", (Object[])new Object[]{planSet.nativeName()}));
        LOGGER.info("Player {} purchased {} in village {}", new Object[]{player.getName().getString(), planSetId, village.getVillageName()});
    }

    static PurchaseResult validatePurchase(Village village, ResourceLocation planSetId, int playerReputation, int playerMoney, boolean isDonor) {
        VillageType villageType;
        BuildingPlanSet planSet = ModCultures.getBuildingPlanSet(planSetId);
        if (planSet == null) {
            return PurchaseResult.error("message.millenaire.purchase_plan_not_found", new Object[0]);
        }
        if (planSet.isGift()) {
            if (!isDonor) {
                return PurchaseResult.error("message.millenaire.purchase_not_donor", new Object[0]);
            }
        } else if (planSet.price() <= 0) {
            return PurchaseResult.error("message.millenaire.purchase_not_buyable", new Object[0]);
        }
        if ((villageType = ModCultures.getVillageType(village.getVillageTypeId())) == null || !villageType.playerBuildings().contains((Object)planSetId)) {
            return PurchaseResult.error("message.millenaire.purchase_not_available", new Object[0]);
        }
        if (village.isBuildingBought(planSetId)) {
            return PurchaseResult.error("message.millenaire.purchase_already_bought", new Object[0]);
        }
        boolean alreadyBuilt = village.getBuildings().stream().anyMatch(b -> planSetId.equals((Object)b.getPlanSetId()));
        if (alreadyBuilt) {
            return PurchaseResult.error("message.millenaire.purchase_already_built", new Object[0]);
        }
        if (playerReputation < planSet.reputation()) {
            return PurchaseResult.error("message.millenaire.purchase_no_rep", playerReputation, planSet.reputation());
        }
        if (!planSet.isGift() && playerMoney < planSet.price()) {
            return PurchaseResult.error("message.millenaire.purchase_no_money", playerMoney, planSet.price());
        }
        return PurchaseResult.ok();
    }

    record PurchaseResult(@Nullable String errorKey, Object[] args) {
        static PurchaseResult ok() {
            return new PurchaseResult(null, new Object[0]);
        }

        static PurchaseResult error(String key, Object ... args) {
            return new PurchaseResult(key, args);
        }

        boolean isOk() {
            return this.errorKey == null;
        }
    }
}

