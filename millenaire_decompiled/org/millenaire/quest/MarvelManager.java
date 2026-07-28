/*
 * Decompiled with CFR 0.150.
 * 
 * Could not load the following classes:
 *  com.mojang.logging.LogUtils
 *  javax.annotation.Nullable
 *  net.minecraft.ChatFormatting
 *  net.minecraft.core.BlockPos
 *  net.minecraft.core.registries.BuiltInRegistries
 *  net.minecraft.nbt.CompoundTag
 *  net.minecraft.nbt.ListTag
 *  net.minecraft.nbt.Tag
 *  net.minecraft.network.chat.Component
 *  net.minecraft.network.chat.MutableComponent
 *  net.minecraft.resources.ResourceLocation
 *  net.minecraft.server.level.ServerLevel
 *  net.minecraft.server.level.ServerPlayer
 *  net.minecraft.world.effect.MobEffectInstance
 *  net.minecraft.world.effect.MobEffects
 *  net.minecraft.world.entity.Entity
 *  net.minecraft.world.item.Item
 *  net.minecraft.world.level.Level
 *  net.minecraft.world.phys.AABB
 *  org.slf4j.Logger
 */
package org.millenaire.quest;

import com.mojang.logging.LogUtils;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CopyOnWriteArrayList;
import javax.annotation.Nullable;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import org.millenaire.advancement.MillAdvancements;
import org.millenaire.building.BuildingInstance;
import org.millenaire.building.BuildingInventory;
import org.millenaire.building.BuildingPlanSet;
import org.millenaire.building.ConstructionTask;
import org.millenaire.commerce.TradeGood;
import org.millenaire.commerce.TradeGoodsLoader;
import org.millenaire.culture.ModCultures;
import org.millenaire.culture.VillageType;
import org.millenaire.entity.MillVillager;
import org.millenaire.quest.QuestRegistry;
import org.millenaire.village.PlayerQuestData;
import org.millenaire.village.Village;
import org.millenaire.village.VillageId;
import org.millenaire.village.VillageSavedData;
import org.slf4j.Logger;

public class MarvelManager {
    private static final Logger LOGGER = LogUtils.getLogger();
    public static final String NORMAN_MARVEL_COMPLETION_TAG = "normanmarvel_helper";
    private static final long DAWN_TIME = 23500L;
    private static final int COMPLETION_CHECK_INTERVAL = 200;
    private static final int LUCK_DURATION_TICKS = 12000;
    private static final int LUCK_AMPLIFIER = 1;
    private static final int BELL_RANGE = 128;
    private static final float DONATION_RATIO = 0.5f;
    private boolean marvelComplete;
    private final CopyOnWriteArrayList<String> donationList = new CopyOnWriteArrayList();
    private long lastDonationDay = -1L;
    private boolean nightActionDone = false;
    private boolean dawnActionDone = false;

    public static boolean isMarvelVillageType(Village village) {
        VillageType vType = ModCultures.getVillageType(village.getVillageTypeId());
        return vType != null && vType.isMarvel();
    }

    public void tick(Village village, ServerLevel level) {
        if ((level.getGameTime() + (long)village.hashCode()) % 200L == 120L) {
            this.testForCompletion(village, level);
        }
        this.updateNightAction(village, level);
        this.updateDawnAction(village, level);
    }

