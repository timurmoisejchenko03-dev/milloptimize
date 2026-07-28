/*
 * Decompiled with CFR 0.152.
 */
package org.millenaire.content.legacy;

import java.time.Clock;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.TreeMap;
import org.millenaire.content.legacy.LegacyLayoutDetector;

public final class LegacyConversionReport {
    private final Map<String, Map<Kind, int[]>> perCulture = new TreeMap<String, Map<Kind, int[]>>();
    private final Map<Kind, int[]> globalCounts = new LinkedHashMap<Kind, int[]>();
    private final List<String> unmappedItems = new ArrayList<String>();
    private final List<String> unmappedBiomes = new ArrayList<String>();
    private final List<SkippedEntry> skipped = new ArrayList<SkippedEntry>();
    private final Set<String> unmappedSpecialPoints = new LinkedHashSet<String>();
    private final Map<Integer, UnmappedColour> unmappedColours = new LinkedHashMap<Integer, UnmappedColour>();
    private final List<LegacyLayoutDetector.Normalisation> normalisations = new ArrayList<LegacyLayoutDetector.Normalisation>();
    private int outOfScopeTxtCount = 0;
    private final List<UnknownKey> unknownKeys = new ArrayList<UnknownKey>();
    private final List<BrokenRef> brokenRefs = new ArrayList<BrokenRef>();
    private final List<InvalidValue> invalidValues = new ArrayList<InvalidValue>();
    private final List<DuplicateEntry> duplicates = new ArrayList<DuplicateEntry>();
    private final List<MissingRequiredKey> missingRequiredKeys = new ArrayList<MissingRequiredKey>();
    private final List<MalformedRow> malformedRows = new ArrayList<MalformedRow>();
    private final List<UnparseableLine> unparseableLines = new ArrayList<UnparseableLine>();
    private final List<EmptyList> emptyLists = new ArrayList<EmptyList>();
    private final List<EmptyNamelist> emptyNamelists = new ArrayList<EmptyNamelist>();
    private final List<InvalidResourceLocation> invalidResourceLocations = new ArrayList<InvalidResourceLocation>();
    private final List<FilenameNormalised> filenameNormalisations = new ArrayList<FilenameNormalised>();
    private final List<QuestStructural> questStructurals = new ArrayList<QuestStructural>();
    private final List<MissingCulturePrefix> missingCulturePrefixes = new ArrayList<MissingCulturePrefix>();
    private final List<UnresolvedTag> unresolvedBuildingTags = new ArrayList<UnresolvedTag>();
    private final List<UnresolvedTag> unresolvedVillagerTags = new ArrayList<UnresolvedTag>();
    private final List<ChainGap> unreachableInputs = new ArrayList<ChainGap>();
    private final List<ChainGap> orphanedOutputs = new ArrayList<ChainGap>();
    private final List<SuspiciousPriority> suspiciousPriorities = new ArrayList<SuspiciousPriority>();
    private final Clock clock;

    public LegacyConversionReport() {
        this(Clock.systemUTC());
    }

    public LegacyConversionReport(Clock clock) {
        this.clock = clock;
    }

    public void recordConverted(String culture, Kind kind) {
        int[] nArray = this.counts(culture, kind);
        nArray[0] = nArray[0] + 1;
    }

    public void recordSkipped(String culture, Kind kind, String path, String reason) {
        this.recordSkipped(culture, kind, path, reason, null);
    }

    void recordSkipped(String culture, Kind kind, String path, String reason, String fixHint) {
        int[] nArray = this.counts(culture, kind);
        nArray[1] = nArray[1] + 1;
        this.skipped.add(new SkippedEntry(culture, kind, path, reason, fixHint, false));
    }

    public void recordPreserved(String culture, Kind kind, String path, String reason) {
        int[] nArray = this.counts(culture, kind);
        nArray[1] = nArray[1] + 1;
        this.skipped.add(new SkippedEntry(culture, kind, path, reason, null, true));
    }

    public void recordUnmappedItem(String culture, String legacyName) {
        this.unmappedItems.add((culture == null ? "<global>" : culture) + "::" + legacyName);
    }

    public void recordUnmappedBiome(String culture, String legacyName) {
        this.unmappedBiomes.add((culture == null ? "<global>" : culture) + "::" + legacyName);
    }

    public void recordOutOfScope(int n) {
        this.outOfScopeTxtCount += n;
    }

    public void recordUnmappedSpecialPoints(Collection<String> signatures) {
        this.unmappedSpecialPoints.addAll(signatures);
    }

    public Set<String> unmappedSpecialPoints() {
        return Collections.unmodifiableSet(this.unmappedSpecialPoints);
    }

