package io.github.arthursouto.archwright.engine.application.usecase;

import io.github.arthursouto.archwright.engine.domain.model.BootProfile;
import io.github.arthursouto.archwright.engine.domain.model.Catalog;
import io.github.arthursouto.archwright.engine.domain.model.DependencyDefinition;
import io.github.arthursouto.archwright.engine.domain.model.DependencyId;
import io.github.arthursouto.archwright.engine.domain.model.MavenCoordinate;
import io.github.arthursouto.archwright.engine.domain.model.ProjectSpec;

import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

final class DependencySection {

    private static final String TEST_SCOPE = "test";

    private static final MavenCoordinate BASE_STARTER =
            MavenCoordinate.of("org.springframework.boot", "spring-boot-starter");

    private static final MavenCoordinate ARCHUNIT =
            new MavenCoordinate("com.tngtech.archunit", "archunit-junit5", "${archunit.version}", TEST_SCOPE);

    private static final Comparator<MavenCoordinate> BY_GROUP_AND_ARTIFACT =
            Comparator.comparing(coordinate -> coordinate.groupId() + ":" + coordinate.artifactId());

    private final Catalog catalog;

    DependencySection(Catalog catalog) {
        this.catalog = catalog;
    }

    Map<String, Object> describe(ProjectSpec spec, BootProfile profile) {
        List<MavenCoordinate> main = sorted(withBaseStarter(profile.artifactsFor(spec.dependencies())));
        List<MavenCoordinate> test = sorted(testCoordinates(spec, profile));
        List<Map<String, Object>> processors = annotationProcessors(spec.dependencies());

        Map<String, Object> section = new LinkedHashMap<>();
        section.put("dependencies", views(main, ""));
        section.put("testDependencies", views(test, TEST_SCOPE));
        section.put("properties", properties(profile, main, test));
        section.put("annotationProcessors", processors);
        section.put("hasAnnotationProcessors", !processors.isEmpty());

        return section;
    }

    private List<MavenCoordinate> withBaseStarter(List<MavenCoordinate> coordinates) {
        boolean hasStarter = coordinates.stream().anyMatch(this::isSpringBootStarter);

        return hasStarter ? coordinates : Stream.concat(coordinates.stream(), Stream.of(BASE_STARTER)).toList();
    }

    private boolean isSpringBootStarter(MavenCoordinate coordinate) {
        return coordinate.groupId().equals(BASE_STARTER.groupId())
                && coordinate.artifactId().startsWith(BASE_STARTER.artifactId());
    }

    private List<MavenCoordinate> testCoordinates(ProjectSpec spec, BootProfile profile) {
        List<MavenCoordinate> test = profile.testArtifactsFor(spec.dependencies());

        if (!spec.fitnessFunctions()) {
            return test;
        }

        return Stream.concat(test.stream(), Stream.of(ARCHUNIT)).toList();
    }

    private List<MavenCoordinate> sorted(List<MavenCoordinate> coordinates) {
        return coordinates.stream().sorted(BY_GROUP_AND_ARTIFACT).toList();
    }

    private List<Map<String, Object>> views(List<MavenCoordinate> coordinates, String defaultScope) {
        return coordinates.stream().map(coordinate -> view(coordinate, defaultScope)).toList();
    }

    private Map<String, Object> view(MavenCoordinate coordinate, String defaultScope) {
        String scope = coordinate.scope() == null ? defaultScope : coordinate.scope();

        return Map.of(
                "groupId", coordinate.groupId(),
                "artifactId", coordinate.artifactId(),
                "version", orEmpty(coordinate.version()),
                "hasVersion", coordinate.version() != null,
                "scope", scope,
                "hasScope", !scope.isEmpty());
    }

    private List<Map<String, Object>> properties(
            BootProfile profile, List<MavenCoordinate> main, List<MavenCoordinate> test) {
        Set<String> referenced = Stream.concat(main.stream(), test.stream())
                .map(MavenCoordinate::versionProperty)
                .flatMap(Optional::stream)
                .collect(Collectors.toSet());

        return profile.pinnedVersions().entrySet().stream()
                .filter(entry -> referenced.contains(entry.getKey()))
                .sorted(Map.Entry.comparingByKey())
                .map(entry -> Map.<String, Object>of("name", entry.getKey(), "value", entry.getValue()))
                .toList();
    }

    private List<Map<String, Object>> annotationProcessors(List<DependencyId> selected) {
        return selected.stream()
                .map(catalog::dependency)
                .flatMap(Optional::stream)
                .map(DependencyDefinition::annotationProcessors)
                .flatMap(List::stream)
                .distinct()
                .map(this::processorView)
                .toList();
    }

    private Map<String, Object> processorView(MavenCoordinate processor) {
        return Map.of(
                "groupId", processor.groupId(),
                "artifactId", processor.artifactId(),
                "version", processor.version(),
                "versionProperty", processor.versionProperty().orElseThrow());
    }

    private String orEmpty(String value) {
        return value == null ? "" : value;
    }
}
