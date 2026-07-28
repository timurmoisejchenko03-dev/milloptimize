/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  javax.annotation.Nullable
 *  net.minecraft.nbt.CompoundTag
 *  net.minecraft.nbt.ListTag
 */
package org.millenaire.building;

import javax.annotation.Nullable;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;

public final class NbtPaletteHelper {
    private NbtPaletteHelper() {
    }

    @Nullable
    public static ListTag resolvePaletteTag(CompoundTag nbt) {
        ListTag palettesTag;
        if (nbt.contains("palette", 9)) {
            return nbt.getList("palette", 10);
        }
        if (nbt.contains("palettes", 9) && !(palettesTag = nbt.getList("palettes", 9)).isEmpty()) {
            return palettesTag.getList(0);
        }
        return null;
    }
}

