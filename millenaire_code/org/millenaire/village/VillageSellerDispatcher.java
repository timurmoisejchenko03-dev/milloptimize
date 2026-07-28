/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.mojang.logging.LogUtils
 *  net.minecraft.core.BlockPos
 *  net.minecraft.core.Vec3i
 *  net.minecraft.resources.ResourceLocation
 *  net.minecraft.server.level.ServerLevel
 *  net.minecraft.server.level.ServerPlayer
 *  net.minecraft.world.entity.Entity
 *  net.minecraft.world.entity.player.Player
 *  net.minecraft.world.level.Level
 *  org.slf4j.Logger
 */
package org.millenaire.village;

import com.mojang.logging.LogUtils;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Vec3i;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import org.millenaire.TickConstants;
import org.millenaire.building.BuildingInstance;
import org.millenaire.building.BuildingPlan;
import org.millenaire.building.SpecialPoint;
import org.millenaire.culture.ModCultures;
import org.millenaire.culture.VillagerType;
import org.millenaire.entity.MillVillager;
import org.millenaire.goal.GoalContext;
import org.millenaire.goal.GoalScheduler;
import org.millenaire.goal.impl.BuildGoal;
import org.millenaire.goal.impl.SellerGoal;
import org.millenaire.village.Village;
import org.millenaire.village.VillagerAnnouncementHelper;
import org.slf4j.Logger;

public final class VillageSellerDispatcher {
    private static final Logger LOGGER = LogUtils.getLogger();

    private VillageSellerDispatcher() {
    }

    public static void checkSeller(ServerLevel level, Village village) {
        if (level.getGameTime() % 10L != 0L) {
            return;
        }
        VillageSellerDispatcher.cleanupActiveSellers(village);
        if (TickConstants.isNight((Level)level)) {
            return;
        }
        if (village.isUnderAttack()) {
            return;
        }
        if (!village.areChestsLocked()) {
            return;
        }
        BlockPos center = village.getCenter();
        Player nearestPlayer = level.getNearestPlayer((double)center.getX(), (double)center.getY(), (double)center.getZ(), 100.0, false);
        if (nearestPlayer == null) {
            return;
        }
        if (!(nearestPlayer instanceof ServerPlayer)) {
            return;
        }
        if (village.isControlledBy(nearestPlayer.getUUID())) {
            return;
        }
        int rep = village.getCombinedReputation(level, nearestPlayer.getUUID());
        if (rep < -1024) {
            return;
        }
        Map<BlockPos, MillVillager> activeSellers = village.getActiveSellers();
        for (BuildingInstance b : village.getBuildings()) {
            BuildingPlan plan;
            if (!b.isOperational() || (plan = ModCultures.getBuildingPlan(b.getPlanId())) == null || plan.shopId() == null) continue;
            List<SpecialPoint> sellingPoints = b.getPointsByType("sellingPos");
            if (sellingPoints.isEmpty()) {
                BlockPos fallback = b.getFirstPointPos("sleepingPos");
                if (fallback == null) continue;
                VillageSellerDispatcher.dispatchIfNeeded(level, village, b, fallback, nearestPlayer, activeSellers);
                continue;
            }
            for (SpecialPoint sp : sellingPoints) {
                VillageSellerDispatcher.dispatchIfNeeded(level, village, b, sp.pos(), nearestPlayer, activeSellers);
            }
        }
    }

    private static void dispatchIfNeeded(ServerLevel level, Village village, BuildingInstance shop, BlockPos sellingPos, Player player, Map<BlockPos, MillVillager> activeSellers) {
        if (activeSellers.containsKey(sellingPos)) {
            return;
        }
        double distSq = player.blockPosition().distSqr((Vec3i)sellingPos);
        if (distSq >= 9.0) {
            return;
        }
        MillVillager bestSeller = VillageSellerDispatcher.findBestSeller(level, village, sellingPos, activeSellers);
        if (bestSeller == null) {
            return;
        }
        GoalScheduler scheduler = bestSeller.getGoalScheduler();
        if (scheduler == null) {
            return;
        }
        GoalContext ctx = bestSeller.buildGoalContext();
        if (ctx == null) {
            return;
        }
        SellerGoal.SellerTask task = new SellerGoal.SellerTask(shop.getId(), sellingPos);
        scheduler.forceTask(task, ctx);
        activeSellers.put(sellingPos, bestSeller);
        VillagerAnnouncementHelper.sendAnnouncement(bestSeller, (ServerPlayer)player, "sellercoming", "message.millenaire.seller_coming");
        LOGGER.debug("[Millenaire] Seller {} sent to {} counter at {}", new Object[]{bestSeller.getVillagerTypeId(), shop.getPlanId(), sellingPos.toShortString()});
    }

    private static MillVillager findBestSeller(ServerLevel level, Village village, BlockPos sellingPos, Map<BlockPos, MillVillager> activeSellers) {
        MillVillager bestSeller = null;
        double bestDist = Double.MAX_VALUE;
        for (UUID uuid : village.getVillagerRecords().keySet()) {
            double dist;
            ResourceLocation currentGoal;
            GoalScheduler sched;
            VillagerType vtype;
            MillVillager mv;
            Entity entity = level.getEntity(uuid);
            if (!(entity instanceof MillVillager) || !(mv = (MillVillager)entity).isAlive() || (vtype = ModCultures.getVillagerType(mv.getVillagerTypeId())) == null || !vtype.hasTag("seller") || (sched = mv.getGoalScheduler()) != null && BuildGoal.ID.equals((Object)(currentGoal = sched.getCurrentGoalId())) || activeSellers.containsValue((Object)mv) || !((dist = mv.blockPosition().distSqr((Vec3i)sellingPos)) < bestDist)) continue;
            bestDist = dist;
            bestSeller = mv;
        }
        return bestSeller;
    }

    private static void cleanupActiveSellers(Village village) {
        Map<BlockPos, MillVillager> activeSellers = village.getActiveSellers();
        if (activeSellers.isEmpty()) {
            return;
        }
        Iterator<Map.Entry<BlockPos, MillVillager>> it = activeSellers.entrySet().iterator();
        while (it.hasNext()) {
            Map.Entry<BlockPos, MillVillager> entry = it.next();
            MillVillager seller = entry.getValue();
            if (!seller.isAlive() || seller.isRemoved()) {
                it.remove();
                continue;
            }
            GoalScheduler sched = seller.getGoalScheduler();
            if (sched != null && SellerGoal.ID.equals((Object)sched.getCurrentGoalId())) continue;
            it.remove();
        }
    }
}

