/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.mojang.logging.LogUtils
 *  javax.annotation.Nullable
 *  net.minecraft.core.BlockPos
 *  net.minecraft.resources.ResourceLocation
 *  net.minecraft.server.level.ServerLevel
 *  net.minecraft.util.RandomSource
 *  net.minecraft.world.item.Item
 *  net.minecraft.world.level.Level
 *  net.minecraft.world.level.block.Rotation
 *  org.slf4j.Logger
 */
package org.millenaire.village;

import com.mojang.logging.LogUtils;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ThreadLocalRandom;
import java.util.random.RandomGenerator;
import javax.annotation.Nullable;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Rotation;
import org.millenaire.DisplayUtils;
import org.millenaire.building.AnywoodHelper;
import org.millenaire.building.BuildingId;
import org.millenaire.building.BuildingInstance;
import org.millenaire.building.BuildingInventory;
import org.millenaire.building.BuildingPlan;
import org.millenaire.building.BuildingPlanSet;
import org.millenaire.building.ClearMargins;
import org.millenaire.building.ConstructionTask;
import org.millenaire.building.GoodAvailabilityHelper;
import org.millenaire.building.PlacementStep;
import org.millenaire.config.MillenaireServerConfig;
import org.millenaire.culture.ModCultures;
import org.millenaire.culture.VillageType;
import org.millenaire.culture.VillagerType;
import org.millenaire.entity.MillVillager;
import org.millenaire.entity.VillagerAppearanceFactory;
import org.millenaire.entity.VillagerSpawnFactory;
import org.millenaire.item.ItemHelper;
import org.millenaire.village.BuildingFinalizer;
import org.millenaire.village.SubBuildingHelper;
import org.millenaire.village.Village;
import org.millenaire.village.VillageEventType;
import org.millenaire.village.VillagerRecord;
import org.millenaire.world.BuildingLocationFinder;
import org.millenaire.world.BuildingPlacer;
import org.millenaire.world.PlacedLocation;
import org.millenaire.world.PlacementConstraints;
import org.millenaire.world.TerrainPreparer;
import org.millenaire.world.TerrainReachability;
import org.millenaire.world.VillageTerrainMap;
import org.slf4j.Logger;

public final class VillageGrowthManager {
    private static final Logger LOGGER = LogUtils.getLogger();
    static final int GROWTH_TICK_INTERVAL = 20;
    private static final long NO_PROJECTS_CACHE_TICKS = 600L;
    private static final long PLACEMENT_FAILURE_COOLDOWN_TICKS = 1600L;
    private static final Map<String, Integer> TIER_MULTIPLIERS = Map.of("centre", 6, "townhall", 6, "start", 6, "player", 6, "core", 4, "secondary", 2, "extra", 1);
    private static final Set<String> CONTROLLED_AUTO_EXCLUDED_ROLES = Set.of("player", "core", "secondary");
    private static final List<String> TIER_ORDER = List.of("start", "player", "core", "secondary", "extra");

    private VillageGrowthManager() {
    }

    public static void evaluateGrowth(ServerLevel level, Village village) {
        long currentTick = level.getGameTime();
        if (currentTick % 600L == 0L) {
            LOGGER.debug("[Growth] {} \u2014 evaluateGrowth ENTRY, tick={}, typeId={}", new Object[]{village.getVillageName(), currentTick, village.getVillageTypeId()});
        }
        if (currentTick < village.getNoProjectsLeftUntil()) {
            if (currentTick % 600L == 0L) {
                LOGGER.debug("[Growth] {} \u2014 cached no-projects until tick {} (now={})", new Object[]{village.getVillageName(), village.getNoProjectsLeftUntil(), currentTick});
            }
            return;
        }
        VillageType villageType = ModCultures.getVillageType(village.getVillageTypeId());
        if (villageType == null) {
            LOGGER.warn("[Growth] {} \u2014 villageType is NULL for typeId={}", (Object)village.getVillageName(), (Object)village.getVillageTypeId());
            return;
        }
        if (villageType.loneBuilding()) {
            LOGGER.debug("[Growth] {} \u2014 loneBuilding, delegating to evaluateLoneBuildingUpgrades", (Object)village.getVillageName());
            VillageGrowthManager.evaluateLoneBuildingUpgrades(level, village, villageType);
            return;
        }
        if (!VillageGrowthManager.canStartNewConstruction(village, villageType)) {
            LOGGER.debug("[Growth] {} \u2014 max constructions reached", (Object)village.getVillageName());
            return;
        }
        if (VillageGrowthManager.tryLaunchPendingProject(level, village)) {
            LOGGER.debug("[Growth] {} \u2014 pending project launched", (Object)village.getVillageName());
            return;
        }
        if (VillageGrowthManager.tryLaunchPlannedSubBuilding(level, village)) {
            LOGGER.debug("[Growth] {} \u2014 planned sub-building launched", (Object)village.getVillageName());
            return;
        }
        if (village.getPendingProject() != null) {
            Village.PendingProject pp = village.getPendingProject();
            LOGGER.debug("[Growth] {} \u2014 pending {} lv{} waiting for resources", new Object[]{village.getVillageName(), pp.planSetId(), pp.level()});
            VillageGrowthManager.tryAffordableFallback(level, village, villageType, null);
            return;
        }
        List<GrowthCandidate> candidates = VillageGrowthManager.getAllCandidates(level, village, villageType);
        if (candidates.isEmpty()) {
            LOGGER.debug("[Growth] {} \u2014 no candidates, caching {}s", (Object)village.getVillageName(), (Object)30L);
            village.setNoProjectsLeftUntil(currentTick + 600L);
            return;
        }
        List<Integer> weights = candidates.stream().map(GrowthCandidate::weight).toList();
        GrowthCandidate chosen = VillageGrowthManager.weightedPick(candidates, weights, ThreadLocalRandom.current());
        if (chosen == null) {
            village.setNoProjectsLeftUntil(currentTick + 600L);
            return;
        }
        LOGGER.debug("[Growth] {} \u2014 chosen: {} lv{} (upgrade={}), checking resources...", new Object[]{village.getVillageName(), chosen.planSet().id(), chosen.levelDef().level(), chosen.isUpgrade()});
        if (VillageGrowthManager.hasResourcesForProject(level, village, chosen.levelDef())) {
            LOGGER.debug("[Growth] {} \u2014 resources OK, launching {}", (Object)village.getVillageName(), (Object)chosen.planSet().id());
            VillageGrowthManager.launchCandidate(level, village, chosen, true);
            return;
        }
        LOGGER.debug("[Growth] {} \u2014 resources INSUFFICIENT for {}, storing as pending", (Object)village.getVillageName(), (Object)chosen.planSet().id());
        VillageGrowthManager.storePending(village, chosen);
        VillageGrowthManager.tryAffordableFallback(level, village, villageType, chosen);
    }

