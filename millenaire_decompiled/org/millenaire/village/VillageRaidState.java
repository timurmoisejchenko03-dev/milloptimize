/*
 * Decompiled with CFR 0.150.
 * 
 * Could not load the following classes:
 *  javax.annotation.Nullable
 */
package org.millenaire.village;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import javax.annotation.Nullable;
import org.millenaire.village.VillageId;

final class VillageRaidState {
    private final Runnable markDirty;
    @Nullable
    private VillageId raidTarget;
    private long raidPlanningStart;
    private long raidStart;
    private boolean underAttack;
    private final List<String> raidsPerformed = new ArrayList<String>();
    private final List<String> raidsSuffered = new ArrayList<String>();

    VillageRaidState(Runnable markDirty) {
        this.markDirty = markDirty;
    }

    @Nullable
    VillageId getRaidTarget() {
        return this.raidTarget;
    }

    void setRaidTarget(@Nullable VillageId target) {
        this.raidTarget = target;
        this.markDirty.run();
    }

    long getRaidPlanningStart() {
        return this.raidPlanningStart;
    }

    void setRaidPlanningStart(long t) {
        this.raidPlanningStart = t;
        this.markDirty.run();
    }

    long getRaidStart() {
        return this.raidStart;
    }

    void setRaidStart(long t) {
        this.raidStart = t;
        this.markDirty.run();
    }

    boolean isUnderAttack() {
        return this.underAttack;
    }

    void setUnderAttack(boolean v) {
        this.underAttack = v;
        this.markDirty.run();
    }

    List<String> getRaidsPerformed() {
        return Collections.unmodifiableList(this.raidsPerformed);
    }

    void addRaidsPerformed(String entry) {
        this.raidsPerformed.add(entry);
        this.markDirty.run();
    }

    List<String> getRaidsSuffered() {
        return Collections.unmodifiableList(this.raidsSuffered);
    }

    void addRaidsSuffered(String entry) {
        this.raidsSuffered.add(entry);
        this.markDirty.run();
    }

    void loadRaidsPerformed(List<String> entries) {
        this.raidsPerformed.clear();
        this.raidsPerformed.addAll(entries);
    }

    void loadRaidsSuffered(List<String> entries) {
        this.raidsSuffered.clear();
        this.raidsSuffered.addAll(entries);
    }

    void clearRaid() {
        this.raidTarget = null;
        this.raidPlanningStart = 0L;
        this.raidStart = 0L;
        this.markDirty.run();
    }

    boolean isRaidActiveOrSuffering() {
        return this.raidTarget != null || this.underAttack;
    }
}

