package io.github.arthursouto.archwright.engine.domain.model;

import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class CatalogTest {

    private static final DependencyId WEB = new DependencyId("web");

    private final BootProfile profile35 = TestProfiles.simple("3.5", 17, 24, false, "web");
    private final BootProfile profile41 = TestProfiles.simple("4.1", 17, 25, false, "web");
    private final DependencyDefinition web = new DependencyDefinition(WEB, List.of());
    private final ExtraDefinition dockerCompose = new ExtraDefinition(new ExtraId("docker-compose"), List.of());

    private final Catalog catalog = new Catalog(List.of(profile41, profile35), List.of(web), List.of(dockerCompose));

    @Test
    void findsProfileByTheLineOfTheVersion() {
        assertThat(catalog.profileFor(new BootVersion(3, 5, 3))).contains(profile35);
        assertThat(catalog.profileFor(new BootVersion(4, 1, 0))).contains(profile41);
    }

    @Test
    void returnsEmptyForLineOutsideTheCatalog() {
        assertThat(catalog.profileFor(new BootVersion(9, 0, 0))).isEmpty();
    }

    @Test
    void findsDependencyAndExtraById() {
        assertThat(catalog.dependency(WEB)).contains(web);
        assertThat(catalog.extra(new ExtraId("docker-compose"))).contains(dockerCompose);
    }

    @Test
    void returnsEmptyForUnknownDependencyAndExtra() {
        assertThat(catalog.dependency(new DependencyId("kafka"))).isEmpty();
        assertThat(catalog.extra(new ExtraId("terraform"))).isEmpty();
    }

    @Test
    void isImmutableAfterConstruction() {
        List<BootProfile> source = new ArrayList<>(List.of(profile35));
        Catalog copy = new Catalog(source, List.of(), List.of());

        source.add(profile41);

        assertThat(copy.bootProfiles()).containsExactly(profile35);
        assertThatThrownBy(() -> copy.bootProfiles().add(profile41)).isInstanceOf(UnsupportedOperationException.class);
    }
}
