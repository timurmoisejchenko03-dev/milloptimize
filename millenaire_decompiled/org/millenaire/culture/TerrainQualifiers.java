/*
 * Decompiled with CFR 0.150.
 * 
 * Could not load the following classes:
 *  javax.annotation.Nullable
 */
package org.millenaire.culture;

import javax.annotation.Nullable;

public record TerrainQualifiers(@Nullable String forest, @Nullable String hill, @Nullable String mountain, @Nullable String desert, @Nullable String lava, @Nullable String lake, @Nullable String ocean) {
    public static final TerrainQualifiers EMPTY = new TerrainQualifiers(null, null, null, null, null, null, null);

    public boolean hasAny() {
        return this.forest != null || this.hill != null || this.mountain != null || this.desert != null || this.lava != null || this.lake != null || this.ocean != null;
    }
}

