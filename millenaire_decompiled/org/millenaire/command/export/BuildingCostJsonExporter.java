/*
 * Decompiled with CFR 0.150.
 * 
 * Could not load the following classes:
 *  com.google.gson.Gson
 *  com.google.gson.GsonBuilder
 *  net.minecraft.resources.ResourceLocation
 */
package org.millenaire.command.export;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import java.io.IOException;
import java.io.Serializable;
import java.nio.file.Files;
import java.nio.file.OpenOption;
import java.nio.file.Path;
import java.nio.file.attribute.FileAttribute;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import net.minecraft.resources.ResourceLocation;
import org.millenaire.building.BuildingPlanSet;
import org.millenaire.culture.ModCultures;

public final class BuildingCostJsonExporter {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

    private BuildingCostJsonExporter() {
    }

    public static Path export(Path dir) throws IOException {
        ResourceLocation cultureId;
        Path outDir = dir.resolve("building-costs-json");
        Files.createDirectories(outDir, new FileAttribute[0]);
        LinkedHashMap<ResourceLocation, List> byCulture = new LinkedHashMap<ResourceLocation, List>();
        ArrayList<BuildingPlanSet> sets = new ArrayList<BuildingPlanSet>(ModCultures.getAllBuildingPlanSets().values());
        sets.sort(Comparator.comparing(s -> s.id().toString()));
        for (BuildingPlanSet buildingPlanSet : sets) {
            cultureId = buildingPlanSet.culture();
            if (cultureId == null) continue;
            byCulture.computeIfAbsent(cultureId, k -> new ArrayList()).add(buildingPlanSet);
        }
        for (Map.Entry entry : byCulture.entrySet()) {
            cultureId = (ResourceLocation)entry.getKey();
            List cultureSets = (List)entry.getValue();
            LinkedHashMap<String, Object> cultureData = new LinkedHashMap<String, Object>();
            cultureData.put("culture", cultureId.toString());
            ArrayList<Map<String, Object>> buildings = new ArrayList<Map<String, Object>>();
            for (BuildingPlanSet set : cultureSets) {
                buildings.add(BuildingCostJsonExporter.buildSetData(set));
            }
            cultureData.put("buildings", buildings);
            String filename = cultureId.getPath() + ".json";
            Files.writeString(outDir.resolve(filename), (CharSequence)GSON.toJson(cultureData), new OpenOption[0]);
        }
        return outDir;
    }

    private static Map<String, Object> buildSetData(BuildingPlanSet set) {
        LinkedHashMap<String, Object> data = new LinkedHashMap<String, Object>();
        data.put("id", set.id().toString());
        data.put("building_id", set.buildingId());
        ArrayList<String> variantKeys = new ArrayList<String>(set.variants().keySet());
        variantKeys.sort(Comparator.naturalOrder());
        ArrayList variants = new ArrayList();
        for (String variantKey : variantKeys) {
            List<BuildingPlanSet.LevelDef> levels = set.variants().get(variantKey);
            if (levels == null) continue;
            LinkedHashMap<String, Object> variantData = new LinkedHashMap<String, Object>();
            variantData.put("variant", variantKey);
            TreeMap totalCost = new TreeMap();
            for (BuildingPlanSet.LevelDef level : levels) {
                level.requiredResources().forEach((id, qty) -> totalCost.merge(id.toString(), qty, Integer::sum));
            }
            variantData.put("total_cost", totalCost);
            ArrayList levelList = new ArrayList();
            for (BuildingPlanSet.LevelDef level : levels) {
                LinkedHashMap<String, Serializable> levelData = new LinkedHashMap<String, Serializable>();
                levelData.put("level", Integer.valueOf(level.level()));
                TreeMap resources = new TreeMap();
                level.requiredResources().forEach((id, qty) -> resources.put(id.toString(), qty));
                levelData.put("resources", resources);
                levelList.add(levelData);
            }
            variantData.put("levels", levelList);
            variants.add(variantData);
        }
        data.put("variants", variants);
        return data;
    }
}

