/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  net.minecraft.client.Minecraft
 *  net.minecraft.client.player.LocalPlayer
 *  net.minecraft.core.BlockPos
 *  net.minecraft.core.Position
 *  net.minecraft.core.particles.ParticleOptions
 *  net.minecraft.core.particles.ParticleTypes
 *  net.minecraft.resources.ResourceKey
 *  net.minecraft.util.RandomSource
 *  net.minecraft.world.level.Level
 */
package org.millenaire.client;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Position;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.resources.ResourceKey;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.Level;

public final class FireplaceSmokeHandler {
    private static final double RENDER_DIST_SQ = 4096.0;
    private static final double EVICT_DIST_SQ = 16384.0;
    private static final int EVICT_CHECK_INTERVAL = 20;
    private static final long SMOKE_START_TIME = 9000L;
    private static final long SMOKE_END_TIME = 16000L;
    private static final Map<UUID, FireplaceVillageData> knownPositions = new HashMap<UUID, FireplaceVillageData>();
    private static ResourceKey<Level> lastDimension = null;

    private FireplaceSmokeHandler() {
    }

    public static void updatePositions(UUID villageId, BlockPos center, List<BlockPos> positions) {
        if (positions.isEmpty()) {
            knownPositions.remove(villageId);
        } else {
            knownPositions.put(villageId, new FireplaceVillageData(center, positions));
        }
    }

    public static void clearAll() {
        knownPositions.clear();
        lastDimension = null;
    }

    public static void tick() {
        LocalPlayer player = Minecraft.getInstance().player;
        if (player == null) {
            return;
        }
        Level level = player.level();
        ResourceKey currentDimension = level.dimension();
        if (lastDimension != null && !lastDimension.equals((Object)currentDimension)) {
            knownPositions.clear();
        }
        lastDimension = currentDimension;
        long gameTime = level.getGameTime();
        if (gameTime % 2L != 0L) {
            return;
        }
        long dayTime = level.getDayTime() % 24000L;
        if (dayTime < 9000L || dayTime > 16000L) {
            return;
        }
        if (gameTime % 20L == 0L) {
            FireplaceSmokeHandler.evictDistant(player);
        }
        RandomSource random = level.getRandom();
        for (FireplaceVillageData data : knownPositions.values()) {
            for (BlockPos pos : data.positions()) {
                if (pos.distToCenterSqr((Position)player.position()) > 4096.0 || !level.isLoaded(pos)) continue;
                level.addAlwaysVisibleParticle((ParticleOptions)ParticleTypes.CAMPFIRE_COSY_SMOKE, true, (double)pos.getX() + 0.5 + random.nextGaussian() * 0.15, (double)pos.getY() + 0.5, (double)pos.getZ() + 0.5 + random.nextGaussian() * 0.15, 0.0, 0.07, 0.0);
            }
        }
    }

    private static void evictDistant(LocalPlayer player) {
        knownPositions.entrySet().removeIf(entry -> {
            BlockPos center = ((FireplaceVillageData)entry.getValue()).center();
            return center.distToCenterSqr((Position)player.position()) > 16384.0;
        });
    }

    public record FireplaceVillageData(BlockPos center, List<BlockPos> positions) {
        public FireplaceVillageData(BlockPos center, List<BlockPos> positions) {
            this.center = center;
            this.positions = Collections.unmodifiableList(positions);
        }
    }
}

