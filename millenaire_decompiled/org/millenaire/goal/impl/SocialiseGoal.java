/*
 * Decompiled with CFR 0.150.
 * 
 * Could not load the following classes:
 *  javax.annotation.Nullable
 *  net.minecraft.core.BlockPos
 *  net.minecraft.network.chat.Component
 *  net.minecraft.resources.ResourceLocation
 *  net.minecraft.world.level.BlockGetter
 *  net.minecraft.world.level.Level
 */
package org.millenaire.goal.impl;

import java.util.ArrayList;
import java.util.concurrent.ThreadLocalRandom;
import javax.annotation.Nullable;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import org.millenaire.building.BuildingInstance;
import org.millenaire.goal.GoalContext;
import org.millenaire.goal.TaskLabels;
import org.millenaire.goal.VillagerGoal;
import org.millenaire.goal.VillagerTask;
import org.millenaire.goal.impl.ChatGoal;
import org.millenaire.goal.impl.WalkAndWaitTask;

public class SocialiseGoal
implements VillagerGoal {
    private static final ResourceLocation ID = ResourceLocation.fromNamespaceAndPath((String)"millenaire", (String)"socialise");
    private static final int MIN_WAIT_TICKS = 200;
    private static final int MAX_WAIT_TICKS = 400;

    @Override
    public boolean showInTravelBook() {
        return false;
    }

    @Override
    public ResourceLocation id() {
        return ID;
    }

    @Override
    public int computePriority(GoalContext context) {
        return 5;
    }

    @Override
    public boolean isLeisure() {
        return true;
    }

    @Override
    public boolean canStart(GoalContext context) {
        return true;
    }

    @Override
    public VillagerTask start(GoalContext context) {
        BlockPos target = SocialiseGoal.findLeisurePos(context);
        return new SocialiseTask(target);
    }

    @Nullable
    private static BlockPos findLeisurePos(GoalContext ctx) {
        BuildingInstance townhall;
        ArrayList<BuildingInstance> leisureBuildings = new ArrayList<BuildingInstance>(ctx.village().getOperationalBuildingsWithTag("leisure"));
        if (leisureBuildings.isEmpty() && (townhall = ctx.village().getTownhall()) != null) {
            leisureBuildings.add(townhall);
        }
        if (leisureBuildings.isEmpty()) {
            return null;
        }
        BuildingInstance chosen = (BuildingInstance)leisureBuildings.get(ThreadLocalRandom.current().nextInt(leisureBuildings.size()));
        BlockPos basePos = chosen.resolveNavigationTarget("leisurePos", "sellingPos", "sleepingPos");
        return SocialiseGoal.findRandomSafePosition((Level)ctx.level(), basePos);
    }

    static BlockPos findRandomSafePosition(Level level, BlockPos center) {
        ArrayList<BlockPos> safe = new ArrayList<BlockPos>();
        for (int dx = -1; dx <= 1; ++dx) {
            for (int dz = -1; dz <= 1; ++dz) {
                BlockPos pos = center.offset(dx, 0, dz);
                BlockPos below = pos.below();
                if (!level.getBlockState(below).isSolidRender((BlockGetter)level, below) || level.getBlockState(pos).isSuffocating((BlockGetter)level, pos) || level.getBlockState(pos.above()).isSuffocating((BlockGetter)level, pos.above())) continue;
                safe.add(pos);
            }
        }
        if (!safe.isEmpty()) {
            return (BlockPos)safe.get(ThreadLocalRandom.current().nextInt(safe.size()));
        }
        return center;
    }

    static class SocialiseTask
    extends WalkAndWaitTask {
        private boolean readyToChat;

        SocialiseTask(@Nullable BlockPos target) {
            super(target, ThreadLocalRandom.current().nextInt(200, 401));
        }

        @Override
        protected boolean allowRandomMoves() {
            return true;
        }

        @Override
        protected void tickAtDestination(GoalContext ctx) {
            if (!this.readyToChat && ChatGoal.isLowestAvailablePartner(ctx, ctx.villager())) {
                this.readyToChat = true;
            }
        }

        @Override
        public boolean isFinished() {
            return this.readyToChat || super.isFinished();
        }

        @Override
        public ResourceLocation goalId() {
            return ID;
        }

        @Override
        @Nullable
        public Component getGoalLabel() {
            return TaskLabels.labelForPhase(this.arrived, "socialise");
        }
    }
}