    public void recordUnmappedColour(int rgb, String plan, String variant, int level) {
        UnmappedColour existing = this.unmappedColours.get(rgb);
        if (existing == null) {
            this.unmappedColours.put(rgb, new UnmappedColour(rgb, plan, variant, level, 1));
        } else {
            this.unmappedColours.put(rgb, new UnmappedColour(existing.rgb(), existing.plan(), existing.variant(), existing.level(), existing.count() + 1));
        }
    }

    public Collection<UnmappedColour> unmappedColours() {
        return Collections.unmodifiableCollection(this.unmappedColours.values());
    }

    public void recordNormalisation(LegacyLayoutDetector.Normalisation n) {
        if (n != null) {
            this.normalisations.add(n);
        }
    }

    public List<LegacyLayoutDetector.Normalisation> normalisations() {
        return Collections.unmodifiableList(this.normalisations);
    }

    public void recordUnknownKey(String culture, String filePath, String key) {
        for (UnknownKey existing : this.unknownKeys) {
            if (!Objects.equals(existing.culture(), culture) || !Objects.equals(existing.filePath(), filePath) || !existing.key().equals(key)) continue;
            return;
        }
        this.unknownKeys.add(new UnknownKey(culture, filePath, key));
    }

    public List<UnknownKey> unknownKeys() {
        return Collections.unmodifiableList(this.unknownKeys);
    }

    public void recordBrokenRef(String culture, String filePath, String key, String refValue, String refType) {
        this.brokenRefs.add(new BrokenRef(culture, filePath, key, refValue, refType));
    }

    public List<BrokenRef> brokenRefs() {
        return Collections.unmodifiableList(this.brokenRefs);
    }

    public void recordInvalidNumeric(String culture, String filePath, String key, String value, String expected) {
        this.invalidValues.add(new InvalidValue(culture, filePath, key, value, expected, "numeric"));
    }

    public void recordInvalidFormat(String culture, String filePath, String key, String value, String expectedFormat) {
        this.invalidValues.add(new InvalidValue(culture, filePath, key, value, expectedFormat, "format"));
    }

    public void recordInvalidEnum(String culture, String filePath, String key, String value, String allowedJoined) {
        this.invalidValues.add(new InvalidValue(culture, filePath, key, value, allowedJoined, "enum"));
    }

    public List<InvalidValue> invalidValues() {
        return Collections.unmodifiableList(this.invalidValues);
    }

    public void recordDuplicate(String culture, String filePath, String key, String value) {
        this.duplicates.add(new DuplicateEntry(culture, filePath, key, value));
    }

    public List<DuplicateEntry> duplicates() {
        return Collections.unmodifiableList(this.duplicates);
    }

    public void recordMissingRequiredKey(String culture, String filePath, String key) {
        this.missingRequiredKeys.add(new MissingRequiredKey(culture, filePath, key));
    }

    public List<MissingRequiredKey> missingRequiredKeys() {
        return Collections.unmodifiableList(this.missingRequiredKeys);
    }

    public void recordMalformedRow(String culture, String filePath, int line, String reason) {
        this.malformedRows.add(new MalformedRow(culture, filePath, line, reason));
    }

    public List<MalformedRow> malformedRows() {
        return Collections.unmodifiableList(this.malformedRows);
    }

    public void recordUnparseableLine(String culture, String filePath, int line, String content) {
        this.unparseableLines.add(new UnparseableLine(culture, filePath, line, content));
    }

    public List<UnparseableLine> unparseableLines() {
        return Collections.unmodifiableList(this.unparseableLines);
    }

    public void recordEmptyList(String culture, String filePath, String key) {
        this.emptyLists.add(new EmptyList(culture, filePath, key));
    }

    public List<EmptyList> emptyLists() {
        return Collections.unmodifiableList(this.emptyLists);
    }

    public void recordEmptyNamelist(String culture, String namelistName) {
        this.emptyNamelists.add(new EmptyNamelist(culture, namelistName));
    }

    public List<EmptyNamelist> emptyNamelists() {
        return Collections.unmodifiableList(this.emptyNamelists);
    }

    public void recordInvalidResourceLocation(String culture, String filePath, String invalidChars) {
        this.invalidResourceLocations.add(new InvalidResourceLocation(culture, filePath, invalidChars));
    }

    public List<InvalidResourceLocation> invalidResourceLocations() {
        return Collections.unmodifiableList(this.invalidResourceLocations);
    }

    public void recordFilenameNormalised(String culture, String filePath, String originalStem, String canonicalStem) {
        this.filenameNormalisations.add(new FilenameNormalised(culture, filePath, originalStem, canonicalStem));
    }

