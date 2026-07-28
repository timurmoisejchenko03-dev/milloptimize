/*
 * Decompiled with CFR 0.150.
 * 
 * Could not load the following classes:
 *  javax.annotation.Nullable
 *  net.minecraft.core.BlockPos
 *  net.minecraft.resources.ResourceLocation
 *  net.minecraft.server.level.ServerLevel
 */
package org.millenaire.world;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import javax.annotation.Nullable;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import org.millenaire.building.BuildingPlan;
import org.millenaire.building.BuildingPlanSet;
import org.millenaire.culture.ModCultures;
import org.millenaire.culture.VillageType;
import org.millenaire.culture.WallType;
import org.millenaire.world.TerrainPreparer;
import org.millenaire.world.TerrainReachability;
import org.millenaire.world.VillageTerrainMap;

public final class VillageWallGenerator {
    private final ServerLevel level;

    public VillageWallGenerator(ServerLevel level) {
        this.level = level;
    }

    public WallLocationResult computeWallBuildingLocations(VillageType villageType, WallType wallType, int maxWallRadius, @Nullable VillageTerrainMap terrainMap, @Nullable TerrainReachability reachability, BlockPos centre) {
        BuildingPlanSet wallPlanSet = VillageWallGenerator.resolveSet(wallType.wallPlanSet());
        BuildingPlanSet towerPlanSet = VillageWallGenerator.resolveSet(wallType.towerPlanSet());
        BuildingPlanSet gatewayPlanSet = VillageWallGenerator.resolveSet(wallType.gatewayPlanSet());
        BuildingPlanSet cornerPlanSet = VillageWallGenerator.resolveSet(wallType.cornerPlanSet());
        BuildingPlanSet capRightSet = VillageWallGenerator.resolveSet(wallType.capRightPlanSet());
        BuildingPlanSet capLeftSet = VillageWallGenerator.resolveSet(wallType.capLeftPlanSet());
        BuildingPlanSet capBothSet = VillageWallGenerator.resolveSet(wallType.capBothPlanSet());
        if (gatewayPlanSet == null) {
            return new WallLocationResult(List.of());
        }
        if (cornerPlanSet == null && towerPlanSet != null) {
            cornerPlanSet = towerPlanSet;
        }
        if (wallPlanSet != null) {
            if (capRightSet == null) {
                capRightSet = wallPlanSet;
            }
            if (capLeftSet == null) {
                capLeftSet = wallPlanSet;
            }
            if (capBothSet == null) {
                capBothSet = wallPlanSet;
            }
        }
        BuildingPlan wallPlan = VillageWallGenerator.firstStartingPlan(wallPlanSet);
        BuildingPlan towerPlan = VillageWallGenerator.firstStartingPlan(towerPlanSet);
        BuildingPlan gatewayPlan = VillageWallGenerator.firstStartingPlan(gatewayPlanSet);
        BuildingPlan cornerPlan = VillageWallGenerator.firstStartingPlan(cornerPlanSet);
        BuildingPlan capRightPlan = VillageWallGenerator.firstStartingPlan(capRightSet);
        BuildingPlan capLeftPlan = VillageWallGenerator.firstStartingPlan(capLeftSet);
        BuildingPlan capBothPlan = VillageWallGenerator.firstStartingPlan(capBothSet);
        if (gatewayPlan == null) {
            return new WallLocationResult(List.of());
        }
        int wallLength = wallPlan != null ? wallPlan.width() : 1;
        int towerLength = towerPlan != null ? towerPlan.width() : 0;
        int cornerLength = cornerPlan != null ? cornerPlan.width() : 0;
        int wallRadius = VillageWallGenerator.computeWallRadius(gatewayPlan.width(), maxWallRadius, villageType.radius(), wallLength, towerLength, cornerLength, wallType.wallsBetweenTowers());
        int wallRadiusLimit = maxWallRadius > 0 ? maxWallRadius : villageType.radius() - wallLength - cornerLength;
        List<WallSide> sides = List.of(new WallSide(1, 0, 1, 0), new WallSide(0, 1, -1, 3), new WallSide(-1, 0, -1, 2), new WallSide(0, -1, 1, 1));
        ArrayList<WallSegment> wallSegments = new ArrayList<WallSegment>();
        for (WallSide side : sides) {
            BlockPos gatewayCentre = centre.offset(wallRadius * side.xMultiplier, 0, wallRadius * side.zMultiplier);
            int y = this.computeAverageYLevel(gatewayPlan, side.buildingOrientation, gatewayCentre);
            BlockPos gatewayPos = new BlockPos(gatewayCentre.getX(), y, gatewayCentre.getZ());
            ArrayList<WallSegment> segmentsForward = new ArrayList<WallSegment>();
            ArrayList<WallSegment> segmentsBackward = new ArrayList<WallSegment>();
            int pos = gatewayPlan.width() / 2;
            int i = 0;
            while (pos < wallRadiusLimit) {
                int segmentLength;
                boolean spawn;
                BuildingPlanSet currentPlanSet;
                if (i % (wallType.wallsBetweenTowers() + 1) == wallType.wallsBetweenTowers()) {
                    currentPlanSet = towerPlanSet;
                    spawn = wallType.towerSpawn();
                    segmentLength = towerLength;
                } else {
                    currentPlanSet = wallPlanSet;
                    spawn = wallType.wallSpawn();
                    segmentLength = wallLength;
                }
                if (currentPlanSet != null) {
                    this.buildNextElements(terrainMap, reachability, wallType, segmentsBackward, segmentsForward, wallRadius, side, pos, currentPlanSet, spawn, true, centre);
                }
                pos += segmentLength;
                ++i;
            }
            if (wallPlanSet != null) {
                this.buildNextElements(terrainMap, reachability, wallType, segmentsBackward, segmentsForward, wallRadius, side, pos, wallPlanSet, wallType.wallSpawn(), true, centre);
            }
            pos += wallLength;
            if (cornerPlanSet != null) {
                this.buildNextElements(terrainMap, reachability, wallType, segmentsBackward, segmentsForward, wallRadius, side, pos, cornerPlanSet, wallType.cornerSpawn(), false, centre);
            }
            Collections.reverse(segmentsBackward);
            wallSegments.addAll(segmentsBackward);
            WallSegment gatewaySegment = this.computeWallElementLocation(terrainMap, reachability, wallType, gatewayPlanSet, gatewayPos, side.buildingOrientation, wallType.gatewaySpawn(), centre);
            if (gatewaySegment != null) {
                wallSegments.add(gatewaySegment);
            }
            wallSegments.addAll(segmentsForward);
        }
        this.computeWallConnections(wallSegments);
        this.smoothWalls(wallSegments, wallType);
        if (wallPlan != null) {
            this.capWalls(wallSegments, wallPlan, capRightPlan, capLeftPlan, capBothPlan);
            if (wallType.slope1LeftPlanSet() != null && wallType.slope1RightPlanSet() != null) {
                this.addSlopes(wallSegments, wallType);
            }
        }
        ArrayList<PlannedWallSegment> planned = new ArrayList<PlannedWallSegment>(wallSegments.size());
        for (WallSegment s : wallSegments) {
            planned.add(new PlannedWallSegment(s.planId, s.pos, s.orientation, s.level));
        }
        return new WallLocationResult(planned);
    }

