/*
 * Decompiled with CFR 0.150.
 * 
 * Could not load the following classes:
 *  com.mojang.blaze3d.vertex.PoseStack
 *  com.mojang.math.Axis
 *  net.minecraft.client.renderer.MultiBufferSource
 *  net.minecraft.client.renderer.blockentity.BlockEntityRenderer
 *  net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider$Context
 *  net.minecraft.client.renderer.entity.ItemRenderer
 *  net.minecraft.core.BlockPos
 *  net.minecraft.world.item.ItemDisplayContext
 *  net.minecraft.world.item.ItemStack
 *  net.minecraft.world.level.Level
 *  net.minecraft.world.level.block.state.BlockState
 */
package org.millenaire.client.render;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.entity.ItemRenderer;
import net.minecraft.core.BlockPos;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import org.millenaire.block.FirePitAlignment;
import org.millenaire.block.FirePitBlock;
import org.millenaire.block.FirePitBlockEntity;

public class FirePitRenderer
implements BlockEntityRenderer<FirePitBlockEntity> {
    private static final float ITEM_SCALE = 0.35f;
    private static final float[][] COOKING_POSITIONS = new float[][]{{0.5f, 1.0f, 0.4f, 25.0f, 180.0f, -45.0f}, {0.5f, 0.9f, 0.5f, 0.0f, 45.0f, -45.0f}, {0.5f, 1.0f, 0.6f, -25.0f, 180.0f, -45.0f}};
    private static final float[][] COOKED_POSITIONS = new float[][]{{0.5f, 0.9f, 0.4f, 25.0f, 180.0f, -45.0f}, {0.5f, 0.9f, 0.5f, 0.0f, -45.0f, -45.0f}, {0.5f, 0.9f, 0.6f, -25.0f, 180.0f, -45.0f}};
    private final ItemRenderer itemRenderer;

    public FirePitRenderer(BlockEntityRendererProvider.Context context) {
        this.itemRenderer = context.getItemRenderer();
    }

    public void render(FirePitBlockEntity blockEntity, float partialTick, PoseStack poseStack, MultiBufferSource bufferSource, int packedLight, int packedOverlay) {
        Level level = blockEntity.getLevel();
        BlockPos pos = blockEntity.getBlockPos();
        BlockState state = blockEntity.getBlockState();
        if (!(state.getBlock() instanceof FirePitBlock)) {
            return;
        }
        double alignment = ((FirePitAlignment)state.getValue(FirePitBlock.ALIGNMENT)).angle;
        poseStack.pushPose();
        poseStack.translate(0.5, 0.5, 0.5);
        poseStack.mulPose(Axis.YP.rotationDegrees((float)alignment));
        poseStack.translate(-0.5, -0.5, -0.5);
        ItemStack fuel = blockEntity.getFuelItem();
        if (!fuel.isEmpty()) {
            poseStack.pushPose();
            poseStack.translate(0.5, 0.2, 0.5);
            poseStack.mulPose(Axis.XP.rotationDegrees(90.0f));
            int posTotal = pos.getX() + pos.getY() + pos.getZ() & 3;
            poseStack.mulPose(Axis.ZP.rotationDegrees(90.0f * (float)posTotal));
            poseStack.scale(0.5f, 0.5f, 0.5f);
            this.renderItem(fuel, level, pos, poseStack, bufferSource, packedLight, packedOverlay);
            poseStack.popPose();
        }
        for (int i = 0; i < 3; ++i) {
            this.renderPlacedItem(blockEntity.getInputItem(i), COOKING_POSITIONS[i], level, pos, poseStack, bufferSource, packedLight, packedOverlay);
            this.renderPlacedItem(blockEntity.getOutputItem(i), COOKED_POSITIONS[i], level, pos, poseStack, bufferSource, packedLight, packedOverlay);
        }
        poseStack.popPose();
    }

    private void renderPlacedItem(ItemStack stack, float[] transform, Level level, BlockPos pos, PoseStack poseStack, MultiBufferSource bufferSource, int packedLight, int packedOverlay) {
        if (stack.isEmpty()) {
            return;
        }
        poseStack.pushPose();
        poseStack.translate(transform[0], transform[1], transform[2]);
        poseStack.mulPose(Axis.XP.rotationDegrees(transform[3]));
        poseStack.mulPose(Axis.YP.rotationDegrees(transform[4]));
        poseStack.mulPose(Axis.ZP.rotationDegrees(transform[5]));
        poseStack.scale(0.35f, 0.35f, 0.35f);
        this.renderItem(stack, level, pos, poseStack, bufferSource, packedLight, packedOverlay);
        poseStack.popPose();
    }

    private void renderItem(ItemStack stack, Level level, BlockPos pos, PoseStack poseStack, MultiBufferSource bufferSource, int packedLight, int packedOverlay) {
        int seed = pos == null ? 0 : (int)pos.asLong();
        this.itemRenderer.renderStatic(stack, ItemDisplayContext.FIXED, packedLight, packedOverlay, poseStack, bufferSource, level, seed);
    }
}

