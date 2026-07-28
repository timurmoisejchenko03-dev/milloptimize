/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  javax.annotation.Nullable
 */
package org.millenaire.language;

import javax.annotation.Nullable;

public final class DisplayNameResolver {
    private DisplayNameResolver() {
    }

    public static String resolve(String resolvedText, boolean translatable, @Nullable String nativePrefix) {
        return DisplayNameResolver.resolve(resolvedText, translatable, nativePrefix, null);
    }

    public static String resolve(String resolvedText, boolean translatable, @Nullable String nativePrefix, @Nullable String originalKey) {
        if (!translatable) {
            return resolvedText;
        }
        if (nativePrefix == null) {
            return resolvedText;
        }
        if (resolvedText.isEmpty()) {
            return nativePrefix;
        }
        if (originalKey != null && resolvedText.equals(originalKey)) {
            return nativePrefix;
        }
        if (DisplayNameResolver.equivalent(nativePrefix, resolvedText)) {
            return nativePrefix;
        }
        return nativePrefix + " (" + resolvedText + ")";
    }

    public static boolean equivalent(String a, String b) {
        if (a == null || b == null) {
            return a == b;
        }
        return a.trim().equalsIgnoreCase(b.trim());
    }
}

