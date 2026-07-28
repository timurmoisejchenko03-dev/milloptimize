/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.google.gson.GsonBuilder
 *  com.google.gson.JsonArray
 *  com.google.gson.JsonElement
 *  com.google.gson.JsonNull
 *  com.google.gson.JsonObject
 *  com.mojang.logging.LogUtils
 *  javax.annotation.Nullable
 *  net.minecraft.core.BlockPos
 *  net.minecraft.core.BlockPos$MutableBlockPos
 *  net.minecraft.core.HolderGetter
 *  net.minecraft.core.Vec3i
 *  net.minecraft.core.registries.BuiltInRegistries
 *  net.minecraft.nbt.CompoundTag
 *  net.minecraft.nbt.IntArrayTag
 *  net.minecraft.nbt.ListTag
 *  net.minecraft.nbt.NbtUtils
 *  net.minecraft.nbt.Tag
 *  net.minecraft.server.level.ServerLevel
 *  net.minecraft.tags.BlockTags
 *  net.minecraft.world.level.BlockGetter
 *  net.minecraft.world.level.block.Block
 *  net.minecraft.world.level.block.Blocks
 *  net.minecraft.world.level.block.CropBlock
 *  net.minecraft.world.level.block.LeavesBlock
 *  net.minecraft.world.level.block.state.BlockState
 *  net.minecraft.world.level.block.state.properties.Property
 *  net.minecraft.world.level.levelgen.Heightmap$Types
 *  org.slf4j.Logger
 */
package org.millenaire.village.path;

import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonNull;
import com.google.gson.JsonObject;
import com.mojang.logging.LogUtils;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.BiConsumer;
import java.util.function.IntFunction;
import java.util.function.Predicate;
import javax.annotation.Nullable;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderGetter;
import net.minecraft.core.Vec3i;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.IntArrayTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.nbt.Tag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.CropBlock;
import net.minecraft.world.level.block.LeavesBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.Property;
import net.minecraft.world.level.levelgen.Heightmap;
import org.millenaire.block.MillPathBlock;
import org.millenaire.block.MillPathSlabBlock;
import org.millenaire.building.BuildingId;
import org.millenaire.building.BuildingInstance;
import org.millenaire.building.BuildingPlanSet;
import org.millenaire.building.SpecialPoint;
import org.millenaire.config.MillenaireServerConfig;
import org.millenaire.culture.ModCultures;
import org.millenaire.culture.VillageType;
import org.millenaire.item.BlockHelper;
import org.millenaire.village.Village;
import org.millenaire.village.path.AStarFailureDetail;
import org.millenaire.village.path.FallbackAnchorFinder;
import org.millenaire.village.path.PathBlockPlacer;
import org.millenaire.village.path.PathDiagnostic;
import org.millenaire.village.path.PathEntry;
import org.millenaire.village.path.PathFailureReason;
import org.millenaire.village.path.PathGridAStar;
import org.millenaire.village.path.PathMaterials;
import org.millenaire.village.path.PathRoute;
import org.millenaire.village.path.PathRouter;
import org.millenaire.world.TerrainPreparer;
import org.slf4j.Logger;

public class VillagePathManager {
    private static final Logger LOGGER = LogUtils.getLogger();
    private static final int MAX_A_STAR_NODES = 5000;
    private static final double PATH_AFFINITY_FACTOR = 0.5;
    private static final int REACHABILITY_PROBE_BUDGET = 5000;
    private final List<PathRoute> pendingRoutes = new ArrayList<PathRoute>();
    private final List<List<PathEntry>> pathsToBuild = new ArrayList<List<PathEntry>>();
    private int buildPathIndex;
    private int buildEntryIndex;
    private final List<RouteIds> buildRouteIds = new ArrayList<RouteIds>();
    private List<BlockPos> pathsToClear = new ArrayList<BlockPos>();
    private int clearIndex;
    private final Set<BlockPos> allNewPathPositions = new HashSet<BlockPos>();
    private final Map<BlockPos, Integer> pathLevelByPos = new HashMap<BlockPos, Integer>();
    private final Map<Long, Integer> pathLevelByColumn = new HashMap<Long, Integer>();
    private final Set<Long> previousSurfaceColumns = new HashSet<Long>();
    private final Set<Long> currentRecalcSurfaceColumns = new HashSet<Long>();
    @Nullable
    private BuildingId thBuildingId;
    private final Map<BuildingId, PathDiagnostic> lastDiagnostics = new HashMap<BuildingId, PathDiagnostic>();
    private final List<PathDiagnostic> lateralDiagnostics = new ArrayList<PathDiagnostic>();
    private long lastRecalcTick = Long.MIN_VALUE;
    private final transient Map<BuildingId, DiagSourceInfo> pendingDiagSources = new HashMap<BuildingId, DiagSourceInfo>();
    @Nullable
    private transient BlockPos pendingThPosForDiag;
    private int pathRecheckFailNights = 0;
    private boolean pathsDirty = false;
    private static final int MAX_FOOTPRINT_PROBE = 16;

