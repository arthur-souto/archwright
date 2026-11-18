package io.github.arthursouto.archwright.engine.adapter.out.export;

import io.github.arthursouto.archwright.engine.application.port.in.ExportFormat;
import io.github.arthursouto.archwright.engine.application.port.in.ExportTarget;
import io.github.arthursouto.archwright.engine.application.port.out.ExportResult;
import io.github.arthursouto.archwright.engine.domain.model.GeneratedProject;
import io.github.arthursouto.archwright.engine.domain.validation.Violation;
import io.github.arthursouto.archwright.engine.domain.validation.ViolationCode;
import org.apache.commons.compress.archivers.zip.ZipArchiveEntry;
import org.apache.commons.compress.archivers.zip.ZipFile;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.FileAlreadyExistsException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.PosixFilePermissions;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Collections;
import java.util.List;
import java.util.TimeZone;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.api.Assumptions.assumeFalse;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

class ZipProjectExporterTest {

    @TempDir
    Path destination;

    private final ZipProjectExporter exporter = new ZipProjectExporter();

    @Test
    void exportsTheZipFormat() {
        assertThat(exporter.format()).isEqualTo(ExportFormat.ZIP);
    }

    @Test
    @DisplayName("ENG-EXP-01 #1 cria <destino>/<artifactId>.zip com todas as entradas dentro de <artifactId>/")
    void createsTheZipWithEveryEntryInsideTheProjectFolder() throws IOException {
        ExportResult result = exporter.write(SampleProject.pedidoApi(), target(false));

        assertThat(result.output()).isEqualTo(destination.resolve("pedido-api.zip").toAbsolutePath().normalize());
        assertThat(entryNames(result.output())).containsExactly(
                "pedido-api/mvnw",
                "pedido-api/pom.xml",
                "pedido-api/src/main/java/com/acme/App.java",
                "pedido-api/src/main/resources/application.yaml");
    }

    @Test
    @DisplayName("ENG-EXP-01 #2 mvnw tem modo Unix 755 e os demais 644")
    void marksOnlyTheWrapperAsExecutable() throws IOException {
        Path zip = exporter.write(SampleProject.pedidoApi(), target(false)).output();

        try (ZipFile archive = ZipFile.builder().setPath(zip).get()) {
            assertThat(archive.getEntry("pedido-api/mvnw").getUnixMode()).isEqualTo(0755);
            assertThat(archive.getEntry("pedido-api/pom.xml").getUnixMode()).isEqualTo(0644);
            assertThat(archive.getEntry("pedido-api/src/main/java/com/acme/App.java").getUnixMode()).isEqualTo(0644);
        }
    }

    @Test
    @DisplayName("ENG-EXP-01 #3 o mesmo projeto exportado duas vezes gera arquivos idênticos")
    void producesIdenticalBytesForTheSameProject() throws IOException {
        Path first = Files.createDirectory(destination.resolve("first"));
        Path second = Files.createDirectory(destination.resolve("second"));

        Path zipA = exporter.write(SampleProject.pedidoApi(), new ExportTarget(ExportFormat.ZIP, first, false)).output();
        Path zipB = exporter.write(SampleProject.pedidoApi(), new ExportTarget(ExportFormat.ZIP, second, false)).output();

        assertThat(Files.readAllBytes(zipA)).isEqualTo(Files.readAllBytes(zipB));
    }

    @Test
    @DisplayName("BR-050 os bytes do ZIP não dependem do fuso horário da máquina")
    void producesIdenticalBytesInDifferentTimeZones() throws IOException {
        Path west = Files.createDirectory(destination.resolve("west"));
        Path east = Files.createDirectory(destination.resolve("east"));
        TimeZone original = TimeZone.getDefault();

        try {
            TimeZone.setDefault(TimeZone.getTimeZone("America/Los_Angeles"));
            Path zipWest = exporter.write(SampleProject.pedidoApi(), new ExportTarget(ExportFormat.ZIP, west, false)).output();

            TimeZone.setDefault(TimeZone.getTimeZone("Pacific/Kiritimati"));
            Path zipEast = exporter.write(SampleProject.pedidoApi(), new ExportTarget(ExportFormat.ZIP, east, false)).output();

            assertThat(Files.readAllBytes(zipWest)).isEqualTo(Files.readAllBytes(zipEast));
        } finally {
            TimeZone.setDefault(original);
        }
    }

