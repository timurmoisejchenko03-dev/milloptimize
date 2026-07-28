/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  net.minecraft.client.model.HumanoidModel
 *  net.minecraft.client.model.geom.ModelPart
 *  net.minecraft.client.model.geom.PartPose
 *  net.minecraft.client.model.geom.builders.CubeDeformation
 *  net.minecraft.client.model.geom.builders.CubeListBuilder
 *  net.minecraft.client.model.geom.builders.LayerDefinition
 *  net.minecraft.client.model.geom.builders.MeshDefinition
 *  net.minecraft.client.model.geom.builders.PartDefinition
 *  net.minecraft.world.entity.LivingEntity
 *  net.neoforged.api.distmarker.Dist
 *  net.neoforged.api.distmarker.OnlyIn
 */
package org.millenaire.client.model;

import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeDeformation;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.client.model.geom.builders.PartDefinition;
import net.minecraft.world.entity.LivingEntity;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import org.millenaire.client.render.VillagerPreviewPose;
import org.millenaire.entity.MillVillager;

@OnlyIn(value=Dist.CLIENT)
public class MillFemaleAsymModel
extends HumanoidModel<MillVillager> {
    public MillFemaleAsymModel(ModelPart root) {
        super(root);
    }

    public void setupAnim(MillVillager entity, float limbSwing, float limbSwingAmount, float ageInTicks, float netHeadYaw, float headPitch) {
        super.setupAnim((LivingEntity)entity, limbSwing, limbSwingAmount, ageInTicks, netHeadYaw, headPitch);
        VillagerPreviewPose.apply(this, entity);
    }

    public static LayerDefinition createBodyLayer(CubeDeformation deformation) {
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition root = mesh.getRoot();
        PartDefinition head = root.addOrReplaceChild("head", CubeListBuilder.create().texOffs(0, 0).addBox(-4.0f, -8.0f, -4.0f, 8.0f, 8.0f, 8.0f, deformation), PartPose.ZERO);
        root.addOrReplaceChild("hat", CubeListBuilder.create().texOffs(32, 0).addBox(-4.0f, -8.0f, -4.0f, 8.0f, 8.0f, 8.0f, deformation.extend(0.5f)), PartPose.ZERO);
        PartDefinition body = root.addOrReplaceChild("body", CubeListBuilder.create().texOffs(16, 17).addBox(-3.5f, 0.0f, -1.5f, 7.0f, 12.0f, 3.0f, deformation), PartPose.ZERO);
        body.addOrReplaceChild("breast", CubeListBuilder.create().texOffs(17, 18).addBox(-3.5f, 0.75f, -3.0f, 7.0f, 4.0f, 2.0f, deformation), PartPose.ZERO);
        root.addOrReplaceChild("right_arm", CubeListBuilder.create().texOffs(36, 17).addBox(-1.5f, -2.0f, -1.5f, 3.0f, 12.0f, 3.0f, deformation), PartPose.offset((float)-5.0f, (float)2.0f, (float)0.0f));
        root.addOrReplaceChild("left_arm", CubeListBuilder.create().texOffs(36, 17).mirror().addBox(-1.5f, -2.0f, -1.5f, 3.0f, 12.0f, 3.0f, deformation), PartPose.offset((float)5.0f, (float)2.0f, (float)0.0f));
        root.addOrReplaceChild("right_leg", CubeListBuilder.create().texOffs(0, 16).addBox(-2.0f, 0.0f, -2.0f, 4.0f, 12.0f, 4.0f, deformation), PartPose.offset((float)-2.0f, (float)12.0f, (float)0.0f));
        root.addOrReplaceChild("left_leg", CubeListBuilder.create().texOffs(48, 16).addBox(-2.0f, 0.0f, -2.0f, 4.0f, 12.0f, 4.0f, deformation), PartPose.offset((float)2.0f, (float)12.0f, (float)0.0f));
        return LayerDefinition.create((MeshDefinition)mesh, (int)64, (int)32);
    }
}

