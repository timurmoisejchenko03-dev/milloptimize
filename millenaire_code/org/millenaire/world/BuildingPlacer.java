/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.mojang.logging.LogUtils
 *  javax.annotation.Nullable
 *  net.minecraft.core.BlockPos
 *  net.minecraft.core.Direction
 *  net.minecraft.core.Direction$Plane
 *  net.minecraft.core.HolderGetter
 *  net.minecraft.core.Vec3i
 *  net.minecraft.core.registries.Registries
 *  net.minecraft.nbt.CompoundTag
 *  net.minecraft.nbt.ListTag
 *  net.minecraft.nbt.NbtUtils
 *  net.minecraft.server.level.ServerLevel
 *  net.minecraft.tags.BlockTags
 *  net.minecraft.world.entity.Entity
 *  net.minecraft.world.entity.item.ItemEntity
 *  net.minecraft.world.item.DyeColor
 *  net.minecraft.world.level.BlockGetter
 *  net.minecraft.world.level.Level
 *  net.minecraft.world.level.LevelAccessor
 *  net.minecraft.world.level.ServerLevelAccessor
 *  net.minecraft.world.level.block.AbstractFurnaceBlock
 *  net.minecraft.world.level.block.BedBlock
 *  net.minecraft.world.level.block.Block
 *  net.minecraft.world.level.block.Blocks
 *  net.minecraft.world.level.block.CarpetBlock
 *  net.minecraft.world.level.block.ChestBlock
 *  net.minecraft.world.level.block.DoorBlock
 *  net.minecraft.world.level.block.FenceBlock
 *  net.minecraft.world.level.block.IronBarsBlock
 *  net.minecraft.world.level.block.Rotation
 *  net.minecraft.world.level.block.SlabBlock
 *  net.minecraft.world.level.block.StairBlock
 *  net.minecraft.world.level.block.WallBlock
 *  net.minecraft.world.level.block.WallTorchBlock
 *  net.minecraft.world.level.block.state.BlockState
 *  net.minecraft.world.level.block.state.properties.BedPart
 *  net.minecraft.world.level.block.state.properties.BlockStateProperties
 *  net.minecraft.world.level.block.state.properties.BooleanProperty
 *  net.minecraft.world.level.block.state.properties.ChestType
 *  net.minecraft.world.level.block.state.properties.DoubleBlockHalf
 *  net.minecraft.world.level.block.state.properties.Half
 *  net.minecraft.world.level.block.state.properties.Property
 *  net.minecraft.world.level.block.state.properties.SlabType
 *  net.minecraft.world.level.block.state.properties.StairsShape
 *  net.minecraft.world.level.block.state.properties.WallSide
 *  net.minecraft.world.level.levelgen.structure.templatesystem.StructurePlaceSettings
 *  net.minecraft.world.level.levelgen.structure.templatesystem.StructureProcessor
 *  net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplate
 *  net.minecraft.world.phys.AABB
 *  org.slf4j.Logger
 */
package org.millenaire.world;

import com.mojang.logging.LogUtils;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import javax.annotation.Nullable;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderGetter;
import net.minecraft.core.Vec3i;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.ServerLevelAccessor;
import net.minecraft.world.level.block.AbstractFurnaceBlock;
import net.minecraft.world.level.block.BedBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.CarpetBlock;
import net.minecraft.world.level.block.ChestBlock;
import net.minecraft.world.level.block.DoorBlock;
import net.minecraft.world.level.block.FenceBlock;
import net.minecraft.world.level.block.IronBarsBlock;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.SlabBlock;
import net.minecraft.world.level.block.StairBlock;
import net.minecraft.world.level.block.WallBlock;
import net.minecraft.world.level.block.WallTorchBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BedPart;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.ChestType;
import net.minecraft.world.level.block.state.properties.DoubleBlockHalf;
import net.minecraft.world.level.block.state.properties.Half;
import net.minecraft.world.level.block.state.properties.Property;
import net.minecraft.world.level.block.state.properties.SlabType;
import net.minecraft.world.level.block.state.properties.StairsShape;
import net.minecraft.world.level.block.state.properties.WallSide;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructurePlaceSettings;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureProcessor;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplate;
import net.minecraft.world.phys.AABB;
import org.millenaire.block.BlockMillCrops;
import org.millenaire.block.IPaintedBlock;
import org.millenaire.block.MillPathBlock;
import org.millenaire.block.MillPathSlabBlock;
import org.millenaire.block.ModBlocks;
import org.millenaire.block.PaintedBrickBlock;
import org.millenaire.block.PaintedBrickSlabBlock;
import org.millenaire.block.PaintedBrickStairBlock;
import org.millenaire.block.PaintedBrickWallBlock;
import org.millenaire.block.mock.FacingMarkerType;
import org.millenaire.block.mock.MarkerType;
import org.millenaire.block.mock.MockBlock;
import org.millenaire.block.mock.MockChestBlock;
import org.millenaire.block.mock.MockFacingMarkerBlock;
import org.millenaire.block.mock.MockMarkerBlock;
import org.millenaire.building.BuildingInstance;
import org.millenaire.building.BuildingPlan;
import org.millenaire.building.ClearMargins;
import org.millenaire.building.NbtPaletteHelper;
import org.millenaire.building.PlacementPhase;
import org.millenaire.building.PlacementStep;
import org.millenaire.building.SpecialPoint;
import org.millenaire.building.TemplateLoader;
import org.millenaire.entity.MillWallDecoration;
import org.millenaire.entity.WallDecorationType;
import org.millenaire.tag.ModTags;
import org.millenaire.world.MockBlockProcessor;
import org.millenaire.world.TerrainPreparer;
import org.millenaire.world.TerrainTraversal;
import org.slf4j.Logger;

public final class BuildingPlacer {
    private static final Logger LOGGER = LogUtils.getLogger();
    private static final int PART_SIZE = 8;

    private BuildingPlacer() {
    }

    public static boolean placeInstantly(ServerLevel level, BuildingPlan plan, BlockPos origin, Rotation rotation) {
        return BuildingPlacer.placeInstantly(level, plan, origin, rotation, true, false, null);
    }

