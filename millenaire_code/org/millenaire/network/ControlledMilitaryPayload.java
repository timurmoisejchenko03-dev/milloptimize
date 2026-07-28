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
import org.millenaire.village.panel.PanelLine;

public record ControlledMilitaryPayload(String villageUuid, String villageName, String ownerName, String currentRaidTargetVillageId, boolean raidInProgress, List<RelationEntry> relations, List<PanelLine> militaryLines) implements CustomPacketPayload
{
    private static final int MAX_RELATION_ENTRIES = 64;
    private static final int MAX_MILITARY_LINES = 512;
    private static final int MAX_MILITARY_LINE_ARGS = 8;
    public static final CustomPacketPayload.Type<ControlledMilitaryPayload> TYPE = new CustomPacketPayload.Type(ResourceLocation.fromNamespaceAndPath((String)"millenaire", (String)"controlled_military"));
    public static final StreamCodec<ByteBuf, ControlledMilitaryPayload> STREAM_CODEC = StreamCodec.of(ControlledMilitaryPayload::encode, ControlledMilitaryPayload::decode);

    private static void encode(ByteBuf buf, ControlledMilitaryPayload payload) {
        ByteBufCodecs.STRING_UTF8.encode((Object)buf, (Object)payload.villageUuid);
        ByteBufCodecs.STRING_UTF8.encode((Object)buf, (Object)payload.villageName);
        ByteBufCodecs.STRING_UTF8.encode((Object)buf, (Object)payload.ownerName);
        ByteBufCodecs.STRING_UTF8.encode((Object)buf, (Object)payload.currentRaidTargetVillageId);
        buf.writeBoolean(payload.raidInProgress);
        int relCount = Math.min(payload.relations.size(), 64);
        ByteBufCodecs.VAR_INT.encode((Object)buf, (Object)relCount);
        for (int i = 0; i < relCount; ++i) {
            RelationEntry re = payload.relations.get(i);
            ByteBufCodecs.STRING_UTF8.encode((Object)buf, (Object)re.villageId());
            ByteBufCodecs.STRING_UTF8.encode((Object)buf, (Object)re.villageName());
            ByteBufCodecs.STRING_UTF8.encode((Object)buf, (Object)re.cultureName());
            ByteBufCodecs.VAR_INT.encode((Object)buf, (Object)re.relation());
            ByteBufCodecs.STRING_UTF8.encode((Object)buf, (Object)re.relationLabel());
        }
        ControlledMilitaryPayload.encodeMilitaryLines(buf, payload.militaryLines);
    }

    private static ControlledMilitaryPayload decode(ByteBuf buf) {
        String villageUuid = (String)ByteBufCodecs.STRING_UTF8.decode((Object)buf);
        String villageName = (String)ByteBufCodecs.STRING_UTF8.decode((Object)buf);
        String ownerName = (String)ByteBufCodecs.STRING_UTF8.decode((Object)buf);
        String currentRaidTargetVillageId = (String)ByteBufCodecs.STRING_UTF8.decode((Object)buf);
        boolean raidInProgress = buf.readBoolean();
        int rawRelCount = (Integer)ByteBufCodecs.VAR_INT.decode((Object)buf);
        int relCount = Math.min(rawRelCount, 64);
        ArrayList<RelationEntry> relations = new ArrayList<RelationEntry>(relCount);
        for (int i = 0; i < rawRelCount; ++i) {
            String vid = (String)ByteBufCodecs.STRING_UTF8.decode((Object)buf);
            String vname = (String)ByteBufCodecs.STRING_UTF8.decode((Object)buf);
            String cname = (String)ByteBufCodecs.STRING_UTF8.decode((Object)buf);
            int rel = (Integer)ByteBufCodecs.VAR_INT.decode((Object)buf);
            String label = (String)ByteBufCodecs.STRING_UTF8.decode((Object)buf);
            if (i >= relCount) continue;
            relations.add(new RelationEntry(vid, vname, cname, rel, label));
        }
        List<PanelLine> militaryLines = ControlledMilitaryPayload.decodeMilitaryLines(buf);
        return new ControlledMilitaryPayload(villageUuid, villageName, ownerName, currentRaidTargetVillageId, raidInProgress, relations, militaryLines);
    }

