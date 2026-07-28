/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.google.gson.Gson
 *  com.google.gson.GsonBuilder
 *  com.google.gson.JsonElement
 *  com.google.gson.JsonObject
 *  com.google.gson.JsonParser
 *  com.google.gson.reflect.TypeToken
 *  com.mojang.logging.LogUtils
 *  net.neoforged.fml.ModContainer
 *  net.neoforged.fml.ModList
 *  net.neoforged.fml.loading.FMLEnvironment
 *  net.neoforged.neoforgespi.language.IModInfo
 *  org.apache.maven.artifact.versioning.ComparableVersion
 *  org.slf4j.Logger
 */
package org.millenaire.content;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.reflect.TypeToken;
import com.mojang.logging.LogUtils;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Type;
import java.net.URI;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.file.FileSystem;
import java.nio.file.FileSystems;
import java.nio.file.FileVisitOption;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.NoSuchFileException;
import java.nio.file.OpenOption;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.attribute.FileAttribute;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.ModList;
import net.neoforged.fml.loading.FMLEnvironment;
import net.neoforged.neoforgespi.language.IModInfo;
import org.apache.maven.artifact.versioning.ComparableVersion;
import org.millenaire.content.ContentDirectoryManager;
import org.slf4j.Logger;

public final class NativeContentDeployer {
    private static final Logger LOGGER = LogUtils.getLogger();
    private static final String VERSION_FILE = "_deployed_version.txt";
    private static final String WARNING_FILE = "WARNING - changes here will be overwritten on update.txt";
    private static final String WARNING_BODY = "The millenaire/ directory is auto-deployed by Mill\u00e9naire on each new\nversion of the mod, erasing all changes made to it.\n\nIf you want to customise content, copy the relevant files to the\ncorresponding location in millenaire-custom/, which is never\ntouched by updates.\n\nMost files in millenaire-custom/ replace their equivalent in\nmillenaire/. The following are additive instead:\n  - Language files (<culture>_sentences.txt, <culture>_dialogues.txt)\n  - Namelists (*.txt under cultures/<culture>/namelists/)\n  - Traded goods (traded_goods.json per culture)\n\nSee docs/feat/custom-content.md for the full authoring workflow.\n";
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

    private NativeContentDeployer() {
    }

    public static synchronized void deployIfNeeded() {
        if (!ContentDirectoryManager.isInitialized()) {
            LOGGER.warn("Skipping deployment: ContentDirectoryManager is not initialised");
            return;
        }
        try {
            boolean devForce;
            Path standardDir = ContentDirectoryManager.getStandardDir();
            String currentVersion = NativeContentDeployer.currentModVersion();
            String deployedVersion = NativeContentDeployer.readDeployedVersion(standardDir);
            boolean bl = devForce = !FMLEnvironment.production;
            if (!devForce && !NativeContentDeployer.shouldDeploy(currentVersion, deployedVersion)) {
                LOGGER.info("Standard content already up-to-date ({}); skipping deployment", (Object)deployedVersion);
                return;
            }
            if (devForce) {
                LOGGER.info("Dev environment detected \u2014 forcing redeploy of standard content to {} (mod {}, previous {})", new Object[]{standardDir, currentVersion, deployedVersion == null ? "never" : deployedVersion});
            } else {
                LOGGER.info("Redeploying Mill\u00e9naire standard content to {} (mod {}, previous {})", new Object[]{standardDir, currentVersion, deployedVersion == null ? "never" : deployedVersion});
            }
            long startMs = System.currentTimeMillis();
            NativeContentDeployer.wipeStandardDir(standardDir);
            int fileCount = 0;
            fileCount += NativeContentDeployer.deployClasspathDirRecursive(standardDir, "/millenaire/cultures", standardDir.resolve("cultures"));
            fileCount += NativeContentDeployer.deployClasspathDir(standardDir, "/millenaire/gathering_type", standardDir.resolve("gathering_type"), null);
            fileCount += NativeContentDeployer.deployClasspathDir(standardDir, "/millenaire/visit_goal", standardDir.resolve("visit_goal"), null);
            fileCount += NativeContentDeployer.deployClasspathDirRecursive(standardDir, "/millenaire/wall_type", standardDir.resolve("wall_type"));
            fileCount += NativeContentDeployer.deployQuests(standardDir);
            fileCount += NativeContentDeployer.deployLanguages(standardDir);
            fileCount += NativeContentDeployer.deployClasspathDir(standardDir, "/millenaire/templates", standardDir.resolve("_templates"), null);
            fileCount += NativeContentDeployer.deployClasspathDir(standardDir, "/millenaire/reference", standardDir.resolve("_reference"), null);
            fileCount += NativeContentDeployer.deployClasspathDirRecursive(standardDir, "/millenaire/docs", standardDir.resolve("_docs"));
            NativeContentDeployer.writeDeployedVersion(standardDir, currentVersion);
            NativeContentDeployer.writeWarningFile(standardDir);
            long elapsedMs = System.currentTimeMillis() - startMs;
            LOGGER.info("Deployment complete: {} files written in {} ms", (Object)(fileCount += NativeContentDeployer.deployWelcomeIfAbsent(ContentDirectoryManager.getCustomDir())), (Object)elapsedMs);
        }
        catch (Exception e) {
            LOGGER.error("Content deployment failed; continuing without standard content on disk: {}", (Object)e.getMessage(), (Object)e);
        }
    }

