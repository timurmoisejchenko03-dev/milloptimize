/*
 * Decompiled with CFR 0.150.
 */
package org.millenaire.encyclopedia;

import java.util.LinkedHashMap;
import java.util.Map;

public final class SlotText {
    private final Map<String, String> values = new LinkedHashMap<String, String>();

    public void put(String slotId, String value) {
        this.values.put(slotId, value);
    }

    public Map<String, String> values() {
        return this.values;
    }
}

