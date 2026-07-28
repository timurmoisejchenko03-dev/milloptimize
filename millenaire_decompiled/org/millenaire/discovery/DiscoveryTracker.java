/*
 * Decompiled with CFR 0.150.
 * 
 * Could not load the following classes:
 *  net.minecraft.core.HolderLookup$Provider
 *  net.minecraft.nbt.CompoundTag
 *  net.minecraft.nbt.ListTag
 *  net.minecraft.nbt.StringTag
 *  net.minecraft.nbt.Tag
 *  net.minecraft.server.level.ServerLevel
 *  net.minecraft.world.level.saveddata.SavedData
 *  net.minecraft.world.level.saveddata.SavedData$Factory
 */
package org.millenaire.discovery;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.nbt.Tag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.saveddata.SavedData;

public class DiscoveryTracker
extends SavedData {
    private static final String DATA_NAME = "millenaire_discovery";
    private final Map<UUID, DiscoveryData> data = new HashMap<UUID, DiscoveryData>();

    DiscoveryTracker() {
    }

    public static SavedData.Factory<DiscoveryTracker> factory() {
        return new SavedData.Factory(DiscoveryTracker::new, (tag, registries) -> DiscoveryTracker.load(tag), null);
    }

    public static DiscoveryTracker get(ServerLevel level) {
        return (DiscoveryTracker)level.getDataStorage().computeIfAbsent(DiscoveryTracker.factory(), DATA_NAME);
    }

    public boolean unlockVillager(UUID player, String cultureKey, String villagerKey) {
        return this.getOrCreate((UUID)player).unlockedVillagers.add(DiscoveryTracker.composeKey(cultureKey, villagerKey)) && this.markDirty();
    }

    public boolean unlockBuilding(UUID player, String cultureKey, String buildingKey) {
        return this.getOrCreate((UUID)player).unlockedBuildings.add(DiscoveryTracker.composeKey(cultureKey, buildingKey)) && this.markDirty();
    }

    public boolean unlockVillage(UUID player, String cultureKey, String villageKey) {
        return this.getOrCreate((UUID)player).unlockedVillages.add(DiscoveryTracker.composeKey(cultureKey, villageKey)) && this.markDirty();
    }

    public boolean unlockTradeGood(UUID player, String cultureKey, String goodKey) {
        return this.getOrCreate((UUID)player).unlockedTradeGoods.add(DiscoveryTracker.composeKey(cultureKey, goodKey)) && this.markDirty();
    }

    public List<Boolean> unlockTradeGoods(UUID player, String cultureKey, List<String> goodKeys) {
        DiscoveryData dd = this.getOrCreate(player);
        boolean anyNew = false;
        ArrayList<Boolean> results = new ArrayList<Boolean>(goodKeys.size());
        for (String key : goodKeys) {
            boolean added = dd.unlockedTradeGoods.add(DiscoveryTracker.composeKey(cultureKey, key));
            results.add(added);
            anyNew = anyNew || added;
        }
        if (anyNew) {
            this.setDirty();
        }
        return results;
    }

    public boolean isVillagerUnlocked(UUID player, String cultureKey, String key) {
        DiscoveryData dd = this.data.get(player);
        return dd != null && dd.unlockedVillagers.contains(DiscoveryTracker.composeKey(cultureKey, key));
    }

    public boolean isBuildingUnlocked(UUID player, String cultureKey, String key) {
        DiscoveryData dd = this.data.get(player);
        return dd != null && dd.unlockedBuildings.contains(DiscoveryTracker.composeKey(cultureKey, key));
    }

    public boolean isVillageUnlocked(UUID player, String cultureKey, String key) {
        DiscoveryData dd = this.data.get(player);
        return dd != null && dd.unlockedVillages.contains(DiscoveryTracker.composeKey(cultureKey, key));
    }

    public boolean isTradeGoodUnlocked(UUID player, String cultureKey, String key) {
        DiscoveryData dd = this.data.get(player);
        return dd != null && dd.unlockedTradeGoods.contains(DiscoveryTracker.composeKey(cultureKey, key));
    }

    public Set<String> getUnlockedVillagers(UUID player) {
        DiscoveryData dd = this.data.get(player);
        return dd != null ? Collections.unmodifiableSet(dd.unlockedVillagers) : Set.of();
    }

    public Set<String> getUnlockedBuildings(UUID player) {
        DiscoveryData dd = this.data.get(player);
        return dd != null ? Collections.unmodifiableSet(dd.unlockedBuildings) : Set.of();
    }

    public Set<String> getUnlockedVillages(UUID player) {
        DiscoveryData dd = this.data.get(player);
        return dd != null ? Collections.unmodifiableSet(dd.unlockedVillages) : Set.of();
    }

    public Set<String> getUnlockedTradeGoods(UUID player) {
        DiscoveryData dd = this.data.get(player);
        return dd != null ? Collections.unmodifiableSet(dd.unlockedTradeGoods) : Set.of();
    }

    public CompoundTag save(CompoundTag root, HolderLookup.Provider registries) {
        ListTag playersList = new ListTag();
        for (Map.Entry<UUID, DiscoveryData> entry : this.data.entrySet()) {
            CompoundTag playerTag = new CompoundTag();
            playerTag.putUUID("player", entry.getKey());
            DiscoveryData dd = entry.getValue();
            DiscoveryTracker.saveStringSet(playerTag, "villagers", dd.unlockedVillagers);
            DiscoveryTracker.saveStringSet(playerTag, "buildings", dd.unlockedBuildings);
            DiscoveryTracker.saveStringSet(playerTag, "villages", dd.unlockedVillages);
            DiscoveryTracker.saveStringSet(playerTag, "trade_goods", dd.unlockedTradeGoods);
            playersList.add((Object)playerTag);
        }
        root.put("players", (Tag)playersList);
        return root;
    }

    private static DiscoveryTracker load(CompoundTag root) {
        DiscoveryTracker result = new DiscoveryTracker();
        ListTag playersList = root.getList("players", 10);
        for (int i = 0; i < playersList.size(); ++i) {
            CompoundTag playerTag = playersList.getCompound(i);
            UUID playerId = playerTag.getUUID("player");
            DiscoveryData dd = new DiscoveryData();
            DiscoveryTracker.loadStringSet(playerTag, "villagers", dd.unlockedVillagers);
            DiscoveryTracker.loadStringSet(playerTag, "buildings", dd.unlockedBuildings);
            DiscoveryTracker.loadStringSet(playerTag, "villages", dd.unlockedVillages);
            DiscoveryTracker.loadStringSet(playerTag, "trade_goods", dd.unlockedTradeGoods);
            result.data.put(playerId, dd);
        }
        return result;
    }

    private DiscoveryData getOrCreate(UUID player) {
        return this.data.computeIfAbsent(player, k -> new DiscoveryData());
    }

    static String composeKey(String cultureKey, String itemKey) {
        return cultureKey + "_" + itemKey;
    }

    private boolean markDirty() {
        this.setDirty();
        return true;
    }

    private static void saveStringSet(CompoundTag parent, String name, Set<String> set) {
        if (set.isEmpty()) {
            return;
        }
        ListTag list = new ListTag();
        for (String s : set) {
            list.add((Object)StringTag.valueOf((String)s));
        }
        parent.put(name, (Tag)list);
    }

    private static void loadStringSet(CompoundTag parent, String name, Set<String> target) {
        if (!parent.contains(name)) {
            return;
        }
        ListTag list = parent.getList(name, 8);
        for (int i = 0; i < list.size(); ++i) {
            target.add(list.getString(i));
        }
    }

    private static class DiscoveryData {
        final Set<String> unlockedVillagers = new HashSet<String>();
        final Set<String> unlockedBuildings = new HashSet<String>();
        final Set<String> unlockedVillages = new HashSet<String>();
        final Set<String> unlockedTradeGoods = new HashSet<String>();

        private DiscoveryData() {
        }
    }
}

