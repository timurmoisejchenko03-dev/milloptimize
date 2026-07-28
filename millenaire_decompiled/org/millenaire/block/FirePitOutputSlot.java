/*
 * Decompiled with CFR 0.150.
 * 
 * Could not load the following classes:
 *  net.minecraft.server.level.ServerLevel
 *  net.minecraft.server.level.ServerPlayer
 *  net.minecraft.util.Mth
 *  net.minecraft.world.Container
 *  net.minecraft.world.entity.ExperienceOrb
 *  net.minecraft.world.entity.player.Player
 *  net.minecraft.world.inventory.Slot
 *  net.minecraft.world.item.ItemStack
 *  net.minecraft.world.phys.Vec3
 */
package org.millenaire.block;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.Mth;
import net.minecraft.world.Container;
import net.minecraft.world.entity.ExperienceOrb;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;
import org.millenaire.block.FirePitBlockEntity;

public class FirePitOutputSlot
extends Slot {
    private final Player player;
    private int removeCount;

    public FirePitOutputSlot(Player player, Container container, int slot, int x, int y) {
        super(container, slot, x, y);
        this.player = player;
    }

    public boolean mayPlace(ItemStack stack) {
        return false;
    }

    public ItemStack remove(int amount) {
        if (this.hasItem()) {
            this.removeCount += Math.min(amount, this.getItem().getCount());
        }
        return super.remove(amount);
    }

    protected void onQuickCraft(ItemStack stack, int amount) {
        this.removeCount += amount;
        this.checkTakeAchievements(stack);
    }

    protected void checkTakeAchievements(ItemStack stack) {
        stack.onCraftedBy(this.player.level(), this.player, this.removeCount);
        Player player = this.player;
        if (player instanceof ServerPlayer) {
            int xpAmount;
            float fractional;
            ServerPlayer serverPlayer = (ServerPlayer)player;
            ServerLevel level = serverPlayer.serverLevel();
            float experience = 0.0f;
            Container container = this.container;
            if (container instanceof FirePitBlockEntity) {
                FirePitBlockEntity blockEntity = (FirePitBlockEntity)container;
                int lane = this.getContainerSlot() - 4;
                experience = blockEntity.claimPendingXp(lane, this.removeCount);
            }
            if ((fractional = experience - (float)(xpAmount = Mth.floor((float)experience))) > 0.0f && Math.random() < (double)fractional) {
                ++xpAmount;
            }
            if (xpAmount > 0) {
                ExperienceOrb.award((ServerLevel)level, (Vec3)serverPlayer.position().add(0.0, 0.5, 0.0), (int)xpAmount);
            }
        }
        this.removeCount = 0;
    }

    public void onTake(Player player, ItemStack stack) {
        this.checkTakeAchievements(stack);
        super.onTake(player, stack);
    }
}

