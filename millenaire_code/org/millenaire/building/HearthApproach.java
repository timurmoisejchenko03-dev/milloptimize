/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  net.minecraft.core.BlockPos
 *  net.minecraft.core.Direction
 *  net.minecraft.core.Direction$Plane
 *  net.minecraft.tags.BlockTags
 *  net.minecraft.world.level.BlockGetter
 *  net.minecraft.world.level.block.Blocks
 *  net.minecraft.world.level.block.CampfireBlock
 *  net.minecraft.world.level.block.state.BlockState
 *  net.minecraft.world.level.block.state.properties.Property
 */
package org.millenaire.building;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.CampfireBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.Property;

public final class HearthApproach {
    private HearthApproach() {
    }

    public static BlockPos findStandPosition(BlockGetter level, BlockPos hearthPos) {
        for (Direction dir : Direction.Plane.HORIZONTAL) {
            BlockPos candidate = hearthPos.relative(dir);
            if (!HearthApproach.isWalkableStand(level, candidate)) continue;
            return candidate;
        }
        return hearthPos;
    }

    static boolean isWalkableStand(BlockGetter level, BlockPos pos) {
        BlockState here = level.getBlockState(pos);
        if (HearthApproach.isLitCampfire(here)) {
            return false;
        }
        if (!HearthApproach.isPassable(level, pos, here)) {
            return false;
        }
        BlockState above = level.getBlockState(pos.above());
        if (!HearthApproach.isPassable(level, pos.above(), above)) {
            return false;
        }
        BlockPos belowPos = pos.below();
        BlockState below = level.getBlockState(belowPos);
        if (below.isAir()) {
            return false;
        }
        if (below.is(BlockTags.FENCES) || below.is(BlockTags.WALLS) || below.is(Blocks.IRON_BARS)) {
            return false;
        }
        return below.isFaceSturdy(level, belowPos, Direction.UP);
    }

    private static boolean isPassable(BlockGetter level, BlockPos pos, BlockState state) {
        if (state.isAir()) {
            return true;
        }
        return state.getCollisionShape(level, pos).isEmpty();
    }

    private static boolean isLitCampfire(BlockState state) {
        return state.getBlock() instanceof CampfireBlock && state.hasProperty((Property)CampfireBlock.LIT) && (Boolean)state.getValue((Property)CampfireBlock.LIT) != false;
    }
}

