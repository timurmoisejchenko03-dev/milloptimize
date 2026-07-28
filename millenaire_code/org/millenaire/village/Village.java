/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.mojang.logging.LogUtils
 *  javax.annotation.Nullable
 *  net.minecraft.core.BlockPos
 *  net.minecraft.core.Position
 *  net.minecraft.core.RegistryAccess
 *  net.minecraft.core.SectionPos
 *  net.minecraft.core.Vec3i
 *  net.minecraft.core.registries.BuiltInRegistries
 *  net.minecraft.network.chat.Component
 *  net.minecraft.network.chat.MutableComponent
 *  net.minecraft.network.protocol.common.custom.CustomPacketPayload
 *  net.minecraft.resources.ResourceLocation
 *  net.minecraft.server.level.ServerLevel
 *  net.minecraft.server.level.ServerPlayer
 *  net.minecraft.tags.TagKey
 *  net.minecraft.world.entity.Entity
 *  net.minecraft.world.item.Item
 *  net.minecraft.world.item.ItemStack
 *  net.minecraft.world.level.ChunkPos
 *  net.minecraft.world.level.Level
 *  net.minecraft.world.phys.AABB
 *  net.minecraft.world.phys.Vec3
 *  net.neoforged.neoforge.network.PacketDistributor
 *  org.slf4j.Logger
 */
package org.millenaire.village;

import com.mojang.logging.LogUtils;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ThreadLocalRandom;
import java.util.function.Function;
import javax.annotation.Nullable;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Position;
import net.minecraft.core.RegistryAccess;
import net.minecraft.core.SectionPos;
import net.minecraft.core.Vec3i;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.TagKey;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.network.PacketDistributor;
import org.millenaire.TickConstants;
import org.millenaire.advancement.MillAdvancements;
import org.millenaire.building.BuildingId;
import org.millenaire.building.BuildingInstance;
import org.millenaire.building.BuildingInventory;
import org.millenaire.building.BuildingPlan;
import org.millenaire.building.BuildingPlanSet;
import org.millenaire.building.ConstructionTask;
import org.millenaire.combat.raid.RaidManager;
import org.millenaire.config.MillenaireServerConfig;
import org.millenaire.culture.Gender;
import org.millenaire.culture.ModCultures;
import org.millenaire.culture.VillageType;
import org.millenaire.culture.VillagerType;
import org.millenaire.entity.MillVillager;
import org.millenaire.goal.NavigationHelperUtils;
import org.millenaire.hire.HiringHelper;
import org.millenaire.item.ModItems;
import org.millenaire.network.FireplacePositionsPayload;
import org.millenaire.quest.MarvelManager;
import org.millenaire.village.BrickColourTheme;
import org.millenaire.village.GoodsRestockHelper;
import org.millenaire.village.HuntUnreachableMemory;
import org.millenaire.village.LocalMerchantHelper;
import org.millenaire.village.MarketManager;
import org.millenaire.village.PlacementSignHelper;
import org.millenaire.village.PlayerCultureReputation;
import org.millenaire.village.PlayerDiscoveryHelper;
import org.millenaire.village.ResidentSlotManager;
import org.millenaire.village.VillageBannerService;
import org.millenaire.village.VillageDiplomacyHelper;
import org.millenaire.village.VillageEnvironmentHelper;
import org.millenaire.village.VillageEvent;
import org.millenaire.village.VillageEventType;
import org.millenaire.village.VillageGrowthManager;
import org.millenaire.village.VillageHistoryEntry;
import org.millenaire.village.VillageId;
import org.millenaire.village.VillageIntegrityChecker;
import org.millenaire.village.VillagePanelHelper;
import org.millenaire.village.VillageRaidState;
import org.millenaire.village.VillageReputation;
import org.millenaire.village.VillageSavedData;
import org.millenaire.village.VillageSellerDispatcher;
import org.millenaire.village.VillageWaypointGraph;
import org.millenaire.village.VillagerRecord;
import org.millenaire.village.VisitorManager;
import org.millenaire.village.WallGrowthManager;
import org.millenaire.village.WaypointRebuildThrottle;
import org.millenaire.village.path.VillagePathManager;
import org.millenaire.world.PlacedLocation;
import org.slf4j.Logger;

public class Village {
    private static final Logger LOGGER = LogUtils.getLogger();
    private static final int INTEGRITY_CHECK_INTERVAL = 600;
    private static final int RESERVATION_EXPIRY_TICKS = 200;
    private static final long WAYPOINT_REBUILD_COALESCE_INTERVAL = 60L;
    private static final int PANEL_UPDATE_INTERVAL = 40;
    private final VillageId id;
    private final ResourceLocation cultureId;
    private final ResourceLocation villageTypeId;
    private final BlockPos center;
    private String villageName;
    private final VillageReputation reputation = new VillageReputation();
    private final Map<VillageId, Integer> relations = new ConcurrentHashMap<VillageId, Integer>();
    private final ResidentSlotManager residentSlotManager = new ResidentSlotManager(this);
    private final VillageWaypointGraph waypointGraph = new VillageWaypointGraph();
    private final VillagePathManager pathManager = new VillagePathManager();
    private final List<BuildingInstance> buildings = new ArrayList<BuildingInstance>();
    private final Map<UUID, VillagerRecord> villagerRecords = new LinkedHashMap<UUID, VillagerRecord>();
    private int cachedDefenderCount = -1;
    private final Map<UUID, Integer> missingCounts = new HashMap<UUID, Integer>();
    private int integrityTickCounter;
    private int growthTickCounter;
    private long noProjectsLeftUntil;
    private final Map<ResourceLocation, Long> placementCooldowns = new HashMap<ResourceLocation, Long>();
    @Nullable
    private PendingProject pendingProject;
    private final Set<ResourceLocation> buildingsBought = new HashSet<ResourceLocation>();
    @Nullable
    private BrickColourTheme brickTheme;
    private static final long DUSK_TIME = 13000L;
    private long lastNightActionDay = -1L;
    @Nullable
    private VillageId parentVillageId;
    @Nullable
    private UUID ownerUUID;
    @Nullable
    private String ownerName;
    @Nullable
    private String bannerNbt;
    private static final int MAX_CHRONICLE_SIZE = 500;
    private static final int MAX_HISTORY_SIZE = 1000;
    private final List<VillageEvent> chronicle = new ArrayList<VillageEvent>();
    private final List<VillageHistoryEntry> history = new ArrayList<VillageHistoryEntry>();
    private long historyStartTick = -1L;
    private int panelTickCounter;
    private long lastGoodsRefresh;
    private final transient WaypointRebuildThrottle waypointRebuildThrottle = new WaypointRebuildThrottle();
    private boolean restockNightActionDone;
    private final VillageRaidState raidState = new VillageRaidState(this::markDirty);
    private boolean allBedManagersInitialized;
    @Nullable
    private transient AABB dangerousMobsArea;
    @Nullable
    private transient Map<String, List<BuildingInstance>> buildingsByTag;
    private static final long RESTOCK_INTERVAL_LOCKED = 20L;
    private static final long RESTOCK_INTERVAL_UNLOCKED = 100L;
    private final transient Map<BlockPos, MillVillager> activeSellers = new HashMap<BlockPos, MillVillager>();
    @Nullable
    private transient Map<Item, Integer> importsNeededCache;
    private transient long importsNeededCacheExpiry;
    @Nullable
    private transient Map<String, Integer> villageStockCache;
    private transient long villageStockCacheTick = -1L;
    private static final long VILLAGE_STOCK_CACHE_TTL_TICKS = 60L;
    private final transient HuntUnreachableMemory huntUnreachable = new HuntUnreachableMemory();
    @Nullable
    private MarvelManager marvelManager;
    private boolean chestLocked = false;
    private boolean dirty;
    static final int CHUNK_MARGIN = 1;
    private boolean active = false;
    private boolean chunksForceLoaded = false;
    private boolean forceActive = false;
    private Set<ChunkPos> loadedChunks = Set.of();
    private boolean chunksNeedRefresh = false;
    private boolean needsOrphanCleanup = false;
    @Nullable
    private String rawBrickThemeName;
    private transient Map<UUID, ResourceLocation> villagerTypesCache;

