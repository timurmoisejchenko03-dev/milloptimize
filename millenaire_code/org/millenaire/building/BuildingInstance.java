/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  javax.annotation.Nullable
 *  net.minecraft.core.BlockPos
 *  net.minecraft.core.Direction
 *  net.minecraft.core.Vec3i
 *  net.minecraft.resources.ResourceLocation
 *  net.minecraft.server.level.ServerLevel
 *  net.minecraft.util.RandomSource
 *  net.minecraft.world.item.DyeColor
 *  net.minecraft.world.item.Item
 *  net.minecraft.world.level.block.BedBlock
 *  net.minecraft.world.level.block.Rotation
 *  net.minecraft.world.level.block.entity.BlockEntity
 *  net.minecraft.world.level.block.state.BlockState
 *  net.minecraft.world.level.block.state.properties.BedPart
 *  net.minecraft.world.level.block.state.properties.Property
 *  net.minecraft.world.level.levelgen.structure.templatesystem.StructurePlaceSettings
 *  net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplate
 */
package org.millenaire.building;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import javax.annotation.Nullable;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Vec3i;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.BedBlock;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BedPart;
import net.minecraft.world.level.block.state.properties.Property;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructurePlaceSettings;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplate;
import org.millenaire.block.LockedChestBlockEntity;
import org.millenaire.building.BedManager;
import org.millenaire.building.BuildingId;
import org.millenaire.building.BuildingInventory;
import org.millenaire.building.BuildingPlan;
import org.millenaire.building.BuildingPlanSet;
import org.millenaire.building.ConstructionTask;
import org.millenaire.building.SpecialPoint;
import org.millenaire.culture.ModCultures;
import org.millenaire.village.BrickColourTheme;

public class BuildingInstance {
    private final BuildingId id;
    private ResourceLocation planId;
    private BlockPos origin;
    private final Rotation rotation;
    @Nullable
    private ResourceLocation planSetId;
    @Nullable
    private String variant;
    private int level;
    private Status status;
    @Nullable
    private ConstructionTask constructionTask;
    private List<SpecialPoint> resolvedPoints = Collections.emptyList();
    @Nullable
    private Map<String, List<SpecialPoint>> pointsByTypeCache;
    @Nullable
    private Map<String, List<BlockPos>> positionsByTypeCache;
    @Nullable
    private BuildingInventory inventory;
    private boolean subBuilding;
    @Nullable
    private Map<DyeColor, DyeColor> brickColourMapping;
    @Nullable
    private BedManager bedManager;
    private boolean bedManagerInitialized;
    private boolean upgradesAllowed = true;
    private long lastMarketNightDay = -1L;
    private int nbNightsMerchant;
    private final Map<Item, Integer> imported = new LinkedHashMap<Item, Integer>();
    private final Map<Item, Integer> exported = new LinkedHashMap<Item, Integer>();
    private final List<String> visitorLog = new ArrayList<String>();
    private static final int MAX_VISITOR_LOG = 50;
    private transient long lastAnimalSpawnTick;
    private final Set<String> runtimeTags = new LinkedHashSet<String>();
    @Nullable
    private BuildingId parentBuildingId;
    private int cachedMinX = Integer.MIN_VALUE;
    private int cachedMaxX;
    private int cachedMinZ;
    private int cachedMaxZ;
    private int cachedWidth = -1;
    private int cachedDepth = -1;
    private static final Set<String> UNIQUE_POINT_TYPES = Set.of("sleepingPos", "craftingPos", "defendingPos", "shelterPos", "pathStartPos", "leisurePos");
    private static final Set<String> OVERRIDE_SET_TYPES = Set.of("sellingPos", "hearth");

    public BuildingInstance(BuildingId id, ResourceLocation planId, BlockPos origin, Rotation rotation, Status status) {
        this.id = id;
        this.planId = planId;
        this.origin = origin;
        this.rotation = rotation;
        this.status = status;
        this.level = 0;
    }

    public BuildingInstance(BuildingId id, ResourceLocation planId, BlockPos origin, Rotation rotation, Status status, @Nullable ResourceLocation planSetId, @Nullable String variant, int level) {
        this.id = id;
        this.planId = planId;
        this.origin = origin;
        this.rotation = rotation;
        this.status = status;
        this.planSetId = planSetId;
        this.variant = variant;
        this.level = level;
    }

    public BuildingId getId() {
        return this.id;
    }

