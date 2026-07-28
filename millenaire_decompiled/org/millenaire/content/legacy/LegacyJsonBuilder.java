/*
 * Decompiled with CFR 0.150.
 */
package org.millenaire.content.legacy;

import java.awt.image.BufferedImage;
import java.io.IOException;
import java.lang.invoke.CallSite;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import javax.imageio.ImageIO;
import org.millenaire.building.MockBlockExtractor;
import org.millenaire.building.SpecialPoint;
import org.millenaire.content.legacy.BiomeMapper;
import org.millenaire.content.legacy.ItemIdMapper;
import org.millenaire.content.legacy.LegacyDataParser;
import org.millenaire.content.legacy.LegacyIdCanonicaliser;
import org.millenaire.content.legacy.PngToNbtConverter;

public final class LegacyJsonBuilder {
    private static final Map<String, String> LEGACY_TAG_FIXES = Map.of("helpinattacks", "helpInAttacks", "HelpInAttacks", "helpInAttacks", "leasure", "leisure");
    private static final Map<String, List<String>> VILLAGER_EXTRA_GOALS = Map.of("smelter_byzantine", List.of("millenaire:cook_iron_ore"));
    private static final Set<String> SKIPPED_GIFT_PLANS = Set.of("gifthouse");
    public static final Map<String, String> LEGACY_TO_GOOD_ID;
    public static final Map<String, String> LEGACY_ITEM_ID_MAP;
    public static final Map<String, String> LEGACY_CROP_BLOCK_MAP;
    public static final Map<String, String> LEGACY_FLOWER_MAP;
    public static final Map<String, String> CROP_SOIL_MAP;
    public static final Map<String, String> CROP_SOIL_SUBTYPE_MAP;
    public static final Map<String, String> CROP_SEED_MAP;
    public static final Map<String, String> MINING_SOURCE_MAP;
    public static final Map<String, String> LEGACY_ANIMAL_MAP;
    public static final Map<String, String[]> LEGACY_COOKING_MAP;

    private LegacyJsonBuilder() {
    }

    private static void putIfNot(Map<String, Object> map, String key, Object value, Object defaultValue) {
        if (!Objects.equals(value, defaultValue)) {
            map.put(key, value);
        }
    }

    static String translateLegacyCardinal(String legacyName) {
        return switch (legacyName.toLowerCase(Locale.ROOT)) {
            case "north" -> "west";
            case "west" -> "south";
            case "south" -> "east";
            case "east" -> "north";
            default -> legacyName;
        };
    }

    static int convertLegacyBuildingOrientation(int legacyBuildingOrientation) {
        return Math.floorMod(legacyBuildingOrientation - 1, 4);
    }

    public static Map<String, Object> buildBuildingPlan(LegacyDataParser.BuildingWithVariants building, String culture, ItemIdMapper items, Map<String, PngToNbtConverter.ConversionResult> conversionResults) {
        return LegacyJsonBuilder.buildBuildingPlan(building, culture, items, conversionResults, Set.of());
    }

    /*
     * WARNING - void declaration
     */
    public static Map<String, Object> buildBuildingPlan(LegacyDataParser.BuildingWithVariants building, String culture, ItemIdMapper items, Map<String, PngToNbtConverter.ConversionResult> conversionResults, Set<String> centralBuildingsForCulture) {
        LegacyDataParser.BuildingMeta meta = building.meta();
        LinkedHashMap<String, Object> json = new LinkedHashMap<String, Object>();
        json.put("culture", "millenaire:" + culture);
        json.put("building_id", LegacyJsonBuilder.sanitize(meta.baseName()));
        json.put("category", meta.category());
        if (meta.isSubBuilding()) {
            json.put("is_sub_building", true);
        }
        if (meta.isWallSegment()) {
            json.put("is_wall_segment", true);
        }
        if (meta.isBorderBuilding()) {
            json.put("is_border_building", true);
        }
        boolean hasMarvelThTag = meta.levels().stream().anyMatch(lm -> lm.tags().contains("marvelth"));
        boolean isCentralInVillage = centralBuildingsForCulture.contains(LegacyJsonBuilder.sanitize(meta.baseName()));
        if ("townhalls".equals(meta.category()) || hasMarvelThTag || isCentralInVillage) {
            json.put("is_town_hall", !meta.isSubBuilding());
        }
        json.put("native_name", meta.levels().getFirst().nativeName());
        if (meta.shop() != null) {
            json.put("shop", LegacyIdCanonicaliser.shopRefId(meta.shop()));
        }
        if (meta.maxCount() != 1) {
            json.put("max_count", meta.maxCount());
        }
        if (meta.minDistance() != 0.0) {
            json.put("min_distance", meta.minDistance());
        }
        if (meta.maxDistance() != 1.0) {
            json.put("max_distance", meta.maxDistance());
        }
        if (!meta.males().isEmpty()) {
            json.put("male", meta.males().stream().map(id -> LegacyIdCanonicaliser.villagerTypeRefId(LegacyJsonBuilder.stripCulturePrefix(culture, id))).toList());
        }
        if (!meta.females().isEmpty()) {
            json.put("female", meta.females().stream().map(id -> LegacyIdCanonicaliser.villagerTypeRefId(LegacyJsonBuilder.stripCulturePrefix(culture, id))).toList());
        }
        if (!meta.visitors().isEmpty()) {
            json.put("visitors", meta.visitors().stream().map(id -> LegacyIdCanonicaliser.villagerTypeRefId(LegacyJsonBuilder.stripCulturePrefix(culture, id))).toList());
        }
        if (!meta.tags().isEmpty()) {
            json.put("tags", meta.tags().stream().map(LegacyJsonBuilder::normalizeLegacyTag).toList());
        }
        if (!meta.startingSubBuildings().isEmpty()) {
            json.put("starting_sub_buildings", meta.startingSubBuildings().stream().map(LegacyIdCanonicaliser::buildingPlanRefId).toList());
        }
        if (meta.icon() != null) {
            json.put("icon", items.resolve(culture, meta.icon()).orElse(meta.icon()));
        }
        if (meta.fixedOrientation() != null) {
            json.put("fixed_orientation", LegacyJsonBuilder.translateLegacyCardinal(meta.fixedOrientation()));
        }
        if (meta.areaToClear() > 0) {
            json.put("area_to_clear", meta.areaToClear());
        }
        if (meta.areaToClearLengthBefore() >= 0) {
            json.put("area_to_clear_length_before", meta.areaToClearLengthBefore());
        }
        if (meta.areaToClearLengthAfter() >= 0) {
            json.put("area_to_clear_length_after", meta.areaToClearLengthAfter());
        }
        if (meta.areaToClearWidthBefore() >= 0) {
            json.put("area_to_clear_width_before", meta.areaToClearWidthBefore());
        }
        if (meta.areaToClearWidthAfter() >= 0) {
            json.put("area_to_clear_width_after", meta.areaToClearWidthAfter());
        }
        if (!meta.farFromTags().isEmpty()) {
            json.put("far_from_tags", meta.farFromTags().stream().map(LegacyJsonBuilder::normalizeFarFromTag).toList());
        }
        if (meta.price() > 0) {
            json.put("price", meta.price());
        }
        if (meta.reputation() > 0) {
            json.put("reputation", meta.reputation());
        }
        if (meta.isGift()) {
            json.put("is_gift", true);
        }
        if (!meta.randomBrickColours().isEmpty()) {
            LinkedHashMap rbcJson = new LinkedHashMap();
            for (String string : meta.randomBrickColours()) {
                String[] parts = string.split(";", 2);
                if (parts.length != 2) continue;
                String inputColor = LegacyJsonBuilder.remapLegacyColorName(parts[0].trim());
                String[] outputs = parts[1].split(",");
                ArrayList pool = new ArrayList();
                for (String out : outputs) {
                    String[] cw = out.trim().split(":");
                    if (cw.length != 2) continue;
                    LinkedHashMap<String, Object> entry = new LinkedHashMap<String, Object>();
                    entry.put("color", LegacyJsonBuilder.remapLegacyColorName(cw[0].trim()));
                    entry.put("weight", Integer.parseInt(cw[1].trim()));
                    pool.add(entry);
                }
                rbcJson.put(inputColor, pool);
            }
            if (!rbcJson.isEmpty()) {
                json.put("random_brick_colours", rbcJson);
            }
        }
        if (!meta.startingGoods().isEmpty()) {
            ArrayList startingGoods = new ArrayList();
            for (LegacyDataParser.StartingGoodMeta startingGoodMeta : meta.startingGoods()) {
                LinkedHashMap<String, Object> sgJson = new LinkedHashMap<String, Object>();
                Optional<String> modernItem = items.resolveForBuildingCost(culture, startingGoodMeta.item(), meta.baseName());
                sgJson.put("item", modernItem.orElse(startingGoodMeta.item()));
                sgJson.put("probability", startingGoodMeta.probability());
                sgJson.put("fixed", startingGoodMeta.fixedNumber());
                sgJson.put("random", startingGoodMeta.randomNumber());
                startingGoods.add(sgJson);
            }
            json.put("starting_goods", startingGoods);
        }
        for (Map.Entry entry : building.variantMetas().entrySet()) {
            LegacyDataParser.BuildingMeta buildingMeta = (LegacyDataParser.BuildingMeta)entry.getValue();
            if (buildingMeta.areaToClear() == meta.areaToClear() && buildingMeta.areaToClearLengthBefore() == meta.areaToClearLengthBefore() && buildingMeta.areaToClearLengthAfter() == meta.areaToClearLengthAfter() && buildingMeta.areaToClearWidthBefore() == meta.areaToClearWidthBefore() && buildingMeta.areaToClearWidthAfter() == meta.areaToClearWidthAfter()) continue;
            System.out.println("  [WARN] areaToClear mismatch in variant " + (String)entry.getKey() + " of " + meta.baseName());
        }
        ArrayList variants = new ArrayList();
        for (Map.Entry<String, List<Path>> entry : building.variantPngs().entrySet()) {
            String variant = entry.getKey().toLowerCase();
            String variantKey = entry.getKey();
            List<Path> pngs = entry.getValue();
            LegacyDataParser.BuildingMeta varMeta = building.metaForVariant(variantKey);
            LinkedHashMap<String, Object> variantJson = new LinkedHashMap<String, Object>();
            variantJson.put("variant", variant);
            variantJson.put("building_orientation", LegacyJsonBuilder.convertLegacyBuildingOrientation(varMeta.buildingOrientation()));
            int[] levelGroundLevels = new int[pngs.size()];
            ArrayList levelCumulativeTags = new ArrayList(pngs.size());
            for (int i = 0; i < pngs.size(); ++i) {
                int gl = 0;
                if (i < varMeta.levels().size()) {
                    gl = varMeta.levels().get(i).startLevel();
                } else if (!varMeta.levels().isEmpty()) {
                    gl = varMeta.levels().get(varMeta.levels().size() - 1).startLevel();
                }
                levelGroundLevels[i] = gl;
                ArrayList<String> cumTags = new ArrayList<String>();
                int maxJ = Math.min(i, varMeta.levels().size() - 1);
                for (int j = 0; j <= maxJ; ++j) {
                    for (String t : varMeta.levels().get(j).tags()) {
                        String normalized = LegacyJsonBuilder.normalizeLegacyTag(t);
                        if (cumTags.contains(normalized)) continue;
                        cumTags.add(normalized);
                    }
                    for (String t : varMeta.levels().get(j).clearTags()) {
                        cumTags.remove(LegacyJsonBuilder.normalizeLegacyTag(t));
                    }
                }
                levelCumulativeTags.add(cumTags);
            }
            boolean groundLevelConstant = true;
            for (int i = 1; i < levelGroundLevels.length; ++i) {
                if (levelGroundLevels[i] == levelGroundLevels[0]) continue;
                groundLevelConstant = false;
                break;
            }
            boolean tagsConstant = true;
            for (int i = 1; i < levelCumulativeTags.size(); ++i) {
                if (((List)levelCumulativeTags.get(i)).equals(levelCumulativeTags.get(0))) continue;
                tagsConstant = false;
                break;
            }
            if (groundLevelConstant && levelGroundLevels.length > 0 && levelGroundLevels[0] != 0) {
                variantJson.put("ground_level", levelGroundLevels[0]);
            }
            if (tagsConstant && !levelCumulativeTags.isEmpty() && !((List)levelCumulativeTags.get(0)).isEmpty()) {
                variantJson.put("tags", levelCumulativeTags.get(0));
            }
            ArrayList levels = new ArrayList();
            for (int i = 0; i < pngs.size(); ++i) {
                void var34_62;
                LinkedHashMap<String, Object> level = new LinkedHashMap<String, Object>();
                level.put("level", i);
                String templateName = LegacyJsonBuilder.sanitize(meta.baseName()) + "_" + variant + "_" + i;
                Path png = pngs.get(i);
                int nbFloors = LegacyJsonBuilder.estimateFloors(png, varMeta.width());
                LinkedHashMap<String, Integer> footprint = new LinkedHashMap<String, Integer>();
                footprint.put("width", varMeta.length());
                footprint.put("height", nbFloors);
                footprint.put("depth", varMeta.width());
                level.put("footprint", footprint);
                int groundLevel = levelGroundLevels[i];
                if (!groundLevelConstant) {
                    level.put("ground_level", groundLevel);
                }
                if (i < varMeta.levels().size()) {
                    List<String> levelSubs;
                    String levelName;
                    LegacyDataParser.LevelMeta levelMeta = varMeta.levels().get(i);
                    if (levelMeta.priority() != 100) {
                        level.put("priority", levelMeta.priority());
                    }
                    if (levelMeta.irrigation() > 0) {
                        level.put("irrigation", levelMeta.irrigation());
                    }
                    if ((levelName = levelMeta.nativeName()) != null && i > 0) {
                        level.put("native_name", levelName);
                    }
                    if (!(levelSubs = levelMeta.subBuildings()).isEmpty()) {
                        level.put("sub_buildings", levelSubs.stream().map(LegacyIdCanonicaliser::buildingPlanRefId).toList());
                    }
                    if (levelMeta.pathLevel() > 0) {
                        level.put("path_level", levelMeta.pathLevel());
                    }
                    if (levelMeta.rebuildPath()) {
                        level.put("rebuild_path", true);
                    }
                    if (levelMeta.pathWidth() != 2) {
                        level.put("path_width", levelMeta.pathWidth());
                    }
                    if (levelMeta.signs() != null) {
                        level.put("signs", levelMeta.signs());
                    }
                    if (levelMeta.priorityMoveIn() > 0) {
                        level.put("priority_move_in", levelMeta.priorityMoveIn());
                    }
                    if (levelMeta.extraSimultaneousWallConstructions() > 0) {
                        level.put("extra_simultaneous_wall_constructions", levelMeta.extraSimultaneousWallConstructions());
                    }
                    if (!levelMeta.requiredTags().isEmpty()) {
                        level.put("required_tags", levelMeta.requiredTags().stream().map(LegacyJsonBuilder::normalizeLegacyTag).toList());
                    }
                    if (!levelMeta.clearTags().isEmpty()) {
                        level.put("clear_tags", levelMeta.clearTags().stream().map(LegacyJsonBuilder::normalizeLegacyTag).toList());
                    }
                    if (!levelMeta.forbiddenTagsInVillage().isEmpty()) {
                        level.put("forbidden_tags_in_village", levelMeta.forbiddenTagsInVillage().stream().map(LegacyJsonBuilder::normalizeLegacyTag).toList());
                    }
                }
                List cumulativeTags = (List)levelCumulativeTags.get(i);
                int maxLevelForTags = Math.min(i, varMeta.levels().size() - 1);
                if (!tagsConstant && !cumulativeTags.isEmpty()) {
                    level.put("tags", cumulativeTags);
                }
                ArrayList<String> cumulativeParentTags = new ArrayList<String>();
                for (int j = 0; j <= maxLevelForTags; ++j) {
                    for (String string : varMeta.levels().get(j).parentTags()) {
                        String normalized = LegacyJsonBuilder.normalizeLegacyTag(string);
                        if (cumulativeParentTags.contains(normalized)) continue;
                        cumulativeParentTags.add(normalized);
                    }
                }
                if (!cumulativeParentTags.isEmpty()) {
                    level.put("parent_tags", cumulativeParentTags);
                }
                ArrayList<String> cumulativeVillageTags = new ArrayList<String>();
                for (int j = 0; j <= maxLevelForTags; ++j) {
                    for (Object t2 : varMeta.levels().get(j).villageTags()) {
                        String normalized = LegacyJsonBuilder.normalizeLegacyTag((String)t2);
                        if (cumulativeVillageTags.contains(normalized)) continue;
                        cumulativeVillageTags.add(normalized);
                    }
                }
                if (!cumulativeVillageTags.isEmpty()) {
                    level.put("village_tags", cumulativeVillageTags);
                }
                ArrayList<String> cumulativeRequiredParentTags = new ArrayList<String>();
                boolean bl = false;
                while (++var34_62 <= maxLevelForTags) {
                    Object t2;
                    t2 = varMeta.levels().get((int)var34_62).requiredParentTags().iterator();
                    while (t2.hasNext()) {
                        String t = (String)t2.next();
                        String normalized = LegacyJsonBuilder.normalizeLegacyTag(t);
                        if (cumulativeRequiredParentTags.contains(normalized)) continue;
                        cumulativeRequiredParentTags.add(normalized);
                    }
                }
                if (!cumulativeRequiredParentTags.isEmpty()) {
                    level.put("required_parent_tags", cumulativeRequiredParentTags);
                }
                LinkedHashMap<String, String> linkedHashMap = new LinkedHashMap<String, String>();
                int maxLevelForAbsProd = Math.min(i, varMeta.levels().size() - 1);
                for (int j = 0; j <= maxLevelForAbsProd; ++j) {
                    for (String entry2 : varMeta.levels().get(j).abstractedProduction()) {
                        String[] parts = entry2.split(",");
                        if (parts.length != 2) continue;
                        linkedHashMap.put(parts[0].trim(), entry2);
                    }
                }
                if (!linkedHashMap.isEmpty()) {
                    level.put("abstracted_production", new ArrayList(linkedHashMap.values()));
                }
                PngToNbtConverter.ConversionResult convResult = conversionResults == null ? null : conversionResults.get(templateName);
                Map<String, Object> infoFields = LegacyJsonBuilder.extractInfoFields(convResult);
                level.putAll(infoFields);
                levels.add(level);
            }
            variantJson.put("levels", levels);
            variants.add(variantJson);
        }
        json.put("variants", variants);
        return json;
    }

    static String remapLegacyColorName(String legacyName) {
        return switch (legacyName.toLowerCase()) {
            case "silver" -> "light_gray";
            default -> legacyName.toLowerCase();
        };
    }

    static int estimateFloors(Path pngPath, int buildingWidth) {
        try {
            BufferedImage img = ImageIO.read(pngPath.toFile());
            if (img == null) {
                return 1;
            }
            int pngWidth = img.getWidth();
            return (pngWidth + 1) / (buildingWidth + 1);
        }
        catch (IOException e) {
            return 1;
        }
    }

    static Map<String, Object> extractInfoFields(PngToNbtConverter.ConversionResult result) {
        LinkedHashMap<String, Object> info = new LinkedHashMap<String, Object>();
        if (result == null || result.templateNbt() == null) {
            return info;
        }
        List<SpecialPoint> points = MockBlockExtractor.extract(result.templateNbt());
        ArrayList soils = new ArrayList();
        ArrayList sources = new ArrayList();
        ArrayList treeSpawns = new ArrayList();
        ArrayList animalSpawns = new ArrayList();
        block12: for (SpecialPoint sp : points) {
            switch (sp.type()) {
                case "soil": {
                    if (sp.subtype() == null || sp.subtype().isEmpty()) break;
                    LinkedHashMap<String, String> entry = new LinkedHashMap<String, String>();
                    entry.put("crop", sp.subtype());
                    if (soils.contains(entry)) continue block12;
                    soils.add(entry);
                    break;
                }
                case "source": {
                    if (sp.subtype() == null || sp.subtype().isEmpty()) break;
                    LinkedHashMap<String, String> entry = new LinkedHashMap();
                    entry.put("type", sp.subtype());
                    if (sources.contains(entry)) continue block12;
                    sources.add(entry);
                    break;
                }
                case "treeSpawn": {
                    if (sp.subtype() == null || sp.subtype().isEmpty()) break;
                    LinkedHashMap<String, String> entry = new LinkedHashMap();
                    entry.put("sapling", sp.subtype());
                    if (treeSpawns.contains(entry)) continue block12;
                    treeSpawns.add(entry);
                    break;
                }
                case "animalSpawn": {
                    if (sp.subtype() == null || sp.subtype().isEmpty()) break;
                    LinkedHashMap<String, String> entry = new LinkedHashMap();
                    entry.put("entity", sp.subtype());
                    if (animalSpawns.contains(entry)) break;
                    animalSpawns.add(entry);
                }
            }
        }
        if (!soils.isEmpty()) {
            info.put("infoSoils", soils);
        }
        if (!sources.isEmpty()) {
            info.put("infoSources", sources);
        }
        if (!treeSpawns.isEmpty()) {
            info.put("infoTreeSpawns", treeSpawns);
        }
        if (!animalSpawns.isEmpty()) {
            info.put("infoAnimalSpawns", animalSpawns);
        }
        return info;
    }

