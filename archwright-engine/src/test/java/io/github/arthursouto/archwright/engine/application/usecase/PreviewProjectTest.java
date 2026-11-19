package io.github.arthursouto.archwright.engine.application.usecase;

import io.github.arthursouto.archwright.engine.adapter.out.template.ClasspathTemplateRepository;
import io.github.arthursouto.archwright.engine.adapter.out.template.MustacheTemplateRenderer;
import io.github.arthursouto.archwright.engine.application.port.in.FitnessRuleSummary;
import io.github.arthursouto.archwright.engine.application.port.in.PreviewFile;
import io.github.arthursouto.archwright.engine.application.port.in.PreviewResult;
import io.github.arthursouto.archwright.engine.application.port.in.ProjectRequest;
import io.github.arthursouto.archwright.engine.application.port.in.ResolvedDependency;
import io.github.arthursouto.archwright.engine.application.port.out.TemplateRepository;
import io.github.arthursouto.archwright.engine.domain.model.FilePlan;
import io.github.arthursouto.archwright.engine.domain.model.GeneratedFile;
import io.github.arthursouto.archwright.engine.domain.model.Manifest;
import io.github.arthursouto.archwright.engine.domain.model.TemplateContext;
import io.github.arthursouto.archwright.engine.application.port.in.Violation;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;

class PreviewProjectTest {

    private final ClasspathTemplateRepository realTemplates = new ClasspathTemplateRepository();
    private final AtomicInteger contentReads = new AtomicInteger();
    private final ProjectPlanning planning = UseCaseFixtures.planning(countingTemplates(), new MustacheTemplateRenderer());
    private final PreviewProject preview = new PreviewProject(
            planning, new ProfileResolver(SpecBuilder.catalog()), new ProjectRenderer(countingTemplates(), new MustacheTemplateRenderer()));

    @Test
    @DisplayName("ENG-UC-02 #1 a pré-visualização lista os mesmos caminhos que a geração grava")
    void listsTheSamePathsThatGenerationWrites() {
        ProjectRequest request = UseCaseFixtures.validRequest();

        PreviewResult.Preview result = (PreviewResult.Preview) preview.preview(request);
        List<String> generated = new ProjectRenderer(realTemplates, new MustacheTemplateRenderer())
                .render(planOf(request), contextOf(request)).files().stream()
                .map(GeneratedFile::path)
                .sorted()
                .toList();

        assertThat(result.paths()).isEqualTo(generated).isSorted();
    }

    @Test
    @DisplayName("ENG-UC-02 #2 pedido inválido devolve Failure com as violações")
    void returnsTheViolationsOfAnInvalidRequest() {
        PreviewResult result = preview.preview(UseCaseFixtures.withTwoViolations());

        assertThat(((PreviewResult.Failure) result).violations()).extracting(Violation::code)
                .containsExactlyInAnyOrder("INVALID_ARTIFACT_ID", "UNKNOWN_EXTRA");
    }

    @Test
    void listsTheSelectedFitnessRulesAndNoneWhenDisabled() {
        ProjectRequest enabled = UseCaseFixtures.validRequest();
        ProjectRequest disabled = new ProjectRequest(
                "pedido-api", "com.acme", null, "", "HEXAGONAL_MODULAR", List.of("pedidos"),
                "4.1.2", "25", "NONE", List.of(), List.of(), false);

        PreviewResult.Preview withRules = (PreviewResult.Preview) preview.preview(enabled);
        PreviewResult.Preview withoutRules = (PreviewResult.Preview) preview.preview(disabled);

        assertThat(withRules.fitnessRules()).extracting(FitnessRuleSummary::id).contains("FF-ADP-01", "FF-PER-01", "FF-MOD-01");
        assertThat(withoutRules.fitnessRules()).isEmpty();
    }

    @Test
    void countsTheLinesOfEachFileLikeGenerationWouldWriteThem() {
        ProjectRequest request = UseCaseFixtures.validRequest();

        PreviewResult.Preview result = (PreviewResult.Preview) preview.preview(request);
        Map<String, Integer> written = new ProjectRenderer(realTemplates, new MustacheTemplateRenderer())
                .render(planOf(request), contextOf(request)).files().stream()
                .collect(Collectors.toMap(GeneratedFile::path, file -> lines(new String(file.content(), StandardCharsets.UTF_8))));

        assertThat(result.files()).extracting(PreviewFile::path).isEqualTo(result.paths());
        assertThat(result.files()).filteredOn(file -> file.path().equals("pom.xml")).singleElement()
                .satisfies(file -> assertThat(file.lines()).isPositive());
        assertThat(result.files()).allSatisfy(file -> assertThat(file.lines()).isEqualTo(written.get(file.path())));
    }

    @Test
    void resolvesMavenCoordinatesWithPinnedVersionsAndScopes() {
        ProjectRequest request = new ProjectRequest(
                "pedido-api", "com.acme", null, "", "HEXAGONAL_MODULAR", List.of("pedidos"),
                "4.1.2", "25", "NONE", List.of("springdoc", "jpa"), List.of(), true);

        PreviewResult.Preview result = (PreviewResult.Preview) preview.preview(request);

        assertThat(result.dependencies()).contains(
                new ResolvedDependency("web", "org.springframework.boot", "spring-boot-starter-webmvc", null, "compile"),
                new ResolvedDependency("springdoc", "org.springdoc", "springdoc-openapi-starter-webmvc-ui", "3.2.0", "compile"),
                new ResolvedDependency("jpa", "org.postgresql", "postgresql", null, "runtime"));
    }

    @Test
    void reportsTheWarningsOfTheValidation() {
        ProjectRequest request = new ProjectRequest(
                "pedido-api", "com.acme", null, "", "HEXAGONAL_MODULAR", List.of("pedidos"),
                "4.1.2", "25", "NONE", List.of("springdoc"), List.of(), true);

        PreviewResult.Preview result = (PreviewResult.Preview) preview.preview(request);

        assertThat(result.warnings()).hasSize(1);
        assertThat(result.warnings().getFirst().params()).containsEntry("added", "web");
    }

    @Test
    void eachPackageInfoCarriesTheDescriptionOfItsPackage() {
        PreviewResult.Preview result = (PreviewResult.Preview) preview.preview(UseCaseFixtures.validRequest());

        assertThat(result.files()).filteredOn(file -> file.path().endsWith("/package-info.java"))
                .isNotEmpty()
                .allSatisfy(file -> assertThat(file.description()).isNotBlank().doesNotContain("{{"));
        assertThat(result.files()).filteredOn(file -> file.path().equals("pom.xml")).singleElement()
                .satisfies(file -> assertThat(file.description()).isEmpty());
    }

    private int lines(String text) {
        if (text.isEmpty()) {
            return 0;
        }

        long breaks = text.chars().filter(character -> character == '\n').count();

        return (int) (text.endsWith("\n") ? breaks : breaks + 1);
    }

    private FilePlan planOf(ProjectRequest request) {
        return ((ProjectPlanning.Outcome.Ready) planning.prepare(request)).plan();
    }

    private TemplateContext contextOf(ProjectRequest request) {
        return ((ProjectPlanning.Outcome.Ready) planning.prepare(request)).context();
    }

    private TemplateRepository countingTemplates() {
        return new TemplateRepository() {
            @Override
            public Manifest manifest(String directory) {
                return realTemplates.manifest(directory);
            }

            @Override
            public byte[] content(String path) {
                contentReads.incrementAndGet();
                return realTemplates.content(path);
            }
        };
    }
}
