/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  javax.annotation.Nullable
 *  net.minecraft.core.BlockPos
 */
package org.millenaire.goal.impl;

import javax.annotation.Nullable;
import net.minecraft.core.BlockPos;
import org.millenaire.entity.VillagerNavDriver;
import org.millenaire.goal.GoalContext;
import org.millenaire.goal.ProgressAwareTask;
import org.millenaire.goal.StopReason;
import org.millenaire.goal.TravelPhase;

abstract class WalkThenActTask
extends ProgressAwareTask {
    protected static final double ARRIVE_DISTANCE = 3.0;
    protected static final double WALK_SPEED = 0.5;
    protected static final int DEFAULT_ACTION_DURATION = 40;
    protected Phase phase = Phase.WALKING;
    protected int actionTicks;

    WalkThenActTask() {
    }

    @Nullable
    protected abstract BlockPos resolveTarget(GoalContext var1);

    protected abstract void performAction(GoalContext var1);

    protected int actionDuration() {
        return 40;
    }

    @Override
    public void tick(GoalContext ctx) {
        switch (this.phase.ordinal()) {
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
        BlockPos target = this.resolveTarget(ctx);
        if (target == null) {
            this.phase = Phase.DONE;
            return;
        }
        VillagerNavDriver nav = ctx.villager().getNavManager();
        if (nav.getDestination() == null) {
            nav.navigateTo(ctx.villager(), target, 0.5);
        }
        if (nav.isArrivedSameFloor(ctx.villager(), 3.0)) {
            nav.stop(ctx.villager());
            this.phase = Phase.ACTING;
            this.actionTicks = 0;
            this.reportProgress();
            return;
        }
        if (nav.isAbandoned()) {
            nav.stop(ctx.villager());
            this.phase = Phase.DONE;
        }
    }

    private void tickActing(GoalContext ctx) {
        ++this.actionTicks;
        if (this.actionTicks < this.actionDuration()) {
            return;
        }
        this.performAction(ctx);
        this.phase = Phase.DONE;
    }

    @Override
    public boolean isFinished() {
        return this.phase == Phase.DONE;
    }

    @Override
    public void stop(GoalContext ctx, StopReason reason) {
        if (ctx == null) {
            return;
        }
        ctx.villager().getNavManager().stop(ctx.villager());
    }

    @Override
    public TravelPhase getTravelPhase() {
        return this.phase == Phase.WALKING ? TravelPhase.TRAVELLING : TravelPhase.AT_DESTINATION;
    }

    protected static enum Phase {
        WALKING,
        ACTING,
        DONE;

    }
}

