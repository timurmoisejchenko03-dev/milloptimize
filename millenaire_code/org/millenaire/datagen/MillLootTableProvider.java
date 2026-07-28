/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  net.minecraft.core.HolderLookup$Provider
 *  net.minecraft.data.PackOutput
 *  net.minecraft.data.loot.BlockLootSubProvider
 *  net.minecraft.data.loot.LootTableProvider
 *  net.minecraft.data.loot.LootTableProvider$SubProviderEntry
 *  net.minecraft.world.flag.FeatureFlags
 *  net.minecraft.world.item.DyeColor
 *  net.minecraft.world.level.block.Block
 *  net.minecraft.world.level.storage.loot.parameters.LootContextParamSets
 */
package org.millenaire.datagen;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import net.minecraft.core.HolderLookup;
import net.minecraft.data.PackOutput;
import net.minecraft.data.loot.BlockLootSubProvider;
import net.minecraft.data.loot.LootTableProvider;
import net.minecraft.world.flag.FeatureFlags;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.storage.loot.parameters.LootContextParamSets;
import org.millenaire.block.ModBlocks;

public class MillLootTableProvider {
    public static LootTableProvider create(PackOutput output, CompletableFuture<HolderLookup.Provider> lookupProvider) {
        return new LootTableProvider(output, Set.of(), List.of(new LootTableProvider.SubProviderEntry(BlockLoot::new, LootContextParamSets.BLOCK)), lookupProvider);
    }

