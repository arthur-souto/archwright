package io.github.arthursouto.archwright.engine.domain.model;

import java.util.List;

public record DependencyDefinition(DependencyId id, List<DependencyId> implies) {

    public DependencyDefinition {
        implies = List.copyOf(implies);
    }
}
