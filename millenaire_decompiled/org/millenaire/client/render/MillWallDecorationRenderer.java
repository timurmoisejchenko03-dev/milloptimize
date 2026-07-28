/*
 * Decompiled with CFR 0.150.
 * 
 * Could not load the following classes:
 *  com.mojang.blaze3d.vertex.PoseStack
 *  com.mojang.blaze3d.vertex.PoseStack$Pose
 *  com.mojang.blaze3d.vertex.VertexConsumer
 *  com.mojang.math.Axis
 *  net.minecraft.client.renderer.MultiBufferSource
 *  net.minecraft.client.renderer.RenderType
 *  net.minecraft.client.renderer.entity.EntityRenderer
 *  net.minecraft.client.renderer.entity.EntityRendererProvider$Context
 *  net.minecraft.client.renderer.texture.OverlayTexture
 *  net.minecraft.core.Direction
 *  net.minecraft.resources.ResourceLocation
 *  net.neoforged.api.distmarker.Dist
 *  net.neoforged.api.distmarker.OnlyIn
 */
package org.millenaire.client.render;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import org.millenaire.entity.MillWallDecoration;
import org.millenaire.entity.WallDecorationVariant;

@OnlyIn(value=Dist.CLIENT)
public class MillWallDecorationRenderer
extends EntityRenderer<MillWallDecoration> {
    private static final float ATLAS_SIZE = 256.0f;

    public MillWallDecorationRenderer(EntityRendererProvider.Context context) {
        super(context);
    }

    public ResourceLocation getTextureLocation(MillWallDecoration entity) {
        return entity.getVariant().type().texture();
    }

    public void render(MillWallDecoration entity, float entityYaw, float partialTick, PoseStack poseStack, MultiBufferSource bufferSource, int packedLight) {
        WallDecorationVariant variant = entity.getVariant();
        ResourceLocation texture = variant.type().texture();
        poseStack.pushPose();
        float yaw = MillWallDecorationRenderer.directionToYaw(entity.getDirection());
        poseStack.mulPose(Axis.YP.rotationDegrees(180.0f - yaw));
        poseStack.scale(0.0625f, 0.0625f, 0.0625f);
        VertexConsumer vertexConsumer = bufferSource.getBuffer(RenderType.entitySolid((ResourceLocation)texture));
        this.renderDecoration(poseStack, vertexConsumer, packedLight, variant.widthPixels(), variant.heightPixels(), variant.textureOffsetX(), variant.textureOffsetY());
        poseStack.popPose();
    }

    private void renderDecoration(PoseStack poseStack, VertexConsumer consumer, int packedLight, int width, int height, int textureU, int textureV) {
        float halfWidth = (float)(-width) / 2.0f;
        float halfHeight = (float)(-height) / 2.0f;
        for (int col = 0; col < width / 16; ++col) {
            for (int row = 0; row < height / 16; ++row) {
                float x1 = halfWidth + (float)((col + 1) * 16);
                float x0 = halfWidth + (float)(col * 16);
                float y1 = halfHeight + (float)((row + 1) * 16);
                float y0 = halfHeight + (float)(row * 16);
                float u0 = (float)(textureU + width - col * 16) / 256.0f;
                float u1 = (float)(textureU + width - (col + 1) * 16) / 256.0f;
                float v0 = (float)(textureV + height - row * 16) / 256.0f;
                float v1 = (float)(textureV + height - (row + 1) * 16) / 256.0f;
                float backU0 = 0.75f;
                float backU1 = 0.8125f;
                float backV0 = 0.0f;
                float backV1 = 0.0625f;
                float edgeU = 0.75390625f;
                float edgeV0 = 0.0f;
                float edgeV1 = 0.00390625f;
                PoseStack.Pose pose = poseStack.last();
                MillWallDecorationRenderer.vertex(consumer, pose, x1, y0, -0.5f, u1, v0, 0, 0, -1, packedLight);
                MillWallDecorationRenderer.vertex(consumer, pose, x0, y0, -0.5f, u0, v0, 0, 0, -1, packedLight);
                MillWallDecorationRenderer.vertex(consumer, pose, x0, y1, -0.5f, u0, v1, 0, 0, -1, packedLight);
                MillWallDecorationRenderer.vertex(consumer, pose, x1, y1, -0.5f, u1, v1, 0, 0, -1, packedLight);
                MillWallDecorationRenderer.vertex(consumer, pose, x1, y1, 0.5f, backU0, backV0, 0, 0, 1, packedLight);
                MillWallDecorationRenderer.vertex(consumer, pose, x0, y1, 0.5f, backU1, backV0, 0, 0, 1, packedLight);
                MillWallDecorationRenderer.vertex(consumer, pose, x0, y0, 0.5f, backU1, backV1, 0, 0, 1, packedLight);
                MillWallDecorationRenderer.vertex(consumer, pose, x1, y0, 0.5f, backU0, backV1, 0, 0, 1, packedLight);
                MillWallDecorationRenderer.vertex(consumer, pose, x1, y1, -0.5f, edgeU, edgeV0, 0, 1, 0, packedLight);
                MillWallDecorationRenderer.vertex(consumer, pose, x0, y1, -0.5f, edgeU, edgeV0, 0, 1, 0, packedLight);
                MillWallDecorationRenderer.vertex(consumer, pose, x0, y1, 0.5f, edgeU, edgeV1, 0, 1, 0, packedLight);
                MillWallDecorationRenderer.vertex(consumer, pose, x1, y1, 0.5f, edgeU, edgeV1, 0, 1, 0, packedLight);
                MillWallDecorationRenderer.vertex(consumer, pose, x1, y0, 0.5f, edgeU, edgeV0, 0, -1, 0, packedLight);
                MillWallDecorationRenderer.vertex(consumer, pose, x0, y0, 0.5f, edgeU, edgeV0, 0, -1, 0, packedLight);
                MillWallDecorationRenderer.vertex(consumer, pose, x0, y0, -0.5f, edgeU, edgeV1, 0, -1, 0, packedLight);
                MillWallDecorationRenderer.vertex(consumer, pose, x1, y0, -0.5f, edgeU, edgeV1, 0, -1, 0, packedLight);
                MillWallDecorationRenderer.vertex(consumer, pose, x1, y1, 0.5f, edgeU, edgeV0, -1, 0, 0, packedLight);
                MillWallDecorationRenderer.vertex(consumer, pose, x1, y0, 0.5f, edgeU, edgeV1, -1, 0, 0, packedLight);
                MillWallDecorationRenderer.vertex(consumer, pose, x1, y0, -0.5f, edgeU, edgeV1, -1, 0, 0, packedLight);
                MillWallDecorationRenderer.vertex(consumer, pose, x1, y1, -0.5f, edgeU, edgeV0, -1, 0, 0, packedLight);
                MillWallDecorationRenderer.vertex(consumer, pose, x0, y1, -0.5f, edgeU, edgeV0, 1, 0, 0, packedLight);
                MillWallDecorationRenderer.vertex(consumer, pose, x0, y0, -0.5f, edgeU, edgeV1, 1, 0, 0, packedLight);
                MillWallDecorationRenderer.vertex(consumer, pose, x0, y0, 0.5f, edgeU, edgeV1, 1, 0, 0, packedLight);
                MillWallDecorationRenderer.vertex(consumer, pose, x0, y1, 0.5f, edgeU, edgeV0, 1, 0, 0, packedLight);
            }
        }
    }

    private static void vertex(VertexConsumer consumer, PoseStack.Pose pose, float x, float y, float z, float u, float v, int nx, int ny, int nz, int packedLight) {
        consumer.addVertex(pose, x, y, z).setColor(255, 255, 255, 255).setUv(u, v).setOverlay(OverlayTexture.NO_OVERLAY).setLight(packedLight).setNormal(pose, (float)nx, (float)ny, (float)nz);
    }

    private static float directionToYaw(Direction direction) {
        return switch (direction) {
            case Direction.SOUTH -> 0.0f;
            case Direction.WEST -> 90.0f;
            case Direction.NORTH -> 180.0f;
            case Direction.EAST -> 270.0f;
            default -> 0.0f;
        };
    }
}