    public void recalculatePaths(ServerLevel level, Village village, boolean autobuild) {
        if (!((Boolean)MillenaireServerConfig.SERVER.buildPaths.get()).booleanValue()) {
            return;
        }
        VillageType villageType = ModCultures.getVillageType(village.getVillageTypeId());
        if (villageType == null) {
            return;
        }
        this.pathRecheckFailNights = 0;
        ArrayList<PathRouter.BuildingInfo> buildingInfos = new ArrayList<PathRouter.BuildingInfo>();
        ArrayList<PathRouter.NodeInfo> nodeInfos = new ArrayList<PathRouter.NodeInfo>();
        BlockPos thPos = village.getCenter();
        boolean thFound = false;
        this.thBuildingId = null;
        for (BuildingInstance buildingInstance : village.getBuildings()) {
            BuildingPlanSet buildingPlanSet;
            if (!VillagePathManager.isPathContributor(buildingInstance) || (buildingPlanSet = buildingInstance.getPlanSetId() != null ? ModCultures.getBuildingPlanSet(buildingInstance.getPlanSetId()) : null) == null || !buildingPlanSet.isTownHall()) continue;
            thFound = true;
            this.thBuildingId = buildingInstance.getId();
            BlockPos thAnchor = VillagePathManager.resolvePathDestination(buildingInstance);
            if (thAnchor == null) break;
            thPos = thAnchor;
            break;
        }
        HashMap<BuildingId, BlockPos> previousParents = new HashMap<BuildingId, BlockPos>();
        for (Map.Entry<BuildingId, PathDiagnostic> entry : this.lastDiagnostics.entrySet()) {
            PathDiagnostic d = entry.getValue();
            if (d.lateral() || d.destination() == null) continue;
            previousParents.put(entry.getKey(), d.destination());
        }
        this.lastDiagnostics.clear();
        this.lateralDiagnostics.clear();
        this.lastRecalcTick = level.getGameTime();
        this.pendingDiagSources.clear();
        this.pendingThPosForDiag = null;
        Map<BuildingId, DiagSourceInfo> map = this.pendingDiagSources;
        HashMap<BuildingId, PathFailureReason> hashMap = new HashMap<BuildingId, PathFailureReason>();
        Map<Long, Integer> buildingFloorsEarly = VillagePathManager.computeBuildingFloors(village);
        Set<Long> borderCellsEarly = VillagePathManager.computeBorderCells(village);
        BlockPos thPosFinal = thPos;
        for (BuildingInstance buildingInstance : village.getBuildings()) {
            BuildingPlanSet.LevelDef levelDef;
            BuildingPlanSet planSet;
            boolean isTH;
            if (!VillagePathManager.isPathContributor(buildingInstance) || (isTH = (planSet = buildingInstance.getPlanSetId() != null ? ModCultures.getBuildingPlanSet(buildingInstance.getPlanSetId()) : null) != null && planSet.isTownHall())) continue;
            if (planSet != null && planSet.tags().contains("nopaths")) {
                if (buildingInstance.getId() == null) continue;
                hashMap.put(buildingInstance.getId(), PathFailureReason.NO_PATHS_TAG);
                continue;
            }
            BlockPos pathStart = VillagePathManager.resolvePathDestination(buildingInstance);
            boolean usedFallback = false;
            if (pathStart == null && (pathStart = VillagePathManager.findFallbackAnchor(level, buildingInstance, thPosFinal, buildingFloorsEarly, borderCellsEarly)) != null) {
                usedFallback = true;
                String planSetIdStr = buildingInstance.getPlanSetId() != null ? buildingInstance.getPlanSetId().toString() : "unknown";
                LOGGER.info("[Path] village={} building={}@{} fallback_anchor={}", new Object[]{village.getVillageTypeId(), planSetIdStr, buildingInstance.getOrigin(), pathStart});
            }
            if (pathStart == null) {
                if (buildingInstance.getId() == null) continue;
                hashMap.put(buildingInstance.getId(), PathFailureReason.NO_PATH_START);
                continue;
            }
            if (planSet != null && planSet.tags().contains("pathnode")) {
                nodeInfos.add(new PathRouter.NodeInfo(pathStart));
            }
            int pathLevel = 0;
            int pathWidth = 2;
            if (planSet != null && buildingInstance.getVariant() != null && (levelDef = planSet.getLevel(buildingInstance.getVariant(), buildingInstance.getLevel())) != null) {
                pathLevel = levelDef.pathLevel();
                pathWidth = levelDef.pathWidth();
            }
            List<String> tags = planSet != null ? planSet.tags() : List.of();
            buildingInfos.add(new PathRouter.BuildingInfo(buildingInstance.getId(), pathStart, false, tags, pathLevel, pathWidth));
            if (buildingInstance.getId() == null) continue;
            map.put(buildingInstance.getId(), new DiagSourceInfo(buildingInstance, usedFallback, pathLevel));
        }
        for (Map.Entry entry : hashMap.entrySet()) {
            BuildingInstance b = VillagePathManager.findBuildingById(village, (BuildingId)entry.getKey());
            if (b == null) continue;
            String planSetIdStr = b.getPlanSetId() != null ? b.getPlanSetId().toString() : "unknown";
            this.lastDiagnostics.put((BuildingId)entry.getKey(), new PathDiagnostic((BuildingId)entry.getKey(), planSetIdStr, b.getOrigin(), 0, 0, b.getOrigin(), false, thPos, false, (PathFailureReason)((Object)entry.getValue()), AStarFailureDetail.empty(), 0, 0));
        }
        if (!thFound) {
            LOGGER.warn("Path: NO town hall found in village {}!", (Object)village.getVillageTypeId());
        }
        this.pendingThPosForDiag = thPos;
        HashSet<BlockPos> unreachableNodePos = new HashSet<BlockPos>();
        for (PathRouter.NodeInfo ni : nodeInfos) {
            if (VillagePathManager.isReachableFromTH(level, ni.pos(), thPos, buildingFloorsEarly, borderCellsEarly, 5000)) continue;
            unreachableNodePos.add(ni.pos());
            LOGGER.warn("Path: pathnode at {} unreachable from TH \u2014 excluded from routing", (Object)ni.pos());
        }
        List<PathRouter.NodeInfo> list = nodeInfos.stream().filter(n -> !unreachableNodePos.contains(n.pos())).toList();
        Set<BuildingId> unreachableIds = Set.of();
        ArrayList<PathRoute> routes = new ArrayList<PathRoute>(PathRouter.computeRoutes(buildingInfos, list, thPos, villageType.pathMaterials(), unreachableIds, previousParents));
        routes.sort(Comparator.comparingInt(PathRoute::pathLevel).reversed().thenComparing(Comparator.comparingDouble(r -> r.from().distSqr((Vec3i)r.to())).reversed()).thenComparingInt(r -> r.from().getX()).thenComparingInt(r -> r.from().getZ()).thenComparingInt(r -> r.to().getX()).thenComparingInt(r -> r.to().getZ()));
        if (autobuild) {
            HashSet<BlockPos> newPositions = new HashSet<BlockPos>();
            HashMap<BlockPos, Integer> newLevels = new HashMap<BlockPos, Integer>();
            HashMap<Long, Integer> newLevelsByColumn = new HashMap<Long, Integer>();
            Map<Long, Integer> buildingFloors = VillagePathManager.computeBuildingFloors(village);
            Set<Long> borderCells = VillagePathManager.computeBorderCells(village);
            int routeFail = 0;
            this.currentRecalcSurfaceColumns.clear();
            PathGridAStar.PathAffinity affinity = this.surfaceAffinity();
            for (PathRoute route : routes) {
                TraceResult tr = VillagePathManager.findTraceWithFallbackDetailed(level, village, route, buildingFloors, borderCells, affinity);
                List<BlockPos> trace = tr.trace();
                if (trace == null) {
                    ++routeFail;
                    this.recordDiagnostic(route, tr, map, thPos, 0);
                    continue;
                }
                PathMaterials.MaterialPair mat = PathMaterials.resolve(route.material());
                if (mat == null) {
                    this.recordDiagnosticExplicit(route, map, thPos, trace.size(), 0, PathFailureReason.UNKNOWN_MATERIAL, tr.detail());
                    continue;
                }
                int routePathLevel = route.pathLevel();
                BuildingId routeSourceId = route.sourceId();
                BuildingId routeDestId = route.destinationId();
                BuildingId thIdSnap = this.thBuildingId;
                List<PathEntry> entries = PathBlockPlacer.buildPath(trace, mat.fullBlock(), mat.slabBlock(), route.width(), routePathLevel, pos -> VillagePathManager.isProtectedFromPathBuilding(level, village, pos) || VillagePathManager.isInsideForeignFootprint(village, pos, routeSourceId, routeDestId, thIdSnap), pos -> VillagePathManager.isHardProtectedFromPath(level, village, pos), pos -> VillagePathManager.isReplaceableForPath(level, pos) && !VillagePathManager.isSurfaceLiquid(level, pos), pos -> newLevelsByColumn.getOrDefault(VillagePathManager.packXZ(pos.getX(), pos.getZ()), -1), pos -> VillagePathManager.hasHeadroom(level, pos), (x, z) -> VillagePathManager.groundForPathing(level, buildingFloors, x, z, true), pos -> VillagePathManager.canFillBelowPath(level, pos), pos -> VillagePathManager.canCutForHeadroom(level, pos), (x, z) -> VillagePathManager.lockedSurfaceHalfYAt(buildingFloors, x, z), pos -> VillagePathManager.footprintIdAt(village, pos), pos -> VillagePathManager.isOnStablePath(level, pos), pos -> VillagePathManager.headroomClearableAt(level, pos));
                for (PathEntry entry : entries) {
                    if (VillagePathManager.wouldOverwriteHigherPath(level, entry.pos(), entry.state(), villageType.pathMaterials())) continue;
                    level.setBlock(entry.pos(), entry.state(), 3);
                    newPositions.add(entry.pos());
                    newLevels.put(entry.pos(), routePathLevel);
                    if (!VillagePathManager.isSurfaceState(entry.state())) continue;
                    long col = VillagePathManager.packXZ(entry.pos().getX(), entry.pos().getZ());
                    this.currentRecalcSurfaceColumns.add(col);
                    newLevelsByColumn.merge(col, routePathLevel, Math::max);
                }
                this.recordDiagnostic(route, tr, map, thPos, entries.size());
            }
            this.previousSurfaceColumns.clear();
            this.previousSurfaceColumns.addAll(this.currentRecalcSurfaceColumns);
            if (routeFail > 0) {
                LOGGER.warn("Path autobuild: {} of {} routes failed A*", (Object)routeFail, (Object)routes.size());
            }
            this.rewriteDiagnosticsConnectivity(newLevels, thPos, this.foreignFootprintProbe(village), village);
            this.pathLevelByPos.clear();
            this.pathLevelByPos.putAll(newLevels);
            this.pathLevelByColumn.clear();
            this.pathLevelByColumn.putAll(newLevelsByColumn);
            newPositions.addAll(this.pathLevelByPos.keySet());
            this.clearOldPaths(level, village, newPositions);
        } else {
            this.pendingRoutes.clear();
            this.pendingRoutes.addAll(routes);
            this.pathsToBuild.clear();
            this.buildRouteIds.clear();
            this.buildPathIndex = 0;
            this.buildEntryIndex = 0;
            this.pathsToClear.clear();
            this.clearIndex = 0;
            this.allNewPathPositions.clear();
            this.pathLevelByPos.clear();
            this.pathLevelByColumn.clear();
            this.currentRecalcSurfaceColumns.clear();
        }
    }

    public void tick(ServerLevel level, Village village) {
        BlockPos thPosSnap;
        if (this.pendingRoutes.isEmpty()) {
            return;
        }
        PathRoute route = this.pendingRoutes.removeFirst();
        Map<Long, Integer> buildingFloors = VillagePathManager.computeBuildingFloors(village);
        Set<Long> borderCells = VillagePathManager.computeBorderCells(village);
        TraceResult tr = VillagePathManager.findTraceWithFallbackDetailed(level, village, route, buildingFloors, borderCells, this.surfaceAffinity());
        List<BlockPos> trace = tr.trace();
        BlockPos blockPos = thPosSnap = this.pendingThPosForDiag != null ? this.pendingThPosForDiag : village.getCenter();
        if (trace == null) {
            this.recordDiagnostic(route, tr, this.pendingDiagSources, thPosSnap, 0);
            this.checkRoutesComplete(level, village);
            return;
        }
        PathMaterials.MaterialPair mat = PathMaterials.resolve(route.material());
        if (mat == null) {
            LOGGER.warn("Unknown path material: {}", (Object)route.material());
            this.recordDiagnosticExplicit(route, this.pendingDiagSources, thPosSnap, trace.size(), 0, PathFailureReason.UNKNOWN_MATERIAL, tr.detail());
            this.checkRoutesComplete(level, village);
            return;
        }
        int routePathLevel = route.pathLevel();
        BuildingId routeSourceIdT = route.sourceId();
        BuildingId routeDestIdT = route.destinationId();
        BuildingId thIdSnapT = this.thBuildingId;
        List<PathEntry> entries = PathBlockPlacer.buildPath(trace, mat.fullBlock(), mat.slabBlock(), route.width(), routePathLevel, pos -> VillagePathManager.isProtectedFromPathBuilding(level, village, pos) || VillagePathManager.isInsideForeignFootprint(village, pos, routeSourceIdT, routeDestIdT, thIdSnapT), pos -> VillagePathManager.isHardProtectedFromPath(level, village, pos), pos -> VillagePathManager.isReplaceableForPath(level, pos) && !VillagePathManager.isSurfaceLiquid(level, pos), pos -> this.pathLevelByColumn.getOrDefault(VillagePathManager.packXZ(pos.getX(), pos.getZ()), -1), pos -> VillagePathManager.hasHeadroom(level, pos), (x, z) -> VillagePathManager.groundForPathing(level, buildingFloors, x, z, true), pos -> VillagePathManager.canFillBelowPath(level, pos), pos -> VillagePathManager.canCutForHeadroom(level, pos), (x, z) -> VillagePathManager.lockedSurfaceHalfYAt(buildingFloors, x, z), pos -> VillagePathManager.footprintIdAt(village, pos), pos -> VillagePathManager.isOnStablePath(level, pos), pos -> VillagePathManager.headroomClearableAt(level, pos));
        if (!entries.isEmpty()) {
            this.pathsToBuild.add(entries);
            this.buildRouteIds.add(new RouteIds(route.sourceId(), route.destinationId()));
            for (PathEntry e : entries) {
                this.allNewPathPositions.add(e.pos());
                this.pathLevelByPos.put(e.pos(), routePathLevel);
                if (!VillagePathManager.isSurfaceState(e.state())) continue;
                long col = VillagePathManager.packXZ(e.pos().getX(), e.pos().getZ());
                this.currentRecalcSurfaceColumns.add(col);
                this.pathLevelByColumn.merge(col, routePathLevel, Math::max);
            }
        }
        this.recordDiagnostic(route, tr, this.pendingDiagSources, thPosSnap, entries.size());
        this.checkRoutesComplete(level, village);
    }

    @Nullable
    private static BuildingInstance findBuildingById(Village village, BuildingId id) {
        for (BuildingInstance b : village.getBuildings()) {
            if (!id.equals(b.getId())) continue;
            return b;
        }
        return null;
    }