    private static void tryAffordableFallback(ServerLevel level, Village village, VillageType villageType, @Nullable GrowthCandidate exclude) {
        GrowthCandidate fallback;
        List<GrowthCandidate> candidates = VillageGrowthManager.getAllCandidates(level, village, villageType, true);
        if (candidates.isEmpty()) {
            return;
        }
        ArrayList<GrowthCandidate> affordable = new ArrayList<GrowthCandidate>();
        ArrayList<Integer> fallbackWeights = new ArrayList<Integer>();
        Village.PendingProject pending = village.getPendingProject();
        for (GrowthCandidate c : candidates) {
            if (c == exclude || pending != null && VillageGrowthManager.isPendingMatch(c, pending) || !VillageGrowthManager.hasResources(level, village, c.levelDef())) continue;
            affordable.add(c);
            fallbackWeights.add(c.weight());
        }
        if (!affordable.isEmpty() && (fallback = (GrowthCandidate)VillageGrowthManager.weightedPick(affordable, fallbackWeights, ThreadLocalRandom.current())) != null) {
            LOGGER.debug("[Millenaire] Growth: fallback \u2014 {} (pending: {})", (Object)fallback.planSet().id(), pending != null ? pending.planSetId() : "none");
            VillageGrowthManager.launchCandidate(level, village, fallback, false);
        }
    }

    private static boolean isPendingMatch(GrowthCandidate c, Village.PendingProject pending) {
        if (!c.planSet().id().equals((Object)pending.planSetId())) {
            return false;
        }
        if (!c.variant().equals(pending.variant())) {
            return false;
        }
        return c.levelDef().level() == pending.level();
    }

    public static void onBuildingCompleted(Village village) {
        village.setNoProjectsLeftUntil(0L);
    }

    /*
     * WARNING - void declaration
     */
    private static void evaluateLoneBuildingUpgrades(ServerLevel level, Village village, VillageType villageType) {
        void var7_11;
        if (!VillageGrowthManager.canStartNewConstruction(village, villageType)) {
            return;
        }
        if (VillageGrowthManager.tryLaunchPendingProject(level, village)) {
            return;
        }
        if (village.getPendingProject() != null) {
            return;
        }
        long currentTick = level.getGameTime();
        ArrayList<GrowthCandidate> candidates = new ArrayList<GrowthCandidate>();
        for (BuildingInstance buildingInstance : village.getBuildings()) {
            BuildingPlanSet.LevelDef nextLevel;
            BuildingPlanSet planSet;
            if (buildingInstance.getStatus() != BuildingInstance.Status.COMPLETE || !buildingInstance.isUpgradesAllowed() || buildingInstance.getPlanSetId() == null || buildingInstance.getVariant() == null || (planSet = ModCultures.getBuildingPlanSet(buildingInstance.getPlanSetId())) == null || !planSet.hasNextLevel(buildingInstance.getVariant(), buildingInstance.getLevel()) || (nextLevel = planSet.getLevel(buildingInstance.getVariant(), buildingInstance.getLevel() + 1)) == null || !VillageGrowthManager.checkBuildConditions(village, buildingInstance, nextLevel)) continue;
            int weight = Math.max(1, nextLevel.priority());
            candidates.add(new GrowthCandidate(planSet, nextLevel, buildingInstance.getVariant(), weight, true, buildingInstance, null));
        }
        if (candidates.isEmpty()) {
            village.setNoProjectsLeftUntil(currentTick + 600L);
            return;
        }
        int totalWeight = 0;
        for (GrowthCandidate c : candidates) {
            totalWeight += c.weight();
        }
        GrowthCandidate growthCandidate = (GrowthCandidate)candidates.get(0);
        if (totalWeight > 0) {
            int roll = ThreadLocalRandom.current().nextInt(totalWeight);
            for (GrowthCandidate c : candidates) {
                if ((roll -= c.weight()) >= 0) continue;
                GrowthCandidate growthCandidate2 = c;
                break;
            }
        }
        if (VillageGrowthManager.hasResourcesForProject(level, village, var7_11.levelDef())) {
            VillageGrowthManager.launchCandidate(level, village, (GrowthCandidate)var7_11, true);
        } else {
            VillageGrowthManager.storePending(village, (GrowthCandidate)var7_11);
        }
    }

    private static List<GrowthCandidate> getAllCandidates(ServerLevel level, Village village, VillageType villageType) {
        return VillageGrowthManager.getAllCandidates(level, village, villageType, false, false);
    }

    private static List<GrowthCandidate> getAllCandidates(ServerLevel level, Village village, VillageType villageType, boolean skipTierGating) {
        return VillageGrowthManager.getAllCandidates(level, village, villageType, skipTierGating, false);
    }

    private static List<GrowthCandidate> getAllCandidates(ServerLevel level, Village village, VillageType villageType, boolean skipTierGating, boolean skipPlacementCooldown) {
        BuildingPlanSet planSet;
        String allowedTier;
        long currentTick = level.getGameTime();
        ArrayList<GrowthCandidate> candidates = new ArrayList<GrowthCandidate>();
        String string = allowedTier = skipTierGating ? null : VillageGrowthManager.findLowestUnsatisfiedTier(village, villageType);
        if (!"player".equals(allowedTier)) {
            for (VillageType.LayoutSlot slot : villageType.layout()) {
                String variant;
                BuildingPlanSet.LevelDef levelDef;
                int layoutCountSoFar;
                int existingCount;
                if (VillageGrowthManager.isCentreRole(slot.role()) || VillageGrowthManager.isAutoExcludedInControlledVillage(villageType.playerControlled(), slot.role()) || !skipTierGating && (allowedTier == null ? TIER_ORDER.contains(slot.role()) : !allowedTier.equals(slot.role()))) continue;
                planSet = ModCultures.getBuildingPlanSet(slot.plan());
                if (planSet == null || villageType.neverBuildings().contains(planSet.buildingId()) || (existingCount = VillageGrowthManager.countBuildingsOfType(village, planSet.id())) > (layoutCountSoFar = VillageGrowthManager.countPriorLayoutSlots(villageType, slot, planSet.id())) || planSet.maxCount() > 0 && existingCount >= planSet.maxCount() || !skipPlacementCooldown && VillageGrowthManager.isOnPlacementCooldown(village, planSet.id(), currentTick) || (levelDef = planSet.getLevel(variant = planSet.pickRandomVariant(ThreadLocalRandom.current()), 0)) == null) continue;
                int weight = VillageGrowthManager.computeLayoutSlotWeight(slot, levelDef);
                candidates.add(new GrowthCandidate(planSet, levelDef, variant, weight, false, null, slot));
            }
        }
        if ("player".equals(allowedTier) || allowedTier == null || skipTierGating) {
            for (ResourceLocation playerPlanId : villageType.playerBuildings()) {
                if (!VillageGrowthManager.isPlayerBuildingCandidate(village, playerPlanId)) {
                    if (!village.isBuildingBought(playerPlanId)) continue;
                    LOGGER.debug("[Growth] Player building {} bought but NOT candidate (already built: {})", (Object)playerPlanId, (Object)VillageGrowthManager.countBuildingsOfType(village, playerPlanId));
                    continue;
                }
                planSet = ModCultures.getBuildingPlanSet(playerPlanId);
                if (planSet == null) continue;
                if (!skipPlacementCooldown && VillageGrowthManager.isOnPlacementCooldown(village, planSet.id(), currentTick)) {
                    LOGGER.debug("[Growth] Player building {} on placement cooldown", (Object)playerPlanId);
                    continue;
                }
                String variant = planSet.pickRandomVariant(ThreadLocalRandom.current());
                BuildingPlanSet.LevelDef levelDef = planSet.getLevel(variant, 0);
                if (levelDef == null) continue;
                int tierMultiplier = TIER_MULTIPLIERS.getOrDefault("player", 6);
                int weight = Math.max(1, levelDef.priority() * tierMultiplier);
                candidates.add(new GrowthCandidate(planSet, levelDef, variant, weight, false, null, null));
            }
        }
        for (BuildingInstance b : village.getBuildings()) {
            BuildingPlanSet.LevelDef nextLevel;
            if (b.getStatus() != BuildingInstance.Status.COMPLETE || b.isWallSegment() || !b.isUpgradesAllowed() || b.getPlanSetId() == null || b.getVariant() == null || (planSet = ModCultures.getBuildingPlanSet(b.getPlanSetId())) == null || !planSet.hasNextLevel(b.getVariant(), b.getLevel()) || (nextLevel = planSet.getLevel(b.getVariant(), b.getLevel() + 1)) == null || !VillageGrowthManager.checkBuildConditions(village, b, nextLevel)) continue;
            int weight = Math.max(1, nextLevel.priority());
            candidates.add(new GrowthCandidate(planSet, nextLevel, b.getVariant(), weight, true, b, null));
        }
        return candidates;
    }

