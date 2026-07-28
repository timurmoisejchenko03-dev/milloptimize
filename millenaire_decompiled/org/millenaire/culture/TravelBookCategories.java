/*
 * Decompiled with CFR 0.150.
 */
package org.millenaire.culture;

import java.util.List;
import java.util.Map;

public record TravelBookCategories(List<String> villagerCategories, List<String> buildingCategories, List<String> tradeGoodCategories, Map<String, String> categoryIcons, Map<String, String> categoryNames) {
    public static final TravelBookCategories EMPTY = new TravelBookCategories(List.of(), List.of(), List.of(), Map.of(), Map.of());
}

