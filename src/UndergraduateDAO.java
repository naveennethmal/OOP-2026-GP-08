import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

public class UndergraduateDAO {

    public List<Undergraduate> getAll() throws SQLException {
        List<Undergraduate> list = new ArrayList<>();
        String sql = "SELECT g.reg_no, u.full_name, u.email, u.phone, u.address, "
                + "g.batch, g.status, d.department_name "
                + "FROM undergraduates g "
                + "JOIN users u ON g.user_id = u.user_id "
                + "JOIN departments d ON g.department_id = d.department_id "
                + "ORDER BY g.batch, g.reg_no";

        try (Connection con = DBConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {

            while (rs.next()) {
                list.add(new Undergraduate(
                        rs.getString("reg_no"),
                        rs.getString("full_name"),
                        rs.getString("email"),
                        rs.getString("phone"),
                        rs.getString("address"),
                        rs.getString("batch"),
                        rs.getString("status"),
                        rs.getString("department_name")));
            }
        }
        return list;
    }

    public List<String> getBatches() throws SQLException {
        List<String> batches = new ArrayList<>();
        String sql = "SELECT DISTINCT batch FROM undergraduates ORDER BY batch";
        try (Connection con = DBConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) batches.add(rs.getString("batch"));
        }
        return batches;
    }
}