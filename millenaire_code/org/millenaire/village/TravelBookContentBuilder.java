/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.mojang.logging.LogUtils
 *  javax.annotation.Nullable
 *  net.minecraft.network.protocol.common.custom.CustomPacketPayload
 *  net.minecraft.resources.ResourceLocation
 *  net.minecraft.server.level.ServerLevel
 *  net.minecraft.server.level.ServerPlayer
 *  net.minecraft.tags.TagKey
 *  net.minecraft.world.item.Item
 *  net.minecraft.world.level.Level
 *  net.minecraft.world.level.biome.Biome
 *  net.neoforged.neoforge.network.PacketDistributor
 *  org.slf4j.Logger
 */
package org.millenaire.village;

import com.mojang.logging.LogUtils;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.TreeMap;
import java.util.UUID;
import java.util.function.UnaryOperator;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import javax.annotation.Nullable;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.biome.Biome;
import net.neoforged.neoforge.network.PacketDistributor;
import org.millenaire.DisplayUtils;
import org.millenaire.Millenaire;
import org.millenaire.building.AnywoodHelper;
import org.millenaire.building.BuildingPlan;
import org.millenaire.building.BuildingPlanSet;
import org.millenaire.commerce.ShopProfile;
import org.millenaire.commerce.ShopProfileLoader;
import org.millenaire.commerce.TradeGood;
import org.millenaire.commerce.TradeGoodsLoader;
import org.millenaire.config.MillenaireServerConfig;
import org.millenaire.culture.Culture;
import org.millenaire.culture.ModCultures;
import org.millenaire.culture.TravelBookCategories;
import org.millenaire.culture.VillageType;
import org.millenaire.culture.VillagerType;
import org.millenaire.discovery.DiscoveryTracker;
import org.millenaire.goal.GoalRegistry;
import org.millenaire.goal.VillagerGoal;
import org.millenaire.item.ItemHelper;
import org.millenaire.item.MoneyHelper;
import org.millenaire.language.BuildingNameHelper;
import org.millenaire.language.LanguageHelper;
import org.millenaire.network.TravelBookContentPayload;
import org.millenaire.network.TravelBookRequestPayload;
import org.millenaire.quest.QuestInstance;
import org.millenaire.quest.QuestRegistry;
import org.millenaire.quest.QuestStep;
import org.millenaire.quest.QuestTextRenderer;
import org.millenaire.village.PlayerQuestData;
import org.millenaire.village.TravelBookLine;
import org.millenaire.village.TravelBookNavigationState;
import org.millenaire.village.TravelBookScreenState;
import org.slf4j.Logger;

public final class TravelBookContentBuilder {
    private static final Logger LOGGER = LogUtils.getLogger();
    private static final ThreadLocal<UnaryOperator<String>> LOCALE_OVERRIDE = new ThreadLocal();

    private TravelBookContentBuilder() {
    }

    /*
     * WARNING - Removed try catching itself - possible behaviour change.
     */
    public static List<TravelBookLine> exportItemDetail(TravelBookScreenState type, String culture, String item, UnaryOperator<String> resolve) {
        LOCALE_OVERRIDE.set(resolve);
        try {
            List<TravelBookLine> list = switch (type) {
                case TravelBookScreenState.CULTURE -> TravelBookContentBuilder.buildCulture(null, culture);
                case TravelBookScreenState.VILLAGER_DETAIL -> TravelBookContentBuilder.buildVillagerDetail(null, culture, item);
                case TravelBookScreenState.VILLAGE_DETAIL -> TravelBookContentBuilder.buildVillageDetail(null, culture, item);
                case TravelBookScreenState.BUILDING_DETAIL -> TravelBookContentBuilder.buildBuildingDetail(null, culture, item);
                case TravelBookScreenState.TRADE_GOOD_DETAIL -> TravelBookContentBuilder.buildTradeGoodDetail(null, culture, item);
                default -> throw new IllegalArgumentException("Not an exportable detail type: " + String.valueOf((Object)type));
            };
            return list;
        }
        finally {
            LOCALE_OVERRIDE.remove();
        }
    }

    public static void handleRequest(ServerPlayer player, TravelBookRequestPayload payload) {
        String item;
        String category;
        String culture;
        TravelBookScreenState targetState;
        UUID playerId = player.getUUID();
        int navAction = payload.navAction();
        switch (navAction) {
            case 1: {
                TravelBookNavigationState.NavEntry prev = TravelBookNavigationState.goBack(playerId);
                if (prev == null) {
                    targetState = TravelBookScreenState.HOME;
                    culture = "";
                    category = "";
                    item = "";
                    break;
                }
                targetState = prev.state();
                culture = prev.culture();
                category = prev.category();
                item = prev.item();
                break;
            }
            case 2: {
                String nextItem = TravelBookNavigationState.getNextItem(playerId);
                if (nextItem == null) {
                    targetState = TravelBookNavigationState.getCurrentState(playerId);
                    culture = TravelBookNavigationState.getCurrentCulture(playerId);
                    category = TravelBookNavigationState.getCurrentCategory(playerId);
                    item = TravelBookNavigationState.getCurrentItem(playerId);
                    break;
                }
                targetState = TravelBookNavigationState.getCurrentState(playerId);
                culture = TravelBookNavigationState.getCurrentCulture(playerId);
                category = TravelBookNavigationState.getCurrentCategory(playerId);
                item = nextItem;
                TravelBookNavigationState.navigate(playerId, targetState, culture, category, item);
                break;
            }
            case 3: {
                String prevItem = TravelBookNavigationState.getPrevItem(playerId);
                if (prevItem == null) {
                    targetState = TravelBookNavigationState.getCurrentState(playerId);
                    culture = TravelBookNavigationState.getCurrentCulture(playerId);
                    category = TravelBookNavigationState.getCurrentCategory(playerId);
                    item = TravelBookNavigationState.getCurrentItem(playerId);
                    break;
                }
                targetState = TravelBookNavigationState.getCurrentState(playerId);
                culture = TravelBookNavigationState.getCurrentCulture(playerId);
                category = TravelBookNavigationState.getCurrentCategory(playerId);
                item = prevItem;
                TravelBookNavigationState.navigate(playerId, targetState, culture, category, item);
                break;
            }
            default: {
                targetState = payload.targetState();
                culture = payload.cultureKey();
                category = payload.categoryKey();
                item = payload.itemKey();
                TravelBookNavigationState.navigate(playerId, targetState, culture, category, item);
            }
        }
        List<TravelBookLine> lines = TravelBookContentBuilder.buildContent(player, targetState, culture, category, item);
        boolean hasBack = TravelBookNavigationState.hasBack(playerId);
        boolean hasNext = TravelBookNavigationState.hasNext(playerId);
        boolean hasPrev = TravelBookNavigationState.hasPrev(playerId);
        TitleResult pageTitle = TravelBookContentBuilder.buildPageTitle(player, targetState, culture, category, item);
        MockAppearance mock = targetState == TravelBookScreenState.VILLAGER_DETAIL ? TravelBookContentBuilder.buildMockAppearance(culture, item) : MockAppearance.EMPTY;
        TravelBookContentPayload response = new TravelBookContentPayload(targetState, lines, hasBack, hasNext, hasPrev, pageTitle.text(), pageTitle.translatable(), mock.modelType(), mock.texture(), mock.cloth0(), mock.cloth1(), mock.scale(), mock.heldItem(), mock.heldItemOffHand());
        PacketDistributor.sendToPlayer((ServerPlayer)player, (CustomPacketPayload)response, (CustomPacketPayload[])new CustomPacketPayload[0]);
    }

