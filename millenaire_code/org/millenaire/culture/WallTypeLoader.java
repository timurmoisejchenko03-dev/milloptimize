/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.google.gson.JsonObject
 *  com.mojang.logging.LogUtils
 *  javax.annotation.Nullable
 *  net.minecraft.resources.ResourceLocation
 *  net.minecraft.util.GsonHelper
 *  org.slf4j.Logger
 */
package org.millenaire.culture;

import com.google.gson.JsonObject;
import com.mojang.logging.LogUtils;
import java.util.List;
import javax.annotation.Nullable;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.GsonHelper;
import org.millenaire.content.ContentFs;
import org.millenaire.content.CustomContentIndex;
import org.millenaire.content.Resource;
import org.millenaire.culture.CultureLoader;
import org.millenaire.culture.ModCultures;
import org.millenaire.culture.WallType;
import org.slf4j.Logger;

final class WallTypeLoader {
    private static final Logger LOGGER = LogUtils.getLogger();
    private static final String WALL_TYPE_DIR = "/millenaire/wall_type/";
    private static final String WALL_TYPE_CONTENT_DIR = "wall_type";
    private static final String JSON_EXT = ".json";

    private WallTypeLoader() {
    }

    static void loadAll() {
        ContentFs fs = CustomContentIndex.current().forGlobalContent(WALL_TYPE_CONTENT_DIR);
        List<Resource> entries = fs.walk("", 1).toList();
        String scopePrefix = "wall_type/";
        int count = 0;
        for (Resource res : entries) {
            String rel;
            String leaf;
            String full = res.relPath();
            if (!full.endsWith(JSON_EXT) || (leaf = WallTypeLoader.leafName(full)) == null || leaf.startsWith("_") || !full.startsWith(scopePrefix) || (rel = full.substring(scopePrefix.length())).indexOf(47) < 0) continue;
            String contentId = rel.substring(0, rel.length() - JSON_EXT.length());
            WallTypeLoader.load(contentId);
            ++count;
        }
        LOGGER.info("{} wall types loaded", (Object)count);
    }

    @Nullable
    private static String leafName(String relPath) {
        if (relPath == null || relPath.isEmpty()) {
            return null;
        }
        int slash = relPath.lastIndexOf(47);
        return slash < 0 ? relPath : relPath.substring(slash + 1);
    }

    static void load(String contentId) {
        ResourceLocation id = ResourceLocation.fromNamespaceAndPath((String)"millenaire", (String)contentId);
        String path = WALL_TYPE_DIR + contentId + JSON_EXT;
        JsonObject json = CultureLoader.readJson(path);
        if (json == null) {
            return;
        }
        try {
            ResourceLocation culture = ResourceLocation.parse((String)GsonHelper.getAsString((JsonObject)json, (String)"culture"));
            String key = GsonHelper.getAsString((JsonObject)json, (String)"key", (String)contentId);
            WallType wallType = new WallType(id, culture, key, WallTypeLoader.optionalRL(json, "wall_plan_set"), WallTypeLoader.optionalRL(json, "tower_plan_set"), ResourceLocation.parse((String)GsonHelper.getAsString((JsonObject)json, (String)"gateway_plan_set")), WallTypeLoader.optionalRL(json, "corner_plan_set"), WallTypeLoader.optionalRL(json, "cap_right_plan_set"), WallTypeLoader.optionalRL(json, "cap_left_plan_set"), WallTypeLoader.optionalRL(json, "cap_both_plan_set"), WallTypeLoader.optionalRL(json, "slope1_left_plan_set"), WallTypeLoader.optionalRL(json, "slope1_right_plan_set"), WallTypeLoader.optionalRL(json, "slope2_left_plan_set"), WallTypeLoader.optionalRL(json, "slope2_right_plan_set"), WallTypeLoader.optionalRL(json, "slope3_left_plan_set"), WallTypeLoader.optionalRL(json, "slope3_right_plan_set"), GsonHelper.getAsBoolean((JsonObject)json, (String)"wall_spawn", (boolean)true), GsonHelper.getAsBoolean((JsonObject)json, (String)"tower_spawn", (boolean)true), GsonHelper.getAsBoolean((JsonObject)json, (String)"gateway_spawn", (boolean)true), GsonHelper.getAsBoolean((JsonObject)json, (String)"corner_spawn", (boolean)true), GsonHelper.getAsBoolean((JsonObject)json, (String)"cap_spawn", (boolean)true), GsonHelper.getAsInt((JsonObject)json, (String)"walls_between_towers", (int)3), GsonHelper.getAsInt((JsonObject)json, (String)"nb_smooth_runs", (int)3), GsonHelper.getAsInt((JsonObject)json, (String)"max_y_delta", (int)15));
            ModCultures.registerWallType(wallType);
            LOGGER.debug("Loaded wall type: {}", (Object)id);
        }
        catch (Exception e) {
            LOGGER.error("Error parsing wall type {}: {}", new Object[]{contentId, e.getMessage(), e});
        }
    }

    @Nullable
    private static ResourceLocation optionalRL(JsonObject json, String field) {
        return json.has(field) ? ResourceLocation.parse((String)GsonHelper.getAsString((JsonObject)json, (String)field)) : null;
    }

    static ResourceLocation resolveWallTypeRL(String wallTypeName, ResourceLocation culture) {
        if (wallTypeName.contains(":")) {
            return ResourceLocation.parse((String)wallTypeName);
        }
        return ResourceLocation.fromNamespaceAndPath((String)culture.getNamespace(), (String)(culture.getPath() + "/" + wallTypeName.toLowerCase()));
    }
}

