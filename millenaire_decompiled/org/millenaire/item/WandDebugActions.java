/*
 * Decompiled with CFR 0.150.
 * 
 * Could not load the following classes:
 *  com.mojang.logging.LogUtils
 *  javax.annotation.Nullable
 *  net.minecraft.core.BlockPos
 *  net.minecraft.core.particles.DustParticleOptions
 *  net.minecraft.core.particles.ParticleOptions
 *  net.minecraft.network.chat.Component
 *  net.minecraft.resources.ResourceLocation
 *  net.minecraft.server.level.ServerLevel
 *  net.minecraft.server.level.ServerPlayer
 *  net.minecraft.world.entity.Entity
 *  net.minecraft.world.item.Item
 *  net.minecraft.world.level.Level
 *  net.minecraft.world.level.block.Blocks
 *  net.minecraft.world.level.block.entity.BlockEntity
 *  net.minecraft.world.level.block.state.BlockState
 *  net.minecraft.world.level.pathfinder.Path
 *  org.joml.Vector3f
 *  org.slf4j.Logger
 */
package org.millenaire.item;

import com.mojang.logging.LogUtils;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import javax.annotation.Nullable;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.pathfinder.Path;
import org.joml.Vector3f;
import org.millenaire.block.LockedChestBlockEntity;
import org.millenaire.block.VillagePanelBlockEntity;
import org.millenaire.building.BuildingId;
import org.millenaire.building.BuildingInstance;
import org.millenaire.building.BuildingInventory;
import org.millenaire.building.BuildingPlanSet;
import org.millenaire.building.ConstructionTask;
import org.millenaire.building.SpecialPoint;
import org.millenaire.commerce.TradeGood;
import org.millenaire.commerce.TradeGoodsLoader;
import org.millenaire.culture.ModCultures;
import org.millenaire.culture.VillageType;
import org.millenaire.culture.VillagerType;
import org.millenaire.diagnostics.WaypointVisualizationManager;
import org.millenaire.entity.MillVillager;
import org.millenaire.entity.VillagerNavDriver;
import org.millenaire.entity.VillagerNavigationManager;
import org.millenaire.goal.GoalScheduler;
import org.millenaire.goal.VillagerTask;
import org.millenaire.goal.WaypointNavigator;
import org.millenaire.item.ItemHelper;
import org.millenaire.network.WandDebugActionPayload;
import org.millenaire.village.BuildingFinalizer;
import org.millenaire.village.LocalMerchantHelper;
import org.millenaire.village.NightActionHelper;
import org.millenaire.village.Village;
import org.millenaire.village.VillageGrowthManager;
import org.millenaire.village.VillageManager;
import org.millenaire.village.VillageSavedData;
import org.millenaire.village.VillageWaypointGraph;
import org.millenaire.village.panel.PanelType;
import org.millenaire.village.path.VillagePathManager;
import org.millenaire.world.TerrainReachability;
import org.millenaire.world.VillageTerrainMap;
import org.slf4j.Logger;

public final class WandDebugActions {
    private static final Logger LOGGER = LogUtils.getLogger();
    private static final DustParticleOptions PATH_DONE_DUST = new DustParticleOptions(new Vector3f(0.2f, 0.4f, 1.0f), 1.2f);
    private static final DustParticleOptions PATH_TODO_DUST = new DustParticleOptions(new Vector3f(1.0f, 0.95f, 0.0f), 1.2f);
    private static final int CLEAR_VIZ_RADIUS = 96;

    private WandDebugActions() {
    }