    public static Map<String, Object> buildVillagerType(LegacyDataParser.VillagerMeta villager, String culture, ItemIdMapper items) {
        Optional<String> mcItem;
        LinkedHashMap<String, Object> json = new LinkedHashMap<String, Object>();
        json.put("culture", "millenaire:" + culture);
        json.put("native_name", villager.nativeName());
        LegacyJsonBuilder.putIfNot(json, "model", villager.model(), "male");
        ArrayList<CallSite> textures = new ArrayList<CallSite>();
        for (String string : villager.textures()) {
            Object path = string.endsWith(".png") ? string : string + ".png";
            textures.add((CallSite)((Object)("millenaire:" + ((String)path).toLowerCase(Locale.ROOT))));
        }
        json.put("textures", textures);
        if (!villager.clothesGroups().isEmpty()) {
            clothesMap = new LinkedHashMap<String, Object>();
            for (LegacyDataParser.ClothesGroup cg : villager.clothesGroups()) {
                Object path;
                Iterator<Object> groupClothes = new LinkedHashMap();
                if (!cg.layer0().isEmpty()) {
                    ArrayList<CallSite> arrayList = new ArrayList<CallSite>();
                    for (String cloth : cg.layer0()) {
                        path = cloth.endsWith(".png") ? cloth : cloth + ".png";
                        arrayList.add((CallSite)((Object)("millenaire:" + ((String)path).toLowerCase(Locale.ROOT))));
                    }
                    groupClothes.put("layer0", arrayList);
                }
                if (!cg.layer1().isEmpty()) {
                    ArrayList<CallSite> arrayList = new ArrayList<CallSite>();
                    for (String cloth : cg.layer1()) {
                        path = cloth.endsWith(".png") ? cloth : cloth + ".png";
                        arrayList.add((CallSite)((Object)("millenaire:" + ((String)path).toLowerCase(Locale.ROOT))));
                    }
                    groupClothes.put("layer1", arrayList);
                }
                clothesMap.put(cg.group(), groupClothes);
            }
            json.put("clothes", clothesMap);
        } else if (!villager.clothesLayer0().isEmpty() || !villager.clothesLayer1().isEmpty()) {
            Object path;
            clothesMap = new LinkedHashMap();
            LinkedHashMap linkedHashMap = new LinkedHashMap();
            if (!villager.clothesLayer0().isEmpty()) {
                ArrayList<CallSite> l0 = new ArrayList<CallSite>();
                for (String string : villager.clothesLayer0()) {
                    path = string.endsWith(".png") ? string : string + ".png";
                    l0.add((CallSite)((Object)("millenaire:" + ((String)path).toLowerCase(Locale.ROOT))));
                }
                linkedHashMap.put("layer0", l0);
            }
            if (!villager.clothesLayer1().isEmpty()) {
                ArrayList<CallSite> l1 = new ArrayList<CallSite>();
                for (String string : villager.clothesLayer1()) {
                    path = string.endsWith(".png") ? string : string + ".png";
                    l1.add((CallSite)((Object)("millenaire:" + ((String)path).toLowerCase(Locale.ROOT))));
                }
                linkedHashMap.put("layer1", l1);
            }
            clothesMap.put("free", linkedHashMap);
            json.put("clothes", clothesMap);
        }
        if (villager.baseHeight() != 1.0) {
            json.put("base_scale", villager.baseHeight());
        }
        if (villager.tags().contains("child")) {
            json.put("is_child", true);
        }
        List<String> goals = LegacyJsonBuilder.mapLegacyGoals(villager.goals());
        if (villager.tags().contains("child") && !goals.contains("millenaire:child_become_adult")) {
            goals.add(0, "millenaire:child_become_adult");
        }
        LegacyJsonBuilder.applyVillagerGoalOverrides(villager.id(), goals);
        json.put("goals", goals);
        List<String> list = villager.tags().stream().map(LegacyJsonBuilder::normalizeLegacyTag).distinct().toList();
        json.put("tags", list);
        json.put("spawn_weight", villager.chanceWeight());
        if (!villager.startingInv().isEmpty()) {
            LinkedHashMap<String, Integer> initialInv = new LinkedHashMap<String, Integer>();
            for (Map.Entry entry : villager.startingInv().entrySet()) {
                mcItem = items.resolve(culture, (String)entry.getKey());
                if (!mcItem.isPresent()) continue;
                initialInv.put(mcItem.get(), (Integer)entry.getValue());
            }
            if (!initialInv.isEmpty()) {
                json.put("initial_inventory", initialInv);
            }
        }
        if (!villager.bringBackHomeGoods().isEmpty()) {
            ArrayList bringBackGoods = new ArrayList();
            for (String string : villager.bringBackHomeGoods()) {
                items.resolve(culture, string).ifPresent(bringBackGoods::add);
            }
            if (!bringBackGoods.isEmpty()) {
                json.put("bring_back_home_goods", bringBackGoods);
            }
        }
        if (!villager.collectGoods().isEmpty()) {
            ArrayList collectGoods = new ArrayList();
            for (String string : villager.collectGoods()) {
                items.resolve(culture, string).ifPresent(collectGoods::add);
            }
            if (!collectGoods.isEmpty()) {
                json.put("collect_goods", collectGoods);
            }
        }
        if (!villager.requiredGoods().isEmpty()) {
            LinkedHashMap<String, Integer> requiredGoods = new LinkedHashMap<String, Integer>();
            for (Map.Entry entry : villager.requiredGoods().entrySet()) {
                mcItem = items.resolve(culture, (String)entry.getKey());
                if (!mcItem.isPresent()) continue;
                requiredGoods.put(mcItem.get(), (Integer)entry.getValue());
            }
            if (!requiredGoods.isEmpty()) {
                json.put("required_goods", requiredGoods);
            }
        }
        json.put("gender", villager.gender());
        if (villager.firstNameList() != null) {
            json.put("first_name_list", villager.firstNameList());
        }
        if (villager.familyNameList() != null) {
            json.put("family_name_list", villager.familyNameList());
        }
        if (villager.maleChild() != null) {
            json.put("male_child", LegacyIdCanonicaliser.villagerTypeRefId(LegacyJsonBuilder.stripCulturePrefix(culture, villager.maleChild())));
        }
        if (villager.femaleChild() != null) {
            json.put("female_child", LegacyIdCanonicaliser.villagerTypeRefId(LegacyJsonBuilder.stripCulturePrefix(culture, villager.femaleChild())));
        }
        if (villager.icon() != null) {
            json.put("icon", items.resolveExact(culture, villager.icon()).orElse(villager.icon()));
        }
        if (villager.health() != 20) {
            json.put("max_health", villager.health());
        }
        if (villager.villagerConfig() != null) {
            json.put("villager_config", villager.villagerConfig());
        }
        if (!villager.toolNeededClasses().isEmpty()) {
            json.put("tool_needed_classes", villager.toolNeededClasses());
        }
        if (!villager.itemsNeeded().isEmpty()) {
            ArrayList mapped = new ArrayList();
            for (String string : villager.itemsNeeded()) {
                items.resolveForVillagerEquipment(culture, string, villager.id()).ifPresent(mapped::add);
            }
            if (!mapped.isEmpty()) {
                json.put("items_needed", mapped);
            }
        }
        if (villager.baseAttackStrength() > 0) {
            json.put("base_attack_strength", villager.baseAttackStrength());
        }
        if (villager.defaultWeapon() != null) {
            String rawWeapon = villager.defaultWeapon();
            String mappedWeapon = LEGACY_ITEM_ID_MAP.get(rawWeapon);
            if (mappedWeapon == null) {
                mappedWeapon = LEGACY_ITEM_ID_MAP.get(rawWeapon.toLowerCase(Locale.ROOT));
            }
            if (mappedWeapon != null) {
                json.put("default_weapon", mappedWeapon);
            } else {
                System.err.println("[WARN] default_weapon not mapped: " + rawWeapon + " (villager " + villager.id() + ")");
            }
        }
        if (villager.experienceGiven() > 0) {
            json.put("experience_given", villager.experienceGiven());
        }
        if (villager.hiringCost() != null) {
            json.put("hiring_cost", villager.hiringCost());
        }
        if (villager.altNativeName() != null) {
            json.put("alt_native_name", villager.altNativeName());
        }
        if (villager.altKey() != null) {
            json.put("alt_key", villager.altKey());
        }
        if (villager.travelbookHeldItem() != null) {
            json.put("travelbook_held_item", items.resolveExact(culture, villager.travelbookHeldItem()).orElse(villager.travelbookHeldItem()));
        }
        if (villager.travelbookHeldItemOffHand() != null) {
            json.put("travelbook_held_item_off_hand", items.resolveExact(culture, villager.travelbookHeldItemOffHand()).orElse(villager.travelbookHeldItemOffHand()));
        }
        if (villager.travelbookMainCultureVillager()) {
            json.put("travelbook_main_culture_villager", true);
        }
        if (!villager.merchantStock().isEmpty()) {
            LinkedHashMap foreignMerchantStock = new LinkedHashMap();
            for (Map.Entry<String, Integer> entry : villager.merchantStock().entrySet()) {
                items.resolveForVillagerEquipment(culture, entry.getKey(), villager.id()).ifPresent(mc -> foreignMerchantStock.put(mc, (Integer)entry.getValue()));
            }
            if (!foreignMerchantStock.isEmpty()) {
                json.put("foreign_merchant_stock", foreignMerchantStock);
            }
        }
        return json;
    }

    static String normalizeLegacyTag(String tag) {
        return LEGACY_TAG_FIXES.getOrDefault(tag, tag);
    }

    private static String normalizeFarFromTag(String entry) {
        int comma = entry.indexOf(44);
        if (comma < 0) {
            return LegacyJsonBuilder.normalizeLegacyTag(entry);
        }
        return LegacyJsonBuilder.normalizeLegacyTag(entry.substring(0, comma)) + entry.substring(comma);
    }

    private static void applyVillagerGoalOverrides(String villagerId, List<String> goals) {
        List<String> extras = VILLAGER_EXTRA_GOALS.get(villagerId);
        if (extras == null) {
            return;
        }
        for (String goal : extras) {
            if (goals.contains(goal)) continue;
            goals.add(goal);
            System.out.println("    [OVERRIDE] +goal " + goal + " for " + villagerId + " (iso-legacy tendfurnace compensation)");
        }
    }

