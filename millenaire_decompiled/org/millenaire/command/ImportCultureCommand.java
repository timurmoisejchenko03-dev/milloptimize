/*
 * Decompiled with CFR 0.150.
 * 
 * Could not load the following classes:
 *  com.mojang.brigadier.arguments.ArgumentType
 *  com.mojang.brigadier.arguments.IntegerArgumentType
 *  com.mojang.brigadier.arguments.StringArgumentType
 *  com.mojang.brigadier.builder.LiteralArgumentBuilder
 *  com.mojang.brigadier.builder.RequiredArgumentBuilder
 *  com.mojang.brigadier.context.CommandContext
 *  com.mojang.brigadier.suggestion.SuggestionProvider
 *  com.mojang.brigadier.suggestion.SuggestionsBuilder
 *  com.mojang.logging.LogUtils
 *  javax.annotation.Nullable
 *  net.minecraft.commands.CommandSourceStack
 *  net.minecraft.commands.Commands
 *  net.minecraft.commands.SharedSuggestionProvider
 *  net.minecraft.core.BlockPos
 *  net.minecraft.core.Direction
 *  net.minecraft.network.chat.Component
 *  net.minecraft.resources.ResourceLocation
 *  net.minecraft.server.level.ServerLevel
 *  net.minecraft.server.level.ServerPlayer
 *  net.minecraft.world.level.block.entity.BlockEntity
 *  net.minecraft.world.level.block.state.BlockState
 *  net.neoforged.neoforge.common.util.FakePlayerFactory
 *  org.slf4j.Logger
 */
package org.millenaire.command;

import com.mojang.brigadier.arguments.ArgumentType;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.builder.RequiredArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.suggestion.SuggestionProvider;
import com.mojang.brigadier.suggestion.SuggestionsBuilder;
import com.mojang.logging.LogUtils;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import javax.annotation.Nullable;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.common.util.FakePlayerFactory;
import org.millenaire.block.ImportTableBlock;
import org.millenaire.block.ImportTableBlockEntity;
import org.millenaire.block.ModBlocks;
import org.millenaire.building.BuildingImporter;
import org.millenaire.building.BuildingPlan;
import org.millenaire.building.BuildingPlanSet;
import org.millenaire.building.ImportTablePlanResolver;
import org.millenaire.culture.ModCultures;
import org.millenaire.world.TerrainPreparer;
import org.slf4j.Logger;

public final class ImportCultureCommand {
    private static final Logger LOGGER = LogUtils.getLogger();
    private static final int BUILDING_GROUP_SPACING = 5;
    private static final SuggestionProvider<CommandSourceStack> CULTURE_SUGGESTIONS = (ctx, builder) -> SharedSuggestionProvider.suggest(ModCultures.getAllCultures().keySet().stream().map(ResourceLocation::getPath), (SuggestionsBuilder)builder);

    private ImportCultureCommand() {
    }

    public static void registerUnder(LiteralArgumentBuilder<CommandSourceStack> parent) {
        parent.then(Commands.literal((String)"importculture").then(((RequiredArgumentBuilder)Commands.argument((String)"culture", (ArgumentType)StringArgumentType.string()).suggests(CULTURE_SUGGESTIONS).executes(ImportCultureCommand::execute)).then(Commands.argument((String)"x", (ArgumentType)IntegerArgumentType.integer()).then(Commands.argument((String)"z", (ArgumentType)IntegerArgumentType.integer()).executes(ImportCultureCommand::execute)))));
    }

