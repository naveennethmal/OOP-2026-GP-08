import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class HomeDAO {

    private int count(String sql, Integer param) throws SQLException {
        try (Connection con = DBConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            if (param != null) ps.setInt(1, param);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? rs.getInt(1) : 0;
            }
        }
    }

    // Courses this lecturer teaches
    public int countCourses(int lecturerId) throws SQLException {
        return count("SELECT COUNT(DISTINCT course_code) FROM time_table WHERE lecturer_id = ?",
                lecturerId);
    }

    // Different students enrolled in any of those courses
    public int countStudents(int lecturerId) throws SQLException {
        return count("SELECT COUNT(DISTINCT reg_no) FROM enrollments WHERE course_code IN "
                + "(SELECT course_code FROM time_table WHERE lecturer_id = ?)", lecturerId);
    }

    // Student x assessment pairs that still have no mark
    public int countPendingMarks(int lecturerId) throws SQLException {
        return count("SELECT COUNT(*) FROM (SELECT DISTINCT reg_no, course_code FROM enrollments) e "
                + "JOIN assessments a ON a.course_code = e.course_code "
                + "LEFT JOIN marks m ON m.assessment_id = a.assessment_id AND m.reg_no = e.reg_no "
                + "WHERE m.mark_id IS NULL AND e.course_code IN "
                + "(SELECT course_code FROM time_table WHERE lecturer_id = ?)", lecturerId);
    }

    public int countNotices() throws SQLException {
        return count("SELECT COUNT(*) FROM notices", null);
    }

    public List<TimetableEntry> getTimetable(int lecturerId) throws SQLException {
        List<TimetableEntry> list = new ArrayList<>();
        String sql = "SELECT t.day, t.start_time, t.end_time, t.course_code, c.course_name, "
                + "t.location, t.session_type "
                + "FROM time_table t JOIN courses c ON c.course_code = t.course_code "
                + "WHERE t.lecturer_id = ? "
                + "ORDER BY FIELD(t.day, 'Monday','Tuesday','Wednesday','Thursday',"
                + "'Friday','Saturday','Sunday'), t.start_time";
        try (Connection con = DBConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, lecturerId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    String start = rs.getTime("start_time").toString().substring(0, 5);
                    String end = rs.getTime("end_time").toString().substring(0, 5);
                    list.add(new TimetableEntry(
                            rs.getString("day"),
                            start + " - " + end,
                            rs.getString("course_code") + " - " + rs.getString("course_name"),
                            rs.getString("location"),
                            rs.getString("session_type")));
                }
            }
        }
        return list;
    }
}