    @Test
    void usesTheFixedEntryTimeInsteadOfTheCurrentTime() throws IOException {
        Path zip = exporter.write(SampleProject.pedidoApi(), target(false)).output();

        try (ZipFile archive = ZipFile.builder().setPath(zip).get()) {
            ZipArchiveEntry entry = archive.getEntry("pedido-api/pom.xml");
            LocalDateTime time = Instant.ofEpochMilli(entry.getTime()).atZone(ZoneId.systemDefault()).toLocalDateTime();

            assertThat(time).isEqualTo(LocalDateTime.of(1980, 1, 1, 0, 0, 0));
        }
    }

    @Test
    void keepsTheExactContentOfEachFile() throws IOException {
        Path zip = exporter.write(SampleProject.pedidoApi(), target(false)).output();

        try (ZipFile archive = ZipFile.builder().setPath(zip).get()) {
            String pom = new String(archive.getInputStream(archive.getEntry("pedido-api/pom.xml")).readAllBytes(), StandardCharsets.UTF_8);

            assertThat(pom).isEqualTo("<project/>\n");
        }
    }

    @Test
    @DisplayName("BR-064 o resultado informa o caminho absoluto, a quantidade de arquivos e o tamanho total")
    void reportsPathCountAndSize() throws IOException {
        GeneratedProject project = SampleProject.pedidoApi();

        ExportResult result = exporter.write(project, target(false));

        assertThat(result.output()).isAbsolute();
        assertThat(result.fileCount()).isEqualTo(4);
        assertThat(result.totalBytes()).isEqualTo(project.totalBytes());
    }

    @Test
    @DisplayName("ENG-EXP-01 #4 zip existente sem overwrite: check devolve TARGET_EXISTS")
    void reportsAnExistingZipWhenOverwriteIsOff() throws IOException {
        exporter.write(SampleProject.pedidoApi(), target(false));

        List<Violation> violations = exporter.check(target(false), "pedido-api");

        assertThat(violations).singleElement().satisfies(violation -> {
            assertThat(violation.code()).isEqualTo(ViolationCode.TARGET_EXISTS);
            assertThat(violation.field()).isEqualTo("destination");
            assertThat(violation.params()).containsEntry("path", destination.resolve("pedido-api.zip").toAbsolutePath().normalize().toString());
        });
    }

    @Test
    void acceptsAnExistingZipWhenOverwriteIsOn() throws IOException {
        exporter.write(SampleProject.pedidoApi(), target(false));

        assertThat(exporter.check(target(true), "pedido-api")).isEmpty();
    }

    @Test
    void acceptsAFreshDestination() {
        assertThat(exporter.check(target(false), "pedido-api")).isEmpty();
    }

    @Test
    void neverReplacesADirectoryEvenWithOverwrite() throws IOException {
        Files.createDirectory(destination.resolve("pedido-api.zip"));

        assertThat(exporter.check(target(true), "pedido-api")).extracting(Violation::code)
                .containsExactly(ViolationCode.TARGET_EXISTS);
    }

    @Test
    @DisplayName("ENG-EXP-01 #5 zip existente com overwrite: substitui o arquivo")
    void replacesTheExistingZipWhenOverwriteIsOn() throws IOException {
        exporter.write(SampleProject.pedidoApi(), target(false));
        GeneratedProject other = SampleProject.withFiles(SampleProject.file("other.txt", "novo\n", false));

        Path zip = exporter.write(other, target(true)).output();

        assertThat(entryNames(zip)).containsExactly("pedido-api/other.txt");
    }

    @Test
    void refusesToOverwriteWhenTheFlagIsOffEvenIfCheckWasSkipped() throws IOException {
        Path zip = exporter.write(SampleProject.pedidoApi(), target(false)).output();
        byte[] original = Files.readAllBytes(zip);

        assertThatThrownBy(() -> exporter.write(SampleProject.pedidoApi(), target(false)))
                .isInstanceOf(FileAlreadyExistsException.class);

        assertThat(Files.readAllBytes(zip)).isEqualTo(original);
        assertThat(directoryContent()).containsExactly("pedido-api.zip");
    }

