package io.github.arthursouto.archwright.desktop.wizard;

import io.github.arthursouto.archwright.desktop.i18n.Messages;
import io.github.arthursouto.archwright.engine.application.port.in.BootVersionOption;
import io.github.arthursouto.archwright.engine.application.port.in.CatalogQuery;
import io.github.arthursouto.archwright.engine.application.port.in.ExportFormat;
import io.github.arthursouto.archwright.engine.application.port.in.GenerationResult;
import io.github.arthursouto.archwright.engine.application.port.in.JavaVersionOption;
import io.github.arthursouto.archwright.engine.application.port.in.KitOption;
import io.github.arthursouto.archwright.engine.application.port.in.PreviewResult;
import io.github.arthursouto.archwright.engine.config.ArchwrightEngine;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;

class FormToEngineTest {

    @TempDir
    Path workspace;

    private ArchwrightEngine engine;
    private CatalogQuery catalog;
    private ProjectForm form;

    @BeforeEach
    void startEngineAndFillTheFormLikeTheWizard() throws IOException {
        engine = ArchwrightEngine.builder().cacheDirectory(workspace.resolve("cache")).offline(true).build();
        catalog = engine.catalog();
        form = new ProjectForm();

        Path destination = Files.createDirectory(workspace.resolve("out"));
        form.artifactId().set("pedido-api");
        form.groupId().set("com.acme");
        form.destination().set(destination.toString());
        pickStyle(0);
        pickLatestBoot();
        pickDefaultJava();
        pickKit(0);
    }

    @Test
    void theDefaultsTheWizardOffersFormAValidProject() {
        assertThat(engine.previewProject().preview(form.toRequest())).isInstanceOf(PreviewResult.Preview.class);
    }

    @Test
    void theModularPackageTreeShowsTheBasePackageAndEachModule() {
        form.modules().set("pedidos, pagamentos");

        PreviewResult.Preview preview = (PreviewResult.Preview) engine.previewProject().preview(form.toRequest());
        PackageNode root = PackageTrees.from(preview.files()).getFirst();

        assertThat(root.name()).isEqualTo("com.acme.pedidoapi");
        assertThat(root.children()).extracting(PackageNode::name).contains("bootstrap", "pagamentos", "pedidos", "shared");
    }

    @Test
    void theLayeredPackageTreeShowsTheLayersAndTheAggregates() {
        catalog.architectureStyles().stream()
                .filter(style -> !style.modulesRequired())
                .findFirst()
                .ifPresent(style -> form.architectureStyle().set(style.id()));
        form.modules().set("pedido, cliente");

        PreviewResult.Preview preview = (PreviewResult.Preview) engine.previewProject().preview(form.toRequest());
        PackageNode root = PackageTrees.from(preview.files()).getFirst();

        assertThat(root.name()).isEqualTo("com.acme.pedidoapi");
        assertThat(root.children()).extracting(PackageNode::name)
                .contains("application", "domain.model", "infrastructure", "PedidoApiApplication.java");
        PackageNode model = root.children().stream().filter(node -> node.name().equals("domain.model")).findFirst().orElseThrow();
        assertThat(model.children()).extracting(PackageNode::name).containsExactly("cliente", "pedido");
    }

    @Test
    void previewListsFilesDependenciesAndFitnessRules() {
        PreviewResult.Preview preview = (PreviewResult.Preview) engine.previewProject().preview(form.toRequest());

        assertThat(preview.paths()).contains("pom.xml");
        assertThat(preview.dependencies()).isNotEmpty();
        assertThat(preview.fitnessRules()).isNotEmpty();
    }

    @Test
    void generatesAZipThroughTheForm() {
        form.format().set(ExportFormat.ZIP);

        GenerationResult result = engine.generateProject().generate(form.toRequest(), form.toTarget(false));

        assertThat(result).isInstanceOf(GenerationResult.Success.class);
        assertThat(((GenerationResult.Success) result).output()).isRegularFile().hasFileName("pedido-api.zip");
    }

    @Test
    void generatesAFolderThroughTheForm() {
        form.format().set(ExportFormat.FOLDER);

        GenerationResult result = engine.generateProject().generate(form.toRequest(), form.toTarget(false));

        assertThat(result).isInstanceOf(GenerationResult.Success.class);
        assertThat(((GenerationResult.Success) result).output().resolve("pom.xml")).isRegularFile();
    }

    @Test
    void generatesTheLayeredStyleWithoutModules() {
        catalog.architectureStyles().stream()
                .filter(style -> !style.modulesRequired())
                .findFirst()
                .ifPresent(style -> form.architectureStyle().set(style.id()));
        form.modules().set("");

        assertThat(engine.previewProject().preview(form.toRequest())).isInstanceOf(PreviewResult.Preview.class);
    }

    @Test
    void askingToOverwriteAnExistingZipNeedsConfirmation() {
        form.format().set(ExportFormat.ZIP);
        engine.generateProject().generate(form.toRequest(), form.toTarget(false));

        GenerationResult second = engine.generateProject().generate(form.toRequest(), form.toTarget(false));
        GenerationResult confirmed = engine.generateProject().generate(form.toRequest(), form.toTarget(true));

        assertThat(second).isInstanceOf(GenerationResult.Failure.class);
        assertThat(((GenerationResult.Failure) second).violations()).extracting(violation -> violation.code())
                .containsExactly("TARGET_EXISTS");
        assertThat(confirmed).isInstanceOf(GenerationResult.Success.class);
    }

    @Test
    void invalidFieldsComeBackAssignedToTheirWizardSteps() {
        form.artifactId().set("Invalido!");
        form.bootVersion().set("9.9.9");

        PreviewResult result = engine.previewProject().preview(form.toRequest());
        FieldErrors errors = FieldErrors.from(((PreviewResult.Failure) result).violations(), Messages.portuguese());

        assertThat(errors.hasErrorIn(WizardStep.PROJECT)).isTrue();
        assertThat(errors.hasErrorIn(WizardStep.VERSIONS)).isTrue();
        assertThat(errors.hasErrorIn(WizardStep.DEPENDENCIES)).isFalse();
        assertThat(errors.messageFor("artifactId")).isNotBlank();
    }

    @Test
    void javaVersionsFollowTheChosenBoot() {
        assertThat(catalog.javaVersions(form.bootVersion().get())).extracting(JavaVersionOption::version)
                .contains(form.javaVersion().get());
    }

    private void pickStyle(int index) {
        form.architectureStyle().set(catalog.architectureStyles().get(index).id());

        if (catalog.architectureStyles().get(index).modulesRequired()) {
            form.modules().set("pedidos");
        }
    }

    private void pickLatestBoot() {
        form.bootVersion().set(catalog.bootVersions().versions().stream()
                .filter(option -> !option.legacy() && option.latestOfLine())
                .map(BootVersionOption::version)
                .findFirst()
                .orElseThrow());
    }

    private void pickDefaultJava() {
        form.javaVersion().set(catalog.javaVersions(form.bootVersion().get()).stream()
                .filter(JavaVersionOption::isDefault)
                .map(JavaVersionOption::version)
                .findFirst()
                .orElseThrow());
    }

    private void pickKit(int index) {
        KitOption kit = catalog.kits().get(index);
        form.kit().set(kit.id());
        form.dependencies().addAll(kit.dependencies());
    }
}