    static int getKeepActiveRadius() {
        return MillenaireServerConfig.SERVER.keepActiveRadius.getAsInt();
    }

    static int getUnloadRadius() {
        return MillenaireServerConfig.SERVER.keepActiveRadius.getAsInt() + 32;
    }

    public Village(VillageId id, ResourceLocation cultureId, ResourceLocation villageTypeId, BlockPos center) {
        this.id = id;
        this.cultureId = cultureId;
        this.villageTypeId = villageTypeId;
        this.center = center;
    }

    @Nullable
    public static Village resolve(ServerLevel level, VillageId villageId) {
        if (level == null || villageId == null) {
            return null;
        }
        return VillageSavedData.get(level).getVillageManager().getVillage(villageId);
    }

    public VillageId getId() {
        return this.id;
    }

    public String getVillageName() {
        return this.villageName;
    }

    public void setVillageName(String villageName) {
        this.villageName = villageName;
    }

    public ResourceLocation getCultureId() {
        return this.cultureId;
    }

    public int getRelation(VillageId otherId) {
        return this.relations.getOrDefault(otherId, 0);
    }

    public void setRelation(VillageId otherId, int value) {
        int clamped = Math.max(-100, Math.min(100, value));
        this.relations.put(otherId, clamped);
        this.markDirty();
    }

    public void adjustRelation(VillageId otherId, int delta, boolean reset) {
        if (reset) {
            this.setRelation(otherId, delta);
        } else {
            int current = this.getRelation(otherId);
            this.setRelation(otherId, current + delta);
        }
    }

    public void adjustRelationSymmetric(ServerLevel level, VillageId targetId, int change, boolean reset) {
        int currentValue = this.getRelation(targetId);
        int newValue = reset ? change : currentValue + change;
        newValue = Math.max(-100, Math.min(100, newValue));
        this.relations.put(targetId, newValue);
        this.markDirty();
        Village other = Village.resolve(level, targetId);
        if (other != null && !other.getId().equals(this.id)) {
            other.relations.put(this.id, newValue);
            other.markDirty();
        }
    }

    public void removeRelation(VillageId otherId) {
        if (this.relations.remove(otherId) != null) {
            this.markDirty();
        }
    }

    public Map<VillageId, Integer> getRelations() {
        return Collections.unmodifiableMap(this.relations);
    }

    void setRelationsFromNbt(Map<VillageId, Integer> loaded) {
        this.relations.clear();
        this.relations.putAll(loaded);
    }

    public ResourceLocation getVillageTypeId() {
        return this.villageTypeId;
    }

    public BlockPos getCenter() {
        return this.center;
    }

    public VillageReputation getReputation() {
        return this.reputation;
    }

    public int getCombinedReputation(ServerLevel level, UUID playerId) {
        int villageRep = this.reputation.get(playerId);
        int cultureRep = PlayerCultureReputation.get(level).get(playerId, this.cultureId);
        return villageRep + cultureRep;
    }

    public boolean isControlledBy(UUID playerId) {
        return this.ownerUUID != null && this.ownerUUID.equals(playerId);
    }

    public boolean isLoneBuilding() {
        VillageType vType = ModCultures.getVillageType(this.villageTypeId);
        return vType != null && vType.loneBuilding();
    }

    public boolean areChestsLocked() {
        return this.chestLocked;
    }

    public void setChestLocked(boolean locked) {
        this.chestLocked = locked;
    }

    public void lockAllChestsPublic(ServerLevel level) {
        this.lockAllChests(VillageSavedData.get(level));
    }

    public void unlockAllChestsPublic(ServerLevel level) {
        this.unlockAllChests(level, VillageSavedData.get(level));
    }

    private void lockAllChests(VillageSavedData savedData) {
        this.chestLocked = true;
        savedData.setDirty();
        LOGGER.info("[Millenaire] Chests LOCKED for village {} ({} buildings)", (Object)(this.villageName != null ? this.villageName : this.villageTypeId.getPath()), (Object)this.buildings.size());
    }

    private void unlockAllChests(ServerLevel level, VillageSavedData savedData) {
        this.chestLocked = false;
        savedData.setDirty();
        String name = this.villageName != null ? this.villageName : this.villageTypeId.getPath();
        LOGGER.info("[Millenaire] Chests UNLOCKED for village {} ({} buildings) \u2014 negation wand placed", (Object)name, (Object)this.buildings.size());
        MutableComponent message = Component.translatable((String)"ui.allchestsunlocked", (Object[])new Object[]{name});
        for (ServerPlayer player : level.players()) {
            if (!player.blockPosition().closerToCenterThan((Position)Vec3.atCenterOf((Vec3i)this.center), 64.0)) continue;
            player.sendSystemMessage((Component)message);
        }
        this.placeNegationWandInChests(level);
    }

    private void placeNegationWandInChests(ServerLevel level) {
        Item wand = (Item)ModItems.NEGATION_WAND.get();
        BuildingInstance townhall = this.getTownhall();
        if (townhall != null && townhall.getInventory() != null && !this.isMarketBuilding(townhall)) {
            if (townhall.getInventory().getCount((Level)level, wand) == 0) {
                if (townhall.getInventory().add((Level)level, wand, 1) > 0) {
                    return;
                }
            } else {
                return;
            }
        }
        for (BuildingInstance building : this.buildings) {
            if (this.isMarketBuilding(building) || building.getInventory() == null || building.getInventory().add((Level)level, wand, 1) <= 0) continue;
            return;
        }
        LOGGER.warn("[Mill\u00e9naire] Could not place negation wand \u2014 all inventories full for village {}", (Object)this.villageName);
    }

    private boolean isMarketBuilding(BuildingInstance building) {
        BuildingPlanSet planSet = ModCultures.getBuildingPlanSet(building.getPlanSetId());
        return planSet != null && planSet.isMarket();
    }

    private void checkBattleStatus(ServerLevel level, VillageSavedData savedData) {
        int nbLiveDefenders = this.getDefenderCount();
        if (this.chestLocked && nbLiveDefenders == 0) {
            this.unlockAllChests(level, savedData);
        } else if (!this.chestLocked && nbLiveDefenders > 0) {
            this.lockAllChests(savedData);
        }
    }

    public void reevaluateChestLock(ServerLevel level) {
        this.checkBattleStatus(level, VillageSavedData.get(level));
    }

