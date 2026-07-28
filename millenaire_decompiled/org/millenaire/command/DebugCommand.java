/*
 * Decompiled with CFR 0.150.
 * 
 * Could not load the following classes:
 *  com.mojang.brigadier.arguments.ArgumentType
 *  com.mojang.brigadier.arguments.IntegerArgumentType
 *  com.mojang.brigadier.arguments.StringArgumentType
 *  com.mojang.brigadier.builder.LiteralArgumentBuilder
 *  com.mojang.brigadier.context.CommandContext
 *  javax.annotation.Nullable
 *  net.minecraft.commands.CommandSourceStack
 *  net.minecraft.commands.Commands
 *  net.minecraft.core.BlockPos
 *  net.minecraft.core.Position
 *  net.minecraft.core.Vec3i
 *  net.minecraft.core.registries.BuiltInRegistries
 *  net.minecraft.network.chat.Component
 *  net.minecraft.resources.ResourceLocation
 *  net.minecraft.server.level.ServerLevel
 *  net.minecraft.server.level.ServerPlayer
 *  net.minecraft.world.entity.Entity
 *  net.minecraft.world.entity.ai.attributes.Attributes
 *  net.minecraft.world.entity.ai.navigation.PathNavigation
 *  net.minecraft.world.item.Item
 *  net.minecraft.world.level.Level
 *  net.minecraft.world.level.pathfinder.Path
 *  net.minecraft.world.phys.AABB
 *  net.minecraft.world.phys.Vec3
 */
package org.millenaire.command;

import com.mojang.brigadier.arguments.ArgumentType;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import javax.annotation.Nullable;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Position;
import net.minecraft.core.Vec3i;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.navigation.PathNavigation;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.pathfinder.Path;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.millenaire.FormatUtils;
import org.millenaire.building.BuildingId;
import org.millenaire.building.BuildingInstance;
import org.millenaire.building.BuildingInventory;
import org.millenaire.building.ConstructionTask;
import org.millenaire.culture.ModCultures;
import org.millenaire.culture.ReputationLabel;
import org.millenaire.entity.MillVillager;
import org.millenaire.entity.VillagerInventory;
import org.millenaire.goal.GoalScheduler;
import org.millenaire.goal.VillagerGoal;
import org.millenaire.goal.VillagerTask;
import org.millenaire.village.PlayerCultureReputation;
import org.millenaire.village.Village;
import org.millenaire.village.VillageGrowthManager;
import org.millenaire.village.VillageManager;
import org.millenaire.village.VillageReputation;
import org.millenaire.village.VillageSavedData;
import org.millenaire.village.VillageWaypointGraph;
import org.millenaire.village.WallGrowthManager;

public final class DebugCommand {
    private static final Set<UUID> VERBOSE_VILLAGERS = Collections.synchronizedSet(new HashSet());

    private DebugCommand() {
    }

    @Nullable
    private static VillagerLookup findVillagerByPrefix(ServerLevel level, String prefix) {
        VillageSavedData savedData = VillageSavedData.get(level);
        for (Village village : savedData.getVillageManager().getAllVillages()) {
            for (UUID uuid : village.getVillagerUuids()) {
                Entity entity;
                if (!uuid.toString().toLowerCase().startsWith(prefix) || !((entity = level.getEntity(uuid)) instanceof MillVillager)) continue;
                MillVillager mv = (MillVillager)entity;
                return new VillagerLookup(mv, village);
            }
        }
        return null;
    }

    public static boolean isVerbose(UUID villagerUuid) {
        return VERBOSE_VILLAGERS.contains(villagerUuid);
    }

