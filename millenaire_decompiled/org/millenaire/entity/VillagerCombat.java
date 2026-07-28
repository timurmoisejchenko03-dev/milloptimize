/*
 * Decompiled with CFR 0.150.
 * 
 * Could not load the following classes:
 *  com.mojang.logging.LogUtils
 *  javax.annotation.Nullable
 *  net.minecraft.ChatFormatting
 *  net.minecraft.core.BlockPos
 *  net.minecraft.core.registries.BuiltInRegistries
 *  net.minecraft.network.chat.Component
 *  net.minecraft.resources.ResourceLocation
 *  net.minecraft.server.level.ServerLevel
 *  net.minecraft.server.level.ServerPlayer
 *  net.minecraft.sounds.SoundEvents
 *  net.minecraft.world.Difficulty
 *  net.minecraft.world.InteractionHand
 *  net.minecraft.world.damagesource.DamageSource
 *  net.minecraft.world.entity.Entity
 *  net.minecraft.world.entity.EquipmentSlot
 *  net.minecraft.world.entity.LivingEntity
 *  net.minecraft.world.entity.monster.Creeper
 *  net.minecraft.world.entity.monster.Monster
 *  net.minecraft.world.entity.player.Player
 *  net.minecraft.world.entity.projectile.Arrow
 *  net.minecraft.world.item.BowItem
 *  net.minecraft.world.item.Item
 *  net.minecraft.world.item.ItemStack
 *  net.minecraft.world.item.Items
 *  net.minecraft.world.level.ItemLike
 *  net.minecraft.world.level.Level
 *  net.minecraft.world.phys.AABB
 *  org.slf4j.Logger
 */
package org.millenaire.entity;

import com.mojang.logging.LogUtils;
import java.util.UUID;
import javax.annotation.Nullable;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.Difficulty;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.monster.Creeper;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.Arrow;
import net.minecraft.world.item.BowItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.ItemLike;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import org.millenaire.building.BuildingInstance;
import org.millenaire.combat.CombatHelper;
import org.millenaire.combat.raid.SpecialPointFallback;
import org.millenaire.culture.ModCultures;
import org.millenaire.culture.VillagerType;
import org.millenaire.entity.MillVillager;
import org.millenaire.item.MillenaireBow;
import org.millenaire.tool.ToolCategory;
import org.millenaire.tool.ToolCategoryRegistry;
import org.millenaire.village.Village;
import org.millenaire.village.VillageId;
import org.slf4j.Logger;

public final class VillagerCombat {
    private static final Logger LOGGER = LogUtils.getLogger();
    private final MillVillager villager;
    @Nullable
    private LivingEntity attackTarget;
    private int attackCooldown;
    private boolean raiderEntity;
    @Nullable
    private UUID pursuitTargetId;
    private int pursuitTicks;
    private int pursuitNullTicks;
    private static final int UNREACHABLE_PURSUIT_TICKS = 600;
    private static final int PURSUIT_NULL_GRACE = 5;

    VillagerCombat(MillVillager villager) {
        this.villager = villager;
    }

    @Nullable
    public LivingEntity getAttackTarget() {
        return this.attackTarget;
    }

    public void setAttackTarget(@Nullable LivingEntity target) {
        this.attackTarget = target;
        this.villager.setTarget(target);
    }

    public int getAttackCooldown() {
        return this.attackCooldown;
    }

    public void setAttackCooldown(int cooldown) {
        this.attackCooldown = cooldown;
    }

    public boolean isRaiderEntity() {
        return this.raiderEntity;
    }

    public void setRaiderEntity(boolean raiderEntity) {
        this.raiderEntity = raiderEntity;
    }

    public int getAttackStrength() {
        VillagerType vt = ModCultures.getVillagerType(this.villager.getVillagerTypeId());
        int base = vt != null ? vt.baseAttackStrength() : 0;
        double weaponDmg = CombatHelper.weaponDamage(this.villager.getMainHandItem());
        return CombatHelper.entityAttackStrength(base, weaponDmg);
    }

