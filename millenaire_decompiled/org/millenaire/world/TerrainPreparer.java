/*
 * Decompiled with CFR 0.150.
 * 
 * Could not load the following classes:
 *  com.mojang.logging.LogUtils
 *  net.minecraft.core.BlockPos
 *  net.minecraft.core.BlockPos$MutableBlockPos
 *  net.minecraft.server.level.ServerLevel
 *  net.minecraft.tags.BlockTags
 *  net.minecraft.world.level.LevelReader
 *  net.minecraft.world.level.block.Block
 *  net.minecraft.world.level.block.Blocks
 *  net.minecraft.world.level.block.LeavesBlock
 *  net.minecraft.world.level.block.Rotation
 *  net.minecraft.world.level.block.state.BlockState
 *  net.minecraft.world.level.block.state.properties.Property
 *  net.minecraft.world.level.levelgen.Heightmap$Types
 *  org.slf4j.Logger
 */
package org.millenaire.world;

import com.mojang.logging.LogUtils;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Set;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.LeavesBlock;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.Property;
import net.minecraft.world.level.levelgen.Heightmap;
import org.millenaire.building.ClearMargins;
import org.millenaire.item.BlockHelper;
import org.millenaire.world.TerrainTraversal;
import org.millenaire.world.VillageTerrainMap;
import org.slf4j.Logger;

public final class TerrainPreparer {
    private static final Logger LOGGER = LogUtils.getLogger();
    private static final int BLOCK_UPDATE_FLAGS = 3;
    static final int DEFAULT_MARGIN = 5;
    private static final int FOUNDATION_DEPTH = 10;
    private static final int CLEAR_ABOVE_MARGIN = 50;
    private static final Set<Block> PRESERVED_FOUNDATION_BLOCKS = Set.of(new Block[]{Blocks.STONE, Blocks.GRANITE, Blocks.DIORITE, Blocks.ANDESITE, Blocks.DIRT, Blocks.COARSE_DIRT, Blocks.GRAVEL, Blocks.SAND, Blocks.RED_SAND, Blocks.SANDSTONE, Blocks.RED_SANDSTONE, Blocks.CLAY, Blocks.DEEPSLATE, Blocks.GRASS_BLOCK, Blocks.PODZOL, Blocks.MYCELIUM});
    private static final int SNOW_RESTORE_SCAN_DEPTH = 16;
    private static final int LOG_SEARCH_MARGIN = 4;
    private static final int LEAF_CLEAR_MARGIN = 2;
    private static final int NON_DECAY_RANGE = 3;
    private static final int MAX_TREE_LOGS = 100;
    private static final int MAX_TREE_HORIZONTAL_DIST_SQ = 100;

    private TerrainPreparer() {
    }

    public static int effectiveWidth(int width, int depth, Rotation rotation) {
        return rotation == Rotation.CLOCKWISE_90 || rotation == Rotation.COUNTERCLOCKWISE_90 ? depth : width;
    }

    public static int effectiveDepth(int width, int depth, Rotation rotation) {
        return rotation == Rotation.CLOCKWISE_90 || rotation == Rotation.COUNTERCLOCKWISE_90 ? width : depth;
    }

    public static BlockPos effectiveOrigin(BlockPos origin, int width, int depth, Rotation rotation) {
        return switch (rotation) {
            case Rotation.CLOCKWISE_90 -> new BlockPos(origin.getX() - depth + 1, origin.getY(), origin.getZ());
            case Rotation.CLOCKWISE_180 -> new BlockPos(origin.getX() - width + 1, origin.getY(), origin.getZ() - depth + 1);
            case Rotation.COUNTERCLOCKWISE_90 -> new BlockPos(origin.getX(), origin.getY(), origin.getZ() - width + 1);
            default -> origin;
        };
    }

    public static int clearAndFlatten(ServerLevel level, BlockPos origin, int width, int height, int depth) {
        return TerrainPreparer.clearAndFlatten(level, origin, width, height, depth, Rotation.NONE, 0, ClearMargins.defaults());
    }

