/*
 * Decompiled with CFR 0.150.
 */
package org.millenaire.goal;

public final class TravelPhase
extends Enum<TravelPhase> {
    public static final /* enum */ TravelPhase TRAVELLING = new TravelPhase();
    public static final /* enum */ TravelPhase AT_DESTINATION = new TravelPhase();
    private static final /* synthetic */ TravelPhase[] $VALUES;

    public static TravelPhase[] values() {
        return (TravelPhase[])$VALUES.clone();
    }

    public static TravelPhase valueOf(String name) {
        return Enum.valueOf(TravelPhase.class, name);
    }

    private static /* synthetic */ TravelPhase[] $values() {
        return new TravelPhase[]{TRAVELLING, AT_DESTINATION};
    }

    static {
        $VALUES = TravelPhase.$values();
    }
}

