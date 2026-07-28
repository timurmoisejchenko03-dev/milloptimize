/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  javax.annotation.Nullable
 *  net.minecraft.core.BlockPos
 *  net.minecraft.resources.ResourceLocation
 */
package org.millenaire.building;

import java.util.List;
import javax.annotation.Nullable;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import org.millenaire.building.SpecialPoint;

public record BuildingPlan(ResourceLocation id, ResourceLocation culture, String nbtPath, int width, int height, int depth, int groundLevel, int buildingOrientation, BlockPos entryOffset, List<String> tags, String constructionOrder, String terrainPolicy, List<SpecialPoint> specialPoints, @Nullable String shopId) {
    public boolean hasTag(String tag) {
        return this.tags.contains(tag);
    }

    public List<SpecialPoint> getPointsByType(String type) {
        return this.specialPoints.stream().filter(p -> p.isType(type)).toList();
    }
}

