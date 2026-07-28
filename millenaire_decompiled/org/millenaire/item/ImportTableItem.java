/*
 * Decompiled with CFR 0.150.
 * 
 * Could not load the following classes:
 *  javax.annotation.Nullable
 *  net.minecraft.core.BlockPos
 *  net.minecraft.core.Vec3i
 *  net.minecraft.network.protocol.common.custom.CustomPacketPayload
 *  net.minecraft.server.level.ServerLevel
 *  net.minecraft.server.level.ServerPlayer
 *  net.minecraft.world.InteractionHand
 *  net.minecraft.world.InteractionResultHolder
 *  net.minecraft.world.entity.player.Player
 *  net.minecraft.world.item.BlockItem
 *  net.minecraft.world.item.Item$Properties
 *  net.minecraft.world.item.ItemStack
 *  net.minecraft.world.level.Level
 *  net.minecraft.world.level.block.Block
 *  net.minecraft.world.level.block.entity.BlockEntity
 *  net.minecraft.world.level.chunk.LevelChunk
 *  net.neoforged.neoforge.network.PacketDistributor
 */
package org.millenaire.item;

import javax.annotation.Nullable;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Vec3i;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.chunk.LevelChunk;
import net.neoforged.neoforge.network.PacketDistributor;
import org.millenaire.block.ImportTableBlockEntity;
import org.millenaire.building.BuildingExporter;
import org.millenaire.network.ImportTableSyncPayload;

public class ImportTableItem
extends BlockItem {
    private static final int SEARCH_CHUNK_RADIUS = 10;
    private static final int Y_COARSE_FILTER = 30;

    public ImportTableItem(Block block, Item.Properties properties) {
        super(block, properties);
    }

    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (level.isClientSide()) {
            return InteractionResultHolder.pass((Object)stack);
        }
        if (!(player instanceof ServerPlayer)) {
            return InteractionResultHolder.pass((Object)stack);
        }
        ServerPlayer serverPlayer = (ServerPlayer)player;
        ServerLevel serverLevel = (ServerLevel)level;
        ImportTableBlockEntity found = ImportTableItem.findTableContainingPlayer(serverLevel, player.blockPosition());
        if (found != null) {
            PacketDistributor.sendToPlayer((ServerPlayer)serverPlayer, (CustomPacketPayload)ImportTableSyncPayload.fromBlockEntity(found), (CustomPacketPayload[])new CustomPacketPayload[0]);
            return InteractionResultHolder.success((Object)stack);
        }
        return InteractionResultHolder.pass((Object)stack);
    }

    @Nullable
    private static ImportTableBlockEntity findTableContainingPlayer(ServerLevel level, BlockPos playerPos) {
        int playerCX = playerPos.getX() >> 4;
        int playerCZ = playerPos.getZ() >> 4;
        int playerY = playerPos.getY();
        ImportTableBlockEntity best = null;
        for (int cx = playerCX - 10; cx <= playerCX + 10; ++cx) {
            for (int cz = playerCZ - 10; cz <= playerCZ + 10; ++cz) {
                LevelChunk chunk = level.getChunkSource().getChunkNow(cx, cz);
                if (chunk == null) continue;
                for (BlockEntity blockEntity : chunk.getBlockEntities().values()) {
                    ImportTableBlockEntity table;
                    if (!(blockEntity instanceof ImportTableBlockEntity) || !(table = (ImportTableBlockEntity)blockEntity).hasPlan() || Math.abs(table.getBlockPos().getY() - playerY) > 30 || !ImportTableItem.isInsidePlot(playerPos, table)) continue;
                    if (best == null || table.isMainTable()) {
                        best = table;
                    }
                    if (!best.isMainTable()) continue;
                    return best;
                }
            }
        }
        return best;
    }

    public static boolean isInsidePlot(BlockPos pos, ImportTableBlockEntity table) {
        BlockPos origin = BuildingExporter.computeScanOrigin(table);
        Vec3i size = BuildingExporter.computeScanSize(table);
        int minX = origin.getX();
        int minZ = origin.getZ();
        int maxX = minX + size.getX() - 1;
        int maxZ = minZ + size.getZ() - 1;
        return pos.getX() >= minX && pos.getX() <= maxX && pos.getZ() >= minZ && pos.getZ() <= maxZ;
    }
}

