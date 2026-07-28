/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.mojang.logging.LogUtils
 *  org.slf4j.Logger
 */
package org.millenaire.content;

import com.mojang.logging.LogUtils;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.millenaire.content.ContentFs;
import org.millenaire.content.LayerEntry;
import org.millenaire.content.OverlayContentFs;
import org.millenaire.content.Resource;
import org.slf4j.Logger;

final class OverlayBuilder {
    private static final Logger LOGGER = LogUtils.getLogger();
    private final LinkedHashMap<String, LayerEntry> overlayEntries = new LinkedHashMap();
    private final Map<String, List<LayerEntry>> historyEntries = new LinkedHashMap<String, List<LayerEntry>>();
    private final Map<String, Integer> overrideCounts = new LinkedHashMap<String, Integer>();
    private final boolean silent;

    OverlayBuilder() {
        this(false);
    }

    OverlayBuilder(boolean silent) {
        this.silent = silent;
    }

    void addLayer(List<LayerEntry> entries) {
        this.addLayer(entries, false);
    }

    void addSubmodLayer(List<LayerEntry> entriesIncludingOversize) {
        this.addLayer(entriesIncludingOversize, true);
    }

    private void addLayer(List<LayerEntry> entries, boolean enforceSizeCap) {
        for (LayerEntry entry : entries) {
            String key = entry.relPath();
            if (enforceSizeCap && entry.size() > 10000000L) {
                if (this.silent) continue;
                LayerEntry existing = this.overlayEntries.get(key);
                if (existing != null) {
                    LOGGER.warn("Rejected oversize override {} from {}; keeping {}", new Object[]{entry.relPath(), entry.source().displayName(), existing.source().displayName()});
                    continue;
                }
                LOGGER.warn("Rejected oversize file {} from {} ({} bytes > {} bytes)", new Object[]{entry.relPath(), entry.source().displayName(), entry.size(), 10000000L});
                continue;
            }
            this.insert(key, entry);
        }
    }

    private void insert(String key, LayerEntry entry) {
        LayerEntry existing = this.overlayEntries.get(key);
        if (existing != null && !this.silent) {
            if (!existing.originalRelPath().equals(entry.originalRelPath())) {
                LOGGER.warn("Case-only collision: '{}' ({}) overlays '{}' ({})", new Object[]{entry.originalRelPath(), entry.source().displayName(), existing.originalRelPath(), existing.source().displayName()});
            }
            if (!existing.source().equals(entry.source())) {
                LOGGER.debug("{}: {} overrides {}", new Object[]{key, entry.source().displayName(), existing.source().displayName()});
                String pair = entry.source().displayName() + " over " + existing.source().displayName();
                this.overrideCounts.merge(pair, 1, Integer::sum);
            }
        }
        this.overlayEntries.put(key, entry);
        this.historyEntries.computeIfAbsent(key, k -> new ArrayList(2)).add(0, entry);
    }

    ContentFs build() {
        if (!this.silent && !this.overrideCounts.isEmpty()) {
            for (Map.Entry<String, Integer> e : this.overrideCounts.entrySet()) {
                LOGGER.info("Overlay: {} ({} files; -Dlog.level.OverlayBuilder=DEBUG for per-file detail)", (Object)e.getKey(), (Object)e.getValue());
            }
        }
        LinkedHashMap<String, Resource> overlay = new LinkedHashMap<String, Resource>(this.overlayEntries.size());
        LinkedHashMap<String, List<Resource>> history = new LinkedHashMap<String, List<Resource>>(this.historyEntries.size());
        OverlayContentFs fs = new OverlayContentFs(overlay, history);
        for (Map.Entry<String, LayerEntry> entry : this.overlayEntries.entrySet()) {
            LayerEntry le = entry.getValue();
            overlay.put(entry.getKey(), le.materialiser().apply(le.relPath(), fs));
        }
        for (Map.Entry<String, Object> entry : this.historyEntries.entrySet()) {
            List src = (List)entry.getValue();
            ArrayList<Resource> dst = new ArrayList<Resource>(src.size());
            for (LayerEntry le : src) {
                dst.add(le.materialiser().apply(le.relPath(), fs));
            }
            history.put(entry.getKey(), List.copyOf(dst));
        }
        return fs;
    }
}

