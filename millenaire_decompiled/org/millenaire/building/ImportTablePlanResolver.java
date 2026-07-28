/*
 * Decompiled with CFR 0.150.
 * 
 * Could not load the following classes:
 *  com.google.gson.JsonArray
 *  com.google.gson.JsonElement
 *  com.google.gson.JsonObject
 *  com.google.gson.JsonParser
 *  com.mojang.logging.LogUtils
 *  net.minecraft.resources.ResourceLocation
 *  org.slf4j.Logger
 */
package org.millenaire.building;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.mojang.logging.LogUtils;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.TreeMap;
import java.util.stream.Stream;
import net.minecraft.resources.ResourceLocation;
import org.millenaire.building.BuildingPlanSet;
import org.millenaire.building.TemplateLoader;
import org.millenaire.content.ContentFs;
import org.millenaire.content.CustomContentIndex;
import org.millenaire.content.Resource;
import org.millenaire.culture.ModCultures;
import org.slf4j.Logger;

public final class ImportTablePlanResolver {
    private static final Logger LOGGER = LogUtils.getLogger();

    private ImportTablePlanResolver() {
    }

    public static Optional<PlanView> resolvePlan(String cultureKey, String buildingId) {
        Optional<PlanView> optional;
        block12: {
            if (cultureKey == null || cultureKey.isEmpty() || buildingId == null || buildingId.isEmpty()) {
                return Optional.empty();
            }
            Optional<String> categoryOpt = ImportTablePlanResolver.resolveCategory(cultureKey, buildingId);
            if (categoryOpt.isEmpty()) {
                return Optional.empty();
            }
            String category = categoryOpt.get();
            ResourceLocation cultureRl = ResourceLocation.tryParse((String)cultureKey);
            if (cultureRl == null) {
                return Optional.empty();
            }
            String culturePath = cultureRl.getPath();
            String parentDir = "buildings/" + category;
            String jsonRelPath = parentDir + "/" + buildingId + ".json";
            ContentFs cultureFs = TemplateLoader.cultureFsForImport(cultureRl);
            Optional<Resource> jsonRes = cultureFs.findFirst(jsonRelPath);
            if (jsonRes.isEmpty()) {
                return Optional.empty();
            }
            InputStream is = jsonRes.get().open();
            try {
                String content = new String(is.readAllBytes(), StandardCharsets.UTF_8);
                JsonObject root = JsonParser.parseString((String)content).getAsJsonObject();
                optional = Optional.of(ImportTablePlanResolver.parsePlan(root, cultureKey, buildingId, category, parentDir));
                if (is == null) break block12;
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
                catch (Exception e) {
                    LOGGER.warn("ImportTablePlanResolver: failed to read {}: {}", (Object)jsonRelPath, (Object)e.getMessage());
                    return Optional.empty();
                }
            }
            is.close();
        }
        return optional;
    }

    public static Optional<String> resolveCategory(String cultureKey, String buildingId) {
        BuildingPlanSet registrySet;
        if (cultureKey == null || cultureKey.isEmpty() || buildingId == null || buildingId.isEmpty()) {
            return Optional.empty();
        }
        ResourceLocation planSetId = ResourceLocation.tryParse((String)(cultureKey + "/" + buildingId));
        if (planSetId != null && (registrySet = ModCultures.getBuildingPlanSet(planSetId)) != null && registrySet.category() != null && !registrySet.category().isEmpty()) {
            return Optional.of(registrySet.category());
        }
        ResourceLocation cultureRl = ResourceLocation.tryParse((String)cultureKey);
        if (cultureRl == null) {
            return Optional.empty();
        }
        String fileName = buildingId + ".json";
        ContentFs cultureFs = TemplateLoader.cultureFsForImport(cultureRl);
        try (Stream<Resource> walk = cultureFs.walk("buildings", 4);){
            Optional<String> optional = walk.map(Resource::relPath).map(p -> ImportTablePlanResolver.categoryFromBuildingsRelPath(p, fileName)).filter(Optional::isPresent).map(Optional::get).findFirst();
            return optional;
        }
    }

    private static Optional<String> categoryFromBuildingsRelPath(String relPath, String fileName) {
        String[] parts = relPath.split("/");
        if (parts.length >= 3 && fileName.equals(parts[parts.length - 1]) && "buildings".equals(parts[parts.length - 3])) {
            return Optional.of(parts[parts.length - 2]);
        }
        return Optional.empty();
    }

