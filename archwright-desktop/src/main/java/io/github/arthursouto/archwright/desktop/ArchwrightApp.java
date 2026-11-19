package io.github.arthursouto.archwright.desktop;

import io.github.arthursouto.archwright.desktop.history.ProjectHistory;
import io.github.arthursouto.archwright.desktop.i18n.Messages;
import io.github.arthursouto.archwright.desktop.preferences.UserPreferences;
import io.github.arthursouto.archwright.desktop.view.DesktopContext;
import io.github.arthursouto.archwright.desktop.view.NativeDialogs;
import io.github.arthursouto.archwright.desktop.view.AppShell;
import io.github.arthursouto.archwright.engine.config.ArchwrightEngine;
import javafx.application.Application;
import javafx.scene.Scene;
import javafx.scene.image.Image;
import javafx.stage.Stage;

public final class ArchwrightApp extends Application {

    private static final double WIDTH = 1480;
    private static final double HEIGHT = 840;
    private static final String ICON = "icon.png";

    private ArchwrightEngine engine;

    @Override
    public void init() {
        engine = ArchwrightEngine.create();
    }

    @Override
    public void start(Stage stage) {
        Messages messages = Messages.portuguese();
        UserPreferences preferences = UserPreferences.system();

        DesktopContext context = new DesktopContext(
                engine.catalog(), engine.previewProject(), engine.generateProject(), messages, preferences, ProjectHistory.system(),
                new NativeDialogs(messages, getHostServices()));

        stage.setTitle(messages.text("app.title"));
        stage.getIcons().add(new Image(ArchwrightApp.class.getResourceAsStream(ICON)));
        stage.setScene(new Scene(new AppShell(context), WIDTH, HEIGHT));
        stage.show();
    }
}
