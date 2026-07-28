/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.mojang.authlib.GameProfile
 *  com.mojang.logging.LogUtils
 *  io.netty.channel.ChannelHandler
 *  io.netty.channel.embedded.EmbeddedChannel
 *  javax.annotation.Nullable
 *  net.minecraft.core.BlockPos
 *  net.minecraft.network.Connection
 *  net.minecraft.network.protocol.PacketFlow
 *  net.minecraft.network.protocol.game.ServerboundAcceptTeleportationPacket
 *  net.minecraft.server.MinecraftServer
 *  net.minecraft.server.level.ClientInformation
 *  net.minecraft.server.level.ServerLevel
 *  net.minecraft.server.level.ServerPlayer
 *  net.minecraft.server.network.CommonListenerCookie
 *  net.minecraft.world.entity.HumanoidArm
 *  net.minecraft.world.entity.player.ChatVisiblity
 *  net.minecraft.world.level.GameType
 *  net.neoforged.neoforge.network.registration.NetworkRegistry
 *  org.slf4j.Logger
 */
package org.millenaire.test;

import com.mojang.authlib.GameProfile;
import com.mojang.logging.LogUtils;
import io.netty.channel.ChannelHandler;
import io.netty.channel.embedded.EmbeddedChannel;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;
import javax.annotation.Nullable;
import net.minecraft.core.BlockPos;
import net.minecraft.network.Connection;
import net.minecraft.network.protocol.PacketFlow;
import net.minecraft.network.protocol.game.ServerboundAcceptTeleportationPacket;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ClientInformation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.network.CommonListenerCookie;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.world.entity.player.ChatVisiblity;
import net.minecraft.world.level.GameType;
import net.neoforged.neoforge.network.registration.NetworkRegistry;
import org.slf4j.Logger;

public final class TestPlayerManager {
    private static final Logger LOGGER = LogUtils.getLogger();
    private static final String PLAYER_NAME = "test-player";
    @Nullable
    private static ServerPlayer instance;
    private static final AtomicInteger teleportIdCounter;

    private TestPlayerManager() {
    }

    public static void confirmTeleport() {
        if (instance == null || TestPlayerManager.instance.connection == null) {
            return;
        }
        int id = teleportIdCounter.incrementAndGet();
        TestPlayerManager.instance.connection.handleAcceptTeleportPacket(new ServerboundAcceptTeleportationPacket(id));
    }

    public static void acknowledgeChunkBatch() {
        if (instance == null || TestPlayerManager.instance.connection == null) {
            return;
        }
        TestPlayerManager.instance.connection.chunkSender.onChunkBatchReceivedByClient(64.0f);
    }

    public static ServerPlayer spawn(MinecraftServer server, ServerLevel level, BlockPos pos) {
        if (instance != null) {
            throw new IllegalStateException("TestPlayer already active. Call remove() first.");
        }
        teleportIdCounter.set(0);
        GameProfile profile = new GameProfile(UUID.randomUUID(), PLAYER_NAME);
        ClientInformation clientInfo = new ClientInformation("en_us", 32, ChatVisiblity.FULL, true, 0, HumanoidArm.RIGHT, false, true);
        CommonListenerCookie cookie = new CommonListenerCookie(profile, 0, clientInfo, false);
        ServerPlayer player = new ServerPlayer(server, level, profile, clientInfo);
        Connection connection = new Connection(PacketFlow.SERVERBOUND);
        new EmbeddedChannel(new ChannelHandler[]{connection});
        NetworkRegistry.configureMockConnection((Connection)connection);
        server.getPlayerList().placeNewPlayer(connection, player, cookie);
        player.gameMode.changeGameModeForPlayer(GameType.SURVIVAL);
        instance = player;
        TestPlayerManager.confirmTeleport();
        player.teleportTo(level, (double)pos.getX() + 0.5, (double)pos.getY(), (double)pos.getZ() + 0.5, Set.of(), player.getYRot(), player.getXRot());
        TestPlayerManager.confirmTeleport();
        TestPlayerManager.acknowledgeChunkBatch();
        LOGGER.info("[Millenaire] TestPlayer created at {} in {}", (Object)pos, (Object)level.dimension().location());
        return player;
    }

    public static void remove() {
        if (instance == null) {
            return;
        }
        ServerPlayer player = instance;
        instance = null;
        player.closeContainer();
        player.server.getPlayerList().remove(player);
        LOGGER.info("[Millenaire] TestPlayer removed");
    }

    @Nullable
    public static ServerPlayer get() {
        return instance;
    }

    public static boolean isActive() {
        return instance != null;
    }

    public static void onServerStopping() {
        if (instance != null) {
            LOGGER.info("[Millenaire] Cleanup TestPlayer (server shutdown)");
            TestPlayerManager.remove();
        }
    }

    static {
        teleportIdCounter = new AtomicInteger(0);
    }
}