    public ResourceLocation getPlanId() {
        return this.planId;
    }

    public BlockPos getOrigin() {
        return this.origin;
    }

    public void setOrigin(BlockPos newOrigin) {
        this.origin = newOrigin;
    }

    public int getWallFootY() {
        if (this.planId == null) {
            return this.origin.getY();
        }
        BuildingPlan plan = ModCultures.getBuildingPlan(this.planId);
        if (plan == null) {
            return this.origin.getY();
        }
        return this.origin.getY() - plan.groundLevel();
    }

    public Rotation getRotation() {
        return this.rotation;
    }

    @Nullable
    public ResourceLocation getPlanSetId() {
        return this.planSetId;
    }

    @Nullable
    public String getVariant() {
        return this.variant;
    }

    public int getLevel() {
        return this.level;
    }

    public Status getStatus() {
        return this.status;
    }

    public void setStatus(Status status) {
        this.status = status;
    }

    public boolean isBeingBuilt() {
        return this.status == Status.UNDER_CONSTRUCTION || this.status == Status.UPGRADING;
    }

    public boolean isOperational() {
        return this.status == Status.COMPLETE || this.status == Status.UPGRADING;
    }

    @Nullable
    public ConstructionTask getConstructionTask() {
        return this.constructionTask;
    }

    public boolean containsPos(BlockPos pos) {
        if (this.cachedMinX == Integer.MIN_VALUE) {
            this.populateDimensionCache();
        }
        if (this.cachedWidth == 0) {
            return false;
        }
        int ox = this.origin.getX();
        int oz = this.origin.getZ();
        return pos.getX() >= ox + this.cachedMinX && pos.getX() <= ox + this.cachedMaxX && pos.getZ() >= oz + this.cachedMinZ && pos.getZ() <= oz + this.cachedMaxZ;
    }

    private void populateDimensionCache() {
        BuildingPlan plan = ModCultures.getBuildingPlan(this.planId);
        if (plan == null) {
            this.cachedWidth = 0;
            this.cachedDepth = 0;
            this.cachedMinX = 0;
            this.cachedMaxX = 0;
            this.cachedMinZ = 0;
            this.cachedMaxZ = 0;
            return;
        }
        StructurePlaceSettings settings = new StructurePlaceSettings().setRotation(this.rotation);
        BlockPos cornerA = StructureTemplate.calculateRelativePosition((StructurePlaceSettings)settings, (BlockPos)BlockPos.ZERO);
        BlockPos cornerB = StructureTemplate.calculateRelativePosition((StructurePlaceSettings)settings, (BlockPos)new BlockPos(plan.width() - 1, 0, plan.depth() - 1));
        this.cachedMinX = Math.min(cornerA.getX(), cornerB.getX());
        this.cachedMaxX = Math.max(cornerA.getX(), cornerB.getX());
        this.cachedMinZ = Math.min(cornerA.getZ(), cornerB.getZ());
        this.cachedMaxZ = Math.max(cornerA.getZ(), cornerB.getZ());
        this.cachedWidth = this.cachedMaxX - this.cachedMinX + 1;
        this.cachedDepth = this.cachedMaxZ - this.cachedMinZ + 1;
    }

    public int getEffectiveWidth() {
        if (this.cachedWidth < 0) {
            this.populateDimensionCache();
        }
        return this.cachedWidth;
    }

    public int getEffectiveDepth() {
        if (this.cachedDepth < 0) {
            this.populateDimensionCache();
        }
        return this.cachedDepth;
    }

    public int getCachedMinX() {
        if (this.cachedMinX == Integer.MIN_VALUE) {
            this.populateDimensionCache();
        }
        return this.cachedMinX;
    }

    public int getCachedMaxX() {
        if (this.cachedMinX == Integer.MIN_VALUE) {
            this.populateDimensionCache();
        }
        return this.cachedMaxX;
    }

    public int getCachedMinZ() {
        if (this.cachedMinX == Integer.MIN_VALUE) {
            this.populateDimensionCache();
        }
        return this.cachedMinZ;
    }

    public int getCachedMaxZ() {
        if (this.cachedMinX == Integer.MIN_VALUE) {
            this.populateDimensionCache();
        }
        return this.cachedMaxZ;
    }

    public boolean isSubBuilding() {
        return this.subBuilding;
    }

