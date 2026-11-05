package Admin;

import Database.Db;
import Common.AcademicResults;
import javafx.collections.FXCollections;
import javafx.scene.Node;
import javafx.scene.control.ComboBox;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.scene.layout.VBox;

import java.sql.SQLException;
import java.util.Map;

public class UsersView extends CrudView {
    private static final String[] DEPARTMENT_NAMES = {
            "Engineering Technology",
            "Information and Communication Technology",
            "Biosystems Technology",
            "Multidisciplinary Studies"
    };

    private final TextField registrationNo = new TextField();
    private final TextField fullName = new TextField();
    private final TextField email = new TextField();
    private final TextField phone = new TextField();
    private final TextField address = new TextField();
    private final PasswordField password = new PasswordField();
    private final PasswordField confirmPassword = new PasswordField();
    private final ComboBox<String> role = new ComboBox<>(FXCollections.observableArrayList(
            "Admin", "Lecturer", "Technical Officer", "Undergraduate"));
    private final TextField batch = new TextField();
    private final VBox batchField = field("Batch", batch);
    private final ComboBox<String> status = new ComboBox<>(
            FXCollections.observableArrayList("Active", "Inactive", "Graduated", "Suspended", "Batch Missed"));
    private final VBox statusField = field("Status", status);
    private final ComboBox<String> level = new ComboBox<>(
            FXCollections.observableArrayList("1", "2", "3", "4"));
    private final ComboBox<String> semester = new ComboBox<>(
            FXCollections.observableArrayList("1", "2"));
    private final ComboBox<Item> department = new ComboBox<>();
    private boolean loadingUser;

    public UsersView() {
        role.setValue("Undergraduate");
        password.setPromptText("Required for a new user");
        ensureUndergraduateColumns();
        ensureUndergraduateStatusColumn();
        ensureUserDepartmentColumn();
        ensureDepartments();
        department.setItems(items(
                        "SELECT department_id k, department_name l FROM departments "
                        + "WHERE department_name IN (?, ?, ?, ?) ORDER BY department_name",
                        (Object[]) DEPARTMENT_NAMES));
        batch.setPromptText("Enter batch");
        batch.setPrefWidth(280);
        status.setValue("Active");
        status.valueProperty().addListener((obs, oldStatus, newStatus) -> {
            if (!loadingUser && "Batch Missed".equals(newStatus) && !"Batch Missed".equals(oldStatus)) {
                moveDownOneLevel();
            }
        });
        role.valueProperty().addListener((o, oldRole, newRole) -> updateBatchVisibility());
        updateBatchVisibility();
        build();
        configureTableWidths();
    }

    @Override
    protected String title() {
        return "User profiles";
    }

    @Override
    protected String searchColumn() {
        return "reg_no";
    }

    @Override
    protected String searchPrompt() {
        return "Search users";
    }

    @Override
    protected String[][] columns() {
        return new String[][]{
                {"Registration No", "reg_no"}, {"Full name", "full_name"},
                {"Role", "role"}, {"Batch", "batch"}, {"Department", "department_name"},
                {"Level", "level"}, {"Semester", "semester"}, {"Status", "status"},
                {"Email", "email"}, {"Phone", "phone"}
        };
    }

    private void configureTableWidths() {
        double[] widths = {150, 220, 170, 120, 270, 90, 110, 140, 260, 170};
        table.setColumnResizePolicy(TableView.UNCONSTRAINED_RESIZE_POLICY);
        for (int index = 0; index < widths.length; index++) {
            table.getColumns().get(index).setMinWidth(widths[index]);
            table.getColumns().get(index).setPrefWidth(widths[index]);
        }
    }

    @Override
    protected String selectSql() {
        return "SELECT u.user_id, COALESCE(ug.reg_no, u.user_name) reg_no, "
                + "COALESCE(ug.level, '') level, COALESCE(ug.semester, '') semester, "
                + "COALESCE(ug.department_id, u.department_id) department_id, COALESCE(ug.batch, '') batch, "
                + "COALESCE(ug.status, 'Active') status, "
                + "d.department_name, "
                + "u.full_name, u.role, u.email, u.phone, u.address "
                + "FROM users u LEFT JOIN undergraduates ug ON ug.user_id = u.user_id "
                + "LEFT JOIN departments d ON d.department_id = COALESCE(ug.department_id, u.department_id) "
                + "ORDER BY u.user_id";
    }

    @Override
    protected Node form() {
        return new VBox(10,
                field("Registration No", registrationNo),
                field("Password", password),
                field("Confirm Password", confirmPassword),
                field("Full name", fullName),
                field("Role", role),
                batchField,
                field("Level", level),
                field("Semester", semester),
                statusField,
                field("Department", department),
                field("Email", email),
                field("Phone", phone),
                field("Address", address));
    }

