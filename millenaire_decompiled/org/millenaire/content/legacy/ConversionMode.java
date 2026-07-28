/*
 * Decompiled with CFR 0.150.
 */
package org.millenaire.content.legacy;

public final class ConversionMode
extends Enum<ConversionMode> {
    public static final /* enum */ ConversionMode AUTO = new ConversionMode();
    public static final /* enum */ ConversionMode CONVERT = new ConversionMode();
    private static final /* synthetic */ ConversionMode[] $VALUES;

    public static ConversionMode[] values() {
        return (ConversionMode[])$VALUES.clone();
    }

    public static ConversionMode valueOf(String name) {
        return Enum.valueOf(ConversionMode.class, name);
    }

    public boolean isStrict() {
        return this == CONVERT;
    }

    private static /* synthetic */ ConversionMode[] $values() {
        return new ConversionMode[]{AUTO, CONVERT};
    }

    static {
        $VALUES = ConversionMode.$values();
    }
}

