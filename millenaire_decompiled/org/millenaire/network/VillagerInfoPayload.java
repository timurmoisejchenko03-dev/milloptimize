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
import java.util.List;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

public record VillagerInfoPayload(int entityId, String displayName, String roleName, String nativeOccupation, String villageName, String goalLabel, float health, float maxHealth, int reputation, String reputationLabel, String cultureName, int languageScore, List<InvEntry> inventory, List<String> possibleGoals, String cultureKey, String villagerTypeKey, boolean travelBookVisible) {
    public static final CustomPacketPayload.Type<VillagerInfoPayload> TYPE = new CustomPacketPayload.Type(ResourceLocation.fromNamespaceAndPath((String)"millenaire", (String)"villager_info"));
    public static final StreamCodec<ByteBuf, VillagerInfoPayload> STREAM_CODEC = StreamCodec.of(VillagerInfoPayload::encode, VillagerInfoPayload::decode);

    private static void encode(ByteBuf buf, VillagerInfoPayload payload) {
        ByteBufCodecs.VAR_INT.encode((Object)buf, (Object)payload.entityId);
        ByteBufCodecs.STRING_UTF8.encode((Object)buf, (Object)payload.displayName);
        ByteBufCodecs.STRING_UTF8.encode((Object)buf, (Object)payload.roleName);
        ByteBufCodecs.STRING_UTF8.encode((Object)buf, (Object)payload.nativeOccupation);
        ByteBufCodecs.STRING_UTF8.encode((Object)buf, (Object)payload.villageName);
        ByteBufCodecs.STRING_UTF8.encode((Object)buf, (Object)payload.goalLabel);
        ByteBufCodecs.FLOAT.encode((Object)buf, (Object)Float.valueOf(payload.health));
        ByteBufCodecs.FLOAT.encode((Object)buf, (Object)Float.valueOf(payload.maxHealth));
        ByteBufCodecs.VAR_INT.encode((Object)buf, (Object)payload.reputation);
        ByteBufCodecs.STRING_UTF8.encode((Object)buf, (Object)payload.reputationLabel);
        ByteBufCodecs.STRING_UTF8.encode((Object)buf, (Object)payload.cultureName);
        ByteBufCodecs.VAR_INT.encode((Object)buf, (Object)payload.languageScore);
        ByteBufCodecs.VAR_INT.encode((Object)buf, (Object)payload.inventory.size());
        for (InvEntry entry : payload.inventory) {
            ByteBufCodecs.STRING_UTF8.encode((Object)buf, (Object)entry.itemId);
            ByteBufCodecs.VAR_INT.encode((Object)buf, (Object)entry.count);
        }
        ByteBufCodecs.VAR_INT.encode((Object)buf, (Object)payload.possibleGoals.size());
        for (String goalKey : payload.possibleGoals) {
            ByteBufCodecs.STRING_UTF8.encode((Object)buf, (Object)goalKey);
        }
        ByteBufCodecs.STRING_UTF8.encode((Object)buf, (Object)payload.cultureKey);
        ByteBufCodecs.STRING_UTF8.encode((Object)buf, (Object)payload.villagerTypeKey);
        buf.writeBoolean(payload.travelBookVisible);
    }

    private static VillagerInfoPayload decode(ByteBuf buf) {
        int entityId = (Integer)ByteBufCodecs.VAR_INT.decode((Object)buf);
        String displayName = (String)ByteBufCodecs.STRING_UTF8.decode((Object)buf);
        String roleName = (String)ByteBufCodecs.STRING_UTF8.decode((Object)buf);
        String nativeOccupation = (String)ByteBufCodecs.STRING_UTF8.decode((Object)buf);
        String villageName = (String)ByteBufCodecs.STRING_UTF8.decode((Object)buf);
        String goalLabel = (String)ByteBufCodecs.STRING_UTF8.decode((Object)buf);
        float health = ((Float)ByteBufCodecs.FLOAT.decode((Object)buf)).floatValue();
        float maxHealth = ((Float)ByteBufCodecs.FLOAT.decode((Object)buf)).floatValue();
        int reputation = (Integer)ByteBufCodecs.VAR_INT.decode((Object)buf);
        String reputationLabel = (String)ByteBufCodecs.STRING_UTF8.decode((Object)buf);
        String cultureName = (String)ByteBufCodecs.STRING_UTF8.decode((Object)buf);
        int languageScore = (Integer)ByteBufCodecs.VAR_INT.decode((Object)buf);
        int invSize = (Integer)ByteBufCodecs.VAR_INT.decode((Object)buf);
        int maxInvSize = Math.min(invSize, 256);
        ArrayList<InvEntry> inventory = new ArrayList<InvEntry>(maxInvSize);
        for (int i = 0; i < invSize; ++i) {
            String itemId = (String)ByteBufCodecs.STRING_UTF8.decode((Object)buf);
            int count = (Integer)ByteBufCodecs.VAR_INT.decode((Object)buf);
            if (i >= maxInvSize) continue;
            inventory.add(new InvEntry(itemId, count));
        }
        int goalsSize = (Integer)ByteBufCodecs.VAR_INT.decode((Object)buf);
        int maxGoalsSize = Math.min(goalsSize, 128);
        ArrayList<String> possibleGoals = new ArrayList<String>(maxGoalsSize);
        for (int i = 0; i < goalsSize; ++i) {
            String goalKey = (String)ByteBufCodecs.STRING_UTF8.decode((Object)buf);
            if (i >= maxGoalsSize) continue;
            possibleGoals.add(goalKey);
        }
        String cultureKey = (String)ByteBufCodecs.STRING_UTF8.decode((Object)buf);
        String villagerTypeKey = (String)ByteBufCodecs.STRING_UTF8.decode((Object)buf);
        boolean travelBookVisible = buf.readBoolean();
        return new VillagerInfoPayload(entityId, displayName, roleName, nativeOccupation, villageName, goalLabel, health, maxHealth, reputation, reputationLabel, cultureName, languageScore, inventory, possibleGoals, cultureKey, villagerTypeKey, travelBookVisible);
    }

    public CustomPacketPayload.Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public record InvEntry(String itemId, int count) {
    }
}

