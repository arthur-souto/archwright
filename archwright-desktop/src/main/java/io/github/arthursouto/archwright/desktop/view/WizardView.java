package io.github.arthursouto.archwright.desktop.view;

import atlantafx.base.theme.Styles;
import io.github.arthursouto.archwright.desktop.i18n.Messages;
import io.github.arthursouto.archwright.desktop.wizard.ProjectForm;
import io.github.arthursouto.archwright.desktop.wizard.WizardStep;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.stage.Window;

import java.util.EnumMap;
import java.util.Map;

public final class WizardView extends BorderPane {

    private static final double PADDING = 24;
    private static final double SPACING = 16;

    private final DesktopContext context;
    private final Messages messages;
    private final LiveValidation validation;
    private final PackageTreePanel packageTree;
    private final Map<WizardStep, StepView> views = new EnumMap<>(WizardStep.class);
    private final Map<WizardStep, Label> indicators = new EnumMap<>(WizardStep.class);
    private final ObjectProperty<WizardStep> current = new SimpleObjectProperty<>(WizardStep.PROJECT);
    private final Button back = new Button();
    private final Button next = new Button();

    public WizardView(DesktopContext context, ProjectForm form) {
        this(context, form, () -> {
        });
    }

    public WizardView(DesktopContext context, ProjectForm form, Runnable afterGenerated) {
        this.context = context;
        this.messages = context.messages();
        this.validation = new LiveValidation(context.preview(), messages, form);
        this.packageTree = new PackageTreePanel(messages, new LivePackageTree(context.preview(), form));

        views.put(WizardStep.PROJECT, new ProjectStepView(context, form, validation));
        views.put(WizardStep.VERSIONS, new VersionsStepView(context, form, validation));
        views.put(WizardStep.DEPENDENCIES, new DependenciesStepView(context, form, validation));
        views.put(WizardStep.EXPORT, new ExportStepView(context, form, validation, this::window, afterGenerated));

        setTop(header());
        setBottom(footer());
        keepIndicatorsAndButtonsUpToDate();
        show(WizardStep.PROJECT);
        VersionPreload.start(context.catalog(), form);
        validation.refresh();
    }

    private Window window() {
        return getScene().getWindow();
    }

    private HBox header() {
        Label title = new Label(messages.text("section.WIZARD"));
        title.getStyleClass().add(Styles.TITLE_3);
        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        HBox steps = new HBox(SPACING);
        steps.setAlignment(Pos.CENTER_LEFT);

        for (WizardStep step : WizardStep.values()) {
            Label indicator = new Label((step.ordinal() + 1) + ". " + messages.text("step." + step.name()));
            indicator.setOnMouseClicked(event -> goBackTo(step));
            indicators.put(step, indicator);
            steps.getChildren().add(indicator);
        }

        HBox header = new HBox(SPACING * 2, title, steps, spacer);
        header.setAlignment(Pos.CENTER_LEFT);
        header.setPadding(new Insets(PADDING, PADDING, SPACING, PADDING));

        return header;
    }

    private HBox footer() {
        back.setText(messages.text("nav.back"));
        next.setText(messages.text("nav.next"));
        next.getStyleClass().add(Styles.ACCENT);
        next.setDefaultButton(true);
        back.setOnAction(event -> show(current.get().previous()));
        next.setOnAction(event -> advance());

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);
        HBox footer = new HBox(SPACING, back, spacer, next);
        footer.setPadding(new Insets(SPACING, PADDING, PADDING, PADDING));

        return footer;
    }

    private void goBackTo(WizardStep step) {
        if (step.ordinal() < current.get().ordinal()) {
            show(step);
        }
    }

    private void advance() {
        WizardStep step = current.get();

        validation.refresh(() -> {
            if (validation.blocks(step)) {
                validation.reveal(step);
            } else {
                show(step.next());
            }
        });
    }

    private void show(WizardStep step) {
        StepView view = views.get(step);
        ScrollPane scroll = new ScrollPane(view.content());
        scroll.setFitToWidth(true);
        scroll.setPadding(new Insets(0, PADDING, 0, PADDING));
        scroll.getStyleClass().add(Styles.FLAT);

        HBox.setHgrow(scroll, Priority.ALWAYS);
        setCenter(new HBox(SPACING, scroll, packageTree));
        current.set(step);
        view.onShow();
    }

    private void keepIndicatorsAndButtonsUpToDate() {
        current.addListener(observable -> refreshChrome());
        validation.errors().addListener(observable -> refreshChrome());

        for (WizardStep step : WizardStep.values()) {
            validation.revealed(step).addListener(observable -> refreshChrome());
        }

        refreshChrome();
    }

    private void refreshChrome() {
        WizardStep step = current.get();
        back.setDisable(step.isFirst());
        next.setVisible(!step.isLast());
        next.setManaged(next.isVisible());

        indicators.forEach((indicatorStep, label) -> {
            boolean failing = validation.revealed(indicatorStep).get() && validation.errors().get().hasErrorIn(indicatorStep);
            label.getStyleClass().removeAll(Styles.ACCENT, Styles.DANGER, Styles.TEXT_BOLD, Styles.TEXT_MUTED);
            label.getStyleClass().add(failing ? Styles.DANGER : indicatorStep == step ? Styles.ACCENT : Styles.TEXT_MUTED);

            if (indicatorStep == step) {
                label.getStyleClass().add(Styles.TEXT_BOLD);
            }
        });
    }
}
