/*
 * Decompiled with CFR 0.150.
 * 
 * Could not load the following classes:
 *  javax.annotation.Nullable
 *  net.minecraft.world.item.ItemStack
 */
package org.millenaire.village.panel;

import java.util.List;
import javax.annotation.Nullable;
import net.minecraft.world.item.ItemStack;

public record PanelLine(String text, boolean isSeparator, @Nullable String leftColumn, @Nullable String rightColumn, @Nullable String leftIcon, boolean translatable, @Nullable PanelNavTarget navTarget, @Nullable String[] translatableArgs, int color, boolean bold, int translatableArgMask, @Nullable String nativePrefix, ItemStack leftIconStack) {
    public static final int DEFAULT_COLOR = -1;

    public PanelLine {
        if (translatableArgs != null && translatableArgs.length > 8) {
            throw new IllegalArgumentException("PanelLine supports max 8 translatable args, got " + translatableArgs.length);
        }
        if (translatableArgMask < 0 || translatableArgMask > 255) {
            throw new IllegalArgumentException("translatableArgMask must fit in 1 byte, got " + translatableArgMask);
        }
    }

    public PanelLine(String text, boolean isSeparator, @Nullable String leftColumn, @Nullable String rightColumn, @Nullable String leftIcon, boolean translatable, @Nullable PanelNavTarget navTarget, @Nullable String[] translatableArgs, int color, boolean bold, int translatableArgMask, @Nullable String nativePrefix) {
        this(text, isSeparator, leftColumn, rightColumn, leftIcon, translatable, navTarget, translatableArgs, color, bold, translatableArgMask, nativePrefix, ItemStack.EMPTY);
    }

    public PanelLine(String text, boolean isSeparator, @Nullable String leftColumn, @Nullable String rightColumn, @Nullable String leftIcon, boolean translatable, @Nullable PanelNavTarget navTarget, @Nullable String[] translatableArgs, int color, boolean bold, int translatableArgMask) {
        this(text, isSeparator, leftColumn, rightColumn, leftIcon, translatable, navTarget, translatableArgs, color, bold, translatableArgMask, null);
    }

    public PanelLine(String text, boolean isSeparator, @Nullable String leftColumn, @Nullable String rightColumn, @Nullable String leftIcon, boolean translatable, @Nullable PanelNavTarget navTarget, @Nullable String[] translatableArgs, int color, boolean bold) {
        this(text, isSeparator, leftColumn, rightColumn, leftIcon, translatable, navTarget, translatableArgs, color, bold, 0);
    }

    public PanelLine(String text, boolean isSeparator, @Nullable String leftColumn, @Nullable String rightColumn, @Nullable String leftIcon, boolean translatable, @Nullable PanelNavTarget navTarget, @Nullable String[] translatableArgs) {
        this(text, isSeparator, leftColumn, rightColumn, leftIcon, translatable, navTarget, translatableArgs, -1, false, 0);
    }

    public PanelLine(String text, boolean isSeparator, @Nullable String leftColumn, @Nullable String rightColumn, @Nullable String leftIcon, boolean translatable, @Nullable PanelNavTarget navTarget) {
        this(text, isSeparator, leftColumn, rightColumn, leftIcon, translatable, navTarget, null);
    }

    public PanelLine(String text, boolean isSeparator, @Nullable String leftColumn, @Nullable String rightColumn, @Nullable String leftIcon, boolean translatable) {
        this(text, isSeparator, leftColumn, rightColumn, leftIcon, translatable, null);
    }

    public PanelLine(String text, boolean isSeparator, @Nullable String leftColumn, @Nullable String rightColumn, @Nullable String leftIcon) {
        this(text, isSeparator, leftColumn, rightColumn, leftIcon, false, null);
    }

    public static PanelLine text(String text) {
        return new PanelLine(text, false, null, null, null, false, null, null);
    }

    public static PanelLine colored(String text, int color) {
        return new PanelLine(text, false, null, null, null, false, null, null, color, false);
    }

    public static PanelLine bold(String text) {
        return new PanelLine(text, false, null, null, null, false, null, null, -1, true);
    }

    public static PanelLine boldColored(String text, int color) {
        return new PanelLine(text, false, null, null, null, false, null, null, color, true);
    }

    public static PanelLine empty() {
        return new PanelLine("", false, null, null, null, false, null, null);
    }

    public static void addButtonPlaceholder(List<PanelLine> lines) {
        lines.add(PanelLine.empty());
        lines.add(PanelLine.empty());
    }

