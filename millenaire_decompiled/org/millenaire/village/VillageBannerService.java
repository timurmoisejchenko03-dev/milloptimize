/*
 * Decompiled with CFR 0.150.
 * 
 * Could not load the following classes:
 *  com.mojang.logging.LogUtils
 *  net.minecraft.core.Holder
 *  net.minecraft.core.HolderLookup$RegistryLookup
 *  net.minecraft.core.RegistryAccess
 *  net.minecraft.core.component.DataComponents
 *  net.minecraft.core.registries.Registries
 *  net.minecraft.nbt.CompoundTag
 *  net.minecraft.nbt.ListTag
 *  net.minecraft.nbt.TagParser
 *  net.minecraft.resources.ResourceKey
 *  net.minecraft.resources.ResourceLocation
 *  net.minecraft.util.RandomSource
 *  net.minecraft.world.item.DyeColor
 *  net.minecraft.world.item.ItemStack
 *  net.minecraft.world.item.Items
 *  net.minecraft.world.level.ItemLike
 *  net.minecraft.world.level.block.BannerBlock
 *  net.minecraft.world.level.block.entity.BannerPatternLayers
 *  net.minecraft.world.level.block.entity.BannerPatternLayers$Layer
 *  org.slf4j.Logger
 */
package org.millenaire.village;

import com.mojang.logging.LogUtils;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import net.minecraft.core.Holder;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.RegistryAccess;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.TagParser;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.ItemLike;
import net.minecraft.world.level.block.BannerBlock;
import net.minecraft.world.level.block.entity.BannerPatternLayers;
import org.millenaire.culture.Culture;
import org.millenaire.culture.VillageType;
import org.slf4j.Logger;

public final class VillageBannerService {
    private static final Logger LOGGER = LogUtils.getLogger();
    private static final Map<String, ResourceLocation> SHORTCODE_TO_PATTERN = VillageBannerService.buildShortcodeMap();

    private VillageBannerService() {
    }

