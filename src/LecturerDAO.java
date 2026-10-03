import java.sql.*;

public class LecturerDAO {

    // Returns the Lecturer if username and password are correct, otherwise null
    public Lecturer login(String username, String password) throws SQLException {
        String sql = "SELECT u.*, l.lecturer_id, l.employee_no, l.department_id "
                + "FROM users u JOIN lecturers l ON u.user_id = l.user_id "
                + "WHERE u.user_name = ? AND u.password = ? AND u.role = 'Lecturer'";

        try (Connection con = DBConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {

            ps.setString(1, username);
            ps.setString(2, password);

            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return new Lecturer(
                            rs.getInt("user_id"),
                            rs.getString("user_name"),
                            rs.getString("password"),
                            rs.getString("full_name"),
                            rs.getString("email"),
                            rs.getString("phone"),
                            rs.getString("address"),
                            rs.getString("profile_picture"),
                            rs.getInt("lecturer_id"),
                            rs.getString("employee_no"),
                            rs.getInt("department_id"));
                }
            }
        }
        return null;
    }
}