    public static int clearAndFlatten(ServerLevel level, BlockPos origin, int width, int height, int depth, Rotation rotation, int groundLevel, ClearMargins margins) {
        int effWidth = TerrainPreparer.effectiveWidth(width, depth, rotation);
        int effDepth = TerrainPreparer.effectiveDepth(width, depth, rotation);
        BlockPos effOrigin = TerrainPreparer.effectiveOrigin(origin, width, depth, rotation);
        ClearMargins effective = margins.forRotation(rotation);
        int baseY = TerrainPreparer.computeAverageSurfaceHeight(level, effOrigin, effWidth, effDepth, effective);
        return TerrainPreparer.clearAndFlattenAtY(level, origin, width, height, depth, rotation, groundLevel, baseY, margins);
    }

    public static int clearAndFlattenAtY(ServerLevel level, BlockPos origin, int width, int height, int depth, Rotation rotation, int groundLevel, int baseY, ClearMargins margins) {
        int effWidth = TerrainPreparer.effectiveWidth(width, depth, rotation);
        int effDepth = TerrainPreparer.effectiveDepth(width, depth, rotation);
        BlockPos effOrigin = TerrainPreparer.effectiveOrigin(origin, width, depth, rotation);
        ClearMargins effective = margins.forRotation(rotation);
        TerrainTraversal.traverse(level, effOrigin, effWidth, effDepth, height, baseY, groundLevel, effective, (ctx, action) -> {
            BlockPos pos = new BlockPos(ctx.wx(), ctx.y(), ctx.wz());
            switch (action) {
                case CLEAR_AIR: 
                case CLEAR_TREE: {
                    TerrainPreparer.clearPlantAbove(level, pos);
                    level.setBlock(pos, Blocks.AIR.defaultBlockState(), 3);
                    TerrainPreparer.fixBlockBelow(level, pos);
                    break;
                }
                case CLEAR_SUBGROUND: {
                    TerrainPreparer.clearPlantAbove(level, pos);
                    level.setBlock(pos, Blocks.AIR.defaultBlockState(), 3);
                    break;
                }
                case FILL_SUBSURFACE: {
                    level.setBlock(pos, TerrainPreparer.stabilizedFallingBlock(ctx.subSurfaceBlock()), 3);
                    break;
                }
                case FILL_SURFACE: {
                    level.setBlock(pos, ctx.surfaceBlock(), 3);
                    break;
                }
                case BORDER_ANTIFLOOD: {
                    level.setBlock(pos, TerrainPreparer.stabilizedFallingBlock(ctx.subSurfaceBlock()), 3);
                    break;
                }
                case STABILIZE_FALLING: {
                    level.setBlock(pos, TerrainPreparer.stabilizedFallingBlock(ctx.existing()), 3);
                }
            }
        });
        return baseY;
    }

    public static int computeAverageSurfaceHeight(ServerLevel level, BlockPos origin, int width, int depth, ClearMargins effectiveMargins) {
        int scanStartX = origin.getX() - effectiveMargins.lengthBefore() - 1;
        int scanEndX = origin.getX() + width + effectiveMargins.lengthAfter() + 1;
        int scanStartZ = origin.getZ() - effectiveMargins.widthBefore() - 1;
        int scanEndZ = origin.getZ() + depth + effectiveMargins.widthAfter() + 1;
        long altitudeTotal = 0L;
        int nbPoints = 0;
        for (int x = scanStartX; x < scanEndX; ++x) {
            for (int z = scanStartZ; z < scanEndZ; ++z) {
                altitudeTotal += (long)TerrainPreparer.getSurfaceOrWaterHeight(level, x, z);
                ++nbPoints;
            }
        }
        if (nbPoints == 0) {
            return origin.getY();
        }
        return Math.round((float)altitudeTotal * 1.0f / (float)nbPoints);
    }

    public static int getGroundHeight(ServerLevel level, int x, int z) {
        return TerrainPreparer.scanColumnSurface(level, x, z, false);
    }

    public static int getSurfaceOrWaterHeight(ServerLevel level, int x, int z) {
        return TerrainPreparer.scanColumnSurface(level, x, z, true);
    }

    private static int scanColumnSurface(ServerLevel level, int x, int z, boolean waterCountsAsSurface) {
        int y = level.getHeight(Heightmap.Types.WORLD_SURFACE, x, z);
        if (y <= level.getMinBuildHeight()) {
            y = level.getMaxBuildHeight();
        }
        for (int cy = y; cy > level.getMinBuildHeight(); --cy) {
            BlockState state = level.getBlockState(new BlockPos(x, cy, z));
            if (state.isAir() || state.is(BlockTags.LEAVES) || state.is(BlockTags.LOGS) || TerrainPreparer.isHugeMushroomBlock(state) || state.is(BlockTags.REPLACEABLE)) continue;
            if (!state.getFluidState().isEmpty()) {
                if (!waterCountsAsSurface) continue;
                return cy + 1;
            }
            if (!state.canOcclude()) continue;
            return cy + 1;
        }
        return level.getMinBuildHeight();
    }

