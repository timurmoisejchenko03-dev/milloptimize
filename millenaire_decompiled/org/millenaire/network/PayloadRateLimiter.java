/*
 * Decompiled with CFR 0.150.
 */
package org.millenaire.network;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

final class PayloadRateLimiter {
    static final int MAX_TRACKED = 10000;
    private final Map<UUID, Long> lastAcceptMs = new ConcurrentHashMap<UUID, Long>();
    private final long minIntervalMs;

    PayloadRateLimiter(long minIntervalMs) {
        if (minIntervalMs < 0L) {
            throw new IllegalArgumentException("minIntervalMs must be >= 0");
        }
        this.minIntervalMs = minIntervalMs;
    }

    boolean acquire(UUID playerId) {
        return this.acquire(playerId, System.currentTimeMillis());
    }

    boolean acquire(UUID playerId, long nowMs) {
        Long prev = this.lastAcceptMs.get(playerId);
        if (prev != null && nowMs - prev < this.minIntervalMs) {
            return false;
        }
        this.lastAcceptMs.put(playerId, nowMs);
        if (this.lastAcceptMs.size() > 10000) {
            long staleCutoff = nowMs - this.minIntervalMs;
            this.lastAcceptMs.entrySet().removeIf(e -> (Long)e.getValue() < staleCutoff);
        }
        return true;
    }

    long getMinIntervalMs() {
        return this.minIntervalMs;
    }

    int getTrackedCount() {
        return this.lastAcceptMs.size();
    }
}