    private void testForCompletion(Village village, ServerLevel level) {
        BuildingInstance marvelBuilding;
        if (!this.marvelComplete && (marvelBuilding = MarvelManager.findBuildingWithTag(village, "marvel")) != null) {
            BuildingPlanSet planSet;
            BuildingPlanSet buildingPlanSet = planSet = marvelBuilding.getPlanSetId() != null ? ModCultures.getBuildingPlanSet(marvelBuilding.getPlanSetId()) : null;
            if (planSet != null) {
                String variant = marvelBuilding.getVariant() != null ? marvelBuilding.getVariant() : "default";
                int totalLevels = planSet.getLevelCount(variant);
                if (marvelBuilding.getLevel() + 1 >= totalLevels) {
                    this.marvelComplete = true;
                    village.markDirty();
                    MutableComponent message = Component.translatable((String)"marvel.norman.marvelbuilt").withStyle(ChatFormatting.BLUE);
                    for (ServerPlayer player : level.getServer().getPlayerList().getPlayers()) {
                        player.sendSystemMessage((Component)message);
                    }
                    LOGGER.info("Marvel village '{}' construction complete!", (Object)village.getVillageName());
                }
            }
        }
        if (this.marvelComplete) {
            PlayerQuestData questData = PlayerQuestData.get(level, QuestRegistry::get);
            for (ServerPlayer player : level.getServer().getPlayerList().getPlayers()) {
                if (!questData.hasPlayerTag(player.getUUID(), NORMAN_MARVEL_COMPLETION_TAG)) continue;
                MillAdvancements.grant(player, MillAdvancements.MARVEL_NORMAN);
            }
        }
    }

    @Nullable
    private static BuildingInstance findBuildingWithTag(Village village, String tag) {
        for (BuildingInstance building : village.getBuildings()) {
            BuildingPlanSet planSet;
            if (!building.isOperational() || (planSet = building.getPlanSetId() != null ? ModCultures.getBuildingPlanSet(building.getPlanSetId()) : null) == null || !planSet.hasTag(tag)) continue;
            return building;
        }
        return null;
    }

    private void updateNightAction(Village village, ServerLevel level) {
        if (level.isDay()) {
            this.nightActionDone = false;
            return;
        }
        if (this.nightActionDone) {
            return;
        }
        long currentDay = level.getDayTime() / 24000L;
        if (currentDay <= this.lastDonationDay) {
            this.nightActionDone = true;
            return;
        }
        if (!this.marvelComplete) {
            this.gatherDonationsFromVillages(village, level);
            this.lastDonationDay = currentDay;
        }
        this.nightActionDone = true;
    }

    private void gatherDonationsFromVillages(Village village, ServerLevel level) {
        Map<ResourceLocation, Integer> needs = this.computeRemainingNeeds(village, level);
        if (needs.isEmpty()) {
            return;
        }
        VillageSavedData savedData = VillageSavedData.get(level);
        for (Map.Entry<VillageId, Integer> entry : village.getRelations().entrySet()) {
            VillageType otherType;
            Village otherVillage;
            VillageId otherId = entry.getKey();
            int relation = entry.getValue();
            if (relation < 90 || (otherVillage = savedData.getVillageManager().getVillage(otherId)) == null || !otherVillage.getCultureId().equals((Object)village.getCultureId()) || (otherType = ModCultures.getVillageType(otherVillage.getVillageTypeId())) == null || !otherType.isRegularVillage() && !otherType.isHamlet()) continue;
            this.gatherDonationsFrom(village, otherVillage, needs, level);
        }
    }

