package Lecturer;

import Lecturer.Academic.Attendance;
import Lecturer.LecturerDAO.Course;
import Lecturer.LecturerDAO.Medical;
import Lecturer.LecturerDAO.SessionRow;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.scene.control.*;
import javafx.scene.layout.*;

import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.HashMap;
import java.util.Map;

import static Lecturer.LecturerUi.*;

/** Attendance of every student of a course, session by session, and the medicals of that course. */
public class AttendanceView extends VBox {

    private final ComboBox<Course> courseBox = new ComboBox<>();
    private final ComboBox<String> batchBox = new ComboBox<>();
    private final Label summary = new Label();

    private final ObservableList<Attendance> data = FXCollections.observableArrayList();
    private final TextField search = searchField("Search name or reg no...");
    private final ComboBox<String> status = statusBox("All", "Eligible", "Not Eligible");
    private final TableView<Attendance> table = table("Select a course", 300);
    private final TableView<SessionRow> detail = table("No student selected", 190);
    private final Label detailTitle = new Label("Select a student to see session by session attendance");
    private final ObservableList<Medical> medicalData = FXCollections.observableArrayList();
    private final TextField medicalSearch = searchField("Search by name, reg no, or reason...");
    private final ComboBox<String> medicalStatus = statusBox("All", "Pending", "Approved", "Rejected");
    private final TableView<Medical> medicalTable = table("Select a course", 380);
    private final Map<String, String> batchesByStudent = new HashMap<>();

    private String selectedComponent;

    public AttendanceView() {
        getStyleClass().add("page");
        setSpacing(18);
        getChildren().add(header("Attendance & medical", "Attendance of your students and their medical records"));

        courseBox.setPromptText("Select course");
        courseBox.setMinWidth(380);
        courseBox.setPrefWidth(420);
        batchBox.setPromptText("Select batch");
        batchBox.setMinWidth(260);
        batchBox.setPrefWidth(280);
        batchBox.setEditable(false);
        search.setPromptText("Search by name or registration number");
        search.setMinWidth(360);
        search.setPrefWidth(420);
        medicalSearch.setMinWidth(360);
        medicalSearch.setPrefWidth(420);

        table.getColumns().add(col("Reg No", Attendance::regNo, 110));
        table.getColumns().add(col("Name", Attendance::name, 200));
        table.getColumns().add(col("Level", Attendance::level, 70));
        table.getColumns().add(col("Semester", Attendance::semester, 85));
        table.getColumns().add(col("Attended", Attendance::sessionsText, 90));
        table.getColumns().add(col("Absent", a -> String.valueOf(a.absent()), 70));
        table.getColumns().add(col("Medicals", a -> String.valueOf(a.covered()), 80));
        table.getColumns().add(col("Attendance %", Attendance::rawText, 110));
        table.getColumns().add(col("With medicals %", Attendance::finalText, 120));
        table.getColumns().add(pillCol("Status", Attendance::status, 110));
        table.setItems(searchable(data, search, a -> a.name() + " " + a.regNo(),
                new Filter<Attendance>(status, Attendance::status)));

        detail.getColumns().add(col("Date", SessionRow::date, 110));
        detail.getColumns().add(col("Session no", SessionRow::no, 90));
        detail.getColumns().add(col("Type", SessionRow::type, 100));
        detail.getColumns().add(pillCol("Status", SessionRow::status, 100));
        detail.getColumns().add(pillCol("Medical", SessionRow::medical, 110));
        detailTitle.getStyleClass().add("card-title");

        medicalTable.getColumns().add(col("Reg No", Medical::regNo, 120));
        medicalTable.getColumns().add(col("Name", Medical::name, 220));
        medicalTable.getColumns().add(col("Medical date", Medical::date, 130));
        medicalTable.getColumns().add(col("Reason", Medical::reason, 400));
        TableColumn<Medical, Medical> action = new TableColumn<>("View");
        action.setPrefWidth(120);
        action.setCellValueFactory(d -> new javafx.beans.property.ReadOnlyObjectWrapper<>(d.getValue()));
        action.setCellFactory(col -> new TableCell<>() {
            private final Button view = ghost("View");
            {
                view.setMinWidth(90);
                view.setPrefWidth(90);
                view.setOnAction(e -> openMedicalReport(getItem()));
                setAlignment(javafx.geometry.Pos.CENTER);
            }
            @Override
            protected void updateItem(Medical item, boolean empty) {
                super.updateItem(item, empty);
                boolean available = !empty && item != null && item.document() != null && !item.document().isBlank();
                view.setDisable(!available);
                setGraphic(empty || item == null ? null : view);
                setText(null);
            }
        });
        medicalTable.getColumns().add(action);
        medicalTable.getColumns().add(pillCol("Status", Medical::status, 120));
        medicalTable.setPrefWidth(1120);
        medicalTable.setItems(searchable(medicalData, medicalSearch,
                m -> m.name() + " " + m.regNo() + " " + m.reason() + " " + m.date(),
                new Filter<Medical>(medicalStatus, Medical::status)));

        VBox attendanceTab = new VBox(12, new HBox(12, field("Search", search), field("Attendance status", status)),
                card(table), card(detailTitle, detail));
        VBox medicalTab = new VBox(12,
                new HBox(12, field("Search", medicalSearch), field("Medical status", medicalStatus)), card(medicalTable));
        TabPane tabs = new TabPane(new Tab("Attendance", attendanceTab), new Tab("Medical records", medicalTab));
        tabs.setTabClosingPolicy(TabPane.TabClosingPolicy.UNAVAILABLE);

        getChildren().addAll(new HBox(12, field("Course", courseBox), field("Batch", batchBox)), summary, tabs);

        try {
            courseBox.setItems(FXCollections.observableArrayList(LecturerDAO.courses()));
        } catch (Exception e) {
            say(summary, "Could not load your courses: " + e.getMessage(), false);
        }
        batchBox.getItems().add("All batches");
        batchBox.setValue("All batches");
        try {
            batchBox.getItems().addAll(LecturerDAO.batches());
            for (LecturerDAO.Student student : LecturerDAO.students()) {
                batchesByStudent.put(student.regNo(), student.batch());
            }
        } catch (Exception e) {
            say(summary, "Could not load batches: " + e.getMessage(), false);
        }
        courseBox.valueProperty().addListener((o, a, c) -> onCourse(c));
        batchBox.valueProperty().addListener((o, a, b) -> {
            if (courseBox.getValue() != null) {
                refresh();
                loadMedicals(courseBox.getValue());
            }
        });
        table.getSelectionModel().selectedItemProperty().addListener((o, a, s) -> showSessions(s));
    }

