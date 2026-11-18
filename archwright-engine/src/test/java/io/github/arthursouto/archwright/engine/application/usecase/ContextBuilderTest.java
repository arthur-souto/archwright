package io.github.arthursouto.archwright.engine.application.usecase;

import io.github.arthursouto.archwright.engine.domain.kit.Kits;
import io.github.arthursouto.archwright.engine.domain.model.TemplateContext;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.tuple;

class ContextBuilderTest {

    private final ContextBuilder builder = new ContextBuilder(
            new ProfileResolver(SpecBuilder.catalog()),
            SpecBuilder.catalog(),
            SpecBuilder.repository().styles(),
            Kits.standard());

    @Test
    @DisplayName("ENG-CTX-01 #1 pacote base e caminho do pacote")
    void exposesBasePackageAndItsPath() {
        TemplateContext context = builder.build(SpecBuilder.basic().build());

        assertThat(context.text("basePackage")).isEqualTo("com.acme.pedidoapi");
        assertThat(context.text("basePackagePath")).isEqualTo("com/acme/pedidoapi");
        assertThat(context.text("applicationClass")).isEqualTo("PedidoApiApplication");
        assertThat(context.text("artifactId")).isEqualTo("pedido-api");
        assertThat(context.text("groupId")).isEqualTo("com.acme");
    }

    @Test
    @DisplayName("ENG-CTX-01 #2 Boot 4.1 com kit BASIC: dependências ordenadas, com os artefatos do perfil")
    void listsProfileArtifactsSortedByGroupAndArtifact() {
        TemplateContext context = builder.build(SpecBuilder.basic().build());

        assertThat(coordinates(context, "dependencies")).containsExactly(
                "org.flywaydb:flyway-database-postgresql",
                "org.postgresql:postgresql",
                "org.projectlombok:lombok",
                "org.springframework.boot:spring-boot-starter-actuator",
                "org.springframework.boot:spring-boot-starter-data-jpa",
                "org.springframework.boot:spring-boot-starter-flyway",
                "org.springframework.boot:spring-boot-starter-validation",
                "org.springframework.boot:spring-boot-starter-webmvc");
        assertThat(context.flag("dep.jpa")).isTrue();
        assertThat(context.flag("dep.security")).isFalse();
    }

    @Test
    @DisplayName("ENG-CTX-01 #3 Java 8 vira 1.8 e Java 21 continua 21")
    void convertsJavaEightToLegacyProperty() {
        TemplateContext legacy = builder.build(SpecBuilder.basic().boot("2.7.18", "8").build());
        TemplateContext modern = builder.build(SpecBuilder.basic().boot("3.5.3", "21").build());

        assertThat(legacy.text("java.property")).isEqualTo("1.8");
        assertThat(legacy.text("java.version")).isEqualTo("8");
        assertThat(modern.text("java.property")).isEqualTo("21");
    }

    @Test
    @DisplayName("ENG-CTX-01 #4 descrição escapada para XML e Markdown")
    void escapesDescriptionForXmlAndMarkdown() {
        TemplateContext context = builder.build(SpecBuilder.basic().description("API <beta> & cia").build());

        assertThat(context.text("descriptionXml")).isEqualTo("API &lt;beta&gt; &amp; cia");
        assertThat(context.text("descriptionMarkdown")).isEqualTo("API <beta> & cia");
        assertThat(context.flag("hasDescription")).isTrue();
    }

    @Test
    @DisplayName("ENG-CTX-01 #4 Markdown protege asteriscos e sublinhados")
    void escapesMarkdownEmphasisCharacters() {
        TemplateContext context = builder.build(SpecBuilder.basic().description("*forte* e snake_case").build());

        assertThat(context.text("descriptionMarkdown")).isEqualTo("\\*forte\\* e snake\\_case");
    }

    @Test
    void marksMissingDescription() {
        TemplateContext context = builder.build(SpecBuilder.basic().build());

        assertThat(context.flag("hasDescription")).isFalse();
        assertThat(context.text("descriptionXml")).isEmpty();
    }

    @Test
    @DisplayName("ENG-CTX-01 #5 módulos na ordem do pedido, com pacote e caminho completos")
    void listsModulesInRequestOrder() {
        TemplateContext context = builder.build(SpecBuilder.basic().build());

        assertThat(context.sections("modules")).extracting(
                        module -> module.text("name"),
                        module -> module.text("packageName"),
                        module -> module.text("packagePath"),
                        module -> module.text("displayName"))
                .containsExactly(
                        tuple("pedidos", "com.acme.pedidoapi.pedidos", "com/acme/pedidoapi/pedidos", "Pedidos"),
                        tuple("pagamentos", "com.acme.pedidoapi.pagamentos", "com/acme/pedidoapi/pagamentos", "Pagamentos"));
        assertThat(context.sections("aggregates")).isEmpty();
    }

