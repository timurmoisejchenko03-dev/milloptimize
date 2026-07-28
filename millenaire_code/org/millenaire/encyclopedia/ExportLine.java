/*
 * Decompiled with CFR 0.152.
 */
package org.millenaire.encyclopedia;

import java.util.List;
import org.millenaire.encyclopedia.ExportColumn;
import org.millenaire.encyclopedia.LocalizedText;

public record ExportLine(String style, LocalizedText text, String iconKey, String iconLabel, String referenceButtonCulture, String referenceButtonType, String referenceButtonKey, LocalizedText referenceButtonLabel, String referenceButtonIconKey, String specialTag, List<ExportColumn> columns) {
    public static final String STYLE_SEPARATOR = "separator";

    public static Builder builder() {
        return new Builder();
    }

    public static final class Builder {
        private String style;
        private LocalizedText text;
        private String iconKey;
        private String iconLabel;
        private String referenceButtonCulture;
        private String referenceButtonType;
        private String referenceButtonKey;
        private LocalizedText referenceButtonLabel;
        private String referenceButtonIconKey;
        private String specialTag;
        private List<ExportColumn> columns;

        public Builder style(String v) {
            this.style = v;
            return this;
        }

        public Builder text(LocalizedText v) {
            this.text = v;
            return this;
        }

        public Builder iconKey(String v) {
            this.iconKey = v;
            return this;
        }

        public Builder iconLabel(String v) {
            this.iconLabel = v;
            return this;
        }

        public Builder referenceButtonCulture(String v) {
            this.referenceButtonCulture = v;
            return this;
        }

        public Builder referenceButtonType(String v) {
            this.referenceButtonType = v;
            return this;
        }

        public Builder referenceButtonKey(String v) {
            this.referenceButtonKey = v;
            return this;
        }

        public Builder referenceButtonLabel(LocalizedText v) {
            this.referenceButtonLabel = v;
            return this;
        }

        public Builder referenceButtonIconKey(String v) {
            this.referenceButtonIconKey = v;
            return this;
        }

        public Builder specialTag(String v) {
            this.specialTag = v;
            return this;
        }

        public Builder columns(List<ExportColumn> v) {
            this.columns = v;
            return this;
        }

        public ExportLine build() {
            return new ExportLine(this.style, this.text, this.iconKey, this.iconLabel, this.referenceButtonCulture, this.referenceButtonType, this.referenceButtonKey, this.referenceButtonLabel, this.referenceButtonIconKey, this.specialTag, this.columns);
        }
    }
}

