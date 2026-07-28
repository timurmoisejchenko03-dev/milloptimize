/*
 * Decompiled with CFR 0.150.
 * 
 * Could not load the following classes:
 *  com.mojang.brigadier.arguments.ArgumentType
 *  com.mojang.brigadier.arguments.IntegerArgumentType
 *  com.mojang.brigadier.arguments.StringArgumentType
 *  com.mojang.brigadier.builder.LiteralArgumentBuilder
 *  com.mojang.brigadier.context.CommandContext
 *  com.mojang.brigadier.exceptions.CommandSyntaxException
 *  com.mojang.logging.LogUtils
 *  net.minecraft.commands.CommandSourceStack
 *  net.minecraft.commands.Commands
 *  net.minecraft.commands.arguments.coordinates.BlockPosArgument
 *  net.minecraft.core.BlockPos
 *  net.minecraft.core.BlockPos$MutableBlockPos
 *  net.minecraft.core.Position
 *  net.minecraft.core.Vec3i
 *  net.minecraft.network.chat.Component
 *  net.minecraft.server.level.ServerLevel
 *  net.minecraft.tags.BlockTags
 *  net.minecraft.world.entity.Entity
 *  net.minecraft.world.level.BlockGetter
 *  net.minecraft.world.level.Level
 *  net.minecraft.world.level.block.SlabBlock
 *  net.minecraft.world.level.block.StairBlock
 *  net.minecraft.world.level.block.state.BlockState
 *  net.minecraft.world.level.block.state.properties.Property
 *  net.minecraft.world.level.block.state.properties.SlabType
 *  net.minecraft.world.level.pathfinder.Node
 *  net.minecraft.world.level.pathfinder.Path
 *  org.slf4j.Logger
 */
package org.millenaire.command;

import com.mojang.brigadier.arguments.ArgumentType;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.logging.LogUtils;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.UUID;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.coordinates.BlockPosArgument;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Position;
import net.minecraft.core.Vec3i;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.SlabBlock;
import net.minecraft.world.level.block.StairBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.Property;
import net.minecraft.world.level.block.state.properties.SlabType;
import net.minecraft.world.level.pathfinder.Node;
import net.minecraft.world.level.pathfinder.Path;
import org.millenaire.diagnostics.NavEvent;
import org.millenaire.diagnostics.NavigationCounters;
import org.millenaire.diagnostics.NavigationEventLog;
import org.millenaire.entity.MillVillager;
import org.millenaire.entity.VillagerNavDriver;
import org.millenaire.goal.GoalScheduler;
import org.millenaire.goal.VillagerGoal;
import org.millenaire.village.Village;
import org.millenaire.village.VillageSavedData;
import org.slf4j.Logger;

public final class NavDiagCommand {
    private static final Logger LOGGER = LogUtils.getLogger();
    private static final int FIND_STUCK_WINDOW_TICKS = 1200;
    private static final int FIND_STUCK_MIN_TELEPORTS = 2;

    private NavDiagCommand() {
    }

