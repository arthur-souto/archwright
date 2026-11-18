package io.github.arthursouto.archwright.engine.domain.model;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

public final class TestProfiles {

    private TestProfiles() {
    }

    public static BootProfile simple(String line, int javaMin, int javaMaxTested, boolean legacy, String... dependencyIds) {
        Map<DependencyId, List<MavenCoordinate>> artifacts = Arrays.stream(dependencyIds)
                .map(DependencyId::new)
                .collect(Collectors.toMap(Function.identity(), id -> List.of()));

        return new BootProfile(
                line, javaMin, javaMaxTested, legacy,
                PersistenceNamespace.JAKARTA, TestStrategy.SINGLE_STARTER,
                artifacts, Map.of(), Map.of());
    }
}
