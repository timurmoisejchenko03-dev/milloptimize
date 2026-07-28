/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  javax.annotation.Nullable
 *  net.minecraft.core.BlockPos
 *  net.minecraft.core.BlockPos$MutableBlockPos
 *  net.minecraft.core.Vec3i
 *  net.minecraft.resources.ResourceLocation
 *  net.minecraft.server.level.ServerLevel
 *  net.minecraft.world.level.block.Blocks
 *  net.minecraft.world.level.block.Rotation
 */
package org.millenaire.catalog;

import java.util.ArrayDeque;
import java.util.LinkedHashSet;
import java.util.Locale;
import java.util.Set;
import javax.annotation.Nullable;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Vec3i;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.Rotation;
import org.millenaire.building.BuildingPlan;
import org.millenaire.building.BuildingPlanSet;
import org.millenaire.building.ClearMargins;
import org.millenaire.building.HearthLightingUtil;
import org.millenaire.culture.ModCultures;
import org.millenaire.world.BuildingPlacer;
import org.millenaire.world.TerrainPreparer;

public final class CatalogBuildingStager {
    public static final int NO_FORCED_BASE_Y = Integer.MIN_VALUE;
    public static final int VOID_BASE_Y = 128;
    private static final int PAD_DEPTH = 4;
    private static final int CATALOG_CLEAR_MARGIN = 1;
    private static final int CATALOG_EARTH_LAYER = 3;
    private static final int PAD_SCAN_BORDER = 8;

    private CatalogBuildingStager() {
    }

    @Nullable
    public static PlacedBuilding placeInVoid(ServerLevel level, BuildingPlanSet planSet, String variant, int targetLevel, BlockPos cell, int clearHeight) {
        BuildingPlan level0Plan;
        BuildingPlanSet.LevelDef level0Def = planSet.getLevel(variant, 0);
        BuildingPlan buildingPlan = level0Plan = level0Def == null ? null : ModCultures.getBuildingPlan(level0Def.planId());
        if (level0Plan == null) {
            return null;
        }
        BlockPos pos = new BlockPos(cell.getX(), 128, cell.getZ());
        CatalogBuildingStager.fillDirtPad(level, pos, level0Plan.width(), level0Plan.depth());
        PlacedBuilding placed = CatalogBuildingStager.place(level, planSet, variant, targetLevel, pos, clearHeight, 128);
        if (placed == null) {
            return null;
        }
        int groundBottomY = CatalogBuildingStager.lowestSolidY(level, pos, level0Plan.width(), level0Plan.depth());
        return new PlacedBuilding(placed.nominalOrigin(), placed.origin(), placed.width(), placed.height(), placed.depth(), placed.aboveFloorBlocks(), placed.baseY(), groundBottomY, placed.doorOrientation());
    }

    private static void fillDirtPad(ServerLevel level, BlockPos origin, int width, int depth) {
        BlockPos.MutableBlockPos cursor = new BlockPos.MutableBlockPos();
        for (int dx = 0; dx < width; ++dx) {
            for (int dz = 0; dz < depth; ++dz) {
                for (int y = 125; y <= 128; ++y) {
                    level.setBlock((BlockPos)cursor.set(origin.getX() + dx, y, origin.getZ() + dz), Blocks.DIRT.defaultBlockState(), 2);
                }
            }
        }
    }

    private static int lowestSolidY(ServerLevel level, BlockPos origin, int width, int depth) {
        int minY = 128;
        BlockPos.MutableBlockPos cursor = new BlockPos.MutableBlockPos();
        for (int dx = -8; dx < width + 8; ++dx) {
            block1: for (int dz = -8; dz < depth + 8; ++dz) {
                for (int y = 112; y <= 128; ++y) {
                    if (level.getBlockState((BlockPos)cursor.set(origin.getX() + dx, y, origin.getZ() + dz)).isAir()) continue;
                    if (y >= minY) continue block1;
                    minY = y;
                    continue block1;
                }
            }
        }
        return minY;
    }