    static List<String> mapLegacyGoals(List<String> legacyGoals) {
        boolean hasChat;
        ArrayList<String> mapped = new ArrayList<String>();
        mapped.add("millenaire:idle");
        boolean hasSocialise = legacyGoals.stream().anyMatch(g -> g.equalsIgnoreCase("gosocialise"));
        if (hasSocialise) {
            mapped.add("millenaire:socialise");
            mapped.add("millenaire:chat");
        }
        if ((hasChat = legacyGoals.stream().anyMatch(g -> g.equalsIgnoreCase("chat"))) && !hasSocialise) {
            mapped.add("millenaire:chat");
        }
        block918: for (String goal : legacyGoals) {
            String lower;
            switch (lower = goal.toLowerCase()) {
                case "gorest": {
                    mapped.add("millenaire:rest");
                    continue block918;
                }
                case "gosocialise": 
                case "chat": {
                    continue block918;
                }
                case "getresourcesforbuild": 
                case "construction": {
                    mapped.add("millenaire:build");
                    continue block918;
                }
                case "chopwood": 
                case "choptrees": {
                    mapped.add("millenaire:chop_trees");
                    mapped.add("millenaire:plant_saplings");
                    continue block918;
                }
                case "gathergoods": {
                    mapped.add("millenaire:gather_goods");
                    continue block918;
                }
                case "plantsaplings": {
                    mapped.add("millenaire:plant_saplings");
                    continue block918;
                }
                case "plantsaplingappletreeorchard": {
                    mapped.add("millenaire:plant_sapling_appletree_orchard");
                    continue block918;
                }
                case "harvestwheathome": {
                    mapped.add("millenaire:harvest_wheat_home");
                    continue block918;
                }
                case "plantwheathome": {
                    mapped.add("millenaire:plant_wheat_home");
                    continue block918;
                }
                case "harvestcarrothome": {
                    mapped.add("millenaire:harvest_carrot_home");
                    continue block918;
                }
                case "plantcarrothome": {
                    mapped.add("millenaire:plant_carrot_home");
                    continue block918;
                }
                case "bringbackresourceshome": {
                    mapped.add("millenaire:bring_back_home");
                    continue block918;
                }
                case "deliverresourcesshop": {
                    mapped.add("millenaire:deliver_resources_shop");
                    continue block918;
                }
                case "gethousethresources": {
                    mapped.add("millenaire:get_resources_for_shops");
                    continue block918;
                }
                case "getgoodshousehold": 
                case "delivergoodshousehold": {
                    mapped.add("millenaire:get_goods_for_household");
                    continue block918;
                }
                case "makebread": {
                    mapped.add("millenaire:craft_bread");
                    continue block918;
                }
                case "quarrymining": 
                case "minestone": {
                    mapped.add("millenaire:mine_stone");
                    continue block918;
                }
                case "minesand": {
                    mapped.add("millenaire:mine_sand");
                    continue block918;
                }
                case "mineclay": {
                    mapped.add("millenaire:mine_clay");
                    continue block918;
                }
                case "minegravel": {
                    mapped.add("millenaire:mine_gravel");
                    continue block918;
                }
                case "minesnow": {
                    mapped.add("millenaire:mine_snow");
                    continue block918;
                }
                case "minesandstone": {
                    mapped.add("millenaire:mine_sandstone");
                    continue block918;
                }
                case "mineice": {
                    mapped.add("millenaire:mine_ice");
                    continue block918;
                }
                case "breedanimals": 
                case "breed": {
                    mapped.add("millenaire:breed_cattle");
                    mapped.add("millenaire:breed_sheep");
                    mapped.add("millenaire:breed_chicken");
                    mapped.add("millenaire:breed_pigs");
                    continue block918;
                }
                case "butcheranimals": {
                    mapped.add("millenaire:slaughter_cow");
                    mapped.add("millenaire:slaughter_sheep");
                    mapped.add("millenaire:slaughter_chicken");
                    mapped.add("millenaire:slaughter_pig");
                    continue block918;
                }
                case "slaughtercownorman": {
                    mapped.add("millenaire:slaughter_cow_norman");
                    continue block918;
                }
                case "slaughterpignorman": {
                    mapped.add("millenaire:slaughter_pig_norman");
                    continue block918;
                }
                case "slaughterchicken": {
                    mapped.add("millenaire:slaughter_chicken");
                    continue block918;
                }
                case "slaughtersheep": {
                    mapped.add("millenaire:slaughter_sheep");
                    continue block918;
                }
                case "shearanimals": 
                case "shearsheep": {
                    mapped.add("millenaire:shear_sheep");
                    continue block918;
                }
                case "fishinggoal": {
                    mapped.add("millenaire:fish");
                    continue block918;
                }
                case "fishinuit": {
                    mapped.add("millenaire:fishinuit");
                    continue block918;
                }
                case "buildpath": {
                    mapped.add("millenaire:build_path");
                    continue block918;
                }
                case "clearoldpath": {
                    mapped.add("millenaire:clear_old_path");
                    continue block918;
                }
                case "maketimberframeplainoak": {
                    mapped.add("millenaire:craft_timberframeplainoak");
                    continue block918;
                }
                case "maketimberframeplainbirch": {
                    mapped.add("millenaire:craft_timberframeplainbirch");
                    continue block918;
                }
                case "maketimberframeplainpine": {
                    mapped.add("millenaire:craft_timberframeplainpine");
                    continue block918;
                }
                case "maketimberframeplainjungle": {
                    mapped.add("millenaire:craft_timberframeplainjungle");
                    continue block918;
                }
                case "maketimberframeplainacacia": {
                    mapped.add("millenaire:craft_timberframeplainacacia");
                    continue block918;
                }
                case "maketimberframeplaindarkoak": {
                    mapped.add("millenaire:craft_timberframeplaindarkoak");
                    continue block918;
                }
                case "maketimberframecrossoak": {
                    mapped.add("millenaire:craft_timberframecrossoak");
                    continue block918;
                }
                case "maketimberframecrossbirch": {
                    mapped.add("millenaire:craft_timberframecrossbirch");
                    continue block918;
                }
                case "maketimberframecrosspine": {
                    mapped.add("millenaire:craft_timberframecrosspine");
                    continue block918;
                }
                case "maketimberframecrossjungle": {
                    mapped.add("millenaire:craft_timberframecrossjungle");
                    continue block918;
                }
                case "maketimberframecrossacacia": {
                    mapped.add("millenaire:craft_timberframecrossacacia");
                    continue block918;
                }
                case "maketimberframecrossdarkoak": {
                    mapped.add("millenaire:craft_timberframecrossdarkoak");
                    continue block918;
                }
                case "maketripes": {
                    mapped.add("millenaire:craft_tripes");
                    continue block918;
                }
                case "makeboudin": {
                    mapped.add("millenaire:craft_boudin");
                    continue block918;
                }
                case "makecider": {
                    mapped.add("millenaire:craft_cider");
                    continue block918;
                }
                case "makeciderhome": {
                    mapped.add("millenaire:craft_ciderhome");
                    continue block918;
                }
                case "makecalvahome": {
                    mapped.add("millenaire:craft_calvahome");
                    continue block918;
                }
                case "makecake": {
                    mapped.add("millenaire:craft_cake");
                    continue block918;
                }
                case "makeglassbottles": {
                    mapped.add("millenaire:craft_glassbottles");
                    continue block918;
                }
                case "makenormanaxe": {
                    mapped.add("millenaire:craft_normanaxe");
                    continue block918;
                }
                case "makenormanpickaxe": {
                    mapped.add("millenaire:craft_normanpickaxe");
                    continue block918;
                }
                case "makenormanshovel": {
                    mapped.add("millenaire:craft_normanshovel");
                    continue block918;
                }
                case "makenormanhoe": {
                    mapped.add("millenaire:craft_normanhoe");
                    continue block918;
                }
                case "makecauldron": {
                    mapped.add("millenaire:craft_cauldron");
                    continue block918;
                }
                case "makenormansword": {
                    mapped.add("millenaire:craft_normansword");
                    continue block918;
                }
                case "makenormanhelmet": {
                    mapped.add("millenaire:craft_normanhelmet");
                    continue block918;
                }
                case "makenormanplate": {
                    mapped.add("millenaire:craft_normanplate");
                    continue block918;
                }
                case "makenormanlegs": {
                    mapped.add("millenaire:craft_normanlegs");
                    continue block918;
                }
                case "makenormanboots": {
                    mapped.add("millenaire:craft_normanboots");
                    continue block918;
                }
                case "makebow": {
                    mapped.add("millenaire:craft_bow");
                    continue block918;
                }
                case "makenormanstainedglass_white": {
                    mapped.add("millenaire:craft_normanstainedglass_white");
                    continue block918;
                }
                case "makenormanstainedglass_yellow": {
                    mapped.add("millenaire:craft_normanstainedglass_yellow");
                    continue block918;
                }
                case "makenormanstainedglass_yellow_red": {
                    mapped.add("millenaire:craft_normanstainedglass_yellow_red");
                    continue block918;
                }
                case "makenormanstainedglass_red_blue": {
                    mapped.add("millenaire:craft_normanstainedglass_red_blue");
                    continue block918;
                }
                case "makenormanstainedglass_green_blue": {
                    mapped.add("millenaire:craft_normanstainedglass_green_blue");
                    continue block918;
                }
                case "makerosette": {
                    mapped.add("millenaire:craft_rosette");
                    continue block918;
                }
                case "makecarpet_white": {
                    mapped.add("millenaire:craft_carpet_white");
                    continue block918;
                }
                case "makecarpet_yellow": {
                    mapped.add("millenaire:craft_carpet_yellow");
                    continue block918;
                }
                case "makecarpet_red": {
                    mapped.add("millenaire:craft_carpet_red");
                    continue block918;
                }
                case "makecarpet_blue": {
                    mapped.add("millenaire:craft_carpet_blue");
                    continue block918;
                }
                case "dyewool_yellow": {
                    mapped.add("millenaire:craft_dyewool_yellow");
                    continue block918;
                }
                case "dyewool_red": {
                    mapped.add("millenaire:craft_dyewool_red");
                    continue block918;
                }
                case "dyewool_blue": {
                    mapped.add("millenaire:craft_dyewool_blue");
                    continue block918;
                }
                case "makebannerwool": {
                    mapped.add("millenaire:craft_bannerwool");
                    continue block918;
                }
                case "makebannerleather": {
                    mapped.add("millenaire:craft_bannerleather");
                    continue block918;
                }
                case "makebannerfeather": {
                    mapped.add("millenaire:craft_bannerfeather");
                    continue block918;
                }
                case "makebannercotton": {
                    mapped.add("millenaire:craft_bannercotton");
                    continue block918;
                }
                case "makebannerrice": {
                    mapped.add("millenaire:craft_bannerrice");
                    continue block918;
                }
                case "maketapestry": {
                    mapped.add("millenaire:craft_tapestry");
                    continue block918;
                }
                case "makebooks": {
                    mapped.add("millenaire:craft_books");
                    continue block918;
                }
                case "makestrawbed": {
                    mapped.add("millenaire:craft_strawbed");
                    continue block918;
                }
                case "makedirtwall": {
                    mapped.add("millenaire:craft_dirtwall");
                    continue block918;
                }
                case "makepathdirt": {
                    mapped.add("millenaire:craft_pathdirt");
                    continue block918;
                }
                case "makepathgravel": {
                    mapped.add("millenaire:craft_pathgravel");
                    continue block918;
                }
                case "makepathslabs": {
                    mapped.add("millenaire:craft_pathslabs");
                    continue block918;
                }
                case "makealchemy": {
                    mapped.add("millenaire:craft_glassbottles");
                    continue block918;
                }
                case "makebonemeal": {
                    mapped.add("millenaire:craft_bonemeal");
                    continue block918;
                }
                case "makenormantools": {
                    mapped.add("millenaire:craft_normanaxe");
                    mapped.add("millenaire:craft_normanpickaxe");
                    mapped.add("millenaire:craft_normanshovel");
                    mapped.add("millenaire:craft_normanhoe");
                    mapped.add("millenaire:craft_cauldron");
                    continue block918;
                }
                case "makenormanarmor": {
                    mapped.add("millenaire:craft_normansword");
                    mapped.add("millenaire:craft_normanhelmet");
                    mapped.add("millenaire:craft_normanplate");
                    mapped.add("millenaire:craft_normanlegs");
                    mapped.add("millenaire:craft_normanboots");
                    mapped.add("millenaire:craft_bow");
                    continue block918;
                }
                case "makeglassitems": {
                    mapped.add("millenaire:craft_normanstainedglass_white");
                    mapped.add("millenaire:craft_normanstainedglass_yellow");
                    mapped.add("millenaire:craft_normanstainedglass_yellow_red");
                    mapped.add("millenaire:craft_normanstainedglass_red_blue");
                    mapped.add("millenaire:craft_normanstainedglass_green_blue");
                    mapped.add("millenaire:craft_rosette");
                    continue block918;
                }
                case "makeweaveitems": {
                    mapped.add("millenaire:craft_carpet_white");
                    mapped.add("millenaire:craft_carpet_yellow");
                    mapped.add("millenaire:craft_carpet_red");
                    mapped.add("millenaire:craft_carpet_blue");
                    mapped.add("millenaire:craft_dyewool_yellow");
                    mapped.add("millenaire:craft_dyewool_red");
                    mapped.add("millenaire:craft_dyewool_blue");
                    mapped.add("millenaire:craft_bannerwool");
                    mapped.add("millenaire:craft_bannerleather");
                    mapped.add("millenaire:craft_bannerfeather");
                    mapped.add("millenaire:craft_bannercotton");
                    mapped.add("millenaire:craft_bannerrice");
                    mapped.add("millenaire:craft_tapestry");
                    continue block918;
                }
                case "huntmonster": {
                    continue block918;
                }
                case "cookstone": {
                    mapped.add("millenaire:cook_stone");
                    continue block918;
                }
                case "cooksand": {
                    mapped.add("millenaire:cook_sand");
                    continue block918;
                }
                case "cooksteak": {
                    mapped.add("millenaire:cook_steak");
                    continue block918;
                }
                case "cookpork": {
                    mapped.add("millenaire:cook_pork");
                    continue block918;
                }
                case "cookchicken": {
                    mapped.add("millenaire:cook_chicken");
                    continue block918;
                }
                case "tendfurnace": {
                    continue block918;
                }
                case "gopray": {
                    mapped.add("millenaire:pray");
                    continue block918;
                }
                case "godrink": {
                    mapped.add("millenaire:drink");
                    continue block918;
                }
                case "godrinkcider": {
                    mapped.add("millenaire:drink_cider");
                    continue block918;
                }
                case "goplay": {
                    mapped.add("millenaire:play");
                    continue block918;
                }
                case "goplaywithfriends": {
                    mapped.add("millenaire:play_with_friends");
                    continue block918;
                }
                case "childobserveagriculture": {
                    mapped.add("millenaire:observe_agriculture");
                    continue block918;
                }
                case "childobserveconstruction": {
                    mapped.add("millenaire:observe_construction");
                    continue block918;
                }
                case "childobservesmithing": {
                    mapped.add("millenaire:observe_smithing");
                    continue block918;
                }
                case "childobserveproducealcohol": {
                    mapped.add("millenaire:observe_produce_alcohol");
                    continue block918;
                }
                case "childobserveproducefood": {
                    mapped.add("millenaire:observe_produce_food");
                    continue block918;
                }
                case "childeatapple": {
                    mapped.add("millenaire:child_eat_apple");
                    continue block918;
                }
                case "childeatcacaobeans": {
                    mapped.add("millenaire:child_eat_cacao_beans");
                    continue block918;
                }
                case "inspectconstruction": {
                    mapped.add("millenaire:inspect_construction");
                    continue block918;
                }
                case "goholdaservice": {
                    mapped.add("millenaire:hold_service");
                    continue block918;
                }
                case "gogardening": {
                    mapped.add("millenaire:gardening_norman");
                    continue block918;
                }
                case "gopreachonpulpit": {
                    mapped.add("millenaire:preach_on_pulpit");
                    continue block918;
                }
                case "goplayorgan": {
                    mapped.add("millenaire:play_organ");
                    continue block918;
                }
                case "goonpilgrimage": {
                    mapped.add("millenaire:pilgrimage");
                    continue block918;
                }
                case "gotendsacrifices": {
                    mapped.add("millenaire:tend_sacrifices");
                    continue block918;
                }
                case "plantnormanflowers": {
                    mapped.add("millenaire:plant_norman_flowers");
                    continue block918;
                }
                case "harvestflowerhome_blue_orchid": {
                    mapped.add("millenaire:harvest_flower_home_blue_orchid");
                    continue block918;
                }
                case "harvestflowerhome_poppy": {
                    mapped.add("millenaire:harvest_flower_home_poppy");
                    continue block918;
                }
                case "harvestflowerhome_dandelion": {
                    mapped.add("millenaire:harvest_flower_home_dandelion");
                    continue block918;
                }
                case "removeflowerhome_blue_orchid": {
                    mapped.add("millenaire:remove_flower_home_blue_orchid");
                    continue block918;
                }
                case "removeflowerhome_poppy": {
                    mapped.add("millenaire:remove_flower_home_poppy");
                    continue block918;
                }
                case "removeflowerhome_dandelion": {
                    mapped.add("millenaire:remove_flower_home_dandelion");
                    continue block918;
                }
                case "plantrosebush": {
                    mapped.add("millenaire:plant_rosebush");
                    continue block918;
                }
                case "harvestflowerhome_rosebush": {
                    mapped.add("millenaire:harvest_flower_home_rosebush");
                    continue block918;
                }
                case "gatherciderappleshome": {
                    mapped.add("millenaire:gather_cider_apples_home");
                    continue block918;
                }
                case "gatherciderappleslumbermen": {
                    mapped.add("millenaire:gather_cider_apples_lumbermen");
                    continue block918;
                }
                case "plantsaplingappletreehome": {
                    mapped.add("millenaire:plant_sapling_appletree_home");
                    continue block918;
                }
                case "gatherciderappleschildren": {
                    mapped.add("millenaire:gather_cider_apples_children");
                    continue block918;
                }
                case "harvestmaize": {
                    mapped.add("millenaire:harvest_maize");
                    continue block918;
                }
                case "plantmaize": {
                    mapped.add("millenaire:plant_maize");
                    continue block918;
                }
                case "makeobsidianflake": {
                    mapped.add("millenaire:craft_obsidianflake");
                    continue block918;
                }
                case "makemayanmace": {
                    mapped.add("millenaire:craft_mayanmace");
                    continue block918;
                }
                case "makemayanaxe": {
                    mapped.add("millenaire:craft_mayanaxe");
                    continue block918;
                }
                case "makemayanpickaxe": {
                    mapped.add("millenaire:craft_mayanpickaxe");
                    continue block918;
                }
                case "makemayanshovel": {
                    mapped.add("millenaire:craft_mayanshovel");
                    continue block918;
                }
                case "makemayanhoe": {
                    mapped.add("millenaire:craft_mayanhoe");
                    continue block918;
                }
                case "makemayanstatue": {
                    mapped.add("millenaire:craft_mayanstatue");
                    continue block918;
                }
                case "makemayangoldblock": {
                    mapped.add("millenaire:craft_mayangoldblock");
                    continue block918;
                }
                case "makemasa": {
                    mapped.add("millenaire:craft_masa");
                    continue block918;
                }
                case "makewah": {
                    mapped.add("millenaire:craft_wah");
                    continue block918;
                }
                case "makecacauhaa": {
                    mapped.add("millenaire:craft_cacauhaa");
                    continue block918;
                }
                case "makestoneaxe": {
                    mapped.add("millenaire:craft_stoneaxe");
                    continue block918;
                }
                case "makestonepickaxe": {
                    mapped.add("millenaire:craft_stonepickaxe");
                    continue block918;
                }
                case "makestoneshovel": {
                    mapped.add("millenaire:craft_stoneshovel");
                    continue block918;
                }
                case "makestonehoe": {
                    mapped.add("millenaire:craft_stonehoe");
                    continue block918;
                }
                case "makestonesword": {
                    mapped.add("millenaire:craft_stonesword");
                    continue block918;
                }
                case "makewoodenpickaxe": {
                    mapped.add("millenaire:craft_woodenpickaxe");
                    continue block918;
                }
                case "harvestcocoa": {
                    mapped.add("millenaire:harvest_cocoa");
                    continue block918;
                }
                case "plantcocoa": {
                    mapped.add("millenaire:plant_cocoa");
                    continue block918;
                }
                case "godrinkcacauhaa": {
                    mapped.add("millenaire:drink_cacauhaa");
                    continue block918;
                }
                case "plantrice": {
                    mapped.add("millenaire:plant_rice");
                    continue block918;
                }
                case "harvestrice": {
                    mapped.add("millenaire:harvest_rice");
                    continue block918;
                }
                case "plantturmeric": {
                    mapped.add("millenaire:plant_turmeric");
                    continue block918;
                }
                case "harvestturmeric": {
                    mapped.add("millenaire:harvest_turmeric");
                    continue block918;
                }
                case "plantsugarcane": {
                    mapped.add("millenaire:plant_sugar_cane");
                    continue block918;
                }
                case "harvestsugarcane": {
                    mapped.add("millenaire:harvest_sugar_cane");
                    continue block918;
                }
                case "plantcotton": {
                    mapped.add("millenaire:plant_cotton");
                    continue block918;
                }
                case "harvestcotton": {
                    mapped.add("millenaire:harvest_cotton");
                    continue block918;
                }
                case "plantindianflowers": {
                    mapped.add("millenaire:plant_indian_flowers");
                    continue block918;
                }
                case "harvestflower_blue_orchid": {
                    mapped.add("millenaire:harvest_flower_blue_orchid");
                    continue block918;
                }
                case "harvestflower_poppy": {
                    mapped.add("millenaire:harvest_flower_poppy");
                    continue block918;
                }
                case "harvestflower_dandelion": {
                    mapped.add("millenaire:harvest_flower_dandelion");
                    continue block918;
                }
                case "drybrick": {
                    mapped.add("millenaire:dry_brick");
                    continue block918;
                }
                case "gatherbrick": {
                    mapped.add("millenaire:gather_brick");
                    continue block918;
                }
                case "cookindianbrick": {
                    mapped.add("millenaire:cook_indian_brick");
                    continue block918;
                }
                case "tendfurnacetownhall": {
                    continue block918;
                }
                case "tendfurnacebrickkiln": {
                    continue block918;
                }
                case "gatherfrombrickkiln": {
                    mapped.add("millenaire:gather_from_brick_kiln");
                    continue block918;
                }
                case "makerasgulla": {
                    mapped.add("millenaire:craft_rasgulla");
                    continue block918;
                }
                case "makecurry": {
                    mapped.add("millenaire:craft_curry");
                    continue block918;
                }
                case "makemurgh": {
                    mapped.add("millenaire:craft_murgh");
                    continue block918;
                }
                case "makecharpoybed": {
                    mapped.add("millenaire:craft_charpoybed");
                    continue block918;
                }
                case "makewoodenbarsindian": {
                    mapped.add("millenaire:craft_woodenbarsindian");
                    continue block918;
                }
                case "makewoodenbarsrosette": {
                    mapped.add("millenaire:craft_woodenbarsrosette");
                    continue block918;
                }
                case "makewoodenbars": {
                    mapped.add("millenaire:craft_woodenbars");
                    continue block918;
                }
                case "makeindianstatue": {
                    mapped.add("millenaire:craft_indianstatue");
                    continue block918;
                }
                case "makesandstonecarved": {
                    mapped.add("millenaire:craft_sandstonecarved");
                    continue block918;
                }
                case "makeredsandstonecarved": {
                    mapped.add("millenaire:craft_redsandstonecarved");
                    continue block918;
                }
                case "makeochresandstonecarved": {
                    mapped.add("millenaire:craft_ochresandstonecarved");
                    continue block918;
                }
                case "makedecoratedbricks": {
                    mapped.add("millenaire:craft_decoratedbricks");
                    continue block918;
                }
                case "makepaintedbucketwhite": {
                    mapped.add("millenaire:craft_paintedbucketwhite");
                    continue block918;
                }
                case "makewoolfromcotton": {
                    mapped.add("millenaire:craft_woolfromcotton");
                    continue block918;
                }
                case "makethatchfromrice": {
                    mapped.add("millenaire:craft_thatchfromrice");
                    continue block918;
                }
                case "makestrawbedindian": {
                    mapped.add("millenaire:craft_strawbedindian");
                    continue block918;
                }
                case "makesteelpickaxe": {
                    mapped.add("millenaire:craft_steelpickaxe");
                    continue block918;
                }
                case "makesteelaxe": {
                    mapped.add("millenaire:craft_steelaxe");
                    continue block918;
                }
                case "makesteelshovel": {
                    mapped.add("millenaire:craft_steelshovel");
                    continue block918;
                }
                case "makesteelhoe": {
                    mapped.add("millenaire:craft_steelhoe");
                    continue block918;
                }
                case "makesteelsword": {
                    mapped.add("millenaire:craft_steelsword");
                    continue block918;
                }
                case "makesteelchest": {
                    mapped.add("millenaire:craft_steelchest");
                    continue block918;
                }
                case "makesteellegs": {
                    mapped.add("millenaire:craft_steellegs");
                    continue block918;
                }
                case "makesteelhelmet": {
                    mapped.add("millenaire:craft_steelhelmet");
                    continue block918;
                }
                case "makesteelboots": {
                    mapped.add("millenaire:craft_steelboots");
                    continue block918;
                }
                case "mineredsandstone": {
                    mapped.add("millenaire:mine_red_sandstone");
                    continue block918;
                }
                case "gomeditate": {
                    mapped.add("millenaire:meditate");
                    continue block918;
                }
                case "performpujas": {
                    mapped.add("millenaire:perform_pujas");
                    continue block918;
                }
                case "bepujaperformer": {
                    mapped.add("millenaire:be_puja_performer");
                    continue block918;
                }
                case "childeatsugarcane": {
                    mapped.add("millenaire:child_eat_sugar_cane");
                    continue block918;
                }
                case "visitinn": {
                    mapped.add("millenaire:visit_inn");
                    continue block918;
                }
                case "visitbuilding": {
                    mapped.add("millenaire:visit_building");
                    continue block918;
                }
                case "keepstall": {
                    mapped.add("millenaire:keep_stall");
                    continue block918;
                }
                case "makepathsandstone": {
                    mapped.add("millenaire:craft_pathsandstone");
                    continue block918;
                }
                case "fish": {
                    mapped.add("millenaire:fish");
                    continue block918;
                }
                case "cookfish": {
                    mapped.add("millenaire:cook_fish");
                    continue block918;
                }
                case "cooklamb": {
                    mapped.add("millenaire:cook_lamb");
                    continue block918;
                }
                case "cookclay": {
                    mapped.add("millenaire:cook_clay");
                    continue block918;
                }
                case "cooksandbyz": {
                    mapped.add("millenaire:cook_sand_byz");
                    continue block918;
                }
                case "cookstonebyz": {
                    mapped.add("millenaire:cook_stone_byz");
                    continue block918;
                }
                case "godrinkwine": {
                    mapped.add("millenaire:drink_wine");
                    continue block918;
                }
                case "listentospeech1": {
                    mapped.add("millenaire:listen_to_speech");
                    continue block918;
                }
                case "listentospeech2": {
                    mapped.add("millenaire:listen_to_speech_2");
                    continue block918;
                }
                case "givespeech": {
                    mapped.add("millenaire:give_speech");
                    continue block918;
                }
                case "golookout": {
                    mapped.add("millenaire:lookout");
                    continue block918;
                }
                case "golookout2": {
                    mapped.add("millenaire:lookout_2");
                    continue block918;
                }
                case "gostudy": {
                    mapped.add("millenaire:study");
                    continue block918;
                }
                case "gotreerelax": {
                    mapped.add("millenaire:tree_relax");
                    continue block918;
                }
                case "childeatgrapes": {
                    mapped.add("millenaire:child_eat_grapes");
                    continue block918;
                }
                case "childobservevines": {
                    mapped.add("millenaire:observe_vines");
                    continue block918;
                }
                case "attendclass": {
                    mapped.add("millenaire:attend_class");
                    continue block918;
                }
                case "becomeadult": {
                    mapped.add("millenaire:child_become_adult");
                    continue block918;
                }
                case "teach": {
                    mapped.add("millenaire:teach");
                    continue block918;
                }
                case "training": {
                    mapped.add("millenaire:training");
                    continue block918;
                }
                case "patrol": {
                    mapped.add("millenaire:patrol");
                    continue block918;
                }
                case "gatherolives": {
                    mapped.add("millenaire:gather_olives");
                    continue block918;
                }
                case "gathersilk": {
                    mapped.add("millenaire:gather_silk");
                    continue block918;
                }
                case "gathersnails": {
                    mapped.add("millenaire:gather_snails");
                    continue block918;
                }
                case "gatherclay": {
                    mapped.add("millenaire:mine_clay_byz");
                    continue block918;
                }
                case "gathersand": {
                    mapped.add("millenaire:mine_sand_byz");
                    continue block918;
                }
                case "gathergravel": {
                    mapped.add("millenaire:mine_gravel_byz");
                    continue block918;
                }
                case "harvestwheat": {
                    mapped.add("millenaire:harvest_wheat");
                    continue block918;
                }
                case "plantwheat": {
                    mapped.add("millenaire:plant_wheat");
                    continue block918;
                }
                case "harvestcarrot": {
                    mapped.add("millenaire:harvest_carrot");
                    continue block918;
                }
                case "plantcarrot": {
                    mapped.add("millenaire:plant_carrot");
                    continue block918;
                }
                case "harvestvines": {
                    mapped.add("millenaire:harvest_vines");
                    continue block918;
                }
                case "plantvines": {
                    mapped.add("millenaire:plant_vines");
                    continue block918;
                }
                case "plantsaplingolivetree": {
                    mapped.add("millenaire:plant_olive_saplings");
                    continue block918;
                }
                case "plantfloweryellow": {
                    mapped.add("millenaire:plant_flowers_yellow");
                    continue block918;
                }
                case "makesouvlaki": {
                    mapped.add("millenaire:craft_souvlaki");
                    continue block918;
                }
                case "makewine": {
                    mapped.add("millenaire:craft_wine");
                    continue block918;
                }
                case "makeoliveoil": {
                    mapped.add("millenaire:craft_oliveoil");
                    continue block918;
                }
                case "makefeta": {
                    mapped.add("millenaire:craft_feta");
                    continue block918;
                }
                case "makefresco": {
                    mapped.add("millenaire:craft_fresco");
                    continue block918;
                }
                case "makeicon_small": {
                    mapped.add("millenaire:craft_icon_small");
                    continue block918;
                }
                case "makeicon_medium": {
                    mapped.add("millenaire:craft_icon_medium");
                    continue block918;
                }
                case "makeicon_large": {
                    mapped.add("millenaire:craft_icon_large");
                    continue block918;
                }
                case "makeiconfancy": {
                    mapped.add("millenaire:craft_icon_fancy");
                    continue block918;
                }
                case "makemosaicprelim": {
                    mapped.add("millenaire:craft_mosaic_prelim");
                    continue block918;
                }
                case "maketiles": {
                    mapped.add("millenaire:craft_tiles");
                    continue block918;
                }
                case "converttiles": {
                    mapped.add("millenaire:craft_convert_tiles");
                    continue block918;
                }
                case "makeclothessilk": {
                    mapped.add("millenaire:craft_clothes_silk");
                    continue block918;
                }
                case "makeclotheswool": {
                    mapped.add("millenaire:craft_clothes_wool");
                    continue block918;
                }
                case "makebyzantinemace": {
                    mapped.add("millenaire:craft_byzantinemace");
                    continue block918;
                }
                case "makebyzantineaxe": {
                    mapped.add("millenaire:craft_byzantineaxe");
                    continue block918;
                }
                case "makebyzantinepickaxe": {
                    mapped.add("millenaire:craft_byzantinepickaxe");
                    continue block918;
                }
                case "makebyzantineshovel": {
                    mapped.add("millenaire:craft_byzantineshovel");
                    continue block918;
                }
                case "makebyzantinehoe": {
                    mapped.add("millenaire:craft_byzantinehoe");
                    continue block918;
                }
                case "makebyzantinehelmet": {
                    mapped.add("millenaire:craft_byzantinehelmet");
                    continue block918;
                }
                case "makebyzantinechest": {
                    mapped.add("millenaire:craft_byzantinechest");
                    continue block918;
                }
                case "makebyzantinelegs": {
                    mapped.add("millenaire:craft_byzantinelegs");
                    continue block918;
                }
                case "makebyzantineboots": {
                    mapped.add("millenaire:craft_byzantineboots");
                    continue block918;
                }
                case "makebyzantinestainedglass_white": {
                    mapped.add("millenaire:craft_byzantinestainedglass_white");
                    continue block918;
                }
                case "makecarpetpurple": {
                    mapped.add("millenaire:craft_carpet_purple");
                    continue block918;
                }
                case "makecarpetyellow": {
                    mapped.add("millenaire:craft_carpet_yellow");
                    continue block918;
                }
                case "makedirtwallbyz": {
                    mapped.add("millenaire:craft_dirtwall");
                    continue block918;
                }
                case "makestrawbedbyz": {
                    mapped.add("millenaire:craft_strawbed");
                    continue block918;
                }
                case "makepathgravelbyz": {
                    mapped.add("millenaire:craft_pathgravel");
                    continue block918;
                }
                case "makepathslabsbyz": {
                    mapped.add("millenaire:craft_pathslabs");
                    continue block918;
                }
                case "makepathsandstonebyz": {
                    mapped.add("millenaire:craft_pathsandstone");
                    continue block918;
                }
                case "makebookshelvesbyz": {
                    mapped.add("millenaire:craft_bookshelves_byz");
                    continue block918;
                }
                case "makeironbyzworkshop": {
                    mapped.add("millenaire:craft_iron_workshop");
                    continue block918;
                }
                case "makesandstoneworkshop": {
                    mapped.add("millenaire:craft_sandstone_workshop");
                    continue block918;
                }
                case "makepaperwoodpulp": {
                    mapped.add("millenaire:craft_paper_woodpulp");
                    continue block918;
                }
                case "makequartz": {
                    mapped.add("millenaire:craft_quartz");
                    continue block918;
                }
                case "polishdiorite": {
                    mapped.add("millenaire:craft_polished_diorite");
                    continue block918;
                }
                case "makebookfancy": {
                    mapped.add("millenaire:craft_book_fancy");
                    continue block918;
                }
                case "makebook": {
                    mapped.add("millenaire:craft_books");
                    continue block918;
                }
                case "makearrow": {
                    mapped.add("millenaire:craft_arrow");
                    continue block918;
                }
                case "minediorite": {
                    mapped.add("millenaire:mine_diorite");
                    continue block918;
                }
                case "mineiron2": {
                    mapped.add("millenaire:mine_iron");
                    continue block918;
                }
                case "minestone2": {
                    mapped.add("millenaire:mine_stone_byz");
                    continue block918;
                }
                case "slaughtersheepbyz": {
                    mapped.add("millenaire:slaughter_sheep_byz");
                    continue block918;
                }
                case "slaughtersheepbyz2": {
                    mapped.add("millenaire:slaughter_sheep_byz2");
                    continue block918;
                }
                case "stealtulipred": {
                    mapped.add("millenaire:steal_tulip_red");
                    continue block918;
                }
                case "stealtulipwhite": {
                    mapped.add("millenaire:steal_tulip_white");
                    continue block918;
                }
                case "stealtulippink": {
                    mapped.add("millenaire:steal_tulip_pink");
                    continue block918;
                }
                case "stealtuliporange": {
                    mapped.add("millenaire:steal_tulip_orange");
                    continue block918;
                }
                case "relight": {
                    mapped.add("millenaire:relight");
                    continue block918;
                }
                case "fetchbreadbyz": {
                    mapped.add("millenaire:fetch_bread_byz");
                    continue block918;
                }
                case "fetchironbyz": {
                    mapped.add("millenaire:fetch_iron_byz");
                    continue block918;
                }
                case "fetchsandstonebyz": {
                    mapped.add("millenaire:fetch_sandstone_byz");
                    continue block918;
                }
                case "gardeningbyz": {
                    mapped.add("millenaire:gardening_byz");
                    continue block918;
                }
                case "makesake": {
                    mapped.add("millenaire:craft_sake");
                    continue block918;
                }
                case "makeudon": {
                    mapped.add("millenaire:craft_udon");
                    continue block918;
                }
                case "makeudonkitchen": {
                    mapped.add("millenaire:craft_udon_kitchen");
                    continue block918;
                }
                case "makepaperwall": {
                    mapped.add("millenaire:craft_paperwall");
                    continue block918;
                }
                case "makepaperrice": {
                    mapped.add("millenaire:craft_paperrice");
                    continue block918;
                }
                case "makethatch": {
                    mapped.add("millenaire:craft_thatchfromwheat");
                    continue block918;
                }
                case "maketachi": {
                    mapped.add("millenaire:craft_tachi");
                    continue block918;
                }
                case "makeyumi": {
                    mapped.add("millenaire:craft_yumi");
                    continue block918;
                }
                case "makejghelmet": {
                    mapped.add("millenaire:craft_jg_helmet");
                    continue block918;
                }
                case "makejgchest": {
                    mapped.add("millenaire:craft_jg_plate");
                    continue block918;
                }
                case "makejglegs": {
                    mapped.add("millenaire:craft_jg_legs");
                    continue block918;
                }
                case "makejgboots": {
                    mapped.add("millenaire:craft_jg_boots");
                    continue block918;
                }
                case "makejwhelmetb": {
                    mapped.add("millenaire:craft_jb_helmet");
                    continue block918;
                }
                case "makejwchestb": {
                    mapped.add("millenaire:craft_jb_plate");
                    continue block918;
                }
                case "makejwlegsb": {
                    mapped.add("millenaire:craft_jb_legs");
                    continue block918;
                }
                case "makejwbootsb": {
                    mapped.add("millenaire:craft_jb_boots");
                    continue block918;
                }
                case "makejwhelmetr": {
                    mapped.add("millenaire:craft_jr_helmet");
                    continue block918;
                }
                case "makejwchestr": {
                    mapped.add("millenaire:craft_jr_plate");
                    continue block918;
                }
                case "makejwlegsr": {
                    mapped.add("millenaire:craft_jr_legs");
                    continue block918;
                }
                case "makejwbootsr": {
                    mapped.add("millenaire:craft_jr_boots");
                    continue block918;
                }
                case "godrinksake": {
                    mapped.add("millenaire:drink_sake");
                    continue block918;
                }
                case "slaughtersquid": {
                    mapped.add("millenaire:slaughter_squid");
                    continue block918;
                }
                case "cookchickenkitchen": {
                    mapped.add("millenaire:cook_chicken_kitchen");
                    continue block918;
                }
                case "tendfurnacekitchen": {
                    continue block918;
                }
                case "makepathgravelslabs": {
                    mapped.add("millenaire:craft_pathgravelslabs");
                    continue block918;
                }
                case "gopraymalemuslim": {
                    mapped.add("millenaire:pray_male_muslim");
                    continue block918;
                }
                case "goprayfemalemuslim": {
                    mapped.add("millenaire:pray_female_muslim");
                    continue block918;
                }
                case "takebath": {
                    mapped.add("millenaire:take_bath");
                    continue block918;
                }
                case "takebathfemale": {
                    mapped.add("millenaire:take_bath_female");
                    continue block918;
                }
                case "godrinkayran": {
                    mapped.add("millenaire:drink_ayran");
                    continue block918;
                }
                case "gogardeningseljuk": {
                    mapped.add("millenaire:gardening_seljuk");
                    continue block918;
                }
                case "makeayranKitchen": 
                case "makeayrankitchen": {
                    mapped.add("millenaire:craft_ayran_kitchen");
                    continue block918;
                }
                case "makehelva": {
                    mapped.add("millenaire:craft_helva");
                    continue block918;
                }
                case "makelokum": {
                    mapped.add("millenaire:craft_lokum");
                    continue block918;
                }
                case "makepidebeef": {
                    mapped.add("millenaire:craft_pide_beef");
                    continue block918;
                }
                case "makepidemutton": {
                    mapped.add("millenaire:craft_pide_mutton");
                    continue block918;
                }
                case "makeyogurt": {
                    mapped.add("millenaire:craft_yogurt");
                    continue block918;
                }
                case "makebreadmill": {
                    mapped.add("millenaire:craft_bread_mill");
                    continue block918;
                }
                case "makecottoncloth": {
                    mapped.add("millenaire:craft_cotton_cloth");
                    continue block918;
                }
                case "makewoolcloth": {
                    mapped.add("millenaire:craft_wool_cloth");
                    continue block918;
                }
                case "makewoolstring": {
                    mapped.add("millenaire:craft_wool_string");
                    continue block918;
                }
                case "makewallcarpetlarge": {
                    mapped.add("millenaire:craft_wallcarpet_large");
                    continue block918;
                }
                case "makewallcarpetmedium": {
                    mapped.add("millenaire:craft_wallcarpet_medium");
                    continue block918;
                }
                case "makewallcarpetsmall": {
                    mapped.add("millenaire:craft_wallcarpet_small");
                    continue block918;
                }
                case "makemudbrickdecorated": {
                    mapped.add("millenaire:craft_mudbrick_decorated");
                    continue block918;
                }
                case "makemudbrickornamented": {
                    mapped.add("millenaire:craft_mudbrick_ornamented");
                    continue block918;
                }
                case "makemudbricksmooth": {
                    mapped.add("millenaire:craft_mudbrick_smooth");
                    continue block918;
                }
                case "makemudbricksandstone": {
                    mapped.add("millenaire:craft_mudbrick_sandstone");
                    continue block918;
                }
                case "makesandstone": {
                    mapped.add("millenaire:craft_sandstone_seljuk");
                    continue block918;
                }
                case "makescimitar": {
                    mapped.add("millenaire:craft_scimitar");
                    continue block918;
                }
                case "makeseljukbow": {
                    mapped.add("millenaire:craft_seljuk_bow");
                    continue block918;
                }
                case "makearrowseljuk": {
                    mapped.add("millenaire:craft_arrow_seljuk");
                    continue block918;
                }
                case "makeseljukhelmet": {
                    mapped.add("millenaire:craft_seljuk_helmet");
                    continue block918;
                }
                case "makeseljukplate": {
                    mapped.add("millenaire:craft_seljuk_plate");
                    continue block918;
                }
                case "makeseljuklegs": {
                    mapped.add("millenaire:craft_seljuk_legs");
                    continue block918;
                }
                case "makeseljukboots": {
                    mapped.add("millenaire:craft_seljuk_boots");
                    continue block918;
                }
                case "makeseljukturban": {
                    mapped.add("millenaire:craft_seljuk_turban");
                    continue block918;
                }
                case "makeleatherhelmet": {
                    mapped.add("millenaire:craft_leather_helmet");
                    continue block918;
                }
                case "makeleatherchest": {
                    mapped.add("millenaire:craft_leather_chest");
                    continue block918;
                }
                case "makeleatherlegs": {
                    mapped.add("millenaire:craft_leather_legs");
                    continue block918;
                }
                case "makeleatherboots": {
                    mapped.add("millenaire:craft_leather_boots");
                    continue block918;
                }
                case "bmakeleatherhelmet": {
                    mapped.add("millenaire:craft_bandit_leather_helmet");
                    continue block918;
                }
                case "bmakeleatherchest": {
                    mapped.add("millenaire:craft_bandit_leather_chest");
                    continue block918;
                }
                case "bmakeleatherlegs": {
                    mapped.add("millenaire:craft_bandit_leather_legs");
                    continue block918;
                }
                case "bmakeleatherboots": {
                    mapped.add("millenaire:craft_bandit_leather_boots");
                    continue block918;
                }
                case "bmakesteelhelmet": {
                    mapped.add("millenaire:craft_bandit_steel_helmet");
                    continue block918;
                }
                case "bmakesteelchest": {
                    mapped.add("millenaire:craft_bandit_steel_chest");
                    continue block918;
                }
                case "bmakesteellegs": {
                    mapped.add("millenaire:craft_bandit_steel_legs");
                    continue block918;
                }
                case "bmakesteelboots": {
                    mapped.add("millenaire:craft_bandit_steel_boots");
                    continue block918;
                }
                case "bmakesteelsword": {
                    mapped.add("millenaire:craft_bandit_steel_sword");
                    continue block918;
                }
                case "bmakestonesword": {
                    mapped.add("millenaire:craft_bandit_stone_sword");
                    continue block918;
                }
                case "bmakebow": {
                    mapped.add("millenaire:craft_bandit_bow");
                    continue block918;
                }
                case "cookbeefkitchen": {
                    mapped.add("millenaire:cook_beef_kitchen");
                    continue block918;
                }
                case "cookchickenkitchenseljuk": {
                    mapped.add("millenaire:cook_chicken_kitchen");
                    continue block918;
                }
                case "cookmuttonkitchen": {
                    mapped.add("millenaire:cook_mutton_kitchen");
                    continue block918;
                }
                case "cookfishkitchen": {
                    mapped.add("millenaire:cook_fish_kitchen");
                    continue block918;
                }
                case "slaughtercowseljuk": {
                    mapped.add("millenaire:slaughter_cow_seljuk");
                    continue block918;
                }
                case "slaughtersheepseljuk": {
                    mapped.add("millenaire:slaughter_sheep_seljuk");
                    continue block918;
                }
                case "gatherpistachioorchard": {
                    mapped.add("millenaire:gather_pistachio_orchard");
                    continue block918;
                }
                case "plantsaplingpistachioorchard": {
                    mapped.add("millenaire:plant_sapling_pistachio_orchard");
                    continue block918;
                }
                case "mineiron": {
                    mapped.add("millenaire:mine_iron");
                    continue block918;
                }
                case "mining": {
                    mapped.add("millenaire:mine_stone");
                    continue block918;
                }
                case "harvestwheatpaddy": {
                    mapped.add("millenaire:harvest_wheat_paddy");
                    continue block918;
                }
                case "plantwheatpaddy": {
                    mapped.add("millenaire:plant_wheat_paddy");
                    continue block918;
                }
                case "harvestcottonhome": {
                    mapped.add("millenaire:harvest_cotton_home");
                    continue block918;
                }
                case "plantcottonhome": {
                    mapped.add("millenaire:plant_cotton_home");
                    continue block918;
                }
                case "cookironore": {
                    mapped.add("millenaire:cook_iron_ore");
                    continue block918;
                }
                case "makeiron": {
                    mapped.add("millenaire:craft_iron");
                    continue block918;
                }
                case "makehay": {
                    mapped.add("millenaire:craft_hay");
                    continue block918;
                }
                case "dyewool_cyan": {
                    mapped.add("millenaire:craft_dyewool_cyan");
                    continue block918;
                }
                case "dyewool_green": {
                    mapped.add("millenaire:craft_dyewool_green");
                    continue block918;
                }
                case "dyewool_lightblue": {
                    mapped.add("millenaire:craft_dyewool_lightblue");
                    continue block918;
                }
                case "makecarpet_cyan": {
                    mapped.add("millenaire:craft_carpet_cyan");
                    continue block918;
                }
                case "makecarpet_green": {
                    mapped.add("millenaire:craft_carpet_green");
                    continue block918;
                }
                case "makecarpet_lightblue": {
                    mapped.add("millenaire:craft_carpet_lightblue");
                    continue block918;
                }
                case "makebearsteak": {
                    mapped.add("millenaire:craft_bearsteak");
                    continue block918;
                }
                case "makebearstew": {
                    mapped.add("millenaire:craft_bearstew");
                    continue block918;
                }
                case "makemeatstew1": {
                    mapped.add("millenaire:craft_meatstew1");
                    continue block918;
                }
                case "makemeatstew2": {
                    mapped.add("millenaire:craft_meatstew2");
                    continue block918;
                }
                case "makemeatstew3": {
                    mapped.add("millenaire:craft_meatstew3");
                    continue block918;
                }
                case "makemeatstew4": {
                    mapped.add("millenaire:craft_meatstew4");
                    continue block918;
                }
                case "makemeatstew5": {
                    mapped.add("millenaire:craft_meatstew5");
                    continue block918;
                }
                case "makemeatstew6": {
                    mapped.add("millenaire:craft_meatstew6");
                    continue block918;
                }
                case "makemeatstew7": {
                    mapped.add("millenaire:craft_meatstew7");
                    continue block918;
                }
                case "makemeatstew8": {
                    mapped.add("millenaire:craft_meatstew8");
                    continue block918;
                }
                case "makepotatostew": {
                    mapped.add("millenaire:craft_potatostew");
                    continue block918;
                }
                case "makewolfsteak": {
                    mapped.add("millenaire:craft_wolfsteak");
                    continue block918;
                }
                case "makedirtwallinuit": {
                    mapped.add("millenaire:craft_dirtwall_inuit");
                    continue block918;
                }
                case "makesnowbricksfromnothing": {
                    mapped.add("millenaire:craft_snow_bricks_from_nothing");
                    continue block918;
                }
                case "makesnowbricksfromsnow": {
                    mapped.add("millenaire:craft_snow_bricks_from_snow");
                    continue block918;
                }
                case "makesnowwall": {
                    mapped.add("millenaire:craft_snowwall");
                    continue block918;
                }
                case "makeicebrick": {
                    mapped.add("millenaire:craft_icebrick");
                    continue block918;
                }
                case "makefirepit": {
                    mapped.add("millenaire:craft_firepit");
                    continue block918;
                }
                case "shoveldirt": {
                    mapped.add("millenaire:craft_shoveldirt");
                    continue block918;
                }
                case "shoveldirt2": {
                    mapped.add("millenaire:craft_shoveldirt2");
                    continue block918;
                }
                case "carveice": {
                    mapped.add("millenaire:craft_carveice");
                    continue block918;
                }
                case "packsodspruce": {
                    mapped.add("millenaire:craft_packsodspruce");
                    continue block918;
                }
                case "packsodbirch": {
                    mapped.add("millenaire:craft_packsodbirch");
                    continue block918;
                }
                case "packsnowblock": {
                    mapped.add("millenaire:craft_packsnowblock");
                    continue block918;
                }
                case "makebone": {
                    mapped.add("millenaire:craft_bone");
                    continue block918;
                }
                case "makespade": {
                    mapped.add("millenaire:craft_spade");
                    continue block918;
                }
                case "makespear": {
                    mapped.add("millenaire:craft_spear");
                    continue block918;
                }
                case "makestonecauldron": {
                    mapped.add("millenaire:craft_stone_cauldron");
                    continue block918;
                }
                case "makearrowinuit": {
                    mapped.add("millenaire:craft_arrowinuit");
                    continue block918;
                }
                case "makewoodenaxeinuit": {
                    mapped.add("millenaire:craft_woodenaxeinuit");
                    continue block918;
                }
                case "makewoodenhoeinuit": {
                    mapped.add("millenaire:craft_woodenhoeinuit");
                    continue block918;
                }
                case "makewoodenpickaxeinuit": {
                    mapped.add("millenaire:craft_woodenpickaxeinuit");
                    continue block918;
                }
                case "makewoodenshovelinuit": {
                    mapped.add("millenaire:craft_woodenshovelinuit");
                    continue block918;
                }
                case "makeinuitbow": {
                    mapped.add("millenaire:craft_inuitbow");
                    continue block918;
                }
                case "makebowinuit": {
                    mapped.add("millenaire:craft_bowinuit");
                    continue block918;
                }
                case "mineicebricks": {
                    mapped.add("millenaire:mine_ice");
                    continue block918;
                }
                case "minesnowbricks": {
                    mapped.add("millenaire:mine_snow");
                    continue block918;
                }
                case "makeleather": {
                    mapped.add("millenaire:craft_leather");
                    continue block918;
                }
                case "maketannedhide": {
                    mapped.add("millenaire:craft_tanned_hide");
                    continue block918;
                }
                case "maketannedhide2": {
                    mapped.add("millenaire:craft_tanned_hide2");
                    continue block918;
                }
                case "makehidehanging": {
                    mapped.add("millenaire:craft_hide_hanging");
                    continue block918;
                }
                case "makeinuitbed": {
                    mapped.add("millenaire:craft_inuit_bed");
                    continue block918;
                }
                case "makeinuitcarving": {
                    mapped.add("millenaire:craft_inuit_carving");
                    continue block918;
                }
                case "makefurboots": {
                    mapped.add("millenaire:craft_furboots");
                    continue block918;
                }
                case "makefurhelmet": {
                    mapped.add("millenaire:craft_furhelmet");
                    continue block918;
                }
                case "makefurlegs": {
                    mapped.add("millenaire:craft_furlegs");
                    continue block918;
                }
                case "makefurplate": {
                    mapped.add("millenaire:craft_furplate");
                    continue block918;
                }
                case "makeleatherboots_inuit": {
                    mapped.add("millenaire:craft_leather_boots_inuit");
                    continue block918;
                }
                case "makeleatherchest_inuit": {
                    mapped.add("millenaire:craft_leather_chest_inuit");
                    continue block918;
                }
                case "makeleatherhelmet_inuit": {
                    mapped.add("millenaire:craft_leather_helmet_inuit");
                    continue block918;
                }
                case "makeleatherlegs_inuit": {
                    mapped.add("millenaire:craft_leather_legs_inuit");
                    continue block918;
                }
                case "plantpotato": {
                    mapped.add("millenaire:plant_potato");
                    continue block918;
                }
                case "harvestpotato": {
                    mapped.add("millenaire:harvest_potato");
                    continue block918;
                }
                case "plantpotatohome": {
                    mapped.add("millenaire:plant_potato_home");
                    continue block918;
                }
                case "harvestpotatohome": {
                    mapped.add("millenaire:harvest_potato_home");
                    continue block918;
                }
                case "minesnowpaths": {
                    mapped.add("millenaire:mine_snow_paths");
                    continue block918;
                }
                case "cookpotato": {
                    mapped.add("millenaire:cook_potato");
                    continue block918;
                }
                case "cookfishslab": {
                    mapped.add("millenaire:cook_seafood");
                    continue block918;
                }
                case "slaughtercowinuits": {
                    mapped.add("millenaire:slaughter_cow_inuit");
                    continue block918;
                }
                case "gatherchicken": {
                    continue block918;
                }
                case "makesugar": {
                    mapped.add("millenaire:craft_sugar");
                    continue block918;
                }
            }
            System.out.println("  [WARN] Goal legacy non mapp\u00e9 : " + goal);
        }
        return new ArrayList<String>(new LinkedHashSet(mapped));
    }

