/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  javax.annotation.Nullable
 *  net.minecraft.network.chat.Component
 *  net.minecraft.resources.ResourceLocation
 *  net.minecraft.server.level.ServerLevel
 *  net.minecraft.server.level.ServerPlayer
 *  net.minecraft.tags.TagKey
 *  net.minecraft.world.entity.Entity
 *  net.minecraft.world.item.Item
 *  net.minecraft.world.item.ItemStack
 *  net.minecraft.world.level.ChunkPos
 *  net.minecraft.world.level.Level
 */
package org.millenaire.village.panel;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.UUID;
import javax.annotation.Nullable;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.TagKey;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import org.millenaire.block.VillagePanelBlockEntity;
import org.millenaire.building.AnywoodHelper;
import org.millenaire.building.BuildingId;
import org.millenaire.building.BuildingInstance;
import org.millenaire.building.BuildingInventory;
import org.millenaire.building.BuildingPlan;
import org.millenaire.building.BuildingPlanSet;
import org.millenaire.building.ClearMargins;
import org.millenaire.building.ConstructionTask;
import org.millenaire.culture.ModCultures;
import org.millenaire.culture.VillageType;
import org.millenaire.culture.VillagerType;
import org.millenaire.entity.MillVillager;
import org.millenaire.goal.GoalScheduler;
import org.millenaire.item.ItemHelper;
import org.millenaire.language.BuildingNameHelper;
import org.millenaire.language.LanguageHelper;
import org.millenaire.network.MapData;
import org.millenaire.network.PanelContentPayload;
import org.millenaire.village.Village;
import org.millenaire.village.panel.BuildingPanelGenerator;
import org.millenaire.village.panel.CommercePanelGenerator;
import org.millenaire.village.panel.ConstructionPanelGenerator;
import org.millenaire.village.panel.MarvelPanelGenerator;
import org.millenaire.village.panel.MilitaryPanelGenerator;
import org.millenaire.village.panel.PanelContent;
import org.millenaire.village.panel.PanelHelper;
import org.millenaire.village.panel.PanelLine;
import org.millenaire.village.panel.PanelType;
import org.millenaire.village.panel.VillageMapPanelGenerator;
import org.millenaire.village.panel.VillageOverviewPanelGenerator;
import org.millenaire.village.path.VillagePathManager;
import org.millenaire.world.VillageTerrainMap;

public final class PanelContentGenerator {
    private static final int MAX_WALL_SEGMENT_LINES = 5;

    private PanelContentGenerator() {
    }

    public static PanelContent generate(PanelType type, Village village, @Nullable BuildingInstance building, @Nullable ServerLevel level, int signIndex, @Nullable ServerPlayer player) {
        return switch (type) {
            default -> throw new MatchException(null, null);
            case PanelType.VILLAGE_SUMMARY -> VillageOverviewPanelGenerator.generateSummary(village, building, level);
            case PanelType.POPULATION -> VillageOverviewPanelGenerator.generatePopulation(village, level);
            case PanelType.CONSTRUCTIONS -> ConstructionPanelGenerator.generateConstructions(village, player);
            case PanelType.PROJECTS -> ConstructionPanelGenerator.generateProjects(village, player);
            case PanelType.HOUSE, PanelType.BUILDING_DEFAULT -> BuildingPanelGenerator.generateBuildingDefault(village, building, level);
            case PanelType.RESOURCES -> ConstructionPanelGenerator.generateResources(village, level);
            case PanelType.VILLAGE_MAP -> VillageMapPanelGenerator.generateVillageMap(village);
            case PanelType.MILITARY -> MilitaryPanelGenerator.generateMilitary(village, level);
            case PanelType.ARCHIVES -> BuildingPanelGenerator.generateArchives(village, building, level, signIndex);
            case PanelType.INN_VISITORS -> CommercePanelGenerator.generateInnVisitors(village, building);
            case PanelType.INN_TRADE_GOODS -> CommercePanelGenerator.generateInnGoods(village, building);
            case PanelType.MARKET_MERCHANTS -> CommercePanelGenerator.generateVisitors(village, building, true);
            case PanelType.VISITORS -> CommercePanelGenerator.generateVisitors(village, building, false);
            case PanelType.WALLS -> PanelContentGenerator.generateWalls(village);
            case PanelType.CONTROLLED_PROJECTS -> PanelContentGenerator.generateStubControlled(village, PanelType.CONTROLLED_PROJECTS, "panel.millenaire.panel_type.controlled_projects");
            case PanelType.CONTROLLED_MILITARY -> PanelContentGenerator.generateStubControlled(village, PanelType.CONTROLLED_MILITARY, "panel.millenaire.panel_type.controlled_military");
            case PanelType.MARVEL_PROJECTS -> MarvelPanelGenerator.generateMarvelProjects(village);
            case PanelType.MARVEL_DONATIONS -> MarvelPanelGenerator.generateMarvelDonations(village);
            case PanelType.MARVEL_RESOURCES -> MarvelPanelGenerator.generateMarvelResources(village, level);
            case PanelType.HALL_OF_FAME -> new PanelContent(PanelType.HALL_OF_FAME, "panel.millenaire.panel_type.hall_of_fame", List.of(), true, null);
            case PanelType.CHRONICLE -> VillageOverviewPanelGenerator.generateChronicle(village);
        };
    }

