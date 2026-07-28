/*
 * Decompiled with CFR 0.150.
 * 
 * Could not load the following classes:
 *  com.mojang.brigadier.arguments.ArgumentType
 *  com.mojang.brigadier.arguments.BoolArgumentType
 *  com.mojang.brigadier.arguments.IntegerArgumentType
 *  com.mojang.brigadier.arguments.StringArgumentType
 *  com.mojang.brigadier.builder.ArgumentBuilder
 *  com.mojang.brigadier.builder.LiteralArgumentBuilder
 *  com.mojang.brigadier.builder.RequiredArgumentBuilder
 *  com.mojang.brigadier.context.CommandContext
 *  com.mojang.brigadier.suggestion.SuggestionProvider
 *  com.mojang.brigadier.suggestion.SuggestionsBuilder
 *  net.minecraft.commands.CommandSourceStack
 *  net.minecraft.commands.Commands
 *  net.minecraft.commands.SharedSuggestionProvider
 *  net.minecraft.core.BlockPos
 *  net.minecraft.network.chat.Component
 *  net.minecraft.resources.ResourceLocation
 *  net.minecraft.server.level.ServerLevel
 *  net.minecraft.server.level.ServerPlayer
 *  net.minecraft.world.level.block.entity.BlockEntity
 *  net.neoforged.neoforge.common.util.FakePlayerFactory
 */
package org.millenaire.command;

import com.mojang.brigadier.arguments.ArgumentType;
import com.mojang.brigadier.arguments.BoolArgumentType;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.ArgumentBuilder;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.builder.RequiredArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.suggestion.SuggestionProvider;
import com.mojang.brigadier.suggestion.SuggestionsBuilder;
import java.util.function.Function;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.neoforged.neoforge.common.util.FakePlayerFactory;
import org.millenaire.block.ImportTableBlock;
import org.millenaire.block.ImportTableBlockEntity;
import org.millenaire.block.ModBlocks;
import org.millenaire.building.BuildingExporter;
import org.millenaire.building.BuildingImporter;
import org.millenaire.building.BuildingPlanSet;
import org.millenaire.culture.ModCultures;

public final class ImportTableDevCommand {
    private static final SuggestionProvider<CommandSourceStack> CULTURE_KEY_SUGGESTIONS = (ctx, builder) -> SharedSuggestionProvider.suggest(ModCultures.getAllCultures().keySet().stream().map(Object::toString), (SuggestionsBuilder)builder);

    private ImportTableDevCommand() {
    }