    int getDefenderCount() {
        if (this.cachedDefenderCount < 0) {
            int count = 0;
            for (VillagerRecord record : this.villagerRecords.values()) {
                VillagerType type;
                ResourceLocation typeId;
                if (record.isKilled() || record.isRaidingVillage() || record.isAwayRaiding() || record.isAwayHired() || (typeId = record.getVillagerTypeId()) == null || (type = ModCultures.getVillagerType(typeId)) == null || !type.hasTag("helpInAttacks")) continue;
                ++count;
            }
            this.cachedDefenderCount = count;
        }
        return this.cachedDefenderCount;
    }

    private void invalidateDefenderCount() {
        this.cachedDefenderCount = -1;
    }

    public void invalidateDefenderCountAndReevaluate(ServerLevel level) {
        this.invalidateDefenderCount();
        this.reevaluateChestLock(level);
    }

    public int getVillageRaidingStrength() {
        int total = 0;
        for (VillagerRecord record : this.villagerRecords.values()) {
            VillagerType type;
            ResourceLocation typeId;
            if (record.isKilled() || record.isRaidingVillage() || (typeId = record.getVillagerTypeId()) == null || (type = ModCultures.getVillagerType(typeId)) == null || !type.isRaider()) continue;
            total += record.getMilitaryStrength();
        }
        return total;
    }

    public int getVillageDefendingStrength() {
        int total = 0;
        for (VillagerRecord record : this.villagerRecords.values()) {
            VillagerType type;
            ResourceLocation typeId;
            if (record.isKilled() || record.isRaidingVillage() || (typeId = record.getVillagerTypeId()) == null || (type = ModCultures.getVillagerType(typeId)) == null || !type.isHelpInAttacks()) continue;
            total += record.getMilitaryStrength();
        }
        return total;
    }

    public int getVillageAttackerStrength() {
        int total = 0;
        for (VillagerRecord record : this.villagerRecords.values()) {
            if (record.isKilled() || !record.isRaidingVillage()) continue;
            total += record.getMilitaryStrength();
        }
        return total;
    }

    @Nullable
    public VillageId getRaidTarget() {
        return this.raidState.getRaidTarget();
    }

    public void setRaidTarget(@Nullable VillageId target) {
        this.raidState.setRaidTarget(target);
    }

    public long getRaidPlanningStart() {
        return this.raidState.getRaidPlanningStart();
    }

    public void setRaidPlanningStart(long t) {
        this.raidState.setRaidPlanningStart(t);
    }

    public long getRaidStart() {
        return this.raidState.getRaidStart();
    }

    public void setRaidStart(long t) {
        this.raidState.setRaidStart(t);
    }

    public boolean isUnderAttack() {
        return this.raidState.isUnderAttack();
    }

    public void setUnderAttack(boolean v) {
        this.raidState.setUnderAttack(v);
    }

    public List<String> getRaidsPerformed() {
        return this.raidState.getRaidsPerformed();
    }

    public void addRaidsPerformed(String entry) {
        this.raidState.addRaidsPerformed(entry);
    }

    public List<String> getRaidsSuffered() {
        return this.raidState.getRaidsSuffered();
    }

    public void addRaidsSuffered(String entry) {
        this.raidState.addRaidsSuffered(entry);
    }

    public void loadRaidsPerformed(List<String> entries) {
        this.raidState.loadRaidsPerformed(entries);
    }

    public void loadRaidsSuffered(List<String> entries) {
        this.raidState.loadRaidsSuffered(entries);
    }

    public void clearRaid() {
        this.raidState.clearRaid();
    }

    public VillagePathManager getPathManager() {
        return this.pathManager;
    }

    public VillageWaypointGraph getWaypointGraph() {
        return this.waypointGraph;
    }

    public long getLastGoodsRefresh() {
        return this.lastGoodsRefresh;
    }

    public void setLastGoodsRefresh(long lastGoodsRefresh) {
        this.lastGoodsRefresh = lastGoodsRefresh;
    }

    public long getNoProjectsLeftUntil() {
        return this.noProjectsLeftUntil;
    }

    public void setNoProjectsLeftUntil(long tick) {
        this.noProjectsLeftUntil = tick;
    }

    public Map<ResourceLocation, Long> getPlacementCooldowns() {
        return Collections.unmodifiableMap(this.placementCooldowns);
    }

    public void addPlacementCooldown(ResourceLocation planSetId, long untilTick) {
        this.placementCooldowns.put(planSetId, untilTick);
    }

    @Nullable
    public PendingProject getPendingProject() {
        return this.pendingProject;
    }

    public void setPendingProject(@Nullable PendingProject project) {
        this.pendingProject = project;
        this.dirty = true;
    }

    public void addBoughtBuilding(ResourceLocation planSetId) {
        if (this.buildingsBought.add(planSetId)) {
            this.markDirty();
        }
    }

    public boolean isBuildingBought(ResourceLocation planSetId) {
        return this.buildingsBought.contains(planSetId);
    }

    public Set<ResourceLocation> getBuildingsBought() {
        return Collections.unmodifiableSet(this.buildingsBought);
    }

    public void loadBuildingsBought(Set<ResourceLocation> bought) {
        this.buildingsBought.clear();
        this.buildingsBought.addAll(bought);
    }

    @Nullable
    public String getBannerNbt() {
        return this.bannerNbt;
    }

    public void setBannerNbt(@Nullable String nbt) {
        this.bannerNbt = nbt;
        this.markDirty();
    }

    public void loadBannerNbt(@Nullable String nbt) {
        this.bannerNbt = nbt;
    }

    public ItemStack getBannerStack(RegistryAccess registryAccess) {
        if (this.bannerNbt == null) {
            return ItemStack.EMPTY;
        }
        return VillageBannerService.parseLegacyBanner(this.bannerNbt, registryAccess);
    }

    @Nullable
    public BrickColourTheme getBrickTheme() {
        return this.brickTheme;
    }

    public void setBrickTheme(@Nullable BrickColourTheme theme) {
        this.brickTheme = theme;
        this.markDirty();
    }

    @Nullable
    public String getBrickThemeName() {
        return this.brickTheme != null ? this.brickTheme.name() : this.rawBrickThemeName;
    }

    public void setRawBrickThemeName(@Nullable String name) {
        this.rawBrickThemeName = name;
    }

    @Nullable
    public String getRawBrickThemeName() {
        return this.rawBrickThemeName;
    }

    public void resolveBrickTheme(VillageType villageType) {
        if (this.rawBrickThemeName != null && this.brickTheme == null) {
            for (BrickColourTheme theme : villageType.brickColourThemes()) {
                if (!theme.name().equals(this.rawBrickThemeName)) continue;
                this.brickTheme = theme;
                break;
            }
            if (this.brickTheme == null) {
                LogUtils.getLogger().warn("Brick theme '{}' not found for village {}", (Object)this.rawBrickThemeName, (Object)this.villageName);
            }
        }
    }

    public List<BuildingInstance> getBuildings() {
        return Collections.unmodifiableList(this.buildings);
    }

