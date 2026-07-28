/*
 * Decompiled with CFR 0.152.
 */
package org.millenaire.map;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import org.millenaire.village.VillageId;

public final class VillageDiscoveryHelper {
    private VillageDiscoveryHelper() {
    }

    public static Set<VillageId> findNewlyDiscovered(int playerX, int playerZ, Map<VillageId, Footprint> townHallFootprints, Set<VillageId> alreadyDiscovered) {
        HashSet<VillageId> result = new HashSet<VillageId>();
        for (Map.Entry<VillageId, Footprint> e : townHallFootprints.entrySet()) {
            if (alreadyDiscovered.contains(e.getKey()) || !e.getValue().contains(playerX, playerZ)) continue;
            result.add(e.getKey());
        }
        return result;
    }

    public record Footprint(int minX, int maxX, int minZ, int maxZ) {
        public boolean contains(int x, int z) {
            return x >= this.minX && x <= this.maxX && z >= this.minZ && z <= this.maxZ;
        }
    }
}

