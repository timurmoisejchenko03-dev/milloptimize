/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  javax.annotation.Nullable
 */
package org.millenaire.village.panel;

import java.util.List;
import javax.annotation.Nullable;
import org.millenaire.village.panel.PanelLine;
import org.millenaire.village.panel.PanelType;

public record PanelContent(PanelType type, String title, List<PanelLine> lines, boolean titleTranslatable, @Nullable String[] titleArgs) {
    public PanelContent(PanelType type, String title, List<PanelLine> lines) {
        this(type, title, lines, false, null);
    }
}

