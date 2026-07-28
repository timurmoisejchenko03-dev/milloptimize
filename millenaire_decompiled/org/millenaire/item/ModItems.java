/*
 * Decompiled with CFR 0.150.
 * 
 * Could not load the following classes:
 *  net.minecraft.core.Holder
 *  net.minecraft.core.registries.Registries
 *  net.minecraft.resources.ResourceKey
 *  net.minecraft.resources.ResourceLocation
 *  net.minecraft.tags.TagKey
 *  net.minecraft.world.effect.MobEffectInstance
 *  net.minecraft.world.effect.MobEffects
 *  net.minecraft.world.item.ArmorItem
 *  net.minecraft.world.item.ArmorItem$Type
 *  net.minecraft.world.item.AxeItem
 *  net.minecraft.world.item.BannerPatternItem
 *  net.minecraft.world.item.BedItem
 *  net.minecraft.world.item.BlockItem
 *  net.minecraft.world.item.BowItem
 *  net.minecraft.world.item.DiggerItem
 *  net.minecraft.world.item.DoubleHighBlockItem
 *  net.minecraft.world.item.DyeColor
 *  net.minecraft.world.item.HoeItem
 *  net.minecraft.world.item.Item
 *  net.minecraft.world.item.Item$Properties
 *  net.minecraft.world.item.PickaxeItem
 *  net.minecraft.world.item.ShovelItem
 *  net.minecraft.world.item.SwordItem
 *  net.minecraft.world.item.Tier
 *  net.minecraft.world.item.Tiers
 *  net.minecraft.world.level.block.Block
 *  net.minecraft.world.level.block.entity.BannerPattern
 *  net.neoforged.bus.api.IEventBus
 *  net.neoforged.neoforge.registries.DeferredItem
 *  net.neoforged.neoforge.registries.DeferredRegister
 *  net.neoforged.neoforge.registries.DeferredRegister$Items
 */
package org.millenaire.item;

import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.AxeItem;
import net.minecraft.world.item.BannerPatternItem;
import net.minecraft.world.item.BedItem;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.BowItem;
import net.minecraft.world.item.DiggerItem;
import net.minecraft.world.item.DoubleHighBlockItem;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.item.HoeItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.PickaxeItem;
import net.minecraft.world.item.ShovelItem;
import net.minecraft.world.item.SwordItem;
import net.minecraft.world.item.Tier;
import net.minecraft.world.item.Tiers;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BannerPattern;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;
import org.millenaire.block.ModBlocks;
import org.millenaire.entity.WallDecorationType;
import org.millenaire.item.BrickMouldItem;
import org.millenaire.item.ByzantineMaterials;
import org.millenaire.item.ClothItem;
import org.millenaire.item.CoinItem;
import org.millenaire.item.CropSeedItem;
import org.millenaire.item.ImportTableItem;
import org.millenaire.item.InuitMaterials;
import org.millenaire.item.JapaneseMaterials;
import org.millenaire.item.LearnedSaplingItem;
import org.millenaire.item.MayanMaterials;
import org.millenaire.item.MillFoodItem;
import org.millenaire.item.MillenaireBow;
import org.millenaire.item.NegationWandItem;
import org.millenaire.item.NormanMaterials;
import org.millenaire.item.PaintBucketItem;
import org.millenaire.item.PurseItem;
import org.millenaire.item.RiceSeedItem;
import org.millenaire.item.SeljukMaterials;
import org.millenaire.item.SummoningWandItem;
import org.millenaire.item.TravelBookItem;
import org.millenaire.item.UluItem;
import org.millenaire.item.VillageBookItem;
import org.millenaire.item.WallDecorationItem;

