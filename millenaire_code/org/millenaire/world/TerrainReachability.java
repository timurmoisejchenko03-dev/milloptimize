/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  net.minecraft.core.BlockPos
 *  net.minecraft.world.level.block.Rotation
 */
package org.millenaire.world;

import java.util.ArrayDeque;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.Rotation;
import org.millenaire.building.ClearMargins;
import org.millenaire.world.VillageTerrainMap;

public class TerrainReachability {
    private static final int[][] DIRS = new int[][]{{-1, 0}, {1, 0}, {0, -1}, {0, 1}};
    private final VillageTerrainMap map;
    private final int[][] regions;
    private final int thRegion;
    private static final float MIN_REACHABLE_RATIO = 0.7f;

    private TerrainReachability(VillageTerrainMap map, int[][] regions, int thRegion) {
        this.map = map;
        this.regions = regions;
        this.thRegion = thRegion;
    }

    public static TerrainReachability compute(VillageTerrainMap map, BlockPos townHallWorldPos) {
        int thLz;
        int size = map.getSize();
        int[][] regions = new int[size][size];
        int nextRegion = 0;
        for (int lx = 0; lx < size; ++lx) {
            for (int lz = 0; lz < size; ++lz) {
                if (regions[lx][lz] != 0 || !TerrainReachability.isCellPassable(map, lx, lz)) continue;
                TerrainReachability.floodFill(map, regions, lx, lz, ++nextRegion, size);
            }
        }
        int thLx = map.toLocalX(townHallWorldPos.getX());
        int thReg = map.inBounds(thLx, thLz = map.toLocalZ(townHallWorldPos.getZ())) ? regions[thLx][thLz] : 0;
        return new TerrainReachability(map, regions, thReg);
    }

    private static boolean isCellPassable(VillageTerrainMap map, int lx, int lz) {
        return !map.isDangerAt(lx, lz) && !map.isWaterAt(lx, lz) && map.getSpaceAbove(lx, lz) > 1;
    }

    private static boolean isConnected(VillageTerrainMap map, int cx, int cz, int nx, int nz) {
        if (!map.inBounds(nx, nz)) {
            return false;
        }
        if (!TerrainReachability.isCellPassable(map, nx, nz)) {
            return false;
        }
        int curY = map.getTopGround(cx, cz);
        int nbrY = map.getTopGround(nx, nz);
        int dy = nbrY - curY;
        short curSpace = map.getSpaceAbove(cx, cz);
        short nbrSpace = map.getSpaceAbove(nx, nz);
        if (dy == 0) {
            return true;
        }
        if (dy == -1) {
            return nbrSpace > 2;
        }
        if (dy == 1) {
            return curSpace > 2;
        }
        return false;
    }

    private static void floodFill(VillageTerrainMap map, int[][] regions, int startLx, int startLz, int regionId, int size) {
        ArrayDeque<int[]> queue = new ArrayDeque<int[]>();
        queue.add(new int[]{startLx, startLz});
        regions[startLx][startLz] = regionId;
        while (!queue.isEmpty()) {
            int[] cell = (int[])queue.poll();
            int cx = cell[0];
            int cz = cell[1];
            for (int[] d : DIRS) {
                int nx = cx + d[0];
                int nz = cz + d[1];
                if (nx < 0 || nx >= size || nz < 0 || nz >= size || regions[nx][nz] != 0 || !TerrainReachability.isConnected(map, cx, cz, nx, nz)) continue;
                regions[nx][nz] = regionId;
                queue.add(new int[]{nx, nz});
            }
        }
    }

    public boolean isReachable(int worldX, int worldZ) {
        int lz;
        int lx = this.map.toLocalX(worldX);
        if (!this.map.inBounds(lx, lz = this.map.toLocalZ(worldZ))) {
            return false;
        }
        if (this.thRegion == 0) {
            return false;
        }
        return this.regions[lx][lz] == this.thRegion;
    }

    public boolean isFootprintReachable(int worldX, int worldZ, int width, int depth, Rotation rotation) {
        VillageTerrainMap.FootprintRect rect = VillageTerrainMap.computeFootprintRect(worldX, worldZ, width, depth, ClearMargins.symmetric(0), rotation);
        int total = 0;
        int reachable = 0;
        for (int dx = 0; dx < rect.width(); ++dx) {
            for (int dz = 0; dz < rect.depth(); ++dz) {
                ++total;
                if (!this.isReachable(rect.startX() + dx, rect.startZ() + dz)) continue;
                ++reachable;
            }
        }
        if (total == 0) {
            return false;
        }
        return (float)reachable / (float)total >= 0.7f;
    }
}

