package io.github.arthursouto.archwright.desktop.wizard;

import io.github.arthursouto.archwright.engine.application.port.in.ExportFormat;
import io.github.arthursouto.archwright.engine.application.port.in.ExportTarget;
import io.github.arthursouto.archwright.engine.application.port.in.ProjectRequest;
import org.junit.jupiter.api.Test;

import java.nio.file.Path;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;

class ProjectFormTest {

    private final ProjectForm form = new ProjectForm();

    @Test
    void buildsTheRequestFromTheFields() {
        form.artifactId().set("  pedido-api ");
        form.groupId().set("com.acme");
        form.description().set("API de pedidos");
        form.architectureStyle().set("HEXAGONAL_MODULAR");
        form.modules().set(" pedidos, pagamentos ,, ");
        form.bootVersion().set("4.1.2");
        form.javaVersion().set("25");
        form.kit().set("BASIC");
        form.dependencies().addAll(List.of("web", "jpa"));
        form.extras().add("docker-compose");

        ProjectRequest request = form.toRequest();

        assertThat(request).isEqualTo(new ProjectRequest("pedido-api", "com.acme", "", "API de pedidos",
                "HEXAGONAL_MODULAR", List.of("pedidos", "pagamentos"), "4.1.2", "25", "BASIC",
                List.of("jpa", "web"), List.of("docker-compose"), true));
    }

    @Test
    void emptyModulesBecomeAnEmptyList() {
        assertThat(form.toRequest().modules()).isEmpty();
    }

    @Test
    void buildsTheTargetFromFormatAndDestination() {
        form.format().set(ExportFormat.FOLDER);
        form.destination().set(" /tmp/out ");

        assertThat(form.hasDestination()).isTrue();
        assertThat(form.toTarget(true)).isEqualTo(new ExportTarget(ExportFormat.FOLDER, Path.of("/tmp/out"), true));
    }

    @Test
    void blankDestinationIsNotADestination() {
        form.destination().set("   ");

        assertThat(form.hasDestination()).isFalse();
    }

    @Test
    void notifiesEveryKindOfChange() {
        AtomicInteger changes = new AtomicInteger();
        form.onChange(changes::incrementAndGet);

        form.artifactId().set("a");
        form.fitnessFunctions().set(false);
        form.format().set(ExportFormat.FOLDER);
        form.dependencies().add("web");
        form.extras().add("docker-compose");

        assertThat(changes.get()).isEqualTo(5);
    }

    @Test
    void loadingAHistoryEntryRestoresEverything() {
        var entry = new io.github.arthursouto.archwright.desktop.history.HistoryEntry(
                "1", java.time.Instant.EPOCH, "pedido-api", "com.acme", "com.acme.base", "descricao", "HEXAGONAL_MODULAR",
                java.util.List.of("pedidos", "pagamentos"), "4.1.2", "25", "BASIC", java.util.List.of("web", "jpa"),
                java.util.List.of("docker-compose"), false,
                io.github.arthursouto.archwright.engine.application.port.in.ExportFormat.FOLDER,
                "/tmp/out", "/tmp/out/pedido-api", 69);
        ProjectForm form = new ProjectForm();

        form.load(entry);

        assertThat(form.artifactId().get()).isEqualTo("pedido-api");
        assertThat(form.basePackage().get()).isEqualTo("com.acme.base");
        assertThat(form.modules().get()).isEqualTo("pedidos, pagamentos");
        assertThat(form.bootVersion().get()).isEqualTo("4.1.2");
        assertThat(form.kit().get()).isEqualTo("BASIC");
        assertThat(form.selectedDependencies()).containsExactly("jpa", "web");
        assertThat(form.extras()).containsExactly("docker-compose");
        assertThat(form.fitnessFunctions().get()).isFalse();
        assertThat(form.destination().get()).isEqualTo("/tmp/out");
        assertThat(form.dependenciesChosen()).isTrue();
    }

    @Test
    void aFreshFormHasNotChosenItsDependencies() {
        assertThat(new ProjectForm().dependenciesChosen()).isFalse();
    }
}
