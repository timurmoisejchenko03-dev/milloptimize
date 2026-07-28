/*
 * Decompiled with CFR 0.150.
 * 
 * Could not load the following classes:
 *  net.minecraft.core.Direction$Axis
 *  net.minecraft.data.PackOutput
 *  net.minecraft.resources.ResourceLocation
 *  net.minecraft.world.item.DyeColor
 *  net.minecraft.world.level.block.Block
 *  net.minecraft.world.level.block.RotatedPillarBlock
 *  net.minecraft.world.level.block.SlabBlock
 *  net.minecraft.world.level.block.StairBlock
 *  net.minecraft.world.level.block.WallBlock
 *  net.minecraft.world.level.block.state.properties.Property
 *  net.neoforged.neoforge.client.model.generators.BlockModelBuilder
 *  net.neoforged.neoforge.client.model.generators.BlockStateProvider
 *  net.neoforged.neoforge.client.model.generators.ModelBuilder
 *  net.neoforged.neoforge.client.model.generators.ModelFile
 *  net.neoforged.neoforge.client.model.generators.ModelFile$UncheckedModelFile
 *  net.neoforged.neoforge.client.model.generators.VariantBlockStateBuilder
 *  net.neoforged.neoforge.common.data.ExistingFileHelper
 *  net.neoforged.neoforge.registries.DeferredBlock
 */
package org.millenaire.datagen;

import net.minecraft.core.Direction;
import net.minecraft.data.PackOutput;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.RotatedPillarBlock;
import net.minecraft.world.level.block.SlabBlock;
import net.minecraft.world.level.block.StairBlock;
import net.minecraft.world.level.block.WallBlock;
import net.minecraft.world.level.block.state.properties.Property;
import net.neoforged.neoforge.client.model.generators.BlockModelBuilder;
import net.neoforged.neoforge.client.model.generators.BlockStateProvider;
import net.neoforged.neoforge.client.model.generators.ModelBuilder;
import net.neoforged.neoforge.client.model.generators.ModelFile;
import net.neoforged.neoforge.client.model.generators.VariantBlockStateBuilder;
import net.neoforged.neoforge.common.data.ExistingFileHelper;
import net.neoforged.neoforge.registries.DeferredBlock;
import org.millenaire.block.ModBlocks;