    public static boolean placeInstantly(ServerLevel level, BuildingPlan plan, BlockPos origin, Rotation rotation, @Nullable BuildingInstance buildingInstance) {
        boolean result = BuildingPlacer.placeInstantly(level, plan, origin, rotation, true, false, buildingInstance);
        if (result && buildingInstance != null && buildingInstance.getBrickColourMapping() != null) {
            BuildingPlacer.remapPlacedPaintedBricks(level, plan, origin, rotation, buildingInstance);
        }
        return result;
    }

    public static boolean placeUpgradeInstantly(ServerLevel level, BuildingPlan plan, BlockPos origin, Rotation rotation) {
        return BuildingPlacer.placeUpgradeInstantly(level, plan, origin, rotation, null);
    }

    public static boolean placeUpgradeInstantly(ServerLevel level, BuildingPlan plan, BlockPos origin, Rotation rotation, @Nullable BuildingInstance buildingInstance) {
        boolean result = BuildingPlacer.placeInstantly(level, plan, origin, rotation, true, true, buildingInstance);
        if (result && buildingInstance != null && buildingInstance.getBrickColourMapping() != null) {
            BuildingPlacer.remapPlacedPaintedBricks(level, plan, origin, rotation, buildingInstance);
        }
        return result;
    }

    public static boolean placeUpgradeInstantly(ServerLevel level, BuildingPlan plan, BlockPos origin, Rotation rotation, boolean processMockBlocks) {
        return BuildingPlacer.placeInstantly(level, plan, origin, rotation, processMockBlocks, true, null);
    }

    public static boolean placeInstantly(ServerLevel level, BuildingPlan plan, BlockPos origin, Rotation rotation, boolean processMockBlocks) {
        return BuildingPlacer.placeInstantly(level, plan, origin, rotation, processMockBlocks, false, null);
    }

    public static boolean placeFromTemplate(ServerLevel level, StructureTemplate template, BlockPos origin, Rotation rotation, boolean processMockBlocks) {
        StructurePlaceSettings settings = new StructurePlaceSettings().setRotation(rotation);
        if (processMockBlocks) {
            settings.addProcessor((StructureProcessor)MockBlockProcessor.INSTANCE);
        }
        StructurePlaceSettings extractSettings = new StructurePlaceSettings().setRotation(rotation);
        List<PlacementStep> allSteps = BuildingPlacer.extractBlocksFromNbt(template, extractSettings, level);
        BuildingPlacer.placePreserveGroundFromSteps(level, allSteps, origin);
        template.placeInWorld((ServerLevelAccessor)level, origin, origin, settings, level.getRandom(), 2);
        BuildingPlacer.fixWaterlogging(level, allSteps, origin);
        for (PlacementStep step : allSteps) {
            BlockPos worldPos = step.relativePos().offset((Vec3i)origin);
            BlockState placed = level.getBlockState(worldPos);
            BlockState fixed = BuildingPlacer.applyFacingGuess((Level)level, worldPos, placed, step.guessChestFacing(), step.guessTorchFacing(), step.guessFurnaceFacing());
            if ((fixed = BuildingPlacer.stabilizePathBlock(fixed)) == placed) continue;
            level.setBlock(worldPos, fixed, 2);
        }
        BuildingPlacer.fixDoubleChests(level, allSteps, origin);
        BuildingPlacer.generateBedFeet(level, allSteps, origin, null);
        return true;
    }

    public static boolean placeUpgradeFromTemplate(ServerLevel level, StructureTemplate template, BlockPos origin, Rotation rotation, boolean processMockBlocks) {
        BlockPos worldPos;
        StructurePlaceSettings settings = new StructurePlaceSettings().setRotation(rotation);
        if (processMockBlocks) {
            settings.addProcessor((StructureProcessor)MockBlockProcessor.INSTANCE);
        }
        StructurePlaceSettings extractSettings = new StructurePlaceSettings().setRotation(rotation);
        List<PlacementStep> allSteps = BuildingPlacer.extractBlocksFromNbt(template, extractSettings, level);
        HashSet<BlockPos> preserveGroundPositions = new HashSet<BlockPos>();
        for (PlacementStep step : allSteps) {
            if (!BuildingPlacer.isPreserveGroundStep(step)) continue;
            preserveGroundPositions.add(step.relativePos());
        }
        BuildingPlacer.placePreserveGroundFromSteps(level, allSteps, origin);
        template.placeInWorld((ServerLevelAccessor)level, origin, origin, settings, level.getRandom(), 2);
        for (PlacementStep step : allSteps) {
            if (step.phase() != PlacementPhase.DELETION || preserveGroundPositions.contains(step.relativePos())) continue;
            worldPos = step.relativePos().offset((Vec3i)origin);
            level.setBlock(worldPos, Blocks.AIR.defaultBlockState(), 2);
        }
        BuildingPlacer.fixWaterlogging(level, allSteps, origin);
        for (PlacementStep step : allSteps) {
            worldPos = step.relativePos().offset((Vec3i)origin);
            BlockState placed = level.getBlockState(worldPos);
            BlockState fixed = BuildingPlacer.applyFacingGuess((Level)level, worldPos, placed, step.guessChestFacing(), step.guessTorchFacing(), step.guessFurnaceFacing());
            if ((fixed = BuildingPlacer.stabilizePathBlock(fixed)) == placed) continue;
            level.setBlock(worldPos, fixed, 2);
        }
        BuildingPlacer.fixDoubleChests(level, allSteps, origin);
        BuildingPlacer.generateBedFeet(level, allSteps, origin, null);
        return true;
    }

    private static void placePreserveGroundFromSteps(ServerLevel level, List<PlacementStep> steps, BlockPos origin) {
        for (PlacementStep step : steps) {
            if (!BuildingPlacer.isPreserveGroundStep(step)) continue;
            BlockPos worldPos = step.relativePos().offset((Vec3i)origin);
            MarkerType markerType = (MarkerType)((Object)step.blockState().getValue(MockMarkerBlock.TYPE));
            String subtype = markerType.specialPointSubtype();
            BlockState ground = BuildingPlacer.resolveGroundBlock(level, worldPos, subtype, true);
            level.setBlock(worldPos, ground, 3);
        }
    }

