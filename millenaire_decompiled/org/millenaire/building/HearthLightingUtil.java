/*
 * Decompiled with CFR 0.150.
 * 
 * Could not load the following classes:
 *  net.minecraft.core.BlockPos
 *  net.minecraft.core.BlockPos$MutableBlockPos
 *  net.minecraft.core.Vec3i
 *  net.minecraft.server.level.ServerLevel
 *  net.minecraft.world.level.block.state.BlockState
 *  net.minecraft.world.level.block.state.properties.BlockStateProperties
 *  net.minecraft.world.level.block.state.properties.Property
 */
package org.millenaire.building;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Vec3i;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.Property;
import org.millenaire.tag.ModTags;

public final class HearthLightingUtil {
    private HearthLightingUtil() {
    }

    public static void lightHearthsInArea(ServerLevel level, BlockPos origin, Vec3i size) {
        if (level == null || origin == null || size == null) {
            return;
        }
        int sx = size.getX();
        int sy = size.getY();
        int sz = size.getZ();
        if (sx <= 0 || sy <= 0 || sz <= 0) {
            return;
        }
        BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();
        for (int x = 0; x < sx; ++x) {
            for (int z = 0; z < sz; ++z) {
                for (int y = 0; y < sy; ++y) {
                    pos.set(origin.getX() + x, origin.getY() + y, origin.getZ() + z);
                    BlockState state = level.getBlockState((BlockPos)pos);
                    if (!state.is(ModTags.Blocks.HEARTH_BLOCKS) || !state.hasProperty((Property)BlockStateProperties.LIT) || ((Boolean)state.getValue((Property)BlockStateProperties.LIT)).booleanValue()) continue;
                    level.setBlock((BlockPos)pos, (BlockState)state.setValue((Property)BlockStateProperties.LIT, (Comparable)Boolean.valueOf(true)), 2);
                }
            }
        }
    }
}

