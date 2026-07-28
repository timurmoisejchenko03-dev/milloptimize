/*
 * Decompiled with CFR 0.152.
 */
package org.millenaire.encyclopedia;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import org.millenaire.encyclopedia.ExportColumn;
import org.millenaire.encyclopedia.ExportLine;
import org.millenaire.encyclopedia.LocalizedText;

public final class StructureInvariance {
    public static final String REFERENCE_LOCALE = "en_us";

    private StructureInvariance() {
    }

    public static List<ExportLine> validate(String itemRef, Map<String, List<ExportLine>> perLocaleSkeletons) {
        List<ExportLine> reference = perLocaleSkeletons.get(REFERENCE_LOCALE);
        if (reference == null) {
            throw new IllegalStateException("Structure invariance check for item '" + itemRef + "' is missing the reference locale 'en_us'");
        }
        List<LineSignature> referenceSignature = StructureInvariance.signature(reference);
        for (Map.Entry<String, List<ExportLine>> entry : perLocaleSkeletons.entrySet()) {
            List<LineSignature> candidate;
            String locale = entry.getKey();
            if (REFERENCE_LOCALE.equals(locale) || (candidate = StructureInvariance.signature(entry.getValue())).equals(referenceSignature)) continue;
            throw new IllegalStateException("Structure invariance violated for item '" + itemRef + "': locale '" + locale + "' skeleton diverges from reference locale 'en_us' (" + StructureInvariance.describeDivergence(referenceSignature, candidate) + ")");
        }
        return reference;
    }

    private static List<LineSignature> signature(List<ExportLine> lines) {
        return lines.stream().map(StructureInvariance::canonical).toList();
    }

    private static String describeDivergence(List<LineSignature> reference, List<LineSignature> candidate) {
        if (reference.size() != candidate.size()) {
            return "line count " + candidate.size() + " != reference " + reference.size();
        }
        for (int i = 0; i < reference.size(); ++i) {
            if (reference.get(i).equals(candidate.get(i))) continue;
            return "first divergence at line index " + i + ": reference=" + String.valueOf(reference.get(i)) + " candidate=" + String.valueOf(candidate.get(i));
        }
        return "structures differ";
    }

    private static LineSignature canonical(ExportLine line) {
        List<ColumnSignature> columns = line.columns() == null ? null : line.columns().stream().map(StructureInvariance::canonical).toList();
        return new LineSignature(line.style(), StructureInvariance.signatureOf(line.text()), line.iconKey(), line.iconLabel(), line.specialTag(), columns, line.referenceButtonCulture(), line.referenceButtonType(), line.referenceButtonKey(), StructureInvariance.signatureOf(line.referenceButtonLabel()), line.referenceButtonIconKey());
    }

    private static ColumnSignature canonical(ExportColumn column) {
        return new ColumnSignature(column.style(), StructureInvariance.signatureOf(column.text()), column.iconKey(), column.iconLabel(), column.referenceButtonCulture(), column.referenceButtonType(), column.referenceButtonKey(), StructureInvariance.signatureOf(column.referenceButtonLabel()), column.referenceButtonIconKey());
    }

    private static TextSignature signatureOf(LocalizedText text) {
        if (text == null) {
            return null;
        }
        if (text.key() != null) {
            return new TextSignature(Mode.KEY, text.key());
        }
        if (text.id() != null) {
            return new TextSignature(Mode.ID, null);
        }
        if (text.text() != null) {
            return new TextSignature(Mode.TEXT, text.text());
        }
        return new TextSignature(Mode.EMPTY, null);
    }

    private record LineSignature(String style, TextSignature text, String iconKey, String iconLabel, String specialTag, List<ColumnSignature> columns, String referenceButtonCulture, String referenceButtonType, String referenceButtonKey, TextSignature referenceButtonLabel, String referenceButtonIconKey) {
    }

    private record TextSignature(Mode mode, String value) {
        private TextSignature {
            Objects.requireNonNull(mode, "mode");
        }
    }

    private record ColumnSignature(String style, TextSignature text, String iconKey, String iconLabel, String referenceButtonCulture, String referenceButtonType, String referenceButtonKey, TextSignature referenceButtonLabel, String referenceButtonIconKey) {
    }

    private static enum Mode {
        KEY,
        ID,
        TEXT,
        EMPTY;

    }
}