    private void gatherDonationsFrom(Village marvelVillage, Village donorVillage, Map<ResourceLocation, Integer> needs, ServerLevel level) {
        BuildingInstance townHall = marvelVillage.getTownhall();
        if (townHall == null) {
            return;
        }
        BuildingInventory thInventory = townHall.getInventory();
        if (thInventory == null) {
            return;
        }
        List<TradeGood> tradeGoods = TradeGoodsLoader.getGoods(marvelVillage.getCultureId());
        StringBuilder donations = new StringBuilder();
        for (Map.Entry<ResourceLocation, Integer> needEntry : needs.entrySet()) {
            String tradeGoodKey;
            ResourceLocation neededItem = needEntry.getKey();
            int needed = needEntry.getValue();
            if (needed <= 0 || (tradeGoodKey = MarvelManager.findTradeGoodKey(tradeGoods, neededItem)) == null) continue;
            int gathered = 0;
            for (BuildingInstance donorBuilding : donorVillage.getBuildings()) {
                int capacity;
                int donated;
                if (!donorBuilding.isOperational() || (donated = (int)((float)(capacity = MarvelManager.getAbstractedProduction(donorBuilding, tradeGoodKey)) * 0.5f)) <= 0) continue;
                donated = Math.min(donated, needed - gathered);
                gathered += donated;
            }
            if (gathered <= 0) continue;
            Item item = BuiltInRegistries.ITEM.getOptional(neededItem).orElse(null);
            if (item != null) {
                thInventory.add((Level)level, item, gathered);
            }
            if (donations.length() > 0) {
                donations.append(";");
            }
            donations.append(tradeGoodKey).append("/").append(gathered);
        }
        if (donations.length() > 0) {
            String donationEntry = "donation;" + donorVillage.getVillageName() + ";" + String.valueOf(donations);
            this.donationList.add(donationEntry);
            marvelVillage.markDirty();
            LOGGER.debug("Marvel donation from '{}': {}", (Object)donorVillage.getVillageName(), (Object)donationEntry);
        }
    }

    private static int getAbstractedProduction(BuildingInstance building, String tradeGoodKey) {
        BuildingPlanSet planSet;
        BuildingPlanSet buildingPlanSet = planSet = building.getPlanSetId() != null ? ModCultures.getBuildingPlanSet(building.getPlanSetId()) : null;
        if (planSet == null) {
            return 0;
        }
        String variant = building.getVariant() != null ? building.getVariant() : "default";
        BuildingPlanSet.LevelDef levelDef = planSet.getLevel(variant, building.getLevel());
        if (levelDef == null) {
            return 0;
        }
        return levelDef.abstractedProduction().getOrDefault(tradeGoodKey, 0);
    }

    @Nullable
    private static String findTradeGoodKey(List<TradeGood> tradeGoods, ResourceLocation itemId) {
        if (tradeGoods == null) {
            return null;
        }
        for (TradeGood good : tradeGoods) {
            if (!good.itemLocation().equals((Object)itemId)) continue;
            return good.id();
        }
        return null;
    }

