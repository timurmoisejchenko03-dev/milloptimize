/*
 * Decompiled with CFR 0.150.
 */
package org.millenaire.culture;

public final class Gender
extends Enum<Gender> {
    public static final /* enum */ Gender MALE = new Gender();
    public static final /* enum */ Gender FEMALE = new Gender();
    private static final /* synthetic */ Gender[] $VALUES;

    public static Gender[] values() {
        return (Gender[])$VALUES.clone();
    }

    public static Gender valueOf(String name) {
        return Enum.valueOf(Gender.class, name);
    }

    public static Gender fromString(String value) {
        return switch (value.toLowerCase()) {
            case "female" -> FEMALE;
            default -> MALE;
        };
    }

    private static /* synthetic */ Gender[] $values() {
        return new Gender[]{MALE, FEMALE};
    }

    static {
        $VALUES = Gender.$values();
    }
}