    public static Map<String, Object> buildVillageType(LegacyDataParser.VillageTypeMeta vt, String culture, ItemIdMapper items, List<LegacyDataParser.BuildingWithVariants> allBuildings) {
        return LegacyJsonBuilder.buildVillageType(vt, culture, items, allBuildings, null);
    }

    public static Map<String, Object> buildVillageType(LegacyDataParser.VillageTypeMeta vt, String culture, ItemIdMapper items, List<LegacyDataParser.BuildingWithVariants> allBuildings, BiomeMapper biomeMapper) {
        LinkedHashMap<String, Object> slot;
        LinkedHashMap<String, Object> json = new LinkedHashMap<String, Object>();
        json.put("culture", "millenaire:" + culture);
        json.put("name", vt.name());
        json.put("weight", vt.playerControlled() ? 0 : vt.weight());
        if (vt.playerControlled()) {
            json.put("player_controlled", true);
        }
        if (vt.loneBuilding()) {
            json.put("lone_building", true);
            if (vt.namelist() != null) {
                json.put("namelist", vt.namelist());
            }
            if (vt.radius() != 80) {
                json.put("radius", vt.radius());
            }
            if (vt.max() >= 0) {
                json.put("max", vt.max());
            }
            if (vt.minDistanceFromSpawn() > 0) {
                json.put("min_distance_from_spawn", vt.minDistanceFromSpawn());
            }
            if (vt.minimumBiomeValidity() > 0.0) {
                json.put("minimum_biome_validity", vt.minimumBiomeValidity());
            }
            if (vt.centre() != null) {
                allBuildings.stream().filter(b -> b.meta().baseName().equals(vt.centre())).findFirst().ifPresent(b -> {
                    if (b.meta().showTownHallSigns()) {
                        json.put("show_town_hall_signs", true);
                    }
                });
            }
        }
        if (!vt.loneBuilding() && vt.namelist() != null) {
            json.put("namelist", vt.namelist());
        }
        if (biomeMapper != null && !vt.playerControlled() && vt.weight() > 0) {
            List<Object> tags;
            List<Object> list = tags = vt.biomes().isEmpty() ? List.of() : biomeMapper.mapAll(culture, vt.biomes());
            if (tags.isEmpty()) {
                tags = List.of("#minecraft:is_overworld");
            }
            json.put("biome_tags", tags);
        }
        ArrayList<Object> layout = new ArrayList<Object>();
        if (vt.centre() != null) {
            Iterator<String> slot2 = new LinkedHashMap();
            slot2.put("plan", "millenaire:" + culture + "/" + LegacyIdCanonicaliser.buildingPlanRefId(vt.centre()));
            slot2.put("role", "centre");
            layout.add(slot2);
        }
        for (String plan : vt.start()) {
            slot = new LinkedHashMap<String, Object>();
            slot.put("plan", "millenaire:" + culture + "/" + LegacyIdCanonicaliser.buildingPlanRefId(plan));
            slot.put("role", "start");
            layout.add(slot);
        }
        for (String plan : vt.core()) {
            if (SKIPPED_GIFT_PLANS.contains(LegacyJsonBuilder.sanitize(plan))) continue;
            slot = new LinkedHashMap();
            slot.put("plan", "millenaire:" + culture + "/" + LegacyIdCanonicaliser.buildingPlanRefId(plan));
            slot.put("role", "core");
            layout.add(slot);
        }
        for (String plan : vt.secondary()) {
            slot = new LinkedHashMap();
            slot.put("plan", "millenaire:" + culture + "/" + LegacyIdCanonicaliser.buildingPlanRefId(plan));
            slot.put("role", "secondary");
            layout.add(slot);
        }
        json.put("layout", layout);
        Map<String, Integer> sellingOverrides = LegacyJsonBuilder.buildPriceOverrides(vt.sellingPriceOverrides());
        Map<String, Integer> buyingOverrides = LegacyJsonBuilder.buildPriceOverrides(vt.buyingPriceOverrides());
        if (!sellingOverrides.isEmpty()) {
            json.put("selling_price_overrides", sellingOverrides);
        }
        if (!buyingOverrides.isEmpty()) {
            json.put("buying_price_overrides", buyingOverrides);
        }
        if (vt.icon() != null) {
            json.put("icon", items.resolve(culture, vt.icon()).orElse(vt.icon()));
        }
        if (vt.carriesRaid()) {
            json.put("carries_raid", true);
        }
        if (!vt.qualifiers().isEmpty()) {
            json.put("qualifiers", vt.qualifiers());
        }
        if (vt.hillQualifier() != null) {
            json.put("hill_qualifier", vt.hillQualifier());
        }
        if (vt.mountainQualifier() != null) {
            json.put("mountain_qualifier", vt.mountainQualifier());
        }
        if (vt.desertQualifier() != null) {
            json.put("desert_qualifier", vt.desertQualifier());
        }
        if (vt.forestQualifier() != null) {
            json.put("forest_qualifier", vt.forestQualifier());
        }
        if (vt.lavaQualifier() != null) {
            json.put("lava_qualifier", vt.lavaQualifier());
        }
        if (vt.lakeQualifier() != null) {
            json.put("lake_qualifier", vt.lakeQualifier());
        }
        if (vt.oceanQualifier() != null) {
            json.put("ocean_qualifier", vt.oceanQualifier());
        }
        if (!vt.pathMaterials().isEmpty()) {
            json.put("path_materials", vt.pathMaterials());
        }
        if (!vt.playerBuildings().isEmpty()) {
            json.put("player_buildings", vt.playerBuildings().stream().map(p -> "millenaire:" + culture + "/" + LegacyIdCanonicaliser.buildingPlanRefId(p)).toList());
        }
        if (vt.innerWallType() != null) {
            json.put("inner_wall_type", LegacyIdCanonicaliser.buildingPlanRefId(vt.innerWallType()));
        }
        if (vt.innerWallRadius() > 0) {
            json.put("inner_wall_radius", vt.innerWallRadius());
        }
        if (vt.outerWallType() != null) {
            json.put("outer_wall_type", LegacyIdCanonicaliser.buildingPlanRefId(vt.outerWallType()));
        }
        if (vt.outerWallRadius() > 0) {
            json.put("outer_wall_radius", vt.outerWallRadius());
        }
        if (!vt.bannerJsons().isEmpty()) {
            json.put("banner_json", vt.bannerJsons());
        }
        if (vt.maxSimultaneousWallConstructions() > 0) {
            json.put("max_simultaneous_wall_constructions", vt.maxSimultaneousWallConstructions());
        }
        if (vt.maxSimultaneousConstructions() > 1) {
            json.put("max_simultaneous_constructions", vt.maxSimultaneousConstructions());
        }
        if (!vt.loneBuilding() && vt.radius() != 80) {
            json.put("radius", vt.radius());
        }
        if (!vt.never().isEmpty()) {
            json.put("never", vt.never().stream().map(LegacyIdCanonicaliser::buildingPlanRefId).toList());
        }
        if (!vt.brickColourThemes().isEmpty()) {
            json.put("brick_colour_themes", vt.brickColourThemes().stream().map(LegacyJsonBuilder::buildBrickColourThemeJson).toList());
        }
        if (!vt.hamlets().isEmpty()) {
            json.put("hamlets", vt.hamlets().stream().map(h -> "millenaire:" + culture + "/" + LegacyIdCanonicaliser.villageTypeRefId(LegacyJsonBuilder.stripCulturePrefix(culture, h))).toList());
        }
        if (vt.specialType() != null) {
            json.put("special_type", vt.specialType());
        }
        if (!vt.loneBuilding() && !vt.spawnable()) {
            json.put("spawnable", false);
        }
        if (!vt.allowExtraBuildings()) {
            json.put("allow_extra_buildings", false);
        }
        return json;
    }

    private static Map<String, Object> buildBrickColourThemeJson(LegacyDataParser.BrickColourThemeLegacy theme) {
        LinkedHashMap<String, Object> json = new LinkedHashMap<String, Object>();
        json.put("name", theme.name());
        json.put("weight", theme.weight());
        LinkedHashMap colours = new LinkedHashMap();
        for (Map.Entry<String, List<LegacyDataParser.WeightedColorLegacy>> entry : theme.colourGroups().entrySet()) {
            ArrayList colorList = new ArrayList();
            for (LegacyDataParser.WeightedColorLegacy wc : entry.getValue()) {
                LinkedHashMap<String, Object> colorObj = new LinkedHashMap<String, Object>();
                String colorName = "silver".equals(wc.color()) ? "light_gray" : wc.color();
                colorObj.put("color", colorName);
                colorObj.put("weight", wc.weight());
                colorList.add(colorObj);
            }
            colours.put(entry.getKey(), colorList);
        }
        json.put("colours", colours);
        return json;
    }

    private static Map<String, Integer> buildPriceOverrides(Map<String, String> legacyPrices) {
        LinkedHashMap<String, Integer> result = new LinkedHashMap<String, Integer>();
        for (Map.Entry<String, String> entry : legacyPrices.entrySet()) {
            String goodId = LEGACY_TO_GOOD_ID.get(entry.getKey());
            if (goodId == null) {
                goodId = LEGACY_TO_GOOD_ID.get(entry.getKey().toLowerCase());
            }
            if (goodId == null) {
                System.out.println("  [INFO] Price override ignored (legacy item not mapped): " + entry.getKey());
                continue;
            }
            try {
                int price = LegacyJsonBuilder.parseLegacyPrice(entry.getValue());
                result.put(goodId, price);
            }
            catch (NumberFormatException e) {
                System.err.println("  [WARN] Invalid price override for " + entry.getKey() + ": " + entry.getValue());
            }
        }
        return result;
    }

    static int parseLegacyPrice(String raw) {
        String[] parts = raw.split("/");
        if (parts.length == 1) {
            return Integer.parseInt(parts[0].trim());
        }
        if (parts.length == 2) {
            return Integer.parseInt(parts[0].trim()) * 64 + Integer.parseInt(parts[1].trim());
        }
        return Integer.parseInt(parts[0].trim()) * 64 * 64 + Integer.parseInt(parts[1].trim()) * 64 + Integer.parseInt(parts[2].trim());
    }