    public List<FilenameNormalised> filenameNormalisations() {
        return Collections.unmodifiableList(this.filenameNormalisations);
    }

    public void recordQuestStructural(String questKey, String filePath, int step, String problem) {
        this.questStructurals.add(new QuestStructural(questKey, filePath, step, problem));
    }

    public List<QuestStructural> questStructurals() {
        return Collections.unmodifiableList(this.questStructurals);
    }

    public void recordMissingCulturePrefix(String culture, String filePath, String key, String value) {
        this.missingCulturePrefixes.add(new MissingCulturePrefix(culture, filePath, key, value));
    }

    public List<MissingCulturePrefix> missingCulturePrefixes() {
        return Collections.unmodifiableList(this.missingCulturePrefixes);
    }

    public void recordUnresolvedBuildingTag(String culture, String tag, String requiredBy) {
        this.unresolvedBuildingTags.add(new UnresolvedTag(culture, tag, requiredBy));
    }

    public List<UnresolvedTag> unresolvedBuildingTags() {
        return Collections.unmodifiableList(this.unresolvedBuildingTags);
    }

    public void recordUnresolvedVillagerTag(String culture, String tag, String requiredBy) {
        this.unresolvedVillagerTags.add(new UnresolvedTag(culture, tag, requiredBy));
    }

    public List<UnresolvedTag> unresolvedVillagerTags() {
        return Collections.unmodifiableList(this.unresolvedVillagerTags);
    }

    public void recordUnreachableInput(String culture, String item, String requiredBy) {
        this.unreachableInputs.add(new ChainGap(culture, item, requiredBy));
    }

    public List<ChainGap> unreachableInputs() {
        return Collections.unmodifiableList(this.unreachableInputs);
    }

    public void recordOrphanedOutput(String culture, String item, String producedBy) {
        this.orphanedOutputs.add(new ChainGap(culture, item, producedBy));
    }

    public List<ChainGap> orphanedOutputs() {
        return Collections.unmodifiableList(this.orphanedOutputs);
    }

    public void recordSuspiciousPriority(String culture, String filePath, int level, int priority, String reason) {
        this.suspiciousPriorities.add(new SuspiciousPriority(culture, filePath, level, priority, reason));
    }

    public List<SuspiciousPriority> suspiciousPriorities() {
        return Collections.unmodifiableList(this.suspiciousPriorities);
    }

    public int totalConverted() {
        int total = 0;
        for (Map<Kind, int[]> m : this.perCulture.values()) {
            for (int[] pair : m.values()) {
                total += pair[0];
            }
        }
        for (int[] pair : this.globalCounts.values()) {
            total += pair[0];
        }
        return total;
    }

    public int totalSkipped() {
        return this.skipped.size();
    }

    public int totalOutOfScope() {
        return this.outOfScopeTxtCount;
    }

    public int cultureCount() {
        return this.perCulture.size();
    }

    public List<String> unmappedItems() {
        return Collections.unmodifiableList(this.unmappedItems);
    }

    public List<String> unmappedBiomes() {
        return Collections.unmodifiableList(this.unmappedBiomes);
    }

    public List<SkippedEntry> skipped() {
        return Collections.unmodifiableList(this.skipped);
    }

    public boolean hasBlockingAmbiguities() {
        for (SkippedEntry e : this.skipped) {
            if (e.fixHint() == null) continue;
            return true;
        }
        return false;
    }

    public int totalPreserved() {
        int n = 0;
        for (SkippedEntry e : this.skipped) {
            if (!e.preserved()) continue;
            ++n;
        }
        return n;
    }

