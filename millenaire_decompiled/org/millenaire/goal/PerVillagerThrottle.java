/*
 * Decompiled with CFR 0.150.
 */
package org.millenaire.goal;

import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public final class PerVillagerThrottle {
    private final int delayTicks;
    private final ConcurrentHashMap<UUID, Long> lastEvalTick = new ConcurrentHashMap();
    private static final long DEFAULT_EXPIRY_TICKS = 12000L;
    private static final long DEFAULT_CLEANUP_INTERVAL = 2400L;
    private final long expiryTicks;
    private final long cleanupInterval;
    private long lastCleanupTick = 0L;

    public PerVillagerThrottle(int delayTicks) {
        this(delayTicks, 12000L, 2400L);
    }

    public PerVillagerThrottle(int delayTicks, long expiryTicks, long cleanupInterval) {
        this.delayTicks = delayTicks;
        this.expiryTicks = expiryTicks;
        this.cleanupInterval = cleanupInterval;
    }

    public boolean shouldEvaluate(UUID villagerUuid, long currentTick) {
        this.runCleanupIfNeeded(currentTick);
        Long last = this.lastEvalTick.get(villagerUuid);
        if (last != null && currentTick - last < (long)this.delayTicks) {
            return false;
        }
        this.lastEvalTick.put(villagerUuid, currentTick);
        return true;
    }

    public boolean isThrottled(UUID villagerUuid, long currentTick) {
        this.runCleanupIfNeeded(currentTick);
        Long last = this.lastEvalTick.get(villagerUuid);
        return last != null && currentTick - last < (long)this.delayTicks;
    }

    public void record(UUID villagerUuid, long currentTick) {
        this.lastEvalTick.put(villagerUuid, currentTick);
    }

    private void runCleanupIfNeeded(long currentTick) {
        if (currentTick - this.lastCleanupTick >= this.cleanupInterval) {
            this.cleanup(currentTick);
            this.lastCleanupTick = currentTick;
        }
    }

    public void cleanup(long currentTick) {
        this.lastEvalTick.entrySet().removeIf(entry -> currentTick - (Long)entry.getValue() > this.expiryTicks);
    }
}

