/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.mojang.logging.LogUtils
 *  javax.annotation.Nullable
 *  net.minecraft.core.BlockPos
 *  net.minecraft.core.Vec3i
 *  net.minecraft.core.registries.BuiltInRegistries
 *  net.minecraft.network.chat.Component
 *  net.minecraft.resources.ResourceLocation
 *  net.minecraft.server.level.ServerLevel
 *  net.minecraft.server.level.ServerPlayer
 *  net.minecraft.world.entity.Entity
 *  net.minecraft.world.item.Item
 *  net.minecraft.world.level.Level
 *  org.slf4j.Logger
 */
package org.millenaire.combat.raid;

import com.mojang.logging.LogUtils;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;
import javax.annotation.Nullable;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Vec3i;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.Level;
import org.millenaire.advancement.MillAdvancements;
import org.millenaire.building.BuildingInstance;
import org.millenaire.building.BuildingInventory;
import org.millenaire.combat.raid.ChestLockDecision;
import org.millenaire.combat.raid.RaidAdvancementResolver;
import org.millenaire.combat.raid.RaidLootPlanner;
import org.millenaire.combat.raid.RaidSpawnLocator;
import org.millenaire.commerce.TradeGood;
import org.millenaire.commerce.TradeGoodsLoader;
import org.millenaire.config.MillenaireServerConfig;
import org.millenaire.culture.ModCultures;
import org.millenaire.culture.VillageType;
import org.millenaire.culture.VillagerType;
import org.millenaire.entity.MillVillager;
import org.millenaire.entity.VillagerSpawnFactory;
import org.millenaire.village.Village;
import org.millenaire.village.VillageId;
import org.millenaire.village.VillageManager;
import org.millenaire.village.VillageSavedData;
import org.millenaire.village.VillagerRecord;
import org.slf4j.Logger;

public final class RaidManager {
    private static final Logger LOGGER = LogUtils.getLogger();

    private RaidManager() {
    }

    public static boolean resolveAttackersWin(int defendingStrength, int attackerStrength, float defendingFactor, float attackingFactor) {
        float defendingForce = (float)defendingStrength * defendingFactor;
        float attackingForce = (float)attackerStrength * attackingFactor;
        if (attackingForce == 0.0f) {
            return false;
        }
        if (defendingForce == 0.0f) {
            return true;
        }
        return attackingForce / defendingForce > 1.2f;
    }

    public static boolean shouldEndRaid(int liveAttackers, int liveDefenders, boolean raiderAtDefendingPos) {
        if (liveAttackers == 0) {
            return true;
        }
        if (liveDefenders == 0) {
            return raiderAtDefendingPos;
        }
        return false;
    }

    public static int maxAcceptableTargetStrength(int attackerRaidingStrength) {
        return attackerRaidingStrength * 2;
    }

    public static boolean isBanditViableTarget(int targetDefendingStrength, int maxAcceptable, boolean targetIsLoneBuilding, boolean withinRadius) {
        if (targetIsLoneBuilding) {
            return false;
        }
        if (!withinRadius) {
            return false;
        }
        return targetDefendingStrength < maxAcceptable;
    }

    public static boolean isRegularViableTarget(int targetDefendingStrength, int maxAcceptable, int relation) {
        if (relation >= -90) {
            return false;
        }
        return targetDefendingStrength < maxAcceptable;
    }

    public static void attemptPlanNewRaid(Village attacker, ServerLevel level) {
        if (attacker == null || attacker.getRaidTarget() != null) {
            return;
        }
        VillageType vt = ModCultures.getVillageType(attacker.getVillageTypeId());
        if (vt == null) {
            return;
        }
        BlockPos c = attacker.getCenter();
        int bgRadius = (Integer)MillenaireServerConfig.SERVER.backgroundRadius.get();
        if (!level.hasNearbyAlivePlayer((double)c.getX() + 0.5, (double)c.getY() + 0.5, (double)c.getZ() + 0.5, (double)bgRadius)) {
            return;
        }
        boolean isBandit = vt.loneBuilding();
        if (!vt.carriesRaid() && !isBandit) {
            return;
        }
        int rate = (Integer)MillenaireServerConfig.SERVER.raidingRate.get();
        if (rate <= 0) {
            return;
        }
        if (ThreadLocalRandom.current().nextInt(100) >= rate) {
            return;
        }
        for (VillagerRecord r : attacker.getVillagerRecords().values()) {
            if (!r.isRaidingVillage()) continue;
            return;
        }
        Village target = RaidManager.selectRaidTarget(attacker, level, isBandit);
        if (target == null) {
            return;
        }
        RaidManager.planRaid(attacker, target, level);
    }

