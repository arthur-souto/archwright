package io.github.arthursouto.archwright.engine.application.port.in;

import java.util.List;

public record DependencyOption(
        String id,
        String label,
        String description,
        String group,
        List<String> implies,
        List<ResolvedDependency> artifacts) {

    public DependencyOption {
        implies = List.copyOf(implies);
        artifacts = List.copyOf(artifacts);
    }
}
