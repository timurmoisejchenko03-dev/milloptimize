/*
 * Decompiled with CFR 0.150.
 * 
 * Could not load the following classes:
 *  com.mojang.logging.LogUtils
 *  javax.annotation.Nullable
 *  net.minecraft.core.BlockPos
 *  net.minecraft.world.level.block.Rotation
 *  org.slf4j.Logger
 */
package org.millenaire.world;

import com.mojang.logging.LogUtils;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ThreadLocalRandom;
import javax.annotation.Nullable;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.Rotation;
import org.millenaire.building.BuildingInstance;
import org.millenaire.building.BuildingPlan;
import org.millenaire.building.ClearMargins;
import org.millenaire.culture.ModCultures;
import org.millenaire.world.PlacedLocation;
import org.millenaire.world.PlacementConstraints;
import org.millenaire.world.TerrainReachability;
import org.millenaire.world.VillageTerrainMap;
import org.slf4j.Logger;

public final class BuildingLocationFinder {
    private static final Logger LOGGER = LogUtils.getLogger();

    private BuildingLocationFinder() {
    }

    @Nullable
    public static PlacedLocation findLocation(VillageTerrainMap map, BuildingPlan plan, BlockPos villageCenter, PlacementConstraints constraints, ClearMargins clearMargins, List<BuildingInstance> existingBuildings) {
        return BuildingLocationFinder.findLocation(map, plan, villageCenter, constraints, clearMargins, existingBuildings, null);
    }

    @Nullable
    public static PlacedLocation findLocation(VillageTerrainMap map, BuildingPlan plan, BlockPos villageCenter, PlacementConstraints constraints, ClearMargins clearMargins, List<BuildingInstance> existingBuildings, @Nullable TerrainReachability reachability) {
        int centerX = villageCenter.getX();
        int centerZ = villageCenter.getZ();
        int minDist = constraints.minDistance();
        int maxDist = constraints.maxDistance();
        int sideOffset = ThreadLocalRandom.current().nextInt(4);
        for (int r = minDist; r <= maxDist; ++r) {
            for (int sideIdx = 0; sideIdx < 4; ++sideIdx) {
                int side = (sideIdx + sideOffset) % 4;
                PlacedLocation result = BuildingLocationFinder.scanSide(side, r, centerX, centerZ, map, plan, villageCenter, constraints, clearMargins, existingBuildings, reachability);
                if (result == null) continue;
                return result;
            }
        }
        return null;
    }

    @Nullable
    private static PlacedLocation scanSide(int side, int r, int centerX, int centerZ, VillageTerrainMap map, BuildingPlan plan, BlockPos villageCenter, PlacementConstraints constraints, ClearMargins clearMargins, List<BuildingInstance> existingBuildings, @Nullable TerrainReachability reachability) {
        switch (side) {
            case 0: {
                for (int x = -r; x <= r; ++x) {
                    PlacedLocation result = BuildingLocationFinder.tryPosition(centerX + x, centerZ - r, map, plan, villageCenter, constraints, clearMargins, existingBuildings, reachability);
                    if (result == null) continue;
                    return result;
                }
                break;
            }
            case 1: {
                for (int z = -r + 1; z <= r; ++z) {
                    PlacedLocation result = BuildingLocationFinder.tryPosition(centerX + r, centerZ + z, map, plan, villageCenter, constraints, clearMargins, existingBuildings, reachability);
                    if (result == null) continue;
                    return result;
                }
                break;
            }
            case 2: {
                for (int x = r - 1; x >= -r; --x) {
                    PlacedLocation result = BuildingLocationFinder.tryPosition(centerX + x, centerZ + r, map, plan, villageCenter, constraints, clearMargins, existingBuildings, reachability);
                    if (result == null) continue;
                    return result;
                }
                break;
            }
            case 3: {
                for (int z = r - 1; z >= -r + 1; --z) {
                    PlacedLocation result = BuildingLocationFinder.tryPosition(centerX - r, centerZ + z, map, plan, villageCenter, constraints, clearMargins, existingBuildings, reachability);
                    if (result == null) continue;
                    return result;
                }
                break;
            }
        }
        return null;
    }

