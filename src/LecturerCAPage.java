import javafx.collections.FXCollections;
import javafx.collections.transformation.FilteredList;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.*;

import java.util.List;

public class LecturerCAPage extends VBox {

    private final Lecturer lecturer;
    private final EligibilityService service = new EligibilityService();
    private final ComboBox<CourseItem> courseBox = new ComboBox<>();
    private final TextField search = new TextField();
    private final ComboBox<String> statusBox = new ComboBox<>();
    private final TableView<CAResult> table = new TableView<>();
    private final TableView<CAResult.Component> detail = new TableView<>();
    private final Label summary = new Label();
    private final Label detailTitle = new Label("Select a student to see the CA breakdown");
    private FilteredList<CAResult> filtered;

    public LecturerCAPage(Lecturer lecturer) {
        this.lecturer = lecturer;
        setSpacing(12);

        Label title = new Label("CA Marks & Eligibility");
        title.setStyle("-fx-font-size: 24px; -fx-font-weight: bold;");

        courseBox.setPromptText("Select course");
        courseBox.setPrefWidth(320);
        search.setPromptText("Search name or reg no...");
        search.setPrefWidth(240);
        statusBox.getItems().addAll("All", "Eligible", "Not Eligible", "Pending");
        statusBox.setValue("All");
        Button clear = new Button("Clear");
        clear.setOnAction(e -> { search.clear(); statusBox.setValue("All"); });

        HBox row1 = new HBox(12, courseBox);
        HBox row2 = new HBox(12, search, statusBox, clear);

        summary.setStyle("-fx-font-weight: bold;");

        // Batch summary table
        TableColumn<CAResult, String> statusCol = col("Status", "status", 120);
        statusCol.setCellFactory(c -> new TableCell<CAResult, String>() {
            @Override
            protected void updateItem(String s, boolean empty) {
                super.updateItem(s, empty);
                if (empty || s == null) { setText(null); setStyle(""); return; }
                setText(s);
                switch (s) {
                    case "Eligible" -> setStyle("-fx-text-fill: #1a9c5b; -fx-font-weight: bold;");
                    case "Not Eligible" -> setStyle("-fx-text-fill: #d93636; -fx-font-weight: bold;");
                    default -> setStyle("-fx-text-fill: #e08a00; -fx-font-weight: bold;");
                }
            }
        });
        table.getColumns().addAll(
                this.<CAResult>col("Reg No", "regNo", 130),
                this.<CAResult>col("Name", "fullName", 230),
                this.<CAResult>col("CA Marks", "caText", 140),
                this.<CAResult>col("CA %", "percentageText", 100),
                this.<CAResult>col("Marks Entered", "enteredText", 130),
                statusCol);
        table.setPlaceholder(new Label("Select a course"));
        VBox.setVgrow(table, Priority.ALWAYS);

        // Individual breakdown table
        detailTitle.setStyle("-fx-font-size: 16px; -fx-font-weight: bold;");
        detail.getColumns().addAll(
                this.<CAResult.Component>col("Assessment", "name", 260),
                this.<CAResult.Component>col("Type", "type", 110),
                this.<CAResult.Component>col("Weight", "weightText", 90),
                this.<CAResult.Component>col("Mark (/100)", "markText", 110),
                this.<CAResult.Component>col("Contribution", "contributionText", 120));
        detail.setPrefHeight(170);
        detail.setPlaceholder(new Label("No student selected"));

        getChildren().addAll(title, row1, summary, row2, table, detailTitle, detail);

        // Load courses
        try {
            courseBox.setItems(FXCollections.observableArrayList(
                    new MarkDAO().getCourses(lecturer.getLecturerId())));
        } catch (Exception ex) {
            summary.setStyle("-fx-text-fill: red;");
            summary.setText("Could not load courses: " + ex.getMessage());
        }

        courseBox.valueProperty().addListener((o, a, c) -> loadResults(c));
        search.textProperty().addListener((o, a, b) -> applyFilter());
        statusBox.valueProperty().addListener((o, a, b) -> applyFilter());
        table.getSelectionModel().selectedItemProperty().addListener((o, a, r) -> showDetail(r));
    }

    private void loadResults(CourseItem course) {
        detail.getItems().clear();
        detailTitle.setText("Select a student to see the CA breakdown");
        if (course == null) return;
        try {
            List<CAResult> all = service.calculate(course.getCode());
            filtered = new FilteredList<>(FXCollections.observableArrayList(all), r -> true);
            table.setItems(filtered);
            table.setPlaceholder(new Label("No CA assessments or students for this course"));

            long ok = all.stream().filter(r -> r.getStatus().equals("Eligible")).count();
            long no = all.stream().filter(r -> r.getStatus().equals("Not Eligible")).count();
            long pend = all.stream().filter(r -> r.getStatus().equals("Pending")).count();
            summary.setStyle("-fx-font-weight: bold;");
            summary.setText("Students: " + all.size() + "   |   Eligible: " + ok
                    + "   |   Not Eligible: " + no + "   |   Pending: " + pend
                    + "   (CA pass mark: " + CAResult.PASS_PERCENTAGE + "%)");
            applyFilter();
        } catch (Exception ex) {
            summary.setStyle("-fx-text-fill: red;");
            summary.setText("Could not load results: " + ex.getMessage());
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

    private void showDetail(CAResult r) {
        if (r == null) {
            detail.getItems().clear();
            detailTitle.setText("Select a student to see the CA breakdown");
            return;
        }
        detail.setItems(FXCollections.observableArrayList(r.getComponents()));
        detailTitle.setText("CA breakdown - " + r.getFullName() + " (" + r.getRegNo() + ")  "
                + r.getCaText() + "  =  " + r.getPercentageText() + "  ->  " + r.getStatus());
    }

    private <S> TableColumn<S, String> col(String header, String property, double width) {
        TableColumn<S, String> c = new TableColumn<>(header);
        c.setCellValueFactory(new PropertyValueFactory<>(property));
        c.setPrefWidth(width);
        return c;
    }
}