    public static Optional<LevelView> resolveLevel(String cultureKey, String buildingId, String variant, int level) {
        return ImportTablePlanResolver.resolvePlan(cultureKey, buildingId).flatMap(plan -> {
            VariantView v = plan.variants().get(variant);
            if (v == null) {
                return Optional.empty();
            }
            return Optional.ofNullable(v.levels().get(level));
        });
    }

    public static Optional<VariantView> resolveVariant(String cultureKey, String buildingId, String variant) {
        return ImportTablePlanResolver.resolvePlan(cultureKey, buildingId).flatMap(plan -> Optional.ofNullable(plan.variants().get(variant)));
    }

    public static boolean isServedFromExports(String cultureKey, String buildingId) {
        if (cultureKey == null || cultureKey.isEmpty() || buildingId == null || buildingId.isEmpty()) {
            return false;
        }
        ResourceLocation planSetId = ResourceLocation.tryParse((String)(cultureKey + "/" + buildingId));
        if (planSetId == null) {
            return false;
        }
        BuildingPlanSet registrySet = ModCultures.getBuildingPlanSet(planSetId);
        String category = registrySet != null && registrySet.category() != null && !registrySet.category().isEmpty() ? registrySet.category() : "houses";
        ResourceLocation cultureRl = ResourceLocation.tryParse((String)cultureKey);
        if (cultureRl == null) {
            return false;
        }
        String jsonRelPath = "buildings/" + category + "/" + buildingId + ".json";
        return CustomContentIndex.current().exportedFs().sub("cultures/" + cultureRl.getPath()).findFirst(jsonRelPath).isPresent();
    }

    public static Optional<String> formatFromExportsMessage(String tag, List<String> names) {
        if (names == null || names.isEmpty()) {
            return Optional.empty();
        }
        return Optional.of("\u00a7e[" + tag + "] From your exports: " + String.join((CharSequence)", ", names) + " (" + names.size() + ")");
    }

    static PlanView parsePlan(JsonObject root, String cultureKey, String buildingId, String category, String parentDir) {
        JsonArray variantsArray;
        int planSetOrientation = root.has("building_orientation") ? root.get("building_orientation").getAsInt() : 1;
        TreeMap<String, VariantView> variants = new TreeMap<String, VariantView>();
        JsonArray jsonArray = variantsArray = root.has("variants") ? root.getAsJsonArray("variants") : null;
        if (variantsArray != null) {
            for (JsonElement ve : variantsArray) {
                JsonArray levelsArray;
                JsonObject variantObj = ve.getAsJsonObject();
                if (!variantObj.has("variant")) continue;
                String variant = variantObj.get("variant").getAsString();
                int variantOrientation = variantObj.has("building_orientation") ? variantObj.get("building_orientation").getAsInt() : planSetOrientation;
                int variantGroundLevel = variantObj.has("ground_level") ? variantObj.get("ground_level").getAsInt() : 0;
                LinkedHashMap<Integer, LevelView> levels = new LinkedHashMap<Integer, LevelView>();
                JsonArray jsonArray2 = levelsArray = variantObj.has("levels") ? variantObj.getAsJsonArray("levels") : null;
                if (levelsArray != null) {
                    for (JsonElement le : levelsArray) {
                        JsonObject levelObj = le.getAsJsonObject();
                        if (!levelObj.has("level")) continue;
                        int level = levelObj.get("level").getAsInt();
                        int width = 0;
                        int height = 0;
                        int depth = 0;
                        if (levelObj.has("footprint")) {
                            JsonObject fp = levelObj.getAsJsonObject("footprint");
                            width = fp.has("width") ? fp.get("width").getAsInt() : 0;
                            height = fp.has("height") ? fp.get("height").getAsInt() : 0;
                            depth = fp.has("depth") ? fp.get("depth").getAsInt() : 0;
                        }
                        int groundLevel = levelObj.has("ground_level") ? levelObj.get("ground_level").getAsInt() : variantGroundLevel;
                        String nbtPath = parentDir + "/" + buildingId + "_" + variant + "_" + level;
                        levels.put(level, new LevelView(level, width, height, depth, groundLevel, variantOrientation, nbtPath));
                    }
                }
                variants.put(variant, new VariantView(variant, variantOrientation, levels));
            }
        }
        return new PlanView(cultureKey, buildingId, category, variants);
    }

    public record PlanView(String culture, String buildingId, String category, Map<String, VariantView> variants) {
    }

    public record LevelView(int level, int width, int height, int depth, int groundLevel, int buildingOrientation, String nbtPath) {
    }

    public record VariantView(String variant, int buildingOrientation, Map<Integer, LevelView> levels) {
    }
}