    @Test
    void listsAggregatesInsteadOfModulesForTheLayeredStyle() {
        TemplateContext context = builder.build(SpecBuilder.basic().layered("paciente", "pedido_item").build());

        List<TemplateContext> aggregates = context.sections("aggregates");

        assertThat(context.sections("modules")).isEmpty();
        assertThat(aggregates).hasSize(2);
        assertThat(aggregates.get(1).text("displayName")).isEqualTo("PedidoItem");
        assertThat(aggregates.get(1).text("packageName")).isEqualTo("com.acme.pedidoapi.pedido_item");
    }

    @Test
    @DisplayName("ENG-CTX-01 #6 o mesmo pedido gera contextos iguais")
    void isDeterministic() {
        TemplateContext first = builder.build(SpecBuilder.basic().extras("docker-compose").build());
        TemplateContext second = builder.build(SpecBuilder.basic().extras("docker-compose").build());

        assertThat(first).isEqualTo(second);
    }

    @Test
    void producesAnImmutableContext() {
        TemplateContext context = builder.build(SpecBuilder.basic().build());

        assertThatThrownBy(() -> context.values().put("x", "y")).isInstanceOf(UnsupportedOperationException.class);
    }

    @Test
    void exposesStyleBootKitAndPersistenceInformation() {
        TemplateContext context = builder.build(SpecBuilder.basic().build());

        assertThat(context.text("style.id")).isEqualTo("HEXAGONAL_MODULAR");
        assertThat(context.text("style.label")).isEqualTo("Hexagonal modular");
        assertThat(context.flag("style.modular")).isTrue();
        assertThat(context.flag("style.layered")).isFalse();
        assertThat(context.text("boot.version")).isEqualTo("4.1.2");
        assertThat(context.text("boot.line")).isEqualTo("4.1");
        assertThat(context.text("kit.id")).isEqualTo("BASIC");
        assertThat(context.text("kit.label")).isEqualTo("Básico");
        assertThat(context.text("persistence.namespace")).isEqualTo("jakarta");
    }

    @Test
    void flagsTheSpringBootGenerationAndTestStarterStyle() {
        TemplateContext boot4 = builder.build(SpecBuilder.basic().build());
        TemplateContext boot3 = builder.build(SpecBuilder.basic().boot("3.5.3", "21").build());
        TemplateContext boot2 = builder.build(SpecBuilder.basic().boot("2.7.18", "17").build());

        assertThat(boot4.flag("boot.v4")).isTrue();
        assertThat(boot4.flag("tests.perTechnology")).isTrue();
        assertThat(boot3.flag("boot.v3")).isTrue();
        assertThat(boot3.flag("boot.v4")).isFalse();
        assertThat(boot3.flag("tests.perTechnology")).isFalse();
        assertThat(boot2.flag("boot.v2")).isTrue();
        assertThat(boot2.text("persistence.namespace")).isEqualTo("javax");
    }

    @Test
    void createsOneFlagPerCatalogDependencyAndExtra() {
        TemplateContext context = builder.build(SpecBuilder.basic().extras("docker-compose").build());

        assertThat(context.section("dep").values()).containsOnlyKeys(
                "web", "validation", "springdoc", "security", "jpa", "flyway", "actuator", "lombok");
        assertThat(context.section("extra").values()).containsOnlyKeys("docker-compose", "github-actions");
        assertThat(context.flag("dep.springdoc")).isFalse();
        assertThat(context.flag("extra.docker-compose")).isTrue();
        assertThat(context.flag("extra.github-actions")).isFalse();
        assertThat(context.flag("fitness.enabled")).isTrue();
    }

    @Test
    void usesTheStarterNamesOfTheChosenBootGeneration() {
        TemplateContext boot3 = builder.build(SpecBuilder.basic().boot("3.5.3", "21").noKit().dependencies("web").build());

        assertThat(coordinates(boot3, "dependencies")).containsExactly("org.springframework.boot:spring-boot-starter-web");
    }

    @Test
    void marksCoordinatesWithAndWithoutExplicitVersionOrScope() {
        TemplateContext context = builder.build(SpecBuilder.basic().noKit().dependencies("springdoc", "jpa").build());

        Map<String, TemplateContext> byArtifact = byArtifact(context, "dependencies");

        TemplateContext springdoc = byArtifact.get("springdoc-openapi-starter-webmvc-ui");
        assertThat(springdoc.flag("hasVersion")).isTrue();
        assertThat(springdoc.text("version")).isEqualTo("${springdoc.version}");

        TemplateContext managed = byArtifact.get("spring-boot-starter-webmvc");
        assertThat(managed.flag("hasVersion")).isFalse();
        assertThat(managed.text("version")).isEmpty();
        assertThat(managed.flag("hasScope")).isFalse();

        TemplateContext driver = byArtifact.get("postgresql");
        assertThat(driver.text("scope")).isEqualTo("runtime");
        assertThat(driver.flag("hasScope")).isTrue();
    }

