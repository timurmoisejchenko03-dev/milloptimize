/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.google.gson.Gson
 *  com.google.gson.GsonBuilder
 *  com.mojang.logging.LogUtils
 *  org.slf4j.Logger
 */
package org.millenaire.content.legacy;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.mojang.logging.LogUtils;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.Charset;
import java.nio.charset.MalformedInputException;
import java.nio.charset.StandardCharsets;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.OpenOption;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.nio.file.attribute.FileAttribute;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.millenaire.content.ContentDirectoryManager;
import org.millenaire.content.CultureIdPolicy;
import org.millenaire.content.legacy.BiomeMapper;
import org.millenaire.content.legacy.ConversionMode;
import org.millenaire.content.legacy.ConverterOutputManifest;
import org.millenaire.content.legacy.ItemIdMapper;
import org.millenaire.content.legacy.LegacyConversionReport;
import org.millenaire.content.legacy.LegacyDataParser;
import org.millenaire.content.legacy.LegacyIdCanonicaliser;
import org.millenaire.content.legacy.LegacyJsonBuilder;
import org.millenaire.content.legacy.LegacyKeySchemas;
import org.millenaire.content.legacy.LegacyLayoutDetector;
import org.millenaire.content.legacy.PngToNbtConverter;
import org.millenaire.content.legacy.PreservationPolicy;
import org.millenaire.content.legacy.ProductionChainChecker;
import org.millenaire.content.legacy.QuestDefinitionConverter;
import org.millenaire.content.legacy.TagConsistencyChecker;
import org.millenaire.content.legacy.ValueValidityChecker;
import org.slf4j.Logger;

public final class LegacyConversionEngine {
    private static final Logger LOGGER = LogUtils.getLogger();
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().disableHtmlEscaping().create();
    private final ItemIdMapper items;
    private final BiomeMapper biomes;
    private final ConverterOutputManifest manifest;
    private final LegacyConversionReport report;
    private final ConversionMode mode;
    private PngToNbtConverter pngConverter;
    private Path currentOutputRoot;
    private CultureRefs currentCultureRefs;
    private static final Pattern VALID_RES_LOC_STEM = Pattern.compile("[a-z0-9_.-]+");
    private static final Pattern VARIANT_SUFFIX = Pattern.compile("(_[A-Z])+$");
    private static final Pattern INTERNAL_VARIANT = Pattern.compile("_[A-Z](?=_)");
    private static final Pattern LR_SUFFIX = Pattern.compile("[LR]\\d+$");
    private static final Set<String> ALL_CULTURE_GOAL_SEGMENTS = Set.of("byzantine", "byzantinerefresh", "inuit", "norman", "indian", "mayan", "japanese", "seljuk");
    private static final int CONSECUTIVE_FAILURE_LIMIT = 10;
    private String lastBuildingFailureClass;
    private static final Pattern BUILDING_PREFIX = Pattern.compile("^(?:building|initial|upgrade\\d+)\\.");
    private static final Set<String> SUPPORTED_QUEST_FAMILIES = Set.of("common", "normanbasic", "indianbasic", "mayanbasic", "byzantinesbasic", "japanesebasic", "seljukbasic", "inuitbasic", "mysterybasic");

    public LegacyConversionEngine(ItemIdMapper items, BiomeMapper biomes, ConverterOutputManifest manifest, LegacyConversionReport report, ConversionMode mode) {
        this.items = items;
        this.biomes = biomes;
        this.manifest = manifest;
        this.report = report;
        this.mode = mode;
        items.installUnmappedReportSink(report::recordUnmappedItem);
        biomes.installUnmappedReportSink(report::recordUnmappedBiome);
    }

    @Deprecated
    public LegacyConversionEngine(ItemIdMapper items, BiomeMapper biomes, ConverterOutputManifest manifest, LegacyConversionReport report) {
        this(items, biomes, manifest, report, ConversionMode.AUTO);
    }

    public void convertAll(Path addonRoot, LegacyLayoutDetector.Detection detection) {
        this.convertAll(addonRoot, addonRoot, detection);
    }

    /*
     * WARNING - Removed try catching itself - possible behaviour change.
     */
    public void convertAll(Path sourceRoot, Path outputRoot, LegacyLayoutDetector.Detection detection) {
        this.pngConverter = null;
        Path previousOutputRoot = this.currentOutputRoot;
        this.currentOutputRoot = outputRoot;
        try {
            for (String culture : detection.culturesWithLegacyContent()) {
                if (Thread.currentThread().isInterrupted()) {
                    LOGGER.warn("Conversion interrupted before culture '{}'", (Object)culture);
                    break;
                }
                if (!CultureIdPolicy.PATTERN.matcher(culture).matches()) {
                    LOGGER.warn("Skipping culture '{}' with invalid id (must match {}); rename the directory under cultures/ to proceed.", (Object)culture, (Object)CultureIdPolicy.PATTERN.pattern());
                    continue;
                }
                Path cultureRoot = outputRoot.resolve("cultures").resolve(culture);
                try {
                    this.convertCulture(sourceRoot, cultureRoot, culture, detection);
                }
                catch (Exception e) {
                    LOGGER.error("Culture '{}' conversion aborted mid-pass", (Object)culture, (Object)e);
                }
            }
            try {
                this.convertGlobalContent(sourceRoot, detection);
            }
            catch (Exception e) {
                LOGGER.error("Global content conversion aborted", (Throwable)e);
            }
            this.report.recordOutOfScope(detection.outOfScopeTxts().size());
            for (Path outOfScope : detection.outOfScopeTxts()) {
                LOGGER.warn("Out-of-scope legacy file, leaving in place: {}", (Object)outOfScope);
            }
            if (this.mode.isStrict()) {
                this.emitConvertAmbiguities();
            }
        }
        finally {
            this.currentOutputRoot = previousOutputRoot;
        }
    }

    private Path writeRootOrSource(Path addonRoot) {
        return this.currentOutputRoot != null ? this.currentOutputRoot : addonRoot;
    }

    private CultureRefs buildCultureRefs(Path sourceRoot, String culture, LegacyLayoutDetector.Detection detection) {
        String stem;
        CultureRefs refs = new CultureRefs();
        Pattern versionMarker = Pattern.compile("_[A-Z]$");
        for (Path p2 : LegacyConversionEngine.onlyCulture(detection.buildingTxts(), culture)) {
            stem = LegacyConversionEngine.stripTxt(p2.getFileName().toString());
            String name = versionMarker.matcher(stem).replaceFirst("").toLowerCase(Locale.ROOT);
            refs.buildingPlans.add(name);
        }
        for (Path p2 : LegacyConversionEngine.onlyCulture(detection.buildingPngs(), culture)) {
            stem = p2.getFileName().toString();
            int dot = stem.lastIndexOf(46);
            if (dot > 0) {
                stem = stem.substring(0, dot);
            }
            String trimmed = stem.replaceAll("(?:_[A-Z]\\d*)+$", "");
            refs.buildingPlans.add(trimmed.toLowerCase(Locale.ROOT));
        }
        for (Path p2 : LegacyConversionEngine.onlyCulture(detection.shopTxts(), culture)) {
            refs.shops.add(LegacyConversionEngine.stripTxt(p2.getFileName().toString()).toLowerCase(Locale.ROOT));
        }
        for (Path p2 : LegacyConversionEngine.onlyCulture(detection.villagerTxts(), culture)) {
            stem = LegacyConversionEngine.stripTxt(p2.getFileName().toString()).toLowerCase(Locale.ROOT);
            refs.villagers.add(culture.toLowerCase(Locale.ROOT) + "/" + stem);
        }
        Path nlDir = sourceRoot.resolve("cultures").resolve(culture).resolve("namelists");
        if (Files.isDirectory(nlDir, new LinkOption[0])) {
            try (Stream<Path> s = Files.list(nlDir);){
                s.filter(p -> p.getFileName().toString().endsWith(".txt")).forEach(p -> refs.namelists.add(LegacyConversionEngine.stripTxt(p.getFileName().toString()).toLowerCase(Locale.ROOT)));
            }
            catch (IOException iOException) {
                // empty catch block
            }
        }
        return refs;
    }

    public void convertCulture(Path addonRoot, Path cultureRoot, String culture, LegacyLayoutDetector.Detection detection) {
        this.currentCultureRefs = this.buildCultureRefs(addonRoot, culture, detection);
        if (this.pngConverter != null) {
            this.pngConverter.clearPerCultureState();
        }
        List<LegacyDataParser.BuildingWithVariants> buildingIndex = this.parseBuildingIndex(culture, detection);
        this.runResourceLocationCheck(addonRoot, culture, detection);
        Set<String> centralBuildings = this.collectCentralBuildings(culture, detection);
        this.convertBuildings(addonRoot, cultureRoot, culture, detection, centralBuildings);
        this.convertVillagerTypes(addonRoot, culture, detection);
        this.convertVillageTypes(addonRoot, culture, detection, buildingIndex);
        this.convertShops(addonRoot, culture, detection);
        this.convertTradedGoods(addonRoot, culture, cultureRoot, detection);
        this.convertCultureStub(addonRoot, culture, cultureRoot, detection);
        this.convertWalls(addonRoot, culture, detection);
        this.runTagConsistencyCheck(addonRoot, culture, detection);
        this.runProductionChainCheck(culture, detection);
    }

    private void runTagConsistencyCheck(Path addonRoot, String culture, LegacyLayoutDetector.Detection detection) {
        Map<String, Path> goalIndex = LegacyConversionEngine.buildGoalIndex(detection, culture);
        TagConsistencyChecker.check(culture, addonRoot, detection, goalIndex, this.report);
    }