    static boolean shouldDeploy(String currentVersion, String deployedVersion) {
        if (deployedVersion == null) {
            return true;
        }
        try {
            return new ComparableVersion(deployedVersion).compareTo(new ComparableVersion(currentVersion)) != 0;
        }
        catch (Exception e) {
            LOGGER.warn("Could not compare versions '{}' vs '{}', forcing redeploy: {}", new Object[]{deployedVersion, currentVersion, e.getMessage()});
            return true;
        }
    }

    private static String currentModVersion() {
        try {
            IModInfo info = ((ModContainer)ModList.get().getModContainerById("millenaire").orElseThrow(() -> new IllegalStateException("Millenaire mod container not found"))).getModInfo();
            return info.getVersion().toString();
        }
        catch (Exception e) {
            LOGGER.warn("Could not resolve current mod version: {}", (Object)e.getMessage());
            return "0.0.0";
        }
    }

    private static String readDeployedVersion(Path standardDir) {
        Path versionPath = standardDir.resolve(VERSION_FILE);
        if (!Files.isRegularFile(versionPath, new LinkOption[0])) {
            return null;
        }
        try {
            return Files.readString(versionPath, StandardCharsets.UTF_8).trim();
        }
        catch (IOException e) {
            LOGGER.warn("Could not read {}: {}", (Object)versionPath, (Object)e.getMessage());
            return null;
        }
    }

    private static void writeDeployedVersion(Path standardDir, String version) throws IOException {
        Files.writeString(standardDir.resolve(VERSION_FILE), (CharSequence)(version + "\n"), StandardCharsets.UTF_8, new OpenOption[0]);
    }

    private static void writeWarningFile(Path standardDir) throws IOException {
        Files.writeString(standardDir.resolve(WARNING_FILE), (CharSequence)WARNING_BODY, StandardCharsets.UTF_8, new OpenOption[0]);
    }

    static void wipeStandardDir(Path standardDir) throws IOException {
        Path liveReal;
        if (!Files.isDirectory(standardDir, new LinkOption[0])) {
            return;
        }
        Path standardRootReal = ContentDirectoryManager.getStandardRootReal();
        if (standardRootReal == null) {
            LOGGER.error("Aborting wipe: ContentDirectoryManager has no standardRootReal set");
            return;
        }
        try {
            liveReal = standardDir.toRealPath(new LinkOption[0]);
        }
        catch (IOException e) {
            LOGGER.error("Aborting wipe: could not resolve real path of {}: {}", (Object)standardDir, (Object)e.getMessage());
            return;
        }
        if (!liveReal.equals(standardRootReal)) {
            LOGGER.error("Aborting wipe: standard dir {} resolves to {} which does not match the root captured at init ({}). Refusing to delete anything.", new Object[]{standardDir, liveReal, standardRootReal});
            return;
        }
        try (Stream<Path> walk = Files.walk(liveReal, new FileVisitOption[0]);){
            walk.sorted(Comparator.reverseOrder()).filter(p -> !p.equals(liveReal)).forEach(p -> NativeContentDeployer.deleteIfInsideRoot(p, standardRootReal));
        }
    }

