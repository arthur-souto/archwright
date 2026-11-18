package io.github.arthursouto.archwright.engine.adapter.out.export;

import io.github.arthursouto.archwright.engine.domain.validation.Violation;
import io.github.arthursouto.archwright.engine.domain.validation.ViolationCode;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

final class ExportChecks {

    private static final String DESTINATION = "destination";

    private ExportChecks() {
    }

    static List<Violation> destination(Path destination) {
        if (!Files.isDirectory(destination)) {
            return List.of(violation(ViolationCode.DESTINATION_NOT_FOUND, destination));
        }

        if (!Files.isWritable(destination)) {
            return List.of(violation(ViolationCode.DESTINATION_NOT_WRITABLE, destination));
        }

        return List.of();
    }

    static Violation violation(ViolationCode code, Path path) {
        return Violation.of(code, DESTINATION, "path", path.toAbsolutePath().normalize().toString());
    }
}
