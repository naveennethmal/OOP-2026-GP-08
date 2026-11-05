package Lecturer;

import Lecturer.LecturerDAO.Assessment;
import Lecturer.LecturerDAO.Course;
import Lecturer.LecturerDAO.MarkEntry;
import javafx.collections.FXCollections;
import javafx.scene.control.*;
import javafx.scene.layout.*;

import java.util.LinkedHashMap;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static Lecturer.LecturerUi.*;

/** Create an exam (assessment) for a course and upload the marks of every student for it. */
public class MarksView extends VBox {

    private static final String[] TYPES = {"Assignment", "Quiz", "Midterm", "Practical", "Final"};

    private final ComboBox<Course> courseBox = new ComboBox<>();
    private final ComboBox<String> batchBox = new ComboBox<>();
    private final TextField studentSearch = searchField("Search by name or registration number");
    private final TabPane sectionTabs = new TabPane();
    private final Map<String, ExamSection> sections = new LinkedHashMap<>();
    private final Map<Tab, ExamSection> sectionByTab = new HashMap<>();
    private final Label message = new Label();
    private final Button removeExam = ghost("Remove custom exam");
    private final TextField quickMark = new TextField();
    private final Button applyMarksButton = primary("Apply Marks");
    private final Button removeMarksButton = ghost("Remove Marks");
    private final Button applyRepeatGradeButton = primary("Apply");
    private final Button removeRepeatGradeButton = ghost("Remove");
    private final ComboBox<String> repeatGrade = new ComboBox<>(
            FXCollections.observableArrayList("Calculated", "C", "EC", "EE", "E"));
    private boolean settingCourseFilters;
    private boolean settingRepeatGrade;
    private boolean repeatGradeSaved;

    public MarksView() {
        getStyleClass().add("page");
        setSpacing(18);

        Button newExam = primary("+ New exam");
        newExam.setMinWidth(130);
        newExam.setOnAction(e -> newAssessment());
        removeExam.setMinWidth(175);
        HBox examActions = new HBox(10, newExam, removeExam);
        examActions.setAlignment(javafx.geometry.Pos.CENTER_RIGHT);
        getChildren().addAll(
                header("Upload marks", "CA 40%. Practical: mid 25% + project 15%. Theory: best 2 quizzes 10%, mid/project 25%, assignments 5%. EC 60%."),
                examActions);

        courseBox.setPromptText("Select course");
        courseBox.setMinWidth(380);
        courseBox.setPrefWidth(420);
        batchBox.setPromptText("Select batch");
        batchBox.setMinWidth(260);
        batchBox.setPrefWidth(280);
        batchBox.setEditable(false);
        studentSearch.setMinWidth(360);
        studentSearch.setPrefWidth(420);
        batchBox.getItems().add("All batches");
        batchBox.setValue("All batches");
        removeExam.setDisable(true);
        removeExam.setOnAction(e -> removeAssessment());
        quickMark.setPromptText("0–100");
        quickMark.setPrefWidth(210);
        quickMark.setMinWidth(190);
        quickMark.setDisable(true);
        applyMarksButton.setDisable(true);
        removeMarksButton.setDisable(true);
        repeatGrade.setValue("Calculated");
        repeatGrade.setPromptText("Select student");
        repeatGrade.setPrefWidth(220);
        repeatGrade.setMinWidth(200);
        repeatGrade.setDisable(true);
        applyRepeatGradeButton.setMinWidth(100);
        applyRepeatGradeButton.setPrefHeight(44);
        applyRepeatGradeButton.setDisable(true);
        removeRepeatGradeButton.setMinWidth(100);
        removeRepeatGradeButton.setPrefHeight(44);
        removeRepeatGradeButton.setDisable(true);
        applyMarksButton.setOnAction(e -> applyMarkToSelectedStudent());
        removeMarksButton.setOnAction(e -> removeMarkFromSelectedStudent());
        applyRepeatGradeButton.setOnAction(e -> saveRepeatGrade());
        removeRepeatGradeButton.setOnAction(e -> removeRepeatGrade());
        quickMark.setOnAction(e -> applyMarkToSelectedStudent());

        addSection("Quiz", "Quizzes");
        addSection("Assignment", "Assignments");
        addSection("Midterm", "Mid exam");
        addSection("Final", "End exam");
        addSection("Project", "Project");
        addSection("Other", "Other exams");

        VBox markField = field("Mark for selected student", quickMark);
        markField.setPrefWidth(220);
        VBox repeatGradeField = field("Repeat student marks", repeatGrade);
        repeatGradeField.setPrefWidth(240);
        applyMarksButton.setMinWidth(145);
        applyMarksButton.setPrefHeight(44);
        removeMarksButton.setMinWidth(155);
        removeMarksButton.setPrefHeight(44);
        VBox markEntry = new VBox(8, markField);
        HBox markActions = new HBox(10, applyMarksButton, removeMarksButton);
        markActions.setAlignment(javafx.geometry.Pos.CENTER_LEFT);
        markEntry.getChildren().add(markActions);
        HBox repeatGradeActions = new HBox(10, applyRepeatGradeButton, removeRepeatGradeButton);
        repeatGradeActions.setAlignment(javafx.geometry.Pos.CENTER_LEFT);
        VBox gradeEntry = new VBox(8, repeatGradeField, repeatGradeActions);
        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);
        HBox quickEntry = new HBox(16, markEntry, spacer, gradeEntry);
        quickEntry.setAlignment(javafx.geometry.Pos.BOTTOM_LEFT);
        quickEntry.setMaxWidth(Double.MAX_VALUE);
        quickEntry.getStyleClass().add("marks-quick-entry");
        VBox studentSearchField = field("Search student", studentSearch);
        studentSearchField.setMaxWidth(420);
        getChildren().addAll(new HBox(12, field("Course", courseBox), field("Batch", batchBox)),
                studentSearchField,
                quickEntry,
                sectionTabs,
                hint("Choose an exam section, select a student, enter the mark, then apply it. Remove Marks clears a saved mark."),
                message);

