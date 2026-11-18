package io.github.arthursouto.archwright.engine.adapter.out.export;

import io.github.arthursouto.archwright.engine.application.port.in.ExportFormat;
import io.github.arthursouto.archwright.engine.application.port.in.ExportTarget;
import io.github.arthursouto.archwright.engine.application.port.out.ExportResult;
import io.github.arthursouto.archwright.engine.application.port.out.ProjectExporter;
import io.github.arthursouto.archwright.engine.domain.model.GeneratedFile;
import io.github.arthursouto.archwright.engine.domain.model.GeneratedProject;
import io.github.arthursouto.archwright.engine.domain.validation.Violation;
import io.github.arthursouto.archwright.engine.domain.validation.ViolationCode;

import java.io.IOException;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.PosixFilePermissions;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Stream;

public final class FolderProjectExporter implements ProjectExporter {

    private static final String PUBLIC_EXECUTABLE_PERMISSIONS = "rwxr-xr-x";

    @Override
    public ExportFormat format() {
        return ExportFormat.FOLDER;
    }

    @Override
    public List<Violation> check(ExportTarget target, String artifactId) {
        List<Violation> destination = ExportChecks.destination(target.destination());

        if (!destination.isEmpty()) {
            return destination;
        }

        Path folder = target.destination().resolve(artifactId);

        if (Files.exists(folder) && !isEmptyDirectory(folder)) {
            return List.of(ExportChecks.violation(ViolationCode.TARGET_NOT_EMPTY, folder));
        }

        return List.of();
    }

    @Override
    public ExportResult write(GeneratedProject project, ExportTarget target) throws IOException {
        Path folder = target.destination().resolve(project.artifactId());
        Path temporary = Files.createTempDirectory(target.destination(), "." + project.artifactId() + "-");

        try {
            makeTraversable(temporary);
            writeFiles(project, temporary);
            publish(temporary, folder);
        } catch (IOException | RuntimeException failure) {
            deleteRecursively(temporary);
            throw failure;
        }

        return new ExportResult(folder.toAbsolutePath().normalize(), project.fileCount(), project.totalBytes());
    }

    private void writeFiles(GeneratedProject project, Path root) throws IOException {
        for (GeneratedFile file : project.files()) {
            Path path = SafePaths.resolveInside(root, file.path());

            Files.createDirectories(path.getParent());
            Files.write(path, file.content());

            if (file.executable()) {
                makeExecutable(path);
            }
        }
    }

    private void publish(Path temporary, Path folder) throws IOException {
        if (Files.isDirectory(folder) && isEmptyDirectory(folder)) {
            Files.delete(folder);
        }

        Files.move(temporary, folder);
    }

    private void makeExecutable(Path file) throws IOException {
        if (isPosix(file)) {
            Files.setPosixFilePermissions(file, PosixFilePermissions.fromString(PUBLIC_EXECUTABLE_PERMISSIONS));
            return;
        }

        file.toFile().setExecutable(true);
    }

    private void makeTraversable(Path directory) throws IOException {
        if (isPosix(directory)) {
            Files.setPosixFilePermissions(directory, PosixFilePermissions.fromString(PUBLIC_EXECUTABLE_PERMISSIONS));
        }
    }

    private boolean isPosix(Path path) {
        return path.getFileSystem().supportedFileAttributeViews().contains("posix");
    }

    private boolean isEmptyDirectory(Path path) {
        if (!Files.isDirectory(path)) {
            return false;
        }

        try (DirectoryStream<Path> entries = Files.newDirectoryStream(path)) {
            return !entries.iterator().hasNext();
        } catch (IOException unreadable) {
            return false;
        }
    }

    private void deleteRecursively(Path root) {
        try (Stream<Path> paths = Files.walk(root)) {
            paths.sorted(Comparator.reverseOrder()).forEach(this::deleteQuietly);
        } catch (IOException ignored) {
            return;
        }
    }

    private void deleteQuietly(Path path) {
        try {
            Files.deleteIfExists(path);
        } catch (IOException ignored) {
            return;
        }
    }
}
