/*
 * Decompiled with CFR 0.152.
 */
package org.millenaire.culture;

public enum Gender {
    MALE,
    FEMALE;


    public static Gender fromString(String value) {
        return switch (value.toLowerCase()) {
            case "female" -> FEMALE;
            default -> MALE;
        };
    }
}