    public static void registerUnder(LiteralArgumentBuilder<CommandSourceStack> parent) {
        parent.then(((LiteralArgumentBuilder)Commands.literal((String)"nav-trace").executes(ctx -> NavDiagCommand.runTrace((CommandContext<CommandSourceStack>)ctx, "nearest"))).then(Commands.argument((String)"selector", (ArgumentType)StringArgumentType.string()).executes(ctx -> NavDiagCommand.runTrace((CommandContext<CommandSourceStack>)ctx, StringArgumentType.getString((CommandContext)ctx, (String)"selector")))));
        parent.then(Commands.literal((String)"nav-counters").executes(NavDiagCommand::runCounters));
        parent.then(Commands.literal((String)"nav-counters-reset").executes(ctx -> {
            NavigationCounters.resetAll();
            ((CommandSourceStack)ctx.getSource()).sendSuccess(() -> Component.literal((String)"Nav counters reset."), false);
            return 1;
        }));
        parent.then(Commands.literal((String)"nav-watch").then(Commands.argument((String)"selector", (ArgumentType)StringArgumentType.string()).executes(ctx -> NavDiagCommand.runWatch((CommandContext<CommandSourceStack>)ctx, StringArgumentType.getString((CommandContext)ctx, (String)"selector")))));
        parent.then(Commands.literal((String)"find-stuck").executes(NavDiagCommand::runFindStuck));
        parent.then(Commands.literal((String)"nav-probe").then(Commands.argument((String)"from", (ArgumentType)BlockPosArgument.blockPos()).then(Commands.argument((String)"to", (ArgumentType)BlockPosArgument.blockPos()).executes(NavDiagCommand::runProbe))));
        parent.then(Commands.literal((String)"block-scan").then(Commands.argument((String)"center", (ArgumentType)BlockPosArgument.blockPos()).then(Commands.argument((String)"radius", (ArgumentType)IntegerArgumentType.integer((int)1, (int)24)).then(Commands.argument((String)"yBelow", (ArgumentType)IntegerArgumentType.integer((int)0, (int)16)).then(Commands.argument((String)"yAbove", (ArgumentType)IntegerArgumentType.integer((int)0, (int)16)).executes(NavDiagCommand::runBlockScan))))));
    }

    private static int runBlockScan(CommandContext<CommandSourceStack> ctx) throws CommandSyntaxException {
        CommandSourceStack source = (CommandSourceStack)ctx.getSource();
        ServerLevel level = source.getLevel();
        BlockPos center = BlockPosArgument.getLoadedBlockPos(ctx, (String)"center");
        int radius = IntegerArgumentType.getInteger(ctx, (String)"radius");
        int yBelow = IntegerArgumentType.getInteger(ctx, (String)"yBelow");
        int yAbove = IntegerArgumentType.getInteger(ctx, (String)"yAbove");
        source.sendSuccess(() -> Component.literal((String)("block-scan center=" + center.toShortString() + " r=" + radius + " (X " + (center.getX() - radius) + ".." + (center.getX() + radius) + ", Z " + (center.getZ() - radius) + ".." + (center.getZ() + radius) + ") legend: .=air s=botSlab S=topSlab >=stairs f=fence #=full ,=other M=center")), false);
        BlockPos.MutableBlockPos cursor = new BlockPos.MutableBlockPos();
        for (int y = center.getY() + yAbove; y >= center.getY() - yBelow; --y) {
            int fy = y;
            source.sendSuccess(() -> Component.literal((String)("Y=" + fy + (fy == center.getY() ? " (counter)" : ""))), false);
            for (int x = center.getX() - radius; x <= center.getX() + radius; ++x) {
                StringBuilder row = new StringBuilder();
                for (int z = center.getZ() - radius; z <= center.getZ() + radius; ++z) {
                    cursor.set(x, y, z);
                    int c = x == center.getX() && y == center.getY() && z == center.getZ() ? 77 : NavDiagCommand.classifyBlock(level, (BlockPos)cursor);
                    row.append((char)c);
                }
                String line = "X" + x + " " + String.valueOf(row);
                source.sendSuccess(() -> Component.literal((String)line), false);
            }
        }
        return 1;
    }

    private static char classifyBlock(ServerLevel level, BlockPos pos) {
        BlockState st = level.getBlockState(pos);
        if (st.isAir()) {
            return '.';
        }
        if (st.getBlock() instanceof SlabBlock) {
            SlabType t = (SlabType)st.getValue((Property)SlabBlock.TYPE);
            return (char)(t == SlabType.TOP ? 83 : (t == SlabType.BOTTOM ? 115 : 35));
        }
        if (st.getBlock() instanceof StairBlock) {
            return '>';
        }
        if (st.is(BlockTags.FENCES) || st.is(BlockTags.WALLS) || st.is(BlockTags.FENCE_GATES) || st.is(BlockTags.DOORS)) {
            return 'f';
        }
        if (st.isSolidRender((BlockGetter)level, pos)) {
            return '#';
        }
        return ',';
    }

