package io.github.arthursouto.archwright.desktop.view;

import atlantafx.base.theme.Styles;
import io.github.arthursouto.archwright.desktop.wizard.WizardStep;
import javafx.beans.binding.Bindings;
import javafx.beans.binding.BooleanBinding;
import javafx.beans.binding.StringBinding;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.Property;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.value.ObservableValue;
import javafx.scene.Node;
import javafx.scene.control.Label;
import javafx.scene.layout.VBox;

final class FieldRow extends VBox {

    private static final double SPACING = 4;

    private final BooleanProperty touched = new SimpleBooleanProperty(false);
    private final BooleanProperty awaitingResult = new SimpleBooleanProperty(false);

    FieldRow(String caption, Node control, WizardStep step, String field, LiveValidation validation, Property<?> value) {
        this(new SimpleStringProperty(caption), control, step, field, validation, value);
    }

    FieldRow(ObservableValue<String> caption, Node control, WizardStep step, String field, LiveValidation validation, Property<?> value) {
        super(SPACING);

        Label title = new Label();
        title.textProperty().bind(caption);
        Label error = new Label();
        title.getStyleClass().add(Styles.TEXT_BOLD);
        error.getStyleClass().addAll(Styles.DANGER, Styles.TEXT_SMALL);
        error.setWrapText(true);

        BooleanBinding shown = touched.or(validation.revealed(step)).and(awaitingResult.not());
        StringBinding message = Bindings.createStringBinding(
                () -> shown.get() ? validation.errors().get().messageFor(field) : "",
                validation.errors(), shown);

        error.textProperty().bind(message);
        error.visibleProperty().bind(message.isNotEmpty());
        error.managedProperty().bind(error.visibleProperty());
        message.addListener((observable, before, after) -> control.pseudoClassStateChanged(Styles.STATE_DANGER, !after.isEmpty()));
        value.addListener(observable -> {
            touched.set(true);
            awaitingResult.set(true);
        });
        validation.completed().addListener(observable -> awaitingResult.set(false));

        getChildren().addAll(title, control, error);
    }
}
