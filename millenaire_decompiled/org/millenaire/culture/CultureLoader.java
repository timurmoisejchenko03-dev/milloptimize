/*
 * Decompiled with CFR 0.150.
 * 
 * Could not load the following classes:
 *  com.google.gson.Gson
 *  com.google.gson.JsonArray
 *  com.google.gson.JsonElement
 *  com.google.gson.JsonObject
 *  com.google.gson.JsonParseException
 *  com.google.gson.JsonParser
 *  com.mojang.logging.LogUtils
 *  javax.annotation.Nullable
 *  net.minecraft.resources.ResourceLocation
 *  net.minecraft.server.level.ServerLevel
 *  net.minecraft.util.GsonHelper
 *  net.minecraft.world.item.DyeColor
 *  net.minecraft.world.level.ChunkPos
 *  org.slf4j.Logger
 */
package org.millenaire.culture;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.google.gson.JsonParser;
import com.mojang.logging.LogUtils;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.TreeSet;
import javax.annotation.Nullable;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.GsonHelper;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.level.ChunkPos;
import org.millenaire.building.BuildingInstance;
import org.millenaire.building.BuildingPlan;
import org.millenaire.building.BuildingPlanSet;
import org.millenaire.building.ConstructionTask;
import org.millenaire.building.PlacementStep;
import org.millenaire.commerce.ShopProfileLoader;
import org.millenaire.commerce.TradeGoodsLoader;
import org.millenaire.content.BuiltInCultures;
import org.millenaire.content.ContentFs;
import org.millenaire.content.ContentLoadReport;
import org.millenaire.content.ContentStatsReporter;
import org.millenaire.content.CustomContentIndex;
import org.millenaire.content.Resource;
import org.millenaire.culture.BuildingPlanSetLoader;
import org.millenaire.culture.CrossFolderConflictException;
import org.millenaire.culture.Culture;
import org.millenaire.culture.CultureContentScanner;
import org.millenaire.culture.CultureMetadataLoader;
import org.millenaire.culture.JsonLoaderUtils;
import org.millenaire.culture.ModCultures;
import org.millenaire.culture.NameLists;
import org.millenaire.culture.TravelBookCategories;
import org.millenaire.culture.TravelBookCategoryAssigner;
import org.millenaire.culture.VillageType;
import org.millenaire.culture.VillageTypeLoader;
import org.millenaire.culture.VillagerType;
import org.millenaire.culture.VillagerTypeLoader;
import org.millenaire.culture.WallTypeLoader;
import org.millenaire.dialogue.DialogueLoader;
import org.millenaire.dialogue.SentenceLoader;
import org.millenaire.goal.GoalRegistry;
import org.millenaire.goal.gathering.GatheringHandlerRegistry;
import org.millenaire.village.BrickColourTheme;
import org.millenaire.village.Village;
import org.millenaire.village.VillageSavedData;
import org.millenaire.world.BuildingPlacer;
import org.millenaire.world.PlacementConstraints;
import org.slf4j.Logger;

public final class CultureLoader {
    private static final Logger LOGGER = LogUtils.getLogger();
    static final Gson GSON = JsonLoaderUtils.GSON;
    public static final List<String> BUILTIN_CULTURES = BuiltInCultures.IDS;
    static volatile List<String> activeCultures = BUILTIN_CULTURES;
    private static volatile List<String> cachedPlayerLanguages = null;

    private CultureLoader() {
    }

    public static List<String> discoverCultures() {
        ArrayList<String> result = new ArrayList<String>(BUILTIN_CULTURES);
        Set<String> custom = CustomContentIndex.current().customCultureIds();
        result.addAll(custom);
        if (!custom.isEmpty()) {
            LOGGER.info("Discovered {} custom culture(s): {}", (Object)custom.size(), custom);
        }
        return result;
    }

    public static void loadAll() {
        ModCultures.clear();
        SentenceLoader.clear();
        DialogueLoader.clear();
        cachedPlayerLanguages = null;
        GatheringHandlerRegistry.clear();
        GatheringHandlerRegistry.registerDefaults();
        TradeGoodsLoader.clear();
        ShopProfileLoader.clear();
        ContentLoadReport.clear();
        BuildingPlanSetLoader.resetCrossFolderTracking();
        VillagerTypeLoader.resetCrossFolderTracking();
        VillageTypeLoader.resetCrossFolderTracking();
        CustomContentIndex.rebuild(BUILTIN_CULTURES);
        WallTypeLoader.loadAll();
        List<String> cultures = CultureLoader.discoverCultures();
        activeCultures = List.copyOf(cultures);
        for (String cultureName : cultures) {
            CultureLoader.loadCultureContent(cultureName);
        }
        CultureLoader.resolveVillagerTypeGoods();
        LOGGER.info("Mill\u00e9naire content loaded: {} cultures, {} plans ({} sets), {} villager types, {} village types", new Object[]{ModCultures.getAllCultures().size(), ModCultures.getAllBuildingPlans().size(), ModCultures.getAllBuildingPlanSets().size(), ModCultures.getAllVillagerTypes().size(), ModCultures.getAllVillageTypes().size()});
        ContentStatsReporter.capture(activeCultures, new HashSet<String>(BUILTIN_CULTURES));
    }

