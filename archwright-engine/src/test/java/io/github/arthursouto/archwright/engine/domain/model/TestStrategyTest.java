package io.github.arthursouto.archwright.engine.domain.model;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class TestStrategyTest {

    private static final MavenCoordinate TEST_STARTER = MavenCoordinate.of("org.springframework.boot", "spring-boot-starter-test");
    private static final MavenCoordinate WEB_TEST = MavenCoordinate.of("org.springframework.boot", "spring-boot-starter-webmvc-test");

    @Test
    void parsesExactNames() {
        assertThat(TestStrategy.parse("SINGLE_STARTER")).contains(TestStrategy.SINGLE_STARTER);
        assertThat(TestStrategy.parse(" PER_TECHNOLOGY ")).contains(TestStrategy.PER_TECHNOLOGY);
    }

    @ParameterizedTest
    @ValueSource(strings = {"", "single_starter", "BOTH"})
    void rejectsOtherNames(String raw) {
        assertThat(TestStrategy.parse(raw)).isEmpty();
    }

    @Test
    void singleStarterAlwaysStartsWithTheBaseStarter() {
        assertThat(TestStrategy.SINGLE_STARTER.resolve(List.of())).containsExactly(TEST_STARTER);
        assertThat(TestStrategy.SINGLE_STARTER.resolve(List.of(WEB_TEST))).containsExactly(TEST_STARTER, WEB_TEST);
    }

    @Test
    void perTechnologyUsesTheBaseStarterOnlyWhenThereIsNothingElse() {
        assertThat(TestStrategy.PER_TECHNOLOGY.resolve(List.of())).containsExactly(TEST_STARTER);
        assertThat(TestStrategy.PER_TECHNOLOGY.resolve(List.of(WEB_TEST))).containsExactly(WEB_TEST);
    }

    @Test
    void parsesPersistenceNamespaces() {
        assertThat(PersistenceNamespace.parse("javax")).contains(PersistenceNamespace.JAVAX);
        assertThat(PersistenceNamespace.parse("jakarta")).contains(PersistenceNamespace.JAKARTA);
        assertThat(PersistenceNamespace.parse("JAKARTA")).isEmpty();
        assertThat(PersistenceNamespace.JAKARTA.prefix()).isEqualTo("jakarta");
    }
}
