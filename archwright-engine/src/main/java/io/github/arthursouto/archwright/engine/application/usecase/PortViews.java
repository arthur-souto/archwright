package io.github.arthursouto.archwright.engine.application.usecase;

import io.github.arthursouto.archwright.engine.application.port.in.Violation;
import io.github.arthursouto.archwright.engine.application.port.in.Warning;

import java.util.List;

final class PortViews {

    private PortViews() {
    }

    static List<Violation> violations(List<io.github.arthursouto.archwright.engine.domain.validation.Violation> violations) {
        return violations.stream()
                .map(violation -> new Violation(violation.code().name(), violation.field(), violation.params()))
                .toList();
    }

    static List<Warning> warnings(List<io.github.arthursouto.archwright.engine.domain.validation.Warning> warnings) {
        return warnings.stream()
                .map(warning -> new Warning(warning.code().name(), warning.params()))
                .toList();
    }
}
