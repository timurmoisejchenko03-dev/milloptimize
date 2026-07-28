/*
 * Decompiled with CFR 0.150.
 * 
 * Could not load the following classes:
 *  com.google.gson.Gson
 *  com.google.gson.GsonBuilder
 *  com.mojang.brigadier.CommandDispatcher
 *  com.mojang.brigadier.arguments.ArgumentType
 *  com.mojang.brigadier.arguments.IntegerArgumentType
 *  com.mojang.brigadier.arguments.StringArgumentType
 *  com.mojang.brigadier.builder.LiteralArgumentBuilder
 *  com.mojang.brigadier.builder.RequiredArgumentBuilder
 *  com.mojang.brigadier.context.CommandContext
 *  net.minecraft.advancements.AdvancementHolder
 *  net.minecraft.advancements.AdvancementProgress
 *  net.minecraft.commands.CommandSourceStack
 *  net.minecraft.commands.Commands
 *  net.minecraft.commands.arguments.ResourceLocationArgument
 *  net.minecraft.core.BlockPos
 *  net.minecraft.network.chat.Component
 *  net.minecraft.resources.ResourceLocation
 *  net.minecraft.server.PlayerAdvancements
 *  net.minecraft.server.ServerAdvancementManager
 *  net.minecraft.server.level.ServerLevel
 *  net.minecraft.server.level.ServerPlayer
 *  net.minecraft.world.InteractionHand
 *  net.minecraft.world.entity.Entity
 *  net.minecraft.world.entity.player.Player
 *  net.minecraft.world.inventory.AbstractContainerMenu
 *  net.minecraft.world.item.Item
 *  net.minecraft.world.item.ItemStack
 *  net.minecraft.world.level.ItemLike
 *  net.minecraft.world.level.Level
 *  net.minecraft.world.phys.AABB
 */
package org.millenaire.command;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.ArgumentType;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.builder.RequiredArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import net.minecraft.advancements.AdvancementHolder;
import net.minecraft.advancements.AdvancementProgress;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.ResourceLocationArgument;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.PlayerAdvancements;
import net.minecraft.server.ServerAdvancementManager;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ItemLike;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import org.millenaire.advancement.MillAdvancements;
import org.millenaire.building.BuildingInstance;
import org.millenaire.building.BuildingPlan;
import org.millenaire.commerce.TradeAction;
import org.millenaire.commerce.TradeMenu;
import org.millenaire.culture.ModCultures;
import org.millenaire.culture.VillagerType;
import org.millenaire.entity.MillVillager;
import org.millenaire.item.ItemHelper;
import org.millenaire.item.MoneyHelper;
import org.millenaire.test.TestPlayerManager;
import org.millenaire.village.Village;
import org.millenaire.village.VillageSavedData;

public final class TestCommand {
    private static final Gson GSON = new GsonBuilder().create();

