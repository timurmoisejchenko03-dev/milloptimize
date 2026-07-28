/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.mojang.logging.LogUtils
 *  org.slf4j.Logger
 */
package org.millenaire.content;

import com.mojang.logging.LogUtils;
import java.io.IOException;
import java.lang.invoke.LambdaMetafactory;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.function.Function;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.millenaire.content.BuiltInCultures;
import org.millenaire.content.ClasspathLayer;
import org.millenaire.content.ContentDirectoryManager;
import org.millenaire.content.ContentFs;
import org.millenaire.content.CultureIdPolicy;
import org.millenaire.content.LayerEntry;
import org.millenaire.content.OverlayBuilder;
import org.millenaire.content.StandardLayer;
import org.millenaire.content.SubmodLayer;
import org.millenaire.content.SubmodRoot;
import org.slf4j.Logger;

public final class CustomContentIndex {
    private static final Logger LOGGER = LogUtils.getLogger();
    private static final List<String> CONTENT_TYPE_DIRS = List.of("cultures", "languages", "gathering_type", "quests", "wall_type");
    private static final Set<String> LEGACY_CONTENT_DIRS = Set.copyOf(CONTENT_TYPE_DIRS);
    private static final Set<String> SILENT_RESERVED_DIRS = Set.of("exports", "exported");
    private static final String EXPORTED_DIR = "exported";
    private static final String CONVERTED_SUFFIX = "_converted";
    private static final Pattern SUBMOD_NAME_PATTERN = Pattern.compile("[A-Za-z0-9_.-]+");
    private static volatile CustomContentIndex current;
    private static boolean warnedLazy;
    private static final Set<String> warnedLegacyNames;
    private static final Set<String> warnedInvalidSubmodNames;
    private static final Set<String> warnedExportsHasCultures;
    private static final Set<String> warnedDroppedAtCap;
    private static final Set<String> warnedPackIdCollision;
    private final List<SubmodRoot> roots;
    private final Map<String, List<SubmodRoot>> rootsPerCulture;
    private final Map<String, SubmodRoot> customCultureOwners;
    private final Map<String, List<SubmodRoot>> rootsByContentType;
    private final ContentFs contentFs;
    private final ContentFs exportedContentFs;

    private CustomContentIndex(List<SubmodRoot> roots, Map<String, List<SubmodRoot>> rootsPerCulture, Map<String, SubmodRoot> customCultureOwners, Map<String, List<SubmodRoot>> rootsByContentType, ContentFs contentFs, ContentFs exportedContentFs) {
        this.roots = List.copyOf(roots);
        this.rootsPerCulture = Collections.unmodifiableMap(rootsPerCulture);
        this.customCultureOwners = Collections.unmodifiableMap(customCultureOwners);
        Map<String, List> frozen = rootsByContentType.entrySet().stream().collect(Collectors.toUnmodifiableMap(Map.Entry::getKey, e -> List.copyOf((Collection)e.getValue())));
        this.rootsByContentType = frozen;
        this.contentFs = contentFs != null ? contentFs : new OverlayBuilder().build();
        this.exportedContentFs = exportedContentFs != null ? exportedContentFs : new OverlayBuilder().build();
    }

    public static CustomContentIndex empty() {
        return new CustomContentIndex(List.of(), Map.of(), Map.of(), Map.of(), new OverlayBuilder().build(), new OverlayBuilder().build());
    }

    public static CustomContentIndex jarOnly(Path classpathRoot) {
        OverlayBuilder overlayBuilder = new OverlayBuilder();
        if (classpathRoot != null) {
            overlayBuilder.addLayer(ClasspathLayer.scan(classpathRoot));
        }
        ContentFs contentFs = overlayBuilder.build();
        return new CustomContentIndex(List.of(), Map.of(), Map.of(), Map.of(), contentFs, new OverlayBuilder().build());
    }

    public static CustomContentIndex jarOnly() {
        return CustomContentIndex.jarOnly(ClasspathLayer.resolveDefaultRoot());
    }

    public static CustomContentIndex rebuild(Collection<String> builtInCultures) {
        return CustomContentIndex.rebuild(builtInCultures, false);
    }

