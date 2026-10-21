public class AttendanceSummary {

    public static final int PASS_PERCENTAGE = 80;

    private final String regNo;
    private final String fullName;
    private final int attended;   // Present + Late
    private final int absent;
    private final int covered;    // absences covered by an approved medical
    private final int total;      // total sessions that should have been held

    public AttendanceSummary(String regNo, String fullName,
                             int attended, int absent, int covered, int total) {
        this.regNo = regNo;
        this.fullName = fullName;
        this.attended = attended;
        this.absent = absent;
        this.covered = covered;
        this.total = total;
    }

    public int getAttendedWithMedical() { return attended + covered; }

    // Integer maths, so "exactly 80%" is never lost to rounding
    public boolean isEligible() {
        return total > 0 && getAttendedWithMedical() * 100 >= PASS_PERCENTAGE * total;
    }

    private double pct(int n) { return total == 0 ? 0 : n * 100.0 / total; }

    public String getRegNo() { return regNo; }
    public String getFullName() { return fullName; }
    public String getSessionsText() { return attended + " / " + total; }
    public String getAbsentText() { return String.valueOf(absent); }
    public String getMedicalText() { return String.valueOf(covered); }
    public String getRawPercentText() { return String.format("%.1f%%", pct(attended)); }
    public String getFinalPercentText() { return String.format("%.1f%%", pct(getAttendedWithMedical())); }
    public String getStatus() { return isEligible() ? "Eligible" : "Not Eligible"; }
}