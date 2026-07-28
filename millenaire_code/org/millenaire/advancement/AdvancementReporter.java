/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.google.gson.Gson
 *  com.google.gson.GsonBuilder
 *  com.google.gson.JsonArray
 *  com.google.gson.JsonElement
 *  com.google.gson.JsonNull
 *  com.google.gson.JsonObject
 *  com.google.gson.JsonPrimitive
 *  com.mojang.logging.LogUtils
 *  net.minecraft.server.MinecraftServer
 *  net.minecraft.server.level.ServerLevel
 *  net.minecraft.server.level.ServerPlayer
 *  net.minecraft.world.level.Level
 *  org.jetbrains.annotations.Nullable
 *  org.slf4j.Logger
 */
package org.millenaire.advancement;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonNull;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;
import com.mojang.logging.LogUtils;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.Nullable;
import org.millenaire.Millenaire;
import org.millenaire.advancement.AdvancementStatsManager;
import org.millenaire.advancement.MillAdvancements;
import org.millenaire.config.MillenaireServerConfig;
import org.millenaire.net.ModApiClient;
import org.slf4j.Logger;

public final class AdvancementReporter {
    private static final Logger LOGGER = LogUtils.getLogger();
    private static final Gson GSON = new GsonBuilder().serializeNulls().create();
    private static final String PATH = "/api/mod/v1/stats/advancements";

    private AdvancementReporter() {
    }

    public static void onPlayerLogin(ServerPlayer player) {
        if (!((Boolean)MillenaireServerConfig.SERVER.sendStatistics.get()).booleanValue()) {
            return;
        }
        MinecraftServer server = player.server;
        ServerLevel overworld = server.getLevel(Level.OVERWORLD);
        if (overworld == null) {
            return;
        }
        try {
            AdvancementStatsManager stats = AdvancementStatsManager.get(overworld);
            UUID playerId = player.getUUID();
            Set<String> survival = stats.getSurvivalStats(playerId);
            Set<String> creative = stats.getCreativeStats(playerId);
            String uuidHash = AdvancementReporter.hashUuid(playerId);
            String login = (Boolean)MillenaireServerConfig.SERVER.sendPlayerName.get() != false ? player.getName().getString() : null;
            String mlnVersion = Millenaire.getModVersion();
            boolean isServer = server.isDedicatedServer();
            String body = AdvancementReporter.buildAdvancementsBody(uuidHash, login, mlnVersion, isServer, survival, creative);
            ModApiClient.postJson(PATH, "", body);
        }
        catch (Exception e) {
            LOGGER.debug("Failed to build advancement report: {}", (Object)e.getMessage());
        }
    }

    public static String buildAdvancementsBody(String uuidHash, @Nullable String login, @Nullable String mlnVersion, boolean isServer, Set<String> survival, Set<String> creative) {
        JsonObject json = new JsonObject();
        json.addProperty("uuid", uuidHash);
        json.add("login", (JsonElement)(login == null ? JsonNull.INSTANCE : new JsonPrimitive(login)));
        json.add("mlnVersion", (JsonElement)(mlnVersion == null ? JsonNull.INSTANCE : new JsonPrimitive(mlnVersion)));
        json.addProperty("isServer", Boolean.valueOf(isServer));
        json.add("advancements", (JsonElement)AdvancementReporter.buildAdvancementsJson(AdvancementReporter.getAllAdvancementKeys(), survival, creative));
        return GSON.toJson((JsonElement)json);
    }

    public static String hashUuid(UUID playerId) {
        MessageDigest md;
        try {
            md = MessageDigest.getInstance("SHA-256");
        }
        catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 not available on this JVM", e);
        }
        byte[] digest = md.digest(playerId.toString().getBytes(StandardCharsets.UTF_8));
        StringBuilder hex = new StringBuilder(digest.length * 2);
        for (byte b : digest) {
            hex.append(Character.forDigit(b >> 4 & 0xF, 16));
            hex.append(Character.forDigit(b & 0xF, 16));
        }
        return hex.toString();
    }

    public static JsonArray buildAdvancementsJson(List<String> keys, Set<String> survival, Set<String> creative) {
        JsonArray arr = new JsonArray();
        for (String key : keys) {
            arr.add((JsonElement)AdvancementReporter.advancementEntry(key, "SURVIVAL", survival.contains(key)));
            arr.add((JsonElement)AdvancementReporter.advancementEntry(key, "CREATIVE", creative.contains(key)));
        }
        return arr;
    }

    private static JsonObject advancementEntry(String key, String surface, boolean done) {
        JsonObject o = new JsonObject();
        o.addProperty("key", key);
        o.addProperty("surface", surface);
        o.addProperty("done", Boolean.valueOf(done));
        return o;
    }

    public static List<String> getAllAdvancementKeys() {
        ArrayList<String> keys = new ArrayList<String>();
        keys.add("firstcontact");
        keys.add("cresus");
        keys.add("summoningwand");
        keys.add("amateurarchitect");
        keys.add("medievalmetropolis");
        keys.add("thequest");
        keys.add("maitreapenser");
        keys.add("explorer");
        keys.add("marcopolo");
        keys.add("magellan");
        keys.add("selfdefense");
        keys.add("pantheon");
        keys.add("darkside");
        keys.add("scipio");
        keys.add("attila");
        keys.add("viking");
        keys.add("cheers");
        keys.add("hired");
        keys.add("masterfarmer");
        keys.add("greathunter");
        keys.add("friendindeed");
        keys.add("rainbow");
        keys.add("seljuk_istanbul");
        keys.add("byzantines_nottoday");
        keys.add("mp_weapon");
        keys.add("mp_hiredgoon");
        keys.add("mp_raidonplayer");
        keys.add("mp_neighbourtrade");
        keys.add("mp_friendlyvillage");
        keys.add("wq_indian");
        keys.add("wq_norman");
        keys.add("wq_mayan");
        keys.add("puja");
        keys.add("sacrifice");
        keys.add("marvel_norman");
        for (String culture : MillAdvancements.ADVANCEMENT_CULTURES) {
            keys.add("rep_" + culture);
            keys.add("complete_" + culture);
            keys.add("leader_" + culture);
        }
        return keys;
    }
}

