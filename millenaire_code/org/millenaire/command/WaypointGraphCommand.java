/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.mojang.brigadier.builder.LiteralArgumentBuilder
 *  com.mojang.brigadier.context.CommandContext
 *  net.minecraft.commands.CommandSourceStack
 *  net.minecraft.commands.Commands
 *  net.minecraft.network.chat.Component
 *  net.minecraft.server.level.ServerLevel
 *  net.minecraft.world.level.Level
 */
package org.millenaire.command;

import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import java.util.ArrayList;
import java.util.Locale;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import org.millenaire.village.Village;
import org.millenaire.village.VillageSavedData;
import org.millenaire.village.VillageWaypointGraph;

public final class WaypointGraphCommand {
    private WaypointGraphCommand() {
    }

    public static void registerUnder(LiteralArgumentBuilder<CommandSourceStack> parent) {
        parent.then(Commands.literal((String)"rebuild-waypoint-graphs").executes(WaypointGraphCommand::run));
    }

    private static int run(CommandContext<CommandSourceStack> ctx) {
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
        int totalNodes = 0;
        int totalEdges = 0;
        long startNs = System.nanoTime();
        for (Village village : villages) {
            village.rebuildWaypointGraph(level);
            VillageWaypointGraph graph = village.getWaypointGraph();
            int nodes = graph.waypointCount();
            int edges = graph.getEdges().size();
            totalNodes += nodes;
            totalEdges += edges;
            String label = String.valueOf(village.getVillageTypeId()) + " @ " + village.getCenter().toShortString();
            String line = String.format(Locale.ROOT, "  %s \u2014 %d nodes, %d edges", label, nodes, edges);
            source.sendSuccess(() -> Component.literal((String)line), false);
        }
        long elapsedMs = (System.nanoTime() - startNs) / 1000000L;
        int villagesCount = villages.size();
        int nodesFinal = totalNodes;
        int edgesFinal = totalEdges;
        long elapsedFinal = elapsedMs;
        source.sendSuccess(() -> Component.literal((String)("Rebuilt " + villagesCount + " village(s) \u2014 total " + nodesFinal + " nodes / " + edgesFinal + " edges (in " + elapsedFinal + "ms).")), false);
        return 1;
    }
}

