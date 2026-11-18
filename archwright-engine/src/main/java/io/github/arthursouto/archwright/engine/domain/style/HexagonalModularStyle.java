package io.github.arthursouto.archwright.engine.domain.style;

import io.github.arthursouto.archwright.engine.domain.model.ArchitectureStyle;

import java.util.Set;

public final class HexagonalModularStyle implements ArchitectureStyleStrategy {

    private static final Set<String> RESERVED_MODULE_NAMES = Set.of("shared", "bootstrap", "architecture");

    @Override
    public ArchitectureStyle style() {
        return ArchitectureStyle.HEXAGONAL_MODULAR;
    }

    @Override
    public boolean modulesRequired() {
        return true;
    }

    @Override
    public Set<String> reservedModuleNames() {
        return RESERVED_MODULE_NAMES;
    }
}
