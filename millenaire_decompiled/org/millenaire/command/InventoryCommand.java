/*
 * Decompiled with CFR 0.150.
 * 
 * Could not load the following classes:
 *  com.mojang.brigadier.arguments.ArgumentType
 *  com.mojang.brigadier.arguments.IntegerArgumentType
 *  com.mojang.brigadier.arguments.StringArgumentType
 *  com.mojang.brigadier.builder.LiteralArgumentBuilder
 *  com.mojang.brigadier.context.CommandContext
 *  net.minecraft.commands.CommandSourceStack
 *  net.minecraft.commands.Commands
 *  net.minecraft.commands.arguments.ResourceLocationArgument
 *  net.minecraft.core.BlockPos
 *  net.minecraft.core.Position
 *  net.minecraft.core.registries.BuiltInRegistries
 *  net.minecraft.network.chat.Component
 *  net.minecraft.resources.ResourceLocation
 *  net.minecraft.server.level.ServerLevel
 *  net.minecraft.world.item.Item
 *  net.minecraft.world.item.Items
 *  net.minecraft.world.level.Level
 */
package org.millenaire.command;

import com.mojang.brigadier.arguments.ArgumentType;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import java.util.Map;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.ResourceLocationArgument;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Position;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import org.millenaire.building.BuildingInstance;
import org.millenaire.building.BuildingInventory;
import org.millenaire.item.ItemHelper;
import org.millenaire.village.Village;
import org.millenaire.village.VillageSavedData;

public final class InventoryCommand {
    private InventoryCommand() {
    }

    public static void registerUnder(LiteralArgumentBuilder<CommandSourceStack> parent) {
        parent.then(((LiteralArgumentBuilder)((LiteralArgumentBuilder)((LiteralArgumentBuilder)((LiteralArgumentBuilder)Commands.literal((String)"inventory").then(Commands.literal((String)"list").executes(InventoryCommand::listInventory))).then(Commands.literal((String)"add").then(Commands.argument((String)"item", (ArgumentType)ResourceLocationArgument.id()).then(Commands.argument((String)"count", (ArgumentType)IntegerArgumentType.integer((int)1)).executes(InventoryCommand::addItem))))).then(Commands.literal((String)"remove").then(Commands.argument((String)"item", (ArgumentType)ResourceLocationArgument.id()).then(Commands.argument((String)"count", (ArgumentType)IntegerArgumentType.integer((int)1)).executes(InventoryCommand::removeItem))))).then(Commands.literal((String)"add-to").then(Commands.argument((String)"building", (ArgumentType)StringArgumentType.string()).then(Commands.argument((String)"item", (ArgumentType)ResourceLocationArgument.id()).then(Commands.argument((String)"count", (ArgumentType)IntegerArgumentType.integer((int)1)).executes(InventoryCommand::addToBuilding)))))).then(Commands.literal((String)"fill").executes(InventoryCommand::fillTestGoods)));
    }

    private static Village findVillage(CommandSourceStack source) {
        ServerLevel level = source.getLevel();
        if (level.dimension() != Level.OVERWORLD) {
            return null;
        }
        BlockPos searchPos = BlockPos.containing((Position)source.getPosition());
        return VillageSavedData.get(level).getVillageManager().findNearestVillage(searchPos, 5000.0);
    }

    private static BuildingInventory getTownHallInventory(CommandSourceStack source) {
        Village village = InventoryCommand.findVillage(source);
        if (village == null) {
            source.sendFailure((Component)Component.translatable((String)"command.millenaire.inv.no_village"));
            return null;
        }
        BuildingInstance th = village.getTownhall();
        if (th == null) {
            source.sendFailure((Component)Component.translatable((String)"command.millenaire.inv.no_townhall"));
            return null;
        }
        BuildingInventory inv = th.getInventory();
        if (inv == null) {
            source.sendFailure((Component)Component.translatable((String)"command.millenaire.inv.no_townhall_inv"));
            return null;
        }
        return inv;
    }

    private static int listInventory(CommandContext<CommandSourceStack> ctx) {
        CommandSourceStack source = (CommandSourceStack)ctx.getSource();
        ServerLevel level = source.getLevel();
        BuildingInventory inv = InventoryCommand.getTownHallInventory(source);
        if (inv == null) {
            return 0;
        }
        Map<Item, Integer> contents = inv.scanChests((Level)level);
        if (contents.isEmpty()) {
            source.sendSuccess(() -> Component.translatable((String)"command.millenaire.inv.empty"), false);
            return 1;
        }
        source.sendSuccess(() -> Component.translatable((String)"command.millenaire.inv.list_header"), false);
        for (Map.Entry<Item, Integer> entry : contents.entrySet()) {
            String itemName = BuiltInRegistries.ITEM.getKey((Object)entry.getKey()).toString();
            int count = entry.getValue();
            source.sendSuccess(() -> Component.literal((String)("  " + itemName + " x" + count)), false);
        }
        return 1;
    }

