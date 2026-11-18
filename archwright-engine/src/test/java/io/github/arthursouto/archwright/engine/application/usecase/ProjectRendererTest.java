package io.github.arthursouto.archwright.engine.application.usecase;

import io.github.arthursouto.archwright.engine.adapter.out.template.ClasspathTemplateRepository;
import io.github.arthursouto.archwright.engine.adapter.out.template.MustacheTemplateRenderer;
import io.github.arthursouto.archwright.engine.application.port.out.TemplateRepository;
import io.github.arthursouto.archwright.engine.domain.kit.Kits;
import io.github.arthursouto.archwright.engine.domain.model.FilePlan;
import io.github.arthursouto.archwright.engine.domain.model.GeneratedFile;
import io.github.arthursouto.archwright.engine.domain.model.GeneratedProject;
import io.github.arthursouto.archwright.engine.domain.model.LineEnding;
import io.github.arthursouto.archwright.engine.domain.model.Manifest;
import io.github.arthursouto.archwright.engine.domain.model.PlannedFile;
import io.github.arthursouto.archwright.engine.domain.model.ProjectSpec;
import io.github.arthursouto.archwright.engine.domain.model.TemplateContext;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ProjectRendererTest {

    private final Map<String, String> templates = new HashMap<>();
    private final ProjectRenderer renderer = new ProjectRenderer(new FakeTemplates(templates), new MustacheTemplateRenderer());
    private final TemplateContext context = new TemplateContext(Map.of("artifactId", "pedido-api", "name", "context-name"));

    @Test
    @DisplayName("ENG-RND-01 #1 o mesmo pedido renderizado duas vezes gera os mesmos bytes")
    void rendersTheSameRequestToIdenticalBytes() {
        ClasspathTemplateRepository repository = new ClasspathTemplateRepository();
        ContextBuilder contextBuilder = new ContextBuilder(
                new ProfileResolver(SpecBuilder.catalog()), SpecBuilder.catalog(), SpecBuilder.repository().styles(), Kits.standard());
        FilePlanner planner = new FilePlanner(repository, new MustacheTemplateRenderer(), SpecBuilder.repository().styles());
        ProjectRenderer real = new ProjectRenderer(repository, new MustacheTemplateRenderer());

        GeneratedProject first = generate(real, planner, contextBuilder, SpecBuilder.basic().extras("docker-compose").build());
        GeneratedProject second = generate(real, planner, contextBuilder, SpecBuilder.basic().extras("docker-compose").build());

        assertThat(first).isEqualTo(second);
        assertThat(first.fileCount()).isGreaterThan(60);
    }

    @Test
    @DisplayName("ENG-RND-01 #2 template com CRLF sai com LF")
    void normalizesCrlfToLf() {
        templates.put("t/a.mustache", "linha {{artifactId}}\r\nsegunda\r\n");

        GeneratedFile file = renderOne(planned("t/a.mustache", "a.txt", true, LineEnding.LF, Map.of()));

        assertThat(text(file)).isEqualTo("linha pedido-api\nsegunda\n");
    }

    @Test
    void normalizesLoneCarriageReturnsToLf() {
        templates.put("t/a.mustache", "a\rb\r\nc\n");

        assertThat(text(renderOne(planned("t/a.mustache", "a.txt", true, LineEnding.LF, Map.of())))).isEqualTo("a\nb\nc\n");
    }

    @Test
    @DisplayName("ENG-RND-01 #3 arquivo marcado como CRLF sai com CRLF")
    void convertsLfToCrlfWhenRequested() {
        templates.put("t/win.cmd", "@echo off\nexit /b 0\n");

        GeneratedFile file = renderOne(planned("t/win.cmd", "mvnw.cmd", false, LineEnding.CRLF, Map.of()));

        assertThat(text(file)).isEqualTo("@echo off\r\nexit /b 0\r\n");
    }

    @Test
    void doesNotDoubleTheCarriageReturnOfFilesThatAlreadyUseCrlf() {
        templates.put("t/win.cmd", "a\r\nb\r\n");

        assertThat(text(renderOne(planned("t/win.cmd", "win.cmd", false, LineEnding.CRLF, Map.of())))).isEqualTo("a\r\nb\r\n");
    }

    @Test
    @DisplayName("ENG-RND-01 #4 variável inexistente é erro citando o arquivo e a variável")
    void failsCitingTheTemplateAndTheMissingVariable() {
        templates.put("t/broken.mustache", "ola {{naoExiste}}");
        PlannedFile broken = planned("t/broken.mustache", "out/broken.txt", true, LineEnding.LF, Map.of());

        assertThatThrownBy(() -> renderOne(broken))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("t/broken.mustache")
                .hasMessageContaining("out/broken.txt")
                .hasMessageContaining("naoExiste");
    }

    @Test
    void copiesFilesMarkedAsNotRenderedWithoutInterpolation() {
        templates.put("t/raw.txt", "keep {{artifactId}} as is\n");

        assertThat(text(renderOne(planned("t/raw.txt", "raw.txt", false, LineEnding.LF, Map.of())))).isEqualTo("keep {{artifactId}} as is\n");
    }

    @Test
    void letsTheFileVariablesOverrideTheContext() {
        templates.put("t/info.mustache", "{{name}} / {{artifactId}}");

        GeneratedFile file = renderOne(planned("t/info.mustache", "info.txt", true, LineEnding.LF, Map.of("name", "from-plan")));

        assertThat(text(file)).isEqualTo("from-plan / pedido-api");
    }

    @Test
    void keepsAccentsAndTheExecutableFlag() {
        templates.put("t/run.sh", "#!/bin/sh\necho 'ação'\n");
        PlannedFile script = new PlannedFile("t/run.sh", "run.sh", true, LineEnding.LF, false, Map.of());

        GeneratedFile file = renderOne(script);

        assertThat(file.executable()).isTrue();
        assertThat(text(file)).contains("ação");
        assertThat(file.content()).isEqualTo("#!/bin/sh\necho 'ação'\n".getBytes(StandardCharsets.UTF_8));
    }

    @Test
    void keepsThePlanOrderAndReportsCountsAndSize() {
        templates.put("t/a", "aa");
        templates.put("t/b", "bbb");
        FilePlan plan = new FilePlan(List.of(
                planned("t/b", "b.txt", false, LineEnding.LF, Map.of()),
                planned("t/a", "a.txt", false, LineEnding.LF, Map.of())));

        GeneratedProject project = renderer.render(plan, context);

        assertThat(project.files()).extracting(GeneratedFile::path).containsExactly("b.txt", "a.txt");
        assertThat(project.fileCount()).isEqualTo(2);
        assertThat(project.totalBytes()).isEqualTo(5);
        assertThat(project.find("a.txt")).isPresent();
        assertThat(project.find("c.txt")).isEmpty();
    }

    @Test
    void generatedFileProtectsItsContentFromOutsideChanges() {
        byte[] original = "abc".getBytes(StandardCharsets.UTF_8);
        GeneratedFile file = new GeneratedFile("a.txt", original, false);

        original[0] = 'X';
        file.content()[1] = 'Y';

        assertThat(text(file)).isEqualTo("abc");
        assertThat(file).isEqualTo(new GeneratedFile("a.txt", "abc".getBytes(StandardCharsets.UTF_8), false));
        assertThat(file).isNotEqualTo(new GeneratedFile("a.txt", "abc".getBytes(StandardCharsets.UTF_8), true));
        assertThat(file).hasSameHashCodeAs(new GeneratedFile("a.txt", "abc".getBytes(StandardCharsets.UTF_8), false));
    }

    private GeneratedFile renderOne(PlannedFile file) {
        return renderer.render(new FilePlan(List.of(file)), context).files().getFirst();
    }

    private static PlannedFile planned(String template, String target, boolean render, LineEnding lineEnding, Map<String, String> variables) {
        return new PlannedFile(template, target, false, lineEnding, render, variables);
    }

    private static String text(GeneratedFile file) {
        return new String(file.content(), StandardCharsets.UTF_8);
    }

    private static GeneratedProject generate(
            ProjectRenderer renderer, FilePlanner planner, ContextBuilder contextBuilder, ProjectSpec spec) {
        TemplateContext context = contextBuilder.build(spec);

        return renderer.render(planner.plan(spec, context), context);
    }

    private static final class FakeTemplates implements TemplateRepository {

        private final Map<String, String> templates;

        private FakeTemplates(Map<String, String> templates) {
            this.templates = templates;
        }

        @Override
        public Manifest manifest(String directory) {
            throw new UnsupportedOperationException("the renderer never reads manifests");
        }

        @Override
        public byte[] content(String path) {
            return templates.get(path).getBytes(StandardCharsets.UTF_8);
        }
    }
}
