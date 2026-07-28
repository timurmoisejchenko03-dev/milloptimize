/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  net.minecraft.network.chat.Component
 */
package org.millenaire.goal;

import net.minecraft.network.chat.Component;
import org.millenaire.goal.TravelPhase;

public final class TaskLabels {
    private TaskLabels() {
    }

    public static TravelPhase phaseFor(boolean atDestination) {
        return atDestination ? TravelPhase.AT_DESTINATION : TravelPhase.TRAVELLING;
    }

    public static Component labelForPhase(boolean atDestination, String goalKey) {
        return Component.translatable((String)("goal.millenaire." + goalKey));
    }
}

