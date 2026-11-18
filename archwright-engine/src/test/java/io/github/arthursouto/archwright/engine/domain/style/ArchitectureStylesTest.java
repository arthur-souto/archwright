package io.github.arthursouto.archwright.engine.domain.style;

import io.github.arthursouto.archwright.engine.domain.model.ArchitectureStyle;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ArchitectureStylesTest {

    @Test
    void standardRegistryCoversEveryArchitectureStyle() {
        ArchitectureStyles styles = ArchitectureStyles.standard();

        for (ArchitectureStyle style : ArchitectureStyle.values()) {
            assertThat(styles.strategyFor(style).style()).isEqualTo(style);
        }
    }

    @Test
    void modularStyleRequiresModulesAndReservesGeneratorNames() {
        ArchitectureStyleStrategy modular = ArchitectureStyles.standard().strategyFor(ArchitectureStyle.HEXAGONAL_MODULAR);

        assertThat(modular.modulesRequired()).isTrue();
        assertThat(modular.reservedModuleNames()).containsExactlyInAnyOrder("shared", "bootstrap", "architecture");
    }

    @Test
    void layeredStyleDoesNotRequireModulesNorReserveNames() {
        ArchitectureStyleStrategy layered = ArchitectureStyles.standard().strategyFor(ArchitectureStyle.HEXAGONAL_LAYERED);

        assertThat(layered.modulesRequired()).isFalse();
        assertThat(layered.reservedModuleNames()).isEmpty();
    }

    @Test
    void failsWhenStyleHasNoRegisteredStrategy() {
        ArchitectureStyles styles = new ArchitectureStyles(List.of(new HexagonalModularStyle()));

        assertThatThrownBy(() -> styles.strategyFor(ArchitectureStyle.HEXAGONAL_LAYERED))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("HEXAGONAL_LAYERED");
    }

    @Test
    void failsWhenTwoStrategiesClaimTheSameStyle() {
        List<ArchitectureStyleStrategy> duplicated = List.of(new HexagonalModularStyle(), new HexagonalModularStyle());

        assertThatThrownBy(() -> new ArchitectureStyles(duplicated))
                .isInstanceOf(IllegalStateException.class);
    }
}