    private static void deleteIfInsideRoot(Path p, Path standardRootReal) {
        try {
            if (Files.isSymbolicLink(p)) {
                Path parentReal;
                Path path = parentReal = p.getParent() == null ? null : p.getParent().toRealPath(new LinkOption[0]);
                if (parentReal == null || !parentReal.startsWith(standardRootReal)) {
                    LOGGER.warn("Skipping symlink whose parent escapes standard root: {}", (Object)p);
                    return;
                }
                Files.delete(p);
                return;
            }
            Path real = p.toRealPath(new LinkOption[0]);
            if (!real.startsWith(standardRootReal)) {
                LOGGER.warn("Skipping path that escapes standard root: {} \u2192 {}", (Object)p, (Object)real);
                return;
            }
            Files.delete(p);
        }
        catch (NoSuchFileException real) {
        }
        catch (IOException e) {
            LOGGER.warn("Could not delete {}: {}", (Object)p, (Object)e.getMessage());
        }
    }

    private static int deployQuests(Path standardDir) {
        List<String> entries = NativeContentDeployer.readManifest("/millenaire/quests/_manifest.json");
        int count = 0;
        for (String entry : entries) {
            Path target;
            String jarPath = "/millenaire/quests/" + entry + ".json";
            if (!NativeContentDeployer.deployFile(jarPath, target = standardDir.resolve("quests").resolve(entry + ".json"))) continue;
            ++count;
        }
        return count += NativeContentDeployer.deployClasspathDir(standardDir, "/millenaire/quests/lang", null, fileName -> {
            String lang = fileName.endsWith(".json") ? fileName.substring(0, fileName.length() - 5) : fileName;
            return standardDir.resolve("languages").resolve(lang).resolve("quest_lang.json");
        });
    }

    /*
     * Enabled aggressive block sorting
     * Enabled unnecessary exception pruning
     * Enabled aggressive exception aggregation
     */
    private static int deployLanguages(Path standardDir) {
        List languages;
        try (InputStream in = NativeContentDeployer.class.getResourceAsStream("/millenaire/languages/_manifest.json");){
            if (in == null) {
                LOGGER.warn("No languages manifest found");
                int n = 0;
                return n;
            }
            JsonObject obj = JsonParser.parseString((String)new String(in.readAllBytes(), StandardCharsets.UTF_8)).getAsJsonObject();
            Type listType = new TypeToken<List<String>>(){}.getType();
            languages = (List)GSON.fromJson(obj.get("languages"), listType);
        }
        catch (Exception e) {
            LOGGER.warn("Could not read languages manifest: {}", (Object)e.getMessage());
            return 0;
        }
        if (languages == null) {
            return 0;
        }
        int count = 0;
        Iterator iterator = languages.iterator();
        while (iterator.hasNext()) {
            String lang = (String)iterator.next();
            Path targetDir = standardDir.resolve("languages").resolve(lang);
            count += NativeContentDeployer.deployClasspathDir(standardDir, "/millenaire/languages/" + lang, targetDir, null);
        }
        return count;
    }

    /*
     * Enabled aggressive block sorting
     * Enabled unnecessary exception pruning
     * Enabled aggressive exception aggregation
     */
    private static List<String> readManifest(String resourcePath) {
        try (InputStream in = NativeContentDeployer.class.getResourceAsStream(resourcePath);){
            if (in == null) {
                List<String> list = List.of();
                return list;
            }
            String json = new String(in.readAllBytes(), StandardCharsets.UTF_8);
            JsonElement root = JsonParser.parseString((String)json);
            if (root.isJsonArray()) {
                Type listType = new TypeToken<List<String>>(){}.getType();
                List list = (List)GSON.fromJson(root, listType);
                return list;
            }
            if (!root.isJsonObject()) return List.of();
            JsonObject obj = root.getAsJsonObject();
            String[] stringArray = new String[]{"files", "languages", "entries"};
            int n = stringArray.length;
            int n2 = 0;
            while (n2 < n) {
                String field = stringArray[n2];
                if (obj.has(field) && obj.get(field).isJsonArray()) {
                    Type listType = new TypeToken<List<String>>(){}.getType();
                    List list = (List)GSON.fromJson(obj.get(field), listType);
                    return list;
                }
                ++n2;
            }
            return List.of();
        }
        catch (Exception e) {
            LOGGER.warn("Could not read manifest {}: {}", (Object)resourcePath, (Object)e.getMessage());
        }
        return List.of();
    }

