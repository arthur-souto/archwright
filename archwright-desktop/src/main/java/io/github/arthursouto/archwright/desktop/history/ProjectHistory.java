package io.github.arthursouto.archwright.desktop.history;

import io.github.arthursouto.archwright.engine.application.port.in.ExportFormat;
import io.github.arthursouto.archwright.engine.application.port.in.ProjectRequest;

import java.io.IOException;
import java.io.StringReader;
import java.io.StringWriter;
import java.time.Clock;
import java.time.Instant;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.Properties;
import java.util.prefs.BackingStoreException;
import java.util.prefs.Preferences;

public final class ProjectHistory {

    private static final int LIMIT = 20;
    private static final String ENTRY_PREFIX = "entry.";
    private static final String TOTAL = "total";
    private static final String LIST_SEPARATOR = ",";
    private static final String ID_FORMAT = "%019d";

    private final Preferences node;
    private final Clock clock;

    public ProjectHistory(Preferences node, Clock clock) {
        this.node = node;
        this.clock = clock;
    }

    public static ProjectHistory system() {
        return new ProjectHistory(Preferences.userNodeForPackage(ProjectHistory.class), Clock.systemDefaultZone());
    }

    public HistoryEntry record(ProjectRequest request, ExportFormat format, String destination, String output, int fileCount) {
        Instant now = clock.instant();
        HistoryEntry entry = HistoryEntry.of(freeId(now), now, request, format, destination, output, fileCount);

        node.put(ENTRY_PREFIX + entry.id(), encode(entry));
        node.putInt(TOTAL, total() + 1);
        trim();

        return entry;
    }

    public List<HistoryEntry> entries() {
        return keys().stream()
                .sorted(Comparator.reverseOrder())
                .map(this::decode)
                .flatMap(Optional::stream)
                .toList();
    }

    public List<HistoryEntry> recent(int count) {
        return entries().stream().limit(count).toList();
    }

    public int total() {
        return node.getInt(TOTAL, 0);
    }

    public void remove(String id) {
        node.remove(ENTRY_PREFIX + id);
    }

    public void clear() {
        keys().forEach(node::remove);
    }

    private String freeId(Instant now) {
        long newest = keys().stream()
                .map(key -> key.substring(ENTRY_PREFIX.length()))
                .mapToLong(ProjectHistory::parse)
                .max()
                .orElse(Long.MIN_VALUE);

        return ID_FORMAT.formatted(Math.max(now.toEpochMilli(), newest == Long.MIN_VALUE ? newest : newest + 1));
    }

    private static long parse(String id) {
        try {
            return Long.parseLong(id);
        } catch (NumberFormatException invalid) {
            return Long.MIN_VALUE;
        }
    }

    private void trim() {
        List<String> ordered = keys().stream().sorted(Comparator.reverseOrder()).toList();

        ordered.stream().skip(LIMIT).forEach(node::remove);
    }

    private List<String> keys() {
        try {
            return Arrays.stream(node.keys()).filter(key -> key.startsWith(ENTRY_PREFIX)).toList();
        } catch (BackingStoreException unavailable) {
            return List.of();
        }
    }

    private String encode(HistoryEntry entry) {
        Properties values = new Properties();
        values.setProperty("id", entry.id());
        values.setProperty("at", entry.at().toString());
        values.setProperty("artifactId", entry.artifactId());
        values.setProperty("groupId", entry.groupId());
        values.setProperty("basePackage", entry.basePackage());
        values.setProperty("description", entry.description());
        values.setProperty("architectureStyle", entry.architectureStyle());
        values.setProperty("modules", String.join(LIST_SEPARATOR, entry.modules()));
        values.setProperty("bootVersion", entry.bootVersion());
        values.setProperty("javaVersion", entry.javaVersion());
        values.setProperty("kit", entry.kit());
        values.setProperty("dependencies", String.join(LIST_SEPARATOR, entry.dependencies()));
        values.setProperty("extras", String.join(LIST_SEPARATOR, entry.extras()));
        values.setProperty("fitnessFunctions", String.valueOf(entry.fitnessFunctions()));
        values.setProperty("format", entry.format().name());
        values.setProperty("destination", entry.destination());
        values.setProperty("output", entry.output());
        values.setProperty("fileCount", String.valueOf(entry.fileCount()));

        StringWriter writer = new StringWriter();

        try {
            values.store(writer, null);
        } catch (IOException impossible) {
            throw new IllegalStateException(impossible);
        }

        return writer.toString();
    }

    private Optional<HistoryEntry> decode(String key) {
        Properties values = new Properties();

        try {
            values.load(new StringReader(node.get(key, "")));

            return Optional.of(new HistoryEntry(
                    values.getProperty("id"),
                    Instant.parse(values.getProperty("at")),
                    values.getProperty("artifactId"),
                    values.getProperty("groupId"),
                    values.getProperty("basePackage"),
                    values.getProperty("description"),
                    values.getProperty("architectureStyle"),
                    list(values.getProperty("modules")),
                    values.getProperty("bootVersion"),
                    values.getProperty("javaVersion"),
                    values.getProperty("kit"),
                    list(values.getProperty("dependencies")),
                    list(values.getProperty("extras")),
                    Boolean.parseBoolean(values.getProperty("fitnessFunctions")),
                    ExportFormat.valueOf(values.getProperty("format")),
                    values.getProperty("destination"),
                    values.getProperty("output"),
                    Integer.parseInt(values.getProperty("fileCount"))));
        } catch (IOException | RuntimeException unreadable) {
            return Optional.empty();
        }
    }

    private List<String> list(String joined) {
        return joined == null || joined.isBlank()
                ? List.of()
                : Arrays.stream(joined.split(LIST_SEPARATOR)).map(String::strip).filter(name -> !name.isEmpty()).toList();
    }
}
