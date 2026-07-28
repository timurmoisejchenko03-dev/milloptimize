/*
 * Decompiled with CFR 0.150.
 * 
 * Could not load the following classes:
 *  com.mojang.logging.LogUtils
 *  net.minecraft.core.BlockPos
 *  net.minecraft.core.Direction
 *  net.minecraft.server.level.ServerLevel
 *  net.minecraft.world.item.BlockItem
 *  net.minecraft.world.item.DyeColor
 *  net.minecraft.world.item.Item
 *  net.minecraft.world.item.ItemStack
 *  net.minecraft.world.level.block.AbstractBannerBlock
 *  net.minecraft.world.level.block.BannerBlock
 *  net.minecraft.world.level.block.Block
 *  net.minecraft.world.level.block.Blocks
 *  net.minecraft.world.level.block.WallBannerBlock
 *  net.minecraft.world.level.block.entity.BannerBlockEntity
 *  net.minecraft.world.level.block.entity.BlockEntity
 *  net.minecraft.world.level.block.state.BlockState
 *  net.minecraft.world.level.block.state.properties.Property
 *  org.slf4j.Logger
 */
package org.millenaire.village;

import com.mojang.logging.LogUtils;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.AbstractBannerBlock;
import net.minecraft.world.level.block.BannerBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.WallBannerBlock;
import net.minecraft.world.level.block.entity.BannerBlockEntity;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.Property;
import org.millenaire.building.BuildingInstance;
import org.millenaire.building.SpecialPoint;
import org.millenaire.culture.Culture;
import org.millenaire.culture.ModCultures;
import org.millenaire.village.Village;
import org.millenaire.village.VillageBannerService;
import org.slf4j.Logger;

public final class VillageBannerApplier {
    private static final Logger LOGGER = LogUtils.getLogger();
    private static final Map<DyeColor, Block> WALL_BANNER_BY_COLOR = VillageBannerApplier.buildWallBannerMap();

    private VillageBannerApplier() {
    }

    private static Map<DyeColor, Block> buildWallBannerMap() {
        EnumMap<DyeColor, Block> m = new EnumMap<DyeColor, Block>(DyeColor.class);
        m.put(DyeColor.WHITE, Blocks.WHITE_WALL_BANNER);
        m.put(DyeColor.ORANGE, Blocks.ORANGE_WALL_BANNER);
        m.put(DyeColor.MAGENTA, Blocks.MAGENTA_WALL_BANNER);
        m.put(DyeColor.LIGHT_BLUE, Blocks.LIGHT_BLUE_WALL_BANNER);
        m.put(DyeColor.YELLOW, Blocks.YELLOW_WALL_BANNER);
        m.put(DyeColor.LIME, Blocks.LIME_WALL_BANNER);
        m.put(DyeColor.PINK, Blocks.PINK_WALL_BANNER);
        m.put(DyeColor.GRAY, Blocks.GRAY_WALL_BANNER);
        m.put(DyeColor.LIGHT_GRAY, Blocks.LIGHT_GRAY_WALL_BANNER);
        m.put(DyeColor.CYAN, Blocks.CYAN_WALL_BANNER);
        m.put(DyeColor.PURPLE, Blocks.PURPLE_WALL_BANNER);
        m.put(DyeColor.BLUE, Blocks.BLUE_WALL_BANNER);
        m.put(DyeColor.BROWN, Blocks.BROWN_WALL_BANNER);
        m.put(DyeColor.GREEN, Blocks.GREEN_WALL_BANNER);
        m.put(DyeColor.RED, Blocks.RED_WALL_BANNER);
        m.put(DyeColor.BLACK, Blocks.BLACK_WALL_BANNER);
        return Map.copyOf(m);
    }

    public static void applyBanners(ServerLevel level, BuildingInstance building, Village village) {
        List<SpecialPoint> points = building.getResolvedPoints();
        if (points.isEmpty()) {
            return;
        }
        for (SpecialPoint sp : points) {
            if (!sp.isType("banner")) continue;
            VillageBannerApplier.applyOne(level, sp, village);
        }
    }

    private static void applyOne(ServerLevel level, SpecialPoint sp, Village village) {
        BlockState replacement;
        ItemStack bannerStack = VillageBannerApplier.resolveBannerStack(level, sp, village);
        if (bannerStack.isEmpty()) {
            return;
        }
        DyeColor color = VillageBannerApplier.extractColor(bannerStack);
        if (color == null) {
            LOGGER.warn("[banner] resolved banner stack has no DyeColor: {}", (Object)bannerStack);
            return;
        }
        BlockPos pos = sp.pos();
        BlockState current = level.getBlockState(pos);
        Block currentBlock = current.getBlock();
        if (currentBlock instanceof WallBannerBlock) {
            Direction facing = (Direction)current.getValue((Property)WallBannerBlock.FACING);
            Block dyed = WALL_BANNER_BY_COLOR.get((Object)color);
            if (dyed == null) {
                LOGGER.warn("[banner] no wall banner for color {}", (Object)color);
                return;
            }
            replacement = (BlockState)dyed.defaultBlockState().setValue((Property)WallBannerBlock.FACING, (Comparable)facing);
        } else if (currentBlock instanceof BannerBlock) {
            int rotation = (Integer)current.getValue((Property)BannerBlock.ROTATION);
            replacement = (BlockState)BannerBlock.byColor((DyeColor)color).defaultBlockState().setValue((Property)BannerBlock.ROTATION, (Comparable)Integer.valueOf(rotation));
        } else {
            LOGGER.warn("[banner] expected a banner block at {}, found {}", (Object)pos, (Object)currentBlock);
            return;
        }
        level.setBlock(pos, replacement, 3);
        BlockEntity blockEntity = level.getBlockEntity(pos);
        if (blockEntity instanceof BannerBlockEntity) {
            BannerBlockEntity be = (BannerBlockEntity)blockEntity;
            be.applyComponentsFromItemStack(bannerStack);
            be.setChanged();
            level.sendBlockUpdated(pos, replacement, replacement, 2);
        }
    }

    private static ItemStack resolveBannerStack(ServerLevel level, SpecialPoint sp, Village village) {
        String subtype = sp.subtype();
        if ("culture".equals(subtype)) {
            if (village == null) {
                return ItemStack.EMPTY;
            }
            Culture culture = ModCultures.getCulture(village.getCultureId());
            if (culture == null) {
                return ItemStack.EMPTY;
            }
            return VillageBannerService.getCultureBanner(culture, level.registryAccess());
        }
        if (village == null) {
            return ItemStack.EMPTY;
        }
        return village.getBannerStack(level.registryAccess());
    }

    private static DyeColor extractColor(ItemStack stack) {
        BlockItem bi;
        Item item = stack.getItem();
        if (item instanceof BlockItem && (item = (bi = (BlockItem)item).getBlock()) instanceof AbstractBannerBlock) {
            AbstractBannerBlock ab = (AbstractBannerBlock)item;
            return ab.getColor();
        }
        return null;
    }
}

