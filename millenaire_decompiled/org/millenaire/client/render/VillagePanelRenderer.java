/*
 * Decompiled with CFR 0.150.
 * 
 * Could not load the following classes:
 *  com.mojang.blaze3d.vertex.PoseStack
 *  com.mojang.blaze3d.vertex.VertexConsumer
 *  com.mojang.logging.LogUtils
 *  com.mojang.math.Axis
 *  net.minecraft.client.Minecraft
 *  net.minecraft.client.gui.Font
 *  net.minecraft.client.gui.Font$DisplayMode
 *  net.minecraft.client.renderer.MultiBufferSource
 *  net.minecraft.client.renderer.MultiBufferSource$BufferSource
 *  net.minecraft.client.renderer.RenderType
 *  net.minecraft.client.renderer.blockentity.BlockEntityRenderer
 *  net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider$Context
 *  net.minecraft.client.renderer.entity.ItemRenderer
 *  net.minecraft.client.resources.language.I18n
 *  net.minecraft.core.Direction
 *  net.minecraft.network.chat.Component
 *  net.minecraft.network.chat.FormattedText
 *  net.minecraft.network.chat.MutableComponent
 *  net.minecraft.resources.ResourceLocation
 *  net.minecraft.world.item.Item
 *  net.minecraft.world.item.ItemDisplayContext
 *  net.minecraft.world.item.ItemStack
 *  net.minecraft.world.level.ItemLike
 *  org.joml.Matrix4f
 *  org.slf4j.Logger
 */
package org.millenaire.client.render;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.logging.LogUtils;
import com.mojang.math.Axis;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.entity.ItemRenderer;
import net.minecraft.client.resources.language.I18n;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.FormattedText;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ItemLike;
import org.joml.Matrix4f;
import org.millenaire.block.VillagePanelBlock;
import org.millenaire.block.VillagePanelBlockEntity;
import org.millenaire.culture.Culture;
import org.millenaire.culture.ModCultures;
import org.millenaire.item.ItemHelper;
import org.millenaire.language.DisplayNameResolver;
import org.millenaire.village.panel.PanelContentGenerator;
import org.slf4j.Logger;

