/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.google.gson.JsonArray
 *  com.google.gson.JsonElement
 *  com.google.gson.JsonObject
 *  com.mojang.logging.LogUtils
 *  javax.annotation.Nullable
 *  net.minecraft.resources.ResourceLocation
 *  net.minecraft.util.GsonHelper
 *  org.slf4j.Logger
 */
package org.millenaire.culture;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.mojang.logging.LogUtils;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.TreeSet;
import javax.annotation.Nullable;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.GsonHelper;
import org.millenaire.content.ContentFs;
import org.millenaire.content.CustomContentIndex;
import org.millenaire.content.Resource;
import org.millenaire.culture.CultureLoader;
import org.millenaire.culture.ModCultures;
import org.millenaire.culture.NameLists;
import org.millenaire.culture.ReputationLabel;
import org.slf4j.Logger;

final class CultureMetadataLoader {
    private static final Logger LOGGER = LogUtils.getLogger();

    private CultureMetadataLoader() {
    }

    static void loadNameLists(String cultureName) {
        ResourceLocation cultureId = ResourceLocation.fromNamespaceAndPath((String)"millenaire", (String)cultureName);
        ContentFs cultureFs = CustomContentIndex.current().forCulture(cultureName);
        Set<String> listNames = CultureMetadataLoader.discoverNameListBasenames(cultureFs);
        if (listNames.isEmpty()) {
            return;
        }
        HashMap<String, List<String>> lists = new HashMap<String, List<String>>();
        for (String listName : listNames) {
            List<String> names = CultureMetadataLoader.readNameListWithOverlay(cultureFs, cultureName, listName);
            if (names.isEmpty()) continue;
            lists.put(listName, names);
        }
        ModCultures.registerNameLists(cultureId, new NameLists(lists));
        LOGGER.debug("Namelists loaded for {}: {} lists ({} names total)", new Object[]{cultureName, lists.size(), lists.values().stream().mapToInt(List::size).sum()});
    }

    private static List<String> readNameListWithOverlay(ContentFs cultureFs, String cultureName, String listName) {
        LinkedHashSet<String> seen = new LinkedHashSet<String>();
        List<Resource> resources = cultureFs.findAll("namelists/" + listName + ".txt");
        for (int i = resources.size() - 1; i >= 0; --i) {
            Resource res = resources.get(i);
            int before = seen.size();
            try (InputStream in = res.open();){
                seen.addAll(CultureMetadataLoader.readTextLines(in, res.relPath()));
            }
            catch (IOException e) {
                LOGGER.warn("Could not read namelist {} from {}: {}", new Object[]{res.relPath(), res.source().displayName(), e.getMessage()});
                continue;
            }
            if (seen.size() <= before || i >= resources.size() - 1) continue;
            LOGGER.debug("Overlay added {} entries to namelist {}/{} from {}", new Object[]{seen.size() - before, cultureName, listName, res.source().displayName()});
        }
        return new ArrayList<String>(seen);
    }

    private static Set<String> discoverNameListBasenames(ContentFs cultureFs) {
        TreeSet<String> out = new TreeSet<String>();
        cultureFs.walk("namelists", 0).forEach(res -> {
            String fileName;
            String rel = res.relPath();
            int slash = rel.lastIndexOf(47);
            String string = fileName = slash < 0 ? rel : rel.substring(slash + 1);
            if (fileName.startsWith("_")) {
                return;
            }
            String lower = fileName.toLowerCase(Locale.ROOT);
            if (!lower.endsWith(".txt")) {
                return;
            }
            out.add(fileName.substring(0, fileName.length() - 4));
        });
        return out;
    }

    private static List<String> readTextLines(@Nullable InputStream is, String label) {
        ArrayList<String> lines = new ArrayList<String>();
        if (is == null) {
            return lines;
        }
        try (InputStream closeMe = is;
             BufferedReader reader = new BufferedReader(new InputStreamReader(closeMe, StandardCharsets.UTF_8));){
            String line;
            while ((line = reader.readLine()) != null) {
                String trimmed = line.trim();
                if (trimmed.isEmpty() || trimmed.startsWith("#")) continue;
                lines.add(trimmed);
            }
        }
        catch (IOException e) {
            LOGGER.error("Error reading {}: {}", new Object[]{label, e.getMessage(), e});
        }
        return lines;
    }

    static void loadReputationLabels(String cultureName) {
        ResourceLocation cultureId = ResourceLocation.fromNamespaceAndPath((String)"millenaire", (String)cultureName);
        String path = "/millenaire/cultures/" + cultureName + "/reputation.json";
        JsonObject json = CultureLoader.readJson(path);
        if (json == null) {
            return;
        }
        ArrayList<ReputationLabel> labels = new ArrayList<ReputationLabel>();
        JsonArray array = GsonHelper.getAsJsonArray((JsonObject)json, (String)"labels");
        for (JsonElement e : array) {
            JsonObject obj = e.getAsJsonObject();
            int threshold = GsonHelper.getAsInt((JsonObject)obj, (String)"threshold");
            String key = GsonHelper.getAsString((JsonObject)obj, (String)"key");
            labels.add(new ReputationLabel(threshold, key));
        }
        Collections.sort(labels);
        ModCultures.registerReputationLabels(cultureId, labels);
        LOGGER.debug("Reputation labels loaded for {}: {} levels", (Object)cultureName, (Object)labels.size());
    }

    static void loadCultureReputationLabels(String cultureName) {
        ResourceLocation cultureId = ResourceLocation.fromNamespaceAndPath((String)"millenaire", (String)cultureName);
        String path = "/millenaire/cultures/" + cultureName + "/culture_reputation.json";
        JsonObject json = CultureLoader.readJson(path);
        if (json == null) {
            return;
        }
        ArrayList<ReputationLabel> labels = new ArrayList<ReputationLabel>();
        JsonArray array = GsonHelper.getAsJsonArray((JsonObject)json, (String)"labels");
        for (JsonElement e : array) {
            JsonObject obj = e.getAsJsonObject();
            int threshold = GsonHelper.getAsInt((JsonObject)obj, (String)"threshold");
            String key = GsonHelper.getAsString((JsonObject)obj, (String)"key");
            labels.add(new ReputationLabel(threshold, key));
        }
        Collections.sort(labels);
        ModCultures.registerCultureReputationLabels(cultureId, labels);
        LOGGER.debug("Culture reputation labels loaded for {}: {} levels", (Object)cultureName, (Object)labels.size());
    }
}

