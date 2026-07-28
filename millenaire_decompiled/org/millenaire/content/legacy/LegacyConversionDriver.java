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
import java.io.RandomAccessFile;
import java.nio.channels.FileChannel;
import java.nio.channels.FileLock;
import java.nio.charset.StandardCharsets;
import java.nio.file.AccessDeniedException;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.nio.file.StandardOpenOption;
import java.nio.file.attribute.FileAttribute;
import java.util.HashSet;
import java.util.Locale;
import java.util.Set;
import java.util.TreeSet;
import org.millenaire.content.ContentDirectoryManager;
import org.millenaire.content.CultureIdPolicy;
import org.millenaire.content.CustomContentIndex;
import org.millenaire.content.legacy.BiomeMapper;
import org.millenaire.content.legacy.ConversionMode;
import org.millenaire.content.legacy.ConverterOutputManifest;
import org.millenaire.content.legacy.ItemIdMapper;
import org.millenaire.content.legacy.LegacyConversionEngine;
import org.millenaire.content.legacy.LegacyConversionReport;
import org.millenaire.content.legacy.LegacyLayoutDetector;
import org.millenaire.culture.CultureLoader;
import org.slf4j.Logger;

public final class LegacyConversionDriver {
    private static final Logger LOGGER = LogUtils.getLogger();
    static final String PROBE_FILENAME = ".millenaire-convert-probe";
    static final String LOCK_FILENAME = ".millenaire-convert.lock";
    static final String REPORT_FILENAME = "_conversion_report.txt";
    public static final String CONVERTER_VERSION = "9.0.0-dev-preview.5";
    private static final Set<String> KNOWN_ROOT_DIRS = Set.of("cultures", "languages", "quests", "goals", "resourcepack");

    private LegacyConversionDriver() {
    }

    public static LockHandle acquireLockAt(Path addonRoot) throws IOException {
        FileLock lock;
        Path lockFile = LegacyConversionDriver.lockFileFor(addonRoot);
        RandomAccessFile raf = new RandomAccessFile(lockFile.toFile(), "rw");
        FileChannel channel = raf.getChannel();
        try {
            lock = channel.tryLock();
        }
        catch (IOException e) {
            try {
                channel.close();
            }
            catch (IOException iOException) {
                // empty catch block
            }
            try {
                raf.close();
            }
            catch (IOException iOException) {
                // empty catch block
            }
            throw e;
        }
        return new LockHandle(lockFile, raf, channel, lock);
    }

    private static Path lockFileFor(Path addonRoot) {
        Path customRoot;
        try {
            customRoot = ContentDirectoryManager.getCustomDir();
        }
        catch (IllegalStateException notInitialised) {
            customRoot = null;
        }
        if (customRoot != null) {
            try {
                if (addonRoot.toRealPath(new LinkOption[0]).startsWith(customRoot.toRealPath(new LinkOption[0]))) {
                    return customRoot.resolve(LOCK_FILENAME);
                }
            }
            catch (IOException iOException) {
                // empty catch block
            }
        }
        return addonRoot.resolve(LOCK_FILENAME);
    }

    public static boolean isWritable(Path addonRoot) {
        Path probe = addonRoot.resolve(PROBE_FILENAME);
        try {
            Files.write(probe, new byte[]{77}, StandardOpenOption.CREATE, StandardOpenOption.WRITE, StandardOpenOption.TRUNCATE_EXISTING);
            Files.deleteIfExists(probe);
            return true;
        }
        catch (AccessDeniedException ade) {
            return false;
        }
        catch (IOException io) {
            return false;
        }
    }

    static void writeReportAtomic(Path target, LegacyConversionReport report) throws IOException {
        Path tmp = target.resolveSibling(String.valueOf(target.getFileName()) + ".tmp");
        Files.writeString(tmp, (CharSequence)report.render(), StandardCharsets.UTF_8, StandardOpenOption.CREATE, StandardOpenOption.WRITE, StandardOpenOption.TRUNCATE_EXISTING);
        try {
            Files.move(tmp, target, StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING);
        }
        catch (AtomicMoveNotSupportedException unsupported) {
            Files.move(tmp, target, StandardCopyOption.REPLACE_EXISTING);
        }
    }

