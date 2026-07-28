/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.google.gson.JsonElement
 *  com.google.gson.JsonObject
 *  com.mojang.logging.LogUtils
 *  javax.annotation.Nullable
 *  net.minecraft.core.BlockPos
 *  net.minecraft.nbt.CompoundTag
 *  net.minecraft.nbt.NbtAccounter
 *  net.minecraft.nbt.NbtIo
 *  net.minecraft.resources.ResourceLocation
 *  net.minecraft.util.GsonHelper
 *  net.minecraft.world.item.DyeColor
 *  org.slf4j.Logger
 */
package org.millenaire.culture;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.mojang.logging.LogUtils;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import javax.annotation.Nullable;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtAccounter;
import net.minecraft.nbt.NbtIo;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.GsonHelper;
import net.minecraft.world.item.DyeColor;
import org.millenaire.building.BuildingCostCalculator;
import org.millenaire.building.BuildingPlan;
import org.millenaire.building.BuildingPlanSet;
import org.millenaire.building.ClearMargins;
import org.millenaire.building.SpecialPoint;
import org.millenaire.building.SpecialPointsLoader;
import org.millenaire.building.TemplateLoader;
import org.millenaire.content.ContentFs;
import org.millenaire.content.CustomContentIndex;
import org.millenaire.content.Resource;
import org.millenaire.culture.CrossFolderConflictException;
import org.millenaire.culture.CultureLoader;
import org.millenaire.culture.JsonLoaderUtils;
import org.millenaire.culture.ModCultures;
import org.millenaire.village.BrickColourTheme;
import org.slf4j.Logger;

final class BuildingPlanSetLoader {
    private static final Logger LOGGER = LogUtils.getLogger();
    private static final Map<ResourceLocation, String> SEEN_BUILDING_IDS = new HashMap<ResourceLocation, String>();
    private static final Set<ResourceLocation> FAILED_CULTURES = new HashSet<ResourceLocation>();

    private BuildingPlanSetLoader() {
    }

    static void resetCrossFolderTracking() {
        SEEN_BUILDING_IDS.clear();
        FAILED_CULTURES.clear();
    }

    static void loadFromContentFs(String contentId, ContentFs cultureFs, String relPath, String pathCategory) {
        String culture = contentId.contains("/") ? contentId.substring(0, contentId.indexOf(47)) : null;
        String synthClasspath = culture != null ? "/millenaire/cultures/" + culture + "/" + relPath : relPath;
        BuildingPlanSetLoader.loadFromOverlay(contentId, synthClasspath, cultureFs, relPath, pathCategory, culture);
    }

    static void loadFromClasspath(String contentId, String classpathPath, String pathCategory) {
        String culturePathPrefix = BuildingPlanSetLoader.extractCulturePrefix(classpathPath);
        if (culturePathPrefix == null) {
            return;
        }
        ContentFs cultureFs = CustomContentIndex.current().forCulture(culturePathPrefix);
        String relPath = BuildingPlanSetLoader.relativiseToCulture(classpathPath, culturePathPrefix);
        if (relPath == null) {
            return;
        }
        BuildingPlanSetLoader.loadFromOverlay(contentId, classpathPath, cultureFs, relPath, pathCategory, culturePathPrefix);
    }

