/*
 * Decompiled with CFR 0.150.
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
 *  net.minecraft.world.damagesource.DamageSource
 *  net.minecraft.world.entity.Entity
 *  net.minecraft.world.entity.EntityType
 *  net.minecraft.world.entity.LivingEntity
 *  net.minecraft.world.entity.Mob
 *  net.minecraft.world.entity.item.ItemEntity
 *  net.minecraft.world.item.Item
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
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.Item;
import net.minecraft.world.phys.AABB;
import org.millenaire.building.BuildingInstance;
import org.millenaire.goal.GoalContext;
import org.millenaire.goal.gathering.GatheringTarget;
import org.millenaire.goal.gathering.GatheringType;
import org.millenaire.goal.gathering.handler.AbstractGatheringHandler;
import org.millenaire.item.ItemHelper;
import org.slf4j.Logger;

public class SlaughterHandler
extends AbstractGatheringHandler {
    private static final Logger LOGGER = LogUtils.getLogger();
    private static final int SCAN_RADIUS_XZ = 25;
    private static final int SCAN_RADIUS_Y = 10;
    private static final int MIN_ANIMALS_KEEP = 2;

    @Override
    public String id() {
        return "slaughter";
    }

    @Override
    public String getHeldToolCategoryId(GatheringType type) {
        return "toolsaxe";
    }

    @Override
    public List<String> validate(GatheringType type) {
        String animalId;
        ArrayList<String> errors = new ArrayList<String>();
        if (this.getBuildingTag(type) == null) {
            errors.add("missing required handlerParam 'buildingTag'");
        }
        if ((animalId = GsonHelper.getAsString((JsonObject)type.handlerParams(), (String)"animalType", null)) == null) {
            errors.add("missing required handlerParam 'animalType'");
        } else if (EntityType.byString((String)animalId).isEmpty()) {
            errors.add("unresolvable entity type '" + animalId + "' in 'animalType'");
        }
        return errors;
    }

    @Override
    public boolean canStart(GoalContext ctx, GatheringType type) {
        String buildingTag = this.getBuildingTag(type);
        EntityType<?> animalType = this.getAnimalType(type);
        if (buildingTag == null || animalType == null) {
            return false;
        }
        List<BuildingInstance> farms = this.findBuildingsWithTag(ctx.village(), buildingTag);
        ServerLevel level = ctx.level();
        for (BuildingInstance farm : farms) {
            List<Mob> adults = this.findAdultMobs(level, farm, animalType);
            int animalSpawnCount = SlaughterHandler.countAnimalSpawnPoints(farm);
            if (adults.size() <= Math.max(animalSpawnCount, 2)) continue;
            return true;
        }
        return false;
    }

    @Override
    @Nullable
    public GatheringTarget findTarget(GoalContext ctx, GatheringType type, @Nullable GatheringTarget lastTarget) {
        String buildingTag = this.getBuildingTag(type);
        EntityType<?> animalType = this.getAnimalType(type);
        if (buildingTag == null || animalType == null) {
            return null;
        }
        ServerLevel level = ctx.level();
        List<BuildingInstance> farms = this.findBuildingsWithTag(ctx.village(), buildingTag);
        BlockPos reference = lastTarget != null ? lastTarget.navigationPos() : ctx.villager().blockPosition();
        ArrayList<Mob> slaughterable = new ArrayList<Mob>();
        for (BuildingInstance farm : farms) {
            List<Mob> adults = this.findAdultMobs(level, farm, animalType);
            int animalSpawnCount = SlaughterHandler.countAnimalSpawnPoints(farm);
            if (adults.size() <= Math.max(animalSpawnCount, 2)) continue;
            slaughterable.addAll(adults);
        }
        Mob bestMob = (Mob)SlaughterHandler.findClosestEntity(slaughterable, reference, lastTarget, type.batchRadius());
        return bestMob != null ? new GatheringTarget.EntityTarget((Entity)bestMob) : null;
    }

    @Override
    public boolean performAction(GoalContext ctx, GatheringType type, GatheringTarget target) {
        if (!(target instanceof GatheringTarget.EntityTarget)) {
            return true;
        }
        GatheringTarget.EntityTarget entityTarget = (GatheringTarget.EntityTarget)target;
        Entity entity = entityTarget.entity();
        if (!(entity instanceof Mob)) {
            return true;
        }
        Mob mob = (Mob)entity;
        if (!mob.isAlive()) {
            this.pickupNearbyItems(ctx, mob.blockPosition());
            return true;
        }
        float damage = this.getDamage(type);
        DamageSource source = ctx.level().damageSources().mobAttack((LivingEntity)ctx.villager());
        mob.hurt(source, damage);
        if (!mob.isAlive()) {
            this.pickupNearbyItems(ctx, mob.blockPosition());
            this.giveBonusItems(ctx, type);
            return true;
        }
        return false;
    }

    private void pickupNearbyItems(GoalContext ctx, BlockPos pos) {
        AABB pickupBox = new AABB((double)pos.getX() - 3.0, (double)pos.getY() - 2.0, (double)pos.getZ() - 3.0, (double)pos.getX() + 3.0, (double)pos.getY() + 2.0, (double)pos.getZ() + 3.0);
        List items = ctx.level().getEntitiesOfClass(ItemEntity.class, pickupBox);
        for (ItemEntity itemEntity : items) {
            if (!itemEntity.isAlive()) continue;
            ctx.villager().getInventory().add(itemEntity.getItem().getItem(), itemEntity.getItem().getCount());
            itemEntity.discard();
        }
    }

    private void giveBonusItems(GoalContext ctx, GatheringType type) {
        JsonArray bonusArray = null;
        if (type.handlerParams().has("bonusItems")) {
            bonusArray = GsonHelper.getAsJsonArray((JsonObject)type.handlerParams(), (String)"bonusItems");
        }
        if (bonusArray == null) {
            return;
        }
        for (JsonElement element : bonusArray) {
            JsonObject bonus = element.getAsJsonObject();
            String itemId = GsonHelper.getAsString((JsonObject)bonus, (String)"item");
            int count = GsonHelper.getAsInt((JsonObject)bonus, (String)"count", (int)1);
            int chance = GsonHelper.getAsInt((JsonObject)bonus, (String)"chance", (int)100);
            if (chance < 100 && ctx.level().random.nextInt(100) >= chance) continue;
            Item item = ItemHelper.resolve(itemId);
            if (item != null) {
                ctx.villager().getInventory().add(item, count);
                continue;
            }
            LOGGER.warn("Bonus item not found: {}", (Object)itemId);
        }
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

    private float getDamage(GatheringType type) {
        return GsonHelper.getAsFloat((JsonObject)type.handlerParams(), (String)"damage", (float)4.0f);
    }

    private List<Mob> findAdultMobs(ServerLevel level, BuildingInstance farm, EntityType<?> animalType) {
        AABB searchBox = SlaughterHandler.scanBoxAround(farm.getOrigin(), 25, 10);
        ArrayList<Mob> result = new ArrayList<Mob>();
        List entities = level.getEntities((Entity)null, searchBox, e -> e.getType() == animalType && e instanceof Mob);
        for (Entity entity : entities) {
            Mob mob = (Mob)entity;
            if (!mob.isAlive() || mob.isBaby()) continue;
            result.add(mob);
        }
        return result;
    }
}