    private static void launchCandidate(ServerLevel level, Village village, GrowthCandidate candidate, boolean clearPending) {
        if (clearPending) {
            village.setPendingProject(null);
        }
        if (candidate.isUpgrade()) {
            VillageGrowthManager.launchUpgrade(level, village, candidate);
        } else {
            VillageGrowthManager.launchNewBuilding(level, village, candidate);
        }
    }

    private static void storePending(Village village, GrowthCandidate candidate) {
        if (candidate.isUpgrade() && candidate.existingBuilding() != null) {
            village.setPendingProject(new Village.PendingProject(candidate.planSet().id(), candidate.variant(), candidate.levelDef().level(), true, candidate.existingBuilding().getId()));
        } else {
            village.setPendingProject(new Village.PendingProject(candidate.planSet().id(), candidate.variant(), candidate.levelDef().level(), false, null));
        }
    }

    private static void launchNewBuilding(ServerLevel level, Village village, GrowthCandidate candidate) {
        VillageGrowthManager.placeNewBuildingForConstruction(level, village, candidate.planSet(), candidate.levelDef(), candidate.variant(), candidate.slot());
    }

    private static boolean placeNewBuildingForConstruction(ServerLevel level, Village village, BuildingPlanSet planSet, BuildingPlanSet.LevelDef levelDef, String variant, @Nullable VillageType.LayoutSlot slot) {
        return VillageGrowthManager.placeNewBuildingForConstruction(level, village, planSet, levelDef, variant, slot, null);
    }

    private static boolean placeNewBuildingForConstruction(ServerLevel level, Village village, BuildingPlanSet planSet, BuildingPlanSet.LevelDef levelDef, String variant, @Nullable VillageType.LayoutSlot slot, @Nullable PlacedLocation preResolvedLocation) {
        BuildingPlan plan = ModCultures.getBuildingPlan(levelDef.planId());
        if (plan == null) {
            return false;
        }
        long currentTick = level.getGameTime();
        VillageType vt = ModCultures.getVillageType(village.getVillageTypeId());
        int radius = vt != null ? vt.radius() : 90;
        VillageTerrainMap terrainMap = VillageTerrainMap.compute(level, village.getCenter(), radius);
        VillageGrowthManager.markExistingBuildingsOccupied(terrainMap, village);
        TerrainReachability reachability = TerrainReachability.compute(terrainMap, village.getCenter());
        PlacementConstraints constraints = PlacementConstraints.resolve(planSet, slot, radius);
        ClearMargins margins = planSet.clearMargins().atLeast(constraints.clearMargin());
        PlacedLocation location = preResolvedLocation;
        if (location == null) {
            location = BuildingLocationFinder.findLocation(terrainMap, plan, village.getCenter(), constraints, margins, new ArrayList<BuildingInstance>(village.getBuildings()), reachability);
        }
        if (location == null) {
            LOGGER.warn("[Millenaire] Growth: no location found for {}, cooldown {}s", (Object)planSet.id(), (Object)80L);
            VillageGrowthManager.addPlacementCooldown(village, planSet.id(), currentTick);
            return false;
        }
        BlockPos requestedOrigin = location.position();
        Rotation rotation = location.rotation();
        int baseY = terrainMap.computeAverageAltitude(requestedOrigin.getX(), requestedOrigin.getZ(), plan.width(), plan.depth(), margins, rotation);
        int placementY = baseY + plan.groundLevel();
        BlockPos adjustedOrigin = new BlockPos(requestedOrigin.getX(), placementY, requestedOrigin.getZ());
        BuildingId buildingId = BuildingId.random();
        BuildingInstance instance = new BuildingInstance(buildingId, plan.id(), adjustedOrigin, rotation, BuildingInstance.Status.UNDER_CONSTRUCTION, planSet.id(), variant, 0);
        VillageGrowthManager.initBrickColours(instance, planSet, village, level.getRandom());
        List<PlacementStep> terrainSteps = BuildingPlacer.compileTerrainPrepSteps(level, requestedOrigin, plan.width(), plan.height(), plan.depth(), baseY, adjustedOrigin, rotation, plan.groundLevel(), margins);
        List<PlacementStep> buildingSteps = BuildingPlacer.compilePlacementSteps(level, plan, adjustedOrigin, rotation, instance);
        TerrainPreparer.clearLeavesInFootprint(level, requestedOrigin, plan.width(), plan.height(), plan.depth(), rotation, baseY, plan.groundLevel(), margins, 2);
        List<PlacementStep> dedupedTerrainSteps = VillageGrowthManager.deduplicateTerrainSteps(terrainSteps, buildingSteps, plan.groundLevel());
        ArrayList<PlacementStep> allSteps = new ArrayList<PlacementStep>(dedupedTerrainSteps.size() + buildingSteps.size());
        allSteps.addAll(dedupedTerrainSteps);
        allSteps.addAll(buildingSteps);
        if (!allSteps.isEmpty()) {
            instance.setConstructionTask(new ConstructionTask(allSteps, 0));
        }
        village.addBuilding(instance);
        if (allSteps.isEmpty()) {
            LOGGER.warn("Building {} has 0 placement steps \u2014 marking COMPLETE immediately", (Object)planSet.buildingId());
            VillageGrowthManager.completeImmediately(level, village, instance, plan, planSet);
        }
        SubBuildingHelper.spawnStartingSubBuildings(level, village, planSet, instance, false);
        SubBuildingHelper.spawnUpgradeSubBuildings(level, village, planSet, levelDef, instance, false);
        village.recordEvent(level, "New building: " + planSet.id().getPath() + " (" + variant + "L0) at " + adjustedOrigin.toShortString());
        village.recordChronicleEvent(level, VillageEventType.BUILDING_STARTED, planSet.nativeName(), null);
        LOGGER.info("[Millenaire] Growth: new building {} ({}L0) at {}", new Object[]{planSet.buildingId(), variant, adjustedOrigin.toShortString()});
        return true;
    }

    static void launchUpgrade(ServerLevel level, Village village, GrowthCandidate candidate) {
        BuildingInstance target = candidate.existingBuilding();
        if (target == null) {
            return;
        }
        VillageGrowthManager.setupUpgradeConstruction(level, village, target, candidate.planSet(), candidate.levelDef());
    }

