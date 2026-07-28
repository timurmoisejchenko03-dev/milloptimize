/*
 * Decompiled with CFR 0.150.
 * 
 * Could not load the following classes:
 *  com.mojang.logging.LogUtils
 *  org.slf4j.Logger
 */
package org.millenaire.content.legacy;

import com.mojang.logging.LogUtils;
import java.io.IOException;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.CopyOption;
import java.nio.file.FileVisitOption;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.TreeSet;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.millenaire.content.CultureIdPolicy;
import org.millenaire.content.legacy.ConversionMode;
import org.millenaire.content.legacy.LegacyConversionReport;
import org.slf4j.Logger;

public final class LegacyLayoutDetector {
    private static final Logger LOGGER = LogUtils.getLogger();
    private static final List<String> OUT_OF_SCOPE_SEGMENTS = List.of("help", "banner", "banners");

    private LegacyLayoutDetector() {
    }

    public static List<Normalisation> normaliseCultureDirsInPlace(Path customRoot, ConversionMode mode, LegacyConversionReport report) {
        ArrayList<Normalisation> out = new ArrayList<Normalisation>();
        if (customRoot == null) {
            return out;
        }
        Path culturesDir = customRoot.resolve("cultures");
        if (!Files.isDirectory(culturesDir, new LinkOption[0])) {
            return out;
        }
        ArrayList children = new ArrayList();
        try (Stream<Path> list = Files.list(culturesDir);){
            list.filter(x$0 -> Files.isDirectory(x$0, new LinkOption[0])).forEach(children::add);
        }
        catch (IOException e) {
            LOGGER.warn("LegacyLayoutDetector: failed to list {}: {}", (Object)culturesDir, (Object)e.getMessage());
            return out;
        }
        children.sort((a, b) -> a.getFileName().toString().compareTo(b.getFileName().toString()));
        for (Path original : children) {
            Normalisation n = LegacyLayoutDetector.normaliseOne(original, mode, report);
            out.add(n);
            if (report == null) continue;
            report.recordNormalisation(n);
        }
        return out;
    }

    private static Normalisation normaliseOne(Path original, ConversionMode mode, LegacyConversionReport report) {
        String name = original.getFileName().toString();
        if (CultureIdPolicy.PATTERN.matcher(name).matches()) {
            return new Normalisation(original, original, Normalisation.Outcome.ALREADY_CANONICAL);
        }
        String lowered = name.toLowerCase(Locale.ROOT);
        boolean foldable = CultureIdPolicy.PATTERN.matcher(lowered).matches();
        Path canonical = original.resolveSibling(lowered);
        if (!foldable) {
            if (mode.isStrict() && report != null) {
                report.recordSkipped(name, LegacyConversionReport.Kind.CULTURE, original.toString(), "invalid culture dir name \"" + name + "\" \u2014 CultureIdPolicy.PATTERN requires " + CultureIdPolicy.PATTERN.pattern(), "Rename the directory to a canonical form (lowercase alphanumeric, optionally with '_' or '-' separators). If the current name contains other punctuation (e.g. dots), pick a canonical form manually.");
            } else {
                LOGGER.warn("Legacy auto-conversion: rejecting culture directory '{}' \u2014 name is not case-foldable to CultureIdPolicy.PATTERN ({}); rename manually.", (Object)name, (Object)CultureIdPolicy.PATTERN.pattern());
            }
            return new Normalisation(original, canonical, Normalisation.Outcome.REJECTED_NON_FOLDABLE);
        }
        if (Files.exists(canonical, new LinkOption[0])) {
            boolean sameFile = false;
            try {
                sameFile = Files.isSameFile(original, canonical);
            }
            catch (IOException iOException) {
                // empty catch block
            }
            if (!sameFile) {
                if (mode.isStrict() && report != null) {
                    report.recordSkipped(lowered, LegacyConversionReport.Kind.CULTURE, original.toString(), "culture dir '" + name + "' coexists with '" + lowered + "' \u2014 can't determine which is authoritative", "Merge the contents of '" + name + "' and '" + lowered + "' manually into the lowercase directory, then remove the other.");
                } else {
                    LOGGER.warn("Legacy auto-conversion: rejecting culture directory '{}' \u2014 coexists with '{}' on a case-sensitive filesystem; merge manually.", (Object)name, (Object)lowered);
                }
                return new Normalisation(original, canonical, Normalisation.Outcome.REJECTED_COEXISTENCE);
            }
            LOGGER.info("Legacy auto-conversion: culture directory '{}' is already canonical on a case-insensitive filesystem (no rename needed)", (Object)name);
            return new Normalisation(original, canonical, Normalisation.Outcome.RENAMED);
        }
        if (!mode.isStrict()) {
            try {
                try {
                    Files.move(original, canonical, StandardCopyOption.ATOMIC_MOVE);
                }
                catch (AtomicMoveNotSupportedException ignored) {
                    Files.move(original, canonical, new CopyOption[0]);
                }
                LOGGER.info("Legacy auto-conversion: normalised culture directory '{}' \u2192 '{}' (case fold)", (Object)name, (Object)lowered);
                return new Normalisation(original, canonical, Normalisation.Outcome.RENAMED);
            }
            catch (IOException e) {
                LOGGER.warn("Legacy auto-conversion: failed to rename culture dir '{}' \u2192 '{}': {}", new Object[]{name, lowered, e.getMessage()});
                return new Normalisation(original, canonical, Normalisation.Outcome.REJECTED_COEXISTENCE);
            }
        }
        if (report != null) {
            report.recordSkipped(lowered, LegacyConversionReport.Kind.CULTURE, original.toString(), "invalid culture dir name \"" + name + "\" \u2014 CultureIdPolicy.PATTERN requires " + CultureIdPolicy.PATTERN.pattern(), "Rename the directory to \"" + lowered + "\" (lowercase, optionally with '_' or '-' separators).");
        }
        return new Normalisation(original, canonical, Normalisation.Outcome.REJECTED_COEXISTENCE);
    }