    @Override
    protected void fill(Map<String, Object> row) {
        loadingUser = true;
        try {
            registrationNo.setText(value(row, "reg_no"));
            fullName.setText(value(row, "full_name"));
            email.setText(value(row, "email"));
            phone.setText(value(row, "phone"));
            address.setText(value(row, "address"));
            role.setValue(value(row, "role"));
            batch.setText(value(row, "batch"));
            status.setValue(value(row, "status"));
            level.setValue(value(row, "level"));
            semester.setValue(value(row, "semester"));
            pick(department, row.get("department_id"));
            password.clear();
            confirmPassword.clear();
        } finally {
            loadingUser = false;
        }
    }

    @Override
    protected void reset() {
        registrationNo.clear();
        fullName.clear();
        email.clear();
        phone.clear();
        address.clear();
        password.clear();
        confirmPassword.clear();
        role.setValue("Undergraduate");
        batch.clear();
        status.setValue("Active");
        level.setValue(null);
        semester.setValue(null);
        department.setValue(null);
    }

    @Override
    protected void save() throws Exception {
        if (selected == null) {
            throw new IllegalArgumentException("Click New to add a user.");
        }
        persist(false);
    }

    @Override
    protected void create() throws Exception {
        if (selected != null) {
            throw new IllegalArgumentException("Click New to start a new user.");
        }
        persist(true);
    }

    private void persist(boolean creating) throws Exception {
        String regNo = need(registrationNo.getText(), "Registration No");
        String name = need(fullName.getText(), "Full name");
        String mail = need(email.getText(), "Email");

        if (!mail.matches("^[^@\\s]+@[^@\\s]+\\.[^@\\s]+$")) {
            throw new IllegalArgumentException("Email format is invalid.");
        }
        if (!phone.getText().isBlank()
                && !phone.getText().matches("^[0-9+\\- ]{9,15}$")) {
            throw new IllegalArgumentException("Phone number is invalid.");
        }
        if (role.getValue() == null) {
            throw new IllegalArgumentException("Select a role.");
        }
        if (creating && password.getText().isBlank()) {
            throw new IllegalArgumentException("Password is required.");
        }
        if (!password.getText().isBlank()
                && !password.getText().equals(confirmPassword.getText())) {
            throw new IllegalArgumentException("Passwords do not match.");
        }
        boolean undergraduate = "Undergraduate".equals(role.getValue());
        String selectedBatch = batch.getText().trim();
        if (!"Admin".equals(role.getValue()) && department.getValue() == null) {
            throw new IllegalArgumentException("Select a department.");
        }
        if (undergraduate && (level.getValue() == null || semester.getValue() == null)) {
            throw new IllegalArgumentException("Select level and semester.");
        }
        if (undergraduate && selectedBatch.isBlank()) {
            throw new IllegalArgumentException("Enter a batch.");
        }
        if (undergraduate && status.getValue() == null) {
            throw new IllegalArgumentException("Select a student status.");
        }

        Object currentId = selected == null ? null : selected.get("user_id");
        String previousRegNo = selected == null ? null : value(selected, "reg_no");
        ensureUnique(regNo, mail, currentId);
        if (!creating && undergraduate && previousRegNo != null
                && "Undergraduate".equals(value(selected, "role"))
                && !value(selected, "level").isBlank() && !value(selected, "semester").isBlank()) {
            ensureSemesterProgression(selected, level.getValue(), semester.getValue());
        }

        String phoneValue = blankToNull(phone.getText());
        String addressValue = blankToNull(address.getText());
        if (creating) {
            String userPassword = need(password.getText(), "Password");
            long userId = Db.insertAndGetId("INSERT INTO users "
                            + "(user_name,password,full_name,email,phone,address,role,department_id) "
                            + "VALUES(?,?,?,?,?,?,?,?)",
                    regNo, userPassword, name, mail, phoneValue, addressValue, role.getValue(),
                    department.getValue() == null ? null : department.getValue().key());
            if (undergraduate) {
                Db.update("INSERT INTO undergraduates "
                                + "(user_id,reg_no,level,semester,department_id,batch,status) VALUES(?,?,?,?,?,?,?)",
                        userId, regNo, level.getValue(), semester.getValue(),
                        department.getValue().key(), selectedBatch, status.getValue());
                syncEnrollments(regNo, department.getValue().key(), level.getValue(),
                        semester.getValue(), selectedBatch);
            } else {
                syncRoleTable(userId, regNo, role.getValue(), department.getValue().key());
            }
            return;
        }
        Db.update("UPDATE users SET user_name=?,full_name=?,email=?,phone=?,address=?,role=? "
                        + ",department_id=? WHERE user_id=?",
                regNo, name, mail, phoneValue, addressValue, role.getValue(),
                department.getValue() == null ? null : department.getValue().key(), currentId);
        if (undergraduate) {
            if (previousRegNo != null && !previousRegNo.equals(regNo)) {
                Db.update("DELETE FROM enrollments WHERE reg_no=?", previousRegNo);
            }
            int updated = Db.update("UPDATE undergraduates SET reg_no=?,level=?,semester=?,department_id=?,batch=?,status=? "
                            + "WHERE user_id=?",
                    regNo, level.getValue(), semester.getValue(), department.getValue().key(), selectedBatch,
                    status.getValue(), currentId);
            if (updated == 0) {
                Db.update("INSERT INTO undergraduates "
                                + "(user_id,reg_no,level,semester,department_id,batch,status) VALUES(?,?,?,?,?,?,?)",
                        currentId, regNo, level.getValue(), semester.getValue(),
                        department.getValue().key(), selectedBatch, status.getValue());
            }
            syncEnrollments(regNo, department.getValue().key(), level.getValue(),
                semester.getValue(), selectedBatch);
        } else {
            Db.update("DELETE FROM enrollments WHERE reg_no=?", regNo);
            if (previousRegNo != null && !previousRegNo.equals(regNo)) {
                Db.update("DELETE FROM enrollments WHERE reg_no=?", previousRegNo);
            }
            Db.update("DELETE FROM undergraduates WHERE user_id=?", currentId);
        }
        syncRoleTable(currentId, regNo, role.getValue(),
                department.getValue() == null ? null : department.getValue().key());
        updatePasswordIfProvided(currentId);
    }

