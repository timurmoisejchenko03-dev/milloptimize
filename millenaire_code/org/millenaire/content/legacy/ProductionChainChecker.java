/*
 * Decompiled with CFR 0.152.
 */
package org.millenaire.content.legacy;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import org.millenaire.content.legacy.LegacyConversionReport;
import org.millenaire.content.legacy.LegacyDataParser;
import org.millenaire.content.legacy.LegacyLayoutDetector;

final class ProductionChainChecker {
    static final Set<String> IGNORE_ORPHAN_OUTPUTS = Set.of("sapling", "sapling_oak", "oak_sapling", "sapling_birch", "birch_sapling", "sapling_spruce", "spruce_sapling", "sapling_pine", "sapling_jungle", "jungle_sapling", "sapling_acacia", "acacia_sapling", "sapling_darkoak", "dark_oak_sapling", "sapling_cherry", "cherry_sapling", "wool_lightgray", "wool_light_gray", "light_gray_wool", "wool_pink", "pink_wool");
    private static final Set<String> BANDIT_KEYWORDS = Set.of("bandit", "felon", "raider");
    static final Set<String> WORLD_AVAILABLE = Set.of("wheat", "carrot", "potato", "beetroot", "beetroot_seeds", "wheat_seeds", "pumpkin_seeds", "melon_seeds", "sugar_cane", "cocoa_beans", "nether_wart", "egg", "feather", "chicken", "leather", "beef", "mutton", "porkchop", "milk_bucket", "milk", "rabbit", "rabbit_hide", "wool", "white_wool", "black_wool", "brown_wool", "gray_wool", "light_gray_wool", "oak_log", "birch_log", "spruce_log", "jungle_log", "acacia_log", "dark_oak_log", "oak_sapling", "birch_sapling", "spruce_sapling", "jungle_sapling", "acacia_sapling", "dark_oak_sapling", "wood", "log", "cod", "salmon", "fish", "raw_fish", "bone", "string", "gunpowder", "slime_ball", "snowball", "ice", "flint", "clay_ball", "clay");

    private ProductionChainChecker() {
    }

