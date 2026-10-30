package Student;

import Common.Session;
import Database.Db;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.collections.FXCollections;
import javafx.scene.control.*;
import javafx.scene.layout.*;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.awt.Desktop;
import java.io.File;
import java.net.URI;

/**
 * My Courses page: enrolled course list (left) and selected course details (right).
 */
public class CourseDetails extends VBox {

    private record Material(String title, String type, String date, String filePath) {}

    private record Course(String code, String name, String lecturer,
                          String description, String color, List<Material> materials) {}

    private final List<Course> courses = new ArrayList<>();
    private final List<HBox> listItems = new ArrayList<>();
    private final VBox detail = new VBox(18);
    private final VBox courseList = new VBox(2);
    private final Label listTitle = new Label();
    private final ComboBox<String> semesterSelector = new ComboBox<>();
    private boolean batchMissed;
    private String studentDepartment;
    private int studentLevel;
    private int studentSemester;
    private String loadError;

    public CourseDetails() {
        super(18);
        getStyleClass().add("content");

        loadStudentContext();
        if (!batchMissed) loadData(null);

        // ---------- left : enrolled courses ----------
        listTitle.getStyleClass().add("course-list-title");
        semesterSelector.setPromptText("Select semester");
        semesterSelector.setMaxWidth(Double.MAX_VALUE);
        semesterSelector.setVisible(batchMissed);
        semesterSelector.setManaged(batchMissed);
        if (batchMissed) {
            try {
                semesterSelector.setItems(FXCollections.observableArrayList(availableSemesters()));
            } catch (Exception ex) {
                loadError = "Unable to load semesters: " + ex.getMessage();
            }
            semesterSelector.valueProperty().addListener((obs, oldSemester, newSemester) -> {
                loadData(newSemester);
                refreshCourseList();
                detail.getChildren().clear();
                if (courses.isEmpty()) {
                    detail.getChildren().add(Ui.text(
                            loadError == null ? "No courses found for this semester." : loadError,
                            "muted-small"));
                } else {
                    select(0);
                }
            });
            if (!semesterSelector.getItems().isEmpty()) {
                semesterSelector.setValue(studentLevel + "." + studentSemester);
            }
        }

        VBox listCard = new VBox(14, listTitle, semesterSelector, courseList);
        listCard.getStyleClass().add("course-card");
        listCard.setPrefWidth(370);
        listCard.setMinWidth(340);
        listCard.setMaxWidth(370);

        // ---------- right : details ----------
        detail.getStyleClass().add("course-card");
        HBox.setHgrow(detail, Priority.ALWAYS);

        HBox body = new HBox(18, listCard, detail);
        body.setAlignment(Pos.TOP_LEFT);

        getChildren().addAll(Ui.header("My Courses", batchMissed
                ? "Select a semester to view its courses and materials."
                : "View the courses you are enrolled in and their details."), body);

        refreshCourseList();
        if (courses.isEmpty()) {
            detail.getChildren().add(Ui.text(
                    loadError != null ? loadError : batchMissed
                            ? "Select a semester to view its courses." : "No enrolled courses found.",
                    "muted-small"));
        } else {
            select(0);
        }
    }

    private void refreshCourseList() {
        courseList.getChildren().clear();
        listItems.clear();
        listTitle.setText((batchMissed ? "Courses" : "Enrolled Courses") + " (" + courses.size() + ")");
        for (int i = 0; i < courses.size(); i++) {
            HBox item = buildListItem(courses.get(i), i);
            listItems.add(item);
            courseList.getChildren().add(item);
        }
    }

    // =====================================================================
    //  Left list
    // =====================================================================
    private HBox buildListItem(Course c, int index) {
        Label badge = badge(c.code(), c.color(), 11);

        Label name = new Label(c.name());
        name.getStyleClass().add("course-list-name");
        VBox text = new VBox(name);

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);
        Label chevron = new Label("›");
        chevron.getStyleClass().add("course-list-chevron");

