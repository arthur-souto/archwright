package io.github.arthursouto.archwright.engine.application.port.in;

public record ArchitectureStyleOption(
        String id, String label, String description, String modulesLabel, boolean modulesRequired) {
}