    private static void setupUpgradeConstruction(ServerLevel level, Village village, BuildingInstance target, BuildingPlanSet planSet, BuildingPlanSet.LevelDef levelDef) {
        BuildingPlan newPlan = ModCultures.getBuildingPlan(levelDef.planId());
        if (newPlan == null) {
            return;
        }
        BlockPos upgradeOrigin = VillageGrowthManager.recalculateOriginForUpgrade(target, newPlan);
        target.startUpgrade(levelDef.planId(), levelDef.level());
        village.invalidateBuildingTagCache();
        if (!upgradeOrigin.equals((Object)target.getOrigin())) {
            target.setOrigin(upgradeOrigin);
        }
        village.recordEvent(level, "Upgrade started: " + planSet.buildingId() + " \u2192 level " + levelDef.level());
        village.recordChronicleEvent(level, VillageEventType.UPGRADE_STARTED, planSet.nativeName(), String.valueOf(levelDef.level()));
        List<PlacementStep> steps = BuildingPlacer.compilePlacementSteps(level, newPlan, upgradeOrigin, target.getRotation(), target);
        if (!steps.isEmpty()) {
            target.setConstructionTask(new ConstructionTask(steps, 0));
        } else {
            LOGGER.warn("Upgrade {} \u2192 level {} has 0 placement steps \u2014 marking COMPLETE immediately", (Object)planSet.buildingId(), (Object)levelDef.level());
            VillageGrowthManager.completeImmediately(level, village, target, newPlan, planSet);
        }
        LOGGER.info("[Millenaire] Upgrade: {} \u2192 level {}", (Object)planSet.buildingId(), (Object)levelDef.level());
    }

    private static void completeImmediately(ServerLevel level, Village village, BuildingInstance building, BuildingPlan plan, BuildingPlanSet planSet) {
        BuildingPlanSet.LevelDef currentLevelDef;
        BuildingPlanSet.LevelDef completedLevelDef;
        building.markComplete();
        BuildingFinalizer.applyPostPlacement(level, village, building, plan);
        String chronicleName = plan.id().getPath();
        if (building.getVariant() != null) {
            BuildingPlanSet.LevelDef ld = planSet.getLevel(building.getVariant(), building.getLevel());
            chronicleName = ld != null && ld.nativeName() != null ? ld.nativeName() : planSet.nativeName();
        }
        village.recordChronicleEvent(level, VillageEventType.BUILDING_COMPLETED, chronicleName, null);
        BuildingFinalizer.applyCompletionEffects(level, village, building);
        BuildingPlacer.spawnWallDecorations(level, building.getResolvedPoints());
        BuildingFinalizer.applyVillageUpdates(level, village);
        if (building.getLevel() > 0 && building.getPlanSetId() != null && building.getVariant() != null && (completedLevelDef = planSet.getLevel(building.getVariant(), building.getLevel())) != null) {
            SubBuildingHelper.spawnUpgradeSubBuildings(level, village, planSet, completedLevelDef, building, true);
        }
        if (building.getLevel() == 0 && building.getVariant() != null && (currentLevelDef = planSet.getLevel(building.getVariant(), building.getLevel())) != null) {
            village.getPathManager().markPathsDirty();
            village.markDirty();
        }
    }

    public static int countTotalProjects(VillageType villageType) {
        int total = 0;
        for (VillageType.LayoutSlot slot : villageType.layout()) {
            BuildingPlanSet planSet = ModCultures.getBuildingPlanSet(slot.plan());
            if (planSet == null) continue;
            if (!VillageGrowthManager.isCentreRole(slot.role())) {
                ++total;
            }
            int maxUpgrades = 0;
            for (String string : planSet.variants().keySet()) {
                int levels = planSet.getLevelCount(string);
                if (levels <= 1) continue;
                maxUpgrades = Math.max(maxUpgrades, levels - 1);
            }
            total += maxUpgrades;
            HashSet<String> allSubKeys = new HashSet<String>(planSet.startingSubBuildings());
            for (String v : planSet.variants().keySet()) {
                List<BuildingPlanSet.LevelDef> levels = planSet.variants().get(v);
                if (levels == null) continue;
                for (BuildingPlanSet.LevelDef ld : levels) {
                    allSubKeys.addAll(ld.subBuildings());
                }
            }
            String string = planSet.culture().getPath();
            for (String subKey : allSubKeys) {
                ResourceLocation subId = ResourceLocation.fromNamespaceAndPath((String)"millenaire", (String)(string + "/" + subKey));
                BuildingPlanSet subPlanSet = ModCultures.getBuildingPlanSet(subId);
                if (subPlanSet == null) continue;
                int maxSubLevels = 0;
                for (String subVariant : subPlanSet.variants().keySet()) {
                    maxSubLevels = Math.max(maxSubLevels, subPlanSet.getLevelCount(subVariant));
                }
                total += maxSubLevels;
            }
        }
        return total;
    }

    public static boolean rushOneProject(ServerLevel level, Village village) {
        return VillageGrowthManager.rushOneProject(level, village, null);
    }

    public static boolean rushOneProject(ServerLevel level, Village village, @Nullable VillageTerrainMap terrainMap) {
        return VillageGrowthManager.rushOneProject(level, village, terrainMap, null);
    }

    public static boolean rushOneProject(ServerLevel level, Village village, @Nullable VillageTerrainMap terrainMap, @Nullable Set<ResourceLocation> rushExcluded) {
        return VillageGrowthManager.rushOneProject(level, village, terrainMap, rushExcluded, null);
    }

    public static boolean rushOneProject(ServerLevel level, Village village, @Nullable VillageTerrainMap terrainMap, @Nullable Set<ResourceLocation> rushExcluded, @Nullable TerrainReachability reachability) {
        return VillageGrowthManager.rushOneProject(level, village, terrainMap, rushExcluded, reachability, null);
    }

    public static boolean rushOneProject(ServerLevel level, Village village, @Nullable VillageTerrainMap terrainMap, @Nullable Set<ResourceLocation> rushExcluded, @Nullable TerrainReachability reachability, @Nullable RushDispersion dispersion) {
        List<Integer> weights;
        GrowthCandidate chosen;
        VillageType villageType = ModCultures.getVillageType(village.getVillageTypeId());
        if (villageType == null) {
            return false;
        }
        List<GrowthCandidate> candidates = VillageGrowthManager.getAllCandidates(level, village, villageType, false, true);
        if (rushExcluded != null && !rushExcluded.isEmpty()) {
            candidates.removeIf(c -> rushExcluded.contains(c.planSet().id()));
        }
        if (candidates.isEmpty()) {
            return false;
        }
        if (dispersion != null) {
            List<GrowthCandidate> available = VillageGrowthManager.filterUnblocked(candidates, dispersion);
            while (available.isEmpty() && dispersion.hasActiveCooldowns()) {
                dispersion.tick();
                available = VillageGrowthManager.filterUnblocked(candidates, dispersion);
            }
            if (!available.isEmpty()) {
                candidates = available;
            }
        }
        if ((chosen = VillageGrowthManager.weightedPick(candidates, weights = candidates.stream().map(GrowthCandidate::weight).toList(), ThreadLocalRandom.current())) == null) {
            return false;
        }
        if (chosen.isUpgrade()) {
            boolean upgraded = VillageGrowthManager.executeRushUpgrade(level, village, chosen);
            if (upgraded) {
                if (rushExcluded != null) {
                    rushExcluded.clear();
                }
                if (dispersion != null && chosen.existingBuilding() != null) {
                    dispersion.tick();
                    dispersion.occupy(chosen.existingBuilding().getId(), chosen.levelDef().width(), chosen.levelDef().depth());
                }
            }
            return upgraded;
        }
        BuildingInstance placed = VillageGrowthManager.executeRushPlacement(level, village, chosen, terrainMap, reachability);
        if (placed != null) {
            if (rushExcluded != null) {
                rushExcluded.clear();
            }
            if (dispersion != null) {
                dispersion.tick();
                dispersion.occupy(placed.getId(), chosen.levelDef().width(), chosen.levelDef().depth());
            }
            return true;
        }
        if (rushExcluded != null) {
            rushExcluded.add(chosen.planSet().id());
        }
        return false;
    }

