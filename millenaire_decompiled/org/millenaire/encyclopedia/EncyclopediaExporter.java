/*
 * Decompiled with CFR 0.150.
 * 
 * Could not load the following classes:
 *  javax.annotation.Nullable
 *  net.minecraft.resources.ResourceLocation
 *  net.minecraft.world.item.Item
 */
package org.millenaire.encyclopedia;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.function.UnaryOperator;
import javax.annotation.Nullable;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import org.millenaire.DisplayUtils;
import org.millenaire.building.BuildingPlanSet;
import org.millenaire.commerce.TradeGood;
import org.millenaire.commerce.TradeGoodsLoader;
import org.millenaire.culture.Culture;
import org.millenaire.culture.ModCultures;
import org.millenaire.culture.VillageType;
import org.millenaire.culture.VillagerType;
import org.millenaire.encyclopedia.EncyclopediaIndex;
import org.millenaire.encyclopedia.EncyclopediaLineMapper;
import org.millenaire.encyclopedia.EncyclopediaWriter;
import org.millenaire.encyclopedia.ExportColumn;
import org.millenaire.encyclopedia.ExportLine;
import org.millenaire.encyclopedia.IndexEntry;
import org.millenaire.encyclopedia.LangFiles;
import org.millenaire.encyclopedia.LocalizedText;
import org.millenaire.encyclopedia.SlotText;
import org.millenaire.encyclopedia.StructureInvariance;
import org.millenaire.encyclopedia.VillageData;
import org.millenaire.item.ItemHelper;
import org.millenaire.language.BuildingNameHelper;
import org.millenaire.village.TravelBookContentBuilder;
import org.millenaire.village.TravelBookLine;
import org.millenaire.village.TravelBookScreenState;

public final class EncyclopediaExporter {
    private static final Set<String> VANILLA_LANG_LOCALES = Set.of("en_us", "fr_fr");

    private EncyclopediaExporter() {
    }

    public static String segment(TravelBookScreenState state) {
        return switch (state) {
            case TravelBookScreenState.CULTURE -> "cultures";
            case TravelBookScreenState.VILLAGER_DETAIL -> "villagers";
            case TravelBookScreenState.VILLAGE_DETAIL -> "villages";
            case TravelBookScreenState.BUILDING_DETAIL -> "buildings";
            case TravelBookScreenState.TRADE_GOOD_DETAIL -> "tradegoods";
            default -> throw new IllegalArgumentException("Not an exportable detail state: " + String.valueOf((Object)state));
        };
    }

    public static String typeName(TravelBookScreenState state) {
        return EncyclopediaExporter.segment(state).toUpperCase(Locale.ROOT);
    }

    public static String simpleName(@Nullable String itemKey) {
        if (itemKey == null) {
            return "";
        }
        int slash = itemKey.lastIndexOf(47);
        return slash >= 0 ? itemKey.substring(slash + 1) : itemKey;
    }

    public static String itemRef(String cultureSlug, TravelBookScreenState state, String itemKey) {
        return cultureSlug + "-" + EncyclopediaExporter.segment(state) + "-" + EncyclopediaExporter.simpleName(itemKey);
    }

    private static boolean resolves(UnaryOperator<String> resolve, String key) {
        if (key == null || key.isEmpty()) {
            return false;
        }
        String v = (String)resolve.apply(key);
        return !v.equals(key) && !v.isEmpty();
    }

    public static LocalizedText labelForCulture(UnaryOperator<String> resolve, String cultureKey, String displayName) {
        String key = "culture.millenaire." + cultureKey;
        return EncyclopediaExporter.resolves(resolve, key) ? LocalizedText.key(key) : LocalizedText.lit(displayName);
    }

    public static VillagerLabel labelForVillager(boolean roleKeyResolves, String roleKey, String nativeName) {
        return roleKeyResolves ? new VillagerLabel(LocalizedText.key(roleKey), nativeName) : new VillagerLabel(LocalizedText.lit(nativeName), null);
    }

    public static LocalizedText labelForBuilding(@Nullable String translationKey, String simpleName) {
        return translationKey != null && !translationKey.isEmpty() ? LocalizedText.key(translationKey) : LocalizedText.lit(simpleName);
    }

    public static LocalizedText labelForVillage(String name) {
        return LocalizedText.lit(name);
    }

