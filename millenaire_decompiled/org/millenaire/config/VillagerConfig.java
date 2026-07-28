/*
 * Decompiled with CFR 0.150.
 * 
 * Could not load the following classes:
 *  com.google.gson.JsonElement
 *  com.google.gson.JsonObject
 *  com.google.gson.JsonParser
 *  com.mojang.logging.LogUtils
 *  javax.annotation.Nullable
 *  net.minecraft.util.GsonHelper
 *  net.minecraft.world.item.Item
 *  net.minecraft.world.item.Items
 *  org.slf4j.Logger
 */
package org.millenaire.config;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.mojang.logging.LogUtils;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import javax.annotation.Nullable;
import net.minecraft.util.GsonHelper;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
import org.millenaire.item.ItemHelper;
import org.slf4j.Logger;

public final class VillagerConfig {
    private static final Logger LOGGER = LogUtils.getLogger();
    private static final String CONFIG_BASE = "/millenaire/villager_config/";
    private static final List<String> OVERLAY_NAMES = List.of("muslimrules", "indiannobeef");
    private static VillagerConfig DEFAULT = new VillagerConfig(Map.of(), Map.of());
    private static boolean buildVillagePaths = true;
    private static Map<String, VillagerConfig> NAMED_CONFIGS = Map.of();
    private final Map<Item, Integer> foodGrowth;
    private final Map<Item, Integer> foodConception;

    private VillagerConfig(Map<Item, Integer> foodGrowth, Map<Item, Integer> foodConception) {
        this.foodGrowth = foodGrowth;
        this.foodConception = foodConception;
    }

    public static void load() {
        DEFAULT = VillagerConfig.loadFromJson("default");
        LOGGER.info("[Millenaire] Loaded default VillagerConfig: {} growth foods, {} conception foods", (Object)VillagerConfig.DEFAULT.foodGrowth.size(), (Object)VillagerConfig.DEFAULT.foodConception.size());
        HashMap<String, VillagerConfig> named = new HashMap<String, VillagerConfig>();
        for (String name : OVERLAY_NAMES) {
            VillagerConfig cfg = VillagerConfig.loadOverride(name);
            if (cfg == null) continue;
            named.put(name, cfg);
            LOGGER.info("[Millenaire] Loaded VillagerConfig overlay '{}': {} growth foods, {} conception foods", new Object[]{name, cfg.foodGrowth.size(), cfg.foodConception.size()});
        }
        NAMED_CONFIGS = Map.copyOf(named);
    }

    public static VillagerConfig get(@Nullable String key) {
        if (key == null || key.isEmpty()) {
            return DEFAULT;
        }
        return NAMED_CONFIGS.getOrDefault(key, DEFAULT);
    }

    public static VillagerConfig getDefault() {
        return DEFAULT;
    }

    public Map<Item, Integer> getFoodGrowth() {
        return this.foodGrowth;
    }

    public Map<Item, Integer> getFoodConception() {
        return this.foodConception;
    }

    public static Map<Item, Integer> getDefaultFoodGrowth() {
        return VillagerConfig.DEFAULT.foodGrowth;
    }

    public static Map<Item, Integer> getDefaultFoodConception() {
        return VillagerConfig.DEFAULT.foodConception;
    }

    /*
     * Enabled aggressive block sorting
     * Enabled unnecessary exception pruning
     * Enabled aggressive exception aggregation
     */
    private static VillagerConfig loadFromJson(String name) {
        String path = CONFIG_BASE + name + ".json";
        try (InputStream is = VillagerConfig.class.getResourceAsStream(path);){
            Map<Item, Integer> foodConception;
            JsonObject root;
            if (is == null) {
                LOGGER.warn("Villager config not found: {}", (Object)path);
                VillagerConfig villagerConfig2 = new VillagerConfig(Map.of(), Map.of());
                return villagerConfig2;
            }
            try (InputStreamReader reader = new InputStreamReader(is);){
                root = JsonParser.parseReader((Reader)reader).getAsJsonObject();
            }
            Map<Item, Integer> foodGrowth = root.has("food_growth") ? VillagerConfig.parseItemMap(root.getAsJsonObject("food_growth")) : Map.of();
            Map<Item, Integer> map = foodConception = root.has("food_conception") ? VillagerConfig.parseItemMap(root.getAsJsonObject("food_conception")) : Map.of();
            if (root.has("build_village_paths")) {
                buildVillagePaths = GsonHelper.getAsBoolean((JsonObject)root, (String)"build_village_paths", (boolean)true);
            }
            VillagerConfig villagerConfig = new VillagerConfig(foodGrowth, foodConception);
            return villagerConfig;
        }
        catch (Exception e) {
            LOGGER.error("Error loading villager config '{}'", (Object)name, (Object)e);
            return new VillagerConfig(Map.of(), Map.of());
        }
    }

