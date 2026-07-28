/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  net.minecraft.core.component.DataComponents
 *  net.minecraft.core.registries.Registries
 *  net.minecraft.network.chat.Component
 *  net.minecraft.resources.ResourceKey
 *  net.minecraft.world.item.CreativeModeTab
 *  net.minecraft.world.item.DyeColor
 *  net.minecraft.world.item.ItemStack
 *  net.minecraft.world.item.component.BlockItemStateProperties
 *  net.minecraft.world.item.component.CustomModelData
 *  net.minecraft.world.level.ItemLike
 *  net.minecraft.world.level.block.Block
 *  net.minecraft.world.level.block.state.properties.EnumProperty
 *  net.neoforged.bus.api.IEventBus
 *  net.neoforged.neoforge.registries.DeferredHolder
 *  net.neoforged.neoforge.registries.DeferredRegister
 */
package org.millenaire.item;

import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.BlockItemStateProperties;
import net.minecraft.world.item.component.CustomModelData;
import net.minecraft.world.level.ItemLike;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;
import org.millenaire.block.ModBlocks;
import org.millenaire.block.mock.AnimalSpawnType;
import org.millenaire.block.mock.BannerSubtype;
import org.millenaire.block.mock.CropType;
import org.millenaire.block.mock.FacingMarkerType;
import org.millenaire.block.mock.FreeBlockType;
import org.millenaire.block.mock.MarkerType;
import org.millenaire.block.mock.MockAnimalSpawnBlock;
import org.millenaire.block.mock.MockBannerStandingBlock;
import org.millenaire.block.mock.MockBannerWallBlock;
import org.millenaire.block.mock.MockChestBlock;
import org.millenaire.block.mock.MockChestType;
import org.millenaire.block.mock.MockDecorBlock;
import org.millenaire.block.mock.MockFacingMarkerBlock;
import org.millenaire.block.mock.MockFreeBlock;
import org.millenaire.block.mock.MockMarkerBlock;
import org.millenaire.block.mock.MockSoilBlock;
import org.millenaire.block.mock.MockSourceBlock;
import org.millenaire.block.mock.MockTreeSpawnBlock;
import org.millenaire.block.mock.SourceType;
import org.millenaire.block.mock.TreeSpawnType;
import org.millenaire.item.ModItems;