    @Nullable
    public static PlacedBuilding place(ServerLevel level, BuildingPlanSet planSet, String variant, int targetLevel, BlockPos pos, int clearHeight, int forcedBaseY) {
        int baseY;
        BuildingPlan level0Plan;
        Rotation rotation = Rotation.NONE;
        BuildingPlanSet.LevelDef levelDef = planSet.getLevel(variant, targetLevel);
        if (levelDef == null) {
            return null;
        }
        BuildingPlan plan = ModCultures.getBuildingPlan(levelDef.planId());
        if (plan == null) {
            return null;
        }
        BuildingPlanSet.LevelDef level0Def = planSet.getLevel(variant, 0);
        BuildingPlan buildingPlan = level0Plan = level0Def != null ? ModCultures.getBuildingPlan(level0Def.planId()) : plan;
        if (level0Plan == null) {
            level0Plan = plan;
        }
        ClearMargins margins = ClearMargins.symmetric(1);
        boolean[][] snowMap = TerrainPreparer.checkForSnow(level, pos, level0Plan.width(), level0Plan.depth(), rotation, margins);
        if (forcedBaseY != Integer.MIN_VALUE) {
            baseY = forcedBaseY;
        } else {
            ClearMargins effective = margins.forRotation(rotation);
            int effWidth = TerrainPreparer.effectiveWidth(level0Plan.width(), level0Plan.depth(), rotation);
            int effDepth = TerrainPreparer.effectiveDepth(level0Plan.width(), level0Plan.depth(), rotation);
            BlockPos effOrigin = TerrainPreparer.effectiveOrigin(pos, level0Plan.width(), level0Plan.depth(), rotation);
            baseY = TerrainPreparer.computeAverageSurfaceHeight(level, effOrigin, effWidth, effDepth, effective);
            baseY = Math.max(baseY, level.getSeaLevel() - level0Plan.groundLevel());
        }
        int effectiveClearHeight = Math.max(level0Plan.height(), clearHeight);
        TerrainPreparer.clearAndFlattenAtY(level, pos, level0Plan.width(), effectiveClearHeight, level0Plan.depth(), rotation, level0Plan.groundLevel(), baseY, margins);
        TerrainPreparer.decayOrphanedLeaves(level, pos, level0Plan.width(), level0Plan.depth(), rotation, baseY, margins);
        int placementY = baseY + level0Plan.groundLevel();
        BlockPos origin = new BlockPos(pos.getX(), placementY, pos.getZ());
        if (!BuildingPlacer.placeInstantly(level, level0Plan, origin, rotation, true)) {
            return null;
        }
        HearthLightingUtil.lightHearthsInArea(level, origin, new Vec3i(level0Plan.width(), level0Plan.height(), level0Plan.depth()));
        for (int lvl = 1; lvl <= targetLevel; ++lvl) {
            BuildingPlan upgradePlan;
            BuildingPlanSet.LevelDef upgradeDef = planSet.getLevel(variant, lvl);
            if (upgradeDef == null || (upgradePlan = ModCultures.getBuildingPlan(upgradeDef.planId())) == null) continue;
            int upgradePlacementY = baseY + upgradePlan.groundLevel();
            BlockPos upgradeOrigin = new BlockPos(pos.getX(), upgradePlacementY, pos.getZ());
            BuildingPlacer.placeUpgradeInstantly(level, upgradePlan, upgradeOrigin, rotation);
            HearthLightingUtil.lightHearthsInArea(level, upgradeOrigin, new Vec3i(upgradePlan.width(), upgradePlan.height(), upgradePlan.depth()));
        }
        TerrainPreparer.restoreSnow(level, pos, level0Plan.width(), level0Plan.depth(), rotation, snowMap, margins);
        int capturedY = baseY + plan.groundLevel();
        BlockPos nominalOrigin = new BlockPos(pos.getX(), capturedY, pos.getZ());
        Set<String> subKeys = CatalogBuildingStager.cumulativeSubBuildings(planSet, variant, targetLevel);
        if (subKeys.isEmpty()) {
            CatalogBuildingStager.carveSocle(level, pos, plan.width(), plan.depth(), baseY);
            return CatalogBuildingStager.tightBounds(level, nominalOrigin, plan.width(), plan.height(), plan.depth(), baseY, plan.buildingOrientation());
        }
        int scanBottomY = nominalOrigin.getY();
        int scanTopY = nominalOrigin.getY() + plan.height();
        int scanWidth = plan.width();
        int scanDepth = plan.depth();
        for (String subKey : subKeys) {
            BuildingPlan subPlan = CatalogBuildingStager.placeSubBuilding(level, planSet, subKey, origin, rotation);
            if (subPlan == null) continue;
            scanBottomY = Math.min(scanBottomY, origin.getY());
            scanTopY = Math.max(scanTopY, origin.getY() + subPlan.height());
            scanWidth = Math.max(scanWidth, subPlan.width());
            scanDepth = Math.max(scanDepth, subPlan.depth());
        }
        CatalogBuildingStager.carveSocle(level, pos, scanWidth, scanDepth, baseY);
        BlockPos scanOrigin = new BlockPos(pos.getX(), scanBottomY, pos.getZ());
        return CatalogBuildingStager.tightBounds(level, scanOrigin, scanWidth, scanTopY - scanBottomY, scanDepth, baseY, plan.buildingOrientation());
    }

