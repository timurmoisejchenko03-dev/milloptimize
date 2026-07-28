/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.google.gson.Gson
 *  com.google.gson.GsonBuilder
 *  com.google.gson.JsonArray
 *  com.google.gson.JsonElement
 *  com.google.gson.JsonObject
 *  com.mojang.logging.LogUtils
 *  net.minecraft.gametest.framework.GameTest
 *  net.minecraft.gametest.framework.GameTestHelper
 *  net.minecraft.resources.ResourceLocation
 *  net.neoforged.neoforge.gametest.GameTestHolder
 *  net.neoforged.neoforge.gametest.PrefixGameTestTemplate
 *  org.slf4j.Logger
 */
package org.millenaire.test;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.mojang.logging.LogUtils;
import java.io.BufferedReader;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import org.millenaire.culture.ModCultures;
import org.millenaire.encyclopedia.EncyclopediaExporter;
import org.slf4j.Logger;

@GameTestHolder(value="millenaire")
@PrefixGameTestTemplate(value=false)
public class EncyclopediaExportGameTests {
    private static final Logger LOGGER = LogUtils.getLogger();
    private static final Gson GSON = new GsonBuilder().disableHtmlEscaping().setPrettyPrinting().create();
    private static final Path OUTPUT_DIR = Path.of("build", "encyclopedia");
    private static final List<String> DATA_LOCALES = List.of("en_us", "fr_fr", "zh_cn", "ru_ru", "es_es", "pt_pt", "ja_jp", "pl_pl", "tr_tr", "de_de", "uk_ua", "it_it", "ar_sa", "cs_cz", "da_dk", "et_ee", "hi_in", "hu_hu", "ko_kr", "nb_no", "nl_nl", "sl_si", "sv_se", "th_th");
    private static final String ITEM_REF_SHAPE = "^[a-z0-9]+-(cultures|villages|villagers|buildings|tradegoods)-[a-z0-9_]+$";

    @GameTest(template="empty_platform", setupTicks=1L, timeoutTicks=400)
    public static void exportAllCulturesEncyclopedia(GameTestHelper helper) {
        List<String> cultureKeys = ModCultures.getAllCultures().keySet().stream().map(ResourceLocation::getPath).sorted().toList();
        if (cultureKeys.isEmpty()) {
            helper.fail("No cultures loaded \u2014 content registries not initialised?");
            return;
        }
        EncyclopediaExporter.exportAll(OUTPUT_DIR, cultureKeys, DATA_LOCALES, null, null);
        JsonObject index = EncyclopediaExportGameTests.readJsonObject(helper, OUTPUT_DIR.resolve("index.json"));
        if (index == null) {
            return;
        }
        JsonArray items = index.getAsJsonArray("items");
        if (items == null || items.isEmpty()) {
            helper.fail("index.json has no items (content not loaded?)");
            return;
        }
        if (!(EncyclopediaExportGameTests.hasType(items, "CULTURES") && EncyclopediaExportGameTests.hasType(items, "VILLAGERS") && EncyclopediaExportGameTests.hasType(items, "BUILDINGS"))) {
            helper.fail("index.json is missing one of the required types CULTURES/VILLAGERS/BUILDINGS");
            return;
        }
        ArrayList<String> itemRefs = new ArrayList<String>();
        for (JsonElement el : items) {
            JsonObject entry = el.getAsJsonObject();
            String itemRef = entry.get("itemRef").getAsString();
            itemRefs.add(itemRef);
            if (!itemRef.matches(ITEM_REF_SHAPE)) {
                helper.fail("Index item ref does not match the website shape: " + itemRef);
                return;
            }
            Path structureFile = OUTPUT_DIR.resolve("generated").resolve("structure").resolve(itemRef + ".json");
            if (!Files.exists(structureFile, new LinkOption[0])) {
                helper.fail("Missing structure file for index item: " + itemRef);
                return;
            }
            if ("VILLAGES".equals(entry.get("type").getAsString()) && !EncyclopediaExportGameTests.assertVillageData(helper, itemRef)) {
                return;
            }
            JsonObject structure = EncyclopediaExportGameTests.readJsonObject(helper, structureFile);
            if (structure == null) {
                return;
            }
            JsonArray pages = structure.getAsJsonArray("pages");
            if (pages == null || pages.isEmpty()) {
                helper.fail("Structure " + itemRef + " has no pages");
                return;
            }
            JsonArray lines = pages.get(0).getAsJsonObject().getAsJsonArray("lines");
            if (lines != null && !lines.isEmpty()) continue;
            helper.fail("Structure " + itemRef + " has an empty first page");
            return;
        }
        JsonObject langEn = EncyclopediaExportGameTests.readJsonObject(helper, OUTPUT_DIR.resolve("generated").resolve("lang").resolve("en_us.json"));
        JsonObject textEn = EncyclopediaExportGameTests.readJsonObject(helper, OUTPUT_DIR.resolve("generated").resolve("text").resolve("en_us.json"));
        if (langEn == null || textEn == null) {
            return;
        }
        List sample = itemRefs.stream().sorted().limit(20L).collect(Collectors.toCollection(ArrayList::new));
        String abbot = "norman-villagers-abbot";
        if (itemRefs.contains(abbot) && !sample.contains(abbot)) {
            sample.add(abbot);
        }
        for (String itemRef : sample) {
            JsonObject structure = EncyclopediaExportGameTests.readJsonObject(helper, OUTPUT_DIR.resolve("generated").resolve("structure").resolve(itemRef + ".json"));
            if (structure == null) {
                return;
            }
            JsonObject textForItem = textEn.getAsJsonObject(itemRef);
            JsonArray lines = structure.getAsJsonArray("pages").get(0).getAsJsonObject().getAsJsonArray("lines");
            for (JsonElement lineEl : lines) {
                if (EncyclopediaExportGameTests.assertLineResolvable(helper, itemRef, lineEl.getAsJsonObject(), langEn, textForItem)) continue;
                return;
            }
        }
        if (itemRefs.contains(abbot) && !EncyclopediaExportGameTests.assertAbbotSample(helper, abbot)) {
            return;
        }
        LOGGER.info("Encyclopedia export: {} items across {} cultures written to {}", new Object[]{itemRefs.size(), cultureKeys.size(), OUTPUT_DIR.toAbsolutePath()});
        helper.succeed();
    }

