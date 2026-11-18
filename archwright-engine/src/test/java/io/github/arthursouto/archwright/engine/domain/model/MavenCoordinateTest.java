package io.github.arthursouto.archwright.engine.domain.model;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class MavenCoordinateTest {

    @Test
    void managedCoordinateHasNoVersionNorScope() {
        MavenCoordinate coordinate = MavenCoordinate.of("org.springframework.boot", "spring-boot-starter-web");

        assertThat(coordinate.version()).isNull();
        assertThat(coordinate.scope()).isNull();
        assertThat(coordinate.versionProperty()).isEmpty();
    }

    @Test
    void exposesThePropertyOfAVersionReference() {
        MavenCoordinate coordinate = new MavenCoordinate("org.springdoc", "springdoc-openapi-ui", "${springdoc.version}", null);

        assertThat(coordinate.versionProperty()).contains("springdoc.version");
    }

    @Test
    void fixedVersionIsNotAPropertyReference() {
        MavenCoordinate coordinate = new MavenCoordinate("org.springdoc", "springdoc-openapi-ui", "1.8.0", null);

        assertThat(coordinate.versionProperty()).isEmpty();
    }

    @Test
    void rejectsBlankOrMissingGroupAndArtifact() {
        assertThatThrownBy(() -> MavenCoordinate.of("", "a")).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> MavenCoordinate.of("g", " ")).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> MavenCoordinate.of(null, "a")).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void rejectsBlankVersionOrScopeWhenPresent() {
        assertThatThrownBy(() -> new MavenCoordinate("g", "a", " ", null)).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new MavenCoordinate("g", "a", null, "")).isInstanceOf(IllegalArgumentException.class);
    }
}
