package io.github.arthursouto.archwright.engine.domain.validation;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class BootValidatorTest {

    private static final String BOOT_VERSION = "bootVersion";

    private final BootValidator validator = new BootValidator(TestCatalog.standard());

    @Test
    @DisplayName("ENG-VAL-03 #1 versão estável de linha conhecida é válida e sem avisos")
    void acceptsStableVersionOfKnownLine() {
        Checked<ResolvedBoot> result = validator.validate(DraftBuilder.valid().boot("3.5.3").build());

        assertThat(result.isValid()).isTrue();
        assertThat(result.value().profile().line()).isEqualTo("3.5");
        assertThat(result.warnings()).isEmpty();
    }

    @Test
    @DisplayName("ENG-VAL-03 #3 linha 2.7 é válida com aviso LEGACY_BOOT_LINE")
    void warnsAboutLegacyLine() {
        Checked<ResolvedBoot> result = validator.validate(DraftBuilder.valid().boot("2.7.18").build());

        assertThat(result.isValid()).isTrue();
        assertThat(result.warnings())
                .containsExactly(new Warning(WarningCode.LEGACY_BOOT_LINE, Map.of("line", "2.7")));
    }

    @Test
    @DisplayName("ENG-VAL-03 #5 versão candidata (RC) é recusada")
    void rejectsPreReleaseVersion() {
        Checked<ResolvedBoot> result = validator.validate(DraftBuilder.valid().boot("3.5.0-RC1").build());

        assertThat(result.violations()).containsExactly(
                Violation.of(ViolationCode.UNSUPPORTED_BOOT_VERSION, BOOT_VERSION, "value", "3.5.0-RC1"));
    }

    @Test
    @DisplayName("ENG-VAL-03 #6 versão abaixo de 2.7.0 é recusada")
    void rejectsVersionBelowMinimum() {
        Checked<ResolvedBoot> result = validator.validate(DraftBuilder.valid().boot("2.6.15").build());

        assertThat(result.violations()).containsExactly(
                Violation.of(ViolationCode.UNSUPPORTED_BOOT_VERSION, BOOT_VERSION, "value", "2.6.15"));
    }

    @Test
    @DisplayName("ENG-VAL-03 #7 linha fora do catálogo é recusada")
    void rejectsLineMissingFromCatalog() {
        Checked<ResolvedBoot> result = validator.validate(DraftBuilder.valid().boot("9.0.0").build());

        assertThat(result.violations()).containsExactly(
                Violation.of(ViolationCode.UNSUPPORTED_BOOT_VERSION, BOOT_VERSION, "value", "9.0.0"));
    }

    @Test
    @DisplayName("BR-020 versão vazia é recusada")
    void rejectsEmptyVersion() {
        Checked<ResolvedBoot> result = validator.validate(DraftBuilder.valid().boot("").build());

        assertThat(result.violations()).extracting(Violation::code)
                .containsExactly(ViolationCode.UNSUPPORTED_BOOT_VERSION);
    }
}
