public abstract class User {
    private int userId;
    private String username;
    private String password;
    private String fullName;
    private String email;
    private String phone;
    private String address;
    private String profilePicture;

    public User(int userId, String username, String password, String fullName,
                String email, String phone, String address, String profilePicture) {
        this.userId = userId;
        this.username = username;
        this.password = password;
        this.fullName = fullName;
        this.email = email;
        this.phone = phone;
        this.address = address;
        this.profilePicture = profilePicture;
    }

    public abstract String getRole();

    public int getUserId() { return userId; }
    public String getUsername() { return username; }
    public String getPassword() { return password; }
    public String getFullName() { return fullName; }
    public String getEmail() { return email; }
    public String getPhone() { return phone; }
    public String getAddress() { return address; }
    public String getProfilePicture() { return profilePicture; }

    // No setUsername or setPassword on purpose: lecturers can't change these.
    public void setFullName(String fullName) { this.fullName = fullName; }
    public void setEmail(String email) { this.email = email; }
    public void setPhone(String phone) { this.phone = phone; }
    public void setAddress(String address) { this.address = address; }
    public void setProfilePicture(String profilePicture) { this.profilePicture = profilePicture; }
}