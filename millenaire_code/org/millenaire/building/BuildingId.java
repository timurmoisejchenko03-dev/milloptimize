/*
 * Decompiled with CFR 0.152.
 */
package org.millenaire.building;

import java.util.UUID;

public record BuildingId(UUID uuid) {
    public static BuildingId random() {
        return new BuildingId(UUID.randomUUID());
    }
}

