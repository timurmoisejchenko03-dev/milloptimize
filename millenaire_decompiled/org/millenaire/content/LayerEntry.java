/*
 * Decompiled with CFR 0.150.
 */
package org.millenaire.content;

import java.util.function.BiFunction;
import org.millenaire.content.ContentFs;
import org.millenaire.content.Resource;
import org.millenaire.content.SourceKind;
import org.millenaire.content.SourceLabel;

record LayerEntry(String relPath, String originalRelPath, long size, SourceLabel source, SourceKind kind, BiFunction<String, ContentFs, Resource> materialiser) {
    LayerEntry {
        if (relPath == null) {
            throw new IllegalArgumentException("relPath null");
        }
        if (originalRelPath == null) {
            throw new IllegalArgumentException("originalRelPath null");
        }
        if (source == null) {
            throw new IllegalArgumentException("source null");
        }
        if (kind == null) {
            throw new IllegalArgumentException("kind null");
        }
        if (materialiser == null) {
            throw new IllegalArgumentException("materialiser null");
        }
    }
}