    private static void encodeMilitaryLines(ByteBuf buf, List<PanelLine> lines) {
        ByteBufCodecs.VAR_INT.encode((Object)buf, (Object)lines.size());
        for (PanelLine line : lines) {
            ByteBufCodecs.STRING_UTF8.encode((Object)buf, (Object)line.text());
            buf.writeBoolean(line.isSeparator());
            ByteBufCodecs.STRING_UTF8.encode((Object)buf, (Object)(line.leftColumn() != null ? line.leftColumn() : ""));
            ByteBufCodecs.STRING_UTF8.encode((Object)buf, (Object)(line.rightColumn() != null ? line.rightColumn() : ""));
            ByteBufCodecs.STRING_UTF8.encode((Object)buf, (Object)(line.leftIcon() != null ? line.leftIcon() : ""));
            buf.writeBoolean(line.translatable());
            String[] args = line.translatableArgs();
            int argsCount = args != null ? args.length : 0;
            ByteBufCodecs.VAR_INT.encode((Object)buf, (Object)argsCount);
            if (args != null) {
                for (String arg : args) {
                    ByteBufCodecs.STRING_UTF8.encode((Object)buf, (Object)(arg != null ? arg : ""));
                }
            }
            ByteBufCodecs.VAR_INT.encode((Object)buf, (Object)line.color());
            buf.writeBoolean(line.bold());
            ByteBufCodecs.BYTE.encode((Object)buf, (Object)((byte)line.translatableArgMask()));
        }
    }

    private static List<PanelLine> decodeMilitaryLines(ByteBuf buf) {
        int rawCount = (Integer)ByteBufCodecs.VAR_INT.decode((Object)buf);
        int count = Math.min(rawCount, 512);
        ArrayList<PanelLine> result = new ArrayList<PanelLine>(count);
        for (int i = 0; i < rawCount; ++i) {
            int a;
            String text = (String)ByteBufCodecs.STRING_UTF8.decode((Object)buf);
            boolean sep = buf.readBoolean();
            String leftCol = (String)ByteBufCodecs.STRING_UTF8.decode((Object)buf);
            String rightCol = (String)ByteBufCodecs.STRING_UTF8.decode((Object)buf);
            String icon = (String)ByteBufCodecs.STRING_UTF8.decode((Object)buf);
            boolean translatable = buf.readBoolean();
            int rawArgs = (Integer)ByteBufCodecs.VAR_INT.decode((Object)buf);
            int argsCount = Math.min(rawArgs, 8);
            String[] args = argsCount > 0 ? new String[argsCount] : null;
            for (a = 0; a < argsCount; ++a) {
                args[a] = (String)ByteBufCodecs.STRING_UTF8.decode((Object)buf);
            }
            for (a = argsCount; a < rawArgs; ++a) {
                ByteBufCodecs.STRING_UTF8.decode((Object)buf);
            }
            int color = (Integer)ByteBufCodecs.VAR_INT.decode((Object)buf);
            boolean bold = buf.readBoolean();
            int argMask = (Byte)ByteBufCodecs.BYTE.decode((Object)buf) & 0xFF;
            if (i >= count) continue;
            String leftColVal = leftCol.isEmpty() ? null : leftCol;
            String rightColVal = rightCol.isEmpty() ? null : rightCol;
            String iconVal = icon.isEmpty() ? null : icon;
            result.add(new PanelLine(text, sep, leftColVal, rightColVal, iconVal, translatable, null, args, color, bold, argMask));
        }
        return result;
    }

    public CustomPacketPayload.Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public record RelationEntry(String villageId, String villageName, String cultureName, int relation, String relationLabel) {
    }
}

