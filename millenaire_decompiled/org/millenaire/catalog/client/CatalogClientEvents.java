/*
 * Decompiled with CFR 0.150.
 * 
 * Could not load the following classes:
 *  com.mojang.logging.LogUtils
 *  net.minecraft.client.Minecraft
 *  net.minecraft.client.server.IntegratedServer
 *  net.minecraft.server.MinecraftServer
 *  net.neoforged.api.distmarker.Dist
 *  net.neoforged.bus.api.SubscribeEvent
 *  net.neoforged.fml.common.EventBusSubscriber
 *  net.neoforged.neoforge.client.event.ClientTickEvent$Post
 *  net.neoforged.neoforge.client.event.RenderLevelStageEvent
 *  net.neoforged.neoforge.client.event.RenderLevelStageEvent$Stage
 *  org.slf4j.Logger
 */
package org.millenaire.catalog.client;

import com.mojang.logging.LogUtils;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.util.List;
import net.minecraft.client.Minecraft;
import net.minecraft.client.server.IntegratedServer;
import net.minecraft.server.MinecraftServer;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.RenderLevelStageEvent;
import org.millenaire.catalog.CatalogJob;
import org.millenaire.catalog.CatalogSeedScout;
import org.millenaire.catalog.CatalogSubjectEnumerator;
import org.millenaire.catalog.ShotSpec;
import org.millenaire.catalog.client.CatalogScreenshotSession;
import org.millenaire.catalog.client.CatalogWorldBootstrap;
import org.millenaire.culture.ModCultures;
import org.slf4j.Logger;

@EventBusSubscriber(modid="millenaire", value={Dist.CLIENT})
public final class CatalogClientEvents {
    private static final Logger LOGGER = LogUtils.getLogger();
    private static final String JOB_FILENAME = "screenshot-job.json";
    private static CatalogScreenshotSession session;
    private static boolean jobConsumed;

    private CatalogClientEvents() {
    }

    public static Path jobFile() {
        return Minecraft.getInstance().gameDirectory.toPath().resolve(JOB_FILENAME);
    }

    @SubscribeEvent
    public static void onClientTick(ClientTickEvent.Post event) {
        Minecraft mc = Minecraft.getInstance();
        if (!Files.exists(CatalogClientEvents.jobFile(), new LinkOption[0])) {
            return;
        }
        if (mc.level == null) {
            CatalogWorldBootstrap.ensureWorld(mc);
            return;
        }
        if (!mc.hasSingleplayerServer()) {
            return;
        }
        IntegratedServer server = mc.getSingleplayerServer();
        if (server == null || server.isDedicatedServer()) {
            return;
        }
        if (session != null) {
            if (!session.isFinished()) {
                session.tick(mc);
            }
            return;
        }
        if (jobConsumed || mc.player == null) {
            return;
        }
        jobConsumed = true;
        CatalogClientEvents.startSession(mc, (MinecraftServer)server);
    }

    @SubscribeEvent
    public static void onRenderLevel(RenderLevelStageEvent event) {
        if (session == null || session.isFinished()) {
            return;
        }
        if (event.getStage() != RenderLevelStageEvent.Stage.AFTER_WEATHER) {
            return;
        }
        session.onRenderFrame(Minecraft.getInstance().getMainRenderTarget());
    }

    private static void startSession(Minecraft mc, MinecraftServer server) {
        try {
            CatalogJob job = CatalogJob.fromJson(Files.readString(CatalogClientEvents.jobFile()));
            if (job.isScout()) {
                CatalogClientEvents.startScout(mc, server, job);
                return;
            }
            Path root = mc.gameDirectory.toPath().resolve("screenshots");
            List<ShotSpec> shots = CatalogSubjectEnumerator.enumerate(ModCultures.getAllVillagerTypes(), ModCultures.getAllBuildingPlanSets(), job, root);
            session = new CatalogScreenshotSession(mc, server, root);
            session.start(shots);
        }
        catch (IOException e) {
            LOGGER.error("Failed to start catalog session: {}", (Object)e.toString());
        }
    }

    private static void startScout(Minecraft mc, MinecraftServer server, CatalogJob job) {
        Path reportDir = mc.gameDirectory.toPath().resolve("seed-reports");
        LOGGER.info("Catalog seed scout starting for seed {}", (Object)job.seed());
        server.execute(() -> {
            try {
                CatalogSeedScout.run(server.overworld(), job.seed(), reportDir);
            }
            catch (Exception e) {
                LOGGER.error("Seed scout failed for seed {}: {}", (Object)job.seed(), (Object)e.toString());
            }
            finally {
                try {
                    Files.deleteIfExists(CatalogClientEvents.jobFile());
                }
                catch (IOException iOException) {}
                LOGGER.info("Catalog seed scout done for seed {}", (Object)job.seed());
                Runtime.getRuntime().halt(0);
            }
        });
    }

    static {
        jobConsumed = false;
    }
}

