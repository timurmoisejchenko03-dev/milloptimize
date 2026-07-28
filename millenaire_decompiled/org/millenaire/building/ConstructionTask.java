/*
 * Decompiled with CFR 0.150.
 * 
 * Could not load the following classes:
 *  javax.annotation.Nullable
 *  net.minecraft.core.BlockPos
 *  net.minecraft.core.HolderLookup
 *  net.minecraft.core.Vec3i
 *  net.minecraft.nbt.CompoundTag
 *  net.minecraft.nbt.ListTag
 *  net.minecraft.nbt.Tag
 *  net.minecraft.world.level.block.Block
 */
package org.millenaire.building;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import javax.annotation.Nullable;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.Vec3i;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.world.level.block.Block;
import org.millenaire.block.mock.MockBlock;
import org.millenaire.building.PlacementStep;

public class ConstructionTask {
    private final List<PlacementStep> steps;
    private int nextStepIndex;
    @Nullable
    private UUID reservedBuilder;
    private int reservationAge;
    private int failedAttempts;
    private static final int MAX_FAILED_ATTEMPTS = 3;

    public ConstructionTask(List<PlacementStep> steps, int nextStepIndex) {
        this.steps = steps;
        this.nextStepIndex = nextStepIndex;
    }

    public boolean isComplete() {
        return this.nextStepIndex >= this.steps.size();
    }

    @Nullable
    public PlacementStep currentStep() {
        if (this.isComplete()) {
            return null;
        }
        return this.steps.get(this.nextStepIndex);
    }

    public void advance() {
        if (!this.isComplete()) {
            ++this.nextStepIndex;
            if (this.failedAttempts > 0) {
                this.failedAttempts = 0;
            }
        }
    }

    public float progress() {
        if (this.steps.isEmpty()) {
            return 1.0f;
        }
        return (float)this.nextStepIndex / (float)this.steps.size();
    }

    public int getNextStepIndex() {
        return this.nextStepIndex;
    }

    public int totalSteps() {
        return this.steps.size();
    }

    public List<BlockPos> upcomingAbsolutePositions(BlockPos origin, int count) {
        if (count <= 0 || this.isComplete()) {
            return List.of();
        }
        int end = Math.min(this.nextStepIndex + count, this.steps.size());
        ArrayList<BlockPos> out = new ArrayList<BlockPos>(end - this.nextStepIndex);
        for (int i = this.nextStepIndex; i < end; ++i) {
            out.add(origin.offset((Vec3i)this.steps.get(i).relativePos()));
        }
        return out;
    }

    public boolean isReserved() {
        return this.reservedBuilder != null;
    }

    @Nullable
    public UUID getReservedBuilder() {
        return this.reservedBuilder;
    }

    public void reserve(UUID builder) {
        this.reservedBuilder = builder;
        this.reservationAge = 0;
    }

    public void releaseReservation() {
        this.reservedBuilder = null;
        this.reservationAge = 0;
    }

    public void tickReservation() {
        if (this.isReserved()) {
            ++this.reservationAge;
        }
    }

    public void resetReservationAge() {
        this.reservationAge = 0;
    }

    public boolean isReservationExpired(int timeoutTicks) {
        return this.isReserved() && this.reservationAge >= timeoutTicks;
    }

    public void incrementFailedAttempts() {
        ++this.failedAttempts;
    }

    public boolean isBlocked() {
        return this.failedAttempts >= 3;
    }

    public int getFailedAttempts() {
        return this.failedAttempts;
    }

    public CompoundTag save() {
        CompoundTag tag = new CompoundTag();
        tag.putInt("cursor", this.nextStepIndex);
        if (this.failedAttempts > 0) {
            tag.putInt("failedAttempts", this.failedAttempts);
        }
        if (this.reservedBuilder != null) {
            tag.putUUID("reservedBuilder", this.reservedBuilder);
        }
        ListTag stepsList = new ListTag();
        for (PlacementStep step : this.steps) {
            stepsList.add((Object)step.save());
        }
        tag.put("steps", (Tag)stepsList);
        return tag;
    }

    public static ConstructionTask load(CompoundTag tag, HolderLookup<Block> blockLookup) {
        int cursor = tag.getInt("cursor");
        ListTag stepsList = tag.getList("steps", 10);
        ArrayList<PlacementStep> steps = new ArrayList<PlacementStep>(stepsList.size());
        for (int i = 0; i < stepsList.size(); ++i) {
            PlacementStep step = PlacementStep.load(stepsList.getCompound(i), blockLookup);
            if (step.blockState().getBlock() instanceof MockBlock) continue;
            steps.add(step);
        }
        cursor = Math.min(cursor, steps.size());
        ConstructionTask task = new ConstructionTask(steps, cursor);
        if (tag.contains("failedAttempts")) {
            task.failedAttempts = tag.getInt("failedAttempts");
        }
        if (tag.hasUUID("reservedBuilder")) {
            task.reservedBuilder = tag.getUUID("reservedBuilder");
        }
        return task;
    }
}

