/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.mojang.blaze3d.vertex.PoseStack
 *  com.mojang.blaze3d.vertex.VertexConsumer
 *  net.minecraft.client.model.HumanoidModel
 *  net.minecraft.client.renderer.MultiBufferSource
 *  net.minecraft.client.renderer.RenderType
 *  net.minecraft.client.renderer.entity.LivingEntityRenderer
 *  net.minecraft.client.renderer.entity.RenderLayerParent
 *  net.minecraft.client.renderer.entity.layers.RenderLayer
 *  net.minecraft.resources.ResourceLocation
 *  net.minecraft.world.entity.LivingEntity
 *  net.neoforged.api.distmarker.Dist
 *  net.neoforged.api.distmarker.OnlyIn
 */
package org.millenaire.client.render;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import java.util.Map;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.LivingEntityRenderer;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.client.renderer.entity.layers.RenderLayer;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.LivingEntity;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import org.millenaire.entity.MillVillager;
import org.millenaire.entity.ModelType;

@OnlyIn(value=Dist.CLIENT)
public class ClothingLayer
extends RenderLayer<MillVillager, HumanoidModel<MillVillager>> {
    private final Map<ModelType, HumanoidModel<MillVillager>> clothModels;
    private final int layerIndex;

    public ClothingLayer(RenderLayerParent<MillVillager, HumanoidModel<MillVillager>> parent, Map<ModelType, HumanoidModel<MillVillager>> clothModels, int layerIndex) {
        super(parent);
        this.clothModels = clothModels;
        this.layerIndex = layerIndex;
    }

    public void render(PoseStack poseStack, MultiBufferSource bufferSource, int packedLight, MillVillager entity, float limbSwing, float limbSwingAmount, float partialTick, float ageInTicks, float netHeadYaw, float headPitch) {
        ResourceLocation clothTexture;
        ResourceLocation resourceLocation = clothTexture = this.layerIndex == 0 ? entity.getClothTexture0() : entity.getClothTexture1();
        if (clothTexture == null) {
            return;
        }
        HumanoidModel<MillVillager> clothModel = this.clothModels.get((Object)entity.getModelType());
        if (clothModel == null) {
            return;
        }
        HumanoidModel parentModel = (HumanoidModel)this.getParentModel();
        parentModel.copyPropertiesTo(clothModel);
        VertexConsumer consumer = bufferSource.getBuffer(RenderType.entityCutoutNoCull((ResourceLocation)clothTexture));
        int overlay = LivingEntityRenderer.getOverlayCoords((LivingEntity)entity, (float)0.0f);
        clothModel.renderToBuffer(poseStack, consumer, packedLight, overlay, -1);
    }
}