    private static boolean isPreserveGroundStep(PlacementStep step) {
        if (!step.blockState().is((Block)ModBlocks.MOCK_MARKER.get())) {
            return false;
        }
        if (!step.blockState().hasProperty(MockMarkerBlock.TYPE)) {
            return false;
        }
        MarkerType type = (MarkerType)((Object)step.blockState().getValue(MockMarkerBlock.TYPE));
        return type.specialPointSubtype() != null;
    }

    private static boolean isPreserveGroundMarker(MarkerType type) {
        return type.specialPointSubtype() != null;
    }

    private static boolean placeInstantly(ServerLevel level, BuildingPlan plan, BlockPos origin, Rotation rotation, boolean processMockBlocks, boolean isUpgrade, @Nullable BuildingInstance building) {
        BlockPos worldPos;
        Optional<StructureTemplate> opt = TemplateLoader.load(plan, level, TemplateLoader.cultureFsFor(plan));
        if (opt.isEmpty()) {
            LOGGER.warn("Template introuvable : {}", (Object)plan.nbtPath());
            return false;
        }
        StructureTemplate template = opt.get();
        StructurePlaceSettings settings = new StructurePlaceSettings().setRotation(rotation);
        if (processMockBlocks) {
            settings.addProcessor((StructureProcessor)MockBlockProcessor.INSTANCE);
        }
        StructurePlaceSettings extractSettings = new StructurePlaceSettings().setRotation(rotation);
        List<PlacementStep> allSteps = BuildingPlacer.extractBlocksFromNbt(template, extractSettings, level);
        BuildingPlacer.placePreserveGround(level, plan, origin, rotation);
        template.placeInWorld((ServerLevelAccessor)level, origin, origin, settings, level.getRandom(), 2);
        HashSet<BlockPos> preserveGroundPositions = new HashSet<BlockPos>();
        for (SpecialPoint sp : plan.specialPoints()) {
            if (!sp.isType("preserve_ground")) continue;
            BlockPos rotated = StructureTemplate.calculateRelativePosition((StructurePlaceSettings)extractSettings, (BlockPos)sp.pos());
            preserveGroundPositions.add(rotated);
        }
        for (PlacementStep step : allSteps) {
            if (step.phase() != PlacementPhase.DELETION || preserveGroundPositions.contains(step.relativePos())) continue;
            worldPos = step.relativePos().offset((Vec3i)origin);
            level.setBlock(worldPos, Blocks.AIR.defaultBlockState(), 2);
        }
        BuildingPlacer.fixWaterlogging(level, allSteps, origin);
        for (PlacementStep step : allSteps) {
            worldPos = step.relativePos().offset((Vec3i)origin);
            BlockState placed = level.getBlockState(worldPos);
            BlockState fixed = BuildingPlacer.applyFacingGuess((Level)level, worldPos, placed, step.guessChestFacing(), step.guessTorchFacing(), step.guessFurnaceFacing());
            if ((fixed = BuildingPlacer.stabilizePathBlock(fixed)) == placed) continue;
            level.setBlock(worldPos, fixed, 2);
        }
        BuildingPlacer.fixDoubleChests(level, allSteps, origin);
        BuildingPlacer.generateBedFeet(level, allSteps, origin, building);
        BuildingPlacer.refreshDependentConnections(level, allSteps, origin);
        BuildingPlacer.cleanupDroppedItems(level, origin, plan.width(), plan.height(), plan.depth(), rotation);
        if (processMockBlocks) {
            BuildingPlacer.spawnWallDecorations(level, plan, origin, rotation);
        }
        return true;
    }

    public static List<PlacementStep> compilePlacementSteps(ServerLevel level, BuildingPlan plan, BlockPos origin, Rotation rotation) {
        Optional<StructureTemplate> opt = TemplateLoader.load(plan, level, TemplateLoader.cultureFsFor(plan));
        if (opt.isEmpty()) {
            LOGGER.warn("Template introuvable : {}", (Object)plan.nbtPath());
            return List.of();
        }
        StructureTemplate template = opt.get();
        StructurePlaceSettings settings = new StructurePlaceSettings().setRotation(rotation);
        List<PlacementStep> steps = BuildingPlacer.extractBlocksFromNbt(template, settings, level);
        steps.removeIf(s -> s.blockState().getBlock() instanceof DoorBlock && s.blockState().hasProperty((Property)DoorBlock.HALF) && s.blockState().getValue((Property)DoorBlock.HALF) == DoubleBlockHalf.UPPER);
        steps.removeIf(BuildingPlacer::isPreserveGroundStep);
        int groundLevel = plan.groundLevel();
        steps.sort(Comparator.comparingInt(s -> s.phase().ordinal()).thenComparingInt(s -> BuildingPlacer.subSurfaceGroup(s, groundLevel)).thenComparingInt(s -> BuildingPlacer.subSurfaceYKey(s, groundLevel)).thenComparing(BuildingPlacer.sPatternXZComparator()));
        List<PlacementStep> groundSteps = BuildingPlacer.compilePreserveGroundSteps(level, plan, origin, rotation);
        if (!groundSteps.isEmpty()) {
            HashSet<BlockPos> preserveGroundPositions = new HashSet<BlockPos>();
            for (PlacementStep gs : groundSteps) {
                preserveGroundPositions.add(gs.relativePos());
            }
            steps.removeIf(s -> s.phase() == PlacementPhase.DELETION && preserveGroundPositions.contains(s.relativePos()));
            groundSteps.addAll(steps);
            return groundSteps;
        }
        return steps;
    }

    public static List<PlacementStep> compilePlacementSteps(ServerLevel level, BuildingPlan plan, BlockPos origin, Rotation rotation, @Nullable BuildingInstance buildingInstance) {
        List<PlacementStep> steps = BuildingPlacer.compilePlacementSteps(level, plan, origin, rotation);
        if (buildingInstance != null && buildingInstance.getBrickColourMapping() != null) {
            steps = BuildingPlacer.remapPaintedBricks(steps, buildingInstance);
        }
        return steps;
    }

