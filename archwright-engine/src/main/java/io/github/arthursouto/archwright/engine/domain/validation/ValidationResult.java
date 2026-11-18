package io.github.arthursouto.archwright.engine.domain.validation;

import io.github.arthursouto.archwright.engine.domain.model.ProjectSpec;

import java.util.List;

public sealed interface ValidationResult {

    record Valid(ProjectSpec spec, List<Warning> warnings) implements ValidationResult {

        public Valid {
            warnings = List.copyOf(warnings);
        }
    }

    record Invalid(List<Violation> violations) implements ValidationResult {

        public Invalid {
            violations = List.copyOf(violations);
        }
    }
}
