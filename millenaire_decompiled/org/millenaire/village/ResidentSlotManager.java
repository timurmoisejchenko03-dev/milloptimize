/*
 * Decompiled with CFR 0.150.
 * 
 * Could not load the following classes:
 *  javax.annotation.Nullable
 *  net.minecraft.server.level.ServerLevel
 *  net.minecraft.world.entity.Entity
 */
package org.millenaire.village;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import javax.annotation.Nullable;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import org.millenaire.building.BuildingId;
import org.millenaire.building.BuildingInstance;
import org.millenaire.building.BuildingPlanSet;
import org.millenaire.culture.Gender;
import org.millenaire.culture.ModCultures;
import org.millenaire.culture.VillagerType;
import org.millenaire.village.Village;
import org.millenaire.village.VillagerRecord;

public class ResidentSlotManager {
    private final Village village;
    private final Map<SlotKey, UUID> reservedSlots = new HashMap<SlotKey, UUID>();

    public ResidentSlotManager(Village village) {
        this.village = village;
    }

    public int countAdultsInBuilding(BuildingId buildingId, Gender gender) {
        int count = 0;
        for (VillagerRecord record : this.village.getVillagerRecords().values()) {
            VillagerType vType;
            BuildingId home = record.getHomeBuilding();
            if (home == null || !home.equals(buildingId) || (vType = ModCultures.getVillagerType(record.getVillagerTypeId())) == null || vType.isChild() || vType.gender() != gender) continue;
            ++count;
        }
        return count;
    }

    public List<String> getFreeSlots(BuildingId buildingId, Gender gender) {
        BuildingInstance building = this.village.getBuilding(buildingId);
        if (building == null || building.getPlanSetId() == null) {
            return List.of();
        }
        BuildingPlanSet planSet = ModCultures.getBuildingPlanSet(building.getPlanSetId());
        if (planSet == null) {
            return List.of();
        }
        List<String> expectedResidents = gender == Gender.MALE ? planSet.maleResidents() : planSet.femaleResidents();
        int currentAdults = this.countAdultsInBuilding(buildingId, gender);
        int reservedCount = 0;
        for (Map.Entry<SlotKey, UUID> entry : this.reservedSlots.entrySet()) {
            SlotKey key = entry.getKey();
            if (!key.buildingId().equals(buildingId) || key.gender() != gender) continue;
            ++reservedCount;
        }
        int occupied = currentAdults + reservedCount;
        if (occupied >= expectedResidents.size()) {
            return List.of();
        }
        ArrayList<String> freeSlots = new ArrayList<String>();
        for (int i = occupied; i < expectedResidents.size(); ++i) {
            freeSlots.add(expectedResidents.get(i));
        }
        return freeSlots;
    }

    public boolean hasFreeSlot(BuildingId buildingId, Gender gender) {
        return !this.getFreeSlots(buildingId, gender).isEmpty();
    }

    @Nullable
    public String reserveSlot(BuildingId buildingId, Gender gender, UUID teenagerId) {
        List<String> freeSlots = this.getFreeSlots(buildingId, gender);
        if (freeSlots.isEmpty()) {
            return null;
        }
        BuildingInstance building = this.village.getBuilding(buildingId);
        if (building == null || building.getPlanSetId() == null) {
            return null;
        }
        BuildingPlanSet planSet = ModCultures.getBuildingPlanSet(building.getPlanSetId());
        if (planSet == null) {
            return null;
        }
        List<String> expectedResidents = gender == Gender.MALE ? planSet.maleResidents() : planSet.femaleResidents();
        int slotIndex = expectedResidents.size() - freeSlots.size();
        SlotKey key = new SlotKey(building.getId(), gender, slotIndex);
        this.reservedSlots.put(key, teenagerId);
        this.village.markDirty();
        return freeSlots.get(0);
    }

    public void releaseSlot(SlotKey key) {
        this.reservedSlots.remove(key);
        this.village.markDirty();
    }

    public void releaseAllSlots(UUID teenagerId) {
        this.reservedSlots.entrySet().removeIf(entry -> ((UUID)entry.getValue()).equals(teenagerId));
        this.village.markDirty();
    }

    public void cleanupStaleReservations(ServerLevel level) {
        this.reservedSlots.entrySet().removeIf(entry -> {
            Entity entity = level.getEntity((UUID)entry.getValue());
            return entity == null || !entity.isAlive();
        });
    }

    public int countChildren() {
        int count = 0;
        for (VillagerRecord record : this.village.getVillagerRecords().values()) {
            VillagerType vType = ModCultures.getVillagerType(record.getVillagerTypeId());
            if (vType == null || !vType.isChild()) continue;
            ++count;
        }
        return count;
    }

    public int countChildrenInBuilding(BuildingId buildingId) {
        int count = 0;
        for (VillagerRecord record : this.village.getVillagerRecords().values()) {
            VillagerType vType;
            BuildingId home = record.getHomeBuilding();
            if (home == null || !home.equals(buildingId) || (vType = ModCultures.getVillagerType(record.getVillagerTypeId())) == null || !vType.isChild()) continue;
            ++count;
        }
        return count;
    }

    public boolean hasAnyFreeSlot(Gender gender) {
        for (BuildingInstance b : this.village.getBuildings()) {
            if (!b.isOperational() || !this.hasFreeSlot(b.getId(), gender)) continue;
            return true;
        }
        return false;
    }

    public record SlotKey(BuildingId buildingId, Gender gender, int slotIndex) {
    }
}