    private void runProductionChainCheck(String culture, LegacyLayoutDetector.Detection detection) {
        Map<String, Path> goalIndex = LegacyConversionEngine.buildGoalIndex(detection, culture);
        ProductionChainChecker.check(culture, detection, goalIndex, this.report);
    }

    private void runResourceLocationCheck(Path addonRoot, String culture, LegacyLayoutDetector.Detection detection) {
        HashSet<Path> seen = new HashSet<Path>();
        for (Path p : LegacyConversionEngine.onlyCulture(detection.villagerTxts(), culture)) {
            this.checkOneFilename(addonRoot, culture, p, seen);
        }
        for (Path p : LegacyConversionEngine.onlyCulture(detection.buildingTxts(), culture)) {
            this.checkOneFilename(addonRoot, culture, p, seen);
        }
        for (Path p : LegacyConversionEngine.onlyCulture(detection.villageTxts(), culture)) {
            this.checkOneFilename(addonRoot, culture, p, seen);
        }
        for (Path p : LegacyConversionEngine.onlyCulture(detection.loneTxts(), culture)) {
            this.checkOneFilename(addonRoot, culture, p, seen);
        }
        for (Path p : LegacyConversionEngine.onlyCulture(detection.shopTxts(), culture)) {
            this.checkOneFilename(addonRoot, culture, p, seen);
        }
        for (Path p : LegacyConversionEngine.onlyCulture(detection.tradedGoodsTxts(), culture)) {
            this.checkOneFilename(addonRoot, culture, p, seen);
        }
    }

    private void checkOneFilename(Path addonRoot, String culture, Path source, Set<Path> seen) {
        if (!seen.add(source)) {
            return;
        }
        String stem = LegacyConversionEngine.stripTxt(source.getFileName().toString());
        String stripped = LegacyConversionEngine.stripVariantsToFixedPoint(stem);
        if (VALID_RES_LOC_STEM.matcher(stripped).matches()) {
            return;
        }
        String canonical = LegacyIdCanonicaliser.sanitize(stripped);
        if (!canonical.isEmpty() && VALID_RES_LOC_STEM.matcher(canonical).matches() && !LegacyConversionEngine.losesMeaningfulCharacters(stripped, canonical)) {
            this.report.recordFilenameNormalised(culture, LegacyConversionEngine.relPathFor(addonRoot, source), stem, canonical);
            return;
        }
        LinkedHashSet<Character> bad = new LinkedHashSet<Character>();
        for (int i = 0; i < stripped.length(); ++i) {
            boolean ok;
            char c = stripped.charAt(i);
            boolean bl = ok = c >= 'a' && c <= 'z' || c >= '0' && c <= '9' || c == '_' || c == '.' || c == '-';
            if (ok) continue;
            bad.add(Character.valueOf(c));
        }
        StringBuilder sb = new StringBuilder();
        for (Character c : bad) {
            if (sb.length() > 0) {
                sb.append(' ');
            }
            sb.append('\'').append(c).append('\'');
        }
        this.report.recordInvalidResourceLocation(culture, LegacyConversionEngine.relPathFor(addonRoot, source), sb.toString());
    }

    private static boolean losesMeaningfulCharacters(String stripped, String canonical) {
        for (int i = 0; i < stripped.length(); ++i) {
            boolean preservable;
            char c = stripped.charAt(i);
            boolean bl = preservable = c >= 'a' && c <= 'z' || c >= 'A' && c <= 'Z' || c >= '0' && c <= '9' || c == '_' || c == '.' || c == '-' || c == '\u00f6' || c == '\u00fc' || c == '\u00e7' || c == '\u015f' || c == '\u0131' || c == '\u011f';
            if (preservable) continue;
            return true;
        }
        return false;
    }

    private static String stripVariantsToFixedPoint(String stem) {
        String prev;
        String cur = stem;
        do {
            Matcher lr;
            prev = cur;
            Matcher tm = VARIANT_SUFFIX.matcher(cur);
            if (tm.find()) {
                cur = cur.substring(0, tm.start());
            }
            if (!(lr = LR_SUFFIX.matcher(cur = INTERNAL_VARIANT.matcher(cur).replaceAll(""))).find()) continue;
            cur = cur.substring(0, lr.start());
        } while (!cur.equals(prev));
        return cur;
    }

    private static Map<String, Path> buildGoalIndex(LegacyLayoutDetector.Detection detection, String culture) {
        Set<String> aliases = LegacyConversionEngine.cultureGoalAliases(culture);
        Set<String> allCultureSegments = ALL_CULTURE_GOAL_SEGMENTS;
        LinkedHashMap<String, List> candidates = new LinkedHashMap<String, List>();
        for (Path gf : detection.gatheringTxts()) {
            String stem = LegacyConversionEngine.stripTxt(gf.getFileName().toString()).toLowerCase(Locale.ROOT);
            candidates.computeIfAbsent(stem, k -> new ArrayList()).add(gf);
        }
        LinkedHashMap<String, Path> goalIndex = new LinkedHashMap<String, Path>();
        block1: for (Map.Entry e : candidates.entrySet()) {
            List paths = (List)e.getValue();
            Path cultureScoped = null;
            for (Path p : paths) {
                if (!LegacyConversionEngine.pathHasAnySegment(p, aliases)) continue;
                cultureScoped = p;
                break;
            }
            if (cultureScoped != null) {
                goalIndex.put((String)e.getKey(), cultureScoped);
                continue;
            }
            for (Path p : paths) {
                if (LegacyConversionEngine.pathHasAnySegment(p, allCultureSegments)) continue;
                goalIndex.put((String)e.getKey(), p);
                continue block1;
            }
        }
        return goalIndex;
    }

    private static Set<String> cultureGoalAliases(String culture) {
        String c;
        if (culture == null) {
            return Set.of();
        }
        return switch (c = culture.toLowerCase(Locale.ROOT)) {
            case "byzantines" -> Set.of("byzantine", "byzantinerefresh");
            case "inuits" -> Set.of("inuit");
            case "norman", "indian", "mayan", "japanese", "seljuk" -> Set.of(c);
            default -> Set.of(c);
        };
    }

    private static boolean pathHasAnySegment(Path path, Set<String> segments) {
        if (segments.isEmpty()) {
            return false;
        }
        for (int i = 0; i < path.getNameCount(); ++i) {
            if (!segments.contains(path.getName(i).toString().toLowerCase(Locale.ROOT))) continue;
            return true;
        }
        return false;
    }

    private Set<String> collectCentralBuildings(String culture, LegacyLayoutDetector.Detection detection) {
        LegacyDataParser.VillageTypeMeta vt2;
        HashSet<String> centres = new HashSet<String>();
        HashSet<String> nonCentres = new HashSet<String>();
        for (Path source : LegacyConversionEngine.onlyCulture(detection.villageTxts(), culture)) {
            try {
                vt2 = LegacyDataParser.parseVillageTypeTxt(source);
                LegacyConversionEngine.accumulateRoles(vt2, centres, nonCentres);
            }
            catch (Exception vt2) {}
        }
        for (Path source : LegacyConversionEngine.onlyCulture(detection.loneTxts(), culture)) {
            try {
                vt2 = LegacyDataParser.parseVillageTypeTxt(source, true);
                LegacyConversionEngine.accumulateRoles(vt2, centres, nonCentres);
            }
            catch (Exception exception) {}
        }
        for (String c : centres) {
            if (!nonCentres.contains(c)) continue;
            LOGGER.warn("Culture {}: building '{}' used as both `centre` and a regular slot in different village_types \u2014 is_town_hall flag will apply to every placement (per-plan attribute).", (Object)culture, (Object)c);
        }
        return centres;
    }

    private static void accumulateRoles(LegacyDataParser.VillageTypeMeta vt, Set<String> centres, Set<String> nonCentres) {
        if (vt.centre() != null && !vt.centre().isEmpty()) {
            centres.add(LegacyJsonBuilder.sanitize(vt.centre()));
        }
        for (String b : vt.start()) {
            nonCentres.add(LegacyJsonBuilder.sanitize(b));
        }
        for (String b : vt.core()) {
            nonCentres.add(LegacyJsonBuilder.sanitize(b));
        }
        for (String b : vt.secondary()) {
            nonCentres.add(LegacyJsonBuilder.sanitize(b));
        }
    }

    private List<LegacyDataParser.BuildingWithVariants> parseBuildingIndex(String culture, LegacyLayoutDetector.Detection detection) {
        if (detection.buildingTxts().isEmpty() && detection.buildingPngs().isEmpty()) {
            return List.of();
        }
        TreeMap<String, Path> categoryDirs = new TreeMap<String, Path>();
        for (Path p : LegacyConversionEngine.onlyCulture(detection.buildingTxts(), culture)) {
            LegacyConversionEngine.appendCategoryDir(p, categoryDirs);
        }
        for (Path p : LegacyConversionEngine.onlyCulture(detection.buildingPngs(), culture)) {
            LegacyConversionEngine.appendCategoryDir(p, categoryDirs);
        }
        ArrayList<LegacyDataParser.BuildingWithVariants> all = new ArrayList<LegacyDataParser.BuildingWithVariants>();
        for (Map.Entry<String, Path> e : categoryDirs.entrySet()) {
            try {
                all.addAll(LegacyDataParser.scanBuildingDirectory(e.getValue(), e.getKey()));
            }
            catch (IOException io) {
                LOGGER.warn("building-index scan ({}) failed: {}", (Object)e.getKey(), (Object)io.getMessage());
            }
        }
        return all;
    }