    private static List<TravelBookLine> buildContent(ServerPlayer player, TravelBookScreenState state, String culture, String category, String item) {
        return switch (state) {
            default -> throw new MatchException(null, null);
            case TravelBookScreenState.HOME -> TravelBookContentBuilder.buildHome(player);
            case TravelBookScreenState.CULTURE -> TravelBookContentBuilder.buildCulture(player, culture);
            case TravelBookScreenState.VILLAGERS_LIST -> TravelBookContentBuilder.buildVillagersList(player, culture, category);
            case TravelBookScreenState.VILLAGER_DETAIL -> TravelBookContentBuilder.buildVillagerDetail(player, culture, item);
            case TravelBookScreenState.VILLAGES_LIST -> TravelBookContentBuilder.buildVillagesList(player, culture);
            case TravelBookScreenState.VILLAGE_DETAIL -> TravelBookContentBuilder.buildVillageDetail(player, culture, item);
            case TravelBookScreenState.BUILDINGS_LIST -> TravelBookContentBuilder.buildBuildingsList(player, culture, category);
            case TravelBookScreenState.BUILDING_DETAIL -> TravelBookContentBuilder.buildBuildingDetail(player, culture, item);
            case TravelBookScreenState.TRADE_GOODS_LIST -> TravelBookContentBuilder.buildTradeGoodsList(player, culture, category);
            case TravelBookScreenState.TRADE_GOOD_DETAIL -> TravelBookContentBuilder.buildTradeGoodDetail(player, culture, item);
        };
    }

    private static TitleResult buildPageTitle(ServerPlayer player, TravelBookScreenState state, String culture, String category, String item) {
        boolean learning = TravelBookContentBuilder.isLearningMode();
        DiscoveryTracker tracker = learning ? TravelBookContentBuilder.getTracker(player) : null;
        UUID playerId = player.getUUID();
        return switch (state) {
            default -> throw new MatchException(null, null);
            case TravelBookScreenState.HOME -> TitleResult.literal(TravelBookContentBuilder.t("millenaire.travel_book.title"));
            case TravelBookScreenState.CULTURE -> TitleResult.literal(TravelBookContentBuilder.getCultureDisplayName(culture));
            case TravelBookScreenState.VILLAGERS_LIST -> TitleResult.literal(TravelBookContentBuilder.getCultureDisplayName(culture) + " - " + TravelBookContentBuilder.resolveCategoryName(culture, category));
            case TravelBookScreenState.VILLAGER_DETAIL -> {
                if (learning && tracker != null && !tracker.isVillagerUnlocked(playerId, culture, item)) {
                    yield TitleResult.literal(TravelBookContentBuilder.t("millenaire.travel_book.unknown_villager"));
                }
                yield TitleResult.literal(TravelBookContentBuilder.getVillagerDisplayName(culture, item));
            }
            case TravelBookScreenState.VILLAGES_LIST -> TitleResult.literal(TravelBookContentBuilder.getCultureDisplayName(culture) + " - " + TravelBookContentBuilder.t("millenaire.travel_book.villages"));
            case TravelBookScreenState.VILLAGE_DETAIL -> {
                if (learning && tracker != null && !tracker.isVillageUnlocked(playerId, culture, item)) {
                    yield TitleResult.literal(TravelBookContentBuilder.t("millenaire.travel_book.unknown_village"));
                }
                yield TitleResult.literal(TravelBookContentBuilder.getVillageDisplayName(culture, item));
            }
            case TravelBookScreenState.BUILDINGS_LIST -> TitleResult.literal(TravelBookContentBuilder.getCultureDisplayName(culture) + " - " + TravelBookContentBuilder.resolveCategoryName(culture, category));
            case TravelBookScreenState.BUILDING_DETAIL -> {
                if (learning && tracker != null && !tracker.isBuildingUnlocked(playerId, culture, item)) {
                    yield TitleResult.literal(TravelBookContentBuilder.t("millenaire.travel_book.unknown_building"));
                }
                ResourceLocation bpsId = ResourceLocation.fromNamespaceAndPath((String)"millenaire", (String)item);
                BuildingPlanSet bps = ModCultures.getBuildingPlanSet(bpsId);
                if (bps != null && LanguageHelper.canReadBuildingNames(player, bps.culture())) {
                    yield TitleResult.translatable(TravelBookContentBuilder.getBuildingTranslationKey(culture, item));
                }
                yield TitleResult.literal(bps != null ? bps.nativeName() : item);
            }
            case TravelBookScreenState.TRADE_GOODS_LIST -> TitleResult.literal(TravelBookContentBuilder.getCultureDisplayName(culture) + " - " + TravelBookContentBuilder.resolveCategoryName(culture, category));
            case TravelBookScreenState.TRADE_GOOD_DETAIL -> learning && tracker != null && !tracker.isTradeGoodUnlocked(playerId, culture, item) ? TitleResult.literal(TravelBookContentBuilder.t("millenaire.travel_book.unknown_trade_good")) : TitleResult.literal(TravelBookContentBuilder.getTradeGoodDisplayName(culture, item));
        };
    }

    private static String resolveCategoryName(String cultureKey, String categoryKey) {
        if (categoryKey == null || categoryKey.isEmpty()) {
            return "";
        }
        Culture culture = ModCultures.getCulture(TravelBookContentBuilder.cultureId(cultureKey));
        if (culture == null) {
            return TravelBookContentBuilder.formatCategoryName(categoryKey);
        }
        return TravelBookContentBuilder.resolveCatName(culture.travelBookCategories(), categoryKey);
    }

    private static String resolveCatName(TravelBookCategories categories, String categoryKey) {
        String i18nKey = categories.categoryNames().getOrDefault(categoryKey, "");
        if (i18nKey.isEmpty()) {
            return TravelBookContentBuilder.formatCategoryName(categoryKey);
        }
        return TravelBookContentBuilder.t(i18nKey);
    }

    private static List<TravelBookLine> buildHome(ServerPlayer player) {
        ArrayList<TravelBookLine> lines = new ArrayList<TravelBookLine>();
        lines.add(TravelBookLine.text(TravelBookContentBuilder.t("millenaire.travel_book.intro")));
        lines.add(TravelBookLine.separator());
        List<Map.Entry> cultures = ModCultures.getAllCultures().entrySet().stream().sorted(Comparator.comparing(e -> ((Culture)e.getValue()).displayName())).toList();
        for (Map.Entry entry : cultures) {
            Culture culture = (Culture)entry.getValue();
            String cultureKey = ((ResourceLocation)entry.getKey()).getPath();
            TravelBookLine.TravelBookNavTarget target = new TravelBookLine.TravelBookNavTarget(TravelBookScreenState.CULTURE, cultureKey, "", "");
            lines.add(TravelBookLine.clickable(TravelBookContentBuilder.getCultureDisplayName(cultureKey), target));
        }
        TravelBookContentBuilder.appendQuestSection(player, lines);
        return lines;
    }

