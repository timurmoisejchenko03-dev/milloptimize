/*
 * Decompiled with CFR 0.150.
 * 
 * Could not load the following classes:
 *  javax.annotation.Nullable
 */
package org.millenaire.content.legacy;

import java.io.IOException;
import java.nio.charset.MalformedInputException;
import java.nio.charset.StandardCharsets;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import javax.annotation.Nullable;
import org.millenaire.content.legacy.LegacyIdCanonicaliser;

public final class LegacyDataParser {
    private static final Set<String> LENIENT_FALLBACK_LOGGED = ConcurrentHashMap.newKeySet();
    private static final int MAX_TRACKED_FALLBACKS = 4096;

    private LegacyDataParser() {
    }

    public static void resetLenientFallbackTracking() {
        LENIENT_FALLBACK_LOGGED.clear();
    }

    static List<String> readLinesLenient(Path path) throws IOException {
        try {
            return Files.readAllLines(path, StandardCharsets.UTF_8);
        }
        catch (MalformedInputException e) {
            String key = path.toAbsolutePath().toString();
            if (LENIENT_FALLBACK_LOGGED.size() < 4096 && LENIENT_FALLBACK_LOGGED.add(key)) {
                System.err.println("[INFO] File " + String.valueOf(path) + " is not UTF-8, read as ISO-8859-1");
            }
            return Files.readAllLines(path, StandardCharsets.ISO_8859_1);
        }
    }

    public static Map<String, List<String>> parseGatheringTypeTxt(Path txtFile) throws IOException {
        LinkedHashMap<String, List<String>> config = new LinkedHashMap<String, List<String>>();
        for (String line : LegacyDataParser.readLinesLenient(txtFile)) {
            int eq;
            if ((line = line.trim()).isEmpty() || line.startsWith("//") || (eq = line.indexOf(61)) < 0) continue;
            String key = line.substring(0, eq).toLowerCase().trim();
            String value = line.substring(eq + 1).trim();
            config.computeIfAbsent(key, k -> new ArrayList()).add(value);
        }
        return config;
    }

