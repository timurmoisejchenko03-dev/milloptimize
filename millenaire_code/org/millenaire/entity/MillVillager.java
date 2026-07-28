/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.mojang.logging.LogUtils
 *  javax.annotation.Nullable
 *  net.minecraft.core.BlockPos
 *  net.minecraft.core.BlockPos$MutableBlockPos
 *  net.minecraft.core.registries.BuiltInRegistries
 *  net.minecraft.nbt.CompoundTag
 *  net.minecraft.network.chat.Component
 *  net.minecraft.network.chat.ComponentContents
 *  net.minecraft.network.chat.contents.TranslatableContents
 *  net.minecraft.network.syncher.EntityDataAccessor
 *  net.minecraft.network.syncher.EntityDataSerializer
 *  net.minecraft.network.syncher.EntityDataSerializers
 *  net.minecraft.network.syncher.SynchedEntityData
 *  net.minecraft.network.syncher.SynchedEntityData$Builder
 *  net.minecraft.resources.ResourceLocation
 *  net.minecraft.server.level.ServerLevel
 *  net.minecraft.server.level.ServerPlayer
 *  net.minecraft.sounds.SoundEvent
 *  net.minecraft.world.InteractionHand
 *  net.minecraft.world.InteractionResult
 *  net.minecraft.world.damagesource.DamageSource
 *  net.minecraft.world.damagesource.DamageTypes
 *  net.minecraft.world.entity.EntityType
 *  net.minecraft.world.entity.EquipmentSlot
 *  net.minecraft.world.entity.LivingEntity
 *  net.minecraft.world.entity.Mob
 *  net.minecraft.world.entity.PathfinderMob
 *  net.minecraft.world.entity.Pose
 *  net.minecraft.world.entity.ai.attributes.AttributeInstance
 *  net.minecraft.world.entity.ai.attributes.AttributeSupplier$Builder
 *  net.minecraft.world.entity.ai.attributes.Attributes
 *  net.minecraft.world.entity.ai.goal.FloatGoal
 *  net.minecraft.world.entity.ai.goal.Goal
 *  net.minecraft.world.entity.ai.goal.OpenDoorGoal
 *  net.minecraft.world.entity.ai.navigation.GroundPathNavigation
 *  net.minecraft.world.entity.ai.navigation.PathNavigation
 *  net.minecraft.world.entity.item.ItemEntity
 *  net.minecraft.world.entity.player.Player
 *  net.minecraft.world.item.Item
 *  net.minecraft.world.item.ItemStack
 *  net.minecraft.world.level.BlockGetter
 *  net.minecraft.world.level.ItemLike
 *  net.minecraft.world.level.Level
 *  net.minecraft.world.level.block.BedBlock
 *  net.minecraft.world.level.block.Block
 *  net.minecraft.world.level.block.LeavesBlock
 *  net.minecraft.world.level.block.state.BlockState
 *  net.minecraft.world.level.block.state.properties.Property
 *  net.minecraft.world.level.levelgen.Heightmap$Types
 *  net.minecraft.world.level.pathfinder.Node
 *  net.minecraft.world.level.pathfinder.Path
 *  net.minecraft.world.level.pathfinder.PathType
 *  net.minecraft.world.phys.AABB
 *  org.jetbrains.annotations.Nullable
 *  org.slf4j.Logger
 */
package org.millenaire.entity;

import com.mojang.logging.LogUtils;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;
import javax.annotation.Nullable;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.ComponentContents;
import net.minecraft.network.chat.contents.TranslatableContents;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializer;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.Pose;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.ai.goal.OpenDoorGoal;
import net.minecraft.world.entity.ai.navigation.GroundPathNavigation;
import net.minecraft.world.entity.ai.navigation.PathNavigation;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.ItemLike;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BedBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.LeavesBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.Property;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.pathfinder.Node;
import net.minecraft.world.level.pathfinder.Path;
import net.minecraft.world.level.pathfinder.PathType;
import net.minecraft.world.phys.AABB;
import org.millenaire.TickConstants;
import org.millenaire.advancement.MillAdvancements;
import org.millenaire.block.AppleTreeLeavesBlock;
import org.millenaire.block.OliveTreeLeavesBlock;
import org.millenaire.block.PistachioTreeLeavesBlock;
import org.millenaire.building.BuildingId;
import org.millenaire.building.BuildingInstance;
import org.millenaire.command.DebugCommand;
import org.millenaire.config.MillenaireServerConfig;
import org.millenaire.config.NavDriverType;
import org.millenaire.culture.ModCultures;
import org.millenaire.culture.VillagerType;
import org.millenaire.diagnostics.NavEvent;
import org.millenaire.diagnostics.NavigationCounters;
import org.millenaire.diagnostics.NavigationEventLog;
import org.millenaire.discovery.DiscoveryTracker;
import org.millenaire.entity.LocalRecoveryNavDriver;
import org.millenaire.entity.MillPathNavigation;
import org.millenaire.entity.ModelType;
import org.millenaire.entity.OpenFenceGateGoal;
import org.millenaire.entity.VillagerAppearanceFactory;
import org.millenaire.entity.VillagerCombat;
import org.millenaire.entity.VillagerIdentity;
import org.millenaire.entity.VillagerInteraction;
import org.millenaire.entity.VillagerInventory;
import org.millenaire.entity.VillagerNavDriver;
import org.millenaire.entity.VillagerNavigationManager;
import org.millenaire.entity.VillagerSpeech;
import org.millenaire.goal.GoalContext;
import org.millenaire.goal.GoalRegistry;
import org.millenaire.goal.GoalScheduler;
import org.millenaire.goal.TravelPhase;
import org.millenaire.goal.VillagerGoal;
import org.millenaire.goal.VillagerTask;
import org.millenaire.goal.impl.EngageTargetGoal;
import org.millenaire.goal.impl.GatherGoodsGoal;
import org.millenaire.goal.impl.GetToolGoal;
import org.millenaire.goal.impl.HiredEscortGoal;
import org.millenaire.goal.impl.LightHearthGoal;
import org.millenaire.goal.impl.RestGoal;
import org.millenaire.item.ClothItem;
import org.millenaire.item.SummoningWandItem;
import org.millenaire.tool.ToolCategory;
import org.millenaire.tool.ToolCategoryRegistry;
import org.millenaire.village.LocalMerchantHelper;
import org.millenaire.village.Village;
import org.millenaire.village.VillageEventType;
import org.millenaire.village.VillageId;
import org.millenaire.village.VillagerRecord;
import org.slf4j.Logger;

