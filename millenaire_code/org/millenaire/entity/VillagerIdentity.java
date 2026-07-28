/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  javax.annotation.Nullable
 *  net.minecraft.nbt.CompoundTag
 *  net.minecraft.resources.ResourceLocation
 */
package org.millenaire.entity;

import javax.annotation.Nullable;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import org.millenaire.entity.ModelType;

public class VillagerIdentity {
    private ModelType modelType = ModelType.MALE;
    @Nullable
    private ResourceLocation texture;
    @Nullable
    private ResourceLocation clothTexture0;
    @Nullable
    private ResourceLocation clothTexture1;
    private float villagerScale = 1.0f;
    private String firstName = "";
    private String familyName = "";
    private String roleName = "";
    private String fathersName = "";
    private String mothersName = "";
    private String spousesName = "";
    private String maidenName = "";
    private int childSize = -1;
    @Nullable
    private String clothName;

    public VillagerIdentity() {
    }

    public VillagerIdentity(VillagerIdentity other) {
        this.modelType = other.modelType;
        this.texture = other.texture;
        this.clothTexture0 = other.clothTexture0;
        this.clothTexture1 = other.clothTexture1;
        this.villagerScale = other.villagerScale;
        this.firstName = other.firstName;
        this.familyName = other.familyName;
        this.roleName = other.roleName;
        this.fathersName = other.fathersName;
        this.mothersName = other.mothersName;
        this.spousesName = other.spousesName;
        this.maidenName = other.maidenName;
        this.childSize = other.childSize;
        this.clothName = other.clothName;
    }

    public ModelType getModelType() {
        return this.modelType;
    }

    public void setModelType(ModelType modelType) {
        this.modelType = modelType;
    }

    @Nullable
    public ResourceLocation getTexture() {
        return this.texture;
    }

    public void setTexture(@Nullable ResourceLocation texture) {
        this.texture = texture;
    }

    @Nullable
    public ResourceLocation getClothTexture0() {
        return this.clothTexture0;
    }

    public void setClothTexture0(@Nullable ResourceLocation clothTexture0) {
        this.clothTexture0 = clothTexture0;
    }

    @Nullable
    public ResourceLocation getClothTexture1() {
        return this.clothTexture1;
    }

    public void setClothTexture1(@Nullable ResourceLocation clothTexture1) {
        this.clothTexture1 = clothTexture1;
    }

    public float getVillagerScale() {
        return this.villagerScale;
    }

    public void setVillagerScale(float villagerScale) {
        this.villagerScale = villagerScale;
    }

    public String getFirstName() {
        return this.firstName;
    }

    public void setFirstName(String firstName) {
        this.firstName = firstName;
    }

    public String getFamilyName() {
        return this.familyName;
    }

    public void setFamilyName(String familyName) {
        this.familyName = familyName;
    }

    public String getRoleName() {
        return this.roleName;
    }

    public void setRoleName(String roleName) {
        this.roleName = roleName;
    }

    public String getFathersName() {
        return this.fathersName;
    }

    public void setFathersName(String fathersName) {
        this.fathersName = fathersName;
    }

    public String getMothersName() {
        return this.mothersName;
    }

    public void setMothersName(String mothersName) {
        this.mothersName = mothersName;
    }

    public String getSpousesName() {
        return this.spousesName;
    }

    public void setSpousesName(String spousesName) {
        this.spousesName = spousesName;
    }

    public String getMaidenName() {
        return this.maidenName;
    }

    public void setMaidenName(String maidenName) {
        this.maidenName = maidenName;
    }

    public int getChildSize() {
        return this.childSize;
    }

    public void setChildSize(int childSize) {
        this.childSize = childSize;
    }

    @Nullable
    public String getClothName() {
        return this.clothName;
    }

    public void setClothName(@Nullable String clothName) {
        this.clothName = clothName;
    }