    private static List<PlacementStep> remapPaintedBricks(List<PlacementStep> steps, BuildingInstance instance) {
        ArrayList<PlacementStep> result = new ArrayList<PlacementStep>(steps.size());
        for (PlacementStep step : steps) {
            BlockState newState;
            IPaintedBlock painted;
            DyeColor remapped;
            BlockState state = step.blockState();
            Block block = state.getBlock();
            if (block instanceof IPaintedBlock && (remapped = instance.remapBrickColour((painted = (IPaintedBlock)block).getColor())) != painted.getColor() && (newState = BuildingPlacer.getRemappedState(state, painted, remapped)) != null) {
                result.add(new PlacementStep(step.relativePos(), newState, step.phase(), step.guessChestFacing()));
                continue;
            }
            result.add(step);
        }
        return result;
    }

    public static List<PlacementStep> compileTerrainPrepSteps(ServerLevel level, BlockPos origin, int width, int height, int depth, int baseY, BlockPos buildingOrigin, Rotation rotation, int groundLevel, ClearMargins margins) {
        int effWidth = TerrainPreparer.effectiveWidth(width, depth, rotation);
        int effDepth = TerrainPreparer.effectiveDepth(width, depth, rotation);
        BlockPos effOrigin = TerrainPreparer.effectiveOrigin(origin, width, depth, rotation);
        ClearMargins effective = margins.forRotation(rotation);
        ArrayList<PlacementStep> clearSteps = new ArrayList<PlacementStep>();
        ArrayList<PlacementStep> fillSteps = new ArrayList<PlacementStep>();
        TerrainTraversal.traverse(level, effOrigin, effWidth, effDepth, height, baseY, groundLevel, effective, (ctx, action) -> {
            BlockPos rel = new BlockPos(ctx.wx() - buildingOrigin.getX(), ctx.y() - buildingOrigin.getY(), ctx.wz() - buildingOrigin.getZ());
            switch (action) {
                case CLEAR_AIR: 
                case CLEAR_TREE: 
                case CLEAR_SUBGROUND: {
                    clearSteps.add(new PlacementStep(rel, Blocks.AIR.defaultBlockState(), PlacementPhase.DELETION));
                    break;
                }
                case FILL_SUBSURFACE: 
                case BORDER_ANTIFLOOD: {
                    fillSteps.add(new PlacementStep(rel, TerrainPreparer.stabilizedFallingBlock(ctx.subSurfaceBlock()), PlacementPhase.DELETION));
                    break;
                }
                case FILL_SURFACE: {
                    fillSteps.add(new PlacementStep(rel, ctx.surfaceBlock(), PlacementPhase.DELETION));
                    break;
                }
                case STABILIZE_FALLING: {
                    fillSteps.add(new PlacementStep(rel, TerrainPreparer.stabilizedFallingBlock(ctx.existing()), PlacementPhase.DELETION));
                }
            }
        });
        clearSteps.sort(Comparator.comparingInt(s -> -s.relativePos().getY()).thenComparing(BuildingPlacer.sPatternXZComparator()));
        fillSteps.sort(Comparator.comparingInt(s -> s.relativePos().getY()).thenComparing(BuildingPlacer.sPatternXZComparator()));
        ArrayList<PlacementStep> result = new ArrayList<PlacementStep>(clearSteps.size() + fillSteps.size());
        result.addAll(clearSteps);
        result.addAll(fillSteps);
        return result;
    }

    private static List<PlacementStep> extractBlocksFromNbt(StructureTemplate template, StructurePlaceSettings settings, ServerLevel level) {
        CompoundTag nbt = template.save(new CompoundTag());
        ArrayList<PlacementStep> steps = new ArrayList<PlacementStep>();
        ArrayList<BlockState> palette = new ArrayList<BlockState>();
        ListTag paletteTag = NbtPaletteHelper.resolvePaletteTag(nbt);
        if (paletteTag != null) {
            for (int i = 0; i < paletteTag.size(); ++i) {
                CompoundTag stateTag = paletteTag.getCompound(i);
                BlockState state = NbtUtils.readBlockState((HolderGetter)level.holderLookup(Registries.BLOCK), (CompoundTag)stateTag);
                palette.add(state);
            }
        }
        if (palette.isEmpty()) {
            return steps;
        }
        ListTag blocksTag = nbt.getList("blocks", 10);
        for (int i = 0; i < blocksTag.size(); ++i) {
            CompoundTag blockEntry = blocksTag.getCompound(i);
            ListTag posTag = blockEntry.getList("pos", 3);
            BlockPos templatePos = new BlockPos(posTag.getInt(0), posTag.getInt(1), posTag.getInt(2));
            int stateIndex = blockEntry.getInt("state");
            if (stateIndex < 0 || stateIndex >= palette.size()) continue;
            BlockState state = (BlockState)palette.get(stateIndex);
            boolean isGuessChest = false;
            boolean isGuessTorch = false;
            boolean isGuessFurnace = false;
            Block block = state.getBlock();
            if (block instanceof MockBlock) {
                BlockState replacement;
                MockBlock mock = (MockBlock)block;
                if (state.getBlock() instanceof MockChestBlock && ((Boolean)state.getValue((Property)MockChestBlock.GUESS)).booleanValue()) {
                    isGuessChest = true;
                }
                if (state.getBlock() instanceof MockMarkerBlock && state.getValue(MockMarkerBlock.TYPE) == MarkerType.TORCH) {
                    isGuessTorch = true;
                }
                if (state.getBlock() instanceof MockFacingMarkerBlock && state.getValue(MockFacingMarkerBlock.TYPE) == FacingMarkerType.FURNACE && ((Boolean)state.getValue((Property)MockFacingMarkerBlock.GUESS)).booleanValue()) {
                    isGuessFurnace = true;
                }
                if ((replacement = mock.getReplacementState(state)) == null || replacement.isAir()) {
                    if (!(state.getBlock() instanceof MockMarkerBlock) || !BuildingPlacer.isPreserveGroundMarker((MarkerType)((Object)state.getValue(MockMarkerBlock.TYPE)))) continue;
                } else {
                    state = replacement;
                }
            }
            BlockPos rotatedPos = StructureTemplate.calculateRelativePosition((StructurePlaceSettings)settings, (BlockPos)templatePos);
            BlockState rotatedState = state.rotate(settings.getRotation());
            PlacementPhase phase = BuildingPlacer.classifyPhase(rotatedState);
            steps.add(new PlacementStep(rotatedPos, rotatedState, phase, isGuessChest, isGuessTorch, isGuessFurnace));
        }
        return steps;
    }

