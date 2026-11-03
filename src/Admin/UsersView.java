package Admin;

import Database.Db;

import javafx.collections.FXCollections;
import javafx.scene.Node;
import javafx.scene.control.ComboBox;
import javafx.scene.control.ListCell;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;

import java.sql.SQLException;
import java.time.Year;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

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

    private final ComboBox<String> role = new ComboBox<>(
            FXCollections.observableArrayList(
                    "Admin",
                    "Lecturer",
                    "Technical Officer",
                    "Undergraduate"
            )
    );

    private final ComboBox<String> level = new ComboBox<>(
            FXCollections.observableArrayList("1", "2", "3", "4")
    );

    private final ComboBox<String> semester = new ComboBox<>(
            FXCollections.observableArrayList("1", "2")
    );

    private final ComboBox<String> batch = new ComboBox<>();

    private final ComboBox<String> repeatStudent = new ComboBox<>(
            FXCollections.observableArrayList("No", "Yes")
    );

    private final ComboBox<Item> department = new ComboBox<>();

    private final VBox departmentField = field("Department", department);
    private final VBox levelField = field("Level", level);
    private final VBox semesterField = field("Semester", semester);
    private final VBox batchField = field("Batch", batch);
    private final VBox repeatField = field("Repeat Student", repeatStudent);

    public UsersView() {

        role.setValue("Undergraduate");

        registrationNo.setPromptText("Registration / Employee No");
        fullName.setPromptText("Full name");
        email.setPromptText("Email address");
        phone.setPromptText("Phone number");
        address.setPromptText("Address");

        password.setPromptText("Required for a new user");
        confirmPassword.setPromptText("Confirm password");

        role.setPromptText("Select role");
        department.setPromptText("Select department");
        level.setPromptText("Select level");
        semester.setPromptText("Select semester");
        batch.setPromptText("Select batch");
        repeatStudent.setPromptText("Select Yes or No");

        ensureUndergraduateColumns();
        ensureUserDepartmentColumn();
        ensureDepartments();

        department.setItems(items(
                "SELECT department_id AS k, department_name AS l " +
                        "FROM departments " +
                        "WHERE department_name IN (?, ?, ?, ?) " +
                        "ORDER BY department_name",
                (Object[]) DEPARTMENT_NAMES
        ));

        loadBatchOptions();

        styleCombo(role);
        styleCombo(level);
        styleCombo(semester);
        styleCombo(batch);
        styleCombo(repeatStudent);
        styleCombo(department);

        styleTextField(registrationNo);
        styleTextField(fullName);
        styleTextField(email);
        styleTextField(phone);
        styleTextField(address);
        styleTextField(password);
        styleTextField(confirmPassword);

        role.setMaxWidth(Double.MAX_VALUE);
        level.setMaxWidth(Double.MAX_VALUE);
        semester.setMaxWidth(Double.MAX_VALUE);
        batch.setMaxWidth(Double.MAX_VALUE);
        repeatStudent.setMaxWidth(Double.MAX_VALUE);
        department.setMaxWidth(Double.MAX_VALUE);

        role.setOnAction(e -> updateRoleFields());

        // TG/2024/2115 -> default batch 2024
        registrationNo.textProperty().addListener(
                (observable, oldValue, newValue) ->
                        updateBatchOptionsForRegistrationNo()
        );

        updateRoleFields();
        build();
    }
    // STYLING
    private <T> void styleCombo(ComboBox<T> combo) {

        combo.setStyle(
                "-fx-background-color: white;" +
                        "-fx-border-color: #CBD5E1;" +
                        "-fx-border-radius: 6;" +
                        "-fx-background-radius: 6;" +
                        "-fx-font-size: 13px;"
        );

        combo.setButtonCell(new ListCell<T>() {
            @Override
            protected void updateItem(T item, boolean empty) {
                super.updateItem(item, empty);

                setText(empty || item == null ? "" : itemLabel(item));
                setTextFill(Color.web("#111827"));
                setStyle("-fx-background-color: white;");
            }
        });

        combo.setCellFactory(list -> new ListCell<T>() {
            @Override
            protected void updateItem(T item, boolean empty) {
                super.updateItem(item, empty);

                setText(empty || item == null ? "" : itemLabel(item));
                setTextFill(Color.web("#111827"));
                setStyle("-fx-background-color: white;");
            }
        });
    }

    private String itemLabel(Object item) {
        if (item instanceof Item) {
            return ((Item) item).label();
        }
        return String.valueOf(item);
    }

    private void styleTextField(TextField textField) {
        textField.setStyle(
                "-fx-background-color: white;" +
                        "-fx-text-fill: #111827;" +
                        "-fx-prompt-text-fill: #64748B;" +
                        "-fx-border-color: #CBD5E1;" +
                        "-fx-border-radius: 6;" +
                        "-fx-background-radius: 6;" +
                        "-fx-font-size: 13px;"
        );
    }
    // TABLE CONFIGURATION
    @Override
    protected String title() {
        return "User Profiles";
    }

    @Override
    protected String addButtonText() {
        return "+ Add User";
    }

    @Override
    protected String searchColumn() {
        // Must match a real column alias returned by selectSql().
        return "search_text";
    }

    @Override
    protected String searchPrompt() {
        return "Search name, reg no, email, phone, role, batch...";
    }

    @Override
    protected String[] filterOptions() {
        return new String[]{
                "All Users",
                "Admin",
                "Lecturer",
                "Technical Officer",
                "Undergraduate",
                "Repeat Students",
                "Batch Missed Students"
        };
    }

    @Override
    protected String filterColumn() {
        return "filter_category";
    }

    @Override
    protected String[][] columns() {
        return new String[][]{
                {"Registration / Employee No", "reg_no"},
                {"Full Name", "full_name"},
                {"Role", "role"},
                {"Department", "department_name"},
                {"Batch", "batch"},
                {"Repeat Student", "repeat_status"},
                {"Level", "level"},
                {"Semester", "semester"},
                {"Email", "email"},
                {"Phone", "phone"}
        };
    }

    @Override
    protected String selectSql() {

        return "SELECT " +
                "u.user_id, " +
                "COALESCE(ug.reg_no, u.user_name) AS reg_no, " +
                "u.full_name, " +
                "u.role, " +
                "u.email, " +
                "u.phone, " +
                "u.address, " +
                "COALESCE(ug.department_id, u.department_id) AS department_id, " +
                "d.department_name, " +
                "COALESCE(ug.level, '') AS level, " +
                "COALESCE(ug.semester, '') AS semester, " +
                "COALESCE(ug.batch, '') AS batch, " +
                "COALESCE(ug.is_repeat, 0) AS is_repeat, " +

                "CASE WHEN COALESCE(ug.is_repeat, 0) = 1 " +
                "THEN 'Yes' ELSE 'No' END AS repeat_status, " +

                // Extract year from TG/2024/2115
                "CASE " +
                "WHEN ug.reg_no REGEXP '^TG/[0-9]{4}/[0-9]+$' " +
                "THEN CAST(SUBSTRING_INDEX(" +
                "SUBSTRING_INDEX(ug.reg_no, '/', 2), '/', -1) AS UNSIGNED) " +
                "ELSE NULL END AS tg_batch_year, " +

                // Category used by the filter dropdown
                "CASE " +
                "WHEN u.role = 'Undergraduate' " +
                "AND ug.reg_no REGEXP '^TG/[0-9]{4}/[0-9]+$' " +
                "AND ug.batch REGEXP '^[0-9]{4}$' " +
                "AND CAST(SUBSTRING_INDEX(" +
                "SUBSTRING_INDEX(ug.reg_no, '/', 2), '/', -1) AS UNSIGNED) " +
                "< CAST(ug.batch AS UNSIGNED) " +
                "THEN 'Batch Missed Students' " +

                "WHEN u.role = 'Undergraduate' " +
                "AND COALESCE(ug.is_repeat, 0) = 1 " +
                "THEN 'Repeat Students' " +

                "WHEN u.role = 'Undergraduate' " +
                "THEN 'Undergraduate' " +
                "ELSE u.role END AS filter_category, " +

                // Searchable text
                "CONCAT_WS(' ', " +
                "u.user_name, ug.reg_no, u.full_name, u.role, " +
                "u.email, u.phone, u.address, d.department_name, " +
                "ug.batch, ug.level, ug.semester, " +
                "CASE WHEN COALESCE(ug.is_repeat, 0) = 1 " +
                "THEN 'Yes Repeat Students' " +
                "ELSE 'No' END) AS search_text " +

                "FROM users u " +
                "LEFT JOIN undergraduates ug ON ug.user_id = u.user_id " +
                "LEFT JOIN departments d ON d.department_id = " +
                "COALESCE(ug.department_id, u.department_id) " +
                "ORDER BY u.user_id";
    }

    // =====================================================
    // FORM
    // =====================================================

    @Override
    protected Node form() {
        return new VBox(
                10,
                field("Registration / Employee No", registrationNo),
                field("Full Name", fullName),
                field("Role", role),
                departmentField,
                levelField,
                semesterField,
                batchField,
                repeatField,
                field("Email", email),
                field("Phone", phone),
                field("Address", address),
                field("Password", password),
                field("Confirm Password", confirmPassword)
        );
    }

    private void updateRoleFields() {

        boolean undergraduate =
                "Undergraduate".equals(role.getValue());

        boolean admin = "Admin".equals(role.getValue());

        setFieldVisible(departmentField, !admin);
        setFieldVisible(levelField, undergraduate);
        setFieldVisible(semesterField, undergraduate);
        setFieldVisible(batchField, undergraduate);
        setFieldVisible(repeatField, undergraduate);
    }

    private void setFieldVisible(VBox field, boolean visible) {
        field.setVisible(visible);
        field.setManaged(visible);
    }

    // =====================================================
    // TG REGISTRATION NUMBER / BATCH OPTIONS
    // =====================================================

    private String getBatchYearFromRegistrationNo(String regNo) {

        if (regNo == null) {
            return null;
        }

        Matcher matcher = Pattern
                .compile("(?i)^TG/(\\d{4})/\\d+$")
                .matcher(regNo.trim());

        return matcher.matches() ? matcher.group(1) : null;
    }

    private String getUndergraduateStatus(String regNo, String batchValue) {
        String registrationYear = getBatchYearFromRegistrationNo(regNo);
        if (registrationYear == null || batchValue == null
                || !batchValue.matches("\\d{4}")) {
            return "Active";
        }

        return Integer.parseInt(batchValue) > Integer.parseInt(registrationYear)
                ? "Batch Missed"
                : "Active";
    }

    private void updateBatchOptionsForRegistrationNo() {

        String tgYearText =
                getBatchYearFromRegistrationNo(registrationNo.getText());

        if (tgYearText == null) {
            return;
        }

        int tgYear = Integer.parseInt(tgYearText);
        Set<String> batches = new TreeSet<>();

        int lastBatchYear = Math.max(Year.now().getValue() + 3, tgYear);
        for (int year = 2020; year <= lastBatchYear; year++) {
            batches.add(String.valueOf(year));
        }

        // Keep existing database batches, including later batches for delayed students.
        try {
            List<Map<String, Object>> rows = Db.query(
                    "SELECT DISTINCT batch FROM undergraduates " +
                            "WHERE batch IS NOT NULL AND TRIM(batch) <> ''"
            );

            for (Map<String, Object> row : rows) {
                Object value = row.get("batch");

                if (value == null) {
                    continue;
                }

                String existing = value.toString().trim();

                try {
                    Integer.parseInt(existing);
                    batches.add(existing);
                } catch (NumberFormatException ignored) {
                    // Ignore non-year batch values.
                }
            }
        } catch (Exception ex) {
            throw new IllegalStateException(
                    "Cannot load batch options.", ex
            );
        }

        String previousSelection = batch.getValue();

        batch.setItems(
                FXCollections.observableArrayList(batches)
        );

        if (previousSelection != null &&
                batches.contains(previousSelection)) {
            batch.setValue(previousSelection);
        } else {
            batch.setValue(tgYearText);
        }
    }

    // =====================================================
    // FILL FORM
    // =====================================================

    @Override
    protected void fill(Map<String, Object> row) {

        registrationNo.setText(value(row, "reg_no"));
        fullName.setText(value(row, "full_name"));
        email.setText(value(row, "email"));
        phone.setText(value(row, "phone"));
        address.setText(value(row, "address"));

        role.setValue(value(row, "role"));

        level.setValue(emptyToNull(value(row, "level")));
        semester.setValue(emptyToNull(value(row, "semester")));
        batch.setValue(emptyToNull(value(row, "batch")));

        Object repeatValue = row.get("is_repeat");

        boolean isRepeat = repeatValue != null &&
                ("1".equals(String.valueOf(repeatValue)) ||
                        "true".equalsIgnoreCase(String.valueOf(repeatValue)) ||
                        "yes".equalsIgnoreCase(String.valueOf(repeatValue)));

        repeatStudent.setValue(isRepeat ? "Yes" : "No");

        pick(department, row.get("department_id"));

        password.clear();
        confirmPassword.clear();

        updateRoleFields();
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
        level.setValue(null);
        semester.setValue(null);
        batch.setValue(null);
        repeatStudent.setValue("No");
        department.setValue(null);

        updateRoleFields();
    }

    // =====================================================
    // CREATE / UPDATE
    // =====================================================

    @Override
    protected void save() throws Exception {

        if (selected == null) {
            throw new IllegalArgumentException(
                    "Select a user to edit."
            );
        }

        persist(false);
    }

    @Override
    protected void create() throws Exception {

        if (selected != null) {
            throw new IllegalArgumentException(
                    "Click Add User to create a new user."
            );
        }

        persist(true);
    }

    private void persist(boolean creating) throws Exception {

        String regNo = need(
                registrationNo.getText(),
                "Registration / Employee No"
        );

        String name = need(fullName.getText(), "Full Name");
        String mail = need(email.getText(), "Email");

        if (!mail.matches("^[^@\\s]+@[^@\\s]+\\.[^@\\s]+$")) {
            throw new IllegalArgumentException(
                    "Email format is invalid."
            );
        }

        String phoneText = phone.getText();

        if (phoneText != null && !phoneText.isBlank()
                && !phoneText.matches("^[0-9+\\- ]{9,15}$")) {
            throw new IllegalArgumentException(
                    "Phone number is invalid."
            );
        }

        if (role.getValue() == null) {
            throw new IllegalArgumentException("Select a role.");
        }

        String passwordText = password.getText();

        if (creating && passwordText.isBlank()) {
            throw new IllegalArgumentException(
                    "Password is required for a new user."
            );
        }

        if (!passwordText.isBlank()
                && !passwordText.equals(confirmPassword.getText())) {
            throw new IllegalArgumentException(
                    "Passwords do not match."
            );
        }

        boolean undergraduate =
                "Undergraduate".equals(role.getValue());

        if (!"Admin".equals(role.getValue())
                && department.getValue() == null) {
            throw new IllegalArgumentException(
                    "Select a department."
            );
        }

        if (undergraduate) {

            if (level.getValue() == null ||
                    semester.getValue() == null) {
                throw new IllegalArgumentException(
                        "Select level and semester."
                );
            }

            if (batch.getValue() == null ||
                    batch.getValue().isBlank()) {
                throw new IllegalArgumentException(
                        "Select a batch."
                );
            }

            if (repeatStudent.getValue() == null) {
                throw new IllegalArgumentException(
                        "Select repeat student status."
                );
            }

        }

        Object departmentId = department.getValue() == null
                ? null
                : Integer.valueOf(department.getValue().key());

        Object currentId = selected == null
                ? null
                : selected.get("user_id");

        String previousRegNo = selected == null
                ? null
                : value(selected, "reg_no");

        ensureUnique(regNo, mail, currentId);

        String phoneValue = blankToNull(phoneText);
        String addressValue = blankToNull(address.getText());

        String batchValue = undergraduate ? batch.getValue() : null;
        String undergraduateStatus = undergraduate
                ? getUndergraduateStatus(regNo, batchValue)
                : null;

        int repeatValue = undergraduate &&
                "Yes".equals(repeatStudent.getValue()) ? 1 : 0;

        if (creating) {

            long userId = Db.insertAndGetId(
                    "INSERT INTO users " +
                            "(user_name, password, full_name, email, phone, address, role, department_id) " +
                            "VALUES (?, ?, ?, ?, ?, ?, ?, ?)",
                    regNo,
                    passwordText,
                    name,
                    mail,
                    phoneValue,
                    addressValue,
                    role.getValue(),
                    departmentId
            );

            if (undergraduate) {

                Db.update(
                        "INSERT INTO undergraduates " +
                                "(user_id, reg_no, level, semester, department_id, batch, is_repeat, status) " +
                                "VALUES (?, ?, ?, ?, ?, ?, ?, ?)",
                        userId,
                        regNo,
                        level.getValue(),
                        semester.getValue(),
                        departmentId,
                        batchValue,
                        repeatValue,
                        undergraduateStatus
                );

                syncEnrollments(
                        regNo,
                        departmentId,
                        level.getValue(),
                        semester.getValue(),
                        batchValue
                );

            } else {
                syncRoleTable(
                        userId,
                        regNo,
                        role.getValue(),
                        departmentId
                );
            }

            updatePasswordFields();
            return;
        }

        Db.update(
                "UPDATE users SET user_name = ?, full_name = ?, email = ?, " +
                        "phone = ?, address = ?, role = ?, department_id = ? " +
                        "WHERE user_id = ?",
                regNo,
                name,
                mail,
                phoneValue,
                addressValue,
                role.getValue(),
                departmentId,
                currentId
        );

        if (undergraduate) {

            if (previousRegNo != null &&
                    !previousRegNo.equals(regNo)) {
                Db.update(
                        "DELETE FROM enrollments WHERE reg_no = ?",
                        previousRegNo
                );
            }

            int updated = Db.update(
                    "UPDATE undergraduates SET reg_no = ?, level = ?, semester = ?, " +
                            "department_id = ?, batch = ?, is_repeat = ?, status = ? WHERE user_id = ?",
                    regNo,
                    level.getValue(),
                    semester.getValue(),
                    departmentId,
                    batchValue,
                    repeatValue,
                    undergraduateStatus,
                    currentId
            );

            if (updated == 0) {
                Db.update(
                        "INSERT INTO undergraduates " +
                                "(user_id, reg_no, level, semester, department_id, batch, is_repeat, status) " +
                                "VALUES (?, ?, ?, ?, ?, ?, ?, ?)",
                        currentId,
                        regNo,
                        level.getValue(),
                        semester.getValue(),
                        departmentId,
                        batchValue,
                        repeatValue,
                        undergraduateStatus
                );
            }

            syncEnrollments(
                    regNo,
                    departmentId,
                    level.getValue(),
                    semester.getValue(),
                    batchValue
            );

        } else {

            if (previousRegNo != null) {
                Db.update(
                        "DELETE FROM enrollments WHERE reg_no = ?",
                        previousRegNo
                );
            }

            Db.update(
                    "DELETE FROM undergraduates WHERE user_id = ?",
                    currentId
            );
        }

        syncRoleTable(
                currentId,
                regNo,
                role.getValue(),
                departmentId
        );

        updatePasswordIfProvided(currentId);
    }

    // =====================================================
    // ENROLLMENTS
    // =====================================================

    private static void syncEnrollments(
            String regNo,
            Object departmentId,
            String levelValue,
            String semesterValue,
            String batchValue
    ) throws Exception {

        Db.update(
                "DELETE FROM enrollments WHERE reg_no = ?",
                regNo
        );

        Db.update(
                "INSERT INTO enrollments (reg_no, course_code, academic_year) " +
                        "SELECT ?, c.course_code, ? FROM courses c " +
                        "WHERE c.department_id = ? " +
                        "AND CAST(c.level AS UNSIGNED) = CAST(? AS UNSIGNED) " +
                        "AND CAST(c.semester AS UNSIGNED) = CAST(? AS UNSIGNED)",
                regNo,
                batchValue,
                departmentId,
                levelValue,
                semesterValue
        );
    }

    // =====================================================
    // ROLE-SPECIFIC TABLES
    // =====================================================

    private static void syncRoleTable(
            Object userId,
            String employeeNo,
            String userRole,
            Object departmentId
    ) throws Exception {

        if ("Lecturer".equals(userRole)) {

            Db.update(
                    "DELETE FROM technical_officers WHERE user_id = ?",
                    userId
            );

            int updated = Db.update(
                    "UPDATE lecturers SET employee_no = ?, department_id = ? " +
                            "WHERE user_id = ?",
                    employeeNo,
                    departmentId,
                    userId
            );

            if (updated == 0) {
                Db.update(
                        "INSERT INTO lecturers " +
                                "(user_id, employee_no, department_id) VALUES (?, ?, ?)",
                        userId,
                        employeeNo,
                        departmentId
                );
            }

        } else if ("Technical Officer".equals(userRole)) {

            Db.update(
                    "DELETE FROM lecturers WHERE user_id = ?",
                    userId
            );

            int updated = Db.update(
                    "UPDATE technical_officers SET employee_no = ?, department_id = ? " +
                            "WHERE user_id = ?",
                    employeeNo,
                    departmentId,
                    userId
            );

            if (updated == 0) {
                Db.update(
                        "INSERT INTO technical_officers " +
                                "(user_id, employee_no, department_id) VALUES (?, ?, ?)",
                        userId,
                        employeeNo,
                        departmentId
                );
            }

        } else {

            Db.update(
                    "DELETE FROM lecturers WHERE user_id = ?",
                    userId
            );

            Db.update(
                    "DELETE FROM technical_officers WHERE user_id = ?",
                    userId
            );
        }
    }

    // =====================================================
    // PASSWORD
    // =====================================================

    private void updatePasswordIfProvided(Object userId) throws Exception {

        if (!password.getText().isBlank()) {
            Db.update(
                    "UPDATE users SET password = ? WHERE user_id = ?",
                    password.getText(),
                    userId
            );
        }

        updatePasswordFields();
    }

    private void updatePasswordFields() {
        password.clear();
        confirmPassword.clear();
    }

    // =====================================================
    // UNIQUE VALIDATION
    // =====================================================

    private static void ensureUnique(
            String regNo,
            String mail,
            Object currentId
    ) throws Exception {

        String sql =
                "SELECT u.user_id FROM users u " +
                        "LEFT JOIN undergraduates ug ON ug.user_id = u.user_id " +
                        "WHERE (u.user_name = ? OR ug.reg_no = ? OR u.email = ?)";

        List<Map<String, Object>> rows;

        if (currentId == null) {
            rows = Db.query(sql, regNo, regNo, mail);
        } else {
            rows = Db.query(
                    sql + " AND u.user_id <> ?",
                    regNo,
                    regNo,
                    mail,
                    currentId
            );
        }

        if (!rows.isEmpty()) {
            throw new IllegalArgumentException(
                    "Registration / Employee No or email already exists."
            );
        }
    }

    // =====================================================
    // DELETE
    // =====================================================

    @Override
    protected void delete(Map<String, Object> row) throws Exception {

        Object userId = row.get("user_id");
        String regNo = value(row, "reg_no");

        List<Map<String, Object>> lecturers = Db.query(
                "SELECT lecturer_id FROM lecturers WHERE user_id = ?",
                userId
        );

        for (Map<String, Object> lecturer : lecturers) {

            Object lecturerId = lecturer.get("lecturer_id");

            Db.update(
                    "DELETE FROM marks WHERE lecturer_id = ?",
                    lecturerId
            );

            Db.update(
                    "DELETE FROM time_table WHERE lecturer_id = ?",
                    lecturerId
            );

            Db.update(
                    "UPDATE courses SET lecturer_id = NULL WHERE lecturer_id = ?",
                    lecturerId
            );
        }

        Db.update(
                "DELETE FROM enrollments WHERE reg_no = ?",
                regNo
        );

        Db.update(
                "DELETE FROM undergraduates WHERE user_id = ?",
                userId
        );

        Db.update(
                "DELETE FROM lecturers WHERE user_id = ?",
                userId
        );

        Db.update(
                "DELETE FROM technical_officers WHERE user_id = ?",
                userId
        );

        Db.update(
                "DELETE FROM users WHERE user_id = ?",
                userId
        );
    }

    // =====================================================
    // BATCH DROPDOWN
    // =====================================================

    private void loadBatchOptions() {

        Set<String> batches = new TreeSet<>();

        try {
            List<Map<String, Object>> rows = Db.query(
                    "SELECT DISTINCT batch FROM undergraduates " +
                            "WHERE batch IS NOT NULL AND TRIM(batch) <> ''"
            );

            for (Map<String, Object> row : rows) {
                Object batchValue = row.get("batch");

                if (batchValue != null) {
                    batches.add(batchValue.toString().trim());
                }
            }

        } catch (Exception ex) {
            throw new IllegalStateException(
                    "Cannot load existing batches from the database.",
                    ex
            );
        }

        int currentYear = Year.now().getValue();

        for (int year = 2020; year <= currentYear + 3; year++) {
            batches.add(String.valueOf(year));
        }

        batch.setItems(
                FXCollections.observableArrayList(
                        new ArrayList<>(batches)
                )
        );
    }

    // =====================================================
    // DATABASE COLUMN PREPARATION
    // =====================================================

    private static void ensureUndergraduateColumns() {
        addColumn("level");
        addColumn("semester");
        ensureRepeatColumn();
    }

    private static void ensureRepeatColumn() {
        try {
            Db.update(
                    "ALTER TABLE undergraduates " +
                            "ADD COLUMN is_repeat TINYINT(1) NOT NULL DEFAULT 0"
            );
        } catch (Exception ex) {
            if (!isDuplicateColumn(ex)) {
                throw new IllegalStateException(
                        "Cannot prepare the repeat-student column.",
                        ex
                );
            }
        }
    }

    private static void ensureUserDepartmentColumn() {
        try {
            Db.update(
                    "ALTER TABLE users ADD COLUMN department_id INT NULL"
            );
        } catch (Exception ex) {
            if (!isDuplicateColumn(ex)) {
                throw new IllegalStateException(
                        "Cannot prepare the user department column.",
                        ex
                );
            }
        }
    }

    private static void ensureDepartments() {
        try {
            for (String name : DEPARTMENT_NAMES) {
                Db.update(
                        "INSERT INTO departments (department_name) " +
                                "SELECT ? FROM DUAL WHERE NOT EXISTS " +
                                "(SELECT 1 FROM departments WHERE department_name = ?)",
                        name,
                        name
                );
            }
        } catch (Exception ex) {
            throw new IllegalStateException(
                    "Cannot prepare the department list.",
                    ex
            );
        }
    }

    private static void addColumn(String column) {
        try {
            Db.update(
                    "ALTER TABLE undergraduates ADD COLUMN " +
                            column + " VARCHAR(30) NULL"
            );
        } catch (Exception ex) {
            if (!isDuplicateColumn(ex)) {
                throw new IllegalStateException(
                        "Cannot prepare undergraduate column: " + column,
                        ex
                );
            }
        }
    }

    private static boolean isDuplicateColumn(Exception ex) {
        Throwable cause = ex;

        while (cause != null) {
            if (cause instanceof SQLException sqlException
                    && sqlException.getErrorCode() == 1060) {
                return true;
            }

            cause = cause.getCause();
        }

        return false;
    }

    // =====================================================
    // HELPERS
    // =====================================================

    private static String emptyToNull(String text) {
        return text == null || text.isBlank() ? null : text;
    }

    private static String blankToNull(String text) {
        return text == null || text.isBlank()
                ? null
                : text.trim();
    }
}