/*
 * Decompiled with CFR 0.150.
 */
package org.millenaire.village.path;

public final class PathFailureReason
extends Enum<PathFailureReason> {
    public static final /* enum */ PathFailureReason NO_PATH_START = new PathFailureReason();
    public static final /* enum */ PathFailureReason FALLBACK_USED = new PathFailureReason();
    public static final /* enum */ PathFailureReason NO_PATHS_TAG = new PathFailureReason();
    public static final /* enum */ PathFailureReason A_STAR_FAILED = new PathFailureReason();
    public static final /* enum */ PathFailureReason UNREACHABLE_TERRAIN = new PathFailureReason();
    public static final /* enum */ PathFailureReason UNKNOWN_MATERIAL = new PathFailureReason();
    public static final /* enum */ PathFailureReason PLACEMENT_EMPTY = new PathFailureReason();
    public static final /* enum */ PathFailureReason NO_TRACE_PLACED = new PathFailureReason();
    private static final /* synthetic */ PathFailureReason[] $VALUES;

    public static PathFailureReason[] values() {
        return (PathFailureReason[])$VALUES.clone();
    }

    public static PathFailureReason valueOf(String name) {
        return Enum.valueOf(PathFailureReason.class, name);
    }

    private static /* synthetic */ PathFailureReason[] $values() {
        return new PathFailureReason[]{NO_PATH_START, FALLBACK_USED, NO_PATHS_TAG, A_STAR_FAILED, UNREACHABLE_TERRAIN, UNKNOWN_MATERIAL, PLACEMENT_EMPTY, NO_TRACE_PLACED};
    }

    static {
        $VALUES = PathFailureReason.$values();
    }
}

