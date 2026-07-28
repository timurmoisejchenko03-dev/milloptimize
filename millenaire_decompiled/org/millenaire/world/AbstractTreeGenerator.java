/*
 * Decompiled with CFR 0.150.
 * 
 * Could not load the following classes:
 *  net.minecraft.core.BlockPos
 *  net.minecraft.core.Direction
 *  net.minecraft.core.Direction$Plane
 *  net.minecraft.server.level.ServerLevel
 *  net.minecraft.tags.BlockTags
 *  net.minecraft.util.RandomSource
 *  net.minecraft.world.level.block.Block
 *  net.minecraft.world.level.block.Blocks
 *  net.minecraft.world.level.block.state.BlockState
 */
package org.millenaire.world;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.BlockTags;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;

abstract class AbstractTreeGenerator {
    private static final int MIN_TREE_HEIGHT = 5;

    AbstractTreeGenerator() {
    }

    protected abstract Block getLogBlock();

    protected abstract BlockState getLeavesState();

    boolean doGenerate(ServerLevel level, BlockPos position, RandomSource rand) {
        int treeHeight = rand.nextInt(2) + 5;
        if (position.getY() < 1 || position.getY() + treeHeight + 1 > level.getMaxBuildHeight()) {
            return false;
        }
        if (!AbstractTreeGenerator.checkSpace(level, position, treeHeight)) {
            return false;
        }
        BlockState groundState = level.getBlockState(position.below());
        if (!groundState.is(BlockTags.DIRT)) {
            return false;
        }
        if (position.getY() >= level.getMaxBuildHeight() - treeHeight - 1) {
            return false;
        }
        level.setBlock(position, Blocks.AIR.defaultBlockState(), 4);
        BlockState leavesState = this.getLeavesState();
        Block logBlock = this.getLogBlock();
        for (int yPos = 0; yPos < 5; ++yPos) {
            BlockPos trunkPos = position.above(yPos);
            BlockState stateAt = level.getBlockState(trunkPos);
            if (!stateAt.isAir() && !stateAt.is(BlockTags.LEAVES) && !stateAt.is(BlockTags.REPLACEABLE)) continue;
            level.setBlock(trunkPos, logBlock.defaultBlockState(), 3);
        }
        for (Direction facing : Direction.Plane.HORIZONTAL) {
            int branchStartY = 3 + rand.nextInt(1);
            int horizontalSize = 3 - rand.nextInt(2);
            int xPos = position.getX();
            int zPos = position.getZ();
            int yPos = position.getY() + branchStartY;
            int curve = rand.nextBoolean() ? 1 : -1;
            for (int hPos = 0; hPos < horizontalSize; ++hPos) {
                BlockPos branchPos;
                BlockState branchState;
                if (yPos < position.getY() + treeHeight && rand.nextFloat() < 0.7f) {
                    ++yPos;
                }
                if (facing.getStepX() != 0) {
                    xPos += facing.getStepX();
                    if (rand.nextFloat() < 0.15f) {
                        zPos += curve;
                    }
                } else {
                    zPos += facing.getStepZ();
                    if (rand.nextFloat() < 0.15f) {
                        xPos += curve;
                    }
                }
                if (!(branchState = level.getBlockState(branchPos = new BlockPos(xPos, yPos, zPos))).isAir() && !branchState.is(BlockTags.LEAVES)) continue;
                level.setBlock(branchPos, logBlock.defaultBlockState(), 3);
                for (int dx = -1; dx < 2; ++dx) {
                    for (int dz = -1; dz < 2; ++dz) {
                        for (int dy = -1; dy < 2; ++dy) {
                            BlockPos leafPos = branchPos.offset(dx, dy, dz);
                            BlockState leafState = level.getBlockState(leafPos);
                            if (!leafState.isAir() || rand.nextInt(100) >= 50) continue;
                            level.setBlock(leafPos, leavesState, 3);
                        }
                    }
                }
            }
        }
        return true;
    }

    private static boolean checkSpace(ServerLevel level, BlockPos position, int treeHeight) {
        for (int j = position.getY(); j <= position.getY() + 1 + treeHeight; ++j) {
            int k = 1;
            if (j == position.getY()) {
                k = 0;
            }
            if (j >= position.getY() + 1 + treeHeight - 2) {
                k = 2;
            }
            for (int l = position.getX() - k; l <= position.getX() + k; ++l) {
                for (int i1 = position.getZ() - k; i1 <= position.getZ() + k; ++i1) {
                    if (j >= 0 && j < level.getMaxBuildHeight()) {
                        BlockState stateAt;
                        BlockPos checkPos = new BlockPos(l, j, i1);
                        if (checkPos.equals((Object)position) || (stateAt = level.getBlockState(checkPos)).isAir() || stateAt.is(BlockTags.LEAVES) || stateAt.is(BlockTags.LOGS) || stateAt.is(BlockTags.REPLACEABLE)) continue;
                        return false;
                    }
                    return false;
                }
            }
        }
        return true;
    }
}

