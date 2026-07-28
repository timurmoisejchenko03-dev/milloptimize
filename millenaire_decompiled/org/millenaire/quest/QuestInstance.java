/*
 * Decompiled with CFR 0.150.
 * 
 * Could not load the following classes:
 *  com.mojang.logging.LogUtils
 *  javax.annotation.Nullable
 *  net.minecraft.ChatFormatting
 *  net.minecraft.core.BlockPos
 *  net.minecraft.nbt.CompoundTag
 *  net.minecraft.nbt.ListTag
 *  net.minecraft.nbt.Tag
 *  net.minecraft.network.chat.Component
 *  net.minecraft.network.protocol.common.custom.CustomPacketPayload
 *  net.minecraft.server.level.ServerLevel
 *  net.minecraft.server.level.ServerPlayer
 *  net.minecraft.world.entity.Entity
 *  net.minecraft.world.entity.ExperienceOrb
 *  net.minecraft.world.entity.item.ItemEntity
 *  net.minecraft.world.entity.player.Player
 *  net.minecraft.world.item.Item
 *  net.minecraft.world.item.ItemStack
 *  net.minecraft.world.level.ItemLike
 *  net.minecraft.world.level.Level
 *  net.neoforged.neoforge.network.PacketDistributor
 *  org.slf4j.Logger
 */
package org.millenaire.quest;

import com.mojang.logging.LogUtils;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Function;
import javax.annotation.Nullable;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.ExperienceOrb;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ItemLike;
import net.minecraft.world.level.Level;
import net.neoforged.neoforge.network.PacketDistributor;
import org.millenaire.advancement.MillAdvancements;
import org.millenaire.entity.MillVillager;
import org.millenaire.item.ItemHelper;
import org.millenaire.item.MoneyHelper;
import org.millenaire.network.QuestInstanceDestroyPayload;
import org.millenaire.network.QuestNetworkHelper;
import org.millenaire.network.QuestResultTextPayload;
import org.millenaire.quest.ActionDataEntry;
import org.millenaire.quest.Quest;
import org.millenaire.quest.QuestInstanceVillager;
import org.millenaire.quest.QuestItemRef;
import org.millenaire.quest.QuestRegistry;
import org.millenaire.quest.QuestStep;
import org.millenaire.quest.QuestTextRenderer;
import org.millenaire.quest.RelationChange;
import org.millenaire.quest.VillagerTagAction;
import org.millenaire.village.PlayerCultureReputation;
import org.millenaire.village.PlayerQuestData;
import org.millenaire.village.Village;
import org.millenaire.village.VillageId;
import org.millenaire.village.VillageSavedData;
import org.millenaire.village.VillagerRecord;
import org.slf4j.Logger;

public class QuestInstance {
    private static final Logger LOGGER = LogUtils.getLogger();
    private static final int QUEST_LANGUAGE_BONUS = 50;
    private final Quest quest;
    private final UUID playerId;
    private final Map<String, QuestInstanceVillager> villagers;
    private int currentStep;
    private final long startTime;
    private long currentStepStart;
    private final long uniqueId;

    public QuestInstance(Quest quest, UUID playerId, Map<String, QuestInstanceVillager> villagers, int currentStep, long startTime, long currentStepStart, long uniqueId) {
        this.quest = quest;
        this.playerId = playerId;
        this.villagers = new HashMap<String, QuestInstanceVillager>(villagers);
        this.currentStep = currentStep;
        this.startTime = startTime;
        this.currentStepStart = currentStepStart;
        this.uniqueId = uniqueId;
    }

    public Quest getQuest() {
        return this.quest;
    }

    public UUID getPlayerId() {
        return this.playerId;
    }

    public Map<String, QuestInstanceVillager> getVillagers() {
        return this.villagers;
    }

    public int getCurrentStepIndex() {
        return this.currentStep;
    }

    public long getStartTime() {
        return this.startTime;
    }

    public long getCurrentStepStart() {
        return this.currentStepStart;
    }

    public void setCurrentStepStart(long currentStepStart) {
        this.currentStepStart = currentStepStart;
    }

