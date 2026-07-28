/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  net.minecraft.Util
 *  net.minecraft.core.Holder
 *  net.minecraft.core.Registry
 *  net.minecraft.core.registries.BuiltInRegistries
 *  net.minecraft.resources.ResourceLocation
 *  net.minecraft.sounds.SoundEvents
 *  net.minecraft.tags.BlockTags
 *  net.minecraft.world.item.ArmorItem$Type
 *  net.minecraft.world.item.ArmorMaterial
 *  net.minecraft.world.item.ArmorMaterial$Layer
 *  net.minecraft.world.item.Items
 *  net.minecraft.world.item.Tier
 *  net.minecraft.world.item.crafting.Ingredient
 *  net.minecraft.world.level.ItemLike
 *  net.neoforged.neoforge.common.SimpleTier
 */
package org.millenaire.item;

import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import net.minecraft.Util;
import net.minecraft.core.Holder;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.ArmorMaterial;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.Tier;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.level.ItemLike;
import net.neoforged.neoforge.common.SimpleTier;

public final class JapaneseMaterials {
    public static final Tier JAPANESE_TOOL = new SimpleTier(BlockTags.INCORRECT_FOR_DIAMOND_TOOL, 1561, 6.0f, 2.0f, 25, () -> Ingredient.of((ItemLike[])new ItemLike[]{Items.DIAMOND}));
    public static final int JAPANESE_GUARD_ARMOR_DURABILITY_MULTIPLIER = 25;
    public static final Holder<ArmorMaterial> JAPANESE_GUARD_ARMOR = Registry.registerForHolder((Registry)BuiltInRegistries.ARMOR_MATERIAL, (ResourceLocation)ResourceLocation.fromNamespaceAndPath((String)"millenaire", (String)"japanese_guard"), (Object)new ArmorMaterial((Map)Util.make(new EnumMap(ArmorItem.Type.class), map -> {
        map.put(ArmorItem.Type.BOOTS, 2);
        map.put(ArmorItem.Type.LEGGINGS, 5);
        map.put(ArmorItem.Type.CHESTPLATE, 4);
        map.put(ArmorItem.Type.HELMET, 1);
    }), 25, SoundEvents.ARMOR_EQUIP_IRON, () -> Ingredient.of((ItemLike[])new ItemLike[]{Items.IRON_INGOT}), List.of(new ArmorMaterial.Layer(ResourceLocation.fromNamespaceAndPath((String)"millenaire", (String)"japanese_guard"))), 0.0f, 0.0f));
    public static final int JAPANESE_BLUE_ARMOR_DURABILITY_MULTIPLIER = 33;
    public static final Holder<ArmorMaterial> JAPANESE_BLUE_ARMOR = Registry.registerForHolder((Registry)BuiltInRegistries.ARMOR_MATERIAL, (ResourceLocation)ResourceLocation.fromNamespaceAndPath((String)"millenaire", (String)"japanese_blue"), (Object)new ArmorMaterial((Map)Util.make(new EnumMap(ArmorItem.Type.class), map -> {
        map.put(ArmorItem.Type.BOOTS, 2);
        map.put(ArmorItem.Type.LEGGINGS, 6);
        map.put(ArmorItem.Type.CHESTPLATE, 5);
        map.put(ArmorItem.Type.HELMET, 2);
    }), 25, SoundEvents.ARMOR_EQUIP_IRON, () -> Ingredient.of((ItemLike[])new ItemLike[]{Items.IRON_INGOT}), List.of(new ArmorMaterial.Layer(ResourceLocation.fromNamespaceAndPath((String)"millenaire", (String)"japanese_blue"))), 0.0f, 0.0f));
    public static final int JAPANESE_RED_ARMOR_DURABILITY_MULTIPLIER = 33;
    public static final Holder<ArmorMaterial> JAPANESE_RED_ARMOR = Registry.registerForHolder((Registry)BuiltInRegistries.ARMOR_MATERIAL, (ResourceLocation)ResourceLocation.fromNamespaceAndPath((String)"millenaire", (String)"japanese_red"), (Object)new ArmorMaterial((Map)Util.make(new EnumMap(ArmorItem.Type.class), map -> {
        map.put(ArmorItem.Type.BOOTS, 2);
        map.put(ArmorItem.Type.LEGGINGS, 6);
        map.put(ArmorItem.Type.CHESTPLATE, 5);
        map.put(ArmorItem.Type.HELMET, 2);
    }), 25, SoundEvents.ARMOR_EQUIP_IRON, () -> Ingredient.of((ItemLike[])new ItemLike[]{Items.IRON_INGOT}), List.of(new ArmorMaterial.Layer(ResourceLocation.fromNamespaceAndPath((String)"millenaire", (String)"japanese_red"))), 0.0f, 0.0f));

    private JapaneseMaterials() {
    }
}