    public static CustomContentIndex rebuild(Collection<String> builtInCultures, boolean silent) {
        CustomContentIndex index;
        current = index = CustomContentIndex.build(builtInCultures, silent);
        return index;
    }

    /*
     * WARNING - Removed try catching itself - possible behaviour change.
     */
    public static CustomContentIndex current() {
        CustomContentIndex snapshot = current;
        if (snapshot != null) {
            return snapshot;
        }
        Class<CustomContentIndex> clazz = CustomContentIndex.class;
        synchronized (CustomContentIndex.class) {
            snapshot = current;
            if (snapshot != null) {
                // ** MonitorExit[var1_1] (shouldn't be in output)
                return snapshot;
            }
            if (!warnedLazy) {
                warnedLazy = true;
                LOGGER.warn("CustomContentIndex.current() called before rebuild(); performing lazy rebuild with BuiltInCultures.IDS. If this fires on the server side, it's a lifecycle bug.");
            }
            if (!ContentDirectoryManager.isInitialized()) {
                // ** MonitorExit[var1_1] (shouldn't be in output)
                return CustomContentIndex.empty();
            }
            // ** MonitorExit[var1_1] (shouldn't be in output)
            return CustomContentIndex.rebuild(BuiltInCultures.IDS);
        }
    }

    /*
     * WARNING - Removed try catching itself - possible behaviour change.
     */
    public static void resetForTesting() {
        Class<CustomContentIndex> clazz = CustomContentIndex.class;
        synchronized (CustomContentIndex.class) {
            current = null;
            warnedLazy = false;
            warnedLegacyNames.clear();
            warnedInvalidSubmodNames.clear();
            warnedExportsHasCultures.clear();
            warnedDroppedAtCap.clear();
            warnedPackIdCollision.clear();
            // ** MonitorExit[var0] (shouldn't be in output)
            return;
        }
    }

    public List<SubmodRoot> roots() {
        return this.roots;
    }

    public List<SubmodRoot> rootsForCulture(String culture) {
        return this.rootsPerCulture.getOrDefault(culture, List.of());
    }

    public List<SubmodRoot> rootsWithLanguages() {
        return this.rootsByContentType.getOrDefault("languages", List.of());
    }

    public List<SubmodRoot> rootsWithGatheringType() {
        return this.rootsByContentType.getOrDefault("gathering_type", List.of());
    }

    public List<SubmodRoot> rootsWithQuests() {
        return this.rootsByContentType.getOrDefault("quests", List.of());
    }

    public Set<String> customCultureIds() {
        return this.customCultureOwners.keySet();
    }

    public Set<String> indexedCultures() {
        return this.rootsPerCulture.keySet();
    }

    public ContentFs root() {
        return this.contentFs;
    }

    public ContentFs exportedFs() {
        return this.exportedContentFs;
    }

    public ContentFs forCulture(String culture) {
        if (culture == null) {
            throw new IllegalArgumentException("culture is null");
        }
        return this.contentFs.sub("cultures/" + culture);
    }

    public ContentFs forGlobalContent(String contentType) {
        if (contentType == null) {
            throw new IllegalArgumentException("contentType is null");
        }
        return this.contentFs.sub(contentType);
    }

    private static CustomContentIndex build(Collection<String> builtInCultures) {
        return CustomContentIndex.build(builtInCultures, false);
    }

