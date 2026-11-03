package Technical_Officer;

import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.control.cell.ComboBoxTableCell;
import javafx.scene.layout.*;
import javafx.util.StringConverter;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;

import static Technical_Officer.TechnicianDAO.codeOf;
import static Technical_Officer.TechnicianDAO.text;
import static Technical_Officer.TechnicianDAO.toInt;


public class AttendanceView extends VBox {

   //give one student row
    private static class Row {
        final String regNo, name, medical;          // medical: None / Pending / Approved
        final int attended, total;
        final StringProperty session;               // this session: Present / Absent / Medical

        Row(Map<String, Object> data) {   //<reg no: tg2116>
            regNo = text(data.get("reg_no"));
            name = text(data.get("full_name"));
            medical = text(data.get("medical_status"));
            attended = toInt(data.get("attended"));
            total = toInt(data.get("total"));
            String current = text(data.get("current_status"));
            session = new SimpleStringProperty(current.isEmpty() ? "Absent" : current);
        }

        double rate() {
            return total == 0 ? 0 : attended * 100.0 / total;
        }

        String eligibility() {
            if (total == 0) return "No sessions";
            if (rate() >= 80) return "Eligible";
            return switch (medical) {
                case "Approved" -> "Eligible (medical)";
                case "Pending" -> "Medical pending";
                default -> "Not eligible";
            };
        }
    }

    private static final String[] STATUSES = {"Present", "Absent", "Medical"};

    private final ObservableList<Row> rows = FXCollections.observableArrayList();  //take data list as rows
    private final TableView<Row> table = new TableView<>(rows);                    //show list as table
    private final Label tableTitle = UiKit.cardTitle("Attendance");
    private final Label message = new Label();
    private final VBox summary = new VBox();
    private Row selectedStudent;   //one row as selected student

    private final ComboBox<String> course = new ComboBox<>(); //drop down
    private final ComboBox<String> level = new ComboBox<>();
    private final ComboBox<String> semester = new ComboBox<>();
    private final ComboBox<String> session = new ComboBox<>();
    private final ComboBox<String> sessionStatus = new ComboBox<>(FXCollections.observableArrayList(STATUSES));
    private final DatePicker date = new DatePicker(LocalDate.now());

    public AttendanceView() {
        getStyleClass().add("page");  //css part
        setSpacing(20);
        message.getStyleClass().add("form-error"); //for error

        Button save = UiKit.primaryButton("Save attendance", "fas-save");
        save.setOnAction(e -> saveAttendance());

        getChildren().addAll(        //using vbox add header ad details to page
                UiKit.pageHeader("Attendance", "Record and review undergraduate attendance", save),
                buildFilters(),
                summary,
                buildTable());

        reload();
    }

    //set data to header line

    private VBox buildFilters() {
        for (ComboBox<String> combo : List.of(course, level, semester, session, sessionStatus)) {
            configureCombo(combo);
        }
        configureDatePicker();

        try {
            course.getItems().setAll(TechnicianDAO.courses());  //get courses from databse into course combo box
        } catch (Exception e) {
            message.setText("Could not load courses: " + e.getMessage());
        }
        level.getItems().setAll(TechnicianDAO.LEVELS);
        semester.getItems().setAll(TechnicianDAO.SEMESTERS);
        for (int i = 1; i <= TechnicianDAO.MAX_SESSIONS; i++) session.getItems().add(String.format("Session %02d", i));

        if (!course.getItems().isEmpty()) course.setValue(course.getItems().get(0)); //show data at first started time
        level.setValue(level.getItems().get(0));
        semester.setValue(semester.getItems().get(0));
        session.setValue(session.getItems().get(0));
        sessionStatus.setValue("Present");

        // the "This session" box changes the selected student's status in header
        sessionStatus.valueProperty().addListener((o, oldValue, newValue) -> {
            if (selectedStudent != null && newValue != null) selectedStudent.session.set(newValue);
        });

        // reload from the database whenever a filter changes
        session.valueProperty().addListener((o, a, b) -> { loadSessionDate(); reload(); });
        course.valueProperty().addListener((o, a, b) -> { loadSessionDate(); reload(); });
        level.valueProperty().addListener((o, a, b) -> reload());
        semester.valueProperty().addListener((o, a, b) -> reload());

        Region spacer = new Region();  //get spacer
        HBox.setHgrow(spacer, Priority.ALWAYS);
        HBox row = new HBox(16,
                UiKit.field("Course", course),
                UiKit.field("Level", level),
                UiKit.field("Semester", semester),
                UiKit.field("Session", session),
                UiKit.field("Date", date),
                UiKit.field("This session", sessionStatus),
                spacer);
        row.setAlignment(Pos.BOTTOM_LEFT);
        setWidth(course, 235);
        setWidth(level, 90);
        setWidth(semester, 100);
        setWidth(session, 115);
        setWidth(sessionStatus, 115);
        return UiKit.card(row);
    }