    public static BuildingMeta parseBuildingTxt(Path txtPath, String category) throws IOException {
        Map<String, List<String>> data = LegacyDataParser.parseKeyValues(txtPath);
        String fileName = txtPath.getFileName().toString();
        String baseName = LegacyDataParser.extractBaseName(fileName);
        int width = LegacyDataParser.getInt(data, "building.width", 10);
        int length = LegacyDataParser.getInt(data, "building.length", 10);
        int orientation = LegacyDataParser.getInt(data, "building.buildingorientation", 1);
        int maxCount = LegacyDataParser.getInt(data, "building.max", 1);
        double minDist = LegacyDataParser.getDouble(data, "building.mindistance", 0.0);
        double maxDist = LegacyDataParser.getDouble(data, "building.maxdistance", 1.0);
        int initialStartLevel = LegacyDataParser.getInt(data, "initial.startlevel", 0);
        int initialPriority = LegacyDataParser.getInt(data, "initial.priority", 100);
        String initialName = LegacyDataParser.getFirst(data, "initial.nativename", baseName);
        ArrayList<String> initialTags = new ArrayList<String>(LegacyDataParser.getAll(data, "initial.tag"));
        if ("true".equalsIgnoreCase(LegacyDataParser.getFirst(data, "initial.nopathstobuilding", "false")) && !initialTags.contains("nopaths")) {
            initialTags.add("nopaths");
        }
        List<String> males = LegacyDataParser.getAll(data, "initial.male");
        List<String> females = LegacyDataParser.getAll(data, "initial.female");
        List<String> visitors = LegacyDataParser.getAll(data, "initial.visitor");
        String shop = LegacyDataParser.getFirst(data, "initial.shop", null);
        List<String> startingSubBuildings = LegacyDataParser.getAll(data, "building.startingsubbuilding");
        String icon = LegacyDataParser.getFirst(data, "building.icon", null);
        String fixedOrientation = LegacyDataParser.getFirst(data, "building.fixedorientation", null);
        int areaToClear = LegacyDataParser.getInt(data, "building.areatoclear", 0);
        int areaToClearLengthBefore = LegacyDataParser.getInt(data, "building.areatoclearlengthbefore", -1);
        int areaToClearLengthAfter = LegacyDataParser.getInt(data, "building.areatoclearlengthafter", -1);
        int areaToClearWidthBefore = LegacyDataParser.getInt(data, "building.areatoclearwidthbefore", -1);
        int areaToClearWidthAfter = LegacyDataParser.getInt(data, "building.areatoclearwidthafter", -1);
        int version = LegacyDataParser.getInt(data, "building.version", 1);
        List<String> farFromTags = LegacyDataParser.getAll(data, "building.farfromtag");
        int price = LegacyDataParser.getInt(data, "building.price", 0);
        int reputation = LegacyDataParser.getInt(data, "building.reputation", 0);
        boolean isGift = "true".equalsIgnoreCase(LegacyDataParser.getFirst(data, "building.isgift", null));
        List<String> randomBrickColours = LegacyDataParser.getAll(data, "building.randombrickcolour");
        ArrayList<StartingGoodMeta> startingGoods = new ArrayList<StartingGoodMeta>();
        for (String raw : LegacyDataParser.getAll(data, "building.startinggood")) {
            String[] parts = raw.split(",");
            if (parts.length < 4) continue;
            try {
                startingGoods.add(new StartingGoodMeta(parts[0].trim(), Double.parseDouble(parts[1].trim()), Integer.parseInt(parts[2].trim()), Integer.parseInt(parts[3].trim())));
            }
            catch (NumberFormatException e) {
                System.err.println("  [WARN] invalid startinggood in " + baseName + ": " + raw);
            }
        }
        boolean showTownHallSigns = !"false".equalsIgnoreCase(LegacyDataParser.getFirst(data, "building.showtownhallsigns", "true"));
        boolean isSubBuilding = "true".equalsIgnoreCase(LegacyDataParser.getFirst(data, "building.issubbuilding", "false"));
        boolean isWallSegment = "true".equalsIgnoreCase(LegacyDataParser.getFirst(data, "building.iswallsegment", "false"));
        boolean isBorderBuilding = "true".equalsIgnoreCase(LegacyDataParser.getFirst(data, "building.isborderbuilding", "false"));
        String initialSigns = LegacyDataParser.getFirst(data, "initial.signs", null);
        int initialPathLevel = LegacyDataParser.getInt(data, "initial.pathlevel", 0);
        int initialPathWidth = LegacyDataParser.getInt(data, "initial.pathwidth", 2);
        int initialPriorityMoveIn = LegacyDataParser.getInt(data, "initial.prioritymovein", 0);
        ArrayList<LevelMeta> levels = new ArrayList<LevelMeta>();
        List<String> initialSubs = LegacyDataParser.getAll(data, "initial.subbuilding");
        List<String> initialRequiredTags = LegacyDataParser.getAll(data, "initial.requiredtag");
        List<String> initialAbstractedProd = LegacyDataParser.getAll(data, "initial.abstractedproduction");
        List<String> initialParentTags = LegacyDataParser.getAll(data, "initial.parenttag");
        List<String> initialRequiredParentTags = LegacyDataParser.getAllWithFallback(data, "initial.requiredparenttag", "initial.requiredparenttags");
        List<String> initialClearTags = LegacyDataParser.getAll(data, "initial.cleartag");
        List<String> initialVillageTags = LegacyDataParser.getAll(data, "initial.villagetag");
        List<String> initialForbiddenTagsInVillage = LegacyDataParser.getAll(data, "initial.forbiddentaginvillage");
        int initialIrrigation = LegacyDataParser.getInt(data, "initial.irrigation", 0);
        levels.add(new LevelMeta(0, initialStartLevel, initialPriority, initialName, initialTags, initialSubs, initialPathLevel, false, initialPathWidth, initialSigns, initialPriorityMoveIn, 0, initialAbstractedProd, initialRequiredTags, initialParentTags, initialRequiredParentTags, initialClearTags, initialVillageTags, initialForbiddenTagsInVillage, initialIrrigation));
        ArrayList<String> allSubBuildings = new ArrayList<String>(initialSubs);
        int previousStartLevel = initialStartLevel;
        int maxUpgrade = 0;
        for (String key : data.keySet()) {
            if (!key.startsWith("upgrade")) continue;
            try {
                int n = Integer.parseInt(key.substring(7, key.indexOf(46)));
                maxUpgrade = Math.max(maxUpgrade, n);
            }
            catch (NumberFormatException | StringIndexOutOfBoundsException n) {}
        }
        for (int i = 1; i <= maxUpgrade; ++i) {
            String prefix = "upgrade" + i + ".";
            if (!LegacyDataParser.hasPrefix(data, prefix)) {
                int inheritedPriority = ((LevelMeta)levels.get(i - 1)).priority();
                int inheritedIrrigation = ((LevelMeta)levels.get(i - 1)).irrigation();
                levels.add(new LevelMeta(i, previousStartLevel, inheritedPriority, null, List.of(), List.of(), 0, false, 2, null, 0, 0, List.of(), List.of(), List.of(), List.of(), List.of(), List.of(), List.of(), inheritedIrrigation));
                continue;
            }
            int startLevel = LegacyDataParser.getInt(data, prefix + "startlevel", previousStartLevel);
            int previousPriority = ((LevelMeta)levels.get(i - 1)).priority();
            int priority = LegacyDataParser.getInt(data, prefix + "priority", previousPriority);
            int previousIrrigation = ((LevelMeta)levels.get(i - 1)).irrigation();
            int irrigation = LegacyDataParser.getInt(data, prefix + "irrigation", previousIrrigation);
            String name = LegacyDataParser.getFirst(data, prefix + "nativename", null);
            ArrayList<String> levelTags = new ArrayList<String>(LegacyDataParser.getAll(data, prefix + "tag"));
            if ("true".equalsIgnoreCase(LegacyDataParser.getFirst(data, prefix + "nopathstobuilding", "false")) && !levelTags.contains("nopaths")) {
                levelTags.add("nopaths");
            }
            List<String> subs = LegacyDataParser.getAll(data, prefix + "subbuilding");
            allSubBuildings.addAll(subs);
            int pathLevel = LegacyDataParser.getInt(data, prefix + "pathlevel", 0);
            boolean rebuildPath = "true".equalsIgnoreCase(LegacyDataParser.getFirst(data, prefix + "rebuildpath", "false"));
            int pathWidth = LegacyDataParser.getInt(data, prefix + "pathwidth", 2);
            int extraWalls = LegacyDataParser.getInt(data, prefix + "extrasimultaneouswallconstructions", 0);
            List<String> abstractedProd = LegacyDataParser.getAll(data, prefix + "abstractedproduction");
            List<String> requiredTags = LegacyDataParser.getAll(data, prefix + "requiredtag");
            List<String> parentTags = LegacyDataParser.getAll(data, prefix + "parenttag");
            List<String> requiredParentTags = LegacyDataParser.getAllWithFallback(data, prefix + "requiredparenttag", prefix + "requiredparenttags");
            List<String> clearTags = LegacyDataParser.getAll(data, prefix + "cleartag");
            List<String> villageTags = LegacyDataParser.getAll(data, prefix + "villagetag");
            List<String> forbiddenTagsInVillage = LegacyDataParser.getAll(data, prefix + "forbiddentaginvillage");
            levels.add(new LevelMeta(i, startLevel, priority, name, levelTags, subs, pathLevel, rebuildPath, pathWidth, null, 0, extraWalls, abstractedProd, requiredTags, parentTags, requiredParentTags, clearTags, villageTags, forbiddenTagsInVillage, irrigation));
            previousStartLevel = startLevel;
        }
        ArrayList<String> tags = new ArrayList<String>();
        for (LevelMeta lm : levels) {
            for (String t : lm.tags()) {
                if (tags.contains(t)) continue;
                tags.add(t);
            }
        }
        return new BuildingMeta(baseName, category, width, length, orientation, maxCount, minDist, maxDist, levels, tags, males, females, visitors, allSubBuildings, startingSubBuildings, shop, icon, fixedOrientation, areaToClear, areaToClearLengthBefore, areaToClearLengthAfter, areaToClearWidthBefore, areaToClearWidthAfter, version, farFromTags, price, reputation, isGift, randomBrickColours, startingGoods, showTownHallSigns, isSubBuilding, isWallSegment, isBorderBuilding);
    }

