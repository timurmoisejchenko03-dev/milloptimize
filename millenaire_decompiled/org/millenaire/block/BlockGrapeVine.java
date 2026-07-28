/*
 * Decompiled with CFR 0.150.
 * 
 * Could not load the following classes:
 *  com.mojang.serialization.MapCodec
 *  net.minecraft.core.BlockPos
 *  net.minecraft.server.level.ServerLevel
 *  net.minecraft.util.RandomSource
 *  net.minecraft.world.level.BlockGetter
 *  net.minecraft.world.level.Level
 *  net.minecraft.world.level.LevelReader
 *  net.minecraft.world.level.block.Block
 *  net.minecraft.world.level.block.Blocks
 *  net.minecraft.world.level.block.BonemealableBlock
 *  net.minecraft.world.level.block.BushBlock
 *  net.minecraft.world.level.block.FarmBlock
 *  net.minecraft.world.level.block.state.BlockBehaviour$Properties
 *  net.minecraft.world.level.block.state.BlockState
 *  net.minecraft.world.level.block.state.StateDefinition$Builder
 *  net.minecraft.world.level.block.state.properties.BlockStateProperties
 *  net.minecraft.world.level.block.state.properties.EnumProperty
 *  net.minecraft.world.level.block.state.properties.Half
 *  net.minecraft.world.level.block.state.properties.IntegerProperty
 *  net.minecraft.world.level.block.state.properties.Property
 *  net.minecraft.world.phys.shapes.CollisionContext
 *  net.minecraft.world.phys.shapes.VoxelShape
 */
package org.millenaire.block;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.BonemealableBlock;
import net.minecraft.world.level.block.BushBlock;
import net.minecraft.world.level.block.FarmBlock;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.level.block.state.properties.Half;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.level.block.state.properties.Property;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

