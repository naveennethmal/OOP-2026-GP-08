public class MedicalRecord {
    private final String regNo;
    private final String fullName;
    private final String date;
    private final String reason;
    private final String status;

    public MedicalRecord(String regNo, String fullName, String date, String reason, String status) {
        this.regNo = regNo;
        this.fullName = fullName;
        this.date = date;
        this.reason = reason;
        this.status = status;
    }

    public String getRegNo() { return regNo; }
    public String getFullName() { return fullName; }
    public String getDate() { return date; }
    public String getReason() { return reason; }
    public String getStatus() { return status; }
}