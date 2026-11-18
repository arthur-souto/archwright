package io.github.arthursouto.archwright.engine.adapter.out.version;

import io.github.arthursouto.archwright.engine.adapter.out.catalog.YamlCatalogRepository;
import io.github.arthursouto.archwright.engine.domain.model.BootProfile;
import io.github.arthursouto.archwright.engine.domain.model.BootVersion;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class EmbeddedBootVersionsTest {

    private final List<String> official = new EmbeddedBootVersions().versions();

    @Test
    void officialListIsNotEmptyAndEveryEntryIsAStableVersion() {
        assertThat(official).isNotEmpty();
        assertThat(official).allSatisfy(version ->
                assertThat(BootVersion.parse(version)).as(version).isPresent());
    }

    @Test
    void officialListStartsAtTwoPointSeven() {
        assertThat(official).allSatisfy(version ->
                assertThat(BootVersion.parse(version).orElseThrow()).isGreaterThanOrEqualTo(BootVersion.MINIMUM_SUPPORTED));
    }

    @Test
    void officialListCoversEveryLineOfTheOfficialCatalog() {
        Set<String> embeddedLines = official.stream()
                .map(BootVersion::parse)
                .flatMap(Optional::stream)
                .map(BootVersion::line)
                .collect(Collectors.toSet());

        Set<String> catalogLines = new YamlCatalogRepository().bootProfiles().stream()
                .map(BootProfile::line)
                .collect(Collectors.toSet());

        assertThat(embeddedLines).containsAll(catalogLines);
    }

    @Test
    void skipsCommentsAndBlankLines(@TempDir Path root) throws IOException {
        Path file = root.resolve("versions.txt");
        Files.writeString(file, "# comment\n\n  3.5.3  \n3.5.2\n   \n# another\n");

        try (URLClassLoader loader = new URLClassLoader(new URL[]{root.toUri().toURL()}, null)) {
            assertThat(new EmbeddedBootVersions(loader, "versions.txt").versions()).containsExactly("3.5.3", "3.5.2");
        }
    }

    @Test
    void failsWhenTheResourceIsMissing() {
        assertThatThrownBy(() -> new EmbeddedBootVersions(getClass().getClassLoader(), "missing.txt"))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("missing.txt");
    }
}
