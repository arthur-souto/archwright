package io.github.arthursouto.archwright.engine.adapter.out.version;

import io.github.arthursouto.archwright.engine.application.port.out.CachedVersions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.List;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;

class FileVersionCacheTest {

    private static final Instant AT = Instant.parse("2026-11-18T12:00:00Z");

    @TempDir
    Path directory;

    @Test
    void readsBackWhatWasWritten() {
        FileVersionCache cache = new FileVersionCache(directory.resolve("cache").resolve("boot-versions.json"));

        cache.write(List.of("4.1.2", "4.2.0-M1"), AT);

        assertThat(cache.read()).contains(new CachedVersions(List.of("4.1.2", "4.2.0-M1"), AT));
    }

    @Test
    void createsMissingParentDirectories() {
        Path file = directory.resolve("a").resolve("b").resolve("boot-versions.json");

        new FileVersionCache(file).write(List.of("3.5.3"), AT);

        assertThat(file).isRegularFile();
    }

    @Test
    void overwritesThePreviousContent() {
        FileVersionCache cache = new FileVersionCache(directory.resolve("boot-versions.json"));

        cache.write(List.of("3.5.3"), AT);
        cache.write(List.of("4.1.2"), AT.plusSeconds(60));

        assertThat(cache.read()).contains(new CachedVersions(List.of("4.1.2"), AT.plusSeconds(60)));
    }

    @Test
    void leavesNoTemporaryFilesBehind() throws IOException {
        FileVersionCache cache = new FileVersionCache(directory.resolve("boot-versions.json"));

        cache.write(List.of("3.5.3"), AT);
        cache.write(List.of("4.1.2"), AT);

        try (Stream<Path> files = Files.list(directory)) {
            assertThat(files.map(path -> path.getFileName().toString())).containsExactly("boot-versions.json");
        }
    }

    @Test
    void readsAsEmptyWhenTheFileDoesNotExist() {
        assertThat(new FileVersionCache(directory.resolve("missing.json")).read()).isEmpty();
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "",
            "not json at all",
            "{broken",
            "[]",
            "{\"versions\":[\"3.5.3\"]}",
            "{\"writtenAt\":\"2026-11-18T12:00:00Z\"}",
            "{\"writtenAt\":\"yesterday\",\"versions\":[\"3.5.3\"]}",
            "{\"writtenAt\":\"2026-11-18T12:00:00Z\",\"versions\":\"3.5.3\"}",
            "{\"writtenAt\":\"2026-11-18T12:00:00Z\",\"versions\":[\"3.5.3\",42]}",
            "{\"writtenAt\":20261118,\"versions\":[\"3.5.3\"]}"
    })
    void readsAsEmptyWhenTheContentIsCorrupt(String content) throws IOException {
        Path file = directory.resolve("boot-versions.json");
        Files.writeString(file, content);

        assertThat(new FileVersionCache(file).read()).isEmpty();
    }

    @Test
    void keepsSpecialCharactersIntact() {
        FileVersionCache cache = new FileVersionCache(directory.resolve("boot-versions.json"));
        List<String> versions = List.of("quote\"inside", "back\\slash", "tab\there", "ação");

        cache.write(versions, AT);

        assertThat(cache.read()).contains(new CachedVersions(versions, AT));
    }

    @Test
    void writingIsBestEffortAndNeverThrows() throws IOException {
        Path blocker = Files.writeString(directory.resolve("not-a-directory"), "x");
        FileVersionCache cache = new FileVersionCache(blocker.resolve("boot-versions.json"));

        assertThatCode(() -> cache.write(List.of("3.5.3"), AT)).doesNotThrowAnyException();
        assertThat(cache.read()).isEmpty();
    }

    @Test
    void defaultLocationIsInsideTheUserHomeArchwrightFolder() {
        assertThat(FileVersionCache.inUserHome()).isNotNull();
    }
}
