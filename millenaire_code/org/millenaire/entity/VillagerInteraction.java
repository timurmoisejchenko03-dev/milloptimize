/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.mojang.logging.LogUtils
 *  javax.annotation.Nullable
 *  net.minecraft.core.BlockPos
 *  net.minecraft.core.Vec3i
 *  net.minecraft.core.registries.BuiltInRegistries
 *  net.minecraft.network.chat.Component
 *  net.minecraft.network.protocol.common.custom.CustomPacketPayload
 *  net.minecraft.resources.ResourceLocation
 *  net.minecraft.server.level.ServerLevel
 *  net.minecraft.server.level.ServerPlayer
 *  net.minecraft.world.MenuProvider
 *  net.minecraft.world.SimpleMenuProvider
 *  net.minecraft.world.item.Item
 *  net.minecraft.world.item.ItemStack
 *  net.minecraft.world.level.Level
 *  net.neoforged.neoforge.network.PacketDistributor
 *  org.slf4j.Logger
 */
package org.millenaire.entity;

import com.mojang.logging.LogUtils;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import javax.annotation.Nullable;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Vec3i;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.neoforged.neoforge.network.PacketDistributor;
import org.millenaire.advancement.MillAdvancements;
import org.millenaire.building.BuildingInstance;
import org.millenaire.building.BuildingPlan;
import org.millenaire.building.BuildingPlanSet;
import org.millenaire.building.SpecialPoint;
import org.millenaire.commerce.ShopProfile;
import org.millenaire.commerce.ShopProfileLoader;
import org.millenaire.commerce.TradeGood;
import org.millenaire.commerce.TradeGoodsLoader;
import org.millenaire.commerce.TradeMenu;
import org.millenaire.config.MillenaireServerConfig;
import org.millenaire.culture.Culture;
import org.millenaire.culture.ModCultures;
import org.millenaire.culture.ReputationLabel;
import org.millenaire.culture.VillageType;
import org.millenaire.culture.VillagerType;
import org.millenaire.entity.MillVillager;
import org.millenaire.goal.impl.SellerGoal;
import org.millenaire.hire.HiringHelper;
import org.millenaire.item.ItemHelper;
import org.millenaire.item.MoneyHelper;
import org.millenaire.language.BuildingNameHelper;
import org.millenaire.net.DonorStatusData;
import org.millenaire.network.OpenHireScreenPayload;
import org.millenaire.network.QuestOpenPayload;
import org.millenaire.network.VillageChiefPayload;
import org.millenaire.network.VillageChiefUpdatePayload;
import org.millenaire.network.VillagerInfoPayload;
import org.millenaire.quest.QuestInstance;
import org.millenaire.quest.QuestItemRef;
import org.millenaire.quest.QuestRegistry;
import org.millenaire.quest.QuestStep;
import org.millenaire.quest.QuestTextRenderer;
import org.millenaire.village.PlayerCultureReputation;
import org.millenaire.village.PlayerQuestData;
import org.millenaire.village.Village;
import org.millenaire.village.VillageId;
import org.millenaire.village.VillageManager;
import org.millenaire.village.VillageRelations;
import org.millenaire.village.VillageReputation;
import org.millenaire.village.VillageSavedData;
import org.millenaire.village.VillagerRecord;
import org.millenaire.village.panel.MilitaryPanelGenerator;
import org.millenaire.village.panel.PanelLine;
import org.slf4j.Logger;

public final class VillagerInteraction {
    private static final Logger LOGGER = LogUtils.getLogger();

    private VillagerInteraction() {
    }