    private static List<GrowthCandidate> filterUnblocked(List<GrowthCandidate> candidates, RushDispersion dispersion) {
        ArrayList<GrowthCandidate> out = new ArrayList<GrowthCandidate>();
        for (GrowthCandidate c : candidates) {
            if (c.isUpgrade() && c.existingBuilding() != null && dispersion.isBlocked(c.existingBuilding().getId())) continue;
            out.add(c);
        }
        return out;
    }

    private static BuildingInstance executeRushPlacement(ServerLevel level, Village village, GrowthCandidate candidate, @Nullable VillageTerrainMap sharedTerrainMap, @Nullable TerrainReachability reachability) {
        VillageTerrainMap terrainMap;
        BuildingPlanSet planSet = candidate.planSet();
        BuildingPlanSet.LevelDef levelDef = candidate.levelDef();
        String variant = candidate.variant();
        VillageType.LayoutSlot slot = candidate.slot();
        BuildingPlan plan = ModCultures.getBuildingPlan(levelDef.planId());
        if (plan == null) {
            return null;
        }
        if (sharedTerrainMap != null) {
            terrainMap = sharedTerrainMap;
        } else {
            VillageType vt = ModCultures.getVillageType(village.getVillageTypeId());
            int radius = vt != null ? vt.radius() : 90;
            terrainMap = VillageTerrainMap.compute(level, village.getCenter(), radius);
            VillageGrowthManager.markExistingBuildingsOccupied(terrainMap, village);
        }
        VillageType vt2 = ModCultures.getVillageType(village.getVillageTypeId());
        int rushRadius = vt2 != null ? vt2.radius() : 90;
        PlacementConstraints constraints = PlacementConstraints.resolve(planSet, slot, rushRadius);
        ClearMargins margins = slot != null ? planSet.clearMargins().atLeast(slot.clearMargin()) : planSet.clearMargins();
        PlacedLocation location = BuildingLocationFinder.findLocation(terrainMap, plan, village.getCenter(), constraints, margins, new ArrayList<BuildingInstance>(village.getBuildings()), reachability);
        if (location == null) {
            LOGGER.warn("[Millenaire] Rush: no location found for {}", (Object)planSet.id());
            return null;
        }
        BlockPos requestedOrigin = location.position();
        Rotation rotation = location.rotation();
        int baseY = terrainMap.computeAverageAltitude(requestedOrigin.getX(), requestedOrigin.getZ(), plan.width(), plan.depth(), margins, rotation);
        boolean[][] snowMap = TerrainPreparer.checkForSnow(level, requestedOrigin, plan.width(), plan.depth(), rotation, margins);
        TerrainPreparer.clearAndFlattenAtY(level, requestedOrigin, plan.width(), plan.height(), plan.depth(), rotation, plan.groundLevel(), baseY, margins);
        TerrainPreparer.decayOrphanedLeaves(level, requestedOrigin, plan.width(), plan.depth(), rotation, baseY, margins);
        int placementY = baseY + plan.groundLevel();
        BlockPos adjustedOrigin = new BlockPos(requestedOrigin.getX(), placementY, requestedOrigin.getZ());
        BuildingId buildingId = BuildingId.random();
        BuildingInstance instance = new BuildingInstance(buildingId, plan.id(), adjustedOrigin, rotation, BuildingInstance.Status.COMPLETE, planSet.id(), variant, 0);
        VillageGrowthManager.initBrickColours(instance, planSet, village, level.getRandom());
        BuildingPlacer.placeInstantly(level, plan, adjustedOrigin, rotation, instance);
        TerrainPreparer.restoreSnow(level, requestedOrigin, plan.width(), plan.depth(), rotation, snowMap, margins);
        BuildingFinalizer.applyPostPlacement(level, village, instance, plan);
        village.addBuilding(instance);
        if (sharedTerrainMap != null) {
            sharedTerrainMap.markBuildingFootprint(requestedOrigin.getX(), requestedOrigin.getZ(), plan.width(), plan.depth(), margins, rotation, baseY);
        }
        BuildingFinalizer.applyCompletionEffects(level, village, instance);
        SubBuildingHelper.spawnStartingSubBuildings(level, village, planSet, instance, true);
        SubBuildingHelper.spawnUpgradeSubBuildings(level, village, planSet, levelDef, instance, true);
        VillageGrowthManager.spawnBuildingOccupants(level, village, planSet, instance);
        village.recordEvent(level, "Rush: new building " + planSet.id().getPath() + " (" + variant + "L0) at " + adjustedOrigin.toShortString());
        LOGGER.info("[Millenaire] Rush: new building {} ({}L0) at {}", new Object[]{planSet.buildingId(), variant, adjustedOrigin.toShortString()});
        return instance;
    }

    private static boolean executeRushUpgrade(ServerLevel level, Village village, GrowthCandidate candidate) {
        BuildingInstance target = candidate.existingBuilding();
        if (target == null) {
            return false;
        }
        BuildingPlanSet planSet = candidate.planSet();
        BuildingPlanSet.LevelDef nextLevel = candidate.levelDef();
        BuildingPlan newPlan = ModCultures.getBuildingPlan(nextLevel.planId());
        if (newPlan == null) {
            return false;
        }
        BlockPos currentOrigin = target.getOrigin();
        BlockPos upgradeOrigin = VillageGrowthManager.recalculateOriginForUpgrade(target, newPlan);
        if (!upgradeOrigin.equals((Object)currentOrigin)) {
            target.setOrigin(upgradeOrigin);
        }
        BuildingPlacer.placeUpgradeInstantly(level, newPlan, upgradeOrigin, target.getRotation(), target);
        target.startUpgrade(nextLevel.planId(), nextLevel.level());
        village.invalidateBuildingTagCache();
        target.markComplete();
        BuildingFinalizer.applyPostPlacement(level, village, target, newPlan);
        SubBuildingHelper.spawnUpgradeSubBuildings(level, village, planSet, nextLevel, target, true);
        BuildingFinalizer.applyCompletionEffects(level, village, target);
        village.recordEvent(level, "Rush: upgrade " + planSet.buildingId() + " \u2192 level " + nextLevel.level());
        LOGGER.info("[Millenaire] Rush: upgrade {} \u2192 level {}", (Object)planSet.buildingId(), (Object)nextLevel.level());
        return true;
    }

