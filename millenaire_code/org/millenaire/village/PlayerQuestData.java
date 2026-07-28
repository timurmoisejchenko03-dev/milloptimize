/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.mojang.logging.LogUtils
 *  javax.annotation.Nullable
 *  net.minecraft.core.HolderLookup$Provider
 *  net.minecraft.nbt.CompoundTag
 *  net.minecraft.nbt.ListTag
 *  net.minecraft.nbt.Tag
 *  net.minecraft.server.level.ServerLevel
 *  net.minecraft.world.level.saveddata.SavedData
 *  net.minecraft.world.level.saveddata.SavedData$Factory
 *  org.slf4j.Logger
 */
package org.millenaire.village;

import com.mojang.logging.LogUtils;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.function.Function;
import javax.annotation.Nullable;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.saveddata.SavedData;
import org.millenaire.quest.Quest;
import org.millenaire.quest.QuestInstance;
import org.millenaire.quest.QuestInstanceVillager;
import org.slf4j.Logger;

public class PlayerQuestData
extends SavedData {
    private static final Logger LOGGER = LogUtils.getLogger();
    private static final String DATA_NAME = "millenaire_quest_data";
    private final Map<UUID, List<QuestInstance>> activeQuests = new HashMap<UUID, List<QuestInstance>>();
    private final Map<UUID, Map<UUID, QuestInstance>> villagersInQuests = new HashMap<UUID, Map<UUID, QuestInstance>>();
    private final Map<UUID, Set<String>> playerTags = new HashMap<UUID, Set<String>>();
    private final Map<UUID, Map<String, String>> actionData = new HashMap<UUID, Map<String, String>>();

    private PlayerQuestData() {
    }

    public static SavedData.Factory<PlayerQuestData> factory(Function<String, Quest> questLookup) {
        return new SavedData.Factory(PlayerQuestData::new, (tag, registries) -> PlayerQuestData.load(tag, questLookup), null);
    }

    public static PlayerQuestData get(ServerLevel level, Function<String, Quest> questLookup) {
        return (PlayerQuestData)level.getDataStorage().computeIfAbsent(PlayerQuestData.factory(questLookup), DATA_NAME);
    }

    public List<QuestInstance> getActiveQuests(UUID playerId) {
        return this.activeQuests.getOrDefault(playerId, Collections.emptyList());
    }

    public void addQuest(UUID playerId, QuestInstance qi) {
        Map playerVillagerIndex = this.villagersInQuests.computeIfAbsent(playerId, k -> new HashMap());
        for (QuestInstanceVillager qiv : qi.getVillagers().values()) {
            if (!playerVillagerIndex.containsKey(qiv.getVillagerId())) continue;
            LOGGER.warn("Villager {} is already in a quest for player {} \u2014 rejecting quest '{}'", new Object[]{qiv.getVillagerId(), playerId, qi.getQuest().key()});
            return;
        }
        this.activeQuests.computeIfAbsent(playerId, k -> new ArrayList()).add(qi);
        for (QuestInstanceVillager qiv : qi.getVillagers().values()) {
            playerVillagerIndex.put(qiv.getVillagerId(), qi);
        }
        this.setDirty();
    }

    public void removeQuest(UUID playerId, QuestInstance qi) {
        Map<UUID, QuestInstance> playerVillagerIndex;
        List<QuestInstance> quests = this.activeQuests.get(playerId);
        if (quests != null) {
            quests.remove(qi);
            if (quests.isEmpty()) {
                this.activeQuests.remove(playerId);
            }
        }
        if ((playerVillagerIndex = this.villagersInQuests.get(playerId)) != null) {
            for (QuestInstanceVillager qiv : qi.getVillagers().values()) {
                playerVillagerIndex.remove(qiv.getVillagerId());
            }
            if (playerVillagerIndex.isEmpty()) {
                this.villagersInQuests.remove(playerId);
            }
        }
        this.setDirty();
    }

    public boolean isVillagerInQuest(UUID playerId, UUID villagerId) {
        Map<UUID, QuestInstance> index = this.villagersInQuests.get(playerId);
        return index != null && index.containsKey(villagerId);
    }

    @Nullable
    public QuestInstance getQuestForVillager(UUID playerId, UUID villagerId) {
        Map<UUID, QuestInstance> index = this.villagersInQuests.get(playerId);
        return index != null ? index.get(villagerId) : null;
    }

    public Set<String> getPlayerTags(UUID playerId) {
        return this.playerTags.getOrDefault(playerId, Collections.emptySet());
    }

    public void setPlayerTag(UUID playerId, String tag) {
        this.playerTags.computeIfAbsent(playerId, k -> new HashSet()).add(tag);
        this.setDirty();
    }

    public void clearPlayerTag(UUID playerId, String tag) {
        Set<String> tags = this.playerTags.get(playerId);
        if (tags != null) {
            tags.remove(tag);
            if (tags.isEmpty()) {
                this.playerTags.remove(playerId);
            }
            this.setDirty();
        }
    }

    public boolean hasPlayerTag(UUID playerId, String tag) {
        Set<String> tags = this.playerTags.get(playerId);
        return tags != null && tags.contains(tag);
    }

    @Nullable
    public String getActionData(UUID playerId, String key) {
        Map<String, String> data = this.actionData.get(playerId);
        return data != null ? data.get(key) : null;
    }

    public void setActionData(UUID playerId, String key, String value) {
        this.actionData.computeIfAbsent(playerId, k -> new HashMap()).put(key, value);
        this.setDirty();
    }

    public CompoundTag save(CompoundTag root, HolderLookup.Provider registries) {
        HashSet<UUID> allPlayers = new HashSet<UUID>();
        allPlayers.addAll(this.activeQuests.keySet());
        allPlayers.addAll(this.playerTags.keySet());
        allPlayers.addAll(this.actionData.keySet());
        ListTag playersList = new ListTag();
        for (UUID playerId : allPlayers) {
            Map<String, String> data;
            Set<String> tags;
            CompoundTag playerTag = new CompoundTag();
            playerTag.putUUID("uuid", playerId);
            List<QuestInstance> quests = this.activeQuests.get(playerId);
            if (quests != null && !quests.isEmpty()) {
                ListTag questsList = new ListTag();
                for (QuestInstance questInstance : quests) {
                    questsList.add((Object)questInstance.save());
                }
                playerTag.put("quests", (Tag)questsList);
            }
            if ((tags = this.playerTags.get(playerId)) != null && !tags.isEmpty()) {
                ListTag tagsList = new ListTag();
                for (String tag : tags) {
                    CompoundTag tagEntry = new CompoundTag();
                    tagEntry.putString("tag", tag);
                    tagsList.add((Object)tagEntry);
                }
                playerTag.put("tags", (Tag)tagsList);
            }
            if ((data = this.actionData.get(playerId)) != null && !data.isEmpty()) {
                CompoundTag compoundTag = new CompoundTag();
                for (Map.Entry<String, String> entry : data.entrySet()) {
                    compoundTag.putString(entry.getKey(), entry.getValue());
                }
                playerTag.put("actionData", (Tag)compoundTag);
            }
            playersList.add((Object)playerTag);
        }
        root.put("players", (Tag)playersList);
        return root;
    }

    private static PlayerQuestData load(CompoundTag root, Function<String, Quest> questLookup) {
        PlayerQuestData result = new PlayerQuestData();
        ListTag playersList = root.getList("players", 10);
        for (int i = 0; i < playersList.size(); ++i) {
            CompoundTag playerTag = playersList.getCompound(i);
            UUID playerId = playerTag.getUUID("uuid");
            if (playerTag.contains("quests")) {
                ListTag questsList = playerTag.getList("quests", 10);
                ArrayList<QuestInstance> quests = new ArrayList<QuestInstance>();
                HashMap<UUID, QuestInstance> villagerIndex = new HashMap<UUID, QuestInstance>();
                for (int j = 0; j < questsList.size(); ++j) {
                    CompoundTag questTag = questsList.getCompound(j);
                    QuestInstance qi = QuestInstance.load(questTag, playerId, questLookup);
                    if (qi == null) continue;
                    quests.add(qi);
                    for (QuestInstanceVillager qiv : qi.getVillagers().values()) {
                        villagerIndex.put(qiv.getVillagerId(), qi);
                    }
                }
                if (!quests.isEmpty()) {
                    result.activeQuests.put(playerId, quests);
                }
                if (!villagerIndex.isEmpty()) {
                    result.villagersInQuests.put(playerId, villagerIndex);
                }
            }
            if (playerTag.contains("tags")) {
                ListTag tagsList = playerTag.getList("tags", 10);
                HashSet<String> tags = new HashSet<String>();
                for (int j = 0; j < tagsList.size(); ++j) {
                    tags.add(tagsList.getCompound(j).getString("tag"));
                }
                if (!tags.isEmpty()) {
                    result.playerTags.put(playerId, tags);
                }
            }
            if (!playerTag.contains("actionData")) continue;
            CompoundTag dataTag = playerTag.getCompound("actionData");
            HashMap<String, String> data = new HashMap<String, String>();
            for (String key : dataTag.getAllKeys()) {
                data.put(key, dataTag.getString(key));
            }
            if (data.isEmpty()) continue;
            result.actionData.put(playerId, data);
        }
        return result;
    }
}

