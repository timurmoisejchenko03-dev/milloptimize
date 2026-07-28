/*
 * Decompiled with CFR 0.152.
 */
package org.millenaire.village;

import java.util.UUID;

public record VillageId(UUID uuid) {
    public static VillageId random() {
        return new VillageId(UUID.randomUUID());
    }
}

