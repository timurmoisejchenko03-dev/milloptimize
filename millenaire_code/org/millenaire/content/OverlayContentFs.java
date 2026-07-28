/*
 * Decompiled with CFR 0.152.
 */
package org.millenaire.content;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Stream;
import org.millenaire.content.ContentFs;
import org.millenaire.content.Resource;

final class OverlayContentFs
implements ContentFs {
    private final LinkedHashMap<String, Resource> overlay;
    private final Map<String, List<Resource>> history;
    private final String prefix;

    OverlayContentFs(LinkedHashMap<String, Resource> overlay, Map<String, List<Resource>> history) {
        this(overlay, history, "");
    }

    private OverlayContentFs(LinkedHashMap<String, Resource> overlay, Map<String, List<Resource>> history, String prefix) {
        this.overlay = overlay;
        this.history = history;
        this.prefix = prefix;
    }

    @Override
    public Optional<Resource> findFirst(String relPath) {
        if (relPath == null) {
            return Optional.empty();
        }
        String key = (this.prefix + OverlayContentFs.normalise(relPath)).toLowerCase(Locale.ROOT);
        return Optional.ofNullable(this.overlay.get(key));
    }

    @Override
    public List<Resource> findAll(String relPath) {
        if (relPath == null) {
            return List.of();
        }
        String key = (this.prefix + OverlayContentFs.normalise(relPath)).toLowerCase(Locale.ROOT);
        List<Resource> hist = this.history.get(key);
        return hist == null ? List.of() : hist;
    }

    @Override
    public Stream<Resource> walk(String relDir, int maxDepth) {
        String norm = OverlayContentFs.normalise(relDir);
        Object base = norm.isEmpty() ? OverlayContentFs.stripTrailingSlash(this.prefix) : this.prefix + norm;
        String dir = ((String)base).toLowerCase(Locale.ROOT);
        Object matchPrefix = dir.isEmpty() ? "" : dir + "/";
        boolean unbounded = maxDepth < 0 || maxDepth == Integer.MAX_VALUE;
        int prefixSlashes = OverlayContentFs.countSlashes((String)matchPrefix);
        return this.overlay.keySet().stream().filter(arg_0 -> OverlayContentFs.lambda$walk$0((String)matchPrefix, arg_0)).filter(k -> {
            if (unbounded) {
                return true;
            }
            int slashes = OverlayContentFs.countSlashes(k) - prefixSlashes;
            return slashes <= maxDepth;
        }).map(this.overlay::get);
    }

    @Override
    public ContentFs sub(String relPath) {
        String norm = OverlayContentFs.normalise(relPath);
        String newPrefix = norm.isEmpty() ? this.prefix : this.prefix + norm + "/";
        return new OverlayContentFs(this.overlay, this.history, newPrefix.toLowerCase(Locale.ROOT));
    }

    private static String normalise(String relPath) {
        int j;
        int i;
        if (relPath == null) {
            return "";
        }
        String s = relPath;
        for (i = 0; i < s.length() && s.charAt(i) == '/'; ++i) {
        }
        if (i > 0) {
            s = s.substring(i);
        }
        for (j = s.length(); j > 0 && s.charAt(j - 1) == '/'; --j) {
        }
        if (j < s.length()) {
            s = s.substring(0, j);
        }
        return s;
    }

    private static int countSlashes(String s) {
        int n = 0;
        for (int i = 0; i < s.length(); ++i) {
            if (s.charAt(i) != '/') continue;
            ++n;
        }
        return n;
    }

    private static String stripTrailingSlash(String s) {
        if (s.isEmpty() || s.charAt(s.length() - 1) != '/') {
            return s;
        }
        return s.substring(0, s.length() - 1);
    }

    private static /* synthetic */ boolean lambda$walk$0(String matchPrefix, String k) {
        return matchPrefix.isEmpty() || k.startsWith(matchPrefix);
    }
}

