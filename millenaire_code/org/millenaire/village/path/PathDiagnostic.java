/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  net.minecraft.core.BlockPos
 */
package org.millenaire.village.path;

import net.minecraft.core.BlockPos;
import org.millenaire.building.BuildingId;
import org.millenaire.village.path.AStarFailureDetail;
import org.millenaire.village.path.PathFailureReason;

public record PathDiagnostic(BuildingId building, String planSetId, BlockPos origin, int expectedTier, int effectiveTier, BlockPos source, boolean sourceIsFallback, BlockPos destination, boolean connected, PathFailureReason failure, AStarFailureDetail astarDetail, int traceLength, int placedBlocks, boolean lateral) {
    public PathDiagnostic(BuildingId building, String planSetId, BlockPos origin, int expectedTier, int effectiveTier, BlockPos source, boolean sourceIsFallback, BlockPos destination, boolean connected, PathFailureReason failure, AStarFailureDetail astarDetail, int traceLength, int placedBlocks) {
        this(building, planSetId, origin, expectedTier, effectiveTier, source, sourceIsFallback, destination, connected, failure, astarDetail, traceLength, placedBlocks, false);
    }
}