    static boolean canStartNewConstruction(Village village, @Nullable VillageType villageType) {
        int maxSlots = villageType != null ? villageType.maxSimultaneousConstructions() : 1;
        int ongoing = 0;
        for (BuildingInstance b : village.getBuildings()) {
            ConstructionTask task;
            if (!b.isBeingBuilt() || (task = b.getConstructionTask()) != null && task.isBlocked() || b.isWallSegment()) continue;
            ++ongoing;
        }
        return ongoing < maxSlots;
    }

    @Nullable
    private static String findLowestUnsatisfiedTier(Village village, VillageType villageType) {
        for (String tier : TIER_ORDER) {
            if (!VillageGrowthManager.hasPendingSlotsForTier(village, villageType, tier)) continue;
            return tier;
        }
        return null;
    }

    private static boolean hasPendingSlotsForTier(Village village, VillageType villageType, String tier) {
        if ("player".equals(tier)) {
            for (ResourceLocation playerPlanId : villageType.playerBuildings()) {
                if (!VillageGrowthManager.isPlayerBuildingCandidate(village, playerPlanId)) continue;
                return true;
            }
            return false;
        }
        for (VillageType.LayoutSlot slot : villageType.layout()) {
            int layoutCountSoFar;
            int existingCount;
            BuildingPlanSet planSet;
            if (!tier.equals(slot.role()) || (planSet = ModCultures.getBuildingPlanSet(slot.plan())) == null || (existingCount = VillageGrowthManager.countBuildingsOfType(village, planSet.id())) > (layoutCountSoFar = VillageGrowthManager.countPriorLayoutSlots(villageType, slot, planSet.id())) || planSet.maxCount() > 0 && existingCount >= planSet.maxCount()) continue;
            return true;
        }
        return false;
    }

    public static void spawnBuildingOccupants(ServerLevel level, Village village, BuildingPlanSet planSet, BuildingInstance building) {
        int i;
        MillVillager wife;
        String culturePath;
        ResourceLocation firstTypeId;
        VillagerType firstType;
        String sharedFamilyName = null;
        if (!planSet.maleResidents().isEmpty() && !planSet.femaleResidents().isEmpty() && (firstType = ModCultures.getVillagerType(firstTypeId = ResourceLocation.fromNamespaceAndPath((String)"millenaire", (String)((culturePath = village.getCultureId().getPath()) + "/" + planSet.maleResidents().get(0))))) != null) {
            HashSet<String> namesTaken = new HashSet<String>();
            for (VillagerRecord record : village.getVillagerRecords().values()) {
                String fam = record.getFamilyName();
                if (fam == null || fam.isEmpty()) continue;
                namesTaken.add(fam);
            }
            sharedFamilyName = VillagerAppearanceFactory.generateUniqueFamilyName(firstType, namesTaken);
        }
        MillVillager husband = planSet.maleResidents().isEmpty() ? null : VillageGrowthManager.spawnOccupant(level, village, planSet.maleResidents().get(0), building, sharedFamilyName);
        MillVillager millVillager = wife = planSet.femaleResidents().isEmpty() ? null : VillageGrowthManager.spawnOccupant(level, village, planSet.femaleResidents().get(0), building, sharedFamilyName);
        if (husband != null && wife != null && !husband.isChild() && !wife.isChild()) {
            husband.setSpousesName(wife.getFirstName() + " " + wife.getFamilyName());
            wife.setSpousesName(husband.getFirstName() + " " + husband.getFamilyName());
        }
        for (i = 1; i < planSet.maleResidents().size(); ++i) {
            VillageGrowthManager.spawnOccupant(level, village, planSet.maleResidents().get(i), building, sharedFamilyName);
        }
        for (i = 1; i < planSet.femaleResidents().size(); ++i) {
            VillageGrowthManager.spawnOccupant(level, village, planSet.femaleResidents().get(i), building, sharedFamilyName);
        }
    }

    @Nullable
    private static MillVillager spawnOccupant(ServerLevel level, Village village, @Nullable String villagerName, BuildingInstance building, @Nullable String sharedFamilyName) {
        BlockPos spawnPos;
        if (villagerName == null) {
            return null;
        }
        String culturePath = village.getCultureId().getPath();
        ResourceLocation villagerTypeId = ResourceLocation.fromNamespaceAndPath((String)"millenaire", (String)(culturePath + "/" + villagerName));
        MillVillager villager = VillagerSpawnFactory.spawnInVillage(level, village, villagerTypeId, spawnPos = building.getPathStartPos(), building.getId());
        if (villager == null) {
            return null;
        }
        if (sharedFamilyName != null) {
            villager.setFamilyName(sharedFamilyName);
        }
        village.recordEvent(level, "New villager: " + villagerTypeId.getPath() + " [" + villager.getUUID().toString().substring(0, 8) + "] in " + building.getPlanId().getPath());
        String roleName = DisplayUtils.resolveRoleName(villagerTypeId);
        village.recordChronicleEvent(level, VillageEventType.VILLAGER_SPAWNED, villager.getFirstName() + " " + villager.getFamilyName(), roleName);
        LOGGER.info("[Millenaire] New villager: {}", (Object)villagerTypeId.getPath());
        return villager;
    }

    private static boolean tryLaunchPendingProject(ServerLevel level, Village village) {
        Village.PendingProject pending = village.getPendingProject();
        if (pending == null) {
            return false;
        }
        BuildingPlanSet planSet = ModCultures.getBuildingPlanSet(pending.planSetId());
        if (planSet == null) {
            village.setPendingProject(null);
            return false;
        }
        BuildingPlanSet.LevelDef levelDef = planSet.getLevel(pending.variant(), pending.level());
        if (levelDef == null) {
            village.setPendingProject(null);
            return false;
        }
        if (!VillageGrowthManager.hasResourcesForProject(level, village, levelDef)) {
            return false;
        }
        if (pending.isUpgrade()) {
            if (pending.buildingId() == null) {
                village.setPendingProject(null);
                return false;
            }
            BuildingInstance target = village.getBuilding(pending.buildingId());
            if (target == null || target.getStatus() != BuildingInstance.Status.COMPLETE) {
                village.setPendingProject(null);
                return false;
            }
            village.setPendingProject(null);
            VillageGrowthManager.setupUpgradeConstruction(level, village, target, planSet, levelDef);
            return true;
        }
        VillageType villageType = ModCultures.getVillageType(village.getVillageTypeId());
        if (villageType == null) {
            village.setPendingProject(null);
            return false;
        }
        VillageType.LayoutSlot slot = VillageGrowthManager.findSlotForPlanSet(villageType, pending.planSetId());
        PlacedLocation planned = pending.plannedLocation();
        village.setPendingProject(null);
        boolean placed = VillageGrowthManager.placeNewBuildingForConstruction(level, village, planSet, levelDef, pending.variant(), slot, planned);
        return placed;
    }

