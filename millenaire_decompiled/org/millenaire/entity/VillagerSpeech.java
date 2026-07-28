/*
 * Decompiled with CFR 0.150.
 * 
 * Could not load the following classes:
 *  com.mojang.logging.LogUtils
 *  net.minecraft.network.protocol.common.custom.CustomPacketPayload
 *  net.minecraft.resources.ResourceLocation
 *  net.minecraft.server.level.ServerLevel
 *  net.minecraft.server.level.ServerPlayer
 *  net.minecraft.world.entity.Entity
 *  net.minecraft.world.entity.player.Player
 *  net.minecraft.world.level.Level
 *  net.neoforged.neoforge.network.PacketDistributor
 *  org.slf4j.Logger
 */
package org.millenaire.entity;

import com.mojang.logging.LogUtils;
import java.util.concurrent.ThreadLocalRandom;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.neoforged.neoforge.network.PacketDistributor;
import org.millenaire.command.DebugCommand;
import org.millenaire.config.MillenaireServerConfig;
import org.millenaire.culture.ModCultures;
import org.millenaire.dialogue.SentenceLoader;
import org.millenaire.entity.MillVillager;
import org.millenaire.goal.VillagerTask;
import org.millenaire.network.SpeechChatPayload;
import org.millenaire.village.PlayerCultureReputation;
import org.slf4j.Logger;

public class VillagerSpeech {
    private static final Logger LOGGER = LogUtils.getLogger();
    private static final int SPEECH_COOLDOWN_TICKS = 600;
    private static final int GREETING_COOLDOWN_TICKS = 12000;
    private static final double SPEECH_PLAYER_DISTANCE = 3.0;
    private static final int DISPLAY_DURATION_FACTOR = 2;
    private final MillVillager villager;
    private int speechCooldown;
    private int speechDisplayTimer;

    public VillagerSpeech(MillVillager villager) {
        this.villager = villager;
    }

    private static int computeDisplayTicks(int textLength) {
        return Math.min((10 + textLength / 5) * 20, 600) * 2;
    }

    public void tick(VillagerTask task) {
        if (this.speechDisplayTimer > 0) {
            --this.speechDisplayTimer;
            if (this.speechDisplayTimer == 0) {
                this.villager.setSpeechData("");
            }
            return;
        }
        if (this.speechCooldown > 0) {
            --this.speechCooldown;
            return;
        }
        String goalKey = task != null ? VillagerSpeech.mapGoalKeyToSentenceKey(task.goalId().getPath()) : "greeting";
        this.fireSentence(goalKey, "greeting".equals(goalKey));
    }

    public void speakGoalChosen(VillagerTask task) {
        if (this.speechDisplayTimer > 0 || this.speechCooldown > 0) {
            return;
        }
        String goalKey = VillagerSpeech.mapGoalKeyToSentenceKey(task.goalId().getPath()) + ".chosen";
        this.fireSentence(goalKey, false);
    }

