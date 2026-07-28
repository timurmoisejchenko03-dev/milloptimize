/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.mojang.logging.LogUtils
 *  org.slf4j.Logger
 */
package org.millenaire.content.legacy;

import com.mojang.logging.LogUtils;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.nio.file.StandardOpenOption;
import java.nio.file.attribute.FileAttribute;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.millenaire.config.MillenaireServerConfig;
import org.millenaire.content.ContentDirectoryManager;
import org.millenaire.content.ContentStatsReporter;
import org.millenaire.content.CultureIdPolicy;
import org.millenaire.content.legacy.ConversionMode;
import org.millenaire.content.legacy.ConverterOutputManifest;
import org.millenaire.content.legacy.LegacyConversionDriver;
import org.millenaire.content.legacy.LegacyConversionReport;
import org.millenaire.content.legacy.LegacyLayoutDetector;
import org.slf4j.Logger;

public final class LegacyAutoConverter {
    private static final Logger LOGGER = LogUtils.getLogger();
    private static final Set<String> warnedVersionDrift = ConcurrentHashMap.newKeySet();
    @Deprecated
    static final String PROBE_FILENAME = ".millenaire-convert-probe";
    static final String REPORT_FILENAME = "_conversion_report.txt";

    public static void resetForTesting() {
        warnedVersionDrift.clear();
    }

    private LegacyAutoConverter() {
    }

    public static void convertIfNeeded() {
        try {
            LegacyAutoConverter.doConvert();
        }
        catch (Throwable t) {
            LOGGER.error("Legacy auto-conversion aborted with unexpected failure; server will continue without converted content: {}", (Object)(t.getClass().getSimpleName() + ": " + t.getMessage()), (Object)t);
        }
    }

    private static void doConvert() {
        Path customRoot;
        if (!ContentDirectoryManager.isInitialized()) {
            return;
        }
        if (!((Boolean)MillenaireServerConfig.SERVER.legacyAutoConvert.get()).booleanValue()) {
            LOGGER.info("Legacy auto-conversion disabled via config (legacyAutoConvert=false)");
            return;
        }
        try {
            customRoot = ContentDirectoryManager.getCustomDir();
        }
        catch (IllegalStateException notInitialised) {
            return;
        }
        if (customRoot == null || !Files.isDirectory(customRoot, new LinkOption[0])) {
            return;
        }
        List<SubmodSource> sources = LegacyAutoConverter.discoverSources(customRoot);
        if (sources.isEmpty()) {
            return;
        }
        if (!LegacyConversionDriver.isWritable(customRoot)) {
            LOGGER.warn("millenaire-custom/ is not writable, skipping legacy auto-conversion. Use /millenaire dev convert-addon from a writable staging dir, or set millenaire.legacyAutoConvert=false to silence this warning.");
            return;
        }
        int totalPngs = 0;
        for (SubmodSource s : sources) {
            totalPngs += s.detection.buildingPngs().size();
        }
        int cap = (Integer)MillenaireServerConfig.SERVER.legacyAutoConvertMaxPngs.get();
        if (totalPngs > cap) {
            LOGGER.warn("Legacy sub-mods total {} PNG files, exceeding the auto-conversion limit ({}). Use /millenaire dev convert-addon offline instead, or raise millenaire.legacyAutoConvertMaxPngs.", (Object)totalPngs, (Object)cap);
            return;
        }
        try (LegacyConversionDriver.LockHandle handle = LegacyConversionDriver.acquireLockAt(customRoot);){
            if (!handle.isHeld()) {
                LOGGER.info("Another process is converting millenaire-custom/, skipping this pass");
                return;
            }
            LegacyAutoConverter.runConversion(customRoot, sources);
        }
        catch (IOException e) {
            LOGGER.warn("Legacy auto-conversion I/O error: {}", (Object)e.getMessage());
        }
    }