    private static Map<String, ResourceLocation> buildShortcodeMap() {
        HashMap<String, ResourceLocation> m = new HashMap<String, ResourceLocation>();
        m.put("bl", VillageBannerService.rl("minecraft", "square_bottom_left"));
        m.put("br", VillageBannerService.rl("minecraft", "square_bottom_right"));
        m.put("tl", VillageBannerService.rl("minecraft", "square_top_left"));
        m.put("tr", VillageBannerService.rl("minecraft", "square_top_right"));
        m.put("bs", VillageBannerService.rl("minecraft", "stripe_bottom"));
        m.put("ts", VillageBannerService.rl("minecraft", "stripe_top"));
        m.put("ls", VillageBannerService.rl("minecraft", "stripe_left"));
        m.put("rs", VillageBannerService.rl("minecraft", "stripe_right"));
        m.put("cs", VillageBannerService.rl("minecraft", "stripe_center"));
        m.put("ms", VillageBannerService.rl("minecraft", "stripe_middle"));
        m.put("drs", VillageBannerService.rl("minecraft", "stripe_downright"));
        m.put("dls", VillageBannerService.rl("minecraft", "stripe_downleft"));
        m.put("ss", VillageBannerService.rl("minecraft", "small_stripes"));
        m.put("cr", VillageBannerService.rl("minecraft", "cross"));
        m.put("sc", VillageBannerService.rl("minecraft", "straight_cross"));
        m.put("bt", VillageBannerService.rl("minecraft", "triangle_bottom"));
        m.put("tt", VillageBannerService.rl("minecraft", "triangle_top"));
        m.put("bts", VillageBannerService.rl("minecraft", "triangles_bottom"));
        m.put("tts", VillageBannerService.rl("minecraft", "triangles_top"));
        m.put("ld", VillageBannerService.rl("minecraft", "diagonal_left"));
        m.put("rd", VillageBannerService.rl("minecraft", "diagonal_right"));
        m.put("lud", VillageBannerService.rl("minecraft", "diagonal_up_left"));
        m.put("rud", VillageBannerService.rl("minecraft", "diagonal_up_right"));
        m.put("vh", VillageBannerService.rl("minecraft", "half_vertical"));
        m.put("vhr", VillageBannerService.rl("minecraft", "half_vertical_right"));
        m.put("hh", VillageBannerService.rl("minecraft", "half_horizontal"));
        m.put("hhb", VillageBannerService.rl("minecraft", "half_horizontal_bottom"));
        m.put("bo", VillageBannerService.rl("minecraft", "border"));
        m.put("cbo", VillageBannerService.rl("minecraft", "curly_border"));
        m.put("cre", VillageBannerService.rl("minecraft", "creeper"));
        m.put("gra", VillageBannerService.rl("minecraft", "gradient"));
        m.put("gru", VillageBannerService.rl("minecraft", "gradient_up"));
        m.put("bri", VillageBannerService.rl("minecraft", "bricks"));
        m.put("sku", VillageBannerService.rl("minecraft", "skull"));
        m.put("flo", VillageBannerService.rl("minecraft", "flower"));
        m.put("moj", VillageBannerService.rl("minecraft", "mojang"));
        m.put("glb", VillageBannerService.rl("minecraft", "globe"));
        m.put("pig", VillageBannerService.rl("minecraft", "piglin"));
        m.put("mc", VillageBannerService.rl("minecraft", "circle"));
        m.put("mr", VillageBannerService.rl("minecraft", "rhombus"));
        m.put("byz", VillageBannerService.rl("millenaire", "byzantine"));
        m.put("by1", VillageBannerService.rl("millenaire", "byzantine_1"));
        m.put("by2", VillageBannerService.rl("millenaire", "byzantine_2"));
        m.put("sjk", VillageBannerService.rl("millenaire", "seljuk"));
        m.put("sjkr", VillageBannerService.rl("millenaire", "seljuk_rel"));
        m.put("sjkm", VillageBannerService.rl("millenaire", "seljuk_mil"));
        m.put("may", VillageBannerService.rl("millenaire", "mayan"));
        m.put("ma1", VillageBannerService.rl("millenaire", "mayan_1"));
        m.put("ma2", VillageBannerService.rl("millenaire", "mayan_2"));
        m.put("ma3", VillageBannerService.rl("millenaire", "mayan_3"));
        m.put("ma4", VillageBannerService.rl("millenaire", "mayan_4"));
        m.put("inu", VillageBannerService.rl("millenaire", "inuit"));
        m.put("iu1", VillageBannerService.rl("millenaire", "inuit_1"));
        m.put("iu2", VillageBannerService.rl("millenaire", "inuit_2"));
        m.put("iu3", VillageBannerService.rl("millenaire", "inuit_3"));
        m.put("iu4", VillageBannerService.rl("millenaire", "inuit_4"));
        m.put("ind", VillageBannerService.rl("millenaire", "indian"));
        m.put("in1", VillageBannerService.rl("millenaire", "indian_1"));
        m.put("in2", VillageBannerService.rl("millenaire", "indian_2"));
        m.put("in3", VillageBannerService.rl("millenaire", "indian_3"));
        m.put("in4", VillageBannerService.rl("millenaire", "indian_4"));
        m.put("in5", VillageBannerService.rl("millenaire", "indian_5"));
        m.put("nor", VillageBannerService.rl("millenaire", "norman"));
        m.put("jap", VillageBannerService.rl("millenaire", "japanese"));
        m.put("jaa", VillageBannerService.rl("millenaire", "japanese_agr"));
        m.put("jam", VillageBannerService.rl("millenaire", "japanese_mil"));
        m.put("jar", VillageBannerService.rl("millenaire", "japanese_rel"));
        m.put("jat", VillageBannerService.rl("millenaire", "japanese_tra"));
        return Map.copyOf(m);
    }

