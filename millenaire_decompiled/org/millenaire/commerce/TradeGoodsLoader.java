/*
 * Decompiled with CFR 0.150.
 * 
 * Could not load the following classes:
 *  com.google.gson.Gson
 *  com.google.gson.JsonArray
 *  com.google.gson.JsonElement
 *  com.google.gson.JsonObject
 *  com.mojang.logging.LogUtils
 *  javax.annotation.Nullable
 *  net.minecraft.resources.ResourceLocation
 *  net.minecraft.util.GsonHelper
 *  net.minecraft.world.item.Item
 *  org.slf4j.Logger
 */
package org.millenaire.commerce;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.mojang.logging.LogUtils;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import javax.annotation.Nullable;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.GsonHelper;
import net.minecraft.world.item.Item;
import org.millenaire.commerce.TradeGood;
import org.millenaire.content.Resource;
import org.millenaire.culture.JsonLoaderUtils;
import org.millenaire.item.ItemHelper;
import org.slf4j.Logger;

public final class TradeGoodsLoader {
    private static final Logger LOGGER = LogUtils.getLogger();
    private static final Gson GSON = JsonLoaderUtils.GSON;
    private static final Map<ResourceLocation, List<TradeGood>> GOODS = new ConcurrentHashMap<ResourceLocation, List<TradeGood>>();
    private static final Map<ResourceLocation, Map<Item, Integer>> TARGET_QTY_CACHE = new ConcurrentHashMap<ResourceLocation, Map<Item, Integer>>();
    private static final Set<String> WARNED_REPLACE_IDS = ConcurrentHashMap.newKeySet();

    private TradeGoodsLoader() {
    }

    public static void loadFromResources(ResourceLocation cultureId, List<Resource> resources) {
        LinkedHashMap<String, TradeGood> byId = new LinkedHashMap<String, TradeGood>();
        int total = 0;
        int removed = 0;
        int duplicatesIgnored = 0;
        if (resources == null || resources.isEmpty()) {
            GOODS.put(cultureId, Collections.unmodifiableList(new ArrayList(byId.values())));
            TARGET_QTY_CACHE.remove((Object)cultureId);
            LOGGER.info("[Millenaire] Loaded 0 goods for culture {}", (Object)cultureId);
            return;
        }
        for (int i = resources.size() - 1; i >= 0; --i) {
            Resource res = resources.get(i);
            String layerLabel = res.source().displayName();
            try (InputStream stream = res.open();){
                List<Entry> entries = TradeGoodsLoader.parseEntries(cultureId, stream, layerLabel);
                if (entries == null) continue;
                for (Entry e : entries) {
                    String id = e.good().id();
                    if (e.disabled()) {
                        if (byId.remove(id) == null) continue;
                        ++removed;
                        continue;
                    }
                    if (byId.containsKey(id)) {
                        ++duplicatesIgnored;
                        String warnKey = "traded_goods:" + String.valueOf((Object)cultureId) + ":" + id;
                        if (!WARNED_REPLACE_IDS.add(warnKey)) continue;
                        LOGGER.debug("Traded good '{}' already declared for culture {}; ignoring duplicate from '{}' (use \"disabled\":true to override)", new Object[]{id, cultureId, layerLabel});
                        continue;
                    }
                    byId.put(id, e.good());
                    ++total;
                }
                continue;
            }
            catch (IOException e) {
                LOGGER.error("[Millenaire] Error reading traded_goods {} for {}: {}", new Object[]{res.relPath(), cultureId, e.getMessage(), e});
            }
        }
        GOODS.put(cultureId, Collections.unmodifiableList(new ArrayList(byId.values())));
        TARGET_QTY_CACHE.remove((Object)cultureId);
        if (resources.size() > 1 || duplicatesIgnored > 0 || removed > 0) {
            LOGGER.info("[Millenaire] Loaded {} goods for culture {} (sources={}, removed={}, duplicates ignored={})", new Object[]{byId.size(), cultureId, resources.size(), removed, duplicatesIgnored});
        } else {
            LOGGER.info("[Millenaire] Loaded {} goods for culture {}", (Object)byId.size(), (Object)cultureId);
        }
    }