    public static void execute(ServerLevel level, ServerPlayer player, WandDebugActionPayload payload) {
        switch (payload.actionId()) {
            case "building_info": {
                WandDebugActions.handleBuildingInfo(level, player, payload.targetPos());
                break;
            }
            case "rush_construction": {
                WandDebugActions.handleRushConstruction(level, player, payload.targetPos());
                break;
            }
            case "fill_resources": {
                WandDebugActions.handleFillResources(level, player, payload.targetPos());
                break;
            }
            case "recalculate_paths": {
                WandDebugActions.handleRecalculatePaths(level, player, payload.targetPos());
                break;
            }
            case "clear_paths": {
                WandDebugActions.handleClearPaths(level, player, payload.targetPos());
                break;
            }
            case "force_merchant": {
                WandDebugActions.handleForceMerchant(level, player, payload.targetPos());
                break;
            }
            case "villager_info": {
                WandDebugActions.handleVillagerInfo(level, player, payload.targetEntityId());
                break;
            }
            case "grow_child": {
                WandDebugActions.handleGrowChild(level, player, payload.targetEntityId());
                break;
            }
            case "force_child": {
                WandDebugActions.handleForceChild(level, player, payload.targetEntityId());
                break;
            }
            case "visualize_path": {
                WandDebugActions.handleVisualizePath(level, player, payload.targetEntityId());
                break;
            }
            case "visualize_waypoints": {
                WandDebugActions.handleVisualizeWaypoints(level, player, payload.targetEntityId());
                break;
            }
            case "clear_waypoint_blocks": {
                WandDebugActions.handleClearWaypointBlocks(level, player, payload.targetEntityId());
                break;
            }
            case "nav_state": {
                WandDebugActions.handleNavState(level, player, payload.targetEntityId());
                break;
            }
            case "rebuild_waypoint_graph": {
                WandDebugActions.handleRebuildWaypointGraph(level, player, payload.targetEntityId());
                break;
            }
            default: {
                LOGGER.warn("Unknown wand debug action: {}", (Object)payload.actionId());
            }
        }
    }

    @Nullable
    private static BuildingContext resolveBuildingContext(ServerLevel level, ServerPlayer player, BlockPos pos) {
        BlockEntity be = level.getBlockEntity(pos);
        BuildingId buildingId = null;
        if (be instanceof LockedChestBlockEntity) {
            LockedChestBlockEntity chest = (LockedChestBlockEntity)be;
            buildingId = chest.getBuildingId();
        } else if (be instanceof VillagePanelBlockEntity) {
            VillagePanelBlockEntity panel = (VillagePanelBlockEntity)be;
            buildingId = panel.getBuildingId();
        }
        if (buildingId == null) {
            player.sendSystemMessage((Component)Component.literal((String)"\u00a7c[Wand] No building block at target position."));
            return null;
        }
        Village village = VillageSavedData.get(level).getVillageManager().findVillageContaining(buildingId);
        if (village == null) {
            player.sendSystemMessage((Component)Component.literal((String)"\u00a7c[Wand] No village found for this building."));
            return null;
        }
        BuildingInstance building = village.findBuildingById(buildingId);
        if (building == null) {
            player.sendSystemMessage((Component)Component.literal((String)("\u00a7c[Wand] Building not found: " + String.valueOf(buildingId.uuid()))));
            return null;
        }
        return new BuildingContext(village, building);
    }

