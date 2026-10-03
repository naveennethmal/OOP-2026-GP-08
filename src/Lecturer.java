public class Lecturer extends User {
    private int lecturerId;
    private String employeeNo;
    private int departmentId;

    public Lecturer(int userId, String username, String password, String fullName,
                    String email, String phone, String address, String profilePicture,
                    int lecturerId, String employeeNo, int departmentId) {
        super(userId, username, password, fullName, email, phone, address, profilePicture);
        this.lecturerId = lecturerId;
        this.employeeNo = employeeNo;
        this.departmentId = departmentId;
    }

    @Override
    public String getRole() { return "Lecturer"; }

    public int getLecturerId() { return lecturerId; }
    public String getEmployeeNo() { return employeeNo; }
    public int getDepartmentId() { return departmentId; }
}