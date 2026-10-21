import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class GradeDAO {

    // One row per student per course: weighted CA earned + the final exam mark
    private static final String BASE =
            "SELECT g.reg_no, u.full_name, c.course_code, c.course_name, c.credits, c.semester, "
                    + "SUM(CASE WHEN a.assessment_type <> 'Final' "
                    + "    THEN COALESCE(m.marks, 0) / 100 * a.weight_percentage ELSE 0 END) AS ca_earned, "
                    + "SUM(CASE WHEN a.assessment_type <> 'Final' THEN a.weight_percentage ELSE 0 END) AS ca_weight, "
                    + "SUM(CASE WHEN a.assessment_type = 'Final' THEN a.weight_percentage ELSE 0 END) AS final_weight, "
                    + "MAX(CASE WHEN a.assessment_type = 'Final' THEN m.marks END) AS final_mark "
                    + "FROM (SELECT DISTINCT reg_no, course_code FROM enrollments) e "
                    + "JOIN undergraduates g ON g.reg_no = e.reg_no "
                    + "JOIN users u ON u.user_id = g.user_id "
                    + "JOIN courses c ON c.course_code = e.course_code "
                    + "JOIN assessments a ON a.course_code = c.course_code "
                    + "LEFT JOIN marks m ON m.assessment_id = a.assessment_id AND m.reg_no = e.reg_no ";

    private static final String GROUP =
            " GROUP BY g.reg_no, u.full_name, c.course_code, c.course_name, c.credits, c.semester "
                    + "ORDER BY g.reg_no, c.semester, c.course_code";

    public List<CourseResult> getByCourse(String courseCode) throws SQLException {
        return query(BASE + "WHERE c.course_code = ?" + GROUP, courseCode);
    }

    public List<CourseResult> getByBatch(String batch) throws SQLException {
        return query(BASE + "WHERE g.batch = ?" + GROUP, batch);
    }

    private List<CourseResult> query(String sql, String param) throws SQLException {
        List<CourseResult> list = new ArrayList<>();
        try (Connection con = DBConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setString(1, param);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    double fm = rs.getDouble("final_mark");
                    Double finalMark = rs.wasNull() ? null : fm;
                    list.add(new CourseResult(
                            rs.getString("reg_no"), rs.getString("full_name"),
                            rs.getString("course_code"), rs.getString("course_name"),
                            rs.getInt("credits"), rs.getInt("semester"),
                            rs.getDouble("ca_earned"), rs.getDouble("ca_weight"),
                            rs.getDouble("final_weight"), finalMark));
                }
            }
        }
        return list;
    }
}