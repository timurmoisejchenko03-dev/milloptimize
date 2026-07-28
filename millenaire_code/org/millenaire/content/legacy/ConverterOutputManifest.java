/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.google.gson.Gson
 *  com.google.gson.GsonBuilder
 *  com.google.gson.JsonElement
 *  com.google.gson.JsonObject
 */
package org.millenaire.content.legacy;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.nio.file.AccessDeniedException;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.OpenOption;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.nio.file.attribute.FileAttribute;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Instant;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;

public final class ConverterOutputManifest {
    public static final String DEFAULT_FILENAME = "_conversion_manifest.json";
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().disableHtmlEscaping().create();
    private static final DateTimeFormatter ISO_INSTANT_SECONDS = DateTimeFormatter.ISO_INSTANT.withZone(ZoneOffset.UTC);
    private final String converterVersion;
    private final List<Entry> converted;

    public ConverterOutputManifest(String converterVersion) {
        this(converterVersion, new ArrayList<Entry>());
    }

    public ConverterOutputManifest(String converterVersion, List<Entry> converted) {
        this.converterVersion = converterVersion;
        this.converted = new ArrayList<Entry>(converted);
    }

    public String converterVersion() {
        return this.converterVersion;
    }

    public List<Entry> entries() {
        return List.copyOf(this.converted);
    }

    public void addEntry(Entry entry) {
        this.converted.add(entry);
    }

    public Entry recordFile(Path addonRoot, Path targetAbsolute, Path sourceAbsolute) throws IOException {
        String targetRel = addonRoot.relativize(targetAbsolute).toString().replace('\\', '/');
        String sourceRel = sourceAbsolute == null ? null : addonRoot.relativize(sourceAbsolute).toString().replace('\\', '/');
        String hash = ConverterOutputManifest.sha256OfFile(targetAbsolute);
        String timestamp = ISO_INSTANT_SECONDS.format(Instant.now());
        Entry entry = new Entry(targetRel, sourceRel, timestamp, "sha256:" + hash);
        this.addEntry(entry);
        return entry;
    }

    /*
     * Enabled aggressive block sorting
     * Enabled unnecessary exception pruning
     * Enabled aggressive exception aggregation
     */
    public static String readVersion(Path path) {
        if (!Files.exists(path, new LinkOption[0])) {
            return null;
        }
        try (BufferedReader reader = Files.newBufferedReader(path, StandardCharsets.UTF_8);){
            JsonObject json = (JsonObject)GSON.fromJson((Reader)reader, JsonObject.class);
            if (json == null) {
                String string2 = null;
                return string2;
            }
            String string = json.has("converter_version") ? json.get("converter_version").getAsString() : null;
            return string;
        }
        catch (IOException e) {
            return null;
        }
    }

    public static ConverterOutputManifest readOrEmpty(Path path, String fallbackConverterVersion) throws IOException {
        if (!Files.exists(path, new LinkOption[0])) {
            return new ConverterOutputManifest(fallbackConverterVersion);
        }
        try (BufferedReader reader = Files.newBufferedReader(path, StandardCharsets.UTF_8);){
            JsonObject json = (JsonObject)GSON.fromJson((Reader)reader, JsonObject.class);
            if (json == null) {
                ConverterOutputManifest converterOutputManifest = new ConverterOutputManifest(fallbackConverterVersion);
                return converterOutputManifest;
            }
            String version = json.has("converter_version") ? json.get("converter_version").getAsString() : fallbackConverterVersion;
            ArrayList<Entry> entries = new ArrayList<Entry>();
            if (json.has("converted") && json.get("converted").isJsonArray()) {
                for (JsonElement el : json.getAsJsonArray("converted")) {
                    if (!el.isJsonObject()) continue;
                    JsonObject obj = el.getAsJsonObject();
                    String entryPath = obj.get("path").getAsString();
                    String entrySource = obj.has("source") && !obj.get("source").isJsonNull() ? obj.get("source").getAsString() : null;
                    String entryTimestamp = obj.get("timestamp").getAsString();
                    String entryHash = obj.get("hash").getAsString();
                    entries.add(new Entry(entryPath, entrySource, entryTimestamp, entryHash));
                }
            }
            ConverterOutputManifest converterOutputManifest = new ConverterOutputManifest(version, entries);
            return converterOutputManifest;
        }
    }

    public void writeAtomic(Path path) throws IOException {
        Path parent = path.getParent();
        if (parent != null) {
            Files.createDirectories(parent, new FileAttribute[0]);
        }
        LinkedHashMap<String, Object> payload = new LinkedHashMap<String, Object>();
        payload.put("converter_version", this.converterVersion);
        ArrayList entries = new ArrayList();
        for (Entry e : this.converted) {
            LinkedHashMap<String, String> map = new LinkedHashMap<String, String>();
            map.put("path", e.path());
            if (e.source() != null) {
                map.put("source", e.source());
            }
            map.put("timestamp", e.timestamp());
            map.put("hash", e.hash());
            entries.add(map);
        }
        payload.put("converted", entries);
        Path tmp = parent != null ? parent.resolve(path.getFileName().toString() + ".tmp") : Path.of(path.getFileName().toString() + ".tmp", new String[0]);
        try (BufferedWriter w = Files.newBufferedWriter(tmp, StandardCharsets.UTF_8, new OpenOption[0]);){
            GSON.toJson(payload, (Appendable)w);
        }
        try {
            Files.move(tmp, path, StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING);
        }
        catch (AtomicMoveNotSupportedException e) {
            ConverterOutputManifest.moveWithBoundedRetry(tmp, path);
        }
        catch (AccessDeniedException e) {
            ConverterOutputManifest.moveWithBoundedRetry(tmp, path);
        }
    }

    private static void moveWithBoundedRetry(Path source, Path target) throws IOException {
        AccessDeniedException last = null;
        for (int attempt = 0; attempt < 5; ++attempt) {
            try {
                Files.move(source, target, StandardCopyOption.REPLACE_EXISTING);
                return;
            }
            catch (AccessDeniedException e) {
                last = e;
                try {
                    Thread.sleep(50L * (1L + (long)attempt));
                    continue;
                }
                catch (InterruptedException ie) {
                    Thread.currentThread().interrupt();
                    throw e;
                }
            }
        }
        throw last;
    }

    public static String sha256OfFile(Path target) throws IOException {
        MessageDigest md;
        try {
            md = MessageDigest.getInstance("SHA-256");
        }
        catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 not available on this JVM", e);
        }
        byte[] bytes = Files.readAllBytes(target);
        byte[] digest = md.digest(bytes);
        StringBuilder sb = new StringBuilder(digest.length * 2);
        for (byte b : digest) {
            sb.append(String.format("%02x", b & 0xFF));
        }
        return sb.toString();
    }

    public record Entry(String path, String source, String timestamp, String hash) {
        public Entry {
            if (path == null || path.isBlank()) {
                throw new IllegalArgumentException("path must not be blank");
            }
            if (timestamp == null || timestamp.isBlank()) {
                throw new IllegalArgumentException("timestamp must not be blank");
            }
            if (hash == null || !hash.startsWith("sha256:")) {
                throw new IllegalArgumentException("hash must start with \"sha256:\"");
            }
        }
    }
}