    public static void openInfoScreen(ServerPlayer player, MillVillager villager) {
        PlayerQuestData questData;
        QuestInstance qi;
        ServerLevel overworld;
        String typePath;
        VillagerType vType = VillagerInteraction.resolveVillagerType(villager);
        MillAdvancements.grant(player, MillAdvancements.FIRST_CONTACT);
        if (vType != null && villager.getVillagerTypeId() != null && ("indian_sadhu".equals(typePath = villager.getVillagerTypeId().getPath()) || "alchemist".equals(typePath))) {
            MillAdvancements.grant(player, MillAdvancements.MAITRE_A_PENSER);
        }
        if ((overworld = player.server.getLevel(Level.OVERWORLD)) != null && (qi = (questData = PlayerQuestData.get(overworld, QuestRegistry::get)).getQuestForVillager(player.getUUID(), villager.getUUID())) != null) {
            UUID stepVillagerId = qi.getCurrentStepVillagerId();
            if (stepVillagerId != null && stepVillagerId.equals(villager.getUUID())) {
                boolean isActivelySelling;
                boolean bl = isActivelySelling = villager.getGoalScheduler() != null && SellerGoal.ID.equals((Object)villager.getGoalScheduler().getCurrentGoalId());
                if (!isActivelySelling) {
                    VillagerInteraction.sendQuestPayload(player, villager, qi, overworld);
                    return;
                }
                LOGGER.debug("BUG-226: villager {} is in SellerGoal \u2014 routing to trade instead of quest '{}'", (Object)villager.getUUID(), (Object)qi.getQuest().key());
            } else {
                LOGGER.debug("Villager {} is in quest '{}' but not current step target (expected={}, got={})", new Object[]{villager.getUUID(), qi.getQuest().key(), stepVillagerId, villager.getUUID()});
            }
        }
        if (vType != null && vType.hasTag("localmerchant")) {
            player.sendSystemMessage((Component)Component.translatable((String)"other.millenaire.localmerchantinteract", (Object[])new Object[]{villager.getVillagerDisplayName()}));
            return;
        }
        if (vType != null && vType.hasTag("chief")) {
            VillagerInteraction.sendChiefPayload(player, villager);
            return;
        }
        if (vType != null && vType.hasTag("foreignmerchant")) {
            VillagerInteraction.sendForeignMerchantTradePayload(player, villager);
            return;
        }
        if (vType != null && HiringHelper.isHireable(vType.hiringCost()) && (qi = villager.level()) instanceof ServerLevel) {
            boolean raiding;
            ServerLevel hireLevel = (ServerLevel)qi;
            Village hireVillage = villager.getVillageId() == null ? null : Village.resolve(hireLevel, villager.getVillageId());
            VillagerRecord record = hireVillage == null ? null : hireVillage.getVillagerRecord(villager.getUUID());
            boolean bl = raiding = record != null && record.isAwayRaiding();
            if (!raiding) {
                UUID owner = villager.getHiredBy();
                if (owner != null && !player.getUUID().equals(owner)) {
                    player.sendSystemMessage((Component)Component.translatable((String)"message.millenaire.hire.hiredbyotherplayer"));
                    return;
                }
                VillagerInteraction.sendHirePayload(player, villager);
                return;
            }
        }
        if (vType != null && vType.hasTag("seller")) {
            VillagerInteraction.sendTradePayload(player, villager);
        } else {
            VillagerInteraction.sendInfoPayload(player, villager);
        }
    }

    private static void sendInfoPayload(ServerPlayer player, MillVillager villager) {
        VillagerType vType;
        Object object;
        String goalLabel;
        Village village = VillagerInteraction.resolveVillage(villager);
        String villageName = VillagerInteraction.buildVillageName(villager, village);
        String cultureName = VillagerInteraction.resolveCultureName(villager);
        int reputation = 0;
        String reputationLabel = "Unknown";
        if (village != null) {
            Level level = villager.level();
            if (level instanceof ServerLevel) {
                ServerLevel serverLevel = (ServerLevel)level;
                reputation = village.getCombinedReputation(serverLevel, player.getUUID());
            }
            reputationLabel = VillagerInteraction.resolveReputationLabel(reputation, village.getCultureId());
        }
        if ((goalLabel = villager.getGoalLabel()) == null || goalLabel.isEmpty()) {
            goalLabel = "Inactive";
        }
        int languageScore = 0;
        if (!((Boolean)MillenaireServerConfig.SERVER.languageLearning.get()).booleanValue()) {
            languageScore = Integer.MAX_VALUE;
        } else if (village != null && (object = villager.level()) instanceof ServerLevel) {
            ServerLevel sl = (ServerLevel)object;
            languageScore = PlayerCultureReputation.get(sl).getLanguageKnowledge(player.getUUID(), village.getCultureId());
        }
        ArrayList<VillagerInfoPayload.InvEntry> invEntries = new ArrayList<VillagerInfoPayload.InvEntry>();
        for (Map.Entry entry : villager.getInventory().getAll().entrySet()) {
            String itemId = BuiltInRegistries.ITEM.getKey((Object)((Item)entry.getKey())).toString();
            invEntries.add(new VillagerInfoPayload.InvEntry(itemId, (Integer)entry.getValue()));
        }
        ArrayList<String> possibleGoals = new ArrayList<String>();
        ResourceLocation resourceLocation = villager.getVillagerTypeId();
        VillagerType villagerType = vType = resourceLocation != null ? ModCultures.getVillagerType(resourceLocation) : null;
        if (vType != null) {
            for (ResourceLocation goalId : vType.goals()) {
                possibleGoals.add("goal.millenaire." + goalId.getPath());
            }
        }
        TravelBookRef tbRef = VillagerInteraction.extractTravelBookRef(villager);
        boolean travelBookVisible = vType != null && vType.travelBookDisplay();
        String nativeOccupation = vType != null ? vType.nativeName() : "";
        VillagerInfoPayload payload = new VillagerInfoPayload(villager.getId(), villager.getVillagerDisplayName(), villager.getRoleName(), nativeOccupation, villageName, goalLabel, villager.getHealth(), villager.getMaxHealth(), reputation, reputationLabel, cultureName, languageScore, invEntries, possibleGoals, tbRef.cultureKey(), tbRef.villagerTypeKey(), travelBookVisible);
        PacketDistributor.sendToPlayer((ServerPlayer)player, (CustomPacketPayload)payload, (CustomPacketPayload[])new CustomPacketPayload[0]);
        LOGGER.debug("VillagerInfo payload sent to {} for villager {} ({})", new Object[]{player.getName().getString(), villager.getVillagerDisplayName(), villager.getRoleName()});
    }

