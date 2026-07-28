/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  net.minecraft.core.HolderLookup$Provider
 *  net.minecraft.nbt.CompoundTag
 *  net.minecraft.nbt.ListTag
 *  net.minecraft.nbt.StringTag
 *  net.minecraft.nbt.Tag
 *  net.minecraft.server.level.ServerLevel
 *  net.minecraft.world.level.saveddata.SavedData
 *  net.minecraft.world.level.saveddata.SavedData$Factory
 */
package org.millenaire.net;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.nbt.Tag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.saveddata.SavedData;

public final class DonorStatusData
extends SavedData {
    private static final String DATA_NAME = "millenaire_donor_status";
    private final Set<UUID> donors = new HashSet<UUID>();

    public static DonorStatusData get(ServerLevel level) {
        return (DonorStatusData)level.getServer().overworld().getDataStorage().computeIfAbsent(new SavedData.Factory(DonorStatusData::new, DonorStatusData::load), DATA_NAME);
    }

    public boolean isDonor(UUID player) {
        return this.donors.contains(player);
    }

    public void setDonor(UUID player, boolean isDonor) {
        boolean changed;
        boolean bl = changed = isDonor ? this.donors.add(player) : this.donors.remove(player);
        if (changed) {
            this.setDirty();
        }
    }

    public CompoundTag save(CompoundTag tag, HolderLookup.Provider registries) {
        ListTag list = new ListTag();
        for (UUID id : this.donors) {
            list.add((Object)StringTag.valueOf((String)id.toString()));
        }
        tag.put("donors", (Tag)list);
        return tag;
    }

    public static DonorStatusData load(CompoundTag tag, HolderLookup.Provider registries) {
        DonorStatusData data = new DonorStatusData();
        if (tag.contains("donors")) {
            ListTag list = tag.getList("donors", 8);
            for (int i = 0; i < list.size(); ++i) {
                try {
                    data.donors.add(UUID.fromString(list.getString(i)));
                    continue;
                }
                catch (IllegalArgumentException illegalArgumentException) {
                    // empty catch block
                }
            }
        }
        return data;
    }
}

