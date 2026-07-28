/*
 * Decompiled with CFR 0.150.
 * 
 * Could not load the following classes:
 *  com.mojang.logging.LogUtils
 *  javax.annotation.Nullable
 *  net.minecraft.core.BlockPos
 *  net.minecraft.core.Vec3i
 *  net.minecraft.server.level.ServerLevel
 *  org.slf4j.Logger
 */
package org.millenaire.village;

import com.mojang.logging.LogUtils;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.PriorityQueue;
import javax.annotation.Nullable;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Vec3i;
import net.minecraft.server.level.ServerLevel;
import org.millenaire.building.BuildingInstance;
import org.millenaire.village.Village;
import org.millenaire.village.Waypoint;
import org.millenaire.village.WaypointEdge;
import org.millenaire.village.WaypointTraversalTester;
import org.slf4j.Logger;

public class VillageWaypointGraph {
    private static final Logger LOGGER = LogUtils.getLogger();
    public static final double MAX_EDGE_DISTANCE = 64.0;
    public static final double SHORT_EDGE_THRESHOLD = 30.0;
    public static final double MACRO_THRESHOLD = 48.0;
    private static final long WARN_REBUILD_MS = 200L;
    private volatile GraphSnapshot snapshot = GraphSnapshot.EMPTY;

    public void rebuild(List<BuildingInstance> buildings, BlockPos villageCenter, ServerLevel level, Village village) {
        Objects.requireNonNull(level, "ServerLevel is required for pathfind validation");
        Objects.requireNonNull(village, "Village is required so the tester binds villageId");
        this.rebuildInternal(buildings, villageCenter, level, village);
    }

    void rebuild(List<BuildingInstance> buildings, BlockPos villageCenter) {
        this.rebuildInternal(buildings, villageCenter, null, null);
    }

    public void rebuildForTesting(List<BuildingInstance> buildings, BlockPos villageCenter, ServerLevel level) {
        Objects.requireNonNull(level, "ServerLevel is required for pathfind validation");
        this.rebuildInternal(buildings, villageCenter, level, null);
    }

