/*
 * Decompiled with CFR 0.152.
 */
package org.millenaire.catalog;

import java.util.Locale;

public enum CatalogTarget {
    VILLAGERS,
    BUILDINGS,
    ALL;


    public static CatalogTarget fromString(String raw) {
        return switch (raw.toLowerCase(Locale.ROOT)) {
            case "villagers" -> VILLAGERS;
            case "buildings" -> BUILDINGS;
            case "all" -> ALL;
            default -> throw new IllegalArgumentException("Unknown catalog target: " + raw);
        };
    }

    public boolean includesVillagers() {
        return this == VILLAGERS || this == ALL;
    }

    public boolean includesBuildings() {
        return this == BUILDINGS || this == ALL;
    }
}

