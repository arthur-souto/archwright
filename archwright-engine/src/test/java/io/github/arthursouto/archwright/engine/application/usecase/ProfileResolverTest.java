package io.github.arthursouto.archwright.engine.application.usecase;

import io.github.arthursouto.archwright.engine.adapter.out.catalog.YamlCatalogRepository;
import io.github.arthursouto.archwright.engine.domain.model.BootProfile;
import io.github.arthursouto.archwright.engine.domain.model.BootVersion;
import io.github.arthursouto.archwright.engine.domain.model.Catalog;
import io.github.arthursouto.archwright.engine.domain.model.DependencyId;
import io.github.arthursouto.archwright.engine.domain.model.MavenCoordinate;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ProfileResolverTest {

    private static final String BOOT_GROUP = "org.springframework.boot";

    private final ProfileResolver resolver = new ProfileResolver(officialCatalog());

    @Test
    @DisplayName("ENG-BPR-01 #1 Boot 3.5.3 resolve o perfil 3.5")
    void resolvesProfileByLine() {
        BootProfile profile = resolver.profileFor(new BootVersion(3, 5, 3));

        assertThat(profile.line()).isEqualTo("3.5");
    }

    @Test
    @DisplayName("ENG-BPR-01 #2 Boot 4.1.2 usa o starter webmvc")
    void usesWebmvcStarterOnBootFour() {
        assertThat(artifacts("4.1.2", "web")).containsExactly(MavenCoordinate.of(BOOT_GROUP, "spring-boot-starter-webmvc"));
    }

    @Test
    @DisplayName("ENG-BPR-01 #3 Boot 3.5.3 usa o starter web")
    void usesWebStarterOnBootThree() {
        assertThat(artifacts("3.5.3", "web")).containsExactly(MavenCoordinate.of(BOOT_GROUP, "spring-boot-starter-web"));
    }

    @Test
    @DisplayName("ENG-BPR-01 #4 Boot 3.2.12 usa só flyway-core")
    void usesOnlyFlywayCoreBeforeBootThreeThree() {
        assertThat(artifacts("3.2.12", "flyway"))
                .containsExactly(MavenCoordinate.of("org.flywaydb", "flyway-core"));
    }

    @Test
    @DisplayName("ENG-BPR-01 #5 Boot 3.3.0 inclui flyway-database-postgresql")
    void addsPostgresqlModuleFromBootThreeThree() {
        assertThat(artifacts("3.3.0", "flyway")).containsExactly(
                MavenCoordinate.of("org.flywaydb", "flyway-core"),
                MavenCoordinate.of("org.flywaydb", "flyway-database-postgresql"));
    }

    @Test
    @DisplayName("ENG-BPR-01 #6 Boot 4.1.2 com web e jpa usa um starter de teste por tecnologia")
    void usesOneTestStarterPerTechnologyOnBootFour() {
        assertThat(testArtifacts("4.1.2", "web", "jpa")).containsExactly(
                MavenCoordinate.of(BOOT_GROUP, "spring-boot-starter-webmvc-test"),
                MavenCoordinate.of(BOOT_GROUP, "spring-boot-starter-data-jpa-test"));
    }

    @Test
    @DisplayName("ENG-BPR-01 #7 Boot 4.1.2 sem dependências usa o starter de teste base")
    void usesBaseTestStarterWithoutDependencies() {
        assertThat(testArtifacts("4.1.2")).containsExactly(MavenCoordinate.of(BOOT_GROUP, "spring-boot-starter-test"));
    }

    @Test
    @DisplayName("ENG-BPR-01 #8 Boot 3.5.3 com security soma spring-security-test ao starter único")
    void addsSecurityTestToTheSingleStarter() {
        assertThat(testArtifacts("3.5.3", "security")).containsExactly(
                MavenCoordinate.of(BOOT_GROUP, "spring-boot-starter-test"),
                MavenCoordinate.of("org.springframework.security", "spring-security-test"));
    }

    @Test
    @DisplayName("ENG-BPR-01 #9 Boot 2.7.18 usa springdoc-openapi-ui com versão por propriedade")
    void usesLegacySpringdocWithVersionProperty() {
        BootVersion version = new BootVersion(2, 7, 18);

        assertThat(resolver.artifactsFor(version, List.of(new DependencyId("springdoc")))).containsExactly(
                new MavenCoordinate("org.springdoc", "springdoc-openapi-ui", "${springdoc.version}", null));
        assertThat(resolver.profileFor(version).pinnedVersions()).containsEntry("springdoc.version", "1.8.0");
    }

    @Test
    void failsForLineOutsideTheCatalog() {
        assertThatThrownBy(() -> resolver.profileFor(new BootVersion(9, 0, 0)))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("9.0");
    }

    private List<MavenCoordinate> artifacts(String version, String... dependencies) {
        return resolver.artifactsFor(BootVersion.parse(version).orElseThrow(), ids(dependencies));
    }

    private List<MavenCoordinate> testArtifacts(String version, String... dependencies) {
        return resolver.testArtifactsFor(BootVersion.parse(version).orElseThrow(), ids(dependencies));
    }

    private static List<DependencyId> ids(String... dependencies) {
        return java.util.Arrays.stream(dependencies).map(DependencyId::new).toList();
    }

    private static Catalog officialCatalog() {
        YamlCatalogRepository repository = new YamlCatalogRepository();

        return new Catalog(repository.bootProfiles(), repository.dependencies(), repository.extras());
    }
}
