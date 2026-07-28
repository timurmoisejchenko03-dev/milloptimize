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
import org.millenaire.network.MapData;
import org.millenaire.village.panel.PanelContent;
import org.millenaire.village.panel.PanelLine;
import org.millenaire.village.panel.PanelType;

public record VillageBookPayload(String villageName, String cultureId, List<PanelContent> sections, List<MapData.MapBuilding> mapBuildings, List<MapData.MapVillager> mapVillagers, int mapPlayerX, int mapPlayerZ, int mapCenterX, int mapCenterZ, MapData.MapTerrain mapTerrain, List<MapData.MapPath> mapPaths, boolean hasMapData, boolean degraded) implements CustomPacketPayload
{
    private static final int MAX_SECTIONS = 7;
    private static final int MAX_LINES = 200;
    private static final int MAX_MAP_ENTRIES = 256;
    private static final int MAX_ARGS = 8;
    private static final int MAX_PATHS = 8192;
    public static final CustomPacketPayload.Type<VillageBookPayload> TYPE = new CustomPacketPayload.Type(ResourceLocation.fromNamespaceAndPath((String)"millenaire", (String)"village_book"));
    public static final StreamCodec<ByteBuf, VillageBookPayload> STREAM_CODEC = StreamCodec.of(VillageBookPayload::encode, VillageBookPayload::decode);

    private static void encode(ByteBuf buf, VillageBookPayload payload) {
        ByteBufCodecs.STRING_UTF8.encode((Object)buf, (Object)payload.villageName);
        ByteBufCodecs.STRING_UTF8.encode((Object)buf, (Object)payload.cultureId);
        ByteBufCodecs.BOOL.encode((Object)buf, (Object)payload.hasMapData);
        ByteBufCodecs.BOOL.encode((Object)buf, (Object)payload.degraded);
        ByteBufCodecs.VAR_INT.encode((Object)buf, (Object)payload.sections.size());
        for (PanelContent section : payload.sections) {
            ByteBufCodecs.STRING_UTF8.encode((Object)buf, (Object)section.type().name());
            ByteBufCodecs.STRING_UTF8.encode((Object)buf, (Object)section.title());
            ByteBufCodecs.BOOL.encode((Object)buf, (Object)section.titleTranslatable());
            String[] titleArgs = section.titleArgs();
            int titleArgCount = titleArgs != null ? titleArgs.length : 0;
            ByteBufCodecs.VAR_INT.encode((Object)buf, (Object)titleArgCount);
            if (titleArgs != null) {
                for (String tArg : titleArgs) {
                    ByteBufCodecs.STRING_UTF8.encode((Object)buf, (Object)(tArg != null ? tArg : ""));
                }
            }
            ByteBufCodecs.VAR_INT.encode((Object)buf, (Object)section.lines().size());
            for (PanelLine line : section.lines()) {
                ByteBufCodecs.STRING_UTF8.encode((Object)buf, (Object)(line.leftColumn() != null ? line.leftColumn() : line.text()));
                ByteBufCodecs.STRING_UTF8.encode((Object)buf, (Object)(line.rightColumn() != null ? line.rightColumn() : ""));
                ByteBufCodecs.STRING_UTF8.encode((Object)buf, (Object)(line.leftIcon() != null ? line.leftIcon() : ""));
                ByteBufCodecs.BOOL.encode((Object)buf, (Object)line.isSeparator());
                ByteBufCodecs.BOOL.encode((Object)buf, (Object)line.translatable());
                String[] args = line.translatableArgs();
                int argsCount = args != null ? args.length : 0;
                ByteBufCodecs.VAR_INT.encode((Object)buf, (Object)argsCount);
                if (args != null) {
                    for (String arg : args) {
                        ByteBufCodecs.STRING_UTF8.encode((Object)buf, (Object)arg);
                    }
                }
                ByteBufCodecs.VAR_INT.encode((Object)buf, (Object)line.color());
                ByteBufCodecs.BOOL.encode((Object)buf, (Object)line.bold());
                ByteBufCodecs.BYTE.encode((Object)buf, (Object)((byte)line.translatableArgMask()));
                ByteBufCodecs.STRING_UTF8.encode((Object)buf, (Object)(line.nativePrefix() != null ? line.nativePrefix() : ""));
            }
        }
        if (payload.hasMapData) {
            MapData.encodeBuildingList(buf, payload.mapBuildings);
            MapData.encodeVillagerList(buf, payload.mapVillagers);
            ByteBufCodecs.VAR_INT.encode((Object)buf, (Object)payload.mapPlayerX);
            ByteBufCodecs.VAR_INT.encode((Object)buf, (Object)payload.mapPlayerZ);
            ByteBufCodecs.VAR_INT.encode((Object)buf, (Object)payload.mapCenterX);
            ByteBufCodecs.VAR_INT.encode((Object)buf, (Object)payload.mapCenterZ);
            MapData.encodeTerrain(buf, payload.mapTerrain);
            MapData.encodePathList(buf, payload.mapPaths);
        }
    }

