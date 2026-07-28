/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  net.minecraft.core.BlockPos
 *  net.minecraft.server.level.ServerLevel
 *  net.minecraft.util.RandomSource
 *  net.minecraft.world.level.block.Block
 *  net.minecraft.world.level.block.state.BlockBehaviour$Properties
 *  net.minecraft.world.level.block.state.BlockState
 *  net.minecraft.world.level.block.state.StateDefinition$Builder
 *  net.minecraft.world.level.block.state.properties.IntegerProperty
 *  net.minecraft.world.level.block.state.properties.Property
 */
package org.millenaire.block;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.level.block.state.properties.Property;
import org.millenaire.block.ModBlocks;

public class BlockWetBrick
extends Block {
    public static final IntegerProperty AGE = IntegerProperty.create((String)"age", (int)0, (int)2);
    private static final int MIN_LIGHT = 15;

    public BlockWetBrick(BlockBehaviour.Properties properties) {
        super(properties);
        this.registerDefaultState((BlockState)((BlockState)this.stateDefinition.any()).setValue((Property)AGE, (Comparable)Integer.valueOf(0)));
    }

    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(new Property[]{AGE});
    }

    public boolean isRandomlyTicking(BlockState state) {
        return true;
    }

    protected void randomTick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
        if (level.getMaxLocalRawBrightness(pos.above()) < 15) {
            return;
        }
        int age = (Integer)state.getValue((Property)AGE);
        if (age >= 2) {
            level.setBlock(pos, ((Block)ModBlocks.MUD_BRICK.get()).defaultBlockState(), 2);
            return;
        }
        int newAge = random.nextBoolean() ? age + 2 : age + 1;
        level.setBlock(pos, (BlockState)state.setValue((Property)AGE, (Comparable)Integer.valueOf(Math.min(newAge, 2))), 2);
    }
}

