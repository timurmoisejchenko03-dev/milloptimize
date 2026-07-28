/*
 * Decompiled with CFR 0.152.
 */
package org.millenaire.encyclopedia;

import java.util.List;
import java.util.function.UnaryOperator;
import org.millenaire.encyclopedia.ExportColumn;
import org.millenaire.encyclopedia.ExportLine;
import org.millenaire.encyclopedia.LocalizedText;
import org.millenaire.encyclopedia.SlotText;
import org.millenaire.language.DisplayNameResolver;
import org.millenaire.village.TravelBookLine;
import org.millenaire.village.TravelBookScreenState;

public final class EncyclopediaLineMapper {
    private static final String DESC_NS_PREFIX = "travelbook.millenaire.";
    private static final String DESC_SUFFIX = ".desc";

    private EncyclopediaLineMapper() {
    }

    public static ExportLine map(TravelBookLine line, int lineIndex, UnaryOperator<String> resolve, SlotText slots) {
        LocalizedText text;
        if (line.isSeparator()) {
            return ExportLine.builder().style("separator").build();
        }
        ExportLine.Builder b = ExportLine.builder();
        if (line.isColumns()) {
            String leftId = EncyclopediaLineMapper.columnSlotId(lineIndex, 0);
            String rightId = EncyclopediaLineMapper.columnSlotId(lineIndex, 1);
            slots.put(leftId, EncyclopediaLineMapper.strip(line.leftColumn()));
            slots.put(rightId, EncyclopediaLineMapper.strip(line.rightColumn()));
            ExportColumn left = line.leftIcon() != null ? ExportColumn.withIcon(LocalizedText.id(leftId), line.leftIcon()) : ExportColumn.text(LocalizedText.id(leftId));
            ExportColumn right = ExportColumn.text(LocalizedText.id(rightId));
            b.columns(List.of(left, right));
            EncyclopediaLineMapper.applyNavTarget(b, line.navTarget(), LocalizedText.id(leftId));
            return b.build();
        }
        if (line.translatable() && line.nativePrefix() == null) {
            text = LocalizedText.key(line.text());
            if (EncyclopediaLineMapper.isDescKey(line.text())) {
                b.specialTag("MAIN_DESC");
            }
        } else {
            String slotId = EncyclopediaLineMapper.lineSlotId(lineIndex);
            String value = line.translatable() ? DisplayNameResolver.resolve(EncyclopediaLineMapper.strip((String)resolve.apply(line.text())), true, EncyclopediaLineMapper.strip(line.nativePrefix()), line.text()) : EncyclopediaLineMapper.strip(line.text());
            slots.put(slotId, value);
            text = LocalizedText.id(slotId);
        }
        b.text(text);
        EncyclopediaLineMapper.applyNavTarget(b, line.navTarget(), text);
        return b.build();
    }

    private static String lineSlotId(int lineIndex) {
        return "p0.l" + lineIndex;
    }

    private static String columnSlotId(int lineIndex, int colIndex) {
        return "p0.l" + lineIndex + ".c" + colIndex;
    }

    private static void applyNavTarget(ExportLine.Builder b, TravelBookLine.TravelBookNavTarget target, LocalizedText label) {
        if (target == null) {
            return;
        }
        String segment = EncyclopediaLineMapper.segmentFor(target.targetState());
        if (segment == null) {
            return;
        }
        b.referenceButtonCulture(target.cultureKey());
        b.referenceButtonType(segment);
        b.referenceButtonKey(EncyclopediaLineMapper.simpleKey(target.itemKey()));
        b.referenceButtonLabel(label);
    }

    private static String segmentFor(TravelBookScreenState state) {
        return switch (state) {
            case TravelBookScreenState.CULTURE -> "cultures";
            case TravelBookScreenState.VILLAGER_DETAIL -> "villagers";
            case TravelBookScreenState.VILLAGE_DETAIL -> "villages";
            case TravelBookScreenState.BUILDING_DETAIL -> "buildings";
            case TravelBookScreenState.TRADE_GOOD_DETAIL -> "tradegoods";
            default -> null;
        };
    }

    private static String simpleKey(String itemKey) {
        if (itemKey == null) {
            return null;
        }
        int slash = itemKey.lastIndexOf(47);
        return slash >= 0 ? itemKey.substring(slash + 1) : itemKey;
    }

    private static boolean isDescKey(String key) {
        return key != null && key.startsWith(DESC_NS_PREFIX) && key.endsWith(DESC_SUFFIX);
    }

    private static String strip(String text) {
        if (text == null) {
            return null;
        }
        return text.replaceAll("(?i)\u00a7[0-9A-FK-OR]", "");
    }
}

