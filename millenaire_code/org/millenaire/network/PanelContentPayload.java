/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  io.netty.buffer.ByteBuf
 *  net.minecraft.network.RegistryFriendlyByteBuf
 *  net.minecraft.network.codec.ByteBufCodecs
 *  net.minecraft.network.codec.StreamCodec
 *  net.minecraft.network.protocol.common.custom.CustomPacketPayload
 *  net.minecraft.network.protocol.common.custom.CustomPacketPayload$Type
 *  net.minecraft.resources.ResourceLocation
 *  net.minecraft.world.item.ItemStack
 */
package org.millenaire.network;

import io.netty.buffer.ByteBuf;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import org.millenaire.network.MapData;
import org.millenaire.village.panel.PanelContent;
import org.millenaire.village.panel.PanelLine;
import org.millenaire.village.panel.PanelType;

public record PanelContentPayload(String panelTypeName, String title, List<String> lineTexts, List<Boolean> lineSeparators, List<String> lineLeftColumns, List<String> lineRightColumns, List<String> lineLeftIcons, List<Boolean> lineTranslatables, List<String> lineNavTargets, List<String[]> lineTranslatableArgs, List<Integer> lineColors, List<Boolean> lineBolds, List<Integer> lineArgMasks, List<String> lineNativePrefixes, List<ItemStack> lineLeftIconStacks, boolean titleTranslatable, List<String> titleArgs, List<MapData.MapBuilding> mapBuildings, List<MapData.MapVillager> mapVillagers, int mapPlayerX, int mapPlayerZ, int mapCenterX, int mapCenterZ, MapData.MapTerrain mapTerrain, List<MapData.MapChunk> forceLoadedChunks, List<MapData.MapPath> mapPaths) implements CustomPacketPayload
{
    private static final int MAX_LINES = 512;
    private static final int MAX_PATHS = 8192;
    private static final int MAX_TITLE_ARGS = 4;
    private static final String NAV_SEP = "|";
    public static final CustomPacketPayload.Type<PanelContentPayload> TYPE = new CustomPacketPayload.Type(ResourceLocation.fromNamespaceAndPath((String)"millenaire", (String)"panel_content"));
    public static final StreamCodec<RegistryFriendlyByteBuf, PanelContentPayload> STREAM_CODEC = StreamCodec.of(PanelContentPayload::encode, PanelContentPayload::decode);
    private static final int MAX_ARGS = 8;

    public static PanelContentPayload fromContent(PanelContent content) {
        return PanelContentPayload.fromContentWithMap(content, List.of(), List.of(), 0, 0, 0, 0, MapData.MapTerrain.EMPTY, List.of(), List.of());
    }