    private static void resolveVillagerTypeGoods() {
        for (Map.Entry<ResourceLocation, VillagerType> entry : ModCultures.getAllVillagerTypes().entrySet()) {
            VillagerType resolved = entry.getValue().withResolvedGoods();
            ModCultures.registerVillagerType(resolved);
        }
    }

    static void loadCultureContent(String cultureName) {
        try {
            CultureLoader.loadCulture(cultureName);
        }
        catch (JsonParseException e) {
            LOGGER.error("Error loading culture {}: {}", new Object[]{cultureName, e.getMessage(), e});
        }
        try {
            CultureContentScanner.scan("building_plan", cultureName);
            CultureContentScanner.scan("villager_type", cultureName);
            CultureContentScanner.scan("village_type", cultureName);
        }
        catch (CrossFolderConflictException e) {
            ResourceLocation cultureRl = e.culture() != null ? e.culture() : ResourceLocation.fromNamespaceAndPath((String)"millenaire", (String)cultureName);
            ModCultures.unregisterCulture(cultureRl);
            LOGGER.error("[Millenaire] Culture '{}' failed to load due to logical-id collision: {}. Registry rolled back; aborting remaining loaders for this culture.", (Object)cultureRl, (Object)e.getMessage());
            return;
        }
        try {
            CultureMetadataLoader.loadReputationLabels(cultureName);
        }
        catch (JsonParseException e) {
            LOGGER.error("Error loading reputation labels {}: {}", new Object[]{cultureName, e.getMessage(), e});
        }
        try {
            CultureMetadataLoader.loadCultureReputationLabels(cultureName);
        }
        catch (JsonParseException e) {
            LOGGER.error("Error loading culture reputation labels {}: {}", new Object[]{cultureName, e.getMessage(), e});
        }
        try {
            CultureMetadataLoader.loadNameLists(cultureName);
        }
        catch (JsonParseException e) {
            LOGGER.error("Error loading namelists {}: {}", new Object[]{cultureName, e.getMessage(), e});
        }
        CultureLoader.loadTradedGoods(cultureName);
        CultureLoader.loadShopProfiles(cultureName);
        CultureLoader.loadSentencesAndDialogues(cultureName);
        CultureLoader.injectExtraBuildings(ResourceLocation.fromNamespaceAndPath((String)"millenaire", (String)cultureName));
        TravelBookCategoryAssigner.autoAssign(ResourceLocation.fromNamespaceAndPath((String)"millenaire", (String)cultureName));
        CultureLoader.validateNameListReferences(ResourceLocation.fromNamespaceAndPath((String)"millenaire", (String)cultureName));
    }

    private static void loadTradedGoods(String cultureName) {
        ResourceLocation cultureId = ResourceLocation.fromNamespaceAndPath((String)"millenaire", (String)cultureName);
        try {
            ContentFs cultureFs = CustomContentIndex.current().forCulture(cultureName);
            List<Resource> resources = cultureFs.findAll("traded_goods.json");
            if (!resources.isEmpty()) {
                TradeGoodsLoader.loadFromResources(cultureId, resources);
            }
        }
        catch (Exception e) {
            LOGGER.error("Error loading traded_goods {}: {}", new Object[]{cultureName, e.getMessage(), e});
        }
    }

    private static void loadShopProfiles(String cultureName) {
        ResourceLocation cultureId = ResourceLocation.fromNamespaceAndPath((String)"millenaire", (String)cultureName);
        TreeSet<String> shopNames = new TreeSet<String>();
        for (BuildingPlan bp : ModCultures.getAllBuildingPlans().values()) {
            if (bp.shopId() == null || !bp.culture().equals((Object)cultureId)) continue;
            shopNames.add(bp.shopId());
        }
        if (shopNames.isEmpty()) {
            return;
        }
        ContentFs shopsFs = CustomContentIndex.current().forCulture(cultureName).sub("shops");
        int loaded = 0;
        for (String shopName : shopNames) {
            Optional<Resource> res = shopsFs.findFirst(shopName + ".json");
            if (res.isEmpty()) continue;
            try {
                InputStream spStream = res.get().open();
                try {
                    ShopProfileLoader.load(cultureId, shopName, spStream);
                    ++loaded;
                }
                finally {
                    if (spStream == null) continue;
                    spStream.close();
                }
            }
            catch (Exception e) {
                LOGGER.error("Error loading shop profile {}: {}", new Object[]{shopName, e.getMessage(), e});
            }
        }
        if (loaded > 0) {
            LOGGER.info("[Millenaire] Loaded {} shop profiles for culture {}", (Object)loaded, (Object)cultureName);
        }
    }

