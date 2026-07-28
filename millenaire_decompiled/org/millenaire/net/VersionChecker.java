/*
 * Decompiled with CFR 0.150.
 * 
 * Could not load the following classes:
 *  com.google.gson.JsonObject
 *  com.google.gson.JsonParser
 *  com.mojang.logging.LogUtils
 *  net.minecraft.ChatFormatting
 *  net.minecraft.network.chat.Component
 *  net.minecraft.server.MinecraftServer
 *  net.minecraft.server.level.ServerPlayer
 *  org.slf4j.Logger
 */
package org.millenaire.net;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.mojang.logging.LogUtils;
import java.util.List;
import java.util.Locale;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import org.millenaire.Millenaire;
import org.millenaire.net.ModApiClient;
import org.slf4j.Logger;

public final class VersionChecker {
    private static final Logger LOGGER = LogUtils.getLogger();
    private static final String PATH = "/api/mod/v1/current-version";
    private static final long DELAY_SECONDS = 60L;
    private static final AtomicBoolean checked = new AtomicBoolean(false);

    private VersionChecker() {
    }

    public static void onPlayerLogin(ServerPlayer player) {
        String modVersion = Millenaire.getModVersion();
        if (!VersionChecker.isReleaseBuild(modVersion)) {
            return;
        }
        if (!checked.compareAndSet(false, true)) {
            return;
        }
        MinecraftServer server = player.server;
        UUID triggerPlayer = player.getUUID();
        String mcVersion = server.getServerVersion();
        Executor delayed = CompletableFuture.delayedExecutor(60L, TimeUnit.SECONDS);
        CompletableFuture.runAsync(() -> ModApiClient.get(PATH, "mcVersion=" + mcVersion).thenAccept(resp -> {
            if (resp == null || resp.status() != 200) {
                return;
            }
            try {
                JsonObject json = JsonParser.parseString((String)resp.body()).getAsJsonObject();
                String latest = json.get("version").getAsString();
                if (!VersionChecker.isOutdated(modVersion, latest)) {
                    return;
                }
                String noteEn = VersionChecker.optString(json, "releaseNotesEn");
                String noteFr = VersionChecker.optString(json, "releaseNotesFr");
                server.execute(() -> VersionChecker.notify(server, triggerPlayer, modVersion, latest, noteEn, noteFr));
            }
            catch (Exception e) {
                LOGGER.debug("Version check parse failed: {}", (Object)e.getMessage());
            }
        }), delayed);
    }

    private static void notify(MinecraftServer server, UUID triggerPlayer, String current, String latest, String noteEn, String noteFr) {
        String locale;
        ServerPlayer target = server.getPlayerList().getPlayer(triggerPlayer);
        if (target == null) {
            List online = server.getPlayerList().getPlayers();
            if (online.isEmpty()) {
                return;
            }
            target = (ServerPlayer)online.getFirst();
        }
        String note = (locale = target.clientInformation().language()) != null && locale.toLowerCase(Locale.ROOT).startsWith("fr") ? noteFr : noteEn;
        target.sendSystemMessage((Component)Component.translatable((String)"millenaire.startup.outdatedversion", (Object[])new Object[]{current, latest, note}).withStyle(ChatFormatting.GOLD));
    }

    private static String optString(JsonObject o, String key) {
        return o.has(key) && !o.get(key).isJsonNull() ? o.get(key).getAsString() : "";
    }

    public static boolean isReleaseBuild(String version) {
        String v = version.toLowerCase(Locale.ROOT);
        return !v.contains("-dev") && !v.contains("-preview") && !v.contains("-alpha") && !v.contains("-beta") && !v.contains("-rc");
    }

    public static boolean isOutdated(String current, String latest) {
        return !current.trim().equals(latest.trim());
    }

    public static void reset() {
        checked.set(false);
    }

    public static void resetForTesting() {
        VersionChecker.reset();
    }
}

