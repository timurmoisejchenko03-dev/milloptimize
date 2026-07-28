/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.google.gson.Gson
 *  com.google.gson.GsonBuilder
 */
package org.millenaire.encyclopedia;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.OpenOption;
import java.nio.file.Path;
import java.nio.file.attribute.FileAttribute;
import java.util.List;
import java.util.Map;
import org.millenaire.encyclopedia.EncyclopediaIndex;
import org.millenaire.encyclopedia.ExportLine;
import org.millenaire.encyclopedia.ExportPage;
import org.millenaire.encyclopedia.Travelbook;
import org.millenaire.encyclopedia.VillageData;

public final class EncyclopediaWriter {
    private static final Gson GSON = new GsonBuilder().disableHtmlEscaping().setPrettyPrinting().create();

    private EncyclopediaWriter() {
    }

    public static void write(Path outDir, EncyclopediaIndex index, Map<String, List<ExportLine>> structures, Map<String, Map<String, Map<String, String>>> textByLocale, Map<String, Map<String, String>> langByLocale, Map<String, VillageData> villageData) {
        try {
            Files.createDirectories(outDir, new FileAttribute[0]);
            EncyclopediaWriter.writeJson(outDir.resolve("index.json"), index);
            Path structureDir = outDir.resolve("generated").resolve("structure");
            Files.createDirectories(structureDir, new FileAttribute[0]);
            for (Map.Entry<String, List<ExportLine>> entry : structures.entrySet()) {
                Travelbook travelbook = new Travelbook(List.of(new ExportPage(entry.getValue())));
                EncyclopediaWriter.writeJson(structureDir.resolve(entry.getKey() + ".json"), travelbook);
            }
            Path textDir = outDir.resolve("generated").resolve("text");
            Files.createDirectories(textDir, new FileAttribute[0]);
            for (Map.Entry<String, Map<String, Map<String, String>>> entry : textByLocale.entrySet()) {
                EncyclopediaWriter.writeJson(textDir.resolve(entry.getKey() + ".json"), entry.getValue());
            }
            Path path = outDir.resolve("generated").resolve("lang");
            Files.createDirectories(path, new FileAttribute[0]);
            for (Map.Entry<String, Map<String, String>> entry : langByLocale.entrySet()) {
                EncyclopediaWriter.writeJson(path.resolve(entry.getKey() + ".json"), entry.getValue());
            }
            Path path2 = outDir.resolve("generated").resolve("villages");
            Files.createDirectories(path2, new FileAttribute[0]);
            for (Map.Entry<String, VillageData> villageEntry : villageData.entrySet()) {
                EncyclopediaWriter.writeJson(path2.resolve(villageEntry.getKey() + ".json"), villageEntry.getValue());
            }
        }
        catch (IOException e) {
            throw new UncheckedIOException("Failed to write encyclopedia export to " + String.valueOf(outDir), e);
        }
    }

    private static void writeJson(Path path, Object value) throws IOException {
        try (BufferedWriter writer = Files.newBufferedWriter(path, StandardCharsets.UTF_8, new OpenOption[0]);){
            GSON.toJson(value, (Appendable)writer);
        }
    }
}

