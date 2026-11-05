package Common;

import Database.Db;
import Lecturer.Academic;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public final class AcademicResults {
    public record CourseGrade(String courseCode, String course, int level, int semester, String grade,
                       double gradePoint, int credits, boolean marksAvailable,
                       boolean finalMarkUploaded, boolean gradeEntered) {
    }

    private AcademicResults() {
    }

    public static List<CourseGrade> beforeTerm(String regNo, int currentTerm) throws Exception {
        String sql = """
                SELECT c.course_code, c.course_name, c.level, c.semester, c.credits,
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
                JOIN courses c ON c.department_id = g.department_id
                LEFT JOIN student_retake_courses rt
                    ON rt.reg_no = g.reg_no AND rt.course_code = c.course_code
                LEFT JOIN student_retake_grades rg
                    ON rg.reg_no = g.reg_no AND rg.course_code = c.course_code
                LEFT JOIN assessments a ON a.course_code = c.course_code
                    AND UPPER(a.assessment_name) IN ('QUIZ 1', 'QUIZ 2', 'QUIZ 3',
                                                     'MID EXAM', 'PROJECT', 'END EXAM')
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
                WHERE g.reg_no = ?
                  AND ((CAST(c.level AS UNSIGNED) - 1) * 2
                       + CAST(c.semester AS UNSIGNED)) < ?
                GROUP BY c.course_code, c.course_name, c.level, c.semester, c.credits
                ORDER BY c.level, c.semester, c.course_code
                """;

        List<CourseGrade> results = new ArrayList<>();
        ensureRetakeTable();
        ensureRetakeGradeTable();
        for (Map<String, Object> row : Db.query(sql, regNo, currentTerm)) {
            Double finalMark = nullableDouble(row.get("final_mark"));
            String gradeOverride = nullableString(row.get("grade_override"));
            Academic.CourseResult result = new Academic.CourseResult(
                    regNo,
                    "",
                    value(row, "course_code"),
                    value(row, "course_name"),
                    value(row, "credits"),
                    Integer.parseInt(value(row, "level")),
                    Integer.parseInt(value(row, "semester")),
                    Academic.caPoints(
                            nullableDouble(row.get("quiz1")),
                            nullableDouble(row.get("quiz2")),
                            nullableDouble(row.get("quiz3")),
                            nullableDouble(row.get("mid_mark")),
                            nullableDouble(row.get("project_mark")),
                            nullableDouble(row.get("assignment_average")),
                            "1".equals(value(row, "practical_course"))),
                    40,
                    60,
                    finalMark,
                    "1".equals(value(row, "retake")),
                    gradeOverride);
            boolean hasMarks = hasAssessmentMarks(row);
            String grade = gradeOverride != null || finalMark != null
                    ? result.grade()
                    : provisionalGrade(result, hasMarks);
            double gradePoint = gradeOverride != null || finalMark != null
                    ? result.gradePoint()
                    : hasMarks ? Academic.pointFor(result.caEarned() / 40.0 * 100.0) : 0.0;
            results.add(new CourseGrade(
                    value(row, "course_code"),
                    value(row, "course_name"),
                    Integer.parseInt(value(row, "level")),
                    Integer.parseInt(value(row, "semester")),
                    grade,
                    gradePoint,
                    result.credits(),
                    hasMarks,
                    finalMark != null,
                    gradeOverride != null || finalMark != null));
        }
        return results;
    }

    public static List<CourseGrade> beforeCurrentTerm(String regNo) throws Exception {
        List<Map<String, Object>> rows = Db.query(
                "SELECT level, semester FROM undergraduates WHERE reg_no = ?", regNo);
        if (rows.isEmpty()) {
            throw new IllegalStateException("Unable to find the student's current semester.");
        }
        int level = Integer.parseInt(value(rows.get(0), "level"));
        int semester = Integer.parseInt(value(rows.get(0), "semester"));
        return beforeTerm(regNo, term(level, semester));
    }

    public static List<String> failedCourseCodes(String regNo) throws Exception {
        return beforeCurrentTerm(regNo).stream()
                .filter(result -> result.grade().startsWith("E"))
                .map(CourseGrade::courseCode)
                .distinct()
                .toList();
    }

    public static void ensureRetakeTable() throws Exception {
        Db.update("""
                CREATE TABLE IF NOT EXISTS student_retake_courses (
                    reg_no VARCHAR(100) NOT NULL,
                    course_code VARCHAR(100) NOT NULL,
                    PRIMARY KEY (reg_no, course_code)
                )
                """);
    }

    public static void ensureRetakeGradeTable() throws Exception {
        Db.update("""
                CREATE TABLE IF NOT EXISTS student_retake_grades (
                    reg_no VARCHAR(100) NOT NULL,
                    course_code VARCHAR(100) NOT NULL,
                    grade VARCHAR(2) NOT NULL,
                    PRIMARY KEY (reg_no, course_code)
                )
                """);
    }

    public static double gpa(List<CourseGrade> results) {
        int credits = results.stream()
                .filter(result -> result.gradeEntered() && result.credits() > 0)
                .mapToInt(CourseGrade::credits)
                .sum();
        if (credits == 0) {
            return 0.0;
        }
        double points = results.stream()
                .filter(result -> result.gradeEntered() && result.credits() > 0)
                .mapToDouble(result -> result.gradePoint() * result.credits())
                .sum();
        return points / credits;
    }

    public static boolean hasGpaResults(List<CourseGrade> results) {
        return results.stream().anyMatch(result -> result.gradeEntered() && result.credits() > 0);
    }

    private static String provisionalGrade(Academic.CourseResult result, boolean hasMarks) {
        if (!hasMarks) {
            return "Awaiting marks";
        }
        return result.grade().equals("EC")
                ? "EC"
                : "Provisional " + Academic.gradeFor(result.caEarned() / 40.0 * 100.0);
    }

    private static boolean hasAssessmentMarks(Map<String, Object> row) {
        return row.get("quiz1") != null || row.get("quiz2") != null || row.get("quiz3") != null
                || row.get("mid_mark") != null || row.get("project_mark") != null
                || row.get("assignment_average") != null || row.get("final_mark") != null;
    }

    private static String value(Map<String, Object> row, String key) {
        Object value = row.get(key);
        return value == null ? "" : value.toString();
    }

    private static Double nullableDouble(Object value) {
        return value instanceof Number number ? number.doubleValue() : null;
    }

    private static String nullableString(Object value) {
        return value == null ? null : value.toString();
    }

    private static int term(int level, int semester) {
        return (level - 1) * 2 + semester;
    }
}
