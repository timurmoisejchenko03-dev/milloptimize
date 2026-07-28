/*
 * Decompiled with CFR 0.150.
 */
package org.millenaire.content.legacy;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;
import org.millenaire.content.legacy.LegacyConversionReport;
import org.millenaire.content.legacy.LegacyDataParser;
import org.millenaire.content.legacy.LegacyLayoutDetector;

final class TagConsistencyChecker {
    static final Set<String> HARDCODED_BUILDING_TAGS = Set.of("archives", "autospawnvillagers", "borderpostsign", "brickkiln", "cattle", "chicken", "despawnallmobs", "fishingspot", "grove", "hof", "inn", "leasure", "leisure", "market", "marvel", "no_upgrade_till_wall_initialized", "nopaths", "pathnode", "pigs", "pujas", "sacrifices", "scaffoldings", "sheeps", "silkwormfarm", "snailsfarm", "sugarplantation");
    static final Set<String> HARDCODED_VILLAGER_TAGS = Set.of("archer", "chief", "child", "defensive", "foreignmerchant", "helpinattacks", "hidename", "hostile", "localmerchant", "meditates", "noleafclearing", "noresurrect", "noteleport", "performssacrifices", "raider", "seller", "showhealth", "visitor");
    private static final Pattern BUILDING_PREFIX_RE = Pattern.compile("^(?:building|initial|upgrade\\d+)\\.");

    private TagConsistencyChecker() {
    }

    static void check(String culture, Path sourceRoot, LegacyLayoutDetector.Detection detection, Map<String, Path> goalIndex, LegacyConversionReport report) {
        HashSet<String> buildingTags = new HashSet<String>();
        for (Path path : TagConsistencyChecker.onlyCulture(detection.buildingTxts(), culture)) {
            for (Map.Entry<String, List<String>> entry : TagConsistencyChecker.safeParseKv(path).entrySet()) {
                String string = BUILDING_PREFIX_RE.matcher(entry.getKey()).replaceFirst("");
                if (!string.equals("tag") && !string.equals("villagetag")) continue;
                for (String value : entry.getValue()) {
                    for (String t : value.split(",")) {
                        String tok = t.trim().toLowerCase(Locale.ROOT);
                        if (tok.isEmpty()) continue;
                        buildingTags.add(tok);
                    }
                }
            }
        }
        HashSet<String> villagerTags = new HashSet<String>();
        for (Path path : TagConsistencyChecker.onlyCulture(detection.villagerTxts(), culture)) {
            for (Map.Entry<String, List<String>> entry : TagConsistencyChecker.safeParseKv(path).entrySet()) {
                if (!entry.getKey().equals("tag")) continue;
                for (String value : entry.getValue()) {
                    String string = value.trim().toLowerCase(Locale.ROOT);
                    if (string.isEmpty()) continue;
                    villagerTags.add(string);
                }
            }
        }
        for (Path path : detection.questTxts()) {
            if (!TagConsistencyChecker.questBelongsToCulture(path, culture)) continue;
            try {
                List<String> list = Files.readAllLines(path);
                for (String string : list) {
                    String value;
                    String[] parts;
                    String key;
                    int n;
                    String line = string.trim();
                    if (line.isEmpty() || line.startsWith("//") || (n = line.indexOf(58)) <= 0 || !(key = line.substring(0, n).trim().toLowerCase(Locale.ROOT)).equals("settagsuccess") && !key.equals("settagfailure") && !key.equals("settag") || (parts = (value = line.substring(n + 1).trim()).split(",")).length < 2) continue;
                    villagerTags.add(parts[1].trim().toLowerCase(Locale.ROOT));
                }
            }
            catch (IOException iOException) {
            }
        }
        HashSet<String> hashSet = new HashSet<String>();
        for (Path path : TagConsistencyChecker.onlyCulture(detection.villagerTxts(), culture)) {
            for (Map.Entry<String, List<String>> entry : TagConsistencyChecker.safeParseKv(path).entrySet()) {
                if (!entry.getKey().equals("goal")) continue;
                for (String string : entry.getValue()) {
                    String tok = string.trim().toLowerCase(Locale.ROOT);
                    if (tok.isEmpty()) continue;
                    hashSet.add(tok);
                }
            }
        }
        HashMap<String, String> hashMap = new HashMap<String, String>();
        for (String string : hashSet) {
            Path path = goalIndex.get(string);
            if (path == null) continue;
            for (Map.Entry entry : TagConsistencyChecker.safeParseKv(path).entrySet()) {
                if (!((String)entry.getKey()).equals("buildingtag")) continue;
                for (String value : (List)entry.getValue()) {
                    String tag = value.trim().toLowerCase(Locale.ROOT);
                    if (tag.isEmpty() || HARDCODED_BUILDING_TAGS.contains(tag) || tag.startsWith("wall_level")) continue;
                    hashMap.putIfAbsent(tag, path.getFileName().toString());
                }
            }
        }
        HashMap<String, String> hashMap2 = new HashMap<String, String>();
        for (Path path : detection.questTxts()) {
            if (!TagConsistencyChecker.questBelongsToCulture(path, culture)) continue;
            try {
                List<String> lines = Files.readAllLines(path);
                for (String raw : lines) {
                    String key;
                    int colon;
                    String line = raw.trim();
                    if (line.isEmpty() || line.startsWith("//") || (colon = line.indexOf(58)) <= 0 || !(key = line.substring(0, colon).trim().toLowerCase(Locale.ROOT)).equals("definevillager")) continue;
                    String value = line.substring(colon + 1).trim();
                    HashMap<String, String> vparams = new HashMap<String, String>();
                    for (String seg : value.split(",")) {
                        int eq = seg.indexOf(61);
                        if (eq <= 0) continue;
                        vparams.put(seg.substring(0, eq).trim().toLowerCase(Locale.ROOT), seg.substring(eq + 1).trim().toLowerCase(Locale.ROOT));
                    }
                    String tag = (String)vparams.get("requiredtag");
                    if (tag == null || tag.isEmpty() || HARDCODED_VILLAGER_TAGS.contains(tag)) continue;
                    hashMap2.putIfAbsent(tag, path.getFileName().toString());
                }
            }
            catch (IOException lines) {
            }
        }
        ArrayList arrayList = new ArrayList(hashMap.keySet());
        arrayList.sort(String::compareTo);
        for (String tag : arrayList) {
            if (buildingTags.contains(tag)) continue;
            report.recordUnresolvedBuildingTag(culture, tag, (String)hashMap.get(tag));
        }
        ArrayList arrayList2 = new ArrayList(hashMap2.keySet());
        arrayList2.sort(String::compareTo);
        for (String string : arrayList2) {
            if (villagerTags.contains(string)) continue;
            report.recordUnresolvedVillagerTag(culture, string, (String)hashMap2.get(string));
        }
    }

