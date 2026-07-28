/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.google.common.collect.ImmutableSet
 *  com.google.common.collect.Lists
 *  com.google.common.collect.Sets
 *  com.mojang.logging.LogUtils
 *  javax.annotation.Nullable
 *  net.minecraft.core.BlockPos
 *  net.minecraft.util.profiling.ProfilerFiller
 *  net.minecraft.util.profiling.metrics.MetricCategory
 *  net.minecraft.world.entity.Mob
 *  net.minecraft.world.level.PathNavigationRegion
 *  net.minecraft.world.level.pathfinder.BinaryHeap
 *  net.minecraft.world.level.pathfinder.Node
 *  net.minecraft.world.level.pathfinder.NodeEvaluator
 *  net.minecraft.world.level.pathfinder.Path
 *  net.minecraft.world.level.pathfinder.PathFinder
 *  net.minecraft.world.level.pathfinder.Target
 *  org.slf4j.Logger
 */
package org.millenaire.entity;

import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import com.mojang.logging.LogUtils;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;
import javax.annotation.Nullable;
import net.minecraft.core.BlockPos;
import net.minecraft.util.profiling.ProfilerFiller;
import net.minecraft.util.profiling.metrics.MetricCategory;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.level.PathNavigationRegion;
import net.minecraft.world.level.pathfinder.BinaryHeap;
import net.minecraft.world.level.pathfinder.Node;
import net.minecraft.world.level.pathfinder.NodeEvaluator;
import net.minecraft.world.level.pathfinder.Path;
import net.minecraft.world.level.pathfinder.PathFinder;
import net.minecraft.world.level.pathfinder.Target;
import org.slf4j.Logger;

public class MillPathFinder
extends PathFinder {
    private static final float FUDGING = 1.5f;
    private final Node[] neighbors = new Node[32];
    private final int maxVisitedNodes;
    private final NodeEvaluator nodeEvaluator;
    private static final boolean DEBUG = false;
    private static final Logger LOGGER = LogUtils.getLogger();
    private final BinaryHeap openSet = new BinaryHeap();
    private final List<Node> toInsert = new ArrayList<Node>(32);

    public MillPathFinder(NodeEvaluator nodeEvaluator, int maxVisitedNodes) {
        super(nodeEvaluator, maxVisitedNodes);
        this.nodeEvaluator = nodeEvaluator;
        this.maxVisitedNodes = maxVisitedNodes;
    }

    @Nullable
    public Path findPath(PathNavigationRegion region, Mob mob, Set<BlockPos> targetPositions, float maxRange, int accuracy, float searchDepthMultiplier) {
        this.openSet.clear();
        this.nodeEvaluator.prepare(region, mob);
        Node node = this.nodeEvaluator.getStart();
        if (node == null) {
            return null;
        }
        Map<Target, BlockPos> map = targetPositions.stream().collect(Collectors.toMap(p_326774_ -> this.nodeEvaluator.getTarget((double)p_326774_.getX(), (double)p_326774_.getY(), (double)p_326774_.getZ()), Function.identity()));
        Path path = this.findPath(region.getProfiler(), node, map, maxRange, accuracy, searchDepthMultiplier);
        this.nodeEvaluator.done();
        return path;
    }

    @Nullable
    private Path findPath(ProfilerFiller profiler, Node p_node, Map<Target, BlockPos> targetPos, float maxRange, int accuracy, float searchDepthMultiplier) {
        profiler.push("find_path");
        profiler.markForCharting(MetricCategory.PATH_FINDING);
        Set<Target> set = targetPos.keySet();
        p_node.g = 0.0f;
        p_node.f = p_node.h = this.getBestH(p_node, set);
        this.openSet.clear();
        this.openSet.insert(p_node);
        ImmutableSet set1 = ImmutableSet.of();
        int i = 0;
        HashSet set2 = Sets.newHashSetWithExpectedSize((int)set.size());
        int j = (int)((float)this.maxVisitedNodes * searchDepthMultiplier);
        while (!this.openSet.isEmpty() && ++i < j) {
            Node node = this.openSet.pop();
            node.closed = true;
            for (Target target : set) {
                if (!(node.distanceManhattan((Node)target) <= (float)accuracy)) continue;
                target.setReached();
                set2.add(target);
            }
            if (!set2.isEmpty()) break;
            if (node.distanceTo(p_node) >= maxRange) continue;
            int k = this.nodeEvaluator.getNeighbors(this.neighbors, node);
            this.toInsert.clear();
            for (int l = 0; l < k; ++l) {
                Node node1 = this.neighbors[l];
                if (node1.closed) continue;
                float f = this.distance(node, node1);
                node1.walkedDistance = node.walkedDistance + f;
                float f1 = node.g + f + node1.costMalus;
                if (!(node1.walkedDistance < maxRange) || node1.inOpenSet() && !(f1 < node1.g)) continue;
                node1.cameFrom = node;
                node1.g = f1;
                node1.h = this.getBestH(node1, set) * 1.5f;
                if (node1.inOpenSet()) {
                    this.openSet.changeCost(node1, node1.g + node1.h);
                    continue;
                }
                node1.f = node1.g + node1.h;
                this.toInsert.add(node1);
            }
            this.toInsert.sort(Comparator.comparingDouble(n -> n.h));
            for (Node n2 : this.toInsert) {
                this.openSet.insert(n2);
            }
        }
        Optional<Path> optional = !set2.isEmpty() ? set2.stream().map(p_77454_ -> this.reconstructPath(p_77454_.getBestNode(), (BlockPos)targetPos.get(p_77454_), true)).min(Comparator.comparingInt(Path::getNodeCount)) : set.stream().map(p_77451_ -> this.reconstructPath(p_77451_.getBestNode(), (BlockPos)targetPos.get(p_77451_), false)).min(Comparator.comparingDouble(Path::getDistToTarget).thenComparingInt(Path::getNodeCount));
        profiler.pop();
        return optional.isEmpty() ? null : optional.get();
    }

    protected float distance(Node first, Node second) {
        return first.distanceTo(second);
    }

    private float getBestH(Node node, Set<Target> targets) {
        float f = Float.MAX_VALUE;
        for (Target target : targets) {
            float f1 = node.distanceTo((Node)target);
            target.updateBest(f1, node);
            f = Math.min(f1, f);
        }
        return f;
    }

    private Path reconstructPath(Node point, BlockPos targetPos, boolean reachesTarget) {
        ArrayList list = Lists.newArrayList();
        Node node = point;
        list.add(0, point);
        while (node.cameFrom != null) {
            node = node.cameFrom;
            list.add(0, node);
        }
        return new Path((List)list, targetPos, reachesTarget);
    }
}

