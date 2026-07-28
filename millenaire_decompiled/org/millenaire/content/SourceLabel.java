/*
 * Decompiled with CFR 0.150.
 */
package org.millenaire.content;

public record SourceLabel(String displayName) {
    public SourceLabel {
        if (displayName == null || displayName.isEmpty()) {
            throw new IllegalArgumentException("SourceLabel.displayName must be non-empty");
        }
    }
}