    /*
     * WARNING - Removed try catching itself - possible behaviour change.
     * Enabled aggressive block sorting
     * Enabled unnecessary exception pruning
     * Enabled aggressive exception aggregation
     * Converted monitor instructions to comments
     * Lifted jumps to return sites
     */
    private static CustomContentIndex build(Collection<String> builtInCultures, boolean silent) {
        if (!ContentDirectoryManager.isInitialized()) {
            OverlayBuilder ob = new OverlayBuilder(silent);
            Path jarRoot = ClasspathLayer.resolveDefaultRoot();
            if (jarRoot == null) return new CustomContentIndex(List.of(), Map.of(), Map.of(), Map.of(), ob.build(), new OverlayBuilder().build());
            ob.addLayer(ClasspathLayer.scan(jarRoot));
            return new CustomContentIndex(List.of(), Map.of(), Map.of(), Map.of(), ob.build(), new OverlayBuilder().build());
        }
        Path customDir = ContentDirectoryManager.getCustomDir();
        Set<String> builtInLower = CustomContentIndex.lowercaseSet(builtInCultures);
        ArrayList<SubmodRoot> roots = new ArrayList<SubmodRoot>();
        TreeMap<String, List<SubmodRoot>> rootsPerCulture = new TreeMap<String, List<SubmodRoot>>();
        TreeMap<String, SubmodRoot> customCultureOwners = new TreeMap<String, SubmodRoot>();
        HashMap<String, List<SubmodRoot>> rootsByContentType = new HashMap<String, List<SubmodRoot>>();
        Set<String> converted = CustomContentIndex.collectConvertedNames(customDir);
        List<String> candidateNames = CustomContentIndex.listDirectChildDirs(customDir);
        for (String name : candidateNames) {
            if (LEGACY_CONTENT_DIRS.contains(name)) {
                CustomContentIndex.warnLegacyLayoutOnce(customDir, name);
                continue;
            }
            if (SILENT_RESERVED_DIRS.contains(name)) {
                CustomContentIndex.warnIfMisplacedExportLayoutOnce(customDir, name);
                continue;
            }
            if (!SUBMOD_NAME_PATTERN.matcher(name).matches()) {
                CustomContentIndex.warnInvalidSubmodNameOnce(name);
                continue;
            }
            if (!name.endsWith(CONVERTED_SUFFIX) && converted.contains(name + CONVERTED_SUFFIX)) {
                LOGGER.info("Ignoring legacy source sub-mod '{}' \u2014 converted sibling '{}' present", (Object)name, (Object)(name + CONVERTED_SUFFIX));
                continue;
            }
            Path submodPath = customDir.resolve(name);
            if (!CustomContentIndex.qualifiesAsSubmod(submodPath)) continue;
            CustomContentIndex.registerRoot(new SubmodRoot(name, submodPath), builtInLower, roots, rootsPerCulture, customCultureOwners, rootsByContentType);
        }
        if (customCultureOwners.size() > 50) {
            int over = customCultureOwners.size() - 50;
            ArrayList ordered = new ArrayList(customCultureOwners.keySet());
            List droppedNames = ordered.subList(50, ordered.size());
            Class<CustomContentIndex> clazz = CustomContentIndex.class;
            // MONITORENTER : org.millenaire.content.CustomContentIndex.class
            boolean firstHeader = warnedDroppedAtCap.add("__header__");
            // MONITOREXIT : clazz
            if (firstHeader) {
                LOGGER.warn("Discovered {} custom cultures; capping at MAX_CUSTOM_CULTURES={} and dropping {} (alpha-last) cultures", new Object[]{customCultureOwners.size(), 50, over});
            }
            for (String dropped : droppedNames) {
                customCultureOwners.remove(dropped);
                rootsPerCulture.remove(dropped);
                Class<CustomContentIndex> clazz2 = CustomContentIndex.class;
                // MONITORENTER : org.millenaire.content.CustomContentIndex.class
                boolean firstForName = warnedDroppedAtCap.add(dropped);
                // MONITOREXIT : clazz2
                if (!firstForName) continue;
                LOGGER.warn("  dropped custom culture beyond cap: '{}'", (Object)dropped);
            }
        }
        Map<SubmodRoot, Set<String>> rejectedByRoot = CustomContentIndex.rejectPackIdCollisions(rootsPerCulture, customCultureOwners);
        OverlayBuilder overlayBuilder = new OverlayBuilder(silent);
        Path classpathRoot = ClasspathLayer.resolveDefaultRoot();
        if (classpathRoot != null) {
            overlayBuilder.addLayer(ClasspathLayer.scan(classpathRoot));
        }
        overlayBuilder.addLayer(StandardLayer.scan(ContentDirectoryManager.getStandardDir()));
        Iterator firstHeader = roots.iterator();
        while (true) {
            if (!firstHeader.hasNext()) {
                ContentFs contentFs = overlayBuilder.build();
                ContentFs exportedFs = CustomContentIndex.buildExportedFs(customDir, silent);
                return new CustomContentIndex(roots, rootsPerCulture, customCultureOwners, rootsByContentType, contentFs, exportedFs);
            }
            SubmodRoot root = (SubmodRoot)firstHeader.next();
            List<LayerEntry> entries = SubmodLayer.scanIncludingOversize(root.root(), SubmodLayer.labelFor(root.name()));
            Set<String> rejectedCultures = rejectedByRoot.get(root);
            if (rejectedCultures != null && !rejectedCultures.isEmpty()) {
                entries = CustomContentIndex.filterRejectedCulturePaths(entries, rejectedCultures);
            }
            overlayBuilder.addSubmodLayer(entries);
        }
    }

