/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  javax.annotation.Nullable
 */
package org.millenaire.village;

import javax.annotation.Nullable;
import org.millenaire.village.VillageEventType;

public record VillageEvent(long gameTime, VillageEventType type, String param1, @Nullable String param2) {
}