    static int distanceToBuildingEdge(int wx, int wz, BlockPos origin, int width, int depth) {
        int dx = 0;
        if (wx < origin.getX()) {
            dx = origin.getX() - wx;
        } else if (wx >= origin.getX() + width) {
            dx = wx - (origin.getX() + width) + 1;
        }
        int dz = 0;
        if (wz < origin.getZ()) {
            dz = origin.getZ() - wz;
        } else if (wz >= origin.getZ() + depth) {
            dz = wz - (origin.getZ() + depth) + 1;
        }
        return Math.max(dx, dz);
    }

    static BlockState getSurfaceBlock(ServerLevel level, int x, int z) {
        int surfaceY;
        for (int y = surfaceY = TerrainPreparer.getGroundHeight(level, x, z); y >= surfaceY - 5; --y) {
            BlockState state = level.getBlockState(new BlockPos(x, y, z));
            Block block = state.getBlock();
            if (block == Blocks.SAND || block == Blocks.RED_SAND) {
                return state;
            }
            if (block == Blocks.GRAVEL) {
                return state;
            }
            if (block == Blocks.GRASS_BLOCK || block == Blocks.MYCELIUM) {
                return state;
            }
            if (block == Blocks.DIRT || block == Blocks.COARSE_DIRT || block == Blocks.PODZOL) {
                return Blocks.GRASS_BLOCK.defaultBlockState();
            }
            if (block == Blocks.STONE || block == Blocks.DEEPSLATE || block == Blocks.GRANITE || block == Blocks.DIORITE || block == Blocks.ANDESITE) {
                return Blocks.GRASS_BLOCK.defaultBlockState();
            }
            if (block != Blocks.TERRACOTTA && block != Blocks.RED_SANDSTONE) continue;
            return state;
        }
        return Blocks.GRASS_BLOCK.defaultBlockState();
    }

    static BlockState getSubSurfaceBlock(ServerLevel level, int x, int z) {
        int surfaceY;
        for (int y = surfaceY = TerrainPreparer.getGroundHeight(level, x, z); y >= surfaceY - 5; --y) {
            BlockState state = level.getBlockState(new BlockPos(x, y, z));
            Block block = state.getBlock();
            if (block == Blocks.SAND) {
                return Blocks.SANDSTONE.defaultBlockState();
            }
            if (block == Blocks.RED_SAND) {
                return Blocks.RED_SANDSTONE.defaultBlockState();
            }
            if (block == Blocks.GRAVEL) {
                return state;
            }
            if (block == Blocks.RED_SANDSTONE) {
                return state;
            }
            if (block == Blocks.TERRACOTTA) {
                return state;
            }
            if (block == Blocks.DIRT || block == Blocks.GRASS_BLOCK || block == Blocks.PODZOL || block == Blocks.COARSE_DIRT || block == Blocks.MYCELIUM) {
                return Blocks.DIRT.defaultBlockState();
            }
            if (block != Blocks.STONE && block != Blocks.DEEPSLATE && block != Blocks.GRANITE && block != Blocks.DIORITE && block != Blocks.ANDESITE) continue;
            return state;
        }
        return Blocks.DIRT.defaultBlockState();
    }

    static boolean isAdjacentToWater(ServerLevel level, BlockPos pos) {
        return level.getBlockState(pos.north()).is(Blocks.WATER) || level.getBlockState(pos.south()).is(Blocks.WATER) || level.getBlockState(pos.east()).is(Blocks.WATER) || level.getBlockState(pos.west()).is(Blocks.WATER);
    }

    static boolean isLeaves(BlockState state) {
        return state.is(BlockTags.LEAVES);
    }

    static boolean isLog(BlockState state) {
        return state.is(BlockTags.LOGS);
    }

    static boolean isHugeMushroomBlock(BlockState state) {
        return state.is(Blocks.MUSHROOM_STEM) || state.is(Blocks.BROWN_MUSHROOM_BLOCK) || state.is(Blocks.RED_MUSHROOM_BLOCK);
    }

