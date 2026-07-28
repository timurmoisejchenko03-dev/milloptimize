/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  net.minecraft.world.item.DyeColor
 *  net.minecraft.world.level.block.Block
 *  net.minecraft.world.level.block.IronBarsBlock
 *  net.minecraft.world.level.block.SlabBlock
 *  net.minecraft.world.level.block.SoundType
 *  net.minecraft.world.level.block.StainedGlassPaneBlock
 *  net.minecraft.world.level.block.StairBlock
 *  net.minecraft.world.level.block.WallBlock
 *  net.minecraft.world.level.block.state.BlockBehaviour
 *  net.minecraft.world.level.block.state.BlockBehaviour$Properties
 *  net.minecraft.world.level.block.state.properties.BlockSetType
 *  net.minecraft.world.level.block.state.properties.Property
 *  net.minecraft.world.level.material.MapColor
 *  net.minecraft.world.level.material.PushReaction
 *  net.neoforged.bus.api.IEventBus
 *  net.neoforged.neoforge.registries.DeferredBlock
 *  net.neoforged.neoforge.registries.DeferredRegister
 *  net.neoforged.neoforge.registries.DeferredRegister$Blocks
 */
package org.millenaire.block;

import java.util.EnumMap;
import java.util.Map;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.IronBarsBlock;
import net.minecraft.world.level.block.SlabBlock;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.StainedGlassPaneBlock;
import net.minecraft.world.level.block.StairBlock;
import net.minecraft.world.level.block.WallBlock;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.properties.BlockSetType;
import net.minecraft.world.level.block.state.properties.Property;
import net.minecraft.world.level.material.MapColor;
import net.minecraft.world.level.material.PushReaction;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredBlock;
import net.neoforged.neoforge.registries.DeferredRegister;
import org.millenaire.block.AppleTreeLeavesBlock;
import org.millenaire.block.AppleTreeSaplingBlock;
import org.millenaire.block.BlockGrapeVine;
import org.millenaire.block.BlockMillCrops;
import org.millenaire.block.BlockSilkWorm;
import org.millenaire.block.BlockSnailSoil;
import org.millenaire.block.BlockWetBrick;
import org.millenaire.block.FirePitBlock;
import org.millenaire.block.HorizontalRotatedPillarBlock;
import org.millenaire.block.ImportTableBlock;
import org.millenaire.block.IndianRosetteBlock;
import org.millenaire.block.LockedChestBlock;
import org.millenaire.block.MillBedBlock;
import org.millenaire.block.MillPathBlock;
import org.millenaire.block.MillPathSlabBlock;
import org.millenaire.block.NormanRosetteBlock;
import org.millenaire.block.OliveTreeLeavesBlock;
import org.millenaire.block.OliveTreeSaplingBlock;
import org.millenaire.block.PaintedBrickBlock;
import org.millenaire.block.PaintedBrickSlabBlock;
import org.millenaire.block.PaintedBrickStairBlock;
import org.millenaire.block.PaintedBrickWallBlock;
import org.millenaire.block.PathTier;
import org.millenaire.block.PistachioTreeLeavesBlock;
import org.millenaire.block.PistachioTreeSaplingBlock;
import org.millenaire.block.RicePaddyBlock;
import org.millenaire.block.SlidingDoorBlock;
import org.millenaire.block.VillagePanelBlock;
import org.millenaire.block.mock.MockAnimalSpawnBlock;
import org.millenaire.block.mock.MockBannerStandingBlock;
import org.millenaire.block.mock.MockBannerWallBlock;
import org.millenaire.block.mock.MockBlock;
import org.millenaire.block.mock.MockChestBlock;
import org.millenaire.block.mock.MockDecorBlock;
import org.millenaire.block.mock.MockFacingMarkerBlock;
import org.millenaire.block.mock.MockFreeBlock;
import org.millenaire.block.mock.MockMarkerBlock;
import org.millenaire.block.mock.MockSoilBlock;
import org.millenaire.block.mock.MockSourceBlock;
import org.millenaire.block.mock.MockTreeSpawnBlock;