    public static void registerUnder(LiteralArgumentBuilder<CommandSourceStack> parent) {
        parent.then(Commands.literal((String)"debug").executes(DebugCommand::execute));
        parent.then(Commands.literal((String)"verbose").executes(DebugCommand::toggleVerbose));
        parent.then(Commands.literal((String)"growth").executes(DebugCommand::forceGrowth));
        parent.then(Commands.literal((String)"villager").then(Commands.argument((String)"uuid_prefix", (ArgumentType)StringArgumentType.string()).executes(DebugCommand::debugVillager)));
        parent.then(Commands.literal((String)"nav").then(Commands.argument((String)"uuid_prefix", (ArgumentType)StringArgumentType.string()).executes(DebugCommand::debugNav)));
        parent.then(Commands.literal((String)"path").then(Commands.argument((String)"uuid_prefix", (ArgumentType)StringArgumentType.string()).then(Commands.argument((String)"x", (ArgumentType)IntegerArgumentType.integer()).then(Commands.argument((String)"y", (ArgumentType)IntegerArgumentType.integer()).then(Commands.argument((String)"z", (ArgumentType)IntegerArgumentType.integer()).executes(DebugCommand::debugPath))))));
    }

    private static int execute(CommandContext<CommandSourceStack> ctx) {
        CommandSourceStack source = (CommandSourceStack)ctx.getSource();
        ServerLevel level = source.getLevel();
        if (level.dimension() != Level.OVERWORLD) {
            source.sendFailure((Component)Component.literal((String)"No villages outside the Overworld."));
            return 0;
        }
        BlockPos searchPos = BlockPos.containing((Position)source.getPosition());
        ServerPlayer player = source.getPlayer();
        VillageSavedData savedData = VillageSavedData.get(level);
        VillageManager villageManager = savedData.getVillageManager();
        Collection<Village> allVillages = villageManager.getAllVillages();
        source.sendSuccess(() -> Component.literal((String)("=== " + allVillages.size() + " village(s) registered ===")), false);
        for (Village village : allVillages) {
            source.sendSuccess(() -> Component.literal((String)("  " + village.getId().uuid().toString().substring(0, 8) + " | " + village.getCenter().toShortString() + " | " + village.getBuildings().size() + " buildings | " + village.getVillagerUuids().size() + " villagers")), false);
        }
        Village village = villageManager.findNearestVillage(searchPos, 5000.0);
        if (village == null) {
            source.sendSuccess(() -> Component.literal((String)"No village within 5000 blocks."), false);
            return 1;
        }
        source.sendSuccess(() -> Component.literal((String)("=== Village " + village.getId().uuid().toString().substring(0, 8) + " | Center: " + village.getCenter().toShortString() + " | Buildings: " + village.getBuildings().size() + " | Villagers: " + village.getVillagerUuids().size() + " ===")), false);
        for (BuildingInstance b : village.getBuildings()) {
            ConstructionTask ct;
            ConstructionTask task;
            String progressStr = b.getStatus() == BuildingInstance.Status.COMPLETE ? "100%" : ((task = b.getConstructionTask()) != null ? String.format("%.0f%%", Float.valueOf(task.progress() * 100.0f)) : "N/A");
            Object extra = "";
            if (b.isBeingBuilt() && (ct = b.getConstructionTask()) != null) {
                extra = " [reserved=" + ct.isReserved() + " blocked=" + ct.isBlocked() + " failed=" + ct.getFailedAttempts() + "]";
            }
            String finalExtra = extra;
            String line = "  [B] " + String.valueOf((Object)b.getPlanId()) + " | " + String.valueOf((Object)b.getStatus()) + " | " + progressStr;
            source.sendSuccess(() -> Component.literal((String)(line + finalExtra)), false);
            BuildingInventory inv = b.getInventory();
            if (inv != null) {
                Map<Item, Integer> contents = inv.scanChests((Level)level);
                int chestCount = inv.getChestCount();
                if (contents.isEmpty()) {
                    source.sendSuccess(() -> Component.literal((String)("    Inventory: (empty, " + chestCount + " chests)")), false);
                    continue;
                }
                source.sendSuccess(() -> Component.literal((String)("    Inventory: (" + chestCount + " chests)")), false);
                for (Map.Entry<Item, Integer> entry : contents.entrySet()) {
                    String itemName = BuiltInRegistries.ITEM.getKey((Object)entry.getKey()).toString();
                    int count = entry.getValue();
                    source.sendSuccess(() -> Component.literal((String)("    " + itemName + " x" + count)), false);
                }
                continue;
            }
            source.sendSuccess(() -> Component.literal((String)"    Inventory: NULL"), false);
        }
        for (Map.Entry<UUID, ResourceLocation> entry : village.getVillagerTypes().entrySet()) {
            String status;
            UUID uuid = entry.getKey();
            ResourceLocation typeId = entry.getValue();
            Entity entity = level.getEntity(uuid);
            if (entity instanceof MillVillager) {
                MillVillager mv = (MillVillager)entity;
                GoalScheduler scheduler = mv.getGoalScheduler();
                Object goalInfo = "no scheduler";
                if (scheduler != null) {
                    VillagerGoal currentGoal = scheduler.getCurrentGoal();
                    VillagerTask currentTask = scheduler.getCurrentTask();
                    goalInfo = "goal=" + (currentGoal != null ? currentGoal.id().getPath() : "idle") + " task=" + (currentTask != null ? currentTask.goalId().getPath() : "none");
                }
                status = "loaded | type=" + String.valueOf((Object)typeId) + " | " + (String)goalInfo;
            } else {
                int missingCount;
                status = entity != null ? "found but not MillVillager: " + entity.getClass().getSimpleName() : ((missingCount = village.getMissingCount(uuid)) > 0 ? "MISSING (" + missingCount + "/3, respawn pending) | type=" + String.valueOf((Object)typeId) : "not loaded (distant chunk) | type=" + String.valueOf((Object)typeId));
            }
            String line = "  [V] " + FormatUtils.shortUuid(uuid) + " | " + status;
            source.sendSuccess(() -> Component.literal((String)line), false);
        }
        if (player != null) {
            List<ReputationLabel> labels;
            PlayerCultureReputation cultureRepData;
            int cultureRep;
            UUID uUID = player.getUUID();
            int villageRep = village.getReputation().get(uUID);
            int effective = villageRep + (cultureRep = (cultureRepData = PlayerCultureReputation.get(level)).get(uUID, village.getCultureId()));
            String labelStr = VillageReputation.getLabel(effective, labels = ModCultures.getReputationLabels(village.getCultureId()));
            String labelDisplay = labelStr != null ? labelStr : "?";
            String repLine = "  [R] Reputation: village=" + villageRep + " culture=" + cultureRep + " effective=" + effective + " label=" + labelDisplay;
            source.sendSuccess(() -> Component.literal((String)repLine), false);
        } else {
            source.sendSuccess(() -> Component.literal((String)"  [R] Reputation: no player"), false);
        }
        return 1;
    }

