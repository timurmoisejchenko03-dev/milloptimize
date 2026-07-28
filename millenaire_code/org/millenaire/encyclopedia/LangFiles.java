/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.google.gson.Gson
 *  com.google.gson.GsonBuilder
 *  com.google.gson.reflect.TypeToken
 */
package org.millenaire.encyclopedia;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.lang.reflect.Type;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.UnaryOperator;
import org.millenaire.DisplayUtils;

public final class LangFiles {
    private static final Gson GSON = new GsonBuilder().create();
    private static final Type MAP_TYPE = new TypeToken<Map<String, String>>(){}.getType();
    private static final String MOD_LANG_PREFIX = "/assets/millenaire/lang/";
    private static final String VANILLA_PREFIX = "/millenaire/encyclopedia/vanilla_items_";
    private static final List<String> LOCALES = List.of("ar_sa", "cs_cz", "da_dk", "de_de", "en_us", "es_es", "et_ee", "fr_fr", "hi_in", "hu_hu", "it_it", "ja_jp", "ko_kr", "nb_no", "nl_nl", "pl_pl", "pt_br", "pt_pt", "ru_ru", "sl_si", "sv_se", "th_th", "tr_tr", "uk_ua", "zh_cn", "zh_tw");
    private static final Map<String, Map<String, String>> MOD_LANG_CACHE = new ConcurrentHashMap<String, Map<String, String>>();
    private static final Map<String, Map<String, String>> VANILLA_CACHE = new ConcurrentHashMap<String, Map<String, String>>();

    private LangFiles() {
    }

    public static List<String> locales() {
        return LOCALES;
    }

    public static UnaryOperator<String> resolver(String locale) {
        Map mod = MOD_LANG_CACHE.computeIfAbsent(locale, LangFiles::parseModLang);
        if (locale.equals("en_us")) {
            return key -> {
                String v = (String)mod.get(key);
                return v != null ? v : DisplayUtils.t(key);
            };
        }
        Map vanilla = VANILLA_CACHE.computeIfAbsent(locale, LangFiles::loadVanilla);
        return key -> {
            String v = (String)mod.get(key);
            if (v != null) {
                return v;
            }
            v = (String)vanilla.get(key);
            if (v != null) {
                return v;
            }
            return DisplayUtils.t(key);
        };
    }

    public static Map<String, String> modKeys(String locale) {
        return MOD_LANG_CACHE.computeIfAbsent(locale, LangFiles::parseModLang);
    }

    /*
     * Enabled aggressive exception aggregation
     */
    private static Map<String, String> parseModLang(String locale) {
        String path = MOD_LANG_PREFIX + locale + ".json";
        try (InputStream in = LangFiles.class.getResourceAsStream(path);){
            Map<String, String> map;
            if (in == null) {
                Map<String, String> map2 = Map.of();
                return map2;
            }
            try (InputStreamReader reader = new InputStreamReader(in, StandardCharsets.UTF_8);){
                Map map3 = (Map)GSON.fromJson((Reader)reader, MAP_TYPE);
                map = map3 != null ? Collections.unmodifiableMap(map3) : Map.of();
            }
            return map;
        }
        catch (Exception e) {
            throw new RuntimeException("Failed to load mod lang file " + path, e);
        }
    }

    /*
     * Enabled aggressive exception aggregation
     */
    private static Map<String, String> loadVanilla(String locale) {
        String path = VANILLA_PREFIX + locale + ".json";
        try (InputStream in = LangFiles.class.getResourceAsStream(path);){
            Map<String, String> map;
            if (in == null) {
                Map<String, String> map2 = Map.of();
                return map2;
            }
            try (InputStreamReader reader = new InputStreamReader(in, StandardCharsets.UTF_8);){
                Map map3 = (Map)GSON.fromJson((Reader)reader, MAP_TYPE);
                map = map3 != null ? Collections.unmodifiableMap(map3) : Map.of();
            }
            return map;
        }
        catch (Exception e) {
            throw new RuntimeException("Failed to load vanilla bundle " + path, e);
        }
    }
}

