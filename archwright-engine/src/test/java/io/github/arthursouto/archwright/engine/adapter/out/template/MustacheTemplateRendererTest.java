package io.github.arthursouto.archwright.engine.adapter.out.template;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class MustacheTemplateRendererTest {

    private final MustacheTemplateRenderer renderer = new MustacheTemplateRenderer();

    @Test
    void substitutesFlatAndDottedVariables() {
        Map<String, Object> context = Map.of("artifactId", "pedido-api", "boot", Map.of("version", "4.1.2"));

        assertThat(renderer.render("{{artifactId}} on {{boot.version}}", context)).isEqualTo("pedido-api on 4.1.2");
    }

    @Test
    @DisplayName("o contexto já vem escapado: o renderizador não escapa de novo")
    void doesNotEscapeHtmlAgain() {
        assertThat(renderer.render("{{x}}", Map.of("x", "API &lt;beta&gt; & <b>"))).isEqualTo("API &lt;beta&gt; & <b>");
    }

    @Test
    void rendersSectionsAndInvertedSections() {
        Map<String, Object> context = Map.of("dep", Map.of("jpa", true, "web", false));

        assertThat(renderer.render("{{#dep.jpa}}jpa{{/dep.jpa}}{{^dep.jpa}}no-jpa{{/dep.jpa}}", context)).isEqualTo("jpa");
        assertThat(renderer.render("{{#dep.web}}web{{/dep.web}}{{^dep.web}}no-web{{/dep.web}}", context)).isEqualTo("no-web");
    }

    @Test
    void supportsFlagNamesWithHyphens() {
        Map<String, Object> context = Map.of("extra", Map.of("docker-compose", true));

        assertThat(renderer.render("{{#extra.docker-compose}}yes{{/extra.docker-compose}}", context)).isEqualTo("yes");
    }

    @Test
    void iteratesListsWithTheItemScopeFirst() {
        Map<String, Object> context = Map.of(
                "artifactId", "app",
                "modules", List.of(Map.of("name", "pedidos"), Map.of("name", "pagamentos")));

        assertThat(renderer.render("{{#modules}}[{{name}}@{{artifactId}}]{{/modules}}", context))
                .isEqualTo("[pedidos@app][pagamentos@app]");
    }

    @Test
    @DisplayName("ENG-RND-01 #4 variável ausente é erro, nunca texto vazio")
    void failsOnMissingVariable() {
        assertThatThrownBy(() -> renderer.render("{{naoExiste}}", Map.of("a", "b")))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("naoExiste");
    }

    @Test
    void failsOnMissingNestedVariable() {
        assertThatThrownBy(() -> renderer.render("{{boot.missing}}", Map.of("boot", Map.of("version", "4.1.2"))))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("missing");
    }

    @Test
    void failsOnSectionOverMissingVariable() {
        assertThatThrownBy(() -> renderer.render("{{#dep.kafka}}x{{/dep.kafka}}", Map.of("dep", Map.of("jpa", true))))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("kafka");
    }

    @Test
    void trimsLinesThatContainOnlyASectionTag() {
        String template = "a\n{{#x}}\nb\n{{/x}}\nc\n";

        assertThat(renderer.render(template, Map.of("x", true))).isEqualTo("a\nb\nc\n");
        assertThat(renderer.render(template, Map.of("x", false))).isEqualTo("a\nc\n");
    }

    @Test
    void keepsSpringPlaceholdersAndComposeEscapesUntouched() {
        String template = "url: ${DATASOURCE_URL:jdbc:postgresql://localhost/{{db}}}\ncheck: $${POSTGRES_USER}\n";

        assertThat(renderer.render(template, Map.of("db", "pedido_api_db")))
                .isEqualTo("url: ${DATASOURCE_URL:jdbc:postgresql://localhost/pedido_api_db}\ncheck: $${POSTGRES_USER}\n");
    }

    @Test
    void keepsLineEndingsOfTheTemplate() {
        assertThat(renderer.render("a\r\nb\r\n", Map.of())).isEqualTo("a\r\nb\r\n");
    }
}
