package io.github.arthursouto.archwright.engine.domain.model;

import java.util.List;

public record DependencyDefinition(
        DependencyId id,
        List<DependencyId> implies,
        List<MavenCoordinate> annotationProcessors) {

    public DependencyDefinition(DependencyId id, List<DependencyId> implies) {
        this(id, implies, List.of());
    }

    public DependencyDefinition {
        implies = List.copyOf(implies);
        annotationProcessors = List.copyOf(annotationProcessors);
    }
}
