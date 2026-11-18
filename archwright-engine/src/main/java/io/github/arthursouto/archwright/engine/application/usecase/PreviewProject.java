package io.github.arthursouto.archwright.engine.application.usecase;

import io.github.arthursouto.archwright.engine.application.port.in.PreviewProjectQuery;
import io.github.arthursouto.archwright.engine.application.port.in.PreviewResult;
import io.github.arthursouto.archwright.engine.application.port.in.ProjectRequest;
import io.github.arthursouto.archwright.engine.application.port.in.ResolvedDependency;
import io.github.arthursouto.archwright.engine.domain.model.BootProfile;
import io.github.arthursouto.archwright.engine.domain.model.DependencyId;
import io.github.arthursouto.archwright.engine.domain.model.MavenCoordinate;
import io.github.arthursouto.archwright.engine.domain.model.ProjectSpec;

import java.util.List;
import java.util.stream.Stream;

public final class PreviewProject implements PreviewProjectQuery {

    private static final String DEFAULT_SCOPE = "compile";

    private final ProjectPlanning planning;
    private final ProfileResolver resolver;

    public PreviewProject(ProjectPlanning planning, ProfileResolver resolver) {
        this.planning = planning;
        this.resolver = resolver;
    }

    @Override
    public PreviewResult preview(ProjectRequest request) {
        return switch (planning.prepare(request.toDraft())) {
            case ProjectPlanning.Outcome.Rejected rejected -> new PreviewResult.Failure(rejected.violations());
            case ProjectPlanning.Outcome.Ready ready -> preview(ready);
        };
    }

    private PreviewResult preview(ProjectPlanning.Outcome.Ready ready) {
        return new PreviewResult.Preview(
                paths(ready),
                dependencies(ready.spec()),
                List.of(),
                ready.warnings());
    }

    private List<String> paths(ProjectPlanning.Outcome.Ready ready) {
        return ready.plan().targets().stream().sorted().toList();
    }

    private List<ResolvedDependency> dependencies(ProjectSpec spec) {
        BootProfile profile = resolver.profileFor(spec.bootVersion());

        return spec.dependencies().stream()
                .flatMap(dependency -> resolved(dependency, profile))
                .toList();
    }

    private Stream<ResolvedDependency> resolved(DependencyId dependency, BootProfile profile) {
        return profile.artifactsFor(List.of(dependency)).stream()
                .map(coordinate -> resolvedDependency(dependency, coordinate, profile));
    }

    private ResolvedDependency resolvedDependency(DependencyId dependency, MavenCoordinate coordinate, BootProfile profile) {
        return new ResolvedDependency(
                dependency.value(),
                coordinate.groupId(),
                coordinate.artifactId(),
                version(coordinate, profile),
                coordinate.scope() == null ? DEFAULT_SCOPE : coordinate.scope());
    }

    private String version(MavenCoordinate coordinate, BootProfile profile) {
        return coordinate.versionProperty()
                .map(property -> profile.pinnedVersions().get(property))
                .orElse(coordinate.version());
    }
}
