package io.github.arthursouto.archwright.desktop.view;

import atlantafx.base.theme.Styles;
import io.github.arthursouto.archwright.desktop.history.HistoryEntry;
import io.github.arthursouto.archwright.desktop.i18n.Messages;
import io.github.arthursouto.archwright.desktop.wizard.Preset;
import io.github.arthursouto.archwright.desktop.wizard.VersionDefaults;
import io.github.arthursouto.archwright.engine.application.port.in.BootVersionOption;
import io.github.arthursouto.archwright.engine.application.port.in.BootVersions;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;

import java.util.List;

final class HomeView extends ScrollPane {

    private static final double SPACING = 28;
    private static final double GAP = 16;
    private static final double PADDING = 32;
    private static final double LOGO_SIZE = 84;
    private static final double CARD_WIDTH = 290;
    private static final int RECENT_COUNT = 4;

    private final DesktopContext context;
    private final Navigation navigation;
    private final Messages messages;
    private final Label bootValue = new Label();
    private final Label bootCaption = new Label();

    HomeView(DesktopContext context, Navigation navigation) {
        this.context = context;
        this.navigation = navigation;
        this.messages = context.messages();

        VBox content = new VBox(SPACING, hero(), stats(), charts(), presets(), recent(), tips());
        content.setPadding(new Insets(PADDING));
        setContent(content);
        setFitToWidth(true);
        getStyleClass().add(Styles.FLAT);
        loadLatestBoot();
    }

    private Node hero() {
        Label title = new Label(messages.text("home.welcome"));
        title.getStyleClass().add(Styles.TITLE_1);
        Label subtitle = new Label(messages.text("home.subtitle"));
        subtitle.setWrapText(true);
        subtitle.getStyleClass().addAll(Styles.TEXT_MUTED, Styles.TEXT_CAPTION);

        Button start = new Button(messages.text("home.start"));
        start.getStyleClass().add(Styles.ACCENT);
        start.setOnAction(event -> navigation.startProject(form -> {
        }));

        VBox text = new VBox(10, title, subtitle, start);
        HBox hero = new HBox(24, Brand.logo(LOGO_SIZE), text);
        hero.getStyleClass().add("hero");
        hero.setAlignment(Pos.CENTER_LEFT);
        HBox.setHgrow(text, Priority.ALWAYS);

        return hero;
    }

    private Node stats() {
        HBox stats = new HBox(GAP,
                stat("home.stats.generated", String.valueOf(context.history().total()), messages.text("home.stats.generated.caption")),
                stat("home.stats.boot", bootValue, bootCaption),
                stat("home.stats.styles", String.valueOf(context.catalog().architectureStyles().size()), messages.text("home.stats.styles.caption")));
        stats.getChildren().forEach(card -> HBox.setHgrow(card, Priority.ALWAYS));

        bootValue.setText("…");
        bootCaption.setText(messages.text("home.stats.boot.loading"));

        return stats;
    }

    private Node charts() {
        return section("home.charts.title", null, new UsageCharts(context, context.history().entries()));
    }

    private VBox stat(String titleKey, String value, String caption) {
        return stat(titleKey, new Label(value), new Label(caption));
    }

    private VBox stat(String titleKey, Label value, Label caption) {
        Label title = new Label(messages.text(titleKey));
        title.getStyleClass().addAll(Styles.TEXT_MUTED, Styles.TEXT_SMALL);
        value.getStyleClass().add("stat-value");
        caption.getStyleClass().addAll(Styles.TEXT_MUTED, Styles.TEXT_SMALL);
        caption.setWrapText(true);

        VBox card = new VBox(4, title, value, caption);
        card.getStyleClass().add("card");
        card.setMaxWidth(Double.MAX_VALUE);

        return card;
    }

    private void loadLatestBoot() {
        Background.run(context.catalog()::bootVersions, this::showBoot, failure -> {
            bootValue.setText("—");
            bootCaption.setText(messages.text("home.stats.boot.failed"));
        });
    }

    private void showBoot(BootVersions versions) {
        bootValue.setText(VersionDefaults.boot(versions.versions(), "").map(BootVersionOption::version).orElse("—"));
        bootCaption.setText(messages.text("versions.origin." + versions.origin().name()));
    }

