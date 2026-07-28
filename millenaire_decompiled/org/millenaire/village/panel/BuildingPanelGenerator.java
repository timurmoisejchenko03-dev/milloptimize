/*
 * Decompiled with CFR 0.150.
 * 
 * Could not load the following classes:
 *  javax.annotation.Nullable
 *  net.minecraft.core.registries.BuiltInRegistries
 *  net.minecraft.network.chat.Component
 *  net.minecraft.resources.ResourceLocation
 *  net.minecraft.server.level.ServerLevel
 *  net.minecraft.world.entity.Entity
 *  net.minecraft.world.item.Item
 *  net.minecraft.world.level.Level
 */
package org.millenaire.village.panel;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.UUID;
import javax.annotation.Nullable;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.Level;
import org.millenaire.DisplayUtils;
import org.millenaire.building.BuildingInstance;
import org.millenaire.building.BuildingInventory;
import org.millenaire.building.BuildingPlan;
import org.millenaire.building.BuildingPlanSet;
import org.millenaire.commerce.ShopProfile;
import org.millenaire.commerce.ShopProfileLoader;
import org.millenaire.commerce.TradeGood;
import org.millenaire.commerce.TradeGoodsLoader;
import org.millenaire.culture.Gender;
import org.millenaire.culture.ModCultures;
import org.millenaire.culture.VillagerType;
import org.millenaire.entity.MillVillager;
import org.millenaire.language.BuildingNameHelper;
import org.millenaire.village.Village;
import org.millenaire.village.VillagerRecord;
import org.millenaire.village.panel.PanelContent;
import org.millenaire.village.panel.PanelContentGenerator;
import org.millenaire.village.panel.PanelHelper;
import org.millenaire.village.panel.PanelLine;
import org.millenaire.village.panel.PanelType;

public final class BuildingPanelGenerator {
    private BuildingPanelGenerator() {
    }

    public static PanelContent generateBuildingDefault(Village village, @Nullable BuildingInstance building, @Nullable ServerLevel level) {
        String titleKey = building != null ? BuildingNameHelper.getServerFallbackName(building) : village.getVillageName();
        ArrayList<PanelLine> lines = new ArrayList<PanelLine>();
        if (building == null) {
            return new PanelContent(PanelType.BUILDING_DEFAULT, titleKey, lines);
        }
        BuildingPanelGenerator.addResidentsSection(lines, village, building, level);
        BuildingPanelGenerator.addTradedGoodsSection(lines, village, building);
        BuildingPanelGenerator.addStoredResourcesSection(lines, building, level);
        return new PanelContent(PanelType.BUILDING_DEFAULT, titleKey, lines, false, null);
    }

