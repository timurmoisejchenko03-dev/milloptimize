/*
 * Decompiled with CFR 0.150.
 * 
 * Could not load the following classes:
 *  com.mojang.brigadier.arguments.ArgumentType
 *  com.mojang.brigadier.builder.LiteralArgumentBuilder
 *  com.mojang.brigadier.context.CommandContext
 *  com.mojang.brigadier.exceptions.CommandSyntaxException
 *  net.minecraft.commands.CommandSourceStack
 *  net.minecraft.commands.Commands
 *  net.minecraft.commands.arguments.coordinates.BlockPosArgument
 *  net.minecraft.core.BlockPos
 *  net.minecraft.network.chat.Component
 *  net.minecraft.server.level.ServerLevel
 *  net.minecraft.world.level.Level
 */
package org.millenaire.command;

import com.mojang.brigadier.arguments.ArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.coordinates.BlockPosArgument;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import org.millenaire.combat.raid.RaidManager;
import org.millenaire.culture.ModCultures;
import org.millenaire.culture.VillagerType;
import org.millenaire.village.Village;
import org.millenaire.village.VillageManager;
import org.millenaire.village.VillageSavedData;
import org.millenaire.village.VillagerRecord;

public final class RaidCommand {
    private static final int SEARCH_RADIUS = 256;

    private RaidCommand() {
    }

    public static void registerUnder(LiteralArgumentBuilder<CommandSourceStack> parent) {
        parent.then(Commands.literal((String)"raid").then(Commands.literal((String)"trigger").then(Commands.argument((String)"attacker_pos", (ArgumentType)BlockPosArgument.blockPos()).then(Commands.argument((String)"target_pos", (ArgumentType)BlockPosArgument.blockPos()).executes(RaidCommand::executeTrigger)))));
    }

    static int executeTrigger(CommandContext<CommandSourceStack> ctx) throws CommandSyntaxException {
        CommandSourceStack source = (CommandSourceStack)ctx.getSource();
        ServerLevel level = source.getLevel();
        if (level.dimension() != Level.OVERWORLD) {
            source.sendFailure((Component)Component.literal((String)"Raid trigger only works in the overworld."));
            return 0;
        }
        BlockPos attackerPos = BlockPosArgument.getLoadedBlockPos(ctx, (String)"attacker_pos");
        BlockPos targetPos = BlockPosArgument.getLoadedBlockPos(ctx, (String)"target_pos");
        VillageSavedData savedData = VillageSavedData.get(level);
        VillageManager villageManager = savedData.getVillageManager();
        Village attacker = villageManager.findNearestVillage(attackerPos, 256.0);
        if (attacker == null) {
            source.sendFailure((Component)Component.literal((String)("No village found within 256 blocks of attacker pos " + attackerPos.toShortString() + ".")));
            return 0;
        }
        Village target = villageManager.findNearestVillage(targetPos, 256.0);
        if (target == null) {
            source.sendFailure((Component)Component.literal((String)("No village found within 256 blocks of target pos " + targetPos.toShortString() + ".")));
            return 0;
        }
        if (attacker.getId().equals(target.getId())) {
            source.sendFailure((Component)Component.literal((String)("Attacker and target resolved to the same village (" + RaidCommand.nameOf(attacker) + ").")));
            return 0;
        }
        int raiderCount = RaidCommand.countAvailableRaiders(attacker);
        if (raiderCount == 0) {
            source.sendFailure((Component)Component.literal((String)("No raiders in attacker village (" + RaidCommand.nameOf(attacker) + ").")));
            return 0;
        }
        RaidManager.planRaid(attacker, target, level);
        RaidManager.startRaidForced(attacker, target, level);
        String msg = "Raid triggered: " + RaidCommand.nameOf(attacker) + " -> " + RaidCommand.nameOf(target) + " (" + raiderCount + " raiders dispatched)";
        source.sendSuccess(() -> Component.literal((String)msg), true);
        return 1;
    }

    private static int countAvailableRaiders(Village attacker) {
        int count = 0;
        for (VillagerRecord r : attacker.getVillagerRecords().values()) {
            VillagerType type;
            if (r.isKilled() || r.isRaidingVillage() || r.isAwayRaiding() || (type = ModCultures.getVillagerType(r.getVillagerTypeId())) == null || !type.isRaider()) continue;
            ++count;
        }
        return count;
    }

    private static String nameOf(Village v) {
        String name = v.getVillageName();
        if (name != null && !name.isBlank()) {
            return name;
        }
        return v.getVillageTypeId().getPath();
    }
}