    private static ResourceLocation rl(String ns, String path) {
        return ResourceLocation.fromNamespaceAndPath((String)ns, (String)path);
    }

    public static ItemStack generateVillageBanner(VillageType type, RegistryAccess registryAccess, RandomSource random) {
        List<String> pool = type.bannerJsons();
        if (pool == null || pool.isEmpty()) {
            return ItemStack.EMPTY;
        }
        ArrayList<String> shuffled = new ArrayList<String>(pool);
        Collections.shuffle(shuffled, new Random(random.nextLong()));
        for (String nbt : shuffled) {
            ItemStack stack = VillageBannerService.parseLegacyBanner(nbt, registryAccess);
            if (stack.isEmpty()) continue;
            return stack;
        }
        LOGGER.warn("[banner] village type {} has {} banner entries but none parsed", (Object)type.id(), (Object)pool.size());
        return ItemStack.EMPTY;
    }

    public static ItemStack getCultureBanner(Culture culture, RegistryAccess registryAccess) {
        if (culture == null || culture.cultureBannerNbt() == null) {
            return ItemStack.EMPTY;
        }
        ItemStack built = VillageBannerService.parseLegacyBanner(culture.cultureBannerNbt(), registryAccess);
        if (built.isEmpty()) {
            LOGGER.warn("[banner] culture {} banner NBT did not parse: {}", (Object)culture.id(), (Object)culture.cultureBannerNbt());
        }
        return built;
    }

    public static ItemStack parseLegacyBanner(String legacyNbt, RegistryAccess registryAccess) {
        if (legacyNbt == null || legacyNbt.isBlank()) {
            return ItemStack.EMPTY;
        }
        try {
            CompoundTag root = TagParser.parseTag((String)legacyNbt);
            CompoundTag bet = root.contains("BlockEntityTag") ? root.getCompound("BlockEntityTag") : root;
            int legacyBase = bet.contains("Base") ? bet.getInt("Base") : 0;
            DyeColor baseColor = DyeColor.byId((int)(15 - legacyBase));
            ItemStack stack = new ItemStack((ItemLike)BannerBlock.byColor((DyeColor)baseColor).asItem(), 1);
            if (!bet.contains("Patterns")) {
                return stack;
            }
            ListTag patList = bet.getList("Patterns", 10);
            HolderLookup.RegistryLookup patternLookup = registryAccess.lookupOrThrow(Registries.BANNER_PATTERN);
            ArrayList<BannerPatternLayers.Layer> layers = new ArrayList<BannerPatternLayers.Layer>(patList.size());
            for (int i = 0; i < patList.size(); ++i) {
                CompoundTag pt = patList.getCompound(i);
                String code = pt.getString("Pattern");
                int legacyColor = pt.getInt("Color");
                DyeColor dye = DyeColor.byId((int)(15 - legacyColor));
                ResourceLocation patternId = SHORTCODE_TO_PATTERN.get(code);
                if (patternId == null) {
                    LOGGER.warn("[banner] unknown legacy pattern shortcode: '{}'", (Object)code);
                    continue;
                }
                Holder holder = patternLookup.get(ResourceKey.create((ResourceKey)Registries.BANNER_PATTERN, (ResourceLocation)patternId)).orElse(null);
                if (holder == null) {
                    LOGGER.warn("[banner] pattern {} missing from registry (asset: {})", (Object)code, (Object)patternId);
                    continue;
                }
                layers.add(new BannerPatternLayers.Layer(holder, dye));
            }
            if (!layers.isEmpty()) {
                stack.set(DataComponents.BANNER_PATTERNS, (Object)new BannerPatternLayers(layers));
            }
            return stack;
        }
        catch (Exception e) {
            LOGGER.warn("[banner] failed to parse '{}': {}", (Object)legacyNbt, (Object)e.getMessage());
            return ItemStack.EMPTY;
        }
    }

    public static ItemStack fallbackBanner() {
        return new ItemStack((ItemLike)Items.WHITE_BANNER);
    }
}