    @Test
    @DisplayName("ENG-EXP-01 #6 destino inexistente: check devolve DESTINATION_NOT_FOUND")
    void reportsAMissingDestination() {
        ExportTarget missing = new ExportTarget(ExportFormat.ZIP, destination.resolve("nao-existe"), false);

        assertThat(exporter.check(missing, "pedido-api")).extracting(Violation::code)
                .containsExactly(ViolationCode.DESTINATION_NOT_FOUND);
    }

    @Test
    void reportsADestinationThatIsAFile() throws IOException {
        Path file = Files.writeString(destination.resolve("arquivo.txt"), "x");

        assertThat(exporter.check(new ExportTarget(ExportFormat.ZIP, file, false), "pedido-api"))
                .extracting(Violation::code).containsExactly(ViolationCode.DESTINATION_NOT_FOUND);
    }

    @Test
    void reportsADestinationWithoutWritePermission() throws IOException {
        assumePosix();
        Files.setPosixFilePermissions(destination, PosixFilePermissions.fromString("r-xr-xr-x"));

        try {
            assumeFalse(Files.isWritable(destination), "running with privileges that ignore permissions");

            assertThat(exporter.check(target(false), "pedido-api")).extracting(Violation::code)
                    .containsExactly(ViolationCode.DESTINATION_NOT_WRITABLE);
        } finally {
            Files.setPosixFilePermissions(destination, PosixFilePermissions.fromString("rwxr-xr-x"));
        }
    }

    @Test
    @DisplayName("ENG-EXP-01 #7 falha no meio da gravação: nenhum .zip parcial nem temporário fica no destino")
    void leavesNoPartialFileWhenWritingFails() {
        ZipProjectExporter failing = new ZipProjectExporter((project, output) -> {
            output.write("partial".getBytes(StandardCharsets.UTF_8));
            throw new IOException("disk full");
        });

        assertThatThrownBy(() -> failing.write(SampleProject.pedidoApi(), target(false)))
                .isInstanceOf(IOException.class)
                .hasMessageContaining("disk full");

        assertThat(directoryContent()).isEmpty();
    }

    @Test
    void keepsTheExistingZipWhenReplacingItFails() throws IOException {
        Path zip = exporter.write(SampleProject.pedidoApi(), target(false)).output();
        byte[] original = Files.readAllBytes(zip);
        ZipProjectExporter failing = new ZipProjectExporter((project, output) -> {
            throw new IOException("disk full");
        });

        assertThatThrownBy(() -> failing.write(SampleProject.pedidoApi(), target(true))).isInstanceOf(IOException.class);

        assertThat(Files.readAllBytes(zip)).isEqualTo(original);
        assertThat(directoryContent()).containsExactly("pedido-api.zip");
    }

    @Test
    void refusesPathsThatWouldEscapeTheProjectFolder() {
        GeneratedProject evil = SampleProject.withFiles(SampleProject.file("../evil.txt", "x", false));

        assertThatThrownBy(() -> exporter.write(evil, target(false)))
                .isInstanceOf(IOException.class)
                .hasMessageContaining("unsafe path");

        assertThat(directoryContent()).isEmpty();
    }

    @Test
    void leavesOnlyTheZipAfterASuccessfulExport() throws IOException {
        exporter.write(SampleProject.pedidoApi(), target(false));

        assertThat(directoryContent()).containsExactly("pedido-api.zip");
    }

    @Test
    void createsTheZipReadableByEveryone() throws IOException {
        assumePosix();

        Path zip = exporter.write(SampleProject.pedidoApi(), target(false)).output();

        assertThat(PosixFilePermissions.toString(Files.getPosixFilePermissions(zip))).isEqualTo("rw-r--r--");
    }

    private ExportTarget target(boolean overwrite) {
        return new ExportTarget(ExportFormat.ZIP, destination, overwrite);
    }

    private List<String> directoryContent() {
        try (Stream<Path> files = Files.list(destination)) {
            return files.map(path -> path.getFileName().toString()).sorted().toList();
        } catch (IOException exception) {
            throw new AssertionError(exception);
        }
    }

    private List<String> entryNames(Path zip) throws IOException {
        try (ZipFile archive = ZipFile.builder().setPath(zip).get()) {
            return Collections.list(archive.getEntries()).stream().map(ZipArchiveEntry::getName).toList();
        }
    }

    private void assumePosix() {
        assumeTrue(destination.getFileSystem().supportedFileAttributeViews().contains("posix"));
    }
}
