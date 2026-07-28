/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.google.gson.Gson
 *  com.google.gson.GsonBuilder
 *  com.mojang.brigadier.builder.LiteralArgumentBuilder
 *  com.mojang.brigadier.context.CommandContext
 *  net.minecraft.commands.CommandSourceStack
 *  net.minecraft.commands.Commands
 *  net.minecraft.core.BlockPos
 *  net.minecraft.core.Position
 *  net.minecraft.network.chat.Component
 *  net.minecraft.resources.ResourceLocation
 *  net.minecraft.server.level.ServerLevel
 *  net.minecraft.world.entity.Entity
 *  net.minecraft.world.entity.LivingEntity
 *  net.minecraft.world.level.Level
 */
package org.millenaire.command;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Position;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;
import org.millenaire.culture.ModCultures;
import org.millenaire.culture.VillageType;
import org.millenaire.culture.VillagerType;
import org.millenaire.entity.MillVillager;
import org.millenaire.goal.GoalScheduler;
import org.millenaire.goal.VillagerGoal;
import org.millenaire.village.Village;
import org.millenaire.village.VillageManager;
import org.millenaire.village.VillageSavedData;
import org.millenaire.village.VillagerRecord;

public final class MilitaryCommand {
    private static final Gson GSON = new GsonBuilder().create();
    private static final int SEARCH_RADIUS = 5000;

    private MilitaryCommand() {
    }

    public static void registerUnder(LiteralArgumentBuilder<CommandSourceStack> parent) {
        parent.then(Commands.literal((String)"military").executes(MilitaryCommand::execute));
    }

    private static int execute(CommandContext<CommandSourceStack> ctx) {
        CommandSourceStack source = (CommandSourceStack)ctx.getSource();
        ServerLevel level = source.getLevel();
        if (level.dimension() != Level.OVERWORLD) {
            source.sendFailure((Component)Component.translatable((String)"command.millenaire.error.not_in_overworld"));
            return 0;
        }
        BlockPos searchPos = BlockPos.containing((Position)source.getPosition());
        VillageSavedData savedData = VillageSavedData.get(level);
        VillageManager villageManager = savedData.getVillageManager();
        Village village = villageManager.findNearestVillage(searchPos, 5000.0);
        if (village == null) {
            source.sendSuccess(() -> Component.translatable((String)"command.millenaire.status.no_village_5000"), false);
            return 1;
        }
        LinkedHashMap<String, Object> payload = new LinkedHashMap<String, Object>();
        payload.put("village", MilitaryCommand.villageHeader(village));
        payload.put("strengths", MilitaryCommand.strengths(village));
        payload.put("fighters", MilitaryCommand.fighters(village, level));
        source.sendSuccess(() -> Component.literal((String)GSON.toJson((Object)payload)), false);
        return 1;
    }

    private static Map<String, Object> villageHeader(Village village) {
        LinkedHashMap<String, Object> header = new LinkedHashMap<String, Object>();
        header.put("id", village.getId().uuid().toString().substring(0, 8));
        header.put("type", village.getVillageTypeId().getPath());
        header.put("center", village.getCenter().toShortString());
        VillageType vt = ModCultures.getVillageType(village.getVillageTypeId());
        header.put("carries_raid", vt != null && vt.carriesRaid());
        return header;
    }

    private static Map<String, Integer> strengths(Village village) {
        LinkedHashMap<String, Integer> s = new LinkedHashMap<String, Integer>();
        s.put("raid", village.getVillageRaidingStrength());
        s.put("defense", village.getVillageDefendingStrength());
        s.put("attacker", village.getVillageAttackerStrength());
        return s;
    }

    private static List<Map<String, Object>> fighters(Village village, ServerLevel level) {
        ArrayList<Map<String, Object>> out = new ArrayList<Map<String, Object>>();
        for (VillagerRecord record : village.getVillagerRecords().values()) {
            ResourceLocation typeId = record.getVillagerTypeId();
            VillagerType type = ModCultures.getVillagerType(typeId);
            if (type == null || !type.isRaider() && !type.isHelpInAttacks()) continue;
            LinkedHashMap<String, Object> f = new LinkedHashMap<String, Object>();
            f.put("type", typeId.getPath());
            f.put("uuid", record.getUuid().toString().substring(0, 8));
            f.put("military_strength", record.getMilitaryStrength());
            ArrayList<String> tags = new ArrayList<String>();
            if (type.isRaider()) {
                tags.add("raider");
            }
            if (type.isHelpInAttacks()) {
                tags.add("defender");
            }
            f.put("tags", tags);
            String state = record.isKilled() ? "killed" : (record.isRaidingVillage() ? "raiding" : "alive");
            f.put("state", state);
            UUID uuid = record.getUuid();
            Entity entity = level.getEntity(uuid);
            if (entity instanceof MillVillager) {
                VillagerGoal goal;
                MillVillager mv = (MillVillager)entity;
                f.put("loaded", true);
                f.put("pos", mv.blockPosition().toShortString());
                f.put("attack_cooldown", mv.getAttackCooldown());
                LivingEntity target = mv.getAttackTarget();
                f.put("attack_target", target == null ? null : MilitaryCommand.describeTarget(target));
                GoalScheduler scheduler = mv.getGoalScheduler();
                String goalStr = "?";
                goalStr = scheduler != null ? ((goal = scheduler.getCurrentGoal()) != null ? goal.id().getPath() : "idle") : "no-sched";
                f.put("goal", goalStr);
            } else {
                f.put("loaded", false);
            }
            out.add(f);
        }
        return out;
    }

    private static String describeTarget(LivingEntity target) {
        String type = target.getType().getDescriptionId();
        String shortUuid = target.getUUID().toString().substring(0, 8);
        return type + ":" + shortUuid;
    }
}