    private static void ensureSemesterProgression(Map<String, Object> current,
                                                  String nextLevel, String nextSemester) {
        int currentTerm = termNumber(value(current, "level"), value(current, "semester"));
        int nextTerm = termNumber(nextLevel, nextSemester);
        if (nextTerm <= currentTerm) return;
        if (nextTerm != currentTerm + 1) {
            throw new IllegalArgumentException("Students can advance only one semester at a time.");
        }
    }

    private static int termNumber(String level, String semester) {
        return (Integer.parseInt(level) - 1) * 2 + Integer.parseInt(semester);
    }

    private void moveDownOneLevel() {
        if (level.getValue() == null || semester.getValue() == null) {
            level.setValue("1");
            semester.setValue("1");
            return;
        }
        int currentLevel = Integer.parseInt(level.getValue());
        level.setValue(String.valueOf(Math.max(1, currentLevel - 1)));
    }

    private static void syncEnrollments(String regNo, Object departmentId,
                                        String level, String semester, String academicYear)
            throws Exception {
        AcademicResults.ensureRetakeTable();
        var failedCourseCodes = AcademicResults.failedCourseCodes(regNo);
        Db.update("DELETE FROM enrollments WHERE reg_no=?", regNo);
        Db.update("""
                INSERT INTO enrollments (reg_no, course_code, academic_year)
                SELECT ?, c.course_code, ?
                FROM courses c
                WHERE c.department_id = ?
                  AND CAST(c.level AS UNSIGNED) = CAST(? AS UNSIGNED)
                  AND CAST(c.semester AS UNSIGNED) = CAST(? AS UNSIGNED)
                """, regNo, academicYear, departmentId, level, semester);
        for (String courseCode : failedCourseCodes) {
            Db.update("""
                    INSERT INTO student_retake_courses (reg_no, course_code)
                    VALUES (?, ?)
                    ON DUPLICATE KEY UPDATE course_code = VALUES(course_code)
                    """, regNo, courseCode);
            Db.update("""
                    INSERT INTO enrollments (reg_no, course_code, academic_year)
                    SELECT ?, ?, ?
                    FROM DUAL
                    WHERE NOT EXISTS (
                        SELECT 1 FROM enrollments WHERE reg_no = ? AND course_code = ?
                    )
                    """, regNo, courseCode, academicYear, regNo, courseCode);
        }
    }

    private static void syncRoleTable(Object userId, String employeeNo, String role, String departmentId)
            throws Exception {
        if ("Lecturer".equals(role)) {
            Db.update("DELETE FROM technical_officers WHERE user_id=?", userId);
            int updated = Db.update("UPDATE lecturers SET employee_no=?,department_id=? WHERE user_id=?",
                    employeeNo, departmentId, userId);
            if (updated == 0) {
                Db.update("INSERT INTO lecturers(user_id,employee_no,department_id) VALUES(?,?,?)",
                        userId, employeeNo, departmentId);
            }
        } else if ("Technical Officer".equals(role)) {
            Db.update("DELETE FROM lecturers WHERE user_id=?", userId);
            int updated = Db.update("UPDATE technical_officers SET employee_no=?,department_id=? "
                            + "WHERE user_id=?",
                    employeeNo, departmentId, userId);
            if (updated == 0) {
                Db.update("INSERT INTO technical_officers(user_id,employee_no,department_id) VALUES(?,?,?)",
                        userId, employeeNo, departmentId);
            }
        } else {
            Db.update("DELETE FROM lecturers WHERE user_id=?", userId);
            Db.update("DELETE FROM technical_officers WHERE user_id=?", userId);
        }
    }

