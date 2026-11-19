package io.github.arthursouto.archwright.desktop.view;

import javafx.scene.control.ListCell;

import java.util.function.Function;

final class Cells {

    private Cells() {
    }

    static <T> ListCell<T> of(Function<T, String> text) {
        return new ListCell<>() {
            @Override
            protected void updateItem(T item, boolean empty) {
                super.updateItem(item, empty);
                setText(empty || item == null ? null : text.apply(item));
            }
        };
    }
}
