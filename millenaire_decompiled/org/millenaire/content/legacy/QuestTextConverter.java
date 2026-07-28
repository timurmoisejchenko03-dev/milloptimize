/*
 * Decompiled with CFR 0.150.
 * 
 * Could not load the following classes:
 *  com.google.gson.Gson
 *  com.google.gson.GsonBuilder
 */
package org.millenaire.content.legacy;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.OpenOption;
import java.nio.file.Path;
import java.nio.file.attribute.FileAttribute;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.stream.Stream;

public class QuestTextConverter {
    private static final Path PROJECT_DIR = Path.of(System.getProperty("user.dir"), new String[0]);
    private static final Path LEGACY_ROOT = QuestTextConverter.resolveLegacyRoot();
    private static final Path QUEST_LANG_OUTPUT = PROJECT_DIR.resolve("src/main/resources/millenaire/quests/lang");
    private final Gson gson = new GsonBuilder().setPrettyPrinting().disableHtmlEscaping().create();
    private int totalLanguages = 0;
    private int totalEntries = 0;
    private int totalWarnings = 0;

    public static void main(String[] args) {
        QuestTextConverter converter = new QuestTextConverter();
        try {
            converter.run();
        }
        catch (Exception e) {
            System.err.println("[FATAL] Quest text conversion failed: " + e.getMessage());
            e.printStackTrace();
            System.exit(1);
        }
    }

    private void run() throws IOException {
        List<Path> langDirs;
        Path languagesRoot = LEGACY_ROOT.resolve("languages");
        if (!Files.isDirectory(languagesRoot, new LinkOption[0])) {
            System.err.println("[FATAL] Legacy languages directory not found: " + String.valueOf(languagesRoot));
            System.exit(1);
        }
        System.out.println("=== QuestTextConverter ===");
        System.out.println("Legacy languages: " + String.valueOf(languagesRoot));
        System.out.println("Output: " + String.valueOf(QUEST_LANG_OUTPUT));
        Files.createDirectories(QUEST_LANG_OUTPUT, new FileAttribute[0]);
        try (Stream<Path> stream = Files.list(languagesRoot);){
            langDirs = stream.filter(x$0 -> Files.isDirectory(x$0, new LinkOption[0])).sorted().toList();
        }
        for (Path langDir : langDirs) {
            this.processLanguage(langDir);
        }
        System.out.println("\n=== Summary ===");
        System.out.println("Languages processed: " + this.totalLanguages);
        System.out.println("Total entries: " + this.totalEntries);
        System.out.println("Warnings: " + this.totalWarnings);
        System.out.println("\n\u2713 Quest text conversion completed.");
    }

    private void processLanguage(Path langDir) throws IOException {
        List<Path> questTextFiles;
        String langCode = langDir.getFileName().toString();
        try (Stream<Path> stream = Files.list(langDir);){
            questTextFiles = stream.filter(p -> {
                String name = p.getFileName().toString();
                return name.startsWith("quests_") && name.endsWith(".txt");
            }).sorted().toList();
        }
        if (questTextFiles.isEmpty()) {
            return;
        }
        TreeMap<String, String> allTexts = new TreeMap<String, String>();
        for (Path textFile : questTextFiles) {
            this.parseQuestTextFile(textFile, allTexts, langCode);
        }
        if (allTexts.isEmpty()) {
            return;
        }
        Path outputFile = QUEST_LANG_OUTPUT.resolve(langCode + ".json");
        try (BufferedWriter writer = Files.newBufferedWriter(outputFile, StandardCharsets.UTF_8, new OpenOption[0]);){
            this.gson.toJson(allTexts, (Appendable)writer);
        }
        ++this.totalLanguages;
        this.totalEntries += allTexts.size();
        System.out.println("[" + langCode + "] " + allTexts.size() + " entries from " + questTextFiles.size() + " files");
    }

    private void parseQuestTextFile(Path textFile, Map<String, String> allTexts, String langCode) {
        try {
            List<String> lines = Files.readAllLines(textFile, StandardCharsets.UTF_8);
            for (String rawLine : lines) {
                int eqIdx;
                String line = rawLine.trim();
                if (line.isEmpty() || line.startsWith("//") || (eqIdx = line.indexOf(61)) < 0) continue;
                String key = line.substring(0, eqIdx).trim();
                String value = line.substring(eqIdx + 1);
                if (key.isEmpty()) continue;
                allTexts.put(key, value);
            }
        }
        catch (IOException e) {
            System.out.println("  [WARN] " + langCode + ": Failed to read " + String.valueOf(textFile.getFileName()) + ": " + e.getMessage());
            ++this.totalWarnings;
        }
    }

    private static Path resolveLegacyRoot() {
        Path relative = PROJECT_DIR.resolve("../millenaire-1.12/content/millenaire");
        if (Files.isDirectory(relative, new LinkOption[0])) {
            return relative;
        }
        try {
            Path fromMainRepo;
            Path mainRepoRoot;
            ProcessBuilder pb = new ProcessBuilder("git", "rev-parse", "--path-format=absolute", "--git-common-dir");
            pb.directory(PROJECT_DIR.toFile());
            pb.redirectErrorStream(true);
            Process p = pb.start();
            String gitCommonDir = new String(p.getInputStream().readAllBytes()).trim();
            p.waitFor();
            if (p.exitValue() == 0 && (mainRepoRoot = Path.of(gitCommonDir, new String[0]).getParent()) != null && Files.isDirectory(fromMainRepo = mainRepoRoot.resolve("../millenaire-1.12/content/millenaire"), new LinkOption[0])) {
                return fromMainRepo;
            }
        }
        catch (Exception exception) {
            // empty catch block
        }
        return relative;
    }
}