    private static int addItem(CommandContext<CommandSourceStack> ctx) {
        CommandSourceStack source = (CommandSourceStack)ctx.getSource();
        ServerLevel level = source.getLevel();
        BuildingInventory inv = InventoryCommand.getTownHallInventory(source);
        if (inv == null) {
            return 0;
        }
        ResourceLocation itemId = ResourceLocationArgument.getId(ctx, (String)"item");
        int count = IntegerArgumentType.getInteger(ctx, (String)"count");
        Item item = ItemHelper.resolve(itemId);
        if (item == null) {
            source.sendFailure((Component)Component.translatable((String)"command.millenaire.error.unknown_item", (Object[])new Object[]{itemId.toString()}));
            return 0;
        }
        int added = inv.add((Level)level, item, count);
        String itemName = itemId.toString();
        source.sendSuccess(() -> Component.translatable((String)"command.millenaire.inv.added", (Object[])new Object[]{added, itemName}), false);
        return 1;
    }

    private static int fillTestGoods(CommandContext<CommandSourceStack> ctx) {
        CommandSourceStack source = (CommandSourceStack)ctx.getSource();
        ServerLevel level = source.getLevel();
        BuildingInventory inv = InventoryCommand.getTownHallInventory(source);
        if (inv == null) {
            return 0;
        }
        inv.add((Level)level, Items.OAK_LOG, 2048);
        inv.add((Level)level, Items.SPRUCE_LOG, 1024);
        inv.add((Level)level, Items.DARK_OAK_LOG, 1024);
        inv.add((Level)level, Items.COBBLESTONE, 2048);
        inv.add((Level)level, Items.STONE, 3500);
        inv.add((Level)level, Items.GLASS, 512);
        inv.add((Level)level, Items.SAND, 256);
        inv.add((Level)level, Items.OAK_PLANKS, 1024);
        inv.add((Level)level, Items.WHITE_WOOL, 256);
        inv.add((Level)level, Items.IRON_INGOT, 128);
        inv.add((Level)level, Items.GOLD_INGOT, 32);
        inv.add((Level)level, Items.WHEAT_SEEDS, 256);
        inv.add((Level)level, Items.WHEAT, 128);
        inv.add((Level)level, Items.BREAD, 64);
        inv.add((Level)level, Items.OAK_SAPLING, 64);
        source.sendSuccess(() -> Component.translatable((String)"command.millenaire.inv.filled"), false);
        return 1;
    }

    private static int addToBuilding(CommandContext<CommandSourceStack> ctx) {
        CommandSourceStack source = (CommandSourceStack)ctx.getSource();
        ServerLevel level = source.getLevel();
        String buildingPattern = StringArgumentType.getString(ctx, (String)"building");
        ResourceLocation itemId = ResourceLocationArgument.getId(ctx, (String)"item");
        int count = IntegerArgumentType.getInteger(ctx, (String)"count");
        Village village = InventoryCommand.findVillage(source);
        if (village == null) {
            source.sendFailure((Component)Component.translatable((String)"command.millenaire.inv.no_village"));
            return 0;
        }
        Item item = ItemHelper.resolve(itemId);
        if (item == null) {
            source.sendFailure((Component)Component.translatable((String)"command.millenaire.error.unknown_item", (Object[])new Object[]{itemId.toString()}));
            return 0;
        }
        for (BuildingInstance building : village.getBuildings()) {
            BuildingInventory inv;
            if (building.getStatus() != BuildingInstance.Status.COMPLETE || !building.getPlanId().toString().contains(buildingPattern) || (inv = building.getInventory()) == null) continue;
            int added = inv.add((Level)level, item, count);
            String planId = building.getPlanId().toString();
            source.sendSuccess(() -> Component.translatable((String)"command.millenaire.inv.added_to", (Object[])new Object[]{added, itemId.toString(), planId}), false);
            return 1;
        }
        source.sendFailure((Component)Component.translatable((String)"command.millenaire.inv.no_complete_building", (Object[])new Object[]{buildingPattern}));
        return 0;
    }

    private static int removeItem(CommandContext<CommandSourceStack> ctx) {
        CommandSourceStack source = (CommandSourceStack)ctx.getSource();
        ServerLevel level = source.getLevel();
        BuildingInventory inv = InventoryCommand.getTownHallInventory(source);
        if (inv == null) {
            return 0;
        }
        ResourceLocation itemId = ResourceLocationArgument.getId(ctx, (String)"item");
        int count = IntegerArgumentType.getInteger(ctx, (String)"count");
        Item item = ItemHelper.resolve(itemId);
        if (item == null) {
            source.sendFailure((Component)Component.translatable((String)"command.millenaire.error.unknown_item", (Object[])new Object[]{itemId.toString()}));
            return 0;
        }
        int removed = inv.remove((Level)level, item, count);
        String itemName = itemId.toString();
        source.sendSuccess(() -> Component.translatable((String)"command.millenaire.inv.removed", (Object[])new Object[]{removed, itemName}), false);
        return 1;
    }
}

