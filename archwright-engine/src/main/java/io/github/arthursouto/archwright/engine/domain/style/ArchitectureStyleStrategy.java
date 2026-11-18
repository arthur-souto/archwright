package io.github.arthursouto.archwright.engine.domain.style;

import io.github.arthursouto.archwright.engine.domain.model.ArchitectureStyle;

import java.util.Set;

public interface ArchitectureStyleStrategy {

    ArchitectureStyle style();

    boolean modulesRequired();

    Set<String> reservedModuleNames();
}
