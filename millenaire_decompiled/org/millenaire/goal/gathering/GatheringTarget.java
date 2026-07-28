/*
 * Decompiled with CFR 0.150.
 * 
 * Could not load the following classes:
 *  net.minecraft.core.BlockPos
 *  net.minecraft.world.entity.Entity
 */
package org.millenaire.goal.gathering;

import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.Entity;

public interface GatheringTarget {
    public BlockPos navigationPos();

    public record AreaTarget(BlockPos center, int radius) {
        @Override
        public BlockPos navigationPos() {
            return this.center;
        }
    }

    public record EntityTarget(Entity entity) {
        @Override
        public BlockPos navigationPos() {
            return this.entity.blockPosition();
        }
    }

    public record BlockTarget(BlockPos pos) {
        @Override
        public BlockPos navigationPos() {
            return this.pos;
        }
    }
}

