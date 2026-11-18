package io.github.arthursouto.archwright.engine.domain.model;

import java.util.Arrays;
import java.util.Objects;

public record GeneratedFile(String path, byte[] content, boolean executable) {

    public GeneratedFile {
        Objects.requireNonNull(path);
        content = content.clone();
    }

    @Override
    public byte[] content() {
        return content.clone();
    }

    public int size() {
        return content.length;
    }

    @Override
    public boolean equals(Object other) {
        return other instanceof GeneratedFile that
                && executable == that.executable
                && path.equals(that.path)
                && Arrays.equals(content, that.content);
    }

    @Override
    public int hashCode() {
        return 31 * Objects.hash(path, executable) + Arrays.hashCode(content);
    }

    @Override
    public String toString() {
        return "GeneratedFile[path=" + path + ", size=" + content.length + ", executable=" + executable + "]";
    }
}