    private static void handleBuildingInfo(ServerLevel level, ServerPlayer player, BlockPos pos) {
        List<SpecialPoint> signs;
        BuildingContext ctx = WandDebugActions.resolveBuildingContext(level, player, pos);
        if (ctx == null) {
            return;
        }
        BuildingInstance b = ctx.building();
        Village village = ctx.village();
        player.sendSystemMessage((Component)Component.literal((String)"\u00a76\u2550\u2550\u2550 Debug Building \u2550\u2550\u2550"));
        player.sendSystemMessage((Component)Component.literal((String)("\u00a7ePlan: \u00a7f" + b.getPlanId().getPath())));
        if (b.getPlanSetId() != null) {
            player.sendSystemMessage((Component)Component.literal((String)("\u00a7ePlanSet: \u00a7f" + b.getPlanSetId().getPath() + " \u00a77[" + b.getVariant() + " L" + b.getLevel() + "]")));
        }
        player.sendSystemMessage((Component)Component.literal((String)("\u00a7eStatus: \u00a7f" + b.getStatus().name())));
        player.sendSystemMessage((Component)Component.literal((String)("\u00a7eOrigin: \u00a7f" + b.getOrigin().toShortString() + " \u00a7eRotation: \u00a7f" + b.getRotation().name())));
        ConstructionTask task = b.getConstructionTask();
        if (task != null) {
            player.sendSystemMessage((Component)Component.literal((String)("\u00a7eConstruction: \u00a7f" + Math.round(task.progress() * 100.0f) + "% (" + task.getNextStepIndex() + "/" + task.totalSteps() + " steps)")));
        }
        player.sendSystemMessage((Component)Component.literal((String)("\u00a7eSpecial points: \u00a7f" + b.getResolvedPoints().size())));
        if (b.getInventory() != null) {
            player.sendSystemMessage((Component)Component.literal((String)("\u00a7eInventory: \u00a7fpresent (" + b.getInventory().getChestCount() + " chests)")));
        }
        player.sendSystemMessage((Component)Component.literal((String)("\u00a7eVillage: \u00a7f" + village.getVillageName() + " \u00a77(" + village.getBuildings().size() + " buildings)")));
        BlockEntity blockEntity = level.getBlockEntity(pos);
        if (blockEntity instanceof VillagePanelBlockEntity) {
            VillagePanelBlockEntity panelBe = (VillagePanelBlockEntity)blockEntity;
            player.sendSystemMessage((Component)Component.literal((String)("\u00a7ePanelType (BE): \u00a7f" + String.valueOf((Object)panelBe.getPanelType()) + " \u00a77signIdx=" + panelBe.getSignIndex())));
        }
        if (!(signs = b.getPointsByType("signPos")).isEmpty()) {
            int placed = 0;
            int td = 0;
            int bd = 0;
            StringBuilder details = new StringBuilder();
            for (int i = 0; i < signs.size(); ++i) {
                SpecialPoint sp = signs.get(i);
                BlockEntity blockEntity2 = level.getBlockEntity(sp.pos());
                if (!(blockEntity2 instanceof VillagePanelBlockEntity)) continue;
                VillagePanelBlockEntity be = (VillagePanelBlockEntity)blockEntity2;
                ++placed;
                PanelType t = be.getPanelType();
                if (t == PanelType.BUILDING_DEFAULT || t == PanelType.HOUSE) {
                    ++bd;
                } else {
                    ++td;
                }
                if (details.length() > 0) {
                    details.append(", ");
                }
                details.append("#").append(i).append("=").append(t.name());
            }
            player.sendSystemMessage((Component)Component.literal((String)("\u00a7ePanels: \u00a7f" + placed + "/" + signs.size() + " placed, " + td + " TH-like, " + bd + " BUILDING_DEFAULT/HOUSE")));
            if (bd > 0) {
                player.sendSystemMessage((Component)Component.literal((String)("\u00a76  " + String.valueOf(details))));
            }
        }
    }

    private static void handleRushConstruction(ServerLevel level, ServerPlayer player, BlockPos pos) {
        boolean progress;
        BuildingContext ctx = WandDebugActions.resolveBuildingContext(level, player, pos);
        if (ctx == null) {
            return;
        }
        Village village = ctx.village();
        VillageType villageType = ModCultures.getVillageType(village.getVillageTypeId());
        if (villageType == null) {
            player.sendSystemMessage((Component)Component.literal((String)"\u00a7c[Wand] Village type not found."));
            return;
        }
        VillageTerrainMap terrainMap = VillageTerrainMap.compute(level, village.getCenter(), villageType.radius());
        BuildingInstance townhall = village.getTownhall();
        TerrainReachability reachability = null;
        if (townhall != null) {
            reachability = TerrainReachability.compute(terrainMap, townhall.getOrigin());
        }
        HashSet<ResourceLocation> rushExcluded = new HashSet<ResourceLocation>();
        int rushed = 0;
        for (int i = 0; i < 50 && (progress = VillageGrowthManager.rushOneProject(level, village, terrainMap, rushExcluded, reachability)); ++i) {
            ++rushed;
        }
        if (rushed > 0) {
            BuildingFinalizer.applyVillageUpdates(level, village);
        }
        VillageSavedData.get(level).setDirty();
        if (rushed > 0) {
            player.sendSystemMessage((Component)Component.literal((String)("\u00a7a[Wand] Rush: " + rushed + " projects completed.")));
        } else {
            player.sendSystemMessage((Component)Component.literal((String)"\u00a77[Wand] No projects to rush."));
        }
    }

