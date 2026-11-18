package io.github.arthursouto.archwright.engine.domain.style;

import io.github.arthursouto.archwright.engine.domain.model.ArchitectureStyle;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

public final class ArchitectureStyles {

    private final Map<ArchitectureStyle, ArchitectureStyleStrategy> strategies;

    public ArchitectureStyles(Collection<ArchitectureStyleStrategy> strategies) {
        this.strategies = strategies.stream()
                .collect(Collectors.toUnmodifiableMap(ArchitectureStyleStrategy::style, Function.identity()));
    }

    public static ArchitectureStyles standard() {
        return new ArchitectureStyles(List.of(new HexagonalModularStyle(), new HexagonalLayeredStyle()));
    }

    public boolean hasStrategyFor(ArchitectureStyle style) {
        return strategies.containsKey(style);
    }

    public ArchitectureStyleStrategy strategyFor(ArchitectureStyle style) {
        ArchitectureStyleStrategy strategy = strategies.get(style);

        if (strategy == null) {
            throw new IllegalStateException("no strategy registered for style " + style);
        }

        return strategy;
    }
}
