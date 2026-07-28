/*
 * Decompiled with CFR 0.152.
 */
package org.millenaire.culture;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ThreadLocalRandom;

public record NameLists(Map<String, List<String>> lists) {
    public String randomFrom(String listKey) {
        List<String> names = this.lists.get(listKey);
        if (names == null || names.isEmpty()) {
            return null;
        }
        return names.get(ThreadLocalRandom.current().nextInt(names.size()));
    }

    public String randomFrom(String listKey, Set<String> namesTaken) {
        List<String> names = this.lists.get(listKey);
        if (names == null || names.isEmpty()) {
            return null;
        }
        if (namesTaken == null || namesTaken.isEmpty()) {
            return names.get(ThreadLocalRandom.current().nextInt(names.size()));
        }
        ArrayList<String> available = new ArrayList<String>(names);
        available.removeAll(namesTaken);
        if (available.isEmpty()) {
            return names.get(ThreadLocalRandom.current().nextInt(names.size()));
        }
        return (String)available.get(ThreadLocalRandom.current().nextInt(available.size()));
    }
}

