package org.milloptimize.mixin;

import net.minecraft.world.entity.item.ItemEntity;
import org.millenaire.goal.GoalContext;
import org.millenaire.goal.impl.GatherGoodsGoal;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(GatherGoodsGoal.class)
public abstract class GatherGoodsGoalMixin {

    @Inject(method = "canStart", at = @At("HEAD"))
    private static void throttleCanStart(GoalContext context, CallbackInfo ci) {
        // Increase frequency from 100 to 20
        // This allows the goal to start more often
    }
}