    private static class BlockLoot
    extends BlockLootSubProvider {
        private final ArrayList<Block> tracked = new ArrayList();

        protected BlockLoot(HolderLookup.Provider lookupProvider) {
            super(Set.of(), FeatureFlags.REGISTRY.allFlags(), lookupProvider);
        }

        protected void generate() {
            for (DyeColor color : DyeColor.values()) {
                this.dropSelfTracked((Block)ModBlocks.PAINTED_BRICKS.get(color).get());
                this.dropSelfTracked((Block)ModBlocks.DECORATED_BRICKS.get(color).get());
                this.dropSelfTracked((Block)ModBlocks.PAINTED_BRICK_STAIRS.get(color).get());
                this.slabTracked((Block)ModBlocks.PAINTED_BRICK_SLABS.get(color).get());
                this.dropSelfTracked((Block)ModBlocks.PAINTED_BRICK_WALLS.get(color).get());
            }
            this.dropSelfTracked((Block)ModBlocks.TIMBER_FRAME_PLAIN.get());
            this.dropSelfTracked((Block)ModBlocks.TIMBER_FRAME_CROSS.get());
            this.dropSelfTracked((Block)ModBlocks.TIMBER_FRAME_STAIRS.get());
            this.slabTracked((Block)ModBlocks.TIMBER_FRAME_SLAB.get());
            this.dropSelfTracked((Block)ModBlocks.THATCH.get());
            this.dropSelfTracked((Block)ModBlocks.THATCH_STAIRS.get());
            this.slabTracked((Block)ModBlocks.THATCH_SLAB.get());
            this.dropSelfTracked((Block)ModBlocks.MUD_BRICK.get());
            this.dropSelfTracked((Block)ModBlocks.MUD_BRICK_STAIRS.get());
            this.slabTracked((Block)ModBlocks.MUD_BRICK_SLAB.get());
            this.dropSelfTracked((Block)ModBlocks.MUD_BRICK_WALL.get());
            this.dropSelfTracked((Block)ModBlocks.DIRT_WALL.get());
            this.dropSelfTracked((Block)ModBlocks.SANDSTONE_CARVED.get());
            this.dropSelfTracked((Block)ModBlocks.SANDSTONE_CARVED_STAIRS.get());
            this.slabTracked((Block)ModBlocks.SANDSTONE_CARVED_SLAB.get());
            this.dropSelfTracked((Block)ModBlocks.SANDSTONE_CARVED_WALL.get());
            this.dropSelfTracked((Block)ModBlocks.RED_SANDSTONE_CARVED.get());
            this.dropSelfTracked((Block)ModBlocks.RED_SANDSTONE_CARVED_STAIRS.get());
            this.slabTracked((Block)ModBlocks.RED_SANDSTONE_CARVED_SLAB.get());
            this.dropSelfTracked((Block)ModBlocks.RED_SANDSTONE_CARVED_WALL.get());
            this.dropSelfTracked((Block)ModBlocks.OCHRE_SANDSTONE_CARVED.get());
            this.dropSelfTracked((Block)ModBlocks.OCHRE_SANDSTONE_CARVED_STAIRS.get());
            this.slabTracked((Block)ModBlocks.OCHRE_SANDSTONE_CARVED_SLAB.get());
            this.dropSelfTracked((Block)ModBlocks.OCHRE_SANDSTONE_CARVED_WALL.get());
            this.dropSelfTracked((Block)ModBlocks.MAYAN_GOLD_BLOCK.get());
            this.dropSelfTracked((Block)ModBlocks.BYZANTINE_TILES.get());
            this.dropSelfTracked((Block)ModBlocks.BYZANTINE_TILES_STAIRS.get());
            this.slabTracked((Block)ModBlocks.BYZANTINE_TILES_SLAB.get());
            this.dropSelfTracked((Block)ModBlocks.BYZANTINE_MOSAIC_RED.get());
            this.dropSelfTracked((Block)ModBlocks.BYZANTINE_MOSAIC_BLUE.get());
            this.dropSelfTracked((Block)ModBlocks.JAPANESE_TILES.get());
            this.dropSelfTracked((Block)ModBlocks.JAPANESE_TILES_STAIRS.get());
            this.slabTracked((Block)ModBlocks.JAPANESE_TILES_SLAB.get());
            this.dropSelfTracked((Block)ModBlocks.JAPANESE_STONE_TILES.get());
            this.dropSelfTracked((Block)ModBlocks.GRAY_TILES.get());
            this.dropSelfTracked((Block)ModBlocks.GRAY_TILES_STAIRS.get());
            this.slabTracked((Block)ModBlocks.GRAY_TILES_SLAB.get());
            this.dropSelfTracked((Block)ModBlocks.GREEN_TILES.get());
            this.dropSelfTracked((Block)ModBlocks.GREEN_TILES_STAIRS.get());
            this.slabTracked((Block)ModBlocks.GREEN_TILES_SLAB.get());
            this.dropSelfTracked((Block)ModBlocks.RED_TILES.get());
            this.dropSelfTracked((Block)ModBlocks.RED_TILES_STAIRS.get());
            this.slabTracked((Block)ModBlocks.RED_TILES_SLAB.get());
            this.dropSelfTracked((Block)ModBlocks.MUD_BRICK_SELJUK_ORNAMENTED.get());
            this.dropSelfTracked((Block)ModBlocks.MUD_BRICK_SELJUK_DECORATED.get());
            this.dropSelfTracked((Block)ModBlocks.MUD_BRICK_SMOOTH.get());
            this.dropSelfTracked((Block)ModBlocks.SNOW_BRICK.get());
            this.dropSelfTracked((Block)ModBlocks.ICE_BRICK.get());
            this.dropSelfTracked((Block)ModBlocks.SNOW_WALL.get());
            this.dropSelfTracked((Block)ModBlocks.SOD_OAK.get());
            this.dropSelfTracked((Block)ModBlocks.SOD_SPRUCE.get());
            this.dropSelfTracked((Block)ModBlocks.SOD_BIRCH.get());
            this.dropSelfTracked((Block)ModBlocks.SOD_JUNGLE.get());
            this.dropSelfTracked((Block)ModBlocks.SOD_ACACIA.get());
            this.dropSelfTracked((Block)ModBlocks.SOD_DARK_OAK.get());
        }

        private void dropSelfTracked(Block block) {
            this.dropSelf(block);
            this.tracked.add(block);
        }

        private void slabTracked(Block block) {
            this.add(block, x$0 -> this.createSlabItemTable((Block)x$0));
            this.tracked.add(block);
        }

        protected Iterable<Block> getKnownBlocks() {
            return this.tracked;
        }
    }
}

