/*
 * Decompiled with CFR 0.150.
 * 
 * Could not load the following classes:
 *  com.mojang.logging.LogUtils
 *  javax.annotation.Nullable
 *  net.minecraft.core.BlockPos
 *  net.minecraft.core.Direction
 *  net.minecraft.core.Vec3i
 *  net.minecraft.world.level.Level
 *  net.minecraft.world.level.block.DoorBlock
 *  net.minecraft.world.level.block.FenceGateBlock
 *  net.minecraft.world.level.block.HorizontalDirectionalBlock
 *  net.minecraft.world.level.block.state.BlockState
 *  net.minecraft.world.level.block.state.properties.Property
 *  net.minecraft.world.level.pathfinder.Path
 *  org.slf4j.Logger
 */
package org.millenaire.entity;

import com.mojang.logging.LogUtils;
import javax.annotation.Nullable;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Vec3i;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.DoorBlock;
import net.minecraft.world.level.block.FenceGateBlock;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.Property;
import net.minecraft.world.level.pathfinder.Path;
import org.millenaire.culture.ModCultures;
import org.millenaire.culture.VillagerType;
import org.millenaire.diagnostics.NavEvent;
import org.millenaire.diagnostics.NavigationCounters;
import org.millenaire.entity.MillVillager;
import org.millenaire.entity.VillagerNavDriver;
import org.millenaire.entity.VillagerNavigationManager;
import org.millenaire.goal.NavigationHelperUtils;
import org.millenaire.village.Village;
import org.slf4j.Logger;