    private static boolean assertLineResolvable(GameTestHelper helper, String itemRef, JsonObject line, JsonObject langEn, JsonObject textForItem) {
        if (!EncyclopediaExportGameTests.assertTextResolvable(helper, itemRef, line.getAsJsonObject("text"), langEn, textForItem)) {
            return false;
        }
        if (!EncyclopediaExportGameTests.assertTextResolvable(helper, itemRef, line.getAsJsonObject("referenceButtonLabel"), langEn, textForItem)) {
            return false;
        }
        JsonArray columns = line.getAsJsonArray("columns");
        if (columns != null) {
            for (JsonElement colEl : columns) {
                JsonObject col = colEl.getAsJsonObject();
                if (!EncyclopediaExportGameTests.assertTextResolvable(helper, itemRef, col.getAsJsonObject("text"), langEn, textForItem)) {
                    return false;
                }
                if (EncyclopediaExportGameTests.assertTextResolvable(helper, itemRef, col.getAsJsonObject("referenceButtonLabel"), langEn, textForItem)) continue;
                return false;
            }
        }
        return true;
    }

    private static boolean assertTextResolvable(GameTestHelper helper, String itemRef, JsonObject node, JsonObject langEn, JsonObject textForItem) {
        if (node == null) {
            return true;
        }
        if (node.has("key")) {
            String key = node.get("key").getAsString();
            if (!langEn.has(key)) {
                helper.fail("Item " + itemRef + " references key '" + key + "' absent from lang/en_us");
                return false;
            }
        } else if (node.has("id")) {
            String id = node.get("id").getAsString();
            if (textForItem == null || !textForItem.has(id)) {
                helper.fail("Item " + itemRef + " references slot id '" + id + "' absent from text/en_us[" + itemRef + "]");
                return false;
            }
        }
        return true;
    }

    private static boolean assertAbbotSample(GameTestHelper helper, String abbot) {
        JsonObject structure = EncyclopediaExportGameTests.readJsonObject(helper, OUTPUT_DIR.resolve("generated").resolve("structure").resolve(abbot + ".json"));
        JsonObject textFr = EncyclopediaExportGameTests.readJsonObject(helper, OUTPUT_DIR.resolve("generated").resolve("text").resolve("fr_fr.json"));
        JsonObject langFr = EncyclopediaExportGameTests.readJsonObject(helper, OUTPUT_DIR.resolve("generated").resolve("lang").resolve("fr_fr.json"));
        if (structure == null || textFr == null || langFr == null) {
            return false;
        }
        if (langFr.size() == 0) {
            helper.fail("lang/fr_fr is empty \u2014 the mod lang file was not copied");
            return false;
        }
        JsonArray lines = structure.getAsJsonArray("pages").get(0).getAsJsonObject().getAsJsonArray("lines");
        if (!EncyclopediaExportGameTests.hasKeyLine(lines)) {
            helper.fail("abbot structure has no key-based ossature line (expected at least one {key:\u2026} text node)");
            return false;
        }
        ArrayList<String> slotIds = new ArrayList<String>();
        EncyclopediaExportGameTests.collectSlotIds(lines, slotIds);
        JsonObject abbotFr = textFr.getAsJsonObject(abbot);
        for (String id : slotIds) {
            if (abbotFr != null && abbotFr.has(id)) continue;
            helper.fail("abbot slot id '" + id + "' not populated in text/fr_fr[" + abbot + "]");
            return false;
        }
        return true;
    }

