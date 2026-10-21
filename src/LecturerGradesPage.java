import javafx.collections.FXCollections;
import javafx.collections.transformation.FilteredList;
import javafx.geometry.Insets;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.*;

import java.util.*;

public class LecturerGradesPage extends VBox {

    private final GradeDAO dao = new GradeDAO();

    // Tab 1: course results
    private final ComboBox<CourseItem> courseBox = new ComboBox<>();
    private final TextField courseSearch = new TextField();
    private final TableView<CourseResult> courseTable = new TableView<>();
    private final Label courseSummary = new Label();
    private FilteredList<CourseResult> courseFiltered;

    // Tab 2: SGPA / CGPA
    private final ComboBox<String> batchBox = new ComboBox<>();
    private final ComboBox<String> semesterBox = new ComboBox<>();
    private final TextField gpaSearch = new TextField();
    private final TableView<GpaSummary> gpaTable = new TableView<>();
    private final TableView<CourseResult> subjectTable = new TableView<>();
    private final Label gpaSummaryLabel = new Label();
    private final Label subjectTitle = new Label("Select a student to see subjects and grades");
    private FilteredList<GpaSummary> gpaFiltered;
    private Map<String, List<CourseResult>> byStudent = new LinkedHashMap<>();
    private boolean loading = false;

    public LecturerGradesPage(Lecturer lecturer) {
        setSpacing(10);

        Label title = new Label("Grades & GPA");
        title.setStyle("-fx-font-size: 24px; -fx-font-weight: bold;");

        TabPane tabs = new TabPane(
                new Tab("Course Results", buildCourseTab()),
                new Tab("SGPA & CGPA", buildGpaTab()));
        tabs.setTabClosingPolicy(TabPane.TabClosingPolicy.UNAVAILABLE);
        VBox.setVgrow(tabs, Priority.ALWAYS);
        getChildren().addAll(title, tabs);

        try {
            courseBox.setItems(FXCollections.observableArrayList(
                    new MarkDAO().getCourses(lecturer.getLecturerId())));
            batchBox.setItems(FXCollections.observableArrayList(
                    new UndergraduateDAO().getBatches()));
        } catch (Exception ex) {
            showError(courseSummary, "Could not load lists: " + ex.getMessage());
        }

        courseBox.valueProperty().addListener((o, a, c) -> loadCourseResults(c));
        courseSearch.textProperty().addListener((o, a, b) -> applyCourseFilter());
        batchBox.valueProperty().addListener((o, a, b) -> loadBatch(b));
        semesterBox.valueProperty().addListener((o, a, b) -> { if (!loading) rebuildGpa(); });
        gpaSearch.textProperty().addListener((o, a, b) -> applyGpaFilter());
        gpaTable.getSelectionModel().selectedItemProperty().addListener((o, a, s) -> showSubjects(s));
    }

    // ---------- Tab 1 ----------
    private VBox buildCourseTab() {
        courseBox.setPromptText("Select course");
        courseBox.setPrefWidth(320);
        courseSearch.setPromptText("Search name or reg no...");
        courseSearch.setPrefWidth(240);
        HBox row = new HBox(12, courseBox, courseSearch);
        courseSummary.setStyle("-fx-font-weight: bold;");

        courseTable.getColumns().addAll(
                this.<CourseResult>col("Reg No", "regNo", 120),
                this.<CourseResult>col("Name", "fullName", 210),
                this.<CourseResult>col("CA %", "caPercentText", 90),
                this.<CourseResult>col("Final Marks", "finalText", 110),
                this.<CourseResult>col("Total (/100)", "totalText", 110),
                this.<CourseResult>col("Grade", "grade", 80),
                this.<CourseResult>col("Grade Point", "gradePointText", 110));
        courseTable.setPlaceholder(new Label("Select a course"));
        VBox.setVgrow(courseTable, Priority.ALWAYS);

        VBox box = new VBox(10, row, courseSummary, courseTable);
        box.setPadding(new Insets(12, 0, 0, 0));
        return box;
    }

    private void loadCourseResults(CourseItem c) {
        if (c == null) {
            courseTable.setItems(FXCollections.observableArrayList());
            courseSummary.setText("");
            return;
        }
        try {
            List<CourseResult> list = dao.getByCourse(c.getCode());
            courseFiltered = new FilteredList<>(FXCollections.observableArrayList(list), r -> true);
            courseTable.setItems(courseFiltered);

            long graded = list.stream().filter(CourseResult::hasFinal).count();
            double avg = list.stream().filter(CourseResult::hasFinal)
                    .mapToDouble(CourseResult::getTotal).average().orElse(0);
            courseSummary.setStyle("-fx-font-weight: bold;");
            courseSummary.setText("Students: " + list.size() + "   |   Final marks uploaded: " + graded
                    + "   |   Average total: " + String.format("%.2f", avg));
            applyCourseFilter();
        } catch (Exception ex) {
            showError(courseSummary, "Could not load results: " + ex.getMessage());
        }
    }

    private void applyCourseFilter() {
        if (courseFiltered == null) return;
        String t = courseSearch.getText() == null ? "" : courseSearch.getText().toLowerCase().trim();
        courseFiltered.setPredicate(r -> t.isEmpty()
                || r.getFullName().toLowerCase().contains(t)
                || r.getRegNo().toLowerCase().contains(t));
    }

