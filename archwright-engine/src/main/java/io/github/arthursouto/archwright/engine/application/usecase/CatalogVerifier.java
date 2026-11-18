package io.github.arthursouto.archwright.engine.application.usecase;

import io.github.arthursouto.archwright.engine.application.port.out.CatalogRepository;
import io.github.arthursouto.archwright.engine.domain.kit.KitStrategy;
import io.github.arthursouto.archwright.engine.domain.kit.Kits;
import io.github.arthursouto.archwright.engine.domain.model.ArchitectureStyleDefinition;
import io.github.arthursouto.archwright.engine.domain.model.DependencyDefinition;
import io.github.arthursouto.archwright.engine.domain.model.DependencyId;
import io.github.arthursouto.archwright.engine.domain.style.ArchitectureStyles;

import java.util.Set;
import java.util.stream.Collectors;

public final class CatalogVerifier {

    private final CatalogRepository repository;
    private final Kits kits;
    private final ArchitectureStyles styles;

    public CatalogVerifier(CatalogRepository repository, Kits kits, ArchitectureStyles styles) {
        this.repository = repository;
        this.kits = kits;
        this.styles = styles;
    }

    public void verify() {
        verifyStyles();
        verifyKits();
    }

    private void verifyStyles() {
        repository.styles().stream()
                .map(ArchitectureStyleDefinition::style)
                .filter(style -> !styles.hasStrategyFor(style))
                .findFirst()
                .ifPresent(missing -> {
                    throw new IllegalStateException(
                            "catalog: style " + missing.name() + ": no strategy registered");
                });
    }

    private void verifyKits() {
        Set<DependencyId> known = repository.dependencies().stream()
                .map(DependencyDefinition::id)
                .collect(Collectors.toSet());

        kits.all().forEach(kit -> verifyKit(kit, known));
    }

    private void verifyKit(KitStrategy kit, Set<DependencyId> known) {
        kit.defaultDependencies().stream()
                .filter(dependency -> !known.contains(dependency))
                .findFirst()
                .ifPresent(missing -> {
                    throw new IllegalStateException(
                            "catalog: kit " + kit.id().value() + ": unknown dependency " + missing.value());
                });
    }
}
