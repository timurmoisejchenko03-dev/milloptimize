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
import org.millenaire.village.TravelBookLine;
import org.millenaire.village.TravelBookScreenState;

public record TravelBookContentPayload(TravelBookScreenState currentState, List<TravelBookLine> lines, boolean hasBack, boolean hasNext, boolean hasPrev, String pageTitle, boolean titleTranslatable, byte mockModelType, String mockTexture, String mockCloth0, String mockCloth1, float mockScale, String mockHeldItem, String mockHeldItemOffHand) {
    private static final int MAX_LINES = 500;
    private static final int MAX_STRING_LENGTH = 512;
    public static final CustomPacketPayload.Type<TravelBookContentPayload> TYPE = new CustomPacketPayload.Type(ResourceLocation.fromNamespaceAndPath((String)"millenaire", (String)"travel_book_content"));
    public static final StreamCodec<ByteBuf, TravelBookContentPayload> STREAM_CODEC = StreamCodec.of(TravelBookContentPayload::encode, TravelBookContentPayload::decode);

    public TravelBookContentPayload(TravelBookScreenState currentState, List<TravelBookLine> lines, boolean hasBack, boolean hasNext, boolean hasPrev, String pageTitle) {
        this(currentState, lines, hasBack, hasNext, hasPrev, pageTitle, false, 0, "", "", "", 0.0f, "", "");
    }

    public TravelBookContentPayload(TravelBookScreenState currentState, List<TravelBookLine> lines, boolean hasBack, boolean hasNext, boolean hasPrev, String pageTitle, boolean titleTranslatable) {
        this(currentState, lines, hasBack, hasNext, hasPrev, pageTitle, titleTranslatable, 0, "", "", "", 0.0f, "", "");
    }

    private static void encode(ByteBuf buf, TravelBookContentPayload payload) {
        ByteBufCodecs.VAR_INT.encode((Object)buf, (Object)payload.currentState.ordinal());
        ByteBufCodecs.BOOL.encode((Object)buf, (Object)payload.hasBack);
        ByteBufCodecs.BOOL.encode((Object)buf, (Object)payload.hasNext);
        ByteBufCodecs.BOOL.encode((Object)buf, (Object)payload.hasPrev);
        ByteBufCodecs.STRING_UTF8.encode((Object)buf, (Object)payload.pageTitle);
        ByteBufCodecs.BOOL.encode((Object)buf, (Object)payload.titleTranslatable);
        buf.writeByte((int)payload.mockModelType);
        ByteBufCodecs.STRING_UTF8.encode((Object)buf, (Object)payload.mockTexture);
        ByteBufCodecs.STRING_UTF8.encode((Object)buf, (Object)payload.mockCloth0);
        ByteBufCodecs.STRING_UTF8.encode((Object)buf, (Object)payload.mockCloth1);
        buf.writeFloat(payload.mockScale);
        ByteBufCodecs.STRING_UTF8.encode((Object)buf, (Object)payload.mockHeldItem);
        ByteBufCodecs.STRING_UTF8.encode((Object)buf, (Object)payload.mockHeldItemOffHand);
        ByteBufCodecs.VAR_INT.encode((Object)buf, (Object)payload.lines.size());
        for (TravelBookLine line : payload.lines) {
            TravelBookContentPayload.encodeLine(buf, line);
        }
    }

    private static void encodeLine(ByteBuf buf, TravelBookLine line) {
        ByteBufCodecs.STRING_UTF8.encode((Object)buf, (Object)line.text());
        ByteBufCodecs.BOOL.encode((Object)buf, (Object)line.isSeparator());
        ByteBufCodecs.STRING_UTF8.encode((Object)buf, (Object)(line.leftColumn() != null ? line.leftColumn() : ""));
        ByteBufCodecs.STRING_UTF8.encode((Object)buf, (Object)(line.rightColumn() != null ? line.rightColumn() : ""));
        ByteBufCodecs.STRING_UTF8.encode((Object)buf, (Object)(line.leftIcon() != null ? line.leftIcon() : ""));
        ByteBufCodecs.BOOL.encode((Object)buf, (Object)line.translatable());
        ByteBufCodecs.STRING_UTF8.encode((Object)buf, (Object)(line.nativePrefix() != null ? line.nativePrefix() : ""));
        boolean hasNav = line.navTarget() != null;
        ByteBufCodecs.BOOL.encode((Object)buf, (Object)hasNav);
        if (hasNav) {
            TravelBookLine.TravelBookNavTarget nav = line.navTarget();
            ByteBufCodecs.VAR_INT.encode((Object)buf, (Object)nav.targetState().ordinal());
            ByteBufCodecs.STRING_UTF8.encode((Object)buf, (Object)(nav.cultureKey() != null ? nav.cultureKey() : ""));
            ByteBufCodecs.STRING_UTF8.encode((Object)buf, (Object)(nav.categoryKey() != null ? nav.categoryKey() : ""));
            ByteBufCodecs.STRING_UTF8.encode((Object)buf, (Object)(nav.itemKey() != null ? nav.itemKey() : ""));
        }
    }