    @Test
    void addsPinnedVersionPropertiesOnlyWhenSomeSelectedArtifactUsesThem() {
        TemplateContext withSpringdoc = builder.build(SpecBuilder.basic().noKit().dependencies("springdoc").build());
        TemplateContext withoutSpringdoc = builder.build(SpecBuilder.basic().noKit().dependencies("web").build());

        assertThat(names(withSpringdoc, "properties")).containsExactly("archunit.version", "springdoc.version");
        assertThat(names(withoutSpringdoc, "properties")).containsExactly("archunit.version");
    }

    @Test
    @DisplayName("ENG-PLN-02 #3 fitness desligada: sem ArchUnit nem a propriedade da versão")
    void leavesArchunitOutWhenFitnessFunctionsAreOff() {
        TemplateContext context = builder.build(SpecBuilder.basic().noKit().fitness(false).build());

        assertThat(coordinates(context, "testDependencies")).doesNotContain("com.tngtech.archunit:archunit-junit5");
        assertThat(names(context, "properties")).isEmpty();
        assertThat(context.flag("fitness.enabled")).isFalse();
    }

    @Test
    void addsArchunitAsATestDependencyWhenFitnessFunctionsAreOn() {
        TemplateContext context = builder.build(SpecBuilder.basic().noKit().build());

        Map<String, TemplateContext> byArtifact = byArtifact(context, "testDependencies");

        assertThat(coordinates(context, "testDependencies")).containsExactly(
                "com.tngtech.archunit:archunit-junit5", "org.springframework.boot:spring-boot-starter-test");
        assertThat(byArtifact.get("archunit-junit5").text("version")).isEqualTo("${archunit.version}");
        assertThat(byArtifact.get("archunit-junit5").text("scope")).isEqualTo("test");
        assertThat(byArtifact.get("spring-boot-starter-test").text("scope")).isEqualTo("test");
    }

    @Test
    void usesPerTechnologyTestStartersOnBootFour() {
        TemplateContext context = builder.build(SpecBuilder.basic().build());

        assertThat(coordinates(context, "testDependencies")).containsExactly(
                "com.tngtech.archunit:archunit-junit5",
                "org.springframework.boot:spring-boot-starter-data-jpa-test",
                "org.springframework.boot:spring-boot-starter-webmvc-test");
    }

    @Test
    void listsAnnotationProcessorsOfTheSelectedDependencies() {
        TemplateContext withLombok = builder.build(SpecBuilder.basic().build());
        TemplateContext withoutLombok = builder.build(SpecBuilder.basic().noKit().dependencies("web").build());

        List<TemplateContext> processors = withLombok.sections("annotationProcessors");

        assertThat(processors).hasSize(1);
        assertThat(processors.getFirst().text("groupId")).isEqualTo("org.projectlombok");
        assertThat(processors.getFirst().text("artifactId")).isEqualTo("lombok");
        assertThat(processors.getFirst().text("versionProperty")).isEqualTo("lombok.version");
        assertThat(processors.getFirst().text("version")).isEqualTo("${lombok.version}");
        assertThat(withoutLombok.sections("annotationProcessors")).isEmpty();
        assertThat(withLombok.flag("hasAnnotationProcessors")).isTrue();
        assertThat(withoutLombok.flag("hasAnnotationProcessors")).isFalse();
    }

    @Test
    @DisplayName("BR-031 sem nenhum starter, o projeto leva o starter base")
    void addsTheBaseStarterWhenNoOtherStarterIsSelected() {
        TemplateContext withoutKit = builder.build(SpecBuilder.basic().noKit().build());
        TemplateContext lombokOnly = builder.build(SpecBuilder.basic().noKit().dependencies("lombok").build());
        TemplateContext withWeb = builder.build(SpecBuilder.basic().noKit().dependencies("web").build());

        assertThat(coordinates(withoutKit, "dependencies")).containsExactly("org.springframework.boot:spring-boot-starter");
        assertThat(coordinates(lombokOnly, "dependencies"))
                .containsExactly("org.projectlombok:lombok", "org.springframework.boot:spring-boot-starter");
        assertThat(coordinates(withWeb, "dependencies")).containsExactly("org.springframework.boot:spring-boot-starter-webmvc");
    }

    @Test
    void namesTheDatabaseOnlyWhenJpaIsSelected() {
        TemplateContext withJpa = builder.build(SpecBuilder.basic().build());
        TemplateContext withoutJpa = builder.build(SpecBuilder.basic().noKit().dependencies("web").build());

        assertThat(withJpa.text("database.name")).isEqualTo("pedido_api_db");
        assertThat(withoutJpa.find("database")).isEmpty();
    }

    private static List<String> coordinates(TemplateContext context, String path) {
        return context.sections(path).stream()
                .map(entry -> entry.text("groupId") + ":" + entry.text("artifactId"))
                .toList();
    }

    private static List<String> names(TemplateContext context, String path) {
        return context.sections(path).stream().map(entry -> entry.text("name")).toList();
    }

    private static Map<String, TemplateContext> byArtifact(TemplateContext context, String path) {
        return context.sections(path).stream()
                .collect(Collectors.toMap(entry -> entry.text("artifactId"), entry -> entry));
    }
}
