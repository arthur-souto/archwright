package io.github.arthursouto.archwright.engine.application.usecase;

import io.github.arthursouto.archwright.engine.application.port.in.BootVersionOption;
import io.github.arthursouto.archwright.engine.application.port.in.BootVersions;
import io.github.arthursouto.archwright.engine.application.port.in.VersionOrigin;
import io.github.arthursouto.archwright.engine.application.port.out.BootVersionSource;
import io.github.arthursouto.archwright.engine.application.port.out.CachedVersions;
import io.github.arthursouto.archwright.engine.application.port.out.EmbeddedVersions;
import io.github.arthursouto.archwright.engine.application.port.out.VersionCache;
import io.github.arthursouto.archwright.engine.domain.model.BootProfile;
import io.github.arthursouto.archwright.engine.domain.model.BootVersion;
import io.github.arthursouto.archwright.engine.domain.model.Catalog;

import java.io.IOException;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;

public final class BootVersionLookup {

    private static final Duration FRESHNESS = Duration.ofHours(24);

    private final BootVersionSource remote;
    private final VersionCache cache;
    private final EmbeddedVersions embedded;
    private final Catalog catalog;
    private final SupportedVersionFilter filter;
    private final Clock clock;

    public BootVersionLookup(
            BootVersionSource remote, VersionCache cache, EmbeddedVersions embedded, Catalog catalog, Clock clock) {
        this.remote = remote;
        this.cache = cache;
        this.embedded = embedded;
        this.catalog = catalog;
        this.filter = new SupportedVersionFilter(catalog);
        this.clock = clock;
    }

    public BootVersions lookup() {
        Instant now = clock.instant();
        Optional<CachedVersions> cached = cache.read();

        return freshFromCache(cached, now)
                .or(() -> fromRemote(now))
                .or(() -> staleFromCache(cached))
                .orElseGet(this::fromEmbedded);
    }

    private Optional<BootVersions> freshFromCache(Optional<CachedVersions> cached, Instant now) {
        return cached
                .filter(entry -> isFresh(entry, now))
                .flatMap(entry -> assemble(entry.versions(), VersionOrigin.CACHE, entry.writtenAt()));
    }

    private Optional<BootVersions> fromRemote(Instant now) {
        List<String> fetched;

        try {
            fetched = remote.fetch();
        } catch (IOException unavailable) {
            return Optional.empty();
        }

        Optional<BootVersions> result = assemble(fetched, VersionOrigin.REMOTE, now);
        result.ifPresent(versions -> cache.write(fetched, now));

        return result;
    }

    private Optional<BootVersions> staleFromCache(Optional<CachedVersions> cached) {
        return cached.flatMap(entry -> assemble(entry.versions(), VersionOrigin.STALE_CACHE, entry.writtenAt()));
    }

    private BootVersions fromEmbedded() {
        return assemble(embedded.versions(), VersionOrigin.EMBEDDED, null)
                .orElseThrow(() -> new IllegalStateException("embedded boot versions match no catalog line"));
    }

    private boolean isFresh(CachedVersions entry, Instant now) {
        Duration age = Duration.between(entry.writtenAt(), now);

        return !age.isNegative() && age.compareTo(FRESHNESS) < 0;
    }

    private Optional<BootVersions> assemble(List<String> rawVersions, VersionOrigin origin, Instant fetchedAt) {
        List<BootVersion> selected = filter.select(rawVersions);

        if (selected.isEmpty()) {
            return Optional.empty();
        }

        return Optional.of(new BootVersions(options(selected), origin, fetchedAt));
    }

    private List<BootVersionOption> options(List<BootVersion> newestFirst) {
        Map<String, BootVersion> latestByLine = newestFirst.stream()
                .collect(Collectors.toMap(BootVersion::line, Function.identity(), (first, second) -> first));

        return newestFirst.stream()
                .map(version -> option(version, latestByLine.get(version.line()).equals(version)))
                .toList();
    }

    private BootVersionOption option(BootVersion version, boolean latestOfLine) {
        boolean legacy = catalog.profileFor(version).map(BootProfile::legacy).orElse(false);

        return new BootVersionOption(version.toString(), version.line(), legacy, latestOfLine);
    }
}
