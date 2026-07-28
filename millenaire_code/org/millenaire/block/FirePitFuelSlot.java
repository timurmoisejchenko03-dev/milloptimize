/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  net.minecraft.world.Container
 *  net.minecraft.world.inventory.Slot
 *  net.minecraft.world.item.ItemStack
 *  net.minecraft.world.item.Items
 *  net.minecraft.world.level.block.entity.AbstractFurnaceBlockEntity
 */
package org.millenaire.block;

import net.minecraft.world.Container;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.entity.AbstractFurnaceBlockEntity;

public class FirePitFuelSlot
extends Slot {
    public FirePitFuelSlot(Container container, int slot, int x, int y) {
        super(container, slot, x, y);
    }

    public boolean mayPlace(ItemStack stack) {
        return AbstractFurnaceBlockEntity.isFuel((ItemStack)stack) || FirePitFuelSlot.isBucket(stack);
    }

    public int getMaxStackSize(ItemStack stack) {
        return FirePitFuelSlot.isBucket(stack) ? 1 : super.getMaxStackSize(stack);
    }

    private static boolean isBucket(ItemStack stack) {
        return stack.is(Items.BUCKET);
    }
}

