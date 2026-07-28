/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.mojang.logging.LogUtils
 *  net.minecraft.core.BlockPos
 *  net.minecraft.server.level.ServerLevel
 *  net.minecraft.world.level.block.Rotation
 *  org.slf4j.Logger
 */
package org.millenaire.village;

import com.mojang.logging.LogUtils;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.Rotation;
import org.millenaire.building.BuildingInstance;
import org.millenaire.building.BuildingPlan;
import org.millenaire.building.BuildingPlanSet;
import org.millenaire.building.ClearMargins;
import org.millenaire.building.ConstructionTask;
import org.millenaire.building.PlacementStep;
import org.millenaire.culture.ModCultures;
import org.millenaire.culture.VillageType;
import org.millenaire.village.BuildingFinalizer;
import org.millenaire.village.Village;
import org.millenaire.village.VillageGrowthManager;
import org.millenaire.world.BuildingPlacer;
import org.millenaire.world.TerrainPreparer;
import org.slf4j.Logger;

public final class WallGrowthManager {
    private static final Logger LOGGER = LogUtils.getLogger();
    private static final double RUSH_PLACEMENT_FLOOR_RATIO = 0.33;

    private WallGrowthManager() {
    }

    public static void evaluate(ServerLevel level, Village village) {
        BuildingPlanSet planSet;
        VillageType villageType = ModCultures.getVillageType(village.getVillageTypeId());
        if (villageType == null) {
            return;
        }
        if (villageType.loneBuilding()) {
            return;
        }
        if (village.isPlayerControlled()) {
            return;
        }
        int maxSlots = WallGrowthManager.computeMaxSlots(village, villageType);
        if (maxSlots <= 0) {
            return;
        }
        int ongoing = WallGrowthManager.countOngoing(village);
        if (ongoing >= maxSlots) {
            return;
        }
        int available = maxSlots - ongoing;
        for (BuildingInstance b : village.getBuildings()) {
            BuildingPlanSet.LevelDef levelDef;
            if (available <= 0) {
                return;
            }
            if (b.getStatus() != BuildingInstance.Status.PLANNED || !b.isWallSegment() || b.getPlanSetId() == null || b.getVariant() == null || (planSet = ModCultures.getBuildingPlanSet(b.getPlanSetId())) == null || (levelDef = planSet.getLevel(b.getVariant(), b.getLevel())) == null || !VillageGrowthManager.hasResourcesForProject(level, village, levelDef) || !WallGrowthManager.launchPlanned(level, village, b, planSet, levelDef)) continue;
            --available;
        }
        for (BuildingInstance b : village.getBuildings()) {
            BuildingPlanSet.LevelDef nextLevel;
            if (available <= 0) {
                return;
            }
            if (b.getStatus() != BuildingInstance.Status.COMPLETE || !b.isWallSegment() || !b.isUpgradesAllowed() || b.getPlanSetId() == null || b.getVariant() == null || (planSet = ModCultures.getBuildingPlanSet(b.getPlanSetId())) == null || !planSet.hasNextLevel(b.getVariant(), b.getLevel()) || (nextLevel = planSet.getLevel(b.getVariant(), b.getLevel() + 1)) == null || !VillageGrowthManager.checkBuildConditions(village, b, nextLevel) || !VillageGrowthManager.hasResourcesForProject(level, village, nextLevel)) continue;
            VillageGrowthManager.GrowthCandidate candidate = new VillageGrowthManager.GrowthCandidate(planSet, nextLevel, b.getVariant(), Math.max(1, nextLevel.priority()), true, b, null);
            VillageGrowthManager.launchUpgrade(level, village, candidate);
            --available;
        }
    }

    public static int rush(ServerLevel level, Village village, double placementRatio, int maxUpgradePasses) {
        int count = 0;
        for (BuildingInstance b : new ArrayList<BuildingInstance>(village.getBuildings())) {
            boolean shouldPlace;
            if (b.getStatus() != BuildingInstance.Status.PLANNED || !b.isWallSegment() || !(shouldPlace = level.getRandom().nextDouble() < placementRatio || placementRatio > 0.33) || !WallGrowthManager.rushPlace(level, village, b)) continue;
            ++count;
        }
        for (int pass = 0; pass < maxUpgradePasses; ++pass) {
            boolean anyUpgrade = false;
            for (BuildingInstance b : new ArrayList<BuildingInstance>(village.getBuildings())) {
                BuildingPlan newPlan;
                BuildingPlanSet.LevelDef nextLevel;
                BuildingPlanSet planSet;
                if (b.getStatus() != BuildingInstance.Status.COMPLETE || !b.isWallSegment() || b.getPlanSetId() == null || b.getVariant() == null || (planSet = ModCultures.getBuildingPlanSet(b.getPlanSetId())) == null || !planSet.hasNextLevel(b.getVariant(), b.getLevel()) || (nextLevel = planSet.getLevel(b.getVariant(), b.getLevel() + 1)) == null || (newPlan = ModCultures.getBuildingPlan(nextLevel.planId())) == null) continue;
                BlockPos upgradeOrigin = VillageGrowthManager.recalculateOriginForUpgrade(b, newPlan);
                if (!upgradeOrigin.equals((Object)b.getOrigin())) {
                    b.setOrigin(upgradeOrigin);
                }
                BuildingPlacer.placeUpgradeInstantly(level, newPlan, upgradeOrigin, b.getRotation(), b);
                b.startUpgrade(nextLevel.planId(), nextLevel.level());
                b.markComplete();
                BuildingFinalizer.applyPostPlacement(level, village, b, newPlan);
                ++count;
                anyUpgrade = true;
            }
            if (!anyUpgrade) break;
        }
        if (count > 0) {
            BuildingFinalizer.applyVillageUpdates(level, village);
        }
        return count;
    }

