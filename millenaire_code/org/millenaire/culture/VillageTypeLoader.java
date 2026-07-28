/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.google.gson.JsonArray
 *  com.google.gson.JsonElement
 *  com.google.gson.JsonObject
 *  com.mojang.logging.LogUtils
 *  net.minecraft.core.BlockPos
 *  net.minecraft.core.registries.Registries
 *  net.minecraft.resources.ResourceKey
 *  net.minecraft.resources.ResourceLocation
 *  net.minecraft.tags.TagKey
 *  net.minecraft.util.GsonHelper
 *  net.minecraft.world.item.DyeColor
 *  net.minecraft.world.level.biome.Biome
 *  net.minecraft.world.level.block.Rotation
 *  org.slf4j.Logger
 */
package org.millenaire.culture;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.mojang.logging.LogUtils;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.util.GsonHelper;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.block.Rotation;
import org.millenaire.config.MillenaireServerConfig;
import org.millenaire.content.ContentFs;
import org.millenaire.culture.BuildingPlanSetLoader;
import org.millenaire.culture.CrossFolderConflictException;
import org.millenaire.culture.CultureLoader;
import org.millenaire.culture.JsonLoaderUtils;
import org.millenaire.culture.ModCultures;
import org.millenaire.culture.TerrainQualifiers;
import org.millenaire.culture.VillageType;
import org.millenaire.culture.WallTypeLoader;
import org.millenaire.village.BrickColourTheme;
import org.millenaire.world.PlacementConstraints;
import org.slf4j.Logger;

final class VillageTypeLoader {
    private static final Logger LOGGER = LogUtils.getLogger();
    private static final Map<ResourceLocation, String> SEEN_VILLAGE_IDS = new HashMap<ResourceLocation, String>();
    private static final Set<ResourceLocation> FAILED_CULTURES = new HashSet<ResourceLocation>();

    private VillageTypeLoader() {
    }

    static void resetCrossFolderTracking() {
        SEEN_VILLAGE_IDS.clear();
        FAILED_CULTURES.clear();
    }

    static void loadFromContentFs(String contentId, ContentFs cultureFs, String relPath, String fallbackName) {
        ResourceLocation id = ResourceLocation.fromNamespaceAndPath((String)"millenaire", (String)contentId);
        JsonObject json = CultureLoader.readJsonFromContentFs(cultureFs, relPath);
        if (json == null) {
            return;
        }
        VillageTypeLoader.loadFromJson(id, relPath, json, fallbackName);
    }

    static void loadFromClasspath(String contentId, String classpathPath, String fallbackName) {
        ResourceLocation id = ResourceLocation.fromNamespaceAndPath((String)"millenaire", (String)contentId);
        JsonObject json = CultureLoader.readJson(classpathPath);
        if (json == null) {
            return;
        }
        VillageTypeLoader.loadFromJson(id, classpathPath, json, fallbackName);
    }

