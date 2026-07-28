/*
 * Decompiled with CFR 0.150.
 * 
 * Could not load the following classes:
 *  javax.annotation.Nullable
 *  net.minecraft.client.Minecraft
 *  net.minecraft.resources.ResourceLocation
 *  net.neoforged.api.distmarker.Dist
 *  net.neoforged.api.distmarker.OnlyIn
 */
package org.millenaire.language;

import java.util.HashSet;
import javax.annotation.Nullable;
import net.minecraft.client.Minecraft;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import org.millenaire.client.ClientLanguageCache;
import org.millenaire.dialogue.DialogueLoader;
import org.millenaire.dialogue.SentenceLoader;
import org.millenaire.language.DisplayNameResolver;
import org.millenaire.language.LocaleResolver;
import org.millenaire.language.SentenceRenderer;
import org.millenaire.language.SpeechRefCodec;

public final class SpeechResolver {
    private SpeechResolver() {
    }

    @OnlyIn(value=Dist.CLIENT)
    public static String[] resolve(String speechRef, @Nullable ResourceLocation cultureIdForCache, int languageScore) {
        String localPlayerName;
        if (speechRef == null || speechRef.isEmpty()) {
            return new String[]{null, null};
        }
        String playerLang = SpeechResolver.resolveContentLang(Minecraft.getInstance().options.languageCode);
        String string = localPlayerName = Minecraft.getInstance().player != null ? Minecraft.getInstance().player.getName().getString() : "";
        if (speechRef.startsWith("s:")) {
            return SpeechResolver.resolveSentence(speechRef, playerLang, localPlayerName, cultureIdForCache, languageScore);
        }
        if (speechRef.startsWith("d:")) {
            return SpeechResolver.resolveDialogue(speechRef, playerLang, localPlayerName, cultureIdForCache, languageScore);
        }
        return new String[]{speechRef, null};
    }

    @OnlyIn(value=Dist.CLIENT)
    public static String[] resolve(String speechRef) {
        return SpeechResolver.resolve(speechRef, null, -1);
    }

    @OnlyIn(value=Dist.CLIENT)
    private static String resolveContentLang(String requested) {
        HashSet<String> supported = new HashSet<String>(DialogueLoader.getLoadedLanguages());
        supported.addAll(SentenceLoader.getLoadedLanguages());
        String resolved = LocaleResolver.resolveSupported(requested, supported);
        return resolved != null ? resolved : requested;
    }

    @OnlyIn(value=Dist.CLIENT)
    private static String[] resolveSentence(String speechRef, String playerLang, String playerName, @Nullable ResourceLocation cultureIdForCache, int languageScore) {
        int idx;
        String[] parts = speechRef.substring(2).split(":", 4);
        if (parts.length < 4) {
            return new String[]{null, null};
        }
        String cultureKey = parts[0];
        String role = parts[1];
        String goalKey = parts[2];
        try {
            idx = Integer.parseInt(parts[3]);
        }
        catch (NumberFormatException e) {
            return new String[]{null, null};
        }
        ResourceLocation cultureId = ResourceLocation.fromNamespaceAndPath((String)"millenaire", (String)cultureKey);
        String nativeText = SentenceLoader.getSentence(cultureId, "native", role, goalKey, idx);
        if (nativeText != null) {
            nativeText = nativeText.replace("$name", playerName);
        }
        String translation = null;
        String raw = SentenceLoader.getSentence(cultureId, playerLang, role, goalKey, idx);
        if (raw != null) {
            raw = raw.replace("$name", playerName);
        }
        if (raw != null && !DisplayNameResolver.equivalent(nativeText, raw)) {
            translation = SpeechResolver.maskTranslation(raw, cultureId, cultureIdForCache, languageScore);
        }
        return new String[]{nativeText, translation};
    }

    @OnlyIn(value=Dist.CLIENT)
    private static String[] resolveDialogue(String speechRef, String playerLang, String playerName, @Nullable ResourceLocation cultureIdForCache, int languageScore) {
        int lineIdx;
        String[] parts = speechRef.substring(2).split(":", 4);
        if (parts.length < 3) {
            return new String[]{null, null};
        }
        String cultureKey = parts[0];
        String dialogueKey = parts[1];
        try {
            lineIdx = Integer.parseInt(parts[2]);
        }
        catch (NumberFormatException e) {
            return new String[]{null, null};
        }
        String targetFirstName = parts.length >= 4 ? SpeechRefCodec.decodeTargetName(parts[3]) : "";
        ResourceLocation cultureId = ResourceLocation.fromNamespaceAndPath((String)"millenaire", (String)cultureKey);
        String nativeText = DialogueLoader.getDialogueLine(cultureId, "native", dialogueKey, lineIdx);
        if (nativeText != null) {
            nativeText = SpeechRefCodec.applyDialogueSubstitutions(nativeText, playerName, targetFirstName);
        }
        String translation = null;
        String raw = DialogueLoader.getDialogueLine(cultureId, playerLang, dialogueKey, lineIdx);
        if (raw != null && !DisplayNameResolver.equivalent(nativeText, raw = SpeechRefCodec.applyDialogueSubstitutions(raw, playerName, targetFirstName))) {
            translation = SpeechResolver.maskTranslation(raw, cultureId, cultureIdForCache, languageScore);
        }
        return new String[]{nativeText, translation};
    }

    @OnlyIn(value=Dist.CLIENT)
    private static String maskTranslation(String raw, ResourceLocation cultureId, @Nullable ResourceLocation cultureIdForCache, int languageScore) {
        int score = languageScore >= 0 ? languageScore : ClientLanguageCache.get(cultureIdForCache != null ? cultureIdForCache : cultureId);
        double ratio = SentenceRenderer.languageRatio(score);
        if (ratio >= 1.0) {
            return raw;
        }
        if (ratio <= 0.0) {
            return null;
        }
        return SentenceRenderer.maskTranslation(raw, ratio);
    }
}