    private Node presets() {
        HBox cards = new HBox(GAP);

        for (Preset preset : Preset.all()) {
            cards.getChildren().add(presetCard(preset));
        }

        cards.getChildren().forEach(card -> HBox.setHgrow(card, Priority.ALWAYS));

        return section("home.presets.title", "home.presets.subtitle", cards);
    }

    private Node presetCard(Preset preset) {
        Label title = new Label(messages.text("preset." + preset.id() + ".title"));
        title.getStyleClass().add(Styles.TEXT_BOLD);
        Label description = new Label(messages.text("preset." + preset.id() + ".description"));
        description.setWrapText(true);
        description.getStyleClass().addAll(Styles.TEXT_MUTED, Styles.TEXT_SMALL);
        Button use = new Button(messages.text("home.presets.use"));
        use.setOnAction(event -> navigation.startProject(preset::applyTo));

        Region grow = new Region();
        VBox.setVgrow(grow, Priority.ALWAYS);
        VBox card = new VBox(8, title, description, grow, use);
        card.getStyleClass().add("card");
        card.setPrefWidth(CARD_WIDTH);
        card.setMaxWidth(Double.MAX_VALUE);
        card.setMinHeight(150);

        return card;
    }

    private Node recent() {
        List<HistoryEntry> entries = context.history().recent(RECENT_COUNT);
        VBox rows = new VBox(10);

        if (entries.isEmpty()) {
            Label empty = new Label(messages.text("home.recent.empty"));
            empty.setWrapText(true);
            empty.getStyleClass().add(Styles.TEXT_MUTED);
            rows.getChildren().add(empty);
        }

        entries.forEach(entry -> rows.getChildren().add(new HistoryRow(context, entry, navigation, null)));

        Button all = new Button(messages.text("home.recent.all"));
        all.getStyleClass().add(Styles.FLAT);
        all.setOnAction(event -> navigation.show(Section.HISTORY));
        all.setVisible(context.history().total() > 0 && !entries.isEmpty());
        all.setManaged(all.isVisible());

        return section("home.recent.title", null, new VBox(10, rows, all));
    }

    private Node tips() {
        HBox cards = new HBox(GAP,
                tip("tip.fitness", null),
                tip("tip.styles", null),
                tip("tip.guide", linkButtons()));
        cards.getChildren().forEach(card -> HBox.setHgrow(card, Priority.ALWAYS));

        return section("home.tips.title", null, cards);
    }

    private Node tip(String key, Node extra) {
        Label title = new Label(messages.text(key + ".title"));
        title.getStyleClass().add(Styles.TEXT_BOLD);
        Label text = new Label(messages.text(key + ".text"));
        text.setWrapText(true);
        text.getStyleClass().addAll(Styles.TEXT_MUTED, Styles.TEXT_SMALL);

        VBox card = new VBox(8, title, text);

        if (extra != null) {
            card.getChildren().add(extra);
        }

        card.getStyleClass().add("card");
        card.setPrefWidth(CARD_WIDTH);
        card.setMaxWidth(Double.MAX_VALUE);

        return card;
    }

    private Node linkButtons() {
        return new FlowPane(8, 8,
                link("link.guide", Links.GUIDE), link("link.repository", Links.REPOSITORY), link("link.releases", Links.RELEASES));
    }

    private Button link(String key, String url) {
        Button button = new Button(messages.text(key));
        button.getStyleClass().add(Styles.SMALL);
        button.setOnAction(event -> context.dialogs().openUrl(url));

        return button;
    }

    private Node section(String titleKey, String subtitleKey, Node body) {
        Label title = new Label(messages.text(titleKey));
        title.getStyleClass().add(Styles.TITLE_3);
        VBox section = new VBox(10, title);

        if (subtitleKey != null) {
            Label subtitle = new Label(messages.text(subtitleKey));
            subtitle.getStyleClass().add(Styles.TEXT_MUTED);
            section.getChildren().add(subtitle);
        }

        section.getChildren().add(body);

        return section;
    }
}
