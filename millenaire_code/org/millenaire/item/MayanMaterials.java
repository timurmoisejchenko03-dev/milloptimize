/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  net.minecraft.tags.BlockTags
 *  net.minecraft.world.item.Items
 *  net.minecraft.world.item.Tier
 *  net.minecraft.world.item.crafting.Ingredient
 *  net.minecraft.world.level.ItemLike
 *  net.neoforged.neoforge.common.SimpleTier
 */
package org.millenaire.item;

import net.minecraft.tags.BlockTags;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.Tier;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.level.ItemLike;
import net.neoforged.neoforge.common.SimpleTier;

public final class MayanMaterials {
    public static final Tier OBSIDIAN_TOOL = new SimpleTier(BlockTags.INCORRECT_FOR_DIAMOND_TOOL, 1561, 6.0f, 2.0f, 25, () -> Ingredient.of((ItemLike[])new ItemLike[]{Items.DIAMOND}));
    public static final Tier IRON_TOOL = new SimpleTier(BlockTags.INCORRECT_FOR_IRON_TOOL, 250, 6.0f, 2.0f, 14, () -> Ingredient.of((ItemLike[])new ItemLike[]{Items.IRON_INGOT}));

    private MayanMaterials() {
    }
}