    public String render() {
        long nameCollisions;
        StringBuilder sb = new StringBuilder();
        int converted = this.totalConverted();
        int skippedCount = this.totalSkipped();
        int unmapped = this.unmappedItems.size();
        if (this.hasBlockingAmbiguities()) {
            this.renderConvertHeader(sb);
        }
        sb.append("Mill\u00e9naire converted ").append(converted).append(" legacy-format files from your millenaire-custom/ folder.").append(" Your server should work normally after restart.\n\n");
        if (unmapped > 0) {
            sb.append(unmapped).append(" item reference").append(unmapped == 1 ? "" : "s").append(" from the pack could not be mapped to modern Minecraft items.").append(" These are usually items that came from a companion mod \u2014 install the").append(" companion mod or ignore the warnings if you don't need those items.\n\n");
        }
        long skippedWorldMarvel = this.skipped.stream().filter(s -> s.kind() == Kind.QUEST).filter(s -> s.reason() != null && s.reason().contains("is out of scope") && s.reason().contains("family '")).count();
        long skippedOtherQuests = this.skipped.stream().filter(s -> s.kind() == Kind.QUEST).count() - skippedWorldMarvel;
        if (skippedWorldMarvel > 0L) {
            sb.append(skippedWorldMarvel).append(" world/marvel quest").append(skippedWorldMarvel == 1L ? " was" : "s were").append(" skipped. Those quest types are not yet supported and will not be").append(" available to players.\n\n");
        }
        if (skippedOtherQuests > 0L) {
            sb.append(skippedOtherQuests).append(" quest").append(skippedOtherQuests == 1L ? " was" : "s were").append(" skipped for other reasons (hand-maintained JSON preserved, parse error,").append(" or missing family directory). See the Skipped files section below.\n\n");
        }
        if (this.outOfScopeTxtCount > 0) {
            sb.append(this.outOfScopeTxtCount).append(" file(s) for out-of-scope features (walls, help, banners) were left").append(" alone. These features are not yet ported to Mill\u00e9naire 9.\n\n");
        }
        if (!this.unmappedSpecialPoints.isEmpty()) {
            int n = this.unmappedSpecialPoints.size();
            sb.append(n).append(" special point type").append(n == 1 ? " was" : "s were").append(" dropped from building plans because Mill\u00e9naire 9").append(" doesn't recognise them (e.g. custom spawners or one-off markers).").append(" The surrounding building still converts; only these points are lost.\n\n");
        }
        if ((nameCollisions = this.skipped.stream().filter(s -> s.reason() != null && s.reason().startsWith("name collides with ")).count()) > 0L) {
            sb.append(nameCollisions).append(" file").append(nameCollisions == 1L ? "" : "s").append(" had name collisions (two TXTs that would produce the same JSON).").append(" The first occurrence was kept and the duplicate skipped.").append(" Rename one of the sources to resolve.\n\n");
        }
        sb.append("Technical details follow below.\n\n");
        sb.append("=== Per-culture summary ===\n");
        if (this.perCulture.isEmpty()) {
            sb.append("  (no culture-scoped content)\n");
        } else {
            for (Map.Entry<String, Map<Kind, int[]>> entry : this.perCulture.entrySet()) {
                sb.append("  ").append(entry.getKey()).append(":\n");
                for (Map.Entry<Kind, int[]> kc : entry.getValue().entrySet()) {
                    sb.append("    ").append(kc.getKey().display()).append(": ").append(kc.getValue()[0]).append(" converted");
                    if (kc.getValue()[1] > 0) {
                        sb.append(", ").append(kc.getValue()[1]).append(" skipped");
                    }
                    sb.append('\n');
                }
            }
        }
        if (!this.globalCounts.isEmpty()) {
            sb.append("\n=== Global content ===\n");
            for (Map.Entry<Object, Object> entry : this.globalCounts.entrySet()) {
                sb.append("  ").append(((Kind)((Object)entry.getKey())).display()).append(": ").append(((int[])entry.getValue())[0]).append(" converted");
                if (((int[])entry.getValue())[1] > 0) {
                    sb.append(", ").append(((int[])entry.getValue())[1]).append(" skipped");
                }
                sb.append('\n');
            }
        }
        if (this.hasBlockingAmbiguities()) {
            this.renderAmbiguitiesSection(sb);
        }
        if (!this.normalisations.isEmpty()) {
            sb.append("\n=== Normalisations ===\n");
            for (LegacyLayoutDetector.Normalisation normalisation : this.normalisations) {
                String original = normalisation.original().getFileName().toString();
                String canonical = normalisation.canonical().getFileName().toString();
                switch (normalisation.outcome()) {
                    case RENAMED: {
                        sb.append("Renamed culture directory '").append(original).append("' \u2192 '").append(canonical).append("'\n");
                        break;
                    }
                    case ALREADY_CANONICAL: {
                        break;
                    }
                    case REJECTED_NON_FOLDABLE: {
                        sb.append("Rejected culture directory '").append(original).append("' (not case-foldable \u2014 rename manually)\n");
                        break;
                    }
                    case REJECTED_COEXISTENCE: {
                        sb.append("Rejected culture directory '").append(original).append("' (coexists with '").append(canonical).append("' \u2014 merge manually)\n");
                    }
                }
            }
        }
        if (!this.unmappedItems.isEmpty()) {
            sb.append("\n=== Unmapped items (").append(this.unmappedItems.size()).append(") ===\n");
            for (String string : this.unmappedItems) {
                sb.append("  ").append(string).append('\n');
            }
        }
        if (!this.unmappedBiomes.isEmpty()) {
            sb.append("\n=== Unmapped biomes (").append(this.unmappedBiomes.size()).append(") ===\n");
            for (String string : this.unmappedBiomes) {
                sb.append("  ").append(string).append('\n');
            }
        }
        if (!this.unmappedSpecialPoints.isEmpty()) {
            sb.append("\n=== Unmapped special-point types (").append(this.unmappedSpecialPoints.size()).append(") ===\n");
            for (String string : this.unmappedSpecialPoints) {
                sb.append("  ").append(string).append('\n');
            }
        }
        if (!this.skipped.isEmpty()) {
            sb.append("\n=== Skipped files (").append(skippedCount).append(") ===\n");
            for (SkippedEntry skippedEntry : this.skipped) {
                sb.append("  [").append(skippedEntry.culture() == null ? "global" : skippedEntry.culture()).append("] ").append(skippedEntry.kind().display()).append(" ").append(skippedEntry.path()).append(" \u2014 ").append(skippedEntry.reason()).append('\n');
            }
        }
        this.renderValidationSections(sb);
        return sb.toString();
    }