    private static void sendChiefPayload(ServerPlayer player, MillVillager villager) {
        ServerLevel sl;
        Village village = VillagerInteraction.resolveVillage(villager);
        Level level = villager.level();
        ServerLevel level2 = level instanceof ServerLevel ? (sl = (ServerLevel)level) : null;
        VillageChiefPayload.ChiefDynamic dynamic = VillagerInteraction.buildChiefDynamic(player, village, level2);
        TravelBookRef chiefRef = VillagerInteraction.extractTravelBookRef(villager);
        VillageChiefPayload.ChiefIdentity identity = new VillageChiefPayload.ChiefIdentity(villager.getId(), villager.getVillagerDisplayName(), villager.getRoleName(), chiefRef.villagerTypeKey());
        PacketDistributor.sendToPlayer((ServerPlayer)player, (CustomPacketPayload)new VillageChiefPayload(identity, dynamic), (CustomPacketPayload[])new CustomPacketPayload[0]);
        LOGGER.debug("VillageChief payload sent to {} for chief {} (village {})", new Object[]{player.getName().getString(), villager.getVillagerDisplayName(), dynamic.villageName()});
    }

    public static void sendChiefRefresh(ServerPlayer player, ServerLevel level, String villageIdStr) {
        UUID uuid;
        try {
            uuid = UUID.fromString(villageIdStr);
        }
        catch (IllegalArgumentException e) {
            return;
        }
        Village village = Village.resolve(level, new VillageId(uuid));
        if (village == null) {
            return;
        }
        VillageChiefPayload.ChiefDynamic dynamic = VillagerInteraction.buildChiefDynamic(player, village, level);
        PacketDistributor.sendToPlayer((ServerPlayer)player, (CustomPacketPayload)new VillageChiefUpdatePayload(dynamic), (CustomPacketPayload[])new CustomPacketPayload[0]);
    }

