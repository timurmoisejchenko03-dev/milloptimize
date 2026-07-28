/*
 * Decompiled with CFR 0.152.
 */
package org.millenaire.content;

import java.util.regex.Pattern;

public final class CultureIdPolicy {
    public static final Pattern PATTERN = Pattern.compile("[a-z0-9_-]+");

    private CultureIdPolicy() {
    }
}