    private void buildNextElements(@Nullable VillageTerrainMap terrainMap, @Nullable TerrainReachability reachability, WallType wallType, List<WallSegment> locationsBackward, List<WallSegment> locationsForward, int wallRadius, WallSide side, int pos, BuildingPlanSet planSet, boolean spawn, boolean buildNegative, BlockPos centre) {
        BuildingPlan plan = VillageWallGenerator.firstStartingPlan(planSet);
        if (plan == null) {
            return;
        }
        int segmentLength = plan.width();
        if (side.xMultiplier != 0) {
            int deltaZ = (pos + segmentLength / 2) * side.direction;
            BlockPos p = centre.offset(wallRadius * side.xMultiplier, 0, deltaZ);
            WallSegment seg = this.computeWallElementLocation(terrainMap, reachability, wallType, planSet, p, side.buildingOrientation, spawn, centre);
            if (seg != null) {
                locationsForward.add(seg);
            }
            if (buildNegative) {
                if (segmentLength % 2 == 1) {
                    deltaZ += side.direction;
                }
                if ((seg = this.computeWallElementLocation(terrainMap, reachability, wallType, planSet, p = centre.offset(wallRadius * side.xMultiplier, 0, -deltaZ), side.buildingOrientation, spawn, centre)) != null) {
                    locationsBackward.add(seg);
                }
            }
        } else {
            int deltaX = (pos + segmentLength / 2) * side.direction;
            BlockPos p = centre.offset(deltaX, 0, wallRadius * side.zMultiplier);
            WallSegment seg = this.computeWallElementLocation(terrainMap, reachability, wallType, planSet, p, side.buildingOrientation, spawn, centre);
            if (seg != null) {
                locationsForward.add(seg);
            }
            if (buildNegative) {
                if (segmentLength % 2 == 1) {
                    deltaX += side.direction;
                }
                if ((seg = this.computeWallElementLocation(terrainMap, reachability, wallType, planSet, p = centre.offset(-deltaX, 0, wallRadius * side.zMultiplier), side.buildingOrientation, spawn, centre)) != null) {
                    locationsBackward.add(seg);
                }
            }
        }
    }

