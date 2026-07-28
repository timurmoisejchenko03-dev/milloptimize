/*
 * Decompiled with CFR 0.150.
 * 
 * Could not load the following classes:
 *  javax.annotation.Nullable
 */
package org.millenaire.encyclopedia;

import java.util.List;
import java.util.Map;
import javax.annotation.Nullable;

public record VillageData(String type, int radius, int weight, boolean playerControlled, boolean carriesRaid, Population population, List<RoleGroup> composition, @Nullable Walls walls, @Nullable Economy economy, @Nullable List<String> qualifiers, @Nullable Map<String, String> terrainQualifiers, @Nullable List<String> hamlets, @Nullable List<String> biomes) {

    public record Population(int total, int male, int female) {
    }

    public record Walls(@Nullable String inner, @Nullable String outer, int innerRadius) {
    }

    public record Economy(@Nullable Map<String, Integer> sellingOverrides, @Nullable Map<String, Integer> buyingOverrides) {
    }

    public record Building(String itemKey, int count, int residents) {
    }

    public record RoleGroup(String role, List<Building> buildings) {
    }
}

