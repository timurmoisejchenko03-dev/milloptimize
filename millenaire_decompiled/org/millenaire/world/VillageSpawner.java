/*
 * Decompiled with CFR 0.150.
 * 
 * Could not load the following classes:
 *  com.mojang.logging.LogUtils
 *  javax.annotation.Nullable
 *  net.minecraft.core.BlockPos
 *  net.minecraft.core.Holder
 *  net.minecraft.core.Vec3i
 *  net.minecraft.network.chat.Component
 *  net.minecraft.resources.ResourceLocation
 *  net.minecraft.server.level.ServerLevel
 *  net.minecraft.server.level.ServerPlayer
 *  net.minecraft.tags.TagKey
 *  net.minecraft.util.RandomSource
 *  net.minecraft.world.item.ItemStack
 *  net.minecraft.world.level.ChunkPos
 *  net.minecraft.world.level.Level
 *  net.minecraft.world.level.biome.Biome
 *  net.minecraft.world.level.block.Blocks
 *  net.minecraft.world.level.block.Rotation
 *  net.minecraft.world.level.block.state.BlockState
 *  net.minecraft.world.level.chunk.LevelChunk
 *  net.minecraft.world.level.levelgen.Heightmap$Types
 *  net.neoforged.neoforge.common.Tags$Biomes
 *  org.jetbrains.annotations.Nullable
 *  org.slf4j.Logger
 */
package org.millenaire.world;

import com.mojang.logging.LogUtils;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.Vec3i;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.TagKey;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.level.levelgen.Heightmap;
import net.neoforged.neoforge.common.Tags;
import org.jetbrains.annotations.Nullable;
import org.millenaire.building.BuildingId;
import org.millenaire.building.BuildingInstance;
import org.millenaire.building.BuildingPlan;
import org.millenaire.building.BuildingPlanSet;
import org.millenaire.building.ClearMargins;
import org.millenaire.config.MillenaireServerConfig;
import org.millenaire.culture.ModCultures;
import org.millenaire.culture.NameLists;
import org.millenaire.culture.VillageType;
import org.millenaire.culture.WallType;
import org.millenaire.village.BrickColourTheme;
import org.millenaire.village.BuildingFinalizer;
import org.millenaire.village.SubBuildingHelper;
import org.millenaire.village.Village;
import org.millenaire.village.VillageBookService;
import org.millenaire.village.VillageChunkLoader;
import org.millenaire.village.VillageEventType;
import org.millenaire.village.VillageGrowthManager;
import org.millenaire.village.VillageId;
import org.millenaire.village.VillageManager;
import org.millenaire.village.VillageReputation;
import org.millenaire.village.VillageSavedData;
import org.millenaire.village.WallGrowthManager;
import org.millenaire.world.BuildingLocationFinder;
import org.millenaire.world.BuildingPlacer;
import org.millenaire.world.PlacedLocation;
import org.millenaire.world.PlacementConstraints;
import org.millenaire.world.TerrainPreparer;
import org.millenaire.world.TerrainReachability;
import org.millenaire.world.VillageNotifier;
import org.millenaire.world.VillageTerrainMap;
import org.millenaire.world.VillageWallGenerator;
import org.slf4j.Logger;

public final class VillageSpawner {
    private static final Logger LOGGER = LogUtils.getLogger();
    private static final Random RANDOM = new Random();
    private static final int HAMLET_ATTEMPT_ANGLE_STEPS = 36;
    private static final int HAMLET_MIN_DISTANCE = 250;
    private static final int HAMLET_MAX_DISTANCE = 350;
    private static final int HAMLET_RADIUS_STEP = 50;

    private VillageSpawner() {
    }

    @Nullable
    public static Component spawnVillage(ServerLevel level, BlockPos center, VillageType villageType) {
        return VillageSpawner.spawnVillage(level, center, villageType, 0, null, null, null);
    }

    @Nullable
    public static Component spawnVillage(ServerLevel level, BlockPos center, VillageType villageType, int completion) {
        return VillageSpawner.spawnVillage(level, center, villageType, completion, null, null, null);
    }

    @Nullable
    public static Component spawnVillage(ServerLevel level, BlockPos center, VillageType villageType, int completion, @javax.annotation.Nullable String parentBaseName, @javax.annotation.Nullable VillageId parentVillageId) {
        return VillageSpawner.spawnVillage(level, center, villageType, completion, parentBaseName, parentVillageId, null);
    }

    @Nullable
    public static Component validateSite(ServerLevel level, BlockPos center, VillageType villageType) {
        ValidationResult result = VillageSpawner.dryRunPlacement(level, center, villageType);
        return result.error;
    }

