/*
 * Decompiled with CFR 0.150.
 * 
 * Could not load the following classes:
 *  com.mojang.logging.LogUtils
 *  javax.annotation.Nullable
 *  net.minecraft.core.BlockPos
 *  net.minecraft.nbt.CompoundTag
 *  net.minecraft.nbt.ListTag
 *  net.minecraft.nbt.StringTag
 *  net.minecraft.nbt.Tag
 *  net.minecraft.resources.ResourceLocation
 *  net.minecraft.world.item.Item
 *  org.slf4j.Logger
 */
package org.millenaire.village;

import com.mojang.logging.LogUtils;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import javax.annotation.Nullable;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import org.millenaire.building.BuildingId;
import org.millenaire.combat.MilitaryStrength;
import org.millenaire.culture.ModCultures;
import org.millenaire.culture.VillagerType;
import org.millenaire.entity.MillVillager;
import org.millenaire.entity.ModelType;
import org.millenaire.entity.VillagerAppearanceFactory;
import org.millenaire.entity.VillagerIdentity;
import org.millenaire.entity.VillagerInventory;
import org.millenaire.village.VillageId;
import org.slf4j.Logger;

public class VillagerRecord {
    private static final Logger LOGGER = LogUtils.getLogger();
    private UUID uuid;
    private ResourceLocation villagerTypeId;
    @Nullable
    private BuildingId homeBuilding;
    private VillagerIdentity identity = new VillagerIdentity();
    private final VillagerInventory inventory = new VillagerInventory();
    private boolean killed;
    private long lastRespawnTick;
    @Nullable
    private BlockPos lastKnownPos;
    private int visitorNbNights;
    private boolean awayRaiding;
    private boolean raidingVillage;
    @Nullable
    private VillageId originalVillageId;
    private long raiderSpawn;
    private boolean awayHired;
    @Nullable
    private UUID hiredBy;
    private long hiredUntil;
    private boolean aggressiveStance;
    private final List<String> questTags = new ArrayList<String>();

    public VillagerRecord(UUID uuid, ResourceLocation villagerTypeId, @Nullable BuildingId homeBuilding) {
        this.uuid = uuid;
        this.villagerTypeId = villagerTypeId;
        this.homeBuilding = homeBuilding;
    }

    public UUID getUuid() {
        return this.uuid;
    }

    public void setUuid(UUID uuid) {
        this.uuid = uuid;
    }

    @Nullable
    public ResourceLocation getVillagerTypeId() {
        return this.villagerTypeId;
    }

    public void setVillagerTypeId(ResourceLocation villagerTypeId) {
        this.villagerTypeId = villagerTypeId;
    }

    @Nullable
    public BuildingId getHomeBuilding() {
        return this.homeBuilding;
    }

    public void setHomeBuilding(@Nullable BuildingId homeBuilding) {
        this.homeBuilding = homeBuilding;
    }

    public VillagerIdentity getIdentity() {
        return this.identity;
    }

    public String getFirstName() {
        return this.identity.getFirstName();
    }

    public String getFamilyName() {
        return this.identity.getFamilyName();
    }

    public String getRoleName() {
        return this.identity.getRoleName();
    }

    public String getFathersName() {
        return this.identity.getFathersName();
    }

    public String getMothersName() {
        return this.identity.getMothersName();
    }

    public String getSpousesName() {
        return this.identity.getSpousesName();
    }

    public String getMaidenName() {
        return this.identity.getMaidenName();
    }

    @Nullable
    public ResourceLocation getTexture() {
        return this.identity.getTexture();
    }

    @Nullable
    public ResourceLocation getClothTexture0() {
        return this.identity.getClothTexture0();
    }

    @Nullable
    public ResourceLocation getClothTexture1() {
        return this.identity.getClothTexture1();
    }

    public float getVillagerScale() {
        return this.identity.getVillagerScale();
    }

    public ModelType getModelType() {
        return this.identity.getModelType();
    }

    public int getChildSize() {
        return this.identity.getChildSize();
    }

    public VillagerInventory getInventory() {
        return this.inventory;
    }

    public boolean isKilled() {
        return this.killed;
    }

    public void setKilled(boolean killed) {
        this.killed = killed;
    }

    public long getLastRespawnTick() {
        return this.lastRespawnTick;
    }

    public void setLastRespawnTick(long lastRespawnTick) {
        this.lastRespawnTick = lastRespawnTick;
    }

    @Nullable
    public BlockPos getLastKnownPos() {
        return this.lastKnownPos;
    }

    public int getVisitorNbNights() {
        return this.visitorNbNights;
    }

    public void setVisitorNbNights(int visitorNbNights) {
        this.visitorNbNights = visitorNbNights;
    }

