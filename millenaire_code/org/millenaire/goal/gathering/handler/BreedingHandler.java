/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.google.gson.JsonArray
 *  com.google.gson.JsonElement
 *  com.google.gson.JsonObject
 *  com.mojang.logging.LogUtils
 *  javax.annotation.Nullable
 *  net.minecraft.core.BlockPos
 *  net.minecraft.server.level.ServerLevel
 *  net.minecraft.util.GsonHelper
 *  net.minecraft.world.entity.Entity
 *  net.minecraft.world.entity.EntityType
 *  net.minecraft.world.entity.animal.Animal
 *  net.minecraft.world.item.Item
 *  net.minecraft.world.item.ItemStack
 *  net.minecraft.world.item.Items
 *  net.minecraft.world.level.ItemLike
 *  net.minecraft.world.level.Level
 *  net.minecraft.world.phys.AABB
 *  org.slf4j.Logger
 */
package org.millenaire.goal.gathering.handler;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.mojang.logging.LogUtils;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import javax.annotation.Nullable;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.GsonHelper;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.animal.Animal;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.ItemLike;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import org.millenaire.building.BuildingInstance;
import org.millenaire.building.BuildingInventory;
import org.millenaire.goal.GoalContext;
import org.millenaire.goal.gathering.GatheringTarget;
import org.millenaire.goal.gathering.GatheringType;
import org.millenaire.goal.gathering.handler.AbstractGatheringHandler;
import org.millenaire.item.ItemHelper;
import org.slf4j.Logger;

