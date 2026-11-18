package io.github.arthursouto.archwright.engine.domain.validation;

import io.github.arthursouto.archwright.engine.domain.model.DependencyId;
import io.github.arthursouto.archwright.engine.domain.model.ExtraId;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class ExtraValidatorTest {

    private static final String EXTRAS = "extras";

    private final ExtraValidator validator = new ExtraValidator(TestCatalog.standard());

    @Test
    @DisplayName("ENG-VAL-04 #7 docker-compose sem jpa é recusado")
    void rejectsDockerComposeWithoutJpa() {
        Checked<List<ExtraId>> result = validator.validate(draft("docker-compose"), dependencies("web"));

        assertThat(result.violations()).containsExactly(new Violation(
                ViolationCode.EXTRA_REQUIRES_DEPENDENCY, EXTRAS, Map.of("id", "docker-compose", "requires", "jpa")));
    }

    @Test
    @DisplayName("BR-034 docker-compose com jpa é aceito")
    void acceptsDockerComposeWithJpa() {
        Checked<List<ExtraId>> result = validator.validate(draft("docker-compose"), dependencies("web", "jpa"));

        assertThat(result.value()).containsExactly(new ExtraId("docker-compose"));
    }

    @Test
    @DisplayName("BR-034 github-actions não tem pré-requisito")
    void acceptsGithubActionsWithoutDependencies() {
        Checked<List<ExtraId>> result = validator.validate(draft("github-actions"), dependencies());

        assertThat(result.value()).containsExactly(new ExtraId("github-actions"));
    }

    @Test
    @DisplayName("BR-034 extra desconhecido é recusado")
    void rejectsUnknownExtra() {
        Checked<List<ExtraId>> result = validator.validate(draft("terraform"), dependencies("jpa"));

        assertThat(result.violations()).containsExactly(
                Violation.of(ViolationCode.UNKNOWN_EXTRA, EXTRAS, "id", "terraform"));
    }

    @Test
    @DisplayName("BR-034 extra repetido entra uma vez só")
    void keepsDuplicatedExtraOnce() {
        Checked<List<ExtraId>> result = validator.validate(draft("github-actions", " github-actions "), dependencies());

        assertThat(result.value()).containsExactly(new ExtraId("github-actions"));
    }

    @Test
    @DisplayName("BR-070 dependências inválidas não geram violação de pré-requisito")
    void skipsRequirementsWhenDependenciesAreInvalid() {
        Checked<List<DependencyId>> invalidDependencies = Checked.invalid(
                Violation.of(ViolationCode.UNKNOWN_DEPENDENCY, "dependencies", "id", "kafka"));

        Checked<List<ExtraId>> result = validator.validate(draft("docker-compose"), invalidDependencies);

        assertThat(result.isValid()).isTrue();
    }

    private static ProjectDraft draft(String... extras) {
        return DraftBuilder.valid().extras(extras).build();
    }

    private static Checked<List<DependencyId>> dependencies(String... ids) {
        return Checked.valid(java.util.Arrays.stream(ids).map(DependencyId::new).toList());
    }
}
