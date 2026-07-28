/*
 * Decompiled with CFR 0.150.
 */
package org.millenaire.entity;

public final class ModelType
extends Enum<ModelType> {
    public static final /* enum */ ModelType MALE = new ModelType("male");
    public static final /* enum */ ModelType FEMALE_SYM = new ModelType("female_symmetrical");
    public static final /* enum */ ModelType FEMALE_ASYM = new ModelType("female_asymmetrical");
    private final String serializedName;
    private static final /* synthetic */ ModelType[] $VALUES;

    public static ModelType[] values() {
        return (ModelType[])$VALUES.clone();
    }

    public static ModelType valueOf(String name) {
        return Enum.valueOf(ModelType.class, name);
    }

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

    private static /* synthetic */ ModelType[] $values() {
        return new ModelType[]{MALE, FEMALE_SYM, FEMALE_ASYM};
    }

    static {
        $VALUES = ModelType.$values();
    }
}