    private static void handleFillResources(ServerLevel level, ServerPlayer player, BlockPos pos) {
        BuildingPlanSet planSet;
        BuildingContext ctx = WandDebugActions.resolveBuildingContext(level, player, pos);
        if (ctx == null) {
            return;
        }
        Village village = ctx.village();
        BuildingInstance building = ctx.building();
        BuildingPlanSet buildingPlanSet = planSet = building.getPlanSetId() != null ? ModCultures.getBuildingPlanSet(building.getPlanSetId()) : null;
        if (planSet == null || !planSet.isTownHall()) {
            player.sendSystemMessage((Component)Component.literal((String)"\u00a7c[Wand] Fill resources only works on the town hall."));
            return;
        }
        BuildingInventory inv = building.getInventory();
        if (inv == null) {
            player.sendSystemMessage((Component)Component.literal((String)"\u00a7c[Wand] Town hall has no inventory."));
            return;
        }
        int totalAdded = 0;
        List<TradeGood> goods = TradeGoodsLoader.getGoods(village.getCultureId());
        for (TradeGood good : goods) {
            Item item;
            if (good.targetQuantity() <= 0 || (item = ItemHelper.resolve(good.item())) == null) continue;
            int added = inv.add((Level)level, item, good.targetQuantity());
            totalAdded += added;
        }
        player.sendSystemMessage((Component)Component.literal((String)("\u00a7a[Wand] Filled " + totalAdded + " items into town hall inventory.")));
    }

    private static void handleRecalculatePaths(ServerLevel level, ServerPlayer player, BlockPos pos) {
        BuildingContext ctx = WandDebugActions.resolveBuildingContext(level, player, pos);
        if (ctx == null) {
            return;
        }
        Village village = ctx.village();
        VillagePathManager pathManager = village.getPathManager();
        if (pathManager == null) {
            player.sendSystemMessage((Component)Component.literal((String)"\u00a7c[Wand] Village has no path manager."));
            return;
        }
        pathManager.recalculatePaths(level, village, true);
        player.sendSystemMessage((Component)Component.literal((String)"\u00a7a[Wand] Paths recalculated (autobuild)."));
    }

    private static void handleClearPaths(ServerLevel level, ServerPlayer player, BlockPos pos) {
        BuildingContext ctx = WandDebugActions.resolveBuildingContext(level, player, pos);
        if (ctx == null) {
            return;
        }
        Village village = ctx.village();
        VillagePathManager pathManager = village.getPathManager();
        if (pathManager == null) {
            player.sendSystemMessage((Component)Component.literal((String)"\u00a7c[Wand] Village has no path manager."));
            return;
        }
        pathManager.clearAllPathsNow(level);
        player.sendSystemMessage((Component)Component.literal((String)"\u00a7a[Wand] All village paths cleared."));
    }

    private static void handleForceMerchant(ServerLevel level, ServerPlayer player, BlockPos pos) {
        BuildingContext ctx = WandDebugActions.resolveBuildingContext(level, player, pos);
        if (ctx == null) {
            return;
        }
        Village village = ctx.village();
        LocalMerchantHelper.forceAttemptMerchantMoves(level, village);
        player.sendSystemMessage((Component)Component.literal((String)("\u00a7a[Wand] Forced merchant move attempts for " + village.getVillageName() + ".")));
    }

    @Nullable
    private static MillVillager resolveVillager(ServerLevel level, ServerPlayer player, int entityId) {
        Entity entity = level.getEntity(entityId);
        if (!(entity instanceof MillVillager)) {
            player.sendSystemMessage((Component)Component.literal((String)"\u00a7c[Wand] Target is not a Millenaire villager."));
            return null;
        }
        MillVillager villager = (MillVillager)entity;
        return villager;
    }