    public static Map<String, Object> buildShop(LegacyDataParser.ShopMeta shop, String culture, ItemIdMapper items) {
        LinkedHashMap<String, Object> json = new LinkedHashMap<String, Object>();
        String shopId = shop.id();
        json.put("sells", LegacyJsonBuilder.mapShopItems(shop.sells(), culture, shopId, "sells", items));
        json.put("buys", LegacyJsonBuilder.mapShopItems(shop.buys(), culture, shopId, "buys", items));
        json.put("buys_optional", LegacyJsonBuilder.mapShopItems(shop.buysOptional(), culture, shopId, "buys_optional", items));
        ArrayList<String> deliverTo = new ArrayList<String>();
        for (String legacyItem : shop.deliverTo()) {
            Optional<String> resolved = items.resolveExact(culture, legacyItem);
            if (resolved.isPresent()) {
                deliverTo.add(resolved.get());
                continue;
            }
            System.out.println("    [WARN] deliver_to item not mapped: " + legacyItem);
        }
        json.put("deliver_to", deliverTo);
        return json;
    }

    private static List<String> mapShopItems(List<String> legacyItems, String culture, String shopId, String slot, ItemIdMapper items) {
        ArrayList<String> mapped = new ArrayList<String>();
        for (String legacyItem : legacyItems) {
            String goodId = LEGACY_TO_GOOD_ID.get(legacyItem);
            if (goodId == null) {
                goodId = LEGACY_TO_GOOD_ID.get(legacyItem.toLowerCase());
            }
            if (goodId != null) {
                mapped.add(goodId);
                continue;
            }
            Optional<String> resolved = items.resolveForShopEntry(culture, legacyItem, shopId, slot);
            if (!resolved.isPresent()) continue;
            mapped.add(LegacyJsonBuilder.legacyItemToGoodId(legacyItem));
        }
        return mapped;
    }

    public static Map<String, Object> buildGatheringType(String category, Map<String, List<String>> txt, String culture, ItemIdMapper items) {
        if (category != null && category.startsWith("genericcrafting")) {
            return LegacyJsonBuilder.buildGatheringTypeJson(txt, culture, items);
        }
        return LegacyJsonBuilder.buildNonCraftingGatheringTypeJson(category, txt, culture, items);
    }

    public static Map<String, Object> buildTradedGoods(List<LegacyDataParser.TradedGoodMeta> goods, String culture, ItemIdMapper items) {
        ArrayList goodsJson = new ArrayList();
        for (LegacyDataParser.TradedGoodMeta tg : goods) {
            int foreignMerchantPrice;
            Optional<String> resolved = items.resolveForTradedGood(culture, tg.name());
            if (resolved.isEmpty()) continue;
            String mcItem = resolved.get();
            String goodId = LegacyJsonBuilder.legacyItemToGoodId(tg.name());
            LinkedHashMap<String, Object> good = new LinkedHashMap<String, Object>();
            good.put("id", goodId);
            good.put("item", mcItem);
            int sellingPrice = LegacyJsonBuilder.evalLegacyPrice(tg.sellingPrice());
            int buyingPrice = LegacyJsonBuilder.evalLegacyPrice(tg.buyingPrice());
            if (sellingPrice > 0) {
                good.put("selling_price", sellingPrice);
            }
            if (buyingPrice > 0) {
                good.put("buying_price", buyingPrice);
            }
            if (tg.reservedQuantity() > 0) {
                good.put("reserved_quantity", tg.reservedQuantity());
            }
            if (tg.targetQuantity() > 0) {
                good.put("target_quantity", tg.targetQuantity());
            }
            if (tg.autoGenerated()) {
                good.put("auto_generate", true);
            }
            if (tg.minReputation() > 0) {
                good.put("min_reputation", tg.minReputation());
            }
            if ((foreignMerchantPrice = LegacyJsonBuilder.evalLegacyPrice(tg.foreignMerchantPrice())) > 0) {
                good.put("foreign_merchant_price", foreignMerchantPrice);
            }
            good.put("category", tg.category());
            goodsJson.add(good);
        }
        LinkedHashMap<String, Object> root = new LinkedHashMap<String, Object>();
        root.put("goods", goodsJson);
        return root;
    }

    static int evalLegacyPrice(String raw) {
        if (raw == null || raw.isBlank()) {
            return 0;
        }
        if ((raw = raw.trim()).equals("0")) {
            return 0;
        }
        try {
            String[] parts = raw.split("\\*");
            int result = 1;
            for (String part : parts) {
                result *= Integer.parseInt(part.trim());
            }
            return result;
        }
        catch (NumberFormatException e) {
            System.out.println("  [WARN] Legacy price not parseable: " + raw);
            return 0;
        }
    }

    public static Map<String, Object> buildCultureStub(String cultureId, Map<String, List<String>> cultureTxt) {
        String cultureBanner;
        String defaultVillageType;
        LinkedHashMap<String, Object> out = new LinkedHashMap<String, Object>();
        out.put("culture_id", cultureId);
        String displayName = LegacyJsonBuilder.firstOrNull(cultureTxt, "name");
        if (displayName != null) {
            out.put("display_name", displayName);
        }
        if ((defaultVillageType = LegacyJsonBuilder.firstOrNull(cultureTxt, "default_village")) != null) {
            out.put("default_village_type", defaultVillageType);
        }
        if ((cultureBanner = LegacyJsonBuilder.firstOrNull(cultureTxt, "cultureBanner")) != null && !cultureBanner.isBlank()) {
            out.put("culture_banner_nbt", cultureBanner);
        }
        for (Map.Entry<String, List<String>> e : cultureTxt.entrySet()) {
            List<String> values;
            String key = e.getKey();
            if (out.containsKey(key) || "name".equals(key) || "default_village".equals(key) || "cultureBanner".equals(key) || (values = e.getValue()).isEmpty()) continue;
            if (values.size() == 1) {
                out.put(key, values.get(0));
                continue;
            }
            out.put(key, List.copyOf(values));
        }
        return out;
    }

    public static Map<String, Object> buildWallType(String culture, String key, Map<String, List<String>> kv) {
        String gate = LegacyJsonBuilder.firstOrNull(kv, "village_wall_gate");
        if (gate == null || gate.isBlank()) {
            return null;
        }
        String ns = "millenaire:" + culture;
        LinkedHashMap<String, Object> json = new LinkedHashMap<String, Object>();
        json.put("culture", ns);
        json.put("key", key);
        LegacyJsonBuilder.putWallPlanSet(json, "wall_plan_set", kv, "village_wall", culture);
        LegacyJsonBuilder.putWallPlanSet(json, "tower_plan_set", kv, "village_wall_tower", culture);
        json.put("gateway_plan_set", ns + "/" + LegacyJsonBuilder.sanitize(gate.trim()));
        LegacyJsonBuilder.putWallPlanSet(json, "corner_plan_set", kv, "village_wall_corner", culture);
        LegacyJsonBuilder.putWallPlanSet(json, "cap_right_plan_set", kv, "village_wall_cap_right", culture);
        LegacyJsonBuilder.putWallPlanSet(json, "cap_left_plan_set", kv, "village_wall_cap_left", culture);
        LegacyJsonBuilder.putWallPlanSet(json, "cap_both_plan_set", kv, "village_wall_cap_both", culture);
        LegacyJsonBuilder.putWallPlanSet(json, "slope1_left_plan_set", kv, "village_wall_slope1_left", culture);
        LegacyJsonBuilder.putWallPlanSet(json, "slope1_right_plan_set", kv, "village_wall_slope1_right", culture);
        LegacyJsonBuilder.putWallPlanSet(json, "slope2_left_plan_set", kv, "village_wall_slope2_left", culture);
        LegacyJsonBuilder.putWallPlanSet(json, "slope2_right_plan_set", kv, "village_wall_slope2_right", culture);
        LegacyJsonBuilder.putWallPlanSet(json, "slope3_left_plan_set", kv, "village_wall_slope3_left", culture);
        LegacyJsonBuilder.putWallPlanSet(json, "slope3_right_plan_set", kv, "village_wall_slope3_right", culture);
        LegacyJsonBuilder.putWallBool(json, "wall_spawn", kv, "village_wall_spawn");
        LegacyJsonBuilder.putWallBool(json, "tower_spawn", kv, "village_wall_tower_spawn");
        LegacyJsonBuilder.putWallBool(json, "gateway_spawn", kv, "village_wall_gate_spawn");
        LegacyJsonBuilder.putWallBool(json, "corner_spawn", kv, "village_wall_corner_spawn");
        LegacyJsonBuilder.putWallBool(json, "cap_spawn", kv, "village_wall_cap_spawn");
        LegacyJsonBuilder.putWallInt(json, "walls_between_towers", kv, "village_wall_nb_between_towers");
        LegacyJsonBuilder.putWallInt(json, "nb_smooth_runs", kv, "nb_smooth_runs");
        LegacyJsonBuilder.putWallInt(json, "max_y_delta", kv, "max_y_delta");
        return json;
    }

    private static void putWallPlanSet(Map<String, Object> json, String field, Map<String, List<String>> kv, String legacyKey, String culture) {
        String v = LegacyJsonBuilder.firstOrNull(kv, legacyKey);
        if (v != null && !v.isBlank()) {
            json.put(field, "millenaire:" + culture + "/" + LegacyJsonBuilder.sanitize(v.trim()));
        }
    }

    private static void putWallBool(Map<String, Object> json, String field, Map<String, List<String>> kv, String legacyKey) {
        String v = LegacyJsonBuilder.firstOrNull(kv, legacyKey);
        json.put(field, v != null && !v.isBlank() && Boolean.parseBoolean(v.trim()));
    }

    private static void putWallInt(Map<String, Object> json, String field, Map<String, List<String>> kv, String legacyKey) {
        String v = LegacyJsonBuilder.firstOrNull(kv, legacyKey);
        if (v == null || v.isBlank()) {
            return;
        }
        try {
            json.put(field, Integer.parseInt(v.trim()));
        }
        catch (NumberFormatException numberFormatException) {
            // empty catch block
        }
    }

    private static String firstOrNull(Map<String, List<String>> m, String key) {
        List<String> v = m.get(key);
        if (v == null || v.isEmpty()) {
            return null;
        }
        return v.get(0);
    }

    public static String sanitize(String name) {
        return LegacyIdCanonicaliser.sanitize(name);
    }

    public static String prefixedId(String culture, String id) {
        return LegacyIdCanonicaliser.prefixed(culture, id);
    }

    public static String stripCulturePrefix(String culture, String id) {
        return LegacyIdCanonicaliser.stripCulturePrefix(culture, id);
    }

    public static String legacyItemToGoodId(String legacyItem) {
        String explicit = LEGACY_TO_GOOD_ID.get(legacyItem);
        if (explicit == null) {
            explicit = LEGACY_TO_GOOD_ID.get(legacyItem.toLowerCase());
        }
        if (explicit != null) {
            return explicit;
        }
        return switch (legacyItem.toLowerCase()) {
            case "normanaxe" -> "norman_axe";
            case "normanpickaxe" -> "norman_pickaxe";
            case "normanshovel" -> "norman_shovel";
            case "normanhoe" -> "norman_hoe";
            case "normanhelmet" -> "norman_helmet";
            case "normanplate" -> "norman_chestplate";
            case "normanlegs" -> "norman_leggings";
            case "normanboots" -> "norman_boots";
            case "normansword" -> "norman_sword";
            case "cider" -> "cider";
            case "calva" -> "calva";
            case "ciderapple" -> "cider_apple";
            case "tripes" -> "tripes";
            case "boudin" -> "boudin";
            case "bottle" -> "bottle";
            case "netherwart" -> "nether_wart";
            case "beefraw" -> "beef_raw";
            case "beefcooked" -> "beef_cooked";
            case "porkchopscooked" -> "porkchop_cooked";
            case "chickenmeat" -> "chicken_raw";
            case "chickenmeatcooked" -> "chicken_cooked";
            case "stained_glass_white" -> "stained_glass_white";
            case "stained_glass_yellow" -> "stained_glass_yellow";
            case "stained_glass_yellow_red" -> "stained_glass_yellow_red";
            case "stained_glass_red_blue" -> "stained_glass_red_blue";
            case "stained_glass_green_blue" -> "stained_glass_green_blue";
            case "rosette" -> "rosette";
            case "timberframeplain" -> "timber_frame_plain";
            case "timberframecross" -> "timber_frame_cross";
            case "bed_straw" -> "straw_bed";
            case "dirtwall" -> "dirt_wall";
            case "pathdirt" -> "path_dirt";
            case "pathgravel" -> "path_gravel";
            case "pathslabs" -> "path_slabs";
            case "pathgravelslabs" -> "path_gravel_slab";
            case "pathochretiles" -> "path_ochre_tiles";
            case "pathsandstone" -> "path_sandstone";
            case "dye_white" -> "dye_white";
            case "dye_red" -> "dye_red";
            case "dye_yellow" -> "dye_yellow";
            case "dye_lightblue" -> "dye_light_blue";
            case "carpet_white" -> "carpet_white";
            case "carpet_red" -> "carpet_red";
            case "carpet_yellow" -> "carpet_yellow";
            case "carpet_blue" -> "carpet_blue";
            case "tapestry" -> "tapestry";
            case "banner_white" -> "banner_white";
            case "cauldron" -> "cauldron";
            case "seeds" -> "seeds";
            case "sand" -> "sand";
            case "sugar" -> "sugar";
            case "bow" -> "bow";
            case "arrow" -> "arrow";
            case "cake" -> "cake";
            case "carrot" -> "carrot";
            case "iron" -> "iron_ingot";
            case "gold" -> "gold_ingot";
            case "glass" -> "glass";
            case "purse" -> "purse";
            case "summoningwand" -> "summoning_wand";
            case "sapling_appletree" -> "sapling_appletree";
            case "norpattern" -> "norpattern";
            case "feather" -> "feather";
            case "bone" -> "bone";
            case "paper" -> "paper";
            case "book" -> "book";
            case "turmeric" -> "turmeric";
            case "rasgulla" -> "rasgulla";
            case "chickencurry" -> "chickencurry";
            case "vegcurry" -> "vegcurry";
            case "rice" -> "rice";
            case "cotton" -> "cotton";
            case "brickmould" -> "brick_mould";
            case "paintbucketwhite" -> "paint_bucket_white";
            case "decoratedbrickwhite" -> "decorated_brick_white";
            case "paintedbrickwhite" -> "painted_brick_white";
            case "indianstatue" -> "indian_statue";
            case "woodenbarsindian" -> "wooden_bars_indian";
            case "bed_charpoy" -> "charpoy";
            case "sandstone_carved" -> "sandstone_carved";
            case "red_sandstone_carved" -> "red_sandstone_carved";
            case "ochre_sandstone_carved" -> "ochre_sandstone_carved";
            case "mudbrick" -> "mud_brick";
            case "thatch" -> "thatch";
            case "woodenbars" -> "wooden_bars";
            case "woodenbarsrosette" -> "wooden_bars_rosette";
            case "sandstone" -> "sandstone";
            case "redsandstone" -> "red_sandstone";
            case "diamond" -> "diamond";
            case "dye_blue" -> "dye_blue";
            case "cactus" -> "cactus";
            case "fishcooked" -> "cooked_cod";
            case "cooked_mutton" -> "cooked_mutton";
            case "cookie" -> "cookie";
            case "sugarcane" -> "sugar_cane";
            case "stonesword" -> "stone_sword";
            case "stonepickaxe" -> "stone_pickaxe";
            case "stoneaxe" -> "stone_axe";
            case "stoneshovel" -> "stone_shovel";
            case "stonehoe" -> "stone_hoe";
            case "steelsword" -> "iron_sword";
            case "steelhelmet" -> "iron_helmet";
            case "steelchest" -> "iron_chestplate";
            case "steellegs" -> "iron_leggings";
            case "steelboots" -> "iron_boots";
            case "steelpickaxe" -> "iron_pickaxe";
            case "steelaxe" -> "iron_axe";
            case "steelshovel" -> "iron_shovel";
            case "steelhoe" -> "iron_hoe";
            case "leatherhelmet" -> "leather_helmet";
            case "leatherchest" -> "leather_chestplate";
            case "leatherlegs" -> "leather_leggings";
            case "leatherboots" -> "leather_boots";
            case "diamondsword" -> "diamond_sword";
            case "diamondhelmet" -> "diamond_helmet";
            case "diamondchest" -> "diamond_chestplate";
            case "diamondlegs" -> "diamond_leggings";
            case "diamondboots" -> "diamond_boots";
            case "diamondpickaxe" -> "diamond_pickaxe";
            case "diamondaxe" -> "diamond_axe";
            case "diamondshovel" -> "diamond_shovel";
            case "diamondhoe" -> "diamond_hoe";
            default -> legacyItem.toLowerCase();
        };
    }

    public static Map<String, Object> buildNonCraftingGatheringTypeJson(String category, Map<String, List<String>> config, String culture, ItemIdMapper items) {
        String baseCategory;
        return switch (baseCategory = category.contains("/") ? category.substring(0, category.indexOf(47)) : category) {
            case "genericharvesting" -> LegacyJsonBuilder.buildHarvestingJson(config, culture, items);
            case "genericplanting" -> LegacyJsonBuilder.buildPlantingJson(config, culture, items);
            case "genericmining" -> LegacyJsonBuilder.buildMiningJson(config, culture, items);
            case "genericslaughteranimal" -> LegacyJsonBuilder.buildSlaughterJson(config, culture, items);
            case "genericcooking" -> LegacyJsonBuilder.buildSmeltingJson(config, culture, items);
            case "genericplantsapling" -> LegacyJsonBuilder.buildSaplingPlantingJson(config, culture, items);
            case "genericgatherblocks" -> LegacyJsonBuilder.buildFruitHarvestingJson(config, culture, items);
            case "generictakefrombuilding" -> LegacyJsonBuilder.buildTakeFromBuildingJson(config, culture, items);
            default -> null;
        };
    }

    static Map<String, Object> buildHarvestingJson(Map<String, List<String>> config, String culture, ItemIdMapper items) {
        int reoccur;
        int maxTotal;
        LinkedHashMap<String, Object> json = new LinkedHashMap<String, Object>();
        json.put("handler", "harvesting");
        int priority = LegacyJsonBuilder.getIntValue(config, "priority", 40);
        json.put("priority", priority);
        if (config.containsKey("priorityrandom")) {
            json.put("priorityRandom", LegacyJsonBuilder.getIntValue(config, "priorityrandom", 10));
        }
        json.put("scanRadius", 32);
        json.put("batchRadius", 8);
        json.put("maxActionsPerTask", 16);
        json.put("actionCooldown", 10);
        json.put("stuckTimeout", 4000);
        json.put("arrivalRange", 8);
        json.put("walkSpeed", 0.6);
        LegacyJsonBuilder.parseLimitField(config, "villagelimit", culture, items).ifPresent(lim -> json.put("villageLimit", lim));
        LegacyJsonBuilder.parseLimitField(config, "buildinglimit", culture, items).ifPresent(lim -> json.put("buildingLimit", lim));
        LegacyJsonBuilder.parseLimitField(config, "townhalllimit", culture, items).ifPresent(lim -> json.put("townhallLimit", lim));
        int maxInBuilding = LegacyJsonBuilder.getIntValue(config, "maxsimultaneousinbuilding", -1);
        if (maxInBuilding > 0) {
            json.put("maxSimultaneousInBuilding", maxInBuilding);
        }
        if ((maxTotal = LegacyJsonBuilder.getIntValue(config, "maxsimultaneoustotal", -1)) > 0) {
            json.put("maxSimultaneousTotal", maxTotal);
        }
        json.put("minimumHour", -1);
        json.put("maximumHour", 12500);
        json.put("reoccurDelay", -1);
        LinkedHashMap<String, Object> handlerParams = new LinkedHashMap<String, Object>();
        String cropType = LegacyJsonBuilder.getStringValue(config, "croptype");
        if ("flower".equals(cropType)) {
            String buildingTag;
            String[] hparts;
            String harvestBlockstate = LegacyJsonBuilder.getStringValue(config, "harvestblockstate");
            if (harvestBlockstate != null) {
                String mapped;
                String modernBlock = LEGACY_FLOWER_MAP.get(harvestBlockstate);
                if (modernBlock == null && !(mapped = LegacyJsonBuilder.mapLegacyBlockStateLine(harvestBlockstate)).equals(harvestBlockstate)) {
                    modernBlock = mapped;
                }
                if (modernBlock != null) {
                    handlerParams.put("targetBlock", modernBlock);
                } else {
                    System.out.println("    [WARN] Unknown flower blockstate: " + harvestBlockstate);
                    handlerParams.put("targetBlock", harvestBlockstate);
                }
            }
            handlerParams.put("soilSubtype", "flower");
            String sentenceKey = LegacyJsonBuilder.getStringValue(config, "sentencekey");
            boolean isSteal = "stealflower".equals(sentenceKey);
            boolean isZeroChanceHarvest = false;
            List harvestItemFlower = config.getOrDefault("harvestitem", List.of());
            if (!harvestItemFlower.isEmpty() && (hparts = ((String)harvestItemFlower.getFirst()).split(",", 2)).length >= 2) {
                try {
                    isZeroChanceHarvest = Integer.parseInt(hparts[1].trim()) == 0;
                }
                catch (NumberFormatException numberFormatException) {
                    // empty catch block
                }
            }
            if (isSteal || isZeroChanceHarvest) {
                handlerParams.put("skipDrops", true);
            }
            if ((buildingTag = LegacyJsonBuilder.getStringValue(config, "buildingtag")) != null) {
                handlerParams.put("buildingTag", buildingTag);
            }
        } else if (cropType != null) {
            String targetBlock = LEGACY_CROP_BLOCK_MAP.getOrDefault(cropType, cropType);
            if ("millenaire:crop_rice".equals(targetBlock)) {
                String buildingTag;
                json.put("handler", "paddy_harvesting");
                soilSubtype = CROP_SOIL_SUBTYPE_MAP.get(targetBlock);
                if (soilSubtype != null) {
                    handlerParams.put("soilSubtype", soilSubtype);
                }
                if ((buildingTag = LegacyJsonBuilder.getStringValue(config, "buildingtag")) != null) {
                    handlerParams.put("buildingTag", buildingTag);
                }
            } else {
                handlerParams.put("targetBlock", targetBlock);
                handlerParams.put("targetState", Map.of("age", 7));
                soilSubtype = CROP_SOIL_SUBTYPE_MAP.get(targetBlock);
                if (soilSubtype != null) {
                    handlerParams.put("soilSubtype", soilSubtype);
                } else {
                    System.out.println("    [WARN] No soilSubtype mapping for targetBlock: " + targetBlock + " (cropType: " + cropType + ")");
                }
                String buildingTag = LegacyJsonBuilder.getStringValue(config, "buildingtag");
                if (buildingTag != null) {
                    handlerParams.put("buildingTag", buildingTag);
                }
            }
        }
        String irrigationBonusCrop = LegacyJsonBuilder.getStringValue(config, "irrigationbonuscrop");
        if (irrigationBonusCrop != null) {
            items.resolve(culture, irrigationBonusCrop).ifPresent(mc -> handlerParams.put("irrigationBonusCrop", mc));
        }
        if ((reoccur = LegacyJsonBuilder.getIntValue(config, "reoccurdelay", -1)) > 0) {
            json.put("reoccurDelay", reoccur / 50);
        }
        json.put("handlerParams", handlerParams);
        LegacyJsonBuilder.addSentenceAndLabelKeys(config, json);
        LegacyJsonBuilder.addHeldItems(config, json, culture, items);
        LegacyJsonBuilder.addLeisureFlag(config, json);
        return json;
    }

