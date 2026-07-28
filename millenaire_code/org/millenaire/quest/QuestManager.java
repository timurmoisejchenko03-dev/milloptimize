/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.mojang.logging.LogUtils
 *  net.minecraft.core.BlockPos
 *  net.minecraft.core.Vec3i
 *  net.minecraft.network.chat.Component
 *  net.minecraft.network.protocol.common.custom.CustomPacketPayload
 *  net.minecraft.resources.ResourceLocation
 *  net.minecraft.server.level.ServerLevel
 *  net.minecraft.server.level.ServerPlayer
 *  net.minecraft.world.level.Level
 *  net.neoforged.neoforge.network.PacketDistributor
 *  org.slf4j.Logger
 */
package org.millenaire.quest;

import com.mojang.logging.LogUtils;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Vec3i;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.Level;
import net.neoforged.neoforge.network.PacketDistributor;
import org.millenaire.culture.ModCultures;
import org.millenaire.culture.VillageType;
import org.millenaire.network.QuestNetworkHelper;
import org.millenaire.quest.Quest;
import org.millenaire.quest.QuestInstance;
import org.millenaire.quest.QuestInstanceVillager;
import org.millenaire.quest.QuestRegistry;
import org.millenaire.quest.QuestVillagerDef;
import org.millenaire.village.PlayerCultureReputation;
import org.millenaire.village.PlayerQuestData;
import org.millenaire.village.Village;
import org.millenaire.village.VillageId;
import org.millenaire.village.VillageSavedData;
import org.millenaire.village.VillagerRecord;
import org.millenaire.world.VillageSpawner;
import org.slf4j.Logger;

public final class QuestManager {
    private static final Logger LOGGER = LogUtils.getLogger();
    private static final int TICK_INTERVAL = 1000;
    private static final double NEARBY_VILLAGE_MAX_DISTANCE = 2000.0;

    private QuestManager() {
    }

    public static void tickQuests(ServerPlayer player, ServerLevel level) {
        long worldTime = level.getDayTime();
        if (!level.isDay()) {
            return;
        }
        if (worldTime % 1000L != 0L) {
            return;
        }
        QuestManager.testQuestsNow(player, level, false);
    }

    public static void testQuestsNow(ServerPlayer player, ServerLevel level, boolean skipChanceRoll) {
        ServerLevel overworld = level.getServer().getLevel(Level.OVERWORLD);
        if (overworld == null) {
            return;
        }
        PlayerQuestData data = PlayerQuestData.get(overworld, QuestRegistry::get);
        UUID playerId = player.getUUID();
        long worldTime = level.getDayTime();
        ArrayList<QuestInstance> active = new ArrayList<QuestInstance>(data.getActiveQuests(playerId));
        for (int i = active.size() - 1; i >= 0; --i) {
            try {
                ((QuestInstance)active.get(i)).checkStatus(worldTime, player, data);
                continue;
            }
            catch (Exception e) {
                LOGGER.error("Quest check error, destroying: {}", (Object)((QuestInstance)active.get(i)).getQuest().key(), (Object)e);
                ((QuestInstance)active.get(i)).destroyQuest(data, playerId);
            }
        }
        VillageSavedData savedData = VillageSavedData.get(overworld);
        for (Quest quest : QuestRegistry.all()) {
            QuestManager.tryInstantiate(quest, player, overworld, data, savedData, skipChanceRoll);
        }
    }

    public static boolean tryInstantiateForced(Quest quest, ServerPlayer player, ServerLevel level) {
        ServerLevel overworld = level.getServer().getLevel(Level.OVERWORLD);
        if (overworld == null) {
            return false;
        }
        PlayerQuestData data = PlayerQuestData.get(overworld, QuestRegistry::get);
        VillageSavedData savedData = VillageSavedData.get(overworld);
        int before = data.getActiveQuests(player.getUUID()).size();
        QuestManager.tryInstantiate(quest, player, overworld, data, savedData, true);
        return data.getActiveQuests(player.getUUID()).size() > before;
    }

