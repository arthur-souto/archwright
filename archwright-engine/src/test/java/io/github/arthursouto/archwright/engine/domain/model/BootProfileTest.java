package io.github.arthursouto.archwright.engine.domain.model;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class BootProfileTest {

    private static final DependencyId WEB = new DependencyId("web");
    private static final DependencyId JPA = new DependencyId("jpa");
    private static final DependencyId SECURITY = new DependencyId("security");

    private static final MavenCoordinate STARTER_WEB = MavenCoordinate.of("org.springframework.boot", "spring-boot-starter-web");
    private static final MavenCoordinate STARTER_JPA = MavenCoordinate.of("org.springframework.boot", "spring-boot-starter-data-jpa");
    private static final MavenCoordinate POSTGRES = new MavenCoordinate("org.postgresql", "postgresql", null, "runtime");
    private static final MavenCoordinate SECURITY_TEST = MavenCoordinate.of("org.springframework.security", "spring-security-test");
    private static final MavenCoordinate WEB_TEST = MavenCoordinate.of("org.springframework.boot", "spring-boot-starter-webmvc-test");
    private static final MavenCoordinate JPA_TEST = MavenCoordinate.of("org.springframework.boot", "spring-boot-starter-data-jpa-test");
    private static final MavenCoordinate TEST_STARTER = MavenCoordinate.of("org.springframework.boot", "spring-boot-starter-test");

    private final BootProfile profile = singleStarter();

    @Test
    @DisplayName("ENG-CAT-01 #6 javaMin maior que javaMaxTested é recusado citando a linha")
    void rejectsJavaMinGreaterThanJavaMaxTested() {
        assertThatThrownBy(() -> build("3.5", 25, 17, TestStrategy.SINGLE_STARTER, Map.of(), Map.of(), Map.of()))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("3.5");
    }

    @Test
    void rejectsTestArtifactsForDependencyWithoutArtifacts() {
        Map<DependencyId, List<MavenCoordinate>> testArtifacts = Map.of(SECURITY, List.of(SECURITY_TEST));

        assertThatThrownBy(() -> build("3.5", 17, 24, TestStrategy.SINGLE_STARTER, Map.of(WEB, List.of(STARTER_WEB)), testArtifacts, Map.of()))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("3.5")
                .hasMessageContaining("security");
    }

    @Test
    void rejectsVersionPropertyThatIsNotPinned() {
        MavenCoordinate springdoc = new MavenCoordinate("org.springdoc", "springdoc-openapi-ui", "${springdoc.version}", null);
        Map<DependencyId, List<MavenCoordinate>> artifacts = Map.of(new DependencyId("springdoc"), List.of(springdoc));

        assertThatThrownBy(() -> build("2.7", 8, 21, TestStrategy.SINGLE_STARTER, artifacts, Map.of(), Map.of()))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("2.7")
                .hasMessageContaining("springdoc.version");
    }

    @Test
    void acceptsVersionPropertyThatIsPinned() {
        MavenCoordinate springdoc = new MavenCoordinate("org.springdoc", "springdoc-openapi-ui", "${springdoc.version}", null);
        Map<DependencyId, List<MavenCoordinate>> artifacts = Map.of(new DependencyId("springdoc"), List.of(springdoc));

        BootProfile pinned = build("2.7", 8, 21, TestStrategy.SINGLE_STARTER, artifacts, Map.of(), Map.of("springdoc.version", "1.8.0"));

        assertThat(pinned.pinnedVersions()).containsEntry("springdoc.version", "1.8.0");
    }

    @Test
    void supportsJavaFromMinimumUpwards() {
        assertThat(profile.supports(new JavaVersion(11))).isFalse();
        assertThat(profile.supports(new JavaVersion(17))).isTrue();
        assertThat(profile.supports(new JavaVersion(25))).isTrue();
    }

    @Test
    void testsJavaUpToTestedMaximum() {
        assertThat(profile.isTested(new JavaVersion(24))).isTrue();
        assertThat(profile.isTested(new JavaVersion(25))).isFalse();
    }

    @Test
    void derivesAvailabilityFromArtifacts() {
        assertThat(profile.isAvailable(WEB)).isTrue();
        assertThat(profile.isAvailable(new DependencyId("kafka"))).isFalse();
        assertThat(profile.availableDependencies()).containsExactlyInAnyOrder(WEB, JPA, SECURITY);
    }

    @Test
    void resolvesArtifactsInTheOrderOfTheRequestedDependencies() {
        assertThat(profile.artifactsFor(List.of(JPA, WEB))).containsExactly(STARTER_JPA, POSTGRES, STARTER_WEB);
    }

    @Test
    void skipsUnavailableDependenciesAndDuplicates() {
        assertThat(profile.artifactsFor(List.of(WEB, new DependencyId("kafka"), WEB))).containsExactly(STARTER_WEB);
    }

    @Test
    void singleStarterKeepsTheBaseTestStarterFirst() {
        assertThat(profile.testArtifactsFor(List.of(SECURITY))).containsExactly(TEST_STARTER, SECURITY_TEST);
        assertThat(profile.testArtifactsFor(List.of(WEB))).containsExactly(TEST_STARTER);
    }

    @Test
    void perTechnologyUsesOnlyTheTestStartersOfTheChosenTechnologies() {
        BootProfile perTechnology = perTechnology();

        assertThat(perTechnology.testArtifactsFor(List.of(WEB, JPA))).containsExactly(WEB_TEST, JPA_TEST);
    }

    @Test
    void perTechnologyFallsBackToTheBaseStarterWhenNoTechnologyHasOne() {
        BootProfile perTechnology = perTechnology();

        assertThat(perTechnology.testArtifactsFor(List.of())).containsExactly(TEST_STARTER);
        assertThat(perTechnology.testArtifactsFor(List.of(SECURITY))).containsExactly(TEST_STARTER);
    }

    private static BootProfile singleStarter() {
        Map<DependencyId, List<MavenCoordinate>> artifacts = Map.of(
                WEB, List.of(STARTER_WEB),
                JPA, List.of(STARTER_JPA, POSTGRES),
                SECURITY, List.of());

        return build("3.5", 17, 24, TestStrategy.SINGLE_STARTER, artifacts, Map.of(SECURITY, List.of(SECURITY_TEST)), Map.of());
    }

    private static BootProfile perTechnology() {
        Map<DependencyId, List<MavenCoordinate>> artifacts = Map.of(WEB, List.of(), JPA, List.of(), SECURITY, List.of());

        return build("4.1", 17, 25, TestStrategy.PER_TECHNOLOGY, artifacts,
                Map.of(WEB, List.of(WEB_TEST), JPA, List.of(JPA_TEST)), Map.of());
    }

    private static BootProfile build(
            String line,
            int javaMin,
            int javaMaxTested,
            TestStrategy strategy,
            Map<DependencyId, List<MavenCoordinate>> artifacts,
            Map<DependencyId, List<MavenCoordinate>> testArtifacts,
            Map<String, String> pinnedVersions) {
        return new BootProfile(
                line, javaMin, javaMaxTested, false, PersistenceNamespace.JAKARTA,
                strategy, artifacts, testArtifacts, pinnedVersions);
    }
}
