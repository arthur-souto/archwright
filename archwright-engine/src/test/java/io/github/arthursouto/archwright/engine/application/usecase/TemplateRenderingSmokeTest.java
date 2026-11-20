package io.github.arthursouto.archwright.engine.application.usecase;

import io.github.arthursouto.archwright.engine.adapter.out.template.ClasspathTemplateRepository;
import io.github.arthursouto.archwright.engine.adapter.out.template.MustacheTemplateRenderer;
import io.github.arthursouto.archwright.engine.domain.kit.Kits;
import io.github.arthursouto.archwright.engine.domain.model.FilePlan;
import io.github.arthursouto.archwright.engine.domain.model.PlannedFile;
import io.github.arthursouto.archwright.engine.domain.model.ProjectSpec;
import io.github.arthursouto.archwright.engine.domain.model.TemplateContext;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.xml.sax.InputSource;
import org.yaml.snakeyaml.Yaml;

import javax.xml.parsers.DocumentBuilderFactory;
import java.io.StringReader;
import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;

class TemplateRenderingSmokeTest {

    private final ClasspathTemplateRepository repository = new ClasspathTemplateRepository();
    private final MustacheTemplateRenderer renderer = new MustacheTemplateRenderer();

    private final ContextBuilder contextBuilder = new ContextBuilder(
            new ProfileResolver(SpecBuilder.catalog()),
            SpecBuilder.catalog(),
            SpecBuilder.repository().styles(),
            Kits.standard());

    private final FilePlanner planner = new FilePlanner(repository, renderer, SpecBuilder.repository().styles());

    @Test
    void rendersEveryPlannedFileOfAFullBootFourProject() {
        Map<String, String> files = renderAll(SpecBuilder.basic()
                .description("API de pedidos & cia")
                .extras("docker-compose", "github-actions")
                .build());

        assertThat(files).containsKeys("pom.xml", "README.md", "src/main/resources/application.yaml", "docker-compose.yml", ".env.example");
        assertThat(files.values()).allSatisfy(content -> assertThat(content).doesNotContain("{{"));
    }

    @Test
    void rendersEveryPlannedFileOfALeanBootThreeProjectWithoutFitnessFunctions() {
        Map<String, String> files = renderAll(SpecBuilder.basic().boot("3.5.3", "21").noKit().fitness(false).build());

        assertThat(files).doesNotContainKeys("docker-compose.yml", "src/test/resources/archunit.properties");
        assertThat(files.values()).allSatisfy(content -> assertThat(content).doesNotContain("{{"));
    }

    @Test
    void rendersEveryPlannedFileOfALayeredProject() {
        Map<String, String> files = renderAll(SpecBuilder.basic().layered("paciente", "pedido_item").extras("docker-compose").build());

        assertThat(files).containsKeys(
                "src/main/java/com/acme/pedidoapi/PedidoApiApplication.java",
                "src/main/java/com/acme/pedidoapi/domain/model/pedido_item/package-info.java",
                "src/test/java/com/acme/pedidoapi/architecture/AdaptersTest.java");
        assertThat(files.get("src/test/java/com/acme/pedidoapi/architecture/HexagonalLayersTest.java"))
                .contains("..infrastructure..")
                .doesNotContain("..adapter..");
        assertThat(files.get("src/main/java/com/acme/pedidoapi/PedidoApiApplication.java")).doesNotContain("scanBasePackages");
        assertThat(files.values()).allSatisfy(content -> assertThat(content).doesNotContain("{{"));
    }

    @Test
    void rendersEveryPlannedFileOfALegacyBootTwoProject() {
        Map<String, String> files = renderAll(SpecBuilder.basic().boot("2.7.18", "8").build());

        assertThat(files.get("pom.xml")).contains("<java.version>1.8</java.version>");
        assertThat(files.values()).allSatisfy(content -> assertThat(content).doesNotContain("{{"));
    }

    @Test
    void generatesAWellFormedPomWithTheProfileDependencies() {
        String pom = renderAll(SpecBuilder.basic().description("API <beta> & cia").build()).get("pom.xml");

        assertThatCode(() -> parseXml(pom)).doesNotThrowAnyException();
        assertThat(pom)
                .contains("<artifactId>spring-boot-starter-parent</artifactId>")
                .contains("<version>4.1.2</version>")
                .contains("<groupId>com.acme</groupId>")
                .contains("<artifactId>pedido-api</artifactId>")
                .contains("<description>API &lt;beta&gt; &amp; cia</description>")
                .contains("<java.version>25</java.version>")
                .contains("<archunit.version>1.5.1</archunit.version>")
                .contains("<artifactId>spring-boot-starter-webmvc</artifactId>")
                .contains("<artifactId>spring-boot-starter-webmvc-test</artifactId>")
                .contains("<artifactId>archunit-junit5</artifactId>")
                .contains("<version>${archunit.version}</version>")
                .doesNotContain("\n\n\n");
    }

    @Test
    void configuresLombokAsAnAnnotationProcessorAndExcludesItFromThePackage() {
        String pom = renderAll(SpecBuilder.basic().build()).get("pom.xml");

        assertThat(pom)
                .contains("<annotationProcessorPaths>")
                .contains("<version>${lombok.version}</version>")
                .contains("<excludes>")
                .contains("<scope>provided</scope>");
    }

