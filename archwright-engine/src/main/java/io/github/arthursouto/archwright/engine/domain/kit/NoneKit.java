package io.github.arthursouto.archwright.engine.domain.kit;

import io.github.arthursouto.archwright.engine.domain.model.DependencyId;
import io.github.arthursouto.archwright.engine.domain.model.KitId;

import java.util.List;

public final class NoneKit implements KitStrategy {

    private static final KitId ID = new KitId("NONE");

    @Override
    public KitId id() {
        return ID;
    }

    @Override
    public List<DependencyId> defaultDependencies() {
        return List.of();
    }
}