    private void autoSpawnResidents(ServerLevel level) {
        for (BuildingInstance b : this.buildings) {
            BuildingPlanSet planSet;
            if (!b.isOperational() || (planSet = ModCultures.getBuildingPlanSet(b.getPlanSetId())) == null || !planSet.tags().contains("autospawnvillagers") || planSet.maleResidents().isEmpty() && planSet.femaleResidents().isEmpty() || this.hasAnyResidentRecord(b.getId())) continue;
            VillageGrowthManager.spawnBuildingOccupants(level, this, planSet, b);
            this.recordEvent(level, "Auto-spawned residents for " + planSet.id().getPath());
            this.markDirty();
        }
    }

    private boolean hasAnyResidentRecord(BuildingId buildingId) {
        for (VillagerRecord rec : this.villagerRecords.values()) {
            if (!buildingId.equals(rec.getHomeBuilding())) continue;
            return true;
        }
        return false;
    }

    public int getVillageIrrigation() {
        int irrigation = 0;
        for (BuildingInstance b : this.buildings) {
            BuildingPlanSet.LevelDef levelDef;
            BuildingPlanSet planSet;
            if (!b.isOperational() || (planSet = ModCultures.getBuildingPlanSet(b.getPlanSetId())) == null || (levelDef = planSet.getLevel(b.getVariant(), b.getLevel())) == null) continue;
            irrigation += levelDef.irrigation();
        }
        return irrigation;
    }

    @Nullable
    public BuildingInstance findBuildingById(BuildingId buildingId) {
        for (BuildingInstance b : this.buildings) {
            if (!b.getId().equals(buildingId)) continue;
            return b;
        }
        return null;
    }

    @Nullable
    public BuildingInstance getBuildingAt(BlockPos pos) {
        for (BuildingInstance b : this.buildings) {
            if (!b.containsPos(pos)) continue;
            return b;
        }
        return null;
    }

    @Nullable
    public BuildingInstance getOperationalBuildingAt(BlockPos pos) {
        for (BuildingInstance b : this.buildings) {
            if (!b.isOperational() || !b.containsPos(pos)) continue;
            return b;
        }
        return null;
    }

    public List<BuildingInstance> getBuildingsWithTag(String tag) {
        if (this.buildingsByTag == null) {
            this.rebuildBuildingTagCache();
        }
        return this.buildingsByTag.getOrDefault(tag, List.of());
    }

    public List<BuildingInstance> getOperationalBuildingsWithTag(String tag) {
        List<BuildingInstance> all = this.getBuildingsWithTag(tag);
        if (all.isEmpty()) {
            return List.of();
        }
        ArrayList<BuildingInstance> result = new ArrayList<BuildingInstance>(all.size());
        for (BuildingInstance b : all) {
            if (!b.isOperational()) continue;
            result.add(b);
        }
        return result;
    }

    public void invalidateBuildingTagCache() {
        this.buildingsByTag = null;
    }

    private void rebuildBuildingTagCache() {
        HashMap<String, List<BuildingInstance>> cache = new HashMap<String, List<BuildingInstance>>();
        for (BuildingInstance building : this.buildings) {
            HashSet<String> allTags = new HashSet<String>();
            BuildingPlan plan = ModCultures.getBuildingPlan(building.getPlanId());
            if (plan != null) {
                allTags.addAll(plan.tags());
            }
            allTags.addAll(building.getRuntimeTags());
            for (String tag2 : allTags) {
                cache.computeIfAbsent(tag2, k -> new ArrayList()).add(building);
            }
        }
        cache.replaceAll((tag, list) -> List.copyOf(list));
        this.buildingsByTag = cache;
    }

    public Set<UUID> getVillagerUuids() {
        return Collections.unmodifiableSet(this.villagerRecords.keySet());
    }

    public Map<UUID, ResourceLocation> getVillagerTypes() {
        if (this.villagerTypesCache == null) {
            LinkedHashMap<UUID, ResourceLocation> result = new LinkedHashMap<UUID, ResourceLocation>();
            for (Map.Entry<UUID, VillagerRecord> entry : this.villagerRecords.entrySet()) {
                result.put(entry.getKey(), entry.getValue().getVillagerTypeId());
            }
            this.villagerTypesCache = Collections.unmodifiableMap(result);
        }
        return this.villagerTypesCache;
    }

    public Map<UUID, VillagerRecord> getVillagerRecords() {
        return Collections.unmodifiableMap(this.villagerRecords);
    }

    public int getMissingCount(UUID uuid) {
        return this.missingCounts.getOrDefault(uuid, 0);
    }

    public void putMissingCount(UUID uuid, int count) {
        this.missingCounts.put(uuid, count);
    }

    public void removeMissingCount(UUID uuid) {
        this.missingCounts.remove(uuid);
    }

    public Map<BlockPos, MillVillager> getActiveSellers() {
        return this.activeSellers;
    }

    public void markHuntUnreachable(BlockPos p, long now) {
        this.huntUnreachable.mark(p.getX(), p.getY(), p.getZ(), now);
    }

    public boolean isHuntUnreachable(BlockPos p, long now) {
        return this.huntUnreachable.contains(p.getX(), p.getY(), p.getZ(), now);
    }

    public void markDirty() {
        this.dirty = true;
    }

    public void removeVillagerRecord(UUID uuid) {
        this.villagerRecords.remove(uuid);
        this.villagerTypesCache = null;
        this.invalidateDefenderCount();
    }

    public boolean forgetBuilding(ServerLevel level, BuildingInstance building) {
        BuildingPlan plan;
        if (building == null) {
            return false;
        }
        BuildingInstance townhall = this.getTownhall();
        if (townhall != null && townhall.getId().equals(building.getId())) {
            LOGGER.warn("[Millenaire] forgetBuilding refused: building {} is the town hall of village {}", (Object)building.getId().uuid(), (Object)(this.villageName != null ? this.villageName : this.villageTypeId));
            return false;
        }
        ConstructionTask task = building.getConstructionTask();
        if (task != null) {
            task.releaseReservation();
            building.setConstructionTask(null);
        }
        if (level != null && building.isBeingBuilt() && building.getPlanId() != null && (plan = ModCultures.getBuildingPlan(building.getPlanId())) != null) {
            PlacementSignHelper.removeCornerSigns(level, plan, new PlacedLocation(building.getOrigin(), building.getRotation()));
        }
        BuildingId buildingId = building.getId();
        ArrayList<UUID> residents = new ArrayList<UUID>();
        for (Map.Entry<UUID, VillagerRecord> entry : this.villagerRecords.entrySet()) {
            BuildingId home = entry.getValue().getHomeBuilding();
            if (home == null || !home.equals(buildingId)) continue;
            residents.add(entry.getKey());
        }
        for (UUID residentId : residents) {
            Entity entity;
            Entity entity2 = entity = level != null ? level.getEntity(residentId) : null;
            if (entity instanceof MillVillager) {
                MillVillager villager = (MillVillager)entity;
                villager.discard();
            }
            this.removeVillagerRecord(residentId);
        }
        this.buildings.remove(building);
        this.invalidateBuildingTagCache();
        this.chunksNeedRefresh = true;
        this.markWaypointGraphDirty();
        this.markDirty();
        LOGGER.info("[Millenaire] Forgot building {} ({}) in village {} \u2014 {} resident(s) removed", new Object[]{buildingId.uuid(), building.getPlanId(), this.villageName != null ? this.villageName : this.villageTypeId, residents.size()});
        return true;
    }