    private static VillageChiefPayload.ChiefDynamic buildChiefDynamic(ServerPlayer player, @Nullable Village village, @Nullable ServerLevel level) {
        boolean bl;
        Culture culture;
        VillageType vType;
        String villageName = VillagerInteraction.villageDisplayName(village);
        String cultureName = VillagerInteraction.cultureDisplayName(village);
        int reputation = 0;
        String reputationLabel = "Unknown";
        int totalBuildings = 0;
        int completeBuildings = 0;
        int underConstructionBuildings = 0;
        int totalVillagers = 0;
        if (village != null) {
            if (level != null) {
                reputation = village.getCombinedReputation(level, player.getUUID());
            }
            reputationLabel = VillagerInteraction.resolveReputationLabel(reputation, village.getCultureId());
            totalVillagers = village.getVillagerUuids().size();
            List<BuildingInstance> buildings = village.getBuildings();
            totalBuildings = buildings.size();
            for (BuildingInstance b2 : buildings) {
                BuildingInstance.Status status = b2.getStatus();
                if (status == BuildingInstance.Status.COMPLETE) {
                    ++completeBuildings;
                    continue;
                }
                if (status != BuildingInstance.Status.UNDER_CONSTRUCTION && status != BuildingInstance.Status.UPGRADING) continue;
                ++underConstructionBuildings;
            }
        }
        String villageId = village != null ? village.getId().uuid().toString() : "";
        int playerMoney = MoneyHelper.getTotalDeniers(player.getInventory());
        int playerReputation = 0;
        if (village != null && level != null) {
            playerReputation = village.getCombinedReputation(level, player.getUUID());
        }
        boolean isDonor = level != null && DonorStatusData.get(level).isDonor(player.getUUID());
        ArrayList<VillageChiefPayload.PlayerBuildingEntry> playerBuildingEntries = new ArrayList<VillageChiefPayload.PlayerBuildingEntry>();
        if (village != null && (vType = ModCultures.getVillageType(village.getVillageTypeId())) != null) {
            for (ResourceLocation pbId : vType.playerBuildings()) {
                boolean bl2;
                BuildingPlanSet pbPlanSet = ModCultures.getBuildingPlanSet(pbId);
                if (pbPlanSet == null || (!(bl2 = pbPlanSet.isGift()) ? pbPlanSet.price() <= 0 : !isDonor)) continue;
                boolean alreadyBuilt = village.getBuildings().stream().anyMatch(b -> pbId.equals((Object)b.getPlanSetId()));
                String status = alreadyBuilt ? "BUILT" : (village.isBuildingBought(pbId) ? "BOUGHT" : (reputation < pbPlanSet.reputation() ? "NO_REP" : (bl2 ? "GIFT_AVAILABLE" : (playerMoney < pbPlanSet.price() ? "NO_MONEY" : "AVAILABLE"))));
                playerBuildingEntries.add(new VillageChiefPayload.PlayerBuildingEntry(pbId.toString(), pbPlanSet.nativeName(), pbPlanSet.price(), pbPlanSet.reputation(), status, BuildingNameHelper.getTranslationKey(pbPlanSet)));
            }
        }
        ArrayList<VillageChiefPayload.RelationEntry> relationEntries = new ArrayList<VillageChiefPayload.RelationEntry>();
        int diplomacyPts = 0;
        if (village != null && level != null) {
            VillageManager vm = VillageSavedData.get(level).getVillageManager();
            for (Map.Entry entry : village.getRelations().entrySet()) {
                Village otherVillage = vm.getVillage((VillageId)entry.getKey());
                if (otherVillage == null || village.getParentVillageId() != null && village.getParentVillageId().equals(otherVillage.getId()) || otherVillage.getParentVillageId() != null && otherVillage.getParentVillageId().equals(village.getId())) continue;
                String otherName = otherVillage.getVillageName() != null ? otherVillage.getVillageName() : otherVillage.getVillageTypeId().getPath();
                Culture otherCulture = ModCultures.getCulture(otherVillage.getCultureId());
                String otherCultureName = otherCulture != null ? otherCulture.displayName() : "?";
                int rel = (Integer)entry.getValue();
                String relLabel = VillageRelations.getRelationKey(rel);
                relationEntries.add(new VillageChiefPayload.RelationEntry(((VillageId)entry.getKey()).uuid().toString(), otherName, otherCultureName, rel, relLabel));
            }
            PlayerCultureReputation cultureRep = PlayerCultureReputation.get(level);
            diplomacyPts = cultureRep.getDiplomacyPoints(player.getUUID(), village.getId());
        }
        ArrayList<VillageChiefPayload.LearningOffer> cropOffers = new ArrayList<VillageChiefPayload.LearningOffer>();
        ArrayList<VillageChiefPayload.LearningOffer> huntingOffers = new ArrayList<VillageChiefPayload.LearningOffer>();
        boolean bl3 = false;
        boolean hasCultureControlFlag = false;
        String cultureIdStr = "";
        if (village != null && level != null && (culture = ModCultures.getCulture(village.getCultureId())) != null) {
            String translationKey;
            String status;
            cultureIdStr = culture.id().toString();
            PlayerCultureReputation cultureRep = PlayerCultureReputation.get(level);
            for (String cropKey : culture.knownCrops()) {
                status = cultureRep.hasLearnedCrop(player.getUUID(), cropKey) ? "LEARNED" : (reputation < 8192 ? "NO_REP" : (playerMoney < 512 ? "NO_MONEY" : "AVAILABLE"));
                translationKey = "millenaire.crop." + cropKey;
                cropOffers.add(new VillageChiefPayload.LearningOffer(cropKey, translationKey, status));
            }
            for (String dropKey : culture.knownHuntingDrops()) {
                status = cultureRep.hasLearnedHuntingDrop(player.getUUID(), dropKey) ? "LEARNED" : (reputation < 8192 ? "NO_REP" : (playerMoney < 512 ? "NO_MONEY" : "AVAILABLE"));
                translationKey = "millenaire.hunting." + dropKey;
                huntingOffers.add(new VillageChiefPayload.LearningOffer(dropKey, translationKey, status));
            }
            hasCultureControlFlag = cultureRep.hasCultureControl(player.getUUID(), culture.id());
            bl = !hasCultureControlFlag && reputation >= 131072;
        }
        boolean controlledByPlayer = village != null && village.getOwnerUUID() != null && village.getOwnerUUID().equals(player.getUUID());
        String currentRaidTargetVillageId = village != null && village.getRaidTarget() != null ? village.getRaidTarget().uuid().toString() : "";
        boolean raidInProgress = village != null && village.getRaidStart() > 0L;
        ArrayList<PanelLine> militaryLines = new ArrayList<PanelLine>();
        if (village != null && level != null) {
            militaryLines.addAll(MilitaryPanelGenerator.generateMilitary(village, level).lines());
        }
        return new VillageChiefPayload.ChiefDynamic(villageName, cultureName, reputation, reputationLabel, totalBuildings, completeBuildings, underConstructionBuildings, totalVillagers, villageId, playerMoney, playerReputation, playerBuildingEntries, relationEntries, diplomacyPts, cropOffers, huntingOffers, bl, hasCultureControlFlag, cultureIdStr, controlledByPlayer, currentRaidTargetVillageId, raidInProgress, militaryLines);
    }

