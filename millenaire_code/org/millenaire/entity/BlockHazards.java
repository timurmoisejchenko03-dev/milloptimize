/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  net.minecraft.core.BlockPos
 *  net.minecraft.world.level.BlockGetter
 *  net.minecraft.world.level.block.Blocks
 *  net.minecraft.world.level.block.CampfireBlock
 *  net.minecraft.world.level.block.state.BlockState
 *  net.minecraft.world.level.block.state.properties.Property
 */
package org.millenaire.entity;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.CampfireBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.Property;

public final class BlockHazards {
    private BlockHazards() {
    }

    public static boolean isHazardous(BlockState state) {
        if (state.is(Blocks.FIRE)) {
            return true;
        }
        if (state.is(Blocks.SOUL_FIRE)) {
            return true;
        }
        if (state.is(Blocks.LAVA)) {
            return true;
        }
        if (state.is(Blocks.MAGMA_BLOCK)) {
            return true;
        }
        if (state.is(Blocks.CACTUS)) {
            return true;
        }
        if (state.is(Blocks.WITHER_ROSE)) {
            return true;
        }
        if (state.is(Blocks.POWDER_SNOW)) {
            return true;
        }
        return state.getBlock() instanceof CampfireBlock && state.hasProperty((Property)CampfireBlock.LIT) && (Boolean)state.getValue((Property)CampfireBlock.LIT) != false;
    }

    public static boolean isHazardousAt(BlockGetter level, BlockPos feet) {
        return BlockHazards.isHazardous(level.getBlockState(feet)) || BlockHazards.isHazardous(level.getBlockState(feet.below()));
    }
}

