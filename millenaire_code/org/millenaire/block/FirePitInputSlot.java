/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  net.minecraft.world.Container
 *  net.minecraft.world.inventory.Slot
 *  net.minecraft.world.item.ItemStack
 *  net.minecraft.world.level.Level
 */
package org.millenaire.block;

import net.minecraft.world.Container;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import org.millenaire.block.FirePitBlockEntity;

public class FirePitInputSlot
extends Slot {
    public FirePitInputSlot(Container container, int slot, int x, int y) {
        super(container, slot, x, y);
    }

    public boolean mayPlace(ItemStack stack) {
        FirePitBlockEntity blockEntity;
        Level level;
        Container container = this.container;
        if (container instanceof FirePitBlockEntity && (level = (blockEntity = (FirePitBlockEntity)container).getLevel()) != null) {
            return FirePitBlockEntity.isFirePitBurnable(stack, level);
        }
        return FirePitBlockEntity.isFirePitBurnable(stack);
    }
}

