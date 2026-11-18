package io.github.arthursouto.archwright.engine.application.usecase;

import io.github.arthursouto.archwright.engine.adapter.out.template.ClasspathTemplateRepository;
import io.github.arthursouto.archwright.engine.application.port.out.TemplateRepository;
import io.github.arthursouto.archwright.engine.domain.model.Manifest;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class FitnessTemplateVerifierTest {

    @Test
    @DisplayName("ENG-CAT-01 #5 todo template citado em fitness-rules.yaml existe")
    void acceptsTheOfficialCatalog() {
        FitnessTemplateVerifier verifier = new FitnessTemplateVerifier(SpecBuilder.repository(), new ClasspathTemplateRepository());

        assertThatCode(verifier::verify).doesNotThrowAnyException();
    }

    @Test
    @DisplayName("ENG-CAT-01 #5 template inexistente falha citando a classe e o caminho")
    void rejectsAMissingTemplateCitingTheClassAndThePath() {
        TemplateRepository empty = new TemplateRepository() {
            @Override
            public Manifest manifest(String directory) {
                throw new UnsupportedOperationException();
            }

            @Override
            public byte[] content(String path) {
                throw new IllegalStateException("template: " + path + ": not found on classpath");
            }
        };

        assertThatThrownBy(() -> new FitnessTemplateVerifier(SpecBuilder.repository(), empty).verify())
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("fitness-rules.yaml")
                .hasMessageContaining("HexagonalLayersTest")
                .hasMessageContaining("fitness/HexagonalLayersTest.java.mustache");
    }
}
