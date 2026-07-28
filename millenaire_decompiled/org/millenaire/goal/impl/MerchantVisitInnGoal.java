/*
 * Decompiled with CFR 0.150.
 * 
 * Could not load the following classes:
 *  com.mojang.logging.LogUtils
 *  javax.annotation.Nullable
 *  net.minecraft.core.BlockPos
 *  net.minecraft.core.registries.BuiltInRegistries
 *  net.minecraft.network.chat.Component
 *  net.minecraft.resources.ResourceLocation
 *  net.minecraft.world.item.Item
 *  net.minecraft.world.level.Level
 *  org.slf4j.Logger
 */
package org.millenaire.goal.impl;

import com.mojang.logging.LogUtils;
import java.util.Map;
import javax.annotation.Nullable;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.Level;
import org.millenaire.building.BuildingInstance;
import org.millenaire.building.BuildingInventory;
import org.millenaire.commerce.TradeGoodsLoader;
import org.millenaire.entity.MillVillager;
import org.millenaire.entity.VillagerNavDriver;
import org.millenaire.goal.GoalContext;
import org.millenaire.goal.PerVillagerThrottle;
import org.millenaire.goal.StopReason;
import org.millenaire.goal.TaskLabels;
import org.millenaire.goal.TravelPhase;
import org.millenaire.goal.VillagerGoal;
import org.millenaire.goal.VillagerTask;
import org.millenaire.village.Village;
import org.slf4j.Logger;

public class MerchantVisitInnGoal
implements VillagerGoal {
    private static final Logger LOGGER = LogUtils.getLogger();
    public static final ResourceLocation ID = ResourceLocation.fromNamespaceAndPath((String)"millenaire", (String)"visit_inn");
    private static final int PRIORITY = 100;
    private static final int STANDARD_DELAY = 2000;
    private final PerVillagerThrottle throttle = new PerVillagerThrottle(2000, 24000L, 6000L);

    @Override
    public ResourceLocation id() {
        return ID;
    }

    @Override
    public int computePriority(GoalContext context) {
        return 100;
    }

    @Override
    public long reoccurDelayTicks() {
        return 2000L;
    }

    @Override
    public boolean canStart(GoalContext context) {
        long currentTick = context.level().getServer().getTickCount();
        boolean delayOver = !this.throttle.isThrottled(context.villager().getUUID(), currentTick);
        Village village = context.village();
        MillVillager villager = context.villager();
        Map<Item, Integer> targetQty = TradeGoodsLoader.getTargetQuantities(village.getCultureId());
        if (targetQty.isEmpty()) {
            return false;
        }
        BuildingInstance home = context.resolveHomeBuilding().orElse(null);
        if (home == null) {
            return false;
        }
        int totalExportGoods = 0;
        for (Item item : villager.getInventory().getAll().keySet()) {
            int carried = villager.getInventory().getCount(item);
            if (carried <= 0 || targetQty.containsKey((Object)item)) continue;
            totalExportGoods += carried;
            if (delayOver) {
                return true;
            }
            if (totalExportGoods <= 64) continue;
            return true;
        }
        BuildingInventory innInv = home.getInventory();
        if (innInv != null) {
            for (Map.Entry<Item, Integer> entry : targetQty.entrySet()) {
                int carried;
                Item item = entry.getKey();
                int innStock = innInv.getCount((Level)context.level(), item);
                if (innStock <= 0 || (carried = villager.getInventory().getCount(item)) >= entry.getValue()) continue;
                return true;
            }
        }
        return false;
    }

    @Override
    public VillagerTask start(GoalContext context) {
        this.throttle.record(context.villager().getUUID(), context.level().getServer().getTickCount());
        BuildingInstance home = context.resolveHomeBuilding().orElse(null);
        BlockPos target = home != null ? home.resolveNavigationTarget("sellingPos", "sleepingPos") : null;
        return new MerchantVisitInnTask(home, target);
    }

    private static int nbGoodNeeded(Village village, Item item) {
        return TradeGoodsLoader.getTargetQuantities(village.getCultureId()).getOrDefault((Object)item, 0);
    }

    static class MerchantVisitInnTask
    implements VillagerTask {
        private static final double ARRIVE_DISTANCE = 3.0;
        private static final double WALK_SPEED = 0.5;
        private static final String GOAL_KEY = "visit_inn";
        @Nullable
        private final BuildingInstance innBuilding;
        @Nullable
        private final BlockPos target;
        private boolean navStarted;
        private boolean arrived;
        private boolean actionDone;

        MerchantVisitInnTask(@Nullable BuildingInstance inn, @Nullable BlockPos target) {
            this.innBuilding = inn;
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
            if (this.innBuilding == null) {
                return;
            }
            Village village = ctx.village();
            MillVillager villager = ctx.villager();
            BuildingInventory innInv = this.innBuilding.getInventory();
            if (innInv == null) {
                return;
            }
            Map<Item, Integer> targetQty = TradeGoodsLoader.getTargetQuantities(village.getCultureId());
            StringBuilder exportLog = new StringBuilder();
            for (Item item : villager.getInventory().getAll().keySet().toArray((T[])new Item[0])) {
                int carried = villager.getInventory().getCount(item);
                if (carried <= 0 || targetQty.containsKey((Object)item)) continue;
                villager.getInventory().remove(item, carried);
                innInv.add((Level)ctx.level(), item, carried);
                if (!exportLog.isEmpty()) {
                    exportLog.append(";");
                }
                exportLog.append((Object)BuiltInRegistries.ITEM.getKey((Object)item)).append("/").append(carried);
            }
            if (!exportLog.isEmpty()) {
                String merchantName = villager.getVillagerDisplayName();
                this.innBuilding.addVisitorLog("storedexports;" + merchantName + ";" + String.valueOf(exportLog));
                LOGGER.debug("[Millenaire] Merchant {} stored exports at inn: {}", (Object)merchantName, (Object)exportLog);
            }
            StringBuilder importLog = new StringBuilder();
            for (Map.Entry<Item, Integer> entry : targetQty.entrySet()) {
                Item item;
                item = entry.getKey();
                int needed = entry.getValue();
                int carried = villager.getInventory().getCount(item);
                if (carried >= needed) continue;
                int toTake = needed - carried;
                int taken = innInv.remove((Level)ctx.level(), item, toTake);
                if (taken <= 0) continue;
                villager.getInventory().add(item, taken);
                if (!importLog.isEmpty()) {
                    importLog.append(";");
                }
                importLog.append((Object)BuiltInRegistries.ITEM.getKey((Object)item)).append("/").append(taken);
            }
            if (!importLog.isEmpty()) {
                String merchantName = villager.getVillagerDisplayName();
                this.innBuilding.addVisitorLog("broughtimport;" + merchantName + ";" + String.valueOf(importLog));
                LOGGER.debug("[Millenaire] Merchant {} took imports from inn: {}", (Object)merchantName, (Object)importLog);
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

