/*
 * Decompiled with CFR 0.150.
 * 
 * Could not load the following classes:
 *  com.mojang.brigadier.CommandDispatcher
 *  com.mojang.brigadier.arguments.ArgumentType
 *  com.mojang.brigadier.arguments.IntegerArgumentType
 *  com.mojang.brigadier.builder.LiteralArgumentBuilder
 *  com.mojang.brigadier.context.CommandContext
 *  com.mojang.brigadier.exceptions.CommandSyntaxException
 *  net.minecraft.commands.CommandSourceStack
 *  net.minecraft.commands.Commands
 *  net.minecraft.commands.arguments.EntityArgument
 *  net.minecraft.core.BlockPos
 *  net.minecraft.network.chat.Component
 *  net.minecraft.resources.ResourceLocation
 *  net.minecraft.server.level.ServerLevel
 *  net.minecraft.server.level.ServerPlayer
 *  net.minecraft.world.level.Level
 */
package org.millenaire.command;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.ArgumentType;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import java.util.List;
import java.util.UUID;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.Level;
import org.millenaire.culture.ModCultures;
import org.millenaire.culture.ReputationLabel;
import org.millenaire.village.PlayerCultureReputation;
import org.millenaire.village.Village;
import org.millenaire.village.VillageManager;
import org.millenaire.village.VillageReputation;
import org.millenaire.village.VillageSavedData;

public final class ReputationCommand {
    private ReputationCommand() {
    }

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register((LiteralArgumentBuilder)Commands.literal((String)"millenaire").then(((LiteralArgumentBuilder)Commands.literal((String)"reputation").requires(source -> source.hasPermission(2))).then(Commands.argument((String)"player", (ArgumentType)EntityArgument.player()).then(Commands.argument((String)"amount", (ArgumentType)IntegerArgumentType.integer()).executes(ReputationCommand::execute)))));
    }

    private static int execute(CommandContext<CommandSourceStack> ctx) throws CommandSyntaxException {
        CommandSourceStack source = (CommandSourceStack)ctx.getSource();
        ServerLevel level = source.getLevel();
        if (level.dimension() != Level.OVERWORLD) {
            source.sendFailure((Component)Component.translatable((String)"command.millenaire.error.no_overworld_villages"));
            return 0;
        }
        ServerPlayer targetPlayer = EntityArgument.getPlayer(ctx, (String)"player");
        int amount = IntegerArgumentType.getInteger(ctx, (String)"amount");
        ServerPlayer executor = source.getPlayer();
        BlockPos searchPos = executor != null ? executor.blockPosition() : targetPlayer.blockPosition();
        VillageSavedData savedData = VillageSavedData.get(level);
        VillageManager villageManager = savedData.getVillageManager();
        Village village = villageManager.findNearestVillage(searchPos, 500.0);
        if (village == null) {
            source.sendFailure((Component)Component.translatable((String)"command.millenaire.error.no_village_radius", (Object[])new Object[]{500}));
            return 0;
        }
        UUID playerId = targetPlayer.getUUID();
        ResourceLocation cultureId = village.getCultureId();
        int newVillageRep = village.adjustReputation(level, playerId, amount);
        savedData.setDirty();
        int cultureRep = PlayerCultureReputation.get(level).get(playerId, cultureId);
        int effective = newVillageRep + cultureRep;
        List<ReputationLabel> labels = ModCultures.getReputationLabels(cultureId);
        String labelStr = VillageReputation.getLabel(effective, labels);
        String labelDisplay = labelStr != null ? labelStr : "?";
        source.sendSuccess(() -> Component.translatable((String)"command.millenaire.reputation.line", (Object[])new Object[]{targetPlayer.getName().getString(), newVillageRep, cultureRep, effective, labelDisplay, amount}), true);
        return 1;
    }
}