    private static void appendQuestSection(ServerPlayer player, List<TravelBookLine> lines) {
        ServerLevel overworld = player.server.getLevel(Level.OVERWORLD);
        if (overworld == null) {
            return;
        }
        PlayerQuestData questData = PlayerQuestData.get(overworld, QuestRegistry::get);
        List<QuestInstance> activeQuests = questData.getActiveQuests(player.getUUID());
        if (activeQuests.isEmpty()) {
            return;
        }
        lines.add(TravelBookLine.separator());
        lines.add(TravelBookLine.translatable("millenaire.travel_book.quests_in_progress"));
        long worldTime = overworld.getDayTime();
        String playerName = player.getName().getString();
        String locale = QuestTextRenderer.playerLocale(player);
        for (QuestInstance qi : activeQuests) {
            long timeLeftHours;
            QuestStep step = qi.getCurrentStep();
            if (step == null) continue;
            String labelKey = qi.getQuest().key() + "_" + qi.getCurrentStepIndex() + "_label";
            String inlineLabel = step.labels().getOrDefault(locale, step.labels().getOrDefault("en", qi.getQuest().key()));
            String label = QuestTextRenderer.lookupText(labelKey, locale, inlineLabel);
            label = QuestTextRenderer.substitute(label, qi, playerName, overworld);
            lines.add(TravelBookLine.text("\u00a76" + label));
            String listingKey = qi.getQuest().key() + "_" + qi.getCurrentStepIndex() + "_listing";
            String inlineListing = step.listings().getOrDefault(locale, step.listings().getOrDefault("en", ""));
            String listing = QuestTextRenderer.lookupText(listingKey, locale, inlineListing);
            if (!listing.isEmpty()) {
                listing = QuestTextRenderer.substitute(listing, qi, playerName, overworld);
                lines.add(TravelBookLine.text("  " + listing));
            }
            if ((timeLeftHours = Math.round((double)(qi.getCurrentStepStart() + (long)step.duration() * 1000L - worldTime) / 1000.0)) <= 0L) {
                lines.add(TravelBookLine.translatable("millenaire.travel_book.quest_time_expired"));
                continue;
            }
            lines.add(TravelBookLine.text("  " + TravelBookContentBuilder.t("millenaire.travel_book.quest_time_remaining") + ": " + timeLeftHours + " " + TravelBookContentBuilder.t("millenaire.travel_book.quest_hours")));
        }
    }

    private static List<TravelBookLine> buildCulture(ServerPlayer player, String cultureKey) {
        TravelBookLine.TravelBookNavTarget target;
        String catName;
        ArrayList<TravelBookLine> lines = new ArrayList<TravelBookLine>();
        ResourceLocation cultureId = TravelBookContentBuilder.cultureId(cultureKey);
        Culture culture = ModCultures.getCulture(cultureId);
        if (culture == null) {
            lines.add(TravelBookLine.text("Unknown culture: " + cultureKey));
            return lines;
        }
        lines.add(TravelBookLine.text(TravelBookContentBuilder.getCultureDisplayName(cultureKey)));
        TravelBookContentBuilder.addDescriptionLine(lines, TravelBookContentBuilder.descKey(cultureKey, "culture", ""));
        lines.add(TravelBookLine.separator());
        TravelBookCategories categories = culture.travelBookCategories();
        if (!categories.villagerCategories().isEmpty()) {
            lines.add(TravelBookLine.text(TravelBookContentBuilder.t("millenaire.travel_book.villagers")));
            for (String cat : categories.villagerCategories()) {
                catName = TravelBookContentBuilder.resolveCatName(categories, cat);
                target = new TravelBookLine.TravelBookNavTarget(TravelBookScreenState.VILLAGERS_LIST, cultureKey, cat, "");
                String icon = categories.categoryIcons().getOrDefault(cat, "");
                if (!icon.isEmpty()) {
                    lines.add(TravelBookLine.clickableWithIcon(catName, "", icon, target));
                    continue;
                }
                lines.add(TravelBookLine.clickable("  " + catName, target));
            }
        }
        lines.add(TravelBookLine.separator());
        lines.add(TravelBookLine.clickable(TravelBookContentBuilder.t("millenaire.travel_book.villages"), new TravelBookLine.TravelBookNavTarget(TravelBookScreenState.VILLAGES_LIST, cultureKey, "", "")));
        if (!categories.buildingCategories().isEmpty()) {
            lines.add(TravelBookLine.separator());
            lines.add(TravelBookLine.text(TravelBookContentBuilder.t("millenaire.travel_book.buildings")));
            for (String cat : categories.buildingCategories()) {
                catName = TravelBookContentBuilder.resolveCatName(categories, cat);
                target = new TravelBookLine.TravelBookNavTarget(TravelBookScreenState.BUILDINGS_LIST, cultureKey, cat, "");
                lines.add(TravelBookLine.clickable("  " + catName, target));
            }
        }
        if (!categories.tradeGoodCategories().isEmpty()) {
            lines.add(TravelBookLine.separator());
            lines.add(TravelBookLine.text(TravelBookContentBuilder.t("millenaire.travel_book.trade_goods")));
            for (String cat : categories.tradeGoodCategories()) {
                catName = TravelBookContentBuilder.resolveCatName(categories, cat);
                target = new TravelBookLine.TravelBookNavTarget(TravelBookScreenState.TRADE_GOODS_LIST, cultureKey, cat, "");
                lines.add(TravelBookLine.clickable("  " + catName, target));
            }
        }
        return lines;
    }

    private static List<TravelBookLine> buildVillagersList(ServerPlayer player, String cultureKey, String category) {
        ArrayList<TravelBookLine> lines = new ArrayList<TravelBookLine>();
        ResourceLocation cultureId = TravelBookContentBuilder.cultureId(cultureKey);
        boolean learning = TravelBookContentBuilder.isLearningMode();
        DiscoveryTracker tracker = TravelBookContentBuilder.getTracker(player);
        UUID playerId = player.getUUID();
        List<VillagerType> types = ModCultures.getAllVillagerTypes().values().stream().filter(vt -> vt.culture().equals((Object)cultureId)).filter(VillagerType::travelBookDisplay).filter(vt -> TravelBookContentBuilder.matchesCategory(vt.travelBookCategory(), category)).sorted(Comparator.comparing(VillagerType::nativeName)).toList();
        List<String> itemKeys = types.stream().map(vt -> vt.id().getPath()).toList();
        TravelBookNavigationState.setCurrentCategoryItems(playerId, itemKeys);
        for (VillagerType vt2 : types) {
            String itemKey = vt2.id().getPath();
            boolean unlocked = !learning || tracker.isVillagerUnlocked(playerId, cultureKey, itemKey);
            TravelBookLine.TravelBookNavTarget target = new TravelBookLine.TravelBookNavTarget(TravelBookScreenState.VILLAGER_DETAIL, cultureKey, category, itemKey);
            if (unlocked) {
                lines.add(TravelBookContentBuilder.villagerLine(vt2, player, target));
                continue;
            }
            lines.add(TravelBookLine.clickable("\u00a7o" + TravelBookContentBuilder.t("millenaire.travel_book.unknown_villager"), target));
        }
        if (types.isEmpty()) {
            lines.add(TravelBookLine.text(TravelBookContentBuilder.t("millenaire.travel_book.no_entries")));
        }
        return lines;
    }

    static List<ResourceLocation> visibleTravelBookGoals(List<ResourceLocation> goals, GoalRegistry registry) {
        ArrayList<ResourceLocation> visible = new ArrayList<ResourceLocation>();
        for (ResourceLocation id : goals) {
            VillagerGoal goal;
            VillagerGoal villagerGoal = goal = registry == null ? null : registry.get(id);
            if (goal != null && !goal.showInTravelBook()) continue;
            visible.add(id);
        }
        return visible;
    }

