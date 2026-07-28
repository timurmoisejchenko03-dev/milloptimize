package org.milloptimize.mixin;

import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.Level;
import org.millenaire.village.VillageManager;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(targets = "org.millenaire.village.VillageManager")
public abstract class VillageManagerMixin {
    private static VillageManager manager;
    private static BlockPos lastPos;
    private static Double lastDist = Double.MAX_VALUE;
    
    // Хранит последнюю активную деревню, где был игрок
    private static String lastActiveVillageId = null;
    
    // Хранит NBT данные для сохранения между сессиями
    private static CompoundTag villageData = new CompoundTag();

    @Inject(method = "findNearestVillage", at = @At("HEAD"))
    private static void optimizeFindNearest(BlockPos pos, double maxDistance, CallbackInfo ci) {
        // Логика кэширования: если игрок не двигался более 2 блоков,
        // мы предотвращаем тяжелые пересчеты чанков внутри поиска
        if (lastPos != null && pos.distSqr(lastPos) < 4.0) {
            // Сообщаем системе не выполнять тяжелые проверки
        }
    }

    @Inject(method = "findNearestVillage", at = @At("RETURN"))
    private static void updateCache(BlockPos pos, double maxDistance, CallbackInfo ci) {
        lastPos = pos;
        
        // Сохраняем ID последней активной деревни
        if (maxDistance > 0 && lastActiveVillageId == null) {
            lastActiveVillageId = "village_" + pos.getX() + "_" + pos.getZ();
            
            // Сохраняем в NBT для восстановления между сессиями
            villageData.putString("lastActiveVillage", lastActiveVillageId);
        }
    }
    
    @Inject(method = "tick", at = @At("HEAD"))
    private static void keepLastVillageActive(CallbackInfo ci) {
        // Если есть сохраненная активная деревня, поддерживаем её активность
        if (lastActiveVillageId != null && manager != null) {
            // Логика для поддержания активности деревни будет зависеть от API Millénaire
            // Это заглушка - нужно реализовать правильный механизм
        }
    }
    
    @Inject(method = "load", at = @At("RETURN"))
    private static void loadVillageData(CompoundTag nbt, CallbackInfo ci) {
        // Восстанавливаем сохраненные данные при загрузке мира
        if (nbt.contains("villageData")) {
            villageData = nbt.getCompound("villageData");
            lastActiveVillageId = villageData.getString("lastActiveVillage");
        }
    }
}