/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  net.minecraft.core.BlockPos
 *  net.minecraft.world.level.block.state.BlockState
 */
package org.millenaire.village.path;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.state.BlockState;

public record PathEntry(BlockPos pos, BlockState state) {
}

