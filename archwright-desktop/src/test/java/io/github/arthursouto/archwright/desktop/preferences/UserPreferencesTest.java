package io.github.arthursouto.archwright.desktop.preferences;

import io.github.arthursouto.archwright.desktop.wizard.ProjectForm;
import io.github.arthursouto.archwright.engine.application.port.in.ExportFormat;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.UUID;
import java.util.prefs.BackingStoreException;
import java.util.prefs.Preferences;

import static org.assertj.core.api.Assertions.assertThat;

class UserPreferencesTest {

    private Preferences node;
    private UserPreferences preferences;

    @BeforeEach
    void createNode() {
        node = Preferences.userRoot().node("archwright-test-" + UUID.randomUUID());
        preferences = new UserPreferences(node);
    }

    @AfterEach
    void removeNode() throws BackingStoreException {
        node.removeNode();
    }

    @Test
    void remembersTheLastChoices() {
        ProjectForm saved = new ProjectForm();
        saved.groupId().set(" com.acme ");
        saved.architectureStyle().set("HEXAGONAL_LAYERED");
        saved.kit().set("NONE");
        saved.fitnessFunctions().set(false);
        saved.format().set(ExportFormat.FOLDER);
        saved.destination().set("/tmp/projects");
        preferences.remember(saved);

        ProjectForm restored = new ProjectForm();
        preferences.restoreInto(restored);

        assertThat(restored.groupId().get()).isEqualTo("com.acme");
        assertThat(restored.architectureStyle().get()).isEqualTo("HEXAGONAL_LAYERED");
        assertThat(restored.kit().get()).isEqualTo("NONE");
        assertThat(restored.fitnessFunctions().get()).isFalse();
        assertThat(restored.format().get()).isEqualTo(ExportFormat.FOLDER);
        assertThat(restored.destination().get()).isEqualTo("/tmp/projects");
    }

    @Test
    void startsWithDefaultsWhenNothingWasSaved() {
        ProjectForm restored = new ProjectForm();
        preferences.restoreInto(restored);

        assertThat(restored.groupId().get()).isEmpty();
        assertThat(restored.fitnessFunctions().get()).isTrue();
        assertThat(restored.format().get()).isEqualTo(ExportFormat.ZIP);
    }

    @Test
    void ignoresAnUnknownStoredFormat() {
        node.put("format", "TAR");
        ProjectForm restored = new ProjectForm();

        preferences.restoreInto(restored);

        assertThat(restored.format().get()).isEqualTo(ExportFormat.ZIP);
    }

    @Test
    void remembersTheTheme() {
        assertThat(preferences.darkTheme()).isFalse();

        preferences.darkTheme(true);

        assertThat(preferences.darkTheme()).isTrue();
    }
}
