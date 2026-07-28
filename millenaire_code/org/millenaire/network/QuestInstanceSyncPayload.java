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
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

public record QuestInstanceSyncPayload(long uniqueId, String questKey, int currentStep, long startTime, long currentStepStart, Map<String, VillagerData> villagers, String displayLabel, String currentStepVillagerId) implements CustomPacketPayload
{
    public static final CustomPacketPayload.Type<QuestInstanceSyncPayload> TYPE = new CustomPacketPayload.Type(ResourceLocation.fromNamespaceAndPath((String)"millenaire", (String)"quest_sync"));
    public static final StreamCodec<ByteBuf, QuestInstanceSyncPayload> STREAM_CODEC = StreamCodec.of(QuestInstanceSyncPayload::encode, QuestInstanceSyncPayload::decode);

    private static void encode(ByteBuf buf, QuestInstanceSyncPayload payload) {
        ByteBufCodecs.VAR_LONG.encode((Object)buf, (Object)payload.uniqueId);
        ByteBufCodecs.STRING_UTF8.encode((Object)buf, (Object)payload.questKey);
        ByteBufCodecs.VAR_INT.encode((Object)buf, (Object)payload.currentStep);
        ByteBufCodecs.VAR_LONG.encode((Object)buf, (Object)payload.startTime);
        ByteBufCodecs.VAR_LONG.encode((Object)buf, (Object)payload.currentStepStart);
        ByteBufCodecs.VAR_INT.encode((Object)buf, (Object)payload.villagers.size());
        for (Map.Entry<String, VillagerData> entry : payload.villagers.entrySet()) {
            ByteBufCodecs.STRING_UTF8.encode((Object)buf, (Object)entry.getKey());
            ByteBufCodecs.STRING_UTF8.encode((Object)buf, (Object)entry.getValue().villagerId.toString());
            ByteBufCodecs.STRING_UTF8.encode((Object)buf, (Object)entry.getValue().villageId.toString());
        }
        ByteBufCodecs.STRING_UTF8.encode((Object)buf, (Object)payload.displayLabel);
        ByteBufCodecs.STRING_UTF8.encode((Object)buf, (Object)payload.currentStepVillagerId);
    }

    private static QuestInstanceSyncPayload decode(ByteBuf buf) {
        long uniqueId = (Long)ByteBufCodecs.VAR_LONG.decode((Object)buf);
        String questKey = (String)ByteBufCodecs.STRING_UTF8.decode((Object)buf);
        int currentStep = (Integer)ByteBufCodecs.VAR_INT.decode((Object)buf);
        long startTime = (Long)ByteBufCodecs.VAR_LONG.decode((Object)buf);
        long currentStepStart = (Long)ByteBufCodecs.VAR_LONG.decode((Object)buf);
        int villagerCount = (Integer)ByteBufCodecs.VAR_INT.decode((Object)buf);
        HashMap<String, VillagerData> villagers = new HashMap<String, VillagerData>();
        for (int i = 0; i < villagerCount; ++i) {
            String key = (String)ByteBufCodecs.STRING_UTF8.decode((Object)buf);
            UUID villagerId = UUID.fromString((String)ByteBufCodecs.STRING_UTF8.decode((Object)buf));
            UUID villageId = UUID.fromString((String)ByteBufCodecs.STRING_UTF8.decode((Object)buf));
            villagers.put(key, new VillagerData(villagerId, villageId));
        }
        String displayLabel = (String)ByteBufCodecs.STRING_UTF8.decode((Object)buf);
        String currentStepVillagerId = (String)ByteBufCodecs.STRING_UTF8.decode((Object)buf);
        return new QuestInstanceSyncPayload(uniqueId, questKey, currentStep, startTime, currentStepStart, villagers, displayLabel, currentStepVillagerId);
    }

    public CustomPacketPayload.Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public record VillagerData(UUID villagerId, UUID villageId) {
    }
}

