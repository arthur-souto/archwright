package io.github.arthursouto.archwright.desktop.view;

import javafx.scene.Group;
import javafx.scene.Node;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.StackPane;
import javafx.scene.shape.SVGPath;

final class Brand {

    private static final String LOGO = "/io/github/arthursouto/archwright/desktop/icon.png";
    private static final String STYLESHEET = "/io/github/arthursouto/archwright/desktop/archwright.css";

    private static final double ICON_BOX = 24;

    private Brand() {
    }

    static ImageView logo(double size) {
        ImageView view = new ImageView(Holder.IMAGE);
        view.setFitWidth(size);
        view.setFitHeight(size);
        view.setPreserveRatio(true);
        view.setSmooth(true);

        return view;
    }

    static Node icon(String path) {
        SVGPath shape = new SVGPath();
        shape.setContent(path);
        shape.getStyleClass().add("nav-icon");

        StackPane box = new StackPane(new Group(shape));
        box.setMinSize(ICON_BOX, ICON_BOX);
        box.setPrefSize(ICON_BOX, ICON_BOX);
        box.setMaxSize(ICON_BOX, ICON_BOX);

        return box;
    }

    static String stylesheet() {
        return Brand.class.getResource(STYLESHEET).toExternalForm();
    }

    private static final class Holder {

        private static final Image IMAGE = new Image(Brand.class.getResourceAsStream(LOGO));
    }
}
