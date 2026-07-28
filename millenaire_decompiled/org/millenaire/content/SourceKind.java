/*
 * Decompiled with CFR 0.150.
 */
package org.millenaire.content;

public final class SourceKind
extends Enum<SourceKind> {
    public static final /* enum */ SourceKind CLASSPATH = new SourceKind();
    public static final /* enum */ SourceKind STANDARD = new SourceKind();
    public static final /* enum */ SourceKind SUBMOD = new SourceKind();
    private static final /* synthetic */ SourceKind[] $VALUES;

    public static SourceKind[] values() {
        return (SourceKind[])$VALUES.clone();
    }

    public static SourceKind valueOf(String name) {
        return Enum.valueOf(SourceKind.class, name);
    }

    private static /* synthetic */ SourceKind[] $values() {
        return new SourceKind[]{CLASSPATH, STANDARD, SUBMOD};
    }

    static {
        $VALUES = SourceKind.$values();
    }
}

