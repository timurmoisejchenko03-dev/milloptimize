/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  javax.annotation.Nullable
 *  net.minecraft.core.BlockPos
 *  net.minecraft.core.Direction
 *  net.minecraft.core.HolderLookup$Provider
 *  net.minecraft.nbt.CompoundTag
 *  net.minecraft.network.chat.Component
 *  net.minecraft.server.level.ServerLevel
 *  net.minecraft.sounds.SoundEvents
 *  net.minecraft.sounds.SoundSource
 *  net.minecraft.world.CompoundContainer
 *  net.minecraft.world.Container
 *  net.minecraft.world.entity.player.Player
 *  net.minecraft.world.inventory.AbstractContainerMenu
 *  net.minecraft.world.level.Level
 *  net.minecraft.world.level.block.ChestBlock
 *  net.minecraft.world.level.block.entity.BlockEntityType
 *  net.minecraft.world.level.block.entity.ChestBlockEntity
 *  net.minecraft.world.level.block.entity.ContainerOpenersCounter
 *  net.minecraft.world.level.block.state.BlockState
 *  net.minecraft.world.level.block.state.properties.ChestType
 *  net.minecraft.world.level.block.state.properties.Property
 */
package org.millenaire.block;

import javax.annotation.Nullable;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.CompoundContainer;
import net.minecraft.world.Container;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.ChestBlock;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.entity.ChestBlockEntity;
import net.minecraft.world.level.block.entity.ContainerOpenersCounter;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.ChestType;
import net.minecraft.world.level.block.state.properties.Property;
import org.millenaire.block.ModBlockEntities;
import org.millenaire.building.BuildingId;
import org.millenaire.building.BuildingInstance;
import org.millenaire.building.BuildingPlanSet;
import org.millenaire.commerce.LockedChestMenu;
import org.millenaire.culture.ModCultures;
import org.millenaire.village.Village;
import org.millenaire.village.VillageSavedData;