public class BlockGrapeVine
extends BushBlock
implements BonemealableBlock {
    public static final MapCodec<BlockGrapeVine> CODEC = BlockGrapeVine.simpleCodec(BlockGrapeVine::new);
    public static final IntegerProperty AGE = IntegerProperty.create((String)"age", (int)0, (int)7);
    public static final EnumProperty<Half> HALF = BlockStateProperties.HALF;
    private static final VoxelShape SHAPE_LOWER = Block.box((double)2.0, (double)0.0, (double)2.0, (double)14.0, (double)16.0, (double)14.0);
    private static final VoxelShape SHAPE_UPPER = Block.box((double)2.0, (double)0.0, (double)2.0, (double)14.0, (double)12.0, (double)14.0);

    public BlockGrapeVine(BlockBehaviour.Properties properties) {
        super(properties);
        this.registerDefaultState((BlockState)((BlockState)((BlockState)this.stateDefinition.any()).setValue((Property)AGE, (Comparable)Integer.valueOf(0))).setValue(HALF, (Comparable)Half.BOTTOM));
    }

    protected MapCodec<? extends BushBlock> codec() {
        return CODEC;
    }

    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(new Property[]{AGE, HALF});
    }

    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return state.getValue(HALF) == Half.BOTTOM ? SHAPE_LOWER : SHAPE_UPPER;
    }

    protected boolean mayPlaceOn(BlockState groundState, BlockGetter level, BlockPos pos) {
        return groundState.getBlock() instanceof FarmBlock;
    }

    protected boolean isRandomlyTicking(BlockState state) {
        return state.getValue(HALF) == Half.BOTTOM && (Integer)state.getValue((Property)AGE) < 7;
    }

    protected boolean canSurvive(BlockState state, LevelReader level, BlockPos pos) {
        if (state.getValue(HALF) == Half.BOTTOM) {
            BlockState below = level.getBlockState(pos.below());
            return below.getBlock() instanceof FarmBlock;
        }
        BlockState below = level.getBlockState(pos.below());
        return below.is((Block)this) && below.getValue(HALF) == Half.BOTTOM;
    }

    protected void randomTick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
        if (state.getValue(HALF) != Half.BOTTOM) {
            return;
        }
        int age = (Integer)state.getValue((Property)AGE);
        if (age >= 7) {
            return;
        }
        if (random.nextInt(5) == 0) {
            int newAge = age + 1;
            level.setBlock(pos, (BlockState)state.setValue((Property)AGE, (Comparable)Integer.valueOf(newAge)), 2);
            BlockPos upperPos = pos.above();
            BlockState upperState = level.getBlockState(upperPos);
            if (newAge >= 2) {
                if (upperState.is((Block)this) && upperState.getValue(HALF) == Half.TOP) {
                    level.setBlock(upperPos, (BlockState)upperState.setValue((Property)AGE, (Comparable)Integer.valueOf(newAge)), 2);
                } else if (upperState.isAir()) {
                    level.setBlock(upperPos, (BlockState)((BlockState)this.defaultBlockState().setValue(HALF, (Comparable)Half.TOP)).setValue((Property)AGE, (Comparable)Integer.valueOf(newAge)), 2);
                }
            }
        }
    }

    protected void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState, boolean isMoving) {
        super.onRemove(state, level, pos, newState, isMoving);
        if (state.is(newState.getBlock())) {
            return;
        }
        if (state.getValue(HALF) == Half.BOTTOM) {
            BlockPos upperPos = pos.above();
            BlockState upperState = level.getBlockState(upperPos);
            if (upperState.is((Block)this) && upperState.getValue(HALF) == Half.TOP) {
                level.setBlock(upperPos, Blocks.AIR.defaultBlockState(), 3);
            }
        } else {
            BlockPos lowerPos = pos.below();
            BlockState lowerState = level.getBlockState(lowerPos);
            if (lowerState.is((Block)this) && lowerState.getValue(HALF) == Half.BOTTOM) {
                level.setBlock(lowerPos, Blocks.AIR.defaultBlockState(), 3);
            }
        }
    }

    public boolean isValidBonemealTarget(LevelReader level, BlockPos pos, BlockState state) {
        return (Integer)state.getValue((Property)AGE) < 7;
    }

    public boolean isBonemealSuccess(Level level, RandomSource random, BlockPos pos, BlockState state) {
        return true;
    }

    public void performBonemeal(ServerLevel level, RandomSource random, BlockPos pos, BlockState state) {
        int newAge = Math.min((Integer)state.getValue((Property)AGE) + random.nextIntBetweenInclusive(2, 5), 7);
        if (state.getValue(HALF) == Half.TOP) {
            BlockPos lowerPos = pos.below();
            BlockState lowerState = level.getBlockState(lowerPos);
            if (lowerState.is((Block)this) && lowerState.getValue(HALF) == Half.BOTTOM) {
                level.setBlock(lowerPos, (BlockState)lowerState.setValue((Property)AGE, (Comparable)Integer.valueOf(newAge)), 2);
            }
            level.setBlock(pos, (BlockState)state.setValue((Property)AGE, (Comparable)Integer.valueOf(newAge)), 2);
        } else {
            level.setBlock(pos, (BlockState)state.setValue((Property)AGE, (Comparable)Integer.valueOf(newAge)), 2);
            BlockPos upperPos = pos.above();
            BlockState upperState = level.getBlockState(upperPos);
            if (upperState.is((Block)this) && upperState.getValue(HALF) == Half.TOP) {
                level.setBlock(upperPos, (BlockState)upperState.setValue((Property)AGE, (Comparable)Integer.valueOf(newAge)), 2);
            } else if (newAge >= 2 && upperState.isAir()) {
                level.setBlock(upperPos, (BlockState)((BlockState)this.defaultBlockState().setValue(HALF, (Comparable)Half.TOP)).setValue((Property)AGE, (Comparable)Integer.valueOf(newAge)), 2);
            }
        }
    }
}

