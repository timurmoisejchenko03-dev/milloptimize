/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.mojang.brigadier.arguments.ArgumentType
 *  com.mojang.brigadier.arguments.StringArgumentType
 *  com.mojang.brigadier.builder.LiteralArgumentBuilder
 *  com.mojang.brigadier.context.CommandContext
 *  net.minecraft.commands.CommandSourceStack
 *  net.minecraft.commands.Commands
 *  net.minecraft.core.BlockPos
 *  net.minecraft.core.Position
 *  net.minecraft.network.chat.Component
 *  net.minecraft.server.level.ServerLevel
 *  net.minecraft.server.level.ServerPlayer
 */
package org.millenaire.command;

import com.mojang.brigadier.arguments.ArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.OpenOption;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.attribute.FileAttribute;
import java.util.List;
import java.util.Map;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Position;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import org.millenaire.building.BuildingId;
import org.millenaire.village.Village;
import org.millenaire.village.VillageSavedData;
import org.millenaire.village.path.PathDiagnostic;

public final class PathsCommand {
    private PathsCommand() {
    }

    public static void registerUnder(LiteralArgumentBuilder<CommandSourceStack> parent) {
        parent.then(((LiteralArgumentBuilder)((LiteralArgumentBuilder)Commands.literal((String)"paths").then(Commands.literal((String)"diagnose").executes(PathsCommand::diagnose))).then(((LiteralArgumentBuilder)Commands.literal((String)"rebuild").executes(ctx -> PathsCommand.rebuild((CommandContext<CommandSourceStack>)ctx, false))).then(Commands.literal((String)"--async").executes(ctx -> PathsCommand.rebuild((CommandContext<CommandSourceStack>)ctx, true))))).then(Commands.literal((String)"dump").then(Commands.argument((String)"name", (ArgumentType)StringArgumentType.word()).executes(PathsCommand::dump))));
    }

    private static Village nearestVillage(CommandContext<CommandSourceStack> ctx) {
        CommandSourceStack src = (CommandSourceStack)ctx.getSource();
        ServerLevel level = src.getLevel();
        ServerPlayer player = src.getPlayer();
        BlockPos searchPos = player != null ? player.blockPosition() : BlockPos.containing((Position)src.getPosition());
        return VillageSavedData.get(level).getVillageManager().findNearestVillage(searchPos, 5000.0);
    }

    private static int diagnose(CommandContext<CommandSourceStack> ctx) {
        String line;
        CommandSourceStack src = (CommandSourceStack)ctx.getSource();
        Village v = PathsCommand.nearestVillage(ctx);
        if (v == null) {
            src.sendFailure((Component)Component.literal((String)"No village within 5000"));
            return 0;
        }
        if (v.getPathManager().isDiagnosticStale(src.getLevel())) {
            src.sendSuccess(() -> Component.literal((String)"[stale: run /millenaire dev paths rebuild first]"), false);
        }
        Map<BuildingId, PathDiagnostic> diags = v.getPathManager().getLastDiagnostics();
        List<PathDiagnostic> laterals = v.getPathManager().getLateralDiagnostics();
        int connected = 0;
        int disconnected = 0;
        for (PathDiagnostic d : diags.values()) {
            if (d.connected()) {
                ++connected;
            } else {
                ++disconnected;
            }
            line = String.format("%s@%s tier=%d/%d src=%s%s dst=%s conn=%s fail=%s", d.planSetId(), d.origin().toShortString(), d.expectedTier(), d.effectiveTier(), d.source() == null ? "null" : d.source().toShortString(), d.sourceIsFallback() ? "(fb)" : "", d.destination() == null ? "null" : d.destination().toShortString(), d.connected(), d.failure() == null ? "-" : d.failure().name());
            src.sendSuccess(() -> Component.literal((String)line), false);
        }
        for (PathDiagnostic d : laterals) {
            line = String.format("[lateral] %s@%s tier=%d src=%s dst=%s fail=%s", d.planSetId(), d.origin().toShortString(), d.effectiveTier(), d.source() == null ? "null" : d.source().toShortString(), d.destination() == null ? "null" : d.destination().toShortString(), d.failure() == null ? "-" : d.failure().name());
            src.sendSuccess(() -> Component.literal((String)line), false);
        }
        int fc = connected;
        int fdc = disconnected;
        int flc = laterals.size();
        src.sendSuccess(() -> Component.literal((String)("summary: connected=" + fc + " disconnected=" + fdc + " lateral=" + flc)), false);
        return 1;
    }

    private static int rebuild(CommandContext<CommandSourceStack> ctx, boolean async) {
        CommandSourceStack src = (CommandSourceStack)ctx.getSource();
        Village v = PathsCommand.nearestVillage(ctx);
        if (v == null) {
            src.sendFailure((Component)Component.literal((String)"No village within 5000"));
            return 0;
        }
        v.getPathManager().recalculatePaths(src.getLevel(), v, !async);
        boolean finalAsync = async;
        src.sendSuccess(() -> Component.literal((String)("rebuild done (" + (finalAsync ? "async" : "sync") + ")")), false);
        return 1;
    }

    private static int dump(CommandContext<CommandSourceStack> ctx) {
        CommandSourceStack src = (CommandSourceStack)ctx.getSource();
        Village v = PathsCommand.nearestVillage(ctx);
        if (v == null) {
            src.sendFailure((Component)Component.literal((String)"No village within 5000"));
            return 0;
        }
        String name = StringArgumentType.getString(ctx, (String)"name");
        Path out = Paths.get("debug", name + ".json");
        try {
            Files.createDirectories(out.getParent(), new FileAttribute[0]);
            String json = v.getPathManager().toDumpJson(src.getLevel(), v);
            Files.writeString(out, (CharSequence)json, new OpenOption[0]);
            src.sendSuccess(() -> Component.literal((String)("dumped: " + String.valueOf(out))), false);
        }
        catch (IOException ex) {
            src.sendFailure((Component)Component.literal((String)("dump failed: " + ex.getMessage())));
            return 0;
        }
        return 1;
    }
}

