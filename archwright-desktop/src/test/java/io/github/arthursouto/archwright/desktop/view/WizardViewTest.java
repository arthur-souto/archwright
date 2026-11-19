package io.github.arthursouto.archwright.desktop.view;

import io.github.arthursouto.archwright.desktop.history.ProjectHistory;
import io.github.arthursouto.archwright.desktop.i18n.Messages;
import io.github.arthursouto.archwright.desktop.preferences.UserPreferences;
import io.github.arthursouto.archwright.desktop.wizard.PackageNode;
import io.github.arthursouto.archwright.desktop.wizard.ProjectForm;
import io.github.arthursouto.archwright.engine.application.port.in.ArchitectureStyleOption;
import io.github.arthursouto.archwright.engine.application.port.in.BootVersionOption;
import io.github.arthursouto.archwright.engine.application.port.in.CatalogQuery;
import io.github.arthursouto.archwright.engine.application.port.in.GenerateProjectUseCase;
import io.github.arthursouto.archwright.engine.application.port.in.JavaVersionOption;
import io.github.arthursouto.archwright.engine.application.port.in.KitOption;
import io.github.arthursouto.archwright.engine.application.port.in.PreviewProjectQuery;
import io.github.arthursouto.archwright.engine.application.port.in.PreviewResult;
import io.github.arthursouto.archwright.engine.application.port.in.Violation;
import io.github.arthursouto.archwright.engine.config.ArchwrightEngine;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.stage.Window;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.RadioButton;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.control.TitledPane;
import javafx.scene.control.TreeItem;
import javafx.scene.control.TreeView;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Proxy;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Clock;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.prefs.BackingStoreException;
import java.util.prefs.Preferences;

import static org.assertj.core.api.Assertions.assertThat;

class WizardViewTest {

    @TempDir
    Path workspace;

    private Preferences node;
    private Preferences historyNode;
    private ProjectHistory history;
    private UserPreferences preferences;
    private ProjectForm form;
    private WizardView view;
    private Path destination;
    private ArchwrightEngine engine;
    private final RecordingDialogs dialogs = new RecordingDialogs();

    @BeforeEach
    void openTheWizard() throws IOException {
        Assumptions.assumeTrue(Fx.available(), "JavaFX toolkit not available (headless environment)");

        node = Preferences.userRoot().node("archwright-test-" + UUID.randomUUID());
        preferences = new UserPreferences(node);
        historyNode = Preferences.userRoot().node("archwright-test-history-" + UUID.randomUUID());
        history = new ProjectHistory(historyNode, Clock.systemDefaultZone());
        destination = Files.createDirectory(workspace.resolve("out"));
        engine = ArchwrightEngine.builder().cacheDirectory(workspace.resolve("cache")).offline(true).build();

        open(engine.catalog(), engine.generateProject());
    }

    private void open(CatalogQuery catalog, GenerateProjectUseCase generator) {
        open(catalog, engine.previewProject(), generator);
    }

    private void open(CatalogQuery catalog, PreviewProjectQuery preview, GenerateProjectUseCase generator) {
        DesktopContext context = new DesktopContext(
                catalog, preview, generator, Messages.portuguese(), preferences, history, dialogs);

        form = new ProjectForm();
        preferences.restoreInto(form);
        view = Fx.call(() -> {
            WizardView created = new WizardView(context, form);
            new Scene(created, 920, 760);

            return created;
        });
    }

    @AfterEach
    void closeTheWizard() throws BackingStoreException {
        if (node != null) {
            node.removeNode();
        }

        if (historyNode != null) {
            historyNode.removeNode();
        }
    }

    @Test
    void everyFieldOfTheProjectStepExplainsWhatItIs() {
        Messages messages = Messages.portuguese();

        assertThat(labels()).contains(
                messages.text("project.artifactId.help"), messages.text("project.groupId.help"),
                messages.text("project.basePackage.help"), messages.text("project.description.help"),
                messages.text("project.style.help"), messages.text("project.modules.help"));
    }

    @Test
    void everyFieldOfTheVersionsStepExplainsWhatItIs() {
        goToVersions();
        Messages messages = Messages.portuguese();

        assertThat(labels()).contains(messages.text("versions.boot.help"), messages.text("versions.java.help"));
    }