    static void tryInstantiate(Quest quest, ServerPlayer player, ServerLevel overworld, PlayerQuestData data, VillageSavedData savedData, boolean skipChanceRoll) {
        if (!skipChanceRoll && Math.random() > quest.chancePerHour()) {
            return;
        }
        UUID playerId = player.getUUID();
        int count = 0;
        for (QuestInstance qi : data.getActiveQuests(playerId)) {
            if (!qi.getQuest().key().equals(quest.key())) continue;
            ++count;
        }
        if (count >= quest.maxSimultaneous()) {
            return;
        }
        for (String tag : quest.globalTagsRequired()) {
            if (savedData.isGlobalTagSet(tag)) continue;
            return;
        }
        for (String tag : quest.globalTagsForbidden()) {
            if (!savedData.isGlobalTagSet(tag)) continue;
            return;
        }
        for (String tag : quest.playerTagsRequired()) {
            if (data.hasPlayerTag(playerId, tag)) continue;
            return;
        }
        for (String tag : quest.playerTagsForbidden()) {
            if (!data.hasPlayerTag(playerId, tag)) continue;
            return;
        }
        LOGGER.debug("Testing quest {} for player {}", (Object)quest.key(), (Object)player.getName().getString());
        QuestVillagerDef startingDef = quest.villagerDefs().get(0);
        ArrayList possibleVillagers = new ArrayList();
        for (Village village : savedData.getVillageManager().getAllVillages()) {
            int cultureRep;
            int villageRep;
            if (!village.isActive() || (villageRep = village.getReputation().get(playerId)) + (cultureRep = PlayerCultureReputation.get(overworld).get(playerId, village.getCultureId())) < quest.minReputation()) continue;
            LOGGER.debug("Looking for starting villager in: {}", (Object)village.getVillageName());
            for (Map.Entry<UUID, VillagerRecord> entry : village.getVillagerRecords().entrySet()) {
                VillagerRecord vr = entry.getValue();
                if (!QuestManager.testVillager(startingDef, playerId, vr, data)) continue;
                HashMap<String, QuestInstanceVillager> villagers = new HashMap<String, QuestInstanceVillager>();
                villagers.put(startingDef.key(), new QuestInstanceVillager(vr.getUuid(), village.getId().uuid()));
                boolean error = false;
                LOGGER.debug("Found possible starting villager: {} ({})", (Object)vr.getFirstName(), (Object)vr.getVillagerTypeId());
                for (QuestVillagerDef qvd : quest.villagerDefs()) {
                    if (error) break;
                    if (qvd == startingDef) continue;
                    QuestInstanceVillager relatedQiv = (QuestInstanceVillager)villagers.get(qvd.relatedTo());
                    if (relatedQiv == null) {
                        error = true;
                        break;
                    }
                    VillagerRecord relatedRecord = QuestManager.findVillagerRecord(savedData, relatedQiv.getVillagerId());
                    if (relatedRecord == null) {
                        error = true;
                        break;
                    }
                    Village relatedVillage = savedData.getVillageManager().getVillage(new VillageId(relatedQiv.getVillageId()));
                    if (relatedVillage == null) {
                        error = true;
                        break;
                    }
                    String relation = qvd.relation();
                    if (relation == null) {
                        error = true;
                        break;
                    }
                    HashSet<UUID> assignedUuids = new HashSet<UUID>();
                    for (QuestInstanceVillager qiv : villagers.values()) {
                        assignedUuids.add(qiv.getVillagerId());
                    }
                    switch (relation) {
                        case "samevillage": {
                            Object chosen;
                            ArrayList<Object> candidates = new ArrayList<Object>();
                            for (VillagerRecord vr2 : relatedVillage.getVillagerRecords().values()) {
                                if (assignedUuids.contains(vr2.getUuid()) || vr2.getHomeBuilding() != null && relatedRecord.getHomeBuilding() != null && vr2.getHomeBuilding().equals(relatedRecord.getHomeBuilding()) || !QuestManager.testVillager(qvd, playerId, vr2, data)) continue;
                                candidates.add(vr2);
                            }
                            if (!candidates.isEmpty()) {
                                chosen = (VillagerRecord)candidates.get((int)(Math.random() * (double)candidates.size()));
                                villagers.put(qvd.key(), new QuestInstanceVillager(((VillagerRecord)chosen).getUuid(), relatedVillage.getId().uuid()));
                                break;
                            }
                            error = true;
                            break;
                        }
                        case "samehouse": {
                            Object chosen;
                            ArrayList<Object> candidates = new ArrayList();
                            for (VillagerRecord vr2 : relatedVillage.getVillagerRecords().values()) {
                                if (assignedUuids.contains(vr2.getUuid()) || vr2.getHomeBuilding() == null || relatedRecord.getHomeBuilding() == null || !vr2.getHomeBuilding().equals(relatedRecord.getHomeBuilding()) || !QuestManager.testVillager(qvd, playerId, vr2, data)) continue;
                                candidates.add(vr2);
                            }
                            if (!candidates.isEmpty()) {
                                chosen = (VillagerRecord)candidates.get((int)(Math.random() * (double)candidates.size()));
                                villagers.put(qvd.key(), new QuestInstanceVillager(((VillagerRecord)chosen).getUuid(), relatedVillage.getId().uuid()));
                                break;
                            }
                            error = true;
                            break;
                        }
                        case "nearbyvillage": 
                        case "anyvillage": {
                            ArrayList<Object> candidates = new ArrayList();
                            for (Village v2 : savedData.getVillageManager().getAllVillages()) {
                                double dist;
                                if (v2.getId().equals(relatedVillage.getId()) || "nearbyvillage".equals(relation) && (dist = Math.sqrt(v2.getCenter().distSqr((Vec3i)relatedVillage.getCenter()))) >= 2000.0) continue;
                                for (VillagerRecord vr2 : v2.getVillagerRecords().values()) {
                                    if (assignedUuids.contains(vr2.getUuid()) || !QuestManager.testVillager(qvd, playerId, vr2, data)) continue;
                                    candidates.add(new QuestInstanceVillager(vr2.getUuid(), v2.getId().uuid()));
                                }
                            }
                            if (!candidates.isEmpty()) {
                                villagers.put(qvd.key(), (QuestInstanceVillager)candidates.get((int)(Math.random() * (double)candidates.size())));
                                break;
                            }
                            error = true;
                            break;
                        }
                        default: {
                            LOGGER.error("Unknown relation: {}", (Object)relation);
                            error = true;
                        }
                    }
                }
                if (error) continue;
                possibleVillagers.add(villagers);
                LOGGER.debug("Found all the villagers needed: {}", (Object)villagers.size());
            }
        }
        if (possibleVillagers.isEmpty()) {
            return;
        }
        Map selectedOption = (Map)possibleVillagers.get((int)(Math.random() * (double)possibleVillagers.size()));
        long worldTime = overworld.getDayTime();
        long uniqueId = (long)(Math.random() * 9.223372036854776E18);
        QuestInstance qi = new QuestInstance(quest, playerId, selectedOption, 0, worldTime, worldTime, uniqueId);
        data.addQuest(playerId, qi);
        PacketDistributor.sendToPlayer((ServerPlayer)player, (CustomPacketPayload)QuestNetworkHelper.buildSyncPayload(qi, player), (CustomPacketPayload[])new CustomPacketPayload[0]);
        LOGGER.info("Quest '{}' instantiated for player {} with {} villagers", new Object[]{quest.key(), player.getName().getString(), selectedOption.size()});
    }

