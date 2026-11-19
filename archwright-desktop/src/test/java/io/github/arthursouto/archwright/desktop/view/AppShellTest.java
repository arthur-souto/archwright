package io.github.arthursouto.archwright.desktop.view;

import io.github.arthursouto.archwright.desktop.history.ProjectHistory;
import io.github.arthursouto.archwright.desktop.i18n.Messages;
import io.github.arthursouto.archwright.desktop.preferences.UserPreferences;
import io.github.arthursouto.archwright.engine.application.port.in.ExportFormat;
import io.github.arthursouto.archwright.engine.application.port.in.ProjectRequest;
import io.github.arthursouto.archwright.engine.config.ArchwrightEngine;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.stage.Window;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;
import java.time.Clock;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.prefs.BackingStoreException;
import java.util.prefs.Preferences;

import static org.assertj.core.api.Assertions.assertThat;

class AppShellTest {

    @TempDir
    Path workspace;

    private Preferences node;
    private Preferences historyNode;
    private ProjectHistory history;
    private UserPreferences preferences;
    private final List<String> urls = new ArrayList<>();
    private final List<Path> folders = new ArrayList<>();
    private boolean confirmClear = true;
    private AppShell shell;

    @BeforeEach
    void openTheShell() {
        Assumptions.assumeTrue(Fx.available(), "JavaFX toolkit not available (headless environment)");

        node = Preferences.userRoot().node("archwright-shell-" + UUID.randomUUID());
        historyNode = Preferences.userRoot().node("archwright-shell-history-" + UUID.randomUUID());
        preferences = new UserPreferences(node);
        history = new ProjectHistory(historyNode, Clock.systemDefaultZone());
        ArchwrightEngine engine = ArchwrightEngine.builder().cacheDirectory(workspace.resolve("cache")).offline(true).build();
        DesktopContext context = new DesktopContext(engine.catalog(), engine.previewProject(), engine.generateProject(),
                Messages.portuguese(), preferences, history, new Dialogs() {
                    @Override
                    public boolean confirmOverwrite(Window owner, String message) {
                        return true;
                    }

                    @Override
                    public Optional<Path> chooseDirectory(Window owner, Path initial) {
                        return Optional.empty();
                    }

                    @Override
                    public boolean confirmAction(Window owner, String message, String action) {
                        return confirmClear;
                    }

                    @Override
                    public void openFolder(Path folder) {
                        folders.add(folder);
                    }

                    @Override
                    public void openUrl(String url) {
                        urls.add(url);
                    }
                });

        shell = Fx.call(() -> {
            AppShell created = new AppShell(context);
            new Scene(created, 1480, 840);

            return created;
        });
    }

    @AfterEach
    void cleanUp() throws BackingStoreException {
        if (node != null) {
            node.removeNode();
        }

        if (historyNode != null) {
            historyNode.removeNode();
        }
    }

    private void generated(String artifact) {
        history.record(new ProjectRequest(artifact, "com.acme", "", "", "HEXAGONAL_LAYERED", List.of("produto"), "4.1.2", "25", "BASIC",
                List.of("web"), List.of(), true), ExportFormat.FOLDER, workspace.toString(), workspace.toString(), 48);
    }

    private List<String> labels() {
        return Fx.call(() -> {
            shell.applyCss();
            shell.layout();

            return shell.lookupAll(".label").stream()
                    .filter(Label.class::isInstance)
                    .map(node -> ((Label) node).getText())
                    .filter(text -> text != null && !text.isEmpty())
                    .toList();
        });
    }

    private Button button(String text) {
        shell.applyCss();
        shell.layout();

        return shell.lookupAll(".button").stream()
                .filter(Button.class::isInstance)
                .map(Button.class::cast)
                .filter(button -> text.equals(button.getText()) && button.isVisible())
                .findFirst()
                .orElseThrow(() -> new AssertionError("button not found: " + text));
    }

    private void click(String text) {
        Fx.run(() -> button(text).fire());
    }

    private boolean hasButton(String text) {
        return Fx.call(() -> {
            shell.applyCss();
            shell.layout();

            return shell.lookupAll(".button").stream()
                .anyMatch(node -> node instanceof Button button && text.equals(button.getText()) && button.isVisible());
        });
    }

    private void type(String prompt, String text) {
        Fx.run(() -> shell.lookupAll(".text-field").stream()
                .filter(node -> node instanceof TextField field && prompt.equals(field.getPromptText()))
                .map(TextField.class::cast)
                .findFirst()
                .orElseThrow()
                .setText(text));
    }

    private boolean field(String prompt) {
        return Fx.call(() -> {
            shell.applyCss();
            shell.layout();

            return shell.lookupAll(".text-field").stream()
                .anyMatch(node -> node instanceof TextField field && prompt.equals(field.getPromptText()));
        });
    }

