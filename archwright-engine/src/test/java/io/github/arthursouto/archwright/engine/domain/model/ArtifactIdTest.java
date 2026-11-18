package io.github.arthursouto.archwright.engine.domain.model;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ArtifactIdTest {

    @Test
    @DisplayName("BR-001 faz trim e converte para minúsculas")
    void trimsAndLowercases() {
        assertThat(ArtifactId.parse("  Pedido-API ").orElseThrow().value()).isEqualTo("pedido-api");
    }

    @ParameterizedTest
    @DisplayName("BR-001 aceita nomes bem formados")
    @ValueSource(strings = {"a", "a1", "pedido-api", "order.service", "a-b.c-d", "app2-core"})
    void acceptsWellFormedNames(String raw) {
        assertThat(ArtifactId.parse(raw)).isPresent();
    }

    @ParameterizedTest
    @DisplayName("BR-001 recusa nomes malformados")
    @ValueSource(strings = {"", "   ", "a-", "-a", "a--b", "a..b", "a.-b", "a_b", "1a", "a b", "ação"})
    void rejectsMalformedNames(String raw) {
        assertThat(ArtifactId.parse(raw)).isEmpty();
    }

    @Test
    @DisplayName("BR-001 aceita 64 caracteres e recusa 65")
    void enforcesMaximumLength() {
        assertThat(ArtifactId.parse("a".repeat(64))).isPresent();
        assertThat(ArtifactId.parse("a".repeat(65))).isEmpty();
    }

    @Test
    void constructorProtectsTheInvariant() {
        assertThatThrownBy(() -> new ArtifactId("Pedido_API")).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    @DisplayName("BR-005 classe principal vem do artifactId")
    void exposesMainClassName() {
        assertThat(new ArtifactId("pedido-api").mainClassName()).isEqualTo("PedidoApiApplication");
    }
}
