/*
 * Decompiled with CFR 0.152.
 */
package org.millenaire.encyclopedia;

import java.util.List;
import org.millenaire.encyclopedia.LocalizedText;

public record IndexEntry(String itemRef, String cultureSlug, String type, String itemKey, LocalizedText label, String nativePrefix, String iconKey, String category, LocalizedText categoryLabel, int displayOrder, List<String> residents) {
}

