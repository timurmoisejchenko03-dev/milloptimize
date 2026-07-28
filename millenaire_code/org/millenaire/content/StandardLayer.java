/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.mojang.logging.LogUtils
 *  org.slf4j.Logger
 */
package org.millenaire.content;

import com.mojang.logging.LogUtils;
import java.io.IOException;
import java.nio.file.FileVisitOption;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.stream.Stream;
import org.millenaire.content.ContentFs;
import org.millenaire.content.LayerEntry;
import org.millenaire.content.Resource;
import org.millenaire.content.SourceKind;
import org.millenaire.content.SourceLabel;
import org.slf4j.Logger;

final class StandardLayer {
    private static final Logger LOGGER = LogUtils.getLogger();
    static final SourceLabel LABEL = new SourceLabel("standard");

    private StandardLayer() {
    }

    static List<LayerEntry> scan(Path root) {
        if (root == null || !Files.isDirectory(root, new LinkOption[0])) {
            return List.of();
        }
        ArrayList entries = new ArrayList();
        try (Stream<Path> stream = Files.walk(root, new FileVisitOption[0]);){
            stream.filter(x$0 -> Files.isRegularFile(x$0, new LinkOption[0])).forEach(path -> entries.add(StandardLayer.toEntry(root, path)));
        }
        catch (IOException e) {
            LOGGER.error("Failed to scan standard layer at {}: {}", new Object[]{root, e.getMessage(), e});
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
        return new LayerEntry(relPath, original, size, LABEL, SourceKind.STANDARD, (rel, owner) -> new Resource.FileResource((String)rel, LABEL, SourceKind.STANDARD, path, (ContentFs)owner));
    }
}

