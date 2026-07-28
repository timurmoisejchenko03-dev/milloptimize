/*
 * Decompiled with CFR 0.150.
 * 
 * Could not load the following classes:
 *  javax.annotation.Nullable
 *  net.minecraft.core.BlockPos
 *  net.minecraft.resources.ResourceLocation
 *  net.minecraft.server.level.ServerLevel
 *  net.minecraft.world.entity.Entity
 *  net.minecraft.world.entity.EntityType
 *  net.minecraft.world.entity.EquipmentSlot
 *  net.minecraft.world.item.Item
 *  net.minecraft.world.item.ItemStack
 *  net.minecraft.world.level.ItemLike
 *  net.minecraft.world.level.Level
 */
package org.millenaire.catalog;

import javax.annotation.Nullable;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ItemLike;
import net.minecraft.world.level.Level;
import org.millenaire.catalog.CatalogCameraFraming;
import org.millenaire.culture.ModCultures;
import org.millenaire.culture.VillagerType;
import org.millenaire.entity.MillVillager;
import org.millenaire.entity.ModEntities;
import org.millenaire.entity.VillagerAppearanceFactory;
import org.millenaire.item.ItemHelper;

public final class CatalogVillagerStager {
    @Nullable
    private static MillVillager lastSpawned;

    private CatalogVillagerStager() {
    }

    @Nullable
    public static MillVillager spawn(ServerLevel level, ResourceLocation typeId, BlockPos pos) {
        VillagerType vt = ModCultures.getVillagerType(typeId);
        if (vt == null) {
            return null;
        }
        MillVillager villager = (MillVillager)((EntityType)ModEntities.MILL_VILLAGER.get()).create((Level)level);
        if (villager == null) {
            return null;
        }
        ItemStack mainHand = CatalogVillagerStager.resolveTravelBookItem(vt.travelBookHeldItem());
        ItemStack offHand = CatalogVillagerStager.resolveTravelBookItem(vt.travelBookHeldItemOffHand());
        float yaw = CatalogCameraFraming.villagerFacingYaw();
        villager.setVillagerTypeId(typeId);
        villager.moveTo((double)pos.getX() + 0.5, pos.getY(), (double)pos.getZ() + 0.5, yaw, 0.0f);
        villager.setYBodyRot(yaw);
        villager.yBodyRotO = yaw;
        villager.setYHeadRot(yaw);
        villager.yHeadRotO = yaw;
        VillagerAppearanceFactory.randomizeAppearance(villager, vt);
        villager.updateClothTextures(vt);
        villager.setItemSlot(EquipmentSlot.MAINHAND, mainHand);
        villager.setItemSlot(EquipmentSlot.OFFHAND, offHand);
        villager.setNoAi(true);
        villager.setNoGravity(true);
        villager.setGuiPreviewMode(true);
        villager.setPersistenceRequired();
        level.addFreshEntity((Entity)villager);
        lastSpawned = villager;
        return villager;
    }

    private static ItemStack resolveTravelBookItem(@Nullable String itemId) {
        if (itemId == null || itemId.isBlank()) {
            return ItemStack.EMPTY;
        }
        Item item = ItemHelper.resolve(itemId);
        return item == null ? ItemStack.EMPTY : new ItemStack((ItemLike)item);
    }

    public static void discardPrevious() {
        if (lastSpawned != null) {
            lastSpawned.discard();
            lastSpawned = null;
        }
    }
}