    public long getUniqueId() {
        return this.uniqueId;
    }

    @Nullable
    public QuestStep getCurrentStep() {
        if (this.currentStep >= 0 && this.currentStep < this.quest.steps().size()) {
            return this.quest.steps().get(this.currentStep);
        }
        return null;
    }

    @Nullable
    public UUID getCurrentStepVillagerId() {
        QuestStep step = this.getCurrentStep();
        if (step == null) {
            return null;
        }
        QuestInstanceVillager qiv = this.villagers.get(step.villagerKey());
        return qiv != null ? qiv.getVillagerId() : null;
    }

    public String completeStep(ServerPlayer player, MillVillager villager) {
        Village village;
        QuestInstanceVillager questInstanceVillager;
        Item item;
        int count;
        QuestItemRef ref;
        QuestStep step = this.getCurrentStep();
        if (step == null) {
            return "";
        }
        UUID expectedVillagerId = this.getCurrentStepVillagerId();
        if (expectedVillagerId == null || !expectedVillagerId.equals(villager.getUUID())) {
            LOGGER.warn("completeStep called with wrong villager {} (expected {})", (Object)villager.getUUID(), (Object)expectedVillagerId);
            return "";
        }
        ServerLevel level = (ServerLevel)villager.level();
        ServerLevel overworld = level.getServer().getLevel(Level.OVERWORLD);
        if (overworld == null) {
            return "";
        }
        PlayerQuestData questData = PlayerQuestData.get(overworld, QuestRegistry::get);
        VillageSavedData savedDataCheck = VillageSavedData.get(overworld);
        for (String string : step.stepRequiredPlayerTags()) {
            if (questData.hasPlayerTag(this.playerId, string)) continue;
            LOGGER.debug("Step prerequisite not met: player tag '{}' required", (Object)string);
            return "";
        }
        for (String string : step.stepForbiddenPlayerTags()) {
            if (!questData.hasPlayerTag(this.playerId, string)) continue;
            LOGGER.debug("Step prerequisite not met: player tag '{}' forbidden", (Object)string);
            return "";
        }
        for (String string : step.stepRequiredGlobalTags()) {
            if (savedDataCheck.isGlobalTagSet(string)) continue;
            LOGGER.debug("Step prerequisite not met: global tag '{}' required", (Object)string);
            return "";
        }
        for (String string : step.stepForbiddenGlobalTags()) {
            if (!savedDataCheck.isGlobalTagSet(string)) continue;
            LOGGER.debug("Step prerequisite not met: global tag '{}' forbidden", (Object)string);
            return "";
        }
        StringBuilder reward = new StringBuilder();
        for (Map.Entry<QuestItemRef, Integer> entry : step.requiredGoods().entrySet()) {
            ref = entry.getKey();
            count = entry.getValue();
            if (ref.meta() != 0 || (item = ItemHelper.resolve(ref.itemId())) == null) continue;
            villager.getInventory().add(item, count);
            QuestInstance.removeItemsFromPlayer(player, item, count);
        }
        for (Map.Entry<QuestItemRef, Integer> entry : step.rewardGoods().entrySet()) {
            ref = entry.getKey();
            count = entry.getValue();
            item = ItemHelper.resolve(ref.itemId());
            if (item == null) continue;
            ItemStack stack = new ItemStack((ItemLike)item, count);
            if (!player.getInventory().add(stack) && !stack.isEmpty()) {
                ItemEntity entityItem = new ItemEntity((Level)level, villager.getX(), villager.getY() + 0.5, villager.getZ(), stack);
                level.addFreshEntity((Entity)entityItem);
            }
            if (reward.length() > 0) {
                reward.append(", ");
            }
            reward.append(count).append(" ").append(item.getDescription().getString());
        }
        if (step.rewardMoney() > 0) {
            MoneyHelper.addDeniers(player.getInventory(), step.rewardMoney(), (Player)player);
            if (reward.length() > 0) {
                reward.append(", ");
            }
            reward.append(step.rewardMoney()).append(" ").append(Component.translatable((String)"gui.millenaire.quest.reward_deniers").getString());
        }
        if (step.rewardReputation() > 0) {
            Village village2;
            QuestInstanceVillager questInstanceVillager2 = this.villagers.get(step.villagerKey());
            if (questInstanceVillager2 != null && (village2 = Village.resolve(overworld, new VillageId(questInstanceVillager2.getVillageId()))) != null) {
                village2.adjustReputation(overworld, this.playerId, step.rewardReputation());
            }
            if (reward.length() > 0) {
                reward.append(", ");
            }
            reward.append(step.rewardReputation()).append(" ").append(Component.translatable((String)"gui.millenaire.quest.reward_reputation").getString());
            int experience = Math.min(step.rewardReputation() / 32, 16);
            if (experience > 0) {
                reward.append(", ").append(experience).append(" ").append(Component.translatable((String)"gui.millenaire.quest.reward_experience").getString());
                BlockPos xpPos = villager.blockPosition().above(2);
                player.level().addFreshEntity((Entity)new ExperienceOrb((Level)level, (double)xpPos.getX() + 0.5, (double)xpPos.getY(), (double)xpPos.getZ() + 0.5, experience));
            }
        }
        if ((questInstanceVillager = this.villagers.get(step.villagerKey())) != null && (village = Village.resolve(overworld, new VillageId(questInstanceVillager.getVillageId()))) != null) {
            PlayerCultureReputation.get(overworld).addLanguageKnowledge(this.playerId, village.getCultureId(), 50);
        }
        VillageSavedData savedData = VillageSavedData.get(overworld);
        this.applyVillagerTags(step.villagerTagsSuccess(), step.clearTagsSuccess(), overworld, savedData);
        this.applyGlobalTags(step.globalTagsSuccess(), step.clearGlobalTagsSuccess(), savedData);
        this.applyPlayerTags(step.playerTagsSuccess(), step.clearPlayerTagsSuccess(), PlayerQuestData.get(overworld, QuestRegistry::get));
        this.applyActionData(step.actionDataSuccess(), PlayerQuestData.get(overworld, QuestRegistry::get));
        this.applyRelationChanges(step.relationChanges(), overworld, savedData);
        String playerName = player.getName().getString();
        String locale = QuestTextRenderer.playerLocale(player);
        String successKey = this.quest.key() + "_" + this.currentStep + "_description_success";
        String inlineSuccess = step.descriptionsSuccess().getOrDefault(locale, step.descriptionsSuccess().getOrDefault("en", ""));
        Object res = QuestTextRenderer.lookupText(successKey, locale, inlineSuccess);
        res = QuestTextRenderer.substitute((String)res, this, playerName, overworld);
        if (reward.length() > 0) {
            if (!((String)res).isEmpty()) {
                res = (String)res + "<ret><ret>";
            }
            String rewardLabel = Component.translatable((String)"gui.millenaire.quest.reward_label").getString();
            res = (String)res + rewardLabel + " " + reward.toString();
        }
        ++this.currentStep;
        if (this.currentStep >= this.quest.steps().size()) {
            MillAdvancements.grant(player, MillAdvancements.THE_QUEST);
            this.destroyQuest(PlayerQuestData.get(overworld, QuestRegistry::get), this.playerId);
            PacketDistributor.sendToPlayer((ServerPlayer)player, (CustomPacketPayload)new QuestInstanceDestroyPayload(this.uniqueId), (CustomPacketPayload[])new CustomPacketPayload[0]);
        } else {
            this.currentStepStart = level.getDayTime();
            questData.setDirty();
            PacketDistributor.sendToPlayer((ServerPlayer)player, (CustomPacketPayload)QuestNetworkHelper.buildSyncPayload(this, player), (CustomPacketPayload[])new CustomPacketPayload[0]);
        }
        PacketDistributor.sendToPlayer((ServerPlayer)player, (CustomPacketPayload)new QuestResultTextPayload(this.uniqueId, (String)res, true), (CustomPacketPayload[])new CustomPacketPayload[0]);
        return res;
    }

