/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.mojang.logging.LogUtils
 *  javax.annotation.Nullable
 *  net.minecraft.core.BlockPos
 *  net.minecraft.server.level.ServerLevel
 *  net.minecraft.world.entity.Entity
 *  net.minecraft.world.entity.animal.Sheep
 *  net.minecraft.world.item.DyeColor
 *  net.minecraft.world.item.Item
 *  net.minecraft.world.item.Items
 *  net.minecraft.world.phys.AABB
 *  org.slf4j.Logger
 */
package org.millenaire.goal.gathering.handler;

import com.mojang.logging.LogUtils;
import java.util.ArrayList;
import java.util.List;
import javax.annotation.Nullable;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.animal.Sheep;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
import net.minecraft.world.phys.AABB;
import org.millenaire.building.BuildingInstance;
import org.millenaire.goal.GoalContext;
import org.millenaire.goal.gathering.GatheringTarget;
import org.millenaire.goal.gathering.GatheringType;
import org.millenaire.goal.gathering.handler.AbstractGatheringHandler;
import org.slf4j.Logger;

public class ShearingHandler
extends AbstractGatheringHandler {
    private static final Logger LOGGER = LogUtils.getLogger();
    private static final int SCAN_RADIUS_XZ = 30;
    private static final int SCAN_RADIUS_Y = 10;
    private static final int WOOL_PER_SHEAR = 3;

    @Override
    public String id() {
        return "shearing";
    }

    @Override
    public Item getDefaultHeldItem(GatheringType type) {
        return Items.SHEARS;
    }

    @Override
    public List<String> validate(GatheringType type) {
        if (this.getBuildingTag(type) == null) {
            return List.of("missing required handlerParam 'buildingTag'");
        }
        return List.of();
    }

    @Override
    public boolean canStart(GoalContext ctx, GatheringType type) {
        String buildingTag = this.getBuildingTag(type);
        if (buildingTag == null) {
            return false;
        }
        List<BuildingInstance> farms = this.findBuildingsWithTag(ctx.village(), buildingTag);
        if (farms.isEmpty()) {
            return false;
        }
        ServerLevel level = ctx.level();
        for (BuildingInstance farm : farms) {
            List<Sheep> shearable = this.findShearableSheep(level, farm);
            if (shearable.isEmpty()) continue;
            return true;
        }
        return false;
    }

    @Override
    @Nullable
    public GatheringTarget findTarget(GoalContext ctx, GatheringType type, @Nullable GatheringTarget lastTarget) {
        String buildingTag = this.getBuildingTag(type);
        if (buildingTag == null) {
            return null;
        }
        ServerLevel level = ctx.level();
        List<BuildingInstance> farms = this.findBuildingsWithTag(ctx.village(), buildingTag);
        BlockPos reference = lastTarget != null ? lastTarget.navigationPos() : ctx.villager().blockPosition();
        ArrayList<Sheep> allShearable = new ArrayList<Sheep>();
        for (BuildingInstance farm : farms) {
            allShearable.addAll(this.findShearableSheep(level, farm));
        }
        Sheep bestSheep = (Sheep)ShearingHandler.findClosestEntity(allShearable, reference, lastTarget, type.batchRadius());
        return bestSheep != null ? new GatheringTarget.EntityTarget((Entity)bestSheep) : null;
    }

    @Override
    public boolean isTargetStillValid(GoalContext ctx, GatheringType type, GatheringTarget target) {
        if (!(target instanceof GatheringTarget.EntityTarget)) {
            return true;
        }
        GatheringTarget.EntityTarget entityTarget = (GatheringTarget.EntityTarget)target;
        Entity entity = entityTarget.entity();
        if (!(entity instanceof Sheep)) {
            return false;
        }
        Sheep sheep = (Sheep)entity;
        return sheep.isAlive() && !sheep.isBaby() && !sheep.isSheared();
    }

    @Override
    public boolean performAction(GoalContext ctx, GatheringType type, GatheringTarget target) {
        if (!(target instanceof GatheringTarget.EntityTarget)) {
            return true;
        }
        GatheringTarget.EntityTarget entityTarget = (GatheringTarget.EntityTarget)target;
        Entity entity = entityTarget.entity();
        if (!(entity instanceof Sheep)) {
            return true;
        }
        Sheep sheep = (Sheep)entity;
        if (!sheep.isAlive() || sheep.isBaby() || sheep.isSheared()) {
            return true;
        }
        sheep.setSheared(true);
        Item woolItem = this.getWoolForColor(sheep.getColor());
        ctx.villager().getInventory().add(woolItem, 3);
        LOGGER.debug("Shearing: sheep sheared at {}, {} wool {}", new Object[]{sheep.blockPosition(), 3, woolItem});
        return true;
    }

    private List<Sheep> findShearableSheep(ServerLevel level, BuildingInstance farm) {
        AABB searchBox = ShearingHandler.scanBoxAround(farm.getOrigin(), 30, 10);
        ArrayList<Sheep> result = new ArrayList<Sheep>();
        List sheep = level.getEntitiesOfClass(Sheep.class, searchBox);
        for (Sheep s : sheep) {
            if (!s.isAlive() || s.isBaby() || s.isSheared()) continue;
            result.add(s);
        }
        return result;
    }

    private Item getWoolForColor(DyeColor color) {
        return switch (color) {
            default -> throw new MatchException(null, null);
            case DyeColor.WHITE -> Items.WHITE_WOOL;
            case DyeColor.ORANGE -> Items.ORANGE_WOOL;
            case DyeColor.MAGENTA -> Items.MAGENTA_WOOL;
            case DyeColor.LIGHT_BLUE -> Items.LIGHT_BLUE_WOOL;
            case DyeColor.YELLOW -> Items.YELLOW_WOOL;
            case DyeColor.LIME -> Items.LIME_WOOL;
            case DyeColor.PINK -> Items.PINK_WOOL;
            case DyeColor.GRAY -> Items.GRAY_WOOL;
            case DyeColor.LIGHT_GRAY -> Items.LIGHT_GRAY_WOOL;
            case DyeColor.CYAN -> Items.CYAN_WOOL;
            case DyeColor.PURPLE -> Items.PURPLE_WOOL;
            case DyeColor.BLUE -> Items.BLUE_WOOL;
            case DyeColor.BROWN -> Items.BROWN_WOOL;
            case DyeColor.GREEN -> Items.GREEN_WOOL;
            case DyeColor.RED -> Items.RED_WOOL;
            case DyeColor.BLACK -> Items.BLACK_WOOL;
        };
    }
}

