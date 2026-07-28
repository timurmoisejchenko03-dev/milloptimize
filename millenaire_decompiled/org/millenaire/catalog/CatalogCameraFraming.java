/*
 * Decompiled with CFR 0.150.
 * 
 * Could not load the following classes:
 *  net.minecraft.world.phys.Vec3
 */
package org.millenaire.catalog;

import net.minecraft.world.phys.Vec3;
import org.millenaire.catalog.CameraPose;

public final class CatalogCameraFraming {
    public static final float BUILDING_YAW_TWIST = 45.0f;
    private static final float BUILDING_PITCH = 30.0f;
    private static final float BUILDING_FOV = 50.0f;
    private static final double BUILDING_MARGIN = 1.1;
    private static final Vec3 WORLD_UP = new Vec3(0.0, 1.0, 0.0);
    private static final float VILLAGER_YAW = 25.0f;
    private static final float VILLAGER_PITCH = 12.0f;
    private static final float VILLAGER_FOV = 40.0f;
    private static final double VILLAGER_DISTANCE = 2.9;
    private static final double VILLAGER_CENTER_HEIGHT = 1.0;
    private static final double VILLAGER_AIM_SHIFT = -0.3;
    private static final float VILLAGER_THREE_QUARTER = 25.0f;

    private CatalogCameraFraming() {
    }

    public static CameraPose forVillager(Vec3 villagerPos) {
        Vec3 view = CatalogCameraFraming.viewVector(25.0f, 12.0f);
        Vec3 right = view.cross(WORLD_UP).normalize();
        Vec3 center = villagerPos.add(0.0, 1.0, 0.0).add(right.scale(-0.3));
        Vec3 pos = center.subtract(view.scale(2.9));
        return new CameraPose(pos, 25.0f, 12.0f, 40.0f);
    }

    public static float villagerFacingYaw() {
        return 230.0f;
    }

    public static CameraPose forBuilding(Vec3 origin, int width, int height, int depth, double aspect, float yaw) {
        Vec3 center = origin.add((double)width / 2.0, (double)height / 2.0, (double)depth / 2.0);
        Vec3 forward = CatalogCameraFraming.viewVector(yaw, 30.0f);
        Vec3 right = forward.cross(WORLD_UP).normalize();
        Vec3 up = right.cross(forward).normalize();
        double tanV = Math.tan(Math.toRadians(25.0));
        double tanH = tanV * aspect;
        double distance = 0.0;
        for (int cx = 0; cx <= 1; ++cx) {
            for (int cy = 0; cy <= 1; ++cy) {
                for (int cz = 0; cz <= 1; ++cz) {
                    Vec3 rel = origin.add((double)(cx * width), (double)(cy * height), (double)(cz * depth)).subtract(center);
                    double a = rel.dot(forward);
                    double h = Math.abs(rel.dot(right));
                    double v = Math.abs(rel.dot(up));
                    distance = Math.max(distance, h / tanH - a);
                    distance = Math.max(distance, v / tanV - a);
                }
            }
        }
        Vec3 pos = center.subtract(forward.scale(distance *= 1.1));
        return new CameraPose(pos, yaw, 30.0f, 50.0f);
    }

    public static float buildingYaw(int doorOrientation) {
        return (float)Math.floorMod(doorOrientation, 4) * 90.0f + 45.0f;
    }

    static Vec3 viewVector(float yawDeg, float pitchDeg) {
        double yaw = Math.toRadians(yawDeg);
        double pitch = Math.toRadians(pitchDeg);
        double x = -Math.sin(yaw) * Math.cos(pitch);
        double y = -Math.sin(pitch);
        double z = Math.cos(yaw) * Math.cos(pitch);
        return new Vec3(x, y, z);
    }
}