    @Test
    void theDependenciesStepExplainsKitDependenciesExtrasAndFitnessFunctions() {
        goToDependencies();
        Messages messages = Messages.portuguese();

        assertThat(labels()).contains(
                messages.text("dependencies.kit.help"), messages.text("dependencies.extras.help"),
                messages.text("dependencies.fitness.help"), messages.text("extra.docker-compose.help"),
                messages.text("extra.github-actions.help"),
                "API REST com Spring MVC e Tomcat.", "Migrations versionadas do banco.");
    }

    @Test
    void theKitShowsWhichDependenciesItMarks() {
        goToDependencies();

        assertThat(labels()).contains("Marca: Spring Web, Validation, Actuator, JPA + PostgreSQL, Flyway, Lombok.");

        selectKit("NONE");

        Fx.await(() -> labels().contains("Não marca nenhuma dependência; você escolhe tudo na lista abaixo."));
    }

    @Test
    void eachDependencyShowsTheMavenArtifactsItBrings() {
        goToDependencies();

        assertThat(labels()).contains(
                "Maven: spring-boot-starter-webmvc", "Maven: postgresql (runtime)");
        assertThat(labels()).anyMatch(text -> text.startsWith("Maven: springdoc-openapi-starter-webmvc-ui:"));
    }

    @Test
    void eachExtraSaysWhichFilesItGenerates() {
        goToDependencies();

        assertThat(labels()).contains("Gera: docker-compose.yml e .env.example", "Gera: .github/workflows/ci.yml");
    }

    @Test
    void eachRuleExplainsWhyItMatters() {
        goToDependencies();
        Messages messages = Messages.portuguese();

        assertThat(labels()).contains(
                messages.text("rule.FF-ADP-01.help"), messages.text("rule.FF-MOD-01.help"), messages.text("rule.FF-COD-01.help"));
    }

    @Test
    void theExportStepExplainsFormatAndDestination() {
        goToExport();
        Messages messages = Messages.portuguese();

        assertThat(labels()).contains(messages.text("export.format.help"), messages.text("export.destination.help"));
    }

    @Test
    void theTreeShowsTheTotalsOfTheGeneratedProject() {
        fillProject();

        Fx.await(() -> labels().stream().anyMatch(text -> text.matches("Prévia: \\d+ arquivos, \\d+ linhas no total\\.")));
    }

    @Test
    void selectingAPackageShowsWhatItIsForAndItsNumbers() {
        fillProject();
        Fx.await(() -> treeNames().contains("pedidos"));

        selectInTree("com.acme.pedidoapi.pedidos");

        Fx.await(() -> labels().contains("com.acme.pedidoapi.pedidos"));
        assertThat(labels()).anyMatch(text -> text.matches("Pacotes: \\d+ · Classes Java: \\d+ · Linhas: \\d+ \\(com os package-info\\)\\."));
        assertThat(labels()).anyMatch(text -> text.startsWith("Módulo Pedidos:"));
    }

    @Test
    void selectingAClassShowsItsLines() {
        fillProject();
        Fx.await(() -> treeNames().contains("PedidoApiApplication.java"));

        selectInTree("PedidoApiApplication.java");

        Fx.await(() -> labels().stream().anyMatch(text -> text.matches("Classe Java · Linhas: \\d+")));
    }

    @Test
    void theTreeStartsWithAProvisionalExampleAndSaysSo() {
        Fx.await(() -> treeNames().contains("com.empresa.projeto"));

        assertThat(treeNames()).contains("modulo", "shared", "bootstrap");
        assertThat(labels()).contains("Exemplo com nomes provisórios: a estrutura final aparece quando os campos estiverem preenchidos.");
    }

    @Test
    void theTreeFollowsGroupArtifactAndBasePackageWhileTyping() {
        fillProject();

        Fx.await(() -> treeNames().contains("com.acme.pedidoapi"));
        Fx.await(() -> labels().stream().noneMatch(text -> text.startsWith("Exemplo com nomes provisórios")));

        type("Sugestão: com.acme.pedidoapi", "br.pedido");

        Fx.await(() -> treeNames().contains("br.pedido"));
        assertThat(treeNames()).doesNotContain("com.acme.pedidoapi");
    }

    @Test
    void theTreeShowsEachModuleAsItIsTyped() {
        fillIdentity();
        type("pedidos, pagamentos", "pedidos");

        Fx.await(() -> treeNames().contains("pedidos"));
        assertThat(treeNames()).doesNotContain("pagamentos");

        type("pedidos, pagamentos", "pedidos, pagamentos");

        Fx.await(() -> treeNames().contains("pagamentos"));
        assertThat(treeNames()).contains("pedidos", "shared", "bootstrap");
    }