    @Nullable
    private static Village selectRaidTarget(Village attacker, ServerLevel level, boolean isBandit) {
        int maxAcceptable = RaidManager.maxAcceptableTargetStrength(attacker.getVillageRaidingStrength());
        if (maxAcceptable <= 0) {
            return null;
        }
        VillageManager vm = VillageSavedData.get(level).getVillageManager();
        ArrayList<Village> candidates = new ArrayList<Village>();
        if (isBandit) {
            int radius = (Integer)MillenaireServerConfig.SERVER.banditRaidRadius.get();
            long radiusSq = (long)radius * (long)radius;
            for (Village v : vm.getAllVillages()) {
                boolean withinRadius;
                VillageType ovt;
                if (v == attacker || (ovt = ModCultures.getVillageType(v.getVillageTypeId())) == null) continue;
                boolean bl = withinRadius = v.getCenter().distSqr((Vec3i)attacker.getCenter()) < (double)radiusSq;
                if (!RaidManager.isBanditViableTarget(v.getVillageDefendingStrength(), maxAcceptable, ovt.loneBuilding(), withinRadius)) continue;
                candidates.add(v);
            }
        } else {
            for (Map.Entry<VillageId, Integer> entry : attacker.getRelations().entrySet()) {
                Village v = vm.getVillage(entry.getKey());
                if (v == null || !RaidManager.isRegularViableTarget(v.getVillageDefendingStrength(), maxAcceptable, entry.getValue())) continue;
                candidates.add(v);
            }
        }
        if (candidates.isEmpty()) {
            return null;
        }
        return (Village)candidates.get(ThreadLocalRandom.current().nextInt(candidates.size()));
    }

    public static void planRaid(Village attacker, Village target, ServerLevel level) {
        attacker.setRaidTarget(target.getId());
        attacker.setRaidPlanningStart(level.getDayTime());
        attacker.setRaidStart(0L);
        String attackerName = RaidManager.nameOf(attacker);
        String targetName = RaidManager.nameOf(target);
        RaidManager.broadcastNear(level, attacker.getCenter(), (Component)Component.translatable((String)"millenaire.combat.raid_planned", (Object[])new Object[]{Component.literal((String)attackerName), Component.literal((String)targetName)}));
        LOGGER.info("[Millenaire] Raid planned: {} -> {} at day-time {}.", new Object[]{attackerName, targetName, level.getDayTime()});
    }

    public static void tickRaid(Village village, ServerLevel level) {
        long now = level.getDayTime();
        if (village.isUnderAttack()) {
            RaidManager.materializeRaiders(village, level, now);
        }
        if (village.getRaidTarget() != null && village.getRaidStart() == 0L && village.getRaidPlanningStart() > 0L && now - village.getRaidPlanningStart() >= 24000L) {
            Village target = VillageSavedData.get(level).getVillageManager().getVillage(village.getRaidTarget());
            if (target == null) {
                village.clearRaid();
            } else {
                RaidManager.startRaid(village, target, level);
            }
        }
        if (village.isUnderAttack() && level.getGameTime() % 10L == Math.floorMod((long)village.getId().uuid().hashCode(), 10L)) {
            RaidManager.checkBattleStatus(village, level);
        }
        if (village.isUnderAttack()) {
            Village attacker;
            VillageId attackerId = RaidManager.findAttackerId(village);
            Village village2 = attacker = attackerId != null ? VillageSavedData.get(level).getVillageManager().getVillage(attackerId) : null;
            if (attacker == null) {
                RaidManager.cleanupClones(village, level);
                village.setUnderAttack(false);
            } else if (attacker.getRaidStart() > 0L && now - attacker.getRaidStart() >= 23000L) {
                LOGGER.warn("[Millenaire] Raid timeout on {} (under attack for {} ticks)", (Object)RaidManager.nameOf(village), (Object)(now - attacker.getRaidStart()));
                RaidManager.endRaid(attacker, village, level);
            }
        }
    }

