package io.github.arthursouto.archwright.desktop.view;

import atlantafx.base.theme.Styles;
import io.github.arthursouto.archwright.desktop.i18n.Messages;
import io.github.arthursouto.archwright.engine.application.port.in.FitnessRuleSummary;
import io.github.arthursouto.archwright.engine.application.port.in.PreviewResult;
import io.github.arthursouto.archwright.engine.application.port.in.ResolvedDependency;
import javafx.collections.FXCollections;
import javafx.scene.Node;
import javafx.scene.control.Label;
import javafx.scene.control.ListView;
import javafx.scene.control.TitledPane;
import javafx.scene.layout.VBox;

import java.util.List;
import java.util.function.Function;

final class PreviewSummaryView extends VBox {

    private static final double SPACING = 8;
    private static final double FILES_HEIGHT = 240;
    private static final double CELL_HEIGHT = 26;

    private final Messages messages;
    private final LiveValidation validation;

    PreviewSummaryView(Messages messages, LiveValidation validation) {
        super(SPACING);
        this.messages = messages;
        this.validation = validation;

        validation.summary().addListener(observable -> render());
        validation.errors().addListener(observable -> render());
        render();
    }

    private void render() {
        PreviewResult.Preview preview = validation.summary().get();

        if (preview == null) {
            getChildren().setAll(placeholder());
            return;
        }

        getChildren().clear();
        preview.warnings().forEach(warning -> getChildren().add(warningLine(messages.warning(warning))));
        getChildren().add(pane(messages.text("export.preview.files", "value", String.valueOf(preview.paths().size())), files(preview.paths())));
        addPane("export.preview.dependencies", preview.dependencies(), this::dependencyText);
        addPane("export.preview.rules", preview.fitnessRules(), this::ruleText);
    }

    private Label placeholder() {
        boolean invalid = !validation.errors().get().isEmpty();
        Label label = new Label(messages.text(invalid ? "export.preview.invalid" : "export.preview.loading"));
        label.setWrapText(true);
        label.getStyleClass().add(invalid ? Styles.DANGER : Styles.TEXT_MUTED);

        return label;
    }

    private Label warningLine(String text) {
        Label line = new Label(text);
        line.setWrapText(true);
        line.setStyle("-fx-text-fill: -color-warning-fg;");

        return line;
    }

    private Node files(List<String> paths) {
        ListView<String> list = new ListView<>(FXCollections.observableList(paths));
        list.setPrefHeight(FILES_HEIGHT);
        list.setFixedCellSize(CELL_HEIGHT);

        return list;
    }

    private <T> void addPane(String titleKey, List<T> items, Function<T, String> text) {
        if (items.isEmpty()) {
            return;
        }

        VBox lines = new VBox(4);
        items.forEach(item -> lines.getChildren().add(line(text.apply(item))));
        getChildren().add(pane(messages.text(titleKey), lines));
    }

    private TitledPane pane(String title, Node content) {
        TitledPane pane = new TitledPane(title, content);
        pane.setExpanded(false);

        return pane;
    }

    private Label line(String text) {
        Label line = new Label(text);
        line.setWrapText(true);
        line.getStyleClass().add(Styles.TEXT_SMALL);

        return line;
    }

    private String dependencyText(ResolvedDependency dependency) {
        String version = dependency.version() == null || dependency.version().isBlank() ? "" : ":" + dependency.version();

        return dependency.groupId() + ":" + dependency.artifactId() + version + " (" + dependency.scope() + ")";
    }

    private String ruleText(FitnessRuleSummary rule) {
        return rule.id() + " — " + rule.description();
    }
}