    public boolean markVillagerKilled(UUID uuid) {
        VillagerRecord record = this.villagerRecords.get(uuid);
        if (record == null) {
            return false;
        }
        record.setKilled(true);
        this.invalidateDefenderCount();
        return true;
    }

    public boolean isDirty() {
        return this.dirty;
    }

    public boolean consumeDirty() {
        boolean wasDirty = this.dirty;
        this.dirty = false;
        return wasDirty;
    }

    public void addBuilding(BuildingInstance building) {
        this.buildings.add(building);
        this.chunksNeedRefresh = true;
        this.allBedManagersInitialized = false;
        this.buildingsByTag = null;
        this.dirty = true;
    }

    public void addVillager(UUID uuid, ResourceLocation villagerTypeId) {
        this.villagerRecords.put(uuid, new VillagerRecord(uuid, villagerTypeId, null));
        this.villagerTypesCache = null;
        this.invalidateDefenderCount();
        this.dirty = true;
    }

    public void addVillager(UUID uuid, ResourceLocation villagerTypeId, @Nullable BuildingId homeBuilding) {
        this.villagerRecords.put(uuid, new VillagerRecord(uuid, villagerTypeId, homeBuilding));
        this.villagerTypesCache = null;
        this.invalidateDefenderCount();
        this.dirty = true;
    }

    public void addVillager(VillagerRecord record) {
        this.villagerRecords.put(record.getUuid(), record);
        this.villagerTypesCache = null;
        this.invalidateDefenderCount();
        this.dirty = true;
    }

    public void transferVillagerPermanently(ServerLevel level, UUID villagerId, Village destination, BuildingId destBuilding) {
        VillagerRecord record = this.villagerRecords.get(villagerId);
        if (record == null) {
            LOGGER.warn("[Millenaire] transferVillagerPermanently: no record for {}", (Object)villagerId);
            return;
        }
        this.removeVillagerRecord(villagerId);
        record.setHomeBuilding(destBuilding);
        destination.addVillager(record);
        Entity entity = level.getEntity(villagerId);
        if (entity instanceof MillVillager) {
            MillVillager villager = (MillVillager)entity;
            villager.setVillageId(destination.getId());
            villager.setHomeBuilding(destBuilding);
            if (destination.isActive()) {
                BlockPos sleepPos;
                BuildingInstance destBldg = destination.getBuilding(destBuilding);
                BlockPos teleportPos = destBldg != null ? ((sleepPos = destBldg.getFirstPointPos("sleepingPos")) != null ? sleepPos : destBldg.getOrigin()) : destination.getCenter();
                NavigationHelperUtils.teleportToSafe(villager, teleportPos);
            } else {
                villager.discard();
            }
        }
        this.markDirty();
        destination.markDirty();
        LOGGER.debug("[Millenaire] Transferred villager {} from {} to {}", new Object[]{villagerId.toString().substring(0, 8), this.villageName != null ? this.villageName : this.villageTypeId, destination.villageName != null ? destination.villageName : destination.villageTypeId});
    }

    @Nullable
    public VillagerRecord getVillagerRecord(UUID uuid) {
        return this.villagerRecords.get(uuid);
    }

    public void setVillagerHired(ServerLevel level, UUID villagerId, @Nullable UUID owner, long until) {
        VillagerRecord rec = this.getVillagerRecord(villagerId);
        if (rec == null) {
            return;
        }
        rec.setHireState(owner, until);
        Entity entity = level.getEntity(villagerId);
        if (entity instanceof MillVillager) {
            MillVillager entity2 = (MillVillager)entity;
            entity2.setHireState(owner, until);
        }
        this.invalidateDefenderCount();
        this.markDirty();
    }

    @Nullable
    public BuildingId getVillagerHome(UUID uuid) {
        VillagerRecord record = this.villagerRecords.get(uuid);
        return record != null ? record.getHomeBuilding() : null;
    }

    public void setVillagerHome(UUID uuid, @Nullable BuildingId homeBuilding) {
        VillagerRecord record = this.villagerRecords.get(uuid);
        if (record != null) {
            record.setHomeBuilding(homeBuilding);
        }
        this.dirty = true;
    }

    public int addReputation(UUID playerId, int amount) {
        int newValue = this.reputation.add(playerId, amount);
        this.dirty = true;
        return newValue;
    }

    public int adjustReputation(ServerLevel level, UUID playerId, int amount) {
        ServerPlayer player;
        int newValue = this.addReputation(playerId, amount);
        int cultureAmount = amount / 10;
        int remainder = Math.abs(amount % 10);
        if (remainder != 0 && ThreadLocalRandom.current().nextInt(10) < remainder) {
            cultureAmount += amount > 0 ? 1 : -1;
        }
        if (cultureAmount != 0) {
            PlayerCultureReputation.get(level).add(playerId, this.cultureId, cultureAmount);
        }
        if ((player = level.getServer().getPlayerList().getPlayer(playerId)) != null) {
            String cultureKey;
            ResourceLocation repAdv;
            if (newValue > 8192) {
                MillAdvancements.grant(player, MillAdvancements.FRIEND_INDEED);
            }
            if (newValue > 32768 && (repAdv = MillAdvancements.REP.get(cultureKey = this.cultureId.getPath().toLowerCase(Locale.ROOT))) != null) {
                MillAdvancements.grant(player, repAdv);
            }
            PlayerCultureReputation.get(level).checkPendingAttila(player);
        }
        return newValue;
    }

    public void recordEvent(ServerLevel level, String message) {
        long tick = level.getServer().getTickCount();
        if (this.historyStartTick < 0L) {
            this.historyStartTick = tick;
        }
        if (this.history.size() >= 1000) {
            this.history.subList(0, 100).clear();
        }
        this.history.add(new VillageHistoryEntry(tick, message));
    }

    public List<VillageHistoryEntry> getHistory() {
        return Collections.unmodifiableList(this.history);
    }

    public long getHistoryStartTick() {
        return this.historyStartTick;
    }

    public void clearHistory() {
        this.history.clear();
        this.historyStartTick = -1L;
    }

    public void recordChronicleEvent(ServerLevel level, VillageEventType type, String param1, @Nullable String param2) {
        long gameTime = level.getGameTime();
        this.addChronicleEventDirect(new VillageEvent(gameTime, type, param1, param2));
        VillageSavedData.get(level).setDirty();
    }

    public void addChronicleEventDirect(VillageEvent event) {
        this.chronicle.add(event);
        if (this.chronicle.size() > 500) {
            this.chronicle.subList(0, this.chronicle.size() - 500).clear();
        }
    }

    public List<VillageEvent> getChronicle() {
        return Collections.unmodifiableList(this.chronicle);
    }

    @Nullable
    public BuildingInstance getBuilding(BuildingId buildingId) {
        for (BuildingInstance b : this.buildings) {
            if (!b.getId().equals(buildingId)) continue;
            return b;
        }
        return null;
    }

    @Nullable
    public BuildingInstance getTownhall() {
        if (this.buildings.isEmpty()) {
            return null;
        }
        for (BuildingInstance b : this.buildings) {
            BuildingPlanSet planSet;
            ResourceLocation planSetId = b.getPlanSetId();
            if (planSetId == null || (planSet = ModCultures.getBuildingPlanSet(planSetId)) == null || !planSet.isTownHall()) continue;
            return b;
        }
        return this.buildings.get(0);
    }

