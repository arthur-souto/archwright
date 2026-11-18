package io.github.arthursouto.archwright.engine.domain.model;

import java.util.List;

public record Architecture(ArchitectureStyle style, List<ModuleName> modules) {

    public Architecture {
        modules = List.copyOf(modules);
    }
}
