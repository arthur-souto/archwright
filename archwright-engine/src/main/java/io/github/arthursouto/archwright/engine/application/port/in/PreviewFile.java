package io.github.arthursouto.archwright.engine.application.port.in;

public record PreviewFile(String path, int lines, String description) {

    public PreviewFile {
        description = description == null ? "" : description;
    }
}