    private static int execute(CommandContext<CommandSourceStack> ctx) {
        CommandSourceStack source = (CommandSourceStack)ctx.getSource();
        ServerPlayer player = ImportCultureCommand.resolvePlayer(ctx);
        String cultureArg = StringArgumentType.getString(ctx, (String)"culture");
        ResourceLocation cultureId = ImportCultureCommand.resolveCultureId(cultureArg);
        if (ModCultures.getCulture(cultureId) == null) {
            source.sendFailure((Component)Component.literal((String)("Unknown culture: " + cultureArg + ". Available: " + String.valueOf(ModCultures.getAllCultures().keySet().stream().map(ResourceLocation::getPath).sorted().toList()))));
            return 0;
        }
        List<BuildingPlanSet> planSets = ModCultures.getAllBuildingPlanSets().values().stream().filter(ps -> cultureId.equals((Object)ps.culture())).filter(ps -> !ps.isSubBuilding()).filter(ps -> !ps.tags().contains("marvel")).sorted(Comparator.comparing(BuildingPlanSet::buildingId)).toList();
        if (planSets.isEmpty()) {
            source.sendFailure((Component)Component.literal((String)("No top-level building plan sets found for culture: " + String.valueOf((Object)cultureId))));
            return 0;
        }
        ServerLevel level = player.serverLevel();
        BlockPos startPos = ImportCultureCommand.resolveStartPos(ctx, player, level);
        int nextBuildingMinX = startPos.getX();
        int importedBuildings = 0;
        int importedPlans = 0;
        int failedCount = 0;
        int importedSubs = 0;
        int importedSubTables = 0;
        HashSet<String> placedSubs = new HashSet<String>();
        ArrayList<String> fromExportsNames = new ArrayList<String>();
        for (BuildingPlanSet planSet : planSets) {
            BuildingPlan firstPlan = ImportCultureCommand.resolveFirstPlan(planSet);
            if (firstPlan == null) {
                source.sendSystemMessage((Component)Component.literal((String)("\u00a7c[ImportCulture] No usable plan for " + String.valueOf((Object)planSet.id()))));
                ++failedCount;
                continue;
            }
            BlockPos tablePos = BuildingImporter.tablePosForBuildingAabb(nextBuildingMinX, startPos.getZ(), startPos.getY());
            ImportTableBlockEntity table = ImportCultureCommand.placeImportTable(level, tablePos, player.getDirection());
            if (table == null) {
                source.sendSystemMessage((Component)Component.literal((String)("\u00a7c[ImportCulture] Failed to create ImportTable for " + String.valueOf((Object)planSet.id()) + " at " + tablePos.toShortString())));
                ++failedCount;
                nextBuildingMinX += 5;
                continue;
            }
            source.sendSystemMessage((Component)Component.literal((String)("\u00a7f[ImportCulture] Importing " + String.valueOf((Object)planSet.id()))));
            BuildingImporter.ImportAabb aabb = BuildingImporter.importAllVariantsFromCulture(level, table, player, cultureId.toString(), planSet.buildingId());
            if (aabb != null) {
                ++importedBuildings;
                importedPlans += ImportCultureCommand.countPlans(planSet);
                if (ImportTablePlanResolver.isServedFromExports(cultureId.toString(), planSet.buildingId())) {
                    fromExportsNames.add(planSet.buildingId());
                }
                SubBandResult band = ImportCultureCommand.placeSubBuildingBand(level, player, cultureId, planSet, aabb, startPos.getY(), placedSubs);
                importedSubs += band.subsPlaced();
                importedSubTables += band.subTablesPlaced();
                failedCount += band.failed();
                nextBuildingMinX = aabb.maxX() + 1 + 5;
                continue;
            }
            ++failedCount;
            nextBuildingMinX += 5;
        }
        int importedBuildingsFinal = importedBuildings;
        int importedPlansFinal = importedPlans;
        int failedFinal = failedCount;
        int importedSubsFinal = importedSubs;
        int importedSubTablesFinal = importedSubTables;
        source.sendSuccess(() -> Component.literal((String)("Imported culture " + cultureId.getPath() + " (buildings=" + importedBuildingsFinal + "/" + planSets.size() + ", plans=" + importedPlansFinal + ", subs=" + importedSubsFinal + ", subTables=" + importedSubTablesFinal + ", failed=" + failedFinal + ")")), true);
        ImportTablePlanResolver.formatFromExportsMessage("ImportCulture", fromExportsNames).ifPresent(msg -> source.sendSystemMessage((Component)Component.literal((String)msg)));
        return 1;
    }