    @Test
    void theTreeChangesWithTheArchitectureStyle() {
        fillProject();
        Fx.await(() -> treeNames().contains("bootstrap"));

        Fx.run(() -> comboBox(0).getSelectionModel().select(1));
        type("pedidos, pagamentos", "pedido, cliente");

        Fx.await(() -> treeNames().contains("infrastructure"));
        Fx.await(() -> treeNames().contains("cliente"));
        assertThat(treeNames()).contains("application", "domain.model", "PedidoApiApplication.java")
                .doesNotContain("bootstrap", "shared");

        Fx.run(() -> comboBox(0).getSelectionModel().select(0));
        type("pedidos, pagamentos", "pedidos");

        Fx.await(() -> treeNames().contains("shared"));
        assertThat(treeNames()).doesNotContain("infrastructure");
    }

    @Test
    void anInvalidNameKeepsTheTreeWithAProvisionalName() {
        type("com.empresa", "com.acme");
        type("pedido-api", "Pedido API");

        Fx.await(() -> treeNames().contains("com.acme.projeto"));
        assertThat(labels()).anyMatch(text -> text.startsWith("Exemplo com nomes provisórios"));
    }

    @Test
    void theTreeIsMarkedAsOutdatedWhenNothingCanBeShown() {
        open(engine.catalog(), request -> new PreviewResult.Failure(List.of(
                new Violation("UNSUPPORTED_BOOT_VERSION", "bootVersion", Map.of("value", "")))), engine.generateProject());

        Fx.await(() -> labels().contains("Corrija os campos destacados para atualizar a estrutura."));
        assertThat(treeNames()).isEmpty();
    }

    @Test
    void theDefaultVersionsAreFilledBeforeTheVersionsStepIsOpened() {
        Fx.await(() -> !form.bootVersion().get().isBlank() && !form.javaVersion().get().isBlank());

        assertThat(catalogJavaVersionsFor(form.bootVersion().get())).contains(form.javaVersion().get());
    }

    @Test
    void theTreePanelStaysVisibleOnEveryStep() {
        goToVersions();
        assertThat(labels()).contains("Estrutura de pacotes");

        click("Avançar");
        Fx.await(() -> labels().contains("Dependências e fitness functions"));
        assertThat(labels()).contains("Estrutura de pacotes");

        click("Avançar");
        Fx.await(() -> labels().contains("Exportar"));
        assertThat(labels()).contains("Estrutura de pacotes");
        Fx.await(() -> treeNames().contains("pedidos"));
    }

    @Test
    void startsOnTheProjectStepWithoutBackButton() {
        assertThat(labels()).contains("Dados do projeto");
        assertThat(Fx.call(() -> button("Voltar").isDisabled())).isTrue();
        assertThat(Fx.call(() -> hasVisibleButton("Avançar"))).isTrue();
    }

    @Test
    void nextIsBlockedAndShowsTheErrorsWhileTheStepIsInvalid() {
        click("Avançar");

        Fx.await(() -> labels().stream().anyMatch(text -> text.startsWith("Use letras minúsculas")));
        assertThat(labels()).contains("Dados do projeto").doesNotContain("Spring Boot e Java");
        assertThat(labels()).anyMatch(text -> text.startsWith("Use segmentos em minúsculas"));
        assertThat(labels()).contains("Informe ao menos um módulo.");
    }

    @Test
    void typingAnInvalidArtifactShowsOnlyThatFieldErrorAndFixingItClearsTheError() {
        type("com.empresa", "com.acme");
        type("pedido-api", "Pedido API");

        Fx.await(() -> labels().stream().anyMatch(text -> text.startsWith("Use letras minúsculas")));
        assertThat(labels()).noneMatch(text -> text.startsWith("Use segmentos em minúsculas"));

        type("pedido-api", "pedido-api");

        Fx.await(() -> labels().stream().noneMatch(text -> text.startsWith("Use letras minúsculas")));
    }

