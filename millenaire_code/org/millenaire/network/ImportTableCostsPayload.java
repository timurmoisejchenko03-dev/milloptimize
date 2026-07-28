/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  io.netty.buffer.ByteBuf
 *  io.netty.handler.codec.DecoderException
 *  net.minecraft.core.BlockPos
 *  net.minecraft.network.codec.ByteBufCodecs
 *  net.minecraft.network.codec.StreamCodec
 *  net.minecraft.network.protocol.common.custom.CustomPacketPayload
 *  net.minecraft.network.protocol.common.custom.CustomPacketPayload$Type
 *  net.minecraft.resources.ResourceLocation
 */
package org.millenaire.network;

import io.netty.buffer.ByteBuf;
import io.netty.handler.codec.DecoderException;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

public record ImportTableCostsPayload(BlockPos blockPos, String buildingId, String variant, int level, List<Entry> costs) implements CustomPacketPayload
{
    public static final CustomPacketPayload.Type<ImportTableCostsPayload> TYPE = new CustomPacketPayload.Type(ResourceLocation.fromNamespaceAndPath((String)"millenaire", (String)"import_table_costs"));
    public static final StreamCodec<ByteBuf, ImportTableCostsPayload> STREAM_CODEC = StreamCodec.of(ImportTableCostsPayload::encode, ImportTableCostsPayload::decode);
    private static final int MAX_COST_ENTRIES = 4096;

    private static void encode(ByteBuf buf, ImportTableCostsPayload p) {
        ByteBufCodecs.VAR_INT.encode((Object)buf, (Object)p.blockPos.getX());
        ByteBufCodecs.VAR_INT.encode((Object)buf, (Object)p.blockPos.getY());
        ByteBufCodecs.VAR_INT.encode((Object)buf, (Object)p.blockPos.getZ());
        ByteBufCodecs.STRING_UTF8.encode((Object)buf, (Object)p.buildingId);
        ByteBufCodecs.STRING_UTF8.encode((Object)buf, (Object)p.variant);
        ByteBufCodecs.VAR_INT.encode((Object)buf, (Object)p.level);
        ByteBufCodecs.VAR_INT.encode((Object)buf, (Object)p.costs.size());
        for (Entry e : p.costs) {
            ByteBufCodecs.STRING_UTF8.encode((Object)buf, (Object)e.itemId());
            ByteBufCodecs.VAR_INT.encode((Object)buf, (Object)e.quantity());
        }
    }

    private static ImportTableCostsPayload decode(ByteBuf buf) {
        int x = (Integer)ByteBufCodecs.VAR_INT.decode((Object)buf);
        int y = (Integer)ByteBufCodecs.VAR_INT.decode((Object)buf);
        int z = (Integer)ByteBufCodecs.VAR_INT.decode((Object)buf);
        String buildingId = (String)ByteBufCodecs.STRING_UTF8.decode((Object)buf);
        String variant = (String)ByteBufCodecs.STRING_UTF8.decode((Object)buf);
        int level = (Integer)ByteBufCodecs.VAR_INT.decode((Object)buf);
        int n = (Integer)ByteBufCodecs.VAR_INT.decode((Object)buf);
        if (n < 0 || n > 4096) {
            throw new DecoderException("ImportTableCostsPayload: cost-list size " + n + " out of bounds (max 4096)");
        }
        ArrayList<Entry> costs = new ArrayList<Entry>(n);
        for (int i = 0; i < n; ++i) {
            String itemId = (String)ByteBufCodecs.STRING_UTF8.decode((Object)buf);
            int qty = (Integer)ByteBufCodecs.VAR_INT.decode((Object)buf);
            costs.add(new Entry(itemId, qty));
        }
        return new ImportTableCostsPayload(new BlockPos(x, y, z), buildingId, variant, level, costs);
    }

    public CustomPacketPayload.Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public record Entry(String itemId, int quantity) {
    }
}