public final class ModBlocks {
    public static final DeferredRegister.Blocks BLOCKS = DeferredRegister.createBlocks((String)"millenaire");
    public static final DeferredBlock<Block> TIMBER_FRAME_PLAIN = BLOCKS.registerSimpleBlock("timber_frame_plain", BlockBehaviour.Properties.of().mapColor(MapColor.WOOD).strength(2.0f, 5.0f).sound(SoundType.WOOD).requiresCorrectToolForDrops());
    public static final DeferredBlock<Block> TIMBER_FRAME_CROSS = BLOCKS.registerSimpleBlock("timber_frame_cross", BlockBehaviour.Properties.of().mapColor(MapColor.WOOD).strength(2.0f, 5.0f).sound(SoundType.WOOD).requiresCorrectToolForDrops());
    public static final DeferredBlock<Block> THATCH = BLOCKS.registerSimpleBlock("thatch", BlockBehaviour.Properties.of().mapColor(MapColor.COLOR_YELLOW).strength(2.0f, 5.0f).sound(SoundType.WOOD).requiresCorrectToolForDrops());
    public static final DeferredBlock<Block> MUD_BRICK = BLOCKS.registerSimpleBlock("mud_brick", BlockBehaviour.Properties.of().mapColor(MapColor.STONE).strength(1.5f, 10.0f).sound(SoundType.STONE).requiresCorrectToolForDrops());
    public static final DeferredBlock<Block> DIRT_WALL = BLOCKS.registerSimpleBlock("dirt_wall", BlockBehaviour.Properties.of().mapColor(MapColor.DIRT).strength(0.8f, 2.0f).sound(SoundType.GRAVEL).requiresCorrectToolForDrops());
    public static final DeferredBlock<StairBlock> TIMBER_FRAME_STAIRS = BLOCKS.register("timber_frame_stairs", key -> new StairBlock(((Block)TIMBER_FRAME_PLAIN.get()).defaultBlockState(), BlockBehaviour.Properties.ofFullCopy((BlockBehaviour)((BlockBehaviour)TIMBER_FRAME_PLAIN.get()))));
    public static final DeferredBlock<StairBlock> THATCH_STAIRS = BLOCKS.register("thatch_stairs", key -> new StairBlock(((Block)THATCH.get()).defaultBlockState(), BlockBehaviour.Properties.ofFullCopy((BlockBehaviour)((BlockBehaviour)THATCH.get()))));
    public static final DeferredBlock<StairBlock> MUD_BRICK_STAIRS = BLOCKS.register("mud_brick_stairs", key -> new StairBlock(((Block)MUD_BRICK.get()).defaultBlockState(), BlockBehaviour.Properties.ofFullCopy((BlockBehaviour)((BlockBehaviour)MUD_BRICK.get()))));
    public static final DeferredBlock<SlabBlock> TIMBER_FRAME_SLAB = BLOCKS.register("timber_frame_slab", key -> new SlabBlock(BlockBehaviour.Properties.ofFullCopy((BlockBehaviour)((BlockBehaviour)TIMBER_FRAME_PLAIN.get()))));
    public static final DeferredBlock<SlabBlock> THATCH_SLAB = BLOCKS.register("thatch_slab", key -> new SlabBlock(BlockBehaviour.Properties.ofFullCopy((BlockBehaviour)((BlockBehaviour)THATCH.get()))));
    public static final DeferredBlock<SlabBlock> MUD_BRICK_SLAB = BLOCKS.register("mud_brick_slab", key -> new SlabBlock(BlockBehaviour.Properties.ofFullCopy((BlockBehaviour)((BlockBehaviour)MUD_BRICK.get()))));
    public static final DeferredBlock<WallBlock> MUD_BRICK_WALL = BLOCKS.register("mud_brick_wall", key -> new WallBlock(BlockBehaviour.Properties.ofFullCopy((BlockBehaviour)((BlockBehaviour)MUD_BRICK.get()))));
    public static final DeferredBlock<StainedGlassPaneBlock> STAINED_GLASS_WHITE = BLOCKS.register("stained_glass_white", key -> new StainedGlassPaneBlock(DyeColor.WHITE, BlockBehaviour.Properties.of().mapColor(MapColor.QUARTZ).strength(0.3f).sound(SoundType.GLASS).noOcclusion()));
    public static final DeferredBlock<StainedGlassPaneBlock> STAINED_GLASS_YELLOW = BLOCKS.register("stained_glass_yellow", key -> new StainedGlassPaneBlock(DyeColor.YELLOW, BlockBehaviour.Properties.of().mapColor(MapColor.COLOR_YELLOW).strength(0.3f).sound(SoundType.GLASS).noOcclusion()));
    public static final DeferredBlock<StainedGlassPaneBlock> STAINED_GLASS_YELLOW_RED = BLOCKS.register("stained_glass_yellow_red", key -> new StainedGlassPaneBlock(DyeColor.ORANGE, BlockBehaviour.Properties.of().mapColor(MapColor.COLOR_ORANGE).strength(0.3f).sound(SoundType.GLASS).noOcclusion()));
    public static final DeferredBlock<StainedGlassPaneBlock> STAINED_GLASS_RED_BLUE = BLOCKS.register("stained_glass_red_blue", key -> new StainedGlassPaneBlock(DyeColor.PURPLE, BlockBehaviour.Properties.of().mapColor(MapColor.COLOR_PURPLE).strength(0.3f).sound(SoundType.GLASS).noOcclusion()));
    public static final DeferredBlock<StainedGlassPaneBlock> STAINED_GLASS_GREEN_BLUE = BLOCKS.register("stained_glass_green_blue", key -> new StainedGlassPaneBlock(DyeColor.CYAN, BlockBehaviour.Properties.of().mapColor(MapColor.COLOR_CYAN).strength(0.3f).sound(SoundType.GLASS).noOcclusion()));
    public static final DeferredBlock<NormanRosetteBlock> ROSETTE = BLOCKS.register("rosette", key -> new NormanRosetteBlock(BlockBehaviour.Properties.of().mapColor(MapColor.NONE).strength(0.3f).sound(SoundType.GLASS).noOcclusion()));
    public static final DeferredBlock<IndianRosetteBlock> WOODEN_BARS_ROSETTE = BLOCKS.register("wooden_bars_rosette", key -> new IndianRosetteBlock(BlockBehaviour.Properties.of().mapColor(MapColor.WOOD).strength(5.0f, 10.0f).sound(SoundType.WOOD).noOcclusion()));
    public static final DeferredBlock<IronBarsBlock> WOODEN_BARS = BLOCKS.register("wooden_bars", key -> new IronBarsBlock(BlockBehaviour.Properties.of().mapColor(MapColor.WOOD).strength(5.0f, 10.0f).sound(SoundType.WOOD).noOcclusion()));
    public static final DeferredBlock<Block> PATH_DIRT = BLOCKS.register("path_dirt", key -> new MillPathBlock(BlockBehaviour.Properties.of().mapColor(MapColor.DIRT).strength(0.8f).sound(SoundType.GRAVEL), PathTier.RUSTIC));
    public static final DeferredBlock<SlabBlock> PATH_DIRT_SLAB = BLOCKS.register("path_dirt_slab", key -> new MillPathSlabBlock(BlockBehaviour.Properties.of().mapColor(MapColor.DIRT).strength(0.8f).sound(SoundType.GRAVEL), PathTier.RUSTIC));
    public static final DeferredBlock<Block> PATH_GRAVEL = BLOCKS.register("path_gravel", key -> new MillPathBlock(BlockBehaviour.Properties.of().mapColor(MapColor.STONE).strength(0.8f).sound(SoundType.GRAVEL), PathTier.RUSTIC));
    public static final DeferredBlock<SlabBlock> PATH_GRAVEL_SLAB = BLOCKS.register("path_gravel_slab", key -> new MillPathSlabBlock(BlockBehaviour.Properties.of().mapColor(MapColor.STONE).strength(0.8f).sound(SoundType.GRAVEL), PathTier.RUSTIC));
    public static final DeferredBlock<Block> PATH_OCHRE_TILES = BLOCKS.register("path_ochre_tiles", key -> new MillPathBlock(BlockBehaviour.Properties.of().mapColor(MapColor.TERRACOTTA_ORANGE).strength(0.8f).sound(SoundType.STONE), PathTier.STONE));
    public static final DeferredBlock<Block> PATH_SANDSTONE = BLOCKS.register("path_sandstone", key -> new MillPathBlock(BlockBehaviour.Properties.of().mapColor(MapColor.SAND).strength(0.8f).sound(SoundType.STONE), PathTier.STONE));
    public static final DeferredBlock<Block> PATH_SLABS = BLOCKS.register("path_slabs", key -> new MillPathBlock(BlockBehaviour.Properties.of().mapColor(MapColor.STONE).strength(0.8f).sound(SoundType.STONE), PathTier.PAVED));
    public static final DeferredBlock<SlabBlock> PATH_SLABS_SLAB = BLOCKS.register("path_slabs_slab", key -> new MillPathSlabBlock(BlockBehaviour.Properties.of().mapColor(MapColor.STONE).strength(0.8f).sound(SoundType.STONE), PathTier.PAVED));
    public static final DeferredBlock<SlabBlock> PATH_OCHRE_TILES_SLAB = BLOCKS.register("path_ochre_tiles_slab", key -> new MillPathSlabBlock(BlockBehaviour.Properties.of().mapColor(MapColor.TERRACOTTA_ORANGE).strength(0.8f).sound(SoundType.STONE), PathTier.STONE));
    public static final DeferredBlock<SlabBlock> PATH_SANDSTONE_SLAB = BLOCKS.register("path_sandstone_slab", key -> new MillPathSlabBlock(BlockBehaviour.Properties.of().mapColor(MapColor.SAND).strength(0.8f).sound(SoundType.STONE), PathTier.STONE));
    public static final DeferredBlock<Block> PATH_GRAVEL_SLABS = BLOCKS.register("path_gravel_slabs", key -> new MillPathBlock(BlockBehaviour.Properties.of().mapColor(MapColor.STONE).strength(0.8f).sound(SoundType.STONE), PathTier.PAVED));
    public static final DeferredBlock<SlabBlock> PATH_GRAVEL_SLABS_SLAB = BLOCKS.register("path_gravel_slabs_slab", key -> new MillPathSlabBlock(BlockBehaviour.Properties.of().mapColor(MapColor.STONE).strength(0.8f).sound(SoundType.STONE), PathTier.PAVED));
    public static final DeferredBlock<Block> PATH_SNOW = BLOCKS.register("path_snow", key -> new MillPathBlock(BlockBehaviour.Properties.of().mapColor(MapColor.SNOW).strength(0.8f).sound(SoundType.SNOW), PathTier.RUSTIC));
    public static final DeferredBlock<SlabBlock> PATH_SNOW_SLAB = BLOCKS.register("path_snow_slab", key -> new MillPathSlabBlock(BlockBehaviour.Properties.of().mapColor(MapColor.SNOW).strength(0.8f).sound(SoundType.SNOW), PathTier.RUSTIC));
    public static final DeferredBlock<Block> TAPESTRY = BLOCKS.registerSimpleBlock("tapestry", BlockBehaviour.Properties.of().mapColor(MapColor.WOOL).strength(0.5f).sound(SoundType.WOOL).noOcclusion());
    public static final DeferredBlock<MillBedBlock> STRAW_BED = BLOCKS.register("straw_bed", key -> new MillBedBlock(DyeColor.WHITE, 4, BlockBehaviour.Properties.of().mapColor(MapColor.COLOR_YELLOW).strength(0.5f).sound(SoundType.GRASS).noOcclusion()));
    public static final DeferredBlock<AppleTreeSaplingBlock> APPLE_TREE_SAPLING = BLOCKS.register("apple_tree_sapling", key -> new AppleTreeSaplingBlock(BlockBehaviour.Properties.of().mapColor(MapColor.PLANT).noCollission().randomTicks().instabreak().sound(SoundType.GRASS).pushReaction(PushReaction.DESTROY)));
    public static final DeferredBlock<AppleTreeLeavesBlock> APPLE_TREE_LEAVES = BLOCKS.register("apple_tree_leaves", key -> new AppleTreeLeavesBlock(BlockBehaviour.Properties.of().mapColor(MapColor.PLANT).strength(0.2f).randomTicks().sound(SoundType.GRASS).noOcclusion().isValidSpawn((state, getter, pos, entityType) -> false).isSuffocating((state, getter, pos) -> false).isViewBlocking((state, getter, pos) -> false).ignitedByLava().pushReaction(PushReaction.DESTROY).isRedstoneConductor((state, getter, pos) -> false)));
    public static final DeferredBlock<Block> RICE_PADDY = BLOCKS.register("rice_paddy", key -> new RicePaddyBlock(BlockBehaviour.Properties.of().mapColor(MapColor.WATER).strength(0.6f).sound(SoundType.MUD).noOcclusion().randomTicks()));
    public static final DeferredBlock<Block> CROP_RICE = BLOCKS.register("crop_rice", key -> new BlockMillCrops(true, false, BlockBehaviour.Properties.of().noCollission().randomTicks().instabreak().sound(SoundType.CROP)));
    public static final DeferredBlock<Block> CROP_TURMERIC = BLOCKS.register("crop_turmeric", key -> new BlockMillCrops(false, false, BlockBehaviour.Properties.of().noCollission().randomTicks().instabreak().sound(SoundType.CROP)));
    public static final DeferredBlock<Block> CROP_COTTON = BLOCKS.register("crop_cotton", key -> new BlockMillCrops(true, false, BlockBehaviour.Properties.of().noCollission().randomTicks().instabreak().sound(SoundType.CROP)));
    public static final DeferredBlock<IronBarsBlock> WOODEN_BARS_INDIAN = BLOCKS.register("wooden_bars_indian", key -> new IronBarsBlock(BlockBehaviour.Properties.of().mapColor(MapColor.WOOD).strength(0.3f, 10.0f).sound(SoundType.WOOD).noOcclusion()));
    public static final DeferredBlock<MillBedBlock> CHARPOY = BLOCKS.register("charpoy", key -> new MillBedBlock(DyeColor.BROWN, 4, BlockBehaviour.Properties.of().mapColor(MapColor.WOOD).strength(0.5f).sound(SoundType.WOOD).noOcclusion()));
    public static final DeferredBlock<MillBedBlock> FUTON = BLOCKS.register("futon", key -> new MillBedBlock(DyeColor.GREEN, 2, BlockBehaviour.Properties.of().mapColor(MapColor.COLOR_GREEN).strength(0.5f).sound(SoundType.WOOL).noOcclusion()));
    public static final DeferredBlock<BlockWetBrick> WET_BRICK = BLOCKS.register("wet_brick", key -> new BlockWetBrick(BlockBehaviour.Properties.of().mapColor(MapColor.DIRT).strength(0.8f).sound(SoundType.GRAVEL).randomTicks()));
    public static final DeferredBlock<Block> SANDSTONE_CARVED = BLOCKS.registerSimpleBlock("sandstone_carved", BlockBehaviour.Properties.of().mapColor(MapColor.SAND).strength(1.5f, 10.0f).sound(SoundType.STONE).requiresCorrectToolForDrops());
    public static final DeferredBlock<StairBlock> SANDSTONE_CARVED_STAIRS = BLOCKS.register("sandstone_carved_stairs", key -> new StairBlock(((Block)SANDSTONE_CARVED.get()).defaultBlockState(), BlockBehaviour.Properties.ofFullCopy((BlockBehaviour)((BlockBehaviour)SANDSTONE_CARVED.get()))));
    public static final DeferredBlock<SlabBlock> SANDSTONE_CARVED_SLAB = BLOCKS.register("sandstone_carved_slab", key -> new SlabBlock(BlockBehaviour.Properties.ofFullCopy((BlockBehaviour)((BlockBehaviour)SANDSTONE_CARVED.get()))));
    public static final DeferredBlock<WallBlock> SANDSTONE_CARVED_WALL = BLOCKS.register("sandstone_carved_wall", key -> new WallBlock(BlockBehaviour.Properties.ofFullCopy((BlockBehaviour)((BlockBehaviour)SANDSTONE_CARVED.get()))));
    public static final DeferredBlock<Block> RED_SANDSTONE_CARVED = BLOCKS.registerSimpleBlock("red_sandstone_carved", BlockBehaviour.Properties.of().mapColor(MapColor.COLOR_ORANGE).strength(1.5f, 10.0f).sound(SoundType.STONE).requiresCorrectToolForDrops());
    public static final DeferredBlock<StairBlock> RED_SANDSTONE_CARVED_STAIRS = BLOCKS.register("red_sandstone_carved_stairs", key -> new StairBlock(((Block)RED_SANDSTONE_CARVED.get()).defaultBlockState(), BlockBehaviour.Properties.ofFullCopy((BlockBehaviour)((BlockBehaviour)RED_SANDSTONE_CARVED.get()))));
    public static final DeferredBlock<SlabBlock> RED_SANDSTONE_CARVED_SLAB = BLOCKS.register("red_sandstone_carved_slab", key -> new SlabBlock(BlockBehaviour.Properties.ofFullCopy((BlockBehaviour)((BlockBehaviour)RED_SANDSTONE_CARVED.get()))));
    public static final DeferredBlock<WallBlock> RED_SANDSTONE_CARVED_WALL = BLOCKS.register("red_sandstone_carved_wall", key -> new WallBlock(BlockBehaviour.Properties.ofFullCopy((BlockBehaviour)((BlockBehaviour)RED_SANDSTONE_CARVED.get()))));
    public static final DeferredBlock<Block> OCHRE_SANDSTONE_CARVED = BLOCKS.registerSimpleBlock("ochre_sandstone_carved", BlockBehaviour.Properties.of().mapColor(MapColor.TERRACOTTA_ORANGE).strength(1.5f, 10.0f).sound(SoundType.STONE).requiresCorrectToolForDrops());
    public static final DeferredBlock<StairBlock> OCHRE_SANDSTONE_CARVED_STAIRS = BLOCKS.register("ochre_sandstone_carved_stairs", key -> new StairBlock(((Block)OCHRE_SANDSTONE_CARVED.get()).defaultBlockState(), BlockBehaviour.Properties.ofFullCopy((BlockBehaviour)((BlockBehaviour)OCHRE_SANDSTONE_CARVED.get()))));
    public static final DeferredBlock<SlabBlock> OCHRE_SANDSTONE_CARVED_SLAB = BLOCKS.register("ochre_sandstone_carved_slab", key -> new SlabBlock(BlockBehaviour.Properties.ofFullCopy((BlockBehaviour)((BlockBehaviour)OCHRE_SANDSTONE_CARVED.get()))));
    public static final DeferredBlock<WallBlock> OCHRE_SANDSTONE_CARVED_WALL = BLOCKS.register("ochre_sandstone_carved_wall", key -> new WallBlock(BlockBehaviour.Properties.ofFullCopy((BlockBehaviour)((BlockBehaviour)OCHRE_SANDSTONE_CARVED.get()))));
    public static final DeferredBlock<Block> MAYAN_GOLD_BLOCK = BLOCKS.registerSimpleBlock("mayan_gold_block", BlockBehaviour.Properties.of().mapColor(MapColor.GOLD).strength(1.5f, 10.0f).sound(SoundType.STONE).requiresCorrectToolForDrops());
    public static final DeferredBlock<IronBarsBlock> MAYAN_STATUE = BLOCKS.register("mayan_statue", key -> new IronBarsBlock(BlockBehaviour.Properties.of().mapColor(MapColor.STONE).strength(2.0f, 6.0f).sound(SoundType.STONE).noOcclusion()));
    public static final DeferredBlock<Block> CROP_MAIZE = BLOCKS.register("crop_maize", key -> new BlockMillCrops(false, true, BlockBehaviour.Properties.of().noCollission().randomTicks().instabreak().sound(SoundType.CROP)));
    public static final DeferredBlock<HorizontalRotatedPillarBlock> BYZANTINE_TILES = BLOCKS.register("byzantine_tiles", key -> new HorizontalRotatedPillarBlock(BlockBehaviour.Properties.of().mapColor(MapColor.SAND).strength(2.0f, 10.0f).sound(SoundType.STONE).requiresCorrectToolForDrops()));
    public static final DeferredBlock<HorizontalRotatedPillarBlock> BYZANTINE_STONE_TILES = BLOCKS.register("byzantine_stone_tiles", key -> new HorizontalRotatedPillarBlock(BlockBehaviour.Properties.of().mapColor(MapColor.STONE).strength(2.0f, 10.0f).sound(SoundType.STONE).requiresCorrectToolForDrops()));
    public static final DeferredBlock<HorizontalRotatedPillarBlock> BYZANTINE_SANDSTONE_TILES = BLOCKS.register("byzantine_sandstone_tiles", key -> new HorizontalRotatedPillarBlock(BlockBehaviour.Properties.of().mapColor(MapColor.SAND).strength(2.0f, 10.0f).sound(SoundType.STONE).requiresCorrectToolForDrops()));
    public static final DeferredBlock<Block> BYZANTINE_STONE_ORNAMENT = BLOCKS.registerSimpleBlock("byzantine_stone_ornament", BlockBehaviour.Properties.of().mapColor(MapColor.STONE).strength(2.0f, 6.0f).sound(SoundType.STONE).requiresCorrectToolForDrops());
    public static final DeferredBlock<Block> BYZANTINE_SANDSTONE_ORNAMENT = BLOCKS.registerSimpleBlock("byzantine_sandstone_ornament", BlockBehaviour.Properties.of().mapColor(MapColor.SAND).strength(2.0f, 6.0f).sound(SoundType.STONE).requiresCorrectToolForDrops());
    public static final DeferredBlock<StairBlock> BYZANTINE_TILES_STAIRS = BLOCKS.register("byzantine_tiles_stairs", key -> new StairBlock(((HorizontalRotatedPillarBlock)((Object)((Object)BYZANTINE_TILES.get()))).defaultBlockState(), BlockBehaviour.Properties.ofFullCopy((BlockBehaviour)((BlockBehaviour)BYZANTINE_TILES.get()))));
    public static final DeferredBlock<SlabBlock> BYZANTINE_TILES_SLAB = BLOCKS.register("byzantine_tiles_slab", key -> new SlabBlock(BlockBehaviour.Properties.ofFullCopy((BlockBehaviour)((BlockBehaviour)BYZANTINE_TILES.get()))));
    public static final DeferredBlock<Block> BYZANTINE_MOSAIC_RED = BLOCKS.registerSimpleBlock("byzantine_mosaic_red", BlockBehaviour.Properties.of().mapColor(MapColor.COLOR_RED).strength(2.0f, 6.0f).sound(SoundType.STONE).requiresCorrectToolForDrops());
    public static final DeferredBlock<Block> BYZANTINE_MOSAIC_BLUE = BLOCKS.registerSimpleBlock("byzantine_mosaic_blue", BlockBehaviour.Properties.of().mapColor(MapColor.COLOR_BLUE).strength(2.0f, 6.0f).sound(SoundType.STONE).requiresCorrectToolForDrops());
    public static final DeferredBlock<OliveTreeSaplingBlock> OLIVE_TREE_SAPLING = BLOCKS.register("olive_tree_sapling", key -> new OliveTreeSaplingBlock(BlockBehaviour.Properties.of().mapColor(MapColor.PLANT).noCollission().randomTicks().instabreak().sound(SoundType.GRASS).pushReaction(PushReaction.DESTROY)));
    public static final DeferredBlock<OliveTreeLeavesBlock> OLIVE_TREE_LEAVES = BLOCKS.register("olive_tree_leaves", key -> new OliveTreeLeavesBlock(BlockBehaviour.Properties.of().mapColor(MapColor.PLANT).strength(0.2f).randomTicks().sound(SoundType.GRASS).noOcclusion().isValidSpawn((state, getter, pos, entityType) -> false).isSuffocating((state, getter, pos) -> false).isViewBlocking((state, getter, pos) -> false).ignitedByLava().pushReaction(PushReaction.DESTROY).isRedstoneConductor((state, getter, pos) -> false)));
    public static final DeferredBlock<BlockGrapeVine> CROP_VINE = BLOCKS.register("crop_vine", key -> new BlockGrapeVine(BlockBehaviour.Properties.of().noCollission().randomTicks().instabreak().sound(SoundType.CROP)));
    public static final DeferredBlock<BlockSilkWorm> SILK_WORM = BLOCKS.register("silk_worm", key -> new BlockSilkWorm(BlockBehaviour.Properties.of().mapColor(MapColor.WOOD).strength(2.0f, 5.0f).sound(SoundType.WOOD).randomTicks()));
    public static final DeferredBlock<BlockSnailSoil> SNAIL_SOIL = BLOCKS.register("snail_soil", key -> new BlockSnailSoil(BlockBehaviour.Properties.of().mapColor(MapColor.DIRT).strength(0.6f).sound(SoundType.GRAVEL).randomTicks()));
    public static final Map<DyeColor, DeferredBlock<Block>> PAINTED_BRICKS = new EnumMap<DyeColor, DeferredBlock<Block>>(DyeColor.class);
    public static final Map<DyeColor, DeferredBlock<Block>> DECORATED_BRICKS = new EnumMap<DyeColor, DeferredBlock<Block>>(DyeColor.class);
    public static final Map<DyeColor, DeferredBlock<Block>> PAINTED_BRICK_STAIRS = new EnumMap<DyeColor, DeferredBlock<Block>>(DyeColor.class);
    public static final Map<DyeColor, DeferredBlock<Block>> PAINTED_BRICK_SLABS = new EnumMap<DyeColor, DeferredBlock<Block>>(DyeColor.class);
    public static final Map<DyeColor, DeferredBlock<Block>> PAINTED_BRICK_WALLS = new EnumMap<DyeColor, DeferredBlock<Block>>(DyeColor.class);
    public static final DeferredBlock<IronBarsBlock> PAPER_WALL;
    public static final DeferredBlock<HorizontalRotatedPillarBlock> JAPANESE_TILES;
    public static final DeferredBlock<StairBlock> JAPANESE_TILES_STAIRS;
    public static final DeferredBlock<SlabBlock> JAPANESE_TILES_SLAB;
    public static final DeferredBlock<HorizontalRotatedPillarBlock> GRAY_TILES;
    public static final DeferredBlock<StairBlock> GRAY_TILES_STAIRS;
    public static final DeferredBlock<SlabBlock> GRAY_TILES_SLAB;
    public static final DeferredBlock<HorizontalRotatedPillarBlock> GREEN_TILES;
    public static final DeferredBlock<StairBlock> GREEN_TILES_STAIRS;
    public static final DeferredBlock<SlabBlock> GREEN_TILES_SLAB;
    public static final DeferredBlock<HorizontalRotatedPillarBlock> RED_TILES;
    public static final DeferredBlock<StairBlock> RED_TILES_STAIRS;
    public static final DeferredBlock<SlabBlock> RED_TILES_SLAB;
    public static final DeferredBlock<SlidingDoorBlock> WOODEN_SLIDING_DOOR;
    public static final DeferredBlock<SlidingDoorBlock> JAPANESE_SLIDING_DOOR;
    public static final DeferredBlock<Block> MUD_BRICK_SELJUK_ORNAMENTED;
    public static final DeferredBlock<Block> MUD_BRICK_SELJUK_DECORATED;
    public static final DeferredBlock<Block> MUD_BRICK_SMOOTH;
    public static final DeferredBlock<PistachioTreeSaplingBlock> PISTACHIO_TREE_SAPLING;
    public static final DeferredBlock<PistachioTreeLeavesBlock> PISTACHIO_TREE_LEAVES;
    public static final DeferredBlock<HorizontalRotatedPillarBlock> JAPANESE_STONE_TILES;
    public static final DeferredBlock<Block> SNOW_BRICK;
    public static final DeferredBlock<Block> ICE_BRICK;
    public static final DeferredBlock<WallBlock> SNOW_WALL;
    public static final DeferredBlock<Block> SOD_OAK;
    public static final DeferredBlock<Block> SOD_SPRUCE;
    public static final DeferredBlock<Block> SOD_BIRCH;
    public static final DeferredBlock<Block> SOD_JUNGLE;
    public static final DeferredBlock<Block> SOD_ACACIA;
    public static final DeferredBlock<Block> SOD_DARK_OAK;
    public static final DeferredBlock<IronBarsBlock> INUIT_CARVING;
    public static final DeferredBlock<FirePitBlock> FIRE_PIT;
    public static final DeferredBlock<LockedChestBlock> LOCKED_CHEST;
    public static final DeferredBlock<VillagePanelBlock> VILLAGE_PANEL;
    public static final DeferredBlock<MockMarkerBlock> MOCK_MARKER;
    public static final DeferredBlock<MockSoilBlock> MOCK_SOIL;
    public static final DeferredBlock<MockSourceBlock> MOCK_SOURCE;
    public static final DeferredBlock<MockFreeBlock> MOCK_FREE;
    public static final DeferredBlock<MockTreeSpawnBlock> MOCK_TREE_SPAWN;
    public static final DeferredBlock<MockAnimalSpawnBlock> MOCK_ANIMAL_SPAWN;
    public static final DeferredBlock<MockChestBlock> MOCK_CHEST;
    public static final DeferredBlock<MockDecorBlock> MOCK_DECOR;
    public static final DeferredBlock<MockFacingMarkerBlock> MOCK_FACING_MARKER;
    public static final DeferredBlock<MockBannerWallBlock> MOCK_BANNER_WALL;
    public static final DeferredBlock<MockBannerStandingBlock> MOCK_BANNER_STANDING;
    public static final DeferredBlock<ImportTableBlock> IMPORT_TABLE;

