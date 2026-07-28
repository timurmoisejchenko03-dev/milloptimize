/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.mojang.logging.LogUtils
 *  javax.annotation.Nullable
 *  net.minecraft.core.BlockPos
 *  org.slf4j.Logger
 */
package org.millenaire.village.path;

import com.mojang.logging.LogUtils;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import javax.annotation.Nullable;
import net.minecraft.core.BlockPos;
import org.millenaire.building.BuildingId;
import org.millenaire.village.path.PathRoute;
import org.slf4j.Logger;

public final class PathRouter {
    private static final Logger LOGGER = LogUtils.getLogger();
    private static final double NODE_SWITCH_FACTOR = 1.3;
    private static final double INTERMEDIATE_FACTOR = 1.5;
    private static final double INTERMEDIATE_THRESHOLD = 20.0;
    private static final double STICKINESS_FACTOR = 1.3;
    private static final double LATERAL_MAX_DIST = 35.0;
    private static final double LATERAL_DETOUR_THRESHOLD = 2.5;
    private static final int LATERAL_MAX_PER_BUILDING = 1;

    private PathRouter() {
    }

    public static List<PathRoute> computeRoutes(List<BuildingInfo> buildings, List<NodeInfo> nodes, BlockPos thPos, List<String> pathMaterials) {
        return PathRouter.computeRoutes(buildings, nodes, thPos, pathMaterials, Set.of());
    }

    public static List<PathRoute> computeRoutes(List<BuildingInfo> buildings, List<NodeInfo> nodes, BlockPos thPos, List<String> pathMaterials, Set<BuildingId> unreachableFromTH) {
        return PathRouter.computeRoutes(buildings, nodes, thPos, pathMaterials, unreachableFromTH, Map.of());
    }

    public static List<PathRoute> computeRoutes(List<BuildingInfo> buildings, List<NodeInfo> nodes, BlockPos thPos, List<String> pathMaterials, Set<BuildingId> unreachableFromTH, Map<BuildingId, BlockPos> previousParents) {
        ArrayList<PathRoute> routes = new ArrayList<PathRoute>();
        ArrayList<BuildingInfo> routable = new ArrayList<BuildingInfo>(buildings.size());
        for (BuildingInfo info : buildings) {
            if (info.isSubBuilding || info.noPaths() || info.pathStart == null) continue;
            routable.add(info);
        }
        for (BuildingInfo b : routable) {
            BlockPos source = b.pathStart;
            BlockPos dest = thPos;
            BuildingId destId = null;
            double destDist = PathRouter.distance(source, dest);
            boolean isNode = b.isPathNode();
            if (!isNode) {
                StickyParent sp;
                BlockPos prev;
                for (NodeInfo node : nodes) {
                    double nodeDist;
                    if (dest == thPos) {
                        nodeDist = PathRouter.distance(source, node.pos);
                        if (!(nodeDist * 1.3 < destDist)) continue;
                        dest = node.pos;
                        destDist = nodeDist;
                        continue;
                    }
                    nodeDist = PathRouter.distance(source, node.pos);
                    if (!(nodeDist < destDist)) continue;
                    dest = node.pos;
                    destDist = nodeDist;
                }
                if (destDist > 20.0) {
                    double thDistSqFromB = PathRouter.distanceSq(thPos, source);
                    BuildingInfo bestIntermediateInfo = null;
                    double bestIntermediateDist = Double.MAX_VALUE;
                    for (BuildingInfo other : routable) {
                        double otherDist;
                        double thDistSqFromOther;
                        if (other == b || other.isPathNode() || other.id != null && unreachableFromTH.contains(other.id) || (thDistSqFromOther = PathRouter.distanceSq(thPos, other.pathStart)) >= thDistSqFromB || !((otherDist = PathRouter.distance(source, other.pathStart)) * 1.5 < destDist) || !(otherDist < bestIntermediateDist)) continue;
                        bestIntermediateInfo = other;
                        bestIntermediateDist = otherDist;
                    }
                    if (bestIntermediateInfo != null) {
                        dest = bestIntermediateInfo.pathStart;
                        destId = bestIntermediateInfo.id;
                        destDist = bestIntermediateDist;
                    }
                }
                if (b.id != null && (prev = previousParents.get(b.id)) != null && !prev.equals((Object)dest) && PathRouter.distance(source, prev) <= destDist * 1.3 && (sp = PathRouter.validateStickyParent(prev, source, thPos, nodes, routable, b)).valid()) {
                    dest = prev;
                    destId = sp.id();
                }
            }
            routes.add(PathRouter.buildRoute(b, source, dest, destId, b.pathWidth, b.pathLevel, pathMaterials));
        }
        List<PathRoute> uplifted = PathRouter.propagateTierUplift(routes, routable, pathMaterials);
        List<PathRoute> laterals = PathRouter.computeLateralRoutes(uplifted, routable, thPos, pathMaterials);
        if (!laterals.isEmpty()) {
            ArrayList<PathRoute> all = new ArrayList<PathRoute>(uplifted);
            all.addAll(laterals);
            return all;
        }
        return uplifted;
    }

