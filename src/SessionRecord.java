public class SessionRecord {
    private final String date;
    private final String sessionNo;
    private final String type;
    private final String status;
    private final String medical;

    public SessionRecord(String date, String sessionNo, String type, String status, String medical) {
        this.date = date;
        this.sessionNo = sessionNo;
        this.type = type;
        this.status = status;
        this.medical = medical;
    }

    public String getDate() { return date; }
    public String getSessionNo() { return sessionNo; }
    public String getType() { return type; }
    public String getStatus() { return status; }
    public String getMedical() { return medical; }
}