    private static ContentFs buildExportedFs(Path customDir, boolean silent) {
        OverlayBuilder builder = new OverlayBuilder(silent);
        Path exportedDir = customDir.resolve(EXPORTED_DIR);
        if (Files.isDirectory(exportedDir, new LinkOption[0])) {
            builder.addSubmodLayer(SubmodLayer.scanIncludingOversize(exportedDir, SubmodLayer.labelFor(EXPORTED_DIR)));
        }
        return builder.build();
    }

    /*
     * WARNING - Removed try catching itself - possible behaviour change.
     */
    private static void warnLegacyLayoutOnce(Path customDir, String name) {
        Class<CustomContentIndex> clazz = CustomContentIndex.class;
        synchronized (CustomContentIndex.class) {
            if (!warnedLegacyNames.add(name)) {
                // ** MonitorExit[var2_2] (shouldn't be in output)
                return;
            }
            // ** MonitorExit[var2_2] (shouldn't be in output)
            LOGGER.warn("Ignoring legacy flat-root directory '{}/{}/' \u2014 wrap your content in a sub-mod directory (e.g. millenaire-custom/my-pack/{}/...).", new Object[]{customDir.getFileName(), name, name});
            return;
        }
    }

    /*
     * WARNING - Removed try catching itself - possible behaviour change.
     */
    private static void warnInvalidSubmodNameOnce(String name) {
        Class<CustomContentIndex> clazz = CustomContentIndex.class;
        synchronized (CustomContentIndex.class) {
            if (!warnedInvalidSubmodNames.add(name)) {
                // ** MonitorExit[var1_1] (shouldn't be in output)
                return;
            }
            // ** MonitorExit[var1_1] (shouldn't be in output)
            LOGGER.warn("Ignoring sub-mod directory '{}' \u2014 name must match {} so it can be combined with culture ids without ambiguity.", (Object)name, (Object)SUBMOD_NAME_PATTERN.pattern());
            return;
        }
    }

    /*
     * WARNING - Removed try catching itself - possible behaviour change.
     */
    private static void warnIfMisplacedExportLayoutOnce(Path customDir, String name) {
        if (!"exports".equals(name)) {
            return;
        }
        Path culturesDir = customDir.resolve(name).resolve("cultures");
        if (!Files.isDirectory(culturesDir, new LinkOption[0])) {
            return;
        }
        Class<CustomContentIndex> clazz = CustomContentIndex.class;
        synchronized (CustomContentIndex.class) {
            if (!warnedExportsHasCultures.add(name)) {
                // ** MonitorExit[var3_3] (shouldn't be in output)
                return;
            }
            // ** MonitorExit[var3_3] (shouldn't be in output)
            LOGGER.warn("Found '{}/{}/cultures/' \u2014 '{}/' is the legacy flat Import Table sink ('{}/<id>.json'). Files under '{}/cultures/' are NOT overlaid into the runtime; move them into a regular sub-mod (e.g. 'millenaire-custom/my-pack/cultures/<c>/...') to become active.", new Object[]{customDir.getFileName(), name, name, name, name});
            return;
        }
    }

    private static boolean qualifiesAsSubmod(Path root) {
        for (String type : CONTENT_TYPE_DIRS) {
            if (!Files.isDirectory(root.resolve(type), new LinkOption[0])) continue;
            return true;
        }
        return false;
    }