    public static LocalizedText labelForTradeGood(ResourceLocation itemLocation, @Nullable String descriptionId) {
        return descriptionId != null ? LocalizedText.key(descriptionId) : LocalizedText.lit(TravelBookContentBuilder.extractSimpleName(itemLocation));
    }

    @Nullable
    public static LocalizedText categoryLabel(Map<String, String> categoryNames, @Nullable String category) {
        if (category == null) {
            return null;
        }
        String i18nKey = categoryNames.getOrDefault(category, category);
        return EncyclopediaExporter.resolves(k -> i18nKey, category) ? LocalizedText.key(i18nKey) : LocalizedText.lit(TravelBookContentBuilder.formatCategoryName(category));
    }

    public static ItemAssembly buildItem(TravelBookScreenState state, String cultureKey, @Nullable String itemKey, List<String> dataLocales) {
        String ref = EncyclopediaExporter.itemRef(cultureKey, state, EncyclopediaExporter.simpleName(itemKey));
        LinkedHashMap<String, List<ExportLine>> skeletons = new LinkedHashMap<String, List<ExportLine>>();
        LinkedHashMap<String, SlotText> slotsPerLocale = new LinkedHashMap<String, SlotText>();
        for (String locale : dataLocales) {
            UnaryOperator<String> resolve = LangFiles.resolver(locale);
            List<TravelBookLine> lines = TravelBookContentBuilder.exportItemDetail(state, cultureKey, itemKey, resolve);
            SlotText slots = new SlotText();
            ArrayList<ExportLine> skeleton = new ArrayList<ExportLine>(lines.size());
            for (int i = 0; i < lines.size(); ++i) {
                skeleton.add(EncyclopediaLineMapper.map(lines.get(i), i, resolve, slots));
            }
            skeletons.put(locale, skeleton);
            slotsPerLocale.put(locale, slots);
        }
        List<ExportLine> structure = StructureInvariance.validate(ref, skeletons);
        return new ItemAssembly(structure, slotsPerLocale);
    }

    private static List<ItemDescriptor> enumerate(List<String> cultureKeys, UnaryOperator<String> resolve) {
        List sortedKeys = cultureKeys.stream().sorted().toList();
        ArrayList<ItemDescriptor> out = new ArrayList<ItemDescriptor>();
        for (String cultureKey : sortedKeys) {
            EncyclopediaExporter.enumerateCulture(out, cultureKey, resolve);
        }
        return out;
    }

