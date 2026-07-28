/*
 * Decompiled with CFR 0.152.
 */
package org.millenaire.content;

import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;
import org.millenaire.content.Resource;

public interface ContentFs {
    public Optional<Resource> findFirst(String var1);

    public List<Resource> findAll(String var1);

    public Stream<Resource> walk(String var1, int var2);

    public ContentFs sub(String var1);
}

