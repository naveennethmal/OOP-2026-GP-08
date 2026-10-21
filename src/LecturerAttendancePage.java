import javafx.collections.FXCollections;
import javafx.collections.transformation.FilteredList;
import javafx.geometry.Insets;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.*;

import java.util.List;

public class LecturerAttendancePage extends VBox {

    private final AttendanceDAO dao = new AttendanceDAO();
    private final ComboBox<CourseItem> courseBox = new ComboBox<>();
    private final ComboBox<String> componentBox = new ComboBox<>();
    private final TextField search = new TextField();
    private final ComboBox<String> statusBox = new ComboBox<>();
    private final TableView<AttendanceSummary> table = new TableView<>();
    private final TableView<SessionRecord> detail = new TableView<>();
    private final TableView<MedicalRecord> medicalTable = new TableView<>();
    private final Label summary = new Label();
    private final Label detailTitle = new Label("Select a student to see session-by-session attendance");
    private FilteredList<AttendanceSummary> filtered;
    private String courseType = "";
    private boolean loading = false;

    public LecturerAttendancePage(Lecturer lecturer) {
        setSpacing(10);

        Label title = new Label("Attendance & Medical");
        title.setStyle("-fx-font-size: 24px; -fx-font-weight: bold;");

        courseBox.setPromptText("Select course");
        courseBox.setPrefWidth(320);
        componentBox.setPrefWidth(140);
        componentBox.setDisable(true);
        search.setPromptText("Search name or reg no...");
        search.setPrefWidth(240);
        statusBox.getItems().addAll("All", "Eligible", "Not Eligible");
        statusBox.setValue("All");
        Button clear = new Button("Clear");
        clear.setOnAction(e -> { search.clear(); statusBox.setValue("All"); });

        HBox row1 = new HBox(12, courseBox, componentBox);
        HBox row2 = new HBox(12, search, statusBox, clear);
        summary.setStyle("-fx-font-weight: bold;");

        // Batch summary table
        TableColumn<AttendanceSummary, String> statusCol = col("Status", "status", 110);
        statusCol.setCellFactory(c -> new TableCell<AttendanceSummary, String>() {
            @Override
            protected void updateItem(String s, boolean empty) {
                super.updateItem(s, empty);
                if (empty || s == null) { setText(null); setStyle(""); return; }
                setText(s);
                setStyle(s.equals("Eligible")
                        ? "-fx-text-fill: #1a9c5b; -fx-font-weight: bold;"
                        : "-fx-text-fill: #d93636; -fx-font-weight: bold;");
            }
        });
        table.getColumns().addAll(
                this.<AttendanceSummary>col("Reg No", "regNo", 120),
                this.<AttendanceSummary>col("Name", "fullName", 210),
                this.<AttendanceSummary>col("Attended", "sessionsText", 100),
                this.<AttendanceSummary>col("Absent", "absentText", 80),
                this.<AttendanceSummary>col("Approved Medicals", "medicalText", 140),
                this.<AttendanceSummary>col("Attendance %", "rawPercentText", 120),
                this.<AttendanceSummary>col("With Medicals %", "finalPercentText", 130),
                statusCol);
        table.setPlaceholder(new Label("Select a course"));
        VBox.setVgrow(table, Priority.ALWAYS);

        // Individual session table
        detailTitle.setStyle("-fx-font-size: 16px; -fx-font-weight: bold;");
        detail.getColumns().addAll(
                this.<SessionRecord>col("Date", "date", 120),
                this.<SessionRecord>col("Session No", "sessionNo", 100),
                this.<SessionRecord>col("Type", "type", 110),
                this.<SessionRecord>col("Status", "status", 100),
                this.<SessionRecord>col("Medical", "medical", 110));
        detail.setPrefHeight(190);
        detail.setPlaceholder(new Label("No student selected"));

        VBox attendanceTab = new VBox(10, row1, summary, row2, table, detailTitle, detail);
        attendanceTab.setPadding(new Insets(12, 0, 0, 0));

        // Medical records tab
        medicalTable.getColumns().addAll(
                this.<MedicalRecord>col("Reg No", "regNo", 120),
                this.<MedicalRecord>col("Name", "fullName", 210),
                this.<MedicalRecord>col("Medical Date", "date", 120),
                this.<MedicalRecord>col("Reason", "reason", 300),
                this.<MedicalRecord>col("Status", "status", 110));
        medicalTable.setPlaceholder(new Label("No medical records for this course"));
        VBox medicalTab = new VBox(10, medicalTable);
        medicalTab.setPadding(new Insets(12, 0, 0, 0));
        VBox.setVgrow(medicalTable, Priority.ALWAYS);

        TabPane tabs = new TabPane(
                new Tab("Attendance", attendanceTab),
                new Tab("Medical Records", medicalTab));
        tabs.setTabClosingPolicy(TabPane.TabClosingPolicy.UNAVAILABLE);
        VBox.setVgrow(tabs, Priority.ALWAYS);

        getChildren().addAll(title, tabs);

        // Load this lecturer's courses
        try {
            courseBox.setItems(FXCollections.observableArrayList(
                    new MarkDAO().getCourses(lecturer.getLecturerId())));
        } catch (Exception ex) {
            showError("Could not load courses: " + ex.getMessage());
        }

        courseBox.valueProperty().addListener((o, a, c) -> onCourseChanged(c));
        componentBox.valueProperty().addListener((o, a, b) -> { if (!loading) refresh(); });
        search.textProperty().addListener((o, a, b) -> applyFilter());
        statusBox.valueProperty().addListener((o, a, b) -> applyFilter());
        table.getSelectionModel().selectedItemProperty().addListener((o, a, s) -> showDetail(s));
    }

