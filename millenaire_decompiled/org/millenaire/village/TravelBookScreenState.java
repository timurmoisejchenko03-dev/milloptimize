/*
 * Decompiled with CFR 0.150.
 */
package org.millenaire.village;

public final class TravelBookScreenState
extends Enum<TravelBookScreenState> {
    public static final /* enum */ TravelBookScreenState HOME = new TravelBookScreenState();
    public static final /* enum */ TravelBookScreenState CULTURE = new TravelBookScreenState();
    public static final /* enum */ TravelBookScreenState VILLAGERS_LIST = new TravelBookScreenState();
    public static final /* enum */ TravelBookScreenState VILLAGER_DETAIL = new TravelBookScreenState();
    public static final /* enum */ TravelBookScreenState VILLAGES_LIST = new TravelBookScreenState();
    public static final /* enum */ TravelBookScreenState VILLAGE_DETAIL = new TravelBookScreenState();
    public static final /* enum */ TravelBookScreenState BUILDINGS_LIST = new TravelBookScreenState();
    public static final /* enum */ TravelBookScreenState BUILDING_DETAIL = new TravelBookScreenState();
    public static final /* enum */ TravelBookScreenState TRADE_GOODS_LIST = new TravelBookScreenState();
    public static final /* enum */ TravelBookScreenState TRADE_GOOD_DETAIL = new TravelBookScreenState();
    private static final /* synthetic */ TravelBookScreenState[] $VALUES;

    public static TravelBookScreenState[] values() {
        return (TravelBookScreenState[])$VALUES.clone();
    }

    public static TravelBookScreenState valueOf(String name) {
        return Enum.valueOf(TravelBookScreenState.class, name);
    }

    private static /* synthetic */ TravelBookScreenState[] $values() {
        return new TravelBookScreenState[]{HOME, CULTURE, VILLAGERS_LIST, VILLAGER_DETAIL, VILLAGES_LIST, VILLAGE_DETAIL, BUILDINGS_LIST, BUILDING_DETAIL, TRADE_GOODS_LIST, TRADE_GOOD_DETAIL};
    }

    static {
        $VALUES = TravelBookScreenState.$values();
    }
}

