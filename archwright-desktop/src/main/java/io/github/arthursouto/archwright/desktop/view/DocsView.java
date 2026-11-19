package io.github.arthursouto.archwright.desktop.view;

import atlantafx.base.theme.Styles;
import io.github.arthursouto.archwright.desktop.i18n.Messages;
import javafx.geometry.Insets;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;

import java.util.List;

final class DocsView extends ScrollPane {

    private static final double GAP = 16;
    private static final double PADDING = 32;
    private static final double CARD_WIDTH = 300;

    private record Doc(String id, String url) {
    }

    private record Group(String id, List<Doc> docs) {
    }

    private static final List<Group> GROUPS = List.of(
            new Group("start", List.of(
                    new Doc("install", Links.GUIDE),
                    new Doc("readme", Links.REPOSITORY + "#readme"),
                    new Doc("generated", Links.DOCS + "contracts/GENERATED_PROJECT.md"))),
            new Group("concepts", List.of(
                    new Doc("architecture", Links.DOCS + "architecture/ARCHITECTURE.md"),
                    new Doc("rules", Links.DOCS + "domain/BUSINESS_RULES.md"),
                    new Doc("adr", Links.DOCS + "adr"))),
            new Group("reference", List.of(
                    new Doc("engine", Links.DOCS + "contracts/ENGINE_API.md"),
                    new Doc("templates", Links.DOCS + "contracts/TEMPLATE_FORMAT.md"),
                    new Doc("testing", Links.DOCS + "operations/TESTING.md"))),
            new Group("project", List.of(
                    new Doc("roadmap", Links.DOCS + "implementation/ROADMAP.md"),
                    new Doc("releases", Links.RELEASES))));

    private final DesktopContext context;
    private final Messages messages;

    DocsView(DesktopContext context) {
        this.context = context;
        this.messages = context.messages();

        Label title = new Label(messages.text("docs.title"));
        title.getStyleClass().add(Styles.TITLE_2);
        Label subtitle = new Label(messages.text("docs.subtitle"));
        subtitle.setWrapText(true);
        subtitle.getStyleClass().add(Styles.TEXT_MUTED);

        VBox content = new VBox(24, new VBox(4, title, subtitle));
        GROUPS.forEach(group -> content.getChildren().add(group(group)));
        content.setPadding(new Insets(PADDING));

        setContent(content);
        setFitToWidth(true);
        getStyleClass().add(Styles.FLAT);
    }

    private VBox group(Group group) {
        Label heading = new Label(messages.text("docs.group." + group.id()));
        heading.getStyleClass().addAll(Styles.TITLE_4, "group-title");

        HBox cards = new HBox(GAP);
        group.docs().forEach(doc -> cards.getChildren().add(card(doc)));

        for (int filler = group.docs().size(); filler < 3; filler++) {
            Region empty = new Region();
            HBox.setHgrow(empty, Priority.ALWAYS);
            empty.setPrefWidth(CARD_WIDTH);
            cards.getChildren().add(empty);
        }

        return new VBox(10, heading, cards);
    }

    private VBox card(Doc doc) {
        Label title = new Label(messages.text("docs." + doc.id() + ".title"));
        title.getStyleClass().add(Styles.TEXT_BOLD);
        Label text = new Label(messages.text("docs." + doc.id() + ".text"));
        text.setWrapText(true);
        text.getStyleClass().addAll(Styles.TEXT_MUTED, Styles.TEXT_SMALL);

        Region grow = new Region();
        VBox.setVgrow(grow, Priority.ALWAYS);
        Button open = new Button(messages.text("docs.open"));
        open.getStyleClass().add(Styles.SMALL);
        open.setOnAction(event -> context.dialogs().openUrl(doc.url()));

        VBox card = new VBox(8, title, text, grow, open);
        card.getStyleClass().add("card");
        card.setMinHeight(130);
        card.setPrefWidth(CARD_WIDTH);
        HBox.setHgrow(card, Priority.ALWAYS);
        card.setMaxWidth(Double.MAX_VALUE);

        return card;
    }
}