    public static PanelLine translatable(String key) {
        return new PanelLine(key, false, null, null, null, true, null, null);
    }

    public static PanelLine translatableWithArgs(String key, String ... args) {
        return new PanelLine(key, false, null, null, null, true, null, (String[])(args.length > 0 ? args : null), -1, false, 0);
    }

    public static PanelLine translatableWithMixedArgs(String key, int argMask, String ... args) {
        if (args.length > 8) {
            throw new IllegalArgumentException("PanelLine supports max 8 args (bitmask is 1 byte), got " + args.length);
        }
        return new PanelLine(key, false, null, null, null, true, null, (String[])(args.length > 0 ? args : null), -1, false, argMask);
    }

    public static PanelLine clickableTranslatable(String key, PanelNavTarget target) {
        return new PanelLine(key, false, null, null, null, true, target, null, -1, false, 0);
    }

    public static PanelLine clickableTranslatableColored(String key, PanelNavTarget target, int color) {
        return new PanelLine(key, false, null, null, null, true, target, null, color, false, 0);
    }

    public static PanelLine clickableTranslatableWithArgs(String key, PanelNavTarget target, String ... args) {
        return new PanelLine(key, false, null, null, null, true, target, (String[])(args.length > 0 ? args : null), -1, false, 0);
    }

    public static PanelLine clickableTranslatableWithMixedArgs(String key, PanelNavTarget target, int argMask, String ... args) {
        return new PanelLine(key, false, null, null, null, true, target, (String[])(args.length > 0 ? args : null), -1, false, argMask);
    }

    public static PanelLine translatableColored(String key, int color) {
        return new PanelLine(key, false, null, null, null, true, null, null, color, false, 0);
    }

    public static PanelLine translatableWithArgsColored(String key, int color, String ... args) {
        return new PanelLine(key, false, null, null, null, true, null, (String[])(args.length > 0 ? args : null), color, false, 0);
    }

    public static PanelLine translatableWithMixedArgsColored(String key, int color, int argMask, String ... args) {
        return new PanelLine(key, false, null, null, null, true, null, (String[])(args.length > 0 ? args : null), color, false, argMask);
    }

    public static PanelLine withIconTranslatable(String itemDescKey, String right, String iconItem, int color) {
        return new PanelLine(itemDescKey, false, null, right, iconItem, true, null, null, color, false, 0);
    }

    public static PanelLine clickableWithIconTranslatable(String itemDescKey, String right, String iconItem, PanelNavTarget target, int color) {
        return new PanelLine(itemDescKey, false, null, right, iconItem, true, target, null, color, false, 0);
    }

    public static PanelLine separator() {
        return new PanelLine("", true, null, null, null, false, null, null);
    }

    public static PanelLine columns(String left, String right) {
        return new PanelLine("", false, left, right, null, false, null, null);
    }

    public static PanelLine withIcon(String left, String right, String iconItem) {
        return new PanelLine("", false, left, right, iconItem, false, null, null);
    }

    public static PanelLine withIconStack(String left, String right, ItemStack stack) {
        return new PanelLine("", false, left, right, null, false, null, null, -1, false, 0, null, stack);
    }

    public static PanelLine clickableText(String text, PanelNavTarget target) {
        return new PanelLine(text, false, null, null, null, false, target, null);
    }

    public static PanelLine clickableWithIcon(String left, String right, String iconItem, PanelNavTarget target) {
        return new PanelLine("", false, left, right, iconItem, false, target, null);
    }

    public static PanelLine withTranslation(String nativePrefix, String translationKey) {
        return new PanelLine(translationKey, false, null, null, null, true, null, null, -1, false, 0, nativePrefix);
    }

    public static PanelLine clickableWithTranslation(String nativePrefix, String translationKey, PanelNavTarget target) {
        return new PanelLine(translationKey, false, null, null, null, true, target, null, -1, false, 0, nativePrefix);
    }

    public static PanelLine withTranslationColored(String nativePrefix, String translationKey, int color) {
        return new PanelLine(translationKey, false, null, null, null, true, null, null, color, false, 0, nativePrefix);
    }

    public static PanelLine clickableWithTranslationColored(String nativePrefix, String translationKey, PanelNavTarget target, int color) {
        return new PanelLine(translationKey, false, null, null, null, true, target, null, color, false, 0, nativePrefix);
    }

    public boolean isColumns() {
        return this.leftColumn != null;
    }

    public boolean isClickable() {
        return this.navTarget != null;
    }

    public record PanelNavTarget(String targetStateName, String cultureKey, String categoryKey, String itemKey) {
    }
}

