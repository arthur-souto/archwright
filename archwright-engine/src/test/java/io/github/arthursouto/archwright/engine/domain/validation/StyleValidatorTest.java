package io.github.arthursouto.archwright.engine.domain.validation;

import io.github.arthursouto.archwright.engine.domain.model.Architecture;
import io.github.arthursouto.archwright.engine.domain.model.ArchitectureStyle;
import io.github.arthursouto.archwright.engine.domain.model.ModuleName;
import io.github.arthursouto.archwright.engine.domain.style.ArchitectureStyles;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.stream.IntStream;

import static org.assertj.core.api.Assertions.assertThat;

class StyleValidatorTest {

    private static final String ARCHITECTURE_STYLE = "architectureStyle";
    private static final String MODULES = "modules";
    private static final String MODULAR = "HEXAGONAL_MODULAR";
    private static final String LAYERED = "HEXAGONAL_LAYERED";

    private final StyleValidator validator = new StyleValidator(ArchitectureStyles.standard());

    @Test
    @DisplayName("ENG-VAL-02 #1 normaliza os módulos preservando a ordem")
    void normalizesModulesKeepingOrder() {
        Checked<Architecture> result = validator.validate(draft(MODULAR, List.of("Pedidos", " pagamentos ")));

        assertThat(result.isValid()).isTrue();
        assertThat(result.value().style()).isEqualTo(ArchitectureStyle.HEXAGONAL_MODULAR);
        assertThat(result.value().modules()).extracting(ModuleName::value).containsExactly("pedidos", "pagamentos");
    }

    @Test
    @DisplayName("ENG-VAL-02 #2 estilo modular sem módulos exige ao menos um")
    void requiresModulesForModularStyle() {
        Checked<Architecture> result = validator.validate(draft(MODULAR, List.of()));

        assertThat(result.violations())
                .containsExactly(Violation.of(ViolationCode.MODULES_REQUIRED, MODULES));
    }

    @Test
    @DisplayName("ENG-VAL-02 #3 módulo com nome reservado é recusado")
    void rejectsReservedModuleName() {
        Checked<Architecture> result = validator.validate(draft(MODULAR, List.of("shared")));

        assertThat(result.violations())
                .containsExactly(Violation.of(ViolationCode.RESERVED_MODULE_NAME, MODULES, "name", "shared"));
    }

    @Test
    @DisplayName("ENG-VAL-02 #4 módulos repetidos após normalizar são recusados")
    void rejectsDuplicatedModulesAfterNormalization() {
        Checked<Architecture> result = validator.validate(draft(MODULAR, List.of("pedidos", "PEDIDOS")));

        assertThat(result.violations())
                .containsExactly(Violation.of(ViolationCode.DUPLICATED_MODULE_NAME, MODULES, "name", "pedidos"));
    }

    @Test
    @DisplayName("ENG-VAL-02 #5 mais de 20 módulos é recusado")
    void rejectsMoreThanTwentyModules() {
        List<String> modules = IntStream.rangeClosed(1, 21).mapToObj(index -> "modulo" + index).toList();

        Checked<Architecture> result = validator.validate(draft(MODULAR, modules));

        assertThat(result.violations())
                .containsExactly(Violation.of(ViolationCode.TOO_MANY_MODULES, MODULES, "max", "20"));
    }

    @Test
    @DisplayName("ENG-VAL-02 #6 estilo em camadas aceita zero agregados")
    void acceptsLayeredStyleWithoutAggregates() {
        Checked<Architecture> result = validator.validate(draft(LAYERED, List.of()));

        assertThat(result.isValid()).isTrue();
        assertThat(result.value().modules()).isEmpty();
    }

    @Test
    @DisplayName("ENG-VAL-02 #7 palavra reservada só vale para o estilo modular")
    void acceptsReservedNameInLayeredStyle() {
        Checked<Architecture> result = validator.validate(draft(LAYERED, List.of("shared")));

        assertThat(result.isValid()).isTrue();
        assertThat(result.value().modules()).extracting(ModuleName::value).containsExactly("shared");
    }

    @Test
    @DisplayName("ENG-VAL-02 #8 estilo desconhecido é recusado")
    void rejectsUnknownStyle() {
        Checked<Architecture> result = validator.validate(draft("ONION", List.of("pedidos")));

        assertThat(result.violations()).containsExactly(
                Violation.of(ViolationCode.UNKNOWN_ARCHITECTURE_STYLE, ARCHITECTURE_STYLE, "value", "ONION"));
    }

    @Test
    @DisplayName("BR-011 nome de módulo que não é pacote Java válido é recusado")
    void rejectsModuleNameThatIsNotAValidPackage() {
        Checked<Architecture> result = validator.validate(draft(MODULAR, List.of("class", "pedidos")));

        assertThat(result.violations())
                .containsExactly(Violation.of(ViolationCode.INVALID_MODULE_NAME, MODULES, "name", "class"));
    }

    @Test
    @DisplayName("BR-070 coleta todas as violações dos módulos de uma vez")
    void collectsAllModuleViolations() {
        Checked<Architecture> result = validator.validate(draft(MODULAR, List.of("shared", "2bad", "pedidos", "pedidos")));

        assertThat(result.violations()).containsExactly(
                Violation.of(ViolationCode.RESERVED_MODULE_NAME, MODULES, "name", "shared"),
                Violation.of(ViolationCode.INVALID_MODULE_NAME, MODULES, "name", "2bad"),
                Violation.of(ViolationCode.DUPLICATED_MODULE_NAME, MODULES, "name", "pedidos"));
    }

    private static ProjectDraft draft(String style, List<String> modules) {
        return new ProjectDraft(
                null, null, null, null,
                style, modules,
                null, null, null, null, null, true);
    }
}