    public static List<BuildingWithVariants> scanBuildingDirectory(Path dir, String category) throws IOException {
        if (!Files.isDirectory(dir, new LinkOption[0])) {
            return List.of();
        }
        TreeMap<String, Map> txtByBaseAndVariant = new TreeMap<String, Map>();
        TreeMap pngsByBaseAndVariant = new TreeMap();
        ArrayList<Path> allFiles = new ArrayList<Path>();
        try (DirectoryStream<Path> stream = Files.newDirectoryStream(dir);){
            Iterator<Object> iterator = stream.iterator();
            while (iterator.hasNext()) {
                Path path = iterator.next();
                if (Files.isDirectory(path, new LinkOption[0])) {
                    DirectoryStream<Path> subStream = Files.newDirectoryStream(path);
                    try {
                        for (Path subFile : subStream) {
                            if (!Files.isRegularFile(subFile, new LinkOption[0])) continue;
                            allFiles.add(subFile);
                        }
                        continue;
                    }
                    finally {
                        if (subStream != null) {
                            subStream.close();
                        }
                        continue;
                    }
                }
                allFiles.add(path);
            }
        }
        for (Path file : allFiles) {
            ParsedPngName parsed;
            String string = file.getFileName().toString();
            if (string.endsWith(".txt")) {
                String base = LegacyDataParser.extractBaseName(string);
                String variant = LegacyDataParser.extractVariant(string);
                if (variant == null) continue;
                txtByBaseAndVariant.computeIfAbsent(base, k -> new TreeMap()).put(variant, file);
                continue;
            }
            if (!string.endsWith(".png") || (parsed = LegacyDataParser.parsePngName(string)) == null) continue;
            pngsByBaseAndVariant.computeIfAbsent(parsed.baseName, k -> new TreeMap()).computeIfAbsent(parsed.variant, k -> new ArrayList()).add(file);
        }
        ArrayList<BuildingWithVariants> results = new ArrayList<BuildingWithVariants>();
        for (Map.Entry entry : txtByBaseAndVariant.entrySet()) {
            String baseName = (String)entry.getKey();
            Map variantTxts = (Map)entry.getValue();
            try {
                TreeMap<String, BuildingMeta> variantMetas = new TreeMap<String, BuildingMeta>();
                BuildingMeta primaryMeta = null;
                for (Map.Entry vtEntry : variantTxts.entrySet()) {
                    BuildingMeta vm = LegacyDataParser.parseBuildingTxt((Path)vtEntry.getValue(), category);
                    variantMetas.put((String)vtEntry.getKey(), vm);
                    if (primaryMeta != null) continue;
                    primaryMeta = vm;
                }
                if (primaryMeta == null) continue;
                Map<String, List<Path>> variants = pngsByBaseAndVariant.getOrDefault(baseName, Map.of());
                for (List pngs : variants.values()) {
                    pngs.sort(Comparator.comparing(p -> {
                        ParsedPngName pp = LegacyDataParser.parsePngName(p.getFileName().toString());
                        return pp != null ? pp.level : 0;
                    }));
                }
                results.add(new BuildingWithVariants(primaryMeta, variants, variantMetas));
            }
            catch (Exception e) {
                System.err.println("[WARN] Parsing error for " + baseName + ": " + e.getMessage());
            }
        }
        return results;
    }

