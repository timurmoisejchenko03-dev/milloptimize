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
 *  net.minecraft.core.BlockPos
 *  net.minecraft.core.Position
 *  net.minecraft.network.chat.Component
 *  net.minecraft.server.level.ServerLevel
 *  net.minecraft.server.level.ServerPlayer
 */
package org.millenaire.command;

import com.mojang.brigadier.arguments.ArgumentType;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Position;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import org.millenaire.village.BuildingFinalizer;
import org.millenaire.village.Village;
import org.millenaire.village.VillageGrowthManager;
import org.millenaire.village.VillageSavedData;

public final class GrowCommand {
    private GrowCommand() {
    }

    public static void registerUnder(LiteralArgumentBuilder<CommandSourceStack> parent) {
        parent.then(Commands.literal((String)"grow").then(Commands.argument((String)"n", (ArgumentType)IntegerArgumentType.integer((int)1, (int)200)).executes(GrowCommand::execute)));
    }

    private static int execute(CommandContext<CommandSourceStack> ctx) {
        boolean ok;
        CommandSourceStack src = (CommandSourceStack)ctx.getSource();
        int n = IntegerArgumentType.getInteger(ctx, (String)"n");
        ServerLevel level = src.getLevel();
        ServerPlayer player = src.getPlayer();
        BlockPos searchPos = player != null ? player.blockPosition() : BlockPos.containing((Position)src.getPosition());
        Village v = VillageSavedData.get(level).getVillageManager().findNearestVillage(searchPos, 5000.0);
        if (v == null) {
            src.sendFailure((Component)Component.translatable((String)"command.millenaire.error.no_village_radius", (Object[])new Object[]{5000}));
            return 0;
        }
        int placed = 0;
        for (int i = 0; i < n && (ok = VillageGrowthManager.rushOneProject(level, v)); ++i) {
            ++placed;
            v.getPathManager().markPathsDirty();
        }
        if (placed > 0) {
            BuildingFinalizer.applyVillageUpdates(level, v);
        }
        v.markDirty();
        int p = placed;
        int skipped = n - placed;
        src.sendSuccess(() -> Component.translatable((String)"command.millenaire.grow.success", (Object[])new Object[]{p, skipped}), false);
        return placed;
    }
}