    static List<SubmodSource> discoverSources(Path customRoot) {
        List children;
        LegacyLayoutDetector.Detection flatRootPreview = LegacyLayoutDetector.scan(customRoot);
        if (LegacyAutoConverter.hasFlatRootLegacyContent(customRoot, flatRootPreview)) {
            LOGGER.warn("millenaire-custom/ contains legacy TXT/PNG files directly at its root \u2014 wrap your legacy files in a sub-directory (e.g. BUILDINGSNORMAN/) so the converter can place the output in BUILDINGSNORMAN_converted/. Flat-root legacy content will NOT be converted.");
        }
        try (Stream<Path> stream = Files.list(customRoot);){
            children = stream.filter(x$0 -> Files.isDirectory(x$0, new LinkOption[0])).sorted(Comparator.comparing(p -> p.getFileName().toString())).collect(Collectors.toList());
        }
        catch (IOException e) {
            LOGGER.warn("Failed to enumerate children of {}: {}", (Object)customRoot, (Object)e.getMessage());
            return List.of();
        }
        ArrayList<SubmodSource> out = new ArrayList<SubmodSource>();
        for (Path child : children) {
            LegacyLayoutDetector.Detection det;
            String name = child.getFileName().toString();
            if (name.endsWith("_converted")) continue;
            Path convertedSibling = customRoot.resolve(name + "_converted");
            Path conversionMarker = convertedSibling.resolve("_conversion_manifest.json");
            if (Files.isRegularFile(conversionMarker, new LinkOption[0])) {
                String recordedVersion = ConverterOutputManifest.readVersion(conversionMarker);
                if (recordedVersion != null && !recordedVersion.equals("9.0.0-dev-preview.5")) {
                    if (!warnedVersionDrift.add(name)) continue;
                    LOGGER.warn("Sub-mod '{}/' was converted by Mill\u00e9naire {} but the running converter is {}. Output may be stale or incompatible. Delete '{}_converted/' to rebuild on the next boot.", new Object[]{name, recordedVersion, "9.0.0-dev-preview.5", name});
                    continue;
                }
                LOGGER.debug("Skipping already-converted source '{}/' (manifest at {})", (Object)name, (Object)conversionMarker);
                continue;
            }
            if (Files.isDirectory(convertedSibling, new LinkOption[0])) {
                LOGGER.warn("Incomplete '{}_converted/' (no {}); retrying conversion", (Object)name, (Object)"_conversion_manifest.json");
            }
            if ((det = LegacyLayoutDetector.scan(child)).isEmpty()) continue;
            if (det.hasOnlyPreservedContent()) {
                LOGGER.info("Legacy sub-mod '{}/' contains only preserved content; no conversion needed", (Object)name);
                continue;
            }
            out.add(new SubmodSource(name, child, convertedSibling, det));
        }
        return out;
    }

    private static boolean hasFlatRootLegacyContent(Path customRoot, LegacyLayoutDetector.Detection preview) {
        if (preview.isEmpty()) {
            return false;
        }
        return !preview.questTxts().isEmpty() && preview.questTxts().stream().anyMatch(p -> LegacyAutoConverter.isDirectRoot(customRoot, p, "quests")) || !preview.gatheringTxts().isEmpty() && preview.gatheringTxts().stream().anyMatch(p -> LegacyAutoConverter.isDirectRoot(customRoot, p, "goals")) || !preview.buildingPngs().isEmpty() && preview.buildingPngs().stream().anyMatch(p -> LegacyAutoConverter.isDirectCulturesRoot(customRoot, p)) || !preview.villagerTxts().isEmpty() && preview.villagerTxts().stream().anyMatch(p -> LegacyAutoConverter.isDirectCulturesRoot(customRoot, p)) || !preview.cultureTxts().isEmpty() && preview.cultureTxts().stream().anyMatch(p -> LegacyAutoConverter.isDirectCulturesRoot(customRoot, p));
    }

    private static boolean isDirectRoot(Path customRoot, Path file, String segment) {
        Path rel;
        try {
            rel = customRoot.relativize(file);
        }
        catch (IllegalArgumentException e) {
            return false;
        }
        return rel.getNameCount() > 1 && segment.equalsIgnoreCase(rel.getName(0).toString());
    }

