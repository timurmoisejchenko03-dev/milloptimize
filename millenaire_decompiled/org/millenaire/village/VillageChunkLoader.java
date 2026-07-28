/*
 * Decompiled with CFR 0.150.
 * 
 * Could not load the following classes:
 *  com.mojang.logging.LogUtils
 *  net.minecraft.core.BlockPos
 *  net.minecraft.resources.ResourceLocation
 *  net.minecraft.server.level.ServerLevel
 *  net.minecraft.world.level.ChunkPos
 *  net.neoforged.neoforge.common.world.chunk.TicketController
 *  org.slf4j.Logger
 */
package org.millenaire.village;

import com.mojang.logging.LogUtils;
import java.util.Set;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.ChunkPos;
import net.neoforged.neoforge.common.world.chunk.TicketController;
import org.millenaire.village.Village;
import org.slf4j.Logger;

public final class VillageChunkLoader {
    private static final Logger LOGGER = LogUtils.getLogger();
    private static final TicketController CONTROLLER = new TicketController(ResourceLocation.fromNamespaceAndPath((String)"millenaire", (String)"village_chunks"), (level, ticketHelper) -> {});

    private VillageChunkLoader() {
    }

    public static TicketController getController() {
        return CONTROLLER;
    }

    public static void forceVillageChunks(ServerLevel level, BlockPos anchor, Set<ChunkPos> chunks) {
        int count = 0;
        for (ChunkPos cp : chunks) {
            CONTROLLER.forceChunk(level, anchor, cp.x, cp.z, true, true);
            ++count;
        }
        LOGGER.debug("[Mill\u00e9naire] Force-loaded {} chunks (anchor {})", (Object)count, (Object)anchor.toShortString());
    }

    public static void releaseVillageChunks(ServerLevel level, BlockPos anchor, Set<ChunkPos> chunks) {
        for (ChunkPos cp : chunks) {
            CONTROLLER.forceChunk(level, anchor, cp.x, cp.z, false, true);
        }
        LOGGER.debug("[Mill\u00e9naire] Released {} chunks (anchor {})", (Object)chunks.size(), (Object)anchor.toShortString());
    }

    public static void updateVillageChunks(ServerLevel level, Village village, Set<ChunkPos> newChunks) {
        Set<ChunkPos> oldChunks = village.getLoadedChunks();
        BlockPos anchor = village.getCenter();
        for (ChunkPos cp : oldChunks) {
            if (newChunks.contains((Object)cp)) continue;
            CONTROLLER.forceChunk(level, anchor, cp.x, cp.z, false, true);
        }
        for (ChunkPos cp : newChunks) {
            if (oldChunks.contains((Object)cp)) continue;
            CONTROLLER.forceChunk(level, anchor, cp.x, cp.z, true, true);
        }
        village.setLoadedChunks(newChunks);
    }
}

