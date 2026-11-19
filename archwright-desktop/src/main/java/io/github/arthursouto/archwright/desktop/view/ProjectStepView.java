package io.github.arthursouto.archwright.desktop.view;

import atlantafx.base.theme.Styles;
import io.github.arthursouto.archwright.desktop.i18n.Messages;
import io.github.arthursouto.archwright.desktop.wizard.ProjectForm;
import io.github.arthursouto.archwright.desktop.wizard.WizardStep;
import io.github.arthursouto.archwright.engine.application.port.in.ArchitectureStyleOption;
import io.github.arthursouto.archwright.engine.application.port.in.CatalogQuery;
import javafx.beans.property.Property;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import javafx.collections.FXCollections;
import javafx.scene.Node;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.layout.VBox;

import java.util.List;

final class ProjectStepView implements StepView {

    private static final double SPACING = 16;

    private final CatalogQuery catalog;
    private final Messages messages;
    private final ProjectForm form;
    private final LiveValidation validation;
    private final ComboBox<ArchitectureStyleOption> styles = new ComboBox<>();
    private final TextField basePackage = new TextField();
    private final StringProperty modulesCaption = new SimpleStringProperty();
    private final FieldRow modulesRow;
    private final VBox root = new VBox(SPACING);

    ProjectStepView(DesktopContext context, ProjectForm form, LiveValidation validation) {
        this.catalog = context.catalog();
        this.messages = context.messages();
        this.form = form;
        this.validation = validation;

        configureStyles();
        modulesRow = modulesRow();
        root.getChildren().addAll(title(), identityRows(), styleRow(), modulesRow);
        keepBasePackageHintUpdated();
        relabelAndClearModulesOnStyleChange();
    }

    @Override
    public Node content() {
        return root;
    }

    private Label title() {
        Label title = new Label(messages.text("project.title"));
        title.getStyleClass().add(Styles.TITLE_3);

        return title;
    }

    private VBox identityRows() {
        TextField artifactId = field(form.artifactId(), "project.artifactId.prompt");
        TextField groupId = field(form.groupId(), "project.groupId.prompt");
        TextField description = field(form.description(), "project.description.prompt");
        basePackage.textProperty().bindBidirectional(form.basePackage());

        return new VBox(SPACING,
                row("project.artifactId", artifactId, "artifactId", form.artifactId()),
                row("project.groupId", groupId, "groupId", form.groupId()),
                row("project.basePackage", basePackage, "basePackage", form.basePackage()),
                row("project.description", description, "description", form.description()));
    }

    private FieldRow styleRow() {
        return new FieldRow(messages.text("project.style"), messages.text("project.style.help"), styles,
                WizardStep.PROJECT, "architectureStyle",
                validation, form.architectureStyle());
    }

    private FieldRow modulesRow() {
        TextField modules = field(form.modules(), "project.modules.prompt");
        modulesCaption.set(modulesCaption());

        return new FieldRow(modulesCaption, messages.text("project.modules.help"), modules, WizardStep.PROJECT, "modules", validation, form.modules());
    }

    private FieldRow row(String captionKey, Node control, String field, Property<?> value) {
        return new FieldRow(messages.text(captionKey), messages.text(captionKey + ".help"), control, WizardStep.PROJECT,
                field, validation, value);
    }

    private TextField field(StringProperty property, String promptKey) {
        TextField field = new TextField();
        field.textProperty().bindBidirectional(property);
        field.setPromptText(messages.text(promptKey));

        return field;
    }

    private void configureStyles() {
        List<ArchitectureStyleOption> options = catalog.architectureStyles();
        styles.setItems(FXCollections.observableList(options));
        styles.setMaxWidth(Double.MAX_VALUE);
        styles.setCellFactory(list -> Cells.of(this::styleText));
        styles.setButtonCell(Cells.of(this::styleText));
        styles.getSelectionModel().select(initialStyle(options));
        form.architectureStyle().set(styles.getValue().id());
        styles.valueProperty().addListener((observable, before, after) -> form.architectureStyle().set(after.id()));
    }

    private ArchitectureStyleOption initialStyle(List<ArchitectureStyleOption> options) {
        return options.stream()
                .filter(option -> option.id().equals(form.architectureStyle().get()))
                .findFirst()
                .orElse(options.getFirst());
    }

    private String styleText(ArchitectureStyleOption option) {
        return option.label() + " — " + option.description();
    }

    private void keepBasePackageHintUpdated() {
        Runnable update = () -> basePackage.setPromptText(basePackageHint());

        form.groupId().addListener(observable -> update.run());
        form.artifactId().addListener(observable -> update.run());
        update.run();
    }

    private String basePackageHint() {
        String suggestion = catalog.suggestBasePackage(form.groupId().get(), form.artifactId().get());

        return suggestion.isBlank()
                ? messages.text("project.basePackage.prompt")
                : messages.text("project.basePackage.hint", "value", suggestion);
    }

    private String modulesCaption() {
        ArchitectureStyleOption style = styles.getValue();
        String label = style == null || style.modulesLabel() == null || style.modulesLabel().isBlank()
                ? messages.text("project.modules")
                : style.modulesLabel();

        return messages.text("project.modules.caption", "value", label);
    }

    private void relabelAndClearModulesOnStyleChange() {
        styles.valueProperty().addListener((observable, before, after) -> {
            modulesCaption.set(modulesCaption());
            form.modules().set("");
        });
    }
}
