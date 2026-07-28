/*
 * Decompiled with CFR 0.152.
 */
package org.millenaire.language;

import java.util.Random;

public final class SentenceRenderer {
    private static final String MASK = "???";
    private static final int MASKING_START = 100;
    private static final int MASKING_END = 500;
    private static final int MASKING_RANGE = 400;

    private SentenceRenderer() {
    }

    public static double languageRatio(int score) {
        if (score < 100) {
            return 0.0;
        }
        if (score >= 500) {
            return 1.0;
        }
        return (double)(score - 100) / 400.0;
    }

    public static String maskTranslation(String translation, double visibilityRatio) {
        if (visibilityRatio >= 1.0) {
            return translation;
        }
        String[] words = translation.split(" ");
        if (words.length == 0) {
            return translation;
        }
        Random rng = new Random(translation.hashCode());
        double[] thresholds = new double[words.length];
        for (int i = 0; i < words.length; ++i) {
            thresholds[i] = rng.nextDouble();
        }
        StringBuilder result = new StringBuilder();
        for (int i = 0; i < words.length; ++i) {
            if (i > 0) {
                result.append(' ');
            }
            if (visibilityRatio >= thresholds[i]) {
                result.append(words[i]);
                continue;
            }
            result.append(MASK);
        }
        return result.toString();
    }
}

