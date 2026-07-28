/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  net.minecraft.core.BlockPos
 *  net.minecraft.server.level.ServerLevel
 *  net.minecraft.tags.BlockTags
 *  net.minecraft.world.level.block.Blocks
 *  net.minecraft.world.level.block.Rotation
 *  net.minecraft.world.level.block.StainedGlassBlock
 *  net.minecraft.world.level.block.StainedGlassPaneBlock
 *  net.minecraft.world.level.block.state.BlockState
 */
package org.millenaire.world;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.StainedGlassBlock;
import net.minecraft.world.level.block.StainedGlassPaneBlock;
import net.minecraft.world.level.block.state.BlockState;
import org.millenaire.building.ClearMargins;
import org.millenaire.tag.ModTags;
import org.millenaire.world.TerrainPreparer;

public class VillageTerrainMap {
    private static final int ALTITUDE_TOLERANCE = 10;
    private static final int SCAN_RANGE = 25;
    private static final int SMALL_BUILDING_FIXED_ERRORS = 10;
    private static final int MEDIUM_BUILDING_THRESHOLD = 200;
    private static final int HUGE_BUILDING_THRESHOLD = 2000;
    private final BlockPos origin;
    private final int size;
    private final int baseline;
    private final int[][] topGround;
    private final boolean[][] canBuild;
    private final boolean[][] danger;
    private final boolean[][] water;
    private final boolean[][] buildingForbidden;
    private final short[][] spaceAbove;
    private final boolean[][] occupied;

    private VillageTerrainMap(BlockPos origin, int size, int baseline, int[][] topGround, boolean[][] canBuild, boolean[][] danger, boolean[][] water, boolean[][] buildingForbidden, short[][] spaceAbove) {
        this.origin = origin;
        this.size = size;
        this.baseline = baseline;
        this.topGround = topGround;
        this.canBuild = canBuild;
        this.danger = danger;
        this.water = water;
        this.buildingForbidden = buildingForbidden;
        this.spaceAbove = spaceAbove;
        this.occupied = new boolean[size][size];
    }

    static VillageTerrainMap forTest(BlockPos origin, int size, int baseline, int[][] topGround, boolean[][] canBuild) {
        boolean[][] danger = new boolean[size][size];
        boolean[][] water = new boolean[size][size];
        boolean[][] forbidden = new boolean[size][size];
        short[][] spaceAbove = new short[size][size];
        for (int x = 0; x < size; ++x) {
            for (int z = 0; z < size; ++z) {
                spaceAbove[x][z] = 3;
            }
        }
        return new VillageTerrainMap(origin, size, baseline, topGround, canBuild, danger, water, forbidden, spaceAbove);
    }

    static VillageTerrainMap forTest(BlockPos origin, int size, int baseline, int[][] topGround, boolean[][] canBuild, boolean[][] danger, boolean[][] water, boolean[][] buildingForbidden, short[][] spaceAbove) {
        return new VillageTerrainMap(origin, size, baseline, topGround, canBuild, danger, water, buildingForbidden, spaceAbove);
    }

    private static boolean isDangerous(BlockState state) {
        return state.is(Blocks.LAVA) || state.is(Blocks.FIRE) || state.is(Blocks.SOUL_FIRE) || state.is(Blocks.CACTUS) || state.is(Blocks.TNT) || state.is(Blocks.MAGMA_BLOCK) || state.is(Blocks.SWEET_BERRY_BUSH) || state.is(Blocks.POWDER_SNOW);
    }

    private static boolean isForbidden(ServerLevel level, BlockPos pos, BlockState state) {
        if (state.isAir()) {
            return false;
        }
        if (state.is(ModTags.Blocks.FORBIDDEN_EXCEPTIONS)) {
            return false;
        }
        if (level.getBlockEntity(pos) != null) {
            return true;
        }
        if (state.getBlock() instanceof StainedGlassBlock || state.getBlock() instanceof StainedGlassPaneBlock) {
            return true;
        }
        return state.is(ModTags.Blocks.ARTIFICIAL_BLOCKS);
    }

    static boolean isGround(BlockState state) {
        if (state.isAir()) {
            return false;
        }
        if (!state.getFluidState().isEmpty()) {
            return false;
        }
        if (state.is(BlockTags.LEAVES) || state.is(BlockTags.LOGS)) {
            return false;
        }
        if (TerrainPreparer.isHugeMushroomBlock(state)) {
            return false;
        }
        if (state.is(BlockTags.REPLACEABLE)) {
            return false;
        }
        return state.canOcclude();
    }

