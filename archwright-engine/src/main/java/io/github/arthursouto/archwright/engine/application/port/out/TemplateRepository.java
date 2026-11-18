package io.github.arthursouto.archwright.engine.application.port.out;

import io.github.arthursouto.archwright.engine.domain.model.Manifest;

public interface TemplateRepository {

    Manifest manifest(String directory);

    byte[] content(String path);
}
