/*
 * Decompiled with CFR 0.150.
 * 
 * Could not load the following classes:
 *  com.google.gson.JsonElement
 *  com.google.gson.JsonParser
 *  com.mojang.logging.LogUtils
 *  org.slf4j.Logger
 */
package org.millenaire.content;

import com.google.gson.JsonElement;
import com.google.gson.JsonParser;
import com.mojang.logging.LogUtils;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.OpenOption;
import java.nio.file.Path;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import org.millenaire.content.ContentDirectoryManager;
import org.slf4j.Logger;

public final class DisabledIdsLoader {
    private static final Logger LOGGER = LogUtils.getLogger();
    private static final String MANIFEST_NAME = "_disabled.json";

    private DisabledIdsLoader() {
    }

    /*
     * Enabled aggressive block sorting
     * Enabled unnecessary exception pruning
     * Enabled aggressive exception aggregation
     */
    public static Set<String> load(Path contentTypeDir) {
        if (contentTypeDir == null) {
            return Collections.emptySet();
        }
        Path manifest = contentTypeDir.resolve(MANIFEST_NAME);
        if (!Files.isRegularFile(manifest, new LinkOption[0])) {
            return Collections.emptySet();
        }
        if (!ContentDirectoryManager.checkSize(manifest, 1000000L)) {
            return Collections.emptySet();
        }
        try (InputStream in = Files.newInputStream(manifest, new OpenOption[0]);){
            String json = new String(in.readAllBytes(), StandardCharsets.UTF_8);
            JsonElement root = JsonParser.parseString((String)json);
            if (!root.isJsonArray()) {
                LOGGER.warn("{} must be a JSON array of strings; ignoring", (Object)manifest);
                Set<String> set = Collections.emptySet();
                return set;
            }
            HashSet<String> out = new HashSet<String>();
            for (JsonElement e : root.getAsJsonArray()) {
                if (!e.isJsonPrimitive() || !e.getAsJsonPrimitive().isString()) continue;
                out.add(e.getAsString());
            }
            HashSet<String> hashSet = out;
            return hashSet;
        }
        catch (Exception e) {
            LOGGER.warn("Could not parse {}: {}", (Object)manifest, (Object)e.getMessage());
            return Collections.emptySet();
        }
    }

    public static Set<String> loadUnion(List<Path> typeDirs) {
        if (typeDirs == null || typeDirs.isEmpty()) {
            return Collections.emptySet();
        }
        HashSet<String> out = new HashSet<String>();
        for (Path dir : typeDirs) {
            if (dir == null) continue;
            out.addAll(DisabledIdsLoader.load(dir));
        }
        return out;
    }
}

