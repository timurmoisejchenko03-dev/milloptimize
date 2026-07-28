/*
 * Decompiled with CFR 0.152.
 */
package org.millenaire.content;

import java.nio.file.Path;

public record SubmodRoot(String name, Path root) {
    public String displayName() {
        return this.name;
    }
}

