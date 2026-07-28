/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  javax.annotation.Nullable
 *  net.minecraft.core.BlockPos
 */
package org.millenaire.village.path;

import javax.annotation.Nullable;
import net.minecraft.core.BlockPos;
import org.millenaire.building.BuildingId;

public record PathRoute(BlockPos from, BlockPos to, String material, int width, int pathLevel, @Nullable BuildingId sourceId, @Nullable BuildingId destinationId, boolean lateral) {
    public PathRoute(BlockPos from, BlockPos to, String material, int width, int pathLevel) {
        this(from, to, material, width, pathLevel, null, null, false);
    }

    public PathRoute withTierAndMaterial(int newPathLevel, String newMaterial) {
        return new PathRoute(this.from, this.to, newMaterial, this.width, newPathLevel, this.sourceId, this.destinationId, this.lateral);
    }
}

