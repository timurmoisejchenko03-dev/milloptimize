/*
 * Decompiled with CFR 0.150.
 * 
 * Could not load the following classes:
 *  com.mojang.logging.LogUtils
 *  javax.annotation.Nullable
 *  net.minecraft.core.BlockPos
 *  net.minecraft.network.chat.Component
 *  net.minecraft.resources.ResourceLocation
 *  net.minecraft.world.item.Item
 *  net.minecraft.world.level.Level
 *  org.slf4j.Logger
 */
package org.millenaire.goal.impl;

import com.mojang.logging.LogUtils;
import java.util.List;
import java.util.Map;
import javax.annotation.Nullable;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.Level;
import org.millenaire.building.BuildingInstance;
import org.millenaire.building.BuildingInventory;
import org.millenaire.building.BuildingPlan;
import org.millenaire.commerce.TradeGood;
import org.millenaire.commerce.TradeGoodsLoader;
import org.millenaire.culture.ModCultures;
import org.millenaire.entity.MillVillager;
import org.millenaire.entity.VillagerNavDriver;
import org.millenaire.goal.GoalContext;
import org.millenaire.goal.StopReason;
import org.millenaire.goal.TaskLabels;
import org.millenaire.goal.TravelPhase;
import org.millenaire.goal.VillagerGoal;
import org.millenaire.goal.VillagerTask;
import org.millenaire.item.ItemHelper;
import org.millenaire.village.LocalMerchantHelper;
import org.millenaire.village.Village;
import org.slf4j.Logger;

