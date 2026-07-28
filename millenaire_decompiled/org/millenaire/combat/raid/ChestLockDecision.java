/*
 * Decompiled with CFR 0.150.
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

    public static final class Action
    extends Enum<Action> {
        public static final /* enum */ Action LOCK = new Action();
        public static final /* enum */ Action UNLOCK = new Action();
        public static final /* enum */ Action NOOP = new Action();
        private static final /* synthetic */ Action[] $VALUES;

        public static Action[] values() {
            return (Action[])$VALUES.clone();
        }

        public static Action valueOf(String name) {
            return Enum.valueOf(Action.class, name);
        }

        private static /* synthetic */ Action[] $values() {
            return new Action[]{LOCK, UNLOCK, NOOP};
        }

        static {
            $VALUES = Action.$values();
        }
    }
}

