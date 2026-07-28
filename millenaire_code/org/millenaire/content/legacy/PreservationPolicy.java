/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.google.gson.JsonElement
 *  com.google.gson.JsonObject
 *  com.google.gson.JsonParser
 *  com.google.gson.JsonSyntaxException
 *  com.mojang.logging.LogUtils
 *  org.slf4j.Logger
 */
package org.millenaire.content.legacy;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.JsonSyntaxException;
import com.mojang.logging.LogUtils;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;
import org.slf4j.Logger;

public final class PreservationPolicy {
    private static final Logger LOGGER = LogUtils.getLogger();

    private PreservationPolicy() {
    }

    public static Decision checkVillageType(Path target, Map<String, Object> builtJson) {
        String k;
        if (!Files.isRegularFile(target, new LinkOption[0])) {
            return Decision.overwriteAsIs();
        }
        JsonObject existing = PreservationPolicy.readObject(target);
        if (existing == null) {
            return Decision.overwriteAsIs();
        }
        if (existing.has("converter_skip") && existing.get("converter_skip").isJsonPrimitive() && existing.getAsJsonPrimitive("converter_skip").isBoolean() && existing.getAsJsonPrimitive("converter_skip").getAsBoolean()) {
            return Decision.skipConverterPinned(target.toString());
        }
        JsonElement biomeTags = existing.get("biome_tags");
        if (biomeTags == null || !biomeTags.isJsonArray()) {
            return Decision.overwriteAsIs();
        }
        ArrayList tags = new ArrayList();
        biomeTags.getAsJsonArray().forEach(e -> {
            if (e.isJsonPrimitive() && e.getAsJsonPrimitive().isString()) {
                tags.add(e.getAsString());
            }
        });
        String precedingKey = null;
        Iterator iterator = existing.keySet().iterator();
        while (iterator.hasNext() && !"biome_tags".equals(k = (String)iterator.next())) {
            precedingKey = k;
        }
        LinkedHashMap<String, Object> reordered = new LinkedHashMap<String, Object>();
        boolean inserted = false;
        for (Map.Entry<String, Object> e2 : builtJson.entrySet()) {
            if ("biome_tags".equals(e2.getKey())) continue;
            reordered.put(e2.getKey(), e2.getValue());
            if (inserted || !e2.getKey().equals(precedingKey)) continue;
            reordered.put("biome_tags", tags);
            inserted = true;
        }
        if (!inserted) {
            reordered.put("biome_tags", tags);
        }
        return Decision.overwriteWithBiomeTags(reordered);
    }

    public static Decision checkConverterSkip(Path target) {
        if (!Files.isRegularFile(target, new LinkOption[0])) {
            return Decision.overwriteAsIs();
        }
        JsonObject existing = PreservationPolicy.readObject(target);
        if (existing == null) {
            return Decision.overwriteAsIs();
        }
        if (existing.has("converter_skip") && existing.get("converter_skip").isJsonPrimitive() && existing.getAsJsonPrimitive("converter_skip").isBoolean() && existing.getAsJsonPrimitive("converter_skip").getAsBoolean()) {
            return Decision.skipConverterPinned(target.toString());
        }
        return Decision.overwriteAsIs();
    }

    private static JsonObject readObject(Path target) {
        try {
            JsonElement el = JsonParser.parseString((String)Files.readString(target, StandardCharsets.UTF_8));
            return el.isJsonObject() ? el.getAsJsonObject() : null;
        }
        catch (IOException e) {
            LOGGER.warn("PreservationPolicy: cannot read {}: {}", (Object)target, (Object)e.getMessage());
            return null;
        }
        catch (JsonSyntaxException e) {
            LOGGER.warn("PreservationPolicy: malformed JSON at {}: {}", (Object)target, (Object)e.getMessage());
            return null;
        }
    }

    public record Decision(boolean overwrite, Map<String, Object> mergedJson, String reason) {
        public static Decision overwriteAsIs() {
            return new Decision(true, null, null);
        }

        public static Decision skipConverterPinned(String path) {
            return new Decision(false, null, "target has converter_skip=true (" + path + ")");
        }

        public static Decision overwriteWithBiomeTags(Map<String, Object> merged) {
            return new Decision(true, merged, "preserved biome_tags from existing target");
        }
    }
}

