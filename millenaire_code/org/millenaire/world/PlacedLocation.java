/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  net.minecraft.core.BlockPos
 *  net.minecraft.world.level.block.Rotation
 */
package org.millenaire.world;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.Rotation;

public record PlacedLocation(BlockPos position, Rotation rotation) {
}

