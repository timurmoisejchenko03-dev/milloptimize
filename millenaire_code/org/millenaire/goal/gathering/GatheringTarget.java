/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  net.minecraft.core.BlockPos
 *  net.minecraft.world.entity.Entity
 */
package org.millenaire.goal.gathering;

import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.Entity;

public sealed interface GatheringTarget {
    public BlockPos navigationPos();

    public record AreaTarget(BlockPos center, int radius) implements GatheringTarget
    {
        @Override
        public BlockPos navigationPos() {
            return this.center;
        }
    }

    public record EntityTarget(Entity entity) implements GatheringTarget
    {
        @Override
        public BlockPos navigationPos() {
            return this.entity.blockPosition();
        }
    }

    public record BlockTarget(BlockPos pos) implements GatheringTarget
    {
        @Override
        public BlockPos navigationPos() {
            return this.pos;
        }
    }
}

