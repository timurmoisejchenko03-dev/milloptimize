/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  net.minecraft.server.level.ServerLevel
 */
package org.millenaire.goal;

import java.util.Optional;
import net.minecraft.server.level.ServerLevel;
import org.millenaire.building.BuildingId;
import org.millenaire.building.BuildingInstance;
import org.millenaire.building.BuildingInventory;
import org.millenaire.entity.MillVillager;
import org.millenaire.village.Village;

public record GoalContext(MillVillager villager, Village village, ServerLevel level, long dayTime, long gameTime) {
    public Optional<BuildingInstance> resolveHomeBuilding() {
        BuildingId homeId = this.villager().getHomeBuilding();
        if (homeId == null) {
            return Optional.empty();
        }
        return Optional.ofNullable(this.village().getBuilding(homeId));
    }

    public Optional<BuildingInventory> resolveHomeInventory() {
        return this.resolveHomeBuilding().map(BuildingInstance::getInventory);
    }
}

