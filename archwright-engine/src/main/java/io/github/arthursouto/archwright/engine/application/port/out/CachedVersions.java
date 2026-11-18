package io.github.arthursouto.archwright.engine.application.port.out;

import java.time.Instant;
import java.util.List;
import java.util.Objects;

public record CachedVersions(List<String> versions, Instant writtenAt) {

    public CachedVersions {
        versions = List.copyOf(versions);
        Objects.requireNonNull(writtenAt);
    }
}
