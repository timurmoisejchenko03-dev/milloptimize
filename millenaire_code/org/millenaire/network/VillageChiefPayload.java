/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  io.netty.buffer.ByteBuf
 *  net.minecraft.network.codec.ByteBufCodecs
 *  net.minecraft.network.codec.StreamCodec
 *  net.minecraft.network.protocol.common.custom.CustomPacketPayload
 *  net.minecraft.network.protocol.common.custom.CustomPacketPayload$Type
 *  net.minecraft.resources.ResourceLocation
 */
package org.millenaire.network;

import io.netty.buffer.ByteBuf;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import org.millenaire.village.panel.PanelLine;

public record VillageChiefPayload(ChiefIdentity identity, ChiefDynamic dynamic) implements CustomPacketPayload
{
    public static final int CROP_REPUTATION = 8192;
    public static final int CROP_PRICE = 512;
    public static final int CULTURE_CONTROL_REPUTATION = 131072;
    private static final int MAX_RELATION_ENTRIES = 64;
    private static final int MAX_LIST_ENTRIES = 32;
    private static final int MAX_MILITARY_LINES = 512;
    private static final int MAX_MILITARY_LINE_ARGS = 8;
    public static final CustomPacketPayload.Type<VillageChiefPayload> TYPE = new CustomPacketPayload.Type(ResourceLocation.fromNamespaceAndPath((String)"millenaire", (String)"village_chief"));
    public static final StreamCodec<ByteBuf, VillageChiefPayload> STREAM_CODEC = StreamCodec.of(VillageChiefPayload::encode, VillageChiefPayload::decode);

    public int entityId() {
        return this.identity.entityId();
    }

    public String chiefName() {
        return this.identity.chiefName();
    }

    public String roleName() {
        return this.identity.roleName();
    }

    public String chiefTypeKey() {
        return this.identity.chiefTypeKey();
    }

    public String villageName() {
        return this.dynamic.villageName();
    }

    public String cultureName() {
        return this.dynamic.cultureName();
    }

    public int reputation() {
        return this.dynamic.reputation();
    }

    public String reputationLabel() {
        return this.dynamic.reputationLabel();
    }

    public int totalBuildings() {
        return this.dynamic.totalBuildings();
    }

    public int completeBuildings() {
        return this.dynamic.completeBuildings();
    }

    public int underConstructionBuildings() {
        return this.dynamic.underConstructionBuildings();
    }

    public int totalVillagers() {
        return this.dynamic.totalVillagers();
    }

    public String villageId() {
        return this.dynamic.villageId();
    }

    public int playerMoney() {
        return this.dynamic.playerMoney();
    }

    public int playerReputation() {
        return this.dynamic.playerReputation();
    }

    public List<PlayerBuildingEntry> playerBuildings() {
        return this.dynamic.playerBuildings();
    }

    public List<RelationEntry> relationEntries() {
        return this.dynamic.relationEntries();
    }

    public int diplomacyPoints() {
        return this.dynamic.diplomacyPoints();
    }

    public List<LearningOffer> cropOffers() {
        return this.dynamic.cropOffers();
    }

    public List<LearningOffer> huntingOffers() {
        return this.dynamic.huntingOffers();
    }

    public boolean cultureControlAvailable() {
        return this.dynamic.cultureControlAvailable();
    }

    public boolean hasCultureControl() {
        return this.dynamic.hasCultureControl();
    }

    public String cultureId() {
        return this.dynamic.cultureId();
    }

    public boolean controlledByPlayer() {
        return this.dynamic.controlledByPlayer();
    }

    public String currentRaidTargetVillageId() {
        return this.dynamic.currentRaidTargetVillageId();
    }

    public boolean raidInProgress() {
        return this.dynamic.raidInProgress();
    }

    public List<PanelLine> militaryLines() {
        return this.dynamic.militaryLines();
    }

    private static void encode(ByteBuf buf, VillageChiefPayload payload) {
        VillageChiefPayload.encodeIdentity(buf, payload.identity);
        VillageChiefPayload.encodeDynamic(buf, payload.dynamic);
    }

    private static VillageChiefPayload decode(ByteBuf buf) {
        ChiefIdentity identity = VillageChiefPayload.decodeIdentity(buf);
        ChiefDynamic dynamic = VillageChiefPayload.decodeDynamic(buf);
        return new VillageChiefPayload(identity, dynamic);
    }

