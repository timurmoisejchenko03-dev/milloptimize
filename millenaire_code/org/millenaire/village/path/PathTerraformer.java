/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  javax.annotation.Nullable
 *  net.minecraft.core.BlockPos
 *  net.minecraft.world.level.block.Block
 *  net.minecraft.world.level.block.SlabBlock
 *  net.minecraft.world.level.block.state.BlockState
 *  net.minecraft.world.level.block.state.properties.Property
 *  net.minecraft.world.level.block.state.properties.SlabType
 */
package org.millenaire.village.path;

import java.util.ArrayList;
import java.util.List;
import java.util.function.IntUnaryOperator;
import javax.annotation.Nullable;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SlabBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.Property;
import net.minecraft.world.level.block.state.properties.SlabType;
import org.millenaire.block.MillPathBlock;
import org.millenaire.block.MillPathSlabBlock;
import org.millenaire.village.path.PathEntry;

public final class PathTerraformer {
    private PathTerraformer() {
    }

    public static List<PathEntry> blocksForColumn(ColumnDecision decision, Block fullBlock, @Nullable SlabBlock slabBlock, BlockState foundationBlock, BlockState airBlock, IntUnaryOperator fillPermitted, IntUnaryOperator cutPermitted) {
        BlockState pathState;
        int placeY;
        ArrayList<PathEntry> out = new ArrayList<PathEntry>(4);
        boolean slab = (decision.surfaceHalfY & 1) == 1;
        int n = placeY = slab ? (decision.surfaceHalfY - 1) / 2 : decision.surfaceHalfY / 2 - 1;
        if (slab && slabBlock != null) {
            pathState = (BlockState)((BlockState)slabBlock.defaultBlockState().setValue((Property)SlabBlock.TYPE, (Comparable)SlabType.BOTTOM)).setValue((Property)MillPathSlabBlock.STABLE, (Comparable)Boolean.valueOf(false));
        } else {
            pathState = (BlockState)fullBlock.defaultBlockState().setValue((Property)MillPathBlock.STABLE, (Comparable)Boolean.valueOf(false));
            if (slab) {
                --placeY;
            }
        }
        for (int fy = decision.groundSurfaceY; fy < placeY; ++fy) {
            if (fillPermitted.applyAsInt(fy) != 1) continue;
            out.add(new PathEntry(new BlockPos(decision.x, fy, decision.z), foundationBlock));
        }
        out.add(new PathEntry(new BlockPos(decision.x, placeY, decision.z), pathState));
        for (int cy = placeY + 1; cy <= placeY + 2; ++cy) {
            if (cutPermitted.applyAsInt(cy) != 1) continue;
            out.add(new PathEntry(new BlockPos(decision.x, cy, decision.z), airBlock));
        }
        return out;
    }

    public record ColumnDecision(int x, int z, int surfaceHalfY, int groundSurfaceY) {
    }
}

