/*
 * Decompiled with CFR 0.150.
 * 
 * Could not load the following classes:
 *  com.mojang.logging.LogUtils
 *  net.neoforged.fml.ModContainer
 *  net.neoforged.fml.ModList
 *  net.neoforged.neoforgespi.locating.IModFile
 *  org.slf4j.Logger
 */
package org.millenaire.content;

import com.mojang.logging.LogUtils;
import java.io.IOException;
import java.net.URL;
import java.nio.file.FileVisitOption;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.stream.Stream;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.ModList;
import net.neoforged.neoforgespi.locating.IModFile;
import org.millenaire.content.ContentFs;
import org.millenaire.content.LayerEntry;
import org.millenaire.content.Resource;
import org.millenaire.content.SourceKind;
import org.millenaire.content.SourceLabel;
import org.slf4j.Logger;

final class ClasspathLayer {
    private static final Logger LOGGER = LogUtils.getLogger();
    static final SourceLabel LABEL = new SourceLabel("millenaire (jar)");
    static final String JAR_CONTENT_ROOT = "millenaire";

    private ClasspathLayer() {
    }

    static Path resolveDefaultRoot() {
        if (ClasspathLayer.isModListAvailable()) {
            return ClasspathLayer.resolveFromModList();
        }
        return ClasspathLayer.resolveFromClassloader();
    }

    private static boolean isModListAvailable() {
        ModList list;
        try {
            list = ModList.get();
        }
        catch (LinkageError t) {
            LOGGER.debug("ModList class not available, switching to classloader fallback: {}", (Object)t.getMessage());
            return false;
        }
        return list != null;
    }

    private static Path resolveFromModList() {
        ModContainer container = (ModContainer)ModList.get().getModContainerById(JAR_CONTENT_ROOT).orElseThrow();
        IModFile modFile = container.getModInfo().getOwningFile().getFile();
        Path jarRoot = modFile.getSecureJar().getRootPath();
        Path contentRoot = jarRoot.resolve(JAR_CONTENT_ROOT);
        return Files.isDirectory(contentRoot, new LinkOption[0]) ? contentRoot : null;
    }

    private static Path resolveFromClassloader() {
        try {
            URL url = ClasspathLayer.class.getResource("/millenaire");
            if (url == null) {
                return null;
            }
            Path contentRoot = Path.of(url.toURI());
            return Files.isDirectory(contentRoot, new LinkOption[0]) ? contentRoot : null;
        }
        catch (Exception t) {
            LOGGER.debug("Classloader fallback for JAR content root failed: {}", (Object)t.getMessage());
            return null;
        }
    }

    static List<LayerEntry> scan(Path root) {
        if (root == null || !Files.isDirectory(root, new LinkOption[0])) {
            return List.of();
        }
        ArrayList entries = new ArrayList();
        try (Stream<Path> stream = Files.walk(root, new FileVisitOption[0]);){
            stream.filter(x$0 -> Files.isRegularFile(x$0, new LinkOption[0])).forEach(path -> entries.add(ClasspathLayer.toEntry(root, path)));
        }
        catch (IOException e) {
            LOGGER.error("Failed to scan classpath layer at {}: {}", new Object[]{root, e.getMessage(), e});
            return List.of();
        }
        entries.sort((a, b) -> a.relPath().compareTo(b.relPath()));
        return Collections.unmodifiableList(entries);
    }

    private static LayerEntry toEntry(Path root, Path path) {
        long size;
        String original = root.relativize(path).toString().replace('\\', '/');
        String relPath = original.toLowerCase(Locale.ROOT);
        try {
            size = Files.size(path);
        }
        catch (IOException e) {
            LOGGER.warn("Could not stat {}: {}", (Object)path, (Object)e.getMessage());
            size = -1L;
        }
        return new LayerEntry(relPath, original, size, LABEL, SourceKind.CLASSPATH, (rel, owner) -> new Resource.ClasspathResource((String)rel, LABEL, SourceKind.CLASSPATH, path, (ContentFs)owner));
    }
}