    public boolean isAwayRaiding() {
        return this.awayRaiding;
    }

    public void setAwayRaiding(boolean awayRaiding) {
        this.awayRaiding = awayRaiding;
    }

    public boolean isRaidingVillage() {
        return this.raidingVillage;
    }

    public void setRaidingVillage(boolean raidingVillage) {
        this.raidingVillage = raidingVillage;
    }

    @Nullable
    public VillageId getOriginalVillageId() {
        return this.originalVillageId;
    }

    public void setOriginalVillageId(@Nullable VillageId originalVillageId) {
        this.originalVillageId = originalVillageId;
    }

    public long getRaiderSpawn() {
        return this.raiderSpawn;
    }

    public void setRaiderSpawn(long raiderSpawn) {
        this.raiderSpawn = raiderSpawn;
    }

    public boolean isAwayHired() {
        return this.awayHired;
    }

    @Nullable
    public UUID getHiredBy() {
        return this.hiredBy;
    }

    public long getHiredUntil() {
        return this.hiredUntil;
    }

    public boolean isAggressiveStance() {
        return this.aggressiveStance;
    }

    public void setAggressiveStance(boolean aggressive) {
        this.aggressiveStance = aggressive;
    }

    public void setHireState(@Nullable UUID owner, long until) {
        this.hiredBy = owner;
        this.hiredUntil = until;
        boolean bl = this.awayHired = owner != null;
        if (owner == null) {
            this.aggressiveStance = false;
        }
    }

    public int getMilitaryStrength() {
        if (this.villagerTypeId == null) {
            return 0;
        }
        VillagerType type = ModCultures.getVillagerType(this.villagerTypeId);
        if (type == null) {
            return 0;
        }
        MilitaryStrength.InventoryScan scan = MilitaryStrength.scan(this.inventory);
        boolean archerWithBow = type.isArcher() && scan.hasBow();
        return MilitaryStrength.compute((int)type.maxHealth(), type.baseAttackStrength(), scan.bestMeleeWeaponDamage(), archerWithBow, scan.totalArmorValue());
    }

    public List<String> getQuestTags() {
        return Collections.unmodifiableList(this.questTags);
    }

    public void addQuestTag(String tag) {
        if (!this.questTags.contains(tag)) {
            this.questTags.add(tag);
        }
    }

    public void removeQuestTag(String tag) {
        this.questTags.remove(tag);
    }

    public boolean hasQuestTag(String tag) {
        return this.questTags.contains(tag);
    }

    public void updateFromEntity(MillVillager villager) {
        this.villagerTypeId = villager.getVillagerTypeId();
        this.homeBuilding = villager.getHomeBuilding();
        this.identity = new VillagerIdentity(villager.getIdentity());
        this.lastKnownPos = villager.blockPosition();
        this.visitorNbNights = villager.getVisitorNbNights();
        this.setHireState(villager.getHiredBy(), villager.getHiredUntil());
        this.setAggressiveStance(villager.isAggressiveStance());
        this.inventory.clear();
        for (Map.Entry<Item, Integer> entry : villager.getInventory().getAll().entrySet()) {
            this.inventory.add(entry.getKey(), entry.getValue());
        }
    }

    public void applyToEntity(MillVillager villager) {
        String[] arrstring;
        Object vType;
        villager.setVillagerTypeId(this.villagerTypeId);
        villager.setHomeBuilding(this.homeBuilding);
        if (VillagerAppearanceFactory.isCorruptedName(this.identity.getFirstName()) && this.villagerTypeId != null && (vType = ModCultures.getVillagerType(this.villagerTypeId)) != null && !VillagerAppearanceFactory.isCorruptedName((arrstring = VillagerAppearanceFactory.generateName((VillagerType)vType))[0])) {
            this.identity.setFirstName(arrstring[0]);
            this.identity.setFamilyName(arrstring[1]);
        }
        villager.initAppearance(this.identity.getModelType(), this.identity.getTexture(), this.identity.getClothTexture0(), this.identity.getClothTexture1(), this.identity.getVillagerScale(), this.identity.getFirstName(), this.identity.getFamilyName(), this.identity.getRoleName());
        villager.setFathersName(this.identity.getFathersName());
        villager.setMothersName(this.identity.getMothersName());
        villager.setSpousesName(this.identity.getSpousesName());
        villager.setMaidenName(this.identity.getMaidenName());
        if (this.identity.getChildSize() >= 0) {
            villager.setChildSize(this.identity.getChildSize());
        }
        villager.getInventory().clear();
        for (Map.Entry entry : this.inventory.getAll().entrySet()) {
            villager.getInventory().add((Item)entry.getKey(), (Integer)entry.getValue());
        }
        villager.getIdentity().setClothName(this.identity.getClothName());
        vType = ModCultures.getVillagerType(this.villagerTypeId);
        if (vType != null) {
            villager.updateClothTextures((VillagerType)vType);
        }
        villager.setVisitorNbNights(this.visitorNbNights);
        villager.setHireState(this.hiredBy, this.hiredUntil);
        villager.setAggressiveStance(this.aggressiveStance);
    }

