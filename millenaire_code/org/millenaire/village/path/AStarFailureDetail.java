/*
 * Decompiled with CFR 0.152.
 */
package org.millenaire.village.path;

public record AStarFailureDetail(int nodesExploredDefault, int nodesExploredRelaxed, int nodesExploredPermissive, int rejectedStep, int rejectedTraversable, int rejectedByFootprint, String defaultReason, String relaxedReason, String permissiveReason) {
    public static AStarFailureDetail empty() {
        return new AStarFailureDetail(0, 0, 0, 0, 0, 0, null, null, null);
    }
}