    public AABB computeBounds() {
        if (this.buildings.isEmpty()) {
            return new AABB((double)(this.center.getX() - 8), (double)(this.center.getY() - 8), (double)(this.center.getZ() - 8), (double)(this.center.getX() + 8), (double)(this.center.getY() + 8), (double)(this.center.getZ() + 8));
        }
        double minX = Double.MAX_VALUE;
        double minY = Double.MAX_VALUE;
        double minZ = Double.MAX_VALUE;
        double maxX = -1.7976931348623157E308;
        double maxY = -1.7976931348623157E308;
        double maxZ = -1.7976931348623157E308;
        for (BuildingInstance b : this.buildings) {
            BlockPos o = b.getOrigin();
            int w = b.getEffectiveWidth();
            int d = b.getEffectiveDepth();
            minX = Math.min(minX, (double)o.getX());
            minY = Math.min(minY, (double)o.getY());
            minZ = Math.min(minZ, (double)o.getZ());
            maxX = Math.max(maxX, w > 0 ? (double)(o.getX() + w - 1) : (double)o.getX());
            maxY = Math.max(maxY, (double)o.getY());
            maxZ = Math.max(maxZ, d > 0 ? (double)(o.getZ() + d - 1) : (double)o.getZ());
        }
        return new AABB(minX - 8.0, minY - 8.0, minZ - 8.0, maxX + 8.0, maxY + 8.0, maxZ + 8.0);
    }

    public boolean isActive() {
        return this.active;
    }

    public void setActive(boolean active) {
        if (active && !this.active) {
            this.needsOrphanCleanup = true;
        }
        this.active = active;
    }

    public boolean isChunksForceLoaded() {
        return this.chunksForceLoaded;
    }

    public void setChunksForceLoaded(boolean chunksForceLoaded) {
        this.chunksForceLoaded = chunksForceLoaded;
    }

    public boolean isForceActive() {
        return this.forceActive;
    }

    public void setForceActive(boolean forceActive) {
        this.forceActive = forceActive;
    }

    public Set<ChunkPos> getLoadedChunks() {
        return this.loadedChunks;
    }

    public void setLoadedChunks(Set<ChunkPos> loadedChunks) {
        this.loadedChunks = loadedChunks;
    }

    public boolean isChunksNeedRefresh() {
        return this.chunksNeedRefresh;
    }

    public void setChunksNeedRefresh(boolean chunksNeedRefresh) {
        this.chunksNeedRefresh = chunksNeedRefresh;
    }

    public Set<ChunkPos> computeVillageChunks() {
        AABB bounds = this.computeBounds();
        int minCX = SectionPos.blockToSectionCoord((int)((int)bounds.minX)) - 1;
        int maxCX = SectionPos.blockToSectionCoord((int)((int)bounds.maxX)) + 1;
        int minCZ = SectionPos.blockToSectionCoord((int)((int)bounds.minZ)) - 1;
        int maxCZ = SectionPos.blockToSectionCoord((int)((int)bounds.maxZ)) + 1;
        HashSet<ChunkPos> chunks = new HashSet<ChunkPos>();
        for (int cx = minCX; cx <= maxCX; ++cx) {
            for (int cz = minCZ; cz <= maxCZ; ++cz) {
                chunks.add(new ChunkPos(cx, cz));
            }
        }
        return chunks;
    }

    public void syncRecords(ServerLevel level) {
        for (Map.Entry<UUID, VillagerRecord> entry : this.villagerRecords.entrySet()) {
            MillVillager villager;
            Entity entity = level.getEntity(entry.getKey());
            if (!(entity instanceof MillVillager) || !(villager = (MillVillager)entity).isAlive()) continue;
            entry.getValue().updateFromEntity(villager);
        }
        this.dirty = true;
    }

    @Nullable
    public BuildingInstance findUnreservedConstruction() {
        VillageType villageType = ModCultures.getVillageType(this.villageTypeId);
        int maxWallBuilders = villageType != null ? WallGrowthManager.computeMaxSlots(this, villageType) : Integer.MAX_VALUE;
        int activeWallBuilders = WallGrowthManager.countActiveBuilders(this);
        for (BuildingInstance b : this.buildings) {
            ConstructionTask task;
            if (!b.isBeingBuilt() || (task = b.getConstructionTask()) == null || task.isReserved() || task.isBlocked()) continue;
            if (b.isWallSegment()) {
                if (activeWallBuilders >= maxWallBuilders) continue;
                return b;
            }
            return b;
        }
        return null;
    }

    public void rebuildWaypointGraph(ServerLevel level) {
        this.waypointGraph.rebuild(this.buildings, this.center, level, this);
        this.waypointRebuildThrottle.onRebuilt(level.getGameTime());
    }

    public void markWaypointGraphDirty() {
        this.waypointRebuildThrottle.markDirty();
    }

    public boolean rebuildWaypointGraphIfStale(ServerLevel level, long minIntervalTicks) {
        if (!this.waypointRebuildThrottle.tryAcquire(level.getGameTime(), minIntervalTicks)) {
            return false;
        }
        this.rebuildWaypointGraph(level);
        return true;
    }

    public void sendFireplacePositions(ServerLevel level) {
        HashSet<BuildingId> occupiedBuildings = new HashSet<BuildingId>();
        for (VillagerRecord villagerRecord : this.villagerRecords.values()) {
            if (villagerRecord.isKilled() || villagerRecord.getHomeBuilding() == null) continue;
            occupiedBuildings.add(villagerRecord.getHomeBuilding());
        }
        ArrayList<BlockPos> allPositions = new ArrayList<BlockPos>();
        for (BuildingInstance b : this.buildings) {
            if (!b.isOperational() || !occupiedBuildings.contains(b.getId())) continue;
            allPositions.addAll(b.getFireplacePositions());
        }
        FireplacePositionsPayload fireplacePositionsPayload = new FireplacePositionsPayload(this.id.uuid(), this.center, allPositions);
        for (ServerPlayer player : level.players()) {
            if (!(player.blockPosition().distSqr((Vec3i)this.center) < 16384.0)) continue;
            PacketDistributor.sendToPlayer((ServerPlayer)player, (CustomPacketPayload)fireplacePositionsPayload, (CustomPacketPayload[])new CustomPacketPayload[0]);
        }
    }

    public void backgroundTick(ServerLevel level) {
        long currentDay = level.getDayTime() / 24000L;
        long timeOfDay = level.getDayTime() % 24000L;
        if (timeOfDay >= 13000L && currentDay > this.lastNightActionDay) {
            this.lastNightActionDay = currentDay;
            VillageDiplomacyHelper.performNightlyDiplomacyDrift(level, this);
            VillageDiplomacyHelper.regenerateDiplomacyPointsForPlayers(level, this);
            RaidManager.attemptPlanNewRaid(this, level);
            this.markDirty();
        }
        if (this.raidState.isRaidActiveOrSuffering()) {
            RaidManager.tickRaid(this, level);
        }
        if (level.getGameTime() % 100L == 0L) {
            this.tickHireExpiry(level);
        }
    }