    public static Detection scan(Path addonRoot) {
        if (addonRoot == null || !Files.isDirectory(addonRoot, new LinkOption[0])) {
            return LegacyLayoutDetector.empty();
        }
        Collector c = new Collector();
        try (Stream<Path> walk = Files.walk(addonRoot, 7, new FileVisitOption[0]);){
            walk.filter(x$0 -> Files.isRegularFile(x$0, new LinkOption[0])).forEach(p -> LegacyLayoutDetector.classify(p, addonRoot, c));
        }
        catch (IOException e) {
            System.err.println("[WARN] LegacyLayoutDetector: failed to walk " + String.valueOf(addonRoot) + ": " + e.getMessage());
            return LegacyLayoutDetector.empty();
        }
        return c.build();
    }

    private static Detection empty() {
        List<Path> e = List.of();
        return new Detection(e, e, e, e, e, e, e, e, e, e, e, e, e);
    }

    private static void classify(Path file, Path root, Collector c) {
        String subdir;
        int cIdx;
        Path rel;
        try {
            rel = root.relativize(file);
        }
        catch (IllegalArgumentException ignored) {
            return;
        }
        String lowerName = file.getFileName().toString().toLowerCase(Locale.ROOT);
        List<String> segs = LegacyLayoutDetector.pathSegmentsLower(rel);
        if (segs.contains("goals") && lowerName.endsWith(".txt") && (segs.contains("genericcrafting") || LegacyLayoutDetector.isNonCraftingGoalFamily(segs))) {
            c.gathering.add(file);
            return;
        }
        if (segs.contains("quests") && lowerName.endsWith(".txt")) {
            boolean isVillagerQuestSubcategory;
            int questsIdx = segs.indexOf("quests");
            boolean bl = isVillagerQuestSubcategory = questsIdx > 0 && "villagers".equals(segs.get(questsIdx - 1));
            if (!isVillagerQuestSubcategory) {
                c.quests.add(file);
                return;
            }
        }
        if (lowerName.endsWith(".txt") && (cIdx = segs.indexOf("cultures")) >= 0 && cIdx + 2 < segs.size() && "walls".equals(segs.get(cIdx + 2))) {
            c.walls.add(file);
            return;
        }
        if (lowerName.endsWith(".txt") && (cIdx = segs.indexOf("cultures")) >= 0 && cIdx + 2 < segs.size() && OUT_OF_SCOPE_SEGMENTS.contains(segs.get(cIdx + 2))) {
            c.outOfScope.add(file);
            return;
        }
        if (lowerName.startsWith("help_") && lowerName.endsWith(".txt")) {
            c.outOfScope.add(file);
            return;
        }
        if (LegacyLayoutDetector.isTopLevelPreservedName(lowerName)) {
            c.preserved.add(file);
            return;
        }
        if (segs.contains("languages")) {
            c.preserved.add(file);
            return;
        }
        int cultIdx = segs.indexOf("cultures");
        if (cultIdx < 0 || cultIdx + 2 >= segs.size()) {
            return;
        }
        switch (subdir = segs.get(cultIdx + 2)) {
            case "buildings": {
                if (lowerName.endsWith(".png")) {
                    c.buildingPngs.add(file);
                    break;
                }
                if (!lowerName.endsWith(".txt")) break;
                c.buildingTxts.add(file);
                break;
            }
            case "custombuildings": {
                if (lowerName.endsWith(".png")) {
                    c.buildingPngs.add(file);
                    break;
                }
                if (!lowerName.endsWith(".txt")) break;
                c.buildingTxts.add(file);
                break;
            }
            case "villagers": {
                if (!lowerName.endsWith(".txt")) break;
                c.villagerTxts.add(file);
                break;
            }
            case "villages": {
                if (!lowerName.endsWith(".txt")) break;
                c.villageTxts.add(file);
                break;
            }
            case "lonebuildings": {
                if (!lowerName.endsWith(".txt")) break;
                c.loneTxts.add(file);
                break;
            }
            case "shops": {
                if (!lowerName.endsWith(".txt")) break;
                c.shopTxts.add(file);
                break;
            }
            case "namelists": 
            case "resourcepack": {
                c.preserved.add(file);
                break;
            }
            default: {
                if (cultIdx + 2 != segs.size() - 1) break;
                if ("traded_goods.txt".equals(lowerName)) {
                    c.tradedGoodsTxts.add(file);
                    break;
                }
                if ("culture.txt".equals(lowerName)) {
                    c.cultureTxts.add(file);
                    break;
                }
                if (!"itemlist.txt".equals(lowerName) && !"biome_map.json".equals(lowerName)) break;
                c.preserved.add(file);
            }
        }
    }