    @Nullable
    private WallSegment computeWallElementLocation(@Nullable VillageTerrainMap terrainMap, @Nullable TerrainReachability reachability, WallType wallType, BuildingPlanSet planSet, BlockPos pos, int orientation, boolean spawn, BlockPos centre) {
        int y;
        BuildingPlan plan = VillageWallGenerator.firstStartingPlan(planSet);
        if (plan == null) {
            return null;
        }
        int finalOrientation = (orientation + plan.buildingOrientation()) % 4;
        int orientatedLength = plan.width();
        int orientatedWidth = plan.depth();
        if (finalOrientation % 2 == 1) {
            orientatedLength = plan.depth();
            orientatedWidth = plan.width();
        }
        if ((y = this.computeAverageYLevel(plan, orientation, pos)) > centre.getY() + wallType.maxYDelta()) {
            return null;
        }
        BlockPos buildingPos = new BlockPos(pos.getX(), y, pos.getZ());
        if (terrainMap != null) {
            BlockPos[] testPoints = new BlockPos[]{buildingPos, buildingPos.offset(orientatedLength / 2, 0, orientatedWidth / 2), buildingPos.offset(orientatedLength / 2, 0, -orientatedWidth / 2), buildingPos.offset(-orientatedLength / 2, 0, orientatedWidth / 2), buildingPos.offset(-orientatedLength / 2, 0, -orientatedWidth / 2)};
            boolean reachable = false;
            for (BlockPos tp : testPoints) {
                if (!VillageWallGenerator.isUsableWallAnchor(terrainMap, reachability, tp)) continue;
                reachable = true;
                break;
            }
            if (!reachable) {
                return null;
            }
        }
        int segmentLevel = planSet.isBorderBuilding() && spawn ? 0 : -1;
        return new WallSegment(plan.id(), buildingPos, finalOrientation, segmentLevel);
    }

    static int computeWallRadius(int gatewayWidth, int maxWallRadius, int villageRadius, int wallLength, int towerLength, int cornerLength, int wallsBetweenTowers) {
        int wallRadius = gatewayWidth / 2;
        int wallRadiusLimit = maxWallRadius > 0 ? maxWallRadius : villageRadius - wallLength - cornerLength;
        int buildNb = 0;
        while (wallRadius < wallRadiusLimit) {
            wallRadius = buildNb % (wallsBetweenTowers + 1) == wallsBetweenTowers ? (wallRadius += towerLength) : (wallRadius += wallLength);
            ++buildNb;
        }
        wallRadius += wallLength;
        return wallRadius += cornerLength / 2;
    }

    static boolean isUsableWallAnchor(VillageTerrainMap terrainMap, @Nullable TerrainReachability reachability, BlockPos pos) {
        int lz;
        int lx = terrainMap.toLocalX(pos.getX());
        if (!terrainMap.inBounds(lx, lz = terrainMap.toLocalZ(pos.getZ()))) {
            return false;
        }
        if (terrainMap.isOccupied(lx, lz)) {
            return false;
        }
        if (reachability != null) {
            return reachability.isReachable(pos.getX(), pos.getZ());
        }
        return terrainMap.canBuildAt(lx, lz) && !terrainMap.isWaterAt(lx, lz) && !terrainMap.isDangerAt(lx, lz) && terrainMap.getSpaceAbove(lx, lz) > 1;
    }

    private int computeAverageYLevel(BuildingPlan plan, int orientation, BlockPos pos) {
        int orient = (orientation + plan.buildingOrientation()) % 4;
        int orientatedLength = plan.width();
        int orientatedWidth = plan.depth();
        if (orient % 2 == 1) {
            orientatedLength = plan.depth();
            orientatedWidth = plan.width();
        }
        BlockPos[] corners = new BlockPos[]{pos.offset(orientatedLength / 2, 0, orientatedWidth / 2), pos.offset(orientatedLength / 2, 0, -orientatedWidth / 2), pos.offset(-orientatedLength / 2, 0, orientatedWidth / 2), pos.offset(-orientatedLength / 2, 0, -orientatedWidth / 2)};
        int sum = 2;
        for (BlockPos c : corners) {
            sum += TerrainPreparer.getSurfaceOrWaterHeight(this.level, c.getX(), c.getZ());
        }
        return sum / corners.length;
    }

