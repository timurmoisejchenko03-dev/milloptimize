/*
 * Decompiled with CFR 0.150.
 * 
 * Could not load the following classes:
 *  javax.annotation.Nullable
 *  net.minecraft.world.item.Item
 *  net.minecraft.world.item.ItemStack
 *  net.minecraft.world.level.ItemLike
 *  net.minecraft.world.level.block.state.BlockState
 */
package org.millenaire.tool;

import java.util.List;
import java.util.function.Predicate;
import javax.annotation.Nullable;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ItemLike;
import net.minecraft.world.level.block.state.BlockState;

public record ToolCategory(String id, List<ToolEntry> items) {
    @Nullable
    public ToolEntry getBestOwned(Predicate<Item> hasItem) {
        for (ToolEntry entry : this.items) {
            if (entry.item == null || !hasItem.test(entry.item)) continue;
            return entry;
        }
        return null;
    }

    public float getBestDestroySpeed(Predicate<Item> hasItem, BlockState testBlock, Item fallback) {
        ToolEntry best = this.getBestOwned(hasItem);
        Item tool = best != null && best.item() != null ? best.item() : fallback;
        return new ItemStack((ItemLike)tool).getDestroySpeed(testBlock);
    }

    @Nullable
    public ToolEntry findUpgrade(@Nullable ToolEntry bestOwned, Predicate<Item> hasStock) {
        for (ToolEntry entry : this.items) {
            if (entry.item == null) continue;
            if (bestOwned != null && entry.equals(bestOwned)) break;
            if (!hasStock.test(entry.item)) continue;
            return entry;
        }
        return null;
    }

    public record ToolEntry(String itemId, @Nullable Item item, int priority) {
    }
}