    private static boolean isDirectCulturesRoot(Path customRoot, Path file) {
        return LegacyAutoConverter.isDirectRoot(customRoot, file, "cultures");
    }

    static void runConversion(Path customRoot, List<SubmodSource> sources) throws IOException {
        ArrayList<LegacyConversionDriver.DriverResult> results = new ArrayList<LegacyConversionDriver.DriverResult>(sources.size());
        for (SubmodSource s : sources) {
            if (Thread.currentThread().isInterrupted()) {
                LOGGER.warn("Legacy auto-conversion interrupted at source '{}'", (Object)s.name());
                break;
            }
            Files.createDirectories(s.output(), new FileAttribute[0]);
            LOGGER.info("Legacy sub-mod '{}/' \u2192 '{}_converted/' ({} PNGs, {} villager TXTs)", new Object[]{s.name(), s.name(), s.detection().buildingPngs().size(), s.detection().villagerTxts().size()});
            try {
                LegacyConversionDriver.DriverResult r = LegacyConversionDriver.runWithOutput(s.source(), s.output(), s.detection(), ConversionMode.AUTO);
                results.add(r);
                ContentStatsReporter.reportConversion(r.report());
            }
            catch (Exception e) {
                LOGGER.error("Legacy sub-mod '{}/' conversion failed: {}", new Object[]{s.name(), e.getMessage(), e});
            }
        }
        LegacyAutoConverter.writeAggregatedReport(customRoot, sources, results);
    }

    private static void writeAggregatedReport(Path customRoot, List<SubmodSource> sources, List<LegacyConversionDriver.DriverResult> results) {
        int total = 0;
        for (LegacyConversionDriver.DriverResult r : results) {
            if (r == null) continue;
            total += r.report().totalConverted() + r.report().totalSkipped() + r.report().totalOutOfScope();
        }
        if (total == 0) {
            return;
        }
        StringBuilder sb = new StringBuilder();
        sb.append("# Mill\u00e9naire legacy auto-conversion \u2014 ").append(sources.size()).append(" sub-mod(s)\n\n");
        for (int i = 0; i < sources.size(); ++i) {
            SubmodSource s = sources.get(i);
            sb.append("## ").append(s.name()).append("/ \u2192 ").append(s.name()).append("_converted/\n\n");
            if (i >= results.size() || results.get(i) == null) {
                sb.append("(no result \u2014 conversion failed, see server log)\n\n");
                continue;
            }
            sb.append(results.get(i).report().render());
            if (!sb.toString().endsWith("\n")) {
                sb.append('\n');
            }
            sb.append('\n');
        }
        Path target = customRoot.resolve(REPORT_FILENAME);
        try {
            Path tmp = target.resolveSibling(String.valueOf(target.getFileName()) + ".tmp");
            Files.writeString(tmp, (CharSequence)sb.toString(), StandardCharsets.UTF_8, StandardOpenOption.CREATE, StandardOpenOption.WRITE, StandardOpenOption.TRUNCATE_EXISTING);
            try {
                Files.move(tmp, target, StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING);
            }
            catch (AtomicMoveNotSupportedException unsupported) {
                Files.move(tmp, target, StandardCopyOption.REPLACE_EXISTING);
            }
        }
        catch (IOException e) {
            LOGGER.warn("Failed to write aggregated conversion report at {}: {}", (Object)target, (Object)e.getMessage());
        }
    }

    static boolean isValidCultureId(String id) {
        return id != null && !id.isEmpty() && CultureIdPolicy.PATTERN.matcher(id).matches();
    }

    @Deprecated
    static boolean isWritable(Path root) {
        return LegacyConversionDriver.isWritable(root);
    }

    @Deprecated
    static void writeReportAtomic(Path target, LegacyConversionReport report) throws IOException {
        LegacyConversionDriver.writeReportAtomic(target, report);
    }

    record SubmodSource(String name, Path source, Path output, LegacyLayoutDetector.Detection detection) {
    }
}