    public void convertGlobalContent(Path addonRoot, LegacyLayoutDetector.Detection detection) {
        this.runGlobalResourceLocationCheck(addonRoot, detection);
        this.convertGatheringTypes(addonRoot, detection);
        this.convertQuests(addonRoot, detection);
    }

    private void runGlobalResourceLocationCheck(Path addonRoot, LegacyLayoutDetector.Detection detection) {
        HashSet<Path> seen = new HashSet<Path>();
        for (Path p : detection.gatheringTxts()) {
            this.checkOneFilename(addonRoot, null, p, seen);
        }
        for (Path p : detection.questTxts()) {
            this.checkOneFilename(addonRoot, null, p, seen);
        }
    }

    private void convertBuildings(Path addonRoot, Path cultureRoot, String culture, LegacyLayoutDetector.Detection detection, Set<String> centralBuildings) {
        List<Path> pngs = LegacyConversionEngine.onlyCulture(detection.buildingPngs(), culture);
        List<Path> txts = LegacyConversionEngine.onlyCulture(detection.buildingTxts(), culture);
        if (pngs.isEmpty() && txts.isEmpty()) {
            return;
        }
        for (Path path : txts) {
            this.checkUnknownBuildingKeys(culture, addonRoot, path);
            this.checkBuildingValues(culture, addonRoot, path);
            this.checkSuspiciousPriorities(culture, addonRoot, path);
        }
        TreeMap<String, Path> categoryDirs = new TreeMap<String, Path>();
        for (Path p : txts) {
            LegacyConversionEngine.appendCategoryDir(p, categoryDirs);
        }
        for (Path p : pngs) {
            LegacyConversionEngine.appendCategoryDir(p, categoryDirs);
        }
        if (categoryDirs.isEmpty()) {
            return;
        }
        PngToNbtConverter pngToNbtConverter = this.ensurePngConverter();
        if (pngToNbtConverter == null) {
            return;
        }
        Path buildingsRoot = cultureRoot.resolve("buildings");
        ArrayList<BuildingPlanWork> work = new ArrayList<BuildingPlanWork>();
        for (Map.Entry<String, Path> e : categoryDirs.entrySet()) {
            List<LegacyDataParser.BuildingWithVariants> group;
            String category = e.getKey();
            try {
                group = LegacyDataParser.scanBuildingDirectory(e.getValue(), category);
            }
            catch (IOException ex) {
                LegacyConversionEngine.logFailure("building scan (" + category + ")", e.getValue(), ex);
                continue;
            }
            for (LegacyDataParser.BuildingWithVariants building : group) {
                work.add(new BuildingPlanWork(category, building));
            }
        }
        if (work.isEmpty()) {
            return;
        }
        int heartbeatEvery = Math.max(1, work.size() / 10);
        int consecutiveFailures = 0;
        String lastFailureClass = null;
        int done = 0;
        for (BuildingPlanWork w : work) {
            Path planTargetDir = buildingsRoot.resolve(w.category());
            String failureClass = this.convertOneBuildingReturnFailure(addonRoot, culture, w.category(), w.building(), pngToNbtConverter, planTargetDir, planTargetDir, centralBuildings);
            if (failureClass != null) {
                if (failureClass.equals(lastFailureClass)) {
                    ++consecutiveFailures;
                } else {
                    consecutiveFailures = 1;
                    lastFailureClass = failureClass;
                }
                if (consecutiveFailures >= 10) {
                    LOGGER.error("Circuit breaker: {} consecutive {} failures converting {} buildings; refusing to continue. Check the custom pack for a corrupted blocklist or classpath mismatch.", new Object[]{consecutiveFailures, failureClass, culture});
                    return;
                }
            } else {
                consecutiveFailures = 0;
                lastFailureClass = null;
            }
            if (++done % heartbeatEvery != 0 || done >= work.size()) continue;
            LOGGER.info("Legacy auto-conversion [{}/{}] cultures/{} building plans", new Object[]{done, work.size(), culture});
        }
        this.report.recordUnmappedSpecialPoints(pngToNbtConverter.drainUnmappedSpecialPoints());
        Set<Integer> colours = pngToNbtConverter.drainUnmappedColours();
        for (int rgb : colours) {
            this.report.recordUnmappedColour(rgb, null, null, 0);
        }
    }

    private String convertOneBuildingReturnFailure(Path addonRoot, String culture, String category, LegacyDataParser.BuildingWithVariants building, PngToNbtConverter conv, Path nbtTargetDir, Path planTargetDir, Set<String> centralBuildings) {
        this.lastBuildingFailureClass = null;
        this.convertOneBuilding(addonRoot, culture, category, building, conv, nbtTargetDir, planTargetDir, centralBuildings);
        return this.lastBuildingFailureClass;
    }

    private void convertOneBuilding(Path addonRoot, String culture, String category, LegacyDataParser.BuildingWithVariants building, PngToNbtConverter conv, Path nbtTargetDir, Path planTargetDir, Set<String> centralBuildings) {
        String baseSanitised = LegacyJsonBuilder.sanitize(building.meta().baseName());
        if (building.variantPngs().isEmpty()) {
            LOGGER.warn("Building plan {} has no PNG variants, skipping", (Object)baseSanitised);
            this.report.recordSkipped(culture, LegacyConversionReport.Kind.BUILDING_PLAN, baseSanitised, "no PNG variants in scan");
            return;
        }
        LinkedHashMap<String, PngToNbtConverter.ConversionResult> conversionResults = new LinkedHashMap<String, PngToNbtConverter.ConversionResult>();
        LinkedHashMap<String, Integer> shippedVariantMaxLevel = new LinkedHashMap<String, Integer>();
        ArrayList<Path> planLevelWriteLog = new ArrayList<Path>();
        ArrayList<VariantDrop> drops = new ArrayList<VariantDrop>();
        try {
            Files.createDirectories(nbtTargetDir, new FileAttribute[0]);
        }
        catch (IOException ioe) {
            this.logFailure(culture, LegacyConversionReport.Kind.BUILDING_PLAN, "building plan " + baseSanitised + " (category=" + category + ")", Path.of(building.meta().baseName(), new String[0]), ioe);
            this.lastBuildingFailureClass = ioe.getClass().getName();
            return;
        }
        for (Map.Entry<String, List<Path>> ve : building.variantPngs().entrySet()) {
            int maxOk;
            String variantKey = ve.getKey();
            String variant = variantKey.toLowerCase(Locale.ROOT);
            List<Path> pngs = ve.getValue();
            LegacyDataParser.BuildingMeta varMeta = building.metaForVariant(variantKey);
            int levelsForVariant = pngs.size();
            ArrayList<Path> variantNbts = new ArrayList<Path>();
            try {
                maxOk = this.convertVariantLevels(conv, nbtTargetDir, baseSanitised, variantKey, variant, pngs, varMeta, conversionResults, variantNbts, drops);
            }
            catch (RuntimeException variantCrash) {
                LegacyConversionEngine.rollback(variantNbts);
                drops.add(VariantDrop.crash(variantKey, variantCrash));
                this.lastBuildingFailureClass = variantCrash.getClass().getName();
                if (!this.mode.isStrict()) continue;
                LegacyConversionEngine.rollback(planLevelWriteLog);
                this.emitPlanSetBlocked(culture, baseSanitised, category, drops);
                return;
            }
            if (maxOk >= 0) {
                shippedVariantMaxLevel.put(variantKey, maxOk);
                planLevelWriteLog.addAll(variantNbts);
            } else if (!variantNbts.isEmpty()) {
                LOGGER.warn("convertVariantLevels returned -1 but left {} NBT(s) on disk; forcing cleanup: {}", (Object)variantNbts.size(), variantNbts);
                LegacyConversionEngine.rollback(variantNbts);
            }
            if (!this.mode.isStrict() || maxOk >= 0 && maxOk >= levelsForVariant - 1) continue;
            LegacyConversionEngine.rollback(planLevelWriteLog);
            this.emitPlanSetBlocked(culture, baseSanitised, category, drops);
            return;
        }
        if (shippedVariantMaxLevel.isEmpty()) {
            LegacyConversionEngine.rollback(planLevelWriteLog);
            this.recordPlanDropped(culture, baseSanitised, drops);
            return;
        }
        LegacyDataParser.BuildingWithVariants shipped = LegacyConversionEngine.trimBuildingToShipped(building, shippedVariantMaxLevel);
        try {
            String planId = LegacyIdCanonicaliser.buildingPlanRefId(baseSanitised);
            Path jsonTarget = planTargetDir.resolve(planId + ".json");
            if (!this.checkConverterSkipOrRollback(jsonTarget, planLevelWriteLog, culture, LegacyConversionReport.Kind.BUILDING_PLAN)) {
                return;
            }
            Map<String, Object> json = LegacyJsonBuilder.buildBuildingPlan(shipped, culture, this.items, conversionResults, centralBuildings);
            this.writeJson(jsonTarget, json);
            Path writeRoot = this.writeRootOrSource(addonRoot);
            this.manifest.recordFile(writeRoot, jsonTarget, null);
            this.report.recordConverted(culture, LegacyConversionReport.Kind.BUILDING_PLAN);
            this.recordPlanDroppedEntries(culture, baseSanitised, drops);
        }
        catch (Exception ex) {
            LegacyConversionEngine.rollback(planLevelWriteLog);
            this.logFailure(culture, LegacyConversionReport.Kind.BUILDING_PLAN, "building plan " + baseSanitised + " (category=" + category + ")", Path.of(building.meta().baseName(), new String[0]), ex);
            this.lastBuildingFailureClass = ex.getClass().getName();
        }
    }

