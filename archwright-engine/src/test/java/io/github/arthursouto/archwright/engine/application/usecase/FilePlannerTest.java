package io.github.arthursouto.archwright.engine.application.usecase;

import io.github.arthursouto.archwright.engine.adapter.out.template.ClasspathTemplateRepository;
import io.github.arthursouto.archwright.engine.adapter.out.template.MustacheTemplateRenderer;
import io.github.arthursouto.archwright.engine.application.port.out.TemplateRepository;
import io.github.arthursouto.archwright.engine.domain.kit.Kits;
import io.github.arthursouto.archwright.engine.domain.model.FilePlan;
import io.github.arthursouto.archwright.engine.domain.model.LineEnding;
import io.github.arthursouto.archwright.engine.domain.model.Manifest;
import io.github.arthursouto.archwright.engine.domain.model.ManifestFile;
import io.github.arthursouto.archwright.engine.domain.model.ManifestPackage;
import io.github.arthursouto.archwright.engine.domain.model.PlannedFile;
import io.github.arthursouto.archwright.engine.domain.model.ProjectSpec;
import io.github.arthursouto.archwright.engine.domain.model.Repeat;
import io.github.arthursouto.archwright.engine.domain.model.TemplateContext;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class FilePlannerTest {

    private static final String BASE = "src/main/java/com/acme/pedidoapi/";

    private final ContextBuilder contextBuilder = new ContextBuilder(
            new ProfileResolver(SpecBuilder.catalog()),
            SpecBuilder.catalog(),
            SpecBuilder.repository().styles(),
            Kits.standard());

    private final FilePlanner planner = new FilePlanner(
            new ClasspathTemplateRepository(), new MustacheTemplateRenderer(), SpecBuilder.repository().styles());

    @Test
    @DisplayName("ENG-PLN-01 #1 estilo modular, kit NONE, fitness ligada: plano igual à lista esperada")
    void plansTheModularStyleWithoutKit() throws IOException {
        FilePlan plan = plan(SpecBuilder.basic().noKit().build());

        assertThat(plan.targets()).containsExactlyElementsOf(expected("modular-none.txt"));
    }

    @Test
    @DisplayName("ENG-PLN-01 #1 contém a classe principal, os package-info e os arquivos do wrapper")
    void containsTheMainClassPackageInfosAndWrapperFiles() {
        List<String> targets = plan(SpecBuilder.basic().noKit().build()).targets();

        assertThat(targets).contains(
                BASE + "bootstrap/PedidoApiApplication.java",
                BASE + "shared/package-info.java",
                BASE + "pedidos/package-info.java",
                BASE + "pagamentos/package-info.java",
                "pom.xml", "mvnw", "mvnw.cmd", ".mvn/wrapper/maven-wrapper.properties");
    }

    @Test
    @DisplayName("ENG-PLN-01 #2 com jpa, um módulo a mais acrescenta 19 package-info")
    void addsNineteenPackageInfosPerExtraModule() {
        FilePlan two = plan(SpecBuilder.basic().modules("pedidos", "pagamentos").build());
        FilePlan three = plan(SpecBuilder.basic().modules("pedidos", "pagamentos", "estoque").build());

        assertThat(packageInfos(two)).hasSize(13 + 19 * 2);
        assertThat(packageInfos(three)).hasSize(13 + 19 * 3);
        assertThat(packageInfos(three)).filteredOn(target -> target.startsWith(BASE + "estoque/")).hasSize(19);
    }

    @Test
    @DisplayName("ENG-PLN-01 #2 sem jpa, um módulo a mais acrescenta 16 package-info e nenhum de persistência")
    void addsSixteenPackageInfosPerExtraModuleWithoutJpa() {
        FilePlan two = plan(SpecBuilder.basic().noKit().modules("pedidos", "pagamentos").build());
        FilePlan three = plan(SpecBuilder.basic().noKit().modules("pedidos", "pagamentos", "estoque").build());

        assertThat(packageInfos(two)).hasSize(12 + 16 * 2);
        assertThat(packageInfos(three)).hasSize(12 + 16 * 3);
        assertThat(three.targets()).noneMatch(target -> target.contains("/persistence/"));
    }

    @Test
    @DisplayName("ENG-PLN-01 #9 sem jpa, o estilo em camadas não gera persistence, entity nem jpa")
    void omitsPersistencePackagesWithoutJpaInTheLayeredStyle() {
        FilePlan plan = plan(SpecBuilder.basic().noKit().layered("paciente").build());

        assertThat(packageInfos(plan)).hasSize(17);
        assertThat(plan.targets()).noneMatch(target -> target.contains("/persistence/"));
    }

    @Test
    @DisplayName("ENG-PLN-01 #4 sem flyway não há migration baseline")
    void plansTheBaselineMigrationOnlyWithFlyway() {
        String migration = "src/main/resources/db/migration/V1__baseline.sql";

        assertThat(plan(SpecBuilder.basic().noKit().build()).targets()).doesNotContain(migration);
        assertThat(plan(SpecBuilder.basic().build()).targets()).contains(migration);
    }

    @Test
    @DisplayName("ENG-PLN-01 #5 extra docker-compose gera docker-compose.yml e .env.example")
    void plansComposeFilesForTheDockerComposeExtra() {
        assertThat(plan(SpecBuilder.basic().extras("docker-compose").build()).targets())
                .contains("docker-compose.yml", ".env.example");
        assertThat(plan(SpecBuilder.basic().build()).targets())
                .doesNotContain("docker-compose.yml", ".env.example");
    }

    @Test
    void plansTheWorkflowOnlyForTheGithubActionsExtra() {
        assertThat(plan(SpecBuilder.basic().extras("github-actions").build()).targets())
                .contains(".github/workflows/ci.yml");
        assertThat(plan(SpecBuilder.basic().build()).targets()).doesNotContain(".github/workflows/ci.yml");
    }

    @Test
    @DisplayName("ENG-PLN-01 #1 kit BASIC com os dois extras: plano igual à lista esperada")
    void plansTheBasicKitWithBothExtras() throws IOException {
        FilePlan plan = plan(SpecBuilder.basic().extras("docker-compose", "github-actions").build());

        assertThat(plan.targets()).containsExactlyElementsOf(expected("modular-basic-extras.txt"));
    }

    @Test
    @DisplayName("ENG-PLN-01 #6 todos os caminhos são relativos, sem .. e com /")
    void producesOnlyRelativePathsWithForwardSlashes() {
        List<String> targets = plan(SpecBuilder.basic().extras("docker-compose", "github-actions").build()).targets();

        assertThat(targets).allSatisfy(target -> {
            assertThat(target).doesNotStartWith("/");
            assertThat(target).doesNotContain("..", "\\", ":", "//");
            assertThat(target).isNotBlank();
        });
    }

    @Test
    @DisplayName("ENG-PLN-01 #8 cada diretório Java gerado tem exatamente um package-info")
    void everyJavaDirectoryHasExactlyOnePackageInfo() {
        List<String> targets = plan(SpecBuilder.basic().extras("docker-compose").build()).targets();

        Map<String, List<String>> byDirectory = targets.stream()
                .filter(target -> target.endsWith(".java"))
                .collect(Collectors.groupingBy(target -> target.substring(0, target.lastIndexOf('/')), TreeMap::new, Collectors.toList()));

        assertThat(byDirectory).isNotEmpty().allSatisfy((directory, files) ->
                assertThat(files.stream().filter(file -> file.endsWith("/package-info.java")))
                        .as(directory)
                        .hasSize(1));
    }

    @Test
    @DisplayName("ENG-PLN-01 #1 estilo em camadas, kit BASIC, docker-compose: plano igual à lista esperada")
    void plansTheLayeredStyleWithTheBasicKit() throws IOException {
        FilePlan plan = plan(SpecBuilder.basic().layered("paciente").extras("docker-compose").build());

        assertThat(plan.targets()).containsExactlyElementsOf(expected("layered-basic.txt"));
    }

    @Test
    @DisplayName("ENG-PLN-01 #3 estilo em camadas: package-info do agregado e nenhum bootstrap")
    void plansTheAggregatePackageAndNoBootstrapForTheLayeredStyle() {
        FilePlan plan = plan(SpecBuilder.basic().layered("paciente").build());

        assertThat(plan.targets()).contains(
                BASE + "domain/model/paciente/package-info.java",
                BASE + "PedidoApiApplication.java");
        assertThat(plan.targets()).noneMatch(target -> target.contains("/bootstrap/"));
    }

    @Test
    void addsOnePackageInfoPerAggregateOnTopOfNineteen() {
        assertThat(packageInfos(plan(SpecBuilder.basic().layered().build()))).hasSize(19);
        assertThat(packageInfos(plan(SpecBuilder.basic().layered("paciente", "prontuario").build()))).hasSize(21);
    }

    @Test
    @DisplayName("ENG-PLN-01 #8 no estilo em camadas cada diretório Java gerado também tem um package-info")
    void everyJavaDirectoryOfTheLayeredStyleHasExactlyOnePackageInfo() {
        List<String> targets = plan(SpecBuilder.basic().layered("paciente").build()).targets();

        Map<String, List<String>> byDirectory = targets.stream()
                .filter(target -> target.endsWith(".java"))
                .collect(Collectors.groupingBy(target -> target.substring(0, target.lastIndexOf('/')), TreeMap::new, Collectors.toList()));

        assertThat(byDirectory).isNotEmpty().allSatisfy((directory, files) ->
                assertThat(files.stream().filter(file -> file.endsWith("/package-info.java"))).as(directory).hasSize(1));
    }

    @Test
    void sortsThePlanByTarget() {
        List<String> targets = plan(SpecBuilder.basic().build()).targets();

        assertThat(targets).isSorted();
    }

    @Test
    void isDeterministic() {
        ProjectSpec spec = SpecBuilder.basic().extras("docker-compose").build();

        assertThat(plan(spec)).isEqualTo(plan(spec));
    }

    @Test
    void remembersHowEachFileMustBeRendered() {
        FilePlan plan = plan(SpecBuilder.basic().build());

        assertThat(file(plan, "pom.xml")).satisfies(pom -> {
            assertThat(pom.template()).isEqualTo("common/pom.xml.mustache");
            assertThat(pom.render()).isTrue();
            assertThat(pom.executable()).isFalse();
            assertThat(pom.lineEnding()).isEqualTo(LineEnding.LF);
        });
        assertThat(file(plan, "mvnw")).satisfies(script -> {
            assertThat(script.executable()).isTrue();
            assertThat(script.render()).isFalse();
        });
        assertThat(file(plan, "mvnw.cmd")).satisfies(script -> {
            assertThat(script.lineEnding()).isEqualTo(LineEnding.CRLF);
            assertThat(script.render()).isFalse();
        });
        assertThat(file(plan, BASE + "bootstrap/PedidoApiApplication.java").template())
                .isEqualTo("hexagonal-modular/Application.java.mustache");
    }

    @Test
    void givesEachPackageInfoItsPackageAndEvaluatedDescription() {
        FilePlan plan = plan(SpecBuilder.basic().build());

        PlannedFile entity = file(plan, BASE + "pedidos/adapter/out/persistence/entity/package-info.java");
        PlannedFile root = file(plan, BASE + "package-info.java");

        assertThat(entity.template()).isEqualTo("common/package-info.java.mustache");
        assertThat(entity.variables()).containsEntry("package", "com.acme.pedidoapi.pedidos.adapter.out.persistence.entity");
        assertThat(entity.variables().get("description")).contains("pedidos");
        assertThat(root.variables()).containsEntry("package", "com.acme.pedidoapi");
        assertThat(root.variables().get("description")).contains("pedido-api");
    }

    @Test
    @DisplayName("ENG-PLN-02 #3 e #4 o archunit.properties acompanha a fitness ligada")
    void plansTheArchunitPropertiesOnlyWhenFitnessIsOn() {
        assertThat(plan(SpecBuilder.basic().fitness(true).build()).targets()).contains("src/test/resources/archunit.properties");
        assertThat(plan(SpecBuilder.basic().fitness(false).build()).targets()).doesNotContain("src/test/resources/archunit.properties");
    }

    @Test
    @DisplayName("ENG-PLN-01 #7 caminho que foge da raiz é erro de template")
    void rejectsPathsThatEscapeTheProjectRoot() {
        FilePlanner invalid = plannerWith(Map.of("common", manifest(file("x.mustache", "../fora.txt"))));

        assertThatThrownBy(() -> invalid.plan(spec(), context()))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("INVALID_TEMPLATE_PATH")
                .hasMessageContaining("../fora.txt");
    }

    @Test
    void rejectsAbsoluteWindowsAndEmptySegmentPaths() {
        for (String target : List.of("/etc/passwd", "C:/Windows/x", "a\\b.txt", "a//b.txt", "./a.txt", "a/./b.txt", " ")) {
            FilePlanner invalid = plannerWith(Map.of("common", manifest(file("x.mustache", target))));

            assertThatThrownBy(() -> invalid.plan(spec(), context()))
                    .as(target)
                    .hasMessageContaining("INVALID_TEMPLATE_PATH");
        }
    }

    @Test
    void rejectsTwoEntriesWithTheSameTarget() {
        FilePlanner duplicated = plannerWith(Map.of("common", manifest(file("a.mustache", "same.txt"), file("b.mustache", "same.txt"))));

        assertThatThrownBy(() -> duplicated.plan(spec(), context()))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("duplicated")
                .hasMessageContaining("same.txt");
    }

    @Test
    void requiresEveryConditionAndHonorsNegation() {
        FilePlanner conditional = plannerWith(Map.of("common", manifest(
                file("a.mustache", "both.txt", "dep.jpa", "dep.web"),
                file("b.mustache", "only-without-security.txt", "!dep.security"),
                file("c.mustache", "needs-security.txt", "dep.security"),
                file("d.mustache", "jpa-and-not-security.txt", "dep.jpa", "!dep.security"))));

        assertThat(conditional.plan(spec(), context()).targets()).containsExactly(
                "both.txt", "jpa-and-not-security.txt", "only-without-security.txt");
    }

    @Test
    void rejectsConditionOverAnUnknownFlag() {
        FilePlanner unknown = plannerWith(Map.of("common", manifest(file("a.mustache", "a.txt", "dep.kafka"))));

        assertThatThrownBy(() -> unknown.plan(spec(), context()))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("common/manifest.yaml")
                .hasMessageContaining("dep.kafka");
    }

    @Test
    void repeatsEntriesForEachModuleWithTheModuleVariable() {
        FilePlanner repeating = plannerWith(Map.of("common", new Manifest(
                List.of(new ManifestFile("m.mustache", "docs/{{module.name}}.md", List.of(), false, true, LineEnding.LF, Repeat.MODULES)),
                List.of())));

        assertThat(repeating.plan(spec(), context()).targets()).containsExactly("docs/pagamentos.md", "docs/pedidos.md");
    }

    @Test
    void repeatsEntriesForEachAggregateInTheLayeredStyle() {
        ProjectSpec layered = SpecBuilder.basic().layered("paciente", "prontuario").fitness(false).build();
        Manifest layeredManifest = new Manifest(
                List.of(new ManifestFile("m.mustache", "docs/{{aggregate.name}}.md", List.of(), false, true, LineEnding.LF, Repeat.AGGREGATES)),
                List.of(new ManifestPackage("domain.model.{{aggregate.name}}", "Agregado {{aggregate.displayName}}.", List.of(), Repeat.AGGREGATES)));
        FilePlanner repeating = plannerWith(Map.of("common", manifest(), "hexagonal-layered", layeredManifest));

        FilePlan plan = repeating.plan(layered, contextBuilder.build(layered));

        assertThat(plan.targets()).containsExactly(
                "docs/paciente.md",
                "docs/prontuario.md",
                BASE + "domain/model/paciente/package-info.java",
                BASE + "domain/model/prontuario/package-info.java");
        assertThat(file(plan, BASE + "domain/model/paciente/package-info.java").variables())
                .containsEntry("description", "Agregado Paciente.");
    }

    @Test
    void producesNothingForAnEntryRepeatedOverAnEmptyList() {
        ProjectSpec layered = SpecBuilder.basic().layered().fitness(false).build();
        Manifest layeredManifest = new Manifest(
                List.of(new ManifestFile("m.mustache", "docs/{{aggregate.name}}.md", List.of(), false, true, LineEnding.LF, Repeat.AGGREGATES)),
                List.of());
        FilePlanner repeating = plannerWith(Map.of("common", manifest(), "hexagonal-layered", layeredManifest));

        assertThat(repeating.plan(layered, contextBuilder.build(layered)).targets()).isEmpty();
    }

    private FilePlan plan(ProjectSpec spec) {
        return planner.plan(spec, contextBuilder.build(spec));
    }

    private FilePlanner plannerWith(Map<String, Manifest> manifests) {
        Map<String, Manifest> withEmptyStyles = new HashMap<>(Map.of(
                "hexagonal-modular", manifest(),
                "hexagonal-layered", manifest()));
        withEmptyStyles.putAll(manifests);

        return new FilePlanner(new FakeTemplates(withEmptyStyles), new MustacheTemplateRenderer(), SpecBuilder.repository().styles());
    }

    private ProjectSpec spec() {
        return SpecBuilder.basic().fitness(false).build();
    }

    private TemplateContext context() {
        return contextBuilder.build(spec());
    }

    private static Manifest manifest(ManifestFile... files) {
        return new Manifest(List.of(files), List.of());
    }

    private static ManifestFile file(String template, String target, String... when) {
        return new ManifestFile(template, target, List.of(when), false, true, LineEnding.LF, Repeat.NONE);
    }

    private static List<String> packageInfos(FilePlan plan) {
        return plan.targets().stream()
                .filter(target -> target.startsWith("src/main/java/") && target.endsWith("/package-info.java"))
                .toList();
    }

    private static PlannedFile file(FilePlan plan, String target) {
        return plan.files().stream().filter(candidate -> candidate.target().equals(target)).findFirst().orElseThrow();
    }

    private List<String> expected(String name) throws IOException {
        try (InputStream stream = getClass().getResourceAsStream("/expected-plans/" + name)) {
            String content = new String(stream.readAllBytes(), StandardCharsets.UTF_8);

            return Arrays.stream(content.split("\n")).filter(line -> !line.isBlank()).toList();
        }
    }

    private static final class FakeTemplates implements TemplateRepository {

        private final Map<String, Manifest> manifests;

        private FakeTemplates(Map<String, Manifest> manifests) {
            this.manifests = manifests;
        }

        @Override
        public Manifest manifest(String directory) {
            Manifest manifest = manifests.get(directory);

            if (manifest == null) {
                throw new IllegalStateException("no manifest for " + directory);
            }

            return manifest;
        }

        @Override
        public byte[] content(String path) {
            throw new UnsupportedOperationException("the planner never reads template content");
        }
    }
}