    public boolean isWallSegment() {
        if (this.planSetId == null) {
            return false;
        }
        BuildingPlanSet planSet = ModCultures.getBuildingPlanSet(this.planSetId);
        return planSet != null && planSet.isWallSegment();
    }

    public void setSubBuilding(boolean subBuilding) {
        this.subBuilding = subBuilding;
    }

    public boolean isUpgradesAllowed() {
        return this.upgradesAllowed;
    }

    public void setUpgradesAllowed(boolean upgradesAllowed) {
        this.upgradesAllowed = upgradesAllowed;
    }

    public long getLastMarketNightDay() {
        return this.lastMarketNightDay;
    }

    public void setLastMarketNightDay(long day) {
        this.lastMarketNightDay = day;
    }

    public long getLastAnimalSpawnTick() {
        return this.lastAnimalSpawnTick;
    }

    public void setLastAnimalSpawnTick(long tick) {
        this.lastAnimalSpawnTick = tick;
    }

    public void initBrickColours(BrickColourTheme theme, RandomSource random) {
        this.brickColourMapping = theme.rollBuildingMapping(random);
    }

    public void initBrickColoursFromPlan(Map<DyeColor, List<BrickColourTheme.WeightedColor>> planColours, RandomSource random) {
        EnumMap<DyeColor, DyeColor> mapping = new EnumMap<DyeColor, DyeColor>(DyeColor.class);
        for (Map.Entry<DyeColor, List<BrickColourTheme.WeightedColor>> entry : planColours.entrySet()) {
            List<BrickColourTheme.WeightedColor> pool = entry.getValue();
            int totalWeight = 0;
            for (BrickColourTheme.WeightedColor wc : pool) {
                totalWeight += wc.weight();
            }
            int roll = random.nextInt(totalWeight);
            int cumulative = 0;
            DyeColor picked = entry.getKey();
            for (BrickColourTheme.WeightedColor wc : pool) {
                if (roll >= (cumulative += wc.weight())) continue;
                picked = wc.color();
                break;
            }
            mapping.put(entry.getKey(), picked);
        }
        this.brickColourMapping = mapping;
    }

    public void copyBrickColours(BuildingInstance source) {
        if (source.brickColourMapping != null) {
            this.brickColourMapping = new EnumMap<DyeColor, DyeColor>(source.brickColourMapping);
        }
    }

    public DyeColor remapBrickColour(DyeColor templateColor) {
        if (this.brickColourMapping == null) {
            return templateColor;
        }
        return this.brickColourMapping.getOrDefault(templateColor, templateColor);
    }

    @Nullable
    public Map<DyeColor, DyeColor> getBrickColourMapping() {
        return this.brickColourMapping;
    }

    public void setBrickColourMapping(@Nullable Map<DyeColor, DyeColor> mapping) {
        this.brickColourMapping = mapping;
    }

    public BedManager getBedManager() {
        if (this.bedManager == null) {
            this.bedManager = new BedManager();
        }
        return this.bedManager;
    }

    public boolean hasBedManager() {
        return this.bedManager != null && this.bedManager.hasBeds();
    }

    public boolean isBedManagerInitialized() {
        return this.bedManagerInitialized;
    }

    public void setBedManagerInitialized(boolean initialized) {
        this.bedManagerInitialized = initialized;
    }

    public void setBedManager(@Nullable BedManager bedManager) {
        this.bedManager = bedManager;
    }

