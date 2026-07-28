/*
 * Decompiled with CFR 0.150.
 */
package org.millenaire.village.path;

import java.util.Arrays;

public final class PathProfileOptimizer {
    private PathProfileOptimizer() {
    }

    public static int[] optimize(int[] groundHalfY, Weights w) {
        int n = groundHalfY.length;
        if (n == 0) {
            return new int[0];
        }
        if (n == 1) {
            return new int[]{groundHalfY[0]};
        }
        int minG = Integer.MAX_VALUE;
        int maxG = Integer.MIN_VALUE;
        for (int g : groundHalfY) {
            minG = Math.min(minG, g);
            maxG = Math.max(maxG, g);
        }
        int lo = minG - w.halfYMargin;
        int hi = maxG + w.halfYMargin;
        int range = hi - lo + 1;
        int deltaDim = 5;
        double INF = Double.POSITIVE_INFINITY;
        double[][][] dp = new double[n][range][5];
        int[][][] back = new int[n][range][5];
        double[][][] arrd = dp;
        int n2 = arrd.length;
        for (int i = 0; i < n2; ++i) {
            double[][] a;
            for (double[] b : a = arrd[i]) {
                Arrays.fill(b, Double.POSITIVE_INFINITY);
            }
        }
        int startG = groundHalfY[0];
        for (int h = Math.max(lo, startG - 1); h <= Math.min(hi, startG + 1); ++h) {
            int idx = h - lo;
            dp[0][idx][2] = PathProfileOptimizer.groundDeviationCost(h, startG, w);
        }
        for (int i = 1; i < n; ++i) {
            int gi = groundHalfY[i];
            for (int idx = 0; idx < range; ++idx) {
                int h = idx + lo;
                double gCost = PathProfileOptimizer.groundDeviationCost(h, gi, w);
                for (int delta = -2; delta <= 2; ++delta) {
                    int prevH = h - delta;
                    int prevIdx = prevH - lo;
                    if (prevIdx < 0 || prevIdx >= range) continue;
                    double stepCost = w.slope * (double)Math.abs(delta);
                    if (Math.abs(delta) == 2) {
                        stepCost += w.jump;
                    }
                    for (int prevDeltaIdx = 0; prevDeltaIdx < 5; ++prevDeltaIdx) {
                        int newDeltaIdx;
                        double tot;
                        double prev = dp[i - 1][prevIdx][prevDeltaIdx];
                        if (prev == Double.POSITIVE_INFINITY) continue;
                        int prevDelta = prevDeltaIdx - 2;
                        double invCost = 0.0;
                        if (prevDelta != 0 && delta != 0 && prevDelta * delta < 0) {
                            invCost = w.inversion;
                        }
                        if (!((tot = prev + stepCost + invCost + gCost) < dp[i][idx][newDeltaIdx = delta + 2])) continue;
                        dp[i][idx][newDeltaIdx] = tot;
                        back[i][idx][newDeltaIdx] = prevIdx * 5 + prevDeltaIdx;
                    }
                }
            }
        }
        int endG = groundHalfY[n - 1];
        double best = Double.POSITIVE_INFINITY;
        int bestIdx = -1;
        int bestDeltaIdx = -1;
        for (int h : new int[]{endG, endG - 1, endG + 1}) {
            if (h < lo || h > hi) continue;
            int idx = h - lo;
            for (int d = 0; d < 5; ++d) {
                if (!(dp[n - 1][idx][d] < best)) continue;
                best = dp[n - 1][idx][d];
                bestIdx = idx;
                bestDeltaIdx = d;
            }
        }
        if (bestIdx < 0) {
            return (int[])groundHalfY.clone();
        }
        int[] out = new int[n];
        int curIdx = bestIdx;
        int curDelta = bestDeltaIdx;
        for (int i = n - 1; i >= 0; --i) {
            out[i] = curIdx + lo;
            if (i <= 0) continue;
            int packed = back[i][curIdx][curDelta];
            curIdx = packed / 5;
            curDelta = packed % 5;
        }
        return out;
    }

    private static double groundDeviationCost(int surface, int ground, Weights w) {
        int d = surface - ground;
        return d >= 0 ? w.fill * (double)d : w.cut * (double)(-d);
    }

    public record Weights(double fill, double cut, double slope, double inversion, double jump, int halfYMargin) {
        public static Weights defaults() {
            return new Weights(1.0, 1.5, 0.5, 6.0, 10.0, 8);
        }
    }
}

