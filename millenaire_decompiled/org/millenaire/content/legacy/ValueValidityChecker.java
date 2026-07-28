/*
 * Decompiled with CFR 0.150.
 */
package org.millenaire.content.legacy;

import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import org.millenaire.content.legacy.LegacyConversionReport;

final class ValueValidityChecker {
    static final Set<String> VILLAGER_INT_KEYS = Set.of("health", "hiringcost", "experiencegiven", "baseattackstrength", "chanceweight", "weight", "priority", "priorityrandom", "duration", "reoccurdelay", "range", "minimumhour", "maximumhour", "maxsimultaneousinbuilding", "maxsimultaneoustotal", "minimumtocook", "minimumfuel", "minimumpickup", "minimumavailableblocks", "max", "reputation", "version", "radius", "irrigation", "buildingorientation");
    static final Set<String> VILLAGER_FLOAT_KEYS = Set.of("baseheight", "basespeed", "mindistance", "maxdistance", "minimumbiomevalidity");
    static final Set<String> VILLAGER_BOOL_KEYS = Set.of("townhallgoal", "allowrandommoves", "holdweapons", "lookatgoal", "leasure", "sprint", "collectinbuilding", "aggressiveslaughter", "isgift", "issubbuilding", "rebuildpath", "nopathstobuilding", "showtownhallsigns", "isborderbuilding", "travelbook_display", "travelbook_main_culture_villager", "travelbookshow");
    static final Set<String> GENDER_VALUES = Set.of("male", "female");
    static final Set<String> TOOL_CLASSES = Set.of("axe", "axes", "hoe", "hoes", "pickaxe", "pickaxes", "shovel", "shovels", "sword", "swords", "fishingrod", "shears", "armour", "toolssword", "rangedweapons");
    static final Set<String> VILLAGE_INT_KEYS = Set.of("weight", "radius", "innerwallradius", "maxsimultaneousconstructions", "maxsimultaneouswallconstructions");
    static final Set<String> VILLAGE_BOOL_KEYS = Set.of("carriesraid", "playercontrolled", "spawnable");
    static final Set<String> LONEBUILDING_INT_KEYS = Set.of("weight", "radius", "mindistancefromspawn", "max", "innerwallradius", "maxsimultaneousconstructions", "maxsimultaneouswallconstructions");
    static final Set<String> LONEBUILDING_FLOAT_KEYS = Set.of("minimumbiomevalidity");
    static final Set<String> LONEBUILDING_BOOL_KEYS = Set.of("carriesraid", "spawnable", "generateforplayer", "playercontrolled", "travelbook_display");
    static final Set<String> BUILDING_INT_KEYS = Set.of("priority", "startlevel", "pathlevel", "pathwidth", "prioritymovein", "length", "width", "areatoclear", "areatoclearlengthbefore", "areatoclearlengthafter", "areatoclearwidthbefore", "areatoclearwidthafter", "buildingorientation", "altitudeoffset", "foundationdepth", "max", "reputation", "version", "irrigation", "extrasimultaneousconstructions", "extrasimultaneouswallconstructions");
    static final Set<String> BUILDING_FLOAT_KEYS = Set.of("mindistance", "maxdistance");
    static final Set<String> BUILDING_BOOL_KEYS = Set.of("rebuildpath", "nopathstobuilding", "isgift", "issubbuilding", "showtownhallsigns", "travelbook_display");

    private ValueValidityChecker() {
    }

    static void checkVillager(String culture, String relPath, Map<String, List<String>> data, LegacyConversionReport report) {
        List<String> toolVals;
        String value;
        ValueValidityChecker.checkPrimitives(culture, relPath, data, report, VILLAGER_INT_KEYS, VILLAGER_FLOAT_KEYS, VILLAGER_BOOL_KEYS);
        List<String> genderVals = data.get("gender");
        if (!(genderVals == null || genderVals.isEmpty() || (value = genderVals.get(0).trim()).isEmpty() || GENDER_VALUES.contains(value.toLowerCase(Locale.ROOT)))) {
            report.recordInvalidEnum(culture, relPath, "gender", value, "male, female");
        }
        if ((toolVals = data.get("toolneededclass")) != null) {
            for (String raw : toolVals) {
                String value2 = raw.trim();
                if (value2.isEmpty() || TOOL_CLASSES.contains(value2.toLowerCase(Locale.ROOT))) continue;
                report.recordInvalidEnum(culture, relPath, "toolneededclass", value2, String.join((CharSequence)", ", TOOL_CLASSES));
            }
        }
    }