    private void updatePasswordIfProvided(Object currentId) throws Exception {
        if (!password.getText().isBlank()) {
            Db.update("UPDATE users SET `password`=? WHERE user_id=?", password.getText(), currentId);
            password.clear();
            confirmPassword.clear();
        }
    }

    private static void ensureUnique(String regNo, String mail, Object currentId)
            throws Exception {
        String sql = "SELECT u.user_id FROM users u LEFT JOIN undergraduates ug ON ug.user_id=u.user_id "
                + "WHERE (u.user_name=? OR ug.reg_no=? OR u.email=?)";
        var rows = currentId == null
                ? Db.query(sql, regNo, regNo, mail)
                : Db.query(sql + " AND u.user_id<>?", regNo, regNo, mail, currentId);
        if (!rows.isEmpty()) {
            throw new IllegalArgumentException("Registration No or email already exists.");
        }
    }

    private static String value(Map<String, Object> row, String key) {
        Object value = row.get(key);
        return value == null ? "" : value.toString();
    }

    private static String blankToNull(String text) {
        return text == null || text.isBlank() ? null : text.trim();
    }

    @Override
    protected void delete(Map<String, Object> row) throws Exception {
        Object userId = row.get("user_id");
        var lecturers = Db.query("SELECT lecturer_id FROM lecturers WHERE user_id=?", userId);
        for (Map<String, Object> lecturer : lecturers) {
            Object lecturerId = lecturer.get("lecturer_id");
            Db.update("DELETE FROM marks WHERE lecturer_id=?", lecturerId);
            Db.update("DELETE FROM time_table WHERE lecturer_id=?", lecturerId);
            Db.update("UPDATE courses SET lecturer_id=NULL WHERE lecturer_id=?", lecturerId);
        }
        Db.update("DELETE FROM technical_officers WHERE user_id=?", userId);
        Db.update("DELETE FROM users WHERE user_id=?", userId);
    }

    private static void ensureUndergraduateColumns() {
        addColumn("level");
        addColumn("semester");
    }

    private static void ensureUndergraduateStatusColumn() {
        try {
            Db.update("ALTER TABLE undergraduates MODIFY COLUMN status VARCHAR(30) NULL");
        } catch (Exception ex) {
            throw new IllegalStateException("Cannot prepare undergraduate statuses.", ex);
        }
    }

    private static void ensureUserDepartmentColumn() {
        try {
            Db.update("ALTER TABLE users ADD COLUMN department_id INT NULL");
        } catch (Exception ex) {
            if (!isDuplicateColumn(ex)) {
                throw new IllegalStateException("Cannot prepare the user department column.", ex);
            }
        }
    }

    private static void ensureDepartments() {
        try {
            for (String departmentName : DEPARTMENT_NAMES) {
                Db.update("INSERT INTO departments (department_name) "
                        + "SELECT ? FROM DUAL WHERE NOT EXISTS "
                        + "(SELECT 1 FROM departments WHERE department_name=?)",
                        departmentName, departmentName);
            }
            Db.update("DELETE d FROM departments d "
                    + "WHERE d.department_name NOT IN (?, ?, ?, ?) "
                    + "AND NOT EXISTS (SELECT 1 FROM users u WHERE u.department_id=d.department_id) "
                    + "AND NOT EXISTS (SELECT 1 FROM undergraduates ug "
                    + "WHERE ug.department_id=d.department_id) "
                    + "AND NOT EXISTS (SELECT 1 FROM lecturers l "
                    + "WHERE l.department_id=d.department_id) "
                    + "AND NOT EXISTS (SELECT 1 FROM technical_officers t "
                    + "WHERE t.department_id=d.department_id) "
                    + "AND NOT EXISTS (SELECT 1 FROM courses c "
                    + "WHERE c.department_id=d.department_id)",
                    (Object[]) DEPARTMENT_NAMES);
        } catch (Exception ex) {
            throw new IllegalStateException("Cannot prepare the department list.", ex);
        }
    }

    private static void addColumn(String column) {
        try {
            Db.update("ALTER TABLE undergraduates ADD COLUMN " + column + " VARCHAR(30) NULL");
        } catch (Exception ex) {
            if (!isDuplicateColumn(ex)) {
                throw new IllegalStateException("Cannot prepare the undergraduate table.", ex);
            }
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

    private void updateBatchVisibility() {
        boolean undergraduate = "Undergraduate".equals(role.getValue());
        batchField.setVisible(undergraduate);
        batchField.setManaged(undergraduate);
        statusField.setVisible(undergraduate);
        statusField.setManaged(undergraduate);
    }
}
