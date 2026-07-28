/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.mojang.blaze3d.vertex.ByteBufferBuilder
 *  com.mojang.blaze3d.vertex.PoseStack
 *  com.mojang.math.Axis
 *  net.minecraft.client.Minecraft
 *  net.minecraft.client.gui.Font
 *  net.minecraft.client.gui.Font$DisplayMode
 *  net.minecraft.client.model.EntityModel
 *  net.minecraft.client.model.HumanoidArmorModel
 *  net.minecraft.client.model.HumanoidModel
 *  net.minecraft.client.model.HumanoidModel$ArmPose
 *  net.minecraft.client.model.geom.ModelLayers
 *  net.minecraft.client.renderer.MultiBufferSource
 *  net.minecraft.client.renderer.MultiBufferSource$BufferSource
 *  net.minecraft.client.renderer.entity.EntityRendererProvider$Context
 *  net.minecraft.client.renderer.entity.MobRenderer
 *  net.minecraft.client.renderer.entity.RenderLayerParent
 *  net.minecraft.client.renderer.entity.layers.HumanoidArmorLayer
 *  net.minecraft.client.renderer.entity.layers.ItemInHandLayer
 *  net.minecraft.client.renderer.entity.layers.RenderLayer
 *  net.minecraft.client.renderer.texture.OverlayTexture
 *  net.minecraft.network.chat.Component
 *  net.minecraft.network.chat.FormattedText
 *  net.minecraft.network.chat.MutableComponent
 *  net.minecraft.resources.ResourceLocation
 *  net.minecraft.world.InteractionHand
 *  net.minecraft.world.entity.Entity
 *  net.minecraft.world.entity.LivingEntity
 *  net.minecraft.world.item.ItemDisplayContext
 *  net.minecraft.world.item.ItemStack
 *  net.minecraft.world.item.Items
 *  net.minecraft.world.level.ItemLike
 *  net.neoforged.api.distmarker.Dist
 *  net.neoforged.api.distmarker.OnlyIn
 *  org.joml.Matrix4f
 */
package org.millenaire.client.render;

import com.mojang.blaze3d.vertex.ByteBufferBuilder;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.model.EntityModel;
import net.minecraft.client.model.HumanoidArmorModel;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.MobRenderer;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.client.renderer.entity.layers.HumanoidArmorLayer;
import net.minecraft.client.renderer.entity.layers.ItemInHandLayer;
import net.minecraft.client.renderer.entity.layers.RenderLayer;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.FormattedText;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.ItemLike;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import org.joml.Matrix4f;
import org.millenaire.DisplayUtils;
import org.millenaire.client.ClientLanguageCache;
import org.millenaire.client.ClientQuestCache;
import org.millenaire.client.model.MillFemaleAsymModel;
import org.millenaire.client.model.MillFemaleSymModel;
import org.millenaire.client.model.MillMaleModel;
import org.millenaire.client.model.MillModelLayers;
import org.millenaire.client.render.ClothingLayer;
import org.millenaire.config.MillenaireClientConfig;
import org.millenaire.entity.MillVillager;
import org.millenaire.entity.ModelType;
import org.millenaire.item.ModItems;
import org.millenaire.language.DisplayNameResolver;
import org.millenaire.language.SpeechResolver;

