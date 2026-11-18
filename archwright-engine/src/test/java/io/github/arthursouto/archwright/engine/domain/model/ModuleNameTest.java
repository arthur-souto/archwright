package io.github.arthursouto.archwright.engine.domain.model;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ModuleNameTest {

    @Test
    @DisplayName("BR-011 faz trim e converte para minúsculas")
    void trimsAndLowercases() {
        assertThat(ModuleName.parse(" Pedidos ").orElseThrow().value()).isEqualTo("pedidos");
    }

    @ParameterizedTest
    @DisplayName("BR-011 recusa o que não é segmento de pacote válido")
    @ValueSource(strings = {"", "  ", "class", "2bad", "a-b", "a.b", "ação"})
    void rejectsInvalidSegments(String raw) {
        assertThat(ModuleName.parse(raw)).isEmpty();
    }

    @Test
    @DisplayName("BR-011 nomes reservados do estilo não são decididos pelo tipo")
    void reservedStyleNamesAreNotTheTypesConcern() {
        assertThat(ModuleName.parse("shared")).isPresent();
    }

    @Test
    void constructorProtectsTheInvariant() {
        assertThatThrownBy(() -> new ModuleName("Pedidos")).isInstanceOf(IllegalArgumentException.class);
    }
}