    private void computeWallConnections(List<WallSegment> segments) {
        if (segments.isEmpty()) {
            return;
        }
        for (int i = 0; i < segments.size(); ++i) {
            double threshold;
            double dz;
            double dx;
            double horizDist;
            WallSegment prev = i == 0 ? segments.get(segments.size() - 1) : segments.get(i - 1);
            WallSegment cur = segments.get(i);
            BuildingPlan prevPlan = ModCultures.getBuildingPlan(prev.planId);
            BuildingPlan curPlan = ModCultures.getBuildingPlan(cur.planId);
            if (prevPlan == null || curPlan == null || !((horizDist = Math.sqrt((dx = (double)(prev.pos.getX() - cur.pos.getX())) * dx + (dz = (double)(prev.pos.getZ() - cur.pos.getZ())) * dz)) < (threshold = (double)(prevPlan.width() + curPlan.width()) / 2.0 + 4.0))) continue;
            prev.nextSegment = cur;
            cur.previousSegment = prev;
        }
    }

    private void smoothWalls(List<WallSegment> segments, WallType wallType) {
        int i;
        if (segments.isEmpty()) {
            return;
        }
        float[] refY = new float[segments.size()];
        for (i = 0; i < segments.size(); ++i) {
            refY[i] = segments.get((int)i).pos.getY();
        }
        for (int run = 0; run < wallType.nbSmoothRuns(); ++run) {
            float[] adjusted = new float[segments.size()];
            for (int i2 = 0; i2 < segments.size(); ++i2) {
                int prevId = i2 == 0 ? segments.size() - 1 : i2 - 1;
                int nextId = i2 == segments.size() - 1 ? 0 : i2 + 1;
                WallSegment s = segments.get(i2);
                int nbPoints = 1;
                float avg = refY[i2];
                if (s.previousSegment != null) {
                    ++nbPoints;
                    avg += refY[prevId];
                }
                if (s.nextSegment != null) {
                    ++nbPoints;
                    avg += refY[nextId];
                }
                adjusted[i2] = avg / (float)nbPoints;
            }
            System.arraycopy(adjusted, 0, refY, 0, refY.length);
        }
        for (i = 0; i < segments.size(); ++i) {
            int finalY = Math.round(refY[i]);
            if (segments.get((int)i).pos.getY() == finalY) continue;
            segments.get(i).setYLevel(finalY);
        }
    }

    private void capWalls(List<WallSegment> segments, BuildingPlan wallPlan, @Nullable BuildingPlan capRight, @Nullable BuildingPlan capLeft, @Nullable BuildingPlan capBoth) {
        for (WallSegment s : segments) {
            BuildingPlan sPlan = ModCultures.getBuildingPlan(s.planId);
            if (sPlan != wallPlan) continue;
            boolean noPrev = s.previousSegment == null;
            boolean noNext = s.nextSegment == null;
            BuildingPlan target = null;
            if (noPrev && noNext) {
                target = capBoth;
            } else if (noPrev) {
                target = capRight;
            } else if (noNext) {
                target = capLeft;
            }
            if (target == null || target == wallPlan) continue;
            s.planId = target.id();
        }
    }

