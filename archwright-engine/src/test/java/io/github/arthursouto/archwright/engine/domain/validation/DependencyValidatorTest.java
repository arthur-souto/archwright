package io.github.arthursouto.archwright.engine.domain.validation;

import io.github.arthursouto.archwright.engine.domain.model.DependencyId;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class DependencyValidatorTest {

    private static final String DEPENDENCIES = "dependencies";

    private final BootValidator bootValidator = new BootValidator(TestCatalog.standard());
    private final DependencyValidator validator = new DependencyValidator(TestCatalog.standard());

    @Test
    @DisplayName("ENG-VAL-04 #1 dependências do kit básico são aceitas, todas as seis")
    void acceptsBasicKitDependencies() {
        Checked<List<DependencyId>> result = check("3.5.3", "web", "validation", "actuator", "jpa", "flyway", "lombok");

        assertThat(result.isValid()).isTrue();
        assertThat(result.value()).hasSize(6);
        assertThat(result.warnings()).isEmpty();
    }

    @Test
    @DisplayName("ENG-VAL-04 #2 nenhuma dependência é válido")
    void acceptsNoDependencies() {
        Checked<List<DependencyId>> result = check("3.5.3");

        assertThat(result.isValid()).isTrue();
        assertThat(result.value()).isEmpty();
    }

    @Test
    @DisplayName("ENG-VAL-04 #3 flyway inclui jpa com aviso DEPENDENCY_ADDED")
    void addsJpaWhenFlywayIsRequested() {
        Checked<List<DependencyId>> result = check("3.5.3", "flyway");

        assertThat(result.value()).containsExactly(new DependencyId("flyway"), new DependencyId("jpa"));
        assertThat(result.warnings()).containsExactly(
                new Warning(WarningCode.DEPENDENCY_ADDED, Map.of("added", "jpa", "by", "flyway")));
    }

    @Test
    @DisplayName("ENG-VAL-04 #4 springdoc inclui web com aviso DEPENDENCY_ADDED")
    void addsWebWhenSpringdocIsRequested() {
        Checked<List<DependencyId>> result = check("3.5.3", "springdoc");

        assertThat(result.value()).containsExactly(new DependencyId("springdoc"), new DependencyId("web"));
        assertThat(result.warnings()).containsExactly(
                new Warning(WarningCode.DEPENDENCY_ADDED, Map.of("added", "web", "by", "springdoc")));
    }

    @Test
    @DisplayName("ENG-VAL-04 #5 dependência inexistente é recusada")
    void rejectsUnknownDependency() {
        Checked<List<DependencyId>> result = check("3.5.3", "kafka");

        assertThat(result.violations()).containsExactly(
                Violation.of(ViolationCode.UNKNOWN_DEPENDENCY, DEPENDENCIES, "id", "kafka"));
    }

    @Test
    @DisplayName("ENG-VAL-04 #6 dependência fora da versão escolhida é indisponível")
    void rejectsDependencyNotAvailableOnBootLine() {
        Checked<List<DependencyId>> result = check("2.7.18", "springdoc");

        assertThat(result.violations()).containsExactly(new Violation(
                ViolationCode.DEPENDENCY_NOT_AVAILABLE, DEPENDENCIES, Map.of("id", "springdoc", "boot", "2.7.18")));
    }

    @Test
    @DisplayName("ENG-VAL-04 #8 dependência repetida entra uma vez só")
    void keepsDuplicatedDependencyOnce() {
        Checked<List<DependencyId>> result = check("3.5.3", "web", "web");

        assertThat(result.value()).containsExactly(new DependencyId("web"));
    }

    @Test
    @DisplayName("BR-033 dependência já presente não gera aviso de inclusão")
    void doesNotWarnWhenImpliedDependencyIsAlreadyPresent() {
        Checked<List<DependencyId>> result = check("3.5.3", "jpa", "flyway");

        assertThat(result.value()).containsExactly(new DependencyId("jpa"), new DependencyId("flyway"));
        assertThat(result.warnings()).isEmpty();
    }

    @Test
    @DisplayName("BR-070 coleta desconhecidas e indisponíveis de uma vez")
    void collectsUnknownAndUnavailableTogether() {
        Checked<List<DependencyId>> result = check("2.7.18", "kafka", "springdoc");

        assertThat(result.violations()).extracting(Violation::code).containsExactly(
                ViolationCode.UNKNOWN_DEPENDENCY, ViolationCode.DEPENDENCY_NOT_AVAILABLE);
    }

    @Test
    @DisplayName("BR-070 Boot inválido não gera violação de disponibilidade")
    void skipsAvailabilityWhenBootIsInvalid() {
        Checked<List<DependencyId>> result = check("9.0.0", "springdoc");

        assertThat(result.isValid()).isTrue();
    }

    private Checked<List<DependencyId>> check(String boot, String... dependencies) {
        ProjectDraft draft = DraftBuilder.valid().boot(boot).dependencies(dependencies).build();

        return validator.validate(draft, bootValidator.validate(draft));
    }
}