    private void onCourse(Course c) {
        data.clear();
        detail.getItems().clear();
        medicalData.clear();
        summary.setText("");
        if (c == null) {
            selectedComponent = null;
            return;
        }
        selectedComponent = c.type().equalsIgnoreCase("Theory") ? "Theory"
                : c.type().equalsIgnoreCase("Practical") ? "Practical" : "Combined";

        loadMedicals(c);
        refresh();
    }

    private void loadMedicals(Course c) {
        try {
            List<Medical> all = LecturerDAO.medicals(c.code());
            medicalData.setAll(all.stream().filter(m -> matchesBatch(m.regNo())).toList());
            medicalTable.setPlaceholder(new Label("No medical records for this course and batch"));
        } catch (Exception e) {
            say(summary, "Could not load medicals: " + e.getMessage(), false);
        }
    }

    private boolean matchesBatch(String regNo) {
        String selected = batchBox.getValue();
        return selected == null || selected.equals("All batches")
                || selected.equals(batchesByStudent.get(regNo));
    }

    private void refresh() {
        Course c = courseBox.getValue();
        String component = selectedComponent;
        detail.getItems().clear();
        detailTitle.setText("Select a student to see session by session attendance");
        if (c == null || component == null) return;
        try {
            int total = Academic.sessionsFor(c.type(), component);
            List<Attendance> all = LecturerDAO.attendance(c.code(), component, total);
            data.setAll(all.stream().filter(a -> matchesBatch(a.regNo())).toList());
            table.setPlaceholder(new Label("No students are enrolled in this course"));
            long ok = data.stream().filter(Attendance::eligible).count();
            say(summary, "Students: " + data.size() + "   |   Eligible: " + ok + "   |   Not eligible: "
                    + (data.size() - ok) + "   |   Sessions: " + total + " (" + component + ")", true);
        } catch (Exception e) {
            say(summary, "Could not load attendance: " + e.getMessage(), false);
        }
    }

    private void openMedicalReport(Medical record) {
        if (record == null || record.document() == null || record.document().isBlank()) {
            new Alert(Alert.AlertType.INFORMATION, "No medical report was uploaded for this student.").show();
            return;
        }
        File file = new File(record.document());
        if (!file.isFile()) {
            new Alert(Alert.AlertType.ERROR, "The submitted medical report could not be found:\n" + file.getAbsolutePath()).show();
            return;
        }
        try {
            if (java.awt.Desktop.isDesktopSupported() && java.awt.Desktop.getDesktop().isSupported(java.awt.Desktop.Action.OPEN)) {
                java.awt.Desktop.getDesktop().open(file);
            } else {
                new Alert(Alert.AlertType.ERROR, "This computer cannot open medical reports automatically.").show();
            }
        } catch (IOException ex) {
            new Alert(Alert.AlertType.ERROR, "Could not open the medical report:\n" + ex.getMessage()).show();
        }
    }

    private void showSessions(Attendance s) {
        Course c = courseBox.getValue();
        String component = selectedComponent;
        if (s == null || c == null || component == null) {
            detail.getItems().clear();
            detailTitle.setText("Select a student to see session by session attendance");
            return;
        }
        try {
            detail.setItems(FXCollections.observableArrayList(LecturerDAO.sessions(s.regNo(), c.code(), component)));
            detailTitle.setText(s.name() + " (" + s.regNo() + ")   " + s.rawText() + "  →  " + s.finalText()
                    + " with medicals  →  " + s.status());
        } catch (Exception e) {
            say(summary, "Could not load the sessions: " + e.getMessage(), false);
        }
    }
}
