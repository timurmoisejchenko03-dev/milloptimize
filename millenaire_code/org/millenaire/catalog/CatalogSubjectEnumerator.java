/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  net.minecraft.resources.ResourceLocation
 */
package org.millenaire.catalog;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import net.minecraft.resources.ResourceLocation;
import org.millenaire.building.BuildingPlanSet;
import org.millenaire.catalog.CatalogJob;
import org.millenaire.catalog.CatalogPaths;
import org.millenaire.catalog.ShotSpec;
import org.millenaire.culture.VillagerType;

public final class CatalogSubjectEnumerator {
    private CatalogSubjectEnumerator() {
    }

    public static List<ShotSpec> enumerate(Map<ResourceLocation, VillagerType> villagerTypes, Map<ResourceLocation, BuildingPlanSet> planSets, CatalogJob job, Path root) {
        String culture;
        int count;
        ArrayList<ShotSpec> shots = new ArrayList<ShotSpec>();
        int limit = job.limit();
        if (job.target().includesVillagers()) {
            count = 0;
            for (VillagerType vt : villagerTypes.values()) {
                culture = vt.culture().getPath();
                if (!job.includesCulture(culture) || !job.includesVillager(vt.id())) continue;
                if (limit > 0 && count >= limit) break;
                ++count;
                shots.add(new ShotSpec.VillagerShot(vt.id(), CatalogPaths.villagerOutput(root, culture, vt.id())));
            }
        }
        if (job.target().includesBuildings()) {
            count = 0;
            for (BuildingPlanSet ps : planSets.values()) {
                culture = ps.culture().getPath();
                if (!job.includesCulture(culture) || ps.isSubBuilding() || ps.isWallSegment() || ps.isBorderBuilding() || ps.tags().contains("worldquest") || !job.includesBuilding(ps.id())) continue;
                if (limit > 0 && count >= limit) break;
                ++count;
                for (Map.Entry<String, List<BuildingPlanSet.LevelDef>> entry : ps.variants().entrySet()) {
                    String variant = entry.getKey();
                    int maxLevel = entry.getValue().size() - 1;
                    if (maxLevel < 0) continue;
                    LinkedHashSet<Integer> levels = new LinkedHashSet<Integer>();
                    levels.add(maxLevel);
                    for (int lvl = 0; lvl <= maxLevel; ++lvl) {
                        levels.add(lvl);
                    }
                    Iterator iterator = levels.iterator();
                    while (iterator.hasNext()) {
                        int level = (Integer)iterator.next();
                        shots.add(new ShotSpec.BuildingShot(ps.id(), variant, level, maxLevel, CatalogPaths.buildingOutput(root, culture, ps.id(), variant, level)));
                    }
                }
            }
        }
        return shots;
    }
}