    public ItemStack getCombatWeapon() {
        Item dflt;
        ToolCategory.ToolEntry best;
        ToolCategory ranged;
        VillagerType vt = ModCultures.getVillagerType(this.villager.getVillagerTypeId());
        if (vt == null) {
            return ItemStack.EMPTY;
        }
        if (vt.isArcher() && !this.isTargetWithinMeleeRange() && (ranged = ToolCategoryRegistry.get("weaponsranged")) != null && (best = ranged.getBestOwned(item -> this.villager.getInventory().getCount((Item)item) > 0)) != null && best.item() != null) {
            return new ItemStack((ItemLike)best.item());
        }
        ToolCategory melee = ToolCategoryRegistry.get("weaponshandtohand");
        if (melee != null && (best = melee.getBestOwned(item -> this.villager.getInventory().getCount((Item)item) > 0)) != null && best.item() != null) {
            return new ItemStack((ItemLike)best.item());
        }
        ResourceLocation dfltId = vt.defaultWeapon();
        if (dfltId != null && (dflt = (Item)BuiltInRegistries.ITEM.get(dfltId)) != Items.AIR) {
            return new ItemStack((ItemLike)dflt);
        }
        return ItemStack.EMPTY;
    }

    private boolean isTargetWithinMeleeRange() {
        if (this.attackTarget == null) {
            return false;
        }
        double minSq = 25.0;
        return this.villager.distanceToSqr((Entity)this.attackTarget) <= minSq;
    }

    public void performRangedAttack(LivingEntity target, float distanceFactor) {
        if (!(this.villager.level() instanceof ServerLevel)) {
            return;
        }
        Arrow arrow = new Arrow(this.villager.level(), (LivingEntity)this.villager, new ItemStack((ItemLike)Items.ARROW), null);
        double dx = target.getX() - this.villager.getX();
        double dy = target.getY(0.3333) - arrow.getY();
        double dz = target.getZ() - this.villager.getZ();
        double dist = Math.sqrt(dx * dx + dz * dz);
        float speedFactor = 1.0f;
        float damageBonus = 0.0f;
        ItemStack weapon = this.villager.getMainHandItem();
        Item item = weapon.getItem();
        if (item instanceof MillenaireBow) {
            MillenaireBow bow = (MillenaireBow)item;
            speedFactor = Math.max(speedFactor, bow.getSpeedFactor());
            damageBonus = Math.max(damageBonus, bow.getDamageBonus());
        }
        arrow.setBaseDamage(arrow.getBaseDamage() + (double)damageBonus);
        float velocity = (float)(14 - this.villager.level().getDifficulty().getId() * 4) * speedFactor;
        arrow.shoot(dx, dy + dist * 0.2, dz, 1.6f, velocity);
        this.villager.playSound(SoundEvents.SKELETON_SHOOT, 1.0f, 1.0f / (this.villager.getRandom().nextFloat() * 0.4f + 0.8f));
        this.villager.level().addFreshEntity((Entity)arrow);
    }

    public void performAttack(LivingEntity target) {
        VillagerType vt = ModCultures.getVillagerType(this.villager.getVillagerTypeId());
        boolean isArcher = vt != null && vt.isArcher();
        boolean hasBow = this.villager.getMainHandItem().getItem() instanceof BowItem;
        double distance = Math.sqrt(this.villager.distanceToSqr((Entity)target));
        boolean verticalOverlap = target.getBoundingBox().maxY > this.villager.getBoundingBox().minY && target.getBoundingBox().minY < this.villager.getBoundingBox().maxY;
        CombatHelper.AttackType type = CombatHelper.decideAttack(distance, isArcher, hasBow, this.getAttackCooldown(), verticalOverlap);
        switch (type) {
            case RANGED: {
                float df = (float)Math.min(1.0, Math.max(0.1, distance / 20.0));
                this.villager.swing(InteractionHand.MAIN_HAND);
                this.performRangedAttack(target, df);
                this.setAttackCooldown(100);
                this.pursuitTicks = 0;
                break;
            }
            case MELEE: {
                target.hurt(this.villager.damageSources().mobAttack((LivingEntity)this.villager), (float)this.getAttackStrength());
                this.setAttackCooldown(20);
                this.villager.swing(InteractionHand.MAIN_HAND);
                this.pursuitTicks = 0;
                break;
            }
        }
    }

