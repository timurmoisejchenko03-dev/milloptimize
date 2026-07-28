/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  net.minecraft.world.item.Item
 */
package org.millenaire.combat.raid;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import net.minecraft.world.item.Item;

public final class RaidLootPlanner {
    private RaidLootPlanner() {
    }

    public static Map<Item, Integer> plan(List<GoodNeed> needs, Map<Item, Integer> targetContents, int budget) {
        LinkedHashMap<Item, Integer> toSteal = new LinkedHashMap<Item, Integer>();
        int remaining = budget;
        for (GoodNeed need : needs) {
            int take;
            int present;
            if (remaining <= 0) break;
            int demand = need.targetQuantity();
            if (demand <= 0 || (present = targetContents.getOrDefault(need.item(), 0).intValue()) <= 0 || (take = Math.min(demand, Math.min(remaining, present))) <= 0) continue;
            toSteal.merge(need.item(), take, Integer::sum);
            remaining -= take;
        }
        return toSteal;
    }

    public record GoodNeed(Item item, int targetQuantity) {
    }
}