    public static PanelContent buildHoFContent(VillagePanelBlockEntity panel) {
        ArrayList<PanelLine> lines = new ArrayList<PanelLine>();
        for (DisplayLine dl : panel.getDisplayLines()) {
            if (dl.translatable()) {
                lines.add(PanelLine.translatable(dl.text()));
                continue;
            }
            lines.add(PanelLine.text(dl.text()));
        }
        return new PanelContent(PanelType.HALL_OF_FAME, "panel.millenaire.panel_type.hall_of_fame", lines);
    }

    static ResourcePanelData collectResourcePanelData(Village village) {
        Village.PendingProject pending = village.getPendingProject();
        if (pending != null) {
            String projectName;
            BuildingPlanSet planSet = ModCultures.getBuildingPlanSet(pending.planSetId());
            String string = projectName = planSet != null ? planSet.nativeName() : pending.planSetId().getPath();
            if (planSet != null) {
                BuildingPlanSet.LevelDef levelDef = planSet.getLevel(pending.variant(), pending.level());
                Map<ResourceLocation, Integer> reqs = levelDef != null && levelDef.requiredResources() != null ? levelDef.requiredResources() : Map.of();
                return new ResourcePanelData(projectName, pending.isUpgrade(), pending.level(), false, reqs);
            }
            return new ResourcePanelData(projectName, pending.isUpgrade(), pending.level(), false, Map.of());
        }
        for (BuildingInstance b : village.getBuildings()) {
            BuildingPlanSet planSet;
            if (!b.isBeingBuilt() || b.getPlanSetId() == null || b.getVariant() == null || (planSet = ModCultures.getBuildingPlanSet(b.getPlanSetId())) == null) continue;
            String projectName = BuildingNameHelper.getServerFallbackName(b);
            boolean isUpgrade = b.getStatus() == BuildingInstance.Status.UPGRADING;
            BuildingPlanSet.LevelDef levelDef = planSet.getLevel(b.getVariant(), b.getLevel());
            Map<ResourceLocation, Integer> reqs = levelDef != null && levelDef.requiredResources() != null ? levelDef.requiredResources() : Map.of();
            return new ResourcePanelData(projectName, isUpgrade, b.getLevel(), true, reqs);
        }
        return ResourcePanelData.EMPTY;
    }

