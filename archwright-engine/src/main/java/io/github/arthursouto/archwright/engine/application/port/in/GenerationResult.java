package io.github.arthursouto.archwright.engine.application.port.in;

import io.github.arthursouto.archwright.engine.domain.validation.Violation;
import io.github.arthursouto.archwright.engine.domain.validation.Warning;

import java.nio.file.Path;
import java.util.List;

public sealed interface GenerationResult {

    record Success(Path output, int fileCount, long totalBytes, List<Warning> warnings) implements GenerationResult {

        public Success {
            warnings = List.copyOf(warnings);
        }
    }

    record Failure(List<Violation> violations) implements GenerationResult {

        public Failure {
            violations = List.copyOf(violations);
        }
    }
}
