/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.mojang.logging.LogUtils
 *  javax.annotation.Nullable
 *  net.minecraft.resources.ResourceLocation
 *  net.minecraft.world.level.Level
 *  org.slf4j.Logger
 */
package org.millenaire.goal;

import com.mojang.logging.LogUtils;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.annotation.Nullable;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.Level;
import org.millenaire.TickConstants;
import org.millenaire.diagnostics.NavEvent;
import org.millenaire.diagnostics.NavigationCounters;
import org.millenaire.goal.GoalContext;
import org.millenaire.goal.StopReason;
import org.millenaire.goal.VillagerGoal;
import org.millenaire.goal.VillagerTask;
import org.slf4j.Logger;

public class GoalScheduler {
    private static final Logger LOGGER = LogUtils.getLogger();
    private static final int MAX_TASK_TICKS = 6000;
    private static final int HARD_MAX_TASK_TICKS = 24000;
    private static final int MAX_IDLE_BACKOFF = 20;
    private final List<VillagerGoal> goals;
    private final Map<ResourceLocation, Long> lastGoalTick = new HashMap<ResourceLocation, Long>();
    @Nullable
    private VillagerGoal currentGoal;
    @Nullable
    private VillagerTask currentTask;
    private int taskTicks;
    private int taskAgeTicks;
    @Nullable
    private VillagerTask lastTickedTask;
    private int idleBackoff = 0;
    private int idleBackoffDelay = 1;

    public int getTaskTicks() {
        return this.taskTicks;
    }

    public int getMaxTaskTicks() {
        return 6000;
    }

    public GoalScheduler(List<VillagerGoal> goals) {
        this.goals = goals;
    }

    public void tick(@Nullable GoalContext ctx) {
        try {
            this.tickInternal(ctx);
        }
        catch (Exception e) {
            LOGGER.error("[Millenaire] Exception in GoalScheduler.tick() \u2014 goal={}, task={} \u2014 forced reset", new Object[]{this.currentGoal != null ? this.currentGoal.id() : "null", this.currentTask != null ? this.currentTask.goalId() : "null", e});
            this.currentGoal = null;
            this.currentTask = null;
            this.taskTicks = 0;
        }
    }

    private void tickInternal(@Nullable GoalContext ctx) {
        if (this.currentTask != null && this.currentTask.isFinished()) {
            this.currentTask.stop(ctx, StopReason.COMPLETED);
            if (ctx != null) {
                ctx.villager().getNavManager().stop(ctx.villager());
            }
            this.currentGoal = null;
            this.currentTask = null;
            this.taskTicks = 0;
            this.resetIdleBackoff();
        }
        if (this.currentTask != null && this.currentGoal != null && !GoalScheduler.isTimeAllowed(this.currentGoal, ctx)) {
            this.currentTask.stop(ctx, StopReason.INTERRUPTED);
            if (ctx != null) {
                ctx.villager().getNavManager().stop(ctx.villager());
            }
            this.currentGoal = null;
            this.currentTask = null;
            this.taskTicks = 0;
            this.resetIdleBackoff();
        }
        if (this.currentTask != null && this.currentGoal != null && this.currentGoal.isLeisure()) {
            for (VillagerGoal goal : this.goals) {
                if (goal.isLeisure() || !GoalScheduler.isTimeAllowed(goal, ctx) || !this.isCooldownExpired(goal.id(), goal.reoccurDelayTicks(), ctx != null ? ctx.gameTime() : 0L) || !goal.canStart(ctx)) continue;
                this.currentTask.stop(ctx, StopReason.INTERRUPTED);
                if (ctx != null) {
                    ctx.villager().getNavManager().stop(ctx.villager());
                }
                this.currentGoal = goal;
                this.currentTask = goal.start(ctx);
                this.lastGoalTick.put(goal.id(), ctx != null ? ctx.gameTime() : 0L);
                this.taskTicks = 0;
                this.resetIdleBackoff();
                break;
            }
        }
        if (this.currentTask != null && this.currentGoal != null) {
            boolean currentIsCombat = this.currentGoal.isCombatUrgent();
            int currentPriority = currentIsCombat ? this.currentGoal.computePriority(ctx) : Integer.MIN_VALUE;
            for (VillagerGoal goal : this.goals) {
                if (!goal.isCombatUrgent() || goal == this.currentGoal || currentIsCombat && goal.computePriority(ctx) <= currentPriority || !GoalScheduler.isTimeAllowed(goal, ctx) || !this.isCooldownExpired(goal.id(), goal.reoccurDelayTicks(), ctx != null ? ctx.gameTime() : 0L) || !goal.canStart(ctx)) continue;
                this.currentTask.stop(ctx, StopReason.INTERRUPTED);
                if (ctx != null) {
                    ctx.villager().getNavManager().stop(ctx.villager());
                }
                this.currentGoal = goal;
                this.currentTask = goal.start(ctx);
                this.lastGoalTick.put(goal.id(), ctx != null ? ctx.gameTime() : 0L);
                this.taskTicks = 0;
                this.resetIdleBackoff();
                break;
            }
        }
        if (this.currentTask == null) {
            if (this.idleBackoff > 0) {
                --this.idleBackoff;
            } else {
                BestGoal best = this.pickBest(ctx);
                if (best != null) {
                    this.currentGoal = best.goal();
                    this.currentTask = best.goal().start(ctx);
                    this.lastGoalTick.put(best.goal().id(), ctx != null ? ctx.gameTime() : 0L);
                    this.resetIdleBackoff();
                } else {
                    this.idleBackoff = this.idleBackoffDelay;
                    this.idleBackoffDelay = Math.min(this.idleBackoffDelay * 2, 20);
                }
            }
        }
        if (this.currentTask != null) {
            boolean hardCeiling;
            if (this.currentTask != this.lastTickedTask) {
                this.taskAgeTicks = 0;
                this.lastTickedTask = this.currentTask;
            }
            if (this.currentTask.consumeProgress()) {
                this.taskTicks = 0;
            }
            ++this.taskTicks;
            ++this.taskAgeTicks;
            boolean noProgressTimeout = this.taskTicks > 6000;
            boolean bl = hardCeiling = this.taskAgeTicks > 24000;
            if (noProgressTimeout || hardCeiling) {
                LOGGER.warn("Watchdog: {} (taskTicks={}, taskAgeTicks={}), goal={} \u2014 forced stop", new Object[]{hardCeiling ? "task exceeded absolute run ceiling" : "task without progress", this.taskTicks, this.taskAgeTicks, this.currentGoal != null ? this.currentGoal.id() : "null"});
                this.recordAbandon(ctx, hardCeiling ? "hard-ceiling age=" + this.taskAgeTicks : "watchdog ticks=" + this.taskTicks);
                this.currentTask.stop(ctx, StopReason.IMPOSSIBLE);
                if (ctx != null) {
                    ctx.villager().getNavManager().stop(ctx.villager());
                }
                this.currentGoal = null;
                this.currentTask = null;
                this.taskTicks = 0;
                this.lastTickedTask = null;
                this.taskAgeTicks = 0;
            } else {
                this.currentTask.tick(ctx);
            }
        }
    }