    private void renderValidationSections(StringBuilder sb) {
        if (!this.unknownKeys.isEmpty()) {
            sb.append("\n=== Unknown legacy keys (").append(this.unknownKeys.size()).append(") ===\n");
            for (UnknownKey unknownKey : this.unknownKeys) {
                sb.append("  [").append(unknownKey.culture() == null ? "global" : unknownKey.culture()).append("] ").append(unknownKey.filePath()).append(": '").append(unknownKey.key()).append("' is not a recognised legacy key\n");
            }
        }
        if (!this.brokenRefs.isEmpty()) {
            sb.append("\n=== Broken cross-references (").append(this.brokenRefs.size()).append(") ===\n");
            for (BrokenRef brokenRef : this.brokenRefs) {
                sb.append("  [").append(brokenRef.culture() == null ? "global" : brokenRef.culture()).append("] ").append(brokenRef.filePath()).append(": ").append(brokenRef.refType()).append(" '").append(brokenRef.refValue()).append("' (in ").append(brokenRef.key()).append(") not found\n");
            }
        }
        if (!this.invalidValues.isEmpty()) {
            sb.append("\n=== Invalid values (").append(this.invalidValues.size()).append(") ===\n");
            for (InvalidValue invalidValue : this.invalidValues) {
                sb.append("  [").append(invalidValue.culture() == null ? "global" : invalidValue.culture()).append("] ").append(invalidValue.filePath()).append(": '").append(invalidValue.key()).append("=").append(invalidValue.value()).append("' \u2014 expected ").append(invalidValue.expected()).append(" (").append(invalidValue.category()).append(")\n");
            }
        }
        if (!this.duplicates.isEmpty()) {
            sb.append("\n=== Duplicates (").append(this.duplicates.size()).append(") ===\n");
            for (DuplicateEntry duplicateEntry : this.duplicates) {
                sb.append("  [").append(duplicateEntry.culture() == null ? "global" : duplicateEntry.culture()).append("] ").append(duplicateEntry.filePath()).append(": '").append(duplicateEntry.key()).append("' has duplicate entry '").append(duplicateEntry.value()).append("'\n");
            }
        }
        if (!this.missingRequiredKeys.isEmpty()) {
            sb.append("\n=== Missing required keys (").append(this.missingRequiredKeys.size()).append(") ===\n");
            for (MissingRequiredKey missingRequiredKey : this.missingRequiredKeys) {
                sb.append("  [").append(missingRequiredKey.culture() == null ? "global" : missingRequiredKey.culture()).append("] ").append(missingRequiredKey.filePath()).append(": missing '").append(missingRequiredKey.key()).append("'\n");
            }
        }
        if (!this.malformedRows.isEmpty()) {
            sb.append("\n=== Malformed rows (").append(this.malformedRows.size()).append(") ===\n");
            for (MalformedRow malformedRow : this.malformedRows) {
                sb.append("  [").append(malformedRow.culture() == null ? "global" : malformedRow.culture()).append("] ").append(malformedRow.filePath());
                if (malformedRow.line() > 0) {
                    sb.append(" line ").append(malformedRow.line());
                }
                sb.append(": ").append(malformedRow.reason()).append('\n');
            }
        }
        if (!this.unparseableLines.isEmpty()) {
            sb.append("\n=== Unparseable lines (").append(this.unparseableLines.size()).append(") ===\n");
            for (UnparseableLine unparseableLine : this.unparseableLines) {
                sb.append("  [").append(unparseableLine.culture() == null ? "global" : unparseableLine.culture()).append("] ").append(unparseableLine.filePath());
                if (unparseableLine.line() > 0) {
                    sb.append(" line ").append(unparseableLine.line());
                }
                sb.append(": ").append(unparseableLine.content()).append('\n');
            }
        }
        if (!this.emptyLists.isEmpty()) {
            sb.append("\n=== Empty lists (").append(this.emptyLists.size()).append(") ===\n");
            for (EmptyList emptyList : this.emptyLists) {
                sb.append("  [").append(emptyList.culture() == null ? "global" : emptyList.culture()).append("] ").append(emptyList.filePath()).append(": '").append(emptyList.key()).append("' is empty\n");
            }
        }
        if (!this.emptyNamelists.isEmpty()) {
            sb.append("\n=== Empty namelists (").append(this.emptyNamelists.size()).append(") ===\n");
            for (EmptyNamelist emptyNamelist : this.emptyNamelists) {
                sb.append("  [").append(emptyNamelist.culture() == null ? "global" : emptyNamelist.culture()).append("] ").append(emptyNamelist.namelistName()).append('\n');
            }
        }
        if (!this.invalidResourceLocations.isEmpty()) {
            sb.append("\n=== Invalid resource locations (").append(this.invalidResourceLocations.size()).append(") ===\n");
            for (InvalidResourceLocation invalidResourceLocation : this.invalidResourceLocations) {
                sb.append("  [").append(invalidResourceLocation.culture() == null ? "global" : invalidResourceLocation.culture()).append("] ").append(invalidResourceLocation.filePath()).append(": invalid characters: ").append(invalidResourceLocation.invalidChars()).append('\n');
            }
        }
        if (!this.filenameNormalisations.isEmpty()) {
            sb.append("\n=== Filename normalisations (").append(this.filenameNormalisations.size()).append(") ===\n");
            for (FilenameNormalised filenameNormalised : this.filenameNormalisations) {
                sb.append("  [").append(filenameNormalised.culture() == null ? "global" : filenameNormalised.culture()).append("] ").append(filenameNormalised.filePath()).append(": '").append(filenameNormalised.originalStem()).append("' \u2192 '").append(filenameNormalised.canonicalStem()).append("'\n");
            }
        }
        if (!this.questStructurals.isEmpty()) {
            sb.append("\n=== Quest structural issues (").append(this.questStructurals.size()).append(") ===\n");
            for (QuestStructural questStructural : this.questStructurals) {
                sb.append("  [quest ").append(questStructural.questKey()).append("] ").append(questStructural.filePath());
                if (questStructural.step() > 0) {
                    sb.append(" step ").append(questStructural.step());
                }
                sb.append(": ").append(questStructural.problem()).append('\n');
            }
        }
        if (!this.missingCulturePrefixes.isEmpty()) {
            sb.append("\n=== Villager refs missing culture prefix (").append(this.missingCulturePrefixes.size()).append(") ===\n");
            for (MissingCulturePrefix missingCulturePrefix : this.missingCulturePrefixes) {
                sb.append("  [").append(missingCulturePrefix.culture() == null ? "global" : missingCulturePrefix.culture()).append("] ").append(missingCulturePrefix.filePath()).append(": '").append(missingCulturePrefix.key()).append("=").append(missingCulturePrefix.value()).append("' missing 'culture/' prefix\n");
            }
        }
        if (!this.unresolvedBuildingTags.isEmpty()) {
            sb.append("\n=== Unresolved building tags (").append(this.unresolvedBuildingTags.size()).append(") ===\n");
            for (UnresolvedTag unresolvedTag : this.unresolvedBuildingTags) {
                sb.append("  [").append(unresolvedTag.culture()).append("] tag '").append(unresolvedTag.tag()).append("' required by ").append(unresolvedTag.requiredBy()).append(" \u2014 no building sets it\n");
            }
        }
        if (!this.unresolvedVillagerTags.isEmpty()) {
            sb.append("\n=== Unresolved villager tags (").append(this.unresolvedVillagerTags.size()).append(") ===\n");
            for (UnresolvedTag unresolvedTag : this.unresolvedVillagerTags) {
                sb.append("  [").append(unresolvedTag.culture()).append("] tag '").append(unresolvedTag.tag()).append("' required by ").append(unresolvedTag.requiredBy()).append(" \u2014 no villager sets it\n");
            }
        }
        if (!this.unreachableInputs.isEmpty()) {
            sb.append("\n=== Unreachable inputs (").append(this.unreachableInputs.size()).append(") ===\n");
            for (ChainGap chainGap : this.unreachableInputs) {
                sb.append("  [").append(chainGap.culture()).append("] '").append(chainGap.item()).append("' needed by ").append(chainGap.reference()).append(" \u2014 not produced, bought, or in traded_goods\n");
            }
        }
        if (!this.orphanedOutputs.isEmpty()) {
            sb.append("\n=== Orphaned outputs (").append(this.orphanedOutputs.size()).append(") ===\n");
            for (ChainGap chainGap : this.orphanedOutputs) {
                sb.append("  [").append(chainGap.culture()).append("] '").append(chainGap.item()).append("' produced by ").append(chainGap.reference()).append(" \u2014 never consumed, sold, or required\n");
            }
        }
        if (!this.suspiciousPriorities.isEmpty()) {
            sb.append("\n=== Suspicious priorities (").append(this.suspiciousPriorities.size()).append(") ===\n");
            for (SuspiciousPriority suspiciousPriority : this.suspiciousPriorities) {
                sb.append("  [").append(suspiciousPriority.culture() == null ? "global" : suspiciousPriority.culture()).append("] ").append(suspiciousPriority.filePath()).append(" level ").append(suspiciousPriority.level()).append(" prio=").append(suspiciousPriority.priority()).append(": ").append(suspiciousPriority.reason()).append('\n');
            }
        }
    }

