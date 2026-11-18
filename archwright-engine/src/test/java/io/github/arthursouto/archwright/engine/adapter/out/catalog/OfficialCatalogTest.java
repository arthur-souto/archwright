package io.github.arthursouto.archwright.engine.adapter.out.catalog;

import io.github.arthursouto.archwright.engine.application.usecase.CatalogVerifier;
import io.github.arthursouto.archwright.engine.domain.kit.Kits;
import io.github.arthursouto.archwright.engine.domain.model.ArchitectureStyle;
import io.github.arthursouto.archwright.engine.domain.model.BootProfile;
import io.github.arthursouto.archwright.engine.domain.model.DependencyDefinition;
import io.github.arthursouto.archwright.engine.domain.model.DependencyId;
import io.github.arthursouto.archwright.engine.domain.model.ExtraDefinition;
import io.github.arthursouto.archwright.engine.domain.model.ExtraId;
import io.github.arthursouto.archwright.engine.domain.model.MavenCoordinate;
import io.github.arthursouto.archwright.engine.domain.model.PersistenceNamespace;
import io.github.arthursouto.archwright.engine.domain.model.TestStrategy;
import io.github.arthursouto.archwright.engine.domain.style.ArchitectureStyles;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;

class OfficialCatalogTest {

    private final YamlCatalogRepository repository = new YamlCatalogRepository();

    @Test
    @DisplayName("ENG-CAT-01 #1 catálogos oficiais carregam com 2 estilos e pelo menos 9 perfis")
    void loadsOfficialCatalogs() {
        assertThat(repository.styles()).hasSize(2);
        assertThat(repository.bootProfiles()).hasSizeGreaterThanOrEqualTo(9);
    }

    @Test
    @DisplayName("ENG-CAT-01 #1 kits NONE e BASIC só citam dependências do catálogo")
    void kitsOnlyReferenceCatalogDependencies() {
        CatalogVerifier verifier = new CatalogVerifier(repository, Kits.standard(), ArchitectureStyles.standard());

        assertThatCode(verifier::verify).doesNotThrowAnyException();
    }

    @Test
    void labelsEveryOfficialDependencyAndExtra() {
        assertThat(repository.dependencies()).allSatisfy(dependency -> {
            assertThat(dependency.label()).isNotEqualTo(dependency.id().value());
            assertThat(dependency.description()).isNotBlank();
            assertThat(dependency.group()).isNotBlank();
        });
        assertThat(repository.extras()).allSatisfy(extra -> assertThat(extra.label()).isNotEqualTo(extra.id().value()));
    }

    @Test
    void definesBothArchitectureStylesWithTheirTemplateDirectories() {
        assertThat(repository.styles()).extracting(style -> style.style(), style -> style.templateDir())
                .containsExactlyInAnyOrder(
                        org.assertj.core.groups.Tuple.tuple(ArchitectureStyle.HEXAGONAL_MODULAR, "hexagonal-modular"),
                        org.assertj.core.groups.Tuple.tuple(ArchitectureStyle.HEXAGONAL_LAYERED, "hexagonal-layered"));
    }

    @Test
    void ordersProfilesFromNewestToOldestLine() {
        List<String> lines = repository.bootProfiles().stream().map(BootProfile::line).toList();

        assertThat(lines).containsExactly("4.1", "4.0", "3.5", "3.4", "3.3", "3.2", "3.1", "3.0", "2.7");
    }

    @Test
    void marksOnlyLineTwoPointSevenAsLegacy() {
        List<String> legacy = repository.bootProfiles().stream()
                .filter(BootProfile::legacy)
                .map(BootProfile::line)
                .toList();

        assertThat(legacy).containsExactly("2.7");
    }

