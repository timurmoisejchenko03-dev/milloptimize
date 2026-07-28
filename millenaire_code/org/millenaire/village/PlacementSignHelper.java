/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.mojang.logging.LogUtils
 *  net.minecraft.core.BlockPos
 *  net.minecraft.network.chat.Component
 *  net.minecraft.server.level.ServerLevel
 *  net.minecraft.world.item.DyeColor
 *  net.minecraft.world.level.block.Blocks
 *  net.minecraft.world.level.block.StandingSignBlock
 *  net.minecraft.world.level.block.entity.BlockEntity
 *  net.minecraft.world.level.block.entity.SignBlockEntity
 *  net.minecraft.world.level.block.entity.SignText
 *  net.minecraft.world.level.block.state.BlockState
 *  net.minecraft.world.level.block.state.properties.Property
 *  org.slf4j.Logger
 */
package org.millenaire.village;

import com.mojang.logging.LogUtils;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.StandingSignBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.SignBlockEntity;
import net.minecraft.world.level.block.entity.SignText;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.Property;
import org.millenaire.building.BuildingPlan;
import org.millenaire.building.ClearMargins;
import org.millenaire.world.PlacedLocation;
import org.millenaire.world.TerrainPreparer;
import org.millenaire.world.VillageTerrainMap;
import org.slf4j.Logger;

public final class PlacementSignHelper {
    private static final Logger LOGGER = LogUtils.getLogger();

    private PlacementSignHelper() {
    }

    public static void placeCornerSigns(ServerLevel level, BuildingPlan plan, PlacedLocation location, String projectName) {
        for (BlockPos corner : PlacementSignHelper.footprintCorners(plan, location)) {
            PlacementSignHelper.placeSign(level, corner.getX(), corner.getZ(), projectName);
        }
    }

    public static void removeCornerSigns(ServerLevel level, BuildingPlan plan, PlacedLocation location) {
        for (BlockPos corner : PlacementSignHelper.footprintCorners(plan, location)) {
            PlacementSignHelper.removeSignAt(level, corner.getX(), corner.getZ());
        }
    }

    private static BlockPos[] footprintCorners(BuildingPlan plan, PlacedLocation location) {
        VillageTerrainMap.FootprintRect rect = VillageTerrainMap.computeFootprintRect(location.position().getX(), location.position().getZ(), plan.width(), plan.depth(), ClearMargins.symmetric(0), location.rotation());
        int x1 = rect.startX();
        int z1 = rect.startZ();
        int x2 = rect.startX() + rect.width() - 1;
        int z2 = rect.startZ() + rect.depth() - 1;
        return new BlockPos[]{new BlockPos(x1, 0, z1), new BlockPos(x2, 0, z1), new BlockPos(x1, 0, z2), new BlockPos(x2, 0, z2)};
    }

    private static void placeSign(ServerLevel level, int x, int z, String label) {
        BlockState signState;
        int placeY = TerrainPreparer.getGroundHeight(level, x, z);
        BlockPos pos = new BlockPos(x, placeY, z);
        if (!level.setBlock(pos, signState = (BlockState)Blocks.OAK_SIGN.defaultBlockState().setValue((Property)StandingSignBlock.ROTATION, (Comparable)Integer.valueOf(8)), 3)) {
            return;
        }
        BlockEntity blockEntity = level.getBlockEntity(pos);
        if (blockEntity instanceof SignBlockEntity) {
            SignBlockEntity sign = (SignBlockEntity)blockEntity;
            SignText text = new SignText().setMessage(0, (Component)Component.literal((String)label)).setMessage(1, (Component)Component.empty()).setMessage(2, (Component)Component.literal((String)"(project)")).setMessage(3, (Component)Component.empty()).setColor(DyeColor.BLACK);
            sign.setText(text, true);
            sign.setText(text, false);
            sign.setChanged();
            level.sendBlockUpdated(pos, signState, signState, 3);
        } else {
            LOGGER.warn("[Millenaire] Placement sign at {} has no SignBlockEntity", (Object)pos);
        }
    }

    private static void removeSignAt(ServerLevel level, int x, int z) {
        int placeY = TerrainPreparer.getGroundHeight(level, x, z);
        for (int dy = -2; dy <= 3; ++dy) {
            BlockPos pos = new BlockPos(x, placeY + dy, z);
            BlockState state = level.getBlockState(pos);
            if (!state.is(Blocks.OAK_SIGN)) continue;
            level.setBlock(pos, Blocks.AIR.defaultBlockState(), 3);
            return;
        }
    }
}

