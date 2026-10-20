import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

public class MarkDAO {

    // Courses this lecturer teaches (from the time table)
    public List<CourseItem> getCourses(int lecturerId) throws SQLException {
        List<CourseItem> list = new ArrayList<>();
        String sql = "SELECT DISTINCT c.course_code, c.course_name "
                + "FROM time_table t JOIN courses c ON t.course_code = c.course_code "
                + "WHERE t.lecturer_id = ? ORDER BY c.course_code";
        try (Connection con = DBConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, lecturerId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    list.add(new CourseItem(rs.getString("course_code"),
                            rs.getString("course_name")));
                }
            }
        }
        return list;
    }

    public List<AssessmentItem> getAssessments(String courseCode) throws SQLException {
        List<AssessmentItem> list = new ArrayList<>();
        String sql = "SELECT assessment_id, assessment_name, assessment_type, weight_percentage "
                + "FROM assessments WHERE course_code = ? ORDER BY assessment_id";
        try (Connection con = DBConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setString(1, courseCode);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    list.add(new AssessmentItem(rs.getInt("assessment_id"),
                            rs.getString("assessment_name"),
                            rs.getString("assessment_type"),
                            rs.getDouble("weight_percentage")));
                }
            }
        }
        return list;
    }

    // Enrolled students of the course, with any marks already saved
    public List<MarkEntry> getStudentMarks(String courseCode, int assessmentId) throws SQLException {
        List<MarkEntry> list = new ArrayList<>();
        String sql = "SELECT DISTINCT g.reg_no, u.full_name, m.marks "
                + "FROM enrollments e "
                + "JOIN undergraduates g ON e.reg_no = g.reg_no "
                + "JOIN users u ON g.user_id = u.user_id "
                + "LEFT JOIN marks m ON m.reg_no = g.reg_no AND m.assessment_id = ? "
                + "WHERE e.course_code = ? ORDER BY g.reg_no";
        try (Connection con = DBConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, assessmentId);
            ps.setString(2, courseCode);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    BigDecimal bd = rs.getBigDecimal("marks");
                    String text = (bd == null) ? "" : bd.stripTrailingZeros().toPlainString();
                    list.add(new MarkEntry(rs.getString("reg_no"),
                            rs.getString("full_name"), text));
                }
            }
        }
        return list;
    }

    // Insert new marks or update existing ones, all in one transaction
    public int saveMarks(List<MarkEntry> entries, int assessmentId, int lecturerId)
            throws SQLException, InvalidMarkException {
        String sql = "INSERT INTO marks (reg_no, assessment_id, lecturer_id, marks) "
                + "VALUES (?, ?, ?, ?) "
                + "ON DUPLICATE KEY UPDATE marks = VALUES(marks), lecturer_id = VALUES(lecturer_id)";
        int count = 0;
        try (Connection con = DBConnection.getConnection()) {
            con.setAutoCommit(false);
            try (PreparedStatement ps = con.prepareStatement(sql)) {
                for (MarkEntry e : entries) {
                    if (!e.hasMark()) continue;
                    ps.setString(1, e.getRegNo());
                    ps.setInt(2, assessmentId);
                    ps.setInt(3, lecturerId);
                    ps.setDouble(4, e.getMarkValue());
                    ps.addBatch();
                    count++;
                }
                ps.executeBatch();
                con.commit();
            } catch (SQLException | InvalidMarkException ex) {
                con.rollback();   // nothing is saved if anything fails
                throw ex;
            }
        }
        return count;
    }
}