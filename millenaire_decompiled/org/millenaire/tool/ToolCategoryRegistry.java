/*
 * Decompiled with CFR 0.150.
 * 
 * Could not load the following classes:
 *  com.google.gson.Gson
 *  com.google.gson.JsonArray
 *  com.google.gson.JsonElement
 *  com.google.gson.JsonObject
 *  com.mojang.logging.LogUtils
 *  javax.annotation.Nullable
 *  net.minecraft.util.GsonHelper
 *  net.minecraft.world.item.Item
 *  org.slf4j.Logger
 */
package org.millenaire.tool;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.mojang.logging.LogUtils;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;
import javax.annotation.Nullable;
import net.minecraft.util.GsonHelper;
import net.minecraft.world.item.Item;
import org.millenaire.culture.JsonLoaderUtils;
import org.millenaire.item.ItemHelper;
import org.millenaire.tool.ToolCategory;
import org.slf4j.Logger;

public final class ToolCategoryRegistry {
    private static final Logger LOGGER = LogUtils.getLogger();
    private static final Gson GSON = JsonLoaderUtils.GSON;
    private static final String CONFIG_PATH = "/millenaire/tool_categories.json";
    private static volatile Map<String, ToolCategory> CATEGORIES = Map.of();
    private static final AtomicBoolean warnedMeleeWeapons = new AtomicBoolean(false);

    private ToolCategoryRegistry() {
    }

    public static void load() {
        HashMap<String, ToolCategory> newCategories = new HashMap<String, ToolCategory>();
        try (InputStream is = ToolCategoryRegistry.class.getResourceAsStream(CONFIG_PATH);){
            JsonObject root;
            if (is == null) {
                LOGGER.error("File tool_categories.json not found in classpath");
                return;
            }
            try (InputStreamReader reader = new InputStreamReader(is, StandardCharsets.UTF_8);){
                root = (JsonObject)GSON.fromJson((Reader)reader, JsonObject.class);
            }
            for (Map.Entry entry : root.entrySet()) {
                String categoryId = (String)entry.getKey();
                JsonArray itemsArray = ((JsonElement)entry.getValue()).getAsJsonArray();
                ArrayList<ToolCategory.ToolEntry> items = new ArrayList<ToolCategory.ToolEntry>();
                for (JsonElement elem : itemsArray) {
                    JsonObject itemObj = elem.getAsJsonObject();
                    String itemId = GsonHelper.getAsString((JsonObject)itemObj, (String)"item");
                    int priority = GsonHelper.getAsInt((JsonObject)itemObj, (String)"priority");
                    Item resolved = ItemHelper.resolve(itemId);
                    items.add(new ToolCategory.ToolEntry(itemId, resolved, priority));
                    if (resolved != null) continue;
                    LOGGER.debug("Item {} not resolved for category {} \u2014 will be ignored at runtime", (Object)itemId, (Object)categoryId);
                }
                items.sort(Comparator.comparingInt(ToolCategory.ToolEntry::priority).reversed());
                newCategories.put(categoryId, new ToolCategory(categoryId, List.copyOf(items)));
            }
            CATEGORIES = Map.copyOf(newCategories);
            LOGGER.info("{} tool categories loaded ({} items total)", (Object)CATEGORIES.size(), (Object)CATEGORIES.values().stream().mapToInt(c -> c.items().size()).sum());
        }
        catch (Exception e) {
            LOGGER.error("Error loading tool_categories.json", (Throwable)e);
        }
    }

    @Nullable
    public static ToolCategory get(String categoryId) {
        return CATEGORIES.get(categoryId);
    }

    public static List<String> expandToolClass(String toolClass) {
        return switch (toolClass.toLowerCase(Locale.ROOT)) {
            case "armour" -> List.of("armourshelmet", "armourschestplate", "armoursleggings", "armoursboots");
            case "meleeweapons" -> {
                if (warnedMeleeWeapons.compareAndSet(false, true)) {
                    LOGGER.warn("Deprecated usage of 'meleeweapons' \u2014 use 'toolssword' (further occurrences silenced)");
                }
                yield List.of("weaponshandtohand");
            }
            case "toolssword" -> List.of("toolssword");
            case "rangedweapons" -> List.of("weaponsranged");
            case "pickaxes" -> List.of("toolspickaxe");
            case "axes" -> List.of("toolsaxe");
            case "shovels" -> List.of("toolsshovel");
            case "hoes" -> List.of("toolshoe");
            default -> {
                LOGGER.warn("Unknown tool class: {}", (Object)toolClass);
                yield List.of();
            }
        };
    }
}