    private static PanelContent generateWalls(Village village) {
        String[] titleArgs = new String[]{village.getVillageName()};
        int planned = 0;
        int underConstruction = 0;
        int complete = 0;
        int upgrading = 0;
        TreeMap<Integer, Integer> levelCounts = new TreeMap<Integer, Integer>();
        ArrayList<BuildingInstance> activeSegments = new ArrayList<BuildingInstance>();
        for (BuildingInstance b : village.getBuildings()) {
            if (!b.isWallSegment()) continue;
            switch (b.getStatus()) {
                case PLANNED: {
                    ++planned;
                    break;
                }
                case UNDER_CONSTRUCTION: {
                    ++underConstruction;
                    activeSegments.add(b);
                    break;
                }
                case COMPLETE: {
                    ++complete;
                    levelCounts.merge(b.getLevel(), 1, Integer::sum);
                    break;
                }
                case UPGRADING: {
                    ++complete;
                    ++upgrading;
                    levelCounts.merge(b.getLevel(), 1, Integer::sum);
                    activeSegments.add(b);
                }
            }
        }
        int total = planned + underConstruction + complete;
        ArrayList<PanelLine> lines = new ArrayList<PanelLine>();
        if (total == 0) {
            lines.add(PanelLine.translatableColored("panel.millenaire.no_walls", 0x555555));
            return new PanelContent(PanelType.WALLS, "panel.millenaire.title.walls", lines, true, titleArgs);
        }
        lines.add(PanelLine.translatableWithMixedArgsColored("panel.millenaire.format.label_value", 0, 1, "panel.millenaire.walls_built", complete + "/" + total));
        int activeWork = underConstruction + upgrading;
        if (activeWork > 0) {
            lines.add(PanelLine.translatableWithMixedArgsColored("panel.millenaire.format.label_value", 0, 1, "panel.millenaire.walls_in_construction", String.valueOf(activeWork)));
        }
        if (planned > 0) {
            lines.add(PanelLine.translatableWithMixedArgsColored("panel.millenaire.format.label_value", 0x555555, 1, "panel.millenaire.walls_planned", String.valueOf(planned)));
        }
        if (levelCounts.size() > 1) {
            StringBuilder levels = new StringBuilder();
            for (Map.Entry e : levelCounts.entrySet()) {
                if (!levels.isEmpty()) {
                    levels.append(", ");
                }
                levels.append("L").append(e.getKey()).append("=").append(e.getValue());
            }
            lines.add(PanelLine.translatableWithMixedArgsColored("panel.millenaire.format.label_value", 0x555555, 1, "panel.millenaire.walls_levels", levels.toString()));
        }
        if (!activeSegments.isEmpty()) {
            lines.add(PanelLine.separator());
            int shown = 0;
            for (BuildingInstance b : activeSegments) {
                if (shown >= 5) {
                    lines.add(PanelLine.colored("  \u2026", 0x555555));
                    break;
                }
                int progress = 0;
                ConstructionTask task = b.getConstructionTask();
                if (task != null) {
                    progress = Math.round(task.progress() * 100.0f);
                }
                PanelHelper.DirectionInfo dir = PanelHelper.computeDirectionInfo(village.getCenter(), b.getOrigin());
                if (b.getStatus() == BuildingInstance.Status.UPGRADING) {
                    if (dir.atCenter()) {
                        lines.add(PanelLine.translatableWithArgs("panel.millenaire.construction_detail_upgrading_center", String.valueOf(b.getLevel()), String.valueOf(progress)));
                    } else {
                        lines.add(PanelLine.translatableWithMixedArgs("panel.millenaire.construction_detail_upgrading_dir", 8, String.valueOf(b.getLevel()), String.valueOf(progress), String.valueOf(dir.distance()), dir.cardinalKey()));
                    }
                } else if (dir.atCenter()) {
                    lines.add(PanelLine.translatableWithArgs("panel.millenaire.construction_detail_building_center", String.valueOf(progress)));
                } else {
                    lines.add(PanelLine.translatableWithMixedArgs("panel.millenaire.construction_detail_building_dir", 4, String.valueOf(progress), String.valueOf(dir.distance()), dir.cardinalKey()));
                }
                ++shown;
            }
        }
        return new PanelContent(PanelType.WALLS, "panel.millenaire.title.walls", lines, true, titleArgs);
    }

    private static PanelContent generateStubControlled(Village village, PanelType type, String typeKey) {
        ArrayList<PanelLine> lines = new ArrayList<PanelLine>();
        String ownerName = village.getOwnerName();
        if (ownerName != null) {
            lines.add(PanelLine.text(ownerName));
        } else {
            lines.add(PanelLine.translatable("panel.millenaire.not_controlled"));
        }
        return new PanelContent(type, typeKey, lines, true, null);
    }

