package io.github.arthursouto.archwright.engine.adapter.out.export;

import io.github.arthursouto.archwright.engine.application.port.in.ExportFormat;
import io.github.arthursouto.archwright.engine.application.port.in.ExportTarget;
import io.github.arthursouto.archwright.engine.application.port.out.ExportResult;
import io.github.arthursouto.archwright.engine.application.port.out.ProjectExporter;
import io.github.arthursouto.archwright.engine.domain.model.GeneratedFile;
import io.github.arthursouto.archwright.engine.domain.model.GeneratedProject;
import io.github.arthursouto.archwright.engine.domain.validation.Violation;
import io.github.arthursouto.archwright.engine.domain.validation.ViolationCode;
import org.apache.commons.compress.archivers.zip.ZipArchiveEntry;
import org.apache.commons.compress.archivers.zip.ZipArchiveOutputStream;

import java.io.BufferedOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.nio.file.attribute.PosixFilePermissions;
import java.time.LocalDateTime;
import java.util.List;

public final class ZipProjectExporter implements ProjectExporter {

    private static final LocalDateTime FIXED_ENTRY_TIME = LocalDateTime.of(1980, 1, 1, 0, 0, 0);
    private static final int EXECUTABLE_MODE = 0755;
    private static final int REGULAR_MODE = 0644;
    private static final String TEMPORARY_SUFFIX = ".zip.tmp";

    @FunctionalInterface
    interface ZipWriter {

        void write(GeneratedProject project, OutputStream output) throws IOException;
    }

    private final ZipWriter writer;

    public ZipProjectExporter() {
        this(ZipProjectExporter::writeZip);
    }

    ZipProjectExporter(ZipWriter writer) {
        this.writer = writer;
    }

    @Override
    public ExportFormat format() {
        return ExportFormat.ZIP;
    }

    @Override
    public List<Violation> check(ExportTarget target, String artifactId) {
        List<Violation> destination = ExportChecks.destination(target.destination());

        if (!destination.isEmpty()) {
            return destination;
        }

        Path zip = zipPath(target.destination(), artifactId);

        if (Files.exists(zip) && (!target.overwrite() || Files.isDirectory(zip))) {
            return List.of(ExportChecks.violation(ViolationCode.TARGET_EXISTS, zip));
        }

        return List.of();
    }

    @Override
    public ExportResult write(GeneratedProject project, ExportTarget target) throws IOException {
        Path zip = zipPath(target.destination(), project.artifactId());
        Path temporary = Files.createTempFile(target.destination(), "." + project.artifactId() + "-", TEMPORARY_SUFFIX);

        try {
            writeTo(temporary, project);
            makeReadable(temporary);
            publish(temporary, zip, target.overwrite());
        } catch (IOException | RuntimeException failure) {
            deleteQuietly(temporary);
            throw failure;
        }

        return new ExportResult(zip.toAbsolutePath().normalize(), project.fileCount(), project.totalBytes());
    }

    private void writeTo(Path temporary, GeneratedProject project) throws IOException {
        try (OutputStream output = new BufferedOutputStream(Files.newOutputStream(temporary))) {
            writer.write(project, output);
        }
    }

    private void publish(Path temporary, Path zip, boolean overwrite) throws IOException {
        if (!overwrite) {
            Files.move(temporary, zip);
            return;
        }

        try {
            Files.move(temporary, zip, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
        } catch (AtomicMoveNotSupportedException notAtomic) {
            Files.move(temporary, zip, StandardCopyOption.REPLACE_EXISTING);
        }
    }

    private void makeReadable(Path file) throws IOException {
        if (file.getFileSystem().supportedFileAttributeViews().contains("posix")) {
            Files.setPosixFilePermissions(file, PosixFilePermissions.fromString("rw-r--r--"));
        }
    }

    private void deleteQuietly(Path file) {
        try {
            Files.deleteIfExists(file);
        } catch (IOException ignored) {
            return;
        }
    }

    private Path zipPath(Path destination, String artifactId) {
        return destination.resolve(artifactId + ".zip");
    }

    private static void writeZip(GeneratedProject project, OutputStream output) throws IOException {
        try (ZipArchiveOutputStream zip = new ZipArchiveOutputStream(output)) {
            for (GeneratedFile file : project.files()) {
                addEntry(zip, project.artifactId(), file);
            }

            zip.finish();
        }
    }

    private static void addEntry(ZipArchiveOutputStream zip, String root, GeneratedFile file) throws IOException {
        SafePaths.requireRelative(file.path());

        ZipArchiveEntry entry = new ZipArchiveEntry(root + "/" + file.path());
        entry.setTimeLocal(FIXED_ENTRY_TIME);
        entry.setUnixMode(file.executable() ? EXECUTABLE_MODE : REGULAR_MODE);

        zip.putArchiveEntry(entry);
        zip.write(file.content());
        zip.closeArchiveEntry();
    }
}
