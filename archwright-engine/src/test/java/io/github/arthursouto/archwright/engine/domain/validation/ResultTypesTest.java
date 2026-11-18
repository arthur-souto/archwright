package io.github.arthursouto.archwright.engine.domain.validation;

import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ResultTypesTest {

    @Test
    void violationFactoriesBuildExpectedParams() {
        assertThat(Violation.of(ViolationCode.INVALID_GROUP_ID, "groupId").params()).isEmpty();
        assertThat(Violation.of(ViolationCode.INVALID_BASE_PACKAGE, "basePackage", "segment", "new").params())
                .containsExactly(Map.entry("segment", "new"));
    }

    @Test
    void violationsWithSameContentAreEqual() {
        assertThat(Violation.of(ViolationCode.UNKNOWN_KIT, "kit", "value", "X"))
                .isEqualTo(Violation.of(ViolationCode.UNKNOWN_KIT, "kit", "value", "X"));
    }

    @Test
    void violationCopiesItsParams() {
        Map<String, String> params = new HashMap<>(Map.of("segment", "new"));
        Violation violation = new Violation(ViolationCode.INVALID_BASE_PACKAGE, "basePackage", params);

        params.put("extra", "x");

        assertThat(violation.params()).containsOnlyKeys("segment");
        assertThatThrownBy(() -> violation.params().put("x", "y")).isInstanceOf(UnsupportedOperationException.class);
    }

    @Test
    void warningCopiesItsParams() {
        Map<String, String> params = new HashMap<>(Map.of("line", "2.7"));
        Warning warning = new Warning(WarningCode.LEGACY_BOOT_LINE, params);

        params.put("extra", "x");

        assertThat(warning.params()).containsOnlyKeys("line");
    }

    @Test
    void invalidResultCopiesItsViolations() {
        List<Violation> source = new ArrayList<>(List.of(Violation.of(ViolationCode.UNKNOWN_KIT, "kit")));
        ValidationResult.Invalid invalid = new ValidationResult.Invalid(source);

        source.clear();

        assertThat(invalid.violations()).hasSize(1);
    }
}