    static Map<String, Object> buildPlantingJson(Map<String, List<String>> config, String culture, ItemIdMapper items) {
        int maxTotal;
        LinkedHashMap<String, Object> json = new LinkedHashMap<String, Object>();
        json.put("handler", "planting");
        int priority = LegacyJsonBuilder.getIntValue(config, "priority", 50);
        json.put("priority", priority);
        if (config.containsKey("priorityrandom")) {
            json.put("priorityRandom", LegacyJsonBuilder.getIntValue(config, "priorityrandom", 10));
        }
        json.put("scanRadius", 32);
        json.put("batchRadius", 8);
        json.put("maxActionsPerTask", 16);
        json.put("actionCooldown", 10);
        json.put("stuckTimeout", 4000);
        json.put("walkSpeed", 0.6);
        int maxInBuilding = LegacyJsonBuilder.getIntValue(config, "maxsimultaneousinbuilding", -1);
        if (maxInBuilding > 0) {
            json.put("maxSimultaneousInBuilding", maxInBuilding);
        }
        if ((maxTotal = LegacyJsonBuilder.getIntValue(config, "maxsimultaneoustotal", -1)) > 0) {
            json.put("maxSimultaneousTotal", maxTotal);
        }
        json.put("minimumHour", -1);
        json.put("maximumHour", 12500);
        json.put("reoccurDelay", -1);
        LinkedHashMap<String, Object> handlerParams = new LinkedHashMap<String, Object>();
        String cropType = LegacyJsonBuilder.getStringValue(config, "croptype");
        if ("flower".equals(cropType)) {
            String buildingTag;
            String legacySeed;
            json.put("handler", "flower_planting");
            List<String> flowers = LegacyJsonBuilder.parseFlowerBlockstates(config, "plantblockstate");
            if (!flowers.isEmpty()) {
                handlerParams.put("flowers", flowers);
            }
            if ((legacySeed = LegacyJsonBuilder.getStringValue(config, "seed")) != null) {
                String seedItem = "dye_white".equals(legacySeed) ? "minecraft:bone_meal" : items.resolve(culture, legacySeed).orElse(legacySeed);
                handlerParams.put("seedItem", seedItem);
            }
            if ((buildingTag = LegacyJsonBuilder.getStringValue(config, "buildingtag")) == null) {
                buildingTag = LegacyJsonBuilder.getStringValue(config, "requiredtag");
            }
            if (buildingTag != null) {
                handlerParams.put("buildingTag", buildingTag);
            }
        } else if (cropType != null) {
            String cropBlock = LEGACY_CROP_BLOCK_MAP.getOrDefault(cropType, cropType);
            if ("millenaire:crop_rice".equals(cropBlock)) {
                String buildingTag;
                json.put("handler", "paddy_planting");
                String soilSubtype = CROP_SOIL_SUBTYPE_MAP.get(cropBlock);
                if (soilSubtype != null) {
                    handlerParams.put("soilSubtype", soilSubtype);
                }
                if ((buildingTag = LegacyJsonBuilder.getStringValue(config, "buildingtag")) != null) {
                    handlerParams.put("buildingTag", buildingTag);
                }
            } else {
                String soilSubtype;
                String seedItem;
                handlerParams.put("cropBlock", cropBlock);
                String soilBlock = CROP_SOIL_MAP.get(cropBlock);
                if (soilBlock != null) {
                    handlerParams.put("soilBlock", soilBlock);
                }
                if ((seedItem = CROP_SEED_MAP.get(cropBlock)) != null) {
                    handlerParams.put("seedItem", seedItem);
                }
                if ((soilSubtype = CROP_SOIL_SUBTYPE_MAP.get(cropBlock)) != null) {
                    handlerParams.put("soilSubtype", soilSubtype);
                } else {
                    System.out.println("    [WARN] No soilSubtype mapping for cropBlock: " + cropBlock + " (cropType: " + cropType + ")");
                }
                String buildingTag = LegacyJsonBuilder.getStringValue(config, "buildingtag");
                if (buildingTag != null) {
                    handlerParams.put("buildingTag", buildingTag);
                }
            }
        } else {
            json.put("handler", "flower_planting");
            String buildingTag = LegacyJsonBuilder.getStringValue(config, "buildingtag");
            if (buildingTag != null) {
                handlerParams.put("buildingTag", buildingTag);
            }
        }
        int reoccur = LegacyJsonBuilder.getIntValue(config, "reoccurdelay", -1);
        if (reoccur > 0) {
            json.put("reoccurDelay", reoccur / 50);
        }
        json.put("handlerParams", handlerParams);
        LegacyJsonBuilder.addSentenceAndLabelKeys(config, json);
        LegacyJsonBuilder.addHeldItems(config, json, culture, items);
        LegacyJsonBuilder.addLeisureFlag(config, json);
        return json;
    }

    static Map<String, Object> buildMiningJson(Map<String, List<String>> config, String culture, ItemIdMapper items) {
        List lootLines;
        String buildingTag;
        String baseBlock;
        String sourceSubtype;
        LinkedHashMap<String, Object> json = new LinkedHashMap<String, Object>();
        json.put("handler", "mining");
        int priority = LegacyJsonBuilder.getIntValue(config, "priority", 35);
        json.put("priority", priority);
        if (config.containsKey("priorityrandom")) {
            json.put("priorityRandom", LegacyJsonBuilder.getIntValue(config, "priorityrandom", 10));
        }
        json.put("scanRadius", 32);
        json.put("batchRadius", 8);
        json.put("maxActionsPerTask", 8);
        json.put("actionCooldown", 70);
        json.put("stuckTimeout", 200);
        json.put("arrivalRange", 5);
        json.put("walkSpeed", 0.6);
        LegacyJsonBuilder.parseLimitField(config, "townhalllimit", culture, items).ifPresent(lim -> json.put("townhallLimit", lim));
        LegacyJsonBuilder.parseLimitField(config, "villagelimit", culture, items).ifPresent(lim -> json.put("villageLimit", lim));
        LegacyJsonBuilder.parseLimitField(config, "buildinglimit", culture, items).ifPresent(lim -> json.put("buildingLimit", lim));
        int maxInBuilding = LegacyJsonBuilder.getIntValue(config, "maxsimultaneousinbuilding", -1);
        if (maxInBuilding > 0) {
            json.put("maxSimultaneousInBuilding", maxInBuilding);
        }
        int maxTotal = LegacyJsonBuilder.getIntValue(config, "maxsimultaneoustotal", 1);
        json.put("maxSimultaneousTotal", maxTotal);
        json.put("minimumHour", -1);
        json.put("maximumHour", 12500);
        LinkedHashMap<String, Object> handlerParams = new LinkedHashMap<String, Object>();
        String sourceBlockState = LegacyJsonBuilder.getStringValue(config, "sourceblockstate");
        if (sourceBlockState != null && (sourceSubtype = MINING_SOURCE_MAP.get(baseBlock = sourceBlockState.contains(";") ? sourceBlockState.substring(0, sourceBlockState.indexOf(59)) : sourceBlockState)) != null) {
            handlerParams.put("sourceSubtype", sourceSubtype);
        }
        if ((buildingTag = LegacyJsonBuilder.getStringValue(config, "buildingtag")) != null) {
            handlerParams.put("buildingTag", buildingTag);
        }
        if (!(lootLines = config.getOrDefault("loot", List.of())).isEmpty()) {
            ArrayList loot = new ArrayList();
            for (String lootLine : lootLines) {
                int count;
                String[] parts = lootLine.split(",", 2);
                String legacyItem = parts[0].trim();
                int n = count = parts.length > 1 ? Integer.parseInt(parts[1].trim()) : 1;
                String modernId = items.resolve(culture, legacyItem).orElse(null);
                if (modernId == null) continue;
                LinkedHashMap<String, Object> entry = new LinkedHashMap<String, Object>();
                entry.put("item", modernId);
                entry.put("count", count);
                loot.add(entry);
            }
            if (!loot.isEmpty()) {
                handlerParams.put("loot", loot);
            }
        }
        json.put("handlerParams", handlerParams);
        LegacyJsonBuilder.addSentenceAndLabelKeys(config, json);
        return json;
    }

    static Map<String, Object> buildSlaughterJson(Map<String, List<String>> config, String culture, ItemIdMapper items) {
        LinkedHashMap<String, Object> json = new LinkedHashMap<String, Object>();
        json.put("handler", "slaughter");
        int priority = LegacyJsonBuilder.getIntValue(config, "priority", 50);
        json.put("priority", priority);
        if (config.containsKey("priorityrandom")) {
            json.put("priorityRandom", LegacyJsonBuilder.getIntValue(config, "priorityrandom", 10));
        }
        json.put("scanRadius", 25);
        json.put("batchRadius", 25);
        json.put("maxActionsPerTask", 3);
        json.put("actionCooldown", 20);
        json.put("stuckTimeout", 4000);
        json.put("arrivalRange", LegacyJsonBuilder.getIntValue(config, "range", 1));
        json.put("walkSpeed", 0.7);
        json.put("minimumHour", -1);
        json.put("maximumHour", 12500);
        json.put("maxSimultaneousTotal", 1);
        LinkedHashMap<String, Object> handlerParams = new LinkedHashMap<String, Object>();
        String animalKey = LegacyJsonBuilder.getStringValue(config, "animalkey");
        if (animalKey != null) {
            String modernAnimal = LEGACY_ANIMAL_MAP.getOrDefault(animalKey, "minecraft:" + animalKey);
            handlerParams.put("animalType", modernAnimal);
        }
        String buildingTag = LegacyJsonBuilder.getStringValue(config, "buildingtag");
        String requiredTag = LegacyJsonBuilder.getStringValue(config, "requiredtag");
        if (buildingTag != null) {
            handlerParams.put("buildingTag", buildingTag);
        } else if (requiredTag != null) {
            handlerParams.put("buildingTag", requiredTag);
        }
        if (requiredTag != null) {
            handlerParams.put("requiredTag", requiredTag);
        }
        handlerParams.put("damage", 4.0);
        List<String> bonusLines = config.get("bonusitem");
        if (bonusLines != null && !bonusLines.isEmpty()) {
            ArrayList bonusItems = new ArrayList();
            for (String line : bonusLines) {
                String modernItem;
                String[] parts = line.split(",");
                if (parts.length < 2 || (modernItem = (String)items.resolve(culture, parts[0].trim()).orElse(null)) == null) continue;
                try {
                    LinkedHashMap<String, Object> bonus = new LinkedHashMap<String, Object>();
                    bonus.put("item", modernItem);
                    bonus.put("chance", Integer.parseInt(parts[1].trim()));
                    if (parts.length >= 3) {
                        bonus.put("requiredTag", parts[2].trim());
                    }
                    bonusItems.add(bonus);
                }
                catch (NumberFormatException e) {
                    System.out.println("  [WARN] Invalid bonusitem chance: " + line);
                }
            }
            if (!bonusItems.isEmpty()) {
                handlerParams.put("bonusItems", bonusItems);
            }
        }
        json.put("handlerParams", handlerParams);
        LegacyJsonBuilder.addSentenceAndLabelKeys(config, json);
        return json;
    }

    static Map<String, Object> buildSmeltingJson(Map<String, List<String>> config, String culture, ItemIdMapper items) {
        String requiredTag;
        int minimumToCook;
        String[] parts;
        String balanceLine;
        LinkedHashMap<String, Object> json = new LinkedHashMap<String, Object>();
        json.put("handler", "smelting");
        int priority = LegacyJsonBuilder.getIntValue(config, "priority", 50);
        json.put("priority", priority);
        if (config.containsKey("priorityrandom")) {
            json.put("priorityRandom", LegacyJsonBuilder.getIntValue(config, "priorityrandom", 10));
        }
        json.put("scanRadius", 32);
        json.put("batchRadius", 8);
        json.put("maxActionsPerTask", 3);
        json.put("actionCooldown", 100);
        json.put("stuckTimeout", 4000);
        json.put("walkSpeed", 0.6);
        LegacyJsonBuilder.parseLimitField(config, "villagelimit", culture, items).ifPresent(lim -> json.put("villageLimit", lim));
        LegacyJsonBuilder.parseLimitField(config, "buildinglimit", culture, items).ifPresent(lim -> json.put("buildingLimit", lim));
        LegacyJsonBuilder.parseLimitField(config, "townhalllimit", culture, items).ifPresent(lim -> json.put("townhallLimit", lim));
        int maxInBuilding = LegacyJsonBuilder.getIntValue(config, "maxsimultaneousinbuilding", -1);
        if (maxInBuilding > 0) {
            json.put("maxSimultaneousInBuilding", maxInBuilding);
        }
        if ((balanceLine = LegacyJsonBuilder.getStringValue(config, "itemsbalance")) != null && (parts = balanceLine.split(",", 2)).length == 2) {
            String inputItem = items.resolve(culture, parts[0].trim()).orElse(null);
            String outputItem = items.resolve(culture, parts[1].trim()).orElse(null);
            if (inputItem != null && outputItem != null) {
                json.put("itemsBalance", Map.of(inputItem, outputItem));
            }
        }
        LinkedHashMap<String, Object> handlerParams = new LinkedHashMap<String, Object>();
        String itemToCook = LegacyJsonBuilder.getStringValue(config, "itemtocook");
        if (itemToCook != null) {
            String buildingTag;
            String[] mapping = LEGACY_COOKING_MAP.get(itemToCook);
            if (mapping != null) {
                LinkedHashMap<String, Object> input = new LinkedHashMap<String, Object>();
                input.put("item", mapping[0]);
                input.put("count", 1);
                LinkedHashMap<String, Object> output = new LinkedHashMap<String, Object>();
                output.put("item", mapping[1]);
                output.put("count", 1);
                handlerParams.put("inputs", List.of(input));
                handlerParams.put("outputs", List.of(output));
            }
            if ((buildingTag = LegacyJsonBuilder.getStringValue(config, "buildingtag")) != null) {
                handlerParams.put("buildingTag", buildingTag);
            }
        }
        if ((minimumToCook = LegacyJsonBuilder.getIntValue(config, "minimumtocook", 3)) != 3) {
            handlerParams.put("minimumToCook", minimumToCook);
        }
        if ((requiredTag = LegacyJsonBuilder.getStringValue(config, "requiredtag")) != null) {
            if (!handlerParams.containsKey("buildingTag")) {
                handlerParams.put("buildingTag", requiredTag);
            } else {
                handlerParams.put("requiredTag", requiredTag);
            }
        }
        json.put("handlerParams", handlerParams);
        LegacyJsonBuilder.addSentenceAndLabelKeys(config, json);
        LegacyJsonBuilder.addHeldItems(config, json, culture, items);
        return json;
    }

    static Map<String, Object> buildSaplingPlantingJson(Map<String, List<String>> config, String culture, ItemIdMapper items) {
        String mapped;
        List heldItems;
        LinkedHashMap<String, Object> json = new LinkedHashMap<String, Object>();
        json.put("handler", "sapling_planting");
        int priority = LegacyJsonBuilder.getIntValue(config, "priority", 2000);
        json.put("priority", priority);
        if (config.containsKey("priorityrandom")) {
            json.put("priorityRandom", LegacyJsonBuilder.getIntValue(config, "priorityrandom", 10));
        }
        json.put("scanRadius", 48);
        json.put("batchRadius", 8);
        json.put("maxActionsPerTask", 5);
        json.put("actionCooldown", 20);
        json.put("stuckTimeout", 4000);
        json.put("arrivalRange", 5);
        json.put("walkSpeed", 0.6);
        int maxInBuilding = LegacyJsonBuilder.getIntValue(config, "maxsimultaneousinbuilding", -1);
        if (maxInBuilding > 0) {
            json.put("maxSimultaneousInBuilding", maxInBuilding);
        }
        json.put("minimumHour", 0);
        json.put("maximumHour", 12500);
        LinkedHashMap<String, String> handlerParams = new LinkedHashMap<String, String>();
        String buildingTag = LegacyJsonBuilder.getStringValue(config, "buildingtag");
        if (buildingTag != null) {
            handlerParams.put("buildingTag", buildingTag);
        }
        if (!(heldItems = config.getOrDefault("helditems", List.of())).isEmpty() && (mapped = (String)items.resolve(culture, ((String)heldItems.getFirst()).trim()).orElse(null)) != null) {
            handlerParams.put("sapling", mapped);
        }
        json.put("handlerParams", handlerParams);
        LegacyJsonBuilder.addSentenceAndLabelKeys(config, json);
        return json;
    }

    static Map<String, Object> buildFruitHarvestingJson(Map<String, List<String>> config, String culture, ItemIdMapper items) {
        String buildingTag;
        String[] parts;
        String mapped;
        List harvestItems;
        String[] bsParts;
        String resultingBlockState;
        String[] kv;
        String[] props;
        LinkedHashMap<String, Object> json = new LinkedHashMap<String, Object>();
        json.put("handler", "fruit_harvesting");
        int priority = LegacyJsonBuilder.getIntValue(config, "priority", 80);
        json.put("priority", priority);
        if (config.containsKey("priorityrandom")) {
            json.put("priorityRandom", LegacyJsonBuilder.getIntValue(config, "priorityrandom", 10));
        }
        json.put("scanRadius", 32);
        json.put("batchRadius", 8);
        json.put("maxActionsPerTask", 16);
        int durationMs = LegacyJsonBuilder.getIntValue(config, "duration", 4000);
        json.put("actionCooldown", Math.max(10, durationMs / 50));
        json.put("stuckTimeout", 4000);
        json.put("arrivalRange", 8);
        json.put("walkSpeed", 0.6);
        LegacyJsonBuilder.parseLimitField(config, "buildinglimit", culture, items).ifPresent(lim -> json.put("buildingLimit", lim));
        LegacyJsonBuilder.parseLimitField(config, "villagelimit", culture, items).ifPresent(lim -> json.put("villageLimit", lim));
        int maxInBuilding = LegacyJsonBuilder.getIntValue(config, "maxsimultaneousinbuilding", -1);
        if (maxInBuilding > 0) {
            json.put("maxSimultaneousInBuilding", maxInBuilding);
        }
        json.put("minimumHour", -1);
        json.put("maximumHour", 12500);
        LinkedHashMap<String, Object> handlerParams = new LinkedHashMap<String, Object>();
        String gatherBlockState = LegacyJsonBuilder.getStringValue(config, "gatherblockstate");
        if (gatherBlockState != null) {
            String[] bsParts2 = gatherBlockState.split(";", 2);
            String modernBlock = LegacyJsonBuilder.mapLegacyBlockStateLine(gatherBlockState);
            handlerParams.put("targetBlock", modernBlock);
            if (bsParts2.length > 1) {
                for (String prop : props = bsParts2[1].split(",")) {
                    kv = prop.split("=", 2);
                    if (kv.length != 2 || !"age".equals(kv[0].trim())) continue;
                    handlerParams.put("ageProperty", "age");
                    handlerParams.put("ripeAge", Integer.parseInt(kv[1].trim()));
                }
            }
        }
        if ((resultingBlockState = LegacyJsonBuilder.getStringValue(config, "resultingblockstate")) != null && (bsParts = resultingBlockState.split(";", 2)).length > 1) {
            for (String prop : props = bsParts[1].split(",")) {
                kv = prop.split("=", 2);
                if (kv.length != 2 || !"age".equals(kv[0].trim())) continue;
                handlerParams.put("resetAge", Integer.parseInt(kv[1].trim()));
            }
        }
        if (!(harvestItems = config.getOrDefault("harvestitem", List.of())).isEmpty() && (mapped = (String)items.resolve(culture, (parts = ((String)harvestItems.getFirst()).split(",", 2))[0].trim()).orElse(null)) != null) {
            handlerParams.put("harvestItem", mapped);
            handlerParams.put("harvestCount", 1);
        }
        if ((buildingTag = LegacyJsonBuilder.getStringValue(config, "buildingtag")) != null) {
            handlerParams.put("buildingTag", buildingTag);
        }
        json.put("handlerParams", handlerParams);
        LegacyJsonBuilder.addSentenceAndLabelKeys(config, json);
        LegacyJsonBuilder.addHeldItems(config, json, culture, items);
        return json;
    }

    static String mapLegacyFruitBlock(String legacyBlockId) {
        return switch (legacyBlockId) {
            case "millenaire:leaves_appletree" -> "millenaire:apple_tree_leaves";
            case "millenaire:leaves_olivetree" -> "millenaire:olive_tree_leaves";
            case "millenaire:leaves_pistachio" -> "millenaire:pistachio_tree_leaves";
            case "minecraft:tallgrass" -> "minecraft:short_grass";
            case "minecraft:hardened_clay" -> "minecraft:terracotta";
            case "minecraft:stained_hardened_clay" -> "minecraft:terracotta";
            case "minecraft:red_flower", "red_flower" -> "minecraft:poppy";
            case "minecraft:yellow_flower", "yellow_flower" -> "minecraft:dandelion";
            default -> legacyBlockId;
        };
    }

    static String mapLegacyBlockStateLine(String stateLine) {
        if (stateLine == null) {
            return null;
        }
        String[] bsParts = stateLine.split(";", 2);
        String blockId = bsParts[0].trim();
        if (bsParts.length < 2) {
            return LegacyJsonBuilder.mapLegacyFruitBlock(blockId);
        }
        String subtype = null;
        for (String prop : bsParts[1].split(",")) {
            String[] kv = prop.split("=", 2);
            if (kv.length != 2 || !"type".equals(kv[0].trim())) continue;
            subtype = kv[1].trim();
            break;
        }
        if (subtype != null) {
            String resolved;
            switch (blockId + ";type=" + subtype) {
                case "minecraft:tallgrass;type=fern": {
                    String string = "minecraft:fern";
                    break;
                }
                case "minecraft:tallgrass;type=tall_grass": {
                    String string = "minecraft:short_grass";
                    break;
                }
                case "minecraft:tallgrass;type=dead_bush": {
                    String string = "minecraft:dead_bush";
                    break;
                }
                default: {
                    String string = resolved = null;
                }
            }
            if (resolved != null) {
                return resolved;
            }
        }
        return LegacyJsonBuilder.mapLegacyFruitBlock(blockId);
    }

    static Map<String, Object> buildTakeFromBuildingJson(Map<String, List<String>> config, String culture, ItemIdMapper items) {
        int minPickup;
        String[] parts;
        String mapped;
        String collectGood;
        LinkedHashMap<String, Object> json = new LinkedHashMap<String, Object>();
        json.put("handler", "take_from_building");
        int priority = LegacyJsonBuilder.getIntValue(config, "priority", 100);
        json.put("priority", priority);
        if (config.containsKey("priorityrandom")) {
            json.put("priorityRandom", LegacyJsonBuilder.getIntValue(config, "priorityrandom", 10));
        }
        json.put("scanRadius", 32);
        json.put("batchRadius", 8);
        json.put("maxActionsPerTask", 1);
        json.put("actionCooldown", 20);
        json.put("stuckTimeout", 4000);
        json.put("walkSpeed", 0.6);
        int maxTotal = LegacyJsonBuilder.getIntValue(config, "maxsimultaneoustotal", -1);
        if (maxTotal > 0) {
            json.put("maxSimultaneousTotal", maxTotal);
        }
        json.put("minimumHour", -1);
        json.put("maximumHour", 12500);
        LinkedHashMap<String, Object> handlerParams = new LinkedHashMap<String, Object>();
        String buildingTag = LegacyJsonBuilder.getStringValue(config, "buildingtag");
        if (buildingTag != null) {
            handlerParams.put("buildingTag", buildingTag);
        }
        if ((collectGood = LegacyJsonBuilder.getStringValue(config, "collect_good")) != null && (mapped = (String)items.resolve(culture, (parts = collectGood.split(",", 2))[0].trim()).orElse(null)) != null) {
            handlerParams.put("collectGood", mapped);
            if (parts.length > 1) {
                handlerParams.put("maxCollect", Integer.parseInt(parts[1].trim()));
            }
        }
        if ((minPickup = LegacyJsonBuilder.getIntValue(config, "minimumpickup", -1)) > 0) {
            handlerParams.put("minimumPickup", minPickup);
        }
        json.put("handlerParams", handlerParams);
        LegacyJsonBuilder.addSentenceAndLabelKeys(config, json);
        return json;
    }

    static void addSentenceAndLabelKeys(Map<String, List<String>> config, Map<String, Object> json) {
        String labelKey;
        String sentenceKey = LegacyJsonBuilder.getStringValue(config, "sentencekey");
        if (sentenceKey != null) {
            json.put("sentenceKey", sentenceKey);
        }
        if ((labelKey = LegacyJsonBuilder.getStringValue(config, "labelkey")) != null) {
            json.put("labelKey", labelKey);
        }
    }

