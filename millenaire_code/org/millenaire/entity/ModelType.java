/*
 * Decompiled with CFR 0.152.
 */
package org.millenaire.entity;

public enum ModelType {
    MALE("male"),
    FEMALE_SYM("female_symmetrical"),
    FEMALE_ASYM("female_asymmetrical");

    private final String serializedName;

    private ModelType(String serializedName) {
        this.serializedName = serializedName;
    }

    public String getSerializedName() {
        return this.serializedName;
    }

    public static ModelType fromString(String name) {
        for (ModelType type : ModelType.values()) {
            if (!type.serializedName.equals(name)) continue;
            return type;
        }
        return MALE;
    }

    public byte toByte() {
        return (byte)this.ordinal();
    }

    public static ModelType fromByte(byte b) {
        ModelType[] values = ModelType.values();
        return b >= 0 && b < values.length ? values[b] : MALE;
    }
}

