package io.github.arthursouto.archwright.desktop.history;

import io.github.arthursouto.archwright.engine.application.port.in.ExportFormat;
import io.github.arthursouto.archwright.engine.application.port.in.ProjectRequest;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.UUID;
import java.util.prefs.BackingStoreException;
import java.util.prefs.Preferences;

import static org.assertj.core.api.Assertions.assertThat;

class ProjectHistoryTest {

    private static final Instant START = Instant.parse("2026-11-18T12:00:00Z");

    private Preferences node;
    private ProjectHistory history;

    @BeforeEach
    void createHistory() {
        node = Preferences.userRoot().node("archwright-test-history-" + UUID.randomUUID());
        history = new ProjectHistory(node, Clock.fixed(START, ZoneOffset.UTC));
    }

    @AfterEach
    void removeNode() throws BackingStoreException {
        node.removeNode();
    }

    private static ProjectRequest request(String artifact) {
        return new ProjectRequest(artifact, "com.acme", "com.acme.base", "Uma linha, com vírgula = e dois pontos: ok",
                "HEXAGONAL_MODULAR", List.of("pedidos", "pagamentos"), "4.1.2", "25", "BASIC",
                List.of("web", "jpa"), List.of("docker-compose"), true);
    }

    private HistoryEntry record(String artifact) {
        return history.record(request(artifact), ExportFormat.ZIP, "/tmp/out", "/tmp/out/" + artifact + ".zip", 69);
    }

    @Test
    void startsEmpty() {
        assertThat(history.entries()).isEmpty();
        assertThat(history.total()).isZero();
    }

    @Test
    void remembersEveryFieldOfTheRequestAndTheOutcome() {
        HistoryEntry saved = record("pedido-api");

        assertThat(history.entries()).containsExactly(saved);
        assertThat(saved.at()).isEqualTo(START);
        assertThat(saved.artifactId()).isEqualTo("pedido-api");
        assertThat(saved.description()).isEqualTo("Uma linha, com vírgula = e dois pontos: ok");
        assertThat(saved.modules()).containsExactly("pedidos", "pagamentos");
        assertThat(saved.dependencies()).containsExactly("web", "jpa");
        assertThat(saved.extras()).containsExactly("docker-compose");
        assertThat(saved.fitnessFunctions()).isTrue();
        assertThat(saved.format()).isEqualTo(ExportFormat.ZIP);
        assertThat(saved.output()).isEqualTo("/tmp/out/pedido-api.zip");
        assertThat(saved.fileCount()).isEqualTo(69);
    }

    @Test
    void keepsEmptyListsAndBlankFieldsEmpty() {
        ProjectRequest bare = new ProjectRequest("a-b", "com.acme", "", "", "HEXAGONAL_LAYERED", List.of(), "4.1.2", "25", "NONE",
                List.of(), List.of(), false);

        history.record(bare, ExportFormat.FOLDER, "/tmp", "/tmp/a-b", 10);
        HistoryEntry loaded = history.entries().getFirst();

        assertThat(loaded.modules()).isEmpty();
        assertThat(loaded.dependencies()).isEmpty();
        assertThat(loaded.extras()).isEmpty();
        assertThat(loaded.basePackage()).isEmpty();
        assertThat(loaded.fitnessFunctions()).isFalse();
        assertThat(loaded.format()).isEqualTo(ExportFormat.FOLDER);
    }

    @Test
    void listsTheNewestFirstEvenWhenTheClockDoesNotMove() {
        record("primeiro");
        record("segundo");
        record("terceiro");

        assertThat(history.entries()).extracting(HistoryEntry::artifactId).containsExactly("terceiro", "segundo", "primeiro");
        assertThat(history.entries()).extracting(HistoryEntry::id).doesNotHaveDuplicates();
        assertThat(history.recent(2)).extracting(HistoryEntry::artifactId).containsExactly("terceiro", "segundo");
    }

    @Test
    void countsEveryGeneratedProject() {
        record("a-um");
        record("a-dois");

        assertThat(history.total()).isEqualTo(2);
    }

    @Test
    void keepsOnlyTheMostRecentTwentyButKeepsCounting() {
        for (int number = 1; number <= 25; number++) {
            record("projeto-" + number);
        }

        assertThat(history.entries()).hasSize(20);
        assertThat(history.entries().getFirst().artifactId()).isEqualTo("projeto-25");
        assertThat(history.entries().getLast().artifactId()).isEqualTo("projeto-6");
        assertThat(history.total()).isEqualTo(25);
    }

    @Test
    void removesOneEntry() {
        record("fica");
        HistoryEntry gone = record("sai");

        history.remove(gone.id());

        assertThat(history.entries()).extracting(HistoryEntry::artifactId).containsExactly("fica");
        assertThat(history.total()).isEqualTo(2);
    }

    @Test
    void clearsTheListButNotTheTotal() {
        record("um");
        record("dois");

        history.clear();

        assertThat(history.entries()).isEmpty();
        assertThat(history.total()).isEqualTo(2);
    }

    @Test
    void ignoresAnEntryThatCannotBeRead() {
        record("bom");
        node.put("entry.0000000000000000001", "isso não é um registro");
        node.put("entry.0000000000000000002", "id=1\nat=data-invalida");

        assertThat(history.entries()).extracting(HistoryEntry::artifactId).containsExactly("bom");
    }

    @Test
    void survivesANewInstanceOverTheSameStore() {
        record("persistido");

        ProjectHistory another = new ProjectHistory(node, Clock.fixed(START, ZoneOffset.UTC));

        assertThat(another.entries()).extracting(HistoryEntry::artifactId).containsExactly("persistido");
        assertThat(another.total()).isEqualTo(1);
    }
}