@OnlyIn(value=Dist.CLIENT)
public class MillVillagerRenderer
extends MobRenderer<MillVillager, HumanoidModel<MillVillager>> {
    private static final ResourceLocation DEFAULT_TEXTURE = ResourceLocation.fromNamespaceAndPath((String)"millenaire", (String)"textures/entity/villager/default.png");
    private final HumanoidModel<MillVillager> maleModel;
    private final HumanoidModel<MillVillager> femaleSymModel;
    private final HumanoidModel<MillVillager> femaleAsymModel;

    public MillVillagerRenderer(EntityRendererProvider.Context context) {
        super(context, (EntityModel)new MillMaleModel(context.bakeLayer(MillModelLayers.MILL_VILLAGER_MALE)), 0.5f);
        this.maleModel = (HumanoidModel)this.model;
        this.femaleSymModel = new MillFemaleSymModel(context.bakeLayer(MillModelLayers.MILL_VILLAGER_FEMALE_SYM));
        this.femaleAsymModel = new MillFemaleAsymModel(context.bakeLayer(MillModelLayers.MILL_VILLAGER_FEMALE_ASYM));
        Map<ModelType, HumanoidModel<MillVillager>> clothModels0 = Map.of(ModelType.MALE, new MillMaleModel(context.bakeLayer(MillModelLayers.MILL_VILLAGER_MALE_CLOTH_0)), ModelType.FEMALE_SYM, new MillFemaleSymModel(context.bakeLayer(MillModelLayers.MILL_VILLAGER_FEMALE_SYM_CLOTH_0)), ModelType.FEMALE_ASYM, new MillFemaleAsymModel(context.bakeLayer(MillModelLayers.MILL_VILLAGER_FEMALE_ASYM_CLOTH_0)));
        Map<ModelType, HumanoidModel<MillVillager>> clothModels1 = Map.of(ModelType.MALE, new MillMaleModel(context.bakeLayer(MillModelLayers.MILL_VILLAGER_MALE_CLOTH_1)), ModelType.FEMALE_SYM, new MillFemaleSymModel(context.bakeLayer(MillModelLayers.MILL_VILLAGER_FEMALE_SYM_CLOTH_1)), ModelType.FEMALE_ASYM, new MillFemaleAsymModel(context.bakeLayer(MillModelLayers.MILL_VILLAGER_FEMALE_ASYM_CLOTH_1)));
        this.addLayer(new ClothingLayer((RenderLayerParent<MillVillager, HumanoidModel<MillVillager>>)this, clothModels0, 0));
        this.addLayer(new ClothingLayer((RenderLayerParent<MillVillager, HumanoidModel<MillVillager>>)this, clothModels1, 1));
        this.addLayer((RenderLayer)new HumanoidArmorLayer((RenderLayerParent)this, (HumanoidModel)new HumanoidArmorModel(context.bakeLayer(ModelLayers.PLAYER_INNER_ARMOR)), (HumanoidModel)new HumanoidArmorModel(context.bakeLayer(ModelLayers.PLAYER_OUTER_ARMOR)), context.getModelManager()));
        this.addLayer((RenderLayer)new ItemInHandLayer((RenderLayerParent)this, context.getItemInHandRenderer()));
    }

    public void render(MillVillager entity, float entityYaw, float partialTick, PoseStack poseStack, MultiBufferSource bufferSource, int packedLight) {
        this.model = switch (entity.getModelType()) {
            case ModelType.FEMALE_SYM -> this.femaleSymModel;
            case ModelType.FEMALE_ASYM -> this.femaleAsymModel;
            default -> this.maleModel;
        };
        ((HumanoidModel)this.model).rightArmPose = entity.getItemInHand(InteractionHand.MAIN_HAND).isEmpty() ? HumanoidModel.ArmPose.EMPTY : HumanoidModel.ArmPose.ITEM;
        ((HumanoidModel)this.model).leftArmPose = entity.getItemInHand(InteractionHand.OFF_HAND).isEmpty() ? HumanoidModel.ArmPose.EMPTY : HumanoidModel.ArmPose.ITEM;
        super.render((LivingEntity)entity, entityYaw, partialTick, poseStack, bufferSource, packedLight);
        List<ItemStack> floatingIcons = MillVillagerRenderer.defineFloatingIcons(entity);
        if (!floatingIcons.isEmpty()) {
            this.renderFloatingIcons(entity, floatingIcons, poseStack, bufferSource, packedLight);
        }
    }

    protected void setupRotations(MillVillager entity, PoseStack poseStack, float bob, float yBodyRot, float partialTick, float scale) {
        if (entity.isVillagerSleeping()) {
            float sleepRot = 180.0f - entity.getYRot();
            poseStack.mulPose(Axis.YP.rotationDegrees(sleepRot));
            poseStack.mulPose(Axis.ZP.rotationDegrees(this.getFlipDegrees((LivingEntity)entity)));
            poseStack.mulPose(Axis.YP.rotationDegrees(270.0f));
        } else {
            super.setupRotations((LivingEntity)entity, poseStack, bob, yBodyRot, partialTick, scale);
        }
    }

    protected void scale(MillVillager entity, PoseStack poseStack, float partialTick) {
        float s = entity.getVillagerScale();
        poseStack.scale(s, s, s);
    }

    public ResourceLocation getTextureLocation(MillVillager entity) {
        ResourceLocation tex = entity.getTexture();
        return tex != null ? tex : DEFAULT_TEXTURE;
    }

    protected boolean shouldShowName(MillVillager entity) {
        if (entity.isGuiPreviewMode()) {
            return false;
        }
        if (!((Boolean)MillenaireClientConfig.CLIENT.showNames.get()).booleanValue()) {
            return false;
        }
        String name = entity.getVillagerDisplayName();
        if (name == null || name.isEmpty()) {
            return false;
        }
        int dist = MillenaireClientConfig.CLIENT.namesDistance.getAsInt();
        double distanceSq = this.entityRenderDispatcher.distanceToSqr((Entity)entity);
        return distanceSq <= (double)dist * (double)dist;
    }

    protected void renderNameTag(MillVillager entity, Component content, PoseStack poseStack, MultiBufferSource bufferSource, int packedLight, float partialTick) {
        boolean hasSpeech;
        Object nameRoleStr;
        boolean hasRole;
        String displayName = entity.getVillagerDisplayName();
        if (displayName == null || displayName.isEmpty()) {
            return;
        }
        Font font = this.getFont();
        String nameStr = displayName.startsWith("entity.") || displayName.startsWith("role.") ? Component.translatable((String)displayName).getString() : displayName;
        String nativeRole = entity.getNativeRoleName();
        String roleName = entity.getRoleName();
        boolean bl = hasRole = nativeRole != null && !nativeRole.isEmpty();
        if (hasRole) {
            boolean canReadNames;
            ResourceLocation cultureId = entity.getCultureId();
            boolean bl2 = canReadNames = cultureId != null && ClientLanguageCache.canReadVillagerNames(cultureId);
            if (canReadNames && roleName != null && !roleName.isEmpty()) {
                String translatedRole = roleName.startsWith("role.") ? Component.translatable((String)roleName).getString() : roleName;
                nameRoleStr = DisplayNameResolver.equivalent(nativeRole, translatedRole) ? nameStr + ", " + nativeRole : nameStr + ", " + nativeRole + " (" + translatedRole + ")";
            } else {
                nameRoleStr = nameStr + ", " + nativeRole;
            }
        } else {
            nameRoleStr = nameStr;
        }
        MutableComponent nameRoleLine = Component.literal((String)nameRoleStr);
        String goalLabel = entity.getGoalLabel();
        boolean hasGoal = goalLabel != null && !goalLabel.isEmpty();
        Component goalLine = hasGoal ? DisplayUtils.resolveGoalLabelComponent(goalLabel) : null;
        String questLabel = ClientQuestCache.getQuestLabelForVillager(entity.getUUID());
        boolean hasQuest = questLabel != null;
        MutableComponent questLine = hasQuest ? Component.literal((String)("[" + questLabel + "]")) : null;
        Objects.requireNonNull(font);
        int lineHeight = 9 + 1;
        int bgColor = -1342177280;
        poseStack.pushPose();
        float scale = entity.getVillagerScale();
        float nameTagHeight = entity.isVillagerSleeping() ? 0.2f * scale + 0.5f : entity.getBbHeight() * scale + 0.5f;
        poseStack.translate(0.0, (double)nameTagHeight, 0.0);
        poseStack.mulPose(this.entityRenderDispatcher.cameraOrientation());
        poseStack.scale(0.025f, -0.025f, 0.025f);
        Matrix4f matrix = poseStack.last().pose();
        int emissiveLight = 0xF000F0;
        MultiBufferSource.BufferSource immediateBuffer = MultiBufferSource.immediate((ByteBufferBuilder)new ByteBufferBuilder(1536));
        float nameY = -lineHeight;
        float questY = nameY - (float)(hasQuest ? lineHeight : 0);
        float goalY = (hasQuest ? questY : nameY) - (float)(hasGoal ? lineHeight : 0);
        if (hasGoal) {
            float goalX = (float)(-font.width((FormattedText)goalLine)) / 2.0f;
            MillVillagerRenderer.drawNameLine(font, goalLine, goalX, goalY, -2307474, matrix, (MultiBufferSource)immediateBuffer, bgColor, emissiveLight);
        }
        if (hasQuest) {
            float questX = (float)(-font.width((FormattedText)questLine)) / 2.0f;
            MillVillagerRenderer.drawNameLine(font, (Component)questLine, questX, questY, -1596072483, matrix, (MultiBufferSource)immediateBuffer, bgColor, emissiveLight);
        }
        boolean hasHire = entity.clientIsHired();
        float hireY = (hasGoal ? goalY : (hasQuest ? questY : nameY)) - (float)(hasHire ? lineHeight : 0);
        if (hasHire) {
            String stanceKey = entity.clientIsAggressive() ? "gui.millenaire.hire.aggressive" : "gui.millenaire.hire.passive";
            String hireStr = Component.translatable((String)"gui.millenaire.hire.health", (Object[])new Object[]{String.valueOf((int)entity.getHealth() / 2), String.valueOf((int)entity.getMaxHealth() / 2)}).getString() + " \u2014 " + Component.translatable((String)stanceKey).getString() + " \u2014 " + Component.translatable((String)"gui.millenaire.hire.hiredvillager", (Object[])new Object[]{entity.clientHireHoursLeft()}).getString();
            MutableComponent hireLine = Component.literal((String)hireStr);
            float hireX = (float)(-font.width((FormattedText)hireLine)) / 2.0f;
            MillVillagerRenderer.drawNameLine(font, (Component)hireLine, hireX, hireY, -2307474, matrix, (MultiBufferSource)immediateBuffer, bgColor, emissiveLight);
        }
        float nameX = (float)(-font.width((FormattedText)nameRoleLine)) / 2.0f;
        MillVillagerRenderer.drawNameLine(font, (Component)nameRoleLine, nameX, nameY, -1, matrix, (MultiBufferSource)immediateBuffer, bgColor, emissiveLight);
        String speechText = entity.getSpeechText();
        boolean bl3 = hasSpeech = speechText != null && !speechText.isEmpty();
        if (hasSpeech) {
            int i;
            List translationLines;
            int speechBg = -1342177280;
            int maxSpeechChars = 60;
            String[] speechParts = MillVillagerRenderer.resolveSpeechLines(speechText, entity);
            List nativeLines = speechParts[0] != null ? MillVillagerRenderer.wordWrap(speechParts[0], maxSpeechChars) : List.of();
            List<Object> list = translationLines = speechParts[1] != null ? MillVillagerRenderer.wordWrap(speechParts[1], maxSpeechChars) : List.of();
            float topLine = hasHire ? hireY : (hasGoal ? goalY : (hasQuest ? questY : nameY));
            float currentY = topLine - (float)lineHeight - 2.0f;
            for (i = translationLines.size() - 1; i >= 0; --i) {
                MutableComponent transComp = Component.literal((String)((String)translationLines.get(i)));
                float transX = (float)(-font.width((FormattedText)transComp)) / 2.0f;
                MillVillagerRenderer.drawNameLine(font, (Component)transComp, transX, currentY, -5636096, matrix, (MultiBufferSource)immediateBuffer, speechBg, emissiveLight);
                currentY -= (float)lineHeight;
            }
            for (i = nativeLines.size() - 1; i >= 0; --i) {
                MutableComponent nativeComp = Component.literal((String)((String)nativeLines.get(i)));
                float nativeX = (float)(-font.width((FormattedText)nativeComp)) / 2.0f;
                MillVillagerRenderer.drawNameLine(font, (Component)nativeComp, nativeX, currentY, -11184641, matrix, (MultiBufferSource)immediateBuffer, speechBg, emissiveLight);
                currentY -= (float)lineHeight;
            }
        }
        immediateBuffer.endBatch();
        poseStack.popPose();
    }

    private static void drawNameLine(Font font, Component text, float x, float y, int color, Matrix4f matrix, MultiBufferSource bufferSource, int bgColor, int emissiveLight) {
        int seeThruColor = color & 0xFFFFFF | Integer.MIN_VALUE;
        font.drawInBatch(text, x, y, seeThruColor, false, matrix, bufferSource, Font.DisplayMode.SEE_THROUGH, bgColor, emissiveLight);
        font.drawInBatch(text, x, y, color, false, matrix, bufferSource, Font.DisplayMode.NORMAL, 0, emissiveLight);
    }

    private static String[] resolveSpeechLines(String speechRef, MillVillager entity) {
        return SpeechResolver.resolve(speechRef);
    }

    private static List<ItemStack> defineFloatingIcons(MillVillager entity) {
        ArrayList<ItemStack> icons = new ArrayList<ItemStack>();
        if (entity.isChief()) {
            icons.add(new ItemStack((ItemLike)Items.GOLDEN_HELMET));
        }
        if (entity.isSelling()) {
            icons.add(new ItemStack((ItemLike)ModItems.PURSE.get()));
        }
        if (entity.isForeignMerchant()) {
            icons.add(new ItemStack((ItemLike)ModItems.PURSE.get()));
        }
        return icons;
    }

    private void renderFloatingIcons(MillVillager entity, List<ItemStack> icons, PoseStack poseStack, MultiBufferSource bufferSource, int packedLight) {
        float scale = entity.getVillagerScale();
        float iconHeight = entity.getBbHeight() * scale + 1.3f;
        poseStack.pushPose();
        poseStack.translate(0.0, (double)iconHeight, 0.0);
        poseStack.mulPose(this.entityRenderDispatcher.cameraOrientation());
        poseStack.scale(0.5f, 0.5f, 0.5f);
        float iconSpacing = 0.8f;
        float startX = -((float)(icons.size() - 1) * iconSpacing) / 2.0f;
        for (int i = 0; i < icons.size(); ++i) {
            poseStack.pushPose();
            poseStack.translate((double)(startX + (float)i * iconSpacing), 0.0, 0.0);
            Minecraft.getInstance().getItemRenderer().renderStatic(icons.get(i), ItemDisplayContext.GUI, 0xF000F0, OverlayTexture.NO_OVERLAY, poseStack, bufferSource, entity.level(), entity.getId());
            poseStack.popPose();
        }
        poseStack.popPose();
    }

    private static List<String> wordWrap(String text, int maxChars) {
        if (text.length() <= maxChars) {
            return List.of(text);
        }
        ArrayList<String> lines = new ArrayList<String>();
        int start = 0;
        while (start < text.length()) {
            if (start + maxChars >= text.length()) {
                lines.add(text.substring(start));
                break;
            }
            int end = start + maxChars;
            int lastSpace = text.lastIndexOf(32, end);
            if (lastSpace <= start) {
                lastSpace = end;
            }
            lines.add(text.substring(start, lastSpace));
            start = lastSpace < text.length() && text.charAt(lastSpace) == ' ' ? lastSpace + 1 : lastSpace;
        }
        return lines;
    }
}

