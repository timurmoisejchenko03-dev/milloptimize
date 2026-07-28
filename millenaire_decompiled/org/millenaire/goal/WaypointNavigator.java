/*
 * Decompiled with CFR 0.150.
 * 
 * Could not load the following classes:
 *  com.mojang.logging.LogUtils
 *  javax.annotation.Nullable
 *  net.minecraft.core.BlockPos
 *  net.minecraft.core.Vec3i
 *  net.minecraft.server.level.ServerLevel
 *  net.minecraft.world.level.BlockGetter
 *  net.minecraft.world.level.Level
 *  net.minecraft.world.level.pathfinder.Path
 *  org.slf4j.Logger
 */
package org.millenaire.goal;

import com.mojang.logging.LogUtils;
import java.util.List;
import javax.annotation.Nullable;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Vec3i;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.pathfinder.Path;
import org.millenaire.diagnostics.NavEvent;
import org.millenaire.diagnostics.NavigationCounters;
import org.millenaire.entity.MillVillager;
import org.millenaire.goal.NavigationHelperUtils;
import org.millenaire.village.Village;
import org.millenaire.village.VillageWaypointGraph;
import org.slf4j.Logger;

public class WaypointNavigator {
    private static final Logger LOGGER = LogUtils.getLogger();
    private static final double WAYPOINT_ARRIVE_DISTANCE_SQ = 9.0;
    private static final int STUCK_REPATH_TIMEOUT = 100;
    private static final int STUCK_TELEPORT_TIMEOUT = 600;
    private static final double STUCK_THRESHOLD_SQ = 0.25;
    private static final double WALK_SPEED = 0.5;
    private static final int MAX_TELEPORTS = 3;
    private static final long REBUILD_THROTTLE = 60L;
    private State state = State.IDLE;
    @Nullable
    private List<BlockPos> macroPath;
    private int currentWaypointIndex;
    @Nullable
    private BlockPos destination;
    private int stuckTicks;
    @Nullable
    private BlockPos lastPos;
    private int teleportCount;
    @Nullable
    private Village village;
    @Nullable
    private VillageWaypointGraph graph;
    private boolean firstLegRebuildAttempted;
    private boolean lastLegRebuildAttempted;

    public int getDebugStuckTicks() {
        return this.stuckTicks;
    }

    public int getDebugWaypointIndex() {
        return this.currentWaypointIndex;
    }

    public int getDebugPathSize() {
        return this.macroPath != null ? this.macroPath.size() : 0;
    }

    public int getDebugTeleportCount() {
        return this.teleportCount;
    }

    public void navigateTo(MillVillager villager, BlockPos dest, @Nullable Village village, @Nullable VillageWaypointGraph graph) {
        this.destination = dest;
        this.stuckTicks = 0;
        this.lastPos = null;
        this.currentWaypointIndex = 0;
        this.teleportCount = 0;
        this.village = village;
        this.graph = graph;
        this.firstLegRebuildAttempted = false;
        this.lastLegRebuildAttempted = false;
        BlockPos currentPos = villager.blockPosition();
        double dist = Math.sqrt(currentPos.distSqr((Vec3i)dest));
        if (dist <= 48.0 || graph == null || !graph.isAvailable()) {
            this.macroPath = null;
            this.state = State.NAVIGATING_TO_DESTINATION;
            villager.getNavigation().moveTo((double)dest.getX() + 0.5, (double)dest.getY(), (double)dest.getZ() + 0.5, 0.5);
            return;
        }
        List<BlockPos> path = graph.findPath(currentPos, dest);
        if (path.isEmpty()) {
            this.macroPath = null;
            this.state = State.NAVIGATING_TO_DESTINATION;
            villager.getNavigation().moveTo((double)dest.getX() + 0.5, (double)dest.getY(), (double)dest.getZ() + 0.5, 0.5);
            return;
        }
        this.macroPath = path;
        this.state = State.NAVIGATING_TO_WAYPOINT;
        BlockPos firstWp = this.macroPath.get(0);
        villager.getNavigation().moveTo((double)firstWp.getX() + 0.5, (double)firstWp.getY(), (double)firstWp.getZ() + 0.5, 0.5);
        LOGGER.debug("[Millenaire] Macro navigation started: {} waypoints to {}", (Object)path.size(), (Object)dest.toShortString());
    }

