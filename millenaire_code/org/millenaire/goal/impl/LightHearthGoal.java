/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  javax.annotation.Nullable
 *  net.minecraft.core.BlockPos
 *  net.minecraft.resources.ResourceLocation
 *  net.minecraft.server.level.ServerLevel
 *  net.minecraft.sounds.SoundEvents
 *  net.minecraft.sounds.SoundSource
 *  net.minecraft.world.InteractionHand
 *  net.minecraft.world.entity.LivingEntity
 *  net.minecraft.world.level.BlockGetter
 *  net.minecraft.world.level.block.CampfireBlock
 *  net.minecraft.world.level.block.state.BlockState
 *  net.minecraft.world.level.block.state.properties.Property
 *  net.minecraft.world.phys.AABB
 */
package org.millenaire.goal.impl;

import javax.annotation.Nullable;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.CampfireBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.Property;
import net.minecraft.world.phys.AABB;
import org.millenaire.building.BuildingInstance;
import org.millenaire.building.BuildingPlanSet;
import org.millenaire.building.HearthApproach;
import org.millenaire.culture.ModCultures;
import org.millenaire.entity.VillagerNavDriver;
import org.millenaire.goal.GoalContext;
import org.millenaire.goal.ProgressAwareTask;
import org.millenaire.goal.StopReason;
import org.millenaire.goal.VillagerGoal;
import org.millenaire.goal.VillagerTask;
import org.millenaire.village.HearthResidentResolver;

