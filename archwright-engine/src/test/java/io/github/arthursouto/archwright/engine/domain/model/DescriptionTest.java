package io.github.arthursouto.archwright.engine.domain.model;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class DescriptionTest {

    @Test
    @DisplayName("BR-004 descrição é opcional")
    void emptyDescriptionIsValid() {
        assertThat(Description.empty().value()).isEmpty();
        assertThat(Description.parse("")).isPresent();
    }

    @Test
    @DisplayName("BR-004 aceita 255 caracteres e recusa 256")
    void enforcesMaximumLength() {
        assertThat(Description.parse("a".repeat(255))).isPresent();
        assertThat(Description.parse("a".repeat(256))).isEmpty();
    }

    @ParameterizedTest
    @DisplayName("BR-004 recusa quebra de linha")
    @ValueSource(strings = {"linha 1\nlinha 2", "linha 1\r\nlinha 2", "fim\r", "\n"})
    void rejectsLineBreaks(String raw) {
        assertThat(Description.parse(raw)).isEmpty();
    }

    @Test
    void keepsTextExactlyAsTyped() {
        assertThat(Description.parse("  API <beta> & cia  ").orElseThrow().value()).isEqualTo("  API <beta> & cia  ");
    }

    @Test
    void constructorProtectsTheInvariant() {
        assertThatThrownBy(() -> new Description("a\nb")).isInstanceOf(IllegalArgumentException.class);
    }
}