    private static void loadFromJson(ResourceLocation id, String pathKey, JsonObject json, String fallbackName) {
        try {
            String vtIcon;
            ResourceLocation culture = ResourceLocation.parse((String)GsonHelper.getAsString((JsonObject)json, (String)"culture"));
            if (FAILED_CULTURES.contains(culture)) {
                return;
            }
            String previousJsonPath = SEEN_VILLAGE_IDS.putIfAbsent(id, pathKey);
            if (previousJsonPath != null && !previousJsonPath.equals(pathKey)) {
                LOGGER.error("[Millenaire] Duplicate village_type id '{}': loaded from '{}', later occurrence at '{}'. Cross-folder logical-id collisions are fatal \u2014 culture '{}' will not register this village type.", new Object[]{id, previousJsonPath, pathKey, culture});
                FAILED_CULTURES.add(culture);
                throw new CrossFolderConflictException(culture, "Duplicate village_type id '" + String.valueOf(id) + "' (first at '" + previousJsonPath + "', second at '" + pathKey + "')");
            }
            String villageName = GsonHelper.getAsString((JsonObject)json, (String)"name", (String)fallbackName);
            int weight = GsonHelper.getAsInt((JsonObject)json, (String)"weight", (int)10);
            ArrayList<TagKey<Biome>> biomeTags = new ArrayList<TagKey<Biome>>();
            if (json.has("biome_tags")) {
                for (Iterator e : GsonHelper.getAsJsonArray((JsonObject)json, (String)"biome_tags")) {
                    String raw = e.getAsString();
                    if (raw.startsWith("#")) {
                        raw = raw.substring(1);
                    }
                    biomeTags.add(TagKey.create((ResourceKey)Registries.BIOME, (ResourceLocation)ResourceLocation.parse((String)raw)));
                }
            }
            ArrayList<VillageType.LayoutSlot> layout = new ArrayList<VillageType.LayoutSlot>();
            for (JsonElement e : GsonHelper.getAsJsonArray((JsonObject)json, (String)"layout")) {
                JsonObject slot = e.getAsJsonObject();
                ResourceLocation plan = ResourceLocation.parse((String)GsonHelper.getAsString((JsonObject)slot, (String)"plan"));
                String role = GsonHelper.getAsString((JsonObject)slot, (String)"role");
                BlockPos offset = null;
                Object rotation = null;
                if (slot.has("offset")) {
                    JsonArray offsetArr = GsonHelper.getAsJsonArray((JsonObject)slot, (String)"offset");
                    offset = new BlockPos(offsetArr.get(0).getAsInt(), offsetArr.get(1).getAsInt(), offsetArr.get(2).getAsInt());
                    rotation = Rotation.valueOf((String)GsonHelper.getAsString((JsonObject)slot, (String)"rotation"));
                }
                double minDistance = slot.has("min_distance") ? slot.get("min_distance").getAsDouble() : -1.0;
                double maxDistance = slot.has("max_distance") ? slot.get("max_distance").getAsDouble() : -1.0;
                int priority = GsonHelper.getAsInt((JsonObject)slot, (String)"priority", (int)-1);
                int clearMargin = GsonHelper.getAsInt((JsonObject)slot, (String)"clear_margin", (int)PlacementConstraints.getDefaultClearMargin());
                Map<String, Integer> farFromTags = JsonLoaderUtils.parseStringIntMap(slot, "far_from_tags");
                Map<String, Integer> closeToTags = JsonLoaderUtils.parseStringIntMap(slot, "close_to_tags");
                Integer fixedOrientation = null;
                if (slot.has("fixed_orientation")) {
                    fixedOrientation = BuildingPlanSetLoader.parseCardinalDirection(GsonHelper.getAsString((JsonObject)slot, (String)"fixed_orientation"));
                }
                layout.add(new VillageType.LayoutSlot(plan, offset, (Rotation)rotation, role, minDistance, maxDistance, priority, farFromTags, closeToTags, clearMargin, fixedOrientation));
            }
            Map<String, Integer> sellingPriceOverrides = JsonLoaderUtils.parseStringIntMap(json, "selling_price_overrides");
            Map<String, Integer> buyingPriceOverrides = JsonLoaderUtils.parseStringIntMap(json, "buying_price_overrides");
            int maxSimultaneousConstructions = json.has("max_simultaneous_constructions") ? GsonHelper.getAsInt((JsonObject)json, (String)"max_simultaneous_constructions") : 1;
            List<String> qualifiers = json.has("qualifiers") ? JsonLoaderUtils.parseStringList(GsonHelper.getAsJsonArray((JsonObject)json, (String)"qualifiers")) : List.of();
            TerrainQualifiers terrainQualifiers = new TerrainQualifiers(json.has("forest_qualifier") ? GsonHelper.getAsString((JsonObject)json, (String)"forest_qualifier") : null, json.has("hill_qualifier") ? GsonHelper.getAsString((JsonObject)json, (String)"hill_qualifier") : null, json.has("mountain_qualifier") ? GsonHelper.getAsString((JsonObject)json, (String)"mountain_qualifier") : null, json.has("desert_qualifier") ? GsonHelper.getAsString((JsonObject)json, (String)"desert_qualifier") : null, json.has("lava_qualifier") ? GsonHelper.getAsString((JsonObject)json, (String)"lava_qualifier") : null, json.has("lake_qualifier") ? GsonHelper.getAsString((JsonObject)json, (String)"lake_qualifier") : null, json.has("ocean_qualifier") ? GsonHelper.getAsString((JsonObject)json, (String)"ocean_qualifier") : null);
            ArrayList<ResourceLocation> playerBuildings = new ArrayList<ResourceLocation>();
            if (json.has("player_buildings")) {
                for (JsonElement e : GsonHelper.getAsJsonArray((JsonObject)json, (String)"player_buildings")) {
                    playerBuildings.add(ResourceLocation.parse((String)e.getAsString()));
                }
            }
            ArrayList<BrickColourTheme> brickColourThemes = new ArrayList<BrickColourTheme>();
            if (json.has("brick_colour_themes")) {
                for (Object e : GsonHelper.getAsJsonArray((JsonObject)json, (String)"brick_colour_themes")) {
                    brickColourThemes.add(VillageTypeLoader.parseBrickColourTheme(e.getAsJsonObject()));
                }
            }
            ArrayList<String> neverBuildings = new ArrayList<String>();
            if (json.has("never")) {
                for (JsonElement e : GsonHelper.getAsJsonArray((JsonObject)json, (String)"never")) {
                    neverBuildings.add(e.getAsString());
                }
            }
            boolean loneBuilding = GsonHelper.getAsBoolean((JsonObject)json, (String)"lone_building", (boolean)false);
            int minDistanceFromSpawn = GsonHelper.getAsInt((JsonObject)json, (String)"min_distance_from_spawn", (int)-1);
            int maxLB = GsonHelper.getAsInt((JsonObject)json, (String)"max", (int)-1);
            boolean keyLoneBuilding = GsonHelper.getAsBoolean((JsonObject)json, (String)"key_lone_building", (boolean)false);
            String keyLoneBuildingGenerateTag = json.has("key_lone_building_generate_tag") ? GsonHelper.getAsString((JsonObject)json, (String)"key_lone_building_generate_tag") : null;
            boolean generatedForPlayer = GsonHelper.getAsBoolean((JsonObject)json, (String)"generated_for_player", (boolean)false);
            boolean spawnableLB = GsonHelper.getAsBoolean((JsonObject)json, (String)"spawnable", (!loneBuilding ? 1 : 0) != 0);
            boolean showTownHallSigns = GsonHelper.getAsBoolean((JsonObject)json, (String)"show_town_hall_signs", (!loneBuilding ? 1 : 0) != 0);
            String nameListVal = json.has("name_list") ? GsonHelper.getAsString((JsonObject)json, (String)"name_list") : (json.has("namelist") ? GsonHelper.getAsString((JsonObject)json, (String)"namelist") : (loneBuilding ? null : "villages"));
            int radius = GsonHelper.getAsInt((JsonObject)json, (String)"radius", (int)90);
            int radiusOverride = MillenaireServerConfig.SERVER.villageRadiusOverride.getAsInt();
            if (radiusOverride > 0) {
                radius = radiusOverride;
            }
            float minimumBiomeValidity = GsonHelper.getAsFloat((JsonObject)json, (String)"minimum_biome_validity", (float)0.6f);
            List<String> pathMaterials = json.has("path_materials") ? JsonLoaderUtils.parseStringList(GsonHelper.getAsJsonArray((JsonObject)json, (String)"path_materials")) : List.of("pathgravel");
            boolean travelBookDisplay = GsonHelper.getAsBoolean((JsonObject)json, (String)"travel_book_display", (boolean)true);
            ArrayList<ResourceLocation> hamlets = new ArrayList<ResourceLocation>();
            if (json.has("hamlets")) {
                for (JsonElement e : GsonHelper.getAsJsonArray((JsonObject)json, (String)"hamlets")) {
                    hamlets.add(ResourceLocation.parse((String)e.getAsString()));
                }
            }
            String specialType = json.has("special_type") ? GsonHelper.getAsString((JsonObject)json, (String)"special_type") : null;
            String string = vtIcon = json.has("icon") ? GsonHelper.getAsString((JsonObject)json, (String)"icon") : null;
            boolean allowExtraBuildings = json.has("allow_extra_buildings") ? GsonHelper.getAsBoolean((JsonObject)json, (String)"allow_extra_buildings") : !loneBuilding && specialType == null;
            boolean playerControlled = GsonHelper.getAsBoolean((JsonObject)json, (String)"player_controlled", (boolean)false);
            if (playerControlled) {
                allowExtraBuildings = false;
            }
            ResourceLocation outerWallType = json.has("outer_wall_type") ? WallTypeLoader.resolveWallTypeRL(GsonHelper.getAsString((JsonObject)json, (String)"outer_wall_type"), culture) : null;
            ResourceLocation innerWallType = json.has("inner_wall_type") ? WallTypeLoader.resolveWallTypeRL(GsonHelper.getAsString((JsonObject)json, (String)"inner_wall_type"), culture) : null;
            int innerWallRadius = GsonHelper.getAsInt((JsonObject)json, (String)"inner_wall_radius", (int)0);
            int maxSimultaneousWallConstructions = GsonHelper.getAsInt((JsonObject)json, (String)"max_simultaneous_wall_constructions", (int)1);
            List<String> bannerJsons = json.has("banner_json") ? JsonLoaderUtils.parseStringList(GsonHelper.getAsJsonArray((JsonObject)json, (String)"banner_json")) : List.of();
            boolean carriesRaid = GsonHelper.getAsBoolean((JsonObject)json, (String)"carries_raid", (boolean)false);
            ModCultures.registerVillageType(new VillageType(id, culture, villageName, weight, biomeTags, layout, sellingPriceOverrides, buyingPriceOverrides, maxSimultaneousConstructions, qualifiers, terrainQualifiers, playerBuildings, brickColourThemes, neverBuildings, loneBuilding, minDistanceFromSpawn, maxLB, keyLoneBuilding, keyLoneBuildingGenerateTag, generatedForPlayer, spawnableLB, showTownHallSigns, nameListVal, radius, minimumBiomeValidity, pathMaterials, travelBookDisplay, hamlets, specialType, allowExtraBuildings, vtIcon, playerControlled, outerWallType, innerWallType, innerWallRadius, maxSimultaneousWallConstructions, bannerJsons, carriesRaid));
        }
        catch (CrossFolderConflictException e) {
            throw e;
        }
        catch (Exception e) {
            LOGGER.error("Error parsing village type {}: {}", new Object[]{id, e.getMessage(), e});
        }
    }