    private static PlacementPhase classifyPhase(BlockState state) {
        if (state.isAir()) {
            return PlacementPhase.DELETION;
        }
        if (state.getBlock() instanceof ChestBlock || state.getBlock() instanceof IronBarsBlock || state.getBlock() instanceof CarpetBlock || state.getBlock() instanceof BlockMillCrops) {
            return PlacementPhase.DEPENDENT;
        }
        if (state.is(ModTags.Blocks.DEPENDENT_BLOCKS) || state.is(ModTags.Blocks.DEPENDENT_VEGETATION)) {
            return PlacementPhase.DEPENDENT;
        }
        if (state.is(BlockTags.DOORS) || state.is(BlockTags.BEDS) || state.is(BlockTags.SIGNS) || state.is(BlockTags.BANNERS) || state.is(BlockTags.RAILS) || state.is(BlockTags.PRESSURE_PLATES) || state.is(BlockTags.BUTTONS) || state.is(BlockTags.FENCE_GATES) || state.is(BlockTags.TRAPDOORS) || state.is(BlockTags.CANDLES) || state.is(BlockTags.FLOWER_POTS)) {
            return PlacementPhase.DEPENDENT;
        }
        return PlacementPhase.STRUCTURE;
    }

    private static void placePreserveGround(ServerLevel level, BuildingPlan plan, BlockPos origin, Rotation rotation) {
        StructurePlaceSettings settings = new StructurePlaceSettings().setRotation(rotation);
        for (SpecialPoint sp : plan.specialPoints()) {
            if (!sp.isType("preserve_ground")) continue;
            BlockPos rotated = StructureTemplate.calculateRelativePosition((StructurePlaceSettings)settings, (BlockPos)sp.pos());
            BlockPos worldPos = rotated.offset((Vec3i)origin);
            BlockState ground = BuildingPlacer.resolveGroundBlock(level, worldPos, sp.subtype(), true);
            level.setBlock(worldPos, ground, 3);
        }
    }

    private static List<PlacementStep> compilePreserveGroundSteps(ServerLevel level, BuildingPlan plan, BlockPos origin, Rotation rotation) {
        StructurePlaceSettings settings = new StructurePlaceSettings().setRotation(rotation);
        ArrayList<PlacementStep> steps = new ArrayList<PlacementStep>();
        for (SpecialPoint sp : plan.specialPoints()) {
            if (!sp.isType("preserve_ground")) continue;
            BlockPos rotated = StructureTemplate.calculateRelativePosition((StructurePlaceSettings)settings, (BlockPos)sp.pos());
            BlockPos worldPos = rotated.offset((Vec3i)origin);
            BlockState ground = BuildingPlacer.resolveGroundBlock(level, worldPos, sp.subtype(), false);
            steps.add(new PlacementStep(rotated, ground, PlacementPhase.STRUCTURE));
        }
        steps.sort(Comparator.comparingInt(s -> s.relativePos().getY()));
        return steps;
    }

    private static BlockState resolveGroundBlock(ServerLevel level, BlockPos worldPos, String subtype, boolean isWorldGen) {
        BlockState below;
        int dy;
        boolean isDepth = "depth".equals(subtype);
        if ("grass".equals(subtype)) {
            return isWorldGen ? Blocks.GRASS_BLOCK.defaultBlockState() : Blocks.DIRT.defaultBlockState();
        }
        BlockState existing = level.getBlockState(worldPos);
        if (BuildingPlacer.isValidGround(existing)) {
            if (isDepth) {
                return existing;
            }
            return BuildingPlacer.adaptToSurface(existing, isWorldGen);
        }
        if ("allbuttrees".equals(subtype) && BuildingPlacer.isTreeBlock(existing)) {
            for (dy = 1; dy < 10; ++dy) {
                below = level.getBlockState(worldPos.below(dy));
                if (!BuildingPlacer.isValidGround(below)) continue;
                return isDepth ? below : BuildingPlacer.adaptToSurface(below, isWorldGen);
            }
        }
        for (dy = 1; dy < 10; ++dy) {
            below = level.getBlockState(worldPos.below(dy));
            if (!BuildingPlacer.isValidGround(below)) continue;
            return isDepth ? below : BuildingPlacer.adaptToSurface(below, isWorldGen);
        }
        return isWorldGen ? Blocks.GRASS_BLOCK.defaultBlockState() : Blocks.DIRT.defaultBlockState();
    }

    private static boolean isValidGround(BlockState state) {
        return state.is(Blocks.GRASS_BLOCK) || state.is(Blocks.DIRT) || state.is(Blocks.COARSE_DIRT) || state.is(Blocks.PODZOL) || state.is(Blocks.MYCELIUM) || state.is(Blocks.SAND) || state.is(Blocks.RED_SAND) || state.is(Blocks.SANDSTONE) || state.is(Blocks.RED_SANDSTONE) || state.is(Blocks.GRAVEL) || state.is(Blocks.STONE) || state.is(Blocks.CLAY) || state.is(Blocks.TERRACOTTA) || state.is(Blocks.SNOW_BLOCK) || state.is(Blocks.MUD);
    }

    private static BlockState adaptToSurface(BlockState ground, boolean isWorldGen) {
        if (ground.is(Blocks.STONE)) {
            return isWorldGen ? Blocks.GRASS_BLOCK.defaultBlockState() : Blocks.DIRT.defaultBlockState();
        }
        if (ground.is(Blocks.SANDSTONE)) {
            return Blocks.SAND.defaultBlockState();
        }
        if (ground.is(Blocks.RED_SANDSTONE)) {
            return Blocks.RED_SAND.defaultBlockState();
        }
        if (ground.is(Blocks.DIRT)) {
            return isWorldGen ? Blocks.GRASS_BLOCK.defaultBlockState() : ground;
        }
        if (ground.is(Blocks.GRASS_BLOCK)) {
            return isWorldGen ? ground : Blocks.DIRT.defaultBlockState();
        }
        return ground;
    }

    private static boolean isTreeBlock(BlockState state) {
        return state.is(BlockTags.LOGS) || state.is(BlockTags.LEAVES);
    }

    private static int subSurfaceGroup(PlacementStep s, int groundLevel) {
        if (s.phase() != PlacementPhase.STRUCTURE || groundLevel >= 0) {
            return 0;
        }
        return s.relativePos().getY() + groundLevel < 0 ? 0 : 1;
    }