    private void recordDiagnostic(PathRoute route, TraceResult tr, Map<BuildingId, DiagSourceInfo> diagSources, BlockPos thPos, int placedBlocks) {
        int traceLen;
        BuildingId sid = route.sourceId();
        if (sid == null) {
            return;
        }
        DiagSourceInfo info = diagSources.get(sid);
        if (info == null) {
            return;
        }
        String planSetIdStr = info.instance().getPlanSetId() != null ? info.instance().getPlanSetId().toString() : "unknown";
        PathFailureReason failure = tr.failure();
        int n = traceLen = tr.trace() == null ? 0 : tr.trace().size();
        if (failure == null && tr.trace() != null && placedBlocks == 0) {
            failure = PathFailureReason.PLACEMENT_EMPTY;
        }
        PathDiagnostic diag = new PathDiagnostic(sid, planSetIdStr, info.instance().getOrigin(), info.expectedTier(), route.pathLevel(), route.from(), info.fallback(), route.to(), false, failure, tr.detail() != null ? tr.detail() : AStarFailureDetail.empty(), traceLen, placedBlocks, route.lateral());
        if (route.lateral()) {
            this.lateralDiagnostics.add(diag);
        } else {
            this.lastDiagnostics.put(sid, diag);
        }
    }

    private void recordDiagnosticExplicit(PathRoute route, Map<BuildingId, DiagSourceInfo> diagSources, BlockPos thPos, int traceLen, int placedBlocks, PathFailureReason failure, AStarFailureDetail detail) {
        BuildingId sid = route.sourceId();
        if (sid == null) {
            return;
        }
        DiagSourceInfo info = diagSources.get(sid);
        if (info == null) {
            return;
        }
        String planSetIdStr = info.instance().getPlanSetId() != null ? info.instance().getPlanSetId().toString() : "unknown";
        PathDiagnostic diag = new PathDiagnostic(sid, planSetIdStr, info.instance().getOrigin(), info.expectedTier(), route.pathLevel(), route.from(), info.fallback(), route.to(), false, failure, detail != null ? detail : AStarFailureDetail.empty(), traceLen, placedBlocks, route.lateral());
        if (route.lateral()) {
            this.lateralDiagnostics.add(diag);
        } else {
            this.lastDiagnostics.put(sid, diag);
        }
    }

    private void rewriteDiagnosticsConnectivity(Map<BlockPos, Integer> placed, BlockPos thPos) {
        this.rewriteDiagnosticsConnectivity(placed, thPos, pos -> false, null);
    }

    private void rewriteDiagnosticsConnectivity(Map<BlockPos, Integer> placed, BlockPos thPos, Predicate<BlockPos> isForeignFootprint, @Nullable Village village) {
        if (this.lastDiagnostics.isEmpty()) {
            return;
        }
        if (this.thBuildingId != null) {
            this.lastDiagnostics.remove(this.thBuildingId);
        }
        HashMap<Long, Integer> placedLong = new HashMap<Long, Integer>();
        for (Map.Entry<BlockPos, Integer> e : placed.entrySet()) {
            BlockPos p = e.getKey();
            placedLong.put(BlockPos.asLong((int)p.getX(), (int)p.getY(), (int)p.getZ()), e.getValue());
        }
        Footprint sinkFootprint = village != null ? VillagePathManager.footprintOfTownhall(village) : null;
        for (Map.Entry<BuildingId, PathDiagnostic> e : new HashMap<BuildingId, PathDiagnostic>(this.lastDiagnostics).entrySet()) {
            boolean connected;
            PathDiagnostic d = e.getValue();
            if (d.source() != null && d.source().getX() == thPos.getX() && d.source().getZ() == thPos.getZ() && Math.abs(d.source().getY() - thPos.getY()) <= 2) {
                connected = true;
            } else {
                Footprint sourceFootprint = village != null ? VillagePathManager.footprintOfBuilding(village, e.getKey()) : null;
                connected = VillagePathManager.traceFromAnchor(placedLong, d.source(), thPos, isForeignFootprint, sourceFootprint, sinkFootprint);
            }
            if (connected == d.connected()) continue;
            this.lastDiagnostics.put(e.getKey(), new PathDiagnostic(d.building(), d.planSetId(), d.origin(), d.expectedTier(), d.effectiveTier(), d.source(), d.sourceIsFallback(), d.destination(), connected, d.failure(), d.astarDetail(), d.traceLength(), d.placedBlocks()));
        }
    }

    @Nullable
    private static Footprint footprintOfBuilding(Village village, BuildingId id) {
        BuildingInstance b = village.findBuildingById(id);
        if (b == null) {
            return null;
        }
        int ox = b.getOrigin().getX();
        int oz = b.getOrigin().getZ();
        return new Footprint(ox + b.getCachedMinX(), ox + b.getCachedMaxX(), oz + b.getCachedMinZ(), oz + b.getCachedMaxZ());
    }

    @Nullable
    private static Footprint footprintOfTownhall(Village village) {
        BuildingInstance th = village.getTownhall();
        if (th == null) {
            return null;
        }
        int ox = th.getOrigin().getX();
        int oz = th.getOrigin().getZ();
        return new Footprint(ox + th.getCachedMinX(), ox + th.getCachedMaxX(), oz + th.getCachedMinZ(), oz + th.getCachedMaxZ());
    }

    private Predicate<BlockPos> foreignFootprintProbe(Village village) {
        return pos -> {
            BuildingInstance b = village.getBuildingAt((BlockPos)pos);
            if (b == null) {
                return false;
            }
            if (this.thBuildingId != null && this.thBuildingId.equals(b.getId())) {
                return false;
            }
            return b.getId() != null;
        };
    }

    @Nullable
    private static List<BlockPos> findTraceWithFallback(ServerLevel level, Village village, PathRoute route, Map<Long, Integer> buildingFloors, Set<Long> borderCells) {
        return VillagePathManager.findTraceWithFallbackDetailed(level, village, route, buildingFloors, borderCells, null).trace();
    }

    private static TraceResult findTraceWithFallbackDetailed(ServerLevel level, Village village, PathRoute route, Map<Long, Integer> buildingFloors, Set<Long> borderCells, @Nullable PathGridAStar.PathAffinity affinity) {
        PathGridAStar.TraversabilityCheck traversable;
        PathGridAStar.GroundHeightProvider ground;
        BlockPos to;
        BlockPos from = route.from();
        PathGridAStar.Result first = PathGridAStar.search(from, to = route.to(), ground = (x, z) -> {
            if (x == from.getX() && z == from.getZ()) {
                return from.getY();
            }
            if (x == to.getX() && z == to.getZ()) {
                return to.getY();
            }
            return VillagePathManager.groundForPathing(level, buildingFloors, x, z, true);
        }, traversable = VillagePathManager.makeTraversable(level, from, to, ground, borderCells, false), PathGridAStar.Weights.defaults(), 5000, affinity, 0.5);
        if (first.success()) {
            AStarFailureDetail d = new AStarFailureDetail(first.nodesExplored(), 0, 0, 0, 0, 0, first.reason(), null, null);
            return new TraceResult(first.path(), d, null);
        }
        PathGridAStar.Result second = PathGridAStar.search(from, to, ground, traversable, PathGridAStar.Weights.relaxed(), 5000, affinity, 0.5);
        if (second.success()) {
            LOGGER.info("Path A* fallback-relaxed succeeded from {} to {} (nodes={}, firstReason={})", new Object[]{from, to, second.nodesExplored(), first.reason()});
            AStarFailureDetail d = new AStarFailureDetail(first.nodesExplored(), second.nodesExplored(), 0, 0, 0, 0, first.reason(), second.reason(), null);
            return new TraceResult(second.path(), d, null);
        }
        PathGridAStar.TraversabilityCheck permissive = VillagePathManager.makeTraversable(level, from, to, ground, borderCells, true);
        PathGridAStar.Result third = PathGridAStar.search(from, to, ground, permissive, PathGridAStar.Weights.relaxed(), 5000, affinity, 0.5);
        if (third.success()) {
            LOGGER.info("Path A* stage-3 permissive succeeded from {} to {} (nodes={}, defaultReason={}, relaxedReason={})", new Object[]{from, to, third.nodesExplored(), first.reason(), second.reason()});
            AStarFailureDetail d = new AStarFailureDetail(first.nodesExplored(), second.nodesExplored(), third.nodesExplored(), 0, 0, 0, first.reason(), second.reason(), third.reason());
            return new TraceResult(third.path(), d, null);
        }
        LOGGER.warn("Path A* failed from {} to {} (distance: {}). default={} nodes, reason={}; relaxed={} nodes, reason={}; permissive={} nodes, reason={}", new Object[]{from, to, String.format("%.0f", Math.sqrt(from.distSqr((Vec3i)to))), first.nodesExplored(), first.reason(), second.nodesExplored(), second.reason(), third.nodesExplored(), third.reason()});
        AStarFailureDetail d = new AStarFailureDetail(first.nodesExplored(), second.nodesExplored(), third.nodesExplored(), 0, 0, 0, first.reason(), second.reason(), third.reason());
        return new TraceResult(null, d, PathFailureReason.A_STAR_FAILED);
    }

    private static boolean isReachableFromTH(ServerLevel level, BlockPos from, BlockPos thPos, Map<Long, Integer> buildingFloors, Set<Long> borderCells, int budget) {
        if (from.equals((Object)thPos)) {
            return true;
        }
        PathGridAStar.GroundHeightProvider ground = (x, z) -> {
            if (x == from.getX() && z == from.getZ()) {
                return from.getY();
            }
            if (x == thPos.getX() && z == thPos.getZ()) {
                return thPos.getY();
            }
            return VillagePathManager.groundForPathing(level, buildingFloors, x, z, true);
        };
        PathGridAStar.TraversabilityCheck traversable = VillagePathManager.makeTraversable(level, from, thPos, ground, borderCells, false);
        PathGridAStar.Result first = PathGridAStar.search(from, thPos, ground, traversable, PathGridAStar.Weights.defaults(), budget);
        if (first.success()) {
            return true;
        }
        PathGridAStar.Result second = PathGridAStar.search(from, thPos, ground, traversable, PathGridAStar.Weights.relaxed(), budget);
        return second.success();
    }

    private static PathGridAStar.TraversabilityCheck makeTraversable(ServerLevel level, BlockPos from, BlockPos to, PathGridAStar.GroundHeightProvider ground, Set<Long> borderCells, boolean permissive) {
        return (x, z) -> {
            if (x == from.getX() && z == from.getZ()) {
                return true;
            }
            if (x == to.getX() && z == to.getZ()) {
                return true;
            }
            if (borderCells.contains(VillagePathManager.packXZ(x, z))) {
                return true;
            }
            int y = ground.heightAt(x, z);
            BlockPos herePos = new BlockPos(x, y, z);
            BlockState here = level.getBlockState(herePos);
            if (here.liquid() || !here.getFluidState().isEmpty()) {
                return false;
            }
            BlockPos surfacePos = new BlockPos(x, y - 1, z);
            BlockState surface = level.getBlockState(surfacePos);
            if (surface.liquid() || !surface.getFluidState().isEmpty()) {
                return false;
            }
            if (surface.isAir()) {
                return false;
            }
            if (permissive) {
                return true;
            }
            return !here.isCollisionShapeFullBlock((BlockGetter)level, herePos);
        };
    }