    private int convertVariantLevels(PngToNbtConverter conv, Path nbtTargetDir, String baseSanitised, String variantKey, String variant, List<Path> pngs, LegacyDataParser.BuildingMeta varMeta, Map<String, PngToNbtConverter.ConversionResult> conversionResults, List<Path> variantNbts, List<VariantDrop> drops) {
        int highestShippedLevel = -1;
        int level = 0;
        while (level < pngs.size()) {
            Path png = pngs.get(level);
            String outputName = baseSanitised + "_" + variant + "_" + level;
            Path nbtPath = nbtTargetDir.resolve(outputName + ".nbt");
            try {
                PngToNbtConverter.ConversionResult r = conv.convert(png, varMeta.width(), varMeta.length(), nbtPath, outputName);
                conversionResults.put(outputName, r);
                variantNbts.add(nbtPath);
                highestShippedLevel = level++;
            }
            catch (IOException | RuntimeException ex) {
                try {
                    Files.deleteIfExists(nbtPath);
                }
                catch (IOException delIo) {
                    LOGGER.warn("Failed to delete partial NBT {}: {}", (Object)nbtPath, (Object)delIo.getMessage());
                }
                drops.add(VariantDrop.levelFailed(variantKey, level, png, ex));
                return highestShippedLevel;
            }
        }
        return highestShippedLevel;
    }

    private static LegacyDataParser.BuildingWithVariants trimBuildingToShipped(LegacyDataParser.BuildingWithVariants original, Map<String, Integer> shippedVariantMaxLevel) {
        LinkedHashMap<String, List<Path>> trimmedPngs = new LinkedHashMap<String, List<Path>>();
        LinkedHashMap<String, LegacyDataParser.BuildingMeta> trimmedMetas = new LinkedHashMap<String, LegacyDataParser.BuildingMeta>();
        for (Map.Entry<String, List<Path>> e : original.variantPngs().entrySet()) {
            Integer maxOk = shippedVariantMaxLevel.get(e.getKey());
            if (maxOk == null) continue;
            List<Path> full = e.getValue();
            trimmedPngs.put(e.getKey(), new ArrayList<Path>(full.subList(0, maxOk + 1)));
            LegacyDataParser.BuildingMeta vm = original.variantMetas().get(e.getKey());
            if (vm == null) continue;
            trimmedMetas.put(e.getKey(), vm);
        }
        return new LegacyDataParser.BuildingWithVariants(original.meta(), trimmedPngs, trimmedMetas);
    }

    private void emitPlanSetBlocked(String culture, String baseSanitised, String category, List<VariantDrop> drops) {
        this.lastBuildingFailureClass = "PlanSetRollback";
        String summary = drops.isEmpty() ? "unknown failure" : drops.stream().map(d -> d.variantKey() + (String)(d.level() >= 0 ? "/level " + d.level() : "") + " (" + d.reason() + ")").collect(Collectors.joining("; "));
        String hint = "Fix or replace the PNG files listed. All variants and levels of a plan must succeed in CONVERT mode.";
        String reportPath = "buildings/" + (String)(category == null ? "" : category + "/") + baseSanitised;
        this.recordAmbiguity(culture, LegacyConversionReport.Kind.BUILDING_PLAN, reportPath, "plan '" + baseSanitised + "' had one or more variant/level failures: " + summary, hint);
    }

    private void recordPlanDropped(String culture, String baseSanitised, List<VariantDrop> drops) {
        if (this.mode.isStrict()) {
            this.emitPlanSetBlocked(culture, baseSanitised, "(no variants)", drops);
            return;
        }
        this.report.recordSkipped(culture, LegacyConversionReport.Kind.BUILDING_PLAN, "buildings/" + baseSanitised, "no variant had a usable level 0");
    }

    private void recordPlanDroppedEntries(String culture, String baseSanitised, List<VariantDrop> drops) {
        if (this.mode.isStrict()) {
            return;
        }
        for (VariantDrop d : drops) {
            String path = "buildings/" + baseSanitised + " (variant " + d.variantKey() + (String)(d.level() >= 0 ? ", level " + d.level() : "") + ")";
            this.report.recordSkipped(culture, LegacyConversionReport.Kind.BUILDING_PLAN, path, d.reason());
        }
    }

    private boolean checkConverterSkipOrRollback(Path jsonTarget, List<Path> writtenNbts, String culture, LegacyConversionReport.Kind kind) {
        PreservationPolicy.Decision d = PreservationPolicy.checkConverterSkip(jsonTarget);
        if (d.overwrite()) {
            return true;
        }
        LOGGER.warn("Preserving {} ({}), building NBTs rolled back; source PNG/TXT left in place", (Object)jsonTarget, (Object)d.reason());
        LegacyConversionEngine.rollback(writtenNbts);
        this.report.recordPreserved(culture, kind, jsonTarget.toString(), d.reason());
        return false;
    }

    private static void rollback(List<Path> writtenNbts) {
        for (Path p : writtenNbts) {
            try {
                Files.deleteIfExists(p);
            }
            catch (IOException io) {
                LOGGER.warn("Failed to rollback {}: {}", (Object)p, (Object)io.getMessage());
            }
        }
    }

    private static void appendCategoryDir(Path source, TreeMap<String, Path> dirs) {
        Path grand;
        Path parent = source.getParent();
        if (parent == null) {
            return;
        }
        Path buildings = parent;
        while (!(buildings == null || (grand = buildings.getParent()) != null && "buildings".equalsIgnoreCase(grand.getFileName().toString()))) {
            buildings = grand;
        }
        if (buildings == null) {
            buildings = parent;
        }
        String category = buildings.getFileName().toString().toLowerCase(Locale.ROOT);
        dirs.putIfAbsent(category, buildings);
    }

    /*
     * Enabled aggressive block sorting
     * Enabled unnecessary exception pruning
     * Enabled aggressive exception aggregation
     */
    private PngToNbtConverter ensurePngConverter() {
        if (this.pngConverter != null) {
            return this.pngConverter;
        }
        try (InputStream in = PngToNbtConverter.class.getResourceAsStream("/millenaire/legacy/blocklist.txt");){
            if (in == null) {
                LOGGER.error("/millenaire/legacy/blocklist.txt missing; skipping building conversion for this pack.");
                PngToNbtConverter pngToNbtConverter2 = null;
                return pngToNbtConverter2;
            }
            PngToNbtConverter c = new PngToNbtConverter(this.mode);
            c.loadBlocklist(in);
            this.pngConverter = c;
            PngToNbtConverter pngToNbtConverter = c;
            return pngToNbtConverter;
        }
        catch (IOException e) {
            LOGGER.error("Failed to load blocklist", (Throwable)e);
            return null;
        }
    }

    private void convertVillagerTypes(Path addonRoot, String culture, LegacyLayoutDetector.Detection detection) {
        Path writeRoot = this.writeRootOrSource(addonRoot);
        Path villagersRoot = writeRoot.resolve("cultures").resolve(culture).resolve("villagers");
        HashMap<String, Path> firstByLogicalId = new HashMap<String, Path>();
        for (Path source : LegacyConversionEngine.onlyCulture(detection.villagerTxts(), culture)) {
            try {
                String category = LegacyConversionEngine.legacySubCategory(source, "villagers");
                LegacyDataParser.VillagerMeta meta = LegacyDataParser.parseVillagerTxt(source, category);
                this.checkUnknownKeys(culture, addonRoot, source, LegacyKeySchemas.VILLAGER, "native_name");
                this.checkVillagerRefs(culture, addonRoot, source);
                this.checkVillagerValues(culture, addonRoot, source);
                String id = LegacyIdCanonicaliser.villagerTypeRefId(meta.id());
                String targetCategory = category.isEmpty() ? "normalvillagers" : category;
                Path target = villagersRoot.resolve(targetCategory).resolve(id + ".json");
                Path previous = firstByLogicalId.putIfAbsent(id, source);
                if (previous != null) {
                    this.recordAmbiguity(culture, LegacyConversionReport.Kind.VILLAGER_TYPE, LegacyConversionEngine.relPathFor(addonRoot, source), "villager id '" + culture + "/" + id + "' already produced from " + LegacyConversionEngine.relPathFor(addonRoot, previous) + " \u2014 runtime cross-folder logical-id collision is fatal", "Rename or merge one of the two TXT files so each id is unique within the culture (e.g. " + id + "_lone for the lonevillagers/ variant).");
                    continue;
                }
                if (!this.maybeSkipConverterPinned(target, LegacyConversionReport.Kind.VILLAGER_TYPE, culture, source)) continue;
                Map<String, Object> json = LegacyJsonBuilder.buildVillagerType(meta, culture, this.items);
                this.writeAndRecord(addonRoot, target, source, json, LegacyConversionReport.Kind.VILLAGER_TYPE, culture);
            }
            catch (Exception e) {
                this.logFailure(culture, LegacyConversionReport.Kind.VILLAGER_TYPE, "villagers", source, e);
            }
        }
    }

    private void checkUnknownKeys(String culture, Path addonRoot, Path source, Set<String> whitelist, String ... missingRequired) {
        Map<String, List<String>> map;
        try {
            map = LegacyDataParser.parseKeyValues(source);
        }
        catch (IOException io) {
            return;
        }
        String relPath = LegacyConversionEngine.relPathFor(addonRoot, source);
        for (String key : map.keySet()) {
            if (whitelist.contains(key) || LegacyKeySchemas.isOutOfScope(key)) continue;
            this.report.recordUnknownKey(culture, relPath, key);
        }
        if (missingRequired != null) {
            for (String required : missingRequired) {
                if (required == null || map.containsKey(required)) continue;
                this.report.recordMissingRequiredKey(culture, relPath, required);
            }
        }
        this.checkDuplicateMultiValues(culture, relPath, map);
    }