    private static void registerRoot(SubmodRoot root, Set<String> builtInLower, List<SubmodRoot> roots, Map<String, List<SubmodRoot>> rootsPerCulture, Map<String, SubmodRoot> customCultureOwners, Map<String, List<SubmodRoot>> rootsByContentType) {
        roots.add(root);
        Path culturesDir = root.root().resolve("cultures");
        if (Files.isDirectory(culturesDir, new LinkOption[0])) {
            for (String culture : CustomContentIndex.listDirectChildDirs(culturesDir)) {
                SubmodRoot existing;
                if (!CultureIdPolicy.PATTERN.matcher(culture).matches()) {
                    LOGGER.warn("Ignoring culture folder with invalid id '{}' in sub-mod '{}'", (Object)culture, (Object)root.displayName());
                    continue;
                }
                rootsPerCulture.computeIfAbsent(culture, k -> new ArrayList()).add(root);
                if (builtInLower.contains(culture.toLowerCase(Locale.ROOT)) || (existing = customCultureOwners.putIfAbsent(culture, root)) == null) continue;
                LOGGER.info("Custom culture '{}' declared by multiple sub-mods \u2014 primary owner '{}', additional contributor '{}'", new Object[]{culture, existing.displayName(), root.displayName()});
            }
        }
        for (String contentType : CONTENT_TYPE_DIRS) {
            if ("cultures".equals(contentType) || !Files.isDirectory(root.root().resolve(contentType), new LinkOption[0])) continue;
            rootsByContentType.computeIfAbsent(contentType, k -> new ArrayList()).add(root);
        }
    }

    private static String buildPackId(String submodName, String culture) {
        return "millenaire_custom_" + submodName + "_" + culture;
    }

    /*
     * WARNING - Removed try catching itself - possible behaviour change.
     * Unable to fully structure code
     * Enabled aggressive block sorting
     * Enabled unnecessary exception pruning
     * Enabled aggressive exception aggregation
     * Converted monitor instructions to comments
     * Lifted jumps to return sites
     */
    private static Map<SubmodRoot, Set<String>> rejectPackIdCollisions(Map<String, List<SubmodRoot>> rootsPerCulture, Map<String, SubmodRoot> customCultureOwners) {
        byPackId = new TreeMap<String, List>();
        for (Map.Entry<String, List<SubmodRoot>> entry : rootsPerCulture.entrySet()) {
            culture = entry.getKey();
            for (SubmodRoot submod : entry.getValue()) {
                packId = CustomContentIndex.buildPackId(submod.name(), culture);
                byPackId.computeIfAbsent(packId, (Function<String, List>)LambdaMetafactory.metafactory(null, null, null, (Ljava/lang/Object;)Ljava/lang/Object;, lambda$rejectPackIdCollisions$3(java.lang.String ), (Ljava/lang/String;)Ljava/util/List;)()).add(new PackIdEntry(submod, culture));
            }
        }
        rejectedByRoot = new HashMap<SubmodRoot, Set<String>>();
        var4_4 = byPackId.entrySet().iterator();
        block4: while (true) {
            if (var4_4.hasNext() == false) return rejectedByRoot;
            e = var4_4.next();
            participants = (List)e.getValue();
            if (participants.size() <= 1) continue;
            packId = (String)e.getKey();
            var9_10 = CustomContentIndex.class;
            // MONITORENTER : org.millenaire.content.CustomContentIndex.class
            firstWarn = CustomContentIndex.warnedPackIdCollision.add(packId);
            // MONITOREXIT : var9_10
            if (firstWarn) {
                rendered = new StringBuilder();
                for (PackIdEntry p : participants) {
                    if (rendered.length() > 0) {
                        rendered.append(", ");
                    }
                    rendered.append("(submod='").append(p.submod().name()).append("', culture='").append(p.culture()).append("')");
                }
                CustomContentIndex.LOGGER.warn("Pack-id collision '{}' \u2014 rejecting {} participants: {}. Rename a sub-mod or culture to disambiguate.", new Object[]{packId, participants.size(), rendered});
            }
            var9_10 = participants.iterator();
            while (true) {
                if (var9_10.hasNext()) ** break;
                continue block4;
                p = (PackIdEntry)var9_10.next();
                rejectedByRoot.computeIfAbsent(p.submod(), (Function<SubmodRoot, Set>)LambdaMetafactory.metafactory(null, null, null, (Ljava/lang/Object;)Ljava/lang/Object;, lambda$rejectPackIdCollisions$4(org.millenaire.content.SubmodRoot ), (Lorg/millenaire/content/SubmodRoot;)Ljava/util/Set;)()).add(p.culture());
                contributors = rootsPerCulture.get(p.culture());
                if (contributors != null) {
                    contributors.remove(p.submod());
                    if (contributors.isEmpty()) {
                        rootsPerCulture.remove(p.culture());
                    }
                }
                if ((owner = customCultureOwners.get(p.culture())) == null || !owner.equals(p.submod())) continue;
                remaining = rootsPerCulture.get(p.culture());
                if (remaining != null && !remaining.isEmpty()) {
                    customCultureOwners.put(p.culture(), remaining.get(0));
                    continue;
                }
                customCultureOwners.remove(p.culture());
            }
            break;
        }
    }

