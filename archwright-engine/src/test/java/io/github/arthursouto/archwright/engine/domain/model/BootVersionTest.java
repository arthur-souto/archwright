package io.github.arthursouto.archwright.engine.domain.model;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static org.assertj.core.api.Assertions.assertThat;

class BootVersionTest {

    @Test
    void parsesStableVersionAndExposesLine() {
        BootVersion version = BootVersion.parse(" 3.5.3 ").orElseThrow();

        assertThat(version).isEqualTo(new BootVersion(3, 5, 3));
        assertThat(version.line()).isEqualTo("3.5");
        assertThat(version).hasToString("3.5.3");
    }

    @ParameterizedTest
    @ValueSource(strings = {"3.5.0-RC1", "4.2.0-M1", "3.5.0-SNAPSHOT", "3.5", "3", "a.b.c", "", "3.5.3.1"})
    void rejectsNonStableOrMalformedVersions(String raw) {
        assertThat(BootVersion.parse(raw)).isEmpty();
    }

    @Test
    void ordersByMajorMinorThenPatch() {
        BootVersion older = new BootVersion(3, 5, 9);
        BootVersion newer = new BootVersion(4, 0, 0);

        assertThat(older).isLessThan(newer);
        assertThat(new BootVersion(3, 10, 0)).isGreaterThan(new BootVersion(3, 9, 99));
    }

    @Test
    void minimumSupportedIsTwoPointSevenZero() {
        assertThat(new BootVersion(2, 6, 15)).isLessThan(BootVersion.MINIMUM_SUPPORTED);
        assertThat(new BootVersion(2, 7, 18)).isGreaterThanOrEqualTo(BootVersion.MINIMUM_SUPPORTED);
    }
}
