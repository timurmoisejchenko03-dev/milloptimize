/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.google.gson.Gson
 *  com.google.gson.JsonArray
 *  com.google.gson.JsonElement
 *  com.google.gson.JsonObject
 *  javax.annotation.Nullable
 *  net.minecraft.util.GsonHelper
 */
package org.millenaire.culture;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.annotation.Nullable;
import net.minecraft.util.GsonHelper;

public final class JsonLoaderUtils {
    public static final Gson GSON = new Gson();

    private JsonLoaderUtils() {
    }

    public static List<String> parseStringList(JsonArray array) {
        ArrayList<String> list = new ArrayList<String>();
        for (JsonElement e : array) {
            list.add(e.getAsString());
        }
        return list;
    }

    public static List<String> parseStringList(JsonObject parent, String key) {
        if (!parent.has(key)) {
            return List.of();
        }
        return JsonLoaderUtils.parseStringList(parent.getAsJsonArray(key));
    }

    @Nullable
    public static List<String> parseStringListOrNull(JsonObject parent, String key) {
        if (!parent.has(key)) {
            return null;
        }
        return JsonLoaderUtils.parseStringList(parent.getAsJsonArray(key));
    }

    public static Map<String, Integer> parseStringIntMap(JsonObject parent, String key) {
        HashMap<String, Integer> map = new HashMap<String, Integer>();
        if (parent.has(key)) {
            JsonObject obj = GsonHelper.getAsJsonObject((JsonObject)parent, (String)key);
            for (Map.Entry entry : obj.entrySet()) {
                map.put((String)entry.getKey(), ((JsonElement)entry.getValue()).getAsInt());
            }
        }
        return map;
    }

    @Nullable
    public static Map<String, Integer> parseStringIntMapOrNull(JsonObject parent, String key) {
        if (!parent.has(key)) {
            return null;
        }
        HashMap<String, Integer> map = new HashMap<String, Integer>();
        JsonObject obj = GsonHelper.getAsJsonObject((JsonObject)parent, (String)key);
        for (String k : obj.keySet()) {
            map.put(k, obj.get(k).getAsInt());
        }
        return map;
    }

    public static Map<String, Integer> parseCommaSeparatedPairs(JsonObject parent, String key) {
        if (!parent.has(key)) {
            return Map.of();
        }
        HashMap<String, Integer> map = new HashMap<String, Integer>();
        for (JsonElement el : parent.getAsJsonArray(key)) {
            String s = el.getAsString();
            int comma = s.indexOf(44);
            if (comma <= 0 || comma >= s.length() - 1) continue;
            map.put(s.substring(0, comma).trim(), Integer.parseInt(s.substring(comma + 1).trim()));
        }
        return Map.copyOf(map);
    }
}