    private static List<TravelBookLine> buildVillagerDetail(ServerPlayer player, String cultureKey, String itemKey) {
        List<VillageType> list;
        List<BuildingPlanSet> list2;
        List<ResourceLocation> visibleGoals;
        ArrayList<TravelBookLine> lines = new ArrayList<TravelBookLine>();
        ResourceLocation cultureId = TravelBookContentBuilder.cultureId(cultureKey);
        boolean learning = TravelBookContentBuilder.isLearningMode() && player != null;
        DiscoveryTracker tracker = learning ? TravelBookContentBuilder.getTracker(player) : null;
        UUID playerId = learning ? player.getUUID() : null;
        boolean unlocked = !learning || tracker.isVillagerUnlocked(playerId, cultureKey, itemKey);
        ResourceLocation vtId = ResourceLocation.fromNamespaceAndPath((String)"millenaire", (String)itemKey);
        VillagerType vt = ModCultures.getVillagerType(vtId);
        if (vt == null) {
            lines.add(TravelBookLine.text("Unknown villager type: " + itemKey));
            return lines;
        }
        if (!unlocked) {
            lines.add(TravelBookLine.text("\u00a74" + TravelBookContentBuilder.t("millenaire.travel_book.unknown_villager")));
            lines.add(TravelBookLine.text(vt.nativeName()));
            return lines;
        }
        lines.add(TravelBookContentBuilder.villagerLine(vt, player, null));
        TravelBookContentBuilder.addDescriptionLine(lines, TravelBookContentBuilder.descKey(cultureKey, "villager", itemKey));
        lines.add(TravelBookLine.separator());
        lines.add(TravelBookLine.columns(TravelBookContentBuilder.t("millenaire.travel_book.health"), String.valueOf((int)vt.maxHealth())));
        float attackStrength = vt.maxHealth() > 20.0f ? 4.0f : 2.0f;
        lines.add(TravelBookLine.columns(TravelBookContentBuilder.t("millenaire.travel_book.attack"), String.valueOf((int)attackStrength)));
        lines.add(TravelBookLine.columns(TravelBookContentBuilder.t("millenaire.travel_book.gender"), TravelBookContentBuilder.t("millenaire.travel_book.gender." + vt.gender().name().toLowerCase())));
        if (!vt.tags().isEmpty()) {
            lines.add(TravelBookLine.separator());
            lines.add(TravelBookLine.text(TravelBookContentBuilder.t("millenaire.travel_book.tags")));
            for (String string : vt.tags()) {
                lines.add(TravelBookLine.text("  - " + TravelBookContentBuilder.t("millenaire.travel_book.tag." + string)));
            }
        }
        if (!(visibleGoals = TravelBookContentBuilder.visibleTravelBookGoals(vt.goals(), Millenaire.getGoalRegistry())).isEmpty()) {
            lines.add(TravelBookLine.separator());
            lines.add(TravelBookLine.text(TravelBookContentBuilder.t("millenaire.travel_book.goals")));
            for (ResourceLocation resourceLocation : visibleGoals) {
                lines.add(TravelBookLine.text("  - " + TravelBookContentBuilder.t("goal.millenaire." + resourceLocation.getPath())));
            }
        }
        if (!vt.requiredGoods().isEmpty()) {
            lines.add(TravelBookLine.separator());
            lines.add(TravelBookLine.text(TravelBookContentBuilder.t("millenaire.travel_book.required_goods")));
            Comparator<Map.Entry> comparator = Comparator.comparing(Map.Entry::getKey);
            List<Map.Entry> list3 = TravelBookContentBuilder.stableForExport(vt.requiredGoods().entrySet().stream(), comparator).toList();
            for (Map.Entry entry : list3) {
                ResourceLocation goodId = ResourceLocation.parse((String)((String)entry.getKey()));
                String left = "  " + TravelBookContentBuilder.formatItemName(goodId);
                String right = "x" + String.valueOf(entry.getValue());
                lines.add(TravelBookContentBuilder.isExportMode() ? TravelBookLine.withIcon(left, right, goodId.toString()) : TravelBookLine.columns(left, right));
            }
        }
        if (!vt.toolNeededClasses().isEmpty()) {
            lines.add(TravelBookLine.separator());
            lines.add(TravelBookLine.text(TravelBookContentBuilder.t("millenaire.travel_book.tools_needed")));
            for (String string : vt.toolNeededClasses()) {
                lines.add(TravelBookLine.text("  - " + TravelBookContentBuilder.t("millenaire.travel_book.tool_class." + string)));
            }
        }
        if (!(list2 = TravelBookContentBuilder.findResidences(cultureId, vt.id().getPath())).isEmpty()) {
            lines.add(TravelBookLine.separator());
            lines.add(TravelBookLine.text(TravelBookContentBuilder.t("millenaire.travel_book.residences")));
            for (BuildingPlanSet bps : list2) {
                TravelBookLine.TravelBookNavTarget target = new TravelBookLine.TravelBookNavTarget(TravelBookScreenState.BUILDING_DETAIL, cultureKey, "", bps.id().getPath());
                lines.add(TravelBookContentBuilder.buildingLine(bps, player, target));
            }
        }
        if (!(list = TravelBookContentBuilder.findVillagesWithVillager(cultureId, vt.id())).isEmpty()) {
            lines.add(TravelBookLine.separator());
            lines.add(TravelBookLine.text(TravelBookContentBuilder.t("millenaire.travel_book.found_in_villages")));
            for (VillageType village : list) {
                TravelBookLine.TravelBookNavTarget target = new TravelBookLine.TravelBookNavTarget(TravelBookScreenState.VILLAGE_DETAIL, cultureKey, "", village.id().getPath());
                lines.add(TravelBookLine.clickable("  " + village.name(), target));
            }
        }
        return lines;
    }

    private static List<TravelBookLine> buildVillagesList(ServerPlayer player, String cultureKey) {
        ArrayList<TravelBookLine> lines = new ArrayList<TravelBookLine>();
        ResourceLocation cultureId = TravelBookContentBuilder.cultureId(cultureKey);
        boolean learning = TravelBookContentBuilder.isLearningMode();
        DiscoveryTracker tracker = TravelBookContentBuilder.getTracker(player);
        UUID playerId = player.getUUID();
        List<VillageType> types = ModCultures.getAllVillageTypes().values().stream().filter(vt -> vt.culture().equals((Object)cultureId)).filter(VillageType::travelBookDisplay).filter(vt -> !vt.loneBuilding() || vt.keyLoneBuilding()).sorted(Comparator.comparing(VillageType::name)).toList();
        List<String> itemKeys = types.stream().map(vt -> vt.id().getPath()).toList();
        TravelBookNavigationState.setCurrentCategoryItems(playerId, itemKeys);
        for (VillageType vt2 : types) {
            String itemKey = vt2.id().getPath();
            boolean unlocked = !learning || tracker.isVillageUnlocked(playerId, cultureKey, itemKey);
            Object displayName = unlocked ? vt2.name() : "\u00a7o" + TravelBookContentBuilder.t("millenaire.travel_book.unknown_village");
            TravelBookLine.TravelBookNavTarget target = new TravelBookLine.TravelBookNavTarget(TravelBookScreenState.VILLAGE_DETAIL, cultureKey, "", itemKey);
            if (vt2.icon() != null && !vt2.icon().isEmpty()) {
                lines.add(TravelBookLine.clickableWithIcon((String)displayName, "", vt2.icon(), target));
                continue;
            }
            lines.add(TravelBookLine.clickable((String)displayName, target));
        }
        if (types.isEmpty()) {
            lines.add(TravelBookLine.text(TravelBookContentBuilder.t("millenaire.travel_book.no_entries")));
        }
        return lines;
    }

