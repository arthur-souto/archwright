package io.github.arthursouto.archwright.engine.domain.kit;

import io.github.arthursouto.archwright.engine.domain.model.DependencyId;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class KitsTest {

    private final Kits kits = Kits.standard();

    @Test
    void findsNoneKitWithoutDependencies() {
        KitStrategy none = kits.find("NONE").orElseThrow();

        assertThat(none.defaultDependencies()).isEmpty();
    }

    @Test
    void findsBasicKitWithItsSixDependencies() {
        KitStrategy basic = kits.find("BASIC").orElseThrow();

        assertThat(basic.defaultDependencies()).extracting(DependencyId::value)
                .containsExactly("web", "validation", "actuator", "jpa", "flyway", "lombok");
    }

    @Test
    void ignoresSurroundingWhitespace() {
        assertThat(kits.find(" BASIC ")).isPresent();
    }

    @Test
    void doesNotFoldCase() {
        assertThat(kits.find("basic")).isEmpty();
    }

    @Test
    void returnsEmptyForUnknownKit() {
        assertThat(kits.find("PREMIUM")).isEmpty();
    }

    @Test
    void failsWhenTwoStrategiesClaimTheSameId() {
        List<KitStrategy> duplicated = List.of(new NoneKit(), new NoneKit());

        assertThatThrownBy(() -> new Kits(duplicated)).isInstanceOf(IllegalStateException.class);
    }
}
