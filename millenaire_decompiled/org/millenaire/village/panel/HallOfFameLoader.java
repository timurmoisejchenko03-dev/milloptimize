/*
 * Decompiled with CFR 0.150.
 * 
 * Could not load the following classes:
 *  com.mojang.logging.LogUtils
 *  org.slf4j.Logger
 */
package org.millenaire.village.panel;

import com.mojang.logging.LogUtils;
import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import org.slf4j.Logger;

public final class HallOfFameLoader {
    private static final Logger LOGGER = LogUtils.getLogger();
    private static final String HOF_RESOURCE = "/millenaire/hof.txt";
    private static List<String> cachedData;

    private HallOfFameLoader() {
    }

    public static List<String> getHoFData() {
        if (cachedData != null) {
            return cachedData;
        }
        ArrayList<String> hofData = new ArrayList<String>();
        try {
            InputStream is = HallOfFameLoader.class.getResourceAsStream(HOF_RESOURCE);
            if (is == null) {
                LOGGER.warn("[Millenaire] HoF file not found: {}", (Object)HOF_RESOURCE);
                cachedData = Collections.emptyList();
                return cachedData;
            }
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(is, StandardCharsets.UTF_8));){
                String line;
                while ((line = reader.readLine()) != null) {
                    if ((line = line.trim()).isEmpty() || line.startsWith("//")) continue;
                    hofData.add(line);
                }
            }
        }
        catch (Exception e) {
            LOGGER.error("[Millenaire] Error loading HoF", (Throwable)e);
        }
        cachedData = Collections.unmodifiableList(hofData);
        LOGGER.debug("[Millenaire] HoF loaded: {} entries", (Object)cachedData.size());
        return cachedData;
    }
}

