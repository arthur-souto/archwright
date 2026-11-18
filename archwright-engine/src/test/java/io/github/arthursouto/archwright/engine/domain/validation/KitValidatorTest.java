package io.github.arthursouto.archwright.engine.domain.validation;

import io.github.arthursouto.archwright.engine.domain.kit.Kits;
import io.github.arthursouto.archwright.engine.domain.model.KitId;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class KitValidatorTest {

    private static final String KIT = "kit";

    private final KitValidator validator = new KitValidator(Kits.standard());

    @Test
    @DisplayName("BR-031 kit BASIC é reconhecido")
    void acceptsBasicKit() {
        Checked<KitId> result = validator.validate(draft("BASIC"));

        assertThat(result.isValid()).isTrue();
        assertThat(result.value()).isEqualTo(new KitId("BASIC"));
    }

    @Test
    @DisplayName("BR-031 kit NONE é reconhecido")
    void acceptsNoneKit() {
        Checked<KitId> result = validator.validate(draft("NONE"));

        assertThat(result.value()).isEqualTo(new KitId("NONE"));
    }

    @Test
    @DisplayName("kit desconhecido devolve UNKNOWN_KIT com o valor informado")
    void rejectsUnknownKit() {
        Checked<KitId> result = validator.validate(draft("PREMIUM"));

        assertThat(result.violations())
                .containsExactly(Violation.of(ViolationCode.UNKNOWN_KIT, KIT, "value", "PREMIUM"));
    }

    @Test
    @DisplayName("kit ausente é tratado como desconhecido")
    void rejectsMissingKit() {
        Checked<KitId> result = validator.validate(draft(null));

        assertThat(result.violations())
                .containsExactly(Violation.of(ViolationCode.UNKNOWN_KIT, KIT, "value", ""));
    }

    private static ProjectDraft draft(String kit) {
        return new ProjectDraft(
                null, null, null, null,
                null, null,
                null, null,
                kit, null, null, true);
    }
}
