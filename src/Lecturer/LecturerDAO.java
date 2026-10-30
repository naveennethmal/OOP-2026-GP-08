package Lecturer;

import Common.Session;
import Common.AcademicResults;
import Database.DBConnection;
import Database.Db;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Every database query of the lecturer part is here.
 * The logged in lecturer is found from Common.Session (set by the shared Login).
 */
public final class LecturerDAO {
    private LecturerDAO() {}

    // ======================= small data classes =======================

    public record Course(String code, String name, String type, String level, String semester) {
        @Override public String toString() { return code + " - " + name; }
    }

    public record Assessment(int id, String name, String type, double weight) {
        @Override public String toString() { return name + " (" + type + ", " + weight + "%)"; }
    }

    public record Material(int id, String courseCode, String title, String type, String date, String filePath) {}

    public record Student(String regNo, String name, String batch, String level, String semester,
                          String status, String email, String phone, String address) {}

    public record Notice(int id, String title, String date, String body) {}

    public record Profile(String username, String employeeNo, String fullName, String email,
                          String phone, String address, String picture, String department) {
        public String initials() {
            if (fullName == null || fullName.isBlank()) return "?";
            String[] p = fullName.replace(".", " ").trim().split("\\s+");
            String a = p[0].substring(0, 1);
            String b = p.length > 1 ? p[p.length - 1].substring(0, 1) : "";
            return (a + b).toUpperCase();
        }
    }

    public record Stats(int courses, int students, int pendingMarks, int notices) {}

    /** One row of the "Upload marks" table. */
    public static final class MarkEntry {
        private final String regNo;
        private final String name;
        private final String level;
        private final String semester;
        private final String batch;
        private final StringProperty marks = new SimpleStringProperty("");

        public MarkEntry(String regNo, String name, String level, String semester, String batch, String marks) {
            this.regNo = regNo;
            this.name = name;
            this.level = level;
            this.semester = semester;
            this.batch = batch;
            this.marks.set(marks == null ? "" : marks);
        }

        public String regNo() { return regNo; }
        public String name() { return name; }
        public String level() { return level; }
        public String semester() { return semester; }
        public String batch() { return batch; }
        public StringProperty marksProperty() { return marks; }
        public boolean hasMark() { return !marks.get().isBlank(); }

        /** The mark as a number between 0 and 100. */
        public double value() {
            double v;
            try {
                v = Double.parseDouble(marks.get().trim());
            } catch (NumberFormatException e) {
                throw new IllegalArgumentException(regNo + ": '" + marks.get() + "' is not a number");
            }
            if (v < 0 || v > 100) throw new IllegalArgumentException(regNo + ": marks must be between 0 and 100");
            return v;
        }
    }

    // ======================= who is logged in =======================

    private static int cachedUser = -1;
    private static int cachedLecturer;

    /** lecturers.lecturer_id of the logged in user. */
    public static int lecturerId() throws Exception {
        int user = Session.getUserId();
        if (user != cachedUser) {
            List<Map<String, Object>> rows = Db.query("SELECT lecturer_id FROM lecturers WHERE user_id = ?", user);
            if (rows.isEmpty()) throw new IllegalStateException("This account has no lecturer record.");
            cachedLecturer = i(rows.get(0), "lecturer_id");
            cachedUser = user;
        }
        return cachedLecturer;
    }

    private static final String MY_COURSES = "(SELECT course_code FROM courses WHERE lecturer_id = %d)";

    private static String myCourseCodes() throws Exception {
        return MY_COURSES.formatted(lecturerId());
    }

    // ======================= profile =======================

    public static Profile profile() throws Exception {
        List<Map<String, Object>> rows = Db.query("""
                SELECT u.user_name, l.employee_no, u.full_name, u.email, u.phone, u.address,
                       u.profile_picture, d.department_name
                FROM users u
                JOIN lecturers l ON l.user_id = u.user_id
                LEFT JOIN departments d ON d.department_id = l.department_id
                WHERE u.user_id = ?
                """, Session.getUserId());
        if (rows.isEmpty()) throw new IllegalStateException("Lecturer profile not found.");
        Map<String, Object> r = rows.get(0);
        return new Profile(s(r, "user_name"), s(r, "employee_no"), s(r, "full_name"), s(r, "email"),
                s(r, "phone"), s(r, "address"), s(r, "profile_picture"), s(r, "department_name"));
    }

    /** Username and password are never touched. A null picture keeps the old one. */
    public static void saveProfile(String name, String email, String phone, String address, String picture)
            throws Exception {
        Db.update("UPDATE users SET full_name = ?, email = ?, phone = ?, address = ?, "
                        + "profile_picture = COALESCE(?, profile_picture) WHERE user_id = ?",
                name, email, phone, address, picture, Session.getUserId());
    }

    public static void removePicture() throws Exception {
        Db.update("UPDATE users SET profile_picture = NULL WHERE user_id = ?", Session.getUserId());
    }

