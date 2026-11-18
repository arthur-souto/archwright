package io.github.arthursouto.archwright.engine.domain.model;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static org.assertj.core.api.Assertions.assertThat;

class ArchitectureStyleTest {

    @Test
    void parsesExactIds() {
        assertThat(ArchitectureStyle.parse("HEXAGONAL_MODULAR")).contains(ArchitectureStyle.HEXAGONAL_MODULAR);
        assertThat(ArchitectureStyle.parse("HEXAGONAL_LAYERED")).contains(ArchitectureStyle.HEXAGONAL_LAYERED);
    }

    @Test
    void ignoresSurroundingWhitespace() {
        assertThat(ArchitectureStyle.parse("  HEXAGONAL_MODULAR ")).contains(ArchitectureStyle.HEXAGONAL_MODULAR);
    }

    @ParameterizedTest
    @ValueSource(strings = {"", "ONION", "hexagonal_modular", "HEXAGONAL", "HEXAGONAL MODULAR"})
    void rejectsAnythingElse(String raw) {
        assertThat(ArchitectureStyle.parse(raw)).isEmpty();
    }
}
