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
import org.millenaire.block.ModBlocks;
import org.millenaire.block.OliveTreeLeavesBlock;
import org.millenaire.world.AbstractTreeGenerator;

public final class OliveTreeGenerator
extends AbstractTreeGenerator {
    private static final OliveTreeGenerator INSTANCE = new OliveTreeGenerator();

    private OliveTreeGenerator() {
    }

    @Override
    protected Block getLogBlock() {
        return Blocks.ACACIA_LOG;
    }

    @Override
    protected BlockState getLeavesState() {
        return (BlockState)((BlockState)((OliveTreeLeavesBlock)((Object)ModBlocks.OLIVE_TREE_LEAVES.get())).defaultBlockState().setValue((Property)OliveTreeLeavesBlock.AGE, (Comparable)Integer.valueOf(0))).setValue((Property)OliveTreeLeavesBlock.PERSISTENT, (Comparable)Boolean.valueOf(true));
    }

    public static boolean generate(ServerLevel level, BlockPos position, RandomSource rand) {
        return INSTANCE.doGenerate(level, position, rand);
    }
}

