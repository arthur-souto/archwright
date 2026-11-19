package io.github.arthursouto.archwright.engine.domain.validation;

import io.github.arthursouto.archwright.engine.domain.model.ProjectIdentity;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class IdentityValidatorTest {

    private static final String ARTIFACT_ID = "artifactId";
    private static final String GROUP_ID = "groupId";
    private static final String BASE_PACKAGE = "basePackage";
    private static final String DESCRIPTION = "description";

    private final IdentityValidator validator = new IdentityValidator();

    @Test
    @DisplayName("ENG-VAL-01 #1 normaliza o artifactId e deriva pacote e classe principal")
    void normalizesArtifactIdAndDerivesPackageAndMainClass() {
        Checked<ProjectIdentity> result = validator.validate(draft(" Pedido-API ", "com.acme", "", ""));

        assertThat(result.isValid()).isTrue();
        assertThat(result.value().artifactId().value()).isEqualTo("pedido-api");
        assertThat(result.value().basePackage().value()).isEqualTo("com.acme.pedidoapi");
        assertThat(result.value().artifactId().mainClassName()).isEqualTo("PedidoApiApplication");
    }

    @Test
    @DisplayName("ENG-VAL-01 #2 artifactId com underscore é inválido")
    void rejectsArtifactIdWithUnderscore() {
        Checked<ProjectIdentity> result = validator.validate(draft("pedido_api", "com.acme", "", ""));

        assertThat(result.violations())
                .containsExactly(Violation.of(ViolationCode.INVALID_ARTIFACT_ID, ARTIFACT_ID));
    }

    @Test
    @DisplayName("ENG-VAL-01 #3 artifactId com 65 caracteres é inválido")
    void rejectsArtifactIdLongerThanSixtyFourCharacters() {
        Checked<ProjectIdentity> result = validator.validate(draft("a".repeat(65), "com.acme", "", ""));

        assertThat(result.violations())
                .containsExactly(Violation.of(ViolationCode.INVALID_ARTIFACT_ID, ARTIFACT_ID));
    }

    @Test
    @DisplayName("ENG-VAL-01 #4 groupId com segmento vazio é inválido")
    void rejectsGroupIdWithEmptySegment() {
        Checked<ProjectIdentity> result = validator.validate(draft("pedido-api", "com..acme", "", ""));

        assertThat(result.violations())
                .containsExactly(Violation.of(ViolationCode.INVALID_GROUP_ID, GROUP_ID));
    }

    @Test
    @DisplayName("ENG-VAL-01 #5 pacote com palavra reservada informa o segmento")
    void rejectsBasePackageWithReservedWord() {
        Checked<ProjectIdentity> result = validator.validate(draft("pedido-api", "com.acme", "com.acme.new", ""));

        assertThat(result.violations())
                .containsExactly(Violation.of(ViolationCode.INVALID_BASE_PACKAGE, BASE_PACKAGE, "segment", "new"));
    }

    @Test
    @DisplayName("ENG-VAL-01 #10 pacote de um trecho só é recusado")
    void rejectsSingleSegmentBasePackage() {
        Checked<ProjectIdentity> result = validator.validate(draft("pedido", "com.empresa", "main", ""));

        assertThat(result.violations())
                .containsExactly(Violation.of(ViolationCode.BASE_PACKAGE_TOO_SHORT, BASE_PACKAGE));
    }

    @Test
    @DisplayName("ENG-VAL-01 #10 pacote de dois trechos é aceito")
    void acceptsTwoSegmentBasePackage() {
        Checked<ProjectIdentity> result = validator.validate(draft("pedido", "com.empresa", "br.pedido", ""));

        assertThat(result.isValid()).isTrue();
    }

    @Test
    @DisplayName("ENG-VAL-01 #10 trecho reservado em pacote de um trecho continua INVALID_BASE_PACKAGE")
    void reservedWordWinsOverTooShort() {
        Checked<ProjectIdentity> result = validator.validate(draft("pedido", "com.empresa", "new", ""));

        assertThat(result.violations())
                .containsExactly(Violation.of(ViolationCode.INVALID_BASE_PACKAGE, BASE_PACKAGE, "segment", "new"));
    }

    @Test
    @DisplayName("BR-003 pacote derivado nunca é curto: group de um trecho + artifact tem dois")
    void derivedPackageFromSingleSegmentGroupIsValid() {
        Checked<ProjectIdentity> result = validator.validate(draft("pedido", "acme", "", ""));

        assertThat(result.isValid()).isTrue();
    }

    @Test
    @DisplayName("ENG-VAL-01 #6 coleta todas as violações na ordem dos campos")
    void collectsAllViolationsInFieldOrder() {
        Checked<ProjectIdentity> result = validator.validate(draft("1app", "", "", ""));

        assertThat(result.violations()).containsExactly(
                Violation.of(ViolationCode.INVALID_ARTIFACT_ID, ARTIFACT_ID),
                Violation.of(ViolationCode.INVALID_GROUP_ID, GROUP_ID));
    }

    @Test
    @DisplayName("ENG-VAL-01 #7 artifactId começando com dígito é inválido")
    void rejectsArtifactIdStartingWithDigit() {
        Checked<ProjectIdentity> result = validator.validate(draft("3d-viewer", "com.acme", "", ""));

        assertThat(result.violations())
                .containsExactly(Violation.of(ViolationCode.INVALID_ARTIFACT_ID, ARTIFACT_ID));
    }

    @Test
    @DisplayName("ENG-VAL-01 #8 descrição com quebra de linha é inválida")
    void rejectsDescriptionWithLineBreak() {
        Checked<ProjectIdentity> result = validator.validate(draft("pedido-api", "com.acme", "", "linha 1\nlinha 2"));

        assertThat(result.violations())
                .containsExactly(Violation.of(ViolationCode.INVALID_DESCRIPTION, DESCRIPTION, "max", "255"));
    }

    @Test
    @DisplayName("ENG-VAL-01 #9 descrição com caracteres especiais é válida e não é escapada")
    void keepsSpecialCharactersInDescription() {
        Checked<ProjectIdentity> result = validator.validate(draft("pedido-api", "com.acme", "", "API <beta> & cia"));

        assertThat(result.isValid()).isTrue();
        assertThat(result.value().description().value()).isEqualTo("API <beta> & cia");
    }

    @Test
    @DisplayName("BR-003 pacote derivado inválido aponta o segmento do artifactId")
    void rejectsDerivedPackageWhenArtifactIdIsReservedWord() {
        Checked<ProjectIdentity> result = validator.validate(draft("new", "com.acme", "", ""));

        assertThat(result.violations())
                .containsExactly(Violation.of(ViolationCode.INVALID_BASE_PACKAGE, BASE_PACKAGE, "segment", "new"));
    }

    @Test
    @DisplayName("BR-004 descrição ausente é aceita como vazia")
    void acceptsMissingDescription() {
        Checked<ProjectIdentity> result = validator.validate(draft("pedido-api", "com.acme", null, null));

        assertThat(result.isValid()).isTrue();
        assertThat(result.value().description().value()).isEmpty();
    }

    private static ProjectDraft draft(String artifactId, String groupId, String basePackage, String description) {
        return new ProjectDraft(
                artifactId, groupId, basePackage, description,
                null, null, null, null, null, null, null, true);
    }
}
