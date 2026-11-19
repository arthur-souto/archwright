package io.github.arthursouto.archwright.desktop;

import javafx.application.Application;

public final class Launcher {

    private Launcher() {
    }

    public static void main(String[] args) {
        Application.launch(ArchwrightApp.class, args);
    }
}
