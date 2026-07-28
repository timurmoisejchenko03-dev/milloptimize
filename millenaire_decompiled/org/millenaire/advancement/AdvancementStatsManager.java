/*
 * Decompiled with CFR 0.150.
 * 
 * Could not load the following classes:
 *  com.mojang.logging.LogUtils
 *  net.minecraft.core.HolderLookup$Provider
 *  net.minecraft.nbt.CompoundTag
 *  net.minecraft.nbt.ListTag
 *  net.minecraft.nbt.StringTag
 *  net.minecraft.nbt.Tag
 *  net.minecraft.resources.ResourceLocation
 *  net.minecraft.server.level.ServerLevel
 *  net.minecraft.server.level.ServerPlayer
 *  net.minecraft.world.level.saveddata.SavedData
 *  net.minecraft.world.level.saveddata.SavedData$Factory
 *  org.slf4j.Logger
 */
package org.millenaire.advancement;

import com.mojang.logging.LogUtils;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.UUID;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.saveddata.SavedData;
import org.slf4j.Logger;

public class AdvancementStatsManager
extends SavedData {
    private static final Logger LOGGER = LogUtils.getLogger();
    private static final String DATA_NAME = "millenaire_advancement_stats";
    private final Map<UUID, Set<String>> survivalStats = new HashMap<UUID, Set<String>>();
    private final Map<UUID, Set<String>> creativeStats = new HashMap<UUID, Set<String>>();
    private long anonymousUid = 0L;

    public static AdvancementStatsManager get(ServerLevel level) {
        return (AdvancementStatsManager)level.getServer().overworld().getDataStorage().computeIfAbsent(new SavedData.Factory(AdvancementStatsManager::new, AdvancementStatsManager::load), DATA_NAME);
    }

    public static void onAdvancementEarned(ServerPlayer player, ResourceLocation advancementId) {
        ServerLevel overworld = player.server.overworld();
        AdvancementStatsManager manager = AdvancementStatsManager.get(overworld);
        String key = advancementId.getPath();
        Map<UUID, Set<String>> targetMap = player.isCreative() ? manager.creativeStats : manager.survivalStats;
        Set playerSet = targetMap.computeIfAbsent(player.getUUID(), k -> new TreeSet());
        if (playerSet.add(key)) {
            manager.setDirty();
            LOGGER.debug("Advancement stat recorded: {} for {} ({})", new Object[]{key, player.getName().getString(), player.isCreative() ? "creative" : "survival"});
        }
    }

    public Set<String> getSurvivalStats(UUID player) {
        return Collections.unmodifiableSet(this.survivalStats.getOrDefault(player, Set.of()));
    }

    public Set<String> getCreativeStats(UUID player) {
        return Collections.unmodifiableSet(this.creativeStats.getOrDefault(player, Set.of()));
    }

    public int getTotalEarned(UUID player) {
        HashSet combined = new HashSet();
        combined.addAll(this.survivalStats.getOrDefault(player, Set.of()));
        combined.addAll(this.creativeStats.getOrDefault(player, Set.of()));
        return combined.size();
    }

    public long getOrCreateUid() {
        if (this.anonymousUid == 0L) {
            this.anonymousUid = (long)(Math.random() * 9.223372036854776E18);
            this.setDirty();
        }
        return this.anonymousUid;
    }

    public long computeKey(UUID player) {
        long key = 346186835L;
        for (String advancement : this.survivalStats.getOrDefault(player, Set.of())) {
            key += (long)AdvancementStatsManager.customStringHash(advancement + "survival");
        }
        for (String advancement : this.creativeStats.getOrDefault(player, Set.of())) {
            key += (long)AdvancementStatsManager.customStringHash(advancement + "creative");
        }
        return key += (long)AdvancementStatsManager.customStringHash(String.valueOf(this.getOrCreateUid())) * 250L;
    }

    private static int customStringHash(String string) {
        int hash = string.length();
        hash += string.indexOf("e");
        return hash += string.indexOf("a") * 2;
    }

    public CompoundTag save(CompoundTag tag, HolderLookup.Provider registries) {
        tag.put("survival", (Tag)AdvancementStatsManager.saveStatsMap(this.survivalStats));
        tag.put("creative", (Tag)AdvancementStatsManager.saveStatsMap(this.creativeStats));
        tag.putLong("anonymousUid", this.anonymousUid);
        return tag;
    }

    private static CompoundTag saveStatsMap(Map<UUID, Set<String>> map) {
        CompoundTag result = new CompoundTag();
        for (Map.Entry<UUID, Set<String>> entry : map.entrySet()) {
            ListTag list = new ListTag();
            for (String key : entry.getValue()) {
                list.add((Object)StringTag.valueOf((String)key));
            }
            result.put(entry.getKey().toString(), (Tag)list);
        }
        return result;
    }

    public static AdvancementStatsManager load(CompoundTag tag, HolderLookup.Provider registries) {
        AdvancementStatsManager manager = new AdvancementStatsManager();
        if (tag.contains("survival")) {
            AdvancementStatsManager.loadStatsMap(tag.getCompound("survival"), manager.survivalStats);
        }
        if (tag.contains("creative")) {
            AdvancementStatsManager.loadStatsMap(tag.getCompound("creative"), manager.creativeStats);
        }
        if (tag.contains("anonymousUid")) {
            manager.anonymousUid = tag.getLong("anonymousUid");
        }
        return manager;
    }

    private static void loadStatsMap(CompoundTag tag, Map<UUID, Set<String>> target) {
        for (String uuidStr : tag.getAllKeys()) {
            try {
                UUID uuid = UUID.fromString(uuidStr);
                ListTag list = tag.getList(uuidStr, 8);
                TreeSet<String> set = new TreeSet<String>();
                for (int i = 0; i < list.size(); ++i) {
                    set.add(list.getString(i));
                }
                target.put(uuid, set);
            }
            catch (IllegalArgumentException e) {
                LOGGER.warn("Invalid UUID in advancement stats: {}", (Object)uuidStr);
            }
        }
    }
}