    public static void startRaid(Village attacker, Village target, ServerLevel level) {
        RaidManager.startRaidInternal(attacker, target, level, false);
    }

    public static void startRaidForced(Village attacker, Village target, ServerLevel level) {
        RaidManager.startRaidInternal(attacker, target, level, true);
    }

    static boolean shouldAbortDueToImprovedRelations(boolean isBandit, Integer relation, boolean skipRelationCheck) {
        if (skipRelationCheck) {
            return false;
        }
        if (isBandit) {
            return false;
        }
        if (relation == null) {
            return false;
        }
        return relation > -90;
    }

    private static void startRaidInternal(Village attacker, Village target, ServerLevel level, boolean skipRelationCheck) {
        Integer rel;
        VillageType attackerVT = ModCultures.getVillageType(attacker.getVillageTypeId());
        boolean isBandit = attackerVT != null && attackerVT.loneBuilding();
        Integer n = rel = isBandit ? null : attacker.getRelations().get(target.getId());
        if (RaidManager.shouldAbortDueToImprovedRelations(isBandit, rel, skipRelationCheck)) {
            LOGGER.info("[Millenaire] Raid aborted (relation improved): {} -> {}.", (Object)RaidManager.nameOf(attacker), (Object)RaidManager.nameOf(target));
            attacker.clearRaid();
            return;
        }
        ArrayList<VillagerRecord> raiders = new ArrayList<VillagerRecord>();
        for (VillagerRecord r : attacker.getVillagerRecords().values()) {
            VillagerType type;
            if (r.isKilled() || r.isRaidingVillage() || r.isAwayRaiding() || r.isAwayHired() || (type = ModCultures.getVillagerType(r.getVillagerTypeId())) == null || !type.isRaider()) continue;
            raiders.add(r);
        }
        int sent = 0;
        for (VillagerRecord r : raiders) {
            if (!RaidManager.sendVillagerOnRaid(r, attacker, target, level)) continue;
            ++sent;
        }
        if (sent == 0) {
            attacker.clearRaid();
            return;
        }
        target.setUnderAttack(true);
        attacker.setRaidStart(level.getDayTime());
        target.clearRaid();
        attacker.invalidateDefenderCountAndReevaluate(level);
        RaidManager.broadcastNear(level, target.getCenter(), (Component)Component.translatable((String)"millenaire.combat.raid_started", (Object[])new Object[]{Component.literal((String)RaidManager.nameOf(attacker)), Component.literal((String)RaidManager.nameOf(target)), Component.literal((String)String.valueOf(sent))}));
        LOGGER.info("[Millenaire] Raid started: {} dispatched {} raiders to {}.", new Object[]{RaidManager.nameOf(attacker), sent, RaidManager.nameOf(target)});
    }

    public static boolean sendVillagerOnRaid(VillagerRecord original, Village attacker, Village target, ServerLevel level) {
        original.setAwayRaiding(true);
        VillagerRecord clone = new VillagerRecord(UUID.randomUUID(), original.getVillagerTypeId(), null);
        clone.copyAppearanceAndInventoryFrom(original);
        clone.setRaidingVillage(true);
        clone.setOriginalVillageId(attacker.getId());
        clone.setRaiderSpawn(level.getDayTime());
        target.addVillager(clone);
        Entity entity = level.getEntity(original.getUuid());
        if (entity instanceof MillVillager) {
            MillVillager v = (MillVillager)entity;
            v.discard();
        }
        attacker.markDirty();
        target.markDirty();
        return true;
    }

