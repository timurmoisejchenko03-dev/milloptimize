/*
 * Decompiled with CFR 0.150.
 */
package org.millenaire.config;

public final class NavDriverType
extends Enum<NavDriverType> {
    public static final /* enum */ NavDriverType WAYPOINT = new NavDriverType();
    public static final /* enum */ NavDriverType LOCAL_RECOVERY = new NavDriverType();
    private static final /* synthetic */ NavDriverType[] $VALUES;

    public static NavDriverType[] values() {
        return (NavDriverType[])$VALUES.clone();
    }

    public static NavDriverType valueOf(String name) {
        return Enum.valueOf(NavDriverType.class, name);
    }

    private static /* synthetic */ NavDriverType[] $values() {
        return new NavDriverType[]{WAYPOINT, LOCAL_RECOVERY};
    }

    static {
        $VALUES = NavDriverType.$values();
    }
}