    private static void loadFromOverlay(String contentId, String classpathPath, ContentFs cultureFs, String relPath, String pathCategory, String expectedCulture) {
        JsonObject json = CultureLoader.readJsonFromContentFs(cultureFs, relPath);
        if (json == null) {
            return;
        }
        try {
            boolean isTownHall;
            double maxDistance;
            String jsonStem;
            ResourceLocation culture = ResourceLocation.parse((String)GsonHelper.getAsString((JsonObject)json, (String)"culture"));
            String buildingId = GsonHelper.getAsString((JsonObject)json, (String)"building_id");
            String category = GsonHelper.getAsString((JsonObject)json, (String)"category", (String)"houses");
            if (expectedCulture != null && !culture.getPath().equals(expectedCulture)) {
                cultureFs = CustomContentIndex.current().forCulture(culture.getPath());
            }
            if ((jsonStem = BuildingPlanSetLoader.filenameStem(classpathPath)) != null && !jsonStem.equalsIgnoreCase(buildingId)) {
                LOGGER.error("[Millenaire] Building plan {} \u2014 filename stem '{}' does not match building_id '{}'. Rename the file (or change the id) so they match. Run ./gradlew convertAll to regenerate.", new Object[]{contentId, jsonStem, buildingId});
                return;
            }
            if (pathCategory != null && !pathCategory.equals(category)) {
                LOGGER.warn("[Millenaire] Building plan {} has category '{}' in JSON but lives under path category '{}'. JSON field wins; consider moving the file to the matching folder.", new Object[]{contentId, category, pathCategory});
            }
            String nativeName = GsonHelper.getAsString((JsonObject)json, (String)"native_name", (String)buildingId);
            int maxCount = GsonHelper.getAsInt((JsonObject)json, (String)"max_count", (int)1);
            double minDistance = json.has("min_distance") ? json.get("min_distance").getAsDouble() : 0.0;
            double d = maxDistance = json.has("max_distance") ? json.get("max_distance").getAsDouble() : 1.0;
            List<String> maleResidents = json.has("male_residents") ? JsonLoaderUtils.parseStringList(GsonHelper.getAsJsonArray((JsonObject)json, (String)"male_residents")) : (json.has("male") && !json.get("male").isJsonNull() ? CultureLoader.parseStringOrArray(json.get("male")) : List.of());
            List<String> femaleResidents = json.has("female_residents") ? JsonLoaderUtils.parseStringList(GsonHelper.getAsJsonArray((JsonObject)json, (String)"female_residents")) : (json.has("female") && !json.get("female").isJsonNull() ? CultureLoader.parseStringOrArray(json.get("female")) : List.of());
            List<String> visitors = json.has("visitors") && !json.get("visitors").isJsonNull() ? JsonLoaderUtils.parseStringList(GsonHelper.getAsJsonArray((JsonObject)json, (String)"visitors")) : List.of();
            int priorityMoveIn = GsonHelper.getAsInt((JsonObject)json, (String)"priority_move_in", (int)-1);
            List<String> tags = json.has("tags") ? JsonLoaderUtils.parseStringList(GsonHelper.getAsJsonArray((JsonObject)json, (String)"tags")) : List.of();
            String terrainPolicy = GsonHelper.getAsString((JsonObject)json, (String)"terrain_policy", (String)"clear_and_flatten");
            String constructionOrder = GsonHelper.getAsString((JsonObject)json, (String)"construction_order", (String)"bottom_up");
            String culturePrefix = culture.getPath();
            ResourceLocation setId = ResourceLocation.fromNamespaceAndPath((String)"millenaire", (String)(culturePrefix + "/" + buildingId));
            if (FAILED_CULTURES.contains(culture)) {
                return;
            }
            String previousJsonPath = SEEN_BUILDING_IDS.putIfAbsent(setId, classpathPath);
            if (previousJsonPath != null && !previousJsonPath.equals(classpathPath)) {
                LOGGER.error("[Millenaire] Duplicate building_id '{}': loaded from '{}', later occurrence at '{}'. Cross-folder logical-id collisions are fatal \u2014 culture '{}' will not be registered. Move or rename one of the files so each building_id is unique within the culture.", new Object[]{setId, previousJsonPath, classpathPath, culture});
                FAILED_CULTURES.add(culture);
                throw new CrossFolderConflictException(culture, "Duplicate building_id '" + String.valueOf(setId) + "' (first at '" + previousJsonPath + "', second at '" + classpathPath + "')");
            }
            String nbtPathParent = BuildingPlanSetLoader.nbtPathParentForCulturePath(classpathPath, culturePrefix);
            ArrayList<BuildingPlan> pendingPlans = new ArrayList<BuildingPlan>();
            LinkedHashMap<String, List<BuildingPlanSet.LevelDef>> variants = new LinkedHashMap<String, List<BuildingPlanSet.LevelDef>>();
            for (JsonElement variantEl : GsonHelper.getAsJsonArray((JsonObject)json, (String)"variants")) {
                JsonObject variantObj = variantEl.getAsJsonObject();
                String variant = GsonHelper.getAsString((JsonObject)variantObj, (String)"variant");
                ArrayList<BuildingPlanSet.LevelDef> levels = new ArrayList<BuildingPlanSet.LevelDef>();
                int variantGroundLevel = GsonHelper.getAsInt((JsonObject)variantObj, (String)"ground_level", (int)0);
                List<String> variantTags = variantObj.has("tags") ? JsonLoaderUtils.parseStringList(GsonHelper.getAsJsonArray((JsonObject)variantObj, (String)"tags")) : null;
                int variantOrientation = BuildingPlanSetLoader.resolveVariantOrientation(json, variantObj);
                int prevPathLevel = 0;
                int prevPathWidth = 2;
                int prevIrrigation = 0;
                for (JsonElement levelEl : GsonHelper.getAsJsonArray((JsonObject)variantObj, (String)"levels")) {
                    int irrigation;
                    CompoundTag templateNbt;
                    JsonObject levelObj = levelEl.getAsJsonObject();
                    int level = GsonHelper.getAsInt((JsonObject)levelObj, (String)"level");
                    if (levelObj.has("template")) {
                        LOGGER.warn("[Millenaire] Building plan {} carries the obsolete 'template:' field at variant '{}' level {} \u2014 ignored, implicit NBT path '{}_{}_{}' is used instead. Re-run the converter (or delete the deployed millenaire/ directory to force redeployment) to silence this warning.", new Object[]{classpathPath, variant, level, buildingId, variant, level});
                    }
                    String nbtPath = nbtPathParent.isEmpty() ? buildingId + "_" + variant + "_" + level : nbtPathParent + "/" + buildingId + "_" + variant + "_" + level;
                    JsonObject footprint = GsonHelper.getAsJsonObject((JsonObject)levelObj, (String)"footprint");
                    int width = GsonHelper.getAsInt((JsonObject)footprint, (String)"width");
                    int height = GsonHelper.getAsInt((JsonObject)footprint, (String)"height");
                    int depth = GsonHelper.getAsInt((JsonObject)footprint, (String)"depth");
                    int groundLevel = GsonHelper.getAsInt((JsonObject)levelObj, (String)"ground_level", (int)variantGroundLevel);
                    int priority = GsonHelper.getAsInt((JsonObject)levelObj, (String)"priority", (int)100);
                    String levelName = levelObj.has("native_name") ? GsonHelper.getAsString((JsonObject)levelObj, (String)"native_name") : null;
                    ResourceLocation planId = ResourceLocation.fromNamespaceAndPath((String)"millenaire", (String)(culturePrefix + "/" + buildingId + "_" + variant + "_" + level));
                    List<String> levelTags = levelObj.has("tags") ? JsonLoaderUtils.parseStringList(GsonHelper.getAsJsonArray((JsonObject)levelObj, (String)"tags")) : (variantTags != null ? variantTags : List.of());
                    List<String> requiredTags = levelObj.has("required_tags") ? JsonLoaderUtils.parseStringList(GsonHelper.getAsJsonArray((JsonObject)levelObj, (String)"required_tags")) : List.of();
                    List<String> forbiddenTagsInVillage = levelObj.has("forbidden_tags_in_village") ? JsonLoaderUtils.parseStringList(GsonHelper.getAsJsonArray((JsonObject)levelObj, (String)"forbidden_tags_in_village")) : List.of();
                    List<String> requiredVillageTags = levelObj.has("required_village_tags") ? JsonLoaderUtils.parseStringList(GsonHelper.getAsJsonArray((JsonObject)levelObj, (String)"required_village_tags")) : List.of();
                    List<String> parentTags = levelObj.has("parent_tags") ? JsonLoaderUtils.parseStringList(GsonHelper.getAsJsonArray((JsonObject)levelObj, (String)"parent_tags")) : List.of();
                    List<String> requiredParentTags = levelObj.has("required_parent_tags") ? JsonLoaderUtils.parseStringList(GsonHelper.getAsJsonArray((JsonObject)levelObj, (String)"required_parent_tags")) : List.of();
                    List<String> clearTags = levelObj.has("clear_tags") ? JsonLoaderUtils.parseStringList(GsonHelper.getAsJsonArray((JsonObject)levelObj, (String)"clear_tags")) : List.of();
                    List<String> villageTags = levelObj.has("village_tags") ? JsonLoaderUtils.parseStringList(GsonHelper.getAsJsonArray((JsonObject)levelObj, (String)"village_tags")) : List.of();
                    List<String> levelSubBuildings = levelObj.has("sub_buildings") ? JsonLoaderUtils.parseStringList(GsonHelper.getAsJsonArray((JsonObject)levelObj, (String)"sub_buildings")) : List.of();
                    Optional<Resource> nbtResource = cultureFs.findFirst(nbtPath + ".nbt");
                    if (nbtResource.isEmpty()) {
                        LOGGER.warn("[Millenaire] Plan set '{}' for culture '{}' dropped because NBT missing for variant='{}' level={} (expected at: {}.nbt). Run ./gradlew convertAll to regenerate or ship the missing file.", new Object[]{setId, culture, variant, level, nbtPath});
                        return;
                    }
                    Resource res = nbtResource.get();
                    NbtAccounter accounter = TemplateLoader.accounterFor(res.kind());
                    try (InputStream nbtIs = res.open();){
                        templateNbt = NbtIo.readCompressed((InputStream)nbtIs, (NbtAccounter)accounter);
                    }
                    catch (Exception e) {
                        LOGGER.warn("[Millenaire] Plan set '{}' for culture '{}' dropped because NBT at '{}.nbt' could not be read for variant='{}' level={}: {}", new Object[]{setId, culture, nbtPath, variant, level, e.getMessage()});
                        return;
                    }
                    Map<ResourceLocation, Integer> requiredResources = BuildingCostCalculator.computeCost(templateNbt);
                    int[] signOrder = null;
                    if (levelObj.has("signs")) {
                        String signsStr = GsonHelper.getAsString((JsonObject)levelObj, (String)"signs");
                        String[] parts = signsStr.split(",");
                        signOrder = new int[parts.length];
                        for (int s = 0; s < parts.length; ++s) {
                            signOrder[s] = Integer.parseInt(parts[s].trim());
                        }
                    }
                    PathFields pathFields = BuildingPlanSetLoader.resolvePathFields(levelObj, prevPathLevel, prevPathWidth);
                    int pathLevel = pathFields.pathLevel();
                    boolean rebuildPath = pathFields.rebuildPath();
                    int pathWidth = pathFields.pathWidth();
                    prevPathLevel = pathLevel;
                    prevPathWidth = pathWidth;
                    prevIrrigation = irrigation = GsonHelper.getAsInt((JsonObject)levelObj, (String)"irrigation", (int)prevIrrigation);
                    HashMap<String, Integer> abstractedProduction = new HashMap<String, Integer>();
                    if (levelObj.has("abstracted_production")) {
                        for (JsonElement apElem : GsonHelper.getAsJsonArray((JsonObject)levelObj, (String)"abstracted_production")) {
                            String entry = apElem.getAsString();
                            String[] apParts = entry.split(",");
                            if (apParts.length != 2) continue;
                            abstractedProduction.put(apParts[0].trim(), Integer.parseInt(apParts[1].trim()));
                        }
                    }
                    levels.add(new BuildingPlanSet.LevelDef(level, planId, nbtPath, width, height, depth, groundLevel, priority, levelName, requiredTags, forbiddenTagsInVillage, requiredVillageTags, parentTags, requiredParentTags, clearTags, villageTags, requiredResources, levelSubBuildings, signOrder, pathLevel, rebuildPath, pathWidth, Map.copyOf(abstractedProduction), irrigation));
                    ResourceLocation legacyTemplateKey = ResourceLocation.fromNamespaceAndPath((String)"millenaire", (String)(culturePrefix + "/" + buildingId + "_" + variant + "_" + level));
                    List<SpecialPoint> specialPoints = SpecialPointsLoader.load(legacyTemplateKey, templateNbt);
                    if (specialPoints.isEmpty()) {
                        specialPoints = SpecialPointsLoader.load(planId, nbtPath, cultureFs);
                    }
                    BlockPos entryOffset = BlockPos.ZERO;
                    String shopId = json.has("shop") ? GsonHelper.getAsString((JsonObject)json, (String)"shop") : null;
                    pendingPlans.add(new BuildingPlan(planId, culture, nbtPath, width, height, depth, groundLevel, variantOrientation, entryOffset, levelTags, constructionOrder, terrainPolicy, specialPoints, shopId));
                }
                variants.put(variant, levels);
            }
            if (priorityMoveIn < 0) {
                block16: for (JsonElement variantEl2 : GsonHelper.getAsJsonArray((JsonObject)json, (String)"variants")) {
                    for (JsonElement levelEl2 : GsonHelper.getAsJsonArray((JsonObject)variantEl2.getAsJsonObject(), (String)"levels")) {
                        JsonObject lo = levelEl2.getAsJsonObject();
                        if (!lo.has("priority_move_in")) continue;
                        priorityMoveIn = GsonHelper.getAsInt((JsonObject)lo, (String)"priority_move_in");
                        break block16;
                    }
                }
                if (priorityMoveIn < 0) {
                    priorityMoveIn = 10;
                }
            }
            List<String> startingSubBuildings = json.has("starting_sub_buildings") ? JsonLoaderUtils.parseStringList(GsonHelper.getAsJsonArray((JsonObject)json, (String)"starting_sub_buildings")) : List.of();
            String icon = json.has("icon") ? GsonHelper.getAsString((JsonObject)json, (String)"icon") : null;
            int areaToClear = GsonHelper.getAsInt((JsonObject)json, (String)"area_to_clear", (int)0);
            int atcLengthBefore = GsonHelper.getAsInt((JsonObject)json, (String)"area_to_clear_length_before", (int)-1);
            int atcLengthAfter = GsonHelper.getAsInt((JsonObject)json, (String)"area_to_clear_length_after", (int)-1);
            int atcWidthBefore = GsonHelper.getAsInt((JsonObject)json, (String)"area_to_clear_width_before", (int)-1);
            int atcWidthAfter = GsonHelper.getAsInt((JsonObject)json, (String)"area_to_clear_width_after", (int)-1);
            ClearMargins clearMargins = ClearMargins.fromLegacy(areaToClear, atcLengthBefore, atcLengthAfter, atcWidthBefore, atcWidthAfter);
            int price = GsonHelper.getAsInt((JsonObject)json, (String)"price", (int)0);
            int reputation = GsonHelper.getAsInt((JsonObject)json, (String)"reputation", (int)0);
            boolean isGift = GsonHelper.getAsBoolean((JsonObject)json, (String)"is_gift", (boolean)false);
            EnumMap<DyeColor, List<BrickColourTheme.WeightedColor>> randomBrickColours = new EnumMap<DyeColor, List<BrickColourTheme.WeightedColor>>(DyeColor.class);
            if (json.has("random_brick_colours")) {
                JsonObject rbcObj = GsonHelper.getAsJsonObject((JsonObject)json, (String)"random_brick_colours");
                for (Map.Entry entry : rbcObj.entrySet()) {
                    DyeColor inputColor = CultureLoader.parseDyeColor((String)entry.getKey());
                    if (inputColor == null) continue;
                    randomBrickColours.put(inputColor, CultureLoader.parseWeightedColorList(((JsonElement)entry.getValue()).getAsJsonArray()));
                }
            }
            ArrayList<BuildingPlanSet.StartingGood> startingGoods = new ArrayList<BuildingPlanSet.StartingGood>();
            if (json.has("starting_goods")) {
                for (JsonElement sgEl : GsonHelper.getAsJsonArray((JsonObject)json, (String)"starting_goods")) {
                    JsonObject sgObj = sgEl.getAsJsonObject();
                    String sgItem = GsonHelper.getAsString((JsonObject)sgObj, (String)"item");
                    double sgProb = sgObj.has("probability") ? sgObj.get("probability").getAsDouble() : 1.0;
                    int sgFixed = GsonHelper.getAsInt((JsonObject)sgObj, (String)"fixed", (int)0);
                    int sgRandom = GsonHelper.getAsInt((JsonObject)sgObj, (String)"random", (int)0);
                    startingGoods.add(new BuildingPlanSet.StartingGood(sgItem, sgProb, sgFixed, sgRandom));
                }
            }
            String travelBookCategory = json.has("travel_book_category") ? GsonHelper.getAsString((JsonObject)json, (String)"travel_book_category") : null;
            boolean travelBookDisplay = GsonHelper.getAsBoolean((JsonObject)json, (String)"travel_book_display", (boolean)true);
            boolean isSubBuilding = GsonHelper.getAsBoolean((JsonObject)json, (String)"is_sub_building", (boolean)false);
            Map<String, Integer> farFromTags = JsonLoaderUtils.parseCommaSeparatedPairs(json, "far_from_tags");
            Map<String, Integer> closeToTags = JsonLoaderUtils.parseCommaSeparatedPairs(json, "close_to_tags");
            Integer fixedOrientation = null;
            if (json.has("fixed_orientation")) {
                fixedOrientation = BuildingPlanSetLoader.parseCardinalDirection(GsonHelper.getAsString((JsonObject)json, (String)"fixed_orientation"));
            }
            boolean isWallSegment = GsonHelper.getAsBoolean((JsonObject)json, (String)"is_wall_segment", (boolean)false);
            boolean isBorderBuilding = GsonHelper.getAsBoolean((JsonObject)json, (String)"is_border_building", (boolean)false);
            int extraWallConstructionSlots = GsonHelper.getAsInt((JsonObject)json, (String)"extra_wall_construction_slots", (int)0);
            if (json.has("is_town_hall")) {
                isTownHall = GsonHelper.getAsBoolean((JsonObject)json, (String)"is_town_hall", (boolean)false);
            } else if ("townhalls".equals(category)) {
                isTownHall = !isSubBuilding;
                LOGGER.warn("[Millenaire] {} has category 'townhalls' but no is_town_hall field \u2014 assuming {}. Re-run the converter to fix.", (Object)contentId, (Object)isTownHall);
            } else if ("lone".equals(category) && !isSubBuilding) {
                isTownHall = true;
            } else if (!isSubBuilding && (tags.contains("playerth") || tags.contains("townhall") || tags.contains("marvelth"))) {
                isTownHall = true;
                LOGGER.warn("[Millenaire] {} inferred is_town_hall=true from legacy TH tag (one of playerth/townhall/marvelth). Add \"is_town_hall\": true to the JSON to silence this warning.", (Object)contentId);
            } else {
                isTownHall = false;
            }
            BuildingPlanSet planSet = new BuildingPlanSet(setId, culture, buildingId, category, nativeName, maxCount, minDistance, maxDistance, maleResidents, femaleResidents, priorityMoveIn, tags, terrainPolicy, constructionOrder, variants, startingSubBuildings, icon, clearMargins, price, reputation, randomBrickColours, startingGoods, travelBookCategory, travelBookDisplay, isSubBuilding, isTownHall, farFromTags, closeToTags, fixedOrientation, isWallSegment, isBorderBuilding, extraWallConstructionSlots, isGift, visitors);
            ArrayList<BuildingPlan> flushed = new ArrayList<BuildingPlan>(pendingPlans.size());
            try {
                for (BuildingPlan p : pendingPlans) {
                    ModCultures.registerBuildingPlan(p);
                    flushed.add(p);
                }
                ModCultures.registerBuildingPlanSet(planSet);
            }
            catch (RuntimeException flushFailure) {
                for (BuildingPlan p : flushed) {
                    ModCultures.unregisterBuildingPlan(p.id());
                }
                throw flushFailure;
            }
        }
        catch (CrossFolderConflictException e) {
            throw e;
        }
        catch (Exception e) {
            LOGGER.error("Error parsing building plan set {}: {}", new Object[]{contentId, e.getMessage(), e});
        }
    }