    @Nullable
    private static PlacedLocation tryPosition(int worldX, int worldZ, VillageTerrainMap map, BuildingPlan plan, BlockPos villageCenter, PlacementConstraints constraints, ClearMargins clearMargins, List<BuildingInstance> existingBuildings, @Nullable TerrainReachability reachability) {
        int pivotX;
        int halfFootprint = (plan.width() + plan.depth()) / 4;
        int candidateCenterX = worldX + halfFootprint;
        int candidateCenterZ = worldZ + halfFootprint;
        int computedDir = BuildingLocationFinder.directionToVillageCenter(candidateCenterX, candidateCenterZ, villageCenter);
        int targetDirection = constraints.fixedOrientation() != null ? constraints.fixedOrientation() : computedDir;
        int rotationOrdinal = Math.floorMod(targetDirection - plan.buildingOrientation(), 4);
        Rotation rotation = Rotation.values()[rotationOrdinal];
        if (!BuildingLocationFinder.checkTagConstraints(pivotX, switch (rotation) {
            case Rotation.CLOCKWISE_90 -> {
                pivotX = worldX + plan.depth() - 1;
                yield worldZ;
            }
            case Rotation.CLOCKWISE_180 -> {
                pivotX = worldX + plan.width() - 1;
                yield worldZ + plan.depth() - 1;
            }
            case Rotation.COUNTERCLOCKWISE_90 -> {
                pivotX = worldX;
                yield worldZ + plan.width() - 1;
            }
            default -> {
                pivotX = worldX;
                yield worldZ;
            }
        }, constraints, existingBuildings)) {
            return null;
        }
        int result = map.testFootprint(pivotX, pivotZ, plan.width(), plan.depth(), clearMargins, rotation);
        if (result < 0) {
            return null;
        }
        if (reachability != null && !reachability.isFootprintReachable(pivotX, pivotZ, plan.width(), plan.depth(), rotation)) {
            return null;
        }
        BlockPos pos = new BlockPos(pivotX, villageCenter.getY(), pivotZ);
        if (LOGGER.isDebugEnabled()) {
            LOGGER.debug("[Orient] plan={} bo={} fixed={} nwCorner=({},{}) wh={}x{} center=({},{}) thCenter=({},{}) dx={} dz={} target={} rot={} pivot=({},{})", new Object[]{plan.id(), plan.buildingOrientation(), constraints.fixedOrientation(), worldX, worldZ, plan.width(), plan.depth(), candidateCenterX, candidateCenterZ, villageCenter.getX(), villageCenter.getZ(), candidateCenterX - villageCenter.getX(), candidateCenterZ - villageCenter.getZ(), targetDirection, rotation, pivotX, pivotZ});
        }
        return new PlacedLocation(pos, rotation);
    }

    public static AnchorEvaluation evaluateAnchor(int worldX, int worldZ, VillageTerrainMap map, BuildingPlan plan, BlockPos villageCenter, PlacementConstraints constraints, ClearMargins clearMargins, List<BuildingInstance> existingBuildings, @Nullable TerrainReachability reachability) {
        int pivotX;
        int halfFootprint = (plan.width() + plan.depth()) / 4;
        int candidateCenterX = worldX + halfFootprint;
        int candidateCenterZ = worldZ + halfFootprint;
        int computedDir = BuildingLocationFinder.directionToVillageCenter(candidateCenterX, candidateCenterZ, villageCenter);
        int targetDirection = constraints.fixedOrientation() != null ? constraints.fixedOrientation() : computedDir;
        int rotationOrdinal = Math.floorMod(targetDirection - plan.buildingOrientation(), 4);
        Rotation rotation = Rotation.values()[rotationOrdinal];
        if (!map.inBounds(map.toLocalX(pivotX), map.toLocalZ(switch (rotation) {
            case Rotation.CLOCKWISE_90 -> {
                pivotX = worldX + plan.depth() - 1;
                yield worldZ;
            }
            case Rotation.CLOCKWISE_180 -> {
                pivotX = worldX + plan.width() - 1;
                yield worldZ + plan.depth() - 1;
            }
            case Rotation.COUNTERCLOCKWISE_90 -> {
                pivotX = worldX;
                yield worldZ + plan.width() - 1;
            }
            default -> {
                pivotX = worldX;
                yield worldZ;
            }
        }))) {
            return new AnchorEvaluation(null, FailureReason.OUTSIDE_RADIUS, new BlockPos(worldX, villageCenter.getY(), worldZ));
        }
        if (!BuildingLocationFinder.checkTagConstraints(pivotX, pivotZ, constraints, existingBuildings)) {
            return new AnchorEvaluation(null, FailureReason.GENERIC, new BlockPos(pivotX, villageCenter.getY(), pivotZ));
        }
        AnchorEvaluation footprintFailure = BuildingLocationFinder.classifyFootprint(pivotX, pivotZ, plan, clearMargins, rotation, map, villageCenter);
        if (footprintFailure != null) {
            return footprintFailure;
        }
        if (reachability != null && !reachability.isFootprintReachable(pivotX, pivotZ, plan.width(), plan.depth(), rotation)) {
            return new AnchorEvaluation(null, FailureReason.NOT_REACHABLE, new BlockPos(pivotX, villageCenter.getY(), pivotZ));
        }
        BlockPos pos = new BlockPos(pivotX, villageCenter.getY(), pivotZ);
        return new AnchorEvaluation(new PlacedLocation(pos, rotation), null, null);
    }

