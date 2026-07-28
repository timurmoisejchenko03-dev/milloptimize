/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  net.minecraft.core.HolderLookup$Provider
 *  net.minecraft.nbt.CompoundTag
 *  net.minecraft.nbt.ListTag
 *  net.minecraft.nbt.Tag
 *  net.minecraft.resources.ResourceLocation
 *  net.minecraft.server.level.ServerLevel
 *  net.minecraft.server.level.ServerPlayer
 *  net.minecraft.world.level.saveddata.SavedData
 *  net.minecraft.world.level.saveddata.SavedData$Factory
 */
package org.millenaire.village;

import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.saveddata.SavedData;
import org.millenaire.advancement.MillAdvancements;
import org.millenaire.village.VillageId;

public class PlayerCultureReputation
extends SavedData {
    private static final String DATA_NAME = "millenaire_culture_reputation";
    public static final int CULTURE_MAX_REPUTATION = 4096;
    public static final int CULTURE_MIN_REPUTATION = -640;
    private final Map<UUID, Map<ResourceLocation, Integer>> data = new HashMap<UUID, Map<ResourceLocation, Integer>>();
    private final Map<UUID, Boolean> donationMode = new HashMap<UUID, Boolean>();
    private final Map<UUID, Map<ResourceLocation, Integer>> languageData = new HashMap<UUID, Map<ResourceLocation, Integer>>();
    private final Map<UUID, Set<ResourceLocation>> visitedCultures = new HashMap<UUID, Set<ResourceLocation>>();
    private final Map<UUID, Set<String>> learnedCrops = new HashMap<UUID, Set<String>>();
    private final Map<UUID, Set<String>> learnedHuntingDrops = new HashMap<UUID, Set<String>>();
    private final Map<UUID, Set<ResourceLocation>> cultureControlTags = new HashMap<UUID, Set<ResourceLocation>>();
    private final Map<UUID, Set<VillageId>> discoveredVillages = new HashMap<UUID, Set<VillageId>>();
    private final Map<UUID, Map<VillageId, Integer>> diplomacyPoints = new HashMap<UUID, Map<VillageId, Integer>>();
    public static final int MAX_DIPLOMACY_POINTS = 5;
    public static final int LANGUAGE_BEGINNER = 100;
    public static final int LANGUAGE_MODERATE = 200;
    public static final int LANGUAGE_FLUENT = 500;
    private final Set<UUID> pendingAttila = new HashSet<UUID>();

    private PlayerCultureReputation() {
    }

    public static PlayerCultureReputation createForTest() {
        return new PlayerCultureReputation();
    }

    public static SavedData.Factory<PlayerCultureReputation> factory() {
        return new SavedData.Factory(PlayerCultureReputation::new, (tag, registries) -> PlayerCultureReputation.load(tag), null);
    }

    public static PlayerCultureReputation get(ServerLevel level) {
        return (PlayerCultureReputation)level.getDataStorage().computeIfAbsent(PlayerCultureReputation.factory(), DATA_NAME);
    }

    public int get(UUID player, ResourceLocation cultureId) {
        Map<ResourceLocation, Integer> playerMap = this.data.get(player);
        if (playerMap == null) {
            return 0;
        }
        return playerMap.getOrDefault(cultureId, 0);
    }

    public int countCulturesWithReputation(UUID player) {
        Map<ResourceLocation, Integer> playerMap = this.data.get(player);
        if (playerMap == null) {
            return 0;
        }
        int count = 0;
        for (int rep : playerMap.values()) {
            if (rep == 0) continue;
            ++count;
        }
        return count;
    }

    public boolean markCultureVisited(UUID player, ResourceLocation cultureId) {
        Set visited = this.visitedCultures.computeIfAbsent(player, k -> new HashSet());
        if (visited.add(cultureId)) {
            this.setDirty();
            return true;
        }
        return false;
    }

    public int countCulturesVisited(UUID player) {
        Set<ResourceLocation> visited = this.visitedCultures.get(player);
        return visited != null ? visited.size() : 0;
    }

    public void add(UUID player, ResourceLocation cultureId, int amount) {
        Map playerMap = this.data.computeIfAbsent(player, k -> new HashMap());
        int current = playerMap.getOrDefault(cultureId, 0);
        int newValue = Math.max(-640, Math.min(4096, current + amount));
        playerMap.put(cultureId, newValue);
        this.setDirty();
        if (newValue <= -640) {
            int nbAwfulRep = 0;
            Iterator iterator = playerMap.values().iterator();
            while (iterator.hasNext()) {
                int rep = (Integer)iterator.next();
                if (rep > -640) continue;
                ++nbAwfulRep;
            }
            if (nbAwfulRep >= 3) {
                this.pendingAttila.add(player);
            }
        }
    }

    public void checkPendingAttila(ServerPlayer player) {
        if (this.pendingAttila.remove(player.getUUID())) {
            MillAdvancements.grant(player, MillAdvancements.ATTILA);
        }
    }

    public boolean isDonationMode(UUID player) {
        return this.donationMode.getOrDefault(player, false);
    }

    public void setDonationMode(UUID player, boolean mode) {
        this.donationMode.put(player, mode);
        this.setDirty();
    }

    public int getLanguageKnowledge(UUID player, ResourceLocation cultureId) {
        Map<ResourceLocation, Integer> map = this.languageData.get(player);
        return map != null ? map.getOrDefault(cultureId, 0) : 0;
    }

    public void addLanguageKnowledge(UUID player, ResourceLocation cultureId, int items) {
        Map map = this.languageData.computeIfAbsent(player, k -> new HashMap());
        int current = map.getOrDefault(cultureId, 0);
        map.put(cultureId, Math.min(current + items, 500));
        this.setDirty();
    }

    public LanguageLevel getLanguageLevel(UUID player, ResourceLocation cultureId) {
        int score = this.getLanguageKnowledge(player, cultureId);
        if (score >= 500) {
            return LanguageLevel.FLUENT;
        }
        if (score >= 200) {
            return LanguageLevel.MODERATE;
        }
        if (score >= 100) {
            return LanguageLevel.BEGINNER;
        }
        return LanguageLevel.MINIMAL;
    }

    public boolean hasLearnedCrop(UUID player, String cropKey) {
        Set<String> set = this.learnedCrops.get(player);
        return set != null && set.contains(cropKey);
    }

    public void learnCrop(UUID player, String cropKey) {
        this.learnedCrops.computeIfAbsent(player, k -> new HashSet()).add(cropKey);
        this.setDirty();
    }

    public boolean hasLearnedHuntingDrop(UUID player, String dropKey) {
        Set<String> set = this.learnedHuntingDrops.get(player);
        return set != null && set.contains(dropKey);
    }

    public void learnHuntingDrop(UUID player, String dropKey) {
        this.learnedHuntingDrops.computeIfAbsent(player, k -> new HashSet()).add(dropKey);
        this.setDirty();
    }

    public boolean hasCultureControl(UUID player, ResourceLocation cultureId) {
        Set<ResourceLocation> set = this.cultureControlTags.get(player);
        return set != null && set.contains(cultureId);
    }

    public void grantCultureControl(UUID player, ResourceLocation cultureId) {
        this.cultureControlTags.computeIfAbsent(player, k -> new HashSet()).add(cultureId);
        this.setDirty();
    }

    public void revokeCultureControl(UUID player, ResourceLocation cultureId) {
        Set<ResourceLocation> set = this.cultureControlTags.get(player);
        if (set != null) {
            set.remove(cultureId);
            this.setDirty();
        }
    }

    public boolean hasDiscoveredVillage(UUID player, VillageId villageId) {
        Set<VillageId> set = this.discoveredVillages.get(player);
        return set != null && set.contains(villageId);
    }

    public boolean markVillageDiscovered(UUID player, VillageId villageId) {
        boolean added = this.discoveredVillages.computeIfAbsent(player, k -> new HashSet()).add(villageId);
        if (added) {
            this.setDirty();
        }
        return added;
    }

    public Set<VillageId> getDiscoveredVillages(UUID player) {
        Set<VillageId> set = this.discoveredVillages.get(player);
        return set != null ? Collections.unmodifiableSet(set) : Set.of();
    }

    public int getDiplomacyPoints(UUID player, VillageId villageId) {
        Map<VillageId, Integer> map = this.diplomacyPoints.get(player);
        return map != null ? map.getOrDefault(villageId, 0) : 0;
    }

    public boolean consumeDiplomacyPoint(UUID player, VillageId villageId) {
        Map<VillageId, Integer> map = this.diplomacyPoints.get(player);
        if (map == null) {
            return false;
        }
        int current = map.getOrDefault(villageId, 0);
        if (current <= 0) {
            return false;
        }
        map.put(villageId, current - 1);
        this.setDirty();
        return true;
    }

    public void regenerateDiplomacyPoints(UUID player, VillageId villageId) {
        Map map = this.diplomacyPoints.computeIfAbsent(player, k -> new HashMap());
        map.put(villageId, 5);
        this.setDirty();
    }

    public CompoundTag save(CompoundTag root, HolderLookup.Provider registries) {
        HashSet<UUID> allPlayers = new HashSet<UUID>();
        allPlayers.addAll(this.data.keySet());
        allPlayers.addAll(this.donationMode.keySet());
        allPlayers.addAll(this.languageData.keySet());
        allPlayers.addAll(this.visitedCultures.keySet());
        allPlayers.addAll(this.diplomacyPoints.keySet());
        allPlayers.addAll(this.learnedCrops.keySet());
        allPlayers.addAll(this.learnedHuntingDrops.keySet());
        allPlayers.addAll(this.cultureControlTags.keySet());
        allPlayers.addAll(this.discoveredVillages.keySet());
        ListTag playersList = new ListTag();
        for (UUID playerId : allPlayers) {
            Set<VillageId> set;
            Set<ResourceLocation> controls;
            Set<String> set2;
            Set<String> crops;
            Map<VillageId, Integer> map;
            Set<ResourceLocation> visited;
            Map<ResourceLocation, Integer> langMap;
            CompoundTag playerTag = new CompoundTag();
            playerTag.putUUID("player", playerId);
            Map<ResourceLocation, Integer> cultureMap = this.data.get(playerId);
            if (cultureMap != null) {
                ListTag culturesList = new ListTag();
                for (Map.Entry<ResourceLocation, Integer> entry : cultureMap.entrySet()) {
                    CompoundTag cultureTag = new CompoundTag();
                    cultureTag.putString("culture", entry.getKey().toString());
                    cultureTag.putInt("value", entry.getValue().intValue());
                    culturesList.add((Object)cultureTag);
                }
                playerTag.put("cultures", (Tag)culturesList);
            }
            if (this.donationMode.getOrDefault(playerId, false).booleanValue()) {
                playerTag.putBoolean("donation_mode", true);
            }
            if ((langMap = this.languageData.get(playerId)) != null && !langMap.isEmpty()) {
                ListTag langList = new ListTag();
                for (Object langEntry : langMap.entrySet()) {
                    CompoundTag compoundTag = new CompoundTag();
                    compoundTag.putString("culture", ((ResourceLocation)langEntry.getKey()).toString());
                    compoundTag.putInt("score", ((Integer)langEntry.getValue()).intValue());
                    langList.add((Object)compoundTag);
                }
                playerTag.put("languages", (Tag)langList);
            }
            if ((visited = this.visitedCultures.get(playerId)) != null && !visited.isEmpty()) {
                Object langEntry;
                ListTag listTag = new ListTag();
                langEntry = visited.iterator();
                while (langEntry.hasNext()) {
                    ResourceLocation resourceLocation = (ResourceLocation)langEntry.next();
                    CompoundTag vTag = new CompoundTag();
                    vTag.putString("culture", resourceLocation.toString());
                    listTag.add((Object)vTag);
                }
                playerTag.put("visited_cultures", (Tag)listTag);
            }
            if ((map = this.diplomacyPoints.get(playerId)) != null && !map.isEmpty()) {
                ListTag dpList = new ListTag();
                for (Object dpEntry : map.entrySet()) {
                    CompoundTag compoundTag = new CompoundTag();
                    compoundTag.putUUID("village_id", ((VillageId)dpEntry.getKey()).uuid());
                    compoundTag.putInt("points", ((Integer)dpEntry.getValue()).intValue());
                    dpList.add((Object)compoundTag);
                }
                playerTag.put("diplomacy_points", (Tag)dpList);
            }
            if ((crops = this.learnedCrops.get(playerId)) != null && !crops.isEmpty()) {
                Object dpEntry;
                ListTag listTag = new ListTag();
                dpEntry = crops.iterator();
                while (dpEntry.hasNext()) {
                    String string = (String)dpEntry.next();
                    CompoundTag cTag = new CompoundTag();
                    cTag.putString("key", string);
                    listTag.add((Object)cTag);
                }
                playerTag.put("learned_crops", (Tag)listTag);
            }
            if ((set2 = this.learnedHuntingDrops.get(playerId)) != null && !set2.isEmpty()) {
                ListTag dropList = new ListTag();
                for (Object drop : set2) {
                    CompoundTag dTag = new CompoundTag();
                    dTag.putString("key", (String)drop);
                    dropList.add((Object)dTag);
                }
                playerTag.put("learned_hunting_drops", (Tag)dropList);
            }
            if ((controls = this.cultureControlTags.get(playerId)) != null && !controls.isEmpty()) {
                Object drop;
                ListTag listTag = new ListTag();
                drop = controls.iterator();
                while (drop.hasNext()) {
                    ResourceLocation ctrl = (ResourceLocation)drop.next();
                    CompoundTag ctTag = new CompoundTag();
                    ctTag.putString("culture", ctrl.toString());
                    listTag.add((Object)ctTag);
                }
                playerTag.put("culture_control", (Tag)listTag);
            }
            if ((set = this.discoveredVillages.get(playerId)) != null && !set.isEmpty()) {
                ListTag discList = new ListTag();
                for (VillageId vid : set) {
                    CompoundTag dTag = new CompoundTag();
                    dTag.putUUID("village_id", vid.uuid());
                    discList.add((Object)dTag);
                }
                playerTag.put("discovered_villages", (Tag)discList);
            }
            playersList.add((Object)playerTag);
        }
        root.put("players", (Tag)playersList);
        return root;
    }

    private static PlayerCultureReputation load(CompoundTag root) {
        PlayerCultureReputation result = new PlayerCultureReputation();
        ListTag playersList = root.getList("players", 10);
        for (int i = 0; i < playersList.size(); ++i) {
            ResourceLocation cultureId;
            int j;
            CompoundTag playerTag = playersList.getCompound(i);
            UUID playerId = playerTag.getUUID("player");
            if (playerTag.contains("cultures")) {
                HashMap<ResourceLocation, Integer> cultureMap = new HashMap<ResourceLocation, Integer>();
                ListTag culturesList = playerTag.getList("cultures", 10);
                for (j = 0; j < culturesList.size(); ++j) {
                    CompoundTag cultureTag = culturesList.getCompound(j);
                    cultureId = ResourceLocation.parse((String)cultureTag.getString("culture"));
                    int value = cultureTag.getInt("value");
                    cultureMap.put(cultureId, value);
                }
                result.data.put(playerId, cultureMap);
            }
            if (playerTag.getBoolean("donation_mode")) {
                result.donationMode.put(playerId, true);
            }
            if (playerTag.contains("visited_cultures")) {
                HashSet<ResourceLocation> visited = new HashSet<ResourceLocation>();
                ListTag visitedList = playerTag.getList("visited_cultures", 10);
                for (j = 0; j < visitedList.size(); ++j) {
                    CompoundTag vTag = visitedList.getCompound(j);
                    visited.add(ResourceLocation.parse((String)vTag.getString("culture")));
                }
                result.visitedCultures.put(playerId, visited);
            }
            if (playerTag.contains("languages")) {
                HashMap<ResourceLocation, Integer> langMap = new HashMap<ResourceLocation, Integer>();
                ListTag langList = playerTag.getList("languages", 10);
                for (j = 0; j < langList.size(); ++j) {
                    CompoundTag langTag = langList.getCompound(j);
                    cultureId = ResourceLocation.parse((String)langTag.getString("culture"));
                    int score = langTag.getInt("score");
                    langMap.put(cultureId, score);
                }
                result.languageData.put(playerId, langMap);
            }
            if (playerTag.contains("diplomacy_points")) {
                HashMap<VillageId, Integer> dpMap = new HashMap<VillageId, Integer>();
                ListTag dpList = playerTag.getList("diplomacy_points", 10);
                for (j = 0; j < dpList.size(); ++j) {
                    CompoundTag dpTag = dpList.getCompound(j);
                    VillageId villageId = new VillageId(dpTag.getUUID("village_id"));
                    int points = dpTag.getInt("points");
                    dpMap.put(villageId, points);
                }
                result.diplomacyPoints.put(playerId, dpMap);
            }
            if (playerTag.contains("learned_crops")) {
                HashSet<String> crops = new HashSet<String>();
                ListTag cropList = playerTag.getList("learned_crops", 10);
                for (j = 0; j < cropList.size(); ++j) {
                    crops.add(cropList.getCompound(j).getString("key"));
                }
                result.learnedCrops.put(playerId, crops);
            }
            if (playerTag.contains("learned_hunting_drops")) {
                HashSet<String> drops = new HashSet<String>();
                ListTag dropList = playerTag.getList("learned_hunting_drops", 10);
                for (j = 0; j < dropList.size(); ++j) {
                    drops.add(dropList.getCompound(j).getString("key"));
                }
                result.learnedHuntingDrops.put(playerId, drops);
            }
            if (playerTag.contains("culture_control")) {
                HashSet<ResourceLocation> controls = new HashSet<ResourceLocation>();
                ListTag controlList = playerTag.getList("culture_control", 10);
                for (j = 0; j < controlList.size(); ++j) {
                    controls.add(ResourceLocation.parse((String)controlList.getCompound(j).getString("culture")));
                }
                result.cultureControlTags.put(playerId, controls);
            }
            if (!playerTag.contains("discovered_villages")) continue;
            HashSet<VillageId> discovered = new HashSet<VillageId>();
            ListTag discList = playerTag.getList("discovered_villages", 10);
            for (j = 0; j < discList.size(); ++j) {
                discovered.add(new VillageId(discList.getCompound(j).getUUID("village_id")));
            }
            result.discoveredVillages.put(playerId, discovered);
        }
        return result;
    }

    public static enum LanguageLevel {
        MINIMAL,
        BEGINNER,
        MODERATE,
        FLUENT;

    }
}

