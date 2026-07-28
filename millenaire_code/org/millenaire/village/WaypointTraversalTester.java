/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.mojang.logging.LogUtils
 *  net.minecraft.core.BlockPos
 *  net.minecraft.core.Direction$Axis
 *  net.minecraft.server.level.ServerLevel
 *  net.minecraft.world.entity.Entity
 *  net.minecraft.world.entity.EntityType
 *  net.minecraft.world.entity.ai.attributes.AttributeInstance
 *  net.minecraft.world.entity.ai.attributes.Attributes
 *  net.minecraft.world.level.BlockGetter
 *  net.minecraft.world.level.Level
 *  net.minecraft.world.level.block.state.BlockState
 *  net.minecraft.world.level.pathfinder.Path
 *  net.minecraft.world.phys.shapes.VoxelShape
 *  org.jetbrains.annotations.Nullable
 *  org.slf4j.Logger
 */
package org.millenaire.village;

import com.mojang.logging.LogUtils;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.pathfinder.Path;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jetbrains.annotations.Nullable;
import org.millenaire.entity.MillVillager;
import org.millenaire.entity.ModEntities;
import org.millenaire.village.Village;
import org.slf4j.Logger;

public final class WaypointTraversalTester
implements AutoCloseable {
    private static final Logger LOGGER = LogUtils.getLogger();
    private static final double TEST_FOLLOW_RANGE = 64.0;
    private static final int PATH_ACCURACY = 1;
    private static final float ALMOST_REACHED_THRESHOLD = 8.0f;
    private final MillVillager testMob;
    private final Map<PairKey, Result> cache = new HashMap<PairKey, Result>();
    private boolean firstFailureLogged = false;

    public WaypointTraversalTester(ServerLevel level, @Nullable Village village) {
        MillVillager mob = (MillVillager)((EntityType)ModEntities.MILL_VILLAGER.get()).create((Level)level);
        if (mob != null) {
            AttributeInstance attr;
            mob.setNoAi(true);
            mob.setSilent(true);
            mob.setInvulnerable(true);
            if (village != null) {
                mob.setVillageId(village.getId());
            }
            if ((attr = mob.getAttribute(Attributes.FOLLOW_RANGE)) != null) {
                attr.setBaseValue(64.0);
            }
            if (!level.addFreshEntity((Entity)mob)) {
                LOGGER.warn("[Millenaire] WaypointTraversalTester: addFreshEntity rejected the test mob");
                mob = null;
            }
        }
        this.testMob = mob;
    }

    public WaypointTraversalTester(ServerLevel level) {
        this(level, null);
    }

    public Result findPath(BlockPos from, BlockPos to) {
        Result result;
        if (this.testMob == null) {
            return Result.PERMISSIVE;
        }
        PairKey key = WaypointTraversalTester.pairKey(from, to);
        Result cached = this.cache.get(key);
        if (cached != null) {
            return cached;
        }
        ServerLevel sl = (ServerLevel)this.testMob.level();
        BlockPos resolvedFrom = WaypointTraversalTester.snapToSubUnitFloor(from, sl);
        BlockPos resolvedTo = WaypointTraversalTester.snapToSubUnitFloor(to, sl);
        this.testMob.teleportTo((double)resolvedFrom.getX() + 0.5, resolvedFrom.getY(), (double)resolvedFrom.getZ() + 0.5);
        this.testMob.setOnGround(true);
        try {
            boolean accept;
            Path path = this.testMob.getNavigation().createPath(resolvedTo, 1);
            boolean bl = accept = path != null && (path.canReach() || path.getDistToTarget() < 8.0f);
            if (accept) {
                ArrayList<BlockPos> nodes = new ArrayList<BlockPos>(path.getNodeCount());
                for (int i = 0; i < path.getNodeCount(); ++i) {
                    nodes.add(path.getNode(i).asBlockPos());
                }
                result = new Result(true, Collections.unmodifiableList(nodes));
            } else {
                if (!this.firstFailureLogged) {
                    this.firstFailureLogged = true;
                    this.logFailureDetails(from, to, path, resolvedTo);
                }
                result = Result.FAIL;
            }
        }
        catch (Exception t) {
            LOGGER.warn("[Millenaire] WaypointTraversalTester pathfind threw: {}", (Object)t.toString());
            result = Result.PERMISSIVE;
        }
        this.cache.put(key, result);
        return result;
    }

    private static BlockPos snapToSubUnitFloor(BlockPos pos, ServerLevel level) {
        BlockState here = level.getBlockState(pos);
        if (!here.isAir()) {
            return pos;
        }
        BlockPos below = pos.below();
        BlockState belowState = level.getBlockState(below);
        if (belowState.isAir()) {
            return pos;
        }
        VoxelShape shape = belowState.getCollisionShape((BlockGetter)level, below);
        if (shape.isEmpty()) {
            return pos;
        }
        double maxY = shape.max(Direction.Axis.Y);
        return maxY < 0.999 ? below : pos;
    }

    @Override
    public void close() {
        if (this.testMob != null) {
            this.testMob.discard();
        }
    }

    private static PairKey pairKey(BlockPos a, BlockPos b) {
        long lb;
        long la = a.asLong();
        return la < (lb = b.asLong()) ? new PairKey(la, lb) : new PairKey(lb, la);
    }

    private void logFailureDetails(BlockPos from, BlockPos to, @Nullable Path path, BlockPos targetTo) {
        int nodeCount = path == null ? -1 : path.getNodeCount();
        boolean canReach = path != null && path.canReach();
        BlockPos mobPos = this.testMob.blockPosition();
        ServerLevel sl = (ServerLevel)this.testMob.level();
        LOGGER.info("[Millenaire] First pathfind failure: from={} to={} (resolvedTo={}) \u2192 path={}, nodeCount={}, canReach={}, mobPos={}, onGround={}, mob@={} mobBelow={}, target@={} targetBelow={}", new Object[]{from, to, targetTo, path, nodeCount, canReach, mobPos, this.testMob.onGround(), sl.getBlockState(mobPos), sl.getBlockState(mobPos.below()), sl.getBlockState(targetTo), sl.getBlockState(targetTo.below())});
    }

    public record Result(boolean reachable, List<BlockPos> nodes) {
        public static final Result FAIL = new Result(false, List.of());
        public static final Result PERMISSIVE = new Result(true, List.of());
    }

    private record PairKey(long lo, long hi) {
    }
}