    private static List<LayerEntry> filterRejectedCulturePaths(List<LayerEntry> entries, Set<String> rejectedCultures) {
        ArrayList<String> prefixes = new ArrayList<String>(rejectedCultures.size());
        for (String c : rejectedCultures) {
            prefixes.add(("cultures/" + c + "/").toLowerCase(Locale.ROOT));
        }
        ArrayList<LayerEntry> kept = new ArrayList<LayerEntry>(entries.size());
        for (LayerEntry e : entries) {
            String rel = e.relPath();
            boolean drop = false;
            for (String prefix : prefixes) {
                if (!rel.startsWith(prefix)) continue;
                drop = true;
                break;
            }
            if (drop) continue;
            kept.add(e);
        }
        return kept;
    }

    private static Set<String> collectConvertedNames(Path customDir) {
        HashSet<String> out = new HashSet<String>();
        try (Stream<Path> stream = Files.list(customDir);){
            stream.filter(x$0 -> Files.isDirectory(x$0, new LinkOption[0])).forEach(p -> {
                String name = p.getFileName().toString();
                if (name.endsWith(CONVERTED_SUFFIX)) {
                    out.add(name);
                }
            });
        }
        catch (IOException e) {
            LOGGER.error("Failed to scan custom dir for converted sub-mods: {}", (Object)e.getMessage(), (Object)e);
        }
        return out;
    }

    private static List<String> listDirectChildDirs(Path dir) {
        ArrayList<String> names = new ArrayList<String>();
        try (Stream<Path> stream = Files.list(dir);){
            stream.filter(x$0 -> Files.isDirectory(x$0, new LinkOption[0])).map(p -> p.getFileName().toString()).forEach(names::add);
        }
        catch (IOException e) {
            LOGGER.error("Failed to list direct children of {}: {}", new Object[]{dir, e.getMessage(), e});
            return List.of();
        }
        names.sort(String::compareTo);
        return names;
    }

    private static Set<String> lowercaseSet(Collection<String> values) {
        TreeSet<String> out = new TreeSet<String>();
        if (values == null) {
            return out;
        }
        for (String v : values) {
            if (v == null) continue;
            out.add(v.toLowerCase(Locale.ROOT));
        }
        return out;
    }

    private static /* synthetic */ Set lambda$rejectPackIdCollisions$4(SubmodRoot k) {
        return new HashSet();
    }

    private static /* synthetic */ List lambda$rejectPackIdCollisions$3(String k) {
        return new ArrayList();
    }

    static {
        warnedLegacyNames = new HashSet<String>();
        warnedInvalidSubmodNames = new HashSet<String>();
        warnedExportsHasCultures = new HashSet<String>();
        warnedDroppedAtCap = new HashSet<String>();
        warnedPackIdCollision = new HashSet<String>();
    }

    private record PackIdEntry(SubmodRoot submod, String culture) {
    }
}

