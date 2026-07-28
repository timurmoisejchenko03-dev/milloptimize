/*
 * Decompiled with CFR 0.152.
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
 *  net.minecraft.commands.arguments.ResourceLocationArgument
 *  net.minecraft.network.chat.Component
 *  net.minecraft.resources.ResourceLocation
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
import net.minecraft.commands.arguments.ResourceLocationArgument;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import org.millenaire.village.PlayerCultureReputation;

public final class GrantCultureControlCommand {
    private static final SuggestionProvider<CommandSourceStack> CULTURE_SUGGESTIONS = (ctx, builder) -> SharedSuggestionProvider.suggest(List.of("millenaire:norman", "millenaire:indian", "millenaire:mayan", "millenaire:byzantines", "millenaire:japanese", "millenaire:seljuk", "millenaire:inuits"), (SuggestionsBuilder)builder);

    private GrantCultureControlCommand() {
    }

    public static void registerUnder(LiteralArgumentBuilder<CommandSourceStack> parent) {
        parent.then(Commands.literal((String)"grantcontrol").then(Commands.argument((String)"culture", (ArgumentType)ResourceLocationArgument.id()).suggests(CULTURE_SUGGESTIONS).then(((RequiredArgumentBuilder)Commands.argument((String)"playerName", (ArgumentType)StringArgumentType.string()).executes(GrantCultureControlCommand::grantControl)).then(Commands.literal((String)"--revoke").executes(GrantCultureControlCommand::revokeControl)))));
    }

    private static int grantControl(CommandContext<CommandSourceStack> ctx) {
        GameProfile profile;
        CommandSourceStack source = (CommandSourceStack)ctx.getSource();
        ResourceLocation cultureId = ResourceLocationArgument.getId(ctx, (String)"culture");
        String playerName = StringArgumentType.getString(ctx, (String)"playerName");
        ServerLevel overworld = source.getServer().getLevel(Level.OVERWORLD);
        if (overworld == null) {
            source.sendFailure((Component)Component.literal((String)"No Overworld available."));
            return 0;
        }
        GameProfile gameProfile = profile = source.getServer().getProfileCache() != null ? (GameProfile)source.getServer().getProfileCache().get(playerName).orElse(null) : null;
        if (profile == null || profile.getId() == null) {
            source.sendFailure((Component)Component.literal((String)("Player not found: " + playerName + " (must have joined the server at least once)")));
            return 0;
        }
        PlayerCultureReputation.get(overworld).grantCultureControl(profile.getId(), cultureId);
        source.sendSuccess(() -> Component.literal((String)("Granted " + String.valueOf(cultureId) + " control to " + profile.getName())), true);
        return 1;
    }

    private static int revokeControl(CommandContext<CommandSourceStack> ctx) {
        GameProfile profile;
        CommandSourceStack source = (CommandSourceStack)ctx.getSource();
        ResourceLocation cultureId = ResourceLocationArgument.getId(ctx, (String)"culture");
        String playerName = StringArgumentType.getString(ctx, (String)"playerName");
        ServerLevel overworld = source.getServer().getLevel(Level.OVERWORLD);
        if (overworld == null) {
            source.sendFailure((Component)Component.literal((String)"No Overworld available."));
            return 0;
        }
        GameProfile gameProfile = profile = source.getServer().getProfileCache() != null ? (GameProfile)source.getServer().getProfileCache().get(playerName).orElse(null) : null;
        if (profile == null || profile.getId() == null) {
            source.sendFailure((Component)Component.literal((String)("Player not found: " + playerName + " (must have joined the server at least once)")));
            return 0;
        }
        PlayerCultureReputation.get(overworld).revokeCultureControl(profile.getId(), cultureId);
        source.sendSuccess(() -> Component.literal((String)("Revoked " + String.valueOf(cultureId) + " control from " + profile.getName())), true);
        return 1;
    }
}