    static List<String> parseFlowerBlockstates(Map<String, List<String>> config, String key) {
        ArrayList<String> result = new ArrayList<String>();
        List entries = config.getOrDefault(key, List.of());
        for (String entry : entries) {
            String modernBlock = LEGACY_FLOWER_MAP.get(entry.trim());
            if (modernBlock != null) {
                result.add(modernBlock);
                continue;
            }
            System.out.println("    [WARN] Unknown flower blockstate: " + entry);
        }
        return result;
    }

    static void addHeldItems(Map<String, List<String>> config, Map<String, Object> json, String culture, ItemIdMapper items) {
        ArrayList allHeld = new ArrayList();
        for (String raw : config.getOrDefault("helditems", List.of())) {
            for (String item : raw.split(",")) {
                items.resolve(culture, item.trim()).ifPresent(mapped -> {
                    if (!allHeld.contains(mapped)) {
                        allHeld.add(mapped);
                    }
                });
            }
        }
        for (String raw : config.getOrDefault("helditemsoffhand", List.of())) {
            for (String item : raw.split(",")) {
                items.resolve(culture, item.trim()).ifPresent(mapped -> {
                    if (!allHeld.contains(mapped)) {
                        allHeld.add(mapped);
                    }
                });
            }
        }
        if (!allHeld.isEmpty()) {
            json.put("heldItems", allHeld);
        }
    }

    static void addLeisureFlag(Map<String, List<String>> config, Map<String, Object> json) {
        String leasure = LegacyJsonBuilder.getStringValue(config, "leasure");
        if ("true".equalsIgnoreCase(leasure)) {
            json.put("leisure", true);
        }
    }

    public static Map<String, Object> buildGatheringTypeJson(Map<String, List<String>> config, String culture, ItemIdMapper items) {
        String goalTag;
        String sound;
        String labelKey;
        String requiredTag;
        String[] balanceParts;
        List<String> balanceLines;
        int maxTotal;
        LinkedHashMap<String, Object> entry;
        String modernId;
        int count;
        String legacyItem;
        String[] parts;
        ArrayList inputs = new ArrayList();
        ArrayList outputs = new ArrayList();
        boolean hasUnmapped = false;
        for (String inputLine : config.getOrDefault("input", List.of())) {
            parts = inputLine.split(",", 2);
            legacyItem = parts[0].trim();
            count = parts.length > 1 ? Integer.parseInt(parts[1].trim()) : 1;
            modernId = items.resolve(culture, legacyItem).orElse(null);
            if ("wood".equals(legacyItem)) {
                modernId = "#minecraft:logs";
            }
            if (modernId == null) {
                System.out.println("    [WARN] Input item not mapped: " + legacyItem);
                hasUnmapped = true;
                continue;
            }
            entry = new LinkedHashMap<String, Object>();
            entry.put("item", modernId);
            entry.put("count", count);
            inputs.add(entry);
        }
        for (String outputLine : config.getOrDefault("output", List.of())) {
            parts = outputLine.split(",", 2);
            legacyItem = parts[0].trim();
            count = parts.length > 1 ? Integer.parseInt(parts[1].trim()) : 1;
            modernId = items.resolve(culture, legacyItem).orElse(null);
            if (modernId == null) {
                System.out.println("    [WARN] Output item not mapped: " + legacyItem);
                hasUnmapped = true;
                continue;
            }
            entry = new LinkedHashMap();
            entry.put("item", modernId);
            entry.put("count", count);
            outputs.add(entry);
        }
        if (outputs.isEmpty()) {
            return null;
        }
        LinkedHashMap<String, Object> json = new LinkedHashMap<String, Object>();
        json.put("handler", "crafting");
        int priority = LegacyJsonBuilder.getIntValue(config, "priority", 50);
        json.put("priority", priority);
        if (config.containsKey("priorityrandom")) {
            json.put("priorityRandom", LegacyJsonBuilder.getIntValue(config, "priorityrandom", 10));
        }
        json.put("scanRadius", 32);
        json.put("batchRadius", 8);
        json.put("maxActionsPerTask", 5);
        int durationMs = LegacyJsonBuilder.getIntValue(config, "duration", 5000);
        json.put("actionCooldown", durationMs / 50);
        json.put("stuckTimeout", 4000);
        json.put("walkSpeed", 0.6);
        LegacyJsonBuilder.parseLimitField(config, "townhalllimit", culture, items).ifPresent(lim -> json.put("townhallLimit", lim));
        LegacyJsonBuilder.parseLimitField(config, "buildinglimit", culture, items).ifPresent(lim -> json.put("buildingLimit", lim));
        LegacyJsonBuilder.parseLimitField(config, "villagelimit", culture, items).ifPresent(lim -> json.put("villageLimit", lim));
        int maxInBuilding = LegacyJsonBuilder.getIntValue(config, "maxsimultaneousinbuilding", -1);
        if (maxInBuilding > 0) {
            json.put("maxSimultaneousInBuilding", maxInBuilding);
        }
        if ((maxTotal = LegacyJsonBuilder.getIntValue(config, "maxsimultaneoustotal", -1)) > 0) {
            json.put("maxSimultaneousTotal", maxTotal);
        }
        if ((balanceLines = config.get("itemsbalance")) != null && !balanceLines.isEmpty() && (balanceParts = balanceLines.get(balanceLines.size() - 1).split(",", 2)).length == 2) {
            String balanceInput = items.resolve(culture, balanceParts[0].trim()).orElse(null);
            String balanceOutput = items.resolve(culture, balanceParts[1].trim()).orElse(null);
            if (balanceInput != null && balanceOutput != null) {
                json.put("itemsBalance", Map.of(balanceInput, balanceOutput));
            }
        }
        json.put("minimumHour", -1);
        json.put("maximumHour", 12500);
        int reoccur = LegacyJsonBuilder.getIntValue(config, "reoccurdelay", -1);
        if (reoccur > 0) {
            json.put("reoccurDelay", reoccur / 50);
        }
        LinkedHashMap<String, Object> handlerParams = new LinkedHashMap<String, Object>();
        String buildingTag = LegacyJsonBuilder.getStringValue(config, "buildingtag");
        if (buildingTag != null) {
            handlerParams.put("buildingTag", buildingTag);
        }
        if ((requiredTag = LegacyJsonBuilder.getStringValue(config, "requiredtag")) != null) {
            handlerParams.put("requiredTag", requiredTag);
        }
        if (!inputs.isEmpty()) {
            handlerParams.put("inputs", inputs);
        }
        handlerParams.put("outputs", outputs);
        json.put("handlerParams", handlerParams);
        LegacyJsonBuilder.addHeldItems(config, json, culture, items);
        String sentenceKey = LegacyJsonBuilder.getStringValue(config, "sentencekey");
        if (sentenceKey != null) {
            json.put("sentenceKey", sentenceKey);
        }
        if ((labelKey = LegacyJsonBuilder.getStringValue(config, "labelkey")) != null) {
            json.put("labelKey", labelKey);
        }
        if ((sound = LegacyJsonBuilder.getStringValue(config, "sound")) != null) {
            json.put("sound", sound);
        }
        if ((goalTag = LegacyJsonBuilder.getStringValue(config, "tag")) != null) {
            json.put("tag", goalTag);
        }
        return json;
    }

    static int getIntValue(Map<String, List<String>> config, String key, int defaultValue) {
        List<String> values = config.get(key);
        if (values == null || values.isEmpty()) {
            return defaultValue;
        }
        try {
            return Integer.parseInt(values.getFirst().trim());
        }
        catch (NumberFormatException e) {
            return defaultValue;
        }
    }

    static String getStringValue(Map<String, List<String>> config, String key) {
        List<String> values = config.get(key);
        if (values == null || values.isEmpty()) {
            return null;
        }
        return values.getFirst().trim();
    }

    static Optional<Map<String, Integer>> parseLimitField(Map<String, List<String>> config, String key, String culture, ItemIdMapper items) {
        List<String> values = config.get(key);
        if (values == null || values.isEmpty()) {
            return Optional.empty();
        }
        LinkedHashMap limits = new LinkedHashMap();
        for (String value : values) {
            String[] parts = value.split(",", 2);
            String legacyItem = parts[0].trim();
            int count = parts.length > 1 ? Integer.parseInt(parts[1].trim()) : 64;
            items.resolve(culture, legacyItem).ifPresent(id -> limits.put(id, count));
        }
        return limits.isEmpty() ? Optional.empty() : Optional.of(limits);
    }

