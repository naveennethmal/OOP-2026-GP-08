package Technical_Officer;

import Database.DBConnection;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;

import java.sql.*;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

public final class MedicalDAO {
    private MedicalDAO() {}

    public record MedicalRecord(int id, String regNo, String name, String startDate, String endDate,
                                String course, String reason, String status, String document) {}

    private static void ensureDateRangeColumns(Connection con) throws SQLException {
        try (Statement statement = con.createStatement()) {
            statement.executeUpdate("ALTER TABLE medical ADD COLUMN IF NOT EXISTS medical_start_date DATE NULL");
            statement.executeUpdate("ALTER TABLE medical ADD COLUMN IF NOT EXISTS medical_end_date DATE NULL");
        }
    }

    /**
     * All medicals submitted by students in the technical officer's department.
     * Student submissions have a null tech_id and must not be filtered out.
     */
    public static ObservableList<MedicalRecord> list() throws SQLException {
        String sql = """
                SELECT m.medical_id, m.reg_no, us.full_name,
                       COALESCE(m.medical_start_date, m.medical_date) AS medical_start_date,
                       COALESCE(m.medical_end_date, m.medical_date) AS medical_end_date,
                       m.course_code, m.reason,
                       COALESCE(NULLIF(m.approval_status, ''), 'Pending') AS approval_status,
                       m.document
                FROM medical m
                JOIN undergraduates u ON u.reg_no = m.reg_no
                JOIN users us ON us.user_id = u.user_id
                WHERE u.department_id = %s
                ORDER BY COALESCE(m.medical_start_date, m.medical_date) DESC,
                         m.medical_id DESC
                """.formatted(TechnicalOfficerContext.DEPARTMENT_SQL);

        ObservableList<MedicalRecord> list = FXCollections.observableArrayList();
        try (Connection con = DBConnection.getConnection()) {
            ensureDateRangeColumns(con);
            try (PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, TechnicalOfficerContext.userId());
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    String course = rs.getString("course_code");
                    list.add(new MedicalRecord(
                            rs.getInt("medical_id"),
                            rs.getString("reg_no"),
                            rs.getString("full_name"),
                            rs.getString("medical_start_date"),
                            rs.getString("medical_end_date"),
                            course == null ? "-" : course,
                            rs.getString("reason"),
                            rs.getString("approval_status"),
                            rs.getString("document")));
                }
            }
        }
        }
        return list;
    }

    /** Undergraduates of the department as "REG NO - Name" (for the add form). */
    public static List<String> students() throws SQLException {
        String sql = """
                SELECT u.reg_no, us.full_name
                FROM undergraduates u
                JOIN users us ON us.user_id = u.user_id
                WHERE u.department_id = %s AND u.status = 'Active'
                ORDER BY u.reg_no
                """.formatted(TechnicalOfficerContext.DEPARTMENT_SQL);
        List<String> list = new ArrayList<>();
        try (Connection con = DBConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, TechnicalOfficerContext.userId());
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) list.add(rs.getString("reg_no") + " - " + rs.getString("full_name"));
            }
        }
        return list;
    }

    public static void add(String regNo, String courseCode, LocalDate startDate, LocalDate endDate,
                           String reason, String status, String document)
            throws SQLException {
        try (Connection con = DBConnection.getConnection();
             PreparedStatement check = con.prepareStatement("""
                     SELECT t.tech_id
                     FROM technical_officers t
                     JOIN undergraduates u ON u.department_id = (
                         SELECT department_id FROM users WHERE user_id = ?
                     )
                     JOIN courses c ON c.department_id = u.department_id
                                           AND c.course_code = ?
                     WHERE t.user_id = ? AND u.reg_no = ?
                     """)) {
            check.setInt(1, TechnicalOfficerContext.userId());
            check.setString(2, courseCode);
            check.setInt(3, TechnicalOfficerContext.userId());
            check.setString(4, regNo);
            try (ResultSet rs = check.executeQuery()) {
                if (!rs.next()) {
                    throw new SQLException("The undergraduate or course is outside your department.");
                }
                int techId = rs.getInt("tech_id");
                ensureDateRangeColumns(con);
                try (PreparedStatement ps = con.prepareStatement("""
                        INSERT INTO medical
                            (reg_no, tech_id, course_code, medical_date,
                             medical_start_date, medical_end_date, reason, approval_status, document)
                        VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)
                        """)) {
                    ps.setString(1, regNo);
                    ps.setInt(2, techId);
                    ps.setString(3, courseCode);
                    ps.setDate(4, Date.valueOf(startDate));
                    ps.setDate(5, Date.valueOf(startDate));
                    ps.setDate(6, Date.valueOf(endDate));
                    ps.setString(7, reason);
                    ps.setString(8, status);
                    ps.setString(9, document);
                    ps.executeUpdate();
                }
            }
        }
    }

    /** Updates a selected submission while restricting it to the officer's department. */
    public static void update(int medicalId, String regNo, String courseCode,
                              LocalDate startDate, LocalDate endDate,
                              String reason, String status, String document) throws SQLException {
        String sql = """
                UPDATE medical
                SET reg_no = ?, course_code = ?, medical_date = ?,
                    medical_start_date = ?, medical_end_date = ?, reason = ?, approval_status = ?,
                    document = COALESCE(?, document)
                WHERE medical_id = ?
                  AND EXISTS (
                      SELECT 1 FROM undergraduates u
                      WHERE u.reg_no = medical.reg_no
                        AND u.department_id = (
                            SELECT department_id FROM users WHERE user_id = ?
                        )
                  )
                  AND EXISTS (
                      SELECT 1 FROM undergraduates u
                      WHERE u.reg_no = ?
                        AND u.department_id = (
                            SELECT department_id FROM users WHERE user_id = ?
                        )
                  )
                  AND EXISTS (
                      SELECT 1 FROM courses c
                      WHERE c.course_code = ?
                        AND c.department_id = (
                            SELECT department_id FROM users WHERE user_id = ?
                        )
                  )
                """;
        try (Connection con = DBConnection.getConnection()) {
            ensureDateRangeColumns(con);
            try (PreparedStatement ps = con.prepareStatement(sql)) {
                int userId = TechnicalOfficerContext.userId();
                ps.setString(1, regNo);
                ps.setString(2, courseCode);
                ps.setDate(3, Date.valueOf(startDate));
                ps.setDate(4, Date.valueOf(startDate));
                ps.setDate(5, Date.valueOf(endDate));
                ps.setString(6, reason);
                ps.setString(7, status);
                ps.setString(8, document);
                ps.setInt(9, medicalId);
                ps.setInt(10, userId);
                ps.setString(11, regNo);
                ps.setInt(12, userId);
                ps.setString(13, courseCode);
                ps.setInt(14, userId);
                if (ps.executeUpdate() == 0) {
                    throw new SQLException("The selected medical record, undergraduate, or course is outside your department.");
                }
            }
        }
    }

    public static void updateStatus(int medicalId, String status) throws SQLException {
        String sql = """
                UPDATE medical
                SET approval_status = ?,
                    tech_id = (SELECT tech_id FROM technical_officers WHERE user_id = ?)
                WHERE medical_id = ?
                  AND EXISTS (
                      SELECT 1
                      FROM undergraduates u
                      WHERE u.reg_no = medical.reg_no
                        AND u.department_id = (
                            SELECT department_id FROM users WHERE user_id = ?
                        )
                  )
                """;
        try (Connection con = DBConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setString(1, status);
            ps.setInt(2, TechnicalOfficerContext.userId());
            ps.setInt(3, medicalId);
            ps.setInt(4, TechnicalOfficerContext.userId());
            ps.executeUpdate();
        }
    }

    public static void delete(int medicalId) throws SQLException {
        try (Connection con = DBConnection.getConnection();
             PreparedStatement ps = con.prepareStatement("""
                     DELETE FROM medical
                     WHERE medical_id = ?
                       AND reg_no IN (
                           SELECT u.reg_no
                           FROM undergraduates u
                           WHERE u.department_id = (
                               SELECT department_id FROM users WHERE user_id = ?
                           )
                       )
                     """)) {
            ps.setInt(1, medicalId);
            ps.setInt(2, TechnicalOfficerContext.userId());
            ps.executeUpdate();
        }
    }
}
