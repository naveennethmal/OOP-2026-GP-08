public class Material {
    private final int id;
    private final String courseCode;
    private final String title;
    private final String description;
    private final String storedName;     // name of the file inside the materials folder
    private final String originalName;   // name the lecturer sees
    private final String uploadedDate;

    public Material(int id, String courseCode, String title, String description,
                    String storedName, String originalName, String uploadedDate) {
        this.id = id;
        this.courseCode = courseCode;
        this.title = title;
        this.description = description == null ? "" : description;
        this.storedName = storedName;
        this.originalName = originalName;
        this.uploadedDate = uploadedDate;
    }

    public int getId() { return id; }
    public String getCourseCode() { return courseCode; }
    public String getTitle() { return title; }
    public String getDescription() { return description; }
    public String getStoredName() { return storedName; }
    public String getOriginalName() { return originalName; }
    public String getUploadedDate() { return uploadedDate; }
}