    private static void setWidth(ComboBox<String> combo, double width) {
        combo.setPrefWidth(width);
        combo.setMinWidth(width);
    }

    // Makes the combo box look like a plain read-only drop-down
    private static void configureCombo(ComboBox<String> combo) {
        combo.setEditable(true);
        combo.getEditor().setEditable(false);
        combo.getEditor().setFocusTraversable(false);
        combo.getEditor().setStyle("-fx-text-fill: #1f2937; -fx-opacity: 1;");
        combo.valueProperty().addListener((obs, oldValue, newValue) ->
                combo.getEditor().setText(newValue == null ? "" : newValue));
    }
         //set date(calender) at headr as "DATE"
    private void configureDatePicker() {
        date.setPrefWidth(145);
        date.setMinWidth(145);
        date.setEditable(false);
        date.setPromptText("DD/MM/YYYY");
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd/MM/yyyy");
        date.setConverter(new StringConverter<>() {
            @Override public String toString(LocalDate value) {
                return value == null ? "" : formatter.format(value);
            }
            @Override public LocalDate fromString(String value) {
                return value == null || value.isBlank() ? null : LocalDate.parse(value, formatter);
            }
        });
    }

    private int sessionNo() {
        return session.getSelectionModel().getSelectedIndex() + 1;
    }

    private String courseCode() {
        return codeOf(course.getValue());
    }

    //Shows the saved date of the selected session
    private void loadSessionDate() {
        if (course.getValue() == null) return;
        try {
            LocalDate saved = TechnicianDAO.sessionDate(courseCode(), sessionNo());
            date.setValue(saved == null ? LocalDate.now() : saved);
        } catch (Exception e) {
            message.setText("Could not load session date: " + e.getMessage());
        }
    }

    // Loads the students of the selected course,level,semester and session
    private void reload() {
        String selectedRegNo = selectedStudent == null ? null : selectedStudent.regNo;
        selectedStudent = null;
        sessionStatus.setValue("Absent");
        message.setText("");

        rows.clear();
        if (course.getValue() != null && level.getValue() != null && semester.getValue() != null) {
            try {
                for (Map<String, Object> data : TechnicianDAO.attendance(
                        courseCode(), level.getValue(), semester.getValue(), sessionNo())) {
                    rows.add(new Row(data));  //get value from databse
                }
                tableTitle.setText("Attendance - " + courseCode() + " - Level " + level.getValue()
                        + " - Semester " + semester.getValue());
                rows.stream().filter(r -> r.regNo.equals(selectedRegNo)).findFirst()
                        .ifPresent(r -> table.getSelectionModel().select(r));
            } catch (Exception e) {
                message.setText("Could not load attendance: " + e.getMessage());
            }
        }
        refreshSummary();
    }

    private void refreshSummary() {
        double avg = rows.stream().filter(r -> r.total > 0).mapToDouble(Row::rate).average().orElse(0);
        long below = rows.stream().filter(r -> r.total > 0 && r.rate() < 80).count();
        long pending = rows.stream().filter(r -> r.medical.equals("Pending")).count();
        summary.getChildren().setAll(UiKit.statRow(
                UiKit.statCard("Class average", String.format("%.0f%%", avg), "fas-chart-line", "green"),
                UiKit.statCard("Below 80%", String.valueOf(below), "fas-exclamation-triangle", "red"),
                UiKit.statCard("Medicals pending", String.valueOf(pending), "fas-notes-medical", "amber")));
    }

    //actions

