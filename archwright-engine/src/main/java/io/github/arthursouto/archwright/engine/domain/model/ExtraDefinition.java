package io.github.arthursouto.archwright.engine.domain.model;

import java.util.List;

public record ExtraDefinition(ExtraId id, List<DependencyId> requires) {

    public ExtraDefinition {
        requires = List.copyOf(requires);
    }
}
