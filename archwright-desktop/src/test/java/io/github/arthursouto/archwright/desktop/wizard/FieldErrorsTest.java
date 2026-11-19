package io.github.arthursouto.archwright.desktop.wizard;

import io.github.arthursouto.archwright.desktop.i18n.Messages;
import io.github.arthursouto.archwright.engine.application.port.in.Violation;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class FieldErrorsTest {

    private final Messages messages = Messages.portuguese();

    @Test
    void groupsMessagesByField() {
        FieldErrors errors = FieldErrors.from(List.of(
                new Violation("INVALID_MODULE_NAME", "modules", Map.of("name", "Bad")),
                new Violation("DUPLICATED_MODULE_NAME", "modules", Map.of("name", "pedidos")),
                new Violation("INVALID_GROUP_ID", "groupId", Map.of())), messages);

        assertThat(errors.forField("modules")).containsExactly(
                "Bad não é um nome de pacote válido.", "O módulo pedidos aparece mais de uma vez.");
        assertThat(errors.messageFor("groupId")).startsWith("Use segmentos em minúsculas");
        assertThat(errors.forField("artifactId")).isEmpty();
        assertThat(errors.messageFor("artifactId")).isEmpty();
    }

    @Test
    void tellsWhichStepHasErrors() {
        FieldErrors errors = FieldErrors.from(List.of(new Violation("UNSUPPORTED_BOOT_VERSION", "bootVersion", Map.of())), messages);

        assertThat(errors.hasErrorIn(WizardStep.VERSIONS)).isTrue();
        assertThat(errors.hasErrorIn(WizardStep.PROJECT)).isFalse();
        assertThat(errors.isEmpty()).isFalse();
    }

    @Test
    void noneHasNoErrors() {
        assertThat(FieldErrors.none().isEmpty()).isTrue();
        assertThat(FieldErrors.none().hasErrorIn(WizardStep.EXPORT)).isFalse();
    }
}