    private static boolean isTopLevelPreservedName(String lowerName) {
        return "itemlist.txt".equals(lowerName) || "biome_map.json".equals(lowerName) || lowerName.startsWith("readme") || lowerName.startsWith("_warning") || lowerName.startsWith("warning");
    }

    private static boolean isNonCraftingGoalFamily(List<String> segs) {
        Iterator<String> iterator = segs.iterator();
        while (iterator.hasNext()) {
            String s;
            switch (s = iterator.next()) {
                case "genericharvesting": 
                case "genericplanting": 
                case "genericmining": 
                case "genericslaughteranimal": 
                case "genericcooking": 
                case "genericplantsapling": 
                case "genericgatherblocks": 
                case "generictakefrombuilding": {
                    return true;
                }
            }
        }
        return false;
    }

    private static List<String> pathSegmentsLower(Path rel) {
        ArrayList<String> out = new ArrayList<String>(rel.getNameCount());
        for (int i = 0; i < rel.getNameCount(); ++i) {
            out.add(rel.getName(i).toString().toLowerCase(Locale.ROOT));
        }
        return out;
    }

    public record Normalisation(Path original, Path canonical, Outcome outcome) {

        public static final class Outcome
        extends Enum<Outcome> {
            public static final /* enum */ Outcome RENAMED = new Outcome();
            public static final /* enum */ Outcome ALREADY_CANONICAL = new Outcome();
            public static final /* enum */ Outcome REJECTED_NON_FOLDABLE = new Outcome();
            public static final /* enum */ Outcome REJECTED_COEXISTENCE = new Outcome();
            private static final /* synthetic */ Outcome[] $VALUES;

            public static Outcome[] values() {
                return (Outcome[])$VALUES.clone();
            }

            public static Outcome valueOf(String name) {
                return Enum.valueOf(Outcome.class, name);
            }

            private static /* synthetic */ Outcome[] $values() {
                return new Outcome[]{RENAMED, ALREADY_CANONICAL, REJECTED_NON_FOLDABLE, REJECTED_COEXISTENCE};
            }

            static {
                $VALUES = Outcome.$values();
            }
        }
    }