    private void renderConvertHeader(StringBuilder sb) {
        int itemCount = 0;
        int biomeCount = 0;
        int colourCount = this.unmappedColours.size();
        int otherCount = 0;
        for (SkippedEntry e : this.skipped) {
            String reason;
            if (e.fixHint() == null) continue;
            String string = reason = e.reason() == null ? "" : e.reason();
            if (reason.startsWith("unmapped item ")) {
                ++itemCount;
                continue;
            }
            if (reason.startsWith("unknown biome ")) {
                ++biomeCount;
                continue;
            }
            if (reason.startsWith("unknown colour ")) continue;
            ++otherCount;
        }
        sb.append("CONVERT mode: this pack is NOT ready to ship.\n\n");
        sb.append("  - ").append(itemCount).append(" unmapped item reference(s) \u2014 see _conversion_unmapped.itemlist.txt\n");
        sb.append("  - ").append(colourCount).append(" unknown PNG colour(s)     \u2014 see _conversion_unmapped.blocklist.txt\n");
        sb.append("  - ").append(biomeCount).append(" unmapped biome(s)         \u2014 see _conversion_unmapped.biome_map.json\n");
        sb.append("  - ").append(otherCount).append(" other ambiguity(ies)      \u2014 see \"Ambiguities requiring action\" below\n\n");
        sb.append("Fix the entries in the three stub files above by filling in their\n");
        sb.append("TODO placeholders, copy them into the real config files, then rerun\n");
        sb.append("/millenaire dev convert-addon. Repeat until all stubs are empty.\n\n");
        sb.append("Separately:\n\n");
    }

