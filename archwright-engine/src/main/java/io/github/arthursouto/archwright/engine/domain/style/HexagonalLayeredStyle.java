package io.github.arthursouto.archwright.engine.domain.style;

import io.github.arthursouto.archwright.engine.domain.model.ArchitectureStyle;

import java.util.Set;

public final class HexagonalLayeredStyle implements ArchitectureStyleStrategy {

    @Override
    public ArchitectureStyle style() {
        return ArchitectureStyle.HEXAGONAL_LAYERED;
    }

    @Override
    public boolean modulesRequired() {
        return false;
    }

    @Override
    public Set<String> reservedModuleNames() {
        return Set.of();
    }
}