public final class ModItems {
    public static final DeferredRegister.Items ITEMS = DeferredRegister.createItems((String)"millenaire");
    public static final DeferredItem<CoinItem> DENIER = ITEMS.registerItem("denier", props -> new CoinItem(props.stacksTo(64)));
    public static final DeferredItem<CoinItem> DENIER_ARGENT = ITEMS.registerItem("denier_argent", props -> new CoinItem(props.stacksTo(64)));
    public static final DeferredItem<CoinItem> DENIER_OR = ITEMS.registerItem("denier_or", props -> new CoinItem(props.stacksTo(64)));
    public static final DeferredItem<SummoningWandItem> SUMMONING_WAND = ITEMS.registerItem("summoning_wand", props -> new SummoningWandItem(props.stacksTo(1)));
    public static final DeferredItem<NegationWandItem> NEGATION_WAND = ITEMS.registerItem("negation_wand", props -> new NegationWandItem(props.stacksTo(1)));
    public static final DeferredItem<PurseItem> PURSE = ITEMS.registerItem("purse", props -> new PurseItem(props.stacksTo(1)));
    public static final DeferredItem<VillageBookItem> VILLAGE_SCROLL = ITEMS.registerItem("village_scroll", props -> new VillageBookItem(props.stacksTo(1)));
    public static final DeferredItem<TravelBookItem> TRAVEL_BOOK = ITEMS.registerItem("travel_book", props -> new TravelBookItem(props.stacksTo(1)));
    public static final DeferredItem<PickaxeItem> NORMAN_PICKAXE = ITEMS.registerItem("norman_pickaxe", props -> new PickaxeItem(NormanMaterials.NORMAN_TOOL, props.attributes(DiggerItem.createAttributes((Tier)NormanMaterials.NORMAN_TOOL, (float)1.0f, (float)-2.8f))));
    public static final DeferredItem<AxeItem> NORMAN_AXE = ITEMS.registerItem("norman_axe", props -> new AxeItem(NormanMaterials.NORMAN_TOOL, props.attributes(DiggerItem.createAttributes((Tier)NormanMaterials.NORMAN_TOOL, (float)4.0f, (float)-3.0f))));
    public static final DeferredItem<ShovelItem> NORMAN_SHOVEL = ITEMS.registerItem("norman_shovel", props -> new ShovelItem(NormanMaterials.NORMAN_TOOL, props.attributes(DiggerItem.createAttributes((Tier)NormanMaterials.NORMAN_TOOL, (float)1.5f, (float)-3.0f))));
    public static final DeferredItem<HoeItem> NORMAN_HOE = ITEMS.registerItem("norman_hoe", props -> new HoeItem(NormanMaterials.NORMAN_TOOL, props.attributes(DiggerItem.createAttributes((Tier)NormanMaterials.NORMAN_TOOL, (float)-2.0f, (float)-1.0f))));
    public static final DeferredItem<SwordItem> NORMAN_SWORD = ITEMS.registerItem("norman_sword", props -> new SwordItem(NormanMaterials.NORMAN_TOOL, props.attributes(SwordItem.createAttributes((Tier)NormanMaterials.NORMAN_TOOL, (float)3.0f, (float)-2.4f))));
    public static final DeferredItem<ArmorItem> NORMAN_HELMET = ITEMS.registerItem("norman_helmet", props -> new ArmorItem(NormanMaterials.NORMAN_ARMOR, ArmorItem.Type.HELMET, props.durability(ArmorItem.Type.HELMET.getDurability(66))));
    public static final DeferredItem<ArmorItem> NORMAN_CHESTPLATE = ITEMS.registerItem("norman_chestplate", props -> new ArmorItem(NormanMaterials.NORMAN_ARMOR, ArmorItem.Type.CHESTPLATE, props.durability(ArmorItem.Type.CHESTPLATE.getDurability(66))));
    public static final DeferredItem<ArmorItem> NORMAN_LEGGINGS = ITEMS.registerItem("norman_leggings", props -> new ArmorItem(NormanMaterials.NORMAN_ARMOR, ArmorItem.Type.LEGGINGS, props.durability(ArmorItem.Type.LEGGINGS.getDurability(66))));
    public static final DeferredItem<ArmorItem> NORMAN_BOOTS = ITEMS.registerItem("norman_boots", props -> new ArmorItem(NormanMaterials.NORMAN_ARMOR, ArmorItem.Type.BOOTS, props.durability(ArmorItem.Type.BOOTS.getDurability(66))));
    public static final DeferredItem<MillFoodItem> CIDERAPPLE = ITEMS.registerItem("cider_apple", props -> new MillFoodItem(props.stacksTo(64), 1, 0.05f, 0, 0, 0, false));
    public static final DeferredItem<MillFoodItem> CIDER = ITEMS.registerItem("cider", props -> new MillFoodItem(MillFoodItem.multiPortionProperties(props, 6), 0, 0.0f, 4, 15, 5, true));
    public static final DeferredItem<MillFoodItem> CALVA = ITEMS.registerItem("calva", props -> new MillFoodItem(MillFoodItem.multiPortionProperties(props, 16), 0, 0.0f, 8, 30, 10, true));
    public static final DeferredItem<MillFoodItem> BOUDIN = ITEMS.registerItem("boudin", props -> new MillFoodItem(MillFoodItem.multiPortionProperties(props, 6), 8, 1.0f, 0, 0, 0, false));
    public static final DeferredItem<MillFoodItem> TRIPES = ITEMS.registerItem("tripes", props -> new MillFoodItem(MillFoodItem.multiPortionProperties(props, 8), 10, 1.0f, 0, 0, 0, false));
    public static final DeferredItem<BlockItem> TIMBER_FRAME_PLAIN_ITEM = ITEMS.registerSimpleBlockItem(ModBlocks.TIMBER_FRAME_PLAIN);
    public static final DeferredItem<BlockItem> TIMBER_FRAME_CROSS_ITEM = ITEMS.registerSimpleBlockItem(ModBlocks.TIMBER_FRAME_CROSS);
    public static final DeferredItem<BlockItem> THATCH_ITEM = ITEMS.registerSimpleBlockItem(ModBlocks.THATCH);
    public static final DeferredItem<BlockItem> MUD_BRICK_ITEM = ITEMS.registerSimpleBlockItem(ModBlocks.MUD_BRICK);
    public static final DeferredItem<BlockItem> DIRT_WALL_ITEM = ITEMS.registerSimpleBlockItem(ModBlocks.DIRT_WALL);
    public static final DeferredItem<BlockItem> TIMBER_FRAME_STAIRS_ITEM = ITEMS.registerSimpleBlockItem(ModBlocks.TIMBER_FRAME_STAIRS);
    public static final DeferredItem<BlockItem> THATCH_STAIRS_ITEM = ITEMS.registerSimpleBlockItem(ModBlocks.THATCH_STAIRS);
    public static final DeferredItem<BlockItem> MUD_BRICK_STAIRS_ITEM = ITEMS.registerSimpleBlockItem(ModBlocks.MUD_BRICK_STAIRS);
    public static final DeferredItem<BlockItem> TIMBER_FRAME_SLAB_ITEM = ITEMS.registerSimpleBlockItem(ModBlocks.TIMBER_FRAME_SLAB);
    public static final DeferredItem<BlockItem> THATCH_SLAB_ITEM = ITEMS.registerSimpleBlockItem(ModBlocks.THATCH_SLAB);
    public static final DeferredItem<BlockItem> MUD_BRICK_SLAB_ITEM = ITEMS.registerSimpleBlockItem(ModBlocks.MUD_BRICK_SLAB);
    public static final DeferredItem<BlockItem> MUD_BRICK_WALL_ITEM = ITEMS.registerSimpleBlockItem(ModBlocks.MUD_BRICK_WALL);
    public static final DeferredItem<BlockItem> STAINED_GLASS_WHITE_ITEM = ITEMS.registerSimpleBlockItem(ModBlocks.STAINED_GLASS_WHITE);
    public static final DeferredItem<BlockItem> STAINED_GLASS_YELLOW_ITEM = ITEMS.registerSimpleBlockItem(ModBlocks.STAINED_GLASS_YELLOW);
    public static final DeferredItem<BlockItem> STAINED_GLASS_YELLOW_RED_ITEM = ITEMS.registerSimpleBlockItem(ModBlocks.STAINED_GLASS_YELLOW_RED);
    public static final DeferredItem<BlockItem> STAINED_GLASS_RED_BLUE_ITEM = ITEMS.registerSimpleBlockItem(ModBlocks.STAINED_GLASS_RED_BLUE);
    public static final DeferredItem<BlockItem> STAINED_GLASS_GREEN_BLUE_ITEM = ITEMS.registerSimpleBlockItem(ModBlocks.STAINED_GLASS_GREEN_BLUE);
    public static final DeferredItem<BlockItem> ROSETTE_ITEM = ITEMS.registerSimpleBlockItem(ModBlocks.ROSETTE);
    public static final DeferredItem<BlockItem> WOODEN_BARS_ROSETTE_ITEM = ITEMS.registerSimpleBlockItem(ModBlocks.WOODEN_BARS_ROSETTE);
    public static final DeferredItem<BlockItem> WOODEN_BARS_ITEM = ITEMS.registerSimpleBlockItem(ModBlocks.WOODEN_BARS);
    public static final DeferredItem<BlockItem> PATH_DIRT_ITEM = ITEMS.registerSimpleBlockItem(ModBlocks.PATH_DIRT);
    public static final DeferredItem<BlockItem> PATH_DIRT_SLAB_ITEM = ITEMS.registerSimpleBlockItem(ModBlocks.PATH_DIRT_SLAB);
    public static final DeferredItem<BlockItem> PATH_GRAVEL_ITEM = ITEMS.registerSimpleBlockItem(ModBlocks.PATH_GRAVEL);
    public static final DeferredItem<BlockItem> PATH_GRAVEL_SLAB_ITEM = ITEMS.registerSimpleBlockItem(ModBlocks.PATH_GRAVEL_SLAB);
    public static final DeferredItem<BlockItem> PATH_SLABS_ITEM = ITEMS.registerSimpleBlockItem(ModBlocks.PATH_SLABS);
    public static final DeferredItem<BlockItem> PATH_SLABS_SLAB_ITEM = ITEMS.registerSimpleBlockItem(ModBlocks.PATH_SLABS_SLAB);
    public static final DeferredItem<BlockItem> PATH_OCHRE_TILES_ITEM = ITEMS.registerSimpleBlockItem(ModBlocks.PATH_OCHRE_TILES);
    public static final DeferredItem<BlockItem> PATH_SANDSTONE_ITEM = ITEMS.registerSimpleBlockItem(ModBlocks.PATH_SANDSTONE);
    public static final DeferredItem<BlockItem> PATH_OCHRE_TILES_SLAB_ITEM = ITEMS.registerSimpleBlockItem(ModBlocks.PATH_OCHRE_TILES_SLAB);
    public static final DeferredItem<BlockItem> PATH_SANDSTONE_SLAB_ITEM = ITEMS.registerSimpleBlockItem(ModBlocks.PATH_SANDSTONE_SLAB);
    public static final DeferredItem<BlockItem> PATH_GRAVEL_SLABS_ITEM = ITEMS.registerSimpleBlockItem(ModBlocks.PATH_GRAVEL_SLABS);
    public static final DeferredItem<BlockItem> PATH_GRAVEL_SLABS_SLAB_ITEM = ITEMS.registerSimpleBlockItem(ModBlocks.PATH_GRAVEL_SLABS_SLAB);
    public static final DeferredItem<BlockItem> PATH_SNOW_ITEM = ITEMS.registerSimpleBlockItem(ModBlocks.PATH_SNOW);
    public static final DeferredItem<BlockItem> PATH_SNOW_SLAB_ITEM = ITEMS.registerSimpleBlockItem(ModBlocks.PATH_SNOW_SLAB);
    public static final DeferredItem<BlockItem> TAPESTRY_ITEM = ITEMS.registerSimpleBlockItem(ModBlocks.TAPESTRY);
    public static final DeferredItem<BlockItem> STRAW_BED_ITEM = ITEMS.registerItem("straw_bed", props -> new BedItem((Block)ModBlocks.STRAW_BED.get(), props));
    public static final DeferredItem<MillFoodItem> VEGCURRY = ITEMS.registerItem("vegcurry", props -> new MillFoodItem(MillFoodItem.multiPortionProperties(props, 6), 6, 0.6f, 0, 0, 0, false));
    public static final DeferredItem<MillFoodItem> CHICKENCURRY = ITEMS.registerItem("chickencurry", props -> new MillFoodItem(MillFoodItem.multiPortionProperties(props, 8), 8, 0.8f, 0, 0, 0, false));
    public static final DeferredItem<MillFoodItem> RASGULLA = ITEMS.registerItem("rasgulla", props -> new MillFoodItem(props.stacksTo(64), 0, 0.0f, 2, 30, 0, 480, false));
    public static final DeferredItem<Item> SILK = ITEMS.registerSimpleItem("silk", new Item.Properties().stacksTo(64));
    public static final DeferredItem<Item> COTTON = ITEMS.registerItem("cotton", props -> new CropSeedItem((Supplier<? extends Block>)ModBlocks.CROP_COTTON, "cotton", props.stacksTo(64)));
    public static final DeferredItem<Item> RICE = ITEMS.registerItem("rice", props -> new RiceSeedItem(props.stacksTo(64)));
    public static final DeferredItem<Item> TURMERIC = ITEMS.registerItem("turmeric", props -> new CropSeedItem((Supplier<? extends Block>)ModBlocks.CROP_TURMERIC, "turmeric", props.stacksTo(64)));
    public static final DeferredItem<Item> ULU = ITEMS.registerItem("ulu", props -> new UluItem(props.stacksTo(1).durability(256)));
    public static final DeferredItem<BlockItem> RICE_PADDY_ITEM = ITEMS.registerSimpleBlockItem(ModBlocks.RICE_PADDY);
    public static final DeferredItem<BlockItem> WOODEN_BARS_INDIAN_ITEM = ITEMS.registerSimpleBlockItem(ModBlocks.WOODEN_BARS_INDIAN);
    public static final DeferredItem<BlockItem> CHARPOY_ITEM = ITEMS.registerItem("charpoy", props -> new BedItem((Block)ModBlocks.CHARPOY.get(), props));
    public static final DeferredItem<BlockItem> FUTON_ITEM = ITEMS.registerItem("futon", props -> new BedItem((Block)ModBlocks.FUTON.get(), props));
    public static final DeferredItem<BlockItem> WET_BRICK_ITEM = ITEMS.registerSimpleBlockItem(ModBlocks.WET_BRICK);
    public static final DeferredItem<BlockItem> SANDSTONE_CARVED_ITEM = ITEMS.registerSimpleBlockItem(ModBlocks.SANDSTONE_CARVED);
    public static final DeferredItem<BlockItem> SANDSTONE_CARVED_STAIRS_ITEM = ITEMS.registerSimpleBlockItem(ModBlocks.SANDSTONE_CARVED_STAIRS);
    public static final DeferredItem<BlockItem> SANDSTONE_CARVED_SLAB_ITEM = ITEMS.registerSimpleBlockItem(ModBlocks.SANDSTONE_CARVED_SLAB);
    public static final DeferredItem<BlockItem> SANDSTONE_CARVED_WALL_ITEM = ITEMS.registerSimpleBlockItem(ModBlocks.SANDSTONE_CARVED_WALL);
    public static final DeferredItem<BlockItem> RED_SANDSTONE_CARVED_ITEM = ITEMS.registerSimpleBlockItem(ModBlocks.RED_SANDSTONE_CARVED);
    public static final DeferredItem<BlockItem> RED_SANDSTONE_CARVED_STAIRS_ITEM = ITEMS.registerSimpleBlockItem(ModBlocks.RED_SANDSTONE_CARVED_STAIRS);
    public static final DeferredItem<BlockItem> RED_SANDSTONE_CARVED_SLAB_ITEM = ITEMS.registerSimpleBlockItem(ModBlocks.RED_SANDSTONE_CARVED_SLAB);
    public static final DeferredItem<BlockItem> RED_SANDSTONE_CARVED_WALL_ITEM = ITEMS.registerSimpleBlockItem(ModBlocks.RED_SANDSTONE_CARVED_WALL);
    public static final DeferredItem<BlockItem> OCHRE_SANDSTONE_CARVED_ITEM = ITEMS.registerSimpleBlockItem(ModBlocks.OCHRE_SANDSTONE_CARVED);
    public static final DeferredItem<BlockItem> OCHRE_SANDSTONE_CARVED_STAIRS_ITEM = ITEMS.registerSimpleBlockItem(ModBlocks.OCHRE_SANDSTONE_CARVED_STAIRS);
    public static final DeferredItem<BlockItem> OCHRE_SANDSTONE_CARVED_SLAB_ITEM = ITEMS.registerSimpleBlockItem(ModBlocks.OCHRE_SANDSTONE_CARVED_SLAB);
    public static final DeferredItem<BlockItem> OCHRE_SANDSTONE_CARVED_WALL_ITEM = ITEMS.registerSimpleBlockItem(ModBlocks.OCHRE_SANDSTONE_CARVED_WALL);
    public static final DeferredItem<BlockItem> MAYAN_GOLD_BLOCK_ITEM = ITEMS.registerSimpleBlockItem(ModBlocks.MAYAN_GOLD_BLOCK);
    public static final DeferredItem<BlockItem> MAYAN_STATUE_BLOCK_ITEM = ITEMS.registerSimpleBlockItem(ModBlocks.MAYAN_STATUE);
    public static final DeferredItem<BlockItem> BYZANTINE_TILES_ITEM = ITEMS.registerSimpleBlockItem(ModBlocks.BYZANTINE_TILES);
    public static final DeferredItem<BlockItem> BYZANTINE_STONE_TILES_ITEM = ITEMS.registerSimpleBlockItem(ModBlocks.BYZANTINE_STONE_TILES);
    public static final DeferredItem<BlockItem> BYZANTINE_SANDSTONE_TILES_ITEM = ITEMS.registerSimpleBlockItem(ModBlocks.BYZANTINE_SANDSTONE_TILES);
    public static final DeferredItem<BlockItem> BYZANTINE_STONE_ORNAMENT_ITEM = ITEMS.registerSimpleBlockItem(ModBlocks.BYZANTINE_STONE_ORNAMENT);
    public static final DeferredItem<BlockItem> BYZANTINE_SANDSTONE_ORNAMENT_ITEM = ITEMS.registerSimpleBlockItem(ModBlocks.BYZANTINE_SANDSTONE_ORNAMENT);
    public static final DeferredItem<BlockItem> BYZANTINE_TILES_STAIRS_ITEM = ITEMS.registerSimpleBlockItem(ModBlocks.BYZANTINE_TILES_STAIRS);
    public static final DeferredItem<BlockItem> BYZANTINE_TILES_SLAB_ITEM = ITEMS.registerSimpleBlockItem(ModBlocks.BYZANTINE_TILES_SLAB);
    public static final DeferredItem<BlockItem> BYZANTINE_MOSAIC_RED_ITEM = ITEMS.registerSimpleBlockItem(ModBlocks.BYZANTINE_MOSAIC_RED);
    public static final DeferredItem<BlockItem> BYZANTINE_MOSAIC_BLUE_ITEM = ITEMS.registerSimpleBlockItem(ModBlocks.BYZANTINE_MOSAIC_BLUE);
    public static final DeferredItem<BlockItem> OLIVE_TREE_SAPLING_ITEM = ITEMS.registerItem("olive_tree_sapling", props -> new LearnedSaplingItem((Block)ModBlocks.OLIVE_TREE_SAPLING.get(), "sapling_olivetree", (Item.Properties)props));
    public static final DeferredItem<BlockItem> OLIVE_TREE_LEAVES_ITEM = ITEMS.registerSimpleBlockItem(ModBlocks.OLIVE_TREE_LEAVES);
    public static final DeferredItem<BlockItem> SILK_WORM_ITEM = ITEMS.registerSimpleBlockItem(ModBlocks.SILK_WORM);
    public static final DeferredItem<BlockItem> SNAIL_SOIL_ITEM = ITEMS.registerSimpleBlockItem(ModBlocks.SNAIL_SOIL);
    public static final DeferredItem<Item> BRICK_MOULD = ITEMS.registerItem("brick_mould", props -> new BrickMouldItem(props.stacksTo(1).durability(512)));
    public static final Map<DyeColor, DeferredItem<BlockItem>> PAINTED_BRICK_ITEMS = new EnumMap<DyeColor, DeferredItem<BlockItem>>(DyeColor.class);
    public static final Map<DyeColor, DeferredItem<BlockItem>> DECORATED_BRICK_ITEMS = new EnumMap<DyeColor, DeferredItem<BlockItem>>(DyeColor.class);
    public static final Map<DyeColor, DeferredItem<BlockItem>> PAINTED_BRICK_STAIRS_ITEMS = new EnumMap<DyeColor, DeferredItem<BlockItem>>(DyeColor.class);
    public static final Map<DyeColor, DeferredItem<BlockItem>> PAINTED_BRICK_SLAB_ITEMS = new EnumMap<DyeColor, DeferredItem<BlockItem>>(DyeColor.class);
    public static final Map<DyeColor, DeferredItem<BlockItem>> PAINTED_BRICK_WALL_ITEMS = new EnumMap<DyeColor, DeferredItem<BlockItem>>(DyeColor.class);
    public static final Map<DyeColor, DeferredItem<Item>> PAINT_BUCKETS;
    public static final DeferredItem<BlockItem> APPLE_TREE_SAPLING_ITEM;
    public static final DeferredItem<BlockItem> APPLE_TREE_LEAVES_ITEM;
    public static final DeferredItem<BlockItem> LOCKED_CHEST_ITEM;
    public static final DeferredItem<BlockItem> VILLAGE_PANEL_ITEM;
    public static final DeferredItem<ImportTableItem> IMPORT_TABLE_ITEM;
    public static final DeferredItem<BlockItem> MOCK_MARKER_ITEM;
    public static final DeferredItem<BlockItem> MOCK_SOIL_ITEM;
    public static final DeferredItem<BlockItem> MOCK_SOURCE_ITEM;
    public static final DeferredItem<BlockItem> MOCK_FREE_ITEM;
    public static final DeferredItem<BlockItem> MOCK_TREE_SPAWN_ITEM;
    public static final DeferredItem<BlockItem> MOCK_ANIMAL_SPAWN_ITEM;
    public static final DeferredItem<BlockItem> MOCK_CHEST_ITEM;
    public static final DeferredItem<BlockItem> MOCK_DECOR_ITEM;
    public static final DeferredItem<BlockItem> MOCK_FACING_MARKER_ITEM;
    public static final DeferredItem<BlockItem> MOCK_BANNER_WALL_ITEM;
    public static final DeferredItem<BlockItem> MOCK_BANNER_STANDING_ITEM;
    public static final DeferredItem<MillFoodItem> MASA;
    public static final DeferredItem<MillFoodItem> WAH;
    public static final DeferredItem<MillFoodItem> CACAUHAA;
    public static final DeferredItem<Item> MAIZE;
    public static final DeferredItem<PickaxeItem> MAYAN_PICKAXE;
    public static final DeferredItem<AxeItem> MAYAN_AXE;
    public static final DeferredItem<ShovelItem> MAYAN_SHOVEL;
    public static final DeferredItem<HoeItem> MAYAN_HOE;
    public static final DeferredItem<SwordItem> MAYAN_MACE;
    public static final DeferredItem<Item> OBSIDIAN_FLAKE;
    public static final DeferredItem<BannerPatternItem> MAYAN_PATTERN;
    public static final DeferredItem<BannerPatternItem> MAYAN_PATTERN_1;
    public static final DeferredItem<BannerPatternItem> MAYAN_PATTERN_2;
    public static final DeferredItem<BannerPatternItem> MAYAN_PATTERN_3;
    public static final DeferredItem<BannerPatternItem> MAYAN_PATTERN_4;
    public static final DeferredItem<MillFoodItem> OLIVES;
    public static final DeferredItem<MillFoodItem> OLIVEOIL;
    public static final DeferredItem<MillFoodItem> FETA;
    public static final DeferredItem<MillFoodItem> SOUVLAKI;
    public static final DeferredItem<MillFoodItem> WINEBASIC;
    public static final DeferredItem<MillFoodItem> WINEFANCY;
    public static final DeferredItem<Item> GRAPES;
    public static final DeferredItem<SwordItem> BYZANTINE_MACE;
    public static final DeferredItem<ArmorItem> BYZANTINE_HELMET;
    public static final DeferredItem<ArmorItem> BYZANTINE_CHESTPLATE;
    public static final DeferredItem<ArmorItem> BYZANTINE_LEGGINGS;
    public static final DeferredItem<ArmorItem> BYZANTINE_BOOTS;
    public static final DeferredItem<PickaxeItem> BYZANTINE_PICKAXE;
    public static final DeferredItem<AxeItem> BYZANTINE_AXE;
    public static final DeferredItem<ShovelItem> BYZANTINE_SHOVEL;
    public static final DeferredItem<HoeItem> BYZANTINE_HOE;
    public static final DeferredItem<ClothItem> CLOTHES_BYZ_WOOL;
    public static final DeferredItem<ClothItem> CLOTHES_BYZ_SILK;
    public static final DeferredItem<Item> BYZANTINE_FRESCO;
    public static final DeferredItem<BannerPatternItem> BYZANTINE_PATTERN;
    public static final DeferredItem<BannerPatternItem> BYZANTINE_PATTERN_1;
    public static final DeferredItem<BannerPatternItem> BYZANTINE_PATTERN_2;
    public static final DeferredItem<WallDecorationItem> TAPESTRY;
    public static final DeferredItem<WallDecorationItem> INDIAN_STATUE;
    public static final DeferredItem<WallDecorationItem> MAYAN_STATUE;
    public static final DeferredItem<WallDecorationItem> BYZANTINE_ICON_SMALL;
    public static final DeferredItem<WallDecorationItem> BYZANTINE_ICON_MEDIUM;
    public static final DeferredItem<WallDecorationItem> BYZANTINE_ICON_LARGE;
    public static final DeferredItem<WallDecorationItem> HIDE_HANGING;
    public static final DeferredItem<MillFoodItem> SAKE;
    public static final DeferredItem<MillFoodItem> UDON;
    public static final DeferredItem<MillFoodItem> IKAYAKI;
    public static final DeferredItem<BannerPatternItem> JAPANESE_PATTERN;
    public static final DeferredItem<BannerPatternItem> JAPANESE_PATTERN_1;
    public static final DeferredItem<BannerPatternItem> JAPANESE_PATTERN_2;
    public static final DeferredItem<BannerPatternItem> JAPANESE_PATTERN_3;
    public static final DeferredItem<BannerPatternItem> JAPANESE_PATTERN_4;
    public static final DeferredItem<SwordItem> JAPANESE_TACHI;
    public static final DeferredItem<MillenaireBow> YUMIBOW;
    public static final DeferredItem<ArmorItem> JAPANESE_GUARD_HELMET;
    public static final DeferredItem<ArmorItem> JAPANESE_GUARD_CHESTPLATE;
    public static final DeferredItem<ArmorItem> JAPANESE_GUARD_LEGGINGS;
    public static final DeferredItem<ArmorItem> JAPANESE_GUARD_BOOTS;
    public static final DeferredItem<ArmorItem> JAPANESE_BLUE_HELMET;
    public static final DeferredItem<ArmorItem> JAPANESE_BLUE_CHESTPLATE;
    public static final DeferredItem<ArmorItem> JAPANESE_BLUE_LEGGINGS;
    public static final DeferredItem<ArmorItem> JAPANESE_BLUE_BOOTS;
    public static final DeferredItem<ArmorItem> JAPANESE_RED_HELMET;
    public static final DeferredItem<ArmorItem> JAPANESE_RED_CHESTPLATE;
    public static final DeferredItem<ArmorItem> JAPANESE_RED_LEGGINGS;
    public static final DeferredItem<ArmorItem> JAPANESE_RED_BOOTS;
    public static final DeferredItem<BlockItem> WOODEN_SLIDING_DOOR_ITEM;
    public static final DeferredItem<BlockItem> JAPANESE_SLIDING_DOOR_ITEM;
    public static final DeferredItem<BlockItem> PAPER_WALL_ITEM;
    public static final DeferredItem<BlockItem> JAPANESE_TILES_ITEM;
    public static final DeferredItem<BlockItem> JAPANESE_TILES_STAIRS_ITEM;
    public static final DeferredItem<BlockItem> JAPANESE_TILES_SLAB_ITEM;
    public static final DeferredItem<BlockItem> JAPANESE_STONE_TILES_ITEM;
    public static final DeferredItem<BlockItem> GRAY_TILES_ITEM;
    public static final DeferredItem<BlockItem> GRAY_TILES_STAIRS_ITEM;
    public static final DeferredItem<BlockItem> GRAY_TILES_SLAB_ITEM;
    public static final DeferredItem<BlockItem> GREEN_TILES_ITEM;
    public static final DeferredItem<BlockItem> GREEN_TILES_STAIRS_ITEM;
    public static final DeferredItem<BlockItem> GREEN_TILES_SLAB_ITEM;
    public static final DeferredItem<BlockItem> RED_TILES_ITEM;
    public static final DeferredItem<BlockItem> RED_TILES_STAIRS_ITEM;
    public static final DeferredItem<BlockItem> RED_TILES_SLAB_ITEM;
    public static final DeferredItem<SwordItem> SELJUK_SCIMITAR;
    public static final DeferredItem<BowItem> SELJUK_BOW;
    public static final DeferredItem<ArmorItem> SELJUK_HELMET;
    public static final DeferredItem<ArmorItem> SELJUK_CHESTPLATE;
    public static final DeferredItem<ArmorItem> SELJUK_LEGGINGS;
    public static final DeferredItem<ArmorItem> SELJUK_BOOTS;
    public static final DeferredItem<ArmorItem> SELJUK_TURBAN;
    public static final DeferredItem<MillFoodItem> YOGURT;
    public static final DeferredItem<MillFoodItem> AYRAN;
    public static final DeferredItem<MillFoodItem> PIDE;
    public static final DeferredItem<MillFoodItem> LOKUM;
    public static final DeferredItem<MillFoodItem> HELVA;
    public static final DeferredItem<MillFoodItem> PISTACHIOS;
    public static final DeferredItem<Item> CLOTHES_SELJUK_WOOL;
    public static final DeferredItem<Item> CLOTHES_SELJUK_COTTON;
    public static final DeferredItem<WallDecorationItem> WALL_CARPET_SMALL;
    public static final DeferredItem<WallDecorationItem> WALL_CARPET_MEDIUM;
    public static final DeferredItem<WallDecorationItem> WALL_CARPET_LARGE;
    public static final DeferredItem<BlockItem> PISTACHIO_TREE_SAPLING_ITEM;
    public static final DeferredItem<BlockItem> PISTACHIO_TREE_LEAVES_ITEM;
    public static final DeferredItem<BlockItem> MUD_BRICK_SELJUK_ORNAMENTED_ITEM;
    public static final DeferredItem<BlockItem> MUD_BRICK_SELJUK_DECORATED_ITEM;
    public static final DeferredItem<BlockItem> MUD_BRICK_SMOOTH_ITEM;
    public static final DeferredItem<MillFoodItem> BEARMEAT_RAW;
    public static final DeferredItem<MillFoodItem> BEARMEAT_COOKED;
    public static final DeferredItem<MillFoodItem> WOLFMEAT_RAW;
    public static final DeferredItem<MillFoodItem> WOLFMEAT_COOKED;
    public static final DeferredItem<MillFoodItem> SEAFOOD_RAW;
    public static final DeferredItem<MillFoodItem> SEAFOOD_COOKED;
    public static final DeferredItem<MillFoodItem> INUIT_POTATO_STEW;
    public static final DeferredItem<MillFoodItem> INUIT_MEATY_STEW;
    public static final DeferredItem<MillFoodItem> INUIT_BEAR_STEW;
    public static final DeferredItem<SwordItem> INUIT_TRIDENT;
    public static final DeferredItem<MillenaireBow> INUIT_BOW;
    public static final DeferredItem<ArmorItem> FUR_HELMET;
    public static final DeferredItem<ArmorItem> FUR_CHESTPLATE;
    public static final DeferredItem<ArmorItem> FUR_LEGGINGS;
    public static final DeferredItem<ArmorItem> FUR_BOOTS;
    public static final DeferredItem<Item> TANNED_HIDE;
    public static final DeferredItem<BlockItem> SNOW_BRICK_ITEM;
    public static final DeferredItem<BlockItem> ICE_BRICK_ITEM;
    public static final DeferredItem<BlockItem> SNOW_WALL_ITEM;
    public static final DeferredItem<BlockItem> SOD_OAK_ITEM;
    public static final DeferredItem<BlockItem> SOD_SPRUCE_ITEM;
    public static final DeferredItem<BlockItem> SOD_BIRCH_ITEM;
    public static final DeferredItem<BlockItem> SOD_JUNGLE_ITEM;
    public static final DeferredItem<BlockItem> SOD_ACACIA_ITEM;
    public static final DeferredItem<BlockItem> SOD_DARK_OAK_ITEM;
    public static final DeferredItem<BlockItem> INUIT_CARVING_ITEM;
    public static final DeferredItem<BlockItem> FIRE_PIT_ITEM;