public class LocalRecoveryNavDriver
implements VillagerNavDriver {
    private static final Logger LOGGER = LogUtils.getLogger();
    private static final int STUCK_COUNTER_MAX = 40;
    private static final float DISTANCE_MAX_MAX = 2.0f;
    private static final int RETRY_COOLDOWN_TICKS = 20;
    private static final int RETRY_MIN_STALL = 10;
    private static final double PROGRESS_EPSILON = 0.25;
    private static final int ESCAPE_STALL_TICKS = 20;
    private static final int ESCAPE_COOLDOWN_SUCCESS = 40;
    private static final int ESCAPE_COOLDOWN_FAIL = 25;
    private static final int ABORT_RETRY_COUNT = 3;
    private static final double ESCAPE_SPEED = 0.4;
    private static final boolean DELEGATE_LONG_HAUL_TO_WAYPOINTS = false;
    private static final int MAX_TELEPORTS = 3;
    private static final double DEST_CHANGE_THRESHOLD_SQ = 4.0;
    @Nullable
    private BlockPos target;
    private double speed = 0.5;
    private float acceptableRange = 2.0f;
    private boolean active = false;
    private double lastProgressDist = Double.MAX_VALUE;
    private int progressStallTicks = 0;
    private int retryCooldown = 20;
    private int retryCount = 0;
    private int localEscapeCooldown = 0;
    private int teleportCount = 0;
    private boolean abandoned = false;

    @Override
    public void navigateTo(MillVillager villager, BlockPos dest, double walkSpeed) {
        this.speed = walkSpeed;
        if (this.acceptableRange <= 0.0f) {
            this.acceptableRange = 2.0f;
        }
        if (this.active && !this.abandoned && this.target != null && this.target.distSqr((Vec3i)dest) <= 4.0) {
            this.setTarget(dest);
            if (villager.getNavigation().isDone()) {
                villager.getNavigation().moveTo((double)dest.getX() + 0.5, (double)dest.getY(), (double)dest.getZ() + 0.5, walkSpeed);
            }
            return;
        }
        this.setTarget(dest);
        this.lastProgressDist = this.distanceToTarget(villager);
        this.active = true;
        this.abandoned = false;
        this.retryCount = 0;
        this.progressStallTicks = 0;
        this.teleportCount = 0;
        if (this.isStandingInDoorOrGate(villager)) {
            this.handleDoorOrGate(villager, villager.blockPosition());
            this.retryCooldown = 0;
            this.recordStart(villager, dest, "start-in-door");
            return;
        }
        boolean success = villager.getNavigation().moveTo((double)dest.getX() + 0.5, (double)dest.getY(), (double)dest.getZ() + 0.5, walkSpeed);
        this.retryCooldown = success ? 20 : 0;
        this.recordStart(villager, dest, success ? "LOCAL" : "LOCAL no-path");
    }

    @Override
    public void tick(MillVillager villager, @Nullable Village village) {
        if (!this.active || this.target == null || this.abandoned) {
            return;
        }
        double distance3d = this.distanceToTarget(villager);
        if (this.isAtTargetInternal(villager, distance3d)) {
            return;
        }
        if (distance3d < this.lastProgressDist - 0.25) {
            this.lastProgressDist = distance3d;
            this.progressStallTicks = 0;
        } else {
            ++this.progressStallTicks;
        }
        if (this.shouldRetry(villager)) {
            this.lastProgressDist = distance3d;
            boolean success = villager.getNavigation().moveTo((double)this.target.getX() + 0.5, (double)this.target.getY(), (double)this.target.getZ() + 0.5, this.speed);
            villager.getNavEventLog().record(villager.level().getGameTime(), NavEvent.Layer.VNM, NavEvent.Type.REPATH, "local-recovery retry=" + this.retryCount + " ok=" + success);
        }
        if (this.localEscapeCooldown > 0) {
            --this.localEscapeCooldown;
        }
        if (this.progressStallTicks >= 20 && this.localEscapeCooldown == 0) {
            if (this.tryDoorOrGateEscape(villager)) {
                this.localEscapeCooldown = 40;
                this.progressStallTicks = 0;
                return;
            }
            this.localEscapeCooldown = 25;
        }
        if (this.progressStallTicks > 40 && this.retryCount > 3) {
            this.abort(villager);
        }
    }

    @Override
    public boolean isArrived(MillVillager villager, double arriveDistance) {
        if (this.target == null) {
            return true;
        }
        return villager.blockPosition().distSqr((Vec3i)this.target) <= arriveDistance * arriveDistance;
    }

    @Override
    public boolean isArrivedHorizontal(MillVillager villager, double arriveDistance) {
        if (this.target == null) {
            return true;
        }
        return NavigationHelperUtils.horizontalDistSq(villager.blockPosition(), this.target) <= arriveDistance * arriveDistance;
    }

    @Override
    public boolean isArrivedSameFloor(MillVillager villager, double arriveDistance) {
        if (this.target == null) {
            return true;
        }
        BlockPos pos = villager.blockPosition();
        if (Math.abs(pos.getY() - this.target.getY()) > 1) {
            return false;
        }
        return NavigationHelperUtils.horizontalDistSq(pos, this.target) <= arriveDistance * arriveDistance;
    }

    @Override
    public boolean isAbandoned() {
        return this.abandoned;
    }

    @Override
    public void stop(MillVillager villager) {
        villager.getNavigation().stop();
        this.active = false;
        this.abandoned = false;
        this.target = null;
        this.retryCooldown = 0;
        this.retryCount = 0;
        this.progressStallTicks = 0;
        this.localEscapeCooldown = 0;
        this.lastProgressDist = Double.MAX_VALUE;
        this.teleportCount = 0;
    }

    @Override
    @Nullable
    public BlockPos getDestination() {
        return this.target;
    }

    @Override
    public VillagerNavDriver.NavDiagnostics getDiagnostics() {
        return new VillagerNavDriver.NavDiagnostics(this.progressStallTicks, this.progressStallTicks, this.teleportCount, null);
    }

    private void setTarget(BlockPos pos) {
        this.target = pos;
    }

    private double distanceToTarget(MillVillager villager) {
        if (this.target == null) {
            return Double.MAX_VALUE;
        }
        double dx = villager.getX() - (double)this.target.getX();
        double dy = villager.getY() - (double)this.target.getY();
        double dz = villager.getZ() - (double)this.target.getZ();
        return Math.sqrt(dx * dx + dy * dy + dz * dz);
    }

    private boolean isAtTargetInternal(MillVillager villager, double distance3d) {
        if (this.target == null) {
            return false;
        }
        return LocalRecoveryNavDriver.isAtTargetGeometry(distance3d, villager.getX(), villager.getZ(), this.target, this.acceptableRange);
    }

    static boolean isAtTargetGeometry(double distance3d, double villagerX, double villagerZ, BlockPos target, float range) {
        double dz;
        if (distance3d <= (double)range - 0.1) {
            return true;
        }
        double dx = villagerX - ((double)target.getX() + 0.5);
        double horiz = Math.sqrt(dx * dx + (dz = villagerZ - ((double)target.getZ() + 0.5)) * dz);
        return horiz < (double)range;
    }

    private boolean shouldRetry(MillVillager villager) {
        if (this.retryCooldown-- > 0) {
            return false;
        }
        Path path = villager.getNavigation().getPath();
        if (path != null && !path.isDone()) {
            return false;
        }
        if (this.target == null) {
            return false;
        }
        if (this.progressStallTicks < 10) {
            return false;
        }
        this.retryCooldown = 20;
        ++this.retryCount;
        return true;
    }

    private boolean isStandingInDoorOrGate(MillVillager villager) {
        BlockState state = villager.level().getBlockState(villager.blockPosition());
        return state.getBlock() instanceof DoorBlock || state.getBlock() instanceof FenceGateBlock;
    }

    @Nullable
    private BlockPos findDoorOrGate(MillVillager villager) {
        int length;
        Path path = villager.getNavigation().getPath();
        if (path == null) {
            return null;
        }
        int ix = path.getNextNodeIndex();
        if (ix < (length = path.getNodeCount())) {
            BlockPos pos1 = path.getNode(ix).asBlockPos();
            if (LocalRecoveryNavDriver.isDoorOrGate(villager.level(), pos1)) {
                return pos1;
            }
            if (ix + 1 < length) {
                BlockPos pos2 = path.getNode(ix + 1).asBlockPos();
                if (LocalRecoveryNavDriver.isDoorOrGate(villager.level(), pos2)) {
                    return pos2;
                }
            }
        }
        return null;
    }

    private static boolean isDoorOrGate(Level level, BlockPos pos) {
        BlockState state = level.getBlockState(pos);
        return state.getBlock() instanceof DoorBlock || state.getBlock() instanceof FenceGateBlock;
    }

    private boolean tryDoorOrGateEscape(MillVillager villager) {
        BlockPos pos = this.findDoorOrGate(villager);
        if (pos == null) {
            return false;
        }
        this.handleDoorOrGate(villager, pos);
        return true;
    }

    private void handleDoorOrGate(MillVillager villager, BlockPos doorPos) {
        BlockState state = villager.level().getBlockState(doorPos);
        Direction facing = LocalRecoveryNavDriver.facingOf(state);
        BlockPos escapePos = LocalRecoveryNavDriver.computeEscapePos(facing, villager.getX(), villager.getZ(), doorPos);
        villager.getNavigation().stop();
        villager.getNavigation().moveTo((double)escapePos.getX() + 0.5, (double)escapePos.getY(), (double)escapePos.getZ() + 0.5, 0.4);
        this.retryCooldown = 20;
        villager.getNavEventLog().record(villager.level().getGameTime(), NavEvent.Layer.VNM, NavEvent.Type.STUCK_DETECTED, "door/gate nudge " + doorPos.toShortString() + "\u2192" + escapePos.toShortString());
    }

    private static Direction facingOf(BlockState state) {
        if (state.hasProperty((Property)HorizontalDirectionalBlock.FACING)) {
            return (Direction)state.getValue((Property)HorizontalDirectionalBlock.FACING);
        }
        return Direction.NORTH;
    }

    static BlockPos computeEscapePos(Direction facing, double mobX, double mobZ, BlockPos doorPos) {
        Direction escapeDir = facing == Direction.NORTH || facing == Direction.SOUTH ? (mobX < (double)doorPos.getX() ? Direction.WEST : Direction.EAST) : (mobZ < (double)doorPos.getZ() ? Direction.NORTH : Direction.SOUTH);
        return doorPos.relative(escapeDir);
    }

    private void abort(MillVillager villager) {
        if (this.target == null) {
            return;
        }
        if (!LocalRecoveryNavDriver.shouldAllowTeleport(villager)) {
            LOGGER.warn("[Millenaire] LocalRecoveryNavDriver \u2014 {} noTeleport forbids rescue, abandoning toward {}", (Object)villager.getVillagerTypeId(), (Object)this.target.toShortString());
            this.abandon(villager, "no-teleport");
            return;
        }
        if (this.teleportCount >= 3) {
            LOGGER.warn("[Millenaire] LocalRecoveryNavDriver \u2014 {} abandon after {} TPs toward {}", new Object[]{villager.getVillagerTypeId(), this.teleportCount, this.target.toShortString()});
            this.abandon(villager, "tp-cap");
            return;
        }
        if (!villager.level().isLoaded(this.target)) {
            this.abandon(villager, "dest-unloaded");
            return;
        }
        BlockPos beforeTP = villager.blockPosition();
        ++this.teleportCount;
        NavigationHelperUtils.teleportToSafe(villager, this.target);
        BlockPos afterTP = villager.blockPosition();
        NavigationCounters.incTeleport(NavEvent.Layer.VNM);
        villager.getNavEventLog().record(villager.level().getGameTime(), NavEvent.Layer.VNM, NavEvent.Type.TELEPORT, "local-recovery #" + this.teleportCount + " " + beforeTP.toShortString() + "\u2192" + afterTP.toShortString());
        if (beforeTP.distSqr((Vec3i)afterTP) < 4.0) {
            ++this.teleportCount;
        }
        this.progressStallTicks = 0;
        this.retryCount = 0;
        this.retryCooldown = 0;
        this.lastProgressDist = Double.MAX_VALUE;
    }

    private void abandon(MillVillager villager, String reason) {
        this.abandoned = true;
        this.active = false;
        villager.getNavEventLog().record(villager.level().getGameTime(), NavEvent.Layer.VNM, NavEvent.Type.GOAL_ABANDONED, "local-recovery " + reason + " dest=" + (this.target != null ? this.target.toShortString() : "null"));
        NavigationCounters.incGoalAbandoned();
    }

    private void recordStart(MillVillager villager, BlockPos dest, String mode) {
        villager.getNavEventLog().record(villager.level().getGameTime(), NavEvent.Layer.VNM, NavEvent.Type.NAV_START, "[local-recovery] " + mode + " dest=" + dest.toShortString());
    }

    private static boolean shouldAllowTeleport(MillVillager villager) {
        VillagerType vType = ModCultures.getVillagerType(villager.getVillagerTypeId());
        return VillagerNavigationManager.shouldAllowTeleport(vType, villager.isRaiderEntity());
    }
}

