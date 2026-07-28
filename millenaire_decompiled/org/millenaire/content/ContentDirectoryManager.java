/*
 * Decompiled with CFR 0.150.
 * 
 * Could not load the following classes:
 *  com.mojang.logging.LogUtils
 *  net.minecraft.server.MinecraftServer
 *  org.slf4j.Logger
 */
package org.millenaire.content;

import com.mojang.logging.LogUtils;
import java.io.IOException;
import java.nio.file.FileVisitOption;
import java.nio.file.FileVisitResult;
import java.nio.file.FileVisitor;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.nio.file.attribute.FileAttribute;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.stream.Stream;
import net.minecraft.server.MinecraftServer;
import org.slf4j.Logger;

public final class ContentDirectoryManager {
    private static final Logger LOGGER = LogUtils.getLogger();
    public static final String STANDARD_DIR_NAME = "millenaire";
    public static final String CUSTOM_DIR_NAME = "millenaire-custom";
    public static final long MAX_JSON_BYTES = 1000000L;
    public static final long MAX_NBT_BYTES = 10000000L;
    public static final long MAX_TXT_BYTES = 512000L;
    public static final int MAX_FILES_PER_TYPE = 500;
    public static final int MAX_CUSTOM_CULTURES = 50;
    public static final int MAX_RECURSION_DEPTH = 7;
    private static volatile Path standardDir;
    private static volatile Path standardRootReal;
    private static volatile Path customDir;
    private static volatile Path customRootReal;

    private ContentDirectoryManager() {
    }

    public static synchronized void init(MinecraftServer server) {
        if (server == null) {
            throw new IllegalArgumentException("MinecraftServer is null");
        }
        ContentDirectoryManager.initFromGameDir(server.getServerDirectory());
    }

    public static synchronized void initClient(Path gameDir) {
        if (gameDir == null) {
            throw new IllegalArgumentException("gameDir is null");
        }
        if (standardRootReal != null && customRootReal != null) {
            try {
                if (standardRootReal.equals(gameDir.resolve(STANDARD_DIR_NAME).toRealPath(new LinkOption[0])) && customRootReal.equals(gameDir.resolve(CUSTOM_DIR_NAME).toRealPath(new LinkOption[0]))) {
                    return;
                }
            }
            catch (IOException iOException) {
                // empty catch block
            }
        }
        ContentDirectoryManager.initFromGameDir(gameDir);
    }

    private static void initFromGameDir(Path gameDir) {
        Path standardTarget = gameDir.resolve(STANDARD_DIR_NAME);
        Path customTarget = gameDir.resolve(CUSTOM_DIR_NAME);
        try {
            if (!Files.isDirectory(standardTarget, new LinkOption[0])) {
                Files.createDirectories(standardTarget, new FileAttribute[0]);
                LOGGER.info("Created standard content directory: {}", (Object)standardTarget);
            }
            if (!Files.isDirectory(customTarget, new LinkOption[0])) {
                Files.createDirectories(customTarget, new FileAttribute[0]);
                LOGGER.info("Created custom content directory: {}", (Object)customTarget);
            }
            standardDir = standardTarget;
            standardRootReal = standardTarget.toRealPath(new LinkOption[0]);
            customDir = customTarget;
            customRootReal = customTarget.toRealPath(new LinkOption[0]);
            LOGGER.info("Content directories initialised: standard={}, custom={}", (Object)standardRootReal, (Object)customRootReal);
        }
        catch (IOException e) {
            standardDir = null;
            standardRootReal = null;
            customDir = null;
            customRootReal = null;
            LOGGER.error("Failed to initialise content directories under {}: {}", new Object[]{gameDir, e.getMessage(), e});
        }
    }

    public static synchronized void resetForTesting() {
        standardDir = null;
        standardRootReal = null;
        customDir = null;
        customRootReal = null;
    }