    static boolean questBelongsToCulture(Path qf, String culture) {
        String cultureLow = culture.toLowerCase(Locale.ROOT);
        String pathCulture = LegacyLayoutDetector.Detection.cultureOf(qf);
        if (pathCulture != null) {
            return cultureLow.equals(pathCulture);
        }
        for (int i = 0; i < qf.getNameCount() - 1; ++i) {
            if (!"quests".equalsIgnoreCase(qf.getName(i).toString()) || i + 1 >= qf.getNameCount()) continue;
            String family = qf.getName(i + 1).toString().toLowerCase(Locale.ROOT);
            if (!family.contains(cultureLow)) break;
            return true;
        }
        try {
            for (String raw : Files.readAllLines(qf)) {
                int colon;
                String line = raw.trim();
                if (line.isEmpty() || line.startsWith("//") || (colon = line.indexOf(58)) <= 0 || !line.substring(0, colon).trim().toLowerCase(Locale.ROOT).equals("definevillager")) continue;
                String value = line.substring(colon + 1);
                for (String seg : value.split(",")) {
                    String typeVal;
                    int slash;
                    String k;
                    int eq = seg.indexOf(61);
                    if (eq <= 0 || !(k = seg.substring(0, eq).trim().toLowerCase(Locale.ROOT)).equals("type") || (slash = (typeVal = seg.substring(eq + 1).trim().toLowerCase(Locale.ROOT)).indexOf(47)) <= 0 || !typeVal.substring(0, slash).equals(cultureLow)) continue;
                    return true;
                }
            }
        }
        catch (IOException iOException) {
            // empty catch block
        }
        return false;
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

