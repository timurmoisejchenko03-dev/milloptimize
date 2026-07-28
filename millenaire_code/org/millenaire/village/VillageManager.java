/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.mojang.logging.LogUtils
 *  javax.annotation.Nullable
 *  net.minecraft.core.BlockPos
 *  net.minecraft.core.Vec3i
 *  net.minecraft.server.level.ServerLevel
 *  net.minecraft.server.level.ServerPlayer
 *  net.minecraft.world.level.ChunkPos
 *  org.slf4j.Logger
 */
package org.millenaire.village;

import com.mojang.logging.LogUtils;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import javax.annotation.Nullable;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Vec3i;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.ChunkPos;
import org.millenaire.building.BuildingId;
import org.millenaire.culture.ModCultures;
import org.millenaire.culture.VillageType;
import org.millenaire.village.Village;
import org.millenaire.village.VillageChunkLoader;
import org.millenaire.village.VillageId;
import org.millenaire.village.VillageSavedData;
import org.slf4j.Logger;

public class VillageManager {
    private static final Logger LOGGER = LogUtils.getLogger();
    private final Map<VillageId, Village> villages = new LinkedHashMap<VillageId, Village>();
    private static final int FIREPLACE_SYNC_INTERVAL = 200;

    public void addVillage(Village village) {
        this.villages.put(village.getId(), village);
    }

    public void removeVillage(VillageId id) {
        this.villages.remove(id);
    }

    @Nullable
    public Village getVillage(VillageId id) {
        return this.villages.get(id);
    }

    public Collection<Village> getAllVillages() {
        return Collections.unmodifiableCollection(this.villages.values());
    }

    public void clear() {
        this.villages.clear();
    }

    @Nullable
    public Village findNearestVillage(BlockPos pos, double maxDistance) {
        Village nearest = null;
        double nearestDistSq = maxDistance * maxDistance;
        for (Village v : this.villages.values()) {
            double distSq = v.getCenter().distSqr((Vec3i)pos);
            if (!(distSq <= nearestDistSq)) continue;
            nearestDistSq = distSq;
            nearest = v;
        }
        return nearest;
    }

    @Nullable
    public Village findVillageContaining(BuildingId buildingId) {
        for (Village v : this.villages.values()) {
            if (v.findBuildingById(buildingId) == null) continue;
            return v;
        }
        return null;
    }

    public boolean isWithinMinDistance(BlockPos pos, double minDistance) {
        double minDistSq = minDistance * minDistance;
        for (Village v : this.villages.values()) {
            VillageType vt = ModCultures.getVillageType(v.getVillageTypeId());
            if (vt != null && vt.loneBuilding() || !(v.getCenter().distSqr((Vec3i)pos) < minDistSq)) continue;
            return true;
        }
        return false;
    }

    public boolean overlapsExistingVillage(BlockPos pos, int radius) {
        for (Village v : this.villages.values()) {
            int dz;
            VillageType vt = ModCultures.getVillageType(v.getVillageTypeId());
            if (vt != null && vt.loneBuilding()) continue;
            int otherRadius = vt != null ? vt.radius() : 0;
            double minDist = (double)radius + (double)otherRadius;
            int dx = v.getCenter().getX() - pos.getX();
            if (!((double)dx * (double)dx + (double)(dz = v.getCenter().getZ() - pos.getZ()) * (double)dz < minDist * minDist)) continue;
            return true;
        }
        return false;
    }

    public boolean isWithinMinDistanceLB(BlockPos pos, double minDistLBtoLB, double minDistLBtoVillage, List<VillageSavedData.LoneBuildingEntry> loneBuildingPositions) {
        double minDistVillageSq = minDistLBtoVillage * minDistLBtoVillage;
        for (Village v : this.villages.values()) {
            VillageType vt = ModCultures.getVillageType(v.getVillageTypeId());
            if (vt != null && vt.loneBuilding() || !(v.getCenter().distSqr((Vec3i)pos) < minDistVillageSq)) continue;
            return true;
        }
        double minDistLBSq = minDistLBtoLB * minDistLBtoLB;
        for (VillageSavedData.LoneBuildingEntry entry : loneBuildingPositions) {
            if (!(entry.pos().distSqr((Vec3i)pos) < minDistLBSq)) continue;
            return true;
        }
        return false;
    }

