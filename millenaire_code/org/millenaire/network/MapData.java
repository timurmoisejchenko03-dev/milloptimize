/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  io.netty.buffer.ByteBuf
 *  javax.annotation.Nullable
 *  net.minecraft.network.codec.ByteBufCodecs
 */
package org.millenaire.network;

import io.netty.buffer.ByteBuf;
import java.util.ArrayList;
import java.util.List;
import javax.annotation.Nullable;
import net.minecraft.network.codec.ByteBufCodecs;

public final class MapData {
    public static final byte TERRAIN_EMPTY = 0;
    public static final byte TERRAIN_WATER = 1;
    public static final byte TERRAIN_DANGER = 2;
    public static final byte TERRAIN_FORBIDDEN = 3;
    public static final byte TERRAIN_UNBUILDABLE = 4;
    public static final byte TERRAIN_BUILDABLE = 5;
    public static final byte TERRAIN_OCCUPIED = 6;

    private MapData() {
    }

    public static void encodeChunkList(ByteBuf buf, List<MapChunk> chunks) {
        ByteBufCodecs.VAR_INT.encode((Object)buf, (Object)chunks.size());
        for (MapChunk c : chunks) {
            ByteBufCodecs.VAR_INT.encode((Object)buf, (Object)c.chunkX());
            ByteBufCodecs.VAR_INT.encode((Object)buf, (Object)c.chunkZ());
            ByteBufCodecs.BOOL.encode((Object)buf, (Object)c.loaded());
        }
    }

    public static List<MapChunk> decodeChunkList(ByteBuf buf, int maxEntries) {
        int rawCount = (Integer)ByteBufCodecs.VAR_INT.decode((Object)buf);
        int count = Math.min(rawCount, maxEntries);
        ArrayList<MapChunk> chunks = new ArrayList<MapChunk>(count);
        for (int i = 0; i < rawCount; ++i) {
            int cx = (Integer)ByteBufCodecs.VAR_INT.decode((Object)buf);
            int cz = (Integer)ByteBufCodecs.VAR_INT.decode((Object)buf);
            boolean loaded = (Boolean)ByteBufCodecs.BOOL.decode((Object)buf);
            if (i >= count) continue;
            chunks.add(new MapChunk(cx, cz, loaded));
        }
        return chunks;
    }

    public static void encodeBuildingList(ByteBuf buf, List<MapBuilding> buildings) {
        ByteBufCodecs.VAR_INT.encode((Object)buf, (Object)buildings.size());
        for (MapBuilding b : buildings) {
            ByteBufCodecs.VAR_INT.encode((Object)buf, (Object)b.x());
            ByteBufCodecs.VAR_INT.encode((Object)buf, (Object)b.z());
            ByteBufCodecs.VAR_INT.encode((Object)buf, (Object)b.width());
            ByteBufCodecs.VAR_INT.encode((Object)buf, (Object)b.depth());
            ByteBufCodecs.STRING_UTF8.encode((Object)buf, (Object)b.name());
            ByteBufCodecs.STRING_UTF8.encode((Object)buf, (Object)b.status());
            ByteBufCodecs.VAR_INT.encode((Object)buf, (Object)b.level());
            ByteBufCodecs.BOOL.encode((Object)buf, (Object)b.nameTranslatable());
            ByteBufCodecs.STRING_UTF8.encode((Object)buf, (Object)(b.nativePrefix() != null ? b.nativePrefix() : ""));
            ByteBufCodecs.BOOL.encode((Object)buf, (Object)b.isWall());
        }
    }