    private static ValidationResult dryRunPlacement(ServerLevel level, BlockPos center, VillageType villageType) {
        BuildingPlanSet centrePlanSetForMargins;
        ResourceLocation villageTypeId = villageType.id();
        List<SlotWithPlan> resolvedSlots = VillageSpawner.resolveSlots(villageType);
        if (resolvedSlots.isEmpty()) {
            LOGGER.error("[Millenaire] No valid slots in village type {}", (Object)villageTypeId);
            return ValidationResult.fail((Component)Component.translatable((String)"millenaire.spawn.error.no_valid_slots", (Object[])new Object[]{villageTypeId.getPath()}));
        }
        SlotWithPlan centreSlot = null;
        ArrayList<SlotWithPlan> otherSlots = new ArrayList<SlotWithPlan>();
        for (SlotWithPlan s2 : resolvedSlots) {
            if (VillageSpawner.isCentreRole(s2.slot.role())) {
                centreSlot = s2;
                continue;
            }
            otherSlots.add(s2);
        }
        if (centreSlot == null) {
            LOGGER.error("[Millenaire] No center slot in village type {}", (Object)villageTypeId);
            return ValidationResult.fail((Component)Component.translatable((String)"millenaire.spawn.error.no_centre", (Object[])new Object[]{villageTypeId.getPath()}));
        }
        ArrayList<SlotWithPlan> slotsToPlace = new ArrayList<SlotWithPlan>(otherSlots.stream().filter(s -> VillageSpawner.isStartRole(s.slot.role())).toList());
        Collections.shuffle(slotsToPlace, RANDOM);
        BlockPos townhallRequestedOrigin = center;
        Rotation townhallRotation = Rotation.NONE;
        if (centreSlot.slot.hasLegacyOffset()) {
            townhallRequestedOrigin = center.offset((Vec3i)centreSlot.slot.offset());
            townhallRotation = centreSlot.slot.rotation();
        }
        ClearMargins centreClearMargins = ClearMargins.symmetric(PlacementConstraints.getDefaultClearMargin());
        if (centreSlot.planSetId != null && (centrePlanSetForMargins = ModCultures.getBuildingPlanSet(centreSlot.planSetId)) != null) {
            centreClearMargins = centrePlanSetForMargins.clearMargins();
        }
        int effWidth = TerrainPreparer.effectiveWidth(centreSlot.plan.width(), centreSlot.plan.depth(), townhallRotation);
        int effDepth = TerrainPreparer.effectiveDepth(centreSlot.plan.width(), centreSlot.plan.depth(), townhallRotation);
        int centerEstimateY = TerrainPreparer.computeAverageSurfaceHeight(level, townhallRequestedOrigin, effWidth, effDepth, centreClearMargins.forRotation(townhallRotation));
        VillageTerrainMap.FootprintRect thRect = VillageTerrainMap.computeFootprintRect(townhallRequestedOrigin.getX(), townhallRequestedOrigin.getZ(), centreSlot.plan.width(), centreSlot.plan.depth(), ClearMargins.symmetric(0), townhallRotation);
        int thCenterX = thRect.startX() + thRect.width() / 2;
        int thCenterZ = thRect.startZ() + thRect.depth() / 2;
        BlockPos centerAtGround = new BlockPos(thCenterX, centerEstimateY, thCenterZ);
        VillageTerrainMap terrainMap = VillageTerrainMap.compute(level, centerAtGround, villageType.radius());
        int townhallBaseY = terrainMap.computeAverageAltitude(townhallRequestedOrigin.getX(), townhallRequestedOrigin.getZ(), centreSlot.plan.width(), centreSlot.plan.depth(), centreClearMargins, townhallRotation);
        terrainMap.markBuildingFootprint(townhallRequestedOrigin.getX(), townhallRequestedOrigin.getZ(), centreSlot.plan.width(), centreSlot.plan.depth(), centreClearMargins, townhallRotation, townhallBaseY);
        TerrainReachability reachability = TerrainReachability.compute(terrainMap, centerAtGround);
        ArrayList<PrecomputedWalls> precomputedWalls = new ArrayList<PrecomputedWalls>();
        if (!villageType.playerControlled()) {
            VillageWallGenerator.WallLocationResult outerResult;
            WallType outerWall;
            ResourceLocation outerWallTypeId;
            VillageWallGenerator.WallLocationResult innerResult;
            WallType innerWall;
            VillageWallGenerator wallGenerator = new VillageWallGenerator(level);
            ResourceLocation innerWallTypeId = villageType.innerWallType();
            if (innerWallTypeId != null && (innerWall = ModCultures.getWallType(innerWallTypeId)) != null && !(innerResult = wallGenerator.computeWallBuildingLocations(villageType, innerWall, villageType.innerWallRadius(), terrainMap, reachability, centerAtGround)).segments().isEmpty()) {
                precomputedWalls.add(new PrecomputedWalls(innerWall, innerResult));
                VillageSpawner.markWallFootprints(terrainMap, innerWall, innerResult);
                reachability = TerrainReachability.compute(terrainMap, centerAtGround);
            }
            if ((outerWallTypeId = villageType.outerWallType()) != null && (outerWall = ModCultures.getWallType(outerWallTypeId)) != null && !(outerResult = wallGenerator.computeWallBuildingLocations(villageType, outerWall, 0, terrainMap, reachability, centerAtGround)).segments().isEmpty()) {
                precomputedWalls.add(new PrecomputedWalls(outerWall, outerResult));
                VillageSpawner.markWallFootprints(terrainMap, outerWall, outerResult);
                reachability = TerrainReachability.compute(terrainMap, centerAtGround);
            }
        }
        ArrayList<PlacedBuilding> placedBuildings = new ArrayList<PlacedBuilding>();
        ArrayList<BuildingInstance> existingBuildings = new ArrayList<BuildingInstance>();
        int townhallPlacementY = townhallBaseY + centreSlot.plan.groundLevel();
        existingBuildings.add(new BuildingInstance(BuildingId.random(), centreSlot.plan.id(), new BlockPos(townhallRequestedOrigin.getX(), townhallPlacementY, townhallRequestedOrigin.getZ()), townhallRotation, BuildingInstance.Status.PLANNED));
        for (SlotWithPlan swp : slotsToPlace) {
            BuildingPlanSet slotPlanSet;
            Rotation rotation;
            BlockPos requestedOrigin;
            BuildingPlan plan = swp.plan;
            VillageType.LayoutSlot slot = swp.slot;
            if (slot.hasLegacyOffset()) {
                requestedOrigin = center.offset((Vec3i)slot.offset());
                rotation = slot.rotation();
            } else {
                PlacedLocation location;
                BuildingPlanSet spawnPlanSet = swp.planSetId != null ? ModCultures.getBuildingPlanSet(swp.planSetId) : null;
                PlacementConstraints constraints = spawnPlanSet != null ? PlacementConstraints.resolve(spawnPlanSet, slot, villageType.radius()) : new PlacementConstraints(5, 60, slot.farFromTags(), slot.closeToTags(), slot.clearMargin(), slot.fixedOrientation());
                ClearMargins searchMargins = ClearMargins.symmetric(PlacementConstraints.getDefaultClearMargin());
                if (spawnPlanSet != null) {
                    searchMargins = spawnPlanSet.clearMargins().atLeast(slot.clearMargin());
                }
                if ((location = BuildingLocationFinder.findLocation(terrainMap, plan, centerAtGround, constraints, searchMargins, existingBuildings, reachability)) == null) {
                    if (VillageSpawner.isStartRole(slot.role())) {
                        LOGGER.warn("[Millenaire] No location found for start building '{}' \u2014 village rejected", (Object)swp.planSetId);
                        return ValidationResult.fail((Component)Component.translatable((String)"millenaire.spawn.error.no_terrain", (Object[])new Object[]{String.valueOf((Object)swp.planSetId)}));
                    }
                    LOGGER.warn("[Millenaire] No location found for '{}', skipped", (Object)swp.planSetId);
                    continue;
                }
                requestedOrigin = location.position();
                rotation = location.rotation();
            }
            ClearMargins slotMargins = ClearMargins.symmetric(PlacementConstraints.getDefaultClearMargin());
            if (swp.planSetId != null && (slotPlanSet = ModCultures.getBuildingPlanSet(swp.planSetId)) != null) {
                slotMargins = slotPlanSet.clearMargins().atLeast(slot.clearMargin());
            }
            int baseY = terrainMap.computeAverageAltitude(requestedOrigin.getX(), requestedOrigin.getZ(), plan.width(), plan.depth(), slotMargins, rotation);
            int placementY = baseY + plan.groundLevel();
            BlockPos adjustedOrigin = new BlockPos(requestedOrigin.getX(), placementY, requestedOrigin.getZ());
            terrainMap.markBuildingFootprint(requestedOrigin.getX(), requestedOrigin.getZ(), plan.width(), plan.depth(), slotMargins, rotation, baseY);
            reachability = TerrainReachability.compute(terrainMap, centerAtGround);
            BuildingInstance tempInstance = new BuildingInstance(BuildingId.random(), plan.id(), adjustedOrigin, rotation, BuildingInstance.Status.PLANNED);
            existingBuildings.add(tempInstance);
            placedBuildings.add(new PlacedBuilding(swp, adjustedOrigin, rotation, requestedOrigin, baseY));
        }
        return new ValidationResult(null, centreSlot, placedBuildings, townhallRequestedOrigin, townhallRotation, centreClearMargins, townhallBaseY, centerAtGround, precomputedWalls);
    }

