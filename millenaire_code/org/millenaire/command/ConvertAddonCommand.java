/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.mojang.brigadier.arguments.ArgumentType
 *  com.mojang.brigadier.arguments.StringArgumentType
 *  com.mojang.brigadier.builder.LiteralArgumentBuilder
 *  com.mojang.brigadier.context.CommandContext
 *  com.mojang.logging.LogUtils
 *  net.minecraft.commands.CommandSourceStack
 *  net.minecraft.commands.Commands
 *  net.minecraft.network.chat.Component
 *  net.neoforged.neoforge.event.server.ServerStoppingEvent
 *  org.slf4j.Logger
 */
package org.millenaire.command;

import com.mojang.brigadier.arguments.ArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.logging.LogUtils;
import java.io.File;
import java.io.IOException;
import java.lang.runtime.SwitchBootstraps;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.util.Locale;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.neoforged.neoforge.event.server.ServerStoppingEvent;
import org.millenaire.content.legacy.ConversionMode;
import org.millenaire.content.legacy.ConverterOutputManifest;
import org.millenaire.content.legacy.LegacyConversionDriver;
import org.millenaire.content.legacy.LegacyConversionReport;
import org.millenaire.content.legacy.LegacyLayoutDetector;
import org.slf4j.Logger;

public final class ConvertAddonCommand {
    private static final Logger LOGGER = LogUtils.getLogger();
    private static final ExecutorService EXECUTOR = Executors.newSingleThreadExecutor(r -> {
        Thread t = new Thread(r, "millenaire-convert-addon");
        t.setDaemon(true);
        return t;
    });
    private static final AtomicReference<CompletableFuture<?>> running = new AtomicReference<Object>(null);

    private ConvertAddonCommand() {
    }

    public static void registerUnder(LiteralArgumentBuilder<CommandSourceStack> dev) {
        dev.then(Commands.literal((String)"convert-addon").then(Commands.argument((String)"path", (ArgumentType)StringArgumentType.greedyString()).executes(ConvertAddonCommand::run)));
    }

    public static void onServerStopping(ServerStoppingEvent event) {
        EXECUTOR.shutdown();
        try {
            if (!EXECUTOR.awaitTermination(10L, TimeUnit.SECONDS)) {
                LOGGER.warn("convert-addon worker did not finish within 10s; requesting interrupt");
                EXECUTOR.shutdownNow();
            }
        }
        catch (InterruptedException e) {
            EXECUTOR.shutdownNow();
            Thread.currentThread().interrupt();
        }
    }

    private static int run(CommandContext<CommandSourceStack> ctx) {
        CommandSourceStack src = (CommandSourceStack)ctx.getSource();
        String arg = StringArgumentType.getString(ctx, (String)"path");
        return ConvertAddonCommand.runImpl(src, arg);
    }

    static int runImpl(CommandSourceStack src, String pathArg) {
        if (!src.getServer().isDedicatedServer()) {
            src.sendFailure((Component)Component.literal((String)"convert-addon: not available on a single-player (integrated) server. Run a dedicated dev server."));
            return 0;
        }
        Path addonRoot = ConvertAddonCommand.resolveAddonRoot(src, pathArg);
        if (addonRoot == null) {
            return 0;
        }
        CompletableFuture<?> current = running.get();
        if (current != null && !current.isDone()) {
            src.sendFailure((Component)Component.literal((String)"convert-addon: another conversion is already in progress. Wait for it to finish."));
            return 0;
        }
        ConvertAddonCommand.launch(src, addonRoot);
        return 1;
    }

    private static void launch(CommandSourceStack src, Path addonRoot) {
        CompletableFuture<Result> future = CompletableFuture.supplyAsync(() -> ConvertAddonCommand.runConversionOnWorker(addonRoot), EXECUTOR);
        running.set(future);
        src.sendSuccess(() -> Component.literal((String)("convert-addon started on " + String.valueOf(addonRoot) + " \u2014 running in background...")), false);
        future.whenComplete((result, ex) -> {
            try {
                src.getServer().execute(() -> ConvertAddonCommand.publishResult(src, result, ex));
            }
            catch (Exception dispatchFailure) {
                LOGGER.info("convert-addon finished during shutdown on {}; result on disk only, no chat delivered.", (Object)addonRoot);
            }
            running.compareAndSet(future, null);
        });
    }

