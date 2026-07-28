/*
 * Decompiled with CFR 0.150.
 * 
 * Could not load the following classes:
 *  com.mojang.logging.LogUtils
 *  org.slf4j.Logger
 */
package org.millenaire.content;

import com.mojang.logging.LogUtils;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.OpenOption;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.stream.Stream;
import org.millenaire.Millenaire;
import org.millenaire.content.ContentDirectoryManager;
import org.millenaire.content.ContentLoadReport;
import org.millenaire.content.CustomContentIndex;
import org.millenaire.content.DisabledIdsLoader;
import org.millenaire.content.SubmodRoot;
import org.millenaire.culture.ModCultures;
import org.slf4j.Logger;

public final class ValidationReport {
    private static final Logger LOGGER = LogUtils.getLogger();
    private static final DateTimeFormatter TIMESTAMP = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
    private static final List<TypeBinding> PER_CULTURE_JSON_TYPES = List.of(new TypeBinding("building_plan", "buildings", DepthMode.NESTED_BY_CATEGORY), new TypeBinding("villager_type", "villagers", DepthMode.NESTED_BY_CATEGORY), new TypeBinding("village_type", "villages", DepthMode.FLAT), new TypeBinding("shops", "shops", DepthMode.FLAT), new TypeBinding("goal", "goal", DepthMode.FLAT), new TypeBinding("quests", "quests", DepthMode.DEEP));

    private ValidationReport() {
    }

    public static void generate(List<String> activeCultures, Set<String> builtInCultures) {
        if (!ContentDirectoryManager.isInitialized()) {
            return;
        }
        try {
            Path customDir = ContentDirectoryManager.getCustomDir();
            String report = ValidationReport.build(customDir, activeCultures, builtInCultures);
            if (report == null) {
                return;
            }
            Files.writeString(customDir.resolve("_validation_report.txt"), (CharSequence)report, StandardCharsets.UTF_8, new OpenOption[0]);
            LOGGER.info("Validation report written: {}", (Object)customDir.resolve("_validation_report.txt"));
        }
        catch (Exception e) {
            LOGGER.warn("Could not write validation report: {}", (Object)e.getMessage(), (Object)e);
        }
    }

