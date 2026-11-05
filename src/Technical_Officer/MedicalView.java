package Technical_Officer;

import Technical_Officer.MedicalDAO.MedicalRecord;
import javafx.beans.property.ReadOnlyObjectWrapper;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.stage.FileChooser;

import java.awt.Desktop;
import java.io.File;
import java.io.IOException;
import java.sql.SQLException;

public class MedicalView extends VBox {

    private final ObservableList<MedicalRecord> records = FXCollections.observableArrayList();
    private final ComboBox<String> student = new ComboBox<>();
    private final ComboBox<String> course = new ComboBox<>();
    private final DatePicker startDate = new DatePicker();
    private final DatePicker endDate = new DatePicker();
    private final TextField reason = new TextField();
    private final ComboBox<String> status = new ComboBox<>(FXCollections.observableArrayList("Pending", "Approved", "Rejected"));
    private final Label error = new Label();
    private final Button save = UiKit.primaryButton("Add record", "fas-plus");
    private MedicalRecord editing;
    private File selectedCertificate;
    private final TextField certificatePath = new TextField();

    public MedicalView() {
        getStyleClass().add("page");
        setSpacing(20);

        HBox body = new HBox(18, buildForm(), buildTable());
        body.setFillHeight(false);
        getChildren().addAll(
                UiKit.pageHeader("Medical records", "Add and maintain undergraduate medical submissions"),
                body);

        try {
            reload();
        } catch (SQLException e) {
            UiKit.showLoadError(this, e);
        }
    }

    private void reload() throws SQLException {
        records.setAll(MedicalDAO.list());
    }

    private VBox buildForm() {
        error.getStyleClass().add("form-error");
        try {
            student.getItems().setAll(MedicalDAO.students());
            course.getItems().setAll(AttendanceDAO.courses());
        } catch (SQLException e) {
            error.setText("Could not load the lists: " + e.getMessage());
            System.err.println(e.getMessage());
        }
        student.setPromptText("Select undergraduate");
        course.setPromptText("Select course");

        startDate.setPromptText("Start date");
        endDate.setPromptText("End date");

        reason.setPromptText("Reason, e.g. Viral fever");
        certificatePath.setPromptText("Optional medical certificate (PDF or image)");
        certificatePath.setEditable(false);
        Button chooseCertificate = UiKit.ghostButton("Choose file", "fas-file-upload");
        chooseCertificate.setOnAction(e -> chooseCertificate());
        HBox certificateControl = new HBox(8, certificatePath, chooseCertificate);
        HBox.setHgrow(certificatePath, Priority.ALWAYS);

        status.setValue("Pending");

        save.setMaxWidth(Double.MAX_VALUE);
        save.setOnAction(e -> {
            if (student.getValue() == null || course.getValue() == null
                    || startDate.getValue() == null || endDate.getValue() == null
                    || reason.getText().isBlank()) {
                error.setText("Select an undergraduate, start date, end date, and enter a reason.");
                return;
            }
            if (endDate.getValue().isBefore(startDate.getValue())) {
                error.setText("End date cannot be before the start date.");
                return;
            }
            String regNo = student.getValue().split(" - ", 2)[0];
            try {
                if (editing == null) {
                    MedicalDAO.add(regNo, AttendanceDAO.codeOf(course.getValue()), startDate.getValue(),
                            endDate.getValue(), reason.getText().trim(), status.getValue(), certificatePathValue());
                } else {
                    MedicalDAO.update(editing.id(), regNo, AttendanceDAO.codeOf(course.getValue()),
                            startDate.getValue(), endDate.getValue(), reason.getText().trim(), status.getValue(),
                            selectedCertificate == null ? null : selectedCertificate.getAbsolutePath());
                }
                reload();
            } catch (SQLException ex) {
                UiKit.showError(editing == null ? "Could not add the medical record." : "Could not update the medical record.", ex);
                return;
            }
            resetForm();
        });

        Button cancel = UiKit.ghostButton("Cancel edit", "fas-times");
        cancel.setMaxWidth(Double.MAX_VALUE);
        cancel.setOnAction(e -> resetForm());

        VBox form = UiKit.card(
                UiKit.cardTitle("Add medical record"),
                UiKit.field("Undergraduate", student),
                UiKit.field("Course", course),
                UiKit.field("Start date", startDate),
                UiKit.field("End date", endDate),
                UiKit.field("Reason", reason),
                UiKit.field("Certificate", certificateControl),
                UiKit.field("Status", status),
                error,
                save,
                cancel);
        form.setPrefWidth(320);
        form.setMinWidth(320);
        return form;
    }

