/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  net.minecraft.core.HolderLookup$Provider
 *  net.minecraft.nbt.CompoundTag
 *  net.minecraft.nbt.ListTag
 *  net.minecraft.nbt.Tag
 *  net.minecraft.server.level.ServerLevel
 *  net.minecraft.world.level.saveddata.SavedData
 *  net.minecraft.world.level.saveddata.SavedData$Factory
 */
package org.millenaire.map;

import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.saveddata.SavedData;
import org.millenaire.village.VillageId;

public class MillenaireMapMarkersData
extends SavedData {
    private static final String DATA_NAME = "millenaire_map_markers";
    private final Map<Integer, Set<VillageId>> trackedByMap = new HashMap<Integer, Set<VillageId>>();

    MillenaireMapMarkersData() {
    }

    public static SavedData.Factory<MillenaireMapMarkersData> factory() {
        return new SavedData.Factory(MillenaireMapMarkersData::new, (tag, registries) -> MillenaireMapMarkersData.load(tag), null);
    }

    public static MillenaireMapMarkersData get(ServerLevel level) {
        return (MillenaireMapMarkersData)level.getDataStorage().computeIfAbsent(MillenaireMapMarkersData.factory(), DATA_NAME);
    }

    public Set<VillageId> tracked(int mapId) {
        Set<VillageId> set = this.trackedByMap.get(mapId);
        return set != null ? Collections.unmodifiableSet(set) : Set.of();
    }

    public void addTracked(int mapId, VillageId villageId) {
        if (this.trackedByMap.computeIfAbsent(mapId, k -> new HashSet()).add(villageId)) {
            this.setDirty();
        }
    }

    public void removeTracked(int mapId, VillageId villageId) {
        Set<VillageId> set = this.trackedByMap.get(mapId);
        if (set != null && set.remove(villageId)) {
            if (set.isEmpty()) {
                this.trackedByMap.remove(mapId);
            }
            this.setDirty();
        }
    }

    public CompoundTag save(CompoundTag root, HolderLookup.Provider registries) {
        ListTag mapsList = new ListTag();
        for (Map.Entry<Integer, Set<VillageId>> entry : this.trackedByMap.entrySet()) {
            CompoundTag mapTag = new CompoundTag();
            mapTag.putInt("map_id", entry.getKey().intValue());
            ListTag villageList = new ListTag();
            for (VillageId vid : entry.getValue()) {
                CompoundTag vTag = new CompoundTag();
                vTag.putUUID("village_id", vid.uuid());
                villageList.add((Object)vTag);
            }
            mapTag.put("villages", (Tag)villageList);
            mapsList.add((Object)mapTag);
        }
        root.put("maps", (Tag)mapsList);
        return root;
    }

    private static MillenaireMapMarkersData load(CompoundTag root) {
        MillenaireMapMarkersData result = new MillenaireMapMarkersData();
        ListTag mapsList = root.getList("maps", 10);
        for (int i = 0; i < mapsList.size(); ++i) {
            CompoundTag mapTag = mapsList.getCompound(i);
            int mapId = mapTag.getInt("map_id");
            HashSet<VillageId> villages = new HashSet<VillageId>();
            ListTag villageList = mapTag.getList("villages", 10);
            for (int j = 0; j < villageList.size(); ++j) {
                villages.add(new VillageId(villageList.getCompound(j).getUUID("village_id")));
            }
            if (villages.isEmpty()) continue;
            result.trackedByMap.put(mapId, villages);
        }
        return result;
    }

    public static MillenaireMapMarkersData createForTest() {
        return new MillenaireMapMarkersData();
    }

    public static MillenaireMapMarkersData loadForTest(CompoundTag tag) {
        return MillenaireMapMarkersData.load(tag);
    }
}

