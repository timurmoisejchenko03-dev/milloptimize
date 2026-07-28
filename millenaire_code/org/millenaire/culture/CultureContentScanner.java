/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.mojang.logging.LogUtils
 *  org.slf4j.Logger
 */
package org.millenaire.culture;

import com.mojang.logging.LogUtils;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import org.millenaire.content.ContentFs;
import org.millenaire.content.CustomContentIndex;
import org.millenaire.content.Resource;
import org.millenaire.content.SourceKind;
import org.millenaire.culture.BuildingPlanSetLoader;
import org.millenaire.culture.CrossFolderConflictException;
import org.millenaire.culture.CultureLoader;
import org.millenaire.culture.VillageTypeLoader;
import org.millenaire.culture.VillagerTypeLoader;
import org.slf4j.Logger;

final class CultureContentScanner {
    private static final Logger LOGGER = LogUtils.getLogger();

    private CultureContentScanner() {
    }

    static void scan(String contentType, String culture) {
        Set<String> disabledIds = CultureLoader.resolveDisabledIds(culture, contentType);
        if ("villager_type".equals(contentType) || "village_type".equals(contentType) || "building_plan".equals(contentType)) {
            CultureContentScanner.scanAndLoadFromContentFs(contentType, culture, disabledIds);
            return;
        }
        LOGGER.error("scan: unknown contentType '{}' for culture {}", (Object)contentType, (Object)culture);
    }

    private static void scanAndLoadFromContentFs(String contentType, String culture, Set<String> disabledIds) {
        String pluralType = CultureLoader.pluralTypeName(contentType);
        if (pluralType == null) {
            return;
        }
        ContentFs cultureFs = CustomContentIndex.current().forCulture(culture);
        boolean flatLayout = "villages".equals(pluralType);
        int maxDepth = flatLayout ? 0 : 1;
        List<Resource> entries = cultureFs.walk(pluralType, maxDepth).collect(Collectors.toList());
        String typeRootPrefix = ("cultures/" + culture + "/" + pluralType + "/").toLowerCase(Locale.ROOT);
        Set<String> crossSourceShadowedPaths = CultureContentScanner.buildCrossSourceShadowSet(contentType, entries, typeRootPrefix, flatLayout);
        for (Resource res : entries) {
            String residualId;
            String fileName;
            String relPathFull = res.relPath();
            if (crossSourceShadowedPaths.contains(relPathFull) || !relPathFull.endsWith(".json") || (fileName = relPathFull.substring(relPathFull.lastIndexOf(47) + 1)).startsWith("_") || "building_plan".equals(contentType) && fileName.endsWith("_special_points.json") || !relPathFull.startsWith(typeRootPrefix)) continue;
            String relInsideType = relPathFull.substring(typeRootPrefix.length());
            int sep = relInsideType.indexOf(47);
            if (flatLayout) {
                if (sep >= 0) {
                    LOGGER.warn("Ignoring {} in sub-directory: {} (villages are flat under cultures/<c>/villages/). Move the file up one level.", (Object)contentType, (Object)relPathFull);
                    continue;
                }
                residualId = relInsideType.substring(0, relInsideType.length() - 5);
            } else {
                if (sep < 0 || relInsideType.indexOf(47, sep + 1) >= 0) {
                    LOGGER.warn("Ignoring {} at unexpected depth: {} (expected cultures/<c>/{}/<category>/<id>.json).", new Object[]{contentType, relPathFull, pluralType});
                    continue;
                }
                String leaf = relInsideType.substring(sep + 1);
                residualId = leaf.substring(0, leaf.length() - 5);
            }
            if (residualId.isEmpty()) continue;
            if (disabledIds.contains(residualId)) {
                LOGGER.debug("Skipping {} '{}' (listed in _disabled.json)", (Object)contentType, (Object)residualId);
                continue;
            }
            String contentId = culture + "/" + residualId;
            String relForCulture = pluralType + "/" + relInsideType;
            String pathCategory = !flatLayout && sep > 0 ? relInsideType.substring(0, sep) : null;
            try {
                CultureContentScanner.dispatch(contentType, contentId, cultureFs, relForCulture, residualId, pathCategory);
            }
            catch (CrossFolderConflictException ex) {
                throw ex;
            }
            catch (Exception ex) {
                LOGGER.error("Error loading {} from ContentFs: {}", new Object[]{relPathFull, ex.getMessage(), ex});
            }
        }
    }

    private static Set<String> buildCrossSourceShadowSet(String contentType, List<Resource> entries, String typeRootPrefix, boolean flatLayout) {
        LinkedHashMap<String, List> byResidualId = new LinkedHashMap<String, List>();
        for (Resource res : entries) {
            String residualId;
            String fileName;
            String relPathFull = res.relPath();
            if (!relPathFull.endsWith(".json") || (fileName = relPathFull.substring(relPathFull.lastIndexOf(47) + 1)).startsWith("_") || "building_plan".equals(contentType) && fileName.endsWith("_special_points.json") || !relPathFull.startsWith(typeRootPrefix)) continue;
            String relInsideType = relPathFull.substring(typeRootPrefix.length());
            int sep = relInsideType.indexOf(47);
            if (flatLayout) {
                if (sep >= 0) continue;
                residualId = relInsideType.substring(0, relInsideType.length() - 5);
            } else {
                if (sep < 0 || relInsideType.indexOf(47, sep + 1) >= 0) continue;
                String leaf = relInsideType.substring(sep + 1);
                residualId = leaf.substring(0, leaf.length() - 5);
            }
            if (residualId.isEmpty()) continue;
            byResidualId.computeIfAbsent(residualId, k -> new ArrayList()).add(res);
        }
        HashSet<String> shadowed = new HashSet<String>();
        for (Map.Entry e : byResidualId.entrySet()) {
            List list = (List)e.getValue();
            if (list.size() < 2) continue;
            SourceKind firstKind = ((Resource)list.get(0)).kind();
            boolean mixedKinds = false;
            for (int i = 1; i < list.size(); ++i) {
                if (((Resource)list.get(i)).kind() == firstKind) continue;
                mixedKinds = true;
                break;
            }
            if (!mixedKinds) continue;
            Resource winner = (Resource)list.get(list.size() - 1);
            for (int i = 0; i < list.size() - 1; ++i) {
                Resource shadowedEntry = (Resource)list.get(i);
                shadowed.add(shadowedEntry.relPath());
                LOGGER.warn("[Millenaire] {} '{}' at '{}' ({}) shadowed by '{}' at '{}' ({}) (cross-source category override \u2014 higher-priority source wins)", new Object[]{contentType, e.getKey(), shadowedEntry.relPath(), shadowedEntry.source().displayName(), winner.relPath(), winner.source().displayName(), winner.kind()});
            }
        }
        return shadowed;
    }

    private static void dispatch(String contentType, String contentId, ContentFs cultureFs, String relPath, String residualId, String pathCategory) {
        switch (contentType) {
            case "building_plan": {
                BuildingPlanSetLoader.loadFromContentFs(contentId, cultureFs, relPath, pathCategory);
                break;
            }
            case "villager_type": {
                VillagerTypeLoader.loadFromContentFs(contentId, cultureFs, relPath);
                break;
            }
            case "village_type": {
                VillageTypeLoader.loadFromContentFs(contentId, cultureFs, relPath, residualId);
                break;
            }
            default: {
                LOGGER.warn("dispatch: unknown contentType '{}'", (Object)contentType);
            }
        }
    }
}

