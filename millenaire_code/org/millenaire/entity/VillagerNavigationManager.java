/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.mojang.logging.LogUtils
 *  javax.annotation.Nullable
 *  net.minecraft.core.BlockPos
 *  net.minecraft.core.Vec3i
 *  net.minecraft.server.level.ServerLevel
 *  net.minecraft.world.level.BlockGetter
 *  net.minecraft.world.level.Level
 *  net.minecraft.world.level.pathfinder.Node
 *  net.minecraft.world.level.pathfinder.Path
 *  org.slf4j.Logger
 */
package org.millenaire.entity;

import com.mojang.logging.LogUtils;
import javax.annotation.Nullable;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Vec3i;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.pathfinder.Node;
import net.minecraft.world.level.pathfinder.Path;
import org.millenaire.combat.CombatHelper;
import org.millenaire.culture.ModCultures;
import org.millenaire.culture.VillagerType;
import org.millenaire.diagnostics.NavEvent;
import org.millenaire.diagnostics.NavigationCounters;
import org.millenaire.entity.MillVillager;
import org.millenaire.entity.VillagerNavDriver;
import org.millenaire.goal.NavigationHelperUtils;
import org.millenaire.goal.WaypointNavigator;
import org.millenaire.village.Village;
import org.millenaire.village.VillageId;
import org.millenaire.village.VillageWaypointGraph;
import org.slf4j.Logger;

