/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.mojang.brigadier.arguments.ArgumentType
 *  com.mojang.brigadier.arguments.IntegerArgumentType
 *  com.mojang.brigadier.builder.LiteralArgumentBuilder
 *  com.mojang.brigadier.context.CommandContext
 *  javax.annotation.Nullable
 *  net.minecraft.commands.CommandSourceStack
 *  net.minecraft.commands.Commands
 *  net.minecraft.core.BlockPos
 *  net.minecraft.core.Position
 *  net.minecraft.network.chat.Component
 *  net.minecraft.server.level.ServerLevel
 *  net.minecraft.world.level.Level
 */
package org.millenaire.command;

import com.mojang.brigadier.arguments.ArgumentType;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.OpenOption;
import java.nio.file.Path;
import java.nio.file.attribute.FileAttribute;
import java.util.Collection;
import java.util.List;
import javax.annotation.Nullable;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Position;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import org.millenaire.command.export.BuildingCheckExporter;
import org.millenaire.command.export.BuildingCostExporter;
import org.millenaire.command.export.BuildingCostJsonExporter;
import org.millenaire.command.export.ExportedBuildingsCostExporter;
import org.millenaire.command.export.MapExporter;
import org.millenaire.command.export.ScanExporter;
import org.millenaire.command.export.StateExporter;
import org.millenaire.command.export.VillagerTypeJsonExporter;
import org.millenaire.command.export.WatchExporter;
import org.millenaire.village.Village;
import org.millenaire.village.VillageHistoryEntry;
import org.millenaire.village.VillageManager;
import org.millenaire.village.VillageSavedData;

public final class ExportCommand {
    private static final Path EXPORT_DIR = Path.of("millenaire-export", new String[0]);

    private ExportCommand() {
    }

    public static void registerUnder(LiteralArgumentBuilder<CommandSourceStack> parent) {
        parent.then(((LiteralArgumentBuilder)((LiteralArgumentBuilder)((LiteralArgumentBuilder)((LiteralArgumentBuilder)((LiteralArgumentBuilder)((LiteralArgumentBuilder)((LiteralArgumentBuilder)((LiteralArgumentBuilder)((LiteralArgumentBuilder)Commands.literal((String)"export").then(Commands.literal((String)"map").executes(ExportCommand::exportMap))).then(Commands.literal((String)"state").executes(ExportCommand::exportState))).then(((LiteralArgumentBuilder)Commands.literal((String)"scan").executes(ctx -> ExportCommand.exportScan((CommandContext<CommandSourceStack>)ctx, 30))).then(Commands.argument((String)"radius", (ArgumentType)IntegerArgumentType.integer((int)5, (int)100)).executes(ctx -> ExportCommand.exportScan((CommandContext<CommandSourceStack>)ctx, IntegerArgumentType.getInteger((CommandContext)ctx, (String)"radius")))))).then(Commands.literal((String)"watch").executes(ExportCommand::exportWatch))).then(Commands.literal((String)"check").executes(ExportCommand::exportCheck))).then(Commands.literal((String)"buildings").executes(ExportCommand::exportBuildingCosts))).then(Commands.literal((String)"buildings-json").executes(ExportCommand::exportBuildingCostsJson))).then(Commands.literal((String)"exported-buildings").executes(ExportCommand::exportExportedBuildingCosts))).then(Commands.literal((String)"villagers-json").executes(ExportCommand::exportVillagerTypesJson))).then(((LiteralArgumentBuilder)Commands.literal((String)"history").executes(ExportCommand::exportHistory)).then(Commands.literal((String)"clear").executes(ExportCommand::clearHistory))));
    }

    @Nullable
    private static Village findVillage(CommandSourceStack source) {
        ServerLevel level = source.getLevel();
        if (level.dimension() != Level.OVERWORLD) {
            return null;
        }
        BlockPos searchPos = BlockPos.containing((Position)source.getPosition());
        return VillageSavedData.get(level).getVillageManager().findNearestVillage(searchPos, 5000.0);
    }

    private static Path ensureExportDir() throws IOException {
        if (!Files.exists(EXPORT_DIR, new LinkOption[0])) {
            Files.createDirectories(EXPORT_DIR, new FileAttribute[0]);
        }
        return EXPORT_DIR;
    }

    private static int exportMap(CommandContext<CommandSourceStack> ctx) {
        CommandSourceStack source = (CommandSourceStack)ctx.getSource();
        Village village = ExportCommand.findVillage(source);
        if (village == null) {
            source.sendFailure((Component)Component.literal((String)"No village found."));
            return 0;
        }
        try {
            Path dir = ExportCommand.ensureExportDir();
            Path file = MapExporter.export(source.getLevel(), village, dir);
            source.sendSuccess(() -> Component.literal((String)("Map \u2192 " + String.valueOf(file))), false);
            return 1;
        }
        catch (IOException e) {
            source.sendFailure((Component)Component.literal((String)("Export error: " + e.getMessage())));
            return 0;
        }
    }