    private static boolean rushPlace(ServerLevel level, Village village, BuildingInstance instance) {
        if (instance.getPlanSetId() == null || instance.getVariant() == null) {
            return false;
        }
        BuildingPlanSet planSet = ModCultures.getBuildingPlanSet(instance.getPlanSetId());
        if (planSet == null) {
            return false;
        }
        BuildingPlan plan = ModCultures.getBuildingPlan(instance.getPlanId());
        if (plan == null) {
            return false;
        }
        BlockPos adjustedOrigin = instance.getOrigin();
        int baseY = adjustedOrigin.getY() - plan.groundLevel();
        BlockPos requestedOrigin = new BlockPos(adjustedOrigin.getX(), baseY, adjustedOrigin.getZ());
        Rotation rotation = instance.getRotation();
        VillageGrowthManager.initBrickColours(instance, planSet, village, level.getRandom());
        ClearMargins margins = planSet.clearMargins();
        boolean[][] snowMap = TerrainPreparer.checkForSnow(level, requestedOrigin, plan.width(), plan.depth(), rotation, margins);
        TerrainPreparer.clearAndFlattenAtY(level, requestedOrigin, plan.width(), plan.height(), plan.depth(), rotation, plan.groundLevel(), baseY, margins);
        TerrainPreparer.decayOrphanedLeaves(level, requestedOrigin, plan.width(), plan.depth(), rotation, baseY, margins);
        BuildingPlacer.placeInstantly(level, plan, adjustedOrigin, rotation, instance);
        TerrainPreparer.restoreSnow(level, requestedOrigin, plan.width(), plan.depth(), rotation, snowMap, margins);
        instance.markComplete();
        instance.setConstructionTask(null);
        BuildingFinalizer.applyPostPlacement(level, village, instance, plan);
        BuildingFinalizer.applyCompletionEffects(level, village, instance);
        LOGGER.info("[Mill\u00e9naire] Rush wall: placed {} L{} at {}", new Object[]{planSet.id(), instance.getLevel(), adjustedOrigin.toShortString()});
        return true;
    }

    public static int computeMaxSlots(Village village, VillageType villageType) {
        int base = villageType.maxSimultaneousWallConstructions();
        int extras = 0;
        for (BuildingInstance b : village.getBuildings()) {
            BuildingPlanSet planSet;
            if (b.getStatus() != BuildingInstance.Status.COMPLETE || b.getPlanSetId() == null || (planSet = ModCultures.getBuildingPlanSet(b.getPlanSetId())) == null) continue;
            extras += planSet.extraWallConstructionSlots();
        }
        return base + extras;
    }

    private static int countOngoing(Village village) {
        int count = 0;
        for (BuildingInstance b : village.getBuildings()) {
            if (b.getStatus() != BuildingInstance.Status.UNDER_CONSTRUCTION || !b.isWallSegment()) continue;
            ++count;
        }
        return count;
    }

    public static int countActiveBuilders(Village village) {
        int count = 0;
        for (BuildingInstance b : village.getBuildings()) {
            ConstructionTask task;
            if (b.getStatus() != BuildingInstance.Status.UNDER_CONSTRUCTION || !b.isWallSegment() || (task = b.getConstructionTask()) == null || !task.isReserved()) continue;
            ++count;
        }
        return count;
    }

    private static boolean launchPlanned(ServerLevel level, Village village, BuildingInstance instance, BuildingPlanSet planSet, BuildingPlanSet.LevelDef levelDef) {
        BuildingPlan plan = ModCultures.getBuildingPlan(levelDef.planId());
        if (plan == null) {
            return false;
        }
        BlockPos adjustedOrigin = instance.getOrigin();
        int baseY = adjustedOrigin.getY() - plan.groundLevel();
        BlockPos requestedOrigin = new BlockPos(adjustedOrigin.getX(), baseY, adjustedOrigin.getZ());
        Rotation rotation = instance.getRotation();
        VillageGrowthManager.initBrickColours(instance, planSet, village, level.getRandom());
        List<PlacementStep> terrainSteps = BuildingPlacer.compileTerrainPrepSteps(level, requestedOrigin, plan.width(), plan.height(), plan.depth(), baseY, adjustedOrigin, rotation, plan.groundLevel(), planSet.clearMargins());
        List<PlacementStep> buildingSteps = BuildingPlacer.compilePlacementSteps(level, plan, adjustedOrigin, rotation, instance);
        List<PlacementStep> dedupedTerrainSteps = VillageGrowthManager.deduplicateTerrainSteps(terrainSteps, buildingSteps, plan.groundLevel());
        ArrayList<PlacementStep> allSteps = new ArrayList<PlacementStep>(dedupedTerrainSteps.size() + buildingSteps.size());
        allSteps.addAll(dedupedTerrainSteps);
        allSteps.addAll(buildingSteps);
        instance.markUnderConstruction();
        if (!allSteps.isEmpty()) {
            instance.setConstructionTask(new ConstructionTask(allSteps, 0));
        }
        village.recordEvent(level, "Wall segment: " + planSet.id().getPath() + " (" + instance.getVariant() + "L" + instance.getLevel() + ") at " + adjustedOrigin.toShortString());
        LOGGER.info("[Mill\u00e9naire] Wall growth: launching {} L{} at {}", new Object[]{planSet.id(), instance.getLevel(), adjustedOrigin.toShortString()});
        return true;
    }
}