    public static PanelContentPayload createMapPayload(PanelContent content, Village village, ServerLevel level, ServerPlayer player) {
        Village.PendingProject pending = village.getPendingProject();
        BuildingId pendingUpgradeTarget = pending != null && pending.isUpgrade() ? pending.buildingId() : null;
        ArrayList<MapData.MapBuilding> buildings = new ArrayList<MapData.MapBuilding>();
        for (BuildingInstance buildingInstance : village.getBuildings()) {
            BuildingPlanSet planSet;
            boolean nameTranslatable;
            String buildingName;
            BuildingPlanSet bps;
            if (buildingInstance.isSubBuilding()) continue;
            BuildingPlan buildingPlan = ModCultures.getBuildingPlan(buildingInstance.getPlanId());
            int w = buildingPlan != null ? buildingPlan.width() : 5;
            int d = buildingPlan != null ? buildingPlan.depth() : 5;
            VillageTerrainMap.FootprintRect rect = VillageTerrainMap.computeFootprintRect(buildingInstance.getOrigin().getX(), buildingInstance.getOrigin().getZ(), w, d, ClearMargins.symmetric(0), buildingInstance.getRotation());
            boolean canRead = LanguageHelper.canReadBuildingNames(player, village.getCultureId());
            String nativePrefix = null;
            BuildingPlanSet buildingPlanSet = bps = buildingInstance.getPlanSetId() != null ? ModCultures.getBuildingPlanSet(buildingInstance.getPlanSetId()) : null;
            if (canRead) {
                buildingName = PanelHelper.getBuildingTranslationKey(buildingInstance);
                nameTranslatable = true;
                nativePrefix = bps != null ? bps.nativeName() : null;
            } else {
                buildingName = BuildingNameHelper.getServerFallbackName(buildingInstance);
                nameTranslatable = false;
            }
            String status = buildingInstance.getStatus() == BuildingInstance.Status.COMPLETE ? (pendingUpgradeTarget != null && buildingInstance.getId().equals(pendingUpgradeTarget) ? "PENDING" : ((planSet = ModCultures.getBuildingPlanSet(buildingInstance.getPlanSetId())) != null && planSet.hasNextLevel(buildingInstance.getVariant(), buildingInstance.getLevel()) ? "IDLE" : "COMPLETE")) : buildingInstance.getStatus().name();
            boolean isWall = buildingInstance.isWallSegment();
            buildings.add(new MapData.MapBuilding(rect.startX(), rect.startZ(), rect.width(), rect.depth(), buildingName, status, buildingInstance.getLevel(), nameTranslatable, nativePrefix, isWall));
        }
        ArrayList<MapData.MapVillager> villagers = new ArrayList<MapData.MapVillager>();
        for (Map.Entry<UUID, ResourceLocation> entry : village.getVillagerTypes().entrySet()) {
            Entity entity = level.getEntity(entry.getKey());
            if (!(entity instanceof MillVillager)) continue;
            MillVillager mv = (MillVillager)entity;
            String name = mv.getFirstName() != null ? mv.getFirstName() : "???";
            VillagerType vtype = ModCultures.getVillagerType(entry.getValue());
            String gender = vtype != null && vtype.isChild() ? "CHILD" : (vtype != null ? vtype.gender().name() : "MALE");
            String roleKey = PanelHelper.resolveRoleKey(entry.getValue());
            String goalLabel = mv.getGoalLabel() != null ? mv.getGoalLabel() : "";
            villagers.add(new MapData.MapVillager(mv.blockPosition().getX(), mv.blockPosition().getZ(), name, gender, roleKey, goalLabel, mv.isChief()));
        }
        MapData.MapTerrain mapTerrain = PanelContentGenerator.computeTerrainGrid(village, level);
        ArrayList<MapData.MapChunk> arrayList = new ArrayList<MapData.MapChunk>();
        Set<ChunkPos> loadedChunks = village.getLoadedChunks();
        Set<ChunkPos> expectedChunks = village.computeVillageChunks();
        for (ChunkPos cp : expectedChunks) {
            arrayList.add(new MapData.MapChunk(cp.x, cp.z, loadedChunks.contains(cp)));
        }
        ArrayList<MapData.MapPath> paths = new ArrayList<MapData.MapPath>();
        VillagePathManager pm = village.getPathManager();
        if (pm != null) {
            pm.forEachPath((pos, level2) -> paths.add(new MapData.MapPath(pos.getX(), pos.getZ(), (byte)level2.intValue())));
        }
        return PanelContentPayload.fromContentWithMap(content, buildings, villagers, player.blockPosition().getX(), player.blockPosition().getZ(), village.getCenter().getX(), village.getCenter().getZ(), mapTerrain, arrayList, paths);
    }

