/*
 * Decompiled with CFR 0.150.
 * 
 * Could not load the following classes:
 *  net.minecraft.resources.ResourceLocation
 */
package org.millenaire.catalog;

import java.nio.file.Path;
import net.minecraft.resources.ResourceLocation;

public interface ShotSpec {
    public Path output();

    public record BuildingShot(ResourceLocation planSet, String variant, int level, int maxLevel, Path output) {
        public boolean isMaxLevel() {
            return this.level == this.maxLevel;
        }
    }

    public record VillagerShot(ResourceLocation villagerType, Path output) {
    }
}

