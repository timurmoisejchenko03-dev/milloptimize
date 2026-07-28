/*
 * Decompiled with CFR 0.150.
 * 
 * Could not load the following classes:
 *  com.mojang.authlib.GameProfile
 *  com.mojang.brigadier.arguments.ArgumentType
 *  com.mojang.brigadier.arguments.StringArgumentType
 *  com.mojang.brigadier.builder.LiteralArgumentBuilder
 *  com.mojang.brigadier.builder.RequiredArgumentBuilder
 *  com.mojang.brigadier.context.CommandContext
 *  com.mojang.brigadier.suggestion.SuggestionProvider
 *  com.mojang.brigadier.suggestion.SuggestionsBuilder
 *  net.minecraft.commands.CommandSourceStack
 *  net.minecraft.commands.Commands
 *  net.minecraft.commands.SharedSuggestionProvider
 *  net.minecraft.network.chat.Component
 *  net.minecraft.server.level.ServerLevel
 *  net.minecraft.world.level.Level
 */
package org.millenaire.command;

import com.mojang.authlib.GameProfile;
import com.mojang.brigadier.arguments.ArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.builder.RequiredArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.suggestion.SuggestionProvider;
import com.mojang.brigadier.suggestion.SuggestionsBuilder;
import java.util.List;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import org.millenaire.village.Village;
import org.millenaire.village.VillageSavedData;

public final class SwitchVillageControlCommand {
    private static final SuggestionProvider<CommandSourceStack> VILLAGE_NAME_SUGGESTIONS = (ctx, builder) -> {
        ServerLevel patt0$temp = ((CommandSourceStack)ctx.getSource()).getServer().getLevel(Level.OVERWORLD);
        if (!(patt0$temp instanceof ServerLevel)) {
            return builder.buildFuture();
        }
        ServerLevel level = patt0$temp;
        List<String> names = VillageSavedData.get(level).getVillageManager().getAllVillages().stream().map(Village::getVillageName).filter(n -> n != null).toList();
        return SharedSuggestionProvider.suggest(names, (SuggestionsBuilder)builder);
    };

    private SwitchVillageControlCommand() {
    }

    public static void registerUnder(LiteralArgumentBuilder<CommandSourceStack> parent) {
        parent.then(Commands.literal((String)"switchcontrol").then(((RequiredArgumentBuilder)Commands.argument((String)"villageName", (ArgumentType)StringArgumentType.string()).suggests(VILLAGE_NAME_SUGGESTIONS).then(Commands.argument((String)"playerName", (ArgumentType)StringArgumentType.string()).executes(SwitchVillageControlCommand::switchControl))).then(Commands.literal((String)"--clear").executes(SwitchVillageControlCommand::clearControl))));
    }

    private static int switchControl(CommandContext<CommandSourceStack> ctx) {
        GameProfile profile;
        CommandSourceStack source = (CommandSourceStack)ctx.getSource();
        String villageName = StringArgumentType.getString(ctx, (String)"villageName");
        String playerName = StringArgumentType.getString(ctx, (String)"playerName");
        ServerLevel overworld = source.getServer().getLevel(Level.OVERWORLD);
        if (overworld == null) {
            source.sendFailure((Component)Component.literal((String)"No Overworld available."));
            return 0;
        }
        Village village = SwitchVillageControlCommand.findVillageByName(overworld, villageName);
        if (village == null) {
            source.sendFailure((Component)Component.literal((String)("Village not found: " + villageName)));
            return 0;
        }
        GameProfile gameProfile = profile = source.getServer().getProfileCache() != null ? (GameProfile)source.getServer().getProfileCache().get(playerName).orElse(null) : null;
        if (profile == null || profile.getId() == null) {
            source.sendFailure((Component)Component.literal((String)("Player not found: " + playerName + " (must have joined the server at least once)")));
            return 0;
        }
        village.setOwner(profile.getId(), profile.getName());
        VillageSavedData.get(overworld).setDirty();
        source.sendSuccess(() -> Component.literal((String)("Village '" + village.getVillageName() + "' ownership transferred to " + profile.getName())), true);
        return 1;
    }

    private static int clearControl(CommandContext<CommandSourceStack> ctx) {
        CommandSourceStack source = (CommandSourceStack)ctx.getSource();
        String villageName = StringArgumentType.getString(ctx, (String)"villageName");
        ServerLevel overworld = source.getServer().getLevel(Level.OVERWORLD);
        if (overworld == null) {
            source.sendFailure((Component)Component.literal((String)"No Overworld available."));
            return 0;
        }
        Village village = SwitchVillageControlCommand.findVillageByName(overworld, villageName);
        if (village == null) {
            source.sendFailure((Component)Component.literal((String)("Village not found: " + villageName)));
            return 0;
        }
        String previous = village.getOwnerName();
        village.setOwner(null, null);
        VillageSavedData.get(overworld).setDirty();
        String prevStr = previous != null ? previous : "(none)";
        source.sendSuccess(() -> Component.literal((String)("Village '" + village.getVillageName() + "' owner cleared (was: " + prevStr + ")")), true);
        return 1;
    }

    private static Village findVillageByName(ServerLevel level, String name) {
        return VillageSavedData.get(level).getVillageManager().getAllVillages().stream().filter(v -> name.equals(v.getVillageName())).findFirst().orElse(null);
    }
}