    private static void enumerateCulture(List<ItemDescriptor> out, String cultureKey, UnaryOperator<String> resolve) {
        ResourceLocation cultureId = ResourceLocation.fromNamespaceAndPath((String)"millenaire", (String)cultureKey);
        Culture culture = ModCultures.getCulture(cultureId);
        if (culture == null) {
            return;
        }
        Map<String, String> categoryNames = culture.travelBookCategories().categoryNames();
        LocalizedText cultureLabel = EncyclopediaExporter.labelForCulture(resolve, cultureKey, culture.displayName());
        EncyclopediaExporter.addDescriptor(out, TravelBookScreenState.CULTURE, cultureKey, cultureKey, cultureKey, cultureLabel, null, "millenaire:" + cultureKey, null, categoryNames);
        List<VillagerType> villagers = ModCultures.getAllVillagerTypes().values().stream().filter(vt -> vt.culture().equals((Object)cultureId)).filter(VillagerType::travelBookDisplay).sorted(Comparator.comparing(VillagerType::nativeName)).toList();
        for (VillagerType villagerType : villagers) {
            String string = villagerType.id().getPath();
            String roleKey = DisplayUtils.resolveRoleKey(villagerType.id());
            VillagerLabel vl = EncyclopediaExporter.labelForVillager(EncyclopediaExporter.resolves(resolve, roleKey), roleKey, villagerType.nativeName());
            EncyclopediaExporter.addDescriptor(out, TravelBookScreenState.VILLAGER_DETAIL, cultureKey, string, EncyclopediaExporter.simpleName(string), vl.label(), vl.nativePrefix(), villagerType.id().toString(), villagerType.travelBookCategory(), categoryNames);
        }
        List<VillageType> villages = ModCultures.getAllVillageTypes().values().stream().filter(vt -> vt.culture().equals((Object)cultureId)).filter(VillageType::travelBookDisplay).sorted(Comparator.comparing(VillageType::name)).toList();
        for (VillageType villageType : villages) {
            String path = villageType.id().getPath();
            String iconKey = villageType.icon() != null ? villageType.icon() : villageType.id().toString();
            IndexEntry entry = EncyclopediaExporter.indexEntry(TravelBookScreenState.VILLAGE_DETAIL, cultureKey, EncyclopediaExporter.simpleName(path), EncyclopediaExporter.labelForVillage(villageType.name()), null, iconKey, null, categoryNames, out.size(), null);
            out.add(new ItemDescriptor(TravelBookScreenState.VILLAGE_DETAIL, cultureKey, path, entry, EncyclopediaExporter.buildVillageData(cultureKey, villageType)));
        }
        List<BuildingPlanSet> list = ModCultures.getAllBuildingPlanSets().values().stream().filter(bps -> bps.culture().equals((Object)cultureId)).filter(BuildingPlanSet::travelBookDisplay).filter(EncyclopediaExporter::isEncyclopediaBuilding).sorted(Comparator.comparing(BuildingPlanSet::nativeName)).toList();
        for (BuildingPlanSet bps2 : list) {
            String path = bps2.id().getPath();
            String simpleKey = EncyclopediaExporter.simpleName(path);
            LocalizedText label = EncyclopediaExporter.labelForBuilding(BuildingNameHelper.getTranslationKey(bps2.id()), simpleKey);
            String iconKey = bps2.icon() != null ? bps2.icon() : bps2.id().toString();
            ArrayList<String> residents = new ArrayList<String>(bps2.maleResidents());
            residents.addAll(bps2.femaleResidents());
            EncyclopediaExporter.addDescriptor(out, TravelBookScreenState.BUILDING_DETAIL, cultureKey, path, simpleKey, label, null, iconKey, bps2.travelBookCategory(), categoryNames, residents.isEmpty() ? null : residents);
        }
        List<TradeGood> list2 = TradeGoodsLoader.getGoods(cultureId).stream().filter(TradeGood::travelBookDisplay).sorted(Comparator.comparing(TradeGood::id)).toList();
        for (TradeGood good : list2) {
            Item item = ItemHelper.resolve(good.itemLocation());
            String descriptionId = item != null ? item.getDescriptionId() : null;
            LocalizedText label = EncyclopediaExporter.labelForTradeGood(good.itemLocation(), descriptionId);
            EncyclopediaExporter.addDescriptor(out, TravelBookScreenState.TRADE_GOOD_DETAIL, cultureKey, good.id(), good.id(), label, null, good.itemLocation().toString(), good.category(), categoryNames);
        }
    }

    private static boolean isEncyclopediaBuilding(BuildingPlanSet bps) {
        return !bps.isSubBuilding() && !bps.isWallSegment() && !bps.isBorderBuilding() && !bps.tags().contains("worldquest");
    }

    private static void addDescriptor(List<ItemDescriptor> out, TravelBookScreenState state, String cultureKey, String builderItemKey, String simpleKey, LocalizedText label, @Nullable String nativePrefix, String iconKey, @Nullable String category, Map<String, String> categoryNames) {
        EncyclopediaExporter.addDescriptor(out, state, cultureKey, builderItemKey, simpleKey, label, nativePrefix, iconKey, category, categoryNames, null);
    }

    private static void addDescriptor(List<ItemDescriptor> out, TravelBookScreenState state, String cultureKey, String builderItemKey, String simpleKey, LocalizedText label, @Nullable String nativePrefix, String iconKey, @Nullable String category, Map<String, String> categoryNames, @Nullable List<String> residents) {
        IndexEntry entry = EncyclopediaExporter.indexEntry(state, cultureKey, simpleKey, label, nativePrefix, iconKey, category, categoryNames, out.size(), residents);
        out.add(new ItemDescriptor(state, cultureKey, builderItemKey, entry, null));
    }

    private static IndexEntry indexEntry(TravelBookScreenState state, String cultureKey, String simpleKey, LocalizedText label, @Nullable String nativePrefix, String iconKey, @Nullable String category, Map<String, String> categoryNames, int displayOrder, @Nullable List<String> residents) {
        return new IndexEntry(EncyclopediaExporter.itemRef(cultureKey, state, simpleKey), cultureKey, EncyclopediaExporter.typeName(state), simpleKey, label, nativePrefix, iconKey, category, EncyclopediaExporter.categoryLabel(categoryNames, category), displayOrder, residents);
    }

