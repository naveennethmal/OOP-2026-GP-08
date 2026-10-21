import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class AttendanceDAO {

    public static final int SESSIONS_PER_COMPONENT = 15;

    // component = "Combined", "Theory" or "Practical" (fixed values, never user text)
    private String filter(String component) {
        return switch (component) {
            case "Theory" -> " AND a.session_type <> 'Practical'";
            case "Practical" -> " AND a.session_type = 'Practical'";
            default -> "";
        };
    }

    public String getCourseType(String courseCode) throws SQLException {
        String sql = "SELECT theory_practical FROM courses WHERE course_code = ?";
        try (Connection con = DBConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setString(1, courseCode);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? rs.getString(1) : "";
            }
        }
    }

    // Batch summary: one row per enrolled student
    public List<AttendanceSummary> getBatchSummary(String courseCode, String component,
                                                   int totalSessions) throws SQLException {
        List<AttendanceSummary> list = new ArrayList<>();
        String sql = "SELECT g.reg_no, u.full_name, "
                + "SUM(CASE WHEN a.status IN ('Present','Late') THEN 1 ELSE 0 END) AS attended, "
                + "SUM(CASE WHEN a.status = 'Absent' THEN 1 ELSE 0 END) AS absent, "
                + "SUM(CASE WHEN a.status = 'Absent' AND EXISTS ("
                + "  SELECT 1 FROM medical m WHERE m.reg_no = a.reg_no "
                + "  AND m.course_code = a.course_code AND m.medical_date = a.session_date "
                + "  AND m.approval_status = 'Approved') THEN 1 ELSE 0 END) AS covered "
                + "FROM (SELECT DISTINCT reg_no FROM enrollments WHERE course_code = ?) e "
                + "JOIN undergraduates g ON g.reg_no = e.reg_no "
                + "JOIN users u ON u.user_id = g.user_id "
                + "LEFT JOIN attendance a ON a.reg_no = g.reg_no AND a.course_code = ?"
                + filter(component)
                + " GROUP BY g.reg_no, u.full_name ORDER BY g.reg_no";

        try (Connection con = DBConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setString(1, courseCode);
            ps.setString(2, courseCode);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    list.add(new AttendanceSummary(
                            rs.getString("reg_no"), rs.getString("full_name"),
                            rs.getInt("attended"), rs.getInt("absent"),
                            rs.getInt("covered"), totalSessions));
                }
            }
        }
        return list;
    }

    // Individual view: every session of one student
    public List<SessionRecord> getSessions(String regNo, String courseCode,
                                           String component) throws SQLException {
        List<SessionRecord> list = new ArrayList<>();
        String sql = "SELECT a.session_date, a.session_no, a.session_type, a.status, "
                + "(SELECT m.approval_status FROM medical m WHERE m.reg_no = a.reg_no "
                + " AND m.course_code = a.course_code AND m.medical_date = a.session_date "
                + " ORDER BY m.medical_id DESC LIMIT 1) AS med "
                + "FROM attendance a WHERE a.reg_no = ? AND a.course_code = ?"
                + filter(component)
                + " ORDER BY a.session_type, a.session_no";

        try (Connection con = DBConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setString(1, regNo);
            ps.setString(2, courseCode);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    String med = rs.getString("med");
                    list.add(new SessionRecord(
                            rs.getDate("session_date").toString(),
                            String.valueOf(rs.getInt("session_no")),
                            rs.getString("session_type"),
                            rs.getString("status"),
                            med == null ? "-" : med));
                }
            }
        }
        return list;
    }

    // Medical records of the students of one course
    public List<MedicalRecord> getMedicals(String courseCode) throws SQLException {
        List<MedicalRecord> list = new ArrayList<>();
        String sql = "SELECT m.reg_no, u.full_name, m.medical_date, m.reason, m.approval_status "
                + "FROM medical m "
                + "JOIN undergraduates g ON g.reg_no = m.reg_no "
                + "JOIN users u ON u.user_id = g.user_id "
                + "WHERE m.course_code = ? ORDER BY m.medical_date DESC, m.reg_no";
        try (Connection con = DBConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setString(1, courseCode);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    list.add(new MedicalRecord(
                            rs.getString("reg_no"), rs.getString("full_name"),
                            rs.getDate("medical_date").toString(),
                            rs.getString("reason"), rs.getString("approval_status")));
                }
            }
        }
        return list;
    }
}