import java.util.List;

public class GpaSummary {

    private final String regNo;
    private final String fullName;
    private final Double sgpa;      // null = nothing graded in that semester
    private final Double cgpa;      // null = nothing graded at all
    private final int credits;      // credits counted in the CGPA
    private final int graded;       // courses with a final mark
    private final int total;        // all courses

    private GpaSummary(String regNo, String fullName, Double sgpa, Double cgpa,
                       int credits, int graded, int total) {
        this.regNo = regNo;
        this.fullName = fullName;
        this.sgpa = sgpa;
        this.cgpa = cgpa;
        this.credits = credits;
        this.graded = graded;
        this.total = total;
    }

    // semester = null means "all semesters" (SGPA = CGPA)
    public static GpaSummary of(List<CourseResult> results, Integer semester) {
        CourseResult first = results.get(0);
        int graded = 0;
        for (CourseResult r : results) if (r.hasFinal()) graded++;

        Double sgpa = credits(results, semester) == 0 ? null : gpa(results, semester);
        Double cgpa = credits(results, null) == 0 ? null : gpa(results, null);
        return new GpaSummary(first.getRegNo(), first.getFullName(), sgpa, cgpa,
                credits(results, null), graded, results.size());
    }

    // GPA = sum(grade point x credits) / sum(credits), only for courses with a final mark
    public static double gpa(List<CourseResult> results, Integer semester) {
        double points = 0;
        for (CourseResult r : results) {
            if (r.hasFinal() && (semester == null || r.getSemester() == semester)) {
                points += r.getGradePoint() * r.getCredits();
            }
        }
        int c = credits(results, semester);
        return c == 0 ? 0 : points / c;
    }

    private static int credits(List<CourseResult> results, Integer semester) {
        int c = 0;
        for (CourseResult r : results) {
            if (r.hasFinal() && (semester == null || r.getSemester() == semester)) {
                c += r.getCredits();
            }
        }
        return c;
    }

    public String getRegNo() { return regNo; }
    public String getFullName() { return fullName; }
    public Double getCgpa() { return cgpa; }
    public String getSgpaText() { return sgpa == null ? "-" : String.format("%.2f", sgpa); }
    public String getCgpaText() { return cgpa == null ? "-" : String.format("%.2f", cgpa); }
    public String getCreditsText() { return String.valueOf(credits); }
    public String getCoursesText() { return graded + " / " + total; }
}