    private TestCommand() {
    }

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register((LiteralArgumentBuilder)Commands.literal((String)"millenaire").then(((LiteralArgumentBuilder)((LiteralArgumentBuilder)((LiteralArgumentBuilder)((LiteralArgumentBuilder)((LiteralArgumentBuilder)((LiteralArgumentBuilder)((LiteralArgumentBuilder)((LiteralArgumentBuilder)((LiteralArgumentBuilder)((LiteralArgumentBuilder)((LiteralArgumentBuilder)((LiteralArgumentBuilder)Commands.literal((String)"test").requires(source -> source.hasPermission(2))).then(((LiteralArgumentBuilder)Commands.literal((String)"spawn-player").executes(TestCommand::spawnPlayerDefault)).then(Commands.argument((String)"x", (ArgumentType)IntegerArgumentType.integer()).then(Commands.argument((String)"y", (ArgumentType)IntegerArgumentType.integer()).then(Commands.argument((String)"z", (ArgumentType)IntegerArgumentType.integer()).executes(TestCommand::spawnPlayerAt)))))).then(Commands.literal((String)"remove-player").executes(TestCommand::removePlayer))).then(Commands.literal((String)"status").executes(TestCommand::status))).then(Commands.literal((String)"give").then(Commands.argument((String)"item", (ArgumentType)ResourceLocationArgument.id()).then(Commands.argument((String)"count", (ArgumentType)IntegerArgumentType.integer((int)1, (int)64)).executes(TestCommand::give))))).then(Commands.literal((String)"move").then(Commands.argument((String)"x", (ArgumentType)IntegerArgumentType.integer()).then(Commands.argument((String)"y", (ArgumentType)IntegerArgumentType.integer()).then(Commands.argument((String)"z", (ArgumentType)IntegerArgumentType.integer()).executes(TestCommand::move)))))).then(Commands.literal((String)"interact").then(Commands.argument((String)"uuid_prefix", (ArgumentType)StringArgumentType.string()).executes(TestCommand::interact)))).then(((LiteralArgumentBuilder)Commands.literal((String)"interact-nearest").executes(TestCommand::interactNearest)).then(Commands.argument((String)"tag", (ArgumentType)StringArgumentType.string()).executes(TestCommand::interactNearestTag)))).then(((LiteralArgumentBuilder)Commands.literal((String)"trade").then(Commands.literal((String)"buy").then(((RequiredArgumentBuilder)Commands.argument((String)"goodIndex", (ArgumentType)IntegerArgumentType.integer((int)0)).executes(ctx -> TestCommand.trade((CommandContext<CommandSourceStack>)ctx, false, 1))).then(Commands.argument((String)"qty", (ArgumentType)IntegerArgumentType.integer((int)1)).executes(ctx -> TestCommand.trade((CommandContext<CommandSourceStack>)ctx, false, IntegerArgumentType.getInteger((CommandContext)ctx, (String)"qty"))))))).then(Commands.literal((String)"sell").then(((RequiredArgumentBuilder)Commands.argument((String)"goodIndex", (ArgumentType)IntegerArgumentType.integer((int)0)).executes(ctx -> TestCommand.trade((CommandContext<CommandSourceStack>)ctx, true, 1))).then(Commands.argument((String)"qty", (ArgumentType)IntegerArgumentType.integer((int)1)).executes(ctx -> TestCommand.trade((CommandContext<CommandSourceStack>)ctx, true, IntegerArgumentType.getInteger((CommandContext)ctx, (String)"qty")))))))).then(Commands.literal((String)"close-menu").executes(TestCommand::closeMenu))).then(((LiteralArgumentBuilder)Commands.literal((String)"find-shop").executes(ctx -> TestCommand.findShop((CommandContext<CommandSourceStack>)ctx, null))).then(Commands.argument((String)"plan_filter", (ArgumentType)StringArgumentType.string()).executes(ctx -> TestCommand.findShop((CommandContext<CommandSourceStack>)ctx, StringArgumentType.getString((CommandContext)ctx, (String)"plan_filter")))))).then(Commands.literal((String)"advancements").executes(TestCommand::advancements))).then(Commands.literal((String)"generate-chunks").then(Commands.argument((String)"x", (ArgumentType)IntegerArgumentType.integer()).then(Commands.argument((String)"z", (ArgumentType)IntegerArgumentType.integer()).then(Commands.argument((String)"radius", (ArgumentType)IntegerArgumentType.integer((int)1, (int)20)).executes(TestCommand::generateChunks)))))));
    }

    private static ServerPlayer requirePlayer(CommandSourceStack source) {
        ServerPlayer player = TestPlayerManager.get();
        if (player == null) {
            source.sendFailure((Component)Component.literal((String)"No active TestPlayer. Use spawn-player first."));
        }
        return player;
    }

    private static void sendJson(CommandSourceStack source, Map<String, Object> data) {
        source.sendSuccess(() -> Component.literal((String)GSON.toJson((Object)data)), false);
    }

    private static int spawnPlayerDefault(CommandContext<CommandSourceStack> ctx) {
        CommandSourceStack source = (CommandSourceStack)ctx.getSource();
        ServerLevel level = source.getServer().getLevel(Level.OVERWORLD);
        if (level == null) {
            source.sendFailure((Component)Component.literal((String)"No Overworld available."));
            return 0;
        }
        BlockPos pos = level.getSharedSpawnPos();
        return TestCommand.doSpawn(source, level, pos);
    }

    private static int spawnPlayerAt(CommandContext<CommandSourceStack> ctx) {
        CommandSourceStack source = (CommandSourceStack)ctx.getSource();
        ServerLevel level = source.getServer().getLevel(Level.OVERWORLD);
        if (level == null) {
            source.sendFailure((Component)Component.literal((String)"No Overworld available."));
            return 0;
        }
        int x = IntegerArgumentType.getInteger(ctx, (String)"x");
        int y = IntegerArgumentType.getInteger(ctx, (String)"y");
        int z = IntegerArgumentType.getInteger(ctx, (String)"z");
        return TestCommand.doSpawn(source, level, new BlockPos(x, y, z));
    }

    private static int doSpawn(CommandSourceStack source, ServerLevel level, BlockPos pos) {
        if (TestPlayerManager.isActive()) {
            source.sendFailure((Component)Component.literal((String)"TestPlayer already active. Use remove-player first."));
            return 0;
        }
        try {
            TestPlayerManager.spawn(source.getServer(), level, pos);
            source.sendSuccess(() -> Component.literal((String)("TestPlayer created at " + pos.getX() + " " + pos.getY() + " " + pos.getZ())), false);
            return 1;
        }
        catch (Exception e) {
            source.sendFailure((Component)Component.literal((String)("Error: " + e.getMessage())));
            return 0;
        }
    }

    private static int removePlayer(CommandContext<CommandSourceStack> ctx) {
        CommandSourceStack source = (CommandSourceStack)ctx.getSource();
        if (!TestPlayerManager.isActive()) {
            source.sendFailure((Component)Component.literal((String)"No active TestPlayer."));
            return 0;
        }
        TestPlayerManager.remove();
        source.sendSuccess(() -> Component.literal((String)"TestPlayer removed."), false);
        return 1;
    }

    private static int status(CommandContext<CommandSourceStack> ctx) {
        CommandSourceStack source = (CommandSourceStack)ctx.getSource();
        ServerPlayer player = TestCommand.requirePlayer(source);
        if (player == null) {
            return 0;
        }
        LinkedHashMap<String, Object> data = new LinkedHashMap<String, Object>();
        data.put("name", player.getGameProfile().getName());
        LinkedHashMap<String, Integer> pos = new LinkedHashMap<String, Integer>();
        BlockPos bp = player.blockPosition();
        pos.put("x", bp.getX());
        pos.put("y", bp.getY());
        pos.put("z", bp.getZ());
        data.put("pos", pos);
        data.put("deniers", MoneyHelper.getTotalDeniers(player.getInventory()));
        int nonEmpty = 0;
        for (int i = 0; i < player.getInventory().getContainerSize(); ++i) {
            if (player.getInventory().getItem(i).isEmpty()) continue;
            ++nonEmpty;
        }
        data.put("inventory_size", nonEmpty);
        boolean menuOpen = player.containerMenu != player.inventoryMenu;
        data.put("menu_open", menuOpen);
        if (menuOpen) {
            data.put("menu_type", player.containerMenu.getClass().getSimpleName());
        }
        TestCommand.sendJson(source, data);
        return 1;
    }

    private static int give(CommandContext<CommandSourceStack> ctx) {
        CommandSourceStack source = (CommandSourceStack)ctx.getSource();
        ServerPlayer player = TestCommand.requirePlayer(source);
        if (player == null) {
            return 0;
        }
        ResourceLocation itemId = ResourceLocationArgument.getId(ctx, (String)"item");
        int count = IntegerArgumentType.getInteger(ctx, (String)"count");
        Item item = ItemHelper.resolve(itemId);
        if (item == null) {
            source.sendFailure((Component)Component.literal((String)("Unknown item: " + String.valueOf((Object)itemId))));
            return 0;
        }
        player.getInventory().add(new ItemStack((ItemLike)item, count));
        source.sendSuccess(() -> Component.literal((String)("Given " + count + "x " + String.valueOf((Object)itemId) + " to TestPlayer.")), false);
        return 1;
    }

    private static int move(CommandContext<CommandSourceStack> ctx) {
        CommandSourceStack source = (CommandSourceStack)ctx.getSource();
        ServerPlayer player = TestCommand.requirePlayer(source);
        if (player == null) {
            return 0;
        }
        int x = IntegerArgumentType.getInteger(ctx, (String)"x");
        int y = IntegerArgumentType.getInteger(ctx, (String)"y");
        int z = IntegerArgumentType.getInteger(ctx, (String)"z");
        ServerLevel level = source.getServer().getLevel(Level.OVERWORLD);
        if (level == null) {
            source.sendFailure((Component)Component.literal((String)"No Overworld."));
            return 0;
        }
        player.teleportTo(level, (double)x + 0.5, (double)y, (double)z + 0.5, Set.of(), player.getYRot(), player.getXRot());
        TestPlayerManager.confirmTeleport();
        TestPlayerManager.acknowledgeChunkBatch();
        source.sendSuccess(() -> Component.literal((String)("TestPlayer moved to " + x + " " + y + " " + z)), false);
        return 1;
    }

    private static int interact(CommandContext<CommandSourceStack> ctx) {
        CommandSourceStack source = (CommandSourceStack)ctx.getSource();
        ServerPlayer player = TestCommand.requirePlayer(source);
        if (player == null) {
            return 0;
        }
        String prefix = StringArgumentType.getString(ctx, (String)"uuid_prefix").toLowerCase();
        ServerLevel level = source.getServer().getLevel(Level.OVERWORLD);
        if (level == null) {
            return 0;
        }
        List<MillVillager> matches = level.getEntitiesOfClass(MillVillager.class, new AABB(player.blockPosition()).inflate(100.0)).stream().filter(v -> v.getUUID().toString().startsWith(prefix)).sorted(Comparator.comparingDouble(v -> v.distanceToSqr((Entity)player))).toList();
        if (matches.isEmpty()) {
            source.sendFailure((Component)Component.literal((String)("No villager found with prefix " + prefix)));
            return 0;
        }
        MillVillager target = matches.getFirst();
        target.mobInteract((Player)player, InteractionHand.MAIN_HAND);
        source.sendSuccess(() -> Component.literal((String)("Interacted with " + target.getUUID().toString().substring(0, 8) + " (" + String.valueOf((Object)target.getVillagerTypeId()) + ")")), false);
        return 1;
    }

    private static int interactNearest(CommandContext<CommandSourceStack> ctx) {
        return TestCommand.doInteractNearest(ctx, null);
    }

    private static int interactNearestTag(CommandContext<CommandSourceStack> ctx) {
        String tag = StringArgumentType.getString(ctx, (String)"tag");
        return TestCommand.doInteractNearest(ctx, tag);
    }

    private static int doInteractNearest(CommandContext<CommandSourceStack> ctx, String tag) {
        CommandSourceStack source = (CommandSourceStack)ctx.getSource();
        ServerPlayer player = TestCommand.requirePlayer(source);
        if (player == null) {
            return 0;
        }
        ServerLevel level = source.getServer().getLevel(Level.OVERWORLD);
        if (level == null) {
            return 0;
        }
        List<MillVillager> candidates = level.getEntitiesOfClass(MillVillager.class, new AABB(player.blockPosition()).inflate(10.0));
        if (tag != null) {
            candidates = candidates.stream().filter(v -> {
                VillagerType vType = ModCultures.getVillagerType(v.getVillagerTypeId());
                return vType != null && vType.hasTag(tag);
            }).toList();
        }
        if (candidates.isEmpty()) {
            Object msg = tag != null ? "No villager found with tag " + tag : "No villager found in range";
            source.sendFailure((Component)Component.literal((String)msg));
            return 0;
        }
        MillVillager target = candidates.stream().min(Comparator.comparingDouble(v -> v.distanceToSqr((Entity)player))).orElseThrow();
        target.mobInteract((Player)player, InteractionHand.MAIN_HAND);
        source.sendSuccess(() -> Component.literal((String)("Interacted with " + target.getUUID().toString().substring(0, 8) + " (" + String.valueOf((Object)target.getVillagerTypeId()) + ")" + (String)(tag != null ? " [tag=" + tag + "]" : ""))), false);
        return 1;
    }

    private static int trade(CommandContext<CommandSourceStack> ctx, boolean isSell, int qty) {
        CommandSourceStack source = (CommandSourceStack)ctx.getSource();
        ServerPlayer player = TestCommand.requirePlayer(source);
        if (player == null) {
            return 0;
        }
        if (qty != 1 && qty != 8 && qty != 64) {
            source.sendFailure((Component)Component.literal((String)("Invalid quantity: " + qty + ". Accepted values: 1, 8, 64.")));
            return 0;
        }
        if (player.containerMenu == player.inventoryMenu) {
            source.sendFailure((Component)Component.literal((String)"No menu open on TestPlayer."));
            return 0;
        }
        AbstractContainerMenu abstractContainerMenu = player.containerMenu;
        if (!(abstractContainerMenu instanceof TradeMenu)) {
            source.sendFailure((Component)Component.literal((String)("Open menu is not a TradeMenu (" + player.containerMenu.getClass().getSimpleName() + ").")));
            return 0;
        }
        TradeMenu menu = (TradeMenu)abstractContainerMenu;
        int goodIndex = IntegerArgumentType.getInteger(ctx, (String)"goodIndex");
        if (goodIndex >= menu.getGoodsCount()) {
            source.sendFailure((Component)Component.literal((String)("Good index " + goodIndex + " invalid (0-" + (menu.getGoodsCount() - 1) + ").")));
            return 0;
        }
        TradeAction action = TradeAction.fromDirectionAndQuantity(!isSell, qty);
        if (action == null) {
            source.sendFailure((Component)Component.literal((String)"Invalid trade action."));
            return 0;
        }
        int buttonId = action.toButtonId(goodIndex);
        int deniersBefore = MoneyHelper.getTotalDeniers(player.getInventory());
        boolean accepted = menu.clickMenuButton((Player)player, buttonId);
        int deniersAfter = MoneyHelper.getTotalDeniers(player.getInventory());
        int delta = deniersAfter - deniersBefore;
        boolean success = !accepted ? false : delta != 0;
        LinkedHashMap<String, Object> result = new LinkedHashMap<String, Object>();
        result.put("deniers_before", deniersBefore);
        result.put("deniers_after", deniersAfter);
        result.put("deniers_delta", delta);
        result.put("success", success);
        result.put("accepted", accepted);
        TestCommand.sendJson(source, result);
        return accepted ? 1 : 0;
    }

    private static int closeMenu(CommandContext<CommandSourceStack> ctx) {
        CommandSourceStack source = (CommandSourceStack)ctx.getSource();
        ServerPlayer player = TestCommand.requirePlayer(source);
        if (player == null) {
            return 0;
        }
        if (player.containerMenu == player.inventoryMenu) {
            source.sendSuccess(() -> Component.literal((String)"No menu open."), false);
            return 1;
        }
        player.closeContainer();
        source.sendSuccess(() -> Component.literal((String)"Menu closed."), false);
        return 1;
    }

    private static int findShop(CommandContext<CommandSourceStack> ctx, String planFilter) {
        CommandSourceStack source = (CommandSourceStack)ctx.getSource();
        ServerLevel level = source.getServer().getLevel(Level.OVERWORLD);
        if (level == null) {
            source.sendFailure((Component)Component.literal((String)"No Overworld."));
            return 0;
        }
        VillageSavedData savedData = VillageSavedData.get(level);
        Village village = savedData.getVillageManager().findNearestVillage(BlockPos.ZERO, 5000.0);
        if (village == null) {
            source.sendFailure((Component)Component.literal((String)"No village found."));
            return 0;
        }
        for (BuildingInstance b : village.getBuildings()) {
            BuildingPlan plan;
            if (b.getStatus() != BuildingInstance.Status.COMPLETE || planFilter != null && !b.getPlanId().toString().contains(planFilter) || (plan = ModCultures.getBuildingPlan(b.getPlanId())) == null || plan.shopId() == null) continue;
            BlockPos sp = b.getFirstPointPos("sellingPos");
            if (sp == null) {
                sp = b.getFirstPointPos("sleepingPos");
            }
            if (sp == null) continue;
            LinkedHashMap<String, Object> result = new LinkedHashMap<String, Object>();
            result.put("building", b.getPlanId().toString());
            result.put("shop_id", plan.shopId());
            result.put("x", sp.getX());
            result.put("y", sp.getY());
            result.put("z", sp.getZ());
            TestCommand.sendJson(source, result);
            return 1;
        }
        source.sendFailure((Component)Component.literal((String)"No shop building with selling position found."));
        return 0;
    }

    private static int advancements(CommandContext<CommandSourceStack> ctx) {
        CommandSourceStack source = (CommandSourceStack)ctx.getSource();
        ServerPlayer player = TestCommand.requirePlayer(source);
        if (player == null) {
            return 0;
        }
        ArrayList<String> earned = new ArrayList<String>();
        ServerAdvancementManager serverAdvancements = player.server.getAdvancements();
        PlayerAdvancements playerAdvancements = player.getAdvancements();
        for (ResourceLocation advId : TestCommand.getAllMillenaireAdvancementIds()) {
            AdvancementProgress progress;
            AdvancementHolder holder = serverAdvancements.get(advId);
            if (holder == null || !(progress = playerAdvancements.getOrStartProgress(holder)).isDone()) continue;
            earned.add(advId.getPath());
        }
        LinkedHashMap<String, Object> data = new LinkedHashMap<String, Object>();
        data.put("count", earned.size());
        data.put("earned", earned);
        TestCommand.sendJson(source, data);
        return 1;
    }

    private static List<ResourceLocation> getAllMillenaireAdvancementIds() {
        ArrayList<ResourceLocation> ids = new ArrayList<ResourceLocation>();
        ids.add(MillAdvancements.FIRST_CONTACT);
        ids.add(MillAdvancements.CRESUS);
        ids.add(MillAdvancements.CHEERS);
        ids.add(MillAdvancements.MASTER_FARMER);
        ids.add(MillAdvancements.GREAT_HUNTER);
        ids.add(MillAdvancements.HIRED);
        ids.add(MillAdvancements.RAINBOW);
        ids.add(MillAdvancements.SUMMONING_WAND);
        ids.add(MillAdvancements.AMATEUR_ARCHITECT);
        ids.add(MillAdvancements.MEDIEVAL_METROPOLIS);
        ids.add(MillAdvancements.EXPLORER);
        ids.add(MillAdvancements.MARCO_POLO);
        ids.add(MillAdvancements.MAGELLAN);
        ids.add(MillAdvancements.PANTHEON);
        ids.add(MillAdvancements.THE_QUEST);
        ids.add(MillAdvancements.MAITRE_A_PENSER);
        ids.add(MillAdvancements.WQ_NORMAN);
        ids.add(MillAdvancements.WQ_INDIAN);
        ids.add(MillAdvancements.WQ_MAYAN);
        ids.add(MillAdvancements.PUJA);
        ids.add(MillAdvancements.SACRIFICE);
        ids.add(MillAdvancements.FRIEND_INDEED);
        ids.add(MillAdvancements.SELF_DEFENSE);
        ids.add(MillAdvancements.DARK_SIDE);
        ids.add(MillAdvancements.ATTILA);
        ids.add(MillAdvancements.SCIPIO);
        ids.add(MillAdvancements.VIKING);
        ids.add(MillAdvancements.SELJUK_ISTANBUL);
        ids.add(MillAdvancements.BYZANTINES_NOTTODAY);
        ids.add(MillAdvancements.MARVEL_NORMAN);
        ids.add(MillAdvancements.MP_WEAPON);
        ids.add(MillAdvancements.MP_HIREDGOON);
        ids.add(MillAdvancements.MP_RAIDONPLAYER);
        ids.add(MillAdvancements.MP_NEIGHBOURTRADE);
        ids.add(MillAdvancements.MP_FRIENDLYVILLAGE);
        for (String culture : MillAdvancements.ADVANCEMENT_CULTURES) {
            ResourceLocation complete;
            ResourceLocation leader;
            ResourceLocation rep = MillAdvancements.REP.get(culture);
            if (rep != null) {
                ids.add(rep);
            }
            if ((leader = MillAdvancements.LEADER.get(culture)) != null) {
                ids.add(leader);
            }
            if ((complete = MillAdvancements.COMPLETE.get(culture)) == null) continue;
            ids.add(complete);
        }
        return ids;
    }

    private static int generateChunks(CommandContext<CommandSourceStack> ctx) {
        final CommandSourceStack source = (CommandSourceStack)ctx.getSource();
        final ServerLevel level = source.getServer().getLevel(Level.OVERWORLD);
        if (level == null) {
            source.sendFailure((Component)Component.literal((String)"No Overworld."));
            return 0;
        }
        int cx = IntegerArgumentType.getInteger(ctx, (String)"x") >> 4;
        int cz = IntegerArgumentType.getInteger(ctx, (String)"z") >> 4;
        int radius = IntegerArgumentType.getInteger(ctx, (String)"radius");
        final int side = 2 * radius + 1;
        final int startDx = -radius;
        final int fCx = cx;
        final int fCz = cz;
        final int fRadius = radius;
        source.getServer().execute(new Runnable(){
            int dx;
            {
                this.dx = startDx;
            }

            @Override
            public void run() {
                for (int dz = -fRadius; dz <= fRadius; ++dz) {
                    level.getChunk(fCx + this.dx, fCz + dz);
                }
                ++this.dx;
                if (this.dx <= fRadius) {
                    source.getServer().execute((Runnable)this);
                } else {
                    int total = side * side;
                    source.sendSuccess(() -> Component.literal((String)("Generated " + total + " chunks around chunk " + fCx + "," + fCz + " (radius=" + fRadius + ")")), false);
                }
            }
        });
        source.sendSuccess(() -> Component.literal((String)("Generating chunks (async, " + side + " rows)...")), false);
        return 1;
    }
}

