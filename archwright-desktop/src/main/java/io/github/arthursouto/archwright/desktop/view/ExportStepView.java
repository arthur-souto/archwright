package io.github.arthursouto.archwright.desktop.view;

import atlantafx.base.theme.Styles;
import io.github.arthursouto.archwright.desktop.i18n.Messages;
import io.github.arthursouto.archwright.desktop.wizard.FieldErrors;
import io.github.arthursouto.archwright.desktop.wizard.ProjectForm;
import io.github.arthursouto.archwright.desktop.wizard.WizardStep;
import io.github.arthursouto.archwright.engine.application.port.in.ExportFormat;
import io.github.arthursouto.archwright.engine.application.port.in.GenerateProjectUseCase;
import io.github.arthursouto.archwright.engine.application.port.in.GenerationResult;
import io.github.arthursouto.archwright.engine.application.port.in.ProjectRequest;
import io.github.arthursouto.archwright.engine.application.port.in.Violation;
import javafx.beans.binding.Bindings;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressIndicator;
import javafx.scene.control.RadioButton;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.control.ToggleGroup;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.stage.Window;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Supplier;

final class ExportStepView implements StepView {

    private static final double SPACING = 16;
    private static final double LOG_HEIGHT = 140;

    private final Messages messages;
    private final ProjectForm form;
    private final LiveValidation validation;
    private final GenerateProjectUseCase generator;
    private final Dialogs dialogs;
    private final Runnable onGenerated;
    private final Supplier<Window> window;
    private final TextField destination = new TextField();
    private final Button generate = new Button();
    private final ProgressIndicator working = new ProgressIndicator();
    private final Label outcome = new Label();
    private final Button openFolder = new Button();
    private final TextArea log = new TextArea();
    private final VBox root = new VBox(SPACING);

    ExportStepView(DesktopContext context, ProjectForm form, LiveValidation validation, Supplier<Window> window) {
        this.messages = context.messages();
        this.form = form;
        this.validation = validation;
        this.generator = context.generator();
        this.dialogs = context.dialogs();
        this.onGenerated = () -> context.preferences().remember(form);
        this.window = window;

        configureActions();
        root.getChildren().addAll(
                title(),
                formatRow(),
                new FieldRow(messages.text("export.destination"), destinationRow(), WizardStep.EXPORT, "destination",
                        validation, form.destination()),
                actionRow(),
                outcome,
                log,
                new PreviewSummaryView(messages, validation));
    }

    @Override
    public Node content() {
        return root;
    }

    @Override
    public void onShow() {
        validation.refresh();
    }

    private Label title() {
        Label title = new Label(messages.text("export.title"));
        title.getStyleClass().add(Styles.TITLE_3);

        return title;
    }

    private VBox formatRow() {
        ToggleGroup group = new ToggleGroup();
        RadioButton zip = formatButton(ExportFormat.ZIP, group);
        RadioButton folder = formatButton(ExportFormat.FOLDER, group);
        Label caption = new Label(messages.text("export.format"));
        caption.getStyleClass().add(Styles.TEXT_BOLD);

        group.selectToggle(form.format().get() == ExportFormat.ZIP ? zip : folder);
        group.selectedToggleProperty().addListener((observable, before, after) -> form.format().set((ExportFormat) after.getUserData()));

        return new VBox(4, caption, new HBox(SPACING, zip, folder));
    }

    private RadioButton formatButton(ExportFormat format, ToggleGroup group) {
        RadioButton button = new RadioButton(messages.text("export.format." + format.name()));
        button.setUserData(format);
        button.setToggleGroup(group);

        return button;
    }

    private HBox destinationRow() {
        Button choose = new Button(messages.text("export.destination.choose"));
        destination.textProperty().bindBidirectional(form.destination());
        HBox.setHgrow(destination, Priority.ALWAYS);
        choose.setOnAction(event -> chooseDestination());

        return new HBox(8, destination, choose);
    }

    private void chooseDestination() {
        Path current = form.hasDestination() ? Path.of(form.destination().get().strip()) : Path.of(System.getProperty("user.home"));

        dialogs.chooseDirectory(window.get(), current)
                .ifPresent(directory -> form.destination().set(directory.toAbsolutePath().toString()));
    }

    private HBox actionRow() {
        working.setPrefSize(20, 20);

        return new HBox(SPACING, generate, working, openFolder);
    }

    private void configureActions() {
        generate.setText(messages.text("nav.generate"));
        generate.getStyleClass().add(Styles.ACCENT);
        generate.disableProperty().bind(Bindings.createBooleanBinding(
                () -> !form.hasDestination() || validation.summary().get() == null || working.isVisible(),
                form.destination(), validation.summary(), working.visibleProperty()));
        generate.setOnAction(event -> generate(false));

        working.setVisible(false);
        working.managedProperty().bind(working.visibleProperty());
        openFolder.setText(messages.text("export.open"));
        hide(openFolder);
        outcome.setWrapText(true);
        log.setEditable(false);
        log.setPrefHeight(LOG_HEIGHT);
        hide(log);
    }

    private void generate(boolean overwrite) {
        ProjectRequest request = form.toRequest();
        working.setVisible(true);
        hide(openFolder);
        hide(log);
        outcome.setStyle("");
        outcome.setText(messages.text("export.working"));

        Background.run(() -> generator.generate(request, form.toTarget(overwrite)), this::finished, this::crashed);
    }

    private void finished(GenerationResult result) {
        working.setVisible(false);

        switch (result) {
            case GenerationResult.Success success -> succeeded(success);
            case GenerationResult.Failure failure -> rejected(failure.violations());
        }
    }

    private void succeeded(GenerationResult.Success success) {
        outcome.setStyle("-fx-text-fill: -color-success-fg;");
        outcome.setText(messages.text("export.done", Map.of(
                "value", String.valueOf(success.fileCount()), "bytes", readable(success.totalBytes()))) + "\n" + success.output());
        openFolder.setOnAction(event -> dialogs.openFolder(folderOf(success.output())));
        show(openFolder);
        onGenerated.run();
    }

    private Path folderOf(Path output) {
        return Files.isDirectory(output) ? output : output.toAbsolutePath().getParent();
    }

    private void rejected(List<Violation> violations) {
        outcome.setText("");
        Optional<Violation> existing = violations.stream().filter(violation -> violation.code().equals("TARGET_EXISTS")).findFirst();

        if (existing.isPresent() && confirmOverwrite(existing.get())) {
            generate(true);
            return;
        }

        validation.report(FieldErrors.from(violations, messages));
        validation.reveal(WizardStep.EXPORT);
    }

    private boolean confirmOverwrite(Violation violation) {
        return dialogs.confirmOverwrite(window.get(), messages.violation(violation));
    }

    private void crashed(Throwable failure) {
        working.setVisible(false);
        outcome.setStyle("-fx-text-fill: -color-danger-fg;");
        outcome.setText(messages.text("export.error"));
        log.setText(stackTrace(failure));
        show(log);
    }

    private String stackTrace(Throwable failure) {
        StringWriter writer = new StringWriter();
        failure.printStackTrace(new PrintWriter(writer));

        return writer.toString();
    }

    private String readable(long bytes) {
        if (bytes < 1024) {
            return bytes + " B";
        }

        return bytes < 1024 * 1024 ? "%.1f KB".formatted(bytes / 1024.0) : "%.1f MB".formatted(bytes / (1024.0 * 1024.0));
    }

    private void hide(Node node) {
        node.setVisible(false);
        node.setManaged(false);
    }

    private void show(Node node) {
        node.setVisible(true);
        node.setManaged(true);
    }
}