    static void check(String culture, LegacyLayoutDetector.Detection detection, Map<String, Path> goalIndex, LegacyConversionReport report) {
        Object fname;
        Iterator<String> data;
        LinkedHashMap<String, String> produced = new LinkedHashMap<String, String>();
        LinkedHashMap<String, String> consumed = new LinkedHashMap<String, String>();
        LinkedHashMap<String, String> sold = new LinkedHashMap<String, String>();
        LinkedHashMap<String, String> bought = new LinkedHashMap<String, String>();
        HashSet<String> banditOnlyOutputs = new HashSet<String>();
        HashSet<String> nonBanditOutputs = new HashSet<String>();
        for (Path path : ProductionChainChecker.onlyCulture(detection.villagerTxts(), culture)) {
            String fname2 = path.getFileName().toString();
            String fnameLow = fname2.toLowerCase(Locale.ROOT);
            boolean bl = false;
            for (String kw : BANDIT_KEYWORDS) {
                if (!fnameLow.contains(kw)) continue;
                bl = true;
                break;
            }
            data = ProductionChainChecker.safeParseKv(path);
            LinkedHashMap<String, String> producedHere = new LinkedHashMap<String, String>();
            ProductionChainChecker.collectFirstToken(data, "bringbackhomegood", producedHere, fname2);
            ProductionChainChecker.collectFirstToken(data, "collectgood", producedHere, fname2);
            for (Map.Entry pe : producedHere.entrySet()) {
                produced.putIfAbsent((String)pe.getKey(), (String)pe.getValue());
                if (bl) {
                    banditOnlyOutputs.add((String)pe.getKey());
                    continue;
                }
                nonBanditOutputs.add((String)pe.getKey());
            }
            ProductionChainChecker.collectFirstToken((Map<String, List<String>>)((Object)data), "requiredgood", consumed, fname2);
            ProductionChainChecker.collectFirstToken((Map<String, List<String>>)((Object)data), "itemneeded", consumed, fname2);
        }
        banditOnlyOutputs.removeAll(nonBanditOutputs);
        HashSet<String> usedGoalNames = new HashSet<String>();
        for (Path vf : ProductionChainChecker.onlyCulture(detection.villagerTxts(), culture)) {
            for (Map.Entry entry : ProductionChainChecker.safeParseKv(vf).entrySet()) {
                if (!((String)entry.getKey()).equals("goal")) continue;
                for (String value : (List)entry.getValue()) {
                    String tok = value.trim().toLowerCase(Locale.ROOT);
                    if (tok.isEmpty()) continue;
                    usedGoalNames.add(tok);
                }
            }
        }
        for (String goalName : usedGoalNames) {
            Path gf = goalIndex.get(goalName);
            if (gf == null) continue;
            String string = gf.getFileName().toString();
            data = ProductionChainChecker.safeParseKv(gf);
            ProductionChainChecker.collectFirstToken(data, "output", produced, string);
            ProductionChainChecker.collectFirstToken((Map<String, List<String>>)((Object)data), "loot", produced, string);
            ProductionChainChecker.collectFirstToken((Map<String, List<String>>)((Object)data), "harvestitem", produced, string);
            ProductionChainChecker.collectFirstToken(data, "bonusitem", produced, string);
            ProductionChainChecker.collectFirstToken(data, "input", consumed, string);
            ProductionChainChecker.collectFirstToken(data, "seed", consumed, string);
            ProductionChainChecker.collectFirstToken(data, "itemtocook", consumed, string);
        }
        for (Path sf : ProductionChainChecker.onlyCulture(detection.shopTxts(), culture)) {
            fname = sf.getFileName().toString();
            Map<String, List<String>> map = ProductionChainChecker.safeParseKv(sf);
            ProductionChainChecker.collectAllTokens(map, "sells", sold, (String)fname);
            ProductionChainChecker.collectAllTokens(map, "deliverto", sold, (String)fname);
            ProductionChainChecker.collectAllTokens(map, "buys", bought, (String)fname);
            ProductionChainChecker.collectAllTokens(map, "buysoptional", bought, (String)fname);
        }
        for (Path tf : ProductionChainChecker.onlyCulture(detection.tradedGoodsTxts(), culture)) {
            fname = tf.getFileName().toString();
            try {
                List<String> list = Files.readAllLines(tf);
                for (String raw : list) {
                    String item;
                    String[] parts;
                    String line = raw.trim();
                    if (line.isEmpty() || line.startsWith("//") || (parts = line.split(",")).length < 1 || (item = ProductionChainChecker.normalise(parts[0].trim())).isEmpty()) continue;
                    bought.putIfAbsent(item, (String)fname);
                }
            }
            catch (IOException iOException) {
            }
        }
        HashSet hashSet = new HashSet(produced.keySet());
        hashSet.addAll(bought.keySet());
        hashSet.addAll(WORLD_AVAILABLE);
        TreeSet needed = new TreeSet();
        needed.addAll(consumed.keySet());
        needed.addAll(sold.keySet());
        for (String string : needed) {
            if (hashSet.contains(string)) continue;
            String requiredBy = consumed.getOrDefault(string, (String)sold.get(string));
            report.recordUnreachableInput(culture, string, requiredBy);
        }
        HashSet consumedOrSold = new HashSet(consumed.keySet());
        consumedOrSold.addAll(sold.keySet());
        ArrayList arrayList = new ArrayList(produced.keySet());
        Collections.sort(arrayList);
        for (String item : arrayList) {
            if (consumedOrSold.contains(item) || IGNORE_ORPHAN_OUTPUTS.contains(item) || banditOnlyOutputs.contains(item)) continue;
            report.recordOrphanedOutput(culture, item, (String)produced.get(item));
        }
    }

    private static void collectFirstToken(Map<String, List<String>> data, String key, Map<String, String> into, String file) {
        List<String> values = data.get(key);
        if (values == null) {
            return;
        }
        for (String raw : values) {
            String item;
            String[] parts = raw.split(",", 2);
            if (parts.length == 0 || (item = ProductionChainChecker.normalise(parts[0].trim())).isEmpty()) continue;
            into.putIfAbsent(item, file);
        }
    }

    private static void collectAllTokens(Map<String, List<String>> data, String key, Map<String, String> into, String file) {
        List<String> values = data.get(key);
        if (values == null) {
            return;
        }
        for (String raw : values) {
            for (String token : raw.split(",")) {
                String item = ProductionChainChecker.normalise(token.trim());
                if (item.isEmpty()) continue;
                into.putIfAbsent(item, file);
            }
        }
    }

    private static String normalise(String raw) {
        String lower = raw.toLowerCase(Locale.ROOT);
        if (lower.startsWith("minecraft:")) {
            lower = lower.substring("minecraft:".length());
        }
        return lower;
    }

    private static Map<String, List<String>> safeParseKv(Path file) {
        try {
            return LegacyDataParser.parseKeyValues(file);
        }
        catch (IOException io) {
            return Map.of();
        }
    }

    private static List<Path> onlyCulture(List<Path> paths, String culture) {
        ArrayList<Path> out = new ArrayList<Path>();
        for (Path p : paths) {
            String c = LegacyLayoutDetector.Detection.cultureOf(p);
            if (!culture.equalsIgnoreCase(c)) continue;
            out.add(p);
        }
        return out;
    }
}