    // ======================= courses, home =======================

    public static List<Course> courses() throws Exception {
        List<Course> list = new ArrayList<>();
        for (Map<String, Object> r : Db.query(
                "SELECT course_code, course_name, theory_practical, level, semester FROM courses WHERE lecturer_id = ? "
                        + "ORDER BY course_code", lecturerId())) {
            list.add(new Course(s(r, "course_code"), s(r, "course_name"), s(r, "theory_practical"),
                    s(r, "level"), s(r, "semester")));
        }
        return list;
    }

    public static Stats stats() throws Exception {
        String mine = myCourseCodes();
        return new Stats(
                count("SELECT COUNT(*) FROM courses WHERE lecturer_id = " + lecturerId()),
                count("SELECT COUNT(DISTINCT reg_no) FROM enrollments WHERE course_code IN " + mine),
                count("SELECT COUNT(*) FROM (SELECT DISTINCT reg_no, course_code FROM enrollments) e "
                        + "JOIN assessments a ON a.course_code = e.course_code "
                        + "LEFT JOIN marks m ON m.assessment_id = a.assessment_id AND m.reg_no = e.reg_no "
                        + "WHERE m.mark_id IS NULL AND e.course_code IN " + mine),
                count("SELECT COUNT(*) FROM notices"));
    }

    // ======================= notices =======================

    public static List<Notice> notices() throws Exception {
        List<Notice> list = new ArrayList<>();
        for (Map<String, Object> r : Db.query(
                "SELECT notice_id, title, published_date, description FROM notices "
                        + "ORDER BY published_date DESC, notice_id DESC")) {
            list.add(new Notice(i(r, "notice_id"), s(r, "title"), s(r, "published_date"), s(r, "description")));
        }
        return list;
    }

    // ======================= course materials =======================

    public static List<Material> materials(String courseCode) throws Exception {
        List<Material> list = new ArrayList<>();
        for (Map<String, Object> r : Db.query(
                "SELECT material_id, course_code, title, type, uploaded_date, file_path FROM course_materials "
                        + "WHERE course_code = ? ORDER BY uploaded_date DESC, material_id DESC", courseCode)) {
            list.add(new Material(i(r, "material_id"), s(r, "course_code"), s(r, "title"), s(r, "type"),
                    s(r, "uploaded_date"), s(r, "file_path")));
        }
        return list;
    }

    public static void addMaterial(String courseCode, String title, String type, String filePath) throws Exception {
        requireOwnCourse(courseCode);
        Db.update("INSERT INTO course_materials (course_code, title, type, uploaded_date, file_path) "
                + "VALUES (?, ?, ?, CURDATE(), ?)", courseCode, title, type, filePath);
    }

    public static void updateMaterial(int id, String title, String type, String filePath) throws Exception {
        Db.update("UPDATE course_materials SET title = ?, type = ?, file_path = ? WHERE material_id = ? "
                + "AND course_code IN " + myCourseCodes(), title, type, filePath, id);
    }

    public static void deleteMaterial(int id) throws Exception {
        Db.update("DELETE FROM course_materials WHERE material_id = ? AND course_code IN " + myCourseCodes(), id);
    }

    // ======================= assessments and marks =======================

    public static List<Assessment> assessments(String courseCode) throws Exception {
        List<Assessment> list = new ArrayList<>();
        for (Map<String, Object> r : Db.query(
                "SELECT assessment_id, assessment_name, assessment_type, weight_percentage FROM assessments "
                        + "WHERE course_code = ? ORDER BY assessment_id", courseCode)) {
            list.add(new Assessment(i(r, "assessment_id"), s(r, "assessment_name"),
                    s(r, "assessment_type"), d(r, "weight_percentage")));
        }
        return list;
    }

    /** Creates the standard mark columns if this course does not already have them. */
    public static void ensureStandardAssessments(String courseCode) throws Exception {
        requireOwnCourse(courseCode);
        List<Assessment> existing = assessments(courseCode);
        String[][] standard = {
                {"QUIZ 1", "Quiz"}, {"QUIZ 2", "Quiz"}, {"QUIZ 3", "Quiz"},
                {"ASSIGNMENT 1", "Assignment"}, {"ASSIGNMENT 2", "Assignment"},
                {"ASSIGNMENT 3", "Assignment"}, {"MID EXAM", "Midterm"},
                {"END EXAM", "Final"}, {"PROJECT", "Practical"}
        };
        for (String[] item : standard) {
            boolean found = existing.stream().anyMatch(a -> a.name().equalsIgnoreCase(item[0]));
            if (!found) {
                Db.update("INSERT INTO assessments (course_code, assessment_type, assessment_name, weight_percentage) "
                        + "VALUES (?, ?, ?, 1)", courseCode, item[1], item[0]);
            }
        }
    }