    private void rebuildInternal(List<BuildingInstance> buildings, BlockPos villageCenter, @Nullable ServerLevel level, @Nullable Village village) {
        double distSq;
        Waypoint b;
        int j;
        Waypoint a;
        int i;
        long rebuildStartNanos = System.nanoTime();
        ArrayList<Waypoint> newWaypoints = new ArrayList<Waypoint>();
        HashMap<Waypoint, List<WaypointEdge>> newAdjacency = new HashMap<Waypoint, List<WaypointEdge>>();
        newWaypoints.add(new Waypoint(villageCenter, null));
        HashSet<BlockPos> seen = new HashSet<BlockPos>();
        seen.add(villageCenter);
        for (BuildingInstance building : buildings) {
            BlockPos anchor = building.resolvePathAnchor();
            if (anchor == null || !seen.add(anchor)) continue;
            newWaypoints.add(new Waypoint(anchor, building.getId()));
        }
        for (Waypoint wp : newWaypoints) {
            newAdjacency.put(wp, new ArrayList());
        }
        int n = newWaypoints.size();
        if (n < 2) {
            this.snapshot = new GraphSnapshot(List.copyOf(newWaypoints), Map.of());
            long elapsedMs = (System.nanoTime() - rebuildStartNanos) / 1000000L;
            LOGGER.info("[Millenaire] Waypoint graph rebuilt: {} nodes, 0 edges (too few) in {} ms", (Object)n, (Object)elapsedMs);
            return;
        }
        int[] parent = new int[n];
        for (int i2 = 0; i2 < n; ++i2) {
            parent[i2] = i2;
        }
        int shortValidated = 0;
        int shortRejected = 0;
        int longValidated = 0;
        int longRejected = 0;
        int longSkippedTransitive = 0;
        try (WaypointTraversalTester tester = level != null ? new WaypointTraversalTester(level, village) : null;){
            WaypointTraversalTester.Result r;
            List<Object> nodes;
            double shortSq = 900.0;
            double maxSq = 4096.0;
            for (i = 0; i < n; ++i) {
                a = (Waypoint)newWaypoints.get(i);
                for (j = i + 1; j < n; ++j) {
                    b = (Waypoint)newWaypoints.get(j);
                    distSq = a.pos().distSqr((Vec3i)b.pos());
                    if (distSq > shortSq) continue;
                    nodes = List.of();
                    if (tester != null) {
                        r = tester.findPath(a.pos(), b.pos());
                        if (!r.reachable()) {
                            ++shortRejected;
                            continue;
                        }
                        nodes = r.nodes();
                    }
                    VillageWaypointGraph.addEdge(newAdjacency, a, b, Math.sqrt(distSq), nodes);
                    VillageWaypointGraph.union(parent, i, j);
                    ++shortValidated;
                }
            }
            for (i = 0; i < n; ++i) {
                a = (Waypoint)newWaypoints.get(i);
                for (j = i + 1; j < n; ++j) {
                    b = (Waypoint)newWaypoints.get(j);
                    distSq = a.pos().distSqr((Vec3i)b.pos());
                    if (distSq <= shortSq || distSq > maxSq) continue;
                    if (VillageWaypointGraph.find(parent, i) == VillageWaypointGraph.find(parent, j)) {
                        ++longSkippedTransitive;
                        continue;
                    }
                    nodes = List.of();
                    if (tester != null) {
                        r = tester.findPath(a.pos(), b.pos());
                        if (!r.reachable()) {
                            ++longRejected;
                            continue;
                        }
                        nodes = r.nodes();
                    }
                    VillageWaypointGraph.addEdge(newAdjacency, a, b, Math.sqrt(distSq), nodes);
                    ++longValidated;
                }
            }
        }
        int totalEdges = shortValidated + longValidated;
        int shortInspected = shortValidated + shortRejected;
        if (level != null && shortInspected >= 4 && shortRejected * 5 >= shortInspected * 4) {
            LOGGER.warn("[Millenaire] Validating tester rejected {}/{} short edges (>= 80%) \u2014 falling back to euclidean validation for this rebuild", (Object)shortRejected, (Object)shortInspected);
            double maxSqLocal = 4096.0;
            int permissiveAdded = 0;
            for (i = 0; i < n; ++i) {
                a = (Waypoint)newWaypoints.get(i);
                for (j = i + 1; j < n; ++j) {
                    b = (Waypoint)newWaypoints.get(j);
                    distSq = a.pos().distSqr((Vec3i)b.pos());
                    if (distSq > maxSqLocal) continue;
                    boolean alreadyConnected = false;
                    for (WaypointEdge edge : (List)newAdjacency.get(a)) {
                        if (!edge.target().equals(b)) continue;
                        alreadyConnected = true;
                        break;
                    }
                    if (alreadyConnected) continue;
                    VillageWaypointGraph.addEdge(newAdjacency, a, b, Math.sqrt(distSq), List.of());
                    ++permissiveAdded;
                }
            }
            LOGGER.warn("[Millenaire] Permissive fallback added {} edges (graph now {} edges total)", (Object)permissiveAdded, (Object)(totalEdges += permissiveAdded));
        }
        HashMap publishAdj = new HashMap(newAdjacency.size() * 2);
        for (Map.Entry e : newAdjacency.entrySet()) {
            publishAdj.put((Waypoint)e.getKey(), List.copyOf((Collection)e.getValue()));
        }
        this.snapshot = new GraphSnapshot(List.copyOf(newWaypoints), Map.copyOf(publishAdj));
        long elapsedMs = (System.nanoTime() - rebuildStartNanos) / 1000000L;
        LOGGER.info("[Millenaire] Waypoint graph rebuilt: {} nodes, {} edges (short: {} valid / {} rejected, long: {} valid / {} rejected / {} skipped-transitive) in {} ms", new Object[]{n, totalEdges, shortValidated, shortRejected, longValidated, longRejected, longSkippedTransitive, elapsedMs});
        if (elapsedMs >= 200L) {
            LOGGER.warn("[Millenaire] Waypoint graph rebuild SLOW: {} ms (>= {} ms threshold) for {} nodes \u2014 see PERF roadmap", new Object[]{elapsedMs, 200L, n});
        }
    }

    private static void addEdge(Map<Waypoint, List<WaypointEdge>> adj, Waypoint a, Waypoint b, double cost, List<BlockPos> nodes) {
        List<BlockPos> reversed;
        if (nodes.isEmpty()) {
            reversed = List.of();
        } else {
            ArrayList<BlockPos> rev = new ArrayList<BlockPos>(nodes);
            Collections.reverse(rev);
            reversed = Collections.unmodifiableList(rev);
        }
        adj.get(a).add(new WaypointEdge(b, cost, nodes));
        adj.get(b).add(new WaypointEdge(a, cost, reversed));
    }

    private static int find(int[] parent, int x) {
        while (parent[x] != x) {
            parent[x] = parent[parent[x]];
            x = parent[x];
        }
        return x;
    }

    private static void union(int[] parent, int x, int y) {
        int ry;
        int rx = VillageWaypointGraph.find(parent, x);
        if (rx != (ry = VillageWaypointGraph.find(parent, y))) {
            parent[rx] = ry;
        }
    }

