package Student;

import Common.Session;
import Common.AcademicResults;
import Database.Db;
import javafx.collections.FXCollections;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class GradesGpa extends VBox {

    private final ComboBox<String> level = new ComboBox<>(
            FXCollections.observableArrayList("1", "2", "3", "4"));
    private final ComboBox<String> semester = new ComboBox<>(
            FXCollections.observableArrayList("1", "2"));
    private final VBox resultsCard = new VBox(16);
    private final VBox statistics = new VBox();
    private final List<AcademicResults.CourseGrade> results = new ArrayList<>();
    private String loadError;
    private int currentTerm;

    public GradesGpa() {
        super(18);
        getStyleClass().add("content");

        HBox filters = new HBox(16,
                filter("Level", level, "level-filter"),
                filter("Semester", semester, "semester-filter"));
        filters.setPrefWidth(620);
        filters.setMaxWidth(620);

        getChildren().addAll(
                Ui.header(
                        "Grades & GPA",
                        "Select a level and semester to view your course results"
                ),
                filters,
                statistics,
                resultsCard
        );

        loadResults();
        level.setOnAction(event -> {
            updateSemesterOptions(null);
            refresh();
        });
        semester.setOnAction(event -> refresh());
        refresh();
    }

    private VBox filter(String title, ComboBox<String> control, String styleClass) {
        Label label = Ui.text(title, "stat-title", "filter-title");
        control.getStyleClass().add(styleClass);
        control.setMinWidth(280);
        control.setPrefWidth(280);
        control.setPrefHeight(42);
        VBox box = new VBox(8, label, control);
        box.setMinWidth(280);
        box.setPrefWidth(280);
        HBox.setHgrow(box, Priority.ALWAYS);
        return box;
    }

    private void refresh() {
        if (loadError != null) {
            statistics.getChildren().clear();
            resultsCard.getChildren().setAll(
                    Ui.text("Semester Results", "card-title"),
                    Ui.text(loadError, "err"));
            resultsCard.getStyleClass().setAll("card");
            return;
        }

        if (level.getValue() == null || semester.getValue() == null) {
            statistics.getChildren().clear();
            resultsCard.getChildren().setAll(
                    Ui.text("Semester Results", "card-title"),
                    Ui.text("Grades and GPA will be available after you advance to the next semester.",
                            "muted-small"));
            resultsCard.getStyleClass().setAll("card");
            return;
        }

        int selectedLevel = Integer.parseInt(level.getValue());
        int selectedSemester = Integer.parseInt(semester.getValue());

        List<AcademicResults.CourseGrade> selected = results.stream()
                .filter(result -> result.level() == selectedLevel
                        && result.semester() == selectedSemester)
                .toList();

        double sgpa = AcademicResults.gpa(selected);
        String sgpaText = AcademicResults.hasGpaResults(selected)
                ? formatGpa(sgpa) : "Pending";
        int selectedTerm = term(selectedLevel, selectedSemester);
        List<AcademicResults.CourseGrade> completed = results.stream()
                .filter(result -> term(result) <= selectedTerm)
                .toList();
        double cgpa = AcademicResults.gpa(completed);
        String cgpaText = AcademicResults.hasGpaResults(completed)
                ? formatGpa(cgpa) : "Pending";

        statistics.getChildren().setAll(Ui.stats(new String[][]{
                {"SGPA", sgpaText, "purple"},
                {"CGPA", cgpaText, "blue"},
                {"Courses", String.valueOf(selected.size()), "green"}
        }));

        if (selected.isEmpty()) {
            resultsCard.getChildren().setAll(
                    Ui.text("Semester Results", "card-title"),
                    Ui.text("No results found for the selected level and semester.", "muted-small"));
        } else {
            Object[][] rows = selected.stream()
                    .map(result -> new Object[]{
                            result.course(),
                            Ui.tag(result.grade(), gradeColor(result.grade()))
                    })
                    .toArray(Object[][]::new);

            resultsCard.getChildren().setAll(
                    Ui.text("Semester Results", "card-title"),
                    Ui.table(new String[]{"Course", "Grade"}, rows)
            );
        }
        resultsCard.getStyleClass().setAll("card");
    }

    private void loadResults() {
        String regNo = Session.getRegNo();
        if (regNo == null || regNo.isBlank()) {
            loadError = "Please login to view your grades.";
            return;
        }

        try {
            List<Map<String, Object>> studentRows = Db.query(
                    "SELECT level, semester FROM undergraduates WHERE reg_no = ?", regNo);
            if (studentRows.isEmpty()) {
                loadError = "Unable to find your current semester.";
                return;
            }
            int currentLevel = Integer.parseInt(value(studentRows.get(0), "level"));
            int currentSemester = Integer.parseInt(value(studentRows.get(0), "semester"));
            currentTerm = term(currentLevel, currentSemester);

            List<String> completedLevels = new ArrayList<>();
            for (int completedLevel = 1; completedLevel <= currentLevel; completedLevel++) {
                if (term(completedLevel, 1) < currentTerm) {
                    completedLevels.add(String.valueOf(completedLevel));
                }
            }
            level.getItems().setAll(completedLevels);
            level.setValue(completedLevels.isEmpty()
                    ? null : completedLevels.get(completedLevels.size() - 1));
            updateSemesterOptions(currentSemester == 1 ? "2" : String.valueOf(currentSemester - 1));
        } catch (Exception ex) {
            loadError = "Unable to load your current semester: " + ex.getMessage();
            return;
        }

        try {
            results.addAll(AcademicResults.beforeTerm(regNo, currentTerm));
        } catch (Exception ex) {
            loadError = "Unable to load grades: " + ex.getMessage();
        }
    }

    private void updateSemesterOptions(String preferredSemester) {
        semester.getItems().clear();
        String selectedLevel = level.getValue();
        if (selectedLevel == null) {
            semester.setValue(null);
            return;
        }

        int selectedLevelNumber = Integer.parseInt(selectedLevel);
        for (int semesterNumber = 1; semesterNumber <= 2; semesterNumber++) {
            if (term(selectedLevelNumber, semesterNumber) < currentTerm) {
                semester.getItems().add(String.valueOf(semesterNumber));
            }
        }
        if (preferredSemester != null && semester.getItems().contains(preferredSemester)) {
            semester.setValue(preferredSemester);
        } else if (!semester.getItems().isEmpty()) {
            semester.setValue(semester.getItems().get(semester.getItems().size() - 1));
        } else {
            semester.setValue(null);
        }
    }

    private static int term(AcademicResults.CourseGrade result) {
        return term(result.level(), result.semester());
    }

    private static String value(Map<String, Object> row, String key) {
        Object value = row.get(key);
        return value == null ? "" : value.toString();
    }

    private static int term(int level, int semester) {
        return (level - 1) * 2 + semester;
    }

    private static String formatGpa(double gpa) {
        return String.format("%.2f", gpa);
    }

    private static String gradeColor(String grade) {
        if (grade.startsWith("A")) {
            return "green";
        }
        if (grade.startsWith("B")) {
            return "blue";
        }
        if (grade.startsWith("E")) {
            return "red";
        }
        if (grade.startsWith("Awaiting")) {
            return "yellow";
        }
        return "blue";
    }
}
