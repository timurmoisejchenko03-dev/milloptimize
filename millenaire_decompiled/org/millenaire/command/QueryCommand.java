/*
 * Decompiled with CFR 0.150.
 * 
 * Could not load the following classes:
 *  com.google.gson.Gson
 *  com.google.gson.GsonBuilder
 *  com.mojang.brigadier.CommandDispatcher
 *  com.mojang.brigadier.arguments.ArgumentType
 *  com.mojang.brigadier.arguments.IntegerArgumentType
 *  com.mojang.brigadier.builder.LiteralArgumentBuilder
 *  com.mojang.brigadier.context.CommandContext
 *  net.minecraft.commands.CommandSourceStack
 *  net.minecraft.commands.Commands
 *  net.minecraft.core.BlockPos
 *  net.minecraft.network.chat.Component
 *  net.minecraft.resources.ResourceLocation
 *  net.minecraft.server.level.ServerLevel
 *  net.minecraft.world.entity.Entity
 *  net.minecraft.world.level.Level
 */
package org.millenaire.command;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.ArgumentType;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;
import org.millenaire.FormatUtils;
import org.millenaire.building.BuildingInstance;
import org.millenaire.building.ConstructionTask;
import org.millenaire.entity.MillVillager;
import org.millenaire.goal.GoalScheduler;
import org.millenaire.goal.VillagerGoal;
import org.millenaire.goal.VillagerTask;
import org.millenaire.village.Village;
import org.millenaire.village.VillageSavedData;

public final class QueryCommand {
    private static final Gson GSON = new GsonBuilder().create();