    private void onCourseChanged(CourseItem c) {
        detail.setItems(FXCollections.observableArrayList());
        medicalTable.setItems(FXCollections.observableArrayList());
        if (c == null) {
            table.setItems(FXCollections.observableArrayList());
            summary.setText("");
            componentBox.setDisable(true);
            return;
        }
        try {
            courseType = dao.getCourseType(c.getCode());

            loading = true;   // stop the component listener while we refill it
            componentBox.getItems().clear();
            if ("Theory".equals(courseType)) {
                componentBox.getItems().add("Theory");
            } else if ("Practical".equals(courseType)) {
                componentBox.getItems().add("Practical");
            } else {
                componentBox.getItems().addAll("Combined", "Theory", "Practical");
            }
            componentBox.setValue(componentBox.getItems().get(0));
            componentBox.setDisable(componentBox.getItems().size() == 1);
            loading = false;

            medicalTable.setItems(FXCollections.observableArrayList(dao.getMedicals(c.getCode())));
            refresh();
        } catch (Exception ex) {
            loading = false;
            showError("Could not load course: " + ex.getMessage());
        }
    }

    private int componentCount(String comp) {
        boolean theory = courseType.contains("Theory");
        boolean practical = courseType.contains("Practical");
        return switch (comp) {
            case "Theory" -> theory ? 1 : 0;
            case "Practical" -> practical ? 1 : 0;
            default -> (theory ? 1 : 0) + (practical ? 1 : 0);
        };
    }

    private void refresh() {
        CourseItem course = courseBox.getValue();
        String comp = componentBox.getValue();
        detail.setItems(FXCollections.observableArrayList());
        detailTitle.setText("Select a student to see session-by-session attendance");
        if (course == null || comp == null) return;
        try {
            int total = AttendanceDAO.SESSIONS_PER_COMPONENT * componentCount(comp);
            List<AttendanceSummary> all = dao.getBatchSummary(course.getCode(), comp, total);
            filtered = new FilteredList<>(FXCollections.observableArrayList(all), r -> true);
            table.setItems(filtered);
            table.setPlaceholder(new Label("No students enrolled in this course"));

            long ok = all.stream().filter(AttendanceSummary::isEligible).count();
            summary.setStyle("-fx-font-weight: bold;");
            summary.setText("Students: " + all.size() + "   |   Eligible: " + ok
                    + "   |   Not Eligible: " + (all.size() - ok)
                    + "   |   Sessions: " + total + " (" + comp + ")"
                    + "   |   Pass mark: " + AttendanceSummary.PASS_PERCENTAGE + "%");
            applyFilter();
        } catch (Exception ex) {
            showError("Could not load attendance: " + ex.getMessage());
        }
    }

    private void applyFilter() {
        if (filtered == null) return;
        String text = search.getText() == null ? "" : search.getText().toLowerCase().trim();
        String status = statusBox.getValue();
        filtered.setPredicate(r ->
                (text.isEmpty()
                        || r.getFullName().toLowerCase().contains(text)
                        || r.getRegNo().toLowerCase().contains(text))
                        && (status == null || status.equals("All") || r.getStatus().equals(status)));
    }

    private void showDetail(AttendanceSummary s) {
        CourseItem c = courseBox.getValue();
        String comp = componentBox.getValue();
        if (s == null || c == null || comp == null) {
            detail.setItems(FXCollections.observableArrayList());
            detailTitle.setText("Select a student to see session-by-session attendance");
            return;
        }
        try {
            detail.setItems(FXCollections.observableArrayList(
                    dao.getSessions(s.getRegNo(), c.getCode(), comp)));
            detailTitle.setText(s.getFullName() + " (" + s.getRegNo() + ")  -  "
                    + s.getRawPercentText() + "  ->  " + s.getFinalPercentText()
                    + " with medicals  ->  " + s.getStatus());
        } catch (Exception ex) {
            showError("Could not load sessions: " + ex.getMessage());
        }
    }

    private void showError(String text) {
        summary.setStyle("-fx-text-fill: red;");
        summary.setText(text);
    }

    private <S> TableColumn<S, String> col(String header, String property, double width) {
        TableColumn<S, String> c = new TableColumn<>(header);
        c.setCellValueFactory(new PropertyValueFactory<>(property));
        c.setPrefWidth(width);
        return c;
    }
}