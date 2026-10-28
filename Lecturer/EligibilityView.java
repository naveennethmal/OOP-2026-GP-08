package Lecturer;

import Lecturer.Academic.Ca;
import Lecturer.Academic.CaPart;
import Lecturer.Academic.Attendance;
import Lecturer.LecturerDAO.Course;
import Lecturer.LecturerDAO.MarkEntry;
import Lecturer.LecturerDAO.SessionRow;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.scene.control.*;
import javafx.scene.layout.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static Lecturer.LecturerUi.*;

/** Displays per-student attendance and CA eligibility, CA details, and end-exam marks. */
public class EligibilityView extends VBox {

    private final ComboBox<Course> courseBox = new ComboBox<>();
    private final ComboBox<String> batchBox = new ComboBox<>();
    private final Label summary = new Label();

    // Attendance + CA eligibility
    private final ObservableList<EligibilityResult> eligibilityData = FXCollections.observableArrayList();
    private final TextField eligibilitySearch = searchField("Search name or reg no...");
    private final ComboBox<String> eligibilityStatus = statusBox("All", "Eligible", "Not Eligible", "Pending");
    private final TableView<EligibilityResult> eligibilityTable = table("Select a course", 260);
    private final Label eligibilitySummary = new Label();
    private final TableView<SessionRow> eligibilitySessions = table("No student selected", 150);
    private final Label eligibilityDetailTitle = new Label("Select a student to see attendance details");

    // CA tab
    private final ObservableList<Ca> caData = FXCollections.observableArrayList();
    private final TextField caSearch = searchField("Search name or reg no...");
    private final ComboBox<String> caStatus = statusBox("All", "Pass", "EC", "Pending");
    private final TableView<Ca> caTable = table("Select a course", 260);
    private final TableView<CaPart> partTable = table("No student selected", 170);
    private final Label partTitle = new Label("Select a student to see the CA breakdown");

    // end exam tab
    private final ObservableList<EndResult> endData = FXCollections.observableArrayList();
    private final TextField endSearch = searchField("Search by name or registration number");
    private final ComboBox<String> endStatus = statusBox("All", "Pass", "EC", "Pending");
    private final TableView<EndResult> endTable = table("Select a course", 300);

    private record EndResult(String regNo, String name, String batch, Double mark) {
        String markText() { return mark == null ? "-" : String.format("%.2f%%", mark); }
        String status() {
            if (mark == null) return "Pending";
            return mark >= Academic.END_EXAM_PASS ? "Pass" : "EC";
        }
    }

    private record EligibilityResult(Attendance attendance, Ca ca, String batch) {
        String attendanceText() {
            return attendance.finalText() + " (" + attendance.withMedical() + "/" + attendance.total() + ")";
        }
        String caText() { return ca == null ? "-" : ca.caText(); }
        String caPercentText() { return ca == null ? "-" : ca.percentText(); }
        String caProgressText() { return ca == null ? "0 / 4" : ca.enteredText(); }
        String status() {
            if (!attendance.eligible()) return "Not Eligible";
            if (ca == null || ca.status().equals("Pending")) return "Pending";
            return ca.eligible() ? "Eligible" : "Not Eligible";
        }
    }