    static void encodeIdentity(ByteBuf buf, ChiefIdentity identity) {
        ByteBufCodecs.VAR_INT.encode((Object)buf, (Object)identity.entityId());
        ByteBufCodecs.STRING_UTF8.encode((Object)buf, (Object)identity.chiefName());
        ByteBufCodecs.STRING_UTF8.encode((Object)buf, (Object)identity.roleName());
        ByteBufCodecs.STRING_UTF8.encode((Object)buf, (Object)identity.chiefTypeKey());
    }

    static ChiefIdentity decodeIdentity(ByteBuf buf) {
        int entityId = (Integer)ByteBufCodecs.VAR_INT.decode((Object)buf);
        String chiefName = (String)ByteBufCodecs.STRING_UTF8.decode((Object)buf);
        String roleName = (String)ByteBufCodecs.STRING_UTF8.decode((Object)buf);
        String chiefTypeKey = (String)ByteBufCodecs.STRING_UTF8.decode((Object)buf);
        return new ChiefIdentity(entityId, chiefName, roleName, chiefTypeKey);
    }

    static void encodeDynamic(ByteBuf buf, ChiefDynamic d) {
        ByteBufCodecs.STRING_UTF8.encode((Object)buf, (Object)d.villageName());
        ByteBufCodecs.STRING_UTF8.encode((Object)buf, (Object)d.cultureName());
        ByteBufCodecs.VAR_INT.encode((Object)buf, (Object)d.reputation());
        ByteBufCodecs.STRING_UTF8.encode((Object)buf, (Object)d.reputationLabel());
        ByteBufCodecs.VAR_INT.encode((Object)buf, (Object)d.totalBuildings());
        ByteBufCodecs.VAR_INT.encode((Object)buf, (Object)d.completeBuildings());
        ByteBufCodecs.VAR_INT.encode((Object)buf, (Object)d.underConstructionBuildings());
        ByteBufCodecs.VAR_INT.encode((Object)buf, (Object)d.totalVillagers());
        ByteBufCodecs.STRING_UTF8.encode((Object)buf, (Object)d.villageId());
        ByteBufCodecs.VAR_INT.encode((Object)buf, (Object)d.playerMoney());
        ByteBufCodecs.VAR_INT.encode((Object)buf, (Object)d.playerReputation());
        ByteBufCodecs.VAR_INT.encode((Object)buf, (Object)d.playerBuildings().size());
        for (PlayerBuildingEntry entry : d.playerBuildings()) {
            ByteBufCodecs.STRING_UTF8.encode((Object)buf, (Object)entry.planSetId());
            ByteBufCodecs.STRING_UTF8.encode((Object)buf, (Object)entry.nativeName());
            ByteBufCodecs.VAR_INT.encode((Object)buf, (Object)entry.price());
            ByteBufCodecs.VAR_INT.encode((Object)buf, (Object)entry.reputation());
            ByteBufCodecs.STRING_UTF8.encode((Object)buf, (Object)entry.status());
            ByteBufCodecs.STRING_UTF8.encode((Object)buf, (Object)entry.translationKey());
        }
        ByteBufCodecs.VAR_INT.encode((Object)buf, (Object)d.relationEntries().size());
        for (RelationEntry re : d.relationEntries()) {
            ByteBufCodecs.STRING_UTF8.encode((Object)buf, (Object)re.villageId());
            ByteBufCodecs.STRING_UTF8.encode((Object)buf, (Object)re.villageName());
            ByteBufCodecs.STRING_UTF8.encode((Object)buf, (Object)re.cultureName());
            ByteBufCodecs.VAR_INT.encode((Object)buf, (Object)re.relation());
            ByteBufCodecs.STRING_UTF8.encode((Object)buf, (Object)re.relationLabel());
        }
        ByteBufCodecs.VAR_INT.encode((Object)buf, (Object)d.diplomacyPoints());
        ByteBufCodecs.VAR_INT.encode((Object)buf, (Object)d.cropOffers().size());
        for (LearningOffer offer : d.cropOffers()) {
            ByteBufCodecs.STRING_UTF8.encode((Object)buf, (Object)offer.key());
            ByteBufCodecs.STRING_UTF8.encode((Object)buf, (Object)offer.itemName());
            ByteBufCodecs.STRING_UTF8.encode((Object)buf, (Object)offer.status());
        }
        ByteBufCodecs.VAR_INT.encode((Object)buf, (Object)d.huntingOffers().size());
        for (LearningOffer offer : d.huntingOffers()) {
            ByteBufCodecs.STRING_UTF8.encode((Object)buf, (Object)offer.key());
            ByteBufCodecs.STRING_UTF8.encode((Object)buf, (Object)offer.itemName());
            ByteBufCodecs.STRING_UTF8.encode((Object)buf, (Object)offer.status());
        }
        ByteBufCodecs.BOOL.encode((Object)buf, (Object)d.cultureControlAvailable());
        ByteBufCodecs.BOOL.encode((Object)buf, (Object)d.hasCultureControl());
        ByteBufCodecs.STRING_UTF8.encode((Object)buf, (Object)d.cultureId());
        ByteBufCodecs.BOOL.encode((Object)buf, (Object)d.controlledByPlayer());
        ByteBufCodecs.STRING_UTF8.encode((Object)buf, (Object)d.currentRaidTargetVillageId());
        ByteBufCodecs.BOOL.encode((Object)buf, (Object)d.raidInProgress());
        VillageChiefPayload.encodeMilitaryLines(buf, d.militaryLines());
    }