    private static boolean tryLaunchPlannedSubBuilding(ServerLevel level, Village village) {
        for (BuildingInstance building : village.getBuildings()) {
            BuildingPlan plan;
            BuildingPlanSet.LevelDef levelDef;
            BuildingPlanSet planSet;
            BuildingInstance parent;
            BuildingId parentId;
            if (!building.isSubBuilding() || building.getStatus() != BuildingInstance.Status.PLANNED || building.getPlanSetId() == null || building.getVariant() == null || (parentId = building.getParentBuildingId()) != null && (parent = village.findBuildingById(parentId)) != null && parent.isBeingBuilt() || (planSet = ModCultures.getBuildingPlanSet(building.getPlanSetId())) == null || (levelDef = planSet.getLevel(building.getVariant(), building.getLevel())) == null || !VillageGrowthManager.hasResourcesForProject(level, village, levelDef) || (plan = ModCultures.getBuildingPlan(levelDef.planId())) == null) continue;
            ConstructionTask task = building.getConstructionTask();
            if (task == null) {
                List<PlacementStep> steps = BuildingPlacer.compilePlacementSteps(level, plan, building.getOrigin(), building.getRotation());
                if (steps.isEmpty()) {
                    VillageGrowthManager.completeImmediately(level, village, building, plan, planSet);
                    return true;
                }
                task = new ConstructionTask(steps, 0);
                building.setConstructionTask(task);
            }
            building.markUnderConstruction();
            village.recordEvent(level, "Sub-building started: " + planSet.id().getPath() + " (" + building.getVariant() + "L" + building.getLevel() + ")");
            village.recordChronicleEvent(level, VillageEventType.BUILDING_STARTED, planSet.nativeName(), null);
            LOGGER.info("[Millenaire] Planned sub-building {} activated", (Object)planSet.id().getPath());
            return true;
        }
        return false;
    }

    @Nullable
    private static VillageType.LayoutSlot findSlotForPlanSet(VillageType villageType, ResourceLocation planSetId) {
        for (VillageType.LayoutSlot slot : villageType.layout()) {
            if (!planSetId.equals((Object)slot.plan())) continue;
            return slot;
        }
        return null;
    }

    static boolean isCentreRole(String role) {
        return "centre".equals(role) || "townhall".equals(role);
    }

    static boolean isAutoExcludedInControlledVillage(boolean playerControlled, String role) {
        return playerControlled && CONTROLLED_AUTO_EXCLUDED_ROLES.contains(role);
    }

    static int computeLayoutSlotWeight(VillageType.LayoutSlot slot, BuildingPlanSet.LevelDef levelDef) {
        int basePriority = slot.hasPriorityOverride() ? slot.priority() : levelDef.priority();
        int tierMultiplier = TIER_MULTIPLIERS.getOrDefault(slot.role(), 1);
        return Math.max(1, basePriority * tierMultiplier);
    }

    static int countPriorLayoutSlots(VillageType villageType, VillageType.LayoutSlot targetSlot, ResourceLocation planSetId) {
        int count = 0;
        for (VillageType.LayoutSlot prev : villageType.layout()) {
            if (prev == targetSlot) break;
            BuildingPlanSet prevSet = ModCultures.getBuildingPlanSet(prev.plan());
            if (prevSet == null || !prevSet.id().equals((Object)planSetId)) continue;
            ++count;
        }
        return count;
    }

    static int countBuildingsOfType(Village village, ResourceLocation planSetId) {
        int count = 0;
        for (BuildingInstance b : village.getBuildings()) {
            if (!planSetId.equals((Object)b.getPlanSetId())) continue;
            ++count;
        }
        return count;
    }

    static boolean isPlayerBuildingCandidate(Village village, ResourceLocation planSetId) {
        if (!village.isBuildingBought(planSetId)) {
            return false;
        }
        return VillageGrowthManager.countBuildingsOfType(village, planSetId) == 0;
    }

    private static boolean hasResources(ServerLevel level, Village village, BuildingPlanSet.LevelDef levelDef) {
        return VillageGrowthManager.hasResources(level, village, levelDef, AffordabilityMode.RESERVATION_AWARE);
    }

    static boolean hasResourcesForProject(ServerLevel level, Village village, BuildingPlanSet.LevelDef levelDef) {
        return VillageGrowthManager.hasResources(level, village, levelDef, AffordabilityMode.RAW_COUNT);
    }

    private static boolean hasResources(ServerLevel level, Village village, BuildingPlanSet.LevelDef levelDef, AffordabilityMode mode) {
        if (((Boolean)MillenaireServerConfig.SERVER.ignoreResourceCost.get()).booleanValue()) {
            return true;
        }
        Map<ResourceLocation, Integer> required = levelDef.requiredResources();
        if (required.isEmpty()) {
            return true;
        }
        BuildingInstance townhall = village.getTownhall();
        if (townhall == null) {
            return false;
        }
        BuildingInventory inv = townhall.getInventory();
        if (inv == null) {
            return false;
        }
        inv.invalidateCache();
        for (Map.Entry<ResourceLocation, Integer> entry : required.entrySet()) {
            if (AnywoodHelper.isAnywood(entry.getKey())) continue;
            Item item = ItemHelper.resolve(entry.getKey());
            if (item == null) {
                LOGGER.warn("[Millenaire] Unknown resource: {}", (Object)entry.getKey());
                return false;
            }
            int available = mode == AffordabilityMode.RAW_COUNT ? inv.getCount((Level)level, item) : GoodAvailabilityHelper.nbGoodAvailable(townhall, item, level, village, village.getCultureId(), false, true);
            if (available >= entry.getValue()) continue;
            LOGGER.debug("[Growth] hasResources FAIL: {} need={} have={} (mode={})", new Object[]{entry.getKey(), entry.getValue(), available, mode});
            return false;
        }
        Integer anywoodNeeded = required.get(AnywoodHelper.ANYWOOD_LOG);
        if (anywoodNeeded != null && anywoodNeeded > 0) {
            int available;
            int rawCount = inv.getCountByTag((Level)level, AnywoodHelper.LOGS_TAG);
            if (mode != AffordabilityMode.RAW_COUNT) {
                rawCount -= GoodAvailabilityHelper.getAnywoodReservedQuantity(level, village, null);
            }
            if ((available = AnywoodHelper.anywoodAvailable(rawCount, required, key -> {
                Item logItem = ItemHelper.resolve(key);
                return logItem == null ? 0 : inv.getCount((Level)level, logItem);
            })) < anywoodNeeded) {
                LOGGER.debug("[Growth] hasResources FAIL: anywood need={} have={}", (Object)anywoodNeeded, (Object)available);
                return false;
            }
        }
        return true;
    }

    static boolean checkBuildConditions(Village village, BuildingInstance building, BuildingPlanSet.LevelDef levelDef) {
        for (String requiredTag : levelDef.requiredTags()) {
            if (building.hasRuntimeTag(requiredTag)) continue;
            return false;
        }
        for (String requiredParentTag : levelDef.requiredParentTags()) {
            if (building.getParentBuildingId() == null) {
                return false;
            }
            BuildingInstance parent = village.findBuildingById(building.getParentBuildingId());
            if (parent != null && parent.hasRuntimeTag(requiredParentTag)) continue;
            return false;
        }
        for (String forbiddenTag : levelDef.forbiddenTagsInVillage()) {
            if (!VillageGrowthManager.villageHasTag(village, forbiddenTag)) continue;
            return false;
        }
        for (String requiredVillageTag : levelDef.requiredVillageTags()) {
            if (VillageGrowthManager.villageHasTag(village, requiredVillageTag)) continue;
            return false;
        }
        return true;
    }

    private static boolean villageHasTag(Village village, String tag) {
        return !village.getBuildingsWithTag(tag).isEmpty();
    }

