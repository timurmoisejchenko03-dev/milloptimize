/*
 * Decompiled with CFR 0.150.
 * 
 * Could not load the following classes:
 *  javax.annotation.Nullable
 *  net.minecraft.core.BlockPos
 *  net.minecraft.core.BlockPos$MutableBlockPos
 *  net.minecraft.world.level.block.Block
 *  net.minecraft.world.level.block.Blocks
 *  net.minecraft.world.level.block.SlabBlock
 *  net.minecraft.world.level.block.state.BlockState
 */
package org.millenaire.village.path;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;
import java.util.function.IntUnaryOperator;
import java.util.function.Predicate;
import java.util.function.ToIntBiFunction;
import java.util.function.ToIntFunction;
import javax.annotation.Nullable;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.SlabBlock;
import net.minecraft.world.level.block.state.BlockState;
import org.millenaire.village.path.PathEntry;
import org.millenaire.village.path.PathProfileOptimizer;
import org.millenaire.village.path.PathTerraformer;

public final class PathBlockPlacer {
    private static final BlockState DEFAULT_FOUNDATION = Blocks.DIRT.defaultBlockState();

    private PathBlockPlacer() {
    }

    public static boolean[] computeBuildMask(List<BlockPos> trace, Predicate<BlockPos> isProtectedFromPath) {
        return PathBlockPlacer.computeBuildMask(trace, isProtectedFromPath, pos -> false);
    }

    public static boolean[] computeBuildMask(List<BlockPos> trace, Predicate<BlockPos> isProtectedFromPath, Predicate<BlockPos> isHardProtected) {
        boolean[] shouldBuild = new boolean[trace.size()];
        Arrays.fill(shouldBuild, true);
        if (trace.size() <= 2) {
            for (int k = 0; k < trace.size(); ++k) {
                if (!isHardProtected.test(trace.get(k))) continue;
                shouldBuild[k] = false;
            }
            return shouldBuild;
        }
        int i = 0;
        while (i < trace.size()) {
            if (!isProtectedFromPath.test(trace.get(i))) {
                ++i;
                continue;
            }
            int runStart = i;
            while (i < trace.size() && isProtectedFromPath.test(trace.get(i))) {
                ++i;
            }
            int runEnd = i;
            boolean hasEntryBefore = runStart == 0 || !isProtectedFromPath.test(trace.get(runStart - 1));
            boolean hasExitAfter = runEnd == trace.size() || !isProtectedFromPath.test(trace.get(runEnd));
            boolean isTraversal = hasEntryBefore && hasExitAfter && runStart > 0 && runEnd < trace.size();
            if (isTraversal) continue;
            for (int j = runStart; j < runEnd; ++j) {
                if (j <= 0 || j >= trace.size() - 1) continue;
                shouldBuild[j] = false;
            }
        }
        for (int k = 0; k < trace.size(); ++k) {
            if (!isHardProtected.test(trace.get(k))) continue;
            shouldBuild[k] = false;
        }
        return shouldBuild;
    }

    public static void pruneInteriorFootprintTails(List<BlockPos> trace, boolean[] shouldBuild, Function<BlockPos, Object> footprintAt, Predicate<BlockPos> isStablePath) {
        int n = trace.size();
        for (int ip = 0; ip < n; ++ip) {
            Object l = footprintAt.apply(trace.get(ip));
            if (l == null) continue;
            if (ip == 0) {
                shouldBuild[ip] = true;
                PathBlockPlacer.clearPathDirectional(trace, shouldBuild, footprintAt, isStablePath, l, ip, 1);
                continue;
            }
            if (ip == n - 1) {
                shouldBuild[ip] = true;
                PathBlockPlacer.clearPathDirectional(trace, shouldBuild, footprintAt, isStablePath, l, ip, -1);
                continue;
            }
            if (!isStablePath.test(trace.get(ip))) continue;
            shouldBuild[ip] = true;
            PathBlockPlacer.clearPathDirectional(trace, shouldBuild, footprintAt, isStablePath, l, ip, -1);
            PathBlockPlacer.clearPathDirectional(trace, shouldBuild, footprintAt, isStablePath, l, ip, 1);
        }
    }

