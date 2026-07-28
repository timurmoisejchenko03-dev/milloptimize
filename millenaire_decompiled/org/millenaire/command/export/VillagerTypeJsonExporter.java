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
import org.millenaire.culture.ModCultures;
import org.millenaire.culture.VillagerType;

public final class VillagerTypeJsonExporter {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final ResourceLocation GET_TOOL_GOAL = ResourceLocation.fromNamespaceAndPath((String)"millenaire", (String)"get_tool");

    private VillagerTypeJsonExporter() {
    }

    public static Path export(Path dir) throws IOException {
        ResourceLocation cultureId;
        Path outDir = dir.resolve("villager-types-json");
        Files.createDirectories(outDir, new FileAttribute[0]);
        LinkedHashMap<ResourceLocation, List> byCulture = new LinkedHashMap<ResourceLocation, List>();
        ArrayList<VillagerType> allTypes = new ArrayList<VillagerType>(ModCultures.getAllVillagerTypes().values());
        allTypes.sort(Comparator.comparing(v -> v.id().toString()));
        for (VillagerType villagerType : allTypes) {
            cultureId = villagerType.culture();
            if (cultureId == null) continue;
            byCulture.computeIfAbsent(cultureId, k -> new ArrayList()).add(villagerType);
        }
        for (Map.Entry entry : byCulture.entrySet()) {
            cultureId = (ResourceLocation)entry.getKey();
            List cultureTypes = (List)entry.getValue();
            LinkedHashMap<String, Object> cultureData = new LinkedHashMap<String, Object>();
            cultureData.put("culture", cultureId.toString());
            ArrayList<Map<String, Object>> villagers = new ArrayList<Map<String, Object>>();
            for (VillagerType vt : cultureTypes) {
                villagers.add(VillagerTypeJsonExporter.buildVillagerData(vt));
            }
            cultureData.put("villagers", villagers);
            String filename = cultureId.getPath() + ".json";
            Files.writeString(outDir.resolve(filename), (CharSequence)GSON.toJson(cultureData), new OpenOption[0]);
        }
        return outDir;
    }

    private static Map<String, Object> buildVillagerData(VillagerType vt) {
        String getToolStr;
        LinkedHashMap<String, Object> data = new LinkedHashMap<String, Object>();
        data.put("id", vt.id().getPath());
        data.put("native_name", vt.nativeName());
        data.put("gender", vt.gender().name().toLowerCase());
        List<String> goals = vt.goals().stream().map(ResourceLocation::toString).toList();
        data.put("goals", goals);
        ArrayList<String> goalsEffective = new ArrayList<String>(goals);
        if (!vt.toolNeededClasses().isEmpty() && !goalsEffective.contains(getToolStr = GET_TOOL_GOAL.toString())) {
            goalsEffective.add(getToolStr);
        }
        data.put("goals_effective", goalsEffective);
        data.put("tags", vt.tags());
        data.put("tool_needed_classes", vt.toolNeededClasses());
        data.put("bring_back_home_goods", vt.bringBackHomeGoods());
        data.put("collect_goods", vt.collectGoods());
        TreeMap<String, Integer> requiredGoods = new TreeMap<String, Integer>();
        for (Map.Entry<String, Integer> entry : vt.requiredGoods().entrySet()) {
            requiredGoods.put(entry.getKey(), entry.getValue());
        }
        data.put("required_goods", requiredGoods);
        TreeMap<String, Integer> initialInventory = new TreeMap<String, Integer>();
        for (Map.Entry<ResourceLocation, Integer> entry : vt.initialInventory().entrySet()) {
            initialInventory.put(entry.getKey().toString(), entry.getValue());
        }
        data.put("initial_inventory", initialInventory);
        data.put("max_health", Float.valueOf(vt.maxHealth()));
        data.put("base_scale", Float.valueOf(vt.baseScale()));
        data.put("is_child", vt.isChild());
        data.put("spawn_weight", vt.spawnWeight());
        data.put("hiring_cost", vt.hiringCost());
        data.put("first_name_list", vt.firstNameList());
        data.put("family_name_list", vt.familyNameList());
        data.put("male_child", vt.maleChild());
        data.put("female_child", vt.femaleChild());
        data.put("textures_count", vt.textures().size());
        ArrayList<String> arrayList = new ArrayList<String>(vt.clothes().keySet());
        arrayList.sort(Comparator.naturalOrder());
        data.put("clothes_types", arrayList);
        return data;
    }
}

