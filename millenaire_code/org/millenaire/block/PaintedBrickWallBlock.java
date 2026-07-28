/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  net.minecraft.world.item.DyeColor
 *  net.minecraft.world.level.block.WallBlock
 *  net.minecraft.world.level.block.state.BlockBehaviour$Properties
 */
package org.millenaire.block;

import net.minecraft.world.item.DyeColor;
import net.minecraft.world.level.block.WallBlock;
import net.minecraft.world.level.block.state.BlockBehaviour;
import org.millenaire.block.IPaintedBlock;
import org.millenaire.block.PaintedBrickBlock;

public class PaintedBrickWallBlock
extends WallBlock
implements IPaintedBlock {
    private final DyeColor color;

    public PaintedBrickWallBlock(DyeColor color, BlockBehaviour.Properties properties) {
        super(properties);
        this.color = color;
    }

    @Override
    public DyeColor getColor() {
        return this.color;
    }

    @Override
    public PaintedBrickBlock.BrickType getBrickType() {
        return PaintedBrickBlock.BrickType.PLAIN;
    }
}

