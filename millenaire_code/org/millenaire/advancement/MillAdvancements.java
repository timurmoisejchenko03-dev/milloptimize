/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.mojang.logging.LogUtils
 *  net.minecraft.advancements.AdvancementHolder
 *  net.minecraft.advancements.AdvancementProgress
 *  net.minecraft.advancements.CriterionTrigger
 *  net.minecraft.core.registries.Registries
 *  net.minecraft.resources.ResourceKey
 *  net.minecraft.resources.ResourceLocation
 *  net.minecraft.server.ServerAdvancementManager
 *  net.minecraft.server.level.ServerPlayer
 *  net.neoforged.bus.api.IEventBus
 *  net.neoforged.neoforge.registries.DeferredRegister
 *  org.slf4j.Logger
 */
package org.millenaire.advancement;

import com.mojang.logging.LogUtils;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;
import net.minecraft.advancements.AdvancementHolder;
import net.minecraft.advancements.AdvancementProgress;
import net.minecraft.advancements.CriterionTrigger;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.ServerAdvancementManager;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredRegister;
import org.millenaire.advancement.AdvancementStatsManager;
import org.millenaire.advancement.MillTrigger;
import org.slf4j.Logger;

public final class MillAdvancements {
    private static final Logger LOGGER = LogUtils.getLogger();
    public static final DeferredRegister<CriterionTrigger<?>> TRIGGER_TYPES = DeferredRegister.create((ResourceKey)Registries.TRIGGER_TYPE, (String)"millenaire");
    public static final Supplier<MillTrigger> TRIGGER = TRIGGER_TYPES.register("millenaire_trigger", MillTrigger::new);
    public static final ResourceLocation FIRST_CONTACT = MillAdvancements.id("firstcontact");
    public static final ResourceLocation CRESUS = MillAdvancements.id("cresus");
    public static final ResourceLocation CHEERS = MillAdvancements.id("cheers");
    public static final ResourceLocation MASTER_FARMER = MillAdvancements.id("masterfarmer");
    public static final ResourceLocation GREAT_HUNTER = MillAdvancements.id("greathunter");
    public static final ResourceLocation HIRED = MillAdvancements.id("hired");
    public static final ResourceLocation RAINBOW = MillAdvancements.id("rainbow");
    public static final ResourceLocation SUMMONING_WAND = MillAdvancements.id("summoningwand");
    public static final ResourceLocation AMATEUR_ARCHITECT = MillAdvancements.id("amateurarchitect");
    public static final ResourceLocation MEDIEVAL_METROPOLIS = MillAdvancements.id("medievalmetropolis");
    public static final ResourceLocation EXPLORER = MillAdvancements.id("explorer");
    public static final ResourceLocation MARCO_POLO = MillAdvancements.id("marcopolo");
    public static final ResourceLocation MAGELLAN = MillAdvancements.id("magellan");
    public static final ResourceLocation PANTHEON = MillAdvancements.id("pantheon");
    public static final ResourceLocation THE_QUEST = MillAdvancements.id("thequest");
    public static final ResourceLocation MAITRE_A_PENSER = MillAdvancements.id("maitreapenser");
    public static final ResourceLocation WQ_NORMAN = MillAdvancements.id("wq_norman");
    public static final ResourceLocation WQ_INDIAN = MillAdvancements.id("wq_indian");
    public static final ResourceLocation WQ_MAYAN = MillAdvancements.id("wq_mayan");
    public static final ResourceLocation PUJA = MillAdvancements.id("puja");
    public static final ResourceLocation SACRIFICE = MillAdvancements.id("sacrifice");
    public static final ResourceLocation FRIEND_INDEED = MillAdvancements.id("friendindeed");
    public static final ResourceLocation SELF_DEFENSE = MillAdvancements.id("selfdefense");
    public static final ResourceLocation DARK_SIDE = MillAdvancements.id("darkside");
    public static final ResourceLocation ATTILA = MillAdvancements.id("attila");
    public static final ResourceLocation SCIPIO = MillAdvancements.id("scipio");
    public static final ResourceLocation VIKING = MillAdvancements.id("viking");
    public static final ResourceLocation SELJUK_ISTANBUL = MillAdvancements.id("seljuk_istanbul");
    public static final ResourceLocation BYZANTINES_NOTTODAY = MillAdvancements.id("byzantines_nottoday");
    public static final ResourceLocation MARVEL_NORMAN = MillAdvancements.id("marvel_norman");
    public static final ResourceLocation MP_WEAPON = MillAdvancements.id("mp_weapon");
    public static final ResourceLocation MP_HIREDGOON = MillAdvancements.id("mp_hiredgoon");
    public static final ResourceLocation MP_RAIDONPLAYER = MillAdvancements.id("mp_raidonplayer");
    public static final ResourceLocation MP_NEIGHBOURTRADE = MillAdvancements.id("mp_neighbourtrade");
    public static final ResourceLocation MP_FRIENDLYVILLAGE = MillAdvancements.id("mp_friendlyvillage");
    public static final List<String> ADVANCEMENT_CULTURES = List.of("norman", "indian", "mayan", "japanese", "byzantines", "inuits", "seljuk");
    public static final Map<String, ResourceLocation> REP = new HashMap<String, ResourceLocation>();
    public static final Map<String, ResourceLocation> LEADER = new HashMap<String, ResourceLocation>();
    public static final Map<String, ResourceLocation> COMPLETE = new HashMap<String, ResourceLocation>();

    private MillAdvancements() {
    }

    public static void register(IEventBus modEventBus) {
        TRIGGER_TYPES.register(modEventBus);
    }

    public static void grant(ServerPlayer player, ResourceLocation advancementId) {
        ServerAdvancementManager serverAdvancements = player.server.getAdvancements();
        AdvancementHolder holder = serverAdvancements.get(advancementId);
        if (holder == null) {
            LOGGER.debug("Advancement not found: {}", (Object)advancementId);
            return;
        }
        AdvancementProgress progress = player.getAdvancements().getOrStartProgress(holder);
        if (progress.isDone()) {
            return;
        }
        for (String criterion : progress.getRemainingCriteria()) {
            player.getAdvancements().award(holder, criterion);
        }
        LOGGER.debug("Advancement {} granted to {}", (Object)advancementId, (Object)player.getName().getString());
        AdvancementStatsManager.onAdvancementEarned(player, advancementId);
    }

    private static ResourceLocation id(String path) {
        return ResourceLocation.fromNamespaceAndPath((String)"millenaire", (String)path);
    }

    static {
        for (String culture : ADVANCEMENT_CULTURES) {
            REP.put(culture, MillAdvancements.id("rep_" + culture));
            LEADER.put(culture, MillAdvancements.id("leader_" + culture));
            COMPLETE.put(culture, MillAdvancements.id("complete_" + culture));
        }
    }
}