    @Test
    void aSingleSegmentBasePackageIsRejectedOnTheBasePackageField() {
        type("com.empresa.projeto", "main");

        Fx.await(() -> labels().contains("O pacote base precisa de ao menos dois trechos, como com.empresa.projeto."));

        type("com.empresa.projeto", "com.empresa.pedido");

        Fx.await(() -> labels().stream().noneMatch(text -> text.startsWith("O pacote base precisa")));
    }

    @Test
    void validProjectDataLetsTheUserWalkThroughEveryStep() {
        fillProject();
        click("Avançar");

        Fx.await(() -> labels().contains("Spring Boot e Java"));
        Fx.await(() -> !Fx.call(() -> comboBox(0).getItems().isEmpty()));
        assertThat(Fx.call(() -> comboBox(0).getValue())).isNotNull();
        assertThat(Fx.call(() -> comboBox(1).getValue())).isNotNull();
        assertThat(labels()).anyMatch(text -> text.startsWith("Sem conexão"));
        assertThat(form.bootVersion().get()).isNotBlank();
        assertThat(form.javaVersion().get()).isNotBlank();

        click("Avançar");
        Fx.await(() -> labels().contains("Dependências e fitness functions"));
        assertThat(form.dependencies()).contains("web", "jpa");

        click("Avançar");
        Fx.await(() -> labels().contains("Exportar"));
        assertThat(Fx.call(() -> hasVisibleButton("Avançar"))).isFalse();
    }

    @Test
    void theStepIndicatorGoesBackToAnEarlierStep() {
        goToVersions();

        Fx.run(() -> label("1. Projeto").getOnMouseClicked().handle(null));

        assertThat(labels()).contains("Dados do projeto");
        assertThat(Fx.call(() -> button("Voltar").isDisabled())).isTrue();
    }

    @Test
    void backReturnsToThePreviousStep() {
        goToVersions();

        click("Voltar");

        assertThat(labels()).contains("Dados do projeto");
    }

    @Test
    void switchingTheStyleRelabelsTheListFieldAndClearsIt() {
        assertThat(labels()).contains("Módulos (separados por vírgula)");
        type("pedidos, pagamentos", "pedidos");

        Fx.run(() -> comboBox(0).getSelectionModel().select(1));

        assertThat(form.architectureStyle().get()).isEqualTo("HEXAGONAL_LAYERED");
        assertThat(labels()).contains("Agregados (separados por vírgula)").doesNotContain("Módulos (separados por vírgula)");
        assertThat(Fx.call(() -> field("pedidos, pagamentos").getParent().isVisible())).isTrue();
        assertThat(form.modules().get()).isEmpty();

        Fx.run(() -> comboBox(0).getSelectionModel().select(0));

        assertThat(labels()).contains("Módulos (separados por vírgula)");
    }

    @Test
    void basicKitSelectsItsDependenciesAndNoneClearsThem() {
        goToDependencies();
        assertThat(form.dependencies()).contains("web", "jpa", "flyway");
        assertThat(Fx.call(() -> checkBox("Spring Web").isSelected())).isTrue();

        selectKit("NONE");

        assertThat(form.kit().get()).isEqualTo("NONE");
        assertThat(form.dependencies()).isEmpty();
        assertThat(Fx.call(() -> checkBox("Spring Web").isSelected())).isFalse();
    }

    @Test
    void togglingADependencyUpdatesTheFormAndTheFitnessRules() {
        goToDependencies();
        selectKit("NONE");
        assertThat(labels()).noneMatch(text -> text.startsWith("FF-ADP-01"));

        Fx.run(() -> checkBox("Spring Web").setSelected(true));

        assertThat(form.dependencies()).contains("web");
        assertThat(labels()).anyMatch(text -> text.startsWith("FF-ADP-01"));
    }

    @Test
    void extrasStayDisabledUntilTheirDependencyIsSelected() {
        goToDependencies();
        selectKit("NONE");

        assertThat(Fx.call(() -> checkBox("docker-compose.yml com PostgreSQL").isDisabled())).isTrue();

        Fx.run(() -> checkBox("JPA + PostgreSQL").setSelected(true));

        assertThat(Fx.call(() -> checkBox("docker-compose.yml com PostgreSQL").isDisabled())).isFalse();

        Fx.run(() -> checkBox("docker-compose.yml com PostgreSQL").setSelected(true));
        assertThat(form.extras()).containsExactly("docker-compose");

        Fx.run(() -> checkBox("JPA + PostgreSQL").setSelected(false));
        assertThat(form.extras()).isEmpty();
    }

