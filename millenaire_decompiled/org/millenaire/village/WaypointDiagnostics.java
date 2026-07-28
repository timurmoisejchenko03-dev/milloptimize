/*
 * Decompiled with CFR 0.150.
 * 
 * Could not load the following classes:
 *  com.mojang.logging.LogUtils
 *  javax.annotation.Nullable
 *  net.minecraft.core.BlockPos
 *  net.minecraft.core.Vec3i
 *  net.minecraft.resources.ResourceLocation
 *  net.minecraft.server.level.ServerLevel
 *  org.slf4j.Logger
 */
package org.millenaire.village;

import com.mojang.logging.LogUtils;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.List;
import javax.annotation.Nullable;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Vec3i;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import org.millenaire.building.BuildingId;
import org.millenaire.building.BuildingInstance;
import org.millenaire.village.Village;
import org.millenaire.village.Waypoint;
import org.millenaire.village.path.PathGridAStar;
import org.slf4j.Logger;

public final class WaypointDiagnostics {
    private static final Logger LOGGER = LogUtils.getLogger();
    public static final int DEFAULT_PROBE_NODES = 200;
    public static final int CENTRAL_ANCHOR_COUNT = 2;

    private WaypointDiagnostics() {
    }

    public static Report analyze(ServerLevel level, Village village) {
        return WaypointDiagnostics.analyze(level, village, 200);
    }

    public static Report analyze(ServerLevel level, Village village, int nodeBudget) {
        List<BlockPos> anchors = WaypointDiagnostics.pickAnchors(village);
        ArrayList<Finding> findings = new ArrayList<Finding>();
        int probed = 0;
        for (Waypoint wp : village.getWaypointGraph().getWaypoints()) {
            if (wp.buildingId() == null || WaypointDiagnostics.isAnchor(wp.pos(), anchors)) continue;
            ++probed;
            BlockPos reached = WaypointDiagnostics.tryReachFromAnyAnchor(level, anchors, wp.pos(), nodeBudget);
            if (reached != null) continue;
            findings.add(new Finding(wp.buildingId(), wp.pos(), WaypointDiagnostics.resolvePlanSetId(village, wp.buildingId()), WaypointDiagnostics.nearestAnchor(anchors, wp.pos())));
        }
        LOGGER.info("[Millenaire] WaypointDiagnostics \u2014 village {} : probed {} waypoints from {} anchors, {} unreachable (budget={})", new Object[]{village.getVillageTypeId(), probed, anchors.size(), findings.size(), nodeBudget});
        return new Report(village, anchors, probed, findings);
    }

    private static List<BlockPos> pickAnchors(Village village) {
        BlockPos anchor;
        BlockPos center = village.getCenter();
        LinkedHashSet<BlockPos> anchors = new LinkedHashSet<BlockPos>();
        BuildingInstance townhall = village.getTownhall();
        if (townhall != null && (anchor = WaypointDiagnostics.resolveAnchorPos(townhall)) != null) {
            anchors.add(anchor);
        }
        ArrayList<BuildingInstance> sorted = new ArrayList<BuildingInstance>(village.getBuildings());
        sorted.sort(Comparator.comparingDouble(b -> b.getOrigin().distSqr((Vec3i)center)));
        for (BuildingInstance b2 : sorted) {
            if (anchors.size() >= 3) break;
            BlockPos anchor2 = WaypointDiagnostics.resolveAnchorPos(b2);
            if (anchor2 == null) continue;
            anchors.add(anchor2);
        }
        return new ArrayList<BlockPos>(anchors);
    }

    @Nullable
    private static BlockPos resolveAnchorPos(BuildingInstance building) {
        return building.resolvePathAnchor();
    }

    private static boolean isAnchor(BlockPos wp, List<BlockPos> anchors) {
        for (BlockPos a : anchors) {
            if (!a.equals((Object)wp)) continue;
            return true;
        }
        return false;
    }

    private static BlockPos nearestAnchor(List<BlockPos> anchors, BlockPos wp) {
        BlockPos best = anchors.isEmpty() ? BlockPos.ZERO : anchors.get(0);
        double bestSq = best.distSqr((Vec3i)wp);
        for (BlockPos a : anchors) {
            double d = a.distSqr((Vec3i)wp);
            if (!(d < bestSq)) continue;
            bestSq = d;
            best = a;
        }
        return best;
    }

    @Nullable
    private static BlockPos tryReachFromAnyAnchor(ServerLevel level, List<BlockPos> anchors, BlockPos target, int nodeBudget) {
        for (BlockPos anchor : anchors) {
            List<BlockPos> path = PathGridAStar.findPath(level, anchor, target, PathGridAStar.Weights.defaults(), nodeBudget);
            if (path == null) continue;
            return anchor;
        }
        return null;
    }

    @Nullable
    private static ResourceLocation resolvePlanSetId(Village village, BuildingId buildingId) {
        BuildingInstance b = village.findBuildingById(buildingId);
        return b != null ? b.getPlanSetId() : null;
    }

    public record Report(Village village, List<BlockPos> anchors, int probedCount, List<Finding> findings) {
        public boolean clean() {
            return this.findings.isEmpty();
        }
    }

    public record Finding(BuildingId buildingId, BlockPos waypointPos, @Nullable ResourceLocation planSetId, BlockPos nearestAnchor) {
        public double distanceToNearestAnchor() {
            return Math.sqrt(this.waypointPos.distSqr((Vec3i)this.nearestAnchor));
        }
    }
}

