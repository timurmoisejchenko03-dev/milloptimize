/*
 * Decompiled with CFR 0.150.
 * 
 * Could not load the following classes:
 *  net.minecraft.core.BlockPos
 *  net.minecraft.server.level.ServerLevel
 *  net.minecraft.world.level.ChunkPos
 */
package org.millenaire.combat.raid;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.ChunkPos;
import org.millenaire.combat.raid.RaidManager;
import org.millenaire.culture.ModCultures;
import org.millenaire.culture.VillageType;
import org.millenaire.village.Village;
import org.millenaire.world.TerrainPreparer;

public final class RaidSpawnLocator {
    private static final int MAX_ATTEMPTS = 40;

    private RaidSpawnLocator() {
    }

    public static int[] perimeterPointTowardAttacker(int centerX, int centerZ, int radius, double attackerX, double attackerZ) {
        double dx = attackerX - (double)centerX;
        double dz = attackerZ - (double)centerZ;
        double dist = Math.sqrt(dx * dx + dz * dz);
        if (dist < 1.0E-6) {
            return new int[]{centerX, centerZ - radius};
        }
        double ux = dx / dist;
        double uz = dz / dist;
        int x = centerX + (int)Math.round(ux * (double)radius);
        int z = centerZ + (int)Math.round(uz * (double)radius);
        return new int[]{x, z};
    }

    public static BlockPos findSpawnPoint(ServerLevel level, Village target, BlockPos attackerCenter) {
        BlockPos center = target.getCenter();
        VillageType vt = ModCultures.getVillageType(target.getVillageTypeId());
        int radius = vt != null ? vt.radius() : 90;
        int[] base = RaidSpawnLocator.perimeterPointTowardAttacker(center.getX(), center.getZ(), radius, attackerCenter.getX(), attackerCenter.getZ());
        for (int i = 0; i < 40; ++i) {
            int groundY;
            int jitter = 5 + i;
            int tx = base[0] + level.random.nextInt(jitter) - level.random.nextInt(jitter);
            int tz = base[1] + level.random.nextInt(jitter) - level.random.nextInt(jitter);
            ChunkPos chunkPos = new ChunkPos(new BlockPos(tx, 0, tz));
            if (!level.hasChunk(chunkPos.x, chunkPos.z) || (groundY = TerrainPreparer.getGroundHeight(level, tx, tz)) <= level.getMinBuildHeight()) continue;
            return new BlockPos(tx, groundY + 1, tz);
        }
        return RaidManager.resolveDefendingPos(target);
    }
}