    private static List<TravelBookLine> buildVillageDetail(ServerPlayer player, String cultureKey, String itemKey) {
        ArrayList<TravelBookLine> lines = new ArrayList<TravelBookLine>();
        ResourceLocation cultureId = TravelBookContentBuilder.cultureId(cultureKey);
        boolean learning = TravelBookContentBuilder.isLearningMode() && player != null;
        DiscoveryTracker tracker = learning ? TravelBookContentBuilder.getTracker(player) : null;
        UUID playerId = learning ? player.getUUID() : null;
        boolean unlocked = !learning || tracker.isVillageUnlocked(playerId, cultureKey, itemKey);
        ResourceLocation vtId = ResourceLocation.fromNamespaceAndPath((String)"millenaire", (String)itemKey);
        VillageType vt = ModCultures.getVillageType(vtId);
        if (vt == null) {
            lines.add(TravelBookLine.text("Unknown village type: " + itemKey));
            return lines;
        }
        if (!unlocked) {
            lines.add(TravelBookLine.text("\u00a74" + TravelBookContentBuilder.t("millenaire.travel_book.unknown_village")));
            lines.add(TravelBookLine.text(vt.name()));
            return lines;
        }
        lines.add(TravelBookLine.text(vt.name()));
        TravelBookContentBuilder.addDescriptionLine(lines, TravelBookContentBuilder.descKey(cultureKey, "village", itemKey));
        lines.add(TravelBookLine.separator());
        lines.add(TravelBookLine.columns(TravelBookContentBuilder.t("millenaire.travel_book.radius"), String.valueOf(vt.radius())));
        if (vt.weight() > 0) {
            lines.add(TravelBookLine.columns(TravelBookContentBuilder.t("millenaire.travel_book.weight"), String.valueOf(vt.weight())));
        }
        if (!vt.biomeTags().isEmpty()) {
            lines.add(TravelBookLine.separator());
            lines.add(TravelBookLine.text(TravelBookContentBuilder.t("millenaire.travel_book.biomes")));
            for (TagKey<Biome> biomeTag : vt.biomeTags()) {
                lines.add(TravelBookLine.text("  - " + biomeTag.location().getPath()));
            }
        }
        if (!vt.layout().isEmpty()) {
            lines.add(TravelBookLine.separator());
            lines.add(TravelBookLine.text(TravelBookContentBuilder.t("millenaire.travel_book.buildings_layout")));
            TreeMap<String, List> byRole = new TreeMap<String, List>();
            for (VillageType.LayoutSlot layoutSlot : vt.layout()) {
                byRole.computeIfAbsent(layoutSlot.role(), k -> new ArrayList()).add(layoutSlot);
            }
            for (Map.Entry entry : byRole.entrySet()) {
                lines.add(TravelBookLine.text("  [" + (String)entry.getKey() + "]"));
                HashSet<ResourceLocation> seen = new HashSet<ResourceLocation>();
                for (VillageType.LayoutSlot slot : (List)entry.getValue()) {
                    if (!seen.add(slot.plan())) continue;
                    TravelBookLine.TravelBookNavTarget target = new TravelBookLine.TravelBookNavTarget(TravelBookScreenState.BUILDING_DETAIL, cultureKey, "", slot.plan().getPath());
                    BuildingPlanSet bps = ModCultures.getBuildingPlanSet(slot.plan());
                    if (bps != null) {
                        lines.add(TravelBookContentBuilder.buildingLine(bps, player, target));
                        continue;
                    }
                    lines.add(TravelBookLine.clickable(slot.plan().getPath(), target));
                }
            }
        }
        return lines;
    }

    private static List<TravelBookLine> buildBuildingsList(ServerPlayer player, String cultureKey, String category) {
        ArrayList<TravelBookLine> lines = new ArrayList<TravelBookLine>();
        ResourceLocation cultureId = TravelBookContentBuilder.cultureId(cultureKey);
        boolean learning = TravelBookContentBuilder.isLearningMode();
        DiscoveryTracker tracker = TravelBookContentBuilder.getTracker(player);
        UUID playerId = player.getUUID();
        List<BuildingPlanSet> sets = ModCultures.getAllBuildingPlanSets().values().stream().filter(bps -> bps.culture().equals((Object)cultureId)).filter(BuildingPlanSet::travelBookDisplay).filter(bps -> TravelBookContentBuilder.matchesCategory(bps.travelBookCategory(), category)).sorted(Comparator.comparing(BuildingPlanSet::nativeName)).toList();
        List<String> itemKeys = sets.stream().map(bps -> bps.id().getPath()).toList();
        TravelBookNavigationState.setCurrentCategoryItems(playerId, itemKeys);
        for (BuildingPlanSet bps2 : sets) {
            String itemKey = bps2.id().getPath();
            boolean unlocked = !learning || tracker.isBuildingUnlocked(playerId, cultureKey, itemKey);
            TravelBookLine.TravelBookNavTarget target = new TravelBookLine.TravelBookNavTarget(TravelBookScreenState.BUILDING_DETAIL, cultureKey, category, itemKey);
            if (unlocked) {
                lines.add(TravelBookContentBuilder.buildingLine(bps2, player, target));
                continue;
            }
            lines.add(TravelBookLine.clickable("\u00a7o" + TravelBookContentBuilder.t("millenaire.travel_book.unknown_building"), target));
        }
        if (sets.isEmpty()) {
            lines.add(TravelBookLine.text(TravelBookContentBuilder.t("millenaire.travel_book.no_entries")));
        }
        return lines;
    }