    @Nullable
    public static Component spawnVillage(ServerLevel level, BlockPos center, VillageType villageType, int completion, @javax.annotation.Nullable String parentBaseName, @javax.annotation.Nullable VillageId parentVillageId, @javax.annotation.Nullable ServerPlayer controller) {
        BuildingPlanSet centrePlanSet;
        BuildingInstance centreBuilding;
        BuildingPlanSet townhallPlanSet;
        ValidationResult validation = VillageSpawner.dryRunPlacement(level, center, villageType);
        if (validation.error != null) {
            return validation.error;
        }
        SlotWithPlan centreSlot = validation.centreSlot;
        List<PlacedBuilding> placedBuildings = validation.placedBuildings;
        BlockPos townhallRequestedOrigin = validation.townhallRequestedOrigin;
        Rotation townhallRotation = validation.townhallRotation;
        ClearMargins centreClearMargins = validation.centreClearMargins;
        int townhallBaseY = validation.townhallBaseY;
        int townhallPlacementY = townhallBaseY + centreSlot.plan.groundLevel();
        BlockPos centerAtGround = validation.centerAtGround;
        ResourceLocation villageTypeId = villageType.id();
        boolean[][] townhallSnow = TerrainPreparer.checkForSnow(level, townhallRequestedOrigin, centreSlot.plan.width(), centreSlot.plan.depth(), townhallRotation, centreClearMargins);
        TerrainPreparer.clearAndFlattenAtY(level, townhallRequestedOrigin, centreSlot.plan.width(), centreSlot.plan.height(), centreSlot.plan.depth(), townhallRotation, centreSlot.plan.groundLevel(), townhallBaseY, centreClearMargins);
        TerrainPreparer.decayOrphanedLeaves(level, townhallRequestedOrigin, centreSlot.plan.width(), centreSlot.plan.depth(), townhallRotation, townhallBaseY, centreClearMargins);
        BlockPos townhallOrigin = new BlockPos(townhallRequestedOrigin.getX(), townhallPlacementY, townhallRequestedOrigin.getZ());
        VillageId villageId = VillageId.random();
        Village village = new Village(villageId, villageType.culture(), villageTypeId, centerAtGround);
        if (parentBaseName != null) {
            String qualifier = VillageSpawner.pickQualifier(level, centerAtGround, villageType);
            String villageName = qualifier != null ? parentBaseName + " " + qualifier : parentBaseName;
            village.setVillageName(villageName);
        } else {
            String nameListKey = villageType.nameList();
            if (nameListKey != null) {
                Object villageName;
                NameLists nameLists = ModCultures.getNameLists(villageType.culture());
                if (nameLists != null && (villageName = nameLists.randomFrom(nameListKey)) != null) {
                    String qualifier = VillageSpawner.pickQualifier(level, centerAtGround, villageType);
                    if (qualifier != null) {
                        villageName = (String)villageName + " " + qualifier;
                    }
                    village.setVillageName((String)villageName);
                }
            } else {
                village.setVillageName(villageType.name());
            }
        }
        if (village.getVillageName() == null) {
            LOGGER.warn("Village name was null after naming logic for type {} \u2014 falling back to type name", (Object)villageTypeId);
            village.setVillageName(villageType.name() != null ? villageType.name() : villageTypeId.getPath());
        }
        if (!villageType.brickColourThemes().isEmpty()) {
            BrickColourTheme chosen = VillageSpawner.pickWeightedTheme(villageType.brickColourThemes(), level.getRandom());
            village.setBrickTheme(chosen);
            LOGGER.debug("Brick theme '{}' chosen for village {}", (Object)chosen.name(), (Object)village.getVillageName());
        }
        if (!villageType.bannerJsons().isEmpty()) {
            List<String> pool = villageType.bannerJsons();
            String pick = pool.get(level.getRandom().nextInt(pool.size()));
            village.setBannerNbt(pick);
        }
        BuildingId buildingId = BuildingId.random();
        BuildingInstance instance = new BuildingInstance(buildingId, centreSlot.plan.id(), townhallOrigin, townhallRotation, BuildingInstance.Status.COMPLETE, centreSlot.planSetId, centreSlot.variant, 0);
        if (village.getBrickTheme() != null) {
            instance.initBrickColours(village.getBrickTheme(), level.getRandom());
        }
        BuildingPlacer.placeInstantly(level, centreSlot.plan, townhallOrigin, townhallRotation, instance);
        TerrainPreparer.restoreSnow(level, townhallRequestedOrigin, centreSlot.plan.width(), centreSlot.plan.depth(), townhallRotation, townhallSnow, centreClearMargins);
        BuildingFinalizer.applyPostPlacement(level, village, instance, centreSlot.plan);
        ItemStack scroll = VillageBookService.createScrollForVillage(village);
        if (instance.getInventory() != null) {
            instance.getInventory().addStack((Level)level, scroll);
        } else {
            LOGGER.warn("Unable to place scroll: townhall without chests (village {})", (Object)village.getVillageName());
        }
        village.addBuilding(instance);
        BuildingFinalizer.applyCompletionEffects(level, village, instance);
        if (centreSlot.planSetId != null && (townhallPlanSet = ModCultures.getBuildingPlanSet(centreSlot.planSetId)) != null) {
            SubBuildingHelper.spawnStartingSubBuildings(level, village, townhallPlanSet, instance, true);
            BuildingPlanSet.LevelDef thLevel0 = townhallPlanSet.getLevel(centreSlot.variant, 0);
            if (thLevel0 != null) {
                SubBuildingHelper.spawnUpgradeSubBuildings(level, village, townhallPlanSet, thLevel0, instance, false);
            }
        }
        if (!villageType.playerControlled() && !validation.precomputedWalls.isEmpty()) {
            Iterator<BuildingInstance> wallsTerrainMap = VillageTerrainMap.compute(level, centerAtGround, villageType.radius());
            for (BuildingInstance existing : village.getBuildings()) {
                BuildingPlanSet wallsPlanSet;
                BuildingPlan ep;
                if (existing.isSubBuilding() || (ep = ModCultures.getBuildingPlan(existing.getPlanId())) == null) continue;
                ClearMargins wallsMargins = ClearMargins.symmetric(PlacementConstraints.getDefaultClearMargin());
                if (existing.getPlanSetId() != null && (wallsPlanSet = ModCultures.getBuildingPlanSet(existing.getPlanSetId())) != null) {
                    wallsMargins = wallsPlanSet.clearMargins();
                }
                int surfaceY = existing.getOrigin().getY() - ep.groundLevel();
                ((VillageTerrainMap)((Object)wallsTerrainMap)).markBuildingFootprint(existing.getOrigin().getX(), existing.getOrigin().getZ(), ep.width(), ep.depth(), wallsMargins, existing.getRotation(), surfaceY);
            }
            VillageSpawner.spawnWalls(level, village, villageType, centerAtGround, wallsTerrainMap, validation.precomputedWalls);
        }
        for (PlacedBuilding pb : placedBuildings) {
            Object starterPlanSet;
            BuildingPlanSet starterPlanSetForMargins;
            BuildingPlan plan = pb.swp.plan;
            BuildingId buildingId2 = BuildingId.random();
            BuildingInstance instance2 = new BuildingInstance(buildingId2, plan.id(), pb.origin, pb.rotation, BuildingInstance.Status.COMPLETE, pb.swp.planSetId, pb.swp.variant, 0);
            if (village.getBrickTheme() != null) {
                instance2.initBrickColours(village.getBrickTheme(), level.getRandom());
            }
            ClearMargins starterMargins = ClearMargins.symmetric(PlacementConstraints.getDefaultClearMargin());
            if (pb.swp.planSetId != null && (starterPlanSetForMargins = ModCultures.getBuildingPlanSet(pb.swp.planSetId)) != null) {
                starterMargins = starterPlanSetForMargins.clearMargins();
            }
            boolean[][] starterSnow = TerrainPreparer.checkForSnow(level, pb.requestedOrigin, plan.width(), plan.depth(), pb.rotation, starterMargins);
            TerrainPreparer.clearAndFlattenAtY(level, pb.requestedOrigin, plan.width(), plan.height(), plan.depth(), pb.rotation, plan.groundLevel(), pb.baseY, starterMargins);
            TerrainPreparer.decayOrphanedLeaves(level, pb.requestedOrigin, plan.width(), plan.depth(), pb.rotation, pb.baseY, starterMargins);
            BuildingPlacer.placeInstantly(level, plan, pb.origin, pb.rotation, instance2);
            TerrainPreparer.restoreSnow(level, pb.requestedOrigin, plan.width(), plan.depth(), pb.rotation, starterSnow, starterMargins);
            BuildingFinalizer.applyPostPlacement(level, village, instance2, plan);
            village.addBuilding(instance2);
            BuildingFinalizer.applyCompletionEffects(level, village, instance2);
            if (pb.swp.planSetId == null || (starterPlanSet = ModCultures.getBuildingPlanSet(pb.swp.planSetId)) == null) continue;
            SubBuildingHelper.spawnStartingSubBuildings(level, village, (BuildingPlanSet)starterPlanSet, instance2, true);
            BuildingPlanSet.LevelDef starterLevel0 = ((BuildingPlanSet)starterPlanSet).getLevel(pb.swp.variant, 0);
            if (starterLevel0 == null) continue;
            SubBuildingHelper.spawnUpgradeSubBuildings(level, village, (BuildingPlanSet)starterPlanSet, starterLevel0, instance2, false);
        }
        for (BuildingInstance building : village.getBuildings()) {
            BuildingPlanSet bps;
            ResourceLocation planSetId;
            if (building.isSubBuilding() || (planSetId = building.getPlanSetId()) == null || (bps = ModCultures.getBuildingPlanSet(planSetId)) == null) continue;
            VillageGrowthManager.spawnBuildingOccupants(level, village, bps, building);
        }
        BuildingInstance buildingInstance = centreBuilding = village.getBuildings().isEmpty() ? null : village.getBuildings().get(0);
        if (centreBuilding != null && centreBuilding.getPlanSetId() != null && (centrePlanSet = ModCultures.getBuildingPlanSet(centreBuilding.getPlanSetId())) != null && !centrePlanSet.startingGoods().isEmpty()) {
            village.fillStartingGoods(level, centreBuilding, centrePlanSet, true);
            village.setLastGoodsRefresh(level.getGameTime());
        }
        VillageSavedData savedData = VillageSavedData.get(level);
        VillageManager villageManager = savedData.getVillageManager();
        villageManager.addVillage(village);
        savedData.setDirty();
        if (parentVillageId != null) {
            village.setParentVillageId(parentVillageId);
        }
        VillageSpawner.initializeRelations(level, village, villageManager);
        if (villageType.playerControlled() && controller != null) {
            village.setOwner(controller.getUUID(), controller.getName().getString());
            VillageReputation rep = village.getReputation();
            rep.add(controller.getUUID(), 20000);
            for (Village other : villageManager.getAllVillages()) {
                if (other == village || !controller.getUUID().equals(other.getOwnerUUID())) continue;
                village.setRelation(other.getId(), 100);
                other.setRelation(village.getId(), 100);
            }
        }
        Set<ChunkPos> chunks = village.computeVillageChunks();
        VillageChunkLoader.forceVillageChunks(level, village.getCenter(), chunks);
        village.setLoadedChunks(chunks);
        village.setChunksForceLoaded(true);
        village.rebuildWaypointGraph(level);
        if (completion > 0 && !villageType.playerControlled()) {
            int wallUpgradePasses;
            double wallPlacementRatio;
            int wallRushed;
            int totalProjects = VillageGrowthManager.countTotalProjects(villageType);
            int target = totalProjects * completion / 100;
            VillageTerrainMap rushTerrainMap = VillageTerrainMap.compute(level, centerAtGround, villageType.radius());
            for (BuildingInstance existing : village.getBuildings()) {
                BuildingPlanSet rushPlanSet;
                BuildingPlan ep;
                if (existing.isSubBuilding() || (ep = ModCultures.getBuildingPlan(existing.getPlanId())) == null) continue;
                ClearMargins rushMargins = ClearMargins.symmetric(PlacementConstraints.getDefaultClearMargin());
                if (existing.getPlanSetId() != null && (rushPlanSet = ModCultures.getBuildingPlanSet(existing.getPlanSetId())) != null) {
                    rushMargins = rushPlanSet.clearMargins();
                }
                int surfaceY = existing.getOrigin().getY() - ep.groundLevel();
                rushTerrainMap.markBuildingFootprint(existing.getOrigin().getX(), existing.getOrigin().getZ(), ep.width(), ep.depth(), rushMargins, existing.getRotation(), surfaceY);
                if (!existing.isWallSegment()) continue;
                rushTerrainMap.markOccupied(existing.getOrigin().getX(), existing.getOrigin().getZ(), ep.width(), ep.depth(), rushMargins, existing.getRotation());
            }
            TerrainReachability rushReachability = TerrainReachability.compute(rushTerrainMap, centerAtGround);
            int rushed = 0;
            int consecutiveFailures = 0;
            HashSet<ResourceLocation> rushExcluded = new HashSet<ResourceLocation>();
            VillageGrowthManager.RushDispersion rushDispersion = new VillageGrowthManager.RushDispersion();
            int maxIterations = completion == 100 ? target + 200 : target;
            for (int i = 0; i < maxIterations; ++i) {
                boolean progress = VillageGrowthManager.rushOneProject(level, village, rushTerrainMap, rushExcluded, rushReachability, rushDispersion);
                if (progress) {
                    ++rushed;
                    consecutiveFailures = 0;
                    rushReachability = TerrainReachability.compute(rushTerrainMap, centerAtGround);
                    continue;
                }
                if (++consecutiveFailures >= 5) break;
            }
            if (rushed > 0) {
                savedData.setDirty();
                LOGGER.info("[Millenaire] Rush completion: {}/{} projects completed (target={}%)", new Object[]{rushed, totalProjects, completion});
            }
            if ((wallRushed = WallGrowthManager.rush(level, village, wallPlacementRatio = (double)completion / 100.0, wallUpgradePasses = completion >= 100 ? 5 : completion / 25)) > 0) {
                savedData.setDirty();
                LOGGER.info("[Millenaire] Rush walls: {} segments (upgrade passes={})", (Object)wallRushed, (Object)wallUpgradePasses);
            }
        }
        village.updatePens(level, true);
        village.getPathManager().recalculatePaths(level, village, true);
        if (completion > 0) {
            BuildingFinalizer.applyVillageUpdates(level, village);
        } else {
            village.sendFireplacePositions(level);
        }
        VillageNotifier.notifySpawn(level, center, village.getVillageName(), villageType);
        village.recordEvent(level, "Village founded (type: " + villageTypeId.getPath() + ", completion: " + completion + "%)");
        village.recordChronicleEvent(level, VillageEventType.FOUNDED, villageType.name(), null);
        LOGGER.info("[Millenaire] Village {} spawned: {} buildings, completion={}%", new Object[]{villageId.uuid().toString().substring(0, 8), village.getBuildings().size(), completion});
        if (!villageType.hamlets().isEmpty()) {
            String baseName = VillageSpawner.extractBaseName(village.getVillageName(), villageType);
            VillageSpawner.generateHamlets(level, village, villageManager, villageType, baseName, completion);
        }
        return null;
    }