    static void materializeRaiders(Village target, ServerLevel level, long now) {
        ArrayList<UUID> cloneUuids = new ArrayList<UUID>();
        for (VillagerRecord r : target.getVillagerRecords().values()) {
            if (!r.isRaidingVillage()) continue;
            cloneUuids.add(r.getUuid());
        }
        for (UUID uuid : cloneUuids) {
            VillagerRecord clone = target.getVillagerRecords().get(uuid);
            if (clone == null || level.getEntity(uuid) instanceof MillVillager || clone.isKilled() || now <= clone.getRaiderSpawn() + 500L) continue;
            VillageId originalId = clone.getOriginalVillageId();
            BlockPos attackerCenter = originalId != null ? RaidManager.attackerCenterOf(originalId, target, level) : target.getCenter();
            BlockPos spawnPos = RaidSpawnLocator.findSpawnPoint(level, target, attackerCenter);
            if (!level.isPositionEntityTicking(spawnPos)) {
                BlockPos fallback = RaidManager.resolveDefendingPos(target);
                if (!level.isPositionEntityTicking(fallback)) continue;
                spawnPos = fallback;
            }
            target.removeVillagerRecord(uuid);
            MillVillager spawned = VillagerSpawnFactory.spawnInVillage(level, target, clone.getVillagerTypeId(), spawnPos, null, clone);
            if (spawned == null) {
                clone.setUuid(uuid);
                target.addVillager(clone);
                continue;
            }
            target.markDirty();
            LOGGER.info("[Millenaire] Raider clone materialized at village edge {} (pos {}).", (Object)RaidManager.nameOf(target), (Object)spawnPos.toShortString());
        }
    }

    private static BlockPos attackerCenterOf(VillageId attackerId, Village target, ServerLevel level) {
        Village attacker = VillageSavedData.get(level).getVillageManager().getVillage(attackerId);
        return attacker != null ? attacker.getCenter() : target.getCenter();
    }

    public static BlockPos resolveDefendingPos(Village target) {
        BuildingInstance th = target.getTownhall();
        if (th != null) {
            BlockPos p = RaidManager.firstPoint(th, "defendingPos", "sellingPos", "sleepingPos", "pathStartPos");
            if (p != null) {
                return p;
            }
            if (th.getOrigin() != null) {
                return th.getOrigin();
            }
        }
        return target.getCenter();
    }

    @Nullable
    static BlockPos firstPoint(BuildingInstance th, String ... types) {
        for (String type : types) {
            BlockPos p = th.getFirstPointPos(type);
            if (p == null) continue;
            return p;
        }
        return null;
    }

    public static void checkBattleStatus(Village target, ServerLevel level) {
        Village attacker;
        int liveAttackers = 0;
        int liveDefenders = 0;
        boolean raiderAtPos = false;
        BlockPos defendingPos = RaidManager.resolveDefendingPos(target);
        for (VillagerRecord r : target.getVillagerRecords().values()) {
            VillagerType t;
            if (r.isRaidingVillage()) {
                MillVillager mv;
                if (r.isKilled()) continue;
                ++liveAttackers;
                Entity entity = level.getEntity(r.getUuid());
                if (!(entity instanceof MillVillager) || !((mv = (MillVillager)entity).blockPosition().distSqr((Vec3i)defendingPos) < 25.0)) continue;
                raiderAtPos = true;
                continue;
            }
            if (r.isKilled() || (t = ModCultures.getVillagerType(r.getVillagerTypeId())) == null || !t.isHelpInAttacks()) continue;
            ++liveDefenders;
        }
        switch (ChestLockDecision.decide(target.areChestsLocked(), liveDefenders)) {
            case LOCK: {
                target.lockAllChestsPublic(level);
                break;
            }
            case UNLOCK: {
                target.unlockAllChestsPublic(level);
                break;
            }
        }
        if (!RaidManager.shouldEndRaid(liveAttackers, liveDefenders, raiderAtPos)) {
            return;
        }
        VillageId attackerId = RaidManager.findAttackerId(target);
        Village village = attacker = attackerId != null ? VillageSavedData.get(level).getVillageManager().getVillage(attackerId) : null;
        if (attacker == null) {
            RaidManager.cleanupClones(target, level);
            target.setUnderAttack(false);
            return;
        }
        RaidManager.endRaid(attacker, target, level);
    }

    static Component raidSuccessMessage(String attackerName, String targetName, int totalStolen) {
        return Component.translatable((String)"millenaire.combat.raid_success", (Object[])new Object[]{Component.literal((String)attackerName), Component.literal((String)targetName), Component.literal((String)String.valueOf(totalStolen))});
    }