    /*
     * Enabled aggressive block sorting
     * Enabled unnecessary exception pruning
     * Enabled aggressive exception aggregation
     */
    @Nullable
    private static VillagerConfig loadOverride(String name) {
        String path = CONFIG_BASE + name + ".json";
        try (InputStream is = VillagerConfig.class.getResourceAsStream(path);){
            JsonObject root;
            if (is == null) {
                LOGGER.debug("Overlay config not found (skipping): {}", (Object)path);
                VillagerConfig villagerConfig2 = null;
                return villagerConfig2;
            }
            try (InputStreamReader reader = new InputStreamReader(is);){
                root = JsonParser.parseReader((Reader)reader).getAsJsonObject();
            }
            LinkedHashMap<Item, Integer> growthMap = new LinkedHashMap<Item, Integer>(VillagerConfig.DEFAULT.foodGrowth);
            LinkedHashMap<Item, Integer> conceptionMap = new LinkedHashMap<Item, Integer>(VillagerConfig.DEFAULT.foodConception);
            if (root.has("food_growth_overrides")) {
                VillagerConfig.applyOverrides(growthMap, root.getAsJsonObject("food_growth_overrides"));
            }
            if (root.has("food_conception_overrides")) {
                VillagerConfig.applyOverrides(conceptionMap, root.getAsJsonObject("food_conception_overrides"));
            }
            Map<Item, Integer> sortedGrowth = VillagerConfig.sortByDescendingValue(growthMap);
            Map<Item, Integer> sortedConception = VillagerConfig.sortByDescendingValue(conceptionMap);
            VillagerConfig villagerConfig = new VillagerConfig(sortedGrowth, sortedConception);
            return villagerConfig;
        }
        catch (Exception e) {
            LOGGER.error("Error loading overlay config '{}'", (Object)name, (Object)e);
            return null;
        }
    }

    private static void applyOverrides(Map<Item, Integer> target, JsonObject overrides) {
        for (Map.Entry e : overrides.entrySet()) {
            Item item;
            String key = (String)e.getKey();
            if (key.startsWith("_") || (item = ItemHelper.resolve(key)) == null || item == Items.AIR) continue;
            int value = ((JsonElement)e.getValue()).getAsInt();
            if (value <= 0) {
                target.remove((Object)item);
                continue;
            }
            target.put(item, value);
        }
    }

    private static Map<Item, Integer> sortByDescendingValue(Map<Item, Integer> map) {
        record Entry(Item item, int value) {
        }
        ArrayList<Entry> entries = new ArrayList<Entry>();
        for (Map.Entry<Item, Integer> e : map.entrySet()) {
            entries.add(new Entry(e.getKey(), e.getValue()));
        }
        entries.sort((a, b) -> Integer.compare(b.value, a.value));
        LinkedHashMap<Item, Integer> result = new LinkedHashMap<Item, Integer>();
        for (Entry entry : entries) {
            result.put(entry.item, entry.value);
        }
        return result;
    }

    private static Map<Item, Integer> parseItemMap(JsonObject obj) {
        record Entry(Item item, int value) {
        }
        ArrayList<Entry> entries = new ArrayList<Entry>();
        for (Map.Entry e : obj.entrySet()) {
            Item item;
            String key = (String)e.getKey();
            if (key.startsWith("_") || (item = ItemHelper.resolve(key)) == null || item == Items.AIR) continue;
            int value = ((JsonElement)e.getValue()).getAsInt();
            entries.add(new Entry(item, value));
        }
        entries.sort((a, b) -> Integer.compare(b.value, a.value));
        LinkedHashMap<Item, Integer> result = new LinkedHashMap<Item, Integer>();
        for (Entry entry : entries) {
            result.put(entry.item, entry.value);
        }
        return result;
    }

    public static boolean isBuildVillagePaths() {
        return buildVillagePaths;
    }
}