public class BreedingHandler
extends AbstractGatheringHandler {
    private static final Logger LOGGER = LogUtils.getLogger();
    private static final int SCAN_RADIUS_XZ = 15;
    private static final int SCAN_RADIUS_Y = 10;
    private static final int MIN_ADULTS_FOR_BREEDING = 2;

    @Override
    public String id() {
        return "breeding";
    }

    @Override
    public Item getDefaultHeldItem(GatheringType type) {
        return Items.WHEAT;
    }

    @Override
    public List<String> validate(GatheringType type) {
        ArrayList<String> errors = new ArrayList<String>();
        if (this.getBuildingTag(type) == null) {
            errors.add("missing required handlerParam 'buildingTag'");
        }
        errors.addAll(BreedingHandler.validateItemIdList(type, "foodItems", true));
        String animalId = GsonHelper.getAsString((JsonObject)type.handlerParams(), (String)"animalType", null);
        if (animalId == null) {
            errors.add("missing required handlerParam 'animalType'");
        } else if (EntityType.byString((String)animalId).isEmpty()) {
            errors.add("unresolvable entity type '" + animalId + "' in 'animalType'");
        }
        return errors;
    }

    @Override
    public boolean canStart(GoalContext ctx, GatheringType type) {
        String buildingTag = this.getBuildingTag(type);
        if (buildingTag == null) {
            return false;
        }
        EntityType<?> animalType = this.getAnimalType(type);
        if (animalType == null) {
            return false;
        }
        List<Item> foodItems = this.getFoodItems(type);
        if (foodItems.isEmpty()) {
            return false;
        }
        List<BuildingInstance> farms = this.findBuildingsWithTag(ctx.village(), buildingTag);
        if (farms.isEmpty()) {
            return false;
        }
        ServerLevel level = ctx.level();
        for (BuildingInstance farm : farms) {
            if (!this.hasFoodInBuilding(farm, level, foodItems) || !this.canBreedAtFarm(level, farm, animalType, foodItems)) continue;
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
        EntityType<?> animalType = this.getAnimalType(type);
        if (animalType == null) {
            return null;
        }
        List<Item> foodItems = this.getFoodItems(type);
        if (foodItems.isEmpty()) {
            return null;
        }
        ServerLevel level = ctx.level();
        List<BuildingInstance> farms = this.findBuildingsWithTag(ctx.village(), buildingTag);
        BlockPos reference = lastTarget != null ? lastTarget.navigationPos() : ctx.villager().blockPosition();
        ArrayList<Animal> breedable = new ArrayList<Animal>();
        for (BuildingInstance farm : farms) {
            if (!this.hasFoodInBuilding(farm, level, foodItems) || !this.canBreedAtFarm(level, farm, animalType, foodItems)) continue;
            breedable.addAll(this.findBreedableAnimals(level, farm, animalType, foodItems));
        }
        Animal bestAnimal = (Animal)BreedingHandler.findClosestEntity(breedable, reference, lastTarget, type.batchRadius());
        return bestAnimal != null ? new GatheringTarget.EntityTarget((Entity)bestAnimal) : null;
    }

    @Override
    public boolean isTargetStillValid(GoalContext ctx, GatheringType type, GatheringTarget target) {
        if (!(target instanceof GatheringTarget.EntityTarget)) {
            return true;
        }
        GatheringTarget.EntityTarget entityTarget = (GatheringTarget.EntityTarget)target;
        Entity entity = entityTarget.entity();
        if (!(entity instanceof Animal)) {
            return false;
        }
        Animal animal = (Animal)entity;
        return animal.isAlive() && !animal.isBaby() && !animal.isInLove() && animal.canFallInLove();
    }

    @Override
    public boolean performAction(GoalContext ctx, GatheringType type, GatheringTarget target) {
        if (!(target instanceof GatheringTarget.EntityTarget)) {
            return true;
        }
        GatheringTarget.EntityTarget entityTarget = (GatheringTarget.EntityTarget)target;
        Entity entity = entityTarget.entity();
        if (!(entity instanceof Animal)) {
            return true;
        }
        Animal animal = (Animal)entity;
        if (!animal.isAlive() || animal.isBaby() || animal.isInLove() || !animal.canFallInLove()) {
            return true;
        }
        List<Item> foodItems = this.getFoodItems(type);
        String buildingTag = this.getBuildingTag(type);
        Item usedFood = null;
        BuildingInstance foodSource = null;
        if (buildingTag != null) {
            for (BuildingInstance farm : this.findBuildingsWithTag(ctx.village(), buildingTag)) {
                BuildingInventory inv = farm.getInventory();
                if (inv == null) continue;
                for (Item food : foodItems) {
                    if (inv.getCount((Level)ctx.level(), food) < 1) continue;
                    usedFood = food;
                    foodSource = farm;
                    break;
                }
                if (usedFood == null) continue;
                break;
            }
        }
        if (usedFood == null || foodSource == null) {
            return true;
        }
        if (!animal.isFood(new ItemStack(usedFood))) {
            return true;
        }
        foodSource.getInventory().remove((Level)ctx.level(), usedFood, 1);
        animal.setInLove(null);
        LOGGER.debug("Breeding: animal {} put in love mode at {}", (Object)animal.getType().getDescriptionId(), (Object)animal.blockPosition());
        return true;
    }

    private boolean hasFoodInBuilding(BuildingInstance building, ServerLevel level, List<Item> foodItems) {
        BuildingInventory inv = building.getInventory();
        if (inv == null) {
            return false;
        }
        for (Item food : foodItems) {
            if (inv.getCount((Level)level, food) < 1) continue;
            return true;
        }
        return false;
    }

    private List<Item> getFoodItems(GatheringType type) {
        JsonObject params = type.handlerParams();
        ArrayList<Item> items = new ArrayList<Item>();
        if (!params.has("foodItems")) {
            return items;
        }
        JsonArray array = params.getAsJsonArray("foodItems");
        for (JsonElement elem : array) {
            String itemId = elem.getAsString();
            Item item = ItemHelper.resolve(itemId);
            if (item == null || item == Items.AIR) continue;
            items.add(item);
        }
        return items;
    }

    private List<Animal> findBreedableAnimals(ServerLevel level, BuildingInstance farm, EntityType<?> animalType, List<Item> foodItems) {
        AABB searchBox = BreedingHandler.scanBoxAround(farm.getOrigin(), 15, 10);
        ArrayList<Animal> result = new ArrayList<Animal>();
        block0: for (Animal animal : this.findAnimalsOfType(level, searchBox, animalType)) {
            if (!animal.isAlive() || animal.isBaby() || animal.isInLove() || !animal.canFallInLove()) continue;
            for (Item food : foodItems) {
                if (!animal.isFood(new ItemStack((ItemLike)food))) continue;
                result.add(animal);
                continue block0;
            }
        }
        return result;
    }

    private List<Animal> findAnimalsOfType(ServerLevel level, AABB box, EntityType<?> animalType) {
        ArrayList<Animal> result = new ArrayList<Animal>();
        List entities = level.getEntities((Entity)null, box, e -> e.getType() == animalType && e instanceof Animal && e.isAlive());
        for (Entity e2 : entities) {
            result.add((Animal)e2);
        }
        return result;
    }

    private boolean canBreedAtFarm(ServerLevel level, BuildingInstance farm, EntityType<?> animalType, List<Item> foodItems) {
        int spawnCount;
        List<Animal> breedable = this.findBreedableAnimals(level, farm, animalType, foodItems);
        if (breedable.size() < 2) {
            return false;
        }
        AABB searchBox = BreedingHandler.scanBoxAround(farm.getOrigin(), 15, 10);
        int totalAnimals = this.findAnimalsOfType(level, searchBox, animalType).size();
        return totalAnimals < (spawnCount = BreedingHandler.countAnimalSpawnPoints(farm)) * 2;
    }

    @Nullable
    private EntityType<?> getAnimalType(GatheringType type) {
        String animalId = GsonHelper.getAsString((JsonObject)type.handlerParams(), (String)"animalType", null);
        if (animalId == null) {
            return null;
        }
        Optional opt = EntityType.byString((String)animalId);
        return opt.orElse(null);
    }
}