    public LBPlacement classifyLBPlacement(BlockPos pos, double minDistLBtoLB, double minDistLBtoVillage, List<VillageSavedData.LoneBuildingEntry> loneBuildingPositions) {
        boolean keyOnly = false;
        double villageBlockSq = minDistLBtoVillage / 2.0 * (minDistLBtoVillage / 2.0);
        double villageNearSq = minDistLBtoVillage * minDistLBtoVillage;
        for (Village v : this.villages.values()) {
            VillageType vt = ModCultures.getVillageType(v.getVillageTypeId());
            if (vt != null && vt.loneBuilding()) continue;
            double d = v.getCenter().distSqr((Vec3i)pos);
            if (d < villageBlockSq) {
                return LBPlacement.BLOCKED;
            }
            if (!(d < villageNearSq)) continue;
            keyOnly = true;
        }
        double lbBlockSq = minDistLBtoLB / 4.0 * (minDistLBtoLB / 4.0);
        double lbNearSq = minDistLBtoLB * minDistLBtoLB;
        for (VillageSavedData.LoneBuildingEntry entry : loneBuildingPositions) {
            double d = entry.pos().distSqr((Vec3i)pos);
            if (d < lbBlockSq) {
                return LBPlacement.BLOCKED;
            }
            if (!(d < lbNearSq)) continue;
            keyOnly = true;
        }
        return keyOnly ? LBPlacement.KEY_ONLY : LBPlacement.FAR;
    }

    public boolean isWithinMinDistanceOfLoneBuildings(BlockPos pos, double minDist, List<VillageSavedData.LoneBuildingEntry> loneBuildingPositions) {
        double minDistSq = minDist * minDist;
        for (VillageSavedData.LoneBuildingEntry entry : loneBuildingPositions) {
            if (!(entry.pos().distSqr((Vec3i)pos) < minDistSq)) continue;
            return true;
        }
        return false;
    }

    public void tick(ServerLevel level) {
        boolean anyDirty = false;
        boolean fireplaceSync = level.getGameTime() % 200L == 0L;
        for (Village v : this.villages.values()) {
            this.tickVillageActivation(level, v);
            if (v.isActive()) {
                v.tick(level);
                if (fireplaceSync) {
                    v.sendFireplacePositions(level);
                }
            }
            v.backgroundTick(level);
            if (!v.consumeDirty()) continue;
            anyDirty = true;
        }
        if (anyDirty) {
            VillageSavedData.get(level).setDirty();
        }
    }

    private void tickVillageActivation(ServerLevel level, Village v) {
        if (v.isForceActive()) {
            if (!v.isChunksForceLoaded()) {
                this.loadChunks(level, v);
            }
        } else {
            double nearestDistSq = Double.MAX_VALUE;
            for (ServerPlayer player : level.players()) {
                double distSq = player.blockPosition().distSqr((Vec3i)v.getCenter());
                if (!(distSq < nearestDistSq)) continue;
                nearestDistSq = distSq;
            }
            double keepRadiusSq = (double)Village.getKeepActiveRadius() * (double)Village.getKeepActiveRadius();
            double unloadRadiusSq = (double)Village.getUnloadRadius() * (double)Village.getUnloadRadius();
            if (nearestDistSq < keepRadiusSq) {
                if (!v.isChunksForceLoaded()) {
                    this.loadChunks(level, v);
                }
            } else if (nearestDistSq > unloadRadiusSq && v.isChunksForceLoaded()) {
                v.syncRecords(level);
                this.unloadChunks(level, v);
            }
        }
        if (v.isChunksForceLoaded() && v.isChunksNeedRefresh()) {
            Set<ChunkPos> newChunks = v.computeVillageChunks();
            VillageChunkLoader.updateVillageChunks(level, v, newChunks);
            v.setChunksNeedRefresh(false);
        }
        boolean active = v.isChunksForceLoaded() ? this.isAreaLoaded(level, v.getLoadedChunks()) : (v.isForceActive() ? true : level.isLoaded(v.getCenter()));
        v.setActive(active);
    }

    private void loadChunks(ServerLevel level, Village v) {
        Set<ChunkPos> chunks = v.computeVillageChunks();
        VillageChunkLoader.forceVillageChunks(level, v.getCenter(), chunks);
        v.setLoadedChunks(chunks);
        v.setChunksForceLoaded(true);
        LOGGER.debug("[Mill\u00e9naire] Village {} activated ({} chunks)", (Object)v.getVillageName(), (Object)chunks.size());
    }

    private void unloadChunks(ServerLevel level, Village v) {
        if (!v.getLoadedChunks().isEmpty()) {
            VillageChunkLoader.releaseVillageChunks(level, v.getCenter(), v.getLoadedChunks());
        }
        v.setLoadedChunks(Set.of());
        v.setChunksForceLoaded(false);
        v.setActive(false);
        LOGGER.debug("[Mill\u00e9naire] Village {} deactivated", (Object)v.getVillageName());
    }

    private boolean isAreaLoaded(ServerLevel level, Set<ChunkPos> chunks) {
        if (chunks.isEmpty()) {
            return false;
        }
        for (ChunkPos cp : chunks) {
            if (level.hasChunk(cp.x, cp.z)) continue;
            return false;
        }
        return true;
    }

    public static enum LBPlacement {
        FAR,
        KEY_ONLY,
        BLOCKED;

    }
}

