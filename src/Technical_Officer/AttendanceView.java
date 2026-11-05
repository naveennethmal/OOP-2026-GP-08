package Technical_Officer;

import Technical_Officer.AttendanceDAO.AttendanceRow;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.control.cell.ComboBoxTableCell;
import javafx.scene.layout.*;

import java.sql.SQLException;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;
import javafx.util.StringConverter;

public class AttendanceView extends VBox {
    private final ObservableList<AttendanceRow> rows = FXCollections.observableArrayList();
    private final Label tableTitle = UiKit.cardTitle("Attendance");
    private final Label status = new Label();
    private final VBox summary = new VBox();
    private TableView<AttendanceRow> attendanceTable;
    private AttendanceRow selectedStudent;

    private final ComboBox<String> course = new ComboBox<>();
    private final ComboBox<String> level = new ComboBox<>();
    private final ComboBox<String> semester = new ComboBox<>();
    private final ComboBox<String> session = new ComboBox<>();
    private final ComboBox<String> sessionStatus = new ComboBox<>(
            FXCollections.observableArrayList("Present", "Absent", "Medical"));
    private final DatePicker date = new DatePicker(LocalDate.now());
    public AttendanceView() {
        getStyleClass().add("page");
        setSpacing(20);
        status.getStyleClass().add("form-error");

        Button save = UiKit.primaryButton("Save attendance", "fas-save");
        save.setOnAction(e -> saveAttendance());

        getChildren().addAll(
                UiKit.pageHeader("Attendance", "Record and review undergraduate attendance", save),
                buildFilters(),
                summary,
                buildTable());

        reload();
    }

    private VBox buildFilters() {
        course.setPrefWidth(260);
        configureCombo(course);
        configureCombo(level);
        configureCombo(semester);
        configureCombo(session);
        configureCombo(sessionStatus);
        configureDatePicker();
        try {
            course.getItems().setAll(AttendanceDAO.courses());
            level.getItems().setAll(AttendanceDAO.levels());
            semester.getItems().setAll(AttendanceDAO.semesters());
        } catch (SQLException e) {
            status.setText("Could not load courses: " + e.getMessage());
            System.err.println(e.getMessage());
        }
        if (!course.getItems().isEmpty()) course.setValue(course.getItems().get(0));
        if (!level.getItems().isEmpty()) level.setValue(level.getItems().get(0));
        if (!semester.getItems().isEmpty()) semester.setValue(semester.getItems().get(0));
        sessionStatus.setValue("Present");
        sessionStatus.valueProperty().addListener((o, oldValue, newValue) -> {
            if (selectedStudent != null && newValue != null) {
                selectedStudent.sessionProperty().set(newValue);
            }
        });
        for (int i = 1; i <= AttendanceDAO.MAX_SESSIONS; i++) session.getItems().add("Session " + String.format("%02d", i));
        session.setValue(session.getItems().get(0));

        // Selecting a session loads its saved date and each student's saved status.
        session.valueProperty().addListener((o, oldValue, newValue) -> {
            if (newValue != null) {
                loadSession();
                reload();
            }
        });

        // reload from the database whenever a filter changes
        course.valueProperty().addListener((o, a, b) -> {
            loadSession();
            reload();
        });
        level.valueProperty().addListener((o, a, b) -> reload());
        semester.valueProperty().addListener((o, a, b) -> reload());

        Region spacer = new Region();
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
        setFilterWidth(course, 235);
        setFilterWidth(level, 90);
        setFilterWidth(semester, 100);
        setFilterWidth(session, 115);
        setFilterWidth(sessionStatus, 115);
        return UiKit.card(row);
    }

    private void configureDatePicker() {
        date.setPrefWidth(145);
        date.setMinWidth(145);
        date.setEditable(false);
        date.setPromptText("DD/MM/YYYY");
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd/MM/yyyy");
        date.setConverter(new StringConverter<>() {
            @Override
            public String toString(LocalDate value) {
                return value == null ? "" : formatter.format(value);
            }

            @Override
            public LocalDate fromString(String value) {
                if (value == null || value.isBlank()) return null;
                return LocalDate.parse(value, formatter);
            }
        });
    }

    private static void setFilterWidth(ComboBox<String> combo, double width) {
        combo.setPrefWidth(width);
        combo.setMinWidth(width);
    }

    private static void configureCombo(ComboBox<String> combo) {
        combo.setEditable(true);
        combo.getEditor().setEditable(false);
        combo.getEditor().setFocusTraversable(false);
        combo.getEditor().setStyle("-fx-text-fill: #1f2937; -fx-opacity: 1;");
        combo.valueProperty().addListener((obs, oldValue, newValue) ->
                combo.getEditor().setText(newValue == null ? "" : newValue));
        combo.getEditor().setText(combo.getValue() == null ? "" : combo.getValue());
    }

    private int sessionNo() {
        return session.getSelectionModel().getSelectedIndex() + 1;
    }

    private String courseCode() {
        return AttendanceDAO.codeOf(course.getValue());
    }

