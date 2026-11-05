package Lecturer;

import Lecturer.Academic.CourseResult;
import Lecturer.Academic.Gpa;
import Lecturer.LecturerDAO.Course;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.scene.control.*;
import javafx.scene.layout.*;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeSet;

import static Lecturer.LecturerUi.*;

/** Marks, grades and GPA. Everything is calculated from the uploaded marks (see Academic). */
public class GradesView extends VBox {

    // tab 1: results of one course
    private final ComboBox<Course> courseBox = new ComboBox<>();
    private final ObservableList<CourseResult> courseData = FXCollections.observableArrayList();
    private final TextField courseSearch = searchField("Search name or reg no...");
    private final ComboBox<String> courseStatus = statusBox("All", "A+", "A", "A-", "B+", "B", "B-",
            "C+", "C", "C-", "D+", "D", "E", "EC", "EE", "Pending");
    private final TableView<CourseResult> courseTable = table("Select a course", 380);
    private final Label courseSummary = new Label();

    // tab 2: SGPA / CGPA of one batch
    private final ComboBox<String> batchBox = new ComboBox<>();
    private final ComboBox<String> levelBox = new ComboBox<>();
    private final ComboBox<String> semesterBox = new ComboBox<>();
    private final ObservableList<Gpa> gpaData = FXCollections.observableArrayList();
    private final TextField gpaSearch = searchField("Search name or reg no...");
    private final ComboBox<String> gpaStatus = statusBox("All", "Graded", "Pending");
    private final TableView<Gpa> gpaTable = table("Select a batch", 360);
    private final TableView<CourseResult> subjectTable = table("No student selected", 360);
    private final Label gpaSummary = new Label();
    private final Label subjectTitle = new Label("Select a student to see subjects and grades");

    private Map<String, List<CourseResult>> byStudent = new LinkedHashMap<>();
    private boolean refilling = false;

    public GradesView() {
        getStyleClass().add("page");
        setSpacing(18);
        getChildren().add(header("Grades & GPA", "Grades use the UGC scale in Academic.java; only courses with a final mark count"));

        TabPane tabs = new TabPane(new Tab("Course results", courseTab()), new Tab("SGPA & CGPA", gpaTab()));
        tabs.setTabClosingPolicy(TabPane.TabClosingPolicy.UNAVAILABLE);
        getChildren().addAll(new HBox(12, field("Course", courseBox), field("Batch", batchBox)), tabs);

        try {
            courseBox.setItems(FXCollections.observableArrayList(LecturerDAO.courses()));
            batchBox.getItems().add("All batches");
            batchBox.getItems().addAll(LecturerDAO.batches());
            batchBox.setValue("All batches");
        } catch (Exception e) {
            say(courseSummary, "Could not load the lists: " + e.getMessage(), false);
        }
        courseBox.valueProperty().addListener((o, a, c) -> loadCourse(c));
        batchBox.valueProperty().addListener((o, a, b) -> {
            loadBatch(b);
            loadCourse(courseBox.getValue());
        });
        levelBox.valueProperty().addListener((o, a, b) -> { if (!refilling) rebuildGpa(); });
        semesterBox.valueProperty().addListener((o, a, b) -> { if (!refilling) rebuildGpa(); });
        gpaTable.getSelectionModel().selectedItemProperty().addListener((o, a, g) -> showSubjects(g));
        loadBatch(batchBox.getValue());
    }

    // ------------------------------------------------------------ course results

