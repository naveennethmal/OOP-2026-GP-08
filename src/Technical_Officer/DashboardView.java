package Technical_Officer;

import Technical_Officer.DashboardDAO.CourseRate;
import Technical_Officer.DashboardDAO.Summary;
import Technical_Officer.MedicalDAO.MedicalRecord;
import Technical_Officer.NoticeDAO.Notice;
import javafx.geometry.Pos;
import javafx.scene.chart.BarChart;
import javafx.scene.chart.CategoryAxis;
import javafx.scene.chart.NumberAxis;
import javafx.scene.chart.XYChart;
import javafx.scene.control.Label;
import javafx.scene.control.Separator;
import javafx.scene.layout.*;

import java.sql.SQLException;
import java.util.List;

public class DashboardView extends VBox {

    public DashboardView() {
        getStyleClass().add("page");
        setSpacing(20);
        getChildren().add(UiKit.pageHeader("Dashboard", "Overview of attendance and medical records"));

        try {
            Summary s = DashboardDAO.summary();
            getChildren().addAll(
                    UiKit.statRow(
                            UiKit.statCard("Undergraduates", String.valueOf(s.undergraduates()), "fas-user-graduate", "blue"),
                            UiKit.statCard("Batch average", String.format("%.0f%%", s.average()), "fas-chart-line", "green"),
                            UiKit.statCard("Below 80%", String.valueOf(s.below()), "fas-exclamation-triangle", "red"),
                            UiKit.statCard("Medicals pending", String.valueOf(s.pendingMedicals()), "fas-notes-medical", "amber")),
                    buildMiddle(),
                    buildPendingMedicals());
        } catch (SQLException e) {
            UiKit.showLoadError(this, e);
        }
    }

    private HBox buildMiddle() throws SQLException {
        CategoryAxis x = new CategoryAxis();
        NumberAxis y = new NumberAxis(0, 100, 20);
        BarChart<String, Number> chart = new BarChart<>(x, y);
        chart.setAnimated(false);
        chart.setPrefHeight(270);

        XYChart.Series<String, Number> theory = new XYChart.Series<>();
        theory.setName("Theory");
        XYChart.Series<String, Number> practical = new XYChart.Series<>();
        practical.setName("Practical");
        for (CourseRate r : DashboardDAO.courseRates()) {
            if (!Double.isNaN(r.theory())) theory.getData().add(new XYChart.Data<>(r.course(), r.theory()));
            if (!Double.isNaN(r.practical())) practical.getData().add(new XYChart.Data<>(r.course(), r.practical()));
        }
        chart.getData().addAll(theory, practical);

        VBox chartCard = UiKit.card(UiKit.cardTitle("Average attendance by course (%)"), chart);
        HBox.setHgrow(chartCard, Priority.ALWAYS);
        chartCard.setMaxWidth(Double.MAX_VALUE);

        VBox notices = UiKit.card(UiKit.cardTitle("Recent notices"));
        List<Notice> all = NoticeDAO.list();
        for (Notice n : all.subList(0, Math.min(3, all.size()))) {
            Label title = new Label(n.title());
            title.setWrapText(true);
            title.getStyleClass().add("notice-mini-title");
            Label date = new Label(n.date());
            date.getStyleClass().add("hint");
            notices.getChildren().addAll(new VBox(2, title, date), new Separator());
        }
        notices.setPrefWidth(340);
        notices.setMinWidth(340);

        return new HBox(18, chartCard, notices);
    }

    private VBox buildPendingMedicals() throws SQLException {
        VBox card = UiKit.card(UiKit.cardTitle("Medicals awaiting review"));
        int count = 0;
        for (MedicalRecord m : MedicalDAO.list()) {
            if (!m.status().equals("Pending")) continue;
            Label name = new Label(m.name() + "  (" + m.regNo() + ")");
            name.getStyleClass().add("row-strong");
            Label reason = new Label(m.reason() + " - " + m.startDate()
                    + " to " + m.endDate());
            reason.getStyleClass().add("hint");
            Region spacer = new Region();
            HBox.setHgrow(spacer, Priority.ALWAYS);
            HBox row = new HBox(12, new VBox(2, name, reason), spacer, UiKit.pill("Pending", "warn"));
            row.setAlignment(Pos.CENTER_LEFT);
            card.getChildren().addAll(row, new Separator());
            count++;
        }
        if (count == 0) {
            Label none = new Label("No medicals waiting for review.");
            none.getStyleClass().add("hint");
            card.getChildren().add(none);
        }
        return card;
    }
}
