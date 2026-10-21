import javafx.collections.FXCollections;
import javafx.collections.transformation.FilteredList;
import javafx.geometry.Insets;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.*;

import java.util.*;

public class LecturerOverallEligibilityPage extends VBox {

    private final EligibilityService caService = new EligibilityService();
    private final AttendanceDAO attDao = new AttendanceDAO();
    private final ComboBox<CourseItem> courseBox = new ComboBox<>();
    private final TextField search = new TextField();
    private final ComboBox<String> statusBox = new ComboBox<>();
    private final TableView<OverallEligibility> table = new TableView<>();
    private final Label summary = new Label();
    private final Label detail = new Label("Select a student to see the breakdown");
    private FilteredList<OverallEligibility> filtered;

    public LecturerOverallEligibilityPage(Lecturer lecturer) {
        setSpacing(10);

        Label title = new Label("Overall Eligibility (Attendance + CA)");
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

        TableColumn<OverallEligibility, String> statusCol = col("Overall", "status", 120);
        statusCol.setCellFactory(c -> new TableCell<OverallEligibility, String>() {
            @Override
            protected void updateItem(String s, boolean empty) {
                super.updateItem(s, empty);
                if (empty || s == null) { setText(null); setStyle(""); return; }
                setText(s);
                if (s.equals("Eligible")) setStyle("-fx-text-fill: #1a9c5b; -fx-font-weight: bold;");
                else if (s.equals("Not Eligible")) setStyle("-fx-text-fill: #d93636; -fx-font-weight: bold;");
                else setStyle("-fx-text-fill: #e08a00; -fx-font-weight: bold;");
            }
        });

        table.getColumns().addAll(
                this.<OverallEligibility>col("Reg No", "regNo", 120),
                this.<OverallEligibility>col("Name", "fullName", 200),
                this.<OverallEligibility>col("Attendance %", "attendanceText", 120),
                this.<OverallEligibility>col("Attendance OK", "attendanceOk", 120),
                this.<OverallEligibility>col("CA %", "caText", 90),
                this.<OverallEligibility>col("CA OK", "caOk", 90),
                statusCol,
                this.<OverallEligibility>col("Reason", "reason", 260));
        table.setPlaceholder(new Label("Select a course"));
        VBox.setVgrow(table, Priority.ALWAYS);

        detail.setStyle("-fx-font-size: 14px;");
        detail.setPadding(new Insets(10));
        detail.setStyle("-fx-background-color: white; -fx-border-color: #ddd;");
        detail.setMaxWidth(Double.MAX_VALUE);

        getChildren().addAll(title, row1, summary, row2, table, detail);

        try {
            courseBox.setItems(FXCollections.observableArrayList(
                    new MarkDAO().getCourses(lecturer.getLecturerId())));
        } catch (Exception ex) {
            showError("Could not load courses: " + ex.getMessage());
        }

        courseBox.valueProperty().addListener((o, a, c) -> loadResults(c));
        search.textProperty().addListener((o, a, b) -> applyFilter());
        statusBox.valueProperty().addListener((o, a, b) -> applyFilter());
        table.getSelectionModel().selectedItemProperty().addListener((o, a, s) ->
                detail.setText(s == null ? "Select a student to see the breakdown" : s.getBreakdown()));
    }

    private void loadResults(CourseItem course) {
        detail.setText("Select a student to see the breakdown");
        if (course == null) {
            table.setItems(FXCollections.observableArrayList());
            summary.setText("");
            return;
        }
        try {
            String type = attDao.getCourseType(course.getCode());
            int components = (type.contains("Theory") ? 1 : 0) + (type.contains("Practical") ? 1 : 0);
            int totalSessions = AttendanceDAO.SESSIONS_PER_COMPONENT * components;

            List<AttendanceSummary> attendance =
                    attDao.getBatchSummary(course.getCode(), "Combined", totalSessions);

            Map<String, CAResult> caMap = new HashMap<>();
            for (CAResult r : caService.calculate(course.getCode())) caMap.put(r.getRegNo(), r);

            List<OverallEligibility> list = new ArrayList<>();
            for (AttendanceSummary a : attendance) {
                list.add(new OverallEligibility(a, caMap.get(a.getRegNo())));
            }

            filtered = new FilteredList<>(FXCollections.observableArrayList(list), r -> true);
            table.setItems(filtered);

            long ok = list.stream().filter(r -> r.getStatus().equals("Eligible")).count();
            long no = list.stream().filter(r -> r.getStatus().equals("Not Eligible")).count();
            long pend = list.stream().filter(r -> r.getStatus().equals("Pending")).count();
            summary.setStyle("-fx-font-weight: bold;");
            summary.setText("Students: " + list.size() + "   |   Eligible: " + ok
                    + "   |   Not Eligible: " + no + "   |   Pending: " + pend
                    + "   (Attendance >= " + AttendanceSummary.PASS_PERCENTAGE
                    + "%, CA >= " + (int) CAResult.PASS_PERCENTAGE + "%)");
            applyFilter();
        } catch (Exception ex) {
            showError("Could not load eligibility: " + ex.getMessage());
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