/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  net.minecraft.resources.ResourceLocation
 */
package org.millenaire.catalog;

import java.nio.file.Path;
import net.minecraft.resources.ResourceLocation;

public sealed interface ShotSpec {
    public Path output();

    public record BuildingShot(ResourceLocation planSet, String variant, int level, int maxLevel, Path output) implements ShotSpec
    {
        public boolean isMaxLevel() {
            return this.level == this.maxLevel;
        }
    }

    public record VillagerShot(ResourceLocation villagerType, Path output) implements ShotSpec
    {
    }
}

