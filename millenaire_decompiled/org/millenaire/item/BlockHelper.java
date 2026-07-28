/*
 * Decompiled with CFR 0.150.
 * 
 * Could not load the following classes:
 *  javax.annotation.Nullable
 *  net.minecraft.core.registries.BuiltInRegistries
 *  net.minecraft.resources.ResourceLocation
 *  net.minecraft.tags.BlockTags
 *  net.minecraft.world.level.block.Block
 *  net.minecraft.world.level.block.LeavesBlock
 *  net.minecraft.world.level.block.state.BlockState
 */
package org.millenaire.item;

import javax.annotation.Nullable;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.LeavesBlock;
import net.minecraft.world.level.block.state.BlockState;

public final class BlockHelper {
    private BlockHelper() {
    }

    public static boolean isDecorativeFlower(BlockState state) {
        return state.is(BlockTags.FLOWERS) && !(state.getBlock() instanceof LeavesBlock);
    }

    @Nullable
    public static Block resolve(String blockId) {
        if (blockId == null || blockId.isEmpty()) {
            return null;
        }
        return BuiltInRegistries.BLOCK.getOptional(ResourceLocation.parse((String)blockId)).orElse(null);
    }
}