    public static void resetPlayerLanguageCacheForTesting() {
        cachedPlayerLanguages = null;
    }

    static List<String> discoverPlayerLanguages() {
        if (cachedPlayerLanguages != null) {
            return cachedPlayerLanguages;
        }
        LinkedHashSet<String> languages = new LinkedHashSet<String>();
        boolean fromManifest = CultureLoader.readPlayerLanguagesManifest(languages);
        if (!fromManifest && languages.isEmpty()) {
            languages.add("en_us");
            languages.add("fr_fr");
        }
        CultureLoader.addSubmodPlayerLanguages(languages);
        LOGGER.info("Loaded {} player languages: {}", (Object)languages.size(), languages);
        cachedPlayerLanguages = List.copyOf(languages);
        return cachedPlayerLanguages;
    }

    /*
     * Enabled aggressive exception aggregation
     */
    private static boolean readPlayerLanguagesManifest(LinkedHashSet<String> out) {
        ContentFs languagesFs = CustomContentIndex.current().forGlobalContent("languages");
        Optional<Resource> manifest = languagesFs.findFirst("_manifest.json");
        if (manifest.isEmpty()) {
            LOGGER.warn("Could not find languages/_manifest.json on the ContentFs overlay, falling back to en_us, fr_fr");
            return false;
        }
        try (InputStream is = manifest.get().open();){
            boolean bl;
            JsonArray arr;
            InputStreamReader reader;
            block17: {
                reader = new InputStreamReader(is, StandardCharsets.UTF_8);
                JsonObject obj = (JsonObject)GSON.fromJson((Reader)reader, JsonObject.class);
                JsonArray jsonArray = arr = obj != null ? obj.getAsJsonArray("languages") : null;
                if (arr != null && !arr.isEmpty()) break block17;
                LOGGER.warn("Language manifest is empty, falling back to en_us, fr_fr");
                boolean bl2 = false;
                reader.close();
                return bl2;
            }
            try {
                for (JsonElement e : arr) {
                    out.add(e.getAsString());
                }
                bl = true;
            }
            catch (Throwable throwable) {
                try {
                    reader.close();
                }
                catch (Throwable throwable2) {
                    throwable.addSuppressed(throwable2);
                }
                throw throwable;
            }
            reader.close();
            return bl;
        }
        catch (Exception e) {
            LOGGER.warn("Error reading language manifest, falling back to en_us, fr_fr: {}", (Object)e.getMessage());
            return false;
        }
    }

    private static void addSubmodPlayerLanguages(LinkedHashSet<String> out) {
        ContentFs languagesFs = CustomContentIndex.current().forGlobalContent("languages");
        languagesFs.walk("", Integer.MAX_VALUE).forEach(res -> {
            String head;
            String relPath = res.relPath();
            if (!relPath.startsWith(head = "languages/")) {
                return;
            }
            String tail = relPath.substring(head.length());
            int slash = tail.indexOf(47);
            if (slash <= 0) {
                return;
            }
            String locale = tail.substring(0, slash);
            if (locale.isEmpty() || locale.startsWith("_") || "native".equals(locale)) {
                return;
            }
            out.add(locale);
        });
    }

    private static void loadSentencesAndDialogues(String cultureName) {
        SentenceLoader.loadSentences(cultureName, "native");
        DialogueLoader.loadDialogues(cultureName, "native");
        for (String lang : CultureLoader.discoverPlayerLanguages()) {
            SentenceLoader.loadSentences(cultureName, lang);
            DialogueLoader.loadDialogues(cultureName, lang);
        }
    }