    private static List<TravelBookLine> buildBuildingDetail(ServerPlayer player, String cultureKey, String itemKey) {
        List<VillageType> villages;
        ArrayList<TravelBookLine> lines = new ArrayList<TravelBookLine>();
        ResourceLocation cultureId = TravelBookContentBuilder.cultureId(cultureKey);
        boolean learning = TravelBookContentBuilder.isLearningMode() && player != null;
        DiscoveryTracker tracker = learning ? TravelBookContentBuilder.getTracker(player) : null;
        UUID playerId = learning ? player.getUUID() : null;
        boolean unlocked = !learning || tracker.isBuildingUnlocked(playerId, cultureKey, itemKey);
        ResourceLocation bpsId = ResourceLocation.fromNamespaceAndPath((String)"millenaire", (String)itemKey);
        BuildingPlanSet bps = ModCultures.getBuildingPlanSet(bpsId);
        if (bps == null) {
            lines.add(TravelBookLine.text("Unknown building: " + itemKey));
            return lines;
        }
        if (!unlocked) {
            lines.add(TravelBookLine.text("\u00a74" + TravelBookContentBuilder.t("millenaire.travel_book.unknown_building")));
            lines.add(TravelBookLine.text(bps.nativeName()));
            return lines;
        }
        lines.add(TravelBookContentBuilder.buildingLine(bps, player, null));
        TravelBookContentBuilder.addDescriptionLine(lines, TravelBookContentBuilder.descKey(cultureKey, "building", TravelBookContentBuilder.extractSimpleName(bpsId)));
        lines.add(TravelBookLine.separator());
        if (bps.price() > 0) {
            lines.add(TravelBookLine.columns(TravelBookContentBuilder.t("millenaire.travel_book.price"), MoneyHelper.formatPrice(bps.price())));
            if (bps.reputation() > 0) {
                lines.add(TravelBookLine.columns(TravelBookContentBuilder.t("millenaire.travel_book.reputation_required"), String.valueOf(bps.reputation())));
            }
            lines.add(TravelBookLine.separator());
        }
        for (Map.Entry<String, List<BuildingPlanSet.LevelDef>> variantEntry : bps.variants().entrySet()) {
            String variant = variantEntry.getKey();
            if (bps.variants().size() > 1) {
                lines.add(TravelBookLine.text(TravelBookContentBuilder.t("millenaire.travel_book.variant") + " " + variant));
            }
            int prevIrrigation = 0;
            for (BuildingPlanSet.LevelDef level : variantEntry.getValue()) {
                String levelLabel = TravelBookContentBuilder.t("millenaire.travel_book.level") + " " + level.level();
                if (level.nativeName() != null) {
                    levelLabel = levelLabel + " - " + level.nativeName();
                }
                lines.add(TravelBookLine.text("  " + levelLabel));
                lines.add(TravelBookLine.columns("    " + TravelBookContentBuilder.t("millenaire.travel_book.size"), level.width() + "x" + level.depth()));
                BuildingPlan plan = ModCultures.getBuildingPlan(level.planId());
                if (plan != null && plan.shopId() != null) {
                    lines.add(TravelBookLine.columns("    " + TravelBookContentBuilder.t("millenaire.travel_book.shop"), plan.shopId()));
                }
                if (level.irrigation() > prevIrrigation) {
                    lines.add(TravelBookLine.columns("    " + TravelBookContentBuilder.t("millenaire.travel_book.irrigation"), "+" + level.irrigation() + "%"));
                }
                prevIrrigation = level.irrigation();
                if (!level.requiredResources().isEmpty()) {
                    lines.add(TravelBookLine.text("    " + TravelBookContentBuilder.t("millenaire.travel_book.construction_cost")));
                    Comparator<Map.Entry> resourcesCmp = Comparator.comparing(e -> ((ResourceLocation)e.getKey()).toString());
                    List<Map.Entry> resourcesSorted = TravelBookContentBuilder.stableForExport(level.requiredResources().entrySet().stream(), resourcesCmp).toList();
                    for (Map.Entry res : resourcesSorted) {
                        String itemName = AnywoodHelper.isAnywood((ResourceLocation)res.getKey()) ? TravelBookContentBuilder.t("item.millenaire.anywood_log") : TravelBookContentBuilder.formatItemName((ResourceLocation)res.getKey());
                        String left = "      " + itemName;
                        String right = "x" + String.valueOf(res.getValue());
                        lines.add(TravelBookContentBuilder.isExportMode() ? TravelBookLine.withIcon(left, right, ((ResourceLocation)res.getKey()).toString()) : TravelBookLine.columns(left, right));
                    }
                }
                if (level.subBuildings().isEmpty()) continue;
                lines.add(TravelBookLine.text("    " + TravelBookContentBuilder.t("millenaire.travel_book.sub_buildings")));
                for (String sub : level.subBuildings()) {
                    ResourceLocation subId = ResourceLocation.fromNamespaceAndPath((String)"millenaire", (String)(cultureKey + "/" + sub));
                    BuildingPlanSet subBps = ModCultures.getBuildingPlanSet(subId);
                    String subName = subBps != null ? subBps.nativeName() : sub;
                    TravelBookLine.TravelBookNavTarget target = new TravelBookLine.TravelBookNavTarget(TravelBookScreenState.BUILDING_DETAIL, cultureKey, "", subId.getPath());
                    lines.add(TravelBookLine.clickable("      " + subName, target));
                }
            }
        }
        if (!bps.maleResidents().isEmpty() || !bps.femaleResidents().isEmpty()) {
            lines.add(TravelBookLine.separator());
            lines.add(TravelBookLine.text(TravelBookContentBuilder.t("millenaire.travel_book.residents")));
            for (String resKey : bps.maleResidents()) {
                TravelBookContentBuilder.addResidentLink(lines, cultureKey, resKey);
            }
            for (String resKey : bps.femaleResidents()) {
                TravelBookContentBuilder.addResidentLink(lines, cultureKey, resKey);
            }
        }
        if (!(villages = TravelBookContentBuilder.findVillagesWithBuilding(cultureId, bpsId)).isEmpty()) {
            lines.add(TravelBookLine.separator());
            lines.add(TravelBookLine.text(TravelBookContentBuilder.t("millenaire.travel_book.found_in_villages")));
            for (VillageType village : villages) {
                TravelBookLine.TravelBookNavTarget target = new TravelBookLine.TravelBookNavTarget(TravelBookScreenState.VILLAGE_DETAIL, cultureKey, "", village.id().getPath());
                lines.add(TravelBookLine.clickable("  " + village.name(), target));
            }
        }
        return lines;
    }

    private static List<TravelBookLine> buildTradeGoodsList(ServerPlayer player, String cultureKey, String category) {
        ArrayList<TravelBookLine> lines = new ArrayList<TravelBookLine>();
        ResourceLocation cultureId = TravelBookContentBuilder.cultureId(cultureKey);
        boolean learning = TravelBookContentBuilder.isLearningMode();
        DiscoveryTracker tracker = TravelBookContentBuilder.getTracker(player);
        UUID playerId = player.getUUID();
        List<TradeGood> goods = TradeGoodsLoader.getGoods(cultureId).stream().filter(TradeGood::travelBookDisplay).filter(g -> TravelBookContentBuilder.matchesCategory(g.category(), category)).sorted(Comparator.comparing(TradeGood::id)).toList();
        List<String> itemKeys = goods.stream().map(TradeGood::id).toList();
        TravelBookNavigationState.setCurrentCategoryItems(playerId, itemKeys);
        for (TradeGood good : goods) {
            boolean unlocked = !learning || tracker.isTradeGoodUnlocked(playerId, cultureKey, good.id());
            Object displayName = unlocked ? good.id() : "\u00a7o" + TravelBookContentBuilder.t("millenaire.travel_book.unknown_trade_good");
            TravelBookLine.TravelBookNavTarget target = new TravelBookLine.TravelBookNavTarget(TravelBookScreenState.TRADE_GOOD_DETAIL, cultureKey, category, good.id());
            lines.add(TravelBookLine.clickable((String)displayName, target));
        }
        if (goods.isEmpty()) {
            lines.add(TravelBookLine.text(TravelBookContentBuilder.t("millenaire.travel_book.no_entries")));
        }
        return lines;
    }

