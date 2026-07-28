/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.mojang.brigadier.CommandDispatcher
 *  com.mojang.brigadier.builder.ArgumentBuilder
 *  com.mojang.brigadier.builder.LiteralArgumentBuilder
 *  net.minecraft.commands.CommandSourceStack
 *  net.minecraft.commands.Commands
 */
package org.millenaire.command;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.builder.ArgumentBuilder;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import org.millenaire.command.ConvertAddonCommand;
import org.millenaire.command.DebugCommand;
import org.millenaire.command.ExportCommand;
import org.millenaire.command.GrantCultureControlCommand;
import org.millenaire.command.GrowCommand;
import org.millenaire.command.HireDevCommand;
import org.millenaire.command.ImportCultureCommand;
import org.millenaire.command.ImportTableDevCommand;
import org.millenaire.command.InventoryCommand;
import org.millenaire.command.MilitaryCommand;
import org.millenaire.command.NavDiagCommand;
import org.millenaire.command.PathAuditCommand;
import org.millenaire.command.PathsCommand;
import org.millenaire.command.QuestCommand;
import org.millenaire.command.RaidCommand;
import org.millenaire.command.RenameVillageCommand;
import org.millenaire.command.ResetVillagersCommand;
import org.millenaire.command.RoundtripCommand;
import org.millenaire.command.SpawnBuildingCommand;
import org.millenaire.command.SpawnInfoCommand;
import org.millenaire.command.StatusCommand;
import org.millenaire.command.SwitchVillageControlCommand;
import org.millenaire.command.WaypointDiagCommand;
import org.millenaire.command.WaypointGraphCommand;

public final class DevCommand {
    private DevCommand() {
    }

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        LiteralArgumentBuilder dev = (LiteralArgumentBuilder)Commands.literal((String)"dev").requires(source -> source.hasPermission(2));
        DebugCommand.registerUnder((LiteralArgumentBuilder<CommandSourceStack>)dev);
        StatusCommand.registerUnder((LiteralArgumentBuilder<CommandSourceStack>)dev);
        MilitaryCommand.registerUnder((LiteralArgumentBuilder<CommandSourceStack>)dev);
        RaidCommand.registerUnder((LiteralArgumentBuilder<CommandSourceStack>)dev);
        InventoryCommand.registerUnder((LiteralArgumentBuilder<CommandSourceStack>)dev);
        SpawnBuildingCommand.registerUnder((LiteralArgumentBuilder<CommandSourceStack>)dev);
        RoundtripCommand.registerUnder((LiteralArgumentBuilder<CommandSourceStack>)dev);
        ExportCommand.registerUnder((LiteralArgumentBuilder<CommandSourceStack>)dev);
        QuestCommand.registerUnder((LiteralArgumentBuilder<CommandSourceStack>)dev);
        NavDiagCommand.registerUnder((LiteralArgumentBuilder<CommandSourceStack>)dev);
        WaypointDiagCommand.registerUnder((LiteralArgumentBuilder<CommandSourceStack>)dev);
        WaypointGraphCommand.registerUnder((LiteralArgumentBuilder<CommandSourceStack>)dev);
        SpawnInfoCommand.registerUnder((LiteralArgumentBuilder<CommandSourceStack>)dev);
        PathAuditCommand.registerUnder((LiteralArgumentBuilder<CommandSourceStack>)dev);
        PathsCommand.registerUnder((LiteralArgumentBuilder<CommandSourceStack>)dev);
        GrowCommand.registerUnder((LiteralArgumentBuilder<CommandSourceStack>)dev);
        RenameVillageCommand.registerUnder((LiteralArgumentBuilder<CommandSourceStack>)dev);
        ResetVillagersCommand.registerUnder((LiteralArgumentBuilder<CommandSourceStack>)dev);
        ConvertAddonCommand.registerUnder((LiteralArgumentBuilder<CommandSourceStack>)dev);
        ImportCultureCommand.registerUnder((LiteralArgumentBuilder<CommandSourceStack>)dev);
        ImportTableDevCommand.registerUnder((LiteralArgumentBuilder<CommandSourceStack>)dev);
        SwitchVillageControlCommand.registerUnder((LiteralArgumentBuilder<CommandSourceStack>)dev);
        GrantCultureControlCommand.registerUnder((LiteralArgumentBuilder<CommandSourceStack>)dev);
        HireDevCommand.registerUnder((LiteralArgumentBuilder<CommandSourceStack>)dev);
        dispatcher.register((LiteralArgumentBuilder)Commands.literal((String)"millenaire").then((ArgumentBuilder)dev));
    }
}

