package io.github.arthursouto.archwright.engine.domain.model;

import java.util.List;

public record ProjectSpec(
        ProjectIdentity identity,
        Architecture architecture,
        BootVersion bootVersion,
        JavaVersion javaVersion,
        KitId kit,
        List<DependencyId> dependencies,
        List<ExtraId> extras,
        boolean fitnessFunctions) {

    public ProjectSpec {
        dependencies = List.copyOf(dependencies);
        extras = List.copyOf(extras);
    }
}