    public void tick(MillVillager villager) {
        switch (this.state.ordinal()) {
            case 1: {
                this.tickNavigatingToWaypoint(villager);
                break;
            }
            case 2: {
                this.tickNavigatingToDestination(villager);
                break;
            }
        }
    }

    public boolean isArrived() {
        return this.state == State.ARRIVED;
    }

    public boolean isDone() {
        return this.state == State.ARRIVED || this.state == State.ABANDONED;
    }

    public State getState() {
        return this.state;
    }

    private void tickNavigatingToWaypoint(MillVillager villager) {
        BlockPos targetWp;
        if (this.macroPath == null || this.currentWaypointIndex >= this.macroPath.size()) {
            this.switchToDestination(villager);
            return;
        }
        BlockPos currentPos = villager.blockPosition();
        if (currentPos.distSqr((Vec3i)(targetWp = this.macroPath.get(this.currentWaypointIndex))) <= 9.0) {
            ++this.currentWaypointIndex;
            this.stuckTicks = 0;
            this.lastPos = null;
            if (this.currentWaypointIndex >= this.macroPath.size()) {
                this.switchToDestination(villager);
            } else {
                BlockPos nextWp = this.macroPath.get(this.currentWaypointIndex);
                villager.getNavigation().moveTo((double)nextWp.getX() + 0.5, (double)nextWp.getY(), (double)nextWp.getZ() + 0.5, 0.5);
            }
            return;
        }
        this.stuckTicks = this.isStuck(currentPos) ? ++this.stuckTicks : 0;
        if (this.stuckTicks >= 100 && this.stuckTicks % 100 == 0 && this.stuckTicks < 600) {
            if (this.currentWaypointIndex == 0 && !this.firstLegRebuildAttempted) {
                this.firstLegRebuildAttempted = true;
                if (this.tryRebuildAndRecomputeMacro(villager, "first-leg")) {
                    return;
                }
            }
            LOGGER.debug("[Millenaire] Waypoint {} \u2014 stuck level 1, recalculating path", (Object)targetWp.toShortString());
            villager.getNavigation().stop();
            villager.getNavigation().moveTo((double)targetWp.getX() + 0.5, (double)targetWp.getY(), (double)targetWp.getZ() + 0.5, 0.5);
            return;
        }
        if (this.stuckTicks > 600) {
            if (this.teleportCount >= 3) {
                LOGGER.warn("[Millenaire] Navigation abandoned after {} teleportations \u2014 villager stuck", (Object)this.teleportCount);
                this.state = State.ABANDONED;
                this.stuckTicks = 0;
                villager.getNavEventLog().record(villager.level().getGameTime(), NavEvent.Layer.WAYPOINT, NavEvent.Type.GOAL_ABANDONED, "tp-cap wp=" + targetWp.toShortString());
                NavigationCounters.incGoalAbandoned();
                return;
            }
            villager.getNavEventLog().record(villager.level().getGameTime(), NavEvent.Layer.WAYPOINT, NavEvent.Type.STUCK_DETECTED, "wp=" + targetWp.toShortString() + " ticks=" + this.stuckTicks);
            this.teleportToSafe(villager, targetWp);
            ++this.teleportCount;
            NavigationCounters.incTeleport(NavEvent.Layer.WAYPOINT);
            villager.getNavEventLog().record(villager.level().getGameTime(), NavEvent.Layer.WAYPOINT, NavEvent.Type.TELEPORT, "wp=" + targetWp.toShortString() + " #" + this.teleportCount);
            LOGGER.debug("[Millenaire] Waypoint {} \u2014 stuck level 2, teleporting ({}/{})", new Object[]{targetWp.toShortString(), this.teleportCount, 3});
            ++this.currentWaypointIndex;
            this.stuckTicks = 0;
            this.lastPos = null;
            if (this.currentWaypointIndex >= this.macroPath.size()) {
                this.switchToDestination(villager);
            } else {
                BlockPos nextWp = this.macroPath.get(this.currentWaypointIndex);
                villager.getNavigation().moveTo((double)nextWp.getX() + 0.5, (double)nextWp.getY(), (double)nextWp.getZ() + 0.5, 0.5);
            }
            return;
        }
        if (villager.getNavigation().isDone()) {
            villager.getNavigation().moveTo((double)targetWp.getX() + 0.5, (double)targetWp.getY(), (double)targetWp.getZ() + 0.5, 0.5);
        }
    }

