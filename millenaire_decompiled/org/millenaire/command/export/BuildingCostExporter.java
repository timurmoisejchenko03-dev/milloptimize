/*
 * Decompiled with CFR 0.150.
 * 
 * Could not load the following classes:
 *  net.minecraft.resources.ResourceLocation
 *  net.minecraft.world.item.Item
 */
package org.millenaire.command.export;

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
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import org.millenaire.building.BuildingPlanSet;
import org.millenaire.culture.ModCultures;
import org.millenaire.item.ItemHelper;

public final class BuildingCostExporter {
    private BuildingCostExporter() {
    }

    public static Path export(Path dir) throws IOException {
        ResourceLocation cultureId;
        Path outDir = dir.resolve("building-costs");
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
            StringBuilder sb = new StringBuilder();
            for (BuildingPlanSet set : cultureSets) {
                BuildingCostExporter.appendSet(sb, set);
                sb.append("\n");
            }
            String filename = cultureId.getPath() + " resources used.txt";
            Files.writeString(outDir.resolve(filename), (CharSequence)sb.toString(), new OpenOption[0]);
        }
        return outDir;
    }

    private static void appendSet(StringBuilder sb, BuildingPlanSet set) {
        sb.append(set.nativeName()).append("\n");
        sb.append(set.id().getPath()).append("\n");
        sb.append("\n");
        ArrayList<String> variants = new ArrayList<String>(set.variants().keySet());
        variants.sort(Comparator.naturalOrder());
        for (int vi = 0; vi < variants.size(); ++vi) {
            String variant = (String)variants.get(vi);
            List<BuildingPlanSet.LevelDef> levels = set.variants().get(variant);
            if (levels == null) continue;
            if (variants.size() > 1) {
                sb.append("===Variation ").append((char)(65 + vi)).append("===\n");
            }
            LinkedHashMap<ResourceLocation, Integer> totalCost = new LinkedHashMap<ResourceLocation, Integer>();
            for (BuildingPlanSet.LevelDef level : levels) {
                level.requiredResources().forEach((id, qty) -> totalCost.merge((ResourceLocation)id, (Integer)qty, Integer::sum));
            }
            sb.append("\nTotal Cost\n");
            BuildingCostExporter.appendItems(sb, totalCost);
            for (BuildingPlanSet.LevelDef level : levels) {
                if (level.level() == 0) {
                    sb.append("\nInitial Construction\n");
                } else {
                    sb.append("\nUpgrade ").append(level.level()).append("\n");
                }
                BuildingCostExporter.appendItems(sb, level.requiredResources());
            }
        }
        sb.append("\n");
    }

    private static void appendItems(StringBuilder sb, Map<ResourceLocation, Integer> resources) {
        ArrayList<Map.Entry<ResourceLocation, Integer>> entries = new ArrayList<Map.Entry<ResourceLocation, Integer>>(resources.entrySet());
        entries.sort(Comparator.comparing(e -> ((ResourceLocation)e.getKey()).toString()));
        for (Map.Entry entry : entries) {
            String itemName = BuildingCostExporter.getItemDisplayName((ResourceLocation)entry.getKey());
            sb.append(itemName).append("(").append(entry.getKey()).append("): ").append(entry.getValue()).append("\n");
        }
    }

    private static String getItemDisplayName(ResourceLocation itemId) {
        Item item = ItemHelper.resolve(itemId);
        if (item == null) {
            return itemId.getPath();
        }
        String name = item.getDefaultInstance().getDisplayName().getString();
        if (name.startsWith("[") && name.endsWith("]")) {
            name = name.substring(1, name.length() - 1);
        }
        return name;
    }
}