    static Component raidFailureMessage(String targetName, String attackerName) {
        return Component.translatable((String)"millenaire.combat.raid_failure", (Object[])new Object[]{Component.literal((String)targetName), Component.literal((String)attackerName)});
    }

    public static void endRaid(Village attacker, Village target, ServerLevel level) {
        int defending = target.getVillageDefendingStrength();
        int attacking = target.getVillageAttackerStrength();
        float defFactor = 1.0f + ThreadLocalRandom.current().nextFloat();
        float atkFactor = 1.0f + ThreadLocalRandom.current().nextFloat();
        boolean attackersWon = RaidManager.resolveAttackersWin(defending, attacking, defFactor, atkFactor);
        String attackerName = RaidManager.nameOf(attacker);
        String targetName = RaidManager.nameOf(target);
        if (attackersWon) {
            Map<Item, Integer> loot = RaidManager.applyLooting(attacker, target, level);
            int totalStolen = 0;
            for (int qty : loot.values()) {
                totalStolen += qty;
            }
            String lootDetail = RaidManager.formatLootHistory(loot);
            attacker.addRaidsPerformed("success;" + targetName + lootDetail);
            target.addRaidsSuffered("success;" + attackerName + lootDetail);
            RaidManager.broadcastNear(level, target.getCenter(), RaidManager.raidSuccessMessage(attackerName, targetName, totalStolen));
            LOGGER.info("[Millenaire] Raid SUCCESS: {} -> {} ({} goods stolen).", new Object[]{attackerName, targetName, totalStolen});
        } else {
            attacker.addRaidsPerformed("failure;" + targetName);
            target.addRaidsSuffered("failure;" + attackerName);
            RaidManager.broadcastNear(level, target.getCenter(), RaidManager.raidFailureMessage(targetName, attackerName));
            LOGGER.info("[Millenaire] Raid FAILURE: {} repulsed at {}.", (Object)attackerName, (Object)targetName);
        }
        RaidManager.grantRaidAdvancements(attacker, target, attackersWon, level);
        ArrayList<VillagerRecord> clones = new ArrayList<VillagerRecord>();
        for (VillagerRecord r : target.getVillagerRecords().values()) {
            if (!r.isRaidingVillage()) continue;
            clones.add(r);
        }
        block2: for (VillagerRecord original : attacker.getVillagerRecords().values()) {
            if (!original.isAwayRaiding()) continue;
            original.setAwayRaiding(false);
            for (VillagerRecord clone : clones) {
                if (clone.getVillagerTypeId() == null || !clone.getVillagerTypeId().equals((Object)original.getVillagerTypeId()) || !clone.getFirstName().equals(original.getFirstName()) || !clone.getFamilyName().equals(original.getFamilyName())) continue;
                if (!clone.isKilled()) continue block2;
                attacker.markVillagerKilled(original.getUuid());
                continue block2;
            }
        }
        attacker.invalidateDefenderCountAndReevaluate(level);
        RaidManager.cleanupClones(target, level);
        attacker.clearRaid();
        target.setUnderAttack(false);
        attacker.markDirty();
        target.markDirty();
    }

    private static void grantRaidAdvancements(Village attacker, Village target, boolean attackersWon, ServerLevel level) {
        List<RaidAdvancementResolver.Grant> grants = RaidAdvancementResolver.resolve(attacker.getCultureId(), target.getCultureId(), attacker.getOwnerUUID(), target.getOwnerUUID(), attackersWon);
        if (grants.isEmpty()) {
            return;
        }
        for (RaidAdvancementResolver.Grant grant : grants) {
            ServerPlayer player = level.getServer().getPlayerList().getPlayer(grant.playerUuid());
            if (player == null) continue;
            MillAdvancements.grant(player, grant.advancement());
        }
    }

