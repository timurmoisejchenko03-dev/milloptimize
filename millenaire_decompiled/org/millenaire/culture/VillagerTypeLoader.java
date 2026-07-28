/*
 * Decompiled with CFR 0.150.
 * 
 * Could not load the following classes:
 *  com.google.gson.JsonElement
 *  com.google.gson.JsonObject
 *  com.mojang.logging.LogUtils
 *  net.minecraft.resources.ResourceLocation
 *  net.minecraft.util.GsonHelper
 *  org.slf4j.Logger
 */
package org.millenaire.culture;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.mojang.logging.LogUtils;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.GsonHelper;
import org.millenaire.content.ContentFs;
import org.millenaire.culture.CrossFolderConflictException;
import org.millenaire.culture.CultureLoader;
import org.millenaire.culture.Gender;
import org.millenaire.culture.JsonLoaderUtils;
import org.millenaire.culture.ModCultures;
import org.millenaire.culture.VillagerType;
import org.millenaire.tool.ToolCategoryRegistry;
import org.slf4j.Logger;

final class VillagerTypeLoader {
    private static final Logger LOGGER = LogUtils.getLogger();
    private static final Map<ResourceLocation, String> SEEN_VILLAGER_IDS = new HashMap<ResourceLocation, String>();
    private static final Set<ResourceLocation> FAILED_CULTURES = new HashSet<ResourceLocation>();

    private VillagerTypeLoader() {
    }

    static void resetCrossFolderTracking() {
        SEEN_VILLAGER_IDS.clear();
        FAILED_CULTURES.clear();
    }

    static void loadFromContentFs(String contentId, ContentFs cultureFs, String relPath) {
        ResourceLocation id = ResourceLocation.fromNamespaceAndPath((String)"millenaire", (String)contentId);
        JsonObject json = CultureLoader.readJsonFromContentFs(cultureFs, relPath);
        if (json == null) {
            return;
        }
        VillagerTypeLoader.loadFromJson(id, relPath, json);
    }

    static void loadFromClasspath(String contentId, String classpathPath) {
        ResourceLocation id = ResourceLocation.fromNamespaceAndPath((String)"millenaire", (String)contentId);
        JsonObject json = CultureLoader.readJson(classpathPath);
        if (json == null) {
            return;
        }
        VillagerTypeLoader.loadFromJson(id, classpathPath, json);
    }

