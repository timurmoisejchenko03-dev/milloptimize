/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.mojang.logging.LogUtils
 *  javax.annotation.Nullable
 *  net.minecraft.core.BlockPos
 *  net.minecraft.network.chat.Component
 *  net.minecraft.resources.ResourceLocation
 *  net.minecraft.world.entity.Entity
 *  net.minecraft.world.entity.player.Player
 *  net.minecraft.world.item.ItemStack
 *  net.minecraft.world.level.ItemLike
 *  org.slf4j.Logger
 */
package org.millenaire.goal.impl;

import com.mojang.logging.LogUtils;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;
import javax.annotation.Nullable;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ItemLike;
import org.millenaire.building.BuildingInstance;
import org.millenaire.building.SpecialPoint;
import org.millenaire.entity.MillVillager;
import org.millenaire.entity.VillagerNavDriver;
import org.millenaire.goal.GoalContext;
import org.millenaire.goal.StopReason;
import org.millenaire.goal.TaskLabels;
import org.millenaire.goal.TravelPhase;
import org.millenaire.goal.VillagerGoal;
import org.millenaire.goal.VillagerTask;
import org.millenaire.item.ModItems;
import org.millenaire.village.Village;
import org.slf4j.Logger;

public class ForeignMerchantKeepStallGoal
implements VillagerGoal {
    private static final Logger LOGGER = LogUtils.getLogger();
    public static final ResourceLocation ID = ResourceLocation.fromNamespaceAndPath((String)"millenaire", (String)"keep_stall");

    @Override
    public ResourceLocation id() {
        return ID;
    }

    @Override
    public int computePriority(GoalContext context) {
        return ThreadLocalRandom.current().nextInt(50);
    }

    @Override
    public boolean canStart(GoalContext context) {
        MillVillager villager = context.villager();
        if (villager.getForeignMerchantStallId() < 0) {
            return false;
        }
        Village village = context.village();
        if (village == null) {
            return false;
        }
        BuildingInstance home = village.getBuilding(villager.getHomeBuilding());
        if (home == null) {
            return false;
        }
        List<SpecialPoint> stalls = home.getPointsByType("stall");
        if (stalls.isEmpty()) {
            stalls = home.getPointsByType("sellingPos");
        }
        return villager.getForeignMerchantStallId() < stalls.size();
    }

    @Override
    public VillagerTask start(GoalContext context) {
        MillVillager villager = context.villager();
        Village village = context.village();
        BuildingInstance home = village.getBuilding(villager.getHomeBuilding());
        List<SpecialPoint> stalls = home.getPointsByType("stall");
        if (stalls.isEmpty()) {
            stalls = home.getPointsByType("sellingPos");
        }
        BlockPos stallPos = stalls.get(villager.getForeignMerchantStallId()).pos();
        return new KeepStallTask(stallPos);
    }

    public static class KeepStallTask
    implements VillagerTask {
        private static final double ARRIVE_DISTANCE = 3.0;
        private static final double WALK_SPEED = 0.5;
        private static final int ACTION_DURATION = 1200;
        private static final int END_CHANCE = 600;
        private final BlockPos stallPos;
        private State state = State.WALKING_TO_STALL;
        private int waitTicks;

        public KeepStallTask(BlockPos stallPos) {
            this.stallPos = stallPos;
        }

        @Override
        public ResourceLocation goalId() {
            return ID;
        }

        @Override
        public void tick(GoalContext ctx) {
            switch (this.state.ordinal()) {
                case 0: {
                    this.tickWalking(ctx);
                    break;
                }
                case 1: {
                    this.tickWaiting(ctx);
                    break;
                }
            }
        }

        private void tickWalking(GoalContext ctx) {
            VillagerNavDriver nav = ctx.villager().getNavManager();
            if (nav.getDestination() == null) {
                nav.navigateTo(ctx.villager(), this.stallPos, 0.5);
            }
            if (nav.isArrived(ctx.villager(), 3.0)) {
                nav.stop(ctx.villager());
                this.state = State.WAITING;
                this.waitTicks = 0;
                LOGGER.debug("[Millenaire] Foreign merchant {} arrived at stall {}", (Object)ctx.villager().getVillagerTypeId(), (Object)this.stallPos.toShortString());
                return;
            }
            if (nav.isAbandoned()) {
                nav.stop(ctx.villager());
                this.state = State.DONE;
                return;
            }
        }

        private void tickWaiting(GoalContext ctx) {
            ++this.waitTicks;
            Player nearest = ctx.level().getNearestPlayer((double)this.stallPos.getX() + 0.5, (double)this.stallPos.getY() + 0.5, (double)this.stallPos.getZ() + 0.5, 8.0, false);
            if (nearest != null) {
                ctx.villager().getLookControl().setLookAt((Entity)nearest, 30.0f, 30.0f);
            }
            if (this.waitTicks >= 1200) {
                if (ThreadLocalRandom.current().nextInt(600) == 0) {
                    this.state = State.DONE;
                }
                this.waitTicks = 0;
            }
        }

        @Override
        public boolean isFinished() {
            return this.state == State.DONE;
        }

        @Override
        public void stop(GoalContext ctx, StopReason reason) {
            if (ctx == null) {
                return;
            }
            ctx.villager().getNavManager().stop(ctx.villager());
        }

        @Override
        public List<ItemStack> getHeldItems(TravelPhase phase) {
            if (phase == TravelPhase.AT_DESTINATION) {
                return List.of(new ItemStack((ItemLike)ModItems.DENIER_ARGENT.get()));
            }
            return List.of();
        }

        @Override
        public List<ItemStack> getOffHandItems(TravelPhase phase) {
            if (phase == TravelPhase.AT_DESTINATION) {
                return List.of(new ItemStack((ItemLike)ModItems.PURSE.get()));
            }
            return List.of();
        }

        @Override
        public TravelPhase getTravelPhase() {
            return TaskLabels.phaseFor(this.state == State.WAITING);
        }

        @Override
        @Nullable
        public Component getGoalLabel() {
            return this.state == State.DONE ? null : TaskLabels.labelForPhase(this.state == State.WAITING, "keep_stall");
        }

        public BlockPos getStallPos() {
            return this.stallPos;
        }

        public boolean isAtStall() {
            return this.state == State.WAITING;
        }

        private static enum State {
            WALKING_TO_STALL,
            WAITING,
            DONE;

        }
    }
}