        HBox item = new HBox(12, badge, text, spacer, chevron);
        item.setAlignment(Pos.CENTER_LEFT);
        item.setPadding(new Insets(14, 12, 14, 12));
        item.getStyleClass().add("course-list-item");
        item.setOnMouseClicked(e -> select(index));
        item.setOnMouseEntered(e -> {
            if (!item.getUserData().equals("selected")) item.getStyleClass().add("course-list-item-hover");
        });
        item.setOnMouseExited(e -> {
            item.getStyleClass().remove("course-list-item-hover");
        });
        item.setUserData("normal");
        return item;
    }

    private void select(int index) {
        for (int i = 0; i < listItems.size(); i++) {
            boolean sel = i == index;
            listItems.get(i).getStyleClass().remove("course-list-item-selected");
            if (sel) listItems.get(i).getStyleClass().add("course-list-item-selected");
            listItems.get(i).setUserData(sel ? "selected" : "normal");
        }
        showCourse(courses.get(index));
    }

    // =====================================================================
    //  Right details
    // =====================================================================
    private void showCourse(Course c) {
        detail.getChildren().clear();

        // header
        Label bigCode = badge(c.code(), "#1b5fd6", 20);
        bigCode.setPadding(new Insets(16, 18, 16, 18));

        Label title = new Label(c.name());
        title.getStyleClass().add("course-title");
        VBox titleBox = new VBox(title);
        titleBox.setAlignment(Pos.CENTER_LEFT);

        HBox header = new HBox(16, bigCode, titleBox);
        header.setAlignment(Pos.CENTER_LEFT);

        detail.getChildren().addAll(header, new Separator0(), overviewContent(c));
    }

    private VBox overviewContent(Course c) {
        // --- course details box
        Label dTitle = sectionTitle("Course Details");
        GridPane g = new GridPane();
        g.setHgap(40);
        g.setVgap(14);
        String[][] rows = {
                {"Course Code", c.code()},
                {"Course Name", c.name()},
                {"Lecturer", c.lecturer()},
                {"Theory / Practical", theoryPractical(c.description())}};
        for (int i = 0; i < rows.length; i++) {
            Label k = new Label(rows[i][0]);
            k.getStyleClass().add("course-detail-key");
            Label v = new Label(rows[i][1]);
            v.getStyleClass().add("course-detail-value");
            g.add(k, 0, i);
            g.add(v, 1, i);
        }
        VBox detailsBox = new VBox(16, dTitle, g);
        detailsBox.getStyleClass().add("course-soft-box");

        // --- materials
        Label mTitle = new Label("\uD83D\uDCC1  Course Materials");
        mTitle.getStyleClass().add("course-material-title");

        VBox content = new VBox(22, detailsBox, new VBox(12, mTitle, materialsTable(c)));
        content.setPadding(new Insets(18, 0, 0, 0));
        return content;
    }

    private GridPane materialsTable(Course c) {
        GridPane t = new GridPane();
        t.setVgap(0);
        double[] pct = {5, 40, 12, 22, 21};
        for (double p : pct) {
            ColumnConstraints cc = new ColumnConstraints();
            cc.setPercentWidth(p);
            t.getColumnConstraints().add(cc);
        }

        String[] heads = {"#", "Title", "Type", "Uploaded Date", "Action"};
        for (int i = 0; i < heads.length; i++) {
            Label h = new Label(heads[i]);
            h.setMaxWidth(Double.MAX_VALUE);
            h.getStyleClass().add("course-material-header");
            t.add(h, i, 0);
        }

        int r = 1;
        for (Material m : c.materials()) {
            Label no = styled(String.valueOf(r), "course-material-cell");
            Label ttl = styled("\uD83D\uDCC4  " + m.title(), "course-material-cell");
            Label type = styled(m.type(), "course-material-cell");
            Label date = styled(m.date(), "course-material-cell");

            boolean link = m.type().equalsIgnoreCase("Link")
                    || (m.filePath() != null && (m.filePath().startsWith("http://") || m.filePath().startsWith("https://")));
            Button action = new Button(link ? "View \u2197" : "\u2B07 Open");
            action.getStyleClass().add("course-material-action");
            action.setOnAction(event -> openMaterial(m));
            if (m.filePath() == null || m.filePath().isBlank() || "-".equals(m.filePath())) {
                action.setDisable(true);
                action.setTooltip(new javafx.scene.control.Tooltip("No file path saved for this material."));
            }
            HBox actionCell = new HBox(action);
            actionCell.setAlignment(Pos.CENTER_LEFT);
            actionCell.getStyleClass().add("course-material-action-cell");

            t.add(no, 0, r);
            t.add(ttl, 1, r);
            t.add(type, 2, r);
            t.add(date, 3, r);
            t.add(actionCell, 4, r);
            for (Label l : new Label[]{no, ttl, type, date}) l.setMaxWidth(Double.MAX_VALUE);
            r++;
        }
        return t;
    }

    // =====================================================================
    //  Small helpers
    // =====================================================================
    private Label badge(String text, String color, int fontSize) {
        Label l = new Label(text);
        l.getStyleClass().addAll("course-badge", colorClass(color));
        if (fontSize >= 20) l.getStyleClass().add("course-badge-large");
        l.setMinWidth(Region.USE_PREF_SIZE);
        return l;
    }

    private Label sectionTitle(String s) {
        Label l = new Label(s);
        l.getStyleClass().add("course-section-title");
        return l;
    }

    private Label styled(String text, String style) {
        Label l = new Label(text);
        l.getStyleClass().add(style);
        return l;
    }

    private String colorClass(String color) {
        return switch (color) {
            case "#16b57a" -> "course-badge-green";
            case "#8a3ffc" -> "course-badge-purple";
            case "#f57c1f" -> "course-badge-orange";
            case "#0ea5b7" -> "course-badge-cyan";
            case "#e8336d" -> "course-badge-pink";
            default -> "course-badge-blue";
        };
    }

    /** thin divider line under the header */
    private static class Separator0 extends Region {
        Separator0() {
            setMinHeight(1);
            setPrefHeight(1);
            setMaxHeight(1);
            getStyleClass().add("course-divider");
        }
    }

    private void loadStudentContext() {
        String regNo = Session.getRegNo();
        if (regNo == null || regNo.isBlank()) {
            loadError = "Please login to view your courses.";
            return;
        }
        try {
            List<Map<String, Object>> rows = Db.query(
                    "SELECT status, department_id, level, semester FROM undergraduates WHERE reg_no = ?", regNo);
            if (!rows.isEmpty()) {
                batchMissed = "Batch Missed".equalsIgnoreCase(value(rows.get(0), "status"));
                studentDepartment = value(rows.get(0), "department_id");
                studentLevel = Integer.parseInt(value(rows.get(0), "level"));
                studentSemester = Integer.parseInt(value(rows.get(0), "semester"));
            }
        } catch (Exception ex) {
            loadError = "Unable to load student details: " + ex.getMessage();
        }
    }

    private List<String> availableSemesters() {
        List<String> semesters = new ArrayList<>();
        for (int level = 1; level <= studentLevel; level++) {
            int lastSemester = level == studentLevel ? studentSemester : 2;
            for (int semester = 1; semester <= lastSemester; semester++) {
                semesters.add(level + "." + semester);
            }
        }
        return semesters;
    }

    private void loadData(String selectedBatch) {
        courses.clear();
        loadError = null;
        String regNo = Session.getRegNo();
        if (regNo == null || regNo.isBlank()) {
            loadError = "Please login to view your courses.";
            return;
        }
        if (batchMissed && (selectedBatch == null || selectedBatch.isBlank())) return;

        String sql = batchMissed
                ? "SELECT DISTINCT c.course_code, c.course_name, c.theory_practical, "
                    + "COALESCE(u.full_name, '-') AS lecturer_name "
                    + "FROM courses c "
                    + "LEFT JOIN lecturers l ON l.lecturer_id = c.lecturer_id "
                    + "LEFT JOIN users u ON u.user_id = l.user_id "
                    + "WHERE c.department_id = ? AND CAST(c.level AS UNSIGNED) = ? "
                    + "AND CAST(c.semester AS UNSIGNED) = ? ORDER BY c.course_code"
                : "SELECT DISTINCT c.course_code, c.course_name, c.theory_practical, "
                + "COALESCE(u.full_name, '-') AS lecturer_name "
                + "FROM undergraduates ug "
                + "JOIN enrollments e ON e.reg_no = ug.reg_no "
                + "JOIN courses c ON c.course_code = e.course_code "
                + "LEFT JOIN lecturers l ON l.lecturer_id = c.lecturer_id "
                + "LEFT JOIN users u ON u.user_id = l.user_id "
                + "WHERE ug.reg_no = ? ORDER BY c.course_code";

        try {
            List<Map<String, Object>> rows = batchMissed
                    ? Db.query(sql, studentDepartment, semesterPart(selectedBatch, 0),
                            semesterPart(selectedBatch, 1))
                    : Db.query(sql, regNo);
            for (Map<String, Object> row : rows) {
                String code = value(row, "course_code");
                courses.add(new Course(
                        code,
                        value(row, "course_name"),
                        value(row, "lecturer_name"),
                        theoryPracticalDescription(value(row, "theory_practical")),
                        color(code),
                        loadMaterials(code)));
            }
        } catch (Exception ex) {
            loadError = "Unable to load course details: " + ex.getMessage();
        }
    }

    private static int semesterPart(String selectedSemester, int part) {
        String[] values = selectedSemester.split("\\.", -1);
        return Integer.parseInt(values[part]);
    }

    private List<Material> loadMaterials(String courseCode) {
        try {
            List<Map<String, Object>> rows = Db.query(
                    "SELECT title, type, uploaded_date, file_path "
                            + "FROM course_materials WHERE course_code = ? "
                            + "ORDER BY uploaded_date DESC, material_id DESC", courseCode);
            List<Material> result = new ArrayList<>();
            for (Map<String, Object> row : rows) {
                result.add(new Material(
                        value(row, "title"),
                        value(row, "type"),
                        value(row, "uploaded_date"),
                        value(row, "file_path")));
            }
            return result;
        } catch (Exception ex) {
            System.err.println("Unable to load course materials for " + courseCode + ": " + ex.getMessage());
            return List.of();
        }
    }

    private void openMaterial(Material material) {
        String path = material.filePath();
        if (path == null || path.isBlank() || "-".equals(path)) {
            showMaterialError("No file path has been saved for this material.");
            return;
        }
        try {
            if (path.startsWith("https://") || path.startsWith("http://")) {
                if (!Desktop.isDesktopSupported()) {
                    showMaterialError("This computer does not support opening web links automatically.");
                    return;
                }
                Desktop.getDesktop().browse(URI.create(path));
            } else {
                File file = new File(path);
                if (!file.isAbsolute()) {
                    file = new File(System.getProperty("user.dir"), path);
                }
                if (!file.exists()) {
                    showMaterialError("File not found:\n" + file.getAbsolutePath()
                            + "\n\nCheck the file_path value in course_materials.");
                    return;
                }
                if (!Desktop.isDesktopSupported()) {
                    showMaterialError("This computer does not support opening files automatically.");
                    return;
                }
                Desktop.getDesktop().open(file);
            }
        } catch (Exception ex) {
            showMaterialError("Could not open material: " + ex.getMessage());
        }
    }

    private void showMaterialError(String message) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("Course Material");
        alert.setHeaderText("Unable to open material");
        alert.setContentText(message);
        alert.showAndWait();
    }

    private static String value(Map<String, Object> row, String key) {
        Object value = row.get(key);
        return value == null ? "-" : value.toString();
    }

    private static String theoryPracticalDescription(String type) {
        if ("BOTH".equalsIgnoreCase(type)
                || "THEORY/PRACTICAL".equalsIgnoreCase(type)) {
            return "Theory and practical course.";
        }
        if ("PRACTICAL".equalsIgnoreCase(type)) return "Practical course.";
        if ("THEORY".equalsIgnoreCase(type)) return "Theory course.";
        if ("TUTORIAL".equalsIgnoreCase(type)) return "Tutorial course.";
        return "Course details provided by the department.";
    }

    private static String theoryPractical(String description) {
        if (description.startsWith("Theory and practical")) return "Theory / Practical";
        if (description.startsWith("Practical")) return "Practical";
        if (description.startsWith("Theory")) return "Theory";
        return "-";
    }

    private static String color(String code) {
        String[] colors = {"#1b5fd6", "#16b57a", "#8a3ffc", "#f57c1f", "#0ea5b7", "#e8336d"};
        return colors[Math.floorMod(code.hashCode(), colors.length)];
    }
}