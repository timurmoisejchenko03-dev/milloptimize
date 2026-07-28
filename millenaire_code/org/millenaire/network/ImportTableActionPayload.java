/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  io.netty.buffer.ByteBuf
 *  io.netty.handler.codec.DecoderException
 *  javax.annotation.Nullable
 *  net.minecraft.core.BlockPos
 *  net.minecraft.nbt.CompoundTag
 *  net.minecraft.network.codec.ByteBufCodecs
 *  net.minecraft.network.codec.StreamCodec
 *  net.minecraft.network.protocol.common.custom.CustomPacketPayload
 *  net.minecraft.network.protocol.common.custom.CustomPacketPayload$Type
 *  net.minecraft.resources.ResourceLocation
 */
package org.millenaire.network;

import io.netty.buffer.ByteBuf;
import io.netty.handler.codec.DecoderException;
import javax.annotation.Nullable;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

public record ImportTableActionPayload(BlockPos blockPos, @Nullable Action action, CompoundTag actionData) implements CustomPacketPayload
{
    public static final CustomPacketPayload.Type<ImportTableActionPayload> TYPE = new CustomPacketPayload.Type(ResourceLocation.fromNamespaceAndPath((String)"millenaire", (String)"import_table_action"));
    public static final StreamCodec<ByteBuf, ImportTableActionPayload> STREAM_CODEC = StreamCodec.of(ImportTableActionPayload::encode, ImportTableActionPayload::decode);

    private static void encode(ByteBuf buf, ImportTableActionPayload p) {
        ByteBufCodecs.VAR_INT.encode((Object)buf, (Object)p.blockPos.getX());
        ByteBufCodecs.VAR_INT.encode((Object)buf, (Object)p.blockPos.getY());
        ByteBufCodecs.VAR_INT.encode((Object)buf, (Object)p.blockPos.getZ());
        ByteBufCodecs.VAR_INT.encode((Object)buf, (Object)p.action.ordinal());
        ByteBufCodecs.COMPOUND_TAG.encode((Object)buf, (Object)p.actionData);
    }

    private static ImportTableActionPayload decode(ByteBuf buf) {
        int x = (Integer)ByteBufCodecs.VAR_INT.decode((Object)buf);
        int y = (Integer)ByteBufCodecs.VAR_INT.decode((Object)buf);
        int z = (Integer)ByteBufCodecs.VAR_INT.decode((Object)buf);
        int actionOrdinal = (Integer)ByteBufCodecs.VAR_INT.decode((Object)buf);
        CompoundTag data = (CompoundTag)ByteBufCodecs.COMPOUND_TAG.decode((Object)buf);
        Action action = Action.fromOrdinal(actionOrdinal);
        if (action == null) {
            throw new DecoderException("ImportTableActionPayload: invalid action ordinal " + actionOrdinal);
        }
        return new ImportTableActionPayload(new BlockPos(x, y, z), action, data);
    }

    public CustomPacketPayload.Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static enum Action {
        CREATE_NEW,
        IMPORT_LEVEL,
        IMPORT_ALL,
        IMPORT_LEVEL_EXPORT,
        IMPORT_ALL_EXPORT,
        REIMPORT,
        REIMPORT_ALL,
        EXPORT,
        EXPORT_NEW_LEVEL,
        UPDATE_SETTINGS,
        SHOW_COSTS;

        private static final Action[] VALUES;

        public static Action fromOrdinal(int ordinal) {
            if (ordinal < 0 || ordinal >= VALUES.length) {
                return null;
            }
            return VALUES[ordinal];
        }

        static {
            VALUES = Action.values();
        }
    }
}