    public String refuseQuest(ServerPlayer player) {
        Village village;
        QuestInstanceVillager currentQiv;
        QuestStep step = this.getCurrentStep();
        if (step == null) {
            return "";
        }
        ServerLevel level = (ServerLevel)player.level();
        ServerLevel overworld = level.getServer().getLevel(Level.OVERWORLD);
        if (overworld == null) {
            return "";
        }
        Object repLost = "";
        if (step.penaltyReputation() > 0 && (currentQiv = this.villagers.get(step.villagerKey())) != null && (village = Village.resolve(overworld, new VillageId(currentQiv.getVillageId()))) != null) {
            village.adjustReputation(overworld, this.playerId, -step.penaltyReputation());
            repLost = " (Reputation lost: " + step.penaltyReputation() + ")";
        }
        VillageSavedData savedData = VillageSavedData.get(overworld);
        this.applyVillagerTags(step.villagerTagsFailure(), step.clearTagsFailure(), overworld, savedData);
        this.applyGlobalTags(step.globalTagsFailure(), step.clearGlobalTagsFailure(), savedData);
        this.applyPlayerTags(step.playerTagsFailure(), step.clearPlayerTagsFailure(), PlayerQuestData.get(overworld, QuestRegistry::get));
        String playerName = player.getName().getString();
        String locale = QuestTextRenderer.playerLocale(player);
        String refuseKey = this.quest.key() + "_" + this.currentStep + "_description_refuse";
        String inlineRefuse = step.descriptionsRefuse().getOrDefault(locale, step.descriptionsRefuse().getOrDefault("en", ""));
        Object text = QuestTextRenderer.lookupText(refuseKey, locale, inlineRefuse);
        text = QuestTextRenderer.substitute((String)text, this, playerName, overworld);
        if (!((String)repLost).isEmpty()) {
            if (!((String)text).isEmpty()) {
                text = (String)text + "\n";
            }
            text = (String)text + (String)repLost;
        }
        this.destroyQuest(PlayerQuestData.get(overworld, QuestRegistry::get), this.playerId);
        PacketDistributor.sendToPlayer((ServerPlayer)player, (CustomPacketPayload)new QuestInstanceDestroyPayload(this.uniqueId), (CustomPacketPayload[])new CustomPacketPayload[0]);
        PacketDistributor.sendToPlayer((ServerPlayer)player, (CustomPacketPayload)new QuestResultTextPayload(this.uniqueId, (String)text, false), (CustomPacketPayload[])new CustomPacketPayload[0]);
        return text;
    }

