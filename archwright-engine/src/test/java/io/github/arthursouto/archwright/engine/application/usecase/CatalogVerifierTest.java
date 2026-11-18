package io.github.arthursouto.archwright.engine.application.usecase;

import io.github.arthursouto.archwright.engine.application.port.out.CatalogRepository;
import io.github.arthursouto.archwright.engine.domain.kit.BasicKit;
import io.github.arthursouto.archwright.engine.domain.kit.KitStrategy;
import io.github.arthursouto.archwright.engine.domain.kit.Kits;
import io.github.arthursouto.archwright.engine.domain.kit.NoneKit;
import io.github.arthursouto.archwright.engine.domain.model.ArchitectureStyle;
import io.github.arthursouto.archwright.engine.domain.model.ArchitectureStyleDefinition;
import io.github.arthursouto.archwright.engine.domain.model.BootProfile;
import io.github.arthursouto.archwright.engine.domain.model.DependencyDefinition;
import io.github.arthursouto.archwright.engine.domain.model.DependencyId;
import io.github.arthursouto.archwright.engine.domain.model.ExtraDefinition;
import io.github.arthursouto.archwright.engine.domain.model.KitId;
import io.github.arthursouto.archwright.engine.domain.style.ArchitectureStyles;
import io.github.arthursouto.archwright.engine.domain.style.HexagonalModularStyle;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class CatalogVerifierTest {

    private static final List<String> BASIC_KIT_IDS = List.of("web", "validation", "actuator", "jpa", "flyway", "lombok");

    @Test
    void acceptsKitsAndStylesBackedByStrategies() {
        CatalogVerifier verifier = new CatalogVerifier(
                new FakeRepository(BASIC_KIT_IDS, allStyles()), Kits.standard(), ArchitectureStyles.standard());

        assertThatCode(verifier::verify).doesNotThrowAnyException();
    }

    @Test
    @DisplayName("ENG-CAT-01 #2 kit com dependência inexistente cita o kit e a dependência")
    void rejectsKitReferencingMissingDependency() {
        CatalogVerifier verifier = new CatalogVerifier(
                new FakeRepository(BASIC_KIT_IDS, allStyles()),
                new Kits(List.of(new BrokenKit())),
                ArchitectureStyles.standard());

        assertThatThrownBy(verifier::verify)
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("BROKEN")
                .hasMessageContaining("foo");
    }

    @Test
    void rejectsStandardKitsWhenCatalogLacksOneOfTheirDependencies() {
        CatalogVerifier verifier = new CatalogVerifier(
                new FakeRepository(List.of("web"), allStyles()), Kits.standard(), ArchitectureStyles.standard());

        assertThatThrownBy(verifier::verify)
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("BASIC");
    }

    @Test
    @DisplayName("ENG-CAT-01 #7 estilo do catálogo sem estratégia registrada cita o estilo")
    void rejectsStyleWithoutRegisteredStrategy() {
        ArchitectureStyles onlyModular = new ArchitectureStyles(List.of(new HexagonalModularStyle()));
        CatalogVerifier verifier = new CatalogVerifier(
                new FakeRepository(BASIC_KIT_IDS, allStyles()), new Kits(List.of(new NoneKit(), new BasicKit())), onlyModular);

        assertThatThrownBy(verifier::verify)
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("HEXAGONAL_LAYERED")
                .hasMessageContaining("no strategy");
    }

    private static List<ArchitectureStyleDefinition> allStyles() {
        return List.of(
                new ArchitectureStyleDefinition(ArchitectureStyle.HEXAGONAL_MODULAR, "Modular", "x", "hexagonal-modular", "Módulos"),
                new ArchitectureStyleDefinition(ArchitectureStyle.HEXAGONAL_LAYERED, "Camadas", "x", "hexagonal-layered", "Agregados"));
    }

    private static final class BrokenKit implements KitStrategy {

        @Override
        public KitId id() {
            return new KitId("BROKEN");
        }

        @Override
        public String label() {
            return "Quebrado";
        }

        @Override
        public List<DependencyId> defaultDependencies() {
            return List.of(new DependencyId("web"), new DependencyId("foo"));
        }
    }

    private static final class FakeRepository implements CatalogRepository {

        private final List<DependencyDefinition> dependencies;
        private final List<ArchitectureStyleDefinition> styles;

        private FakeRepository(List<String> dependencyIds, List<ArchitectureStyleDefinition> styles) {
            this.dependencies = dependencyIds.stream()
                    .map(id -> new DependencyDefinition(new DependencyId(id), List.of()))
                    .toList();
            this.styles = styles;
        }

        @Override
        public List<BootProfile> bootProfiles() {
            return List.of();
        }

        @Override
        public List<DependencyDefinition> dependencies() {
            return dependencies;
        }

        @Override
        public List<ExtraDefinition> extras() {
            return List.of();
        }

        @Override
        public List<ArchitectureStyleDefinition> styles() {
            return styles;
        }
    }
}