    private static VillageBookPayload decode(ByteBuf buf) {
        int s;
        String villageName = (String)ByteBufCodecs.STRING_UTF8.decode((Object)buf);
        String cultureId = (String)ByteBufCodecs.STRING_UTF8.decode((Object)buf);
        boolean hasMapData = (Boolean)ByteBufCodecs.BOOL.decode((Object)buf);
        boolean degraded = (Boolean)ByteBufCodecs.BOOL.decode((Object)buf);
        int rawSectionCount = (Integer)ByteBufCodecs.VAR_INT.decode((Object)buf);
        int sectionCount = Math.min(rawSectionCount, 7);
        ArrayList<PanelContent> sections = new ArrayList<PanelContent>(sectionCount);
        for (s = 0; s < sectionCount; ++s) {
            int i;
            int ta;
            String typeName = (String)ByteBufCodecs.STRING_UTF8.decode((Object)buf);
            String title = (String)ByteBufCodecs.STRING_UTF8.decode((Object)buf);
            boolean titleTranslatable = (Boolean)ByteBufCodecs.BOOL.decode((Object)buf);
            int rawTitleArgCount = (Integer)ByteBufCodecs.VAR_INT.decode((Object)buf);
            int titleArgCount = Math.min(rawTitleArgCount, 8);
            String[] titleArgs = null;
            if (titleArgCount > 0) {
                titleArgs = new String[titleArgCount];
                for (ta = 0; ta < titleArgCount; ++ta) {
                    titleArgs[ta] = (String)ByteBufCodecs.STRING_UTF8.decode((Object)buf);
                }
            }
            for (ta = titleArgCount; ta < rawTitleArgCount; ++ta) {
                ByteBufCodecs.STRING_UTF8.decode((Object)buf);
            }
            int rawLineCount = (Integer)ByteBufCodecs.VAR_INT.decode((Object)buf);
            int lineCount = Math.min(rawLineCount, 200);
            ArrayList<PanelLine> lines = new ArrayList<PanelLine>(lineCount);
            for (i = 0; i < lineCount; ++i) {
                String nativePrefixVal;
                int a;
                String leftText = (String)ByteBufCodecs.STRING_UTF8.decode((Object)buf);
                String rightText = (String)ByteBufCodecs.STRING_UTF8.decode((Object)buf);
                String leftIcon = (String)ByteBufCodecs.STRING_UTF8.decode((Object)buf);
                boolean isSeparator = (Boolean)ByteBufCodecs.BOOL.decode((Object)buf);
                boolean translatable = (Boolean)ByteBufCodecs.BOOL.decode((Object)buf);
                int rawArgsCount = (Integer)ByteBufCodecs.VAR_INT.decode((Object)buf);
                int argsCount = Math.min(rawArgsCount, 8);
                String[] transArgs = null;
                if (argsCount > 0) {
                    transArgs = new String[argsCount];
                    for (a = 0; a < argsCount; ++a) {
                        transArgs[a] = (String)ByteBufCodecs.STRING_UTF8.decode((Object)buf);
                    }
                }
                for (a = argsCount; a < rawArgsCount; ++a) {
                    ByteBufCodecs.STRING_UTF8.decode((Object)buf);
                }
                int color = (Integer)ByteBufCodecs.VAR_INT.decode((Object)buf);
                boolean bold = (Boolean)ByteBufCodecs.BOOL.decode((Object)buf);
                int argMask = (Byte)ByteBufCodecs.BYTE.decode((Object)buf) & 0xFF;
                String nativePfx = (String)ByteBufCodecs.STRING_UTF8.decode((Object)buf);
                String string = nativePrefixVal = nativePfx.isEmpty() ? null : nativePfx;
                if (isSeparator) {
                    lines.add(PanelLine.separator());
                    continue;
                }
                if (!leftIcon.isEmpty() && translatable) {
                    String rightColVal = rightText.isEmpty() ? null : rightText;
                    lines.add(new PanelLine(leftText, false, null, rightColVal, leftIcon, true, null, transArgs, color, bold, argMask, nativePrefixVal));
                    continue;
                }
                if (!leftIcon.isEmpty()) {
                    lines.add(new PanelLine("", false, leftText, rightText, leftIcon, translatable, null, transArgs, color, bold, argMask, nativePrefixVal));
                    continue;
                }
                if (!rightText.isEmpty()) {
                    lines.add(new PanelLine("", false, leftText, rightText, null, translatable, null, transArgs, color, bold, argMask, nativePrefixVal));
                    continue;
                }
                lines.add(new PanelLine(leftText, false, null, null, null, translatable, null, transArgs, color, bold, argMask, nativePrefixVal));
            }
            for (i = lineCount; i < rawLineCount; ++i) {
                ByteBufCodecs.STRING_UTF8.decode((Object)buf);
                ByteBufCodecs.STRING_UTF8.decode((Object)buf);
                ByteBufCodecs.STRING_UTF8.decode((Object)buf);
                ByteBufCodecs.BOOL.decode((Object)buf);
                ByteBufCodecs.BOOL.decode((Object)buf);
                int skipArgs = (Integer)ByteBufCodecs.VAR_INT.decode((Object)buf);
                for (int a = 0; a < skipArgs; ++a) {
                    ByteBufCodecs.STRING_UTF8.decode((Object)buf);
                }
                ByteBufCodecs.VAR_INT.decode((Object)buf);
                ByteBufCodecs.BOOL.decode((Object)buf);
                ByteBufCodecs.BYTE.decode((Object)buf);
                ByteBufCodecs.STRING_UTF8.decode((Object)buf);
            }
            PanelType type = PanelType.fromName(typeName);
            sections.add(new PanelContent(type, title, lines, titleTranslatable, titleArgs));
        }
        for (s = sectionCount; s < rawSectionCount; ++s) {
            ByteBufCodecs.STRING_UTF8.decode((Object)buf);
            ByteBufCodecs.STRING_UTF8.decode((Object)buf);
            ByteBufCodecs.BOOL.decode((Object)buf);
            int skipTitleArgs = (Integer)ByteBufCodecs.VAR_INT.decode((Object)buf);
            for (int ta = 0; ta < skipTitleArgs; ++ta) {
                ByteBufCodecs.STRING_UTF8.decode((Object)buf);
            }
            int skipLineCount = (Integer)ByteBufCodecs.VAR_INT.decode((Object)buf);
            for (int i = 0; i < skipLineCount; ++i) {
                ByteBufCodecs.STRING_UTF8.decode((Object)buf);
                ByteBufCodecs.STRING_UTF8.decode((Object)buf);
                ByteBufCodecs.STRING_UTF8.decode((Object)buf);
                ByteBufCodecs.BOOL.decode((Object)buf);
                ByteBufCodecs.BOOL.decode((Object)buf);
                int skipArgs = (Integer)ByteBufCodecs.VAR_INT.decode((Object)buf);
                for (int a = 0; a < skipArgs; ++a) {
                    ByteBufCodecs.STRING_UTF8.decode((Object)buf);
                }
                ByteBufCodecs.VAR_INT.decode((Object)buf);
                ByteBufCodecs.BOOL.decode((Object)buf);
                ByteBufCodecs.BYTE.decode((Object)buf);
                ByteBufCodecs.STRING_UTF8.decode((Object)buf);
            }
        }
        List<MapData.MapBuilding> buildings = List.of();
        List<MapData.MapVillager> villagers = List.of();
        int playerX = 0;
        int playerZ = 0;
        int centerX = 0;
        int centerZ = 0;
        MapData.MapTerrain terrain = MapData.MapTerrain.EMPTY;
        List<MapData.MapPath> paths = List.of();
        if (hasMapData) {
            buildings = MapData.decodeBuildingList(buf, 256);
            villagers = MapData.decodeVillagerList(buf, 256);
            playerX = (Integer)ByteBufCodecs.VAR_INT.decode((Object)buf);
            playerZ = (Integer)ByteBufCodecs.VAR_INT.decode((Object)buf);
            centerX = (Integer)ByteBufCodecs.VAR_INT.decode((Object)buf);
            centerZ = (Integer)ByteBufCodecs.VAR_INT.decode((Object)buf);
            terrain = MapData.decodeTerrain(buf);
            paths = MapData.decodePathList(buf, 8192);
        }
        return new VillageBookPayload(villageName, cultureId, sections, buildings, villagers, playerX, playerZ, centerX, centerZ, terrain, paths, hasMapData, degraded);
    }

    public CustomPacketPayload.Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}