public class VillagePanelRenderer
implements BlockEntityRenderer<VillagePanelBlockEntity> {
    private static final Logger LOGGER = LogUtils.getLogger();
    private static final float TEXT_SCALE = 0.010416667f;
    private static final int TEXT_COLOR = -16777216;
    private static final int MAX_LINES = 8;
    private static final int MAX_TEXT_WIDTH = 80;
    private static final int LINE_SPACING = 10;
    private static final int START_Y = -15;
    private static final int LEFT_X = -29;
    private static final int RIGHT_COLUMN_X = 11;
    private static final ResourceLocation DEFAULT_TEXTURE = ResourceLocation.fromNamespaceAndPath((String)"millenaire", (String)"textures/entity/panels/default.png");
    private static final float PANEL_HALF_W = 0.5f;
    private static final float PANEL_HALF_H = 0.5f;
    private static final float PANEL_DEPTH = 0.083333336f;
    private static final float FRONT_U0 = 0.4375f;
    private static final float FRONT_V0 = 0.0625f;
    private static final float FRONT_U1 = 0.8125f;
    private static final float FRONT_V1 = 0.8125f;
    private static final float BACK_U0 = 0.03125f;
    private static final float BACK_V0 = 0.0625f;
    private static final float BACK_U1 = 0.40625f;
    private static final float BACK_V1 = 0.8125f;
    private static final float LEFT_U0 = 0.0f;
    private static final float LEFT_V0 = 0.0625f;
    private static final float LEFT_U1 = 0.03125f;
    private static final float LEFT_V1 = 0.8125f;
    private static final float RIGHT_U0 = 0.40625f;
    private static final float RIGHT_V0 = 0.0625f;
    private static final float RIGHT_U1 = 0.4375f;
    private static final float RIGHT_V1 = 0.8125f;
    private static final float TOP_U0 = 0.03125f;
    private static final float TOP_V0 = 0.0f;
    private static final float TOP_U1 = 0.40625f;
    private static final float TOP_V1 = 0.0625f;
    private static final float BOT_U0 = 0.40625f;
    private static final float BOT_V0 = 0.0f;
    private static final float BOT_U1 = 0.78125f;
    private static final float BOT_V1 = 0.0625f;
    private final Font font;

    public VillagePanelRenderer(BlockEntityRendererProvider.Context context) {
        this.font = context.getFont();
    }

    public void render(VillagePanelBlockEntity blockEntity, float partialTick, PoseStack poseStack, MultiBufferSource bufferSource, int packedLight, int packedOverlay) {
        LayoutRow last;
        Direction facing = Direction.NORTH;
        if (blockEntity.getBlockState().hasProperty(VillagePanelBlock.FACING)) {
            facing = (Direction)blockEntity.getBlockState().getValue(VillagePanelBlock.FACING);
        }
        poseStack.pushPose();
        poseStack.translate(0.5, 0.5, 0.5);
        poseStack.mulPose(Axis.YP.rotationDegrees(-facing.toYRot()));
        VillagePanelRenderer.renderBackgroundQuad(blockEntity, poseStack, bufferSource, packedLight, packedOverlay);
        List<PanelContentGenerator.DisplayLine> lines = blockEntity.getDisplayLines();
        if (lines.isEmpty()) {
            poseStack.popPose();
            return;
        }
        poseStack.translate(0.0, 0.0, -0.4375);
        poseStack.translate(0.0f, 0.25f, 0.046666667f);
        poseStack.scale(0.010416667f, -0.010416667f, 0.010416667f);
        Matrix4f matrix = poseStack.last().pose();
        int lineCount = Math.min(lines.size(), 8);
        ArrayList<LayoutRow> rows = new ArrayList<LayoutRow>();
        for (int i = 0; i < lineCount; ++i) {
            PanelContentGenerator.DisplayLine line = lines.get(i);
            if (VillagePanelRenderer.isColumnMode(line)) {
                rows.add(new LayoutRow(i, null, false));
                continue;
            }
            String text = this.resolveFullLineText(line);
            int maxWidth = !line.leftIcon().isEmpty() ? 62 : 80;
            List<String> subLines = this.wrapToWidth(text, maxWidth);
            for (String sub : subLines) {
                rows.add(new LayoutRow(i, sub, line.centered()));
            }
        }
        if (rows.size() > 8 && (last = (LayoutRow)(rows = new ArrayList(rows.subList(0, 8))).get(7)).text() != null) {
            rows.set(7, new LayoutRow(last.lineIndex(), this.truncateWithEllipsis(last.text(), 80), last.centered()));
        }
        float blockCenter = -15.0f + (float)(lineCount - 1) / 2.0f * 10.0f;
        float startY = blockCenter - (float)(rows.size() - 1) / 2.0f * 10.0f;
        float[] firstRowY = new float[lineCount];
        Arrays.fill(firstRowY, Float.NaN);
        for (int r = 0; r < rows.size(); ++r) {
            LayoutRow row = (LayoutRow)rows.get(r);
            float y = startY + (float)(r * 10);
            if (row.lineIndex() < firstRowY.length && Float.isNaN(firstRowY[row.lineIndex()])) {
                firstRowY[row.lineIndex()] = y;
            }
            if (row.text() == null) {
                PanelContentGenerator.DisplayLine line = lines.get(row.lineIndex());
                if (!line.leftColumn().isEmpty()) {
                    String leftText = this.truncateWithEllipsis(VillagePanelRenderer.stripFormattingCodes(line.leftColumn()), 32);
                    this.font.drawInBatch((Component)Component.literal((String)leftText), -29.0f, y, -16777216, false, matrix, bufferSource, Font.DisplayMode.POLYGON_OFFSET, 0, packedLight);
                }
                if (line.rightColumn().isEmpty()) continue;
                String rightText = this.truncateWithEllipsis(VillagePanelRenderer.stripFormattingCodes(line.rightColumn()), 32);
                this.font.drawInBatch((Component)Component.literal((String)rightText), 11.0f, y, -16777216, false, matrix, bufferSource, Font.DisplayMode.POLYGON_OFFSET, 0, packedLight);
                continue;
            }
            MutableComponent comp = Component.literal((String)row.text());
            float x = row.centered() ? (float)(-this.font.width((FormattedText)comp)) / 2.0f : -29.0f;
            this.font.drawInBatch((Component)comp, x, y, -16777216, false, matrix, bufferSource, Font.DisplayMode.POLYGON_OFFSET, 0, packedLight);
        }
        this.renderIcons(blockEntity, lines, firstRowY, poseStack, bufferSource, packedLight, packedOverlay);
        poseStack.popPose();
    }

    private static void renderBackgroundQuad(VillagePanelBlockEntity blockEntity, PoseStack poseStack, MultiBufferSource bufferSource, int packedLight, int packedOverlay) {
        ResourceLocation texture = VillagePanelRenderer.resolveTexture(blockEntity.getCultureId());
        VertexConsumer consumer = bufferSource.getBuffer(RenderType.entityCutoutNoCull((ResourceLocation)texture));
        Matrix4f matrix = poseStack.last().pose();
        float zMid = -0.4375f;
        float zFront = zMid - 0.041666668f;
        float zBack = zMid + 0.041666668f;
        float x0 = -0.5f;
        float x1 = 0.5f;
        float y0 = -0.5f;
        float y1 = 0.5f;
        consumer.addVertex(matrix, x0, y1, zFront).setColor(255, 255, 255, 255).setUv(0.4375f, 0.0625f).setOverlay(packedOverlay).setLight(packedLight).setNormal(0.0f, 0.0f, -1.0f);
        consumer.addVertex(matrix, x0, y0, zFront).setColor(255, 255, 255, 255).setUv(0.4375f, 0.8125f).setOverlay(packedOverlay).setLight(packedLight).setNormal(0.0f, 0.0f, -1.0f);
        consumer.addVertex(matrix, x1, y0, zFront).setColor(255, 255, 255, 255).setUv(0.8125f, 0.8125f).setOverlay(packedOverlay).setLight(packedLight).setNormal(0.0f, 0.0f, -1.0f);
        consumer.addVertex(matrix, x1, y1, zFront).setColor(255, 255, 255, 255).setUv(0.8125f, 0.0625f).setOverlay(packedOverlay).setLight(packedLight).setNormal(0.0f, 0.0f, -1.0f);
        consumer.addVertex(matrix, x1, y1, zBack).setColor(255, 255, 255, 255).setUv(0.03125f, 0.0625f).setOverlay(packedOverlay).setLight(packedLight).setNormal(0.0f, 0.0f, 1.0f);
        consumer.addVertex(matrix, x1, y0, zBack).setColor(255, 255, 255, 255).setUv(0.03125f, 0.8125f).setOverlay(packedOverlay).setLight(packedLight).setNormal(0.0f, 0.0f, 1.0f);
        consumer.addVertex(matrix, x0, y0, zBack).setColor(255, 255, 255, 255).setUv(0.40625f, 0.8125f).setOverlay(packedOverlay).setLight(packedLight).setNormal(0.0f, 0.0f, 1.0f);
        consumer.addVertex(matrix, x0, y1, zBack).setColor(255, 255, 255, 255).setUv(0.40625f, 0.0625f).setOverlay(packedOverlay).setLight(packedLight).setNormal(0.0f, 0.0f, 1.0f);
        consumer.addVertex(matrix, x0, y1, zBack).setColor(255, 255, 255, 255).setUv(0.03125f, 0.0f).setOverlay(packedOverlay).setLight(packedLight).setNormal(0.0f, 1.0f, 0.0f);
        consumer.addVertex(matrix, x0, y1, zFront).setColor(255, 255, 255, 255).setUv(0.03125f, 0.0625f).setOverlay(packedOverlay).setLight(packedLight).setNormal(0.0f, 1.0f, 0.0f);
        consumer.addVertex(matrix, x1, y1, zFront).setColor(255, 255, 255, 255).setUv(0.40625f, 0.0625f).setOverlay(packedOverlay).setLight(packedLight).setNormal(0.0f, 1.0f, 0.0f);
        consumer.addVertex(matrix, x1, y1, zBack).setColor(255, 255, 255, 255).setUv(0.40625f, 0.0f).setOverlay(packedOverlay).setLight(packedLight).setNormal(0.0f, 1.0f, 0.0f);
        consumer.addVertex(matrix, x0, y0, zFront).setColor(255, 255, 255, 255).setUv(0.40625f, 0.0f).setOverlay(packedOverlay).setLight(packedLight).setNormal(0.0f, -1.0f, 0.0f);
        consumer.addVertex(matrix, x0, y0, zBack).setColor(255, 255, 255, 255).setUv(0.40625f, 0.0625f).setOverlay(packedOverlay).setLight(packedLight).setNormal(0.0f, -1.0f, 0.0f);
        consumer.addVertex(matrix, x1, y0, zBack).setColor(255, 255, 255, 255).setUv(0.78125f, 0.0625f).setOverlay(packedOverlay).setLight(packedLight).setNormal(0.0f, -1.0f, 0.0f);
        consumer.addVertex(matrix, x1, y0, zFront).setColor(255, 255, 255, 255).setUv(0.78125f, 0.0f).setOverlay(packedOverlay).setLight(packedLight).setNormal(0.0f, -1.0f, 0.0f);
        consumer.addVertex(matrix, x0, y1, zBack).setColor(255, 255, 255, 255).setUv(0.0f, 0.0625f).setOverlay(packedOverlay).setLight(packedLight).setNormal(-1.0f, 0.0f, 0.0f);
        consumer.addVertex(matrix, x0, y0, zBack).setColor(255, 255, 255, 255).setUv(0.0f, 0.8125f).setOverlay(packedOverlay).setLight(packedLight).setNormal(-1.0f, 0.0f, 0.0f);
        consumer.addVertex(matrix, x0, y0, zFront).setColor(255, 255, 255, 255).setUv(0.03125f, 0.8125f).setOverlay(packedOverlay).setLight(packedLight).setNormal(-1.0f, 0.0f, 0.0f);
        consumer.addVertex(matrix, x0, y1, zFront).setColor(255, 255, 255, 255).setUv(0.03125f, 0.0625f).setOverlay(packedOverlay).setLight(packedLight).setNormal(-1.0f, 0.0f, 0.0f);
        consumer.addVertex(matrix, x1, y1, zFront).setColor(255, 255, 255, 255).setUv(0.40625f, 0.0625f).setOverlay(packedOverlay).setLight(packedLight).setNormal(1.0f, 0.0f, 0.0f);
        consumer.addVertex(matrix, x1, y0, zFront).setColor(255, 255, 255, 255).setUv(0.40625f, 0.8125f).setOverlay(packedOverlay).setLight(packedLight).setNormal(1.0f, 0.0f, 0.0f);
        consumer.addVertex(matrix, x1, y0, zBack).setColor(255, 255, 255, 255).setUv(0.4375f, 0.8125f).setOverlay(packedOverlay).setLight(packedLight).setNormal(1.0f, 0.0f, 0.0f);
        consumer.addVertex(matrix, x1, y1, zBack).setColor(255, 255, 255, 255).setUv(0.4375f, 0.0625f).setOverlay(packedOverlay).setLight(packedLight).setNormal(1.0f, 0.0f, 0.0f);
    }

    private static ResourceLocation resolveTexture(ResourceLocation cultureId) {
        if (cultureId == null) {
            return DEFAULT_TEXTURE;
        }
        Culture culture = ModCultures.getCulture(cultureId);
        if (culture == null) {
            return DEFAULT_TEXTURE;
        }
        return culture.panelTexture();
    }

    private void renderIcons(VillagePanelBlockEntity blockEntity, List<PanelContentGenerator.DisplayLine> lines, float[] firstRowY, PoseStack poseStack, MultiBufferSource bufferSource, int packedLight, int packedOverlay) {
        ItemRenderer itemRenderer = Minecraft.getInstance().getItemRenderer();
        MultiBufferSource.BufferSource immediate = Minecraft.getInstance().renderBuffers().bufferSource();
        float iconSize = 10.0f;
        float iconLeftX = -48.0f;
        float iconRightX = 48.0f - iconSize;
        for (int i = 0; i < lines.size() && i < firstRowY.length; ++i) {
            PanelContentGenerator.DisplayLine line = lines.get(i);
            float y = firstRowY[i];
            if (Float.isNaN(y)) continue;
            if (!line.leftIconStack().isEmpty()) {
                VillagePanelRenderer.drawSingleIconStack(blockEntity, line.leftIconStack(), iconLeftX, y, iconSize, poseStack, (MultiBufferSource)immediate, packedLight, packedOverlay, itemRenderer);
            } else if (!line.leftIcon().isEmpty()) {
                VillagePanelRenderer.drawSingleIcon(blockEntity, line.leftIcon(), iconLeftX, y, iconSize, poseStack, (MultiBufferSource)immediate, packedLight, packedOverlay, itemRenderer);
            }
            if (!line.middleIcon().isEmpty()) {
                VillagePanelRenderer.drawSingleIcon(blockEntity, line.middleIcon(), -iconSize / 2.0f, y, iconSize, poseStack, (MultiBufferSource)immediate, packedLight, packedOverlay, itemRenderer);
            }
            if (!line.rightIconStack().isEmpty()) {
                VillagePanelRenderer.drawSingleIconStack(blockEntity, line.rightIconStack(), iconRightX, y, iconSize, poseStack, (MultiBufferSource)immediate, packedLight, packedOverlay, itemRenderer);
                continue;
            }
            if (line.rightIcon().isEmpty()) continue;
            VillagePanelRenderer.drawSingleIcon(blockEntity, line.rightIcon(), iconRightX, y, iconSize, poseStack, (MultiBufferSource)immediate, packedLight, packedOverlay, itemRenderer);
        }
        immediate.endBatch();
    }

    private static void drawSingleIcon(VillagePanelBlockEntity blockEntity, String iconId, float x, float y, float size, PoseStack poseStack, MultiBufferSource bufferSource, int packedLight, int packedOverlay, ItemRenderer itemRenderer) {
        if (iconId == null || iconId.isEmpty()) {
            return;
        }
        ItemStack iconStack = VillagePanelRenderer.resolveItemStack(iconId);
        if (iconStack.isEmpty()) {
            return;
        }
        poseStack.pushPose();
        poseStack.translate(x + size / 2.0f, y + size / 2.0f, 0.0f);
        poseStack.scale(size, -size, size);
        itemRenderer.renderStatic(iconStack, ItemDisplayContext.FIXED, packedLight, packedOverlay, poseStack, bufferSource, blockEntity.getLevel(), 0);
        poseStack.popPose();
    }

    private static void drawSingleIconStack(VillagePanelBlockEntity blockEntity, ItemStack iconStack, float x, float y, float size, PoseStack poseStack, MultiBufferSource bufferSource, int packedLight, int packedOverlay, ItemRenderer itemRenderer) {
        if (iconStack.isEmpty()) {
            return;
        }
        poseStack.pushPose();
        poseStack.translate(x + size / 2.0f, y + size / 2.0f, 0.0f);
        poseStack.scale(size, -size, size);
        itemRenderer.renderStatic(iconStack, ItemDisplayContext.FIXED, packedLight, packedOverlay, poseStack, bufferSource, blockEntity.getLevel(), 0);
        poseStack.popPose();
    }

    private static ItemStack resolveItemStack(String itemId) {
        try {
            Item item = ItemHelper.resolve(itemId);
            return item != null ? new ItemStack((ItemLike)item) : ItemStack.EMPTY;
        }
        catch (Exception e) {
            LOGGER.warn("Failed to resolve item stack for '{}': {}", (Object)itemId, (Object)e.getMessage());
            return ItemStack.EMPTY;
        }
    }

    private static boolean isColumnMode(PanelContentGenerator.DisplayLine line) {
        return !line.leftColumn().isEmpty() || !line.rightColumn().isEmpty();
    }

    private String resolveFullLineText(PanelContentGenerator.DisplayLine line) {
        String text = line.text();
        if (line.translatable() && !text.isEmpty()) {
            String originalKey = text;
            String resolved = I18n.get((String)text, (Object[])new Object[0]);
            text = line.nativePrefix() != null ? DisplayNameResolver.resolve(resolved, true, line.nativePrefix(), originalKey) : resolved;
        }
        return VillagePanelRenderer.stripFormattingCodes(text);
    }

    private List<String> wrapToWidth(String text, int maxWidth) {
        ArrayList<String> out = new ArrayList<String>();
        if (text.isEmpty()) {
            out.add("");
            return out;
        }
        if (this.font.width(text) <= maxWidth) {
            out.add(text);
            return out;
        }
        StringBuilder line = new StringBuilder();
        for (String word : text.split(" ", -1)) {
            if (line.length() == 0) {
                if (this.font.width(word) <= maxWidth) {
                    line.append(word);
                    continue;
                }
                this.hardBreak(word, maxWidth, out, line);
                continue;
            }
            String candidate = String.valueOf(line) + " " + word;
            if (this.font.width(candidate) <= maxWidth) {
                line.append(' ').append(word);
                continue;
            }
            out.add(line.toString());
            line.setLength(0);
            if (this.font.width(word) <= maxWidth) {
                line.append(word);
                continue;
            }
            this.hardBreak(word, maxWidth, out, line);
        }
        if (line.length() > 0) {
            out.add(line.toString());
        }
        if (out.isEmpty()) {
            out.add("");
        }
        return out;
    }

    private void hardBreak(String word, int maxWidth, List<String> out, StringBuilder line) {
        StringBuilder piece = new StringBuilder();
        for (int k = 0; k < word.length(); ++k) {
            char c = word.charAt(k);
            if (piece.length() > 0 && this.font.width(piece.toString() + c) > maxWidth) {
                out.add(piece.toString());
                piece.setLength(0);
            }
            piece.append(c);
        }
        line.append((CharSequence)piece);
    }

    private String truncateWithEllipsis(String text, int maxWidth) {
        if (this.font.width(text) <= maxWidth) {
            return text;
        }
        String ellipsis = "...";
        while (text.length() > 0 && this.font.width(text + ellipsis) > maxWidth) {
            text = text.substring(0, text.length() - 1);
        }
        return text + ellipsis;
    }

    private static String stripFormattingCodes(String text) {
        if (text == null || text.isEmpty()) {
            return text;
        }
        StringBuilder sb = new StringBuilder(text.length());
        for (int i = 0; i < text.length(); ++i) {
            char c = text.charAt(i);
            if (c == '\u00a7' && i + 1 < text.length()) {
                ++i;
                continue;
            }
            sb.append(c);
        }
        return sb.toString();
    }

    private record LayoutRow(int lineIndex, String text, boolean centered) {
    }
}

