/*
 * Decompiled with CFR 0.152.
 */
package org.millenaire.combat.raid;

public final class ChestLockDecision {
    private ChestLockDecision() {
    }

    public static Action decide(boolean currentlyLocked, int nbLiveDefenders) {
        if (currentlyLocked && nbLiveDefenders <= 0) {
            return Action.UNLOCK;
        }
        if (!currentlyLocked && nbLiveDefenders > 0) {
            return Action.LOCK;
        }
        return Action.NOOP;
    }

    public static enum Action {
        LOCK,
        UNLOCK,
        NOOP;

    }
}