    private static void generateHamlets(ServerLevel level, Village parent, VillageManager vm, VillageType parentType, String baseName, int parentCompletion) {
        List<ResourceLocation> hamletTypes = parentType.hamlets();
        int hamletCount = hamletTypes.size();
        double baseAngle = 0.06283185307179587 * (double)RANDOM.nextInt(100);
        for (int hamletIdx = 0; hamletIdx < hamletCount; ++hamletIdx) {
            ResourceLocation hamletTypeId = hamletTypes.get(hamletIdx);
            VillageType hamletType = ModCultures.getVillageType(hamletTypeId);
            if (hamletType == null) {
                LOGGER.warn("[Millenaire] Hamlet type not found: {}", (Object)hamletTypeId);
                continue;
            }
            boolean placed = false;
            double sectorOffset = (double)hamletIdx * (Math.PI * 2 / (double)hamletCount);
            for (int minRadius = 250; minRadius < 350 && !placed; minRadius += 50) {
                double startAngle = baseAngle + sectorOffset;
                for (int step = 0; step < 36 && !placed; ++step) {
                    Component failure;
                    BlockPos surfacePos;
                    int surfaceY;
                    LevelChunk chunk;
                    double angle = startAngle + (double)(step + 1) * 0.17453292519943295;
                    int radius = minRadius + RANDOM.nextInt(40);
                    int dx = (int)(Math.cos(angle) * (double)radius);
                    int dz = (int)(Math.sin(angle) * (double)radius);
                    BlockPos candidate = parent.getCenter().offset(dx, 0, dz);
                    if (!VillageSpawner.isBiomeValidForHamlet(level, candidate, hamletType) || (chunk = level.getChunk(candidate.getX() >> 4, candidate.getZ() >> 4)) == null || (surfaceY = level.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, candidate.getX(), candidate.getZ())) <= level.getMinBuildHeight() || vm.isWithinMinDistance(surfacePos = new BlockPos(candidate.getX(), surfaceY, candidate.getZ()), 100.0) || (failure = VillageSpawner.spawnVillage(level, surfacePos, hamletType, parentCompletion, baseName, parent.getId())) != null) continue;
                    placed = true;
                    LOGGER.info("[Millenaire] Hamlet {} spawned at {} for parent {}", new Object[]{hamletTypeId, surfacePos.toShortString(), parent.getVillageName()});
                }
            }
            if (placed) continue;
            LOGGER.warn("[Millenaire] Failed to place hamlet {} for parent {}", (Object)hamletTypeId, (Object)parent.getVillageName());
        }
    }

    private static boolean isBiomeValidForHamlet(ServerLevel level, BlockPos pos, VillageType hamletType) {
        if (hamletType.biomeTags().isEmpty()) {
            return true;
        }
        int validCount = 0;
        int totalCount = 0;
        int biomeSampleY = level.getMaxBuildHeight() - 1;
        for (int gx = -hamletType.radius(); gx <= hamletType.radius(); gx += 16) {
            block1: for (int gz = -hamletType.radius(); gz <= hamletType.radius(); gz += 16) {
                ++totalCount;
                BlockPos samplePos = new BlockPos(pos.getX() + gx, biomeSampleY, pos.getZ() + gz);
                Holder sampleBiome = level.getBiome(samplePos);
                for (TagKey<Biome> tag : hamletType.biomeTags()) {
                    if (!sampleBiome.is(tag)) continue;
                    ++validCount;
                    continue block1;
                }
            }
        }
        float validPerc = (float)validCount / (float)totalCount;
        return validPerc >= hamletType.minimumBiomeValidity();
    }

    static String extractBaseName(String fullName, VillageType villageType) {
        if (fullName == null) {
            return null;
        }
        String[] terrainQualifiers = new String[]{villageType.forestQualifier(), villageType.hillQualifier(), villageType.mountainQualifier(), villageType.desertQualifier(), villageType.lavaQualifier(), villageType.lakeQualifier(), villageType.oceanQualifier()};
        for (String terrainQ : terrainQualifiers) {
            String suffix;
            if (terrainQ == null || !fullName.endsWith(suffix = " " + terrainQ)) continue;
            return fullName.substring(0, fullName.length() - suffix.length());
        }
        for (String qualifier : villageType.qualifiers()) {
            String suffix = " " + qualifier;
            if (!fullName.endsWith(suffix)) continue;
            return fullName.substring(0, fullName.length() - suffix.length());
        }
        return fullName;
    }

    private static void initializeRelations(ServerLevel level, Village newVillage, VillageManager manager) {
        if (newVillage.isLoneBuilding()) {
            return;
        }
        int backgroundRadius = MillenaireServerConfig.SERVER.backgroundRadius.getAsInt();
        long bgRadiusSq = (long)backgroundRadius * (long)backgroundRadius;
        for (Village other : manager.getAllVillages()) {
            if (other.getId().equals(newVillage.getId()) || other.isLoneBuilding() || newVillage.getCenter().distSqr((Vec3i)other.getCenter()) >= (double)bgRadiusSq) continue;
            VillageId newParent = newVillage.getParentVillageId();
            VillageId otherParent = other.getParentVillageId();
            int initialRelation = newParent != null && newParent.equals(other.getId()) ? 100 : (otherParent != null && otherParent.equals(newVillage.getId()) ? 100 : (newParent != null && otherParent != null && newParent.equals(otherParent) ? 100 : (newVillage.getCultureId().equals((Object)other.getCultureId()) ? 50 : -30)));
            newVillage.setRelation(other.getId(), initialRelation);
            other.setRelation(newVillage.getId(), initialRelation);
        }
    }

    @javax.annotation.Nullable
    private static String pickQualifier(ServerLevel level, BlockPos center, VillageType villageType) {
        boolean hasAnyQualifier;
        boolean bl = hasAnyQualifier = !villageType.qualifiers().isEmpty() || villageType.forestQualifier() != null || villageType.hillQualifier() != null || villageType.mountainQualifier() != null || villageType.desertQualifier() != null || villageType.lavaQualifier() != null || villageType.lakeQualifier() != null || villageType.oceanQualifier() != null;
        if (!hasAnyQualifier) {
            return null;
        }
        ArrayList<String> candidates = new ArrayList<String>(villageType.qualifiers());
        Holder biomeHolder = level.getBiome(center);
        if (biomeHolder.is(Tags.Biomes.IS_FOREST) && villageType.forestQualifier() != null) {
            candidates.add(villageType.forestQualifier());
        }
        if (biomeHolder.is(Tags.Biomes.IS_HILL) && villageType.hillQualifier() != null) {
            candidates.add(villageType.hillQualifier());
        }
        if (biomeHolder.is(Tags.Biomes.IS_MOUNTAIN) && villageType.mountainQualifier() != null) {
            candidates.add(villageType.mountainQualifier());
        }
        if (biomeHolder.is(Tags.Biomes.IS_DESERT) && villageType.desertQualifier() != null) {
            candidates.add(villageType.desertQualifier());
        }
        if (biomeHolder.is(Tags.Biomes.IS_OCEAN) && villageType.oceanQualifier() != null) {
            candidates.add(villageType.oceanQualifier());
        }
        if (villageType.lavaQualifier() != null || villageType.lakeQualifier() != null) {
            boolean lavaFound = false;
            boolean lakeFound = false;
            for (int dx = -50; !(dx > 50 || lavaFound && lakeFound); dx += 4) {
                for (int dz = -50; !(dz > 50 || lavaFound && lakeFound); dz += 4) {
                    for (int dy = -10; !(dy > 20 || lavaFound && lakeFound); dy += 2) {
                        BlockPos pos = center.offset(dx, dy, dz);
                        BlockState state = level.getBlockState(pos);
                        if (!lavaFound && state.is(Blocks.LAVA)) {
                            lavaFound = true;
                        }
                        if (lakeFound || !state.is(Blocks.WATER) || pos.getY() < 65 || !level.getBlockState(pos.above()).isAir()) continue;
                        lakeFound = true;
                    }
                }
            }
            if (lavaFound && villageType.lavaQualifier() != null) {
                candidates.add(villageType.lavaQualifier());
            }
            if (lakeFound && villageType.lakeQualifier() != null) {
                candidates.add(villageType.lakeQualifier());
            }
        }
        if (candidates.isEmpty()) {
            return null;
        }
        return (String)candidates.get(level.random.nextInt(candidates.size()));
    }

    private static BrickColourTheme pickWeightedTheme(List<BrickColourTheme> themes, RandomSource random) {
        int totalWeight = 0;
        for (BrickColourTheme theme : themes) {
            totalWeight += theme.weight();
        }
        int roll = random.nextInt(totalWeight);
        int cumulative = 0;
        for (BrickColourTheme theme : themes) {
            if (roll >= (cumulative += theme.weight())) continue;
            return theme;
        }
        return themes.getLast();
    }

    private static List<SlotWithPlan> resolveSlots(VillageType villageType) {
        ArrayList<SlotWithPlan> result = new ArrayList<SlotWithPlan>();
        for (VillageType.LayoutSlot slot : villageType.layout()) {
            BuildingPlan directPlan;
            ResourceLocation planRef = slot.plan();
            BuildingPlanSet planSet = ModCultures.getBuildingPlanSet(planRef);
            if (planSet != null) {
                BuildingPlan plan;
                String variant = planSet.pickRandomVariant(RANDOM);
                BuildingPlanSet.LevelDef levelDef = planSet.getLevel(variant, 0);
                if (levelDef != null && (plan = ModCultures.getBuildingPlan(levelDef.planId())) != null) {
                    result.add(new SlotWithPlan(slot, plan, planSet.id(), variant));
                    continue;
                }
                LOGGER.warn("[Millenaire] BuildingPlanSet {} found but plan level 0 not found", (Object)planRef);
            }
            if ((directPlan = ModCultures.getBuildingPlan(planRef)) != null) {
                result.add(new SlotWithPlan(slot, directPlan, null, null));
                continue;
            }
            LOGGER.warn("[Millenaire] Plan not found: {} (neither set nor direct plan)", (Object)planRef);
        }
        return result;
    }

    private static boolean isCentreRole(String role) {
        return "centre".equals(role) || "townhall".equals(role);
    }

    private static boolean isStartRole(String role) {
        return "start".equals(role) || "starter".equals(role);
    }

    private static void spawnWalls(ServerLevel level, Village village, VillageType villageType, BlockPos centre, VillageTerrainMap terrainMap, List<PrecomputedWalls> precomputedWalls) {
        TerrainReachability reachability = TerrainReachability.compute(terrainMap, centre);
        for (PrecomputedWalls pw : precomputedWalls) {
            reachability = VillageSpawner.placeWallSegments(level, village, centre, terrainMap, reachability, pw.wallType(), pw.result());
        }
    }

    private static TerrainReachability placeWallSegments(ServerLevel level, Village village, BlockPos centre, VillageTerrainMap terrainMap, @javax.annotation.Nullable TerrainReachability reachability, WallType wallType, VillageWallGenerator.WallLocationResult result) {
        if (result.segments().isEmpty()) {
            return reachability != null ? reachability : TerrainReachability.compute(terrainMap, centre);
        }
        Map<ResourceLocation, PlanSetRef> planToSet = VillageSpawner.buildWallPlanSetMap(wallType);
        int placed = 0;
        int planned = 0;
        boolean terrainChanged = false;
        record ResolvedSegment(VillageWallGenerator.PlannedWallSegment seg, BuildingPlan plan, int targetLevel, PlanSetRef ref, Rotation rotation, BlockPos centredOrigin, BlockPos adjustedOrigin, boolean spawn, BuildingId buildingId, @javax.annotation.Nullable boolean[][] snowSnapshot, ClearMargins margins) {
        }
        ArrayList<ResolvedSegment> resolvedSegments = new ArrayList<ResolvedSegment>(result.segments().size());
        for (VillageWallGenerator.PlannedWallSegment seg : result.segments()) {
            int targetLevel;
            PlanSetRef ref = planToSet.get((Object)seg.planId());
            if (ref == null) {
                LOGGER.warn("[Millenaire] Wall segment plan {} has no matching plan set in wall type {}", (Object)seg.planId(), (Object)wallType.id());
                continue;
            }
            BuildingPlanSet wallSet = ModCultures.getBuildingPlanSet(ref.planSetId());
            BuildingPlan plan = null;
            if (wallSet != null) {
                BuildingPlanSet.LevelDef levelDef;
                for (targetLevel = Math.max(0, seg.level()); targetLevel >= 0 && ((levelDef = wallSet.getLevel(ref.variant(), targetLevel)) == null || (plan = ModCultures.getBuildingPlan(levelDef.planId())) == null); --targetLevel) {
                }
            }
            if (plan == null) {
                plan = ModCultures.getBuildingPlan(seg.planId());
                targetLevel = 0;
            }
            if (plan == null) continue;
            Rotation rotation = VillageSpawner.intToRotation(seg.orientation());
            BlockPos centredOrigin = VillageSpawner.centreOriginOnPos(seg.pos(), plan.width(), plan.depth(), rotation);
            BlockPos adjustedOrigin = new BlockPos(centredOrigin.getX(), centredOrigin.getY() + plan.groundLevel(), centredOrigin.getZ());
            boolean spawn = seg.level() >= 0;
            ClearMargins segmentMargins = wallSet != null ? wallSet.clearMargins() : ClearMargins.defaults();
            boolean[][] snowSnapshot = null;
            if (spawn) {
                snowSnapshot = TerrainPreparer.checkForSnow(level, centredOrigin, plan.width(), plan.depth(), rotation, segmentMargins);
                TerrainPreparer.clearAndFlattenAtY(level, centredOrigin, plan.width(), plan.height(), plan.depth(), rotation, plan.groundLevel(), centredOrigin.getY(), segmentMargins);
                TerrainPreparer.decayOrphanedLeaves(level, centredOrigin, plan.width(), plan.depth(), rotation, centredOrigin.getY(), segmentMargins);
            }
            resolvedSegments.add(new ResolvedSegment(seg, plan, targetLevel, ref, rotation, centredOrigin, adjustedOrigin, spawn, BuildingId.random(), snowSnapshot, segmentMargins));
        }
        for (ResolvedSegment rs : resolvedSegments) {
            BuildingPlan plan = rs.plan();
            BuildingInstance.Status status = rs.spawn() ? BuildingInstance.Status.COMPLETE : BuildingInstance.Status.PLANNED;
            BuildingInstance instance = new BuildingInstance(rs.buildingId(), plan.id(), rs.adjustedOrigin(), rs.rotation(), status, rs.ref().planSetId(), rs.ref().variant(), rs.targetLevel());
            if (rs.spawn()) {
                if (village.getBrickTheme() != null) {
                    instance.initBrickColours(village.getBrickTheme(), level.getRandom());
                }
                BuildingPlacer.placeInstantly(level, plan, rs.adjustedOrigin(), rs.rotation(), instance);
                TerrainPreparer.restoreSnow(level, rs.centredOrigin(), plan.width(), plan.depth(), rs.rotation(), rs.snowSnapshot(), rs.margins());
                BuildingFinalizer.applyPostPlacement(level, village, instance, plan);
                ++placed;
                terrainMap.markBuildingFootprint(rs.centredOrigin().getX(), rs.centredOrigin().getZ(), plan.width(), plan.depth(), ClearMargins.symmetric(0), rs.rotation(), rs.centredOrigin().getY());
                terrainMap.markOccupied(rs.centredOrigin().getX(), rs.centredOrigin().getZ(), plan.width(), plan.depth(), rs.margins(), rs.rotation());
                terrainChanged = true;
            } else {
                ++planned;
                terrainMap.markOccupied(rs.centredOrigin().getX(), rs.centredOrigin().getZ(), plan.width(), plan.depth(), rs.margins(), rs.rotation());
                terrainChanged = true;
            }
            village.addBuilding(instance);
            if (!rs.spawn()) continue;
            BuildingFinalizer.applyCompletionEffects(level, village, instance);
        }
        LOGGER.info("[Millenaire] Wall type {}: {} segments placed, {} planned", new Object[]{wallType.id(), placed, planned});
        return terrainChanged ? TerrainReachability.compute(terrainMap, centre) : (reachability != null ? reachability : TerrainReachability.compute(terrainMap, centre));
    }

    private static void markWallFootprints(VillageTerrainMap terrainMap, WallType wallType, VillageWallGenerator.WallLocationResult result) {
        Map<ResourceLocation, PlanSetRef> planToSet = VillageSpawner.buildWallPlanSetMap(wallType);
        for (VillageWallGenerator.PlannedWallSegment seg : result.segments()) {
            PlanSetRef ref = planToSet.get((Object)seg.planId());
            BuildingPlanSet wallSet = ref != null ? ModCultures.getBuildingPlanSet(ref.planSetId()) : null;
            BuildingPlan plan = null;
            if (wallSet != null) {
                BuildingPlanSet.LevelDef levelDef;
                for (int targetLevel = Math.max(0, seg.level()); targetLevel >= 0 && ((levelDef = wallSet.getLevel(ref.variant(), targetLevel)) == null || (plan = ModCultures.getBuildingPlan(levelDef.planId())) == null); --targetLevel) {
                }
            }
            if (plan == null) {
                plan = ModCultures.getBuildingPlan(seg.planId());
            }
            if (plan == null) continue;
            Rotation rotation = VillageSpawner.intToRotation(seg.orientation());
            BlockPos centredOrigin = VillageSpawner.centreOriginOnPos(seg.pos(), plan.width(), plan.depth(), rotation);
            ClearMargins wallClear = wallSet != null ? wallSet.clearMargins() : ClearMargins.symmetric(0);
            terrainMap.markOccupied(centredOrigin.getX(), centredOrigin.getZ(), plan.width(), plan.depth(), wallClear, rotation);
        }
    }

    private static Map<ResourceLocation, PlanSetRef> buildWallPlanSetMap(WallType wallType) {
        HashMap<ResourceLocation, PlanSetRef> map = new HashMap<ResourceLocation, PlanSetRef>();
        VillageSpawner.addWallSetToMap(map, wallType.wallPlanSet());
        VillageSpawner.addWallSetToMap(map, wallType.towerPlanSet());
        VillageSpawner.addWallSetToMap(map, wallType.gatewayPlanSet());
        VillageSpawner.addWallSetToMap(map, wallType.cornerPlanSet());
        VillageSpawner.addWallSetToMap(map, wallType.capRightPlanSet());
        VillageSpawner.addWallSetToMap(map, wallType.capLeftPlanSet());
        VillageSpawner.addWallSetToMap(map, wallType.capBothPlanSet());
        VillageSpawner.addWallSetToMap(map, wallType.slope1LeftPlanSet());
        VillageSpawner.addWallSetToMap(map, wallType.slope1RightPlanSet());
        VillageSpawner.addWallSetToMap(map, wallType.slope2LeftPlanSet());
        VillageSpawner.addWallSetToMap(map, wallType.slope2RightPlanSet());
        VillageSpawner.addWallSetToMap(map, wallType.slope3LeftPlanSet());
        VillageSpawner.addWallSetToMap(map, wallType.slope3RightPlanSet());
        return map;
    }

    private static void addWallSetToMap(Map<ResourceLocation, PlanSetRef> map, @javax.annotation.Nullable ResourceLocation setId) {
        if (setId == null) {
            return;
        }
        BuildingPlanSet set = ModCultures.getBuildingPlanSet(setId);
        if (set == null) {
            return;
        }
        String variant = set.variants().keySet().stream().findFirst().orElse(null);
        if (variant == null) {
            return;
        }
        BuildingPlanSet.LevelDef level0 = set.getLevel(variant, 0);
        if (level0 == null) {
            return;
        }
        map.putIfAbsent(level0.planId(), new PlanSetRef(setId, variant));
    }

    private static Rotation intToRotation(int orientation) {
        return Rotation.values()[Math.floorMod(3 - orientation, 4)];
    }

    private static BlockPos centreOriginOnPos(BlockPos pos, int planWidth, int planDepth, Rotation rotation) {
        int L = planWidth;
        int W = planDepth;
        return switch (rotation) {
            case Rotation.CLOCKWISE_90 -> new BlockPos(pos.getX() + W / 2 - 1, pos.getY(), pos.getZ() - L / 2);
            case Rotation.CLOCKWISE_180 -> new BlockPos(pos.getX() + L / 2 - 1, pos.getY(), pos.getZ() + W / 2 - 1);
            case Rotation.COUNTERCLOCKWISE_90 -> new BlockPos(pos.getX() - W / 2, pos.getY(), pos.getZ() + L / 2 - 1);
            default -> new BlockPos(pos.getX() - L / 2, pos.getY(), pos.getZ() - W / 2);
        };
    }

    private record ValidationResult(@javax.annotation.Nullable Component error, @javax.annotation.Nullable SlotWithPlan centreSlot, List<PlacedBuilding> placedBuildings, BlockPos townhallRequestedOrigin, Rotation townhallRotation, ClearMargins centreClearMargins, int townhallBaseY, BlockPos centerAtGround, List<PrecomputedWalls> precomputedWalls) {
        static ValidationResult fail(Component error) {
            return new ValidationResult(error, null, List.of(), BlockPos.ZERO, Rotation.NONE, ClearMargins.symmetric(0), 0, BlockPos.ZERO, List.of());
        }
    }

    private record SlotWithPlan(VillageType.LayoutSlot slot, BuildingPlan plan, @javax.annotation.Nullable ResourceLocation planSetId, @javax.annotation.Nullable String variant) {
    }

    private record PrecomputedWalls(WallType wallType, VillageWallGenerator.WallLocationResult result) {
    }

    private record PlacedBuilding(SlotWithPlan swp, BlockPos origin, Rotation rotation, BlockPos requestedOrigin, int baseY) {
    }

    private record PlanSetRef(ResourceLocation planSetId, String variant) {
    }
}

