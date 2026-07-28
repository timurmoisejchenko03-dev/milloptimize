/*
 * Decompiled with CFR 0.150.
 * 
 * Could not load the following classes:
 *  com.google.gson.Gson
 *  com.google.gson.GsonBuilder
 *  com.mojang.logging.LogUtils
 *  net.minecraft.nbt.CompoundTag
 *  net.minecraft.nbt.NbtAccounter
 *  net.minecraft.nbt.NbtIo
 *  net.minecraft.resources.ResourceLocation
 *  net.minecraft.server.level.ServerLevel
 *  net.minecraft.world.item.Item
 *  org.slf4j.Logger
 */
package org.millenaire.command.export;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.mojang.logging.LogUtils;
import java.io.IOException;
import java.io.InputStream;
import java.io.Serializable;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.OpenOption;
import java.nio.file.Path;
import java.nio.file.attribute.FileAttribute;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.TreeMap;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtAccounter;
import net.minecraft.nbt.NbtIo;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.Item;
import org.millenaire.building.BuildingCostCalculator;
import org.millenaire.building.BuildingExporter;
import org.millenaire.item.ItemHelper;
import org.slf4j.Logger;

public final class ExportedBuildingsCostExporter {
    private static final Logger LOGGER = LogUtils.getLogger();
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

    private ExportedBuildingsCostExporter() {
    }

    public static Path export(ServerLevel level, Path dir) throws IOException {
        Path exportedRoot;
        TreeMap<String, Map<String, Map<Integer, Path>>> grouped = new TreeMap<String, Map<String, Map<Integer, Path>>>();
        Path legacyDir = BuildingExporter.getLegacyExportDir(level);
        if (Files.isDirectory(legacyDir, new LinkOption[0])) {
            ExportedBuildingsCostExporter.scanFlatDir(legacyDir, grouped);
        }
        if (Files.isDirectory(exportedRoot = BuildingExporter.getExportedRoot(level).resolve("cultures"), new LinkOption[0])) {
            try (DirectoryStream<Path> cultures = Files.newDirectoryStream(exportedRoot);){
                for (Path culturePath : cultures) {
                    Path buildingsDir = culturePath.resolve("buildings");
                    if (!Files.isDirectory(buildingsDir, new LinkOption[0])) continue;
                    DirectoryStream<Path> categories = Files.newDirectoryStream(buildingsDir);
                    try {
                        for (Path path : categories) {
                            if (!Files.isDirectory(path, new LinkOption[0])) continue;
                            ExportedBuildingsCostExporter.scanFlatDir(path, grouped);
                        }
                    }
                    finally {
                        if (categories == null) continue;
                        categories.close();
                    }
                }
            }
        }
        if (grouped.isEmpty()) {
            return null;
        }
        Path outDir = dir.resolve("exported-building-costs");
        Files.createDirectories(outDir, new FileAttribute[0]);
        LinkedHashMap<String, Map<String, Map<Integer, Map<ResourceLocation, Integer>>>> costs = new LinkedHashMap<String, Map<String, Map<Integer, Map<ResourceLocation, Integer>>>>();
        for (Map.Entry buildingEntry : grouped.entrySet()) {
            LinkedHashMap perVariant = new LinkedHashMap();
            for (Map.Entry entry : ((Map)buildingEntry.getValue()).entrySet()) {
                LinkedHashMap<Integer, Map<ResourceLocation, Integer>> perLevel = new LinkedHashMap<Integer, Map<ResourceLocation, Integer>>();
                for (Map.Entry levelEntry : ((Map)entry.getValue()).entrySet()) {
                    Map<ResourceLocation, Integer> cost = ExportedBuildingsCostExporter.computeCost((Path)levelEntry.getValue());
                    if (cost == null) continue;
                    perLevel.put((Integer)levelEntry.getKey(), cost);
                }
                if (perLevel.isEmpty()) continue;
                perVariant.put((String)entry.getKey(), perLevel);
            }
            if (perVariant.isEmpty()) continue;
            costs.put((String)buildingEntry.getKey(), perVariant);
        }
        ExportedBuildingsCostExporter.writeTextFile(outDir, costs);
        ExportedBuildingsCostExporter.writeJsonFile(outDir, costs);
        return outDir;
    }

    private static void scanFlatDir(Path dir, Map<String, Map<String, Map<Integer, Path>>> grouped) throws IOException {
        try (DirectoryStream<Path> stream = Files.newDirectoryStream(dir, "*.nbt");){
            for (Path nbtFile : stream) {
                Parsed parsed = ExportedBuildingsCostExporter.parseName(nbtFile.getFileName().toString());
                if (parsed == null) continue;
                grouped.computeIfAbsent(parsed.buildingId, k -> new TreeMap()).computeIfAbsent(parsed.variant, k -> new TreeMap()).put(parsed.level, nbtFile);
            }
        }
    }

    private static Parsed parseName(String filename) {
        int level;
        if (!filename.endsWith(".nbt")) {
            return null;
        }
        String stem = filename.substring(0, filename.length() - 4);
        int lastUnderscore = stem.lastIndexOf(95);
        if (lastUnderscore < 0) {
            return null;
        }
        String levelStr = stem.substring(lastUnderscore + 1);
        try {
            level = Integer.parseInt(levelStr);
        }
        catch (NumberFormatException e) {
            return null;
        }
        String rest = stem.substring(0, lastUnderscore);
        int variantUnderscore = rest.lastIndexOf(95);
        if (variantUnderscore < 0) {
            return null;
        }
        String variant = rest.substring(variantUnderscore + 1);
        String buildingId = rest.substring(0, variantUnderscore);
        if (buildingId.isEmpty()) {
            return null;
        }
        return new Parsed(buildingId, variant, level);
    }

