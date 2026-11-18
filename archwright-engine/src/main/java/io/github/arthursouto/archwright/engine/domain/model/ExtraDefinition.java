package io.github.arthursouto.archwright.engine.domain.model;

import java.util.List;

public record ExtraDefinition(ExtraId id, String label, List<DependencyId> requires) {

    public ExtraDefinition(ExtraId id, List<DependencyId> requires) {
        this(id, id.value(), requires);
    }

    public ExtraDefinition {
        requires = List.copyOf(requires);
    }
}