    private static void handleVillagerInfo(ServerLevel level, ServerPlayer player, int entityId) {
        GoalScheduler scheduler;
        VillagerTask currentTask;
        BuildingId home;
        VillageManager vm;
        Village village;
        MillVillager villager = WandDebugActions.resolveVillager(level, player, entityId);
        if (villager == null) {
            return;
        }
        player.sendSystemMessage((Component)Component.literal((String)"\u00a76\u2550\u2550\u2550 Debug Villager \u2550\u2550\u2550"));
        player.sendSystemMessage((Component)Component.literal((String)("\u00a7eName: \u00a7f" + villager.getVillagerDisplayName())));
        ResourceLocation debugTypeId = villager.getVillagerTypeId();
        player.sendSystemMessage((Component)Component.literal((String)("\u00a7eType: \u00a7f" + (debugTypeId != null ? debugTypeId.getPath() : "unknown"))));
        player.sendSystemMessage((Component)Component.literal((String)("\u00a7ePos: \u00a7f" + villager.blockPosition().toShortString())));
        VillagerType vType = ModCultures.getVillagerType(villager.getVillagerTypeId());
        if (vType != null) {
            player.sendSystemMessage((Component)Component.literal((String)("\u00a7eChild: \u00a7f" + vType.isChild())));
        }
        if (villager.getVillageId() != null && (village = (vm = VillageSavedData.get(level).getVillageManager()).getVillage(villager.getVillageId())) != null) {
            player.sendSystemMessage((Component)Component.literal((String)("\u00a7eVillage: \u00a7f" + village.getVillageName())));
        }
        if ((home = villager.getHomeBuilding()) != null) {
            player.sendSystemMessage((Component)Component.literal((String)("\u00a7eHome: \u00a7f" + home.uuid().toString().substring(0, 8))));
        }
        if ((currentTask = (scheduler = villager.getGoalScheduler()).getCurrentTask()) != null) {
            player.sendSystemMessage((Component)Component.literal((String)("\u00a7eGoal: \u00a7f" + currentTask.goalId().getPath())));
        } else {
            player.sendSystemMessage((Component)Component.literal((String)"\u00a7eGoal: \u00a77(idle)"));
        }
    }

    private static void handleGrowChild(ServerLevel level, ServerPlayer player, int entityId) {
        MillVillager villager = WandDebugActions.resolveVillager(level, player, entityId);
        if (villager == null) {
            return;
        }
        VillagerType vType = ModCultures.getVillagerType(villager.getVillagerTypeId());
        if (vType == null || !vType.isChild()) {
            player.sendSystemMessage((Component)Component.literal((String)"\u00a7c[Wand] This villager is not a child."));
            return;
        }
        villager.setChildSize(20);
        player.sendSystemMessage((Component)Component.literal((String)("\u00a7a[Wand] Child " + villager.getVillagerDisplayName() + " grown to adult size.")));
    }

    private static void handleForceChild(ServerLevel level, ServerPlayer player, int entityId) {
        MillVillager villager = WandDebugActions.resolveVillager(level, player, entityId);
        if (villager == null) {
            return;
        }
        VillagerType vType = ModCultures.getVillagerType(villager.getVillagerTypeId());
        if (vType == null) {
            player.sendSystemMessage((Component)Component.literal((String)"\u00a7c[Wand] Unknown villager type."));
            return;
        }
        if (villager.getVillageId() == null) {
            player.sendSystemMessage((Component)Component.literal((String)"\u00a7c[Wand] Villager has no village."));
            return;
        }
        VillageManager vm = VillageSavedData.get(level).getVillageManager();
        Village village = vm.getVillage(villager.getVillageId());
        if (village == null) {
            player.sendSystemMessage((Component)Component.literal((String)"\u00a7c[Wand] Village not found."));
            return;
        }
        NightActionHelper.forceSpawnChild(level, village, villager, vType);
        player.sendSystemMessage((Component)Component.literal((String)("\u00a7a[Wand] Forced child spawn for " + villager.getVillagerDisplayName() + ".")));
    }

