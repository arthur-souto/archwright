package io.github.arthursouto.archwright.desktop.i18n;

import io.github.arthursouto.archwright.engine.application.port.in.Violation;
import io.github.arthursouto.archwright.engine.application.port.in.Warning;
import io.github.arthursouto.archwright.engine.domain.validation.ViolationCode;
import io.github.arthursouto.archwright.engine.domain.validation.WarningCode;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class MessagesTest {

    private final Messages messages = Messages.portuguese();

    @Test
    void fillsViolationParameters() {
        Violation violation = new Violation("INCOMPATIBLE_JAVA_VERSION", "javaVersion", Map.of("min", "17", "boot", "3.5.0"));

        assertThat(messages.violation(violation)).isEqualTo("O Spring Boot 3.5.0 exige Java 17 ou superior.");
    }

    @Test
    void fillsWarningParameters() {
        Warning warning = new Warning("DEPENDENCY_ADDED", Map.of("added", "jpa", "by", "flyway"));

        assertThat(messages.warning(warning)).isEqualTo("jpa foi incluída porque flyway precisa dela.");
    }

    @Test
    void fallsBackToTheCodeWhenThereIsNoMessage() {
        assertThat(messages.violation(new Violation("SOMETHING_NEW", "x", Map.of()))).isEqualTo("SOMETHING_NEW");
        assertThat(messages.warning(new Warning("SOMETHING_NEW", Map.of()))).isEqualTo("SOMETHING_NEW");
    }

    @Test
    void returnsTheKeyForUnknownTexts() {
        assertThat(messages.text("does.not.exist")).isEqualTo("does.not.exist");
    }

    @Test
    void translatesEveryViolationCodeOfTheEngine() {
        for (ViolationCode code : ViolationCode.values()) {
            Violation violation = new Violation(code.name(), "field", Map.of());

            assertThat(messages.violation(violation)).as(code.name()).isNotEqualTo(code.name());
        }
    }

    @Test
    void translatesEveryWarningCodeOfTheEngine() {
        for (WarningCode code : WarningCode.values()) {
            Warning warning = new Warning(code.name(), Map.of());

            assertThat(messages.warning(warning)).as(code.name()).isNotEqualTo(code.name());
        }
    }

    @Test
    void namesEveryWizardStep() {
        for (var step : io.github.arthursouto.archwright.desktop.wizard.WizardStep.values()) {
            assertThat(messages.text("step." + step.name())).isNotEqualTo("step." + step.name());
        }
    }
}
