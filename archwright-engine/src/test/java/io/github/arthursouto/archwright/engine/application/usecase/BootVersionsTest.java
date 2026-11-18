package io.github.arthursouto.archwright.engine.application.usecase;

import io.github.arthursouto.archwright.engine.adapter.out.version.FileVersionCache;
import io.github.arthursouto.archwright.engine.application.port.in.BootVersionOption;
import io.github.arthursouto.archwright.engine.application.port.in.BootVersions;
import io.github.arthursouto.archwright.engine.application.port.in.VersionOrigin;
import io.github.arthursouto.archwright.engine.application.port.out.BootVersionSource;
import io.github.arthursouto.archwright.engine.application.port.out.CachedVersions;
import io.github.arthursouto.archwright.engine.application.port.out.VersionCache;
import io.github.arthursouto.archwright.engine.domain.model.Catalog;
import io.github.arthursouto.archwright.engine.domain.model.TestProfiles;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class BootVersionsTest {

    private static final Instant NOW = Instant.parse("2026-11-18T12:00:00Z");
    private static final List<String> EMBEDDED = List.of("4.1.0", "3.5.0", "2.7.18");

    private final FakeSource source = new FakeSource();
    private final FakeCache cache = new FakeCache();

    @Test
    @DisplayName("ENG-VER-01 #1 Maven Central responde: filtra, ordena e grava o cache")
    void usesRemoteVersionsAndWritesCache() {
        source.respondWith("4.1.2", "4.1.1", "4.2.0-M1", "2.6.9");

        BootVersions result = lookup();

        assertThat(versionsOf(result)).containsExactly("4.1.2", "4.1.1");
        assertThat(result.origin()).isEqualTo(VersionOrigin.REMOTE);
        assertThat(result.fetchedAt()).isEqualTo(NOW);
        assertThat(cache.stored).isEqualTo(new CachedVersions(List.of("4.1.2", "4.1.1", "4.2.0-M1", "2.6.9"), NOW));
    }

    @Test
    @DisplayName("ENG-VER-01 #2 Maven Central fora e cache de 3 horas: usa o cache")
    void usesFreshCacheWhenRemoteIsDown() {
        source.failWith("offline");
        cache.stored = new CachedVersions(List.of("3.5.3", "3.5.2"), NOW.minus(Duration.ofHours(3)));

        BootVersions result = lookup();

        assertThat(versionsOf(result)).containsExactly("3.5.3", "3.5.2");
        assertThat(result.origin()).isEqualTo(VersionOrigin.CACHE);
        assertThat(result.fetchedAt()).isEqualTo(NOW.minus(Duration.ofHours(3)));
    }

    @Test
    @DisplayName("ENG-VER-01 #3 Maven Central fora e cache de 3 dias: usa o cache velho")
    void usesStaleCacheWhenRemoteIsDown() {
        source.failWith("offline");
        cache.stored = new CachedVersions(List.of("3.5.3"), NOW.minus(Duration.ofDays(3)));

        BootVersions result = lookup();

        assertThat(versionsOf(result)).containsExactly("3.5.3");
        assertThat(result.origin()).isEqualTo(VersionOrigin.STALE_CACHE);
    }

    @Test
    @DisplayName("ENG-VER-01 #4 Maven Central fora e sem cache: usa a lista embutida")
    void usesEmbeddedListWhenNothingElseIsAvailable() {
        source.failWith("offline");

        BootVersions result = lookup();

        assertThat(versionsOf(result)).containsExactly("4.1.0", "3.5.0", "2.7.18");
        assertThat(result.origin()).isEqualTo(VersionOrigin.EMBEDDED);
        assertThat(result.fetchedAt()).isNull();
    }

    @Test
    @DisplayName("ENG-VER-01 #5 Maven Central que estoura o tempo cai no cache velho ou na lista embutida")
    void fallsBackWhenRemoteTimesOut() {
        source.failWith("maven central did not answer within PT8S");
        cache.stored = new CachedVersions(List.of("3.5.3"), NOW.minus(Duration.ofDays(2)));

        assertThat(lookup().origin()).isEqualTo(VersionOrigin.STALE_CACHE);

        cache.stored = null;

        assertThat(lookup().origin()).isEqualTo(VersionOrigin.EMBEDDED);
    }

    @Test
    @DisplayName("ENG-VER-01 #6 linha fora do catálogo não aparece")
    void hidesVersionsOfLinesMissingFromCatalog() {
        source.respondWith("5.0.0", "4.1.2");

        assertThat(versionsOf(lookup())).containsExactly("4.1.2");
    }

    @Test
    @DisplayName("ENG-VER-01 #7 cache corrompido é ignorado e regravado quando o remoto responde")
    void ignoresCorruptCacheAndRewritesIt(@TempDir Path directory) throws IOException {
        Path file = directory.resolve("boot-versions.json");
        Files.writeString(file, "{not valid json");
        source.respondWith("4.1.2", "3.5.3");
        FileVersionCache realCache = new FileVersionCache(file);

        BootVersions result = lookupWith(realCache);

        assertThat(result.origin()).isEqualTo(VersionOrigin.REMOTE);
        assertThat(realCache.read()).contains(new CachedVersions(List.of("4.1.2", "3.5.3"), NOW));
    }

    @Test
    @DisplayName("ENG-VER-01 #8 cache fresco não chama o Maven Central")
    void doesNotCallRemoteWhenCacheIsFresh() {
        source.respondWith("4.1.2");
        cache.stored = new CachedVersions(List.of("3.5.3"), NOW.minus(Duration.ofHours(2)));

        lookup();

        assertThat(source.calls).isZero();
    }

    @Test
    @DisplayName("BR-023 cache com menos de 24 horas é fresco, com exatamente 24 horas já não é")
    void treatsCacheOlderThanTwentyFourHoursAsNotFresh() {
        source.respondWith("4.1.2");
        cache.stored = new CachedVersions(List.of("3.5.3"), NOW.minus(Duration.ofHours(24)).plusSeconds(1));

        assertThat(lookup().origin()).isEqualTo(VersionOrigin.CACHE);
        assertThat(source.calls).isZero();

        cache.stored = new CachedVersions(List.of("3.5.3"), NOW.minus(Duration.ofHours(24)));

        assertThat(lookup().origin()).isEqualTo(VersionOrigin.REMOTE);
        assertThat(source.calls).isEqualTo(1);
    }

    @Test
    void doesNotTrustACacheWrittenInTheFuture() {
        source.respondWith("4.1.2");
        cache.stored = new CachedVersions(List.of("3.5.3"), NOW.plus(Duration.ofDays(30)));

        assertThat(lookup().origin()).isEqualTo(VersionOrigin.REMOTE);
    }

    @Test
    void sortsNewestFirstAndRemovesDuplicates() {
        source.respondWith("3.5.1", "4.1.0", "3.5.3", "4.1.0", "2.7.18", "3.5.2");

        assertThat(versionsOf(lookup())).containsExactly("4.1.0", "3.5.3", "3.5.2", "3.5.1", "2.7.18");
    }

    @Test
    void marksLegacyLineAndLatestOfEachLine() {
        source.respondWith("4.1.2", "4.1.1", "3.5.3", "2.7.18", "2.7.17");

        List<BootVersionOption> options = lookup().versions();

        assertThat(options).containsExactly(
                new BootVersionOption("4.1.2", "4.1", false, true),
                new BootVersionOption("4.1.1", "4.1", false, false),
                new BootVersionOption("3.5.3", "3.5", false, true),
                new BootVersionOption("2.7.18", "2.7", true, true),
                new BootVersionOption("2.7.17", "2.7", true, false));
    }

    @Test
    void ignoresRemoteAnswerWithNothingUsableAndKeepsTheCache() {
        source.respondWith("5.0.0", "4.2.0-RC1");
        CachedVersions stale = new CachedVersions(List.of("3.5.3"), NOW.minus(Duration.ofDays(3)));
        cache.stored = stale;

        BootVersions result = lookup();

        assertThat(result.origin()).isEqualTo(VersionOrigin.STALE_CACHE);
        assertThat(cache.stored).isSameAs(stale);
        assertThat(cache.writes).isZero();
    }

    @Test
    void ignoresEmptyRemoteAnswer() {
        source.respondWith();

        assertThat(lookup().origin()).isEqualTo(VersionOrigin.EMBEDDED);
    }

    @Test
    void skipsCacheWhoseVersionsAreAllFilteredOut() {
        source.failWith("offline");
        cache.stored = new CachedVersions(List.of("5.0.0", "4.2.0-M1"), NOW.minus(Duration.ofHours(1)));

        assertThat(lookup().origin()).isEqualTo(VersionOrigin.EMBEDDED);
    }

    @Test
    void failsLoudlyWhenEmbeddedListMatchesNoCatalogLine() {
        source.failWith("offline");
        BootVersionLookup lookup = new BootVersionLookup(source, cache, () -> List.of("9.9.9"), catalog(), clock());

        assertThatThrownBy(lookup::lookup)
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("embedded");
    }

    private BootVersions lookup() {
        return lookupWith(cache);
    }

    private BootVersions lookupWith(VersionCache versionCache) {
        return new BootVersionLookup(source, versionCache, () -> EMBEDDED, catalog(), clock()).lookup();
    }

    private static Clock clock() {
        return Clock.fixed(NOW, ZoneOffset.UTC);
    }

    private static Catalog catalog() {
        return new Catalog(
                List.of(
                        TestProfiles.simple("4.1", 17, 25, false),
                        TestProfiles.simple("3.5", 17, 24, false),
                        TestProfiles.simple("2.7", 8, 21, true)),
                List.of(),
                List.of());
    }

    private static List<String> versionsOf(BootVersions result) {
        return result.versions().stream().map(BootVersionOption::version).toList();
    }

    private static final class FakeSource implements BootVersionSource {

        private List<String> versions = List.of();
        private IOException failure;
        private int calls;

        void respondWith(String... answer) {
            this.versions = List.of(answer);
            this.failure = null;
        }

        void failWith(String message) {
            this.failure = new IOException(message);
        }

        @Override
        public List<String> fetch() throws IOException {
            calls++;

            if (failure != null) {
                throw failure;
            }

            return versions;
        }
    }

    private static final class FakeCache implements VersionCache {

        private CachedVersions stored;
        private int writes;

        @Override
        public Optional<CachedVersions> read() {
            return Optional.ofNullable(stored);
        }

        @Override
        public void write(List<String> versions, Instant at) {
            this.stored = new CachedVersions(versions, at);
            this.writes++;
        }
    }
}
