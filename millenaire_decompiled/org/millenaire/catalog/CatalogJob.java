/*
 * Decompiled with CFR 0.150.
 * 
 * Could not load the following classes:
 *  com.google.gson.JsonArray
 *  com.google.gson.JsonObject
 *  com.google.gson.JsonParser
 *  net.minecraft.resources.ResourceLocation
 */
package org.millenaire.catalog;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.resources.ResourceLocation;
import org.millenaire.catalog.CatalogTarget;

public record CatalogJob(List<String> cultures, CatalogTarget target, int width, int height, int limit, List<String> buildings, List<String> villagers, long seed, String mode) {
    private static final int DEFAULT_WIDTH = 1920;
    private static final int DEFAULT_HEIGHT = 1080;
    public static final long DEFAULT_SEED = 80085L;
    public static final String MODE_SCOUT = "scout";

    public static CatalogJob fromJson(String json) {
        JsonArray arr;
        JsonObject obj = JsonParser.parseString((String)json).getAsJsonObject();
        ArrayList cultures = new ArrayList();
        if (obj.has("cultures") && obj.get("cultures").isJsonArray()) {
            obj.getAsJsonArray("cultures").forEach(e -> cultures.add(e.getAsString()));
        }
        CatalogTarget target = obj.has("targets") ? CatalogTarget.fromString(obj.get("targets").getAsString()) : CatalogTarget.ALL;
        int width = 1920;
        int height = 1080;
        if (obj.has("resolution") && obj.get("resolution").isJsonArray() && (arr = obj.getAsJsonArray("resolution")).size() == 2) {
            width = arr.get(0).getAsInt();
            height = arr.get(1).getAsInt();
        }
        int limit = obj.has("limit") ? obj.get("limit").getAsInt() : 0;
        ArrayList buildings = new ArrayList();
        if (obj.has("buildings") && obj.get("buildings").isJsonArray()) {
            obj.getAsJsonArray("buildings").forEach(e -> buildings.add(e.getAsString()));
        }
        ArrayList villagers = new ArrayList();
        if (obj.has("villagers") && obj.get("villagers").isJsonArray()) {
            obj.getAsJsonArray("villagers").forEach(e -> villagers.add(e.getAsString()));
        }
        long seed = obj.has("seed") ? obj.get("seed").getAsLong() : 80085L;
        String mode = obj.has("mode") ? obj.get("mode").getAsString() : "capture";
        return new CatalogJob(List.copyOf(cultures), target, width, height, limit, List.copyOf(buildings), List.copyOf(villagers), seed, mode);
    }

    public boolean isScout() {
        return MODE_SCOUT.equalsIgnoreCase(this.mode);
    }

    public boolean includesCulture(String culturePath) {
        if (this.cultures.isEmpty()) {
            return true;
        }
        for (String c : this.cultures) {
            if (!c.equalsIgnoreCase("all") && !c.equalsIgnoreCase(culturePath)) continue;
            return true;
        }
        return false;
    }

    public boolean includesBuilding(ResourceLocation planSet) {
        if (this.buildings.isEmpty()) {
            return true;
        }
        String path = planSet.getPath();
        String name = path.substring(path.lastIndexOf(47) + 1);
        for (String b : this.buildings) {
            if (!b.equalsIgnoreCase(name) && !b.equalsIgnoreCase(path)) continue;
            return true;
        }
        return false;
    }

    public boolean includesVillager(ResourceLocation villagerType) {
        if (this.villagers.isEmpty()) {
            return true;
        }
        String path = villagerType.getPath();
        String name = path.substring(path.lastIndexOf(47) + 1);
        for (String v : this.villagers) {
            if (!v.equalsIgnoreCase(name) && !v.equalsIgnoreCase(path)) continue;
            return true;
        }
        return false;
    }
}