    void performNightlyActions(ServerLevel level) {
        VillageDiplomacyHelper.performNightlyDiplomacyDrift(level, this);
        VillageDiplomacyHelper.regenerateDiplomacyPointsForPlayers(level, this);
        RaidManager.attemptPlanNewRaid(this, level);
        if (!this.isLoneBuilding()) {
            LocalMerchantHelper.attemptMerchantMoves(level, this);
        }
        this.pathManager.nightlyRecheck(level, this);
    }

    public void tick(ServerLevel level) {
        if (!this.allBedManagersInitialized) {
            boolean allDone = true;
            for (BuildingInstance b : this.buildings) {
                if (!b.isOperational() || b.isBedManagerInitialized()) continue;
                allDone = false;
                BuildingPlan plan = ModCultures.getBuildingPlan(b.getPlanId());
                if (plan != null) {
                    b.backfillBedPositions(level, plan);
                }
                this.markDirty();
            }
            if (allDone) {
                this.allBedManagersInitialized = true;
            }
        }
        for (BuildingInstance b : this.buildings) {
            ConstructionTask task = b.getConstructionTask();
            if (task == null || !task.isReserved()) continue;
            UUID builderUuid = task.getReservedBuilder();
            if (level.getEntity(builderUuid) == null) {
                task.tickReservation();
                if (!task.isReservationExpired(200)) continue;
                task.releaseReservation();
                this.dirty = true;
                continue;
            }
            task.resetReservationAge();
        }
        if (this.waypointRebuildThrottle.isDirty()) {
            this.rebuildWaypointGraphIfStale(level, 60L);
        }
        if (this.integrityTickCounter == 599) {
            this.cleanupStaleReservations(level);
        }
        ++this.integrityTickCounter;
        if (this.needsOrphanCleanup || this.integrityTickCounter >= 600) {
            boolean wasOrphanCleanup = this.needsOrphanCleanup;
            this.needsOrphanCleanup = false;
            this.integrityTickCounter = 0;
            this.syncRecords(level);
            VillageIntegrityChecker.checkIntegrity(level, this);
            VillageIntegrityChecker.cleanupOrphanedEntities(level, this);
            if (!level.isDay()) {
                this.autoSpawnResidents(level);
            }
        }
        if (this.integrityTickCounter == 300) {
            Set<UUID> knownUuids = this.getVillagerUuids();
            for (BuildingInstance b : this.buildings) {
                if (!b.hasBedManager() || !b.getBedManager().validate(level, knownUuids)) continue;
                this.markDirty();
            }
        }
        ++this.growthTickCounter;
        if (this.growthTickCounter >= 20) {
            this.growthTickCounter = 0;
            VillageGrowthManager.evaluateGrowth(level, this);
            WallGrowthManager.evaluate(level, this);
        }
        if (level.getGameTime() % 20L == 0L) {
            this.despawnDangerousMobs(level);
        }
        if (level.getGameTime() % 20L == 10L) {
            this.checkBattleStatus(level, VillageSavedData.get(level));
        }
        if (this.isPlayerControlled() && level.getGameTime() % 1000L == 0L) {
            this.placeNegationWandInChests(level);
        }
        if (level.getGameTime() % 100L == 0L) {
            this.updatePens(level, false);
        }
        VillageSellerDispatcher.checkSeller(level, this);
        if (level.getGameTime() % 100L == 50L) {
            boolean isDaytime = !TickConstants.isNight((Level)level);
            for (BuildingInstance b : this.buildings) {
                BuildingPlanSet planSet;
                if (!b.isOperational() || (planSet = ModCultures.getBuildingPlanSet(b.getPlanSetId())) == null) continue;
                if (planSet.isMarket()) {
                    MarketManager.updateMarket(level, this, b, isDaytime);
                    continue;
                }
                if (!planSet.hasVisitors()) continue;
                VisitorManager.updateVisitors(level, this, b, isDaytime);
            }
        }
        if (level.getGameTime() % 20L == 0L) {
            VillageEnvironmentHelper.tickGroveSaplings(level, this);
        }
        this.refreshGoods(level);
        if (level.getGameTime() % 1000L == 0L) {
            this.regenerateScrollIfNeeded(level);
        }
        this.pathManager.tick(level, this);
        this.pathManager.startRecalcIfDirty(level, this);
        if (this.marvelManager == null && MarvelManager.isMarvelVillageType(this)) {
            this.marvelManager = new MarvelManager();
        }
        if (this.marvelManager != null) {
            this.marvelManager.tick(this, level);
        }
        long currentDay = level.getDayTime() / 24000L;
        long timeOfDay = level.getDayTime() % 24000L;
        if (timeOfDay >= 13000L && currentDay > this.lastNightActionDay) {
            this.lastNightActionDay = currentDay;
            this.performNightlyActions(level);
            this.markDirty();
        }
        ++this.panelTickCounter;
        if (this.panelTickCounter >= 40) {
            this.panelTickCounter = 0;
            this.updatePanelDisplayLines(level);
        }
        if (level.getGameTime() % 200L == 0L) {
            this.checkExplorationAdvancements(level);
        }
        if (level.getGameTime() % 200L == 0L) {
            this.checkTravelBookDiscoveries(level);
        }
    }

    private void checkExplorationAdvancements(ServerLevel level) {
        PlayerDiscoveryHelper.checkExplorationAdvancements(level, this);
    }

    private void checkTravelBookDiscoveries(ServerLevel level) {
        if (this.dangerousMobsArea == null) {
            return;
        }
        PlayerDiscoveryHelper.checkTravelBookDiscoveries(level, this, this.dangerousMobsArea);
    }

    private void regenerateScrollIfNeeded(ServerLevel level) {
        PlayerDiscoveryHelper.regenerateScrollIfNeeded(level, this);
    }

    public boolean isPlayerControlled() {
        VillageType vType = ModCultures.getVillageType(this.villageTypeId);
        return vType != null && vType.playerControlled();
    }

    @Nullable
    public UUID getOwnerUUID() {
        return this.ownerUUID;
    }

    @Nullable
    public String getOwnerName() {
        return this.ownerName;
    }

    public void setOwner(@Nullable UUID uuid, @Nullable String name) {
        this.ownerUUID = uuid;
        this.ownerName = name;
        this.markDirty();
    }

    public long getLastNightActionDay() {
        return this.lastNightActionDay;
    }

    public void setLastNightActionDay(long day) {
        this.lastNightActionDay = day;
    }

    @Nullable
    public VillageId getParentVillageId() {
        return this.parentVillageId;
    }

    public void setParentVillageId(@Nullable VillageId parentId) {
        this.parentVillageId = parentId;
        this.markDirty();
    }

    @Nullable
    public MarvelManager getMarvelManager() {
        return this.marvelManager;
    }

    public void setMarvelManager(@Nullable MarvelManager manager) {
        this.marvelManager = manager;
    }

    public void updatePens(ServerLevel level, boolean completeRespawn) {
        VillageEnvironmentHelper.updatePens(level, this, completeRespawn);
    }

