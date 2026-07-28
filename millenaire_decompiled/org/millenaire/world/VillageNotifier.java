/*
 * Decompiled with CFR 0.150.
 * 
 * Could not load the following classes:
 *  net.minecraft.ChatFormatting
 *  net.minecraft.core.BlockPos
 *  net.minecraft.core.Vec3i
 *  net.minecraft.network.chat.Component
 *  net.minecraft.network.chat.MutableComponent
 *  net.minecraft.server.level.ServerLevel
 *  net.minecraft.server.level.ServerPlayer
 */
package org.millenaire.world;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Vec3i;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import org.millenaire.config.MillenaireServerConfig;
import org.millenaire.culture.Culture;
import org.millenaire.culture.ModCultures;
import org.millenaire.culture.VillageType;
import org.millenaire.village.Village;
import org.millenaire.village.VillageManager;
import org.millenaire.village.VillageSavedData;

public final class VillageNotifier {
    private static final String[] DIRECTION_KEYS = new String[]{"direction.millenaire.north", "direction.millenaire.northeast", "direction.millenaire.east", "direction.millenaire.southeast", "direction.millenaire.south", "direction.millenaire.southwest", "direction.millenaire.west", "direction.millenaire.northwest"};

    public static int getNotificationRadius() {
        return MillenaireServerConfig.SERVER.backgroundRadius.getAsInt();
    }

    private VillageNotifier() {
    }

    public static String cardinalDirection(BlockPos from, BlockPos to) {
        double dz;
        double dx = to.getX() - from.getX();
        double angle = Math.toDegrees(Math.atan2(dx, -(dz = (double)(to.getZ() - from.getZ()))));
        if (angle < 0.0) {
            angle += 360.0;
        }
        int sector = (int)Math.floor((angle + 22.5) / 45.0) % 8;
        return Component.translatable((String)DIRECTION_KEYS[sector]).getString();
    }

    public static String cardinalDirectionFromYaw(float yaw) {
        float normalized = (yaw % 360.0f + 360.0f) % 360.0f;
        int sector = (int)Math.floor(((double)(normalized + 180.0f) + 22.5) / 45.0) % 8;
        return Component.translatable((String)DIRECTION_KEYS[sector]).getString();
    }

    public static int horizontalDistance(BlockPos from, BlockPos to) {
        double dx = to.getX() - from.getX();
        double dz = to.getZ() - from.getZ();
        return (int)Math.round(Math.sqrt(dx * dx + dz * dz));
    }

    public static void notifySpawn(ServerLevel level, BlockPos villageCenter, String villageName, VillageType villageType) {
        double radiusSq = (double)VillageNotifier.getNotificationRadius() * (double)VillageNotifier.getNotificationRadius();
        List nearby = level.getPlayers(p -> p.blockPosition().distSqr((Vec3i)villageCenter) < radiusSq);
        boolean isLoneBuilding = villageType.loneBuilding();
        for (ServerPlayer player : nearby) {
            MutableComponent message;
            int distance = VillageNotifier.horizontalDistance(player.blockPosition(), villageCenter);
            String direction = VillageNotifier.cardinalDirection(player.blockPosition(), villageCenter);
            if (isLoneBuilding) {
                message = Component.translatable((String)"command.millenaire.new_lone_building_found", (Object[])new Object[]{villageType.name(), String.valueOf(distance), direction});
            } else {
                Culture culture = ModCultures.getCulture(villageType.culture());
                String cultureAdj = culture != null ? culture.displayName() : villageType.culture().getPath();
                message = Component.translatable((String)"command.millenaire.new_village_found", (Object[])new Object[]{villageName, villageType.name(), cultureAdj, String.valueOf(distance), direction});
            }
            player.sendSystemMessage((Component)message.withStyle(ChatFormatting.YELLOW));
        }
    }

    public static void sendVillageList(ServerPlayer player, ServerLevel level) {
        VillageSavedData savedData = VillageSavedData.get(level);
        VillageManager manager = savedData.getVillageManager();
        BlockPos playerPos = player.blockPosition();
        double radiusSq = (double)VillageNotifier.getNotificationRadius() * (double)VillageNotifier.getNotificationRadius();
        ArrayList<VillageInfo> infos = new ArrayList<VillageInfo>();
        for (Village village : manager.getAllVillages()) {
            double distSq = village.getCenter().distSqr((Vec3i)playerPos);
            if (!(distSq < radiusSq)) continue;
            int distance = VillageNotifier.horizontalDistance(playerPos, village.getCenter());
            String direction = VillageNotifier.cardinalDirection(playerPos, village.getCenter());
            infos.add(new VillageInfo(village, distance, direction));
        }
        infos.sort(Comparator.comparingInt(VillageInfo::distance).reversed());
        if (infos.isEmpty()) {
            player.sendSystemMessage((Component)Component.translatable((String)"command.millenaire.no_known_village").withStyle(ChatFormatting.GRAY));
            return;
        }
        for (VillageInfo info : infos) {
            MutableComponent line;
            Village village = info.village();
            VillageType vt = ModCultures.getVillageType(village.getVillageTypeId());
            if (vt != null && vt.loneBuilding()) {
                line = Component.translatable((String)"command.millenaire.villagelist_lonebuilding", (Object[])new Object[]{vt.name(), String.valueOf(info.distance()), info.direction()});
            } else {
                String villageName = village.getVillageName() != null ? village.getVillageName() : "???";
                String status = VillageNotifier.resolveStatus(village);
                String typeName = vt != null ? vt.name() : village.getVillageTypeId().getPath();
                line = Component.translatable((String)"command.millenaire.villagelist", (Object[])new Object[]{typeName, villageName, status, String.valueOf(info.distance()), info.direction()});
            }
            ChatFormatting color = vt != null && vt.loneBuilding() ? ChatFormatting.DARK_GRAY : ChatFormatting.GRAY;
            player.sendSystemMessage((Component)line.withStyle(color));
        }
        String facingDirection = VillageNotifier.cardinalDirectionFromYaw(player.getYRot());
        player.sendSystemMessage((Component)Component.translatable((String)"command.millenaire.facing_direction", (Object[])new Object[]{facingDirection}).withStyle(ChatFormatting.DARK_GREEN));
    }

    private static String resolveStatus(Village village) {
        if (village.isChunksForceLoaded() || village.isForceActive()) {
            return Component.translatable((String)"command.millenaire.active").getString();
        }
        return Component.translatable((String)"command.millenaire.inactive").getString();
    }

    private record VillageInfo(Village village, int distance, String direction) {
    }
}