public final class ModCreativeTabs {
    public static final DeferredRegister<CreativeModeTab> TABS = DeferredRegister.create((ResourceKey)Registries.CREATIVE_MODE_TAB, (String)"millenaire");
    public static final DeferredHolder<CreativeModeTab, CreativeModeTab> MILLENAIRE_TAB = TABS.register("millenaire_tab", () -> CreativeModeTab.builder().title((Component)Component.translatable((String)"itemGroup.millenaire.millenaire_tab")).icon(() -> new ItemStack((ItemLike)ModItems.DENIER_OR.get())).displayItems((params, output) -> {
        output.accept((ItemLike)ModItems.DENIER.get());
        output.accept((ItemLike)ModItems.DENIER_ARGENT.get());
        output.accept((ItemLike)ModItems.DENIER_OR.get());
        output.accept((ItemLike)ModItems.PURSE.get());
        output.accept((ItemLike)ModItems.SUMMONING_WAND.get());
        output.accept((ItemLike)ModItems.NEGATION_WAND.get());
        output.accept((ItemLike)ModItems.VILLAGE_SCROLL.get());
        output.accept((ItemLike)ModItems.TRAVEL_BOOK.get());
        output.accept((ItemLike)ModItems.NORMAN_SWORD.get());
        output.accept((ItemLike)ModItems.NORMAN_PICKAXE.get());
        output.accept((ItemLike)ModItems.NORMAN_AXE.get());
        output.accept((ItemLike)ModItems.NORMAN_SHOVEL.get());
        output.accept((ItemLike)ModItems.NORMAN_HOE.get());
        output.accept((ItemLike)ModItems.NORMAN_HELMET.get());
        output.accept((ItemLike)ModItems.NORMAN_CHESTPLATE.get());
        output.accept((ItemLike)ModItems.NORMAN_LEGGINGS.get());
        output.accept((ItemLike)ModItems.NORMAN_BOOTS.get());
        output.accept((ItemLike)ModBlocks.TIMBER_FRAME_PLAIN.get());
        output.accept((ItemLike)ModBlocks.TIMBER_FRAME_CROSS.get());
        output.accept((ItemLike)ModBlocks.THATCH.get());
        output.accept((ItemLike)ModBlocks.MUD_BRICK.get());
        output.accept((ItemLike)ModBlocks.DIRT_WALL.get());
        output.accept((ItemLike)ModBlocks.TIMBER_FRAME_STAIRS.get());
        output.accept((ItemLike)ModBlocks.THATCH_STAIRS.get());
        output.accept((ItemLike)ModBlocks.MUD_BRICK_STAIRS.get());
        output.accept((ItemLike)ModBlocks.TIMBER_FRAME_SLAB.get());
        output.accept((ItemLike)ModBlocks.THATCH_SLAB.get());
        output.accept((ItemLike)ModBlocks.MUD_BRICK_SLAB.get());
        output.accept((ItemLike)ModBlocks.MUD_BRICK_WALL.get());
        output.accept((ItemLike)ModBlocks.STRAW_BED.get());
        output.accept((ItemLike)ModItems.TAPESTRY.get());
        output.accept((ItemLike)ModItems.INDIAN_STATUE.get());
        output.accept((ItemLike)ModItems.MAYAN_STATUE.get());
        output.accept((ItemLike)ModItems.BYZANTINE_ICON_SMALL.get());
        output.accept((ItemLike)ModItems.BYZANTINE_ICON_MEDIUM.get());
        output.accept((ItemLike)ModItems.BYZANTINE_ICON_LARGE.get());
        output.accept((ItemLike)ModItems.HIDE_HANGING.get());
        output.accept((ItemLike)ModBlocks.STAINED_GLASS_WHITE.get());
        output.accept((ItemLike)ModBlocks.STAINED_GLASS_YELLOW.get());
        output.accept((ItemLike)ModBlocks.STAINED_GLASS_YELLOW_RED.get());
        output.accept((ItemLike)ModBlocks.STAINED_GLASS_RED_BLUE.get());
        output.accept((ItemLike)ModBlocks.STAINED_GLASS_GREEN_BLUE.get());
        output.accept((ItemLike)ModBlocks.ROSETTE.get());
        output.accept((ItemLike)ModBlocks.WOODEN_BARS_ROSETTE.get());
        output.accept((ItemLike)ModBlocks.WOODEN_BARS.get());
        output.accept((ItemLike)ModBlocks.PATH_DIRT.get());
        output.accept((ItemLike)ModBlocks.PATH_DIRT_SLAB.get());
        output.accept((ItemLike)ModBlocks.PATH_GRAVEL.get());
        output.accept((ItemLike)ModBlocks.PATH_GRAVEL_SLAB.get());
        output.accept((ItemLike)ModBlocks.PATH_SLABS.get());
        output.accept((ItemLike)ModBlocks.PATH_SLABS_SLAB.get());
        output.accept((ItemLike)ModBlocks.PATH_OCHRE_TILES.get());
        output.accept((ItemLike)ModBlocks.PATH_OCHRE_TILES_SLAB.get());
        output.accept((ItemLike)ModBlocks.PATH_SANDSTONE.get());
        output.accept((ItemLike)ModBlocks.PATH_SANDSTONE_SLAB.get());
        output.accept((ItemLike)ModBlocks.PATH_GRAVEL_SLABS.get());
        output.accept((ItemLike)ModBlocks.PATH_GRAVEL_SLABS_SLAB.get());
        output.accept((ItemLike)ModBlocks.PATH_SNOW.get());
        output.accept((ItemLike)ModBlocks.PATH_SNOW_SLAB.get());
        output.accept((ItemLike)ModBlocks.APPLE_TREE_SAPLING.get());
        output.accept((ItemLike)ModBlocks.APPLE_TREE_LEAVES.get());
        output.accept((ItemLike)ModItems.CIDERAPPLE.get());
        output.accept((ItemLike)ModItems.CIDER.get());
        output.accept((ItemLike)ModItems.CALVA.get());
        output.accept((ItemLike)ModItems.BOUDIN.get());
        output.accept((ItemLike)ModItems.TRIPES.get());
        output.accept((ItemLike)ModItems.SILK.get());
        output.accept((ItemLike)ModItems.COTTON.get());
        output.accept((ItemLike)ModItems.RICE.get());
        output.accept((ItemLike)ModItems.ULU.get());
        for (DyeColor color : DyeColor.values()) {
            output.accept((ItemLike)ModItems.PAINT_BUCKETS.get(color).get());
        }
        for (DyeColor color : DyeColor.values()) {
            output.accept((ItemLike)ModBlocks.PAINTED_BRICKS.get(color).get());
            output.accept((ItemLike)ModBlocks.DECORATED_BRICKS.get(color).get());
            output.accept((ItemLike)ModBlocks.PAINTED_BRICK_STAIRS.get(color).get());
            output.accept((ItemLike)ModBlocks.PAINTED_BRICK_SLABS.get(color).get());
            output.accept((ItemLike)ModBlocks.PAINTED_BRICK_WALLS.get(color).get());
        }
        output.accept((ItemLike)ModItems.VEGCURRY.get());
        output.accept((ItemLike)ModItems.CHICKENCURRY.get());
        output.accept((ItemLike)ModItems.RASGULLA.get());
        output.accept((ItemLike)ModItems.TURMERIC.get());
        output.accept((ItemLike)ModItems.BRICK_MOULD.get());
        output.accept((ItemLike)ModBlocks.WET_BRICK.get());
        output.accept((ItemLike)ModBlocks.SANDSTONE_CARVED.get());
        output.accept((ItemLike)ModBlocks.SANDSTONE_CARVED_STAIRS.get());
        output.accept((ItemLike)ModBlocks.SANDSTONE_CARVED_SLAB.get());
        output.accept((ItemLike)ModBlocks.SANDSTONE_CARVED_WALL.get());
        output.accept((ItemLike)ModBlocks.RED_SANDSTONE_CARVED.get());
        output.accept((ItemLike)ModBlocks.RED_SANDSTONE_CARVED_STAIRS.get());
        output.accept((ItemLike)ModBlocks.RED_SANDSTONE_CARVED_SLAB.get());
        output.accept((ItemLike)ModBlocks.RED_SANDSTONE_CARVED_WALL.get());
        output.accept((ItemLike)ModBlocks.OCHRE_SANDSTONE_CARVED.get());
        output.accept((ItemLike)ModBlocks.OCHRE_SANDSTONE_CARVED_STAIRS.get());
        output.accept((ItemLike)ModBlocks.OCHRE_SANDSTONE_CARVED_SLAB.get());
        output.accept((ItemLike)ModBlocks.OCHRE_SANDSTONE_CARVED_WALL.get());
        output.accept((ItemLike)ModBlocks.WOODEN_BARS_INDIAN.get());
        output.accept((ItemLike)ModBlocks.CHARPOY.get());
        output.accept((ItemLike)ModItems.MAYAN_MACE.get());
        output.accept((ItemLike)ModItems.MAYAN_PICKAXE.get());
        output.accept((ItemLike)ModItems.MAYAN_AXE.get());
        output.accept((ItemLike)ModItems.MAYAN_SHOVEL.get());
        output.accept((ItemLike)ModItems.MAYAN_HOE.get());
        output.accept((ItemLike)ModItems.MASA.get());
        output.accept((ItemLike)ModItems.WAH.get());
        output.accept((ItemLike)ModItems.CACAUHAA.get());
        output.accept((ItemLike)ModItems.MAIZE.get());
        output.accept((ItemLike)ModItems.OBSIDIAN_FLAKE.get());
        output.accept((ItemLike)ModItems.MAYAN_PATTERN.get());
        output.accept((ItemLike)ModItems.MAYAN_PATTERN_1.get());
        output.accept((ItemLike)ModItems.MAYAN_PATTERN_2.get());
        output.accept((ItemLike)ModItems.MAYAN_PATTERN_3.get());
        output.accept((ItemLike)ModItems.MAYAN_PATTERN_4.get());
        output.accept((ItemLike)ModBlocks.MAYAN_GOLD_BLOCK.get());
        output.accept((ItemLike)ModBlocks.MAYAN_STATUE.get());
        output.accept((ItemLike)ModItems.BYZANTINE_MACE.get());
        output.accept((ItemLike)ModItems.BYZANTINE_PICKAXE.get());
        output.accept((ItemLike)ModItems.BYZANTINE_AXE.get());
        output.accept((ItemLike)ModItems.BYZANTINE_SHOVEL.get());
        output.accept((ItemLike)ModItems.BYZANTINE_HOE.get());
        output.accept((ItemLike)ModItems.BYZANTINE_HELMET.get());
        output.accept((ItemLike)ModItems.BYZANTINE_CHESTPLATE.get());
        output.accept((ItemLike)ModItems.BYZANTINE_LEGGINGS.get());
        output.accept((ItemLike)ModItems.BYZANTINE_BOOTS.get());
        output.accept((ItemLike)ModItems.OLIVES.get());
        output.accept((ItemLike)ModItems.OLIVEOIL.get());
        output.accept((ItemLike)ModItems.FETA.get());
        output.accept((ItemLike)ModItems.SOUVLAKI.get());
        output.accept((ItemLike)ModItems.GRAPES.get());
        output.accept((ItemLike)ModItems.WINEBASIC.get());
        output.accept((ItemLike)ModItems.WINEFANCY.get());
        output.accept((ItemLike)ModItems.CLOTHES_BYZ_WOOL.get());
        output.accept((ItemLike)ModItems.CLOTHES_BYZ_SILK.get());
        output.accept((ItemLike)ModItems.BYZANTINE_FRESCO.get());
        output.accept((ItemLike)ModBlocks.BYZANTINE_TILES.get());
        output.accept((ItemLike)ModBlocks.BYZANTINE_STONE_TILES.get());
        output.accept((ItemLike)ModBlocks.BYZANTINE_SANDSTONE_TILES.get());
        output.accept((ItemLike)ModBlocks.BYZANTINE_STONE_ORNAMENT.get());
        output.accept((ItemLike)ModBlocks.BYZANTINE_SANDSTONE_ORNAMENT.get());
        output.accept((ItemLike)ModBlocks.BYZANTINE_TILES_STAIRS.get());
        output.accept((ItemLike)ModBlocks.BYZANTINE_TILES_SLAB.get());
        output.accept((ItemLike)ModBlocks.BYZANTINE_MOSAIC_RED.get());
        output.accept((ItemLike)ModBlocks.BYZANTINE_MOSAIC_BLUE.get());
        output.accept((ItemLike)ModBlocks.OLIVE_TREE_SAPLING.get());
        output.accept((ItemLike)ModBlocks.OLIVE_TREE_LEAVES.get());
        output.accept((ItemLike)ModBlocks.SILK_WORM.get());
        output.accept((ItemLike)ModBlocks.SNAIL_SOIL.get());
        output.accept((ItemLike)ModItems.BYZANTINE_PATTERN.get());
        output.accept((ItemLike)ModItems.BYZANTINE_PATTERN_1.get());
        output.accept((ItemLike)ModItems.BYZANTINE_PATTERN_2.get());
        output.accept((ItemLike)ModItems.JAPANESE_TACHI.get());
        output.accept((ItemLike)ModItems.YUMIBOW.get());
        output.accept((ItemLike)ModItems.JAPANESE_GUARD_HELMET.get());
        output.accept((ItemLike)ModItems.JAPANESE_GUARD_CHESTPLATE.get());
        output.accept((ItemLike)ModItems.JAPANESE_GUARD_LEGGINGS.get());
        output.accept((ItemLike)ModItems.JAPANESE_GUARD_BOOTS.get());
        output.accept((ItemLike)ModItems.JAPANESE_BLUE_HELMET.get());
        output.accept((ItemLike)ModItems.JAPANESE_BLUE_CHESTPLATE.get());
        output.accept((ItemLike)ModItems.JAPANESE_BLUE_LEGGINGS.get());
        output.accept((ItemLike)ModItems.JAPANESE_BLUE_BOOTS.get());
        output.accept((ItemLike)ModItems.JAPANESE_RED_HELMET.get());
        output.accept((ItemLike)ModItems.JAPANESE_RED_CHESTPLATE.get());
        output.accept((ItemLike)ModItems.JAPANESE_RED_LEGGINGS.get());
        output.accept((ItemLike)ModItems.JAPANESE_RED_BOOTS.get());
        output.accept((ItemLike)ModItems.JAPANESE_PATTERN.get());
        output.accept((ItemLike)ModItems.JAPANESE_PATTERN_1.get());
        output.accept((ItemLike)ModItems.JAPANESE_PATTERN_2.get());
        output.accept((ItemLike)ModItems.JAPANESE_PATTERN_3.get());
        output.accept((ItemLike)ModItems.JAPANESE_PATTERN_4.get());
        output.accept((ItemLike)ModItems.SAKE.get());
        output.accept((ItemLike)ModItems.UDON.get());
        output.accept((ItemLike)ModItems.IKAYAKI.get());
        output.accept((ItemLike)ModBlocks.PAPER_WALL.get());
        output.accept((ItemLike)ModBlocks.JAPANESE_TILES.get());
        output.accept((ItemLike)ModBlocks.JAPANESE_TILES_STAIRS.get());
        output.accept((ItemLike)ModBlocks.JAPANESE_TILES_SLAB.get());
        output.accept((ItemLike)ModBlocks.JAPANESE_STONE_TILES.get());
        output.accept((ItemLike)ModBlocks.GRAY_TILES.get());
        output.accept((ItemLike)ModBlocks.GRAY_TILES_STAIRS.get());
        output.accept((ItemLike)ModBlocks.GRAY_TILES_SLAB.get());
        output.accept((ItemLike)ModBlocks.GREEN_TILES.get());
        output.accept((ItemLike)ModBlocks.GREEN_TILES_STAIRS.get());
        output.accept((ItemLike)ModBlocks.GREEN_TILES_SLAB.get());
        output.accept((ItemLike)ModBlocks.RED_TILES.get());
        output.accept((ItemLike)ModBlocks.RED_TILES_STAIRS.get());
        output.accept((ItemLike)ModBlocks.RED_TILES_SLAB.get());
        output.accept((ItemLike)ModBlocks.FUTON.get());
        output.accept((ItemLike)ModItems.WOODEN_SLIDING_DOOR_ITEM.get());
        output.accept((ItemLike)ModItems.JAPANESE_SLIDING_DOOR_ITEM.get());
        output.accept((ItemLike)ModItems.SELJUK_SCIMITAR.get());
        output.accept((ItemLike)ModItems.SELJUK_BOW.get());
        output.accept((ItemLike)ModItems.SELJUK_HELMET.get());
        output.accept((ItemLike)ModItems.SELJUK_CHESTPLATE.get());
        output.accept((ItemLike)ModItems.SELJUK_LEGGINGS.get());
        output.accept((ItemLike)ModItems.SELJUK_BOOTS.get());
        output.accept((ItemLike)ModItems.SELJUK_TURBAN.get());
        output.accept((ItemLike)ModItems.YOGURT.get());
        output.accept((ItemLike)ModItems.AYRAN.get());
        output.accept((ItemLike)ModItems.PIDE.get());
        output.accept((ItemLike)ModItems.LOKUM.get());
        output.accept((ItemLike)ModItems.HELVA.get());
        output.accept((ItemLike)ModItems.PISTACHIOS.get());
        output.accept((ItemLike)ModItems.CLOTHES_SELJUK_WOOL.get());
        output.accept((ItemLike)ModItems.CLOTHES_SELJUK_COTTON.get());
        output.accept((ItemLike)ModItems.WALL_CARPET_SMALL.get());
        output.accept((ItemLike)ModItems.WALL_CARPET_MEDIUM.get());
        output.accept((ItemLike)ModItems.WALL_CARPET_LARGE.get());
        output.accept((ItemLike)ModBlocks.MUD_BRICK_SELJUK_ORNAMENTED.get());
        output.accept((ItemLike)ModBlocks.MUD_BRICK_SELJUK_DECORATED.get());
        output.accept((ItemLike)ModBlocks.MUD_BRICK_SMOOTH.get());
        output.accept((ItemLike)ModBlocks.PISTACHIO_TREE_SAPLING.get());
        output.accept((ItemLike)ModBlocks.PISTACHIO_TREE_LEAVES.get());
        output.accept((ItemLike)ModItems.INUIT_TRIDENT.get());
        output.accept((ItemLike)ModItems.INUIT_BOW.get());
        output.accept((ItemLike)ModItems.FUR_HELMET.get());
        output.accept((ItemLike)ModItems.FUR_CHESTPLATE.get());
        output.accept((ItemLike)ModItems.FUR_LEGGINGS.get());
        output.accept((ItemLike)ModItems.FUR_BOOTS.get());
        output.accept((ItemLike)ModItems.BEARMEAT_RAW.get());
        output.accept((ItemLike)ModItems.BEARMEAT_COOKED.get());
        output.accept((ItemLike)ModItems.WOLFMEAT_RAW.get());
        output.accept((ItemLike)ModItems.WOLFMEAT_COOKED.get());
        output.accept((ItemLike)ModItems.SEAFOOD_RAW.get());
        output.accept((ItemLike)ModItems.SEAFOOD_COOKED.get());
        output.accept((ItemLike)ModItems.INUIT_POTATO_STEW.get());
        output.accept((ItemLike)ModItems.INUIT_MEATY_STEW.get());
        output.accept((ItemLike)ModItems.INUIT_BEAR_STEW.get());
        output.accept((ItemLike)ModItems.TANNED_HIDE.get());
        output.accept((ItemLike)ModItems.SNOW_BRICK_ITEM.get());
        output.accept((ItemLike)ModItems.ICE_BRICK_ITEM.get());
        output.accept((ItemLike)ModItems.SNOW_WALL_ITEM.get());
        output.accept((ItemLike)ModItems.SOD_OAK_ITEM.get());
        output.accept((ItemLike)ModItems.SOD_SPRUCE_ITEM.get());
        output.accept((ItemLike)ModItems.SOD_BIRCH_ITEM.get());
        output.accept((ItemLike)ModItems.SOD_JUNGLE_ITEM.get());
        output.accept((ItemLike)ModItems.SOD_ACACIA_ITEM.get());
        output.accept((ItemLike)ModItems.SOD_DARK_OAK_ITEM.get());
        output.accept((ItemLike)ModItems.INUIT_CARVING_ITEM.get());
        output.accept((ItemLike)ModItems.FIRE_PIT_ITEM.get());
    }).build());
    public static final DeferredHolder<CreativeModeTab, CreativeModeTab> CONTENT_CREATOR_TAB = TABS.register("millenaire_content_creator", () -> CreativeModeTab.builder().title((Component)Component.translatable((String)"itemGroup.millenaire.millenaire_content_creator")).icon(() -> new ItemStack((ItemLike)ModBlocks.IMPORT_TABLE.get())).displayItems((params, output) -> {
        output.accept((ItemLike)ModBlocks.IMPORT_TABLE.get());
        for (MarkerType markerType : MarkerType.values()) {
            output.accept(ModCreativeTabs.mockStack((Block)ModBlocks.MOCK_MARKER.get(), MockMarkerBlock.TYPE, markerType, "block.millenaire.mock_marker." + markerType.getSerializedName()));
        }
        for (Enum enum_ : FacingMarkerType.values()) {
            output.accept(ModCreativeTabs.mockStack((Block)ModBlocks.MOCK_FACING_MARKER.get(), MockFacingMarkerBlock.TYPE, enum_, "block.millenaire.mock_facing_marker." + ((FacingMarkerType)enum_).getSerializedName()));
        }
        for (Enum enum_ : MockChestType.values()) {
            output.accept(ModCreativeTabs.mockStack((Block)ModBlocks.MOCK_CHEST.get(), MockChestBlock.CHEST_TYPE, enum_, "block.millenaire.mock_chest." + ((MockChestType)enum_).getSerializedName()));
        }
        for (Enum enum_ : CropType.values()) {
            output.accept(ModCreativeTabs.mockStack((Block)ModBlocks.MOCK_SOIL.get(), MockSoilBlock.CROP, enum_, "block.millenaire.mock_soil." + ((CropType)enum_).getSerializedName()));
        }
        for (Enum enum_ : SourceType.values()) {
            output.accept(ModCreativeTabs.mockStack((Block)ModBlocks.MOCK_SOURCE.get(), MockSourceBlock.MATERIAL, enum_, "block.millenaire.mock_source." + ((SourceType)enum_).getSerializedName()));
        }
        for (Enum enum_ : FreeBlockType.values()) {
            output.accept(ModCreativeTabs.mockStack((Block)ModBlocks.MOCK_FREE.get(), MockFreeBlock.MATERIAL, enum_, "block.millenaire.mock_free." + ((FreeBlockType)enum_).getSerializedName()));
        }
        for (Enum enum_ : TreeSpawnType.values()) {
            output.accept(ModCreativeTabs.mockStack((Block)ModBlocks.MOCK_TREE_SPAWN.get(), MockTreeSpawnBlock.TREE, enum_, "block.millenaire.mock_tree_spawn." + ((TreeSpawnType)enum_).getSerializedName()));
        }
        for (Enum enum_ : AnimalSpawnType.values()) {
            output.accept(ModCreativeTabs.mockStack((Block)ModBlocks.MOCK_ANIMAL_SPAWN.get(), MockAnimalSpawnBlock.ANIMAL, enum_, "block.millenaire.mock_animal_spawn." + ((AnimalSpawnType)enum_).getSerializedName()));
        }
        for (Enum enum_ : MockDecorBlock.DecorType.values()) {
            output.accept(ModCreativeTabs.mockStack((Block)ModBlocks.MOCK_DECOR.get(), MockDecorBlock.DECOR_TYPE, enum_, "block.millenaire.mock_decor." + ((MockDecorBlock.DecorType)enum_).getSerializedName()));
        }
        for (Enum enum_ : BannerSubtype.values()) {
            output.accept(ModCreativeTabs.mockStack((Block)ModBlocks.MOCK_BANNER_WALL.get(), MockBannerWallBlock.SUBTYPE, enum_, "block.millenaire.mock_banner_wall." + ((BannerSubtype)enum_).getSerializedName()));
        }
        for (Enum enum_ : BannerSubtype.values()) {
            output.accept(ModCreativeTabs.mockStack((Block)ModBlocks.MOCK_BANNER_STANDING.get(), MockBannerStandingBlock.SUBTYPE, enum_, "block.millenaire.mock_banner_standing." + ((BannerSubtype)enum_).getSerializedName()));
        }
    }).build());

    private ModCreativeTabs() {
    }

    private static <T extends Enum<T>> ItemStack mockStack(Block block, EnumProperty<T> property, T value, String translationKey) {
        ItemStack stack = new ItemStack((ItemLike)block);
        stack.set(DataComponents.BLOCK_STATE, (Object)BlockItemStateProperties.EMPTY.with(property, value));
        stack.set(DataComponents.ITEM_NAME, (Object)Component.translatable((String)translationKey));
        stack.set(DataComponents.CUSTOM_MODEL_DATA, (Object)new CustomModelData(value.ordinal() + 1));
        return stack;
    }

    public static void register(IEventBus modEventBus) {
        TABS.register(modEventBus);
    }
}