    static PathFields resolvePathFields(JsonObject levelObj, int prevPathLevel, int prevPathWidth) {
        int pathLevel = levelObj.has("path_level") ? GsonHelper.getAsInt((JsonObject)levelObj, (String)"path_level") : prevPathLevel;
        boolean rebuildPath = GsonHelper.getAsBoolean((JsonObject)levelObj, (String)"rebuild_path", (boolean)false);
        int pathWidth = levelObj.has("path_width") ? GsonHelper.getAsInt((JsonObject)levelObj, (String)"path_width") : prevPathWidth;
        return new PathFields(pathLevel, rebuildPath, pathWidth);
    }

    static int resolveVariantOrientation(JsonObject planSetJson, JsonObject variantJson) {
        if (variantJson.has("building_orientation")) {
            return GsonHelper.getAsInt((JsonObject)variantJson, (String)"building_orientation");
        }
        if (planSetJson.has("building_orientation")) {
            LOGGER.warn("Plan set '{}': plan-set-scope 'building_orientation' is deprecated \u2014 declare it inside each variant object instead", (Object)GsonHelper.getAsString((JsonObject)planSetJson, (String)"building_id", (String)"?"));
            return GsonHelper.getAsInt((JsonObject)planSetJson, (String)"building_orientation");
        }
        return 1;
    }