    static ChiefDynamic decodeDynamic(ByteBuf buf) {
        int i;
        int i2;
        int i3;
        String villageName = (String)ByteBufCodecs.STRING_UTF8.decode((Object)buf);
        String cultureName = (String)ByteBufCodecs.STRING_UTF8.decode((Object)buf);
        int reputation = (Integer)ByteBufCodecs.VAR_INT.decode((Object)buf);
        String reputationLabel = (String)ByteBufCodecs.STRING_UTF8.decode((Object)buf);
        int totalBuildings = (Integer)ByteBufCodecs.VAR_INT.decode((Object)buf);
        int completeBuildings = (Integer)ByteBufCodecs.VAR_INT.decode((Object)buf);
        int underConstructionBuildings = (Integer)ByteBufCodecs.VAR_INT.decode((Object)buf);
        int totalVillagers = (Integer)ByteBufCodecs.VAR_INT.decode((Object)buf);
        String villageId = (String)ByteBufCodecs.STRING_UTF8.decode((Object)buf);
        int playerMoney = (Integer)ByteBufCodecs.VAR_INT.decode((Object)buf);
        int playerReputation = (Integer)ByteBufCodecs.VAR_INT.decode((Object)buf);
        int pbCount = Math.min((Integer)ByteBufCodecs.VAR_INT.decode((Object)buf), 32);
        ArrayList<PlayerBuildingEntry> playerBuildings = new ArrayList<PlayerBuildingEntry>(pbCount);
        for (int i4 = 0; i4 < pbCount; ++i4) {
            playerBuildings.add(new PlayerBuildingEntry((String)ByteBufCodecs.STRING_UTF8.decode((Object)buf), (String)ByteBufCodecs.STRING_UTF8.decode((Object)buf), (Integer)ByteBufCodecs.VAR_INT.decode((Object)buf), (Integer)ByteBufCodecs.VAR_INT.decode((Object)buf), (String)ByteBufCodecs.STRING_UTF8.decode((Object)buf), (String)ByteBufCodecs.STRING_UTF8.decode((Object)buf)));
        }
        int rawReCount = (Integer)ByteBufCodecs.VAR_INT.decode((Object)buf);
        int reCount = Math.min(rawReCount, 64);
        ArrayList<RelationEntry> relationEntries = new ArrayList<RelationEntry>(reCount);
        for (i3 = 0; i3 < reCount; ++i3) {
            relationEntries.add(new RelationEntry((String)ByteBufCodecs.STRING_UTF8.decode((Object)buf), (String)ByteBufCodecs.STRING_UTF8.decode((Object)buf), (String)ByteBufCodecs.STRING_UTF8.decode((Object)buf), (Integer)ByteBufCodecs.VAR_INT.decode((Object)buf), (String)ByteBufCodecs.STRING_UTF8.decode((Object)buf)));
        }
        for (i3 = reCount; i3 < rawReCount; ++i3) {
            ByteBufCodecs.STRING_UTF8.decode((Object)buf);
            ByteBufCodecs.STRING_UTF8.decode((Object)buf);
            ByteBufCodecs.STRING_UTF8.decode((Object)buf);
            ByteBufCodecs.VAR_INT.decode((Object)buf);
            ByteBufCodecs.STRING_UTF8.decode((Object)buf);
        }
        int diplomacyPoints = (Integer)ByteBufCodecs.VAR_INT.decode((Object)buf);
        int rawCropCount = (Integer)ByteBufCodecs.VAR_INT.decode((Object)buf);
        int cropCount = Math.min(rawCropCount, 32);
        ArrayList<LearningOffer> cropOffers = new ArrayList<LearningOffer>(cropCount);
        for (i2 = 0; i2 < cropCount; ++i2) {
            cropOffers.add(new LearningOffer((String)ByteBufCodecs.STRING_UTF8.decode((Object)buf), (String)ByteBufCodecs.STRING_UTF8.decode((Object)buf), (String)ByteBufCodecs.STRING_UTF8.decode((Object)buf)));
        }
        for (i2 = cropCount; i2 < rawCropCount; ++i2) {
            ByteBufCodecs.STRING_UTF8.decode((Object)buf);
            ByteBufCodecs.STRING_UTF8.decode((Object)buf);
            ByteBufCodecs.STRING_UTF8.decode((Object)buf);
        }
        int rawHuntCount = (Integer)ByteBufCodecs.VAR_INT.decode((Object)buf);
        int huntCount = Math.min(rawHuntCount, 32);
        ArrayList<LearningOffer> huntingOffers = new ArrayList<LearningOffer>(huntCount);
        for (i = 0; i < huntCount; ++i) {
            huntingOffers.add(new LearningOffer((String)ByteBufCodecs.STRING_UTF8.decode((Object)buf), (String)ByteBufCodecs.STRING_UTF8.decode((Object)buf), (String)ByteBufCodecs.STRING_UTF8.decode((Object)buf)));
        }
        for (i = huntCount; i < rawHuntCount; ++i) {
            ByteBufCodecs.STRING_UTF8.decode((Object)buf);
            ByteBufCodecs.STRING_UTF8.decode((Object)buf);
            ByteBufCodecs.STRING_UTF8.decode((Object)buf);
        }
        boolean cultureControlAvailable = (Boolean)ByteBufCodecs.BOOL.decode((Object)buf);
        boolean hasCultureControl = (Boolean)ByteBufCodecs.BOOL.decode((Object)buf);
        String cultureId = (String)ByteBufCodecs.STRING_UTF8.decode((Object)buf);
        boolean controlledByPlayer = (Boolean)ByteBufCodecs.BOOL.decode((Object)buf);
        String currentRaidTargetVillageId = (String)ByteBufCodecs.STRING_UTF8.decode((Object)buf);
        boolean raidInProgress = (Boolean)ByteBufCodecs.BOOL.decode((Object)buf);
        List<PanelLine> militaryLines = VillageChiefPayload.decodeMilitaryLines(buf);
        return new ChiefDynamic(villageName, cultureName, reputation, reputationLabel, totalBuildings, completeBuildings, underConstructionBuildings, totalVillagers, villageId, playerMoney, playerReputation, playerBuildings, relationEntries, diplomacyPoints, cropOffers, huntingOffers, cultureControlAvailable, hasCultureControl, cultureId, controlledByPlayer, currentRaidTargetVillageId, raidInProgress, militaryLines);
    }

