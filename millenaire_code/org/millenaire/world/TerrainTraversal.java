/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  net.minecraft.core.BlockPos
 *  net.minecraft.server.level.ServerLevel
 *  net.minecraft.tags.BlockTags
 *  net.minecraft.world.level.block.state.BlockState
 */
package org.millenaire.world;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.level.block.state.BlockState;
import org.millenaire.building.ClearMargins;
import org.millenaire.world.TerrainPreparer;

public final class TerrainTraversal {
    public static final int CLEAR_ABOVE_MARGIN = 50;
    public static final int FOUNDATION_DEPTH = 10;

    private TerrainTraversal() {
    }

    public static int computeOffsetComponent(int worldCoord, int originCoord, int size) {
        if (worldCoord < originCoord) {
            return originCoord - worldCoord;
        }
        if (worldCoord >= originCoord + size) {
            return worldCoord - (originCoord + size) + 1;
        }
        return 0;
    }

    public static boolean isInsideFootprint(int wx, int wz, BlockPos origin, int width, int depth) {
        return wx >= origin.getX() && wx < origin.getX() + width && wz >= origin.getZ() && wz < origin.getZ() + depth;
    }

    public static void traverse(ServerLevel level, BlockPos effOrigin, int effWidth, int effDepth, int height, int baseY, int groundLevel, ClearMargins effectiveMargins, BlockVisitor visitor) {
        int y;
        int startX = effOrigin.getX() - effectiveMargins.lengthBefore();
        int endX = effOrigin.getX() + effWidth + effectiveMargins.lengthAfter();
        int startZ = effOrigin.getZ() - effectiveMargins.widthBefore();
        int endZ = effOrigin.getZ() + effDepth + effectiveMargins.widthAfter();
        int gridW = endX - startX;
        int gridD = endZ - startZ;
        BlockState[][] surfaceBlocks = new BlockState[gridW][gridD];
        BlockState[][] subSurfaceBlocks = new BlockState[gridW][gridD];
        for (int ix = 0; ix < gridW; ++ix) {
            for (int iz = 0; iz < gridD; ++iz) {
                surfaceBlocks[ix][iz] = TerrainPreparer.getSurfaceBlock(level, startX + ix, startZ + iz);
                subSurfaceBlocks[ix][iz] = TerrainPreparer.getSubSurfaceBlock(level, startX + ix, startZ + iz);
            }
        }
        for (int wx = startX; wx < endX; ++wx) {
            for (int wz = startZ; wz < endZ; ++wz) {
                BlockContext ctx;
                BlockState existing;
                BlockPos pos;
                int topDeltaY;
                int offsetX = TerrainTraversal.computeOffsetComponent(wx, effOrigin.getX(), effWidth);
                int offsetZ = TerrainTraversal.computeOffsetComponent(wz, effOrigin.getZ(), effDepth);
                int offset = Math.max(offsetX, offsetZ);
                if (Math.abs(offsetX - offsetZ) < 3) {
                    ++offset;
                }
                --offset;
                boolean isInside = TerrainTraversal.isInsideFootprint(wx, wz, effOrigin, effWidth, effDepth);
                boolean isSideBorder = wx == startX || wx == endX - 1 || wz == startZ || wz == endZ - 1;
                BlockState surfaceBlock = surfaceBlocks[wx - startX][wz - startZ];
                BlockState subSurfaceBlock = subSurfaceBlocks[wx - startX][wz - startZ];
                for (int deltaY = topDeltaY = height + 50; deltaY >= 0; --deltaY) {
                    y = baseY + deltaY;
                    pos = new BlockPos(wx, y, wz);
                    existing = level.getBlockState(pos);
                    if (existing.isAir()) continue;
                    ctx = new BlockContext(wx, wz, y, existing, isInside, surfaceBlock, subSurfaceBlock);
                    if (deltaY >= offset - 2) {
                        if (!isInside) {
                            boolean isBorder;
                            boolean bl = isBorder = deltaY == offset - 2 || deltaY == 0 || isSideBorder;
                            if (isBorder) {
                                if (TerrainPreparer.isDecorativePlant(existing) || TerrainPreparer.isLeaves(existing)) continue;
                                if (TerrainPreparer.isAdjacentToWater(level, pos)) {
                                    visitor.visit(ctx, Action.BORDER_ANTIFLOOD);
                                    continue;
                                }
                                visitor.visit(ctx, Action.CLEAR_AIR);
                                continue;
                            }
                            if (TerrainPreparer.isDecorativePlant(existing) || TerrainPreparer.isLeaves(existing)) continue;
                            visitor.visit(ctx, Action.CLEAR_AIR);
                            continue;
                        }
                        if (TerrainPreparer.isDecorativePlant(existing)) continue;
                        visitor.visit(ctx, Action.CLEAR_AIR);
                        continue;
                    }
                    if (!TerrainPreparer.isLog(existing) && !TerrainPreparer.isHugeMushroomBlock(existing)) continue;
                    visitor.visit(ctx, Action.CLEAR_TREE);
                }
                if (groundLevel >= 0 || !isInside) continue;
                int clearBottom = baseY + groundLevel + 1;
                for (y = baseY - 1; y >= clearBottom; --y) {
                    pos = new BlockPos(wx, y, wz);
                    existing = level.getBlockState(pos);
                    if (existing.isAir()) continue;
                    ctx = new BlockContext(wx, wz, y, existing, true, surfaceBlock, subSurfaceBlock);
                    visitor.visit(ctx, Action.CLEAR_SUBGROUND);
                }
            }
        }
        int foundationBottom = baseY - 10;
        int insideFoundationTop = baseY + Math.min(groundLevel, 0);
        for (int wx = startX; wx < endX; ++wx) {
            for (int wz = startZ; wz < endZ; ++wz) {
                int offsetX = TerrainTraversal.computeOffsetComponent(wx, effOrigin.getX(), effWidth);
                int offsetZ = TerrainTraversal.computeOffsetComponent(wz, effOrigin.getZ(), effDepth);
                int offset = Math.max(offsetX, offsetZ);
                if (Math.abs(offsetX - offsetZ) < 3) {
                    ++offset;
                }
                --offset;
                boolean isInside = TerrainTraversal.isInsideFootprint(wx, wz, effOrigin, effWidth, effDepth);
                int columnTop = isInside ? insideFoundationTop : baseY;
                BlockState surfaceBlock = surfaceBlocks[wx - startX][wz - startZ];
                BlockState subSurfaceBlock = subSurfaceBlocks[wx - startX][wz - startZ];
                for (y = foundationBottom; y < columnTop; ++y) {
                    int depthBelowBase = baseY - y;
                    BlockPos pos = new BlockPos(wx, y, wz);
                    BlockState existing = level.getBlockState(pos);
                    BlockContext ctx = new BlockContext(wx, wz, y, existing, isInside, surfaceBlock, subSurfaceBlock);
                    if (depthBelowBase > offset) {
                        if (TerrainPreparer.isPreservedFoundationBlock(existing)) {
                            if (groundLevel >= 0 || !isInside || !TerrainPreparer.isFallingBlock(existing)) continue;
                            visitor.visit(ctx, Action.STABILIZE_FALLING);
                            continue;
                        }
                        if (!existing.isAir() && existing.getFluidState().isEmpty() && !TerrainPreparer.isLeaves(existing) && !TerrainPreparer.isLog(existing) && !TerrainPreparer.isHugeMushroomBlock(existing) && !existing.is(BlockTags.REPLACEABLE) && existing.canOcclude()) continue;
                        visitor.visit(ctx, Action.FILL_SUBSURFACE);
                        continue;
                    }
                    if (depthBelowBase >= offset - 1) {
                        if (TerrainPreparer.isPreservedFoundationBlock(existing)) {
                            if (groundLevel >= 0 || !isInside || !TerrainPreparer.isFallingBlock(existing)) continue;
                            visitor.visit(ctx, Action.STABILIZE_FALLING);
                            continue;
                        }
                        if (!existing.isAir() && existing.getFluidState().isEmpty() && !TerrainPreparer.isLeaves(existing) && !TerrainPreparer.isLog(existing) && !TerrainPreparer.isHugeMushroomBlock(existing) && !existing.is(BlockTags.REPLACEABLE) && existing.canOcclude()) continue;
                        visitor.visit(ctx, Action.FILL_SURFACE);
                        continue;
                    }
                    if (!TerrainPreparer.isLog(existing) && !TerrainPreparer.isHugeMushroomBlock(existing)) continue;
                    visitor.visit(ctx, Action.CLEAR_TREE);
                }
            }
        }
    }

    public record BlockContext(int wx, int wz, int y, BlockState existing, boolean isInsideBuilding, BlockState surfaceBlock, BlockState subSurfaceBlock) {
    }

    public static enum Action {
        CLEAR_AIR,
        CLEAR_TREE,
        FILL_SUBSURFACE,
        FILL_SURFACE,
        BORDER_ANTIFLOOD,
        CLEAR_SUBGROUND,
        STABILIZE_FALLING;

    }

    @FunctionalInterface
    public static interface BlockVisitor {
        public void visit(BlockContext var1, Action var2);
    }
}