    private static StickyParent validateStickyParent(BlockPos prev, BlockPos source, BlockPos thPos, List<NodeInfo> nodes, List<BuildingInfo> routable, BuildingInfo b) {
        if (prev.equals((Object)thPos)) {
            return new StickyParent(true, null);
        }
        for (NodeInfo node : nodes) {
            if (!node.pos.equals((Object)prev)) continue;
            return new StickyParent(true, null);
        }
        double thDistSqFromB = PathRouter.distanceSq(thPos, source);
        for (BuildingInfo other : routable) {
            if (other == b || other.isPathNode() || !other.pathStart.equals((Object)prev)) continue;
            if (PathRouter.distanceSq(thPos, other.pathStart) >= thDistSqFromB) {
                return new StickyParent(false, null);
            }
            return new StickyParent(true, other.id);
        }
        return new StickyParent(false, null);
    }

    private static List<PathRoute> propagateTierUplift(List<PathRoute> routes, List<BuildingInfo> routable, List<String> pathMaterials) {
        if (routes.isEmpty() || pathMaterials.isEmpty()) {
            return routes;
        }
        HashMap<BuildingId, Integer> rawTier = new HashMap<BuildingId, Integer>();
        for (BuildingInfo buildingInfo : routable) {
            if (buildingInfo.id == null) continue;
            rawTier.put(buildingInfo.id, buildingInfo.pathLevel);
        }
        HashMap<BuildingId, List<BuildingId>> upstream = new HashMap<BuildingId, List<BuildingId>>();
        for (PathRoute r : routes) {
            if (r.destinationId() == null || r.sourceId() == null) continue;
            upstream.computeIfAbsent(r.destinationId(), k -> new ArrayList()).add(r.sourceId());
        }
        HashMap<BuildingId, Integer> hashMap = new HashMap<BuildingId, Integer>();
        HashSet<BuildingId> visiting = new HashSet<BuildingId>();
        for (BuildingId id : rawTier.keySet()) {
            PathRouter.effectiveTier(id, rawTier, upstream, hashMap, visiting);
        }
        ArrayList<PathRoute> rewritten = new ArrayList<PathRoute>(routes.size());
        for (PathRoute r : routes) {
            int srcTier = r.sourceId() != null ? hashMap.getOrDefault(r.sourceId(), r.pathLevel()).intValue() : r.pathLevel();
            int dstTier = r.destinationId() != null ? hashMap.getOrDefault(r.destinationId(), r.pathLevel()).intValue() : r.pathLevel();
            int t = Math.max(srcTier, dstTier);
            int clamped = Math.min(t, pathMaterials.size() - 1);
            rewritten.add(r.withTierAndMaterial(clamped, pathMaterials.get(clamped)));
        }
        return rewritten;
    }

    private static int effectiveTier(BuildingId id, Map<BuildingId, Integer> rawTier, Map<BuildingId, List<BuildingId>> upstream, Map<BuildingId, Integer> memo, Set<BuildingId> visiting) {
        if (id == null) {
            return 0;
        }
        Integer cached = memo.get(id);
        if (cached != null) {
            return cached;
        }
        if (visiting.contains(id)) {
            LOGGER.warn("path graph cycle at {} \u2014 breaking with rawTier", (Object)id);
            return rawTier.getOrDefault(id, 0);
        }
        visiting.add(id);
        int max = rawTier.getOrDefault(id, 0);
        for (BuildingId up : upstream.getOrDefault(id, List.of())) {
            max = Math.max(max, PathRouter.effectiveTier(up, rawTier, upstream, memo, visiting));
        }
        visiting.remove(id);
        memo.put(id, max);
        return max;
    }