    public static void registerUnder(LiteralArgumentBuilder<CommandSourceStack> parent) {
        parent.then(((LiteralArgumentBuilder)((LiteralArgumentBuilder)((LiteralArgumentBuilder)((LiteralArgumentBuilder)((LiteralArgumentBuilder)((LiteralArgumentBuilder)((LiteralArgumentBuilder)Commands.literal((String)"import-table").then(Commands.literal((String)"place").then(ImportTableDevCommand.pos3d(z -> (RequiredArgumentBuilder)z.executes(ImportTableDevCommand::executePlace))))).then(Commands.literal((String)"info").then(ImportTableDevCommand.pos3d(z -> (RequiredArgumentBuilder)z.executes(ImportTableDevCommand::executeInfo))))).then(Commands.literal((String)"load").then(ImportTableDevCommand.pos3d(z -> (RequiredArgumentBuilder)z.then(Commands.argument((String)"cultureKey", (ArgumentType)StringArgumentType.string()).suggests(CULTURE_KEY_SUGGESTIONS).then(((RequiredArgumentBuilder)Commands.argument((String)"buildingId", (ArgumentType)StringArgumentType.string()).executes(ctx -> ImportTableDevCommand.executeLoad((CommandContext<CommandSourceStack>)ctx, "a", 0))).then(((RequiredArgumentBuilder)Commands.argument((String)"variant", (ArgumentType)StringArgumentType.string()).executes(ctx -> ImportTableDevCommand.executeLoad((CommandContext<CommandSourceStack>)ctx, StringArgumentType.getString((CommandContext)ctx, (String)"variant"), 0))).then(Commands.argument((String)"level", (ArgumentType)IntegerArgumentType.integer((int)0)).executes(ctx -> ImportTableDevCommand.executeLoad((CommandContext<CommandSourceStack>)ctx, StringArgumentType.getString((CommandContext)ctx, (String)"variant"), IntegerArgumentType.getInteger((CommandContext)ctx, (String)"level"))))))))))).then(Commands.literal((String)"load-all").then(ImportTableDevCommand.pos3d(z -> (RequiredArgumentBuilder)z.then(Commands.argument((String)"cultureKey", (ArgumentType)StringArgumentType.string()).suggests(CULTURE_KEY_SUGGESTIONS).then(((RequiredArgumentBuilder)Commands.argument((String)"buildingId", (ArgumentType)StringArgumentType.string()).executes(ctx -> ImportTableDevCommand.executeLoadAll((CommandContext<CommandSourceStack>)ctx, "a"))).then(Commands.argument((String)"variant", (ArgumentType)StringArgumentType.string()).executes(ctx -> ImportTableDevCommand.executeLoadAll((CommandContext<CommandSourceStack>)ctx, StringArgumentType.getString((CommandContext)ctx, (String)"variant")))))))))).then(Commands.literal((String)"export").then(ImportTableDevCommand.pos3d(z -> (RequiredArgumentBuilder)z.executes(ImportTableDevCommand::executeExport))))).then(Commands.literal((String)"export-new-level").then(ImportTableDevCommand.pos3d(z -> (RequiredArgumentBuilder)z.executes(ImportTableDevCommand::executeExportNewLevel))))).then(Commands.literal((String)"create").then(ImportTableDevCommand.pos3d(z -> (RequiredArgumentBuilder)z.then(ImportTableDevCommand.createTail()))))).then(Commands.literal((String)"create-sub").then(ImportTableDevCommand.pos3d(z -> (RequiredArgumentBuilder)z.then(Commands.argument((String)"cultureKey", (ArgumentType)StringArgumentType.string()).suggests(CULTURE_KEY_SUGGESTIONS).then(Commands.argument((String)"parentId", (ArgumentType)StringArgumentType.string()).then(Commands.argument((String)"parentVariant", (ArgumentType)StringArgumentType.string()).then(Commands.argument((String)"triggerLevel", (ArgumentType)IntegerArgumentType.integer((int)0)).then(Commands.argument((String)"subName", (ArgumentType)StringArgumentType.string()).executes(ImportTableDevCommand::executeCreateSub))))))))));
    }

    private static RequiredArgumentBuilder<CommandSourceStack, Integer> createTail() {
        return (RequiredArgumentBuilder)Commands.argument((String)"length", (ArgumentType)IntegerArgumentType.integer((int)1, (int)256)).then(Commands.argument((String)"width", (ArgumentType)IntegerArgumentType.integer((int)1, (int)256)).then(Commands.argument((String)"startingLevel", (ArgumentType)IntegerArgumentType.integer((int)-64, (int)320)).then(((RequiredArgumentBuilder)Commands.argument((String)"height", (ArgumentType)IntegerArgumentType.integer((int)1, (int)256)).executes(ctx -> ImportTableDevCommand.executeCreate((CommandContext<CommandSourceStack>)ctx, false, 0, ""))).then(((RequiredArgumentBuilder)Commands.argument((String)"clearGround", (ArgumentType)BoolArgumentType.bool()).executes(ctx -> ImportTableDevCommand.executeCreate((CommandContext<CommandSourceStack>)ctx, BoolArgumentType.getBool((CommandContext)ctx, (String)"clearGround"), 0, ""))).then(((RequiredArgumentBuilder)Commands.argument((String)"upgrades", (ArgumentType)IntegerArgumentType.integer((int)0, (int)16)).executes(ctx -> ImportTableDevCommand.executeCreate((CommandContext<CommandSourceStack>)ctx, BoolArgumentType.getBool((CommandContext)ctx, (String)"clearGround"), IntegerArgumentType.getInteger((CommandContext)ctx, (String)"upgrades"), ""))).then(Commands.argument((String)"name", (ArgumentType)StringArgumentType.string()).executes(ctx -> ImportTableDevCommand.executeCreate((CommandContext<CommandSourceStack>)ctx, BoolArgumentType.getBool((CommandContext)ctx, (String)"clearGround"), IntegerArgumentType.getInteger((CommandContext)ctx, (String)"upgrades"), StringArgumentType.getString((CommandContext)ctx, (String)"name")))))))));
    }