    private static boolean hasKeyLine(JsonArray lines) {
        for (JsonElement lineEl : lines) {
            JsonObject line = lineEl.getAsJsonObject();
            if (EncyclopediaExportGameTests.nodeHasKey(line.getAsJsonObject("text"))) {
                return true;
            }
            if (EncyclopediaExportGameTests.nodeHasKey(line.getAsJsonObject("referenceButtonLabel"))) {
                return true;
            }
            JsonArray columns = line.getAsJsonArray("columns");
            if (columns == null) continue;
            for (JsonElement colEl : columns) {
                JsonObject col = colEl.getAsJsonObject();
                if (EncyclopediaExportGameTests.nodeHasKey(col.getAsJsonObject("text"))) {
                    return true;
                }
                if (!EncyclopediaExportGameTests.nodeHasKey(col.getAsJsonObject("referenceButtonLabel"))) continue;
                return true;
            }
        }
        return false;
    }

    private static boolean nodeHasKey(JsonObject node) {
        return node != null && node.has("key");
    }

    private static void collectSlotIds(JsonArray lines, List<String> out) {
        for (JsonElement lineEl : lines) {
            JsonObject line = lineEl.getAsJsonObject();
            EncyclopediaExportGameTests.collectSlotId(line.getAsJsonObject("text"), out);
            EncyclopediaExportGameTests.collectSlotId(line.getAsJsonObject("referenceButtonLabel"), out);
            JsonArray columns = line.getAsJsonArray("columns");
            if (columns == null) continue;
            for (JsonElement colEl : columns) {
                JsonObject col = colEl.getAsJsonObject();
                EncyclopediaExportGameTests.collectSlotId(col.getAsJsonObject("text"), out);
                EncyclopediaExportGameTests.collectSlotId(col.getAsJsonObject("referenceButtonLabel"), out);
            }
        }
    }

    private static void collectSlotId(JsonObject node, List<String> out) {
        if (node != null && node.has("id")) {
            out.add(node.get("id").getAsString());
        }
    }

    private static boolean assertVillageData(GameTestHelper helper, String itemRef) {
        Path villageFile = OUTPUT_DIR.resolve("generated").resolve("villages").resolve(itemRef + ".json");
        if (!Files.exists(villageFile, new LinkOption[0])) {
            helper.fail("Missing village data file for index item: " + itemRef);
            return false;
        }
        JsonObject village = EncyclopediaExportGameTests.readJsonObject(helper, villageFile);
        if (village == null) {
            return false;
        }
        if (!(village.has("type") && village.has("population") && village.has("composition"))) {
            helper.fail("Village data " + itemRef + " is missing type/population/composition");
            return false;
        }
        return true;
    }

    private static boolean hasType(JsonArray items, String type) {
        for (JsonElement el : items) {
            if (!type.equals(el.getAsJsonObject().get("type").getAsString())) continue;
            return true;
        }
        return false;
    }

    private static JsonObject readJsonObject(GameTestHelper helper, Path path) {
        JsonObject jsonObject;
        block8: {
            BufferedReader reader = Files.newBufferedReader(path, StandardCharsets.UTF_8);
            try {
                jsonObject = (JsonObject)GSON.fromJson((Reader)reader, JsonObject.class);
                if (reader == null) break block8;
            }
            catch (Throwable throwable) {
                try {
                    if (reader != null) {
                        try {
                            ((Reader)reader).close();
                        }
                        catch (Throwable throwable2) {
                            throwable.addSuppressed(throwable2);
                        }
                    }
                    throw throwable;
                }
                catch (Exception e) {
                    helper.fail("Failed to read JSON file " + String.valueOf(path) + ": " + e.getMessage());
                    return null;
                }
            }
            ((Reader)reader).close();
        }
        return jsonObject;
    }
}

