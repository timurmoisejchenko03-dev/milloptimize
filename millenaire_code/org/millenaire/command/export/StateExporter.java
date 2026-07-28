/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.google.gson.Gson
 *  com.google.gson.GsonBuilder
 *  net.minecraft.core.BlockPos
 *  net.minecraft.core.registries.BuiltInRegistries
 *  net.minecraft.resources.ResourceLocation
 *  net.minecraft.server.level.ServerLevel
 *  net.minecraft.world.entity.Entity
 *  net.minecraft.world.item.Item
 *  net.minecraft.world.level.Level
 */
package org.millenaire.command.export;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.OpenOption;
import java.nio.file.Path;
import java.time.Instant;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.Level;
import org.millenaire.FormatUtils;
import org.millenaire.building.BuildingInstance;
import org.millenaire.building.BuildingInventory;
import org.millenaire.building.BuildingPlan;
import org.millenaire.building.ConstructionTask;
import org.millenaire.building.SpecialPoint;
import org.millenaire.culture.ModCultures;
import org.millenaire.culture.ReputationLabel;
import org.millenaire.entity.MillVillager;
import org.millenaire.goal.GoalScheduler;
import org.millenaire.goal.VillagerGoal;
import org.millenaire.goal.VillagerTask;
import org.millenaire.village.PlayerCultureReputation;
import org.millenaire.village.Village;
import org.millenaire.village.VillageReputation;

public final class StateExporter {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

    private StateExporter() {
    }

    public static Path export(ServerLevel level, Village village, Path dir) throws IOException {
        Path file = dir.resolve("village-state.json");
        LinkedHashMap<String, Object> root = new LinkedHashMap<String, Object>();
        root.put("exportedAt", DateTimeFormatter.ISO_INSTANT.format(Instant.now().atOffset(ZoneOffset.UTC)));
        root.put("tick", level.getServer().getTickCount());
        root.put("dayTime", level.getDayTime());
        root.put("village", StateExporter.buildVillageMap(level, village));
        Files.writeString(file, (CharSequence)GSON.toJson(root), new OpenOption[0]);
        return file;
    }

    private static Map<String, Object> buildVillageMap(ServerLevel level, Village village) {
        LinkedHashMap<String, Object> map = new LinkedHashMap<String, Object>();
        map.put("id", StateExporter.shortUuid(village.getId().uuid()));
        map.put("type", village.getVillageTypeId().toString());
        map.put("culture", village.getCultureId().toString());
        map.put("center", StateExporter.blockPosArray(village.getCenter()));
        ArrayList<Map<String, Object>> buildingList = new ArrayList<Map<String, Object>>();
        for (BuildingInstance b : village.getBuildings()) {
            buildingList.add(StateExporter.buildBuildingMap(level, b));
        }
        map.put("buildings", buildingList);
        ArrayList<Map<String, Object>> villagerList = new ArrayList<Map<String, Object>>();
        for (Map.Entry<UUID, ResourceLocation> entry : village.getVillagerTypes().entrySet()) {
            villagerList.add(StateExporter.buildVillagerMap(level, entry.getKey(), entry.getValue()));
        }
        map.put("villagers", villagerList);
        map.put("reputation", StateExporter.buildReputationMap(level, village));
        map.put("waypointCount", village.getWaypointGraph().waypointCount());
        map.put("growth", StateExporter.buildGrowthMap(village));
        return map;
    }

    private static Map<String, Object> buildBuildingMap(ServerLevel level, BuildingInstance b) {
        List<SpecialPoint> points;
        Map<Item, Integer> contents;
        BuildingInventory inv;
        ConstructionTask task;
        LinkedHashMap<String, Object> map = new LinkedHashMap<String, Object>();
        map.put("id", StateExporter.shortUuid(b.getId().uuid()));
        map.put("plan", b.getPlanId().toString());
        map.put("planSet", b.getPlanSetId() != null ? b.getPlanSetId().toString() : null);
        map.put("variant", b.getVariant());
        map.put("level", b.getLevel());
        map.put("status", b.getStatus().name());
        map.put("origin", StateExporter.blockPosArray(b.getOrigin()));
        map.put("rotation", b.getRotation().name());
        BuildingPlan plan = ModCultures.getBuildingPlan(b.getPlanId());
        if (plan != null) {
            map.put("tags", plan.tags());
            map.put("size", List.of(Integer.valueOf(plan.width()), Integer.valueOf(plan.height()), Integer.valueOf(plan.depth())));
        } else {
            map.put("tags", List.of());
            map.put("size", null);
        }
        if (!b.getRuntimeTags().isEmpty()) {
            map.put("runtimeTags", new ArrayList<String>(b.getRuntimeTags()));
        }
        if (b.getParentBuildingId() != null) {
            map.put("parentBuildingId", StateExporter.shortUuid(b.getParentBuildingId().uuid()));
        }
        if ((task = b.getConstructionTask()) != null) {
            LinkedHashMap<String, Object> cMap = new LinkedHashMap<String, Object>();
            cMap.put("progress", Math.round(task.progress() * 100.0f));
            cMap.put("step", task.getNextStepIndex());
            cMap.put("totalSteps", task.totalSteps());
            cMap.put("reserved", task.isReserved());
            cMap.put("builder", task.getReservedBuilder() != null ? StateExporter.shortUuid(task.getReservedBuilder()) : null);
            cMap.put("blocked", task.isBlocked());
            cMap.put("failedAttempts", task.getFailedAttempts());
            map.put("construction", cMap);
        }
        if (b.getStatus() == BuildingInstance.Status.COMPLETE && (inv = b.getInventory()) != null && !(contents = inv.scanChests((Level)level)).isEmpty()) {
            LinkedHashMap<String, Integer> invMap = new LinkedHashMap<String, Integer>();
            for (Map.Entry<Item, Integer> entry : contents.entrySet()) {
                ResourceLocation resourceLocation = BuiltInRegistries.ITEM.getKey((Object)entry.getKey());
                invMap.put(resourceLocation.toString(), entry.getValue());
            }
            map.put("inventory", invMap);
        }
        if (!(points = b.getResolvedPoints()).isEmpty()) {
            LinkedHashMap<String, Map<String, Integer>> spMap = new LinkedHashMap<String, Map<String, Integer>>();
            LinkedHashMap<String, List> grouped = new LinkedHashMap<String, List>();
            LinkedHashMap<String, Integer> counts = new LinkedHashMap<String, Integer>();
            for (SpecialPoint specialPoint : points) {
                counts.merge(specialPoint.type(), 1, Integer::sum);
                grouped.computeIfAbsent(specialPoint.type(), k -> new ArrayList()).add(StateExporter.blockPosArray(specialPoint.pos()));
            }
            for (Map.Entry entry : grouped.entrySet()) {
                if (((List)entry.getValue()).size() > 20) {
                    spMap.put((String)entry.getKey(), Map.of("count", ((List)entry.getValue()).size()));
                    continue;
                }
                spMap.put((String)entry.getKey(), (Map<String, Integer>)entry.getValue());
            }
            map.put("specialPoints", spMap);
        }
        return map;
    }

