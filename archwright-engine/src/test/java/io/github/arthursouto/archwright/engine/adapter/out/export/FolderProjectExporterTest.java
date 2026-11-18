package io.github.arthursouto.archwright.engine.adapter.out.export;

import io.github.arthursouto.archwright.engine.application.port.in.ExportFormat;
import io.github.arthursouto.archwright.engine.application.port.in.ExportTarget;
import io.github.arthursouto.archwright.engine.application.port.out.ExportResult;
import io.github.arthursouto.archwright.engine.domain.model.GeneratedProject;
import io.github.arthursouto.archwright.engine.domain.validation.Violation;
import io.github.arthursouto.archwright.engine.domain.validation.ViolationCode;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.FileAlreadyExistsException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.PosixFilePermissions;
import java.util.List;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.api.Assumptions.assumeFalse;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

class FolderProjectExporterTest {

    @TempDir
    Path destination;

    private final FolderProjectExporter exporter = new FolderProjectExporter();

    @Test
    void exportsTheFolderFormat() {
        assertThat(exporter.format()).isEqualTo(ExportFormat.FOLDER);
    }

    @Test
    @DisplayName("ENG-EXP-02 #1 destino sem a pasta: cria <destino>/<artifactId>/ com todos os arquivos")
    void createsTheProjectFolderWithEveryFile() throws IOException {
        ExportResult result = exporter.write(SampleProject.pedidoApi(), target());

        Path folder = destination.resolve("pedido-api");

        assertThat(result.output()).isEqualTo(folder.toAbsolutePath().normalize());
        assertThat(read(folder.resolve("pom.xml"))).isEqualTo("<project/>\n");
        assertThat(read(folder.resolve("src/main/java/com/acme/App.java"))).isEqualTo("class App {}\n");
        assertThat(read(folder.resolve("src/main/resources/application.yaml"))).isEqualTo("server:\n  port: 8080\n");
        assertThat(relativeFiles(folder)).containsExactly(
                "mvnw", "pom.xml", "src/main/java/com/acme/App.java", "src/main/resources/application.yaml");
    }

    @Test
    @DisplayName("ENG-EXP-02 #2 pasta existente e vazia: usa a pasta")
    void usesAnExistingEmptyFolder() throws IOException {
        Files.createDirectory(destination.resolve("pedido-api"));

        assertThat(exporter.check(target(), "pedido-api")).isEmpty();

        exporter.write(SampleProject.pedidoApi(), target());

        assertThat(destination.resolve("pedido-api/pom.xml")).exists();
    }

    @Test
    @DisplayName("ENG-EXP-02 #3 pasta com um arquivo: check devolve TARGET_NOT_EMPTY e nada é alterado")
    void refusesAFolderThatIsNotEmpty() throws IOException {
        Path folder = Files.createDirectory(destination.resolve("pedido-api"));
        Files.writeString(folder.resolve("meu-arquivo.txt"), "meu conteúdo");

        List<Violation> violations = exporter.check(target(), "pedido-api");

        assertThat(violations).singleElement().satisfies(violation -> {
            assertThat(violation.code()).isEqualTo(ViolationCode.TARGET_NOT_EMPTY);
            assertThat(violation.params()).containsEntry("path", folder.toAbsolutePath().normalize().toString());
        });
        assertThat(relativeFiles(folder)).containsExactly("meu-arquivo.txt");
        assertThat(read(folder.resolve("meu-arquivo.txt"))).isEqualTo("meu conteúdo");
    }

    @Test
    void refusesATargetThatIsAFileInsteadOfAFolder() throws IOException {
        Files.writeString(destination.resolve("pedido-api"), "sou um arquivo");

        assertThat(exporter.check(target(), "pedido-api")).extracting(Violation::code)
                .containsExactly(ViolationCode.TARGET_NOT_EMPTY);
    }

    @Test
    @DisplayName("BR-062 nunca sobrescreve: gravar sobre pasta ocupada falha e preserva o conteúdo")
    void neverOverwritesEvenIfCheckWasSkipped() throws IOException {
        Path folder = Files.createDirectory(destination.resolve("pedido-api"));
        Files.writeString(folder.resolve("meu-arquivo.txt"), "meu conteúdo");

        assertThatThrownBy(() -> exporter.write(SampleProject.pedidoApi(), target()))
                .isInstanceOf(FileAlreadyExistsException.class);

        assertThat(relativeFiles(folder)).containsExactly("meu-arquivo.txt");
        assertThat(read(folder.resolve("meu-arquivo.txt"))).isEqualTo("meu conteúdo");
        assertThat(directoryContent()).containsExactly("pedido-api");
    }

    @Test
    @DisplayName("ENG-EXP-02 #4 em sistema POSIX o mvnw sai executável e os demais não")
    void marksOnlyTheWrapperAsExecutable() throws IOException {
        assumePosix();

        exporter.write(SampleProject.pedidoApi(), target());
        Path folder = destination.resolve("pedido-api");

        assertThat(Files.isExecutable(folder.resolve("mvnw"))).isTrue();
        assertThat(Files.isExecutable(folder.resolve("pom.xml"))).isFalse();
        assertThat(Files.isExecutable(folder.resolve("src/main/java/com/acme/App.java"))).isFalse();
    }

    @Test
    void createsTheProjectFolderTraversableByEveryone() throws IOException {
        assumePosix();

        exporter.write(SampleProject.pedidoApi(), target());

        assertThat(PosixFilePermissions.toString(Files.getPosixFilePermissions(destination.resolve("pedido-api"))))
                .isEqualTo("rwxr-xr-x");
    }