    private void tickNavigatingToDestination(MillVillager villager) {
        if (this.destination == null) {
            this.state = State.ARRIVED;
            return;
        }
        BlockPos currentPos = villager.blockPosition();
        this.stuckTicks = this.isStuck(currentPos) ? ++this.stuckTicks : 0;
        if (this.stuckTicks >= 100 && this.stuckTicks % 100 == 0 && this.stuckTicks < 600) {
            if (!this.lastLegRebuildAttempted) {
                this.lastLegRebuildAttempted = true;
                if (this.tryRebuildAndRecomputeMacro(villager, "last-leg")) {
                    return;
                }
            }
            LOGGER.debug("[Millenaire] Destination {} \u2014 stuck level 1, recalculating path", (Object)this.destination.toShortString());
            villager.getNavigation().stop();
            villager.getNavigation().moveTo((double)this.destination.getX() + 0.5, (double)this.destination.getY(), (double)this.destination.getZ() + 0.5, 0.5);
            return;
        }
        if (this.stuckTicks > 600) {
            if (this.teleportCount >= 3) {
                LOGGER.warn("[Millenaire] Navigation abandoned after {} teleportations \u2014 villager stuck", (Object)this.teleportCount);
                this.state = State.ABANDONED;
                this.stuckTicks = 0;
                villager.getNavEventLog().record(villager.level().getGameTime(), NavEvent.Layer.WAYPOINT, NavEvent.Type.GOAL_ABANDONED, "tp-cap dest=" + this.destination.toShortString());
                NavigationCounters.incGoalAbandoned();
                return;
            }
            villager.getNavEventLog().record(villager.level().getGameTime(), NavEvent.Layer.WAYPOINT, NavEvent.Type.STUCK_DETECTED, "dest=" + this.destination.toShortString() + " ticks=" + this.stuckTicks);
            this.teleportToSafe(villager, this.destination);
            ++this.teleportCount;
            NavigationCounters.incTeleport(NavEvent.Layer.WAYPOINT);
            villager.getNavEventLog().record(villager.level().getGameTime(), NavEvent.Layer.WAYPOINT, NavEvent.Type.TELEPORT, "dest=" + this.destination.toShortString() + " #" + this.teleportCount);
            LOGGER.debug("[Millenaire] Destination {} \u2014 stuck level 2, teleporting ({}/{})", new Object[]{this.destination.toShortString(), this.teleportCount, 3});
            this.stuckTicks = 0;
            this.lastPos = null;
            return;
        }
        if (villager.getNavigation().isDone()) {
            villager.getNavigation().moveTo((double)this.destination.getX() + 0.5, (double)this.destination.getY(), (double)this.destination.getZ() + 0.5, 0.5);
        }
    }

    private boolean tryRebuildAndRecomputeMacro(MillVillager villager, String reason) {
        BlockPos currentFirstWp;
        if (this.village == null || this.graph == null || this.destination == null) {
            return false;
        }
        Level level = villager.level();
        if (!(level instanceof ServerLevel)) {
            return false;
        }
        ServerLevel sl = (ServerLevel)level;
        boolean rebuilt = this.village.rebuildWaypointGraphIfStale(sl, 60L);
        List<BlockPos> newPath = this.graph.findPath(villager.blockPosition(), this.destination);
        if (newPath.isEmpty()) {
            return false;
        }
        BlockPos blockPos = currentFirstWp = this.macroPath != null && this.currentWaypointIndex < this.macroPath.size() ? this.macroPath.get(this.currentWaypointIndex) : null;
        if (currentFirstWp != null && currentFirstWp.equals((Object)newPath.get(0))) {
            return false;
        }
        BlockPos firstWp = newPath.get(0);
        if (!WaypointNavigator.isLegReachable(villager, villager.blockPosition(), firstWp)) {
            LOGGER.debug("[Millenaire] {} entry-leg {} -> {} unreachable, abandoning recompute", new Object[]{reason, villager.blockPosition().toShortString(), firstWp.toShortString()});
            return false;
        }
        BlockPos lastWp = newPath.get(newPath.size() - 1);
        if (!WaypointNavigator.isLegReachableFromPos(villager, lastWp, this.destination)) {
            LOGGER.debug("[Millenaire] {} exit-leg {} -> {} unreachable, abandoning recompute", new Object[]{reason, lastWp.toShortString(), this.destination.toShortString()});
            return false;
        }
        this.macroPath = newPath;
        this.currentWaypointIndex = 0;
        this.stuckTicks = 0;
        this.lastPos = null;
        this.firstLegRebuildAttempted = false;
        this.lastLegRebuildAttempted = false;
        this.state = State.NAVIGATING_TO_WAYPOINT;
        villager.getNavigation().stop();
        villager.getNavigation().moveTo((double)firstWp.getX() + 0.5, (double)firstWp.getY(), (double)firstWp.getZ() + 0.5, 0.5);
        villager.getNavEventLog().record(villager.level().getGameTime(), NavEvent.Layer.WAYPOINT, NavEvent.Type.NAV_START, "MACRO recompute (" + reason + ", rebuilt=" + rebuilt + ") wps=" + this.macroPath.size());
        LOGGER.debug("[Millenaire] WaypointNavigator \u2014 {} recompute, {} new waypoints", (Object)reason, (Object)this.macroPath.size());
        return true;
    }