    private static int executePlace(CommandContext<CommandSourceStack> ctx) {
        BlockPos pos = ImportTableDevCommand.readPos(ctx);
        ServerLevel level = ((CommandSourceStack)ctx.getSource()).getLevel();
        level.setBlock(pos, ((ImportTableBlock)((Object)ModBlocks.IMPORT_TABLE.get())).defaultBlockState(), 3);
        BlockEntity be = level.getBlockEntity(pos);
        if (!(be instanceof ImportTableBlockEntity)) {
            ((CommandSourceStack)ctx.getSource()).sendFailure((Component)Component.translatable((String)"command.millenaire.importtable.place_failed", (Object[])new Object[]{pos.toShortString()}));
            return 0;
        }
        ((CommandSourceStack)ctx.getSource()).sendSuccess(() -> Component.translatable((String)"command.millenaire.importtable.placed", (Object[])new Object[]{pos.toShortString()}), false);
        return 1;
    }

    private static int executeInfo(CommandContext<CommandSourceStack> ctx) {
        ImportTableBlockEntity be = ImportTableDevCommand.requireImportTable(ctx);
        if (be == null) {
            return 0;
        }
        String culture = be.getCultureKey().isEmpty() ? "<none>" : be.getCultureKey();
        String building = be.getBuildingId().isEmpty() ? "<none>" : be.getBuildingId();
        String variant = be.getVariant().isEmpty() ? "<none>" : be.getVariant();
        ((CommandSourceStack)ctx.getSource()).sendSuccess(() -> Component.translatable((String)"command.millenaire.importtable.info", (Object[])new Object[]{be.getBlockPos().toShortString(), culture, building, variant, be.getUpgradeLevel(), be.hasPlan(), be.getLength(), be.getWidth(), be.getHeight(), be.getStartingLevel(), be.getOrientation()}), false);
        return 1;
    }

    private static int executeLoad(CommandContext<CommandSourceStack> ctx, String variant, int level) {
        ImportTableBlockEntity be = ImportTableDevCommand.requireImportTable(ctx);
        if (be == null) {
            return 0;
        }
        String cultureKey = StringArgumentType.getString(ctx, (String)"cultureKey");
        String buildingId = StringArgumentType.getString(ctx, (String)"buildingId");
        ServerLevel serverLevel = ((CommandSourceStack)ctx.getSource()).getLevel();
        ServerPlayer player = ImportTableDevCommand.resolvePlayer(ctx);
        BuildingImporter.importLevelFromCulture(serverLevel, be, player, cultureKey, buildingId, variant, level, "", "");
        if (!buildingId.equals(be.getBuildingId()) || !cultureKey.equals(be.getCultureKey())) {
            ((CommandSourceStack)ctx.getSource()).sendFailure((Component)Component.translatable((String)"command.millenaire.importtable.load_failed", (Object[])new Object[]{cultureKey, buildingId, variant, level}));
            return 0;
        }
        ((CommandSourceStack)ctx.getSource()).sendSuccess(() -> Component.translatable((String)"command.millenaire.importtable.loaded", (Object[])new Object[]{cultureKey, buildingId, variant, level}), false);
        return 1;
    }