    private static TagKey<BannerPattern> bannerPatternTag(String path) {
        return TagKey.create((ResourceKey)Registries.BANNER_PATTERN, (ResourceLocation)ResourceLocation.fromNamespaceAndPath((String)"millenaire", (String)("pattern_item/" + path)));
    }

    private ModItems() {
    }

    public static void register(IEventBus modEventBus) {
        ITEMS.register(modEventBus);
    }

    static {
        for (DyeColor color : DyeColor.values()) {
            PAINTED_BRICK_ITEMS.put(color, (DeferredItem<BlockItem>)ITEMS.registerSimpleBlockItem((Holder)ModBlocks.PAINTED_BRICKS.get((Object)color)));
            DECORATED_BRICK_ITEMS.put(color, (DeferredItem<BlockItem>)ITEMS.registerSimpleBlockItem((Holder)ModBlocks.DECORATED_BRICKS.get((Object)color)));
            PAINTED_BRICK_STAIRS_ITEMS.put(color, (DeferredItem<BlockItem>)ITEMS.registerSimpleBlockItem((Holder)ModBlocks.PAINTED_BRICK_STAIRS.get((Object)color)));
            PAINTED_BRICK_SLAB_ITEMS.put(color, (DeferredItem<BlockItem>)ITEMS.registerSimpleBlockItem((Holder)ModBlocks.PAINTED_BRICK_SLABS.get((Object)color)));
            PAINTED_BRICK_WALL_ITEMS.put(color, (DeferredItem<BlockItem>)ITEMS.registerSimpleBlockItem((Holder)ModBlocks.PAINTED_BRICK_WALLS.get((Object)color)));
        }
        PAINT_BUCKETS = new EnumMap<DyeColor, DeferredItem<Item>>(DyeColor.class);
        for (DyeColor color : DyeColor.values()) {
            PAINT_BUCKETS.put(color, (DeferredItem<Item>)ITEMS.registerItem("paint_bucket_" + color.getSerializedName(), props -> new PaintBucketItem(color, props.stacksTo(1).durability(2048))));
        }
        APPLE_TREE_SAPLING_ITEM = ITEMS.registerItem("apple_tree_sapling", props -> new LearnedSaplingItem((Block)ModBlocks.APPLE_TREE_SAPLING.get(), "sapling_appletree", (Item.Properties)props));
        APPLE_TREE_LEAVES_ITEM = ITEMS.registerSimpleBlockItem(ModBlocks.APPLE_TREE_LEAVES);
        LOCKED_CHEST_ITEM = ITEMS.registerSimpleBlockItem(ModBlocks.LOCKED_CHEST);
        VILLAGE_PANEL_ITEM = ITEMS.registerSimpleBlockItem(ModBlocks.VILLAGE_PANEL);
        IMPORT_TABLE_ITEM = ITEMS.register("import_table", key -> new ImportTableItem((Block)ModBlocks.IMPORT_TABLE.get(), new Item.Properties()));
        MOCK_MARKER_ITEM = ITEMS.registerSimpleBlockItem(ModBlocks.MOCK_MARKER);
        MOCK_SOIL_ITEM = ITEMS.registerSimpleBlockItem(ModBlocks.MOCK_SOIL);
        MOCK_SOURCE_ITEM = ITEMS.registerSimpleBlockItem(ModBlocks.MOCK_SOURCE);
        MOCK_FREE_ITEM = ITEMS.registerSimpleBlockItem(ModBlocks.MOCK_FREE);
        MOCK_TREE_SPAWN_ITEM = ITEMS.registerSimpleBlockItem(ModBlocks.MOCK_TREE_SPAWN);
        MOCK_ANIMAL_SPAWN_ITEM = ITEMS.registerSimpleBlockItem(ModBlocks.MOCK_ANIMAL_SPAWN);
        MOCK_CHEST_ITEM = ITEMS.registerSimpleBlockItem(ModBlocks.MOCK_CHEST);
        MOCK_DECOR_ITEM = ITEMS.registerSimpleBlockItem(ModBlocks.MOCK_DECOR);
        MOCK_FACING_MARKER_ITEM = ITEMS.registerSimpleBlockItem(ModBlocks.MOCK_FACING_MARKER);
        MOCK_BANNER_WALL_ITEM = ITEMS.registerSimpleBlockItem(ModBlocks.MOCK_BANNER_WALL);
        MOCK_BANNER_STANDING_ITEM = ITEMS.registerSimpleBlockItem(ModBlocks.MOCK_BANNER_STANDING);
        MASA = ITEMS.registerItem("masa", props -> new MillFoodItem(MillFoodItem.multiPortionProperties(props, 4), 6, 0.6f, 0, 0, 0, false));
        WAH = ITEMS.registerItem("wah", props -> new MillFoodItem(MillFoodItem.multiPortionProperties(props, 6), 10, 1.0f, 0, 0, 0, false));
        CACAUHAA = ITEMS.registerItem("cacauhaa", props -> new MillFoodItem(MillFoodItem.multiPortionProperties(props, 6), 0, 0.0f, 6, 30, 0, true));
        MAIZE = ITEMS.registerItem("maize", props -> new CropSeedItem((Supplier<? extends Block>)ModBlocks.CROP_MAIZE, "maize", props.stacksTo(64)));
        MAYAN_PICKAXE = ITEMS.registerItem("mayan_pickaxe", props -> new PickaxeItem(MayanMaterials.OBSIDIAN_TOOL, props.attributes(DiggerItem.createAttributes((Tier)MayanMaterials.OBSIDIAN_TOOL, (float)1.0f, (float)-2.8f))));
        MAYAN_AXE = ITEMS.registerItem("mayan_axe", props -> new AxeItem(MayanMaterials.OBSIDIAN_TOOL, props.attributes(DiggerItem.createAttributes((Tier)MayanMaterials.OBSIDIAN_TOOL, (float)5.0f, (float)-3.0f))));
        MAYAN_SHOVEL = ITEMS.registerItem("mayan_shovel", props -> new ShovelItem(MayanMaterials.OBSIDIAN_TOOL, props.attributes(DiggerItem.createAttributes((Tier)MayanMaterials.OBSIDIAN_TOOL, (float)1.5f, (float)-3.0f))));
        MAYAN_HOE = ITEMS.registerItem("mayan_hoe", props -> new HoeItem(MayanMaterials.OBSIDIAN_TOOL, props.attributes(DiggerItem.createAttributes((Tier)MayanMaterials.OBSIDIAN_TOOL, (float)-1.0f, (float)-2.0f))));
        MAYAN_MACE = ITEMS.registerItem("mayan_mace", props -> new SwordItem(MayanMaterials.IRON_TOOL, props.attributes(SwordItem.createAttributes((Tier)MayanMaterials.IRON_TOOL, (float)3.0f, (float)-2.4f))));
        OBSIDIAN_FLAKE = ITEMS.registerSimpleItem("obsidian_flake", new Item.Properties().stacksTo(64));
        MAYAN_PATTERN = ITEMS.registerItem("mayan_pattern", props -> new BannerPatternItem(ModItems.bannerPatternTag("mayan"), props.stacksTo(1)));
        MAYAN_PATTERN_1 = ITEMS.registerItem("mayan_pattern_1", props -> new BannerPatternItem(ModItems.bannerPatternTag("mayan_1"), props.stacksTo(1)));
        MAYAN_PATTERN_2 = ITEMS.registerItem("mayan_pattern_2", props -> new BannerPatternItem(ModItems.bannerPatternTag("mayan_2"), props.stacksTo(1)));
        MAYAN_PATTERN_3 = ITEMS.registerItem("mayan_pattern_3", props -> new BannerPatternItem(ModItems.bannerPatternTag("mayan_3"), props.stacksTo(1)));
        MAYAN_PATTERN_4 = ITEMS.registerItem("mayan_pattern_4", props -> new BannerPatternItem(ModItems.bannerPatternTag("mayan_4"), props.stacksTo(1)));
        OLIVES = ITEMS.registerItem("olives", props -> new MillFoodItem(props.stacksTo(64), 1, 0.05f, 0, 0, 0, false));
        OLIVEOIL = ITEMS.registerItem("oliveoil", props -> new MillFoodItem(props.stacksTo(16), 0, 0.0f, 0, 0, 0, 0, true, true, List.of()));
        FETA = ITEMS.registerItem("feta", props -> new MillFoodItem(props.stacksTo(64), 0, 0.0f, 2, 15, 0, false));
        SOUVLAKI = ITEMS.registerItem("souvlaki", props -> new MillFoodItem(MillFoodItem.multiPortionProperties(props, 8), 10, 1.0f, 0, 0, 0, 0, false, false, List.of(new MobEffectInstance(MobEffects.HEAL, 1, 1))));
        WINEBASIC = ITEMS.registerItem("winebasic", props -> new MillFoodItem(MillFoodItem.multiPortionProperties(props, 6), 0, 0.0f, 3, 15, 5, true));
        WINEFANCY = ITEMS.registerItem("winefancy", props -> new MillFoodItem(MillFoodItem.multiPortionProperties(props, 16), 0, 0.0f, 8, 30, 5, 0, true, false, List.of(new MobEffectInstance(MobEffects.DAMAGE_RESISTANCE, 9600, 1))));
        GRAPES = ITEMS.registerItem("grapes", props -> new CropSeedItem((Supplier<? extends Block>)ModBlocks.CROP_VINE, "grapes", props.stacksTo(64)));
        BYZANTINE_MACE = ITEMS.registerItem("byzantine_mace", props -> new SwordItem(ByzantineMaterials.BYZANTINE_TOOL, props.attributes(SwordItem.createAttributes((Tier)ByzantineMaterials.BYZANTINE_TOOL, (float)3.0f, (float)-2.4f))));
        BYZANTINE_HELMET = ITEMS.registerItem("byzantine_helmet", props -> new ArmorItem(ByzantineMaterials.BYZANTINE_ARMOR, ArmorItem.Type.HELMET, props.durability(ArmorItem.Type.HELMET.getDurability(33))));
        BYZANTINE_CHESTPLATE = ITEMS.registerItem("byzantine_chestplate", props -> new ArmorItem(ByzantineMaterials.BYZANTINE_ARMOR, ArmorItem.Type.CHESTPLATE, props.durability(ArmorItem.Type.CHESTPLATE.getDurability(33))));
        BYZANTINE_LEGGINGS = ITEMS.registerItem("byzantine_leggings", props -> new ArmorItem(ByzantineMaterials.BYZANTINE_ARMOR, ArmorItem.Type.LEGGINGS, props.durability(ArmorItem.Type.LEGGINGS.getDurability(33))));
        BYZANTINE_BOOTS = ITEMS.registerItem("byzantine_boots", props -> new ArmorItem(ByzantineMaterials.BYZANTINE_ARMOR, ArmorItem.Type.BOOTS, props.durability(ArmorItem.Type.BOOTS.getDurability(33))));
        BYZANTINE_PICKAXE = ITEMS.registerItem("byzantine_pickaxe", props -> new PickaxeItem(ByzantineMaterials.BYZANTINE_TOOL, props.attributes(DiggerItem.createAttributes((Tier)ByzantineMaterials.BYZANTINE_TOOL, (float)1.0f, (float)-2.8f))));
        BYZANTINE_AXE = ITEMS.registerItem("byzantine_axe", props -> new AxeItem(ByzantineMaterials.BYZANTINE_TOOL, props.attributes(DiggerItem.createAttributes((Tier)ByzantineMaterials.BYZANTINE_TOOL, (float)5.0f, (float)-3.0f))));
        BYZANTINE_SHOVEL = ITEMS.registerItem("byzantine_shovel", props -> new ShovelItem(ByzantineMaterials.BYZANTINE_TOOL, props.attributes(DiggerItem.createAttributes((Tier)ByzantineMaterials.BYZANTINE_TOOL, (float)1.5f, (float)-3.0f))));
        BYZANTINE_HOE = ITEMS.registerItem("byzantine_hoe", props -> new HoeItem(ByzantineMaterials.BYZANTINE_TOOL, props.attributes(DiggerItem.createAttributes((Tier)ByzantineMaterials.BYZANTINE_TOOL, (float)-1.0f, (float)-2.0f))));
        CLOTHES_BYZ_WOOL = ITEMS.registerItem("clothes_byz_wool", props -> new ClothItem("clothes_byz_wool", 1, props.stacksTo(1)));
        CLOTHES_BYZ_SILK = ITEMS.registerItem("clothes_byz_silk", props -> new ClothItem("clothes_byz_silk", 2, props.stacksTo(1)));
        BYZANTINE_FRESCO = ITEMS.registerSimpleItem("byzantine_fresco", new Item.Properties().stacksTo(64));
        BYZANTINE_PATTERN = ITEMS.registerItem("byzantine_pattern", props -> new BannerPatternItem(ModItems.bannerPatternTag("byzantine"), props.stacksTo(1)));
        BYZANTINE_PATTERN_1 = ITEMS.registerItem("byzantine_pattern_1", props -> new BannerPatternItem(ModItems.bannerPatternTag("byzantine_1"), props.stacksTo(1)));
        BYZANTINE_PATTERN_2 = ITEMS.registerItem("byzantine_pattern_2", props -> new BannerPatternItem(ModItems.bannerPatternTag("byzantine_2"), props.stacksTo(1)));
        TAPESTRY = ITEMS.registerItem("wall_tapestry", props -> new WallDecorationItem(props.stacksTo(64), WallDecorationType.NORMAN_TAPESTRY));
        INDIAN_STATUE = ITEMS.registerItem("wall_indian_statue", props -> new WallDecorationItem(props.stacksTo(64), WallDecorationType.INDIAN_STATUE));
        MAYAN_STATUE = ITEMS.registerItem("wall_mayan_statue", props -> new WallDecorationItem(props.stacksTo(64), WallDecorationType.MAYAN_STATUE));
        BYZANTINE_ICON_SMALL = ITEMS.registerItem("wall_byzantine_icon_small", props -> new WallDecorationItem(props.stacksTo(64), WallDecorationType.BYZANTINE_ICON_SMALL));
        BYZANTINE_ICON_MEDIUM = ITEMS.registerItem("wall_byzantine_icon_medium", props -> new WallDecorationItem(props.stacksTo(64), WallDecorationType.BYZANTINE_ICON_MEDIUM));
        BYZANTINE_ICON_LARGE = ITEMS.registerItem("wall_byzantine_icon_large", props -> new WallDecorationItem(props.stacksTo(64), WallDecorationType.BYZANTINE_ICON_LARGE));
        HIDE_HANGING = ITEMS.registerItem("wall_hide_hanging", props -> new WallDecorationItem(props.stacksTo(64), WallDecorationType.HIDE_HANGING));
        SAKE = ITEMS.registerItem("sake", props -> new MillFoodItem(MillFoodItem.multiPortionProperties(props, 8), 0, 0.0f, 8, 30, 10, 0, true, false, List.of(new MobEffectInstance(MobEffects.JUMP, 9600, 1))));
        UDON = ITEMS.registerItem("udon", props -> new MillFoodItem(MillFoodItem.multiPortionProperties(props, 6), 8, 0.8f, 0, 0, 0, false));
        IKAYAKI = ITEMS.registerItem("ikayaki", props -> new MillFoodItem(MillFoodItem.multiPortionProperties(props, 8), 10, 1.0f, 0, 0, 0, 0, false, false, List.of(new MobEffectInstance(MobEffects.WATER_BREATHING, 9600, 2))));
        JAPANESE_PATTERN = ITEMS.registerItem("jappattern", props -> new BannerPatternItem(ModItems.bannerPatternTag("japanese"), props.stacksTo(1)));
        JAPANESE_PATTERN_1 = ITEMS.registerItem("jappattern1", props -> new BannerPatternItem(ModItems.bannerPatternTag("japanese_agr"), props.stacksTo(1)));
        JAPANESE_PATTERN_2 = ITEMS.registerItem("jappattern2", props -> new BannerPatternItem(ModItems.bannerPatternTag("japanese_mil"), props.stacksTo(1)));
        JAPANESE_PATTERN_3 = ITEMS.registerItem("jappattern3", props -> new BannerPatternItem(ModItems.bannerPatternTag("japanese_rel"), props.stacksTo(1)));
        JAPANESE_PATTERN_4 = ITEMS.registerItem("jappattern4", props -> new BannerPatternItem(ModItems.bannerPatternTag("japanese_tra"), props.stacksTo(1)));
        JAPANESE_TACHI = ITEMS.registerItem("japanese_tachi", props -> new SwordItem(JapaneseMaterials.JAPANESE_TOOL, props.attributes(SwordItem.createAttributes((Tier)JapaneseMaterials.JAPANESE_TOOL, (float)3.0f, (float)-2.4f))));
        YUMIBOW = ITEMS.registerItem("yumibow", props -> new MillenaireBow((Item.Properties)props, 2.0f, 0.5f, 1));
        JAPANESE_GUARD_HELMET = ITEMS.registerItem("japaneseguardhelmet", props -> new ArmorItem(JapaneseMaterials.JAPANESE_GUARD_ARMOR, ArmorItem.Type.HELMET, props.durability(ArmorItem.Type.HELMET.getDurability(25))));
        JAPANESE_GUARD_CHESTPLATE = ITEMS.registerItem("japaneseguardplate", props -> new ArmorItem(JapaneseMaterials.JAPANESE_GUARD_ARMOR, ArmorItem.Type.CHESTPLATE, props.durability(ArmorItem.Type.CHESTPLATE.getDurability(25))));
        JAPANESE_GUARD_LEGGINGS = ITEMS.registerItem("japaneseguardlegs", props -> new ArmorItem(JapaneseMaterials.JAPANESE_GUARD_ARMOR, ArmorItem.Type.LEGGINGS, props.durability(ArmorItem.Type.LEGGINGS.getDurability(25))));
        JAPANESE_GUARD_BOOTS = ITEMS.registerItem("japaneseguardboots", props -> new ArmorItem(JapaneseMaterials.JAPANESE_GUARD_ARMOR, ArmorItem.Type.BOOTS, props.durability(ArmorItem.Type.BOOTS.getDurability(25))));
        JAPANESE_BLUE_HELMET = ITEMS.registerItem("japanesebluehelmet", props -> new ArmorItem(JapaneseMaterials.JAPANESE_BLUE_ARMOR, ArmorItem.Type.HELMET, props.durability(ArmorItem.Type.HELMET.getDurability(33))));
        JAPANESE_BLUE_CHESTPLATE = ITEMS.registerItem("japaneseblueplate", props -> new ArmorItem(JapaneseMaterials.JAPANESE_BLUE_ARMOR, ArmorItem.Type.CHESTPLATE, props.durability(ArmorItem.Type.CHESTPLATE.getDurability(33))));
        JAPANESE_BLUE_LEGGINGS = ITEMS.registerItem("japanesebluelegs", props -> new ArmorItem(JapaneseMaterials.JAPANESE_BLUE_ARMOR, ArmorItem.Type.LEGGINGS, props.durability(ArmorItem.Type.LEGGINGS.getDurability(33))));
        JAPANESE_BLUE_BOOTS = ITEMS.registerItem("japaneseblueboots", props -> new ArmorItem(JapaneseMaterials.JAPANESE_BLUE_ARMOR, ArmorItem.Type.BOOTS, props.durability(ArmorItem.Type.BOOTS.getDurability(33))));
        JAPANESE_RED_HELMET = ITEMS.registerItem("japaneseredhelmet", props -> new ArmorItem(JapaneseMaterials.JAPANESE_RED_ARMOR, ArmorItem.Type.HELMET, props.durability(ArmorItem.Type.HELMET.getDurability(33))));
        JAPANESE_RED_CHESTPLATE = ITEMS.registerItem("japaneseredplate", props -> new ArmorItem(JapaneseMaterials.JAPANESE_RED_ARMOR, ArmorItem.Type.CHESTPLATE, props.durability(ArmorItem.Type.CHESTPLATE.getDurability(33))));
        JAPANESE_RED_LEGGINGS = ITEMS.registerItem("japaneseredlegs", props -> new ArmorItem(JapaneseMaterials.JAPANESE_RED_ARMOR, ArmorItem.Type.LEGGINGS, props.durability(ArmorItem.Type.LEGGINGS.getDurability(33))));
        JAPANESE_RED_BOOTS = ITEMS.registerItem("japaneseredboots", props -> new ArmorItem(JapaneseMaterials.JAPANESE_RED_ARMOR, ArmorItem.Type.BOOTS, props.durability(ArmorItem.Type.BOOTS.getDurability(33))));
        WOODEN_SLIDING_DOOR_ITEM = ITEMS.register("wooden_sliding_door", key -> new DoubleHighBlockItem((Block)ModBlocks.WOODEN_SLIDING_DOOR.get(), new Item.Properties()));
        JAPANESE_SLIDING_DOOR_ITEM = ITEMS.register("japanese_sliding_door", key -> new DoubleHighBlockItem((Block)ModBlocks.JAPANESE_SLIDING_DOOR.get(), new Item.Properties()));
        PAPER_WALL_ITEM = ITEMS.registerSimpleBlockItem(ModBlocks.PAPER_WALL);
        JAPANESE_TILES_ITEM = ITEMS.registerSimpleBlockItem(ModBlocks.JAPANESE_TILES);
        JAPANESE_TILES_STAIRS_ITEM = ITEMS.registerSimpleBlockItem(ModBlocks.JAPANESE_TILES_STAIRS);
        JAPANESE_TILES_SLAB_ITEM = ITEMS.registerSimpleBlockItem(ModBlocks.JAPANESE_TILES_SLAB);
        JAPANESE_STONE_TILES_ITEM = ITEMS.registerSimpleBlockItem(ModBlocks.JAPANESE_STONE_TILES);
        GRAY_TILES_ITEM = ITEMS.registerSimpleBlockItem(ModBlocks.GRAY_TILES);
        GRAY_TILES_STAIRS_ITEM = ITEMS.registerSimpleBlockItem(ModBlocks.GRAY_TILES_STAIRS);
        GRAY_TILES_SLAB_ITEM = ITEMS.registerSimpleBlockItem(ModBlocks.GRAY_TILES_SLAB);
        GREEN_TILES_ITEM = ITEMS.registerSimpleBlockItem(ModBlocks.GREEN_TILES);
        GREEN_TILES_STAIRS_ITEM = ITEMS.registerSimpleBlockItem(ModBlocks.GREEN_TILES_STAIRS);
        GREEN_TILES_SLAB_ITEM = ITEMS.registerSimpleBlockItem(ModBlocks.GREEN_TILES_SLAB);
        RED_TILES_ITEM = ITEMS.registerSimpleBlockItem(ModBlocks.RED_TILES);
        RED_TILES_STAIRS_ITEM = ITEMS.registerSimpleBlockItem(ModBlocks.RED_TILES_STAIRS);
        RED_TILES_SLAB_ITEM = ITEMS.registerSimpleBlockItem(ModBlocks.RED_TILES_SLAB);
        SELJUK_SCIMITAR = ITEMS.registerItem("seljuk_scimitar", props -> new SwordItem(SeljukMaterials.SELJUK_TOOL, props.attributes(SwordItem.createAttributes((Tier)SeljukMaterials.SELJUK_TOOL, (float)3.0f, (float)-2.4f))));
        SELJUK_BOW = ITEMS.registerItem("seljuk_bow", props -> new BowItem(props.durability(384)));
        SELJUK_HELMET = ITEMS.registerItem("seljuk_helmet", props -> new ArmorItem(SeljukMaterials.SELJUK_ARMOR, ArmorItem.Type.HELMET, props.durability(ArmorItem.Type.HELMET.getDurability(66))));
        SELJUK_CHESTPLATE = ITEMS.registerItem("seljuk_chestplate", props -> new ArmorItem(SeljukMaterials.SELJUK_ARMOR, ArmorItem.Type.CHESTPLATE, props.durability(ArmorItem.Type.CHESTPLATE.getDurability(66))));
        SELJUK_LEGGINGS = ITEMS.registerItem("seljuk_leggings", props -> new ArmorItem(SeljukMaterials.SELJUK_ARMOR, ArmorItem.Type.LEGGINGS, props.durability(ArmorItem.Type.LEGGINGS.getDurability(66))));
        SELJUK_BOOTS = ITEMS.registerItem("seljuk_boots", props -> new ArmorItem(SeljukMaterials.SELJUK_ARMOR, ArmorItem.Type.BOOTS, props.durability(ArmorItem.Type.BOOTS.getDurability(66))));
        SELJUK_TURBAN = ITEMS.registerItem("seljuk_turban", props -> new ArmorItem(SeljukMaterials.SELJUK_WOOL_ARMOR, ArmorItem.Type.HELMET, props.durability(ArmorItem.Type.HELMET.getDurability(7))));
        YOGURT = ITEMS.registerItem("yogurt", props -> new MillFoodItem(props.stacksTo(64), 2, 0.3f, 0, 0, 0, false));
        AYRAN = ITEMS.registerItem("ayran", props -> new MillFoodItem(MillFoodItem.multiPortionProperties(props, 4), 0, 0.0f, 2, 10, 0, true));
        PIDE = ITEMS.registerItem("pide", props -> new MillFoodItem(MillFoodItem.multiPortionProperties(props, 6), 6, 0.8f, 0, 0, 0, false));
        LOKUM = ITEMS.registerItem("lokum", props -> new MillFoodItem(props.stacksTo(64), 3, 0.4f, 0, 0, 0, false));
        HELVA = ITEMS.registerItem("helva", props -> new MillFoodItem(props.stacksTo(64), 4, 0.5f, 0, 0, 0, false));
        PISTACHIOS = ITEMS.registerItem("pistachios", props -> new MillFoodItem(props.stacksTo(64), 1, 0.1f, 0, 0, 0, false));
        CLOTHES_SELJUK_WOOL = ITEMS.registerSimpleItem("clothes_seljuk_wool", new Item.Properties().stacksTo(64));
        CLOTHES_SELJUK_COTTON = ITEMS.registerSimpleItem("clothes_seljuk_cotton", new Item.Properties().stacksTo(64));
        WALL_CARPET_SMALL = ITEMS.registerItem("wall_carpet_small", props -> new WallDecorationItem(props.stacksTo(64), WallDecorationType.WALL_CARPET_SMALL));
        WALL_CARPET_MEDIUM = ITEMS.registerItem("wall_carpet_medium", props -> new WallDecorationItem(props.stacksTo(64), WallDecorationType.WALL_CARPET_MEDIUM));
        WALL_CARPET_LARGE = ITEMS.registerItem("wall_carpet_large", props -> new WallDecorationItem(props.stacksTo(64), WallDecorationType.WALL_CARPET_LARGE));
        PISTACHIO_TREE_SAPLING_ITEM = ITEMS.registerItem("pistachio_tree_sapling", props -> new LearnedSaplingItem((Block)ModBlocks.PISTACHIO_TREE_SAPLING.get(), "sapling_pistachio", (Item.Properties)props));
        PISTACHIO_TREE_LEAVES_ITEM = ITEMS.registerSimpleBlockItem(ModBlocks.PISTACHIO_TREE_LEAVES);
        MUD_BRICK_SELJUK_ORNAMENTED_ITEM = ITEMS.registerSimpleBlockItem(ModBlocks.MUD_BRICK_SELJUK_ORNAMENTED);
        MUD_BRICK_SELJUK_DECORATED_ITEM = ITEMS.registerSimpleBlockItem(ModBlocks.MUD_BRICK_SELJUK_DECORATED);
        MUD_BRICK_SMOOTH_ITEM = ITEMS.registerSimpleBlockItem(ModBlocks.MUD_BRICK_SMOOTH);
        BEARMEAT_RAW = ITEMS.registerItem("bearmeat_raw", props -> new MillFoodItem(props.stacksTo(64), 4, 0.5f, 0, 0, 0, 0, false, false, List.of(new MobEffectInstance(MobEffects.DAMAGE_BOOST, 4800, 0))));
        BEARMEAT_COOKED = ITEMS.registerItem("bearmeat_cooked", props -> new MillFoodItem(props.stacksTo(64), 10, 1.0f, 0, 0, 0, 0, false, false, List.of(new MobEffectInstance(MobEffects.DAMAGE_BOOST, 9600, 1))));
        WOLFMEAT_RAW = ITEMS.registerItem("wolfmeat_raw", props -> new MillFoodItem(props.stacksTo(64), 3, 0.3f, 0, 0, 0, false));
        WOLFMEAT_COOKED = ITEMS.registerItem("wolfmeat_cooked", props -> new MillFoodItem(props.stacksTo(64), 5, 0.6f, 0, 0, 0, 0, false, false, List.of(new MobEffectInstance(MobEffects.DAMAGE_BOOST, 1200, 0))));
        SEAFOOD_RAW = ITEMS.registerItem("seafood_raw", props -> new MillFoodItem(props.stacksTo(1), 2, 0.2f, 0, 0, 0, false));
        SEAFOOD_COOKED = ITEMS.registerItem("seafood_cooked", props -> new MillFoodItem(props.stacksTo(1), 2, 0.25f, 0, 0, 0, 0, false, false, List.of(new MobEffectInstance(MobEffects.WATER_BREATHING, 1200, 0))));
        INUIT_POTATO_STEW = ITEMS.registerItem("inuitpotatostew", props -> new MillFoodItem(MillFoodItem.multiPortionProperties(props, 6), 6, 0.6f, 0, 0, 0, false));
        INUIT_MEATY_STEW = ITEMS.registerItem("inuitmeatystew", props -> new MillFoodItem(MillFoodItem.multiPortionProperties(props, 8), 8, 0.8f, 0, 0, 0, false));
        INUIT_BEAR_STEW = ITEMS.registerItem("inuitbearstew", props -> new MillFoodItem(MillFoodItem.multiPortionProperties(props, 8), 8, 1.0f, 0, 0, 0, 0, false, false, List.of(new MobEffectInstance(MobEffects.DAMAGE_BOOST, 9600, 3))));
        INUIT_TRIDENT = ITEMS.registerItem("inuittrident", props -> new SwordItem((Tier)Tiers.IRON, props.durability(20).attributes(SwordItem.createAttributes((Tier)Tiers.IRON, (int)3, (float)-2.4f))));
        INUIT_BOW = ITEMS.registerItem("inuitbow", props -> new MillenaireBow((Item.Properties)props, 1.0f, 0.0f, 20));
        FUR_HELMET = ITEMS.registerItem("furhelmet", props -> new ArmorItem(InuitMaterials.FUR_ARMOR, ArmorItem.Type.HELMET, props.durability(ArmorItem.Type.HELMET.getDurability(7))));
        FUR_CHESTPLATE = ITEMS.registerItem("furplate", props -> new ArmorItem(InuitMaterials.FUR_ARMOR, ArmorItem.Type.CHESTPLATE, props.durability(ArmorItem.Type.CHESTPLATE.getDurability(7))));
        FUR_LEGGINGS = ITEMS.registerItem("furlegs", props -> new ArmorItem(InuitMaterials.FUR_ARMOR, ArmorItem.Type.LEGGINGS, props.durability(ArmorItem.Type.LEGGINGS.getDurability(7))));
        FUR_BOOTS = ITEMS.registerItem("furboots", props -> new ArmorItem(InuitMaterials.FUR_ARMOR, ArmorItem.Type.BOOTS, props.durability(ArmorItem.Type.BOOTS.getDurability(7))));
        TANNED_HIDE = ITEMS.registerSimpleItem("tannedhide", new Item.Properties().stacksTo(64));
        SNOW_BRICK_ITEM = ITEMS.registerSimpleBlockItem(ModBlocks.SNOW_BRICK);
        ICE_BRICK_ITEM = ITEMS.registerSimpleBlockItem(ModBlocks.ICE_BRICK);
        SNOW_WALL_ITEM = ITEMS.registerSimpleBlockItem(ModBlocks.SNOW_WALL);
        SOD_OAK_ITEM = ITEMS.registerSimpleBlockItem(ModBlocks.SOD_OAK);
        SOD_SPRUCE_ITEM = ITEMS.registerSimpleBlockItem(ModBlocks.SOD_SPRUCE);
        SOD_BIRCH_ITEM = ITEMS.registerSimpleBlockItem(ModBlocks.SOD_BIRCH);
        SOD_JUNGLE_ITEM = ITEMS.registerSimpleBlockItem(ModBlocks.SOD_JUNGLE);
        SOD_ACACIA_ITEM = ITEMS.registerSimpleBlockItem(ModBlocks.SOD_ACACIA);
        SOD_DARK_OAK_ITEM = ITEMS.registerSimpleBlockItem(ModBlocks.SOD_DARK_OAK);
        INUIT_CARVING_ITEM = ITEMS.registerSimpleBlockItem(ModBlocks.INUIT_CARVING);
        FIRE_PIT_ITEM = ITEMS.registerSimpleBlockItem(ModBlocks.FIRE_PIT);
    }
}

