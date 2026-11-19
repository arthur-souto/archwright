package io.github.arthursouto.archwright.desktop.view;

import io.github.arthursouto.archwright.desktop.wizard.PackageNode;
import io.github.arthursouto.archwright.desktop.wizard.PackageTrees;
import io.github.arthursouto.archwright.desktop.wizard.ProjectForm;
import io.github.arthursouto.archwright.desktop.wizard.ProvisionalRequest;
import io.github.arthursouto.archwright.engine.application.port.in.PreviewFile;
import io.github.arthursouto.archwright.engine.application.port.in.PreviewProjectQuery;
import io.github.arthursouto.archwright.engine.application.port.in.PreviewResult;
import io.github.arthursouto.archwright.engine.application.port.in.ProjectRequest;
import javafx.animation.PauseTransition;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.util.Duration;

import java.util.List;
import java.util.Optional;

final class LivePackageTree {

    record Snapshot(List<PackageNode> roots, boolean provisional, int files, int lines) {
    }

    private static final Duration TYPING_DELAY = Duration.millis(250);
    private static final int MAX_REPAIRS = 4;

    private final PreviewProjectQuery preview;
    private final ProjectForm form;
    private final ObjectProperty<Snapshot> snapshot = new SimpleObjectProperty<>();
    private final BooleanProperty stale = new SimpleBooleanProperty(false);
    private final PauseTransition debounce = new PauseTransition(TYPING_DELAY);

    private int ticket;

    LivePackageTree(PreviewProjectQuery preview, ProjectForm form) {
        this.preview = preview;
        this.form = form;

        debounce.setOnFinished(event -> refresh());
        form.onChange(debounce::playFromStart);
        refresh();
    }

    ObjectProperty<Snapshot> snapshot() {
        return snapshot;
    }

    BooleanProperty stale() {
        return stale;
    }

    void refresh() {
        int current = ++ticket;
        ProjectRequest request = form.toRequest();

        Background.run(() -> compute(request), result -> {
            if (current == ticket) {
                result.ifPresentOrElse(this::show, () -> stale.set(true));
            }
        });
    }

    private void show(Snapshot fresh) {
        snapshot.set(fresh);
        stale.set(false);
    }

    private Snapshot snapshot(PreviewResult.Preview ready, boolean provisional) {
        int lines = ready.files().stream().mapToInt(PreviewFile::lines).sum();

        return new Snapshot(PackageTrees.from(ready.files()), provisional, ready.files().size(), lines);
    }

    private Optional<Snapshot> compute(ProjectRequest request) {
        ProjectRequest current = request;

        for (int attempt = 0; attempt <= MAX_REPAIRS; attempt++) {
            PreviewResult result = preview.preview(current);

            if (result instanceof PreviewResult.Preview ready) {
                return Optional.of(snapshot(ready, !current.equals(request)));
            }

            Optional<ProjectRequest> repaired = ProvisionalRequest.repair(current, ((PreviewResult.Failure) result).violations());

            if (repaired.isEmpty()) {
                return Optional.empty();
            }

            current = repaired.get();
        }

        return Optional.empty();
    }
}
