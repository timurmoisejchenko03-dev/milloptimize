package org.milloptimize.entity;

import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.player.Player;
import org.millenaire.entity.MillVillager;

public class MillVillagerHelper {
    
    /**
     * Проверяет, находится ли игрок рядом с деревней
     * @param villager сущность NPC из Millénaire
     * @return true если игрок находится в пределах 100 блоков
     */
    public static boolean isPlayerNearby(MillVillager villager) {
        if (villager == null || !villager.isAlive()) {
            return false;
        }
        
        // Получаем мир и текущую позицию NPC
        var world = villager.level();
        BlockPos villagerPos = villager.blockPosition();
        
        // Ищем ближайшего игрока в радиусе 100 блоков
        Player nearestPlayer = world.getNearestPlayer(villager, 100.0);
        
        if (nearestPlayer != null) {
            double distanceSq = villagerPos.distSqr(nearestPlayer.blockPosition());
            return distanceSq <= 100 * 100; // 100 блоков
        }
        
        return false;
    }
}
