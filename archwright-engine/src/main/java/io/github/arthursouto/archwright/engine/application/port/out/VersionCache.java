package io.github.arthursouto.archwright.engine.application.port.out;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

public interface VersionCache {

    Optional<CachedVersions> read();

    void write(List<String> versions, Instant at);
}
