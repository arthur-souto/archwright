package io.github.arthursouto.archwright.engine.application.usecase;

import io.github.arthursouto.archwright.engine.application.port.in.ArchitectureStyleOption;
import io.github.arthursouto.archwright.engine.application.port.in.BootVersions;
import io.github.arthursouto.archwright.engine.application.port.in.DependencyOption;
import io.github.arthursouto.archwright.engine.application.port.in.JavaVersionOption;
import io.github.arthursouto.archwright.engine.application.port.in.KitOption;
import io.github.arthursouto.archwright.engine.application.port.in.VersionOrigin;
import io.github.arthursouto.archwright.engine.adapter.out.version.EmbeddedBootVersions;
import io.github.arthursouto.archwright.engine.application.port.out.CachedVersions;
import io.github.arthursouto.archwright.engine.application.port.out.VersionCache;
import io.github.arthursouto.archwright.engine.domain.kit.Kits;
import io.github.arthursouto.archwright.engine.domain.style.ArchitectureStyles;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.time.Clock;
import java.time.Instant;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

class BrowseCatalogTest {

    private final BrowseCatalog catalog = new BrowseCatalog(
            SpecBuilder.catalog(),
            SpecBuilder.repository().styles(),
            ArchitectureStyles.standard(),
            Kits.standard(),
            new BootVersionLookup(
                    () -> {
                        throw new IOException("offline");
                    },
                    new EmptyCache(),
                    new EmbeddedBootVersions(),
                    SpecBuilder.catalog(),
                    Clock.systemUTC()));

    @Test
    @DisplayName("ENG-UC-03 #1 Boot 3.5.3 lista Java 17 a 25, marca os acima do testado e usa 21 como padrão")
    void listsJavaVersionsForBoot35() {
        List<JavaVersionOption> options = catalog.javaVersions("3.5.3");

        assertThat(options).extracting(JavaVersionOption::version).containsExactly("17", "21", "22", "23", "24", "25");
        assertThat(options).filteredOn(JavaVersionOption::aboveTested).extracting(JavaVersionOption::version).containsExactly("25");
        assertThat(options).filteredOn(JavaVersionOption::isDefault).extracting(JavaVersionOption::version).containsExactly("21");
        assertThat(options).filteredOn(JavaVersionOption::lts).extracting(JavaVersionOption::version).containsExactly("17", "21", "25");
    }

    @Test
    @DisplayName("ENG-UC-03 #2 Boot 2.7.18 começa no Java 8 e usa 21 como padrão")
    void listsJavaVersionsForBoot27() {
        List<JavaVersionOption> options = catalog.javaVersions("2.7.18");

        assertThat(options.getFirst().version()).isEqualTo("8");
        assertThat(options).filteredOn(JavaVersionOption::isDefault).extracting(JavaVersionOption::version).containsExactly("21");
    }

    @Test
    void answersEmptyForUnsupportedOrMalformedBootVersions() {
        assertThat(catalog.javaVersions("1.5.0")).isEmpty();
        assertThat(catalog.javaVersions("abc")).isEmpty();
        assertThat(catalog.javaVersions(null)).isEmpty();
        assertThat(catalog.dependencies("1.5.0")).isEmpty();
    }

    @Test
    @DisplayName("ENG-UC-03 #3 os kits são NONE e BASIC com as dependências de cada um")
    void listsKitsWithTheirDependencies() {
        List<KitOption> kits = catalog.kits();

        assertThat(kits).extracting(KitOption::id).containsExactly("BASIC", "NONE");
        assertThat(kits.getFirst().dependencies()).containsExactly("web", "validation", "actuator", "jpa", "flyway", "lombok");
        assertThat(kits.getLast().dependencies()).isEmpty();
    }

    @Test
    @DisplayName("ENG-UC-03 #4 Boot 4.1.2 lista as dependências do catálogo disponíveis na linha")
    void listsDependenciesAvailableForTheLine() {
        List<DependencyOption> options = catalog.dependencies("4.1.2");

        assertThat(options).extracting(DependencyOption::id)
                .containsExactlyInAnyOrderElementsOf(
                        SpecBuilder.catalog().bootProfiles().stream()
                                .filter(profile -> profile.line().equals("4.1"))
                                .findFirst().orElseThrow()
                                .availableDependencies().stream()
                                .map(id -> id.value())
                                .toList());
        assertThat(options).filteredOn(option -> option.id().equals("flyway")).singleElement()
                .satisfies(option -> {
                    assertThat(option.implies()).containsExactly("jpa");
                    assertThat(option.label()).isEqualTo("Flyway");
                    assertThat(option.group()).isEqualTo("Dados");
                    assertThat(option.description()).isNotBlank();
                });
    }

    @Test
    void listsTheArchitectureStylesWithTheModulesRequirement() {
        List<ArchitectureStyleOption> styles = catalog.architectureStyles();

        assertThat(styles).extracting(ArchitectureStyleOption::id).containsExactly("HEXAGONAL_MODULAR", "HEXAGONAL_LAYERED");
        assertThat(styles).extracting(ArchitectureStyleOption::modulesLabel).containsExactly("Módulos", "Agregados");
        assertThat(styles).extracting(ArchitectureStyleOption::modulesRequired).containsExactly(true, false);
    }

    @Test
    void listsTheExtrasWithTheirRequirements() {
        assertThat(catalog.extras()).hasSize(2);
        assertThat(catalog.extras().getFirst().label()).isEqualTo("docker-compose.yml com PostgreSQL");
        assertThat(catalog.extras().getFirst().requires()).containsExactly("jpa");
    }

    @Test
    void delegatesTheBootVersionsToTheLookup() {
        BootVersions versions = catalog.bootVersions();

        assertThat(versions.origin()).isEqualTo(VersionOrigin.EMBEDDED);
        assertThat(versions.versions()).isNotEmpty();
    }

    @Test
    void suggestsTheBasePackageAndToleratesNulls() {
        assertThat(catalog.suggestBasePackage("com.acme", "pedido-api")).isEqualTo("com.acme.pedidoapi");
        assertThat(catalog.suggestBasePackage(null, "pedido-api")).isEmpty();
        assertThat(catalog.suggestBasePackage("com.acme", null)).isEmpty();
    }

    @Test
    void hasNoFitnessRulesYet() {
        assertThat(catalog.fitnessRules("HEXAGONAL_MODULAR", List.of(), "4.1.2")).isEmpty();
    }

    private static final class EmptyCache implements VersionCache {

        @Override
        public Optional<CachedVersions> read() {
            return Optional.empty();
        }

        @Override
        public void write(List<String> versions, Instant at) {
        }
    }
}