    private static List<TravelBookLine> buildTradeGoodDetail(ServerPlayer player, String cultureKey, String itemKey) {
        TravelBookLine.TravelBookNavTarget target;
        BuildingPlanSet bps;
        ArrayList<TravelBookLine> lines = new ArrayList<TravelBookLine>();
        ResourceLocation cultureId = TravelBookContentBuilder.cultureId(cultureKey);
        boolean learning = TravelBookContentBuilder.isLearningMode() && player != null;
        DiscoveryTracker tracker = learning ? TravelBookContentBuilder.getTracker(player) : null;
        UUID playerId = learning ? player.getUUID() : null;
        boolean unlocked = !learning || tracker.isTradeGoodUnlocked(playerId, cultureKey, itemKey);
        TradeGood good = TradeGoodsLoader.getGoodById(cultureId, itemKey);
        if (good == null) {
            lines.add(TravelBookLine.text("Unknown trade good: " + itemKey));
            return lines;
        }
        if (!unlocked) {
            lines.add(TravelBookLine.text("\u00a74" + TravelBookContentBuilder.t("millenaire.travel_book.unknown_trade_good")));
            lines.add(TravelBookLine.text(good.id()));
            return lines;
        }
        lines.add(TravelBookLine.text(TravelBookContentBuilder.formatItemName(good.itemLocation())));
        TravelBookContentBuilder.addDescriptionLine(lines, TravelBookContentBuilder.descKey(cultureKey, "trade_good", itemKey));
        lines.add(TravelBookLine.separator());
        String itemName = TravelBookContentBuilder.formatItemName(good.itemLocation());
        lines.add(TravelBookLine.columns(TravelBookContentBuilder.t("millenaire.travel_book.item"), itemName));
        if (good.canSell()) {
            lines.add(TravelBookLine.columns(TravelBookContentBuilder.t("millenaire.travel_book.sell_price"), MoneyHelper.formatPrice(good.sellingPrice())));
        }
        if (good.canBuy()) {
            lines.add(TravelBookLine.columns(TravelBookContentBuilder.t("millenaire.travel_book.buy_price"), MoneyHelper.formatPrice(good.buyingPrice())));
        }
        if (good.minReputation() != 0) {
            lines.add(TravelBookLine.columns(TravelBookContentBuilder.t("millenaire.travel_book.min_reputation"), String.valueOf(good.minReputation())));
        }
        if (good.foreignMerchantPrice() > 0) {
            lines.add(TravelBookLine.columns(TravelBookContentBuilder.t("millenaire.travel_book.market_price"), MoneyHelper.formatPrice(good.foreignMerchantPrice())));
        }
        if (good.autoGenerate()) {
            lines.add(TravelBookLine.text(TravelBookContentBuilder.t("millenaire.travel_book.auto_generated")));
        }
        ArrayList<String> sellingShops = new ArrayList<String>();
        ArrayList<String> buyingShops = new ArrayList<String>();
        Map<String, ShopProfile> profiles = ShopProfileLoader.getProfiles(cultureId);
        if (profiles != null) {
            Iterator profilesCmp = Comparator.comparing(Map.Entry::getKey);
            List<Map.Entry> profilesSorted = TravelBookContentBuilder.stableForExport(profiles.entrySet().stream(), profilesCmp).toList();
            for (Map.Entry entry : profilesSorted) {
                ShopProfile profile = (ShopProfile)entry.getValue();
                if (profile.sells().contains(itemKey)) {
                    sellingShops.add((String)entry.getKey());
                }
                if (!profile.buys().contains(itemKey) && !profile.buysOptional().contains(itemKey)) continue;
                buyingShops.add((String)entry.getKey());
            }
        }
        if (!sellingShops.isEmpty()) {
            lines.add(TravelBookLine.separator());
            lines.add(TravelBookLine.text(TravelBookContentBuilder.t("millenaire.travel_book.sold_by")));
            for (String shopId : sellingShops) {
                bps = TravelBookContentBuilder.findBuildingWithShop(cultureId, shopId);
                if (bps != null) {
                    target = new TravelBookLine.TravelBookNavTarget(TravelBookScreenState.BUILDING_DETAIL, cultureKey, "", bps.id().getPath());
                    lines.add(TravelBookContentBuilder.buildingLine(bps, player, target));
                    continue;
                }
                lines.add(TravelBookLine.text("  " + shopId));
            }
        }
        if (!buyingShops.isEmpty()) {
            lines.add(TravelBookLine.separator());
            lines.add(TravelBookLine.text(TravelBookContentBuilder.t("millenaire.travel_book.bought_by")));
            for (String shopId : buyingShops) {
                bps = TravelBookContentBuilder.findBuildingWithShop(cultureId, shopId);
                if (bps != null) {
                    target = new TravelBookLine.TravelBookNavTarget(TravelBookScreenState.BUILDING_DETAIL, cultureKey, "", bps.id().getPath());
                    lines.add(TravelBookContentBuilder.buildingLine(bps, player, target));
                    continue;
                }
                lines.add(TravelBookLine.text("  " + shopId));
            }
        }
        if (good.foreignMerchantPrice() > 0) {
            ArrayList<String> merchantNames = new ArrayList<String>();
            Comparator<Map.Entry> villagersCmp = Comparator.comparing(e -> ((ResourceLocation)e.getKey()).toString());
            List<Map.Entry> villagersSorted = TravelBookContentBuilder.stableForExport(ModCultures.getAllVillagerTypes().entrySet().stream(), villagersCmp).toList();
            for (Map.Entry vtEntry : villagersSorted) {
                VillagerType vt = (VillagerType)vtEntry.getValue();
                if (!vt.culture().equals((Object)cultureId) || !vt.hasTag("foreignmerchant") || !vt.foreignMerchantStock().containsKey(good.itemLocation())) continue;
                merchantNames.add(TravelBookContentBuilder.resolveRoleName((ResourceLocation)vtEntry.getKey()));
            }
            if (!merchantNames.isEmpty()) {
                lines.add(TravelBookLine.separator());
                lines.add(TravelBookLine.text(TravelBookContentBuilder.t("millenaire.travel_book.sold_by_merchants")));
                for (String name : merchantNames) {
                    lines.add(TravelBookLine.text("  " + name));
                }
            }
        }
        return lines;
    }

    private static <T> Stream<T> stableForExport(Stream<T> stream, Comparator<T> comparator) {
        return LOCALE_OVERRIDE.get() != null ? stream.sorted(comparator) : stream;
    }

    private static boolean isExportMode() {
        return LOCALE_OVERRIDE.get() != null;
    }

    private static String t(String key) {
        UnaryOperator<String> override = LOCALE_OVERRIDE.get();
        return override != null ? (String)override.apply(key) : DisplayUtils.t(key);
    }

    static String descKey(String cultureKey, String type, String itemKey) {
        int slash = itemKey.lastIndexOf(47);
        String simpleName = slash >= 0 ? itemKey.substring(slash + 1) : itemKey;
        String base = "travelbook.millenaire." + cultureKey + "." + type;
        return simpleName.isEmpty() ? base + ".desc" : base + "." + simpleName + ".desc";
    }

    private static void addDescriptionLine(List<TravelBookLine> lines, String descKey) {
        if (!TravelBookContentBuilder.t(descKey).equals(descKey)) {
            lines.add(TravelBookLine.translatable(descKey));
        }
    }

    private static boolean isLearningMode() {
        return (Boolean)MillenaireServerConfig.SERVER.travelBookLearning.get();
    }

    private static DiscoveryTracker getTracker(ServerPlayer player) {
        ServerLevel overworld = player.getServer().getLevel(Level.OVERWORLD);
        return DiscoveryTracker.get(overworld);
    }

    private static ResourceLocation cultureId(String cultureKey) {
        return ResourceLocation.fromNamespaceAndPath((String)"millenaire", (String)cultureKey);
    }

    private static boolean matchesCategory(@Nullable String typeCategory, String requestedCategory) {
        if (requestedCategory.isEmpty()) {
            return true;
        }
        String effective = typeCategory != null ? typeCategory : "misc";
        return effective.equals(requestedCategory);
    }

    public static String extractSimpleName(ResourceLocation id) {
        String path = id.getPath();
        int slash = path.lastIndexOf(47);
        return slash >= 0 ? path.substring(slash + 1) : path;
    }

    public static String formatCategoryName(String category) {
        if (category.isEmpty()) {
            return "";
        }
        String formatted = category.replace('_', ' ');
        return Character.toUpperCase(formatted.charAt(0)) + formatted.substring(1);
    }

    private static TravelBookLine buildingLine(BuildingPlanSet bps, ServerPlayer player, @Nullable TravelBookLine.TravelBookNavTarget target) {
        if (player == null || LanguageHelper.canReadBuildingNames(player, bps.culture())) {
            String key = BuildingNameHelper.getTranslationKey(bps);
            if (target != null) {
                return TravelBookLine.clickableWithTranslation(bps.nativeName(), key, target);
            }
            return TravelBookLine.withTranslation(bps.nativeName(), key);
        }
        if (target != null) {
            return TravelBookLine.clickable(bps.nativeName(), target);
        }
        return TravelBookLine.text(bps.nativeName());
    }

    private static TravelBookLine villagerLine(VillagerType vt, ServerPlayer player, @Nullable TravelBookLine.TravelBookNavTarget target) {
        if (player == null || LanguageHelper.canReadVillagerNames(player, vt.culture())) {
            String key = BuildingNameHelper.getVillagerRoleKey(vt);
            if (target != null) {
                return TravelBookLine.clickableWithTranslation(vt.nativeName(), key, target);
            }
            return TravelBookLine.withTranslation(vt.nativeName(), key);
        }
        if (target != null) {
            return TravelBookLine.clickable(vt.nativeName(), target);
        }
        return TravelBookLine.text(vt.nativeName());
    }

    private static String resolveRoleName(ResourceLocation typeId) {
        String key = DisplayUtils.resolveRoleKey(typeId);
        String translated = TravelBookContentBuilder.t(key);
        String qualifiedRole = typeId.getPath().replace('/', '_');
        return translated.equals(key) ? qualifiedRole : translated;
    }

    private static String formatItemName(ResourceLocation itemId) {
        Item item = ItemHelper.resolve(itemId);
        if (item != null) {
            return TravelBookContentBuilder.t(item.getDescriptionId());
        }
        return TravelBookContentBuilder.extractSimpleName(itemId);
    }