    @Nullable
    private static BlockPos findFallbackAnchor(ServerLevel level, BuildingInstance b, BlockPos thPos, Map<Long, Integer> buildingFloors, Set<Long> borderCells) {
        Predicate<BlockPos> traversable;
        BlockPos max;
        int ox = b.getOrigin().getX();
        int oz = b.getOrigin().getZ();
        int y = b.getOrigin().getY();
        BlockPos min = new BlockPos(ox + b.getCachedMinX(), y, oz + b.getCachedMinZ());
        List<BlockPos> cands = FallbackAnchorFinder.findCandidates(min, max = new BlockPos(ox + b.getCachedMaxX(), y, oz + b.getCachedMaxZ()), traversable = pos -> VillagePathManager.isTraversableForFallback(level, pos, buildingFloors, borderCells));
        if (cands.isEmpty()) {
            return null;
        }
        cands = FallbackAnchorFinder.sortedByDistance(cands, thPos);
        BlockPos chosen = cands.get(0);
        int groundY = VillagePathManager.groundForPathing(level, buildingFloors, chosen.getX(), chosen.getZ(), true);
        return new BlockPos(chosen.getX(), groundY, chosen.getZ());
    }

    private static boolean isTraversableForFallback(ServerLevel level, BlockPos pos, Map<Long, Integer> buildingFloors, Set<Long> borderCells) {
        if (borderCells.contains(VillagePathManager.packXZ(pos.getX(), pos.getZ()))) {
            return true;
        }
        int y = VillagePathManager.groundForPathing(level, buildingFloors, pos.getX(), pos.getZ(), true);
        BlockPos here = new BlockPos(pos.getX(), y, pos.getZ());
        BlockState hereState = level.getBlockState(here);
        if (hereState.liquid() || !hereState.getFluidState().isEmpty()) {
            return false;
        }
        BlockPos surfacePos = new BlockPos(pos.getX(), y - 1, pos.getZ());
        BlockState surface = level.getBlockState(surfacePos);
        if (surface.liquid() || !surface.getFluidState().isEmpty()) {
            return false;
        }
        if (surface.isAir()) {
            return false;
        }
        return !hereState.isCollisionShapeFullBlock((BlockGetter)level, here);
    }

    static boolean traceFromAnchor(Map<Long, Integer> placedPositions, BlockPos anchor, BlockPos sink) {
        return VillagePathManager.traceFromAnchor(placedPositions, anchor, sink, pos -> false);
    }

    static boolean traceFromAnchor(Map<Long, Integer> placedPositions, BlockPos anchor, BlockPos sink, Predicate<BlockPos> isForeignFootprint) {
        return VillagePathManager.traceFromAnchor(placedPositions, anchor, sink, isForeignFootprint, null, null);
    }

    static boolean traceFromAnchor(Map<Long, Integer> placedPositions, BlockPos anchor, BlockPos sink, Predicate<BlockPos> isForeignFootprint, @Nullable Footprint sourceFootprint, @Nullable Footprint sinkFootprint) {
        int seedMaxZ;
        int seedMaxX;
        ArrayList<long[]> seeds = new ArrayList<long[]>();
        if (sourceFootprint != null) {
            seedMinX = sourceFootprint.minX() - 1;
            seedMaxX = sourceFootprint.maxX() + 1;
            seedMinZ = sourceFootprint.minZ() - 1;
            seedMaxZ = sourceFootprint.maxZ() + 1;
        } else {
            seedMinX = anchor.getX() - 1;
            seedMaxX = anchor.getX() + 1;
            seedMinZ = anchor.getZ() - 1;
            seedMaxZ = anchor.getZ() + 1;
        }
        for (int x = seedMinX; x <= seedMaxX; ++x) {
            for (int z = seedMinZ; z <= seedMaxZ; ++z) {
                for (int dy = -2; dy <= 1; ++dy) {
                    int y = anchor.getY() + dy;
                    long k = BlockPos.asLong((int)x, (int)y, (int)z);
                    if (!placedPositions.containsKey(k)) continue;
                    seeds.add(new long[]{k, x, y, z});
                }
            }
        }
        if (seeds.isEmpty()) {
            return false;
        }
        int sinkMinX = sinkFootprint != null ? sinkFootprint.minX() - 1 : sink.getX() - 1;
        int sinkMaxX = sinkFootprint != null ? sinkFootprint.maxX() + 1 : sink.getX() + 1;
        int sinkMinZ = sinkFootprint != null ? sinkFootprint.minZ() - 1 : sink.getZ() - 1;
        int sinkMaxZ = sinkFootprint != null ? sinkFootprint.maxZ() + 1 : sink.getZ() + 1;
        Predicate<long[]> atSink = c -> {
            int cx = (int)c[1];
            int cy = (int)c[2];
            int cz = (int)c[3];
            if (cx < sinkMinX || cx > sinkMaxX) {
                return false;
            }
            if (cz < sinkMinZ || cz > sinkMaxZ) {
                return false;
            }
            int dy = cy - sink.getY();
            return dy >= -2 && dy <= 1;
        };
        HashSet<Long> visited = new HashSet<Long>();
        ArrayDeque<int[]> queue = new ArrayDeque<int[]>();
        for (long[] seed : seeds) {
            if (!visited.add(seed[0])) continue;
            if (atSink.test(seed)) {
                return true;
            }
            queue.add(new int[]{(int)seed[1], (int)seed[2], (int)seed[3]});
        }
        int[][] dirs = new int[][]{{1, 0}, {-1, 0}, {0, 1}, {0, -1}};
        while (!queue.isEmpty()) {
            int[] c2 = (int[])queue.poll();
            for (int[] d : dirs) {
                BlockPos probe0;
                int nx = c2[0] + d[0];
                int nz = c2[2] + d[1];
                boolean placedNeighbour = false;
                for (int dy = -1; dy <= 1; ++dy) {
                    int ny = c2[1] + dy;
                    long k = BlockPos.asLong((int)nx, (int)ny, (int)nz);
                    if (!placedPositions.containsKey(k)) continue;
                    placedNeighbour = true;
                    if (!visited.add(k)) continue;
                    long[] node = new long[]{k, nx, ny, nz};
                    if (atSink.test(node)) {
                        return true;
                    }
                    queue.add(new int[]{nx, ny, nz});
                }
                if (placedNeighbour || !isForeignFootprint.test(probe0 = new BlockPos(nx, c2[1], nz))) continue;
                boolean foundBridge = false;
                block7: for (int step = 2; step <= 16 && !foundBridge; ++step) {
                    int px = c2[0] + d[0] * step;
                    int pz = c2[2] + d[1] * step;
                    for (int dy = -step; dy <= step && !foundBridge; ++dy) {
                        int py = c2[1] + dy;
                        long k = BlockPos.asLong((int)px, (int)py, (int)pz);
                        if (!placedPositions.containsKey(k)) continue;
                        if (!visited.add(k)) {
                            foundBridge = true;
                            continue block7;
                        }
                        long[] node = new long[]{k, px, py, pz};
                        if (atSink.test(node)) {
                            return true;
                        }
                        queue.add(new int[]{px, py, pz});
                        foundBridge = true;
                    }
                }
            }
        }
        return false;
    }

    private void checkRoutesComplete(ServerLevel level, Village village) {
        if (this.pendingRoutes.isEmpty()) {
            this.snapshotSurfaceColumns();
            this.pathsToClear = this.scanExistingPaths(level, village, this.allNewPathPositions);
            this.clearIndex = 0;
            LOGGER.info("Path calculation complete: {} path lists, {} blocks to clear", (Object)this.pathsToBuild.size(), (Object)this.pathsToClear.size());
            if (this.pendingThPosForDiag != null) {
                this.rewriteDiagnosticsConnectivity(this.pathLevelByPos, this.pendingThPosForDiag, this.foreignFootprintProbe(village), village);
            }
            village.markDirty();
        }
    }

    public Map<BuildingId, PathDiagnostic> getLastDiagnostics() {
        return Collections.unmodifiableMap(this.lastDiagnostics);
    }

    public List<PathDiagnostic> getLateralDiagnostics() {
        return Collections.unmodifiableList(this.lateralDiagnostics);
    }

    public boolean isDiagnosticStale(ServerLevel level) {
        if (this.lastRecalcTick == Long.MIN_VALUE) {
            return true;
        }
        return level.getGameTime() - this.lastRecalcTick > 600L;
    }

    public boolean hasPathsToClear() {
        return this.clearIndex < this.pathsToClear.size();
    }

    public boolean hasPathsToBuild() {
        return this.buildPathIndex < this.pathsToBuild.size();
    }

    @Nullable
    public BlockPos getNextClearPos() {
        if (!this.hasPathsToClear()) {
            return null;
        }
        return this.pathsToClear.get(this.clearIndex);
    }

    public void advanceClear() {
        ++this.clearIndex;
    }

    @Nullable
    public PathEntry getNextBuildEntry() {
        if (!this.hasPathsToBuild()) {
            return null;
        }
        List<PathEntry> current = this.pathsToBuild.get(this.buildPathIndex);
        if (this.buildEntryIndex >= current.size()) {
            return null;
        }
        return current.get(this.buildEntryIndex);
    }

    public void advanceBuild() {
        if (!this.hasPathsToBuild()) {
            return;
        }
        ++this.buildEntryIndex;
        if (this.buildEntryIndex >= this.pathsToBuild.get(this.buildPathIndex).size()) {
            ++this.buildPathIndex;
            this.buildEntryIndex = 0;
        }
    }

    public boolean isCurrentBuildEntryInForeignFootprint(Village village, BlockPos pos) {
        if (this.buildPathIndex < 0 || this.buildPathIndex >= this.buildRouteIds.size()) {
            return false;
        }
        RouteIds ids = this.buildRouteIds.get(this.buildPathIndex);
        return VillagePathManager.isInsideForeignFootprint(village, pos, ids.source(), ids.dest(), this.thBuildingId);
    }

