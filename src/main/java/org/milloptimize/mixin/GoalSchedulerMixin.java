package org.milloptimize.mixin;

import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.player.Player;
import org.millenaire.goal.GoalScheduler;
import org.millenaire.goal.GoalContext;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(targets = "org.millenaire.goal.GoalScheduler")
public abstract class GoalSchedulerMixin {
    // Хранит последнюю позицию игрока для оптимизации
    private static BlockPos lastPlayerPos = null;
    
    // Порог расстояния для оптимизации (в блоках)
    private static final double DISTANCE_THRESHOLD = 100.0;
    
    @Inject(method = "tick", at = @At("HEAD"))
    private static void throttleDistanceAI(GoalContext ctx, CallbackInfo ci) {
        // Получаем NPC из контекста
        var villager = ctx.villager();
        
        if (villager != null && villager.isAlive()) {
            // Получаем мир и текущую позицию игрока
            var world = villager.level();
            Player player = world.getNearestPlayer(villager, 100.0);
            
            if (player != null) {
                lastPlayerPos = new BlockPos((int)player.getX(), (int)player.getY(), (int)player.getZ());
            }
            
            // Получаем текущую цель NPC для расчета дистанции
            var goalScheduler = villager.getGoalScheduler();
            
            // Если у NPC есть активная цель с позицией, используем её для оптимизации
            if (goalScheduler != null) {
                // Получаем текущую позицию NPC
                double x = villager.getX();
                double y = villager.getY();
                double z = villager.getZ();
                
                // Проверяем расстояние до игрока
                if (lastPlayerPos != null) {
                    double distanceSq = lastPlayerPos.distSqr(new BlockPos((int)x, (int)y, (int)z));
                    
                    // Если игрок далеко (>100 блоков), оптимизируем обновления
                    if (distanceSq > DISTANCE_THRESHOLD * DISTANCE_THRESHOLD) {
                        // Блокируем выполнение планировщика задач для этого NPC
                        ci.cancel();
                    }
                }
            }
        }
    }
}
