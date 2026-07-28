/*
 * Decompiled with CFR 0.150.
 * 
 * Could not load the following classes:
 *  com.mojang.logging.LogUtils
 *  net.minecraft.core.BlockPos
 *  net.minecraft.core.Holder
 *  net.minecraft.core.HolderLookup$RegistryLookup
 *  net.minecraft.core.registries.Registries
 *  net.minecraft.resources.ResourceKey
 *  net.minecraft.server.level.ServerLevel
 *  net.minecraft.tags.StructureTags
 *  net.minecraft.world.level.StructureManager
 *  net.minecraft.world.level.levelgen.structure.BuiltinStructures
 *  net.minecraft.world.level.levelgen.structure.Structure
 *  net.minecraft.world.level.levelgen.structure.StructureStart
 *  org.slf4j.Logger
 */
package org.millenaire.world;

import com.mojang.logging.LogUtils;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.StructureTags;
import net.minecraft.world.level.StructureManager;
import net.minecraft.world.level.levelgen.structure.BuiltinStructures;
import net.minecraft.world.level.levelgen.structure.Structure;
import net.minecraft.world.level.levelgen.structure.StructureStart;
import org.slf4j.Logger;

public final class StructureAvoidance {
    private static final Logger LOGGER = LogUtils.getLogger();
    static final int SAFETY_MARGIN = 3;
    private static final int SCAN_STEP = 8;
    private static final List<ResourceKey<Structure>> AVOIDED_STRUCTURE_KEYS = List.of(BuiltinStructures.PILLAGER_OUTPOST, BuiltinStructures.DESERT_PYRAMID, BuiltinStructures.JUNGLE_TEMPLE, BuiltinStructures.SWAMP_HUT, BuiltinStructures.WOODLAND_MANSION, BuiltinStructures.IGLOO);

    private StructureAvoidance() {
    }

    public static boolean hasConflict(ServerLevel level, BlockPos center, int villageRadius) {
        int scanRadius = villageRadius + 3;
        StructureManager sm = level.structureManager();
        HolderLookup.RegistryLookup registry = level.registryAccess().lookupOrThrow(Registries.STRUCTURE);
        List<Structure> avoidedStructures = AVOIDED_STRUCTURE_KEYS.stream().map(key -> registry.get(key).map(Holder::value).orElse(null)).filter(s -> s != null).toList();
        for (int dx = -scanRadius; dx <= scanRadius; dx += 8) {
            for (int dz = -scanRadius; dz <= scanRadius; dz += 8) {
                BlockPos check = center.offset(dx, 0, dz);
                StructureStart villageStart = sm.getStructureWithPieceAt(check, StructureTags.VILLAGE);
                if (villageStart.isValid()) {
                    LOGGER.debug("[Millenaire] Spawn rejected at {} : vanilla village detected at {}", (Object)center.toShortString(), (Object)check.toShortString());
                    return true;
                }
                for (int i = 0; i < avoidedStructures.size(); ++i) {
                    if (!sm.getStructureAt(check, avoidedStructures.get(i)).isValid()) continue;
                    LOGGER.debug("[Millenaire] Spawn rejected at {} : structure '{}' detected at {}", new Object[]{center.toShortString(), AVOIDED_STRUCTURE_KEYS.get(i).location(), check.toShortString()});
                    return true;
                }
            }
        }
        return false;
    }
}

