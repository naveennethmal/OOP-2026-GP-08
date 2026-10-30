package Technical_Officer;

import Database.DBConnection;
import java.sql.*;

public final class ProfileDAO {
    private ProfileDAO() {}

    public record TechnicianProfile(String fullName, String email, String phone, String address,
                                    String username, String picture, String department) {
        public String initials() {
            if (fullName == null || fullName.isBlank()) return "?";
            String[] parts = fullName.trim().split("\\s+");
            String first = parts[0].substring(0, 1);
            String second = parts.length > 1 ? parts[parts.length - 1].substring(0, 1) : "";
            return (first + second).toUpperCase();
        }
    }

    public static TechnicianProfile get() throws SQLException {
        String sql = """
                SELECT us.full_name, us.email, us.phone, us.address, us.user_name,
                       us.profile_picture, d.department_name
                FROM users us
                JOIN technical_officers t ON t.user_id = us.user_id
                JOIN departments d ON d.department_id = us.department_id
                WHERE us.user_id = ?
                """;
        try (Connection con = DBConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, TechnicalOfficerContext.userId());
            try (ResultSet rs = ps.executeQuery()) {
                if (!rs.next()) throw new SQLException("Technical officer profile not found.");
                return new TechnicianProfile(
                        rs.getString("full_name"),
                        rs.getString("email"),
                        rs.getString("phone"),
                        rs.getString("address"),
                        rs.getString("user_name"),
                        rs.getString("profile_picture"),
                        rs.getString("department_name"));
            }
        }
    }

    /** Username and password are never changed here. A null picture keeps the old one. */
    public static void save(String fullName, String email, String phone, String address, String picture)
            throws SQLException {
        String sql = "UPDATE users SET full_name = ?, email = ?, phone = ?, address = ?, "
                + "profile_picture = COALESCE(?, profile_picture) WHERE user_id = ?";
        try (Connection con = DBConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setString(1, fullName);
            ps.setString(2, email);
            ps.setString(3, phone);
            ps.setString(4, address);
            ps.setString(5, picture);
            ps.setInt(6, TechnicalOfficerContext.userId());
            ps.executeUpdate();
        }
    }

    public static void removePicture() throws SQLException {
        try (Connection con = DBConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(
                     "UPDATE users SET profile_picture = NULL WHERE user_id = ?")) {
            ps.setInt(1, TechnicalOfficerContext.userId());
            ps.executeUpdate();
        }
    }
}