    private Map<ResourceLocation, Integer> computeRemainingNeeds(Village village, ServerLevel level) {
        BuildingInstance townHall;
        HashMap<ResourceLocation, Integer> needs = new HashMap<ResourceLocation, Integer>();
        HashMap<ResourceLocation, Integer> placedPlanSetCounts = new HashMap<ResourceLocation, Integer>();
        for (BuildingInstance b : village.getBuildings()) {
            if (b.getPlanSetId() == null) continue;
            placedPlanSetCounts.merge(b.getPlanSetId(), 1, Integer::sum);
        }
        for (BuildingInstance building : village.getBuildings()) {
            int startLevel;
            BuildingPlanSet planSet = building.getPlanSetId() != null ? ModCultures.getBuildingPlanSet(building.getPlanSetId()) : null;
            if (planSet == null) continue;
            String string = building.getVariant() != null ? building.getVariant() : "default";
            int currentLevel = building.getLevel();
            int totalLevels = planSet.getLevelCount(string);
            boolean needsCurrentLevel = building.isBeingBuilt() || building.getStatus() == BuildingInstance.Status.PLANNED;
            int n = startLevel = needsCurrentLevel ? currentLevel : currentLevel + 1;
            if (startLevel >= totalLevels) continue;
            HashSet<String> existingSubBuildings = new HashSet<String>();
            for (int lvl = 0; lvl < startLevel; ++lvl) {
                BuildingPlanSet.LevelDef completedDef = planSet.getLevel(string, lvl);
                if (completedDef == null) continue;
                existingSubBuildings.addAll(completedDef.subBuildings());
            }
            ArrayList<String> newSubBuildings = new ArrayList<String>();
            for (int lvl = startLevel; lvl < totalLevels; ++lvl) {
                BuildingPlanSet.LevelDef levelDef = planSet.getLevel(string, lvl);
                if (levelDef == null) continue;
                MarvelManager.addPlanCost(levelDef, needs);
                for (String subKey : levelDef.subBuildings()) {
                    if (newSubBuildings.contains(subKey) || existingSubBuildings.contains(subKey)) continue;
                    newSubBuildings.add(subKey);
                }
            }
            MarvelManager.addSubBuildingCosts(village, newSubBuildings, needs, placedPlanSetCounts);
        }
        VillageType vType = ModCultures.getVillageType(village.getVillageTypeId());
        if (vType != null) {
            HashMap<ResourceLocation, Integer> layoutCounts = new HashMap<ResourceLocation, Integer>();
            for (VillageType.LayoutSlot layoutSlot : vType.layout()) {
                layoutCounts.merge(layoutSlot.plan(), 1, Integer::sum);
            }
            for (Map.Entry entry : layoutCounts.entrySet()) {
                BuildingPlanSet planSet;
                int placedCount;
                ResourceLocation planId = (ResourceLocation)entry.getKey();
                int layoutCount = (Integer)entry.getValue();
                int unplacedCount = layoutCount - (placedCount = placedPlanSetCounts.getOrDefault((Object)planId, 0).intValue());
                if (unplacedCount <= 0 || (planSet = ModCultures.getBuildingPlanSet(planId)) == null) continue;
                String variant = planSet.variants().keySet().iterator().next();
                int totalLevels = planSet.getLevelCount(variant);
                ArrayList<String> allSubBuildings = new ArrayList<String>();
                for (int lvl = 0; lvl < totalLevels; ++lvl) {
                    BuildingPlanSet.LevelDef levelDef = planSet.getLevel(variant, lvl);
                    if (levelDef == null) continue;
                    MarvelManager.addPlanCostMultiplied(levelDef, needs, unplacedCount);
                    for (String subKey : levelDef.subBuildings()) {
                        if (allSubBuildings.contains(subKey)) continue;
                        allSubBuildings.add(subKey);
                    }
                }
                MarvelManager.addSubBuildingCosts(village, allSubBuildings, needs, placedPlanSetCounts, unplacedCount);
            }
        }
        if ((townHall = village.getTownhall()) != null && townHall.getInventory() != null) {
            for (Map.Entry entry : new HashMap(needs).entrySet()) {
                Item item = BuiltInRegistries.ITEM.getOptional((ResourceLocation)entry.getKey()).orElse(null);
                if (item == null) continue;
                int stock = townHall.getInventory().getCount((Level)level, item);
                needs.put((ResourceLocation)entry.getKey(), (Integer)needs.get(entry.getKey()) - stock);
            }
        }
        for (BuildingInstance buildingInstance : village.getBuildings()) {
            Entity builderEntity;
            ConstructionTask task;
            if (!buildingInstance.isBeingBuilt() || (task = buildingInstance.getConstructionTask()) == null || task.getReservedBuilder() == null || !((builderEntity = level.getEntity(task.getReservedBuilder())) instanceof MillVillager)) continue;
            MillVillager builder = (MillVillager)builderEntity;
            for (Map.Entry<ResourceLocation, Integer> entry : new HashMap<ResourceLocation, Integer>(needs).entrySet()) {
                Item item = BuiltInRegistries.ITEM.getOptional(entry.getKey()).orElse(null);
                if (item == null) continue;
                int carried = builder.getInventory().getCount(item);
                needs.put(entry.getKey(), (Integer)needs.get((Object)entry.getKey()) - carried);
            }
        }
        needs.entrySet().removeIf(e -> (Integer)e.getValue() <= 0);
        return needs;
    }

    private static void addPlanCost(BuildingPlanSet.LevelDef levelDef, Map<ResourceLocation, Integer> needs) {
        MarvelManager.addPlanCostMultiplied(levelDef, needs, 1);
    }

