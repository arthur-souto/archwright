package io.github.arthursouto.archwright.desktop.view;

import atlantafx.base.theme.Styles;
import io.github.arthursouto.archwright.desktop.history.HistoryEntry;
import io.github.arthursouto.archwright.desktop.i18n.Messages;
import io.github.arthursouto.archwright.engine.application.port.in.ArchitectureStyleOption;
import javafx.scene.Node;
import javafx.scene.chart.BarChart;
import javafx.scene.chart.CategoryAxis;
import javafx.scene.chart.NumberAxis;
import javafx.scene.chart.XYChart;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;

import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

final class UsageCharts extends HBox {

    private static final double GAP = 16;
    private static final double HEIGHT = 230;
    private static final int TOP = 6;

    private final Messages messages;

    UsageCharts(DesktopContext context, List<HistoryEntry> entries) {
        super(GAP);
        this.messages = context.messages();

        Map<String, String> styleLabels = context.catalog().architectureStyles().stream()
                .collect(Collectors.toMap(ArchitectureStyleOption::id, ArchitectureStyleOption::label, (first, second) -> first));

        getChildren().addAll(
                panel("chart.styles", entries, count(entries, entry -> List.of(styleLabels.getOrDefault(entry.architectureStyle(), entry.architectureStyle())))),
                panel("chart.dependencies", entries, count(entries, HistoryEntry::dependencies)));
        getChildren().forEach(panel -> HBox.setHgrow(panel, Priority.ALWAYS));
    }

    private Map<String, Long> count(List<HistoryEntry> entries, Function<HistoryEntry, List<String>> names) {
        return entries.stream()
                .flatMap(entry -> names.apply(entry).stream())
                .collect(Collectors.groupingBy(Function.identity(), Collectors.counting()))
                .entrySet().stream()
                .sorted(Map.Entry.<String, Long>comparingByValue(Comparator.reverseOrder()).thenComparing(Map.Entry.comparingByKey()))
                .limit(TOP)
                .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue, (first, second) -> first, LinkedHashMap::new));
    }

    private VBox panel(String key, List<HistoryEntry> entries, Map<String, Long> values) {
        Label title = new Label(messages.text(key + ".title"));
        title.getStyleClass().add(Styles.TEXT_BOLD);
        Label caption = new Label(messages.text(key + ".caption"));
        caption.getStyleClass().addAll(Styles.TEXT_MUTED, Styles.TEXT_SMALL);

        VBox panel = new VBox(8, title, caption, entries.isEmpty() ? empty() : chart(values));
        panel.getStyleClass().add("card");
        panel.setPrefWidth(0);
        panel.setMaxWidth(Double.MAX_VALUE);

        return panel;
    }

    private Node empty() {
        Label empty = new Label(messages.text("chart.empty"));
        empty.setWrapText(true);
        empty.setMinHeight(HEIGHT);
        empty.getStyleClass().add(Styles.TEXT_MUTED);

        return empty;
    }

    private Node chart(Map<String, Long> values) {
        CategoryAxis names = new CategoryAxis();
        NumberAxis counts = new NumberAxis();
        counts.setMinorTickVisible(false);
        counts.setTickUnit(1);
        counts.setForceZeroInRange(true);

        BarChart<String, Number> chart = new BarChart<>(names, counts);
        chart.setLegendVisible(false);
        chart.setAnimated(false);
        chart.setHorizontalGridLinesVisible(true);
        chart.setVerticalGridLinesVisible(false);
        chart.setBarGap(2);
        chart.setCategoryGap(24);
        chart.setPrefHeight(HEIGHT);
        chart.setMinHeight(HEIGHT);

        XYChart.Series<String, Number> series = new XYChart.Series<>();
        values.forEach((name, total) -> series.getData().add(new XYChart.Data<>(name, total)));
        chart.getData().add(series);

        return chart;
    }
}
