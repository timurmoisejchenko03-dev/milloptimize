/*
 * Decompiled with CFR 0.152.
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
import java.util.List;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

public record ControlledProjectsPayload(String villageUuid, String villageName, String pendingPlanName, List<ProjectEntry> entries) implements CustomPacketPayload
{
    private static final int MAX_ENTRIES = 128;
    public static final CustomPacketPayload.Type<ControlledProjectsPayload> TYPE = new CustomPacketPayload.Type(ResourceLocation.fromNamespaceAndPath((String)"millenaire", (String)"controlled_projects"));
    public static final StreamCodec<ByteBuf, ControlledProjectsPayload> STREAM_CODEC = StreamCodec.of(ControlledProjectsPayload::encode, ControlledProjectsPayload::decode);

    private static void encode(ByteBuf buf, ControlledProjectsPayload payload) {
        ByteBufCodecs.STRING_UTF8.encode((Object)buf, (Object)payload.villageUuid);
        ByteBufCodecs.STRING_UTF8.encode((Object)buf, (Object)payload.villageName);
        ByteBufCodecs.STRING_UTF8.encode((Object)buf, (Object)payload.pendingPlanName);
        int count = Math.min(payload.entries.size(), 128);
        ByteBufCodecs.VAR_INT.encode((Object)buf, (Object)count);
        for (int i = 0; i < count; ++i) {
            ProjectEntry e = payload.entries.get(i);
            ByteBufCodecs.STRING_UTF8.encode((Object)buf, (Object)e.buildingId());
            ByteBufCodecs.STRING_UTF8.encode((Object)buf, (Object)e.displayName());
            ByteBufCodecs.STRING_UTF8.encode((Object)buf, (Object)e.planSetId());
            ByteBufCodecs.VAR_INT.encode((Object)buf, (Object)e.currentLevel());
            ByteBufCodecs.VAR_INT.encode((Object)buf, (Object)e.maxLevel());
            buf.writeBoolean(e.upgradesAllowed());
            ByteBufCodecs.STRING_UTF8.encode((Object)buf, (Object)e.distanceLabel());
            buf.writeBoolean(e.isTownHall());
        }
    }

    private static ControlledProjectsPayload decode(ByteBuf buf) {
        String villageUuid = (String)ByteBufCodecs.STRING_UTF8.decode((Object)buf);
        String villageName = (String)ByteBufCodecs.STRING_UTF8.decode((Object)buf);
        String pendingPlanName = (String)ByteBufCodecs.STRING_UTF8.decode((Object)buf);
        int count = Math.min((Integer)ByteBufCodecs.VAR_INT.decode((Object)buf), 128);
        ArrayList<ProjectEntry> entries = new ArrayList<ProjectEntry>(count);
        for (int i = 0; i < count; ++i) {
            entries.add(new ProjectEntry((String)ByteBufCodecs.STRING_UTF8.decode((Object)buf), (String)ByteBufCodecs.STRING_UTF8.decode((Object)buf), (String)ByteBufCodecs.STRING_UTF8.decode((Object)buf), (Integer)ByteBufCodecs.VAR_INT.decode((Object)buf), (Integer)ByteBufCodecs.VAR_INT.decode((Object)buf), buf.readBoolean(), (String)ByteBufCodecs.STRING_UTF8.decode((Object)buf), buf.readBoolean()));
        }
        return new ControlledProjectsPayload(villageUuid, villageName, pendingPlanName, entries);
    }

    public CustomPacketPayload.Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public record ProjectEntry(String buildingId, String displayName, String planSetId, int currentLevel, int maxLevel, boolean upgradesAllowed, String distanceLabel, boolean isTownHall) {
    }
}

