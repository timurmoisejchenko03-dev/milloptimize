/*
 * Decompiled with CFR 0.150.
 * 
 * Could not load the following classes:
 *  com.google.gson.JsonArray
 *  com.google.gson.JsonElement
 *  com.google.gson.JsonObject
 *  com.google.gson.JsonParser
 *  com.mojang.logging.LogUtils
 *  net.minecraft.core.BlockPos
 *  net.minecraft.nbt.CompoundTag
 *  net.minecraft.resources.ResourceLocation
 *  net.minecraft.util.GsonHelper
 *  org.slf4j.Logger
 */
package org.millenaire.building;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.mojang.logging.LogUtils;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.GsonHelper;
import org.millenaire.building.MockBlockExtractor;
import org.millenaire.building.SpecialPoint;
import org.millenaire.content.ContentFs;
import org.millenaire.content.Resource;
import org.slf4j.Logger;

public final class SpecialPointsLoader {
    private static final Logger LOGGER = LogUtils.getLogger();

    private SpecialPointsLoader() {
    }

    public static List<SpecialPoint> load(ResourceLocation templateId, CompoundTag templateNbt) {
        List<SpecialPoint> fromNbt = MockBlockExtractor.extract(templateNbt);
        if (!fromNbt.isEmpty()) {
            LOGGER.debug("Loaded {} special points from NBT: {}", (Object)fromNbt.size(), (Object)templateId);
            return fromNbt;
        }
        return Collections.emptyList();
    }

    /*
     * Enabled aggressive block sorting
     * Enabled unnecessary exception pruning
     * Enabled aggressive exception aggregation
     */
    public static List<SpecialPoint> load(ResourceLocation templateId, String nbtPath, ContentFs cultureFs) {
        if (nbtPath == null || cultureFs == null) {
            LOGGER.debug("SpecialPoints: missing path/fs for {}", (Object)templateId);
            return Collections.emptyList();
        }
        Optional<Resource> resource = cultureFs.findFirst(nbtPath + "_special_points.json");
        if (resource.isEmpty()) {
            LOGGER.debug("No special points sidecar at {}", (Object)nbtPath);
            return Collections.emptyList();
        }
        try (InputStream is = resource.get().open();){
            JsonObject root = JsonParser.parseReader((Reader)new InputStreamReader(is, StandardCharsets.UTF_8)).getAsJsonObject();
            JsonArray pointsArray = root.getAsJsonArray("points");
            if (pointsArray == null) {
                List<SpecialPoint> list2 = Collections.emptyList();
                return list2;
            }
            ArrayList<SpecialPoint> points = new ArrayList<SpecialPoint>();
            for (JsonElement elem : pointsArray) {
                int z;
                int y;
                int x;
                JsonObject obj = elem.getAsJsonObject();
                String type = GsonHelper.getAsString((JsonObject)obj, (String)"type");
                String subtype = GsonHelper.getAsString((JsonObject)obj, (String)"subtype", null);
                String orientation = GsonHelper.getAsString((JsonObject)obj, (String)"orientation", null);
                String placement = GsonHelper.getAsString((JsonObject)obj, (String)"placement", null);
                if (obj.has("pos")) {
                    JsonArray posArr = obj.getAsJsonArray("pos");
                    x = posArr.get(0).getAsInt();
                    y = posArr.get(1).getAsInt();
                    z = posArr.get(2).getAsInt();
                } else {
                    x = obj.get("x").getAsInt();
                    y = obj.get("y").getAsInt();
                    z = obj.get("z").getAsInt();
                }
                points.add(new SpecialPoint(type, subtype, orientation, placement, new BlockPos(x, y, z)));
            }
            LOGGER.debug("Loaded {} special points from {}", (Object)points.size(), (Object)resource.get().relPath());
            List list = Collections.unmodifiableList(points);
            return list;
        }
        catch (Exception e) {
            LOGGER.warn("Error loading sidecar {}", (Object)resource.get().relPath(), (Object)e);
            return Collections.emptyList();
        }
    }
}

