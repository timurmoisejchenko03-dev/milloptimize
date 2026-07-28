/*
 * Decompiled with CFR 0.150.
 * 
 * Could not load the following classes:
 *  com.mojang.logging.LogUtils
 *  javax.annotation.Nullable
 *  net.minecraft.core.BlockPos
 *  net.minecraft.core.HolderLookup
 *  net.minecraft.core.HolderLookup$Provider
 *  net.minecraft.core.registries.BuiltInRegistries
 *  net.minecraft.nbt.CompoundTag
 *  net.minecraft.nbt.IntArrayTag
 *  net.minecraft.nbt.ListTag
 *  net.minecraft.nbt.StringTag
 *  net.minecraft.nbt.Tag
 *  net.minecraft.resources.ResourceLocation
 *  net.minecraft.server.level.ServerLevel
 *  net.minecraft.world.item.DyeColor
 *  net.minecraft.world.item.Item
 *  net.minecraft.world.level.block.Block
 *  net.minecraft.world.level.block.Rotation
 *  net.minecraft.world.level.saveddata.SavedData
 *  net.minecraft.world.level.saveddata.SavedData$Factory
 *  org.slf4j.Logger
 */
package org.millenaire.village;

import com.mojang.logging.LogUtils;
import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import javax.annotation.Nullable;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.IntArrayTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.saveddata.SavedData;
import org.millenaire.building.BedManager;
import org.millenaire.building.BuildingId;
import org.millenaire.building.BuildingInstance;
import org.millenaire.building.ConstructionTask;
import org.millenaire.quest.MarvelManager;
import org.millenaire.village.Village;
import org.millenaire.village.VillageEvent;
import org.millenaire.village.VillageEventType;
import org.millenaire.village.VillageId;
import org.millenaire.village.VillageManager;
import org.millenaire.village.VillagerRecord;
import org.millenaire.world.PlacedLocation;
import org.slf4j.Logger;

