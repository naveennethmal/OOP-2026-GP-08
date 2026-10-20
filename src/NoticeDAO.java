import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class NoticeDAO {

    // All notices, newest first
    public List<Notice> getAllNotices() throws SQLException {
        List<Notice> list = new ArrayList<>();
        String sql = "SELECT notice_id, title, published_date, description "
                + "FROM notices ORDER BY published_date DESC, notice_id DESC";

        try (Connection con = DBConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {

            while (rs.next()) {
                list.add(new Notice(
                        rs.getInt("notice_id"),
                        rs.getString("title"),
                        rs.getDate("published_date").toString(),
                        rs.getString("description")));
            }
        }
        return list;
    }
}