    private static MapData.MapTerrain computeTerrainGrid(Village village, ServerLevel level) {
        VillageType vt = ModCultures.getVillageType(village.getVillageTypeId());
        int radius = vt != null ? vt.radius() : 90;
        VillageTerrainMap terrainMap = VillageTerrainMap.compute(level, village.getCenter(), radius);
        int size = terrainMap.getSize();
        int originX = village.getCenter().getX() - radius;
        int originZ = village.getCenter().getZ() - radius;
        byte[] data = new byte[size * size];
        for (int lx = 0; lx < size; ++lx) {
            for (int lz = 0; lz < size; ++lz) {
                int tile = terrainMap.isWaterAt(lx, lz) ? 1 : (terrainMap.isDangerAt(lx, lz) ? 2 : (terrainMap.isBuildingForbiddenAt(lx, lz) ? 3 : (terrainMap.isOccupied(lx, lz) ? 6 : (!terrainMap.canBuildAt(lx, lz) ? 4 : 5))));
                data[lx * size + lz] = tile;
            }
        }
        return new MapData.MapTerrain(originX, originZ, size, size, data);
    }

    public static DisplayData generateDisplayLines(PanelType type, Village village, @Nullable BuildingInstance building, @Nullable ServerLevel level, int signIndex) {
        DisplayData data = new DisplayData(new ArrayList<DisplayLine>());
        switch (type) {
            case VILLAGE_SUMMARY: {
                VillageOverviewPanelGenerator.addSummary3D(data, village);
                break;
            }
            case HOUSE: {
                if (building == null) break;
                BuildingPanelGenerator.addBuildingDefault3D(data, village, building, level);
                break;
            }
            case CONSTRUCTIONS: {
                ConstructionPanelGenerator.addConstructions3D(data, village);
                break;
            }
            case POPULATION: {
                VillageOverviewPanelGenerator.addPopulation3D(data, village);
                break;
            }
            case PROJECTS: {
                ConstructionPanelGenerator.addProjects3D(data, village);
                break;
            }
            case RESOURCES: {
                ConstructionPanelGenerator.addResources3D(data, village, level);
                break;
            }
            case MILITARY: {
                MilitaryPanelGenerator.addMilitary3D(data, village);
                break;
            }
            case VILLAGE_MAP: {
                VillageMapPanelGenerator.addVillageMap3D(data, village);
                break;
            }
            case ARCHIVES: {
                if (building != null && level != null) {
                    BuildingPanelGenerator.addArchives3D(data, village, building, level, signIndex);
                    break;
                }
                data.addCentered(PanelHelper.t("panel.millenaire.reserved_for_future", ""));
                break;
            }
            case WALLS: {
                data.addCenteredTranslatable("panel.millenaire.panel_type.walls");
                data.addCentered("");
                int wallsPlanned = 0;
                int wallsUnderConstruction = 0;
                int wallsComplete = 0;
                int wallsUpgrading = 0;
                for (BuildingInstance b : village.getBuildings()) {
                    if (!b.isWallSegment()) continue;
                    switch (b.getStatus()) {
                        case PLANNED: {
                            ++wallsPlanned;
                            break;
                        }
                        case UNDER_CONSTRUCTION: {
                            ++wallsUnderConstruction;
                            break;
                        }
                        case COMPLETE: {
                            ++wallsComplete;
                            break;
                        }
                        case UPGRADING: {
                            ++wallsComplete;
                            ++wallsUpgrading;
                        }
                    }
                }
                int wallsTotal = wallsPlanned + wallsUnderConstruction + wallsComplete;
                if (wallsTotal == 0) {
                    data.addCenteredTranslatable("panel.millenaire.no_walls");
                    break;
                }
                data.addCentered(PanelHelper.t("panel.millenaire.walls_built") + ": " + wallsComplete + "/" + wallsTotal);
                int wallsActiveWork = wallsUnderConstruction + wallsUpgrading;
                if (wallsActiveWork > 0) {
                    data.addCentered(PanelHelper.t("panel.millenaire.walls_in_construction") + ": " + wallsActiveWork);
                }
                if (wallsPlanned <= 0) break;
                data.addCentered(PanelHelper.t("panel.millenaire.walls_planned") + ": " + wallsPlanned);
                break;
            }
            case INN_VISITORS: 
            case INN_TRADE_GOODS: 
            case MARKET_MERCHANTS: 
            case VISITORS: {
                CommercePanelGenerator.addCommerce3D(data, type, village, building);
                break;
            }
            case MARVEL_PROJECTS: {
                MarvelPanelGenerator.addMarvelProjects3D(data, village);
                break;
            }
            case MARVEL_DONATIONS: {
                MarvelPanelGenerator.addMarvelDonations3D(data, village);
                break;
            }
            case MARVEL_RESOURCES: {
                MarvelPanelGenerator.addMarvelResources3D(data, village, level);
                break;
            }
            case BUILDING_DEFAULT: {
                if (building == null) break;
                BuildingPanelGenerator.addBuildingDefault3D(data, village, building, level);
                break;
            }
            case HALL_OF_FAME: {
                break;
            }
            default: {
                data.addCentered(PanelHelper.t("panel.millenaire.panel_type." + type.name().toLowerCase(Locale.ROOT)));
                if (building == null) break;
                data.addCenteredBuildingName(PanelHelper.getBuildingTranslationKey(building), BuildingNameHelper.getServerFallbackName(building));
            }
        }
        if (data.displayLines().size() > 8) {
            return new DisplayData(new ArrayList<DisplayLine>(data.displayLines().subList(0, 8)));
        }
        return data;
    }