    public EligibilityView() {
        getStyleClass().add("page");
        setSpacing(18);
        eligibilitySessions.setPrefHeight(360);
        eligibilitySessions.setMinHeight(360);
        getChildren().add(header("CA & eligibility",
                "Check each student's attendance and CA eligibility, or review totals for the selected batch."));

        courseBox.setPromptText("Select course");
        courseBox.setMinWidth(380);
        courseBox.setPrefWidth(420);
        batchBox.setPromptText("Select batch");
        batchBox.setMinWidth(260);
        batchBox.setPrefWidth(280);
        batchBox.setEditable(false);
        caSearch.setPromptText("Search by name or registration number");
        caSearch.setMinWidth(360);
        caSearch.setPrefWidth(420);
        endSearch.setMinWidth(360);
        endSearch.setPrefWidth(420);
        eligibilitySearch.setMinWidth(360);
        eligibilitySearch.setPrefWidth(420);

        TabPane tabs = new TabPane(new Tab("Attendance + CA", eligibilityTab()),
                new Tab("CA marks", caTab()), new Tab("End", endTab()));
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
        } catch (Exception e) {
            say(summary, "Could not load batches: " + e.getMessage(), false);
        }
        courseBox.valueProperty().addListener((o, a, c) -> load(c));
        batchBox.valueProperty().addListener((o, a, b) -> load(courseBox.getValue()));
    }

    private VBox eligibilityTab() {
        eligibilityTable.getColumns().add(col("Reg No", r -> r.attendance().regNo(), 120));
        eligibilityTable.getColumns().add(col("Name", r -> r.attendance().name(), 200));
        eligibilityTable.getColumns().add(col("Batch", EligibilityResult::batch, 120));
        eligibilityTable.getColumns().add(col("Attendance", EligibilityResult::attendanceText, 150));
        eligibilityTable.getColumns().add(col("CA marks", EligibilityResult::caText, 130));
        eligibilityTable.getColumns().add(col("CA %", EligibilityResult::caPercentText, 90));
        eligibilityTable.getColumns().add(col("Marks entered", EligibilityResult::caProgressText, 120));
        eligibilityTable.getColumns().add(pillCol("Eligibility", EligibilityResult::status, 130));
        eligibilityTable.setItems(searchable(eligibilityData, eligibilitySearch,
                r -> r.attendance().name() + " " + r.attendance().regNo(),
                new Filter<EligibilityResult>(eligibilityStatus, EligibilityResult::status)));

        eligibilitySessions.getColumns().add(col("Date", SessionRow::date, 110));
        eligibilitySessions.getColumns().add(col("Session", SessionRow::no, 90));
        eligibilitySessions.getColumns().add(col("Type", SessionRow::type, 100));
        eligibilitySessions.getColumns().add(pillCol("Status", SessionRow::status, 100));
        eligibilitySessions.getColumns().add(pillCol("Medical", SessionRow::medical, 110));

        eligibilityDetailTitle.getStyleClass().add("card-title");
        eligibilityTable.getSelectionModel().selectedItemProperty().addListener((obs, oldValue, selected) ->
                showEligibilityDetails(selected));

        VBox details = new VBox(12,
                card(eligibilityDetailTitle),
                card(new VBox(8, new Label("Attendance sessions"), eligibilitySessions)));
        return new VBox(12,
                new HBox(12, field("Search", eligibilitySearch), field("Eligibility status", eligibilityStatus)),
                eligibilitySummary, card(eligibilityTable), details);
    }

    private VBox caTab() {
        caTable.getColumns().add(col("Reg No", Ca::regNo, 120));
        caTable.getColumns().add(col("Name", Ca::name, 220));
        caTable.getColumns().add(col("CA marks", Ca::caText, 130));
        caTable.getColumns().add(col("CA %", Ca::percentText, 90));
        caTable.getColumns().add(col("Marks entered", Ca::enteredText, 120));
        caTable.getColumns().add(pillCol("Status", Ca::status, 120));
        caTable.setItems(searchable(caData, caSearch, c -> c.name() + " " + c.regNo(),
                new Filter<Ca>(caStatus, Ca::status)));

        partTable.getColumns().add(col("Exam", CaPart::name, 240));
        partTable.getColumns().add(col("Type", CaPart::type, 110));
        partTable.getColumns().add(col("Weight", CaPart::weightText, 90));
        partTable.getColumns().add(col("Mark (/100)", CaPart::markText, 110));
        partTable.getColumns().add(col("Contribution", CaPart::contributionText, 110));
        partTitle.getStyleClass().add("card-title");

        caTable.getSelectionModel().selectedItemProperty().addListener((obs, a, c) -> {
            if (c == null) {
                partTable.getItems().clear();
                partTitle.setText("Select a student to see the CA breakdown");
                return;
            }
            partTable.setItems(FXCollections.observableArrayList(c.parts()));
            partTitle.setText("CA breakdown - " + c.name() + " (" + c.regNo() + ")   "
                    + c.caText() + " = " + c.percentText() + "  →  " + c.status());
        });

        return new VBox(12, new HBox(12, field("Search", caSearch), field("CA status", caStatus)),
                card(caTable), card(partTitle, partTable));
    }

    private VBox endTab() {
        endTable.getColumns().add(col("Reg No", EndResult::regNo, 140));
        endTable.getColumns().add(col("Name", EndResult::name, 260));
        endTable.getColumns().add(col("Batch", EndResult::batch, 160));
        endTable.getColumns().add(col("End exam marks (%)", EndResult::markText, 180));
        endTable.getColumns().add(pillCol("Status", EndResult::status, 140));
        endTable.setItems(searchable(endData, endSearch,
                e -> e.name() + " " + e.regNo(), new Filter<EndResult>(endStatus, EndResult::status)));
        return new VBox(12, new HBox(12, field("Search", endSearch), field("Status", endStatus)), card(endTable));
    }

    private void load(Course course) {
        caData.clear();
        endData.clear();
        eligibilityData.clear();
        partTable.getItems().clear();
        eligibilitySessions.getItems().clear();
        eligibilityDetailTitle.setText("Select a student to see attendance details");
        summary.setText("");
        eligibilitySummary.setText("");
        if (course == null) return;
        try {
            List<Ca> ca = LecturerDAO.ca(course.code());
            List<MarkEntry> students = LecturerDAO.courseStudents(course.code());
            Map<String, String> batchesByStudent = new HashMap<>();
            for (MarkEntry student : students) batchesByStudent.put(student.regNo(), student.batch());
            String selectedBatch = batchBox.getValue();
            java.util.function.Predicate<String> batchMatches = regNo -> selectedBatch == null
                    || selectedBatch.equals("All batches")
                    || selectedBatch.equals(batchesByStudent.get(regNo));

            caData.setAll(ca.stream().filter(c -> batchMatches.test(c.regNo())).toList());

            Map<String, Ca> caByStudent = new HashMap<>();
            for (Ca result : ca) caByStudent.put(result.regNo(), result);
            String component = course.type().equalsIgnoreCase("Theory") ? "Theory"
                    : course.type().equalsIgnoreCase("Practical") ? "Practical" : "Combined";
            int totalSessions = Academic.sessionsFor(course.type(), component);
            List<Attendance> attendance = LecturerDAO.eligibilityAttendance(course.code(), component, totalSessions);
            for (Attendance result : attendance) {
                if (batchMatches.test(result.regNo())) {
                    eligibilityData.add(new EligibilityResult(result, caByStudent.get(result.regNo()),
                            batchesByStudent.getOrDefault(result.regNo(), "")));
                }
            }

            Map<String, Double> endMarks = LecturerDAO.endExamMarks(course.code());
            for (MarkEntry student : students) {
                if (batchMatches.test(student.regNo())) endData.add(new EndResult(student.regNo(), student.name(),
                        batchesByStudent.getOrDefault(student.regNo(), ""), endMarks.get(student.regNo())));
            }

            say(summary, "Students in selected course and batch: " + endData.size(), true);
            long eligible = eligibilityData.stream().filter(r -> r.status().equals("Eligible")).count();
            long pending = eligibilityData.stream().filter(r -> r.status().equals("Pending")).count();
            long notEligible = eligibilityData.size() - eligible - pending;
            say(eligibilitySummary, "Batch totals: " + eligibilityData.size() + " students   |   Eligible: "
                    + eligible + "   |   Not eligible: " + notEligible + "   |   Pending: " + pending, true);
            eligibilityTable.setPlaceholder(new Label("No students are enrolled in this course and batch"));
            if (ca.isEmpty()) {
                caTable.setPlaceholder(new Label("This course has no CA exams yet (create them in Upload marks)"));
            }
        } catch (Exception e) {
            say(summary, "Could not load eligibility: " + e.getMessage(), false);
        }
    }

    private void showEligibilityDetails(EligibilityResult selected) {
        eligibilitySessions.getItems().clear();
        if (selected == null) {
            eligibilityDetailTitle.setText("Select a student to see attendance details");
            return;
        }
        Attendance attendance = selected.attendance();
        eligibilityDetailTitle.setText(attendance.name() + " (" + attendance.regNo() + ")   "
                + attendance.finalText() + " attendance   |   " + selected.status());
        Course course = courseBox.getValue();
        if (course == null) return;
        String component = course.type().equalsIgnoreCase("Theory") ? "Theory"
                : course.type().equalsIgnoreCase("Practical") ? "Practical" : "Combined";
        try {
            eligibilitySessions.setItems(FXCollections.observableArrayList(
                    LecturerDAO.sessions(attendance.regNo(), course.code(), component)));
        } catch (Exception e) {
            say(eligibilitySummary, "Could not load attendance details: " + e.getMessage(), false);
        }
    }
}