    public static VillagerMeta parseVillagerTxt(Path txtPath, String category) throws IOException {
        String model;
        Map<String, List<String>> data = LegacyDataParser.parseKeyValues(txtPath);
        String id = txtPath.getFileName().toString().replace(".txt", "");
        String nativeName = LegacyDataParser.getFirst(data, "native_name", id);
        String gender = LegacyDataParser.getFirst(data, "gender", "male");
        String rawModel = LegacyDataParser.getFirst(data, "model", null);
        if (rawModel != null) {
            model = switch (rawModel.toLowerCase()) {
                case "femaleasymmetrical" -> "female_asymmetrical";
                case "femalesymmetrical" -> "female_symmetrical";
                default -> rawModel.toLowerCase();
            };
        } else {
            model = gender.equals("female") ? "female_symmetrical" : "male";
        }
        List<String> textures = LegacyDataParser.getAll(data, "texture");
        List<String> goals = LegacyDataParser.getAll(data, "goal");
        ArrayList<String> tags = new ArrayList<String>(LegacyDataParser.getAll(data, "tag"));
        int health = LegacyDataParser.getLastInt(data, "health", 20);
        ArrayList<String> clothesLayer0 = new ArrayList<String>();
        ArrayList<String> clothesLayer1 = new ArrayList<String>();
        LinkedHashMap groupedLayer0 = new LinkedHashMap();
        LinkedHashMap groupedLayer1 = new LinkedHashMap();
        for (String raw : LegacyDataParser.getAll(data, "clothes")) {
            String group;
            String[] parts = raw.split(",");
            if (parts.length >= 3) {
                group = parts[0].trim();
                int layer = Integer.parseInt(parts[1].trim());
                String tex = ((String)parts[2]).trim();
                if (layer == 0) {
                    clothesLayer0.add(tex);
                    groupedLayer0.computeIfAbsent(group, k -> new ArrayList()).add(tex);
                    continue;
                }
                clothesLayer1.add(tex);
                groupedLayer1.computeIfAbsent(group, k -> new ArrayList()).add(tex);
                continue;
            }
            if (parts.length != 2) continue;
            group = parts[0].trim();
            String tex = ((String)parts[1]).trim();
            clothesLayer0.add(tex);
            groupedLayer0.computeIfAbsent(group, k -> new ArrayList()).add(tex);
        }
        LinkedHashSet allGroups = new LinkedHashSet(groupedLayer0.keySet());
        allGroups.addAll(groupedLayer1.keySet());
        ArrayList<ClothesGroup> clothesGroups = new ArrayList<ClothesGroup>();
        for (String group : allGroups) {
            clothesGroups.add(new ClothesGroup(group, groupedLayer0.getOrDefault(group, List.of()), groupedLayer1.getOrDefault(group, List.of())));
        }
        ArrayList<String> clothes = new ArrayList<String>(clothesLayer0);
        LinkedHashMap<String, Integer> startingInv = new LinkedHashMap<String, Integer>();
        for (String raw : LegacyDataParser.getAll(data, "startingInv")) {
            String[] parts = raw.split(",");
            if (parts.length != 2) continue;
            startingInv.put(parts[0].trim(), Integer.parseInt(parts[1].trim()));
        }
        String firstNameList = LegacyDataParser.getFirst(data, "firstNameList", null);
        String familyNameList = LegacyDataParser.getFirst(data, "familyNameList", null);
        String maleChild = LegacyDataParser.getFirst(data, "malechild", null);
        String femaleChild = LegacyDataParser.getFirst(data, "femalechild", null);
        List<String> bringBackHomeGoods = LegacyDataParser.getAll(data, "bringBackHomeGood");
        List<String> collectGoods = LegacyDataParser.getAll(data, "collectGood");
        LinkedHashMap<String, Integer> requiredGoods = new LinkedHashMap<String, Integer>();
        for (String raw : LegacyDataParser.getAll(data, "requiredGood")) {
            String[] parts = raw.split(",");
            if (parts.length != 2) continue;
            requiredGoods.put(parts[0].trim(), Integer.parseInt(parts[1].trim()));
        }
        String icon = LegacyDataParser.getFirst(data, "icon", null);
        double baseHeight = LegacyDataParser.getDouble(data, "baseheight", 1.0);
        List<String> toolNeededClasses = LegacyDataParser.getAll(data, "toolneededclass");
        List<String> itemsNeeded = LegacyDataParser.getAll(data, "itemneeded");
        int baseAttackStrength = LegacyDataParser.getLastInt(data, "baseattackstrength", 0);
        String defaultWeapon = LegacyDataParser.getFirst(data, "defaultweapon", null);
        int experienceGiven = LegacyDataParser.getInt(data, "experiencegiven", 0);
        String hiringCost = LegacyDataParser.getFirst(data, "hiringcost", null);
        String altNativeName = LegacyDataParser.getFirst(data, "alt_native_name", null);
        String altKey = LegacyDataParser.getFirst(data, "alt_key", null);
        String travelbookHeldItem = LegacyDataParser.getFirst(data, "travelbook_held_item", null);
        String travelbookHeldItemOffHand = LegacyDataParser.getFirst(data, "travelbook_held_item_off_hand", null);
        boolean travelbookMain = "true".equalsIgnoreCase(LegacyDataParser.getFirst(data, "travelbook_main_culture_villager", "false"));
        String villagerConfig = LegacyDataParser.getFirst(data, "villagerconfig", null);
        int chanceWeight = LegacyDataParser.getInt(data, "chanceweight", 0);
        LinkedHashMap<String, Integer> merchantStock = new LinkedHashMap<String, Integer>();
        for (String raw : LegacyDataParser.getAll(data, "merchantstock")) {
            String[] parts = raw.split(",");
            if (parts.length != 2) continue;
            merchantStock.put(parts[0].trim(), Integer.parseInt(parts[1].trim()));
        }
        return new VillagerMeta(id, nativeName, gender, model, textures, clothes, goals, tags, health, category, clothesLayer0, clothesLayer1, clothesGroups, startingInv, firstNameList, familyNameList, maleChild, femaleChild, bringBackHomeGoods, collectGoods, requiredGoods, icon, baseHeight, toolNeededClasses, itemsNeeded, baseAttackStrength, defaultWeapon, experienceGiven, hiringCost, altNativeName, altKey, travelbookHeldItem, travelbookHeldItemOffHand, travelbookMain, villagerConfig, merchantStock, chanceWeight);
    }