    private VBox courseTab() {
        courseBox.setPromptText("Select course");
        courseBox.setMinWidth(380);
        courseBox.setPrefWidth(420);
        courseSearch.setPromptText("Search by name or registration number");
        courseSearch.setMinWidth(360);
        courseSearch.setPrefWidth(420);
        gpaSearch.setPromptText("Search by name or registration number");
        gpaSearch.setMinWidth(360);
        gpaSearch.setPrefWidth(420);

        courseTable.getColumns().add(col("Reg No", CourseResult::regNo, 110));
        courseTable.getColumns().add(col("Name", CourseResult::name, 210));
        courseTable.getColumns().add(col("CA %", CourseResult::caPercentText, 80));
        courseTable.getColumns().add(col("Final marks", CourseResult::finalText, 100));
        courseTable.getColumns().add(col("Total (/100)", CourseResult::totalText, 100));
        courseTable.getColumns().add(col("Grade", CourseResult::grade, 70));
        courseTable.getColumns().add(col("Grade point", CourseResult::gradePointText, 100));
        courseTable.setItems(searchable(courseData, courseSearch, r -> r.name() + " " + r.regNo(),
                new Filter<CourseResult>(courseStatus, r -> r.isGraded() ? r.grade() : "Pending")));

        return new VBox(12, new HBox(12, field("Search", courseSearch), field("Grade status", courseStatus)),
                courseSummary, card(courseTable));
    }

    private void loadCourse(Course c) {
        courseData.clear();
        courseSummary.setText("");
        if (c == null) return;
        try {
            List<CourseResult> list = LecturerDAO.resultsOfCourse(c.code());
            String batch = batchBox.getValue();
            if (batch != null && !batch.equals("All batches")) {
                Map<String, String> batchByStudent = new java.util.HashMap<>();
                for (LecturerDAO.Student student : LecturerDAO.students()) {
                    batchByStudent.put(student.regNo(), student.batch());
                }
                list = list.stream().filter(r -> batch.equals(batchByStudent.get(r.regNo()))).toList();
            }
            courseData.setAll(list);
            long graded = list.stream().filter(CourseResult::hasFinal).count();
            double avg = list.stream().filter(CourseResult::hasFinal).mapToDouble(CourseResult::total).average().orElse(0);
            say(courseSummary, "Students: " + list.size() + "   |   Final marks uploaded: " + graded
                    + "   |   Average total: " + String.format("%.2f", avg), true);
            courseTable.setPlaceholder(new Label("No exams or students for this course yet"));
        } catch (Exception e) {
            say(courseSummary, "Could not load the results: " + e.getMessage(), false);
        }
    }

    // ------------------------------------------------------------------ GPA

    private VBox gpaTab() {
        batchBox.setPromptText("Select batch");
        batchBox.setMinWidth(260);
        batchBox.setPrefWidth(280);
        batchBox.setEditable(false);
        levelBox.setPromptText("Level");
        levelBox.setMinWidth(190);
        levelBox.setPrefWidth(210);
        semesterBox.setPromptText("Semester");
        semesterBox.setMinWidth(160);
        semesterBox.setPrefWidth(180);

        gpaTable.getColumns().add(col("Reg No", Gpa::regNo, 110));
        gpaTable.getColumns().add(col("Name", Gpa::name, 220));
        gpaTable.getColumns().add(col("Courses graded", Gpa::coursesText, 120));
        gpaTable.getColumns().add(col("Credits", g -> String.valueOf(g.credits()), 80));
        gpaTable.getColumns().add(col("SGPA", Gpa::sgpaText, 80));
        gpaTable.getColumns().add(col("CGPA", Gpa::cgpaText, 80));
        gpaTable.setItems(searchable(gpaData, gpaSearch, g -> g.name() + " " + g.regNo(),
                new Filter<Gpa>(gpaStatus, g -> g.sgpa() != null ? "Graded" : "Pending")));

        subjectTable.getColumns().add(col("Code", CourseResult::courseCode, 90));
        subjectTable.getColumns().add(col("Course", CourseResult::courseName, 260));
        subjectTable.getColumns().add(col("Credits", CourseResult::creditsText, 70));
        subjectTable.getColumns().add(col("Total (/100)", CourseResult::totalText, 100));
        subjectTable.getColumns().add(col("Grade", CourseResult::grade, 70));
        subjectTable.getColumns().add(col("Grade point", CourseResult::gradePointText, 100));
        subjectTitle.getStyleClass().add("card-title");

        HBox tables = new HBox(12, card(gpaTable), card(subjectTitle, subjectTable));
        HBox.setHgrow(tables.getChildren().get(0), Priority.ALWAYS);
        HBox.setHgrow(tables.getChildren().get(1), Priority.ALWAYS);
        return new VBox(12,
                new HBox(12, field("Search", gpaSearch), field("Result status", gpaStatus),
                        field("Level", levelBox), field("Semester", semesterBox)),
                gpaSummary, tables);
    }

