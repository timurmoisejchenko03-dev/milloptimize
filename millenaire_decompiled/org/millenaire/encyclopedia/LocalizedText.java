/*
 * Decompiled with CFR 0.150.
 */
package org.millenaire.encyclopedia;

public record LocalizedText(String key, String id, String text) {
    public LocalizedText {
        int nonNull = (key != null ? 1 : 0) + (id != null ? 1 : 0) + (text != null ? 1 : 0);
        if (nonNull > 1) {
            throw new IllegalArgumentException("LocalizedText must have at most one non-null field, got: key=" + key + ", id=" + id + ", text=" + text);
        }
    }

    public static LocalizedText key(String k) {
        return new LocalizedText(k, null, null);
    }

    public static LocalizedText id(String i) {
        return new LocalizedText(null, i, null);
    }

    public static LocalizedText lit(String t) {
        return new LocalizedText(null, null, t);
    }
}

