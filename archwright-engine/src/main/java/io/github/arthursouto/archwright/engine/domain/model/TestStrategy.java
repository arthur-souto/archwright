package io.github.arthursouto.archwright.engine.domain.model;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

public enum TestStrategy {
    SINGLE_STARTER,
    PER_TECHNOLOGY;

    private static final MavenCoordinate TEST_STARTER =
            MavenCoordinate.of("org.springframework.boot", "spring-boot-starter-test");

    public static Optional<TestStrategy> parse(String raw) {
        String id = raw.trim();

        return Arrays.stream(values())
                .filter(strategy -> strategy.name().equals(id))
                .findFirst();
    }

    public List<MavenCoordinate> resolve(List<MavenCoordinate> perDependency) {
        return switch (this) {
            case SINGLE_STARTER -> withTestStarter(perDependency);
            case PER_TECHNOLOGY -> perDependency.isEmpty() ? List.of(TEST_STARTER) : perDependency;
        };
    }

    private static List<MavenCoordinate> withTestStarter(List<MavenCoordinate> perDependency) {
        List<MavenCoordinate> all = new ArrayList<>();
        all.add(TEST_STARTER);
        all.addAll(perDependency);

        return List.copyOf(all);
    }
}
