/*
 * Decompiled with CFR 0.150.
 * 
 * Could not load the following classes:
 *  net.minecraft.resources.ResourceLocation
 *  net.neoforged.api.distmarker.Dist
 *  net.neoforged.api.distmarker.OnlyIn
 */
package org.millenaire.client;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

@OnlyIn(value=Dist.CLIENT)
public final class ClientLanguageCache {
    private static final Map<ResourceLocation, Integer> scores = new ConcurrentHashMap<ResourceLocation, Integer>();

    private ClientLanguageCache() {
    }

    public static void update(ResourceLocation cultureId, int score) {
        scores.put(cultureId, score);
    }

    public static int get(ResourceLocation cultureId) {
        return scores.getOrDefault((Object)cultureId, 0);
    }

    public static boolean canReadBuildingNames(ResourceLocation cultureId) {
        return ClientLanguageCache.get(cultureId) >= 100;
    }

    public static boolean canReadVillagerNames(ResourceLocation cultureId) {
        return ClientLanguageCache.get(cultureId) >= 200;
    }

    public static void clear() {
        scores.clear();
    }
}