    @Test
    void fitnessFunctionsCanBeTurnedOffAndTheRulesDisappear() {
        goToDependencies();
        assertThat(labels()).anyMatch(text -> text.startsWith("FF-LAY-01"));

        Fx.run(() -> checkBox("Gerar fitness functions (ArchUnit)").setSelected(false));

        assertThat(form.fitnessFunctions().get()).isFalse();
        assertThat(Fx.call(() -> labels().stream().filter(text -> text.startsWith("FF-LAY-01")).count())).isZero();
    }

    @Test
    void generatesAZipFromTheWizardAndRemembersTheChoices() {
        goToExport();
        form.destination().set(destination.toString());
        Fx.await(() -> !Fx.call(() -> button("Gerar projeto").isDisabled()));

        click("Gerar projeto");

        Fx.await(() -> labels().stream().anyMatch(text -> text.startsWith("Projeto gerado:")));
        assertThat(destination.resolve("pedido-api.zip")).isRegularFile();
        assertThat(Fx.call(() -> button("Abrir pasta").isVisible())).isTrue();

        ProjectForm restored = new ProjectForm();
        preferences.restoreInto(restored);
        assertThat(restored.groupId().get()).isEqualTo("com.acme");
        assertThat(restored.destination().get()).isEqualTo(destination.toString());
    }

    @Test
    void generatesAFolderWithTheLayeredStyleAndAggregatesFromTheWizard() {
        Fx.run(() -> comboBox(0).getSelectionModel().select(1));
        fillIdentity();
        type("pedidos, pagamentos", "pedido, cliente");
        click("Avançar");
        Fx.await(() -> labels().contains("Spring Boot e Java"));
        Fx.await(() -> !Fx.call(() -> comboBox(0).getItems().isEmpty()));
        click("Avançar");
        Fx.await(() -> labels().contains("Dependências e fitness functions"));
        click("Avançar");
        Fx.await(() -> labels().contains("Exportar"));
        Fx.run(() -> radio("Pasta").setSelected(true));
        form.destination().set(destination.toString());
        Fx.await(() -> !Fx.call(() -> button("Gerar projeto").isDisabled()));

        click("Gerar projeto");

        Fx.await(() -> labels().stream().anyMatch(text -> text.startsWith("Projeto gerado:")));
        assertThat(destination.resolve("pedido-api/pom.xml")).isRegularFile();
        Path model = destination.resolve("pedido-api/src/main/java/com/acme/pedidoapi/domain/model");
        assertThat(model.resolve("pedido")).isDirectory();
        assertThat(model.resolve("cliente")).isDirectory();
    }

    @Test
    void aMissingDestinationFolderShowsTheErrorOnTheDestinationField() {
        goToExport();
        form.destination().set(workspace.resolve("nao-existe").toString());
        Fx.await(() -> !Fx.call(() -> button("Gerar projeto").isDisabled()));

        click("Gerar projeto");

        Fx.await(() -> labels().contains("A pasta de destino não existe."));
        assertThat(labels()).noneMatch(text -> text.startsWith("Projeto gerado:"));
    }

    @Test
    void generateStaysDisabledWithoutADestination() {
        goToExport();

        assertThat(Fx.call(() -> button("Gerar projeto").isDisabled())).isTrue();
    }

    @Test
    void exportSummaryListsFilesDependenciesAndRules() {
        goToExport();

        Fx.await(() -> paneTitles().stream().anyMatch(text -> text.startsWith("Arquivos (")));
        assertThat(paneTitles()).contains("Dependências", "Fitness functions");
    }

    @Test
    void chooseButtonFillsTheDestinationFromTheDialogAndCancelKeepsIt() {
        goToExport();
        dialogs.directory = Optional.of(destination);

        click("Escolher...");

        assertThat(form.destination().get()).isEqualTo(destination.toAbsolutePath().toString());

        dialogs.directory = Optional.empty();
        click("Escolher...");

        assertThat(form.destination().get()).isEqualTo(destination.toAbsolutePath().toString());
    }

