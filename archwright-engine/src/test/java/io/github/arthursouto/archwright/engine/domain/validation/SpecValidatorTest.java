package io.github.arthursouto.archwright.engine.domain.validation;

import io.github.arthursouto.archwright.engine.domain.kit.Kits;
import io.github.arthursouto.archwright.engine.domain.model.BootVersion;
import io.github.arthursouto.archwright.engine.domain.model.DependencyId;
import io.github.arthursouto.archwright.engine.domain.model.ExtraId;
import io.github.arthursouto.archwright.engine.domain.model.JavaVersion;
import io.github.arthursouto.archwright.engine.domain.model.KitId;
import io.github.arthursouto.archwright.engine.domain.model.ProjectSpec;
import io.github.arthursouto.archwright.engine.domain.style.ArchitectureStyles;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class SpecValidatorTest {

    private final SpecValidator validator =
            new SpecValidator(TestCatalog.standard(), ArchitectureStyles.standard(), Kits.standard());

    @Test
    @DisplayName("ENG-VAL-01 #1 / ENG-VAL-03 #1 / ENG-VAL-04 #1 pedido completo vira ProjectSpec sem avisos")
    void buildsSpecFromCompleteValidDraft() {
        ValidationResult result = validator.validate(DraftBuilder.valid().extras("github-actions").build());

        ValidationResult.Valid valid = assertValid(result);
        ProjectSpec spec = valid.spec();

        assertThat(spec.identity().artifactId().value()).isEqualTo("pedido-api");
        assertThat(spec.identity().basePackage().value()).isEqualTo("com.acme.pedidoapi");
        assertThat(spec.architecture().modules()).hasSize(2);
        assertThat(spec.bootVersion()).isEqualTo(new BootVersion(3, 5, 3));
        assertThat(spec.javaVersion()).isEqualTo(new JavaVersion(21));
        assertThat(spec.kit()).isEqualTo(new KitId("BASIC"));
        assertThat(spec.dependencies()).hasSize(6);
        assertThat(spec.extras()).containsExactly(new ExtraId("github-actions"));
        assertThat(spec.fitnessFunctions()).isTrue();
        assertThat(valid.warnings()).isEmpty();
    }

    @Test
    @DisplayName("ENG-VAL-04 #2 kit NONE sem dependências é válido")
    void acceptsNoneKitWithoutDependencies() {
        ValidationResult result = validator.validate(DraftBuilder.valid().kit("NONE").dependencies().build());

        assertThat(assertValid(result).spec().dependencies()).isEmpty();
    }

    @Test
    @DisplayName("ENG-VAL-03 #3 Boot 2.7.18 com Java 8 é válido com aviso LEGACY_BOOT_LINE")
    void warnsAboutLegacyBootLine() {
        ValidationResult result = validator.validate(DraftBuilder.valid().boot("2.7.18").java("8").build());

        assertThat(assertValid(result).warnings())
                .containsExactly(new Warning(WarningCode.LEGACY_BOOT_LINE, Map.of("line", "2.7")));
    }

    @Test
    @DisplayName("BR-070 avisos saem na ordem Boot, Java, dependências")
    void ordersWarningsByBootJavaDependencies() {
        ValidationResult result = validator.validate(
                DraftBuilder.valid().boot("2.7.18").java("25").kit("NONE").dependencies("flyway").build());

        assertThat(assertValid(result).warnings()).extracting(Warning::code).containsExactly(
                WarningCode.LEGACY_BOOT_LINE, WarningCode.JAVA_ABOVE_TESTED, WarningCode.DEPENDENCY_ADDED);
    }

    @Test
    @DisplayName("ENG-VAL-04 #3 dependência implícita entra no ProjectSpec")
    void includesImpliedDependencyInSpec() {
        ValidationResult result = validator.validate(DraftBuilder.valid().kit("NONE").dependencies("flyway").build());

        assertThat(assertValid(result).spec().dependencies())
                .containsExactly(new DependencyId("flyway"), new DependencyId("jpa"));
    }

    @Test
    @DisplayName("ENG-VAL-04 #7 docker-compose é validado contra as dependências já resolvidas")
    void checksExtraRequirementsAgainstResolvedDependencies() {
        ValidationResult result = validator.validate(
                DraftBuilder.valid().kit("NONE").dependencies("flyway").extras("docker-compose").build());

        assertThat(assertValid(result).spec().extras()).containsExactly(new ExtraId("docker-compose"));
    }

    @Test
    @DisplayName("ENG-VAL-04 #9 kit desconhecido é recusado")
    void rejectsUnknownKit() {
        ValidationResult result = validator.validate(DraftBuilder.valid().kit("PREMIUM").build());

        assertThat(assertInvalid(result).violations()).containsExactly(
                Violation.of(ViolationCode.UNKNOWN_KIT, "kit", "value", "PREMIUM"));
    }

    @Test
    @DisplayName("BR-070 coleta violações de todos os assuntos na ordem dos campos")
    void collectsViolationsFromEverySubjectInFieldOrder() {
        ValidationResult result = validator.validate(DraftBuilder.valid()
                .artifactId("1app")
                .style("ONION")
                .boot("9.0.0")
                .java("10")
                .kit("PREMIUM")
                .dependencies("kafka")
                .extras("terraform")
                .build());

        assertThat(assertInvalid(result).violations()).extracting(Violation::code).containsExactly(
                ViolationCode.INVALID_ARTIFACT_ID,
                ViolationCode.UNKNOWN_ARCHITECTURE_STYLE,
                ViolationCode.UNSUPPORTED_BOOT_VERSION,
                ViolationCode.UNSUPPORTED_JAVA_VERSION,
                ViolationCode.UNKNOWN_KIT,
                ViolationCode.UNKNOWN_DEPENDENCY,
                ViolationCode.UNKNOWN_EXTRA);
    }

    @Test
    @DisplayName("BR-071 pedido todo nulo devolve violações, nunca exceção")
    void neverThrowsForEmptyDraft() {
        ProjectDraft empty = new ProjectDraft(null, null, null, null, null, null, null, null, null, null, null, false);

        ValidationResult result = validator.validate(empty);

        assertThat(assertInvalid(result).violations()).isNotEmpty();
    }

    private static ValidationResult.Valid assertValid(ValidationResult result) {
        assertThat(result).isInstanceOf(ValidationResult.Valid.class);

        return (ValidationResult.Valid) result;
    }

    private static ValidationResult.Invalid assertInvalid(ValidationResult result) {
        assertThat(result).isInstanceOf(ValidationResult.Invalid.class);

        return (ValidationResult.Invalid) result;
    }
}
