package io.github.arthursouto.archwright.engine.domain.model;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class BootProfileTest {

    private final BootProfile profile = new BootProfile("3.5", 17, 24, false, Set.of(new DependencyId("web")));

    @Test
    @DisplayName("ENG-CAT-01 #6 javaMin maior que javaMaxTested é recusado citando a linha")
    void rejectsJavaMinGreaterThanJavaMaxTested() {
        assertThatThrownBy(() -> new BootProfile("3.5", 25, 17, false, Set.of()))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("3.5");
    }

    @Test
    void supportsJavaFromMinimumUpwards() {
        assertThat(profile.supports(new JavaVersion(11))).isFalse();
        assertThat(profile.supports(new JavaVersion(17))).isTrue();
        assertThat(profile.supports(new JavaVersion(25))).isTrue();
    }

    @Test
    void testsJavaUpToTestedMaximum() {
        assertThat(profile.isTested(new JavaVersion(24))).isTrue();
        assertThat(profile.isTested(new JavaVersion(25))).isFalse();
    }

    @Test
    void reportsDependencyAvailability() {
        assertThat(profile.isAvailable(new DependencyId("web"))).isTrue();
        assertThat(profile.isAvailable(new DependencyId("jpa"))).isFalse();
    }
}