    private static DeferredBlock<Block> registerSod(String woodName) {
        return BLOCKS.registerSimpleBlock("sod_" + woodName, BlockBehaviour.Properties.of().mapColor(MapColor.DIRT).strength(1.0f).sound(SoundType.GRASS));
    }

    private ModBlocks() {
    }

    public static void register(IEventBus modEventBus) {
        BLOCKS.register(modEventBus);
    }

    static {
        for (DyeColor color : DyeColor.values()) {
            String name = "painted_brick_" + color.getSerializedName();
            PAINTED_BRICKS.put(color, (DeferredBlock<Block>)BLOCKS.register(name, key -> new PaintedBrickBlock(color, PaintedBrickBlock.BrickType.PLAIN, BlockBehaviour.Properties.of().mapColor(color.getMapColor()).strength(1.5f, 10.0f).sound(SoundType.STONE).requiresCorrectToolForDrops())));
            DECORATED_BRICKS.put(color, (DeferredBlock<Block>)BLOCKS.register("decorated_brick_" + color.getSerializedName(), key -> new PaintedBrickBlock(color, PaintedBrickBlock.BrickType.DECORATED, BlockBehaviour.Properties.of().mapColor(color.getMapColor()).strength(1.5f, 10.0f).sound(SoundType.STONE).requiresCorrectToolForDrops())));
            PAINTED_BRICK_STAIRS.put(color, (DeferredBlock<Block>)BLOCKS.register(name + "_stairs", key -> new PaintedBrickStairBlock(color, ((Block)PAINTED_BRICKS.get(color).get()).defaultBlockState(), BlockBehaviour.Properties.of().mapColor(color.getMapColor()).strength(1.5f, 10.0f).sound(SoundType.STONE).requiresCorrectToolForDrops())));
            PAINTED_BRICK_SLABS.put(color, (DeferredBlock<Block>)BLOCKS.register(name + "_slab", key -> new PaintedBrickSlabBlock(color, BlockBehaviour.Properties.of().mapColor(color.getMapColor()).strength(1.5f, 10.0f).sound(SoundType.STONE).requiresCorrectToolForDrops())));
            PAINTED_BRICK_WALLS.put(color, (DeferredBlock<Block>)BLOCKS.register(name + "_wall", key -> new PaintedBrickWallBlock(color, BlockBehaviour.Properties.of().mapColor(color.getMapColor()).strength(1.5f, 10.0f).sound(SoundType.STONE).requiresCorrectToolForDrops())));
        }
        PAPER_WALL = BLOCKS.register("paper_wall", key -> new IronBarsBlock(BlockBehaviour.Properties.of().mapColor(MapColor.WOOL).strength(0.3f).sound(SoundType.WOOL).noOcclusion()));
        JAPANESE_TILES = BLOCKS.register("japanese_tiles", key -> new HorizontalRotatedPillarBlock(BlockBehaviour.Properties.of().mapColor(MapColor.STONE).strength(2.0f, 10.0f).sound(SoundType.STONE).requiresCorrectToolForDrops()));
        JAPANESE_TILES_STAIRS = BLOCKS.register("japanese_tiles_stairs", key -> new StairBlock(((HorizontalRotatedPillarBlock)((Object)((Object)JAPANESE_TILES.get()))).defaultBlockState(), BlockBehaviour.Properties.ofFullCopy((BlockBehaviour)((BlockBehaviour)JAPANESE_TILES.get()))));
        JAPANESE_TILES_SLAB = BLOCKS.register("japanese_tiles_slab", key -> new SlabBlock(BlockBehaviour.Properties.ofFullCopy((BlockBehaviour)((BlockBehaviour)JAPANESE_TILES.get()))));
        GRAY_TILES = BLOCKS.register("gray_tiles", key -> new HorizontalRotatedPillarBlock(BlockBehaviour.Properties.of().mapColor(MapColor.COLOR_GRAY).strength(2.0f, 10.0f).sound(SoundType.STONE).requiresCorrectToolForDrops()));
        GRAY_TILES_STAIRS = BLOCKS.register("gray_tiles_stairs", key -> new StairBlock(((HorizontalRotatedPillarBlock)((Object)((Object)GRAY_TILES.get()))).defaultBlockState(), BlockBehaviour.Properties.ofFullCopy((BlockBehaviour)((BlockBehaviour)GRAY_TILES.get()))));
        GRAY_TILES_SLAB = BLOCKS.register("gray_tiles_slab", key -> new SlabBlock(BlockBehaviour.Properties.ofFullCopy((BlockBehaviour)((BlockBehaviour)GRAY_TILES.get()))));
        GREEN_TILES = BLOCKS.register("green_tiles", key -> new HorizontalRotatedPillarBlock(BlockBehaviour.Properties.of().mapColor(MapColor.COLOR_GREEN).strength(2.0f, 10.0f).sound(SoundType.STONE).requiresCorrectToolForDrops()));
        GREEN_TILES_STAIRS = BLOCKS.register("green_tiles_stairs", key -> new StairBlock(((HorizontalRotatedPillarBlock)((Object)((Object)GREEN_TILES.get()))).defaultBlockState(), BlockBehaviour.Properties.ofFullCopy((BlockBehaviour)((BlockBehaviour)GREEN_TILES.get()))));
        GREEN_TILES_SLAB = BLOCKS.register("green_tiles_slab", key -> new SlabBlock(BlockBehaviour.Properties.ofFullCopy((BlockBehaviour)((BlockBehaviour)GREEN_TILES.get()))));
        RED_TILES = BLOCKS.register("red_tiles", key -> new HorizontalRotatedPillarBlock(BlockBehaviour.Properties.of().mapColor(MapColor.COLOR_RED).strength(2.0f, 10.0f).sound(SoundType.STONE).requiresCorrectToolForDrops()));
        RED_TILES_STAIRS = BLOCKS.register("red_tiles_stairs", key -> new StairBlock(((HorizontalRotatedPillarBlock)((Object)((Object)RED_TILES.get()))).defaultBlockState(), BlockBehaviour.Properties.ofFullCopy((BlockBehaviour)((BlockBehaviour)RED_TILES.get()))));
        RED_TILES_SLAB = BLOCKS.register("red_tiles_slab", key -> new SlabBlock(BlockBehaviour.Properties.ofFullCopy((BlockBehaviour)((BlockBehaviour)RED_TILES.get()))));
        WOODEN_SLIDING_DOOR = BLOCKS.register("wooden_sliding_door", key -> new SlidingDoorBlock(BlockSetType.OAK, BlockBehaviour.Properties.of().mapColor(MapColor.WOOD).strength(0.1f).sound(SoundType.WOOD).noOcclusion()));
        JAPANESE_SLIDING_DOOR = BLOCKS.register("japanese_sliding_door", key -> new SlidingDoorBlock(BlockSetType.OAK, BlockBehaviour.Properties.of().mapColor(MapColor.WOOL).strength(0.1f).sound(SoundType.WOOL).noOcclusion()));
        MUD_BRICK_SELJUK_ORNAMENTED = BLOCKS.registerSimpleBlock("mud_brick_seljuk_ornamented", BlockBehaviour.Properties.of().mapColor(MapColor.STONE).strength(1.5f, 10.0f).sound(SoundType.STONE).requiresCorrectToolForDrops());
        MUD_BRICK_SELJUK_DECORATED = BLOCKS.registerSimpleBlock("mud_brick_seljuk_decorated", BlockBehaviour.Properties.of().mapColor(MapColor.STONE).strength(1.5f, 10.0f).sound(SoundType.STONE).requiresCorrectToolForDrops());
        MUD_BRICK_SMOOTH = BLOCKS.registerSimpleBlock("mud_brick_smooth", BlockBehaviour.Properties.of().mapColor(MapColor.STONE).strength(1.5f, 10.0f).sound(SoundType.STONE).requiresCorrectToolForDrops());
        PISTACHIO_TREE_SAPLING = BLOCKS.register("pistachio_tree_sapling", key -> new PistachioTreeSaplingBlock(BlockBehaviour.Properties.of().mapColor(MapColor.PLANT).noCollission().randomTicks().instabreak().sound(SoundType.GRASS).pushReaction(PushReaction.DESTROY)));
        PISTACHIO_TREE_LEAVES = BLOCKS.register("pistachio_tree_leaves", key -> new PistachioTreeLeavesBlock(BlockBehaviour.Properties.of().mapColor(MapColor.PLANT).strength(0.2f).randomTicks().sound(SoundType.GRASS).noOcclusion().isValidSpawn((state, getter, pos, entityType) -> false).isSuffocating((state, getter, pos) -> false).isViewBlocking((state, getter, pos) -> false).ignitedByLava().pushReaction(PushReaction.DESTROY).isRedstoneConductor((state, getter, pos) -> false)));
        JAPANESE_STONE_TILES = BLOCKS.register("japanese_stone_tiles", key -> new HorizontalRotatedPillarBlock(BlockBehaviour.Properties.of().mapColor(MapColor.STONE).strength(2.0f, 10.0f).sound(SoundType.STONE).requiresCorrectToolForDrops()));
        SNOW_BRICK = BLOCKS.registerSimpleBlock("snow_brick", BlockBehaviour.Properties.of().mapColor(MapColor.SNOW).strength(0.4f).sound(SoundType.SNOW));
        ICE_BRICK = BLOCKS.registerSimpleBlock("ice_brick", BlockBehaviour.Properties.of().mapColor(MapColor.ICE).strength(0.5f).sound(SoundType.GLASS).friction(0.98f));
        SNOW_WALL = BLOCKS.register("snow_wall", key -> new WallBlock(BlockBehaviour.Properties.of().mapColor(MapColor.SNOW).strength(0.4f).sound(SoundType.SNOW)));
        SOD_OAK = ModBlocks.registerSod("oak");
        SOD_SPRUCE = ModBlocks.registerSod("spruce");
        SOD_BIRCH = ModBlocks.registerSod("birch");
        SOD_JUNGLE = ModBlocks.registerSod("jungle");
        SOD_ACACIA = ModBlocks.registerSod("acacia");
        SOD_DARK_OAK = ModBlocks.registerSod("dark_oak");
        INUIT_CARVING = BLOCKS.register("inuit_carving", key -> new IronBarsBlock(BlockBehaviour.Properties.of().mapColor(MapColor.ICE).strength(0.5f).sound(SoundType.SNOW).noOcclusion()));
        FIRE_PIT = BLOCKS.register("fire_pit", key -> new FirePitBlock(BlockBehaviour.Properties.of().mapColor(MapColor.WOOD).strength(0.2f).sound(SoundType.WOOD).lightLevel(state -> (Boolean)state.getValue((Property)FirePitBlock.LIT) != false ? 15 : 0).noOcclusion()));
        LOCKED_CHEST = BLOCKS.register("locked_chest", key -> new LockedChestBlock(BlockBehaviour.Properties.of().mapColor(MapColor.WOOD).strength(50.0f, 2000.0f).sound(SoundType.WOOD)));
        VILLAGE_PANEL = BLOCKS.register("village_panel", key -> new VillagePanelBlock(BlockBehaviour.Properties.of().mapColor(MapColor.WOOD).strength(2.0f, 5.0f).sound(SoundType.WOOD).noOcclusion()));
        MOCK_MARKER = BLOCKS.register("mock_marker", key -> new MockMarkerBlock(MockBlock.mockProperties()));
        MOCK_SOIL = BLOCKS.register("mock_soil", key -> new MockSoilBlock(MockBlock.mockProperties()));
        MOCK_SOURCE = BLOCKS.register("mock_source", key -> new MockSourceBlock(MockBlock.mockProperties()));
        MOCK_FREE = BLOCKS.register("mock_free", key -> new MockFreeBlock(MockBlock.mockProperties()));
        MOCK_TREE_SPAWN = BLOCKS.register("mock_tree_spawn", key -> new MockTreeSpawnBlock(MockBlock.mockProperties()));
        MOCK_ANIMAL_SPAWN = BLOCKS.register("mock_animal_spawn", key -> new MockAnimalSpawnBlock(MockBlock.mockProperties()));
        MOCK_CHEST = BLOCKS.register("mock_chest", key -> new MockChestBlock(MockBlock.mockProperties()));
        MOCK_DECOR = BLOCKS.register("mock_decor", key -> new MockDecorBlock(MockBlock.mockProperties()));
        MOCK_FACING_MARKER = BLOCKS.register("mock_facing_marker", key -> new MockFacingMarkerBlock(MockBlock.mockProperties()));
        MOCK_BANNER_WALL = BLOCKS.register("mock_banner_wall", key -> new MockBannerWallBlock(MockBlock.mockProperties()));
        MOCK_BANNER_STANDING = BLOCKS.register("mock_banner_standing", key -> new MockBannerStandingBlock(MockBlock.mockProperties()));
        IMPORT_TABLE = BLOCKS.register("import_table", key -> new ImportTableBlock(BlockBehaviour.Properties.of().mapColor(MapColor.WOOD).strength(1.0f).sound(SoundType.WOOD)));
    }
}

