/*
 * Decompiled with CFR 0.150.
 * 
 * Could not load the following classes:
 *  net.minecraft.nbt.CompoundTag
 */
package org.millenaire.quest;

import java.util.UUID;
import net.minecraft.nbt.CompoundTag;

public class QuestInstanceVillager {
    private final UUID villagerId;
    private final UUID villageId;

    public QuestInstanceVillager(UUID villagerId, UUID villageId) {
        this.villagerId = villagerId;
        this.villageId = villageId;
    }

    public UUID getVillagerId() {
        return this.villagerId;
    }

    public UUID getVillageId() {
        return this.villageId;
    }

    public CompoundTag save() {
        CompoundTag tag = new CompoundTag();
        tag.putUUID("villagerId", this.villagerId);
        tag.putUUID("villageId", this.villageId);
        return tag;
    }

    public static QuestInstanceVillager load(CompoundTag tag) {
        UUID villagerId = tag.getUUID("villagerId");
        UUID villageId = tag.getUUID("villageId");
        return new QuestInstanceVillager(villagerId, villageId);
    }
}

