/*
 * Decompiled with CFR 0.150.
 * 
 * Could not load the following classes:
 *  net.minecraft.core.BlockPos
 *  net.minecraft.server.level.ServerLevel
 *  net.minecraft.util.RandomSource
 *  net.minecraft.world.level.block.Block
 *  net.minecraft.world.level.block.Blocks
 *  net.minecraft.world.level.block.state.BlockState
 *  net.minecraft.world.level.block.state.properties.Property
 */
package org.millenaire.world;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.Property;
import org.millenaire.block.AppleTreeLeavesBlock;
import org.millenaire.block.ModBlocks;
import org.millenaire.world.AbstractTreeGenerator;

public final class AppleTreeGenerator
extends AbstractTreeGenerator {
    private static final AppleTreeGenerator INSTANCE = new AppleTreeGenerator();

    private AppleTreeGenerator() {
    }

    @Override
    protected Block getLogBlock() {
        return Blocks.OAK_LOG;
    }

    @Override
    protected BlockState getLeavesState() {
        return (BlockState)((BlockState)((AppleTreeLeavesBlock)((Object)ModBlocks.APPLE_TREE_LEAVES.get())).defaultBlockState().setValue((Property)AppleTreeLeavesBlock.AGE, (Comparable)Integer.valueOf(0))).setValue((Property)AppleTreeLeavesBlock.PERSISTENT, (Comparable)Boolean.valueOf(true));
    }

    public static boolean generate(ServerLevel level, BlockPos position, RandomSource rand) {
        return INSTANCE.doGenerate(level, position, rand);
    }
}

