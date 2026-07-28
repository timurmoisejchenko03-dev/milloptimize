/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  javax.annotation.Nullable
 *  net.minecraft.resources.ResourceLocation
 *  net.minecraft.world.item.Item
 */
package org.millenaire.culture;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import javax.annotation.Nullable;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import org.millenaire.config.VillagerConfig;
import org.millenaire.culture.Gender;
import org.millenaire.entity.ModelType;
import org.millenaire.item.ItemHelper;

public record VillagerType(ResourceLocation id, ResourceLocation culture, String model, List<ResourceLocation> textures, Map<String, ClothSet> clothes, float baseScale, boolean isChild, List<ResourceLocation> goals, List<String> tags, int spawnWeight, Map<ResourceLocation, Integer> initialInventory, Gender gender, @Nullable String firstNameList, @Nullable String familyNameList, @Nullable String maleChild, @Nullable String femaleChild, List<String> bringBackHomeGoods, List<String> collectGoods, Map<String, Integer> requiredGoods, @Nullable String icon, List<String> toolNeededClasses, List<ResourceLocation> itemsNeeded, float maxHealth, @Nullable String villagerConfigKey, @Nullable String travelBookCategory, boolean travelBookDisplay, String nativeName, Map<ResourceLocation, Integer> foreignMerchantStock, int hiringCost, @Nullable String travelBookHeldItem, @Nullable String travelBookHeldItemOffHand, @Nullable String altNativeName, @Nullable String altKey, boolean travelBookMainCultureVillager, @Nullable ResourceLocation defaultWeapon, int baseAttackStrength, Set<Item> resolvedBringBackHomeGoods, Set<ResourceLocation> resolvedCollectGoods, Map<Item, Integer> resolvedRequiredGoods) {
    public boolean hasTag(String tag) {
        return this.tags.contains(tag);
    }

    public boolean isHelpInAttacks() {
        return this.tags.contains("helpInAttacks");
    }

    public boolean isRaider() {
        return this.tags.contains("raider");
    }

    public boolean isHostile() {
        return this.tags.contains("hostile");
    }

    public boolean isDefensive() {
        return this.tags.contains("defensive");
    }

    public boolean isArcher() {
        return this.tags.contains("archer");
    }

    public boolean isNoTeleport() {
        return this.tags.contains("noteleport");
    }

    public boolean isNoResurrect() {
        return this.tags.contains("noresurrect");
    }

    public ModelType modelType() {
        return ModelType.fromString(this.model);
    }

    /*
     * WARNING - void declaration
     */
    public VillagerType withResolvedGoods() {
        void var5_27;
        LinkedHashSet<Item> resolvedBBH = new LinkedHashSet<Item>();
        for (String string : this.bringBackHomeGoods) {
            Item item = ItemHelper.resolve(string);
            if (item == null) continue;
            resolvedBBH.add(item);
        }
        LinkedHashSet<ResourceLocation> resolvedCG = new LinkedHashSet<ResourceLocation>();
        for (String string : this.collectGoods) {
            resolvedCG.add(ResourceLocation.parse((String)string));
        }
        LinkedHashMap<Item, Integer> linkedHashMap = new LinkedHashMap<Item, Integer>();
        for (Map.Entry<String, Integer> entry : this.requiredGoods.entrySet()) {
            Item item = ItemHelper.resolve(entry.getKey());
            if (item == null) continue;
            linkedHashMap.put(item, entry.getValue());
        }
        VillagerConfig villagerConfig = VillagerConfig.get(this.villagerConfigKey);
        if (this.isChild) {
            for (Map.Entry<Item, Integer> entry : villagerConfig.getFoodGrowth().entrySet()) {
                linkedHashMap.putIfAbsent(entry.getKey(), 2);
            }
        }
        if (this.hasChildren()) {
            for (Map.Entry<Item, Integer> entry : villagerConfig.getFoodConception().entrySet()) {
                linkedHashMap.putIfAbsent(entry.getKey(), 2);
            }
        }
        List<ResourceLocation> list = this.goals;
        if (!this.toolNeededClasses.isEmpty()) {
            ResourceLocation getToolId = ResourceLocation.fromNamespaceAndPath((String)"millenaire", (String)"get_tool");
            boolean found = false;
            for (ResourceLocation g : this.goals) {
                if (!g.equals((Object)getToolId)) continue;
                found = true;
                break;
            }
            if (!found) {
                ArrayList<ResourceLocation> arrayList = new ArrayList<ResourceLocation>(this.goals);
                arrayList.add(getToolId);
            }
        }
        boolean isHelpInAttacks = this.tags.contains("helpInAttacks");
        boolean isRaiderTag = this.tags.contains("raider");
        if (isHelpInAttacks) {
            void var5_18;
            List<ResourceLocation> list3 = VillagerType.injectGoal((List<ResourceLocation>)var5_18, "hunt_monster");
            list3 = VillagerType.injectGoal(list3, "defend_village");
        }
        if (isRaiderTag) {
            void var5_21;
            List<ResourceLocation> list4 = VillagerType.injectGoal((List<ResourceLocation>)var5_21, "raid_village");
        }
        if (!isHelpInAttacks && !isRaiderTag) {
            void var5_23;
            List<ResourceLocation> list5 = VillagerType.injectGoal((List<ResourceLocation>)var5_23, "hide");
        }
        if (this.tags.contains("hostile")) {
            void var5_25;
            List<ResourceLocation> list6 = VillagerType.injectGoal((List<ResourceLocation>)var5_25, "hostile_villager");
        }
        return new VillagerType(this.id, this.culture, this.model, this.textures, this.clothes, this.baseScale, this.isChild, (List<ResourceLocation>)var5_27, this.tags, this.spawnWeight, this.initialInventory, this.gender, this.firstNameList, this.familyNameList, this.maleChild, this.femaleChild, this.bringBackHomeGoods, this.collectGoods, this.requiredGoods, this.icon, this.toolNeededClasses, this.itemsNeeded, this.maxHealth, this.villagerConfigKey, this.travelBookCategory, this.travelBookDisplay, this.nativeName, this.foreignMerchantStock, this.hiringCost, this.travelBookHeldItem, this.travelBookHeldItemOffHand, this.altNativeName, this.altKey, this.travelBookMainCultureVillager, this.defaultWeapon, this.baseAttackStrength, Set.copyOf(resolvedBBH), Set.copyOf(resolvedCG), Map.copyOf(linkedHashMap));
    }

    private static List<ResourceLocation> injectGoal(List<ResourceLocation> goals, String path) {
        ResourceLocation id = ResourceLocation.fromNamespaceAndPath((String)"millenaire", (String)path);
        for (ResourceLocation g : goals) {
            if (!g.equals((Object)id)) continue;
            return goals;
        }
        ArrayList<ResourceLocation> copy = new ArrayList<ResourceLocation>(goals);
        copy.add(id);
        return copy;
    }

    public boolean hasChildren() {
        return this.maleChild != null && this.femaleChild != null;
    }

    public boolean hasClothSet(String clothSetName) {
        return this.clothes.containsKey(clothSetName);
    }

    public boolean hasNaturalLayer(int layer) {
        ClothSet natural = this.clothes.get("natural");
        if (natural == null) {
            return false;
        }
        List<ResourceLocation> textures = layer == 0 ? natural.layer0() : natural.layer1();
        return textures != null && !textures.isEmpty();
    }

    public record ClothSet(List<ResourceLocation> layer0, List<ResourceLocation> layer1) {
    }
}