    @Test
    void opensOnTheHomeScreenWithTheLogoAndTheMenu() {
        assertThat(labels()).contains("Bem-vindo ao Archwright", "Início rápido", "Projetos recentes", "Bom saber");
        assertThat(hasButton("Início")).isTrue();
        assertThat(hasButton("Novo projeto")).isTrue();
        assertThat(hasButton("Histórico")).isTrue();
        assertThat(hasButton("Sobre")).isTrue();
        assertThat(Fx.call(() -> shell.lookupAll(".sidebar").size())).isEqualTo(1);
    }

    @Test
    void theHomeShowsAnEmptyStateAndZeroProjects() {
        assertThat(labels()).contains("Você ainda não gerou nenhum projeto. Os próximos aparecem aqui.", "0");
    }

    @Test
    void theHomeShowsTheStatsTheRecentProjectsAndThePresets() {
        generated("pedido-api");
        generated("loja-catalogo");
        Fx.run(() -> button("Início").fire());

        assertThat(labels()).contains("2", "loja-catalogo", "pedido-api", "Monólito modular", "API em camadas", "API mínima, sem banco");
        assertThat(hasButton("Reutilizar")).isTrue();
        assertThat(hasButton("Ver todos")).isTrue();
    }

    @Test
    void newProjectOpensTheWizardOnTheFirstStep() {
        click("Novo projeto");

        Fx.await(() -> labels().contains("Dados do projeto"));

        assertThat(field("pedido-api")).isTrue();
    }

    @Test
    void aPresetFillsTheWizardWithItsKitAndDependencies() {
        click("Usar este modelo");
        Fx.await(() -> labels().contains("Dados do projeto"));
        type("pedido-api", "pedido-api");
        type("com.empresa", "com.acme");
        type("pedidos, pagamentos", "pedidos");
        Fx.await(() -> Fx.call(() -> !button("Avançar").isDisabled()));
        click("Avançar");
        Fx.await(() -> labels().contains("Spring Boot e Java"));
        Fx.await(() -> Fx.call(() -> !button("Avançar").isDisabled()));
        click("Avançar");
        Fx.await(() -> labels().contains("Dependências e fitness functions"));

        assertThat(labels()).anyMatch(text -> text.startsWith("Marca:") && text.contains("Web"));
    }

    @Test
    void reusingAProjectFromTheHistoryFillsTheNameAndTheStyle() {
        generated("loja-catalogo");
        Fx.run(() -> button("Início").fire());

        click("Reutilizar");
        Fx.await(() -> labels().contains("Dados do projeto"));

        assertThat(Fx.call(() -> shell.lookupAll(".text-field").stream()
                .filter(TextField.class::isInstance)
                .map(node -> ((TextField) node).getText())
                .toList())).contains("loja-catalogo", "com.acme");
    }

    @Test
    void theHistoryListsEveryProjectAndCanOpenItsFolder() {
        generated("um");
        generated("dois");

        click("Histórico");

        assertThat(labels()).contains("Histórico", "um", "dois");

        click("Abrir pasta");

        assertThat(folders).hasSize(1);
    }

    @Test
    void theHistoryHasAnEmptyState() {
        click("Histórico");

        assertThat(labels()).contains("Você ainda não gerou nenhum projeto. Quando gerar, ele aparece aqui.");
    }

    @Test
    void removingOneEntryKeepsTheOthers() {
        generated("fica");
        generated("sai");
        click("Histórico");

        click("Remover");

        assertThat(history.entries()).hasSize(1);
    }

    @Test
    void clearingAsksForConfirmationAndEmptiesTheList() {
        generated("um");
        click("Histórico");

        confirmClear = false;
        click("Limpar histórico");
        assertThat(history.entries()).hasSize(1);

        confirmClear = true;
        click("Limpar histórico");
        assertThat(history.entries()).isEmpty();
    }

    @Test
    void theAboutScreenOpensTheLinks() {
        click("Sobre");

        assertThat(labels()).contains("Licença Apache 2.0.");

        click("Guia de instalação");
        click("Repositório");
        click("Versões");

        assertThat(urls).containsExactly(Links.GUIDE, Links.REPOSITORY, Links.RELEASES);
    }

    @Test
    void theThemeToggleLivesInTheMenuAndIsRemembered() {
        assertThat(hasButton("Tema escuro")).isTrue();

        click("Tema escuro");

        assertThat(preferences.darkTheme()).isTrue();
        assertThat(hasButton("Tema claro")).isTrue();
        assertThat(Fx.call(() -> shell.getStyleClass().contains("dark"))).isTrue();

        click("Tema claro");

        assertThat(preferences.darkTheme()).isFalse();
        assertThat(Fx.call(() -> shell.getStyleClass().contains("dark"))).isFalse();
    }

    @Test
    void finishingAProjectStartsAFreshWizardNextTime() {
        click("Novo projeto");
        Fx.await(() -> labels().contains("Dados do projeto"));

        assertThat(Fx.call(() -> (Node) shell.getCenter())).isInstanceOf(WizardView.class);

        click("Início");
        click("Novo projeto");

        assertThat(Fx.call(() -> (Node) shell.getCenter())).isInstanceOf(WizardView.class);
    }
}