    /*
     * Enabled aggressive block sorting
     * Enabled unnecessary exception pruning
     * Enabled aggressive exception aggregation
     */
    static Result runConversionOnWorker(Path addonRoot) {
        Path probeRoot;
        Path outputRoot = addonRoot.resolveSibling(String.valueOf(addonRoot.getFileName()) + "_converted");
        Path manifestPath = outputRoot.resolve("_conversion_manifest.json");
        if (Files.isRegularFile(manifestPath, new LinkOption[0])) {
            String recordedVersion = ConverterOutputManifest.readVersion(manifestPath);
            if (recordedVersion == null) return new Result.AlreadyConverted(addonRoot, outputRoot);
            if (recordedVersion.equals("9.0.0-dev-preview.5")) return new Result.AlreadyConverted(addonRoot, outputRoot);
            return new Result.VersionDrift(addonRoot, outputRoot, recordedVersion, "9.0.0-dev-preview.5");
        }
        LegacyLayoutDetector.Detection detection = LegacyLayoutDetector.scan(addonRoot);
        if (detection.isEmpty()) {
            return new Result.EmptyPack(addonRoot);
        }
        Path path = probeRoot = outputRoot.getParent() != null ? outputRoot.getParent() : addonRoot;
        if (!LegacyConversionDriver.isWritable(probeRoot)) {
            return new Result.NotWritable(probeRoot);
        }
        try (LegacyConversionDriver.LockHandle handle = LegacyConversionDriver.acquireLockAt(addonRoot);){
            if (!handle.isHeld()) {
                Result.LockContention lockContention = new Result.LockContention(handle.lockPath());
                return lockContention;
            }
            LegacyConversionDriver.DriverResult r = LegacyConversionDriver.runWithOutput(addonRoot, outputRoot, detection, ConversionMode.CONVERT);
            Result.Completed completed = new Result.Completed(r);
            return completed;
        }
        catch (IOException e) {
            LOGGER.error("convert-addon I/O error on {}: {}", new Object[]{addonRoot, e.getMessage(), e});
            return new Result.NotWritable(addonRoot);
        }
    }

    private static void publishResult(CommandSourceStack src, Result result, Throwable ex) {
        try {
            if (src.getServer().isStopped() || src.getServer().isShutdown()) {
                LOGGER.info("convert-addon completion skipped: server stopped/shut down");
                return;
            }
            if (ex != null) {
                src.sendFailure((Component)Component.literal((String)("convert-addon crashed: " + ex.getClass().getSimpleName() + ". See server log.")));
                LOGGER.error("convert-addon future completed exceptionally", ex);
                return;
            }
            ConvertAddonCommand.renderResultLines(src, result);
        }
        catch (Exception deliveryFailure) {
            try {
                LOGGER.warn("convert-addon result could not be delivered: {}", (Object)deliveryFailure.getMessage());
            }
            catch (RuntimeException runtimeException) {
                // empty catch block
            }
        }
    }

    private static void renderResultLines(CommandSourceStack src, Result result) {
        Result result2 = result;
        Objects.requireNonNull(result2);
        Result result3 = result2;
        int n = 0;
        switch (SwitchBootstraps.typeSwitch("typeSwitch", new Object[]{Result.EmptyPack.class, Result.NotWritable.class, Result.LockContention.class, Result.AlreadyConverted.class, Result.VersionDrift.class, Result.Completed.class}, (Object)result3, n)) {
            default: {
                throw new MatchException(null, null);
            }
            case 0: {
                Result.EmptyPack ep = (Result.EmptyPack)result3;
                src.sendSuccess(() -> Component.literal((String)("convert-addon: no legacy content detected at " + String.valueOf(ep.addonRoot()) + ".")), false);
                break;
            }
            case 1: {
                Result.NotWritable nw = (Result.NotWritable)result3;
                src.sendFailure((Component)Component.literal((String)("convert-addon: " + String.valueOf(nw.addonRoot()) + " is not writable. Fix permissions and retry.")));
                break;
            }
            case 2: {
                Result.LockContention lc = (Result.LockContention)result3;
                src.sendFailure((Component)Component.literal((String)("convert-addon: another convert pass holds the lock at " + String.valueOf(lc.lockFile()) + ". Wait and retry.")));
                break;
            }
            case 3: {
                Result.AlreadyConverted ac = (Result.AlreadyConverted)result3;
                src.sendSuccess(() -> Component.literal((String)("convert-addon: " + String.valueOf(ac.outputRoot()) + " already exists \u2014 skipping. Delete it to re-convert.")), false);
                break;
            }
            case 4: {
                Result.VersionDrift vd = (Result.VersionDrift)result3;
                src.sendFailure((Component)Component.literal((String)("convert-addon: " + String.valueOf(vd.outputRoot()) + " was produced by Mill\u00e9naire " + vd.recordedVersion() + " but the running converter is " + vd.runningVersion() + ". Delete the directory and re-run to rebuild with the current format.")));
                break;
            }
            case 5: {
                Result.Completed c = (Result.Completed)result3;
                LegacyConversionReport report = c.driver().report();
                int n2 = report.totalConverted();
                long ms = c.driver().elapsedMillis();
                if (report.hasBlockingAmbiguities()) {
                    int k = (int)report.skipped().stream().filter(s -> s.fixHint() != null).count();
                    src.sendFailure((Component)Component.literal((String)String.format("convert-addon: %d converted, %d blocking ambiguities in %d ms. See _conversion_report.txt and _conversion_unmapped.*", n2, k, ms)));
                    break;
                }
                src.sendSuccess(() -> Component.literal((String)String.format("convert-addon: %d files converted in %d ms. Clean \u2014 ready to ship.", n2, ms)), false);
            }
        }
    }

