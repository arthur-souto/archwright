package io.github.arthursouto.archwright.engine.domain.kit;

import io.github.arthursouto.archwright.engine.domain.model.DependencyId;
import io.github.arthursouto.archwright.engine.domain.model.KitId;

import java.util.List;

public final class BasicKit implements KitStrategy {

    private static final KitId ID = new KitId("BASIC");

    private static final List<DependencyId> DEPENDENCIES = List.of(
            new DependencyId("web"),
            new DependencyId("validation"),
            new DependencyId("actuator"),
            new DependencyId("jpa"),
            new DependencyId("flyway"),
            new DependencyId("lombok"));

    @Override
    public KitId id() {
        return ID;
    }

    @Override
    public List<DependencyId> defaultDependencies() {
        return DEPENDENCIES;
    }
}