    /** Creates an exam / assessment for a course (the weights of one course may not pass 100%). */
    public static void addAssessment(String courseCode, String name, String type, double weight) throws Exception {
        requireOwnCourse(courseCode);
        if (name == null || name.isBlank()) throw new IllegalArgumentException("Exam name cannot be empty.");
        if (weight <= 0 || weight > 100) throw new IllegalArgumentException("Weight must be between 0 and 100.");
        if (Db.query("SELECT 1 FROM assessments WHERE course_code = ? AND LOWER(assessment_name) = LOWER(?)",
                courseCode, name.trim()).size() > 0) {
            throw new IllegalArgumentException("An exam with that name already exists for this course.");
        }
        double used = d(Db.query("SELECT COALESCE(SUM(weight_percentage), 0) AS w FROM assessments "
                + "WHERE course_code = ?", courseCode).get(0), "w");
        if (used + weight > 100) {
            throw new IllegalArgumentException("Total weight would be " + (used + weight)
                    + "%. This course only has " + (100 - used) + "% left.");
        }
        Db.update("INSERT INTO assessments (course_code, assessment_type, assessment_name, weight_percentage) "
                + "VALUES (?, ?, ?, ?)", courseCode, type, name, weight);
    }

    /** Removes a lecturer-created exam and its marks; standard exam rows are protected. */
    public static void deleteAssessment(int assessmentId) throws Exception {
        List<Map<String, Object>> rows = Db.query("SELECT a.assessment_name FROM assessments a "
                + "JOIN courses c ON c.course_code = a.course_code "
                + "WHERE a.assessment_id = ? AND c.lecturer_id = ?", assessmentId, lecturerId());
        if (rows.isEmpty()) throw new IllegalStateException("You can only remove exams from your own courses.");
        if (isStandardAssessment(s(rows.get(0), "assessment_name"))) {
            throw new IllegalArgumentException("Standard exams cannot be removed.");
        }
        try (Connection con = DBConnection.getConnection()) {
            con.setAutoCommit(false);
            try {
                try (PreparedStatement ps = con.prepareStatement("DELETE FROM marks WHERE assessment_id = ?")) {
                    ps.setInt(1, assessmentId);
                    ps.executeUpdate();
                }
                try (PreparedStatement ps = con.prepareStatement("DELETE FROM assessments WHERE assessment_id = ?")) {
                    ps.setInt(1, assessmentId);
                    ps.executeUpdate();
                }
                con.commit();
            } catch (Exception ex) {
                con.rollback();
                throw ex;
            }
        }
    }

    public static boolean isStandardAssessment(String name) {
        return name != null && (name.matches("(?i)QUIZ [123]") || name.matches("(?i)ASSIGNMENT [123]")
                || name.equalsIgnoreCase("MID EXAM") || name.equalsIgnoreCase("END EXAM")
                || name.equalsIgnoreCase("PROJECT"));
    }

    /** Current enrollees and students who took the course in an earlier semester. */
    public static List<MarkEntry> courseStudents(String courseCode) throws Exception {
        List<MarkEntry> list = new ArrayList<>();
        for (Map<String, Object> r : Db.query("""
                SELECT DISTINCT g.reg_no, u.full_name, g.level, g.semester, g.batch
                FROM undergraduates g
                JOIN users u ON u.user_id = g.user_id
                JOIN courses c ON c.course_code = ? AND c.department_id = g.department_id
                LEFT JOIN enrollments e ON e.reg_no = g.reg_no AND e.course_code = c.course_code
                WHERE e.course_code IS NOT NULL
                   OR ((CAST(c.level AS UNSIGNED) - 1) * 2 + CAST(c.semester AS UNSIGNED))
                      < ((CAST(g.level AS UNSIGNED) - 1) * 2 + CAST(g.semester AS UNSIGNED))
                ORDER BY g.reg_no
                """, courseCode)) {
            list.add(new MarkEntry(s(r, "reg_no"), s(r, "full_name"), s(r, "level"), s(r, "semester"),
                    s(r, "batch"), ""));
        }
        return list;
    }

    /** Current enrollees and students who took the course in an earlier semester. */
    public static List<MarkEntry> markEntries(String courseCode, int assessmentId, String batch) throws Exception {
        List<MarkEntry> list = new ArrayList<>();
        for (Map<String, Object> r : Db.query("""
                SELECT DISTINCT g.reg_no, u.full_name, g.level, g.semester, g.batch, m.marks
                FROM undergraduates g
                JOIN users u ON u.user_id = g.user_id
                JOIN courses c ON c.course_code = ?
                    AND c.department_id = g.department_id
                LEFT JOIN enrollments e ON e.reg_no = g.reg_no AND e.course_code = c.course_code
                LEFT JOIN marks m ON m.reg_no = g.reg_no AND m.assessment_id = ?
                WHERE (? = 'All batches' OR g.batch = ?)
                  AND (
                      e.course_code IS NOT NULL
                      OR ((CAST(c.level AS UNSIGNED) - 1) * 2 + CAST(c.semester AS UNSIGNED))
                         < ((CAST(g.level AS UNSIGNED) - 1) * 2 + CAST(g.semester AS UNSIGNED))
                      OR m.marks IS NOT NULL
                  )
                ORDER BY g.reg_no
                """, courseCode, assessmentId, batch, batch)) {
            Object m = r.get("marks");
            String text = m == null ? "" : new java.math.BigDecimal(m.toString()).stripTrailingZeros().toPlainString();
            list.add(new MarkEntry(s(r, "reg_no"), s(r, "full_name"), s(r, "level"), s(r, "semester"),
                    s(r, "batch"), text));
        }
        return list;
    }

