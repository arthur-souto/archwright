package io.github.arthursouto.archwright.engine.adapter.out.catalog;

import io.github.arthursouto.archwright.engine.domain.model.FitnessRuleDefinition;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Path;
import java.util.List;

import static io.github.arthursouto.archwright.engine.adapter.out.catalog.CatalogFixture.FITNESS;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class FitnessRuleCatalogTest {

    @TempDir
    Path root;

    @Test
    void loadsClassesWithTheirConditionsAndRules() throws IOException {
        YamlCatalogRepository repository = CatalogFixture.valid().with(FITNESS, """
                - testClass: AdaptersTest
                  template: AdaptersTest.java.mustache
                  when: [dep.web, "!dep.security"]
                  rules:
                    - id: FF-ADP-01
                      description: Uma.
                    - id: FF-ADP-02
                      description: Duas.
                """).load(root);

        FitnessRuleDefinition definition = repository.fitnessRules().getFirst();

        assertThat(definition.testClass()).isEqualTo("AdaptersTest");
        assertThat(definition.template()).isEqualTo("AdaptersTest.java.mustache");
        assertThat(definition.when()).containsExactly("dep.web", "!dep.security");
        assertThat(definition.rules()).extracting(rule -> rule.id()).containsExactly("FF-ADP-01", "FF-ADP-02");
    }

    @Test
    void defaultsToNoConditions() throws IOException {
        assertThat(CatalogFixture.valid().load(root).fitnessRules().getFirst().when()).isEmpty();
    }

    @Test
    void rejectsADuplicatedRuleId() {
        assertThatThrownBy(() -> load(entry("A", "FF-DOM-01") + entry("B", "FF-DOM-01")))
                .hasMessageContaining("fitness-rules.yaml")
                .hasMessageContaining("FF-DOM-01");
    }

    @Test
    void rejectsADuplicatedTestClass() {
        assertThatThrownBy(() -> load(entry("A", "FF-DOM-01") + entry("A", "FF-DOM-02")))
                .hasMessageContaining("fitness-rules.yaml")
                .hasMessageContaining("duplicated test class A");
    }

    @Test
    void rejectsAMalformedRuleId() {
        assertThatThrownBy(() -> load(entry("A", "DOM-1")))
                .hasMessageContaining("fitness-rules.yaml")
                .hasMessageContaining("DOM-1");
    }

    @Test
    void rejectsAClassWithoutRules() {
        assertThatThrownBy(() -> load("""
                - testClass: A
                  template: A.java.mustache
                  rules: []
                """))
                .hasMessageContaining("fitness-rules.yaml")
                .hasMessageContaining("at least one rule");
    }

    @Test
    void rejectsAnInvalidClassName() {
        assertThatThrownBy(() -> load(entry("a-test", "FF-DOM-01")))
                .hasMessageContaining("invalid test class name 'a-test'");
    }

    @Test
    void rejectsAMissingFile() {
        assertThatThrownBy(() -> CatalogFixture.valid().without(FITNESS).load(root))
                .hasMessageContaining("fitness-rules.yaml");
    }

    private List<FitnessRuleDefinition> load(String content) throws IOException {
        return CatalogFixture.valid().with(FITNESS, content).load(root).fitnessRules();
    }

    private static String entry(String testClass, String ruleId) {
        return """
                - testClass: %s
                  template: %s.java.mustache
                  rules:
                    - id: %s
                      description: Regra.
                """.formatted(testClass, testClass, ruleId);
    }
}