    private QueryCommand() {
    }

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register((LiteralArgumentBuilder)Commands.literal((String)"millenaire").then(((LiteralArgumentBuilder)((LiteralArgumentBuilder)((LiteralArgumentBuilder)((LiteralArgumentBuilder)((LiteralArgumentBuilder)Commands.literal((String)"query").requires(source -> source.hasPermission(2))).then(((LiteralArgumentBuilder)Commands.literal((String)"village").executes(ctx -> QueryCommand.queryVillage((CommandContext<CommandSourceStack>)ctx, 0))).then(Commands.argument((String)"index", (ArgumentType)IntegerArgumentType.integer((int)0)).executes(ctx -> QueryCommand.queryVillage((CommandContext<CommandSourceStack>)ctx, IntegerArgumentType.getInteger((CommandContext)ctx, (String)"index")))))).then(((LiteralArgumentBuilder)Commands.literal((String)"buildings").executes(ctx -> QueryCommand.queryBuildings((CommandContext<CommandSourceStack>)ctx, 0))).then(Commands.argument((String)"index", (ArgumentType)IntegerArgumentType.integer((int)0)).executes(ctx -> QueryCommand.queryBuildings((CommandContext<CommandSourceStack>)ctx, IntegerArgumentType.getInteger((CommandContext)ctx, (String)"index")))))).then(((LiteralArgumentBuilder)Commands.literal((String)"villagers").executes(ctx -> QueryCommand.queryVillagers((CommandContext<CommandSourceStack>)ctx, 0))).then(Commands.argument((String)"index", (ArgumentType)IntegerArgumentType.integer((int)0)).executes(ctx -> QueryCommand.queryVillagers((CommandContext<CommandSourceStack>)ctx, IntegerArgumentType.getInteger((CommandContext)ctx, (String)"index")))))).then(((LiteralArgumentBuilder)Commands.literal((String)"growth").executes(ctx -> QueryCommand.queryGrowth((CommandContext<CommandSourceStack>)ctx, 0))).then(Commands.argument((String)"index", (ArgumentType)IntegerArgumentType.integer((int)0)).executes(ctx -> QueryCommand.queryGrowth((CommandContext<CommandSourceStack>)ctx, IntegerArgumentType.getInteger((CommandContext)ctx, (String)"index")))))).then(Commands.literal((String)"lonebuildings").executes(QueryCommand::queryLoneBuildings))));
    }

    private static Village getVillageByIndex(CommandSourceStack source, int index) {
        ServerLevel level = source.getLevel();
        if (level.dimension() != Level.OVERWORLD) {
            return null;
        }
        VillageSavedData savedData = VillageSavedData.get(level);
        ArrayList<Village> villages = new ArrayList<Village>(savedData.getVillageManager().getAllVillages());
        if (index < 0 || index >= villages.size()) {
            return null;
        }
        return (Village)villages.get(index);
    }

    private static void sendJson(CommandSourceStack source, Object data) {
        source.sendSuccess(() -> Component.literal((String)GSON.toJson(data)), false);
    }

    private static int queryVillage(CommandContext<CommandSourceStack> ctx, int index) {
        CommandSourceStack source = (CommandSourceStack)ctx.getSource();
        Village village = QueryCommand.getVillageByIndex(source, index);
        if (village == null) {
            source.sendFailure((Component)Component.literal((String)"{\"error\":\"village not found\"}"));
            return 0;
        }
        LinkedHashMap<String, Object> result = new LinkedHashMap<String, Object>();
        result.put("id", village.getId().uuid().toString().substring(0, 8));
        result.put("name", village.getVillageName());
        result.put("center", QueryCommand.posToList(village.getCenter()));
        result.put("culture", village.getCultureId().toString());
        result.put("type", village.getVillageTypeId().toString());
        result.put("buildingCount", village.getBuildings().size());
        result.put("villagerCount", village.getVillagerUuids().size());
        QueryCommand.sendJson(source, result);
        return 1;
    }

    private static int queryBuildings(CommandContext<CommandSourceStack> ctx, int index) {
        CommandSourceStack source = (CommandSourceStack)ctx.getSource();
        Village village = QueryCommand.getVillageByIndex(source, index);
        if (village == null) {
            source.sendFailure((Component)Component.literal((String)"{\"error\":\"village not found\"}"));
            return 0;
        }
        ArrayList buildings = new ArrayList();
        for (BuildingInstance b : village.getBuildings()) {
            LinkedHashMap<String, Object> bMap = new LinkedHashMap<String, Object>();
            bMap.put("plan", b.getPlanId().toString());
            bMap.put("status", b.getStatus().name());
            double progress = 1.0;
            if (b.getStatus() != BuildingInstance.Status.COMPLETE) {
                ConstructionTask task = b.getConstructionTask();
                progress = task != null ? (double)task.progress() : 0.0;
            }
            bMap.put("progress", (double)Math.round(progress * 100.0) / 100.0);
            bMap.put("origin", QueryCommand.posToList(b.getOrigin()));
            if (!b.getRuntimeTags().isEmpty()) {
                bMap.put("runtimeTags", new ArrayList<String>(b.getRuntimeTags()));
            }
            if (b.getParentBuildingId() != null) {
                bMap.put("parentBuildingId", b.getParentBuildingId().uuid().toString());
            }
            buildings.add(bMap);
        }
        QueryCommand.sendJson(source, buildings);
        return 1;
    }

    private static int queryVillagers(CommandContext<CommandSourceStack> ctx, int index) {
        CommandSourceStack source = (CommandSourceStack)ctx.getSource();
        Village village = QueryCommand.getVillageByIndex(source, index);
        if (village == null) {
            source.sendFailure((Component)Component.literal((String)"{\"error\":\"village not found\"}"));
            return 0;
        }
        ServerLevel level = source.getLevel();
        ArrayList villagers = new ArrayList();
        for (Map.Entry<UUID, ResourceLocation> entry : village.getVillagerTypes().entrySet()) {
            UUID uuid = entry.getKey();
            ResourceLocation typeId = entry.getValue();
            LinkedHashMap<String, Object> vMap = new LinkedHashMap<String, Object>();
            vMap.put("uuid", FormatUtils.shortUuid(uuid));
            vMap.put("type", typeId.toString());
            Entity entity = level.getEntity(uuid);
            if (entity instanceof MillVillager) {
                MillVillager mv = (MillVillager)entity;
                vMap.put("loaded", true);
                vMap.put("pos", QueryCommand.posToList(mv.blockPosition()));
                GoalScheduler scheduler = mv.getGoalScheduler();
                if (scheduler != null) {
                    VillagerGoal goal = scheduler.getCurrentGoal();
                    VillagerTask task = scheduler.getCurrentTask();
                    vMap.put("goal", goal != null ? goal.id().getPath() : "idle");
                    vMap.put("task", task != null ? task.goalId().getPath() : null);
                } else {
                    vMap.put("goal", null);
                    vMap.put("task", null);
                }
            } else {
                vMap.put("loaded", false);
                int missingCount = village.getMissingCount(uuid);
                vMap.put("missing", missingCount > 0);
                vMap.put("missingCount", missingCount);
            }
            villagers.add(vMap);
        }
        QueryCommand.sendJson(source, villagers);
        return 1;
    }

    private static int queryGrowth(CommandContext<CommandSourceStack> ctx, int index) {
        CommandSourceStack source = (CommandSourceStack)ctx.getSource();
        Village village = QueryCommand.getVillageByIndex(source, index);
        if (village == null) {
            source.sendFailure((Component)Component.literal((String)"{\"error\":\"village not found\"}"));
            return 0;
        }
        boolean ongoingConstruction = village.getBuildings().stream().anyMatch(BuildingInstance::isBeingBuilt);
        long completeCount = village.getBuildings().stream().filter(b -> b.getStatus() == BuildingInstance.Status.COMPLETE).count();
        LinkedHashMap<String, Serializable> result = new LinkedHashMap<String, Serializable>();
        result.put("buildingCount", Integer.valueOf(village.getBuildings().size()));
        result.put("completeCount", Long.valueOf(completeCount));
        result.put("ongoingConstruction", Boolean.valueOf(ongoingConstruction));
        QueryCommand.sendJson(source, result);
        return 1;
    }

    private static int queryLoneBuildings(CommandContext<CommandSourceStack> ctx) {
        CommandSourceStack source = (CommandSourceStack)ctx.getSource();
        ServerLevel level = source.getLevel();
        if (level.dimension() != Level.OVERWORLD) {
            source.sendFailure((Component)Component.literal((String)"{\"error\":\"not in overworld\"}"));
            return 0;
        }
        VillageSavedData savedData = VillageSavedData.get(level);
        ArrayList result = new ArrayList();
        for (VillageSavedData.LoneBuildingEntry entry : savedData.getLoneBuildingPositions()) {
            LinkedHashMap<String, Object> map = new LinkedHashMap<String, Object>();
            map.put("type", entry.type().toString());
            map.put("culture", entry.culture());
            map.put("pos", QueryCommand.posToList(entry.pos()));
            if (entry.generatedFor() != null) {
                map.put("generatedFor", entry.generatedFor());
            }
            result.add(map);
        }
        QueryCommand.sendJson(source, result);
        return 1;
    }

    private static List<Integer> posToList(BlockPos pos) {
        return List.of(Integer.valueOf(pos.getX()), Integer.valueOf(pos.getY()), Integer.valueOf(pos.getZ()));
    }
}

