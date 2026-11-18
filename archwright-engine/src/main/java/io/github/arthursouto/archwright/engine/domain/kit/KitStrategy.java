package io.github.arthursouto.archwright.engine.domain.kit;

import io.github.arthursouto.archwright.engine.domain.model.DependencyId;
import io.github.arthursouto.archwright.engine.domain.model.KitId;

import java.util.List;

public interface KitStrategy {

    KitId id();

    String label();

    List<DependencyId> defaultDependencies();
}
