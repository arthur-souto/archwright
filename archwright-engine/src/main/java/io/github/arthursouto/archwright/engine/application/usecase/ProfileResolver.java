package io.github.arthursouto.archwright.engine.application.usecase;

import io.github.arthursouto.archwright.engine.domain.model.BootProfile;
import io.github.arthursouto.archwright.engine.domain.model.BootVersion;
import io.github.arthursouto.archwright.engine.domain.model.Catalog;
import io.github.arthursouto.archwright.engine.domain.model.DependencyId;
import io.github.arthursouto.archwright.engine.domain.model.MavenCoordinate;

import java.util.Collection;
import java.util.List;

public final class ProfileResolver {

    private final Catalog catalog;

    public ProfileResolver(Catalog catalog) {
        this.catalog = catalog;
    }

    public BootProfile profileFor(BootVersion version) {
        return catalog.profileFor(version)
                .orElseThrow(() -> new IllegalStateException("no boot profile for line " + version.line()));
    }

    public List<MavenCoordinate> artifactsFor(BootVersion version, Collection<DependencyId> dependencies) {
        return profileFor(version).artifactsFor(dependencies);
    }

    public List<MavenCoordinate> testArtifactsFor(BootVersion version, Collection<DependencyId> dependencies) {
        return profileFor(version).testArtifactsFor(dependencies);
    }
}
