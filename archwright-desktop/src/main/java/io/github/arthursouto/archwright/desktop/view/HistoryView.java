package io.github.arthursouto.archwright.desktop.view;

import atlantafx.base.theme.Styles;
import io.github.arthursouto.archwright.desktop.history.HistoryEntry;
import io.github.arthursouto.archwright.desktop.i18n.Messages;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;

import java.util.List;

final class HistoryView extends ScrollPane {

    private static final double SPACING = 12;
    private static final double PADDING = 32;

    private final DesktopContext context;
    private final Navigation navigation;
    private final Messages messages;
    private final VBox content = new VBox(SPACING);

    HistoryView(DesktopContext context, Navigation navigation) {
        this.context = context;
        this.navigation = navigation;
        this.messages = context.messages();

        content.setPadding(new Insets(PADDING));
        setContent(content);
        setFitToWidth(true);
        getStyleClass().add(Styles.FLAT);
        render();
    }

    private void render() {
        List<HistoryEntry> entries = context.history().entries();
        content.getChildren().setAll(header(!entries.isEmpty()));

        if (entries.isEmpty()) {
            content.getChildren().add(empty());
            return;
        }

        entries.forEach(entry -> content.getChildren().add(
                new HistoryRow(context, entry, navigation, () -> remove(entry))));
    }

    private HBox header(boolean canClear) {
        Label title = new Label(messages.text("history.title"));
        title.getStyleClass().add(Styles.TITLE_2);
        Label subtitle = new Label(messages.text("history.subtitle"));
        subtitle.setWrapText(true);
        subtitle.getStyleClass().add(Styles.TEXT_MUTED);

        Button clear = new Button(messages.text("history.clear"));
        clear.setDisable(!canClear);
        clear.setOnAction(event -> clear());

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);
        VBox text = new VBox(4, title, subtitle);
        HBox header = new HBox(SPACING, text, spacer, clear);
        header.setAlignment(Pos.CENTER_LEFT);

        return header;
    }

    private Label empty() {
        Label empty = new Label(messages.text("history.empty"));
        empty.setWrapText(true);
        empty.getStyleClass().add(Styles.TEXT_MUTED);

        return empty;
    }

    private void remove(HistoryEntry entry) {
        context.history().remove(entry.id());
        render();
    }

    private void clear() {
        if (context.dialogs().confirmAction(getScene().getWindow(), messages.text("history.clear.confirm"), messages.text("history.clear.action"))) {
            context.history().clear();
            render();
        }
    }
}
