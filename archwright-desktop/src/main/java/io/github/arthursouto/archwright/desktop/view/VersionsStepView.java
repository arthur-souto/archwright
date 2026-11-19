package io.github.arthursouto.archwright.desktop.view;

import atlantafx.base.theme.Styles;
import io.github.arthursouto.archwright.desktop.i18n.Messages;
import io.github.arthursouto.archwright.desktop.wizard.ProjectForm;
import io.github.arthursouto.archwright.desktop.wizard.WizardStep;
import io.github.arthursouto.archwright.engine.application.port.in.BootVersionOption;
import io.github.arthursouto.archwright.engine.application.port.in.BootVersions;
import io.github.arthursouto.archwright.engine.application.port.in.CatalogQuery;
import io.github.arthursouto.archwright.engine.application.port.in.JavaVersionOption;
import javafx.collections.FXCollections;
import javafx.scene.Node;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressIndicator;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;

import java.util.ArrayList;
import java.util.List;

final class VersionsStepView implements StepView {

    private static final double SPACING = 16;

    private final CatalogQuery catalog;
    private final Messages messages;
    private final ProjectForm form;
    private final ComboBox<BootVersionOption> boot = new ComboBox<>();
    private final ComboBox<JavaVersionOption> java = new ComboBox<>();
    private final Label origin = new Label();
    private final ProgressIndicator loading = new ProgressIndicator();
    private final VBox root = new VBox(SPACING);

    private boolean loaded;

    VersionsStepView(DesktopContext context, ProjectForm form, LiveValidation validation) {
        this.catalog = context.catalog();
        this.messages = context.messages();
        this.form = form;

        configureBoot();
        configureJava();
        root.getChildren().addAll(
                title(),
                new FieldRow(messages.text("versions.boot"), boot, WizardStep.VERSIONS, "bootVersion", validation, form.bootVersion()),
                new FieldRow(messages.text("versions.java"), java, WizardStep.VERSIONS, "javaVersion", validation, form.javaVersion()),
                statusLine());
    }

    @Override
    public Node content() {
        return root;
    }

    @Override
    public void onShow() {
        if (!loaded) {
            loaded = true;
            load();
        }
    }

    private Label title() {
        Label title = new Label(messages.text("versions.title"));
        title.getStyleClass().add(Styles.TITLE_3);

        return title;
    }

    private HBox statusLine() {
        origin.getStyleClass().addAll(Styles.TEXT_MUTED, Styles.TEXT_SMALL);
        loading.setPrefSize(18, 18);
        loading.managedProperty().bind(loading.visibleProperty());

        return new HBox(8, loading, origin);
    }

    private void load() {
        loading.setVisible(true);
        origin.setText(messages.text("versions.loading"));

        Background.run(catalog::bootVersions, this::show, failure -> {
            loaded = false;
            loading.setVisible(false);
            origin.setText(messages.text("versions.failed"));
        });
    }

    private void show(BootVersions versions) {
        loading.setVisible(false);
        origin.setText(messages.text("versions.origin." + versions.origin().name()));
        boot.setItems(FXCollections.observableList(versions.versions()));
        boot.getSelectionModel().select(initialBoot(versions.versions()));
    }

    private BootVersionOption initialBoot(List<BootVersionOption> options) {
        return options.stream()
                .filter(option -> option.version().equals(form.bootVersion().get()))
                .findFirst()
                .or(() -> options.stream().filter(option -> !option.legacy() && option.latestOfLine()).findFirst())
                .orElse(options.getFirst());
    }

    private void configureBoot() {
        boot.setMaxWidth(Double.MAX_VALUE);
        boot.setCellFactory(list -> Cells.of(this::bootText));
        boot.setButtonCell(Cells.of(this::bootText));
        boot.valueProperty().addListener((observable, before, after) -> bootChanged(after));
    }

    private void bootChanged(BootVersionOption selected) {
        if (selected == null) {
            return;
        }

        form.bootVersion().set(selected.version());
        List<JavaVersionOption> options = catalog.javaVersions(selected.version());
        java.setItems(FXCollections.observableList(new ArrayList<>(options)));
        options.stream()
                .filter(option -> option.version().equals(form.javaVersion().get()))
                .findFirst()
                .or(() -> options.stream().filter(JavaVersionOption::isDefault).findFirst())
                .ifPresent(option -> java.getSelectionModel().select(option));
    }

    private void configureJava() {
        java.setMaxWidth(Double.MAX_VALUE);
        java.setCellFactory(list -> Cells.of(this::javaText));
        java.setButtonCell(Cells.of(this::javaText));
        java.valueProperty().addListener((observable, before, after) -> {
            if (after != null) {
                form.javaVersion().set(after.version());
            }
        });
    }

    private String bootText(BootVersionOption option) {
        return option.legacy() ? option.version() + " — " + messages.text("versions.legacy") : option.version();
    }

    private String javaText(JavaVersionOption option) {
        List<String> tags = new ArrayList<>();

        if (option.lts()) {
            tags.add(messages.text("versions.java.lts"));
        }

        if (option.aboveTested()) {
            tags.add(messages.text("versions.java.untested"));
        }

        if (option.isDefault()) {
            tags.add(messages.text("versions.java.default"));
        }

        return tags.isEmpty() ? option.version() : option.version() + " — " + String.join(", ", tags);
    }
}