    public void clearAllPathsNow(ServerLevel level) {
        while (this.hasPathsToClear()) {
            BlockPos pos = this.getNextClearPos();
            if (pos != null && level.isLoaded(pos)) {
                level.removeBlock(pos, false);
            }
            this.advanceClear();
        }
        for (BlockPos pos : new ArrayList<BlockPos>(this.allNewPathPositions)) {
            if (!level.isLoaded(pos)) continue;
            level.removeBlock(pos, false);
        }
        int cleared = this.allNewPathPositions.size();
        this.allNewPathPositions.clear();
        this.pathLevelByPos.clear();
        this.pathLevelByColumn.clear();
        LOGGER.debug("Bulk-cleared {} path blocks", (Object)cleared);
    }

    private static boolean isProtectedFromPathBuilding(ServerLevel level, Village village, BlockPos pos) {
        BuildingPlanSet planSet;
        BuildingInstance building = village.getBuildingAt(pos);
        if (building == null) {
            return false;
        }
        if (building.getPlanSetId() != null && (planSet = ModCultures.getBuildingPlanSet(building.getPlanSetId())) != null && planSet.tags().contains("nopaths")) {
            return true;
        }
        return VillagePathManager.matchesSoilOrSource(building, pos);
    }

    private static boolean isInsideForeignFootprint(Village village, BlockPos pos, @Nullable BuildingId sourceId, @Nullable BuildingId destinationId, @Nullable BuildingId thId) {
        BuildingInstance building = village.getBuildingAt(pos);
        if (building == null) {
            return false;
        }
        BuildingId bid = building.getId();
        if (bid == null) {
            return false;
        }
        if (sourceId != null && bid.equals(sourceId)) {
            return false;
        }
        if (destinationId != null && bid.equals(destinationId)) {
            return false;
        }
        return thId == null || !bid.equals(thId);
    }

    private static boolean isHardProtectedFromPath(ServerLevel level, Village village, BlockPos pos) {
        BuildingInstance building = village.getBuildingAt(pos);
        if (building == null) {
            return false;
        }
        return VillagePathManager.matchesSoilOrSource(building, pos);
    }

    private static boolean matchesSoilOrSource(BuildingInstance building, BlockPos pos) {
        for (SpecialPoint sp : building.getResolvedPoints()) {
            BlockPos spPos;
            String type = sp.type();
            if (!"soil".equals(type) && !"source".equals(type) || !(spPos = sp.pos()).equals((Object)pos) && !spPos.above().equals((Object)pos) && !spPos.below().equals((Object)pos)) continue;
            return true;
        }
        return false;
    }

    private static boolean isSurfaceLiquid(ServerLevel level, BlockPos pos) {
        BlockState below = level.getBlockState(pos.below());
        return below.liquid() || !below.getFluidState().isEmpty();
    }

    private static boolean hasHeadroom(ServerLevel level, BlockPos pos) {
        BlockState above1 = level.getBlockState(pos.above());
        BlockState above2 = level.getBlockState(pos.above(2));
        return VillagePathManager.isPassable(above1) && VillagePathManager.isPassable(above2);
    }

    private static boolean isPassable(BlockState state) {
        if (state.isAir()) {
            return true;
        }
        if (state.liquid()) {
            return false;
        }
        if (state.getBlock() instanceof LeavesBlock) {
            return true;
        }
        return !state.isSolid();
    }

    private static boolean headroomClearableAt(ServerLevel level, BlockPos surfacePos) {
        for (int dy = 1; dy <= 2; ++dy) {
            BlockPos p = surfacePos.above(dy);
            BlockState s = level.getBlockState(p);
            if (VillagePathManager.isPassable(s) || VillagePathManager.canCutForHeadroom(level, p)) continue;
            return false;
        }
        return true;
    }

    private void clearOldPaths(ServerLevel level, Village village, Set<BlockPos> newPositions) {
        List<BlockPos> toClear = this.scanExistingPaths(level, village, newPositions);
        for (BlockPos pos : toClear) {
            BlockState current = level.getBlockState(pos);
            level.setBlock(pos, VillagePathManager.pathClearReplacement(current, level.getBlockState(pos.below())), 3);
        }
    }

    public static BlockState pathClearReplacement(BlockState current, BlockState below) {
        if (current.getBlock() instanceof MillPathSlabBlock) {
            return Blocks.AIR.defaultBlockState();
        }
        BlockState ground = VillagePathManager.validGroundForClear(below);
        return ground != null ? ground : Blocks.DIRT.defaultBlockState();
    }

    @Nullable
    static BlockState validGroundForClear(BlockState below) {
        Block b = below.getBlock();
        if (b == Blocks.BEDROCK) {
            return Blocks.DIRT.defaultBlockState();
        }
        if (b == Blocks.STONE) {
            return Blocks.DIRT.defaultBlockState();
        }
        if (b == Blocks.DIRT) {
            return below;
        }
        if (b == Blocks.GRASS_BLOCK) {
            return Blocks.DIRT.defaultBlockState();
        }
        if (b == Blocks.GRAVEL) {
            return below;
        }
        if (b == Blocks.SAND || b == Blocks.RED_SAND) {
            return below;
        }
        if (b == Blocks.SANDSTONE) {
            return Blocks.SAND.defaultBlockState();
        }
        if (b == Blocks.TERRACOTTA) {
            return below;
        }
        return null;
    }

