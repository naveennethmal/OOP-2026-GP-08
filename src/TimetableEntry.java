public class TimetableEntry {
    private final String day;
    private final String time;
    private final String course;
    private final String location;
    private final String type;

    public TimetableEntry(String day, String time, String course, String location, String type) {
        this.day = day;
        this.time = time;
        this.course = course;
        this.location = location;
        this.type = type;
    }

    public String getDay() { return day; }
    public String getTime() { return time; }
    public String getCourse() { return course; }
    public String getLocation() { return location; }
    public String getType() { return type; }
}