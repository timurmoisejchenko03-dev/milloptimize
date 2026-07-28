/*
 * Decompiled with CFR 0.150.
 */
package org.millenaire.content;

import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;
import org.millenaire.content.BuiltInCultures;

public final class ContentLoadReport {
    private static final Map<String, Set<String>> MISSINGS_BY_CULTURE = new LinkedHashMap<String, Set<String>>();

    private ContentLoadReport() {
    }

    /*
     * WARNING - Removed try catching itself - possible behaviour change.
     */
    public static void clear() {
        Map<String, Set<String>> map = MISSINGS_BY_CULTURE;
        synchronized (map) {
            MISSINGS_BY_CULTURE.clear();
        }
    }

    /*
     * WARNING - Removed try catching itself - possible behaviour change.
     */
    public static void recordMissingClasspathFile(String culture, String classpathPath) {
        if (culture == null || classpathPath == null) {
            return;
        }
        if (BuiltInCultures.IDS.contains(culture)) {
            return;
        }
        Map<String, Set<String>> map = MISSINGS_BY_CULTURE;
        synchronized (map) {
            MISSINGS_BY_CULTURE.computeIfAbsent(culture, k -> new TreeSet()).add(classpathPath);
        }
    }

    /*
     * WARNING - Removed try catching itself - possible behaviour change.
     */
    public static Map<String, List<String>> snapshot() {
        TreeMap out = new TreeMap();
        Map<String, Set<String>> map = MISSINGS_BY_CULTURE;
        synchronized (map) {
            for (Map.Entry<String, Set<String>> e : MISSINGS_BY_CULTURE.entrySet()) {
                out.put(e.getKey(), List.copyOf((Collection)e.getValue()));
            }
        }
        return Collections.unmodifiableMap(out);
    }

    /*
     * WARNING - Removed try catching itself - possible behaviour change.
     */
    public static boolean isEmpty() {
        Map<String, Set<String>> map = MISSINGS_BY_CULTURE;
        synchronized (map) {
            return MISSINGS_BY_CULTURE.isEmpty();
        }
    }
}

