/*
 * Decompiled with CFR 0.150.
 */
package org.millenaire.content.legacy;

import java.text.Normalizer;
import java.util.Locale;
import java.util.Map;

public final class LegacyIdCanonicaliser {
    private static final Map<String, String> VILLAGE_TYPE_ID_FIXES = Map.of("kunfun_tomb", "kufun_tomb");

    private LegacyIdCanonicaliser() {
    }

    public static String buildingPlanId(String culture, String legacyName) {
        return LegacyIdCanonicaliser.prefixed(culture, LegacyIdCanonicaliser.sanitize(legacyName));
    }

    public static String villagerTypeId(String culture, String legacyName) {
        return LegacyIdCanonicaliser.prefixed(culture, LegacyIdCanonicaliser.sanitize(legacyName));
    }

    public static String villageTypeId(String culture, String legacyName) {
        String base = LegacyIdCanonicaliser.sanitize(legacyName);
        base = VILLAGE_TYPE_ID_FIXES.getOrDefault(base, base);
        return LegacyIdCanonicaliser.prefixed(culture, base);
    }

    public static String shopId(String culture, String legacyName) {
        return LegacyIdCanonicaliser.sanitize(legacyName);
    }

    public static String buildingPlanRefId(String legacyName) {
        return LegacyIdCanonicaliser.sanitize(legacyName);
    }

    public static String villagerTypeRefId(String legacyName) {
        return LegacyIdCanonicaliser.sanitize(legacyName);
    }

    public static String villageTypeRefId(String legacyName) {
        String base = LegacyIdCanonicaliser.sanitize(legacyName);
        return VILLAGE_TYPE_ID_FIXES.getOrDefault(base, base);
    }

    public static String applyVillageTypeTypoFix(String alreadyCleanId) {
        return VILLAGE_TYPE_ID_FIXES.getOrDefault(alreadyCleanId, alreadyCleanId);
    }

    public static String shopRefId(String legacyName) {
        return LegacyIdCanonicaliser.sanitize(legacyName);
    }

    static String prefixed(String culture, String id) {
        String prefix = culture + "_";
        return id.startsWith(prefix) ? id : prefix + id;
    }

    static String stripCulturePrefix(String culture, String id) {
        String prefix = culture + "_";
        return id.startsWith(prefix) ? id.substring(prefix.length()) : id;
    }

    static String sanitize(String name) {
        StringBuilder sb = new StringBuilder();
        for (int cp : name.toLowerCase(Locale.ROOT).codePoints().toArray()) {
            sb.appendCodePoint(switch (cp) {
                case 246 -> 111;
                case 252 -> 117;
                case 231 -> 99;
                case 351 -> 115;
                case 305 -> 105;
                case 287 -> 103;
                default -> cp;
            });
        }
        String result = Normalizer.normalize(sb.toString(), Normalizer.Form.NFD);
        result = result.replaceAll("[\\u0300-\\u036f]", "");
        result = result.replaceAll("[^a-z0-9_]", "");
        return result;
    }
}

