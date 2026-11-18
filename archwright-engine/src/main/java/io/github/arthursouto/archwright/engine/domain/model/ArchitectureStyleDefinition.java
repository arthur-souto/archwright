package io.github.arthursouto.archwright.engine.domain.model;

public record ArchitectureStyleDefinition(
        ArchitectureStyle style,
        String label,
        String description,
        String templateDir,
        String modulesLabel) {
}
