/*
 * Decompiled with CFR 0.152.
 */
package org.millenaire.content;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;
import org.millenaire.content.ContentFs;
import org.millenaire.content.Resource;

public final class ChainedContentFs
implements ContentFs {
    private final ContentFs primary;
    private final ContentFs fallback;

    public ChainedContentFs(ContentFs primary, ContentFs fallback) {
        if (primary == null || fallback == null) {
            throw new IllegalArgumentException("primary and fallback must be non-null");
        }
        this.primary = primary;
        this.fallback = fallback;
    }

    @Override
    public Optional<Resource> findFirst(String relPath) {
        Optional<Resource> hit = this.primary.findFirst(relPath);
        return hit.isPresent() ? hit : this.fallback.findFirst(relPath);
    }

    @Override
    public List<Resource> findAll(String relPath) {
        List<Resource> primaryList = this.primary.findAll(relPath);
        List<Resource> fallbackList = this.fallback.findAll(relPath);
        if (primaryList.isEmpty()) {
            return fallbackList;
        }
        if (fallbackList.isEmpty()) {
            return primaryList;
        }
        ArrayList<Resource> merged = new ArrayList<Resource>(primaryList.size() + fallbackList.size());
        merged.addAll(primaryList);
        merged.addAll(fallbackList);
        return List.copyOf(merged);
    }

    @Override
    public Stream<Resource> walk(String relDir, int maxDepth) {
        HashSet seen = new HashSet();
        Stream<Resource> primaryStream = this.primary.walk(relDir, maxDepth).peek(r -> seen.add(r.relPath()));
        Stream<Resource> fallbackStream = this.fallback.walk(relDir, maxDepth).filter(r -> !seen.contains(r.relPath()));
        return Stream.concat(primaryStream, fallbackStream);
    }

    @Override
    public ContentFs sub(String relPath) {
        return new ChainedContentFs(this.primary.sub(relPath), this.fallback.sub(relPath));
    }
}

