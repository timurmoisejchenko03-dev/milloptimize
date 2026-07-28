/*
 * Decompiled with CFR 0.152.
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
 *  net.minecraft.commands.arguments.ResourceLocationArgument
 *  net.minecraft.core.BlockPos
 *  net.minecraft.core.Direction
 *  net.minecraft.core.Vec3i
 *  net.minecraft.network.chat.Component
 *  net.minecraft.resources.ResourceLocation
 *  net.minecraft.server.level.ServerLevel
 *  net.minecraft.server.level.ServerPlayer
 *  net.minecraft.world.level.block.Rotation
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
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Random;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.commands.arguments.ResourceLocationArgument;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Vec3i;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.block.Rotation;
import org.millenaire.building.BuildingPlan;
import org.millenaire.building.BuildingPlanSet;
import org.millenaire.building.ClearMargins;
import org.millenaire.building.HearthLightingUtil;
import org.millenaire.culture.ModCultures;
import org.millenaire.world.BuildingPlacer;
import org.millenaire.world.TerrainPreparer;

public final class SpawnBuildingCommand {
    private static final Random RANDOM = new Random();
    private static final SuggestionProvider<CommandSourceStack> PLAN_SET_SUGGESTIONS = (ctx, builder) -> SharedSuggestionProvider.suggest(ModCultures.getAllBuildingPlanSets().keySet().stream().map(id -> id.getPath().replace('/', '_')), (SuggestionsBuilder)builder);
    private static final SuggestionProvider<CommandSourceStack> VARIANT_SUGGESTIONS = (ctx, builder) -> {
        BuildingPlanSet planSet = ModCultures.getBuildingPlanSet(SpawnBuildingCommand.resolvePlanSetId((CommandContext<CommandSourceStack>)ctx));
        if (planSet != null) {
            return SharedSuggestionProvider.suggest(planSet.variants().keySet().stream(), (SuggestionsBuilder)builder);
        }
        return builder.buildFuture();
    };
    private static final SuggestionProvider<CommandSourceStack> ROTATION_SUGGESTIONS = (ctx, builder) -> SharedSuggestionProvider.suggest(Arrays.stream(Rotation.values()).map(r -> r.name().toLowerCase()), (SuggestionsBuilder)builder);
    private static final SuggestionProvider<CommandSourceStack> LEVEL_SUGGESTIONS = (ctx, builder) -> {
        BuildingPlanSet planSet = ModCultures.getBuildingPlanSet(SpawnBuildingCommand.resolvePlanSetId((CommandContext<CommandSourceStack>)ctx));
        if (planSet != null) {
            String variant = StringArgumentType.getString((CommandContext)ctx, (String)"variant");
            int count = planSet.getLevelCount(variant);
            ArrayList<String> levels = new ArrayList<String>();
            for (int i = 0; i < count; ++i) {
                levels.add(String.valueOf(i));
            }
            return SharedSuggestionProvider.suggest(levels, (SuggestionsBuilder)builder);
        }
        return builder.buildFuture();
    };

    private SpawnBuildingCommand() {
    }

    public static void registerUnder(LiteralArgumentBuilder<CommandSourceStack> parent) {
        RequiredArgumentBuilder herePlanSet = (RequiredArgumentBuilder)((RequiredArgumentBuilder)Commands.argument((String)"plan_set", (ArgumentType)ResourceLocationArgument.id()).suggests(PLAN_SET_SUGGESTIONS).executes(ctx -> SpawnBuildingCommand.executeHere((CommandContext<CommandSourceStack>)ctx))).then(((RequiredArgumentBuilder)Commands.argument((String)"variant", (ArgumentType)StringArgumentType.string()).suggests(VARIANT_SUGGESTIONS).executes(ctx -> SpawnBuildingCommand.executeHere((CommandContext<CommandSourceStack>)ctx))).then(((RequiredArgumentBuilder)Commands.argument((String)"level", (ArgumentType)IntegerArgumentType.integer((int)0)).suggests(LEVEL_SUGGESTIONS).executes(ctx -> SpawnBuildingCommand.executeHere((CommandContext<CommandSourceStack>)ctx))).then(((RequiredArgumentBuilder)Commands.argument((String)"rotation", (ArgumentType)StringArgumentType.string()).suggests(ROTATION_SUGGESTIONS).executes(ctx -> SpawnBuildingCommand.executeHere((CommandContext<CommandSourceStack>)ctx))).then(Commands.argument((String)"showMockBlocks", (ArgumentType)BoolArgumentType.bool()).executes(ctx -> SpawnBuildingCommand.executeHere((CommandContext<CommandSourceStack>)ctx))))));
        RequiredArgumentBuilder atPlanSet = (RequiredArgumentBuilder)((RequiredArgumentBuilder)Commands.argument((String)"plan_set", (ArgumentType)ResourceLocationArgument.id()).suggests(PLAN_SET_SUGGESTIONS).executes(ctx -> SpawnBuildingCommand.executeAt((CommandContext<CommandSourceStack>)ctx))).then(((RequiredArgumentBuilder)Commands.argument((String)"variant", (ArgumentType)StringArgumentType.string()).suggests(VARIANT_SUGGESTIONS).executes(ctx -> SpawnBuildingCommand.executeAt((CommandContext<CommandSourceStack>)ctx))).then(((RequiredArgumentBuilder)Commands.argument((String)"level", (ArgumentType)IntegerArgumentType.integer((int)0)).suggests(LEVEL_SUGGESTIONS).executes(ctx -> SpawnBuildingCommand.executeAt((CommandContext<CommandSourceStack>)ctx))).then(((RequiredArgumentBuilder)Commands.argument((String)"rotation", (ArgumentType)StringArgumentType.string()).suggests(ROTATION_SUGGESTIONS).executes(ctx -> SpawnBuildingCommand.executeAt((CommandContext<CommandSourceStack>)ctx))).then(Commands.argument((String)"showMockBlocks", (ArgumentType)BoolArgumentType.bool()).executes(ctx -> SpawnBuildingCommand.executeAt((CommandContext<CommandSourceStack>)ctx))))));
        LiteralArgumentBuilder atBranch = (LiteralArgumentBuilder)Commands.literal((String)"at").then(Commands.argument((String)"x", (ArgumentType)IntegerArgumentType.integer()).then(Commands.argument((String)"y", (ArgumentType)IntegerArgumentType.integer()).then(Commands.argument((String)"z", (ArgumentType)IntegerArgumentType.integer()).then((ArgumentBuilder)atPlanSet))));
        parent.then(((LiteralArgumentBuilder)Commands.literal((String)"building").then((ArgumentBuilder)herePlanSet)).then((ArgumentBuilder)atBranch));
    }

    private static int executeHere(CommandContext<CommandSourceStack> ctx) {
        ServerPlayer player = ((CommandSourceStack)ctx.getSource()).getPlayer();
        if (player == null) {
            ((CommandSourceStack)ctx.getSource()).sendFailure((Component)Component.literal((String)"This command must be run by a player. Use: /millenaire building at <x> <y> <z> <plan_set>"));
            return 0;
        }
        BlockPos playerPos = player.blockPosition();
        Direction direction = player.getDirection();
        BlockPos spawnPos = playerPos.relative(direction, 2);
        return SpawnBuildingCommand.doSpawn(ctx, player.serverLevel(), spawnPos);
    }

    private static int executeAt(CommandContext<CommandSourceStack> ctx) {
        int x = IntegerArgumentType.getInteger(ctx, (String)"x");
        int y = IntegerArgumentType.getInteger(ctx, (String)"y");
        int z = IntegerArgumentType.getInteger(ctx, (String)"z");
        return SpawnBuildingCommand.doSpawn(ctx, ((CommandSourceStack)ctx.getSource()).getLevel(), new BlockPos(x, y, z));
    }

    private static int doSpawn(CommandContext<CommandSourceStack> ctx, ServerLevel level, BlockPos pos) {
        BuildingPlan level0Plan;
        CommandSourceStack source = (CommandSourceStack)ctx.getSource();
        ResourceLocation planSetId = SpawnBuildingCommand.resolvePlanSetId(ctx);
        BuildingPlanSet planSet = ModCultures.getBuildingPlanSet(planSetId);
        if (planSet == null) {
            List<String> available = ModCultures.getAllBuildingPlanSets().keySet().stream().map(ResourceLocation::getPath).sorted().toList();
            source.sendFailure((Component)Component.literal((String)("Unknown BuildingPlanSet: " + String.valueOf(planSetId) + ". Available: " + String.valueOf(available))));
            return 0;
        }
        String variant = SpawnBuildingCommand.getOptionalString(ctx, "variant");
        if (variant == null) {
            variant = planSet.pickRandomVariant(RANDOM);
        } else if (!planSet.variants().containsKey(variant)) {
            source.sendFailure((Component)Component.literal((String)("Unknown variant '" + variant + "' for " + String.valueOf(planSetId) + ". Available: " + String.valueOf(planSet.variants().keySet()))));
            return 0;
        }
        int requestedLevel = SpawnBuildingCommand.getOptionalInt(ctx, "level", 0);
        BuildingPlanSet.LevelDef levelDef = planSet.getLevel(variant, requestedLevel);
        if (levelDef == null) {
            int maxLevel = planSet.getLevelCount(variant) - 1;
            source.sendFailure((Component)Component.literal((String)("Level " + requestedLevel + " does not exist for " + String.valueOf(planSetId) + " variant " + variant + ". Max: " + maxLevel)));
            return 0;
        }
        BuildingPlan plan = ModCultures.getBuildingPlan(levelDef.planId());
        if (plan == null) {
            source.sendFailure((Component)Component.literal((String)("BuildingPlan not found: " + String.valueOf(levelDef.planId()))));
            return 0;
        }
        Rotation rotation = Rotation.NONE;
        String rotationArg = SpawnBuildingCommand.getOptionalString(ctx, "rotation");
        if (rotationArg != null && (rotation = SpawnBuildingCommand.parseRotation(rotationArg)) == null) {
            source.sendFailure((Component)Component.literal((String)("Unknown rotation: " + rotationArg + ". Values: none, clockwise_90, clockwise_180, counterclockwise_90")));
            return 0;
        }
        boolean showMockBlocks = SpawnBuildingCommand.getOptionalBool(ctx, "showMockBlocks", false);
        BuildingPlanSet.LevelDef level0Def = planSet.getLevel(variant, 0);
        BuildingPlan buildingPlan = level0Plan = level0Def != null ? ModCultures.getBuildingPlan(level0Def.planId()) : plan;
        if (level0Plan == null) {
            level0Plan = plan;
        }
        ClearMargins margins = planSet.clearMargins();
        boolean[][] snowMap = TerrainPreparer.checkForSnow(level, pos, level0Plan.width(), level0Plan.depth(), rotation, margins);
        int baseY = TerrainPreparer.clearAndFlatten(level, pos, level0Plan.width(), level0Plan.height(), level0Plan.depth(), rotation, level0Plan.groundLevel(), margins);
        TerrainPreparer.decayOrphanedLeaves(level, pos, level0Plan.width(), level0Plan.depth(), rotation, baseY, margins);
        int placementY = baseY + level0Plan.groundLevel();
        BlockPos origin = new BlockPos(pos.getX(), placementY, pos.getZ());
        if (!BuildingPlacer.placeInstantly(level, level0Plan, origin, rotation, !showMockBlocks)) {
            source.sendFailure((Component)Component.literal((String)"Failed to place level 0 (template not found?)."));
            return 0;
        }
        HearthLightingUtil.lightHearthsInArea(level, origin, new Vec3i(level0Plan.width(), level0Plan.height(), level0Plan.depth()));
        for (int lvl = 1; lvl <= requestedLevel; ++lvl) {
            BuildingPlan upgradePlan;
            BuildingPlanSet.LevelDef upgradeDef = planSet.getLevel(variant, lvl);
            if (upgradeDef == null || (upgradePlan = ModCultures.getBuildingPlan(upgradeDef.planId())) == null) continue;
            int upgradePlacementY = baseY + upgradePlan.groundLevel();
            BlockPos upgradeOrigin = new BlockPos(pos.getX(), upgradePlacementY, pos.getZ());
            BuildingPlacer.placeUpgradeInstantly(level, upgradePlan, upgradeOrigin, rotation);
            HearthLightingUtil.lightHearthsInArea(level, upgradeOrigin, new Vec3i(upgradePlan.width(), upgradePlan.height(), upgradePlan.depth()));
        }
        TerrainPreparer.restoreSnow(level, pos, level0Plan.width(), level0Plan.depth(), rotation, snowMap, margins);
        String finalVariant = variant;
        Rotation finalRotation = rotation;
        source.sendSuccess(() -> Component.literal((String)("Building placed: " + String.valueOf(planSetId) + " [variant=" + finalVariant + ", level=0\u2192" + requestedLevel + ", rotation=" + finalRotation.name().toLowerCase() + ", mockBlocks=" + showMockBlocks + "] at " + origin.toShortString() + " (baseY=" + baseY + ", groundLevel=" + plan.groundLevel() + ", dim=" + plan.width() + "\u00d7" + plan.height() + "\u00d7" + plan.depth() + ")")), true);
        return 1;
    }

    private static ResourceLocation resolvePlanSetId(CommandContext<CommandSourceStack> ctx) {
        ResourceLocation converted;
        String path;
        int idx;
        ResourceLocation id;
        ResourceLocation raw = ResourceLocationArgument.getId(ctx, (String)"plan_set");
        ResourceLocation resourceLocation = id = raw.getNamespace().equals("minecraft") ? ResourceLocation.fromNamespaceAndPath((String)"millenaire", (String)raw.getPath()) : raw;
        if (ModCultures.getBuildingPlanSet(id) == null && (idx = (path = id.getPath()).indexOf(95)) > 0 && ModCultures.getBuildingPlanSet(converted = ResourceLocation.fromNamespaceAndPath((String)id.getNamespace(), (String)(path.substring(0, idx) + "/" + path.substring(idx + 1)))) != null) {
            return converted;
        }
        return id;
    }

    private static Rotation parseRotation(String arg) {
        return switch (arg.toLowerCase()) {
            case "none" -> Rotation.NONE;
            case "clockwise_90", "cw90", "90" -> Rotation.CLOCKWISE_90;
            case "clockwise_180", "180" -> Rotation.CLOCKWISE_180;
            case "counterclockwise_90", "ccw90", "270" -> Rotation.COUNTERCLOCKWISE_90;
            default -> null;
        };
    }

    private static String getOptionalString(CommandContext<CommandSourceStack> ctx, String name) {
        try {
            return StringArgumentType.getString(ctx, (String)name);
        }
        catch (IllegalArgumentException e) {
            return null;
        }
    }

    private static int getOptionalInt(CommandContext<CommandSourceStack> ctx, String name, int defaultValue) {
        try {
            return IntegerArgumentType.getInteger(ctx, (String)name);
        }
        catch (IllegalArgumentException e) {
            return defaultValue;
        }
    }

    private static boolean getOptionalBool(CommandContext<CommandSourceStack> ctx, String name, boolean defaultValue) {
        try {
            return BoolArgumentType.getBool(ctx, (String)name);
        }
        catch (IllegalArgumentException e) {
            return defaultValue;
        }
    }
}

