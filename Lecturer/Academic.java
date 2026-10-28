package Lecturer;

import java.util.ArrayList;
import java.util.List;

/**
 * Pure calculation rules (no database, no screens):
 * attendance eligibility, CA eligibility, overall eligibility, grades and GPA.
 */
public final class Academic {
    private Academic() {}

    public static final int ATTENDANCE_PASS = 80;       // % of sessions
    public static final double CA_PASS = 50.0;          // 20 of 40 CA points
    public static final double END_EXAM_PASS = 35.0;    // percent mark
    public static final int SESSIONS_PER_COMPONENT = 15;

    // ---------------------------------------------------------------- grades

    // *** CHECK THESE 3 ARRAYS AGAINST UGC CIRCULAR 12-2024 ***
    // Row i means: marks >= MIN_MARKS[i] gets GRADES[i] and POINTS[i]
    private static final double[] MIN_MARKS = {85, 75, 70, 65, 60, 55, 50, 45, 40, 35, 30, 0};
    private static final String[] GRADES = {"A+", "A", "A-", "B+", "B", "B-", "C+", "C", "C-", "D+", "D", "E"};
    private static final double[] POINTS = {4.0, 4.0, 3.7, 3.3, 3.0, 2.7, 2.3, 2.0, 1.7, 1.3, 1.0, 0.0};

    private static int gradeIndex(double marks) {
        for (int i = 0; i < MIN_MARKS.length; i++) if (marks >= MIN_MARKS[i]) return i;
        return MIN_MARKS.length - 1;
    }

    public static String gradeFor(double marks) { return GRADES[gradeIndex(marks)]; }

    public static double pointFor(double marks) { return POINTS[gradeIndex(marks)]; }

    public static double pointForGrade(String grade) {
        for (int i = 0; i < GRADES.length; i++) {
            if (GRADES[i].equals(grade)) return POINTS[i];
        }
        return 0;
    }

    // ------------------------------------------------------------ attendance

    /**
     * How many sessions a student should have attended: 15 per component.
     * courseType is courses.theory_practical ("Theory", "Practical" or "Theory/Practical").
     * component is "Combined", "Theory" or "Practical".
     */
    public static int sessionsFor(String courseType, String component) {
        int theory = courseType.contains("Theory") ? 1 : 0;
        int practical = courseType.contains("Practical") ? 1 : 0;
        int parts = switch (component) {
            case "Theory" -> theory;
            case "Practical" -> practical;
            default -> theory + practical;
        };
        return SESSIONS_PER_COMPONENT * parts;
    }

    /** attended = Present/Late, covered = absences covered by an approved medical. */
    public record Attendance(String regNo, String name, String level, String semester,
                             int attended, int absent, int covered, int total) {

        public int withMedical() { return attended + covered; }

        // integer maths, so "exactly 80%" is never lost to rounding
        public boolean eligible() { return total > 0 && withMedical() * 100 >= ATTENDANCE_PASS * total; }

        private double pct(int n) { return total == 0 ? 0 : n * 100.0 / total; }

        public String sessionsText() { return attended + " / " + total; }
        public String rawText() { return String.format("%.1f%%", pct(attended)); }
        public String finalText() { return String.format("%.1f%%", pct(withMedical())); }
        public String status() { return eligible() ? "Eligible" : "Not Eligible"; }
    }

    // -------------------------------------------------------------------- CA

    /** One assessment of one student. mark == null means "not uploaded yet". */
    public record CaPart(String name, String type, double weight, Double mark) {
        public double contribution() { return mark == null ? 0 : mark / 100.0 * weight; }
        public String weightText() { return String.format("%.2f%%", weight); }
        public String markText() { return mark == null ? "-" : String.format("%.2f", mark); }
        public String contributionText() { return String.format("%.2f", contribution()); }
    }

    /** CA result of one student in one course. */
    public static final class Ca {
        private final String regNo;
        private final String name;
        private final double totalWeight;
        private final boolean practicalCourse;
        private final List<CaPart> parts = new ArrayList<>();

        public Ca(String regNo, String name, double totalWeight, boolean practicalCourse) {
            this.regNo = regNo;
            this.name = name;
            this.totalWeight = totalWeight;
            this.practicalCourse = practicalCourse;
        }

        public void add(CaPart p) { parts.add(p); }

        public String regNo() { return regNo; }
        public String name() { return name; }
        public List<CaPart> parts() { return parts; }

        private boolean hasMidAndProject() { return practicalCourse; }