    @Test
    void confirmedOverwriteReplacesTheExistingZip() throws IOException {
        generateZip();
        byte[] first = Files.readAllBytes(destination.resolve("pedido-api.zip"));
        dialogs.overwrite = true;
        form.description().set("Descricao nova");
        Fx.await(() -> !Fx.call(() -> button("Gerar projeto").isDisabled()));

        click("Gerar projeto");

        Fx.await(() -> labels().stream().anyMatch(text -> text.startsWith("Projeto gerado:")));
        assertThat(dialogs.confirmations).containsExactly("Já existe um arquivo com esse nome. Deseja substituir?");
        assertThat(Files.readAllBytes(destination.resolve("pedido-api.zip"))).isNotEqualTo(first);
    }

    @Test
    void declinedOverwriteKeepsTheExistingZipAndExplainsWhy() throws IOException {
        generateZip();
        byte[] first = Files.readAllBytes(destination.resolve("pedido-api.zip"));
        dialogs.overwrite = false;
        form.description().set("Descricao nova");
        Fx.await(() -> !Fx.call(() -> button("Gerar projeto").isDisabled()));

        click("Gerar projeto");

        Fx.await(() -> labels().contains("Já existe um arquivo com esse nome. Deseja substituir?"));
        assertThat(dialogs.confirmations).hasSize(1);
        assertThat(Files.readAllBytes(destination.resolve("pedido-api.zip"))).isEqualTo(first);
    }

    @Test
    void openFolderPointsAtTheZipDirectory() {
        generateZip();

        click("Abrir pasta");

        assertThat(dialogs.opened).containsExactly(destination);
    }

    @Test
    void openFolderPointsAtTheGeneratedProjectFolder() {
        goToExport();
        Fx.run(() -> radio("Pasta").setSelected(true));
        form.destination().set(destination.toString());
        Fx.await(() -> !Fx.call(() -> button("Gerar projeto").isDisabled()));
        click("Gerar projeto");
        Fx.await(() -> labels().stream().anyMatch(text -> text.startsWith("Projeto gerado:")));

        click("Abrir pasta");

        assertThat(dialogs.opened).containsExactly(destination.resolve("pedido-api").toAbsolutePath());
    }

    @Test
    void anUnexpectedFailureShowsAGenericMessageAndTheTechnicalDetail() {
        open(engine.catalog(), (request, target) -> {
            throw new IllegalStateException("kaboom");
        });
        goToExport();
        form.destination().set(destination.toString());
        Fx.await(() -> !Fx.call(() -> button("Gerar projeto").isDisabled()));

        click("Gerar projeto");

        Fx.await(() -> labels().contains("Não foi possível gerar o projeto."));
        assertThat(Fx.call(() -> nodes(".text-area").stream().map(TextArea.class::cast).filter(Node::isVisible)
                .map(TextArea::getText).findFirst().orElse(""))).contains("kaboom");
        assertThat(Fx.call(() -> button("Gerar projeto").isDisabled())).isFalse();
    }

    @Test
    void aFailedVersionSearchShowsAMessageAndTriesAgainOnTheNextVisit() {
        AtomicInteger calls = new AtomicInteger();
        open(failingVersions(calls), engine.generateProject());
        Fx.await(() -> calls.get() == 1);
        fillProject();

        click("Avançar");

        Fx.await(() -> labels().stream().anyMatch(text -> text.startsWith("Não foi possível buscar as versões")));
        assertThat(calls.get()).isEqualTo(2);

        click("Voltar");
        click("Avançar");

        Fx.await(() -> calls.get() == 3);
    }

    @Test
    void theNextLaunchStartsWithTheLastChoices() {
        form.groupId().set("com.acme");
        form.architectureStyle().set("HEXAGONAL_LAYERED");
        form.kit().set("NONE");
        form.fitnessFunctions().set(false);
        form.destination().set(destination.toString());
        preferences.remember(form);

        open(engine.catalog(), engine.generateProject());

        assertThat(Fx.call(() -> field("com.empresa").getText())).isEqualTo("com.acme");
        assertThat(Fx.call(() -> ((ArchitectureStyleOption) comboBox(0).getValue()).id())).isEqualTo("HEXAGONAL_LAYERED");
        assertThat(form.destination().get()).isEqualTo(destination.toString());
        assertThat(form.fitnessFunctions().get()).isFalse();
    }

    @Test
    void nextIsTheDefaultButtonSoEnterAdvances() {
        assertThat(Fx.call(() -> button("Avançar").isDefaultButton())).isTrue();
    }

