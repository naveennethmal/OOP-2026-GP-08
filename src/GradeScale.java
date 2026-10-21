public class GradeScale {

    // *** CHECK THESE 3 ARRAYS AGAINST UGC CIRCULAR 12-2024 ***
    // Row i means: marks >= MIN_MARKS[i] gets GRADES[i] and POINTS[i]
    private static final double[] MIN_MARKS = {85, 75, 70, 65, 60, 55, 50, 45, 40, 35, 30, 0};
    private static final String[] GRADES    = {"A+", "A", "A-", "B+", "B", "B-", "C+", "C", "C-", "D+", "D", "E"};
    private static final double[] POINTS    = {4.0, 4.0, 3.7, 3.3, 3.0, 2.7, 2.3, 2.0, 1.7, 1.3, 1.0, 0.0};

    public static String gradeFor(double marks) {
        return GRADES[indexOf(marks)];
    }

    public static double pointFor(double marks) {
        return POINTS[indexOf(marks)];
    }

    private static int indexOf(double marks) {
        for (int i = 0; i < MIN_MARKS.length; i++) {
            if (marks >= MIN_MARKS[i]) return i;
        }
        return MIN_MARKS.length - 1;
    }
}