public class MillBlockStateProvider
extends BlockStateProvider {
    public MillBlockStateProvider(PackOutput output, ExistingFileHelper existingFileHelper) {
        super(output, "millenaire", existingFileHelper);
    }

    protected void registerStatesAndModels() {
        this.registerPaintedBricks();
        this.registerNormanBlocks();
        this.registerIndianBlocks();
        this.registerMayanBlocks();
        this.registerByzantineBlocks();
        this.registerJapaneseBlocks();
        this.registerSeljukBlocks();
        this.registerInuitBlocks();
    }

    private void registerPaintedBricks() {
        for (DyeColor color : DyeColor.values()) {
            String colorName = color.getSerializedName();
            ResourceLocation texture = this.modLoc("block/painted_brick_" + colorName);
            this.simpleBlockWithItem((Block)ModBlocks.PAINTED_BRICKS.get((Object)color).get(), this.cubeAll((Block)ModBlocks.PAINTED_BRICKS.get((Object)color).get()));
            ResourceLocation decoratedTexture = this.modLoc("block/decorated_brick_" + colorName);
            ModelBuilder decoratedModel = ((BlockModelBuilder)this.models().cube("decorated_brick_" + colorName, texture, texture, decoratedTexture, decoratedTexture, decoratedTexture, decoratedTexture)).texture("particle", decoratedTexture);
            this.simpleBlockWithItem((Block)ModBlocks.DECORATED_BRICKS.get((Object)color).get(), (ModelFile)decoratedModel);
            this.stairsWithItem(ModBlocks.PAINTED_BRICK_STAIRS.get((Object)color), texture);
            this.slabWithItem(ModBlocks.PAINTED_BRICK_SLABS.get((Object)color), "painted_brick_" + colorName + "_slab", texture, this.modLoc("block/painted_brick_" + colorName));
            this.wallWithItem(ModBlocks.PAINTED_BRICK_WALLS.get((Object)color), "painted_brick_" + colorName + "_wall", texture);
        }
    }

    private void registerNormanBlocks() {
        this.simpleCubeWithItem(ModBlocks.TIMBER_FRAME_PLAIN);
        this.simpleCubeWithItem(ModBlocks.TIMBER_FRAME_CROSS);
        this.stairsWithItem(ModBlocks.TIMBER_FRAME_STAIRS, this.modLoc("block/timber_frame_plain"));
        this.slabWithItem(ModBlocks.TIMBER_FRAME_SLAB, "timber_frame_slab", this.modLoc("block/timber_frame_plain"), this.modLoc("block/timber_frame_plain"));
        this.simpleCubeWithItem(ModBlocks.THATCH);
        this.stairsWithItem(ModBlocks.THATCH_STAIRS, this.modLoc("block/thatch"));
        this.slabWithItem(ModBlocks.THATCH_SLAB, "thatch_slab", this.modLoc("block/thatch"), this.modLoc("block/thatch"));
        this.simpleCubeWithItem(ModBlocks.MUD_BRICK);
        this.stairsWithItem(ModBlocks.MUD_BRICK_STAIRS, this.modLoc("block/mud_brick"));
        this.slabWithItem(ModBlocks.MUD_BRICK_SLAB, "mud_brick_slab", this.modLoc("block/mud_brick"), this.modLoc("block/mud_brick"));
        this.wallWithItem(ModBlocks.MUD_BRICK_WALL, "mud_brick_wall", this.modLoc("block/mud_brick"));
        this.simpleCubeWithItem(ModBlocks.DIRT_WALL);
    }

    private void registerIndianBlocks() {
        this.simpleCubeWithItem(ModBlocks.SANDSTONE_CARVED);
        this.stairsWithItem(ModBlocks.SANDSTONE_CARVED_STAIRS, this.modLoc("block/sandstone_carved"));
        this.slabWithItem(ModBlocks.SANDSTONE_CARVED_SLAB, "sandstone_carved_slab", this.modLoc("block/sandstone_carved"), this.modLoc("block/sandstone_carved"));
        this.wallWithItem(ModBlocks.SANDSTONE_CARVED_WALL, "sandstone_carved_wall", this.modLoc("block/sandstone_carved"));
        this.simpleCubeWithItem(ModBlocks.RED_SANDSTONE_CARVED);
        this.stairsWithItem(ModBlocks.RED_SANDSTONE_CARVED_STAIRS, this.modLoc("block/red_sandstone_carved"));
        this.slabWithItem(ModBlocks.RED_SANDSTONE_CARVED_SLAB, "red_sandstone_carved_slab", this.modLoc("block/red_sandstone_carved"), this.modLoc("block/red_sandstone_carved"));
        this.wallWithItem(ModBlocks.RED_SANDSTONE_CARVED_WALL, "red_sandstone_carved_wall", this.modLoc("block/red_sandstone_carved"));
        this.simpleCubeWithItem(ModBlocks.OCHRE_SANDSTONE_CARVED);
        this.stairsWithItem(ModBlocks.OCHRE_SANDSTONE_CARVED_STAIRS, this.modLoc("block/ochre_sandstone_carved"));
        this.slabWithItem(ModBlocks.OCHRE_SANDSTONE_CARVED_SLAB, "ochre_sandstone_carved_slab", this.modLoc("block/ochre_sandstone_carved"), this.modLoc("block/ochre_sandstone_carved"));
        this.wallWithItem(ModBlocks.OCHRE_SANDSTONE_CARVED_WALL, "ochre_sandstone_carved_wall", this.modLoc("block/ochre_sandstone_carved"));
    }

    private void registerMayanBlocks() {
        this.simpleCubeWithItem(ModBlocks.MAYAN_GOLD_BLOCK);
    }

    private void registerByzantineBlocks() {
        ResourceLocation top = this.modLoc("block/byzantine_tiles_top");
        ResourceLocation side = this.modLoc("block/byzantine_tiles_side");
        ModelBuilder tilesModel = ((BlockModelBuilder)this.models().cube("byzantine_tiles", top, top, side, side, top, top)).texture("particle", top);
        this.horizontalAxisBlock(ModBlocks.BYZANTINE_TILES, (ModelFile)tilesModel);
        this.stairsWithItem(ModBlocks.BYZANTINE_TILES_STAIRS, side);
        this.slabWithItem(ModBlocks.BYZANTINE_TILES_SLAB, "byzantine_tiles_slab", side, this.modLoc("block/byzantine_tiles"));
        this.simpleCubeWithItem(ModBlocks.BYZANTINE_MOSAIC_RED);
        this.simpleCubeWithItem(ModBlocks.BYZANTINE_MOSAIC_BLUE);
    }

    private void registerJapaneseBlocks() {
        this.directionalTilesWithItem(ModBlocks.JAPANESE_TILES, "japanese_tiles");
        this.stairsWithItem(ModBlocks.JAPANESE_TILES_STAIRS, this.modLoc("block/japanese_tiles_0"));
        this.slabWithItem(ModBlocks.JAPANESE_TILES_SLAB, "japanese_tiles_slab", this.modLoc("block/japanese_tiles_0"), this.modLoc("block/japanese_tiles"));
        ModelBuilder japaneseStoneTilesModel = this.models().cubeColumn("japanese_stone_tiles", this.modLoc("block/japanese_tiles_half_front"), this.modLoc("block/japanese_tiles_top"));
        this.horizontalAxisBlock(ModBlocks.JAPANESE_STONE_TILES, (ModelFile)japaneseStoneTilesModel);
        this.directionalTilesWithItem(ModBlocks.GRAY_TILES, "gray_tiles");
        this.stairsWithItem(ModBlocks.GRAY_TILES_STAIRS, this.modLoc("block/gray_tiles_0"));
        this.slabWithItem(ModBlocks.GRAY_TILES_SLAB, "gray_tiles_slab", this.modLoc("block/gray_tiles_0"), this.modLoc("block/gray_tiles"));
        this.directionalTilesWithItem(ModBlocks.GREEN_TILES, "green_tiles");
        this.stairsWithItem(ModBlocks.GREEN_TILES_STAIRS, this.modLoc("block/green_tiles_0"));
        this.slabWithItem(ModBlocks.GREEN_TILES_SLAB, "green_tiles_slab", this.modLoc("block/green_tiles_0"), this.modLoc("block/green_tiles"));
        this.directionalTilesWithItem(ModBlocks.RED_TILES, "red_tiles");
        this.stairsWithItem(ModBlocks.RED_TILES_STAIRS, this.modLoc("block/red_tiles_0"));
        this.slabWithItem(ModBlocks.RED_TILES_SLAB, "red_tiles_slab", this.modLoc("block/red_tiles_0"), this.modLoc("block/red_tiles"));
    }

    private void registerSeljukBlocks() {
        this.simpleCubeWithItem(ModBlocks.MUD_BRICK_SELJUK_ORNAMENTED);
        this.simpleCubeWithItem(ModBlocks.MUD_BRICK_SELJUK_DECORATED);
        this.simpleCubeWithItem(ModBlocks.MUD_BRICK_SMOOTH);
    }

    private void registerInuitBlocks() {
        this.simpleCubeWithItem(ModBlocks.SNOW_BRICK);
        this.simpleCubeWithItem(ModBlocks.ICE_BRICK);
        this.wallWithItem(ModBlocks.SNOW_WALL, "snow_wall", this.modLoc("block/snow_brick"));
        this.sodBlockWithItem(ModBlocks.SOD_OAK, "sod_oak", this.mcLoc("block/oak_planks"));
        this.sodBlockWithItem(ModBlocks.SOD_SPRUCE, "sod_spruce", this.mcLoc("block/spruce_planks"));
        this.sodBlockWithItem(ModBlocks.SOD_BIRCH, "sod_birch", this.mcLoc("block/birch_planks"));
        this.sodBlockWithItem(ModBlocks.SOD_JUNGLE, "sod_jungle", this.mcLoc("block/jungle_planks"));
        this.sodBlockWithItem(ModBlocks.SOD_ACACIA, "sod_acacia", this.mcLoc("block/acacia_planks"));
        this.sodBlockWithItem(ModBlocks.SOD_DARK_OAK, "sod_dark_oak", this.mcLoc("block/dark_oak_planks"));
    }

    private <T extends Block> void simpleCubeWithItem(DeferredBlock<T> block) {
        this.simpleBlockWithItem((Block)block.get(), this.cubeAll((Block)block.get()));
    }

    private void sodBlockWithItem(DeferredBlock<Block> block, String name, ResourceLocation planks) {
        ModelBuilder model = this.models().cubeBottomTop(name, this.modLoc("block/" + name), planks, planks);
        this.simpleBlockWithItem((Block)block.get(), (ModelFile)model);
    }

    private void cubeColumnWithItem(DeferredBlock<? extends RotatedPillarBlock> block, String name, ResourceLocation end, ResourceLocation side) {
        ModelBuilder model = this.models().cubeColumn(name, side, end);
        this.axisBlock((RotatedPillarBlock)block.get(), (ModelFile)model, (ModelFile)model);
        this.simpleBlockItem((Block)block.get(), (ModelFile)model);
    }

    private void directionalTilesWithItem(DeferredBlock<? extends RotatedPillarBlock> block, String baseName) {
        ResourceLocation tex0 = this.modLoc("block/" + baseName + "_0");
        ResourceLocation tex90p = this.modLoc("block/" + baseName + "_90p");
        ResourceLocation tex90n = this.modLoc("block/" + baseName + "_90n");
        ResourceLocation tex180 = this.modLoc("block/" + baseName + "_180");
        ModelBuilder model = ((BlockModelBuilder)this.models().cube(baseName, tex0, tex0, tex180, tex0, tex90p, tex90n)).texture("particle", tex0);
        this.horizontalAxisBlock(block, (ModelFile)model);
    }

    private void horizontalAxisBlock(DeferredBlock<? extends RotatedPillarBlock> block, ModelFile model) {
        ((VariantBlockStateBuilder)((VariantBlockStateBuilder)this.getVariantBuilder((Block)block.get()).partialState().with((Property)RotatedPillarBlock.AXIS, (Comparable)Direction.Axis.Y).modelForState().modelFile(model).addModel()).partialState().with((Property)RotatedPillarBlock.AXIS, (Comparable)Direction.Axis.Z).modelForState().modelFile(model).rotationY(90).addModel()).partialState().with((Property)RotatedPillarBlock.AXIS, (Comparable)Direction.Axis.X).modelForState().modelFile(model).addModel();
        this.simpleBlockItem((Block)block.get(), model);
    }

    private void stairsWithItem(DeferredBlock<? extends Block> block, ResourceLocation texture) {
        this.stairsBlock((StairBlock)block.get(), texture);
        this.simpleBlockItem((Block)block.get(), (ModelFile)new ModelFile.UncheckedModelFile(this.modLoc("block/" + block.getId().getPath())));
    }

    private void slabWithItem(DeferredBlock<? extends Block> block, String name, ResourceLocation texture, ResourceLocation fullBlockModel) {
        ModelBuilder slabBottom = this.models().slab(name, texture, texture, texture);
        ModelBuilder slabTop = this.models().slabTop(name + "_top", texture, texture, texture);
        ModelFile.UncheckedModelFile slabDouble = new ModelFile.UncheckedModelFile(fullBlockModel);
        this.slabBlock((SlabBlock)block.get(), (ModelFile)slabBottom, (ModelFile)slabTop, (ModelFile)slabDouble);
        this.simpleBlockItem((Block)block.get(), (ModelFile)slabBottom);
    }

    private void wallWithItem(DeferredBlock<? extends Block> block, String name, ResourceLocation texture) {
        this.wallBlock((WallBlock)block.get(), texture);
        this.itemModels().wallInventory(name, texture);
    }
}