    private static boolean isLegReachable(MillVillager villager, BlockPos from, BlockPos target) {
        Path probe = villager.getNavigation().createPath(target, 1);
        boolean reachable = probe != null && probe.canReach();
        return reachable;
    }

    private static boolean isLegReachableFromPos(MillVillager villager, BlockPos from, BlockPos target) {
        if (!villager.level().isLoaded(target)) {
            return false;
        }
        return from.distSqr((Vec3i)target) <= 4096.0;
    }

    private void switchToDestination(MillVillager villager) {
        this.state = State.NAVIGATING_TO_DESTINATION;
        this.stuckTicks = 0;
        this.lastPos = null;
        if (this.destination != null) {
            villager.getNavigation().moveTo((double)this.destination.getX() + 0.5, (double)this.destination.getY(), (double)this.destination.getZ() + 0.5, 0.5);
        }
    }

    private boolean isStuck(BlockPos currentPos) {
        if (this.lastPos == null) {
            this.lastPos = currentPos;
            return false;
        }
        double distSq = currentPos.distSqr((Vec3i)this.lastPos);
        if (distSq > 0.25) {
            this.lastPos = currentPos;
            return false;
        }
        return true;
    }

    private void teleportToSafe(MillVillager villager, BlockPos target) {
        Level level = villager.level();
        if (NavigationHelperUtils.isSafeLanding(level, target)) {
            villager.teleportTo((double)target.getX() + 0.5, target.getY(), (double)target.getZ() + 0.5);
            villager.getNavigation().stop();
            return;
        }
        int surfaceY = NavigationHelperUtils.villagerStandY(level, target.getX(), target.getZ());
        BlockPos surfacePos = new BlockPos(target.getX(), surfaceY, target.getZ());
        for (int nudge = 0; nudge < 5; ++nudge) {
            BlockPos candidate = surfacePos.above(nudge);
            if (level.getBlockState(candidate).isSuffocating((BlockGetter)level, candidate) || level.getBlockState(candidate.above()).isSuffocating((BlockGetter)level, candidate.above())) continue;
            villager.teleportTo((double)candidate.getX() + 0.5, candidate.getY(), (double)candidate.getZ() + 0.5);
            villager.getNavigation().stop();
            return;
        }
        villager.teleportTo((double)surfacePos.getX() + 0.5, surfacePos.getY(), (double)surfacePos.getZ() + 0.5);
        villager.getNavigation().stop();
    }

    public static final class State
    extends Enum<State> {
        public static final /* enum */ State IDLE = new State();
        public static final /* enum */ State NAVIGATING_TO_WAYPOINT = new State();
        public static final /* enum */ State NAVIGATING_TO_DESTINATION = new State();
        public static final /* enum */ State ARRIVED = new State();
        public static final /* enum */ State ABANDONED = new State();
        private static final /* synthetic */ State[] $VALUES;

        public static State[] values() {
            return (State[])$VALUES.clone();
        }

        public static State valueOf(String name) {
            return Enum.valueOf(State.class, name);
        }

        private static /* synthetic */ State[] $values() {
            return new State[]{IDLE, NAVIGATING_TO_WAYPOINT, NAVIGATING_TO_DESTINATION, ARRIVED, ABANDONED};
        }

        static {
            $VALUES = State.$values();
        }
    }
}

