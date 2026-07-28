/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  net.minecraft.world.item.DyeColor
 */
package org.millenaire.block;

import net.minecraft.world.item.DyeColor;
import org.millenaire.block.PaintedBrickBlock;

public interface IPaintedBlock {
    public DyeColor getColor();

    public PaintedBrickBlock.BrickType getBrickType();
}

