package io.github.arthursouto.archwright.engine.adapter.out.catalog;

import io.github.arthursouto.archwright.engine.domain.model.BootProfile;
import io.github.arthursouto.archwright.engine.domain.model.DependencyId;
import io.github.arthursouto.archwright.engine.domain.model.MavenCoordinate;
import io.github.arthursouto.archwright.engine.domain.model.PersistenceNamespace;
import io.github.arthursouto.archwright.engine.domain.model.TestStrategy;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Path;

import static io.github.arthursouto.archwright.engine.adapter.out.catalog.CatalogFixture.DEPENDENCIES;
import static io.github.arthursouto.archwright.engine.adapter.out.catalog.CatalogFixture.EXTRAS;
import static io.github.arthursouto.archwright.engine.adapter.out.catalog.CatalogFixture.PROFILES;
import static io.github.arthursouto.archwright.engine.adapter.out.catalog.CatalogFixture.STYLES;
import static io.github.arthursouto.archwright.engine.adapter.out.catalog.CatalogFixture.profile;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class CatalogConsistencyTest {

    @TempDir
    Path root;

    @Test
    void loadsAMinimalValidCatalog() throws IOException {
        YamlCatalogRepository repository = CatalogFixture.valid().load(root);

        assertThat(repository.styles()).hasSize(2);
        assertThat(repository.bootProfiles()).hasSize(1);
        assertThat(repository.dependencies()).hasSize(1);
        assertThat(repository.extras()).hasSize(1);
    }

    @Test
    @DisplayName("ENG-CAT-01 #3 duas linhas iguais citam o arquivo e a linha")
    void rejectsDuplicatedProfileLine() {
        CatalogFixture fixture = CatalogFixture.valid()
                .with(PROFILES, profile("3.5", 17, 24, false, "web") + profile("3.5", 17, 21, false, "web"));

        assertThatThrownBy(() -> fixture.load(root))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("boot-profiles.yaml")
                .hasMessageContaining("3.5");
    }

    @Test
    @DisplayName("ENG-CAT-01 #4 perfil com artefato de dependência inexistente cita o perfil e a dependência")
    void rejectsProfileArtifactsForUnknownDependency() {
        CatalogFixture fixture = CatalogFixture.valid().with(PROFILES, profile("3.5", 17, 24, false, "web", "foo"));

        assertThatThrownBy(() -> fixture.load(root))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("boot-profiles.yaml")
                .hasMessageContaining("3.5")
                .hasMessageContaining("foo");
    }

    @Test
    @DisplayName("ENG-CAT-01 #6 javaMin maior que javaMaxTested cita a linha")
    void rejectsJavaMinGreaterThanJavaMaxTested() {
        CatalogFixture fixture = CatalogFixture.valid().with(PROFILES, profile("3.5", 25, 17, false, "web"));

        assertThatThrownBy(() -> fixture.load(root))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("boot-profiles.yaml")
                .hasMessageContaining("3.5");
    }

    @Test
    void rejectsDependencyThatImpliesAnUnknownDependency() {
        CatalogFixture fixture = CatalogFixture.valid().with(DEPENDENCIES, """
                - id: web
                  implies: [foo]
                """);

        assertThatThrownBy(() -> fixture.load(root))
                .hasMessageContaining("dependencies.yaml")
                .hasMessageContaining("web")
                .hasMessageContaining("foo");
    }

    @Test
    void rejectsExtraThatRequiresAnUnknownDependency() {
        CatalogFixture fixture = CatalogFixture.valid().with(EXTRAS, """
                - id: docker-compose
                  requires: [foo]
                """);

        assertThatThrownBy(() -> fixture.load(root))
                .hasMessageContaining("extras.yaml")
                .hasMessageContaining("docker-compose")
                .hasMessageContaining("foo");
    }

    @Test
    void rejectsDuplicatedDependencyId() {
        CatalogFixture fixture = CatalogFixture.valid().with(DEPENDENCIES, """
                - id: web
                - id: web
                """);

        assertThatThrownBy(() -> fixture.load(root))
                .hasMessageContaining("dependencies.yaml")
                .hasMessageContaining("duplicated dependency web");
    }

    @Test
    void rejectsUnknownStyleId() {
        CatalogFixture fixture = CatalogFixture.valid().with(STYLES, """
                - id: ONION
                  label: Onion
                  description: x
                  templateDir: onion
                  modulesLabel: Módulos
                """);

        assertThatThrownBy(() -> fixture.load(root))
                .hasMessageContaining("styles.yaml")
                .hasMessageContaining("ONION");
    }

    @Test
    void rejectsCatalogWithoutEveryArchitectureStyle() {
        CatalogFixture fixture = CatalogFixture.valid().with(STYLES, """
                - id: HEXAGONAL_MODULAR
                  label: Modular
                  description: x
                  templateDir: hexagonal-modular
                  modulesLabel: Módulos
                """);

        assertThatThrownBy(() -> fixture.load(root))
                .hasMessageContaining("styles.yaml")
                .hasMessageContaining("missing style HEXAGONAL_LAYERED");
    }

    @Test
    void rejectsMissingFile() {
        CatalogFixture fixture = CatalogFixture.valid().without(EXTRAS);

        assertThatThrownBy(() -> fixture.load(root))
                .hasMessageContaining("extras.yaml")
                .hasMessageContaining("not found on classpath");
    }

    @Test
    void rejectsEmptyFile() {
        CatalogFixture fixture = CatalogFixture.valid().with(EXTRAS, "");

        assertThatThrownBy(() -> fixture.load(root))
                .hasMessageContaining("extras.yaml")
                .hasMessageContaining("non-empty list");
    }

    @Test
    void rejectsMalformedYaml() {
        CatalogFixture fixture = CatalogFixture.valid().with(EXTRAS, "- id: [unclosed");

        assertThatThrownBy(() -> fixture.load(root)).hasMessageContaining("extras.yaml");
    }

    @Test
    void rejectsDuplicatedYamlKeys() {
        CatalogFixture fixture = CatalogFixture.valid().with(DEPENDENCIES, """
                - id: web
                  id: other
                """);

        assertThatThrownBy(() -> fixture.load(root)).hasMessageContaining("dependencies.yaml");
    }

    @Test
    void rejectsUnquotedLineThatYamlReadsAsANumber() {
        CatalogFixture fixture = CatalogFixture.valid().with(PROFILES, """
                - line: 3.10
                  javaMin: 17
                  javaMaxTested: 24
                  persistenceNamespace: jakarta
                  tests: SINGLE_STARTER
                  artifacts:
                    web: []
                """);

        assertThatThrownBy(() -> fixture.load(root))
                .hasMessageContaining("boot-profiles.yaml")
                .hasMessageContaining("quote it");
    }

    @Test
    void rejectsMissingRequiredField() {
        CatalogFixture fixture = CatalogFixture.valid().with(PROFILES, """
                - line: "3.5"
                  javaMaxTested: 24
                  persistenceNamespace: jakarta
                  tests: SINGLE_STARTER
                  artifacts:
                    web: []
                """);

        assertThatThrownBy(() -> fixture.load(root))
                .hasMessageContaining("entry 1 (3.5)")
                .hasMessageContaining("javaMin");
    }

    @Test
    void rejectsUnknownPersistenceNamespace() {
        CatalogFixture fixture = CatalogFixture.valid().with(PROFILES, profile("3.5", 17, 24, false, "web")
                .replace("persistenceNamespace: jakarta", "persistenceNamespace: jdk"));

        assertThatThrownBy(() -> fixture.load(root))
                .hasMessageContaining("persistenceNamespace")
                .hasMessageContaining("jdk");
    }

    @Test
    void rejectsUnknownTestStrategy() {
        CatalogFixture fixture = CatalogFixture.valid().with(PROFILES, profile("3.5", 17, 24, false, "web")
                .replace("tests: SINGLE_STARTER", "tests: EVERYTHING"));

        assertThatThrownBy(() -> fixture.load(root))
                .hasMessageContaining("'tests'")
                .hasMessageContaining("EVERYTHING");
    }

    @Test
    void rejectsArtifactEntryWithoutGroup() {
        CatalogFixture fixture = CatalogFixture.valid().with(PROFILES, """
                - line: "3.5"
                  javaMin: 17
                  javaMaxTested: 24
                  persistenceNamespace: jakarta
                  tests: SINGLE_STARTER
                  artifacts:
                    web: [{ a: spring-boot-starter-web }]
                """);

        assertThatThrownBy(() -> fixture.load(root))
                .hasMessageContaining("entry 1 (3.5)")
                .hasMessageContaining("artifacts.web[1]")
                .hasMessageContaining("'g'");
    }

    @Test
    void rejectsVersionPropertyThatIsNotPinned() {
        CatalogFixture fixture = CatalogFixture.valid().with(PROFILES, """
                - line: "3.5"
                  javaMin: 17
                  javaMaxTested: 24
                  persistenceNamespace: jakarta
                  tests: SINGLE_STARTER
                  artifacts:
                    web: [{ g: org.springdoc, a: springdoc, v: "${springdoc.version}" }]
                """);

        assertThatThrownBy(() -> fixture.load(root))
                .hasMessageContaining("boot-profiles.yaml")
                .hasMessageContaining("springdoc.version");
    }

    @Test
    void rejectsTestArtifactsForDependencyWithoutArtifacts() {
        CatalogFixture fixture = CatalogFixture.valid().with(PROFILES, """
                - line: "3.5"
                  javaMin: 17
                  javaMaxTested: 24
                  persistenceNamespace: jakarta
                  tests: SINGLE_STARTER
                  artifacts:
                    web: []
                  testArtifacts:
                    security: [{ g: org.springframework.security, a: spring-security-test }]
                """);

        assertThatThrownBy(() -> fixture.load(root))
                .hasMessageContaining("3.5")
                .hasMessageContaining("testArtifacts")
                .hasMessageContaining("security");
    }

    @Test
    void rejectsPinnedVersionWrittenAsNumber() {
        CatalogFixture fixture = CatalogFixture.valid().with(PROFILES, profile("3.5", 17, 24, false, "web")
                + "  pinnedVersions:\n    springdoc.version: 3.0\n");

        assertThatThrownBy(() -> fixture.load(root))
                .hasMessageContaining("pinnedVersions.springdoc.version")
                .hasMessageContaining("quote");
    }

    @Test
    void readsNamespaceStrategyCoordinatesAndPinnedVersions() throws IOException {
        CatalogFixture fixture = CatalogFixture.valid()
                .with(DEPENDENCIES, """
                        - id: web
                        - id: springdoc
                        """)
                .with(PROFILES, """
                        - line: "3.5"
                          javaMin: 17
                          javaMaxTested: 24
                          persistenceNamespace: javax
                          tests: PER_TECHNOLOGY
                          pinnedVersions:
                            springdoc.version: "2.8.17"
                          artifacts:
                            web: [{ g: org.springframework.boot, a: spring-boot-starter-web }]
                            springdoc: [{ g: org.springdoc, a: springdoc-openapi-starter-webmvc-ui, v: "${springdoc.version}", scope: compile }]
                          testArtifacts:
                            web: [{ g: org.springframework.boot, a: spring-boot-starter-webmvc-test }]
                        """);

        BootProfile profile = fixture.load(root).bootProfiles().getFirst();

        assertThat(profile.persistenceNamespace()).isEqualTo(PersistenceNamespace.JAVAX);
        assertThat(profile.testStrategy()).isEqualTo(TestStrategy.PER_TECHNOLOGY);
        assertThat(profile.pinnedVersions()).containsEntry("springdoc.version", "2.8.17");
        assertThat(profile.artifacts().get(new DependencyId("springdoc"))).containsExactly(
                new MavenCoordinate("org.springdoc", "springdoc-openapi-starter-webmvc-ui", "${springdoc.version}", "compile"));
        assertThat(profile.testArtifacts().get(new DependencyId("web"))).containsExactly(
                MavenCoordinate.of("org.springframework.boot", "spring-boot-starter-webmvc-test"));
    }

    @Test
    void sortsProfilesNewestFirstByNumbersNotByText() throws IOException {
        CatalogFixture fixture = CatalogFixture.valid().with(PROFILES,
                profile("3.5", 17, 24, false, "web") + profile("3.10", 17, 24, false, "web") + profile("4.1", 17, 25, false, "web"));

        assertThat(fixture.load(root).bootProfiles()).extracting(BootProfile::line)
                .containsExactly("4.1", "3.10", "3.5");
    }

    @Test
    void readsLegacyFlagAndDefaultsToFalse() throws IOException {
        String legacy = profile("2.7", 8, 21, true, "web");
        String current = profile("3.5", 17, 24, false, "web").replace("  legacy: false\n", "");
        CatalogFixture fixture = CatalogFixture.valid().with(PROFILES, legacy + current);

        assertThat(fixture.load(root).bootProfiles()).extracting(BootProfile::legacy)
                .containsExactly(false, true);
    }
}