    private static void handleVisualizePath(ServerLevel level, ServerPlayer player, int entityId) {
        MillVillager villager = WandDebugActions.resolveVillager(level, player, entityId);
        if (villager == null) {
            return;
        }
        Path path = villager.getNavigation().getPath();
        if (path == null) {
            player.sendSystemMessage((Component)Component.literal((String)"\u00a77[Wand] Villager has no active path (getNavigation().getPath() == null)."));
            return;
        }
        int total = path.getNodeCount();
        int next = path.getNextNodeIndex();
        BlockPos target = path.getTarget();
        BlockPos endNode = total > 0 ? path.getEndNode().asBlockPos() : null;
        player.sendSystemMessage((Component)Component.literal((String)"\u00a76\u2550\u2550\u2550 Path snapshot \u2550\u2550\u2550"));
        player.sendSystemMessage((Component)Component.literal((String)String.format("\u00a7eNodes: \u00a7f%d total\u00a77, \u00a7fnext index = %d \u00a77(%s)", total, next, next >= total ? "\u00a7cpath consumed \u2014 nav thinks it's arrived" : "\u00a7a" + (total - next) + " remaining")));
        if (target != null) {
            player.sendSystemMessage((Component)Component.literal((String)("\u00a7eTarget (moveTo): \u00a7f" + target.toShortString())));
        }
        if (endNode != null) {
            player.sendSystemMessage((Component)Component.literal((String)("\u00a7eEnd node:        \u00a7f" + endNode.toShortString())));
        }
        if (total > 0) {
            int firstShown = Math.min(3, total);
            StringBuilder firstNodes = new StringBuilder();
            for (int i = 0; i < firstShown; ++i) {
                if (i > 0) {
                    firstNodes.append(", ");
                }
                firstNodes.append(path.getNode(i).asBlockPos().toShortString());
            }
            player.sendSystemMessage((Component)Component.literal((String)("\u00a7eFirst nodes: \u00a77" + String.valueOf(firstNodes))));
            if (total > firstShown + 3) {
                int last = Math.max(total - 3, firstShown);
                StringBuilder lastNodes = new StringBuilder();
                for (int i = last; i < total; ++i) {
                    if (i > last) {
                        lastNodes.append(", ");
                    }
                    lastNodes.append(path.getNode(i).asBlockPos().toShortString());
                }
                player.sendSystemMessage((Component)Component.literal((String)("\u00a7eLast nodes:  \u00a77" + String.valueOf(lastNodes))));
            }
        }
        for (int burst = 0; burst < 2; ++burst) {
            for (int i = 0; i < total; ++i) {
                BlockPos nodePos = path.getNode(i).asBlockPos();
                DustParticleOptions dust = i < next ? PATH_DONE_DUST : PATH_TODO_DUST;
                level.sendParticles(player, (ParticleOptions)dust, true, (double)nodePos.getX() + 0.5, (double)nodePos.getY() + 1.2, (double)nodePos.getZ() + 0.5, 3, 0.15, 0.05, 0.15, 0.0);
            }
        }
        player.sendSystemMessage((Component)Component.literal((String)String.format("\u00a77[Wand] Particles: \u00a79blue\u00a77 = traversed, \u00a7eyellow\u00a77 = remaining (visible ~2s).", new Object[0])));
    }

    private static void handleVisualizeWaypoints(ServerLevel level, ServerPlayer player, int entityId) {
        MillVillager villager = WandDebugActions.resolveVillager(level, player, entityId);
        if (villager == null) {
            return;
        }
        if (villager.getVillageId() == null) {
            player.sendSystemMessage((Component)Component.literal((String)"\u00a7c[Wand] Villager has no village."));
            return;
        }
        VillageManager vm = VillageSavedData.get(level).getVillageManager();
        Village village = vm.getVillage(villager.getVillageId());
        if (village == null) {
            player.sendSystemMessage((Component)Component.literal((String)"\u00a7c[Wand] Village not found."));
            return;
        }
        if (village.getWaypointGraph().getWaypoints().isEmpty()) {
            player.sendSystemMessage((Component)Component.literal((String)"\u00a77[Wand] Waypoint graph is empty."));
            return;
        }
        WaypointVisualizationManager.toggle(player, level, village);
    }

    private static void handleClearWaypointBlocks(ServerLevel level, ServerPlayer player, int entityId) {
        MillVillager villager = WandDebugActions.resolveVillager(level, player, entityId);
        if (villager == null) {
            return;
        }
        BlockPos origin = villager.blockPosition();
        int removed = 0;
        for (int dx = -96; dx <= 96; ++dx) {
            for (int dz = -96; dz <= 96; ++dz) {
                for (int dy = -8; dy <= 8; ++dy) {
                    BlockState state;
                    BlockPos pos = origin.offset(dx, dy, dz);
                    if (!level.isLoaded(pos) || !(state = level.getBlockState(pos)).is(Blocks.REDSTONE_BLOCK) && !state.is(Blocks.SEA_LANTERN)) continue;
                    level.setBlock(pos, Blocks.AIR.defaultBlockState(), 3);
                    ++removed;
                }
            }
        }
        player.sendSystemMessage((Component)Component.literal((String)String.format("\u00a7a[Wand] Removed %d viz blocks (redstone + sea lanterns) within %d blocks.", removed, 96)));
    }