    static Path resolveAddonRoot(CommandSourceStack src, String arg) {
        Path serverRoot;
        AddonRootCheck result;
        Path candidate = Path.of(arg, new String[0]);
        if (!candidate.isAbsolute()) {
            candidate = src.getServer().getServerDirectory().resolve(candidate);
        }
        if ((result = ConvertAddonCommand.checkAddonRoot(candidate, serverRoot = src.getServer().getServerDirectory(), ConvertAddonCommand.isWindows())) instanceof AddonRootCheck.Fail) {
            AddonRootCheck.Fail fail = (AddonRootCheck.Fail)result;
            src.sendFailure((Component)Component.literal((String)("convert-addon: " + fail.reason())));
            return null;
        }
        return ((AddonRootCheck.Ok)result).root();
    }

    static AddonRootCheck checkAddonRoot(Path candidate, Path serverRoot, boolean isWindows) {
        boolean looksLikeAddon;
        boolean contained;
        Path realServer;
        Path realAddon;
        try {
            realAddon = candidate.toRealPath(new LinkOption[0]);
            realServer = serverRoot.toRealPath(new LinkOption[0]);
        }
        catch (IOException e) {
            return new AddonRootCheck.Fail("path does not exist or cannot be canonicalised: " + String.valueOf(candidate));
        }
        if (!Files.isDirectory(realAddon, new LinkOption[0])) {
            return new AddonRootCheck.Fail("not a directory: " + String.valueOf(realAddon));
        }
        if (isWindows) {
            String sep = File.separator;
            String addonLower = realAddon.toString().toLowerCase(Locale.ROOT) + sep;
            String serverLower = realServer.toString().toLowerCase(Locale.ROOT) + sep;
            contained = addonLower.startsWith(serverLower);
        } else {
            contained = realAddon.startsWith(realServer);
        }
        if (!contained) {
            return new AddonRootCheck.Fail("path escapes the server root: " + String.valueOf(realAddon));
        }
        boolean bl = looksLikeAddon = Files.isDirectory(realAddon.resolve("cultures"), new LinkOption[0]) || Files.isDirectory(realAddon.resolve("goals"), new LinkOption[0]) || Files.isDirectory(realAddon.resolve("quests"), new LinkOption[0]) || Files.isDirectory(realAddon.resolve("languages"), new LinkOption[0]) || Files.exists(realAddon.resolve("itemlist.txt"), new LinkOption[0]);
        if (!looksLikeAddon) {
            return new AddonRootCheck.Fail("path does not look like a Mill\u00e9naire legacy addon root (expected cultures/, goals/, quests/, languages/, or itemlist.txt)");
        }
        return new AddonRootCheck.Ok(realAddon);
    }

    private static boolean isWindows() {
        return System.getProperty("os.name", "").toLowerCase(Locale.ROOT).startsWith("windows");
    }

    static sealed interface Result {

        public record Completed(LegacyConversionDriver.DriverResult driver) implements Result
        {
        }

        public record VersionDrift(Path addonRoot, Path outputRoot, String recordedVersion, String runningVersion) implements Result
        {
        }

        public record AlreadyConverted(Path addonRoot, Path outputRoot) implements Result
        {
        }

        public record LockContention(Path lockFile) implements Result
        {
        }

        public record NotWritable(Path addonRoot) implements Result
        {
        }

        public record EmptyPack(Path addonRoot) implements Result
        {
        }
    }

    static sealed interface AddonRootCheck {

        public record Fail(String reason) implements AddonRootCheck
        {
        }

        public record Ok(Path root) implements AddonRootCheck
        {
        }
    }
}

