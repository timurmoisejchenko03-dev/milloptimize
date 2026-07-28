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
import net.minecraft.core.BlockPos;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import org.millenaire.block.ImportTableBlockEntity;

public record ImportTableSyncPayload(BlockPos blockPos, String buildingId, String variant, String cultureKey, String parentBuildingId, String parentVariant, int parentTriggerLevel, int length, int width, int upgradeLevel, int startingLevel, int height, int orientation, boolean clearGround, boolean exportSnow, boolean importMockBlocks, boolean convertToPreserveGround, boolean hasMainTablePos, int mainTableX, int mainTableY, int mainTableZ, boolean isMainTable, boolean importedFromCulture) implements CustomPacketPayload
{
    public static final CustomPacketPayload.Type<ImportTableSyncPayload> TYPE = new CustomPacketPayload.Type(ResourceLocation.fromNamespaceAndPath((String)"millenaire", (String)"import_table_sync"));
    public static final StreamCodec<ByteBuf, ImportTableSyncPayload> STREAM_CODEC = StreamCodec.of(ImportTableSyncPayload::encode, ImportTableSyncPayload::decode);

    public static ImportTableSyncPayload fromBlockEntity(ImportTableBlockEntity be) {
        BlockPos mainPos = be.getMainTablePos();
        return new ImportTableSyncPayload(be.getBlockPos(), be.getBuildingId(), be.getVariant(), be.getCultureKey(), be.getParentBuildingId(), be.getParentVariant(), be.getParentTriggerLevel(), be.getLength(), be.getWidth(), be.getUpgradeLevel(), be.getStartingLevel(), be.getHeight(), be.getOrientation(), be.isClearGround(), be.isExportSnow(), be.isImportMockBlocks(), be.isConvertToPreserveGround(), mainPos != null, mainPos != null ? mainPos.getX() : 0, mainPos != null ? mainPos.getY() : 0, mainPos != null ? mainPos.getZ() : 0, be.isMainTable(), be.isImportedFromCulture());
    }

    public BlockPos mainTablePos() {
        return this.hasMainTablePos ? new BlockPos(this.mainTableX, this.mainTableY, this.mainTableZ) : null;
    }

    private static void encode(ByteBuf buf, ImportTableSyncPayload p) {
        ByteBufCodecs.VAR_INT.encode((Object)buf, (Object)p.blockPos.getX());
        ByteBufCodecs.VAR_INT.encode((Object)buf, (Object)p.blockPos.getY());
        ByteBufCodecs.VAR_INT.encode((Object)buf, (Object)p.blockPos.getZ());
        ByteBufCodecs.STRING_UTF8.encode((Object)buf, (Object)p.buildingId);
        ByteBufCodecs.STRING_UTF8.encode((Object)buf, (Object)p.variant);
        ByteBufCodecs.STRING_UTF8.encode((Object)buf, (Object)p.cultureKey);
        ByteBufCodecs.STRING_UTF8.encode((Object)buf, (Object)p.parentBuildingId);
        ByteBufCodecs.STRING_UTF8.encode((Object)buf, (Object)p.parentVariant);
        ByteBufCodecs.VAR_INT.encode((Object)buf, (Object)p.parentTriggerLevel);
        ByteBufCodecs.VAR_INT.encode((Object)buf, (Object)p.length);
        ByteBufCodecs.VAR_INT.encode((Object)buf, (Object)p.width);
        ByteBufCodecs.VAR_INT.encode((Object)buf, (Object)p.upgradeLevel);
        ByteBufCodecs.VAR_INT.encode((Object)buf, (Object)p.startingLevel);
        ByteBufCodecs.VAR_INT.encode((Object)buf, (Object)p.height);
        ByteBufCodecs.VAR_INT.encode((Object)buf, (Object)p.orientation);
        ByteBufCodecs.BOOL.encode((Object)buf, (Object)p.clearGround);
        ByteBufCodecs.BOOL.encode((Object)buf, (Object)p.exportSnow);
        ByteBufCodecs.BOOL.encode((Object)buf, (Object)p.importMockBlocks);
        ByteBufCodecs.BOOL.encode((Object)buf, (Object)p.convertToPreserveGround);
        ByteBufCodecs.BOOL.encode((Object)buf, (Object)p.hasMainTablePos);
        ByteBufCodecs.VAR_INT.encode((Object)buf, (Object)p.mainTableX);
        ByteBufCodecs.VAR_INT.encode((Object)buf, (Object)p.mainTableY);
        ByteBufCodecs.VAR_INT.encode((Object)buf, (Object)p.mainTableZ);
        ByteBufCodecs.BOOL.encode((Object)buf, (Object)p.isMainTable);
        ByteBufCodecs.BOOL.encode((Object)buf, (Object)p.importedFromCulture);
    }

