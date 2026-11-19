package io.github.arthursouto.archwright.engine.application.usecase;

import io.github.arthursouto.archwright.engine.application.port.in.ResolvedDependency;
import io.github.arthursouto.archwright.engine.domain.model.BootProfile;
import io.github.arthursouto.archwright.engine.domain.model.DependencyId;
import io.github.arthursouto.archwright.engine.domain.model.MavenCoordinate;

import java.util.List;

final class DependencyArtifacts {

    private static final String DEFAULT_SCOPE = "compile";

    private DependencyArtifacts() {
    }

    static List<ResolvedDependency> of(DependencyId dependency, BootProfile profile) {
        return profile.artifactsFor(List.of(dependency)).stream()
                .map(coordinate -> resolved(dependency, coordinate, profile))
                .toList();
    }

    private static ResolvedDependency resolved(DependencyId dependency, MavenCoordinate coordinate, BootProfile profile) {
        return new ResolvedDependency(
                dependency.value(),
                coordinate.groupId(),
                coordinate.artifactId(),
                version(coordinate, profile),
                coordinate.scope() == null ? DEFAULT_SCOPE : coordinate.scope());
    }

    private static String version(MavenCoordinate coordinate, BootProfile profile) {
        return coordinate.versionProperty()
                .map(property -> profile.pinnedVersions().get(property))
                .orElse(coordinate.version());
    }
}