    private static String getCultureDisplayName(String cultureKey) {
        String i18nKey = "culture.millenaire." + cultureKey;
        String resolved = TravelBookContentBuilder.t(i18nKey);
        if (!resolved.equals(i18nKey)) {
            return resolved;
        }
        Culture culture = ModCultures.getCulture(TravelBookContentBuilder.cultureId(cultureKey));
        return culture != null ? culture.displayName() : cultureKey;
    }

    private static String getVillagerDisplayName(String cultureKey, String itemKey) {
        ResourceLocation vtId = ResourceLocation.fromNamespaceAndPath((String)"millenaire", (String)itemKey);
        VillagerType vt = ModCultures.getVillagerType(vtId);
        return vt != null ? vt.nativeName() : itemKey;
    }

    private static String getVillageDisplayName(String cultureKey, String itemKey) {
        ResourceLocation vtId = ResourceLocation.fromNamespaceAndPath((String)"millenaire", (String)itemKey);
        VillageType vt = ModCultures.getVillageType(vtId);
        return vt != null ? vt.name() : itemKey;
    }

    private static String getBuildingTranslationKey(String cultureKey, String itemKey) {
        ResourceLocation bpsId = ResourceLocation.fromNamespaceAndPath((String)"millenaire", (String)itemKey);
        String key = BuildingNameHelper.getTranslationKey(bpsId);
        return key != null ? key : itemKey;
    }

    private static String getTradeGoodDisplayName(String cultureKey, String itemKey) {
        ResourceLocation cultureId = TravelBookContentBuilder.cultureId(cultureKey);
        TradeGood good = TradeGoodsLoader.getGoodById(cultureId, itemKey);
        if (good != null) {
            return TravelBookContentBuilder.formatItemName(good.itemLocation());
        }
        return itemKey;
    }

    private static void addResidentLink(List<TravelBookLine> lines, String cultureKey, String resKey) {
        String fullPath = cultureKey + "_" + resKey;
        ResourceLocation vtId = ResourceLocation.fromNamespaceAndPath((String)"millenaire", (String)fullPath);
        VillagerType vt = ModCultures.getVillagerType(vtId);
        String name = vt != null ? vt.nativeName() : resKey;
        TravelBookLine.TravelBookNavTarget target = new TravelBookLine.TravelBookNavTarget(TravelBookScreenState.VILLAGER_DETAIL, cultureKey, "", vtId.getPath());
        lines.add(TravelBookLine.clickable("  " + name, target));
    }

    private static String toResidentKey(ResourceLocation cultureId, String villagerPath) {
        String prefix = cultureId.getPath() + "_";
        return villagerPath.startsWith(prefix) ? villagerPath.substring(prefix.length()) : villagerPath;
    }

    private static List<BuildingPlanSet> findResidences(ResourceLocation cultureId, String villagerIdPath) {
        String residentKey = TravelBookContentBuilder.toResidentKey(cultureId, villagerIdPath);
        return ModCultures.getAllBuildingPlanSets().values().stream().filter(bps -> bps.culture().equals((Object)cultureId)).filter(bps -> bps.maleResidents().contains(residentKey) || bps.femaleResidents().contains(residentKey)).sorted(Comparator.comparing(BuildingPlanSet::nativeName)).toList();
    }

    private static List<VillageType> findVillagesWithVillager(ResourceLocation cultureId, ResourceLocation villagerId) {
        String residentKey = TravelBookContentBuilder.toResidentKey(cultureId, villagerId.getPath());
        Set buildingIds = ModCultures.getAllBuildingPlanSets().values().stream().filter(bps -> bps.culture().equals((Object)cultureId)).filter(bps -> bps.maleResidents().contains(residentKey) || bps.femaleResidents().contains(residentKey)).map(BuildingPlanSet::id).collect(Collectors.toSet());
        return ModCultures.getAllVillageTypes().values().stream().filter(vt -> vt.culture().equals((Object)cultureId)).filter(vt -> !vt.loneBuilding()).filter(vt -> vt.layout().stream().anyMatch(slot -> buildingIds.contains(slot.plan()))).sorted(Comparator.comparing(VillageType::name)).toList();
    }

    private static List<VillageType> findVillagesWithBuilding(ResourceLocation cultureId, ResourceLocation buildingId) {
        return ModCultures.getAllVillageTypes().values().stream().filter(vt -> vt.culture().equals((Object)cultureId)).filter(vt -> !vt.loneBuilding()).filter(vt -> vt.layout().stream().anyMatch(slot -> slot.plan().equals((Object)buildingId))).sorted(Comparator.comparing(VillageType::name)).toList();
    }

    @Nullable
    private static BuildingPlanSet findBuildingWithShop(ResourceLocation cultureId, String shopId) {
        for (BuildingPlanSet bps : ModCultures.getAllBuildingPlanSets().values()) {
            if (!bps.culture().equals((Object)cultureId)) continue;
            for (List<BuildingPlanSet.LevelDef> levels : bps.variants().values()) {
                for (BuildingPlanSet.LevelDef level : levels) {
                    BuildingPlan plan = ModCultures.getBuildingPlan(level.planId());
                    if (plan == null || !shopId.equals(plan.shopId())) continue;
                    return bps;
                }
            }
        }
        return null;
    }

    private static MockAppearance buildMockAppearance(String cultureKey, String itemKey) {
        ResourceLocation vtId = ResourceLocation.fromNamespaceAndPath((String)"millenaire", (String)itemKey);
        VillagerType vt = ModCultures.getVillagerType(vtId);
        if (vt == null) {
            return MockAppearance.EMPTY;
        }
        Random random = new Random(vtId.hashCode());
        List<ResourceLocation> textures = vt.textures();
        String texture = textures.isEmpty() ? "" : textures.get(random.nextInt(textures.size())).toString();
        float scale = vt.isChild() ? 0.5f : vt.baseScale() * (0.8f + random.nextFloat() * 0.09f);
        String cloth0 = "";
        String cloth1 = "";
        VillagerType.ClothSet freeSet = vt.clothes().get("free");
        VillagerType.ClothSet naturalSet = vt.clothes().get("natural");
        if (naturalSet != null && naturalSet.layer0() != null && !naturalSet.layer0().isEmpty()) {
            cloth0 = naturalSet.layer0().get(random.nextInt(naturalSet.layer0().size())).toString();
        } else if (freeSet != null && freeSet.layer0() != null && !freeSet.layer0().isEmpty()) {
            cloth0 = freeSet.layer0().get(random.nextInt(freeSet.layer0().size())).toString();
        }
        if (naturalSet != null && naturalSet.layer1() != null && !naturalSet.layer1().isEmpty()) {
            cloth1 = naturalSet.layer1().get(random.nextInt(naturalSet.layer1().size())).toString();
        } else if (freeSet != null && freeSet.layer1() != null && !freeSet.layer1().isEmpty()) {
            cloth1 = freeSet.layer1().get(random.nextInt(freeSet.layer1().size())).toString();
        }
        String heldItem = vt.travelBookHeldItem() != null ? vt.travelBookHeldItem() : "";
        String heldItemOffHand = vt.travelBookHeldItemOffHand() != null ? vt.travelBookHeldItemOffHand() : "";
        return new MockAppearance(vt.modelType().toByte(), texture, cloth0, cloth1, scale, heldItem, heldItemOffHand);
    }

    private record TitleResult(String text, boolean translatable) {
        static TitleResult literal(String text) {
            return new TitleResult(text, false);
        }

        static TitleResult translatable(String key) {
            return new TitleResult(key, true);
        }
    }

    private record MockAppearance(byte modelType, String texture, String cloth0, String cloth1, float scale, String heldItem, String heldItemOffHand) {
        static final MockAppearance EMPTY = new MockAppearance(0, "", "", "", 0.0f, "", "");
    }
}