    private void loadBatch(String batch) {
        subjectTable.getItems().clear();
        gpaData.clear();
        if (batch == null) return;
        try {
            byStudent = new LinkedHashMap<>();
            TreeSet<Integer> semesters = new TreeSet<>();
            TreeSet<Integer> levels = new TreeSet<>();
            List<CourseResult> results = batch.equals("All batches")
                    ? LecturerDAO.resultsOfAllBatches() : LecturerDAO.resultsOfBatch(batch);
            for (CourseResult r : results) {
                byStudent.computeIfAbsent(r.regNo(), k -> new ArrayList<>()).add(r);
                semesters.add(r.semester());
                levels.add(r.level());
            }

            refilling = true;                                  // do not rebuild while the list is refilled
            levelBox.getItems().clear();
            levelBox.getItems().add("All levels");
            levels.forEach(level -> levelBox.getItems().add(String.valueOf(level)));
            levelBox.setValue("All levels");
            semesterBox.getItems().clear();
            semesterBox.getItems().add("All");
            for (int s : semesters) semesterBox.getItems().add(String.valueOf(s));
            semesterBox.setValue("All");
            refilling = false;

            rebuildGpa();
        } catch (Exception e) {
            refilling = false;
            say(gpaSummary, "Could not load the batch: " + e.getMessage(), false);
        }
    }

    private void rebuildGpa() {
        String sel = semesterBox.getValue();
        Integer semester = (sel == null || sel.equals("All")) ? null : Integer.valueOf(sel);
        String selectedLevel = levelBox.getValue();
        Integer level = selectedLevel == null || selectedLevel.equals("All levels")
                ? null : Integer.valueOf(selectedLevel);

        List<Gpa> list = new ArrayList<>();
        for (Map.Entry<String, List<CourseResult>> entry : byStudent.entrySet()) {
            list.add(Gpa.of(entry.getValue(), level, semester));
        }
        gpaData.setAll(list);
        subjectTable.getItems().clear();

        double avg = list.stream().filter(g -> g.cgpa() != null).mapToDouble(Gpa::cgpa).average().orElse(0);
        String batch = batchBox.getValue() == null || batchBox.getValue().equals("All batches")
                ? "All batches" : "Batch " + batchBox.getValue();
        String levelLabel = selectedLevel == null || selectedLevel.equals("All levels")
                ? "All levels" : "Level " + selectedLevel;
        say(gpaSummary, "Students: " + list.size() + "   |   " + batch + " / " + levelLabel
                + " average CGPA: " + String.format("%.2f", avg)
                + "   |   NGPA courses and courses without a final mark or repeat grade are not counted", true);
        gpaTable.setPlaceholder(new Label("No exams have been created for this batch yet"));
    }

    private void showSubjects(Gpa g) {
        if (g == null) {
            subjectTable.getItems().clear();
            subjectTitle.setText("Select a student to see subjects and grades");
            return;
        }
        List<CourseResult> subjects = byStudent.getOrDefault(g.regNo(), List.of());
        String selectedLevel = levelBox.getValue();
        String selectedSemester = semesterBox.getValue();
        List<CourseResult> filteredSubjects = subjects.stream()
                .filter(result -> selectedLevel == null || selectedLevel.equals("All levels")
                        || result.level() == Integer.parseInt(selectedLevel))
                .filter(result -> selectedSemester == null || selectedSemester.equals("All")
                        || result.semester() == Integer.parseInt(selectedSemester))
                .toList();
        subjectTable.setItems(FXCollections.observableArrayList(filteredSubjects));
        subjectTitle.setText(g.name() + " (" + g.regNo() + ")   SGPA: " + g.sgpaText() + "   CGPA: " + g.cgpaText());
    }
}
