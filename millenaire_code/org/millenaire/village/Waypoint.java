/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  javax.annotation.Nullable
 *  net.minecraft.core.BlockPos
 */
package org.millenaire.village;

import javax.annotation.Nullable;
import net.minecraft.core.BlockPos;
import org.millenaire.building.BuildingId;

public record Waypoint(BlockPos pos, @Nullable BuildingId buildingId) {
}