    private static void clearPathDirectional(List<BlockPos> trace, boolean[] shouldBuild, Function<BlockPos, Object> footprintAt, Predicate<BlockPos> isStablePath, Object l, int index, int step) {
        Object l2;
        BlockPos np;
        int i;
        int n = trace.size();
        boolean leadsToBorder = false;
        for (i = index + step; i >= 0 && i < n; i += step) {
            np = trace.get(i);
            l2 = footprintAt.apply(np);
            if (!Objects.equals(l2, l)) {
                leadsToBorder = true;
                break;
            }
            if (isStablePath.test(np)) break;
        }
        if (!leadsToBorder) {
            for (i = index + step; i >= 0 && i < n && Objects.equals(l2 = footprintAt.apply(np = trace.get(i)), l) && !isStablePath.test(np); i += step) {
                shouldBuild[i] = false;
            }
        }
    }

    public static List<WidenedColumn> computeWidenedColumns(List<BlockPos> centreTrace, int[] surfaceHalfY, int pathWidth, Predicate<BlockPos> canPlaceAt) {
        LinkedHashMap<Long, WidenedColumn> byXZ = new LinkedHashMap<Long, WidenedColumn>();
        for (int i = 0; i < centreTrace.size(); ++i) {
            BlockPos next;
            BlockPos prev;
            BlockPos node = centreTrace.get(i);
            int halfY = surfaceHalfY[i];
            PathBlockPlacer.addIfFree(byXZ, node.getX(), node.getZ(), halfY);
            boolean corner = PathBlockPlacer.isCorner(centreTrace, i);
            if (corner) {
                prev = i > 0 ? centreTrace.get(i - 1) : node;
                next = i < centreTrace.size() - 1 ? centreTrace.get(i + 1) : node;
                int dx1 = Integer.signum(node.getX() - prev.getX());
                int dz1 = Integer.signum(node.getZ() - prev.getZ());
                int dx2 = Integer.signum(next.getX() - node.getX());
                int dz2 = Integer.signum(next.getZ() - node.getZ());
                PathBlockPlacer.addPerpendicular(byXZ, node, halfY, dx1, dz1, canPlaceAt);
                PathBlockPlacer.addPerpendicular(byXZ, node, halfY, dx2, dz2, canPlaceAt);
                continue;
            }
            if (pathWidth <= 1) continue;
            prev = i > 0 ? centreTrace.get(i - 1) : node;
            next = i < centreTrace.size() - 1 ? centreTrace.get(i + 1) : node;
            int dx = Integer.signum(next.getX() - prev.getX());
            int dz = Integer.signum(next.getZ() - prev.getZ());
            if (dx != 0) {
                PathBlockPlacer.trySide(byXZ, node, halfY, 0, 1, canPlaceAt);
                continue;
            }
            if (dz == 0) continue;
            PathBlockPlacer.trySide(byXZ, node, halfY, 1, 0, canPlaceAt);
        }
        return new ArrayList<WidenedColumn>(byXZ.values());
    }

    private static void trySide(Map<Long, WidenedColumn> out, BlockPos node, int halfY, int ax, int az, Predicate<BlockPos> canPlaceAt) {
        BlockPos preferred = node.offset(ax, 0, az);
        BlockPos alternate = node.offset(-ax, 0, -az);
        if (canPlaceAt.test(preferred)) {
            PathBlockPlacer.addIfFree(out, preferred.getX(), preferred.getZ(), halfY);
        } else if (canPlaceAt.test(alternate)) {
            PathBlockPlacer.addIfFree(out, alternate.getX(), alternate.getZ(), halfY);
        }
    }