    private void despawnDangerousMobs(ServerLevel level) {
        if (this.dangerousMobsArea == null) {
            VillageType vType = ModCultures.getVillageType(this.villageTypeId);
            int radius = (vType != null ? vType.radius() : 90) + 20;
            this.dangerousMobsArea = new AABB((double)(this.center.getX() - radius), (double)(this.center.getY() - 20), (double)(this.center.getZ() - radius), (double)(this.center.getX() + radius), (double)(this.center.getY() + 50), (double)(this.center.getZ() + radius));
        }
        VillageEnvironmentHelper.despawnDangerousMobs(level, this.dangerousMobsArea, this);
    }

    private void tickHireExpiry(ServerLevel level) {
        long now = level.getGameTime();
        for (VillagerRecord rec : this.getVillagerRecords().values()) {
            if (!rec.isAwayHired() || !HiringHelper.isExpired(rec.getHiredUntil(), now)) continue;
            this.setVillagerHired(level, rec.getUuid(), null, 0L);
        }
    }

    private void refreshGoods(ServerLevel level) {
        BuildingPlanSet planSet;
        BuildingInstance centre = this.getCentre();
        if (centre == null) {
            return;
        }
        BuildingPlanSet buildingPlanSet = planSet = centre.getPlanSetId() != null ? ModCultures.getBuildingPlanSet(centre.getPlanSetId()) : null;
        if (planSet == null || planSet.startingGoods().isEmpty()) {
            return;
        }
        if (level.isDay()) {
            this.restockNightActionDone = false;
        } else if (!this.restockNightActionDone) {
            long interval = this.areChestsLocked() ? 20L : 100L;
            if (this.areChestsLocked() && this.lastGoodsRefresh + interval * 24000L < level.getGameTime()) {
                this.fillStartingGoods(level, centre, planSet, false);
                this.lastGoodsRefresh = level.getGameTime();
                this.markDirty();
            }
            this.restockNightActionDone = true;
        }
    }

    public void fillStartingGoods(ServerLevel level, BuildingInstance building, BuildingPlanSet planSet, boolean initialSpawn) {
        GoodsRestockHelper.fillStartingGoods(level, building, planSet, initialSpawn);
    }

    @Nullable
    private BuildingInstance getCentre() {
        if (this.buildings.isEmpty()) {
            return null;
        }
        return this.buildings.get(0);
    }

    private void updatePanelDisplayLines(ServerLevel level) {
        VillagePanelHelper.updatePanelDisplayLines(level, this);
    }

    public ResidentSlotManager getResidentSlotManager() {
        return this.residentSlotManager;
    }

    public int countAdultsInBuilding(BuildingId buildingId, Gender gender) {
        return this.residentSlotManager.countAdultsInBuilding(buildingId, gender);
    }

    public List<String> getFreeSlots(BuildingId buildingId, Gender gender) {
        return this.residentSlotManager.getFreeSlots(buildingId, gender);
    }

    public boolean hasFreeSlot(BuildingId buildingId, Gender gender) {
        return this.residentSlotManager.hasFreeSlot(buildingId, gender);
    }

    @Nullable
    public String reserveSlot(BuildingId buildingId, Gender gender, UUID teenagerId) {
        return this.residentSlotManager.reserveSlot(buildingId, gender, teenagerId);
    }

    public void releaseSlot(ResidentSlotManager.SlotKey key) {
        this.residentSlotManager.releaseSlot(key);
    }

    public void releaseAllSlots(UUID teenagerId) {
        this.residentSlotManager.releaseAllSlots(teenagerId);
    }

    public void cleanupStaleReservations(ServerLevel level) {
        this.residentSlotManager.cleanupStaleReservations(level);
    }

    public int countChildren() {
        return this.residentSlotManager.countChildren();
    }

    public int countChildrenInBuilding(BuildingId buildingId) {
        return this.residentSlotManager.countChildrenInBuilding(buildingId);
    }

    public boolean hasAnyFreeSlot(Gender gender) {
        return this.residentSlotManager.hasAnyFreeSlot(gender);
    }

    public boolean hasRelativeOfOppositeGenderInRecords(BuildingId buildingId, Gender teenagerGender, String familyName) {
        if (familyName == null || familyName.isEmpty()) {
            return false;
        }
        Gender oppositeGender = teenagerGender == Gender.MALE ? Gender.FEMALE : Gender.MALE;
        for (VillagerRecord record : this.villagerRecords.values()) {
            VillagerType vType;
            if (record.getHomeBuilding() == null || !record.getHomeBuilding().equals(buildingId) || (vType = ModCultures.getVillagerType(record.getVillagerTypeId())) == null || vType.isChild() || vType.gender() != oppositeGender || !familyName.equals(record.getFamilyName())) continue;
            return true;
        }
        return false;
    }

    public int countResidentsInBuilding(BuildingId buildingId) {
        int count = 0;
        for (VillagerRecord record : this.villagerRecords.values()) {
            if (!buildingId.equals(record.getHomeBuilding())) continue;
            ++count;
        }
        return count;
    }

    public void updateVillagerType(UUID uuid, ResourceLocation newTypeId) {
        VillagerRecord record = this.villagerRecords.get(uuid);
        if (record != null) {
            record.setVillagerTypeId(newTypeId);
            this.villagerTypesCache = null;
            this.invalidateDefenderCount();
        }
        this.dirty = true;
    }

    @Nullable
    Map<Item, Integer> getImportsNeededCache() {
        return this.importsNeededCache;
    }

    long getImportsNeededCacheExpiry() {
        return this.importsNeededCacheExpiry;
    }

    void setImportsNeededCache(Map<Item, Integer> cache, long expiryMs) {
        this.importsNeededCache = cache;
        this.importsNeededCacheExpiry = expiryMs;
    }

    public int getVillageItemCount(ServerLevel level, Item item) {
        String key = BuiltInRegistries.ITEM.getKey((Object)item).toString();
        return this.getOrComputeVillageStock(level, key, k -> {
            int total = 0;
            for (BuildingInstance b : this.buildings) {
                BuildingInventory inv = b.getInventory();
                if (inv == null) continue;
                total += inv.getCount((Level)level, item);
            }
            return total;
        });
    }

    public int getVillageTagCount(ServerLevel level, TagKey<Item> tag) {
        String key = "#" + tag.location().toString();
        return this.getOrComputeVillageStock(level, key, k -> {
            int total = 0;
            for (BuildingInstance b : this.buildings) {
                BuildingInventory inv = b.getInventory();
                if (inv == null) continue;
                total += inv.getCountByTag((Level)level, tag);
            }
            return total;
        });
    }

    private int getOrComputeVillageStock(ServerLevel level, String key, Function<String, Integer> compute) {
        Integer cached;
        long now = level.getGameTime();
        if (this.villageStockCache == null || now - this.villageStockCacheTick > 60L) {
            this.villageStockCache = new HashMap<String, Integer>();
            this.villageStockCacheTick = now;
        }
        if ((cached = this.villageStockCache.get(key)) != null) {
            return cached;
        }
        int total = compute.apply(key);
        this.villageStockCache.put(key, total);
        return total;
    }

    public record PendingProject(ResourceLocation planSetId, String variant, int level, boolean isUpgrade, @Nullable BuildingId buildingId, @Nullable PlacedLocation plannedLocation) {
        public PendingProject(ResourceLocation planSetId, String variant, int level, boolean isUpgrade, @Nullable BuildingId buildingId) {
            this(planSetId, variant, level, isUpgrade, buildingId, null);
        }
    }
}

