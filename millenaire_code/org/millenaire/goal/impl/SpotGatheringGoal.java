/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  javax.annotation.Nullable
 *  net.minecraft.core.BlockPos
 *  net.minecraft.core.Vec3i
 *  net.minecraft.resources.ResourceLocation
 *  net.minecraft.server.level.ServerLevel
 *  net.minecraft.world.InteractionHand
 *  net.minecraft.world.item.Item
 *  net.minecraft.world.level.Level
 */
package org.millenaire.goal.impl;

import java.util.List;
import java.util.function.Supplier;
import javax.annotation.Nullable;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Vec3i;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.Level;
import org.millenaire.building.BuildingInstance;
import org.millenaire.building.BuildingInventory;
import org.millenaire.entity.VillagerNavDriver;
import org.millenaire.goal.GoalContext;
import org.millenaire.goal.GoalUtils;
import org.millenaire.goal.ProgressAwareTask;
import org.millenaire.goal.StopReason;
import org.millenaire.goal.VillagerGoal;
import org.millenaire.goal.VillagerTask;

public class SpotGatheringGoal
implements VillagerGoal {
    private final ResourceLocation id;
    private final String buildingTag;
    private final int maxConcurrent;
    private final int priorityBase;
    private final int priorityMultiplier;
    private final StockSource stockSource;
    @Nullable
    private final Supplier<Item> stockItem;
    private final int townhallLimit;
    private final SpotFinder spotFinder;
    private final SpotCondition spotCondition;
    private final SpotAction action;
    private final boolean swingHand;
    private static final long STANDARD_DELAY = 2000L;

    public SpotGatheringGoal(ResourceLocation id, String buildingTag, int maxConcurrent, int priorityBase, int priorityMultiplier, StockSource stockSource, @Nullable Supplier<Item> stockItem, int townhallLimit, SpotFinder spotFinder, SpotCondition spotCondition, SpotAction action, boolean swingHand) {
        this.id = id;
        this.buildingTag = buildingTag;
        this.maxConcurrent = maxConcurrent;
        this.priorityBase = priorityBase;
        this.priorityMultiplier = priorityMultiplier;
        this.stockSource = stockSource;
        this.stockItem = stockItem;
        this.townhallLimit = townhallLimit;
        this.spotFinder = spotFinder;
        this.spotCondition = spotCondition;
        this.action = action;
        this.swingHand = swingHand;
    }

    @Override
    public ResourceLocation id() {
        return this.id;
    }

    @Override
    public long reoccurDelayTicks() {
        return 2000L;
    }

    @Override
    public int computePriority(GoalContext ctx) {
        int p = this.priorityBase;
        if (this.priorityMultiplier != 0 && this.stockItem != null) {
            int stock = this.countStock(ctx);
            p = this.priorityBase - stock * this.priorityMultiplier;
        }
        int simultaneous = GoalUtils.countSimultaneous(ctx, this.id);
        for (int i = 0; i < simultaneous; ++i) {
            p /= 2;
        }
        return p;
    }

    @Override
    public boolean canStart(GoalContext ctx) {
        if (GoalUtils.countSimultaneous(ctx, this.id) >= this.maxConcurrent) {
            return false;
        }
        if (this.townhallLimit > 0 && this.stockItem != null && this.countStock(ctx) >= this.townhallLimit) {
            return false;
        }
        for (BuildingInstance building : this.findBuildings(ctx)) {
            for (BlockPos pos : this.spotFinder.getPositions(building)) {
                if (!this.spotCondition.test(ctx.level(), pos)) continue;
                return true;
            }
        }
        return false;
    }

    @Override
    public VillagerTask start(GoalContext ctx) {
        return new SpotGatheringTask();
    }

    private int countStock(GoalContext ctx) {
        return switch (this.stockSource.ordinal()) {
            default -> throw new MatchException(null, null);
            case 1 -> this.countTownhallStock(ctx);
            case 2 -> this.countFarmStock(ctx);
            case 0 -> 0;
        };
    }

    private int countTownhallStock(GoalContext ctx) {
        BuildingInstance townhall = ctx.village().getTownhall();
        if (townhall == null) {
            return 0;
        }
        BuildingInventory inv = townhall.getInventory();
        if (inv == null) {
            return 0;
        }
        return inv.getCount((Level)ctx.level(), this.stockItem.get());
    }

    private int countFarmStock(GoalContext ctx) {
        int count = 0;
        for (BuildingInstance b : this.findBuildings(ctx)) {
            if (b.getInventory() == null) continue;
            count += b.getInventory().getCount((Level)ctx.level(), this.stockItem.get());
        }
        return count;
    }

    private List<BuildingInstance> findBuildings(GoalContext ctx) {
        return ctx.village().getOperationalBuildingsWithTag(this.buildingTag);
    }

    public static enum StockSource {
        NONE,
        TOWNHALL,
        FARM;

    }

    @FunctionalInterface
    public static interface SpotFinder {
        public List<BlockPos> getPositions(BuildingInstance var1);
    }

    @FunctionalInterface
    public static interface SpotCondition {
        public boolean test(ServerLevel var1, BlockPos var2);
    }

    @FunctionalInterface
    public static interface SpotAction {
        public void perform(GoalContext var1, BlockPos var2);
    }

    private class SpotGatheringTask
    extends ProgressAwareTask {
        @Nullable
        private BlockPos targetSpot;
        private boolean navigating;
        private boolean finished;
        private int actionTicks;
        private static final int ACTION_DURATION = 20;

        private SpotGatheringTask() {
        }

        @Override
        public ResourceLocation goalId() {
            return SpotGatheringGoal.this.id;
        }

        @Override
        public void tick(GoalContext ctx) {
            if (this.finished) {
                return;
            }
            VillagerNavDriver nav = ctx.villager().getNavManager();
            if (this.targetSpot == null) {
                this.targetSpot = this.findClosestSpot(ctx);
                if (this.targetSpot == null) {
                    this.finished = true;
                    return;
                }
                this.navigating = true;
            }
            if (this.navigating) {
                if (nav.getDestination() == null) {
                    nav.navigateTo(ctx.villager(), this.targetSpot, 0.5);
                }
                if (nav.isAbandoned()) {
                    this.finished = true;
                    return;
                }
                if (!nav.isArrivedHorizontal(ctx.villager(), 3.0)) {
                    return;
                }
                nav.stop(ctx.villager());
                this.navigating = false;
                this.actionTicks = 0;
            }
            ++this.actionTicks;
            if (this.actionTicks < 20) {
                return;
            }
            SpotGatheringGoal.this.action.perform(ctx, this.targetSpot);
            if (SpotGatheringGoal.this.swingHand) {
                ctx.villager().swing(InteractionHand.MAIN_HAND);
            }
            this.reportProgress();
            this.targetSpot = this.findClosestSpot(ctx);
            if (this.targetSpot == null) {
                this.finished = true;
                return;
            }
            this.navigating = true;
        }

        @Override
        public boolean isFinished() {
            return this.finished;
        }

        @Override
        public void stop(GoalContext ctx, StopReason reason) {
            if (ctx != null) {
                ctx.villager().getNavManager().stop(ctx.villager());
            }
        }

        @Nullable
        private BlockPos findClosestSpot(GoalContext ctx) {
            BlockPos villagerPos = ctx.villager().blockPosition();
            BlockPos best = null;
            double bestDistSq = Double.MAX_VALUE;
            for (BuildingInstance building : SpotGatheringGoal.this.findBuildings(ctx)) {
                for (BlockPos pos : SpotGatheringGoal.this.spotFinder.getPositions(building)) {
                    double distSq;
                    if (!SpotGatheringGoal.this.spotCondition.test(ctx.level(), pos) || !((distSq = pos.distSqr((Vec3i)villagerPos)) < bestDistSq)) continue;
                    bestDistSq = distSq;
                    best = pos;
                }
            }
            return best;
        }
    }
}