        public double earned() {
            List<Double> quizzes = parts.stream()
                    .filter(p -> p.type().equalsIgnoreCase("Quiz") && p.mark() != null)
                    .map(CaPart::mark).sorted(java.util.Comparator.reverseOrder()).toList();
            boolean paperCourse = hasMidAndProject();
            double quizPoints = paperCourse ? 0 : quizzes.stream().limit(2).mapToDouble(mark -> mark * 0.05).sum();
            CaPart mid = parts.stream().filter(p -> p.name().equalsIgnoreCase("MID EXAM")).findFirst().orElse(null);
            CaPart project = parts.stream().filter(p -> p.name().equalsIgnoreCase("PROJECT")).findFirst().orElse(null);
            double corePoints;
            if (paperCourse) {
                corePoints = (mid == null || mid.mark() == null ? 0 : mid.mark() * 0.25)
                        + (project == null || project.mark() == null ? 0 : project.mark() * 0.15);
            } else {
                CaPart core = mid != null && mid.mark() != null ? mid : project;
                corePoints = core == null || core.mark() == null ? 0 : core.mark() * 0.25;
            }
            List<Double> assignmentMarks = parts.stream()
                    .filter(p -> p.type().equalsIgnoreCase("Assignment") && p.mark() != null)
                    .map(CaPart::mark).toList();
            double assignmentPoints = paperCourse ? 0 : assignmentMarks.stream()
                    .mapToDouble(mark -> mark * 0.05).average().orElse(0);
            return quizPoints + corePoints + assignmentPoints;
        }
        public int entered() {
            if (hasMidAndProject()) {
                return (int) parts.stream().filter(p -> (p.name().equalsIgnoreCase("MID EXAM")
                        || p.name().equalsIgnoreCase("PROJECT")) && p.mark() != null).count();
            }
            long quizzes = parts.stream().filter(p -> p.type().equalsIgnoreCase("Quiz") && p.mark() != null).count();
            boolean core = parts.stream().anyMatch(p -> (p.name().equalsIgnoreCase("MID EXAM")
                    || p.name().equalsIgnoreCase("PROJECT")) && p.mark() != null);
            return (int) Math.min(3, quizzes) + (core ? 1 : 0);
        }
        private int required() { return hasMidAndProject() ? 2 : 4; }
        public double percent() { return totalWeight == 0 ? 0 : earned() / totalWeight * 100; }
        public boolean eligible() { return entered() == required() && totalWeight > 0 && percent() >= CA_PASS; }

        public String status() {
            if (eligible()) return "Pass";
            if (entered() < required()) return "Pending";
            return "EC";
        }

        public String caText() { return String.format("%.2f / %.2f", earned(), totalWeight); }
        public String percentText() { return String.format("%.2f%%", percent()); }
        public String enteredText() { return entered() + " / " + required(); }
    }

    /** CA points out of 40: practical courses use mid+project (25%+15%); other courses use quizzes, core and assignments. */
    public static double caPoints(Double quiz1, Double quiz2, Double quiz3, Double mid, Double project,
                                  Double assignmentAverage, boolean practicalCourse) {
        if (practicalCourse) {
            return (mid == null ? 0 : mid * 0.25) + (project == null ? 0 : project * 0.15);
        }
        List<Double> quizzes = new ArrayList<>();
        if (quiz1 != null) quizzes.add(quiz1);
        if (quiz2 != null) quizzes.add(quiz2);
        if (quiz3 != null) quizzes.add(quiz3);
        quizzes.sort(java.util.Comparator.reverseOrder());
        double points = quizzes.stream().limit(2).mapToDouble(mark -> mark * 0.05).sum();
        Double core = mid != null ? mid : project;
        return points + (core == null ? 0 : core * 0.25)
                + (assignmentAverage == null ? 0 : assignmentAverage * 0.05);
    }

    // --------------------------------------------------------------- overall

    /** Eligibility requires attendance, at least 20/40 CA, and at least 35% in the end exam. */
    public record Overall(Attendance att, Ca ca, Double endMark) {

        private boolean caOk() { return ca != null && ca.eligible(); }
        private boolean caPending() { return ca == null || ca.status().equals("Pending"); }
        private boolean endPending() { return endMark == null; }
        private boolean endOk() { return endMark != null && endMark >= END_EXAM_PASS; }

        public String status() {
            if (!att.eligible()) return "Not Eligible";
            if (caPending() || endPending()) return "Pending";
            return caOk() && endOk() ? "Eligible" : "Not Eligible";
        }

        public String reason() {
            String status = status();
            if (status.equals("Eligible")) return "Attendance, CA and end exam requirements met";
            List<String> reasons = new ArrayList<>();
            if (!att.eligible()) reasons.add("Attendance below " + ATTENDANCE_PASS + "%");
            if (caPending()) reasons.add("CA marks not fully uploaded");
            else if (!caOk()) reasons.add("CA below 20/40");
            if (endPending()) reasons.add("End exam mark not uploaded");
            else if (!endOk()) reasons.add("End exam below " + (int) END_EXAM_PASS + "%");
            return String.join(" and ", reasons);
        }

