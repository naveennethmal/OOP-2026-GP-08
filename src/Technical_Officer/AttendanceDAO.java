package Technical_Officer;

import Database.DBConnection;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;

import java.sql.*;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

public final class AttendanceDAO {
    private AttendanceDAO() {}

    public static final int MAX_SESSIONS = 15;

    public static final class AttendanceRow {
        private final String regNo;
        private final String name;
        private final int attended;
        private final int total;
        private final String medical;                 // None / Pending / Approved
        private final StringProperty session;

        public AttendanceRow(String regNo, String name, int attended, int total, String medical, String session) {
            this.regNo = regNo;
            this.name = name;
            this.attended = attended;
            this.total = total;
            this.medical = medical;
            this.session = new SimpleStringProperty(session);
        }

        public String regNo() { return regNo; }
        public String name() { return name; }
        public int attended() { return attended; }
        public int total() { return total; }
        public String medical() { return medical; }
        public StringProperty sessionProperty() { return session; }

        public double rate() { return total == 0 ? 0 : attended * 100.0 / total; }

        public String eligibility() {
            if (total == 0) return "No sessions";
            if (rate() >= 80) return "Eligible";
            return switch (medical) {
                case "Approved" -> "Eligible (medical)";
                case "Pending" -> "Medical pending";
                default -> "Not eligible";
            };
        }
    }

    /** "IT1010 - Introduction to ..." -> "IT1010" */
    public static String codeOf(String courseOption) {
        int i = courseOption.indexOf(" - ");
        return i < 0 ? courseOption : courseOption.substring(0, i);
    }

