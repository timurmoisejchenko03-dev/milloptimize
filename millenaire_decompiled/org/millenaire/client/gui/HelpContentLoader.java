/*
 * Decompiled with CFR 0.150.
 * 
 * Could not load the following classes:
 *  com.mojang.logging.LogUtils
 *  net.minecraft.client.Minecraft
 *  net.minecraft.resources.ResourceLocation
 *  net.minecraft.server.packs.resources.Resource
 *  net.minecraft.server.packs.resources.ResourceManager
 *  org.slf4j.Logger
 */
package org.millenaire.client.gui;

import com.mojang.logging.LogUtils;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Optional;
import net.minecraft.client.Minecraft;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.Resource;
import net.minecraft.server.packs.resources.ResourceManager;
import org.millenaire.language.LocaleResolver;
import org.slf4j.Logger;

public final class HelpContentLoader {
    private static final Logger LOGGER = LogUtils.getLogger();
    private static final int NUM_CHAPTERS = 13;

    private HelpContentLoader() {
    }

    private static Optional<Resource> resolveHelpResource(int chapter) {
        String langCode = Minecraft.getInstance().getLanguageManager().getSelected();
        ResourceManager resourceManager = Minecraft.getInstance().getResourceManager();
        LinkedHashSet<String> supported = new LinkedHashSet<String>();
        if (HelpContentLoader.hasHelpFile(resourceManager, "en", chapter)) {
            supported.add("en");
        }
        for (String lc : Minecraft.getInstance().getLanguageManager().getLanguages().keySet()) {
            if (!HelpContentLoader.hasHelpFile(resourceManager, lc, chapter)) continue;
            supported.add(lc);
        }
        String resolved = LocaleResolver.resolveSupported(langCode, supported);
        if (resolved == null) {
            resolved = "en";
        }
        return resourceManager.getResource(ResourceLocation.fromNamespaceAndPath((String)"millenaire", (String)("help/" + resolved + "/help_" + chapter + ".txt")));
    }

    private static boolean hasHelpFile(ResourceManager rm, String locale, int chapter) {
        return rm.getResource(ResourceLocation.fromNamespaceAndPath((String)"millenaire", (String)("help/" + locale + "/help_" + chapter + ".txt"))).isPresent();
    }

    public static List<List<String>> loadChapter(int chapter) {
        if (chapter < 1 || chapter > 13) {
            return List.of();
        }
        try {
            Optional<Resource> res = HelpContentLoader.resolveHelpResource(chapter);
            if (res.isEmpty()) {
                LOGGER.warn("Help file not found for chapter {}", (Object)chapter);
                return List.of(List.of("Help file not found for chapter " + chapter));
            }
            ArrayList<String> allLines = new ArrayList<String>();
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(res.get().open(), StandardCharsets.UTF_8));){
                String line;
                while ((line = reader.readLine()) != null) {
                    allLines.add(line);
                }
            }
            int headerEnd = Math.min(allLines.size(), 5);
            for (int i = headerEnd - 1; i >= 0; --i) {
                if (!((String)allLines.get(i)).trim().startsWith("version:")) continue;
                allLines.remove(i);
            }
            while (!allLines.isEmpty() && ((String)allLines.get(0)).trim().isEmpty()) {
                allLines.remove(0);
            }
            ArrayList<List<String>> pages = new ArrayList<List<String>>();
            ArrayList<String> currentPage = new ArrayList<String>();
            for (String line : allLines) {
                if ("NEW_PAGE".equals(line.trim())) {
                    if (currentPage.isEmpty()) continue;
                    pages.add(currentPage);
                    currentPage = new ArrayList();
                    continue;
                }
                currentPage.add(HelpContentLoader.processColorTags(line));
            }
            if (!currentPage.isEmpty()) {
                pages.add(currentPage);
            }
            return pages;
        }
        catch (Exception e) {
            LOGGER.error("Failed to load help chapter {}", (Object)chapter, (Object)e);
            return List.of(List.of("Error loading help chapter " + chapter));
        }
    }

    private static String processColorTags(String line) {
        return line.replace("<darkblue>", "\u00a71").replace("<blue>", "\u00a79").replace("<darkgreen>", "\u00a72").replace("<green>", "\u00a7a").replace("<darkred>", "\u00a74").replace("<red>", "\u00a7c").replace("<black>", "\u00a70").replace("<gray>", "\u00a77").replace("<darkgray>", "\u00a78").replace("<gold>", "\u00a76").replace("<yellow>", "\u00a7e").replace("<white>", "\u00a7f");
    }
}