    private static void addPerpendicular(Map<Long, WidenedColumn> out, BlockPos node, int halfY, int dx, int dz, Predicate<BlockPos> canPlaceAt) {
        BlockPos p2;
        BlockPos p1;
        if (dx != 0) {
            p1 = node.offset(0, 0, 1);
            p2 = node.offset(0, 0, -1);
            if (canPlaceAt.test(p1)) {
                PathBlockPlacer.addIfFree(out, p1.getX(), p1.getZ(), halfY);
            }
            if (canPlaceAt.test(p2)) {
                PathBlockPlacer.addIfFree(out, p2.getX(), p2.getZ(), halfY);
            }
        }
        if (dz != 0) {
            p1 = node.offset(1, 0, 0);
            p2 = node.offset(-1, 0, 0);
            if (canPlaceAt.test(p1)) {
                PathBlockPlacer.addIfFree(out, p1.getX(), p1.getZ(), halfY);
            }
            if (canPlaceAt.test(p2)) {
                PathBlockPlacer.addIfFree(out, p2.getX(), p2.getZ(), halfY);
            }
        }
    }

    private static void addIfFree(Map<Long, WidenedColumn> out, int x, int z, int halfY) {
        long key = (long)x << 32 | (long)z & 0xFFFFFFFFL;
        out.putIfAbsent(key, new WidenedColumn(x, z, halfY));
    }

    private static boolean isCorner(List<BlockPos> trace, int i) {
        if (i <= 0 || i >= trace.size() - 1) {
            return false;
        }
        BlockPos prev = trace.get(i - 1);
        BlockPos node = trace.get(i);
        BlockPos next = trace.get(i + 1);
        int dx1 = Integer.signum(node.getX() - prev.getX());
        int dz1 = Integer.signum(node.getZ() - prev.getZ());
        int dx2 = Integer.signum(next.getX() - node.getX());
        int dz2 = Integer.signum(next.getZ() - node.getZ());
        return dx1 != dx2 || dz1 != dz2;
    }

    public static boolean canPlaceSurfaceAt(int x, int z, int effectiveHalfY, boolean hasSlabVariant, Predicate<BlockPos> isReplaceable) {
        return PathBlockPlacer.canPlaceSurfaceAt(x, z, effectiveHalfY, hasSlabVariant, isReplaceable, new BlockPos.MutableBlockPos());
    }

    private static boolean canPlaceSurfaceAt(int x, int z, int effectiveHalfY, boolean hasSlabVariant, Predicate<BlockPos> isReplaceable, BlockPos.MutableBlockPos probe) {
        int minPlaceY;
        boolean slab = (effectiveHalfY & 1) == 1;
        int placeY = slab ? (effectiveHalfY - 1) / 2 : effectiveHalfY / 2 - 1;
        for (int py = minPlaceY = slab && !hasSlabVariant ? placeY - 1 : placeY; py <= placeY; ++py) {
            probe.set(x, py, z);
            if (isReplaceable.test((BlockPos)probe)) continue;
            return false;
        }
        return true;
    }

