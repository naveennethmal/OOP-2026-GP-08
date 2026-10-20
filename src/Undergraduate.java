public class Undergraduate {
    private final String regNo;
    private final String fullName;
    private final String email;
    private final String phone;
    private final String address;
    private final String batch;
    private final String status;
    private final String department;

    public Undergraduate(String regNo, String fullName, String email, String phone,
                         String address, String batch, String status, String department) {
        this.regNo = regNo;
        this.fullName = fullName;
        this.email = email;
        this.phone = phone;
        this.address = address;
        this.batch = batch;
        this.status = status;
        this.department = department;
    }

    public String getRegNo() { return regNo; }
    public String getFullName() { return fullName; }
    public String getEmail() { return email; }
    public String getPhone() { return phone; }
    public String getAddress() { return address; }
    public String getBatch() { return batch; }
    public String getStatus() { return status; }
    public String getDepartment() { return department; }
}