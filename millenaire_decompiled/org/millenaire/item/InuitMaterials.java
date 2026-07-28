/*
 * Decompiled with CFR 0.150.
 * 
 * Could not load the following classes:
 *  net.minecraft.Util
 *  net.minecraft.core.Holder
 *  net.minecraft.core.Registry
 *  net.minecraft.core.registries.BuiltInRegistries
 *  net.minecraft.resources.ResourceLocation
 *  net.minecraft.sounds.SoundEvents
 *  net.minecraft.world.item.ArmorItem$Type
 *  net.minecraft.world.item.ArmorMaterial
 *  net.minecraft.world.item.ArmorMaterial$Layer
 *  net.minecraft.world.item.Items
 *  net.minecraft.world.item.crafting.Ingredient
 *  net.minecraft.world.level.ItemLike
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
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.ArmorMaterial;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.level.ItemLike;

public final class InuitMaterials {
    public static final int FUR_ARMOR_DURABILITY_MULTIPLIER = 7;
    public static final Holder<ArmorMaterial> FUR_ARMOR = Registry.registerForHolder((Registry)BuiltInRegistries.ARMOR_MATERIAL, (ResourceLocation)ResourceLocation.fromNamespaceAndPath((String)"millenaire", (String)"fur"), (Object)new ArmorMaterial((Map)Util.make(new EnumMap(ArmorItem.Type.class), map -> {
        map.put(ArmorItem.Type.BOOTS, 2);
        map.put(ArmorItem.Type.LEGGINGS, 5);
        map.put(ArmorItem.Type.CHESTPLATE, 3);
        map.put(ArmorItem.Type.HELMET, 1);
    }), 25, SoundEvents.ARMOR_EQUIP_LEATHER, () -> Ingredient.of((ItemLike[])new ItemLike[]{Items.LEATHER}), List.of(new ArmorMaterial.Layer(ResourceLocation.fromNamespaceAndPath((String)"millenaire", (String)"fur"))), 0.0f, 0.0f));

    private InuitMaterials() {
    }
}