    /** Inserts or updates every filled mark in one transaction. Blank rows are skipped. */
    public static int saveMarks(List<MarkEntry> entries, int assessmentId) throws Exception {
        for (MarkEntry e : entries) if (e.hasMark()) e.value();            // check everything first

        int lecturer = lecturerId();
        if (Db.query("SELECT 1 FROM assessments a JOIN courses c ON c.course_code = a.course_code "
                + "WHERE a.assessment_id = ? AND c.lecturer_id = ?", assessmentId, lecturer).isEmpty()) {
            throw new IllegalStateException("You can only upload marks for your own courses.");
        }

        String sql = "INSERT INTO marks (reg_no, assessment_id, lecturer_id, marks) VALUES (?, ?, ?, ?) "
                + "ON DUPLICATE KEY UPDATE marks = VALUES(marks), lecturer_id = VALUES(lecturer_id)";
        int count = 0;
        try (Connection con = DBConnection.getConnection()) {
            con.setAutoCommit(false);
            try (PreparedStatement ps = con.prepareStatement(sql)) {
                for (MarkEntry e : entries) {
                    if (!e.hasMark()) continue;
                    ps.setString(1, e.regNo());
                    ps.setInt(2, assessmentId);
                    ps.setInt(3, lecturer);
                    ps.setDouble(4, e.value());
                    ps.addBatch();
                    count++;
                }
                ps.executeBatch();
                con.commit();
            } catch (Exception ex) {
                con.rollback();
                throw ex;
            }
        }
        return count;
    }

    /** Removes one saved mark after verifying that the exam belongs to this lecturer's course. */
    public static void removeMark(String regNo, int assessmentId) throws Exception {
        int lecturer = lecturerId();
        if (Db.query("SELECT 1 FROM assessments a JOIN courses c ON c.course_code = a.course_code "
                        + "JOIN undergraduates g ON g.reg_no = ? AND g.department_id = c.department_id "
                        + "LEFT JOIN enrollments e ON e.course_code = c.course_code AND e.reg_no = g.reg_no "
                        + "WHERE a.assessment_id = ? AND c.lecturer_id = ? AND (e.reg_no IS NOT NULL OR "
                        + "((CAST(c.level AS UNSIGNED) - 1) * 2 + CAST(c.semester AS UNSIGNED)) "
                        + "< ((CAST(g.level AS UNSIGNED) - 1) * 2 + CAST(g.semester AS UNSIGNED)))",
                regNo, assessmentId, lecturer).isEmpty()) {
            throw new IllegalStateException("You can only remove marks for students in your own courses.");
        }
        Db.update("DELETE FROM marks WHERE assessment_id = ? AND reg_no = ?", assessmentId, regNo);
    }

    public static boolean isGradeEligibleStudent(String courseCode, String regNo) throws Exception {
        AcademicResults.ensureRetakeTable();
        List<Map<String, Object>> rows = Db.query("""
                SELECT (e.reg_no IS NOT NULL) AS enrolled,
                       ((CAST(c.level AS UNSIGNED) - 1) * 2 + CAST(c.semester AS UNSIGNED))
                        < ((CAST(g.level AS UNSIGNED) - 1) * 2 + CAST(g.semester AS UNSIGNED))
                        AS historical_course
                FROM courses c
                JOIN undergraduates g ON g.reg_no = ?
                    AND g.department_id = c.department_id
                LEFT JOIN enrollments e
                    ON e.course_code = c.course_code AND e.reg_no = g.reg_no
                WHERE c.course_code = ? AND c.lecturer_id = ?
                """, regNo, courseCode, lecturerId());
        if (rows.isEmpty()) return false;
        Map<String, Object> row = rows.get(0);
        if (i(row, "enrolled") == 0 && i(row, "historical_course") == 0) return false;
        if (i(row, "historical_course") != 0) {
            Db.update("""
                    INSERT INTO student_retake_courses (reg_no, course_code)
                    VALUES (?, ?)
                    ON DUPLICATE KEY UPDATE course_code = VALUES(course_code)
                    """, regNo, courseCode);
        }
        return true;
    }