    // ---------- Tab 2 ----------
    private VBox buildGpaTab() {
        batchBox.setPromptText("Select batch");
        semesterBox.setPromptText("Semester");
        semesterBox.setPrefWidth(120);
        gpaSearch.setPromptText("Search name or reg no...");
        gpaSearch.setPrefWidth(240);
        HBox row = new HBox(12, batchBox, new Label("SGPA for:"), semesterBox, gpaSearch);
        row.setAlignment(javafx.geometry.Pos.CENTER_LEFT);
        gpaSummaryLabel.setStyle("-fx-font-weight: bold;");

        gpaTable.getColumns().addAll(
                this.<GpaSummary>col("Reg No", "regNo", 120),
                this.<GpaSummary>col("Name", "fullName", 220),
                this.<GpaSummary>col("Courses Graded", "coursesText", 130),
                this.<GpaSummary>col("Credits", "creditsText", 90),
                this.<GpaSummary>col("SGPA", "sgpaText", 90),
                this.<GpaSummary>col("CGPA", "cgpaText", 90));
        gpaTable.setPlaceholder(new Label("Select a batch"));
        VBox.setVgrow(gpaTable, Priority.ALWAYS);

        subjectTitle.setStyle("-fx-font-size: 16px; -fx-font-weight: bold;");
        subjectTable.getColumns().addAll(
                this.<CourseResult>col("Code", "courseCode", 90),
                this.<CourseResult>col("Course", "courseName", 260),
                this.<CourseResult>col("Credits", "creditsText", 80),
                this.<CourseResult>col("Sem", "semesterText", 60),
                this.<CourseResult>col("Total (/100)", "totalText", 110),
                this.<CourseResult>col("Grade", "grade", 70),
                this.<CourseResult>col("Grade Point", "gradePointText", 110));
        subjectTable.setPrefHeight(190);
        subjectTable.setPlaceholder(new Label("No student selected"));

        VBox box = new VBox(10, row, gpaSummaryLabel, gpaTable, subjectTitle, subjectTable);
        box.setPadding(new Insets(12, 0, 0, 0));
        return box;
    }

    private void loadBatch(String batch) {
        subjectTable.setItems(FXCollections.observableArrayList());
        if (batch == null) return;
        try {
            List<CourseResult> all = dao.getByBatch(batch);
            byStudent = new LinkedHashMap<>();
            TreeSet<Integer> semesters = new TreeSet<>();
            for (CourseResult r : all) {
                byStudent.computeIfAbsent(r.getRegNo(), k -> new ArrayList<>()).add(r);
                semesters.add(r.getSemester());
            }

            loading = true;
            semesterBox.getItems().clear();
            semesterBox.getItems().add("All");
            for (int s : semesters) semesterBox.getItems().add(String.valueOf(s));
            semesterBox.setValue("All");
            loading = false;

            rebuildGpa();
        } catch (Exception ex) {
            loading = false;
            showError(gpaSummaryLabel, "Could not load batch: " + ex.getMessage());
        }
    }

    private void rebuildGpa() {
        String sel = semesterBox.getValue();
        Integer sem = (sel == null || sel.equals("All")) ? null : Integer.valueOf(sel);

        List<GpaSummary> list = new ArrayList<>();
        for (List<CourseResult> results : byStudent.values()) {
            list.add(GpaSummary.of(results, sem));
        }
        gpaFiltered = new FilteredList<>(FXCollections.observableArrayList(list), s -> true);
        gpaTable.setItems(gpaFiltered);
        subjectTable.setItems(FXCollections.observableArrayList());

        double avg = list.stream().filter(s -> s.getCgpa() != null)
                .mapToDouble(GpaSummary::getCgpa).average().orElse(0);
        gpaSummaryLabel.setStyle("-fx-font-weight: bold;");
        gpaSummaryLabel.setText("Students: " + list.size()
                + "   |   Batch average CGPA: " + String.format("%.2f", avg)
                + "   |   (only courses with a final mark are counted)");
        applyGpaFilter();
    }

    private void applyGpaFilter() {
        if (gpaFiltered == null) return;
        String t = gpaSearch.getText() == null ? "" : gpaSearch.getText().toLowerCase().trim();
        gpaFiltered.setPredicate(s -> t.isEmpty()
                || s.getFullName().toLowerCase().contains(t)
                || s.getRegNo().toLowerCase().contains(t));
    }

    private void showSubjects(GpaSummary s) {
        if (s == null) {
            subjectTable.setItems(FXCollections.observableArrayList());
            subjectTitle.setText("Select a student to see subjects and grades");
            return;
        }
        subjectTable.setItems(FXCollections.observableArrayList(byStudent.get(s.getRegNo())));
        subjectTitle.setText(s.getFullName() + " (" + s.getRegNo() + ")   SGPA: "
                + s.getSgpaText() + "   CGPA: " + s.getCgpaText());
    }

    private void showError(Label label, String text) {
        label.setStyle("-fx-text-fill: red;");
        label.setText(text);
    }

    private <S> TableColumn<S, String> col(String header, String property, double width) {
        TableColumn<S, String> c = new TableColumn<>(header);
        c.setCellValueFactory(new PropertyValueFactory<>(property));
        c.setPrefWidth(width);
        return c;
    }
}