    private static int subSurfaceYKey(PlacementStep s, int groundLevel) {
        int y = s.relativePos().getY();
        if (s.phase() == PlacementPhase.DELETION) {
            return -y;
        }
        if (s.phase() == PlacementPhase.STRUCTURE && groundLevel < 0 && y + groundLevel < 0) {
            return -y;
        }
        return y;
    }

    private static BlockState stabilizePathBlock(BlockState state) {
        if (state.getBlock() instanceof MillPathBlock) {
            return (BlockState)state.setValue((Property)MillPathBlock.STABLE, (Comparable)Boolean.valueOf(true));
        }
        if (state.getBlock() instanceof MillPathSlabBlock) {
            return (BlockState)state.setValue((Property)MillPathSlabBlock.STABLE, (Comparable)Boolean.valueOf(true));
        }
        return state;
    }

    public static BlockState computePaneConnections(LevelAccessor level, BlockPos pos, BlockState state) {
        if (!BuildingPlacer.isConnectionBlock(state)) {
            return state;
        }
        for (Direction dir : Direction.Plane.HORIZONTAL) {
            BlockPos neighborPos = pos.relative(dir);
            BlockState neighborState = level.getBlockState(neighborPos);
            state = state.updateShape(dir, neighborState, level, pos, neighborPos);
        }
        return state;
    }

    public static boolean isConnectionBlock(BlockState state) {
        Block block = state.getBlock();
        return block instanceof IronBarsBlock || block instanceof FenceBlock || block instanceof WallBlock || block instanceof StairBlock;
    }

    public static void refreshNeighborConnections(LevelAccessor level, BlockPos placedPos) {
        for (Direction dir : Direction.Plane.HORIZONTAL) {
            BlockState updated;
            BlockPos neighborPos = placedPos.relative(dir);
            BlockState neighborState = level.getBlockState(neighborPos);
            if (!BuildingPlacer.isConnectionBlock(neighborState) || (updated = neighborState.updateShape(dir.getOpposite(), level.getBlockState(placedPos), level, neighborPos, placedPos)) == neighborState) continue;
            level.setBlock(neighborPos, updated, 2);
        }
    }

    public static BlockState applyFacingGuess(Level level, BlockPos pos, BlockState state, boolean isGuessChest, boolean isGuessTorch, boolean isGuessFurnace) {
        if (isGuessFurnace && state.getBlock() instanceof AbstractFurnaceBlock) {
            Direction guessed = BuildingPlacer.guessChestFacing(level, pos, null);
            return (BlockState)state.setValue((Property)AbstractFurnaceBlock.FACING, (Comparable)guessed);
        }
        if (isGuessChest && state.getBlock() instanceof ChestBlock) {
            Direction guessed = BuildingPlacer.guessChestFacing(level, pos, state.getBlock());
            return (BlockState)state.setValue((Property)ChestBlock.FACING, (Comparable)guessed);
        }
        if (isGuessTorch && state.is(Blocks.TORCH)) {
            for (Direction dir : Direction.Plane.HORIZONTAL) {
                BlockPos wallPos = pos.relative(dir.getOpposite());
                BlockState wallState = level.getBlockState(wallPos);
                if (!wallState.isFaceSturdy((BlockGetter)level, wallPos, dir)) continue;
                return (BlockState)Blocks.WALL_TORCH.defaultBlockState().setValue((Property)WallTorchBlock.FACING, (Comparable)dir);
            }
        }
        return state;
    }

    public static BlockState applyFacingGuess(Level level, BlockPos pos, BlockState state, boolean isGuessChest) {
        return BuildingPlacer.applyFacingGuess(level, pos, state, isGuessChest, false, false);
    }

    public static BlockState applyFacingGuess(Level level, BlockPos pos, BlockState state, boolean isGuessChest, boolean isGuessTorch) {
        return BuildingPlacer.applyFacingGuess(level, pos, state, isGuessChest, isGuessTorch, false);
    }

    private static Direction guessChestFacing(Level level, BlockPos pos, @Nullable Block placingBlock) {
        BlockState bsNorth = level.getBlockState(pos.north());
        BlockState bsSouth = level.getBlockState(pos.south());
        BlockState bsWest = level.getBlockState(pos.west());
        BlockState bsEast = level.getBlockState(pos.east());
        if (placingBlock instanceof ChestBlock) {
            for (Direction dir : Direction.Plane.HORIZONTAL) {
                BlockState adjState = level.getBlockState(pos.relative(dir));
                if (!(adjState.getBlock() instanceof ChestBlock) || adjState.getBlock().getClass() != placingBlock.getClass()) continue;
                return (Direction)adjState.getValue((Property)ChestBlock.FACING);
            }
        }
        if (BuildingPlacer.isOpaqueNonContainer(bsNorth, (BlockGetter)level, pos.north()) && !BuildingPlacer.isSolidOpaque(bsSouth, (BlockGetter)level, pos.south())) {
            return Direction.SOUTH;
        }
        if (BuildingPlacer.isOpaqueNonContainer(bsSouth, (BlockGetter)level, pos.south()) && !BuildingPlacer.isSolidOpaque(bsNorth, (BlockGetter)level, pos.north())) {
            return Direction.NORTH;
        }
        if (BuildingPlacer.isOpaqueNonContainer(bsWest, (BlockGetter)level, pos.west()) && !BuildingPlacer.isSolidOpaque(bsEast, (BlockGetter)level, pos.east())) {
            return Direction.EAST;
        }
        if (BuildingPlacer.isOpaqueNonContainer(bsEast, (BlockGetter)level, pos.east()) && !BuildingPlacer.isSolidOpaque(bsWest, (BlockGetter)level, pos.west())) {
            return Direction.WEST;
        }
        if (!BuildingPlacer.isSolidOpaque(bsSouth, (BlockGetter)level, pos.south())) {
            return Direction.SOUTH;
        }
        if (!BuildingPlacer.isSolidOpaque(bsNorth, (BlockGetter)level, pos.north())) {
            return Direction.NORTH;
        }
        if (!BuildingPlacer.isSolidOpaque(bsEast, (BlockGetter)level, pos.east())) {
            return Direction.EAST;
        }
        if (!BuildingPlacer.isSolidOpaque(bsWest, (BlockGetter)level, pos.west())) {
            return Direction.WEST;
        }
        return Direction.NORTH;
    }

