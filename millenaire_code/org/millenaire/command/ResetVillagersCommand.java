/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
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

import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Position;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import org.millenaire.village.Village;
import org.millenaire.village.VillageIntegrityChecker;
import org.millenaire.village.VillageSavedData;

public final class ResetVillagersCommand {
    private ResetVillagersCommand() {
    }

    public static void registerUnder(LiteralArgumentBuilder<CommandSourceStack> parent) {
        parent.then(Commands.literal((String)"resetvillagers").executes(ResetVillagersCommand::execute));
    }

    private static int execute(CommandContext<CommandSourceStack> ctx) {
        CommandSourceStack src = (CommandSourceStack)ctx.getSource();
        ServerLevel level = src.getLevel();
        ServerPlayer player = src.getPlayer();
        BlockPos searchPos = player != null ? player.blockPosition() : BlockPos.containing((Position)src.getPosition());
        Village village = VillageSavedData.get(level).getVillageManager().findNearestVillage(searchPos, 5000.0);
        if (village == null) {
            src.sendFailure((Component)Component.translatable((String)"command.millenaire.error.no_village_radius", (Object[])new Object[]{5000}));
            return 0;
        }
        int respawned = VillageIntegrityChecker.forceRespawnMissing(level, village);
        village.markDirty();
        String name = village.getVillageName();
        src.sendSuccess(() -> Component.translatable((String)"command.millenaire.resetvillagers.success", (Object[])new Object[]{name, respawned}), true);
        return respawned;
    }
}

