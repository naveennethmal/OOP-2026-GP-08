package Technical_Officer;

import javafx.geometry.Pos;
import javafx.scene.chart.BarChart;
import javafx.scene.chart.CategoryAxis;
import javafx.scene.chart.NumberAxis;
import javafx.scene.chart.XYChart;
import javafx.scene.control.Label;
import javafx.scene.control.Separator;
import javafx.scene.layout.*;

import java.util.List;
import java.util.Map;

import static Technical_Officer.TechnicianDAO.text;
import static Technical_Officer.TechnicianDAO.toDouble;
import static Technical_Officer.TechnicianDAO.toInt;

public class DashboardView extends VBox {

    public DashboardView() {
        getStyleClass().add("page");
        setSpacing(20);
        getChildren().add(UiKit.pageHeader("Dashboard", "Overview of attendance and medical records"));

        try {
            Map<String, Object> s = TechnicianDAO.dashboardSummary();
            getChildren().addAll(
                    UiKit.statRow(
                            UiKit.statCard("Undergraduates", String.valueOf(toInt(s.get("students"))), "fas-user-graduate", "blue"),
                            UiKit.statCard("Batch average", String.format("%.0f%%", toDouble(s.get("avg_rate"))), "fas-chart-line", "green"),
                            UiKit.statCard("Below 80%", String.valueOf(toInt(s.get("below"))), "fas-exclamation-triangle", "red"),
                            UiKit.statCard("Medicals pending", String.valueOf(toInt(s.get("pending"))), "fas-notes-medical", "amber")),
                    buildMiddle(),
                    buildPendingMedicals());
        } catch (Exception e) {
            UiKit.showLoadError(this, e);
        }
    }

    private HBox buildMiddle() throws Exception {
        BarChart<String, Number> chart = new BarChart<>(new CategoryAxis(), new NumberAxis(0, 100, 20));
        chart.setAnimated(false);
        chart.setPrefHeight(270);

        XYChart.Series<String, Number> theory = new XYChart.Series<>();
        theory.setName("Theory");
        XYChart.Series<String, Number> practical = new XYChart.Series<>();
        practical.setName("Practical");
        for (Map<String, Object> r : TechnicianDAO.courseRates()) {
            XYChart.Series<String, Number> series = text(r.get("kind")).equals("P") ? practical : theory;
            series.getData().add(new XYChart.Data<>(text(r.get("course_code")), toDouble(r.get("rate"))));
        }
        chart.getData().addAll(theory, practical);

        VBox chartCard = UiKit.card(UiKit.cardTitle("Average attendance by course (%)"), chart);
        HBox.setHgrow(chartCard, Priority.ALWAYS);
        chartCard.setMaxWidth(Double.MAX_VALUE);

        VBox notices = UiKit.card(UiKit.cardTitle("Recent notices"));
        List<Map<String, Object>> all = TechnicianDAO.notices();
        for (Map<String, Object> n : all.subList(0, Math.min(3, all.size()))) {
            Label title = new Label(text(n.get("title")));
            title.setWrapText(true);
            title.getStyleClass().add("notice-mini-title");
            Label date = new Label(text(n.get("published")));
            date.getStyleClass().add("hint");
            notices.getChildren().addAll(new VBox(2, title, date), new Separator());
        }
        notices.setPrefWidth(340);
        notices.setMinWidth(340);

        return new HBox(18, chartCard, notices);
    }

    private VBox buildPendingMedicals() throws Exception {
        VBox card = UiKit.card(UiKit.cardTitle("Medicals awaiting review"));
        int count = 0;
        for (Map<String, Object> m : TechnicianDAO.medicals()) {
            if (!text(m.get("approval_status")).equals("Pending")) continue;
            Label name = new Label(text(m.get("full_name")) + "  (" + text(m.get("reg_no")) + ")");
            name.getStyleClass().add("row-strong");
            Label reason = new Label(text(m.get("reason")) + " - " + text(m.get("start_date"))
                    + " to " + text(m.get("end_date")));
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
