/*
 * Decompiled with CFR 0.150.
 * 
 * Could not load the following classes:
 *  com.google.gson.Gson
 *  com.google.gson.JsonObject
 *  com.mojang.logging.LogUtils
 *  javax.annotation.Nullable
 *  net.minecraft.resources.ResourceLocation
 *  org.slf4j.Logger
 */
package org.millenaire.commerce;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.mojang.logging.LogUtils;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import javax.annotation.Nullable;
import net.minecraft.resources.ResourceLocation;
import org.millenaire.commerce.ShopProfile;
import org.millenaire.culture.JsonLoaderUtils;
import org.slf4j.Logger;

public final class ShopProfileLoader {
    private static final Logger LOGGER = LogUtils.getLogger();
    private static final Gson GSON = JsonLoaderUtils.GSON;
    private static final Map<ResourceLocation, Map<String, ShopProfile>> PROFILES = new ConcurrentHashMap<ResourceLocation, Map<String, ShopProfile>>();

    private ShopProfileLoader() {
    }

    public static void load(ResourceLocation cultureId, String shopId, InputStream stream) {
        try (InputStreamReader reader = new InputStreamReader(stream, StandardCharsets.UTF_8);){
            JsonObject root = (JsonObject)GSON.fromJson((Reader)reader, JsonObject.class);
            List<String> sells = JsonLoaderUtils.parseStringList(root, "sells");
            List<String> buys = JsonLoaderUtils.parseStringList(root, "buys");
            List<String> buysOptional = JsonLoaderUtils.parseStringList(root, "buys_optional");
            List<String> deliverTo = JsonLoaderUtils.parseStringList(root, "deliver_to");
            ShopProfile profile = new ShopProfile(Collections.unmodifiableList(sells), Collections.unmodifiableList(buys), Collections.unmodifiableList(buysOptional), Collections.unmodifiableList(deliverTo));
            PROFILES.computeIfAbsent(cultureId, k -> new ConcurrentHashMap()).put(shopId, profile);
            LOGGER.debug("[Millenaire] Shop profile '{}' loaded for culture {} (sells={}, buys={}, optional={})", new Object[]{shopId, cultureId, sells.size(), buys.size(), buysOptional.size()});
        }
        catch (Exception e) {
            LOGGER.error("[Millenaire] Error loading shop profile '{}' for {}", new Object[]{shopId, cultureId, e});
        }
    }

    @Nullable
    public static ShopProfile getProfile(ResourceLocation cultureId, String shopId) {
        Map<String, ShopProfile> cultureProfiles = PROFILES.get((Object)cultureId);
        if (cultureProfiles == null) {
            return null;
        }
        return cultureProfiles.get(shopId);
    }

    @Nullable
    public static Map<String, ShopProfile> getProfiles(ResourceLocation cultureId) {
        return PROFILES.get((Object)cultureId);
    }

    public static void clear() {
        PROFILES.clear();
    }
}