    @Test
    @DisplayName("ENG-EXP-02 #5 falha no meio da gravação: pasta temporária removida e a pasta final não existe")
    void cleansUpWhenWritingFailsHalfway() {
        GeneratedProject conflicting = SampleProject.withFiles(
                SampleProject.file("a.txt", "arquivo", false),
                SampleProject.file("a.txt/dentro.txt", "impossível: a.txt é um arquivo", false));

        assertThatThrownBy(() -> exporter.write(conflicting, target())).isInstanceOf(IOException.class);

        assertThat(directoryContent()).isEmpty();
    }

    @Test
    void keepsAnExistingEmptyFolderWhenWritingFails() throws IOException {
        Files.createDirectory(destination.resolve("pedido-api"));
        GeneratedProject conflicting = SampleProject.withFiles(
                SampleProject.file("a.txt", "arquivo", false),
                SampleProject.file("a.txt/dentro.txt", "x", false));

        assertThatThrownBy(() -> exporter.write(conflicting, target())).isInstanceOf(IOException.class);

        assertThat(directoryContent()).containsExactly("pedido-api");
        assertThat(relativeFiles(destination.resolve("pedido-api"))).isEmpty();
    }

    @Test
    void refusesPathsThatWouldEscapeTheProjectFolder() {
        GeneratedProject evil = SampleProject.withFiles(SampleProject.file("../fora.txt", "x", false));

        assertThatThrownBy(() -> exporter.write(evil, target()))
                .isInstanceOf(IOException.class)
                .hasMessageContaining("unsafe path");

        assertThat(directoryContent()).isEmpty();
        assertThat(destination.resolveSibling("fora.txt")).doesNotExist();
    }

    @Test
    void refusesAbsolutePaths() {
        GeneratedProject absolute = SampleProject.withFiles(SampleProject.file("/tmp/absoluto.txt", "x", false));

        assertThatThrownBy(() -> exporter.write(absolute, target())).isInstanceOf(IOException.class);

        assertThat(directoryContent()).isEmpty();
    }

    @Test
    @DisplayName("ENG-EXP-02 #6 destino sem permissão de escrita: check devolve DESTINATION_NOT_WRITABLE")
    void reportsADestinationWithoutWritePermission() throws IOException {
        assumePosix();
        Files.setPosixFilePermissions(destination, PosixFilePermissions.fromString("r-xr-xr-x"));

        try {
            assumeFalse(Files.isWritable(destination), "running with privileges that ignore permissions");

            assertThat(exporter.check(target(), "pedido-api")).extracting(Violation::code)
                    .containsExactly(ViolationCode.DESTINATION_NOT_WRITABLE);
        } finally {
            Files.setPosixFilePermissions(destination, PosixFilePermissions.fromString("rwxr-xr-x"));
        }
    }

    @Test
    void reportsAMissingDestination() {
        ExportTarget missing = new ExportTarget(ExportFormat.FOLDER, destination.resolve("nao-existe"), false);

        assertThat(exporter.check(missing, "pedido-api")).extracting(Violation::code)
                .containsExactly(ViolationCode.DESTINATION_NOT_FOUND);
    }

    @Test
    @DisplayName("BR-064 o resultado informa o caminho absoluto, a quantidade de arquivos e o tamanho total")
    void reportsPathCountAndSize() throws IOException {
        GeneratedProject project = SampleProject.pedidoApi();

        ExportResult result = exporter.write(project, target());

        assertThat(result.output()).isAbsolute();
        assertThat(result.fileCount()).isEqualTo(4);
        assertThat(result.totalBytes()).isEqualTo(project.totalBytes());
    }

    @Test
    void leavesNoTemporaryFolderAfterASuccessfulExport() throws IOException {
        exporter.write(SampleProject.pedidoApi(), target());

        assertThat(directoryContent()).containsExactly("pedido-api");
    }

    @Test
    void keepsNonAsciiContentAndCrlfFilesExactly() throws IOException {
        GeneratedProject project = SampleProject.withFiles(
                SampleProject.file("mvnw.cmd", "@echo off\r\nexit /b 0\r\n", false),
                SampleProject.file("README.md", "ação & coração\n", false));

        exporter.write(project, target());

        assertThat(Files.readAllBytes(destination.resolve("pedido-api/mvnw.cmd")))
                .isEqualTo("@echo off\r\nexit /b 0\r\n".getBytes(StandardCharsets.UTF_8));
        assertThat(read(destination.resolve("pedido-api/README.md"))).isEqualTo("ação & coração\n");
    }

    private ExportTarget target() {
        return new ExportTarget(ExportFormat.FOLDER, destination, false);
    }

    private String read(Path file) throws IOException {
        return Files.readString(file, StandardCharsets.UTF_8);
    }

    private List<String> directoryContent() {
        try (Stream<Path> files = Files.list(destination)) {
            return files.map(path -> path.getFileName().toString()).sorted().toList();
        } catch (IOException exception) {
            throw new AssertionError(exception);
        }
    }

    private List<String> relativeFiles(Path root) throws IOException {
        try (Stream<Path> files = Files.walk(root)) {
            return files.filter(Files::isRegularFile)
                    .map(file -> root.relativize(file).toString().replace('\\', '/'))
                    .sorted()
                    .toList();
        }
    }

    private void assumePosix() {
        assumeTrue(destination.getFileSystem().supportedFileAttributeViews().contains("posix"));
    }
}
