package io.github.arthursouto.archwright.engine.adapter.out.export;

import java.io.IOException;
import java.nio.file.Path;
import java.util.Arrays;

final class SafePaths {

    private SafePaths() {
    }

    static void requireRelative(String relative) throws IOException {
        boolean unsafe = relative.isBlank()
                || relative.startsWith("/")
                || relative.contains("\\")
                || relative.contains(":")
                || Arrays.stream(relative.split("/", -1)).anyMatch(SafePaths::isForbiddenSegment);

        if (unsafe) {
            throw new IOException("unsafe path in the generated project: " + relative);
        }
    }

    static Path resolveInside(Path root, String relative) throws IOException {
        requireRelative(relative);

        Path normalizedRoot = root.toAbsolutePath().normalize();
        Path resolved = normalizedRoot.resolve(relative).normalize();

        if (!resolved.startsWith(normalizedRoot) || resolved.equals(normalizedRoot)) {
            throw new IOException("path escapes the project folder: " + relative);
        }

        return resolved;
    }

    private static boolean isForbiddenSegment(String segment) {
        return segment.isEmpty() || segment.equals(".") || segment.equals("..");
    }
}