    public void ensureCombatWeaponEquipped() {
        ItemStack weapon = this.getCombatWeapon();
        if (!ItemStack.matches((ItemStack)this.villager.getMainHandItem(), (ItemStack)weapon)) {
            this.villager.setItemSlot(EquipmentSlot.MAINHAND, weapon);
        }
    }

    void tickCombatMaintenance() {
        if (this.attackCooldown > 0) {
            --this.attackCooldown;
        }
        if (this.attackTarget != null && this.villager.getHiredBy() == null && this.isDefensiveType() && this.straysFromDefendingPos()) {
            this.setAttackTarget(null);
        }
        if (this.attackTarget != null) {
            double maxSq = 6400.0;
            if (!this.attackTarget.isAlive() || this.villager.distanceToSqr((Entity)this.attackTarget) > maxSq || this.villager.level().getDifficulty() == Difficulty.PEACEFUL) {
                this.setAttackTarget(null);
            }
        }
        if (this.attackTarget != null) {
            this.pursuitNullTicks = 0;
            this.tickUnreachableTargetWatchdog();
        } else if (this.pursuitTargetId != null && ++this.pursuitNullTicks > 5) {
            this.pursuitTargetId = null;
            this.pursuitTicks = 0;
        }
    }

    private void tickUnreachableTargetWatchdog() {
        ServerLevel serverLevel;
        Village village;
        Level level;
        LivingEntity t = this.attackTarget;
        if (this.raiderEntity || t instanceof Player || t instanceof MillVillager || t instanceof Creeper || !(t instanceof Monster)) {
            this.pursuitTargetId = null;
            this.pursuitTicks = 0;
            return;
        }
        UUID id = t.getUUID();
        if (!id.equals(this.pursuitTargetId)) {
            this.pursuitTargetId = id;
            this.pursuitTicks = 0;
            return;
        }
        if (++this.pursuitTicks < 600) {
            return;
        }
        VillageId villageId = this.villager.getVillageId();
        if (villageId != null && (level = this.villager.level()) instanceof ServerLevel && (village = Village.resolve(serverLevel = (ServerLevel)level, villageId)) != null) {
            village.markHuntUnreachable(t.blockPosition(), serverLevel.getGameTime());
            LOGGER.debug("[Millenaire] {} gave up unreachable target {} at {} \u2014 cell blacklisted", new Object[]{this.villager.getVillagerTypeId(), t.getType().toShortString(), t.blockPosition().toShortString()});
        }
        this.setAttackTarget(null);
        this.pursuitTargetId = null;
        this.pursuitTicks = 0;
    }

    void triggerMobAttacks() {
        AABB box = new AABB(this.villager.getX() - 16.0, this.villager.getY() - 5.0, this.villager.getZ() - 16.0, this.villager.getX() + 16.0, this.villager.getY() + 5.0, this.villager.getZ() + 16.0);
        for (Monster mob : this.villager.level().getEntitiesOfClass(Monster.class, box)) {
            if (mob.getTarget() != null || !mob.hasLineOfSight((Entity)this.villager)) continue;
            mob.setTarget((LivingEntity)this.villager);
        }
    }

    private boolean isDefensiveType() {
        ResourceLocation vtId = this.villager.getVillagerTypeId();
        VillagerType vt = vtId != null ? ModCultures.getVillagerType(vtId) : null;
        return vt != null && vt.isDefensive();
    }