    private void checkDuplicateMultiValues(String culture, String relPath, Map<String, List<String>> map) {
        Set<String> itemBearing = Set.of("requiredgood", "collectgood", "bringbackhomegood", "itemneeded", "merchantstock", "startinginv");
        for (Map.Entry<String, List<String>> e : map.entrySet()) {
            String key = e.getKey();
            List<String> values = e.getValue();
            if (values.size() < 2) continue;
            HashSet<String> seen = new HashSet<String>();
            for (String v : values) {
                String token;
                if (key.equals("goal")) {
                    token = v.trim().toLowerCase(Locale.ROOT);
                } else {
                    if (!itemBearing.contains(key)) continue;
                    token = v.split(",", 2)[0].trim().toLowerCase(Locale.ROOT);
                }
                if (token.isEmpty() || seen.add(token)) continue;
                this.report.recordDuplicate(culture, relPath, key, token);
            }
        }
    }

    private void checkUnknownBuildingKeys(String culture, Path addonRoot, Path source) {
        Map<String, List<String>> map;
        try {
            map = LegacyDataParser.parseKeyValues(source);
        }
        catch (IOException io) {
            return;
        }
        String relPath = LegacyConversionEngine.relPathFor(addonRoot, source);
        for (String key : map.keySet()) {
            String bare = LegacyConversionEngine.stripBuildingPrefix(key);
            if (LegacyKeySchemas.BUILDING_BARE.contains(bare) || LegacyKeySchemas.isOutOfScope(bare)) continue;
            this.report.recordUnknownKey(culture, relPath, key);
        }
    }

    private static String stripBuildingPrefix(String key) {
        return BUILDING_PREFIX.matcher(key).replaceFirst("");
    }

    private void checkUnknownQuestKeys(Path addonRoot, Path source) {
        List<String> lines;
        String relPath = LegacyConversionEngine.relPathFor(addonRoot, source);
        try {
            lines = Files.readAllLines(source, StandardCharsets.UTF_8);
        }
        catch (IOException io) {
            return;
        }
        for (String raw : lines) {
            String key;
            int colon;
            String line = raw.trim();
            if (line.isEmpty() || line.startsWith("//") || (colon = line.indexOf(58)) <= 0 || LegacyKeySchemas.QUEST.contains(key = line.substring(0, colon).trim().toLowerCase(Locale.ROOT)) || LegacyKeySchemas.isOutOfScope(key)) continue;
            this.report.recordUnknownKey(null, relPath, key);
        }
    }

    private void checkVillageRefs(String culture, Path addonRoot, Path source) {
        Map<String, List<String>> map;
        if (this.currentCultureRefs == null) {
            return;
        }
        try {
            map = LegacyDataParser.parseKeyValues(source);
        }
        catch (IOException io) {
            return;
        }
        String relPath = LegacyConversionEngine.relPathFor(addonRoot, source);
        Set<String> buildingKeys = Set.of("centre", "start", "core", "secondary", "player", "customcentre", "never");
        for (Map.Entry<String, List<String>> e : map.entrySet()) {
            String name;
            String key = e.getKey();
            if (buildingKeys.contains(key)) {
                for (String value : e.getValue()) {
                    name = value.trim().toLowerCase(Locale.ROOT);
                    if (name.isEmpty() || this.currentCultureRefs.buildingPlans.contains(name)) continue;
                    this.report.recordBrokenRef(culture, relPath, key, value, "building_plan");
                }
                continue;
            }
            if (!key.equals("namelist")) continue;
            for (String value : e.getValue()) {
                name = value.trim().toLowerCase(Locale.ROOT);
                if (name.isEmpty() || this.currentCultureRefs.namelists.contains(name)) continue;
                this.report.recordBrokenRef(culture, relPath, key, value, "namelist");
            }
        }
    }

    private void checkVillagerRefs(String culture, Path addonRoot, Path source) {
        Map<String, List<String>> map;
        if (this.currentCultureRefs == null) {
            return;
        }
        try {
            map = LegacyDataParser.parseKeyValues(source);
        }
        catch (IOException io) {
            return;
        }
        String relPath = LegacyConversionEngine.relPathFor(addonRoot, source);
        for (Map.Entry<String, List<String>> e : map.entrySet()) {
            String key = e.getKey();
            if (!key.equals("familynamelist") && !key.equals("firstnamelist") && !key.equals("namelist")) continue;
            for (String value : e.getValue()) {
                String name = value.trim().toLowerCase(Locale.ROOT);
                if (name.isEmpty() || this.currentCultureRefs.namelists.contains(name)) continue;
                this.report.recordBrokenRef(culture, relPath, key, value, "namelist");
            }
        }
    }

    private void checkVillagerValues(String culture, Path addonRoot, Path source) {
        Map<String, List<String>> map;
        try {
            map = LegacyDataParser.parseKeyValues(source);
        }
        catch (IOException io) {
            return;
        }
        ValueValidityChecker.checkVillager(culture, LegacyConversionEngine.relPathFor(addonRoot, source), map, this.report);
    }

    private void checkVillageValues(String culture, Path addonRoot, Path source, boolean lone) {
        Map<String, List<String>> map;
        try {
            map = LegacyDataParser.parseKeyValues(source);
        }
        catch (IOException io) {
            return;
        }
        String relPath = LegacyConversionEngine.relPathFor(addonRoot, source);
        if (lone) {
            ValueValidityChecker.checkLoneBuilding(culture, relPath, map, this.report);
        } else {
            ValueValidityChecker.checkVillage(culture, relPath, map, this.report);
        }
    }

    private void checkBuildingValues(String culture, Path addonRoot, Path source) {
        Map<String, List<String>> map;
        try {
            map = LegacyDataParser.parseKeyValues(source);
        }
        catch (IOException io) {
            return;
        }
        LinkedHashMap<String, List<String>> bareMap = new LinkedHashMap<String, List<String>>();
        for (Map.Entry<String, List<String>> e : map.entrySet()) {
            String bare = LegacyConversionEngine.stripBuildingPrefix(e.getKey());
            bareMap.computeIfAbsent(bare, k -> new ArrayList()).addAll((Collection)e.getValue());
        }
        ValueValidityChecker.checkBuildingBare(culture, LegacyConversionEngine.relPathFor(addonRoot, source), bareMap, this.report);
    }

    private void checkSuspiciousPriorities(String culture, Path addonRoot, Path source) {
        Map<String, List<String>> map;
        try {
            map = LegacyDataParser.parseKeyValues(source);
        }
        catch (IOException io) {
            return;
        }
        String relPath = LegacyConversionEngine.relPathFor(addonRoot, source);
        Integer initialPriority = LegacyConversionEngine.parseFirstInt(map.get("initial.priority"));
        if (initialPriority != null && initialPriority == 0) {
            this.report.recordSuspiciousPriority(culture, relPath, 0, 0, "zero_initial");
        }
        TreeMap<Integer, Integer> upgradePriorities = new TreeMap<Integer, Integer>();
        Pattern pat = Pattern.compile("^upgrade(\\d+)\\.priority$");
        for (Map.Entry<String, List<String>> e : map.entrySet()) {
            Matcher m = pat.matcher(e.getKey());
            if (!m.matches()) continue;
            int level = Integer.parseInt(m.group(1));
            Integer p = LegacyConversionEngine.parseFirstInt(e.getValue());
            if (p == null) continue;
            upgradePriorities.put(level, p);
        }
        if (initialPriority != null && initialPriority > 0 && upgradePriorities.size() >= 2) {
            int referencePriority = initialPriority;
            boolean sawValley = false;
            int valleyLevel = -1;
            int valleyPriority = -1;
            for (Map.Entry entry : upgradePriorities.entrySet()) {
                int level = (Integer)entry.getKey();
                int prio = (Integer)entry.getValue();
                if (!sawValley && prio > 0 && prio < referencePriority) {
                    sawValley = true;
                    valleyLevel = level;
                    valleyPriority = prio;
                    continue;
                }
                if (!sawValley || prio <= referencePriority) continue;
                this.report.recordSuspiciousPriority(culture, relPath, valleyLevel, valleyPriority, "valley_then_spike (initial=" + referencePriority + ", upgrade" + valleyLevel + "=" + valleyPriority + ", upgrade" + level + "=" + prio + ")");
                break;
            }
        }
    }

    private static Integer parseFirstInt(List<String> values) {
        if (values == null || values.isEmpty()) {
            return null;
        }
        try {
            return Integer.parseInt(values.get(0).trim());
        }
        catch (NumberFormatException nfe) {
            return null;
        }
    }