    private static void handleNavState(ServerLevel level, ServerPlayer player, int entityId) {
        Map<String, String> taskInfo;
        WaypointNavigator waypointNavigator;
        MillVillager villager = WandDebugActions.resolveVillager(level, player, entityId);
        if (villager == null) {
            return;
        }
        VillagerNavDriver nav = villager.getNavManager();
        VillagerNavDriver.NavDiagnostics diag = nav.getDiagnostics();
        GoalScheduler scheduler = villager.getGoalScheduler();
        VillagerTask task = scheduler != null ? scheduler.getCurrentTask() : null;
        boolean hasPath = villager.getNavigation().getPath() != null && !villager.getNavigation().isDone();
        BlockPos dest = nav.getDestination();
        if (nav instanceof VillagerNavigationManager) {
            VillagerNavigationManager vnm = (VillagerNavigationManager)nav;
            waypointNavigator = vnm.getWaypointNavigator();
        } else {
            waypointNavigator = null;
        }
        WaypointNavigator wpn = waypointNavigator;
        player.sendSystemMessage((Component)Component.literal((String)"\u00a76\u2550\u2550\u2550 NavManager State \u2550\u2550\u2550"));
        player.sendSystemMessage((Component)Component.literal((String)("\u00a7eVillager: \u00a7f" + villager.getVillagerDisplayName() + " \u00a77at " + villager.blockPosition().toShortString())));
        player.sendSystemMessage((Component)Component.literal((String)("\u00a7eGoal: \u00a7f" + (task != null ? task.goalId().getPath() : "(idle)"))));
        player.sendSystemMessage((Component)Component.literal((String)("\u00a7eDestination: \u00a7f" + (dest != null ? dest.toShortString() : "null"))));
        player.sendSystemMessage((Component)Component.literal((String)("\u00a7eHas active path: \u00a7f" + hasPath)));
        player.sendSystemMessage((Component)Component.literal((String)("\u00a7eAbandoned: \u00a7f" + nav.isAbandoned() + "  \u00a7eteleports: \u00a7f" + diag.teleportCount())));
        player.sendSystemMessage((Component)Component.literal((String)("\u00a7eStuck (local/long): \u00a7f" + diag.localStuck() + " / " + diag.longDistanceStuck())));
        if (wpn != null) {
            player.sendSystemMessage((Component)Component.literal((String)("\u00a7eWaypointNavigator: \u00a7f" + wpn.getState().name() + " \u00a77(idx " + wpn.getDebugWaypointIndex() + "/" + wpn.getDebugPathSize() + ", stuckTicks=" + wpn.getDebugStuckTicks() + ", tp=" + wpn.getDebugTeleportCount() + ")")));
        } else {
            player.sendSystemMessage((Component)Component.literal((String)"\u00a7eWaypointNavigator: \u00a77(none)"));
        }
        if (task != null && (taskInfo = task.getNavDebugInfo()) != null && !taskInfo.isEmpty()) {
            player.sendSystemMessage((Component)Component.literal((String)"\u00a7eTask debug:"));
            for (Map.Entry<String, String> entry : taskInfo.entrySet()) {
                player.sendSystemMessage((Component)Component.literal((String)("  \u00a77" + entry.getKey() + "\u00a7f=\u00a7f" + entry.getValue())));
            }
        }
    }

    private static void handleRebuildWaypointGraph(ServerLevel level, ServerPlayer player, int entityId) {
        MillVillager villager = WandDebugActions.resolveVillager(level, player, entityId);
        if (villager == null) {
            return;
        }
        if (villager.getVillageId() == null) {
            player.sendSystemMessage((Component)Component.literal((String)"\u00a7c[Wand] Villager has no village."));
            return;
        }
        VillageManager vm = VillageSavedData.get(level).getVillageManager();
        Village village = vm.getVillage(villager.getVillageId());
        if (village == null) {
            player.sendSystemMessage((Component)Component.literal((String)"\u00a7c[Wand] Village not found."));
            return;
        }
        long start = System.nanoTime();
        village.rebuildWaypointGraph(level);
        long elapsedMs = (System.nanoTime() - start) / 1000000L;
        VillageWaypointGraph graph = village.getWaypointGraph();
        int nodes = graph.waypointCount();
        int edges = graph.getEdges().size();
        player.sendSystemMessage((Component)Component.literal((String)String.format("\u00a7a[Wand] Waypoint graph rebuilt: \u00a7f%d nodes, %d edges \u00a77(in %dms). See server log for details.", nodes, edges, elapsedMs)));
    }

    private record BuildingContext(Village village, BuildingInstance building) {
    }
}