    private static VillageData buildVillageData(String cultureKey, VillageType vt) {
        String type = vt.loneBuilding() ? "lone" : (vt.isMarvel() ? "marvel" : (vt.isHamlet() ? "hameau" : "regular"));
        LinkedHashMap<String, Map> byRole = new LinkedHashMap<String, Map>();
        for (VillageType.LayoutSlot slot : vt.layout()) {
            byRole.computeIfAbsent(slot.role(), k -> new LinkedHashMap()).merge(slot.plan(), 1, Integer::sum);
        }
        int male = 0;
        int female = 0;
        ArrayList<VillageData.RoleGroup> composition = new ArrayList<VillageData.RoleGroup>();
        for (Map.Entry roleEntry : byRole.entrySet()) {
            ArrayList<VillageData.Building> buildings = new ArrayList<VillageData.Building>();
            for (Map.Entry planEntry : ((Map)roleEntry.getValue()).entrySet()) {
                int residents;
                int count = (Integer)planEntry.getValue();
                BuildingPlanSet bps = ModCultures.getBuildingPlanSet((ResourceLocation)planEntry.getKey());
                int n = residents = bps != null ? bps.maleResidents().size() + bps.femaleResidents().size() : 0;
                if (bps != null) {
                    male += bps.maleResidents().size() * count;
                    female += bps.femaleResidents().size() * count;
                }
                buildings.add(new VillageData.Building(EncyclopediaExporter.simpleName(((ResourceLocation)planEntry.getKey()).getPath()), count, residents));
            }
            composition.add(new VillageData.RoleGroup((String)roleEntry.getKey(), buildings));
        }
        VillageData.Population population = new VillageData.Population(male + female, male, female);
        VillageData.Walls walls = vt.innerWallType() != null || vt.outerWallType() != null ? new VillageData.Walls(vt.innerWallType() != null ? vt.innerWallType().getPath() : null, vt.outerWallType() != null ? vt.outerWallType().getPath() : null, vt.innerWallRadius()) : null;
        VillageData.Economy economy = !vt.sellingPriceOverrides().isEmpty() || !vt.buyingPriceOverrides().isEmpty() ? new VillageData.Economy(EncyclopediaExporter.sortedOrNull(vt.sellingPriceOverrides()), EncyclopediaExporter.sortedOrNull(vt.buyingPriceOverrides())) : null;
        LinkedHashMap<String, String> terrain = new LinkedHashMap<String, String>();
        EncyclopediaExporter.putIfPresent(terrain, "forest", vt.forestQualifier());
        EncyclopediaExporter.putIfPresent(terrain, "hill", vt.hillQualifier());
        EncyclopediaExporter.putIfPresent(terrain, "mountain", vt.mountainQualifier());
        EncyclopediaExporter.putIfPresent(terrain, "desert", vt.desertQualifier());
        EncyclopediaExporter.putIfPresent(terrain, "lava", vt.lavaQualifier());
        EncyclopediaExporter.putIfPresent(terrain, "lake", vt.lakeQualifier());
        EncyclopediaExporter.putIfPresent(terrain, "ocean", vt.oceanQualifier());
        ArrayList<String> hamlets = null;
        if (!vt.hamlets().isEmpty()) {
            hamlets = new ArrayList<String>(vt.hamlets().size());
            for (ResourceLocation h : vt.hamlets()) {
                hamlets.add(EncyclopediaExporter.itemRef(cultureKey, TravelBookScreenState.VILLAGE_DETAIL, EncyclopediaExporter.simpleName(h.getPath())));
            }
        }
        List<String> biomes = vt.biomeTags().isEmpty() ? null : vt.biomeTags().stream().map(tag -> tag.location().getPath()).toList();
        return new VillageData(type, vt.radius(), vt.weight(), vt.playerControlled(), vt.carriesRaid(), population, composition, walls, economy, vt.qualifiers().isEmpty() ? null : List.copyOf(vt.qualifiers()), terrain.isEmpty() ? null : terrain, hamlets, biomes);
    }

    @Nullable
    private static Map<String, Integer> sortedOrNull(Map<String, Integer> map) {
        return map.isEmpty() ? null : new TreeMap<String, Integer>(map);
    }

    private static void putIfPresent(Map<String, String> map, String key, @Nullable String value) {
        if (value != null) {
            map.put(key, value);
        }
    }