public class VillagerNavigationManager
implements VillagerNavDriver {
    private static final Logger LOGGER = LogUtils.getLogger();
    private static final int LOCAL_STUCK_INCREMENT = 4;
    private static final int LOCAL_STUCK_REPATH = 30;
    private static final int LOCAL_STUCK_JUMP = 100;
    private static final double PROGRESS_THRESHOLD = 2.0E-4;
    private static final int LONG_STUCK_TELEPORT_DIRECT = 3000;
    private static final int LONG_STUCK_TELEPORT_MACRO = 3000;
    private static final int MAX_TELEPORTS = 3;
    private static final double DEST_CHANGE_THRESHOLD_SQ = 4.0;
    private static final int REPATH_INTERVAL = 10;
    private static final double WPN_ARRIVAL_DISTANCE_SQ = 25.0;
    @Nullable
    private BlockPos destination;
    private double speed = 0.5;
    private int localStuck;
    private double prevDistToNextNode = -1.0;
    private int longDistanceStuck;
    private double prevDistToDest = -1.0;
    @Nullable
    private WaypointNavigator waypointNavigator;
    private int teleportCount;
    private boolean abandoned;
    private int repathAttempts;
    private int consecutivePathFailures;
    static final int MAX_CONSECUTIVE_PATH_FAILURES = 3;
    private int pathFailLogCooldown;
    private int pathfindCooldown;
    static final int PATHFIND_COOLDOWN_TICKS = 15;
    private static final long WAYPOINT_REBUILD_THROTTLE = 60L;
    private static final int NO_PATH_STUCK_ACCELERATION = 5;
    private static final double UNDER_TARGET_HORIZ_SQ = 9.0;
    private static final int UNDER_TARGET_MIN_DY = 2;
    private static final int UNDER_TARGET_FAST_TICKS = 100;
    private static final int DEFAULT_FLOOR_TOLERANCE = 1;

    @Override
    public void navigateTo(MillVillager villager, BlockPos dest, double walkSpeed) {
        boolean macro;
        boolean forceReset = this.abandoned;
        if (!forceReset && this.destination != null && dest.distSqr((Vec3i)this.destination) <= 4.0) {
            this.destination = dest;
            this.speed = walkSpeed;
            if (this.waypointNavigator == null && villager.getNavigation().isDone()) {
                villager.getNavigation().moveTo((double)dest.getX() + 0.5, (double)dest.getY(), (double)dest.getZ() + 0.5, walkSpeed);
            }
            return;
        }
        this.destination = dest;
        this.speed = walkSpeed;
        this.localStuck = 0;
        this.longDistanceStuck = 0;
        this.prevDistToNextNode = -1.0;
        this.prevDistToDest = -1.0;
        this.teleportCount = 0;
        this.abandoned = false;
        this.waypointNavigator = null;
        this.pathfindCooldown = 0;
        this.repathAttempts = 0;
        this.consecutivePathFailures = 0;
        Village village = VillagerNavigationManager.resolveVillage(villager);
        VillageWaypointGraph graph = village != null ? village.getWaypointGraph() : null;
        double dist = Math.sqrt(villager.blockPosition().distSqr((Vec3i)dest));
        boolean bl = macro = dist > 48.0 && graph != null && graph.isAvailable();
        if (macro) {
            this.waypointNavigator = new WaypointNavigator();
            this.waypointNavigator.navigateTo(villager, dest, village, graph);
        } else {
            villager.getNavigation().moveTo((double)dest.getX() + 0.5, (double)dest.getY(), (double)dest.getZ() + 0.5, walkSpeed);
        }
        villager.getNavEventLog().record(villager.level().getGameTime(), NavEvent.Layer.VNM, NavEvent.Type.NAV_START, (macro ? "MACRO" : "DIRECT") + " dest=" + dest.toShortString() + " dist=" + String.format("%.1f", dist));
    }

    @Override
    public void tick(MillVillager villager, @Nullable Village village) {
        if (this.destination == null || this.abandoned) {
            return;
        }
        if (this.waypointNavigator != null) {
            this.tickWaypointNavigator(villager);
        } else if (villager.getNavigation().isDone()) {
            if (this.pathfindCooldown > 0) {
                --this.pathfindCooldown;
            } else {
                boolean ok = villager.getNavigation().moveTo((double)this.destination.getX() + 0.5, (double)this.destination.getY(), (double)this.destination.getZ() + 0.5, this.speed);
                if (ok) {
                    this.pathfindCooldown = 0;
                    this.consecutivePathFailures = 0;
                } else {
                    this.pathfindCooldown = 15;
                    ++this.consecutivePathFailures;
                    if (this.pathFailLogCooldown <= 0) {
                        LOGGER.debug("[Millenaire] NavManager \u2014 pathfind FAILED for {} from {} to {} (dist={}, consec={})", new Object[]{villager.getVillagerTypeId(), villager.blockPosition().toShortString(), this.destination.toShortString(), String.format("%.1f", Math.sqrt(villager.blockPosition().distSqr((Vec3i)this.destination))), this.consecutivePathFailures});
                        this.pathFailLogCooldown = 100;
                    }
                    if (this.consecutivePathFailures >= 3) {
                        this.tryPromoteToMacro(villager, "no-path\u00d7" + this.consecutivePathFailures);
                        this.consecutivePathFailures = 0;
                    }
                }
            }
            if (this.pathFailLogCooldown > 0) {
                --this.pathFailLogCooldown;
            }
        }
        this.tickLocalStuck(villager);
        this.tickLongDistanceStuck(villager);
    }

    private void tickLocalStuck(MillVillager villager) {
        Path path = villager.getNavigation().getPath();
        if (path == null || path.isDone() || path.getNextNodeIndex() >= path.getNodeCount()) {
            this.localStuck = 0;
            this.prevDistToNextNode = -1.0;
            return;
        }
        Node nextNode = path.getNode(path.getNextNodeIndex());
        double distToNext = villager.position().distanceToSqr((double)nextNode.x + 0.5, (double)nextNode.y + 0.5, (double)nextNode.z + 0.5);
        if (this.prevDistToNextNode >= 0.0) {
            double progress = this.prevDistToNextNode - distToNext;
            if (progress < 2.0E-4) {
                this.localStuck += 4;
            } else {
                this.localStuck = Math.max(0, this.localStuck - 1);
                if (this.localStuck == 0) {
                    this.repathAttempts = 0;
                }
            }
        }
        this.prevDistToNextNode = distToNext;
        if (this.localStuck > 30 && this.localStuck % 10 == 0 && this.localStuck <= 100) {
            villager.getNavigation().stop();
            villager.getNavEventLog().record(villager.level().getGameTime(), NavEvent.Layer.VNM, NavEvent.Type.REPATH, "local stuck=" + this.localStuck);
            ++this.repathAttempts;
            if (this.repathAttempts >= 2) {
                this.tryPromoteToMacro(villager, "after " + this.repathAttempts + " repath");
                this.repathAttempts = 0;
            }
        }
        if (this.localStuck > 100) {
            villager.getNavEventLog().record(villager.level().getGameTime(), NavEvent.Layer.VNM, NavEvent.Type.STUCK_DETECTED, "local stuck=" + this.localStuck);
            this.doShortJump(villager, path);
            this.localStuck = 0;
            this.prevDistToNextNode = -1.0;
            this.repathAttempts = 0;
        }
    }

    private void tryPromoteToMacro(MillVillager villager, String reason) {
        ServerLevel sl;
        boolean rebuilt;
        if (this.waypointNavigator != null) {
            return;
        }
        if (this.destination == null) {
            return;
        }
        BlockPos vp = villager.blockPosition();
        if (NavigationHelperUtils.horizontalDistSq(vp, this.destination) <= 9.0 && Math.abs(vp.getY() - this.destination.getY()) >= 2) {
            return;
        }
        Village village = VillagerNavigationManager.resolveVillage(villager);
        if (village == null) {
            return;
        }
        VillageWaypointGraph graph = village.getWaypointGraph();
        if (graph == null) {
            return;
        }
        Level level = villager.level();
        if (level instanceof ServerLevel && (rebuilt = village.rebuildWaypointGraphIfStale(sl = (ServerLevel)level, 60L))) {
            villager.getNavEventLog().record(villager.level().getGameTime(), NavEvent.Layer.VNM, NavEvent.Type.NAV_START, "GRAPH rebuilt (on-stuck) edges=" + graph.getEdges().size());
        }
        if (!graph.isAvailable()) {
            return;
        }
        villager.getNavEventLog().record(villager.level().getGameTime(), NavEvent.Layer.VNM, NavEvent.Type.NAV_START, "MACRO promote (" + reason + ") dest=" + this.destination.toShortString());
        this.waypointNavigator = new WaypointNavigator();
        this.waypointNavigator.navigateTo(villager, this.destination, village, graph);
        this.localStuck = 0;
        this.prevDistToNextNode = -1.0;
    }

    private void doShortJump(MillVillager villager, Path path) {
        BlockPos current = villager.blockPosition();
        for (int i = path.getNextNodeIndex(); i < path.getNodeCount(); ++i) {
            Level level;
            BlockPos next = path.getNode(i).asBlockPos();
            if (next.equals((Object)current) || current.distSqr((Vec3i)next) < 1.0 || (level = villager.level()).getBlockState(next).isSuffocating((BlockGetter)level, next) || level.getBlockState(next.above()).isSuffocating((BlockGetter)level, next.above())) continue;
            LOGGER.debug("[Millenaire] NavManager \u2014 short jump {} \u2192 {}", (Object)current.toShortString(), (Object)next.toShortString());
            villager.teleportTo((double)next.getX() + 0.5, next.getY(), (double)next.getZ() + 0.5);
            villager.getNavigation().stop();
            villager.getNavEventLog().record(villager.level().getGameTime(), NavEvent.Layer.VNM, NavEvent.Type.SHORT_JUMP, current.toShortString() + "\u2192" + next.toShortString());
            NavigationCounters.incShortJump();
            return;
        }
        LOGGER.debug("[Millenaire] NavManager \u2014 short jump: no safe node in path");
    }

    private void tickLongDistanceStuck(MillVillager villager) {
        int threshold;
        boolean underTargetDeadEnd;
        int increment;
        if (this.destination == null) {
            return;
        }
        double dx = villager.getX() - ((double)this.destination.getX() + 0.5);
        double dy = villager.getY() - (double)this.destination.getY();
        double dz = villager.getZ() - ((double)this.destination.getZ() + 0.5);
        double distToDest = dx * dx + dy * dy + dz * dz;
        boolean noPath = this.waypointNavigator == null && villager.getNavigation().getPath() == null && villager.getNavigation().isDone();
        int n = increment = noPath ? 5 : 1;
        if (this.prevDistToDest >= 0.0) {
            double progress = this.prevDistToDest - distToDest;
            this.longDistanceStuck = progress < 2.0E-4 ? (this.longDistanceStuck += increment) : Math.max(0, this.longDistanceStuck - 1);
        }
        this.prevDistToDest = distToDest;
        boolean bl = underTargetDeadEnd = dx * dx + dz * dz <= 9.0 && Math.abs(dy) >= 2.0;
        int n2 = underTargetDeadEnd ? 100 : (threshold = this.waypointNavigator != null ? 3000 : 3000);
        if (this.longDistanceStuck > threshold) {
            this.doLongDistanceTeleport(villager);
            this.longDistanceStuck = 0;
            this.prevDistToDest = -1.0;
        }
    }

    private void doLongDistanceTeleport(MillVillager villager) {
        if (this.destination == null) {
            return;
        }
        if (!VillagerNavigationManager.shouldAllowTeleport(villager)) {
            LOGGER.warn("[Millenaire] NavManager \u2014 {} noTeleport tag forbids TP rescue, abandoning toward {}", (Object)villager.getVillagerTypeId(), (Object)this.destination.toShortString());
            this.abandoned = true;
            villager.getNavEventLog().record(villager.level().getGameTime(), NavEvent.Layer.VNM, NavEvent.Type.GOAL_ABANDONED, "no-teleport dest=" + this.destination.toShortString());
            NavigationCounters.incGoalAbandoned();
            return;
        }
        if (this.teleportCount >= 3) {
            LOGGER.warn("[Millenaire] NavManager \u2014 {} abandon after {} TPs toward {}", new Object[]{villager.getVillagerTypeId(), this.teleportCount, this.destination.toShortString()});
            this.abandoned = true;
            villager.getNavEventLog().record(villager.level().getGameTime(), NavEvent.Layer.VNM, NavEvent.Type.GOAL_ABANDONED, "tp-cap dest=" + this.destination.toShortString());
            NavigationCounters.incGoalAbandoned();
            return;
        }
        if (!villager.level().isLoaded(this.destination)) {
            LOGGER.warn("[Millenaire] NavManager \u2014 {} destination not loaded, abandoning", (Object)villager.getVillagerTypeId());
            this.abandoned = true;
            villager.getNavEventLog().record(villager.level().getGameTime(), NavEvent.Layer.VNM, NavEvent.Type.GOAL_ABANDONED, "dest-unloaded=" + this.destination.toShortString());
            NavigationCounters.incGoalAbandoned();
            return;
        }
        BlockPos beforeTP = villager.blockPosition();
        ++this.teleportCount;
        villager.getNavEventLog().record(villager.level().getGameTime(), NavEvent.Layer.VNM, NavEvent.Type.STUCK_DETECTED, "long-dist #" + this.teleportCount + " ticks=" + this.longDistanceStuck);
        NavigationHelperUtils.teleportToSafe(villager, this.destination);
        BlockPos afterTP = villager.blockPosition();
        NavigationCounters.incTeleport(NavEvent.Layer.VNM);
        villager.getNavEventLog().record(villager.level().getGameTime(), NavEvent.Layer.VNM, NavEvent.Type.TELEPORT, "#" + this.teleportCount + " " + beforeTP.toShortString() + "\u2192" + afterTP.toShortString() + " dest=" + this.destination.toShortString());
        if (beforeTP.distSqr((Vec3i)afterTP) < 4.0) {
            LOGGER.warn("[Millenaire] NavManager \u2014 {} TP #{} ineffective (stayed at {}), dest={}", new Object[]{villager.getVillagerTypeId(), this.teleportCount, afterTP.toShortString(), this.destination.toShortString()});
            ++this.teleportCount;
        } else {
            LOGGER.info("[Millenaire] NavManager \u2014 {} TP #{} from {} to {} (dest={})", new Object[]{villager.getVillagerTypeId(), this.teleportCount, beforeTP.toShortString(), afterTP.toShortString(), this.destination.toShortString()});
        }
        this.localStuck = 0;
        this.prevDistToNextNode = -1.0;
    }

    private void tickWaypointNavigator(MillVillager villager) {
        this.waypointNavigator.tick(villager);
        if (this.destination != null && villager.blockPosition().distSqr((Vec3i)this.destination) <= 25.0) {
            this.waypointNavigator = null;
            return;
        }
        if (this.waypointNavigator.isDone()) {
            this.waypointNavigator = null;
            this.longDistanceStuck = 3001;
        }
    }

    @Override
    public boolean isArrived(MillVillager villager, double arriveDistance) {
        if (this.destination == null) {
            return true;
        }
        return villager.blockPosition().distSqr((Vec3i)this.destination) <= arriveDistance * arriveDistance;
    }

    @Override
    public boolean isArrivedHorizontal(MillVillager villager, double arriveDistance) {
        if (this.destination == null) {
            return true;
        }
        return NavigationHelperUtils.horizontalDistSq(villager.blockPosition(), this.destination) <= arriveDistance * arriveDistance;
    }

    @Override
    public boolean isArrivedSameFloor(MillVillager villager, double arriveDistance) {
        if (this.destination == null) {
            return true;
        }
        BlockPos pos = villager.blockPosition();
        if (Math.abs(pos.getY() - this.destination.getY()) > 1) {
            return false;
        }
        return NavigationHelperUtils.horizontalDistSq(pos, this.destination) <= arriveDistance * arriveDistance;
    }

    @Override
    public boolean isAbandoned() {
        return this.abandoned;
    }

    @Override
    public void stop(MillVillager villager) {
        this.destination = null;
        this.waypointNavigator = null;
        villager.getNavigation().stop();
        this.localStuck = 0;
        this.longDistanceStuck = 0;
        this.prevDistToNextNode = -1.0;
        this.prevDistToDest = -1.0;
        this.teleportCount = 0;
        this.abandoned = false;
        this.pathfindCooldown = 0;
    }

    @Override
    @Nullable
    public BlockPos getDestination() {
        return this.destination;
    }

    @Override
    public VillagerNavDriver.NavDiagnostics getDiagnostics() {
        return new VillagerNavDriver.NavDiagnostics(this.localStuck, this.longDistanceStuck, this.teleportCount, this.waypointNavigator != null ? this.waypointNavigator.getState().name() : null);
    }

    public int getLocalStuck() {
        return this.localStuck;
    }

    public int getLongDistanceStuck() {
        return this.longDistanceStuck;
    }

    public int getTeleportCount() {
        return this.teleportCount;
    }

    @Nullable
    public WaypointNavigator getWaypointNavigator() {
        return this.waypointNavigator;
    }

    int getPathfindCooldown() {
        return this.pathfindCooldown;
    }

    static boolean shouldAllowTeleport(MillVillager villager) {
        VillagerType vType = ModCultures.getVillagerType(villager.getVillagerTypeId());
        return VillagerNavigationManager.shouldAllowTeleport(vType, villager.isRaiderEntity());
    }

    static boolean shouldAllowTeleport(@Nullable VillagerType vType, boolean isRaider) {
        if (vType == null) {
            return true;
        }
        return CombatHelper.canTeleport(vType.isNoTeleport(), isRaider);
    }

    @Nullable
    private static Village resolveVillage(MillVillager villager) {
        VillageId vid = villager.getVillageId();
        if (vid == null) {
            return null;
        }
        Level level = villager.level();
        if (!(level instanceof ServerLevel)) {
            return null;
        }
        ServerLevel serverLevel = (ServerLevel)level;
        return Village.resolve(serverLevel, vid);
    }
}

