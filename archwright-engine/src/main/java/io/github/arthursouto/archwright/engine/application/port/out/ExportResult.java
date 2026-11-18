package io.github.arthursouto.archwright.engine.application.port.out;

import java.nio.file.Path;

public record ExportResult(Path output, int fileCount, long totalBytes) {
}