    public static void exportAll(Path outDir, List<String> cultureKeys, List<String> dataLocales, String modVersion, String generatedAt) {
        UnaryOperator<String> refResolve = LangFiles.resolver("en_us");
        List<ItemDescriptor> descriptors = EncyclopediaExporter.enumerate(cultureKeys, refResolve);
        ArrayList<IndexEntry> entries = new ArrayList<IndexEntry>(descriptors.size());
        LinkedHashMap<String, VillageData> villageData = new LinkedHashMap<String, VillageData>();
        for (ItemDescriptor d : descriptors) {
            entries.add(d.entry());
            if (d.villageData() == null) continue;
            villageData.put(d.entry().itemRef(), d.villageData());
        }
        EncyclopediaIndex index = new EncyclopediaIndex(1, modVersion, generatedAt, entries);
        LinkedHashMap<String, List<ExportLine>> structures = new LinkedHashMap<String, List<ExportLine>>();
        LinkedHashMap<String, Map<String, Map<String, String>>> textByLocale = new LinkedHashMap<String, Map<String, Map<String, String>>>();
        for (String string : dataLocales) {
            textByLocale.put(string, new LinkedHashMap());
        }
        LinkedHashSet<String> vanillaKeys = new LinkedHashSet<String>();
        EncyclopediaExporter.collectVanillaKeysFromEntries(entries, vanillaKeys);
        for (ItemDescriptor d : descriptors) {
            ItemAssembly assembly = EncyclopediaExporter.buildItem(d.state(), d.cultureKey(), d.builderItemKey(), dataLocales);
            String ref = d.entry().itemRef();
            structures.put(ref, assembly.structure());
            EncyclopediaExporter.collectVanillaKeysFromLines(assembly.structure(), vanillaKeys);
            for (String locale : dataLocales) {
                SlotText slots = assembly.slotsPerLocale().get(locale);
                ((Map)textByLocale.get(locale)).put(ref, slots.values());
            }
        }
        LinkedHashMap<String, Map<String, String>> linkedHashMap = new LinkedHashMap<String, Map<String, String>>();
        for (String locale : LangFiles.locales()) {
            LinkedHashMap<String, String> lang = new LinkedHashMap<String, String>(LangFiles.modKeys(locale));
            if (VANILLA_LANG_LOCALES.contains(locale)) {
                UnaryOperator<String> resolve = LangFiles.resolver(locale);
                for (String key : vanillaKeys) {
                    String value = (String)resolve.apply(key);
                    if (value.equals(key)) continue;
                    lang.put(key, value);
                }
            }
            linkedHashMap.put(locale, lang);
        }
        EncyclopediaWriter.write(outDir, index, structures, textByLocale, linkedHashMap, villageData);
    }

    private static void collectVanillaKeysFromEntries(List<IndexEntry> entries, Set<String> out) {
        for (IndexEntry e : entries) {
            EncyclopediaExporter.collectVanillaKey(e.label(), out);
            EncyclopediaExporter.collectVanillaKey(e.categoryLabel(), out);
        }
    }

    private static void collectVanillaKeysFromLines(List<ExportLine> lines, Set<String> out) {
        for (ExportLine line : lines) {
            EncyclopediaExporter.collectVanillaKey(line.text(), out);
            EncyclopediaExporter.collectVanillaKey(line.referenceButtonLabel(), out);
            if (line.columns() == null) continue;
            for (ExportColumn col : line.columns()) {
                EncyclopediaExporter.collectVanillaKey(col.text(), out);
                EncyclopediaExporter.collectVanillaKey(col.referenceButtonLabel(), out);
            }
        }
    }

    private static void collectVanillaKey(@Nullable LocalizedText node, Set<String> out) {
        if (node == null || node.key() == null) {
            return;
        }
        String key = node.key();
        if ((key.startsWith("item.") || key.startsWith("block.")) && !key.contains(".millenaire.")) {
            out.add(key);
        }
    }

    public record VillagerLabel(LocalizedText label, String nativePrefix) {
    }

    public record ItemAssembly(List<ExportLine> structure, Map<String, SlotText> slotsPerLocale) {
    }

    private record ItemDescriptor(TravelBookScreenState state, String cultureKey, String builderItemKey, IndexEntry entry, @Nullable VillageData villageData) {
    }
}

