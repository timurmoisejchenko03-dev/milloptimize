/*
 * Decompiled with CFR 0.150.
 * 
 * Could not load the following classes:
 *  net.minecraft.util.RandomSource
 *  net.minecraft.world.item.DyeColor
 */
package org.millenaire.village;

import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.DyeColor;

public record BrickColourTheme(String name, int weight, Map<DyeColor, List<WeightedColor>> colorPools) {
    public DyeColor rollColor(DyeColor input, RandomSource random) {
        List<WeightedColor> pool = this.colorPools.get((Object)input);
        if (pool == null || pool.isEmpty()) {
            return input;
        }
        int totalWeight = 0;
        for (WeightedColor wc : pool) {
            totalWeight += wc.weight();
        }
        int roll = random.nextInt(totalWeight);
        int cumulative = 0;
        for (WeightedColor wc : pool) {
            if (roll >= (cumulative += wc.weight())) continue;
            return wc.color();
        }
        return DyeColor.WHITE;
    }

    public Map<DyeColor, DyeColor> rollBuildingMapping(RandomSource random) {
        EnumMap<DyeColor, DyeColor> mapping = new EnumMap<DyeColor, DyeColor>(DyeColor.class);
        for (DyeColor color : DyeColor.values()) {
            mapping.put(color, this.rollColor(color, random));
        }
        return mapping;
    }

    public record WeightedColor(DyeColor color, int weight) {
    }
}

