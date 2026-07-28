/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.mojang.logging.LogUtils
 *  net.minecraft.client.Minecraft
 *  net.minecraft.client.resources.language.ClientLanguage
 *  net.minecraft.client.resources.language.LanguageInfo
 *  net.minecraft.client.resources.language.LanguageManager
 *  net.minecraft.locale.Language
 *  net.minecraft.server.packs.resources.ResourceManager
 *  net.minecraft.server.packs.resources.ResourceManagerReloadListener
 *  org.slf4j.Logger
 */
package org.millenaire.client;

import com.mojang.logging.LogUtils;
import java.lang.reflect.Field;
import java.util.ArrayList;
import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.language.ClientLanguage;
import net.minecraft.client.resources.language.LanguageInfo;
import net.minecraft.client.resources.language.LanguageManager;
import net.minecraft.locale.Language;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.ResourceManagerReloadListener;
import org.slf4j.Logger;

public class LanguageFallbackListener
implements ResourceManagerReloadListener {
    private static final Logger LOGGER = LogUtils.getLogger();
    private static final Field I18N_LANGUAGE_FIELD;

    public void onResourceManagerReload(ResourceManager resourceManager) {
        LanguageManager langManager = Minecraft.getInstance().getLanguageManager();
        String currentCode = langManager.getSelected();
        if ("en_us".equals(currentCode)) {
            return;
        }
        String parentCode = LanguageFallbackListener.findParentLocale(langManager, currentCode);
        if (parentCode == null) {
            return;
        }
        LanguageInfo currentInfo = langManager.getLanguage(currentCode);
        boolean rtl = currentInfo != null && currentInfo.bidirectional();
        ArrayList<String> chain = new ArrayList<String>(3);
        chain.add("en_us");
        chain.add(parentCode);
        chain.add(currentCode);
        ClientLanguage clientLanguage = ClientLanguage.loadFrom((ResourceManager)resourceManager, chain, (boolean)rtl);
        Language.inject((Language)clientLanguage);
        if (I18N_LANGUAGE_FIELD != null) {
            try {
                I18N_LANGUAGE_FIELD.set(null, clientLanguage);
            }
            catch (ReflectiveOperationException e) {
                LOGGER.warn("Failed to update I18n.language field", (Throwable)e);
            }
        }
        LOGGER.debug("Language fallback chain: {} -> {} -> en_us", (Object)currentCode, (Object)parentCode);
    }

    private static String findParentLocale(LanguageManager langManager, String currentCode) {
        String[] parts = currentCode.split("_");
        if (parts.length != 2) {
            return null;
        }
        String langPrefix = parts[0];
        String candidate = langPrefix + "_" + langPrefix;
        if (!candidate.equals(currentCode) && langManager.getLanguage(candidate) != null) {
            return candidate;
        }
        for (String code : langManager.getLanguages().keySet()) {
            if (code.equals(currentCode) || !code.startsWith(langPrefix + "_")) continue;
            return code;
        }
        return null;
    }

    static {
        Field field = null;
        try {
            Class<?> i18nClass = Class.forName("net.minecraft.client.resources.language.I18n");
            field = i18nClass.getDeclaredField("language");
            field.setAccessible(true);
        }
        catch (ReflectiveOperationException e) {
            LOGGER.warn("Could not access I18n.language field; regional fallback may not apply to I18n.get() calls", (Throwable)e);
        }
        I18N_LANGUAGE_FIELD = field;
    }
}