    private static int runProbe(CommandContext<CommandSourceStack> ctx) throws CommandSyntaxException {
        CommandSourceStack source = (CommandSourceStack)ctx.getSource();
        ServerLevel level = source.getLevel();
        BlockPos from = BlockPosArgument.getLoadedBlockPos(ctx, (String)"from");
        BlockPos to = BlockPosArgument.getLoadedBlockPos(ctx, (String)"to");
        MillVillager v = NavDiagCommand.nearestVillagerTo(level, from);
        if (v == null) {
            source.sendFailure((Component)Component.literal((String)"No loaded villager to use as probe."));
            return 0;
        }
        v.getNavigation().stop();
        v.teleportTo((double)from.getX() + 0.5, from.getY(), (double)from.getZ() + 0.5);
        BlockPos actualStart = v.blockPosition();
        Path path = v.getNavigation().createPath(to, 1);
        StringBuilder sb = new StringBuilder();
        sb.append("nav-probe ").append(NavDiagCommand.describe(v)).append(" from=").append(from.toShortString()).append(" (landed=").append(actualStart.toShortString()).append(")").append(" to=").append(to.toShortString()).append(" \u2192 ");
        if (path == null) {
            sb.append("NO PATH (createPath returned null)");
        } else {
            Node end = path.getEndNode();
            BlockPos endPos = end != null ? end.asBlockPos() : null;
            sb.append("canReach=").append(path.canReach()).append(" nodes=").append(path.getNodeCount());
            if (endPos != null) {
                sb.append(" end=").append(endPos.toShortString()).append(" endDy=").append(endPos.getY() - to.getY()).append(" endHorizDist=").append(String.format(Locale.ROOT, "%.1f", Math.sqrt(NavDiagCommand.NavDiagHorizSq(endPos, to))));
            }
        }
        String line = sb.toString();
        source.sendSuccess(() -> Component.literal((String)line), false);
        return 1;
    }

    private static double NavDiagHorizSq(BlockPos a, BlockPos b) {
        double dx = a.getX() - b.getX();
        double dz = a.getZ() - b.getZ();
        return dx * dx + dz * dz;
    }

    private static MillVillager nearestVillagerTo(ServerLevel level, BlockPos pos) {
        MillVillager best = null;
        double bestSq = Double.MAX_VALUE;
        for (Village village : VillageSavedData.get(level).getVillageManager().getAllVillages()) {
            for (UUID uuid : village.getVillagerUuids()) {
                MillVillager v;
                double ds;
                Entity entity = level.getEntity(uuid);
                if (!(entity instanceof MillVillager) || !((ds = (v = (MillVillager)entity).blockPosition().distSqr((Vec3i)pos)) < bestSq)) continue;
                bestSq = ds;
                best = v;
            }
        }
        return best;
    }

