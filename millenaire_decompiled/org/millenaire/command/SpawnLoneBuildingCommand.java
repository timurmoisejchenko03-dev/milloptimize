/*
 * Decompiled with CFR 0.150.
 * 
 * Could not load the following classes:
 *  com.mojang.brigadier.CommandDispatcher
 *  com.mojang.brigadier.arguments.ArgumentType
 *  com.mojang.brigadier.arguments.IntegerArgumentType
 *  com.mojang.brigadier.arguments.StringArgumentType
 *  com.mojang.brigadier.builder.LiteralArgumentBuilder
 *  com.mojang.brigadier.builder.RequiredArgumentBuilder
 *  com.mojang.brigadier.context.CommandContext
 *  com.mojang.brigadier.suggestion.SuggestionProvider
 *  net.minecraft.commands.CommandSourceStack
 *  net.minecraft.commands.Commands
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
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.builder.RequiredArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.suggestion.SuggestionProvider;
import java.util.List;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.Level;
import org.millenaire.culture.ModCultures;
import org.millenaire.culture.VillageType;
import org.millenaire.village.VillageManager;
import org.millenaire.village.VillageSavedData;
import org.millenaire.world.VillageSpawner;

public final class SpawnLoneBuildingCommand {
    private static final SuggestionProvider<CommandSourceStack> LONE_BUILDING_SUGGESTIONS = (ctx, builder) -> {
        String remaining = builder.getRemainingLowerCase();
        ModCultures.getAllVillageTypes().entrySet().stream().filter(e -> ((VillageType)e.getValue()).loneBuilding()).forEach(e -> {
            String id = ((ResourceLocation)e.getKey()).getPath().replace('/', '_');
            if (id.toLowerCase().startsWith(remaining)) {
                builder.suggest(id, () -> ((VillageType)e.getValue()).name());
            }
        });
        return builder.buildFuture();
    };

    private SpawnLoneBuildingCommand() {
    }

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register((LiteralArgumentBuilder)Commands.literal((String)"millenaire").then(((LiteralArgumentBuilder)((LiteralArgumentBuilder)Commands.literal((String)"spawn_lb").requires(source -> source.hasPermission(2))).then(((RequiredArgumentBuilder)Commands.argument((String)"type", (ArgumentType)StringArgumentType.string()).suggests(LONE_BUILDING_SUGGESTIONS).executes(ctx -> SpawnLoneBuildingCommand.execute((CommandContext<CommandSourceStack>)ctx, StringArgumentType.getString((CommandContext)ctx, (String)"type"), 0))).then(Commands.argument((String)"completion", (ArgumentType)IntegerArgumentType.integer((int)0, (int)100)).executes(ctx -> SpawnLoneBuildingCommand.execute((CommandContext<CommandSourceStack>)ctx, StringArgumentType.getString((CommandContext)ctx, (String)"type"), IntegerArgumentType.getInteger((CommandContext)ctx, (String)"completion")))))).then(Commands.literal((String)"at").then(Commands.argument((String)"x", (ArgumentType)IntegerArgumentType.integer()).then(Commands.argument((String)"y", (ArgumentType)IntegerArgumentType.integer()).then(Commands.argument((String)"z", (ArgumentType)IntegerArgumentType.integer()).then(((RequiredArgumentBuilder)Commands.argument((String)"type", (ArgumentType)StringArgumentType.string()).suggests(LONE_BUILDING_SUGGESTIONS).executes(ctx -> SpawnLoneBuildingCommand.executeAt((CommandContext<CommandSourceStack>)ctx, StringArgumentType.getString((CommandContext)ctx, (String)"type"), IntegerArgumentType.getInteger((CommandContext)ctx, (String)"x"), IntegerArgumentType.getInteger((CommandContext)ctx, (String)"y"), IntegerArgumentType.getInteger((CommandContext)ctx, (String)"z"), 0))).then(Commands.argument((String)"completion", (ArgumentType)IntegerArgumentType.integer((int)0, (int)100)).executes(ctx -> SpawnLoneBuildingCommand.executeAt((CommandContext<CommandSourceStack>)ctx, StringArgumentType.getString((CommandContext)ctx, (String)"type"), IntegerArgumentType.getInteger((CommandContext)ctx, (String)"x"), IntegerArgumentType.getInteger((CommandContext)ctx, (String)"y"), IntegerArgumentType.getInteger((CommandContext)ctx, (String)"z"), IntegerArgumentType.getInteger((CommandContext)ctx, (String)"completion")))))))))));
    }

    private static int execute(CommandContext<CommandSourceStack> ctx, String typeArg, int completion) {
        CommandSourceStack source = (CommandSourceStack)ctx.getSource();
        ServerPlayer player = source.getPlayer();
        if (player == null) {
            source.sendFailure((Component)Component.literal((String)"This command must be run by a player. Use: /millenaire spawn_lb at <x> <y> <z> <type>"));
            return 0;
        }
        return SpawnLoneBuildingCommand.doSpawn(source, player.serverLevel(), player.blockPosition(), typeArg, completion);
    }

    private static int executeAt(CommandContext<CommandSourceStack> ctx, String typeArg, int x, int y, int z, int completion) {
        CommandSourceStack source = (CommandSourceStack)ctx.getSource();
        return SpawnLoneBuildingCommand.doSpawn(source, source.getLevel(), new BlockPos(x, y, z), typeArg, completion);
    }

    private static int doSpawn(CommandSourceStack source, ServerLevel level, BlockPos center, String typeArg, int completion) {
        if (level.dimension() != Level.OVERWORLD) {
            source.sendFailure((Component)Component.literal((String)"Lone buildings can only be spawned in the Overworld."));
            return 0;
        }
        ResourceLocation resolvedId = SpawnLoneBuildingCommand.resolveType(typeArg);
        VillageType villageType = ModCultures.getVillageType(resolvedId);
        if (villageType == null) {
            List<String> shortNames = ModCultures.getAllVillageTypes().keySet().stream().filter(id -> ModCultures.getVillageType(id).loneBuilding()).map(ResourceLocation::getPath).toList();
            source.sendFailure((Component)Component.literal((String)("Unknown lone building type: " + typeArg + ". Available types: " + String.valueOf(shortNames))));
            return 0;
        }
        if (!villageType.loneBuilding()) {
            source.sendFailure((Component)Component.literal((String)(typeArg + " is a village, not a lone building. Use /millenaire spawn instead.")));
            return 0;
        }
        ResourceLocation villageTypeId = resolvedId;
        VillageSavedData savedData = VillageSavedData.get(level);
        VillageManager villageManager = savedData.getVillageManager();
        if (villageManager.isWithinMinDistance(center, 100.0)) {
            source.sendFailure((Component)Component.literal((String)"A village or lone building already exists within 100 blocks."));
            return 0;
        }
        Component failure = VillageSpawner.spawnVillage(level, center, villageType, completion);
        if (failure != null) {
            source.sendFailure(failure);
            return 0;
        }
        savedData.registerLoneBuilding(center, villageTypeId, villageType.culture().getPath(), null);
        Object completionStr = completion > 0 ? " (completion=" + completion + "%)" : "";
        source.sendSuccess(() -> SpawnLoneBuildingCommand.lambda$doSpawn$10(villageTypeId, center, (String)completionStr), true);
        return 1;
    }

    private static ResourceLocation resolveType(String arg) {
        String asContentId;
        ResourceLocation converted;
        if (arg.contains(":")) {
            return ResourceLocation.parse((String)arg);
        }
        ResourceLocation direct = ResourceLocation.fromNamespaceAndPath((String)"millenaire", (String)arg);
        if (ModCultures.getVillageType(direct) != null) {
            return direct;
        }
        int idx = arg.indexOf(95);
        if (idx > 0 && ModCultures.getVillageType(converted = ResourceLocation.fromNamespaceAndPath((String)"millenaire", (String)(asContentId = arg.substring(0, idx) + "/" + arg.substring(idx + 1)))) != null) {
            return converted;
        }
        return direct;
    }

    private static /* synthetic */ Component lambda$doSpawn$10(ResourceLocation villageTypeId, BlockPos center, String completionStr) {
        return Component.literal((String)("Lone building " + villageTypeId.getPath() + " spawned at " + center.toShortString() + completionStr));
    }
}