    private static BlockPos resolveStartPos(CommandContext<CommandSourceStack> ctx, ServerPlayer player, ServerLevel level) {
        Integer x = ImportCultureCommand.getOptionalInt(ctx, "x");
        Integer z = ImportCultureCommand.getOptionalInt(ctx, "z");
        if (x == null || z == null) {
            BlockPos inFront = player.blockPosition().relative(player.getDirection(), 2);
            int y = TerrainPreparer.getGroundHeight(level, inFront.getX(), inFront.getZ());
            return new BlockPos(inFront.getX(), y, inFront.getZ());
        }
        int y = TerrainPreparer.getGroundHeight(level, x, z);
        return new BlockPos(x.intValue(), y, z.intValue());
    }

    private static ImportTableBlockEntity placeImportTable(ServerLevel level, BlockPos pos, Direction playerDirection) {
        Direction facing = playerDirection.getOpposite();
        level.setBlock(pos, (BlockState)((ImportTableBlock)((Object)ModBlocks.IMPORT_TABLE.get())).defaultBlockState().setValue(ImportTableBlock.FACING, (Comparable)facing), 2);
        BlockEntity blockEntity = level.getBlockEntity(pos);
        if (blockEntity instanceof ImportTableBlockEntity) {
            ImportTableBlockEntity table = (ImportTableBlockEntity)blockEntity;
            table.setBuildingId("");
            table.setCultureKey("");
            table.setOrientation(0);
            table.setImportMockBlocks(true);
            return table;
        }
        return null;
    }

    private static SubBandResult placeSubBuildingBand(ServerLevel level, ServerPlayer player, ResourceLocation cultureId, BuildingPlanSet parent, BuildingImporter.ImportAabb parentAabb, int startY, Set<String> placedSubs) {
        String cultureKey = cultureId.toString();
        List<BuildingImporter.SubRef> refs = BuildingImporter.listReferencedSubs(cultureKey, parent.buildingId());
        if (refs.isEmpty()) {
            return SubBandResult.EMPTY;
        }
        Optional<ImportTablePlanResolver.PlanView> parentPlan = ImportTablePlanResolver.resolvePlan(cultureKey, parent.buildingId());
        int subMinX = parentAabb.minX();
        int nextSubMinZ = parentAabb.maxZ() + 1 + 5;
        int subsPlaced = 0;
        int subTablesPlaced = 0;
        int failed = 0;
        for (BuildingImporter.SubRef ref : refs) {
            if (placedSubs.contains(ref.subId())) continue;
            Optional<ImportTablePlanResolver.PlanView> subPlan = ImportTablePlanResolver.resolvePlan(cultureKey, ref.subId());
            if (subPlan.isEmpty() || !BuildingImporter.readIsSubBuildingFlag(cultureKey, ref.subId())) {
                LOGGER.warn("ImportCulture: sub '{}' referenced by '{}' is missing or not a sub-building \u2014 skipped", (Object)ref.subId(), (Object)parent.buildingId());
                ++failed;
                continue;
            }
            Map<String, ImportTablePlanResolver.VariantView> subVariants = subPlan.get().variants();
            if (subVariants.isEmpty()) {
                LOGGER.warn("ImportCulture: sub '{}' has no variants \u2014 skipped", (Object)ref.subId());
                ++failed;
                continue;
            }
            String subVariant = subVariants.keySet().iterator().next();
            ImportTablePlanResolver.VariantView vv = subVariants.get(subVariant);
            if (vv == null || vv.levels().isEmpty()) {
                LOGGER.warn("ImportCulture: sub '{}' variant '{}' has no levels \u2014 skipped", (Object)ref.subId(), (Object)subVariant);
                ++failed;
                continue;
            }
            List sortedLevels = vv.levels().keySet().stream().sorted().toList();
            String parentVariant = ref.parentVariant().isEmpty() ? ImportCultureCommand.firstVariant(parent) : ref.parentVariant();
            int parentDepth = ImportCultureCommand.parentDepthForVariant(parentPlan, parentVariant);
            boolean anyLevelPlaced = false;
            Iterator iterator = sortedLevels.iterator();
            while (iterator.hasNext()) {
                int levelNum = (Integer)iterator.next();
                BlockPos tablePos = BuildingImporter.tablePosForBuildingAabb(subMinX, nextSubMinZ, startY);
                ImportTableBlockEntity subTable = ImportCultureCommand.placeImportTable(level, tablePos, player.getDirection());
                if (subTable == null) {
                    player.sendSystemMessage((Component)Component.literal((String)("\u00a7c[ImportCulture] Failed to create ImportTable for sub " + ref.subId() + " at " + tablePos.toShortString())));
                    ++failed;
                    nextSubMinZ += 5;
                    continue;
                }
                BuildingImporter.importLevelFromCulture(level, subTable, player, cultureKey, ref.subId(), subVariant, levelNum, parent.buildingId(), parentVariant);
                if (!ref.subId().equals(subTable.getBuildingId())) {
                    LOGGER.warn("ImportCulture: sub '{}' under parent '{}/{}' rejected by importer at L{} (trigger unresolved or template missing) \u2014 skipped", new Object[]{ref.subId(), parent.buildingId(), parentVariant, levelNum});
                    ++failed;
                    nextSubMinZ += 5;
                    continue;
                }
                int compositeDepth = Math.max(subTable.getLength(), parentDepth);
                nextSubMinZ += compositeDepth + 5;
                ++subTablesPlaced;
                anyLevelPlaced = true;
            }
            if (!anyLevelPlaced) continue;
            placedSubs.add(ref.subId());
            ++subsPlaced;
        }
        return new SubBandResult(subsPlaced, subTablesPlaced, failed);
    }

