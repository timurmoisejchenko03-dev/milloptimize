/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.mojang.logging.LogUtils
 *  net.minecraft.resources.ResourceLocation
 *  org.slf4j.Logger
 */
package org.millenaire.language;

import com.mojang.logging.LogUtils;
import java.lang.invoke.CallSite;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.TreeSet;
import net.minecraft.resources.ResourceLocation;
import org.millenaire.content.BuiltInCultures;
import org.millenaire.culture.Culture;
import org.millenaire.culture.ModCultures;
import org.millenaire.culture.ReputationLabel;
import org.millenaire.culture.VillagerType;
import org.millenaire.language.ServerTranslationCache;
import org.slf4j.Logger;

public final class I18nKeyAuditor {
    private static final Logger LOGGER = LogUtils.getLogger();

    private I18nKeyAuditor() {
    }

    public static void audit() {
        ArrayList<String> missingCultures = new ArrayList<String>();
        ArrayList<String> missingRoles = new ArrayList<String>();
        ArrayList<String> missingGoals = new ArrayList<String>();
        ArrayList<String> missingReputations = new ArrayList<String>();
        HashSet<String> builtIn = new HashSet<String>(BuiltInCultures.IDS);
        Map<ResourceLocation, Culture> cultures = ModCultures.getAllCultures();
        for (ResourceLocation resourceLocation : cultures.keySet()) {
            String string;
            if (!builtIn.contains(resourceLocation.getPath()) || ServerTranslationCache.has(string = "culture.millenaire." + resourceLocation.getPath())) continue;
            missingCultures.add(string);
        }
        TreeSet<CallSite> seenGoalKeys = new TreeSet<CallSite>();
        for (Map.Entry<ResourceLocation, VillagerType> entry : ModCultures.getAllVillagerTypes().entrySet()) {
            VillagerType vt = entry.getValue();
            ResourceLocation vtCulture = ModCultures.extractCultureId(vt.id());
            if (!builtIn.contains(vtCulture.getPath())) continue;
            String qualifiedRole = vt.id().getPath().replace('/', '_');
            String roleKey = "role.millenaire." + qualifiedRole;
            if (!ServerTranslationCache.has(roleKey)) {
                missingRoles.add(roleKey);
            }
            for (ResourceLocation goalId : vt.goals()) {
                String goalKey = "goal.millenaire." + goalId.getPath();
                if (!seenGoalKeys.add((CallSite)((Object)goalKey)) || ServerTranslationCache.has(goalKey)) continue;
                missingGoals.add(goalKey);
            }
        }
        TreeSet<String> treeSet = new TreeSet<String>();
        for (ResourceLocation cId : cultures.keySet()) {
            if (!builtIn.contains(cId.getPath())) continue;
            I18nKeyAuditor.collectReputationGaps(ModCultures.getReputationLabels(cId), treeSet, missingReputations);
            I18nKeyAuditor.collectReputationGaps(ModCultures.getCultureReputationLabels(cId), treeSet, missingReputations);
        }
        int n = missingCultures.size() + missingRoles.size() + missingGoals.size() + missingReputations.size();
        if (n == 0) {
            LOGGER.info("[I18nKeyAuditor] All referenced i18n keys are present in en_us.json");
            return;
        }
        StringBuilder sb = new StringBuilder();
        sb.append("[I18nKeyAuditor] ").append(n).append(" i18n keys referenced by loaded content but missing from en_us.json:");
        I18nKeyAuditor.appendCategory(sb, "cultures", missingCultures);
        I18nKeyAuditor.appendCategory(sb, "roles", missingRoles);
        I18nKeyAuditor.appendCategory(sb, "goals", missingGoals);
        I18nKeyAuditor.appendCategory(sb, "reputations", missingReputations);
        LOGGER.warn(sb.toString());
    }

    private static void collectReputationGaps(List<ReputationLabel> labels, TreeSet<String> seen, List<String> missing) {
        if (labels == null) {
            return;
        }
        for (ReputationLabel label : labels) {
            String key = label.key();
            if (!seen.add(key) || ServerTranslationCache.has(key)) continue;
            missing.add(key);
        }
    }

    private static void appendCategory(StringBuilder sb, String name, List<String> missing) {
        if (missing.isEmpty()) {
            return;
        }
        sb.append("\n  ").append(name).append(" (").append(missing.size()).append("):");
        for (String key : missing) {
            sb.append("\n    - ").append(key);
        }
    }
}

