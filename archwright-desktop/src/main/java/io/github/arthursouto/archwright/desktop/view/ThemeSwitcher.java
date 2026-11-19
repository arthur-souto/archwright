package io.github.arthursouto.archwright.desktop.view;

import atlantafx.base.theme.PrimerDark;
import atlantafx.base.theme.PrimerLight;
import javafx.application.Application;

final class ThemeSwitcher {

    private ThemeSwitcher() {
    }

    static void apply(boolean dark) {
        String stylesheet = dark ? new PrimerDark().getUserAgentStylesheet() : new PrimerLight().getUserAgentStylesheet();
        Application.setUserAgentStylesheet(stylesheet);
    }
}
