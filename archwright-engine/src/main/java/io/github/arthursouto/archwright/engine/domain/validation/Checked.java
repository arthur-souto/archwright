package io.github.arthursouto.archwright.engine.domain.validation;

import java.util.Arrays;
import java.util.List;
import java.util.Objects;

record Checked<T>(T value, List<Violation> violations, List<Warning> warnings) {

    Checked {
        violations = List.copyOf(violations);
        warnings = List.copyOf(warnings);
    }

    static <T> Checked<T> valid(T value) {
        return valid(value, List.of());
    }

    static <T> Checked<T> valid(T value, List<Warning> warnings) {
        return new Checked<>(Objects.requireNonNull(value), List.of(), warnings);
    }

    static <T> Checked<T> invalid(Violation violation) {
        return invalid(List.of(violation));
    }

    static <T> Checked<T> invalid(List<Violation> violations) {
        return new Checked<>(null, violations, List.of());
    }

    static <T> Checked<T> skipped() {
        return new Checked<>(null, List.of(), List.of());
    }

    static boolean allValid(Checked<?>... checks) {
        return Arrays.stream(checks).allMatch(Checked::isValid);
    }

    static List<Violation> violationsOf(Checked<?>... checks) {
        return Arrays.stream(checks).flatMap(check -> check.violations().stream()).toList();
    }

    static List<Warning> warningsOf(Checked<?>... checks) {
        return Arrays.stream(checks).flatMap(check -> check.warnings().stream()).toList();
    }

    boolean isValid() {
        return value != null;
    }
}
