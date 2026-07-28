/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  net.minecraft.server.level.ServerPlayer
 *  net.minecraft.world.InteractionHand
 *  net.minecraft.world.InteractionResultHolder
 *  net.minecraft.world.effect.MobEffectInstance
 *  net.minecraft.world.effect.MobEffects
 *  net.minecraft.world.entity.LivingEntity
 *  net.minecraft.world.entity.player.Player
 *  net.minecraft.world.item.Item
 *  net.minecraft.world.item.Item$Properties
 *  net.minecraft.world.item.ItemStack
 *  net.minecraft.world.item.UseAnim
 *  net.minecraft.world.level.Level
 */
package org.millenaire.item;

import java.util.List;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.UseAnim;
import net.minecraft.world.level.Level;
import org.millenaire.advancement.MillAdvancements;

public class MillFoodItem
extends Item {
    private static final int DAMAGE_PER_PORTION = 64;
    private static final int USE_DURATION_TICKS = 32;
    private final int nutrition;
    private final float saturationModifier;
    private final int healAmount;
    private final int regenerationSeconds;
    private final int drunkSeconds;
    private final int speedSeconds;
    private final boolean drink;
    private final boolean clearEffects;
    private final List<MobEffectInstance> extraEffects;

    public MillFoodItem(Item.Properties properties, int nutrition, float saturationModifier, int healAmount, int regenerationSeconds, int drunkSeconds, boolean drink) {
        this(properties, nutrition, saturationModifier, healAmount, regenerationSeconds, drunkSeconds, 0, drink, false, List.of());
    }

    public MillFoodItem(Item.Properties properties, int nutrition, float saturationModifier, int healAmount, int regenerationSeconds, int drunkSeconds, int speedSeconds, boolean drink) {
        this(properties, nutrition, saturationModifier, healAmount, regenerationSeconds, drunkSeconds, speedSeconds, drink, false, List.of());
    }

    public MillFoodItem(Item.Properties properties, int nutrition, float saturationModifier, int healAmount, int regenerationSeconds, int drunkSeconds, int speedSeconds, boolean drink, boolean clearEffects, List<MobEffectInstance> extraEffects) {
        super(properties);
        this.nutrition = nutrition;
        this.saturationModifier = saturationModifier;
        this.healAmount = healAmount;
        this.regenerationSeconds = regenerationSeconds;
        this.drunkSeconds = drunkSeconds;
        this.speedSeconds = speedSeconds;
        this.drink = drink;
        this.clearEffects = clearEffects;
        this.extraEffects = List.copyOf(extraEffects);
    }

    public UseAnim getUseAnimation(ItemStack stack) {
        return this.drink ? UseAnim.DRINK : UseAnim.EAT;
    }

    public int getUseDuration(ItemStack stack, LivingEntity entity) {
        return 32;
    }

    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        player.startUsingItem(hand);
        return InteractionResultHolder.consume((Object)player.getItemInHand(hand));
    }

    public ItemStack finishUsingItem(ItemStack stack, Level level, LivingEntity entity) {
        if (!level.isClientSide() && entity instanceof Player) {
            Player player = (Player)entity;
            if (this.nutrition > 0) {
                player.getFoodData().eat(this.nutrition, this.saturationModifier);
            }
            if (this.healAmount > 0) {
                player.heal((float)this.healAmount);
            }
            if (this.regenerationSeconds > 0) {
                player.addEffect(new MobEffectInstance(MobEffects.REGENERATION, this.regenerationSeconds * 20, 0));
            }
            if (this.drunkSeconds > 0) {
                player.addEffect(new MobEffectInstance(MobEffects.CONFUSION, this.drunkSeconds * 20, 0));
            }
            if (this.speedSeconds > 0) {
                player.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SPEED, this.speedSeconds * 20, 1));
            }
            if (this.drink && player instanceof ServerPlayer) {
                ServerPlayer sp = (ServerPlayer)player;
                MillAdvancements.grant(sp, MillAdvancements.CHEERS);
            }
            if (this.clearEffects) {
                player.removeAllEffects();
            }
            for (MobEffectInstance effect : this.extraEffects) {
                player.addEffect(new MobEffectInstance(effect));
            }
        }
        if (stack.isDamageableItem()) {
            int newDamage = stack.getDamageValue() + 64;
            if (newDamage < stack.getMaxDamage()) {
                stack.setDamageValue(newDamage);
            } else {
                stack.shrink(1);
            }
        } else if (entity instanceof Player) {
            Player p = (Player)entity;
            if (!p.getAbilities().instabuild) {
                stack.shrink(1);
            }
        }
        return stack;
    }

    static Item.Properties multiPortionProperties(Item.Properties props, int portions) {
        if (portions > 1) {
            props.durability(portions * 64).stacksTo(1);
        }
        return props;
    }
}