        public String breakdown() {
            String a = "Attendance: " + att.sessionsText() + " attended, " + att.covered()
                    + " absences covered by approved medicals = " + att.finalText()
                    + " (needs " + ATTENDANCE_PASS + "%)";
            String c = ca == null ? "CA: no CA marks available"
                    : "CA: " + ca.caText() + " = " + ca.percentText() + " (needs 20/40), marks entered " + ca.enteredText();
            String e = endMark == null ? "End exam: mark not uploaded (needs " + (int) END_EXAM_PASS + ")"
                    : "End exam: " + String.format("%.2f", endMark) + "% (needs " + (int) END_EXAM_PASS + ")";
            return a + "\n" + c + "\n" + e;
        }

        public String caPercentText() { return ca == null ? "-" : ca.percentText(); }
        public String endExamText() { return endMark == null ? "-" : String.format("%.2f", endMark); }
        public String attendanceOk() { return att.eligible() ? "Yes" : "No"; }
        public String caOkText() { return caOk() ? "Yes" : (caPending() ? "Pending" : "No"); }
        public String endExamOkText() { return endMark == null ? "Pending" : (endOk() ? "Yes" : "No"); }
    }

    // ----------------------------------------------------------- grades, GPA

    /**
     * One student in one course. finalMark == null means the final exam is not uploaded yet.
     * creditsText is the courses.credits column: a number, or "NGPA" for courses that do not count in the GPA.
     */
    public record CourseResult(String regNo, String name, String courseCode, String courseName,
                               String creditsText, int level, int semester, double caEarned, double caWeight,
                               double finalWeight, Double finalMark, boolean retake, String gradeOverride) {

        public int credits() {
            try {
                return Integer.parseInt(creditsText.trim());
            } catch (NumberFormatException e) {
                return 0;                                      // NGPA
            }
        }

        public boolean hasFinal() { return finalMark != null; }
        public boolean isGraded() { return hasFinal() || gradeOverride != null; }

        // total marks out of 100 (CA + final), only once the final mark exists
        public double total() {
            double weight = caWeight + finalWeight;
            if (!hasFinal() || weight == 0) return 0;
            return (caEarned + finalMark / 100.0 * finalWeight) / weight * 100;
        }

        public double caPercent() { return caWeight == 0 ? 0 : caEarned / caWeight * 100; }

        public String grade() {
            if (gradeOverride != null) return gradeOverride;
            if (retake && hasFinal()) return "C";
            boolean caFail = caWeight > 0 && caEarned < caWeight * CA_PASS / 100.0;
            boolean endFail = hasFinal() && finalMark < END_EXAM_PASS;
            if (caFail && endFail) return "E";
            if (caFail) return "EC";
            if (endFail) return "EE";
            if (!hasFinal()) return "-";
            return gradeFor(total());
        }
        public double gradePoint() {
            if (!isGraded() || grade().startsWith("E")) return 0;
            if (gradeOverride != null) return pointForGrade(gradeOverride);
            return retake ? pointFor(45) : pointFor(total());
        }

        public String caPercentText() { return String.format("%.1f%%", caPercent()); }
        public String finalText() { return hasFinal() ? String.format("%.1f", finalMark) : "-"; }
        public String totalText() { return hasFinal() ? String.format("%.2f", total()) : "-"; }
        public String gradePointText() { return isGraded() ? String.format("%.2f", gradePoint()) : "-"; }
    }

    /** SGPA / CGPA of one student. A null value means nothing was graded. */
    public record Gpa(String regNo, String name, Double sgpa, Double cgpa, int credits, int graded, int total) {

        /** semester == null means "all semesters". */
        public static Gpa of(List<CourseResult> results, Integer level, Integer semester) {
            CourseResult first = results.get(0);
            List<CourseResult> selected = results.stream()
                    .filter(result -> level == null || result.level() == level)
                    .filter(result -> semester == null || result.semester() == semester)
                    .toList();
            int graded = (int) selected.stream().filter(CourseResult::isGraded).count();
            return new Gpa(first.regNo(), first.name(),
                    credits(selected) == 0 ? null : gpa(selected),
                    credits(results) == 0 ? null : gpa(results),
                    credits(selected), graded, selected.size());
        }

        // Failed courses have zero grade points but their credits remain in the denominator.
        private static double gpa(List<CourseResult> results) {
            double points = 0;
            for (CourseResult r : results) if (isGradedCreditCourse(r)) points += r.gradePoint() * r.credits();
            int c = credits(results);
            return c == 0 ? 0 : points / c;
        }

        private static int credits(List<CourseResult> results) {
            int c = 0;
            for (CourseResult r : results) if (isGradedCreditCourse(r)) c += r.credits();
            return c;
        }

        private static boolean isGradedCreditCourse(CourseResult r) {
            return r.isGraded() && r.credits() > 0;
        }

        public String sgpaText() { return sgpa == null ? "-" : String.format("%.2f", sgpa); }
        public String cgpaText() { return cgpa == null ? "-" : String.format("%.2f", cgpa); }
        public String coursesText() { return graded + " / " + total; }
    }
}
