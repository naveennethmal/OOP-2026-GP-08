package Technical_Officer;

import Database.DBConnection;
import java.sql.*;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

public final class NoticeDAO {
    private NoticeDAO() {}

    public record Notice(String title, String category, String date, String body, boolean isNew) {}

    /** All notices, newest first. The notices table has no category column, so it is derived from the title. */
    public static List<Notice> list() throws SQLException {
        String sql = "SELECT title, published_date, description FROM notices ORDER BY published_date DESC, notice_id DESC";
        List<Notice> list = new ArrayList<>();
        LocalDate recent = LocalDate.now().minusDays(7);
        try (Connection con = DBConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                String title = rs.getString("title");
                LocalDate published = rs.getDate("published_date").toLocalDate();
                list.add(new Notice(title, categoryOf(title), published.toString(),
                        rs.getString("description"), !published.isBefore(recent)));
            }
        }
        return list;
    }

    private static String categoryOf(String title) {
        String t = title.toLowerCase();
        if (t.contains("exam")) return "Exam";
        if (t.contains("meeting") || t.contains("event")) return "Event";
        return "General";
    }
}
