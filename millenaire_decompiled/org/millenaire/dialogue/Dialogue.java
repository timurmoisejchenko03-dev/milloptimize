/*
 * Decompiled with CFR 0.150.
 * 
 * Could not load the following classes:
 *  net.minecraft.resources.ResourceLocation
 */
package org.millenaire.dialogue;

import java.util.List;
import net.minecraft.resources.ResourceLocation;

public record Dialogue(String key, int weight, List<String> tags, String v1Constraint, String v2Constraint, List<Line> lines, ResourceLocation culture, List<String> buildings, List<String> notBuildings, List<String> villagers, List<String> notVillagers, List<String> relations, List<String> notRelations) {

    public record Line(int speaker, int delay, String text) {
    }
}

