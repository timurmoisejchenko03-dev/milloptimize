/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.google.gson.Gson
 *  com.google.gson.GsonBuilder
 *  com.mojang.brigadier.builder.LiteralArgumentBuilder
 *  com.mojang.brigadier.context.CommandContext
 *  net.minecraft.commands.CommandSourceStack
 *  net.minecraft.commands.Commands
 *  net.minecraft.network.chat.Component
 */
package org.millenaire.command;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import java.util.Map;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import org.millenaire.Millenaire;
import org.millenaire.world.VillageSpawnQueue;

public final class SpawnInfoCommand {
    private static final Gson GSON = new GsonBuilder().create();

    private SpawnInfoCommand() {
    }

    public static void registerUnder(LiteralArgumentBuilder<CommandSourceStack> parent) {
        parent.then(Commands.literal((String)"spawn-info").executes(SpawnInfoCommand::execute));
    }

    private static int execute(CommandContext<CommandSourceStack> ctx) {
        CommandSourceStack source = (CommandSourceStack)ctx.getSource();
        VillageSpawnQueue queue = Millenaire.getSpawnQueue();
        if (queue == null) {
            source.sendFailure((Component)Component.literal((String)"Spawn queue not initialized."));
            return 0;
        }
        Map<String, Object> stats = queue.getStats();
        source.sendSuccess(() -> Component.literal((String)GSON.toJson((Object)stats)), false);
        return 1;
    }
}

