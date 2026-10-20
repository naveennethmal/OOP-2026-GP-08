import javafx.collections.FXCollections;
import javafx.geometry.Insets;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.control.cell.TextFieldTableCell;
import javafx.scene.layout.*;

import java.util.List;

public class LecturerMarksPage extends VBox {

    private final Lecturer lecturer;
    private final MarkDAO dao = new MarkDAO();
    private final ComboBox<CourseItem> courseBox = new ComboBox<>();
    private final ComboBox<AssessmentItem> assessBox = new ComboBox<>();
    private final TableView<MarkEntry> table = new TableView<>();
    private final Label message = new Label();

    public LecturerMarksPage(Lecturer lecturer) {
        this.lecturer = lecturer;
        setSpacing(15);

        Label title = new Label("Upload Marks");
        title.setStyle("-fx-font-size: 24px; -fx-font-weight: bold;");

        courseBox.setPromptText("Select course");
        courseBox.setPrefWidth(300);
        assessBox.setPromptText("Select assessment");
        assessBox.setPrefWidth(360);
        assessBox.setDisable(true);

        HBox pickers = new HBox(12, courseBox, assessBox);

        // Editable table
        table.setEditable(true);
        TableColumn<MarkEntry, String> regCol = new TableColumn<>("Reg No");
        regCol.setCellValueFactory(new PropertyValueFactory<>("regNo"));
        regCol.setPrefWidth(140);

        TableColumn<MarkEntry, String> nameCol = new TableColumn<>("Name");
        nameCol.setCellValueFactory(new PropertyValueFactory<>("fullName"));
        nameCol.setPrefWidth(260);

        TableColumn<MarkEntry, String> markCol = new TableColumn<>("Marks (out of 100)");
        markCol.setCellValueFactory(new PropertyValueFactory<>("marks"));
        markCol.setCellFactory(TextFieldTableCell.forTableColumn());
        markCol.setOnEditCommit(ev -> ev.getRowValue().setMarks(ev.getNewValue()));
        markCol.setPrefWidth(170);

        table.getColumns().addAll(regCol, nameCol, markCol);
        table.setPlaceholder(new Label("Select a course and an assessment"));
        VBox.setVgrow(table, Priority.ALWAYS);

        Button save = new Button("Save Marks");
        save.setOnAction(e -> save());
        Label hint = new Label("Double-click a mark, type it, then press Enter. Leave blank to skip.");
        hint.setStyle("-fx-text-fill: #666;");
        HBox bottom = new HBox(15, save, hint);
        bottom.setPadding(new Insets(5, 0, 0, 0));

        getChildren().addAll(title, pickers, table, bottom, message);

        loadCourses();
        courseBox.valueProperty().addListener((o, a, c) -> loadAssessments(c));
        assessBox.valueProperty().addListener((o, a, s) -> loadStudents());
    }

    private void loadCourses() {
        try {
            courseBox.setItems(FXCollections.observableArrayList(
                    dao.getCourses(lecturer.getLecturerId())));
        } catch (Exception ex) {
            showError("Could not load courses: " + ex.getMessage());
        }
    }

    private void loadAssessments(CourseItem course) {
        table.getItems().clear();
        assessBox.getItems().clear();
        assessBox.setDisable(course == null);
        message.setText("");
        if (course == null) return;
        try {
            assessBox.setItems(FXCollections.observableArrayList(
                    dao.getAssessments(course.getCode())));
        } catch (Exception ex) {
            showError("Could not load assessments: " + ex.getMessage());
        }
    }

    private void loadStudents() {
        message.setText("");
        CourseItem c = courseBox.getValue();
        AssessmentItem a = assessBox.getValue();
        if (c == null || a == null) {
            table.getItems().clear();
            return;
        }
        try {
            List<MarkEntry> list = dao.getStudentMarks(c.getCode(), a.getId());
            table.setItems(FXCollections.observableArrayList(list));
        } catch (Exception ex) {
            showError("Could not load students: " + ex.getMessage());
        }
    }

    private void save() {
        AssessmentItem a = assessBox.getValue();
        if (a == null) {
            showError("Please select a course and an assessment first");
            return;
        }
        try {
            int saved = dao.saveMarks(table.getItems(), a.getId(), lecturer.getLecturerId());
            message.setStyle("-fx-text-fill: green;");
            message.setText(saved + " marks saved successfully");
        } catch (InvalidMarkException ex) {
            showError(ex.getMessage());
        } catch (Exception ex) {
            showError("Database error: " + ex.getMessage());
        }
    }

    private void showError(String text) {
        message.setStyle("-fx-text-fill: red;");
        message.setText(text);
    }
}