        try {
            courseBox.setItems(FXCollections.observableArrayList(LecturerDAO.courses()));
        } catch (Exception e) {
            say(message, "Could not load your courses: " + e.getMessage(), false);
        }
        courseBox.valueProperty().addListener((o, a, c) -> onCourseSelected(c));
        batchBox.valueProperty().addListener((o, a, b) -> { if (!settingCourseFilters) reloadStudentLists(); });
        studentSearch.textProperty().addListener((o, a, b) -> reloadStudentLists());
        sectionTabs.getSelectionModel().selectedItemProperty().addListener((o, oldTab, newTab) -> {
            updateRemoveButton();
            updateQuickMark();
        });
        refreshBatchOptions();
    }

    private void addSection(String key, String title) {
        ExamSection section = new ExamSection(key, title);
        sections.put(key, section);
        section.examBox.valueProperty().addListener((o, oldExam, newExam) -> loadStudents(section));
        section.table.getSelectionModel().selectedItemProperty().addListener((o, oldStudent, newStudent) -> {
            if (currentSection() == section) updateQuickMark();
        });
        Tab tab = new Tab(title, new VBox(12, field("Exam", section.examBox), card(section.table)));
        sectionByTab.put(tab, section);
        sectionTabs.getTabs().add(tab);
    }

    private static final class ExamSection {
        private final String key;
        private final ComboBox<Assessment> examBox = new ComboBox<>();
        private final TableView<MarkEntry> table;

        private ExamSection(String key, String title) {
            this.key = key;
            examBox.setPromptText("Select " + title.toLowerCase() + " exam");
            examBox.setPrefWidth(380);
            table = table("Select an exam", 420);
            table.getStyleClass().add("marks-table");
            table.getColumns().add(col("Reg No", MarkEntry::regNo, 140));
            table.getColumns().add(col("Name", MarkEntry::name, 280));
            table.getColumns().add(col("Batch", MarkEntry::batch, 100));
            TableColumn<MarkEntry, String> marks = new TableColumn<>("Marks (out of 100)");
            marks.setCellValueFactory(d -> d.getValue().marksProperty());
            marks.setPrefWidth(180);
            table.getColumns().add(marks);
        }
    }

    private ExamSection currentSection() {
        Tab tab = sectionTabs.getSelectionModel().getSelectedItem();
        return tab == null ? null : sectionByTab.get(tab);
    }

    private MarkEntry selectedStudent() {
        ExamSection section = currentSection();
        return section == null ? null : section.table.getSelectionModel().getSelectedItem();
    }

    private Assessment selectedAssessment() {
        ExamSection section = currentSection();
        return section == null ? null : section.examBox.getValue();
    }

    private void updateQuickMark() {
        MarkEntry selected = selectedStudent();
        Assessment exam = selectedAssessment();
        quickMark.setText(selected == null ? "" : selected.marksProperty().get());
        quickMark.setDisable(selected == null || exam == null);
        applyMarksButton.setDisable(selected == null || exam == null);
        removeMarksButton.setDisable(selected == null || exam == null || !selected.hasMark());
        loadRepeatGrade(selected);
    }

    private void loadRepeatGrade(MarkEntry student) {
        settingRepeatGrade = true;
        try {
            Course course = courseBox.getValue();
            if (student == null || course == null) {
                repeatGrade.setValue("Calculated");
                repeatGrade.setDisable(true);
                repeatGrade.setPromptText("Select student");
                repeatGradeSaved = false;
                updateRepeatGradeButtons();
                return;
            }
            String savedGrade = LecturerDAO.retakeGrade(course.code(), student.regNo());
            repeatGrade.setValue(savedGrade == null ? "Calculated" : savedGrade);
            repeatGrade.setDisable(false);
            repeatGrade.setPromptText("Select grade");
            repeatGradeSaved = savedGrade != null;
            updateRepeatGradeButtons();
        } catch (Exception e) {
            repeatGrade.setValue("Calculated");
            repeatGrade.setDisable(true);
            repeatGrade.setPromptText("Grade unavailable");
            repeatGradeSaved = false;
            updateRepeatGradeButtons();
            say(message, "Could not load the repeat-student grade: " + e.getMessage(), false);
        } finally {
            settingRepeatGrade = false;
        }
    }

    private void updateRepeatGradeButtons() {
        boolean selected = selectedStudent() != null && !repeatGrade.isDisabled();
        applyRepeatGradeButton.setDisable(!selected);
        removeRepeatGradeButton.setDisable(!selected || !repeatGradeSaved);
    }

    private void saveRepeatGrade() {
        MarkEntry student = selectedStudent();
        Course course = courseBox.getValue();
        if (student == null || course == null || repeatGrade.isDisabled()) return;
        try {
            String grade = repeatGrade.getValue();
            if (grade == null || grade.equals("Calculated")) {
                LecturerDAO.clearRetakeGrade(course.code(), student.regNo());
                repeatGradeSaved = false;
            } else {
                LecturerDAO.saveRetakeGrade(course.code(), student.regNo(), grade);
                repeatGradeSaved = true;
            }
            updateRepeatGradeButtons();
            say(message, "Repeat-student grade saved for " + student.name() + ".", true);
        } catch (Exception e) {
            say(message, "Could not save the repeat-student grade: " + e.getMessage(), false);
            loadRepeatGrade(student);
        }
    }

    private void removeRepeatGrade() {
        MarkEntry student = selectedStudent();
        Course course = courseBox.getValue();
        if (student == null || course == null || repeatGrade.isDisabled() || !repeatGradeSaved) return;
        try {
            LecturerDAO.clearRetakeGrade(course.code(), student.regNo());
            repeatGradeSaved = false;
            settingRepeatGrade = true;
            repeatGrade.setValue("Calculated");
            settingRepeatGrade = false;
            updateRepeatGradeButtons();
            say(message, "Repeat-student grade removed for " + student.name() + ".", true);
        } catch (Exception e) {
            settingRepeatGrade = false;
            say(message, "Could not remove the repeat-student grade: " + e.getMessage(), false);
            loadRepeatGrade(student);
        }
    }

    private void loadAssessments(Course course) {
        message.setText("");
        for (ExamSection section : sections.values()) {
            section.examBox.getItems().clear();
            section.table.getItems().clear();
            section.examBox.setDisable(course == null);
        }
        if (course == null) {
            updateRemoveButton();
            updateQuickMark();
            return;
        }
        try {
            LecturerDAO.ensureStandardAssessments(course.code());
            List<Assessment> all = LecturerDAO.assessments(course.code());
            for (Assessment assessment : all) {
                ExamSection section = sections.get(category(assessment));
                if (section != null) section.examBox.getItems().add(assessment);
            }
            for (ExamSection section : sections.values()) {
                if (!section.examBox.getItems().isEmpty()) section.examBox.getSelectionModel().selectFirst();
            }
            updateRemoveButton();
        } catch (Exception e) {
            say(message, "Could not load the exams: " + e.getMessage(), false);
        }
    }

    private void onCourseSelected(Course course) {
        loadAssessments(course);
        refreshBatchOptions();
    }

    private void refreshBatchOptions() {
        String previous = batchBox.getValue();
        settingCourseFilters = true;
        batchBox.getItems().setAll("All batches");
        try {
            batchBox.getItems().addAll(LecturerDAO.batches());
            batchBox.setValue(batchBox.getItems().contains(previous) ? previous : "All batches");
        } catch (Exception e) {
            say(message, "Could not load batches: " + e.getMessage(), false);
        } finally {
            settingCourseFilters = false;
        }
        reloadStudentLists();
    }

    private void reloadStudentLists() {
        for (ExamSection section : sections.values()) loadStudents(section);
    }

    private void loadStudents(ExamSection section) {
        message.setText("");
        Course c = courseBox.getValue();
        Assessment assessment = section.examBox.getValue();
        String batch = batchBox.getValue();
        if (c == null || assessment == null || batch == null) {
            section.table.getItems().clear();
            updateQuickMark();
            return;
        }
        try {
            String query = studentSearch.getText() == null ? "" : studentSearch.getText().trim().toLowerCase(java.util.Locale.ROOT);
            List<MarkEntry> entries = LecturerDAO.markEntries(c.code(), assessment.id(), batch);
            if (!query.isEmpty()) {
                entries = entries.stream()
                        .filter(student -> student.name().toLowerCase(java.util.Locale.ROOT).contains(query)
                                || student.regNo().toLowerCase(java.util.Locale.ROOT).contains(query))
                        .toList();
            }
            section.table.setItems(FXCollections.observableArrayList(entries));
            updateQuickMark();
        } catch (Exception e) {
            say(message, "Could not load the students: " + e.getMessage(), false);
        }
    }

    private String category(Assessment assessment) {
        String name = assessment.name().toUpperCase(java.util.Locale.ROOT);
        // Custom assignment assessments (for example Assignment 4) must stay
        // in the Assignment marks section instead of being routed to Other.
        if ("Assignment".equalsIgnoreCase(assessment.type()) || name.startsWith("ASSIGNMENT")) {
            return "Assignment";
        }
        if (!LecturerDAO.isStandardAssessment(assessment.name())) return "Other";
        if (name.startsWith("QUIZ ")) return "Quiz";
        if (name.equals("PROJECT")) return "Project";
        if (name.equals("MID EXAM")) return "Midterm";
        if (name.equals("END EXAM")) return "Final";
        return "Other";
    }

    private void applyMarkToSelectedStudent() {
        MarkEntry student = selectedStudent();
        Assessment exam = selectedAssessment();
        if (student == null || exam == null) {
            say(message, "Select an exam and student first.", false);
            return;
        }
        String entered = quickMark.getText().trim();
        try {
            double value = Double.parseDouble(entered);
            if (value < 0 || value > 100) throw new NumberFormatException();
            String previous = student.marksProperty().get();
            student.marksProperty().set(entered);
            try {
                LecturerDAO.saveMarks(List.of(student), exam.id());
            } catch (Exception e) {
                student.marksProperty().set(previous);
                throw e;
            }
            currentSection().table.refresh();
            updateQuickMark();
            say(message, "Mark saved for " + student.name() + ".", true);
        } catch (NumberFormatException e) {
            say(message, "Enter a valid mark from 0 to 100.", false);
        } catch (Exception e) {
            say(message, "Could not save the mark: " + e.getMessage(), false);
        }
    }

    private void removeMarkFromSelectedStudent() {
        MarkEntry student = selectedStudent();
        Assessment exam = selectedAssessment();
        if (student == null || exam == null || !student.hasMark()) return;
        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION,
                "Remove the saved mark for " + student.name() + " from " + exam.name() + "?",
                ButtonType.CANCEL, ButtonType.OK);
        confirm.setHeaderText("Remove mark");
        if (confirm.showAndWait().orElse(ButtonType.CANCEL) != ButtonType.OK) return;
        try {
            LecturerDAO.removeMark(student.regNo(), exam.id());
            student.marksProperty().set("");
            quickMark.clear();
            currentSection().table.refresh();
            updateQuickMark();
            say(message, "Mark removed for " + student.name() + ".", true);
        } catch (Exception e) {
            say(message, "Could not remove the mark: " + e.getMessage(), false);
        }
    }

    private void updateRemoveButton() {
        Assessment selected = selectedAssessment();
        removeExam.setDisable(selected == null || LecturerDAO.isStandardAssessment(selected.name()));
    }

    private void removeAssessment() {
        Assessment selected = selectedAssessment();
        Course course = courseBox.getValue();
        if (selected == null || course == null || LecturerDAO.isStandardAssessment(selected.name())) return;
        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION,
                "Remove '" + selected.name() + "' and all marks entered for it?", ButtonType.CANCEL, ButtonType.OK);
        confirm.setHeaderText("Remove custom exam");
        if (confirm.showAndWait().orElse(ButtonType.CANCEL) != ButtonType.OK) return;
        try {
            LecturerDAO.deleteAssessment(selected.id());
            loadAssessments(course);
            say(message, "Custom exam and its marks removed.", true);
        } catch (Exception e) {
            say(message, "Could not remove the exam: " + e.getMessage(), false);
        }
    }

    /** Small pop-up to create an exam (name, type and weight) for the selected course. */
    private void newAssessment() {
        Course c = courseBox.getValue();
        if (c == null) {
            say(message, "Select a course first.", false);
            return;
        }
        TextField name = new TextField();
        name.setPromptText("e.g. Quiz 1");
        ComboBox<String> type = new ComboBox<>(FXCollections.observableArrayList(TYPES));
        type.setValue(TYPES[0]);
        VBox form = new VBox(12, field("Exam name", name), field("Type", type));
        form.setPrefWidth(320);
        Dialog<ButtonType> dialog = new Dialog<>();
        dialog.setTitle("New exam - " + c.code());
        dialog.getDialogPane().setContent(form);
        dialog.getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);

        Optional<ButtonType> answer = dialog.showAndWait();
        if (answer.isEmpty() || answer.get() != ButtonType.OK) return;

        try {
            if (name.getText().isBlank()) throw new IllegalArgumentException("Exam name cannot be empty.");
            LecturerDAO.addAssessment(c.code(), name.getText().trim(), type.getValue(), 1);
            loadAssessments(c);
            String category = "Assignment".equalsIgnoreCase(type.getValue())
                    || name.getText().trim().toUpperCase(java.util.Locale.ROOT).startsWith("ASSIGNMENT")
                    ? "Assignment" : "Other";
            ExamSection target = sections.get(category);
            target.examBox.getItems().stream()
                    .filter(exam -> exam.name().equalsIgnoreCase(name.getText().trim()))
                    .findFirst().ifPresent(exam -> {
                        sectionByTab.forEach((tab, section) -> {
                            if (section == target) sectionTabs.getSelectionModel().select(tab);
                        });
                        target.examBox.setValue(exam);
                    });
            say(message, "Exam created. Select it to upload marks.", true);
        } catch (IllegalArgumentException e) {
            say(message, e.getMessage(), false);
        } catch (Exception e) {
            say(message, "Could not create the exam: " + e.getMessage(), false);
        }
    }
}