    private static int executeLoadAll(CommandContext<CommandSourceStack> ctx, String variant) {
        ImportTableBlockEntity be = ImportTableDevCommand.requireImportTable(ctx);
        if (be == null) {
            return 0;
        }
        String cultureKey = StringArgumentType.getString(ctx, (String)"cultureKey");
        String buildingId = StringArgumentType.getString(ctx, (String)"buildingId");
        ServerLevel serverLevel = ((CommandSourceStack)ctx.getSource()).getLevel();
        ServerPlayer player = ImportTableDevCommand.resolvePlayer(ctx);
        BuildingImporter.importAllFromCulture(serverLevel, be, player, cultureKey, buildingId, variant);
        if (!buildingId.equals(be.getBuildingId()) || !cultureKey.equals(be.getCultureKey())) {
            ((CommandSourceStack)ctx.getSource()).sendFailure((Component)Component.translatable((String)"command.millenaire.importtable.load_all_failed", (Object[])new Object[]{cultureKey, buildingId, variant}));
            return 0;
        }
        BuildingPlanSet planSet = ModCultures.getBuildingPlanSet(ResourceLocation.tryParse((String)(cultureKey + "/" + buildingId)));
        int count = planSet != null ? planSet.getLevelCount(variant) : -1;
        ((CommandSourceStack)ctx.getSource()).sendSuccess(() -> Component.translatable((String)"command.millenaire.importtable.loaded_all", (Object[])new Object[]{count, cultureKey, buildingId, variant}), false);
        return 1;
    }

    private static int executeExport(CommandContext<CommandSourceStack> ctx) {
        ImportTableBlockEntity be = ImportTableDevCommand.requireImportTable(ctx);
        if (be == null) {
            return 0;
        }
        if (!be.hasPlan()) {
            ((CommandSourceStack)ctx.getSource()).sendFailure((Component)Component.translatable((String)"command.millenaire.importtable.export_no_plan"));
            return 0;
        }
        String buildingId = be.getBuildingId();
        String variant = be.getVariant();
        int level = be.getUpgradeLevel();
        ServerLevel serverLevel = ((CommandSourceStack)ctx.getSource()).getLevel();
        ServerPlayer player = ImportTableDevCommand.resolvePlayer(ctx);
        BuildingExporter.exportLevel(serverLevel, be, player);
        ((CommandSourceStack)ctx.getSource()).sendSuccess(() -> Component.translatable((String)"command.millenaire.importtable.export_triggered", (Object[])new Object[]{buildingId, variant, level}), false);
        return 1;
    }

    private static int executeExportNewLevel(CommandContext<CommandSourceStack> ctx) {
        ImportTableBlockEntity be = ImportTableDevCommand.requireImportTable(ctx);
        if (be == null) {
            return 0;
        }
        if (!be.hasPlan()) {
            ((CommandSourceStack)ctx.getSource()).sendFailure((Component)Component.translatable((String)"command.millenaire.importtable.export_new_level_no_plan"));
            return 0;
        }
        ServerLevel serverLevel = ((CommandSourceStack)ctx.getSource()).getLevel();
        ServerPlayer player = ImportTableDevCommand.resolvePlayer(ctx);
        BuildingExporter.exportNewLevel(serverLevel, be, player);
        ((CommandSourceStack)ctx.getSource()).sendSuccess(() -> Component.translatable((String)"command.millenaire.importtable.export_new_level_triggered"), false);
        return 1;
    }

    private static int executeCreate(CommandContext<CommandSourceStack> ctx, boolean clearGround, int numberOfUpgrades, String buildingName) {
        ImportTableBlockEntity be = ImportTableDevCommand.requireImportTable(ctx);
        if (be == null) {
            return 0;
        }
        int length = IntegerArgumentType.getInteger(ctx, (String)"length");
        int width = IntegerArgumentType.getInteger(ctx, (String)"width");
        int startingLevel = IntegerArgumentType.getInteger(ctx, (String)"startingLevel");
        int height = IntegerArgumentType.getInteger(ctx, (String)"height");
        ServerLevel serverLevel = ((CommandSourceStack)ctx.getSource()).getLevel();
        ServerPlayer player = ImportTableDevCommand.resolvePlayer(ctx);
        BuildingImporter.createNewBuilding(serverLevel, be, player, length, width, startingLevel, height, clearGround, numberOfUpgrades, false, buildingName);
        String buildingId = be.getBuildingId();
        ((CommandSourceStack)ctx.getSource()).sendSuccess(() -> Component.translatable((String)"command.millenaire.importtable.created", (Object[])new Object[]{buildingId, width, length}), false);
        return 1;
    }

