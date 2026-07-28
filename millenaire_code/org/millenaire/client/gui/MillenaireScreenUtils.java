/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  javax.annotation.Nullable
 *  net.minecraft.client.Minecraft
 *  net.minecraft.client.gui.GuiGraphics
 *  net.minecraft.client.gui.screens.inventory.InventoryScreen
 *  net.minecraft.network.chat.Component
 *  net.minecraft.world.entity.Entity
 *  net.minecraft.world.entity.LivingEntity
 */
package org.millenaire.client.gui;

import javax.annotation.Nullable;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import org.millenaire.entity.MillVillager;

public final class MillenaireScreenUtils {
    public static final int COLOR_WHITE = 0xFFFFFF;
    public static final int COLOR_GRAY = 0x555555;
    public static final int COLOR_GREEN = 0x55FF55;
    public static final int COLOR_REP_POSITIVE = 0x55FF55;
    public static final int COLOR_REP_NEGATIVE = 0xFF5555;
    public static final int COLOR_REP_NEUTRAL = 0xCCCC00;
    public static final int COLOR_REP_DARKGREEN = 43520;
    public static final int COLOR_REP_DARKBLUE = 170;
    public static final int COLOR_REP_LIGHTRED = 0xFF5555;
    public static final int COLOR_REP_DARKRED = 0xAA0000;

    private MillenaireScreenUtils() {
    }

    @Nullable
    public static LivingEntity findEntityById(int entityId) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null) {
            return null;
        }
        Entity entity = mc.level.getEntity(entityId);
        if (entity instanceof LivingEntity) {
            LivingEntity living = (LivingEntity)entity;
            return living;
        }
        return null;
    }

    /*
     * WARNING - Removed try catching itself - possible behaviour change.
     */
    public static int renderVillagerPreview(GuiGraphics graphics, int panelX, int panelY, int panelWidth, int padding, int y, int mouseX, int mouseY, int entityId) {
        LivingEntity entity = MillenaireScreenUtils.findEntityById(entityId);
        if (entity == null) {
            return y;
        }
        int entityAreaHeight = 60;
        int centerX = panelX + panelWidth / 2;
        int entityY = panelY + padding + entityAreaHeight;
        boolean wasPreview = false;
        if (entity instanceof MillVillager) {
            MillVillager mv = (MillVillager)entity;
            wasPreview = mv.isGuiPreviewMode();
            mv.setGuiPreviewMode(true);
        }
        try {
            InventoryScreen.renderEntityInInventoryFollowsMouse((GuiGraphics)graphics, (int)(centerX - 25), (int)(panelY + padding + 5), (int)(centerX + 25), (int)entityY, (int)25, (float)0.0625f, (float)mouseX, (float)mouseY, (LivingEntity)entity);
        }
        catch (Exception e) {
            int n = y;
            return n;
        }
        finally {
            if (entity instanceof MillVillager) {
                MillVillager mv = (MillVillager)entity;
                mv.setGuiPreviewMode(wasPreview);
            }
        }
        return panelY + padding + entityAreaHeight + 4;
    }

    /*
     * WARNING - Removed try catching itself - possible behaviour change.
     */
    public static void renderStaticVillagerPreview(GuiGraphics graphics, int centerX, int topY, int bottomY, int scale, LivingEntity entity) {
        MillVillager mv;
        float fixedMouseX = centerX + 40;
        float fixedMouseY = (float)(topY + bottomY) / 2.0f - 10.0f;
        boolean wasPreview = false;
        if (entity instanceof MillVillager) {
            mv = (MillVillager)entity;
            wasPreview = mv.isGuiPreviewMode();
            mv.setGuiPreviewMode(true);
        }
        try {
            InventoryScreen.renderEntityInInventoryFollowsMouse((GuiGraphics)graphics, (int)(centerX - 30), (int)topY, (int)(centerX + 30), (int)bottomY, (int)scale, (float)0.0625f, (float)fixedMouseX, (float)fixedMouseY, (LivingEntity)entity);
        }
        catch (Exception mv2) {
        }
        finally {
            if (entity instanceof MillVillager) {
                mv = (MillVillager)entity;
                mv.setGuiPreviewMode(wasPreview);
            }
        }
    }

    public static int getReputationColor(int reputation) {
        if (reputation >= 32768) {
            return 43520;
        }
        if (reputation >= 4096) {
            return 170;
        }
        if (reputation < -256) {
            return 0xAA0000;
        }
        if (reputation < 0) {
            return 0xFF5555;
        }
        return 0xCCCC00;
    }

    public static String resolveReputationLabel(String reputationLabel) {
        if (reputationLabel.startsWith("reputation.")) {
            return Component.translatable((String)reputationLabel).getString();
        }
        return reputationLabel;
    }
}

