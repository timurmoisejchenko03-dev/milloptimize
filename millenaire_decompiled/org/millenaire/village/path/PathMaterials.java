/*
 * Decompiled with CFR 0.150.
 * 
 * Could not load the following classes:
 *  javax.annotation.Nullable
 *  net.minecraft.world.level.block.Block
 *  net.minecraft.world.level.block.SlabBlock
 *  net.minecraft.world.level.block.state.BlockState
 */
package org.millenaire.village.path;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.annotation.Nullable;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SlabBlock;
import net.minecraft.world.level.block.state.BlockState;
import org.millenaire.block.ModBlocks;

public final class PathMaterials {
    private static Map<String, MaterialPair> materials;

    private PathMaterials() {
    }

    private static Map<String, MaterialPair> getMaterials() {
        if (materials == null) {
            materials = new HashMap<String, MaterialPair>();
            materials.put("pathdirt", new MaterialPair((Block)ModBlocks.PATH_DIRT.get(), (SlabBlock)ModBlocks.PATH_DIRT_SLAB.get()));
            materials.put("pathgravel", new MaterialPair((Block)ModBlocks.PATH_GRAVEL.get(), (SlabBlock)ModBlocks.PATH_GRAVEL_SLAB.get()));
            materials.put("pathslabs", new MaterialPair((Block)ModBlocks.PATH_SLABS.get(), (SlabBlock)ModBlocks.PATH_SLABS_SLAB.get()));
            materials.put("pathsandstone", new MaterialPair((Block)ModBlocks.PATH_SANDSTONE.get(), (SlabBlock)ModBlocks.PATH_SANDSTONE_SLAB.get()));
            materials.put("pathochretiles", new MaterialPair((Block)ModBlocks.PATH_OCHRE_TILES.get(), (SlabBlock)ModBlocks.PATH_OCHRE_TILES_SLAB.get()));
            materials.put("pathgravelslabs", new MaterialPair((Block)ModBlocks.PATH_GRAVEL_SLABS.get(), (SlabBlock)ModBlocks.PATH_GRAVEL_SLABS_SLAB.get()));
            materials.put("pathsnow", new MaterialPair((Block)ModBlocks.PATH_SNOW.get(), (SlabBlock)ModBlocks.PATH_SNOW_SLAB.get()));
        }
        return materials;
    }

    @Nullable
    public static MaterialPair resolve(String materialName) {
        return PathMaterials.getMaterials().get(materialName);
    }

    public static int rankOf(BlockState state, List<String> orderedMaterials) {
        Block block = state.getBlock();
        for (int i = 0; i < orderedMaterials.size(); ++i) {
            MaterialPair mp = PathMaterials.resolve(orderedMaterials.get(i));
            if (mp == null || mp.fullBlock() != block && mp.slabBlock() != block) continue;
            return i;
        }
        return -1;
    }

    public record MaterialPair(Block fullBlock, SlabBlock slabBlock) {
    }
}

