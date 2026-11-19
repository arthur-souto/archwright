package io.github.arthursouto.archwright.desktop.view;

import atlantafx.base.theme.Styles;
import javafx.geometry.Insets;
import javafx.scene.control.Label;

final class Hint {

    private static final double INDENT = 26;

    private Hint() {
    }

    static Label of(String text) {
        Label hint = new Label(text);
        hint.setWrapText(true);
        hint.getStyleClass().addAll(Styles.TEXT_MUTED, Styles.TEXT_SMALL);

        return hint;
    }

    static Label indented(String text) {
        Label hint = of(text);
        hint.setPadding(new Insets(0, 0, 0, INDENT));

        return hint;
    }
}
