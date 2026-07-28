/*
 * Decompiled with CFR 0.150.
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
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.function.BiConsumer;
import org.millenaire.content.legacy.LegacyDataParser;

public final class ItemIdMapper {
    public static final String BUILTIN_RESOURCE = "/millenaire/legacy/legacy_item_map.json";
    static final Map<String, String> LEGACY_1_12_TO_1_21_MAP = Map.ofEntries(Map.entry("minecraft:hardened_clay", "minecraft:terracotta"), Map.entry("minecraft:hardened_clay;0", "minecraft:terracotta"), Map.entry("minecraft:stained_hardened_clay", "minecraft:terracotta"), Map.entry("minecraft:stained_hardened_clay;0", "minecraft:white_terracotta"), Map.entry("minecraft:stained_hardened_clay;1", "minecraft:orange_terracotta"), Map.entry("minecraft:stained_hardened_clay;2", "minecraft:magenta_terracotta"), Map.entry("minecraft:stained_hardened_clay;3", "minecraft:light_blue_terracotta"), Map.entry("minecraft:stained_hardened_clay;4", "minecraft:yellow_terracotta"), Map.entry("minecraft:stained_hardened_clay;5", "minecraft:lime_terracotta"), Map.entry("minecraft:stained_hardened_clay;6", "minecraft:pink_terracotta"), Map.entry("minecraft:stained_hardened_clay;7", "minecraft:gray_terracotta"), Map.entry("minecraft:stained_hardened_clay;8", "minecraft:light_gray_terracotta"), Map.entry("minecraft:stained_hardened_clay;9", "minecraft:cyan_terracotta"), Map.entry("minecraft:stained_hardened_clay;10", "minecraft:purple_terracotta"), Map.entry("minecraft:stained_hardened_clay;11", "minecraft:blue_terracotta"), Map.entry("minecraft:stained_hardened_clay;12", "minecraft:brown_terracotta"), Map.entry("minecraft:stained_hardened_clay;13", "minecraft:green_terracotta"), Map.entry("minecraft:stained_hardened_clay;14", "minecraft:red_terracotta"), Map.entry("minecraft:stained_hardened_clay;15", "minecraft:black_terracotta"), Map.entry("minecraft:dye", "minecraft:red_dye"), Map.entry("minecraft:dye;0", "minecraft:ink_sac"), Map.entry("minecraft:dye;1", "minecraft:red_dye"), Map.entry("minecraft:dye;2", "minecraft:green_dye"), Map.entry("minecraft:dye;3", "minecraft:cocoa_beans"), Map.entry("minecraft:dye;4", "minecraft:lapis_lazuli"), Map.entry("minecraft:dye;5", "minecraft:purple_dye"), Map.entry("minecraft:dye;6", "minecraft:cyan_dye"), Map.entry("minecraft:dye;7", "minecraft:light_gray_dye"), Map.entry("minecraft:dye;8", "minecraft:gray_dye"), Map.entry("minecraft:dye;9", "minecraft:pink_dye"), Map.entry("minecraft:dye;10", "minecraft:lime_dye"), Map.entry("minecraft:dye;11", "minecraft:yellow_dye"), Map.entry("minecraft:dye;12", "minecraft:light_blue_dye"), Map.entry("minecraft:dye;13", "minecraft:magenta_dye"), Map.entry("minecraft:dye;14", "minecraft:orange_dye"), Map.entry("minecraft:dye;15", "minecraft:bone_meal"), Map.entry("minecraft:tallgrass", "minecraft:short_grass"), Map.entry("minecraft:tallgrass;0", "minecraft:dead_bush"), Map.entry("minecraft:tallgrass;1", "minecraft:short_grass"), Map.entry("minecraft:tallgrass;2", "minecraft:fern"), Map.entry("minecraft:red_flower", "minecraft:poppy"), Map.entry("red_flower", "minecraft:poppy"), Map.entry("minecraft:yellow_flower", "minecraft:dandelion"), Map.entry("yellow_flower", "minecraft:dandelion"));
    private final Map<String, String> builtinMillenaire;
    private final Map<String, String> builtinVanilla;
    private final Map<String, String> globalOverrides;
    private final Map<String, Map<String, String>> cultureOverrides;
    private BiConsumer<String, String> unmappedReportSink;

    private ItemIdMapper(Map<String, String> builtinMillenaire, Map<String, String> builtinVanilla, Map<String, String> globalOverrides, Map<String, Map<String, String>> cultureOverrides) {
        this.builtinMillenaire = builtinMillenaire;
        this.builtinVanilla = builtinVanilla;
        this.globalOverrides = globalOverrides;
        this.cultureOverrides = cultureOverrides;
    }

    public void installUnmappedReportSink(BiConsumer<String, String> sink) {
        this.unmappedReportSink = sink;
    }

    private void reportMiss(String culture, String legacyName) {
        if (this.unmappedReportSink != null) {
            this.unmappedReportSink.accept(culture, legacyName);
        }
    }

    public static ItemIdMapper loadBuiltin() {
        Built b = ItemIdMapper.loadBuiltinMap();
        return new ItemIdMapper(b.millenaire, b.vanilla, Collections.emptyMap(), Collections.emptyMap());
    }

    public static ItemIdMapper loadAll(Path customRoot, Iterable<String> customCultures) {
        Built b = ItemIdMapper.loadBuiltinMap();
        LinkedHashMap<String, String> global = new LinkedHashMap<String, String>();
        LinkedHashMap<String, Map<String, String>> cultures = new LinkedHashMap<String, Map<String, String>>();
        if (customRoot != null) {
            Path globalFile = customRoot.resolve("itemlist.txt");
            if (Files.isRegularFile(globalFile, new LinkOption[0])) {
                ItemIdMapper.parseItemlist(globalFile, global);
            }
            if (customCultures != null) {
                for (String culture : customCultures) {
                    Path cultureFile = customRoot.resolve("cultures/" + culture + "/itemlist.txt");
                    if (!Files.isRegularFile(cultureFile, new LinkOption[0])) continue;
                    LinkedHashMap<String, String> cm = new LinkedHashMap<String, String>();
                    ItemIdMapper.parseItemlist(cultureFile, cm);
                    if (cm.isEmpty()) continue;
                    cultures.put(culture, cm);
                }
            }
        }
        ItemIdMapper.logCrossTierCollisions(global, cultures, b);
        return new ItemIdMapper(b.millenaire, b.vanilla, global, cultures);
    }

    private static void logCrossTierCollisions(Map<String, String> global, Map<String, Map<String, String>> cultures, Built builtin) {
        for (Map.Entry<String, String> entry : global.entrySet()) {
            String builtinHit = ItemIdMapper.lookupBuiltin(entry.getKey(), builtin);
            if (builtinHit == null || builtinHit.equals(entry.getValue())) continue;
            System.out.println("  [INFO] global itemlist overrides built-in '" + entry.getKey() + "': " + builtinHit + " \u2192 " + entry.getValue());
        }
        for (Map.Entry<String, Object> entry : cultures.entrySet()) {
            String culture = entry.getKey();
            for (Map.Entry e : ((Map)entry.getValue()).entrySet()) {
                String higher = global.get(e.getKey());
                if (higher == null) {
                    higher = ItemIdMapper.lookupBuiltin((String)e.getKey(), builtin);
                }
                if (higher == null || higher.equals(e.getValue())) continue;
                System.out.println("  [INFO] culture-local itemlist [" + culture + "] overrides inherited mapping for '" + (String)e.getKey() + "': " + higher + " \u2192 " + (String)e.getValue());
            }
        }
    }

    private static String lookupBuiltin(String lcKey, Built b) {
        String v = b.millenaire.get(lcKey);
        if (v != null) {
            return v;
        }
        return b.vanilla.get(lcKey);
    }

    public static ItemIdMapper of(Map<String, String> builtinMillenaire, Map<String, String> builtinVanilla, Map<String, String> globalOverrides, Map<String, Map<String, String>> cultureOverrides) {
        return new ItemIdMapper(ItemIdMapper.normalise(builtinMillenaire), ItemIdMapper.normalise(builtinVanilla), ItemIdMapper.normalise(globalOverrides), ItemIdMapper.normaliseByCulture(cultureOverrides));
    }

    public Optional<String> resolve(String cultureContext, String legacyName) {
        String v;
        Map<String, String> cm;
        if (legacyName == null) {
            return Optional.empty();
        }
        String key = legacyName.toLowerCase(Locale.ROOT);
        if (cultureContext != null && (cm = this.cultureOverrides.get(cultureContext)) != null && (v = cm.get(key)) != null) {
            return Optional.of(v);
        }
        String v2 = this.globalOverrides.get(key);
        if (v2 != null) {
            return Optional.of(v2);
        }
        v2 = this.builtinMillenaire.get(key);
        if (v2 != null) {
            return Optional.of(v2);
        }
        v2 = this.builtinVanilla.get(key);
        if (v2 != null) {
            return Optional.of(v2);
        }
        return Optional.empty();
    }

    public boolean isKnown(String cultureContext, String legacyName) {
        return this.resolve(cultureContext, legacyName).isPresent();
    }

    public Optional<String> resolveExact(String cultureContext, String legacyName) {
        String v;
        Map<String, String> cm;
        if (legacyName == null) {
            return Optional.empty();
        }
        String lcKey = legacyName.toLowerCase(Locale.ROOT);
        if (cultureContext != null && (cm = this.cultureOverrides.get(cultureContext)) != null && (v = cm.get(lcKey)) != null) {
            return Optional.of(v);
        }
        String v2 = this.globalOverrides.get(lcKey);
        if (v2 != null) {
            return Optional.of(v2);
        }
        v2 = this.builtinMillenaire.get(legacyName);
        if (v2 != null) {
            return Optional.of(v2);
        }
        v2 = this.builtinVanilla.get(legacyName);
        if (v2 != null) {
            return Optional.of(v2);
        }
        return Optional.empty();
    }

    public Optional<String> resolveOrWarn(String culture, String legacyName, String context) {
        Optional<String> r = this.resolve(culture, legacyName);
        if (r.isEmpty()) {
            System.out.println("  [WARN] " + context + " item not mapped: " + legacyName);
            this.reportMiss(culture, legacyName);
        }
        return r;
    }

    public Optional<String> resolveOrError(String culture, String legacyName, String context) {
        Optional<String> r = this.resolve(culture, legacyName);
        if (r.isEmpty()) {
            System.err.println("  [ERROR] " + context + " item not mapped: " + legacyName);
            this.reportMiss(culture, legacyName);
        }
        return r;
    }

    public Optional<String> resolveForCraftingGoal(String culture, String legacyName, String goalId) {
        Optional<String> r = this.resolve(culture, legacyName);
        if (r.isEmpty()) {
            System.out.println("    [WARN] crafting goal '" + goalId + "' item not mapped: " + legacyName);
            this.reportMiss(culture, legacyName);
        }
        return r;
    }

    public Optional<String> resolveForTradedGood(String culture, String legacyName) {
        Optional<String> r = this.resolve(culture, legacyName);
        if (r.isEmpty()) {
            System.out.println("  [WARN] Traded good item not mapped: " + legacyName);
            this.reportMiss(culture, legacyName);
        }
        return r;
    }

    public Optional<String> resolveForVillagerEquipment(String culture, String legacyName, String villagerId) {
        Optional<String> r = this.resolve(culture, legacyName);
        if (r.isEmpty()) {
            System.out.println("  [WARN] villager '" + villagerId + "' equipment item not mapped: " + legacyName);
            this.reportMiss(culture, legacyName);
        }
        return r;
    }

    public Optional<String> resolveForBuildingCost(String culture, String legacyName, String planId) {
        Optional<String> r = this.resolve(culture, legacyName);
        if (r.isEmpty()) {
            System.out.println("  [WARN] building plan '" + planId + "' cost item not mapped: " + legacyName);
            this.reportMiss(culture, legacyName);
        }
        return r;
    }

    public Optional<String> resolveForShopEntry(String culture, String legacyName, String shopId, String slot) {
        Optional<String> r = this.resolve(culture, legacyName);
        if (r.isEmpty()) {
            System.out.println("    [WARN] shop '" + shopId + "' " + slot + " item not mapped: " + legacyName);
            this.reportMiss(culture, legacyName);
        }
        return r;
    }

    /*
     * Enabled aggressive exception aggregation
     */
    private static Built loadBuiltinMap() {
        try (InputStream in = ItemIdMapper.class.getResourceAsStream(BUILTIN_RESOURCE);){
            Built built;
            if (in == null) {
                throw new IllegalStateException("Built-in legacy item map missing: /millenaire/legacy/legacy_item_map.json \u2014 this resource ships in src/main/resources/ and must be on the classpath.");
            }
            try (InputStreamReader r = new InputStreamReader(in, StandardCharsets.UTF_8);){
                JsonObject root = JsonParser.parseReader((Reader)r).getAsJsonObject();
                Map<String, String> mill = ItemIdMapper.parseMapField(root, "millenaire_items");
                Map<String, String> vanilla = ItemIdMapper.parseMapField(root, "vanilla_items");
                built = new Built(mill, vanilla);
            }
            return built;
        }
        catch (IOException e) {
            throw new IllegalStateException("Failed to load /millenaire/legacy/legacy_item_map.json", e);
        }
    }

    private static Map<String, String> parseMapField(JsonObject root, String field) {
        LinkedHashMap<String, String> out = new LinkedHashMap<String, String>();
        if (!root.has(field)) {
            return out;
        }
        JsonObject obj = root.getAsJsonObject(field);
        for (Map.Entry e : obj.entrySet()) {
            String originalKey = (String)e.getKey();
            String value = ((JsonElement)e.getValue()).getAsString();
            out.put(originalKey, value);
            String lc = originalKey.toLowerCase(Locale.ROOT);
            if (lc.equals(originalKey)) continue;
            out.putIfAbsent(lc, value);
        }
        return out;
    }

    private static void parseItemlist(Path file, Map<String, String> target) {
        List<String> lines;
        try {
            lines = LegacyDataParser.readLinesLenient(file);
        }
        catch (IOException e) {
            System.err.println("[WARN] Failed to read itemlist " + String.valueOf(file) + ": " + e.getMessage());
            return;
        }
        ArrayList<String> duplicates = new ArrayList<String>();
        for (String raw : lines) {
            String metadata;
            String[] parts;
            String line = raw.trim();
            if (line.isEmpty() || line.startsWith("#") || line.startsWith("//") || (parts = line.split(";")).length < 2) continue;
            String legacy = parts[0].trim().toLowerCase(Locale.ROOT);
            String modern = parts[1].trim();
            if (legacy.isEmpty() || modern.isEmpty()) continue;
            String string = metadata = parts.length >= 3 ? parts[2].trim() : "";
            if (!metadata.isEmpty()) {
                translated = LEGACY_1_12_TO_1_21_MAP.get(modern + ";" + metadata);
                if (translated != null) {
                    modern = translated;
                } else {
                    String fallback = LEGACY_1_12_TO_1_21_MAP.get(modern);
                    if (fallback != null) {
                        modern = fallback;
                    }
                }
            } else {
                translated = LEGACY_1_12_TO_1_21_MAP.get(modern);
                if (translated != null) {
                    modern = translated;
                }
            }
            if (target.containsKey(legacy)) {
                duplicates.add(legacy);
            }
            target.put(legacy, modern);
        }
        if (!duplicates.isEmpty()) {
            System.out.println("  [INFO] " + String.valueOf(file) + " has duplicate itemlist keys (last wins): " + String.valueOf(duplicates));
        }
    }

    private static Map<String, String> normalise(Map<String, String> in) {
        if (in == null || in.isEmpty()) {
            return Collections.emptyMap();
        }
        LinkedHashMap<String, String> out = new LinkedHashMap<String, String>();
        for (Map.Entry<String, String> e : in.entrySet()) {
            if (e.getKey() == null || e.getValue() == null) continue;
            out.put(e.getKey().toLowerCase(Locale.ROOT), e.getValue());
        }
        return out;
    }

    private static Map<String, Map<String, String>> normaliseByCulture(Map<String, Map<String, String>> in) {
        if (in == null || in.isEmpty()) {
            return Collections.emptyMap();
        }
        LinkedHashMap<String, Map<String, String>> out = new LinkedHashMap<String, Map<String, String>>();
        for (Map.Entry<String, Map<String, String>> e : in.entrySet()) {
            out.put(e.getKey(), ItemIdMapper.normalise(e.getValue()));
        }
        return out;
    }

    Map<String, String> builtinMillenaireSnapshot() {
        return Collections.unmodifiableMap(this.builtinMillenaire);
    }

    Map<String, String> builtinVanillaSnapshot() {
        return Collections.unmodifiableMap(this.builtinVanilla);
    }

    private record Built(Map<String, String> millenaire, Map<String, String> vanilla) {
    }
}