    @Nullable
    private static AnchorEvaluation classifyFootprint(int pivotX, int pivotZ, BuildingPlan plan, ClearMargins clearMargins, Rotation rotation, VillageTerrainMap map, BlockPos villageCenter) {
        boolean hugeBuilding;
        VillageTerrainMap.FootprintRect rect = VillageTerrainMap.computeFootprintRect(pivotX, pivotZ, plan.width(), plan.depth(), clearMargins, rotation);
        int surface = rect.width() * rect.depth();
        boolean bl = hugeBuilding = surface > 2000;
        int allowedErrors = surface > 2000 ? surface / 10 : (surface > 200 ? surface / 20 : 10);
        int nbError = 0;
        for (int dx = 0; dx < rect.width(); ++dx) {
            for (int dz = 0; dz < rect.depth(); ++dz) {
                int wx = rect.startX() + dx;
                int wz = rect.startZ() + dz;
                int lx = map.toLocalX(wx);
                int lz = map.toLocalZ(wz);
                BlockPos errorPos = new BlockPos(wx, villageCenter.getY(), wz);
                if (!map.inBounds(lx, lz)) {
                    return new AnchorEvaluation(null, FailureReason.OUTSIDE_RADIUS, errorPos);
                }
                if (map.isOccupied(lx, lz)) {
                    return new AnchorEvaluation(null, FailureReason.LOCATION_CLASH, errorPos);
                }
                if (map.isBuildingForbiddenAt(lx, lz)) {
                    if (!hugeBuilding || nbError > allowedErrors) {
                        return new AnchorEvaluation(null, FailureReason.CONSTRUCTION_FORBIDDEN, errorPos);
                    }
                    ++nbError;
                    continue;
                }
                if (map.isDangerAt(lx, lz)) {
                    if (nbError > allowedErrors) {
                        return new AnchorEvaluation(null, FailureReason.DANGER, errorPos);
                    }
                    ++nbError;
                    continue;
                }
                if (map.canBuildAt(lx, lz)) continue;
                if (nbError > allowedErrors) {
                    return new AnchorEvaluation(null, FailureReason.WRONG_ALTITUDE, errorPos);
                }
                ++nbError;
            }
        }
        return null;
    }

    static int directionToVillageCenter(int buildingX, int buildingZ, BlockPos villageCenter) {
        int relz;
        int relx = villageCenter.getX() - buildingX;
        if (relx * relx > (relz = villageCenter.getZ() - buildingZ) * relz) {
            return relx > 0 ? 1 : 3;
        }
        return relz > 0 ? 2 : 0;
    }

    private static boolean checkTagConstraints(int worldX, int worldZ, PlacementConstraints constraints, List<BuildingInstance> existingBuildings) {
        String tag;
        for (Map.Entry<String, Integer> entry : constraints.farFromTags().entrySet()) {
            tag = entry.getKey();
            int distMin = entry.getValue();
            for (BuildingInstance building : existingBuildings) {
                double dist;
                BuildingPlan existingPlan = ModCultures.getBuildingPlan(building.getPlanId());
                if (existingPlan == null || !existingPlan.hasTag(tag) || !((dist = BuildingLocationFinder.horizontalDistance(worldX, worldZ, building.getOrigin())) < (double)distMin)) continue;
                return false;
            }
        }
        for (Map.Entry<String, Integer> entry : constraints.closeToTags().entrySet()) {
            tag = entry.getKey();
            int distMax = entry.getValue();
            boolean found = false;
            for (BuildingInstance building : existingBuildings) {
                double dist;
                BuildingPlan existingPlan = ModCultures.getBuildingPlan(building.getPlanId());
                if (existingPlan == null || !existingPlan.hasTag(tag) || !((dist = BuildingLocationFinder.horizontalDistance(worldX, worldZ, building.getOrigin())) <= (double)distMax)) continue;
                found = true;
                break;
            }
            if (found) continue;
            return false;
        }
        return true;
    }

    private static double horizontalDistance(int x, int z, BlockPos pos) {
        int dx = x - pos.getX();
        int dz = z - pos.getZ();
        return Math.sqrt(dx * dx + dz * dz);
    }

    public record AnchorEvaluation(@Nullable PlacedLocation location, @Nullable FailureReason reason, @Nullable BlockPos errorPos) {
        public boolean isSuccess() {
            return this.location != null;
        }
    }

    public static final class FailureReason
    extends Enum<FailureReason> {
        public static final /* enum */ FailureReason OUTSIDE_RADIUS = new FailureReason();
        public static final /* enum */ FailureReason LOCATION_CLASH = new FailureReason();
        public static final /* enum */ FailureReason CONSTRUCTION_FORBIDDEN = new FailureReason();
        public static final /* enum */ FailureReason WRONG_ALTITUDE = new FailureReason();
        public static final /* enum */ FailureReason DANGER = new FailureReason();
        public static final /* enum */ FailureReason NOT_REACHABLE = new FailureReason();
        public static final /* enum */ FailureReason GENERIC = new FailureReason();
        private static final /* synthetic */ FailureReason[] $VALUES;

        public static FailureReason[] values() {
            return (FailureReason[])$VALUES.clone();
        }

        public static FailureReason valueOf(String name) {
            return Enum.valueOf(FailureReason.class, name);
        }

        private static /* synthetic */ FailureReason[] $values() {
            return new FailureReason[]{OUTSIDE_RADIUS, LOCATION_CLASH, CONSTRUCTION_FORBIDDEN, WRONG_ALTITUDE, DANGER, NOT_REACHABLE, GENERIC};
        }

        static {
            $VALUES = FailureReason.$values();
        }
    }
}