    private static String villageDisplayName(@Nullable Village village) {
        if (village == null) {
            return "Unaffiliated";
        }
        if (village.getVillageName() != null) {
            return village.getVillageName();
        }
        return VillagerInteraction.cultureDisplayName(village);
    }

    private static String cultureDisplayName(@Nullable Village village) {
        if (village == null) {
            return "Unknown";
        }
        Culture culture = ModCultures.getCulture(village.getCultureId());
        if (culture != null) {
            return culture.displayName();
        }
        String cp = village.getCultureId().getPath();
        return cp.substring(0, 1).toUpperCase() + cp.substring(1);
    }

    private static void sendTradePayload(ServerPlayer player, MillVillager villager) {
        Village village = VillagerInteraction.resolveVillage(villager);
        if (village == null) {
            LOGGER.warn("No village for seller {} \u2014 fallback to info screen", (Object)villager.getVillagerTypeId());
            VillagerInteraction.sendInfoPayload(player, villager);
            return;
        }
        if (village.isControlledBy(player.getUUID())) {
            VillagerInteraction.sendInfoPayload(player, villager);
            return;
        }
        if (!village.areChestsLocked()) {
            player.sendSystemMessage((Component)Component.translatable((String)"message.millenaire.trade_not_possible"));
            return;
        }
        int totalRep = 0;
        Level level = villager.level();
        if (level instanceof ServerLevel) {
            ServerLevel sl = (ServerLevel)level;
            totalRep = village.getCombinedReputation(sl, player.getUUID());
        }
        if (totalRep < -1024) {
            player.sendSystemMessage((Component)Component.translatable((String)"message.millenaire.trade_boycott"));
            return;
        }
        BuildingInstance shopBuilding = null;
        BlockPos sellingPos = null;
        double bestDistSq = Double.MAX_VALUE;
        for (BuildingInstance b : village.getBuildings()) {
            BuildingPlan bPlan;
            if (!b.isOperational() || (bPlan = ModCultures.getBuildingPlan(b.getPlanId())) == null || bPlan.shopId() == null) continue;
            List<SpecialPoint> sellingPoints = b.getPointsByType("sellingPos");
            if (sellingPoints.isEmpty()) {
                double distSq;
                BlockPos sp = b.getFirstPointPos("sleepingPos");
                if (sp == null || !((distSq = villager.blockPosition().distSqr((Vec3i)sp)) < 25.0) || !(distSq < bestDistSq)) continue;
                bestDistSq = distSq;
                shopBuilding = b;
                sellingPos = sp;
                continue;
            }
            for (SpecialPoint point : sellingPoints) {
                double distSq = villager.blockPosition().distSqr((Vec3i)point.pos());
                if (!(distSq < 25.0) || !(distSq < bestDistSq)) continue;
                bestDistSq = distSq;
                shopBuilding = b;
                sellingPos = point.pos();
            }
        }
        if (shopBuilding == null) {
            LOGGER.warn("No shop within range of seller {} \u2014 fallback to info screen", (Object)villager.getVillagerTypeId());
            VillagerInteraction.sendInfoPayload(player, villager);
            return;
        }
        BuildingPlan plan = ModCultures.getBuildingPlan(shopBuilding.getPlanId());
        if (plan == null || plan.shopId() == null) {
            LOGGER.warn("No shopId for building {} \u2014 fallback to info screen", (Object)shopBuilding.getPlanId());
            VillagerInteraction.sendInfoPayload(player, villager);
            return;
        }
        String shopId = plan.shopId();
        ResourceLocation cultureId = village.getCultureId();
        ShopProfile shopProfile = ShopProfileLoader.getProfile(cultureId, shopId);
        if (shopProfile == null) {
            LOGGER.warn("No shop profile '{}' for culture {} \u2014 fallback to info screen", (Object)shopId, (Object)cultureId);
            VillagerInteraction.sendInfoPayload(player, villager);
            return;
        }
        List<TradeGood> tradeCatalog = TradeGoodsLoader.getGoods(cultureId);
        if (tradeCatalog.isEmpty()) {
            LOGGER.warn("No traded_goods for culture {} \u2014 fallback to info screen", (Object)cultureId);
            VillagerInteraction.sendInfoPayload(player, villager);
            return;
        }
        BuildingInstance finalShop = shopBuilding;
        ShopProfile finalProfile = shopProfile;
        List<TradeGood> finalCatalog = tradeCatalog;
        BlockPos finalSellingPos = sellingPos;
        TradeMenu previewMenu = new TradeMenu(0, player.getInventory(), village, finalShop, finalProfile, finalCatalog, finalSellingPos);
        player.openMenu((MenuProvider)new SimpleMenuProvider((containerId, playerInv, p) -> new TradeMenu(containerId, playerInv, village, finalShop, finalProfile, finalCatalog, finalSellingPos), (Component)Component.translatable((String)"container.millenaire.trade")), previewMenu::writeToBuffer);
        LOGGER.debug("TradeMenu opened for {} in shop {} (village {})", new Object[]{player.getName().getString(), shopId, village.getVillageName()});
    }

