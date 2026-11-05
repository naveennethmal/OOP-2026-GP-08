package Technical_Officer;

import Common.Session;
import Database.Db;

import java.sql.Date;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public final class TechnicianDAO {
    private TechnicianDAO() {}

    public static final List<String> LEVELS = List.of("1", "2", "3", "4");
    public static final List<String> SEMESTERS = List.of("1", "2");
    public static final int MAX_SESSIONS = 15;


    // First column of the first row, or null when there are no rows.
    private static Object scalar(String sql, Object... params) throws Exception {
        List<Map<String, Object>> rows = Db.query(sql, params);
        return rows.isEmpty() ? null : rows.get(0).values().iterator().next();
    }

    public static int toInt(Object value) {
        return value == null ? 0 : ((Number) value).intValue();
    }

    public static double toDouble(Object value) {
        return value == null ? 0 : ((Number) value).doubleValue();
    }

    // null -> empty text, so tables never show "null"
    public static String text(Object value) {
        return value == null ? "" : value.toString();
    }

    // Department of the logged-in technical officer.
    public static int deptId() throws Exception {
        Object id = scalar("SELECT department_id FROM users WHERE user_id = ?", Session.getUserId());
        if (id == null) throw new Exception("Technical officer department was not found.");
        return toInt(id);
    }

    // "IT1010 - Introduction" -> "IT1010"
    public static String codeOf(String courseOption) {
        int i = courseOption.indexOf(" - ");
        return i < 0 ? courseOption : courseOption.substring(0, i);
    }

    // ---------- profile ----------

    public static Map<String, Object> profile() throws Exception {
        List<Map<String, Object>> rows = Db.query("""
                SELECT us.full_name, us.email, us.phone, us.address, us.user_name,
                       us.profile_picture, d.department_name
                FROM users us
                JOIN departments d ON d.department_id = us.department_id
                WHERE us.user_id = ?
                """, Session.getUserId());
        if (rows.isEmpty()) throw new Exception("Technical officer profile not found.");
        return rows.get(0);
    }

    // A null picture keeps the old one. Username and password are never changed here. */
    public static void saveProfile(String fullName, String email, String phone, String address, String picture)
            throws Exception {
        Db.update("UPDATE users SET full_name = ?, email = ?, phone = ?, address = ?, "
                        + "profile_picture = COALESCE(?, profile_picture) WHERE user_id = ?",
                fullName, email, phone, address, picture, Session.getUserId());
    }

    public static void removePicture() throws Exception {
        Db.update("UPDATE users SET profile_picture = NULL WHERE user_id = ?", Session.getUserId());
    }

    // ---------- notices ----------

    // Newest first. is_new = 1 when published within the last 7 days.
    public static List<Map<String, Object>> notices() throws Exception {
        List<Map<String, Object>> result = new ArrayList<>();
        for (Map<String, Object> notice : Db.noticesForRole("Technical Officer")) {
            Map<String, Object> row = new LinkedHashMap<>();
            row.put("title", notice.get("title"));
            row.put("description", notice.get("description"));
            row.put("published", notice.get("published_date"));
            row.put("is_new", notice.get("is_new"));
            result.add(row);
        }
        return result;
    }

    // ---------- timetable ----------

    public static List<Map<String, Object>> departments() throws Exception {
        return Db.query("SELECT department_id, department_name FROM departments ORDER BY department_name");
    }

    public static List<Map<String, Object>> timetable(int departmentId, String level, String semester)
            throws Exception {
        return Db.query("""
                SELECT t.day, t.location, t.session_type, c.course_code, c.course_name,
                       TIME_FORMAT(t.start_time, '%H:%i') AS start_time,
                       TIME_FORMAT(t.end_time, '%H:%i') AS end_time
                FROM time_table t
                JOIN courses c ON c.course_code = t.course_code
                WHERE c.department_id = ? AND c.level = ? AND c.semester = ?
                ORDER BY t.start_time
                """, departmentId, level, semester);
    }

    // ---------- attendance ----------

    // Courses of the officer's department, as "CODE - Name".
    public static List<String> courses() throws Exception {
        List<String> list = new ArrayList<>();
        for (Map<String, Object> row : Db.query(
                "SELECT course_code, course_name FROM courses WHERE department_id = ? ORDER BY course_code",
                deptId())) {
            list.add(row.get("course_code") + " - " + row.get("course_name"));
        }
        return list;
    }

    //  Saved date of a course session, or null if it was never recorded
    public static LocalDate sessionDate(String courseCode, int sessionNo) throws Exception {
        Object date = scalar("SELECT DATE_FORMAT(MAX(session_date), '%Y-%m-%d') FROM attendance "
                + "WHERE course_code = ? AND session_no = ?", courseCode, sessionNo);
        return date == null ? null : LocalDate.parse(date.toString());
    }

    /**
     * One row per student of the course / level / semester:
     * attended, total, current_status (this session) and medical_status (None / Pending / Approved).
     */
    public static List<Map<String, Object>> attendance(String courseCode, String level, String semester,
                                                       int sessionNo) throws Exception {
        return Db.query("""
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
                WHERE e.course_code = ? AND u.level = ? AND u.semester = ? AND u.department_id = ?
                ORDER BY u.reg_no
                """, courseCode, courseCode, courseCode, sessionNo, courseCode, courseCode,
                courseCode, level, semester, deptId());
    }

    // Updates the student's record for this session, or inserts it when it does not exist yet
    public static void saveAttendance(String regNo, String courseCode, int sessionNo,
                                      LocalDate date, String status) throws Exception {
        int updated = Db.update("UPDATE attendance SET status = ?, session_date = ? "
                        + "WHERE reg_no = ? AND course_code = ? AND session_no = ?",
                status, Date.valueOf(date), regNo, courseCode, sessionNo);
        if (updated == 0) {
            Db.update("INSERT INTO attendance (reg_no, course_code, department_id, session_type, "
                            + "session_date, session_no, status) "
                            + "VALUES (?, ?, (SELECT department_id FROM courses WHERE course_code = ?), 'Lecture', ?, ?, ?)",
                    regNo, courseCode, courseCode, Date.valueOf(date), sessionNo, status);
        }
    }

    // Deletes every attendance record of a course (own department only). Returns how many were deleted.
    public static int clearAttendance(String courseCode) throws Exception {
        return Db.update("DELETE FROM attendance WHERE course_code = ? AND department_id = ?",
                courseCode, deptId());
    }

    // ---------- medicals ----------

    // "REG NO - Name" of the active undergraduates of the department (for the add form)
    public static List<String> students() throws Exception {
        List<String> list = new ArrayList<>();
        for (Map<String, Object> row : Db.query("""
                SELECT u.reg_no, us.full_name
                FROM undergraduates u JOIN users us ON us.user_id = u.user_id
                WHERE u.department_id = ? AND u.status = 'Active'
                ORDER BY u.reg_no
                """, deptId())) {
            list.add(row.get("reg_no") + " - " + row.get("full_name"));
        }
        return list;
    }

    //  All medicals of the department, including the ones students submitted themselves.
    public static List<Map<String, Object>> medicals() throws Exception {
        return Db.query("""
                SELECT m.medical_id, m.reg_no, us.full_name, m.reason, m.document,
                       DATE_FORMAT(COALESCE(m.medical_start_date, m.medical_date), '%Y-%m-%d') AS start_date,
                       DATE_FORMAT(COALESCE(m.medical_end_date, m.medical_date), '%Y-%m-%d') AS end_date,
                       COALESCE(m.course_code, '-') AS course_code,
                       COALESCE(NULLIF(m.approval_status, ''), 'Pending') AS approval_status
                FROM medical m
                JOIN undergraduates u ON u.reg_no = m.reg_no
                JOIN users us ON us.user_id = u.user_id
                WHERE u.department_id = ?
                ORDER BY COALESCE(m.medical_start_date, m.medical_date) DESC, m.medical_id DESC
                """, deptId());
    }

    public static void addMedical(String regNo, String courseCode, LocalDate start, LocalDate end,
                                  String reason, String status, String document) throws Exception {
        Db.update("""
                INSERT INTO medical (reg_no, tech_id, course_code, medical_date, medical_start_date,
                                     medical_end_date, reason, approval_status, document)
                VALUES (?, (SELECT tech_id FROM technical_officers WHERE user_id = ?), ?, ?, ?, ?, ?, ?, ?)
                """, regNo, Session.getUserId(), courseCode, Date.valueOf(start), Date.valueOf(start),
                Date.valueOf(end), reason, status, document);
    }

    // A null document keeps the old certificate
    public static void updateMedical(int medicalId, String regNo, String courseCode, LocalDate start,
                                     LocalDate end, String reason, String status, String document)
            throws Exception {
        Db.update("""
                UPDATE medical
                SET reg_no = ?, course_code = ?, medical_date = ?, medical_start_date = ?,
                    medical_end_date = ?, reason = ?, approval_status = ?,
                    document = COALESCE(?, document)
                WHERE medical_id = ?
                """, regNo, courseCode, Date.valueOf(start), Date.valueOf(start), Date.valueOf(end),
                reason, status, document, medicalId);
    }

    public static void deleteMedical(int medicalId) throws Exception {
        Db.update("DELETE FROM medical WHERE medical_id = ?", medicalId);
    }

    // ---------- dashboard ----------

    // students, avg_rate (batch average %), below (students under 80%), pending (medicals)
    public static Map<String, Object> dashboardSummary() throws Exception {
        int dept = deptId();
        // one attendance percentage per student, then average / count below 80
        Map<String, Object> summary = new LinkedHashMap<>(Db.query("""
                SELECT COALESCE(AVG(rate), 0) AS avg_rate, COALESCE(SUM(rate < 80), 0) AS below
                FROM (SELECT SUM(a.status IN ('Present', 'Late', 'Medical')) * 100.0 / COUNT(*) AS rate
                      FROM attendance a
                      JOIN undergraduates u ON u.reg_no = a.reg_no
                      WHERE u.department_id = ?
                      GROUP BY a.reg_no) t
                """, dept).get(0));
        summary.put("students", scalar("SELECT COUNT(*) FROM undergraduates WHERE department_id = ?", dept));
        summary.put("pending", scalar("""
                SELECT COUNT(*) FROM medical m
                JOIN undergraduates u ON u.reg_no = m.reg_no
                WHERE m.approval_status = 'Pending' AND u.department_id = ?
                """, dept));
        return summary;
    }

    // course_code, kind ('T' theory / 'P' practical), rate (%)
    public static List<Map<String, Object>> courseRates() throws Exception {
        return Db.query("""
                SELECT a.course_code,
                       IF(a.session_type = 'Practical', 'P', 'T') AS kind,
                       AVG(a.status IN ('Present', 'Late', 'Medical')) * 100 AS rate
                FROM attendance a
                JOIN courses c ON c.course_code = a.course_code
                WHERE c.department_id = ?
                GROUP BY a.course_code, kind
                ORDER BY a.course_code
                """, deptId());
    }
}
