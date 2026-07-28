/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  net.minecraft.core.registries.BuiltInRegistries
 *  net.minecraft.nbt.CompoundTag
 *  net.minecraft.nbt.ListTag
 *  net.minecraft.nbt.Tag
 *  net.minecraft.resources.ResourceLocation
 *  net.minecraft.tags.TagKey
 *  net.minecraft.world.item.Item
 */
package org.millenaire.entity;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;
import org.millenaire.item.ItemHelper;

public class VillagerInventory {
    private final Map<Item, Integer> items = new HashMap<Item, Integer>();
    private int modCount;

    public int modCount() {
        return this.modCount;
    }

    public void add(Item item, int count) {
        if (count <= 0) {
            return;
        }
        this.items.merge(item, count, Integer::sum);
        ++this.modCount;
    }

    public int remove(Item item, int count) {
        if (count <= 0) {
            return 0;
        }
        int current = this.items.getOrDefault(item, 0);
        if (current <= 0) {
            return 0;
        }
        int removed = Math.min(current, count);
        int remaining = current - removed;
        if (remaining <= 0) {
            this.items.remove(item);
        } else {
            this.items.put(item, remaining);
        }
        ++this.modCount;
        return removed;
    }

    public int getCount(Item item) {
        return this.items.getOrDefault(item, 0);
    }

    public boolean has(Item item, int count) {
        return this.getCount(item) >= count;
    }

    public int getCountByTag(TagKey<Item> tag) {
        int total = 0;
        for (Map.Entry<Item, Integer> entry : this.items.entrySet()) {
            if (!entry.getKey().builtInRegistryHolder().is(tag)) continue;
            total += entry.getValue().intValue();
        }
        return total;
    }

    public boolean hasByTag(TagKey<Item> tag, int count) {
        return this.getCountByTag(tag) >= count;
    }

    public boolean isEmpty() {
        return this.items.isEmpty();
    }

    public void clear() {
        this.items.clear();
        ++this.modCount;
    }

    public Map<Item, Integer> getAll() {
        return Collections.unmodifiableMap(new HashMap<Item, Integer>(this.items));
    }

    public void save(CompoundTag tag) {
        ListTag list = new ListTag();
        for (Map.Entry<Item, Integer> entry : this.items.entrySet()) {
            ResourceLocation key = BuiltInRegistries.ITEM.getKey((Object)entry.getKey());
            if (key == null) continue;
            CompoundTag itemTag = new CompoundTag();
            itemTag.putString("item", key.toString());
            itemTag.putInt("count", entry.getValue().intValue());
            list.add((Object)itemTag);
        }
        tag.put("carriedItems", (Tag)list);
    }

    public void load(CompoundTag tag) {
        this.items.clear();
        ++this.modCount;
        if (!tag.contains("carriedItems")) {
            return;
        }
        ListTag list = tag.getList("carriedItems", 10);
        for (int i = 0; i < list.size(); ++i) {
            int count;
            CompoundTag itemTag = list.getCompound(i);
            Item item = ItemHelper.resolve(itemTag.getString("item"));
            if (item == null || (count = itemTag.getInt("count")) <= 0) continue;
            this.items.put(item, count);
        }
    }
}