    private static void addResidentsSection(List<PanelLine> lines, Village village, BuildingInstance building, @Nullable ServerLevel level) {
        boolean expectsMale;
        ArrayList<ResidentInfo> residents = new ArrayList<ResidentInfo>();
        for (Map.Entry<UUID, ResourceLocation> entry : village.getVillagerTypes().entrySet()) {
            Entity entity;
            UUID uuid = entry.getKey();
            if (!building.getId().equals(village.getVillagerHome(uuid))) continue;
            ResourceLocation typeId = entry.getValue();
            VillagerType vType = ModCultures.getVillagerType(typeId);
            String roleKey = PanelHelper.resolveRoleKey(typeId);
            Object displayName = "???";
            if (level != null && (entity = level.getEntity(uuid)) instanceof MillVillager) {
                MillVillager mv = (MillVillager)entity;
                String first = mv.getFirstName();
                String family = mv.getFamilyName();
                if (first != null && !first.isEmpty()) {
                    displayName = first + (String)(family != null && !family.isEmpty() ? " " + family : "");
                }
            }
            Gender gender = vType != null ? vType.gender() : Gender.MALE;
            boolean isChild = vType != null && vType.isChild();
            residents.add(new ResidentInfo((String)displayName, roleKey, gender, isChild, typeId));
        }
        if (residents.isEmpty()) {
            return;
        }
        ArrayList<ResidentInfo> adults = new ArrayList<ResidentInfo>();
        ArrayList<ResidentInfo> children = new ArrayList<ResidentInfo>();
        for (ResidentInfo r : residents) {
            if (r.isChild()) {
                children.add(r);
                continue;
            }
            adults.add(r);
        }
        ArrayList<ResidentInfo> men = new ArrayList<ResidentInfo>();
        ArrayList<ResidentInfo> women = new ArrayList<ResidentInfo>();
        for (ResidentInfo r : adults) {
            if (r.gender() == Gender.FEMALE) {
                women.add(r);
                continue;
            }
            men.add(r);
        }
        BuildingPlanSet planSet = building.getPlanSetId() != null ? ModCultures.getBuildingPlanSet(building.getPlanSetId()) : null;
        boolean expectsFemale = planSet != null && !planSet.femaleResidents().isEmpty();
        boolean bl = expectsMale = planSet != null && !planSet.maleResidents().isEmpty();
        if (men.size() == 1 && women.size() == 1) {
            wife = (ResidentInfo)women.get(0);
            ResidentInfo husband = (ResidentInfo)men.get(0);
            BuildingPanelGenerator.addResidentTranslatableLine(lines, "panel.millenaire.woman_resident", wife.name(), wife.roleKey(), wife.typeId());
            BuildingPanelGenerator.addResidentTranslatableLine(lines, "panel.millenaire.man_resident", husband.name(), husband.roleKey(), husband.typeId());
        } else if (men.size() == 1 && women.isEmpty() && expectsFemale) {
            ResidentInfo husband = (ResidentInfo)men.get(0);
            BuildingPanelGenerator.addResidentTranslatableLine(lines, "panel.millenaire.man_resident", husband.name(), husband.roleKey(), husband.typeId());
            lines.add(PanelLine.translatable("panel.millenaire.bachelor"));
        } else if (women.size() == 1 && men.isEmpty() && expectsMale) {
            wife = (ResidentInfo)women.get(0);
            BuildingPanelGenerator.addResidentTranslatableLine(lines, "panel.millenaire.woman_resident", wife.name(), wife.roleKey(), wife.typeId());
            lines.add(PanelLine.translatable("panel.millenaire.spinster"));
        } else {
            for (ResidentInfo r : adults) {
                BuildingPanelGenerator.addResidentTranslatableLine(lines, "panel.millenaire.resident", r.name(), r.roleKey(), r.typeId());
            }
        }
        if (!children.isEmpty()) {
            lines.add(PanelLine.text(""));
            lines.add(PanelLine.translatable("panel.millenaire.children_section"));
            for (ResidentInfo child : children) {
                lines.add(PanelLine.text("  \u00a70" + child.name()));
            }
        }
    }

    private static void addResidentTranslatableLine(List<PanelLine> lines, String key, String name, String roleKey, ResourceLocation typeId) {
        PanelLine.PanelNavTarget nav = PanelHelper.villagerNavTarget(typeId);
        if (nav != null) {
            lines.add(PanelLine.clickableTranslatableWithMixedArgs(key, nav, 2, name, roleKey));
        } else {
            lines.add(PanelLine.translatableWithMixedArgs(key, 2, name, roleKey));
        }
    }