public class LockedChestBlockEntity
extends ChestBlockEntity {
    @Nullable
    private BuildingId buildingId;
    private final ContainerOpenersCounter lockedChestOpenersCounter = new ContainerOpenersCounter(){

        protected void onOpen(Level level, BlockPos pos, BlockState state) {
            ChestType chestType = (ChestType)state.getValue((Property)ChestBlock.TYPE);
            if (chestType != ChestType.LEFT) {
                double x = (double)pos.getX() + 0.5;
                double y = (double)pos.getY() + 0.5;
                double z = (double)pos.getZ() + 0.5;
                if (chestType == ChestType.RIGHT) {
                    Direction dir = ChestBlock.getConnectedDirection((BlockState)state);
                    x += (double)dir.getStepX() * 0.5;
                    z += (double)dir.getStepZ() * 0.5;
                }
                level.playSound(null, x, y, z, SoundEvents.CHEST_OPEN, SoundSource.BLOCKS, 0.5f, level.random.nextFloat() * 0.1f + 0.9f);
            }
        }

        protected void onClose(Level level, BlockPos pos, BlockState state) {
            ChestType chestType = (ChestType)state.getValue((Property)ChestBlock.TYPE);
            if (chestType != ChestType.LEFT) {
                double x = (double)pos.getX() + 0.5;
                double y = (double)pos.getY() + 0.5;
                double z = (double)pos.getZ() + 0.5;
                if (chestType == ChestType.RIGHT) {
                    Direction dir = ChestBlock.getConnectedDirection((BlockState)state);
                    x += (double)dir.getStepX() * 0.5;
                    z += (double)dir.getStepZ() * 0.5;
                }
                level.playSound(null, x, y, z, SoundEvents.CHEST_CLOSE, SoundSource.BLOCKS, 0.5f, level.random.nextFloat() * 0.1f + 0.9f);
            }
        }

        protected void openerCountChanged(Level level, BlockPos pos, BlockState state, int count, int openCount) {
            LockedChestBlockEntity.this.signalOpenCount(level, pos, state, count, openCount);
        }

        protected boolean isOwnContainer(Player player) {
            AbstractContainerMenu abstractContainerMenu = player.containerMenu;
            if (abstractContainerMenu instanceof LockedChestMenu) {
                CompoundContainer cc;
                LockedChestMenu lockedMenu = (LockedChestMenu)abstractContainerMenu;
                Container container = lockedMenu.getContainer();
                return container == LockedChestBlockEntity.this || container instanceof CompoundContainer && (cc = (CompoundContainer)container).contains((Container)LockedChestBlockEntity.this);
            }
            return false;
        }
    };

    public LockedChestBlockEntity(BlockPos pos, BlockState state) {
        super((BlockEntityType)ModBlockEntities.LOCKED_CHEST.get(), pos, state);
    }

    public void startOpen(Player player) {
        if (!this.isRemoved() && !player.isSpectator()) {
            this.lockedChestOpenersCounter.incrementOpeners(player, this.getLevel(), this.getBlockPos(), this.getBlockState());
        }
    }

    public void stopOpen(Player player) {
        if (!this.isRemoved() && !player.isSpectator()) {
            this.lockedChestOpenersCounter.decrementOpeners(player, this.getLevel(), this.getBlockPos(), this.getBlockState());
        }
    }

    public void recheckOpen() {
        if (!this.isRemoved()) {
            this.lockedChestOpenersCounter.recheckOpeners(this.getLevel(), this.getBlockPos(), this.getBlockState());
        }
    }

    @Nullable
    public BuildingId getBuildingId() {
        return this.buildingId;
    }

    public void setBuildingId(@Nullable BuildingId buildingId) {
        this.buildingId = buildingId;
        this.setChanged();
    }

    public boolean isLockedFor(Player player) {
        if (this.buildingId == null) {
            return false;
        }
        Level level = this.level;
        if (!(level instanceof ServerLevel)) {
            return true;
        }
        ServerLevel serverLevel = (ServerLevel)level;
        Village village = VillageSavedData.get(serverLevel).getVillageManager().findVillageContaining(this.buildingId);
        if (village == null) {
            return false;
        }
        if (village.isControlledBy(player.getUUID())) {
            return false;
        }
        return village.areChestsLocked();
    }

    protected Component getDefaultName() {
        String buildingName = this.resolveBuildingNativeName();
        if (buildingName != null) {
            return Component.translatable((String)"block.millenaire.chest_named", (Object[])new Object[]{buildingName});
        }
        return Component.translatable((String)"block.millenaire.chest");
    }

    @Nullable
    private String resolveBuildingNativeName() {
        BuildingPlanSet planSet;
        Level level;
        if (this.buildingId == null || !((level = this.level) instanceof ServerLevel)) {
            return null;
        }
        ServerLevel serverLevel = (ServerLevel)level;
        Village village = VillageSavedData.get(serverLevel).getVillageManager().findVillageContaining(this.buildingId);
        if (village == null) {
            return null;
        }
        BuildingInstance building = village.findBuildingById(this.buildingId);
        if (building == null) {
            return null;
        }
        if (building.getPlanSetId() != null && (planSet = ModCultures.getBuildingPlanSet(building.getPlanSetId())) != null) {
            BuildingPlanSet.LevelDef levelDef = planSet.getLevel(building.getVariant(), building.getLevel());
            if (levelDef != null && levelDef.nativeName() != null) {
                return levelDef.nativeName();
            }
            return planSet.nativeName();
        }
        return building.getPlanId().getPath();
    }

    public boolean hasCustomName() {
        return true;
    }

    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        if (this.buildingId != null) {
            tag.putUUID("building_id", this.buildingId.uuid());
        }
    }

    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        if (tag.hasUUID("building_id")) {
            this.buildingId = new BuildingId(tag.getUUID("building_id"));
        }
    }
}

