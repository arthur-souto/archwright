package io.github.arthursouto.archwright.engine.config;

import io.github.arthursouto.archwright.engine.application.port.in.ExportFormat;
import io.github.arthursouto.archwright.engine.application.port.in.ExportTarget;
import io.github.arthursouto.archwright.engine.application.port.in.GenerationResult;
import io.github.arthursouto.archwright.engine.application.port.in.PreviewResult;
import io.github.arthursouto.archwright.engine.application.port.in.ProjectRequest;
import io.github.arthursouto.archwright.engine.application.port.in.VersionOrigin;
import io.github.arthursouto.archwright.engine.domain.validation.Violation;
import io.github.arthursouto.archwright.engine.domain.validation.ViolationCode;
import org.apache.commons.compress.archivers.zip.ZipArchiveEntry;
import org.apache.commons.compress.archivers.zip.ZipFile;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collections;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class EngineEndToEndTest {

    @TempDir
    Path workspace;

    private ArchwrightEngine engine;
    private Path destination;

    @BeforeEach
    void createEngine() throws IOException {
        destination = Files.createDirectory(workspace.resolve("out"));
        engine = ArchwrightEngine.builder().offline(true).cacheDirectory(workspace.resolve("cache")).build();
    }

    @Test
    @DisplayName("ENG-UC-01 gera um ZIP real com o catálogo oficial")
    void generatesARealZipFromTheOfficialCatalog() throws IOException {
        GenerationResult result = engine.generateProject().generate(request(), new ExportTarget(ExportFormat.ZIP, destination, false));

        GenerationResult.Success success = (GenerationResult.Success) result;
        assertThat(success.output()).isEqualTo(destination.resolve("pedido-api.zip").toAbsolutePath());
        assertThat(success.output()).isRegularFile();
        assertThat(entries(success.output())).hasSize(success.fileCount())
                .contains("pedido-api/pom.xml", "pedido-api/mvnw", "pedido-api/.mvn/wrapper/maven-wrapper.properties");
    }

    @Test
    void generatesAFolderWithTheSameFilesAsTheZip() throws IOException {
        GenerationResult.Success zip = (GenerationResult.Success) engine.generateProject()
                .generate(request(), new ExportTarget(ExportFormat.ZIP, destination, false));
        GenerationResult.Success folder = (GenerationResult.Success) engine.generateProject()
                .generate(request(), new ExportTarget(ExportFormat.FOLDER, destination, false));

        assertThat(folder.output()).isDirectory();
        assertThat(folder.fileCount()).isEqualTo(zip.fileCount());
        assertThat(folder.output().resolve("pom.xml")).isRegularFile();
    }

    @Test
    void refusesToOverwriteAnExistingZipUnlessAskedTo() {
        ExportTarget keep = new ExportTarget(ExportFormat.ZIP, destination, false);
        ExportTarget replace = new ExportTarget(ExportFormat.ZIP, destination, true);

        engine.generateProject().generate(request(), keep);

        GenerationResult second = engine.generateProject().generate(request(), keep);
        GenerationResult third = engine.generateProject().generate(request(), replace);

        assertThat(((GenerationResult.Failure) second).violations()).extracting(Violation::code)
                .containsExactly(ViolationCode.TARGET_EXISTS);
        assertThat(third).isInstanceOf(GenerationResult.Success.class);
    }

    @Test
    void writesNothingWhenTheRequestIsInvalid() throws IOException {
        ProjectRequest invalid = new ProjectRequest(
                "Pedido API", "com.acme", null, null, "HEXAGONAL_MODULAR", List.of("pedidos"),
                "4.1.2", "25", "NONE", null, null, true);

        GenerationResult result = engine.generateProject().generate(invalid, new ExportTarget(ExportFormat.ZIP, destination, false));

        assertThat(result).isInstanceOf(GenerationResult.Failure.class);
        try (var children = Files.list(destination)) {
            assertThat(children).isEmpty();
        }
    }

    @Test
    void previewsTheSamePathsThatTheZipContains() throws IOException {
        PreviewResult.Preview preview = (PreviewResult.Preview) engine.previewProject().preview(request());
        GenerationResult.Success success = (GenerationResult.Success) engine.generateProject()
                .generate(request(), new ExportTarget(ExportFormat.ZIP, destination, false));

        List<String> inZip = entries(success.output()).stream().map(entry -> entry.substring("pedido-api/".length())).sorted().toList();

        assertThat(preview.paths()).isEqualTo(inZip);
    }

    @Test
    void servesTheCatalogOfflineFromTheEmbeddedVersions() {
        assertThat(engine.catalog().bootVersions().origin()).isEqualTo(VersionOrigin.EMBEDDED);
        assertThat(engine.catalog().architectureStyles()).hasSize(2);
        assertThat(engine.catalog().kits()).hasSize(2);
        assertThat(engine.catalog().dependencies("4.1.2")).isNotEmpty();
        assertThat(engine.catalog().extras()).hasSize(2);
        assertThat(engine.catalog().suggestBasePackage("com.acme", "pedido-api")).isEqualTo("com.acme.pedidoapi");
    }

    @Test
    void createsTheEngineWithDefaults() {
        assertThat(ArchwrightEngine.create().catalog().kits()).hasSize(2);
    }

    private static ProjectRequest request() {
        return new ProjectRequest(
                "pedido-api", "com.acme", null, "API de pedidos", "HEXAGONAL_MODULAR", List.of("pedidos", "pagamentos"),
                "4.1.2", "25", "BASIC", List.of("web", "validation", "actuator", "jpa", "flyway", "lombok"),
                List.of("docker-compose", "github-actions"), true);
    }

    private static List<String> entries(Path zip) throws IOException {
        try (ZipFile file = ZipFile.builder().setPath(zip).get()) {
            return Collections.list(file.getEntries()).stream().map(ZipArchiveEntry::getName).toList();
        }
    }
}
