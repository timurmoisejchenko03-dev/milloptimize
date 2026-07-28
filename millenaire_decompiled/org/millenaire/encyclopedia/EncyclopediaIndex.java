/*
 * Decompiled with CFR 0.150.
 */
package org.millenaire.encyclopedia;

import java.util.List;
import org.millenaire.encyclopedia.IndexEntry;

public record EncyclopediaIndex(int schemaVersion, String modVersion, String generatedAt, List<IndexEntry> items) {
    public static final int SCHEMA_VERSION = 1;
}