    private static int toggleVerbose(CommandContext<CommandSourceStack> ctx) {
        CommandSourceStack source = (CommandSourceStack)ctx.getSource();
        ServerPlayer player = source.getPlayer();
        if (player == null) {
            source.sendFailure((Component)Component.literal((String)"Player command only."));
            return 0;
        }
        ServerLevel level = player.serverLevel();
        MillVillager nearest = null;
        double nearestDist = 400.0;
        AABB searchBox = AABB.ofSize((Vec3)player.position(), (double)40.0, (double)40.0, (double)40.0);
        for (MillVillager mv : level.getEntitiesOfClass(MillVillager.class, searchBox)) {
            double dist = mv.distanceToSqr((Entity)player);
            if (!(dist < nearestDist)) continue;
            nearestDist = dist;
            nearest = mv;
        }
        if (nearest == null) {
            source.sendFailure((Component)Component.literal((String)"No Millenaire villager within 20 blocks."));
            return 0;
        }
        UUID uuid = nearest.getUUID();
        if (VERBOSE_VILLAGERS.contains(uuid)) {
            VERBOSE_VILLAGERS.remove(uuid);
            finalNearest = nearest;
            source.sendSuccess(() -> Component.literal((String)("Verbose logs DISABLED for " + finalNearest.getVillagerDisplayName() + " (" + FormatUtils.shortUuid(uuid) + ")")), false);
        } else {
            VERBOSE_VILLAGERS.add(uuid);
            finalNearest = nearest;
            source.sendSuccess(() -> Component.literal((String)("Verbose logs ENABLED for " + finalNearest.getVillagerDisplayName() + " (" + FormatUtils.shortUuid(uuid) + ")")), false);
        }
        return 1;
    }