    public static DriverResult runWithOutput(Path sourceRoot, Path outputRoot, LegacyLayoutDetector.Detection detection, ConversionMode mode) {
        boolean manifestChanged;
        ConverterOutputManifest manifest;
        long tStart = System.currentTimeMillis();
        HashSet<String> builtinCultures = new HashSet<String>(CultureLoader.BUILTIN_CULTURES);
        TreeSet<String> mapperCultures = new TreeSet<String>(builtinCultures);
        if (ContentDirectoryManager.isInitialized()) {
            for (String c : CustomContentIndex.current().customCultureIds()) {
                if (!LegacyConversionDriver.isValidCultureId(c)) continue;
                mapperCultures.add(c);
            }
        }
        for (String c : detection.culturesWithLegacyContent()) {
            if (LegacyConversionDriver.isValidCultureId(c)) {
                mapperCultures.add(c);
                continue;
            }
            LOGGER.warn("Skipping legacy pack with invalid culture id '{}' (must match {}); rename the directory to proceed.", (Object)c, (Object)CultureIdPolicy.PATTERN.pattern());
        }
        ItemIdMapper items = ItemIdMapper.loadAll(sourceRoot, mapperCultures);
        BiomeMapper biomes = BiomeMapper.loadAll(sourceRoot, mapperCultures);
        Path manifestPath = outputRoot.resolve("_conversion_manifest.json");
        try {
            manifest = ConverterOutputManifest.readOrEmpty(manifestPath, CONVERTER_VERSION);
        }
        catch (IOException e) {
            LOGGER.warn("Failed to read conversion manifest at {}: {}. Starting with an empty manifest.", (Object)manifestPath, (Object)e.getMessage());
            manifest = new ConverterOutputManifest(CONVERTER_VERSION);
        }
        int manifestEntriesBefore = manifest.entries().size();
        LegacyConversionReport report = new LegacyConversionReport();
        LegacyConversionEngine engine = new LegacyConversionEngine(items, biomes, manifest, report, mode);
        LOGGER.info("Legacy conversion starting in {} mode \u2014 {} cultures, {} PNGs, {} villager TXTs (source {} \u2192 output {})", new Object[]{mode, detection.culturesWithLegacyContent().size(), detection.buildingPngs().size(), detection.villagerTxts().size(), sourceRoot, outputRoot});
        if (Thread.currentThread().isInterrupted()) {
            LOGGER.warn("Legacy conversion interrupted before culture pass; returning partial report");
            long elapsedInterrupted = System.currentTimeMillis() - tStart;
            return new DriverResult(report, manifest, elapsedInterrupted);
        }
        try {
            engine.convertAll(sourceRoot, outputRoot, detection);
        }
        catch (Exception e) {
            LOGGER.error("Legacy conversion aborted: {}", (Object)e.getMessage(), (Object)e);
        }
        LegacyConversionDriver.copyPreservedFiles(sourceRoot, outputRoot, detection);
        boolean bl = manifestChanged = manifest.entries().size() > manifestEntriesBefore;
        if (manifestChanged) {
            try {
                Path parent = manifestPath.getParent();
                if (parent != null) {
                    Files.createDirectories(parent, new FileAttribute[0]);
                }
                manifest.writeAtomic(manifestPath);
            }
            catch (IOException e) {
                LOGGER.warn("Failed to write conversion manifest: {}", (Object)e.getMessage());
            }
        }
        if (mode == ConversionMode.CONVERT && (report.totalConverted() > 0 || report.totalSkipped() > 0 || report.totalOutOfScope() > 0)) {
            try {
                Files.createDirectories(outputRoot, new FileAttribute[0]);
                LegacyConversionDriver.writeReportAtomic(outputRoot.resolve(REPORT_FILENAME), report);
            }
            catch (IOException e) {
                LOGGER.warn("Failed to write conversion report at {}: {}", (Object)outputRoot, (Object)e.getMessage());
            }
        }
        long elapsed = System.currentTimeMillis() - tStart;
        LOGGER.info("Legacy conversion in {} mode: {} files converted, {} skipped across {} cultures in {} ms", new Object[]{mode, report.totalConverted(), report.totalSkipped(), report.cultureCount(), elapsed});
        return new DriverResult(report, manifest, elapsed);
    }

