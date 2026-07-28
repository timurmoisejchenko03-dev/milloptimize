/*
 * Decompiled with CFR 0.150.
 */
package org.millenaire;

import java.util.UUID;

public final class FormatUtils {
    private FormatUtils() {
    }

    public static String shortUuid(UUID uuid) {
        return uuid.toString().substring(0, 8);
    }
}

