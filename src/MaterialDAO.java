import java.sql.*;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

public class MaterialDAO {

    public List<Material> getByCourse(String courseCode) throws SQLException {
        List<Material> list = new ArrayList<>();
        String sql = "SELECT material_id, course_code, title, description, file_name, "
                + "original_name, uploaded_date FROM course_materials "
                + "WHERE course_code = ? ORDER BY uploaded_date DESC, material_id DESC";
        try (Connection con = DBConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setString(1, courseCode);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    list.add(new Material(
                            rs.getInt("material_id"), rs.getString("course_code"),
                            rs.getString("title"), rs.getString("description"),
                            rs.getString("file_name"), rs.getString("original_name"),
                            rs.getDate("uploaded_date").toString()));
                }
            }
        }
        return list;
    }

    public void add(Material m, int lecturerId) throws SQLException {
        String sql = "INSERT INTO course_materials (course_code, lecturer_id, title, description, "
                + "file_name, original_name, uploaded_date) VALUES (?, ?, ?, ?, ?, ?, ?)";
        try (Connection con = DBConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setString(1, m.getCourseCode());
            ps.setInt(2, lecturerId);
            ps.setString(3, m.getTitle());
            ps.setString(4, m.getDescription());
            ps.setString(5, m.getStoredName());
            ps.setString(6, m.getOriginalName());
            ps.setDate(7, Date.valueOf(LocalDate.now()));
            ps.executeUpdate();
        }
    }

    public void update(Material m) throws SQLException {
        String sql = "UPDATE course_materials SET title = ?, description = ?, "
                + "file_name = ?, original_name = ? WHERE material_id = ?";
        try (Connection con = DBConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setString(1, m.getTitle());
            ps.setString(2, m.getDescription());
            ps.setString(3, m.getStoredName());
            ps.setString(4, m.getOriginalName());
            ps.setInt(5, m.getId());
            ps.executeUpdate();
        }
    }

    public void delete(int materialId) throws SQLException {
        String sql = "DELETE FROM course_materials WHERE material_id = ?";
        try (Connection con = DBConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, materialId);
            ps.executeUpdate();
        }
    }
}