    public static PanelContentPayload fromContentWithMap(PanelContent content, List<MapData.MapBuilding> buildings, List<MapData.MapVillager> villagers, int playerX, int playerZ, int centerX, int centerZ, MapData.MapTerrain terrain, List<MapData.MapChunk> forceLoadedChunks, List<MapData.MapPath> mapPaths) {
        int size = content.lines().size();
        ArrayList<String> texts = new ArrayList<String>(size);
        ArrayList<Boolean> separators = new ArrayList<Boolean>(size);
        ArrayList<String> leftCols = new ArrayList<String>(size);
        ArrayList<String> rightCols = new ArrayList<String>(size);
        ArrayList<String> leftIcons = new ArrayList<String>(size);
        ArrayList<Boolean> translatables = new ArrayList<Boolean>(size);
        ArrayList<String> navTargets = new ArrayList<String>(size);
        ArrayList<String[]> transArgsList = new ArrayList<String[]>(size);
        ArrayList<Integer> colors = new ArrayList<Integer>(size);
        ArrayList<Boolean> bolds = new ArrayList<Boolean>(size);
        ArrayList<Integer> argMasks = new ArrayList<Integer>(size);
        ArrayList<String> nativePrefixes = new ArrayList<String>(size);
        ArrayList<ItemStack> leftIconStacks = new ArrayList<ItemStack>(size);
        for (PanelLine line : content.lines()) {
            texts.add(line.text());
            separators.add(line.isSeparator());
            leftCols.add(line.leftColumn() != null ? line.leftColumn() : "");
            rightCols.add(line.rightColumn() != null ? line.rightColumn() : "");
            leftIcons.add(line.leftIcon() != null ? line.leftIcon() : "");
            translatables.add(line.translatable());
            navTargets.add(PanelContentPayload.encodeNavTarget(line.navTarget()));
            transArgsList.add(line.translatableArgs());
            colors.add(line.color());
            bolds.add(line.bold());
            argMasks.add(line.translatableArgMask());
            nativePrefixes.add(line.nativePrefix() != null ? line.nativePrefix() : "");
            leftIconStacks.add(line.leftIconStack() != null ? line.leftIconStack() : ItemStack.EMPTY);
        }
        List<String> titleArgsList = content.titleArgs() != null ? List.of(content.titleArgs()) : List.of();
        return new PanelContentPayload(content.type().name(), content.title(), texts, separators, leftCols, rightCols, leftIcons, translatables, navTargets, transArgsList, colors, bolds, argMasks, nativePrefixes, leftIconStacks, content.titleTranslatable(), titleArgsList, buildings, villagers, playerX, playerZ, centerX, centerZ, terrain, forceLoadedChunks, mapPaths);
    }

    public boolean hasMapData() {
        return !this.mapBuildings.isEmpty() || !this.forceLoadedChunks.isEmpty();
    }

    public PanelContent toContent() {
        PanelType type = PanelType.fromName(this.panelTypeName);
        ArrayList<PanelLine> lines = new ArrayList<PanelLine>(this.lineTexts.size());
        for (int i = 0; i < this.lineTexts.size(); ++i) {
            ItemStack iconStack;
            boolean sep = i < this.lineSeparators.size() && this.lineSeparators.get(i) != false;
            String leftCol = i < this.lineLeftColumns.size() ? this.lineLeftColumns.get(i) : "";
            String rightCol = i < this.lineRightColumns.size() ? this.lineRightColumns.get(i) : "";
            String icon = i < this.lineLeftIcons.size() ? this.lineLeftIcons.get(i) : "";
            boolean trans = i < this.lineTranslatables.size() && this.lineTranslatables.get(i) != false;
            String navStr = i < this.lineNavTargets.size() ? this.lineNavTargets.get(i) : "";
            PanelLine.PanelNavTarget nav = PanelContentPayload.decodeNavTarget(navStr);
            String[] transArgs = i < this.lineTranslatableArgs.size() ? this.lineTranslatableArgs.get(i) : null;
            int color = i < this.lineColors.size() ? this.lineColors.get(i) : -1;
            boolean bold = i < this.lineBolds.size() && this.lineBolds.get(i) != false;
            int argMask = i < this.lineArgMasks.size() ? this.lineArgMasks.get(i) : 0;
            String nativePfx = i < this.lineNativePrefixes.size() ? this.lineNativePrefixes.get(i) : "";
            String nativePrefixVal = nativePfx.isEmpty() ? null : nativePfx;
            ItemStack itemStack = iconStack = i < this.lineLeftIconStacks.size() ? this.lineLeftIconStacks.get(i) : ItemStack.EMPTY;
            if (iconStack == null) {
                iconStack = ItemStack.EMPTY;
            }
            if (!leftCol.isEmpty()) {
                String iconVal = icon.isEmpty() ? null : icon;
                lines.add(new PanelLine("", sep, leftCol, rightCol, iconVal, trans, nav, transArgs, color, bold, argMask, nativePrefixVal, iconStack));
                continue;
            }
            if (!icon.isEmpty()) {
                String rightColVal = rightCol.isEmpty() ? null : rightCol;
                lines.add(new PanelLine(this.lineTexts.get(i), sep, null, rightColVal, icon, trans, nav, transArgs, color, bold, argMask, nativePrefixVal, iconStack));
                continue;
            }
            lines.add(new PanelLine(this.lineTexts.get(i), sep, null, null, null, trans, nav, transArgs, color, bold, argMask, nativePrefixVal, iconStack));
        }
        String[] tArgs = this.titleArgs != null && !this.titleArgs.isEmpty() ? this.titleArgs.toArray(new String[0]) : null;
        return new PanelContent(type, this.title, lines, this.titleTranslatable, tArgs);
    }

