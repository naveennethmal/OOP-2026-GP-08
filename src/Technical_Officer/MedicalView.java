package Technical_Officer;

import javafx.beans.property.ReadOnlyObjectWrapper;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.stage.FileChooser;

import java.awt.Desktop;
import java.io.File;
import java.time.LocalDate;
import java.util.Map;

import static Technical_Officer.TechnicianDAO.codeOf;
import static Technical_Officer.TechnicianDAO.text;
import static Technical_Officer.TechnicianDAO.toInt;

// Add, edit and delete the medical submissions of the officer's department.
public class MedicalView extends VBox {

    private final ObservableList<Map<String, Object>> records = FXCollections.observableArrayList();
    private final ComboBox<String> student = new ComboBox<>();
    private final ComboBox<String> course = new ComboBox<>();
    private final DatePicker startDate = new DatePicker();
    private final DatePicker endDate = new DatePicker();
    private final TextField reason = new TextField();
    private final ComboBox<String> status = new ComboBox<>(
            FXCollections.observableArrayList("Pending", "Approved", "Rejected"));
    private final TextField certificatePath = new TextField();
    private final Label error = new Label();
    private final Button save = UiKit.primaryButton("Add record", "fas-plus");
    private Map<String, Object> editing;          //editing == null ? add records : update records
    private File selectedCertificate;   //file

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
        } catch (Exception e) {
            UiKit.showLoadError(this, e);
        }
    }

    private void reload() throws Exception {
        records.setAll(TechnicianDAO.medicals());   //get and set all data to observableList from database
    }

    // ---------- form ----------

    private VBox buildForm() {
        error.getStyleClass().add("form-error");
        try {
            student.getItems().setAll(TechnicianDAO.students()); //get student and course data
            course.getItems().setAll(TechnicianDAO.courses());
        } catch (Exception e) {
            error.setText("Could not load the lists: " + e.getMessage());
        }


                 //place holders...
        student.setPromptText("Select undergraduate");
        course.setPromptText("Select course");
        startDate.setPromptText("Start date");
        endDate.setPromptText("End date");
        reason.setPromptText("Reason, e.g. Viral fever");
        status.setValue("Pending");

        certificatePath.setPromptText("Optional medical certificate (PDF or image)");
        certificatePath.setEditable(false);
        Button choose = UiKit.ghostButton("Choose file", "fas-file-upload");
        choose.setOnAction(e -> chooseCertificate());
        HBox certificateControl = new HBox(8, certificatePath, choose);
        HBox.setHgrow(certificatePath, Priority.ALWAYS);

        save.setMaxWidth(Double.MAX_VALUE);
        save.setOnAction(e -> saveRecord());

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
                error, save, cancel);
        form.setPrefWidth(320);
        form.setMinWidth(320);
        return form;
    }

    private void saveRecord() {
        if (student.getValue() == null || course.getValue() == null   //must enter full details
                || startDate.getValue() == null || endDate.getValue() == null
                || reason.getText().isBlank()) {
            error.setText("Select an undergraduate, start date, end date, and enter a reason.");
            return;
        }
        if (endDate.getValue().isBefore(startDate.getValue())) {   //end date >= start date
            error.setText("End date cannot be before the start date.");
            return;
        }
        String regNo = student.getValue().split(" - ", 2)[0]; //get regNo only
        String courseCode = codeOf(course.getValue());                   //get course code only
        String certificate = selectedCertificate == null ? null : selectedCertificate.getAbsolutePath();   //get file path
        try {             //adding new medical
            if (editing == null) {
                TechnicianDAO.addMedical(regNo, courseCode, startDate.getValue(), endDate.getValue(),
                        reason.getText().trim(), status.getValue(), certificate);
            } else {
                TechnicianDAO.updateMedical(toInt(editing.get("medical_id")), regNo, courseCode,
                        startDate.getValue(), endDate.getValue(), reason.getText().trim(),
                        status.getValue(), certificate);
            }
            reload();
        } catch (Exception ex) {
            UiKit.showError(editing == null ? "Could not add the medical record."
                    : "Could not update the medical record.", ex);
            return;
        }
        resetForm();
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

    private void editRecord(Map<String, Object> record) {
        editing = record;
        String regNo = text(record.get("reg_no"));
        student.setValue(student.getItems().stream()
                .filter(option -> option.startsWith(regNo + " - ")).findFirst().orElse(null));
        course.setValue(course.getItems().stream()
                .filter(option -> codeOf(option).equals(text(record.get("course_code")))).findFirst().orElse(null));
        startDate.setValue(LocalDate.parse(text(record.get("start_date"))));
        endDate.setValue(LocalDate.parse(text(record.get("end_date"))));
        reason.setText(text(record.get("reason")));
        selectedCertificate = null;
        certificatePath.setText(text(record.get("document")));
        status.setValue(text(record.get("approval_status")));
        error.setText("Editing selected student submission.");
        save.setText("Update record");  //change button to update
        save.setGraphic(UiKit.icon("fas-save"));
    }

    private void chooseCertificate() {   //get medical file
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

    // ---------- table ----------

    private static TableColumn<Map<String, Object>, String> column(String title, String key, double width) {
        TableColumn<Map<String, Object>, String> col = UiKit.column(title, row -> text(row.get(key)));
        col.setPrefWidth(width);
        return col;
    }

    private VBox buildTable() {
        TableView<Map<String, Object>> table = new TableView<>(records);
        table.setRowFactory(view -> {
            TableRow<Map<String, Object>> row = new TableRow<>();
            row.setOnMouseClicked(event -> {
                if (!row.isEmpty() && event.getClickCount() == 1) editRecord(row.getItem());
            });
            return row;
        });
        table.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);
        table.setFixedCellSize(48);
        table.setPrefHeight(48 * 8 + 40);
        table.setMinWidth(1200);
        table.setPlaceholder(new Label("No medical records yet."));

        TableColumn<Map<String, Object>, String> statusCol = column("Status", "approval_status", 95);
        statusCol.setCellFactory(UiKit.pillCells());

        TableColumn<Map<String, Object>, Map<String, Object>> certificateCol = new TableColumn<>("Certificate");
        certificateCol.setPrefWidth(145);
        certificateCol.setCellValueFactory(d -> new ReadOnlyObjectWrapper<>(d.getValue()));
        certificateCol.setCellFactory(col -> new TableCell<>() {
            private final Button view = UiKit.ghostButton("View certificate", "fas-file-pdf");
            {
                view.setMinWidth(125);
                view.setOnAction(e -> openCertificate(text(getItem().get("document"))));
            }
            @Override
            protected void updateItem(Map<String, Object> item, boolean empty) {
                super.updateItem(item, empty);
                view.setDisable(empty || item == null || text(item.get("document")).isBlank());
                setGraphic(empty || item == null ? null : view);
            }
        });

        TableColumn<Map<String, Object>, Map<String, Object>> actionsCol = new TableColumn<>("Actions");
        actionsCol.setPrefWidth(100);
        actionsCol.setCellValueFactory(d -> new ReadOnlyObjectWrapper<>(d.getValue()));
        actionsCol.setCellFactory(col -> new TableCell<>() {
            private final Button delete = UiKit.ghostButton("Delete", "fas-trash");
            {
                delete.getStyleClass().add("action-delete");
                delete.setOnAction(e -> deleteRecord(getItem()));
            }
            @Override
            protected void updateItem(Map<String, Object> item, boolean empty) {
                super.updateItem(item, empty);
                setGraphic(empty || item == null ? null : delete);
            }
        });

        table.getColumns().addAll(
                column("Reg no", "reg_no", 90),
                column("Name", "full_name", 170),
                column("Course", "course_code", 100),
                column("Start date", "start_date", 105),
                column("End date", "end_date", 105),
                column("Reason", "reason", 170),
                statusCol, certificateCol, actionsCol);

        VBox card = UiKit.card(UiKit.cardTitle("Student submitted medicals"), table);
        HBox.setHgrow(card, Priority.ALWAYS);
        card.setMaxWidth(Double.MAX_VALUE);
        return card;
    }

    private void openCertificate(String path) {
        File file = new File(path);
        try {
            if (!file.isFile()) throw new Exception("The certificate cannot be found:\n" + file.getAbsolutePath());
            if (!Desktop.isDesktopSupported()) throw new Exception("This computer cannot open files automatically.");
            Desktop.getDesktop().open(file);
        } catch (Exception ex) {
            UiKit.showError("Could not open the medical certificate.", ex);
        }
    }

    private void deleteRecord(Map<String, Object> record) {
        try {
            TechnicianDAO.deleteMedical(toInt(record.get("medical_id")));
            reload();
        } catch (Exception e) {
            UiKit.showError("Could not delete the medical record.", e);
        }
    }
}