    static {
        HashMap<String, Object> m = new HashMap<String, Object>();
        m.put("cobblestone", "cobblestone");
        m.put("stone", "stone");
        m.put("glass", "glass");
        m.put("sand", "sand");
        m.put("iron", "iron_ingot");
        m.put("gold", "gold_ingot");
        m.put("cauldron", "cauldron");
        m.put("bookshelves", "bookshelf");
        m.put("banner_white", "banner_white");
        m.put("wool_white", "wool_white");
        m.put("wool_gray", "wool_gray");
        m.put("wool_lightgray", "wool_light_gray");
        m.put("wool_black", "wool_black");
        m.put("wool_brown", "wool_brown");
        m.put("wool_blue", "wool_blue");
        m.put("wool_red", "wool_red");
        m.put("wool_pink", "wool_pink");
        m.put("seeds", "seeds");
        m.put("wheat", "wheat");
        m.put("carrot", "carrot");
        m.put("bread", "bread");
        m.put("egg", "egg");
        m.put("sugar", "sugar");
        m.put("cake", "cake");
        m.put("ciderapple", "cider_apple");
        m.put("leather", "leather");
        m.put("feather", "feather");
        m.put("bone", "bone");
        m.put("beefraw", "beef_raw");
        m.put("beefcooked", "beef_cooked");
        m.put("porkchops", "porkchop_raw");
        m.put("porkchopscooked", "porkchop_cooked");
        m.put("chickenmeat", "chicken_raw");
        m.put("chickenmeatcooked", "chicken_cooked");
        m.put("dye_white", "dye_white");
        m.put("dye_red", "dye_red");
        m.put("dye_yellow", "dye_yellow");
        m.put("dye_lightblue", "dye_light_blue");
        m.put("carpet_white", "carpet_white");
        m.put("carpet_red", "carpet_red");
        m.put("carpet_yellow", "carpet_yellow");
        m.put("carpet_blue", "carpet_blue");
        m.put("bow", "bow");
        m.put("arrow", "arrow");
        m.put("bottle", "bottle");
        m.put("cider", "cider");
        m.put("calva", "calva");
        m.put("tripes", "tripes");
        m.put("boudin", "boudin");
        m.put("timberframeplain", "timber_frame_plain");
        m.put("timberframecross", "timber_frame_cross");
        m.put("normanSword", "norman_sword");
        m.put("normanBroadsword", "norman_sword");
        m.put("normanPickaxe", "norman_pickaxe");
        m.put("normanAxe", "norman_axe");
        m.put("normanShovel", "norman_shovel");
        m.put("normanHoe", "norman_hoe");
        m.put("normanHelmet", "norman_helmet");
        m.put("normanplate", "norman_chestplate");
        m.put("normanLegs", "norman_leggings");
        m.put("normanBoots", "norman_boots");
        m.put("parchment_normanvillagers", "parchment_normanvillagers");
        m.put("parchment_normanbuildings", "parchment_normanbuildings");
        m.put("parchment_normanitems", "parchment_normanitems");
        m.put("parchment_normanfull", "parchment_normanfull");
        m.put("turmeric", "turmeric");
        m.put("rasgulla", "rasgulla");
        m.put("chickencurry", "chickencurry");
        m.put("vegcurry", "vegcurry");
        m.put("rice", "rice");
        m.put("cotton", "cotton");
        m.put("brickmould", "brick_mould");
        m.put("paintbucketwhite", "paint_bucket_white");
        m.put("decoratedbrickwhite", "decorated_brick_white");
        m.put("paintedbrickwhite", "painted_brick_white");
        m.put("indianstatue", "indian_statue");
        m.put("woodenbarsindian", "wooden_bars_indian");
        m.put("bed_charpoy", "charpoy");
        m.put("sandstone_carved", "sandstone_carved");
        m.put("red_sandstone_carved", "red_sandstone_carved");
        m.put("ochre_sandstone_carved", "ochre_sandstone_carved");
        m.put("sandstone", "sandstone");
        m.put("redsandstone", "red_sandstone");
        m.put("diamond", "diamond");
        m.put("dye_blue", "dye_blue");
        m.put("cactus", "cactus");
        m.put("fishcooked", "cooked_cod");
        m.put("cooked_mutton", "cooked_mutton");
        m.put("cookie", "cookie");
        m.put("mudbrick", "mud_brick");
        m.put("thatch", "thatch");
        m.put("woodenbars", "wooden_bars");
        m.put("woodenbarsrosette", "wooden_bars_rosette");
        m.put("summoningwand", "summoning_wand");
        m.put("sugarcane", "sugar_cane");
        m.put("maize", "maize");
        m.put("masa", "masa");
        m.put("wah", "wah");
        m.put("cacauhaa", "cacauhaa");
        m.put("obsidianflake", "obsidian_flake");
        m.put("mayanmace", "mayan_mace");
        m.put("mayanpickaxe", "mayan_pickaxe");
        m.put("mayanaxe", "mayan_axe");
        m.put("mayanshovel", "mayan_shovel");
        m.put("mayanhoe", "mayan_hoe");
        m.put("mayanstatue", "mayan_statue");
        m.put("mayangold", "mayan_gold_block");
        m.put("maypattern", "mayan_pattern");
        m.put("maypattern1", "mayan_pattern_1");
        m.put("maypattern2", "mayan_pattern_2");
        m.put("maypattern3", "mayan_pattern_3");
        m.put("maypattern4", "mayan_pattern_4");
        m.put("obsidian", "obsidian");
        m.put("dye_brown", "dye_brown");
        m.put("spidereye", "spider_eye");
        m.put("rottenflesh", "rotten_flesh");
        m.put("grapes", "grapes");
        m.put("winefancy", "winefancy");
        m.put("winebasic", "winebasic");
        m.put("olives", "olives");
        m.put("oliveoil", "oliveoil");
        m.put("feta", "feta");
        m.put("souvlaki", "souvlaki");
        m.put("byzantinemace", "byzantine_mace");
        m.put("byzantinehelmet", "byzantine_helmet");
        m.put("byzantineplate", "byzantine_chestplate");
        m.put("byzantinelegs", "byzantine_leggings");
        m.put("byzantineboots", "byzantine_boots");
        m.put("byzantineaxe", "byzantine_axe");
        m.put("byzantinepickaxe", "byzantine_pickaxe");
        m.put("byzantineshovel", "byzantine_shovel");
        m.put("byzantinehoe", "byzantine_hoe");
        m.put("silk", "silk");
        m.put("clothes_byz_wool", "clothes_byz_wool");
        m.put("clothes_byz_silk", "clothes_byz_silk");
        m.put("byzantine_fresco", "byzantine_fresco");
        m.put("byzpattern", "byzantine_pattern");
        m.put("byzpattern1", "byzantine_pattern_1");
        m.put("byzpattern2", "byzantine_pattern_2");
        m.put("byzantine_tiles", "byzantine_tiles");
        m.put("byzantine_mosaic", "byzantine_mosaic_red");
        m.put("byzantine_mosaic_red", "byzantine_mosaic_red");
        m.put("byzantine_mosaic_blue", "byzantine_mosaic_blue");
        m.put("byzantineiconsmall", "wall_byzantine_icon_small");
        m.put("byzantineiconmedium", "wall_byzantine_icon_medium");
        m.put("byzantineiconlarge", "wall_byzantine_icon_large");
        m.put("sapling_olivetree", "olive_tree_sapling");
        m.put("diorite", "diorite");
        m.put("smooth_diorite", "polished_diorite");
        m.put("ironnugget", "iron_nugget");
        m.put("dye_purple", "dye_purple");
        m.put("mutton", "mutton_raw");
        m.put("muttonraw", "mutton_raw");
        m.put("muttoncooked", "cooked_mutton");
        m.put("quartz", "quartz");
        m.put("sake", "sake");
        m.put("udon", "udon");
        m.put("ikayaki", "ikayaki");
        m.put("japanese_tachi", "japanese_tachi");
        m.put("tachisword", "japanese_tachi");
        m.put("yumibow", "yumibow");
        m.put("paper_wall", "paper_wall");
        m.put("japaneseguardhelmet", "japaneseguardhelmet");
        m.put("japaneseguardplate", "japaneseguardplate");
        m.put("japaneseguardlegs", "japaneseguardlegs");
        m.put("japaneseguardboots", "japaneseguardboots");
        m.put("japanesebluehelmet", "japanesebluehelmet");
        m.put("japaneseblueplate", "japaneseblueplate");
        m.put("japanesebluelegs", "japanesebluelegs");
        m.put("japaneseblueboots", "japaneseblueboots");
        m.put("japaneseredhelmet", "japaneseredhelmet");
        m.put("japaneseredplate", "japaneseredplate");
        m.put("japaneseredlegs", "japaneseredlegs");
        m.put("japaneseredboots", "japaneseredboots");
        m.put("bed_futon", "futon");
        m.put("mudbrick_seljuk_ornamented", "mudbrick_seljuk_ornamented");
        m.put("mudbrick_seljuk_decorated", "mudbrick_seljuk_decorated");
        m.put("yogurt", "yogurt");
        m.put("ayran", "ayran");
        m.put("pistachios", "pistachios");
        m.put("book", "book");
        m.put("pide", "pide");
        m.put("mudbrick_smooth", "mudbrick_smooth");
        m.put("wallcarpetlarge", "wall_carpet_large");
        m.put("wallcarpetmedium", "wall_carpet_medium");
        m.put("wallcarpetsmall", "wall_carpet_small");
        m.put("clothes_seljuk_cotton", "clothes_seljuk_cotton");
        m.put("clothes_seljuk_wool", "clothes_seljuk_wool");
        m.put("stoneaxe", "stone_axe");
        m.put("stonehoe", "stone_hoe");
        m.put("stonepickaxe", "stone_pickaxe");
        m.put("stoneshovel", "stone_shovel");
        LEGACY_TO_GOOD_ID = Collections.unmodifiableMap(m);
        m = new LinkedHashMap();
        m.put("wood", "minecraft:oak_log");
        m.put("wood_oak", "minecraft:oak_log");
        m.put("wood_pine", "minecraft:spruce_log");
        m.put("wood_birch", "minecraft:birch_log");
        m.put("wood_jungle", "minecraft:jungle_log");
        m.put("wood_acacia", "minecraft:acacia_log");
        m.put("wood_darkoak", "minecraft:dark_oak_log");
        m.put("wood_any", "#minecraft:logs");
        m.put("planks_pine", "minecraft:spruce_planks");
        m.put("cobblestone", "minecraft:cobblestone");
        m.put("stone", "minecraft:stone");
        m.put("iron", "minecraft:iron_ingot");
        m.put("glass", "minecraft:glass");
        m.put("clay", "minecraft:clay_ball");
        m.put("flint", "minecraft:flint");
        m.put("bone", "minecraft:bone");
        m.put("leather", "minecraft:leather");
        m.put("feather", "minecraft:feather");
        m.put("wheat", "minecraft:wheat");
        m.put("sugarcane", "minecraft:sugar_cane");
        m.put("sugar", "minecraft:sugar");
        m.put("beefraw", "minecraft:beef");
        m.put("porkchops", "minecraft:porkchop");
        m.put("ciderapple", "millenaire:cider_apple");
        m.put("wool_white", "minecraft:white_wool");
        m.put("wool_gray", "minecraft:gray_wool");
        m.put("wool_lightgray", "minecraft:light_gray_wool");
        m.put("wool_black", "minecraft:black_wool");
        m.put("wool_brown", "minecraft:brown_wool");
        m.put("wool_red", "minecraft:red_wool");
        m.put("wool_blue", "minecraft:blue_wool");
        m.put("wool_yellow", "minecraft:yellow_wool");
        m.put("dye_white", "minecraft:white_dye");
        m.put("dye_red", "minecraft:red_dye");
        m.put("dye_yellow", "minecraft:yellow_dye");
        m.put("dye_lightblue", "minecraft:light_blue_dye");
        m.put("paper", "minecraft:paper");
        m.put("book", "minecraft:book");
        m.put("bookandquill", "minecraft:writable_book");
        m.put("bread", "minecraft:bread");
        m.put("cake", "minecraft:cake");
        m.put("bricks", "minecraft:bricks");
        m.put("bookshelves", "minecraft:bookshelf");
        m.put("arrow", "minecraft:arrow");
        m.put("bow", "minecraft:bow");
        m.put("painting", "minecraft:painting");
        m.put("bottle", "minecraft:glass_bottle");
        m.put("cauldron", "minecraft:cauldron");
        m.put("woodsword", "minecraft:wooden_sword");
        m.put("woodaxe", "minecraft:wooden_axe");
        m.put("woodpickaxe", "minecraft:wooden_pickaxe");
        m.put("woodshovel", "minecraft:wooden_shovel");
        m.put("woodhoe", "minecraft:wooden_hoe");
        m.put("stoneaxe", "minecraft:stone_axe");
        m.put("stonepickaxe", "minecraft:stone_pickaxe");
        m.put("stoneshovel", "minecraft:stone_shovel");
        m.put("stonehoe", "minecraft:stone_hoe");
        m.put("stonesword", "minecraft:stone_sword");
        m.put("steelaxe", "minecraft:iron_axe");
        m.put("steelpickaxe", "minecraft:iron_pickaxe");
        m.put("steelshovel", "minecraft:iron_shovel");
        m.put("steelhoe", "minecraft:iron_hoe");
        m.put("steelsword", "minecraft:iron_sword");
        m.put("leatherhelmet", "minecraft:leather_helmet");
        m.put("leatherchest", "minecraft:leather_chestplate");
        m.put("leatherlegs", "minecraft:leather_leggings");
        m.put("leatherboots", "minecraft:leather_boots");
        m.put("steelhelmet", "minecraft:iron_helmet");
        m.put("steelchest", "minecraft:iron_chestplate");
        m.put("steellegs", "minecraft:iron_leggings");
        m.put("steelboots", "minecraft:iron_boots");
        m.put("glass_pane_white", "minecraft:white_stained_glass_pane");
        m.put("glass_pane_black", "minecraft:black_stained_glass_pane");
        m.put("glass_pane_blue", "minecraft:blue_stained_glass_pane");
        m.put("glass_pane_brown", "minecraft:brown_stained_glass_pane");
        m.put("glass_pane_cyan", "minecraft:cyan_stained_glass_pane");
        m.put("glass_pane_gray", "minecraft:gray_stained_glass_pane");
        m.put("glass_pane_green", "minecraft:green_stained_glass_pane");
        m.put("glass_pane_light_blue", "minecraft:light_blue_stained_glass_pane");
        m.put("glass_pane_light_gray", "minecraft:light_gray_stained_glass_pane");
        m.put("glass_pane_lime", "minecraft:lime_stained_glass_pane");
        m.put("glass_pane_magenta", "minecraft:magenta_stained_glass_pane");
        m.put("glass_pane_orange", "minecraft:orange_stained_glass_pane");
        m.put("glass_pane_pink", "minecraft:pink_stained_glass_pane");
        m.put("glass_pane_purple", "minecraft:purple_stained_glass_pane");
        m.put("glass_pane_red", "minecraft:red_stained_glass_pane");
        m.put("glass_pane_yellow", "minecraft:yellow_stained_glass_pane");
        m.put("carpet_white", "minecraft:white_carpet");
        m.put("carpet_red", "minecraft:red_carpet");
        m.put("carpet_blue", "minecraft:blue_carpet");
        m.put("carpet_yellow", "minecraft:yellow_carpet");
        m.put("timberframeplain", "millenaire:timber_frame_plain");
        m.put("timberframecross", "millenaire:timber_frame_cross");
        m.put("normanhelmet", "millenaire:norman_helmet");
        m.put("normanplate", "millenaire:norman_chestplate");
        m.put("normanlegs", "millenaire:norman_leggings");
        m.put("normanboots", "millenaire:norman_boots");
        m.put("normansword", "millenaire:norman_sword");
        m.put("normanbroadsword", "millenaire:norman_sword");
        m.put("normanaxe", "millenaire:norman_axe");
        m.put("normanpickaxe", "millenaire:norman_pickaxe");
        m.put("normanshovel", "millenaire:norman_shovel");
        m.put("normanhoe", "millenaire:norman_hoe");
        m.put("cider", "millenaire:cider");
        m.put("calva", "millenaire:calva");
        m.put("boudin", "millenaire:boudin");
        m.put("tripes", "millenaire:tripes");
        m.put("ciderapple", "millenaire:cider_apple");
        m.put("tapestry", "millenaire:wall_tapestry");
        m.put("bed_straw", "millenaire:straw_bed");
        m.put("banner_white", "minecraft:white_banner");
        m.put("ulu", "millenaire:ulu");
        m.put("rosette", "millenaire:rosette");
        m.put("pathdirt", "millenaire:path_dirt");
        m.put("pathgravel", "millenaire:path_gravel");
        m.put("pathgravelslabs", "millenaire:path_gravel_slab");
        m.put("pathochretiles", "millenaire:path_ochre_tiles");
        m.put("pathsandstone", "millenaire:path_sandstone");
        m.put("pathslabs", "millenaire:path_slabs");
        m.put("stained_glass_white", "millenaire:stained_glass_white");
        m.put("stained_glass_green_blue", "millenaire:stained_glass_green_blue");
        m.put("stained_glass_red_blue", "millenaire:stained_glass_red_blue");
        m.put("stained_glass_yellow", "millenaire:stained_glass_yellow");
        m.put("stained_glass_yellow_red", "millenaire:stained_glass_yellow_red");
        m.put("dirtwall", "millenaire:dirt_wall");
        m.put("denier", "millenaire:denier");
        m.put("denierargent", "millenaire:denier_argent");
        m.put("denierdor", "millenaire:denier_or");
        m.put("coal", "minecraft:coal");
        m.put("silk", "millenaire:silk");
        m.put("cotton", "millenaire:cotton");
        m.put("rice", "millenaire:rice");
        m.put("seeds", "minecraft:wheat_seeds");
        m.put("carrot", "minecraft:carrot");
        m.put("sand", "minecraft:sand");
        m.put("gravel", "minecraft:gravel");
        m.put("egg", "minecraft:egg");
        m.put("chickenmeat", "minecraft:chicken");
        m.put("chickenmeatcooked", "minecraft:cooked_chicken");
        m.put("beefcooked", "minecraft:cooked_beef");
        m.put("porkchopscooked", "minecraft:cooked_porkchop");
        m.put("wool_pink", "minecraft:pink_wool");
        m.put("sapling", "minecraft:oak_sapling");
        m.put("sapling_pine", "minecraft:spruce_sapling");
        m.put("sapling_birch", "minecraft:birch_sapling");
        m.put("sapling_jungle", "minecraft:jungle_sapling");
        m.put("sapling_acacia", "minecraft:acacia_sapling");
        m.put("sapling_darkoak", "minecraft:dark_oak_sapling");
        m.put("sapling_appletree", "millenaire:apple_tree_sapling");
        m.put("apple", "minecraft:apple");
        m.put("muttoncooked", "minecraft:cooked_mutton");
        m.put("muttonraw", "minecraft:mutton");
        m.put("purse", "millenaire:purse");
        m.put("blueflower", "minecraft:cornflower");
        m.put("pinkflower", "minecraft:pink_tulip");
        m.put("redflower", "minecraft:poppy");
        m.put("netherwart", "minecraft:nether_wart");
        m.put("dirt", "minecraft:dirt");
        m.put("turmeric", "millenaire:turmeric");
        m.put("rasgulla", "millenaire:rasgulla");
        m.put("chickencurry", "millenaire:chickencurry");
        m.put("vegcurry", "millenaire:vegcurry");
        m.put("rice", "millenaire:rice");
        m.put("cotton", "millenaire:cotton");
        m.put("brickmould", "millenaire:brick_mould");
        m.put("paintbucketwhite", "millenaire:paint_bucket_white");
        m.put("decoratedbrickwhite", "millenaire:decorated_brick_white");
        m.put("paintedbrickwhite", "millenaire:painted_brick_white");
        m.put("indianstatue", "millenaire:wall_indian_statue");
        m.put("woodenbarsindian", "millenaire:wooden_bars_indian");
        m.put("bed_charpoy", "millenaire:charpoy");
        m.put("sandstone_carved", "millenaire:sandstone_carved");
        m.put("red_sandstone_carved", "millenaire:red_sandstone_carved");
        m.put("ochre_sandstone_carved", "millenaire:ochre_sandstone_carved");
        m.put("gold", "minecraft:gold_ingot");
        m.put("sandstone", "minecraft:sandstone");
        m.put("redsandstone", "minecraft:red_sandstone");
        m.put("diamond", "minecraft:diamond");
        m.put("dye_blue", "minecraft:lapis_lazuli");
        m.put("cactus", "minecraft:cactus");
        m.put("fishcooked", "minecraft:cooked_cod");
        m.put("cooked_mutton", "minecraft:cooked_mutton");
        m.put("cookie", "minecraft:cookie");
        m.put("stonepickaxe", "minecraft:stone_pickaxe");
        m.put("stoneaxe", "minecraft:stone_axe");
        m.put("stoneshovel", "minecraft:stone_shovel");
        m.put("stonehoe", "minecraft:stone_hoe");
        m.put("diamondpickaxe", "minecraft:diamond_pickaxe");
        m.put("diamondaxe", "minecraft:diamond_axe");
        m.put("diamondshovel", "minecraft:diamond_shovel");
        m.put("diamondhoe", "minecraft:diamond_hoe");
        m.put("diamondsword", "minecraft:diamond_sword");
        m.put("diamondhelmet", "minecraft:diamond_helmet");
        m.put("diamondchest", "minecraft:diamond_chestplate");
        m.put("diamondlegs", "minecraft:diamond_leggings");
        m.put("diamondboots", "minecraft:diamond_boots");
        m.put("mudbrick", "millenaire:mud_brick");
        m.put("thatch", "millenaire:thatch");
        m.put("woodenbars", "millenaire:wooden_bars");
        m.put("woodenbarsrosette", "millenaire:wooden_bars_rosette");
        m.put("summoningwand", "millenaire:summoning_wand");
        m.put("maize", "millenaire:maize");
        m.put("masa", "millenaire:masa");
        m.put("wah", "millenaire:wah");
        m.put("cacauhaa", "millenaire:cacauhaa");
        m.put("obsidianflake", "millenaire:obsidian_flake");
        m.put("mayanmace", "millenaire:mayan_mace");
        m.put("mayanpickaxe", "millenaire:mayan_pickaxe");
        m.put("mayanaxe", "millenaire:mayan_axe");
        m.put("mayanshovel", "millenaire:mayan_shovel");
        m.put("mayanhoe", "millenaire:mayan_hoe");
        m.put("mayanstatue", "millenaire:mayan_statue");
        m.put("mayangold", "millenaire:mayan_gold_block");
        m.put("maypattern", "millenaire:mayan_pattern");
        m.put("maypattern1", "millenaire:mayan_pattern_1");
        m.put("maypattern2", "millenaire:mayan_pattern_2");
        m.put("maypattern3", "millenaire:mayan_pattern_3");
        m.put("maypattern4", "millenaire:mayan_pattern_4");
        m.put("obsidian", "minecraft:obsidian");
        m.put("dye_brown", "minecraft:cocoa_beans");
        m.put("spidereye", "minecraft:spider_eye");
        m.put("rottenflesh", "minecraft:rotten_flesh");
        m.put("pumpkin", "minecraft:pumpkin");
        m.put("ghasttear", "minecraft:ghast_tear");
        m.put("goldsword", "minecraft:golden_sword");
        m.put("sake", "millenaire:sake");
        m.put("udon", "millenaire:udon");
        m.put("ikayaki", "millenaire:ikayaki");
        m.put("japanese_tachi", "millenaire:japanese_tachi");
        m.put("tachisword", "millenaire:japanese_tachi");
        m.put("yumibow", "millenaire:yumibow");
        m.put("paper_wall", "millenaire:paper_wall");
        m.put("jappattern", "millenaire:jappattern");
        m.put("jappattern1", "millenaire:jappattern1");
        m.put("jappattern2", "millenaire:jappattern2");
        m.put("jappattern3", "millenaire:jappattern3");
        m.put("jappattern4", "millenaire:jappattern4");
        m.put("japaneseguardhelmet", "millenaire:japaneseguardhelmet");
        m.put("japaneseguardplate", "millenaire:japaneseguardplate");
        m.put("japaneseguardlegs", "millenaire:japaneseguardlegs");
        m.put("japaneseguardboots", "millenaire:japaneseguardboots");
        m.put("japanesebluehelmet", "millenaire:japanesebluehelmet");
        m.put("japaneseblueplate", "millenaire:japaneseblueplate");
        m.put("japanesebluelegs", "millenaire:japanesebluelegs");
        m.put("japaneseblueboots", "millenaire:japaneseblueboots");
        m.put("japaneseredhelmet", "millenaire:japaneseredhelmet");
        m.put("japaneseredplate", "millenaire:japaneseredplate");
        m.put("japaneseredlegs", "millenaire:japaneseredlegs");
        m.put("japaneseredboots", "millenaire:japaneseredboots");
        m.put("bed_futon", "millenaire:futon");
        m.put("japanese_tiles", "millenaire:japanese_tiles");
        m.put("japanese_tiles_slab", "millenaire:japanese_tiles_slab");
        m.put("denieror", "millenaire:denier_or");
        m.put("grapes", "millenaire:grapes");
        m.put("winefancy", "millenaire:winefancy");
        m.put("winebasic", "millenaire:winebasic");
        m.put("olives", "millenaire:olives");
        m.put("oliveoil", "millenaire:oliveoil");
        m.put("feta", "millenaire:feta");
        m.put("souvlaki", "millenaire:souvlaki");
        m.put("byzantinemace", "millenaire:byzantine_mace");
        m.put("byzantinehelmet", "millenaire:byzantine_helmet");
        m.put("byzantineplate", "millenaire:byzantine_chestplate");
        m.put("byzantinelegs", "millenaire:byzantine_leggings");
        m.put("byzantineboots", "millenaire:byzantine_boots");
        m.put("byzantineaxe", "millenaire:byzantine_axe");
        m.put("byzantinepickaxe", "millenaire:byzantine_pickaxe");
        m.put("byzantineshovel", "millenaire:byzantine_shovel");
        m.put("byzantinehoe", "millenaire:byzantine_hoe");
        m.put("clothes_byz_wool", "millenaire:clothes_byz_wool");
        m.put("clothes_byz_silk", "millenaire:clothes_byz_silk");
        m.put("byzantine_fresco", "millenaire:byzantine_fresco");
        m.put("byzpattern", "millenaire:byzantine_pattern");
        m.put("byzpattern1", "millenaire:byzantine_pattern_1");
        m.put("byzpattern2", "millenaire:byzantine_pattern_2");
        m.put("byzantine_tiles", "millenaire:byzantine_tiles");
        m.put("byzantine_tiles_slab", "millenaire:byzantine_tiles_slab");
        m.put("byzantine_stone_tiles", "millenaire:byzantine_stone_tiles");
        m.put("byzantine_sandstone_tiles", "millenaire:byzantine_sandstone_tiles");
        m.put("byzantine_stone_ornament", "millenaire:byzantine_stone_ornament");
        m.put("byzantine_sandstone_ornament", "millenaire:byzantine_sandstone_ornament");
        m.put("byzantine_mosaic", "millenaire:byzantine_mosaic_red");
        m.put("byzantine_mosaic_red", "millenaire:byzantine_mosaic_red");
        m.put("byzantine_mosaic_blue", "millenaire:byzantine_mosaic_blue");
        m.put("byzantineiconsmall", "millenaire:wall_byzantine_icon_small");
        m.put("byzantineiconmedium", "millenaire:wall_byzantine_icon_medium");
        m.put("byzantineiconlarge", "millenaire:wall_byzantine_icon_large");
        m.put("sapling_olivetree", "millenaire:olive_tree_sapling");
        m.put("leaves_olivetree", "millenaire:olive_tree_leaves");
        m.put("diorite", "minecraft:diorite");
        m.put("smooth_diorite", "minecraft:polished_diorite");
        m.put("ironnugget", "minecraft:iron_nugget");
        m.put("dye_purple", "minecraft:purple_dye");
        m.put("dye_yellow", "minecraft:yellow_dye");
        m.put("mutton", "minecraft:mutton");
        m.put("muttonraw", "minecraft:mutton");
        m.put("muttoncooked", "minecraft:cooked_mutton");
        m.put("cooked_mutton", "minecraft:cooked_mutton");
        m.put("quartz", "minecraft:quartz_block");
        m.put("brick", "minecraft:brick");
        m.put("fishraw", "minecraft:cod");
        m.put("charcoal", "minecraft:charcoal");
        m.put("byzantine_mosaic", "millenaire:byzantine_mosaic_red");
        m.put("byzantine_tiles", "millenaire:byzantine_tiles");
        m.put("winefancy", "millenaire:winefancy");
        m.put("winebasic", "millenaire:winebasic");
        m.put("wool_cyan", "minecraft:cyan_wool");
        m.put("wool_green", "minecraft:green_wool");
        m.put("wool_lightblue", "minecraft:light_blue_wool");
        m.put("wool_orange", "minecraft:orange_wool");
        m.put("wool_magenta", "minecraft:magenta_wool");
        m.put("wool_purple", "minecraft:purple_wool");
        m.put("wool_limegreen", "minecraft:lime_wool");
        m.put("dye_green", "minecraft:green_dye");
        m.put("dye_orange", "minecraft:orange_dye");
        m.put("dye_magenta", "minecraft:magenta_dye");
        m.put("dye_pink", "minecraft:pink_dye");
        m.put("dye_gray", "minecraft:gray_dye");
        m.put("dye_lightgray", "minecraft:light_gray_dye");
        m.put("dye_cyan", "minecraft:cyan_dye");
        m.put("dye_black", "minecraft:black_dye");
        m.put("carpet_cyan", "minecraft:cyan_carpet");
        m.put("carpet_green", "minecraft:green_carpet");
        m.put("carpet_lightblue", "minecraft:light_blue_carpet");
        m.put("hay", "minecraft:hay_block");
        m.put("goldnugget", "minecraft:gold_nugget");
        m.put("akwardpotion", "minecraft:potion");
        m.put("yellowFlower", "minecraft:dandelion");
        m.put("inuitmeatystew", "millenaire:inuitmeatystew");
        m.put("inuitpotatostew", "millenaire:inuitpotatostew");
        m.put("inuitbearstew", "millenaire:inuitbearstew");
        m.put("bearmeat_raw", "millenaire:bearmeat_raw");
        m.put("bearmeat_cooked", "millenaire:bearmeat_cooked");
        m.put("wolfmeat_raw", "millenaire:wolfmeat_raw");
        m.put("wolfmeat_cooked", "millenaire:wolfmeat_cooked");
        m.put("seafood_raw", "millenaire:seafood_raw");
        m.put("seafood_cooked", "millenaire:seafood_cooked");
        m.put("inuitbow", "millenaire:inuitbow");
        m.put("inuittrident", "millenaire:inuittrident");
        m.put("furhelmet", "millenaire:furhelmet");
        m.put("furplate", "millenaire:furplate");
        m.put("furlegs", "millenaire:furlegs");
        m.put("furboots", "millenaire:furboots");
        m.put("tannedhide", "millenaire:tannedhide");
        m.put("hidehanging", "millenaire:wall_hide_hanging");
        m.put("inuitcarving", "millenaire:inuit_carving");
        m.put("snowbrick", "millenaire:snow_brick");
        m.put("icebrick", "millenaire:ice_brick");
        m.put("snowwall", "millenaire:snow_wall");
        m.put("sod_spruce", "millenaire:sod_spruce");
        m.put("sod_birch", "millenaire:sod_birch");
        m.put("fire_pit", "millenaire:fire_pit");
        m.put("rabbit", "minecraft:rabbit");
        m.put("rabbit_hide", "minecraft:rabbit_hide");
        m.put("potato", "minecraft:potato");
        m.put("ice", "minecraft:ice");
        m.put("snowblock", "minecraft:snow_block");
        m.put("coarse_dirt", "minecraft:coarse_dirt");
        m.put("boneblock", "minecraft:bone_block");
        m.put("sod_oak", "millenaire:sod_oak");
        m.put("sod_jungle", "millenaire:sod_jungle");
        m.put("pathsnow", "millenaire:path_snow");
        m.put("snow", "minecraft:snow_block");
        m.put("baked_potato", "minecraft:baked_potato");
        m.put("yogurt", "millenaire:yogurt");
        m.put("ayran", "millenaire:ayran");
        m.put("pistachios", "millenaire:pistachios");
        m.put("helva", "millenaire:helva");
        m.put("lokum", "millenaire:lokum");
        m.put("mudbrick_seljuk_decorated", "millenaire:mud_brick_seljuk_decorated");
        m.put("mudbrick_seljuk_ornamented", "millenaire:mud_brick_seljuk_ornamented");
        m.put("ironore", "minecraft:raw_iron");
        m.put("string", "minecraft:string");
        m.put("seljukboots", "millenaire:seljuk_boots");
        m.put("seljukbow", "millenaire:seljuk_bow");
        m.put("seljuklegs", "millenaire:seljuk_leggings");
        m.put("seljukplate", "millenaire:seljuk_chestplate");
        m.put("seljukturban", "millenaire:seljuk_turban");
        m.put("seljukscimitar", "millenaire:seljuk_scimitar");
        m.put("seljukhelmet", "millenaire:seljuk_helmet");
        m.put("pide", "millenaire:pide");
        m.put("mudbrick_smooth", "millenaire:mud_brick_smooth");
        m.put("wallcarpetlarge", "millenaire:wall_carpet_large");
        m.put("wallcarpetmedium", "millenaire:wall_carpet_medium");
        m.put("wallcarpetsmall", "millenaire:wall_carpet_small");
        m.put("clothes_seljuk_cotton", "millenaire:clothes_seljuk_cotton");
        m.put("clothes_seljuk_wool", "millenaire:clothes_seljuk_wool");
        m.put("sapling_pistachio", "millenaire:pistachio_tree_sapling");
        m.put("pumpkinseeds", "minecraft:pumpkin_seeds");
        m.put("melonseeds", "minecraft:melon_seeds");
        m.put("sod_acacia", "millenaire:sod_acacia");
        m.put("sod_dark_oak", "millenaire:sod_dark_oak");
        m.put("fishing_rod", "minecraft:fishing_rod");
        m.put("enderpearl", "minecraft:ender_pearl");
        m.put("totem_of_undying", "minecraft:totem_of_undying");
        LEGACY_ITEM_ID_MAP = Collections.unmodifiableMap(m);
        m = new LinkedHashMap();
        m.put("wheat", "minecraft:wheat");
        m.put("carrot", "minecraft:carrots");
        m.put("carrots", "minecraft:carrots");
        m.put("potato", "minecraft:potatoes");
        m.put("potatoes", "minecraft:potatoes");
        m.put("millenaire:crop_rice", "millenaire:crop_rice");
        m.put("millenaire:crop_cotton", "millenaire:crop_cotton");
        m.put("millenaire:crop_turmeric", "millenaire:crop_turmeric");
        m.put("millenaire:crop_vine", "millenaire:crop_vine");
        m.put("millenaire:crop_maize", "millenaire:crop_maize");
        LEGACY_CROP_BLOCK_MAP = Collections.unmodifiableMap(m);
        LEGACY_FLOWER_MAP = Map.ofEntries(Map.entry("red_flower;type=poppy", "minecraft:poppy"), Map.entry("red_flower;type=blue_orchid", "minecraft:blue_orchid"), Map.entry("red_flower;type=red_tulip", "minecraft:red_tulip"), Map.entry("red_flower;type=white_tulip", "minecraft:white_tulip"), Map.entry("red_flower;type=pink_tulip", "minecraft:pink_tulip"), Map.entry("red_flower;type=orange_tulip", "minecraft:orange_tulip"), Map.entry("red_flower;type=allium", "minecraft:allium"), Map.entry("red_flower;type=houstonia", "minecraft:azure_bluet"), Map.entry("red_flower;type=oxeye_daisy", "minecraft:oxeye_daisy"), Map.entry("yellow_flower;type=dandelion", "minecraft:dandelion"), Map.entry("double_plant;type=rose", "minecraft:rose_bush"), Map.entry("double_plant;facing=north,half=lower,variant=double_rose", "minecraft:rose_bush"), Map.entry("double_plant;type=sunflower", "minecraft:sunflower"));
        CROP_SOIL_MAP = Map.of("minecraft:wheat", "minecraft:farmland", "minecraft:carrots", "minecraft:farmland", "minecraft:potatoes", "minecraft:farmland", "millenaire:crop_rice", "millenaire:rice_paddy", "millenaire:crop_cotton", "minecraft:farmland", "millenaire:crop_turmeric", "minecraft:farmland", "millenaire:crop_vine", "minecraft:farmland", "millenaire:crop_maize", "minecraft:farmland");
        CROP_SOIL_SUBTYPE_MAP = Map.of("minecraft:wheat", "wheat", "minecraft:carrots", "carrot", "minecraft:potatoes", "potato", "millenaire:crop_rice", "rice", "millenaire:crop_cotton", "cotton", "millenaire:crop_turmeric", "turmeric", "millenaire:crop_vine", "vine", "millenaire:crop_maize", "maize");
        CROP_SEED_MAP = Map.of("minecraft:wheat", "minecraft:wheat_seeds");
        m = new LinkedHashMap();
        m.put("minecraft:stone", "stone");
        m.put("minecraft:sand", "sand");
        m.put("minecraft:clay", "clay");
        m.put("minecraft:gravel", "gravel");
        m.put("minecraft:sandstone", "sandstone");
        m.put("minecraft:red_sandstone", "red_sandstone");
        m.put("minecraft:snow_layer", "snow");
        m.put("minecraft:ice", "ice");
        m.put("minecraft:diorite", "diorite");
        m.put("minecraft:granite", "granite");
        m.put("minecraft:andesite", "andesite");
        MINING_SOURCE_MAP = Collections.unmodifiableMap(m);
        LEGACY_ANIMAL_MAP = Map.of("cow", "minecraft:cow", "pig", "minecraft:pig", "sheep", "minecraft:sheep", "chicken", "minecraft:chicken", "squid", "minecraft:squid");
        m = new LinkedHashMap();
        m.put("beefraw", new String[]{"minecraft:beef", "minecraft:cooked_beef"});
        m.put("porkchops", new String[]{"minecraft:porkchop", "minecraft:cooked_porkchop"});
        m.put("chickenmeat", new String[]{"minecraft:chicken", "minecraft:cooked_chicken"});
        m.put("fishraw", new String[]{"minecraft:cod", "minecraft:cooked_cod"});
        m.put("muttonraw", new String[]{"minecraft:mutton", "minecraft:cooked_mutton"});
        m.put("mutton", new String[]{"minecraft:mutton", "minecraft:cooked_mutton"});
        m.put("ironore", new String[]{"minecraft:raw_iron", "minecraft:iron_ingot"});
        m.put("cobblestone", new String[]{"minecraft:cobblestone", "minecraft:stone"});
        m.put("stone", new String[]{"minecraft:cobblestone", "minecraft:stone"});
        m.put("sand", new String[]{"minecraft:sand", "minecraft:glass"});
        m.put("clay", new String[]{"minecraft:clay_ball", "minecraft:brick"});
        m.put("mudbrick", new String[]{"millenaire:mud_brick", "millenaire:painted_brick_white"});
        m.put("seafood_raw", new String[]{"millenaire:seafood_raw", "millenaire:seafood_cooked"});
        m.put("potato", new String[]{"minecraft:potato", "minecraft:baked_potato"});
        m.put("wood_any", new String[]{"minecraft:oak_log", "minecraft:charcoal"});
        m.put("clayblock", new String[]{"minecraft:clay", "minecraft:terracotta"});
        LEGACY_COOKING_MAP = Collections.unmodifiableMap(m);
    }
}