    /** Courses of the technical officer's department, as "CODE - Name". */
    public static List<String> courses() throws SQLException {
        String sql = "SELECT course_code, course_name FROM courses WHERE department_id = "
                + TechnicalOfficerContext.DEPARTMENT_SQL + " ORDER BY course_code";
        List<String> list = new ArrayList<>();
        try (Connection con = DBConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, TechnicalOfficerContext.userId());
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) list.add(rs.getString("course_code") + " - " + rs.getString("course_name"));
            }
        }
        return list;
    }

    /** Levels available to the technical officer's department. */
    public static List<String> levels() throws SQLException {
        return List.of("1", "2", "3", "4");
    }

    /** All semesters supported by the academic system. */
    public static List<String> semesters() throws SQLException {
        return List.of("1", "2");
    }

    /** Returns the saved date for a course session, or null before it has been recorded. */
    public static LocalDate sessionDate(String courseCode, int sessionNo) throws SQLException {
        String sql = "SELECT MAX(session_date) FROM attendance WHERE course_code = ? AND session_no = ?";
        try (Connection con = DBConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setString(1, courseCode);
            ps.setInt(2, sessionNo);
            try (ResultSet rs = ps.executeQuery()) {
                if (!rs.next()) return null;
                Date value = rs.getDate(1);
                return value == null ? null : value.toLocalDate();
            }
        }
    }

    /** Students enrolled in the course and matching the selected level and semester. */
    public static ObservableList<AttendanceRow> load(String courseCode, String level, String semester,
                                                     int sessionNo)
            throws SQLException {
        String sql = """
                SELECT DISTINCT u.reg_no, us.full_name,
                  (SELECT COUNT(*) FROM attendance a
                    WHERE a.reg_no = u.reg_no AND a.course_code = ?
                      AND a.status IN ('Present', 'Late', 'Medical')) AS attended,
                  (SELECT COUNT(DISTINCT a.session_no) FROM attendance a
                    WHERE a.reg_no = u.reg_no AND a.course_code = ?) AS total,
                  (SELECT a.status FROM attendance a
                    WHERE a.reg_no = u.reg_no AND a.course_code = ? AND a.session_no = ?
                    LIMIT 1) AS current_status,
                  CASE
                    WHEN EXISTS (SELECT 1 FROM medical m WHERE m.reg_no = u.reg_no
                                 AND m.course_code = ? AND m.approval_status = 'Approved') THEN 'Approved'
                    WHEN EXISTS (SELECT 1 FROM medical m WHERE m.reg_no = u.reg_no
                                 AND m.course_code = ? AND m.approval_status = 'Pending') THEN 'Pending'
                    ELSE 'None'
                  END AS medical_status
                FROM enrollments e
                JOIN undergraduates u ON u.reg_no = e.reg_no
                JOIN users us ON us.user_id = u.user_id
                JOIN courses c ON c.course_code = e.course_code
                WHERE u.department_id = c.department_id
                  AND c.department_id = (
                      SELECT department_id FROM users WHERE user_id = ?
                  )
                  AND e.course_code = ?
                  AND u.level = ?
                  AND u.semester = ?
                ORDER BY u.reg_no
                """;

        ObservableList<AttendanceRow> rows = FXCollections.observableArrayList();
        try (Connection con = DBConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setString(1, courseCode);
            ps.setString(2, courseCode);
            ps.setString(3, courseCode);
            ps.setInt(4, sessionNo);
            ps.setString(5, courseCode);
            ps.setString(6, courseCode);
            ps.setInt(7, TechnicalOfficerContext.userId());
            ps.setString(8, courseCode);
            ps.setString(9, level);
            ps.setString(10, semester);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    String current = rs.getString("current_status");
                    String shown = current == null ? "Absent" : current;
                    rows.add(new AttendanceRow(
                            rs.getString("reg_no"),
                            rs.getString("full_name"),
                            rs.getInt("attended"),
                            rs.getInt("total"),
                            rs.getString("medical_status"),
                            shown));
                }
            }
        }
        return rows;
    }

    /**
     * Saves the "This session" column using the date selected by the technical officer.
     */
    public static void save(String courseCode, int sessionNo, LocalDate date,
                            List<AttendanceRow> rows) throws SQLException {
        ensureMedicalStatusSupported();
        String update = "UPDATE attendance SET status = ?, session_date = ? "
                + "WHERE reg_no = ? AND course_code = ? AND session_no = ?";
        String insert = "INSERT INTO attendance "
                + "(reg_no, course_code, department_id, session_type, session_date, session_no, status) "
                + "VALUES (?, ?, (SELECT department_id FROM courses WHERE course_code = ?), ?, ?, ?, ?)";
        String sessionType = "Lecture";

        try (Connection con = DBConnection.getConnection()) {
            validateScope(con, courseCode, rows);
            con.setAutoCommit(false);
            try (PreparedStatement up = con.prepareStatement(update);
                 PreparedStatement in = con.prepareStatement(insert)) {
                for (AttendanceRow r : rows) {
                    String status = r.sessionProperty().get();
                    if (!List.of("Present", "Absent", "Medical").contains(status)) status = "Absent";

                    up.setString(1, status);
                    up.setDate(2, Date.valueOf(date));
                    up.setString(3, r.regNo());
                    up.setString(4, courseCode);
                    up.setInt(5, sessionNo);
                    if (up.executeUpdate() == 0) {
                        in.setString(1, r.regNo());
                        in.setString(2, courseCode);
                        in.setString(3, courseCode);
                        in.setString(4, sessionType);
                        in.setDate(5, Date.valueOf(date));
                        in.setInt(6, sessionNo);
                        in.setString(7, status);
                        in.executeUpdate();
                    }
                }
                con.commit();
            } catch (SQLException e) {
                con.rollback();
                throw e;
            }
        }
    }

    /** Expands legacy ENUM status columns so the Medical attendance value can be stored. */
    private static void ensureMedicalStatusSupported() throws SQLException {
        String sql = "SELECT DATA_TYPE FROM information_schema.COLUMNS "
                + "WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'attendance' AND COLUMN_NAME = 'status'";
        try (Connection con = DBConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            if (rs.next() && "enum".equalsIgnoreCase(rs.getString("DATA_TYPE"))) {
                try (Statement alter = con.createStatement()) {
                    alter.executeUpdate("ALTER TABLE attendance MODIFY COLUMN status VARCHAR(20) NOT NULL");
                }
            }
        }
    }

    /** Deletes every attendance record for a course within the logged-in officer's department. */
    public static int clear(String courseCode) throws SQLException {
        String sql = """
                DELETE FROM attendance
                WHERE course_code = ?
                  AND department_id = (
                      SELECT department_id FROM users WHERE user_id = ?
                  )
                  AND EXISTS (
                      SELECT 1 FROM courses c
                      WHERE c.course_code = attendance.course_code
                        AND c.department_id = (
                            SELECT department_id FROM users WHERE user_id = ?
                        )
                  )
                """;
        try (Connection con = DBConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setString(1, courseCode);
            ps.setInt(2, TechnicalOfficerContext.userId());
            ps.setInt(3, TechnicalOfficerContext.userId());
            return ps.executeUpdate();
        }
    }

    private static void validateScope(Connection con, String courseCode,
                                      List<AttendanceRow> rows) throws SQLException {
        String sql = """
                SELECT EXISTS (
                    SELECT 1
                    FROM enrollments e
                    JOIN undergraduates u ON u.reg_no = e.reg_no
                    JOIN courses c ON c.course_code = ?
                    WHERE u.department_id = c.department_id
                      AND e.course_code = c.course_code
                      AND u.reg_no = ?
                      AND c.department_id = (
                          SELECT department_id FROM users WHERE user_id = ?
                      )
                )
                """;
        try (PreparedStatement ps = con.prepareStatement(sql)) {
            for (AttendanceRow row : rows) {
                ps.setString(1, courseCode);
                ps.setString(2, row.regNo());
                ps.setInt(3, TechnicalOfficerContext.userId());
                try (ResultSet rs = ps.executeQuery()) {
                    if (!rs.next() || !rs.getBoolean(1)) {
                        throw new SQLException("An undergraduate or course is outside your department.");
                    }
                }
            }
        }
    }
}