    public static void validateAll(GoalRegistry goalRegistry) {
        int warnings = 0;
        int checks = 0;
        LinkedHashSet<ResourceLocation> villageTypesToDisable = new LinkedHashSet<ResourceLocation>();
        for (VillageType vt : ModCultures.getAllVillageTypes().values()) {
            if (vt.weight() > 0 && vt.biomeTags().isEmpty()) {
                LOGGER.error("VillageType {} has weight={} but no biomeTags \u2014 will never spawn naturally (biome pre-filter requires at least one tag). Add biomeTags or set weight=0.", (Object)vt.id(), (Object)vt.weight());
                ++warnings;
            }
            ++checks;
        }
        for (VillageType vt : ModCultures.getAllVillageTypes().values()) {
            for (VillageType.LayoutSlot layoutSlot : vt.layout()) {
                ++checks;
                if (ModCultures.getBuildingPlanSet(layoutSlot.plan()) != null) continue;
                LOGGER.error("VillageType {}: layout slot references non-existent BuildingPlanSet {} \u2014 DISABLING this village type (would NPE at spawn)", (Object)vt.id(), (Object)layoutSlot.plan());
                villageTypesToDisable.add(vt.id());
                ++warnings;
            }
            for (ResourceLocation resourceLocation : vt.playerBuildings()) {
                ++checks;
                if (ModCultures.getBuildingPlanSet(resourceLocation) != null) continue;
                LOGGER.error("VillageType {}: playerBuilding references non-existent BuildingPlanSet: {}", (Object)vt.id(), (Object)resourceLocation);
                ++warnings;
            }
            for (ResourceLocation resourceLocation : vt.hamlets()) {
                ++checks;
                if (ModCultures.getVillageType(resourceLocation) != null) continue;
                LOGGER.error("VillageType {}: hamlets references non-existent VillageType: {}", (Object)vt.id(), (Object)resourceLocation);
                ++warnings;
            }
        }
        for (VillagerType villagerType : ModCultures.getAllVillagerTypes().values()) {
            for (ResourceLocation resourceLocation : villagerType.goals()) {
                ++checks;
                if (goalRegistry.get(resourceLocation) != null) continue;
                LOGGER.warn("VillagerType {}: missing goal in GoalRegistry: {}", (Object)villagerType.id(), (Object)resourceLocation);
                ++warnings;
            }
        }
        for (BuildingPlanSet set : ModCultures.getAllBuildingPlanSets().values()) {
            for (List list : set.variants().values()) {
                for (BuildingPlanSet.LevelDef levelDef : list) {
                    ++checks;
                    if (ModCultures.getBuildingPlan(levelDef.planId()) != null) continue;
                    LOGGER.warn("BuildingPlanSet {}: LevelDef references non-existent BuildingPlan: {}", (Object)set.id(), (Object)levelDef.planId());
                    ++warnings;
                }
            }
        }
        for (BuildingPlan plan : ModCultures.getAllBuildingPlans().values()) {
            String shopId = plan.shopId();
            if (shopId == null || shopId.isEmpty()) continue;
            ++checks;
            if (ShopProfileLoader.getProfile(plan.culture(), shopId) != null) continue;
            LOGGER.error("BuildingPlan {}: shopId '{}' has no loaded ShopProfile for culture {}", new Object[]{plan.id(), shopId, plan.culture()});
            ++warnings;
        }
        for (ResourceLocation id : villageTypesToDisable) {
            ModCultures.unregisterVillageType(id);
        }
        if (!villageTypesToDisable.isEmpty()) {
            LOGGER.error("Cross-reference validation DISABLED {} village type(s) with broken critical refs: {}", (Object)villageTypesToDisable.size(), villageTypesToDisable);
        }
        LOGGER.info("Cross-reference validation: {} warnings out of {} checks", (Object)warnings, (Object)checks);
    }