public class VillageSavedData
extends SavedData {
    private static final Logger LOGGER = LogUtils.getLogger();
    private static final int CURRENT_VERSION = 5;
    private static final String DATA_NAME = "millenaire_villages";
    private final VillageManager villageManager;
    private final List<LoneBuildingEntry> loneBuildingPositions = new ArrayList<LoneBuildingEntry>();
    private final Set<String> globalTags = new HashSet<String>();

    private VillageSavedData() {
        this.villageManager = new VillageManager();
    }

    public static SavedData.Factory<VillageSavedData> factory() {
        return new SavedData.Factory(VillageSavedData::new, (tag, registries) -> VillageSavedData.load(tag), null);
    }

    public static VillageSavedData get(ServerLevel level) {
        return (VillageSavedData)level.getDataStorage().computeIfAbsent(VillageSavedData.factory(), DATA_NAME);
    }

    public VillageManager getVillageManager() {
        return this.villageManager;
    }

    public List<LoneBuildingEntry> getLoneBuildingPositions() {
        return this.loneBuildingPositions;
    }

    public void registerLoneBuilding(BlockPos pos, ResourceLocation type, String culture, @Nullable String generatedFor) {
        for (LoneBuildingEntry entry : this.loneBuildingPositions) {
            if (!entry.pos().equals((Object)pos)) continue;
            return;
        }
        this.loneBuildingPositions.add(new LoneBuildingEntry(pos, type, culture, generatedFor));
        this.setDirty();
    }

    public void removeLoneBuilding(BlockPos pos) {
        this.loneBuildingPositions.removeIf(entry -> entry.pos().equals((Object)pos));
        this.setDirty();
    }

    public void setGlobalTag(String tag) {
        if (this.globalTags.add(tag)) {
            this.setDirty();
        }
    }

    public void clearGlobalTag(String tag) {
        if (this.globalTags.remove(tag)) {
            this.setDirty();
        }
    }

    public boolean isGlobalTagSet(String tag) {
        return this.globalTags.contains(tag);
    }

    public Set<String> getGlobalTags() {
        return Collections.unmodifiableSet(this.globalTags);
    }

    public CompoundTag save(CompoundTag root, HolderLookup.Provider registries) {
        ListTag villagesList = new ListTag();
        for (Village village : this.villageManager.getAllVillages()) {
            ListTag list;
            CompoundTag villageTag = new CompoundTag();
            villageTag.putUUID("id", village.getId().uuid());
            villageTag.putString("culture", village.getCultureId().toString());
            villageTag.putString("village_type", village.getVillageTypeId().toString());
            villageTag.put("center", (Tag)VillageSavedData.encodeBlockPos(village.getCenter()));
            if (village.getVillageName() != null) {
                villageTag.putString("name", village.getVillageName());
            }
            ListTag buildingsList = new ListTag();
            for (BuildingInstance buildingInstance : village.getBuildings()) {
                CompoundTag buildingTag = new CompoundTag();
                buildingTag.putUUID("id", buildingInstance.getId().uuid());
                buildingTag.putString("plan", buildingInstance.getPlanId().toString());
                buildingTag.put("origin", (Tag)VillageSavedData.encodeBlockPos(buildingInstance.getOrigin()));
                buildingTag.putString("rotation", buildingInstance.getRotation().name());
                buildingTag.putString("status", buildingInstance.getStatus().name());
                if (buildingInstance.getPlanSetId() != null) {
                    buildingTag.putString("plan_set", buildingInstance.getPlanSetId().toString());
                }
                if (buildingInstance.getVariant() != null) {
                    buildingTag.putString("variant", buildingInstance.getVariant());
                }
                buildingTag.putInt("level", buildingInstance.getLevel());
                if (buildingInstance.isSubBuilding()) {
                    buildingTag.putBoolean("sub_building", true);
                }
                if (!buildingInstance.isUpgradesAllowed()) {
                    buildingTag.putBoolean("upgrades_allowed", false);
                }
                if (buildingInstance.getBrickColourMapping() != null) {
                    CompoundTag brickTag = new CompoundTag();
                    for (Map.Entry<DyeColor, DyeColor> entry : buildingInstance.getBrickColourMapping().entrySet()) {
                        brickTag.putString(entry.getKey().getSerializedName(), entry.getValue().getSerializedName());
                    }
                    buildingTag.put("brick_colours", (Tag)brickTag);
                }
                if (buildingInstance.hasBedManager()) {
                    buildingTag.put("beds", (Tag)buildingInstance.getBedManager().save());
                }
                if (buildingInstance.getConstructionTask() != null) {
                    buildingTag.put("construction_task", (Tag)buildingInstance.getConstructionTask().save());
                }
                if (buildingInstance.getNbNightsMerchant() > 0) {
                    buildingTag.putInt("nb_nights_merchant", buildingInstance.getNbNightsMerchant());
                }
                if (!buildingInstance.getImported().isEmpty()) {
                    CompoundTag importTag = new CompoundTag();
                    for (Map.Entry<Object, Object> entry : buildingInstance.getImported().entrySet()) {
                        String string = BuiltInRegistries.ITEM.getKey((Object)((Item)entry.getKey())).toString();
                        importTag.putInt(string, ((Integer)entry.getValue()).intValue());
                    }
                    buildingTag.put("imported", (Tag)importTag);
                }
                if (!buildingInstance.getExported().isEmpty()) {
                    CompoundTag exportTag = new CompoundTag();
                    for (Map.Entry entry : buildingInstance.getExported().entrySet()) {
                        String string = BuiltInRegistries.ITEM.getKey((Object)((Item)entry.getKey())).toString();
                        exportTag.putInt(string, ((Integer)entry.getValue()).intValue());
                    }
                    buildingTag.put("exported", (Tag)exportTag);
                }
                if (!buildingInstance.getVisitorLog().isEmpty()) {
                    ListTag logList = new ListTag();
                    for (String string : buildingInstance.getVisitorLog()) {
                        CompoundTag compoundTag = new CompoundTag();
                        compoundTag.putString("msg", string);
                        logList.add((Object)compoundTag);
                    }
                    buildingTag.put("visitor_log", (Tag)logList);
                }
                if (buildingInstance.getLastMarketNightDay() >= 0L) {
                    buildingTag.putLong("last_market_night_day", buildingInstance.getLastMarketNightDay());
                }
                if (!buildingInstance.getRuntimeTags().isEmpty()) {
                    ListTag runtimeTagsList = new ListTag();
                    for (String string : buildingInstance.getRuntimeTags()) {
                        runtimeTagsList.add((Object)StringTag.valueOf((String)string));
                    }
                    buildingTag.put("runtime_tags", (Tag)runtimeTagsList);
                }
                if (buildingInstance.getParentBuildingId() != null) {
                    buildingTag.putUUID("parent_building_id", buildingInstance.getParentBuildingId().uuid());
                }
                buildingsList.add((Object)buildingTag);
            }
            villageTag.put("buildings", (Tag)buildingsList);
            ListTag villagersList = new ListTag();
            for (Map.Entry<UUID, VillagerRecord> entry : village.getVillagerRecords().entrySet()) {
                CompoundTag villagerTag = new CompoundTag();
                entry.getValue().save(villagerTag);
                villagersList.add((Object)villagerTag);
            }
            villageTag.put("villagers", (Tag)villagersList);
            CompoundTag compoundTag = new CompoundTag();
            village.getReputation().save(compoundTag);
            villageTag.put("reputation", (Tag)compoundTag);
            villageTag.putLong("lastGoodsRefresh", village.getLastGoodsRefresh());
            villageTag.putBoolean("chestLocked", village.areChestsLocked());
            Village.PendingProject pending = village.getPendingProject();
            if (pending != null) {
                CompoundTag pendingTag = new CompoundTag();
                pendingTag.putString("plan_set", pending.planSetId().toString());
                pendingTag.putString("variant", pending.variant());
                pendingTag.putInt("level", pending.level());
                pendingTag.putBoolean("is_upgrade", pending.isUpgrade());
                if (pending.buildingId() != null) {
                    pendingTag.putUUID("building_id", pending.buildingId().uuid());
                }
                if (pending.plannedLocation() != null) {
                    PlacedLocation planned = pending.plannedLocation();
                    pendingTag.putInt("planned_x", planned.position().getX());
                    pendingTag.putInt("planned_y", planned.position().getY());
                    pendingTag.putInt("planned_z", planned.position().getZ());
                    pendingTag.putInt("planned_rotation", planned.rotation().ordinal());
                }
                villageTag.put("pending_project", (Tag)pendingTag);
            }
            if (village.getBrickThemeName() != null) {
                villageTag.putString("brick_theme", village.getBrickThemeName());
            }
            if (!village.getBuildingsBought().isEmpty()) {
                ListTag boughtList = new ListTag();
                for (ResourceLocation resourceLocation : village.getBuildingsBought()) {
                    CompoundTag compoundTag2 = new CompoundTag();
                    compoundTag2.putString("id", resourceLocation.toString());
                    boughtList.add((Object)compoundTag2);
                }
                villageTag.put("bought_buildings", (Tag)boughtList);
            }
            CompoundTag pathsTag = new CompoundTag();
            village.getPathManager().save(pathsTag);
            villageTag.put("paths", (Tag)pathsTag);
            villageTag.putLong("last_night_action_day", village.getLastNightActionDay());
            if (village.getRaidTarget() != null) {
                villageTag.putUUID("raid_target", village.getRaidTarget().uuid());
            }
            if (village.getRaidPlanningStart() != 0L) {
                villageTag.putLong("raid_planning_start", village.getRaidPlanningStart());
            }
            if (village.getRaidStart() != 0L) {
                villageTag.putLong("raid_start", village.getRaidStart());
            }
            if (village.isUnderAttack()) {
                villageTag.putBoolean("under_attack", true);
            }
            if (!village.getRaidsPerformed().isEmpty()) {
                list = new ListTag();
                for (String string : village.getRaidsPerformed()) {
                    list.add((Object)StringTag.valueOf((String)string));
                }
                villageTag.put("raids_performed", (Tag)list);
            }
            if (!village.getRaidsSuffered().isEmpty()) {
                list = new ListTag();
                for (String string : village.getRaidsSuffered()) {
                    list.add((Object)StringTag.valueOf((String)string));
                }
                villageTag.put("raids_suffered", (Tag)list);
            }
            if (village.getParentVillageId() != null) {
                villageTag.putUUID("parent_village_id", village.getParentVillageId().uuid());
            }
            if (village.getOwnerUUID() != null) {
                villageTag.putUUID("owner_uuid", village.getOwnerUUID());
                if (village.getOwnerName() != null) {
                    villageTag.putString("owner_name", village.getOwnerName());
                }
            }
            if (village.getBannerNbt() != null) {
                villageTag.putString("banner_nbt", village.getBannerNbt());
            }
            if (!village.getRelations().isEmpty()) {
                ListTag relationsList = new ListTag();
                for (Map.Entry<VillageId, Integer> entry : village.getRelations().entrySet()) {
                    CompoundTag relTag = new CompoundTag();
                    relTag.putUUID("village_id", entry.getKey().uuid());
                    relTag.putInt("value", entry.getValue().intValue());
                    relationsList.add((Object)relTag);
                }
                villageTag.put("relations", (Tag)relationsList);
            }
            if (village.getMarvelManager() != null) {
                villageTag.put("marvel", (Tag)village.getMarvelManager().save());
            }
            if (!village.getChronicle().isEmpty()) {
                ListTag chronicleList = new ListTag();
                for (VillageEvent villageEvent : village.getChronicle()) {
                    CompoundTag eventTag = new CompoundTag();
                    eventTag.putLong("time", villageEvent.gameTime());
                    eventTag.putString("type", villageEvent.type().name());
                    eventTag.putString("p1", villageEvent.param1());
                    if (villageEvent.param2() != null) {
                        eventTag.putString("p2", villageEvent.param2());
                    }
                    chronicleList.add((Object)eventTag);
                }
                villageTag.put("chronicle", (Tag)chronicleList);
            }
            villagesList.add((Object)villageTag);
        }
        root.putInt("version", 5);
        root.put("villages", (Tag)villagesList);
        ListTag lbList = new ListTag();
        for (LoneBuildingEntry entry : this.loneBuildingPositions) {
            CompoundTag lbTag = new CompoundTag();
            lbTag.put("pos", (Tag)VillageSavedData.encodeBlockPos(entry.pos()));
            lbTag.putString("type", entry.type().toString());
            lbTag.putString("culture", entry.culture());
            if (entry.generatedFor() != null) {
                lbTag.putString("generated_for", entry.generatedFor());
            }
            lbList.add((Object)lbTag);
        }
        root.put("lone_buildings", (Tag)lbList);
        if (!this.globalTags.isEmpty()) {
            ListTag listTag = new ListTag();
            for (String tag : this.globalTags) {
                listTag.add((Object)StringTag.valueOf((String)tag));
            }
            root.put("globalTags", (Tag)listTag);
        }
        return root;
    }

    static VillageSavedData load(CompoundTag root) {
        VillageSavedData data = new VillageSavedData();
        int version = root.contains("version") ? root.getInt("version") : 0;
        LOGGER.info("Loading VillageSavedData version {}", (Object)version);
        ListTag villagesList = root.getList("villages", 10);
        for (int i = 0; i < villagesList.size(); ++i) {
            try {
                ArrayList<String> entries;
                CompoundTag villageTag = villagesList.getCompound(i);
                VillageId villageId = new VillageId(villageTag.getUUID("id"));
                ResourceLocation cultureId = ResourceLocation.parse((String)villageTag.getString("culture"));
                ResourceLocation villageTypeId = VillageSavedData.migrateVillageTypeId(ResourceLocation.parse((String)villageTag.getString("village_type")));
                BlockPos center = VillageSavedData.decodeBlockPos(villageTag.getIntArray("center"));
                Village village = new Village(villageId, cultureId, villageTypeId, center);
                if (villageTag.contains("name")) {
                    village.setVillageName(villageTag.getString("name"));
                }
                ListTag buildingsList = villageTag.getList("buildings", 10);
                for (int j = 0; j < buildingsList.size(); ++j) {
                    try {
                        Item item;
                        CompoundTag buildingTag = buildingsList.getCompound(j);
                        BuildingId buildingId = new BuildingId(buildingTag.getUUID("id"));
                        ResourceLocation planId = ResourceLocation.parse((String)buildingTag.getString("plan"));
                        BlockPos origin = VillageSavedData.decodeBlockPos(buildingTag.getIntArray("origin"));
                        Rotation rotation = Rotation.valueOf((String)buildingTag.getString("rotation"));
                        BuildingInstance.Status status = BuildingInstance.Status.valueOf(buildingTag.getString("status"));
                        ResourceLocation planSetId = buildingTag.contains("plan_set") ? ResourceLocation.parse((String)buildingTag.getString("plan_set")) : null;
                        String variant = buildingTag.contains("variant") ? buildingTag.getString("variant") : null;
                        int level = buildingTag.getInt("level");
                        BuildingInstance building = new BuildingInstance(buildingId, planId, origin, rotation, status, planSetId, variant, level);
                        if (buildingTag.getBoolean("sub_building")) {
                            building.setSubBuilding(true);
                        }
                        if (buildingTag.contains("upgrades_allowed")) {
                            building.setUpgradesAllowed(buildingTag.getBoolean("upgrades_allowed"));
                        }
                        if (buildingTag.contains("brick_colours")) {
                            CompoundTag brickTag = buildingTag.getCompound("brick_colours");
                            EnumMap<DyeColor, DyeColor> mapping = new EnumMap<DyeColor, DyeColor>(DyeColor.class);
                            for (String key : brickTag.getAllKeys()) {
                                DyeColor from = VillageSavedData.dyeColorFromName(key);
                                DyeColor to = VillageSavedData.dyeColorFromName(brickTag.getString(key));
                                if (from == null || to == null) continue;
                                mapping.put(from, to);
                            }
                            building.setBrickColourMapping(mapping);
                        }
                        if (buildingTag.contains("beds", 9)) {
                            BedManager loaded = BedManager.load(buildingTag.getList("beds", 10));
                            building.setBedManager(loaded);
                            building.setBedManagerInitialized(true);
                        }
                        if (buildingTag.contains("construction_task")) {
                            building.setConstructionTask(ConstructionTask.load(buildingTag.getCompound("construction_task"), (HolderLookup<Block>)BuiltInRegistries.BLOCK.asLookup()));
                        }
                        if (buildingTag.contains("nb_nights_merchant")) {
                            building.setNbNightsMerchant(buildingTag.getInt("nb_nights_merchant"));
                        }
                        if (buildingTag.contains("imported")) {
                            CompoundTag importTag = buildingTag.getCompound("imported");
                            LinkedHashMap<Item, Integer> importMap = new LinkedHashMap<Item, Integer>();
                            for (String itemId : importTag.getAllKeys()) {
                                item = BuiltInRegistries.ITEM.getOptional(ResourceLocation.parse((String)itemId)).orElse(null);
                                if (item == null) continue;
                                importMap.put(item, importTag.getInt(itemId));
                            }
                            building.setImported(importMap);
                        }
                        if (buildingTag.contains("exported")) {
                            CompoundTag exportTag = buildingTag.getCompound("exported");
                            LinkedHashMap<Item, Integer> exportMap = new LinkedHashMap<Item, Integer>();
                            for (String itemId : exportTag.getAllKeys()) {
                                item = BuiltInRegistries.ITEM.getOptional(ResourceLocation.parse((String)itemId)).orElse(null);
                                if (item == null) continue;
                                exportMap.put(item, exportTag.getInt(itemId));
                            }
                            building.setExported(exportMap);
                        }
                        if (buildingTag.contains("visitor_log", 9)) {
                            ListTag logList = buildingTag.getList("visitor_log", 10);
                            ArrayList<String> entries2 = new ArrayList<String>();
                            for (int k = 0; k < logList.size(); ++k) {
                                entries2.add(logList.getCompound(k).getString("msg"));
                            }
                            building.setVisitorLog(entries2);
                        }
                        if (buildingTag.contains("last_market_night_day")) {
                            building.setLastMarketNightDay(buildingTag.getLong("last_market_night_day"));
                        }
                        if (buildingTag.contains("runtime_tags", 9)) {
                            ListTag runtimeTagsList = buildingTag.getList("runtime_tags", 8);
                            ArrayList<String> tags = new ArrayList<String>();
                            for (int k = 0; k < runtimeTagsList.size(); ++k) {
                                tags.add(runtimeTagsList.getString(k));
                            }
                            building.addRuntimeTags(tags);
                        }
                        if (buildingTag.contains("parent_building_id")) {
                            building.setParentBuildingId(new BuildingId(buildingTag.getUUID("parent_building_id")));
                        }
                        village.addBuilding(building);
                        continue;
                    }
                    catch (Exception e) {
                        LOGGER.error("Error loading building index {} of village {} \u2014 building ignored", new Object[]{j, i, e});
                    }
                }
                ListTag villagersList = villageTag.getList("villagers", 10);
                for (int j = 0; j < villagersList.size(); ++j) {
                    CompoundTag villagerTag = villagersList.getCompound(j);
                    if (version >= 2) {
                        VillagerRecord record = VillagerRecord.load(villagerTag);
                        if (record == null) continue;
                        village.addVillager(record);
                        continue;
                    }
                    UUID uuid = villagerTag.getUUID("uuid");
                    ResourceLocation typeId = villagerTag.contains("type") ? ResourceLocation.parse((String)villagerTag.getString("type")) : ResourceLocation.withDefaultNamespace((String)"unknown");
                    BuildingId home = villagerTag.hasUUID("home") ? new BuildingId(villagerTag.getUUID("home")) : null;
                    village.addVillager(uuid, typeId, home);
                }
                if (villageTag.contains("reputation")) {
                    village.getReputation().load(villageTag.getCompound("reputation"));
                }
                if (villageTag.contains("lastGoodsRefresh")) {
                    village.setLastGoodsRefresh(villageTag.getLong("lastGoodsRefresh"));
                }
                if (villageTag.contains("chestLocked")) {
                    village.setChestLocked(villageTag.getBoolean("chestLocked"));
                }
                if (villageTag.contains("pending_project")) {
                    try {
                        CompoundTag pendingTag = villageTag.getCompound("pending_project");
                        ResourceLocation pendingPlanSetId = ResourceLocation.parse((String)pendingTag.getString("plan_set"));
                        String pendingVariant = pendingTag.getString("variant");
                        int pendingLevel = pendingTag.getInt("level");
                        boolean pendingIsUpgrade = pendingTag.getBoolean("is_upgrade");
                        BuildingId pendingBuildingId = pendingTag.hasUUID("building_id") ? new BuildingId(pendingTag.getUUID("building_id")) : null;
                        PlacedLocation pendingPlanned = null;
                        if (pendingTag.contains("planned_x")) {
                            BlockPos plannedPos = new BlockPos(pendingTag.getInt("planned_x"), pendingTag.getInt("planned_y"), pendingTag.getInt("planned_z"));
                            int rotOrd = Math.floorMod(pendingTag.getInt("planned_rotation"), Rotation.values().length);
                            pendingPlanned = new PlacedLocation(plannedPos, Rotation.values()[rotOrd]);
                        }
                        village.setPendingProject(new Village.PendingProject(pendingPlanSetId, pendingVariant, pendingLevel, pendingIsUpgrade, pendingBuildingId, pendingPlanned));
                    }
                    catch (Exception e) {
                        LOGGER.error("Error loading pending_project of village {} \u2014 project ignored", (Object)i, (Object)e);
                    }
                }
                if (villageTag.contains("brick_theme")) {
                    village.setRawBrickThemeName(villageTag.getString("brick_theme"));
                }
                if (villageTag.contains("bought_buildings")) {
                    ListTag boughtList = villageTag.getList("bought_buildings", 10);
                    HashSet<ResourceLocation> bought = new HashSet<ResourceLocation>();
                    for (int j = 0; j < boughtList.size(); ++j) {
                        CompoundTag entry = boughtList.getCompound(j);
                        bought.add(ResourceLocation.parse((String)entry.getString("id")));
                    }
                    village.loadBuildingsBought(bought);
                }
                if (villageTag.contains("paths")) {
                    village.getPathManager().load(villageTag.getCompound("paths"));
                }
                if (villageTag.contains("last_night_action_day")) {
                    village.setLastNightActionDay(villageTag.getLong("last_night_action_day"));
                }
                if (villageTag.hasUUID("raid_target")) {
                    village.setRaidTarget(new VillageId(villageTag.getUUID("raid_target")));
                }
                if (villageTag.contains("raid_planning_start")) {
                    village.setRaidPlanningStart(villageTag.getLong("raid_planning_start"));
                }
                if (villageTag.contains("raid_start")) {
                    village.setRaidStart(villageTag.getLong("raid_start"));
                }
                if (villageTag.contains("under_attack")) {
                    village.setUnderAttack(villageTag.getBoolean("under_attack"));
                }
                if (villageTag.contains("raids_performed")) {
                    ListTag list = villageTag.getList("raids_performed", 8);
                    entries = new ArrayList<String>();
                    for (int r = 0; r < list.size(); ++r) {
                        entries.add(list.getString(r));
                    }
                    village.loadRaidsPerformed(entries);
                }
                if (villageTag.contains("raids_suffered")) {
                    ListTag list = villageTag.getList("raids_suffered", 8);
                    entries = new ArrayList();
                    for (int r = 0; r < list.size(); ++r) {
                        entries.add(list.getString(r));
                    }
                    village.loadRaidsSuffered(entries);
                }
                if (villageTag.contains("parent_village_id")) {
                    village.setParentVillageId(new VillageId(villageTag.getUUID("parent_village_id")));
                }
                if (villageTag.contains("owner_uuid")) {
                    UUID ownerUUID = villageTag.getUUID("owner_uuid");
                    String ownerName = villageTag.contains("owner_name") ? villageTag.getString("owner_name") : null;
                    village.setOwner(ownerUUID, ownerName);
                }
                if (villageTag.contains("banner_nbt")) {
                    village.loadBannerNbt(villageTag.getString("banner_nbt"));
                }
                if (villageTag.contains("relations")) {
                    ListTag relationsList = villageTag.getList("relations", 10);
                    HashMap<VillageId, Integer> loaded = new HashMap<VillageId, Integer>();
                    for (int r = 0; r < relationsList.size(); ++r) {
                        CompoundTag relTag = relationsList.getCompound(r);
                        VillageId otherId = new VillageId(relTag.getUUID("village_id"));
                        int value = relTag.getInt("value");
                        loaded.put(otherId, value);
                    }
                    village.setRelationsFromNbt(loaded);
                }
                if (villageTag.contains("chronicle")) {
                    ListTag chronicleList = villageTag.getList("chronicle", 10);
                    for (int c = 0; c < chronicleList.size(); ++c) {
                        VillageEventType type;
                        CompoundTag eventTag = chronicleList.getCompound(c);
                        long gameTime = eventTag.getLong("time");
                        try {
                            type = VillageEventType.valueOf(eventTag.getString("type"));
                        }
                        catch (IllegalArgumentException e) {
                            LOGGER.warn("Unknown chronicle event type: {}", (Object)eventTag.getString("type"));
                            continue;
                        }
                        String p1 = eventTag.getString("p1");
                        String p2 = eventTag.contains("p2") ? eventTag.getString("p2") : null;
                        village.addChronicleEventDirect(new VillageEvent(gameTime, type, p1, p2));
                    }
                }
                if (villageTag.contains("marvel")) {
                    MarvelManager mm = new MarvelManager();
                    mm.load(villageTag.getCompound("marvel"));
                    village.setMarvelManager(mm);
                }
                data.villageManager.addVillage(village);
                continue;
            }
            catch (Exception e) {
                LOGGER.error("Error loading village index {} \u2014 village ignored", (Object)i, (Object)e);
            }
        }
        if (root.contains("lone_buildings")) {
            ListTag lbList = root.getList("lone_buildings", 10);
            for (int i = 0; i < lbList.size(); ++i) {
                try {
                    CompoundTag lbTag = lbList.getCompound(i);
                    BlockPos pos = VillageSavedData.decodeBlockPos(lbTag.getIntArray("pos"));
                    ResourceLocation type = ResourceLocation.parse((String)lbTag.getString("type"));
                    String culture = lbTag.getString("culture");
                    String generatedFor = lbTag.contains("generated_for") ? lbTag.getString("generated_for") : null;
                    data.loneBuildingPositions.add(new LoneBuildingEntry(pos, type, culture, generatedFor));
                    continue;
                }
                catch (Exception e) {
                    LOGGER.error("Error loading lone building index {} \u2014 ignored", (Object)i, (Object)e);
                }
            }
        }
        if (root.contains("globalTags")) {
            ListTag globalTagsList = root.getList("globalTags", 8);
            for (int i = 0; i < globalTagsList.size(); ++i) {
                data.globalTags.add(globalTagsList.getString(i));
            }
        }
        return data;
    }

    private static IntArrayTag encodeBlockPos(BlockPos pos) {
        return new IntArrayTag(new int[]{pos.getX(), pos.getY(), pos.getZ()});
    }

    private static BlockPos decodeBlockPos(int[] arr) {
        if (arr.length != 3) {
            throw new IllegalArgumentException("Expected BlockPos as int[3], received int[" + arr.length + "]");
        }
        return new BlockPos(arr[0], arr[1], arr[2]);
    }

    private static ResourceLocation migrateVillageTypeId(ResourceLocation id) {
        int idx;
        String path = id.getPath();
        if (!path.contains("/") && (idx = path.indexOf(95)) > 0) {
            String migrated = path.substring(0, idx) + "/" + path.substring(idx + 1);
            LOGGER.info("Migrating village type ID: {} -> {}", (Object)id, (Object)migrated);
            return ResourceLocation.fromNamespaceAndPath((String)id.getNamespace(), (String)migrated);
        }
        return id;
    }

    private static DyeColor dyeColorFromName(String name) {
        for (DyeColor c : DyeColor.values()) {
            if (!c.getSerializedName().equals(name)) continue;
            return c;
        }
        return null;
    }

    public record LoneBuildingEntry(BlockPos pos, ResourceLocation type, String culture, @Nullable String generatedFor) {
    }
}