    static void addResourceLines(List<PanelLine> lines, Map<ResourceLocation, Integer> required, Village village, @Nullable ServerLevel level) {
        PanelContentGenerator.addResourceLines(lines, required, village, level, true);
    }

    static void addResourceLines(List<PanelLine> lines, Map<ResourceLocation, Integer> required, Village village, @Nullable ServerLevel level, boolean includeBuilders) {
        int have;
        BuildingInventory inv;
        BuildingInstance townhall = village.getTownhall();
        BuildingInventory buildingInventory = inv = townhall != null ? townhall.getInventory() : null;
        if (inv != null) {
            inv.invalidateCache();
        }
        TreeMap<String, ResourceLineInfo> sorted = new TreeMap<String, ResourceLineInfo>();
        for (Map.Entry<ResourceLocation, Integer> entry : required.entrySet()) {
            Item item;
            String itemName;
            int need;
            if (entry.getKey().getPath().startsWith("mock_")) continue;
            if (AnywoodHelper.isAnywood(entry.getKey())) {
                need = entry.getValue();
                have = 0;
                if (inv != null && level != null) {
                    ServerLevel lvl = level;
                    BuildingInventory thInv = inv;
                    int rawCount = thInv.getCountByTag((Level)lvl, AnywoodHelper.LOGS_TAG);
                    if (includeBuilders) {
                        rawCount += PanelContentGenerator.countBuildersInventoryByTag(village, lvl, AnywoodHelper.LOGS_TAG);
                    }
                    have = AnywoodHelper.anywoodAvailable(rawCount, required, key -> {
                        Item logItem = ItemHelper.resolve(key);
                        if (logItem == null) {
                            return 0;
                        }
                        int stock = thInv.getCount((Level)lvl, logItem);
                        if (includeBuilders) {
                            stock += PanelContentGenerator.countBuildersInventory(village, lvl, logItem);
                        }
                        return stock;
                    });
                }
                have = Math.min(have, need);
                String anywoodDescId = "item.millenaire.anywood_log";
                itemName = Component.translatable((String)anywoodDescId).getString();
                sorted.put(itemName, new ResourceLineInfo(have, need, "minecraft:oak_log", anywoodDescId));
                continue;
            }
            need = entry.getValue();
            have = 0;
            if (inv != null && level != null && (item = ItemHelper.resolve(entry.getKey())) != null) {
                have = inv.getCount((Level)level, item);
                if (includeBuilders) {
                    have += PanelContentGenerator.countBuildersInventory(village, level, item);
                }
            }
            have = Math.min(have, need);
            item = ItemHelper.resolve(entry.getKey());
            String descId = null;
            if (item != null) {
                descId = item.getDescriptionId();
                itemName = Component.translatable((String)descId).getString();
            } else {
                itemName = entry.getKey().getPath().replace('_', ' ');
            }
            String itemId = entry.getKey().toString();
            sorted.put(itemName, new ResourceLineInfo(have, need, itemId, descId));
        }
        for (Map.Entry<Object, Integer> entry : sorted.entrySet()) {
            String left;
            int colorInt;
            ResourceLineInfo info = (ResourceLineInfo)((Object)entry.getValue());
            have = info.have;
            int need = info.need;
            String color = have >= need ? "\u00a72" : "\u00a74";
            String right = color + have + "/" + need;
            int n = colorInt = have >= need ? 43520 : 0xAA0000;
            if (info.descId != null && info.itemId != null) {
                lines.add(PanelLine.withIconTranslatable(info.descId, right, info.itemId, colorInt));
                continue;
            }
            if (info.itemId != null) {
                left = "  " + color + (String)entry.getKey();
                lines.add(PanelLine.withIcon(left, right, info.itemId));
                continue;
            }
            left = "  " + color + (String)entry.getKey();
            lines.add(PanelLine.columns(left, right));
        }
    }