    public static void rehydrateConstructionTasks(ServerLevel level) {
        VillageSavedData data = VillageSavedData.get(level);
        for (Village village : data.getVillageManager().getAllVillages()) {
            VillageType villageType = ModCultures.getVillageType(village.getVillageTypeId());
            if (villageType != null) {
                village.resolveBrickTheme(villageType);
            }
            for (BuildingInstance building : village.getBuildings()) {
                BuildingPlan plan;
                if (building.getStatus() == BuildingInstance.Status.UNDER_CONSTRUCTION && building.getLevel() > 0) {
                    building.setStatus(BuildingInstance.Status.UPGRADING);
                    LOGGER.info("Migrated building {} from UNDER_CONSTRUCTION to UPGRADING (level {})", (Object)building.getPlanId(), (Object)building.getLevel());
                }
                if ((plan = ModCultures.getBuildingPlan(building.getPlanId())) == null) {
                    LOGGER.warn("Plan not found for rehydration: {}", (Object)building.getPlanId());
                    continue;
                }
                building.addRuntimeTags(plan.tags());
                building.resolveSpecialPoints(plan);
                building.initInventory();
                building.linkChestsToBuilding(level);
                if (!building.isBeingBuilt()) continue;
                if (building.getConstructionTask() != null) {
                    LOGGER.debug("Rehydration: {} \u2014 {} steps loaded from NBT, cursor at {}", new Object[]{plan.id(), building.getConstructionTask().totalSteps(), building.getConstructionTask().getNextStepIndex()});
                    continue;
                }
                List<PlacementStep> steps = BuildingPlacer.compilePlacementSteps(level, plan, building.getOrigin(), building.getRotation(), building);
                if (steps.isEmpty()) {
                    LOGGER.warn("No steps compiled for plan {} \u2014 missing template?", (Object)plan.id());
                    continue;
                }
                building.setConstructionTask(new ConstructionTask(steps, 0));
                LOGGER.debug("Rehydration (legacy save): {} \u2014 {} steps recompiled from scratch", (Object)plan.id(), (Object)steps.size());
            }
            if (CultureLoader.allVillageChunksLoaded(level, village)) {
                village.rebuildWaypointGraph(level);
                continue;
            }
            LOGGER.info("Skipping waypoint graph rebuild for village {} \u2014 some chunks not loaded yet", (Object)village.getId());
        }
    }

    private static boolean allVillageChunksLoaded(ServerLevel level, Village village) {
        for (ChunkPos cp : village.computeVillageChunks()) {
            if (level.hasChunk(cp.x, cp.z)) continue;
            return false;
        }
        return true;
    }

    private static void loadCulture(String name) {
        String displayName;
        JsonObject json;
        ResourceLocation id = ResourceLocation.fromNamespaceAndPath((String)"millenaire", (String)name);
        ContentFs cultureFs = CustomContentIndex.current().forCulture(name);
        List<Resource> allCultureJsons = cultureFs.findAll("culture.json");
        if (allCultureJsons.isEmpty()) {
            String path = "/millenaire/cultures/" + name + "/culture.json";
            json = CultureLoader.readJson(path);
        } else {
            json = CultureLoader.mergeCultureJsons(allCultureJsons);
        }
        if (json == null) {
            return;
        }
        if (json.has("display_name")) {
            displayName = GsonHelper.getAsString((JsonObject)json, (String)"display_name");
        } else {
            displayName = name.isEmpty() ? name : Character.toUpperCase(name.charAt(0)) + name.substring(1);
            LOGGER.warn("Culture '{}': missing 'display_name', falling back to '{}'", (Object)name, (Object)displayName);
        }
        ResourceLocation panelTexture = json.has("panel_texture") ? ResourceLocation.parse((String)GsonHelper.getAsString((JsonObject)json, (String)"panel_texture")) : Culture.DEFAULT_PANEL_TEXTURE;
        String nativeLanguage = GsonHelper.getAsString((JsonObject)json, (String)"native_language", (String)"en");
        TravelBookCategories travelBookCategories = TravelBookCategories.EMPTY;
        if (json.has("travel_book")) {
            travelBookCategories = CultureLoader.parseTravelBookCategories(GsonHelper.getAsJsonObject((JsonObject)json, (String)"travel_book"));
        }
        List<String> knownCrops = json.has("known_crops") ? JsonLoaderUtils.parseStringList(GsonHelper.getAsJsonArray((JsonObject)json, (String)"known_crops")) : List.of();
        List<String> knownHuntingDrops = json.has("known_hunting_drops") ? JsonLoaderUtils.parseStringList(GsonHelper.getAsJsonArray((JsonObject)json, (String)"known_hunting_drops")) : List.of();
        Optional<ResourceLocation> mapIcon = CultureLoader.parseMapIcon(json);
        String cultureBannerNbt = json.has("culture_banner_nbt") ? GsonHelper.getAsString((JsonObject)json, (String)"culture_banner_nbt") : null;
        ModCultures.registerCulture(new Culture(id, displayName, panelTexture, nativeLanguage, travelBookCategories, knownCrops, knownHuntingDrops, mapIcon, cultureBannerNbt));
    }

    static Optional<ResourceLocation> parseMapIcon(JsonObject json) {
        if (!json.has("map_icon")) {
            return Optional.empty();
        }
        String raw = GsonHelper.getAsString((JsonObject)json, (String)"map_icon");
        return Optional.of(ResourceLocation.parse((String)raw));
    }