    private static void encodeMilitaryLines(ByteBuf buf, List<PanelLine> lines) {
        ByteBufCodecs.VAR_INT.encode((Object)buf, (Object)lines.size());
        for (PanelLine line : lines) {
            ByteBufCodecs.STRING_UTF8.encode((Object)buf, (Object)line.text());
            ByteBufCodecs.BOOL.encode((Object)buf, (Object)line.isSeparator());
            ByteBufCodecs.STRING_UTF8.encode((Object)buf, (Object)(line.leftColumn() != null ? line.leftColumn() : ""));
            ByteBufCodecs.STRING_UTF8.encode((Object)buf, (Object)(line.rightColumn() != null ? line.rightColumn() : ""));
            ByteBufCodecs.STRING_UTF8.encode((Object)buf, (Object)(line.leftIcon() != null ? line.leftIcon() : ""));
            ByteBufCodecs.BOOL.encode((Object)buf, (Object)line.translatable());
            String[] args = line.translatableArgs();
            int argsCount = args != null ? args.length : 0;
            ByteBufCodecs.VAR_INT.encode((Object)buf, (Object)argsCount);
            if (args != null) {
                for (String arg : args) {
                    ByteBufCodecs.STRING_UTF8.encode((Object)buf, (Object)(arg != null ? arg : ""));
                }
            }
            ByteBufCodecs.VAR_INT.encode((Object)buf, (Object)line.color());
            ByteBufCodecs.BOOL.encode((Object)buf, (Object)line.bold());
            ByteBufCodecs.BYTE.encode((Object)buf, (Object)((byte)line.translatableArgMask()));
        }
    }