    private void addSlopes(List<WallSegment> segments, WallType wallType) {
        BuildingPlan wallPlan = VillageWallGenerator.firstStartingPlan(VillageWallGenerator.resolveSet(wallType.wallPlanSet()));
        if (wallPlan == null) {
            return;
        }
        BuildingPlan[] leftSlopes = new BuildingPlan[]{VillageWallGenerator.firstStartingPlan(VillageWallGenerator.resolveSet(wallType.slope1LeftPlanSet())), VillageWallGenerator.firstStartingPlan(VillageWallGenerator.resolveSet(wallType.slope2LeftPlanSet())), VillageWallGenerator.firstStartingPlan(VillageWallGenerator.resolveSet(wallType.slope3LeftPlanSet()))};
        BuildingPlan[] rightSlopes = new BuildingPlan[]{VillageWallGenerator.firstStartingPlan(VillageWallGenerator.resolveSet(wallType.slope1RightPlanSet())), VillageWallGenerator.firstStartingPlan(VillageWallGenerator.resolveSet(wallType.slope2RightPlanSet())), VillageWallGenerator.firstStartingPlan(VillageWallGenerator.resolveSet(wallType.slope3RightPlanSet()))};
        for (WallSegment s : segments) {
            BuildingPlan sPlan = ModCultures.getBuildingPlan(s.planId);
            s.sloppable = sPlan != null && sPlan.id().equals((Object)wallPlan.id());
        }
        for (WallSegment s : segments) {
            int y = s.pos.getY();
            if (s.previousSegment != null && s.nextSegment != null) {
                if (s.previousSegment.pos.getY() < y && s.nextSegment.pos.getY() < y) {
                    s.setYLevel(Math.max(s.previousSegment.pos.getY(), s.nextSegment.pos.getY()));
                    y = s.pos.getY();
                } else if (s.previousSegment.pos.getY() > y && s.nextSegment.pos.getY() > y) {
                    s.setYLevel(Math.min(s.previousSegment.pos.getY(), s.nextSegment.pos.getY()));
                    y = s.pos.getY();
                }
            }
            if (!s.sloppable) continue;
            if (s.previousSegment != null && !s.previousSegment.sloppable && s.previousSegment.pos.getY() < y) {
                s.setYLevel(s.previousSegment.pos.getY());
                y = s.pos.getY();
            } else if (s.nextSegment != null && !s.nextSegment.sloppable && s.nextSegment.pos.getY() < y) {
                s.setYLevel(s.nextSegment.pos.getY());
                y = s.pos.getY();
            }
            BuildingPlan slopePlan = null;
            if (s.nextSegment != null && s.nextSegment.yTowardsPrevious > y) {
                deltaY = s.nextSegment.yTowardsPrevious - y;
                for (d = leftSlopes.length; d > 0; --d) {
                    if (deltaY < d || leftSlopes[d - 1] == null) continue;
                    slopePlan = leftSlopes[d - 1];
                    s.yTowardsNext += d;
                    break;
                }
            } else if (s.previousSegment != null && s.previousSegment.yTowardsNext > y) {
                deltaY = s.previousSegment.yTowardsNext - y;
                for (d = rightSlopes.length; d > 0; --d) {
                    if (deltaY < d || rightSlopes[d - 1] == null) continue;
                    slopePlan = rightSlopes[d - 1];
                    s.yTowardsPrevious += d;
                    break;
                }
            }
            if (slopePlan == null) continue;
            s.planId = slopePlan.id();
        }
    }

    @Nullable
    private static BuildingPlanSet resolveSet(@Nullable ResourceLocation id) {
        return id == null ? null : ModCultures.getBuildingPlanSet(id);
    }

    @Nullable
    private static BuildingPlan firstStartingPlan(@Nullable BuildingPlanSet set) {
        if (set == null) {
            return null;
        }
        String variant = set.variants().keySet().stream().findFirst().orElse(null);
        if (variant == null) {
            return null;
        }
        BuildingPlanSet.LevelDef level0 = set.getLevel(variant, 0);
        if (level0 == null) {
            return null;
        }
        return ModCultures.getBuildingPlan(level0.planId());
    }

    public record WallLocationResult(List<PlannedWallSegment> segments) {
    }

    private record WallSide(int xMultiplier, int zMultiplier, int direction, int buildingOrientation) {
    }

    private static final class WallSegment {
        ResourceLocation planId;
        BlockPos pos;
        final int orientation;
        final int level;
        @Nullable
        WallSegment previousSegment = null;
        @Nullable
        WallSegment nextSegment = null;
        boolean sloppable = false;
        int yTowardsPrevious;
        int yTowardsNext;

        WallSegment(ResourceLocation planId, BlockPos pos, int orientation, int level) {
            this.planId = planId;
            this.pos = pos;
            this.orientation = orientation;
            this.level = level;
            this.yTowardsPrevious = pos.getY();
            this.yTowardsNext = pos.getY();
        }

        void setYLevel(int newY) {
            int delta = newY - this.pos.getY();
            this.pos = new BlockPos(this.pos.getX(), newY, this.pos.getZ());
            this.yTowardsPrevious += delta;
            this.yTowardsNext += delta;
        }
    }

    public record PlannedWallSegment(ResourceLocation planId, BlockPos pos, int orientation, int level) {
    }
}

