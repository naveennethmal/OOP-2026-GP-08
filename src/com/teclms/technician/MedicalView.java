package com.teclms.technician;

import com.teclms.technician.DemoData.AttendanceRow;
import com.teclms.technician.DemoData.MedicalRecord;
import javafx.beans.property.ReadOnlyObjectWrapper;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.scene.control.*;
import javafx.scene.layout.*;

public class MedicalView extends VBox {

    private final ObservableList<MedicalRecord> records = DemoData.medicals();

    public MedicalView() {
        getStyleClass().add("page");
        setSpacing(20);

        HBox body = new HBox(18, buildForm(), buildTable());
        body.setFillHeight(false);
        getChildren().addAll(
                UiKit.pageHeader("Medical records", "Add and maintain undergraduate medical submissions"),
                body);
    }

    private VBox buildForm() {
        ComboBox<String> student = new ComboBox<>();
        for (AttendanceRow r : DemoData.attendance()) student.getItems().add(r.regNo() + " - " + r.name());
        student.setPromptText("Select undergraduate");

        DatePicker date = new DatePicker();
        date.setPromptText("Medical date");

        TextField reason = new TextField();
        reason.setPromptText("Reason, e.g. Viral fever");

        ComboBox<String> status = new ComboBox<>(FXCollections.observableArrayList("Pending", "Approved", "Rejected"));
        status.setValue("Pending");

        Label error = new Label();
        error.getStyleClass().add("form-error");

        Button add = UiKit.primaryButton("Add record", "fas-plus");
        add.setMaxWidth(Double.MAX_VALUE);
        add.setOnAction(e -> {
            if (student.getValue() == null || date.getValue() == null || reason.getText().isBlank()) {
                error.setText("Select an undergraduate, a date, and enter a reason.");
                return;
            }
            String[] parts = student.getValue().split(" - ", 2);
            records.add(0, new MedicalRecord(parts[0], parts[1], date.getValue().toString(),
                    reason.getText().trim(), status.getValue()));
            student.setValue(null);
            date.setValue(null);
            reason.clear();
            status.setValue("Pending");
            error.setText("");
        });

        VBox form = UiKit.card(
                UiKit.cardTitle("Add medical record"),
                UiKit.field("Undergraduate", student),
                UiKit.field("Medical date", date),
                UiKit.field("Reason", reason),
                UiKit.field("Status", status),
                error,
                add);
        form.setPrefWidth(320);
        form.setMinWidth(320);
        return form;
    }

    private VBox buildTable() {
        TableView<MedicalRecord> table = new TableView<>(records);
        table.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);
        table.setFixedCellSize(48);
        table.setPrefHeight(48 * 8 + 40);
        table.setPlaceholder(new Label("No medical records yet."));

        TableColumn<MedicalRecord, String> reg = new TableColumn<>("Reg no");
        reg.setCellValueFactory(d -> new SimpleStringProperty(d.getValue().regNo()));

        TableColumn<MedicalRecord, String> name = new TableColumn<>("Name");
        name.setCellValueFactory(d -> new SimpleStringProperty(d.getValue().name()));

        TableColumn<MedicalRecord, String> date = new TableColumn<>("Date");
        date.setCellValueFactory(d -> new SimpleStringProperty(d.getValue().date()));

        TableColumn<MedicalRecord, String> reason = new TableColumn<>("Reason");
        reason.setCellValueFactory(d -> new SimpleStringProperty(d.getValue().reason()));

        TableColumn<MedicalRecord, String> status = new TableColumn<>("Status");
        status.setCellValueFactory(d -> new SimpleStringProperty(d.getValue().status()));
        status.setCellFactory(UiKit.pillCells());

        TableColumn<MedicalRecord, MedicalRecord> actions = new TableColumn<>("Actions");
        actions.setCellValueFactory(d -> new ReadOnlyObjectWrapper<>(d.getValue()));
        actions.setCellFactory(col -> new TableCell<>() {
            private final Button approve = UiKit.iconButton("fas-check", "icon-btn-ok");
            private final Button reject = UiKit.iconButton("fas-times", "icon-btn-bad");
            private final Button delete = UiKit.iconButton("fas-trash", "icon-btn-bad");
            private final HBox box = new HBox(2, approve, reject, delete);
            {
                approve.setOnAction(e -> changeStatus(getIndex(), "Approved"));
                reject.setOnAction(e -> changeStatus(getIndex(), "Rejected"));
                delete.setOnAction(e -> {
                    if (getIndex() >= 0 && getIndex() < records.size()) records.remove(getIndex());
                });
            }
            @Override
            protected void updateItem(MedicalRecord item, boolean empty) {
                super.updateItem(item, empty);
                setGraphic(empty || item == null ? null : box);
            }
        });

        table.getColumns().addAll(reg, name, date, reason, status, actions);

        VBox card = UiKit.card(UiKit.cardTitle("Submitted medicals"), table);
        HBox.setHgrow(card, Priority.ALWAYS);
        card.setMaxWidth(Double.MAX_VALUE);
        return card;
    }

    private void changeStatus(int index, String status) {
        if (index < 0 || index >= records.size()) return;
        MedicalRecord r = records.get(index);
        records.set(index, new MedicalRecord(r.regNo(), r.name(), r.date(), r.reason(), status));
    }
}