    private static int countBuildersInventoryByTag(Village village, ServerLevel level, TagKey<Item> tag) {
        int total = 0;
        for (BuildingInstance b : village.getBuildings()) {
            Entity entity;
            UUID builderUuid;
            ConstructionTask task = b.getConstructionTask();
            if (task == null || task.isComplete() || (builderUuid = task.getReservedBuilder()) == null || !((entity = level.getEntity(builderUuid)) instanceof MillVillager)) continue;
            MillVillager villager = (MillVillager)entity;
            total += villager.getInventory().getCountByTag(tag);
        }
        return total;
    }

    private static int countBuildersInventory(Village village, ServerLevel level, Item item) {
        int total = 0;
        for (UUID uuid : village.getVillagerUuids()) {
            MillVillager mv;
            GoalScheduler scheduler;
            Entity entity = level.getEntity(uuid);
            if (!(entity instanceof MillVillager) || (scheduler = (mv = (MillVillager)entity).getGoalScheduler()) == null || scheduler.getCurrentTask() == null || !"build".equals(scheduler.getCurrentTask().goalId().getPath())) continue;
            total += mv.getInventory().getCount(item);
        }
        return total;
    }

    public record DisplayLine(String text, String leftIcon, String middleIcon, String rightIcon, String leftColumn, String rightColumn, boolean centered, boolean translatable, ItemStack leftIconStack, ItemStack rightIconStack, @Nullable String nativePrefix) {
        public DisplayLine(String text, String leftIcon, String middleIcon, String rightIcon, String leftColumn, String rightColumn, boolean centered) {
            this(text, leftIcon, middleIcon, rightIcon, leftColumn, rightColumn, centered, false, ItemStack.EMPTY, ItemStack.EMPTY, null);
        }

        public DisplayLine(String text, String leftIcon, String middleIcon, String rightIcon, String leftColumn, String rightColumn, boolean centered, boolean translatable) {
            this(text, leftIcon, middleIcon, rightIcon, leftColumn, rightColumn, centered, translatable, ItemStack.EMPTY, ItemStack.EMPTY, null);
        }

        public DisplayLine(String text, String leftIcon, String middleIcon, String rightIcon, String leftColumn, String rightColumn, boolean centered, boolean translatable, ItemStack leftIconStack, ItemStack rightIconStack) {
            this(text, leftIcon, middleIcon, rightIcon, leftColumn, rightColumn, centered, translatable, leftIconStack, rightIconStack, null);
        }

        public static DisplayLine centered(String text) {
            return new DisplayLine(text, "", "", "", "", "", true, false);
        }

        public static DisplayLine centeredTranslatable(String key) {
            return new DisplayLine(key, "", "", "", "", "", true, true);
        }

        public static DisplayLine centeredTranslatableNative(String key, @Nullable String nativePrefix) {
            return new DisplayLine(key, "", "", "", "", "", true, true, ItemStack.EMPTY, ItemStack.EMPTY, nativePrefix);
        }

        public static DisplayLine left(String text) {
            return new DisplayLine(text, "", "", "", "", "", false, false);
        }

        public static DisplayLine centeredWithLeftIcon(String text, String leftIcon) {
            return new DisplayLine(text, leftIcon != null ? leftIcon : "", "", "", "", "", true, false);
        }

