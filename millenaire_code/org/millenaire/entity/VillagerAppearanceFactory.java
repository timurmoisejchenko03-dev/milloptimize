/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.mojang.logging.LogUtils
 *  javax.annotation.Nullable
 *  net.minecraft.resources.ResourceLocation
 *  org.slf4j.Logger
 */
package org.millenaire.entity;

import com.mojang.logging.LogUtils;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ThreadLocalRandom;
import javax.annotation.Nullable;
import net.minecraft.resources.ResourceLocation;
import org.millenaire.DisplayUtils;
import org.millenaire.culture.Gender;
import org.millenaire.culture.ModCultures;
import org.millenaire.culture.NameLists;
import org.millenaire.culture.VillagerType;
import org.millenaire.entity.MillVillager;
import org.millenaire.entity.ModelType;
import org.slf4j.Logger;

public final class VillagerAppearanceFactory {
    private static final Logger LOGGER = LogUtils.getLogger();
    public static final int MAX_CHILD_SIZE = 20;
    private static final float SCALE_BASE_MIN = 0.8f;
    private static final float SCALE_VARIATION = 0.09f;

    private VillagerAppearanceFactory() {
    }

    public static void randomizeAppearance(MillVillager villager, VillagerType vType) {
        float scale;
        ResourceLocation texture;
        ThreadLocalRandom random = ThreadLocalRandom.current();
        ModelType modelType = vType.modelType();
        List<ResourceLocation> textures = vType.textures();
        ResourceLocation resourceLocation = texture = textures.isEmpty() ? null : textures.get(random.nextInt(textures.size()));
        if (vType.isChild()) {
            if (villager.getChildSize() < 0) {
                villager.setChildSize(0);
            }
            scale = villager.getVillagerScale();
        } else {
            scale = vType.baseScale() * (0.8f + random.nextFloat() * 0.09f);
        }
        String roleName = DisplayUtils.resolveRoleKey(vType.id());
        String[] names = VillagerAppearanceFactory.generateName(vType);
        villager.initAppearance(modelType, texture, null, null, scale, names[0], names[1], roleName);
        villager.updateClothTextures(vType);
    }

    public static String[] generateName(VillagerType vType) {
        ResourceLocation cultureId = vType.culture();
        NameLists nameLists = ModCultures.getNameLists(cultureId);
        if (nameLists == null) {
            LOGGER.warn("[Mill\u00e9naire] NameLists not loaded for culture {} \u2014 villager will get a placeholder name", (Object)cultureId);
            return new String[]{"entity.millenaire.villager", ""};
        }
        String firstNameKey = vType.firstNameList();
        if (firstNameKey == null) {
            firstNameKey = vType.gender() == Gender.FEMALE ? "women_names" : "men_names";
        }
        String firstName = nameLists.randomFrom(firstNameKey);
        String familyName = nameLists.randomFrom(VillagerAppearanceFactory.resolveFamilyKey(vType));
        if (firstName == null) {
            LOGGER.warn("[Mill\u00e9naire] No first name found for key '{}' in culture {} \u2014 using placeholder", (Object)firstNameKey, (Object)cultureId);
            firstName = "entity.millenaire.villager";
        }
        if (familyName == null) {
            familyName = "";
        }
        return new String[]{firstName, familyName};
    }

    private static String resolveFamilyKey(VillagerType vType) {
        String familyKey = vType.familyNameList();
        if (familyKey == null) {
            familyKey = vType.hasTag("noble") ? "noble_family_names" : "family_names";
        }
        return familyKey;
    }

    public static String generateUniqueFamilyName(VillagerType vType, Set<String> namesTaken) {
        NameLists nameLists = ModCultures.getNameLists(vType.culture());
        if (nameLists == null) {
            return "";
        }
        String familyName = nameLists.randomFrom(VillagerAppearanceFactory.resolveFamilyKey(vType), namesTaken);
        return familyName == null ? "" : familyName;
    }

    public static boolean isCorruptedName(@Nullable String name) {
        return name != null && name.startsWith("entity.");
    }

    public static float computeChildScale(int childSize, Gender gender) {
        if (childSize >= 20) {
            return gender == Gender.FEMALE ? 0.8f : 0.9f;
        }
        return 0.5f + (float)childSize / 100.0f;
    }
}