    private static int executeCreateSub(CommandContext<CommandSourceStack> ctx) {
        ServerPlayer player;
        ImportTableBlockEntity be = ImportTableDevCommand.requireImportTable(ctx);
        if (be == null) {
            return 0;
        }
        String cultureKey = StringArgumentType.getString(ctx, (String)"cultureKey");
        String parentId = StringArgumentType.getString(ctx, (String)"parentId");
        String parentVariant = StringArgumentType.getString(ctx, (String)"parentVariant");
        int triggerLevel = IntegerArgumentType.getInteger(ctx, (String)"triggerLevel");
        String subName = StringArgumentType.getString(ctx, (String)"subName");
        ServerLevel serverLevel = ((CommandSourceStack)ctx.getSource()).getLevel();
        String subId = BuildingImporter.createSubBuilding(serverLevel, be, player = ImportTableDevCommand.resolvePlayer(ctx), cultureKey, parentId, parentVariant, triggerLevel, subName);
        if (subId == null) {
            ((CommandSourceStack)ctx.getSource()).sendFailure((Component)Component.translatable((String)"command.millenaire.importtable.create_sub_failed", (Object[])new Object[]{parentId, parentVariant, triggerLevel, subName}));
            return 0;
        }
        ((CommandSourceStack)ctx.getSource()).sendSuccess(() -> Component.translatable((String)"command.millenaire.importtable.created_sub", (Object[])new Object[]{subId, parentId, parentVariant, triggerLevel}), false);
        return 1;
    }

    private static RequiredArgumentBuilder<CommandSourceStack, Integer> pos3d(Function<RequiredArgumentBuilder<CommandSourceStack, Integer>, RequiredArgumentBuilder<CommandSourceStack, Integer>> zTail) {
        return (RequiredArgumentBuilder)Commands.argument((String)"x", (ArgumentType)IntegerArgumentType.integer()).then(Commands.argument((String)"y", (ArgumentType)IntegerArgumentType.integer()).then((ArgumentBuilder)zTail.apply((RequiredArgumentBuilder<CommandSourceStack, Integer>)Commands.argument((String)"z", (ArgumentType)IntegerArgumentType.integer()))));
    }

    private static BlockPos readPos(CommandContext<CommandSourceStack> ctx) {
        return new BlockPos(IntegerArgumentType.getInteger(ctx, (String)"x"), IntegerArgumentType.getInteger(ctx, (String)"y"), IntegerArgumentType.getInteger(ctx, (String)"z"));
    }

    private static ImportTableBlockEntity requireImportTable(CommandContext<CommandSourceStack> ctx) {
        BlockPos pos = ImportTableDevCommand.readPos(ctx);
        ServerLevel level = ((CommandSourceStack)ctx.getSource()).getLevel();
        level.getChunk(pos);
        BlockEntity be = level.getBlockEntity(pos);
        if (!(be instanceof ImportTableBlockEntity)) {
            ((CommandSourceStack)ctx.getSource()).sendFailure((Component)Component.translatable((String)"command.millenaire.importtable.no_table_at", (Object[])new Object[]{pos.toShortString(), pos.getX(), pos.getY(), pos.getZ()}));
            return null;
        }
        ImportTableBlockEntity table = (ImportTableBlockEntity)be;
        return table;
    }

    private static ServerPlayer resolvePlayer(CommandContext<CommandSourceStack> ctx) {
        ServerPlayer player = ((CommandSourceStack)ctx.getSource()).getPlayer();
        if (player != null) {
            return player;
        }
        return FakePlayerFactory.getMinecraft((ServerLevel)((CommandSourceStack)ctx.getSource()).getLevel());
    }
}

