package Technical_Officer;

import Database.DBConnection;

import java.sql.*;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public final class DashboardDAO {
    private DashboardDAO() {}

    public record Summary(int undergraduates, double average, int below, int pendingMedicals) {}

    /** Attendance percentage per course; NaN when there is no record of that kind. */
    public record CourseRate(String course, double theory, double practical) {}

    public static Summary summary() throws SQLException {
        String countSql = "SELECT COUNT(*) FROM undergraduates WHERE department_id = "
                + TechnicalOfficerContext.DEPARTMENT_SQL;

        // one attendance percentage per student, then average / count below 80
        String rateSql = """
                SELECT COALESCE(AVG(rate), 0) AS avg_rate, COALESCE(SUM(rate < 80), 0) AS below
                FROM (SELECT SUM(a.status IN ('Present', 'Late', 'Medical')) * 100.0 / COUNT(*) AS rate
                      FROM attendance a
                      JOIN undergraduates u ON u.reg_no = a.reg_no
                      WHERE u.department_id = %s
                      GROUP BY a.reg_no) t
                """.formatted(TechnicalOfficerContext.DEPARTMENT_SQL);

        String pendingSql = """
                SELECT COUNT(*) FROM medical m
                JOIN undergraduates u ON u.reg_no = m.reg_no
                WHERE m.approval_status = 'Pending' AND u.department_id = %s
                """.formatted(TechnicalOfficerContext.DEPARTMENT_SQL);

        try (Connection con = DBConnection.getConnection()) {
            int count = 0, below = 0, pending = 0;
            double avg = 0;

            try (PreparedStatement ps = con.prepareStatement(countSql)) {
                ps.setInt(1, TechnicalOfficerContext.userId());
                try (ResultSet rs = ps.executeQuery()) {
                    if (rs.next()) count = rs.getInt(1);
                }
            }
            try (PreparedStatement ps = con.prepareStatement(rateSql)) {
                ps.setInt(1, TechnicalOfficerContext.userId());
                try (ResultSet rs = ps.executeQuery()) {
                    if (rs.next()) {
                        avg = rs.getDouble("avg_rate");
                        below = rs.getInt("below");
                    }
                }
            }
            try (PreparedStatement ps = con.prepareStatement(pendingSql)) {
                ps.setInt(1, TechnicalOfficerContext.userId());
                try (ResultSet rs = ps.executeQuery()) {
                    if (rs.next()) pending = rs.getInt(1);
                }
            }
            return new Summary(count, avg, below, pending);
        }
    }

    public static List<CourseRate> courseRates() throws SQLException {
        String sql = """
                SELECT a.course_code,
                       IF(a.session_type = 'Practical', 'P', 'T') AS kind,
                       AVG(a.status IN ('Present', 'Late', 'Medical')) * 100 AS rate
                FROM attendance a
                JOIN courses c ON c.course_code = a.course_code
                WHERE c.department_id = %s
                GROUP BY a.course_code, kind
                ORDER BY a.course_code
                """.formatted(TechnicalOfficerContext.DEPARTMENT_SQL);

        Map<String, double[]> map = new LinkedHashMap<>();
        try (Connection con = DBConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, TechnicalOfficerContext.userId());
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    double[] v = map.computeIfAbsent(rs.getString("course_code"),
                            k -> new double[]{Double.NaN, Double.NaN});
                    v["P".equals(rs.getString("kind")) ? 1 : 0] = rs.getDouble("rate");
                }
            }
        }
        List<CourseRate> list = new ArrayList<>();
        map.forEach((course, v) -> list.add(new CourseRate(course, v[0], v[1])));
        return list;
    }
}
