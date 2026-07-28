/*
 * Decompiled with CFR 0.152.
 */
package org.millenaire.content.legacy;

public enum ConversionMode {
    AUTO,
    CONVERT;


    public boolean isStrict() {
        return this == CONVERT;
    }
}

