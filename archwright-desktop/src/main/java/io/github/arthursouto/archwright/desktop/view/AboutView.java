package io.github.arthursouto.archwright.desktop.view;

import atlantafx.base.theme.Styles;
import io.github.arthursouto.archwright.desktop.i18n.Messages;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.VBox;

final class AboutView extends ScrollPane {

    private static final double LOGO_SIZE = 112;
    private static final double TEXT_WIDTH = 520;

    AboutView(DesktopContext context) {
        Messages messages = context.messages();

        Label title = new Label(messages.text("app.title"));
        title.getStyleClass().add(Styles.TITLE_1);
        Label tagline = new Label(messages.text("brand.tagline"));
        tagline.getStyleClass().add(Styles.TEXT_MUTED);

        FlowPane links = new FlowPane(12, 12,
                link(context, "link.guide", Links.GUIDE),
                link(context, "link.repository", Links.REPOSITORY),
                link(context, "link.releases", Links.RELEASES));
        links.setAlignment(Pos.CENTER);

        VBox content = new VBox(16, Brand.logo(LOGO_SIZE), title, tagline, paragraph(messages.text("about.description")), links,
                small(messages.text("about.license")), small(messages.text("about.trademark")));
        content.setAlignment(Pos.TOP_CENTER);
        content.setPadding(new Insets(48, 32, 32, 32));

        setContent(content);
        setFitToWidth(true);
        getStyleClass().add(Styles.FLAT);
    }

    private Label paragraph(String text) {
        Label label = new Label(text);
        label.setWrapText(true);
        label.setMaxWidth(TEXT_WIDTH);
        label.setAlignment(Pos.CENTER);
        label.setStyle("-fx-text-alignment: center;");

        return label;
    }

    private Label small(String text) {
        Label label = paragraph(text);
        label.getStyleClass().addAll(Styles.TEXT_MUTED, Styles.TEXT_SMALL);

        return label;
    }

    private Button link(DesktopContext context, String key, String url) {
        Button button = new Button(context.messages().text(key));
        button.setOnAction(event -> context.dialogs().openUrl(url));

        return button;
    }
}