    private static void copyPreservedFiles(Path sourceRoot, Path outputRoot, LegacyLayoutDetector.Detection detection) {
        for (Path src : detection.preservedPaths()) {
            Path rel;
            try {
                rel = sourceRoot.relativize(src);
            }
            catch (IllegalArgumentException e) {
                LOGGER.warn("Preserved path {} is not under source root {} \u2014 skipping", (Object)src, (Object)sourceRoot);
                continue;
            }
            Path normalised = LegacyConversionDriver.stripWrapperPrefix(rel);
            Path dest = outputRoot.resolve(normalised);
            try {
                Path parent = dest.getParent();
                if (parent != null) {
                    Files.createDirectories(parent, new FileAttribute[0]);
                }
                Files.copy(src, dest, StandardCopyOption.REPLACE_EXISTING);
            }
            catch (IOException e) {
                LOGGER.warn("Failed to copy preserved file {} \u2192 {}: {}", new Object[]{src, dest, e.getMessage()});
            }
        }
    }

    static Path stripWrapperPrefix(Path rel) {
        String last;
        if (rel == null || rel.getNameCount() == 0) {
            return rel;
        }
        for (int i = 0; i < rel.getNameCount(); ++i) {
            String seg = rel.getName(i).toString().toLowerCase(Locale.ROOT);
            if (!KNOWN_ROOT_DIRS.contains(seg)) continue;
            if (i == 0) {
                return rel;
            }
            return rel.subpath(i, rel.getNameCount());
        }
        if (rel.getNameCount() >= 2 && ("itemlist.txt".equals(last = rel.getFileName().toString().toLowerCase(Locale.ROOT)) || "biome_map.json".equals(last) || last.startsWith("readme") || last.startsWith("_warning") || last.startsWith("warning"))) {
            return rel.getFileName();
        }
        return rel;
    }

    static boolean isValidCultureId(String id) {
        return id != null && !id.isEmpty() && CultureIdPolicy.PATTERN.matcher(id).matches();
    }

    public static final class LockHandle
    implements AutoCloseable {
        private final Path lockPath;
        private final RandomAccessFile raf;
        private final FileChannel channel;
        private final FileLock lock;

        private LockHandle(Path lockPath, RandomAccessFile raf, FileChannel channel, FileLock lock) {
            this.lockPath = lockPath;
            this.raf = raf;
            this.channel = channel;
            this.lock = lock;
        }

        public Path lockPath() {
            return this.lockPath;
        }

        public boolean isHeld() {
            return this.lock != null;
        }

        @Override
        public void close() {
            try {
                if (this.lock != null && this.lock.isValid()) {
                    this.lock.release();
                }
            }
            catch (IOException e) {
                LOGGER.warn("advisory lock release failed on {}: {}", (Object)this.lockPath, (Object)e.getMessage());
            }
            try {
                if (this.channel != null) {
                    this.channel.close();
                }
            }
            catch (IOException e) {
                LOGGER.warn("advisory lock channel close failed on {}: {}", (Object)this.lockPath, (Object)e.getMessage());
            }
            try {
                this.raf.close();
            }
            catch (IOException iOException) {
                // empty catch block
            }
        }
    }

    public record DriverResult(LegacyConversionReport report, ConverterOutputManifest manifest, long elapsedMillis) {
    }
}

