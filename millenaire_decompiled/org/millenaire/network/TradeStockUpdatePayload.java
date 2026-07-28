/*
 * Decompiled with CFR 0.150.
 * 
 * Could not load the following classes:
 *  io.netty.buffer.ByteBuf
 *  net.minecraft.network.codec.ByteBufCodecs
 *  net.minecraft.network.codec.StreamCodec
 *  net.minecraft.network.protocol.common.custom.CustomPacketPayload
 *  net.minecraft.network.protocol.common.custom.CustomPacketPayload$Type
 *  net.minecraft.resources.ResourceLocation
 */
package org.millenaire.network;

import io.netty.buffer.ByteBuf;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

public record TradeStockUpdatePayload(int containerId, List<Integer> stocks, boolean donationMode) {
    public static final CustomPacketPayload.Type<TradeStockUpdatePayload> TYPE = new CustomPacketPayload.Type(ResourceLocation.fromNamespaceAndPath((String)"millenaire", (String)"trade_stock_update"));
    public static final StreamCodec<ByteBuf, TradeStockUpdatePayload> STREAM_CODEC = StreamCodec.of(TradeStockUpdatePayload::encode, TradeStockUpdatePayload::decode);

    private static void encode(ByteBuf buf, TradeStockUpdatePayload payload) {
        ByteBufCodecs.VAR_INT.encode((Object)buf, (Object)payload.containerId);
        ByteBufCodecs.VAR_INT.encode((Object)buf, (Object)payload.stocks.size());
        for (int stock : payload.stocks) {
            ByteBufCodecs.VAR_INT.encode((Object)buf, (Object)stock);
        }
        ByteBufCodecs.BOOL.encode((Object)buf, (Object)payload.donationMode);
    }

    private static TradeStockUpdatePayload decode(ByteBuf buf) {
        int containerId = (Integer)ByteBufCodecs.VAR_INT.decode((Object)buf);
        int count = (Integer)ByteBufCodecs.VAR_INT.decode((Object)buf);
        int maxCount = Math.min(count, 512);
        ArrayList<Integer> stocks = new ArrayList<Integer>(maxCount);
        for (int i = 0; i < count; ++i) {
            int stock = (Integer)ByteBufCodecs.VAR_INT.decode((Object)buf);
            if (i >= maxCount) continue;
            stocks.add(stock);
        }
        boolean donationMode = (Boolean)ByteBufCodecs.BOOL.decode((Object)buf);
        return new TradeStockUpdatePayload(containerId, Collections.unmodifiableList(stocks), donationMode);
    }

    public CustomPacketPayload.Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}

