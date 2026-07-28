/*
 * Decompiled with CFR 0.150.
 * 
 * Could not load the following classes:
 *  javax.annotation.Nullable
 *  net.minecraft.network.chat.Component
 *  net.minecraft.resources.ResourceLocation
 *  net.minecraft.server.level.ServerLevel
 *  net.minecraft.world.entity.Entity
 *  net.minecraft.world.phys.AABB
 */
package org.millenaire.goal.impl;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;
import javax.annotation.Nullable;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.AABB;
import org.millenaire.culture.ModCultures;
import org.millenaire.culture.VillagerType;
import org.millenaire.dialogue.Dialogue;
import org.millenaire.dialogue.DialogueLoader;
import org.millenaire.entity.MillVillager;
import org.millenaire.entity.ModelType;
import org.millenaire.entity.VillagerNavDriver;
import org.millenaire.goal.GoalContext;
import org.millenaire.goal.GoalScheduler;
import org.millenaire.goal.StopReason;
import org.millenaire.goal.TaskLabels;
import org.millenaire.goal.TravelPhase;
import org.millenaire.goal.VillagerGoal;
import org.millenaire.goal.VillagerTask;
import org.millenaire.goal.impl.SocialiseGoal;
import org.millenaire.language.SpeechRefCodec;
import org.millenaire.village.Village;