public class LightHearthGoal
implements VillagerGoal {
    public static final ResourceLocation ID = ResourceLocation.fromNamespaceAndPath((String)"millenaire", (String)"light_hearth");
    private static final int PRIORITY = 2000;
    static final long WINDOW_START = 1000L;
    static final long WINDOW_END = 3000L;
    private static final double ARRIVE_DISTANCE = 2.0;
    private static final double WALK_SPEED = 0.5;
    private static final int ACTION_DURATION = 20;
    private static final int MAX_OCCUPANT_RETRIES = 100;

    @Override
    public ResourceLocation id() {
        return ID;
    }

    @Override
    public int computePriority(GoalContext context) {
        if (!LightHearthGoal.isMorningWindow(context.dayTime())) {
            return 0;
        }
        if (!LightHearthGoal.isDesignatedResidentOfHearthHome(context)) {
            return 0;
        }
        if (LightHearthGoal.findUnlitHearth(context) == null) {
            return 0;
        }
        return 2000;
    }

    @Override
    public boolean canStart(GoalContext context) {
        return this.computePriority(context) > 0;
    }

    @Override
    public VillagerTask start(GoalContext context) {
        return new LightHearthTask();
    }

    @Override
    public boolean canBeDoneInDayTime() {
        return true;
    }

    @Override
    public boolean canBeDoneAtNight() {
        return false;
    }

    static boolean isMorningWindow(long dayTime) {
        long normalized = dayTime % 24000L;
        if (normalized < 0L) {
            normalized += 24000L;
        }
        return normalized >= 1000L && normalized <= 3000L;
    }

    static boolean isDesignatedResidentOfHearthHome(GoalContext ctx) {
        BuildingInstance home = ctx.resolveHomeBuilding().orElse(null);
        if (home == null) {
            return false;
        }
        if (home.getHearthPositions().isEmpty()) {
            return false;
        }
        return HearthResidentResolver.isDesignatedResident(ctx.village(), home, ctx.villager().getUUID());
    }

    @Nullable
    static BlockPos findUnlitHearth(GoalContext ctx) {
        BuildingInstance home = ctx.resolveHomeBuilding().orElse(null);
        if (home == null) {
            return null;
        }
        for (BlockPos pos : home.getHearthPositions()) {
            BlockState state = ctx.level().getBlockState(pos);
            if (!(state.getBlock() instanceof CampfireBlock) || !state.hasProperty((Property)CampfireBlock.LIT) || ((Boolean)state.getValue((Property)CampfireBlock.LIT)).booleanValue()) continue;
            return pos;
        }
        return null;
    }

    static class LightHearthTask
    extends ProgressAwareTask {
        private State state = State.WALKING;
        @Nullable
        private BlockPos target;
        @Nullable
        private BlockPos standPos;
        private int actionTicks;
        private int occupantRetries;

        LightHearthTask() {
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
                    this.tickActing(ctx);
                    break;
                }
            }
        }

        private void tickWalking(GoalContext ctx) {
            VillagerNavDriver nav;
            if (this.target == null) {
                this.target = LightHearthGoal.findUnlitHearth(ctx);
                if (this.target == null) {
                    this.state = State.DONE;
                    return;
                }
                this.standPos = HearthApproach.findStandPosition((BlockGetter)ctx.level(), this.target);
            }
            if ((nav = ctx.villager().getNavManager()).getDestination() == null) {
                nav.navigateTo(ctx.villager(), this.standPos, 0.5);
            }
            if (nav.isAbandoned()) {
                this.state = State.DONE;
                nav.stop(ctx.villager());
                return;
            }
            if (nav.isArrivedHorizontal(ctx.villager(), 2.0)) {
                nav.stop(ctx.villager());
                this.state = State.ACTING;
                this.actionTicks = 0;
            }
        }

        private void tickActing(GoalContext ctx) {
            ctx.villager().swing(InteractionHand.MAIN_HAND);
            ++this.actionTicks;
            if (this.actionTicks < 20) {
                return;
            }
            if (this.target == null) {
                this.state = State.DONE;
                return;
            }
            ServerLevel level = ctx.level();
            if (LightHearthTask.hasOtherOccupantOnHearth(level, this.target, (LivingEntity)ctx.villager())) {
                ++this.occupantRetries;
                if (this.occupantRetries >= 100) {
                    this.state = State.DONE;
                    return;
                }
                this.actionTicks = 19;
                return;
            }
            BlockState current = level.getBlockState(this.target);
            if (current.getBlock() instanceof CampfireBlock && current.hasProperty((Property)CampfireBlock.LIT) && !((Boolean)current.getValue((Property)CampfireBlock.LIT)).booleanValue()) {
                BlockState lit = (BlockState)current.setValue((Property)CampfireBlock.LIT, (Comparable)Boolean.valueOf(true));
                if (LightHearthTask.isTownHallHearth(ctx) && lit.hasProperty((Property)CampfireBlock.SIGNAL_FIRE)) {
                    lit = (BlockState)lit.setValue((Property)CampfireBlock.SIGNAL_FIRE, (Comparable)Boolean.valueOf(true));
                }
                level.setBlock(this.target, lit, 3);
                level.playSound(null, this.target, SoundEvents.FLINTANDSTEEL_USE, SoundSource.BLOCKS, 1.0f, level.getRandom().nextFloat() * 0.4f + 0.8f);
            }
            this.reportProgress();
            this.state = State.DONE;
        }

        static boolean hasOtherOccupantOnHearth(ServerLevel level, BlockPos hearth, LivingEntity actor) {
            AABB zone = new AABB((double)hearth.getX(), (double)hearth.getY(), (double)hearth.getZ(), (double)(hearth.getX() + 1), (double)(hearth.getY() + 2), (double)(hearth.getZ() + 1));
            for (LivingEntity e : level.getEntitiesOfClass(LivingEntity.class, zone)) {
                if (e == actor) continue;
                return true;
            }
            return false;
        }

        private static boolean isTownHallHearth(GoalContext ctx) {
            BuildingInstance home = ctx.resolveHomeBuilding().orElse(null);
            if (home == null || home.getPlanSetId() == null) {
                return false;
            }
            BuildingPlanSet planSet = ModCultures.getBuildingPlanSet(home.getPlanSetId());
            return planSet != null && planSet.isTownHall();
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

        private static enum State {
            WALKING,
            ACTING,
            DONE;

        }
    }
}

