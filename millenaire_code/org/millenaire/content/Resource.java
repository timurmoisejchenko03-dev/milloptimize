/*
 * Decompiled with CFR 0.152.
 */
package org.millenaire.content;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.OpenOption;
import java.nio.file.Path;
import java.util.Optional;
import org.millenaire.content.ContentFs;
import org.millenaire.content.SourceKind;
import org.millenaire.content.SourceLabel;

public sealed interface Resource {
    public String relPath();

    public SourceLabel source();

    public SourceKind kind();

    public InputStream open() throws IOException;

    public long size() throws IOException;

    public Optional<Resource> sibling(String var1);

    public static String siblingPath(String relPath, String name) {
        int slash = relPath.lastIndexOf(47);
        if (slash < 0) {
            return name;
        }
        return relPath.substring(0, slash + 1) + name;
    }

    public record FileResource(String relPath, SourceLabel source, SourceKind kind, Path path, ContentFs owner) implements Resource
    {
        public FileResource {
            if (relPath == null) {
                throw new IllegalArgumentException("relPath null");
            }
            if (source == null) {
                throw new IllegalArgumentException("source null");
            }
            if (kind == null) {
                throw new IllegalArgumentException("kind null");
            }
            if (path == null) {
                throw new IllegalArgumentException("path null");
            }
            if (owner == null) {
                throw new IllegalArgumentException("owner null");
            }
        }

        @Override
        public InputStream open() throws IOException {
            return Files.newInputStream(this.path, new OpenOption[0]);
        }

        @Override
        public long size() throws IOException {
            return Files.size(this.path);
        }

        @Override
        public Optional<Resource> sibling(String name) {
            return this.owner.findFirst(Resource.siblingPath(this.relPath, name));
        }
    }

    public record ClasspathResource(String relPath, SourceLabel source, SourceKind kind, Path path, ContentFs owner) implements Resource
    {
        public ClasspathResource {
            if (relPath == null) {
                throw new IllegalArgumentException("relPath null");
            }
            if (source == null) {
                throw new IllegalArgumentException("source null");
            }
            if (kind == null) {
                throw new IllegalArgumentException("kind null");
            }
            if (path == null) {
                throw new IllegalArgumentException("path null");
            }
            if (owner == null) {
                throw new IllegalArgumentException("owner null");
            }
        }

        @Override
        public InputStream open() throws IOException {
            return Files.newInputStream(this.path, new OpenOption[0]);
        }

        @Override
        public long size() throws IOException {
            return Files.size(this.path);
        }

        @Override
        public Optional<Resource> sibling(String name) {
            return this.owner.findFirst(Resource.siblingPath(this.relPath, name));
        }
    }
}

