/*
 * Decompiled with CFR 0.150.
 * 
 * Could not load the following classes:
 *  javax.annotation.Nullable
 *  net.minecraft.core.BlockPos
 *  net.minecraft.core.HolderLookup$Provider
 *  net.minecraft.core.Vec3i
 *  net.minecraft.nbt.CompoundTag
 *  net.minecraft.network.protocol.Packet
 *  net.minecraft.network.protocol.game.ClientGamePacketListener
 *  net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket
 *  net.minecraft.server.level.ServerLevel
 *  net.minecraft.server.level.ServerPlayer
 *  net.minecraft.world.level.Level
 *  net.minecraft.world.level.block.entity.BlockEntity
 *  net.minecraft.world.level.block.entity.BlockEntityType
 *  net.minecraft.world.level.block.state.BlockState
 *  net.minecraft.world.level.chunk.LevelChunk
 */
package org.millenaire.block;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import javax.annotation.Nullable;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.Vec3i;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.LevelChunk;
import org.millenaire.block.ModBlockEntities;
import org.millenaire.building.BuildingExporter;
import org.millenaire.building.BuildingImporter;

public class ImportTableBlockEntity
extends BlockEntity {
    private String buildingId = "";
    private String variant = "a";
    private String cultureKey = "";
    private String parentBuildingId = "";
    private String parentVariant = "";
    private int parentTriggerLevel = -1;
    private int length = 10;
    private int width = 10;
    private int upgradeLevel = 0;
    private int startingLevel = -1;
    private int height = 10;
    private int orientation = 0;
    private boolean clearGround = false;
    private boolean exportSnow = false;
    private boolean importMockBlocks = true;
    private boolean convertToPreserveGround = true;
    @Nullable
    private BlockPos mainTablePos = null;
    private boolean isMainTable = false;
    private boolean importedFromCulture = false;
    private long lastSavedBlocksHash = 0L;
    private boolean dirty = false;
    private int ticksSinceLastCheck = 0;
    private int savedOrientation = 0;
    private int savedStartingLevel = -1;
    private int savedHeight = 10;
    private boolean savedExportSnow = false;
    private boolean savedImportMockBlocks = true;
    private boolean savedConvertToPreserveGround = true;
    private boolean savedStateInitialised = false;
    private boolean importSettlePending = false;
    private static final int CHILD_SEARCH_RADIUS = 1360;
    private static final int CHECK_INTERVAL_TICKS = 100;
    private static final int NEAR_PLAYER_MARGIN = 16;

    public ImportTableBlockEntity(BlockPos pos, BlockState state) {
        super((BlockEntityType)ModBlockEntities.IMPORT_TABLE.get(), pos, state);
    }

    public String getBuildingId() {
        return this.buildingId;
    }

    public String getVariant() {
        return this.variant;
    }

    public String getCultureKey() {
        return this.cultureKey;
    }

    public String getParentBuildingId() {
        return this.parentBuildingId;
    }

    public String getParentVariant() {
        return this.parentVariant;
    }

    public int getParentTriggerLevel() {
        return this.parentTriggerLevel;
    }

    public int getLength() {
        return this.length;
    }

    public int getWidth() {
        return this.width;
    }

    public int getUpgradeLevel() {
        return this.upgradeLevel;
    }

    public int getStartingLevel() {
        return this.startingLevel;
    }

    public int getHeight() {
        return this.height;
    }

    public int getOrientation() {
        return this.orientation;
    }

    public boolean isClearGround() {
        return this.clearGround;
    }

    public boolean isExportSnow() {
        return this.exportSnow;
    }

    public boolean isImportMockBlocks() {
        return this.importMockBlocks;
    }

    public boolean isConvertToPreserveGround() {
        return this.convertToPreserveGround;
    }

    @Nullable
    public BlockPos getMainTablePos() {
        return this.mainTablePos;
    }

    public boolean isMainTable() {
        return this.isMainTable;
    }

    public boolean isImportedFromCulture() {
        return this.importedFromCulture;
    }

    public long getLastSavedBlocksHash() {
        return this.lastSavedBlocksHash;
    }

    public boolean isDirty() {
        return this.dirty;
    }

    public boolean hasPlan() {
        return !this.buildingId.isEmpty();
    }

    public boolean hasParentContext() {
        return !this.parentBuildingId.isEmpty();
    }

    public void clearParentContext() {
        this.parentBuildingId = "";
        this.parentVariant = "";
        this.parentTriggerLevel = -1;
        this.setChanged();
    }

    public boolean isLinked() {
        return ImportTableBlockEntity.isLinked(this.isMainTable, this.mainTablePos);
    }

    static boolean isLinked(boolean isMainTable, @Nullable BlockPos mainTablePos) {
        return isMainTable || mainTablePos != null;
    }

    public ImportTableBlockEntity resolveMainTable(ServerLevel level) {
        ImportTableBlockEntity main;
        if (this.isMainTable || this.mainTablePos == null) {
            return this;
        }
        BlockEntity be = level.getBlockEntity(this.mainTablePos);
        if (be instanceof ImportTableBlockEntity && (main = (ImportTableBlockEntity)be).isMainTable()) {
            return main;
        }
        return this;
    }

    public List<ImportTableBlockEntity> findChildTables(ServerLevel level) {
        if (!this.isMainTable) {
            return List.of();
        }
        BlockPos mainPos = this.getBlockPos();
        ArrayList<ImportTableBlockEntity> heads = new ArrayList<ImportTableBlockEntity>();
        ImportTableBlockEntity.collectChildTables(level, mainPos, mainPos.getX() - 4 >> 4, mainPos.getX() + 1360 >> 4, mainPos.getZ() - 4 >> 4, mainPos.getZ() + 4 >> 4, heads);
        ArrayList<ImportTableBlockEntity> result = new ArrayList<ImportTableBlockEntity>(heads);
        ArrayList<BlockPos> columnAnchors = new ArrayList<BlockPos>(1 + heads.size());
        columnAnchors.add(mainPos);
        for (ImportTableBlockEntity head : heads) {
            columnAnchors.add(head.getBlockPos());
        }
        for (BlockPos anchor : columnAnchors) {
            ImportTableBlockEntity.collectChildTables(level, mainPos, anchor.getX() - 4 >> 4, anchor.getX() + 4 >> 4, mainPos.getZ() - 4 >> 4, mainPos.getZ() + 1360 >> 4, result);
        }
        result.sort(Comparator.comparingInt(ImportTableBlockEntity::getUpgradeLevel));
        return result;
    }

    private static void collectChildTables(ServerLevel level, BlockPos mainPos, int minCX, int maxCX, int minCZ, int maxCZ, List<ImportTableBlockEntity> children) {
        for (int cx = minCX; cx <= maxCX; ++cx) {
            for (int cz = minCZ; cz <= maxCZ; ++cz) {
                LevelChunk chunk = level.getChunk(cx, cz);
                for (BlockEntity blockEntity : chunk.getBlockEntities().values()) {
                    ImportTableBlockEntity childBe;
                    BlockPos childMain;
                    if (!(blockEntity instanceof ImportTableBlockEntity) || (childMain = (childBe = (ImportTableBlockEntity)blockEntity).getMainTablePos()) == null || !childMain.equals((Object)mainPos) || children.contains((Object)childBe)) continue;
                    children.add(childBe);
                }
            }
        }
    }

    public void propagatePlanIdentity(ServerLevel level) {
        for (ImportTableBlockEntity child : this.findChildTables(level)) {
            child.setCultureKey(this.cultureKey);
            child.setBuildingId(this.buildingId);
        }
    }

    public void setBuildingId(String buildingId) {
        this.buildingId = buildingId;
        this.setChanged();
    }

    public void setVariant(String variant) {
        this.variant = variant;
        this.setChanged();
    }

    public void setCultureKey(String cultureKey) {
        this.cultureKey = cultureKey;
        this.setChanged();
    }

    public void setParentBuildingId(String parentBuildingId) {
        this.parentBuildingId = parentBuildingId;
        this.setChanged();
    }

    public void setParentVariant(String parentVariant) {
        this.parentVariant = parentVariant;
        this.setChanged();
    }

    public void setParentTriggerLevel(int parentTriggerLevel) {
        this.parentTriggerLevel = parentTriggerLevel;
        this.setChanged();
    }

    public void setLength(int length) {
        this.length = length;
        this.setChanged();
    }

    public void setWidth(int width) {
        this.width = width;
        this.setChanged();
    }

    public void setUpgradeLevel(int upgradeLevel) {
        this.upgradeLevel = upgradeLevel;
        this.setChanged();
    }

    public void setStartingLevel(int startingLevel) {
        this.startingLevel = startingLevel;
        this.setChanged();
    }

    public void setHeight(int height) {
        this.height = height;
        this.setChanged();
    }

    public void setOrientation(int orientation) {
        this.orientation = orientation;
        this.setChanged();
    }

    public void setClearGround(boolean clearGround) {
        this.clearGround = clearGround;
        this.setChanged();
    }

    public void setExportSnow(boolean exportSnow) {
        this.exportSnow = exportSnow;
        this.setChanged();
    }

    public void setImportMockBlocks(boolean importMockBlocks) {
        this.importMockBlocks = importMockBlocks;
        this.setChanged();
    }

    public void setConvertToPreserveGround(boolean convertToPreserveGround) {
        this.convertToPreserveGround = convertToPreserveGround;
        this.setChanged();
    }

    public void setMainTablePos(@Nullable BlockPos mainTablePos) {
        this.mainTablePos = mainTablePos;
        this.setChanged();
    }

    public void setIsMainTable(boolean isMainTable) {
        this.isMainTable = isMainTable;
        this.setChanged();
    }

    public void setImportedFromCulture(boolean importedFromCulture) {
        this.importedFromCulture = importedFromCulture;
        this.setChanged();
    }

    public void setLastSavedBlocksHash(long hash) {
        this.lastSavedBlocksHash = hash;
        this.setChanged();
    }

    public void setDirty(boolean dirty) {
        if (this.dirty == dirty) {
            return;
        }
        this.dirty = dirty;
        this.setChanged();
        Level level = this.level;
        if (level instanceof ServerLevel) {
            ServerLevel serverLevel = (ServerLevel)level;
            if (this.hasPlan()) {
                BuildingImporter.placeConstructionBorder(serverLevel, this);
            }
        }
    }

    public void captureSavedState(long blocksHash) {
        this.lastSavedBlocksHash = blocksHash;
        this.savedOrientation = this.orientation;
        this.savedStartingLevel = this.startingLevel;
        this.savedHeight = this.height;
        this.savedExportSnow = this.exportSnow;
        this.savedImportMockBlocks = this.importMockBlocks;
        this.savedConvertToPreserveGround = this.convertToPreserveGround;
        this.savedStateInitialised = true;
        this.setChanged();
    }

    public void markImportSettlePending() {
        this.importSettlePending = true;
    }

    public boolean hasUnsavedChanges(ServerLevel level) {
        if (!this.hasPlan() || !this.savedStateInitialised) {
            return false;
        }
        long current = BuildingExporter.computeBlocksHash(level, this);
        return current != this.lastSavedBlocksHash || !this.matchesSavedMeta();
    }

    private boolean matchesSavedMeta() {
        return this.orientation == this.savedOrientation && this.startingLevel == this.savedStartingLevel && this.height == this.savedHeight && this.exportSnow == this.savedExportSnow && this.importMockBlocks == this.savedImportMockBlocks && this.convertToPreserveGround == this.savedConvertToPreserveGround;
    }

    public void copySettingsTo(ImportTableBlockEntity other) {
        other.buildingId = this.buildingId;
        other.variant = this.variant;
        other.cultureKey = this.cultureKey;
        other.parentBuildingId = this.parentBuildingId;
        other.parentVariant = this.parentVariant;
        other.parentTriggerLevel = this.parentTriggerLevel;
        other.length = this.length;
        other.width = this.width;
        other.startingLevel = this.startingLevel;
        other.height = this.height;
        other.orientation = this.orientation;
        other.clearGround = this.clearGround;
        other.exportSnow = this.exportSnow;
        other.importMockBlocks = this.importMockBlocks;
        other.convertToPreserveGround = this.convertToPreserveGround;
        other.mainTablePos = this.getBlockPos();
        other.isMainTable = false;
        other.importedFromCulture = this.importedFromCulture;
        other.setChanged();
    }

    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        tag.putString("bid", this.buildingId);
        tag.putString("var", this.variant);
        tag.putString("culture", this.cultureKey);
        tag.putString("parent", this.parentBuildingId);
        tag.putString("parentVar", this.parentVariant);
        tag.putInt("parentTrigger", this.parentTriggerLevel);
        tag.putInt("len", this.length);
        tag.putInt("wid", this.width);
        tag.putInt("lvl", this.upgradeLevel);
        tag.putInt("slev", this.startingLevel);
        tag.putInt("hgt", this.height);
        tag.putInt("ori", this.orientation);
        tag.putBoolean("cg", this.clearGround);
        tag.putBoolean("snow", this.exportSnow);
        tag.putBoolean("mocks", this.importMockBlocks);
        tag.putBoolean("preserve", this.convertToPreserveGround);
        tag.putBoolean("isMain", this.isMainTable);
        tag.putBoolean("ifc", this.importedFromCulture);
        tag.putLong("savedHash", this.lastSavedBlocksHash);
        tag.putBoolean("dirty", this.dirty);
        tag.putBoolean("savedInit", this.savedStateInitialised);
        if (this.savedStateInitialised) {
            tag.putInt("savedOri", this.savedOrientation);
            tag.putInt("savedSlev", this.savedStartingLevel);
            tag.putInt("savedHgt", this.savedHeight);
            tag.putBoolean("savedSnow", this.savedExportSnow);
            tag.putBoolean("savedMocks", this.savedImportMockBlocks);
            tag.putBoolean("savedPreserve", this.savedConvertToPreserveGround);
        }
        if (this.mainTablePos != null) {
            tag.putInt("mainX", this.mainTablePos.getX());
            tag.putInt("mainY", this.mainTablePos.getY());
            tag.putInt("mainZ", this.mainTablePos.getZ());
        }
    }

    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        this.buildingId = tag.getString("bid");
        this.variant = tag.getString("var");
        this.cultureKey = tag.getString("culture");
        this.parentBuildingId = tag.getString("parent");
        this.parentVariant = tag.getString("parentVar");
        this.parentTriggerLevel = tag.contains("parentTrigger") ? tag.getInt("parentTrigger") : -1;
        this.length = tag.getInt("len");
        this.width = tag.getInt("wid");
        this.upgradeLevel = tag.getInt("lvl");
        this.startingLevel = tag.getInt("slev");
        this.height = tag.getInt("hgt");
        this.orientation = tag.getInt("ori");
        this.clearGround = tag.getBoolean("cg");
        this.exportSnow = tag.getBoolean("snow");
        this.importMockBlocks = tag.getBoolean("mocks");
        this.convertToPreserveGround = tag.getBoolean("preserve");
        this.isMainTable = tag.getBoolean("isMain");
        this.importedFromCulture = tag.getBoolean("ifc");
        this.lastSavedBlocksHash = tag.getLong("savedHash");
        this.dirty = tag.getBoolean("dirty");
        this.savedStateInitialised = tag.getBoolean("savedInit");
        if (this.savedStateInitialised) {
            this.savedOrientation = tag.getInt("savedOri");
            this.savedStartingLevel = tag.getInt("savedSlev");
            this.savedHeight = tag.getInt("savedHgt");
            this.savedExportSnow = tag.getBoolean("savedSnow");
            this.savedImportMockBlocks = tag.getBoolean("savedMocks");
            this.savedConvertToPreserveGround = tag.getBoolean("savedPreserve");
        }
        this.mainTablePos = tag.contains("mainX") ? new BlockPos(tag.getInt("mainX"), tag.getInt("mainY"), tag.getInt("mainZ")) : null;
    }

    public CompoundTag getUpdateTag(HolderLookup.Provider registries) {
        CompoundTag tag = new CompoundTag();
        this.saveAdditional(tag, registries);
        return tag;
    }

    @Nullable
    public Packet<ClientGamePacketListener> getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create((BlockEntity)this);
    }

    public static void serverTick(Level level, BlockPos pos, BlockState state, ImportTableBlockEntity be) {
        boolean shouldBeDirty;
        if (!(level instanceof ServerLevel)) {
            return;
        }
        ServerLevel serverLevel = (ServerLevel)level;
        if (!be.hasPlan()) {
            return;
        }
        ++be.ticksSinceLastCheck;
        if (be.ticksSinceLastCheck < 100) {
            return;
        }
        be.ticksSinceLastCheck = 0;
        if (!ImportTableBlockEntity.hasPlayerNear(serverLevel, be)) {
            return;
        }
        long current = BuildingExporter.computeBlocksHash(serverLevel, be);
        if (!be.savedStateInitialised) {
            be.captureSavedState(current);
            be.setDirty(false);
            return;
        }
        if (be.importSettlePending) {
            be.importSettlePending = false;
            be.captureSavedState(current);
            be.setDirty(false);
            return;
        }
        boolean blocksDirty = current != be.lastSavedBlocksHash;
        boolean metaDirty = !be.matchesSavedMeta();
        boolean bl = shouldBeDirty = blocksDirty || metaDirty;
        if (shouldBeDirty != be.dirty) {
            be.setDirty(shouldBeDirty);
        }
    }

    private static boolean hasPlayerNear(ServerLevel level, ImportTableBlockEntity be) {
        BlockPos origin = BuildingExporter.computeScanOrigin(be);
        Vec3i size = BuildingExporter.computeScanSize(be);
        int minX = origin.getX() - 16;
        int maxX = origin.getX() + size.getX() + 16;
        int minY = origin.getY() - 16;
        int maxY = origin.getY() + size.getY() + 16;
        int minZ = origin.getZ() - 16;
        int maxZ = origin.getZ() + size.getZ() + 16;
        for (ServerPlayer player : level.players()) {
            double px = player.getX();
            double py = player.getY();
            double pz = player.getZ();
            if (!(px >= (double)minX) || !(px <= (double)maxX) || !(py >= (double)minY) || !(py <= (double)maxY) || !(pz >= (double)minZ) || !(pz <= (double)maxZ)) continue;
            return true;
        }
        return false;
    }
}

