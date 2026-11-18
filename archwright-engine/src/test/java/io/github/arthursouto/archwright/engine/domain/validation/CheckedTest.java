package io.github.arthursouto.archwright.engine.domain.validation;

import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class CheckedTest {

    private static final Violation FIRST = Violation.of(ViolationCode.INVALID_ARTIFACT_ID, "artifactId");
    private static final Violation SECOND = Violation.of(ViolationCode.INVALID_GROUP_ID, "groupId");
    private static final Warning WARNING = new Warning(WarningCode.LEGACY_BOOT_LINE, Map.of("line", "2.7"));

    @Test
    void validCarriesValueAndWarnings() {
        Checked<String> checked = Checked.valid("ok", List.of(WARNING));

        assertThat(checked.isValid()).isTrue();
        assertThat(checked.value()).isEqualTo("ok");
        assertThat(checked.violations()).isEmpty();
        assertThat(checked.warnings()).containsExactly(WARNING);
    }

    @Test
    void validRejectsNullValue() {
        assertThatThrownBy(() -> Checked.valid(null)).isInstanceOf(NullPointerException.class);
    }

    @Test
    void invalidCarriesViolationsAndNoValue() {
        Checked<String> checked = Checked.invalid(FIRST);

        assertThat(checked.isValid()).isFalse();
        assertThat(checked.value()).isNull();
        assertThat(checked.violations()).containsExactly(FIRST);
    }

    @Test
    void skippedIsNeitherValidNorReportsViolations() {
        Checked<String> checked = Checked.skipped();

        assertThat(checked.isValid()).isFalse();
        assertThat(checked.violations()).isEmpty();
    }

    @Test
    void allValidIsTrueOnlyWhenEveryCheckIsValid() {
        assertThat(Checked.allValid(Checked.valid("a"), Checked.valid(1))).isTrue();
        assertThat(Checked.allValid(Checked.valid("a"), Checked.invalid(FIRST))).isFalse();
        assertThat(Checked.allValid(Checked.valid("a"), Checked.skipped())).isFalse();
    }

    @Test
    void violationsAreCollectedInArgumentOrder() {
        List<Violation> violations = Checked.violationsOf(
                Checked.invalid(FIRST), Checked.valid("x"), Checked.skipped(), Checked.invalid(List.of(SECOND, FIRST)));

        assertThat(violations).containsExactly(FIRST, SECOND, FIRST);
    }

    @Test
    void warningsAreCollectedInArgumentOrder() {
        Warning other = new Warning(WarningCode.JAVA_ABOVE_TESTED, Map.of());

        List<Warning> warnings = Checked.warningsOf(
                Checked.valid("a", List.of(WARNING)), Checked.invalid(FIRST), Checked.valid("b", List.of(other)));

        assertThat(warnings).containsExactly(WARNING, other);
    }
}