    static boolean isPreservedFoundationBlock(BlockState state) {
        return PRESERVED_FOUNDATION_BLOCKS.contains((Object)state.getBlock());
    }

    static boolean isFallingBlock(BlockState state) {
        return state.is(Blocks.SAND) || state.is(Blocks.RED_SAND) || state.is(Blocks.GRAVEL);
    }

    static BlockState stabilizedFallingBlock(BlockState state) {
        if (state.is(Blocks.SAND) || state.is(Blocks.RED_SAND) || state.is(Blocks.GRAVEL)) {
            return Blocks.DIRT.defaultBlockState();
        }
        return state;
    }

    static boolean isDecorativePlant(BlockState state) {
        return BlockHelper.isDecorativeFlower(state) || state.is(BlockTags.SMALL_FLOWERS) || state.is(Blocks.SHORT_GRASS) || state.is(Blocks.TALL_GRASS) || state.is(Blocks.FERN) || state.is(Blocks.LARGE_FERN) || state.is(Blocks.BROWN_MUSHROOM) || state.is(Blocks.RED_MUSHROOM) || state.is(Blocks.DEAD_BUSH) || state.is(BlockTags.SAPLINGS);
    }

    private static void clearPlantAbove(ServerLevel level, BlockPos pos) {
        BlockPos above = pos.above();
        BlockState aboveState = level.getBlockState(above);
        if (TerrainPreparer.isDecorativePlant(aboveState)) {
            level.setBlock(above, Blocks.AIR.defaultBlockState(), 2);
        }
    }

    private static void fixBlockBelow(ServerLevel level, BlockPos pos) {
        BlockPos below = pos.below();
        BlockState belowState = level.getBlockState(below);
        if (belowState.is(Blocks.DIRT)) {
            level.setBlock(below, Blocks.GRASS_BLOCK.defaultBlockState(), 3);
        }
    }

    public static boolean[][] checkForSnow(ServerLevel level, BlockPos origin, int width, int depth, Rotation rotation, ClearMargins margins) {
        int effWidth = TerrainPreparer.effectiveWidth(width, depth, rotation);
        int effDepth = TerrainPreparer.effectiveDepth(width, depth, rotation);
        BlockPos effOrigin = TerrainPreparer.effectiveOrigin(origin, width, depth, rotation);
        ClearMargins effective = margins.forRotation(rotation);
        int gridW = effWidth + effective.lengthBefore() + effective.lengthAfter();
        int gridD = effDepth + effective.widthBefore() + effective.widthAfter();
        boolean[][] snow = new boolean[gridW][gridD];
        boolean anySnow = false;
        for (int dx = 0; dx < gridW; ++dx) {
            block1: for (int dz = 0; dz < gridD; ++dz) {
                int startY;
                int wx = effOrigin.getX() - effective.lengthBefore() + dx;
                int wz = effOrigin.getZ() - effective.widthBefore() + dz;
                for (int y = startY = level.getHeight(Heightmap.Types.WORLD_SURFACE, wx, wz); y > level.getMinBuildHeight(); --y) {
                    BlockState state = level.getBlockState(new BlockPos(wx, y, wz));
                    if (state.is(Blocks.SNOW) || state.is(Blocks.POWDER_SNOW)) {
                        snow[dx][dz] = true;
                        anySnow = true;
                        continue block1;
                    }
                    if (state.canOcclude()) continue block1;
                }
            }
        }
        return (boolean[][])(anySnow ? snow : null);
    }

