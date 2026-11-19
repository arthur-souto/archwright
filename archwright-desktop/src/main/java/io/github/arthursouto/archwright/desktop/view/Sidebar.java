package io.github.arthursouto.archwright.desktop.view;

import io.github.arthursouto.archwright.desktop.i18n.Messages;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;

import java.util.EnumMap;
import java.util.Map;
import java.util.function.Consumer;

final class Sidebar extends VBox {

    private static final double WIDTH = 232;
    private static final double LOGO_SIZE = 44;
    private static final double SPACING = 6;
    private static final String SELECTED = "selected";

    private final Messages messages;
    private final Map<Section, Button> items = new EnumMap<>(Section.class);
    private final Button theme = new Button();

    Sidebar(Messages messages, Consumer<Section> navigate, Runnable toggleTheme) {
        super(SPACING);
        this.messages = messages;

        getStyleClass().add("sidebar");
        setPrefWidth(WIDTH);
        setMinWidth(WIDTH);
        setPadding(new Insets(24, 16, 20, 16));

        getChildren().add(brand());

        for (Section section : Section.values()) {
            Button item = item(section, navigate);
            items.put(section, item);
            getChildren().add(item);
        }

        Region spacer = new Region();
        VBox.setVgrow(spacer, Priority.ALWAYS);
        theme.getStyleClass().addAll("nav-item", "theme-toggle");
        theme.setMaxWidth(Double.MAX_VALUE);
        theme.setOnAction(event -> toggleTheme.run());
        getChildren().addAll(spacer, theme);
    }

    void select(Section selected) {
        items.forEach((section, item) -> {
            item.getStyleClass().remove(SELECTED);

            if (section == selected) {
                item.getStyleClass().add(SELECTED);
            }
        });
    }

    void themeIsDark(boolean dark) {
        theme.setText(messages.text(dark ? "theme.toggle.light" : "theme.toggle.dark"));
    }

    private HBox brand() {
        Label name = new Label(messages.text("app.title"));
        name.getStyleClass().add("brand-title");
        Label tagline = new Label(messages.text("brand.tagline"));
        tagline.getStyleClass().add("brand-tagline");
        tagline.setWrapText(true);

        VBox text = new VBox(2, name, tagline);
        HBox brand = new HBox(12, Brand.logo(LOGO_SIZE), text);
        brand.setAlignment(Pos.CENTER_LEFT);
        brand.setPadding(new Insets(0, 4, 20, 4));

        return brand;
    }

    private Button item(Section section, Consumer<Section> navigate) {
        Button item = new Button(messages.text(section.messageKey()), Brand.icon(section.icon()));
        item.getStyleClass().add("nav-item");
        item.setGraphicTextGap(12);
        item.setMaxWidth(Double.MAX_VALUE);
        item.setOnAction(event -> navigate.accept(section));

        return item;
    }
}