    private static int runTrace(CommandContext<CommandSourceStack> ctx, String selector) {
        ServerLevel level;
        CommandSourceStack source = (CommandSourceStack)ctx.getSource();
        MillVillager v = NavDiagCommand.resolveVillager(source, level = source.getLevel(), selector);
        if (v == null) {
            source.sendFailure((Component)Component.literal((String)("Villager not found: " + selector)));
            return 0;
        }
        NavigationEventLog log = v.getNavEventLog();
        List<NavEvent> events = log.snapshot();
        long now = level.getGameTime();
        String header = "Nav trace \u2014 " + NavDiagCommand.describe(v) + " @ " + v.blockPosition().toShortString() + " (events=" + events.size() + "/256, lastEvent t-" + String.valueOf(log.lastEventTick() < 0L ? "\u221e" : Long.valueOf(now - log.lastEventTick())) + ")";
        source.sendSuccess(() -> Component.literal((String)header), false);
        if (events.isEmpty()) {
            source.sendSuccess(() -> Component.literal((String)"  (no events recorded)"), false);
            return 1;
        }
        for (NavEvent e : events) {
            long age = now - e.tick();
            String line = String.format(Locale.ROOT, "  t-%-5d %s/%s %s", new Object[]{age, e.layer(), e.type(), e.detail()});
            source.sendSuccess(() -> Component.literal((String)line), false);
        }
        GoalScheduler scheduler = v.getGoalScheduler();
        if (scheduler != null) {
            VillagerGoal goal = scheduler.getCurrentGoal();
            String goalId = goal != null ? goal.id().toString() : "idle";
            VillagerNavDriver.NavDiagnostics diag = v.getNavManager().getDiagnostics();
            BlockPos dest = v.getNavManager().getDestination();
            String tail = "Goal=" + goalId + " localStuck=" + diag.localStuck() + " longDistStuck=" + diag.longDistanceStuck() + " teleports=" + diag.teleportCount() + (String)(dest != null ? " dest=" + dest.toShortString() : "");
            source.sendSuccess(() -> Component.literal((String)tail), false);
        }
        return 1;
    }

    private static int runCounters(CommandContext<CommandSourceStack> ctx) {
        CommandSourceStack source = (CommandSourceStack)ctx.getSource();
        StringBuilder sb = new StringBuilder();
        sb.append("Nav counters \u2014 ");
        sb.append("teleport.total=").append(NavigationCounters.teleportTotal()).append(" ");
        for (NavEvent.Layer l : NavEvent.Layer.values()) {
            long c = NavigationCounters.teleportFor(l);
            if (c <= 0L) continue;
            sb.append("tp.").append((Object)l).append("=").append(c).append(" ");
        }
        sb.append("short-jump=").append(NavigationCounters.shortJump()).append(" ");
        sb.append("goal.abandoned=").append(NavigationCounters.goalAbandoned()).append(" ");
        sb.append("target.invalid=").append(NavigationCounters.targetInvalid()).append(" ");
        sb.append("reload.pose_restored=").append(NavigationCounters.poseSleepingRestored()).append(" ");
        sb.append("reload.pose_cleared=").append(NavigationCounters.poseSleepingCleared()).append(" ");
        sb.append("bed.suffocation=").append(NavigationCounters.bedSuffocation()).append(" ");
        sb.append("leaf_clear.skipped_in_building=").append(NavigationCounters.leafClearSkippedInBuilding());
        String line = sb.toString();
        source.sendSuccess(() -> Component.literal((String)line), false);
        return 1;
    }

    private static int runWatch(CommandContext<CommandSourceStack> ctx, String selector) {
        ServerLevel level;
        CommandSourceStack source = (CommandSourceStack)ctx.getSource();
        MillVillager v = NavDiagCommand.resolveVillager(source, level = source.getLevel(), selector);
        if (v == null) {
            source.sendFailure((Component)Component.literal((String)("Villager not found: " + selector)));
            return 0;
        }
        NavigationEventLog log = v.getNavEventLog();
        if (log.isWatched()) {
            log.disableWatch();
            source.sendSuccess(() -> Component.literal((String)("Watch OFF \u2014 " + NavDiagCommand.describe(v))), false);
        } else {
            log.enableWatch(LOGGER, "[nav-watch " + NavDiagCommand.describe(v) + "]");
            source.sendSuccess(() -> Component.literal((String)("Watch ON \u2014 " + NavDiagCommand.describe(v) + " (see server log)")), false);
        }
        return 1;
    }

