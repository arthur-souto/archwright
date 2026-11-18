package io.github.arthursouto.archwright.engine.domain.validation;

import io.github.arthursouto.archwright.engine.domain.model.BootProfile;
import io.github.arthursouto.archwright.engine.domain.model.Catalog;
import io.github.arthursouto.archwright.engine.domain.model.DependencyDefinition;
import io.github.arthursouto.archwright.engine.domain.model.DependencyId;
import io.github.arthursouto.archwright.engine.domain.model.ExtraDefinition;
import io.github.arthursouto.archwright.engine.domain.model.ExtraId;

import java.util.List;
import java.util.Set;

final class TestCatalog {

    private static final Set<DependencyId> LEGACY_DEPENDENCIES = ids("web", "validation", "actuator", "jpa", "flyway", "lombok");
    private static final Set<DependencyId> ALL_DEPENDENCIES = ids(
            "web", "validation", "actuator", "jpa", "flyway", "lombok", "springdoc");

    private TestCatalog() {
    }

    static Catalog standard() {
        return new Catalog(
                List.of(
                        new BootProfile("4.1", 17, 25, false, ALL_DEPENDENCIES),
                        new BootProfile("3.5", 17, 24, false, ALL_DEPENDENCIES),
                        new BootProfile("3.2", 17, 21, false, ALL_DEPENDENCIES),
                        new BootProfile("2.7", 8, 21, true, LEGACY_DEPENDENCIES)),
                List.of(
                        definition("web"),
                        definition("validation"),
                        definition("actuator"),
                        definition("jpa"),
                        definition("flyway", "jpa"),
                        definition("lombok"),
                        definition("springdoc", "web")),
                List.of(
                        new ExtraDefinition(new ExtraId("docker-compose"), List.of(new DependencyId("jpa"))),
                        new ExtraDefinition(new ExtraId("github-actions"), List.of())));
    }

    private static DependencyDefinition definition(String id, String... implies) {
        return new DependencyDefinition(
                new DependencyId(id),
                java.util.Arrays.stream(implies).map(DependencyId::new).toList());
    }

    private static Set<DependencyId> ids(String... values) {
        return java.util.Arrays.stream(values).map(DependencyId::new).collect(java.util.stream.Collectors.toSet());
    }
}