    public void copyAppearanceAndInventoryFrom(VillagerRecord source) {
        this.identity = new VillagerIdentity(source.identity);
        this.inventory.clear();
        for (Map.Entry<Item, Integer> entry : source.inventory.getAll().entrySet()) {
            this.inventory.add(entry.getKey(), entry.getValue());
        }
    }

    public void save(CompoundTag tag) {
        tag.putUUID("uuid", this.uuid);
        tag.putString("type", this.villagerTypeId.toString());
        if (this.homeBuilding != null) {
            tag.putUUID("home", this.homeBuilding.uuid());
        }
        this.identity.save(tag);
        tag.putBoolean("killed", this.killed);
        tag.putLong("lastRespawnTick", this.lastRespawnTick);
        if (this.lastKnownPos != null) {
            tag.putIntArray("last_known_pos", new int[]{this.lastKnownPos.getX(), this.lastKnownPos.getY(), this.lastKnownPos.getZ()});
        }
        tag.putInt("visitor_nb_nights", this.visitorNbNights);
        if (this.awayRaiding) {
            tag.putBoolean("away_raiding", true);
        }
        if (this.raidingVillage) {
            tag.putBoolean("raiding_village", true);
        }
        if (this.originalVillageId != null) {
            tag.putUUID("original_village_id", this.originalVillageId.uuid());
        }
        if (this.raiderSpawn != 0L) {
            tag.putLong("raider_spawn", this.raiderSpawn);
        }
        tag.putBoolean("away_hired", this.awayHired);
        tag.putBoolean("hire_stance", this.aggressiveStance);
        if (this.hiredBy != null) {
            tag.putUUID("hired_by", this.hiredBy);
            tag.putLong("hired_until", this.hiredUntil);
        }
        CompoundTag invTag = new CompoundTag();
        this.inventory.save(invTag);
        tag.put("inventory", (Tag)invTag);
        if (!this.questTags.isEmpty()) {
            ListTag tagList = new ListTag();
            for (String qt : this.questTags) {
                tagList.add((Object)StringTag.valueOf((String)qt));
            }
            tag.put("questTags", (Tag)tagList);
        }
    }

    @Nullable
    public static VillagerRecord load(CompoundTag tag) {
        try {
            int[] pos;
            UUID uuid = tag.getUUID("uuid");
            ResourceLocation typeId = ResourceLocation.parse((String)tag.getString("type"));
            BuildingId home = tag.hasUUID("home") ? new BuildingId(tag.getUUID("home")) : null;
            VillagerRecord record = new VillagerRecord(uuid, typeId, home);
            record.identity = VillagerIdentity.load(tag);
            record.killed = tag.getBoolean("killed");
            record.lastRespawnTick = tag.getLong("lastRespawnTick");
            if (tag.contains("last_known_pos") && (pos = tag.getIntArray("last_known_pos")).length == 3) {
                record.lastKnownPos = new BlockPos(pos[0], pos[1], pos[2]);
            }
            record.visitorNbNights = tag.getInt("visitor_nb_nights");
            record.awayRaiding = tag.getBoolean("away_raiding");
            record.raidingVillage = tag.getBoolean("raiding_village");
            if (tag.hasUUID("original_village_id")) {
                record.originalVillageId = new VillageId(tag.getUUID("original_village_id"));
            }
            record.raiderSpawn = tag.getLong("raider_spawn");
            record.awayHired = tag.getBoolean("away_hired");
            record.aggressiveStance = tag.getBoolean("hire_stance");
            if (tag.hasUUID("hired_by")) {
                record.hiredBy = tag.getUUID("hired_by");
                record.hiredUntil = tag.getLong("hired_until");
            }
            if (tag.contains("inventory")) {
                record.inventory.load(tag.getCompound("inventory"));
            }
            if (tag.contains("questTags")) {
                ListTag tagList = tag.getList("questTags", 8);
                for (int i = 0; i < tagList.size(); ++i) {
                    record.questTags.add(tagList.getString(i));
                }
            }
            return record;
        }
        catch (Exception e) {
            LOGGER.error("Unable to load VillagerRecord from NBT: {}", (Object)e.getMessage());
            return null;
        }
    }
}

