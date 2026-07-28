/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.mojang.brigadier.CommandDispatcher
 *  com.mojang.brigadier.builder.LiteralArgumentBuilder
 *  com.mojang.brigadier.context.CommandContext
 *  net.minecraft.commands.CommandSourceStack
 *  net.minecraft.commands.Commands
 *  net.minecraft.core.BlockPos
 *  net.minecraft.network.chat.Component
 *  net.minecraft.server.level.ServerLevel
 *  net.minecraft.server.level.ServerPlayer
 *  net.minecraft.world.level.ChunkPos
 *  net.minecraft.world.level.Level
 */
package org.millenaire.command;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import java.util.Collection;
import java.util.Set;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import org.millenaire.village.Village;
import org.millenaire.village.VillageChunkLoader;
import org.millenaire.village.VillageManager;
import org.millenaire.village.VillageSavedData;
import org.millenaire.world.VillageNotifier;

public final class VillagesCommand {
    private VillagesCommand() {
    }

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register((LiteralArgumentBuilder)((LiteralArgumentBuilder)Commands.literal((String)"millenaire").then(((LiteralArgumentBuilder)Commands.literal((String)"villages").requires(cs -> cs.hasPermission(0))).executes(VillagesCommand::execute))).then(((LiteralArgumentBuilder)Commands.literal((String)"chunkload").requires(source -> source.hasPermission(2))).executes(VillagesCommand::chunkloadAll)));
    }

    private static int execute(CommandContext<CommandSourceStack> ctx) {
        CommandSourceStack source = (CommandSourceStack)ctx.getSource();
        ServerPlayer player = source.getPlayer();
        ServerLevel level = source.getLevel();
        if (level.dimension() != Level.OVERWORLD) {
            source.sendFailure((Component)Component.translatable((String)"command.millenaire.error.no_overworld_villages"));
            return 0;
        }
        if (player != null) {
            VillageNotifier.sendVillageList(player, level);
        } else {
            VillageManager manager = VillageSavedData.get(level).getVillageManager();
            Collection<Village> villages = manager.getAllVillages();
            if (villages.isEmpty()) {
                source.sendSuccess(() -> Component.translatable((String)"command.millenaire.villages.empty"), false);
                return 0;
            }
            StringBuilder sb = new StringBuilder();
            sb.append(villages.size()).append(" village(s):\n");
            for (Village v : villages) {
                BlockPos c = v.getCenter();
                String name = v.getVillageName() != null ? v.getVillageName() : "???";
                sb.append("  ").append(name).append(" (").append(v.getVillageTypeId().getPath()).append(")").append(" at ").append(c.getX()).append(", ").append(c.getY()).append(", ").append(c.getZ()).append(" \u2014 ").append(v.getBuildings().size()).append(" buildings, ").append(v.getVillagerUuids().size()).append(" villagers\n");
            }
            source.sendSuccess(() -> Component.literal((String)sb.toString().trim()), false);
        }
        return 1;
    }

    private static int chunkloadAll(CommandContext<CommandSourceStack> ctx) {
        CommandSourceStack source = (CommandSourceStack)ctx.getSource();
        ServerLevel level = source.getLevel();
        if (level.dimension() != Level.OVERWORLD) {
            source.sendFailure((Component)Component.translatable((String)"command.millenaire.error.not_in_overworld"));
            return 0;
        }
        VillageManager manager = VillageSavedData.get(level).getVillageManager();
        int count = 0;
        for (Village v : manager.getAllVillages()) {
            v.setForceActive(true);
            if (!v.isChunksForceLoaded()) {
                Set<ChunkPos> chunks = v.computeVillageChunks();
                VillageChunkLoader.forceVillageChunks(level, v.getCenter(), chunks);
                v.setLoadedChunks(chunks);
                v.setChunksForceLoaded(true);
            }
            ++count;
        }
        int finalCount = count;
        source.sendSuccess(() -> Component.translatable((String)"command.millenaire.villages.forceactive", (Object[])new Object[]{finalCount}), false);
        return 1;
    }
}