    private void resetForm() {
        editing = null;
        student.setValue(null);
        course.setValue(null);
        startDate.setValue(null);
        endDate.setValue(null);
        reason.clear();
        selectedCertificate = null;
        certificatePath.clear();
        status.setValue("Pending");
        error.setText("");
        save.setText("Add record");
        save.setGraphic(UiKit.icon("fas-plus"));
    }

    private void editRecord(MedicalRecord record) {
        editing = record;
        student.setValue(student.getItems().stream()
                .filter(option -> option.startsWith(record.regNo() + " - ")).findFirst().orElse(null));
        course.setValue(course.getItems().stream()
                .filter(option -> AttendanceDAO.codeOf(option).equals(record.course())).findFirst().orElse(null));
        startDate.setValue(java.time.LocalDate.parse(record.startDate()));
        endDate.setValue(java.time.LocalDate.parse(record.endDate()));
        reason.setText(record.reason());
        selectedCertificate = null;
        certificatePath.setText(record.document() == null ? "" : record.document());
        status.setValue(record.status());
        error.setText("Editing selected student submission.");
        save.setText("Update record");
        save.setGraphic(UiKit.icon("fas-save"));
    }

    private String certificatePathValue() {
        return selectedCertificate == null ? null : selectedCertificate.getAbsolutePath();
    }

    private void chooseCertificate() {
        FileChooser chooser = new FileChooser();
        chooser.setTitle("Select medical certificate");
        chooser.getExtensionFilters().add(new FileChooser.ExtensionFilter(
                "PDF and images", "*.pdf", "*.png", "*.jpg", "*.jpeg"));
        File file = chooser.showOpenDialog(getScene() == null ? null : getScene().getWindow());
        if (file != null) {
            selectedCertificate = file;
            certificatePath.setText(file.getAbsolutePath());
        }
    }

    private VBox buildTable() {
        TableView<MedicalRecord> table = new TableView<>(records);
        table.getStyleClass().add("medical-records-table");
        table.setRowFactory(view -> {
            TableRow<MedicalRecord> row = new TableRow<>();
            row.setOnMouseClicked(event -> {
                if (!row.isEmpty() && event.getClickCount() == 1) editRecord(row.getItem());
            });
            return row;
        });
        table.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);
        table.setFixedCellSize(48);
        table.setPrefHeight(48 * 8 + 40);
        table.setPlaceholder(new Label("No medical records yet."));

        TableColumn<MedicalRecord, String> reg = new TableColumn<>("Reg no");
        reg.setCellValueFactory(d -> new SimpleStringProperty(d.getValue().regNo()));

        TableColumn<MedicalRecord, String> name = new TableColumn<>("Name");
        name.setCellValueFactory(d -> new SimpleStringProperty(d.getValue().name()));

        TableColumn<MedicalRecord, String> course = new TableColumn<>("Course");
        course.setCellValueFactory(d -> new SimpleStringProperty(d.getValue().course()));

        TableColumn<MedicalRecord, String> startDate = new TableColumn<>("Start date");
        startDate.setCellValueFactory(d -> new SimpleStringProperty(d.getValue().startDate()));

