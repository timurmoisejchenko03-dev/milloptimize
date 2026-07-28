/*
 * Decompiled with CFR 0.150.
 * 
 * Could not load the following classes:
 *  net.minecraft.core.registries.BuiltInRegistries
 *  net.minecraft.core.registries.Registries
 *  net.minecraft.resources.ResourceKey
 *  net.minecraft.resources.ResourceLocation
 *  net.minecraft.tags.TagKey
 *  net.minecraft.world.item.Item
 */
package org.millenaire.building;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.function.ToIntFunction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;

public final class AnywoodHelper {
    public static final ResourceLocation ANYWOOD_LOG = ResourceLocation.parse((String)"millenaire:anywood_log");
    public static final TagKey<Item> LOGS_TAG = TagKey.create((ResourceKey)Registries.ITEM, (ResourceLocation)ResourceLocation.parse((String)"minecraft:logs"));

    private AnywoodHelper() {
    }

    public static boolean isAnywood(ResourceLocation key) {
        return ANYWOOD_LOG.equals((Object)key);
    }

    public static boolean isLogResourceLocation(ResourceLocation key) {
        if (AnywoodHelper.isAnywood(key)) {
            return false;
        }
        String path = key.getPath();
        return path.endsWith("_log");
    }

    public static Set<Item> collectSpecificLogs(Map<ResourceLocation, Integer> requiredResources) {
        HashSet<Item> result = new HashSet<Item>();
        for (ResourceLocation key : requiredResources.keySet()) {
            Item item;
            if (!AnywoodHelper.isLogResourceLocation(key) || (item = (Item)BuiltInRegistries.ITEM.getOptional(key).orElse(null)) == null) continue;
            result.add(item);
        }
        return result;
    }

    public static int sumConsumableSpecificLogs(Map<ResourceLocation, Integer> requiredResources, ToIntFunction<ResourceLocation> stockOf) {
        int total = 0;
        for (Map.Entry<ResourceLocation, Integer> entry : requiredResources.entrySet()) {
            if (!AnywoodHelper.isLogResourceLocation(entry.getKey())) continue;
            total += Math.min(entry.getValue(), stockOf.applyAsInt(entry.getKey()));
        }
        return total;
    }

    public static int anywoodAvailable(int rawLogCount, Map<ResourceLocation, Integer> requiredResources, ToIntFunction<ResourceLocation> stockOf) {
        return Math.max(0, rawLogCount - AnywoodHelper.sumConsumableSpecificLogs(requiredResources, stockOf));
    }
}

