/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.mojang.logging.LogUtils
 *  org.slf4j.Logger
 */
package org.millenaire.net;

import com.mojang.logging.LogUtils;
import java.io.InputStream;
import java.util.List;
import java.util.Properties;
import org.millenaire.config.MillenaireServerConfig;
import org.slf4j.Logger;

public final class ModApiConfig {
    private static final Logger LOGGER = LogUtils.getLogger();
    private static final String SECRET;
    private static final String BAKED_BASE;
    private static final String FALLBACK_BASE;

    private ModApiConfig() {
    }

    public static String secret() {
        return SECRET;
    }

    public static boolean hasSecret() {
        return !SECRET.isBlank();
    }

    public static List<String> baseUrls() {
        return ModApiConfig.resolveBases((String)MillenaireServerConfig.SERVER.apiBaseUrlOverride.get(), BAKED_BASE, FALLBACK_BASE);
    }

    static List<String> resolveBases(String override, String primary, String fallback) {
        String f;
        if (override != null && !override.isBlank()) {
            return List.of(ModApiConfig.normalizeBase(override.strip()));
        }
        String p = ModApiConfig.normalizeBase(primary);
        return p.equals(f = ModApiConfig.normalizeBase(fallback)) ? List.of(p) : List.of(p, f);
    }

    static String normalizeBase(String base) {
        return base.endsWith("/") ? base.substring(0, base.length() - 1) : base;
    }

    static {
        Properties props = new Properties();
        try (InputStream in = ModApiConfig.class.getResourceAsStream("/millenaire-build.properties");){
            if (in != null) {
                props.load(in);
            } else {
                LOGGER.debug("millenaire-build.properties not found \u2014 mod API disabled");
            }
        }
        catch (Exception e) {
            LOGGER.debug("Could not read millenaire-build.properties: {}", (Object)e.getMessage());
        }
        SECRET = props.getProperty("mod.hmac.secret", "");
        BAKED_BASE = ModApiConfig.normalizeBase(props.getProperty("mod.api.base", "https://millenaire.org"));
        FALLBACK_BASE = ModApiConfig.normalizeBase(props.getProperty("mod.api.base.fallback", "https://new.millenaire.org"));
    }
}