    /*
     * Enabled aggressive block sorting
     * Enabled unnecessary exception pruning
     * Enabled aggressive exception aggregation
     */
    @Nullable
    private static List<Entry> parseEntries(ResourceLocation cultureId, InputStream stream, String layer) {
        try (InputStreamReader reader = new InputStreamReader(stream, StandardCharsets.UTF_8);){
            JsonObject root = (JsonObject)GSON.fromJson((Reader)reader, JsonObject.class);
            JsonArray goodsArray = root.getAsJsonArray("goods");
            if (goodsArray == null) {
                LOGGER.warn("[Millenaire] traded_goods for {} ({} layer): no 'goods' array", (Object)cultureId, (Object)layer);
                List<Entry> list = null;
                return list;
            }
            ArrayList<Entry> out = new ArrayList<Entry>();
            for (JsonElement element : goodsArray) {
                JsonObject obj = element.getAsJsonObject();
                String id = GsonHelper.getAsString((JsonObject)obj, (String)"id");
                boolean disabled = GsonHelper.getAsBoolean((JsonObject)obj, (String)"disabled", (boolean)false);
                if (disabled) {
                    out.add(new Entry(new TradeGood(id, "", 0, 0, 0, 0, false, 0, "misc", true, 0), true));
                    continue;
                }
                String item = GsonHelper.getAsString((JsonObject)obj, (String)"item");
                int sellingPrice = GsonHelper.getAsInt((JsonObject)obj, (String)"selling_price", (int)0);
                int buyingPrice = GsonHelper.getAsInt((JsonObject)obj, (String)"buying_price", (int)0);
                int reservedQuantity = GsonHelper.getAsInt((JsonObject)obj, (String)"reserved_quantity", (int)0);
                int targetQuantity = GsonHelper.getAsInt((JsonObject)obj, (String)"target_quantity", (int)0);
                boolean autoGenerate = GsonHelper.getAsBoolean((JsonObject)obj, (String)"auto_generate", (boolean)false);
                int minReputation = GsonHelper.getAsInt((JsonObject)obj, (String)"min_reputation", (int)0);
                String category = GsonHelper.getAsString((JsonObject)obj, (String)"category", (String)"misc");
                boolean travelBookDisplay = GsonHelper.getAsBoolean((JsonObject)obj, (String)"travel_book_display", (boolean)true);
                int foreignMerchantPrice = GsonHelper.getAsInt((JsonObject)obj, (String)"foreign_merchant_price", (int)0);
                out.add(new Entry(new TradeGood(id, item, sellingPrice, buyingPrice, reservedQuantity, targetQuantity, autoGenerate, minReputation, category, travelBookDisplay, foreignMerchantPrice), false));
            }
            ArrayList<Entry> arrayList = out;
            return arrayList;
        }
        catch (Exception e) {
            LOGGER.error("[Millenaire] Error parsing traded_goods ({} layer) for {}", new Object[]{layer, cultureId, e});
            return null;
        }
    }

    public static List<TradeGood> getGoods(ResourceLocation cultureId) {
        return GOODS.getOrDefault((Object)cultureId, Collections.emptyList());
    }

    @Nullable
    public static TradeGood getGoodById(ResourceLocation cultureId, String goodId) {
        for (TradeGood good : TradeGoodsLoader.getGoods(cultureId)) {
            if (!good.id().equals(goodId)) continue;
            return good;
        }
        return null;
    }

    public static Map<Item, Integer> getTargetQuantities(ResourceLocation cultureId) {
        return TARGET_QTY_CACHE.computeIfAbsent(cultureId, id -> {
            List<TradeGood> goods = TradeGoodsLoader.getGoods(id);
            HashMap<Item, Integer> map = new HashMap<Item, Integer>();
            for (TradeGood good : goods) {
                Item item;
                if (good.isTag() || good.targetQuantity() <= 0 || (item = ItemHelper.resolve(good.item())) == null) continue;
                map.merge(item, good.targetQuantity(), Integer::sum);
            }
            return Collections.unmodifiableMap(map);
        });
    }

    public static void clear() {
        GOODS.clear();
        TARGET_QTY_CACHE.clear();
        WARNED_REPLACE_IDS.clear();
    }

    private record Entry(TradeGood good, boolean disabled) {
    }
}