    private static void addTradedGoodsSection(List<PanelLine> lines, Village village, BuildingInstance building) {
        Item item;
        TradeGood good;
        boolean headerAdded;
        BuildingPlan plan = ModCultures.getBuildingPlan(building.getPlanId());
        String shopId = plan != null ? plan.shopId() : null;
        ResourceLocation cultureId = village.getCultureId();
        if (shopId == null || cultureId == null) {
            return;
        }
        ShopProfile profile = ShopProfileLoader.getProfile(cultureId, shopId);
        if (profile == null) {
            return;
        }
        String cultureKey = cultureId.getPath();
        if (!profile.sells().isEmpty()) {
            headerAdded = false;
            for (String goodId : profile.sells()) {
                good = TradeGoodsLoader.getGoodById(cultureId, goodId);
                if (good == null || !good.canSell() || (item = good.resolveItem()) == null) continue;
                if (!headerAdded) {
                    lines.add(PanelLine.separator());
                    lines.add(PanelLine.translatable("panel.millenaire.sold_here"));
                    headerAdded = true;
                }
                BuildingPanelGenerator.addTradeGoodLine(lines, good, item, cultureKey);
            }
        }
        if (!profile.buys().isEmpty()) {
            headerAdded = false;
            for (String goodId : profile.buys()) {
                good = TradeGoodsLoader.getGoodById(cultureId, goodId);
                if (good == null || !good.canBuy() || (item = good.resolveItem()) == null) continue;
                if (!headerAdded) {
                    lines.add(PanelLine.separator());
                    lines.add(PanelLine.translatable("panel.millenaire.bought_here"));
                    headerAdded = true;
                }
                BuildingPanelGenerator.addTradeGoodLine(lines, good, item, cultureKey);
            }
        }
    }

    private static void addTradeGoodLine(List<PanelLine> lines, TradeGood good, Item item, String cultureKey) {
        String descId = item.getDescriptionId();
        String iconId = BuiltInRegistries.ITEM.getKey((Object)item).toString();
        PanelLine.PanelNavTarget nav = PanelHelper.tradeGoodNavTarget(good, cultureKey);
        if (nav != null) {
            lines.add(PanelLine.clickableWithIconTranslatable(descId, "", iconId, nav, 0x555555));
        } else {
            lines.add(PanelLine.withIconTranslatable(descId, "", iconId, 0x555555));
        }
    }

    private static void addStoredResourcesSection(List<PanelLine> lines, BuildingInstance building, @Nullable ServerLevel level) {
        if (building.getInventory() == null || level == null) {
            return;
        }
        BuildingInventory inv = building.getInventory();
        Map<Item, Integer> contents = inv.scanChests((Level)level);
        record InvEntry(String descId, String itemId, int count) {
        }
        TreeMap<String, InvEntry> sorted = new TreeMap<String, InvEntry>();
        for (Map.Entry<Item, Integer> entry : contents.entrySet()) {
            if (entry.getValue() <= 0) continue;
            String descId = entry.getKey().getDescriptionId();
            String sortName = Component.translatable((String)descId).getString();
            ResourceLocation itemKey = BuiltInRegistries.ITEM.getKey((Object)entry.getKey());
            sorted.put(sortName, new InvEntry(descId, itemKey.toString(), entry.getValue()));
        }
        lines.add(PanelLine.separator());
        lines.add(PanelLine.translatable("panel.millenaire.stored_resources"));
        lines.add(PanelLine.text(""));
        if (sorted.isEmpty()) {
            lines.add(PanelLine.translatable("panel.millenaire.no_stored_resources"));
        } else {
            for (Map.Entry<Object, Integer> entry : sorted.entrySet()) {
                InvEntry entry2 = (InvEntry)((Object)entry.getValue());
                lines.add(PanelLine.withIconTranslatable(entry2.descId, "\u00a71" + entry2.count, entry2.itemId, 0));
            }
        }
    }