    private void checkShopEmptyAndDupes(String culture, Path addonRoot, Path source) {
        Map<String, List<String>> map;
        try {
            map = LegacyDataParser.parseKeyValues(source);
        }
        catch (IOException io) {
            return;
        }
        String relPath = LegacyConversionEngine.relPathFor(addonRoot, source);
        LinkedHashMap flatTokens = new LinkedHashMap();
        for (Map.Entry<String, List<String>> e : map.entrySet()) {
            String key = e.getKey();
            if (!LegacyKeySchemas.SHOP.contains(key)) continue;
            ArrayList<String> arrayList = new ArrayList<String>();
            for (String value : (List)e.getValue()) {
                for (String t : value.split(",")) {
                    String trimmed = t.trim();
                    if (trimmed.isEmpty()) continue;
                    arrayList.add(trimmed);
                }
            }
            flatTokens.put(key, arrayList);
        }
        HashSet<String> deliveryLower = new HashSet<String>();
        for (String t : flatTokens.getOrDefault("deliverto", List.of())) {
            deliveryLower.add(t.toLowerCase(Locale.ROOT));
        }
        HashSet<String> optionalLower = new HashSet<String>();
        for (String string : flatTokens.getOrDefault("buysoptional", List.of())) {
            optionalLower.add(string.toLowerCase(Locale.ROOT));
        }
        for (Map.Entry entry : flatTokens.entrySet()) {
            String key = (String)entry.getKey();
            List tokens = (List)entry.getValue();
            if (tokens.isEmpty()) {
                this.report.recordEmptyList(culture, relPath, key);
                continue;
            }
            HashSet<String> seen = new HashSet<String>();
            for (String t : tokens) {
                String low = t.toLowerCase(Locale.ROOT);
                if (seen.add(low) || key.equals("buysoptional") && deliveryLower.contains(low) || key.equals("deliverto") && optionalLower.contains(low)) continue;
                this.report.recordDuplicate(culture, relPath, key, t);
            }
        }
    }

    private void checkTradedGoodsRows(String culture, Path addonRoot, Path source) {
        List<String> lines;
        try {
            lines = Files.readAllLines(source, StandardCharsets.UTF_8);
        }
        catch (IOException io) {
            return;
        }
        String relPath = LegacyConversionEngine.relPathFor(addonRoot, source);
        for (int i = 0; i < lines.size(); ++i) {
            String raw = lines.get(i);
            String line = raw.trim();
            if (line.isEmpty() || line.startsWith("//")) continue;
            String[] parts = line.split(",");
            if (parts.length < 3) {
                this.report.recordMalformedRow(culture, relPath, i + 1, "too few columns (" + parts.length + ", need at least 3)");
                continue;
            }
            String item = parts[0].trim();
            if (!item.isEmpty()) continue;
            this.report.recordMalformedRow(culture, relPath, i + 1, "empty item in column 0");
        }
    }

    private static String relPathFor(Path addonRoot, Path source) {
        try {
            return addonRoot.relativize(source).toString();
        }
        catch (IllegalArgumentException ex) {
            return source.toString();
        }
    }

    private void convertVillageTypes(Path addonRoot, String culture, LegacyLayoutDetector.Detection detection, List<LegacyDataParser.BuildingWithVariants> buildingIndex) {
        Map<String, Object> toWrite;
        PreservationPolicy.Decision d;
        Map<String, Object> json;
        Path target;
        String id;
        LegacyDataParser.VillageTypeMeta vt;
        Path writeRoot = this.writeRootOrSource(addonRoot);
        Path targetDir = writeRoot.resolve("cultures").resolve(culture).resolve("villages");
        BiomeMapper biomeMapperForCustomCulture = this.biomes;
        for (Path source : LegacyConversionEngine.onlyCulture(detection.villageTxts(), culture)) {
            try {
                vt = LegacyDataParser.parseVillageTypeTxt(source);
                this.checkUnknownKeys(culture, addonRoot, source, LegacyKeySchemas.VILLAGE, "name", "centre");
                this.checkVillageRefs(culture, addonRoot, source);
                this.checkVillageValues(culture, addonRoot, source, false);
                id = LegacyIdCanonicaliser.villageTypeRefId(vt.id());
                target = targetDir.resolve(id + ".json");
                json = LegacyJsonBuilder.buildVillageType(vt, culture, this.items, buildingIndex, biomeMapperForCustomCulture);
                d = PreservationPolicy.checkVillageType(target, json);
                if (!d.overwrite()) {
                    this.noteConverterPinned(target, LegacyConversionReport.Kind.VILLAGE_TYPE, culture, source, d.reason());
                    continue;
                }
                toWrite = d.mergedJson() != null ? d.mergedJson() : json;
                this.writeAndRecord(addonRoot, target, source, toWrite, LegacyConversionReport.Kind.VILLAGE_TYPE, culture);
            }
            catch (Exception e) {
                this.logFailure(culture, LegacyConversionReport.Kind.VILLAGE_TYPE, "villages", source, e);
            }
        }
        for (Path source : LegacyConversionEngine.onlyCulture(detection.loneTxts(), culture)) {
            try {
                vt = LegacyDataParser.parseVillageTypeTxt(source, true);
                this.checkUnknownKeys(culture, addonRoot, source, LegacyKeySchemas.LONEBUILDING, "name", "centre");
                this.checkVillageRefs(culture, addonRoot, source);
                this.checkVillageValues(culture, addonRoot, source, true);
                id = LegacyIdCanonicaliser.villageTypeRefId(vt.id());
                target = targetDir.resolve(id + ".json");
                json = LegacyJsonBuilder.buildVillageType(vt, culture, this.items, buildingIndex, biomeMapperForCustomCulture);
                d = PreservationPolicy.checkVillageType(target, json);
                if (!d.overwrite()) {
                    this.noteConverterPinned(target, LegacyConversionReport.Kind.VILLAGE_TYPE, culture, source, d.reason());
                    continue;
                }
                toWrite = d.mergedJson() != null ? d.mergedJson() : json;
                this.writeAndRecord(addonRoot, target, source, toWrite, LegacyConversionReport.Kind.VILLAGE_TYPE, culture);
            }
            catch (Exception e) {
                this.logFailure(culture, LegacyConversionReport.Kind.VILLAGE_TYPE, "villages (lone)", source, e);
            }
        }
    }

    private void convertShops(Path addonRoot, String culture, LegacyLayoutDetector.Detection detection) {
        Path writeRoot = this.writeRootOrSource(addonRoot);
        Path targetDir = writeRoot.resolve("cultures").resolve(culture).resolve("shops");
        for (Path source : LegacyConversionEngine.onlyCulture(detection.shopTxts(), culture)) {
            try {
                LegacyDataParser.ShopMeta shop = LegacyDataParser.parseShopTxt(source);
                this.checkUnknownKeys(culture, addonRoot, source, LegacyKeySchemas.SHOP, new String[0]);
                this.checkShopEmptyAndDupes(culture, addonRoot, source);
                String id = LegacyIdCanonicaliser.shopId(culture, shop.id());
                Path target = targetDir.resolve(id + ".json");
                if (!this.maybeSkipConverterPinned(target, LegacyConversionReport.Kind.SHOP, culture, source)) continue;
                Map<String, Object> json = LegacyJsonBuilder.buildShop(shop, culture, this.items);
                this.writeAndRecord(addonRoot, target, source, json, LegacyConversionReport.Kind.SHOP, culture);
            }
            catch (Exception e) {
                this.logFailure(culture, LegacyConversionReport.Kind.SHOP, "shop", source, e);
            }
        }
    }

    private void convertTradedGoods(Path addonRoot, String culture, Path cultureRoot, LegacyLayoutDetector.Detection detection) {
        for (Path source : LegacyConversionEngine.onlyCulture(detection.tradedGoodsTxts(), culture)) {
            try {
                this.checkTradedGoodsRows(culture, addonRoot, source);
                List<LegacyDataParser.TradedGoodMeta> goods = LegacyDataParser.parseTradedGoodsTxt(source);
                Path target = cultureRoot.resolve("traded_goods.json");
                if (!this.maybeSkipConverterPinned(target, LegacyConversionReport.Kind.TRADED_GOOD, culture, source)) continue;
                Map<String, Object> json = LegacyJsonBuilder.buildTradedGoods(goods, culture, this.items);
                this.writeAndRecord(addonRoot, target, source, json, LegacyConversionReport.Kind.TRADED_GOOD, culture);
            }
            catch (Exception e) {
                this.logFailure(culture, LegacyConversionReport.Kind.TRADED_GOOD, "traded_goods", source, e);
            }
        }
    }

    private void convertWalls(Path addonRoot, String culture, LegacyLayoutDetector.Detection detection) {
        List<Path> sources = LegacyConversionEngine.onlyCulture(detection.wallTxts(), culture);
        if (sources.isEmpty()) {
            return;
        }
        Path wallTypeDir = this.writeRootOrSource(addonRoot).resolve("wall_type").resolve(culture);
        for (Path source : sources) {
            String id = LegacyIdCanonicaliser.buildingPlanRefId(LegacyConversionEngine.stripTxt(source.getFileName().toString()));
            Path target = wallTypeDir.resolve(id + ".json");
            if (!this.maybeSkipConverterPinned(target, LegacyConversionReport.Kind.WALL_TYPE, culture, source)) continue;
            try {
                Map<String, List<String>> kv = LegacyDataParser.parseKeyValues(source);
                Map<String, Object> json = LegacyJsonBuilder.buildWallType(culture, id, kv);
                if (json == null) {
                    LOGGER.warn("Wall type {}/{}: no gateway plan-set (village_wall_gate) \u2014 skipped (the wall loader requires one).", (Object)culture, (Object)id);
                    this.report.recordSkipped(culture, LegacyConversionReport.Kind.WALL_TYPE, id, "no gateway plan-set (village_wall_gate)");
                    continue;
                }
                this.warnMissingWallPlanRefs(culture, id, json);
                this.writeAndRecord(addonRoot, target, source, json, LegacyConversionReport.Kind.WALL_TYPE, culture);
            }
            catch (Exception e) {
                this.logFailure(culture, LegacyConversionReport.Kind.WALL_TYPE, "wall type " + id, source, e);
            }
        }
    }