public class ChatGoal
implements VillagerGoal {
    private static final ResourceLocation ID = ResourceLocation.fromNamespaceAndPath((String)"millenaire", (String)"chat");
    private static final double SEARCH_RADIUS = 5.0;

    @Override
    public boolean showInTravelBook() {
        return false;
    }

    @Override
    public ResourceLocation id() {
        return ID;
    }

    @Override
    public int computePriority(GoalContext context) {
        return 10;
    }

    @Override
    public boolean isLeisure() {
        return true;
    }

    @Override
    public boolean canStart(GoalContext context) {
        return this.findPartner(context) != null;
    }

    @Override
    public VillagerTask start(GoalContext context) {
        MillVillager partner = this.findPartner(context);
        return new ChatTask(partner, context.villager());
    }

    @Nullable
    private MillVillager findPartner(GoalContext ctx) {
        List<MillVillager> nearby = ChatGoal.findAvailablePartners(ctx, ctx.villager());
        if (nearby.isEmpty()) {
            return null;
        }
        return nearby.get(ThreadLocalRandom.current().nextInt(nearby.size()));
    }

    private static List<MillVillager> findAvailablePartners(GoalContext ctx, MillVillager self) {
        AABB searchBox = self.getBoundingBox().inflate(5.0);
        return ctx.level().getEntitiesOfClass(MillVillager.class, searchBox, other -> other != self && other.getVillageId() != null && other.getVillageId().equals(self.getVillageId()) && ChatGoal.isAvailableForChat(other));
    }

    static boolean isLowestAvailablePartner(GoalContext ctx, MillVillager self) {
        List<MillVillager> nearby = ChatGoal.findAvailablePartners(ctx, self);
        if (nearby.isEmpty()) {
            return false;
        }
        for (MillVillager other : nearby) {
            if (other.getId() >= self.getId()) continue;
            return false;
        }
        return true;
    }

    private static boolean isAvailableForChat(MillVillager villager) {
        GoalScheduler scheduler = villager.getGoalScheduler();
        if (scheduler == null) {
            return false;
        }
        return scheduler.getCurrentTask() instanceof SocialiseGoal.SocialiseTask;
    }

    static class ChatTask
    implements VillagerTask {
        private static final double CHAT_DISTANCE = 3.0;
        private static final double WALK_SPEED = 0.5;
        private static final int FALLBACK_DISPLAY_TICKS = 80;
        private static final int LINE_DURATION_FACTOR = 2;
        private final MillVillager partner;
        private final MillVillager self;
        private Phase phase = Phase.WALKING;
        @Nullable
        private Dialogue dialogue;
        private int lineIndex;
        private int lineTimer;
        private boolean selfIsSpeaker1 = true;
        private boolean displaySubtitles = true;
        @Nullable
        private CompanionChatTask companionTask;

        ChatTask(@Nullable MillVillager partner, MillVillager self) {
            this.partner = partner;
            this.self = self;
        }

        @Override
        public ResourceLocation goalId() {
            return ID;
        }

        @Override
        public void tick(GoalContext ctx) {
            if (this.partner == null || !this.partner.isAlive()) {
                this.phase = Phase.DONE;
                return;
            }
            switch (this.phase.ordinal()) {
                case 0: {
                    this.tickWalking(ctx);
                    break;
                }
                case 1: {
                    this.tickChatting();
                    break;
                }
            }
        }

        private void tickWalking(GoalContext ctx) {
            VillagerNavDriver nav = ctx.villager().getNavManager();
            if (nav.getDestination() == null) {
                nav.navigateTo(ctx.villager(), this.partner.blockPosition(), 0.5);
            }
            if (nav.isArrived(ctx.villager(), 3.0)) {
                nav.stop(ctx.villager());
                ctx.villager().getLookControl().setLookAt((Entity)this.partner);
                this.startChatting(ctx);
            } else if (nav.isAbandoned()) {
                nav.stop(ctx.villager());
                this.phase = Phase.DONE;
            }
        }

        /*
         * WARNING - void declaration
         */
        private void startChatting(GoalContext ctx) {
            GoalScheduler partnerScheduler;
            void var11_17;
            String lang;
            ResourceLocation cultureId = ctx.village().getCultureId();
            List<Dialogue> allDialogues = DialogueLoader.getDialogues(cultureId, lang = "native");
            if (allDialogues.isEmpty()) {
                this.phase = Phase.DONE;
                return;
            }
            ServerLevel level = ctx.level();
            boolean isRaining = level.isRaining();
            ArrayList<Dialogue> eligible = new ArrayList<Dialogue>();
            ArrayList<Boolean> eligibleMapping = new ArrayList<Boolean>();
            for (Dialogue dialogue : allDialogues) {
                if (!ChatTask.checkTags(dialogue.tags(), isRaining) || !ChatTask.checkBuildingConditions(dialogue, ctx.village()) || !ChatTask.checkVillagerConditions(dialogue, ctx.village()) || !dialogue.relations().isEmpty()) continue;
                if (ChatTask.checkVillagerConstraint(dialogue.v1Constraint(), this.self) && ChatTask.checkVillagerConstraint(dialogue.v2Constraint(), this.partner)) {
                    eligible.add(dialogue);
                    eligibleMapping.add(true);
                    continue;
                }
                if (!ChatTask.checkVillagerConstraint(dialogue.v1Constraint(), this.partner) || !ChatTask.checkVillagerConstraint(dialogue.v2Constraint(), this.self)) continue;
                eligible.add(dialogue);
                eligibleMapping.add(false);
            }
            if (eligible.isEmpty()) {
                this.phase = Phase.DONE;
                return;
            }
            int totalWeight = 0;
            for (Dialogue dialogue : eligible) {
                totalWeight += dialogue.weight();
            }
            if (totalWeight <= 0) {
                this.phase = Phase.DONE;
                return;
            }
            int n = ThreadLocalRandom.current().nextInt(totalWeight);
            Dialogue dialogue = (Dialogue)eligible.get(0);
            boolean chosenMapping = (Boolean)eligibleMapping.get(0);
            int cumulative = 0;
            for (int i = 0; i < eligible.size(); ++i) {
                if (n >= (cumulative += ((Dialogue)eligible.get(i)).weight())) continue;
                Dialogue dialogue2 = (Dialogue)eligible.get(i);
                chosenMapping = (Boolean)eligibleMapping.get(i);
                break;
            }
            this.dialogue = var11_17;
            this.selfIsSpeaker1 = chosenMapping;
            this.lineIndex = 0;
            this.phase = Phase.CHATTING;
            AABB zone = this.self.getBoundingBox().inflate(5.0);
            for (MillVillager nearby : ctx.level().getEntitiesOfClass(MillVillager.class, zone, e -> e != this.self && e != this.partner)) {
                VillagerTask t;
                GoalScheduler s = nearby.getGoalScheduler();
                if (s == null || !((t = s.getCurrentTask()) instanceof ChatTask)) continue;
                ChatTask ct = (ChatTask)t;
                if (ct.phase != Phase.CHATTING || !ct.displaySubtitles) continue;
                this.displaySubtitles = false;
                break;
            }
            if ((partnerScheduler = this.partner.getGoalScheduler()) != null) {
                this.companionTask = new CompanionChatTask(this.self);
                partnerScheduler.forceTask(this.companionTask, null);
            }
            this.showCurrentLine();
        }

        private static boolean checkTags(List<String> tags, boolean isRaining) {
            for (String tag : tags) {
                if ("raining".equals(tag) && !isRaining) {
                    return false;
                }
                if (!"notraining".equals(tag) || !isRaining) continue;
                return false;
            }
            return true;
        }

        private static boolean checkBuildingConditions(Dialogue d, Village village) {
            for (String required : d.buildings()) {
                if (ChatTask.villagHasBuildingTag(village, required)) continue;
                return false;
            }
            for (String excluded : d.notBuildings()) {
                if (!ChatTask.villagHasBuildingTag(village, excluded)) continue;
                return false;
            }
            return true;
        }

        private static boolean villagHasBuildingTag(Village village, String tag) {
            return !village.getBuildingsWithTag(tag).isEmpty();
        }

        private static boolean checkVillagerConditions(Dialogue d, Village village) {
            for (String required : d.villagers()) {
                if (ChatTask.villageHasVillagerType(village, required)) continue;
                return false;
            }
            for (String excluded : d.notVillagers()) {
                if (!ChatTask.villageHasVillagerType(village, excluded)) continue;
                return false;
            }
            return true;
        }

        private static boolean villageHasVillagerType(Village village, String typeKey) {
            for (ResourceLocation typeId : village.getVillagerTypes().values()) {
                String path = typeId.getPath();
                int slashIdx = path.indexOf(47);
                String shortType = slashIdx >= 0 ? path.substring(slashIdx + 1) : path;
                if (!shortType.equals(typeKey)) continue;
                return true;
            }
            return false;
        }

        private static boolean checkVillagerConstraint(@Nullable String constraint, MillVillager villager) {
            if (constraint == null || constraint.isEmpty()) {
                return true;
            }
            VillagerType vType = ModCultures.getVillagerType(villager.getVillagerTypeId());
            if (vType == null) {
                return true;
            }
            for (String part : constraint.split(",")) {
                String effectiveKey;
                String c = part.trim();
                if (c.isEmpty()) continue;
                if ("child".equals(c) && !vType.isChild()) {
                    return false;
                }
                if ("adult".equals(c) && vType.isChild()) {
                    return false;
                }
                if ("male".equals(c) && vType.modelType() != ModelType.MALE) {
                    return false;
                }
                if ("female".equals(c) && vType.modelType() == ModelType.MALE) {
                    return false;
                }
                if (c.startsWith("vtype:")) {
                    String expected = c.substring(6);
                    effectiveKey = ChatTask.getEffectiveVillagerKey(villager, vType);
                    boolean matches = false;
                    String[] arrstring = expected.split("-");
                    int n = arrstring.length;
                    for (int i = 0; i < n; ++i) {
                        String vt = arrstring[i];
                        if (!effectiveKey.equals(vt)) continue;
                        matches = true;
                        break;
                    }
                    if (!matches) {
                        return false;
                    }
                }
                if (!c.startsWith("notvtype:")) continue;
                String excluded = c.substring(9);
                effectiveKey = ChatTask.getEffectiveVillagerKey(villager, vType);
                for (String vt : excluded.split("-")) {
                    if (!effectiveKey.equals(vt)) continue;
                    return false;
                }
            }
            return true;
        }

        private static String getEffectiveVillagerKey(MillVillager villager, VillagerType vType) {
            if (vType.isChild() && vType.altKey() != null && villager.getChildSize() >= 20) {
                return vType.altKey();
            }
            String typeId = villager.getVillagerTypeId() != null ? villager.getVillagerTypeId().getPath() : "";
            int slashIdx = typeId.lastIndexOf(47);
            return slashIdx >= 0 ? typeId.substring(slashIdx + 1) : typeId;
        }

        private void tickChatting() {
            if (this.dialogue == null) {
                this.phase = Phase.DONE;
                return;
            }
            this.self.getLookControl().setLookAt((Entity)this.partner);
            this.partner.getLookControl().setLookAt((Entity)this.self);
            --this.lineTimer;
            if (this.lineTimer <= 0) {
                ++this.lineIndex;
                if (this.lineIndex >= this.dialogue.lines().size()) {
                    this.self.setSpeechText("");
                    this.partner.setSpeechText("");
                    if (this.companionTask != null) {
                        this.companionTask.markDone();
                    }
                    this.phase = Phase.DONE;
                } else {
                    this.showCurrentLine();
                }
            }
        }

        private void showCurrentLine() {
            int base;
            MillVillager listener;
            MillVillager speaker;
            if (this.dialogue == null) {
                return;
            }
            Dialogue.Line line = this.dialogue.lines().get(this.lineIndex);
            if (this.selfIsSpeaker1) {
                speaker = line.speaker() == 1 ? this.self : this.partner;
                listener = line.speaker() == 1 ? this.partner : this.self;
            } else {
                speaker = line.speaker() == 1 ? this.partner : this.self;
                listener = line.speaker() == 1 ? this.self : this.partner;
            }
            ResourceLocation speakerTypeId = speaker.getVillagerTypeId();
            if (speakerTypeId == null) {
                return;
            }
            ResourceLocation cultureId = ModCultures.extractCultureId(speakerTypeId);
            String cultureKey = cultureId != null ? cultureId.getPath() : "unknown";
            String encodedTarget = SpeechRefCodec.encodeTargetName(listener.getFirstName());
            String speechRef = "d:" + cultureKey + ":" + this.dialogue.key() + ":" + this.lineIndex + ":" + encodedTarget;
            if (this.displaySubtitles) {
                speaker.setSpeechText(speechRef);
            }
            if (this.lineIndex + 1 < this.dialogue.lines().size()) {
                int nextDelay = this.dialogue.lines().get(this.lineIndex + 1).delay();
                int currentDelay = line.delay();
                base = Math.max(nextDelay - currentDelay, 80);
            } else {
                base = 80;
            }
            this.lineTimer = base * 2;
        }

        @Override
        public boolean isFinished() {
            return this.phase == Phase.DONE;
        }

        @Override
        public void stop(GoalContext ctx, StopReason reason) {
            if (ctx == null) {
                return;
            }
            ctx.villager().getNavManager().stop(ctx.villager());
            this.self.setSpeechText("");
            if (this.partner != null && this.partner.isAlive()) {
                this.partner.setSpeechText("");
            }
            if (this.companionTask != null) {
                this.companionTask.markDone();
            }
        }

        @Override
        public TravelPhase getTravelPhase() {
            return TaskLabels.phaseFor(this.phase != Phase.WALKING);
        }

        @Override
        @Nullable
        public Component getGoalLabel() {
            return TaskLabels.labelForPhase(this.phase != Phase.WALKING, "chat");
        }

        private static final class Phase
        extends Enum<Phase> {
            public static final /* enum */ Phase WALKING = new Phase();
            public static final /* enum */ Phase CHATTING = new Phase();
            public static final /* enum */ Phase DONE = new Phase();
            private static final /* synthetic */ Phase[] $VALUES;

            public static Phase[] values() {
                return (Phase[])$VALUES.clone();
            }

            public static Phase valueOf(String name) {
                return Enum.valueOf(Phase.class, name);
            }

            private static /* synthetic */ Phase[] $values() {
                return new Phase[]{WALKING, CHATTING, DONE};
            }

            static {
                $VALUES = Phase.$values();
            }
        }
    }

    static class CompanionChatTask
    implements VillagerTask {
        private final MillVillager initiator;
        private boolean done;

        CompanionChatTask(MillVillager initiator) {
            this.initiator = initiator;
        }

        @Override
        public ResourceLocation goalId() {
            return ID;
        }

        @Override
        public void tick(GoalContext ctx) {
            if (!this.initiator.isAlive()) {
                this.done = true;
                return;
            }
            ctx.villager().getLookControl().setLookAt((Entity)this.initiator);
        }

        @Override
        public boolean isFinished() {
            return this.done;
        }

        @Override
        public void stop(GoalContext ctx, StopReason reason) {
            if (ctx != null) {
                ctx.villager().setSpeechText("");
            }
        }

        @Override
        public TravelPhase getTravelPhase() {
            return TravelPhase.AT_DESTINATION;
        }

        @Override
        @Nullable
        public Component getGoalLabel() {
            return TaskLabels.labelForPhase(true, "chat");
        }

        void markDone() {
            this.done = true;
        }
    }
}

