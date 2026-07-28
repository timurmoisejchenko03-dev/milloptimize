/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  javax.annotation.Nullable
 */
package org.millenaire.combat.raid;

import javax.annotation.Nullable;

public enum RaidButtonState {
    PLAN,
    CANCEL,
    IN_PROGRESS_HERE,
    IN_PROGRESS_OTHER,
    PLANNED_OTHER;


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
}

