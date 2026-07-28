/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  javax.annotation.Nullable
 */
package org.millenaire.goal.visit;

import java.util.Set;
import javax.annotation.Nullable;
import org.millenaire.building.SpecialPoint;

public final class SpecialPointResolver {
    private static final Set<String> VALID_VALUES = SpecialPoint.values();

    private SpecialPointResolver() {
    }

    @Nullable
    public static String resolveOrThrow(@Nullable String value, String context) {
        if (value == null) {
            return null;
        }
        if (VALID_VALUES.contains(value)) {
            return value;
        }
        throw new IllegalArgumentException("Unknown SpecialPoint value '" + value + "' in " + context + " \u2014 expected one of " + String.valueOf(VALID_VALUES));
    }
}