    /*
     * Enabled force condition propagation
     * Lifted jumps to return sites
     */
    private static List<String> listClasspathDir(String resourceDir) {
        List<String> list;
        URL url = NativeContentDeployer.class.getResource(resourceDir);
        if (url == null) {
            return List.of();
        }
        URI uri = url.toURI();
        if (!"jar".equals(uri.getScheme())) return NativeContentDeployer.listJsonOrAny(Paths.get(uri));
        FileSystem fs = FileSystems.newFileSystem(uri, Map.of());
        try {
            Path dir = fs.getPath(resourceDir, new String[0]);
            list = NativeContentDeployer.listJsonOrAny(dir);
            if (fs == null) return list;
        }
        catch (Throwable throwable) {
            try {
                if (fs == null) throw throwable;
                try {
                    fs.close();
                    throw throwable;
                }
                catch (Throwable throwable2) {
                    throwable.addSuppressed(throwable2);
                }
                throw throwable;
            }
            catch (Exception e) {
                LOGGER.warn("Could not list classpath directory {}: {}", (Object)resourceDir, (Object)e.getMessage());
                return List.of();
            }
        }
        fs.close();
        return list;
    }

    private static List<String> listJsonOrAny(Path dir) throws IOException {
        if (!Files.isDirectory(dir, new LinkOption[0])) {
            return List.of();
        }
        try (Stream<Path> s = Files.list(dir);){
            List<String> list = s.filter(x$0 -> Files.isRegularFile(x$0, new LinkOption[0])).map(p -> p.getFileName().toString()).collect(Collectors.toList());
            return list;
        }
    }

    private static int deployClasspathDir(Path standardDir, String resourceDir, Path targetDir, Function<String, Path> targetMapper) {
        List<String> children = NativeContentDeployer.listClasspathDir(resourceDir);
        int count = 0;
        for (String child : children) {
            Path target;
            String jarPath = resourceDir + "/" + child;
            if (!NativeContentDeployer.deployFile(jarPath, target = targetMapper != null ? targetMapper.apply(child) : targetDir.resolve(child))) continue;
            ++count;
        }
        return count;
    }

    /*
     * Enabled force condition propagation
     * Lifted jumps to return sites
     */
    static int deployClasspathDirRecursive(Path standardDir, String resourceDir, Path targetDir) {
        int n;
        URL url = NativeContentDeployer.class.getResource(resourceDir);
        if (url == null) {
            return 0;
        }
        URI uri = url.toURI();
        if (!"jar".equals(uri.getScheme())) return NativeContentDeployer.walkAndDeploy(Paths.get(uri), resourceDir, targetDir);
        FileSystem fs = FileSystems.newFileSystem(uri, Map.of());
        try {
            n = NativeContentDeployer.walkAndDeploy(fs.getPath(resourceDir, new String[0]), resourceDir, targetDir);
            if (fs == null) return n;
        }
        catch (Throwable throwable) {
            try {
                if (fs == null) throw throwable;
                try {
                    fs.close();
                    throw throwable;
                }
                catch (Throwable throwable2) {
                    throwable.addSuppressed(throwable2);
                }
                throw throwable;
            }
            catch (Exception e) {
                LOGGER.warn("Could not walk classpath directory {}: {}", (Object)resourceDir, (Object)e.getMessage());
                return 0;
            }
        }
        fs.close();
        return n;
    }

    private static int walkAndDeploy(Path rootDir, String resourcePrefix, Path targetDir) throws IOException {
        if (!Files.isDirectory(rootDir, new LinkOption[0])) {
            return 0;
        }
        int count = 0;
        try (Stream<Path> walk = Files.walk(rootDir, new FileVisitOption[0]);){
            for (Path file : walk::iterator) {
                Path target;
                String relText;
                String jarPath;
                Path rel;
                if (!Files.isRegularFile(file, new LinkOption[0]) || NativeContentDeployer.segmentStartsWithUnderscore(rel = rootDir.relativize(file)) || !NativeContentDeployer.deployFile(jarPath = resourcePrefix + "/" + (relText = rel.toString().replace('\\', '/')), target = targetDir.resolve(relText))) continue;
                ++count;
            }
        }
        return count;
    }

    private static boolean segmentStartsWithUnderscore(Path relative) {
        for (Path segment : relative) {
            if (!segment.toString().startsWith("_")) continue;
            return true;
        }
        return false;
    }