    public static List<MapBuilding> decodeBuildingList(ByteBuf buf, int maxEntries) {
        int rawCount = (Integer)ByteBufCodecs.VAR_INT.decode((Object)buf);
        int count = Math.min(rawCount, maxEntries);
        ArrayList<MapBuilding> buildings = new ArrayList<MapBuilding>(count);
        for (int i = 0; i < rawCount; ++i) {
            int x = (Integer)ByteBufCodecs.VAR_INT.decode((Object)buf);
            int z = (Integer)ByteBufCodecs.VAR_INT.decode((Object)buf);
            int w = (Integer)ByteBufCodecs.VAR_INT.decode((Object)buf);
            int d = (Integer)ByteBufCodecs.VAR_INT.decode((Object)buf);
            String name = (String)ByteBufCodecs.STRING_UTF8.decode((Object)buf);
            String status = (String)ByteBufCodecs.STRING_UTF8.decode((Object)buf);
            int level = (Integer)ByteBufCodecs.VAR_INT.decode((Object)buf);
            boolean nameTranslatable = (Boolean)ByteBufCodecs.BOOL.decode((Object)buf);
            String nativePrefix = (String)ByteBufCodecs.STRING_UTF8.decode((Object)buf);
            String nativePrefixVal = nativePrefix.isEmpty() ? null : nativePrefix;
            boolean isWall = (Boolean)ByteBufCodecs.BOOL.decode((Object)buf);
            if (i >= count) continue;
            buildings.add(new MapBuilding(x, z, w, d, name, status, level, nameTranslatable, nativePrefixVal, isWall));
        }
        return buildings;
    }

    public static void encodeVillagerList(ByteBuf buf, List<MapVillager> villagers) {
        ByteBufCodecs.VAR_INT.encode((Object)buf, (Object)villagers.size());
        for (MapVillager v : villagers) {
            ByteBufCodecs.VAR_INT.encode((Object)buf, (Object)v.x());
            ByteBufCodecs.VAR_INT.encode((Object)buf, (Object)v.z());
            ByteBufCodecs.STRING_UTF8.encode((Object)buf, (Object)v.name());
            ByteBufCodecs.STRING_UTF8.encode((Object)buf, (Object)v.gender());
            ByteBufCodecs.STRING_UTF8.encode((Object)buf, (Object)v.role());
            ByteBufCodecs.STRING_UTF8.encode((Object)buf, (Object)v.goalLabel());
            ByteBufCodecs.BOOL.encode((Object)buf, (Object)v.isChief());
        }
    }

    public static void encodeTerrain(ByteBuf buf, MapTerrain terrain) {
        int run;
        ByteBufCodecs.VAR_INT.encode((Object)buf, (Object)terrain.startX());
        ByteBufCodecs.VAR_INT.encode((Object)buf, (Object)terrain.startZ());
        ByteBufCodecs.VAR_INT.encode((Object)buf, (Object)terrain.width());
        ByteBufCodecs.VAR_INT.encode((Object)buf, (Object)terrain.depth());
        byte[] data = terrain.data();
        if (data.length == 0) {
            ByteBufCodecs.VAR_INT.encode((Object)buf, (Object)0);
            return;
        }
        ArrayList<byte[]> rleChunks = new ArrayList<byte[]>();
        for (int i = 0; i < data.length; i += run) {
            byte val = data[i];
            for (run = 1; i + run < data.length && data[i + run] == val && run < 255; ++run) {
            }
            rleChunks.add(new byte[]{(byte)run, val});
        }
        ByteBufCodecs.VAR_INT.encode((Object)buf, (Object)rleChunks.size());
        for (byte[] chunk : rleChunks) {
            buf.writeByte((int)chunk[0]);
            buf.writeByte((int)chunk[1]);
        }
    }

    public static MapTerrain decodeTerrain(ByteBuf buf) {
        int startX = (Integer)ByteBufCodecs.VAR_INT.decode((Object)buf);
        int startZ = (Integer)ByteBufCodecs.VAR_INT.decode((Object)buf);
        int width = (Integer)ByteBufCodecs.VAR_INT.decode((Object)buf);
        int depth = (Integer)ByteBufCodecs.VAR_INT.decode((Object)buf);
        int rleCount = (Integer)ByteBufCodecs.VAR_INT.decode((Object)buf);
        if (rleCount == 0 || width <= 0 || depth <= 0 || width > 512 || depth > 512) {
            for (int r = 0; r < rleCount; ++r) {
                buf.readByte();
                buf.readByte();
            }
            return MapTerrain.EMPTY;
        }
        int totalSize = width * depth;
        byte[] data = new byte[totalSize];
        int pos = 0;
        for (int r = 0; r < rleCount; ++r) {
            int run = buf.readByte() & 0xFF;
            byte val = buf.readByte();
            int end = Math.min(pos + run, totalSize);
            for (int j = pos; j < end; ++j) {
                data[j] = val;
            }
            pos = end;
        }
        return new MapTerrain(startX, startZ, width, depth, data);
    }