    private static int forceGrowth(CommandContext<CommandSourceStack> ctx) {
        CommandSourceStack source = (CommandSourceStack)ctx.getSource();
        ServerLevel level = source.getLevel();
        BlockPos searchPos = BlockPos.containing((Position)source.getPosition());
        VillageSavedData savedData = VillageSavedData.get(level);
        VillageManager villageManager = savedData.getVillageManager();
        Village village = villageManager.findNearestVillage(searchPos, 5000.0);
        if (village == null) {
            source.sendFailure((Component)Component.literal((String)"No village found."));
            return 0;
        }
        VillageGrowthManager.evaluateGrowth(level, village);
        WallGrowthManager.evaluate(level, village);
        savedData.setDirty();
        source.sendSuccess(() -> Component.literal((String)("Growth tick forced for village " + village.getId().uuid().toString().substring(0, 8))), false);
        return 1;
    }

    private static int debugVillager(CommandContext<CommandSourceStack> ctx) {
        String inventoryStr;
        Object goalStr;
        BuildingInstance homeBuilding;
        String prefix;
        CommandSourceStack source = (CommandSourceStack)ctx.getSource();
        ServerLevel level = source.getLevel();
        VillagerLookup lookup = DebugCommand.findVillagerByPrefix(level, prefix = StringArgumentType.getString(ctx, (String)"uuid_prefix").toLowerCase());
        if (lookup == null) {
            source.sendFailure((Component)Component.literal((String)("No loaded villager with UUID starting with '" + prefix + "'.")));
            return 0;
        }
        MillVillager mv = lookup.villager();
        Village village = lookup.village();
        String shortUuid = FormatUtils.shortUuid(mv.getUUID());
        ResourceLocation typeId = mv.getVillagerTypeId();
        String typeStr = typeId != null ? typeId.toString() : "unknown";
        String name = mv.getVillagerDisplayName();
        BlockPos pos = mv.blockPosition();
        float health = mv.getHealth();
        float maxHealth = mv.getMaxHealth();
        BuildingId homeId = mv.getHomeBuilding();
        Object homeStr = homeId != null ? ((homeBuilding = village.getBuilding(homeId)) != null ? homeBuilding.getPlanId().getPath() + " @ " + homeBuilding.getOrigin().toShortString() : homeId.uuid().toString().substring(0, 8) + " (not found)") : "(none)";
        GoalScheduler scheduler = mv.getGoalScheduler();
        if (scheduler != null) {
            VillagerGoal currentGoal = scheduler.getCurrentGoal();
            VillagerTask currentTask = scheduler.getCurrentTask();
            String goalPart = currentGoal != null ? currentGoal.id().getPath() : "idle";
            String taskPart = currentTask != null ? currentTask.goalId().getPath() : "none";
            goalStr = goalPart + " (task: " + taskPart + ")";
        } else {
            goalStr = "no scheduler";
        }
        VillagerInventory inv = mv.getInventory();
        Map<Item, Integer> items = inv.getAll();
        if (items.isEmpty()) {
            inventoryStr = "(empty)";
        } else {
            StringBuilder sb = new StringBuilder();
            boolean first = true;
            for (Map.Entry<Item, Integer> entry : items.entrySet()) {
                if (!first) {
                    sb.append(", ");
                }
                sb.append((Object)BuiltInRegistries.ITEM.getKey((Object)entry.getKey()));
                sb.append(" x").append(entry.getValue());
                first = false;
            }
            inventoryStr = sb.toString();
        }
        boolean isChild = mv.isChild();
        float scale = mv.getVillagerScale();
        String childStr = isChild ? "yes (scale=" + String.format("%.2f", Float.valueOf(scale)) + ")" : "no (adult, scale=" + String.format("%.2f", Float.valueOf(scale)) + ")";
        source.sendSuccess(() -> Component.literal((String)("=== Villager " + shortUuid + " ===")), false);
        source.sendSuccess(() -> Component.literal((String)("Type: " + typeStr)), false);
        source.sendSuccess(() -> Component.literal((String)("Name: " + name)), false);
        source.sendSuccess(() -> Component.literal((String)("Position: " + pos.getX() + ", " + pos.getY() + ", " + pos.getZ())), false);
        source.sendSuccess(() -> Component.literal((String)("Health: " + String.format("%.0f", Float.valueOf(health)) + "/" + String.format("%.0f", Float.valueOf(maxHealth)))), false);
        source.sendSuccess(() -> DebugCommand.lambda$debugVillager$20((String)homeStr), false);
        source.sendSuccess(() -> DebugCommand.lambda$debugVillager$21((String)goalStr), false);
        source.sendSuccess(() -> Component.literal((String)("Inventory: " + inventoryStr)), false);
        source.sendSuccess(() -> Component.literal((String)("Child: " + childStr)), false);
        return 1;
    }

