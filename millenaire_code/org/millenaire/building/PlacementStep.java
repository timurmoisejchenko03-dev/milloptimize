/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  net.minecraft.core.BlockPos
 *  net.minecraft.core.HolderLookup
 *  net.minecraft.nbt.CompoundTag
 *  net.minecraft.nbt.NbtUtils
 *  net.minecraft.nbt.Tag
 *  net.minecraft.world.level.block.Block
 *  net.minecraft.world.level.block.state.BlockState
 */
package org.millenaire.building;

import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.nbt.Tag;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import org.millenaire.building.PlacementPhase;

public record PlacementStep(BlockPos relativePos, BlockState blockState, PlacementPhase phase, boolean guessChestFacing, boolean guessTorchFacing, boolean guessFurnaceFacing) {
    public PlacementStep(BlockPos relativePos, BlockState blockState) {
        this(relativePos, blockState, PlacementPhase.STRUCTURE, false, false, false);
    }

    public PlacementStep(BlockPos relativePos, BlockState blockState, PlacementPhase phase) {
        this(relativePos, blockState, phase, false, false, false);
    }

    public PlacementStep(BlockPos relativePos, BlockState blockState, PlacementPhase phase, boolean guessChestFacing) {
        this(relativePos, blockState, phase, guessChestFacing, false, false);
    }

    public PlacementStep(BlockPos relativePos, BlockState blockState, PlacementPhase phase, boolean guessChestFacing, boolean guessTorchFacing) {
        this(relativePos, blockState, phase, guessChestFacing, guessTorchFacing, false);
    }

    public CompoundTag save() {
        CompoundTag tag = new CompoundTag();
        tag.putIntArray("pos", new int[]{this.relativePos.getX(), this.relativePos.getY(), this.relativePos.getZ()});
        tag.put("state", (Tag)NbtUtils.writeBlockState((BlockState)this.blockState));
        tag.putByte("phase", (byte)this.phase.ordinal());
        if (this.guessChestFacing) {
            tag.putBoolean("chest", true);
        }
        if (this.guessTorchFacing) {
            tag.putBoolean("torch", true);
        }
        if (this.guessFurnaceFacing) {
            tag.putBoolean("furnace", true);
        }
        return tag;
    }

    public static PlacementStep load(CompoundTag tag, HolderLookup<Block> blockLookup) {
        int[] pos = tag.getIntArray("pos");
        BlockPos relPos = new BlockPos(pos[0], pos[1], pos[2]);
        BlockState state = NbtUtils.readBlockState(blockLookup, (CompoundTag)tag.getCompound("state"));
        PlacementPhase ph = PlacementPhase.values()[tag.getByte("phase")];
        boolean chest = tag.getBoolean("chest");
        boolean torch = tag.getBoolean("torch");
        boolean furnace = tag.getBoolean("furnace");
        return new PlacementStep(relPos, state, ph, chest, torch, furnace);
    }
}

