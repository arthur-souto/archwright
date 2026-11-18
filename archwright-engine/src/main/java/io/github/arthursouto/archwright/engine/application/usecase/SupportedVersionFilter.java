package io.github.arthursouto.archwright.engine.application.usecase;

import io.github.arthursouto.archwright.engine.domain.model.BootVersion;
import io.github.arthursouto.archwright.engine.domain.model.Catalog;

import java.util.Comparator;
import java.util.List;
import java.util.Optional;

final class SupportedVersionFilter {

    private final Catalog catalog;

    SupportedVersionFilter(Catalog catalog) {
        this.catalog = catalog;
    }

    List<BootVersion> select(List<String> rawVersions) {
        return rawVersions.stream()
                .map(BootVersion::parse)
                .flatMap(Optional::stream)
                .filter(version -> version.compareTo(BootVersion.MINIMUM_SUPPORTED) >= 0)
                .filter(version -> catalog.profileFor(version).isPresent())
                .distinct()
                .sorted(Comparator.reverseOrder())
                .toList();
    }
}
