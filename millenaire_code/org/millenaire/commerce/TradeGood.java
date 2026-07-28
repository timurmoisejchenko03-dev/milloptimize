/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  javax.annotation.Nullable
 *  net.minecraft.core.registries.BuiltInRegistries
 *  net.minecraft.core.registries.Registries
 *  net.minecraft.resources.ResourceKey
 *  net.minecraft.resources.ResourceLocation
 *  net.minecraft.tags.TagKey
 *  net.minecraft.world.item.Item
 *  net.minecraft.world.item.ItemStack
 */
package org.millenaire.commerce;

import javax.annotation.Nullable;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import org.millenaire.item.ItemHelper;

public record TradeGood(String id, String item, int sellingPrice, int buyingPrice, int reservedQuantity, int targetQuantity, boolean autoGenerate, int minReputation, String category, boolean travelBookDisplay, int foreignMerchantPrice) {
    public boolean canSell() {
        return this.sellingPrice > 0;
    }

    public boolean canBuy() {
        return this.buyingPrice > 0;
    }

    public boolean isTag() {
        return this.item.startsWith("#");
    }

    public ResourceLocation itemLocation() {
        return ResourceLocation.parse((String)(this.isTag() ? this.item.substring(1) : this.item));
    }

    public boolean matchesItem(ItemStack stack) {
        if (stack.isEmpty()) {
            return false;
        }
        if (this.isTag()) {
            TagKey tag = TagKey.create((ResourceKey)Registries.ITEM, (ResourceLocation)this.itemLocation());
            return stack.is(tag);
        }
        return BuiltInRegistries.ITEM.getKey((Object)stack.getItem()).equals((Object)this.itemLocation());
    }

    @Nullable
    public Item resolveItem() {
        return ItemHelper.resolve(this.item);
    }
}

