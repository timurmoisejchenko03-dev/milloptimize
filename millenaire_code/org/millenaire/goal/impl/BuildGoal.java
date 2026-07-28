/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.mojang.logging.LogUtils
 *  javax.annotation.Nullable
 *  net.minecraft.core.BlockPos
 *  net.minecraft.core.Vec3i
 *  net.minecraft.network.chat.Component
 *  net.minecraft.resources.ResourceLocation
 *  net.minecraft.server.level.ServerLevel
 *  net.minecraft.sounds.SoundSource
 *  net.minecraft.tags.BlockTags
 *  net.minecraft.world.InteractionHand
 *  net.minecraft.world.item.Item
 *  net.minecraft.world.item.ItemStack
 *  net.minecraft.world.item.Items
 *  net.minecraft.world.level.BlockGetter
 *  net.minecraft.world.level.ItemLike
 *  net.minecraft.world.level.Level
 *  net.minecraft.world.level.LevelAccessor
 *  net.minecraft.world.level.LevelReader
 *  net.minecraft.world.level.block.Blocks
 *  net.minecraft.world.level.block.ChestBlock
 *  net.minecraft.world.level.block.SoundType
 *  net.minecraft.world.level.block.state.BlockState
 *  net.minecraft.world.level.levelgen.Heightmap$Types
 *  net.minecraft.world.phys.AABB
 *  org.slf4j.Logger
 */
package org.millenaire.goal.impl;

import com.mojang.logging.LogUtils;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import javax.annotation.Nullable;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Vec3i;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.ItemLike;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.ChestBlock;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.phys.AABB;
import org.millenaire.block.mock.MockBlock;
import org.millenaire.building.AnywoodHelper;
import org.millenaire.building.BuildingId;
import org.millenaire.building.BuildingInstance;
import org.millenaire.building.BuildingInventory;
import org.millenaire.building.BuildingPlan;
import org.millenaire.building.BuildingPlanSet;
import org.millenaire.building.ConstructionTask;
import org.millenaire.building.PlacementStep;
import org.millenaire.building.placement.BlockPlacementEngine;
import org.millenaire.culture.ModCultures;
import org.millenaire.entity.BuilderSafetyHandler;
import org.millenaire.entity.MillVillager;
import org.millenaire.entity.VillagerInventory;
import org.millenaire.entity.VillagerNavDriver;
import org.millenaire.goal.GoalContext;
import org.millenaire.goal.NavigationHelperUtils;
import org.millenaire.goal.StopReason;
import org.millenaire.goal.TravelPhase;
import org.millenaire.goal.VillagerGoal;
import org.millenaire.goal.VillagerTask;
import org.millenaire.goal.impl.IdleGoal;
import org.millenaire.goal.impl.WalkStuckTracker;
import org.millenaire.item.ItemHelper;
import org.millenaire.tool.ToolCategory;
import org.millenaire.tool.ToolCategoryRegistry;
import org.millenaire.village.BuildingFinalizer;
import org.millenaire.village.SubBuildingHelper;
import org.millenaire.village.Village;
import org.millenaire.village.VillageEventType;
import org.millenaire.village.VillageSavedData;
import org.millenaire.world.BuildingPlacer;
import org.slf4j.Logger;

