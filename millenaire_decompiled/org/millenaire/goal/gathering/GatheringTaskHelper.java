/*
 * Decompiled with CFR 0.150.
 * 
 * Could not load the following classes:
 *  javax.annotation.Nullable
 *  net.minecraft.core.BlockPos
 *  net.minecraft.core.BlockPos$MutableBlockPos
 *  net.minecraft.core.Vec3i
 *  net.minecraft.server.level.ServerLevel
 *  net.minecraft.world.level.block.state.BlockState
 */
package org.millenaire.goal.gathering;

import java.util.function.Predicate;
import javax.annotation.Nullable;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Vec3i;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.state.BlockState;

public final class GatheringTaskHelper {
    private static final int VERTICAL_SCAN_RANGE = 20;

    private GatheringTaskHelper() {
    }

    @Nullable
    public static BlockPos findNearestBlock(ServerLevel level, BlockPos center, int radius, Predicate<BlockState> predicate) {
        return GatheringTaskHelper.findNearestBlock(level, center, radius, predicate, null);
    }

    @Nullable
    public static BlockPos findNearestBlock(ServerLevel level, BlockPos center, int radius, Predicate<BlockState> statePredicate, @Nullable Predicate<BlockPos> posPredicate) {
        BlockPos best = null;
        double bestDistSq = Double.MAX_VALUE;
        int foundAtDist = -1;
        BlockPos.MutableBlockPos mutable = new BlockPos.MutableBlockPos();
        int cx = center.getX();
        int cy = center.getY();
        int cz = center.getZ();
        for (int d = 0; d <= radius && (foundAtDist < 0 || d <= foundAtDist); ++d) {
            double distSq;
            BlockState state;
            int y;
            if (d == 0) {
                for (int y2 = -20; y2 <= 20; ++y2) {
                    double distSq2;
                    BlockState state2;
                    mutable.set(cx, cy + y2, cz);
                    if (!level.isLoaded((BlockPos)mutable) || !statePredicate.test(state2 = level.getBlockState((BlockPos)mutable)) || posPredicate != null && !posPredicate.test((BlockPos)mutable) || !((distSq2 = center.distSqr((Vec3i)mutable)) < bestDistSq)) continue;
                    bestDistSq = distSq2;
                    best = mutable.immutable();
                    foundAtDist = d;
                }
                continue;
            }
            for (int x = -d; x <= d; ++x) {
                for (int side = -1; side <= 1; side += 2) {
                    int z = side * d;
                    for (y = -20; y <= 20; ++y) {
                        mutable.set(cx + x, cy + y, cz + z);
                        if (!level.isLoaded((BlockPos)mutable) || !statePredicate.test(state = level.getBlockState((BlockPos)mutable)) || posPredicate != null && !posPredicate.test((BlockPos)mutable) || !((distSq = center.distSqr((Vec3i)mutable)) < bestDistSq)) continue;
                        bestDistSq = distSq;
                        best = mutable.immutable();
                        foundAtDist = d;
                    }
                }
            }
            for (int z = -d + 1; z <= d - 1; ++z) {
                for (int side = -1; side <= 1; side += 2) {
                    int x = side * d;
                    for (y = -20; y <= 20; ++y) {
                        mutable.set(cx + x, cy + y, cz + z);
                        if (!level.isLoaded((BlockPos)mutable) || !statePredicate.test(state = level.getBlockState((BlockPos)mutable)) || posPredicate != null && !posPredicate.test((BlockPos)mutable) || !((distSq = center.distSqr((Vec3i)mutable)) < bestDistSq)) continue;
                        bestDistSq = distSq;
                        best = mutable.immutable();
                        foundAtDist = d;
                    }
                }
            }
        }
        return best;
    }

    public static boolean isStuck(BlockPos currentPos, @Nullable BlockPos lastPos, double thresholdSq) {
        if (lastPos == null) {
            return false;
        }
        return currentPos.distSqr((Vec3i)lastPos) < thresholdSq;
    }
}