    static String filenameStem(String path) {
        String name;
        if (path == null || path.isEmpty()) {
            return null;
        }
        int slash = Math.max(path.lastIndexOf(47), path.lastIndexOf(92));
        String string = name = slash >= 0 ? path.substring(slash + 1) : path;
        if (!name.toLowerCase(Locale.ROOT).endsWith(".json")) {
            return name;
        }
        return name.substring(0, name.length() - ".json".length());
    }

    @Nullable
    static String extractCulturePrefix(String classpathPath) {
        if (classpathPath == null) {
            return null;
        }
        String marker = "cultures/";
        int idx = classpathPath.indexOf(marker);
        if (idx < 0) {
            return null;
        }
        String afterMarker = classpathPath.substring(idx + marker.length());
        int slash = afterMarker.indexOf(47);
        if (slash <= 0) {
            return null;
        }
        return afterMarker.substring(0, slash);
    }

    @Nullable
    static String relativiseToCulture(String classpathPath, String culture) {
        if (classpathPath == null || culture == null) {
            return null;
        }
        String marker = "cultures/" + culture + "/";
        int idx = classpathPath.indexOf(marker);
        if (idx < 0) {
            return null;
        }
        return classpathPath.substring(idx + marker.length());
    }

    static String nbtPathParentForCulturePath(String classpathPath, String culturePrefix) {
        if (classpathPath == null || culturePrefix == null) {
            return "";
        }
        String marker = "cultures/" + culturePrefix + "/";
        int idx = classpathPath.indexOf(marker);
        if (idx < 0) {
            return "";
        }
        String afterCulture = classpathPath.substring(idx + marker.length());
        int lastSlash = afterCulture.lastIndexOf(47);
        if (lastSlash < 0) {
            return "";
        }
        return afterCulture.substring(0, lastSlash);
    }

    static Integer parseCardinalDirection(String direction) {
        return switch (direction.toLowerCase(Locale.ROOT)) {
            case "north" -> 0;
            case "east" -> 1;
            case "south" -> 2;
            case "west" -> 3;
            default -> {
                LOGGER.warn("Unknown fixed_orientation '{}', defaulting to north (0)", (Object)direction);
                yield 0;
            }
        };
    }

    record PathFields(int pathLevel, boolean rebuildPath, int pathWidth) {
    }
}