    private static int exportState(CommandContext<CommandSourceStack> ctx) {
        CommandSourceStack source = (CommandSourceStack)ctx.getSource();
        Village village = ExportCommand.findVillage(source);
        if (village == null) {
            source.sendFailure((Component)Component.literal((String)"No village found."));
            return 0;
        }
        try {
            Path dir = ExportCommand.ensureExportDir();
            Path file = StateExporter.export(source.getLevel(), village, dir);
            source.sendSuccess(() -> Component.literal((String)("State \u2192 " + String.valueOf(file))), false);
            return 1;
        }
        catch (IOException e) {
            source.sendFailure((Component)Component.literal((String)("Export error: " + e.getMessage())));
            return 0;
        }
    }

    private static int exportScan(CommandContext<CommandSourceStack> ctx, int radius) {
        CommandSourceStack source = (CommandSourceStack)ctx.getSource();
        Village village = ExportCommand.findVillage(source);
        if (village == null) {
            source.sendFailure((Component)Component.literal((String)"No village found."));
            return 0;
        }
        try {
            Path dir = ExportCommand.ensureExportDir();
            Path file = ScanExporter.export(source.getLevel(), village, radius, dir);
            source.sendSuccess(() -> Component.literal((String)("Scan \u2192 " + String.valueOf(file))), false);
            return 1;
        }
        catch (IOException e) {
            source.sendFailure((Component)Component.literal((String)("Export error: " + e.getMessage())));
            return 0;
        }
    }

    private static int exportCheck(CommandContext<CommandSourceStack> ctx) {
        CommandSourceStack source = (CommandSourceStack)ctx.getSource();
        Village village = ExportCommand.findVillage(source);
        if (village == null) {
            source.sendFailure((Component)Component.literal((String)"No village found."));
            return 0;
        }
        try {
            Path dir = ExportCommand.ensureExportDir();
            Path file = BuildingCheckExporter.export(source.getLevel(), village, dir);
            source.sendSuccess(() -> Component.literal((String)("Check \u2192 " + String.valueOf(file))), false);
            return 1;
        }
        catch (IOException e) {
            source.sendFailure((Component)Component.literal((String)("Export error: " + e.getMessage())));
            return 0;
        }
    }

    private static int exportBuildingCosts(CommandContext<CommandSourceStack> ctx) {
        CommandSourceStack source = (CommandSourceStack)ctx.getSource();
        try {
            Path dir = ExportCommand.ensureExportDir();
            Path outDir = BuildingCostExporter.export(dir);
            source.sendSuccess(() -> Component.literal((String)("Building costs \u2192 " + String.valueOf(outDir))), false);
            return 1;
        }
        catch (IOException e) {
            source.sendFailure((Component)Component.literal((String)("Export error: " + e.getMessage())));
            return 0;
        }
    }

    private static int exportBuildingCostsJson(CommandContext<CommandSourceStack> ctx) {
        CommandSourceStack source = (CommandSourceStack)ctx.getSource();
        try {
            Path dir = ExportCommand.ensureExportDir();
            Path outDir = BuildingCostJsonExporter.export(dir);
            source.sendSuccess(() -> Component.literal((String)("Building costs (JSON) \u2192 " + String.valueOf(outDir))), false);
            return 1;
        }
        catch (IOException e) {
            source.sendFailure((Component)Component.literal((String)("Export error: " + e.getMessage())));
            return 0;
        }
    }

    private static int exportExportedBuildingCosts(CommandContext<CommandSourceStack> ctx) {
        CommandSourceStack source = (CommandSourceStack)ctx.getSource();
        try {
            Path dir = ExportCommand.ensureExportDir();
            Path outDir = ExportedBuildingsCostExporter.export(source.getLevel(), dir);
            if (outDir == null) {
                source.sendFailure((Component)Component.literal((String)"No exports found in world's exports/ directory"));
                return 0;
            }
            source.sendSuccess(() -> Component.literal((String)("Exported-buildings costs \u2192 " + String.valueOf(outDir))), false);
            return 1;
        }
        catch (IOException e) {
            source.sendFailure((Component)Component.literal((String)("Export error: " + e.getMessage())));
            return 0;
        }
    }

    private static int exportVillagerTypesJson(CommandContext<CommandSourceStack> ctx) {
        CommandSourceStack source = (CommandSourceStack)ctx.getSource();
        try {
            Path dir = ExportCommand.ensureExportDir();
            Path outDir = VillagerTypeJsonExporter.export(dir);
            source.sendSuccess(() -> Component.literal((String)("Villager types (JSON) \u2192 " + String.valueOf(outDir))), false);
            return 1;
        }
        catch (IOException e) {
            source.sendFailure((Component)Component.literal((String)("Export error: " + e.getMessage())));
            return 0;
        }
    }