    public static synchronized void initForTesting(Path gameDir) throws IOException {
        if (gameDir == null) {
            throw new IllegalArgumentException("gameDir is null");
        }
        Path standardTarget = gameDir.resolve(STANDARD_DIR_NAME);
        Path customTarget = gameDir.resolve(CUSTOM_DIR_NAME);
        Files.createDirectories(standardTarget, new FileAttribute[0]);
        Files.createDirectories(customTarget, new FileAttribute[0]);
        standardDir = standardTarget;
        standardRootReal = standardTarget.toRealPath(new LinkOption[0]);
        customDir = customTarget;
        customRootReal = customTarget.toRealPath(new LinkOption[0]);
    }

    public static boolean isInitialized() {
        return standardDir != null && customDir != null && standardRootReal != null && customRootReal != null;
    }

    public static Path getStandardDir() {
        Path dir = standardDir;
        if (dir == null) {
            throw new IllegalStateException("ContentDirectoryManager not initialised. Call init() first.");
        }
        return dir;
    }

    public static Path getCustomDir() {
        Path dir = customDir;
        if (dir == null) {
            throw new IllegalStateException("ContentDirectoryManager not initialised. Call init() first.");
        }
        return dir;
    }

    public static Path getStandardRootReal() {
        return standardRootReal;
    }

    public static Path getCustomRootReal() {
        return customRootReal;
    }

    public static Path getStandardCulturesDir() {
        return ContentDirectoryManager.getStandardDir().resolve("cultures");
    }

    public static Path safeResolve(Path path) {
        if (!ContentDirectoryManager.isInitialized() || path == null) {
            return null;
        }
        try {
            Path real = path.toRealPath(new LinkOption[0]);
            if (real.startsWith(standardRootReal) || real.startsWith(customRootReal)) {
                return real;
            }
            LOGGER.warn("Path escapes content directories and will be skipped: {}", (Object)path);
            return null;
        }
        catch (IOException e) {
            return null;
        }
    }

    public static boolean isInsideRoot(Path path) {
        return ContentDirectoryManager.safeResolve(path) != null;
    }

    public static Stream<Path> safeWalk(Path dir) throws IOException {
        if (!Files.isDirectory(dir, new LinkOption[0])) {
            return Stream.empty();
        }
        final ArrayList results = new ArrayList();
        Files.walkFileTree(dir, EnumSet.of(FileVisitOption.FOLLOW_LINKS), 7, (FileVisitor<? super Path>)new SimpleFileVisitor<Path>(){

            @Override
            public FileVisitResult preVisitDirectory(Path d, BasicFileAttributes attrs) {
                if (Files.isSymbolicLink(d) && ContentDirectoryManager.safeResolve(d) == null) {
                    LOGGER.warn("Skipping symlinked subtree pointing outside content directories: {}", (Object)d);
                    return FileVisitResult.SKIP_SUBTREE;
                }
                results.add(d);
                return FileVisitResult.CONTINUE;
            }

            @Override
            public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) {
                if (Files.isSymbolicLink(file) && ContentDirectoryManager.safeResolve(file) == null) {
                    LOGGER.warn("Skipping symlinked file pointing outside content directories: {}", (Object)file);
                    return FileVisitResult.CONTINUE;
                }
                results.add(file);
                return FileVisitResult.CONTINUE;
            }

            @Override
            public FileVisitResult visitFileFailed(Path file, IOException exc) {
                LOGGER.debug("Could not visit {}: {}", (Object)file, (Object)exc.getMessage());
                return FileVisitResult.CONTINUE;
            }
        });
        return results.stream();
    }

    public static boolean checkSize(Path file, long maxBytes) {
        try {
            if (!Files.exists(file, new LinkOption[0])) {
                return true;
            }
            long size = Files.size(file);
            if (size > maxBytes) {
                LOGGER.error("File {} exceeds size limit ({} bytes > {} bytes) \u2014 skipping", new Object[]{file, size, maxBytes});
                return false;
            }
            return true;
        }
        catch (IOException e) {
            LOGGER.error("Failed to read size of {}: {}", (Object)file, (Object)e.getMessage());
            return false;
        }
    }
}

