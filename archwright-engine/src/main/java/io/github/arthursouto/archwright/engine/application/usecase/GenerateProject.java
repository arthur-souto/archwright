package io.github.arthursouto.archwright.engine.application.usecase;

import io.github.arthursouto.archwright.engine.application.port.in.ExportFormat;
import io.github.arthursouto.archwright.engine.application.port.in.ExportTarget;
import io.github.arthursouto.archwright.engine.application.port.in.GenerateProjectUseCase;
import io.github.arthursouto.archwright.engine.application.port.in.GenerationResult;
import io.github.arthursouto.archwright.engine.application.port.in.ProjectRequest;
import io.github.arthursouto.archwright.engine.application.port.out.ExportResult;
import io.github.arthursouto.archwright.engine.application.port.out.ProjectExporter;
import io.github.arthursouto.archwright.engine.domain.model.GeneratedProject;
import io.github.arthursouto.archwright.engine.domain.validation.Violation;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

public final class GenerateProject implements GenerateProjectUseCase {

    private final ProjectPlanning planning;
    private final ProjectRenderer renderer;
    private final Map<ExportFormat, ProjectExporter> exporters;

    public GenerateProject(ProjectPlanning planning, ProjectRenderer renderer, Collection<ProjectExporter> exporters) {
        this.planning = planning;
        this.renderer = renderer;
        this.exporters = exporters.stream()
                .collect(Collectors.toUnmodifiableMap(ProjectExporter::format, Function.identity()));
    }

    @Override
    public GenerationResult generate(ProjectRequest request, ExportTarget target) {
        return switch (planning.prepare(request.toDraft())) {
            case ProjectPlanning.Outcome.Rejected rejected -> new GenerationResult.Failure(rejected.violations());
            case ProjectPlanning.Outcome.Ready ready -> export(ready, target);
        };
    }

    private GenerationResult export(ProjectPlanning.Outcome.Ready ready, ExportTarget target) {
        ProjectExporter exporter = exporterFor(target.format());
        String artifactId = ready.spec().identity().artifactId().value();

        List<Violation> destination = exporter.check(target, artifactId);

        if (!destination.isEmpty()) {
            return new GenerationResult.Failure(destination);
        }

        GeneratedProject project = renderer.render(ready.plan(), ready.context());

        return success(write(exporter, project, target), ready);
    }

    private ExportResult write(ProjectExporter exporter, GeneratedProject project, ExportTarget target) {
        try {
            return exporter.write(project, target);
        } catch (IOException failure) {
            throw new UncheckedIOException("could not export " + project.artifactId(), failure);
        }
    }

    private GenerationResult success(ExportResult result, ProjectPlanning.Outcome.Ready ready) {
        return new GenerationResult.Success(result.output(), result.fileCount(), result.totalBytes(), ready.warnings());
    }

    private ProjectExporter exporterFor(ExportFormat format) {
        ProjectExporter exporter = exporters.get(format);

        if (exporter == null) {
            throw new IllegalStateException("no exporter registered for format " + format);
        }

        return exporter;
    }
}