    private static List<PathRoute> computeLateralRoutes(List<PathRoute> mainRoutes, List<BuildingInfo> routable, BlockPos thPos, List<String> pathMaterials) {
        if (routable.size() < 2) {
            return List.of();
        }
        HashMap<BuildingId, BuildingInfo> infoById = new HashMap<BuildingId, BuildingInfo>();
        for (BuildingInfo info : routable) {
            if (info.id() == null) continue;
            infoById.put(info.id(), info);
        }
        HashMap<BuildingId, BuildingId> parentOf = new HashMap<BuildingId, BuildingId>();
        HashMap<BuildingId, Double> distToParent = new HashMap<BuildingId, Double>();
        HashMap<BlockPos, BuildingId> posToBuildingId = new HashMap<BlockPos, BuildingId>();
        for (BuildingInfo info : routable) {
            if (info.id() == null) continue;
            posToBuildingId.put(info.pathStart(), info.id());
        }
        for (PathRoute r : mainRoutes) {
            if (r.lateral() || r.sourceId() == null) continue;
            Object destBuildingId = r.destinationId();
            if (destBuildingId == null) {
                destBuildingId = (BuildingId)posToBuildingId.get(r.to());
            }
            parentOf.put(r.sourceId(), (BuildingId)destBuildingId);
            distToParent.put(r.sourceId(), PathRouter.distance(r.from(), r.to()));
        }
        HashMap<BuildingId, TreeNode> tree = new HashMap<BuildingId, TreeNode>();
        HashSet<BuildingId> inProgress = new HashSet<BuildingId>();
        for (BuildingId id : infoById.keySet()) {
            PathRouter.buildTreeNode(id, parentOf, distToParent, tree, inProgress);
        }
        ArrayList<LateralCandidate> candidates = new ArrayList<LateralCandidate>();
        ArrayList ids = new ArrayList(infoById.keySet());
        for (int i = 0; i < ids.size(); ++i) {
            for (int j = i + 1; j < ids.size(); ++j) {
                double detour;
                double treeDist;
                BlockPos bPos;
                BlockPos aPos;
                double directDist;
                BuildingId aId = (BuildingId)ids.get(i);
                BuildingId bId = (BuildingId)ids.get(j);
                TreeNode nodeA = (TreeNode)tree.get(aId);
                TreeNode nodeB = (TreeNode)tree.get(bId);
                if (nodeA == null || nodeB == null || nodeA.ancestors.contains(bId) || nodeB.ancestors.contains(aId) || (directDist = PathRouter.distance(aPos = ((BuildingInfo)infoById.get(aId)).pathStart(), bPos = ((BuildingInfo)infoById.get(bId)).pathStart())) > 35.0 || directDist < 0.001 || (treeDist = PathRouter.computeTreeDistViaLCA(nodeA, nodeB, tree)) < 0.0 || (detour = treeDist / directDist) <= 2.5) continue;
                candidates.add(new LateralCandidate(aId, bId, aPos, bPos, detour, directDist));
            }
        }
        if (candidates.isEmpty()) {
            return List.of();
        }
        candidates.sort(Comparator.comparingDouble(c -> c.detourFactor).reversed().thenComparingDouble(c -> c.directDist).thenComparing(c -> c.aId.toString()).thenComparing(c -> c.bId.toString()));
        HashMap<BuildingId, Integer> lateralCount = new HashMap<BuildingId, Integer>();
        ArrayList<PathRoute> laterals = new ArrayList<PathRoute>();
        String lateralMaterial = pathMaterials.isEmpty() ? "pathgravel" : pathMaterials.getFirst();
        for (LateralCandidate c2 : candidates) {
            int countA = lateralCount.getOrDefault(c2.aId, 0);
            int countB = lateralCount.getOrDefault(c2.bId, 0);
            if (countA >= 1 || countB >= 1) continue;
            laterals.add(new PathRoute(c2.aPos, c2.bPos, lateralMaterial, 1, 0, c2.aId, c2.bId, true));
            lateralCount.merge(c2.aId, 1, Integer::sum);
            lateralCount.merge(c2.bId, 1, Integer::sum);
        }
        if (!laterals.isEmpty()) {
            LOGGER.debug("Pass 3: emitted {} lateral route(s)", (Object)laterals.size());
        }
        return laterals;
    }

