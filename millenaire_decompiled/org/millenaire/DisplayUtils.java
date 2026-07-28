/*
 * Decompiled with CFR 0.150.
 * 
 * Could not load the following classes:
 *  javax.annotation.Nullable
 *  net.minecraft.network.chat.Component
 *  net.minecraft.resources.ResourceLocation
 */
package org.millenaire;

import javax.annotation.Nullable;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;

public final class DisplayUtils {
    private static final String GOAL_TRANSLATION_PREFIX = "goal.millenaire.";

    private DisplayUtils() {
    }

    public static String t(String key) {
        return Component.translatable((String)key).getString();
    }

    public static String t(String key, Object ... args) {
        return Component.translatable((String)key, (Object[])args).getString();
    }

    public static String resolveRoleName(ResourceLocation typeId) {
        String key = DisplayUtils.resolveRoleKey(typeId);
        String translated = Component.translatable((String)key).getString();
        String qualifiedRole = typeId.getPath().replace('/', '_');
        return translated.equals(key) ? qualifiedRole : translated;
    }

    public static String resolveRoleKey(ResourceLocation typeId) {
        String path = typeId.getPath();
        String qualifiedRole = path.replace('/', '_');
        return "role.millenaire." + qualifiedRole;
    }

    public static boolean isGoalTranslationKey(@Nullable String goalLabel) {
        return goalLabel != null && goalLabel.startsWith(GOAL_TRANSLATION_PREFIX);
    }

    public static String resolveGoalLabel(@Nullable String goalLabel) {
        if (goalLabel == null || goalLabel.isEmpty()) {
            return "";
        }
        return DisplayUtils.isGoalTranslationKey(goalLabel) ? Component.translatable((String)goalLabel).getString() : goalLabel;
    }

    public static Component resolveGoalLabelComponent(String goalLabel) {
        return DisplayUtils.isGoalTranslationKey(goalLabel) ? Component.translatable((String)goalLabel) : Component.literal((String)goalLabel);
    }
}