    private static String encodeNavTarget(PanelLine.PanelNavTarget target) {
        if (target == null) {
            return "";
        }
        return target.targetStateName() + NAV_SEP + target.cultureKey() + NAV_SEP + target.categoryKey() + NAV_SEP + target.itemKey();
    }

    private static PanelLine.PanelNavTarget decodeNavTarget(String encoded) {
        if (encoded == null || encoded.isEmpty()) {
            return null;
        }
        String[] parts = encoded.split("\\|", -1);
        if (parts.length < 4) {
            return null;
        }
        return new PanelLine.PanelNavTarget(parts[0], parts[1], parts[2], parts[3]);
    }

    private static void encode(RegistryFriendlyByteBuf buf, PanelContentPayload payload) {
        int i;
        ByteBufCodecs.STRING_UTF8.encode((Object)buf, (Object)payload.panelTypeName);
        ByteBufCodecs.STRING_UTF8.encode((Object)buf, (Object)payload.title);
        ByteBufCodecs.BOOL.encode((Object)buf, (Object)payload.titleTranslatable);
        int titleArgCount = payload.titleArgs != null ? payload.titleArgs.size() : 0;
        ByteBufCodecs.VAR_INT.encode((Object)buf, (Object)titleArgCount);
        for (i = 0; i < titleArgCount; ++i) {
            ByteBufCodecs.STRING_UTF8.encode((Object)buf, (Object)payload.titleArgs.get(i));
        }
        ByteBufCodecs.VAR_INT.encode((Object)buf, (Object)payload.lineTexts.size());
        for (i = 0; i < payload.lineTexts.size(); ++i) {
            ItemStack iconStack;
            ByteBufCodecs.STRING_UTF8.encode((Object)buf, (Object)payload.lineTexts.get(i));
            ByteBufCodecs.BOOL.encode((Object)buf, (Object)(i < payload.lineSeparators.size() && payload.lineSeparators.get(i) != false ? 1 : 0));
            ByteBufCodecs.STRING_UTF8.encode((Object)buf, (Object)(i < payload.lineLeftColumns.size() ? payload.lineLeftColumns.get(i) : ""));
            ByteBufCodecs.STRING_UTF8.encode((Object)buf, (Object)(i < payload.lineRightColumns.size() ? payload.lineRightColumns.get(i) : ""));
            ByteBufCodecs.STRING_UTF8.encode((Object)buf, (Object)(i < payload.lineLeftIcons.size() ? payload.lineLeftIcons.get(i) : ""));
            ByteBufCodecs.BOOL.encode((Object)buf, (Object)(i < payload.lineTranslatables.size() && payload.lineTranslatables.get(i) != false ? 1 : 0));
            ByteBufCodecs.STRING_UTF8.encode((Object)buf, (Object)(i < payload.lineNavTargets.size() ? payload.lineNavTargets.get(i) : ""));
            String[] args = i < payload.lineTranslatableArgs.size() ? payload.lineTranslatableArgs.get(i) : null;
            int argsCount = args != null ? args.length : 0;
            ByteBufCodecs.VAR_INT.encode((Object)buf, (Object)argsCount);
            if (args != null) {
                for (String arg : args) {
                    ByteBufCodecs.STRING_UTF8.encode((Object)buf, (Object)(arg != null ? arg : ""));
                }
            }
            ByteBufCodecs.VAR_INT.encode((Object)buf, (Object)(i < payload.lineColors.size() ? payload.lineColors.get(i) : -1));
            ByteBufCodecs.BOOL.encode((Object)buf, (Object)(i < payload.lineBolds.size() && payload.lineBolds.get(i) != false ? 1 : 0));
            ByteBufCodecs.BYTE.encode((Object)buf, (Object)((byte)(i < payload.lineArgMasks.size() ? payload.lineArgMasks.get(i) : 0)));
            ByteBufCodecs.STRING_UTF8.encode((Object)buf, (Object)(i < payload.lineNativePrefixes.size() ? payload.lineNativePrefixes.get(i) : ""));
            ItemStack itemStack = iconStack = i < payload.lineLeftIconStacks.size() ? payload.lineLeftIconStacks.get(i) : ItemStack.EMPTY;
            if (iconStack == null) {
                iconStack = ItemStack.EMPTY;
            }
            ItemStack.OPTIONAL_STREAM_CODEC.encode((Object)buf, (Object)iconStack);
        }
        MapData.encodeBuildingList((ByteBuf)buf, payload.mapBuildings);
        MapData.encodeVillagerList((ByteBuf)buf, payload.mapVillagers);
        ByteBufCodecs.VAR_INT.encode((Object)buf, (Object)payload.mapPlayerX);
        ByteBufCodecs.VAR_INT.encode((Object)buf, (Object)payload.mapPlayerZ);
        ByteBufCodecs.VAR_INT.encode((Object)buf, (Object)payload.mapCenterX);
        ByteBufCodecs.VAR_INT.encode((Object)buf, (Object)payload.mapCenterZ);
        MapData.encodeTerrain((ByteBuf)buf, payload.mapTerrain);
        MapData.encodeChunkList((ByteBuf)buf, payload.forceLoadedChunks);
        MapData.encodePathList((ByteBuf)buf, payload.mapPaths);
    }