    private List<BlockPos> scanExistingPaths(ServerLevel level, Village village, Set<BlockPos> newPositions) {
        ArrayList<BlockPos> result = new ArrayList<BlockPos>();
        int radius = 80;
        VillageType vt = ModCultures.getVillageType(village.getVillageTypeId());
        if (vt != null) {
            radius = vt.radius();
        }
        int cx = village.getCenter().getX();
        int cz = village.getCenter().getZ();
        BlockPos.MutableBlockPos mutable = new BlockPos.MutableBlockPos();
        for (int x = cx - radius; x <= cx + radius; ++x) {
            for (int z = cz - radius; z <= cz + radius; ++z) {
                int terrainY = level.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, x, z) - 1;
                for (int y = terrainY - 2; y <= terrainY + 3; ++y) {
                    mutable.set(x, y, z);
                    BlockState state = level.getBlockState((BlockPos)mutable);
                    if (!VillagePathManager.isSystemPathBlock(state) || newPositions.contains(mutable)) continue;
                    result.add(mutable.immutable());
                }
            }
        }
        return result;
    }

    private static boolean isSystemPathBlock(BlockState state) {
        if (state.getBlock() instanceof MillPathBlock) {
            return (Boolean)state.getValue((Property)MillPathBlock.STABLE) == false;
        }
        if (state.getBlock() instanceof MillPathSlabBlock) {
            return (Boolean)state.getValue((Property)MillPathSlabBlock.STABLE) == false;
        }
        return false;
    }

    public static boolean wouldOverwriteHigherPath(ServerLevel level, BlockPos pos, BlockState newState, List<String> orderedMaterials) {
        BlockState current = level.getBlockState(pos);
        if (!VillagePathManager.isSystemPathBlock(current)) {
            return false;
        }
        int currentRank = PathMaterials.rankOf(current, orderedMaterials);
        if (currentRank < 0) {
            return false;
        }
        return currentRank > PathMaterials.rankOf(newState, orderedMaterials);
    }

    @Nullable
    private static Object footprintIdAt(Village village, BlockPos pos) {
        BuildingInstance b = village.getBuildingAt(pos);
        return b == null ? null : b.getId();
    }

    private static boolean isOnStablePath(ServerLevel level, BlockPos pos) {
        return VillagePathManager.isStablePathBlock(level.getBlockState(pos)) || VillagePathManager.isStablePathBlock(level.getBlockState(pos.below()));
    }

    private static boolean isStablePathBlock(BlockState state) {
        if (state.getBlock() instanceof MillPathBlock) {
            return (Boolean)state.getValue((Property)MillPathBlock.STABLE);
        }
        if (state.getBlock() instanceof MillPathSlabBlock) {
            return (Boolean)state.getValue((Property)MillPathSlabBlock.STABLE);
        }
        return false;
    }

    static boolean isReplaceableForPath(ServerLevel level, BlockPos pos) {
        BlockState state = level.getBlockState(pos);
        if (state.liquid() || !state.getFluidState().isEmpty()) {
            return false;
        }
        if (VillagePathManager.isSystemPathBlock(state)) {
            return true;
        }
        Block block = state.getBlock();
        if (block == Blocks.GRASS_BLOCK || block == Blocks.SAND || block == Blocks.RED_SAND || block == Blocks.GRAVEL || block == Blocks.CLAY || block == Blocks.TERRACOTTA || state.is(BlockTags.DIRT)) {
            return true;
        }
        if (block instanceof CropBlock) {
            return false;
        }
        return BlockHelper.isDecorativeFlower(state) || state.is(BlockTags.REPLACEABLE);
    }

    @Nullable
    private static BlockPos resolvePathDestination(BuildingInstance b) {
        return b.resolvePathAnchor();
    }

    private static boolean isPathContributor(BuildingInstance b) {
        return !b.isSubBuilding() && b.getStatus() == BuildingInstance.Status.COMPLETE;
    }

    private static Map<Long, Integer> computeBuildingFloors(Village village) {
        Integer existing;
        long key;
        int z;
        int x;
        int maxZ;
        int minZ;
        int maxX;
        int minX;
        int oz;
        int ox;
        HashMap<Long, Integer> out = new HashMap<Long, Integer>();
        for (BuildingInstance b : village.getBuildings()) {
            BlockPos floor;
            if (!VillagePathManager.isPathContributor(b) || (floor = VillagePathManager.resolvePathDestination(b)) == null) continue;
            int floorY = floor.getY();
            ox = b.getOrigin().getX();
            oz = b.getOrigin().getZ();
            minX = ox + b.getCachedMinX();
            maxX = ox + b.getCachedMaxX();
            minZ = oz + b.getCachedMinZ();
            maxZ = oz + b.getCachedMaxZ();
            for (x = minX; x <= maxX; ++x) {
                for (z = minZ; z <= maxZ; ++z) {
                    key = VillagePathManager.packXZ(x, z);
                    existing = (Integer)out.get(key);
                    if (existing != null && floorY >= existing) continue;
                    out.put(key, floorY);
                }
            }
        }
        HashSet footprintKeys = new HashSet(out.keySet());
        for (BuildingInstance b : village.getBuildings()) {
            if (!VillagePathManager.isPathContributor(b)) continue;
            int wallY = b.getWallFootY();
            ox = b.getOrigin().getX();
            oz = b.getOrigin().getZ();
            minX = ox + b.getCachedMinX() - 1;
            maxX = ox + b.getCachedMaxX() + 1;
            minZ = oz + b.getCachedMinZ() - 1;
            maxZ = oz + b.getCachedMaxZ() + 1;
            for (x = minX; x <= maxX; ++x) {
                for (z = minZ; z <= maxZ; ++z) {
                    key = VillagePathManager.packXZ(x, z);
                    if (footprintKeys.contains(key) || (existing = (Integer)out.get(key)) != null && wallY >= existing) continue;
                    out.put(key, wallY);
                }
            }
        }
        return out;
    }

    private static Set<Long> computeBorderCells(Village village) {
        HashSet<Long> footprint = new HashSet<Long>();
        for (BuildingInstance b : village.getBuildings()) {
            if (!VillagePathManager.isPathContributor(b)) continue;
            int ox = b.getOrigin().getX();
            int oz = b.getOrigin().getZ();
            int minX = ox + b.getCachedMinX();
            int maxX = ox + b.getCachedMaxX();
            int minZ = oz + b.getCachedMinZ();
            int maxZ = oz + b.getCachedMaxZ();
            for (int x = minX; x <= maxX; ++x) {
                for (int z = minZ; z <= maxZ; ++z) {
                    footprint.add(VillagePathManager.packXZ(x, z));
                }
            }
        }
        HashSet<Long> border = new HashSet<Long>();
        for (BuildingInstance b : village.getBuildings()) {
            if (!VillagePathManager.isPathContributor(b)) continue;
            int ox = b.getOrigin().getX();
            int oz = b.getOrigin().getZ();
            int minX = ox + b.getCachedMinX() - 1;
            int maxX = ox + b.getCachedMaxX() + 1;
            int minZ = oz + b.getCachedMinZ() - 1;
            int maxZ = oz + b.getCachedMaxZ() + 1;
            for (int x = minX; x <= maxX; ++x) {
                for (int z = minZ; z <= maxZ; ++z) {
                    long key = VillagePathManager.packXZ(x, z);
                    if (footprint.contains(key)) continue;
                    border.add(key);
                }
            }
        }
        return border;
    }

    private static long packXZ(int x, int z) {
        return (long)x << 32 | (long)z & 0xFFFFFFFFL;
    }

    private static int groundForPathing(ServerLevel level, Map<Long, Integer> buildingFloors, int x, int z, boolean ignorePaths) {
        Integer floorY = buildingFloors.get(VillagePathManager.packXZ(x, z));
        if (floorY != null) {
            return floorY;
        }
        return ignorePaths ? VillagePathManager.groundHeightIgnoringPaths(level, x, z) : TerrainPreparer.getGroundHeight(level, x, z);
    }

    private static int lockedSurfaceHalfYAt(Map<Long, Integer> buildingFloors, int x, int z) {
        Integer floorY = buildingFloors.get(VillagePathManager.packXZ(x, z));
        return floorY != null ? 2 * floorY : Integer.MIN_VALUE;
    }

    private static boolean canFillBelowPath(ServerLevel level, BlockPos pos) {
        BlockState state = level.getBlockState(pos);
        if (state.isAir()) {
            return true;
        }
        if (VillagePathManager.isSystemPathBlock(state)) {
            return true;
        }
        Block block = state.getBlock();
        if (block == Blocks.GRASS_BLOCK || block == Blocks.SAND || block == Blocks.RED_SAND || block == Blocks.GRAVEL || block == Blocks.CLAY || block == Blocks.TERRACOTTA) {
            return true;
        }
        if (state.is(BlockTags.DIRT)) {
            return true;
        }
        if (BlockHelper.isDecorativeFlower(state)) {
            return true;
        }
        if (block instanceof CropBlock) {
            return false;
        }
        return state.is(BlockTags.REPLACEABLE);
    }

    private static boolean canCutForHeadroom(ServerLevel level, BlockPos pos) {
        BlockState state = level.getBlockState(pos);
        if (state.isAir()) {
            return true;
        }
        if (state.is(BlockTags.LEAVES)) {
            return true;
        }
        if (state.is(BlockTags.REPLACEABLE)) {
            return true;
        }
        if (VillagePathManager.isSystemPathBlock(state)) {
            return true;
        }
        Block block = state.getBlock();
        if (block instanceof CropBlock) {
            return false;
        }
        if (block == Blocks.GRASS_BLOCK || block == Blocks.SAND || block == Blocks.RED_SAND || block == Blocks.GRAVEL || block == Blocks.CLAY || block == Blocks.TERRACOTTA) {
            return true;
        }
        return state.is(BlockTags.DIRT);
    }

    private static int groundHeightIgnoringPaths(ServerLevel level, int x, int z) {
        int top = level.getHeight(Heightmap.Types.WORLD_SURFACE, x, z);
        if (top <= level.getMinBuildHeight()) {
            top = level.getMaxBuildHeight();
        }
        BlockPos.MutableBlockPos mutable = new BlockPos.MutableBlockPos();
        return VillagePathManager.scanGroundIgnoringPaths(top, level.getMinBuildHeight(), cy -> {
            mutable.set(x, cy, z);
            return VillagePathManager.classifyColumnCell(level.getBlockState((BlockPos)mutable));
        });
    }

    static ScanCell classifyColumnCell(BlockState state) {
        if (state.isAir()) {
            return ScanCell.SKIP;
        }
        if (state.getBlock() instanceof MillPathBlock) {
            return ScanCell.PATH;
        }
        if (state.getBlock() instanceof MillPathSlabBlock) {
            return ScanCell.PATH;
        }
        if (state.is(BlockTags.LEAVES) || state.is(BlockTags.LOGS)) {
            return ScanCell.SKIP;
        }
        if (state.is(BlockTags.REPLACEABLE)) {
            return ScanCell.SKIP;
        }
        if (!state.getFluidState().isEmpty()) {
            return ScanCell.SKIP;
        }
        if (!state.canOcclude()) {
            return ScanCell.SKIP;
        }
        return ScanCell.GROUND;
    }

    static int scanGroundIgnoringPaths(int topY, int minY, IntFunction<ScanCell> cellAt) {
        boolean pathDirectlyAbove = false;
        block5: for (int cy = topY; cy > minY; --cy) {
            switch (cellAt.apply(cy).ordinal()) {
                case 0: {
                    pathDirectlyAbove = false;
                    continue block5;
                }
                case 1: {
                    pathDirectlyAbove = true;
                    continue block5;
                }
                case 2: {
                    return pathDirectlyAbove ? cy + 2 : cy + 1;
                }
            }
        }
        return minY;
    }

    int getExistingPathLevel(BlockPos pos) {
        return this.pathLevelByPos.getOrDefault(pos, -1);
    }

    public void forEachPath(BiConsumer<BlockPos, Integer> consumer) {
        this.pathLevelByPos.forEach(consumer);
    }

    public static PlacementCheck canPlacePathAt(ServerLevel level, Village village, BlockPos pos) {
        BlockState stateAtPos = level.getBlockState(pos);
        if (stateAtPos.liquid() || !stateAtPos.getFluidState().isEmpty()) {
            return PlacementCheck.BLOCKED;
        }
        if (VillagePathManager.isProtectedFromPathBuilding(level, village, pos)) {
            return PlacementCheck.BLOCKED;
        }
        if (!VillagePathManager.isReplaceableForPath(level, pos) && !VillagePathManager.isSystemPathBlock(level.getBlockState(pos))) {
            return PlacementCheck.BLOCKED;
        }
        if (!VillagePathManager.hasHeadroom(level, pos)) {
            return PlacementCheck.BLOCKED;
        }
        return PlacementCheck.ALLOWED;
    }

    public void addPendingRoute(PathRoute route) {
        this.pendingRoutes.add(route);
    }

    public int getPendingRouteCount() {
        return this.pendingRoutes.size();
    }

    public void addBuiltPath(List<PathEntry> entries) {
        this.pathsToBuild.add(entries);
    }

    public void setPathsToClear(List<BlockPos> positions) {
        this.pathsToClear = new ArrayList<BlockPos>(positions);
    }

    public int getClearIndex() {
        return this.clearIndex;
    }

    public void setClearIndex(int index) {
        this.clearIndex = index;
    }

    public static boolean shouldRunNightlyRecalc(int failNights) {
        if (failNights < 0) {
            return false;
        }
        if (failNights == 0) {
            return true;
        }
        if (failNights <= 64) {
            return (failNights & failNights - 1) == 0;
        }
        return failNights % 64 == 0;
    }

    public void nightlyRecheck(ServerLevel level, Village village) {
        if (!((Boolean)MillenaireServerConfig.SERVER.buildPaths.get()).booleanValue()) {
            return;
        }
        if (!this.pendingRoutes.isEmpty() || this.hasPathsToBuild() || this.hasPathsToClear()) {
            return;
        }
        boolean allOk = this.verifyInvariantAll(level, village);
        if (allOk) {
            if (this.pathRecheckFailNights != 0) {
                LOGGER.info("[Path] village={} nightly recheck ok, reset counter (was {})", (Object)village.getId(), (Object)this.pathRecheckFailNights);
                this.pathRecheckFailNights = 0;
            }
            return;
        }
        ++this.pathRecheckFailNights;
        if (!VillagePathManager.shouldRunNightlyRecalc(this.pathRecheckFailNights)) {
            return;
        }
        LOGGER.info("[Path] village={} nightly recalcul queued (failNights={})", (Object)village.getId(), (Object)this.pathRecheckFailNights);
        this.recalculatePaths(level, village, false);
    }

    public void markPathsDirty() {
        this.pathsDirty = true;
    }

    public void startRecalcIfDirty(ServerLevel level, Village village) {
        if (!this.pathsDirty) {
            return;
        }
        if (!this.pendingRoutes.isEmpty() || this.hasPathsToBuild() || this.hasPathsToClear()) {
            return;
        }
        this.pathsDirty = false;
        this.recalculatePaths(level, village, false);
        village.markDirty();
    }

    private boolean verifyInvariantAll(ServerLevel level, Village village) {
        BlockPos thPos = null;
        for (BuildingInstance b : village.getBuildings()) {
            BuildingPlanSet planSet;
            if (!VillagePathManager.isPathContributor(b) || (planSet = b.getPlanSetId() != null ? ModCultures.getBuildingPlanSet(b.getPlanSetId()) : null) == null || !planSet.isTownHall()) continue;
            thPos = VillagePathManager.resolvePathDestination(b);
            break;
        }
        if (thPos == null) {
            thPos = village.getCenter();
        }
        HashMap<Long, Integer> placed = new HashMap<Long, Integer>(this.pathLevelByPos.size());
        for (Map.Entry<BlockPos, Integer> e : this.pathLevelByPos.entrySet()) {
            BlockPos p = e.getKey();
            placed.put(BlockPos.asLong((int)p.getX(), (int)p.getY(), (int)p.getZ()), e.getValue());
        }
        for (BuildingInstance b : village.getBuildings()) {
            BlockPos anchor;
            PathDiagnostic last;
            BuildingPlanSet planSet;
            if (!VillagePathManager.isPathContributor(b) || (planSet = b.getPlanSetId() != null ? ModCultures.getBuildingPlanSet(b.getPlanSetId()) : null) == null || planSet.isTownHall() || planSet.tags().contains("nopaths") || b.getId() != null && (last = this.lastDiagnostics.get(b.getId())) != null && last.failure() == PathFailureReason.UNREACHABLE_TERRAIN || (anchor = VillagePathManager.resolvePathDestination(b)) == null || VillagePathManager.traceFromAnchor(placed, anchor, thPos, this.foreignFootprintProbe(village))) continue;
            return false;
        }
        return true;
    }

    public void save(CompoundTag tag) {
        ListTag routesList = new ListTag();
        for (PathRoute pathRoute : this.pendingRoutes) {
            CompoundTag compoundTag = new CompoundTag();
            compoundTag.putIntArray("from", new int[]{pathRoute.from().getX(), pathRoute.from().getY(), pathRoute.from().getZ()});
            compoundTag.putIntArray("to", new int[]{pathRoute.to().getX(), pathRoute.to().getY(), pathRoute.to().getZ()});
            compoundTag.putString("material", pathRoute.material());
            compoundTag.putInt("width", pathRoute.width());
            compoundTag.putInt("path_level", pathRoute.pathLevel());
            compoundTag.putBoolean("lateral", pathRoute.lateral());
            routesList.add((Object)compoundTag);
        }
        tag.put("pending_routes", (Tag)routesList);
        ListTag buildList = new ListTag();
        for (List<PathEntry> list : this.pathsToBuild) {
            CompoundTag pathTag = new CompoundTag();
            ListTag entriesList = new ListTag();
            for (PathEntry entry : list) {
                CompoundTag entryTag = new CompoundTag();
                entryTag.putIntArray("pos", new int[]{entry.pos().getX(), entry.pos().getY(), entry.pos().getZ()});
                entryTag.put("state", (Tag)NbtUtils.writeBlockState((BlockState)entry.state()));
                entriesList.add((Object)entryTag);
            }
            pathTag.put("entries", (Tag)entriesList);
            buildList.add((Object)pathTag);
        }
        tag.put("paths_to_build", (Tag)buildList);
        tag.putInt("build_path_index", this.buildPathIndex);
        tag.putInt("build_entry_index", this.buildEntryIndex);
        ListTag listTag = new ListTag();
        for (BlockPos pos : this.pathsToClear) {
            listTag.add((Object)new IntArrayTag(new int[]{pos.getX(), pos.getY(), pos.getZ()}));
        }
        tag.put("paths_to_clear", (Tag)listTag);
        tag.putInt("clear_index", this.clearIndex);
        ListTag listTag2 = new ListTag();
        for (Map.Entry<BlockPos, Integer> entry : this.pathLevelByPos.entrySet()) {
            CompoundTag e = new CompoundTag();
            BlockPos pos = entry.getKey();
            e.putIntArray("pos", new int[]{pos.getX(), pos.getY(), pos.getZ()});
            e.putInt("level", entry.getValue().intValue());
            listTag2.add((Object)e);
        }
        tag.put("path_levels", (Tag)listTag2);
        tag.putInt("recheck_fail_nights", this.pathRecheckFailNights);
    }

    @Nullable
    private static BlockPos readPos(int[] arr) {
        if (arr == null || arr.length != 3) {
            return null;
        }
        return new BlockPos(arr[0], arr[1], arr[2]);
    }

    /*
     * WARNING - void declaration
     */
    public void load(CompoundTag tag) {
        this.pendingRoutes.clear();
        if (tag.contains("pending_routes")) {
            void var3_4;
            ListTag routesList = tag.getList("pending_routes", 10);
            boolean bl = false;
            while (var3_4 < routesList.size()) {
                try {
                    CompoundTag routeTag = routesList.getCompound((int)var3_4);
                    BlockPos from = VillagePathManager.readPos(routeTag.getIntArray("from"));
                    BlockPos to = VillagePathManager.readPos(routeTag.getIntArray("to"));
                    if (from == null || to == null) {
                        LOGGER.warn("Skipping corrupt pending route {} (malformed from/to)", (Object)((int)var3_4));
                    } else {
                        String material = routeTag.getString("material");
                        int width = routeTag.getInt("width");
                        int pathLevel = routeTag.getInt("path_level");
                        boolean lateral = routeTag.getBoolean("lateral");
                        this.pendingRoutes.add(new PathRoute(from, to, material, width, pathLevel, null, null, lateral));
                    }
                }
                catch (RuntimeException e) {
                    LOGGER.warn("Skipping corrupt pending route {}: {}", (Object)((int)var3_4), (Object)e.toString());
                }
                ++var3_4;
            }
        }
        this.pathsToBuild.clear();
        if (tag.contains("paths_to_build")) {
            void var3_6;
            ListTag buildList = tag.getList("paths_to_build", 10);
            boolean bl = false;
            while (var3_6 < buildList.size()) {
                try {
                    CompoundTag pathTag = buildList.getCompound((int)var3_6);
                    ListTag entriesList = pathTag.getList("entries", 10);
                    ArrayList<PathEntry> entries = new ArrayList<PathEntry>();
                    for (int j = 0; j < entriesList.size(); ++j) {
                        try {
                            CompoundTag entryTag = entriesList.getCompound(j);
                            BlockPos pos = VillagePathManager.readPos(entryTag.getIntArray("pos"));
                            if (pos == null) {
                                LOGGER.warn("Skipping corrupt path entry {}/{} (malformed pos)", (Object)((int)var3_6), (Object)j);
                                continue;
                            }
                            BlockState state = NbtUtils.readBlockState((HolderGetter)BuiltInRegistries.BLOCK.asLookup(), (CompoundTag)entryTag.getCompound("state"));
                            if (state.isAir()) continue;
                            entries.add(new PathEntry(pos, state));
                            continue;
                        }
                        catch (RuntimeException e) {
                            LOGGER.warn("Skipping corrupt path entry {}/{}: {}", new Object[]{(int)var3_6, j, e.toString()});
                        }
                    }
                    if (!entries.isEmpty()) {
                        this.pathsToBuild.add(entries);
                    }
                }
                catch (RuntimeException e) {
                    LOGGER.warn("Skipping corrupt path-to-build {}: {}", (Object)((int)var3_6), (Object)e.toString());
                }
                ++var3_6;
            }
        }
        this.buildPathIndex = tag.getInt("build_path_index");
        this.buildEntryIndex = tag.getInt("build_entry_index");
        this.pathsToClear.clear();
        if (tag.contains("paths_to_clear")) {
            void var3_8;
            ListTag clearList = tag.getList("paths_to_clear", 11);
            boolean bl = false;
            while (var3_8 < clearList.size()) {
                BlockPos pos = VillagePathManager.readPos(clearList.getIntArray((int)var3_8));
                if (pos == null) {
                    LOGGER.warn("Skipping corrupt path-to-clear {} (malformed pos)", (Object)((int)var3_8));
                } else {
                    this.pathsToClear.add(pos);
                }
                ++var3_8;
            }
        }
        this.clearIndex = tag.getInt("clear_index");
        this.pathLevelByPos.clear();
        if (tag.contains("path_levels")) {
            void var3_10;
            ListTag levelList = tag.getList("path_levels", 10);
            boolean bl = false;
            while (var3_10 < levelList.size()) {
                CompoundTag e = levelList.getCompound((int)var3_10);
                BlockPos pos = VillagePathManager.readPos(e.getIntArray("pos"));
                if (pos == null) {
                    LOGGER.warn("Skipping corrupt path level entry {} (malformed pos)", (Object)((int)var3_10));
                } else {
                    this.pathLevelByPos.put(pos, e.getInt("level"));
                }
                ++var3_10;
            }
        }
        this.pathLevelByColumn.clear();
        for (Map.Entry<BlockPos, Integer> entry : this.pathLevelByPos.entrySet()) {
            this.pathLevelByColumn.merge(VillagePathManager.packXZ(entry.getKey().getX(), entry.getKey().getZ()), entry.getValue(), Math::max);
        }
        this.allNewPathPositions.clear();
        for (List list : this.pathsToBuild) {
            for (PathEntry entry : list) {
                this.allNewPathPositions.add(entry.pos());
            }
        }
        this.snapshotSurfaceColumns();
        this.pathRecheckFailNights = tag.getInt("recheck_fail_nights");
    }

    /*
     * WARNING - void declaration
     */
    public String toDumpJson(ServerLevel level, Village village) {
        void var13_39;
        JsonObject root = new JsonObject();
        BlockPos center = village.getCenter();
        JsonObject centerJson = new JsonObject();
        centerJson.addProperty("x", (Number)center.getX());
        centerJson.addProperty("y", (Number)center.getY());
        centerJson.addProperty("z", (Number)center.getZ());
        root.add("center", (JsonElement)centerJson);
        int radius = 80;
        VillageType vt = ModCultures.getVillageType(village.getVillageTypeId());
        if (vt != null) {
            radius = vt.radius();
        }
        root.addProperty("radius", (Number)radius);
        JsonArray buildings = new JsonArray();
        for (BuildingInstance buildingInstance : village.getBuildings()) {
            BuildingPlanSet planSet;
            if (!VillagePathManager.isPathContributor(buildingInstance)) continue;
            JsonObject jsonObject = new JsonObject();
            jsonObject.addProperty("id", buildingInstance.getId() == null ? "null" : buildingInstance.getId().uuid().toString());
            jsonObject.addProperty("planSetId", buildingInstance.getPlanSetId() == null ? "null" : buildingInstance.getPlanSetId().toString());
            BlockPos blockPos = buildingInstance.getOrigin();
            JsonObject jsonObject2 = new JsonObject();
            jsonObject2.addProperty("x", (Number)blockPos.getX());
            jsonObject2.addProperty("y", (Number)blockPos.getY());
            jsonObject2.addProperty("z", (Number)blockPos.getZ());
            jsonObject.add("origin", (JsonElement)jsonObject2);
            int pathLevel = 0;
            boolean noPaths = false;
            BuildingPlanSet buildingPlanSet = planSet = buildingInstance.getPlanSetId() != null ? ModCultures.getBuildingPlanSet(buildingInstance.getPlanSetId()) : null;
            if (planSet != null) {
                BuildingPlanSet.LevelDef levelDef;
                noPaths = planSet.tags().contains("nopaths");
                if (buildingInstance.getVariant() != null && (levelDef = planSet.getLevel(buildingInstance.getVariant(), buildingInstance.getLevel())) != null) {
                    pathLevel = levelDef.pathLevel();
                }
            }
            jsonObject.addProperty("pathLevel", (Number)pathLevel);
            jsonObject.addProperty("noPaths", Boolean.valueOf(noPaths));
            int ox = buildingInstance.getOrigin().getX();
            int oz = buildingInstance.getOrigin().getZ();
            JsonObject fp = new JsonObject();
            fp.addProperty("minX", (Number)(ox + buildingInstance.getCachedMinX()));
            fp.addProperty("maxX", (Number)(ox + buildingInstance.getCachedMaxX()));
            fp.addProperty("minZ", (Number)(oz + buildingInstance.getCachedMinZ()));
            fp.addProperty("maxZ", (Number)(oz + buildingInstance.getCachedMaxZ()));
            jsonObject.add("footprint", (JsonElement)fp);
            buildings.add((JsonElement)jsonObject);
        }
        root.add("buildings", (JsonElement)buildings);
        JsonArray diags = new JsonArray();
        for (Map.Entry<BuildingId, PathDiagnostic> entry : this.lastDiagnostics.entrySet()) {
            PathDiagnostic pathDiagnostic = entry.getValue();
            JsonObject jsonObject = new JsonObject();
            jsonObject.addProperty("buildingId", entry.getKey().uuid().toString());
            jsonObject.addProperty("planSetId", pathDiagnostic.planSetId() == null ? "null" : pathDiagnostic.planSetId().toString());
            jsonObject.add("origin", (JsonElement)VillagePathManager.posJson(pathDiagnostic.origin()));
            jsonObject.addProperty("expectedTier", (Number)pathDiagnostic.expectedTier());
            jsonObject.addProperty("effectiveTier", (Number)pathDiagnostic.effectiveTier());
            jsonObject.add("source", (JsonElement)(pathDiagnostic.source() == null ? JsonNull.INSTANCE : VillagePathManager.posJson(pathDiagnostic.source())));
            jsonObject.addProperty("sourceIsFallback", Boolean.valueOf(pathDiagnostic.sourceIsFallback()));
            jsonObject.add("destination", (JsonElement)(pathDiagnostic.destination() == null ? JsonNull.INSTANCE : VillagePathManager.posJson(pathDiagnostic.destination())));
            jsonObject.addProperty("connected", Boolean.valueOf(pathDiagnostic.connected()));
            jsonObject.addProperty("failure", pathDiagnostic.failure() == null ? "null" : pathDiagnostic.failure().name());
            jsonObject.addProperty("traceLength", (Number)pathDiagnostic.traceLength());
            jsonObject.addProperty("placedBlocks", (Number)pathDiagnostic.placedBlocks());
            jsonObject.addProperty("lateral", Boolean.valueOf(pathDiagnostic.lateral()));
            if (pathDiagnostic.astarDetail() != null) {
                AStarFailureDetail a = pathDiagnostic.astarDetail();
                JsonObject aj = new JsonObject();
                aj.addProperty("nodesExploredDefault", (Number)a.nodesExploredDefault());
                aj.addProperty("nodesExploredRelaxed", (Number)a.nodesExploredRelaxed());
                aj.addProperty("nodesExploredPermissive", (Number)a.nodesExploredPermissive());
                aj.addProperty("rejectedStep", (Number)a.rejectedStep());
                aj.addProperty("rejectedTraversable", (Number)a.rejectedTraversable());
                aj.addProperty("rejectedByFootprint", (Number)a.rejectedByFootprint());
                aj.addProperty("defaultReason", a.defaultReason());
                aj.addProperty("relaxedReason", a.relaxedReason());
                aj.addProperty("permissiveReason", a.permissiveReason());
                jsonObject.add("astarDetail", (JsonElement)aj);
            } else {
                jsonObject.add("astarDetail", (JsonElement)JsonNull.INSTANCE);
            }
            diags.add((JsonElement)jsonObject);
        }
        root.add("diagnostics", (JsonElement)diags);
        JsonArray jsonArray = new JsonArray();
        for (PathDiagnostic pathDiagnostic : this.lateralDiagnostics) {
            JsonObject jsonObject = new JsonObject();
            jsonObject.addProperty("buildingId", pathDiagnostic.building() != null ? pathDiagnostic.building().uuid().toString() : "null");
            jsonObject.addProperty("planSetId", pathDiagnostic.planSetId() == null ? "null" : pathDiagnostic.planSetId().toString());
            jsonObject.add("source", (JsonElement)(pathDiagnostic.source() == null ? JsonNull.INSTANCE : VillagePathManager.posJson(pathDiagnostic.source())));
            jsonObject.add("destination", (JsonElement)(pathDiagnostic.destination() == null ? JsonNull.INSTANCE : VillagePathManager.posJson(pathDiagnostic.destination())));
            jsonObject.addProperty("effectiveTier", (Number)pathDiagnostic.effectiveTier());
            jsonObject.addProperty("failure", pathDiagnostic.failure() == null ? "null" : pathDiagnostic.failure().name());
            jsonObject.addProperty("traceLength", (Number)pathDiagnostic.traceLength());
            jsonObject.addProperty("placedBlocks", (Number)pathDiagnostic.placedBlocks());
            jsonArray.add((JsonElement)jsonObject);
        }
        root.add("lateralDiagnostics", (JsonElement)jsonArray);
        JsonArray jsonArray2 = new JsonArray();
        if (!this.pathsToBuild.isEmpty()) {
            for (List<PathEntry> list : this.pathsToBuild) {
                for (PathEntry e : list) {
                    JsonObject pj = new JsonObject();
                    pj.add("pos", (JsonElement)VillagePathManager.posJson(e.pos()));
                    pj.addProperty("tier", (Number)this.pathLevelByPos.getOrDefault(e.pos(), -1));
                    pj.addProperty("kind", VillagePathManager.pathEntryKind(e.state()));
                    jsonArray2.add((JsonElement)pj);
                }
            }
        } else {
            for (Map.Entry<BlockPos, Integer> entry : this.pathLevelByPos.entrySet()) {
                JsonObject pj = new JsonObject();
                pj.add("pos", (JsonElement)VillagePathManager.posJson(entry.getKey()));
                pj.addProperty("tier", (Number)entry.getValue());
                pj.addProperty("kind", VillagePathManager.pathEntryKind(level.getBlockState(entry.getKey())));
                jsonArray2.add((JsonElement)pj);
            }
        }
        root.add("path_positions", (JsonElement)jsonArray2);
        JsonArray jsonArray3 = new JsonArray();
        for (PathRoute r : this.pendingRoutes) {
            JsonObject rj = new JsonObject();
            rj.add("from", (JsonElement)VillagePathManager.posJson(r.from()));
            rj.add("to", (JsonElement)VillagePathManager.posJson(r.to()));
            rj.addProperty("material", r.material());
            rj.addProperty("width", (Number)r.width());
            rj.addProperty("pathLevel", (Number)r.pathLevel());
            rj.addProperty("sourceId", r.sourceId() == null ? "null" : r.sourceId().uuid().toString());
            rj.addProperty("destinationId", r.destinationId() == null ? "null" : r.destinationId().uuid().toString());
            jsonArray3.add((JsonElement)rj);
        }
        root.add("pending_routes", (JsonElement)jsonArray3);
        boolean bl = false;
        for (int i = this.buildPathIndex; i < this.pathsToBuild.size(); ++i) {
            int start = i == this.buildPathIndex ? this.buildEntryIndex : 0;
            var13_39 += Math.max(0, this.pathsToBuild.get(i).size() - start);
        }
        root.addProperty("paths_to_build_remaining", (Number)((int)var13_39));
        root.addProperty("paths_to_clear_remaining", (Number)Math.max(0, this.pathsToClear.size() - this.clearIndex));
        root.addProperty("pending_routes_count", (Number)this.pendingRoutes.size());
        root.addProperty("lastRecalcTick", (Number)this.lastRecalcTick);
        return new GsonBuilder().setPrettyPrinting().create().toJson((JsonElement)root);
    }

    private static String pathEntryKind(BlockState state) {
        if (VillagePathManager.isSurfaceState(state)) {
            return "surface";
        }
        if (state.isAir()) {
            return "cut";
        }
        return "fill";
    }

    private static boolean isSurfaceState(BlockState state) {
        Block b = state.getBlock();
        return b instanceof MillPathBlock || b instanceof MillPathSlabBlock;
    }

    private void snapshotSurfaceColumns() {
        this.previousSurfaceColumns.clear();
        for (List<PathEntry> path : this.pathsToBuild) {
            for (PathEntry e : path) {
                if (!VillagePathManager.isSurfaceState(e.state())) continue;
                this.previousSurfaceColumns.add(VillagePathManager.packXZ(e.pos().getX(), e.pos().getZ()));
            }
        }
    }

    private PathGridAStar.PathAffinity surfaceAffinity() {
        return (x, z) -> {
            long col = VillagePathManager.packXZ(x, z);
            return this.previousSurfaceColumns.contains(col) || this.currentRecalcSurfaceColumns.contains(col);
        };
    }

    private static JsonObject posJson(BlockPos p) {
        JsonObject o = new JsonObject();
        o.addProperty("x", (Number)p.getX());
        o.addProperty("y", (Number)p.getY());
        o.addProperty("z", (Number)p.getZ());
        return o;
    }

    private record DiagSourceInfo(BuildingInstance instance, boolean fallback, int expectedTier) {
    }

    private record TraceResult(@Nullable List<BlockPos> trace, AStarFailureDetail detail, @Nullable PathFailureReason failure) {
    }

    private record RouteIds(@Nullable BuildingId source, @Nullable BuildingId dest) {
    }

    record Footprint(int minX, int maxX, int minZ, int maxZ) {
    }

    static enum ScanCell {
        SKIP,
        PATH,
        GROUND;

    }

    public static enum PlacementCheck {
        ALLOWED,
        BLOCKED;

    }
}

