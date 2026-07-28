/*
 * Decompiled with CFR 0.150.
 * 
 * Could not load the following classes:
 *  org.slf4j.Logger
 */
package org.millenaire.diagnostics;

import java.util.ArrayList;
import java.util.List;
import org.millenaire.diagnostics.NavEvent;
import org.slf4j.Logger;

public final class NavigationEventLog {
    public static final int CAPACITY = 256;
    private final NavEvent[] buffer = new NavEvent[256];
    private int head;
    private int size;
    private long lastEventTick = -1L;
    private Logger watchLogger;
    private String watchPrefix = "";

    public void record(NavEvent event) {
        this.buffer[this.head] = event;
        this.head = (this.head + 1) % 256;
        if (this.size < 256) {
            ++this.size;
        }
        this.lastEventTick = event.tick();
        if (this.watchLogger != null) {
            this.watchLogger.info("{} {}", (Object)this.watchPrefix, (Object)event);
        }
    }

    public void record(long tick, NavEvent.Layer layer, NavEvent.Type type, String detail) {
        this.record(new NavEvent(tick, layer, type, detail == null ? "" : detail));
    }

    public List<NavEvent> snapshot() {
        ArrayList<NavEvent> out = new ArrayList<NavEvent>(this.size);
        int start = (this.head - this.size + 256) % 256;
        for (int i = 0; i < this.size; ++i) {
            out.add(this.buffer[(start + i) % 256]);
        }
        return out;
    }

    public int size() {
        return this.size;
    }

    public long lastEventTick() {
        return this.lastEventTick;
    }

    public int countRecent(NavEvent.Type type, long currentTick, long windowTicks) {
        int n = 0;
        int start = (this.head - this.size + 256) % 256;
        for (int i = 0; i < this.size; ++i) {
            NavEvent e = this.buffer[(start + i) % 256];
            if (e.type() != type || currentTick - e.tick() > windowTicks) continue;
            ++n;
        }
        return n;
    }

    public boolean hasStuckSinceLastStart() {
        int start = (this.head - this.size + 256) % 256;
        boolean stuckSeen = false;
        for (int i = 0; i < this.size; ++i) {
            NavEvent e = this.buffer[(start + i) % 256];
            if (e.type() == NavEvent.Type.NAV_START) {
                stuckSeen = false;
                continue;
            }
            if (e.type() != NavEvent.Type.STUCK_DETECTED) continue;
            stuckSeen = true;
        }
        return stuckSeen;
    }

    public void enableWatch(Logger logger, String prefix) {
        this.watchLogger = logger;
        this.watchPrefix = prefix == null ? "" : prefix;
    }

    public void disableWatch() {
        this.watchLogger = null;
        this.watchPrefix = "";
    }

    public boolean isWatched() {
        return this.watchLogger != null;
    }
}