    @Nullable
    private static JsonObject mergeCultureJsons(List<Resource> resources) {
        JsonObject merged = null;
        for (int i = resources.size() - 1; i >= 0; --i) {
            JsonObject layer;
            Resource res = resources.get(i);
            try (InputStream is = res.open();){
                layer = (JsonObject)GSON.fromJson((Reader)new InputStreamReader(is, StandardCharsets.UTF_8), JsonObject.class);
            }
            catch (Exception e) {
                LOGGER.error("Error reading {} from {}: {}", new Object[]{res.relPath(), res.source().displayName(), e.getMessage(), e});
                continue;
            }
            if (layer == null) continue;
            if (merged == null) {
                merged = layer;
                continue;
            }
            for (Map.Entry e : layer.entrySet()) {
                merged.add((String)e.getKey(), (JsonElement)e.getValue());
            }
        }
        return merged;
    }

    static void injectExtraBuildings(ResourceLocation cultureId) {
        Set<String> excludedCategories = Set.of("townhalls", "lone", "loneold", "player", "walls");
        ArrayList<BuildingPlanSet> cultureSets = new ArrayList<BuildingPlanSet>();
        for (BuildingPlanSet set : ModCultures.getAllBuildingPlanSets().values()) {
            if (!set.culture().equals((Object)cultureId) || excludedCategories.contains(set.category()) || set.isSubBuilding() || set.isTownHall()) continue;
            cultureSets.add(set);
        }
        if (cultureSets.isEmpty()) {
            return;
        }
        for (VillageType vt : new ArrayList<VillageType>(ModCultures.getAllVillageTypes().values())) {
            if (!vt.culture().equals((Object)cultureId) || !vt.allowExtraBuildings()) continue;
            HashMap<ResourceLocation, Integer> existingCounts = new HashMap<ResourceLocation, Integer>();
            for (VillageType.LayoutSlot layoutSlot : vt.layout()) {
                existingCounts.merge(layoutSlot.plan(), 1, Integer::sum);
            }
            ArrayList<VillageType.LayoutSlot> extraSlots = new ArrayList<VillageType.LayoutSlot>();
            for (BuildingPlanSet set : cultureSets) {
                int slotsToAdd;
                if (vt.neverBuildings().contains(set.buildingId())) continue;
                int existing = existingCounts.getOrDefault((Object)set.id(), 0);
                if (set.maxCount() == 0 || (slotsToAdd = set.maxCount() - existing) <= 0) continue;
                for (int i = 0; i < slotsToAdd; ++i) {
                    extraSlots.add(new VillageType.LayoutSlot(set.id(), null, null, "extra", -1.0, -1.0, 1, Map.of(), Map.of(), PlacementConstraints.getDefaultClearMargin(), null));
                }
            }
            if (extraSlots.isEmpty()) continue;
            ArrayList<VillageType.LayoutSlot> arrayList = new ArrayList<VillageType.LayoutSlot>(vt.layout());
            arrayList.addAll(extraSlots);
            ModCultures.registerVillageType(vt.withLayout(arrayList));
            LOGGER.debug("Injected {} extra building slots into {}", (Object)extraSlots.size(), (Object)vt.id());
        }
    }

    private static void validateNameListReferences(ResourceLocation cultureId) {
        NameLists nameLists = ModCultures.getNameLists(cultureId);
        for (VillageType vt : ModCultures.getAllVillageTypes().values()) {
            String nameListKey;
            if (!vt.culture().equals((Object)cultureId) || (nameListKey = vt.nameList()) == null) continue;
            if (nameLists == null) {
                LOGGER.error("[Millenaire] VillageType {} references nameList '{}' but culture {} has no namelists loaded!", new Object[]{vt.id(), nameListKey, cultureId});
                continue;
            }
            if (nameLists.randomFrom(nameListKey) != null) continue;
            LOGGER.error("[Millenaire] VillageType {} references nameList '{}' but it is missing or empty in culture {}", new Object[]{vt.id(), nameListKey, cultureId});
        }
    }

    static List<BrickColourTheme.WeightedColor> parseWeightedColorList(JsonArray array) {
        ArrayList<BrickColourTheme.WeightedColor> list = new ArrayList<BrickColourTheme.WeightedColor>();
        for (JsonElement e : array) {
            JsonObject obj = e.getAsJsonObject();
            DyeColor color = CultureLoader.parseDyeColor(GsonHelper.getAsString((JsonObject)obj, (String)"color"));
            int w = GsonHelper.getAsInt((JsonObject)obj, (String)"weight");
            if (color == null) continue;
            list.add(new BrickColourTheme.WeightedColor(color, w));
        }
        return list;
    }

    static DyeColor parseDyeColor(String name) {
        if ("silver".equalsIgnoreCase(name)) {
            return DyeColor.LIGHT_GRAY;
        }
        for (DyeColor color : DyeColor.values()) {
            if (!color.getSerializedName().equals(name)) continue;
            return color;
        }
        LOGGER.warn("Unknown DyeColor: {}", (Object)name);
        return null;
    }