    private void warnMissingWallPlanRefs(String culture, String id, Map<String, Object> json) {
        if (this.currentCultureRefs == null) {
            return;
        }
        String prefix = "millenaire:" + culture + "/";
        for (Map.Entry<String, Object> e : json.entrySet()) {
            String plan;
            String ref;
            if (!e.getKey().endsWith("_plan_set") || !(ref = String.valueOf(e.getValue())).startsWith(prefix) || this.currentCultureRefs.buildingPlans.contains(plan = ref.substring(prefix.length()))) continue;
            LOGGER.warn("Wall type {}/{} references plan '{}' not found among converted buildings/walls/ plans \u2014 that piece won't build.", new Object[]{culture, id, plan});
        }
    }

    private void convertCultureStub(Path addonRoot, String culture, Path cultureRoot, LegacyLayoutDetector.Detection detection) {
        Path target = cultureRoot.resolve("culture.json");
        Path writeRoot = this.writeRootOrSource(addonRoot);
        if (Files.isRegularFile(target, new LinkOption[0])) {
            return;
        }
        List<Path> sources = LegacyConversionEngine.onlyCulture(detection.cultureTxts(), culture);
        if (sources.isEmpty()) {
            LOGGER.warn("No culture definition found for '{}'. Generated minimal default.", (Object)culture);
            LinkedHashMap<String, Object> stub = new LinkedHashMap<String, Object>();
            stub.put("culture_id", culture);
            String fromLang = LegacyConversionEngine.findDisplayNameFromLanguages(addonRoot, culture);
            stub.put("display_name", fromLang != null ? fromLang : LegacyConversionEngine.titleCase(culture));
            try {
                this.writeJson(target, stub);
                this.manifest.recordFile(writeRoot, target, null);
                this.report.recordConverted(culture, LegacyConversionReport.Kind.CULTURE);
            }
            catch (IOException e) {
                LegacyConversionEngine.logFailure("culture (auto-stub)", target, e);
            }
            return;
        }
        Path source = sources.getFirst();
        try {
            Map<String, List<String>> parsed = LegacyDataParser.parseKeyValues(source);
            Map<String, Object> json = LegacyJsonBuilder.buildCultureStub(culture, parsed);
            if (!json.containsKey("display_name")) {
                String fromLang = LegacyConversionEngine.findDisplayNameFromLanguages(addonRoot, culture);
                json.put("display_name", fromLang != null ? fromLang : LegacyConversionEngine.titleCase(culture));
            }
            this.writeAndRecord(addonRoot, target, source, json, LegacyConversionReport.Kind.CULTURE, culture);
        }
        catch (Exception e) {
            this.logFailure(culture, LegacyConversionReport.Kind.CULTURE, "culture", source, e);
        }
    }

    /*
     * Enabled aggressive block sorting
     * Enabled unnecessary exception pruning
     * Enabled aggressive exception aggregation
     */
    private static String findDisplayNameFromLanguages(Path addonRoot, String culture) {
        Path languages = addonRoot.resolve("languages");
        if (!Files.isDirectory(languages, new LinkOption[0])) {
            return null;
        }
        if (!CultureIdPolicy.PATTERN.matcher(culture).matches()) {
            return null;
        }
        String filename = culture + "_strings.txt";
        String key = "culture." + culture;
        Path en = languages.resolve("en").resolve(filename);
        String found = LegacyConversionEngine.readCultureKey(en, key);
        if (found != null) {
            return found;
        }
        try (Stream<Path> locales = Files.list(languages);){
            Path locale;
            String value;
            List<Path> sorted = locales.filter(x$0 -> Files.isDirectory(x$0, new LinkOption[0])).filter(p -> !"en".equals(p.getFileName().toString())).sorted(Comparator.comparing(p -> p.getFileName().toString())).toList();
            Iterator<Path> iterator = sorted.iterator();
            do {
                if (!iterator.hasNext()) return null;
            } while ((value = LegacyConversionEngine.readCultureKey((locale = iterator.next()).resolve(filename), key)) == null);
            String string = value;
            return string;
        }
        catch (IOException iOException) {
            // empty catch block
        }
        return null;
    }

    private static String readCultureKey(Path file, String key) {
        Charset[] encodings;
        if (!Files.isRegularFile(file, new LinkOption[0])) {
            return null;
        }
        if (!ContentDirectoryManager.checkSize(file, 512000L)) {
            return null;
        }
        for (Charset cs : encodings = new Charset[]{StandardCharsets.UTF_8, StandardCharsets.ISO_8859_1}) {
            try {
                List<String> lines = Files.readAllLines(file, cs);
                String value = LegacyConversionEngine.scanLinesForKey(lines, key);
                if (value != null) {
                    return value;
                }
                return null;
            }
            catch (MalformedInputException lines) {
            }
            catch (IOException ignored) {
                return null;
            }
        }
        return null;
    }

    private static String scanLinesForKey(List<String> lines, String key) {
        boolean first = true;
        Iterator<String> iterator = lines.iterator();
        while (iterator.hasNext()) {
            String value;
            int eq;
            String raw;
            String line = raw = iterator.next();
            if (first) {
                first = false;
                if (!line.isEmpty() && line.charAt(0) == '\ufeff') {
                    line = line.substring(1);
                }
            }
            if (!line.startsWith(key) || (eq = line.indexOf(61)) <= 0 || !line.substring(0, eq).trim().equals(key) || (value = line.substring(eq + 1).trim()).isEmpty()) continue;
            return value;
        }
        return null;
    }

    private void convertGatheringTypes(Path addonRoot, LegacyLayoutDetector.Detection detection) {
        Path writeRoot = this.writeRootOrSource(addonRoot);
        Path targetDir = writeRoot.resolve("gathering_type");
        LinkedHashMap<Path, Path> targetFirstSource = new LinkedHashMap<Path, Path>();
        for (Path source : detection.gatheringTxts()) {
            try {
                Map<String, List<String>> config = LegacyDataParser.parseGatheringTypeTxt(source);
                String category = LegacyConversionEngine.extractGoalCategory(source);
                Map<String, Object> json = LegacyJsonBuilder.buildGatheringType(category, config, null, this.items);
                if (json == null) {
                    this.report.recordSkipped(null, LegacyConversionReport.Kind.GATHERING_TYPE, source.toString(), "no valid output after item-mapping");
                    continue;
                }
                String baseName = LegacyConversionEngine.stripTxt(source.getFileName().toString());
                Path target = targetDir.resolve(baseName + ".json");
                Path previous = targetFirstSource.putIfAbsent(target, source);
                if (previous != null) {
                    LOGGER.warn("gathering-type name collision: '{}' would overwrite the output of '{}' (same target {}); skipping the second entry", new Object[]{source, previous, target});
                    this.report.recordSkipped(null, LegacyConversionReport.Kind.GATHERING_TYPE, source.toString(), "name collides with " + String.valueOf(previous));
                    continue;
                }
                if (!this.maybeSkipConverterPinned(target, LegacyConversionReport.Kind.GATHERING_TYPE, null, source)) continue;
                this.writeAndRecord(addonRoot, target, source, json, LegacyConversionReport.Kind.GATHERING_TYPE, null);
            }
            catch (Exception e) {
                this.logFailure(null, LegacyConversionReport.Kind.GATHERING_TYPE, "gathering_type", source, e);
            }
        }
    }

    private void convertQuests(Path addonRoot, LegacyLayoutDetector.Detection detection) {
        Set<String> packCultures = Set.copyOf(detection.culturesWithLegacyContent());
        LinkedHashSet<String> supportedFamilies = new LinkedHashSet<String>(SUPPORTED_QUEST_FAMILIES);
        for (String c : packCultures) {
            supportedFamilies.add(c + "basic");
        }
        for (Path source : detection.questTxts()) {
            String subDir = LegacyConversionEngine.questFamily(source);
            if (subDir == null) {
                String inferred = LegacyConversionEngine.inferQuestFamilyFromPackContext(source, packCultures);
                if (inferred != null) {
                    subDir = inferred;
                    LOGGER.info("Quest {} promoted to family '{}' (pack-context inference)", (Object)source.getFileName(), (Object)subDir);
                } else {
                    this.recordAmbiguity(null, LegacyConversionReport.Kind.QUEST, source.toString(), "missing family sub-directory \u2014 expected quests/<family>/<id>.txt (spec \u00a77.6)", "Move the file under quests/<family>/<name>.txt. Supported families in this pack: " + String.join((CharSequence)", ", supportedFamilies) + ". If this is a world/marvel quest chain, those are not yet ported (plan \u00a76.3) \u2014 leave the TXT where it is.");
                    LOGGER.warn("Skipping quest with no family directory: {}", (Object)source);
                    continue;
                }
            }
            if (!supportedFamilies.contains(subDir)) {
                this.report.recordSkipped(null, LegacyConversionReport.Kind.QUEST, source.toString(), "quest family '" + subDir + "' is out of scope (spec \u00a77.6)");
                LOGGER.warn("Skipping unsupported quest family: {}", (Object)source);
                continue;
            }
            try {
                Path writeRoot = this.writeRootOrSource(addonRoot);
                Path target = writeRoot.resolve("quests").resolve(subDir).resolve(LegacyConversionEngine.stripTxt(source.getFileName().toString()) + ".json");
                if (!this.maybeSkipConverterPinned(target, LegacyConversionReport.Kind.QUEST, null, source)) continue;
                this.checkUnknownQuestKeys(addonRoot, source);
                String questKey = LegacyConversionEngine.stripTxt(source.getFileName().toString());
                Map<String, Object> quest = QuestDefinitionConverter.parseQuest(source, new QuestDefinitionConverter.Issues(this.report, questKey, LegacyConversionEngine.relPathFor(addonRoot, source)));
                this.writeAndRecord(addonRoot, target, source, quest, LegacyConversionReport.Kind.QUEST, null);
            }
            catch (Exception e) {
                this.logFailure(null, LegacyConversionReport.Kind.QUEST, "quest", source, e);
            }
        }
    }

