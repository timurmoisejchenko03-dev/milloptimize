/*
 * Decompiled with CFR 0.152.
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
 *  net.minecraft.server.level.ServerPlayer
 *  net.minecraft.world.entity.Entity
 *  net.minecraft.world.phys.AABB
 */
package org.millenaire.command;

import com.mojang.brigadier.arguments.ArgumentType;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import java.util.List;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.AABB;
import org.millenaire.entity.MillVillager;
import org.millenaire.village.Village;

public final class HireDevCommand {
    private static final double SEARCH_RADIUS = 8.0;

    private HireDevCommand() {
    }

    public static void registerUnder(LiteralArgumentBuilder<CommandSourceStack> parent) {
        parent.then(((LiteralArgumentBuilder)Commands.literal((String)"hire").then(Commands.literal((String)"clear").executes(HireDevCommand::clearHire))).then(Commands.argument((String)"hours", (ArgumentType)IntegerArgumentType.integer((int)0)).executes(HireDevCommand::doHire)));
    }

    private static MillVillager findNearest(ServerPlayer player) {
        ServerLevel level = player.serverLevel();
        AABB box = player.getBoundingBox().inflate(8.0);
        List list = level.getEntitiesOfClass(MillVillager.class, box);
        MillVillager best = null;
        double bestSq = Double.MAX_VALUE;
        for (MillVillager v : list) {
            double d = player.distanceToSqr((Entity)v);
            if (!(d < bestSq)) continue;
            bestSq = d;
            best = v;
        }
        return best;
    }

    private static int doHire(CommandContext<CommandSourceStack> ctx) {
        CommandSourceStack source = (CommandSourceStack)ctx.getSource();
        Entity entity = source.getEntity();
        if (!(entity instanceof ServerPlayer)) {
            source.sendFailure((Component)Component.literal((String)"Must be run by a player."));
            return 0;
        }
        ServerPlayer player = (ServerPlayer)entity;
        int hours = IntegerArgumentType.getInteger(ctx, (String)"hours");
        MillVillager v = HireDevCommand.findNearest(player);
        if (v == null || v.getVillageId() == null) {
            source.sendFailure((Component)Component.literal((String)"No nearby villager (with a village) found."));
            return 0;
        }
        ServerLevel level = player.serverLevel();
        Village village = Village.resolve(level, v.getVillageId());
        if (village == null) {
            source.sendFailure((Component)Component.literal((String)"Villager has no resolvable village."));
            return 0;
        }
        long until = level.getGameTime() + (long)hours * 1000L;
        if (hours == 0) {
            until = level.getGameTime() + 24000L;
        }
        village.setVillagerHired(level, v.getUUID(), player.getUUID(), until);
        long u = until;
        source.sendSuccess(() -> Component.literal((String)("Hired " + v.getName().getString() + " until gametime " + u)), false);
        return 1;
    }

    private static int clearHire(CommandContext<CommandSourceStack> ctx) {
        CommandSourceStack source = (CommandSourceStack)ctx.getSource();
        Entity entity = source.getEntity();
        if (!(entity instanceof ServerPlayer)) {
            source.sendFailure((Component)Component.literal((String)"Must be run by a player."));
            return 0;
        }
        ServerPlayer player = (ServerPlayer)entity;
        MillVillager v = HireDevCommand.findNearest(player);
        if (v == null || v.getVillageId() == null) {
            source.sendFailure((Component)Component.literal((String)"No nearby villager (with a village) found."));
            return 0;
        }
        ServerLevel level = player.serverLevel();
        Village village = Village.resolve(level, v.getVillageId());
        if (village == null) {
            source.sendFailure((Component)Component.literal((String)"Villager has no resolvable village."));
            return 0;
        }
        village.setVillagerHired(level, v.getUUID(), null, 0L);
        source.sendSuccess(() -> Component.literal((String)("Cleared hire on " + v.getName().getString())), false);
        return 1;
    }
}