    private static boolean testVillager(QuestVillagerDef def, UUID playerId, VillagerRecord vr, PlayerQuestData data) {
        String tagPlayer;
        if (data.isVillagerInQuest(playerId, vr.getUuid())) {
            return false;
        }
        if (!def.villagerTypes().isEmpty()) {
            String typeIdPath;
            String string = typeIdPath = vr.getVillagerTypeId() != null ? vr.getVillagerTypeId().getPath() : "";
            if (!def.villagerTypes().contains(typeIdPath)) {
                return false;
            }
        }
        for (String tag : def.requiredTags()) {
            tagPlayer = String.valueOf(playerId) + "_" + tag;
            if (vr.hasQuestTag(tagPlayer)) continue;
            return false;
        }
        for (String tag : def.forbiddenTags()) {
            tagPlayer = String.valueOf(playerId) + "_" + tag;
            if (!vr.hasQuestTag(tagPlayer)) continue;
            return false;
        }
        return true;
    }

    private static VillagerRecord findVillagerRecord(VillageSavedData savedData, UUID villagerId) {
        for (Village village : savedData.getVillageManager().getAllVillages()) {
            VillagerRecord vr = village.getVillagerRecord(villagerId);
            if (vr == null) continue;
            return vr;
        }
        return null;
    }

