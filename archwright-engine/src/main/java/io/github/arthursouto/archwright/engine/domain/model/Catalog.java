package io.github.arthursouto.archwright.engine.domain.model;

import java.util.List;
import java.util.Optional;

public record Catalog(
        List<BootProfile> bootProfiles,
        List<DependencyDefinition> dependencies,
        List<ExtraDefinition> extras) {

    public Catalog {
        bootProfiles = List.copyOf(bootProfiles);
        dependencies = List.copyOf(dependencies);
        extras = List.copyOf(extras);
    }

    public Optional<BootProfile> profileFor(BootVersion version) {
        return bootProfiles.stream()
                .filter(profile -> profile.line().equals(version.line()))
                .findFirst();
    }

    public Optional<DependencyDefinition> dependency(DependencyId id) {
        return dependencies.stream()
                .filter(definition -> definition.id().equals(id))
                .findFirst();
    }

    public Optional<ExtraDefinition> extra(ExtraId id) {
        return extras.stream()
                .filter(definition -> definition.id().equals(id))
                .findFirst();
    }
}