    public void backfillBedPositions(ServerLevel level, BuildingPlan plan) {
        if (this.origin == null) {
            return;
        }
        BedManager mgr = this.getBedManager();
        StructurePlaceSettings settings = new StructurePlaceSettings().setRotation(this.rotation);
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
                    BlockPos pos = this.origin.offset(dx, dy, dz);
                    BlockState state = level.getBlockState(pos);
                    if (!(state.getBlock() instanceof BedBlock) || state.getValue((Property)BedBlock.PART) != BedPart.FOOT) continue;
                    mgr.add(pos);
                }
            }
        }
        this.bedManagerInitialized = true;
    }

    public void setConstructionTask(@Nullable ConstructionTask constructionTask) {
        this.constructionTask = constructionTask;
    }

    public void markUnderConstruction() {
        this.status = Status.UNDER_CONSTRUCTION;
    }

    public void markComplete() {
        this.status = Status.COMPLETE;
    }

    public void startUpgrade(ResourceLocation newPlanId, int newLevel) {
        this.planId = newPlanId;
        this.level = newLevel;
        this.status = Status.UPGRADING;
        this.constructionTask = null;
        this.cachedWidth = -1;
        this.cachedDepth = -1;
        this.cachedMinX = Integer.MIN_VALUE;
    }

    public void resolveSpecialPoints(BuildingPlan plan) {
        List<SpecialPoint> allPoints = this.planSetId == null || this.variant == null || this.level == 0 ? plan.specialPoints() : this.accumulatePointsFromAllLevels(plan);
        if (allPoints.isEmpty()) {
            this.resolvedPoints = Collections.emptyList();
            return;
        }
        ArrayList<SpecialPoint> resolved = new ArrayList<SpecialPoint>();
        for (SpecialPoint sp : allPoints) {
            BlockPos rotated = StructureTemplate.calculateRelativePosition((StructurePlaceSettings)new StructurePlaceSettings().setRotation(this.rotation), (BlockPos)sp.pos());
            BlockPos absolute = rotated.offset((Vec3i)this.origin);
            String rotatedOrientation = BuildingInstance.rotateOrientation(sp.orientation(), this.rotation);
            resolved.add(new SpecialPoint(sp.type(), sp.subtype(), rotatedOrientation, sp.placement(), absolute));
        }
        this.resolvedPoints = Collections.unmodifiableList(resolved);
        this.pointsByTypeCache = null;
        this.positionsByTypeCache = null;
    }

    static String rotateOrientation(@Nullable String orientation, Rotation rotation) {
        Direction dir;
        if (orientation == null || "guess".equals(orientation) || rotation == Rotation.NONE) {
            return orientation;
        }
        switch (orientation.toLowerCase(Locale.ROOT)) {
            case "north": {
                Direction direction = Direction.NORTH;
                break;
            }
            case "south": {
                Direction direction = Direction.SOUTH;
                break;
            }
            case "east": {
                Direction direction = Direction.EAST;
                break;
            }
            case "west": {
                Direction direction = Direction.WEST;
                break;
            }
            default: {
                Direction direction = dir = null;
            }
        }
        if (dir == null) {
            return orientation;
        }
        Direction rotated = rotation.rotate(dir);
        return rotated.getName();
    }

    private List<SpecialPoint> accumulatePointsFromAllLevels(BuildingPlan currentPlan) {
        BuildingPlanSet planSet = ModCultures.getBuildingPlanSet(this.planSetId);
        if (planSet == null) {
            return currentPlan.specialPoints();
        }
        LinkedHashMap<String, SpecialPoint> uniquePoints = new LinkedHashMap<String, SpecialPoint>();
        LinkedHashMap<String, List> overrideSetPoints = new LinkedHashMap<String, List>();
        ArrayList<SpecialPoint> multiplePoints = new ArrayList<SpecialPoint>();
        int currentGroundLevel = currentPlan.groundLevel();
        for (int lvl = 0; lvl <= this.level; ++lvl) {
            BuildingPlan lvlPlan;
            ResourceLocation lvlPlanId = planSet.getPlanId(this.variant, lvl);
            if (lvlPlanId == null || (lvlPlan = ModCultures.getBuildingPlan(lvlPlanId)) == null || lvlPlan.specialPoints().isEmpty()) continue;
            int yOffset = lvlPlan.groundLevel() - currentGroundLevel;
            HashMap<String, List> levelOverrideSets = new HashMap<String, List>();
            for (SpecialPoint specialPoint : lvlPlan.specialPoints()) {
                SpecialPoint adjusted = BuildingInstance.adjustForGroundLevel(specialPoint, lvlPlan.groundLevel(), currentGroundLevel);
                if (UNIQUE_POINT_TYPES.contains(adjusted.type())) {
                    uniquePoints.put(adjusted.type(), adjusted);
                    continue;
                }
                if (OVERRIDE_SET_TYPES.contains(adjusted.type())) {
                    levelOverrideSets.computeIfAbsent(adjusted.type(), k -> new ArrayList()).add(adjusted);
                    continue;
                }
                boolean duplicate = multiplePoints.stream().anyMatch(existing -> existing.pos().equals((Object)adjusted.pos()) && existing.type().equals(adjusted.type()));
                if (duplicate) continue;
                multiplePoints.add(adjusted);
            }
            for (Map.Entry entry : levelOverrideSets.entrySet()) {
                overrideSetPoints.put((String)entry.getKey(), (List)entry.getValue());
            }
        }
        ArrayList<SpecialPoint> result = new ArrayList<SpecialPoint>(uniquePoints.values());
        for (List overrideSet : overrideSetPoints.values()) {
            result.addAll(overrideSet);
        }
        result.addAll(multiplePoints);
        return result;
    }

    static SpecialPoint adjustForGroundLevel(SpecialPoint sp, int sourceGroundLevel, int currentGroundLevel) {
        int yOffset = sourceGroundLevel - currentGroundLevel;
        if (yOffset == 0) {
            return sp;
        }
        return new SpecialPoint(sp.type(), sp.subtype(), sp.orientation(), sp.placement(), sp.pos().offset(0, yOffset, 0));
    }

    public List<SpecialPoint> getResolvedPoints() {
        return this.resolvedPoints;
    }

    private Map<String, List<SpecialPoint>> getOrBuildPointsByTypeCache() {
        if (this.pointsByTypeCache == null) {
            HashMap<String, List<SpecialPoint>> map = new HashMap<String, List<SpecialPoint>>();
            for (SpecialPoint sp : this.resolvedPoints) {
                map.computeIfAbsent(sp.type(), k -> new ArrayList()).add(sp);
            }
            map.replaceAll((k, v) -> Collections.unmodifiableList(v));
            this.pointsByTypeCache = map;
        }
        return this.pointsByTypeCache;
    }

    private Map<String, List<BlockPos>> getOrBuildPositionsByTypeCache() {
        if (this.positionsByTypeCache == null) {
            HashMap<String, List<BlockPos>> map = new HashMap<String, List<BlockPos>>();
            for (SpecialPoint sp : this.resolvedPoints) {
                map.computeIfAbsent(sp.type(), k -> new ArrayList()).add(sp.pos());
            }
            map.replaceAll((k, v) -> Collections.unmodifiableList(v));
            this.positionsByTypeCache = map;
        }
        return this.positionsByTypeCache;
    }

    public List<SpecialPoint> getPointsByType(String type) {
        return this.getOrBuildPointsByTypeCache().getOrDefault(type, Collections.emptyList());
    }

    public List<BlockPos> getFireplacePositions() {
        return this.getOrBuildPositionsByTypeCache().getOrDefault("fireplace", Collections.emptyList());
    }

    public List<BlockPos> getHearthPositions() {
        return this.getOrBuildPositionsByTypeCache().getOrDefault("hearth", Collections.emptyList());
    }

    public BlockPos getPathStartPos() {
        return this.resolveNavigationTarget("pathStartPos", "sellingPos", "sleepingPos");
    }

    @Nullable
    public BlockPos resolvePathAnchor() {
        BlockPos p = this.getFirstPointPos("pathStartPos");
        if (p != null) {
            return p;
        }
        p = this.getFirstPointPos("sellingPos");
        if (p != null) {
            return p;
        }
        return this.getFirstPointPos("sleepingPos");
    }

    public BlockPos getSellingPos() {
        return this.resolveNavigationTarget("sellingPos", "pathStartPos", "sleepingPos");
    }

    public BlockPos getSleepingPos() {
        return this.resolveNavigationTarget("sleepingPos");
    }

    public BlockPos resolveNavigationTarget(String ... priorities) {
        for (String type : priorities) {
            BlockPos pos = this.getFirstPointPos(type);
            if (pos == null) continue;
            return pos;
        }
        return this.getOrigin();
    }

    @Nullable
    public BlockPos getFirstPointPos(String type) {
        List<BlockPos> positions = this.getOrBuildPositionsByTypeCache().get(type);
        return positions != null && !positions.isEmpty() ? positions.get(0) : null;
    }

    public List<BlockPos> getChestPositions() {
        return this.getOrBuildPositionsByTypeCache().getOrDefault("chest", Collections.emptyList());
    }

    public List<BlockPos> getFurnacePositions() {
        return this.getOrBuildPositionsByTypeCache().getOrDefault("furnace", Collections.emptyList());
    }

    public List<BlockPos> getFirePitPositions() {
        return this.getOrBuildPositionsByTypeCache().getOrDefault("fire_pit", Collections.emptyList());
    }

    public List<BlockPos> getSoilPositions() {
        return this.getOrBuildPositionsByTypeCache().getOrDefault("soil", Collections.emptyList());
    }

    public List<BlockPos> getSoilPositions(@Nullable String cropSubtype) {
        if (cropSubtype == null) {
            return this.getSoilPositions();
        }
        List soilPoints = this.getOrBuildPointsByTypeCache().getOrDefault("soil", Collections.emptyList());
        ArrayList<BlockPos> result = new ArrayList<BlockPos>();
        for (SpecialPoint sp : soilPoints) {
            if (!cropSubtype.equals(sp.subtype())) continue;
            result.add(sp.pos());
        }
        return result;
    }

    public List<BlockPos> getBrickSpotPositions() {
        return this.getOrBuildPositionsByTypeCache().getOrDefault("brick_spot", Collections.emptyList());
    }

    public List<BlockPos> getSilkwormBlockPositions() {
        return this.getOrBuildPositionsByTypeCache().getOrDefault("silkwormBlock", Collections.emptyList());
    }

    public List<BlockPos> getSnailSoilBlockPositions() {
        return this.getOrBuildPositionsByTypeCache().getOrDefault("snailSoilBlock", Collections.emptyList());
    }

    public int getNbNightsMerchant() {
        return this.nbNightsMerchant;
    }

    public int incrementAndGetMerchantNights() {
        return ++this.nbNightsMerchant;
    }

    public void resetMerchantNights() {
        this.nbNightsMerchant = 0;
    }

    public void setNbNightsMerchant(int value) {
        this.nbNightsMerchant = value;
    }

    public void addImported(Item item, int count) {
        this.imported.merge(item, count, Integer::sum);
    }

    public void addExported(Item item, int count) {
        this.exported.merge(item, count, Integer::sum);
    }

    public Map<Item, Integer> getImported() {
        return Collections.unmodifiableMap(this.imported);
    }

    public Map<Item, Integer> getExported() {
        return Collections.unmodifiableMap(this.exported);
    }

    public void setImported(Map<Item, Integer> data) {
        this.imported.clear();
        this.imported.putAll(data);
    }

    public void setExported(Map<Item, Integer> data) {
        this.exported.clear();
        this.exported.putAll(data);
    }

    public void addVisitorLog(String entry) {
        this.visitorLog.add(entry);
        if (this.visitorLog.size() > 50) {
            this.visitorLog.subList(0, this.visitorLog.size() - 50).clear();
        }
    }

    public List<String> getVisitorLog() {
        return Collections.unmodifiableList(this.visitorLog);
    }

    public void setVisitorLog(List<String> entries) {
        this.visitorLog.clear();
        this.visitorLog.addAll(entries);
    }

    public void addRuntimeTags(Collection<String> tags) {
        for (String tag : tags) {
            this.runtimeTags.add(tag.toLowerCase(Locale.ROOT));
        }
    }

    public void removeRuntimeTags(Collection<String> tags) {
        for (String tag : tags) {
            this.runtimeTags.remove(tag.toLowerCase(Locale.ROOT));
        }
    }

    public boolean hasRuntimeTag(String tag) {
        return this.runtimeTags.contains(tag.toLowerCase(Locale.ROOT));
    }

    public Set<String> getRuntimeTags() {
        return Collections.unmodifiableSet(this.runtimeTags);
    }

    @Nullable
    public BuildingId getParentBuildingId() {
        return this.parentBuildingId;
    }

    public void setParentBuildingId(@Nullable BuildingId parentBuildingId) {
        this.parentBuildingId = parentBuildingId;
    }

    public void initInventory() {
        List<BlockPos> chests = this.getChestPositions();
        List<BlockPos> furnaces = this.getFurnacePositions();
        List<BlockPos> firepits = this.getFirePitPositions();
        if (!(chests.isEmpty() && furnaces.isEmpty() && firepits.isEmpty())) {
            this.inventory = new BuildingInventory(chests, furnaces, firepits);
        }
    }

    public void linkChestsToBuilding(ServerLevel level) {
        for (BlockPos chestPos : this.getChestPositions()) {
            BlockEntity be = level.getBlockEntity(chestPos);
            if (!(be instanceof LockedChestBlockEntity)) continue;
            LockedChestBlockEntity lockedChest = (LockedChestBlockEntity)be;
            lockedChest.setBuildingId(this.id);
        }
    }

    @Nullable
    public BuildingInventory getInventory() {
        return this.inventory;
    }

    public static enum Status {
        PLANNED,
        UNDER_CONSTRUCTION,
        UPGRADING,
        COMPLETE;

    }
}

