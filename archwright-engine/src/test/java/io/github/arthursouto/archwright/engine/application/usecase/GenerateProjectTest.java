package io.github.arthursouto.archwright.engine.application.usecase;

import io.github.arthursouto.archwright.engine.adapter.out.template.ClasspathTemplateRepository;
import io.github.arthursouto.archwright.engine.adapter.out.template.MustacheTemplateRenderer;
import io.github.arthursouto.archwright.engine.application.port.in.ExportFormat;
import io.github.arthursouto.archwright.engine.application.port.in.ExportTarget;
import io.github.arthursouto.archwright.engine.application.port.in.GenerationResult;
import io.github.arthursouto.archwright.engine.application.port.out.ExportResult;
import io.github.arthursouto.archwright.engine.application.port.out.ProjectExporter;
import io.github.arthursouto.archwright.engine.application.port.out.TemplateRepository;
import io.github.arthursouto.archwright.engine.domain.model.GeneratedProject;
import io.github.arthursouto.archwright.engine.domain.model.Manifest;
import io.github.arthursouto.archwright.engine.domain.validation.Violation;
import io.github.arthursouto.archwright.engine.domain.validation.ViolationCode;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class GenerateProjectTest {

    private static final Path DESTINATION = Path.of("/tmp/aw-destination");

    private final ClasspathTemplateRepository realTemplates = new ClasspathTemplateRepository();
    private final AtomicInteger templateReads = new AtomicInteger();
    private final FakeExporter exporter = new FakeExporter(ExportFormat.ZIP);
    private final ExportTarget target = new ExportTarget(ExportFormat.ZIP, DESTINATION, false);

    @Test
    @DisplayName("ENG-UC-01 #1 pedido válido em ZIP devolve Success com caminho, contagem e avisos")
    void returnsSuccessWithOutputCountAndWarnings() {
        GenerateProject generate = generator(countingTemplates());

        GenerationResult result = generate.generate(UseCaseFixtures.validRequest(), target);

        GenerationResult.Success success = (GenerationResult.Success) result;
        assertThat(success.output()).isEqualTo(DESTINATION.resolve("pedido-api.zip"));
        assertThat(success.fileCount()).isEqualTo(exporter.written.getFirst().fileCount()).isGreaterThan(60);
        assertThat(success.totalBytes()).isEqualTo(exporter.written.getFirst().totalBytes());
        assertThat(success.warnings()).isEmpty();
        assertThat(exporter.checkedArtifactIds).containsExactly("pedido-api");
    }

    @Test
    @DisplayName("ENG-UC-01 #2 pedido com duas violações devolve as duas e nada é renderizado nem verificado no destino")
    void reportsRequestViolationsWithoutTouchingTheDestination() {
        GenerateProject generate = generator(countingTemplates());

        GenerationResult result = generate.generate(UseCaseFixtures.withTwoViolations(), target);

        GenerationResult.Failure failure = (GenerationResult.Failure) result;
        assertThat(failure.violations()).extracting(Violation::code)
                .containsExactlyInAnyOrder(ViolationCode.INVALID_ARTIFACT_ID, ViolationCode.UNKNOWN_EXTRA);
        assertThat(templateReads).hasValue(0);
        assertThat(exporter.written).isEmpty();
        assertThat(exporter.checkedArtifactIds).isEmpty();
    }

    @Test
    @DisplayName("ENG-UC-01 #3 template quebrado propaga a exceção e nada é gravado")
    void propagatesRenderingBugsWithoutWriting() {
        GenerateProject generate = new GenerateProject(
                UseCaseFixtures.realPlanning(),
                new ProjectRenderer(brokenTemplates(), new MustacheTemplateRenderer()),
                List.of(exporter));

        assertThatThrownBy(() -> generate.generate(UseCaseFixtures.validRequest(), target))
                .isInstanceOf(IllegalStateException.class);

        assertThat(exporter.written).isEmpty();
    }

    @Test
    @DisplayName("ENG-UC-01 #4 destino ocupado devolve a violação do exportador e nada é renderizado")
    void reportsTheOccupiedTargetBeforeRendering() {
        exporter.violations = List.of(Violation.of(ViolationCode.TARGET_NOT_EMPTY, "destination", "path", "/tmp/x/pedido-api"));
        GenerateProject generate = generator(countingTemplates());

        GenerationResult result = generate.generate(UseCaseFixtures.validRequest(), new ExportTarget(ExportFormat.ZIP, DESTINATION, false));

        assertThat(((GenerationResult.Failure) result).violations()).extracting(Violation::code)
                .containsExactly(ViolationCode.TARGET_NOT_EMPTY);
        assertThat(exporter.written).isEmpty();
    }

    @Test
    void failsWhenNoExporterHandlesTheRequestedFormat() {
        GenerateProject generate = generator(countingTemplates());
        ExportTarget folder = new ExportTarget(ExportFormat.FOLDER, DESTINATION, false);

        assertThatThrownBy(() -> generate.generate(UseCaseFixtures.validRequest(), folder))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("FOLDER");
    }

    @Test
    void wrapsInputOutputFailuresFromTheExporter() {
        exporter.failure = new IOException("disk full");
        GenerateProject generate = generator(countingTemplates());

        assertThatThrownBy(() -> generate.generate(UseCaseFixtures.validRequest(), target))
                .isInstanceOf(UncheckedIOException.class)
                .hasRootCauseMessage("disk full");
    }

    private GenerateProject generator(TemplateRepository templates) {
        return new GenerateProject(
                UseCaseFixtures.planning(templates, new MustacheTemplateRenderer()),
                new ProjectRenderer(templates, new MustacheTemplateRenderer()),
                List.of(exporter));
    }

    private TemplateRepository countingTemplates() {
        return new TemplateRepository() {
            @Override
            public Manifest manifest(String directory) {
                return realTemplates.manifest(directory);
            }

            @Override
            public byte[] content(String path) {
                templateReads.incrementAndGet();
                return realTemplates.content(path);
            }
        };
    }

    private TemplateRepository brokenTemplates() {
        return new TemplateRepository() {
            @Override
            public Manifest manifest(String directory) {
                return realTemplates.manifest(directory);
            }

            @Override
            public byte[] content(String path) {
                throw new IllegalStateException("broken template " + path);
            }
        };
    }

    private static final class FakeExporter implements ProjectExporter {

        private final ExportFormat format;
        private final List<String> checkedArtifactIds = new ArrayList<>();
        private final List<ExportResult> written = new ArrayList<>();
        private List<Violation> violations = List.of();
        private IOException failure;

        private FakeExporter(ExportFormat format) {
            this.format = format;
        }

        @Override
        public ExportFormat format() {
            return format;
        }

        @Override
        public List<Violation> check(ExportTarget target, String artifactId) {
            checkedArtifactIds.add(artifactId);
            return violations;
        }

        @Override
        public ExportResult write(GeneratedProject project, ExportTarget target) throws IOException {
            if (failure != null) {
                throw failure;
            }

            ExportResult result = new ExportResult(
                    target.destination().resolve(project.artifactId() + ".zip"), project.fileCount(), project.totalBytes());
            written.add(result);

            return result;
        }
    }
}