    /*
     * WARNING - Removed try catching itself - possible behaviour change.
     */
    private static TreeNode buildTreeNode(BuildingId id, Map<BuildingId, BuildingId> parentOf, Map<BuildingId, Double> distToParentMap, Map<BuildingId, TreeNode> tree, Set<BuildingId> inProgress) {
        TreeNode existing = tree.get(id);
        if (existing != null) {
            return existing;
        }
        if (!inProgress.add(id)) {
            TreeNode node = new TreeNode(id, null, 0.0, Set.of(), 0.0);
            tree.put(id, node);
            return node;
        }
        try {
            double distToRoot;
            HashSet<Object> ancestors;
            BuildingId parentId = parentOf.get(id);
            double dtp = distToParentMap.getOrDefault(id, 0.0);
            if (parentId == null) {
                TreeNode node = new TreeNode(id, null, dtp, Set.of(), dtp);
                tree.put(id, node);
                TreeNode treeNode = node;
                return treeNode;
            }
            if (tree.containsKey(parentId) || parentOf.containsKey(parentId)) {
                TreeNode parentNode = PathRouter.buildTreeNode(parentId, parentOf, distToParentMap, tree, inProgress);
                ancestors = new HashSet<BuildingId>(parentNode.ancestors);
                ancestors.add(parentId);
                distToRoot = parentNode.distToRoot + dtp;
            } else {
                ancestors = new HashSet<BuildingId>();
                ancestors.add(parentId);
                distToRoot = dtp;
            }
            TreeNode node = new TreeNode(id, parentId, dtp, Set.copyOf(ancestors), distToRoot);
            tree.put(id, node);
            TreeNode treeNode = node;
            return treeNode;
        }
        finally {
            inProgress.remove(id);
        }
    }

    private static double computeTreeDistViaLCA(TreeNode a, TreeNode b, Map<BuildingId, TreeNode> tree) {
        HashMap<BuildingId, Double> distFromA = new HashMap<BuildingId, Double>();
        distFromA.put(a.id, 0.0);
        double cumA = 0.0;
        TreeNode cur = a;
        while (cur.parentId != null) {
            distFromA.put(cur.parentId, cumA += cur.distToParent);
            TreeNode parent = tree.get(cur.parentId);
            if (parent == null) break;
            cur = parent;
        }
        double cumB = 0.0;
        cur = b;
        if (distFromA.containsKey(b.id)) {
            return (Double)distFromA.get(b.id);
        }
        while (cur.parentId != null) {
            cumB += cur.distToParent;
            if (distFromA.containsKey(cur.parentId)) {
                return (Double)distFromA.get(cur.parentId) + cumB;
            }
            TreeNode parent = tree.get(cur.parentId);
            if (parent == null) break;
            cur = parent;
        }
        return a.distToRoot + b.distToRoot;
    }

    private static PathRoute buildRoute(BuildingInfo source, BlockPos from, BlockPos to, @Nullable BuildingId destinationId, int width, int pathLevel, List<String> pathMaterials) {
        String material;
        if (pathMaterials.isEmpty()) {
            material = "pathgravel";
        } else {
            int index = Math.min(pathLevel, pathMaterials.size() - 1);
            material = pathMaterials.get(index);
        }
        return new PathRoute(from, to, material, width, pathLevel, source.id, destinationId, false);
    }

    private static double distance(BlockPos a, BlockPos b) {
        double dx = a.getX() - b.getX();
        double dz = a.getZ() - b.getZ();
        return Math.sqrt(dx * dx + dz * dz);
    }

    private static double distanceSq(BlockPos a, BlockPos b) {
        double dx = a.getX() - b.getX();
        double dz = a.getZ() - b.getZ();
        return dx * dx + dz * dz;
    }

    public record BuildingInfo(@Nullable BuildingId id, BlockPos pathStart, boolean isSubBuilding, List<String> tags, int pathLevel, int pathWidth) {
        public BuildingInfo(BlockPos pathStart, boolean isSubBuilding, List<String> tags, int pathLevel, int pathWidth) {
            this(null, pathStart, isSubBuilding, tags, pathLevel, pathWidth);
        }

        public boolean noPaths() {
            return this.tags != null && this.tags.contains("nopaths");
        }

        public boolean isPathNode() {
            return this.tags != null && this.tags.contains("pathnode");
        }
    }

    public record NodeInfo(BlockPos pos) {
    }

    private record StickyParent(boolean valid, BuildingId id) {
    }

    private record TreeNode(BuildingId id, @Nullable BuildingId parentId, double distToParent, Set<BuildingId> ancestors, double distToRoot) {
    }

    private record LateralCandidate(BuildingId aId, BuildingId bId, BlockPos aPos, BlockPos bPos, double detourFactor, double directDist) {
    }
}