    public static VillageTerrainMap compute(ServerLevel level, BlockPos villageCenter, int radius) {
        int size = 2 * radius + 1;
        int originX = villageCenter.getX() - radius;
        int originZ = villageCenter.getZ() - radius;
        BlockPos origin = new BlockPos(originX, villageCenter.getY(), originZ);
        int baseline = villageCenter.getY();
        int miny = Math.max(baseline - 25, level.getMinBuildHeight());
        int maxy = Math.min(baseline + 25, level.getMaxBuildHeight());
        int[][] topGround = new int[size][size];
        boolean[][] canBuild = new boolean[size][size];
        boolean[][] danger = new boolean[size][size];
        boolean[][] water = new boolean[size][size];
        boolean[][] buildingForbidden = new boolean[size][size];
        short[][] spaceAbove = new short[size][size];
        for (int lx = 0; lx < size; ++lx) {
            for (int lz = 0; lz < size; ++lz) {
                int wx = originX + lx;
                int wz = originZ + lz;
                boolean hasForbidden = false;
                int groundY = miny;
                for (int y = maxy; y >= miny; --y) {
                    BlockPos pos = new BlockPos(wx, y, wz);
                    BlockState state = level.getBlockState(pos);
                    if (VillageTerrainMap.isForbidden(level, pos, state)) {
                        hasForbidden = true;
                    }
                    if (!VillageTerrainMap.isGround(state)) continue;
                    groundY = y;
                    break;
                }
                boolean onGround = true;
                int lastLiquid = groundY;
                int surfaceY = groundY;
                int y = groundY + 1;
                while (y <= maxy) {
                    BlockState state = level.getBlockState(new BlockPos(wx, y, wz));
                    if (!state.getFluidState().isEmpty()) {
                        onGround = false;
                        lastLiquid = y;
                    } else if (state.canOcclude() && !state.is(BlockTags.REPLACEABLE)) {
                        onGround = true;
                    } else {
                        surfaceY = y;
                        break;
                    }
                    surfaceY = y++;
                }
                if (!onGround) {
                    surfaceY = lastLiquid;
                }
                topGround[lx][lz] = surfaceY;
                BlockState groundState = level.getBlockState(new BlockPos(wx, groundY, wz));
                boolean hasDanger = VillageTerrainMap.isDangerous(groundState);
                if (!hasDanger) {
                    BlockState surfaceState = level.getBlockState(new BlockPos(wx, surfaceY, wz));
                    hasDanger = VillageTerrainMap.isDangerous(surfaceState);
                }
                boolean hasWater = !level.getFluidState(new BlockPos(wx, surfaceY, wz)).isEmpty();
                for (int y2 = surfaceY; y2 <= maxy; ++y2) {
                    BlockPos pos = new BlockPos(wx, y2, wz);
                    BlockState state = level.getBlockState(pos);
                    if (VillageTerrainMap.isForbidden(level, pos, state)) {
                        hasForbidden = true;
                    }
                    if (!VillageTerrainMap.isDangerous(state)) continue;
                    hasDanger = true;
                }
                int sa = 0;
                boolean blocked = false;
                for (int y3 = surfaceY; y3 < surfaceY + 3 && y3 <= maxy; ++y3) {
                    BlockState state = level.getBlockState(new BlockPos(wx, y3, wz));
                    if (!(blocked || state.canOcclude() && !state.is(BlockTags.REPLACEABLE))) {
                        sa = (short)(sa + 1);
                        continue;
                    }
                    blocked = true;
                }
                danger[lx][lz] = hasDanger;
                water[lx][lz] = hasWater;
                buildingForbidden[lx][lz] = hasForbidden;
                spaceAbove[lx][lz] = sa;
                boolean altitudeOk = surfaceY > baseline - 10 && surfaceY < baseline + 10;
                canBuild[lx][lz] = altitudeOk && !hasDanger && !hasForbidden;
            }
        }
        return new VillageTerrainMap(origin, size, baseline, topGround, canBuild, danger, water, buildingForbidden, spaceAbove);
    }

    public int toLocalX(int worldX) {
        return worldX - this.origin.getX();
    }