    private void fireSentence(String goalKey, boolean isGreeting) {
        Player nearestPlayer = this.villager.level().getNearestPlayer((Entity)this.villager, 3.0);
        if (nearestPlayer == null) {
            return;
        }
        ResourceLocation villagerTypeId = this.villager.getVillagerTypeId();
        if (villagerTypeId == null) {
            return;
        }
        String vtPath = villagerTypeId.getPath();
        int slashIdx = vtPath.indexOf(47);
        if (slashIdx <= 0) {
            return;
        }
        ResourceLocation cultureId = ModCultures.extractCultureId(villagerTypeId);
        String role = vtPath.substring(slashIdx + 1);
        if (ModCultures.getCulture(cultureId) == null) {
            return;
        }
        String effectiveRole = SentenceLoader.resolveEffectiveRole(cultureId, "native", role, null, goalKey);
        if (effectiveRole == null) {
            return;
        }
        int count = SentenceLoader.countVariants(cultureId, "native", effectiveRole, goalKey);
        if (count == 0) {
            return;
        }
        if (isGreeting && ThreadLocalRandom.current().nextInt(10) != 0) {
            this.speechCooldown = 60;
            return;
        }
        int idx = ThreadLocalRandom.current().nextInt(count);
        String speechRef = "s:" + cultureId.getPath() + ":" + effectiveRole + ":" + goalKey + ":" + idx;
        this.villager.setSpeechData(speechRef);
        String nativeText = SentenceLoader.getSentence(cultureId, "native", effectiveRole, goalKey, idx);
        if (nativeText == null) {
            nativeText = goalKey;
        }
        nativeText = nativeText.replace("$name", nearestPlayer.getName().getString());
        this.speechDisplayTimer = VillagerSpeech.computeDisplayTicks(nativeText.length());
        this.speechCooldown = isGreeting ? 12000 : 600;
        Level level = this.villager.level();
        if (level instanceof ServerLevel) {
            int chatRange;
            ServerLevel serverLevel = (ServerLevel)level;
            int n = chatRange = serverLevel.getServer().isSingleplayer() ? MillenaireServerConfig.SERVER.sentenceDistanceSingleplayer.getAsInt() : MillenaireServerConfig.SERVER.sentenceDistanceMultiplayer.getAsInt();
            if (chatRange > 0) {
                String displayName = this.villager.getVillagerDisplayName();
                String cultureKeyStr = cultureId.getPath();
                for (ServerPlayer sp : serverLevel.players()) {
                    if (!(sp.distanceTo((Entity)this.villager) <= (float)chatRange)) continue;
                    int langScore = (Boolean)MillenaireServerConfig.SERVER.languageLearning.get() == false ? Integer.MAX_VALUE : PlayerCultureReputation.get(serverLevel).getLanguageKnowledge(sp.getUUID(), cultureId);
                    SpeechChatPayload payload = new SpeechChatPayload(displayName != null ? displayName : "", speechRef, cultureKeyStr, langScore, "");
                    PacketDistributor.sendToPlayer((ServerPlayer)sp, (CustomPacketPayload)payload, (CustomPacketPayload[])new CustomPacketPayload[0]);
                }
            }
        }
        if (DebugCommand.isVerbose(this.villager.getUUID())) {
            LOGGER.info("[V-DEBUG] {} says ref: {} (native: {})", new Object[]{this.villager.getVillagerDisplayName(), speechRef, nativeText});
        }
    }

    public void speakCombatCall(String goalKey) {
        ResourceLocation villagerTypeId = this.villager.getVillagerTypeId();
        if (villagerTypeId == null) {
            return;
        }
        String vtPath = villagerTypeId.getPath();
        int slashIdx = vtPath.indexOf(47);
        if (slashIdx <= 0) {
            return;
        }
        ResourceLocation cultureId = ModCultures.extractCultureId(villagerTypeId);
        String role = vtPath.substring(slashIdx + 1);
        if (ModCultures.getCulture(cultureId) == null) {
            return;
        }
        String effectiveRole = SentenceLoader.resolveEffectiveRole(cultureId, "native", role, null, goalKey);
        if (effectiveRole == null) {
            return;
        }
        int count = SentenceLoader.countVariants(cultureId, "native", effectiveRole, goalKey);
        if (count == 0) {
            return;
        }
        int idx = ThreadLocalRandom.current().nextInt(count);
        String speechRef = "s:" + cultureId.getPath() + ":" + effectiveRole + ":" + goalKey + ":" + idx;
        this.villager.setSpeechData(speechRef);
        String nativeText = SentenceLoader.getSentence(cultureId, "native", effectiveRole, goalKey, idx);
        if (nativeText == null) {
            nativeText = goalKey;
        }
        this.speechDisplayTimer = VillagerSpeech.computeDisplayTicks(nativeText.length());
        this.speechCooldown = 600;
        Level level = this.villager.level();
        if (level instanceof ServerLevel) {
            int chatRange;
            ServerLevel serverLevel = (ServerLevel)level;
            int n = chatRange = serverLevel.getServer().isSingleplayer() ? MillenaireServerConfig.SERVER.sentenceDistanceSingleplayer.getAsInt() : MillenaireServerConfig.SERVER.sentenceDistanceMultiplayer.getAsInt();
            if (chatRange > 0) {
                String displayName = this.villager.getVillagerDisplayName();
                String cultureKeyStr = cultureId.getPath();
                for (ServerPlayer sp : serverLevel.players()) {
                    if (!(sp.distanceTo((Entity)this.villager) <= (float)chatRange)) continue;
                    int langScore = (Boolean)MillenaireServerConfig.SERVER.languageLearning.get() == false ? Integer.MAX_VALUE : PlayerCultureReputation.get(serverLevel).getLanguageKnowledge(sp.getUUID(), cultureId);
                    SpeechChatPayload payload = new SpeechChatPayload(displayName != null ? displayName : "", speechRef, cultureKeyStr, langScore, "");
                    PacketDistributor.sendToPlayer((ServerPlayer)sp, (CustomPacketPayload)payload, (CustomPacketPayload[])new CustomPacketPayload[0]);
                }
            }
        }
        if (DebugCommand.isVerbose(this.villager.getUUID())) {
            LOGGER.info("[V-DEBUG] {} shouts ref: {} (native: {})", new Object[]{this.villager.getVillagerDisplayName(), speechRef, nativeText});
        }
    }

