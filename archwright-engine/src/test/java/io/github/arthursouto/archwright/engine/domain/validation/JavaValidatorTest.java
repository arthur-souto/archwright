package io.github.arthursouto.archwright.engine.domain.validation;

import io.github.arthursouto.archwright.engine.domain.model.JavaVersion;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class JavaValidatorTest {

    private static final String JAVA_VERSION = "javaVersion";

    private final BootValidator bootValidator = new BootValidator(TestCatalog.standard());
    private final JavaValidator validator = new JavaValidator();

    @Test
    @DisplayName("ENG-VAL-03 #1 Java dentro da faixa testada é válido e sem avisos")
    void acceptsJavaWithinTestedRange() {
        Checked<JavaVersion> result = check("3.5.3", "21");

        assertThat(result.isValid()).isTrue();
        assertThat(result.warnings()).isEmpty();
    }

    @Test
    @DisplayName("ENG-VAL-03 #2 Java abaixo do mínimo do perfil é incompatível")
    void rejectsJavaBelowProfileMinimum() {
        Checked<JavaVersion> result = check("3.2.0", "11");

        assertThat(result.violations()).containsExactly(new Violation(
                ViolationCode.INCOMPATIBLE_JAVA_VERSION, JAVA_VERSION, Map.of("min", "17", "boot", "3.2.0")));
    }

    @Test
    @DisplayName("ENG-VAL-03 #3 Java 8 é aceito na linha 2.7")
    void acceptsJavaEightOnLegacyLine() {
        Checked<JavaVersion> result = check("2.7.18", "8");

        assertThat(result.isValid()).isTrue();
    }

    @Test
    @DisplayName("ENG-VAL-03 #4 Java acima do testado é válido com aviso")
    void warnsWhenJavaIsAboveTested() {
        Checked<JavaVersion> result = check("4.1.2", "26");

        assertThat(result.isValid()).isTrue();
        assertThat(result.warnings()).containsExactly(
                new Warning(WarningCode.JAVA_ABOVE_TESTED, Map.of("java", "26", "maxTested", "25")));
    }

    @Test
    @DisplayName("ENG-VAL-03 #8 Java desconhecido é recusado")
    void rejectsUnknownJavaVersion() {
        Checked<JavaVersion> result = check("3.5.3", "10");

        assertThat(result.violations()).containsExactly(
                Violation.of(ViolationCode.UNSUPPORTED_JAVA_VERSION, JAVA_VERSION, "value", "10"));
    }

    @Test
    @DisplayName("BR-021 Java não numérico é recusado")
    void rejectsNonNumericJavaVersion() {
        Checked<JavaVersion> result = check("3.5.3", "latest");

        assertThat(result.violations()).extracting(Violation::code)
                .containsExactly(ViolationCode.UNSUPPORTED_JAVA_VERSION);
    }

    @Test
    @DisplayName("BR-070 Boot inválido não gera violação extra de compatibilidade")
    void skipsCompatibilityWhenBootIsInvalid() {
        Checked<JavaVersion> result = check("9.0.0", "8");

        assertThat(result.isValid()).isTrue();
        assertThat(result.violations()).isEmpty();
    }

    private Checked<JavaVersion> check(String boot, String java) {
        ProjectDraft draft = DraftBuilder.valid().boot(boot).java(java).build();

        return validator.validate(draft, bootValidator.validate(draft));
    }
}