    private void renderAmbiguitiesSection(StringBuilder sb) {
        sb.append("\n=== Ambiguities requiring action ===\n");
        for (SkippedEntry e : this.skipped) {
            if (e.fixHint() == null) continue;
            sb.append("  [").append(e.culture() == null ? "global" : e.culture()).append("] ").append(e.kind().display()).append(" ").append(e.path()).append('\n');
            sb.append("    reason:   ").append(e.reason()).append('\n');
            sb.append("    fix hint: ").append(e.fixHint()).append('\n');
        }
    }

    public String oneLineSummary() {
        return String.format(Locale.ROOT, "Mill\u00e9naire: legacy content auto-converted (%d files, %d cultures).", this.totalConverted(), this.cultureCount());
    }

    /*
     * Exception decompiling
     */
    private int[] counts(String culture, Kind kind) {
        /*
         * This method has failed to decompile.  When submitting a bug report, please provide this stack trace, and (if you hold appropriate legal rights) the relevant class file.
         * 
         * java.lang.UnsupportedOperationException
         *     at org.benf.cfr.reader.bytecode.analysis.parse.expression.NewAnonymousArray.getDimSize(NewAnonymousArray.java:142)
         *     at org.benf.cfr.reader.bytecode.analysis.opgraph.op4rewriters.LambdaRewriter.isNewArrayLambda(LambdaRewriter.java:455)
         *     at org.benf.cfr.reader.bytecode.analysis.opgraph.op4rewriters.LambdaRewriter.rewriteDynamicExpression(LambdaRewriter.java:409)
         *     at org.benf.cfr.reader.bytecode.analysis.opgraph.op4rewriters.LambdaRewriter.rewriteDynamicExpression(LambdaRewriter.java:167)
         *     at org.benf.cfr.reader.bytecode.analysis.opgraph.op4rewriters.LambdaRewriter.rewriteExpression(LambdaRewriter.java:105)
         *     at org.benf.cfr.reader.bytecode.analysis.parse.rewriters.ExpressionRewriterHelper.applyForwards(ExpressionRewriterHelper.java:12)
         *     at org.benf.cfr.reader.bytecode.analysis.parse.expression.AbstractMemberFunctionInvokation.applyExpressionRewriterToArgs(AbstractMemberFunctionInvokation.java:101)
         *     at org.benf.cfr.reader.bytecode.analysis.parse.expression.AbstractMemberFunctionInvokation.applyExpressionRewriter(AbstractMemberFunctionInvokation.java:88)
         *     at org.benf.cfr.reader.bytecode.analysis.opgraph.op4rewriters.LambdaRewriter.rewriteExpression(LambdaRewriter.java:103)
         *     at org.benf.cfr.reader.bytecode.analysis.structured.statement.StructuredReturn.rewriteExpressions(StructuredReturn.java:99)
         *     at org.benf.cfr.reader.bytecode.analysis.opgraph.op4rewriters.LambdaRewriter.rewrite(LambdaRewriter.java:88)
         *     at org.benf.cfr.reader.bytecode.analysis.opgraph.Op04StructuredStatement.rewriteLambdas(Op04StructuredStatement.java:1137)
         *     at org.benf.cfr.reader.bytecode.CodeAnalyser.getAnalysisInner(CodeAnalyser.java:912)
         *     at org.benf.cfr.reader.bytecode.CodeAnalyser.getAnalysisOrWrapFail(CodeAnalyser.java:278)
         *     at org.benf.cfr.reader.bytecode.CodeAnalyser.getAnalysis(CodeAnalyser.java:201)
         *     at org.benf.cfr.reader.entities.attributes.AttributeCode.analyse(AttributeCode.java:94)
         *     at org.benf.cfr.reader.entities.Method.analyse(Method.java:531)
         *     at org.benf.cfr.reader.entities.ClassFile.analyseMid(ClassFile.java:1055)
         *     at org.benf.cfr.reader.entities.ClassFile.analyseTop(ClassFile.java:942)
         *     at org.benf.cfr.reader.Driver.doJarVersionTypes(Driver.java:257)
         *     at org.benf.cfr.reader.Driver.doJar(Driver.java:139)
         *     at org.benf.cfr.reader.CfrDriverImpl.analyse(CfrDriverImpl.java:76)
         *     at org.benf.cfr.reader.Main.main(Main.java:54)
         */
        throw new IllegalStateException("Decompilation failed");
    }

