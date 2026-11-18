package io.github.arthursouto.archwright.engine.domain.model;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class GroupIdTest {

    @ParameterizedTest
    @DisplayName("BR-002 aceita segmentos em minúsculas separados por ponto")
    @ValueSource(strings = {"com", "com.acme", "com.acme_x", "br.com.a1"})
    void acceptsDottedLowercaseSegments(String raw) {
        assertThat(GroupId.parse(raw)).isPresent();
    }

    @ParameterizedTest
    @DisplayName("BR-002 recusa segmentos vazios, maiúsculas e caracteres inválidos")
    @ValueSource(strings = {"", "com.", ".com", "com..acme", "Com.acme", "com.1acme", "com-acme", "com. acme"})
    void rejectsMalformedGroups(String raw) {
        assertThat(GroupId.parse(raw)).isEmpty();
    }

    @Test
    @DisplayName("BR-002 palavra reservada do Java é aceita no groupId")
    void acceptsReservedWordsUnlikePackages() {
        assertThat(GroupId.parse("com.new")).isPresent();
    }

    @Test
    @DisplayName("BR-002 aceita 128 caracteres e recusa 129")
    void enforcesMaximumLength() {
        String atLimit = "a".repeat(63) + "." + "b".repeat(64);

        assertThat(atLimit).hasSize(128);
        assertThat(GroupId.parse(atLimit)).isPresent();
        assertThat(GroupId.parse(atLimit + "c")).isEmpty();
    }

    @Test
    void trimsButDoesNotChangeCase() {
        assertThat(GroupId.parse("  com.acme ").orElseThrow().value()).isEqualTo("com.acme");
        assertThat(GroupId.parse("Com.acme")).isEmpty();
    }

    @Test
    void constructorProtectsTheInvariant() {
        assertThatThrownBy(() -> new GroupId("com..acme")).isInstanceOf(IllegalArgumentException.class);
    }
}