    public static void restoreSnow(ServerLevel level, BlockPos origin, int width, int depth, Rotation rotation, boolean[][] snowMap, ClearMargins margins) {
        if (snowMap == null) {
            return;
        }
        int effWidth = TerrainPreparer.effectiveWidth(width, depth, rotation);
        int effDepth = TerrainPreparer.effectiveDepth(width, depth, rotation);
        BlockPos effOrigin = TerrainPreparer.effectiveOrigin(origin, width, depth, rotation);
        ClearMargins effective = margins.forRotation(rotation);
        BlockState snowLayer = Blocks.SNOW.defaultBlockState();
        int gridW = effWidth + effective.lengthBefore() + effective.lengthAfter();
        int gridD = effDepth + effective.widthBefore() + effective.widthAfter();
        for (int dx = 0; dx < gridW; ++dx) {
            block1: for (int dz = 0; dz < gridD; ++dz) {
                int startY;
                if (!snowMap[dx][dz]) continue;
                int wx = effOrigin.getX() - effective.lengthBefore() + dx;
                int wz = effOrigin.getZ() - effective.widthBefore() + dz;
                for (int y = startY = level.getHeight(Heightmap.Types.WORLD_SURFACE, wx, wz); y > startY - 16 && y > level.getMinBuildHeight(); --y) {
                    boolean aboveReplaceable;
                    BlockPos p = new BlockPos(wx, y, wz);
                    BlockState s = level.getBlockState(p);
                    if (s.isAir() || s.canBeReplaced()) continue;
                    BlockPos above = p.above();
                    BlockState aboveState = level.getBlockState(above);
                    boolean bl = aboveReplaceable = aboveState.isAir() || aboveState.canBeReplaced();
                    if (!aboveReplaceable || !snowLayer.canSurvive((LevelReader)level, above)) continue block1;
                    level.setBlock(above, snowLayer, 3);
                    continue block1;
                }
            }
        }
    }

    public static void decayOrphanedLeaves(ServerLevel level, BlockPos origin, int width, int depth, Rotation rotation, int baseY, ClearMargins margins) {
        int effWidth = TerrainPreparer.effectiveWidth(width, depth, rotation);
        int effDepth = TerrainPreparer.effectiveDepth(width, depth, rotation);
        BlockPos effOrigin = TerrainPreparer.effectiveOrigin(origin, width, depth, rotation);
        ClearMargins effective = margins.forRotation(rotation);
        int footStartX = effOrigin.getX();
        int footEndX = effOrigin.getX() + effWidth;
        int footStartZ = effOrigin.getZ();
        int footEndZ = effOrigin.getZ() + effDepth;
        int scanStartY = baseY - 10;
        int scanEndY = baseY + 50;
        HashSet<BlockPos> treeLogs = new HashSet<BlockPos>();
        HashSet<BlockPos> visited = new HashSet<BlockPos>();
        int treeStartX = footStartX - effective.lengthBefore() - 4;
        int treeEndX = footEndX + effective.lengthAfter() + 4;
        int treeStartZ = footStartZ - effective.widthBefore() - 4;
        int treeEndZ = footEndZ + effective.widthAfter() + 4;
        BlockPos.MutableBlockPos cursor = new BlockPos.MutableBlockPos();
        for (int x = treeStartX; x < treeEndX; ++x) {
            for (int z = treeStartZ; z < treeEndZ; ++z) {
                if (x >= footStartX && x < footEndX && z >= footStartZ && z < footEndZ) continue;
                for (int y = scanStartY; y < scanEndY; ++y) {
                    cursor.set(x, y, z);
                    if (visited.contains((Object)cursor) || !level.getBlockState((BlockPos)cursor).is(BlockTags.LOGS) || !VillageTerrainMap.isGround(level.getBlockState(new BlockPos(x, y - 1, z)))) continue;
                    TerrainPreparer.collectTree(level, cursor.immutable(), treeLogs, visited);
                }
            }
        }
        HashSet<BlockPos> protectedLeaves = new HashSet<BlockPos>();
        for (BlockPos logPos : treeLogs) {
            for (int dx = -3; dx <= 3; ++dx) {
                for (int dy = -3; dy <= 3; ++dy) {
                    for (int dz = -3; dz <= 3; ++dz) {
                        protectedLeaves.add(logPos.offset(dx, dy, dz));
                    }
                }
            }
        }
        int clearStartX = footStartX - effective.lengthBefore() - 2;
        int clearEndX = footEndX + effective.lengthAfter() + 2;
        int clearStartZ = footStartZ - effective.widthBefore() - 2;
        int clearEndZ = footEndZ + effective.widthAfter() + 2;
        int removedLogs = 0;
        int removedLeaves = 0;
        for (int x = clearStartX; x < clearEndX; ++x) {
            for (int z = clearStartZ; z < clearEndZ; ++z) {
                if (x >= footStartX && x < footEndX && z >= footStartZ && z < footEndZ) continue;
                for (int y = scanStartY; y < scanEndY; ++y) {
                    cursor.set(x, y, z);
                    BlockState state = level.getBlockState((BlockPos)cursor);
                    if (state.is(BlockTags.LOGS)) {
                        if (treeLogs.contains((Object)cursor)) continue;
                        level.setBlock((BlockPos)cursor, Blocks.AIR.defaultBlockState(), 3);
                        ++removedLogs;
                        continue;
                    }
                    if (!(state.getBlock() instanceof LeavesBlock) || ((Boolean)state.getValue((Property)LeavesBlock.PERSISTENT)).booleanValue() || protectedLeaves.contains((Object)cursor)) continue;
                    level.setBlock((BlockPos)cursor, Blocks.AIR.defaultBlockState(), 3);
                    ++removedLeaves;
                }
            }
        }
        LOGGER.debug("[TreeClearer] around {}: removed {} stray logs + {} orphan leaves, preserved {} complete-tree logs", new Object[]{origin.toShortString(), removedLogs, removedLeaves, treeLogs.size()});
    }

