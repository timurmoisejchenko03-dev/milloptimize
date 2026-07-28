/*
 * Decompiled with CFR 0.150.
 * 
 * Could not load the following classes:
 *  javax.annotation.Nullable
 *  net.minecraft.core.BlockPos
 *  net.minecraft.core.HolderLookup$Provider
 *  net.minecraft.nbt.CompoundTag
 *  net.minecraft.nbt.ListTag
 *  net.minecraft.nbt.Tag
 *  net.minecraft.network.protocol.Packet
 *  net.minecraft.network.protocol.game.ClientGamePacketListener
 *  net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket
 *  net.minecraft.resources.ResourceLocation
 *  net.minecraft.world.item.ItemStack
 *  net.minecraft.world.level.block.entity.BlockEntity
 *  net.minecraft.world.level.block.entity.BlockEntityType
 *  net.minecraft.world.level.block.state.BlockState
 */
package org.millenaire.block;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.UUID;
import javax.annotation.Nullable;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import org.millenaire.block.ModBlockEntities;
import org.millenaire.building.BuildingId;
import org.millenaire.village.panel.PanelContentGenerator;
import org.millenaire.village.panel.PanelType;

public class VillagePanelBlockEntity
extends BlockEntity {
    public static final int MAX_DISPLAY_LINES = 8;
    @Nullable
    private BuildingId buildingId;
    private PanelType panelType = PanelType.VILLAGE_SUMMARY;
    @Nullable
    private UUID villageId;
    @Nullable
    private ResourceLocation cultureId;
    private int signIndex;
    private List<PanelContentGenerator.DisplayLine> displayLines = Collections.emptyList();

    public VillagePanelBlockEntity(BlockPos pos, BlockState state) {
        super((BlockEntityType)ModBlockEntities.VILLAGE_PANEL.get(), pos, state);
    }

    @Nullable
    public BuildingId getBuildingId() {
        return this.buildingId;
    }

    public void setBuildingId(@Nullable BuildingId buildingId) {
        this.buildingId = buildingId;
        this.setChanged();
    }

    public PanelType getPanelType() {
        return this.panelType;
    }

    public void setPanelType(PanelType panelType) {
        this.panelType = panelType;
        this.setChanged();
    }

    @Nullable
    public UUID getVillageId() {
        return this.villageId;
    }

    public void setVillageId(@Nullable UUID villageId) {
        this.villageId = villageId;
        this.setChanged();
    }

    @Nullable
    public ResourceLocation getCultureId() {
        return this.cultureId;
    }

    public void setCultureId(@Nullable ResourceLocation cultureId) {
        this.cultureId = cultureId;
        this.setChanged();
    }

    public int getSignIndex() {
        return this.signIndex;
    }

    public void setSignIndex(int signIndex) {
        this.signIndex = signIndex;
        this.setChanged();
    }

    public List<PanelContentGenerator.DisplayLine> getDisplayLines() {
        return this.displayLines;
    }

    public void updateDisplayLines(List<PanelContentGenerator.DisplayLine> lines) {
        this.displayLines = lines.size() > 8 ? new ArrayList<PanelContentGenerator.DisplayLine>(lines.subList(0, 8)) : new ArrayList<PanelContentGenerator.DisplayLine>(lines);
        this.setChanged();
    }

    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        if (this.buildingId != null) {
            tag.putUUID("building_id", this.buildingId.uuid());
        }
        tag.putString("panel_type", this.panelType.name());
        if (this.villageId != null) {
            tag.putUUID("village_id", this.villageId);
        }
        if (this.cultureId != null) {
            tag.putString("culture_id", this.cultureId.toString());
        }
        if (this.signIndex != 0) {
            tag.putInt("sign_index", this.signIndex);
        }
        if (!this.displayLines.isEmpty()) {
            ListTag linesTag = new ListTag();
            for (PanelContentGenerator.DisplayLine line : this.displayLines) {
                CompoundTag lineTag = new CompoundTag();
                lineTag.putString("text", line.text());
                lineTag.putString("left_icon", line.leftIcon());
                lineTag.putString("middle_icon", line.middleIcon());
                lineTag.putString("right_icon", line.rightIcon());
                lineTag.putString("left_column", line.leftColumn());
                lineTag.putString("right_column", line.rightColumn());
                lineTag.putBoolean("centered", line.centered());
                if (line.translatable()) {
                    lineTag.putBoolean("translatable", true);
                }
                if (line.nativePrefix() != null) {
                    lineTag.putString("native_prefix", line.nativePrefix());
                }
                linesTag.add((Object)lineTag);
            }
            tag.put("display_lines_v2", (Tag)linesTag);
        }
    }

    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        if (tag.hasUUID("building_id")) {
            this.buildingId = new BuildingId(tag.getUUID("building_id"));
        }
        if (tag.contains("panel_type")) {
            this.panelType = PanelType.fromName(tag.getString("panel_type"));
        }
        if (tag.hasUUID("village_id")) {
            this.villageId = tag.getUUID("village_id");
        }
        if (tag.contains("culture_id")) {
            this.cultureId = ResourceLocation.tryParse((String)tag.getString("culture_id"));
        }
        if (tag.contains("sign_index")) {
            this.signIndex = tag.getInt("sign_index");
        }
        if (tag.contains("display_lines_v2", 9)) {
            ListTag linesTag = tag.getList("display_lines_v2", 10);
            ArrayList<PanelContentGenerator.DisplayLine> loaded = new ArrayList<PanelContentGenerator.DisplayLine>(linesTag.size());
            for (int i = 0; i < linesTag.size(); ++i) {
                CompoundTag lineTag = linesTag.getCompound(i);
                String nativePrefix = lineTag.contains("native_prefix") ? lineTag.getString("native_prefix") : null;
                loaded.add(new PanelContentGenerator.DisplayLine(lineTag.getString("text"), lineTag.getString("left_icon"), lineTag.getString("middle_icon"), lineTag.getString("right_icon"), lineTag.getString("left_column"), lineTag.getString("right_column"), lineTag.getBoolean("centered"), lineTag.getBoolean("translatable"), ItemStack.EMPTY, ItemStack.EMPTY, nativePrefix));
            }
            this.displayLines = loaded;
        } else if (tag.contains("display_lines", 9)) {
            ListTag linesTag = tag.getList("display_lines", 8);
            ListTag iconsTag = tag.contains("display_icons", 9) ? tag.getList("display_icons", 8) : new ListTag();
            ArrayList<PanelContentGenerator.DisplayLine> loaded = new ArrayList<PanelContentGenerator.DisplayLine>(linesTag.size());
            for (int i = 0; i < linesTag.size(); ++i) {
                String text = linesTag.getString(i);
                String icon = i < iconsTag.size() ? iconsTag.getString(i) : "";
                boolean centered = i == 0;
                loaded.add(new PanelContentGenerator.DisplayLine(text, icon, "", "", "", "", centered));
            }
            this.displayLines = loaded;
        } else {
            this.displayLines = Collections.emptyList();
        }
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
}