    private static TravelBookContentPayload decode(ByteBuf buf) {
        int i;
        int stateOrdinal = (Integer)ByteBufCodecs.VAR_INT.decode((Object)buf);
        TravelBookScreenState[] states = TravelBookScreenState.values();
        TravelBookScreenState state = stateOrdinal >= 0 && stateOrdinal < states.length ? states[stateOrdinal] : TravelBookScreenState.HOME;
        boolean hasBack = (Boolean)ByteBufCodecs.BOOL.decode((Object)buf);
        boolean hasNext = (Boolean)ByteBufCodecs.BOOL.decode((Object)buf);
        boolean hasPrev = (Boolean)ByteBufCodecs.BOOL.decode((Object)buf);
        String pageTitle = TravelBookContentPayload.truncate((String)ByteBufCodecs.STRING_UTF8.decode((Object)buf));
        boolean titleTranslatable = (Boolean)ByteBufCodecs.BOOL.decode((Object)buf);
        byte mockModelType = buf.readByte();
        String mockTexture = TravelBookContentPayload.truncate((String)ByteBufCodecs.STRING_UTF8.decode((Object)buf));
        String mockCloth0 = TravelBookContentPayload.truncate((String)ByteBufCodecs.STRING_UTF8.decode((Object)buf));
        String mockCloth1 = TravelBookContentPayload.truncate((String)ByteBufCodecs.STRING_UTF8.decode((Object)buf));
        float mockScale = buf.readFloat();
        String mockHeldItem = TravelBookContentPayload.truncate((String)ByteBufCodecs.STRING_UTF8.decode((Object)buf));
        String mockHeldItemOffHand = TravelBookContentPayload.truncate((String)ByteBufCodecs.STRING_UTF8.decode((Object)buf));
        int rawLineCount = (Integer)ByteBufCodecs.VAR_INT.decode((Object)buf);
        int lineCount = Math.min(rawLineCount, 500);
        ArrayList<TravelBookLine> lines = new ArrayList<TravelBookLine>(lineCount);
        for (i = 0; i < lineCount; ++i) {
            lines.add(TravelBookContentPayload.decodeLine(buf));
        }
        for (i = lineCount; i < rawLineCount; ++i) {
            TravelBookContentPayload.decodeLine(buf);
        }
        return new TravelBookContentPayload(state, lines, hasBack, hasNext, hasPrev, pageTitle, titleTranslatable, mockModelType, mockTexture, mockCloth0, mockCloth1, mockScale, mockHeldItem, mockHeldItemOffHand);
    }

    private static TravelBookLine decodeLine(ByteBuf buf) {
        String text = TravelBookContentPayload.truncate((String)ByteBufCodecs.STRING_UTF8.decode((Object)buf));
        boolean isSeparator = (Boolean)ByteBufCodecs.BOOL.decode((Object)buf);
        String leftColumn = TravelBookContentPayload.nullIfEmpty(TravelBookContentPayload.truncate((String)ByteBufCodecs.STRING_UTF8.decode((Object)buf)));
        String rightColumn = TravelBookContentPayload.nullIfEmpty(TravelBookContentPayload.truncate((String)ByteBufCodecs.STRING_UTF8.decode((Object)buf)));
        String leftIcon = TravelBookContentPayload.nullIfEmpty(TravelBookContentPayload.truncate((String)ByteBufCodecs.STRING_UTF8.decode((Object)buf)));
        boolean translatable = (Boolean)ByteBufCodecs.BOOL.decode((Object)buf);
        String nativePrefix = TravelBookContentPayload.nullIfEmpty(TravelBookContentPayload.truncate((String)ByteBufCodecs.STRING_UTF8.decode((Object)buf)));
        boolean hasNav = (Boolean)ByteBufCodecs.BOOL.decode((Object)buf);
        TravelBookLine.TravelBookNavTarget navTarget = null;
        if (hasNav) {
            int navOrdinal = (Integer)ByteBufCodecs.VAR_INT.decode((Object)buf);
            TravelBookScreenState[] states = TravelBookScreenState.values();
            TravelBookScreenState navState = navOrdinal >= 0 && navOrdinal < states.length ? states[navOrdinal] : TravelBookScreenState.HOME;
            String cultureKey = TravelBookContentPayload.nullIfEmpty(TravelBookContentPayload.truncate((String)ByteBufCodecs.STRING_UTF8.decode((Object)buf)));
            String categoryKey = TravelBookContentPayload.nullIfEmpty(TravelBookContentPayload.truncate((String)ByteBufCodecs.STRING_UTF8.decode((Object)buf)));
            String itemKey = TravelBookContentPayload.nullIfEmpty(TravelBookContentPayload.truncate((String)ByteBufCodecs.STRING_UTF8.decode((Object)buf)));
            navTarget = new TravelBookLine.TravelBookNavTarget(navState, cultureKey, categoryKey, itemKey);
        }
        return new TravelBookLine(text, isSeparator, leftColumn, rightColumn, leftIcon, translatable, nativePrefix, navTarget);
    }

    private static String truncate(String raw) {
        return raw.length() > 512 ? raw.substring(0, 512) : raw;
    }

    private static String nullIfEmpty(String s) {
        return s.isEmpty() ? null : s;
    }

    public boolean hasMockVillager() {
        return !this.mockTexture.isEmpty();
    }

    public CustomPacketPayload.Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}

