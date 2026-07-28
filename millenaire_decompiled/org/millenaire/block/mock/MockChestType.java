/*
 * Decompiled with CFR 0.150.
 * 
 * Could not load the following classes:
 *  net.minecraft.util.StringRepresentable
 */
package org.millenaire.block.mock;

import net.minecraft.util.StringRepresentable;

public final class MockChestType
extends Enum<MockChestType>
implements StringRepresentable {
    public static final /* enum */ MockChestType MAIN = new MockChestType("main");
    public static final /* enum */ MockChestType LOCKED = new MockChestType("locked");
    private final String serializedName;
    private static final /* synthetic */ MockChestType[] $VALUES;

    public static MockChestType[] values() {
        return (MockChestType[])$VALUES.clone();
    }

    public static MockChestType valueOf(String name) {
        return Enum.valueOf(MockChestType.class, name);
    }

    private MockChestType(String serializedName) {
        this.serializedName = serializedName;
    }

    public String getSerializedName() {
        return this.serializedName;
    }

    private static /* synthetic */ MockChestType[] $values() {
        return new MockChestType[]{MAIN, LOCKED};
    }

    static {
        $VALUES = MockChestType.$values();
    }
}