    public static String retakeGrade(String courseCode, String regNo) throws Exception {
        requireGradeEligibleStudent(courseCode, regNo);
        AcademicResults.ensureRetakeGradeTable();
        List<Map<String, Object>> rows = Db.query(
                "SELECT grade FROM student_retake_grades WHERE reg_no = ? AND course_code = ?",
                regNo, courseCode);
        return rows.isEmpty() ? null : s(rows.get(0), "grade");
    }

    public static void saveRetakeGrade(String courseCode, String regNo, String grade) throws Exception {
        if (!"C".equals(grade) && !"EC".equals(grade) && !"EE".equals(grade) && !"E".equals(grade)) {
            throw new IllegalArgumentException("Choose C, EC, EE or E.");
        }
        requireGradeEligibleStudent(courseCode, regNo);
        AcademicResults.ensureRetakeGradeTable();
        Db.update("""
                INSERT INTO student_retake_grades (reg_no, course_code, grade)
                VALUES (?, ?, ?)
                ON DUPLICATE KEY UPDATE grade = VALUES(grade)
                """, regNo, courseCode, grade);
    }

    public static void clearRetakeGrade(String courseCode, String regNo) throws Exception {
        requireGradeEligibleStudent(courseCode, regNo);
        AcademicResults.ensureRetakeGradeTable();
        Db.update("DELETE FROM student_retake_grades WHERE reg_no = ? AND course_code = ?",
                regNo, courseCode);
    }

    private static void requireGradeEligibleStudent(String courseCode, String regNo) throws Exception {
        if (!isGradeEligibleStudent(courseCode, regNo)) {
            throw new IllegalStateException("You can only set a grade for an eligible student in your course.");
        }
    }

    // ======================= undergraduates =======================

    public static List<Student> students() throws Exception {
        List<Student> list = new ArrayList<>();
        for (Map<String, Object> r : Db.query("""
                SELECT g.reg_no, u.full_name, g.batch, g.level, g.semester, g.status, u.email, u.phone, u.address
                FROM undergraduates g
                JOIN users u ON u.user_id = g.user_id
                ORDER BY g.batch, g.reg_no
                """)) {
            list.add(new Student(s(r, "reg_no"), s(r, "full_name"), s(r, "batch"), s(r, "level"),
                    s(r, "semester"), s(r, "status"), s(r, "email"), s(r, "phone"), s(r, "address")));
        }
        return list;
    }

    public static List<String> batches() throws Exception {
        List<String> list = new ArrayList<>();
        for (Map<String, Object> r : Db.query("SELECT DISTINCT TRIM(batch) AS batch FROM undergraduates "
                + "WHERE batch IS NOT NULL AND TRIM(batch) <> '' ORDER BY batch")) {
            list.add(s(r, "batch"));
        }
        return list;
    }

    // ======================= attendance and medical =======================

    /** component: Combined, Theory or Practical (fixed values from the screen, never typed text). */
    private static String sessionFilter(String component) {
        return switch (component) {
            case "Theory" -> " AND a.session_type <> 'Practical'";
            case "Practical" -> " AND a.session_type = 'Practical'";
            default -> "";
        };
    }

    // an absence is "covered" when an approved medical of that course includes the session date
    private static final String COVERED = "EXISTS (SELECT 1 FROM medical m WHERE m.reg_no = a.reg_no "
            + "AND m.course_code = a.course_code AND m.approval_status = 'Approved' "
            + "AND a.session_date BETWEEN COALESCE(m.medical_start_date, m.medical_date) "
            + "AND COALESCE(m.medical_end_date, m.medical_date))";

    /** One row per enrolled student. */
    public static List<Academic.Attendance> attendance(String courseCode, String component, int totalSessions)
            throws Exception {
        return attendance(courseCode, component, totalSessions, false);
    }

    /** Attendance for everyone included in the course's CA/marks roster, including prior batches. */
    public static List<Academic.Attendance> eligibilityAttendance(
            String courseCode, String component, int totalSessions) throws Exception {
        return attendance(courseCode, component, totalSessions, true);
    }

