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
import java.util.Collections;
import java.util.List;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

public record FireplacePositionsPayload(UUID villageId, BlockPos villageCenter, List<BlockPos> positions) implements CustomPacketPayload
{
    private static final int MAX_POSITIONS = 512;
    public static final CustomPacketPayload.Type<FireplacePositionsPayload> TYPE = new CustomPacketPayload.Type(ResourceLocation.fromNamespaceAndPath((String)"millenaire", (String)"fireplace_positions"));
    public static final StreamCodec<ByteBuf, FireplacePositionsPayload> STREAM_CODEC = StreamCodec.of(FireplacePositionsPayload::encode, FireplacePositionsPayload::decode);

    private static void encode(ByteBuf buf, FireplacePositionsPayload payload) {
        buf.writeLong(payload.villageId.getMostSignificantBits());
        buf.writeLong(payload.villageId.getLeastSignificantBits());
        ByteBufCodecs.VAR_INT.encode((Object)buf, (Object)payload.villageCenter.getX());
        ByteBufCodecs.VAR_INT.encode((Object)buf, (Object)payload.villageCenter.getY());
        ByteBufCodecs.VAR_INT.encode((Object)buf, (Object)payload.villageCenter.getZ());
        int toWrite = Math.min(payload.positions.size(), 512);
        ByteBufCodecs.VAR_INT.encode((Object)buf, (Object)toWrite);
        for (int i = 0; i < toWrite; ++i) {
            BlockPos pos = payload.positions.get(i);
            ByteBufCodecs.VAR_INT.encode((Object)buf, (Object)pos.getX());
            ByteBufCodecs.VAR_INT.encode((Object)buf, (Object)pos.getY());
            ByteBufCodecs.VAR_INT.encode((Object)buf, (Object)pos.getZ());
        }
    }

    private static FireplacePositionsPayload decode(ByteBuf buf) {
        UUID villageId = new UUID(buf.readLong(), buf.readLong());
        int cx = (Integer)ByteBufCodecs.VAR_INT.decode((Object)buf);
        int cy = (Integer)ByteBufCodecs.VAR_INT.decode((Object)buf);
        int cz = (Integer)ByteBufCodecs.VAR_INT.decode((Object)buf);
        BlockPos center = new BlockPos(cx, cy, cz);
        int rawCount = (Integer)ByteBufCodecs.VAR_INT.decode((Object)buf);
        int kept = Math.min(rawCount, 512);
        ArrayList<BlockPos> positions = new ArrayList<BlockPos>(kept);
        for (int i = 0; i < rawCount; ++i) {
            int x = (Integer)ByteBufCodecs.VAR_INT.decode((Object)buf);
            int y = (Integer)ByteBufCodecs.VAR_INT.decode((Object)buf);
            int z = (Integer)ByteBufCodecs.VAR_INT.decode((Object)buf);
            if (i >= kept) continue;
            positions.add(new BlockPos(x, y, z));
        }
        return new FireplacePositionsPayload(villageId, center, Collections.unmodifiableList(positions));
    }

    public CustomPacketPayload.Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}

