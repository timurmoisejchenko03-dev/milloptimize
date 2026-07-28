/*
 * Decompiled with CFR 0.150.
 */
package org.millenaire.catalog;

import java.util.Locale;

public final class CatalogTarget
extends Enum<CatalogTarget> {
    public static final /* enum */ CatalogTarget VILLAGERS = new CatalogTarget();
    public static final /* enum */ CatalogTarget BUILDINGS = new CatalogTarget();
    public static final /* enum */ CatalogTarget ALL = new CatalogTarget();
    private static final /* synthetic */ CatalogTarget[] $VALUES;

    public static CatalogTarget[] values() {
        return (CatalogTarget[])$VALUES.clone();
    }

    public static CatalogTarget valueOf(String name) {
        return Enum.valueOf(CatalogTarget.class, name);
    }

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

    private static /* synthetic */ CatalogTarget[] $values() {
        return new CatalogTarget[]{VILLAGERS, BUILDINGS, ALL};
    }

    static {
        $VALUES = CatalogTarget.$values();
    }
}