    private static List<Academic.Attendance> attendance(
            String courseCode, String component, int totalSessions, boolean includePreviousStudents) throws Exception {
        List<Academic.Attendance> list = new ArrayList<>();
        String studentRoster = includePreviousStudents
                ? "SELECT DISTINCT g.reg_no FROM undergraduates g "
                    + "JOIN courses c ON c.course_code = ? AND c.department_id = g.department_id "
                    + "LEFT JOIN enrollments ce ON ce.reg_no = g.reg_no AND ce.course_code = c.course_code "
                    + "WHERE ce.course_code IS NOT NULL "
                    + "OR ((CAST(c.level AS UNSIGNED) - 1) * 2 + CAST(c.semester AS UNSIGNED)) "
                    + "< ((CAST(g.level AS UNSIGNED) - 1) * 2 + CAST(g.semester AS UNSIGNED))"
                : "SELECT DISTINCT reg_no FROM enrollments WHERE course_code = ?";
        String sql = "SELECT g.reg_no, u.full_name, g.level, g.semester, "
                + "COALESCE(SUM(CASE WHEN a.status IN ('Present','Late','Medical') THEN 1 ELSE 0 END), 0) AS attended, "
                + "COALESCE(SUM(CASE WHEN a.status = 'Absent' THEN 1 ELSE 0 END), 0) AS absent, "
                + "COALESCE(SUM(CASE WHEN a.status = 'Absent' AND " + COVERED + " THEN 1 ELSE 0 END), 0) AS covered "
                + "FROM (" + studentRoster + ") e "
                + "JOIN undergraduates g ON g.reg_no = e.reg_no "
                + "JOIN users u ON u.user_id = g.user_id "
                + "LEFT JOIN attendance a ON a.reg_no = g.reg_no AND a.course_code = ?" + sessionFilter(component)
                + " GROUP BY g.reg_no, u.full_name, g.level, g.semester ORDER BY g.reg_no";
        for (Map<String, Object> r : Db.query(sql, courseCode, courseCode)) {
            list.add(new Academic.Attendance(s(r, "reg_no"), s(r, "full_name"), s(r, "level"), s(r, "semester"),
                    i(r, "attended"), i(r, "absent"), i(r, "covered"), totalSessions));
        }
        return list;
    }

    public record SessionRow(String date, String no, String type, String status, String medical) {}

    /** Every session of one student (used by the detail table). */
    public static List<SessionRow> sessions(String regNo, String courseCode, String component) throws Exception {
        List<SessionRow> list = new ArrayList<>();
        String sql = "SELECT a.session_date, a.session_no, a.session_type, a.status, "
                + "(SELECT m.approval_status FROM medical m WHERE m.reg_no = a.reg_no AND m.course_code = a.course_code "
                + " AND a.session_date BETWEEN COALESCE(m.medical_start_date, m.medical_date) "
                + " AND COALESCE(m.medical_end_date, m.medical_date) ORDER BY m.medical_id DESC LIMIT 1) AS med "
                + "FROM attendance a WHERE a.reg_no = ? AND a.course_code = ?" + sessionFilter(component)
                + " ORDER BY a.session_type, a.session_no";
        for (Map<String, Object> r : Db.query(sql, regNo, courseCode)) {
            String med = s(r, "med");
            list.add(new SessionRow(s(r, "session_date"), s(r, "session_no"), s(r, "session_type"),
                    s(r, "status"), med.isEmpty() ? "-" : med));
        }
        return list;
    }

    public record Medical(String regNo, String name, String date, String reason, String status, String document) {}

    public static List<Medical> medicals(String courseCode) throws Exception {
        List<Medical> list = new ArrayList<>();
        for (Map<String, Object> r : Db.query("""
                SELECT m.reg_no, u.full_name, m.medical_date, m.reason, m.approval_status, m.document
                FROM medical m
                JOIN undergraduates g ON g.reg_no = m.reg_no
                JOIN users u ON u.user_id = g.user_id
                WHERE m.course_code = ?
                ORDER BY m.medical_date DESC, m.reg_no
                """, courseCode)) {
            list.add(new Medical(s(r, "reg_no"), s(r, "full_name"), s(r, "medical_date"),
                    s(r, "reason"), s(r, "approval_status"), s(r, "document")));
        }
        return list;
    }

    // ======================= CA, grades, GPA =======================

