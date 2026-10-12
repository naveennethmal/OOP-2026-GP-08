package com.teclms.technician;

import com.teclms.technician.DemoData.AttendanceRow;
import com.teclms.technician.DemoData.MedicalRecord;
import com.teclms.technician.DemoData.Notice;
import javafx.collections.ObservableList;
import javafx.geometry.Pos;
import javafx.scene.chart.BarChart;
import javafx.scene.chart.CategoryAxis;
import javafx.scene.chart.NumberAxis;
import javafx.scene.chart.XYChart;
import javafx.scene.control.Label;
import javafx.scene.control.Separator;
import javafx.scene.layout.*;

public class DashboardView extends VBox {

    public DashboardView() {
        getStyleClass().add("page");
        setSpacing(20);

        ObservableList<AttendanceRow> rows = DemoData.attendance();
        double avg = rows.stream().mapToDouble(AttendanceRow::rate).average().orElse(0);
        long below = rows.stream().filter(r -> r.rate() < 80).count();
        long pending = rows.stream().filter(r -> r.medical().equals("Pending")).count();

        getChildren().addAll(
                UiKit.pageHeader("Dashboard", "Overview of attendance and medical records"),
                UiKit.statRow(
                        UiKit.statCard("Undergraduates", String.valueOf(rows.size()), "fas-user-graduate", "blue"),
                        UiKit.statCard("Batch average", String.format("%.0f%%", avg), "fas-chart-line", "green"),
                        UiKit.statCard("Below 80%", String.valueOf(below), "fas-exclamation-triangle", "red"),
                        UiKit.statCard("Medicals pending", String.valueOf(pending), "fas-notes-medical", "amber")),
                buildMiddle(),
                buildPendingMedicals());
    }

    private HBox buildMiddle() {
        CategoryAxis x = new CategoryAxis();
        NumberAxis y = new NumberAxis(0, 100, 20);
        BarChart<String, Number> chart = new BarChart<>(x, y);
        chart.setAnimated(false);
        chart.setPrefHeight(270);

        XYChart.Series<String, Number> theory = new XYChart.Series<>();
        theory.setName("Theory");
        XYChart.Series<String, Number> practical = new XYChart.Series<>();
        practical.setName("Practical");
        String[] courses = {"ICT2113", "ICT2122", "ICT2132", "ICT2142"};
        int[] t = {88, 82, 79, 85};
        int[] p = {91, 76, 84, 80};
        for (int i = 0; i < courses.length; i++) {
            theory.getData().add(new XYChart.Data<>(courses[i], t[i]));
            practical.getData().add(new XYChart.Data<>(courses[i], p[i]));
        }
        chart.getData().addAll(theory, practical);

        VBox chartCard = UiKit.card(UiKit.cardTitle("Average attendance by course (%)"), chart);
        HBox.setHgrow(chartCard, Priority.ALWAYS);
        chartCard.setMaxWidth(Double.MAX_VALUE);

        VBox notices = UiKit.card(UiKit.cardTitle("Recent notices"));
        for (Notice n : DemoData.notices().subList(0, 3)) {
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

    private VBox buildPendingMedicals() {
        VBox card = UiKit.card(UiKit.cardTitle("Medicals awaiting review"));
        int count = 0;
        for (MedicalRecord m : DemoData.medicals()) {
            if (!m.status().equals("Pending")) continue;
            Label name = new Label(m.name() + "  (" + m.regNo() + ")");
            name.getStyleClass().add("row-strong");
            Label reason = new Label(m.reason() + " - " + m.date());
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