    private static void loadFromJson(ResourceLocation id, String pathKey, JsonObject json) {
        try {
            Map<ResourceLocation, Integer> initialInventory;
            ResourceLocation culture = ResourceLocation.parse((String)GsonHelper.getAsString((JsonObject)json, (String)"culture"));
            if (FAILED_CULTURES.contains((Object)culture)) {
                return;
            }
            String previousJsonPath = SEEN_VILLAGER_IDS.putIfAbsent(id, pathKey);
            if (previousJsonPath != null && !previousJsonPath.equals(pathKey)) {
                LOGGER.error("[Millenaire] Duplicate villager_type id '{}': loaded from '{}', later occurrence at '{}'. Cross-folder logical-id collisions are fatal \u2014 culture '{}' will not register this villager type. Move or rename one of the files so each id is unique within the culture.", new Object[]{id, previousJsonPath, pathKey, culture});
                FAILED_CULTURES.add(culture);
                throw new CrossFolderConflictException(culture, "Duplicate villager_type id '" + String.valueOf((Object)id) + "' (first at '" + previousJsonPath + "', second at '" + pathKey + "')");
            }
            String model = GsonHelper.getAsString((JsonObject)json, (String)"model", (String)"male");
            ArrayList<ResourceLocation> textures = new ArrayList<ResourceLocation>();
            if (json.has("textures")) {
                for (JsonElement e : GsonHelper.getAsJsonArray((JsonObject)json, (String)"textures")) {
                    textures.add(ResourceLocation.parse((String)e.getAsString()));
                }
            } else if (json.has("texture")) {
                textures.add(ResourceLocation.parse((String)GsonHelper.getAsString((JsonObject)json, (String)"texture")));
            }
            LinkedHashMap<String, VillagerType.ClothSet> clothes = new LinkedHashMap<String, VillagerType.ClothSet>();
            if (json.has("clothes")) {
                JsonObject clothesJson = GsonHelper.getAsJsonObject((JsonObject)json, (String)"clothes");
                for (Map.Entry entry : clothesJson.entrySet()) {
                    Iterator clothObj = ((JsonElement)entry.getValue()).getAsJsonObject();
                    List<ResourceLocation> layer0 = CultureLoader.parseResourceLocationList((JsonObject)clothObj, "layer0");
                    List<ResourceLocation> layer1 = CultureLoader.parseResourceLocationList((JsonObject)clothObj, "layer1");
                    clothes.put((String)entry.getKey(), new VillagerType.ClothSet(layer0, layer1));
                }
            }
            float baseScale = GsonHelper.getAsFloat((JsonObject)json, (String)"base_scale", (float)1.0f);
            boolean isChild = GsonHelper.getAsBoolean((JsonObject)json, (String)"is_child", (boolean)false);
            ArrayList<ResourceLocation> goals = new ArrayList<ResourceLocation>();
            for (JsonElement e : GsonHelper.getAsJsonArray((JsonObject)json, (String)"goals")) {
                goals.add(ResourceLocation.parse((String)e.getAsString()));
            }
            List<String> tags = JsonLoaderUtils.parseStringList(GsonHelper.getAsJsonArray((JsonObject)json, (String)"tags"));
            int spawnWeight = GsonHelper.getAsInt((JsonObject)json, (String)"spawn_weight");
            Map<ResourceLocation, Integer> map = initialInventory = json.has("initial_inventory") ? CultureLoader.parseResourceIntMap(GsonHelper.getAsJsonObject((JsonObject)json, (String)"initial_inventory")) : Map.of();
            Gender gender = json.has("gender") ? Gender.fromString(GsonHelper.getAsString((JsonObject)json, (String)"gender")) : (model.startsWith("female") ? Gender.FEMALE : Gender.MALE);
            String firstNameList = json.has("first_name_list") ? GsonHelper.getAsString((JsonObject)json, (String)"first_name_list") : null;
            String familyNameList = json.has("family_name_list") ? GsonHelper.getAsString((JsonObject)json, (String)"family_name_list") : null;
            String maleChild = json.has("male_child") ? GsonHelper.getAsString((JsonObject)json, (String)"male_child") : null;
            String femaleChild = json.has("female_child") ? GsonHelper.getAsString((JsonObject)json, (String)"female_child") : null;
            List<String> bringBackHomeGoods = json.has("bring_back_home_goods") ? JsonLoaderUtils.parseStringList(GsonHelper.getAsJsonArray((JsonObject)json, (String)"bring_back_home_goods")) : List.of();
            List<String> collectGoods = json.has("collect_goods") ? JsonLoaderUtils.parseStringList(GsonHelper.getAsJsonArray((JsonObject)json, (String)"collect_goods")) : List.of();
            HashMap<String, Integer> requiredGoods = new HashMap<String, Integer>();
            if (json.has("required_goods")) {
                JsonObject rgObj = GsonHelper.getAsJsonObject((JsonObject)json, (String)"required_goods");
                for (Object entry : rgObj.entrySet()) {
                    requiredGoods.put((String)entry.getKey(), ((JsonElement)entry.getValue()).getAsInt());
                }
            }
            String villagerIcon = json.has("icon") ? GsonHelper.getAsString((JsonObject)json, (String)"icon") : null;
            ArrayList<String> toolNeededClasses = new ArrayList<String>();
            if (json.has("tool_needed_classes")) {
                for (Object e : GsonHelper.getAsJsonArray((JsonObject)json, (String)"tool_needed_classes")) {
                    toolNeededClasses.addAll(ToolCategoryRegistry.expandToolClass(e.getAsString()));
                }
            }
            ArrayList<ResourceLocation> itemsNeeded = new ArrayList<ResourceLocation>();
            if (json.has("items_needed")) {
                for (JsonElement e : GsonHelper.getAsJsonArray((JsonObject)json, (String)"items_needed")) {
                    itemsNeeded.add(ResourceLocation.parse((String)e.getAsString()));
                }
            }
            float maxHealth = json.has("max_health") ? GsonHelper.getAsFloat((JsonObject)json, (String)"max_health") : 20.0f;
            String villagerConfigKey = json.has("villager_config") ? GsonHelper.getAsString((JsonObject)json, (String)"villager_config") : null;
            String travelBookCategory = json.has("travel_book_category") ? GsonHelper.getAsString((JsonObject)json, (String)"travel_book_category") : null;
            boolean travelBookDisplay = GsonHelper.getAsBoolean((JsonObject)json, (String)"travel_book_display", (boolean)true);
            String nativeName = GsonHelper.getAsString((JsonObject)json, (String)"native_name", (String)CultureLoader.formatSimpleName(id.getPath()));
            HashMap<ResourceLocation, Integer> foreignMerchantStock = new HashMap<ResourceLocation, Integer>();
            if (json.has("foreign_merchant_stock")) {
                JsonObject fmsObj = GsonHelper.getAsJsonObject((JsonObject)json, (String)"foreign_merchant_stock");
                for (Map.Entry entry : fmsObj.entrySet()) {
                    foreignMerchantStock.put(ResourceLocation.parse((String)((String)entry.getKey())), ((JsonElement)entry.getValue()).getAsInt());
                }
            }
            int hiringCost = 0;
            if (json.has("hiring_cost")) {
                JsonElement hcElem = json.get("hiring_cost");
                if (hcElem.isJsonPrimitive() && hcElem.getAsJsonPrimitive().isNumber()) {
                    hiringCost = hcElem.getAsInt();
                } else {
                    try {
                        hiringCost = Integer.parseInt(hcElem.getAsString());
                    }
                    catch (NumberFormatException entry) {
                        // empty catch block
                    }
                }
            }
            String travelBookHeldItem = json.has("travelbook_held_item") ? GsonHelper.getAsString((JsonObject)json, (String)"travelbook_held_item") : null;
            String travelBookHeldItemOffHand = json.has("travelbook_held_item_off_hand") ? GsonHelper.getAsString((JsonObject)json, (String)"travelbook_held_item_off_hand") : null;
            String altNativeName = json.has("alt_native_name") ? GsonHelper.getAsString((JsonObject)json, (String)"alt_native_name") : null;
            String altKey = json.has("alt_key") ? GsonHelper.getAsString((JsonObject)json, (String)"alt_key") : null;
            boolean travelBookMainCultureVillager = GsonHelper.getAsBoolean((JsonObject)json, (String)"travelbook_main_culture_villager", (boolean)false);
            ResourceLocation defaultWeapon = json.has("default_weapon") ? ResourceLocation.parse((String)GsonHelper.getAsString((JsonObject)json, (String)"default_weapon")) : null;
            int baseAttackStrength = GsonHelper.getAsInt((JsonObject)json, (String)"base_attack_strength", (int)0);
            ModCultures.registerVillagerType(new VillagerType(id, culture, model, textures, clothes, baseScale, isChild, goals, tags, spawnWeight, initialInventory, gender, firstNameList, familyNameList, maleChild, femaleChild, bringBackHomeGoods, collectGoods, requiredGoods, villagerIcon, toolNeededClasses, itemsNeeded, maxHealth, villagerConfigKey, travelBookCategory, travelBookDisplay, nativeName, foreignMerchantStock, hiringCost, travelBookHeldItem, travelBookHeldItemOffHand, altNativeName, altKey, travelBookMainCultureVillager, defaultWeapon, baseAttackStrength, Set.of(), Set.of(), Map.of()));
        }
        catch (CrossFolderConflictException e) {
            throw e;
        }
        catch (Exception e) {
            LOGGER.error("Error parsing villager type {}: {}", new Object[]{id, e.getMessage(), e});
        }
    }
}

