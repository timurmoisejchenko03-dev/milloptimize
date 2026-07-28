/*
 * Decompiled with CFR 0.150.
 * 
 * Could not load the following classes:
 *  net.minecraft.client.model.HumanoidModel
 *  net.minecraft.world.InteractionHand
 *  net.minecraft.world.item.BlockItem
 *  net.minecraft.world.item.ItemStack
 *  net.minecraft.world.item.TieredItem
 */
package org.millenaire.client.render;

import net.minecraft.client.model.HumanoidModel;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TieredItem;
import org.millenaire.entity.MillVillager;

public final class VillagerPreviewPose {
    private static final float PRESENT_X = -1.5f;
    private static final float PRESENT_X_OFF = -1.0f;
    private static final float PRESENT_Z = 0.0f;

    private VillagerPreviewPose() {
    }

    public static void apply(HumanoidModel<MillVillager> model, MillVillager entity) {
        if (!entity.isGuiPreviewMode()) {
            return;
        }
        if (VillagerPreviewPose.isFlatItem(entity.getItemInHand(InteractionHand.MAIN_HAND))) {
            model.rightArm.xRot = -1.5f;
            model.rightArm.yRot = 0.0f;
            model.rightArm.zRot = 0.0f;
        }
        if (VillagerPreviewPose.isFlatItem(entity.getItemInHand(InteractionHand.OFF_HAND))) {
            model.leftArm.xRot = -1.0f;
            model.leftArm.yRot = 0.0f;
            model.leftArm.zRot = -0.0f;
        }
    }

    private static boolean isFlatItem(ItemStack stack) {
        if (stack.isEmpty()) {
            return false;
        }
        return !(stack.getItem() instanceof TieredItem) && !(stack.getItem() instanceof BlockItem);
    }
}