    static void checkVillage(String culture, String relPath, Map<String, List<String>> data, LegacyConversionReport report) {
        ValueValidityChecker.checkPrimitives(culture, relPath, data, report, VILLAGE_INT_KEYS, Set.of(), VILLAGE_BOOL_KEYS);
        ValueValidityChecker.checkPriceFormat(culture, relPath, "sellingprice", data, report);
        ValueValidityChecker.checkPriceFormat(culture, relPath, "buyingprice", data, report);
    }

    static void checkLoneBuilding(String culture, String relPath, Map<String, List<String>> data, LegacyConversionReport report) {
        ValueValidityChecker.checkPrimitives(culture, relPath, data, report, LONEBUILDING_INT_KEYS, LONEBUILDING_FLOAT_KEYS, LONEBUILDING_BOOL_KEYS);
        ValueValidityChecker.checkPriceFormat(culture, relPath, "sellingprice", data, report);
        ValueValidityChecker.checkPriceFormat(culture, relPath, "buyingprice", data, report);
    }

    static void checkBuildingBare(String culture, String relPath, Map<String, List<String>> dataByBareKey, LegacyConversionReport report) {
        ValueValidityChecker.checkPrimitives(culture, relPath, dataByBareKey, report, BUILDING_INT_KEYS, BUILDING_FLOAT_KEYS, BUILDING_BOOL_KEYS);
    }

    private static void checkPrimitives(String culture, String relPath, Map<String, List<String>> data, LegacyConversionReport report, Set<String> intKeys, Set<String> floatKeys, Set<String> boolKeys) {
        for (Map.Entry<String, List<String>> e : data.entrySet()) {
            String key = e.getKey();
            for (String raw : e.getValue()) {
                String value = raw.trim();
                if (value.isEmpty()) continue;
                if (intKeys.contains(key) && !ValueValidityChecker.isInt(value)) {
                    report.recordInvalidNumeric(culture, relPath, key, value, "int");
                    continue;
                }
                if (floatKeys.contains(key) && !ValueValidityChecker.isFloat(value)) {
                    report.recordInvalidNumeric(culture, relPath, key, value, "float");
                    continue;
                }
                if (!boolKeys.contains(key) || ValueValidityChecker.isBool(value)) continue;
                report.recordInvalidNumeric(culture, relPath, key, value, "bool");
            }
        }
    }

    private static void checkPriceFormat(String culture, String relPath, String key, Map<String, List<String>> data, LegacyConversionReport report) {
        List<String> values = data.get(key);
        if (values == null) {
            return;
        }
        for (String raw : values) {
            int comma;
            String pricePart;
            String trimmed = raw.trim();
            if (trimmed.isEmpty() || (pricePart = (comma = trimmed.indexOf(44)) >= 0 ? trimmed.substring(comma + 1).trim() : trimmed).isEmpty() || ValueValidityChecker.isPriceTuple(pricePart)) continue;
            report.recordInvalidFormat(culture, relPath, key, raw, "N or N/N or N/N/N price tuple");
        }
    }

    private static boolean isInt(String value) {
        try {
            Integer.parseInt(value);
            return true;
        }
        catch (NumberFormatException nfe) {
            return false;
        }
    }

    private static boolean isFloat(String value) {
        try {
            Double.parseDouble(value);
            return true;
        }
        catch (NumberFormatException nfe) {
            return false;
        }
    }

    private static boolean isBool(String value) {
        String lower = value.toLowerCase(Locale.ROOT);
        return lower.equals("true") || lower.equals("false");
    }

    private static boolean isPriceTuple(String value) {
        String[] parts = value.split("/");
        if (parts.length < 1 || parts.length > 3) {
            return false;
        }
        for (String p : parts) {
            if (ValueValidityChecker.isInt(p.trim())) continue;
            return false;
        }
        return true;
    }
}

