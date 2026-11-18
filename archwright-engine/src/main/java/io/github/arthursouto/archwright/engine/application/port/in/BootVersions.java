package io.github.arthursouto.archwright.engine.application.port.in;

import java.time.Instant;
import java.util.List;

public record BootVersions(List<BootVersionOption> versions, VersionOrigin origin, Instant fetchedAt) {

    public BootVersions {
        versions = List.copyOf(versions);
    }
}
