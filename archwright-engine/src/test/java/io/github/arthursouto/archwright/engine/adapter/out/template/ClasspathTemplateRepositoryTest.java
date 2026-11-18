package io.github.arthursouto.archwright.engine.adapter.out.template;

import io.github.arthursouto.archwright.engine.domain.model.LineEnding;
import io.github.arthursouto.archwright.engine.domain.model.Manifest;
import io.github.arthursouto.archwright.engine.domain.model.ManifestFile;
import io.github.arthursouto.archwright.engine.domain.model.ManifestPackage;
import io.github.arthursouto.archwright.engine.domain.model.Repeat;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.io.IOException;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ClasspathTemplateRepositoryTest {

    private final ClasspathTemplateRepository repository = new ClasspathTemplateRepository();

    @TempDir
    Path root;

    @ParameterizedTest
    @ValueSource(strings = {"common", "hexagonal-modular"})
    void everyTemplateOfAnOfficialManifestExistsOnTheClasspath(String directory) {
        Manifest manifest = repository.manifest(directory);

        assertThat(manifest.files()).isNotEmpty().allSatisfy(file ->
                assertThat(repository.content(directory + "/" + file.template()))
                        .as(file.template())
                        .isNotEmpty());
    }

    @Test
    void packageInfoTemplateExists() {
        assertThat(repository.content("common/package-info.java.mustache")).isNotEmpty();
    }

    @Test
    void commonManifestDescribesTheWrapperFilesAsCopiedWithoutRendering() {
        ManifestFile mvnw = file(repository.manifest("common"), "mvnw");
        ManifestFile mvnwCmd = file(repository.manifest("common"), "mvnw.cmd");

        assertThat(mvnw.executable()).isTrue();
        assertThat(mvnw.render()).isFalse();
        assertThat(mvnw.lineEnding()).isEqualTo(LineEnding.LF);
        assertThat(mvnwCmd.render()).isFalse();
        assertThat(mvnwCmd.lineEnding()).isEqualTo(LineEnding.CRLF);
    }

    @Test
    void readsConditionsAndDefaults() {
        ManifestFile migration = file(repository.manifest("common"), "V1__baseline.sql.mustache");
        ManifestFile pom = file(repository.manifest("common"), "pom.xml.mustache");

        assertThat(migration.when()).containsExactly("dep.flyway");
        assertThat(pom.when()).isEmpty();
        assertThat(pom.render()).isTrue();
        assertThat(pom.executable()).isFalse();
        assertThat(pom.repeat()).isEqualTo(Repeat.NONE);
    }

    @Test
    void modularManifestHasThirteenFixedPackagesAndNineteenPerModule() {
        Manifest manifest = repository.manifest("hexagonal-modular");

        assertThat(manifest.packages()).filteredOn(entry -> entry.repeat() == Repeat.NONE).hasSize(13);
        assertThat(manifest.packages()).filteredOn(entry -> entry.repeat() == Repeat.MODULES).hasSize(19);
        assertThat(manifest.packages().getFirst().name()).isEmpty();
    }

    @Test
    void everyPackageHasADescription() {
        assertThat(repository.manifest("hexagonal-modular").packages())
                .extracting(ManifestPackage::description)
                .allSatisfy(description -> assertThat(description).isNotBlank());
    }

    @Test
    void contentKeepsTheExactBytesOfTheTemplate() {
        byte[] windowsScript = repository.content("common/mvnw.cmd");
        String unixScript = new String(repository.content("common/mvnw"), StandardCharsets.UTF_8);

        assertThat(windowsScript).contains((byte) '\r');
        assertThat(unixScript).startsWith("#!/bin/sh");
        assertThat(unixScript).doesNotContain("\r");
    }

    @Test
    void failsWhenTheTemplateDoesNotExist() {
        assertThatThrownBy(() -> repository.content("common/nao-existe.mustache"))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("nao-existe.mustache");
    }

    @Test
    void failsWhenTheManifestDoesNotExist() {
        assertThatThrownBy(() -> repository.manifest("hexagonal-onion"))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("hexagonal-onion/manifest.yaml")
                .hasMessageContaining("not found");
    }

    @Test
    void readsAManifestWithDefaultsFromAnotherClasspath() throws IOException {
        Manifest manifest = load("""
                style: HEXAGONAL_LAYERED
                files:
                  - template: A.mustache
                    target: A.txt
                packages:
                  - package: ""
                    description: Raiz.
                  - forEach: aggregates
                    package: "{{aggregate.name}}"
                    description: "Agregado {{aggregate.name}}."
                """);

        assertThat(manifest.files()).singleElement().satisfies(file -> {
            assertThat(file.render()).isTrue();
            assertThat(file.lineEnding()).isEqualTo(LineEnding.LF);
            assertThat(file.repeat()).isEqualTo(Repeat.NONE);
        });
        assertThat(manifest.packages()).extracting(ManifestPackage::repeat).containsExactly(Repeat.NONE, Repeat.AGGREGATES);
    }

    @Test
    void acceptsAManifestWithoutFilesOrPackages() throws IOException {
        Manifest manifest = load("style: HEXAGONAL_MODULAR\n");

        assertThat(manifest.files()).isEmpty();
        assertThat(manifest.packages()).isEmpty();
    }

    @Test
    void rejectsUnknownKeysToCatchTypos() {
        assertThatThrownBy(() -> load("""
                files:
                  - template: A.mustache
                    target: A.txt
                    executeable: true
                """))
                .hasMessageContaining("manifest.yaml")
                .hasMessageContaining("unknown key 'executeable'");
    }

    @Test
    void rejectsUnknownRootKey() {
        assertThatThrownBy(() -> load("file: []\n")).hasMessageContaining("unknown key 'file'");
    }

    @Test
    void rejectsUnknownStyle() {
        assertThatThrownBy(() -> load("style: ONION\n"))
                .hasMessageContaining("unknown architecture style 'ONION'");
    }

    @Test
    void rejectsUnknownLineEnding() {
        assertThatThrownBy(() -> load("""
                files:
                  - template: A.mustache
                    target: A.txt
                    lineEnding: CR
                """))
                .hasMessageContaining("lineEnding")
                .hasMessageContaining("CR");
    }

    @Test
    void rejectsUnknownRepeat() {
        assertThatThrownBy(() -> load("""
                packages:
                  - forEach: sections
                    package: x
                    description: y
                """))
                .hasMessageContaining("forEach")
                .hasMessageContaining("sections");
    }

    @ParameterizedTest
    @ValueSource(strings = {"dep.jpa or dep.web", "dep.jpa == true", "!", "9flag"})
    void rejectsConditionsThatAreNotFlagNames(String condition) {
        assertThatThrownBy(() -> load("""
                files:
                  - template: A.mustache
                    target: A.txt
                    when: ["%s"]
                """.formatted(condition)))
                .hasMessageContaining("when")
                .hasMessageContaining(condition);
    }

    @Test
    void acceptsNegatedConditions() throws IOException {
        Manifest manifest = load("""
                files:
                  - template: A.mustache
                    target: A.txt
                    when: [dep.web, "!dep.jpa"]
                """);

        assertThat(manifest.files().getFirst().when()).containsExactly("dep.web", "!dep.jpa");
    }

    @Test
    void rejectsMissingTemplateOrTarget() {
        assertThatThrownBy(() -> load("""
                files:
                  - target: A.txt
                """)).hasMessageContaining("'template'");
        assertThatThrownBy(() -> load("""
                files:
                  - template: A.mustache
                """)).hasMessageContaining("'target'");
    }

    @Test
    void rejectsPackageWithoutDescription() {
        assertThatThrownBy(() -> load("""
                packages:
                  - package: x
                """)).hasMessageContaining("'description'");
    }

    @Test
    void rejectsRenderThatIsNotABoolean() {
        assertThatThrownBy(() -> load("""
                files:
                  - template: A.mustache
                    target: A.txt
                    render: maybe
                """)).hasMessageContaining("'render'");
    }

    @Test
    void reportsTheEntryThatFailed() {
        assertThatThrownBy(() -> load("""
                files:
                  - template: A.mustache
                    target: A.txt
                  - template: B.mustache
                """))
                .hasMessageContaining("files[2]")
                .hasMessageContaining("'target'");
    }

    private Manifest load(String yaml) throws IOException {
        Path directory = Files.createDirectories(root.resolve("archwright/templates/fixture"));
        Files.writeString(directory.resolve("manifest.yaml"), yaml);

        try (URLClassLoader loader = new URLClassLoader(new URL[]{root.toUri().toURL()}, null)) {
            return new ClasspathTemplateRepository(loader, "archwright/templates").manifest("fixture");
        }
    }

    private static ManifestFile file(Manifest manifest, String template) {
        return manifest.files().stream()
                .filter(candidate -> candidate.template().equals(template))
                .findFirst()
                .orElseThrow();
    }
}
