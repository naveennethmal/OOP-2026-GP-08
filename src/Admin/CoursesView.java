package Admin;

import Database.Db;
import javafx.collections.FXCollections;
import javafx.scene.Node;
import javafx.scene.control.ComboBox;
import javafx.scene.control.TextField;
import javafx.scene.layout.VBox;

import java.sql.SQLException;
import java.util.Map;

public class CoursesView extends CrudView {
    private static final String[] DEPARTMENT_NAMES = {
            "Biosystems Technology",
            "Engineering Technology",
            "Information and Communication Technology",
            "Multidisciplinary Studies"
    };

    private final TextField code = new TextField();
    private final TextField name = new TextField();
    private final ComboBox<String> credits = new ComboBox<>(
            FXCollections.observableArrayList("1", "2", "3", "4", "5", "NGPA"));
    private final ComboBox<String> type = new ComboBox<>(
            FXCollections.observableArrayList(
                    "THEORY", "PRACTICAL", "THEORY/PRACTICAL", "TUTORIAL"));
    private final ComboBox<String> semester = new ComboBox<>(
            FXCollections.observableArrayList("1", "2"));
    private final ComboBox<String> level = new ComboBox<>(
            FXCollections.observableArrayList("1", "2", "3", "4"));
    private final ComboBox<Item> department = new ComboBox<>(
            items("SELECT department_id k, department_name l FROM departments "
                    + "WHERE department_name IN (?, ?, ?, ?) ORDER BY department_name",
                    (Object[]) DEPARTMENT_NAMES));
    private final ComboBox<Item> lecturer = new ComboBox<>(
            items("SELECT l.lecturer_id k, u.full_name l FROM lecturers l "
                    + "JOIN users u ON u.user_id = l.user_id ORDER BY u.full_name"));

    public CoursesView() {
        ensureCourseColumns();
        ensureCreditsColumn();
        build();
    }

    @Override
    protected String title() {
        return "Courses";
    }

    @Override
    protected String searchColumn() {
        return "course_code";
    }

    @Override
    protected String searchPrompt() {
        return "Search courses";
    }

    @Override
    protected String[][] columns() {
        return new String[][]{
                {"Code", "course_code"},
                {"Name", "course_name"},
                {"Credits", "credits"},
                {"Theory / Practical", "type_display"},
                {"Level", "level"},
                {"Sem", "semester"},
                {"Department", "department_name"},
                {"Lecturer", "lecturer_name"}
        };
    }

    @Override
    protected String selectSql() {
        return "SELECT c.course_code, c.course_name, c.credits, c.semester, "
                + "c.theory_practical, CASE c.theory_practical "
                + "WHEN 'THEORY' THEN 'Theory' WHEN 'PRACTICAL' THEN 'Practical' "
                + "WHEN 'THEORY/PRACTICAL' THEN 'Theory / Practical' "
                + "WHEN 'BOTH' THEN 'Theory / Practical' WHEN 'TUTORIAL' THEN 'Tutorial' "
                + "ELSE c.theory_practical END type_display, "
                + "c.department_id, c.level, c.lecturer_id, "
                + "d.department_name, COALESCE(u.full_name, '-') lecturer_name "
                + "FROM courses c "
                + "JOIN departments d ON d.department_id = c.department_id "
                + "LEFT JOIN lecturers l ON l.lecturer_id = c.lecturer_id "
                + "LEFT JOIN users u ON u.user_id = l.user_id "
                + "ORDER BY c.course_code";
    }

    @Override
    protected Node form() {
        return new VBox(10,
                field("Course code", code),
                field("Course name", name),
                field("Credits", credits),
                field("Type", type),
                field("Level", level),
                field("Semester", semester),
                field("Department", department),
                field("Lecturer", lecturer));
    }

    @Override
    protected void fill(Map<String, Object> row) {
        code.setText(value(row, "course_code"));
        code.setDisable(true);
        name.setText(value(row, "course_name"));
        credits.setValue(value(row, "credits"));
        type.setValue(value(row, "theory_practical"));
        semester.setValue(value(row, "semester"));
        level.setValue(value(row, "level"));
        pick(department, row.get("department_id"));
        pick(lecturer, row.get("lecturer_id"));
    }

    @Override
    protected void reset() {
        code.clear();
        code.setDisable(false);
        name.clear();
        credits.setValue(null);
        type.setValue(null);
        semester.setValue(null);
        level.setValue(null);
        department.setValue(null);
        lecturer.setValue(null);
    }

    @Override
    protected void save() throws Exception {
        String courseCode = need(code.getText(), "Course code").toUpperCase();
        String courseName = need(name.getText(), "Course name");

        String creditValue = credits.getValue();
        if (creditValue == null) {
            throw new IllegalArgumentException("Select credits.");
        }
        if (type.getValue() == null || semester.getValue() == null || level.getValue() == null
                || department.getValue() == null) {
            throw new IllegalArgumentException("Select type, semester, level and department.");
        }

        Object lecturerId = lecturer.getValue() == null ? null : lecturer.getValue().key();
        if (selected == null) {
            Db.update("INSERT INTO courses "
                            + "(course_code, department_id, course_name, credits, semester, theory_practical, level, lecturer_id) "
                            + "VALUES (?, ?, ?, ?, ?, ?, ?, ?)",
                    courseCode, department.getValue().key(), courseName, creditValue,
                    semester.getValue(), type.getValue(), level.getValue(), lecturerId);
        } else {
            Db.update("UPDATE courses SET course_name=?, credits=?, semester=?, theory_practical=?, "
                            + "level=?, department_id=?, lecturer_id=? WHERE course_code=?",
                    courseName, creditValue, semester.getValue(), type.getValue(), level.getValue(),
                    department.getValue().key(), lecturerId, courseCode);
        }
    }

    @Override
    protected void delete(Map<String, Object> row) throws Exception {
        Db.update("DELETE FROM courses WHERE course_code=?", row.get("course_code"));
    }

    private static String value(Map<String, Object> row, String key) {
        Object value = row.get(key);
        return value == null ? "" : value.toString();
    }

    private static void ensureCourseColumns() {
        try {
            Db.update("ALTER TABLE courses ADD COLUMN level VARCHAR(30) NULL");
        } catch (Exception ex) {
            if (!isDuplicateColumn(ex)) {
                throw new IllegalStateException("Cannot prepare the courses table.", ex);
            }
        }
        try {
            Db.update("ALTER TABLE courses ADD COLUMN lecturer_id INT NULL");
        } catch (Exception ex) {
            if (!isDuplicateColumn(ex)) {
                throw new IllegalStateException("Cannot prepare the courses table.", ex);
            }
        }
    }

    private static void ensureCreditsColumn() {
        try {
            Db.update("ALTER TABLE courses MODIFY COLUMN credits VARCHAR(10) NOT NULL");
        } catch (Exception ex) {
            throw new IllegalStateException("Cannot prepare the course credits column.", ex);
        }
    }

    private static boolean isDuplicateColumn(Exception ex) {
        Throwable cause = ex;
        while (cause != null) {
            if (cause instanceof SQLException sqlException && sqlException.getErrorCode() == 1060) {
                return true;
            }
            cause = cause.getCause();
        }
        return false;
    }
}