    @Nullable
    static String pluralTypeName(String contentType) {
        return switch (contentType) {
            case "building_plan" -> "buildings";
            case "villager_type" -> "villagers";
            case "village_type" -> "villages";
            default -> null;
        };
    }

    static Set<String> resolveDisabledIds(String culture, String contentType) {
        String pluralType = CultureLoader.pluralTypeName(contentType);
        if (pluralType == null) {
            return Set.of();
        }
        ContentFs cultureFs = CustomContentIndex.current().forCulture(culture);
        List<Resource> manifests = cultureFs.findAll(pluralType + "/_disabled.json");
        if (manifests.isEmpty()) {
            return Set.of();
        }
        HashSet<String> out = new HashSet<String>();
        for (int i = manifests.size() - 1; i >= 0; --i) {
            Resource res = manifests.get(i);
            try (InputStream in = res.open();){
                String body = new String(in.readAllBytes(), StandardCharsets.UTF_8);
                JsonElement root = JsonParser.parseString((String)body);
                if (!root.isJsonArray()) {
                    LOGGER.warn("{} from {} must be a JSON array of strings; ignoring", (Object)res.relPath(), (Object)res.source().displayName());
                    continue;
                }
                for (JsonElement e : root.getAsJsonArray()) {
                    if (!e.isJsonPrimitive() || !e.getAsJsonPrimitive().isString()) continue;
                    out.add(e.getAsString());
                }
                continue;
            }
            catch (Exception e) {
                LOGGER.warn("Could not parse {} from {}: {}", new Object[]{res.relPath(), res.source().displayName(), e.getMessage()});
            }
        }
        return out;
    }

    static String formatSimpleName(String contentId) {
        String formatted;
        String path = contentId;
        int underscore = path.indexOf(95);
        if (underscore >= 0) {
            path = path.substring(underscore + 1);
        }
        return (formatted = path.replace('_', ' ')).isEmpty() ? contentId : Character.toUpperCase(formatted.charAt(0)) + formatted.substring(1);
    }

    @Nullable
    static JsonObject readJsonFromContentFs(ContentFs fs, String relPath) {
        JsonObject jsonObject;
        block10: {
            if (fs == null || relPath == null) {
                return null;
            }
            Optional<Resource> res = fs.findFirst(relPath);
            if (res.isEmpty()) {
                return null;
            }
            InputStream is = res.get().open();
            try {
                jsonObject = (JsonObject)GSON.fromJson((Reader)new InputStreamReader(is, StandardCharsets.UTF_8), JsonObject.class);
                if (is == null) break block10;
            }
            catch (Throwable throwable) {
                try {
                    if (is != null) {
                        try {
                            is.close();
                        }
                        catch (Throwable throwable2) {
                            throwable.addSuppressed(throwable2);
                        }
                    }
                    throw throwable;
                }
                catch (Exception e) {
                    LOGGER.error("Error reading {} from {}: {}", new Object[]{res.get().relPath(), res.get().source().displayName(), e.getMessage(), e});
                    return null;
                }
            }
            is.close();
        }
        return jsonObject;
    }

    @Nullable
    static JsonObject readJson(String classpathPath) {
        if (classpathPath == null) {
            return null;
        }
        String relPath = CultureLoader.classpathPathToRelPath(classpathPath);
        if (relPath == null) {
            LOGGER.error("Content file path does not start with /millenaire/: {}", (Object)classpathPath);
            return null;
        }
        ContentFs root = CustomContentIndex.current().root();
        Optional<Resource> res = root.findFirst(relPath);
        if (res.isPresent()) {
            JsonObject jsonObject;
            block13: {
                InputStream is = res.get().open();
                try {
                    jsonObject = (JsonObject)GSON.fromJson((Reader)new InputStreamReader(is, StandardCharsets.UTF_8), JsonObject.class);
                    if (is == null) break block13;
                }
                catch (Throwable throwable) {
                    try {
                        if (is != null) {
                            try {
                                is.close();
                            }
                            catch (Throwable throwable2) {
                                throwable.addSuppressed(throwable2);
                            }
                        }
                        throw throwable;
                    }
                    catch (Exception e) {
                        LOGGER.error("Error reading {} from {}: {}", new Object[]{res.get().relPath(), res.get().source().displayName(), e.getMessage(), e});
                        return null;
                    }
                }
                is.close();
            }
            return jsonObject;
        }
        String customCulture = CultureLoader.extractCustomCultureFromPath(classpathPath);
        if (customCulture != null) {
            ContentLoadReport.recordMissingClasspathFile(customCulture, classpathPath);
            LOGGER.debug("Content file not found on overlay (custom culture '{}'): {}", (Object)customCulture, (Object)classpathPath);
        } else {
            LOGGER.error("Content file not found: {}", (Object)classpathPath);
        }
        return null;
    }

