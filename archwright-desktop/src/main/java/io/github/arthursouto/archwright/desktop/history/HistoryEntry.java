package io.github.arthursouto.archwright.desktop.history;

import io.github.arthursouto.archwright.engine.application.port.in.ExportFormat;
import io.github.arthursouto.archwright.engine.application.port.in.ProjectRequest;

import java.time.Instant;
import java.util.List;

public record HistoryEntry(
        String id,
        Instant at,
        String artifactId,
        String groupId,
        String basePackage,
        String description,
        String architectureStyle,
        List<String> modules,
        String bootVersion,
        String javaVersion,
        String kit,
        List<String> dependencies,
        List<String> extras,
        boolean fitnessFunctions,
        ExportFormat format,
        String destination,
        String output,
        int fileCount) {

    public HistoryEntry {
        modules = List.copyOf(modules);
        dependencies = List.copyOf(dependencies);
        extras = List.copyOf(extras);
    }

    public static HistoryEntry of(
            String id, Instant at, ProjectRequest request, ExportFormat format, String destination, String output, int fileCount) {
        return new HistoryEntry(
                id, at, request.artifactId(), request.groupId(), request.basePackage(), request.description(),
                request.architectureStyle(), request.modules(), request.bootVersion(), request.javaVersion(), request.kit(),
                request.dependencies(), request.extras(), request.fitnessFunctions(), format, destination, output, fileCount);
    }
}
