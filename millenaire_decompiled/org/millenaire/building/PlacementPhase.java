/*
 * Decompiled with CFR 0.150.
 */
package org.millenaire.building;

public final class PlacementPhase
extends Enum<PlacementPhase> {
    public static final /* enum */ PlacementPhase DELETION = new PlacementPhase();
    public static final /* enum */ PlacementPhase STRUCTURE = new PlacementPhase();
    public static final /* enum */ PlacementPhase DEPENDENT = new PlacementPhase();
    public static final /* enum */ PlacementPhase SPECIAL = new PlacementPhase();
    private static final /* synthetic */ PlacementPhase[] $VALUES;

    public static PlacementPhase[] values() {
        return (PlacementPhase[])$VALUES.clone();
    }

    public static PlacementPhase valueOf(String name) {
        return Enum.valueOf(PlacementPhase.class, name);
    }

    private static /* synthetic */ PlacementPhase[] $values() {
        return new PlacementPhase[]{DELETION, STRUCTURE, DEPENDENT, SPECIAL};
    }

    static {
        $VALUES = PlacementPhase.$values();
    }
}