    public void save(CompoundTag tag) {
        tag.putByte("model_type", this.modelType.toByte());
        if (this.texture != null) {
            tag.putString("texture", this.texture.toString());
        }
        if (this.clothTexture0 != null) {
            tag.putString("cloth_texture_0", this.clothTexture0.toString());
        }
        if (this.clothTexture1 != null) {
            tag.putString("cloth_texture_1", this.clothTexture1.toString());
        }
        tag.putFloat("villager_scale", this.villagerScale);
        tag.putString("first_name", this.firstName);
        tag.putString("family_name", this.familyName);
        tag.putString("role_name", this.roleName);
        tag.putString("fathers_name", this.fathersName);
        tag.putString("mothers_name", this.mothersName);
        tag.putString("spouses_name", this.spousesName);
        tag.putString("maiden_name", this.maidenName);
        tag.putInt("child_size", this.childSize);
        if (this.clothName != null) {
            tag.putString("cloth_name", this.clothName);
        }
    }

    public static VillagerIdentity load(CompoundTag tag) {
        String clothNameKey;
        String childSizeKey;
        String maidenNameKey;
        String spousesNameKey;
        String mothersNameKey;
        String fathersNameKey;
        String roleNameKey;
        String scaleKey;
        String cloth1Key;
        String cloth0Key;
        VillagerIdentity id = new VillagerIdentity();
        String modelTypeKey = VillagerIdentity.firstPresentKey(tag, "model_type", "modelType");
        if (modelTypeKey != null) {
            id.modelType = ModelType.fromByte(tag.getByte(modelTypeKey));
        }
        if (tag.contains("texture")) {
            id.texture = ResourceLocation.parse((String)tag.getString("texture"));
        }
        if ((cloth0Key = VillagerIdentity.firstPresentKey(tag, "cloth_texture_0", "clothTexture0", "cloth_0")) != null) {
            id.clothTexture0 = ResourceLocation.parse((String)tag.getString(cloth0Key));
        }
        if ((cloth1Key = VillagerIdentity.firstPresentKey(tag, "cloth_texture_1", "clothTexture1", "cloth_1")) != null) {
            id.clothTexture1 = ResourceLocation.parse((String)tag.getString(cloth1Key));
        }
        if ((scaleKey = VillagerIdentity.firstPresentKey(tag, "villager_scale", "villagerScale", "scale")) != null) {
            id.villagerScale = tag.getFloat(scaleKey);
        }
        if (tag.contains("first_name")) {
            id.firstName = tag.getString("first_name");
        } else if (tag.contains("firstName")) {
            id.firstName = tag.getString("firstName");
        } else if (tag.contains("displayName")) {
            String oldDisplayName = tag.getString("displayName");
            int spaceIdx = oldDisplayName.indexOf(32);
            if (spaceIdx > 0) {
                id.firstName = oldDisplayName.substring(0, spaceIdx);
                id.familyName = oldDisplayName.substring(spaceIdx + 1);
            } else {
                id.firstName = oldDisplayName;
            }
        }
        String familyNameKey = VillagerIdentity.firstPresentKey(tag, "family_name", "familyName");
        if (familyNameKey != null) {
            id.familyName = tag.getString(familyNameKey);
        }
        if ((roleNameKey = VillagerIdentity.firstPresentKey(tag, "role_name", "roleName")) != null) {
            id.roleName = tag.getString(roleNameKey);
        }
        if ((fathersNameKey = VillagerIdentity.firstPresentKey(tag, "fathers_name", "fathersName")) != null) {
            id.fathersName = tag.getString(fathersNameKey);
        }
        if ((mothersNameKey = VillagerIdentity.firstPresentKey(tag, "mothers_name", "mothersName")) != null) {
            id.mothersName = tag.getString(mothersNameKey);
        }
        if ((spousesNameKey = VillagerIdentity.firstPresentKey(tag, "spouses_name", "spousesName")) != null) {
            id.spousesName = tag.getString(spousesNameKey);
        }
        if ((maidenNameKey = VillagerIdentity.firstPresentKey(tag, "maiden_name", "maidenName")) != null) {
            id.maidenName = tag.getString(maidenNameKey);
        }
        if ((childSizeKey = VillagerIdentity.firstPresentKey(tag, "child_size", "childSize")) != null) {
            id.childSize = tag.getInt(childSizeKey);
        }
        if ((clothNameKey = VillagerIdentity.firstPresentKey(tag, "cloth_name", "clothName")) != null) {
            id.clothName = tag.getString(clothNameKey);
        }
        return id;
    }

    @Nullable
    private static String firstPresentKey(CompoundTag tag, String ... keys) {
        for (String key : keys) {
            if (!tag.contains(key)) continue;
            return key;
        }
        return null;
    }
}