    private boolean maybeSkipConverterPinned(Path target, LegacyConversionReport.Kind kind, String culture, Path source) {
        PreservationPolicy.Decision d = PreservationPolicy.checkConverterSkip(target);
        if (d.overwrite()) {
            return true;
        }
        this.noteConverterPinned(target, kind, culture, source, d.reason());
        return false;
    }

    private void noteConverterPinned(Path target, LegacyConversionReport.Kind kind, String culture, Path source, String reason) {
        LOGGER.warn("Preserving {} ({}), source left in place", (Object)target, (Object)reason);
        this.report.recordPreserved(culture, kind, target.toString(), reason);
    }

    private void writeAndRecord(Path addonRoot, Path target, Path source, Map<String, Object> json, LegacyConversionReport.Kind kind, String culture) throws IOException {
        this.writeJson(target, json);
        Path writeRoot = this.writeRootOrSource(addonRoot);
        this.manifest.recordFile(writeRoot, target, null);
        this.report.recordConverted(culture, kind);
    }

    private void writeJson(Path target, Map<String, Object> json) throws IOException {
        Files.createDirectories(target.getParent(), new FileAttribute[0]);
        Path tmp = target.resolveSibling(String.valueOf(target.getFileName()) + ".tmp");
        try (BufferedWriter w = Files.newBufferedWriter(tmp, StandardCharsets.UTF_8, new OpenOption[0]);){
            GSON.toJson(json, (Appendable)w);
        }
        try {
            Files.move(tmp, target, StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING);
        }
        catch (AtomicMoveNotSupportedException unsupported) {
            Files.move(tmp, target, StandardCopyOption.REPLACE_EXISTING);
        }
    }

    private static List<Path> onlyCulture(List<Path> paths, String culture) {
        ArrayList<Path> out = new ArrayList<Path>(paths.size());
        for (Path p : paths) {
            String c = LegacyLayoutDetector.Detection.cultureOf(p);
            if (!culture.equalsIgnoreCase(c)) continue;
            out.add(p);
        }
        return out;
    }

    private static String legacySubCategory(Path source, String pivotDir) {
        for (int i = 0; i < source.getNameCount() - 1; ++i) {
            if (!pivotDir.equalsIgnoreCase(source.getName(i).toString())) continue;
            int cat = i + 1;
            if (cat < source.getNameCount() - 1) {
                return source.getName(cat).toString().toLowerCase(Locale.ROOT);
            }
            return "";
        }
        return "";
    }

    private static String extractGoalCategory(Path source) {
        boolean seenGoals = false;
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < source.getNameCount() - 1; ++i) {
            String seg = source.getName(i).toString().toLowerCase(Locale.ROOT);
            if (seenGoals) {
                if (sb.length() > 0) {
                    sb.append('/');
                }
                sb.append(seg);
                continue;
            }
            if (!"goals".equals(seg)) continue;
            seenGoals = true;
        }
        return sb.toString();
    }

    static String inferQuestFamilyFromPackContext(Path source, Set<String> packCultures) {
        if (packCultures.size() == 1) {
            return packCultures.iterator().next() + "basic";
        }
        String stem = LegacyConversionEngine.stripTxt(source.getFileName().toString()).toLowerCase(Locale.ROOT);
        ArrayList<String> byLengthDesc = new ArrayList<String>(packCultures);
        byLengthDesc.sort(Comparator.comparingInt(String::length).reversed().thenComparing(Comparator.naturalOrder()));
        for (String c : byLengthDesc) {
            if (!stem.startsWith(c) || stem.length() != c.length() && Character.isLetterOrDigit(stem.charAt(c.length()))) continue;
            return c + "basic";
        }
        return null;
    }

    private static String questFamily(Path source) {
        for (int i = 0; i < source.getNameCount() - 1; ++i) {
            if (!"quests".equalsIgnoreCase(source.getName(i).toString()) || i + 1 >= source.getNameCount() - 1) continue;
            return source.getName(i + 1).toString().toLowerCase(Locale.ROOT);
        }
        return null;
    }

    private static boolean isSupportedQuestFamily(String subDir) {
        return SUPPORTED_QUEST_FAMILIES.contains(subDir);
    }

    private static String stripTxt(String name) {
        String lc = name.toLowerCase(Locale.ROOT);
        if (lc.endsWith(".txt")) {
            return name.substring(0, name.length() - 4);
        }
        return name;
    }

    private static String titleCase(String s) {
        if (s.isEmpty()) {
            return s;
        }
        return Character.toUpperCase(s.charAt(0)) + s.substring(1);
    }

    private static void logFailure(String kind, Path source, Exception e) {
        LOGGER.error("Failed to convert {} {}", new Object[]{kind, source, e});
    }

    private void logFailure(String culture, LegacyConversionReport.Kind kind, String label, Path source, Exception e) {
        LegacyConversionEngine.logFailure(label, source, e);
        String reason = "parse error: " + e.getClass().getSimpleName() + ": " + e.getMessage();
        String hint = this.mode.isStrict() ? "Inspect the TXT at " + String.valueOf(source) + ". The parser expected valid " + kind.display() + " data. Re-run the command after fixing." : null;
        this.recordAmbiguity(culture, kind, source.toString(), reason, hint);
    }

    private void recordAmbiguity(String culture, LegacyConversionReport.Kind kind, String path, String reason, String fixHint) {
        if (this.mode.isStrict()) {
            this.report.recordSkipped(culture, kind, path, reason, fixHint);
        } else {
            this.report.recordSkipped(culture, kind, path, reason);
        }
    }

    void emitConvertAmbiguities() {
        String hint;
        String legacy;
        String culture;
        int idx;
        for (String entry : this.report.unmappedItems()) {
            idx = entry.indexOf("::");
            culture = idx < 0 ? null : entry.substring(0, idx);
            String string = legacy = idx < 0 ? entry : entry.substring(idx + 2);
            if ("<global>".equals(culture)) {
                culture = null;
            }
            hint = "Add one line to " + (String)(culture == null ? "<addon_root>/itemlist.txt" : "<addon_root>/cultures/" + culture + "/itemlist.txt") + ": " + legacy + ";<modern:registry_id>. If the item no longer exists in 9.0, remove the reference from the legacy TXT.";
            this.recordAmbiguity(culture, LegacyConversionReport.Kind.SHOP, "unmapped item reference", "unmapped item \"" + legacy + "\"", hint);
        }
        for (String entry : this.report.unmappedBiomes()) {
            idx = entry.indexOf("::");
            culture = idx < 0 ? null : entry.substring(0, idx);
            String string = legacy = idx < 0 ? entry : entry.substring(idx + 2);
            if ("<global>".equals(culture)) {
                culture = null;
            }
            hint = "Add one entry to <addon_root>/biome_map.json: \"" + legacy + "\": \"<modern_biome_tag>\". Example: \"mountains\": \"#c:is_mountain\".";
            this.recordAmbiguity(culture, LegacyConversionReport.Kind.VILLAGE_TYPE, "unmapped biome reference", "unknown biome \"" + legacy + "\"", hint);
        }
        for (String sig : this.report.unmappedSpecialPoints()) {
            String hint2 = "Mill\u00e9naire 9 doesn't map this special-point type. Map it in PngToNbtConverter.mockStateForSpecialPoint(), or remove the marker from your PNG.";
            this.recordAmbiguity(null, LegacyConversionReport.Kind.BUILDING_PLAN, "unmapped special-point type", "unmapped special-point type \"" + sig + "\"", hint2);
        }
        for (LegacyConversionReport.UnmappedColour uc : this.report.unmappedColours()) {
            int rgb = uc.rgb();
            int r = rgb >> 16 & 0xFF;
            int g = rgb >> 8 & 0xFF;
            int b = rgb & 0xFF;
            String triplet = r + "," + g + "," + b;
            String hint3 = "Add a line to <addon_root>/blocklist.txt mapping RGB(" + triplet + ") to a modern block id, or remove the colour from the PNG. See _conversion_unmapped.blocklist.txt for a fillable stub.";
            this.recordAmbiguity(null, LegacyConversionReport.Kind.BUILDING_PLAN, "unknown colour RGB(" + triplet + ")", "unknown colour RGB(" + triplet + ")", hint3);
        }
    }

    static final class CultureRefs {
        final Set<String> buildingPlans = new HashSet<String>();
        final Set<String> shops = new HashSet<String>();
        final Set<String> villagers = new HashSet<String>();
        final Set<String> namelists = new HashSet<String>();

        CultureRefs() {
        }
    }

    private record BuildingPlanWork(String category, LegacyDataParser.BuildingWithVariants building) {
    }

    private record VariantDrop(String variantKey, int level, Path png, String reason) {
        static VariantDrop crash(String variantKey, Throwable t) {
            return new VariantDrop(variantKey, -1, null, "variant crash: " + t.getClass().getSimpleName() + (String)(t.getMessage() == null ? "" : ": " + t.getMessage()));
        }

        static VariantDrop levelFailed(String variantKey, int level, Path png, Throwable t) {
            return new VariantDrop(variantKey, level, png, "level " + level + " failed: " + t.getClass().getSimpleName() + (String)(t.getMessage() == null ? "" : ": " + t.getMessage()));
        }
    }
}