    public void checkStatus(long worldTime, ServerPlayer player, PlayerQuestData questData) {
        Village village;
        QuestInstanceVillager currentQiv;
        QuestStep step = this.getCurrentStep();
        if (step == null) {
            return;
        }
        if (this.currentStepStart + (long)step.duration() * 1000L > worldTime) {
            return;
        }
        ServerLevel level = (ServerLevel)player.level();
        ServerLevel overworld = level.getServer().getLevel(Level.OVERWORLD);
        if (overworld == null) {
            return;
        }
        for (QuestInstanceVillager qiv : this.villagers.values()) {
            Village village2 = Village.resolve(overworld, new VillageId(qiv.getVillageId()));
            if (village2 == null) {
                LOGGER.debug("Dropping quest as village {} is null", (Object)qiv.getVillageId());
                this.destroyQuest(questData, this.playerId);
                PacketDistributor.sendToPlayer((ServerPlayer)player, (CustomPacketPayload)new QuestInstanceDestroyPayload(this.uniqueId), (CustomPacketPayload[])new CustomPacketPayload[0]);
                return;
            }
            VillagerRecord vr = village2.getVillagerRecord(qiv.getVillagerId());
            if (vr != null && !vr.isKilled()) continue;
            LOGGER.debug("Dropping quest as villager {} is dead or missing", (Object)qiv.getVillagerId());
            this.destroyQuest(questData, this.playerId);
            PacketDistributor.sendToPlayer((ServerPlayer)player, (CustomPacketPayload)new QuestInstanceDestroyPayload(this.uniqueId), (CustomPacketPayload[])new CustomPacketPayload[0]);
            return;
        }
        if (step.penaltyReputation() > 0 && (currentQiv = this.villagers.get(step.villagerKey())) != null && (village = Village.resolve(overworld, new VillageId(currentQiv.getVillageId()))) != null) {
            village.adjustReputation(overworld, this.playerId, -step.penaltyReputation());
        }
        VillageSavedData savedData = VillageSavedData.get(overworld);
        this.applyVillagerTags(step.villagerTagsFailure(), step.clearTagsFailure(), overworld, savedData);
        this.applyGlobalTags(step.globalTagsFailure(), step.clearGlobalTagsFailure(), savedData);
        this.applyPlayerTags(step.playerTagsFailure(), step.clearPlayerTagsFailure(), questData);
        String playerName = player.getName().getString();
        String locale = QuestTextRenderer.playerLocale(player);
        String timeupKey = this.quest.key() + "_" + this.currentStep + "_description_timeup";
        String inlineTimeup = step.descriptionsTimeUp().getOrDefault(locale, step.descriptionsTimeUp().getOrDefault("en", ""));
        Object timeupText = QuestTextRenderer.lookupText(timeupKey, locale, inlineTimeup);
        timeupText = QuestTextRenderer.substitute((String)timeupText, this, playerName, overworld);
        if (((String)timeupText).isEmpty() && step.penaltyReputation() > 0) {
            timeupText = Component.translatable((String)"gui.millenaire.quest.timeout", (Object[])new Object[]{step.penaltyReputation()}).getString();
        } else if (!((String)timeupText).isEmpty() && step.penaltyReputation() > 0) {
            timeupText = (String)timeupText + " (" + Component.translatable((String)"gui.millenaire.quest.rep_lost", (Object[])new Object[]{step.penaltyReputation()}).getString() + ")";
        }
        if (!((String)timeupText).isEmpty()) {
            player.sendSystemMessage((Component)Component.literal((String)timeupText).withStyle(ChatFormatting.RED));
        }
        this.destroyQuest(questData, this.playerId);
        PacketDistributor.sendToPlayer((ServerPlayer)player, (CustomPacketPayload)new QuestInstanceDestroyPayload(this.uniqueId), (CustomPacketPayload[])new CustomPacketPayload[0]);
    }

