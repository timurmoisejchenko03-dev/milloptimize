/*
 * Decompiled with CFR 0.152.
 */
package org.millenaire.village.panel;

public enum PanelType {
    VILLAGE_SUMMARY,
    POPULATION,
    CONSTRUCTIONS,
    PROJECTS,
    CONTROLLED_PROJECTS,
    HOUSE,
    RESOURCES,
    ARCHIVES,
    VILLAGE_MAP,
    MILITARY,
    CONTROLLED_MILITARY,
    INN_TRADE_GOODS,
    INN_VISITORS,
    MARKET_MERCHANTS,
    VISITORS,
    WALLS,
    MARVEL_PROJECTS,
    MARVEL_DONATIONS,
    MARVEL_RESOURCES,
    HALL_OF_FAME,
    CHRONICLE,
    BUILDING_DEFAULT;


    public static PanelType fromName(String name) {
        try {
            return PanelType.valueOf(name);
        }
        catch (IllegalArgumentException e) {
            return VILLAGE_SUMMARY;
        }
    }
}

