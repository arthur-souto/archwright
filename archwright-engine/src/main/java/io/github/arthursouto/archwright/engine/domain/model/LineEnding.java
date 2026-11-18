package io.github.arthursouto.archwright.engine.domain.model;

import java.util.Arrays;
import java.util.Optional;

public enum LineEnding {
    LF,
    CRLF;

    public static Optional<LineEnding> parse(String raw) {
        String id = raw.trim();

        return Arrays.stream(values())
                .filter(lineEnding -> lineEnding.name().equals(id))
                .findFirst();
    }
}