    private static List<PanelLine> decodeMilitaryLines(ByteBuf buf) {
        int rawCount = (Integer)ByteBufCodecs.VAR_INT.decode((Object)buf);
        int count = Math.min(rawCount, 512);
        ArrayList<PanelLine> result = new ArrayList<PanelLine>(count);
        for (int i = 0; i < rawCount; ++i) {
            int a;
            String text = (String)ByteBufCodecs.STRING_UTF8.decode((Object)buf);
            boolean sep = (Boolean)ByteBufCodecs.BOOL.decode((Object)buf);
            String leftCol = (String)ByteBufCodecs.STRING_UTF8.decode((Object)buf);
            String rightCol = (String)ByteBufCodecs.STRING_UTF8.decode((Object)buf);
            String icon = (String)ByteBufCodecs.STRING_UTF8.decode((Object)buf);
            boolean translatable = (Boolean)ByteBufCodecs.BOOL.decode((Object)buf);
            int rawArgs = (Integer)ByteBufCodecs.VAR_INT.decode((Object)buf);
            int argsCount = Math.min(rawArgs, 8);
            String[] args = argsCount > 0 ? new String[argsCount] : null;
            for (a = 0; a < argsCount; ++a) {
                args[a] = (String)ByteBufCodecs.STRING_UTF8.decode((Object)buf);
            }
            for (a = argsCount; a < rawArgs; ++a) {
                ByteBufCodecs.STRING_UTF8.decode((Object)buf);
            }
            int color = (Integer)ByteBufCodecs.VAR_INT.decode((Object)buf);
            boolean bold = (Boolean)ByteBufCodecs.BOOL.decode((Object)buf);
            int argMask = (Byte)ByteBufCodecs.BYTE.decode((Object)buf) & 0xFF;
            if (i >= count) continue;
            String leftColVal = leftCol.isEmpty() ? null : leftCol;
            String rightColVal = rightCol.isEmpty() ? null : rightCol;
            String iconVal = icon.isEmpty() ? null : icon;
            result.add(new PanelLine(text, sep, leftColVal, rightColVal, iconVal, translatable, null, args, color, bold, argMask));
        }
        return result;
    }

    public CustomPacketPayload.Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public record ChiefIdentity(int entityId, String chiefName, String roleName, String chiefTypeKey) {
    }

    public record ChiefDynamic(String villageName, String cultureName, int reputation, String reputationLabel, int totalBuildings, int completeBuildings, int underConstructionBuildings, int totalVillagers, String villageId, int playerMoney, int playerReputation, List<PlayerBuildingEntry> playerBuildings, List<RelationEntry> relationEntries, int diplomacyPoints, List<LearningOffer> cropOffers, List<LearningOffer> huntingOffers, boolean cultureControlAvailable, boolean hasCultureControl, String cultureId, boolean controlledByPlayer, String currentRaidTargetVillageId, boolean raidInProgress, List<PanelLine> militaryLines) {
    }

    public record PlayerBuildingEntry(String planSetId, String nativeName, int price, int reputation, String status, String translationKey) {
    }

    public record RelationEntry(String villageId, String villageName, String cultureName, int relation, String relationLabel) {
    }

    public record LearningOffer(String key, String itemName, String status) {
    }
}

