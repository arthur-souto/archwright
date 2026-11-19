package io.github.arthursouto.archwright.desktop.i18n;

import io.github.arthursouto.archwright.engine.application.port.in.Violation;
import io.github.arthursouto.archwright.engine.application.port.in.Warning;
import io.github.arthursouto.archwright.engine.config.ArchwrightEngine;
import io.github.arthursouto.archwright.engine.domain.validation.ViolationCode;
import io.github.arthursouto.archwright.engine.domain.validation.WarningCode;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;
import java.util.List;
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
    void describesEveryField() {
        for (String key : new String[] {
                "project.artifactId.help", "project.groupId.help", "project.basePackage.help", "project.description.help",
                "project.style.help", "project.modules.help", "versions.boot.help", "versions.java.help",
                "dependencies.kit.help", "dependencies.extras.help", "dependencies.fitness.help",
                "extra.docker-compose.help", "extra.github-actions.help", "export.format.help", "export.destination.help"}) {
            assertThat(messages.has(key)).as(key).isTrue();
            assertThat(messages.text(key)).as(key).isNotBlank().isNotEqualTo(key);
        }
    }

    @Test
    void explainsEveryRuleAndEveryExtraTheCatalogCanOffer(@TempDir Path workspace) {
        ArchwrightEngine engine = ArchwrightEngine.builder().cacheDirectory(workspace).offline(true).build();
        List<String> dependencies = engine.catalog().dependencies("4.1.2").stream().map(option -> option.id()).toList();

        for (var style : engine.catalog().architectureStyles()) {
            engine.catalog().fitnessRules(style.id(), dependencies, "4.1.2").forEach(rule ->
                    assertThat(messages.has("rule." + rule.id() + ".help")).as(rule.id()).isTrue());
        }

        engine.catalog().extras().forEach(extra -> {
            assertThat(messages.has("extra." + extra.id() + ".help")).as(extra.id()).isTrue();
            assertThat(messages.has("extra." + extra.id() + ".files")).as(extra.id()).isTrue();
        });
    }

    @Test
    void namesEveryWizardStep() {
        for (var step : io.github.arthursouto.archwright.desktop.wizard.WizardStep.values()) {
            assertThat(messages.text("step." + step.name())).isNotEqualTo("step." + step.name());
        }
    }
}