    public static List<MapVillager> decodeVillagerList(ByteBuf buf, int maxEntries) {
        int rawCount = (Integer)ByteBufCodecs.VAR_INT.decode((Object)buf);
        int count = Math.min(rawCount, maxEntries);
        ArrayList<MapVillager> villagers = new ArrayList<MapVillager>(count);
        for (int i = 0; i < rawCount; ++i) {
            int x = (Integer)ByteBufCodecs.VAR_INT.decode((Object)buf);
            int z = (Integer)ByteBufCodecs.VAR_INT.decode((Object)buf);
            String name = (String)ByteBufCodecs.STRING_UTF8.decode((Object)buf);
            String gender = (String)ByteBufCodecs.STRING_UTF8.decode((Object)buf);
            String role = (String)ByteBufCodecs.STRING_UTF8.decode((Object)buf);
            String goalLabel = (String)ByteBufCodecs.STRING_UTF8.decode((Object)buf);
            boolean isChief = (Boolean)ByteBufCodecs.BOOL.decode((Object)buf);
            if (i >= count) continue;
            villagers.add(new MapVillager(x, z, name, gender, role, goalLabel, isChief));
        }
        return villagers;
    }

    public static void encodePathList(ByteBuf buf, List<MapPath> paths) {
        ByteBufCodecs.VAR_INT.encode((Object)buf, (Object)paths.size());
        for (MapPath p : paths) {
            ByteBufCodecs.VAR_INT.encode((Object)buf, (Object)p.x());
            ByteBufCodecs.VAR_INT.encode((Object)buf, (Object)p.z());
            buf.writeByte((int)p.level());
        }
    }

    public static List<MapPath> decodePathList(ByteBuf buf, int maxEntries) {
        int rawCount = (Integer)ByteBufCodecs.VAR_INT.decode((Object)buf);
        int count = Math.min(rawCount, maxEntries);
        ArrayList<MapPath> paths = new ArrayList<MapPath>(count);
        for (int i = 0; i < rawCount; ++i) {
            int x = (Integer)ByteBufCodecs.VAR_INT.decode((Object)buf);
            int z = (Integer)ByteBufCodecs.VAR_INT.decode((Object)buf);
            byte level = buf.readByte();
            if (i >= count) continue;
            paths.add(new MapPath(x, z, level));
        }
        return paths;
    }

    public record MapChunk(int chunkX, int chunkZ, boolean loaded) {
    }

    public record MapBuilding(int x, int z, int width, int depth, String name, String status, int level, boolean nameTranslatable, @Nullable String nativePrefix, boolean isWall) {
        public MapBuilding(int x, int z, int width, int depth, String name, String status, int level, boolean nameTranslatable, @Nullable String nativePrefix) {
            this(x, z, width, depth, name, status, level, nameTranslatable, nativePrefix, false);
        }

        public MapBuilding(int x, int z, int width, int depth, String name, String status, int level, boolean nameTranslatable) {
            this(x, z, width, depth, name, status, level, nameTranslatable, null, false);
        }

        public MapBuilding(int x, int z, int width, int depth, String name, String status, int level) {
            this(x, z, width, depth, name, status, level, false, null, false);
        }
    }

    public record MapVillager(int x, int z, String name, String gender, String role, String goalLabel, boolean isChief) {
    }

    public record MapTerrain(int startX, int startZ, int width, int depth, byte[] data) {
        public static final MapTerrain EMPTY = new MapTerrain(0, 0, 0, 0, new byte[0]);

        public byte tileAt(int worldX, int worldZ) {
            int lx = worldX - this.startX;
            int lz = worldZ - this.startZ;
            if (lx < 0 || lx >= this.width || lz < 0 || lz >= this.depth) {
                return 0;
            }
            return this.data[lx * this.depth + lz];
        }
    }

    public record MapPath(int x, int z, byte level) {
    }
}

