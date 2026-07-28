/*
 * Decompiled with CFR 0.150.
 * 
 * Could not load the following classes:
 *  javax.annotation.Nullable
 *  net.minecraft.core.Holder
 *  net.minecraft.core.HolderSet$Named
 *  net.minecraft.core.registries.BuiltInRegistries
 *  net.minecraft.core.registries.Registries
 *  net.minecraft.resources.ResourceKey
 *  net.minecraft.resources.ResourceLocation
 *  net.minecraft.tags.TagKey
 *  net.minecraft.world.item.Item
 */
package org.millenaire.item;

import java.util.Iterator;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import javax.annotation.Nullable;
import net.minecraft.core.Holder;
import net.minecraft.core.HolderSet;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;

public final class ItemHelper {
    private static final Map<String, Item> CACHE = new ConcurrentHashMap<String, Item>();

    private ItemHelper() {
    }

    @Nullable
    public static Item resolve(String itemId) {
        if (itemId == null || itemId.isEmpty()) {
            return null;
        }
        if (itemId.startsWith("#")) {
            Iterator it;
            ResourceLocation tagId = ResourceLocation.tryParse((String)itemId.substring(1));
            if (tagId == null) {
                return null;
            }
            TagKey tag = TagKey.create((ResourceKey)Registries.ITEM, (ResourceLocation)tagId);
            Optional entries = BuiltInRegistries.ITEM.getTag(tag);
            if (entries.isPresent() && (it = ((HolderSet.Named)entries.get()).iterator()).hasNext()) {
                return (Item)((Holder)it.next()).value();
            }
            return null;
        }
        return CACHE.computeIfAbsent(itemId, id -> {
            ResourceLocation rl = ResourceLocation.tryParse((String)id);
            return rl == null ? null : (Item)BuiltInRegistries.ITEM.getOptional(rl).orElse(null);
        });
    }

    @Nullable
    public static Item resolve(ResourceLocation itemId) {
        if (itemId == null) {
            return null;
        }
        return CACHE.computeIfAbsent(itemId.toString(), id -> BuiltInRegistries.ITEM.getOptional(itemId).orElse(null));
    }

    public static void clearCache() {
        CACHE.clear();
    }
}