public class BuildGoal
implements VillagerGoal {
    public static final ResourceLocation ID = ResourceLocation.fromNamespaceAndPath((String)"millenaire", (String)"build");
    private static final Logger LOGGER = LogUtils.getLogger();

    @Override
    public ResourceLocation id() {
        return ID;
    }

    @Override
    public int computePriority(GoalContext context) {
        return 1500;
    }

    @Override
    public boolean canStart(GoalContext context) {
        BuildingId savedId = context.villager().getConstructionBuildingId();
        if (savedId != null) {
            BuildingInstance saved = context.village().getBuilding(savedId);
            if (saved != null && saved.isBeingBuilt() && saved.getConstructionTask() != null && (!saved.getConstructionTask().isReserved() || context.villager().getUUID().equals(saved.getConstructionTask().getReservedBuilder()))) {
                return true;
            }
            context.villager().setConstructionBuildingId(null);
        }
        return context.village().findUnreservedConstruction() != null;
    }

    @Override
    public VillagerTask start(GoalContext context) {
        BuildingInstance saved;
        BuildingInstance building = null;
        BuildingId savedId = context.villager().getConstructionBuildingId();
        if (savedId != null && (saved = context.village().getBuilding(savedId)) != null && saved.isBeingBuilt() && saved.getConstructionTask() != null && (!saved.getConstructionTask().isReserved() || context.villager().getUUID().equals(saved.getConstructionTask().getReservedBuilder()))) {
            building = saved;
        }
        if (building == null) {
            building = context.village().findUnreservedConstruction();
        }
        if (building == null) {
            context.villager().setConstructionBuildingId(null);
            return new IdleGoal().start(context);
        }
        ConstructionTask task = building.getConstructionTask();
        if (task == null) {
            context.villager().setConstructionBuildingId(null);
            return new IdleGoal().start(context);
        }
        task.reserve(context.villager().getUUID());
        context.villager().setConstructionBuildingId(building.getId());
        LOGGER.info("Builder starting construction of {} \u2014 {} steps total, progress {}%", new Object[]{building.getPlanId(), task.totalSteps(), String.format("%.0f", Float.valueOf(task.progress() * 100.0f))});
        return new BuildTask(building, task);
    }

    static int computeConstructionDuration(float shovelEfficiency, boolean isSoftBlock) {
        int baseDuration = shovelEfficiency > 8.0f ? 7 : (shovelEfficiency == 8.0f ? 8 : (shovelEfficiency >= 6.0f ? 10 : (shovelEfficiency >= 4.0f ? 12 : (shovelEfficiency >= 2.0f ? 14 : 16))));
        if (isSoftBlock) {
            return (int)((float)baseDuration / 4.0f);
        }
        return baseDuration;
    }

    static class BuildTask
    implements VillagerTask {
        private static final double WALK_SPEED = 0.5;
        private static final double ARRIVE_DISTANCE_SQ = 25.0;
        private static final double REMOTE_PLACE_DISTANCE_SQ = 2500.0;
        private static final int PLACING_STUCK_TIMEOUT = 400;
        private static final int WALKING_STUCK_TIMEOUT = 1200;
        private static final double WALKING_PROGRESS_EPSILON_SQ = 1.0;
        private static final int SUFFOCATION_GRACE_TICKS = 10;
        private static final int NAVIGATE_STUCK_THRESHOLD = 100;
        private static final double HORIZONTAL_APPROACH_DISTANCE_SQ = 16.0;
        private static final int ROOF_STRAND_HEIGHT = 3;
        private final BuildingInstance building;
        private final ConstructionTask constructionTask;
        private State state = State.FETCHING_RESOURCES;
        private int actionCooldown;
        private int placingStuckTicks;
        private final WalkStuckTracker walkStuckTracker = new WalkStuckTracker(1200, 1.0);
        private int navigateStuckTicks;
        private int navigateStuckStepIndex = -1;
        private boolean lastNavWasHorizontalApproach;
        private boolean progressFlag;
        private float cachedShovelEfficiency = 2.0f;
        @Nullable
        private VillagerInventory villagerInventory;
        @Nullable
        private List<ItemStack> travellingItems;
        @Nullable
        private List<ItemStack> destinationItems;
        private static final double ARRIVE_AT_BUILDING_DIST = 5.0;
        private static final int FETCHING_TIMEOUT = 600;
        private int fetchingTicks;
        private static final double BLOCK_REACH_DISTANCE_SQ = 25.0;

        BuildTask(BuildingInstance building, ConstructionTask constructionTask) {
            this.building = building;
            this.constructionTask = constructionTask;
            if (constructionTask.getNextStepIndex() > 0) {
                this.state = State.WALKING_TO_SITE;
            }
        }

        @Override
        public ResourceLocation goalId() {
            return ID;
        }

        @Override
        public void reportProgress() {
            this.progressFlag = true;
        }

        @Override
        public boolean consumeProgress() {
            if (this.progressFlag) {
                this.progressFlag = false;
                return true;
            }
            return false;
        }

        @Override
        public void tick(GoalContext ctx) {
            if (this.villagerInventory == null) {
                this.villagerInventory = ctx.villager().getInventory();
                this.travellingItems = List.of(this.getBestToolStack("toolsshovel", Items.WOODEN_SHOVEL));
                this.destinationItems = List.of(this.getBestToolStack("toolsshovel", Items.WOODEN_SHOVEL));
                this.cachedShovelEfficiency = BuildTask.computeShovelEfficiency(this.villagerInventory);
            }
            switch (this.state.ordinal()) {
                case 0: {
                    this.tickFetching(ctx);
                    break;
                }
                case 1: {
                    this.tickWalking(ctx);
                    break;
                }
                case 2: {
                    this.tickPlacing(ctx);
                    break;
                }
            }
        }

        @Override
        public boolean isFinished() {
            return this.state == State.DONE;
        }

        @Override
        public void stop(GoalContext ctx, StopReason reason) {
            if (ctx == null) {
                return;
            }
            ctx.villager().getNavManager().stop(ctx.villager());
            if (reason == StopReason.INTERRUPTED) {
                this.constructionTask.resetReservationAge();
            } else {
                this.constructionTask.releaseReservation();
                ctx.villager().setConstructionBuildingId(null);
            }
            if (reason == StopReason.IMPOSSIBLE) {
                this.constructionTask.incrementFailedAttempts();
                LOGGER.warn("Construction impossible for building {} \u2014 attempt failed ({}/3)", (Object)this.building.getPlanId(), (Object)this.constructionTask.getFailedAttempts());
            }
        }

        @Override
        public TravelPhase getTravelPhase() {
            return this.state == State.WALKING_TO_SITE || this.state == State.FETCHING_RESOURCES ? TravelPhase.TRAVELLING : TravelPhase.AT_DESTINATION;
        }

        @Override
        public List<ItemStack> getHeldItems(TravelPhase phase) {
            if (phase == TravelPhase.TRAVELLING) {
                return this.travellingItems != null ? this.travellingItems : List.of(new ItemStack((ItemLike)Items.WOODEN_SHOVEL));
            }
            return this.destinationItems != null ? this.destinationItems : List.of(new ItemStack((ItemLike)Items.WOODEN_SHOVEL));
        }

        private ItemStack getBestToolStack(String categoryId, Item fallback) {
            if (this.villagerInventory == null) {
                return new ItemStack((ItemLike)fallback);
            }
            ToolCategory category = ToolCategoryRegistry.get(categoryId);
            if (category == null) {
                return new ItemStack((ItemLike)fallback);
            }
            ToolCategory.ToolEntry best = category.getBestOwned(item -> this.villagerInventory.getCount((Item)item) > 0);
            if (best != null && best.item() != null) {
                return new ItemStack((ItemLike)best.item());
            }
            return new ItemStack((ItemLike)fallback);
        }

        @Override
        public List<ItemStack> getOffHandItems(TravelPhase phase) {
            if (phase != TravelPhase.AT_DESTINATION) {
                return List.of();
            }
            PlacementStep step = this.constructionTask.currentStep();
            if (step == null || step.blockState().isAir()) {
                return List.of();
            }
            ItemStack blockItem = new ItemStack((ItemLike)step.blockState().getBlock().asItem());
            if (blockItem.isEmpty()) {
                return List.of();
            }
            return List.of(blockItem);
        }

        @Override
        @Nullable
        public Component getGoalLabel() {
            if (this.state == State.FETCHING_RESOURCES) {
                return Component.translatable((String)"goal.millenaire.build.fetching");
            }
            return this.state == State.WALKING_TO_SITE ? Component.translatable((String)"goal.millenaire.build.travelling") : Component.translatable((String)"goal.millenaire.build");
        }

        private void tickFetching(GoalContext ctx) {
            if (this.hasRequiredResources(ctx)) {
                LOGGER.debug("Builder already has required resources \u2014 skip FETCHING");
                this.transitionToWalking();
                this.reportProgress();
                return;
            }
            BuildingInstance townhall = ctx.village().getTownhall();
            if (townhall == null) {
                LOGGER.warn("No TownHall found for builder \u2014 skip FETCHING phase");
                this.transitionToWalking();
                return;
            }
            ++this.fetchingTicks;
            if (this.fetchingTicks > 600) {
                LOGGER.debug("Builder FETCHING timeout ({} ticks) \u2014 skip to WALKING", (Object)this.fetchingTicks);
                ctx.villager().getNavManager().stop(ctx.villager());
                this.transitionToWalking();
                this.reportProgress();
                return;
            }
            BlockPos thTarget = BuildTask.resolveNavTarget(townhall);
            VillagerNavDriver nav = ctx.villager().getNavManager();
            if (nav.getDestination() == null) {
                nav.navigateTo(ctx.villager(), thTarget, 0.5);
            }
            if (nav.isArrivedHorizontal(ctx.villager(), 5.0)) {
                this.fetchResourcesFromTownhall(ctx, townhall);
                nav.stop(ctx.villager());
                this.transitionToWalking();
                this.reportProgress();
            } else if (nav.isAbandoned()) {
                LOGGER.debug("Builder cannot reach TownHall \u2014 skip FETCHING phase");
                nav.stop(ctx.villager());
                this.transitionToWalking();
                this.reportProgress();
            }
        }

        private void fetchResourcesFromTownhall(GoalContext ctx, BuildingInstance townhall) {
            int taken;
            Map<ResourceLocation, Integer> required = this.getRequiredResources();
            if (required.isEmpty()) {
                LOGGER.debug("Construction {}: no required resources", (Object)this.building.getPlanId());
                return;
            }
            BuildingInventory thInventory = townhall.getInventory();
            if (thInventory == null) {
                LOGGER.warn("TownHall {} has no inventory \u2014 resources not transferred", (Object)townhall.getPlanId());
                return;
            }
            Set<Item> specificLogs = AnywoodHelper.collectSpecificLogs(required);
            for (Map.Entry<ResourceLocation, Integer> entry : required.entrySet()) {
                Item item;
                if (AnywoodHelper.isAnywood(entry.getKey()) || (item = ItemHelper.resolve(entry.getKey())) == null) continue;
                int needed = entry.getValue();
                int taken2 = thInventory.remove((Level)ctx.level(), item, needed);
                if (taken2 <= 0) continue;
                ctx.villager().getInventory().add(item, taken2);
            }
            Integer anywoodNeeded = required.get(AnywoodHelper.ANYWOOD_LOG);
            if (anywoodNeeded != null && anywoodNeeded > 0 && (taken = thInventory.removeByTag((Level)ctx.level(), AnywoodHelper.LOGS_TAG, anywoodNeeded, specificLogs)) > 0) {
                ctx.villager().getInventory().add(Items.OAK_LOG, taken);
            }
            LOGGER.info("Builder fetched resources from TH for construction of {}", (Object)this.building.getPlanId());
        }

        private void deductResourcesFromInventory(GoalContext ctx) {
            Map<ResourceLocation, Integer> required = this.getRequiredResources();
            if (required.isEmpty()) {
                return;
            }
            VillagerInventory inventory = ctx.villager().getInventory();
            for (Map.Entry<ResourceLocation, Integer> entry : required.entrySet()) {
                Item item;
                if (AnywoodHelper.isAnywood(entry.getKey()) || (item = ItemHelper.resolve(entry.getKey())) == null) continue;
                inventory.remove(item, entry.getValue());
            }
            Integer anywoodNeeded = required.get(AnywoodHelper.ANYWOOD_LOG);
            if (anywoodNeeded != null && anywoodNeeded > 0) {
                inventory.remove(Items.OAK_LOG, anywoodNeeded);
            }
        }

        private Map<ResourceLocation, Integer> getRequiredResources() {
            if (this.building.getPlanSetId() == null || this.building.getVariant() == null) {
                return Map.of();
            }
            BuildingPlanSet planSet = ModCultures.getBuildingPlanSet(this.building.getPlanSetId());
            if (planSet == null) {
                return Map.of();
            }
            BuildingPlanSet.LevelDef levelDef = planSet.getLevel(this.building.getVariant(), this.building.getLevel());
            if (levelDef == null) {
                return Map.of();
            }
            return levelDef.requiredResources();
        }

        private boolean hasRequiredResources(GoalContext ctx) {
            Map<ResourceLocation, Integer> required = this.getRequiredResources();
            if (required.isEmpty()) {
                return true;
            }
            for (Map.Entry<ResourceLocation, Integer> entry : required.entrySet()) {
                Item item;
                if (AnywoodHelper.isAnywood(entry.getKey()) || (item = ItemHelper.resolve(entry.getKey())) == null || ctx.villager().getInventory().has(item, entry.getValue())) continue;
                return false;
            }
            Integer anywoodNeeded = required.get(AnywoodHelper.ANYWOOD_LOG);
            return anywoodNeeded == null || anywoodNeeded <= 0 || ctx.villager().getInventory().hasByTag(AnywoodHelper.LOGS_TAG, anywoodNeeded);
        }

        private void transitionToWalking() {
            this.state = State.WALKING_TO_SITE;
            this.walkStuckTracker.reset();
        }

        private void tickWalking(GoalContext ctx) {
            double dz;
            PlacementStep step = this.constructionTask.currentStep();
            if (step == null) {
                this.finishConstruction(ctx);
                return;
            }
            BlockPos siteTarget = BuildTask.resolveNavTarget(this.building);
            VillagerNavDriver nav = ctx.villager().getNavManager();
            if (nav.getDestination() == null || nav.getDestination().distSqr((Vec3i)siteTarget) > 4.0) {
                nav.navigateTo(ctx.villager(), siteTarget, 0.5);
            }
            if (nav.isArrivedHorizontal(ctx.villager(), Math.sqrt(25.0))) {
                nav.stop(ctx.villager());
                this.state = State.PLACING;
                this.actionCooldown = 0;
                return;
            }
            BlockPos current = ctx.villager().blockPosition();
            double dx = current.getX() - siteTarget.getX();
            if (this.walkStuckTracker.update(dx * dx + (dz = (double)(current.getZ() - siteTarget.getZ())) * dz)) {
                this.teleportToSafeNear(ctx, siteTarget);
                LOGGER.warn("Builder rescue-teleported to site {} (no walking progress for {} ticks)", (Object)this.building.getPlanId(), (Object)1200);
            }
            if (nav.isAbandoned()) {
                nav.navigateTo(ctx.villager(), siteTarget, 0.5);
                this.reportProgress();
            }
            this.reportProgress();
        }

        private void tickPlacing(GoalContext ctx) {
            BlockPos bedFoot;
            PlacementStep step = this.constructionTask.currentStep();
            if (step == null) {
                this.finishConstruction(ctx);
                return;
            }
            if (step.blockState().getBlock() instanceof MockBlock) {
                this.advanceStep(ctx);
                return;
            }
            BlockPos absolutePos = this.building.getOrigin().offset((Vec3i)step.relativePos());
            ServerLevel level = ctx.level();
            BlockState stateToPlace = BuildingPlacer.applyFacingGuess((Level)level, absolutePos, step.blockState(), step.guessChestFacing(), step.guessTorchFacing(), step.guessFurnaceFacing());
            stateToPlace = BlockPlacementEngine.hydrateIfFarmland(stateToPlace);
            stateToPlace = BlockPlacementEngine.stabilizePathBlock(stateToPlace);
            stateToPlace = BuildingPlacer.computePaneConnections((LevelAccessor)level, absolutePos, stateToPlace);
            if (BlockPlacementEngine.isBlockAlreadySuitable(level.getBlockState(absolutePos), stateToPlace)) {
                this.advanceStep(ctx);
                return;
            }
            BlockPos currentPos = ctx.villager().blockPosition();
            ++this.placingStuckTicks;
            if (this.placingStuckTicks > 400) {
                BlockPos navTarget = BuildTask.resolveNavTarget(this.building);
                this.teleportToSafeNear(ctx, navTarget);
                this.placingStuckTicks = 0;
                this.reportProgress();
                LOGGER.debug("Builder TP chantier {} (bloqu\u00e9 PLACING {}+ ticks)", (Object)this.building.getPlanId(), (Object)400);
            }
            double distToBlockSq = currentPos.distSqr((Vec3i)absolutePos);
            int currentStepIdx = this.constructionTask.getNextStepIndex();
            if (currentStepIdx != this.navigateStuckStepIndex) {
                this.navigateStuckTicks = 0;
                this.navigateStuckStepIndex = currentStepIdx;
            }
            if (distToBlockSq > 25.0) {
                boolean withinRemoteRange;
                boolean accessible = this.isBlockAccessible(ctx, absolutePos);
                boolean bl = withinRemoteRange = distToBlockSq <= 2500.0;
                if (!accessible && withinRemoteRange) {
                    int dz;
                    int dx = currentPos.getX() - absolutePos.getX();
                    double horizontalDistSq = dx * dx + (dz = currentPos.getZ() - absolutePos.getZ()) * dz;
                    if (horizontalDistSq > 16.0) {
                        if (!this.lastNavWasHorizontalApproach) {
                            this.navigateStuckTicks = 0;
                            this.lastNavWasHorizontalApproach = true;
                        }
                        ++this.navigateStuckTicks;
                        if (this.navigateStuckTicks > 100) {
                            LOGGER.debug("Builder places remotely (horizontal stuck {} ticks) step {} at {}", new Object[]{this.navigateStuckTicks, currentStepIdx, absolutePos.toShortString()});
                            this.remotePlaceBlock(ctx, step, absolutePos);
                            return;
                        }
                        BlockPos approachPos = new BlockPos(absolutePos.getX(), currentPos.getY(), absolutePos.getZ());
                        this.navigateToBlock(ctx, approachPos);
                        return;
                    }
                    ++this.actionCooldown;
                    if (this.actionCooldown < this.getActionDuration(step)) {
                        return;
                    }
                    this.actionCooldown = 0;
                    this.remotePlaceBlock(ctx, step, absolutePos);
                    return;
                }
                if (this.lastNavWasHorizontalApproach) {
                    this.navigateStuckTicks = 0;
                    this.lastNavWasHorizontalApproach = false;
                }
                ++this.navigateStuckTicks;
                if (this.navigateStuckTicks > 100) {
                    LOGGER.debug("Builder places remotely (nav stuck {} ticks) step {} at {}", new Object[]{this.navigateStuckTicks, currentStepIdx, absolutePos.toShortString()});
                    this.remotePlaceBlock(ctx, step, absolutePos);
                    return;
                }
                this.navigateToBlock(ctx, absolutePos);
                return;
            }
            ++this.actionCooldown;
            if (this.actionCooldown < this.getActionDuration(step)) {
                return;
            }
            this.actionCooldown = 0;
            this.dodgeIfNeeded(ctx, absolutePos, stateToPlace);
            this.displaceBystandersAt(ctx, absolutePos, stateToPlace);
            ctx.villager().getLookControl().setLookAt((double)absolutePos.getX() + 0.5, (double)absolutePos.getY() + 0.5, (double)absolutePos.getZ() + 0.5);
            BlockState oldState = level.getBlockState(absolutePos);
            if (!oldState.isAir()) {
                SoundType oldSound = oldState.getSoundType((LevelReader)level, absolutePos, null);
                level.playSound(null, absolutePos, oldSound.getBreakSound(), SoundSource.BLOCKS, oldSound.getVolume() * 0.5f, oldSound.getPitch());
            }
            BlockPlacementEngine.clearBedIfPresent(level, absolutePos);
            BlockPlacementEngine.clearDoorIfPresent(level, absolutePos);
            level.setBlock(absolutePos, stateToPlace, BlockPlacementEngine.getPlacementFlags(stateToPlace));
            BlockPlacementEngine.fixWaterlogging(level, absolutePos, step.blockState());
            if (stateToPlace.getBlock() instanceof ChestBlock) {
                BlockPlacementEngine.fixAdjacentChestType(level, absolutePos, stateToPlace);
            }
            if ((bedFoot = BlockPlacementEngine.generateBedFoot(level, absolutePos, stateToPlace)) != null && this.building != null) {
                this.building.getBedManager().add(bedFoot);
            }
            BuildingPlacer.refreshNeighborConnections((LevelAccessor)level, absolutePos);
            BlockPlacementEngine.generateDoorUpper(level, absolutePos, stateToPlace);
            SoundType soundType = step.blockState().getSoundType((LevelReader)level, absolutePos, null);
            level.playSound(null, absolutePos, soundType.getPlaceSound(), SoundSource.BLOCKS, (soundType.getVolume() + 1.0f) / 2.0f * 0.6f, soundType.getPitch() * 0.8f);
            ctx.villager().swing(InteractionHand.MAIN_HAND);
            ctx.villager().grantSuffocationGrace(10);
            this.advanceStep(ctx);
        }

        private void advanceStep(GoalContext ctx) {
            this.constructionTask.advance();
            this.placingStuckTicks = 0;
            this.navigateStuckTicks = 0;
            this.reportProgress();
            VillageSavedData.get(ctx.level()).setDirty();
            ctx.villager().getNavigation().stop();
            if (this.constructionTask.isComplete()) {
                this.finishConstruction(ctx);
            }
        }

        private void navigateToBlock(GoalContext ctx, BlockPos targetPos) {
            if (ctx.villager().getNavigation().isDone()) {
                ctx.villager().getNavigation().moveTo((double)targetPos.getX() + 0.5, ctx.villager().getY(), (double)targetPos.getZ() + 0.5, 0.5);
            }
            ctx.villager().getLookControl().setLookAt((double)targetPos.getX() + 0.5, (double)targetPos.getY() + 0.5, (double)targetPos.getZ() + 0.5);
        }

        private void remotePlaceBlock(GoalContext ctx, PlacementStep step, BlockPos absolutePos) {
            ServerLevel level = ctx.level();
            BlockState stateToPlace = BuildingPlacer.applyFacingGuess((Level)level, absolutePos, step.blockState(), step.guessChestFacing(), step.guessTorchFacing(), step.guessFurnaceFacing());
            stateToPlace = BlockPlacementEngine.hydrateIfFarmland(stateToPlace);
            stateToPlace = BlockPlacementEngine.stabilizePathBlock(stateToPlace);
            stateToPlace = BuildingPlacer.computePaneConnections((LevelAccessor)level, absolutePos, stateToPlace);
            BlockState oldState = level.getBlockState(absolutePos);
            if (!BlockPlacementEngine.isBlockAlreadySuitable(oldState, stateToPlace)) {
                BlockPos bedFoot;
                this.displaceBystandersAt(ctx, absolutePos, stateToPlace);
                BlockPlacementEngine.clearBedIfPresent(level, absolutePos);
                BlockPlacementEngine.clearDoorIfPresent(level, absolutePos);
                if (!oldState.isAir()) {
                    SoundType oldSound = oldState.getSoundType((LevelReader)level, absolutePos, null);
                    level.playSound(null, absolutePos, oldSound.getBreakSound(), SoundSource.BLOCKS, oldSound.getVolume() * 0.5f, oldSound.getPitch());
                }
                ctx.villager().getLookControl().setLookAt((double)absolutePos.getX() + 0.5, (double)absolutePos.getY() + 0.5, (double)absolutePos.getZ() + 0.5);
                ctx.villager().swing(InteractionHand.MAIN_HAND);
                level.setBlock(absolutePos, stateToPlace, BlockPlacementEngine.getPlacementFlags(stateToPlace));
                BlockPlacementEngine.fixWaterlogging(level, absolutePos, step.blockState());
                if (stateToPlace.getBlock() instanceof ChestBlock) {
                    BlockPlacementEngine.fixAdjacentChestType(level, absolutePos, stateToPlace);
                }
                if ((bedFoot = BlockPlacementEngine.generateBedFoot(level, absolutePos, stateToPlace)) != null && this.building != null) {
                    this.building.getBedManager().add(bedFoot);
                }
                BlockPlacementEngine.generateDoorUpper(level, absolutePos, stateToPlace);
                SoundType soundType = step.blockState().getSoundType((LevelReader)level, absolutePos, null);
                level.playSound(null, absolutePos, soundType.getPlaceSound(), SoundSource.BLOCKS, (soundType.getVolume() + 1.0f) / 2.0f * 0.4f, soundType.getPitch() * 0.8f);
            }
            this.advanceStep(ctx);
        }

        private boolean isBlockAccessible(GoalContext ctx, BlockPos targetPos) {
            int builderY = ctx.villager().blockPosition().getY();
            int blockY = targetPos.getY();
            if (Math.abs(blockY - builderY) > 3) {
                return false;
            }
            BlockPos below = targetPos.below();
            BlockState belowState = ctx.level().getBlockState(below);
            return belowState.canOcclude();
        }

        private void dodgeIfNeeded(GoalContext ctx, BlockPos absolutePos, BlockState stateToPlace) {
            int[][] offsets;
            int feetY;
            if (!stateToPlace.isSuffocating((BlockGetter)ctx.level(), absolutePos)) {
                return;
            }
            BlockPos feetPos = ctx.villager().blockPosition();
            double hDist = Math.max(Math.abs(absolutePos.getX() - feetPos.getX()), Math.abs(absolutePos.getZ() - feetPos.getZ()));
            if (hDist >= 1.0) {
                return;
            }
            int blockY = absolutePos.getY();
            if (blockY < (feetY = feetPos.getY()) || blockY > feetY + 1) {
                return;
            }
            ServerLevel level = ctx.level();
            for (int[] off : offsets = new int[][]{{1, 0}, {-1, 0}, {0, 1}, {0, -1}}) {
                int nx = feetPos.getX() + off[0];
                int nz = feetPos.getZ() + off[1];
                BlockPos candidateFeet = new BlockPos(nx, feetY, nz);
                BlockPos candidateHead = candidateFeet.above();
                BlockPos belowPos = candidateFeet.below();
                if (level.getBlockState(belowPos).getCollisionShape((BlockGetter)level, belowPos).isEmpty() || level.getBlockState(candidateFeet).isSuffocating((BlockGetter)level, candidateFeet) || level.getBlockState(candidateHead).isSuffocating((BlockGetter)level, candidateHead)) continue;
                ctx.villager().teleportTo((double)nx + 0.5, feetY, (double)nz + 0.5);
                LOGGER.debug("Builder esquive vers ({}, {}, {}) avant de poser un bloc \u00e0 {}", new Object[]{nx, feetY, nz, absolutePos.toShortString()});
                return;
            }
            LOGGER.warn("Builder ne peut pas esquiver le bloc \u00e0 {} \u2014 risque de suffocation", (Object)absolutePos.toShortString());
        }

        private void displaceBystandersAt(GoalContext ctx, BlockPos absolutePos, BlockState stateToPlace) {
            ServerLevel level = ctx.level();
            if (!stateToPlace.isSuffocating((BlockGetter)level, absolutePos)) {
                return;
            }
            AABB area = new AABB((double)absolutePos.getX(), (double)absolutePos.getY(), (double)absolutePos.getZ(), (double)(absolutePos.getX() + 1), (double)(absolutePos.getY() + 2), (double)(absolutePos.getZ() + 1));
            List bystanders = level.getEntitiesOfClass(MillVillager.class, area, e -> e != ctx.villager());
            if (bystanders.isEmpty()) {
                return;
            }
            int[][] offsets = new int[][]{{1, 0}, {-1, 0}, {0, 1}, {0, -1}};
            for (MillVillager bystander : bystanders) {
                BlockPos feetPos = bystander.blockPosition();
                boolean displaced = false;
                for (int[] off : offsets) {
                    BlockPos candidate = new BlockPos(feetPos.getX() + off[0], feetPos.getY(), feetPos.getZ() + off[1]);
                    BlockPos below = candidate.below();
                    if (level.getBlockState(below).getCollisionShape((BlockGetter)level, below).isEmpty() || level.getBlockState(candidate).isSuffocating((BlockGetter)level, candidate) || level.getBlockState(candidate.above()).isSuffocating((BlockGetter)level, candidate.above())) continue;
                    bystander.teleportTo((double)candidate.getX() + 0.5, candidate.getY(), (double)candidate.getZ() + 0.5);
                    displaced = true;
                    break;
                }
                if (displaced) continue;
                int safeY = level.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, feetPos.getX(), feetPos.getZ());
                bystander.teleportTo((double)feetPos.getX() + 0.5, safeY, (double)feetPos.getZ() + 0.5);
            }
        }

        private void teleportToSafeNear(GoalContext ctx, BlockPos target) {
            NavigationHelperUtils.teleportToSafeNearTarget(ctx.villager(), target, BuilderSafetyHandler.buildExcludedTilesPredicate(this.building));
        }

        private int getActionDuration(PlacementStep step) {
            BlockState blockState = step.blockState();
            boolean isSoft = blockState.isAir() || blockState.is(BlockTags.DIRT) || blockState.is(BlockTags.SAND);
            return BuildGoal.computeConstructionDuration(this.cachedShovelEfficiency, isSoft);
        }

        private static float computeShovelEfficiency(VillagerInventory inventory) {
            ToolCategory category = ToolCategoryRegistry.get("toolsshovel");
            if (category == null) {
                return 2.0f;
            }
            return category.getBestDestroySpeed(item -> inventory.getCount((Item)item) > 0, Blocks.DIRT.defaultBlockState(), Items.WOODEN_SHOVEL);
        }

        private void finishConstruction(GoalContext ctx) {
            BuildingPlanSet.LevelDef currentLevelDef;
            BuildingPlanSet ps;
            BuildingPlanSet.LevelDef levelDef;
            BuildingPlanSet planSet;
            ServerLevel level = (ServerLevel)ctx.villager().level();
            Village village = ctx.village();
            this.building.markComplete();
            this.deductResourcesFromInventory(ctx);
            BuildingPlan plan = ModCultures.getBuildingPlan(this.building.getPlanId());
            if (plan != null) {
                BuildingPlanSet ps2;
                BuildingFinalizer.applyPostPlacement(level, village, this.building, plan);
                village.recordEvent(level, "Construction termin\u00e9e : " + this.building.getPlanId().getPath() + " \u00e0 " + this.building.getOrigin().toShortString());
                String chronicleName = this.building.getPlanId().getPath();
                if (this.building.getPlanSetId() != null && (ps2 = ModCultures.getBuildingPlanSet(this.building.getPlanSetId())) != null) {
                    BuildingPlanSet.LevelDef ld = ps2.getLevel(this.building.getVariant(), this.building.getLevel());
                    chronicleName = ld != null && ld.nativeName() != null ? ld.nativeName() : ps2.nativeName();
                }
                village.recordChronicleEvent(level, VillageEventType.BUILDING_COMPLETED, chronicleName, null);
                LOGGER.info("Building {} completed \u2014 {} special points resolved", (Object)this.building.getPlanId(), (Object)this.building.getResolvedPoints().size());
            }
            if (this.building.getLevel() > 0 && this.building.getPlanSetId() != null && this.building.getVariant() != null && (planSet = ModCultures.getBuildingPlanSet(this.building.getPlanSetId())) != null && (levelDef = planSet.getLevel(this.building.getVariant(), this.building.getLevel())) != null) {
                SubBuildingHelper.spawnUpgradeSubBuildings(level, village, planSet, levelDef, this.building, false);
            }
            BuildingFinalizer.applyCompletionEffects(level, village, this.building);
            BuildingPlacer.spawnWallDecorations(level, this.building.getResolvedPoints());
            BuildingFinalizer.applyVillageUpdates(level, village);
            if (this.building.getPlanSetId() != null && this.building.getVariant() != null && (ps = ModCultures.getBuildingPlanSet(this.building.getPlanSetId())) != null && (currentLevelDef = ps.getLevel(this.building.getVariant(), this.building.getLevel())) != null) {
                BuildingPlanSet.LevelDef prevLevelDef;
                boolean shouldRecalculate;
                boolean bl = shouldRecalculate = this.building.getLevel() == 0 || currentLevelDef.rebuildPath();
                if (!shouldRecalculate && this.building.getLevel() > 0 && (prevLevelDef = ps.getLevel(this.building.getVariant(), this.building.getLevel() - 1)) != null && prevLevelDef.pathLevel() != currentLevelDef.pathLevel()) {
                    shouldRecalculate = true;
                }
                if (shouldRecalculate) {
                    ctx.village().getPathManager().markPathsDirty();
                    ctx.village().markDirty();
                }
            }
            this.descendIfStrandedOnRoof(ctx);
            this.state = State.DONE;
        }

        private void descendIfStrandedOnRoof(GoalContext ctx) {
            BlockPos navTarget = BuildTask.resolveNavTarget(this.building);
            if (navTarget == null) {
                return;
            }
            int currentY = ctx.villager().blockPosition().getY();
            if (currentY - navTarget.getY() >= 3) {
                this.teleportToSafeNear(ctx, navTarget);
                LOGGER.debug("Builder TP down from finished structure {} (Y {} -> path-start {})", new Object[]{this.building.getPlanId(), currentY, navTarget.toShortString()});
            }
        }

        private static BlockPos resolveNavTarget(BuildingInstance building) {
            return building.getPathStartPos();
        }

        @Override
        public Map<String, String> getNavDebugInfo() {
            LinkedHashMap<String, String> info = new LinkedHashMap<String, String>();
            info.put("buildState", this.state.name());
            info.put("step", this.constructionTask.getNextStepIndex() + "/" + this.constructionTask.totalSteps() + " (" + Math.round(this.constructionTask.progress() * 100.0f) + "%)");
            info.put("building", this.building.getPlanId().getPath() + " @ " + this.building.getOrigin().toShortString());
            info.put("placingStuckTicks", String.valueOf(this.placingStuckTicks));
            info.put("navigateStuckTicks", this.navigateStuckTicks + " (step " + this.navigateStuckStepIndex + ")");
            return info;
        }

        static enum State {
            FETCHING_RESOURCES,
            WALKING_TO_SITE,
            PLACING,
            DONE;

        }
    }
}

