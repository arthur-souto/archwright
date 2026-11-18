package io.github.arthursouto.archwright.engine.application.port.in;

import java.nio.file.Path;
import java.util.Objects;

public record ExportTarget(ExportFormat format, Path destination, boolean overwrite) {

    public ExportTarget {
        Objects.requireNonNull(format, "format");
        Objects.requireNonNull(destination, "destination");
    }
}