    public int toLocalZ(int worldZ) {
        return worldZ - this.origin.getZ();
    }

    public boolean inBounds(int localX, int localZ) {
        return localX >= 0 && localX < this.size && localZ >= 0 && localZ < this.size;
    }

    public int testFootprint(int worldX, int worldZ, int width, int depth, ClearMargins margins, Rotation rotation) {
        boolean hugeBuilding;
        FootprintRect rect = VillageTerrainMap.computeFootprintRect(worldX, worldZ, width, depth, margins, rotation);
        int surface = rect.width() * rect.depth();
        boolean bl = hugeBuilding = surface > 2000;
        int allowedErrors = surface > 2000 ? surface / 10 : (surface > 200 ? surface / 20 : 10);
        int nbError = 0;
        for (int dx = 0; dx < rect.width(); ++dx) {
            for (int dz = 0; dz < rect.depth(); ++dz) {
                int lz;
                int lx = this.toLocalX(rect.startX() + dx);
                if (!this.inBounds(lx, lz = this.toLocalZ(rect.startZ() + dz))) {
                    return -1;
                }
                if (this.occupied[lx][lz]) {
                    return -1;
                }
                if (this.buildingForbidden[lx][lz]) {
                    if (!hugeBuilding || nbError > allowedErrors) {
                        return -1;
                    }
                    ++nbError;
                    continue;
                }
                if (this.danger[lx][lz]) {
                    if (nbError > allowedErrors) {
                        return -1;
                    }
                    ++nbError;
                    continue;
                }
                if (this.canBuild[lx][lz]) continue;
                if (nbError > allowedErrors) {
                    return -1;
                }
                ++nbError;
            }
        }
        return nbError;
    }

    public int computeAverageAltitude(int worldX, int worldZ, int width, int depth, ClearMargins margins, Rotation rotation) {
        FootprintRect rect = VillageTerrainMap.computeFootprintRect(worldX, worldZ, width, depth, margins, rotation);
        long altitudeTotal = 0L;
        int nbPoints = 0;
        for (int dx = 0; dx < rect.width(); ++dx) {
            for (int dz = 0; dz < rect.depth(); ++dz) {
                int lz;
                int lx = this.toLocalX(rect.startX() + dx);
                if (!this.inBounds(lx, lz = this.toLocalZ(rect.startZ() + dz))) continue;
                altitudeTotal += (long)this.topGround[lx][lz];
                ++nbPoints;
            }
        }
        if (nbPoints == 0) {
            return this.baseline;
        }
        return Math.round((float)altitudeTotal / (float)nbPoints);
    }

    public void markBuildingFootprint(int worldX, int worldZ, int width, int depth, ClearMargins margins, Rotation rotation, int buildingY) {
        int lz;
        int lx;
        int dz;
        int dx;
        FootprintRect fullRect = VillageTerrainMap.computeFootprintRect(worldX, worldZ, width, depth, margins, rotation);
        FootprintRect coreRect = VillageTerrainMap.computeFootprintRect(worldX, worldZ, width, depth, ClearMargins.symmetric(0), rotation);
        for (dx = 0; dx < coreRect.width(); ++dx) {
            for (dz = 0; dz < coreRect.depth(); ++dz) {
                lx = this.toLocalX(coreRect.startX() + dx);
                if (!this.inBounds(lx, lz = this.toLocalZ(coreRect.startZ() + dz))) continue;
                this.occupied[lx][lz] = true;
            }
        }
        for (dx = 0; dx < coreRect.width(); ++dx) {
            for (dz = 0; dz < coreRect.depth(); ++dz) {
                lx = this.toLocalX(coreRect.startX() + dx);
                if (!this.inBounds(lx, lz = this.toLocalZ(coreRect.startZ() + dz))) continue;
                this.topGround[lx][lz] = buildingY;
                this.spaceAbove[lx][lz] = 3;
                this.water[lx][lz] = false;
                this.danger[lx][lz] = false;
                this.canBuild[lx][lz] = true;
            }
        }
        for (dx = 0; dx < fullRect.width(); ++dx) {
            for (dz = 0; dz < fullRect.depth(); ++dz) {
                int lz2;
                int lx2;
                int wx = fullRect.startX() + dx;
                int wz = fullRect.startZ() + dz;
                if (wx >= coreRect.startX() && wx < coreRect.startX() + coreRect.width() && wz >= coreRect.startZ() && wz < coreRect.startZ() + coreRect.depth() || !this.inBounds(lx2 = this.toLocalX(wx), lz2 = this.toLocalZ(wz)) || this.occupied[lx2][lz2]) continue;
                this.canBuild[lx2][lz2] = true;
                this.water[lx2][lz2] = false;
                this.danger[lx2][lz2] = false;
            }
        }
    }

