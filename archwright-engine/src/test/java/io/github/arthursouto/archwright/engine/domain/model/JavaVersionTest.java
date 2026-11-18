package io.github.arthursouto.archwright.engine.domain.model;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static org.assertj.core.api.Assertions.assertThat;

class JavaVersionTest {

    @ParameterizedTest
    @ValueSource(strings = {"8", "11", "17", "21", "22", "25", " 25 ", "30"})
    void acceptsKnownVersions(String raw) {
        assertThat(JavaVersion.parse(raw)).isPresent();
    }

    @ParameterizedTest
    @ValueSource(strings = {"7", "9", "10", "12", "16", "18", "20", "abc", "", "21.0", "-8"})
    void rejectsUnknownVersions(String raw) {
        assertThat(JavaVersion.parse(raw)).isEmpty();
    }

    @ParameterizedTest
    @ValueSource(ints = {8, 17, 25})
    void comparesByNumber(int value) {
        assertThat(new JavaVersion(value)).isLessThan(new JavaVersion(value + 100));
    }
}