    public void setSpeechText(String text) {
        this.villager.setSpeechData(text);
        if (text.isEmpty()) {
            this.speechDisplayTimer = 0;
        } else {
            this.speechDisplayTimer = VillagerSpeech.computeDisplayTicks(text.length());
            this.speechCooldown = 600;
        }
    }

    public String getSpeechText() {
        return this.villager.getSpeechData();
    }

    static String mapGoalKeyToSentenceKey(String goalKey) {
        return switch (goalKey) {
            case "chop_trees" -> "choptrees";
            case "plant_saplings" -> "plantsaplings";
            case "harvest_wheat_home" -> "collectcrop";
            case "plant_wheat_home" -> "plantseeds";
            case "build" -> "construction";
            case "idle" -> "greeting";
            case "rest" -> "gorest";
            case "pray" -> "gopray";
            case "drink" -> "godrink";
            case "play", "play_with_friends" -> "goplay";
            case "hold_service" -> "goholdaservice";
            case "bring_back_home" -> "bringbackresourceshome";
            case "child_become_adult" -> "becomeadult";
            case "be_seller" -> "keepstall";
            case "craft_boudin" -> "makeboudin";
            case "craft_books" -> "makebooks";
            case "craft_tapestry" -> "maketapestry";
            case "craft_bread" -> "makebread";
            case "craft_cider", "craft_ciderhome" -> "makecider";
            case "craft_calvahome" -> "makecalva";
            case "craft_glassbottles" -> "makeglassbottles";
            case "craft_rasgulla" -> "makerasgulla";
            case "craft_indianstatue" -> "scult";
            case "craft_arrow" -> "makearrow";
            case "craft_bookshelves" -> "makebookshelves";
            case "craft_bow" -> "makebow";
            case "craft_brick" -> "makebrick";
            case "craft_byzantineboots" -> "makebyzantineboots";
            case "craft_byzantinechest" -> "makebyzantinechest";
            case "craft_byzantinehelmet" -> "makebyzantinehelmet";
            case "craft_byzantinelegs" -> "makebyzantinelegs";
            case "craft_byzantinemace" -> "makebyzantinemace";
            case "craft_byzantinetiles" -> "makebyzantinetiles";
            case "craft_lokum" -> "makelokum";
            case "craft_pide_beef" -> "makepidebeef";
            case "craft_pide_mutton" -> "makepidemutton";
            case "craft_steelaxe" -> "makesteelaxe";
            case "craft_steelhoe" -> "makesteelhoe";
            case "craft_steelpickaxe" -> "makesteelpickaxe";
            case "craft_steelshovel" -> "makesteelshovel";
            case "craft_wallcarpet_large" -> "makewallcarpetlarge";
            case "craft_wallcarpet_medium" -> "makewallcarpetmedium";
            case "craft_wallcarpet_small" -> "makewallcarpetsmall";
            case "craft_yogurt" -> "makeyogurt";
            case "get_goods_for_household" -> "getgoodshousehold";
            case "cook_indian_brick" -> "cookbrick";
            case "listen_to_speech_2" -> "listentospeech";
            default -> goalKey.replace("_", "");
        };
    }
}