    private static Map<ResourceLocation, Integer> computeCost(Path nbtPath) {
        Map<ResourceLocation, Integer> map;
        block8: {
            InputStream is = Files.newInputStream(nbtPath, new OpenOption[0]);
            try {
                CompoundTag nbt = NbtIo.readCompressed((InputStream)is, (NbtAccounter)NbtAccounter.unlimitedHeap());
                map = BuildingCostCalculator.computeCost(nbt);
                if (is == null) break block8;
            }
            catch (Throwable throwable) {
                try {
                    if (is != null) {
                        try {
                            is.close();
                        }
                        catch (Throwable throwable2) {
                            throwable.addSuppressed(throwable2);
                        }
                    }
                    throw throwable;
                }
                catch (IOException e) {
                    LOGGER.warn("Failed to load NBT for cost calculation: {}", (Object)nbtPath, (Object)e);
                    return null;
                }
            }
            is.close();
        }
        return map;
    }

    private static void writeTextFile(Path outDir, Map<String, Map<String, Map<Integer, Map<ResourceLocation, Integer>>>> costs) throws IOException {
        StringBuilder sb = new StringBuilder();
        for (Map.Entry<String, Map<String, Map<Integer, Map<ResourceLocation, Integer>>>> buildingEntry : costs.entrySet()) {
            String buildingId = buildingEntry.getKey();
            sb.append(buildingId).append("\n\n");
            Map<String, Map<Integer, Map<ResourceLocation, Integer>>> variants = buildingEntry.getValue();
            ArrayList<String> variantKeys = new ArrayList<String>(variants.keySet());
            for (int vi = 0; vi < variantKeys.size(); ++vi) {
                String variant = (String)variantKeys.get(vi);
                Map<Integer, Map<ResourceLocation, Integer>> levels = variants.get(variant);
                if (variantKeys.size() > 1) {
                    sb.append("===Variation ").append((char)(65 + vi)).append("===\n");
                }
                LinkedHashMap<ResourceLocation, Integer> totalCost = new LinkedHashMap<ResourceLocation, Integer>();
                for (Map<ResourceLocation, Integer> map : levels.values()) {
                    map.forEach((id, qty) -> totalCost.merge((ResourceLocation)id, (Integer)qty, Integer::sum));
                }
                sb.append("\nTotal Cost\n");
                ExportedBuildingsCostExporter.appendItems(sb, totalCost);
                for (Map.Entry entry : levels.entrySet()) {
                    if ((Integer)entry.getKey() == 0) {
                        sb.append("\nInitial Construction\n");
                    } else {
                        sb.append("\nUpgrade ").append(entry.getKey()).append("\n");
                    }
                    ExportedBuildingsCostExporter.appendItems(sb, (Map)entry.getValue());
                }
            }
            sb.append("\n\n");
        }
        Files.writeString(outDir.resolve("exports resources used.txt"), (CharSequence)sb.toString(), new OpenOption[0]);
    }

    private static void writeJsonFile(Path outDir, Map<String, Map<String, Map<Integer, Map<ResourceLocation, Integer>>>> costs) throws IOException {
        ArrayList buildings = new ArrayList();
        for (Map.Entry<String, Map<String, Map<Integer, Map<ResourceLocation, Integer>>>> buildingEntry : costs.entrySet()) {
            LinkedHashMap<String, Object> buildingData = new LinkedHashMap<String, Object>();
            buildingData.put("building_id", buildingEntry.getKey());
            ArrayList variantList = new ArrayList();
            for (Map.Entry<String, Map<Integer, Map<ResourceLocation, Integer>>> variantEntry : buildingEntry.getValue().entrySet()) {
                LinkedHashMap<String, Object> variantData = new LinkedHashMap<String, Object>();
                variantData.put("variant", variantEntry.getKey());
                TreeMap totalCost = new TreeMap();
                for (Map<ResourceLocation, Integer> levelCost : variantEntry.getValue().values()) {
                    levelCost.forEach((id, qty) -> totalCost.merge(id.toString(), qty, Integer::sum));
                }
                variantData.put("total_cost", totalCost);
                ArrayList levelList = new ArrayList();
                for (Map.Entry<Integer, Map<ResourceLocation, Integer>> levelEntry : variantEntry.getValue().entrySet()) {
                    LinkedHashMap<String, Serializable> levelData = new LinkedHashMap<String, Serializable>();
                    levelData.put("level", levelEntry.getKey());
                    TreeMap resources = new TreeMap();
                    levelEntry.getValue().forEach((id, qty) -> resources.put(id.toString(), qty));
                    levelData.put("resources", resources);
                    levelList.add(levelData);
                }
                variantData.put("levels", levelList);
                variantList.add(variantData);
            }
            buildingData.put("variants", variantList);
            buildings.add(buildingData);
        }
        LinkedHashMap root = new LinkedHashMap();
        root.put("buildings", buildings);
        Files.writeString(outDir.resolve("exports.json"), (CharSequence)GSON.toJson(root), new OpenOption[0]);
    }

    private static void appendItems(StringBuilder sb, Map<ResourceLocation, Integer> resources) {
        ArrayList<Map.Entry<ResourceLocation, Integer>> entries = new ArrayList<Map.Entry<ResourceLocation, Integer>>(resources.entrySet());
        entries.sort(Comparator.comparing(e -> ((ResourceLocation)e.getKey()).toString()));
        for (Map.Entry entry : entries) {
            String itemName = ExportedBuildingsCostExporter.getItemDisplayName((ResourceLocation)entry.getKey());
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

    private record Parsed(String buildingId, String variant, int level) {
    }
}

