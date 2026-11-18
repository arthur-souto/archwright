package io.github.arthursouto.archwright.engine.domain.naming;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static org.assertj.core.api.Assertions.assertThat;

class PackageSegmentsTest {

    @ParameterizedTest
    @ValueSource(strings = {"pedidos", "order2", "my_module", "a"})
    void acceptsLowercaseIdentifiers(String segment) {
        assertThat(PackageSegments.isValid(segment)).isTrue();
    }

    @ParameterizedTest
    @ValueSource(strings = {"", "Order", "2order", "my-module", "new", "class", "record", "var"})
    void rejectsBadShapeAndReservedWords(String segment) {
        assertThat(PackageSegments.isValid(segment)).isFalse();
    }

    @Test
    void reservedWordHasValidShapeButIsNotValid() {
        assertThat(PackageSegments.hasIdentifierShape("new")).isTrue();
        assertThat(PackageSegments.isValid("new")).isFalse();
    }

    @Test
    void firstInvalidReturnsOffendingSegment() {
        assertThat(PackageSegments.firstInvalid("com.acme.new")).contains("new");
    }

    @Test
    void firstInvalidReturnsEmptySegmentForDoubleDot() {
        assertThat(PackageSegments.firstInvalid("com..acme")).contains("");
    }

    @Test
    void firstInvalidIsEmptyForValidName() {
        assertThat(PackageSegments.firstInvalid("com.acme.pedidoapi")).isEmpty();
    }
}
