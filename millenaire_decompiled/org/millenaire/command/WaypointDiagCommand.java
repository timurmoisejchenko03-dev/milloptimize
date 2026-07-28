/*
 * Decompiled with CFR 0.150.
 * 
 * Could not load the following classes:
 *  com.mojang.brigadier.arguments.ArgumentType
 *  com.mojang.brigadier.arguments.IntegerArgumentType
 *  com.mojang.brigadier.builder.LiteralArgumentBuilder
 *  com.mojang.brigadier.context.CommandContext
 *  net.minecraft.commands.CommandSourceStack
 *  net.minecraft.commands.Commands
 *  net.minecraft.network.chat.Component
 *  net.minecraft.server.level.ServerLevel
 *  net.minecraft.world.level.Level
 */
package org.millenaire.command;

import com.mojang.brigadier.arguments.ArgumentType;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import java.util.ArrayList;
import java.util.Locale;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import org.millenaire.building.BuildingId;
import org.millenaire.village.Village;
import org.millenaire.village.VillageSavedData;
import org.millenaire.village.WaypointDiagnostics;

public final class WaypointDiagCommand {
    private WaypointDiagCommand() {
    }

    public static void registerUnder(LiteralArgumentBuilder<CommandSourceStack> parent) {
        parent.then(((LiteralArgumentBuilder)Commands.literal((String)"waypoint-report").executes(ctx -> WaypointDiagCommand.runReport((CommandContext<CommandSourceStack>)ctx, 200))).then(Commands.argument((String)"nodeBudget", (ArgumentType)IntegerArgumentType.integer((int)50, (int)20000)).executes(ctx -> WaypointDiagCommand.runReport((CommandContext<CommandSourceStack>)ctx, IntegerArgumentType.getInteger((CommandContext)ctx, (String)"nodeBudget")))));
    }

    private static int runReport(CommandContext<CommandSourceStack> ctx, int nodeBudget) {
        CommandSourceStack source = (CommandSourceStack)ctx.getSource();
        ServerLevel level = source.getLevel();
        if (level.dimension() != Level.OVERWORLD) {
            source.sendFailure((Component)Component.literal((String)"Run this command in the Overworld."));
            return 0;
        }
        ArrayList<Village> villages = new ArrayList<Village>(VillageSavedData.get(level).getVillageManager().getAllVillages());
        if (villages.isEmpty()) {
            source.sendSuccess(() -> Component.literal((String)"No villages loaded."), false);
            return 1;
        }
        int totalFindings = 0;
        int totalProbed = 0;
        for (Village village : villages) {
            WaypointDiagnostics.Report report = WaypointDiagnostics.analyze(level, village, nodeBudget);
            totalFindings += report.findings().size();
            totalProbed += report.probedCount();
            WaypointDiagCommand.emitVillageSection(source, report);
        }
        int findingsFinal = totalFindings;
        int probedFinal = totalProbed;
        int budgetFinal = nodeBudget;
        source.sendSuccess(() -> Component.literal((String)("Summary: " + villages.size() + " villages \u2014 " + probedFinal + " waypoints probed \u2014 " + findingsFinal + " unreachable (nodeBudget=" + budgetFinal + ")")), false);
        return 1;
    }

    private static void emitVillageSection(CommandSourceStack source, WaypointDiagnostics.Report report) {
        Village v = report.village();
        String header = "\u2500\u2500 " + String.valueOf((Object)v.getVillageTypeId()) + " @ " + v.getCenter().toShortString() + " \u2014 " + report.probedCount() + " probed, " + report.findings().size() + " unreachable (" + report.anchors().size() + " anchors)";
        source.sendSuccess(() -> Component.literal((String)header), false);
        if (report.findings().isEmpty()) {
            source.sendSuccess(() -> Component.literal((String)"  clean"), false);
            return;
        }
        for (WaypointDiagnostics.Finding f : report.findings()) {
            String planSet = f.planSetId() != null ? f.planSetId().toString() : "?";
            String line = String.format(Locale.ROOT, "  WARN %s  wp=%s  nearestAnchor=%s  dist=%.1f  building=%s", planSet, f.waypointPos().toShortString(), f.nearestAnchor().toShortString(), f.distanceToNearestAnchor(), WaypointDiagCommand.shortBuildingId(f.buildingId()));
            source.sendSuccess(() -> Component.literal((String)line), false);
        }
    }

    private static String shortBuildingId(BuildingId id) {
        return id.uuid().toString().substring(0, 8);
    }
}

