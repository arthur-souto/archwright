package io.github.arthursouto.archwright.engine.adapter.out.catalog;

import io.github.arthursouto.archwright.engine.domain.model.BootProfile;
import io.github.arthursouto.archwright.engine.domain.model.DependencyDefinition;
import io.github.arthursouto.archwright.engine.domain.model.DependencyId;
import io.github.arthursouto.archwright.engine.domain.model.ExtraDefinition;

import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

final class CatalogConsistency {

    void check(List<BootProfile> profiles, List<DependencyDefinition> dependencies, List<ExtraDefinition> extras) {
        Set<DependencyId> known = dependencies.stream()
                .map(DependencyDefinition::id)
                .collect(Collectors.toSet());

        profiles.forEach(profile -> requireKnown(
                BootProfileParser.FILE, "profile " + profile.line() + " artifacts", profile.availableDependencies(), known));
        dependencies.forEach(dependency -> requireKnown(
                DependencyParser.FILE, "dependency " + dependency.id().value() + " implies", dependency.implies(), known));
        extras.forEach(extra -> requireKnown(
                ExtraParser.FILE, "extra " + extra.id().value() + " requires", extra.requires(), known));
    }

    private void requireKnown(String file, String owner, Collection<DependencyId> referenced, Set<DependencyId> known) {
        referenced.stream()
                .filter(id -> !known.contains(id))
                .min(Comparator.comparing(DependencyId::value))
                .ifPresent(unknown -> {
                    throw CatalogChecks.problem(file, owner + " unknown dependency " + unknown.value());
                });
    }
}