    private void saveAttendance() {
        if (course.getValue() == null || selectedStudent == null) {
            UiKit.warn("Select one student before saving attendance.");
            return;
        }
        if (date.getValue() == null) {
            UiKit.warn("Select the session date.");
            return;
        }
        try {
            TechnicianDAO.saveAttendance(selectedStudent.regNo, courseCode(), sessionNo(),
                    date.getValue(), selectedStudent.session.get());
            UiKit.info("Attendance saved for " + selectedStudent.name + ".");
            reload();
        } catch (Exception e) {
            UiKit.showError("Could not save attendance.", e);
        }
    }

    private void clearCourseAttendance() {
        if (course.getValue() == null) {
            UiKit.warn("Select a course first.");
            return;
        }
        Alert confirmation = new Alert(Alert.AlertType.CONFIRMATION,
                "Delete all attendance records for " + courseCode() + "?", ButtonType.CANCEL, ButtonType.OK);
        confirmation.setHeaderText("Clear course attendance");
        confirmation.showAndWait().ifPresent(result -> {
            if (result != ButtonType.OK) return;
            try {
                int deleted = TechnicianDAO.clearAttendance(courseCode());
                table.getSelectionModel().clearSelection();
                reload();
                UiKit.info(deleted + " attendance records deleted for " + courseCode() + ".");
            } catch (Exception e) {
                UiKit.showError("Could not clear course attendance.", e);
            }
        });
    }

    // ---------- table ----------

    private VBox buildTable() {
        table.getStyleClass().add("attendance-table");
        table.getSelectionModel().selectedItemProperty().addListener((obs, oldValue, newValue) -> {
            selectedStudent = newValue;
            if (newValue != null) sessionStatus.setValue(newValue.session.get()); //get status selected student
        });
        table.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);
        table.setEditable(true);
        table.setFixedCellSize(48);
        table.setPrefHeight(48 * 9 + 40);
        table.setPlaceholder(new Label("No undergraduates found."));

        TableColumn<Row, Double> rate = new TableColumn<>("Attendance rate");
        rate.setCellValueFactory(d -> new SimpleObjectProperty<>(d.getValue().rate()));
        rate.setCellFactory(col -> new TableCell<>() { //precentage bar
            private final ProgressBar bar = new ProgressBar();
            private final Label percent = new Label();
            private final HBox box = new HBox(10, bar, percent);
            {
                box.setAlignment(Pos.CENTER_LEFT);
                bar.setPrefWidth(90);
            }                                                                //
            @Override
            protected void updateItem(Double v, boolean empty) {        //for j.fx
                super.updateItem(v, empty);
                if (empty || v == null) {
                    setGraphic(null);
                    return;
                }
                bar.setProgress(v / 100.0);
                percent.setText(String.format("%.0f%%", v));    //14.8->15%
                bar.getStyleClass().removeAll("bar-ok", "bar-bad");
                bar.getStyleClass().add(v >= 80 ? "bar-ok" : "bar-bad");
                setGraphic(box);
            }
        });

        TableColumn<Row, String> medical = UiKit.column("Medical", r -> r.medical); //Row eke medicle assign to Medicl colum
        medical.setCellFactory(UiKit.pillCells());
        TableColumn<Row, String> eligible = UiKit.column("Eligibility", Row::eligibility); //method in Row assign to colum
        eligible.setCellFactory(UiKit.pillCells());

        TableColumn<Row, String> thisSession = new TableColumn<>("This session");
        thisSession.setCellValueFactory(d -> d.getValue().session);
        thisSession.setCellFactory(ComboBoxTableCell.forTableColumn(STATUSES));
        thisSession.setOnEditCommit(event -> event.getRowValue().session.set(event.getNewValue()));
        thisSession.setEditable(true);

        table.getColumns().addAll(
                UiKit.<Row>column("Reg no", r -> r.regNo),
                UiKit.<Row>column("Name", r -> r.name),
                UiKit.<Row>column("Sessions", r -> r.attended + " / " + r.total),
                rate, medical, eligible, thisSession);

        Label hint = new Label("Select Present, Absent, or Medical for the selected student, then press Save attendance.");
        hint.getStyleClass().add("hint");
        Button clear = UiKit.ghostButton("Clear course attendance", "fas-trash");
        clear.setOnAction(e -> clearCourseAttendance());
        HBox actions = new HBox(clear);
        actions.setAlignment(Pos.CENTER_RIGHT);

        return UiKit.card(tableTitle, message, table, hint, actions);
    }
}