    private void loadSession() {
        if (course.getValue() == null) return;
        try {
            LocalDate savedDate = AttendanceDAO.sessionDate(courseCode(), sessionNo());
            date.setValue(savedDate == null ? LocalDate.now() : savedDate);
        } catch (SQLException e) {
            status.setText("Could not load session date: " + e.getMessage());
        }
    }

    /** Loads the students of the selected course, level, and semester. */
    private void reload() {
        String selectedRegNo = selectedStudent == null ? null : selectedStudent.regNo();
        selectedStudent = null;
        sessionStatus.setValue("Absent");
        status.setText("");
        if (course.getValue() == null || level.getValue() == null || semester.getValue() == null
                ) {
            rows.clear();
        } else {
            try {
                rows.setAll(AttendanceDAO.load(courseCode(), level.getValue(), semester.getValue(),
                        sessionNo()));
                tableTitle.setText("Attendance - " + courseCode() + " - Level " + level.getValue()
                        + " - Semester " + semester.getValue());
                if (attendanceTable != null && !rows.isEmpty()) {
                    AttendanceRow toReselect = rows.stream()
                            .filter(row -> row.regNo().equals(selectedRegNo))
                            .findFirst().orElse(null);
                    if (toReselect != null) attendanceTable.getSelectionModel().select(toReselect);
                }
            } catch (SQLException e) {
                rows.clear();
                status.setText("Could not load attendance: " + e.getMessage());
                System.err.println(e.getMessage());
            }
        }
        refreshSummary();
    }

    private void refreshSummary() {
        double avg = rows.stream().filter(r -> r.total() > 0).mapToDouble(AttendanceRow::rate).average().orElse(0);
        long below = rows.stream().filter(r -> r.total() > 0 && r.rate() < 80).count();
        long pending = rows.stream().filter(r -> r.medical().equals("Pending")).count();
        summary.getChildren().setAll(UiKit.statRow(
                UiKit.statCard("Class average", String.format("%.0f%%", avg), "fas-chart-line", "green"),
                UiKit.statCard("Below 80%", String.valueOf(below), "fas-exclamation-triangle", "red"),
                UiKit.statCard("Medicals pending", String.valueOf(pending), "fas-notes-medical", "amber")));
    }

    private void saveAttendance() {
        if (course.getValue() == null || selectedStudent == null) {
            new Alert(Alert.AlertType.WARNING, "Select one student before saving attendance.").show();
            return;
        }

        if (date.getValue() == null) {
            new Alert(Alert.AlertType.WARNING, "Select the session date.").show();
            return;
        }
        try {
            AttendanceDAO.save(courseCode(), sessionNo(), date.getValue(), List.of(selectedStudent));
            new Alert(Alert.AlertType.INFORMATION,
                    "Attendance saved for " + selectedStudent.name() + ".").show();
            reload();
        } catch (SQLException e) {
            UiKit.showError("Could not save attendance.", e);
        }
    }

    private void clearCourseAttendance() {
        if (course.getValue() == null) {
            new Alert(Alert.AlertType.WARNING, "Select a course first.").show();
            return;
        }
        Alert confirmation = new Alert(Alert.AlertType.CONFIRMATION,
                "Delete all attendance records for " + courseCode() + "?",
                ButtonType.CANCEL, ButtonType.OK);
        confirmation.setHeaderText("Clear course attendance");
        confirmation.showAndWait().ifPresent(result -> {
            if (result != ButtonType.OK) return;
            try {
                int deleted = AttendanceDAO.clear(courseCode());
                selectedStudent = null;
                if (attendanceTable != null) attendanceTable.getSelectionModel().clearSelection();
                reload();
                new Alert(Alert.AlertType.INFORMATION,
                        deleted + " attendance records deleted for " + courseCode() + ".").show();
            } catch (SQLException e) {
                UiKit.showError("Could not clear course attendance.", e);
            }
        });
    }

    private VBox buildTable() {
        TableView<AttendanceRow> table = new TableView<>(rows);
        table.getStyleClass().add("attendance-table");
        attendanceTable = table;
        table.getSelectionModel().selectedItemProperty().addListener((obs, oldValue, newValue) -> {
            selectedStudent = newValue;
            if (newValue != null) {
                sessionStatus.setValue(newValue.sessionProperty().get());
            }
        });
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
                d.getValue().attended() + " / " + d.getValue().total()));

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
        today.setOnEditCommit(event ->
                event.getRowValue().sessionProperty().set(event.getNewValue()));
        today.setEditable(true);

        table.getColumns().addAll(reg, name, sessions, rate, medical, eligible, today);

        Label hint = new Label("Select Present, Absent, or Medical for the selected student, then press Save attendance.");
        hint.getStyleClass().add("hint");
        Button clear = UiKit.ghostButton("Clear course attendance", "fas-trash");
        clear.setOnAction(e -> clearCourseAttendance());
        HBox actions = new HBox(clear);
        actions.setAlignment(Pos.CENTER_RIGHT);

        return UiKit.card(tableTitle, status, table, hint, actions);
    }
}