    private static void sendForeignMerchantTradePayload(ServerPlayer player, MillVillager villager) {
        ResourceLocation villageCulture;
        BuildingInstance market;
        Village village = VillagerInteraction.resolveVillage(villager);
        if (village == null) {
            LOGGER.warn("No village for foreign merchant {} \u2014 fallback to info screen", (Object)villager.getVillagerTypeId());
            VillagerInteraction.sendInfoPayload(player, villager);
            return;
        }
        BuildingInstance buildingInstance = market = villager.getHomeBuilding() != null ? village.getBuilding(villager.getHomeBuilding()) : null;
        if (market == null) {
            LOGGER.warn("No market building for foreign merchant {} \u2014 fallback to info screen", (Object)villager.getVillagerTypeId());
            VillagerInteraction.sendInfoPayload(player, villager);
            return;
        }
        VillagerType vType = VillagerInteraction.resolveVillagerType(villager);
        if (vType == null || vType.foreignMerchantStock().isEmpty()) {
            VillagerInteraction.sendInfoPayload(player, villager);
            return;
        }
        ResourceLocation merchantCulture = vType.culture();
        boolean crossCulture = !merchantCulture.equals((Object)(villageCulture = village.getCultureId()));
        ArrayList<TradeGood> merchantGoods = new ArrayList<TradeGood>();
        List<TradeGood> cultureCatalog = TradeGoodsLoader.getGoods(merchantCulture);
        for (Map.Entry<ResourceLocation, Integer> entry : vType.foreignMerchantStock().entrySet()) {
            ResourceLocation itemId = entry.getKey();
            String itemStr = itemId.toString();
            int foreignPrice = 0;
            String goodId = itemStr;
            for (TradeGood tg : cultureCatalog) {
                if (!tg.item().equals(itemStr) || tg.foreignMerchantPrice() <= 0) continue;
                foreignPrice = tg.foreignMerchantPrice();
                goodId = tg.id();
                break;
            }
            if (crossCulture && foreignPrice > 0) {
                foreignPrice = (int)((double)foreignPrice * 1.5);
            }
            if (foreignPrice <= 0) continue;
            merchantGoods.add(new TradeGood(goodId, itemStr, foreignPrice, 0, 0, entry.getValue(), false, 0, "merchant", true, foreignPrice));
        }
        if (merchantGoods.isEmpty()) {
            VillagerInteraction.sendInfoPayload(player, villager);
            return;
        }
        List<String> sellIds = merchantGoods.stream().map(TradeGood::id).toList();
        ShopProfile merchantProfile = new ShopProfile(sellIds, List.of(), List.of(), List.of());
        BlockPos finalStallPos = null;
        BuildingInstance finalMarket = market;
        ShopProfile finalProfile = merchantProfile;
        ArrayList<TradeGood> finalGoods = merchantGoods;
        TradeMenu previewMenu = new TradeMenu(0, player.getInventory(), village, finalMarket, finalProfile, finalGoods, finalStallPos);
        previewMenu.setMerchantCultureId(merchantCulture);
        player.openMenu((MenuProvider)new SimpleMenuProvider((containerId, playerInv, p) -> {
            TradeMenu menu = new TradeMenu(containerId, playerInv, village, finalMarket, finalProfile, finalGoods, finalStallPos);
            menu.setMerchantCultureId(merchantCulture);
            return menu;
        }, (Component)Component.translatable((String)"container.millenaire.trade")), previewMenu::writeToBuffer);
        LOGGER.debug("Foreign merchant trade opened for {} with {} (village {})", new Object[]{player.getName().getString(), villager.getVillagerTypeId(), village.getVillageName()});
    }