    public static PanelContent generateArchives(Village village, @Nullable BuildingInstance building, @Nullable ServerLevel level, int signIndex) {
        Entity entity;
        String spouse;
        String father;
        String iconItem;
        ArrayList<PanelLine> lines = new ArrayList<PanelLine>();
        String titleKey = "panel.millenaire.title.archives";
        ArrayList<Map.Entry<UUID, VillagerRecord>> records = new ArrayList<Map.Entry<UUID, VillagerRecord>>(village.getVillagerRecords().entrySet());
        if (signIndex < 0 || signIndex >= records.size()) {
            lines.add(PanelLine.translatableWithArgs("panel.millenaire.reserved_for_future", String.valueOf(signIndex + 1)));
            return new PanelContent(PanelType.ARCHIVES, titleKey, lines, true, null);
        }
        Map.Entry entry = (Map.Entry)records.get(signIndex);
        UUID uuid = (UUID)entry.getKey();
        VillagerRecord record = (VillagerRecord)entry.getValue();
        String firstName = record.getFirstName() != null ? record.getFirstName() : "???";
        String familyName = record.getFamilyName() != null ? record.getFamilyName() : "";
        String fullName = firstName + (String)(familyName.isEmpty() ? "" : " " + familyName);
        VillagerType vtype = ModCultures.getVillagerType(record.getVillagerTypeId());
        String string = iconItem = vtype != null && vtype.icon() != null ? vtype.icon() : "";
        if (!iconItem.isEmpty()) {
            lines.add(PanelLine.withIcon("\u00a71" + fullName, "", iconItem));
        } else {
            lines.add(PanelLine.text("\u00a71" + fullName));
        }
        lines.add(PanelLine.translatableColored(PanelHelper.resolveRoleKey(record.getVillagerTypeId()), 0));
        lines.add(PanelLine.text(""));
        String mother = record.getMothersName();
        if (mother != null && !mother.isEmpty()) {
            lines.add(PanelLine.translatableWithArgs("panel.millenaire.mother_value", mother));
        }
        if ((father = record.getFathersName()) != null && !father.isEmpty()) {
            lines.add(PanelLine.translatableWithArgs("panel.millenaire.father_value", father));
        }
        if ((spouse = record.getSpousesName()) != null && !spouse.isEmpty()) {
            lines.add(PanelLine.translatableWithArgs("panel.millenaire.spouse_value", spouse));
        }
        lines.add(PanelLine.text(""));
        MillVillager villager = null;
        if (level != null && (entity = level.getEntity(uuid)) instanceof MillVillager) {
            MillVillager mv;
            villager = mv = (MillVillager)entity;
        }
        lines.add(PanelLine.text(""));
        if (villager == null) {
            if (record.isKilled()) {
                lines.add(PanelLine.translatable("panel.millenaire.dead"));
            } else {
                lines.add(PanelLine.translatable("panel.millenaire.missing"));
            }
        } else {
            String occupation = "";
            String goalLabel = villager.getGoalLabel();
            if (goalLabel != null && !goalLabel.isEmpty()) {
                occupation = goalLabel;
            }
            if (DisplayUtils.isGoalTranslationKey(occupation)) {
                lines.add(PanelLine.translatableWithMixedArgs("panel.millenaire.occupation_value", 1, occupation));
            } else {
                lines.add(PanelLine.translatableWithArgs("panel.millenaire.occupation_value", occupation));
            }
        }
        return new PanelContent(PanelType.ARCHIVES, titleKey, lines, true, null);
    }

