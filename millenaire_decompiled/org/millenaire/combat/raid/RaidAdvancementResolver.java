/*
 * Decompiled with CFR 0.150.
 * 
 * Could not load the following classes:
 *  javax.annotation.Nullable
 *  net.minecraft.resources.ResourceLocation
 */
package org.millenaire.combat.raid;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import javax.annotation.Nullable;
import net.minecraft.resources.ResourceLocation;
import org.millenaire.advancement.MillAdvancements;

public final class RaidAdvancementResolver {
    private static final String SELJUK = "seljuk";
    private static final String BYZANTINES = "byzantines";

    private RaidAdvancementResolver() {
    }

    public static List<Grant> resolve(ResourceLocation attackerCulture, ResourceLocation targetCulture, @Nullable UUID attackerOwner, @Nullable UUID targetOwner, boolean attackersWon) {
        String targetKey;
        ArrayList<Grant> grants = new ArrayList<Grant>();
        String attackerKey = attackerCulture != null ? attackerCulture.getPath() : "";
        String string = targetKey = targetCulture != null ? targetCulture.getPath() : "";
        if (attackersWon) {
            if (attackerOwner != null) {
                grants.add(new Grant(attackerOwner, MillAdvancements.VIKING));
                if (SELJUK.equals(attackerKey) && BYZANTINES.equals(targetKey)) {
                    grants.add(new Grant(attackerOwner, MillAdvancements.SELJUK_ISTANBUL));
                }
                if (targetOwner != null && !attackerOwner.equals(targetOwner)) {
                    grants.add(new Grant(attackerOwner, MillAdvancements.MP_RAIDONPLAYER));
                }
            }
        } else if (SELJUK.equals(attackerKey) && BYZANTINES.equals(targetKey) && targetOwner != null) {
            grants.add(new Grant(targetOwner, MillAdvancements.BYZANTINES_NOTTODAY));
        }
        return grants;
    }

    public record Grant(UUID playerUuid, ResourceLocation advancement) {
    }
}