public class MerchantVisitBuildingGoal
implements VillagerGoal {
    private static final Logger LOGGER = LogUtils.getLogger();
    public static final ResourceLocation ID = ResourceLocation.fromNamespaceAndPath((String)"millenaire", (String)"visit_building");
    private static final int PRIORITY = 100;
    @Nullable
    private TargetInfo cachedTarget;
    private long cachedTick = -1L;

    @Override
    public ResourceLocation id() {
        return ID;
    }

    @Override
    public int computePriority(GoalContext context) {
        return 100;
    }

    @Override
    public boolean canStart(GoalContext context) {
        this.cachedTarget = this.findDestination(context);
        this.cachedTick = context.level().getServer().getTickCount();
        return this.cachedTarget != null;
    }

    @Override
    public VillagerTask start(GoalContext context) {
        long currentTick = context.level().getServer().getTickCount();
        TargetInfo target = this.cachedTarget != null && this.cachedTick == currentTick ? this.cachedTarget : this.findDestination(context);
        this.cachedTarget = null;
        return new MerchantVisitBuildingTask(target != null ? target.building() : null, target != null ? target.pos() : null);
    }

    @Nullable
    private TargetInfo findDestination(GoalContext ctx) {
        Village village = ctx.village();
        MillVillager villager = ctx.villager();
        Map<Item, Integer> targetQty = TradeGoodsLoader.getTargetQuantities(village.getCultureId());
        if (targetQty.isEmpty()) {
            return null;
        }
        BuildingInstance townhall = village.getTownhall();
        if (townhall == null) {
            return null;
        }
        for (Map.Entry<Item, Integer> entry : targetQty.entrySet()) {
            int currentStock;
            Item item = entry.getKey();
            int carriedCount = villager.getInventory().getCount(item);
            if (carriedCount <= 0) continue;
            int n = currentStock = townhall.getInventory() != null ? townhall.getInventory().getCount((Level)ctx.level(), item) : 0;
            int needed = Math.max(entry.getValue() - currentStock, 0);
            if (needed <= 0) continue;
            BlockPos sellingPos = townhall.resolveNavigationTarget("sellingPos", "sleepingPos");
            return new TargetInfo(townhall, sellingPos);
        }
        Map<Item, Integer> neededGoods = LocalMerchantHelper.getImportsNeededByOtherVillages(ctx.level(), village);
        if (neededGoods.isEmpty()) {
            return null;
        }
        BuildingInstance home = ctx.resolveHomeBuilding().orElse(null);
        for (BuildingInstance shop : village.getBuildings()) {
            BuildingInventory shopInv;
            BuildingPlan shopPlan;
            if (!shop.isOperational() || (shopPlan = ModCultures.getBuildingPlan(shop.getPlanId())) == null || shopPlan.shopId() == null || shopPlan.hasTag("inn") || (shopInv = shop.getInventory()) == null) continue;
            for (Map.Entry<Item, Integer> entry : neededGoods.entrySet()) {
                int carriedCount;
                int homeStock;
                Item item = entry.getKey();
                int shopAvailable = shopInv.getCount((Level)ctx.level(), item);
                if (shopAvailable <= 0 || (homeStock = home != null && home.getInventory() != null ? home.getInventory().getCount((Level)ctx.level(), item) : 0) + (carriedCount = villager.getInventory().getCount(item)) >= entry.getValue()) continue;
                BlockPos shopPos = shop.resolveNavigationTarget("sellingPos", "sleepingPos");
                return new TargetInfo(shop, shopPos);
            }
        }
        return null;
    }

    private record TargetInfo(BuildingInstance building, BlockPos pos) {
    }

    static class MerchantVisitBuildingTask
    implements VillagerTask {
        private static final double ARRIVE_DISTANCE = 3.0;
        private static final double WALK_SPEED = 0.5;
        private static final String GOAL_KEY = "visit_building";
        @Nullable
        private final BuildingInstance targetBuilding;
        @Nullable
        private final BlockPos target;
        private boolean navStarted;
        private boolean arrived;
        private boolean actionDone;

        MerchantVisitBuildingTask(@Nullable BuildingInstance building, @Nullable BlockPos target) {
            this.targetBuilding = building;
            this.target = target;
            if (target == null) {
                this.arrived = true;
                this.actionDone = true;
            }
        }

        @Override
        public ResourceLocation goalId() {
            return ID;
        }

        @Override
        public void tick(GoalContext ctx) {
            if (!this.arrived) {
                VillagerNavDriver nav = ctx.villager().getNavManager();
                if (!this.navStarted) {
                    nav.navigateTo(ctx.villager(), this.target, 0.5);
                    this.navStarted = true;
                }
                if (nav.isArrived(ctx.villager(), 3.0)) {
                    nav.stop(ctx.villager());
                    this.arrived = true;
                } else if (nav.isAbandoned()) {
                    this.arrived = true;
                    this.actionDone = true;
                }
                return;
            }
            if (!this.actionDone) {
                this.performAction(ctx);
                this.actionDone = true;
            }
        }

        private void performAction(GoalContext ctx) {
            if (this.targetBuilding == null) {
                return;
            }
            Village village = ctx.village();
            MillVillager villager = ctx.villager();
            BuildingPlan plan = ModCultures.getBuildingPlan(this.targetBuilding.getPlanId());
            if (plan == null) {
                return;
            }
            List<TradeGood> goodsList = TradeGoodsLoader.getGoods(village.getCultureId());
            BuildingInventory shopInv = this.targetBuilding.getInventory();
            if (shopInv == null) {
                return;
            }
            if (plan.hasTag("townhalls") || village.getTownhall() == this.targetBuilding) {
                for (TradeGood good : goodsList) {
                    int carried;
                    int nbNeeded;
                    Item item;
                    if (good.isTag() || (item = ItemHelper.resolve(good.item())) == null || (nbNeeded = good.targetQuantity()) <= 0 || (carried = villager.getInventory().getCount(item)) <= 0) continue;
                    int toDeliver = Math.min(carried, nbNeeded);
                    villager.getInventory().remove(item, toDeliver);
                    shopInv.add((Level)ctx.level(), item, toDeliver);
                    LOGGER.debug("[Millenaire] Merchant {} delivered {} {} to TH", new Object[]{villager.getVillagerDisplayName(), toDeliver, item});
                }
                return;
            }
            Map<Item, Integer> neededGoods = LocalMerchantHelper.getImportsNeededByOtherVillages(ctx.level(), village);
            BuildingInstance home = ctx.resolveHomeBuilding().orElse(null);
            for (TradeGood good : goodsList) {
                int taken;
                int shopAvailable;
                Item item;
                if (good.isTag() || (item = ItemHelper.resolve(good.item())) == null || !neededGoods.containsKey((Object)item) || (shopAvailable = shopInv.getCount((Level)ctx.level(), item)) <= 0) continue;
                int homeStock = home != null && home.getInventory() != null ? home.getInventory().getCount((Level)ctx.level(), item) : 0;
                int carried = villager.getInventory().getCount(item);
                int needed = neededGoods.get((Object)item);
                int toTake = Math.min(shopAvailable, needed - homeStock - carried);
                if (toTake <= 0 || (taken = shopInv.remove((Level)ctx.level(), item, toTake)) <= 0) continue;
                villager.getInventory().add(item, taken);
                LOGGER.debug("[Millenaire] Merchant {} picked up {} {} from shop", new Object[]{villager.getVillagerDisplayName(), taken, item});
            }
        }

        @Override
        public boolean isFinished() {
            return this.arrived && this.actionDone;
        }

        @Override
        public void stop(GoalContext ctx, StopReason reason) {
        }

        @Override
        public TravelPhase getTravelPhase() {
            return TaskLabels.phaseFor(this.arrived);
        }

        @Override
        @Nullable
        public Component getGoalLabel() {
            return TaskLabels.labelForPhase(this.arrived, GOAL_KEY);
        }
    }
}