    private void resetIdleBackoff() {
        this.idleBackoff = 0;
        this.idleBackoffDelay = 1;
    }

    private boolean isCooldownExpired(ResourceLocation goalId, long cooldownTicks, long currentGameTime) {
        if (cooldownTicks <= 0L) {
            return true;
        }
        Long lastTick = this.lastGoalTick.get(goalId);
        if (lastTick == null) {
            return true;
        }
        return currentGameTime >= lastTick + cooldownTicks;
    }

    @Nullable
    private BestGoal pickBest(@Nullable GoalContext ctx) {
        VillagerGoal best = null;
        int bestPriority = Integer.MIN_VALUE;
        boolean bestIsLeisure = true;
        for (VillagerGoal goal : this.goals) {
            if (!GoalScheduler.isTimeAllowed(goal, ctx) || !this.isCooldownExpired(goal.id(), goal.reoccurDelayTicks(), ctx != null ? ctx.gameTime() : 0L) || !goal.canStart(ctx)) continue;
            int priority = goal.computePriority(ctx);
            boolean isLeisure = goal.isLeisure();
            if ((isLeisure || !bestIsLeisure) && (isLeisure != bestIsLeisure || priority <= bestPriority)) continue;
            bestPriority = priority;
            best = goal;
            bestIsLeisure = isLeisure;
        }
        return best != null ? new BestGoal(best, bestPriority) : null;
    }

    private static boolean isTimeAllowed(VillagerGoal goal, @Nullable GoalContext ctx) {
        if (ctx == null) {
            return true;
        }
        boolean isNight = TickConstants.isNight((Level)ctx.level());
        if (isNight && !goal.canBeDoneAtNight()) {
            return false;
        }
        return isNight || goal.canBeDoneInDayTime();
    }

    public void forceTask(VillagerTask task, @Nullable GoalContext ctx) {
        if (this.currentTask != null) {
            try {
                this.currentTask.stop(ctx, StopReason.INTERRUPTED);
                if (ctx != null) {
                    ctx.villager().getNavManager().stop(ctx.villager());
                }
            }
            catch (Exception e) {
                LOGGER.error("[Millenaire] Exception in stop() during forceTask \u2014 goal={}", this.currentGoal != null ? this.currentGoal.id() : "null", (Object)e);
            }
        }
        this.currentGoal = null;
        this.currentTask = task;
        this.taskTicks = 0;
        this.resetIdleBackoff();
    }

    public void forceStop(@Nullable GoalContext ctx) {
        if (this.currentTask != null) {
            this.recordAbandon(ctx, "force-stop");
            this.currentTask.stop(ctx, StopReason.IMPOSSIBLE);
            if (ctx != null) {
                ctx.villager().getNavManager().stop(ctx.villager());
            }
            LOGGER.info("forceStop: forced stop of goal {}", this.currentGoal != null ? this.currentGoal.id() : "null");
            this.currentGoal = null;
            this.currentTask = null;
            this.taskTicks = 0;
        }
    }

    private void recordAbandon(@Nullable GoalContext ctx, String detail) {
        String goalSuffix = " goal=" + String.valueOf(this.currentGoal != null ? this.currentGoal.id() : "forced");
        if (ctx != null) {
            ctx.villager().getNavEventLog().record(ctx.gameTime(), NavEvent.Layer.SCHEDULER, NavEvent.Type.GOAL_ABANDONED, detail + goalSuffix);
        }
        NavigationCounters.incGoalAbandoned();
    }

    @Nullable
    public VillagerGoal getCurrentGoal() {
        return this.currentGoal;
    }

    @Nullable
    public VillagerTask getCurrentTask() {
        return this.currentTask;
    }

    @Nullable
    public ResourceLocation getCurrentGoalId() {
        if (this.currentGoal != null) {
            return this.currentGoal.id();
        }
        if (this.currentTask != null) {
            return this.currentTask.goalId();
        }
        return null;
    }

    private record BestGoal(VillagerGoal goal, int priority) {
    }
}