    private static int debugNav(CommandContext<CommandSourceStack> ctx) {
        Village v;
        VillageWaypointGraph graph;
        Map<String, String> navDebug;
        String prefix;
        CommandSourceStack source = (CommandSourceStack)ctx.getSource();
        ServerLevel level = source.getLevel();
        VillagerLookup lookup = DebugCommand.findVillagerByPrefix(level, prefix = StringArgumentType.getString(ctx, (String)"uuid_prefix").toLowerCase());
        if (lookup == null) {
            source.sendFailure((Component)Component.literal((String)("No loaded villager with UUID '" + prefix + "'.")));
            return 0;
        }
        MillVillager mv = lookup.villager();
        String shortUuid = FormatUtils.shortUuid(mv.getUUID());
        String name = mv.getVillagerDisplayName();
        BlockPos pos = mv.blockPosition();
        GoalScheduler scheduler = mv.getGoalScheduler();
        VillagerGoal goal = scheduler.getCurrentGoal();
        VillagerTask task = scheduler.getCurrentTask();
        String goalName = goal != null ? goal.id().getPath() : "none";
        String taskName = task != null ? task.getClass().getSimpleName() : "none";
        source.sendSuccess(() -> Component.literal((String)("\u00a76=== Nav Debug: " + name + " (" + shortUuid + ") ===")), false);
        source.sendSuccess(() -> Component.literal((String)("\u00a7eGoal: \u00a7f" + goalName + " \u00a7e| Task: \u00a7f" + taskName)), false);
        source.sendSuccess(() -> Component.literal((String)("\u00a7ePos: \u00a7f" + pos.getX() + ", " + pos.getY() + ", " + pos.getZ())), false);
        int taskTicks = scheduler.getTaskTicks();
        int maxTicks = scheduler.getMaxTaskTicks();
        int pct = maxTicks > 0 ? taskTicks * 100 / maxTicks : 0;
        source.sendSuccess(() -> Component.literal((String)("\u00a7eWatchdog: \u00a7f" + taskTicks + "/" + maxTicks + " (" + pct + "%)")), false);
        PathNavigation nav = mv.getNavigation();
        boolean isDone = nav.isDone();
        boolean inProgress = nav.isInProgress();
        Path path = nav.getPath();
        String pathStr = path != null ? path.getNodeCount() + " nodes, idx " + path.getNextNodeIndex() : "null";
        source.sendSuccess(() -> Component.literal((String)("\u00a7eVanilla nav: \u00a7fdone=" + isDone + " inProgress=" + inProgress + " path=[" + pathStr + "]")), false);
        if (task != null && !(navDebug = task.getNavDebugInfo()).isEmpty()) {
            source.sendSuccess(() -> Component.literal((String)"\u00a7e--- Task nav ---"), false);
            for (Map.Entry<String, String> entry : navDebug.entrySet()) {
                String key = entry.getKey();
                String val = entry.getValue();
                source.sendSuccess(() -> Component.literal((String)("\u00a7e" + key + ": \u00a7f" + val)), false);
            }
        }
        if ((graph = (v = lookup.village()).getWaypointGraph()) != null) {
            source.sendSuccess(() -> Component.literal((String)("\u00a7eWaypoint graph: \u00a7f" + graph.waypointCount() + " waypoints, available=" + graph.isAvailable())), false);
        } else {
            source.sendSuccess(() -> Component.literal((String)"\u00a7eWaypoint graph: \u00a7cnull"), false);
        }
        return 1;
    }

