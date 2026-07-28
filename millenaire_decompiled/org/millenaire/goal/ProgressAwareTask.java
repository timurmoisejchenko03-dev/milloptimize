/*
 * Decompiled with CFR 0.150.
 */
package org.millenaire.goal;

import org.millenaire.goal.VillagerTask;

public abstract class ProgressAwareTask
implements VillagerTask {
    private boolean progressMade;

    @Override
    public void reportProgress() {
        this.progressMade = true;
    }

    @Override
    public boolean consumeProgress() {
        boolean had = this.progressMade;
        this.progressMade = false;
        return had;
    }
}