    @Test
    void aLegacyBootLineGetsItsJavaListAndAWarningInTheSummary() {
        goToVersions();
        Fx.run(() -> {
            ComboBox<BootVersionOption> boot = comboBox(0);
            boot.getSelectionModel().select(boot.getItems().stream().filter(BootVersionOption::legacy).findFirst().orElseThrow());
        });

        assertThat(javaVersionsOffered()).contains("8");

        click("Avançar");
        Fx.await(() -> labels().contains("Dependências e fitness functions"));
        click("Avançar");

        Fx.await(() -> labels().stream().anyMatch(text -> text.contains("fora do suporte gratuito")));
    }

    @Test
    void generateIsDisabledWhileTheProjectIsBeingGenerated() throws InterruptedException {
        CountDownLatch release = new CountDownLatch(1);
        open(engine.catalog(), (request, target) -> {
            await(release);

            return engine.generateProject().generate(request, target);
        });
        goToExport();
        form.destination().set(destination.toString());
        Fx.await(() -> !Fx.call(() -> button("Gerar projeto").isDisabled()));

        click("Gerar projeto");

        assertThat(Fx.call(() -> button("Gerar projeto").isDisabled())).isTrue();
        assertThat(labels()).contains("Gerando o projeto...");

        release.countDown();

        Fx.await(() -> labels().stream().anyMatch(text -> text.startsWith("Projeto gerado:")));
        assertThat(Fx.call(() -> button("Gerar projeto").isDisabled())).isFalse();
    }

    private void selectInTree(String qualifiedName) {
        Fx.run(() -> {
            TreeView<PackageNode> tree = tree();
            find(tree.getRoot(), qualifiedName).ifPresentOrElse(
                    item -> tree.getSelectionModel().select(item), () -> { throw new AssertionError("node not found: " + qualifiedName); });
        });
    }

    private Optional<TreeItem<PackageNode>> find(TreeItem<PackageNode> from, String qualifiedName) {
        if (from.getValue() != null && from.getValue().qualifiedName().equals(qualifiedName)) {
            return Optional.of(from);
        }

        return from.getChildren().stream().map(child -> find(child, qualifiedName)).flatMap(Optional::stream).findFirst();
    }

    private List<String> treeNames() {
        return Fx.call(() -> {
            TreeView<PackageNode> tree = tree();
            List<String> names = new ArrayList<>();
            collect(tree.getRoot(), names);

            return names;
        });
    }

    @SuppressWarnings("unchecked")
    private TreeView<PackageNode> tree() {
        return (TreeView<PackageNode>) nodes(".tree-view").iterator().next();
    }

    private void collect(TreeItem<PackageNode> item, List<String> names) {
        if (item.getValue() != null) {
            names.add(item.getValue().name());
        }

        item.getChildren().forEach(child -> collect(child, names));
    }

    private List<String> catalogJavaVersionsFor(String boot) {
        return engine.catalog().javaVersions(boot).stream().map(JavaVersionOption::version).toList();
    }

    private List<String> javaVersionsOffered() {
        return Fx.call(() -> {
            ComboBox<JavaVersionOption> java = comboBox(1);

            return java.getItems().stream().map(JavaVersionOption::version).toList();
        });
    }

    private CatalogQuery failingVersions(AtomicInteger calls) {
        return (CatalogQuery) Proxy.newProxyInstance(getClass().getClassLoader(), new Class<?>[] {CatalogQuery.class},
                (proxy, method, args) -> {
                    if (method.getName().equals("bootVersions")) {
                        calls.incrementAndGet();
                        throw new IllegalStateException("sem rede");
                    }

                    try {
                        return method.invoke(engine.catalog(), args);
                    } catch (InvocationTargetException failure) {
                        throw failure.getCause();
                    }
                });
    }

    private static void await(CountDownLatch latch) {
        try {
            latch.await(15, TimeUnit.SECONDS);
        } catch (InterruptedException interrupted) {
            Thread.currentThread().interrupt();
        }
    }

    private void generateZip() {
        goToExport();
        form.destination().set(destination.toString());
        Fx.await(() -> !Fx.call(() -> button("Gerar projeto").isDisabled()));
        click("Gerar projeto");
        Fx.await(() -> labels().stream().anyMatch(text -> text.startsWith("Projeto gerado:")));
    }

    private static final class RecordingDialogs implements Dialogs {

        private final List<String> confirmations = new ArrayList<>();
        private final List<Path> opened = new ArrayList<>();
        private boolean overwrite;
        private Optional<Path> directory = Optional.empty();

