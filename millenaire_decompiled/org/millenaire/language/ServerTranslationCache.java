/*
 * Decompiled with CFR 0.150.
 * 
 * Could not load the following classes:
 *  com.google.gson.Gson
 *  com.google.gson.reflect.TypeToken
 *  com.mojang.logging.LogUtils
 *  org.slf4j.Logger
 */
package org.millenaire.language;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.mojang.logging.LogUtils;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.lang.reflect.Type;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import org.slf4j.Logger;

public final class ServerTranslationCache {
    private static final Logger LOGGER = LogUtils.getLogger();
    private static final String LANG_RESOURCE = "/assets/millenaire/lang/en_us.json";
    private static Map<String, String> entries = Collections.emptyMap();

    private ServerTranslationCache() {
    }

    public static void load() {
        try (InputStream is = ServerTranslationCache.class.getResourceAsStream(LANG_RESOURCE);){
            if (is == null) {
                LOGGER.warn("[Millenaire] Could not find lang resource: {}", (Object)LANG_RESOURCE);
                entries = Collections.emptyMap();
                return;
            }
            Type mapType = new TypeToken<Map<String, String>>(){}.getType();
            Map loaded = (Map)new Gson().fromJson((Reader)new InputStreamReader(is, StandardCharsets.UTF_8), mapType);
            entries = loaded != null ? loaded : Collections.emptyMap();
            LOGGER.info("[Millenaire] ServerTranslationCache loaded {} entries", (Object)entries.size());
        }
        catch (Exception e) {
            LOGGER.error("[Millenaire] Failed to load ServerTranslationCache", (Throwable)e);
            entries = Collections.emptyMap();
        }
    }

    public static String get(String key) {
        return entries.getOrDefault(key, key);
    }

    public static boolean has(String key) {
        return entries.containsKey(key);
    }

    static void setEntries(Map<String, String> testEntries) {
        entries = new HashMap<String, String>(testEntries);
    }

    public static void clear() {
        entries = Collections.emptyMap();
    }
}