    /** Mid+project courses use 25%+15%; other courses use best quizzes, mid/project core and assignments. */
    public static List<Academic.Ca> ca(String courseCode) throws Exception {
        ensureStandardAssessments(courseCode);
        String courseType = s(Db.query("SELECT theory_practical FROM courses WHERE course_code = ?", courseCode)
                .get(0), "theory_practical");
        boolean practicalCourse = courseType.toUpperCase(java.util.Locale.ROOT).contains("PRACTICAL");
        Map<String, Assessment> selected = new java.util.LinkedHashMap<>();
        for (Assessment a : assessments(courseCode)) {
            String name = a.name().toUpperCase(java.util.Locale.ROOT);
            if (name.matches("QUIZ [123]") || a.type().equalsIgnoreCase("Assignment") || name.matches("ASSIGNMENT.*")
                    || name.equals("MID EXAM") || name.equals("PROJECT")) {
                selected.putIfAbsent(name, a);
            }
        }
        List<Assessment> assignments = selected.values().stream()
                .filter(a -> a.type().equalsIgnoreCase("Assignment")
                        || a.name().toUpperCase(java.util.Locale.ROOT).startsWith("ASSIGNMENT"))
                .toList();
        Map<Integer, Map<String, Double>> marksByAssessment = new java.util.HashMap<>();
        for (Assessment a : selected.values()) {
            Map<String, Double> byStudent = new java.util.HashMap<>();
            for (Map<String, Object> r : Db.query("SELECT reg_no, marks FROM marks WHERE assessment_id = ?", a.id())) {
                if (r.get("marks") != null) byStudent.put(s(r, "reg_no"), d(r, "marks"));
            }
            marksByAssessment.put(a.id(), byStudent);
        }
        List<Academic.Ca> result = new ArrayList<>();
        for (MarkEntry student : courseStudents(courseCode)) {
            Academic.Ca row = new Academic.Ca(student.regNo(), student.name(), 40, practicalCourse);
            Assessment mid = selected.get("MID EXAM");
            Assessment project = selected.get("PROJECT");
            boolean paperCourse = practicalCourse;
            List<Integer> bestQuizIds = new ArrayList<>();
            List<Map.Entry<Integer, Double>> rankedQuizzes = new ArrayList<>();
            for (int quiz = 1; quiz <= 3; quiz++) {
                Assessment a = selected.get("QUIZ " + quiz);
                Double mark = a == null ? null : marksByAssessment.get(a.id()).get(student.regNo());
                if (a != null && mark != null) rankedQuizzes.add(Map.entry(a.id(), mark));
            }
            rankedQuizzes.sort((left, right) -> Double.compare(right.getValue(), left.getValue()));
            rankedQuizzes.stream().limit(2).forEach(entry -> bestQuizIds.add(entry.getKey()));
            for (int quiz = 1; quiz <= 3; quiz++) {
                Assessment a = selected.get("QUIZ " + quiz);
                Double mark = a == null ? null : marksByAssessment.get(a.id()).get(student.regNo());
                row.add(new Academic.CaPart("QUIZ " + quiz, "Quiz",
                        !paperCourse && a != null && bestQuizIds.contains(a.id()) ? 5 : 0, mark));
            }
            Double midMark = mid == null ? null : marksByAssessment.get(mid.id()).get(student.regNo());
            Double projectMark = project == null ? null : marksByAssessment.get(project.id()).get(student.regNo());
            row.add(new Academic.CaPart("MID EXAM", "Midterm", 25, midMark));
            row.add(new Academic.CaPart("PROJECT", "Project",
                    paperCourse ? 15 : midMark == null ? 25 : 0, projectMark));
            long markedAssignments = assignments.stream()
                    .filter(a -> marksByAssessment.get(a.id()).containsKey(student.regNo())).count();
            double assignmentWeight = paperCourse || markedAssignments == 0 ? 0 : 5.0 / markedAssignments;
            for (Assessment a : assignments) {
                Double assignmentMark = marksByAssessment.get(a.id()).get(student.regNo());
                row.add(new Academic.CaPart(a.name(), "Assignment",
                        assignmentMark == null ? 0 : assignmentWeight, assignmentMark));
            }
            result.add(row);
        }
        return result;
    }

    /** End exam marks keyed by student, used by the eligibility screen. Missing rows remain pending. */
    public static Map<String, Double> endExamMarks(String courseCode) throws Exception {
        Map<String, Double> marks = new java.util.HashMap<>();
        for (Map<String, Object> r : Db.query("""
                SELECT m.reg_no, m.marks
                FROM marks m
                JOIN assessments a ON a.assessment_id = m.assessment_id
                WHERE a.course_code = ? AND UPPER(a.assessment_name) = 'END EXAM'
                """, courseCode)) {
            if (r.get("marks") != null) marks.put(s(r, "reg_no"), d(r, "marks"));
        }
        return marks;
    }

