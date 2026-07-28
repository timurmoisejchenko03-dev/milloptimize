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

public record BuildingProjectListPayload(String villageUuid, String villageName, BlockPos clickedPos, List<BuildingEntry> entries) implements CustomPacketPayload
{
    private static final int MAX_ENTRIES = 64;
    public static final CustomPacketPayload.Type<BuildingProjectListPayload> TYPE = new CustomPacketPayload.Type(ResourceLocation.fromNamespaceAndPath((String)"millenaire", (String)"building_project_list"));
    public static final StreamCodec<ByteBuf, BuildingProjectListPayload> STREAM_CODEC = StreamCodec.of(BuildingProjectListPayload::encode, BuildingProjectListPayload::decode);

    private static void encode(ByteBuf buf, BuildingProjectListPayload payload) {
        ByteBufCodecs.STRING_UTF8.encode((Object)buf, (Object)payload.villageUuid);
        ByteBufCodecs.STRING_UTF8.encode((Object)buf, (Object)payload.villageName);
        ByteBufCodecs.VAR_INT.encode((Object)buf, (Object)payload.clickedPos.getX());
        ByteBufCodecs.VAR_INT.encode((Object)buf, (Object)payload.clickedPos.getY());
        ByteBufCodecs.VAR_INT.encode((Object)buf, (Object)payload.clickedPos.getZ());
        int count = Math.min(payload.entries.size(), 64);
        ByteBufCodecs.VAR_INT.encode((Object)buf, (Object)count);
        for (int i = 0; i < count; ++i) {
            BuildingEntry entry = payload.entries.get(i);
            ByteBufCodecs.STRING_UTF8.encode((Object)buf, (Object)entry.planSetId());
            ByteBufCodecs.STRING_UTF8.encode((Object)buf, (Object)entry.displayName());
        }
    }

    private static BuildingProjectListPayload decode(ByteBuf buf) {
        String villageUuid = (String)ByteBufCodecs.STRING_UTF8.decode((Object)buf);
        String villageName = (String)ByteBufCodecs.STRING_UTF8.decode((Object)buf);
        int px = (Integer)ByteBufCodecs.VAR_INT.decode((Object)buf);
        int py = (Integer)ByteBufCodecs.VAR_INT.decode((Object)buf);
        int pz = (Integer)ByteBufCodecs.VAR_INT.decode((Object)buf);
        BlockPos clickedPos = new BlockPos(px, py, pz);
        int count = Math.min((Integer)ByteBufCodecs.VAR_INT.decode((Object)buf), 64);
        ArrayList<BuildingEntry> entries = new ArrayList<BuildingEntry>(count);
        for (int i = 0; i < count; ++i) {
            entries.add(new BuildingEntry((String)ByteBufCodecs.STRING_UTF8.decode((Object)buf), (String)ByteBufCodecs.STRING_UTF8.decode((Object)buf)));
        }
        return new BuildingProjectListPayload(villageUuid, villageName, clickedPos, entries);
    }

    public CustomPacketPayload.Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public record BuildingEntry(String planSetId, String displayName) {
    }
}