    private static ImportTableSyncPayload decode(ByteBuf buf) {
        int x = (Integer)ByteBufCodecs.VAR_INT.decode((Object)buf);
        int y = (Integer)ByteBufCodecs.VAR_INT.decode((Object)buf);
        int z = (Integer)ByteBufCodecs.VAR_INT.decode((Object)buf);
        String buildingId = (String)ByteBufCodecs.STRING_UTF8.decode((Object)buf);
        String variant = (String)ByteBufCodecs.STRING_UTF8.decode((Object)buf);
        String cultureKey = (String)ByteBufCodecs.STRING_UTF8.decode((Object)buf);
        String parentBuildingId = (String)ByteBufCodecs.STRING_UTF8.decode((Object)buf);
        String parentVariant = (String)ByteBufCodecs.STRING_UTF8.decode((Object)buf);
        int parentTriggerLevel = (Integer)ByteBufCodecs.VAR_INT.decode((Object)buf);
        int length = (Integer)ByteBufCodecs.VAR_INT.decode((Object)buf);
        int width = (Integer)ByteBufCodecs.VAR_INT.decode((Object)buf);
        int upgradeLevel = (Integer)ByteBufCodecs.VAR_INT.decode((Object)buf);
        int startingLevel = (Integer)ByteBufCodecs.VAR_INT.decode((Object)buf);
        int height = (Integer)ByteBufCodecs.VAR_INT.decode((Object)buf);
        int orientation = (Integer)ByteBufCodecs.VAR_INT.decode((Object)buf);
        boolean clearGround = (Boolean)ByteBufCodecs.BOOL.decode((Object)buf);
        boolean exportSnow = (Boolean)ByteBufCodecs.BOOL.decode((Object)buf);
        boolean importMockBlocks = (Boolean)ByteBufCodecs.BOOL.decode((Object)buf);
        boolean convertToPreserveGround = (Boolean)ByteBufCodecs.BOOL.decode((Object)buf);
        boolean hasMainTablePos = (Boolean)ByteBufCodecs.BOOL.decode((Object)buf);
        int mainX = (Integer)ByteBufCodecs.VAR_INT.decode((Object)buf);
        int mainY = (Integer)ByteBufCodecs.VAR_INT.decode((Object)buf);
        int mainZ = (Integer)ByteBufCodecs.VAR_INT.decode((Object)buf);
        boolean isMainTable = (Boolean)ByteBufCodecs.BOOL.decode((Object)buf);
        boolean importedFromCulture = (Boolean)ByteBufCodecs.BOOL.decode((Object)buf);
        return new ImportTableSyncPayload(new BlockPos(x, y, z), buildingId, variant, cultureKey, parentBuildingId, parentVariant, parentTriggerLevel, length, width, upgradeLevel, startingLevel, height, orientation, clearGround, exportSnow, importMockBlocks, convertToPreserveGround, hasMainTablePos, mainX, mainY, mainZ, isMainTable, importedFromCulture);
    }

    public CustomPacketPayload.Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}

