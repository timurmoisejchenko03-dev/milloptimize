/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  net.minecraft.resources.ResourceLocation
 */
package org.millenaire.catalog;

import java.nio.file.Path;
import net.minecraft.resources.ResourceLocation;

public final class CatalogPaths {
    private CatalogPaths() {
    }

    public static String flattenBuilding(ResourceLocation planSet, String variant, int level) {
        return planSet.getPath().replace("/", "__") + "__" + variant + "__L" + level;
    }

    public static String flattenVillager(ResourceLocation villagerType) {
        return villagerType.getPath().replace("/", "__");
    }

    public static Path buildingOutput(Path root, String culture, ResourceLocation planSet, String variant, int level) {
        return root.resolve("millenaire").resolve(culture).resolve("buildings").resolve(CatalogPaths.flattenBuilding(planSet, variant, level) + ".png");
    }

    public static Path villagerOutput(Path root, String culture, ResourceLocation villagerType) {
        return root.resolve("millenaire").resolve(culture).resolve("villagers").resolve(CatalogPaths.flattenVillager(villagerType) + ".png");
    }
}