    // One row per student per course with the source marks needed for the fixed 40/60 rule.
    private static final String RESULT_SQL = """
            SELECT g.reg_no, u.full_name, c.course_code, c.course_name, c.credits, c.level, c.semester,
                   MAX(CASE WHEN UPPER(a.assessment_name) = 'QUIZ 1' THEN m.marks END) AS quiz1,
                   MAX(CASE WHEN UPPER(a.assessment_name) = 'QUIZ 2' THEN m.marks END) AS quiz2,
                   MAX(CASE WHEN UPPER(a.assessment_name) = 'QUIZ 3' THEN m.marks END) AS quiz3,
                   MAX(CASE WHEN UPPER(a.assessment_name) = 'MID EXAM' THEN m.marks END) AS mid_mark,
                   MAX(CASE WHEN UPPER(a.assessment_name) = 'PROJECT' THEN m.marks END) AS project_mark,
                   MAX(CASE WHEN UPPER(c.theory_practical) LIKE '%PRACTICAL%' THEN 1 ELSE 0 END) AS practical_course,
                   MAX(assignment_scores.assignment_average) AS assignment_average,
                   MAX(CASE WHEN UPPER(a.assessment_name) = 'END EXAM' THEN m.marks END) AS final_mark,
                   MAX(CASE WHEN rt.reg_no IS NOT NULL THEN 1 ELSE 0 END) AS retake,
                   MAX(rg.grade) AS grade_override
            FROM undergraduates g
            JOIN users u ON u.user_id = g.user_id
            JOIN courses c ON c.department_id = g.department_id
                AND ((CAST(c.level AS UNSIGNED) - 1) * 2 + CAST(c.semester AS UNSIGNED))
                    <= ((CAST(g.level AS UNSIGNED) - 1) * 2 + CAST(g.semester AS UNSIGNED))
            LEFT JOIN student_retake_courses rt
                ON rt.reg_no = g.reg_no AND rt.course_code = c.course_code
            LEFT JOIN student_retake_grades rg
                ON rg.reg_no = g.reg_no AND rg.course_code = c.course_code
            LEFT JOIN assessments a ON a.course_code = c.course_code
                AND UPPER(a.assessment_name) IN ('QUIZ 1', 'QUIZ 2', 'QUIZ 3', 'MID EXAM', 'PROJECT', 'END EXAM')
            LEFT JOIN marks m ON m.assessment_id = a.assessment_id AND m.reg_no = g.reg_no
            LEFT JOIN (
                SELECT ass.course_code, mark.reg_no, AVG(mark.marks) AS assignment_average
                FROM assessments ass
                JOIN marks mark ON mark.assessment_id = ass.assessment_id
                WHERE UPPER(ass.assessment_type) = 'ASSIGNMENT'
                    OR UPPER(ass.assessment_name) LIKE 'ASSIGNMENT%'
                GROUP BY ass.course_code, mark.reg_no
            ) assignment_scores ON assignment_scores.course_code = c.course_code
                AND assignment_scores.reg_no = g.reg_no
            """;
    private static final String RESULT_GROUP = " GROUP BY g.reg_no, u.full_name, c.course_code, c.course_name, "
            + "c.credits, c.level, c.semester ORDER BY g.reg_no, c.level, c.semester, c.course_code";

    public static List<Academic.CourseResult> resultsOfCourse(String courseCode) throws Exception {
        return results(RESULT_SQL + "WHERE c.course_code = ?" + RESULT_GROUP, courseCode);
    }

    public static List<Academic.CourseResult> resultsOfBatch(String batch) throws Exception {
        return results(RESULT_SQL + "WHERE g.batch = ?" + RESULT_GROUP, batch);
    }

    public static List<Academic.CourseResult> resultsOfAllBatches() throws Exception {
        return results(RESULT_SQL + RESULT_GROUP);
    }

    private static List<Academic.CourseResult> results(String sql, Object... params) throws Exception {
        AcademicResults.ensureRetakeTable();
        AcademicResults.ensureRetakeGradeTable();
        List<Academic.CourseResult> list = new ArrayList<>();
        for (Map<String, Object> r : Db.query(sql, params)) {
            Double quiz1 = nullableDouble(r.get("quiz1"));
            Double quiz2 = nullableDouble(r.get("quiz2"));
            Double quiz3 = nullableDouble(r.get("quiz3"));
            Double mid = nullableDouble(r.get("mid_mark"));
            Double project = nullableDouble(r.get("project_mark"));
            Double assignmentAverage = nullableDouble(r.get("assignment_average"));
            list.add(new Academic.CourseResult(s(r, "reg_no"), s(r, "full_name"), s(r, "course_code"),
                    s(r, "course_name"), s(r, "credits"), i(r, "level"), i(r, "semester"),
                    Academic.caPoints(quiz1, quiz2, quiz3, mid, project, assignmentAverage,
                            i(r, "practical_course") != 0), 40, 60,
                    nullableDouble(r.get("final_mark")), i(r, "retake") != 0,
                    nullableString(r.get("grade_override"))));
        }
        return list;
    }

    private static Double nullableDouble(Object value) {
        return value instanceof Number n ? n.doubleValue() : null;
    }

    private static String nullableString(Object value) {
        return value == null ? null : value.toString();
    }

    // ======================= helpers =======================

    private static void requireOwnCourse(String courseCode) throws Exception {
        if (Db.query("SELECT 1 FROM courses WHERE course_code = ? AND lecturer_id = ?",
                courseCode, lecturerId()).isEmpty()) {
            throw new IllegalStateException("You can only change your own courses.");
        }
    }

    private static int count(String sql) throws Exception {
        return i(Db.query(sql).get(0).values().iterator().next());
    }

    private static String s(Map<String, Object> row, String key) {
        Object o = row.get(key);
        return o == null ? "" : o.toString();
    }

    private static int i(Map<String, Object> row, String key) { return i(row.get(key)); }

    private static int i(Object o) { return o instanceof Number n ? n.intValue() : 0; }

    private static double d(Map<String, Object> row, String key) {
        return row.get(key) instanceof Number n ? n.doubleValue() : 0;
    }
}