    private static String build(Path customDir, List<String> activeCultures, Set<String> builtInCultures) throws IOException {
        String version;
        StringBuilder sb = new StringBuilder();
        sb.append("=== Mill\u00e9naire Custom Content Report ===\n");
        sb.append("Generated: ").append(LocalDateTime.now().format(TIMESTAMP)).append("\n");
        try {
            version = Millenaire.getModVersion();
        }
        catch (Exception e) {
            version = "(dev)";
        }
        sb.append("Mod version: ").append("millenaire").append(" ").append(version).append("\n\n");
        int customCultureCount = 0;
        int overrideCultureCount = 0;
        boolean anyContent = false;
        TreeMap<String, CultureStats> perCulture = new TreeMap<String, CultureStats>();
        for (String culture : activeCultures) {
            CultureStats stats = ValidationReport.inspectCulture(culture);
            perCulture.put(culture, stats);
            if (stats.isEmpty() && builtInCultures.contains(culture)) continue;
            anyContent = true;
        }
        int gatheringCount = ValidationReport.countGlobalExtras("gathering_type");
        int questCount = ValidationReport.countGlobalExtras("quests");
        int languageCount = ValidationReport.countLanguageExtras();
        boolean hasGlobalExtras = gatheringCount > 0 || questCount > 0 || languageCount > 0;
        Map<String, List<String>> missings = ContentLoadReport.snapshot();
        if (!anyContent && !hasGlobalExtras && missings.isEmpty()) {
            return null;
        }
        for (Map.Entry entry : perCulture.entrySet()) {
            boolean isCustom;
            String culture = (String)entry.getKey();
            CultureStats stats = (CultureStats)entry.getValue();
            if (stats.isEmpty() && builtInCultures.contains(culture)) continue;
            boolean bl = isCustom = !builtInCultures.contains(culture);
            if (isCustom) {
                ++customCultureCount;
            } else if (!stats.isEmpty()) {
                ++overrideCultureCount;
            }
            sb.append("=== Culture: ").append(culture);
            if (isCustom) {
                sb.append(" (custom)");
            } else {
                sb.append(" (built-in with overrides)");
            }
            sb.append(" ===\n");
            sb.append(String.format("  Source: %s%n", isCustom ? "external JSON" : "standard + custom overlay"));
            if (!stats.contributingSubmods.isEmpty()) {
                sb.append(String.format("  sub-mods: %s%n", ValidationReport.formatSubmodList(stats.contributingSubmods)));
            }
            for (Map.Entry<String, TypeStats> typeEntry : stats.perType.entrySet()) {
                TypeStats ts = typeEntry.getValue();
                if (ts.externalFiles == 0 && ts.disabled == 0) continue;
                sb.append(String.format("  %-15s  %d file(s) on disk", typeEntry.getKey(), ts.externalFiles));
                if (ts.disabled > 0) {
                    sb.append(String.format(", %d disabled", ts.disabled));
                }
                sb.append("\n");
            }
            if (stats.hasCultureJson) {
                sb.append("  culture.json     present\n");
            }
            if (stats.hasTradedGoods) {
                sb.append("  traded_goods     present (additive merge)\n");
            }
            if (stats.hasReputation) {
                sb.append("  reputation       present\n");
            }
            if (stats.hasCultureRep) {
                sb.append("  culture_reputation present\n");
            }
            if (stats.namelistCount > 0) {
                sb.append(String.format("  namelists        %d file(s) (additive append)%n", stats.namelistCount));
            }
            sb.append("\n");
        }
        sb.append("=== Global ===\n");
        sb.append(String.format("  gathering_type   %d custom file(s)%n", gatheringCount));
        sb.append(String.format("  quests           %d custom file(s)%n", questCount));
        sb.append(String.format("  language overlays %d custom file(s) across all locales%n", languageCount));
        sb.append("\n");
        if (!missings.isEmpty()) {
            sb.append("=== Missing content (custom cultures) ===\n");
            sb.append("These classpath files were consulted during load but were absent\n");
            sb.append("from both the mod JAR and every sub-mod overlay. For custom (non-\n");
            sb.append("built-in) cultures this is expected when the pack does not provide\n");
            sb.append("the corresponding resource \u2014 the loader falls back or skips the\n");
            sb.append("entry silently. Listed here so pack authors can spot unintended\n");
            sb.append("omissions without grep-ing the server log.\n\n");
            for (Map.Entry<Object, Object> entry : missings.entrySet()) {
                sb.append("  ").append((String)entry.getKey()).append(":\n");
                for (String path : (List)entry.getValue()) {
                    sb.append("    - ").append(path).append("\n");
                }
            }
            sb.append("\n");
        }
        sb.append("=== Summary ===\n");
        sb.append(String.format("Cultures: %d custom, %d built-in with overrides, %d total loaded%n", customCultureCount, overrideCultureCount, activeCultures.size()));
        sb.append(String.format("Registries after load: %d building plans, %d villager types, %d village types%n", ModCultures.getAllBuildingPlans().size(), ModCultures.getAllVillagerTypes().size(), ModCultures.getAllVillageTypes().size()));
        sb.append("\n");
        sb.append("For detailed errors and warnings during loading, see logs/latest.log\n");
        sb.append("(search for 'Error', 'WARN CultureLoader', 'WARN BuildingPlanSet').\n");
        return sb.toString();
    }