    public void markOccupied(int worldX, int worldZ, int width, int depth, Rotation rotation) {
        this.markOccupied(worldX, worldZ, width, depth, ClearMargins.symmetric(0), rotation);
    }

    public void markOccupied(int worldX, int worldZ, int width, int depth, ClearMargins margins, Rotation rotation) {
        FootprintRect rect = VillageTerrainMap.computeFootprintRect(worldX, worldZ, width, depth, margins, rotation);
        for (int dx = 0; dx < rect.width(); ++dx) {
            for (int dz = 0; dz < rect.depth(); ++dz) {
                int lz;
                int lx = this.toLocalX(rect.startX() + dx);
                if (!this.inBounds(lx, lz = this.toLocalZ(rect.startZ() + dz))) continue;
                this.occupied[lx][lz] = true;
            }
        }
    }

    public int getSize() {
        return this.size;
    }

    public int getBaseline() {
        return this.baseline;
    }

    public int getGroundHeight(int worldX, int worldZ) {
        int lz;
        int lx = this.toLocalX(worldX);
        if (!this.inBounds(lx, lz = this.toLocalZ(worldZ))) {
            return this.baseline;
        }
        return this.topGround[lx][lz];
    }

    public int getTopGround(int localX, int localZ) {
        return this.inBounds(localX, localZ) ? this.topGround[localX][localZ] : 0;
    }

    public boolean canBuildAt(int localX, int localZ) {
        return this.inBounds(localX, localZ) && this.canBuild[localX][localZ];
    }

    public boolean isDangerAt(int localX, int localZ) {
        return this.inBounds(localX, localZ) && this.danger[localX][localZ];
    }

    public boolean isWaterAt(int localX, int localZ) {
        return this.inBounds(localX, localZ) && this.water[localX][localZ];
    }

    public boolean isBuildingForbiddenAt(int localX, int localZ) {
        return this.inBounds(localX, localZ) && this.buildingForbidden[localX][localZ];
    }

    public short getSpaceAbove(int localX, int localZ) {
        return this.inBounds(localX, localZ) ? this.spaceAbove[localX][localZ] : (short)0;
    }

    public boolean isOccupied(int localX, int localZ) {
        return this.inBounds(localX, localZ) && this.occupied[localX][localZ];
    }

    public static FootprintRect computeFootprintRect(int worldX, int worldZ, int width, int depth, ClearMargins margins, Rotation rotation) {
        int effectiveWidth;
        int startZ;
        int startX;
        ClearMargins effective = margins.forRotation(rotation);
        return new FootprintRect(startX, startZ, effectiveWidth, switch (rotation) {
            case Rotation.CLOCKWISE_90 -> {
                startX = worldX - depth + 1 - effective.lengthBefore();
                startZ = worldZ - effective.widthBefore();
                effectiveWidth = depth + effective.lengthBefore() + effective.lengthAfter();
                yield width + effective.widthBefore() + effective.widthAfter();
            }
            case Rotation.CLOCKWISE_180 -> {
                startX = worldX - width + 1 - effective.lengthBefore();
                startZ = worldZ - depth + 1 - effective.widthBefore();
                effectiveWidth = width + effective.lengthBefore() + effective.lengthAfter();
                yield depth + effective.widthBefore() + effective.widthAfter();
            }
            case Rotation.COUNTERCLOCKWISE_90 -> {
                startX = worldX - effective.lengthBefore();
                startZ = worldZ - width + 1 - effective.widthBefore();
                effectiveWidth = depth + effective.lengthBefore() + effective.lengthAfter();
                yield width + effective.widthBefore() + effective.widthAfter();
            }
            default -> {
                startX = worldX - effective.lengthBefore();
                startZ = worldZ - effective.widthBefore();
                effectiveWidth = width + effective.lengthBefore() + effective.lengthAfter();
                yield depth + effective.widthBefore() + effective.widthAfter();
            }
        });
    }

    public record FootprintRect(int startX, int startZ, int width, int depth) {
    }
}