    public static List<VillagerMeta> scanVillagerDirectory(Path dir, String category) throws IOException {
        if (!Files.isDirectory(dir, new LinkOption[0])) {
            return List.of();
        }
        ArrayList<VillagerMeta> results = new ArrayList<VillagerMeta>();
        try (DirectoryStream<Path> stream = Files.newDirectoryStream(dir, "*.txt");){
            for (Path file : stream) {
                try {
                    results.add(LegacyDataParser.parseVillagerTxt(file, category));
                }
                catch (Exception e) {
                    System.err.println("[WARN] Erreur parsing villager " + String.valueOf(file) + ": " + e.getMessage());
                }
            }
        }
        results.sort(Comparator.comparing(VillagerMeta::id));
        return results;
    }

    public static BrickColourThemeLegacy parseBrickColourTheme(String raw) {
        String[] parts = raw.split(";");
        String[] nameWeight = parts[0].split(":");
        String name = nameWeight[0].trim();
        int weight = Integer.parseInt(nameWeight[1].trim());
        LinkedHashMap<String, List<WeightedColorLegacy>> groups = new LinkedHashMap<String, List<WeightedColorLegacy>>();
        for (int i = 1; i < parts.length; ++i) {
            String part = parts[i].trim();
            int firstColon = part.indexOf(58);
            String groupName = part.substring(0, firstColon);
            String colorsStr = part.substring(firstColon + 1);
            ArrayList<WeightedColorLegacy> colors = new ArrayList<WeightedColorLegacy>();
            for (String colorEntry : colorsStr.split(",")) {
                String[] cv = colorEntry.trim().split(":");
                String colorName = cv[0].trim();
                int colorWeight = Integer.parseInt(cv[1].trim());
                colors.add(new WeightedColorLegacy(colorName, colorWeight));
            }
            groups.put(groupName, colors);
        }
        return new BrickColourThemeLegacy(name, weight, groups);
    }

    public static VillageTypeMeta parseVillageTypeTxt(Path txtPath) throws IOException {
        return LegacyDataParser.parseVillageTypeTxt(txtPath, false);
    }

    public static VillageTypeMeta parseVillageTypeTxt(Path txtPath, boolean loneBuilding) throws IOException {
        Map<String, List<String>> data = LegacyDataParser.parseKeyValues(txtPath);
        String id = txtPath.getFileName().toString().replace(".txt", "").toLowerCase(Locale.ROOT);
        id = LegacyIdCanonicaliser.applyVillageTypeTypoFix(id);
        String name = LegacyDataParser.getFirst(data, "name", id);
        int weight = LegacyDataParser.getInt(data, "weight", 10);
        boolean playerControlled = "true".equalsIgnoreCase(LegacyDataParser.getFirst(data, "playercontrolled", "false"));
        String centre = LegacyDataParser.getFirst(data, "centre", null);
        List<String> start = LegacyDataParser.getAll(data, "start");
        List<String> core = LegacyDataParser.getAll(data, "core");
        List<String> secondary = LegacyDataParser.getAll(data, "secondary");
        List<String> never = LegacyDataParser.getAll(data, "never");
        List<String> biomes = LegacyDataParser.getAll(data, "biome");
        LinkedHashMap<String, String> sellingPrices = new LinkedHashMap<String, String>();
        for (String string : LegacyDataParser.getAll(data, "sellingprice")) {
            int comma = string.indexOf(44);
            if (comma <= 0) continue;
            sellingPrices.put(string.substring(0, comma).trim(), string.substring(comma + 1).trim());
        }
        LinkedHashMap<String, String> buyingPrices = new LinkedHashMap<String, String>();
        for (String entry : LegacyDataParser.getAll(data, "buyingprice")) {
            int comma = entry.indexOf(44);
            if (comma <= 0) continue;
            buyingPrices.put(entry.substring(0, comma).trim(), entry.substring(comma + 1).trim());
        }
        String string = LegacyDataParser.getFirst(data, "icon", null);
        boolean carriesRaid = "true".equalsIgnoreCase(LegacyDataParser.getFirst(data, "carriesraid", "false"));
        List<String> qualifiers = LegacyDataParser.getAll(data, "qualifier");
        String hillQualifier = LegacyDataParser.getFirst(data, "hillqualifier", null);
        String mountainQualifier = LegacyDataParser.getFirst(data, "mountainqualifier", null);
        String desertQualifier = LegacyDataParser.getFirst(data, "desertqualifier", null);
        String forestQualifier = LegacyDataParser.getFirst(data, "forestqualifier", null);
        String lavaQualifier = LegacyDataParser.getFirst(data, "lavaqualifier", null);
        String lakeQualifier = LegacyDataParser.getFirst(data, "lakequalifier", null);
        String oceanQualifier = LegacyDataParser.getFirst(data, "oceanqualifier", null);
        List<String> pathMaterials = LegacyDataParser.getAll(data, "pathmaterial");
        List<String> playerBuildings = LegacyDataParser.getAll(data, "player");
        String innerWallType = LegacyDataParser.getFirst(data, "innerwalltype", null);
        int innerWallRadius = LegacyDataParser.getInt(data, "innerwallradius", 0);
        String outerWallType = LegacyDataParser.getFirst(data, "outerwalltype", null);
        int outerWallRadius = LegacyDataParser.getInt(data, "outerwallradius", 0);
        List<String> bannerJsons = LegacyDataParser.getAll(data, "banner_json");
        int maxSimWalls = LegacyDataParser.getInt(data, "maxsimultaneouswallconstructions", 0);
        int maxSimConstructions = LegacyDataParser.getInt(data, "maxsimultaneousconstructions", 1);
        ArrayList<BrickColourThemeLegacy> brickThemes = new ArrayList<BrickColourThemeLegacy>();
        for (String raw : LegacyDataParser.getAll(data, "brickcolourtheme")) {
            try {
                brickThemes.add(LegacyDataParser.parseBrickColourTheme(raw));
            }
            catch (Exception e) {
                System.err.println("  [WARN] brickcolourtheme invalide dans " + id + " : " + raw);
            }
        }
        String namelist = LegacyDataParser.getFirst(data, "namelist", null);
        int radius = LegacyDataParser.getInt(data, "radius", 80);
        boolean generateForPlayer = "true".equalsIgnoreCase(LegacyDataParser.getFirst(data, "generateforplayer", "false"));
        boolean keyLB = "true".equalsIgnoreCase(LegacyDataParser.getFirst(data, "keylonebuilding", "false"));
        String keyLBGenerateTag = LegacyDataParser.getFirst(data, "keylonebuildinggeneratetag", null);
        int maxLB = LegacyDataParser.getInt(data, "max", -1);
        int minDistFromSpawn = LegacyDataParser.getInt(data, "mindistancefromspawn", -1);
        double minBiomeValidity = LegacyDataParser.getDouble(data, "minimumbiomevalidity", 0.0);
        List<String> hamlets = LegacyDataParser.getAll(data, "hameau");
        String specialType = LegacyDataParser.getFirst(data, "type", null);
        if (!hamlets.isEmpty() && "hameau".equals(specialType)) {
            specialType = null;
        }
        boolean spawnableFlag = "true".equalsIgnoreCase(LegacyDataParser.getFirst(data, "spawnable", loneBuilding ? "false" : "true"));
        boolean allowExtraBuildings = !loneBuilding && specialType == null && !playerControlled;
        return new VillageTypeMeta(id, name, weight, playerControlled, centre, start, core, secondary, never, biomes, sellingPrices, buyingPrices, string, carriesRaid, qualifiers, hillQualifier, mountainQualifier, desertQualifier, forestQualifier, lavaQualifier, lakeQualifier, oceanQualifier, pathMaterials, playerBuildings, innerWallType, innerWallRadius, outerWallType, outerWallRadius, bannerJsons, maxSimWalls, maxSimConstructions, brickThemes, loneBuilding, namelist, radius, generateForPlayer, keyLB, keyLBGenerateTag, maxLB, minDistFromSpawn, minBiomeValidity, hamlets, specialType, spawnableFlag, allowExtraBuildings);
    }