    private static CultureStats inspectCulture(String culture) throws IOException {
        CultureStats stats = new CultureStats();
        List<SubmodRoot> submods = CustomContentIndex.current().rootsForCulture(culture);
        if (submods.isEmpty()) {
            return stats;
        }
        TreeMap disabledUnion = new TreeMap();
        TreeMap<String, Integer> externalCount = new TreeMap<String, Integer>();
        for (SubmodRoot submod : submods) {
            Path cultureDir = submod.root().resolve("cultures").resolve(culture);
            if (!Files.isDirectory(cultureDir, new LinkOption[0])) continue;
            stats.contributingSubmods.add(submod);
            stats.hasCultureJson |= Files.isRegularFile(cultureDir.resolve("culture.json"), new LinkOption[0]);
            stats.hasTradedGoods |= Files.isRegularFile(cultureDir.resolve("traded_goods.json"), new LinkOption[0]);
            stats.hasReputation |= Files.isRegularFile(cultureDir.resolve("reputation.json"), new LinkOption[0]);
            stats.hasCultureRep |= Files.isRegularFile(cultureDir.resolve("culture_reputation.json"), new LinkOption[0]);
            Path namelistsDir = cultureDir.resolve("namelists");
            if (Files.isDirectory(namelistsDir, new LinkOption[0])) {
                stats.namelistCount += ValidationReport.countFiles(namelistsDir, ".txt");
            }
            for (TypeBinding binding : PER_CULTURE_JSON_TYPES) {
                Path typeDir = cultureDir.resolve(binding.dirName());
                if (!Files.isDirectory(typeDir, new LinkOption[0])) continue;
                int count = switch (binding.depth().ordinal()) {
                    default -> throw new MatchException(null, null);
                    case 1 -> ValidationReport.countFilesAtDepth(typeDir, 2, ".json");
                    case 2 -> ValidationReport.countFilesDeep(typeDir, ".json");
                    case 0 -> ValidationReport.countFiles(typeDir, ".json");
                };
                Set<String> disabled = DisabledIdsLoader.load(typeDir);
                if (count > 0) {
                    externalCount.merge(binding.label(), count, Integer::sum);
                }
                if (disabled.isEmpty()) continue;
                disabledUnion.computeIfAbsent(binding.label(), k -> new HashSet()).addAll(disabled);
            }
        }
        LinkedHashSet touchedTypes = new LinkedHashSet();
        touchedTypes.addAll(externalCount.keySet());
        touchedTypes.addAll(disabledUnion.keySet());
        for (String type : touchedTypes) {
            int count = externalCount.getOrDefault(type, 0);
            int disabled = disabledUnion.getOrDefault(type, Set.of()).size();
            if (count <= 0 && disabled <= 0) continue;
            stats.perType.put(type, new TypeStats(count, disabled));
        }
        return stats;
    }

    private static int countFilesAtDepth(Path dir, int depth, String ext) throws IOException {
        if (!Files.isDirectory(dir, new LinkOption[0])) {
            return 0;
        }
        try (Stream<Path> stream = ContentDirectoryManager.safeWalk(dir);){
            int n = (int)stream.filter(x$0 -> Files.isRegularFile(x$0, new LinkOption[0])).filter(p -> dir.relativize((Path)p).getNameCount() == depth).filter(p -> !p.getFileName().toString().startsWith("_")).filter(p -> p.getFileName().toString().endsWith(ext)).count();
            return n;
        }
    }

    private static int countFilesDeep(Path dir, String ext) throws IOException {
        if (!Files.isDirectory(dir, new LinkOption[0])) {
            return 0;
        }
        try (Stream<Path> stream = ContentDirectoryManager.safeWalk(dir);){
            int n = (int)stream.filter(x$0 -> Files.isRegularFile(x$0, new LinkOption[0])).filter(p -> !p.getFileName().toString().startsWith("_")).filter(p -> p.getFileName().toString().endsWith(ext)).count();
            return n;
        }
    }

    private static int countFiles(Path dir, String ext) throws IOException {
        if (!Files.isDirectory(dir, new LinkOption[0])) {
            return 0;
        }
        try (Stream<Path> stream = Files.list(dir);){
            int n = (int)stream.filter(x$0 -> Files.isRegularFile(x$0, new LinkOption[0])).filter(p -> !p.getFileName().toString().startsWith("_")).filter(p -> p.getFileName().toString().endsWith(ext)).count();
            return n;
        }
    }

