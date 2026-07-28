/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  net.minecraft.core.BlockPos
 *  net.minecraft.server.level.ServerLevel
 *  net.minecraft.util.RandomSource
 *  net.minecraft.world.InteractionResult
 *  net.minecraft.world.entity.player.Player
 *  net.minecraft.world.item.ItemStack
 *  net.minecraft.world.level.ItemLike
 *  net.minecraft.world.level.Level
 *  net.minecraft.world.level.block.Block
 *  net.minecraft.world.level.block.LeavesBlock
 *  net.minecraft.world.level.block.state.BlockBehaviour$Properties
 *  net.minecraft.world.level.block.state.BlockState
 *  net.minecraft.world.level.block.state.StateDefinition$Builder
 *  net.minecraft.world.level.block.state.properties.IntegerProperty
 *  net.minecraft.world.level.block.state.properties.Property
 *  net.minecraft.world.phys.BlockHitResult
 */
package org.millenaire.block;

import java.util.ArrayList;
import java.util.HashSet;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ItemLike;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.LeavesBlock;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.level.block.state.properties.Property;
import net.minecraft.world.phys.BlockHitResult;
import org.millenaire.item.ModItems;

public class OliveTreeLeavesBlock
extends LeavesBlock {
    public static final int MAX_AGE = 3;
    public static final IntegerProperty AGE = IntegerProperty.create((String)"age", (int)0, (int)3);

    public OliveTreeLeavesBlock(BlockBehaviour.Properties properties) {
        super(properties);
        this.registerDefaultState((BlockState)((BlockState)((BlockState)((BlockState)((BlockState)this.stateDefinition.any()).setValue((Property)AGE, (Comparable)Integer.valueOf(0))).setValue((Property)DISTANCE, (Comparable)Integer.valueOf(7))).setValue((Property)PERSISTENT, (Comparable)Boolean.valueOf(false))).setValue((Property)WATERLOGGED, (Comparable)Boolean.valueOf(false)));
    }

    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        super.createBlockStateDefinition(builder);
        builder.add(new Property[]{AGE});
    }

    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hitResult) {
        if ((Integer)state.getValue((Property)AGE) == 3) {
            if (!level.isClientSide) {
                OliveTreeLeavesBlock.popResource((Level)level, (BlockPos)pos.below(), (ItemStack)new ItemStack((ItemLike)ModItems.OLIVES.get()));
                level.setBlock(pos, (BlockState)state.setValue((Property)AGE, (Comparable)Integer.valueOf(0)), 2);
            }
            return InteractionResult.SUCCESS;
        }
        return InteractionResult.PASS;
    }

    protected void randomTick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
        int currentAge;
        super.randomTick(state, level, pos, random);
        long worldTime = level.getDayTime() % 24000L;
        int targetAge = 0;
        if (worldTime > 3000L && worldTime < 5000L) {
            targetAge = 1;
        } else if (worldTime > 5000L && worldTime < 6000L) {
            targetAge = 2;
        } else if (worldTime > 6000L && worldTime < 10000L) {
            targetAge = 3;
        }
        int validCurrentAge = targetAge - 1;
        if (validCurrentAge < 0) {
            validCurrentAge = 3;
        }
        if ((currentAge = ((Integer)state.getValue((Property)AGE)).intValue()) == validCurrentAge) {
            ArrayList<BlockPos> toTest = new ArrayList<BlockPos>();
            HashSet<BlockPos> visited = new HashSet<BlockPos>();
            toTest.add(pos);
            int count = 0;
            while (!toTest.isEmpty() && count < 10000) {
                BlockPos p = (BlockPos)toTest.remove(toTest.size() - 1);
                if (!visited.add(p)) {
                    ++count;
                    continue;
                }
                if (!level.isLoaded(p)) {
                    ++count;
                    continue;
                }
                BlockState bs = level.getBlockState(p);
                if (bs.getBlock() == this && (Integer)bs.getValue((Property)AGE) == validCurrentAge) {
                    level.setBlock(p, (BlockState)bs.setValue((Property)AGE, (Comparable)Integer.valueOf(targetAge)), 2);
                    for (int dx = -1; dx < 2; ++dx) {
                        for (int dy = -1; dy < 2; ++dy) {
                            for (int dz = -1; dz < 2; ++dz) {
                                toTest.add(p.offset(dx, dy, dz));
                            }
                        }
                    }
                }
                ++count;
            }
        }
    }

    public boolean isRandomlyTicking(BlockState state) {
        return true;
    }
}

