package io.github.arthursouto.archwright.engine.domain.model;

import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class TemplateContextTest {

    private final TemplateContext context = new TemplateContext(Map.of(
            "artifactId", "pedido-api",
            "dep", Map.of("jpa", true, "web", false),
            "boot", Map.of("version", "4.1.2")));

    private static TemplateContext contextWithLists() {
        return new TemplateContext(Map.of(
                "artifactId", "pedido-api",
                "dep", Map.of("jpa", true),
                "boot", Map.of("version", "4.1.2"),
                "modules", List.of(Map.of("name", "pedidos"), Map.of("name", "pagamentos")),
                "noModules", List.of(),
                "texts", List.of("a", "b")));
    }

    @Test
    void evaluatesConditionsWithOptionalNegation() {
        assertThat(context.satisfies("dep.jpa")).isTrue();
        assertThat(context.satisfies("dep.web")).isFalse();
        assertThat(context.satisfies("!dep.web")).isTrue();
        assertThat(context.satisfies("!dep.jpa")).isFalse();
    }

    @Test
    void rejectsConditionsOverNamesThatAreNotFlags() {
        assertThatThrownBy(() -> context.satisfies("dep.kafka")).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> context.satisfies("!artifactId")).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void findsTopLevelAndNestedValues() {
        assertThat(context.find("artifactId")).contains("pedido-api");
        assertThat(context.find("boot.version")).contains("4.1.2");
        assertThat(context.find("dep.jpa")).contains(true);
    }

    @Test
    void findsNothingForUnknownPaths() {
        assertThat(context.find("missing")).isEmpty();
        assertThat(context.find("boot.missing")).isEmpty();
        assertThat(context.find("artifactId.deeper")).isEmpty();
    }

    @Test
    void readsFlagsByPath() {
        assertThat(context.flag("dep.jpa")).isTrue();
        assertThat(context.flag("dep.web")).isFalse();
    }

    @Test
    void refusesFlagsThatDoNotExistOrAreNotBooleans() {
        assertThatThrownBy(() -> context.flag("dep.kafka"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("dep.kafka");
        assertThatThrownBy(() -> context.flag("artifactId")).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void readsTextsByPath() {
        assertThat(contextWithLists().text("boot.version")).isEqualTo("4.1.2");
        assertThatThrownBy(() -> contextWithLists().text("dep.jpa")).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> contextWithLists().text("missing")).hasMessageContaining("missing");
    }

    @Test
    void readsNestedMapsAsSections() {
        TemplateContext boot = contextWithLists().section("boot");

        assertThat(boot.text("version")).isEqualTo("4.1.2");
        assertThatThrownBy(() -> contextWithLists().section("artifactId")).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> contextWithLists().section("missing")).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void readsListsOfMapsAsSections() {
        List<TemplateContext> modules = contextWithLists().sections("modules");

        assertThat(modules).extracting(module -> module.text("name")).containsExactly("pedidos", "pagamentos");
        assertThat(contextWithLists().sections("noModules")).isEmpty();
    }

    @Test
    void refusesSectionsThatAreNotListsOfMaps() {
        assertThatThrownBy(() -> contextWithLists().sections("boot")).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> contextWithLists().sections("missing")).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> contextWithLists().sections("texts"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("texts");
    }

    @Test
    void doesNotChangeWhenTheSourceMapChanges() {
        Map<String, Object> source = new HashMap<>(Map.of("a", "1"));
        TemplateContext copy = new TemplateContext(source);

        source.put("b", "2");

        assertThat(copy.find("b")).isEmpty();
    }
}