    static BrickColourTheme parseBrickColourTheme(JsonObject json) {
        String name = GsonHelper.getAsString((JsonObject)json, (String)"name");
        int weight = GsonHelper.getAsInt((JsonObject)json, (String)"weight");
        JsonObject coloursObj = GsonHelper.getAsJsonObject((JsonObject)json, (String)"colours");
        List<BrickColourTheme.WeightedColor> otherPool = null;
        if (coloursObj.has("other")) {
            otherPool = CultureLoader.parseWeightedColorList(GsonHelper.getAsJsonArray((JsonObject)coloursObj, (String)"other"));
        }
        EnumMap<DyeColor, List<BrickColourTheme.WeightedColor>> colorPools = new EnumMap<DyeColor, List<BrickColourTheme.WeightedColor>>(DyeColor.class);
        for (Map.Entry entry : coloursObj.entrySet()) {
            DyeColor inputColor;
            if (((String)entry.getKey()).equals("other") || (inputColor = CultureLoader.parseDyeColor((String)entry.getKey())) == null) continue;
            colorPools.put(inputColor, CultureLoader.parseWeightedColorList(((JsonElement)entry.getValue()).getAsJsonArray()));
        }
        if (otherPool != null) {
            for (DyeColor color : DyeColor.values()) {
                if (colorPools.containsKey(color)) continue;
                colorPools.put(color, otherPool);
            }
        }
        return new BrickColourTheme(name, weight, colorPools);
    }
}

