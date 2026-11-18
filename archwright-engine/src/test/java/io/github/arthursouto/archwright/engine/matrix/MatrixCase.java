package io.github.arthursouto.archwright.engine.matrix;

import io.github.arthursouto.archwright.engine.application.port.in.ProjectRequest;

public record MatrixCase(String name, String java, String jdk, ProjectRequest request) {
}