    public static List<VillageTypeMeta> scanVillageTypes(Path dir) throws IOException {
        if (!Files.isDirectory(dir, new LinkOption[0])) {
            return List.of();
        }
        ArrayList<VillageTypeMeta> results = new ArrayList<VillageTypeMeta>();
        LegacyDataParser.scanVillageTypesInDir(dir, results);
        try (DirectoryStream<Path> subdirs = Files.newDirectoryStream(dir, x$0 -> Files.isDirectory(x$0, new LinkOption[0]));){
            for (Path subdir : subdirs) {
                LegacyDataParser.scanVillageTypesInDir(subdir, results);
            }
        }
        results.sort(Comparator.comparing(VillageTypeMeta::id));
        return results;
    }

    private static void scanVillageTypesInDir(Path dir, List<VillageTypeMeta> results) throws IOException {
        try (DirectoryStream<Path> stream = Files.newDirectoryStream(dir, "*.txt");){
            for (Path file : stream) {
                String name = file.getFileName().toString();
                if (name.startsWith("custom")) continue;
                try {
                    results.add(LegacyDataParser.parseVillageTypeTxt(file));
                }
                catch (Exception e) {
                    System.err.println("[WARN] Error parsing village type " + String.valueOf(file) + ": " + e.getMessage());
                }
            }
        }
    }

    public static List<VillageTypeMeta> scanLoneBuildingTypes(Path dir) throws IOException {
        if (!Files.isDirectory(dir, new LinkOption[0])) {
            return List.of();
        }
        ArrayList<VillageTypeMeta> results = new ArrayList<VillageTypeMeta>();
        try (DirectoryStream<Path> stream = Files.newDirectoryStream(dir, "*.txt");){
            for (Path file : stream) {
                try {
                    results.add(LegacyDataParser.parseVillageTypeTxt(file, true));
                }
                catch (Exception e) {
                    System.err.println("[WARN] Erreur parsing lone building type " + String.valueOf(file) + ": " + e.getMessage());
                }
            }
        }
        results.sort(Comparator.comparing(VillageTypeMeta::id));
        return results;
    }

    static ParsedPngName parsePngName(String fileName) {
        if (!fileName.endsWith(".png")) {
            return null;
        }
        String name = fileName.substring(0, fileName.length() - 4);
        Pattern p = Pattern.compile("^(.+)_([A-Za-z])(\\d+)$");
        Matcher m = p.matcher(name);
        if (m.matches()) {
            return new ParsedPngName(m.group(1), m.group(2).toUpperCase(), Integer.parseInt(m.group(3)));
        }
        return null;
    }

    static String extractBaseName(String fileName) {
        Pattern p;
        Matcher m;
        String name = fileName;
        if (name.endsWith(".txt")) {
            name = name.substring(0, name.length() - 4);
        }
        if ((m = (p = Pattern.compile("^(.+)_([A-Z])$")).matcher(name)).matches()) {
            return m.group(1);
        }
        return name;
    }

    static String extractVariant(String fileName) {
        Pattern p;
        Matcher m;
        String name = fileName;
        if (name.endsWith(".txt")) {
            name = name.substring(0, name.length() - 4);
        }
        if ((m = (p = Pattern.compile("^.+_([A-Z])$")).matcher(name)).matches()) {
            return m.group(1);
        }
        return null;
    }

