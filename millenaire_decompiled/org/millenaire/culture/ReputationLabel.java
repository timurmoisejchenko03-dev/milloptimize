/*
 * Decompiled with CFR 0.150.
 */
package org.millenaire.culture;

public record ReputationLabel(int threshold, String key) {
    @Override
    public int compareTo(ReputationLabel o) {
        return Integer.compare(this.threshold, o.threshold);
    }
}