    @Nullable
    static <T> T weightedPick(List<T> candidates, List<Integer> weights, RandomGenerator rng) {
        if (candidates.isEmpty()) {
            return null;
        }
        if (candidates.size() == 1) {
            return candidates.getFirst();
        }
        int totalWeight = 0;
        for (int w : weights) {
            totalWeight += w;
        }
        if (totalWeight <= 0) {
            return null;
        }
        int roll = rng.nextInt(totalWeight);
        int cumulative = 0;
        for (int i = 0; i < candidates.size(); ++i) {
            if (roll >= (cumulative += weights.get(i).intValue())) continue;
            return candidates.get(i);
        }
        return candidates.getLast();
    }

    static BlockPos recalculateOriginForUpgrade(BuildingInstance target, BuildingPlan newPlan) {
        BlockPos currentOrigin = target.getOrigin();
        BuildingPlan currentPlan = ModCultures.getBuildingPlan(target.getPlanId());
        int oldGroundLevel = currentPlan != null ? currentPlan.groundLevel() : 0;
        int newGroundLevel = newPlan.groundLevel();
        if (newGroundLevel != oldGroundLevel) {
            int baseY = currentOrigin.getY() - oldGroundLevel;
            int newPlacementY = baseY + newGroundLevel;
            return new BlockPos(currentOrigin.getX(), newPlacementY, currentOrigin.getZ());
        }
        return currentOrigin;
    }

    @Nullable
    public static PlacedLocation findLocationForNewBuilding(ServerLevel level, Village village, BuildingPlanSet planSet, BuildingPlan plan, @Nullable VillageType.LayoutSlot slot) {
        VillageType vt = ModCultures.getVillageType(village.getVillageTypeId());
        int radius = vt != null ? vt.radius() : 90;
        VillageTerrainMap terrainMap = VillageTerrainMap.compute(level, village.getCenter(), radius);
        VillageGrowthManager.markExistingBuildingsOccupied(terrainMap, village);
        TerrainReachability reachability = TerrainReachability.compute(terrainMap, village.getCenter());
        PlacementConstraints constraints = PlacementConstraints.resolve(planSet, slot, radius);
        ClearMargins margins = planSet.clearMargins().atLeast(constraints.clearMargin());
        return BuildingLocationFinder.findLocation(terrainMap, plan, village.getCenter(), constraints, margins, new ArrayList<BuildingInstance>(village.getBuildings()), reachability);
    }

    public static BuildingLocationFinder.AnchorEvaluation validateLocationAt(ServerLevel level, Village village, BuildingPlanSet planSet, BuildingPlan plan, @Nullable VillageType.LayoutSlot slot, BlockPos clickedPos) {
        VillageType vt = ModCultures.getVillageType(village.getVillageTypeId());
        int radius = vt != null ? vt.radius() : 90;
        VillageTerrainMap terrainMap = VillageTerrainMap.compute(level, village.getCenter(), radius);
        VillageGrowthManager.markExistingBuildingsOccupied(terrainMap, village);
        TerrainReachability reachability = TerrainReachability.compute(terrainMap, village.getCenter());
        PlacementConstraints constraints = PlacementConstraints.resolve(planSet, slot, radius);
        ClearMargins margins = planSet.clearMargins().atLeast(constraints.clearMargin());
        return BuildingLocationFinder.evaluateAnchor(clickedPos.getX(), clickedPos.getZ(), terrainMap, plan, village.getCenter(), constraints, margins, new ArrayList<BuildingInstance>(village.getBuildings()), reachability);
    }

    static void markExistingBuildingsOccupied(VillageTerrainMap terrainMap, Village village) {
        for (BuildingInstance existing : village.getBuildings()) {
            BuildingPlan existingPlan;
            if (existing.isSubBuilding() || (existingPlan = ModCultures.getBuildingPlan(existing.getPlanId())) == null) continue;
            int surfaceY = existing.getOrigin().getY() - existingPlan.groundLevel();
            BuildingPlanSet existingPlanSet = ModCultures.getBuildingPlanSet(existing.getPlanSetId());
            ClearMargins margins = existingPlanSet != null ? existingPlanSet.clearMargins().atLeast(PlacementConstraints.getDefaultClearMargin()) : ClearMargins.symmetric(PlacementConstraints.getDefaultClearMargin());
            terrainMap.markBuildingFootprint(existing.getOrigin().getX(), existing.getOrigin().getZ(), existingPlan.width(), existingPlan.depth(), margins, existing.getRotation(), surfaceY);
            if (!existing.isWallSegment() || existingPlanSet == null) continue;
            terrainMap.markOccupied(existing.getOrigin().getX(), existing.getOrigin().getZ(), existingPlan.width(), existingPlan.depth(), existingPlanSet.clearMargins(), existing.getRotation());
        }
    }

    static boolean isOnPlacementCooldown(Village village, ResourceLocation planSetId, long currentTick) {
        Long until = village.getPlacementCooldowns().get(planSetId);
        return until != null && currentTick < until;
    }

    private static void addPlacementCooldown(Village village, ResourceLocation planSetId, long currentTick) {
        village.addPlacementCooldown(planSetId, currentTick + 1600L);
    }

    static void initBrickColours(BuildingInstance instance, BuildingPlanSet planSet, Village village, RandomSource random) {
        if (!planSet.randomBrickColours().isEmpty()) {
            instance.initBrickColoursFromPlan(planSet.randomBrickColours(), random);
        } else if (village.getBrickTheme() != null) {
            instance.initBrickColours(village.getBrickTheme(), random);
        }
    }

    static List<PlacementStep> deduplicateTerrainSteps(List<PlacementStep> terrainSteps, List<PlacementStep> buildingSteps, int groundLevel) {
        HashSet<BlockPos> buildingPositions = new HashSet<BlockPos>(buildingSteps.size());
        for (PlacementStep step : buildingSteps) {
            buildingPositions.add(step.relativePos());
        }
        int naturalSurfaceY = -groundLevel;
        ArrayList<PlacementStep> result = new ArrayList<PlacementStep>(terrainSteps.size());
        for (PlacementStep step : terrainSteps) {
            if (!buildingPositions.contains(step.relativePos())) {
                result.add(step);
                continue;
            }
            if (step.relativePos().getY() < naturalSurfaceY) continue;
            result.add(step);
        }
        return result;
    }

    record GrowthCandidate(BuildingPlanSet planSet, BuildingPlanSet.LevelDef levelDef, String variant, int weight, boolean isUpgrade, @Nullable BuildingInstance existingBuilding, @Nullable VillageType.LayoutSlot slot) {
    }

    public static final class RushDispersion {
        private static final int AREA_PER_COOLDOWN_TICK = 60;
        private final Map<BuildingId, Integer> cooldowns = new HashMap<BuildingId, Integer>();

        boolean isBlocked(BuildingId id) {
            return this.cooldowns.getOrDefault(id, 0) > 0;
        }

        boolean hasActiveCooldowns() {
            return !this.cooldowns.isEmpty();
        }

        void tick() {
            this.cooldowns.replaceAll((id, remaining) -> remaining - 1);
            this.cooldowns.values().removeIf(remaining -> remaining <= 0);
        }

        void occupy(BuildingId id, int width, int depth) {
            int area = Math.max(1, width * depth);
            int ticks = Math.max(1, Math.round((float)area / 60.0f));
            this.cooldowns.put(id, ticks);
        }
    }

    static enum AffordabilityMode {
        RAW_COUNT,
        RESERVATION_AWARE;

    }
}