    private static boolean isOpaqueNonContainer(BlockState state, BlockGetter level, BlockPos pos) {
        if (!BuildingPlacer.isSolidOpaque(state, level, pos)) {
            return false;
        }
        if (state.getBlock() instanceof AbstractFurnaceBlock) {
            return false;
        }
        return !(state.getBlock() instanceof ChestBlock);
    }

    private static boolean isSolidOpaque(BlockState state, BlockGetter level, BlockPos pos) {
        return state.isSolidRender(level, pos);
    }

    private static void remapPlacedPaintedBricks(ServerLevel level, BuildingPlan plan, BlockPos origin, Rotation rotation, BuildingInstance instance) {
        Optional<StructureTemplate> opt = TemplateLoader.load(plan, level, TemplateLoader.cultureFsFor(plan));
        if (opt.isEmpty()) {
            return;
        }
        StructureTemplate template = opt.get();
        StructurePlaceSettings settings = new StructurePlaceSettings().setRotation(rotation);
        List<PlacementStep> steps = BuildingPlacer.extractBlocksFromNbt(template, settings, level);
        for (PlacementStep step : steps) {
            BlockState newState;
            IPaintedBlock painted;
            DyeColor remapped;
            BlockPos worldPos = step.relativePos().offset((Vec3i)origin);
            BlockState placed = level.getBlockState(worldPos);
            Block block = placed.getBlock();
            if (!(block instanceof IPaintedBlock) || (remapped = instance.remapBrickColour((painted = (IPaintedBlock)block).getColor())) == painted.getColor() || (newState = BuildingPlacer.getRemappedState(placed, painted, remapped)) == null) continue;
            level.setBlock(worldPos, newState, 3);
        }
    }

    private static BlockState getRemappedState(BlockState original, IPaintedBlock painted, DyeColor newColor) {
        if (painted instanceof PaintedBrickBlock) {
            PaintedBrickBlock brick = (PaintedBrickBlock)painted;
            Block newBlock = brick.getBrickType() == PaintedBrickBlock.BrickType.DECORATED ? (Block)ModBlocks.DECORATED_BRICKS.get(newColor).get() : (Block)ModBlocks.PAINTED_BRICKS.get(newColor).get();
            return newBlock.defaultBlockState();
        }
        if (painted instanceof PaintedBrickStairBlock) {
            BlockState base = ((Block)ModBlocks.PAINTED_BRICK_STAIRS.get(newColor).get()).defaultBlockState();
            return (BlockState)((BlockState)((BlockState)((BlockState)base.setValue((Property)StairBlock.FACING, (Comparable)((Direction)original.getValue((Property)StairBlock.FACING)))).setValue((Property)StairBlock.HALF, (Comparable)((Half)original.getValue((Property)StairBlock.HALF)))).setValue((Property)StairBlock.SHAPE, (Comparable)((StairsShape)original.getValue((Property)StairBlock.SHAPE)))).setValue((Property)StairBlock.WATERLOGGED, (Comparable)((Boolean)original.getValue((Property)StairBlock.WATERLOGGED)));
        }
        if (painted instanceof PaintedBrickSlabBlock) {
            BlockState base = ((Block)ModBlocks.PAINTED_BRICK_SLABS.get(newColor).get()).defaultBlockState();
            return (BlockState)((BlockState)base.setValue((Property)SlabBlock.TYPE, (Comparable)((SlabType)original.getValue((Property)SlabBlock.TYPE)))).setValue((Property)SlabBlock.WATERLOGGED, (Comparable)((Boolean)original.getValue((Property)SlabBlock.WATERLOGGED)));
        }
        if (painted instanceof PaintedBrickWallBlock) {
            BlockState base = ((Block)ModBlocks.PAINTED_BRICK_WALLS.get(newColor).get()).defaultBlockState();
            return (BlockState)((BlockState)((BlockState)((BlockState)((BlockState)((BlockState)base.setValue((Property)WallBlock.UP, (Comparable)((Boolean)original.getValue((Property)WallBlock.UP)))).setValue((Property)WallBlock.NORTH_WALL, (Comparable)((WallSide)original.getValue((Property)WallBlock.NORTH_WALL)))).setValue((Property)WallBlock.EAST_WALL, (Comparable)((WallSide)original.getValue((Property)WallBlock.EAST_WALL)))).setValue((Property)WallBlock.SOUTH_WALL, (Comparable)((WallSide)original.getValue((Property)WallBlock.SOUTH_WALL)))).setValue((Property)WallBlock.WEST_WALL, (Comparable)((WallSide)original.getValue((Property)WallBlock.WEST_WALL)))).setValue((Property)WallBlock.WATERLOGGED, (Comparable)((Boolean)original.getValue((Property)WallBlock.WATERLOGGED)));
        }
        return null;
    }

    private static void cleanupDroppedItems(ServerLevel level, BlockPos origin, int width, int height, int depth, Rotation rotation) {
        int effectiveWidth = rotation == Rotation.CLOCKWISE_90 || rotation == Rotation.COUNTERCLOCKWISE_90 ? depth : width;
        int effectiveDepth = rotation == Rotation.CLOCKWISE_90 || rotation == Rotation.COUNTERCLOCKWISE_90 ? width : depth;
        int margin = 5;
        AABB aabb = new AABB((double)(origin.getX() - margin), (double)(origin.getY() - 10), (double)(origin.getZ() - margin), (double)(origin.getX() + effectiveWidth + margin), (double)(origin.getY() + height + 10), (double)(origin.getZ() + effectiveDepth + margin));
        List items = level.getEntitiesOfClass(ItemEntity.class, aabb);
        for (ItemEntity item : items) {
            item.discard();
        }
    }

    public static void clearPlantsAboveSoil(ServerLevel level, BuildingInstance instance) {
        for (BlockPos soilPos : instance.getSoilPositions()) {
            BlockPos above = soilPos.above();
            BlockState state = level.getBlockState(above);
            if (!TerrainPreparer.isDecorativePlant(state)) continue;
            level.setBlock(above, Blocks.AIR.defaultBlockState(), 3);
        }
    }

