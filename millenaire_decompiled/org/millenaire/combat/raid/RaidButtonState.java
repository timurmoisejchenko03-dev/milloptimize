/*
 * Decompiled with CFR 0.150.
 * 
 * Could not load the following classes:
 *  javax.annotation.Nullable
 */
package org.millenaire.combat.raid;

import javax.annotation.Nullable;

public final class RaidButtonState
extends Enum<RaidButtonState> {
    public static final /* enum */ RaidButtonState PLAN = new RaidButtonState();
    public static final /* enum */ RaidButtonState CANCEL = new RaidButtonState();
    public static final /* enum */ RaidButtonState IN_PROGRESS_HERE = new RaidButtonState();
    public static final /* enum */ RaidButtonState IN_PROGRESS_OTHER = new RaidButtonState();
    public static final /* enum */ RaidButtonState PLANNED_OTHER = new RaidButtonState();
    private static final /* synthetic */ RaidButtonState[] $VALUES;

    public static RaidButtonState[] values() {
        return (RaidButtonState[])$VALUES.clone();
    }

    public static RaidButtonState valueOf(String name) {
        return Enum.valueOf(RaidButtonState.class, name);
    }

    public static RaidButtonState compute(@Nullable String currentRaidTargetVillageId, boolean raidInProgress, String thisRelationVillageId) {
        if (currentRaidTargetVillageId == null) {
            return PLAN;
        }
        boolean targetIsThis = currentRaidTargetVillageId.equals(thisRelationVillageId);
        if (raidInProgress) {
            return targetIsThis ? IN_PROGRESS_HERE : IN_PROGRESS_OTHER;
        }
        return targetIsThis ? CANCEL : PLANNED_OTHER;
    }

    private static /* synthetic */ RaidButtonState[] $values() {
        return new RaidButtonState[]{PLAN, CANCEL, IN_PROGRESS_HERE, IN_PROGRESS_OTHER, PLANNED_OTHER};
    }

    static {
        $VALUES = RaidButtonState.$values();
    }
}