    private static void addPlanCostMultiplied(BuildingPlanSet.LevelDef levelDef, Map<ResourceLocation, Integer> needs, int multiplier) {
        for (Map.Entry<ResourceLocation, Integer> resEntry : levelDef.requiredResources().entrySet()) {
            needs.merge(resEntry.getKey(), resEntry.getValue() * multiplier, Integer::sum);
        }
    }

    private static void addSubBuildingCosts(Village village, List<String> subBuildingKeys, Map<ResourceLocation, Integer> needs, Map<ResourceLocation, Integer> placedPlanSetCounts) {
        MarvelManager.addSubBuildingCosts(village, subBuildingKeys, needs, placedPlanSetCounts, 1);
    }

    private static void addSubBuildingCosts(Village village, List<String> subBuildingKeys, Map<ResourceLocation, Integer> needs, Map<ResourceLocation, Integer> placedPlanSetCounts, int multiplier) {
        for (String subKey : subBuildingKeys) {
            BuildingPlanSet subPlanSet;
            ResourceLocation subPlanSetId = ResourceLocation.fromNamespaceAndPath((String)"millenaire", (String)(village.getCultureId().getPath() + "/" + subKey.toLowerCase()));
            if (placedPlanSetCounts.containsKey((Object)subPlanSetId) || (subPlanSet = ModCultures.getBuildingPlanSet(subPlanSetId)) == null) continue;
            String subVariant = subPlanSet.variants().keySet().iterator().next();
            int subTotalLevels = subPlanSet.getLevelCount(subVariant);
            for (int lvl = 0; lvl < subTotalLevels; ++lvl) {
                BuildingPlanSet.LevelDef subLevelDef = subPlanSet.getLevel(subVariant, lvl);
                if (subLevelDef == null) continue;
                MarvelManager.addPlanCostMultiplied(subLevelDef, needs, multiplier);
            }
        }
    }

    private void updateDawnAction(Village village, ServerLevel level) {
        boolean isDawn;
        long timeOfDay = level.getDayTime() % 24000L;
        boolean bl = isDawn = timeOfDay > 23500L;
        if (!isDawn) {
            this.dawnActionDone = false;
            return;
        }
        if (this.dawnActionDone) {
            return;
        }
        if (this.marvelComplete) {
            this.ringMorningBells(village, level);
        }
        this.dawnActionDone = true;
    }

    private void ringMorningBells(Village village, ServerLevel level) {
        BuildingInstance marvelBuilding = MarvelManager.findBuildingWithTag(village, "marvel");
        BlockPos bellPos = marvelBuilding != null ? marvelBuilding.getOrigin() : village.getCenter();
        LOGGER.info("Norman bells ring at marvel village '{}'!", (Object)village.getVillageName());
        AABB bellArea = new AABB(bellPos).inflate(128.0);
        List nearbyPlayers = level.getEntitiesOfClass(ServerPlayer.class, bellArea);
        for (ServerPlayer player : nearbyPlayers) {
            player.addEffect(new MobEffectInstance(MobEffects.LUCK, 12000, 1, true, true));
            player.sendSystemMessage((Component)Component.translatable((String)"marvel.norman.morningbells", (Object[])new Object[]{village.getVillageName()}));
        }
    }

    public boolean isMarvelComplete() {
        return this.marvelComplete;
    }

    public List<String> getDonationList() {
        return this.donationList;
    }

    public Map<ResourceLocation, Integer> getRemainingNeeds(Village village, ServerLevel level) {
        return this.computeRemainingNeeds(village, level);
    }