    @Test
    void keepsJavaRangesOfTheDocumentedProfiles() {
        assertThat(profile("2.7")).extracting(BootProfile::javaMin, BootProfile::javaMaxTested).containsExactly(8, 21);
        assertThat(profile("3.3")).extracting(BootProfile::javaMin, BootProfile::javaMaxTested).containsExactly(17, 22);
        assertThat(profile("3.5")).extracting(BootProfile::javaMin, BootProfile::javaMaxTested).containsExactly(17, 24);
        assertThat(profile("4.1")).extracting(BootProfile::javaMin, BootProfile::javaMaxTested).containsExactly(17, 25);
    }

    @Test
    void offersEveryCatalogDependencyOnEveryProfile() {
        Set<DependencyId> all = repository.dependencies().stream()
                .map(DependencyDefinition::id)
                .collect(Collectors.toSet());

        assertThat(repository.bootProfiles()).allSatisfy(profile ->
                assertThat(profile.availableDependencies()).containsExactlyInAnyOrderElementsOf(all));
    }

    @Test
    @DisplayName("BR-033 flyway implica jpa e springdoc implica web")
    void declaresImpliedDependencies() {
        assertThat(dependency("flyway").implies()).containsExactly(new DependencyId("jpa"));
        assertThat(dependency("springdoc").implies()).containsExactly(new DependencyId("web"));
        assertThat(dependency("web").implies()).isEmpty();
    }

    @Test
    @DisplayName("BR-034 docker-compose exige jpa e github-actions não exige nada")
    void declaresExtraRequirements() {
        assertThat(extra("docker-compose").requires()).containsExactly(new DependencyId("jpa"));
        assertThat(extra("github-actions").requires()).isEmpty();
    }

    @Test
    @DisplayName("BR-031 as seis dependências do kit BASIC existem no catálogo")
    void containsTheBasicKitDependencies() {
        Set<String> ids = repository.dependencies().stream().map(definition -> definition.id().value()).collect(Collectors.toSet());

        assertThat(ids).contains("web", "validation", "actuator", "jpa", "flyway", "lombok");
    }

    @Test
    void usesJavaxNamespaceOnlyOnTheLegacyLine() {
        assertThat(repository.bootProfiles())
                .filteredOn(profile -> profile.persistenceNamespace() == PersistenceNamespace.JAVAX)
                .extracting(BootProfile::line)
                .containsExactly("2.7");
    }

    @Test
    void usesPerTechnologyTestStartersOnlyFromBootFour() {
        assertThat(repository.bootProfiles())
                .filteredOn(profile -> profile.testStrategy() == TestStrategy.PER_TECHNOLOGY)
                .extracting(BootProfile::line)
                .containsExactly("4.1", "4.0");
    }

    @Test
    void pinsSpringdocAndArchunitOnEveryProfile() {
        assertThat(repository.bootProfiles()).allSatisfy(profile ->
                assertThat(profile.pinnedVersions()).containsKeys("springdoc.version", "archunit.version"));
    }

    @Test
    void givesEveryAvailableDependencyAtLeastOneArtifact() {
        assertThat(repository.bootProfiles()).allSatisfy(profile ->
                assertThat(profile.artifacts()).allSatisfy((dependency, coordinates) ->
                        assertThat(coordinates).as("%s on %s", dependency.value(), profile.line()).isNotEmpty()));
    }

    @Test
    void declaresLombokAsTheOnlyAnnotationProcessor() {
        assertThat(dependency("lombok").annotationProcessors()).containsExactly(
                new MavenCoordinate("org.projectlombok", "lombok", "${lombok.version}", null));
        assertThat(repository.dependencies())
                .filteredOn(definition -> !definition.annotationProcessors().isEmpty())
                .extracting(definition -> definition.id().value())
                .containsExactly("lombok");
    }

    private BootProfile profile(String line) {
        return repository.bootProfiles().stream().filter(profile -> profile.line().equals(line)).findFirst().orElseThrow();
    }

    private DependencyDefinition dependency(String id) {
        return repository.dependencies().stream().filter(definition -> definition.id().value().equals(id)).findFirst().orElseThrow();
    }

    private ExtraDefinition extra(String id) {
        return repository.extras().stream().filter(definition -> definition.id().equals(new ExtraId(id))).findFirst().orElseThrow();
    }
}