        public static DisplayLine leftWithIcon(String text, String leftIcon) {
            return new DisplayLine(text, leftIcon != null ? leftIcon : "", "", "", "", "", false, false);
        }

        public static DisplayLine centeredWithIcons(String text, String leftIcon, String rightIcon) {
            return new DisplayLine(text, leftIcon != null ? leftIcon : "", "", rightIcon != null ? rightIcon : "", "", "", true, false);
        }

        public static DisplayLine centeredWithIconsTranslatable(String key, String leftIcon, String rightIcon) {
            return new DisplayLine(key, leftIcon != null ? leftIcon : "", "", rightIcon != null ? rightIcon : "", "", "", true, true);
        }

        public static DisplayLine centeredWithIconsTranslatableNative(String key, String leftIcon, String rightIcon, @Nullable String nativePrefix) {
            return new DisplayLine(key, leftIcon != null ? leftIcon : "", "", rightIcon != null ? rightIcon : "", "", "", true, true, ItemStack.EMPTY, ItemStack.EMPTY, nativePrefix);
        }

        public static DisplayLine centeredWithIconStacks(String text, ItemStack leftStack, ItemStack rightStack) {
            return new DisplayLine(text, "", "", "", "", "", true, false, leftStack != null ? leftStack : ItemStack.EMPTY, rightStack != null ? rightStack : ItemStack.EMPTY);
        }

        public static DisplayLine columns(String leftCol, String rightCol) {
            return new DisplayLine("", "", "", "", leftCol != null ? leftCol : "", rightCol != null ? rightCol : "", false, false);
        }
    }

    record ResourcePanelData(@Nullable String projectNameKey, boolean isUpgrade, int level, boolean inProgress, Map<ResourceLocation, Integer> resources) {
        static final ResourcePanelData EMPTY = new ResourcePanelData(null, false, 0, false, Map.of());
    }

    public record DisplayData(List<DisplayLine> displayLines) {
        void addCentered(String text) {
            this.displayLines.add(DisplayLine.centered(text));
        }

        void addCenteredTranslatable(String key) {
            this.displayLines.add(DisplayLine.centeredTranslatable(key));
        }

        void addCenteredBuildingName(String translationKey, @Nullable String nativePrefix) {
            if (nativePrefix != null) {
                this.displayLines.add(DisplayLine.centeredTranslatableNative(translationKey, nativePrefix));
            } else {
                this.displayLines.add(DisplayLine.centeredTranslatable(translationKey));
            }
        }

        void addLeft(String text) {
            this.displayLines.add(DisplayLine.left(text));
        }

        void addCenteredWithLeftIcon(String text, String icon) {
            this.displayLines.add(DisplayLine.centeredWithLeftIcon(text, icon));
        }

        void addLeftWithIcon(String text, String icon) {
            this.displayLines.add(DisplayLine.leftWithIcon(text, icon));
        }

        void addCenteredWithIcons(String text, String leftIcon, String rightIcon) {
            this.displayLines.add(DisplayLine.centeredWithIcons(text, leftIcon, rightIcon));
        }

        void addCenteredWithIconsTranslatable(String key, String leftIcon, String rightIcon) {
            this.displayLines.add(DisplayLine.centeredWithIconsTranslatable(key, leftIcon, rightIcon));
        }

        void addCenteredBuildingNameWithIcons(String translationKey, String leftIcon, String rightIcon, @Nullable String nativePrefix) {
            if (nativePrefix != null) {
                this.displayLines.add(DisplayLine.centeredWithIconsTranslatableNative(translationKey, leftIcon, rightIcon, nativePrefix));
            } else {
                this.displayLines.add(DisplayLine.centeredWithIconsTranslatable(translationKey, leftIcon, rightIcon));
            }
        }

        void addCenteredWithIconStacks(String text, ItemStack leftStack, ItemStack rightStack) {
            this.displayLines.add(DisplayLine.centeredWithIconStacks(text, leftStack, rightStack));
        }

        void addColumns(String left, String right) {
            this.displayLines.add(DisplayLine.columns(left, right));
        }
    }

    private record ResourceLineInfo(int have, int need, @Nullable String itemId, @Nullable String descId) {
    }
}

