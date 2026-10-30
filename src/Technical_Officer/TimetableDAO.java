package Technical_Officer;

import Database.DBConnection;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public final class TimetableDAO {
    private TimetableDAO() {}

    /** day: 0 = Monday ... 4 = Friday. */
    public record Slot(int day, String start, String end, String code, String name,
                       String room, boolean practical) {}
    public record Department(int id, String name) {
        @Override public String toString() { return name; }
    }

    private static final List<String> DAYS = List.of("Monday", "Tuesday", "Wednesday", "Thursday", "Friday");

    public static List<Department> departments() throws SQLException {
        List<Department> departments = new ArrayList<>();
        try (Connection con = DBConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(
                     "SELECT department_id, department_name FROM departments ORDER BY department_name");
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) departments.add(new Department(
                    rs.getInt("department_id"), rs.getString("department_name")));
        }
        return departments;
    }

    public static int currentDepartmentId() throws SQLException {
        try (Connection con = DBConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(
                     "SELECT department_id FROM users WHERE user_id = ?")) {
            ps.setInt(1, TechnicalOfficerContext.userId());
            try (ResultSet rs = ps.executeQuery()) {
                if (!rs.next()) throw new SQLException("Technical officer department was not found.");
                return rs.getInt("department_id");
            }
        }
    }

    public static List<Slot> list(int departmentId, String level, String semester) throws SQLException {
        String sql = """
                SELECT t.day,
                       TIME_FORMAT(t.start_time, '%H:%i') AS start_time,
                       TIME_FORMAT(t.end_time, '%H:%i') AS end_time,
                       t.location, t.session_type, c.course_code, c.course_name
                FROM time_table t
                JOIN courses c ON c.course_code = t.course_code
                WHERE c.department_id = ?
                  AND c.level = ?
                  AND c.semester = ?
                """
                + " ORDER BY t.start_time, FIELD(t.day, 'Monday', 'Tuesday', 'Wednesday', 'Thursday', 'Friday')";
        List<Slot> slots = new ArrayList<>();
        try (Connection con = DBConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, departmentId);
            ps.setString(2, level);
            ps.setString(3, semester);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    int day = DAYS.indexOf(rs.getString("day"));
                    if (day < 0) continue; // outside the Mon-Fri grid
                    String sessionType = rs.getString("session_type");
                    slots.add(new Slot(day,
                            rs.getString("start_time"),
                            rs.getString("end_time"),
                            rs.getString("course_code"),
                            rs.getString("course_name"),
                            rs.getString("location") == null ? "-" : rs.getString("location"),
                            sessionType != null && sessionType.toUpperCase().contains("PRACTICAL")));
                }
            }
        }
        return slots;
    }

}
