/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  net.minecraft.world.item.DyeColor
 *  net.minecraft.world.level.block.Block
 *  net.minecraft.world.level.block.state.BlockBehaviour$Properties
 */
package org.millenaire.block;

import net.minecraft.world.item.DyeColor;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockBehaviour;
import org.millenaire.block.IPaintedBlock;

public class PaintedBrickBlock
extends Block
implements IPaintedBlock {
    private final DyeColor color;
    private final BrickType brickType;

    public PaintedBrickBlock(DyeColor color, BrickType brickType, BlockBehaviour.Properties properties) {
        super(properties);
        this.color = color;
        this.brickType = brickType;
    }

    @Override
    public DyeColor getColor() {
        return this.color;
    }

    @Override
    public BrickType getBrickType() {
        return this.brickType;
    }

    public static enum BrickType {
        PLAIN,
        DECORATED;

    }
}