    public void destroyQuest(PlayerQuestData questData, UUID playerId) {
        questData.removeQuest(playerId, this);
    }

    private void applyVillagerTags(List<VillagerTagAction> setTags, List<VillagerTagAction> clearTags, ServerLevel overworld, VillageSavedData savedData) {
        VillagerRecord vr;
        Village village;
        QuestInstanceVillager qiv;
        String tag;
        for (VillagerTagAction vta : setTags) {
            tag = String.valueOf(this.playerId) + "_" + vta.tag();
            qiv = this.villagers.get(vta.villagerKey());
            if (qiv == null || (village = savedData.getVillageManager().getVillage(new VillageId(qiv.getVillageId()))) == null || (vr = village.getVillagerRecord(qiv.getVillagerId())) == null) continue;
            vr.addQuestTag(tag);
            savedData.setDirty();
            LOGGER.debug("Set quest tag '{}' on villager {}", (Object)tag, (Object)vr.getFirstName());
        }
        for (VillagerTagAction vta : clearTags) {
            tag = String.valueOf(this.playerId) + "_" + vta.tag();
            qiv = this.villagers.get(vta.villagerKey());
            if (qiv == null || (village = savedData.getVillageManager().getVillage(new VillageId(qiv.getVillageId()))) == null || (vr = village.getVillagerRecord(qiv.getVillagerId())) == null) continue;
            vr.removeQuestTag(tag);
            savedData.setDirty();
            LOGGER.debug("Cleared quest tag '{}' on villager {}", (Object)tag, (Object)vr.getFirstName());
        }
    }

    private void applyGlobalTags(List<String> setTags, List<String> clearTags, VillageSavedData savedData) {
        for (String tag : setTags) {
            savedData.setGlobalTag(tag);
        }
        for (String tag : clearTags) {
            savedData.clearGlobalTag(tag);
        }
    }