    public record Detection(List<Path> buildingPngs, List<Path> buildingTxts, List<Path> villagerTxts, List<Path> villageTxts, List<Path> loneTxts, List<Path> shopTxts, List<Path> tradedGoodsTxts, List<Path> cultureTxts, List<Path> gatheringTxts, List<Path> questTxts, List<Path> outOfScopeTxts, List<Path> preservedPaths, List<Path> wallTxts) {
        public boolean isEmpty() {
            return this.buildingPngs.isEmpty() && this.buildingTxts.isEmpty() && this.villagerTxts.isEmpty() && this.villageTxts.isEmpty() && this.loneTxts.isEmpty() && this.shopTxts.isEmpty() && this.tradedGoodsTxts.isEmpty() && this.cultureTxts.isEmpty() && this.gatheringTxts.isEmpty() && this.questTxts.isEmpty() && this.outOfScopeTxts.isEmpty() && this.wallTxts.isEmpty() && this.preservedPaths.isEmpty();
        }

        public boolean hasOnlyPreservedContent() {
            return !this.preservedPaths.isEmpty() && this.buildingPngs.isEmpty() && this.buildingTxts.isEmpty() && this.villagerTxts.isEmpty() && this.villageTxts.isEmpty() && this.loneTxts.isEmpty() && this.shopTxts.isEmpty() && this.tradedGoodsTxts.isEmpty() && this.cultureTxts.isEmpty() && this.gatheringTxts.isEmpty() && this.questTxts.isEmpty() && this.outOfScopeTxts.isEmpty() && this.wallTxts.isEmpty();
        }

        public List<String> culturesWithLegacyContent() {
            TreeSet cultures = new TreeSet();
            Stream.of(this.buildingTxts, this.buildingPngs, this.villagerTxts, this.villageTxts, this.loneTxts, this.shopTxts, this.tradedGoodsTxts, this.cultureTxts, this.wallTxts).flatMap(Collection::stream).map(Detection::cultureOf).filter(s -> s != null && !s.isEmpty()).forEach(cultures::add);
            return List.copyOf(cultures);
        }

        static String cultureOf(Path p) {
            for (int i = 0; i < p.getNameCount() - 1; ++i) {
                if (!"cultures".equalsIgnoreCase(p.getName(i).toString())) continue;
                return p.getName(i + 1).toString().toLowerCase(Locale.ROOT);
            }
            return null;
        }
    }

    private static final class Collector {
        final List<Path> buildingPngs = new ArrayList<Path>();
        final List<Path> buildingTxts = new ArrayList<Path>();
        final List<Path> villagerTxts = new ArrayList<Path>();
        final List<Path> villageTxts = new ArrayList<Path>();
        final List<Path> loneTxts = new ArrayList<Path>();
        final List<Path> shopTxts = new ArrayList<Path>();
        final List<Path> tradedGoodsTxts = new ArrayList<Path>();
        final List<Path> cultureTxts = new ArrayList<Path>();
        final List<Path> gathering = new ArrayList<Path>();
        final List<Path> quests = new ArrayList<Path>();
        final List<Path> outOfScope = new ArrayList<Path>();
        final List<Path> preserved = new ArrayList<Path>();
        final List<Path> walls = new ArrayList<Path>();

        private Collector() {
        }

        Detection build() {
            return new Detection(Collector.sorted(this.buildingPngs), Collector.sorted(this.buildingTxts), Collector.sorted(this.villagerTxts), Collector.sorted(this.villageTxts), Collector.sorted(this.loneTxts), Collector.sorted(this.shopTxts), Collector.sorted(this.tradedGoodsTxts), Collector.sorted(this.cultureTxts), Collector.sorted(this.gathering), Collector.sorted(this.quests), Collector.sorted(this.outOfScope), Collector.sorted(this.preserved), Collector.sorted(this.walls));
        }

        private static List<Path> sorted(List<Path> in) {
            return in.stream().sorted((a, b) -> a.toString().compareTo(b.toString())).collect(Collectors.collectingAndThen(Collectors.toList(), Collections::unmodifiableList));
        }
    }
}

