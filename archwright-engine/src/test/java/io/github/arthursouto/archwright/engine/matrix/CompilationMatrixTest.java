package io.github.arthursouto.archwright.engine.matrix;

import io.github.arthursouto.archwright.engine.application.port.in.ExportFormat;
import io.github.arthursouto.archwright.engine.application.port.in.ExportTarget;
import io.github.arthursouto.archwright.engine.application.port.in.GenerationResult;
import io.github.arthursouto.archwright.engine.config.ArchwrightEngine;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;

class CompilationMatrixTest {

    private static final String OUTPUT_PROPERTY = "matrix.output";

    @TempDir
    Path workspace;

    private final ArchwrightEngine engine = ArchwrightEngine.builder().offline(true).build();
    private final List<MatrixCase> cases = new MatrixCases(engine.catalog()).all();

    @Test
    @DisplayName("BR-044 a matriz cobre toda linha do catálogo, os dois estilos e os dois kits")
    void coversEveryLineStyleAndKit() {
        assertThat(cases).extracting(matrixCase -> matrixCase.request().bootVersion().substring(0, 3))
                .containsAll(List.of("2.7", "3.0", "3.1", "3.2", "3.3", "3.4", "3.5", "4.0", "4.1"));
        assertThat(cases).extracting(matrixCase -> matrixCase.request().architectureStyle())
                .contains("HEXAGONAL_MODULAR", "HEXAGONAL_LAYERED");
        assertThat(cases).extracting(matrixCase -> matrixCase.request().kit()).contains("NONE", "BASIC");
    }

    @Test
    void namesEveryCaseUniquely() {
        assertThat(cases).extracting(MatrixCase::name).doesNotHaveDuplicates();
    }

    @Test
    void exercisesTheDefaultTheNewestTestedAndTheLegacyJava() {
        assertThat(javaOf("boot-3.5", "hexagonal-modular-basic")).containsExactlyInAnyOrder("21", "24");
        assertThat(javaOf("boot-4.1", "hexagonal-layered-none")).containsExactly("25");
        assertThat(javaOf("boot-2.7", "hexagonal-modular-none")).containsExactlyInAnyOrder("21", "8");
    }

    @Test
    void runsLegacyJavaOnAModernJdk() {
        assertThat(cases).filteredOn(matrixCase -> matrixCase.java().equals("8")).isNotEmpty()
                .allSatisfy(matrixCase -> assertThat(matrixCase.jdk()).isEqualTo("21"));
    }

    @Test
    @DisplayName("BR-044 todo caso da matriz gera um projeto válido")
    void generatesEveryCaseSuccessfully() throws IOException {
        Path destination = Files.createDirectory(workspace.resolve("out"));

        for (MatrixCase matrixCase : cases) {
            Path caseDirectory = Files.createDirectory(destination.resolve(matrixCase.name()));
            GenerationResult result = engine.generateProject()
                    .generate(matrixCase.request(), new ExportTarget(ExportFormat.FOLDER, caseDirectory, false));

            assertThat(result).as(matrixCase.name()).isInstanceOf(GenerationResult.Success.class);
        }
    }

    @Test
    @EnabledIfSystemProperty(named = OUTPUT_PROPERTY, matches = ".+")
    void writesTheCasesAndTheMatrixDescriptorForTheContinuousIntegration() throws IOException {
        Path output = Files.createDirectories(Path.of(System.getProperty(OUTPUT_PROPERTY)).toAbsolutePath());

        for (MatrixCase matrixCase : cases) {
            Path caseDirectory = Files.createDirectory(output.resolve(matrixCase.name()));
            GenerationResult result = engine.generateProject()
                    .generate(matrixCase.request(), new ExportTarget(ExportFormat.FOLDER, caseDirectory, false));

            assertThat(result).as(matrixCase.name()).isInstanceOf(GenerationResult.Success.class);
        }

        Files.writeString(output.resolve("matrix.json"), descriptor());
    }

    private String descriptor() {
        return cases.stream()
                .map(matrixCase -> "{\"name\":\"%s\",\"java\":\"%s\",\"jdk\":\"%s\"}"
                        .formatted(matrixCase.name(), matrixCase.java(), matrixCase.jdk()))
                .collect(Collectors.joining(",", "{\"include\":[", "]}"));
    }

    private List<String> javaOf(String bootPrefix, String styleAndKit) {
        return cases.stream()
                .filter(matrixCase -> matrixCase.name().startsWith(bootPrefix) && matrixCase.name().contains(styleAndKit))
                .map(MatrixCase::java)
                .toList();
    }
}