    public static enum Kind {
        BUILDING_PLAN("building plan"),
        VILLAGER_TYPE("villager type"),
        VILLAGE_TYPE("village type"),
        SHOP("shop"),
        TRADED_GOOD("traded good"),
        CULTURE("culture"),
        GATHERING_TYPE("gathering type"),
        QUEST("quest"),
        WALL_TYPE("wall type");

        private final String display;

        private Kind(String display) {
            this.display = display;
        }

        public String display() {
            return this.display;
        }
    }

    public record SkippedEntry(String culture, Kind kind, String path, String reason, String fixHint, boolean preserved) {
    }

    public record UnmappedColour(int rgb, String plan, String variant, int level, int count) {
    }

    public record UnknownKey(String culture, String filePath, String key) {
    }

    public record BrokenRef(String culture, String filePath, String key, String refValue, String refType) {
    }

    public record InvalidValue(String culture, String filePath, String key, String value, String expected, String category) {
    }

    public record DuplicateEntry(String culture, String filePath, String key, String value) {
    }

    public record MissingRequiredKey(String culture, String filePath, String key) {
    }

    public record MalformedRow(String culture, String filePath, int line, String reason) {
    }

    public record UnparseableLine(String culture, String filePath, int line, String content) {
    }

    public record EmptyList(String culture, String filePath, String key) {
    }

    public record EmptyNamelist(String culture, String namelistName) {
    }

    public record InvalidResourceLocation(String culture, String filePath, String invalidChars) {
    }

    public record FilenameNormalised(String culture, String filePath, String originalStem, String canonicalStem) {
    }

    public record QuestStructural(String questKey, String filePath, int step, String problem) {
    }

    public record MissingCulturePrefix(String culture, String filePath, String key, String value) {
    }

    public record UnresolvedTag(String culture, String tag, String requiredBy) {
    }

    public record ChainGap(String culture, String item, String reference) {
    }

    public record SuspiciousPriority(String culture, String filePath, int level, int priority, String reason) {
    }
}