        @Override
        public boolean confirmOverwrite(Window owner, String message) {
            confirmations.add(message);

            return overwrite;
        }

        @Override
        public Optional<Path> chooseDirectory(Window owner, Path initial) {
            return directory;
        }

        @Override
        public boolean confirmAction(Window owner, String message, String action) {
            return true;
        }

        @Override
        public void openFolder(Path folder) {
            opened.add(folder);
        }

        @Override
        public void openUrl(String url) {
        }
    }

    private void fillIdentity() {
        type("pedido-api", "pedido-api");
        type("com.empresa", "com.acme");
    }

    private void fillProject() {
        fillIdentity();
        type("pedidos, pagamentos", "pedidos, pagamentos");
    }

    private void goToVersions() {
        fillProject();
        click("Avançar");
        Fx.await(() -> labels().contains("Spring Boot e Java"));
        Fx.await(() -> !Fx.call(() -> comboBox(0).getItems().isEmpty()));
    }

    private void goToDependencies() {
        goToVersions();
        click("Avançar");
        Fx.await(() -> labels().contains("Dependências e fitness functions"));
    }

    private void goToExport() {
        goToDependencies();
        click("Avançar");
        Fx.await(() -> labels().contains("Exportar"));
    }

    private void click(String text) {
        Fx.run(() -> button(text).fire());
    }

    private void type(String prompt, String text) {
        Fx.run(() -> field(prompt).setText(text));
    }

    private List<String> labels() {
        return Fx.call(() -> nodes(".label").stream()
                .filter(Label.class::isInstance)
                .map(node -> ((Label) node).getText())
                .filter(text -> text != null && !text.isEmpty())
                .toList());
    }

    private List<String> paneTitles() {
        return Fx.call(() -> nodes(".titled-pane").stream()
                .filter(TitledPane.class::isInstance)
                .map(node -> ((TitledPane) node).getText())
                .toList());
    }

    private boolean hasVisibleButton(String text) {
        return nodes(".button").stream()
                .filter(Button.class::isInstance)
                .map(Button.class::cast)
                .anyMatch(button -> text.equals(button.getText()) && button.isVisible());
    }

    private void selectKit(String id) {
        Fx.run(() -> {
            ComboBox<KitOption> kits = comboBox(0);
            kits.getSelectionModel().select(kits.getItems().stream().filter(kit -> kit.id().equals(id)).findFirst().orElseThrow());
        });
    }

    private Set<Node> nodes(String selector) {
        view.applyCss();
        view.layout();

        return view.lookupAll(selector);
    }

    private Button button(String text) {
        return nodes(".button").stream()
                .filter(Button.class::isInstance)
                .map(Button.class::cast)
                .filter(button -> text.equals(button.getText()) && button.isVisible())
                .findFirst()
                .orElseThrow(() -> new AssertionError("button not found: " + text));
    }

    private TextField field(String prompt) {
        return nodes(".text-field").stream()
                .filter(TextField.class::isInstance)
                .map(TextField.class::cast)
                .filter(field -> prompt.equals(field.getPromptText()))
                .findFirst()
                .orElseThrow(() -> new AssertionError("field not found: " + prompt));
    }

    @SuppressWarnings("unchecked")
    private <T> ComboBox<T> comboBox(int index) {
        List<Node> boxes = nodes(".combo-box").stream().toList();

        return (ComboBox<T>) boxes.get(index);
    }

    private CheckBox checkBox(String text) {
        return nodes(".check-box").stream()
                .filter(CheckBox.class::isInstance)
                .map(CheckBox.class::cast)
                .filter(box -> text.equals(box.getText()))
                .findFirst()
                .orElseThrow(() -> new AssertionError("checkbox not found: " + text));
    }

    private RadioButton radio(String text) {
        return nodes(".radio-button").stream()
                .filter(RadioButton.class::isInstance)
                .map(RadioButton.class::cast)
                .filter(radio -> text.equals(radio.getText()))
                .findFirst()
                .orElseThrow(() -> new AssertionError("radio not found: " + text));
    }

    private Label label(String text) {
        return nodes(".label").stream()
                .filter(Label.class::isInstance)
                .map(Label.class::cast)
                .filter(label -> text.equals(label.getText()))
                .findFirst()
                .orElseThrow(() -> new AssertionError("label not found: " + text));
    }
}
