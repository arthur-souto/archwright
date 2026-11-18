package io.github.arthursouto.archwright.engine.domain.validation;

import io.github.arthursouto.archwright.engine.domain.model.BootProfile;
import io.github.arthursouto.archwright.engine.domain.model.Catalog;
import io.github.arthursouto.archwright.engine.domain.model.DependencyDefinition;
import io.github.arthursouto.archwright.engine.domain.model.DependencyId;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Stream;

final class DependencyValidator {

    private static final String DEPENDENCIES = "dependencies";

    private final Catalog catalog;

    DependencyValidator(Catalog catalog) {
        this.catalog = catalog;
    }

    Checked<List<DependencyId>> validate(ProjectDraft draft, Checked<ResolvedBoot> boot) {
        List<Violation> unknown = unknownViolations(draft.dependencies());
        Expansion expansion = expand(knownDistinct(draft.dependencies()));
        List<Violation> unavailable = unavailableViolations(expansion.dependencies(), boot);

        List<Violation> violations = Stream.concat(unknown.stream(), unavailable.stream()).toList();

        if (!violations.isEmpty()) {
            return Checked.invalid(violations);
        }

        return Checked.valid(expansion.dependencies(), expansion.warnings());
    }

    private List<Violation> unknownViolations(List<String> rawIds) {
        return rawIds.stream()
                .filter(raw -> findDefinition(raw).isEmpty())
                .map(raw -> Violation.of(ViolationCode.UNKNOWN_DEPENDENCY, DEPENDENCIES, "id", raw.trim()))
                .distinct()
                .toList();
    }

    private List<DependencyId> knownDistinct(List<String> rawIds) {
        return rawIds.stream()
                .map(this::findDefinition)
                .flatMap(Optional::stream)
                .map(DependencyDefinition::id)
                .distinct()
                .toList();
    }

    private Optional<DependencyDefinition> findDefinition(String raw) {
        return DependencyId.parse(raw).flatMap(catalog::dependency);
    }

    private Expansion expand(List<DependencyId> requested) {
        Set<DependencyId> resolved = new LinkedHashSet<>(requested);
        List<Warning> added = new ArrayList<>();
        Deque<DependencyId> pending = new ArrayDeque<>(requested);

        while (!pending.isEmpty()) {
            DependencyId current = pending.poll();

            for (DependencyId implied : impliedBy(current)) {
                if (resolved.add(implied)) {
                    added.add(addedWarning(implied, current));
                    pending.add(implied);
                }
            }
        }

        return new Expansion(List.copyOf(resolved), added);
    }

    private List<DependencyId> impliedBy(DependencyId dependency) {
        return catalog.dependency(dependency)
                .map(DependencyDefinition::implies)
                .orElse(List.of());
    }

    private Warning addedWarning(DependencyId added, DependencyId by) {
        return new Warning(WarningCode.DEPENDENCY_ADDED, Map.of("added", added.value(), "by", by.value()));
    }

    private List<Violation> unavailableViolations(List<DependencyId> dependencies, Checked<ResolvedBoot> boot) {
        if (!boot.isValid()) {
            return List.of();
        }

        BootProfile profile = boot.value().profile();

        return dependencies.stream()
                .filter(dependency -> !profile.isAvailable(dependency))
                .map(dependency -> notAvailable(dependency, boot.value()))
                .toList();
    }

    private Violation notAvailable(DependencyId dependency, ResolvedBoot boot) {
        return new Violation(
                ViolationCode.DEPENDENCY_NOT_AVAILABLE,
                DEPENDENCIES,
                Map.of("id", dependency.value(), "boot", boot.version().toString()));
    }

    private record Expansion(List<DependencyId> dependencies, List<Warning> warnings) {
    }
}