    private static Map<Item, Integer> applyLooting(Village attacker, Village target, ServerLevel level) {
        BuildingInstance attackerTh = attacker.getTownhall();
        if (attackerTh == null || attackerTh.getInventory() == null) {
            return Map.of();
        }
        List<TradeGood> goods = TradeGoodsLoader.getGoods(attacker.getCultureId());
        if (goods.isEmpty()) {
            return Map.of();
        }
        HashMap<Item, Integer> targetContents = new HashMap<Item, Integer>();
        for (BuildingInstance buildingInstance : target.getBuildings()) {
            BuildingInventory inv = buildingInstance.getInventory();
            if (inv == null) continue;
            for (Map.Entry<Item, Integer> e : inv.scanChests((Level)level).entrySet()) {
                targetContents.merge(e.getKey(), e.getValue(), Integer::sum);
            }
        }
        if (targetContents.isEmpty()) {
            return Map.of();
        }
        ArrayList<RaidLootPlanner.GoodNeed> needs = new ArrayList<RaidLootPlanner.GoodNeed>();
        for (TradeGood good : goods) {
            Item item = good.resolveItem();
            if (item == null) continue;
            needs.add(new RaidLootPlanner.GoodNeed(item, good.targetQuantity()));
        }
        Map<Item, Integer> map = RaidLootPlanner.plan(needs, targetContents, 1024);
        if (map.isEmpty()) {
            LOGGER.info("[Millenaire] Loot: nothing to take from {} (no culture goods present).", (Object)RaidManager.nameOf(target));
            return Map.of();
        }
        LinkedHashMap<Item, Integer> actualStolen = new LinkedHashMap<Item, Integer>();
        int stolen = 0;
        block3: for (Map.Entry<Item, Integer> plan : map.entrySet()) {
            Item item = plan.getKey();
            int wanted = plan.getValue();
            for (BuildingInstance b : target.getBuildings()) {
                int removed;
                if (wanted <= 0) continue block3;
                BuildingInventory inv = b.getInventory();
                if (inv == null || (removed = inv.remove((Level)level, item, wanted)) <= 0) continue;
                attackerTh.getInventory().add((Level)level, item, removed);
                wanted -= removed;
                stolen += removed;
                actualStolen.merge(item, removed, Integer::sum);
            }
        }
        LOGGER.info("[Millenaire] Loot: {} culture-good units transferred from {} to {}.", new Object[]{stolen, RaidManager.nameOf(target), RaidManager.nameOf(attacker)});
        return actualStolen;
    }

    static String formatLootHistory(Map<Item, Integer> loot) {
        StringBuilder sb = new StringBuilder();
        for (Map.Entry<Item, Integer> e : loot.entrySet()) {
            ResourceLocation id = BuiltInRegistries.ITEM.getKey((Object)e.getKey());
            sb.append(';').append(id).append('/').append(e.getValue());
        }
        return sb.toString();
    }

    private static void cleanupClones(Village target, ServerLevel level) {
        ArrayList<UUID> toRemove = new ArrayList<UUID>();
        for (VillagerRecord r : target.getVillagerRecords().values()) {
            if (!r.isRaidingVillage()) continue;
            Entity entity = level.getEntity(r.getUuid());
            if (entity instanceof MillVillager) {
                MillVillager mv = (MillVillager)entity;
                mv.discard();
            }
            toRemove.add(r.getUuid());
        }
        for (UUID id : toRemove) {
            target.removeVillagerRecord(id);
        }
    }

    @Nullable
    private static VillageId findAttackerId(Village target) {
        for (VillagerRecord r : target.getVillagerRecords().values()) {
            if (!r.isRaidingVillage() || r.getOriginalVillageId() == null) continue;
            return r.getOriginalVillageId();
        }
        return null;
    }

    private static String nameOf(Village v) {
        return v.getVillageName() != null ? v.getVillageName() : v.getVillageTypeId().getPath();
    }

    private static void broadcastNear(ServerLevel level, BlockPos center, Component msg) {
        int radius = (Integer)MillenaireServerConfig.SERVER.backgroundRadius.get();
        if (radius <= 0) {
            radius = 2000;
        }
        long radiusSq = (long)radius * (long)radius;
        for (ServerPlayer p : level.players()) {
            if (!(p.blockPosition().distSqr((Vec3i)center) <= (double)radiusSq)) continue;
            p.sendSystemMessage(msg);
        }
    }
}