    private void applyPlayerTags(List<String> setTags, List<String> clearTags, PlayerQuestData questData) {
        for (String tag : setTags) {
            questData.setPlayerTag(this.playerId, tag);
        }
        for (String tag : clearTags) {
            questData.clearPlayerTag(this.playerId, tag);
        }
    }

    private void applyActionData(List<ActionDataEntry> entries, PlayerQuestData questData) {
        for (ActionDataEntry entry : entries) {
            questData.setActionData(this.playerId, entry.key(), entry.value());
        }
    }

    private void applyRelationChanges(List<RelationChange> changes, ServerLevel overworld, VillageSavedData savedData) {
        for (RelationChange change : changes) {
            QuestInstanceVillager qiv1 = this.villagers.get(change.firstVillager());
            QuestInstanceVillager qiv2 = this.villagers.get(change.secondVillager());
            if (qiv1 == null) {
                LOGGER.error("Unknown villager reference in relation change: {}", (Object)change.firstVillager());
                continue;
            }
            if (qiv2 == null) {
                LOGGER.error("Unknown villager reference in relation change: {}", (Object)change.secondVillager());
                continue;
            }
            Village village1 = savedData.getVillageManager().getVillage(new VillageId(qiv1.getVillageId()));
            VillageId villageId2 = new VillageId(qiv2.getVillageId());
            if (village1 == null) continue;
            village1.adjustRelationSymmetric(overworld, villageId2, change.change(), false);
            LOGGER.debug("Adjusted relation (symmetric) between {} and {} by {}", new Object[]{village1.getVillageName(), villageId2, change.change()});
        }
    }

    private static void removeItemsFromPlayer(ServerPlayer player, Item item, int count) {
        int remaining = count;
        for (int i = 0; i < player.getInventory().getContainerSize(); ++i) {
            ItemStack stack = player.getInventory().getItem(i);
            if (!stack.is(item)) continue;
            int toRemove = Math.min(remaining, stack.getCount());
            stack.shrink(toRemove);
            if ((remaining -= toRemove) <= 0) break;
        }
    }

    public CompoundTag save() {
        CompoundTag tag = new CompoundTag();
        tag.putString("quest", this.quest.key());
        tag.putInt("step", this.currentStep);
        tag.putLong("startTime", this.startTime);
        tag.putLong("stepStart", this.currentStepStart);
        tag.putLong("uniqueId", this.uniqueId);
        ListTag villagersList = new ListTag();
        for (Map.Entry<String, QuestInstanceVillager> entry : this.villagers.entrySet()) {
            CompoundTag villagerTag = entry.getValue().save();
            villagerTag.putString("key", entry.getKey());
            villagersList.add((Object)villagerTag);
        }
        tag.put("villagers", (Tag)villagersList);
        return tag;
    }

    @Nullable
    public static QuestInstance load(CompoundTag tag, UUID playerId, Function<String, Quest> questLookup) {
        String questKey = tag.getString("quest");
        Quest quest = questLookup.apply(questKey);
        if (quest == null) {
            LOGGER.warn("Unknown quest type '{}' in saved data \u2014 quest instance dropped", (Object)questKey);
            return null;
        }
        int step = tag.getInt("step");
        if (step < 0 || step >= quest.steps().size()) {
            LOGGER.warn("Quest '{}' has out-of-range step {} (max {}) \u2014 quest instance dropped", new Object[]{questKey, step, quest.steps().size() - 1});
            return null;
        }
        long startTime = tag.getLong("startTime");
        long stepStart = tag.getLong("stepStart");
        long uniqueId = tag.getLong("uniqueId");
        HashMap<String, QuestInstanceVillager> villagers = new HashMap<String, QuestInstanceVillager>();
        ListTag villagersList = tag.getList("villagers", 10);
        for (int i = 0; i < villagersList.size(); ++i) {
            CompoundTag villagerTag = villagersList.getCompound(i);
            String key = villagerTag.getString("key");
            villagers.put(key, QuestInstanceVillager.load(villagerTag));
        }
        return new QuestInstance(quest, playerId, villagers, step, startTime, stepStart, uniqueId);
    }
}

