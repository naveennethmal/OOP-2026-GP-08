
package Admin;

import Database.Db;

import javafx.scene.Node;
import javafx.scene.control.TextField;
import javafx.scene.layout.VBox;

import java.util.List;
import java.util.Map;

public class DepartmentsView extends CrudView {

    private final TextField departmentName = new TextField();

    public DepartmentsView() {
        departmentName.setPromptText("Enter department name");

        departmentName.setStyle(
                "-fx-background-color: white;" +
                        "-fx-text-fill: #111827;" +
                        "-fx-prompt-text-fill: #64748B;" +
                        "-fx-border-color: #CBD5E1;" +
                        "-fx-border-radius: 6px;" +
                        "-fx-background-radius: 6px;" +
                        "-fx-font-size: 13px;" +
                        "-fx-padding: 9px;"
        );

        departmentName.setMaxWidth(Double.MAX_VALUE);

        build();
    }

    // Page title
    @Override
    protected String title() {
        return "Department Management";
    }

    // Add button
    @Override
    protected String addButtonText() {
        return "+ Add Department";
    }

    // Enable search
    @Override
    protected String searchColumn() {
        return "department_name";
    }

    @Override
    protected String searchPrompt() {
        return "Search department name...";
    }

    // Table columns
    @Override
    protected String[][] columns() {
        return new String[][]{
                {"Department ID", "department_id"},
                {"Department Name", "department_name"}
        };
    }

    // Load departments from database
    @Override
    protected String selectSql() {
        return "SELECT department_id, department_name " +
                "FROM departments " +
                "ORDER BY department_id";
    }

    // Add/Edit form
    @Override
    protected Node form() {
        return new VBox(
                12,
                field("Department Name", departmentName)
        );
    }

    // Fill form when Edit is clicked
    @Override
    protected void fill(Map<String, Object> row) {
        departmentName.setText(
                value(row, "department_name")
        );
    }

    // Clear form
    @Override
    protected void reset() {
        departmentName.clear();
    }

    // Add a new department
    @Override
    protected void create() throws Exception {
        String name = need(
                departmentName.getText(),
                "Department Name"
        );

        checkDuplicate(name, null);

        Db.update(
                "INSERT INTO departments (department_name) VALUES (?)",
                name
        );
    }

    // Update an existing department
    @Override
    protected void save() throws Exception {
        if (selected == null) {
            throw new IllegalArgumentException(
                    "Select a department to edit."
            );
        }

        String name = need(
                departmentName.getText(),
                "Department Name"
        );

        Object departmentId = selected.get("department_id");

        checkDuplicate(name, departmentId);

        Db.update(
                "UPDATE departments SET department_name = ? " +
                        "WHERE department_id = ?",
                name,
                departmentId
        );
    }

    // Prevent duplicate department names
    private void checkDuplicate(
            String name,
            Object currentId
    ) throws Exception {

        String sql =
                "SELECT department_id FROM departments " +
                        "WHERE LOWER(TRIM(department_name)) = LOWER(TRIM(?))";

        List<Map<String, Object>> rows;

        if (currentId == null) {
            rows = Db.query(sql, name);
        } else {
            rows = Db.query(
                    sql + " AND department_id <> ?",
                    name,
                    currentId
            );
        }

        if (!rows.isEmpty()) {
            throw new IllegalArgumentException(
                    "This department already exists."
            );
        }
    }

    // Delete a department
    @Override
    protected void delete(
            Map<String, Object> row
    ) throws Exception {

        Object departmentId = row.get("department_id");

        // Avoid deleting a department that is still in use.
        List<Map<String, Object>> users = Db.query(
                "SELECT user_id FROM users WHERE department_id = ? LIMIT 1",
                departmentId
        );

        List<Map<String, Object>> undergraduates = Db.query(
                "SELECT user_id FROM undergraduates " +
                        "WHERE department_id = ? LIMIT 1",
                departmentId
        );

        List<Map<String, Object>> lecturers = Db.query(
                "SELECT user_id FROM lecturers " +
                        "WHERE department_id = ? LIMIT 1",
                departmentId
        );

        List<Map<String, Object>> officers = Db.query(
                "SELECT user_id FROM technical_officers " +
                        "WHERE department_id = ? LIMIT 1",
                departmentId
        );

        List<Map<String, Object>> courses = Db.query(
                "SELECT course_code FROM courses " +
                        "WHERE department_id = ? LIMIT 1",
                departmentId
        );

        if (!users.isEmpty()
                || !undergraduates.isEmpty()
                || !lecturers.isEmpty()
                || !officers.isEmpty()
                || !courses.isEmpty()) {

            throw new IllegalArgumentException(
                    "Cannot delete this department because it is " +
                            "being used by users, students, staff, or courses."
            );
        }

        Db.update(
                "DELETE FROM departments WHERE department_id = ?",
                departmentId
        );
    }
}