    public static void tickSpecialActions(ServerPlayer player, ServerLevel level) {
        ServerLevel overworld = level.getServer().getLevel(Level.OVERWORLD);
        if (overworld == null) {
            return;
        }
        PlayerQuestData data = PlayerQuestData.get(overworld, QuestRegistry::get);
        UUID playerId = player.getUUID();
        QuestManager.checkNormanMarvelPickLocationComplete(playerId, data);
        if (level.getGameTime() % 10L == 0L) {
            QuestManager.normanMarvelGenerateMarvel(playerId, data, overworld);
        }
    }

    private static void checkNormanMarvelPickLocationComplete(UUID playerId, PlayerQuestData data) {
    }

    private static void normanMarvelGenerateMarvel(UUID playerId, PlayerQuestData data, ServerLevel overworld) {
        if (!data.hasPlayerTag(playerId, "normanmarvel_generate")) {
            return;
        }
        String locationStr = data.getActionData(playerId, "normanmarvel_location");
        if (locationStr == null) {
            LOGGER.warn("normanmarvel_generate tag set but no normanmarvel_location action data for player {}", (Object)playerId);
            data.clearPlayerTag(playerId, "normanmarvel_generate");
            return;
        }
        String[] parts = locationStr.split("/");
        if (parts.length != 3) {
            LOGGER.warn("Invalid normanmarvel_location format '{}' for player {}", (Object)locationStr, (Object)playerId);
            data.clearPlayerTag(playerId, "normanmarvel_generate");
            return;
        }
        try {
            int x = Integer.parseInt(parts[0]);
            int y = Integer.parseInt(parts[1]);
            int z = Integer.parseInt(parts[2]);
            BlockPos pos = new BlockPos(x, y, z);
            ResourceLocation notredameId = ResourceLocation.fromNamespaceAndPath((String)"millenaire", (String)"norman/notredame");
            VillageType notredameType = ModCultures.getVillageType(notredameId);
            if (notredameType == null) {
                LOGGER.warn("Village type 'norman/notredame' not found \u2014 cannot generate marvel");
                data.clearPlayerTag(playerId, "normanmarvel_generate");
                return;
            }
            Component failure = VillageSpawner.spawnVillage(overworld, pos, notredameType);
            if (failure == null) {
                data.clearPlayerTag(playerId, "normanmarvel_picklocation");
                data.clearPlayerTag(playerId, "normanmarvel_picklocation_complete");
                data.clearPlayerTag(playerId, "normanmarvel_generate");
                data.setActionData(playerId, "normanmarvel_villagepos", x + "/" + y + "/" + z);
                LOGGER.info("Marvel village spawned at {},{},{} for player {}", new Object[]{x, y, z, playerId});
                ServerPlayer player = overworld.getServer().getPlayerList().getPlayer(playerId);
                if (player != null) {
                    player.sendSystemMessage((Component)Component.translatable((String)"actions.normanmarvel_generated"));
                }
                return;
            }
            LOGGER.warn("Failed to spawn marvel village at {},{},{}: {}", new Object[]{x, y, z, failure.getString()});
            data.clearPlayerTag(playerId, "normanmarvel_picklocation_complete");
            data.clearPlayerTag(playerId, "normanmarvel_generate");
            ServerPlayer player = overworld.getServer().getPlayerList().getPlayer(playerId);
            if (player != null) {
                player.sendSystemMessage((Component)Component.translatable((String)"actions.normanmarvel_notgenerated"));
            }
        }
        catch (NumberFormatException e) {
            LOGGER.warn("Invalid coordinates in normanmarvel_location '{}' for player {}", (Object)locationStr, (Object)playerId);
            data.clearPlayerTag(playerId, "normanmarvel_generate");
        }
    }
}

