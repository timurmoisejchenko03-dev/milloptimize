/*
 * Decompiled with CFR 0.150.
 * 
 * Could not load the following classes:
 *  javax.annotation.Nullable
 *  net.minecraft.core.BlockPos
 *  net.minecraft.server.level.ServerLevel
 *  net.minecraft.world.level.block.state.BlockState
 */
package org.millenaire.village.path;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.PriorityQueue;
import java.util.function.BiFunction;
import java.util.function.BiPredicate;
import javax.annotation.Nullable;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.state.BlockState;
import org.millenaire.world.TerrainPreparer;

public final class PathGridAStar {
    private static final int[][] DIRECTIONS = new int[][]{{1, 0}, {-1, 0}, {0, 1}, {0, -1}};
    private static final int DIR_NONE = -1;

    private PathGridAStar() {
    }

    @Nullable
    public static List<BlockPos> findPath(BlockPos from, BlockPos to, GroundHeightProvider ground, TraversabilityCheck traversable, Weights w, int maxNodes) {
        return PathGridAStar.search((BlockPos)from, (BlockPos)to, (GroundHeightProvider)ground, (TraversabilityCheck)traversable, (Weights)w, (int)maxNodes).path;
    }

    public static Result search(BlockPos from, BlockPos to, GroundHeightProvider ground, TraversabilityCheck traversable, Weights w, int maxNodes) {
        return PathGridAStar.search(from, to, ground, traversable, w, maxNodes, null, 1.0);
    }

    public static Result search(BlockPos from, BlockPos to, GroundHeightProvider ground, TraversabilityCheck traversable, Weights w, int maxNodes, @Nullable PathAffinity affinity, double affinityFactor) {
        int fx = from.getX();
        int fz = from.getZ();
        int tx = to.getX();
        int tz = to.getZ();
        Comparator comparator = (a, b) -> {
            int cmp = Double.compare(a.f, b.f);
            if (cmp != 0) {
                return cmp;
            }
            cmp = Double.compare(a.f - a.g, b.f - b.g);
            if (cmp != 0) {
                return cmp;
            }
            cmp = Integer.compare(a.x, b.x);
            if (cmp != 0) {
                return cmp;
            }
            return Integer.compare(a.z, b.z);
        };
        PriorityQueue<Node> open = new PriorityQueue<Node>(comparator);
        HashMap<DirKey, Double> bestG = new HashMap<DirKey, Double>();
        int startY = ground.heightAt(fx, fz);
        double h0 = w.heuristicScale * PathGridAStar.manhattan(fx, fz, tx, tz);
        open.add(new Node(fx, fz, startY, -1, 0.0, h0, null));
        bestG.put(new DirKey(fx, fz, -1), 0.0);
        int explored = 0;
        int rejectedStep = 0;
        int rejectedTraversable = 0;
        while (!open.isEmpty() && explored < maxNodes) {
            Node cur = (Node)open.poll();
            ++explored;
            if (cur.x == tx && cur.z == tz) {
                return new Result(PathGridAStar.reconstruct(cur), explored, "found");
            }
            for (int di = 0; di < DIRECTIONS.length; ++di) {
                int[] d = DIRECTIONS[di];
                int nx = cur.x + d[0];
                int nz = cur.z + d[1];
                if (!traversable.allows(nx, nz)) {
                    ++rejectedTraversable;
                    continue;
                }
                int ny = ground.heightAt(nx, nz);
                int dh = ny - cur.y;
                if (Math.abs(dh) > w.maxStepUp) {
                    ++rejectedStep;
                    continue;
                }
                double stepCost = 1.0 + w.gradientAlpha * (double)dh * (double)dh;
                if (affinity != null && affinity.onPath(nx, nz)) {
                    stepCost *= affinityFactor;
                }
                if (cur.dirIdx != -1 && cur.dirIdx != di) {
                    stepCost += w.turnPenalty;
                }
                double newG = cur.g + stepCost;
                DirKey key = new DirKey(nx, nz, di);
                Double existing = (Double)bestG.get(key);
                if (existing != null && existing <= newG) continue;
                bestG.put(key, newG);
                double hEst = w.heuristicScale * PathGridAStar.manhattan(nx, nz, tx, tz);
                open.add(new Node(nx, nz, ny, di, newG, newG + hEst, cur));
            }
        }
        String reason = explored >= maxNodes ? "budget_exhausted" : String.format("frontier_empty (rejectedStep=%d, rejectedTraversable=%d)", rejectedStep, rejectedTraversable);
        return new Result(null, explored, reason);
    }

    @Nullable
    public static List<BlockPos> findPath(ServerLevel level, BlockPos from, BlockPos to, Weights w, int maxNodes) {
        return PathGridAStar.searchOnLevel((ServerLevel)level, (BlockPos)from, (BlockPos)to, (Weights)w, (int)maxNodes).path;
    }

    public static Result searchOnLevel(ServerLevel level, BlockPos from, BlockPos to, Weights w, int maxNodes) {
        GroundHeightProvider ground = (x, z) -> TerrainPreparer.getGroundHeight(level, x, z);
        TraversabilityCheck traversable = (x, z) -> PathGridAStar.isWalkableColumn(level, x, z, ground.heightAt(x, z));
        return PathGridAStar.search(from, to, ground, traversable, w, maxNodes);
    }

    private static boolean isWalkableColumn(ServerLevel level, int x, int z, int y) {
        BlockPos surface = new BlockPos(x, y - 1, z);
        BlockState surfaceState = level.getBlockState(surface);
        if (surfaceState.liquid()) {
            return false;
        }
        return surfaceState.getFluidState().isEmpty();
    }

    @Nullable
    public static List<BlockPos> findPath(BlockPos from, BlockPos to, BiFunction<Integer, Integer, Integer> heightProvider, BiPredicate<Integer, Integer> traversable, int maxNodes) {
        GroundHeightProvider g = (x, z) -> (Integer)heightProvider.apply(x, z);
        TraversabilityCheck t = (x, z) -> traversable.test(x, z);
        return PathGridAStar.findPath(from, to, g, t, Weights.defaults(), maxNodes);
    }

    private static double manhattan(int x1, int z1, int x2, int z2) {
        return Math.abs(x1 - x2) + Math.abs(z1 - z2);
    }

    private static List<BlockPos> reconstruct(Node end) {
        ArrayList<BlockPos> path = new ArrayList<BlockPos>();
        Node cur = end;
        while (cur != null) {
            path.add(new BlockPos(cur.x, cur.y, cur.z));
            cur = cur.parent;
        }
        Collections.reverse(path);
        return path;
    }

    @FunctionalInterface
    public static interface GroundHeightProvider {
        public int heightAt(int var1, int var2);
    }

    @FunctionalInterface
    public static interface TraversabilityCheck {
        public boolean allows(int var1, int var2);
    }

    public record Weights(double gradientAlpha, int maxStepUp, double heuristicScale, double turnPenalty) {
        public static Weights defaults() {
            return new Weights(2.0, 3, 1.0, 0.3);
        }

        public static Weights relaxed() {
            return new Weights(0.5, 6, 1.0, 0.3);
        }
    }

    public record Result(@Nullable List<BlockPos> path, int nodesExplored, String reason) {
        public boolean success() {
            return this.path != null;
        }
    }

    @FunctionalInterface
    public static interface PathAffinity {
        public boolean onPath(int var1, int var2);
    }

    private record Node(int x, int z, int y, int dirIdx, double g, double f, @Nullable Node parent) {
    }

    private record DirKey(int x, int z, int dirIdx) {
    }
}

