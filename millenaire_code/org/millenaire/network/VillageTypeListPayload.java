/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  io.netty.buffer.ByteBuf
 *  net.minecraft.core.BlockPos
 *  net.minecraft.network.codec.ByteBufCodecs
 *  net.minecraft.network.codec.StreamCodec
 *  net.minecraft.network.protocol.common.custom.CustomPacketPayload
 *  net.minecraft.network.protocol.common.custom.CustomPacketPayload$Type
 *  net.minecraft.resources.ResourceLocation
 */
package org.millenaire.network;

import io.netty.buffer.ByteBuf;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

public record VillageTypeListPayload(BlockPos targetPos, List<VillageTypeEntry> entries) implements CustomPacketPayload
{
    private static final int MAX_ENTRIES = 256;
    public static final CustomPacketPayload.Type<VillageTypeListPayload> TYPE = new CustomPacketPayload.Type(ResourceLocation.fromNamespaceAndPath((String)"millenaire", (String)"village_type_list"));
    public static final StreamCodec<ByteBuf, VillageTypeListPayload> STREAM_CODEC = StreamCodec.of(VillageTypeListPayload::encode, VillageTypeListPayload::decode);

    private static void encode(ByteBuf buf, VillageTypeListPayload payload) {
        ByteBufCodecs.VAR_INT.encode((Object)buf, (Object)payload.targetPos.getX());
        ByteBufCodecs.VAR_INT.encode((Object)buf, (Object)payload.targetPos.getY());
        ByteBufCodecs.VAR_INT.encode((Object)buf, (Object)payload.targetPos.getZ());
        ByteBufCodecs.VAR_INT.encode((Object)buf, (Object)payload.entries.size());
        for (VillageTypeEntry entry : payload.entries) {
            ByteBufCodecs.STRING_UTF8.encode((Object)buf, (Object)entry.cultureKey);
            ByteBufCodecs.STRING_UTF8.encode((Object)buf, (Object)entry.cultureName);
            ByteBufCodecs.STRING_UTF8.encode((Object)buf, (Object)entry.typeKey);
            ByteBufCodecs.STRING_UTF8.encode((Object)buf, (Object)entry.displayName);
            ByteBufCodecs.VAR_INT.encode((Object)buf, (Object)entry.weight);
            ByteBufCodecs.BOOL.encode((Object)buf, (Object)entry.requiresControl);
            ByteBufCodecs.BOOL.encode((Object)buf, (Object)entry.hasControl);
        }
    }

    private static VillageTypeListPayload decode(ByteBuf buf) {
        int i;
        int x = (Integer)ByteBufCodecs.VAR_INT.decode((Object)buf);
        int y = (Integer)ByteBufCodecs.VAR_INT.decode((Object)buf);
        int z = (Integer)ByteBufCodecs.VAR_INT.decode((Object)buf);
        BlockPos pos = new BlockPos(x, y, z);
        int rawCount = (Integer)ByteBufCodecs.VAR_INT.decode((Object)buf);
        int count = Math.min(rawCount, 256);
        ArrayList<VillageTypeEntry> entries = new ArrayList<VillageTypeEntry>(count);
        for (i = 0; i < count; ++i) {
            entries.add(new VillageTypeEntry((String)ByteBufCodecs.STRING_UTF8.decode((Object)buf), (String)ByteBufCodecs.STRING_UTF8.decode((Object)buf), (String)ByteBufCodecs.STRING_UTF8.decode((Object)buf), (String)ByteBufCodecs.STRING_UTF8.decode((Object)buf), (Integer)ByteBufCodecs.VAR_INT.decode((Object)buf), (Boolean)ByteBufCodecs.BOOL.decode((Object)buf), (Boolean)ByteBufCodecs.BOOL.decode((Object)buf)));
        }
        for (i = count; i < rawCount; ++i) {
            ByteBufCodecs.STRING_UTF8.decode((Object)buf);
            ByteBufCodecs.STRING_UTF8.decode((Object)buf);
            ByteBufCodecs.STRING_UTF8.decode((Object)buf);
            ByteBufCodecs.STRING_UTF8.decode((Object)buf);
            ByteBufCodecs.VAR_INT.decode((Object)buf);
            ByteBufCodecs.BOOL.decode((Object)buf);
            ByteBufCodecs.BOOL.decode((Object)buf);
        }
        return new VillageTypeListPayload(pos, entries);
    }

    public CustomPacketPayload.Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public record VillageTypeEntry(String cultureKey, String cultureName, String typeKey, String displayName, int weight, boolean requiresControl, boolean hasControl) {
    }
}