    private static void fixWaterlogging(ServerLevel level, List<PlacementStep> steps, BlockPos origin) {
        BooleanProperty waterloggedProp = BlockStateProperties.WATERLOGGED;
        for (PlacementStep step : steps) {
            BlockPos worldPos;
            BlockState worldState;
            BlockState templateState = step.blockState();
            if (!templateState.hasProperty((Property)waterloggedProp) || ((Boolean)templateState.getValue((Property)waterloggedProp)).booleanValue() || !(worldState = level.getBlockState(worldPos = step.relativePos().offset((Vec3i)origin))).hasProperty((Property)waterloggedProp) || !((Boolean)worldState.getValue((Property)waterloggedProp)).booleanValue()) continue;
            level.setBlock(worldPos, (BlockState)worldState.setValue((Property)waterloggedProp, (Comparable)Boolean.valueOf(false)), 2);
        }
    }

    private static void fixDoubleChests(ServerLevel level, List<PlacementStep> steps, BlockPos origin) {
        block0: for (PlacementStep step : steps) {
            BlockPos worldPos = step.relativePos().offset((Vec3i)origin);
            BlockState placed = level.getBlockState(worldPos);
            if (!(placed.getBlock() instanceof ChestBlock) || placed.getValue((Property)ChestBlock.TYPE) != ChestType.SINGLE) continue;
            Direction facing = (Direction)placed.getValue((Property)ChestBlock.FACING);
            for (Direction dir : new Direction[]{facing.getClockWise(), facing.getCounterClockWise()}) {
                BlockPos adjPos = worldPos.relative(dir);
                BlockState adjState = level.getBlockState(adjPos);
                if (adjState.getBlock() != placed.getBlock() || adjState.getValue((Property)ChestBlock.FACING) != facing || adjState.getValue((Property)ChestBlock.TYPE) != ChestType.SINGLE) continue;
                ChestType thisType = dir == facing.getClockWise() ? ChestType.LEFT : ChestType.RIGHT;
                ChestType otherType = thisType == ChestType.LEFT ? ChestType.RIGHT : ChestType.LEFT;
                level.setBlock(worldPos, (BlockState)placed.setValue((Property)ChestBlock.TYPE, (Comparable)thisType), 2);
                level.setBlock(adjPos, (BlockState)adjState.setValue((Property)ChestBlock.TYPE, (Comparable)otherType), 2);
                continue block0;
            }
        }
    }

    private static void refreshDependentConnections(ServerLevel level, List<PlacementStep> steps, BlockPos origin) {
        for (PlacementStep step : steps) {
            BlockState corrected;
            BlockPos worldPos = step.relativePos().offset((Vec3i)origin);
            BlockState placed = level.getBlockState(worldPos);
            if (!BuildingPlacer.isConnectionBlock(placed) || (corrected = BuildingPlacer.computePaneConnections((LevelAccessor)level, worldPos, placed)) == placed) continue;
            level.setBlock(worldPos, corrected, 2);
        }
    }

    private static void generateBedFeet(ServerLevel level, List<PlacementStep> steps, BlockPos origin, @Nullable BuildingInstance building) {
        for (PlacementStep step : steps) {
            Direction facing;
            BlockPos headPos;
            BlockPos footPos;
            BlockState existing;
            BlockState state = step.blockState();
            if (!(state.getBlock() instanceof BedBlock) || !state.hasProperty((Property)BedBlock.PART) || state.getValue((Property)BedBlock.PART) != BedPart.HEAD || (existing = level.getBlockState(footPos = (headPos = step.relativePos().offset((Vec3i)origin)).relative((facing = (Direction)state.getValue((Property)BedBlock.FACING)).getOpposite()))).getBlock() instanceof BedBlock) continue;
            BlockState footState = (BlockState)state.setValue((Property)BedBlock.PART, (Comparable)BedPart.FOOT);
            level.setBlock(footPos, footState, 2);
            level.setBlock(headPos, state, 2);
            if (building == null) continue;
            building.getBedManager().add(footPos);
        }
    }

    public static void spawnWallDecorations(ServerLevel level, BuildingPlan plan, BlockPos origin, Rotation rotation) {
        StructurePlaceSettings settings = new StructurePlaceSettings().setRotation(rotation);
        for (SpecialPoint sp : plan.specialPoints()) {
            if (!sp.isType("wall_decoration")) continue;
            WallDecorationType type = WallDecorationType.fromName(sp.subtype());
            if (type == null) {
                LOGGER.warn("Unknown wall decoration type: {}", (Object)sp.subtype());
                continue;
            }
            BlockPos rotated = StructureTemplate.calculateRelativePosition((StructurePlaceSettings)settings, (BlockPos)sp.pos());
            BlockPos worldPos = rotated.offset((Vec3i)origin);
            MillWallDecoration decoration = MillWallDecoration.createForBuilding((Level)level, worldPos, type);
            if (decoration == null) continue;
            level.addFreshEntity((Entity)decoration);
        }
    }

    public static void spawnWallDecorations(ServerLevel level, List<SpecialPoint> resolvedPoints) {
        for (SpecialPoint sp : resolvedPoints) {
            if (!sp.isType("wall_decoration")) continue;
            WallDecorationType type = WallDecorationType.fromName(sp.subtype());
            if (type == null) {
                LOGGER.warn("Unknown wall decoration type: {}", (Object)sp.subtype());
                continue;
            }
            MillWallDecoration decoration = MillWallDecoration.createForBuilding((Level)level, sp.pos(), type);
            if (decoration == null) continue;
            level.addFreshEntity((Entity)decoration);
        }
    }

    private static Comparator<PlacementStep> sPatternXZComparator() {
        return Comparator.comparingInt(s -> Math.floorDiv(s.relativePos().getX(), 8)).thenComparingInt(s -> Math.floorDiv(s.relativePos().getZ(), 8)).thenComparingInt(s -> Math.floorMod(s.relativePos().getX(), 8)).thenComparingInt(s -> {
            int withinX = Math.floorMod(s.relativePos().getX(), 8);
            int withinZ = Math.floorMod(s.relativePos().getZ(), 8);
            return withinX % 2 == 0 ? withinZ : 7 - withinZ;
        });
    }
}

