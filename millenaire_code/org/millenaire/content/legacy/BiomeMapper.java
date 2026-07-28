/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.google.gson.JsonElement
 *  com.google.gson.JsonObject
 *  com.google.gson.JsonParser
 */
package org.millenaire.content.legacy;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.BiConsumer;

public final class BiomeMapper {
    public static final String BUILTIN_RESOURCE = "/millenaire/legacy/legacy_biome_map.json";
    private final Map<String, String> builtinMap;
    private final Map<String, Map<String, String>> cultureOverrides;
    private static final Set<String> WARNED_UNKNOWNS = Collections.synchronizedSet(new HashSet());
    private BiConsumer<String, String> unmappedReportSink;

    private BiomeMapper(Map<String, String> builtinMap, Map<String, Map<String, String>> cultureOverrides) {
        this.builtinMap = builtinMap;
        this.cultureOverrides = cultureOverrides;
    }

    static void resetWarnedUnknownsForTesting() {
        WARNED_UNKNOWNS.clear();
    }

    public void installUnmappedReportSink(BiConsumer<String, String> sink) {
        this.unmappedReportSink = sink;
    }

    public static BiomeMapper loadBuiltin() {
        return new BiomeMapper(BiomeMapper.loadBuiltinMap(), Collections.emptyMap());
    }

    public static BiomeMapper loadAll(Path customRoot, Iterable<String> customCultures) {
        Map<String, String> builtin = BiomeMapper.loadBuiltinMap();
        LinkedHashMap<String, Map<String, String>> cultures = new LinkedHashMap<String, Map<String, String>>();
        if (customRoot != null && customCultures != null) {
            for (String culture : customCultures) {
                Map<String, String> cm;
                Path f = customRoot.resolve("cultures/" + culture + "/biome_map.json");
                if (!Files.isRegularFile(f, new LinkOption[0]) || (cm = BiomeMapper.parseOverrideFile(f)).isEmpty()) continue;
                cultures.put(culture, cm);
            }
        }
        return new BiomeMapper(builtin, cultures);
    }

    public static BiomeMapper of(Map<String, String> builtinMap, Map<String, Map<String, String>> cultureOverrides) {
        return new BiomeMapper(BiomeMapper.normalise(builtinMap), BiomeMapper.normaliseByCulture(cultureOverrides));
    }

    public static String normaliseKey(String raw) {
        if (raw == null) {
            return "";
        }
        String s = raw.trim().toLowerCase(Locale.ROOT);
        s = s.replaceAll("\\s+", " ");
        return s;
    }

    public Optional<String> resolveOne(String cultureContext, String legacyName) {
        String v;
        Map<String, String> cm;
        String key = BiomeMapper.normaliseKey(legacyName);
        if (key.isEmpty()) {
            return Optional.empty();
        }
        if (cultureContext != null && (cm = this.cultureOverrides.get(cultureContext)) != null && (v = cm.get(key)) != null) {
            return Optional.of(v);
        }
        String v2 = this.builtinMap.get(key);
        return Optional.ofNullable(v2);
    }

    public List<String> mapAll(String cultureContext, List<String> legacyBiomes) {
        if (legacyBiomes == null || legacyBiomes.isEmpty()) {
            return List.of();
        }
        LinkedHashSet<String> seen = new LinkedHashSet<String>();
        for (String raw : legacyBiomes) {
            Optional<String> mapped = this.resolveOne(cultureContext, raw);
            if (mapped.isPresent()) {
                seen.add(mapped.get());
                continue;
            }
            String warnKey = (cultureContext == null ? "" : cultureContext) + "::" + BiomeMapper.normaliseKey(raw);
            if (WARNED_UNKNOWNS.add(warnKey)) {
                System.out.println("  [WARN] Unknown legacy biome: '" + raw + "' (culture=" + cultureContext + "), dropping");
            }
            if (this.unmappedReportSink == null) continue;
            this.unmappedReportSink.accept(cultureContext, raw);
        }
        return new ArrayList<String>(seen);
    }

    /*
     * Enabled aggressive exception aggregation
     */
    private static Map<String, String> loadBuiltinMap() {
        try (InputStream in = BiomeMapper.class.getResourceAsStream(BUILTIN_RESOURCE);){
            Map<String, String> map;
            if (in == null) {
                throw new IllegalStateException("Built-in legacy biome map missing: /millenaire/legacy/legacy_biome_map.json");
            }
            try (InputStreamReader r = new InputStreamReader(in, StandardCharsets.UTF_8);){
                JsonObject obj = JsonParser.parseReader((Reader)r).getAsJsonObject();
                map = BiomeMapper.parseJsonObject(obj);
            }
            return map;
        }
        catch (IOException e) {
            throw new IllegalStateException("Failed to load /millenaire/legacy/legacy_biome_map.json", e);
        }
    }

    private static Map<String, String> parseOverrideFile(Path file) {
        try {
            String content = Files.readString(file, StandardCharsets.UTF_8);
            return BiomeMapper.parseJsonObject(JsonParser.parseString((String)content).getAsJsonObject());
        }
        catch (IOException e) {
            System.err.println("[WARN] Failed to read biome_map " + String.valueOf(file) + ": " + e.getMessage());
            return Collections.emptyMap();
        }
    }

    private static Map<String, String> parseJsonObject(JsonObject obj) {
        LinkedHashMap<String, String> out = new LinkedHashMap<String, String>();
        for (Map.Entry e : obj.entrySet()) {
            String k = (String)e.getKey();
            if (k.startsWith("_") || !((JsonElement)e.getValue()).isJsonPrimitive()) continue;
            out.put(BiomeMapper.normaliseKey(k), ((JsonElement)e.getValue()).getAsString());
        }
        return out;
    }

    private static Map<String, String> normalise(Map<String, String> in) {
        if (in == null || in.isEmpty()) {
            return Collections.emptyMap();
        }
        LinkedHashMap<String, String> out = new LinkedHashMap<String, String>();
        for (Map.Entry<String, String> e : in.entrySet()) {
            if (e.getKey() == null || e.getValue() == null) continue;
            out.put(BiomeMapper.normaliseKey(e.getKey()), e.getValue());
        }
        return out;
    }

    private static Map<String, Map<String, String>> normaliseByCulture(Map<String, Map<String, String>> in) {
        if (in == null || in.isEmpty()) {
            return Collections.emptyMap();
        }
        LinkedHashMap<String, Map<String, String>> out = new LinkedHashMap<String, Map<String, String>>();
        for (Map.Entry<String, Map<String, String>> e : in.entrySet()) {
            out.put(e.getKey(), BiomeMapper.normalise(e.getValue()));
        }
        return out;
    }

    Map<String, String> builtinSnapshot() {
        return Collections.unmodifiableMap(this.builtinMap);
    }
}