        TableColumn<MedicalRecord, String> endDate = new TableColumn<>("End date");
        endDate.setCellValueFactory(d -> new SimpleStringProperty(d.getValue().endDate()));

        TableColumn<MedicalRecord, String> reason = new TableColumn<>("Reason");
        reason.setCellValueFactory(d -> new SimpleStringProperty(d.getValue().reason()));

        TableColumn<MedicalRecord, String> status = new TableColumn<>("Status");
        status.setCellValueFactory(d -> new SimpleStringProperty(d.getValue().status()));
        status.setCellFactory(UiKit.pillCells());

        TableColumn<MedicalRecord, MedicalRecord> document = new TableColumn<>("View");
        document.setCellValueFactory(d -> new ReadOnlyObjectWrapper<>(d.getValue()));
        document.setCellFactory(col -> new TableCell<>() {
            private final Button view = UiKit.ghostButton("View", "fas-file-pdf");
            {
                view.setMinWidth(90);
                view.setOnAction(e -> openDocument(getItem()));
            }

            @Override
            protected void updateItem(MedicalRecord item, boolean empty) {
                super.updateItem(item, empty);
                boolean available = !empty && item != null
                        && item.document() != null && !item.document().isBlank();
                view.setDisable(!available);
                setGraphic(empty || item == null ? null : view);
            }
        });

        TableColumn<MedicalRecord, MedicalRecord> actions = new TableColumn<>("Actions");
        actions.setCellValueFactory(d -> new ReadOnlyObjectWrapper<>(d.getValue()));
        actions.setCellFactory(col -> new TableCell<>() {
            private final Button delete = UiKit.ghostButton("Delete", "fas-trash");
            private final HBox box = new HBox(5, delete);
            {
                delete.getStyleClass().add("action-delete");
                delete.setOnAction(e -> deleteRecord(getIndex()));
            }
            @Override
            protected void updateItem(MedicalRecord item, boolean empty) {
                super.updateItem(item, empty);
                setGraphic(empty || item == null ? null : box);
            }
        });

        table.getColumns().addAll(reg, name, course, startDate, endDate, reason,
                status, document, actions);
        reg.setPrefWidth(90);
        name.setPrefWidth(170);
        course.setPrefWidth(100);
        startDate.setPrefWidth(105);
        endDate.setPrefWidth(105);
        reason.setPrefWidth(170);
        status.setPrefWidth(95);
        document.setPrefWidth(100);
        actions.setPrefWidth(100);
        table.setMinWidth(1200);

        VBox card = UiKit.card(UiKit.cardTitle("Student submitted medicals"), table);
        HBox.setHgrow(card, Priority.ALWAYS);
        card.setMaxWidth(Double.MAX_VALUE);
        return card;
    }

    private void openDocument(MedicalRecord record) {
        if (record == null || record.document() == null || record.document().isBlank()) {
            new Alert(Alert.AlertType.INFORMATION,
                    "No medical certificate was submitted.").show();
            return;
        }
        File file = new File(record.document());
        if (!file.isFile()) {
            new Alert(Alert.AlertType.ERROR,
                    "The submitted certificate cannot be found:\n" + file.getAbsolutePath()).show();
            return;
        }
        if (!Desktop.isDesktopSupported()
                || !Desktop.getDesktop().isSupported(Desktop.Action.OPEN)) {
            new Alert(Alert.AlertType.ERROR,
                    "This computer cannot open medical certificates automatically.").show();
            return;
        }
        try {
            Desktop.getDesktop().open(file);
        } catch (IOException ex) {
            new Alert(Alert.AlertType.ERROR,
                    "Could not open the medical certificate:\n" + ex.getMessage()).show();
        }
    }

    private void deleteRecord(int index) {
        if (index < 0 || index >= records.size()) return;
        try {
            MedicalDAO.delete(records.get(index).id());
            reload();
        } catch (SQLException e) {
            UiKit.showError("Could not delete the medical record.", e);
        }
    }
}