    @Nullable
    static String classpathPathToRelPath(String classpathPath) {
        String tail;
        if (classpathPath == null) {
            return null;
        }
        String string = tail = classpathPath.startsWith("/") ? classpathPath.substring(1) : classpathPath;
        if (!tail.startsWith("millenaire/")) {
            return null;
        }
        return tail.substring("millenaire/".length());
    }

    @Nullable
    static String extractCustomCultureFromPath(String classpathPath) {
        String candidate;
        String tail;
        if (classpathPath == null) {
            return null;
        }
        String string = tail = classpathPath.startsWith("/") ? classpathPath.substring(1) : classpathPath;
        if (!tail.startsWith("millenaire/")) {
            return null;
        }
        int firstSlash = (tail = tail.substring("millenaire/".length())).indexOf(47);
        if (firstSlash < 0) {
            return null;
        }
        String type = tail.substring(0, firstSlash);
        String rest = tail.substring(firstSlash + 1);
        switch (type) {
            case "cultures": {
                String string2;
                int sep = rest.indexOf(47);
                if (sep < 0) {
                    string2 = rest;
                    break;
                }
                string2 = rest.substring(0, sep);
                break;
            }
            default: {
                String string2 = candidate = null;
            }
        }
        if (candidate == null || candidate.isEmpty()) {
            return null;
        }
        if (BUILTIN_CULTURES.contains(candidate)) {
            return null;
        }
        return activeCultures.contains(candidate) ? candidate : null;
    }

    private static TravelBookCategories parseTravelBookCategories(JsonObject json) {
        List<String> villagerCategories = json.has("villager_categories") ? JsonLoaderUtils.parseStringList(GsonHelper.getAsJsonArray((JsonObject)json, (String)"villager_categories")) : List.of();
        List<String> buildingCategories = json.has("building_categories") ? JsonLoaderUtils.parseStringList(GsonHelper.getAsJsonArray((JsonObject)json, (String)"building_categories")) : List.of();
        List<String> tradeGoodCategories = json.has("trade_good_categories") ? JsonLoaderUtils.parseStringList(GsonHelper.getAsJsonArray((JsonObject)json, (String)"trade_good_categories")) : List.of();
        HashMap<String, String> categoryIcons = new HashMap<String, String>();
        if (json.has("category_icons")) {
            JsonObject iconsObj = GsonHelper.getAsJsonObject((JsonObject)json, (String)"category_icons");
            for (Map.Entry entry : iconsObj.entrySet()) {
                categoryIcons.put((String)entry.getKey(), ((JsonElement)entry.getValue()).getAsString());
            }
        }
        HashMap<String, String> categoryNames = new HashMap<String, String>();
        if (json.has("category_names")) {
            JsonObject namesObj = GsonHelper.getAsJsonObject((JsonObject)json, (String)"category_names");
            for (Map.Entry entry : namesObj.entrySet()) {
                categoryNames.put((String)entry.getKey(), ((JsonElement)entry.getValue()).getAsString());
            }
        }
        return new TravelBookCategories(villagerCategories, buildingCategories, tradeGoodCategories, categoryIcons, categoryNames);
    }

    static List<String> parseStringOrArray(JsonElement element) {
        if (element.isJsonArray()) {
            return JsonLoaderUtils.parseStringList(element.getAsJsonArray());
        }
        return List.of(element.getAsString());
    }

    static Map<ResourceLocation, Integer> parseResourceIntMap(JsonObject obj) {
        HashMap<ResourceLocation, Integer> map = new HashMap<ResourceLocation, Integer>();
        for (Map.Entry entry : obj.entrySet()) {
            map.put(ResourceLocation.parse((String)((String)entry.getKey())), ((JsonElement)entry.getValue()).getAsInt());
        }
        return map;
    }

    static List<ResourceLocation> parseResourceLocationList(JsonObject parent, String key) {
        ArrayList<ResourceLocation> list = new ArrayList<ResourceLocation>();
        if (parent.has(key)) {
            for (JsonElement e : GsonHelper.getAsJsonArray((JsonObject)parent, (String)key)) {
                list.add(ResourceLocation.parse((String)e.getAsString()));
            }
        }
        return list;
    }
}

