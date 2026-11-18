package io.github.arthursouto.archwright.engine.domain.model;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public record BootProfile(
        String line,
        int javaMin,
        int javaMaxTested,
        boolean legacy,
        PersistenceNamespace persistenceNamespace,
        TestStrategy testStrategy,
        Map<DependencyId, List<MavenCoordinate>> artifacts,
        Map<DependencyId, List<MavenCoordinate>> testArtifacts,
        Map<String, String> pinnedVersions) {

    public BootProfile {
        artifacts = immutableCopy(artifacts);
        testArtifacts = immutableCopy(testArtifacts);
        pinnedVersions = Map.copyOf(pinnedVersions);

        requireJavaRange(line, javaMin, javaMaxTested);
        requireTestArtifactsOnlyForAvailableDependencies(line, artifacts, testArtifacts);
        requireDefinedVersionProperties(line, artifacts, testArtifacts, pinnedVersions);
    }

    public Set<DependencyId> availableDependencies() {
        return artifacts.keySet();
    }

    public boolean isAvailable(DependencyId dependency) {
        return artifacts.containsKey(dependency);
    }

    public boolean supports(JavaVersion java) {
        return java.value() >= javaMin;
    }

    public boolean isTested(JavaVersion java) {
        return java.value() <= javaMaxTested;
    }

    public List<MavenCoordinate> artifactsFor(Collection<DependencyId> dependencies) {
        return coordinatesOf(artifacts, dependencies);
    }

    public List<MavenCoordinate> testArtifactsFor(Collection<DependencyId> dependencies) {
        return testStrategy.resolve(coordinatesOf(testArtifacts, dependencies));
    }

    private static List<MavenCoordinate> coordinatesOf(
            Map<DependencyId, List<MavenCoordinate>> source, Collection<DependencyId> dependencies) {
        return dependencies.stream()
                .flatMap(dependency -> source.getOrDefault(dependency, List.of()).stream())
                .distinct()
                .toList();
    }

    private static Map<DependencyId, List<MavenCoordinate>> immutableCopy(
            Map<DependencyId, List<MavenCoordinate>> source) {
        return source.entrySet().stream()
                .collect(Collectors.toUnmodifiableMap(Map.Entry::getKey, entry -> List.copyOf(entry.getValue())));
    }

    private static void requireJavaRange(String line, int javaMin, int javaMaxTested) {
        if (javaMin > javaMaxTested) {
            throw new IllegalArgumentException(
                    "boot profile " + line + ": javaMin " + javaMin + " is greater than javaMaxTested " + javaMaxTested);
        }
    }

    private static void requireTestArtifactsOnlyForAvailableDependencies(
            String line,
            Map<DependencyId, List<MavenCoordinate>> artifacts,
            Map<DependencyId, List<MavenCoordinate>> testArtifacts) {
        testArtifacts.keySet().stream()
                .filter(dependency -> !artifacts.containsKey(dependency))
                .map(DependencyId::value)
                .sorted()
                .findFirst()
                .ifPresent(orphan -> {
                    throw new IllegalArgumentException(
                            "boot profile " + line + ": testArtifacts for " + orphan + " without artifacts");
                });
    }

    private static void requireDefinedVersionProperties(
            String line,
            Map<DependencyId, List<MavenCoordinate>> artifacts,
            Map<DependencyId, List<MavenCoordinate>> testArtifacts,
            Map<String, String> pinnedVersions) {
        Stream.concat(artifacts.values().stream(), testArtifacts.values().stream())
                .flatMap(List::stream)
                .map(MavenCoordinate::versionProperty)
                .flatMap(Optional::stream)
                .filter(property -> !pinnedVersions.containsKey(property))
                .sorted()
                .findFirst()
                .ifPresent(property -> {
                    throw new IllegalArgumentException(
                            "boot profile " + line + ": undefined version property " + property);
                });
    }
}
