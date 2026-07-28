/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.mojang.logging.LogUtils
 *  net.minecraft.core.BlockPos
 *  net.minecraft.resources.ResourceLocation
 *  net.minecraft.server.level.ServerLevel
 *  net.minecraft.world.Container
 *  net.minecraft.world.item.Item
 *  net.minecraft.world.item.ItemStack
 *  net.minecraft.world.level.ItemLike
 *  net.minecraft.world.level.block.ChestBlock
 *  net.minecraft.world.level.block.entity.BlockEntity
 *  net.minecraft.world.level.levelgen.structure.templatesystem.StructurePlaceSettings
 *  net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplate
 *  org.slf4j.Logger
 */
package org.millenaire.village;

import com.mojang.logging.LogUtils;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.Container;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ItemLike;
import net.minecraft.world.level.block.ChestBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructurePlaceSettings;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplate;
import org.millenaire.building.BuildingInstance;
import org.millenaire.building.BuildingInventory;
import org.millenaire.building.BuildingPlan;
import org.millenaire.building.BuildingPlanSet;
import org.millenaire.culture.ModCultures;
import org.millenaire.item.ItemHelper;
import org.slf4j.Logger;

public final class GoodsRestockHelper {
    private static final Logger LOGGER = LogUtils.getLogger();

    private GoodsRestockHelper() {
    }

    public static void fillStartingGoods(ServerLevel level, BuildingInstance building, BuildingPlanSet planSet, boolean initialSpawn) {
        List<BlockPos> chestPositions;
        BuildingInventory inv = building.getInventory();
        if (inv != null && inv.getChestCount() > 0) {
            chestPositions = new ArrayList<BlockPos>(inv.getChestPositions());
        } else if (initialSpawn) {
            chestPositions = GoodsRestockHelper.scanVanillaChests(level, building, planSet);
        } else {
            return;
        }
        if (chestPositions.isEmpty()) {
            return;
        }
        for (BlockPos chestPos : chestPositions) {
            BlockEntity be;
            if (!level.isLoaded(chestPos) || !((be = level.getBlockEntity(chestPos)) instanceof Container)) continue;
            Container container = (Container)be;
            container.clearContent();
        }
        for (BuildingPlanSet.StartingGood sg : planSet.startingGoods()) {
            BlockEntity be;
            if (ThreadLocalRandom.current().nextDouble() >= sg.probability()) continue;
            int nb = sg.fixedNumber();
            if (sg.randomNumber() > 0) {
                nb += ThreadLocalRandom.current().nextInt(sg.randomNumber() + 1);
            }
            if (nb <= 0) continue;
            Item item = ItemHelper.resolve(sg.item());
            if (item == null) {
                LOGGER.warn("[Millenaire] fillStartingGoods: unknown item: {}", (Object)sg.item());
                continue;
            }
            int chestIndex = ThreadLocalRandom.current().nextInt(chestPositions.size());
            BlockPos chestPos = chestPositions.get(chestIndex);
            if (!level.isLoaded(chestPos) || !((be = level.getBlockEntity(chestPos)) instanceof Container)) continue;
            Container container = (Container)be;
            int remaining = nb;
            for (int slot = 0; slot < container.getContainerSize() && remaining > 0; ++slot) {
                ItemStack existing = container.getItem(slot);
                if (!existing.isEmpty()) continue;
                int toPlace = Math.min(remaining, item.getDefaultMaxStackSize());
                container.setItem(slot, new ItemStack((ItemLike)item, toPlace));
                remaining -= toPlace;
            }
        }
        if (inv != null) {
            inv.invalidateCache();
        }
    }

    static List<BlockPos> scanVanillaChests(ServerLevel level, BuildingInstance building, BuildingPlanSet planSet) {
        ArrayList<BlockPos> result = new ArrayList<BlockPos>();
        BlockPos origin = building.getOrigin();
        if (origin == null) {
            return result;
        }
        ResourceLocation planId = planSet.getPlanId(building.getVariant(), building.getLevel());
        if (planId == null) {
            return result;
        }
        BuildingPlan plan = ModCultures.getBuildingPlan(planId);
        if (plan == null) {
            return result;
        }
        StructurePlaceSettings settings = new StructurePlaceSettings().setRotation(building.getRotation());
        BlockPos cornerA = StructureTemplate.calculateRelativePosition((StructurePlaceSettings)settings, (BlockPos)BlockPos.ZERO);
        BlockPos cornerB = StructureTemplate.calculateRelativePosition((StructurePlaceSettings)settings, (BlockPos)new BlockPos(plan.width() - 1, plan.height() - 1, plan.depth() - 1));
        int minX = Math.min(cornerA.getX(), cornerB.getX());
        int maxX = Math.max(cornerA.getX(), cornerB.getX());
        int minY = Math.min(cornerA.getY(), cornerB.getY());
        int maxY = Math.max(cornerA.getY(), cornerB.getY());
        int minZ = Math.min(cornerA.getZ(), cornerB.getZ());
        int maxZ = Math.max(cornerA.getZ(), cornerB.getZ());
        for (int dx = minX; dx <= maxX; ++dx) {
            for (int dy = minY; dy <= maxY; ++dy) {
                for (int dz = minZ; dz <= maxZ; ++dz) {
                    BlockPos pos = origin.offset(dx, dy, dz);
                    if (!level.isLoaded(pos) || !(level.getBlockState(pos).getBlock() instanceof ChestBlock)) continue;
                    result.add(pos);
                }
            }
        }
        return result;
    }
}