public class MillVillager
extends PathfinderMob {
    private static final Logger LOGGER = LogUtils.getLogger();
    public static final float PATH_PREFERENCE_MALUS = 1.2f;
    private static final EntityDataAccessor<String> DATA_VILLAGER_TYPE = SynchedEntityData.defineId(MillVillager.class, (EntityDataSerializer)EntityDataSerializers.STRING);
    private static final EntityDataAccessor<Byte> DATA_MODEL_TYPE = SynchedEntityData.defineId(MillVillager.class, (EntityDataSerializer)EntityDataSerializers.BYTE);
    private static final EntityDataAccessor<String> DATA_TEXTURE = SynchedEntityData.defineId(MillVillager.class, (EntityDataSerializer)EntityDataSerializers.STRING);
    private static final EntityDataAccessor<String> DATA_CLOTH_0 = SynchedEntityData.defineId(MillVillager.class, (EntityDataSerializer)EntityDataSerializers.STRING);
    private static final EntityDataAccessor<String> DATA_CLOTH_1 = SynchedEntityData.defineId(MillVillager.class, (EntityDataSerializer)EntityDataSerializers.STRING);
    private static final EntityDataAccessor<Float> DATA_SCALE = SynchedEntityData.defineId(MillVillager.class, (EntityDataSerializer)EntityDataSerializers.FLOAT);
    private static final EntityDataAccessor<String> DATA_DISPLAY_NAME = SynchedEntityData.defineId(MillVillager.class, (EntityDataSerializer)EntityDataSerializers.STRING);
    private static final EntityDataAccessor<String> DATA_ROLE_NAME = SynchedEntityData.defineId(MillVillager.class, (EntityDataSerializer)EntityDataSerializers.STRING);
    private static final EntityDataAccessor<String> DATA_NATIVE_ROLE_NAME = SynchedEntityData.defineId(MillVillager.class, (EntityDataSerializer)EntityDataSerializers.STRING);
    private static final EntityDataAccessor<String> DATA_GOAL_LABEL = SynchedEntityData.defineId(MillVillager.class, (EntityDataSerializer)EntityDataSerializers.STRING);
    private static final EntityDataAccessor<String> DATA_SPEECH_TEXT = SynchedEntityData.defineId(MillVillager.class, (EntityDataSerializer)EntityDataSerializers.STRING);
    private static final EntityDataAccessor<Boolean> DATA_SLEEPING = SynchedEntityData.defineId(MillVillager.class, (EntityDataSerializer)EntityDataSerializers.BOOLEAN);
    private static final EntityDataAccessor<Boolean> DATA_FOREIGN_MERCHANT = SynchedEntityData.defineId(MillVillager.class, (EntityDataSerializer)EntityDataSerializers.BOOLEAN);
    private static final EntityDataAccessor<Boolean> DATA_IS_CHIEF = SynchedEntityData.defineId(MillVillager.class, (EntityDataSerializer)EntityDataSerializers.BOOLEAN);
    private static final EntityDataAccessor<Boolean> DATA_IS_SELLING = SynchedEntityData.defineId(MillVillager.class, (EntityDataSerializer)EntityDataSerializers.BOOLEAN);
    private static final EntityDataAccessor<Boolean> DATA_HIRED = SynchedEntityData.defineId(MillVillager.class, (EntityDataSerializer)EntityDataSerializers.BOOLEAN);
    private static final EntityDataAccessor<Boolean> DATA_STANCE_AGGRESSIVE = SynchedEntityData.defineId(MillVillager.class, (EntityDataSerializer)EntityDataSerializers.BOOLEAN);
    private static final EntityDataAccessor<Integer> DATA_HIRE_HOURS_LEFT = SynchedEntityData.defineId(MillVillager.class, (EntityDataSerializer)EntityDataSerializers.INT);
    private static final int HELD_ITEM_CYCLE_DURATION = 20;
    public static final int MAX_CHILD_SIZE = 20;
    private static final int WATER_DANGER_TICKS = 100;
    private static final int MAX_SLEEP_DEBT = 6000;
    private static final int SLEEP_DEBT_DECAY_PER_TICK = 2;
    private static final int SLEEP_DEBT_CARRY_GRACE_TICKS = 1200;
    private static final String FREE_CLOTHES = "free";
    private static final String NATURAL = "natural";
    private VillageId villageId;
    private ResourceLocation villagerTypeId;
    private final VillagerIdentity identity = new VillagerIdentity();
    @Nullable
    private BuildingId homeBuilding;
    @Nullable
    private BuildingId constructionBuildingId;
    private int foreignMerchantStallId = -1;
    private int visitorNbNights = 0;
    private final VillagerInventory inventory = new VillagerInventory();
    private final VillagerSpeech speech = new VillagerSpeech(this);
    @Nullable
    private GoalScheduler goalScheduler;
    private final VillagerNavDriver navManager = MillVillager.createNavDriver();
    private final NavigationEventLog navEventLog = new NavigationEventLog();
    private long lastLeafClearNotLeavesTick = Long.MIN_VALUE;
    private int heldItemTick;
    private int heldItemIndex;
    private int offHandItemIndex;
    private String lastGoalLabel = "";
    @Nullable
    private VillagerTask lastTrackedTask;
    private int suffocationGraceTicks;
    private int waterTicks;
    private int sleepDebtTicks;
    private boolean guiPreviewMode;
    @Nullable
    private BlockPos lastOutdoorPos;
    private final VillagerCombat combat = new VillagerCombat(this);
    @org.jetbrains.annotations.Nullable
    private UUID hiredBy;
    private long hiredUntil;
    private boolean aggressiveStance;
    private static final Map<EquipmentSlot, String> ARMOR_SLOT_CATEGORIES = Map.of(EquipmentSlot.HEAD, "armourshelmet", EquipmentSlot.CHEST, "armourschestplate", EquipmentSlot.LEGS, "armoursleggings", EquipmentSlot.FEET, "armoursboots");

    public VillagerNavDriver getNavManager() {
        return this.navManager;
    }

    private static VillagerNavDriver createNavDriver() {
        try {
            NavDriverType type = (NavDriverType)((Object)MillenaireServerConfig.SERVER.navDriver.get());
            if (type == NavDriverType.LOCAL_RECOVERY) {
                return new LocalRecoveryNavDriver();
            }
        }
        catch (RuntimeException runtimeException) {
            // empty catch block
        }
        return new VillagerNavigationManager();
    }

    public NavigationEventLog getNavEventLog() {
        return this.navEventLog;
    }

    public int getSleepDebtTicks() {
        return this.sleepDebtTicks;
    }

    public void setSleepDebtTicks(int v) {
        this.sleepDebtTicks = Math.max(0, Math.min(6000, v));
    }

    public void setGuiPreviewMode(boolean preview) {
        this.guiPreviewMode = preview;
    }

    public boolean isGuiPreviewMode() {
        return this.guiPreviewMode;
    }

    @Nullable
    public LivingEntity getAttackTarget() {
        return this.combat.getAttackTarget();
    }

    public void setAttackTarget(@Nullable LivingEntity target) {
        this.combat.setAttackTarget(target);
    }

    public int getAttackCooldown() {
        return this.combat.getAttackCooldown();
    }

    public void setAttackCooldown(int cooldown) {
        this.combat.setAttackCooldown(cooldown);
    }

    public boolean isRaiderEntity() {
        return this.combat.isRaiderEntity();
    }

    public void setRaiderEntity(boolean raiderEntity) {
        this.combat.setRaiderEntity(raiderEntity);
    }

    public int getAttackStrength() {
        return this.combat.getAttackStrength();
    }

    public ItemStack getCombatWeapon() {
        return this.combat.getCombatWeapon();
    }

    public void performRangedAttack(LivingEntity target, float distanceFactor) {
        this.combat.performRangedAttack(target, distanceFactor);
    }

    public void performAttack(LivingEntity target) {
        this.combat.performAttack(target);
    }

    public void ensureCombatWeaponEquipped() {
        this.combat.ensureCombatWeaponEquipped();
    }

    @org.jetbrains.annotations.Nullable
    public UUID getHiredBy() {
        return this.hiredBy;
    }

    public long getHiredUntil() {
        return this.hiredUntil;
    }

    public boolean isHired() {
        return this.hiredBy != null;
    }

    public boolean isAggressiveStance() {
        return this.aggressiveStance;
    }

    public void setHireState(@org.jetbrains.annotations.Nullable UUID owner, long until) {
        this.hiredBy = owner;
        this.hiredUntil = until;
        this.entityData.set(DATA_HIRED, (Object)(owner != null ? 1 : 0));
        if (owner == null) {
            this.setAggressiveStance(false);
        }
    }

    public void setAggressiveStance(boolean aggressive) {
        this.aggressiveStance = aggressive;
        this.entityData.set(DATA_STANCE_AGGRESSIVE, (Object)aggressive);
    }

    public boolean clientIsHired() {
        return (Boolean)this.entityData.get(DATA_HIRED);
    }

    public boolean clientIsAggressive() {
        return (Boolean)this.entityData.get(DATA_STANCE_AGGRESSIVE);
    }

    public int clientHireHoursLeft() {
        return (Integer)this.entityData.get(DATA_HIRE_HOURS_LEFT);
    }

    public void setHireHoursLeft(int h) {
        this.entityData.set(DATA_HIRE_HOURS_LEFT, (Object)h);
    }

    public MillVillager(EntityType<? extends MillVillager> entityType, Level level) {
        super(entityType, level);
        this.setPersistenceRequired();
        this.setPathfindingMalus(PathType.WATER, -1.0f);
        this.setPathfindingMalus(PathType.WATER_BORDER, 8.0f);
        this.setPathfindingMalus(PathType.WALKABLE, 1.2f);
        this.setPathfindingMalus(PathType.COCOA, 0.0f);
        this.goalSelector.addGoal(0, (Goal)new FloatGoal((Mob)this));
        this.goalSelector.addGoal(1, (Goal)new OpenDoorGoal((Mob)this, true));
        this.goalSelector.addGoal(1, (Goal)new OpenFenceGateGoal((Mob)this, true));
        PathNavigation pathNavigation = this.getNavigation();
        if (pathNavigation instanceof GroundPathNavigation) {
            GroundPathNavigation groundNav = (GroundPathNavigation)pathNavigation;
            groundNav.setCanOpenDoors(true);
        }
    }

    public boolean removeWhenFarAway(double distanceToClosestPlayer) {
        return false;
    }

    public int getMaxFallDistance() {
        return 1;
    }

    public InteractionResult mobInteract(Player player, InteractionHand hand) {
        if (this.level().isClientSide()) {
            if (player.getItemInHand(hand).getItem() instanceof SummoningWandItem) {
                return InteractionResult.PASS;
            }
            return InteractionResult.SUCCESS;
        }
        if (this.isSleeping() || this.isVillagerSleeping()) {
            return InteractionResult.PASS;
        }
        if (player instanceof ServerPlayer) {
            ServerPlayer serverPlayer = (ServerPlayer)player;
            if (player.getItemInHand(hand).getItem() instanceof SummoningWandItem && (serverPlayer.hasPermissions(2) || serverPlayer.server.isSingleplayer())) {
                return InteractionResult.PASS;
            }
            if (this.goalScheduler != null) {
                Map<String, String> debugInfo;
                VillagerNavDriver nav = this.navManager;
                VillagerNavDriver.NavDiagnostics diag = nav.getDiagnostics();
                VillagerTask task = this.goalScheduler.getCurrentTask();
                boolean hasPath = this.getNavigation().getPath() != null && !this.getNavigation().isDone();
                LOGGER.info("[NavDebug] {} ({}) pos={} \u2014 goal={}, dest={}, hasPath={}, localStuck={}, longStuck={}, tp={}, abandoned={}, wpn={}", new Object[]{this.getVillagerDisplayName(), this.getVillagerTypeId(), this.blockPosition().toShortString(), this.goalScheduler.getCurrentGoalId(), nav.getDestination() != null ? nav.getDestination().toShortString() : "null", hasPath, diag.localStuck(), diag.longDistanceStuck(), diag.teleportCount(), nav.isAbandoned(), diag.waypointState() != null ? diag.waypointState() : "null"});
                if (task != null && (debugInfo = task.getNavDebugInfo()) != null && !debugInfo.isEmpty()) {
                    LOGGER.info("[NavDebug]   task: {}", debugInfo);
                }
            }
            VillagerInteraction.openInfoScreen(serverPlayer, this);
        }
        return InteractionResult.SUCCESS;
    }

    public boolean hurt(DamageSource source, float amount) {
        boolean hadFullHealth = this.getHealth() >= this.getMaxHealth();
        boolean result = super.hurt(source, amount);
        if (result) {
            this.combat.onHurt(source, amount, hadFullHealth);
        }
        return result;
    }

    public void die(DamageSource cause) {
        ServerPlayer killer;
        Object village;
        UUID owner;
        Level level;
        this.clearHeldItems();
        if (this.hiredBy != null && (level = this.level()) instanceof ServerLevel) {
            ServerLevel hireLevel = (ServerLevel)level;
            if (this.getVillageId() != null) {
                owner = this.hiredBy;
                Village hireVillage = Village.resolve(hireLevel, this.getVillageId());
                if (hireVillage != null) {
                    hireVillage.setVillagerHired(hireLevel, this.getUUID(), null, 0L);
                } else {
                    this.setHireState(null, 0L);
                }
                ServerPlayer ownerPlayer = hireLevel.getServer().getPlayerList().getPlayer(owner);
                if (ownerPlayer != null) {
                    ownerPlayer.sendSystemMessage((Component)Component.translatable((String)"message.millenaire.hire.hiredied", (Object[])new Object[]{this.getName()}));
                }
            }
        }
        LOGGER.warn("[Mill\u00e9naire] Villager {} ({}) died: cause={}, pos={}, health={}", new Object[]{this.getUUID().toString().substring(0, 8), this.getVillagerTypeId(), cause.getMsgId(), this.blockPosition().toShortString(), Float.valueOf(this.getHealth())});
        owner = this.level();
        if (owner instanceof ServerLevel) {
            ServerLevel serverLevel = (ServerLevel)owner;
            if (this.getVillageId() != null && (village = Village.resolve(serverLevel, this.getVillageId())) != null) {
                ResourceLocation deathTypeId;
                BuildingInstance home;
                VillagerRecord record = ((Village)village).getVillagerRecord(this.getUUID());
                if (record != null) {
                    record.updateFromEntity(this);
                    ((Village)village).markVillagerKilled(this.getUUID());
                    ((Village)village).markDirty();
                    ((Village)village).reevaluateChestLock(serverLevel);
                }
                if (this.homeBuilding != null && (home = ((Village)village).getBuilding(this.homeBuilding)) != null && home.hasBedManager()) {
                    home.getBedManager().releaseBedByVillager(this.getUUID());
                    ((Village)village).markDirty();
                }
                ((Village)village).recordEvent(serverLevel, "Villager died: " + ((deathTypeId = this.getVillagerTypeId()) != null ? deathTypeId.getPath() : "unknown") + " [" + this.getUUID().toString().substring(0, 8) + "] \u2014 cause: " + cause.getMsgId() + " at " + this.blockPosition().toShortString());
                ((Village)village).recordChronicleEvent(serverLevel, VillageEventType.DEATH, this.getFirstName() + " " + this.getFamilyName(), cause.getMsgId());
            }
        }
        if ((village = cause.getEntity()) instanceof ServerPlayer && !(killer = (ServerPlayer)village).isCreative() && !killer.isSpectator()) {
            VillagerType vType = ModCultures.getVillagerType(this.getVillagerTypeId());
            if (vType != null && vType.hasTag("hostile")) {
                MillAdvancements.grant(killer, MillAdvancements.SELF_DEFENSE);
            } else {
                MillAdvancements.grant(killer, MillAdvancements.DARK_SIDE);
            }
        }
        super.die(cause);
    }

    public void grantSuffocationGrace(int ticks) {
        this.suffocationGraceTicks = ticks;
    }

    public boolean isInvulnerableTo(DamageSource source) {
        if (this.suffocationGraceTicks > 0 && source.is(DamageTypes.IN_WALL)) {
            return true;
        }
        return super.isInvulnerableTo(source);
    }

    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        super.defineSynchedData(builder);
        builder.define(DATA_VILLAGER_TYPE, (Object)"");
        builder.define(DATA_MODEL_TYPE, (Object)0);
        builder.define(DATA_TEXTURE, (Object)"");
        builder.define(DATA_CLOTH_0, (Object)"");
        builder.define(DATA_CLOTH_1, (Object)"");
        builder.define(DATA_SCALE, (Object)Float.valueOf(1.0f));
        builder.define(DATA_DISPLAY_NAME, (Object)"");
        builder.define(DATA_ROLE_NAME, (Object)"");
        builder.define(DATA_NATIVE_ROLE_NAME, (Object)"");
        builder.define(DATA_GOAL_LABEL, (Object)"");
        builder.define(DATA_SPEECH_TEXT, (Object)"");
        builder.define(DATA_SLEEPING, (Object)false);
        builder.define(DATA_FOREIGN_MERCHANT, (Object)false);
        builder.define(DATA_IS_CHIEF, (Object)false);
        builder.define(DATA_IS_SELLING, (Object)false);
        builder.define(DATA_HIRED, (Object)false);
        builder.define(DATA_STANCE_AGGRESSIVE, (Object)false);
        builder.define(DATA_HIRE_HOURS_LEFT, (Object)0);
    }

    public VillageId getVillageId() {
        return this.villageId;
    }

    public void setVillageId(VillageId villageId) {
        this.villageId = villageId;
    }

    @Nullable
    public ResourceLocation getVillagerTypeId() {
        if (this.level().isClientSide()) {
            String synced = (String)this.entityData.get(DATA_VILLAGER_TYPE);
            return synced.isEmpty() ? null : ResourceLocation.parse((String)synced);
        }
        return this.villagerTypeId;
    }

    public void setVillagerTypeId(ResourceLocation villagerTypeId) {
        this.villagerTypeId = villagerTypeId;
        this.entityData.set(DATA_VILLAGER_TYPE, (Object)(villagerTypeId != null ? villagerTypeId.toString() : ""));
        if (villagerTypeId != null && !this.level().isClientSide()) {
            VillagerType vType = ModCultures.getVillagerType(villagerTypeId);
            this.entityData.set(DATA_IS_CHIEF, (Object)(vType != null && vType.hasTag("chief") ? 1 : 0));
        }
    }

    public VillagerIdentity getIdentity() {
        return this.identity;
    }

    public ModelType getModelType() {
        if (this.level().isClientSide()) {
            return ModelType.fromByte((Byte)this.entityData.get(DATA_MODEL_TYPE));
        }
        return this.identity.getModelType();
    }

    public ResourceLocation getTexture() {
        if (this.level().isClientSide()) {
            String synced = (String)this.entityData.get(DATA_TEXTURE);
            return synced.isEmpty() ? null : ResourceLocation.parse((String)synced);
        }
        return this.identity.getTexture();
    }

    public ResourceLocation getClothTexture0() {
        if (this.level().isClientSide()) {
            String synced = (String)this.entityData.get(DATA_CLOTH_0);
            return synced.isEmpty() ? null : ResourceLocation.parse((String)synced);
        }
        return this.identity.getClothTexture0();
    }

    public ResourceLocation getClothTexture1() {
        if (this.level().isClientSide()) {
            String synced = (String)this.entityData.get(DATA_CLOTH_1);
            return synced.isEmpty() ? null : ResourceLocation.parse((String)synced);
        }
        return this.identity.getClothTexture1();
    }

    public float getVillagerScale() {
        if (this.level().isClientSide()) {
            return ((Float)this.entityData.get(DATA_SCALE)).floatValue();
        }
        return this.identity.getVillagerScale();
    }

    public Component getDisplayName() {
        String name = this.getVillagerDisplayName();
        if (name != null && !name.isEmpty() && !name.equals("entity.millenaire.villager")) {
            return Component.literal((String)name);
        }
        return super.getDisplayName();
    }

    public String getVillagerDisplayName() {
        if (this.level().isClientSide()) {
            return (String)this.entityData.get(DATA_DISPLAY_NAME);
        }
        String fn = this.identity.getFamilyName();
        String gn = this.identity.getFirstName();
        if (fn.isEmpty()) {
            return gn;
        }
        if (gn.isEmpty()) {
            return fn;
        }
        return gn + " " + fn;
    }

    public String getFirstName() {
        return this.identity.getFirstName();
    }

    public String getFamilyName() {
        return this.identity.getFamilyName();
    }

    public void setFirstName(String firstName) {
        this.identity.setFirstName(firstName);
        this.syncDisplayName();
    }

    public void setFamilyName(String familyName) {
        this.identity.setFamilyName(familyName);
        this.syncDisplayName();
    }

    private void syncDisplayName() {
        String display = this.getVillagerDisplayName();
        this.entityData.set(DATA_DISPLAY_NAME, (Object)display);
    }

    public String getRoleName() {
        if (this.level().isClientSide()) {
            return (String)this.entityData.get(DATA_ROLE_NAME);
        }
        return this.identity.getRoleName();
    }

    public String getNativeRoleName() {
        return (String)this.entityData.get(DATA_NATIVE_ROLE_NAME);
    }

    @Nullable
    public ResourceLocation getCultureId() {
        ResourceLocation vtId = this.getVillagerTypeId();
        if (vtId == null) {
            return null;
        }
        String path = vtId.getPath();
        int slash = path.indexOf(47);
        if (slash < 0) {
            return null;
        }
        return ResourceLocation.fromNamespaceAndPath((String)vtId.getNamespace(), (String)path.substring(0, slash));
    }

    private void syncNativeRoleName() {
        VillagerType vt;
        if (this.villagerTypeId != null && (vt = ModCultures.getVillagerType(this.villagerTypeId)) != null) {
            if (vt.isChild() && this.getChildSize() >= 20 && vt.altNativeName() != null) {
                this.entityData.set(DATA_NATIVE_ROLE_NAME, (Object)vt.altNativeName());
                return;
            }
            if (vt.nativeName() != null) {
                this.entityData.set(DATA_NATIVE_ROLE_NAME, (Object)vt.nativeName());
                return;
            }
        }
        this.entityData.set(DATA_NATIVE_ROLE_NAME, (Object)"");
    }

    public int getChildSize() {
        return this.identity.getChildSize();
    }

    public void setChildSize(int childSize) {
        this.identity.setChildSize(childSize);
        if (childSize >= 0) {
            VillagerType vType = ModCultures.getVillagerType(this.villagerTypeId);
            if (vType != null) {
                float scale = VillagerAppearanceFactory.computeChildScale(childSize, vType.gender());
                this.setVillagerScale(scale);
            }
            double childHealth = 10.0 + (double)childSize;
            AttributeInstance attr = this.getAttribute(Attributes.MAX_HEALTH);
            if (attr != null) {
                attr.setBaseValue(childHealth);
                if (this.getHealth() > (float)childHealth) {
                    this.setHealth((float)childHealth);
                }
            }
            if (childSize >= 20) {
                this.syncNativeRoleName();
            }
        }
    }

    public boolean isChild() {
        return this.identity.getChildSize() >= 0;
    }

    public String getFathersName() {
        return this.identity.getFathersName();
    }

    public void setFathersName(String fathersName) {
        this.identity.setFathersName(fathersName);
    }

    public String getMothersName() {
        return this.identity.getMothersName();
    }

    public void setMothersName(String mothersName) {
        this.identity.setMothersName(mothersName);
    }

    public String getSpousesName() {
        return this.identity.getSpousesName();
    }

    public void setSpousesName(String spousesName) {
        this.identity.setSpousesName(spousesName);
    }

    public String getMaidenName() {
        return this.identity.getMaidenName();
    }

    public void setMaidenName(String maidenName) {
        this.identity.setMaidenName(maidenName);
    }

    public void setVillagerScale(float scale) {
        this.identity.setVillagerScale(scale);
        this.entityData.set(DATA_SCALE, (Object)Float.valueOf(scale));
    }

    @Nullable
    public BuildingId getHomeBuilding() {
        return this.homeBuilding;
    }

    public void setHomeBuilding(@Nullable BuildingId newHome) {
        BuildingInstance oldHome;
        ServerLevel sl;
        Object village;
        Level level;
        if (this.homeBuilding != null && !this.homeBuilding.equals(newHome) && this.villageId != null && (level = this.level()) instanceof ServerLevel && (village = Village.resolve(sl = (ServerLevel)level, this.villageId)) != null && (oldHome = ((Village)village).getBuilding(this.homeBuilding)) != null && oldHome.hasBedManager()) {
            oldHome.getBedManager().releaseBedByVillager(this.getUUID());
            ((Village)village).markDirty();
        }
        this.homeBuilding = newHome;
        if (this.villageId != null && (village = this.level()) instanceof ServerLevel && (village = Village.resolve(sl = (ServerLevel)village, this.villageId)) != null) {
            ((Village)village).setVillagerHome(this.getUUID(), newHome);
        }
    }

    @Nullable
    public BuildingId getConstructionBuildingId() {
        return this.constructionBuildingId;
    }

    public void setConstructionBuildingId(@Nullable BuildingId id) {
        this.constructionBuildingId = id;
    }

    public int getForeignMerchantStallId() {
        return this.foreignMerchantStallId;
    }

    public void setForeignMerchantStallId(int stallId) {
        this.foreignMerchantStallId = stallId;
        if (stallId >= 0) {
            this.entityData.set(DATA_FOREIGN_MERCHANT, (Object)true);
        }
    }

    public int getVisitorNbNights() {
        return this.visitorNbNights;
    }

    public void setVisitorNbNights(int nights) {
        this.visitorNbNights = nights;
    }

    public boolean isForeignMerchant() {
        if (this.level().isClientSide()) {
            return (Boolean)this.entityData.get(DATA_FOREIGN_MERCHANT);
        }
        VillagerType vType = ModCultures.getVillagerType(this.getVillagerTypeId());
        return vType != null && vType.hasTag("foreignmerchant");
    }

    public boolean isLocalMerchant() {
        VillagerType vType = ModCultures.getVillagerType(this.getVillagerTypeId());
        return vType != null && vType.hasTag("localmerchant");
    }

    private void localMerchantRescue() {
        Level level = this.level();
        if (!(level instanceof ServerLevel)) {
            return;
        }
        ServerLevel sl = (ServerLevel)level;
        if (this.homeBuilding == null || this.villageId == null) {
            return;
        }
        Village village = Village.resolve(sl, this.villageId);
        if (village == null) {
            return;
        }
        BuildingInstance townhall = village.getTownhall();
        if (townhall == null) {
            return;
        }
        if (!this.homeBuilding.equals(townhall.getId())) {
            return;
        }
        List<BuildingInstance> inns = village.getOperationalBuildingsWithTag("inn");
        BuildingInstance freeInn = null;
        for (BuildingInstance inn : inns) {
            if (LocalMerchantHelper.getMerchantRecord(village, inn) != null) continue;
            freeInn = inn;
            break;
        }
        if (freeInn != null) {
            this.setHomeBuilding(freeInn.getId());
            LOGGER.warn("[Millenaire] Merchant {} had Town Hall as home. Moved to inn.", (Object)this.getVillagerDisplayName());
        } else {
            village.removeVillagerRecord(this.getUUID());
            village.markDirty();
            this.discard();
            LOGGER.warn("[Millenaire] Merchant {} had Town Hall as home and no free inn. Despawned.", (Object)this.getVillagerDisplayName());
        }
    }

    public boolean isChief() {
        return (Boolean)this.entityData.get(DATA_IS_CHIEF);
    }

    public void syncChiefFlag(VillagerType vType) {
        this.entityData.set(DATA_IS_CHIEF, (Object)vType.hasTag("chief"));
    }

    public boolean isSelling() {
        return (Boolean)this.entityData.get(DATA_IS_SELLING);
    }

    public void setSelling(boolean selling) {
        this.entityData.set(DATA_IS_SELLING, (Object)selling);
    }

    public VillagerInventory getInventory() {
        return this.inventory;
    }

    @Nullable
    public GoalScheduler getGoalScheduler() {
        return this.goalScheduler;
    }

    @Nullable
    public BlockPos getLastOutdoorPos() {
        return this.lastOutdoorPos;
    }

    public String getGoalLabel() {
        return (String)this.entityData.get(DATA_GOAL_LABEL);
    }

    public boolean isVillagerSleeping() {
        return (Boolean)this.entityData.get(DATA_SLEEPING);
    }

    public void setVillagerSleeping(boolean sleeping) {
        this.entityData.set(DATA_SLEEPING, (Object)sleeping);
    }

    public void initGoals(GoalRegistry registry, VillagerType villagerType) {
        VillagerGoal escort;
        VillagerGoal engageGoal;
        VillagerGoal lightHearthGoal;
        VillagerGoal gatherGoodsGoal;
        VillagerGoal getToolGoal;
        ArrayList<VillagerGoal> goals = new ArrayList<VillagerGoal>(registry.resolve(villagerType.goals()));
        if (!villagerType.toolNeededClasses().isEmpty() && (getToolGoal = registry.get(GetToolGoal.ID)) != null && !goals.contains(getToolGoal)) {
            goals.add(getToolGoal);
        }
        if (!villagerType.collectGoods().isEmpty() && (gatherGoodsGoal = registry.get(GatherGoodsGoal.ID)) != null && !goals.contains(gatherGoodsGoal)) {
            goals.add(gatherGoodsGoal);
        }
        if (!villagerType.isChild() && (lightHearthGoal = registry.get(LightHearthGoal.ID)) != null && !goals.contains(lightHearthGoal)) {
            goals.add(lightHearthGoal);
        }
        if (!villagerType.isChild() && (engageGoal = registry.get(EngageTargetGoal.ID)) != null && !goals.contains(engageGoal)) {
            goals.add(engageGoal);
        }
        if (!villagerType.isChild() && (escort = registry.get(HiredEscortGoal.ID)) != null && !goals.contains(escort)) {
            goals.add(escort);
        }
        this.goalScheduler = new GoalScheduler(goals);
    }

    public void initAppearance(ModelType modelType, ResourceLocation texture, ResourceLocation cloth0, ResourceLocation cloth1, float scale, String firstName, String familyName, String roleName) {
        this.identity.setModelType(modelType);
        this.identity.setTexture(texture);
        this.identity.setClothTexture0(cloth0);
        this.identity.setClothTexture1(cloth1);
        this.identity.setVillagerScale(scale);
        this.identity.setFirstName(firstName);
        this.identity.setFamilyName(familyName);
        this.identity.setRoleName(roleName);
        this.entityData.set(DATA_MODEL_TYPE, (Object)modelType.toByte());
        this.entityData.set(DATA_TEXTURE, (Object)(texture != null ? texture.toString() : ""));
        this.entityData.set(DATA_CLOTH_0, (Object)(cloth0 != null ? cloth0.toString() : ""));
        this.entityData.set(DATA_CLOTH_1, (Object)(cloth1 != null ? cloth1.toString() : ""));
        this.entityData.set(DATA_SCALE, (Object)Float.valueOf(scale));
        this.entityData.set(DATA_DISPLAY_NAME, (Object)this.getVillagerDisplayName());
        this.entityData.set(DATA_ROLE_NAME, (Object)(roleName != null ? roleName : ""));
        this.syncNativeRoleName();
    }

    public void updateClothTextures(VillagerType vType) {
        if (vType == null) {
            return;
        }
        String bestClothName = null;
        int clothLevel = -1;
        if (vType.hasClothSet(FREE_CLOTHES)) {
            bestClothName = FREE_CLOTHES;
            clothLevel = 0;
        }
        for (Map.Entry<Item, Integer> entry : this.inventory.getAll().entrySet()) {
            ClothItem clothItem;
            Item item;
            if (entry.getValue() <= 0 || !((item = entry.getKey()) instanceof ClothItem) || (clothItem = (ClothItem)item).getPriority() <= clothLevel || !vType.hasClothSet(clothItem.getClothName())) continue;
            bestClothName = clothItem.getClothName();
            clothLevel = clothItem.getPriority();
        }
        if (bestClothName != null) {
            if (!bestClothName.equals(this.identity.getClothName())) {
                this.identity.setClothName(bestClothName);
                ThreadLocalRandom random = ThreadLocalRandom.current();
                for (int layer = 0; layer < 2; ++layer) {
                    ResourceLocation texture = vType.hasNaturalLayer(layer) ? MillVillager.randomClothTexture(vType, NATURAL, layer, random) : MillVillager.randomClothTexture(vType, bestClothName, layer, random);
                    if (layer == 0) {
                        this.identity.setClothTexture0(texture);
                        continue;
                    }
                    this.identity.setClothTexture1(texture);
                }
                this.entityData.set(DATA_CLOTH_0, (Object)(this.identity.getClothTexture0() != null ? this.identity.getClothTexture0().toString() : ""));
                this.entityData.set(DATA_CLOTH_1, (Object)(this.identity.getClothTexture1() != null ? this.identity.getClothTexture1().toString() : ""));
            }
        } else {
            this.identity.setClothName(null);
            this.identity.setClothTexture0(null);
            this.identity.setClothTexture1(null);
            this.entityData.set(DATA_CLOTH_0, (Object)"");
            this.entityData.set(DATA_CLOTH_1, (Object)"");
        }
    }

    @Nullable
    private static ResourceLocation randomClothTexture(VillagerType vType, String clothSetName, int layer, ThreadLocalRandom random) {
        List<ResourceLocation> textures;
        VillagerType.ClothSet clothSet = vType.clothes().get(clothSetName);
        if (clothSet == null) {
            return null;
        }
        List<ResourceLocation> list = textures = layer == 0 ? clothSet.layer0() : clothSet.layer1();
        if (textures == null || textures.isEmpty()) {
            return null;
        }
        return textures.get(random.nextInt(textures.size()));
    }

    public static AttributeSupplier.Builder createAttributes() {
        return PathfinderMob.createMobAttributes().add(Attributes.MAX_HEALTH, 20.0).add(Attributes.MOVEMENT_SPEED, 0.55).add(Attributes.FOLLOW_RANGE, 120.0).add(Attributes.ATTACK_DAMAGE, 1.0).add(Attributes.STEP_HEIGHT, 1.0);
    }

    protected PathNavigation createNavigation(Level level) {
        return new MillPathNavigation((Mob)this, level);
    }

    @Nullable
    protected SoundEvent getAmbientSound() {
        return null;
    }

    @Nullable
    protected SoundEvent getHurtSound(DamageSource source) {
        return null;
    }

    @Nullable
    protected SoundEvent getDeathSound() {
        return null;
    }

    public void aiStep() {
        super.aiStep();
        this.updateSwingTime();
        if (!this.level().isClientSide()) {
            this.combat.tickCombatMaintenance();
        }
    }

    public void tick() {
        VillagerType vType;
        super.tick();
        if (!this.level().isClientSide() && this.suffocationGraceTicks > 0) {
            --this.suffocationGraceTicks;
        }
        if (!this.level().isClientSide() && this.goalScheduler != null) {
            if (this.isInWater()) {
                ++this.waterTicks;
                if (this.waterTicks > 100) {
                    int safeY = this.level().getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, this.blockPosition().getX(), this.blockPosition().getZ());
                    this.teleportTo((double)this.blockPosition().getX() + 0.5, safeY, (double)this.blockPosition().getZ() + 0.5);
                    LOGGER.debug("[Mill\u00e9naire] Anti-drowning: {} TP surface (in water for {} ticks)", (Object)this.getVillagerTypeId(), (Object)this.waterTicks);
                    this.waterTicks = 0;
                }
            } else {
                this.waterTicks = 0;
            }
        }
        if (!this.level().isClientSide() && this.suffocationGraceTicks <= 0) {
            BlockPos feet = this.blockPosition();
            BlockPos head = feet.above();
            if (this.level().getBlockState(feet).isSuffocating((BlockGetter)this.level(), feet) && this.level().getBlockState(head).isSuffocating((BlockGetter)this.level(), head)) {
                int safeY = this.level().getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, feet.getX(), feet.getZ());
                this.teleportTo((double)feet.getX() + 0.5, safeY, (double)feet.getZ() + 0.5);
                if (this.tickCount % 100 == 0) {
                    LOGGER.warn("[Millenaire] Anti-suffocation safety net: {} TP to surface Y={} from {}", new Object[]{this.getVillagerTypeId(), safeY, feet.toShortString()});
                }
            }
        }
        if (!this.level().isClientSide() && this.level().getGameTime() % 40L == 7L && this.level().canSeeSky(this.blockPosition())) {
            this.lastOutdoorPos = this.blockPosition().immutable();
        }
        if (!this.level().isClientSide() && this.tickCount % 20 == 0 && this.villagerTypeId != null && (vType = ModCultures.getVillagerType(this.villagerTypeId)) != null) {
            this.updateClothTextures(vType);
        }
        if (!this.level().isClientSide() && this.tickCount % 100 == 0 && this.villagerTypeId != null) {
            this.unlockForNearbyPlayers();
        }
        if (!this.level().isClientSide() && this.tickCount % 100 == 0 && this.isLocalMerchant()) {
            this.localMerchantRescue();
        }
        if (!this.level().isClientSide() && Math.abs(this.level().getGameTime() + (long)this.hashCode()) % 10L == 6L) {
            this.handleLeafClearing();
        }
        if (!this.level().isClientSide() && Math.abs(this.level().getGameTime() + (long)this.hashCode()) % 10L == 5L) {
            this.combat.triggerMobAttacks();
        }
        if (!this.level().isClientSide()) {
            this.tickSleepDebt();
        }
        if (!this.level().isClientSide() && this.tickCount % 40 == 13) {
            this.enforceSleepInvariants();
        }
        if (!this.level().isClientSide() && this.goalScheduler != null) {
            GoalContext ctx = this.buildGoalContext();
            if (ctx != null) {
                this.goalScheduler.tick(ctx);
            }
            Village navVillage = ctx != null ? ctx.village() : null;
            this.navManager.tick(this, navVillage);
            VillagerTask task = this.goalScheduler.getCurrentTask();
            if (task != null) {
                if (task != this.lastTrackedTask) {
                    this.heldItemTick = 0;
                    this.heldItemIndex = 0;
                    this.offHandItemIndex = 0;
                    this.lastTrackedTask = task;
                    this.applyHeldItems(task);
                    if (!this.isSleeping() && !this.isVillagerSleeping()) {
                        this.speech.speakGoalChosen(task);
                    }
                    if (DebugCommand.isVerbose(this.getUUID())) {
                        LOGGER.info("[V-DEBUG] {} : new task \u2192 {}", (Object)this.getVillagerDisplayName(), (Object)task.goalId());
                    }
                }
                ++this.heldItemTick;
                if (this.heldItemTick >= 20) {
                    this.heldItemTick = 0;
                    this.cycleHeldItems(task);
                }
                this.updateGoalLabel(task);
                if (!this.isSleeping() && !this.isVillagerSleeping()) {
                    this.speech.tick(task);
                }
            } else {
                if (this.lastTrackedTask != null) {
                    this.clearHeldItems();
                    this.clearGoalLabel();
                    this.lastTrackedTask = null;
                }
                if (!this.isSleeping() && !this.isVillagerSleeping()) {
                    this.speech.tick(null);
                }
            }
            this.tickPassivePickup();
        }
    }

    private void unlockForNearbyPlayers() {
        Level level = this.level();
        if (!(level instanceof ServerLevel)) {
            return;
        }
        ServerLevel serverLevel = (ServerLevel)level;
        if (!((Boolean)MillenaireServerConfig.SERVER.travelBookLearning.get()).booleanValue()) {
            return;
        }
        AABB area = this.getBoundingBox().inflate(5.0);
        VillagerType vType = ModCultures.getVillagerType(this.villagerTypeId);
        if (vType == null) {
            return;
        }
        String cultureKey = vType.culture().getPath();
        String villagerKey = this.villagerTypeId.getPath();
        for (ServerPlayer player : serverLevel.getEntitiesOfClass(ServerPlayer.class, area)) {
            DiscoveryTracker tracker = DiscoveryTracker.get(serverLevel);
            if (!tracker.unlockVillager(player.getUUID(), cultureKey, villagerKey)) continue;
            player.sendSystemMessage((Component)Component.translatable((String)"travelbook.discovered.villager", (Object[])new Object[]{vType.nativeName()}));
        }
    }

    private void tickSleepDebt() {
        boolean isNight = TickConstants.isNight(this.level());
        long dayTime = this.level().getDayTime() % 24000L;
        boolean asleep = this.isSleeping() || this.isVillagerSleeping();
        this.sleepDebtTicks = MillVillager.nextSleepDebt(this.sleepDebtTicks, asleep, isNight, dayTime);
    }

    static int nextSleepDebt(int current, boolean asleep, boolean isNight, long dayTimeInCycle) {
        if (asleep) {
            return current > 0 ? Math.max(0, current - 2) : 0;
        }
        if (isNight) {
            return current < 6000 ? current + 1 : 6000;
        }
        if (current > 0 && dayTimeInCycle >= 1200L) {
            return 0;
        }
        return current;
    }

    private void handleLeafClearing() {
        Node node;
        int nextNext;
        Node node2;
        if (this.getNavigation().getPath() == null) {
            return;
        }
        Path path = this.getNavigation().getPath();
        if (path.getNodeCount() == 0) {
            return;
        }
        VillagerType vtype = ModCultures.getVillagerType(this.getVillagerTypeId());
        if (vtype != null && vtype.hasTag("noleafclearing")) {
            this.recordLeafClearSkipped(null, "noleafclearing_tag");
            return;
        }
        BlockPos.MutableBlockPos mutable = new BlockPos.MutableBlockPos();
        int nextIdx = path.getNextNodeIndex();
        if (nextIdx < path.getNodeCount() && (node2 = path.getNode(nextIdx)) != null) {
            this.clearLeafAt((BlockPos)mutable.set(node2.x, node2.y, node2.z));
            this.clearLeafAt((BlockPos)mutable.set(node2.x, node2.y + 1, node2.z));
        }
        if ((nextNext = nextIdx + 1) < path.getNodeCount() && (node = path.getNode(nextNext)) != null) {
            for (int dx = -1; dx <= 1; ++dx) {
                for (int dz = -1; dz <= 1; ++dz) {
                    this.clearLeafAt((BlockPos)mutable.set(node.x + dx, node.y, node.z + dz));
                    this.clearLeafAt((BlockPos)mutable.set(node.x + dx, node.y + 1, node.z + dz));
                }
            }
        }
    }

    private void clearLeafAt(BlockPos pos) {
        BlockState state = this.level().getBlockState(pos);
        if (!(state.getBlock() instanceof LeavesBlock)) {
            this.recordLeafClearSkipped(pos, "not_leaves");
            return;
        }
        if (((Boolean)state.getValue((Property)LeavesBlock.PERSISTENT)).booleanValue()) {
            this.recordLeafClearSkipped(pos, "persistent_leaf");
            return;
        }
        if (MillVillager.isFruitLeaves(state)) {
            this.recordLeafClearSkipped(pos, "fruit_leaf");
            return;
        }
        this.level().destroyBlock(pos, true);
    }

    private static boolean isFruitLeaves(BlockState state) {
        Block b = state.getBlock();
        return b instanceof AppleTreeLeavesBlock || b instanceof OliveTreeLeavesBlock || b instanceof PistachioTreeLeavesBlock;
    }

    private void recordLeafClearSkipped(@Nullable BlockPos pos, String reason) {
        long now = this.level().getGameTime();
        if ("not_leaves".equals(reason)) {
            if (this.lastLeafClearNotLeavesTick == now) {
                return;
            }
            this.lastLeafClearNotLeavesTick = now;
        }
        String detail = pos == null ? "reason=" + reason : "pos=" + pos.getX() + "," + pos.getY() + "," + pos.getZ() + " reason=" + reason;
        this.navEventLog.record(now, NavEvent.Layer.VNM, NavEvent.Type.LEAF_CLEAR_SKIPPED, detail);
    }

    private void tickPassivePickup() {
        if (this.tickCount % 20 != Math.floorMod(this.getUUID().hashCode(), 20)) {
            return;
        }
        if (TickConstants.isNight(this.level())) {
            return;
        }
        VillagerType vtype = ModCultures.getVillagerType(this.getVillagerTypeId());
        if (vtype == null || vtype.resolvedCollectGoods().isEmpty()) {
            return;
        }
        AABB scanBox = this.getBoundingBox().inflate(5.0, 30.0, 5.0);
        List items = this.level().getEntitiesOfClass(ItemEntity.class, scanBox);
        for (ItemEntity itemEntity : items) {
            ResourceLocation itemId = BuiltInRegistries.ITEM.getKey((Object)itemEntity.getItem().getItem());
            if (!vtype.resolvedCollectGoods().contains(itemId)) continue;
            this.getInventory().add(itemEntity.getItem().getItem(), 1);
            itemEntity.getItem().shrink(1);
            if (!itemEntity.getItem().isEmpty()) break;
            itemEntity.discard();
            break;
        }
    }

    private void applyHeldItems(VillagerTask task) {
        TravelPhase phase = task.getTravelPhase();
        List<ItemStack> mainItems = task.getHeldItems(phase);
        List<ItemStack> offItems = task.getOffHandItems(phase);
        if (!mainItems.isEmpty()) {
            this.heldItemIndex = 0;
            this.setItemSlot(EquipmentSlot.MAINHAND, mainItems.get(0));
        } else {
            this.setItemSlot(EquipmentSlot.MAINHAND, ItemStack.EMPTY);
        }
        if (!offItems.isEmpty()) {
            this.offHandItemIndex = 0;
            this.setItemSlot(EquipmentSlot.OFFHAND, offItems.get(0));
        } else {
            this.setItemSlot(EquipmentSlot.OFFHAND, ItemStack.EMPTY);
        }
    }

    private void cycleHeldItems(VillagerTask task) {
        TravelPhase phase = task.getTravelPhase();
        List<ItemStack> mainItems = task.getHeldItems(phase);
        List<ItemStack> offItems = task.getOffHandItems(phase);
        if (!mainItems.isEmpty()) {
            this.heldItemIndex = (this.heldItemIndex + 1) % mainItems.size();
            this.setItemSlot(EquipmentSlot.MAINHAND, mainItems.get(this.heldItemIndex));
        } else {
            this.setItemSlot(EquipmentSlot.MAINHAND, ItemStack.EMPTY);
        }
        if (!offItems.isEmpty()) {
            this.offHandItemIndex = (this.offHandItemIndex + 1) % offItems.size();
            this.setItemSlot(EquipmentSlot.OFFHAND, offItems.get(this.offHandItemIndex));
        } else {
            this.setItemSlot(EquipmentSlot.OFFHAND, ItemStack.EMPTY);
        }
    }

    private void updateGoalLabel(VillagerTask task) {
        Component label = task.getGoalLabel();
        String key = "";
        if (label != null) {
            ComponentContents componentContents = label.getContents();
            if (componentContents instanceof TranslatableContents) {
                TranslatableContents tc = (TranslatableContents)componentContents;
                key = tc.getKey();
            } else {
                key = label.getString();
            }
        }
        if (!key.equals(this.lastGoalLabel)) {
            this.lastGoalLabel = key;
            this.entityData.set(DATA_GOAL_LABEL, (Object)key);
        }
    }

    public ItemStack getItemBySlot(EquipmentSlot slot) {
        String categoryId = ARMOR_SLOT_CATEGORIES.get(slot);
        if (categoryId == null) {
            return super.getItemBySlot(slot);
        }
        ToolCategory category = ToolCategoryRegistry.get(categoryId);
        if (category == null) {
            return ItemStack.EMPTY;
        }
        ToolCategory.ToolEntry best = category.getBestOwned(item -> this.getInventory().getCount((Item)item) > 0);
        return best != null && best.item() != null ? new ItemStack((ItemLike)best.item()) : ItemStack.EMPTY;
    }

    private void clearHeldItems() {
        this.setItemSlot(EquipmentSlot.MAINHAND, ItemStack.EMPTY);
        this.setItemSlot(EquipmentSlot.OFFHAND, ItemStack.EMPTY);
        this.setItemSlot(EquipmentSlot.HEAD, ItemStack.EMPTY);
        this.setItemSlot(EquipmentSlot.CHEST, ItemStack.EMPTY);
        this.setItemSlot(EquipmentSlot.LEGS, ItemStack.EMPTY);
        this.setItemSlot(EquipmentSlot.FEET, ItemStack.EMPTY);
        this.heldItemTick = 0;
        this.heldItemIndex = 0;
        this.offHandItemIndex = 0;
    }

    private void clearGoalLabel() {
        if (!this.lastGoalLabel.isEmpty()) {
            this.lastGoalLabel = "";
            this.entityData.set(DATA_GOAL_LABEL, (Object)"");
        }
    }

    public VillagerSpeech getSpeech() {
        return this.speech;
    }

    public String getSpeechText() {
        return this.speech.getSpeechText();
    }

    public void speakCombatCall(String key) {
        this.speech.speakCombatCall(key);
    }

    public void setSpeechText(String text) {
        this.speech.setSpeechText(text);
    }

    void setSpeechData(String text) {
        this.entityData.set(DATA_SPEECH_TEXT, (Object)text);
    }

    String getSpeechData() {
        return (String)this.entityData.get(DATA_SPEECH_TEXT);
    }

    @Nullable
    public GoalContext buildGoalContext() {
        Level level = this.level();
        if (!(level instanceof ServerLevel)) {
            return null;
        }
        ServerLevel serverLevel = (ServerLevel)level;
        if (this.villageId == null) {
            return null;
        }
        Village village = Village.resolve(serverLevel, this.villageId);
        if (village == null) {
            return null;
        }
        long dayTime = serverLevel.getDayTime() % 24000L;
        long gameTime = serverLevel.getGameTime();
        return new GoalContext(this, village, serverLevel, dayTime, gameTime);
    }

    public void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        if (this.villageId != null) {
            tag.putUUID("villageId", this.villageId.uuid());
        }
        if (this.villagerTypeId != null) {
            tag.putString("villagerType", this.villagerTypeId.toString());
        }
        if (this.homeBuilding != null) {
            tag.putUUID("homeBuilding", this.homeBuilding.uuid());
        }
        if (this.constructionBuildingId != null) {
            tag.putUUID("constructionBuilding", this.constructionBuildingId.uuid());
        }
        if (this.foreignMerchantStallId >= 0) {
            tag.putInt("foreignMerchantStallId", this.foreignMerchantStallId);
        }
        if (this.visitorNbNights > 0) {
            tag.putInt("visitorNbNights", this.visitorNbNights);
        }
        this.identity.save(tag);
        this.inventory.save(tag);
        if (this.isVillagerSleeping()) {
            tag.putBoolean("customSleeping", true);
        }
        if (this.sleepDebtTicks > 0) {
            tag.putInt("sleepDebtTicks", this.sleepDebtTicks);
        }
        if (this.combat.isRaiderEntity()) {
            tag.putBoolean("raider_entity", true);
        }
        if (this.hiredBy != null) {
            tag.putUUID("hiredBy", this.hiredBy);
            tag.putLong("hiredUntil", this.hiredUntil);
            tag.putBoolean("aggressiveStance", this.aggressiveStance);
        }
    }

    public void readAdditionalSaveData(CompoundTag tag) {
        String[] names;
        VillagerType vType;
        super.readAdditionalSaveData(tag);
        if (tag.hasUUID("villageId")) {
            this.villageId = new VillageId(tag.getUUID("villageId"));
        }
        if (tag.contains("villagerType")) {
            this.villagerTypeId = ResourceLocation.parse((String)tag.getString("villagerType"));
        }
        if (tag.hasUUID("homeBuilding")) {
            this.homeBuilding = new BuildingId(tag.getUUID("homeBuilding"));
        }
        if (tag.hasUUID("constructionBuilding")) {
            this.constructionBuildingId = new BuildingId(tag.getUUID("constructionBuilding"));
        }
        if (tag.contains("foreignMerchantStallId")) {
            this.foreignMerchantStallId = tag.getInt("foreignMerchantStallId");
            if (this.foreignMerchantStallId >= 0) {
                this.entityData.set(DATA_FOREIGN_MERCHANT, (Object)true);
            }
        }
        if (tag.contains("visitorNbNights")) {
            this.visitorNbNights = tag.getInt("visitorNbNights");
        }
        if (tag.contains("sleepDebtTicks")) {
            this.sleepDebtTicks = Math.max(0, Math.min(6000, tag.getInt("sleepDebtTicks")));
        }
        this.combat.setRaiderEntity(tag.getBoolean("raider_entity"));
        if (tag.hasUUID("hiredBy")) {
            this.hiredBy = tag.getUUID("hiredBy");
            this.hiredUntil = tag.getLong("hiredUntil");
            this.aggressiveStance = tag.getBoolean("aggressiveStance");
            this.entityData.set(DATA_HIRED, (Object)true);
            this.entityData.set(DATA_STANCE_AGGRESSIVE, (Object)this.aggressiveStance);
        }
        VillagerIdentity loaded = VillagerIdentity.load(tag);
        this.identity.setModelType(loaded.getModelType());
        this.identity.setTexture(loaded.getTexture());
        this.identity.setClothTexture0(loaded.getClothTexture0());
        this.identity.setClothTexture1(loaded.getClothTexture1());
        this.identity.setVillagerScale(loaded.getVillagerScale());
        this.identity.setFirstName(loaded.getFirstName());
        this.identity.setFamilyName(loaded.getFamilyName());
        this.identity.setRoleName(loaded.getRoleName());
        this.identity.setFathersName(loaded.getFathersName());
        this.identity.setMothersName(loaded.getMothersName());
        this.identity.setSpousesName(loaded.getSpousesName());
        this.identity.setMaidenName(loaded.getMaidenName());
        this.identity.setClothName(loaded.getClothName());
        if (loaded.getChildSize() >= 0) {
            this.setChildSize(loaded.getChildSize());
        }
        if (VillagerAppearanceFactory.isCorruptedName(this.identity.getFirstName()) && this.villagerTypeId != null && (vType = ModCultures.getVillagerType(this.villagerTypeId)) != null && !VillagerAppearanceFactory.isCorruptedName((names = VillagerAppearanceFactory.generateName(vType))[0])) {
            this.identity.setFirstName(names[0]);
            this.identity.setFamilyName(names[1]);
            LOGGER.info("[Mill\u00e9naire] Regenerated corrupted name for {} \u2192 {}", (Object)this.villagerTypeId, (Object)names[0]);
        }
        this.entityData.set(DATA_VILLAGER_TYPE, (Object)(this.villagerTypeId != null ? this.villagerTypeId.toString() : ""));
        this.entityData.set(DATA_MODEL_TYPE, (Object)this.identity.getModelType().toByte());
        this.entityData.set(DATA_TEXTURE, (Object)(this.identity.getTexture() != null ? this.identity.getTexture().toString() : ""));
        this.entityData.set(DATA_CLOTH_0, (Object)(this.identity.getClothTexture0() != null ? this.identity.getClothTexture0().toString() : ""));
        this.entityData.set(DATA_CLOTH_1, (Object)(this.identity.getClothTexture1() != null ? this.identity.getClothTexture1().toString() : ""));
        this.entityData.set(DATA_SCALE, (Object)Float.valueOf(this.identity.getVillagerScale()));
        this.entityData.set(DATA_DISPLAY_NAME, (Object)this.getVillagerDisplayName());
        this.entityData.set(DATA_ROLE_NAME, (Object)this.identity.getRoleName());
        this.syncNativeRoleName();
        if (this.villagerTypeId != null) {
            VillagerType vType2 = ModCultures.getVillagerType(this.villagerTypeId);
            this.entityData.set(DATA_IS_CHIEF, (Object)(vType2 != null && vType2.hasTag("chief") ? 1 : 0));
        }
        this.inventory.load(tag);
        if (tag.getBoolean("customSleeping")) {
            this.entityData.set(DATA_SLEEPING, (Object)true);
        }
        this.setItemSlot(EquipmentSlot.MAINHAND, ItemStack.EMPTY);
        this.setItemSlot(EquipmentSlot.OFFHAND, ItemStack.EMPTY);
        VillagerType villagerType = vType = this.villagerTypeId != null ? ModCultures.getVillagerType(this.villagerTypeId) : null;
        if (vType != null) {
            this.updateClothTextures(vType);
        }
    }

    private void enforceSleepInvariants() {
        boolean vanillaSleeping = this.isSleeping();
        boolean customSleeping = this.isVillagerSleeping();
        if (!vanillaSleeping && !customSleeping) {
            return;
        }
        if (TickConstants.isNight(this.level())) {
            return;
        }
        if (this.goalScheduler != null && RestGoal.ID.equals((Object)this.goalScheduler.getCurrentGoalId())) {
            return;
        }
        if (vanillaSleeping) {
            this.stopSleeping();
        }
        if (customSleeping) {
            this.setVillagerSleeping(false);
        }
        if (this.getPose() == Pose.SLEEPING) {
            this.setPose(Pose.STANDING);
        }
        this.navEventLog.record(this.level().getGameTime(), NavEvent.Layer.SCHEDULER, NavEvent.Type.POSE_SLEEPING_CLEARED, "reason=daytime-safeguard");
        NavigationCounters.incPoseSleepingCleared();
        LOGGER.debug("[Mill\u00e9naire] BUG-164 safeguard: cleared stuck sleep state on {} (daytime, no RestGoal active)", (Object)this.getVillagerDisplayName());
    }

    public void onAddedToLevel() {
        boolean shouldPreserve;
        super.onAddedToLevel();
        Level level = this.level();
        if (!(level instanceof ServerLevel)) {
            return;
        }
        ServerLevel serverLevel = (ServerLevel)level;
        if (this.isRaiderEntity() && this.getVillageId() != null) {
            VillagerRecord record;
            Village village = Village.resolve(serverLevel, this.getVillageId());
            VillagerRecord villagerRecord = record = village != null ? village.getVillagerRecord(this.getUUID()) : null;
            if (record == null || !record.isRaidingVillage()) {
                this.discard();
                return;
            }
        }
        boolean vanillaSleeping = this.getSleepingPos().isPresent();
        boolean customSleeping = this.isVillagerSleeping();
        if (!vanillaSleeping && !customSleeping) {
            return;
        }
        long gameTime = this.level().getGameTime();
        boolean isNight = TickConstants.isNight(this.level());
        long dayTime = this.level().getDayTime() % 24000L;
        boolean bedValid = false;
        if (vanillaSleeping) {
            BlockPos bedPos = (BlockPos)this.getSleepingPos().get();
            bedValid = this.level().getBlockState(bedPos).getBlock() instanceof BedBlock;
        }
        boolean bl = shouldPreserve = isNight && (customSleeping || bedValid);
        if (shouldPreserve) {
            this.navEventLog.record(gameTime, NavEvent.Layer.RELOAD, NavEvent.Type.POSE_SLEEPING_RESTORED, bedValid ? "via=bed" : "via=custom");
            NavigationCounters.incPoseSleepingRestored();
        } else {
            String reason;
            String string = reason = !isNight ? "daytime" : "bed-gone";
            if (vanillaSleeping) {
                this.stopSleeping();
            }
            if (customSleeping) {
                this.setVillagerSleeping(false);
            }
            if (this.getPose() == Pose.SLEEPING) {
                this.setPose(Pose.STANDING);
            }
            this.navEventLog.record(gameTime, NavEvent.Layer.RELOAD, NavEvent.Type.POSE_SLEEPING_CLEARED, "reason=" + reason);
            NavigationCounters.incPoseSleepingCleared();
        }
    }
}