    @Test
    void pinsTheLombokVersionOnlyWhenLombokIsSelected() {
        String withLombok = renderAll(SpecBuilder.basic().build()).get("pom.xml");
        String withoutLombok = renderAll(SpecBuilder.basic().noKit().dependencies("web").build()).get("pom.xml");

        assertThat(withLombok).contains("<lombok.version>1.18.48</lombok.version>");
        assertThat(withoutLombok).doesNotContain("lombok");
    }

    @Test
    void leavesOutTheCompilerPluginWhenThereIsNoAnnotationProcessor() {
        String pom = renderAll(SpecBuilder.basic().noKit().dependencies("web").build()).get("pom.xml");

        assertThat(pom).doesNotContain("maven-compiler-plugin").doesNotContain("annotationProcessorPaths").doesNotContain("<excludes>");
    }

    @Test
    void addsTheBaseStarterWhenThereIsNoOtherStarter() {
        String pom = renderAll(SpecBuilder.basic().noKit().build()).get("pom.xml");

        assertThat(pom).contains("<artifactId>spring-boot-starter</artifactId>");
        assertThatCode(() -> parseXml(pom)).doesNotThrowAnyException();
    }

    @Test
    void makesUnmappedRoutesThrowOnlyOnLinesBeforeSpringSixPointOne() {
        String boot3Old = renderAll(SpecBuilder.basic().boot("3.1.5", "21").build()).get("src/main/resources/application.yaml");
        String boot2 = renderAll(SpecBuilder.basic().boot("2.7.18", "17").build()).get("src/main/resources/application.yaml");
        String boot4 = renderAll(SpecBuilder.basic().build()).get("src/main/resources/application.yaml");
        String noWeb = renderAll(SpecBuilder.basic().boot("2.7.18", "17").noKit().build()).get("src/main/resources/application.yaml");

        assertThat(boot3Old).contains("throw-exception-if-no-handler-found: true").contains("add-mappings: false");
        assertThat(boot2).contains("throw-exception-if-no-handler-found: true").contains("add-mappings: false");
        assertThat(boot4).doesNotContain("throw-exception-if-no-handler-found").doesNotContain("add-mappings");
        assertThat(noWeb).doesNotContain("throw-exception-if-no-handler-found");
        assertThatCode(() -> new Yaml().load(boot2)).doesNotThrowAnyException();
    }

    @Test
    void generatesTheApplicationYamlForPersistenceWithFlyway() {
        String yaml = renderAll(SpecBuilder.basic().build()).get("src/main/resources/application.yaml");

        assertThatCode(() -> new Yaml().load(yaml)).doesNotThrowAnyException();
        assertThat(yaml)
                .contains("name: pedido-api")
                .contains("jdbc:postgresql://localhost:5433/pedido_api_db?sslmode=disable")
                .contains("ddl-auto: validate")
                .contains("open-in-view: false")
                .contains("include: health");
    }

    @Test
    void generatesTheApplicationYamlWithoutDatabaseWhenThereIsNoJpa() {
        String yaml = renderAll(SpecBuilder.basic().noKit().dependencies("web").build()).get("src/main/resources/application.yaml");

        assertThatCode(() -> new Yaml().load(yaml)).doesNotThrowAnyException();
        assertThat(yaml).contains("name: pedido-api").contains("port: ${SERVER_PORT:8080}")
                .doesNotContain("datasource").doesNotContain("management");
    }

    @Test
    @DisplayName("BR-031 projeto sem banco: nada de datasource, driver, migration nem docker-compose")
    void generatesAProjectWithoutDatabase() {
        Map<String, String> files = renderAll(SpecBuilder.basic().noKit().dependencies("web", "validation", "actuator").build());

        assertThat(files).doesNotContainKeys(
                "src/main/resources/db/migration/V1__baseline.sql", "docker-compose.yml", ".env.example");
        assertThat(files.get("pom.xml"))
                .doesNotContain("postgresql")
                .doesNotContain("flyway")
                .doesNotContain("data-jpa");
        assertThat(files.get("src/main/resources/application.yaml"))
                .doesNotContain("datasource")
                .doesNotContain("jpa")
                .doesNotContain("DATASOURCE");
        assertThat(files.get("README.md")).doesNotContain("docker compose").doesNotContain("PostgreSQL");
        assertThat(files.values()).allSatisfy(content -> assertThat(content).doesNotContain("pedido_api_db"));
    }

    @Test
    void usesNoDdlAutoWhenThereIsJpaButNoFlyway() {
        String yaml = renderAll(SpecBuilder.basic().noKit().dependencies("jpa").build()).get("src/main/resources/application.yaml");

        assertThat(yaml).contains("ddl-auto: none");
    }