    public static Map<String, List<String>> parseKeyValues(Path path) throws IOException {
        LinkedHashMap<String, List<String>> data = new LinkedHashMap<String, List<String>>();
        for (String line : LegacyDataParser.readLinesLenient(path)) {
            int eq;
            if ((line = line.trim()).isEmpty() || line.startsWith("//") || (eq = line.indexOf(61)) < 0) continue;
            String key = line.substring(0, eq).trim().toLowerCase();
            String value = line.substring(eq + 1).trim();
            data.computeIfAbsent(key, k -> new ArrayList()).add(value);
        }
        return data;
    }

    private static String getFirst(Map<String, List<String>> data, String key, String defaultValue) {
        List<String> values = data.get(key.toLowerCase());
        if (values != null && !values.isEmpty()) {
            return values.getFirst();
        }
        return defaultValue;
    }

    private static List<String> getAll(Map<String, List<String>> data, String key) {
        return data.getOrDefault(key.toLowerCase(), List.of());
    }

    private static List<String> getAllWithFallback(Map<String, List<String>> data, String primaryKey, String fallbackKey) {
        ArrayList<String> result = new ArrayList<String>(LegacyDataParser.getAll(data, primaryKey));
        for (String v : LegacyDataParser.getAll(data, fallbackKey)) {
            if (result.contains(v)) continue;
            result.add(v);
        }
        return result.isEmpty() ? List.of() : result;
    }

    private static String getLast(Map<String, List<String>> data, String key, String defaultValue) {
        List<String> values = data.get(key.toLowerCase());
        if (values != null && !values.isEmpty()) {
            return values.getLast();
        }
        return defaultValue;
    }

    private static int getLastInt(Map<String, List<String>> data, String key, int defaultValue) {
        String val = LegacyDataParser.getLast(data, key, null);
        if (val == null) {
            return defaultValue;
        }
        try {
            return Integer.parseInt(val);
        }
        catch (NumberFormatException e) {
            return defaultValue;
        }
    }

    private static int getInt(Map<String, List<String>> data, String key, int defaultValue) {
        String val = LegacyDataParser.getFirst(data, key, null);
        if (val == null) {
            return defaultValue;
        }
        try {
            return Integer.parseInt(val);
        }
        catch (NumberFormatException e) {
            return defaultValue;
        }
    }

    private static double getDouble(Map<String, List<String>> data, String key, double defaultValue) {
        String val = LegacyDataParser.getFirst(data, key, null);
        if (val == null) {
            return defaultValue;
        }
        try {
            return Double.parseDouble(val);
        }
        catch (NumberFormatException e) {
            return defaultValue;
        }
    }

    private static boolean hasPrefix(Map<String, List<String>> data, String prefix) {
        String lowerPrefix = prefix.toLowerCase();
        return data.keySet().stream().anyMatch(k -> k.startsWith(lowerPrefix));
    }

    public static ShopMeta parseShopTxt(Path txtPath) throws IOException {
        Map<String, List<String>> data = LegacyDataParser.parseKeyValues(txtPath);
        String id = txtPath.getFileName().toString().replace(".txt", "");
        List<String> sells = LegacyDataParser.splitCsv(LegacyDataParser.getFirst(data, "sells", ""));
        List<String> buys = LegacyDataParser.splitCsv(LegacyDataParser.getFirst(data, "buys", ""));
        List<String> buysOptional = LegacyDataParser.splitCsv(LegacyDataParser.getFirst(data, "buysoptional", ""));
        List<String> deliverTo = LegacyDataParser.splitCsv(LegacyDataParser.getFirst(data, "deliverto", ""));
        return new ShopMeta(id, sells, buys, buysOptional, deliverTo);
    }

    public static List<ShopMeta> scanShopDirectory(Path dir) throws IOException {
        if (!Files.isDirectory(dir, new LinkOption[0])) {
            return List.of();
        }
        ArrayList<ShopMeta> results = new ArrayList<ShopMeta>();
        try (DirectoryStream<Path> stream = Files.newDirectoryStream(dir, "*.txt");){
            for (Path file : stream) {
                try {
                    results.add(LegacyDataParser.parseShopTxt(file));
                }
                catch (Exception e) {
                    System.err.println("[WARN] Erreur parsing shop " + String.valueOf(file) + ": " + e.getMessage());
                }
            }
        }
        results.sort(Comparator.comparing(ShopMeta::id));
        return results;
    }

    private static List<String> splitCsv(String csv) {
        if (csv == null || csv.isBlank()) {
            return List.of();
        }
        ArrayList<String> result = new ArrayList<String>();
        for (String s : csv.split(",")) {
            String trimmed = s.trim();
            if (trimmed.isEmpty()) continue;
            result.add(trimmed);
        }
        return result;
    }

    public static List<TradedGoodMeta> parseTradedGoodsTxt(Path txtPath) throws IOException {
        ArrayList<TradedGoodMeta> results = new ArrayList<TradedGoodMeta>();
        for (String line : LegacyDataParser.readLinesLenient(txtPath)) {
            String[] parts;
            if ((line = line.trim()).isEmpty() || line.startsWith("//") || (parts = line.split(",", -1)).length < 10) continue;
            String name = parts[0].trim();
            String sellingPrice = parts[1].trim();
            String buyingPrice = parts[2].trim();
            int reservedQty = LegacyDataParser.safeParseInt(parts[3].trim(), 0);
            int targetQty = LegacyDataParser.safeParseInt(parts[4].trim(), 0);
            String foreignPrice = parts[5].trim();
            boolean autoGen = "true".equalsIgnoreCase(parts[6].trim());
            int minRep = LegacyDataParser.safeParseInt(parts[8].trim(), 0);
            String category = parts[9].trim();
            results.add(new TradedGoodMeta(name, sellingPrice, buyingPrice, reservedQty, targetQty, foreignPrice, autoGen, minRep, category));
        }
        return results;
    }

