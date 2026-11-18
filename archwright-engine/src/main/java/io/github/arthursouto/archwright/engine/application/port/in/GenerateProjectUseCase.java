package io.github.arthursouto.archwright.engine.application.port.in;

public interface GenerateProjectUseCase {

    GenerationResult generate(ProjectRequest request, ExportTarget target);
}
