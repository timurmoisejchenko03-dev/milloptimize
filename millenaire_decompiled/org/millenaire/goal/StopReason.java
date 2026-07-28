/*
 * Decompiled with CFR 0.150.
 */
package org.millenaire.goal;

public final class StopReason
extends Enum<StopReason> {
    public static final /* enum */ StopReason COMPLETED = new StopReason();
    public static final /* enum */ StopReason INTERRUPTED = new StopReason();
    public static final /* enum */ StopReason IMPOSSIBLE = new StopReason();
    private static final /* synthetic */ StopReason[] $VALUES;

    public static StopReason[] values() {
        return (StopReason[])$VALUES.clone();
    }

    public static StopReason valueOf(String name) {
        return Enum.valueOf(StopReason.class, name);
    }

    private static /* synthetic */ StopReason[] $values() {
        return new StopReason[]{COMPLETED, INTERRUPTED, IMPOSSIBLE};
    }

    static {
        $VALUES = StopReason.$values();
    }
}