    @Nullable
    public Waypoint findNearestWaypoint(BlockPos pos) {
        return VillageWaypointGraph.findNearestWaypoint(this.snapshot.waypoints(), pos);
    }

    @Nullable
    private static Waypoint findNearestWaypoint(List<Waypoint> wps, BlockPos pos) {
        Waypoint nearest = null;
        double nearestDistSq = Double.MAX_VALUE;
        for (Waypoint wp : wps) {
            double distSq = wp.pos().distSqr((Vec3i)pos);
            if (!(distSq < nearestDistSq)) continue;
            nearestDistSq = distSq;
            nearest = wp;
        }
        return nearest;
    }

    public List<BlockPos> findPath(BlockPos from, BlockPos to) {
        GraphSnapshot s = this.snapshot;
        List<Waypoint> wps = s.waypoints();
        Map<Waypoint, List<WaypointEdge>> adj = s.adjacency();
        if (wps.size() < 2) {
            return Collections.emptyList();
        }
        Waypoint startWp = VillageWaypointGraph.findNearestWaypoint(wps, from);
        Waypoint endWp = VillageWaypointGraph.findNearestWaypoint(wps, to);
        if (startWp == null || endWp == null) {
            return Collections.emptyList();
        }
        if (startWp.equals(endWp)) {
            return Collections.emptyList();
        }
        HashMap<Waypoint, Double> gScore = new HashMap<Waypoint, Double>();
        HashMap<Waypoint, Waypoint> cameFrom = new HashMap<Waypoint, Waypoint>();
        HashSet<Waypoint> closed = new HashSet<Waypoint>();
        gScore.put(startWp, 0.0);
        PriorityQueue<Waypoint> open = new PriorityQueue<Waypoint>(Comparator.comparingDouble(wp -> gScore.getOrDefault(wp, (Double)Double.MAX_VALUE) + this.heuristic((Waypoint)wp, endWp)));
        open.add(startWp);
        while (!open.isEmpty()) {
            Waypoint current = open.poll();
            if (current.equals(endWp)) {
                return this.reconstructPath(cameFrom, current);
            }
            if (closed.contains(current)) continue;
            closed.add(current);
            List edges = adj.getOrDefault(current, Collections.emptyList());
            for (WaypointEdge edge : edges) {
                double tentativeG;
                Waypoint neighbor = edge.target();
                if (closed.contains(neighbor) || !((tentativeG = gScore.getOrDefault(current, (Double)Double.MAX_VALUE) + edge.cost()) < gScore.getOrDefault(neighbor, (Double)Double.MAX_VALUE))) continue;
                gScore.put(neighbor, tentativeG);
                cameFrom.put(neighbor, current);
                open.add(neighbor);
            }
        }
        return Collections.emptyList();
    }

    public boolean isAvailable() {
        return this.snapshot.waypoints().size() >= 2;
    }

    public List<Waypoint> getWaypoints() {
        return Collections.unmodifiableList(this.snapshot.waypoints());
    }

    public List<DirectedEdge> getEdges() {
        GraphSnapshot s = this.snapshot;
        List<Waypoint> wps = s.waypoints();
        Map<Waypoint, List<WaypointEdge>> adj = s.adjacency();
        ArrayList<DirectedEdge> out = new ArrayList<DirectedEdge>();
        HashSet<Waypoint> seen = new HashSet<Waypoint>();
        for (Waypoint a : wps) {
            seen.add(a);
            for (WaypointEdge edge : adj.getOrDefault(a, Collections.emptyList())) {
                if (seen.contains(edge.target())) continue;
                out.add(new DirectedEdge(a, edge.target(), edge.cost(), edge.pathNodes()));
            }
        }
        return out;
    }

    public int waypointCount() {
        return this.snapshot.waypoints().size();
    }

    private double heuristic(Waypoint a, Waypoint b) {
        return Math.sqrt(a.pos().distSqr((Vec3i)b.pos()));
    }

    private List<BlockPos> reconstructPath(Map<Waypoint, Waypoint> cameFrom, Waypoint current) {
        ArrayList<BlockPos> path = new ArrayList<BlockPos>();
        while (cameFrom.containsKey(current)) {
            path.add(current.pos());
            current = cameFrom.get(current);
        }
        Collections.reverse(path);
        return path;
    }

    private record GraphSnapshot(List<Waypoint> waypoints, Map<Waypoint, List<WaypointEdge>> adjacency) {
        static final GraphSnapshot EMPTY = new GraphSnapshot(List.of(), Map.of());
    }

    public record DirectedEdge(Waypoint from, Waypoint to, double cost, List<BlockPos> pathNodes) {
    }
}