    private static int countGlobalExtras(String subdir) {
        List<SubmodRoot> submods = "gathering_type".equals(subdir) ? CustomContentIndex.current().rootsWithGatheringType() : ("quests".equals(subdir) ? CustomContentIndex.current().rootsWithQuests() : CustomContentIndex.current().roots());
        int total = 0;
        for (SubmodRoot submod : submods) {
            Path dir = submod.root().resolve(subdir);
            if (!Files.isDirectory(dir, new LinkOption[0])) continue;
            try {
                Stream<Path> stream = ContentDirectoryManager.safeWalk(dir);
                try {
                    total += (int)stream.filter(x$0 -> Files.isRegularFile(x$0, new LinkOption[0])).filter(p -> !p.getFileName().toString().startsWith("_")).filter(p -> p.getFileName().toString().endsWith(".json")).count();
                }
                finally {
                    if (stream == null) continue;
                    stream.close();
                }
            }
            catch (IOException iOException) {}
        }
        return total;
    }

    private static int countLanguageExtras() {
        int total = 0;
        for (SubmodRoot submod : CustomContentIndex.current().rootsWithLanguages()) {
            Path langs = submod.root().resolve("languages");
            if (!Files.isDirectory(langs, new LinkOption[0])) continue;
            try {
                Stream<Path> langSub = Files.list(langs);
                try {
                    for (Path lang : langSub::iterator) {
                        if (!Files.isDirectory(lang, new LinkOption[0])) continue;
                        total += ValidationReport.countFiles(lang, ".txt");
                        total += ValidationReport.countFiles(lang, ".json");
                    }
                }
                finally {
                    if (langSub == null) continue;
                    langSub.close();
                }
            }
            catch (IOException iOException) {}
        }
        return total;
    }

    private static String formatSubmodList(List<SubmodRoot> submods) {
        StringBuilder out = new StringBuilder();
        for (int i = 0; i < submods.size(); ++i) {
            if (i > 0) {
                out.append(", ");
            }
            out.append(submods.get(i).name());
        }
        return out.toString();
    }

    private static class CultureStats {
        final Map<String, TypeStats> perType = new TreeMap<String, TypeStats>();
        final List<SubmodRoot> contributingSubmods = new ArrayList<SubmodRoot>();
        boolean hasCultureJson;
        boolean hasTradedGoods;
        boolean hasReputation;
        boolean hasCultureRep;
        int namelistCount;

        private CultureStats() {
        }

        boolean isEmpty() {
            return !this.hasCultureJson && !this.hasTradedGoods && !this.hasReputation && !this.hasCultureRep && this.namelistCount == 0 && this.perType.values().stream().allMatch(t -> t.externalFiles == 0 && t.disabled == 0);
        }
    }

    private record TypeStats(int externalFiles, int disabled) {
        int total() {
            return this.externalFiles;
        }
    }

    private record TypeBinding(String label, String dirName, DepthMode depth) {
    }

    private static final class DepthMode
    extends Enum<DepthMode> {
        public static final /* enum */ DepthMode FLAT = new DepthMode();
        public static final /* enum */ DepthMode NESTED_BY_CATEGORY = new DepthMode();
        public static final /* enum */ DepthMode DEEP = new DepthMode();
        private static final /* synthetic */ DepthMode[] $VALUES;

        public static DepthMode[] values() {
            return (DepthMode[])$VALUES.clone();
        }

        public static DepthMode valueOf(String name) {
            return Enum.valueOf(DepthMode.class, name);
        }

        private static /* synthetic */ DepthMode[] $values() {
            return new DepthMode[]{FLAT, NESTED_BY_CATEGORY, DEEP};
        }

        static {
            $VALUES = DepthMode.$values();
        }
    }
}