    private static void carveSocle(ServerLevel level, BlockPos pos, int width, int depth, int baseY) {
        int top = baseY - 3;
        int bottom = 92;
        BlockPos.MutableBlockPos cursor = new BlockPos.MutableBlockPos();
        for (int dx = -8; dx < width + 8; ++dx) {
            for (int dz = -8; dz < depth + 8; ++dz) {
                for (int y = top - 1; y >= bottom; --y) {
                    cursor.set(pos.getX() + dx, y, pos.getZ() + dz);
                    if (level.getBlockState((BlockPos)cursor).isAir()) continue;
                    level.setBlock((BlockPos)cursor, Blocks.AIR.defaultBlockState(), 2);
                }
            }
        }
    }

    static Set<String> cumulativeSubBuildings(BuildingPlanSet planSet, String variant, int targetLevel) {
        LinkedHashSet<String> keys = new LinkedHashSet<String>(planSet.startingSubBuildings());
        for (int lvl = 0; lvl <= targetLevel; ++lvl) {
            BuildingPlanSet.LevelDef def = planSet.getLevel(variant, lvl);
            if (def == null) continue;
            keys.addAll(def.subBuildings());
        }
        return keys;
    }

    @Nullable
    private static BuildingPlan placeSubBuilding(ServerLevel level, BuildingPlanSet parentSet, String subKey, BlockPos origin, Rotation rotation) {
        BuildingPlan subPlan;
        ResourceLocation subSetId = ResourceLocation.fromNamespaceAndPath((String)"millenaire", (String)(parentSet.culture().getPath() + "/" + subKey.toLowerCase(Locale.ROOT)));
        BuildingPlanSet subSet = ModCultures.getBuildingPlanSet(subSetId);
        if (subSet == null || subSet.variants().isEmpty()) {
            return null;
        }
        String variant = subSet.variants().keySet().stream().sorted().findFirst().orElse(null);
        if (variant == null) {
            return null;
        }
        BuildingPlanSet.LevelDef def = subSet.getLevel(variant, 0);
        BuildingPlan buildingPlan = subPlan = def == null ? null : ModCultures.getBuildingPlan(def.planId());
        if (subPlan == null) {
            return null;
        }
        BuildingPlacer.placeInstantly(level, subPlan, origin, rotation, true);
        return subPlan;
    }