    private boolean straysFromDefendingPos() {
        Level level;
        VillageId villageId = this.villager.getVillageId();
        if (villageId == null || !((level = this.villager.level()) instanceof ServerLevel)) {
            return false;
        }
        ServerLevel serverLevel = (ServerLevel)level;
        Village village = Village.resolve(serverLevel, villageId);
        if (village == null) {
            return false;
        }
        BuildingInstance th = village.getTownhall();
        if (th == null) {
            return false;
        }
        BlockPos defendingPos = SpecialPointFallback.resolveOrFallback(th.getFirstPointPos("defendingPos"), th.getOrigin());
        if (defendingPos == null) {
            return false;
        }
        double maxSq = 400.0;
        return this.villager.distanceToSqr((double)defendingPos.getX() + 0.5, (double)defendingPos.getY() + 0.5, (double)defendingPos.getZ() + 0.5) > maxSq;
    }

    void onHurt(DamageSource source, float amount, boolean hadFullHealth) {
        LivingEntity mobAttacker;
        MillVillager attacker;
        Entity vtId;
        ServerPlayer player;
        Entity entity = source.getEntity();
        if (entity instanceof ServerPlayer && !(player = (ServerPlayer)entity).isCreative() && !player.isSpectator() && !this.isRaiderEntity()) {
            ItemStack mainHand;
            ServerLevel serverLevel;
            Village village;
            Level level;
            vtId = this.villager.getVillagerTypeId();
            VillagerType vtype = vtId != null ? ModCultures.getVillagerType((ResourceLocation)vtId) : null;
            boolean nonHostileType = vtype != null && !vtype.isHostile();
            VillageId villageId = this.villager.getVillageId();
            if (nonHostileType && villageId != null && (level = this.villager.level()) instanceof ServerLevel && (village = Village.resolve(serverLevel = (ServerLevel)level, villageId)) != null) {
                int repChange = -((int)(amount * 10.0f));
                village.adjustReputation(serverLevel, player.getUUID(), repChange);
                LOGGER.debug("[Millenaire] {} hit {} : rep {} (village {})", new Object[]{player.getGameProfile().getName(), this.villager.getVillagerTypeId(), repChange, village.getVillageName()});
            }
            if (this.villager.level().getDifficulty() != Difficulty.PEACEFUL && this.villager.getHealth() < this.villager.getMaxHealth() - 10.0f) {
                this.setAttackTarget((LivingEntity)player);
                CombatHelper.callForHelp(this.villager, (LivingEntity)player);
            }
            if (nonHostileType && hadFullHealth && this.villager.level() instanceof ServerLevel && ((mainHand = player.getMainHandItem()).isEmpty() || CombatHelper.weaponDamage(mainHand) <= 1.0)) {
                player.sendSystemMessage((Component)Component.translatable((String)"message.millenaire.communication_explanations").withStyle(ChatFormatting.GOLD));
            }
        }
        if (!this.villager.level().isClientSide() && (vtId = source.getEntity()) instanceof MillVillager && (attacker = (MillVillager)vtId) != this.villager) {
            boolean sameVillage;
            VillageId villageId = this.villager.getVillageId();
            boolean bl = sameVillage = villageId != null && attacker.getVillageId() != null && villageId.equals(attacker.getVillageId());
            if (CombatHelper.isVillagerHostile(this.raiderEntity, attacker.isRaiderEntity(), sameVillage)) {
                this.setAttackTarget((LivingEntity)attacker);
                CombatHelper.callForHelp(this.villager, (LivingEntity)attacker);
            }
        }
        if (!(this.villager.level().isClientSide() || !((entity = source.getEntity()) instanceof LivingEntity) || (mobAttacker = (LivingEntity)entity) instanceof Player || mobAttacker instanceof MillVillager || this.isRaiderEntity())) {
            this.setAttackTarget(mobAttacker);
            CombatHelper.callForHelp(this.villager, mobAttacker);
        }
    }
}

