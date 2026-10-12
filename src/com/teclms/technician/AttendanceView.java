package com.teclms.technician;

import com.teclms.technician.DemoData.AttendanceRow;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.control.cell.ComboBoxTableCell;
import javafx.scene.layout.*;

public class AttendanceView extends VBox {

    private final ObservableList<AttendanceRow> rows = DemoData.attendance();
    private final FilteredList<AttendanceRow> filtered = new FilteredList<>(rows, r -> true);
    private final Label tableTitle = UiKit.cardTitle("Combined attendance - Batch 2022");

    public AttendanceView() {
        getStyleClass().add("page");
        setSpacing(20);

        Button save = UiKit.primaryButton("Save attendance", "fas-save");
        save.setOnAction(e -> new Alert(Alert.AlertType.INFORMATION,
                "Attendance saved for " + rows.size() + " undergraduates.").show());

        getChildren().addAll(
                UiKit.pageHeader("Attendance", "Record and review undergraduate attendance", save),
                buildFilters(),
                buildSummary(),
                buildTable());
    }

    private VBox buildFilters() {
        ComboBox<String> course = new ComboBox<>(FXCollections.observableArrayList(
                "ICT2132 - OOP Practicum", "ICT2113 - Data Structures",
                "ICT2122 - Web Technologies", "ICT2142 - Database Systems"));
        course.setValue("ICT2132 - OOP Practicum");
        course.setPrefWidth(220);

        ComboBox<String> batch = new ComboBox<>(FXCollections.observableArrayList("Batch 2021", "Batch 2022"));
        batch.setValue("Batch 2022");

        ComboBox<String> session = new ComboBox<>();
        for (int i = 1; i <= AttendanceRow.TOTAL_SESSIONS; i++) session.getItems().add("Session " + String.format("%02d", i));
        session.setValue("Session 07");

        DatePicker date = new DatePicker(java.time.LocalDate.now());

        ToggleGroup group = new ToggleGroup();
        ToggleButton theory = seg("Theory", "seg-first", group);
        ToggleButton practical = seg("Practical", "seg-mid", group);
        ToggleButton combined = seg("Combined", "seg-last", group);
        group.selectToggle(combined);
        group.selectedToggleProperty().addListener((o, oldT, newT) -> {
            if (newT == null) oldT.setSelected(true);
            else tableTitle.setText(((ToggleButton) newT).getText() + " attendance - " + batch.getValue());
        });
        batch.valueProperty().addListener((o, a, b) ->
                tableTitle.setText(((ToggleButton) group.getSelectedToggle()).getText() + " attendance - " + b));
        HBox segments = new HBox(-1, theory, practical, combined);

        TextField search = new TextField();
        search.setPromptText("Name or reg no (individual view)");
        search.setPrefWidth(250);
        search.textProperty().addListener((o, a, text) -> {
            String k = text.toLowerCase().trim();
            filtered.setPredicate(r -> k.isEmpty()
                    || r.name().toLowerCase().contains(k)
                    || r.regNo().toLowerCase().contains(k));
        });

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        HBox row = new HBox(16,
                UiKit.field("Course", course),
                UiKit.field("Batch", batch),
                UiKit.field("Date", date),
                UiKit.field("Attendance type", segments),
                spacer,
                UiKit.field("Find undergraduate", search));
        row.setAlignment(Pos.BOTTOM_LEFT);
        return UiKit.card(row);
    }

    private ToggleButton seg(String text, String position, ToggleGroup group) {
        ToggleButton b = new ToggleButton(text);
        b.getStyleClass().addAll("seg", position);
        b.setToggleGroup(group);
        return b;
    }

    private HBox buildSummary() {
        double avg = rows.stream().mapToDouble(AttendanceRow::rate).average().orElse(0);
        long below = rows.stream().filter(r -> r.rate() < 80).count();
        long pending = rows.stream().filter(r -> r.medical().equals("Pending")).count();
        return UiKit.statRow(
                UiKit.statCard("Batch average", String.format("%.0f%%", avg), "fas-chart-line", "green"),
                UiKit.statCard("Below 80%", String.valueOf(below), "fas-exclamation-triangle", "red"),
                UiKit.statCard("Medicals pending", String.valueOf(pending), "fas-notes-medical", "amber"));
    }

    private VBox buildTable() {
        TableView<AttendanceRow> table = new TableView<>(filtered);
        table.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);
        table.setEditable(true);
        table.setFixedCellSize(48);
        table.setPrefHeight(48 * 9 + 40);
        table.setPlaceholder(new Label("No undergraduates found."));

        TableColumn<AttendanceRow, String> reg = new TableColumn<>("Reg no");
        reg.setCellValueFactory(d -> new SimpleStringProperty(d.getValue().regNo()));

        TableColumn<AttendanceRow, String> name = new TableColumn<>("Name");
        name.setCellValueFactory(d -> new SimpleStringProperty(d.getValue().name()));

        TableColumn<AttendanceRow, String> sessions = new TableColumn<>("Sessions");
        sessions.setCellValueFactory(d -> new SimpleStringProperty(
                d.getValue().attended() + " / " + AttendanceRow.TOTAL_SESSIONS));

        TableColumn<AttendanceRow, Double> rate = new TableColumn<>("Attendance rate");
        rate.setCellValueFactory(d -> new SimpleObjectProperty<>(d.getValue().rate()));
        rate.setCellFactory(col -> new TableCell<>() {
            private final ProgressBar bar = new ProgressBar();
            private final Label text = new Label();
            private final HBox box = new HBox(10, bar, text);
            {
                box.setAlignment(Pos.CENTER_LEFT);
                bar.setPrefWidth(90);
            }
            @Override
            protected void updateItem(Double v, boolean empty) {
                super.updateItem(v, empty);
                if (empty || v == null) {
                    setGraphic(null);
                    return;
                }
                bar.setProgress(v / 100.0);
                text.setText(String.format("%.0f%%", v));
                bar.getStyleClass().removeAll("bar-ok", "bar-bad");
                bar.getStyleClass().add(v >= 80 ? "bar-ok" : "bar-bad");
                setGraphic(box);
            }
        });

        TableColumn<AttendanceRow, String> medical = new TableColumn<>("Medical");
        medical.setCellValueFactory(d -> new SimpleStringProperty(d.getValue().medical()));
        medical.setCellFactory(UiKit.pillCells());

        TableColumn<AttendanceRow, String> eligible = new TableColumn<>("Eligibility");
        eligible.setCellValueFactory(d -> new SimpleStringProperty(d.getValue().eligibility()));
        eligible.setCellFactory(UiKit.pillCells());

        TableColumn<AttendanceRow, String> today = new TableColumn<>("This session");
        today.setCellValueFactory(d -> d.getValue().sessionProperty());
        today.setCellFactory(ComboBoxTableCell.forTableColumn("Present", "Absent", "Medical"));
        today.setEditable(true);

        table.getColumns().addAll(reg, name, sessions, rate, medical, eligible, today);

        Label hint = new Label("Double-click a cell in 'This session' to mark Present, Absent, or Medical.");
        hint.getStyleClass().add("hint");

        return UiKit.card(tableTitle, table, hint);
    }
}