    static void addBuildingDefault3D(PanelContentGenerator.DisplayData data, Village village, BuildingInstance building, @Nullable ServerLevel level) {
        block8: {
            MillVillager mv;
            String icon;
            VillagerType vtype;
            ArrayList<Map.Entry<UUID, ResourceLocation>> residents;
            block6: {
                MillVillager wife;
                block7: {
                    String name;
                    String bldIcon = PanelHelper.resolveBuildingIcon(building);
                    data.addCenteredBuildingNameWithIcons(PanelHelper.getBuildingTranslationKey(building), bldIcon, bldIcon, BuildingNameHelper.getServerFallbackName(building));
                    data.addCentered("");
                    wife = null;
                    MillVillager husband = null;
                    String wifeIcon = "";
                    String husbandIcon = "";
                    int nbMaleAdults = 0;
                    int nbFemaleAdults = 0;
                    int nbResidents = 0;
                    residents = new ArrayList<Map.Entry<UUID, ResourceLocation>>();
                    for (Map.Entry<UUID, ResourceLocation> entry : village.getVillagerTypes().entrySet()) {
                        if (!building.getId().equals(village.getVillagerHome(entry.getKey()))) continue;
                        ++nbResidents;
                        residents.add(entry);
                        vtype = ModCultures.getVillagerType(entry.getValue());
                        if (vtype == null || vtype.isChild()) continue;
                        icon = vtype.icon() != null ? vtype.icon() : "";
                        MillVillager millVillager = mv = level != null ? PanelHelper.findVillager(level, entry.getKey()) : null;
                        if (vtype.gender() == Gender.FEMALE) {
                            ++nbFemaleAdults;
                            if (wife != null) continue;
                            wife = mv;
                            wifeIcon = icon;
                            continue;
                        }
                        ++nbMaleAdults;
                        if (husband != null) continue;
                        husband = mv;
                        husbandIcon = icon;
                    }
                    if (nbResidents == 0) {
                        return;
                    }
                    if (nbMaleAdults > true || nbFemaleAdults > true || wife == null && husband == null) break block6;
                    if (wife != null) {
                        name = wife.getFirstName() != null ? wife.getFirstName() : PanelHelper.resolveRoleName(PanelHelper.findTypeId(village, wife));
                        data.addCenteredWithIcons(name, wifeIcon, wifeIcon);
                    }
                    if (husband != null) {
                        name = husband.getFirstName() != null ? husband.getFirstName() : PanelHelper.resolveRoleName(PanelHelper.findTypeId(village, husband));
                        data.addCenteredWithIcons(name, husbandIcon, husbandIcon);
                    }
                    if (husband == null || husband.getFamilyName() == null) break block7;
                    data.addCentered(husband.getFamilyName());
                    break block8;
                }
                if (wife == null || wife.getFamilyName() == null) break block8;
                data.addCentered(wife.getFamilyName());
                break block8;
            }
            for (Map.Entry<UUID, Object> entry : residents) {
                vtype = ModCultures.getVillagerType((ResourceLocation)entry.getValue());
                icon = vtype != null && vtype.icon() != null ? vtype.icon() : "";
                mv = level != null ? PanelHelper.findVillager(level, entry.getKey()) : null;
                String name = mv != null && mv.getFirstName() != null ? mv.getFirstName() : PanelHelper.resolveRoleName((ResourceLocation)entry.getValue());
                data.addCenteredWithIcons(name, icon, icon);
                if (data.displayLines().size() < 7) continue;
                break;
            }
        }
    }

    static void addArchives3D(PanelContentGenerator.DisplayData data, Village village, BuildingInstance building, ServerLevel level, int signIndex) {
        int idx = 0;
        for (Map.Entry<UUID, ResourceLocation> entry : village.getVillagerTypes().entrySet()) {
            if (idx == signIndex) {
                MillVillager mv;
                VillagerType vtype = ModCultures.getVillagerType(entry.getValue());
                String icon = vtype != null && vtype.icon() != null ? vtype.icon() : "";
                Entity entity = level.getEntity(entry.getKey());
                if (entity instanceof MillVillager && (mv = (MillVillager)entity).getFirstName() != null) {
                    data.addCenteredWithIcons(mv.getFirstName(), icon, icon);
                    if (mv.getFamilyName() != null) {
                        data.addCentered(mv.getFamilyName());
                    }
                    data.addCentered("");
                    data.addCentered(PanelHelper.computeDirection(building.getOrigin(), mv.blockPosition()));
                    String goalLabel = mv.getGoalLabel();
                    if (goalLabel != null && !goalLabel.isEmpty()) {
                        if (DisplayUtils.isGoalTranslationKey(goalLabel)) {
                            data.addCenteredTranslatable(goalLabel);
                            break;
                        }
                        data.addCentered(goalLabel);
                        break;
                    }
                    data.addCentered(PanelHelper.resolveRoleName(entry.getValue()));
                    break;
                }
                data.addCenteredWithIcons(PanelHelper.resolveRoleName(entry.getValue()), icon, icon);
                data.addCentered("");
                data.addCentered(PanelHelper.t("panel.millenaire.missing"));
                break;
            }
            ++idx;
        }
        if (data.displayLines().isEmpty()) {
            data.addCentered(PanelHelper.t("panel.millenaire.reserved_for_future", ""));
        }
    }

    record ResidentInfo(String name, String roleKey, Gender gender, boolean isChild, ResourceLocation typeId) {
    }
}