    public static void sendHirePayload(ServerPlayer player, MillVillager villager) {
        Level level = villager.level();
        if (!(level instanceof ServerLevel)) {
            return;
        }
        ServerLevel level2 = (ServerLevel)level;
        VillagerType vType = VillagerInteraction.resolveVillagerType(villager);
        if (vType == null) {
            return;
        }
        Village village = VillagerInteraction.resolveVillage(villager);
        boolean controlled = village != null && village.isControlledBy(player.getUUID());
        int cost = HiringHelper.hireCost(vType.hiringCost(), controlled);
        boolean hiredByMe = player.getUUID().equals(villager.getHiredBy());
        int reputation = village != null ? village.getCombinedReputation(level2, player.getUUID()) : 0;
        boolean hasReputation = reputation >= 4096;
        boolean canAfford = MoneyHelper.getTotalDeniers(player.getInventory()) >= cost;
        int hoursLeft = HiringHelper.hoursLeft(villager.getHiredUntil(), level2.getGameTime());
        String occupation = villager.getRoleName() != null ? villager.getRoleName() : "";
        OpenHireScreenPayload payload = new OpenHireScreenPayload(villager.getUUID(), villager.getVillagerDisplayName(), occupation, villager.getHealth() <= 0.0f ? 0 : (int)villager.getHealth(), (int)villager.getMaxHealth(), villager.getAttackStrength(), cost, hiredByMe, hoursLeft, hasReputation, canAfford);
        PacketDistributor.sendToPlayer((ServerPlayer)player, (CustomPacketPayload)payload, (CustomPacketPayload[])new CustomPacketPayload[0]);
    }

