package io.github.arthursouto.archwright.desktop.view;

import atlantafx.base.theme.Styles;
import io.github.arthursouto.archwright.desktop.history.HistoryEntry;
import io.github.arthursouto.archwright.desktop.i18n.Messages;
import io.github.arthursouto.archwright.engine.application.port.in.ArchitectureStyleOption;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.Tooltip;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;

import java.nio.file.Files;
import java.nio.file.Path;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Map;

final class HistoryRow extends HBox {

    private static final DateTimeFormatter WHEN = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm").withZone(ZoneId.systemDefault());

    HistoryRow(DesktopContext context, HistoryEntry entry, Navigation navigation, Runnable onRemove) {
        super(16);
        Messages messages = context.messages();

        getStyleClass().add("card");
        setAlignment(Pos.CENTER_LEFT);

        Label name = new Label(entry.artifactId());
        name.getStyleClass().add(Styles.TEXT_BOLD);
        Label details = muted(messages.text("history.entry.details", Map.of(
                "style", styleLabel(context, entry.architectureStyle()),
                "boot", entry.bootVersion(),
                "java", entry.javaVersion(),
                "files", String.valueOf(entry.fileCount()))));
        Label where = muted(WHEN.format(entry.at()) + " · " + entry.output());
        where.setWrapText(false);

        VBox text = new VBox(2, name, details, where);
        HBox.setHgrow(text, Priority.ALWAYS);

        HBox actions = new HBox(8, open(context, entry), reuse(messages, entry, navigation));

        if (onRemove != null) {
            actions.getChildren().add(remove(messages, onRemove));
        }

        getChildren().addAll(text, actions);
    }

    private Button open(DesktopContext context, HistoryEntry entry) {
        Button open = new Button(context.messages().text("history.open"));
        Path folder = folderOf(entry);
        open.setDisable(folder == null || !Files.isDirectory(folder));

        if (open.isDisabled()) {
            open.setTooltip(new Tooltip(context.messages().text("history.missing")));
        }

        open.setOnAction(event -> context.dialogs().openFolder(folder));

        return open;
    }

    private Button reuse(Messages messages, HistoryEntry entry, Navigation navigation) {
        Button reuse = new Button(messages.text("history.reuse"));
        reuse.getStyleClass().add(Styles.ACCENT);
        reuse.setOnAction(event -> navigation.startProject(form -> form.load(entry)));

        return reuse;
    }

    private Button remove(Messages messages, Runnable onRemove) {
        Button remove = new Button(messages.text("history.remove"));
        remove.getStyleClass().add(Styles.FLAT);
        remove.setOnAction(event -> onRemove.run());

        return remove;
    }

    private Path folderOf(HistoryEntry entry) {
        Path output = Path.of(entry.output());

        return Files.isDirectory(output) ? output : output.toAbsolutePath().getParent();
    }

    private String styleLabel(DesktopContext context, String id) {
        return context.catalog().architectureStyles().stream()
                .filter(style -> style.id().equals(id))
                .map(ArchitectureStyleOption::label)
                .findFirst()
                .orElse(id);
    }

    private Label muted(String text) {
        Label label = new Label(text);
        label.getStyleClass().addAll(Styles.TEXT_MUTED, Styles.TEXT_SMALL);

        return label;
    }
}
