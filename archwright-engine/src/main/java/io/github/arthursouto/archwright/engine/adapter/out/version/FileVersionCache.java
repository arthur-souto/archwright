package io.github.arthursouto.archwright.engine.adapter.out.version;

import io.github.arthursouto.archwright.engine.application.port.out.CachedVersions;
import io.github.arthursouto.archwright.engine.application.port.out.VersionCache;
import org.yaml.snakeyaml.LoaderOptions;
import org.yaml.snakeyaml.Yaml;
import org.yaml.snakeyaml.constructor.SafeConstructor;
import org.yaml.snakeyaml.error.YAMLException;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.time.DateTimeException;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

public final class FileVersionCache implements VersionCache {

    private static final String WRITTEN_AT = "writtenAt";
    private static final String VERSIONS = "versions";

    private final Path file;

    public FileVersionCache(Path file) {
        this.file = file.toAbsolutePath();
    }

    public static FileVersionCache inUserHome() {
        return new FileVersionCache(Path.of(System.getProperty("user.home"), ".archwright", "cache", "boot-versions.json"));
    }

    @Override
    public Optional<CachedVersions> read() {
        if (!Files.isRegularFile(file)) {
            return Optional.empty();
        }

        try {
            return parse(Files.readString(file, StandardCharsets.UTF_8));
        } catch (IOException | YAMLException | DateTimeException unreadable) {
            return Optional.empty();
        }
    }

    @Override
    public void write(List<String> versions, Instant at) {
        Path temporary = null;

        try {
            Files.createDirectories(file.getParent());
            temporary = Files.createTempFile(file.getParent(), "boot-versions", ".tmp");
            Files.writeString(temporary, json(versions, at), StandardCharsets.UTF_8);
            replaceWith(temporary);
        } catch (IOException unwritable) {
            deleteQuietly(temporary);
        }
    }

    private Optional<CachedVersions> parse(String text) {
        Object document = yaml().load(text);

        if (!(document instanceof Map<?, ?> values)
                || !(values.get(WRITTEN_AT) instanceof String writtenAt)
                || !(values.get(VERSIONS) instanceof List<?> items)) {
            return Optional.empty();
        }

        List<String> versions = items.stream()
                .filter(String.class::isInstance)
                .map(String.class::cast)
                .toList();

        if (versions.size() != items.size()) {
            return Optional.empty();
        }

        return Optional.of(new CachedVersions(versions, Instant.parse(writtenAt)));
    }

    private Yaml yaml() {
        return new Yaml(new SafeConstructor(new LoaderOptions()));
    }

    private void replaceWith(Path temporary) throws IOException {
        try {
            Files.move(temporary, file, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
        } catch (AtomicMoveNotSupportedException notAtomic) {
            Files.move(temporary, file, StandardCopyOption.REPLACE_EXISTING);
        }
    }

    private void deleteQuietly(Path path) {
        if (path == null) {
            return;
        }

        try {
            Files.deleteIfExists(path);
        } catch (IOException ignored) {
            return;
        }
    }

    private String json(List<String> versions, Instant at) {
        String items = versions.stream().map(FileVersionCache::quoted).collect(Collectors.joining(","));

        return "{" + quoted(WRITTEN_AT) + ":" + quoted(at.toString()) + "," + quoted(VERSIONS) + ":[" + items + "]}\n";
    }

    private static String quoted(String text) {
        StringBuilder quoted = new StringBuilder("\"");

        text.chars().forEach(character -> quoted.append(escaped(character)));

        return quoted.append('"').toString();
    }

    private static String escaped(int character) {
        return switch (character) {
            case '"' -> "\\\"";
            case '\\' -> "\\\\";
            default -> character < 0x20 ? String.format("\\u%04x", character) : String.valueOf((char) character);
        };
    }
}