    public static List<PathEntry> buildPath(List<BlockPos> trace, Block fullBlock, @Nullable SlabBlock slabBlock, int pathWidth, int pathLevel, Predicate<BlockPos> isProtectedFromPath, Predicate<BlockPos> isHardProtected, Predicate<BlockPos> isReplaceable, ToIntFunction<BlockPos> existingPathLevel, @Nullable Predicate<BlockPos> hasHeadroom, ToIntBiFunction<Integer, Integer> groundAtColumn, Predicate<BlockPos> canFillBelowPath, Predicate<BlockPos> canCutForHeadroom, ToIntBiFunction<Integer, Integer> lockedSurfaceHalfYAt, Function<BlockPos, Object> footprintAt, Predicate<BlockPos> isStablePath, @Nullable Predicate<BlockPos> headroomClearable) {
        if (trace.size() < 2) {
            return List.of();
        }
        boolean[] shouldBuild = PathBlockPlacer.computeBuildMask(trace, isProtectedFromPath, isHardProtected);
        PathBlockPlacer.pruneInteriorFootprintTails(trace, shouldBuild, footprintAt, isStablePath);
        ArrayList<BlockPos> centreTrace = new ArrayList<BlockPos>();
        for (int i = 0; i < trace.size(); ++i) {
            if (!shouldBuild[i]) continue;
            centreTrace.add(trace.get(i));
        }
        if (centreTrace.size() < 2) {
            return List.of();
        }
        int[] groundHalfY = new int[centreTrace.size()];
        for (int i = 0; i < centreTrace.size(); ++i) {
            groundHalfY[i] = 2 * ((BlockPos)centreTrace.get(i)).getY();
        }
        int[] surfaceHalfY = PathProfileOptimizer.optimize(groundHalfY, PathProfileOptimizer.Weights.defaults());
        for (int i = 0; i < centreTrace.size(); ++i) {
            BlockPos p = (BlockPos)centreTrace.get(i);
            int locked = lockedSurfaceHalfYAt.applyAsInt(p.getX(), p.getZ());
            if (locked == Integer.MIN_VALUE) continue;
            surfaceHalfY[i] = locked;
        }
        Predicate<BlockPos> canPlaceAt = pos -> {
            if (isProtectedFromPath.test((BlockPos)pos)) {
                return false;
            }
            if (isHardProtected.test((BlockPos)pos)) {
                return false;
            }
            if (!isReplaceable.test((BlockPos)pos)) {
                return false;
            }
            if (hasHeadroom != null && !hasHeadroom.test((BlockPos)pos)) {
                return false;
            }
            return existingPathLevel.applyAsInt((BlockPos)pos) < pathLevel;
        };
        List<WidenedColumn> columns = PathBlockPlacer.computeWidenedColumns(centreTrace, surfaceHalfY, pathWidth, canPlaceAt);
        ArrayList<PathEntry> out = new ArrayList<PathEntry>();
        BlockState air = Blocks.AIR.defaultBlockState();
        HashSet<Long> columnsBuilt = new HashSet<Long>();
        BlockPos.MutableBlockPos probe = new BlockPos.MutableBlockPos();
        for (WidenedColumn c : columns) {
            int lockedWidened;
            int effectiveHalfY;
            long k = (long)c.x << 32 | (long)c.z & 0xFFFFFFFFL;
            if (!columnsBuilt.add(k) || existingPathLevel.applyAsInt((BlockPos)probe.set(c.x, 0, c.z)) >= pathLevel || !PathBlockPlacer.canPlaceSurfaceAt(c.x, c.z, effectiveHalfY = (lockedWidened = lockedSurfaceHalfYAt.applyAsInt(c.x, c.z)) != Integer.MIN_VALUE ? lockedWidened : c.surfaceHalfY, slabBlock != null, isReplaceable, probe)) continue;
            if (headroomClearable != null) {
                int placeY;
                boolean slabHere = (effectiveHalfY & 1) == 1;
                int n = placeY = slabHere ? (effectiveHalfY - 1) / 2 : effectiveHalfY / 2 - 1;
                if (slabHere && slabBlock == null) {
                    --placeY;
                }
                if (!headroomClearable.test((BlockPos)probe.set(c.x, placeY, c.z))) continue;
            }
            int groundY = groundAtColumn.applyAsInt(c.x, c.z);
            PathTerraformer.ColumnDecision decision = new PathTerraformer.ColumnDecision(c.x, c.z, effectiveHalfY, groundY);
            int cx = c.x;
            int cz = c.z;
            IntUnaryOperator fill = y -> {
                probe.set(cx, y, cz);
                return canFillBelowPath.test((BlockPos)probe) ? 1 : 0;
            };
            IntUnaryOperator cut = y -> {
                probe.set(cx, y, cz);
                return canCutForHeadroom.test((BlockPos)probe) ? 1 : 0;
            };
            out.addAll(PathTerraformer.blocksForColumn(decision, fullBlock, slabBlock, DEFAULT_FOUNDATION, air, fill, cut));
        }
        return out;
    }

    public record WidenedColumn(int x, int z, int surfaceHalfY) {
    }
}

