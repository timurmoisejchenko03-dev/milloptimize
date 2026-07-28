/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  net.minecraft.world.item.DyeColor
 *  net.minecraft.world.level.block.StairBlock
 *  net.minecraft.world.level.block.state.BlockBehaviour$Properties
 *  net.minecraft.world.level.block.state.BlockState
 */
package org.millenaire.block;

import net.minecraft.world.item.DyeColor;
import net.minecraft.world.level.block.StairBlock;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import org.millenaire.block.IPaintedBlock;
import org.millenaire.block.PaintedBrickBlock;

public class PaintedBrickStairBlock
extends StairBlock
implements IPaintedBlock {
    private final DyeColor color;

    public PaintedBrickStairBlock(DyeColor color, BlockState baseState, BlockBehaviour.Properties properties) {
        super(baseState, properties);
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

