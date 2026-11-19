package io.github.arthursouto.archwright.desktop.view;

import javafx.scene.Node;

interface StepView {

    Node content();

    default void onShow() {
    }
}