    private static int safeParseInt(String s, int fallback) {
        if (s == null || s.isBlank()) {
            return fallback;
        }
        try {
            return Integer.parseInt(s);
        }
        catch (NumberFormatException e) {
            return fallback;
        }
    }

    public static List<String> parseNamelistTxt(Path txtPath) throws IOException {
        ArrayList<String> names = new ArrayList<String>();
        for (String line : LegacyDataParser.readLinesLenient(txtPath)) {
            if ((line = line.trim()).isEmpty() || line.startsWith("//")) continue;
            names.add(line);
        }
        return names;
    }

    public static Map<String, List<String>> scanNamelistDirectory(Path dir) throws IOException {
        if (!Files.isDirectory(dir, new LinkOption[0])) {
            return Map.of();
        }
        TreeMap<String, List<String>> results = new TreeMap<String, List<String>>();
        try (DirectoryStream<Path> stream = Files.newDirectoryStream(dir, "*.txt");){
            for (Path file : stream) {
                String key = file.getFileName().toString().replace(".txt", "");
                try {
                    List<String> names;
                    if (key.endsWith(" copie") || (names = LegacyDataParser.parseNamelistTxt(file)).isEmpty()) continue;
                    results.put(key, names);
                }
                catch (Exception e) {
                    System.err.println("[WARN] Erreur parsing namelist " + String.valueOf(file) + ": " + e.getMessage());
                }
            }
        }
        return results;
    }

    public record StartingGoodMeta(String item, double probability, int fixedNumber, int randomNumber) {
    }

    public record LevelMeta(int level, int startLevel, int priority, String nativeName, List<String> tags, List<String> subBuildings, int pathLevel, boolean rebuildPath, int pathWidth, String signs, int priorityMoveIn, int extraSimultaneousWallConstructions, List<String> abstractedProduction, List<String> requiredTags, List<String> parentTags, List<String> requiredParentTags, List<String> clearTags, List<String> villageTags, List<String> forbiddenTagsInVillage, int irrigation) {
    }

    public record BuildingMeta(String baseName, String category, int width, int length, int buildingOrientation, int maxCount, double minDistance, double maxDistance, List<LevelMeta> levels, List<String> tags, List<String> males, List<String> females, List<String> visitors, List<String> subBuildings, List<String> startingSubBuildings, String shop, String icon, String fixedOrientation, int areaToClear, int areaToClearLengthBefore, int areaToClearLengthAfter, int areaToClearWidthBefore, int areaToClearWidthAfter, int version, List<String> farFromTags, int price, int reputation, boolean isGift, List<String> randomBrickColours, List<StartingGoodMeta> startingGoods, boolean showTownHallSigns, boolean isSubBuilding, boolean isWallSegment, boolean isBorderBuilding) {
    }

    private record ParsedPngName(String baseName, String variant, int level) {
    }

    public record BuildingWithVariants(BuildingMeta meta, Map<String, List<Path>> variantPngs, Map<String, BuildingMeta> variantMetas) {
        public BuildingMeta metaForVariant(String variant) {
            return this.variantMetas.getOrDefault(variant, this.meta);
        }
    }

    public record ClothesGroup(String group, List<String> layer0, List<String> layer1) {
    }

    public record VillagerMeta(String id, String nativeName, String gender, String model, List<String> textures, List<String> clothes, List<String> goals, List<String> tags, int health, String category, List<String> clothesLayer0, List<String> clothesLayer1, List<ClothesGroup> clothesGroups, Map<String, Integer> startingInv, String firstNameList, String familyNameList, String maleChild, String femaleChild, List<String> bringBackHomeGoods, List<String> collectGoods, Map<String, Integer> requiredGoods, String icon, double baseHeight, List<String> toolNeededClasses, List<String> itemsNeeded, int baseAttackStrength, String defaultWeapon, int experienceGiven, String hiringCost, String altNativeName, String altKey, String travelbookHeldItem, String travelbookHeldItemOffHand, boolean travelbookMainCultureVillager, String villagerConfig, Map<String, Integer> merchantStock, int chanceWeight) {
    }

    public record WeightedColorLegacy(String color, int weight) {
    }

    public record BrickColourThemeLegacy(String name, int weight, Map<String, List<WeightedColorLegacy>> colourGroups) {
    }

    public record VillageTypeMeta(String id, String name, int weight, boolean playerControlled, String centre, List<String> start, List<String> core, List<String> secondary, List<String> never, List<String> biomes, Map<String, String> sellingPriceOverrides, Map<String, String> buyingPriceOverrides, String icon, boolean carriesRaid, List<String> qualifiers, String hillQualifier, String mountainQualifier, String desertQualifier, String forestQualifier, String lavaQualifier, String lakeQualifier, String oceanQualifier, List<String> pathMaterials, List<String> playerBuildings, String innerWallType, int innerWallRadius, String outerWallType, int outerWallRadius, List<String> bannerJsons, int maxSimultaneousWallConstructions, int maxSimultaneousConstructions, List<BrickColourThemeLegacy> brickColourThemes, boolean loneBuilding, String namelist, int radius, boolean generateForPlayer, boolean keyLoneBuilding, String keyLoneBuildingGenerateTag, int max, int minDistanceFromSpawn, double minimumBiomeValidity, List<String> hamlets, @Nullable String specialType, boolean spawnable, boolean allowExtraBuildings) {
    }

    public record ShopMeta(String id, List<String> sells, List<String> buys, List<String> buysOptional, List<String> deliverTo) {
    }

    public record TradedGoodMeta(String name, String sellingPrice, String buyingPrice, int reservedQuantity, int targetQuantity, String foreignMerchantPrice, boolean autoGenerated, int minReputation, String category) {
    }
}

