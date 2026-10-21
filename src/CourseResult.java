public class CourseResult {

    private final String regNo;
    private final String fullName;
    private final String courseCode;
    private final String courseName;
    private final int credits;
    private final int semester;
    private final double caEarned;     // weighted CA marks earned
    private final double caWeight;     // total weight of the CA assessments
    private final double finalWeight;  // weight of the final exam
    private final Double finalMark;    // null = final marks not uploaded yet

    public CourseResult(String regNo, String fullName, String courseCode, String courseName,
                        int credits, int semester, double caEarned, double caWeight,
                        double finalWeight, Double finalMark) {
        this.regNo = regNo;
        this.fullName = fullName;
        this.courseCode = courseCode;
        this.courseName = courseName;
        this.credits = credits;
        this.semester = semester;
        this.caEarned = caEarned;
        this.caWeight = caWeight;
        this.finalWeight = finalWeight;
        this.finalMark = finalMark;
    }

    public boolean hasFinal() { return finalMark != null; }

    // Total marks out of 100 (CA + final), only once the final mark exists
    public double getTotal() {
        double weight = caWeight + finalWeight;
        if (!hasFinal() || weight == 0) return 0;
        double earned = caEarned + finalMark / 100.0 * finalWeight;
        return earned / weight * 100;
    }

    public double getCaPercent() { return caWeight == 0 ? 0 : caEarned / caWeight * 100; }
    public boolean isCaEligible() { return caWeight > 0 && getCaPercent() >= CAResult.PASS_PERCENTAGE; }

    public String getGrade() { return hasFinal() ? GradeScale.gradeFor(getTotal()) : "-"; }
    public double getGradePoint() { return hasFinal() ? GradeScale.pointFor(getTotal()) : 0; }

    public String getRegNo() { return regNo; }
    public String getFullName() { return fullName; }
    public String getCourseCode() { return courseCode; }
    public String getCourseName() { return courseName; }
    public int getCredits() { return credits; }
    public int getSemester() { return semester; }

    // Text getters used by the tables
    public String getCreditsText() { return String.valueOf(credits); }
    public String getSemesterText() { return String.valueOf(semester); }
    public String getCaPercentText() { return String.format("%.1f%%", getCaPercent()); }
    public String getFinalText() { return hasFinal() ? String.format("%.1f", finalMark) : "-"; }
    public String getTotalText() { return hasFinal() ? String.format("%.2f", getTotal()) : "-"; }
    public String getGradePointText() { return hasFinal() ? String.format("%.2f", getGradePoint()) : "-"; }
}