    public Map<ResourceLocation, Integer> getTotalNeeds(Village village) {
        HashMap<ResourceLocation, Integer> needs = new HashMap<ResourceLocation, Integer>();
        HashMap<ResourceLocation, Integer> placedPlanSetCounts = new HashMap<ResourceLocation, Integer>();
        for (BuildingInstance b : village.getBuildings()) {
            if (b.getPlanSetId() == null) continue;
            placedPlanSetCounts.merge(b.getPlanSetId(), 1, Integer::sum);
        }
        for (BuildingInstance building : village.getBuildings()) {
            BuildingPlanSet planSet = building.getPlanSetId() != null ? ModCultures.getBuildingPlanSet(building.getPlanSetId()) : null;
            if (planSet == null) continue;
            String string = building.getVariant() != null ? building.getVariant() : "default";
            int totalLevels = planSet.getLevelCount(string);
            ArrayList<String> allSubBuildings = new ArrayList<String>();
            for (int lvl = 0; lvl < totalLevels; ++lvl) {
                BuildingPlanSet.LevelDef levelDef = planSet.getLevel(string, lvl);
                if (levelDef == null) continue;
                MarvelManager.addPlanCost(levelDef, needs);
                for (String subKey : levelDef.subBuildings()) {
                    if (allSubBuildings.contains(subKey)) continue;
                    allSubBuildings.add(subKey);
                }
            }
            MarvelManager.addSubBuildingCosts(village, allSubBuildings, needs, placedPlanSetCounts);
        }
        VillageType vType = ModCultures.getVillageType(village.getVillageTypeId());
        if (vType != null) {
            HashMap<ResourceLocation, Integer> layoutCounts = new HashMap<ResourceLocation, Integer>();
            for (VillageType.LayoutSlot layoutSlot : vType.layout()) {
                layoutCounts.merge(layoutSlot.plan(), 1, Integer::sum);
            }
            for (Map.Entry entry : layoutCounts.entrySet()) {
                BuildingPlanSet planSet;
                int placedCount;
                ResourceLocation planId = (ResourceLocation)entry.getKey();
                int layoutCount = (Integer)entry.getValue();
                int unplacedCount = layoutCount - (placedCount = placedPlanSetCounts.getOrDefault((Object)planId, 0).intValue());
                if (unplacedCount <= 0 || (planSet = ModCultures.getBuildingPlanSet(planId)) == null) continue;
                String variant = planSet.variants().keySet().iterator().next();
                int totalLevels = planSet.getLevelCount(variant);
                ArrayList<String> allSubBuildings = new ArrayList<String>();
                for (int lvl = 0; lvl < totalLevels; ++lvl) {
                    BuildingPlanSet.LevelDef levelDef = planSet.getLevel(variant, lvl);
                    if (levelDef == null) continue;
                    MarvelManager.addPlanCostMultiplied(levelDef, needs, unplacedCount);
                    for (String subKey : levelDef.subBuildings()) {
                        if (allSubBuildings.contains(subKey)) continue;
                        allSubBuildings.add(subKey);
                    }
                }
                MarvelManager.addSubBuildingCosts(village, allSubBuildings, needs, placedPlanSetCounts, unplacedCount);
            }
        }
        return needs;
    }

    public CompoundTag save() {
        CompoundTag tag = new CompoundTag();
        tag.putBoolean("marvelComplete", this.marvelComplete);
        tag.putLong("lastDonationDay", this.lastDonationDay);
        ListTag donationListTag = new ListTag();
        for (String s : this.donationList) {
            CompoundTag entry = new CompoundTag();
            entry.putString("donation", s);
            donationListTag.add((Object)entry);
        }
        tag.put("marvelDonationList", (Tag)donationListTag);
        return tag;
    }

    public void load(CompoundTag tag) {
        this.marvelComplete = tag.getBoolean("marvelComplete");
        this.lastDonationDay = tag.getLong("lastDonationDay");
        if (tag.contains("marvelDonationList")) {
            ListTag donationListTag = tag.getList("marvelDonationList", 10);
            this.donationList.clear();
            for (int i = 0; i < donationListTag.size(); ++i) {
                this.donationList.add(donationListTag.getCompound(i).getString("donation"));
            }
        }
    }
}