    private static void sendQuestPayload(ServerPlayer player, MillVillager villager, QuestInstance qi, ServerLevel overworld) {
        QuestStep step = qi.getCurrentStep();
        if (step == null) {
            return;
        }
        String playerName = player.getName().getString();
        String locale = QuestTextRenderer.playerLocale(player);
        String descKey = qi.getQuest().key() + "_" + qi.getCurrentStepIndex() + "_description";
        String inlineDesc = step.descriptions().getOrDefault(locale, step.descriptions().getOrDefault("en", ""));
        String descText = QuestTextRenderer.lookupText(descKey, locale, inlineDesc);
        descText = QuestTextRenderer.substitute(descText, qi, playerName, overworld);
        boolean conditionsMet = true;
        StringBuilder conditionText = new StringBuilder();
        if (!step.requiredGoods().isEmpty()) {
            for (Map.Entry<QuestItemRef, Integer> entry : step.requiredGoods().entrySet()) {
                QuestItemRef ref = entry.getKey();
                int needed = entry.getValue();
                Item item = ItemHelper.resolve(ref.itemId());
                if (item == null) {
                    conditionsMet = false;
                    continue;
                }
                int playerHas = VillagerInteraction.countPlayerItems(player, item);
                if (playerHas >= needed) continue;
                conditionsMet = false;
                if (!step.showRequiredGoods()) continue;
                if (conditionText.length() > 0) {
                    conditionText.append(", ");
                }
                conditionText.append(needed - playerHas).append(" ").append(item.getDescription().getString());
            }
            if (!conditionsMet && conditionText.length() > 0) {
                conditionText.insert(0, Component.translatable((String)"gui.millenaire.quest.missing_goods").getString() + ": ");
            } else if (!conditionsMet) {
                conditionText.append(Component.translatable((String)"gui.millenaire.quest.conditions_not_met").getString());
            }
        }
        String nativeOccupation = "";
        String gameOccupation = "";
        VillagerType vType = VillagerInteraction.resolveVillagerType(villager);
        if (vType != null) {
            nativeOccupation = vType.nativeName() != null ? vType.nativeName() : "";
            String roleName = villager.getRoleName();
            if (roleName != null && !roleName.isEmpty()) {
                gameOccupation = roleName.startsWith("role.") ? Component.translatable((String)roleName).getString() : roleName;
            }
        }
        TravelBookRef questRef = VillagerInteraction.extractTravelBookRef(villager);
        QuestOpenPayload payload = new QuestOpenPayload(villager.getId(), qi.getUniqueId(), villager.getVillagerDisplayName(), nativeOccupation, gameOccupation, descText, conditionText.toString(), conditionsMet, qi.getCurrentStepIndex() == 0, qi.getCurrentStepIndex(), questRef.cultureKey(), questRef.villagerTypeKey());
        PacketDistributor.sendToPlayer((ServerPlayer)player, (CustomPacketPayload)payload, (CustomPacketPayload[])new CustomPacketPayload[0]);
        LOGGER.debug("QuestOpen payload sent to {} for quest {} step {}", new Object[]{player.getName().getString(), qi.getQuest().key(), qi.getCurrentStepIndex()});
    }

    private static int countPlayerItems(ServerPlayer player, Item item) {
        int count = 0;
        for (int i = 0; i < player.getInventory().getContainerSize(); ++i) {
            ItemStack stack = player.getInventory().getItem(i);
            if (!stack.is(item)) continue;
            count += stack.getCount();
        }
        return count;
    }

    @Nullable
    private static VillagerType resolveVillagerType(MillVillager villager) {
        ResourceLocation typeId = villager.getVillagerTypeId();
        if (typeId == null) {
            return null;
        }
        return ModCultures.getVillagerType(typeId);
    }

    private static TravelBookRef extractTravelBookRef(MillVillager villager) {
        ResourceLocation vtId = villager.getVillagerTypeId();
        if (vtId == null) {
            return TravelBookRef.EMPTY;
        }
        return new TravelBookRef(ModCultures.extractCultureId(vtId).getPath(), vtId.getPath());
    }

    @Nullable
    private static Village resolveVillage(MillVillager villager) {
        if (villager.getVillageId() == null) {
            return null;
        }
        Level level = villager.level();
        if (!(level instanceof ServerLevel)) {
            return null;
        }
        ServerLevel serverLevel = (ServerLevel)level;
        return Village.resolve(serverLevel, villager.getVillageId());
    }

    private static String buildVillageName(MillVillager villager, @Nullable Village village) {
        if (village == null) {
            return "Unaffiliated";
        }
        if (village.getVillageName() != null) {
            return village.getVillageName();
        }
        return VillagerInteraction.resolveCultureName(villager);
    }

    private static String resolveCultureName(MillVillager villager) {
        ResourceLocation vtId = villager.getVillagerTypeId();
        if (vtId == null) {
            return "Unknown";
        }
        ResourceLocation cultureId = ModCultures.extractCultureId(vtId);
        Culture culture = ModCultures.getCulture(cultureId);
        if (culture != null) {
            return culture.displayName();
        }
        String cp = cultureId.getPath();
        return cp.substring(0, 1).toUpperCase() + cp.substring(1);
    }

    private static String resolveReputationLabel(int reputation, ResourceLocation cultureId) {
        List<ReputationLabel> labels = ModCultures.getReputationLabels(cultureId);
        String label = VillageReputation.getLabel(reputation, labels);
        return label != null ? label : "Unknown";
    }

    private record TravelBookRef(String cultureKey, String villagerTypeKey) {
        static final TravelBookRef EMPTY = new TravelBookRef("", "");
    }
}

