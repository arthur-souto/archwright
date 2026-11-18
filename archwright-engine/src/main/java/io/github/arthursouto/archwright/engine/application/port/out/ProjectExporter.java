package io.github.arthursouto.archwright.engine.application.port.out;

import io.github.arthursouto.archwright.engine.application.port.in.ExportFormat;
import io.github.arthursouto.archwright.engine.application.port.in.ExportTarget;
import io.github.arthursouto.archwright.engine.domain.model.GeneratedProject;
import io.github.arthursouto.archwright.engine.domain.validation.Violation;

import java.io.IOException;
import java.util.List;

public interface ProjectExporter {

    ExportFormat format();

    List<Violation> check(ExportTarget target, String artifactId);

    ExportResult write(GeneratedProject project, ExportTarget target) throws IOException;
}