    private static PanelContentPayload decode(RegistryFriendlyByteBuf buf) {
        int i;
        int i2;
        String typeName = (String)ByteBufCodecs.STRING_UTF8.decode((Object)buf);
        String title = (String)ByteBufCodecs.STRING_UTF8.decode((Object)buf);
        boolean titleTranslatable = (Boolean)ByteBufCodecs.BOOL.decode((Object)buf);
        int rawTitleArgCount = (Integer)ByteBufCodecs.VAR_INT.decode((Object)buf);
        int titleArgCount = Math.min(rawTitleArgCount, 4);
        ArrayList<String> titleArgs = new ArrayList<String>(titleArgCount);
        for (i2 = 0; i2 < titleArgCount; ++i2) {
            titleArgs.add((String)ByteBufCodecs.STRING_UTF8.decode((Object)buf));
        }
        for (i2 = titleArgCount; i2 < rawTitleArgCount; ++i2) {
            ByteBufCodecs.STRING_UTF8.decode((Object)buf);
        }
        int rawCount = (Integer)ByteBufCodecs.VAR_INT.decode((Object)buf);
        int count = Math.min(rawCount, 512);
        ArrayList<String> texts = new ArrayList<String>(count);
        ArrayList<Boolean> seps = new ArrayList<Boolean>(count);
        ArrayList<String> leftCols = new ArrayList<String>(count);
        ArrayList<String> rightCols = new ArrayList<String>(count);
        ArrayList<String> leftIcons = new ArrayList<String>(count);
        ArrayList<Boolean> translatables = new ArrayList<Boolean>(count);
        ArrayList<String> navTargets = new ArrayList<String>(count);
        ArrayList<String[]> transArgsList = new ArrayList<String[]>(count);
        ArrayList<Integer> colors = new ArrayList<Integer>(count);
        ArrayList<Boolean> bolds = new ArrayList<Boolean>(count);
        ArrayList<Integer> argMasks = new ArrayList<Integer>(count);
        ArrayList<String> nativePrefixes = new ArrayList<String>(count);
        ArrayList<ItemStack> iconStacks = new ArrayList<ItemStack>(count);
        for (i = 0; i < count; ++i) {
            int a;
            texts.add((String)ByteBufCodecs.STRING_UTF8.decode((Object)buf));
            seps.add((Boolean)ByteBufCodecs.BOOL.decode((Object)buf));
            leftCols.add((String)ByteBufCodecs.STRING_UTF8.decode((Object)buf));
            rightCols.add((String)ByteBufCodecs.STRING_UTF8.decode((Object)buf));
            leftIcons.add((String)ByteBufCodecs.STRING_UTF8.decode((Object)buf));
            translatables.add((Boolean)ByteBufCodecs.BOOL.decode((Object)buf));
            navTargets.add((String)ByteBufCodecs.STRING_UTF8.decode((Object)buf));
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
            transArgsList.add(transArgs);
            colors.add((Integer)ByteBufCodecs.VAR_INT.decode((Object)buf));
            bolds.add((Boolean)ByteBufCodecs.BOOL.decode((Object)buf));
            argMasks.add((Byte)ByteBufCodecs.BYTE.decode((Object)buf) & 0xFF);
            nativePrefixes.add((String)ByteBufCodecs.STRING_UTF8.decode((Object)buf));
            iconStacks.add((ItemStack)ItemStack.OPTIONAL_STREAM_CODEC.decode((Object)buf));
        }
        for (i = count; i < rawCount; ++i) {
            ByteBufCodecs.STRING_UTF8.decode((Object)buf);
            ByteBufCodecs.BOOL.decode((Object)buf);
            ByteBufCodecs.STRING_UTF8.decode((Object)buf);
            ByteBufCodecs.STRING_UTF8.decode((Object)buf);
            ByteBufCodecs.STRING_UTF8.decode((Object)buf);
            ByteBufCodecs.BOOL.decode((Object)buf);
            ByteBufCodecs.STRING_UTF8.decode((Object)buf);
            int skipArgs = (Integer)ByteBufCodecs.VAR_INT.decode((Object)buf);
            for (int a = 0; a < skipArgs; ++a) {
                ByteBufCodecs.STRING_UTF8.decode((Object)buf);
            }
            ByteBufCodecs.VAR_INT.decode((Object)buf);
            ByteBufCodecs.BOOL.decode((Object)buf);
            ByteBufCodecs.BYTE.decode((Object)buf);
            ByteBufCodecs.STRING_UTF8.decode((Object)buf);
            ItemStack.OPTIONAL_STREAM_CODEC.decode((Object)buf);
        }
        List<MapData.MapBuilding> buildings = MapData.decodeBuildingList((ByteBuf)buf, 256);
        List<MapData.MapVillager> villagers = MapData.decodeVillagerList((ByteBuf)buf, 256);
        int playerX = (Integer)ByteBufCodecs.VAR_INT.decode((Object)buf);
        int playerZ = (Integer)ByteBufCodecs.VAR_INT.decode((Object)buf);
        int centerX = (Integer)ByteBufCodecs.VAR_INT.decode((Object)buf);
        int centerZ = (Integer)ByteBufCodecs.VAR_INT.decode((Object)buf);
        MapData.MapTerrain terrain = MapData.decodeTerrain((ByteBuf)buf);
        List<MapData.MapChunk> chunks = MapData.decodeChunkList((ByteBuf)buf, 1024);
        List<MapData.MapPath> paths = MapData.decodePathList((ByteBuf)buf, 8192);
        return new PanelContentPayload(typeName, title, texts, seps, leftCols, rightCols, leftIcons, translatables, navTargets, transArgsList, colors, bolds, argMasks, nativePrefixes, iconStacks, titleTranslatable, titleArgs, buildings, villagers, playerX, playerZ, centerX, centerZ, terrain, chunks, paths);
    }

    public CustomPacketPayload.Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}

