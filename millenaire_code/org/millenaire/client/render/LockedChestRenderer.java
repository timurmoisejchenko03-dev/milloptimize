/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.mojang.blaze3d.vertex.PoseStack
 *  com.mojang.blaze3d.vertex.VertexConsumer
 *  com.mojang.math.Axis
 *  net.minecraft.client.model.geom.ModelLayers
 *  net.minecraft.client.model.geom.ModelPart
 *  net.minecraft.client.renderer.MultiBufferSource
 *  net.minecraft.client.renderer.RenderType
 *  net.minecraft.client.renderer.Sheets
 *  net.minecraft.client.renderer.blockentity.BlockEntityRenderer
 *  net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider$Context
 *  net.minecraft.client.resources.model.Material
 *  net.minecraft.core.Direction
 *  net.minecraft.resources.ResourceLocation
 *  net.minecraft.world.level.block.ChestBlock
 *  net.minecraft.world.level.block.state.BlockState
 *  net.minecraft.world.level.block.state.properties.ChestType
 *  net.minecraft.world.level.block.state.properties.Property
 */
package org.millenaire.client.render;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.Sheets;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.resources.model.Material;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.ChestBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.ChestType;
import net.minecraft.world.level.block.state.properties.Property;
import org.millenaire.block.LockedChestBlock;
import org.millenaire.block.LockedChestBlockEntity;

public class LockedChestRenderer
implements BlockEntityRenderer<LockedChestBlockEntity> {
    private static final Material LOCKED_CHEST = new Material(Sheets.CHEST_SHEET, ResourceLocation.fromNamespaceAndPath((String)"millenaire", (String)"entity/chest/locked_normal"));
    private static final Material LOCKED_CHEST_LEFT = new Material(Sheets.CHEST_SHEET, ResourceLocation.fromNamespaceAndPath((String)"millenaire", (String)"entity/chest/locked_normal_left"));
    private static final Material LOCKED_CHEST_RIGHT = new Material(Sheets.CHEST_SHEET, ResourceLocation.fromNamespaceAndPath((String)"millenaire", (String)"entity/chest/locked_normal_right"));
    private final ModelPart singleBottom;
    private final ModelPart singleLid;
    private final ModelPart singleLock;
    private final ModelPart leftBottom;
    private final ModelPart leftLid;
    private final ModelPart leftLock;
    private final ModelPart rightBottom;
    private final ModelPart rightLid;
    private final ModelPart rightLock;

    public LockedChestRenderer(BlockEntityRendererProvider.Context context) {
        ModelPart single = context.bakeLayer(ModelLayers.CHEST);
        this.singleBottom = single.getChild("bottom");
        this.singleLid = single.getChild("lid");
        this.singleLock = single.getChild("lock");
        ModelPart left = context.bakeLayer(ModelLayers.DOUBLE_CHEST_LEFT);
        this.leftBottom = left.getChild("bottom");
        this.leftLid = left.getChild("lid");
        this.leftLock = left.getChild("lock");
        ModelPart right = context.bakeLayer(ModelLayers.DOUBLE_CHEST_RIGHT);
        this.rightBottom = right.getChild("bottom");
        this.rightLid = right.getChild("lid");
        this.rightLock = right.getChild("lock");
    }

    public void render(LockedChestBlockEntity blockEntity, float partialTick, PoseStack poseStack, MultiBufferSource bufferSource, int packedLight, int packedOverlay) {
        ModelPart bottom;
        ModelPart lock;
        ModelPart lid;
        BlockState state = blockEntity.getBlockState();
        ChestType chestType = state.hasProperty((Property)ChestBlock.TYPE) ? (ChestType)state.getValue((Property)ChestBlock.TYPE) : ChestType.SINGLE;
        poseStack.pushPose();
        Direction facing = state.hasProperty((Property)LockedChestBlock.FACING) ? (Direction)state.getValue((Property)LockedChestBlock.FACING) : Direction.NORTH;
        float rotation = facing.toYRot();
        poseStack.translate(0.5f, 0.5f, 0.5f);
        poseStack.mulPose(Axis.YP.rotationDegrees(-rotation));
        poseStack.translate(-0.5f, -0.5f, -0.5f);
        Material material = switch (chestType) {
            case ChestType.LEFT -> {
                lid = this.leftLid;
                lock = this.leftLock;
                bottom = this.leftBottom;
                yield LOCKED_CHEST_LEFT;
            }
            case ChestType.RIGHT -> {
                lid = this.rightLid;
                lock = this.rightLock;
                bottom = this.rightBottom;
                yield LOCKED_CHEST_RIGHT;
            }
            default -> {
                lid = this.singleLid;
                lock = this.singleLock;
                bottom = this.singleBottom;
                yield LOCKED_CHEST;
            }
        };
        float openness = blockEntity.getOpenNess(partialTick);
        openness = 1.0f - openness;
        openness = 1.0f - openness * openness * openness;
        lock.xRot = lid.xRot = -(openness * 1.5707964f);
        VertexConsumer vertexConsumer = material.buffer(bufferSource, RenderType::entityCutout);
        lid.render(poseStack, vertexConsumer, packedLight, packedOverlay);
        lock.render(poseStack, vertexConsumer, packedLight, packedOverlay);
        bottom.render(poseStack, vertexConsumer, packedLight, packedOverlay);
        poseStack.popPose();
    }
}