    private static int runFindStuck(CommandContext<CommandSourceStack> ctx) {
        CommandSourceStack source = (CommandSourceStack)ctx.getSource();
        ServerLevel level = source.getLevel();
        if (level.dimension() != Level.OVERWORLD) {
            source.sendFailure((Component)Component.literal((String)"Run this command in the Overworld."));
            return 0;
        }
        long now = level.getGameTime();
        ArrayList<MillVillager> flagged = new ArrayList<MillVillager>();
        int scanned = 0;
        for (Village village : VillageSavedData.get(level).getVillageManager().getAllVillages()) {
            for (UUID uuid : village.getVillagerUuids()) {
                Entity entity = level.getEntity(uuid);
                if (!(entity instanceof MillVillager)) continue;
                MillVillager v = (MillVillager)entity;
                ++scanned;
                NavigationEventLog log = v.getNavEventLog();
                int recentTps = log.countRecent(NavEvent.Type.TELEPORT, now, 1200L);
                int recentAbandons = log.countRecent(NavEvent.Type.GOAL_ABANDONED, now, 1200L);
                boolean flag = recentTps >= 2 || recentAbandons > 0 || log.hasStuckSinceLastStart();
                if (!flag) continue;
                flagged.add(v);
            }
        }
        int scannedFinal = scanned;
        source.sendSuccess(() -> Component.literal((String)("Scanned " + scannedFinal + " loaded villagers \u2014 " + flagged.size() + " flagged")), false);
        for (MillVillager v : flagged) {
            NavigationEventLog log = v.getNavEventLog();
            int tp = log.countRecent(NavEvent.Type.TELEPORT, now, 1200L);
            int ab = log.countRecent(NavEvent.Type.GOAL_ABANDONED, now, 1200L);
            String line = "  " + NavDiagCommand.describe(v) + " @ " + v.blockPosition().toShortString() + " tps20s=" + tp + " abandons20s=" + ab + " stuckSig=" + log.hasStuckSinceLastStart();
            source.sendSuccess(() -> Component.literal((String)line), false);
        }
        return 1;
    }

    private static MillVillager resolveVillager(CommandSourceStack source, ServerLevel level, String selector) {
        if (selector == null || selector.isBlank() || "-".equals(selector) || selector.equalsIgnoreCase("nearest")) {
            BlockPos origin = BlockPos.containing((Position)source.getPosition());
            MillVillager best = null;
            double bestSq = Double.MAX_VALUE;
            for (Village village : VillageSavedData.get(level).getVillageManager().getAllVillages()) {
                for (UUID uuid : village.getVillagerUuids()) {
                    MillVillager v;
                    double ds;
                    Entity entity = level.getEntity(uuid);
                    if (!(entity instanceof MillVillager) || !((ds = (v = (MillVillager)entity).blockPosition().distSqr((Vec3i)origin)) < bestSq)) continue;
                    bestSq = ds;
                    best = v;
                }
            }
            return best;
        }
        String needle = selector.toLowerCase(Locale.ROOT);
        for (Village village : VillageSavedData.get(level).getVillageManager().getAllVillages()) {
            for (UUID uuid : village.getVillagerUuids()) {
                String typeName;
                Entity entity = level.getEntity(uuid);
                if (!(entity instanceof MillVillager)) continue;
                MillVillager v = (MillVillager)entity;
                if (uuid.toString().startsWith(needle)) {
                    return v;
                }
                String string = typeName = v.getVillagerTypeId() != null ? v.getVillagerTypeId().getPath() : "";
                if (typeName.toLowerCase(Locale.ROOT).contains(needle)) {
                    return v;
                }
                if (v.getName() == null || !v.getName().getString().toLowerCase(Locale.ROOT).contains(needle)) continue;
                return v;
            }
        }
        return null;
    }

    private static String describe(MillVillager v) {
        String type = v.getVillagerTypeId() != null ? v.getVillagerTypeId().getPath() : "?";
        String uuid = v.getUUID().toString().substring(0, 8);
        return type + "[" + uuid + "]";
    }
}

