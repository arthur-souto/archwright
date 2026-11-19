package io.github.arthursouto.archwright.desktop;

import io.github.arthursouto.archwright.desktop.i18n.Messages;
import io.github.arthursouto.archwright.desktop.preferences.UserPreferences;
import io.github.arthursouto.archwright.desktop.view.DesktopContext;
import io.github.arthursouto.archwright.desktop.view.NativeDialogs;
import io.github.arthursouto.archwright.desktop.view.WizardView;
import io.github.arthursouto.archwright.desktop.wizard.ProjectForm;
import io.github.arthursouto.archwright.engine.config.ArchwrightEngine;
import javafx.application.Application;
import javafx.scene.Scene;
import javafx.stage.Stage;

public final class ArchwrightApp extends Application {

    private static final double WIDTH = 1240;
    private static final double HEIGHT = 760;

    private ArchwrightEngine engine;

    @Override
    public void init() {
        engine = ArchwrightEngine.create();
    }

    @Override
    public void start(Stage stage) {
        Messages messages = Messages.portuguese();
        UserPreferences preferences = UserPreferences.system();
        ProjectForm form = new ProjectForm();
        preferences.restoreInto(form);

        DesktopContext context = new DesktopContext(
                engine.catalog(), engine.previewProject(), engine.generateProject(), messages, preferences,
                new NativeDialogs(messages, getHostServices()));

        stage.setTitle(messages.text("app.title"));
        stage.setScene(new Scene(new WizardView(context, form), WIDTH, HEIGHT));
        stage.show();
    }
}
