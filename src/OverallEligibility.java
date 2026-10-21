public class OverallEligibility {

    private final AttendanceSummary att;
    private final CAResult ca;          // may be null if no CA data exists

    public OverallEligibility(AttendanceSummary att, CAResult ca) {
        this.att = att;
        this.ca = ca;
    }

    private boolean attendanceOk() { return att.isEligible(); }
    private boolean caOk() { return ca != null && ca.isEligible(); }
    private boolean caPending() { return ca == null || ca.getStatus().equals("Pending"); }

    public String getStatus() {
        if (!attendanceOk()) return "Not Eligible";
        if (caOk()) return "Eligible";
        if (caPending()) return "Pending";
        return "Not Eligible";
    }

    public String getReason() {
        String status = getStatus();
        if (status.equals("Eligible")) return "Attendance and CA requirements met";
        if (status.equals("Pending")) return "CA marks not fully uploaded";

        boolean lowCa = !caOk() && !caPending();
        String lowAtt = "Attendance below " + AttendanceSummary.PASS_PERCENTAGE + "%";
        String lowCaText = "CA below " + (int) CAResult.PASS_PERCENTAGE + "%";
        if (!attendanceOk() && lowCa) return lowAtt + " and " + lowCaText;
        if (!attendanceOk()) return lowAtt;
        return lowCaText;
    }

    // Explanation shown when a single student is selected
    public String getBreakdown() {
        String a = "Attendance: " + att.getSessionsText() + " attended, "
                + att.getMedicalText() + " absences covered by approved medicals = "
                + att.getFinalPercentText()
                + " (needs " + AttendanceSummary.PASS_PERCENTAGE + "%)";
        String c = (ca == null)
                ? "CA: no CA marks available"
                : "CA: " + ca.getCaText() + " = " + ca.getPercentageText()
                + " (needs " + (int) CAResult.PASS_PERCENTAGE + "%), marks entered "
                + ca.getEnteredText();
        return a + "\n" + c;
    }

    public String getRegNo() { return att.getRegNo(); }
    public String getFullName() { return att.getFullName(); }
    public String getAttendanceText() { return att.getFinalPercentText(); }
    public String getAttendanceOk() { return attendanceOk() ? "Yes" : "No"; }
    public String getCaText() { return ca == null ? "-" : ca.getPercentageText(); }
    public String getCaOk() { return caOk() ? "Yes" : (caPending() ? "Pending" : "No"); }
}