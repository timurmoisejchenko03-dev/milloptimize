/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  javax.annotation.Nullable
 */
package org.millenaire.village;

import javax.annotation.Nullable;
import org.millenaire.village.TravelBookScreenState;

public record TravelBookLine(String text, boolean isSeparator, @Nullable String leftColumn, @Nullable String rightColumn, @Nullable String leftIcon, boolean translatable, @Nullable String nativePrefix, @Nullable TravelBookNavTarget navTarget) {
    public static TravelBookLine text(String text) {
        return new TravelBookLine(text, false, null, null, null, false, null, null);
    }

    public static TravelBookLine translatable(String key) {
        return new TravelBookLine(key, false, null, null, null, true, null, null);
    }

    public static TravelBookLine separator() {
        return new TravelBookLine("", true, null, null, null, false, null, null);
    }

    public static TravelBookLine columns(String left, String right) {
        return new TravelBookLine("", false, left, right, null, false, null, null);
    }

    public static TravelBookLine withIcon(String left, String right, String iconItem) {
        return new TravelBookLine("", false, left, right, iconItem, false, null, null);
    }

    public static TravelBookLine clickable(String text, TravelBookNavTarget target) {
        return new TravelBookLine(text, false, null, null, null, false, null, target);
    }

    public static TravelBookLine clickableTranslatable(String key, TravelBookNavTarget target) {
        return new TravelBookLine(key, false, null, null, null, true, null, target);
    }

    public static TravelBookLine clickableColumns(String left, String right, TravelBookNavTarget target) {
        return new TravelBookLine("", false, left, right, null, false, null, target);
    }

    public static TravelBookLine clickableWithIcon(String left, String right, String icon, TravelBookNavTarget target) {
        return new TravelBookLine("", false, left, right, icon, false, null, target);
    }

    public static TravelBookLine withTranslation(String nativePrefix, String translationKey) {
        return new TravelBookLine(translationKey, false, null, null, null, true, nativePrefix, null);
    }

    public static TravelBookLine clickableWithTranslation(String nativePrefix, String translationKey, TravelBookNavTarget target) {
        return new TravelBookLine(translationKey, false, null, null, null, true, nativePrefix, target);
    }

    public boolean isColumns() {
        return this.leftColumn != null;
    }

    public boolean isClickable() {
        return this.navTarget != null;
    }

    public record TravelBookNavTarget(TravelBookScreenState targetState, @Nullable String cultureKey, @Nullable String categoryKey, @Nullable String itemKey) {
    }
}