    static int deployWelcomeIfAbsent(Path customDir) {
        Path customRootReal = ContentDirectoryManager.getCustomRootReal();
        if (customRootReal == null) {
            LOGGER.error("Cannot deploy welcome README: custom root is not initialised");
            return 0;
        }
        int written = 0;
        for (String basename : new String[]{"README.en.md", "README.fr.md"}) {
            byte[] jarBytes;
            Path target = customDir.resolve(basename);
            if (Files.exists(target, new LinkOption[0])) continue;
            Path customDirApparent = customDir.toAbsolutePath().normalize();
            Path normalised = target.toAbsolutePath().normalize();
            if (!normalised.startsWith(customDirApparent) && !normalised.startsWith(customRootReal)) {
                LOGGER.error("Refusing to write welcome README to {}: target escapes custom root ({})", (Object)target, (Object)customRootReal);
                continue;
            }
            String jarPath = "/millenaire/welcome/" + basename;
            try (InputStream in = NativeContentDeployer.class.getResourceAsStream(jarPath);){
                if (in == null) {
                    LOGGER.debug("Welcome README missing from JAR: {}", (Object)jarPath);
                    continue;
                }
                jarBytes = in.readAllBytes();
            }
            catch (IOException e) {
                LOGGER.warn("Could not read welcome README {}: {}", (Object)jarPath, (Object)e.getMessage());
                continue;
            }
            try {
                Files.createDirectories(target.getParent(), new FileAttribute[0]);
                Files.write(target, jarBytes, new OpenOption[0]);
                ++written;
                LOGGER.info("Deployed welcome README to {} (modder-owned, never overwritten)", (Object)target);
            }
            catch (IOException e) {
                LOGGER.warn("Could not write welcome README {}: {}", (Object)target, (Object)e.getMessage());
            }
        }
        return written;
    }

    /*
     * Enabled aggressive block sorting
     * Enabled unnecessary exception pruning
     * Enabled aggressive exception aggregation
     */
    static boolean deployFile(String jarResourcePath, Path targetPath) {
        byte[] jarBytes;
        Path standardRootReal = ContentDirectoryManager.getStandardRootReal();
        if (standardRootReal == null) {
            LOGGER.error("Cannot deploy {}: standard root is not initialised", (Object)jarResourcePath);
            return false;
        }
        Path standardDirApparent = ContentDirectoryManager.getStandardDir().toAbsolutePath().normalize();
        Path normalised = targetPath.toAbsolutePath().normalize();
        if (!normalised.startsWith(standardDirApparent) && !normalised.startsWith(standardRootReal)) {
            LOGGER.error("Refusing to write {} to {}: target escapes standard root ({})", new Object[]{jarResourcePath, targetPath, standardRootReal});
            return false;
        }
        long sizeLimit = 1000000L;
        if (jarResourcePath.endsWith(".nbt")) {
            sizeLimit = 10000000L;
        } else if (jarResourcePath.endsWith(".txt")) {
            sizeLimit = 512000L;
        }
        try (InputStream in = NativeContentDeployer.class.getResourceAsStream(jarResourcePath);){
            int read;
            if (in == null) {
                LOGGER.debug("JAR resource missing: {}", (Object)jarResourcePath);
                boolean bl = false;
                return bl;
            }
            ByteArrayOutputStream buf = new ByteArrayOutputStream();
            byte[] chunk = new byte[8192];
            while ((read = in.read(chunk)) != -1) {
                if ((long)(buf.size() + read) > sizeLimit) {
                    LOGGER.error("JAR resource {} exceeds size limit ({} bytes) \u2014 skipping", (Object)jarResourcePath, (Object)sizeLimit);
                    boolean bl = false;
                    return bl;
                }
                buf.write(chunk, 0, read);
            }
            jarBytes = buf.toByteArray();
        }
        catch (IOException e) {
            LOGGER.warn("Could not read JAR resource {}: {}", (Object)jarResourcePath, (Object)e.getMessage());
            return false;
        }
        try {
            Path standardDirReal;
            Path parentReal;
            Files.createDirectories(targetPath.getParent(), new FileAttribute[0]);
            try {
                parentReal = targetPath.getParent().toRealPath(new LinkOption[0]);
            }
            catch (IOException e) {
                LOGGER.warn("Could not resolve real path of {}: {}", (Object)targetPath.getParent(), (Object)e.getMessage());
                return false;
            }
            try {
                standardDirReal = ContentDirectoryManager.getStandardDir().toRealPath(new LinkOption[0]);
            }
            catch (IOException e) {
                standardDirReal = standardRootReal;
            }
            if (!parentReal.startsWith(standardRootReal) && !parentReal.startsWith(standardDirReal)) {
                LOGGER.error("Refusing to write {} to {}: parent {} escapes standard root via symlink", new Object[]{jarResourcePath, targetPath, parentReal});
                return false;
            }
            Files.write(targetPath, jarBytes, new OpenOption[0]);
            return true;
        }
        catch (IOException e) {
            LOGGER.warn("Could not deploy {} to {}: {}", new Object[]{jarResourcePath, targetPath, e.getMessage()});
            return false;
        }
    }
}

