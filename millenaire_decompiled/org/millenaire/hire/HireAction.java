/*
 * Decompiled with CFR 0.150.
 */
package org.millenaire.hire;

public final class HireAction
extends Enum<HireAction> {
    public static final /* enum */ HireAction HIRE = new HireAction();
    public static final /* enum */ HireAction EXTEND = new HireAction();
    public static final /* enum */ HireAction RELEASE = new HireAction();
    private static final /* synthetic */ HireAction[] $VALUES;

    public static HireAction[] values() {
        return (HireAction[])$VALUES.clone();
    }

    public static HireAction valueOf(String name) {
        return Enum.valueOf(HireAction.class, name);
    }

    private static /* synthetic */ HireAction[] $values() {
        return new HireAction[]{HIRE, EXTEND, RELEASE};
    }

    static {
        $VALUES = HireAction.$values();
    }
}

