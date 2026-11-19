package io.github.arthursouto.archwright.desktop.view;

import io.github.arthursouto.archwright.desktop.i18n.Messages;
import io.github.arthursouto.archwright.desktop.wizard.FieldErrors;
import io.github.arthursouto.archwright.desktop.wizard.ProjectForm;
import io.github.arthursouto.archwright.desktop.wizard.WizardStep;
import io.github.arthursouto.archwright.engine.application.port.in.PreviewProjectQuery;
import io.github.arthursouto.archwright.engine.application.port.in.PreviewResult;
import io.github.arthursouto.archwright.engine.application.port.in.ProjectRequest;
import javafx.animation.PauseTransition;
import javafx.beans.binding.Bindings;
import javafx.beans.binding.BooleanBinding;
import javafx.beans.property.IntegerProperty;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableSet;
import javafx.util.Duration;

public final class LiveValidation {

    private static final Duration TYPING_DELAY = Duration.millis(350);

    private final PreviewProjectQuery preview;
    private final Messages messages;
    private final ProjectForm form;
    private final ObjectProperty<FieldErrors> errors = new SimpleObjectProperty<>(FieldErrors.none());
    private final IntegerProperty completed = new SimpleIntegerProperty();
    private final ObjectProperty<PreviewResult.Preview> summary = new SimpleObjectProperty<>();
    private final ObservableSet<WizardStep> revealed = FXCollections.observableSet();
    private final PauseTransition debounce = new PauseTransition(TYPING_DELAY);

    private int ticket;

    public LiveValidation(PreviewProjectQuery preview, Messages messages, ProjectForm form) {
        this.preview = preview;
        this.messages = messages;
        this.form = form;

        debounce.setOnFinished(event -> refresh());
        form.onChange(debounce::playFromStart);
    }

    public ObjectProperty<FieldErrors> errors() {
        return errors;
    }

    public IntegerProperty completed() {
        return completed;
    }

    public ObjectProperty<PreviewResult.Preview> summary() {
        return summary;
    }

    public BooleanBinding revealed(WizardStep step) {
        return Bindings.createBooleanBinding(() -> revealed.contains(step), revealed);
    }

    public void reveal(WizardStep step) {
        revealed.add(step);
    }

    public boolean blocks(WizardStep step) {
        return errors.get().hasErrorIn(step);
    }

    public void refresh() {
        refresh(() -> { });
    }

    public void refresh(Runnable whenDone) {
        int current = ++ticket;
        ProjectRequest request = form.toRequest();

        Background.run(() -> preview.preview(request), result -> {
            if (current == ticket) {
                apply(result);
                completed.set(completed.get() + 1);
            }

            whenDone.run();
        });
    }

    public void report(FieldErrors reported) {
        ticket++;
        debounce.stop();
        errors.set(reported);
        completed.set(completed.get() + 1);
    }

    private void apply(PreviewResult result) {
        switch (result) {
            case PreviewResult.Preview ready -> {
                errors.set(FieldErrors.none());
                summary.set(ready);
            }
            case PreviewResult.Failure failure -> {
                errors.set(FieldErrors.from(failure.violations(), messages));
                summary.set(null);
            }
        }
    }
}
