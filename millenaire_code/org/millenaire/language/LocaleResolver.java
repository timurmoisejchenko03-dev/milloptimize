/*
 * Decompiled with CFR 0.152.
 */
package org.millenaire.language;

import java.util.Collection;
import java.util.TreeSet;

public final class LocaleResolver {
    private LocaleResolver() {
    }

    public static String resolveSupported(String requested, Collection<String> supported) {
        if (requested == null || requested.isEmpty()) {
            return null;
        }
        if (supported.contains(requested)) {
            return requested;
        }
        int idx = requested.indexOf(95);
        if (idx <= 0) {
            return null;
        }
        String prefix = requested.substring(0, idx);
        if (supported.contains(prefix)) {
            return prefix;
        }
        String canonical = prefix + "_" + prefix;
        if (!canonical.equals(requested) && supported.contains(canonical)) {
            return canonical;
        }
        String sameLangPrefix = prefix + "_";
        TreeSet<String> sorted = new TreeSet<String>(supported);
        for (String code : sorted) {
            if (code.equals(requested) || !code.startsWith(sameLangPrefix)) continue;
            return code;
        }
        return null;
    }
}