    private static void collectTree(ServerLevel level, BlockPos start, Set<BlockPos> treeLogs, Set<BlockPos> visited) {
        ArrayList<BlockPos> found = new ArrayList<BlockPos>();
        ArrayDeque<BlockPos> stack = new ArrayDeque<BlockPos>();
        stack.push(start);
        boolean abort = false;
        while (!stack.isEmpty() && !abort) {
            BlockPos p = (BlockPos)stack.pop();
            if (!visited.add(p)) continue;
            if (level.getBlockState(p).is(BlockTags.LOGS)) {
                found.add(p);
                for (int dx = -1; dx <= 1; ++dx) {
                    for (int dy = -1; dy <= 1; ++dy) {
                        for (int dz = -1; dz <= 1; ++dz) {
                            if (dx == 0 && dy == 0 && dz == 0) continue;
                            stack.push(p.offset(dx, dy, dz));
                        }
                    }
                }
            }
            int ddx = p.getX() - start.getX();
            int ddz = p.getZ() - start.getZ();
            abort = found.size() > 100 || ddx * ddx + ddz * ddz > 100;
        }
        treeLogs.addAll(found);
    }

    public static int clearLeavesInFootprint(ServerLevel level, BlockPos origin, int width, int height, int depth, Rotation rotation, int baseY, int groundLevel, ClearMargins margins, int buffer) {
        int effWidth = TerrainPreparer.effectiveWidth(width, depth, rotation);
        int effDepth = TerrainPreparer.effectiveDepth(width, depth, rotation);
        BlockPos effOrigin = TerrainPreparer.effectiveOrigin(origin, width, depth, rotation);
        ClearMargins effective = margins.forRotation(rotation);
        int scanStartX = effOrigin.getX() - effective.lengthBefore() - buffer;
        int scanEndX = effOrigin.getX() + effWidth + effective.lengthAfter() + buffer;
        int scanStartZ = effOrigin.getZ() - effective.widthBefore() - buffer;
        int scanEndZ = effOrigin.getZ() + effDepth + effective.widthAfter() + buffer;
        int scanStartY = baseY + Math.min(groundLevel, 0) - 1;
        int scanEndY = baseY + height + 50;
        int cleared = 0;
        BlockPos.MutableBlockPos mutable = new BlockPos.MutableBlockPos();
        for (int x = scanStartX; x < scanEndX; ++x) {
            for (int z = scanStartZ; z < scanEndZ; ++z) {
                for (int y = scanStartY; y < scanEndY; ++y) {
                    mutable.set(x, y, z);
                    BlockState state = level.getBlockState((BlockPos)mutable);
                    if (!(state.getBlock() instanceof LeavesBlock) || ((Boolean)state.getValue((Property)LeavesBlock.PERSISTENT)).booleanValue()) continue;
                    level.setBlock((BlockPos)mutable, Blocks.AIR.defaultBlockState(), 2);
                    ++cleared;
                }
            }
        }
        if (cleared > 0) {
            LOGGER.debug("[GH-76] Pre-cleared {} pre-existing leaves in footprint of building at {} (buffer={}, groundLevel={})", new Object[]{cleared, origin.toShortString(), buffer, groundLevel});
            TerrainPreparer.decayOrphanedLeaves(level, origin, width, depth, rotation, baseY, margins);
        } else {
            LOGGER.debug("[GH-76] No pre-existing leaves to clear in footprint of building at {} (buffer={}, groundLevel={})", new Object[]{origin.toShortString(), buffer, groundLevel});
        }
        return cleared;
    }
}

