/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  net.minecraft.world.entity.Mob
 *  net.minecraft.world.entity.ai.navigation.GroundPathNavigation
 *  net.minecraft.world.level.Level
 *  net.minecraft.world.level.pathfinder.PathFinder
 */
package org.millenaire.entity;

import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.navigation.GroundPathNavigation;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.pathfinder.PathFinder;
import org.millenaire.entity.MillWalkNodeEvaluator;

public class MillPathNavigation
extends GroundPathNavigation {
    private static final int NODE_BUDGET_MULTIPLIER = 8;

    public MillPathNavigation(Mob mob, Level level) {
        super(mob, level);
        this.setCanOpenDoors(true);
        this.setCanWalkOverFences(false);
        this.setCanFloat(true);
    }

    protected PathFinder createPathFinder(int maxVisitedNodes) {
        this.nodeEvaluator = new MillWalkNodeEvaluator(this.mob);
        this.nodeEvaluator.setCanPassDoors(true);
        this.nodeEvaluator.setCanOpenDoors(true);
        return new PathFinder(this.nodeEvaluator, maxVisitedNodes * 8);
    }
}

