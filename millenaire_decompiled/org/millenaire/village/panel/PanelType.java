/*
 * Decompiled with CFR 0.150.
 */
package org.millenaire.village.panel;

public final class PanelType
extends Enum<PanelType> {
    public static final /* enum */ PanelType VILLAGE_SUMMARY = new PanelType();
    public static final /* enum */ PanelType POPULATION = new PanelType();
    public static final /* enum */ PanelType CONSTRUCTIONS = new PanelType();
    public static final /* enum */ PanelType PROJECTS = new PanelType();
    public static final /* enum */ PanelType CONTROLLED_PROJECTS = new PanelType();
    public static final /* enum */ PanelType HOUSE = new PanelType();
    public static final /* enum */ PanelType RESOURCES = new PanelType();
    public static final /* enum */ PanelType ARCHIVES = new PanelType();
    public static final /* enum */ PanelType VILLAGE_MAP = new PanelType();
    public static final /* enum */ PanelType MILITARY = new PanelType();
    public static final /* enum */ PanelType CONTROLLED_MILITARY = new PanelType();
    public static final /* enum */ PanelType INN_TRADE_GOODS = new PanelType();
    public static final /* enum */ PanelType INN_VISITORS = new PanelType();
    public static final /* enum */ PanelType MARKET_MERCHANTS = new PanelType();
    public static final /* enum */ PanelType VISITORS = new PanelType();
    public static final /* enum */ PanelType WALLS = new PanelType();
    public static final /* enum */ PanelType MARVEL_PROJECTS = new PanelType();
    public static final /* enum */ PanelType MARVEL_DONATIONS = new PanelType();
    public static final /* enum */ PanelType MARVEL_RESOURCES = new PanelType();
    public static final /* enum */ PanelType HALL_OF_FAME = new PanelType();
    public static final /* enum */ PanelType CHRONICLE = new PanelType();
    public static final /* enum */ PanelType BUILDING_DEFAULT = new PanelType();
    private static final /* synthetic */ PanelType[] $VALUES;

    public static PanelType[] values() {
        return (PanelType[])$VALUES.clone();
    }

    public static PanelType valueOf(String name) {
        return Enum.valueOf(PanelType.class, name);
    }

    public static PanelType fromName(String name) {
        try {
            return PanelType.valueOf(name);
        }
        catch (IllegalArgumentException e) {
            return VILLAGE_SUMMARY;
        }
    }

    private static /* synthetic */ PanelType[] $values() {
        return new PanelType[]{VILLAGE_SUMMARY, POPULATION, CONSTRUCTIONS, PROJECTS, CONTROLLED_PROJECTS, HOUSE, RESOURCES, ARCHIVES, VILLAGE_MAP, MILITARY, CONTROLLED_MILITARY, INN_TRADE_GOODS, INN_VISITORS, MARKET_MERCHANTS, VISITORS, WALLS, MARVEL_PROJECTS, MARVEL_DONATIONS, MARVEL_RESOURCES, HALL_OF_FAME, CHRONICLE, BUILDING_DEFAULT};
    }

    static {
        $VALUES = PanelType.$values();
    }
}