    private static int debugPath(CommandContext<CommandSourceStack> ctx) {
        CommandSourceStack source = (CommandSourceStack)ctx.getSource();
        ServerLevel level = source.getLevel();
        String prefix = StringArgumentType.getString(ctx, (String)"uuid_prefix").toLowerCase();
        int tx = IntegerArgumentType.getInteger(ctx, (String)"x");
        int ty = IntegerArgumentType.getInteger(ctx, (String)"y");
        int tz = IntegerArgumentType.getInteger(ctx, (String)"z");
        VillagerLookup lookup = DebugCommand.findVillagerByPrefix(level, prefix);
        if (lookup == null) {
            source.sendFailure((Component)Component.literal((String)("Villager not found: " + prefix)));
            return 0;
        }
        MillVillager mv = lookup.villager();
        BlockPos from = mv.blockPosition();
        BlockPos to = new BlockPos(tx, ty, tz);
        double dist = Math.sqrt(from.distSqr((Vec3i)to));
        source.sendSuccess(() -> Component.literal((String)("\u00a76=== Path Test: " + mv.getVillagerDisplayName() + " ===")), false);
        source.sendSuccess(() -> Component.literal((String)("\u00a7eFrom: \u00a7f" + from.toShortString() + " \u00a7eTo: \u00a7f" + to.toShortString() + " \u00a7eDist: \u00a7f" + String.format("%.1f", dist))), false);
        PathNavigation nav = mv.getNavigation();
        boolean success = nav.moveTo((double)tx + 0.5, (double)ty, (double)tz + 0.5, 0.5);
        Path path = nav.getPath();
        if (success && path != null) {
            int nodes = path.getNodeCount();
            double pathDist = path.getDistToTarget();
            source.sendSuccess(() -> Component.literal((String)("\u00a7a\u2713 Path found: \u00a7f" + nodes + " nodes, target dist=" + String.format("%.1f", pathDist))), false);
            nav.stop();
        } else {
            source.sendSuccess(() -> Component.literal((String)("\u00a7c\u2717 No path found (FOLLOW_RANGE=" + String.format("%.0f", mv.getAttribute(Attributes.FOLLOW_RANGE).getValue()) + ")")), false);
            nav.stop();
        }
        return 1;
    }

    private static /* synthetic */ Component lambda$debugVillager$21(String goalStr) {
        return Component.literal((String)("Goal: " + goalStr));
    }

    private static /* synthetic */ Component lambda$debugVillager$20(String homeStr) {
        return Component.literal((String)("Home: " + homeStr));
    }

    private record VillagerLookup(MillVillager villager, Village village) {
    }
}