    private static int exportWatch(CommandContext<CommandSourceStack> ctx) {
        CommandSourceStack source = (CommandSourceStack)ctx.getSource();
        Village village = ExportCommand.findVillage(source);
        if (village == null) {
            source.sendFailure((Component)Component.literal((String)"No village found."));
            return 0;
        }
        try {
            Path dir = ExportCommand.ensureExportDir();
            Path file = WatchExporter.export(source.getLevel(), village, dir);
            source.sendSuccess(() -> Component.literal((String)("Watch \u2192 " + String.valueOf(file))), false);
            return 1;
        }
        catch (IOException e) {
            source.sendFailure((Component)Component.literal((String)("Export error: " + e.getMessage())));
            return 0;
        }
    }

    @Nullable
    private static Village resolveHistoryVillage(CommandSourceStack source) {
        ServerLevel level = source.getLevel();
        if (level.dimension() != Level.OVERWORLD) {
            source.sendFailure((Component)Component.literal((String)"Command reserved for the Overworld."));
            return null;
        }
        VillageManager manager = VillageSavedData.get(level).getVillageManager();
        Collection<Village> villages = manager.getAllVillages();
        if (villages.isEmpty()) {
            source.sendFailure((Component)Component.literal((String)"No villages."));
            return null;
        }
        if (villages.size() == 1) {
            return villages.iterator().next();
        }
        StringBuilder sb = new StringBuilder("Multiple villages, please specify:\n");
        for (Village v : villages) {
            sb.append("  - ").append(v.getVillageTypeId().getPath()).append(" [").append(v.getId().uuid().toString(), 0, 8).append("]").append(" centre=").append(v.getCenter().toShortString()).append("\n");
        }
        source.sendFailure((Component)Component.literal((String)sb.toString()));
        return null;
    }

    private static int exportHistory(CommandContext<CommandSourceStack> ctx) {
        CommandSourceStack source = (CommandSourceStack)ctx.getSource();
        Village village = ExportCommand.resolveHistoryVillage(source);
        if (village == null) {
            return 0;
        }
        List<VillageHistoryEntry> events = village.getHistory();
        if (events.isEmpty()) {
            source.sendSuccess(() -> Component.literal((String)"History is empty."), false);
            return 1;
        }
        long startTick = village.getHistoryStartTick();
        String typeName = village.getVillageTypeId().getPath();
        String id8 = village.getId().uuid().toString().substring(0, 8);
        StringBuilder full = new StringBuilder();
        full.append("=== Village ").append(typeName).append(" [").append(id8).append("]").append(" (center: ").append(village.getCenter().toShortString()).append(") ===\n");
        long lastTick = events.get(events.size() - 1).tick();
        full.append("Duration: ").append(ExportCommand.formatRelative(lastTick - startTick)).append(" (").append(events.size()).append(" events)\n\n");
        for (VillageHistoryEntry e : events) {
            full.append("[").append(ExportCommand.formatRelative(e.tick() - startTick)).append("]  ").append(e.message()).append("\n");
        }
        try {
            Path dir = ExportCommand.ensureExportDir();
            long currentTick = source.getLevel().getServer().getTickCount();
            String safeTypeName = typeName.replace('/', '_');
            String filename = "history-" + safeTypeName + "-" + id8 + "-T" + currentTick + ".txt";
            Path file = dir.resolve(filename);
            Files.writeString(file, (CharSequence)full.toString(), new OpenOption[0]);
            int maxRcon = 10;
            StringBuilder rcon = new StringBuilder();
            rcon.append("=== ").append(typeName).append(" [").append(id8).append("] ===\n");
            int shown = Math.min(events.size(), maxRcon);
            for (int i = 0; i < shown; ++i) {
                VillageHistoryEntry e = events.get(i);
                rcon.append("[").append(ExportCommand.formatRelative(e.tick() - startTick)).append("]  ").append(e.message()).append("\n");
            }
            if (events.size() > maxRcon) {
                rcon.append("... and ").append(events.size() - maxRcon).append(" more\n");
            }
            rcon.append("\u2192 ").append(file);
            source.sendSuccess(() -> Component.literal((String)rcon.toString()), false);
            return 1;
        }
        catch (IOException e) {
            source.sendFailure((Component)Component.literal((String)("Write error: " + e.getMessage())));
            return 0;
        }
    }

    private static int clearHistory(CommandContext<CommandSourceStack> ctx) {
        CommandSourceStack source = (CommandSourceStack)ctx.getSource();
        Village village = ExportCommand.resolveHistoryVillage(source);
        if (village == null) {
            return 0;
        }
        int count = village.getHistory().size();
        village.clearHistory();
        source.sendSuccess(() -> Component.literal((String)("History cleared (" + count + " events).")), false);
        return 1;
    }

    static String formatRelative(long deltaTicks) {
        long totalSeconds = deltaTicks / 20L;
        long minutes = totalSeconds / 60L;
        long seconds = totalSeconds % 60L;
        return String.format("T+%d:%02d", minutes, seconds);
    }
}

