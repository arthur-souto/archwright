package io.github.arthursouto.archwright.engine.domain.model;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class IdentifiersTest {

    @ParameterizedTest
    @ValueSource(strings = {"web", "spring-doc", "jpa2", " flyway "})
    void dependencyIdAcceptsLowercaseKebabCase(String raw) {
        assertThat(DependencyId.parse(raw)).isPresent();
    }

    @ParameterizedTest
    @ValueSource(strings = {"", "Web", "1x", "a_b", "a b", "-a"})
    void dependencyIdRejectsOtherShapes(String raw) {
        assertThat(DependencyId.parse(raw)).isEmpty();
    }

    @Test
    void dependencyIdParseTrims() {
        assertThat(DependencyId.parse(" web ").orElseThrow().value()).isEqualTo("web");
    }

    @ParameterizedTest
    @ValueSource(strings = {"docker-compose", "github-actions", "x"})
    void extraIdAcceptsLowercaseKebabCase(String raw) {
        assertThat(ExtraId.parse(raw)).isPresent();
    }

    @ParameterizedTest
    @ValueSource(strings = {"", "Docker", "1x", "a_b", "a.b"})
    void extraIdRejectsOtherShapes(String raw) {
        assertThat(ExtraId.parse(raw)).isEmpty();
    }

    @Test
    void identifierConstructorsProtectTheirInvariants() {
        assertThatThrownBy(() -> new DependencyId("Web")).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new ExtraId("Docker")).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new KitId(" ")).isInstanceOf(IllegalArgumentException.class);
    }
}
