package io.github.arthursouto.archwright.engine.application.usecase;

import io.github.arthursouto.archwright.engine.adapter.out.template.ClasspathTemplateRepository;
import io.github.arthursouto.archwright.engine.adapter.out.template.MustacheTemplateRenderer;
import io.github.arthursouto.archwright.engine.domain.kit.Kits;
import io.github.arthursouto.archwright.engine.domain.model.FilePlan;
import io.github.arthursouto.archwright.engine.domain.model.PlannedFile;
import io.github.arthursouto.archwright.engine.domain.model.ProjectSpec;
import io.github.arthursouto.archwright.engine.domain.model.TemplateContext;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class FitnessSelectionTest {

    private static final String TESTS = "src/test/java/com/acme/pedidoapi/architecture/";

    private final ContextBuilder contextBuilder = new ContextBuilder(
            new ProfileResolver(SpecBuilder.catalog()),
            SpecBuilder.catalog(),
            SpecBuilder.repository().styles(),
            Kits.standard());

    private final FilePlanner planner = new FilePlanner(
            new ClasspathTemplateRepository(), new MustacheTemplateRenderer(), SpecBuilder.repository().styles());

    @Test
    @DisplayName("ENG-PLN-02 #1 estilo modular sem kit: sem AdaptersTest e sem PersistenceTest")
    void selectsOnlyTheClassesThatApplyToTheModularStyleWithoutKit() {
        FilePlan plan = plan(SpecBuilder.basic().noKit().build());

        assertThat(testClasses(plan)).containsExactlyInAnyOrder(
                "HexagonalLayersTest", "DomainPurityTest", "PortsTest", "CodingRulesTest", "ModuleBoundariesTest");
    }

    @Test
    @DisplayName("ENG-PLN-02 #2 estilo em camadas com kit BASIC: sem ModuleBoundariesTest")
    void selectsAdaptersAndPersistenceWithoutModuleBoundariesForTheLayeredStyle() {
        TemplateContext context = contextBuilder.build(SpecBuilder.basic().layered("paciente").build());

        assertThat(context.sections("fitness.classes")).extracting(entry -> entry.text("testClass"))
                .containsExactlyInAnyOrder(
                        "HexagonalLayersTest", "DomainPurityTest", "PortsTest", "AdaptersTest", "PersistenceTest", "CodingRulesTest");
    }

    @Test
    @DisplayName("ENG-PLN-02 #3 fitness desligada: nenhuma classe em architecture, sem archunit.properties")
    void plansNoFitnessFilesWhenDisabled() {
        FilePlan plan = plan(SpecBuilder.basic().fitness(false).build());

        assertThat(plan.targets()).noneMatch(target -> target.contains("/architecture/"));
        assertThat(plan.targets()).doesNotContain("src/test/resources/archunit.properties");
        assertThat(contextBuilder.build(SpecBuilder.basic().fitness(false).build()).sections("fitness.rules")).isEmpty();
    }

    @Test
    @DisplayName("ENG-PLN-02 #4 fitness ligada: archunit.properties presente")
    void plansTheArchunitPropertiesWhenEnabled() {
        assertThat(plan(SpecBuilder.basic().build()).targets()).contains("src/test/resources/archunit.properties");
    }

    @Test
    void givesTheArchitecturePackageItsPackageInfo() {
        FilePlan plan = plan(SpecBuilder.basic().noKit().build());

        PlannedFile packageInfo = plan.files().stream()
                .filter(file -> file.target().equals(TESTS + "package-info.java"))
                .findFirst()
                .orElseThrow();

        assertThat(packageInfo.template()).isEqualTo("common/package-info.java.mustache");
        assertThat(packageInfo.variables()).containsEntry("package", "com.acme.pedidoapi.architecture");
    }

    @Test
    void plansEachClassFromTheFitnessTemplateDirectory() {
        PlannedFile ports = plan(SpecBuilder.basic().noKit().build()).files().stream()
                .filter(file -> file.target().equals(TESTS + "PortsTest.java"))
                .findFirst()
                .orElseThrow();

        assertThat(ports.template()).isEqualTo("fitness/PortsTest.java.mustache");
        assertThat(ports.render()).isTrue();
    }

    @Test
    void exposesTheSelectedRulesToTheReadmeWithTheirClass() {
        TemplateContext context = contextBuilder.build(SpecBuilder.basic().noKit().build());

        assertThat(context.sections("fitness.rules")).extracting(rule -> rule.text("id"))
                .containsExactly(
                        "FF-LAY-01", "FF-LAY-02", "FF-DOM-01", "FF-PRT-01", "FF-PRT-02",
                        "FF-MOD-01", "FF-MOD-02", "FF-MOD-03", "FF-COD-01", "FF-COD-02", "FF-COD-03");
        assertThat(context.sections("fitness.rules").getFirst().text("testClass")).isEqualTo("HexagonalLayersTest");
    }

    private FilePlan plan(ProjectSpec spec) {
        return planner.plan(spec, contextBuilder.build(spec));
    }

    private static List<String> testClasses(FilePlan plan) {
        return plan.targets().stream()
                .filter(target -> target.startsWith(TESTS) && !target.endsWith("package-info.java"))
                .map(target -> target.substring(TESTS.length(), target.length() - ".java".length()))
                .toList();
    }
}