    @Test
    void generatesTheMainClassInTheBootstrapPackage() {
        String main = renderAll(SpecBuilder.basic().build())
                .get("src/main/java/com/acme/pedidoapi/bootstrap/PedidoApiApplication.java");

        assertThat(main)
                .startsWith("package com.acme.pedidoapi.bootstrap;")
                .contains("@SpringBootApplication(scanBasePackages = \"com.acme.pedidoapi\")")
                .contains("@AutoConfigurationPackage(basePackages = \"com.acme.pedidoapi\")")
                .contains("public class PedidoApiApplication {")
                .contains("SpringApplication.run(PedidoApiApplication.class, args);");
    }

    @Test
    void generatesPackageInfosWithTheirPackageAndDescription() {
        Map<String, String> files = renderAll(SpecBuilder.basic().build());

        assertThat(files.get("src/main/java/com/acme/pedidoapi/package-info.java"))
                .isEqualTo("/**\n * Raiz do projeto pedido-api.\n */\npackage com.acme.pedidoapi;\n");
        assertThat(files.get("src/main/java/com/acme/pedidoapi/pedidos/domain/package-info.java"))
                .contains("package com.acme.pedidoapi.pedidos.domain;")
                .contains("Regras e modelo de negócio do módulo pedidos.");
    }

    @Test
    void generatesTheReadmeWithTheChosenOptions() {
        String readme = renderAll(SpecBuilder.basic().description("API de pedidos").build()).get("README.md");

        assertThat(readme)
                .startsWith("# pedido-api\n\nAPI de pedidos\n\n")
                .contains("| Arquitetura | Hexagonal modular |")
                .contains("| Spring Boot | 4.1.2 |")
                .contains("| Kit de dependências | Básico |")
                .contains("- `pedidos`: módulo Pedidos.")
                .contains("- `pagamentos`: módulo Pagamentos.")
                .contains("src/test/java/com/acme/pedidoapi/architecture")
                .contains("FreezingArchRule.freeze");
    }

    @Test
    void leavesTheFitnessSectionOutOfTheReadmeWhenItIsOff() {
        String readme = renderAll(SpecBuilder.basic().fitness(false).build()).get("README.md");

        assertThat(readme).doesNotContain("Fitness functions").doesNotContain("FreezingArchRule");
    }

    @Test
    void generatesComposeAndEnvFilesConsistentWithTheApplicationYaml() {
        Map<String, String> files = renderAll(SpecBuilder.basic().extras("docker-compose").build());

        assertThatCode(() -> new Yaml().load(files.get("docker-compose.yml"))).doesNotThrowAnyException();
        assertThat(files.get("docker-compose.yml"))
                .contains("pedido-api-database:")
                .contains("POSTGRES_DB: ${POSTGRES_DB:-pedido_api_db}")
                .contains("pg_isready -U $${POSTGRES_USER} -d $${POSTGRES_DB}")
                .contains("pedido-api-db-data:/var/lib/postgresql/data");
        assertThat(files.get(".env.example"))
                .contains("POSTGRES_DB=pedido_api_db")
                .contains("DATASOURCE_URL=jdbc:postgresql://localhost:5433/pedido_api_db?sslmode=disable");
    }

    @Test
    void generatesTheWorkflowWithTheChosenJavaVersion() {
        String workflow = renderAll(SpecBuilder.basic().extras("github-actions").build()).get(".github/workflows/ci.yml");

        assertThatCode(() -> new Yaml().load(workflow)).doesNotThrowAnyException();
        assertThat(workflow).contains("java-version: '25'").contains("run: ./mvnw -B verify");
    }

    @Test
    void copiesTheWrapperAndGitignoreWithoutRendering() {
        Map<String, String> files = renderAll(SpecBuilder.basic().build());

        assertThat(files.get("mvnw")).startsWith("#!/bin/sh");
        assertThat(files.get("mvnw.cmd")).contains("\r\n");
        assertThat(files.get(".mvn/wrapper/maven-wrapper.properties")).contains("distributionType=only-script");
        assertThat(files.get(".gitignore")).contains("target/").contains(".env");
        assertThat(files.get("src/test/resources/archunit.properties")).isEqualTo("archRule.failOnEmptyShould=false\n");
    }

    @Test
    void generatesABaselineMigrationWithoutDdl() {
        String migration = renderAll(SpecBuilder.basic().build()).get("src/main/resources/db/migration/V1__baseline.sql");

        assertThat(migration).startsWith("-- Baseline do schema de pedido-api.")
                .doesNotContainIgnoringCase("create ");
    }

    private Map<String, String> renderAll(ProjectSpec spec) {
        TemplateContext context = contextBuilder.build(spec);
        FilePlan plan = planner.plan(spec, context);

        return plan.files().stream().collect(Collectors.toMap(
                PlannedFile::target, file -> render(file, context), (first, second) -> first, LinkedHashMap::new));
    }

    private String render(PlannedFile file, TemplateContext context) {
        String content = new String(repository.content(file.template()), StandardCharsets.UTF_8);

        if (!file.render()) {
            return content;
        }

        Map<String, Object> scope = new LinkedHashMap<>(context.values());
        scope.putAll(file.variables());

        return renderer.render(content, scope);
    }

    private static void parseXml(String xml) throws Exception {
        DocumentBuilderFactory.newInstance().newDocumentBuilder().parse(new InputSource(new StringReader(xml)));
    }
}
