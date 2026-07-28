/*
 * Decompiled with CFR 0.150.
 * 
 * Could not load the following classes:
 *  net.minecraft.server.level.ServerLevel
 *  net.minecraft.server.level.ServerPlayer
 *  net.minecraft.world.damagesource.DamageSource
 *  net.minecraft.world.entity.Entity
 *  net.minecraft.world.entity.LivingEntity
 *  net.minecraft.world.entity.animal.PolarBear
 *  net.minecraft.world.entity.animal.Squid
 *  net.minecraft.world.entity.animal.Wolf
 *  net.minecraft.world.entity.item.ItemEntity
 *  net.minecraft.world.entity.monster.ElderGuardian
 *  net.minecraft.world.entity.monster.Guardian
 *  net.minecraft.world.item.ItemStack
 *  net.minecraft.world.level.ItemLike
 *  net.neoforged.neoforge.event.entity.living.LivingDropsEvent
 */
package org.millenaire.item;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.animal.PolarBear;
import net.minecraft.world.entity.animal.Squid;
import net.minecraft.world.entity.animal.Wolf;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.monster.ElderGuardian;
import net.minecraft.world.entity.monster.Guardian;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ItemLike;
import net.neoforged.neoforge.event.entity.living.LivingDropsEvent;
import org.millenaire.item.ModItems;
import org.millenaire.village.PlayerCultureReputation;

public final class InuitHuntingDropHandler {
    private InuitHuntingDropHandler() {
    }

    public static void onLivingDrops(LivingDropsEvent event) {
        LivingEntity entity = event.getEntity();
        if (entity instanceof Squid || entity instanceof Guardian) {
            InuitHuntingDropHandler.handleSeafoodDrop(event);
        } else if (entity instanceof Wolf) {
            InuitHuntingDropHandler.handleWolfMeatDrop(event);
        } else if (entity instanceof PolarBear) {
            int quantity = 1 + entity.level().random.nextInt(2);
            InuitHuntingDropHandler.addDrop(event, new ItemStack((ItemLike)ModItems.BEARMEAT_RAW.get(), quantity));
        }
    }

    private static void handleSeafoodDrop(LivingDropsEvent event) {
        ServerPlayer player;
        LivingEntity entity = event.getEntity();
        DamageSource source = event.getSource();
        Entity entity2 = source.getEntity();
        if (entity2 instanceof ServerPlayer && (entity2 = (player = (ServerPlayer)entity2).level()) instanceof ServerLevel) {
            ServerLevel serverLevel = (ServerLevel)entity2;
            PlayerCultureReputation rep = PlayerCultureReputation.get(serverLevel);
            if (!rep.hasLearnedHuntingDrop(player.getUUID(), "seafood_raw")) {
                return;
            }
            int quantity = 0;
            if (entity instanceof ElderGuardian) {
                quantity = 5 + entity.level().random.nextInt(5);
            } else if (entity instanceof Guardian) {
                quantity = 2 + entity.level().random.nextInt(2);
            } else if (entity instanceof Squid && entity.level().random.nextInt(10) == 0) {
                quantity = 1;
            }
            if (quantity > 0) {
                InuitHuntingDropHandler.addDrop(event, new ItemStack((ItemLike)ModItems.SEAFOOD_RAW.get(), quantity));
            }
        }
    }

    private static void handleWolfMeatDrop(LivingDropsEvent event) {
        ServerPlayer player;
        LivingEntity entity = event.getEntity();
        DamageSource source = event.getSource();
        Entity entity2 = source.getEntity();
        if (entity2 instanceof ServerPlayer && (entity2 = (player = (ServerPlayer)entity2).level()) instanceof ServerLevel) {
            ServerLevel serverLevel = (ServerLevel)entity2;
            PlayerCultureReputation rep = PlayerCultureReputation.get(serverLevel);
            if (!rep.hasLearnedHuntingDrop(player.getUUID(), "wolfmeat_raw")) {
                return;
            }
            int quantity = entity.level().random.nextInt(3);
            if (quantity > 0) {
                InuitHuntingDropHandler.addDrop(event, new ItemStack((ItemLike)ModItems.WOLFMEAT_RAW.get(), quantity));
            }
        }
    }

    private static void addDrop(LivingDropsEvent event, ItemStack stack) {
        LivingEntity entity = event.getEntity();
        event.getDrops().add(new ItemEntity(entity.level(), entity.getX(), entity.getY(), entity.getZ(), stack));
    }
}

