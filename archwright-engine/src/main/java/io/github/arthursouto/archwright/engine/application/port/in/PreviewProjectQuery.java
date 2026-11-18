package io.github.arthursouto.archwright.engine.application.port.in;

public interface PreviewProjectQuery {

    PreviewResult preview(ProjectRequest request);
}