    private static int parentDepthForVariant(Optional<ImportTablePlanResolver.PlanView> parentPlan, String variant) {
        if (parentPlan.isEmpty()) {
            return 0;
        }
        ImportTablePlanResolver.VariantView vv = parentPlan.get().variants().get(variant);
        if (vv == null) {
            return 0;
        }
        return vv.levels().values().stream().mapToInt(ImportTablePlanResolver.LevelView::depth).max().orElse(0);
    }

    private static String firstVariant(BuildingPlanSet planSet) {
        return planSet.variants().keySet().stream().sorted().findFirst().orElse("a");
    }

    private static int countPlans(BuildingPlanSet planSet) {
        return planSet.variants().values().stream().mapToInt(List::size).sum();
    }

    @Nullable
    private static BuildingPlan resolveFirstPlan(BuildingPlanSet planSet) {
        List variants = planSet.variants().keySet().stream().sorted().toList();
        for (String variantKey : variants) {
            BuildingPlan plan;
            BuildingPlanSet.LevelDef first;
            List<BuildingPlanSet.LevelDef> levels = planSet.variants().get(variantKey);
            if (levels == null || levels.isEmpty() || (first = (BuildingPlanSet.LevelDef)levels.stream().min(Comparator.comparingInt(BuildingPlanSet.LevelDef::level)).orElse(null)) == null || (plan = ModCultures.getBuildingPlan(first.planId())) == null) continue;
            return plan;
        }
        return null;
    }

    private static ResourceLocation resolveCultureId(String arg) {
        if (arg.contains(":")) {
            ResourceLocation parsed = ResourceLocation.tryParse((String)arg);
            return parsed != null ? parsed : ResourceLocation.fromNamespaceAndPath((String)"millenaire", (String)arg);
        }
        return ResourceLocation.fromNamespaceAndPath((String)"millenaire", (String)arg);
    }

    private static ServerPlayer resolvePlayer(CommandContext<CommandSourceStack> ctx) {
        ServerPlayer player = ((CommandSourceStack)ctx.getSource()).getPlayer();
        if (player != null) {
            return player;
        }
        return FakePlayerFactory.getMinecraft((ServerLevel)((CommandSourceStack)ctx.getSource()).getLevel());
    }

    private static Integer getOptionalInt(CommandContext<CommandSourceStack> ctx, String name) {
        try {
            return IntegerArgumentType.getInteger(ctx, (String)name);
        }
        catch (IllegalArgumentException e) {
            return null;
        }
    }

    private record SubBandResult(int subsPlaced, int subTablesPlaced, int failed) {
        static final SubBandResult EMPTY = new SubBandResult(0, 0, 0);
    }
}

