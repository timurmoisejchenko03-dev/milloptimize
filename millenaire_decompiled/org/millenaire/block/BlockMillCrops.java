/*
 * Decompiled with CFR 0.150.
 * 
 * Could not load the following classes:
 *  net.minecraft.core.BlockPos
 *  net.minecraft.server.level.ServerLevel
 *  net.minecraft.util.RandomSource
 *  net.minecraft.world.level.BlockGetter
 *  net.minecraft.world.level.block.CropBlock
 *  net.minecraft.world.level.block.FarmBlock
 *  net.minecraft.world.level.block.state.BlockBehaviour$Properties
 *  net.minecraft.world.level.block.state.BlockState
 *  net.minecraft.world.level.block.state.properties.Property
 */
package org.millenaire.block;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.CropBlock;
import net.minecraft.world.level.block.FarmBlock;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.Property;

public class BlockMillCrops
extends CropBlock {
    private final boolean requireIrrigation;
    private final boolean slowGrowth;

    public BlockMillCrops(boolean requireIrrigation, boolean slowGrowth, BlockBehaviour.Properties properties) {
        super(properties);
        this.requireIrrigation = requireIrrigation;
        this.slowGrowth = slowGrowth;
    }

    public boolean requiresIrrigation() {
        return this.requireIrrigation;
    }

    public boolean isSlowGrowth() {
        return this.slowGrowth;
    }

    public void randomTick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
        BlockState below;
        if (this.requireIrrigation && (below = level.getBlockState(pos.below())).getBlock() instanceof FarmBlock && (Integer)below.getValue((Property)FarmBlock.MOISTURE) == 0) {
            return;
        }
        if (this.slowGrowth && random.nextBoolean()) {
            return;
        }
        super.randomTick(state, level, pos, random);
    }

    protected boolean mayPlaceOn(BlockState state, BlockGetter level, BlockPos pos) {
        return state.getBlock() instanceof FarmBlock;
    }
}