    private static Map<String, Object> buildVillagerMap(ServerLevel level, UUID uuid, ResourceLocation typeId) {
        LinkedHashMap<String, Object> map = new LinkedHashMap<String, Object>();
        map.put("uuid", StateExporter.shortUuid(uuid));
        map.put("type", typeId.toString());
        Entity entity = level.getEntity(uuid);
        boolean loaded = entity instanceof MillVillager;
        map.put("loaded", loaded);
        if (loaded) {
            MillVillager v = (MillVillager)entity;
            map.put("pos", List.of(Double.valueOf(StateExporter.round1(v.getX())), Double.valueOf(StateExporter.round1(v.getY())), Double.valueOf(StateExporter.round1(v.getZ()))));
            map.put("health", StateExporter.round1(v.getHealth()));
            map.put("displayName", v.getVillagerDisplayName());
            GoalScheduler scheduler = v.getGoalScheduler();
            if (scheduler != null) {
                VillagerGoal goal = scheduler.getCurrentGoal();
                VillagerTask task = scheduler.getCurrentTask();
                map.put("goal", goal != null ? goal.id().getPath() : null);
                map.put("taskGoalId", task != null ? task.goalId().getPath() : null);
                map.put("taskFinished", task != null ? Boolean.valueOf(task.isFinished()) : null);
            } else {
                map.put("goal", null);
                map.put("taskGoalId", null);
                map.put("taskFinished", null);
            }
            map.put("goalLabel", v.getGoalLabel());
        }
        return map;
    }

    private static Map<String, Object> buildReputationMap(ServerLevel level, Village village) {
        LinkedHashMap<String, Object> repRoot = new LinkedHashMap<String, Object>();
        LinkedHashMap playersMap = new LinkedHashMap();
        VillageReputation villageRep = village.getReputation();
        Map<UUID, Integer> allReps = villageRep.getAll();
        ResourceLocation cultureId = village.getCultureId();
        PlayerCultureReputation cultureRep = PlayerCultureReputation.get(level);
        List<ReputationLabel> labels = ModCultures.getReputationLabels(cultureId);
        for (Map.Entry<UUID, Integer> entry : allReps.entrySet()) {
            UUID playerId = entry.getKey();
            int villageValue = entry.getValue();
            int cultureValue = cultureRep.get(playerId, cultureId);
            int effective = villageValue + cultureValue;
            LinkedHashMap<String, Object> pMap = new LinkedHashMap<String, Object>();
            pMap.put("village", villageValue);
            pMap.put("culture", cultureValue);
            pMap.put("effective", effective);
            pMap.put("label", VillageReputation.getLabel(effective, labels));
            playersMap.put(StateExporter.shortUuid(playerId), pMap);
        }
        repRoot.put("players", playersMap);
        return repRoot;
    }

    private static Map<String, Object> buildGrowthMap(Village village) {
        LinkedHashMap<String, Object> map = new LinkedHashMap<String, Object>();
        int underConstruction = 0;
        int planned = 0;
        for (BuildingInstance b : village.getBuildings()) {
            if (b.isBeingBuilt()) {
                ++underConstruction;
            }
            if (b.getStatus() != BuildingInstance.Status.PLANNED) continue;
            ++planned;
        }
        map.put("underConstruction", underConstruction);
        map.put("planned", planned);
        return map;
    }

    private static String shortUuid(UUID uuid) {
        return FormatUtils.shortUuid(uuid);
    }

    private static List<Integer> blockPosArray(BlockPos pos) {
        return List.of(Integer.valueOf(pos.getX()), Integer.valueOf(pos.getY()), Integer.valueOf(pos.getZ()));
    }

    private static double round1(double value) {
        return (double)Math.round(value * 10.0) / 10.0;
    }

    private static double round1(float value) {
        return (double)Math.round((double)value * 10.0) / 10.0;
    }
}

