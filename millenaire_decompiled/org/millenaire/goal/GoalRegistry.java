/*
 * Decompiled with CFR 0.150.
 * 
 * Could not load the following classes:
 *  com.mojang.logging.LogUtils
 *  net.minecraft.resources.ResourceLocation
 *  org.slf4j.Logger
 */
package org.millenaire.goal;

import com.mojang.logging.LogUtils;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import net.minecraft.resources.ResourceLocation;
import org.millenaire.goal.VillagerGoal;
import org.millenaire.goal.gathering.GatheringGoal;
import org.slf4j.Logger;

public class GoalRegistry {
    private static final Logger LOGGER = LogUtils.getLogger();
    private final Map<ResourceLocation, VillagerGoal> goals = new HashMap<ResourceLocation, VillagerGoal>();
    private Map<ResourceLocation, VillagerGoal> builtinSnapshot;

    public void register(VillagerGoal goal) {
        VillagerGoal existing = this.goals.putIfAbsent(goal.id(), goal);
        if (existing != null) {
            LOGGER.warn("Duplicate goal registration ignored: {}", (Object)goal.id());
        }
    }

    public void replace(VillagerGoal goal) {
        VillagerGoal previous = this.goals.put(goal.id(), goal);
        if (previous != null) {
            LOGGER.debug("External content overrides built-in goal: {}", (Object)goal.id());
        }
    }

    public void freezeBuiltins() {
        if (this.builtinSnapshot != null) {
            LOGGER.warn("GoalRegistry.freezeBuiltins() called twice \u2014 keeping the first snapshot");
            return;
        }
        this.builtinSnapshot = Map.copyOf(this.goals);
        LOGGER.debug("GoalRegistry built-in snapshot frozen: {} goals", (Object)this.builtinSnapshot.size());
    }

    public void resetToBuiltins() {
        if (this.builtinSnapshot == null) {
            throw new IllegalStateException("GoalRegistry.resetToBuiltins() called before freezeBuiltins()");
        }
        this.goals.clear();
        this.goals.putAll(this.builtinSnapshot);
    }

    public VillagerGoal get(ResourceLocation id) {
        return this.goals.get((Object)id);
    }

    public List<ResourceLocation> getAllIds() {
        return this.goals.keySet().stream().sorted(Comparator.comparing(ResourceLocation::toString)).toList();
    }

    public List<GatheringGoal> getGatheringGoals() {
        return this.goals.values().stream().filter(g -> g instanceof GatheringGoal).map(g -> (GatheringGoal)g).toList();
    }

    public List<VillagerGoal> resolve(List<ResourceLocation> ids) {
        return ids.stream().map(id -> {
            VillagerGoal goal = this.goals.get(id);
            if (goal == null) {
                LOGGER.warn("Unknown goal ignored: {}", id);
            }
            return goal;
        }).filter(g -> g != null).toList();
    }
}