    private static PlacedBuilding tightBounds(ServerLevel level, BlockPos nominalOrigin, int width, int height, int depth, int baseY, int doorOrientation) {
        boolean[] solid = new boolean[width * height * depth];
        BlockPos.MutableBlockPos cursor = new BlockPos.MutableBlockPos();
        boolean any = false;
        for (int dx = 0; dx < width; ++dx) {
            for (int dy = 0; dy < height; ++dy) {
                for (int dz = 0; dz < depth; ++dz) {
                    if (level.getBlockState((BlockPos)cursor.set(nominalOrigin.getX() + dx, nominalOrigin.getY() + dy, nominalOrigin.getZ() + dz)).isAir()) continue;
                    solid[CatalogBuildingStager.idx((int)dx, (int)dy, (int)dz, (int)height, (int)depth)] = true;
                    any = true;
                }
            }
        }
        if (!any) {
            return new PlacedBuilding(nominalOrigin, nominalOrigin, width, height, depth, 0, baseY, nominalOrigin.getY(), doorOrientation);
        }
        boolean[] visited = new boolean[solid.length];
        int[] best = null;
        int[] bestByLevel = null;
        int bestSize = 0;
        ArrayDeque<Integer> stack = new ArrayDeque<Integer>();
        for (int seed = 0; seed < solid.length; ++seed) {
            if (!solid[seed] || visited[seed]) continue;
            visited[seed] = true;
            stack.push(seed);
            int size = 0;
            int minX = Integer.MAX_VALUE;
            int minY = Integer.MAX_VALUE;
            int minZ = Integer.MAX_VALUE;
            int maxX = Integer.MIN_VALUE;
            int maxY = Integer.MIN_VALUE;
            int maxZ = Integer.MIN_VALUE;
            int[] byLevel = new int[height];
            while (!stack.isEmpty()) {
                int cur = (Integer)stack.pop();
                int cz = cur % depth;
                int cy = cur / depth % height;
                int cx = cur / (depth * height);
                ++size;
                int n = cy;
                byLevel[n] = byLevel[n] + 1;
                if (cx < minX) {
                    minX = cx;
                }
                if (cy < minY) {
                    minY = cy;
                }
                if (cz < minZ) {
                    minZ = cz;
                }
                if (cx > maxX) {
                    maxX = cx;
                }
                if (cy > maxY) {
                    maxY = cy;
                }
                if (cz > maxZ) {
                    maxZ = cz;
                }
                for (int ox = -1; ox <= 1; ++ox) {
                    for (int oy = -1; oy <= 1; ++oy) {
                        for (int oz = -1; oz <= 1; ++oz) {
                            int ni;
                            if (ox == 0 && oy == 0 && oz == 0) continue;
                            int nx = cx + ox;
                            int ny = cy + oy;
                            int nz = cz + oz;
                            if (nx < 0 || ny < 0 || nz < 0 || nx >= width || ny >= height || nz >= depth || !solid[ni = CatalogBuildingStager.idx(nx, ny, nz, height, depth)] || visited[ni]) continue;
                            visited[ni] = true;
                            stack.push(ni);
                        }
                    }
                }
            }
            if (size <= bestSize) continue;
            bestSize = size;
            best = new int[]{minX, minY, minZ, maxX, maxY, maxZ};
            bestByLevel = byLevel;
        }
        void floorLevel = best[1];
        int aboveFloor = 0;
        for (void dy = floorLevel + true; dy < height; ++dy) {
            aboveFloor += bestByLevel[dy];
        }
        BlockPos boxOrigin = new BlockPos(nominalOrigin.getX() + best[0], nominalOrigin.getY() + best[1], nominalOrigin.getZ() + best[2]);
        return new PlacedBuilding(nominalOrigin, boxOrigin, (int)(best[3] - best[0] + true), (int)(best[4] - best[1] + true), (int)(best[5] - best[2] + true), aboveFloor, baseY, boxOrigin.getY(), doorOrientation);
    }

    private static int idx(int dx, int dy, int dz, int height, int depth) {
        return (dx * height + dy) * depth + dz;
    }

    public record PlacedBuilding(BlockPos nominalOrigin, BlockPos origin, int width, int height, int depth, int aboveFloorBlocks, int baseY, int groundBottomY, int doorOrientation) {
    }
}

